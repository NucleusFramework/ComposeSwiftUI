# Versioning, deprecation and compatibility

## Semantic versioning

From 1.0.0, Compose SwiftUI follows [Semantic Versioning](https://semver.org) strictly; until then (0.x), a minor
release may still break APIs, always listed under *Breaking* in the CHANGELOG. The library
(`dev.nucleusframework:compose-swiftui`), the Gradle plugin (`dev.nucleusframework.compose-swiftui`), the Swift
renderer it writes and the IDE plugin always share one version, read from [`VERSION`](../VERSION).

| Change | Version |
|---|---|
| A public API removed or changed incompatibly (Kotlin ABI, Swift renderer API, plugin DSL, minimum OS or Xcode) | major |
| New APIs, new bindings from a new SDK, deprecations | minor |
| Fixes with no API change | patch |

What counts as public API:

- Kotlin: every declaration of the runtime's ABI dump ([`runtime/api`](../runtime/api)), except those annotated
  `@InternalComposeSwiftUIApi` (for the generated bindings and tooling) and `@ExperimentalComposeSwiftUIApi`
  (may change in a minor release). `checkKotlinAbi` fails the build on any other change; an intended change is
  recorded with `updateKotlinAbi` in the same pull request, and is a major change unless it only adds declarations.
- Swift: the `public` declarations of the renderer (`ComposeView`, `ComposeRegistry`, `ComposeIntegrations`, `Args`…).
- Gradle: the `composeSwiftUI { }` DSL and the task names.

Regenerating the bindings from a new SDK only adds APIs. An API Apple removes or renames stays, deprecated, until the
next major version.

## Deprecation policy

1. An API is deprecated (`@Deprecated` with a `ReplaceWith` in Kotlin, `@available(*, deprecated)` in Swift) in a
   minor release, with its replacement available in the same release and named in the CHANGELOG.
2. It stays for at least **two minor releases and three months**, whichever is longer, at `WARNING` level, then one
   minor release at `ERROR` level.
3. It is removed only in a major release.

## Compatibility matrix

| Compose SwiftUI | Kotlin | Compose runtime | AndroidX Lifecycle | Xcode (build) | Minimum OS |
|---|---|---|---|---|---|
| 0.10.x | 2.4.20 | 1.11.4 | 2.11.0 | 27.x (N), 26.x (N-1) | iOS 17, macOS 14, tvOS 17, watchOS 10 |
| 0.9.x | 2.4.20 | 1.10.4 | 2.10.0 | 27.2 | iOS 17, macOS 14, tvOS 17, watchOS 10 |

- **Kotlin:** the version the library is built with. A newer Kotlin patch release works; a newer minor release works
  once the Compose compiler of that version supports the Compose runtime above.
- **Xcode:** the renderer is built with the newest Xcode (N) and still compiles with the previous major (N-1): the
  APIs of the newest SDKs sit behind `#if compiler(>=…)` and render nothing with N-1 (`scripts/check_n1.py` checks it
  on every pull request).
- **Targets:** `iosArm64`, `iosSimulatorArm64`, `macosArm64`, `tvosArm64`, `tvosSimulatorArm64`, `watchosArm64`,
  `watchosDeviceArm64`, `watchosSimulatorArm64`. Intel targets (`iosX64`, `macosX64`) were dropped in 0.10.0, with the
  Compose runtime and AndroidX Lifecycle.
