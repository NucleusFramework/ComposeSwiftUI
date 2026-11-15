// MapKit for Compose SwiftUI: renders the Kotlin `Map`, map content, map controls and `map*` modifiers
// (runtime MapKit.kt). Add this file to the app target and call `ComposeIntegrations.registerMapKit()` at launch.
import SwiftUI
import MapKit

public extension ComposeIntegrations {
    @MainActor static func registerMapKit() {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return }
        ComposeRegistry.views[200_000] = { m in ModChain.apply(KotlinMap(m: m), m, 0) }
        ComposeRegistry.views[200_007] = { m in ModChain.apply(MapControl(m: m, kind: .compass), m, 0) }
        ComposeRegistry.views[200_008] = { m in ModChain.apply(MapControl(m: m, kind: .scale), m, 0) }
        ComposeRegistry.views[200_009] = { m in ModChain.apply(MapControl(m: m, kind: .userLocation), m, 0) }
        ComposeRegistry.views[200_010] = { m in ModChain.apply(MapControl(m: m, kind: .pitch), m, 0) }
        #if os(iOS) || os(macOS)
        ComposeRegistry.views[200_011] = { m in ModChain.apply(KotlinLookAroundPreview(m: m), m, 0) }
        #endif
        ComposeRegistry.views[200_012] = { m in ModChain.apply(KotlinMapReader(m: m), m, 0) }

        ComposeRegistry.modifiers[200_000] = { v, a, _ in AnyView(v.mapStyle(MapKitValues.style(a.list(0)))) }
        ComposeRegistry.modifiers[200_001] = { v, a, _ in AnyView(v.mapControls { SlotView(model: a.slotModel(0)) }) }
        ComposeRegistry.modifiers[200_002] = { v, a, _ in AnyView(v.mapControlVisibility(d_Visibility(a.expr(0)) ?? .automatic)) }
        ComposeRegistry.modifiers[200_003] = { v, a, _ in
            let report = a.callback(1, box: { (l: [Double]) -> Any? in l.map { KotlinDouble(value: $0) } as NSArray })
            return AnyView(v.onMapCameraChange(frequency: a.int(0) == 1 ? .continuous : .onEnd) { ctx in
                let c = ctx.camera, r = ctx.region
                report([c.centerCoordinate.latitude, c.centerCoordinate.longitude, c.distance, c.heading, c.pitch,
                        r.center.latitude, r.center.longitude, r.span.latitudeDelta, r.span.longitudeDelta])
            })
        }
        ComposeRegistry.modifiers[200_009] = { v, a, _ in AnyView(v.modifier(MapScopeModifier(namespace: Int64(a.int(0))))) }
        ComposeRegistry.modifiers[200_010] = { v, a, _ in
            AnyView(v.modifier(MapKeyframes(trigger: a.hashable(0), frames: a.list(1).map { MapKitValues.numbers($0) })))
        }
        #if os(iOS) || os(macOS) || os(visionOS)
        if #available(iOS 18.0, macOS 15.0, *) {
            ComposeRegistry.modifiers[200_004] = { v, a, _ in
                AnyView(v.mapItemDetailSheet(isPresented: a.boolBinding(0), item: a.isNil(2) ? nil : MapKitValues.item(a.list(2)),
                                             displaysMap: a.bool(3)))
            }
            ComposeRegistry.modifiers[200_005] = { v, a, _ in
                let item = a.isNil(2) ? nil : MapKitValues.item(a.list(2))
                if let edge = d_Edge(a.expr(4)) {
                    return AnyView(v.mapItemDetailPopover(isPresented: a.boolBinding(0), item: item, displaysMap: a.bool(3), arrowEdge: edge))
                }
                return AnyView(v.mapItemDetailPopover(isPresented: a.boolBinding(0), item: item, displaysMap: a.bool(3)))
            }
            #if os(iOS)
            ComposeRegistry.modifiers[200_007] = { v, a, _ in
                AnyView(v.mapFeatureSelectionAccessory(a.isNil(0) ? nil : MapKitValues.accessory(a.list(0))))
            }
            #endif
        }
        #endif
        #if os(iOS)
        ComposeRegistry.modifiers[200_006] = { v, a, _ in AnyView(v.modifier(LookAroundViewerModifier(a: a))) }
        ComposeRegistry.modifiers[200_008] = { v, a, _ in
            let disabled = a.list(0).map { ($0 as? NSNumber)?.boolValue ?? false }
            return AnyView(v.mapFeatureSelectionDisabled { feature in
                let i = feature.kind == .territory ? 1 : feature.kind == .physicalFeature ? 2 : 0
                return i < disabled.count && disabled[i]
            })
        }
        #endif
    }
}

// MARK: - Values

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@MainActor enum MapKitValues {
    static func numbers(_ v: Any?) -> [Double] {
        (v as? [Any] ?? []).map { ($0 as? NSNumber)?.doubleValue ?? 0 }
    }

    static func num(_ l: [Any], _ i: Int) -> Double { i < l.count ? (l[i] as? NSNumber)?.doubleValue ?? 0 : 0 }

    static func coordinate(_ l: [Any], _ i: Int = 0) -> CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: num(l, i), longitude: num(l, i + 1))
    }

    static func coordinates(_ l: [Any]) -> [CLLocationCoordinate2D] {
        stride(from: 0, to: l.count - 1, by: 2).map { coordinate(l, $0) }
    }

    static func region(_ l: [Any], _ i: Int = 0) -> MKCoordinateRegion {
        MKCoordinateRegion(center: coordinate(l, i), span: MKCoordinateSpan(latitudeDelta: num(l, i + 2), longitudeDelta: num(l, i + 3)))
    }

    static func camera(_ l: [Any], _ i: Int = 0) -> MapCamera {
        MapCamera(centerCoordinate: coordinate(l, i), distance: num(l, i + 2), heading: num(l, i + 3), pitch: num(l, i + 4))
    }

    static func item(_ l: [Any]) -> MKMapItem {
        let c = coordinate(l)
        let item: MKMapItem
        if #available(iOS 26.0, macOS 26.0, tvOS 26.0, watchOS 26.0, *) {
            item = MKMapItem(location: CLLocation(latitude: c.latitude, longitude: c.longitude), address: nil)
        } else {
            item = MKMapItem(placemark: MKPlacemark(coordinate: c))
        }
        item.name = l.count > 2 ? l[2] as? String : nil
        return item
    }

    static func position(_ l: [Any]) -> MapCameraPosition {
        switch Int(num(l, 0)) {
        case 1: return .region(region(l, 1))
        case 2: return .camera(camera(l, 1))
        case 3:
            let fallback = l.count > 2 ? position(l[2] as? [Any] ?? []) : .automatic
            return .userLocation(followsHeading: (l[1] as? NSNumber)?.boolValue ?? false, fallback: fallback)
        case 4: return .item(item(l[1] as? [Any] ?? []), allowsAutomaticPitch: (l[2] as? NSNumber)?.boolValue ?? true)
        case 5: return .rect(MKMapRect(x: num(l, 1), y: num(l, 2), width: num(l, 3), height: num(l, 4)))
        default: return .automatic
        }
    }

    static func pointsOfInterest(_ mode: Int, _ names: [Any]) -> PointOfInterestCategories {
        let categories = names.compactMap { $0 as? String }.map { MKPointOfInterestCategory(rawValue: $0) }
        switch mode {
        case 1: return .excludingAll
        case 2: return .including(categories)
        case 3: return .excluding(categories)
        default: return .all
        }
    }

    static func style(_ l: [Any]) -> MapStyle {
        let elevation: MapStyle.Elevation = [.automatic, .flat, .realistic][min(max(Int(num(l, 1)), 0), 2)]
        let pois = pointsOfInterest(Int(num(l, 3)), l.count > 4 ? l[4] as? [Any] ?? [] : [])
        let traffic = l.count > 5 ? (l[5] as? NSNumber)?.boolValue ?? false : false
        switch Int(num(l, 0)) {
        case 1: return .imagery(elevation: elevation)
        case 2: return .hybrid(elevation: elevation, pointsOfInterest: pois, showsTraffic: traffic)
        default:
            return .standard(elevation: elevation, emphasis: num(l, 2) == 1 ? .muted : .automatic, pointsOfInterest: pois, showsTraffic: traffic)
        }
    }

    #if os(iOS)
    @available(iOS 18.0, *)
    static func accessory(_ l: [Any]) -> MapItemDetailSelectionAccessoryStyle {
        switch Int(num(l, 0)) {
        case 1:
            let callouts: [MapItemDetailSelectionAccessoryStyle.CalloutStyle] = [.automatic, .full, .compact]
            return .callout(callouts[min(max(Int(num(l, 1)), 0), 2)])
        case 2: return .sheet
        case 3: return .caption
        default: return .automatic
        }
    }
    #endif
}

/// `MapContentStyle` from Kotlin: tint, fill, stroke, annotation titles, overlay level, selection accessory.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@MainActor struct MapContentStyleValues {
    var tint: AnyShapeStyle?
    var foreground: AnyShapeStyle?
    var stroke: AnyShapeStyle?
    var lineWidth: CGFloat?
    var strokeStyle: StrokeStyle?
    var titles: Visibility?
    var subtitles: Visibility?
    var level: MKOverlayLevel?
    var accessory: [Any]?

    init(_ l: [Any]) {
        func style(_ i: Int) -> AnyShapeStyle? { i < l.count ? dp_ShapeStyle(l[i] as? SExpr).map { AnyShapeStyle($0) } : nil }
        tint = style(0)
        foreground = style(1)
        stroke = style(2)
        lineWidth = l.count > 3 ? (l[3] as? NSNumber).map { CGFloat($0.doubleValue) } : nil
        strokeStyle = l.count > 4 ? d_StrokeStyle(l[4] as? SExpr) : nil
        titles = l.count > 5 ? d_Visibility(l[5] as? SExpr) : nil
        subtitles = l.count > 6 ? d_Visibility(l[6] as? SExpr) : nil
        level = l.count > 7 ? (l[7] as? NSNumber).map { $0.intValue == 1 ? .aboveLabels : .aboveRoads } : nil
        accessory = l.count > 8 ? l[8] as? [Any] : nil
    }
}

// MARK: - Map

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct KotlinMap: View {
    let m: NodeModel
    @State private var position: MapCameraPosition = .automatic
    @State private var applied: [Double] = []
    #if os(iOS)
    @State private var feature: MapFeature?
    #endif
    @Environment(\.composeNamespaces) private var namespaces

    private static func flatten(_ v: Any?) -> [Double] {
        if let n = v as? NSNumber { return [n.doubleValue] }
        if let l = v as? [Any] { return l.flatMap(flatten) }
        return [.nan]
    }

    var body: some View {
        let _ = m.version
        let a = m.args
        let key = a.isNil(0) ? [] : Self.flatten(a.list(0))
        let bounds = a.isNil(2) ? nil : Self.bounds(a.list(2))
        let modes = MapInteractionModes(rawValue: a.int(3))
        let scope = a.isNil(9) ? nil : namespaces[Int64(a.int(9))]
        map(a, bounds: bounds, modes: modes, scope: scope)
            .onChange(of: key, initial: true) { _, k in
                // apply Kotlin's position unless it is the one this map just reported
                guard !k.isEmpty, k != applied else { return }
                applied = k
                position = MapKitValues.position(m.args.list(0))
            }
            .onMapCameraChange(frequency: .onEnd) { ctx in
                guard !m.args.isNil(1) else { return }
                let r = ctx.region
                let values = [1, r.center.latitude, r.center.longitude, r.span.latitudeDelta, r.span.longitudeDelta]
                applied = values
                m.args.callback(1, box: { (l: [Double]) -> Any? in l.map { KotlinDouble(value: $0) } as NSArray })(values)
            }
    }

    private static func bounds(_ l: [Any]) -> MapCameraBounds {
        let minD = l.count > 1 ? (l[1] as? NSNumber)?.doubleValue : nil
        let maxD = l.count > 2 ? (l[2] as? NSNumber)?.doubleValue : nil
        if let r = l.first as? [Any] {
            return MapCameraBounds(centerCoordinateBounds: MapKitValues.region(r), minimumDistance: minD, maximumDistance: maxD)
        }
        return MapCameraBounds(minimumDistance: minD, maximumDistance: maxD)
    }

    @ViewBuilder
    private func map(_ a: Args, bounds: MapCameraBounds?, modes: MapInteractionModes, scope: Namespace.ID?) -> some View {
        #if os(iOS)
        if !a.isNil(6) {
            // point-of-interest selection
            let report = a.callback(6, box: { (f: MapFeature?) -> Any? in
                guard let f else { return NSNull() }
                let kind = f.kind == .territory ? 1 : f.kind == .physicalFeature ? 2 : 0
                return [f.title as Any? ?? NSNull(), KotlinDouble(value: f.coordinate.latitude),
                        KotlinDouble(value: f.coordinate.longitude), KotlinInt(value: Int32(kind))] as NSArray
            })
            let selection = Binding<MapFeature?>(get: { feature }, set: { feature = $0; report($0) })
            Map(position: $position, bounds: bounds, interactionModes: modes, selection: selection, scope: scope) {
                MapContentList(model: m)
            }
            .modifier(FeatureContent(slot: a.slotModel(7)))
        } else {
            Map(position: $position, bounds: bounds, interactionModes: modes, selection: a.optionalHashableBinding(4), scope: scope) {
                MapContentList(model: m)
            }
        }
        #else
        // point-of-interest selection is iOS-only
        Map(position: $position, bounds: bounds, interactionModes: modes, selection: a.optionalHashableBinding(4), scope: scope) {
            MapContentList(model: m)
        }
        #endif
    }
}

#if os(iOS)
@available(iOS 17.0, *)
private struct FeatureContent: ViewModifier {
    let slot: NodeModel?

    func body(content: Content) -> some View {
        if let slot {
            content.mapFeatureSelectionContent { _ in MapContentList(model: slot) }
        } else {
            content
        }
    }
}
#endif

// MARK: - Map content

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@MainActor private struct MapContentList: MapContent {
    let model: NodeModel

    var body: some MapContent {
        let _ = model.version
        ForEach(model.children, id: \.nodeId) { child in
            MapContentNode(model: child)
        }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@MainActor private struct MapContentNode: MapContent {
    let model: NodeModel

    var body: some MapContent {
        let _ = model.version
        let a = model.args
        switch model.kind {
        case 200_001: styled(marker(a), a, style: 7, tag: 6)
        case 200_002: styled(annotation(a), a, style: 6, tag: 5)
        case 200_003:
            styled(MapCircle(center: MapKitValues.coordinate(a.list(0)), radius: a.double(1)), a, style: 3, tag: 2)
        case 200_004:
            styled(MapPolyline(coordinates: MapKitValues.coordinates(a.list(0)), contourStyle: a.int(1) == 1 ? .geodesic : .straight), a, style: 3, tag: 2)
        case 200_005:
            styled(MapPolygon(coordinates: MapKitValues.coordinates(a.list(0))), a, style: 2, tag: 1)
        case 200_006: tagged(userAnnotation(a), a, tag: 1)
        default:
            // a Kotlin group (loop, if, key) of map content
            MapContentList(model: model)
        }
    }

    @MapContentBuilder private func marker(_ a: Args) -> some MapContent {
        let title = a.string(1)
        switch a.int(0) {
        case 1: Marker(item: MapKitValues.item(a.list(2)))
        case 2: Marker(coordinate: MapKitValues.coordinate(a.list(2))) { SlotView(model: a.slotModel(8)) }
        default:
            let c = MapKitValues.coordinate(a.list(2))
            if let symbol = a.stringOpt(3) {
                Marker(title, systemImage: symbol, coordinate: c)
            } else if let image = a.stringOpt(4) {
                Marker(title, image: image, coordinate: c)
            } else if let monogram = a.stringOpt(5) {
                Marker(title, monogram: Text(monogram), coordinate: c)
            } else {
                Marker(title, coordinate: c)
            }
        }
    }

    @MapContentBuilder private func annotation(_ a: Args) -> some MapContent {
        if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
            annotationWithAccessory(a)
        } else {
            // before iOS 18: no accessory anchor, an item is placed at its coordinate with its name
            let anchor = d_UnitPoint(a.expr(3)) ?? .center
            let content = SlotView(model: a.slotModel(7))
            if let label = a.slotModel(8) {
                Annotation(coordinate: MapKitValues.coordinate(a.list(2)), anchor: anchor) { content } label: { NodeView(model: label) }
            } else {
                Annotation(a.string(1), coordinate: MapKitValues.coordinate(a.list(2)), anchor: anchor) { content }
            }
        }
    }

    @available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
    @MapContentBuilder private func annotationWithAccessory(_ a: Args) -> some MapContent {
        let anchor = d_UnitPoint(a.expr(3)) ?? .center
        let accessory = d_UnitPoint(a.expr(4)) ?? .center
        let content = SlotView(model: a.slotModel(7))
        if a.int(0) == 1 {
            Annotation(item: MapKitValues.item(a.list(2)), anchor: anchor, accessoryAnchor: accessory) { content }
        } else if let label = a.slotModel(8) {
            Annotation(coordinate: MapKitValues.coordinate(a.list(2)), anchor: anchor, accessoryAnchor: accessory) { content } label: { NodeView(model: label) }
        } else {
            Annotation(a.string(1), coordinate: MapKitValues.coordinate(a.list(2)), anchor: anchor, accessoryAnchor: accessory) { content }
        }
    }

    @MapContentBuilder private func userAnnotation(_ a: Args) -> some MapContent {
        if let slot = a.slotModel(2) {
            UserAnnotation(anchor: d_UnitPoint(a.expr(0)) ?? .center) { NodeView(model: slot) }
        } else {
            UserAnnotation()
        }
    }

    @MapContentBuilder private func tagged<C: MapContent>(_ c: C, _ a: Args, tag: Int) -> some MapContent {
        if let t = a.hashableOpt(tag) { c.tag(t) } else { c }
    }

    @MapContentBuilder private func styled<C: MapContent>(_ c: C, _ a: Args, style: Int, tag: Int) -> some MapContent {
        let s = MapContentStyleValues(a.list(style))
        tagged(level(titles(strokeStyle(stroke(fill(tint(c, s.tint), s.foreground), s), s), s), s.level), a, tag: tag)
    }

    @MapContentBuilder private func tint<C: MapContent>(_ c: C, _ t: AnyShapeStyle?) -> some MapContent {
        if let t { c.tint(t) } else { c }
    }

    @MapContentBuilder private func fill<C: MapContent>(_ c: C, _ t: AnyShapeStyle?) -> some MapContent {
        if let t { c.foregroundStyle(t) } else { c }
    }

    @MapContentBuilder private func stroke<C: MapContent>(_ c: C, _ s: MapContentStyleValues) -> some MapContent {
        if let st = s.stroke {
            if let style = s.strokeStyle { c.stroke(st, style: style) } else { c.stroke(st, lineWidth: s.lineWidth ?? 1) }
        } else if let w = s.lineWidth {
            c.stroke(lineWidth: w)
        } else {
            c
        }
    }

    @MapContentBuilder private func strokeStyle<C: MapContent>(_ c: C, _ s: MapContentStyleValues) -> some MapContent {
        if s.stroke == nil, let style = s.strokeStyle { c.strokeStyle(style: style) } else { c }
    }

    @MapContentBuilder private func titles<C: MapContent>(_ c: C, _ s: MapContentStyleValues) -> some MapContent {
        switch (s.titles, s.subtitles) {
        case let (t?, u?): c.annotationTitles(t).annotationSubtitles(u)
        case let (t?, nil): c.annotationTitles(t)
        case let (nil, u?): c.annotationSubtitles(u)
        default: c
        }
    }

    @MapContentBuilder private func level<C: MapContent>(_ c: C, _ l: MKOverlayLevel?) -> some MapContent {
        if let l { c.mapOverlayLevel(level: l) } else { c }
    }
}

// MARK: - Controls

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct MapControl: View {
    enum Kind { case compass, scale, userLocation, pitch }
    let m: NodeModel
    let kind: Kind
    @Environment(\.composeNamespaces) private var namespaces

    var body: some View {
        let a = m.args
        let scopeIndex = kind == .scale ? 1 : 0
        let scope = a.isNil(scopeIndex) ? nil : namespaces[Int64(a.int(scopeIndex))]
        switch kind {
        case .compass:
            #if os(tvOS)
            EmptyView()
            #else
            MapCompass(scope: scope)
            #endif
        case .scale:
            #if os(tvOS) || os(watchOS)
            EmptyView()
            #else
            MapScaleView(anchorEdge: d_HorizontalEdge(a.expr(0)) ?? .leading, scope: scope)
            #endif
        case .userLocation:
            #if os(tvOS)
            EmptyView()
            #else
            MapUserLocationButton(scope: scope)
            #endif
        case .pitch:
            #if os(iOS) || os(macOS)
            MapPitchToggle(scope: scope)
            #else
            EmptyView()
            #endif
        }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct MapScopeModifier: ViewModifier {
    let namespace: Int64
    @Environment(\.composeNamespaces) private var namespaces

    func body(content: Content) -> some View {
        if let ns = namespaces[namespace] { content.mapScope(ns) } else { content }
    }
}

// MARK: - Look Around

#if os(iOS) || os(macOS)
@available(iOS 17.0, macOS 14.0, *)
private struct KotlinLookAroundPreview: View {
    let m: NodeModel
    @State private var scene: MKLookAroundScene?

    var body: some View {
        let _ = m.version
        let a = m.args
        let c = MapKitValues.coordinate(a.list(0))
        let badges: [MKLookAroundBadgePosition] = [.topLeading, .topTrailing, .bottomTrailing]
        LookAroundPreview(scene: $scene, allowsNavigation: a.bool(1), showsRoadLabels: a.bool(2),
                          pointsOfInterest: MapKitValues.pointsOfInterest(a.int(3), a.list(4)),
                          badgePosition: badges[min(max(a.int(5), 0), 2)])
            .task(id: "\(c.latitude),\(c.longitude)") {
                scene = try? await MKLookAroundSceneRequest(coordinate: c).scene
            }
    }
}

#endif

#if os(iOS)
@available(iOS 17.0, *)
private struct LookAroundViewerModifier: ViewModifier {
    let a: Args
    @State private var scene: MKLookAroundScene?

    func body(content: Content) -> some View {
        let c = MapKitValues.coordinate(a.list(2))
        content
            .lookAroundViewer(isPresented: a.boolBinding(0), scene: $scene, allowsNavigation: a.bool(3), showsRoadLabels: a.bool(4),
                              pointsOfInterest: MapKitValues.pointsOfInterest(a.int(5), a.list(6)))
            .task(id: "\(c.latitude),\(c.longitude)") {
                scene = try? await MKLookAroundSceneRequest(coordinate: c).scene
            }
    }
}
#endif

// MARK: - MapReader

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct KotlinMapReader: View {
    let m: NodeModel

    var body: some View {
        MapReader { proxy in
            ChildrenView(m).onAppear {
                m.args.callback(0, box: { (b: MapProxyBox) -> Any? in b })(MapProxyBox(proxy))
            }
        }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@MainActor final class MapProxyBox: NSObject, @preconcurrency MapProxyHandle {
    let proxy: MapProxy
    init(_ proxy: MapProxy) { self.proxy = proxy }

    func coordinate(x: Double, y: Double) -> [KotlinDouble]? {
        proxy.convert(CGPoint(x: x, y: y), from: .local).map { [KotlinDouble(value: $0.latitude), KotlinDouble(value: $0.longitude)] }
    }

    func point(latitude: Double, longitude: Double) -> [KotlinDouble]? {
        proxy.convert(CLLocationCoordinate2D(latitude: latitude, longitude: longitude), to: .local)
            .map { [KotlinDouble(value: Double($0.x)), KotlinDouble(value: Double($0.y))] }
    }

    func camera(framing: [KotlinDouble]) -> [KotlinDouble]? {
        let l = framing.map { $0.doubleValue }
        guard l.count >= 4 else { return nil }
        let c: MapCamera
        if l[2] < 0 {
            c = proxy.camera(framing: MapKitValues.item([l[0], l[1]] as [Any]), allowPitch: l[3] != 0)
        } else {
            c = proxy.camera(framing: MapKitValues.region(l as [Any]))
        }
        return [c.centerCoordinate.latitude, c.centerCoordinate.longitude, c.distance, c.heading, c.pitch].map { KotlinDouble(value: $0) }
    }
}

// MARK: - Camera keyframes

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct MapKeyframes: ViewModifier {
    let trigger: AnyHashable
    let frames: [[Double]]

    func body(content: Content) -> some View {
        let cams = frames.map { MapKitValues.camera($0 as [Any]) }
        let times = frames.map { $0.count > 5 ? $0[5] : 1 }
        content.mapCameraKeyframeAnimator(trigger: trigger) { initial in
            KeyframeTrack(\MapCamera.centerCoordinate) {
                for (c, t) in zip(cams, times) { CubicKeyframe(c.centerCoordinate, duration: t) }
            }
            KeyframeTrack(\MapCamera.distance) {
                for (c, t) in zip(cams, times) { CubicKeyframe(c.distance, duration: t) }
            }
            KeyframeTrack(\MapCamera.heading) {
                for (c, t) in zip(cams, times) { CubicKeyframe(c.heading, duration: t) }
            }
            KeyframeTrack(\MapCamera.pitch) {
                for (c, t) in zip(cams, times) { CubicKeyframe(c.pitch, duration: t) }
            }
        }
    }
}
