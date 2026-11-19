// Streams the screen of an iOS simulator and injects touches, keys and gestures, headless: no Simulator window and
// no screen recording permission. It reads the device's framebuffer (an IOSurface shared by CoreSimulator) and sends
// HID events the way Simulator.app does (SimulatorKit's Indigo messages), both private APIs loaded at run time.
//
// Usage: simstream <udid> <port> [quality 0..1] [scale 0..1]
//
// Listens on 127.0.0.1:<port>, one viewer at a time, with the protocol of the interactive preview:
//   to the viewer: [type: u8][length: u32 big-endian][payload], type 1 a JPEG frame, type 2 a JSON message
//                  ({"t":"info","width":…,"height":…} in points, sent first and when the screen changes size);
//   from the viewer: one JSON command per line, coordinates in points:
//                  down/move/up {x,y}, scroll {x,y,dx,dy}, text {s}, backspace, key {usage,shift},
//                  button {name: home, appSwitcher}, edge {phase: down/move/up, x, y, edge} (a touch from a screen edge).
//
// Debugging: `simstream <udid> 0 <quality> <scale> --commands '<JSON array>'` runs commands and exits;
// `--frame <file>` writes one frame. SIMSTREAM_DEBUG=1 logs the HID messages; SIMSTREAM_TOUCH_TARGET=<n> forces the
// Indigo target of touches.

#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>
#import <IOSurface/IOSurface.h>
#import <ImageIO/ImageIO.h>
#import <CoreImage/CoreImage.h>
#import <objc/message.h>
#import <mach/mach_time.h>
#include <dlfcn.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <unistd.h>

typedef struct IndigoMessage IndigoMessage;
typedef IndigoMessage *(*MouseEventFn)(CGPoint *, CGPoint *, uint32_t, NSEventType, uint64_t, CGSize);
typedef IndigoMessage *(*KeyboardFn)(uint32_t, uint32_t);


static const uint32_t kDown = 1;
static const uint32_t kUp = 2;

static MouseEventFn mouseEvent;
static KeyboardFn keyboardEvent;

static id device;
static id hidClient;
static id screen;
static uint32_t touchTarget = 1;
static dispatch_queue_t hidQueue;
static dispatch_queue_t sendQueue;
static int clientFd = -1;
static float jpegQuality = 0.75f;
static double frameScale = 1.0;
static double pointScale = 3.0;

static void fail(NSString *message) {
    fprintf(stderr, "simstream: %s\n", message.UTF8String);
    exit(1);
}

// MARK: - Simulator

static id findDevice(NSString *udid) {
    if (!dlopen("/Library/Developer/PrivateFrameworks/CoreSimulator.framework/CoreSimulator", RTLD_NOW)) fail(@"CoreSimulator not found");
    NSString *developer = [[NSProcessInfo processInfo].environment objectForKey:@"DEVELOPER_DIR"] ?: @"/Applications/Xcode.app/Contents/Developer";
    NSString *simulatorKit = [[developer stringByDeletingLastPathComponent] stringByAppendingPathComponent:@"SharedFrameworks/SimulatorKit.framework/SimulatorKit"];
    void *kit = dlopen(simulatorKit.fileSystemRepresentation, RTLD_NOW);
    if (!kit) fail([NSString stringWithFormat:@"SimulatorKit not found at %@", simulatorKit]);
    mouseEvent = (MouseEventFn)dlsym(kit, "IndigoHIDMessageForMouseNSEvent");
    keyboardEvent = (KeyboardFn)dlsym(kit, "IndigoHIDMessageForKeyboardArbitrary");

    if (!mouseEvent || !keyboardEvent) fail(@"Indigo HID functions not found");

    NSError *error = nil;
    id context = ((id (*)(id, SEL, id, NSError **))objc_msgSend)(NSClassFromString(@"SimServiceContext"),
        NSSelectorFromString(@"sharedServiceContextForDeveloperDir:error:"), developer, &error);
    id set = ((id (*)(id, SEL, NSError **))objc_msgSend)(context, NSSelectorFromString(@"defaultDeviceSetWithError:"), &error);
    for (id candidate in [set valueForKey:@"devices"]) {
        if ([[[candidate valueForKey:@"UDID"] UUIDString] caseInsensitiveCompare:udid] == NSOrderedSame) return candidate;
    }
    fail([NSString stringWithFormat:@"No simulator %@", udid]);
    return nil;
}

/** UIInterfaceOrientation of the shown screen: 1 portrait, 2 upside down, 3 and 4 landscape. */
static volatile long orientation = 1;
/** ID of the shown screen, to notice a switch (each lookup returns a new proxy for the same screen). */
static volatile uint32_t shownScreenID = 0;
/**
 * Whether touches can be sent: not on a device with several integrated screens (iPhone Duo), whose touchscreens are
 * CoreDevice HID services that Indigo messages don't reach (one sent there crashes backboardd).
 */
static BOOL touchesSupported = YES;

static long longProperty(id object, NSString *name) {
    return ((long (*)(id, SEL))objc_msgSend)(object, NSSelectorFromString(name));
}

/**
 * The screen to show: the integrated screen whose backlight is on (state 2), else the first IO port whose descriptor
 * renders to an IOSurface. A foldable device (iPhone Duo) has two, and lights one or the other with its posture.
 * Also sets the Indigo target of its touches the way SimulatorKit derives it from the screen properties (1 for screen
 * types 1 and 2, else the screen ID with bit 30 set), and its orientation.
 */
static id findScreen(void) {
    id io = ((id (*)(id, SEL))objc_msgSend)(device, NSSelectorFromString(@"io"));
    id first = nil, lit = nil;
    int integrated = 0;
    for (id port in ((id (*)(id, SEL))objc_msgSend)(io, NSSelectorFromString(@"ioPorts"))) {
        id descriptor = ((id (*)(id, SEL))objc_msgSend)(port, NSSelectorFromString(@"descriptor"));
        if (![descriptor conformsToProtocol:NSProtocolFromString(@"SimDisplayIOSurfaceRenderable")]) continue;
        if (!((id (*)(id, SEL))objc_msgSend)(descriptor, NSSelectorFromString(@"framebufferSurface"))) continue;
        if (!first) first = descriptor;
        integrated++;
        if (![descriptor conformsToProtocol:NSProtocolFromString(@"SimScreen")]) continue;
        id properties = ((id (*)(id, SEL))objc_msgSend)(descriptor, NSSelectorFromString(@"screenProperties"));
        id backlight = ((id (*)(id, SEL))objc_msgSend)(properties, NSSelectorFromString(@"backlight"));
        if (backlight && (int32_t)longProperty(backlight, @"state") == 2 && !lit) lit = descriptor;
    }
    touchesSupported = integrated <= 1;
    id chosen = lit ?: first;
    if ([chosen conformsToProtocol:NSProtocolFromString(@"SimScreen")]) {
        id properties = ((id (*)(id, SEL))objc_msgSend)(chosen, NSSelectorFromString(@"screenProperties"));
        long type = longProperty(properties, @"screenType");
        uint32_t screenID = (uint32_t)longProperty(properties, @"screenID");
        touchTarget = (type == 1 || type == 2) ? 1 : (screenID | 0x40000000);
        shownScreenID = screenID;
        long uiOrientation = longProperty(properties, @"uiOrientation");
        orientation = (uiOrientation >= 1 && uiOrientation <= 4) ? uiOrientation : 1;
    }
    return chosen;
}

static BOOL landscape(void) {
    return orientation == 3 || orientation == 4;
}

static IOSurfaceRef currentSurface(void) {
    return (__bridge IOSurfaceRef)((id (*)(id, SEL))objc_msgSend)(screen, NSSelectorFromString(@"framebufferSurface"));
}

/** Screen size in points, from the device type's scale, as the framebuffer is laid out (the device held upright). */
static CGSize pointSize(IOSurfaceRef surface) {
    return CGSizeMake(IOSurfaceGetWidth(surface) / pointScale, IOSurfaceGetHeight(surface) / pointScale);
}

/** Screen size in points as the viewer shows it: turned with the interface. */
static CGSize displaySize(IOSurfaceRef surface) {
    CGSize size = pointSize(surface);
    return landscape() ? CGSizeMake(size.height, size.width) : size;
}

/** A point of the viewer (the turned screen) in the framebuffer's coordinates, which touches use. */
static CGPoint framebufferPoint(double x, double y, CGSize native) {
    switch (orientation) {
        case 2: return CGPointMake(native.width - x, native.height - y);
        case 3: return CGPointMake(y, native.height - x);
        case 4: return CGPointMake(native.width - y, x);
        default: return CGPointMake(x, y);
    }
}

static void sendHID(IndigoMessage *message) {
    if (getenv("SIMSTREAM_DEBUG")) fprintf(stderr, "simstream: message %p\n", message);
    if (!message) return;
    ((void (*)(id, SEL, IndigoMessage *, BOOL, dispatch_queue_t, void (^)(NSError *)))objc_msgSend)(hidClient,
        NSSelectorFromString(@"sendWithMessage:freeWhenDone:completionQueue:completion:"), message, YES, hidQueue, ^(NSError *error) {
            if (error) fprintf(stderr, "simstream: HID %s\n", error.description.UTF8String);
            else if (getenv("SIMSTREAM_DEBUG")) fprintf(stderr, "simstream: HID sent\n");
        });
}

/** A touch at ([x], [y]) of the viewer, from screen [edge] (0: none) for system gestures. */
static void touchFromEdge(NSEventType type, double x, double y, uint64_t edge) {
    if (!touchesSupported && !getenv("SIMSTREAM_TOUCH_TARGET")) return;
    IOSurfaceRef surface = currentSurface();
    if (!surface) return;
    CGSize size = pointSize(surface);
    CGPoint point = framebufferPoint(x, y, size);
    // Debugging: SIMSTREAM_TOUCH_TARGET overrides the Indigo target.
    const char *forced = getenv("SIMSTREAM_TOUCH_TARGET");
    uint32_t target = forced ? (uint32_t)strtoul(forced, NULL, 0) : touchTarget;
    sendHID(mouseEvent(&point, NULL, target, type, edge, size));
}

static void touch(NSEventType type, double x, double y) {
    touchFromEdge(type, x, y, 0);
}

static void key(uint32_t usage, BOOL shift) {
    if (shift) sendHID(keyboardEvent(0xE1, kDown));
    sendHID(keyboardEvent(usage, kDown));
    sendHID(keyboardEvent(usage, kUp));
    if (shift) sendHID(keyboardEvent(0xE1, kUp));
}

/** HID usage of a character on a US keyboard, and whether it needs shift; 0 when it has none. */
static uint32_t usageOf(unichar c, BOOL *shift) {
    *shift = NO;
    if (c >= 'a' && c <= 'z') return 0x04 + (c - 'a');
    if (c >= 'A' && c <= 'Z') { *shift = YES; return 0x04 + (c - 'A'); }
    if (c >= '1' && c <= '9') return 0x1E + (c - '1');
    if (c == '0') return 0x27;
    static NSString *plain = @"\n\t -=[]\\;'`,./";
    static const uint32_t plainUsages[] = {0x28, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38};
    static NSString *shifted = @"!@#$%^&*()_+{}|:\"~<>?";
    static const uint32_t shiftedUsages[] = {0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38};
    NSRange r = [plain rangeOfString:[NSString stringWithCharacters:&c length:1]];
    if (r.location != NSNotFound) return plainUsages[r.location];
    r = [shifted rangeOfString:[NSString stringWithCharacters:&c length:1]];
    if (r.location != NSNotFound) { *shift = YES; return shiftedUsages[r.location]; }
    return 0;
}

static void typeString(NSString *text) {
    // Characters without a key (accents, emoji) go through the pasteboard and a paste.
    BOOL allKeys = YES;
    for (NSUInteger i = 0; i < text.length; i++) {
        BOOL shift;
        if (!usageOf([text characterAtIndex:i], &shift)) allKeys = NO;
    }
    if (!allKeys) {
        NSTask *task = [NSTask new];
        task.executableURL = [NSURL fileURLWithPath:@"/usr/bin/xcrun"];
        task.arguments = @[@"simctl", @"pbcopy", [[device valueForKey:@"UDID"] UUIDString]];
        NSPipe *pipe = [NSPipe pipe];
        task.standardInput = pipe;
        [task launchAndReturnError:nil];
        [pipe.fileHandleForWriting writeData:[text dataUsingEncoding:NSUTF8StringEncoding]];
        [pipe.fileHandleForWriting closeFile];
        [task waitUntilExit];
        sendHID(keyboardEvent(0xE3, kDown));  // Command
        key(0x19, NO);                        // V
        sendHID(keyboardEvent(0xE3, kUp));
        return;
    }
    for (NSUInteger i = 0; i < text.length; i++) {
        BOOL shift;
        uint32_t usage = usageOf([text characterAtIndex:i], &shift);
        key(usage, shift);
    }
}

/** The framebuffer's edge that is the bottom of the screen as shown (Indigo edges: top 1, left 2, bottom 3, right 4). */
static uint64_t bottomEdge(void) {
    switch (orientation) {
        case 2: return 1;
        case 3: return 4;
        case 4: return 2;
        default: return 3;
    }
}

/** A swipe up from the bottom of the screen as shown, to [stop] of its height, held [hold] µs before lifting. */
static void swipeUpFromBottom(double stop, int steps, useconds_t stepDelay, useconds_t hold) {
    IOSurfaceRef surface = currentSurface();
    if (!surface) return;
    CGSize size = displaySize(surface);
    uint64_t edge = bottomEdge();
    double x = size.width / 2;
    touchFromEdge(NSEventTypeLeftMouseDown, x, size.height - 2, edge);
    for (int i = 1; i <= steps; i++) {
        usleep(stepDelay);
        touchFromEdge(NSEventTypeLeftMouseDragged, x, size.height - 2 - size.height * stop * i / steps, edge);
    }
    // Holding still (moving by a point) is what tells the app switcher from Home.
    for (useconds_t held = 0; held < hold; held += 60000) {
        usleep(60000);
        touchFromEdge(NSEventTypeLeftMouseDragged, x, size.height * (1 - stop) - (held / 60000 % 2), edge);
    }
    touchFromEdge(NSEventTypeLeftMouseUp, x, size.height * (1 - stop), edge);
}

/** Home as on a Face ID iPhone: a swipe up from the bottom edge. */
static void home(void) {
    swipeUpFromBottom(0.5, 8, 10000, 0);
}

/** The app switcher as on a Face ID iPhone: a swipe up from the bottom edge that pauses midway. */
static void appSwitcher(void) {
    swipeUpFromBottom(0.35, 10, 15000, 480000);
}

/**
 * The hardware gestures: `home` and `appSwitcher`. Physical buttons (lock, volume) aren't sent: the Indigo button
 * messages of SimulatorKit have no effect, or crash backboardd, on current runtimes.
 */
static void button(NSString *name) {
    if ([name isEqual:@"home"]) home();
    else if ([name isEqual:@"appSwitcher"]) appSwitcher();
}

/** A wheel scroll as a short drag, as a trackpad would do on a device. */
static void scroll(double x, double y, double dx, double dy) {
    const int steps = 6;
    touch(NSEventTypeLeftMouseDown, x, y);
    for (int i = 1; i <= steps; i++) {
        usleep(8000);
        touch(NSEventTypeLeftMouseDragged, x - dx * i / steps, y - dy * i / steps);
    }
    usleep(8000);
    touch(NSEventTypeLeftMouseUp, x - dx, y - dy);
}

// MARK: - Streaming

static void sendPacket(uint8_t type, NSData *payload) {
    int fd = clientFd;
    if (fd < 0) return;
    uint8_t header[5];
    uint32_t length = htonl((uint32_t)payload.length);
    header[0] = type;
    memcpy(header + 1, &length, 4);
    NSMutableData *packet = [NSMutableData dataWithBytes:header length:5];
    [packet appendData:payload];
    const uint8_t *bytes = packet.bytes;
    size_t left = packet.length;
    while (left > 0) {
        ssize_t n = write(fd, bytes, left);
        if (n <= 0) return;
        bytes += n;
        left -= n;
    }
}

static void sendJSON(NSDictionary *message) {
    sendPacket(2, [NSJSONSerialization dataWithJSONObject:message options:0 error:nil]);
}

static CIContext *ciContext;

static NSData *encodeFrame(IOSurfaceRef surface) {
    CIImage *image = [CIImage imageWithIOSurface:surface];
    // Turned with the interface (Core Image turns counterclockwise for positive angles).
    double angle = orientation == 4 ? M_PI_2 : orientation == 3 ? -M_PI_2 : orientation == 2 ? M_PI : 0;
    if (angle != 0) image = [image imageByApplyingTransform:CGAffineTransformMakeRotation(angle)];
    if (frameScale != 1.0) image = [image imageByApplyingTransform:CGAffineTransformMakeScale(frameScale, frameScale)];
    CGImageRef cgImage = [ciContext createCGImage:image fromRect:image.extent];
    if (!cgImage) return nil;
    NSMutableData *data = [NSMutableData data];
    CGImageDestinationRef destination = CGImageDestinationCreateWithData((__bridge CFMutableDataRef)data, (__bridge CFStringRef)@"public.jpeg", 1, NULL);
    CGImageDestinationAddImage(destination, cgImage, (__bridge CFDictionaryRef)@{(__bridge NSString *)kCGImageDestinationLossyCompressionQuality: @(jpegQuality)});
    CGImageDestinationFinalize(destination);
    CFRelease(destination);
    CGImageRelease(cgImage);
    return data;
}

/** Sends a frame whenever the framebuffer changed, at most 60 times a second, while a viewer is connected. */
static void streamFrames(int fd) {
    uint32_t lastSeed = UINT32_MAX;
    size_t lastWidth = 0, lastHeight = 0;
    long lastOrientation = 0;
    uint32_t lastScreenID = UINT32_MAX;
    const uint64_t period = 16666667;  // ns: 60 frames a second
    mach_timebase_info_data_t timebase;
    mach_timebase_info(&timebase);
    while (clientFd == fd) {
        uint64_t tick = mach_absolute_time() * timebase.numer / timebase.denom;
        @autoreleasepool {
            IOSurfaceRef surface = currentSurface();
            if (surface) {
                size_t width = IOSurfaceGetWidth(surface), height = IOSurfaceGetHeight(surface);
                if (width != lastWidth || height != lastHeight || orientation != lastOrientation || shownScreenID != lastScreenID) {
                    CGSize size = displaySize(surface);
                    __block NSDictionary *info = @{@"t": @"info", @"width": @(size.width), @"height": @(size.height), @"touches": @(touchesSupported), @"scale": @(pointScale), @"screen": @(shownScreenID)};
                    dispatch_sync(sendQueue, ^{ sendJSON(info); });
                    lastWidth = width;
                    lastHeight = height;
                    lastOrientation = orientation;
                    lastScreenID = shownScreenID;
                    lastSeed = UINT32_MAX;
                }
                uint32_t seed = IOSurfaceGetSeed(surface);
                if (seed != lastSeed) {
                    lastSeed = seed;
                    NSData *frame = encodeFrame(surface);
                    if (frame) dispatch_sync(sendQueue, ^{ sendPacket(1, frame); });
                }
            }
        }
        uint64_t elapsed = mach_absolute_time() * timebase.numer / timebase.denom - tick;
        if (elapsed < period) usleep((useconds_t)((period - elapsed) / 1000));
    }
}

static void handle(NSDictionary *command) {
    NSString *type = command[@"t"];
    double x = [command[@"x"] doubleValue], y = [command[@"y"] doubleValue];
    if ([type isEqual:@"down"]) touch(NSEventTypeLeftMouseDown, x, y);
    else if ([type isEqual:@"move"]) touch(NSEventTypeLeftMouseDragged, x, y);
    else if ([type isEqual:@"up"]) touch(NSEventTypeLeftMouseUp, x, y);
    else if ([type isEqual:@"scroll"]) scroll(x, y, [command[@"dx"] doubleValue], [command[@"dy"] doubleValue]);
    else if ([type isEqual:@"text"]) typeString(command[@"s"] ?: @"");
    else if ([type isEqual:@"backspace"]) key(0x2A, NO);
    else if ([type isEqual:@"key"]) key([command[@"usage"] unsignedIntValue], [command[@"shift"] boolValue]);
    else if ([type isEqual:@"button"]) button(command[@"name"]);
    else if ([type isEqual:@"edge"]) touchFromEdge([command[@"phase"] isEqual:@"down"] ? NSEventTypeLeftMouseDown : [command[@"phase"] isEqual:@"up"] ? NSEventTypeLeftMouseUp : NSEventTypeLeftMouseDragged, x, y, [command[@"edge"] unsignedLongLongValue]);
}

static void serve(int fd) {
    clientFd = fd;
    int yes = 1;
    setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &yes, sizeof(yes));
    [NSThread detachNewThreadWithBlock:^{ streamFrames(fd); }];
    NSMutableData *buffer = [NSMutableData data];
    char chunk[4096];
    ssize_t n;
    while ((n = read(fd, chunk, sizeof(chunk))) > 0) {
        [buffer appendBytes:chunk length:n];
        while (true) {
            const char *bytes = buffer.bytes;
            const char *newline = memchr(bytes, '\n', buffer.length);
            if (!newline) break;
            NSUInteger length = newline - bytes;
            NSData *line = [buffer subdataWithRange:NSMakeRange(0, length)];
            [buffer replaceBytesInRange:NSMakeRange(0, length + 1) withBytes:NULL length:0];
            NSDictionary *command = [NSJSONSerialization JSONObjectWithData:line options:0 error:nil];
            if ([command isKindOfClass:NSDictionary.class]) {
                if ([command[@"t"] isEqual:@"stop"]) goto done;
                handle(command);
            }
        }
    }
done:
    // A newer viewer may have replaced this one already.
    if (clientFd == fd) clientFd = -1;
    close(fd);
}

int main(int argc, const char *argv[]) {
    @autoreleasepool {
        if (argc < 3) fail(@"usage: simstream <udid> <port> [quality] [scale]");
        NSString *udid = @(argv[1]);
        int port = atoi(argv[2]);
        if (argc > 3) jpegQuality = atof(argv[3]);
        if (argc > 4) frameScale = atof(argv[4]);

        device = findDevice(udid);
        if ([[device valueForKey:@"state"] integerValue] != 3) fail(@"The simulator is not booted");
        screen = findScreen();
        if (!screen) fail(@"No screen found");
        // Follows the lit screen (a foldable's posture) and the interface's orientation.
        [NSThread detachNewThreadWithBlock:^{
            while (true) {
                usleep(300000);
                @autoreleasepool {
                    uint32_t before = shownScreenID;
                    id found = findScreen();
                    // Keep the same proxy while the same screen shows: frames compare its surface's seed.
                    if (found && shownScreenID != before) screen = found;
                }
            }
        }];
        id deviceType = [device valueForKey:@"deviceType"];
        double scale = [[deviceType valueForKey:@"mainScreenScale"] doubleValue];
        if (scale > 0) pointScale = scale;

        NSError *error = nil;
        hidClient = ((id (*)(id, SEL, id, NSError **))objc_msgSend)([NSClassFromString(@"_TtC12SimulatorKit24SimDeviceLegacyHIDClient") alloc],
            NSSelectorFromString(@"initWithDevice:error:"), device, &error);
        if (!hidClient) fail([NSString stringWithFormat:@"HID client: %@", error]);
        hidQueue = dispatch_queue_create("simstream.hid", DISPATCH_QUEUE_SERIAL);
        sendQueue = dispatch_queue_create("simstream.send", DISPATCH_QUEUE_SERIAL);
        ciContext = [CIContext contextWithOptions:@{kCIContextUseSoftwareRenderer: @NO}];

        // Debugging hooks (see the top of this file).
        if (argc > 5 && strcmp(argv[5], "--commands") == 0) {
            NSArray *commands = [NSJSONSerialization JSONObjectWithData:[@(argv[6]) dataUsingEncoding:NSUTF8StringEncoding] options:0 error:nil];
            dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
                for (NSDictionary *command in commands) {
                    handle(command);
                    usleep(80000);
                }
                usleep(500000);
                exit(0);
            });
            dispatch_main();
        }
        if (argc > 5 && strcmp(argv[5], "--frame") == 0) {
            [encodeFrame(currentSurface()) writeToFile:@(argv[6]) atomically:YES];
            return 0;
        }

        int server = socket(AF_INET, SOCK_STREAM, 0);
        int yes = 1;
        setsockopt(server, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(yes));
        struct sockaddr_in address = {.sin_family = AF_INET, .sin_port = htons(port), .sin_addr.s_addr = htonl(INADDR_LOOPBACK)};
        if (bind(server, (struct sockaddr *)&address, sizeof(address)) != 0) fail(@"Port in use");
        listen(server, 1);
        printf("simstream: listening on %d\n", port);
        fflush(stdout);
        // CoreSimulator's remote proxies answer on the main queue: keep it free.
        [NSThread detachNewThreadWithBlock:^{
            while (true) {
                int fd = accept(server, NULL, NULL);
                if (fd < 0) continue;
                // A new viewer replaces the previous one.
                if (clientFd >= 0) shutdown(clientFd, SHUT_RDWR);
                [NSThread detachNewThreadWithBlock:^{ serve(fd); }];
            }
        }];
        dispatch_main();
    }
}
