// WidgetKit and ActivityKit view APIs for Compose SwiftUI content shown in widgets and Live Activities (runtime
// AppFrameworks.kt). Add this file to the target that renders the content and call
// `ComposeIntegrations.registerWidgetKit()`. The widget's `@main` entry point stays Swift (WidgetKit requires it), but
// its timeline and views can come from Kotlin:
//
//     @main struct WeatherWidget: Widget {
//         var body: some WidgetConfiguration {
//             StaticConfiguration(kind: "weather", provider: ComposeTimelineProvider { family in WeatherKt.timeline(family: family) }) { entry in
//                 ComposeWidgetView(entry) { input, family in WeatherKt.widget(input: input, family: family) }
//             }
//             .configurationDisplayName("Weather")
//         }
//     }
#if os(iOS) || os(macOS) || os(watchOS)
import SwiftUI
import WidgetKit

public extension ComposeIntegrations {
    @MainActor static func registerWidgetKit() {
        ComposeRegistry.modifiers[200_343] = { v, a, _ in WidgetModifiers.style(v, a) }
        #if !os(macOS)
        ComposeRegistry.modifiers[200_344] = { v, a, _ in
            if let text = a.stringOpt(0) { return AnyView(v.widgetLabel(text)) }
            return AnyView(v.widgetLabel { a.slot(1) })
        }
        #endif
        #if os(iOS)
        ComposeRegistry.modifiers[200_345] = { view, a, _ in
            var v = view
            if !a.isNil(0) { v = AnyView(v.activityBackgroundTint(d_Color(a.expr(0)))) }
            if !a.isNil(1) { v = AnyView(v.activitySystemActionForegroundColor(d_Color(a.expr(1)))) }
            if a.bool(2) { v = AnyView(v.dynamicIsland(verticalPlacement: .belowIfTooWide)) }
            return v
        }
        #endif
        if #available(iOS 18.0, macOS 26.0, watchOS 26.0, *) {
            ComposeRegistry.modifiers[200_346] = { view, a, _ in
                var v = view
                if let s = a.stringOpt(0) { v = AnyView(v.controlWidgetStatus(s)) }
                if let s = a.stringOpt(1) { v = AnyView(v.controlWidgetActionHint(s)) }
                return v
            }
        }
        ComposeRegistry.views[200_341] = { m in ModChain.apply(AccessoryWidgetBackground(), m, 0) }
        #if os(watchOS)
        ComposeRegistry.views[200_342] = { m in ModChain.apply(KotlinAccessoryWidgetGroup(m: m), m, 0) }
        #endif
        if #available(iOS 18.0, macOS 15.0, watchOS 11.0, *) {
            ComposeRegistry.views[200_343] = { m in
                let a = m.args
                let modes: [WidgetAccentedRenderingMode] = [.accented, .desaturated, .accentedDesaturated, .fullColor]
                let image = a.stringOpt(1).map { Image(systemName: $0) } ?? Image(a.string(0))
                return ModChain.apply(image.widgetAccentedRenderingMode(modes[min(max(a.int(2), 0), 3)]), m, 0)
            }
        }
    }
}

@MainActor private enum WidgetModifiers {
    static func style(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        if let b = a.boolOpt(0) { v = AnyView(v.widgetAccentable(b)) }
        if let s = a.stringOpt(1) { v = AnyView(v.widgetURL(URL(string: s))) }
        #if !os(macOS)
        if #available(iOS 17.0, watchOS 10.0, *), let b = a.boolOpt(2) { v = AnyView(v.widgetCurvesContent(b)) }
        #endif
        if #available(iOS 17.0, macOS 14.0, watchOS 10.0, *), let s = dp_ShapeStyle(a.expr(3)) {
            v = AnyView(v.containerBackground(AnyShapeStyle(s), for: .widget))
        }
        #if os(watchOS)
        if #available(watchOS 11.0, *), let i = a.intOpt(4) {
            let styles: [AccessoryWidgetGroupStyle] = [.automatic, .circular, .roundedSquare]
            v = AnyView(v.accessoryWidgetGroupStyle(styles[min(max(i, 0), 2)]))
        }
        #endif
        return v
    }
}

/// A timeline entry carrying the Kotlin input of the widget content.
public struct ComposeWidgetEntry: TimelineEntry {
    public let date: Date
    public let input: String
    public let relevance: TimelineEntryRelevance?
}

/// A `TimelineProvider` backed by Kotlin: [timeline] receives the widget family (`WidgetFamily` description, e.g.
/// "systemSmall") and returns a Kotlin `WidgetTimeline`. The placeholder and snapshot use its first entry.
public struct ComposeTimelineProvider: TimelineProvider {
    let timeline: @MainActor (String) -> WidgetTimeline

    public init(_ timeline: @escaping @MainActor (String) -> WidgetTimeline) { self.timeline = timeline }

    @MainActor private func entries(_ context: Context) -> (WidgetTimeline, [ComposeWidgetEntry]) {
        let t = timeline("\(context.family)")
        let list = t.entries.map { e in
            ComposeWidgetEntry(date: Date(timeIntervalSince1970: e.date), input: e.input,
                               relevance: e.relevance.map { TimelineEntryRelevance(score: Float(truncating: $0)) })
        }
        return (t, list)
    }

    public func placeholder(in context: Context) -> ComposeWidgetEntry {
        MainActor.assumeIsolated { entries(context).1.first } ?? ComposeWidgetEntry(date: Date(), input: "", relevance: nil)
    }

    public func getSnapshot(in context: Context, completion: @escaping (ComposeWidgetEntry) -> Void) {
        Task { @MainActor in completion(entries(context).1.first ?? ComposeWidgetEntry(date: Date(), input: "", relevance: nil)) }
    }

    public func getTimeline(in context: Context, completion: @escaping (Timeline<ComposeWidgetEntry>) -> Void) {
        Task { @MainActor in
            let (t, list) = entries(context)
            let policy: TimelineReloadPolicy = t.never ? .never : t.reloadAfter.map { .after(Date(timeIntervalSince1970: Double(truncating: $0))) } ?? .atEnd
            completion(Timeline(entries: list, policy: policy))
        }
    }
}

/// The Compose content of a widget entry: [makeHost] gets the entry's input and the widget family description.
public struct ComposeWidgetView: View {
    let entry: ComposeWidgetEntry
    let makeHost: (String, String) -> ComposeSwiftUIHost
    @Environment(\.widgetFamily) private var family

    public init(_ entry: ComposeWidgetEntry, _ makeHost: @escaping (String, String) -> ComposeSwiftUIHost) {
        self.entry = entry
        self.makeHost = makeHost
    }

    public var body: some View {
        let family = "\(family)"
        ComposeView(input: "\(family)|\(entry.input)") { makeHost(entry.input, family) }
    }
}

#if os(watchOS)
private struct KotlinAccessoryWidgetGroup: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        if #available(watchOS 11.0, *) {
            if let label = a.slotModel(3) {
                AccessoryWidgetGroup { NodeView(model: label) } content: { ChildrenView(m) }
            } else if let symbol = a.stringOpt(1) {
                AccessoryWidgetGroup(a.string(0), systemImage: symbol) { ChildrenView(m) }
            } else if let image = a.stringOpt(2) {
                AccessoryWidgetGroup(a.string(0), image: ImageResource(name: image, bundle: .main)) { ChildrenView(m) }
            } else {
                AccessoryWidgetGroup(a.string(0)) { ChildrenView(m) }
            }
        }
    }
}
#endif
#endif
