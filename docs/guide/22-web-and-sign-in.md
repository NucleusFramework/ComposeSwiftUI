<!-- group: Apple frameworks -->
# Web and sign-in

> Embed web pages with WebKit's SwiftUI WebView, and sign users in with Sign in with Apple, passkeys, web authentication, the location button and Touch ID.

This chapter covers two integration files: `WebKit.swift` (the SwiftUI `WebView` of iOS 26 / macOS 26) and
`AuthenticationServices.swift`, which also carries CoreLocationUI's `LocationButton` and LocalAuthentication's
`LocalAuthenticationView`. Add the files you use to the app target and register them at launch (see
[Apple frameworks](apple-frameworks.md)):

```swift
@main
struct MyApp: App {
    init() {
        #if os(iOS) || os(macOS) || os(visionOS)
        ComposeIntegrations.registerWebKit()                  // WebKit.swift, iOS 26 / macOS 26
        #endif
        ComposeIntegrations.registerAuthenticationServices()  // every platform
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

| Kotlin API | Framework | Platforms |
|---|---|---|
| `WebView`, `WebViewController`, `Modifier.webViewStyle`, `Modifier.webViewScroll` | WebKit | iOS 26, macOS 26, visionOS |
| `SignInWithAppleButton`, `Modifier.signInWithAppleButtonStyle` | AuthenticationServices | all |
| `Modifier.authorizationRequest` (Apple ID, password) | AuthenticationServices | iOS 16, macOS 13, tvOS 16, watchOS 9 |
| `Modifier.authorizationRequest` (passkeys) | AuthenticationServices | iOS, macOS, visionOS |
| `Modifier.webAuthenticationSession` | AuthenticationServices | iOS 16.4, macOS 13.3, watchOS 9.4 (not tvOS) |
| `LocationButton` | CoreLocationUI | iOS, watchOS |
| `LocalAuthenticationView` | LocalAuthentication | macOS 15 |

## WebView

`WebView` is SwiftUI's own web view, backed by a WebKit `WebPage` that the renderer owns. The simplest use only needs a
URL; changing `url` loads the new page:

```kotlin
WebView(url = "https://developer.apple.com/documentation/webkit", modifier = Modifier.frame(minHeight = 400.0))
```

### WebViewController

To drive the page and observe it, pass a `WebViewController`. Its properties are Compose state, updated by the renderer
as the page loads, so reading them in a composable recomposes it:

| Property / method | WebKit |
|---|---|
| `title`, `url`, `isLoading`, `estimatedProgress` | `WebPage.title`, `.url`, `.isLoading`, `.estimatedProgress` |
| `canGoBack`, `canGoForward` | the back-forward list |
| `load(url)`, `loadHtml(html, baseUrl)` | `load(_:)`, `load(html:baseURL:)` |
| `reload()`, `stopLoading()`, `goBack()`, `goForward()` | the page navigation methods |
| `evaluateJavaScript(script) { result -> }` | `callJavaScript(_:)` |

```kotlin
@Composable
fun Browser() {
    val controller = remember { WebViewController() }
    var address by remember { mutableStateOf("https://www.swift.org") }

    VStack(spacing = 0.0) {
        HStack(modifier = Modifier.padding()) {
            Button("Back") { controller.goBack() }
            TextField("Address", text = address, onTextChange = { address = it })
            Button("Go") { controller.load(address) }
        }
        if (controller.isLoading) {
            ProgressView(value = controller.estimatedProgress)
        }
        WebView(url = "https://www.swift.org", controller = controller)
    }
}
```

The controller attaches when the `WebView` appears; calls made before that are ignored. Keep it in `remember` (or a view
model) so it survives recompositions.

`evaluateJavaScript` returns the script's result converted to a string. A JavaScript error is delivered to the same
callback as its localized message, so return a value you can recognize when you need to tell them apart:

```kotlin
@Composable
fun PageTitleReader(controller: WebViewController) {
    var heading by remember { mutableStateOf<String?>(null) }
    Button("Read heading") {
        controller.evaluateJavaScript("return document.querySelector('h1')?.textContent ?? ''") { heading = it }
    }
    heading?.let { Text(it) }
}
```

`callJavaScript` runs the script as a function body, so use `return` to produce a value.

### Loading local HTML

```kotlin
@Composable
fun HelpPage() {
    val controller = remember { WebViewController() }
    WebView(
        url = null,
        controller = controller,
        modifier = Modifier.onAppear { controller.loadHtml("<h1>Help</h1><p>Generated in Kotlin.</p>") },
    )
}
```

### Behaviors and scrolling

`Modifier.webViewStyle` groups the WebKit view modifiers. Each parameter left `null` keeps the system default:

```kotlin
WebView(
    url = "https://www.swift.org",
    modifier = Modifier.webViewStyle(
        backForwardNavigationGestures = WebViewBehavior.enabled,
        magnificationGestures = WebViewBehavior.disabled,
        linkPreviews = WebViewBehavior.disabled,
        textSelectionEnabled = false,
        contentBackground = Visibility.hidden,
    ),
)
```

| Parameter | SwiftUI modifier |
|---|---|
| `backForwardNavigationGestures` | `webViewBackForwardNavigationGestures(_:)` |
| `magnificationGestures` | `webViewMagnificationGestures(_:)` |
| `linkPreviews` | `webViewLinkPreviews(_:)` |
| `elementFullscreen` | `webViewElementFullscreenBehavior(_:)` |
| `textSelectionEnabled` | `webViewTextSelection(_:)` |
| `contentBackground` | `webViewContentBackground(_:)` |
| `scrollInputBehavior` + `scrollInputKind` (both needed) | `webViewScrollInputBehavior(_:for:)` |

`Modifier.webViewScroll` scrolls the page and observes its scroll geometry:

```kotlin
var offsetY by remember { mutableStateOf(0.0) }

WebView(
    url = "https://www.swift.org",
    modifier = Modifier.webViewScroll(position = ScrollPosition(edge = Edge.top)) { offset, contentSize ->
        offsetY = offset.y
    },
)
```

`position` is one-way: the page scrolls to it when it changes, but user scrolling does not write back. Use
`onScrollGeometryChange` to follow the user.

### Context menu (macOS)

On macOS, `contextMenu` builds the menu shown when the user right-clicks; it receives the URL of the link under the
pointer, or `null`:

```kotlin
WebView(url = "https://www.swift.org", contextMenu = { link ->
    if (link != null) {
        Button("Copy link") { println("copy $link") }
    }
    Button("Reload") { }
})
```

The parameter is ignored on the other platforms.

### Info.plist

WebKit needs no entitlement. Loading plain `http://` pages needs an App Transport Security exception
(`NSAppTransportSecurity`) in Info.plist; a macOS sandboxed app needs the "Outgoing Connections (Client)" capability.

## Sign in with Apple

`SignInWithAppleButton` shows the system button and runs the whole flow; `onCompletion` receives an
`AuthorizationResult`:

```kotlin
class SessionModel {
    var userId by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    fun handle(result: AuthorizationResult) {
        when (result) {
            is AuthorizationResult.AppleId -> {
                userId = result.credential.user
                // Send result.credential.identityToken and authorizationCode to your server to verify them.
            }
            is AuthorizationResult.Failure -> error = result.message
            else -> Unit
        }
    }
}

@Composable
fun SignInScreen(model: SessionModel) {
    VStack(spacing = 12.0, modifier = Modifier.padding()) {
        SignInWithAppleButton(
            onCompletion = model::handle,
            label = SignInWithAppleLabel.`continue`,
            requestedScopes = listOf("fullName", "email"),
            nonce = "a-hashed-nonce-from-your-server",
            modifier = Modifier.signInWithAppleButtonStyle(SignInWithAppleStyle.whiteOutline).frame(height = 50.0),
        )
        model.error?.let { Text(it) }
    }
}
```

- `requestedScopes` takes `"fullName"` and `"email"`. Apple returns the name and email only the first time the user
  authorizes your app; store them then.
- `identityToken` and `authorizationCode` are UTF-8 strings (a JWT and a short-lived code) ready to send to your server.
- `nonce` and `state` are passed to the request unchanged. Send the SHA-256 hash of your nonce, as Apple recommends.
- A user cancellation also arrives as `AuthorizationResult.Failure`.

The app needs the **Sign in with Apple** capability (`com.apple.developer.applesignin` entitlement) on its App ID.

## Authorization requests and passkeys

`Modifier.authorizationRequest` runs SwiftUI's `authorizationController` with several requests at once, the way you
offer "Sign in with Apple, a saved password or a passkey" in one sheet. The requests run each time `trigger` changes
(not when the modifier first appears):

```kotlin
@Composable
fun PasskeySignIn(challenge: String, onSignedIn: (AuthorizationResult) -> Unit) {
    var attempt by remember { mutableStateOf(0) }

    Button(
        "Sign in",
        modifier = Modifier.authorizationRequest(
            trigger = attempt,
            requests = listOf(
                AuthorizationRequest.PasskeyAssertion(relyingParty = "example.com", challenge = challenge),
                AuthorizationRequest.Password,
                AuthorizationRequest.AppleId(requestedScopes = listOf("email")),
            ),
            preferImmediatelyAvailableCredentials = true,
            onResult = onSignedIn,
        ),
    ) { attempt++ }
}
```

The result is one of:

| `AuthorizationResult` | Content |
|---|---|
| `AppleId(credential)` | the `AppleIdCredential`, as with the button |
| `Password(user, password)` | a credential from iCloud Keychain |
| `Passkey(isRegistration, credentialId, rawClientDataJson, authenticatorData, signature, userId, attestationObject)` | base64 values to send to your server for verification |
| `Failure(message)` | an error or a cancellation |

Register a new passkey with `AuthorizationRequest.PasskeyRegistration(relyingParty, challenge, userName, userId)`; the
result is a `Passkey` with `isRegistration = true` and an `attestationObject`. Challenges and user ids are base64
strings generated by your server.

`autoFillAssisted = true` uses `performAutoFillAssistedRequests` (iOS): passkeys are offered in the QuickType bar of a
username field marked `Modifier.textContentType(TextContentType.Username)`, without a sheet.
`preferImmediatelyAvailableCredentials` fails right away, instead of showing the QR-code flow, when no credential is on
the device.

Requirements:

- Passkeys and passwords need the **Associated Domains** capability with a `webcredentials:example.com` entry, and the
  `apple-app-site-association` file on that domain listing your app.
- Apple ID requests need the Sign in with Apple capability.
- Passkey requests are not available on tvOS and watchOS; the other requests are.

## Web authentication sessions

`Modifier.webAuthenticationSession` opens an OAuth / OpenID Connect login page in a secure browser session and
returns the callback URL. Like `authorizationRequest`, it starts when `trigger` changes:

```kotlin
@Composable
fun GitHubLogin(onCode: (String) -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    VStack {
        Button(
            "Sign in with GitHub",
            modifier = Modifier.webAuthenticationSession(
                trigger = attempt,
                url = "https://github.com/login/oauth/authorize?client_id=YOUR_ID&scope=read:user",
                callbackUrlScheme = "myapp",
                ephemeral = true,
            ) { callbackUrl, failure ->
                error = failure
                callbackUrl?.substringAfter("code=", "")?.takeIf { it.isNotEmpty() }?.let(onCode)
            },
        ) { attempt++ }
        error?.let { Text(it) }
    }
}
```

`ephemeral = true` uses a private browser session (`preferredBrowserSession: .ephemeral`) that shares no cookies with
Safari. The custom `callbackUrlScheme` doesn't need to be registered in Info.plist. Not available on tvOS.

## LocationButton

`LocationButton` (CoreLocationUI, iOS and watchOS) grants one-time location access when tapped, without the
authorization alert, then calls `action`:

```kotlin
LocationButton(
    action = { println("Authorized: read the location now") },
    title = LocationButtonTitle.shareCurrentLocation,
    modifier = Modifier.frame(height = 44.0),
)
```

`title = null` shows the icon only. The button only grants the permission: read the location yourself in `action`, with
a `CLLocationManager` created in `iosMain` through `platform.CoreLocation`, or in Swift. As with any location access,
the app needs `NSLocationWhenInUseUsageDescription` in Info.plist.

## LocalAuthenticationView (macOS)

`LocalAuthenticationView` (macOS 15) shows the Touch ID / password button of LocalAuthentication. `onResult` receives
`null` on success, or the error message:

```kotlin
var unlocked by remember { mutableStateOf(false) }

if (unlocked) {
    Text("Secret notes")
} else {
    LocalAuthenticationView(
        reason = "Unlock your notes",
        title = "Unlock",
        onResult = { error -> unlocked = error == null },
    )
}
```

`label` replaces the default title with any view. On iOS, use `LAContext.evaluatePolicy` (through
`platform.LocalAuthentication` in `iosMain`) instead; Face ID then needs `NSFaceIDUsageDescription`.
