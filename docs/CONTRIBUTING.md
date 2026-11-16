# Contributing

## Project layout

| Path | Contents |
|---|---|
| `runtime/` | Kotlin Multiplatform library: node tree, Compose applier, host, `Modifier`, `viewModel { }` |
| `runtime/src/generated/` | Generated Kotlin bindings. Don't edit by hand. |
| `runtime/src/extras/` | Hand-written Kotlin APIs (`NavigationStack(path)`, `EnvironmentReader`, gestures…) |
| `swift/ComposeSwiftUI/` | Swift renderer. `Generated/` is generated; the other files are hand-written. |
| `gradle-plugin/` | The `dev.nucleusframework.compose-swiftui` Gradle plugin |
| `generator/` | Python scripts that generate the bindings from the SwiftUI SDK |
| `bench/` | 50 screens written twice, and the benchmark app |
| `samples/kmp-app/` | A standalone KMP app that uses the plugin |

## Regenerating the bindings

The generator reads Apple's symbol graphs for SwiftUI and SwiftUICore, emits the Kotlin API and the Swift renderer,
then compiles both. When the Swift compiler rejects an API on any platform, `verify.py` records the error in
`generator/rejected.json`, drops that API and regenerates. It loops until everything compiles.

```bash
# 1. Symbol graphs of the installed Xcode SDKs (iOS, macOS, tvOS, watchOS)
generator/extract.sh /tmp/sg

# 2. Runtime-only frameworks, used to typecheck the generated Swift
./gradlew :runtime:linkDebugFrameworkMacosArm64 :runtime:linkDebugFrameworkIosSimulatorArm64 \
  :runtime:linkDebugFrameworkTvosSimulatorArm64 :runtime:linkDebugFrameworkWatchosSimulatorArm64 -Pcsui.coreOnly=true
# copy each runtime/build/bin/<target>/debugFramework/BenchShared.framework to /tmp/corefw/<platform>/
# (platform = macos, ios, tvos or watchos)

# 3. Generate and verify (Swift on 4 platforms, then Kotlin)
python3 -I generator/verify.py /tmp/sg . /tmp/corefw --kotlin

# 4. Reference docs
python3 -I generator/api_index.py .                                 # docs/API.md
python3 -I generator/overlays.py /tmp/sg .                          # generator/coverage_overlays.json
python3 -I generator/coverage_report.py <apple-doc-index.json> .    # docs/COVERAGE.md
```

`extract.sh` also extracts the SwiftUI overlays of other frameworks (MapKit, Charts, StoreKit…) into
`/tmp/sg/overlays`. They aren't generated: their Kotlin APIs are hand-written (`runtime/src/extras/.../MapKit.kt`,
`Charts.kt`, `StoreKit.kt`) with one opt-in Swift renderer each in `swift/ComposeSwiftUIIntegrations`, and
`generator/integrations.json` maps each overlay symbol to its Kotlin API or gives the reason it doesn't apply.
`verify.py` typechecks the integrations with the renderer; the Gradle plugin writes them to
`iosApp/ComposeSwiftUIIntegrations`.

`<apple-doc-index.json>` is the documentation index from
`https://developer.apple.com/tutorials/data/index/swiftui`.

## Running the benchmark

Each screen lives in two files: `bench/shared/.../screens/ScreenNN.kt` (Compose SwiftUI) and
`bench/app/Screens/ScreenNNView.swift` (plain SwiftUI). Both read the same Kotlin `ViewModel`.
[bench/BENCH_GUIDE.md](../bench/BENCH_GUIDE.md) covers the rules for writing a screen.

```bash
# One screen, debug build, iOS simulator and macOS: pixel diff and timings
python3 -I scripts/check_screens.py 7

# All 50 screens, release builds; writes ../bench-results/full/report.html
scripts/run_full_bench.sh

# Scenarios: long lists, one-row change, animations, against plain SwiftUI with native @Observable models
scripts/run_scenarios.sh -sizes 1000,10000 -lazy
```

The iOS runs need a booted simulator. Both scripts take `SIM=<udid>` to pick one and `PLATFORMS=ios` to skip macOS.

To typecheck the Swift renderer on the four platforms after changing it:

```bash
scripts/typecheck_swift.sh /tmp/corefw      # the frameworks from step 2 of "Regenerating the bindings"
```

## Publishing a release

Pushing a tag `vX.Y.Z` runs two workflows:

- `publish-maven.yaml` publishes `dev.nucleusframework:compose-swiftui` to Maven Central.
- `publish-plugin.yaml` publishes the `dev.nucleusframework.compose-swiftui` plugin to the Gradle Plugin Portal.

Both use the tag as the version, and `release.yaml` creates the GitHub release. The publish workflows need these
repository secrets:

| Secret | Used for |
|---|---|
| `MAVENCENTRALUSERNAME`, `MAVENCENTRALPASSWORD` | Sonatype Central Portal token |
| `SIGNINGINMEMORYKEY`, `SIGNINGKEYID`, `SIGNINGPASSWORD` | GPG signing of the Maven artifacts |
| `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET` | Gradle Plugin Portal API key |

The sample builds the library and the plugin from source, so you can open it directly. To test the published
artifacts instead, publish them to Maven Local:

```bash
./gradlew :runtime:publishToMavenLocal --no-configuration-cache
./gradlew --project-dir gradle-plugin publishToMavenLocal

```

## Documentation

The guide lives in `docs/guide/NN-slug.md` (first lines: `<!-- group: … -->`, `# Title`, a blank line, `> abstract`).
Run `python3 scripts/check_docs.py` after editing it: every ```` ```kotlin ```` block must compile (fence a block as
```` ```kotlin skip ```` only for iOS-only code). `python3 generator/docsite.py . site` builds the website.
