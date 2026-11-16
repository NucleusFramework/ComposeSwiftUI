<!-- group: Apple frameworks -->
# Maps

> Show MapKit maps from Kotlin: camera position, markers, annotations and overlays, map styles and controls, selection, MapReader and Look Around.

The MapKit for SwiftUI API (`Map`, `Marker`, `Annotation`, `MapPolyline`…) is available from Kotlin with the same names.
The Kotlin side describes the map content; MapKit renders it natively, with its gestures, its 3D buildings and its
controls.

MapKit is an opt-in integration (see [Apple frameworks](apple-frameworks.md)): add
`ComposeSwiftUIIntegrations/MapKit.swift` to your target and register it at launch.

```swift
@main
struct MyApp: App {
    init() { ComposeIntegrations.registerMapKit() }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

The map needs iOS 17, macOS 14, tvOS 17 or watchOS 10. A few pieces are platform-specific; they are listed with each
API below.

## A first map

```kotlin
val paris = CLLocationCoordinate2D(48.8566, 2.3522)
val eiffel = CLLocationCoordinate2D(48.8584, 2.2945)

Map(modifier = Modifier.mapStyle(MapStyle.standard(elevation = MapStyle.Elevation.realistic))) {
    Marker("Eiffel Tower", eiffel, systemImage = "star.fill")
    Marker("Notre-Dame", CLLocationCoordinate2D(48.8530, 2.3499), monogram = "ND")
    MapCircle(paris, radius = 800.0, style = MapContentStyle(foregroundStyle = Color.blue.opacity(0.2)))
}
```

Without a `position`, the map frames its content (`MapCameraPosition.automatic`). The content lambda takes map content
only: `Marker`, `Annotation`, `MapCircle`, `MapPolyline`, `MapPolygon` and `UserAnnotation`. Regular views there are not
supported. Use Kotlin loops and conditions to build it from your data.

| SwiftUI | Compose SwiftUI |
|---|---|
| `Map(position: $position) { … }` | `Map(position = position, onPositionChange = { position = it }) { … }` |
| `Marker("Cafe", coordinate: c)` | `Marker("Cafe", c)` |
| `Annotation("Cafe", coordinate: c) { … }` | `Annotation(c, title = "Cafe") { … }` |
| `.tint(.orange)` on map content | `style = MapContentStyle(tint = Color.orange)` |
| `.mapStyle(.hybrid)` | `Modifier.mapStyle(MapStyle.hybrid)` |
| `.mapControls { MapCompass() }` | `Modifier.mapControls { MapCompass() }` |

Map content modifiers (`tint`, `foregroundStyle`, `stroke`, `annotationTitles`…) are not `Modifier`s, because map
content is not a view: each content composable takes a `MapContentStyle` instead.

## Camera position

`position` is controlled by Kotlin. Keep it in state and update it from `onPositionChange`:

```kotlin
@Composable
fun CityMap(city: CLLocationCoordinate2D) {
    var position by remember {
        mutableStateOf(MapCameraPosition.region(MKCoordinateRegion(city, latitudeDelta = 0.05, longitudeDelta = 0.05)))
    }
    VStack {
        Map(position = position, onPositionChange = { position = it })
        Button("Back to the center") {
            position = MapCameraPosition.region(MKCoordinateRegion(city, 0.05, 0.05))
        }
    }
}
```

`onPositionChange` is called when a user's camera move ends (pan, zoom, rotate). It reports a
`MapCameraPosition.Camera` when your `position` was a camera or the map is pitched or rotated, so the heading and
pitch are kept, and a `MapCameraPosition.Region` otherwise. A `userLocation`, `item` or `automatic` position stays as
you set it (following the user, framing the item or the content) until the user pans the map; only then is it replaced
by the reported region or camera. Setting `position` to a new value moves the camera; setting it back to the value
the map just reported does nothing.
If you pass `position` without `onPositionChange`, the map still moves under the user's fingers, and Kotlin only moves
it when your value changes.

The positions:

| Kotlin | SwiftUI |
|---|---|
| `MapCameraPosition.automatic` | `.automatic`: frames the content |
| `MapCameraPosition.region(MKCoordinateRegion(center, latDelta, lonDelta))` | `.region(_:)` |
| `MapCameraPosition.region(MKCoordinateRegion.meters(center, 2_000.0, 2_000.0))` | a region in meters |
| `MapCameraPosition.camera(MapCamera(center, distance = 800.0, heading = 30.0, pitch = 60.0))` | `.camera(_:)` |
| `MapCameraPosition.userLocation(followsHeading = true, fallback = …)` | `.userLocation(followsHeading:fallback:)` |
| `MapCameraPosition.item(MapItem(coordinate, "Louvre"))` | `.item(_:allowsAutomaticPitch:)` |
| `MapCameraPosition.rect(x, y, width, height)` | `.rect(_:)` in map points |

### Bounds and interaction modes

```kotlin
val france = MKCoordinateRegion(CLLocationCoordinate2D(46.6, 2.4), 10.0, 12.0)

Map(
    bounds = MapCameraBounds(centerCoordinateBounds = france, minimumDistance = 500.0, maximumDistance = 2_000_000.0),
    interactionModes = MapInteractionModes.pan + MapInteractionModes.zoom,
)
```

`MapInteractionModes` combines with `+` (`pan`, `zoom`, `rotate`, `pitch`, `all`).

### Following the camera

`Modifier.onMapCameraChange` reports the camera and the visible region, at the end of each move or continuously:

```kotlin
var distance by remember { mutableStateOf(0.0) }

Map(
    modifier = Modifier.onMapCameraChange(MapCameraUpdateFrequency.continuous) { context ->
        distance = context.camera.distance
    },
)
```

Use it to load the points of the visible region (`context.region`) or to show the zoom level. The continuous frequency
calls Kotlin on every frame of a gesture: keep the work light.

### Animating the camera

`Modifier.mapCameraKeyframeAnimator` flies the camera through keyframes when `trigger` changes. Each keyframe is a
camera and the duration in seconds to reach it:

```kotlin
val start = CLLocationCoordinate2D(37.3349, -122.0090)
var flights by remember { mutableStateOf(0) }

VStack {
    Map(
        modifier = Modifier.mapCameraKeyframeAnimator(
            trigger = flights,
            keyframes = listOf(
                MapCamera(start, distance = 3_000.0) to 1.0,
                MapCamera(start, distance = 600.0, heading = 90.0, pitch = 60.0) to 2.0,
                MapCamera(start, distance = 600.0, heading = 270.0, pitch = 60.0) to 4.0,
            ),
        ),
    )
    Button("Fly around") { flights++ }
}
```

## Markers and annotations

`Marker` is MapKit's balloon. It shows a system image, an image from your asset catalog, a monogram or a custom label:

```kotlin
val louvre = CLLocationCoordinate2D(48.8606, 2.3376)

Map {
    Marker("Louvre", louvre, systemImage = "building.columns", style = MapContentStyle(tint = Color.orange))
    Marker("Home", CLLocationCoordinate2D(48.85, 2.35), image = "HomePin")
    Marker(MapItem(CLLocationCoordinate2D(48.8738, 2.2950), name = "Arc de Triomphe"))
    Marker(CLLocationCoordinate2D(48.8867, 2.3431)) {
        Label("Sacré-Cœur", systemImage = SFSymbol("sparkles"))
    }
}
```

`Annotation` places any Kotlin view at a coordinate. The view is a regular composable, so it can hold state and react
to taps:

```kotlin
@Composable
fun StationPin(bikes: Int, onTap: () -> Unit) {
    VStack(spacing = 2.0, modifier = Modifier.onTapGesture { onTap() }) {
        Text("$bikes", modifier = Modifier.font(Font.caption).padding(4.0).background(Color.green, Circle()))
        Image(systemName = SFSymbol("bicycle"))
    }
}

data class Station(val id: String, val name: String, val coordinate: CLLocationCoordinate2D, val bikes: Int)

@Composable
fun StationsMap(stations: List<Station>, onOpen: (Station) -> Unit) {
    Map(
        modifier = Modifier.mapStyle(MapStyle.standard(pointsOfInterest = PointOfInterestCategories.excludingAll)),
    ) {
        for (station in stations) {
            Annotation(station.coordinate, title = station.name, anchor = UnitPoint.bottom) {
                StationPin(station.bikes) { onOpen(station) }
            }
        }
    }
}
```

`anchor` is the point of the view placed on the coordinate (`UnitPoint.bottom` for a pin). Use
`MapContentStyle(annotationTitles = Visibility.hidden)` to hide the titles.

`UserAnnotation()` draws the user location, with MapKit's blue dot or with your own content. It needs location
authorization (see Permissions below).

## Overlays

Circles, polylines and polygons are styled with `MapContentStyle`: `foregroundStyle` fills circles and polygons and
colors polylines, `stroke`, `lineWidth` and `strokeStyle` draw outlines, and `overlayLevel` places the overlay above
the roads or above the labels.

```kotlin
val route = listOf(
    CLLocationCoordinate2D(48.8584, 2.2945),
    CLLocationCoordinate2D(48.8606, 2.3376),
    CLLocationCoordinate2D(48.8530, 2.3499),
)
val park = listOf(
    CLLocationCoordinate2D(48.8462, 2.3371),
    CLLocationCoordinate2D(48.8462, 2.3412),
    CLLocationCoordinate2D(48.8440, 2.3412),
    CLLocationCoordinate2D(48.8440, 2.3371),
)

Map {
    MapPolyline(route, style = MapContentStyle(stroke = Color.blue, lineWidth = 5.0))
    MapPolygon(
        park,
        style = MapContentStyle(
            foregroundStyle = Color.green.opacity(0.3),
            stroke = Color.green,
            strokeStyle = StrokeStyle(lineWidth = 2.0, dash = listOf(6.0, 4.0)),
            overlayLevel = MKOverlayLevel.aboveLabels,
        ),
    )
    MapCircle(route.first(), radius = 300.0, style = MapContentStyle(foregroundStyle = Color.orange.opacity(0.25)))
}
```

`MapPolyline` also takes `contourStyle = ContourStyle.geodesic` for long distances (the line follows the curvature of
the Earth). Routes from `MKDirections` are Swift objects: compute them in Swift and pass the coordinates to Kotlin (see
[Interop](interop.md)).

## Map styles

| Kotlin | Result |
|---|---|
| `MapStyle.standard` | the default map |
| `MapStyle.standard(emphasis = MapStyle.StandardEmphasis.muted)` | muted colors, for data on top |
| `MapStyle.standard(elevation = MapStyle.Elevation.realistic)` | 3D terrain and buildings |
| `MapStyle.standard(pointsOfInterest = PointOfInterestCategories.including(listOf("MKPOICategoryCafe")))` | only some points of interest |
| `MapStyle.standard(showsTraffic = true)` | traffic |
| `MapStyle.imagery(elevation = …)` | satellite imagery |
| `MapStyle.hybrid(elevation = …, pointsOfInterest = …, showsTraffic = …)` | imagery with roads and labels |

Point-of-interest categories are the raw values of `MKPointOfInterestCategory` (`"MKPOICategoryCafe"`,
`"MKPOICategoryPark"`…). `PointOfInterestCategories.excludingAll` hides them all.

## Controls

Controls go in `Modifier.mapControls`. On the map, MapKit positions them:

```kotlin
Map(
    modifier = Modifier.mapControls {
        MapCompass()
        MapScaleView()
        MapUserLocationButton()
        MapPitchToggle()
    },
)
```

`Modifier.mapControlVisibility(Visibility.hidden)` hides the default controls.

Not every control exists on every platform. The unsupported ones render nothing, so shared Kotlin code can list them all:

| Control | Platforms |
|---|---|
| `MapCompass` | iOS, macOS, watchOS |
| `MapScaleView` | iOS, macOS |
| `MapUserLocationButton` | iOS, macOS, watchOS |
| `MapPitchToggle` | iOS, macOS |
| `MapPitchSlider`, `MapZoomStepper` | macOS |
| `MapLocationCompass` | watchOS |

### Controls outside the map

To place a control anywhere in your layout, give the map and the control the same scope. A scope is a SwiftUI
`@Namespace`, created with `NamespaceScope`:

```kotlin
NamespaceScope { mapScope ->
    ZStack(alignment = Alignment.topTrailing, modifier = Modifier.mapScope(mapScope)) {
        Map(scope = mapScope, modifier = Modifier.mapControlVisibility(Visibility.hidden))
        VStack(modifier = Modifier.padding()) {
            MapUserLocationButton(scope = mapScope)
            MapCompass(scope = mapScope)
        }
    }
}
```

`Modifier.mapScope` goes on a common ancestor of the map and its controls.

## Selection

### Your markers

Give markers and annotations a `tag`, and pass the selected tag to `Map`:

```kotlin
data class Cafe(val id: String, val name: String, val coordinate: CLLocationCoordinate2D)

@Composable
fun CafesMap(cafes: List<Cafe>) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    VStack {
        Map(selection = selectedId, onSelectionChange = { selectedId = it as String? }) {
            for (cafe in cafes) {
                Marker(cafe.name, cafe.coordinate, systemImage = "cup.and.saucer.fill", tag = cafe.id)
            }
        }
        val selected = cafes.firstOrNull { it.id == selectedId }
        if (selected != null) Text(selected.name, modifier = Modifier.font(Font.headline).padding())
    }
}
```

The tag is any value Swift can compare (strings and numbers are the safe choice). A selected marker grows; a selected
annotation is up to you (read `selectedId` in its content).

### Points of interest (iOS)

`onFeatureSelect` reports the map feature the user taps (a point of interest, a territory or a physical feature), and
`featureSelectionContent` replaces its selected marker with your view:

```kotlin
var feature by remember { mutableStateOf<MapFeature?>(null) }

Map(
    onFeatureSelect = { feature = it },
    featureSelectionContent = { f -> Text(f.title ?: "", modifier = Modifier.font(Font.caption)) },
    modifier = Modifier.mapFeatureSelectionDisabled { kind -> kind == MapFeature.Kind.territory },
)
```

Feature selection is iOS only, and on a map that uses it, `selection` (marker tags) is not applied: choose one or the
other. `Modifier.mapFeatureSelectionAccessory(MapItemDetailSelectionAccessoryStyle.callout)` (iOS 18) shows MapKit's
place card for the selected feature; for your own item markers, set `selectionAccessory` in their `MapContentStyle`.

## Map item details

`Modifier.mapItemDetailSheet` and `Modifier.mapItemDetailPopover` show MapKit's place card for a `MapItem` (iOS 18,
macOS 15):

```kotlin
var showDetail by remember { mutableStateOf(false) }
val museum = MapItem(CLLocationCoordinate2D(48.8600, 2.3266), name = "Musée d'Orsay")

Button(
    "Details",
    modifier = Modifier.mapItemDetailSheet(isPresented = showDetail, onDismiss = { showDetail = false }, item = museum),
) { showDetail = true }
```

A `MapItem` carries a coordinate and a name, not the full `MKMapItem` of a search result, so the card shows what MapKit
can resolve from them.

## MapReader

`MapReader` gives a `MapProxy` to convert between screen points and coordinates, for example to drop a pin where the
user taps:

```kotlin
@Composable
fun DropPinMap() {
    var pins by remember { mutableStateOf(listOf<CLLocationCoordinate2D>()) }
    MapReader { proxy ->
        Map(
            modifier = Modifier.onTapGesture { x, y ->
                proxy.convert(CGPoint(x, y))?.let { pins = pins + it }
            },
        ) {
            for ((i, pin) in pins.withIndex()) Marker("Pin ${i + 1}", pin)
        }
    }
}
```

The proxy also converts a coordinate to a point (`proxy.convert(coordinate)`) and computes the camera that frames a
region or an item (`proxy.camera(framing = region)`). Its functions return `null` until the map has been laid out.

## Look Around

`LookAroundPreview` shows the street-level imagery at a coordinate (iOS and macOS). It loads the scene itself and stays
empty where Apple has no imagery:

```kotlin
LookAroundPreview(
    CLLocationCoordinate2D(48.8584, 2.2945),
    modifier = Modifier.frame(height = 200.0),
    badgePosition = LookAroundBadgePosition.bottomTrailing,
)
```

`Modifier.lookAroundViewer(isPresented, onDismiss, coordinate)` opens it full screen (iOS).

## Permissions

The map itself needs no permission. Everything about the user location does: `UserAnnotation`,
`MapUserLocationButton` and `MapCameraPosition.userLocation` show nothing until the app is authorized.

1. Add `NSLocationWhenInUseUsageDescription` to the app's Info.plist.
2. Request authorization in Swift with a `CLLocationManager` kept alive by your app (for example in the `App` struct):

```swift
import CoreLocation

let locationManager = CLLocationManager()
locationManager.requestWhenInUseAuthorization()
```

Alternatively, `LocationButton` (CoreLocationUI, iOS and watchOS, registered by `AuthenticationServices.swift`) grants
one-time access from Kotlin without a prompt.

## Not covered

The integration covers the SwiftUI map views. MapKit services such as `MKLocalSearch`, `MKDirections`, geocoding or
`MKLookAroundSceneRequest` results are Swift APIs: call them in Swift and pass plain data (coordinates, names) to
Kotlin. The full list of mapped APIs is on [/mapkit.html](/mapkit.html).
