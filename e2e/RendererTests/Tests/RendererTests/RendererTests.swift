import AppKit
import ComposeSwiftUI
import SwiftUI
import XCTest

@MainActor
final class RendererTests: XCTestCase {
    override func setUp() async throws {
        ComposeGenerated.setUp()
        E2E.shared.reset()
    }

    // MARK: Node tree and arguments

    func testNodeModelIsUniquePerNodeAndCachesChildren() {
        let host = E2E.shared.host(name: "callbacks")
        host.start()
        defer { host.dispose() }
        let root = NodeModel.of(host.root)
        XCTAssertTrue(root === NodeModel.of(host.root), "one model per node")
        let stack = root.children[0]
        XCTAssertEqual(stack.children.count, 4)
        XCTAssertTrue(stack.children[0] === stack.children[0], "children are cached until the node changes")
    }

    func testArgsDecodeTheKotlinArguments() {
        let host = E2E.shared.host(name: "callbacks")
        host.start()
        defer { host.dispose() }
        let texts = NodeModel.of(host.root).children[0].children.map { $0.args.string(0) }
        XCTAssertEqual(texts, ["Throw in action", "Throw in binding", "Increment", "count=0"])
    }

    func testActionRecomposesSynchronously() {
        let host = E2E.shared.host(name: "callbacks")
        host.start()
        defer { host.dispose() }
        let stack = NodeModel.of(host.root).children[0]
        let before = stack.version
        stack.children[2].args.action(1)()  // Button("Increment"): title, action
        XCTAssertEqual(NodeModel.of(host.root).children[0].children[3].args.string(0), "count=1")
        XCTAssertGreaterThan(stack.children[3].version + stack.version, before, "the changed node was notified")
    }

    func testLongArgumentsKeepTheir64Bits() {
        let a = Args(expr: E2E.shared.longArgs())
        XCTAssertEqual(a.long(0), 9_007_199_254_740_993)
        XCTAssertEqual(a.long(1), Int64.min)
        XCTAssertEqual(UInt64(bitPattern: a.long(2)), UInt64.max, "a UInt64 travels as its bit pattern")
        XCTAssertNotEqual(Int64(a.int(0)), a.long(0), "int() would have truncated it")
    }

    // MARK: Snapshots

    func testSnapshotOfCallbacksScreen() throws {
        try assertSnapshot(named: "callbacks", ComposeView { E2E.shared.host(name: "callbacks") })
    }

    func testSnapshotOfDarkEnvironment() throws {
        try assertSnapshot(named: "environment-dark", ComposeView { E2E.shared.host(name: "environment") }.environment(\.colorScheme, .dark))
    }

    // MARK: Memory

    func testDisposedHostsAreReclaimed() async throws {
        weak var rootModel: NodeModel?
        weak var childModel: NodeModel?
        do {
            let window = NSWindow(contentRect: CGRect(x: 0, y: 0, width: 300, height: 300), styleMask: [.borderless], backing: .buffered, defer: false)
            window.isReleasedWhenClosed = false
            var host: ComposeSwiftUIHost?
            let view = NSHostingView(rootView: ComposeView { let h = E2E.shared.host(name: "callbacks"); host = h; return h })
            window.contentView = view
            view.layoutSubtreeIfNeeded()
            try await Task.sleep(nanoseconds: 200_000_000)
            let h = try XCTUnwrap(host)
            E2E.shared.trackHost(host: h)
            rootModel = NodeModel.of(h.root)
            childModel = NodeModel.of(h.root).children.first
            XCTAssertNotNil(childModel)
            host = nil
            window.contentView = nil
            window.close()
        }
        // HostHolder disposes the host on the next main-queue turn
        for _ in 0..<20 {
            try await Task.sleep(nanoseconds: 50_000_000)
            E2E.shared.collectGarbage()
            if rootModel == nil && childModel == nil && E2E.shared.trackedHostsAlive() == 0 { break }
        }
        XCTAssertNil(childModel, "Swift models of the nodes are released")
        XCTAssertNil(rootModel, "the root model is released")
        XCTAssertEqual(E2E.shared.trackedHostsAlive(), 0, "the Kotlin host is garbage collected")
    }

    // MARK: Helpers

    private func assertSnapshot(named name: String, _ view: some View, file: StaticString = #filePath, line: UInt = #line) throws {
        let renderer = ImageRenderer(content: view.frame(width: 320, height: 240).background(Color(white: 0.5)))
        renderer.scale = 2
        let image = try XCTUnwrap(renderer.cgImage, "rendering failed", file: file, line: line)
        let reference = URL(fileURLWithPath: #filePath).deletingLastPathComponent().appendingPathComponent("../../Snapshots/\(name).png").standardized
        if ProcessInfo.processInfo.environment["RECORD_SNAPSHOTS"] != nil || !FileManager.default.fileExists(atPath: reference.path) {
            try png(image).write(to: reference)
            if ProcessInfo.processInfo.environment["RECORD_SNAPSHOTS"] == nil {
                XCTFail("No reference for \(name): recorded \(reference.path)", file: file, line: line)
            }
            return
        }
        let expected = try XCTUnwrap(NSImage(contentsOf: reference)?.cgImage(forProposedRect: nil, context: nil, hints: nil))
        let ratio = difference(image, expected)
        if ratio > 0.01 {
            let actualURL = reference.deletingPathExtension().appendingPathExtension("actual.png")
            try png(image).write(to: actualURL)
            XCTFail("\(name) differs from its reference by \(Int(ratio * 100))% of pixels; see \(actualURL.path)", file: file, line: line)
        }
    }

    private func png(_ image: CGImage) throws -> Data {
        try XCTUnwrap(NSBitmapImageRep(cgImage: image).representation(using: .png, properties: [:]))
    }

    /// Fraction of pixels whose color differs by more than a small tolerance (antialiasing).
    private func difference(_ a: CGImage, _ b: CGImage) -> Double {
        guard a.width == b.width, a.height == b.height else { return 1 }
        func pixels(_ image: CGImage) -> [UInt8] {
            var data = [UInt8](repeating: 0, count: image.width * image.height * 4)
            let context = CGContext(data: &data, width: image.width, height: image.height, bitsPerComponent: 8, bytesPerRow: image.width * 4,
                                    space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
            context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
            return data
        }
        let pa = pixels(a), pb = pixels(b)
        var differing = 0
        for p in stride(from: 0, to: pa.count, by: 4) where (0..<3).contains(where: { abs(Int(pa[p + $0]) - Int(pb[p + $0])) > 16 }) {
            differing += 1
        }
        return Double(differing) / Double(a.width * a.height)
    }
}
