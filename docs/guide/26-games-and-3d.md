<!-- group: Apple frameworks -->
# Games and 3D

> Embed SpriteKit scenes, SceneKit scenes and RealityKit models in Kotlin screens, and receive game controller input.

Compose SwiftUI is a UI toolkit, not a game engine: the game loop, physics and rendering stay in SpriteKit, SceneKit or
RealityKit. What the integrations give you is a way to place those scenes in a Kotlin screen, next to your menus, HUD
and settings, and to configure how they are displayed.

| Kotlin API | Framework | File, register | Platforms |
|---|---|---|---|
| `SpriteView` | SpriteKit | `SpriteKit.swift`, `registerSpriteKit()` | all |
| `SceneView` | SceneKit | `SpriteKit.swift`, `registerSpriteKit()` | all |
| `Modifier.handlesGameControllerEvents` | GameController | `SpriteKit.swift`, `registerSpriteKit()` | iOS 26, visionOS |
| `RealityView`, `Modifier.realityViewStyle` | RealityKit | `RealityKit.swift`, `registerRealityKit()` | iOS 18, macOS 15, visionOS |

```swift
@main
struct MyApp: App {
    init() {
        ComposeIntegrations.registerSpriteKit()   // SpriteKit.swift: SpriteKit, SceneKit, GameController
        #if os(iOS) || os(macOS) || os(visionOS)
        ComposeIntegrations.registerRealityKit()  // RealityKit.swift
        #endif
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

## Where the scene comes from

A scene is a graph of Swift (or Objective-C) objects, so it can't be built from Kotlin composables. The views load it
from the app bundle instead, by name:

| View | Loads | Swift equivalent |
|---|---|---|
| `SpriteView(sceneFileName = "Level1")` | `Level1.sks` from the main bundle | `SKScene(fileNamed: "Level1")` |
| `SpriteView(sceneClassName = "GameScene")` | an instance of the `SKScene` subclass registered under that Objective-C name | `GameScene(size: viewSize)` |
| `SceneView(sceneFileName = "art.scnassets/ship.scn")` | a `.scn`, `.usdz`, `.dae`… scene from the main bundle | `SCNScene(named:)` |
| `RealityView(models = listOf("Robot"))` | `Robot.usdz` / `Robot.reality` from the main bundle | `Entity(named: "Robot")` |

Add the asset files to the app target in Xcode (Copy Bundle Resources) so they end up in the main bundle. Files in the
Kotlin module's resources are not in the main bundle.

## SpriteKit: SpriteView

With a scene file designed in Xcode's SpriteKit editor:

```kotlin
var paused by remember { mutableStateOf(false) }

ZStack(alignment = Alignment.topTrailing) {
    SpriteView(
        sceneFileName = "Level1",
        isPaused = paused,
        options = listOf(SpriteViewOption.ignoresSiblingOrder, SpriteViewOption.shouldCullNonVisibleNodes),
        debugOptions = listOf(SpriteViewDebugOption.showsFPS, SpriteViewDebugOption.showsNodeCount),
        modifier = Modifier.ignoresSafeArea(),
    )
    Button(if (paused) "Resume" else "Pause", modifier = Modifier.padding()) { paused = !paused }
}
```

With a scene class written in Swift, give it an Objective-C name so the renderer can find it with `NSClassFromString`:

```swift
import SpriteKit

@objc(GameScene)
final class GameScene: SKScene {
    override func didMove(to view: SKView) {
        backgroundColor = .black
        let ship = SKSpriteNode(imageNamed: "Ship")
        ship.position = CGPoint(x: frame.midX, y: frame.midY)
        addChild(ship)
    }
}
```

```kotlin
SpriteView(sceneClassName = "GameScene", preferredFramesPerSecond = 120)
```

A class scene is created at the view's size with `scaleMode = .resizeFill`. When neither a file nor a class is found,
the view shows an empty scene.

Things to know:

- The scene is created when the view first appears and kept while `sceneFileName` and `sceneClassName` stay the same.
  Changing either one builds the new scene, so `SpriteView(sceneFileName = level)` switches levels; switching scenes
  inside SpriteKit with `view?.presentScene(_:)` (for a custom transition) works too.
- `isPaused` and `preferredFramesPerSecond` apply live.
- On watchOS, `options` and `debugOptions` are ignored (SwiftUI's watchOS `SpriteView` doesn't take them).
- Game state that the Kotlin UI needs (score, lives) has to cross from the scene to Kotlin. Keep it in a Kotlin object
  that the Swift scene updates (Kotlin classes are visible from Swift), for example a view model whose properties are
  Compose state.

```kotlin
class GameHud {
    var score by mutableStateOf(0)
    var lives by mutableStateOf(3)

    // Called from the Swift scene: hud.addPoints(points: 10)
    fun addPoints(points: Int) { score += points }
}

@Composable
fun GameScreen(hud: GameHud) {
    ZStack(alignment = Alignment.top) {
        SpriteView(sceneClassName = "GameScene")
        HStack(modifier = Modifier.padding()) {
            Text("Score ${hud.score}")
            Spacer()
            Text("Lives ${hud.lives}")
        }
    }
}
```

Pass the same `GameHud` instance to the Swift scene (for example from the Swift code that creates the `ComposeView`),
and the HUD recomposes when the scene calls it.

## SceneKit: SceneView

```kotlin
SceneView(
    sceneFileName = "art.scnassets/ship.scn",
    pointOfView = "camera",
    options = listOf(SceneViewOption.allowsCameraControl, SceneViewOption.autoenablesDefaultLighting),
    antialiasingMode = SceneAntialiasing.multisampling4X,
    modifier = Modifier.frame(height = 320.0),
)
```

- `pointOfView` names a node of the scene (searched recursively) to use as the camera; `null` uses the scene's default.
- `allowsCameraControl` lets the user orbit and zoom with gestures; `rendersContinuously` keeps rendering when nothing
  changes (needed for animations driven outside SceneKit).
- Changing `sceneFileName` loads the new scene.

SceneKit is in maintenance mode at Apple; for new 3D content on iOS 18 and macOS 15 and later, prefer RealityKit.

## RealityKit: RealityView

`RealityView` loads one or more Reality Composer Pro / USDZ models from the main bundle into a RealityKit scene, with
`placeholder` shown while they load:

```kotlin
RealityView(
    models = listOf("Robot", "Pedestal"),
    modifier = Modifier
        .realityViewStyle(cameraControls = RealityCameraControls.orbit, layout = RealityViewLayout.centered)
        .frame(height = 360.0),
    placeholder = { ProgressView() },
)
```

| Kotlin | SwiftUI | Availability |
|---|---|---|
| `realityViewStyle(cameraControls)` | `realityViewCameraControls(_:)` (`none`, `orbit`, `dolly`, `pan`, `tilt`) | iOS 18, macOS 15 (not visionOS) |
| `realityViewStyle(layout)` | `realityViewLayoutBehavior(_:)` (`flexible`, `centered`, `fixedSize`) | iOS 26, macOS 26, visionOS 26 |

The models are loaded with `Entity(named:)` and added to the scene's root, at their authored positions. A model that
fails to load is skipped. Changing the `models` list rebuilds the view and loads them again.

Anything beyond placing bundled models (anchors, gestures on entities, physics, custom systems, immersive spaces on
visionOS) is RealityKit code in Swift. Write that `RealityView` in Swift and show it from Kotlin with a `NativeView`
(see [Interop](interop.md)).

## GameController events

On iOS 26, a game controller's buttons also drive the UI (focus and buttons) by default. In a game view, you want them
delivered to your game instead. `Modifier.handlesGameControllerEvents` marks the view that handles them:

```kotlin
SpriteView(
    sceneClassName = "GameScene",
    modifier = Modifier.handlesGameControllerEvents(receivesEventsInView = true),
)
```

Inside the scene, read the controller with the GameController framework (`GCController.current`) in Swift as usual.
`receivesEventsInView` maps to `withOptions: .receivesEventsInView(_:)`. iOS and visionOS only.

Declare the controller profiles your game supports in Info.plist with `GCSupportedGameControllers`.
