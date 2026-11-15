@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC

// MapKit for SwiftUI (`Map`, map content, controls). Rendered by swift/ComposeSwiftUIIntegrations/MapKit.swift:
// add that file to the app and call `ComposeIntegrations.registerMapKit()` once at launch.

internal object MapKitIds {
    const val MAP = 200_000
    const val MARKER = 200_001
    const val ANNOTATION = 200_002
    const val CIRCLE = 200_003
    const val POLYLINE = 200_004
    const val POLYGON = 200_005
    const val USER_ANNOTATION = 200_006
    const val COMPASS = 200_007
    const val SCALE_VIEW = 200_008
    const val USER_LOCATION_BUTTON = 200_009
    const val PITCH_TOGGLE = 200_010
    const val LOOK_AROUND = 200_011
    const val MAP_READER = 200_012

    const val MAP_STYLE = 200_000
    const val MAP_CONTROLS = 200_001
    const val MAP_CONTROL_VISIBILITY = 200_002
    const val CAMERA_CHANGE = 200_003
    const val ITEM_DETAIL_SHEET = 200_004
    const val ITEM_DETAIL_POPOVER = 200_005
    const val LOOK_AROUND_VIEWER = 200_006
    const val FEATURE_ACCESSORY = 200_007
    const val FEATURE_DISABLED = 200_008
    const val MAP_SCOPE = 200_009
    const val CAMERA_KEYFRAMES = 200_010
}

/** SwiftUI / Core Location `CLLocationCoordinate2D`. */
@HiddenFromObjC
public data class CLLocationCoordinate2D(val latitude: Double, val longitude: Double) {
    internal fun encode(): List<Double> = listOf(latitude, longitude)
}

/** MapKit `MKCoordinateRegion`: [center] and the span in degrees. */
@HiddenFromObjC
public data class MKCoordinateRegion(val center: CLLocationCoordinate2D, val latitudeDelta: Double, val longitudeDelta: Double) {
    internal fun encode(): List<Double> = listOf(center.latitude, center.longitude, latitudeDelta, longitudeDelta)

    public companion object {
        /** MapKit `MKCoordinateRegion(center:latitudinalMeters:longitudinalMeters:)`. */
        public fun meters(center: CLLocationCoordinate2D, latitudinalMeters: Double, longitudinalMeters: Double): MKCoordinateRegion =
            MKCoordinateRegion(center, latitudinalMeters / 111_000.0, longitudinalMeters / (111_000.0 * kotlin.math.cos(center.latitude * kotlin.math.PI / 180).coerceAtLeast(0.01)))
    }
}

/** SwiftUI `MapCamera(centerCoordinate:distance:heading:pitch:)`. */
@HiddenFromObjC
public data class MapCamera(val centerCoordinate: CLLocationCoordinate2D, val distance: Double, val heading: Double = 0.0, val pitch: Double = 0.0) {
    internal fun encode(): List<Double> = listOf(centerCoordinate.latitude, centerCoordinate.longitude, distance, heading, pitch)
}

/**
 * A place (MapKit `MKMapItem`), built from a coordinate and a name: used by `Marker(item)`, `Annotation(item)`,
 * `MapCameraPosition.item` and the map item detail sheet and popover.
 */
@HiddenFromObjC
public data class MapItem(val coordinate: CLLocationCoordinate2D, val name: String? = null) {
    internal fun encode(): List<Any?> = listOf(coordinate.latitude, coordinate.longitude, name)
}

/** SwiftUI `MapCameraPosition`. */
@HiddenFromObjC
public sealed class MapCameraPosition {
    internal abstract fun encode(): List<Any?>

    /** SwiftUI `MapCameraPosition.automatic`: frames the map content. */
    public data object Automatic : MapCameraPosition() {
        override fun encode(): List<Any?> = listOf(0)
    }

    /** SwiftUI `MapCameraPosition.region(_:)`. */
    public data class Region(val region: MKCoordinateRegion) : MapCameraPosition() {
        override fun encode(): List<Any?> = listOf(1) + region.encode()
    }

    /** SwiftUI `MapCameraPosition.camera(_:)`. */
    public data class Camera(val camera: MapCamera) : MapCameraPosition() {
        override fun encode(): List<Any?> = listOf(2) + camera.encode()
    }

    /** SwiftUI `MapCameraPosition.userLocation(followsHeading:fallback:)`. */
    public data class UserLocation(val followsHeading: Boolean = false, val fallback: MapCameraPosition = Automatic) : MapCameraPosition() {
        override fun encode(): List<Any?> = listOf(3, followsHeading, fallback.encode())
    }

    /** SwiftUI `MapCameraPosition.item(_:allowsAutomaticPitch:)`. */
    public data class Item(val item: MapItem, val allowsAutomaticPitch: Boolean = true) : MapCameraPosition() {
        override fun encode(): List<Any?> = listOf(4, item.encode(), allowsAutomaticPitch)
    }

    /** SwiftUI `MapCameraPosition.rect(_:)` with an `MKMapRect` in map points (x, y, width, height). */
    public data class Rect(val x: Double, val y: Double, val width: Double, val height: Double) : MapCameraPosition() {
        override fun encode(): List<Any?> = listOf(5, x, y, width, height)
    }

    public companion object {
        public val automatic: MapCameraPosition get() = Automatic
        public fun region(region: MKCoordinateRegion): MapCameraPosition = Region(region)
        public fun camera(camera: MapCamera): MapCameraPosition = Camera(camera)
        public fun userLocation(followsHeading: Boolean = false, fallback: MapCameraPosition = Automatic): MapCameraPosition =
            UserLocation(followsHeading, fallback)
        public fun item(item: MapItem, allowsAutomaticPitch: Boolean = true): MapCameraPosition = Item(item, allowsAutomaticPitch)
        public fun rect(x: Double, y: Double, width: Double, height: Double): MapCameraPosition = Rect(x, y, width, height)

        internal fun decode(v: List<*>): MapCameraPosition? {
            val d = v.map { (it as? Number)?.toDouble() ?: 0.0 }
            return when (d.firstOrNull()?.toInt()) {
                1 -> Region(MKCoordinateRegion(CLLocationCoordinate2D(d[1], d[2]), d[3], d[4]))
                2 -> Camera(MapCamera(CLLocationCoordinate2D(d[1], d[2]), d[3], d[4], d[5]))
                0 -> Automatic
                else -> null
            }
        }
    }
}

/** SwiftUI `MapCameraBounds`: limits the center to [centerCoordinateBounds] and the camera distance. */
@HiddenFromObjC
public data class MapCameraBounds(
    val centerCoordinateBounds: MKCoordinateRegion? = null,
    val minimumDistance: Double? = null,
    val maximumDistance: Double? = null,
) {
    internal fun encode(): List<Any?> = listOf(centerCoordinateBounds?.encode(), minimumDistance, maximumDistance)
}

/** SwiftUI `MapInteractionModes`. */
@HiddenFromObjC
public data class MapInteractionModes(val rawValue: Int) {
    public operator fun plus(other: MapInteractionModes): MapInteractionModes = MapInteractionModes(rawValue or other.rawValue)

    public companion object {
        public val pan: MapInteractionModes = MapInteractionModes(1)
        public val zoom: MapInteractionModes = MapInteractionModes(2)
        public val rotate: MapInteractionModes = MapInteractionModes(4)
        public val pitch: MapInteractionModes = MapInteractionModes(8)
        public val all: MapInteractionModes = MapInteractionModes(15)
    }
}

/** SwiftUI `PointOfInterestCategories`: MapKit category raw values (`MKPointOfInterestCategory`, e.g. "MKPOICategoryCafe"). */
@HiddenFromObjC
public data class PointOfInterestCategories(val mode: Int, val categories: List<String> = emptyList()) {
    public companion object {
        public val all: PointOfInterestCategories = PointOfInterestCategories(0)
        public val excludingAll: PointOfInterestCategories = PointOfInterestCategories(1)
        public fun including(categories: List<String>): PointOfInterestCategories = PointOfInterestCategories(2, categories)
        public fun excluding(categories: List<String>): PointOfInterestCategories = PointOfInterestCategories(3, categories)
    }
}

/** SwiftUI `MapStyle`. */
@HiddenFromObjC
public data class MapStyle(
    val kind: Int,
    val elevation: Elevation = Elevation.automatic,
    val emphasis: StandardEmphasis = StandardEmphasis.automatic,
    val pointsOfInterest: PointOfInterestCategories = PointOfInterestCategories.all,
    val showsTraffic: Boolean = false,
) {
    /** SwiftUI `MapStyle.Elevation`. */
    public enum class Elevation { automatic, flat, realistic }

    /** SwiftUI `MapStyle.StandardEmphasis`. */
    public enum class StandardEmphasis { automatic, muted }

    internal fun encode(): List<Any?> =
        listOf(kind, elevation.ordinal, emphasis.ordinal, pointsOfInterest.mode, pointsOfInterest.categories, showsTraffic)

    public companion object {
        public val standard: MapStyle = MapStyle(0)
        public val imagery: MapStyle = MapStyle(1)
        public val hybrid: MapStyle = MapStyle(2)

        public fun standard(
            elevation: Elevation = Elevation.automatic,
            emphasis: StandardEmphasis = StandardEmphasis.automatic,
            pointsOfInterest: PointOfInterestCategories = PointOfInterestCategories.all,
            showsTraffic: Boolean = false,
        ): MapStyle = MapStyle(0, elevation, emphasis, pointsOfInterest, showsTraffic)

        public fun imagery(elevation: Elevation = Elevation.automatic): MapStyle = MapStyle(1, elevation)

        public fun hybrid(
            elevation: Elevation = Elevation.automatic,
            pointsOfInterest: PointOfInterestCategories = PointOfInterestCategories.all,
            showsTraffic: Boolean = false,
        ): MapStyle = MapStyle(2, elevation, pointsOfInterest = pointsOfInterest, showsTraffic = showsTraffic)
    }
}

/** A map feature (point of interest, territory, physical feature) the user selected on the map. */
@HiddenFromObjC
public data class MapFeature(val title: String?, val coordinate: CLLocationCoordinate2D, val kind: Kind) {
    /** SwiftUI `MapFeature.FeatureKind`. */
    public enum class Kind { pointOfInterest, territory, physicalFeature }
}

/** SwiftUI `MapItemDetailSelectionAccessoryStyle`. */
@HiddenFromObjC
public data class MapItemDetailSelectionAccessoryStyle internal constructor(val kind: Int, val callout: Int = 0) {
    /** SwiftUI `MapItemDetailSelectionAccessoryStyle.CalloutStyle`. */
    public enum class CalloutStyle { automatic, full, compact }

    public companion object {
        public val automatic: MapItemDetailSelectionAccessoryStyle = MapItemDetailSelectionAccessoryStyle(0)
        public val callout: MapItemDetailSelectionAccessoryStyle = MapItemDetailSelectionAccessoryStyle(1)
        public fun callout(style: CalloutStyle = CalloutStyle.automatic): MapItemDetailSelectionAccessoryStyle =
            MapItemDetailSelectionAccessoryStyle(1, style.ordinal)
        public val sheet: MapItemDetailSelectionAccessoryStyle = MapItemDetailSelectionAccessoryStyle(2)
        public val caption: MapItemDetailSelectionAccessoryStyle = MapItemDetailSelectionAccessoryStyle(3)
    }
}

/** MapKit `MKOverlayLevel`: draw an overlay above the roads or above the labels. */
@HiddenFromObjC
public enum class MKOverlayLevel { aboveRoads, aboveLabels }

/** SwiftUI `MapPolyline.ContourStyle`. */
@HiddenFromObjC
public enum class ContourStyle { straight, geodesic }

/** SwiftUI `MapCameraUpdateFrequency`. */
@HiddenFromObjC
public enum class MapCameraUpdateFrequency { onEnd, continuous }

/** The camera and visible region reported by [onMapCameraChange]. */
@HiddenFromObjC
public data class MapCameraUpdateContext(val camera: MapCamera, val region: MKCoordinateRegion)

/** Styling shared by the map content (SwiftUI `MapContent` modifiers). */
@HiddenFromObjC
public data class MapContentStyle(
    /** `tint(_:)` of markers and annotations. */
    val tint: ShapeStyle? = null,
    /** `foregroundStyle(_:)`: the fill of circles and polygons, the line of polylines. */
    val foregroundStyle: ShapeStyle? = null,
    /** `stroke(_:lineWidth:)` / `stroke(_:style:)` of overlays. */
    val stroke: ShapeStyle? = null,
    val lineWidth: Double? = null,
    val strokeStyle: StrokeStyle? = null,
    /** `annotationTitles(_:)` / `annotationSubtitles(_:)`. */
    val annotationTitles: Visibility? = null,
    val annotationSubtitles: Visibility? = null,
    /** `mapOverlayLevel(level:)`. */
    val overlayLevel: MKOverlayLevel? = null,
    /** `mapItemDetailSelectionAccessory(_:)` of an item marker. */
    val selectionAccessory: MapItemDetailSelectionAccessoryStyle? = null,
) {
    internal fun encode(): List<Any?> = listOf(tint, foregroundStyle, stroke, lineWidth, strokeStyle, annotationTitles, annotationSubtitles,
        overlayLevel?.ordinal, selectionAccessory?.let { listOf(it.kind, it.callout) })
}

/**
 * SwiftUI `Map(position:bounds:interactionModes:selection:scope:content:)`.
 *
 * [position] is controlled by Kotlin: when the user moves the map, [onPositionChange] receives the new
 * region or camera. [selection] is the `tag` of the selected [Marker] or [Annotation]; [onFeatureSelect] reports
 * the selected point of interest, and [featureSelectionContent] replaces its marker
 * (`mapFeatureSelectionContent`). [content] holds map content only: [Marker], [Annotation], [MapCircle],
 * [MapPolyline], [MapPolygon], [UserAnnotation] (wrap a list in a Kotlin loop).
 */
@HiddenFromObjC
@Composable
public fun Map(
    modifier: Modifier = Modifier,
    position: MapCameraPosition? = null,
    onPositionChange: ((MapCameraPosition) -> Unit)? = null,
    bounds: MapCameraBounds? = null,
    interactionModes: MapInteractionModes = MapInteractionModes.all,
    selection: Any? = null,
    onSelectionChange: ((Any?) -> Unit)? = null,
    onFeatureSelect: ((MapFeature?) -> Unit)? = null,
    featureSelectionContent: (@Composable (MapFeature) -> Unit)? = null,
    scope: NamespaceId? = null,
    content: @Composable () -> Unit = {},
) {
    var feature by remember { mutableStateOf<MapFeature?>(null) }
    val featureSlot: (@Composable () -> Unit)? = featureSelectionContent?.let { c -> { feature?.let { c(it) } } }
    SwiftView(
        MapKitIds.MAP,
        listOf(
            position?.encode(),
            onPositionChange?.let { cb -> SValueCallback { v -> MapCameraPosition.decode(v as List<*>)?.let(cb) } },
            bounds?.encode(),
            interactionModes.rawValue,
            selection,
            onSelectionChange?.let { SValueCallback(it) },
            if (onFeatureSelect != null || featureSelectionContent != null) SValueCallback { v ->
                val f = (v as? List<*>)?.let { l ->
                    MapFeature(l[0] as String?, CLLocationCoordinate2D((l[1] as Number).toDouble(), (l[2] as Number).toDouble()),
                        MapFeature.Kind.entries[(l[3] as Number).toInt()])
                }
                feature = f
                onFeatureSelect?.invoke(f)
            } else null,
            featureSlot?.let { SSlotRef(0) },
            scope?.id,
        ),
        modifier,
        slots = listOfNotNull(featureSlot),
        content = content,
    )
}

/** SwiftUI `Marker(_:coordinate:)` / `Marker(_:systemImage:coordinate:)` / `Marker(_:image:coordinate:)` / `Marker(_:monogram:coordinate:)`. */
@HiddenFromObjC
@Composable
public fun Marker(
    title: String,
    coordinate: CLLocationCoordinate2D,
    systemImage: String? = null,
    image: String? = null,
    monogram: String? = null,
    tag: Any? = null,
    style: MapContentStyle = MapContentStyle(),
) {
    SwiftView(MapKitIds.MARKER, listOf(0, title, coordinate.encode(), systemImage, image, monogram, tag, style.encode()), Modifier)
}

/** SwiftUI `Marker(item:)`. */
@HiddenFromObjC
@Composable
public fun Marker(item: MapItem, tag: Any? = null, style: MapContentStyle = MapContentStyle()) {
    SwiftView(MapKitIds.MARKER, listOf(1, item.name, item.encode(), null, null, null, tag, style.encode()), Modifier)
}

/** SwiftUI `Marker(coordinate:label:)`: [label] is a `Text` or `Label`. */
@HiddenFromObjC
@Composable
public fun Marker(coordinate: CLLocationCoordinate2D, tag: Any? = null, style: MapContentStyle = MapContentStyle(), label: @Composable () -> Unit) {
    SwiftView(MapKitIds.MARKER, listOf(2, null, coordinate.encode(), null, null, null, tag, style.encode(), SSlotRef(0)), Modifier, slots = listOf(label))
}

/**
 * SwiftUI `Annotation(_:coordinate:anchor:accessoryAnchor:content:)` and `Annotation(coordinate:…content:label:)`:
 * a Kotlin view on the map at [coordinate].
 */
@HiddenFromObjC
@Composable
public fun Annotation(
    coordinate: CLLocationCoordinate2D,
    title: String? = null,
    anchor: UnitPoint = UnitPoint.center,
    accessoryAnchor: UnitPoint? = null,
    tag: Any? = null,
    style: MapContentStyle = MapContentStyle(),
    label: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    SwiftView(
        MapKitIds.ANNOTATION,
        listOf(0, title, coordinate.encode(), anchor, accessoryAnchor, tag, style.encode(), SSlotRef(0), label?.let { SSlotRef(1) }),
        Modifier,
        slots = listOfNotNull(content, label),
    )
}

/** SwiftUI `Annotation(item:anchor:accessoryAnchor:content:)`. */
@HiddenFromObjC
@Composable
public fun Annotation(
    item: MapItem,
    anchor: UnitPoint = UnitPoint.center,
    accessoryAnchor: UnitPoint = UnitPoint.center,
    tag: Any? = null,
    style: MapContentStyle = MapContentStyle(),
    content: @Composable () -> Unit,
) {
    SwiftView(
        MapKitIds.ANNOTATION,
        listOf(1, item.name, item.encode(), anchor, accessoryAnchor, tag, style.encode(), SSlotRef(0), null),
        Modifier,
        slots = listOf(content),
    )
}

/** SwiftUI `MapCircle(center:radius:)` ([radius] in meters). */
@HiddenFromObjC
@Composable
public fun MapCircle(center: CLLocationCoordinate2D, radius: Double, tag: Any? = null, style: MapContentStyle = MapContentStyle()) {
    SwiftView(MapKitIds.CIRCLE, listOf(center.encode(), radius, tag, style.encode()), Modifier)
}

/** SwiftUI `MapPolyline(coordinates:contourStyle:)`. */
@HiddenFromObjC
@Composable
public fun MapPolyline(
    coordinates: List<CLLocationCoordinate2D>,
    contourStyle: ContourStyle = ContourStyle.straight,
    tag: Any? = null,
    style: MapContentStyle = MapContentStyle(),
) {
    SwiftView(MapKitIds.POLYLINE, listOf(coordinates.flatMap { it.encode() }, contourStyle.ordinal, tag, style.encode()), Modifier)
}

/** SwiftUI `MapPolygon(coordinates:)`. */
@HiddenFromObjC
@Composable
public fun MapPolygon(coordinates: List<CLLocationCoordinate2D>, tag: Any? = null, style: MapContentStyle = MapContentStyle()) {
    SwiftView(MapKitIds.POLYGON, listOf(coordinates.flatMap { it.encode() }, tag, style.encode()), Modifier)
}

/** SwiftUI `UserAnnotation(anchor:content:)`: the user location, drawn by [content] or by MapKit. */
@HiddenFromObjC
@Composable
public fun UserAnnotation(anchor: UnitPoint = UnitPoint.center, tag: Any? = null, content: (@Composable () -> Unit)? = null) {
    SwiftView(MapKitIds.USER_ANNOTATION, listOf(anchor, tag, content?.let { SSlotRef(0) }), Modifier, slots = listOfNotNull(content))
}

/** SwiftUI `MapCompass(scope:)`. */
@HiddenFromObjC
@Composable
public fun MapCompass(modifier: Modifier = Modifier, scope: NamespaceId? = null) {
    SwiftView(MapKitIds.COMPASS, listOf(scope?.id), modifier)
}

/** SwiftUI `MapScaleView(anchorEdge:scope:)` / `MapScaleView(alignment:scope:)`. */
@HiddenFromObjC
@Composable
public fun MapScaleView(modifier: Modifier = Modifier, anchorEdge: HorizontalEdge = HorizontalEdge.leading, scope: NamespaceId? = null) {
    SwiftView(MapKitIds.SCALE_VIEW, listOf(anchorEdge, scope?.id), modifier)
}

/** SwiftUI `MapUserLocationButton(scope:)`. */
@HiddenFromObjC
@Composable
public fun MapUserLocationButton(modifier: Modifier = Modifier, scope: NamespaceId? = null) {
    SwiftView(MapKitIds.USER_LOCATION_BUTTON, listOf(scope?.id), modifier)
}

/** SwiftUI `MapPitchToggle(scope:)`. */
@HiddenFromObjC
@Composable
public fun MapPitchToggle(modifier: Modifier = Modifier, scope: NamespaceId? = null) {
    SwiftView(MapKitIds.PITCH_TOGGLE, listOf(scope?.id), modifier)
}

/** MapKit `MKLookAroundBadgePosition`. */
@HiddenFromObjC
public enum class LookAroundBadgePosition { topLeading, topTrailing, bottomTrailing }

/**
 * SwiftUI `LookAroundPreview(initialScene:allowsNavigation:showsRoadLabels:pointsOfInterest:badgePosition:)`: the
 * Look Around scene at [coordinate] (loaded with `MKLookAroundSceneRequest`; empty where there is none).
 */
@HiddenFromObjC
@Composable
public fun LookAroundPreview(
    coordinate: CLLocationCoordinate2D,
    modifier: Modifier = Modifier,
    allowsNavigation: Boolean = true,
    showsRoadLabels: Boolean = true,
    pointsOfInterest: PointOfInterestCategories = PointOfInterestCategories.all,
    badgePosition: LookAroundBadgePosition = LookAroundBadgePosition.topLeading,
) {
    SwiftView(MapKitIds.LOOK_AROUND, listOf(coordinate.encode(), allowsNavigation, showsRoadLabels, pointsOfInterest.mode,
        pointsOfInterest.categories, badgePosition.ordinal), modifier)
}

/** Converts between screen points and coordinates on a [Map] (SwiftUI `MapProxy`). */
@HiddenFromObjC
public class MapProxy internal constructor(private val handle: () -> MapProxyHandle?) {
    /** SwiftUI `MapProxy.convert(_:from:)` in the local coordinate space of the [MapReader] content. */
    public fun convert(point: CGPoint): CLLocationCoordinate2D? =
        handle()?.coordinate(point.x, point.y)?.let { CLLocationCoordinate2D(it[0], it[1]) }

    /** SwiftUI `MapProxy.convert(_:to:)`. */
    public fun convert(coordinate: CLLocationCoordinate2D): CGPoint? =
        handle()?.point(coordinate.latitude, coordinate.longitude)?.let { CGPoint(it[0], it[1]) }

    /** SwiftUI `MapProxy.camera(framing:)`. */
    public fun camera(framing: MKCoordinateRegion): MapCamera? =
        handle()?.camera(framing.encode())?.let { MapCamera(CLLocationCoordinate2D(it[0], it[1]), it[2], it[3], it[4]) }

    /** SwiftUI `MapProxy.camera(framing:allowPitch:)` for an item. */
    public fun camera(framing: MapItem, allowPitch: Boolean = true): MapCamera? =
        handle()?.camera(listOf(framing.coordinate.latitude, framing.coordinate.longitude, -1.0, if (allowPitch) 1.0 else 0.0))
            ?.let { MapCamera(CLLocationCoordinate2D(it[0], it[1]), it[2], it[3], it[4]) }
}

/** SwiftUI `MapReader { proxy in }`: put the [Map] inside [content]. */
@HiddenFromObjC
@Composable
public fun MapReader(modifier: Modifier = Modifier, content: @Composable (MapProxy) -> Unit) {
    val holder = remember { arrayOfNulls<MapProxyHandle>(1) }
    val proxy = remember { MapProxy { holder[0] } }
    SwiftView(MapKitIds.MAP_READER, listOf(SValueCallback { holder[0] = it as? MapProxyHandle }), modifier) {
        content(proxy)
    }
}

/** SwiftUI `mapStyle(_:)`. */
@HiddenFromObjC
public fun Modifier.mapStyle(style: MapStyle): Modifier = element(MapKitIds.MAP_STYLE, style.encode())

/** SwiftUI `mapControls(_:)`: [content] lists [MapCompass], [MapScaleView], [MapUserLocationButton], [MapPitchToggle]. */
@HiddenFromObjC
public fun Modifier.mapControls(content: @Composable () -> Unit): Modifier = element(MapKitIds.MAP_CONTROLS, slotContent(content))

/** SwiftUI `mapControlVisibility(_:)`. */
@HiddenFromObjC
public fun Modifier.mapControlVisibility(visibility: Visibility): Modifier = element(MapKitIds.MAP_CONTROL_VISIBILITY, visibility)

/** SwiftUI `mapScope(_:)`: map controls outside the map with the same [scope] drive it. */
@HiddenFromObjC
public fun Modifier.mapScope(scope: NamespaceId): Modifier = element(MapKitIds.MAP_SCOPE, scope.id)

/** SwiftUI `onMapCameraChange(frequency:_:)`. */
@HiddenFromObjC
public fun Modifier.onMapCameraChange(
    frequency: MapCameraUpdateFrequency = MapCameraUpdateFrequency.onEnd,
    action: (MapCameraUpdateContext) -> Unit,
): Modifier = element(MapKitIds.CAMERA_CHANGE, frequency.ordinal, SValueCallback { v ->
    val d = (v as List<*>).map { (it as Number).toDouble() }
    action(MapCameraUpdateContext(MapCamera(CLLocationCoordinate2D(d[0], d[1]), d[2], d[3], d[4]),
        MKCoordinateRegion(CLLocationCoordinate2D(d[5], d[6]), d[7], d[8])))
})

/** SwiftUI `mapItemDetailSheet(isPresented:item:displaysMap:)`. */
@HiddenFromObjC
public fun Modifier.mapItemDetailSheet(isPresented: Boolean, onDismiss: () -> Unit, item: MapItem?, displaysMap: Boolean = true): Modifier =
    element(MapKitIds.ITEM_DETAIL_SHEET, isPresented, SValueCallback { if (it == false) onDismiss() }, item?.encode(), displaysMap)

/** SwiftUI `mapItemDetailPopover(isPresented:item:displaysMap:attachmentAnchor:arrowEdge:)`. */
@HiddenFromObjC
public fun Modifier.mapItemDetailPopover(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    item: MapItem?,
    displaysMap: Boolean = true,
    arrowEdge: Edge? = null,
): Modifier = element(MapKitIds.ITEM_DETAIL_POPOVER, isPresented, SValueCallback { if (it == false) onDismiss() }, item?.encode(), displaysMap, arrowEdge)

/** SwiftUI `lookAroundViewer(isPresented:initialScene:…)`: full-screen Look Around at [coordinate]. */
@HiddenFromObjC
public fun Modifier.lookAroundViewer(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    coordinate: CLLocationCoordinate2D,
    allowsNavigation: Boolean = true,
    showsRoadLabels: Boolean = true,
    pointsOfInterest: PointOfInterestCategories = PointOfInterestCategories.all,
): Modifier = element(MapKitIds.LOOK_AROUND_VIEWER, isPresented, SValueCallback { if (it == false) onDismiss() }, coordinate.encode(),
    allowsNavigation, showsRoadLabels, pointsOfInterest.mode, pointsOfInterest.categories)

/** SwiftUI `mapFeatureSelectionAccessory(_:)`. */
@HiddenFromObjC
public fun Modifier.mapFeatureSelectionAccessory(style: MapItemDetailSelectionAccessoryStyle? = MapItemDetailSelectionAccessoryStyle.automatic): Modifier =
    element(MapKitIds.FEATURE_ACCESSORY, style?.let { listOf(it.kind, it.callout) })

/** SwiftUI `mapFeatureSelectionDisabled(_:)`: [disabled] decides per feature kind. */
@HiddenFromObjC
public fun Modifier.mapFeatureSelectionDisabled(disabled: (MapFeature.Kind) -> Boolean): Modifier =
    element(MapKitIds.FEATURE_DISABLED, MapFeature.Kind.entries.map(disabled))

/**
 * SwiftUI `mapCameraKeyframeAnimator(trigger:keyframes:)`: when [trigger] changes, the camera moves through
 * [keyframes] (each camera reached after its duration in seconds, with a cubic curve).
 */
@HiddenFromObjC
public fun Modifier.mapCameraKeyframeAnimator(trigger: Any, keyframes: List<Pair<MapCamera, Double>>): Modifier =
    element(MapKitIds.CAMERA_KEYFRAMES, trigger, keyframes.map { it.first.encode() + it.second })
