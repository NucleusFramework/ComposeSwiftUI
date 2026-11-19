// Presses a hardware button of an iOS simulator (lock, volume) the way Xcode's device window does: through CoreDevice,
// whose button capability reaches buttons that SimulatorKit's Indigo messages don't on current runtimes.
//
// Usage: simbutton <udid> <usage page> <usage code> (hexadecimal HID consumer usages: c 30 is the lock button)

import CoreDevice
import Foundation

let arguments = CommandLine.arguments
guard arguments.count == 4, let udid = UUID(uuidString: arguments[1]),
      let page = Int(arguments[2], radix: 16), let code = Int(arguments[3], radix: 16) else {
    FileHandle.standardError.write(Data("usage: simbutton <udid> <usage page> <usage code>\n".utf8))
    exit(2)
}
let manager = DeviceManager(serviceConnection: CoreDeviceService.sharedConnection, allowedDeviceVisibilityClasses: [.default, .simulators])
await manager.awaitFullInitialization()
guard let device = manager.devices[udid] else {
    FileHandle.standardError.write(Data("simbutton: no simulator \(udid)\n".utf8))
    exit(1)
}
do {
    let buttons = try await device.getImplementation(for: .hid.button)
    try buttons.sendCustomButton(usagePage: page, usageCode: code, state: .down)
    try await Task.sleep(nanoseconds: 120_000_000)
    try buttons.sendCustomButton(usagePage: page, usageCode: code, state: .up)
    buttons.sendBarrier()
    // Delivered asynchronously: leave time before the connection closes.
    try await Task.sleep(nanoseconds: 500_000_000)
} catch {
    FileHandle.standardError.write(Data("simbutton: \(error)\n".utf8))
    exit(1)
}
