// swift-tools-version:5.9
// XCTest suite of the Swift renderer, consuming the package `assembleComposeSwiftUIPackage` generates for e2e/shared
// (as an app would). Run by scripts/run_e2e.sh, or: (cd e2e && ./gradlew :shared:assembleComposeSwiftUIPackage) then
// `swift test --package-path e2e/RendererTests`. RECORD_SNAPSHOTS=1 rewrites the reference images.
import PackageDescription

let package = Package(
    name: "RendererTests",
    platforms: [.macOS(.v14)],
    dependencies: [.package(path: "../shared/build/composeSwiftUI/package")],
    targets: [
        .testTarget(
            name: "RendererTests",
            dependencies: [.product(name: "ComposeSwiftUI", package: "package")],
            resources: [.copy("../../Snapshots")]
        ),
    ]
)
