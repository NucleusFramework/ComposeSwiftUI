# Compose SwiftUI — generated Kotlin API index

Generated from the SwiftUI symbol graphs. Every entry is compiler-verified on iOS and macOS.

## Views (@Composable)

```kotlin
AngularGradientView(angularGradient: AngularGradient, modifier: Modifier = Modifier) { SwiftView(1, listOf(angularGradient), modifier) } /** * SwiftUI `AnyShape` * * ```swift * @frozen struct AnyShape * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ * Renders the value as a view. */ @HiddenFromObjC @Composable public fun AnyShapeView(anyShape: AnyShape, modifier: Modifier = Modifier) { SwiftView(2, listOf(anyShape), modifier) } /** * SwiftUI `ArrangementView.init(primary:secondary:)` * * ```swift * nonisolated init(@ContentBuilder primary: () -> Primary, @ContentBuilder secondary: () -> Secondary) * ``` * Availability: iOS 27.1+, macOS 27.1+, tvOS 27.1+, watchOS 27.1+ */ @HiddenFromObjC @Composable public fun ArrangementView(modifier: Modifier = Modifier, primary: @Composable () -> Unit, secondary: @Composable () -> Unit,)
AsyncImage(url: String?, modifier: Modifier = Modifier, scale: Double = 1.0,)
Button(role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit, content: @Composable () -> Unit,)
Button(modifier: Modifier = Modifier, action: () -> Unit, content: @Composable () -> Unit,)
Button(role: ButtonRole, modifier: Modifier = Modifier, action: () -> Unit,)
Button(title: String, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
Button(title: String, modifier: Modifier = Modifier, action: () -> Unit,)
Button(title: String, systemImage: SFSymbol, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
Button(title: String, systemImage: SFSymbol, modifier: Modifier = Modifier, action: () -> Unit,)
ButtonBorderShapeView(buttonBorderShape: ButtonBorderShape, modifier: Modifier = Modifier) { SwiftView(28, listOf(buttonBorderShape), modifier) } /** * SwiftUI `Capsule` * * ```swift * @frozen struct Capsule * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ * Renders the value as a view. */ @HiddenFromObjC @Composable public fun CapsuleView(capsule: Capsule, modifier: Modifier = Modifier) { SwiftView(29, listOf(capsule), modifier) } /** * SwiftUI `Circle` * * ```swift * @frozen struct Circle * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ * Renders the value as a view. */ @HiddenFromObjC @Composable public fun CircleView(circle: Circle, modifier: Modifier = Modifier) { SwiftView(30, listOf(circle), modifier) } /** * SwiftUI `Color` * * ```swift * @frozen struct Color * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ * Renders the value as a view. */ @HiddenFromObjC @Composable public fun ColorView(color: Color, modifier: Modifier = Modifier) { SwiftView(31, listOf(color), modifier) } /** * SwiftUI `ColorPicker.init(selection:supportsOpacity:label:)` * * ```swift * nonisolated init(selection: Binding<Color>, supportsOpacity: Bool = true, @ContentBuilder label: () -> Label) * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS: unavailable (no-op), watchOS: unavailable (no-op) */ @HiddenFromObjC @Composable public fun ColorPicker(selection: Color, modifier: Modifier = Modifier, supportsOpacity: Boolean = true, onSelectionChange: (Color) -> Unit, content: @Composable () -> Unit,)
ColorPicker(title: String, selection: Color, modifier: Modifier = Modifier, supportsOpacity: Boolean = true, onSelectionChange: (Color) -> Unit,)
ConcentricRectangleView(concentricRectangle: ConcentricRectangle, modifier: Modifier = Modifier) { SwiftView(36, listOf(concentricRectangle), modifier) } /** * SwiftUI `ContainerRelativeShape` * * ```swift * @frozen struct ContainerRelativeShape * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ * Renders the value as a view. */ @HiddenFromObjC @Composable public fun ContainerRelativeShapeView(containerRelativeShape: ContainerRelativeShape, modifier: Modifier = Modifier) { SwiftView(37, listOf(containerRelativeShape), modifier) } /** * SwiftUI `ContentUnavailableView.init(label:description:actions:)` * * ```swift * nonisolated init(@ContentBuilder label: () -> Label, @ContentBuilder description: () -> Description = { EmptyView() }, @ContentBuilder actions: () -> Actions = { EmptyView() }) * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ */ @HiddenFromObjC @Composable public fun ContentUnavailableView(modifier: Modifier = Modifier, label: @Composable () -> Unit, description: @Composable () -> Unit, actions: @Composable () -> Unit,)
ControlGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit, label: @Composable () -> Unit,)
ControlGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
ControlGroup(title: String, systemImage: SFSymbol, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
ControlGroup(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
DatePicker(selection: Double, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit, content: @Composable () -> Unit,)
DatePicker(title: String, selection: Double, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit,)
DefaultDocumentGroupLaunchActions(modifier: Modifier = Modifier,)
DefaultGlassEffectShapeView(defaultGlassEffectShape: DefaultGlassEffectShape, modifier: Modifier = Modifier) { SwiftView(65, listOf(defaultGlassEffectShape), modifier) } /** * SwiftUI `DisclosureGroup.init(isExpanded:content:label:)` * * ```swift * nonisolated init(isExpanded: Binding<Bool>, @ContentBuilder content: @escaping () -> Content, @ContentBuilder label: () -> Label) * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS: unavailable (no-op), watchOS: unavailable (no-op) */ @HiddenFromObjC @Composable public fun DisclosureGroup(isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit, label: @Composable () -> Unit,)
DisclosureGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit, label: @Composable () -> Unit,)
DisclosureGroup(label: String, isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
DisclosureGroup(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Divider(modifier: Modifier = Modifier,)
EditButton(modifier: Modifier = Modifier,)
EllipseView(ellipse: Ellipse, modifier: Modifier = Modifier) { SwiftView(76, listOf(ellipse), modifier) } /** * SwiftUI `EllipticalGradient` * * ```swift * @frozen nonisolated struct EllipticalGradient * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ * Renders the value as a view. */ @HiddenFromObjC @Composable public fun EllipticalGradientView(ellipticalGradient: EllipticalGradient, modifier: Modifier = Modifier) { SwiftView(77, listOf(ellipticalGradient), modifier) } /** * SwiftUI `EmptyView.init()` * * ```swift * nonisolated init() * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ */ @HiddenFromObjC @Composable public fun EmptyView(modifier: Modifier = Modifier,)
Form(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Gauge(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit, markedValueLabels: @Composable () -> Unit,)
Gauge(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit, markedValueLabels: @Composable () -> Unit,)
Gauge(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit,)
Gauge(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Gauge(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
GlassEffectContainer(modifier: Modifier = Modifier, spacing: Double? = null, content: @Composable () -> Unit,)
Grid(modifier: Modifier = Modifier, alignment: Alignment = Alignment.center, horizontalSpacing: Double? = null, verticalSpacing: Double? = null, content: @Composable () -> Unit,)
GridRow(modifier: Modifier = Modifier, alignment: VerticalAlignment? = null, content: @Composable () -> Unit,)
Group(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
GroupBox(modifier: Modifier = Modifier, content: @Composable () -> Unit, label: @Composable () -> Unit,)
GroupBox(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
GroupBox(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
HSplitView(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
HStack(modifier: Modifier = Modifier, alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, content: @Composable () -> Unit,)
HelpLink(destination: String, modifier: Modifier = Modifier,)
HelpLink(modifier: Modifier = Modifier, action: () -> Unit,)
Image(decorative: String, variableValue: Double?, modifier: Modifier = Modifier,)
Image(decorative: String, modifier: Modifier = Modifier,)
Image(systemName: SFSymbol, variableValue: Double?, modifier: Modifier = Modifier,)
Image(systemName: SFSymbol, modifier: Modifier = Modifier,)
ImageVariableValue(name: String, variableValue: Double?, modifier: Modifier = Modifier,)
Label(modifier: Modifier = Modifier, title: @Composable () -> Unit, icon: @Composable () -> Unit,)
Label(title: String, systemImage: SFSymbol, modifier: Modifier = Modifier,)
LabeledContent(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
LabeledContent(title: String, value: String, modifier: Modifier = Modifier,)
LabeledContent(modifier: Modifier = Modifier, content: @Composable () -> Unit, label: @Composable () -> Unit,)
LazyHGrid(rows: List<GridItem>, modifier: Modifier = Modifier, alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, pinnedViews: PinnedScrollableViews? = null, content: @Composable () -> Unit,)
LazyHStack(modifier: Modifier = Modifier, alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, pinnedViews: PinnedScrollableViews? = null, content: @Composable () -> Unit,)
LazyVGrid(columns: List<GridItem>, modifier: Modifier = Modifier, alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null, pinnedViews: PinnedScrollableViews? = null, content: @Composable () -> Unit,)
LazyVStack(modifier: Modifier = Modifier, alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null, pinnedViews: PinnedScrollableViews? = null, content: @Composable () -> Unit,)
LinearGradientView(linearGradient: LinearGradient, modifier: Modifier = Modifier) { SwiftView(130, listOf(linearGradient), modifier) } /** * SwiftUI `Link.init(destination:label:)` * * ```swift * @MainActor @preconcurrency init(destination: URL, @ContentBuilder label: () -> Label) * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ */ @HiddenFromObjC @Composable public fun Link(destination: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Link(title: String, destination: String, modifier: Modifier = Modifier,)
List(selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
List(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Menu(modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit, label: @Composable () -> Unit,)
Menu(modifier: Modifier = Modifier, content: @Composable () -> Unit, label: @Composable () -> Unit,)
Menu(title: String, systemImage: SFSymbol, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Menu(title: String, modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit,)
Menu(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
MenuButton(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
NavigationLink(modifier: Modifier = Modifier, destination: @Composable () -> Unit, label: @Composable () -> Unit,)
NavigationLink(destinationName: String, tag: Any, selection: Any?, modifier: Modifier = Modifier, onSelectionChange: (Any?) -> Unit, content: @Composable () -> Unit,)
NavigationLink(destinationName: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
NavigationLink(destinationName: String, isActive: Boolean, modifier: Modifier = Modifier, onIsActiveChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
NavigationLink(value: Any?, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
NavigationLinkValue(value: Any?, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
NavigationLink(title: String, value: Any?, modifier: Modifier = Modifier,)
NavigationLinkValue(title: String, value: Any?, modifier: Modifier = Modifier,)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility) -> Unit, onPreferredCompactColumnChange: (NavigationSplitViewColumn) -> Unit, sidebar: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility) -> Unit, onPreferredCompactColumnChange: (NavigationSplitViewColumn) -> Unit, sidebar: @Composable () -> Unit, content: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility) -> Unit, sidebar: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility) -> Unit, sidebar: @Composable () -> Unit, content: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationSplitView(preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onPreferredCompactColumnChange: (NavigationSplitViewColumn) -> Unit, sidebar: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationSplitView(preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onPreferredCompactColumnChange: (NavigationSplitViewColumn) -> Unit, sidebar: @Composable () -> Unit, content: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationSplitView(modifier: Modifier = Modifier, sidebar: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationSplitView(modifier: Modifier = Modifier, sidebar: @Composable () -> Unit, content: @Composable () -> Unit, detail: @Composable () -> Unit,)
NavigationStack(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
NavigationView(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
OffsetShape(shape: Shape, offset: CGSize, modifier: Modifier = Modifier,)
PathView(path: Path, modifier: Modifier = Modifier) { SwiftView(223, listOf(path), modifier) } /** * SwiftUI `Picker.init(selection:content:label:currentValueLabel:)` * * ```swift * nonisolated init(selection: Binding<SelectionValue>, @ContentBuilder content: () -> Content, @ContentBuilder label: () -> Label, @ContentBuilder currentValueLabel: () -> some View) * ``` * Availability: iOS 18.0+, macOS 15.0+, tvOS 18.0+, watchOS 11.0+ */ @HiddenFromObjC @Composable public fun Picker(selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Picker(selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, label: @Composable () -> Unit,)
Picker(title: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Picker(title: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
Picker(title: String, systemImage: SFSymbol, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Picker(title: String, systemImage: SFSymbol, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
ProgressView(value: Double?, modifier: Modifier = Modifier, total: Double = 1.0, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
ProgressView(value: Double?, modifier: Modifier = Modifier, total: Double = 1.0, content: @Composable () -> Unit,)
ProgressView(value: Double?, modifier: Modifier = Modifier, total: Double = 1.0,)
ProgressView(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
ProgressView(modifier: Modifier = Modifier,)
ProgressView(title: String, modifier: Modifier = Modifier,)
ProgressView(title: String, value: Double?, modifier: Modifier = Modifier, total: Double = 1.0,)
RadialGradientView(radialGradient: RadialGradient, modifier: Modifier = Modifier) { SwiftView(259, listOf(radialGradient), modifier) } /** * SwiftUI `Rectangle` * * ```swift * @frozen struct Rectangle * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ * Renders the value as a view. */ @HiddenFromObjC @Composable public fun RectangleView(rectangle: Rectangle, modifier: Modifier = Modifier) { SwiftView(260, listOf(rectangle), modifier) } /** * SwiftUI `RenameButton.init()` * * ```swift * nonisolated init() where Label == Label<Text, Image> * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ */ @HiddenFromObjC @Composable public fun RenameButton(modifier: Modifier = Modifier,)
RotatedShape(shape: Shape, angle: Angle, modifier: Modifier = Modifier, anchor: UnitPoint = UnitPoint.center,)
RoundedRectangleView(roundedRectangle: RoundedRectangle, modifier: Modifier = Modifier) { SwiftView(263, listOf(roundedRectangle), modifier) } /** * SwiftUI `ScaledShape.init(shape:scale:anchor:)` * * ```swift * init(shape: Content, scale: CGSize, anchor: UnitPoint = .center) * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ */ @HiddenFromObjC @Composable public fun ScaledShape(shape: Shape, scale: CGSize, modifier: Modifier = Modifier, anchor: UnitPoint = UnitPoint.center,)
ScrollView(modifier: Modifier = Modifier, axes: Axis.Set = Axis.Set.vertical, showsIndicators: Boolean = true, content: @Composable () -> Unit,)
ScrollView(modifier: Modifier = Modifier, axes: Axis.Set = Axis.Set.vertical, content: @Composable () -> Unit,)
Section(modifier: Modifier = Modifier, content: @Composable () -> Unit, header: @Composable () -> Unit, footer: @Composable () -> Unit,)
Section(title: String, isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Section(isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit, header: @Composable () -> Unit,)
Section(modifier: Modifier = Modifier, content: @Composable () -> Unit, header: @Composable () -> Unit,)
Section(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
SecureField(title: String, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onCommit: () -> Unit,)
SecureField(title: String, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
SettingsLink(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
SettingsLink(modifier: Modifier = Modifier,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, neutralValue: Double? = null, enabledBounds: ClosedFloatingPointRange<Double>? = null, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, step: Double = 1.0, neutralValue: Double? = null, enabledBounds: ClosedFloatingPointRange<Double>? = null, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, step: Double = 1.0, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, label: @Composable () -> Unit, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, label: @Composable () -> Unit, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, step: Double = 1.0, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
SliderValue(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, step: Double = 1.0, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
SliderValue(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Double) -> Unit,)
Slider(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, step: Double = 1.0, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Double) -> Unit,)
Spacer(modifier: Modifier = Modifier, minLength: Double? = null,)
Stepper(modifier: Modifier = Modifier, onIncrement: (() -> Unit)?, onDecrement: (() -> Unit)?, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
StepperOnIncrement(modifier: Modifier = Modifier, onIncrement: (() -> Unit)?, onDecrement: (() -> Unit)?, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
Stepper(value: Int, bounds: IntRange, modifier: Modifier = Modifier, step: Int = 1, onValueChange: (Int) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
StepperValue(value: Int, bounds: IntRange, modifier: Modifier = Modifier, step: Int = 1, onValueChange: (Int) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
Stepper(value: Int, modifier: Modifier = Modifier, step: Int = 1, onValueChange: (Int) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
StepperValue(value: Int, modifier: Modifier = Modifier, step: Int = 1, onValueChange: (Int) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit,)
Stepper(title: String, modifier: Modifier = Modifier, onIncrement: (() -> Unit)?, onEditingChanged: (Boolean) -> Unit = {}, onDecrement: (() -> Unit)?,)
Stepper(title: String, value: Int, bounds: IntRange, modifier: Modifier = Modifier, step: Int = 1, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Int) -> Unit,)
Stepper(title: String, value: Int, modifier: Modifier = Modifier, step: Int = 1, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Int) -> Unit,)
TabView(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Text(verbatim: String, modifier: Modifier = Modifier,)
Text(date: Double, style: Text.DateStyle, modifier: Modifier = Modifier,)
TextEditor(text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextField(title: String, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onEditingChanged: (Boolean) -> Unit, onCommit: () -> Unit,)
TextField(title: String, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onEditingChanged: (Boolean) -> Unit,)
TextField(title: String, text: String, axis: Axis, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextField(title: String, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onCommit: () -> Unit,)
TextField(title: String, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextInputBorderShapeView(textInputBorderShape: TextInputBorderShape, modifier: Modifier = Modifier) { SwiftView(369, listOf(textInputBorderShape), modifier) } /** * SwiftUI `Toggle.init(isOn:label:)` * * ```swift * nonisolated init(isOn: Binding<Bool>, @ContentBuilder label: () -> Label) * ``` * Availability: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ */ @HiddenFromObjC @Composable public fun Toggle(isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
Toggle(title: String, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
Toggle(title: String, systemImage: SFSymbol, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
TransformedShape(shape: Shape, transform: CGAffineTransform, modifier: Modifier = Modifier,)
UnevenRoundedRectangleView(unevenRoundedRectangle: UnevenRoundedRectangle, modifier: Modifier = Modifier) { SwiftView(381, listOf(unevenRoundedRectangle), modifier) } /** * SwiftUI `VSplitView.init(content:)` * * ```swift * nonisolated init(@ContentBuilder content: () -> Content) * ``` * Availability: iOS: unavailable (no-op), macOS 14.0+, tvOS: unavailable (no-op), watchOS: unavailable (no-op) */ @HiddenFromObjC @Composable public fun VSplitView(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
VStack(modifier: Modifier = Modifier, alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null, content: @Composable () -> Unit,)
ViewThatFits(modifier: Modifier = Modifier, axes: Axis.Set? = null, content: @Composable () -> Unit,)
WindowVisibilityToggle(windowID: String, modifier: Modifier = Modifier,)
ZStack(modifier: Modifier = Modifier, alignment: Alignment = Alignment.center, content: @Composable () -> Unit,)
Label(title: String, image: String, modifier: Modifier = Modifier,)
Button(titleResource: LocalizedKey, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
ButtonRole(titleKey: LocalizedKey, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
Button(titleResource: LocalizedKey, modifier: Modifier = Modifier, action: () -> Unit,)
ButtonAction(titleKey: LocalizedKey, modifier: Modifier = Modifier, action: () -> Unit,)
Button(titleResource: LocalizedKey, systemImage: SFSymbol, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
ButtonSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
Button(titleResource: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, action: () -> Unit,)
ButtonSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, action: () -> Unit,)
ColorPicker(titleResource: LocalizedKey, selection: Color, modifier: Modifier = Modifier, supportsOpacity: Boolean = true, onSelectionChange: (Color) -> Unit,)
ColorPickerSelection(titleKey: LocalizedKey, selection: Color, modifier: Modifier = Modifier, supportsOpacity: Boolean = true, onSelectionChange: (Color) -> Unit,)
ContentUnavailableView(title: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, description: String? = null,)
ContentUnavailableViewSystemImage(title: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, description: String? = null,)
ContentUnavailableView(title: String, systemImage: SFSymbol, modifier: Modifier = Modifier, description: String? = null,)
ControlGroup(titleResource: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
ControlGroupSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
ControlGroup(titleResource: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
DatePicker(titleResource: LocalizedKey, selection: Double, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit,)
DatePickerSelection(titleKey: LocalizedKey, selection: Double, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit,)
DisclosureGroup(titleResource: LocalizedKey, isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
DisclosureGroupIsExpanded(titleKey: LocalizedKey, isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
DisclosureGroup(titleResource: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
GroupBox(titleResource: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Image(name: String, variableValue: Double?, label: String, modifier: Modifier = Modifier,)
Image(name: String, label: String, modifier: Modifier = Modifier,)
Label(titleResource: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier,)
LabelSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier,)
LabeledContent(titleResource: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
LabeledContent(titleResource: LocalizedKey, value: String, modifier: Modifier = Modifier,)
LabeledContentValue(titleKey: LocalizedKey, value: String, modifier: Modifier = Modifier,)
Link(titleResource: LocalizedKey, destination: String, modifier: Modifier = Modifier,)
LinkDestination(titleKey: LocalizedKey, destination: String, modifier: Modifier = Modifier,)
Menu(titleResource: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit,)
MenuSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit,)
Menu(titleResource: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
MenuSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Menu(titleResource: LocalizedKey, modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit,)
MenuPrimaryAction(titleKey: LocalizedKey, modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit,)
Menu(titleResource: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
MenuButton(titleKey: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
NavigationLink(titleResource: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
NavigationLink(titleResource: LocalizedKey, value: Any?, modifier: Modifier = Modifier,)
NavigationLinkValue(titleResource: LocalizedKey, value: Any?, modifier: Modifier = Modifier,)
Picker(titleResource: LocalizedKey, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
PickerSelection(titleKey: LocalizedKey, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Picker(titleResource: LocalizedKey, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
PickerSelection(titleKey: LocalizedKey, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
Picker(titleResource: LocalizedKey, systemImage: SFSymbol, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
PickerSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Picker(titleResource: LocalizedKey, systemImage: SFSymbol, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
PickerSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
ProgressView(titleResource: LocalizedKey, modifier: Modifier = Modifier,)
ProgressView(titleResource: LocalizedKey, value: Double?, modifier: Modifier = Modifier, total: Double = 1.0,)
ProgressViewValue(titleKey: LocalizedKey, value: Double?, modifier: Modifier = Modifier, total: Double = 1.0,)
Section(titleResource: LocalizedKey, isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
SectionIsExpanded(titleKey: LocalizedKey, isExpanded: Boolean, modifier: Modifier = Modifier, onIsExpandedChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
Section(titleResource: LocalizedKey, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
SecureField(text: String, modifier: Modifier = Modifier, prompt: String? = null, onTextChange: (String) -> Unit, content: @Composable () -> Unit,)
SecureField(titleResource: LocalizedKey, text: String, prompt: String?, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
SecureFieldText(titleKey: LocalizedKey, text: String, prompt: String?, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
SecureField(title: String, text: String, prompt: String?, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
SecureField(titleKey: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onCommit: () -> Unit,)
SecureField(titleResource: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
SecureFieldText(titleKey: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
ShareLink(item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null,)
ShareLinkItem(item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null,)
ShareLink(titleKey: LocalizedKey, item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null,)
ShareLink(title: String, item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null,)
ShareLinkItem(titleResource: LocalizedKey, item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null,)
ShareLinkItem(title: String, item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null,)
ShareLink(item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null, content: @Composable () -> Unit,)
ShareLinkItem(item: String, modifier: Modifier = Modifier, subject: String? = null, message: String? = null, content: @Composable () -> Unit,)
Stepper(titleResource: LocalizedKey, modifier: Modifier = Modifier, onIncrement: (() -> Unit)?, onEditingChanged: (Boolean) -> Unit = {}, onDecrement: (() -> Unit)?,)
StepperOnIncrement(titleKey: LocalizedKey, modifier: Modifier = Modifier, onIncrement: (() -> Unit)?, onEditingChanged: (Boolean) -> Unit = {}, onDecrement: (() -> Unit)?,)
Stepper(titleResource: LocalizedKey, value: Int, bounds: IntRange, modifier: Modifier = Modifier, step: Int = 1, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Int) -> Unit,)
StepperValue(titleKey: LocalizedKey, value: Int, bounds: IntRange, modifier: Modifier = Modifier, step: Int = 1, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Int) -> Unit,)
Stepper(titleResource: LocalizedKey, value: Int, modifier: Modifier = Modifier, step: Int = 1, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Int) -> Unit,)
StepperValue(titleKey: LocalizedKey, value: Int, modifier: Modifier = Modifier, step: Int = 1, onEditingChanged: (Boolean) -> Unit = {}, onValueChange: (Int) -> Unit,)
Text(key: LocalizedKey, modifier: Modifier = Modifier, tableName: String? = null,)
Text(resource: LocalizedKey, modifier: Modifier = Modifier,)
TextField(text: String, axis: Axis, modifier: Modifier = Modifier, prompt: String? = null, onTextChange: (String) -> Unit, content: @Composable () -> Unit,)
TextField(text: String, modifier: Modifier = Modifier, prompt: String? = null, onTextChange: (String) -> Unit, content: @Composable () -> Unit,)
TextField(titleKey: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onEditingChanged: (Boolean) -> Unit, onCommit: () -> Unit,)
TextField(titleKey: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onEditingChanged: (Boolean) -> Unit,)
TextField(titleResource: LocalizedKey, text: String, axis: Axis, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextFieldText(titleKey: LocalizedKey, text: String, axis: Axis, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextField(titleResource: LocalizedKey, text: String, prompt: String?, axis: Axis, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextFieldText(titleKey: LocalizedKey, text: String, prompt: String?, axis: Axis, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextField(title: String, text: String, prompt: String?, axis: Axis, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextField(titleResource: LocalizedKey, text: String, prompt: String?, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextFieldText(titleKey: LocalizedKey, text: String, prompt: String?, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextField(title: String, text: String, prompt: String?, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextField(titleKey: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit, onCommit: () -> Unit,)
TextField(titleResource: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextFieldText(titleKey: LocalizedKey, text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
TextFieldLink(modifier: Modifier = Modifier, prompt: String? = null, onSubmit: (String) -> Unit, content: @Composable () -> Unit,)
TextFieldLink(titleResource: LocalizedKey, modifier: Modifier = Modifier, prompt: String? = null, onSubmit: (String) -> Unit,)
TextFieldLinkPrompt(titleKey: LocalizedKey, modifier: Modifier = Modifier, prompt: String? = null, onSubmit: (String) -> Unit,)
TextFieldLink(title: String, modifier: Modifier = Modifier, prompt: String? = null, onSubmit: (String) -> Unit,)
Toggle(titleResource: LocalizedKey, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
ToggleIsOn(titleKey: LocalizedKey, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
Toggle(titleResource: LocalizedKey, systemImage: SFSymbol, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
ToggleSystemImage(titleKey: LocalizedKey, systemImage: SFSymbol, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
ContentUnavailableView(title: LocalizedKey, image: String, modifier: Modifier = Modifier, description: String? = null,)
ContentUnavailableViewImage(title: LocalizedKey, image: String, modifier: Modifier = Modifier, description: String? = null,)
ContentUnavailableView(title: String, image: String, modifier: Modifier = Modifier, description: String? = null,)
Label(titleResource: LocalizedKey, image: String, modifier: Modifier = Modifier,)
LabelImage(titleKey: LocalizedKey, image: String, modifier: Modifier = Modifier,)
AnyView(modifier: Modifier = Modifier, content: @Composable () -> Unit,)
DatePicker(selection: Double, range: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit, content: @Composable () -> Unit,)
DatePicker(title: String, selection: Double, range: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit,)
FillShapeView(shape: Shape, style: ShapeStyle, fillStyle: FillStyle, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
MenuButton(modifier: Modifier = Modifier, label: @Composable () -> Unit, content: @Composable () -> Unit,)
NewDocumentButton(title: String, contentType: String, modifier: Modifier = Modifier,)
NewDocumentButton(title: String, contentType: String, source: DocumentCreationSource, modifier: Modifier = Modifier,)
NewDocumentButton(title: String, source: DocumentCreationSource, modifier: Modifier = Modifier, contentType: String? = null,)
NewDocumentButton(title: String, modifier: Modifier = Modifier, contentType: String? = null,)
NewDocumentButtonContentType(title: String, modifier: Modifier = Modifier, contentType: String? = null,)
PickerSelection(selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, label: @Composable () -> Unit, content: @Composable () -> Unit,)
ProgressView(timerInterval: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, countsDown: Boolean = true, label: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
ProgressView(timerInterval: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, countsDown: Boolean = true, content: @Composable () -> Unit,)
ProgressView(timerInterval: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, countsDown: Boolean = true,)
SliderValue(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit, label: @Composable () -> Unit,)
SliderValue(value: Double, bounds: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, step: Double = 1.0, onValueChange: (Double) -> Unit, onEditingChanged: (Boolean) -> Unit = {}, minimumValueLabel: @Composable () -> Unit, maximumValueLabel: @Composable () -> Unit, label: @Composable () -> Unit,)
StrokeBorderShapeView(shape: InsettableShape, style: ShapeStyle, strokeStyle: StrokeStyle, isAntialiased: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
StrokeShapeView(shape: Shape, style: ShapeStyle, strokeStyle: StrokeStyle, isAntialiased: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Text(timerInterval: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, pauseTime: Double? = null, countsDown: Boolean = true, showsHours: Boolean = true,)
Text(dates: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier,)
TextEditorText(text: String, modifier: Modifier = Modifier, onTextChange: (String) -> Unit,)
Button(title: String, image: String, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
Button(title: String, image: String, modifier: Modifier = Modifier, action: () -> Unit,)
ControlGroup(title: String, image: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
LabelImage(title: String, image: String, modifier: Modifier = Modifier,)
Menu(title: String, image: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Picker(title: String, image: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Picker(title: String, image: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
Toggle(title: String, image: String, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
DatePicker(titleResource: LocalizedKey, selection: Double, range: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit,)
DatePickerSelection(titleKey: LocalizedKey, selection: Double, range: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, displayedComponents: DatePickerComponents? = null, onSelectionChange: (Double) -> Unit,)
NewDocumentButton(title: LocalizedKey, contentType: String, modifier: Modifier = Modifier,)
NewDocumentButtonContentType(title: LocalizedKey, contentType: String, modifier: Modifier = Modifier,)
NewDocumentButton(title: LocalizedKey, contentType: String, source: DocumentCreationSource, modifier: Modifier = Modifier,)
NewDocumentButtonContentType(title: LocalizedKey, contentType: String, source: DocumentCreationSource, modifier: Modifier = Modifier,)
NewDocumentButton(title: LocalizedKey, source: DocumentCreationSource, modifier: Modifier = Modifier, contentType: String? = null,)
NewDocumentButtonContentType(title: LocalizedKey, source: DocumentCreationSource, modifier: Modifier = Modifier, contentType: String? = null,)
NewDocumentButton(source: DocumentCreationSource, modifier: Modifier = Modifier, label: String? = null, contentType: String? = null,)
NewDocumentButtonContentType(contentType: String, source: DocumentCreationSource, modifier: Modifier = Modifier, label: String? = null,)
NewDocumentButton(title: LocalizedKey, modifier: Modifier = Modifier, contentType: String? = null,)
NewDocumentButtonContentType(title: LocalizedKey, modifier: Modifier = Modifier, contentType: String? = null,)
NewDocumentButton(modifier: Modifier = Modifier, label: String? = null, contentType: String? = null,)
NewDocumentButtonContentType(modifier: Modifier = Modifier, label: String? = null, contentType: String? = null,)
Button(titleResource: LocalizedKey, image: String, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
ButtonImage(titleKey: LocalizedKey, image: String, role: ButtonRole?, modifier: Modifier = Modifier, action: () -> Unit,)
Button(titleResource: LocalizedKey, image: String, modifier: Modifier = Modifier, action: () -> Unit,)
ButtonImage(titleKey: LocalizedKey, image: String, modifier: Modifier = Modifier, action: () -> Unit,)
ControlGroup(titleResource: LocalizedKey, image: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
ControlGroupImage(titleKey: LocalizedKey, image: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Menu(titleResource: LocalizedKey, image: String, modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit,)
MenuImage(titleKey: LocalizedKey, image: String, modifier: Modifier = Modifier, primaryAction: () -> Unit, content: @Composable () -> Unit,)
Menu(titleResource: LocalizedKey, image: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
MenuImage(titleKey: LocalizedKey, image: String, modifier: Modifier = Modifier, content: @Composable () -> Unit,)
Picker(titleResource: LocalizedKey, image: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
PickerImage(titleKey: LocalizedKey, image: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit, currentValueLabel: @Composable () -> Unit,)
Picker(titleResource: LocalizedKey, image: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
PickerImage(titleKey: LocalizedKey, image: String, selection: Any, modifier: Modifier = Modifier, onSelectionChange: (Any) -> Unit, content: @Composable () -> Unit,)
Toggle(titleResource: LocalizedKey, image: String, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
ToggleImage(titleKey: LocalizedKey, image: String, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit,)
NavigationLink(tag: Any, selection: Any?, modifier: Modifier = Modifier, onSelectionChange: (Any?) -> Unit, destination: @Composable () -> Unit, label: @Composable () -> Unit,)
NavigationLink(isActive: Boolean, modifier: Modifier = Modifier, onIsActiveChange: (Boolean) -> Unit, destination: @Composable () -> Unit, label: @Composable () -> Unit,)
NavigationLinkTag(title: String, tag: Any, selection: Any?, modifier: Modifier = Modifier, onSelectionChange: (Any?) -> Unit, content: @Composable () -> Unit,)
NavigationLinkIsActive(title: String, isActive: Boolean, modifier: Modifier = Modifier, onIsActiveChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
NavigationLink(titleKey: LocalizedKey, tag: Any, selection: Any?, modifier: Modifier = Modifier, onSelectionChange: (Any?) -> Unit, content: @Composable () -> Unit,)
NavigationLink(titleKey: LocalizedKey, isActive: Boolean, modifier: Modifier = Modifier, onIsActiveChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
NavigationLinkTag(tag: Any, selection: Any?, modifier: Modifier = Modifier, onSelectionChange: (Any?) -> Unit, destination: @Composable () -> Unit, label: @Composable () -> Unit,)
NavigationLinkIsActive(isActive: Boolean, modifier: Modifier = Modifier, onIsActiveChange: (Boolean) -> Unit, destination: @Composable () -> Unit, label: @Composable () -> Unit,)
NavigationLinkTag(titleKey: LocalizedKey, tag: Any, selection: Any?, modifier: Modifier = Modifier, onSelectionChange: (Any?) -> Unit, content: @Composable () -> Unit,)
NavigationLinkIsActive(titleKey: LocalizedKey, isActive: Boolean, modifier: Modifier = Modifier, onIsActiveChange: (Boolean) -> Unit, content: @Composable () -> Unit,)
Button(titleKey: LocalizedKey? = null, role: ButtonRole? = null, systemImage: SFSymbol? = null, image: String? = null, modifier: Modifier = Modifier, action: (() -> Unit)? = null,)
ColorPicker(titleKey: LocalizedKey? = null, selection: Color? = null, supportsOpacity: Boolean? = null, modifier: Modifier = Modifier, onSelectionChange: ((Color) -> Unit)? = null,)
ContentUnavailableView(title: LocalizedKey? = null, systemImage: SFSymbol? = null, description: String? = null, image: String? = null, modifier: Modifier = Modifier,)
ControlGroup(titleKey: LocalizedKey? = null, systemImage: SFSymbol? = null, image: String? = null, modifier: Modifier = Modifier, content: (@Composable () -> Unit)? = null,)
DatePicker(titleKey: LocalizedKey? = null, selection: Double? = null, displayedComponents: DatePickerComponents? = null, range: ClosedFloatingPointRange<Double>? = null, modifier: Modifier = Modifier, onSelectionChange: ((Double) -> Unit)? = null,)
DisclosureGroup(titleKey: LocalizedKey? = null, isExpanded: Boolean? = null, modifier: Modifier = Modifier, onIsExpandedChange: ((Boolean) -> Unit)? = null, content: (@Composable () -> Unit)? = null,)
Image(name: String? = null, variableValue: Double? = null, modifier: Modifier = Modifier,)
Label(titleKey: LocalizedKey? = null, systemImage: SFSymbol? = null, image: String? = null, title: String? = null, modifier: Modifier = Modifier,)
LabeledContent(titleKey: LocalizedKey? = null, value: String? = null, modifier: Modifier = Modifier,)
Link(titleKey: LocalizedKey? = null, destination: String? = null, modifier: Modifier = Modifier,)
Menu(titleKey: LocalizedKey? = null, systemImage: SFSymbol? = null, image: String? = null, modifier: Modifier = Modifier, primaryAction: (() -> Unit)? = null, content: (@Composable () -> Unit)? = null,)
NavigationLink(value: Any? = null, title: String? = null, titleResource: LocalizedKey? = null, tag: Any? = null, selection: Any? = null, isActive: Boolean? = null, titleKey: LocalizedKey? = null, modifier: Modifier = Modifier, onSelectionChange: ((Any?) -> Unit)? = null, onIsActiveChange: ((Boolean) -> Unit)? = null, destination: (@Composable () -> Unit)? = null, label: (@Composable () -> Unit)? = null, content: (@Composable () -> Unit)? = null,)
NewDocumentButton(title: String? = null, contentType: String? = null, source: DocumentCreationSource? = null, label: String? = null, modifier: Modifier = Modifier,)
Picker(titleKey: LocalizedKey? = null, selection: Any? = null, systemImage: SFSymbol? = null, image: String? = null, modifier: Modifier = Modifier, onSelectionChange: ((Any) -> Unit)? = null, currentValueLabel: (@Composable () -> Unit)? = null, label: (@Composable () -> Unit)? = null, content: (@Composable () -> Unit)? = null,)
ProgressView(titleKey: LocalizedKey? = null, value: Double? = null, total: Double? = null, modifier: Modifier = Modifier,)
Section(titleKey: LocalizedKey? = null, isExpanded: Boolean? = null, modifier: Modifier = Modifier, onIsExpandedChange: ((Boolean) -> Unit)? = null, content: (@Composable () -> Unit)? = null,)
SecureField(titleKey: LocalizedKey? = null, text: String? = null, prompt: String? = null, modifier: Modifier = Modifier, onTextChange: ((String) -> Unit)? = null,)
ShareLink(item: String? = null, subject: String? = null, message: String? = null, titleResource: LocalizedKey? = null, title: String? = null, modifier: Modifier = Modifier, content: (@Composable () -> Unit)? = null,)
Slider(value: Double? = null, bounds: ClosedFloatingPointRange<Double>? = null, step: Double? = null, modifier: Modifier = Modifier, onValueChange: ((Double) -> Unit)? = null, onEditingChanged: ((Boolean) -> Unit)? = null, minimumValueLabel: (@Composable () -> Unit)? = null, maximumValueLabel: (@Composable () -> Unit)? = null, label: (@Composable () -> Unit)? = null, content: (@Composable () -> Unit)? = null,)
Stepper(value: Int? = null, bounds: IntRange? = null, step: Int? = null, titleKey: LocalizedKey? = null, modifier: Modifier = Modifier, onIncrement: ((() -> Unit)?)? = null, onDecrement: ((() -> Unit)?)? = null, onEditingChanged: ((Boolean) -> Unit)? = null, onValueChange: ((Int) -> Unit)? = null, content: (@Composable () -> Unit)? = null,)
TextEditor(text: String? = null, modifier: Modifier = Modifier, onTextChange: ((String) -> Unit)? = null,)
TextField(titleKey: LocalizedKey? = null, text: String? = null, axis: Axis? = null, prompt: String? = null, modifier: Modifier = Modifier, onTextChange: ((String) -> Unit)? = null,)
TextFieldLink(titleKey: LocalizedKey? = null, prompt: String? = null, modifier: Modifier = Modifier, onSubmit: ((String) -> Unit)? = null,)
Toggle(titleKey: LocalizedKey? = null, isOn: Boolean? = null, systemImage: SFSymbol? = null, image: String? = null, modifier: Modifier = Modifier, onIsOnChange: ((Boolean) -> Unit)? = null,)
AngularGradientView(angularGradient: AngularGradient, modifier: Modifier = Modifier)
AnyShapeView(anyShape: AnyShape, modifier: Modifier = Modifier)
ArrangementView(modifier: Modifier = Modifier, primary: @Composable ()
ButtonBorderShapeView(buttonBorderShape: ButtonBorderShape, modifier: Modifier = Modifier)
CapsuleView(capsule: Capsule, modifier: Modifier = Modifier)
CircleView(circle: Circle, modifier: Modifier = Modifier)
ColorView(color: Color, modifier: Modifier = Modifier)
ConcentricRectangleView(concentricRectangle: ConcentricRectangle, modifier: Modifier = Modifier)
ContainerRelativeShapeView(containerRelativeShape: ContainerRelativeShape, modifier: Modifier = Modifier)
ContentUnavailableView(modifier: Modifier = Modifier, label: @Composable ()
DefaultGlassEffectShapeView(defaultGlassEffectShape: DefaultGlassEffectShape, modifier: Modifier = Modifier)
EllipseView(ellipse: Ellipse, modifier: Modifier = Modifier)
EllipticalGradientView(ellipticalGradient: EllipticalGradient, modifier: Modifier = Modifier)
EmptyView(modifier: Modifier = Modifier)
HSplitView(modifier: Modifier = Modifier, content: @Composable ()
LinearGradientView(linearGradient: LinearGradient, modifier: Modifier = Modifier)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility)
NavigationSplitView(columnVisibility: NavigationSplitViewVisibility, modifier: Modifier = Modifier, onColumnVisibilityChange: (NavigationSplitViewVisibility)
NavigationSplitView(preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onPreferredCompactColumnChange: (NavigationSplitViewColumn)
NavigationSplitView(preferredCompactColumn: NavigationSplitViewColumn, modifier: Modifier = Modifier, onPreferredCompactColumnChange: (NavigationSplitViewColumn)
NavigationSplitView(modifier: Modifier = Modifier, sidebar: @Composable ()
NavigationSplitView(modifier: Modifier = Modifier, sidebar: @Composable ()
NavigationView(modifier: Modifier = Modifier, content: @Composable ()
PathView(path: Path, modifier: Modifier = Modifier)
ProgressView(value: Double?, modifier: Modifier = Modifier, total: Double = 1.0, label: @Composable ()
ProgressView(value: Double?, modifier: Modifier = Modifier, total: Double = 1.0, content: @Composable ()
ProgressView(value: Double?, modifier: Modifier = Modifier, total: Double = 1.0)
ProgressView(modifier: Modifier = Modifier, content: @Composable ()
ProgressView(modifier: Modifier = Modifier)
ProgressView(title: String, modifier: Modifier = Modifier)
ProgressView(title: String, value: Double?, modifier: Modifier = Modifier, total: Double = 1.0)
RadialGradientView(radialGradient: RadialGradient, modifier: Modifier = Modifier)
RectangleView(rectangle: Rectangle, modifier: Modifier = Modifier)
RoundedRectangleView(roundedRectangle: RoundedRectangle, modifier: Modifier = Modifier)
ScrollView(modifier: Modifier = Modifier, axes: Axis.Set = Axis.Set.vertical, showsIndicators: Boolean = true, content: @Composable ()
ScrollView(modifier: Modifier = Modifier, axes: Axis.Set = Axis.Set.vertical, content: @Composable ()
TabView(modifier: Modifier = Modifier, content: @Composable ()
TextInputBorderShapeView(textInputBorderShape: TextInputBorderShape, modifier: Modifier = Modifier)
UnevenRoundedRectangleView(unevenRoundedRectangle: UnevenRoundedRectangle, modifier: Modifier = Modifier)
VSplitView(modifier: Modifier = Modifier, content: @Composable ()
ContentUnavailableView(title: LocalizedKey, systemImage: SFSymbol, modifier: Modifier = Modifier, description: String? = null)
ContentUnavailableView(title: String, systemImage: SFSymbol, modifier: Modifier = Modifier, description: String? = null)
ProgressView(titleResource: LocalizedKey, modifier: Modifier = Modifier)
ProgressView(titleResource: LocalizedKey, value: Double?, modifier: Modifier = Modifier, total: Double = 1.0)
ContentUnavailableView(title: LocalizedKey, image: String, modifier: Modifier = Modifier, description: String? = null)
ContentUnavailableView(title: String, image: String, modifier: Modifier = Modifier, description: String? = null)
AnyView(modifier: Modifier = Modifier, content: @Composable ()
FillShapeView(shape: Shape, style: ShapeStyle, fillStyle: FillStyle, modifier: Modifier = Modifier, content: @Composable ()
ProgressView(timerInterval: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, countsDown: Boolean = true, label: @Composable ()
ProgressView(timerInterval: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, countsDown: Boolean = true, content: @Composable ()
ProgressView(timerInterval: ClosedFloatingPointRange<Double>, modifier: Modifier = Modifier, countsDown: Boolean = true)
StrokeBorderShapeView(shape: InsettableShape, style: ShapeStyle, strokeStyle: StrokeStyle, isAntialiased: Boolean, modifier: Modifier = Modifier, content: @Composable ()
StrokeShapeView(shape: Shape, style: ShapeStyle, strokeStyle: StrokeStyle, isAntialiased: Boolean, modifier: Modifier = Modifier, content: @Composable ()
ContentUnavailableView(title: LocalizedKey? = null, systemImage: SFSymbol? = null, description: String? = null, image: String? = null, modifier: Modifier = Modifier)
ProgressView(titleKey: LocalizedKey? = null, value: Double? = null, total: Double? = null, modifier: Modifier = Modifier)
ShapeView(shape: Shape, modifier: Modifier = Modifier, fill: ShapeStyle? = null, stroke: ShapeStyle? = null, lineWidth: Double = 1.0, strokeStyle: StrokeStyle? = null, trimFrom: Double = 0.0, trimTo: Double = 1.0,)
```

## Modifiers

```kotlin
Modifier.accentColor(accentColor: Color?)
Modifier.accessibility(activationPoint: CGPoint)
Modifier.accessibility(activationPoint: UnitPoint)
Modifier.accessibility(addTraits: AccessibilityTraits)
Modifier.accessibility(hidden: Boolean)
Modifier.accessibility(identifier: String)
Modifier.accessibility(removeTraits: AccessibilityTraits? = null)
Modifier.accessibility(sortPriority: Double)
Modifier.accessibilityAction(action: () -> Unit, label: @Composable () -> Unit)
Modifier.accessibilityAction(actionKind: AccessibilityActionKind = AccessibilityActionKind.default, handler: () -> Unit)
Modifier.accessibilityAction(named: LocalizedKey, handler: () -> Unit)
Modifier.accessibilityAction(named: String, handler: () -> Unit)
Modifier.accessibilityAction(named: String? = null, handler: (() -> Unit)? = null)
Modifier.accessibilityActionNamed(named: LocalizedKey, handler: () -> Unit)
Modifier.accessibilityActionNamed(named: String, handler: () -> Unit)
Modifier.accessibilityActions(category: AccessibilityActionCategory, content: @Composable () -> Unit)
Modifier.accessibilityActions(content: @Composable () -> Unit)
Modifier.accessibilityActivationPoint(activationPoint: CGPoint)
Modifier.accessibilityActivationPoint(activationPoint: CGPoint, isEnabled: Boolean)
Modifier.accessibilityActivationPoint(activationPoint: UnitPoint)
Modifier.accessibilityActivationPoint(activationPoint: UnitPoint, isEnabled: Boolean)
Modifier.accessibilityAddTraits(traits: AccessibilityTraits)
Modifier.accessibilityAdjustableAction(handler: (AccessibilityAdjustmentDirection) -> Unit)
Modifier.accessibilityChildren(children: @Composable () -> Unit)
Modifier.accessibilityCustomContent(key: AccessibilityCustomContentKey, value: String)
Modifier.accessibilityCustomContent(key: AccessibilityCustomContentKey, valueKey: LocalizedKey)
Modifier.accessibilityCustomContent(label: String, value: String)
Modifier.accessibilityCustomContent(labelKey: LocalizedKey, value: String)
Modifier.accessibilityCustomContent(labelKey: LocalizedKey, valueKey: LocalizedKey)
Modifier.accessibilityDirectTouch(isDirectTouchArea: Boolean = true, options: AccessibilityDirectTouchOptions? = null)
Modifier.accessibilityDragPoint(point: UnitPoint, description: LocalizedKey)
Modifier.accessibilityDragPoint(point: UnitPoint, description: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityDragPoint(point: UnitPoint, description: String)
Modifier.accessibilityDragPoint(point: UnitPoint, description: String, isEnabled: Boolean)
Modifier.accessibilityDragPoint(point: UnitPoint? = null, description: String? = null, isEnabled: Boolean? = null)
Modifier.accessibilityDragPointDescription(point: UnitPoint, description: LocalizedKey)
Modifier.accessibilityDragPointDescription(point: UnitPoint, description: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityDragPointDescription(point: UnitPoint, description: String)
Modifier.accessibilityDragPointDescription(point: UnitPoint, description: String, isEnabled: Boolean)
Modifier.accessibilityDropPoint(point: UnitPoint, description: LocalizedKey)
Modifier.accessibilityDropPoint(point: UnitPoint, description: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityDropPoint(point: UnitPoint, description: String)
Modifier.accessibilityDropPoint(point: UnitPoint, description: String, isEnabled: Boolean)
Modifier.accessibilityDropPoint(point: UnitPoint? = null, description: String? = null, isEnabled: Boolean? = null)
Modifier.accessibilityDropPointDescription(point: UnitPoint, description: LocalizedKey)
Modifier.accessibilityDropPointDescription(point: UnitPoint, description: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityDropPointDescription(point: UnitPoint, description: String)
Modifier.accessibilityDropPointDescription(point: UnitPoint, description: String, isEnabled: Boolean)
Modifier.accessibilityElement(children: AccessibilityChildBehavior = AccessibilityChildBehavior.ignore)
Modifier.accessibilityHeading(level: AccessibilityHeadingLevel)
Modifier.accessibilityHidden(hidden: Boolean)
Modifier.accessibilityHidden(hidden: Boolean, isEnabled: Boolean)
Modifier.accessibilityHint(hint: String)
Modifier.accessibilityHint(hint: String, isEnabled: Boolean)
Modifier.accessibilityHint(hint: String? = null, isEnabled: Boolean? = null)
Modifier.accessibilityHint(hintKey: LocalizedKey)
Modifier.accessibilityHint(hintKey: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityHintIsEnabled(hint: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityHintIsEnabled(hint: String, isEnabled: Boolean)
Modifier.accessibilityIdentifier(identifier: String)
Modifier.accessibilityIdentifier(identifier: String, isEnabled: Boolean)
Modifier.accessibilityIgnoresInvertColors(active: Boolean = true)
Modifier.accessibilityInputLabels(inputLabels: List<String>)
Modifier.accessibilityInputLabels(inputLabels: List<String>, isEnabled: Boolean)
Modifier.accessibilityLabel(label: String)
Modifier.accessibilityLabel(label: String, isEnabled: Boolean)
Modifier.accessibilityLabel(label: String? = null, isEnabled: Boolean? = null)
Modifier.accessibilityLabel(labelKey: LocalizedKey)
Modifier.accessibilityLabel(labelKey: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityLabelIsEnabled(label: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityLabelIsEnabled(label: String, isEnabled: Boolean)
Modifier.accessibilityQuickAction(style: AccessibilityQuickActionStyle, content: @Composable () -> Unit)
Modifier.accessibilityQuickAction(style: AccessibilityQuickActionStyle, isActive: Boolean, onIsActiveChange: (Boolean) -> Unit, content: @Composable () -> Unit)
Modifier.accessibilityRemoveTraits(removeTraits: AccessibilityTraits)
Modifier.accessibilityRepresentation(representation: @Composable () -> Unit)
Modifier.accessibilityRespondsToUserInteraction(respondsToUserInteraction: Boolean = true)
Modifier.accessibilityRespondsToUserInteraction(respondsToUserInteraction: Boolean, isEnabled: Boolean)
Modifier.accessibilityScrollAction(handler: (Edge) -> Unit)
Modifier.accessibilityScrollStatus(status: String, isEnabled: Boolean = true)
Modifier.accessibilityScrollStatus(status: String? = null, isEnabled: Boolean? = null)
Modifier.accessibilityScrollStatus(statusKey: LocalizedKey, isEnabled: Boolean = true)
Modifier.accessibilityScrollStatusIsEnabled(status: LocalizedKey, isEnabled: Boolean = true)
Modifier.accessibilityScrollStatusIsEnabled(status: String, isEnabled: Boolean = true)
Modifier.accessibilityShowsLargeContentViewer()
Modifier.accessibilityShowsLargeContentViewer(largeContentView: @Composable () -> Unit)
Modifier.accessibilitySortPriority(sortPriority: Double)
Modifier.accessibilityTextContentType(value: AccessibilityTextContentType)
Modifier.accessibilityValue(value: String)
Modifier.accessibilityValue(value: String, isEnabled: Boolean)
Modifier.accessibilityValue(valueDescription: String? = null, isEnabled: Boolean? = null, valueResource: LocalizedKey? = null)
Modifier.accessibilityValue(valueKey: LocalizedKey)
Modifier.accessibilityValue(valueKey: LocalizedKey, isEnabled: Boolean)
Modifier.accessibilityValueIsEnabled(valueDescription: String, isEnabled: Boolean)
Modifier.accessibilityValueIsEnabled(valueResource: LocalizedKey, isEnabled: Boolean)
Modifier.alert(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit)
Modifier.alert(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.alert(titleKey: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit)
Modifier.alert(titleKey: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.alert(titleResource: LocalizedKey? = null, isPresented: Boolean? = null, title: String? = null, onIsPresentedChange: ((Boolean) -> Unit)? = null, actions: (@Composable () -> Unit)? = null, message: (@Composable () -> Unit)? = null)
Modifier.alertIsPresented(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit)
Modifier.alertIsPresented(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.alertIsPresented(titleResource: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit)
Modifier.alertIsPresented(titleResource: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.allowedDynamicRange(range: Image.DynamicRange?)
Modifier.allowsHitTesting(enabled: Boolean)
Modifier.allowsTightening(flag: Boolean)
Modifier.allowsWindowActivationEvents()
Modifier.allowsWindowActivationEvents(value: Boolean?)
Modifier.alternatingRowBackgrounds(behavior: AlternatingRowBackgroundBehavior = AlternatingRowBackgroundBehavior.enabled)
Modifier.animation(animation: Animation?)
Modifier.animation(animation: Animation?, value: Any)
Modifier.antialiased(isAntialiased: Boolean)
Modifier.arrangementViewStyle(style: ArrangementViewStyle)
Modifier.aspectRatio(aspectRatio: CGSize, contentMode: ContentMode)
Modifier.aspectRatio(aspectRatio: Double? = null, contentMode: ContentMode)
Modifier.assistiveAccessNavigationIcon(systemImage: SFSymbol)
Modifier.autocorrectionDisabled(disable: Boolean = true)
Modifier.background(alignment: Alignment = Alignment.center, content: @Composable () -> Unit)
Modifier.background(background: @Composable () -> Unit, alignment: Alignment = Alignment.center)
Modifier.background(ignoresSafeAreaEdges: Edge.Set = Edge.Set.all)
Modifier.background(shape: InsettableShape, fillStyle: FillStyle = FillStyle())
Modifier.background(shape: Shape, fillStyle: FillStyle = FillStyle())
Modifier.background(style: ShapeStyle, ignoresSafeAreaEdges: Edge.Set = Edge.Set.all)
Modifier.background(style: ShapeStyle, shape: InsettableShape, fillStyle: FillStyle = FillStyle())
Modifier.background(style: ShapeStyle, shape: Shape, fillStyle: FillStyle = FillStyle())
Modifier.backgroundExtensionEffect()
Modifier.backgroundExtensionEffect(isEnabled: Boolean)
Modifier.backgroundStyle(style: ShapeStyle)
Modifier.badge(count: Int)
Modifier.badge(key: LocalizedKey?)
Modifier.badge(label: String?)
Modifier.badgeProminence(prominence: BadgeProminence)
Modifier.baselineOffset(baselineOffset: Double)
Modifier.blendMode(blendMode: BlendMode)
Modifier.blur(radius: Double, opaque: Boolean = false)
Modifier.bold()
Modifier.bold(isActive: Boolean = true)
Modifier.border(content: ShapeStyle, width: Double = 1.0)
Modifier.brightness(amount: Double)
Modifier.buttonBorderShape(shape: ButtonBorderShape)
Modifier.buttonRepeatBehavior(behavior: ButtonRepeatBehavior)
Modifier.buttonSizing(sizing: ButtonSizing)
Modifier.buttonStyle(style: PrimitiveButtonStyle)
Modifier.clipped(antialiased: Boolean = false)
Modifier.clipShape(shape: Shape, style: FillStyle = FillStyle())
Modifier.colorEffect(shader: Shader, isEnabled: Boolean = true)
Modifier.colorInvert()
Modifier.colorMultiply(color: Color)
Modifier.colorScheme(colorScheme: ColorScheme)
Modifier.compositingGroup()
Modifier.confirmationDialog(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit)
Modifier.confirmationDialog(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.confirmationDialog(titleKey: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit)
Modifier.confirmationDialog(titleKey: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.confirmationDialog(titleResource: LocalizedKey? = null, isPresented: Boolean? = null, titleVisibility: Visibility? = null, title: String? = null, onIsPresentedChange: ((Boolean) -> Unit)? = null, actions: (@Composable () -> Unit)? = null, message: (@Composable () -> Unit)? = null)
Modifier.confirmationDialogIsPresented(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit)
Modifier.confirmationDialogIsPresented(title: String, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.confirmationDialogIsPresented(titleResource: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit)
Modifier.confirmationDialogIsPresented(titleResource: LocalizedKey, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, titleVisibility: Visibility = Visibility.automatic, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.containerBackground(container: ContainerBackgroundPlacement, alignment: Alignment = Alignment.center, content: @Composable () -> Unit)
Modifier.containerBackground(style: ShapeStyle, container: ContainerBackgroundPlacement)
Modifier.containerCornerOffset(edges: Edge.Set, sizeToFit: Boolean = false)
Modifier.containerRelativeFrame(axes: Axis.Set, alignment: Alignment = Alignment.center)
Modifier.containerRelativeFrame(axes: Axis.Set, count: Int, span: Int = 1, spacing: Double, alignment: Alignment = Alignment.center)
Modifier.containerShape(shape: InsettableShape)
Modifier.containerShape(shape: RoundedRectangularShape)
Modifier.contentMargins(edges: Edge.Set = Edge.Set.all, insets: EdgeInsets, placement: ContentMarginPlacement = ContentMarginPlacement.automatic)
Modifier.contentMargins(edges: Edge.Set = Edge.Set.all, length: Double?, placement: ContentMarginPlacement = ContentMarginPlacement.automatic)
Modifier.contentMargins(guide: ContentMarginGuide, edges: Edge.Set = Edge.Set.all, alignment: Alignment? = null)
Modifier.contentMargins(length: Double, placement: ContentMarginPlacement = ContentMarginPlacement.automatic)
Modifier.contentShape(kind: ContentShapeKinds, shape: Shape, eoFill: Boolean = false)
Modifier.contentShape(shape: Shape, eoFill: Boolean = false)
Modifier.contentToolbar(placement: ContentToolbarPlacement, content: @Composable () -> Unit)
Modifier.contentTransition(transition: ContentTransition)
Modifier.contextMenu(menuItems: @Composable () -> Unit)
Modifier.contextMenu(menuItems: @Composable () -> Unit, preview: @Composable () -> Unit)
Modifier.contrast(amount: Double)
Modifier.controlGroupStyle(style: ControlGroupStyle)
Modifier.controlSize(controlSize: ControlSize)
Modifier.coordinateSpace(name: Any)
Modifier.coordinateSpace(name: NamedCoordinateSpace)
Modifier.cornerRadius(radius: Double, antialiased: Boolean = true)
Modifier.datePickerStyle(style: DatePickerStyle)
Modifier.defaultAdaptableTabBarPlacement(defaultPlacement: AdaptableTabBarPlacement = AdaptableTabBarPlacement.automatic)
Modifier.defaultHoverEffect(effect: CustomHoverEffect)
Modifier.defaultHoverEffect(effect: HoverEffect?)
Modifier.defaultScrollAnchor(anchor: UnitPoint?)
Modifier.defaultScrollAnchor(anchor: UnitPoint?, role: ScrollAnchorRole)
Modifier.defaultTabBarPlacement(defaultPlacement: AdaptableTabBarPlacement)
Modifier.defaultWheelPickerItemHeight(height: Double)
Modifier.defersSystemGestures(edges: Edge.Set)
Modifier.deleteDisabled(isDisabled: Boolean)
Modifier.dialogPreventsAppTermination(prevents: Boolean?)
Modifier.dialogSeverity(severity: DialogSeverity)
Modifier.dialogSuppressionToggle(isSuppressed: Boolean, onIsSuppressedChange: (Boolean) -> Unit)
Modifier.dialogSuppressionToggle(title: String, isSuppressed: Boolean, onIsSuppressedChange: (Boolean) -> Unit)
Modifier.dialogSuppressionToggle(titleKey: LocalizedKey, isSuppressed: Boolean, onIsSuppressedChange: (Boolean) -> Unit)
Modifier.dialogSuppressionToggle(titleResource: LocalizedKey? = null, isSuppressed: Boolean? = null, label: String? = null, onIsSuppressedChange: ((Boolean) -> Unit)? = null)
Modifier.dialogSuppressionToggleIsSuppressed(label: String, isSuppressed: Boolean, onIsSuppressedChange: (Boolean) -> Unit)
Modifier.dialogSuppressionToggleIsSuppressed(titleResource: LocalizedKey, isSuppressed: Boolean, onIsSuppressedChange: (Boolean) -> Unit)
Modifier.digitalCrownAccessory(content: @Composable () -> Unit)
Modifier.digitalCrownAccessory(visibility: Visibility)
Modifier.digitalCrownRotation(binding: Double, onBindingChange: (Double) -> Unit)
Modifier.digitalCrownRotation(binding: Double, onBindingChange: (Double) -> Unit, minValue: Double, through: Double, stride: Double? = null, sensitivity: DigitalCrownRotationalSensitivity = DigitalCrownRotationalSensitivity.high, isContinuous: Boolean = false, isHapticFeedbackEnabled: Boolean = true)
Modifier.digitalCrownRotation(binding: Double, onIdle: () -> Unit = {}, onBindingChange: (Double) -> Unit)
Modifier.digitalCrownRotation(binding: Double, onIdle: () -> Unit = {}, onBindingChange: (Double) -> Unit, minValue: Double, through: Double, sensitivity: DigitalCrownRotationalSensitivity = DigitalCrownRotationalSensitivity.high, isContinuous: Boolean = false, isHapticFeedbackEnabled: Boolean = true)
Modifier.digitalCrownRotation(detent: Double, onIdle: () -> Unit = {}, onDetentChange: (Double) -> Unit, minValue: Double, through: Double, stride: Double, sensitivity: DigitalCrownRotationalSensitivity = DigitalCrownRotationalSensitivity.high, isContinuous: Boolean = false, isHapticFeedbackEnabled: Boolean = true)
Modifier.digitalCrownRotation(detent: Int, onIdle: () -> Unit = {}, onDetentChange: (Int) -> Unit, minValue: Int, through: Int, stride: Int, sensitivity: DigitalCrownRotationalSensitivity = DigitalCrownRotationalSensitivity.high, isContinuous: Boolean = false, isHapticFeedbackEnabled: Boolean = true)
Modifier.disableAutocorrection(disable: Boolean?)
Modifier.disabled(disabled: Boolean)
Modifier.disclosureGroupStyle(style: DisclosureGroupStyle)
Modifier.dismissalConfirmationDialog(title: String, shouldPresent: Boolean, actions: @Composable () -> Unit)
Modifier.dismissalConfirmationDialog(title: String, shouldPresent: Boolean, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.dismissalConfirmationDialog(titleKey: LocalizedKey, shouldPresent: Boolean, actions: @Composable () -> Unit)
Modifier.dismissalConfirmationDialog(titleKey: LocalizedKey, shouldPresent: Boolean, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.dismissalConfirmationDialog(titleResource: LocalizedKey? = null, shouldPresent: Boolean? = null, title: String? = null, actions: (@Composable () -> Unit)? = null, message: (@Composable () -> Unit)? = null)
Modifier.dismissalConfirmationDialogShouldPresent(title: String, shouldPresent: Boolean, actions: @Composable () -> Unit)
Modifier.dismissalConfirmationDialogShouldPresent(title: String, shouldPresent: Boolean, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.dismissalConfirmationDialogShouldPresent(titleResource: LocalizedKey, shouldPresent: Boolean, actions: @Composable () -> Unit)
Modifier.dismissalConfirmationDialogShouldPresent(titleResource: LocalizedKey, shouldPresent: Boolean, actions: @Composable () -> Unit, message: @Composable () -> Unit)
Modifier.distortionEffect(shader: Shader, maxSampleOffset: CGSize, isEnabled: Boolean = true)
Modifier.documentLaunchSubtitle(subtitle: String)
Modifier.documentLaunchSubtitle(subtitleKey: LocalizedKey)
Modifier.documentLaunchTitle(title: String)
Modifier.documentLaunchTitle(titleKey: LocalizedKey)
Modifier.dragConfiguration(configuration: DragConfiguration)
Modifier.draggable(containerItemID: Any)
Modifier.dragPreviewsFormation(formation: DragDropPreviewsFormation)
Modifier.drawingGroup(opaque: Boolean = false, colorMode: ColorRenderingMode = ColorRenderingMode.nonLinear)
Modifier.dropPreviewsFormation(formation: DragDropPreviewsFormation)
Modifier.dynamicTypeSize(size: DynamicTypeSize)
Modifier.edgesIgnoringSafeArea(edges: Edge.Set)
Modifier.environment(allowedDynamicRange: Image.DynamicRange?)
Modifier.environment(appearsActive: Boolean)
Modifier.environment(backgroundMaterial: Material?)
Modifier.environment(backgroundProminence: BackgroundProminence)
Modifier.environment(backgroundStyle: AnyShapeStyle?)
Modifier.environment(badgeProminence: BadgeProminence)
Modifier.environment(colorScheme: ColorScheme)
Modifier.environment(contentTransition: ContentTransition)
Modifier.environment(controlActiveState: ControlActiveState)
Modifier.environment(controlSize: ControlSize)
Modifier.environment(displayScale: Double? = null, allowsTightening: Boolean? = null, isLuminanceReduced: Boolean? = null, minimumScaleFactor: Double? = null, horizontalSizeClass: UserInterfaceSizeClass? = null, accessibilityEnabled: Boolean? = null, isHoverEffectEnabled: Boolean? = null, contentTransitionAddsDrawingGroup: Boolean? = null, accessibilityPrefersCrossFadeTransitions: Boolean? = null, isEnabled: Boolean? = null, lineLimit: Int? = null, isSceneCaptured: Boolean? = null, isScrollEnabled: Boolean? = null, isFocusEffectEnabled: Boolean? = null, disableAutocorrection: Boolean? = null, defaultMinListRowHeight: Double? = null, menuIndicatorVisibility: Visibility? = null, defaultMinListHeaderHeight: Double? = null, defaultWheelPickerItemHeight: Double? = null, horizontalScrollBounceBehavior: ScrollBounceBehavior? = null, systemPrefersReducedResourceUsage: Boolean? = null, horizontalScrollIndicatorVisibility: ScrollIndicatorVisibility? = null, calendar: String? = null, timeZone: String? = null)
Modifier.environment(dynamicTypeSize: DynamicTypeSize)
Modifier.environment(font: Font?)
Modifier.environment(headerProminence: Prominence)
Modifier.environment(imageScale: Image.Scale)
Modifier.environment(labelsVisibility: Visibility)
Modifier.environment(layoutDirection: LayoutDirection)
Modifier.environment(legibilityWeight: LegibilityWeight?)
Modifier.environment(lineSpacing: Double)
Modifier.environment(locale: String)
Modifier.environment(materialActiveAppearance: MaterialActiveAppearance)
Modifier.environment(menuOrder: MenuOrder)
Modifier.environment(multilineTextAlignment: TextAlignment)
Modifier.environment(overlayArrangementZIndex: Int)
Modifier.environment(redactionReasons: RedactionReasons)
Modifier.environment(scenePhase: ScenePhase)
Modifier.environment(scrollDismissesKeyboardMode: ScrollDismissesKeyboardMode)
Modifier.environment(sidebarRowSize: SidebarRowSize)
Modifier.environment(sizeCategory: ContentSizeCategory)
Modifier.environment(splitArrangementAxis: Axis?)
Modifier.environment(symbolColorRenderingMode: SymbolColorRenderingMode?)
Modifier.environment(symbolRenderingMode: SymbolRenderingMode?)
Modifier.environment(symbolVariableValueMode: SymbolVariableValueMode?)
Modifier.environment(symbolVariants: SymbolVariants)
Modifier.environment(textCase: Text.Case?)
Modifier.environment(textSelectionAffinity: TextSelectionAffinity)
Modifier.environment(toolbarTitleAlignment: ToolbarTitleAlignment)
Modifier.environment(truncationMode: Text.TruncationMode)
Modifier.environment(verticalScrollBounceBehavior: ScrollBounceBehavior)
Modifier.environment(verticalScrollIndicatorVisibility: ScrollIndicatorVisibility)
Modifier.environment(verticalSizeClass: UserInterfaceSizeClass?)
Modifier.environment(worldTrackingLimitations: Set<WorldTrackingLimitation>)
Modifier.environmentAccessibilityEnabled(accessibilityEnabled: Boolean)
Modifier.environmentAccessibilityPrefersCrossFadeTransitions(accessibilityPrefersCrossFadeTransitions: Boolean)
Modifier.environmentAllowsTightening(allowsTightening: Boolean)
Modifier.environmentCalendar(calendar: String)
Modifier.environmentContentTransitionAddsDrawingGroup(contentTransitionAddsDrawingGroup: Boolean)
Modifier.environmentDefaultMinListHeaderHeight(defaultMinListHeaderHeight: Double?)
Modifier.environmentDefaultMinListRowHeight(defaultMinListRowHeight: Double)
Modifier.environmentDefaultWheelPickerItemHeight(defaultWheelPickerItemHeight: Double)
Modifier.environmentDisableAutocorrection(disableAutocorrection: Boolean?)
Modifier.environmentDisplayScale(displayScale: Double)
Modifier.environmentHorizontalScrollBounceBehavior(horizontalScrollBounceBehavior: ScrollBounceBehavior)
Modifier.environmentHorizontalScrollIndicatorVisibility(horizontalScrollIndicatorVisibility: ScrollIndicatorVisibility)
Modifier.environmentHorizontalSizeClass(horizontalSizeClass: UserInterfaceSizeClass?)
Modifier.environmentIsEnabled(isEnabled: Boolean)
Modifier.environmentIsFocusEffectEnabled(isFocusEffectEnabled: Boolean)
Modifier.environmentIsHoverEffectEnabled(isHoverEffectEnabled: Boolean)
Modifier.environmentIsLuminanceReduced(isLuminanceReduced: Boolean)
Modifier.environmentIsSceneCaptured(isSceneCaptured: Boolean)
Modifier.environmentIsScrollEnabled(isScrollEnabled: Boolean)
Modifier.environmentLineLimit(lineLimit: Int?)
Modifier.environmentMenuIndicatorVisibility(menuIndicatorVisibility: Visibility)
Modifier.environmentMinimumScaleFactor(minimumScaleFactor: Double)
Modifier.environmentSystemPrefersReducedResourceUsage(systemPrefersReducedResourceUsage: Boolean)
Modifier.environmentTimeZone(timeZone: String)
Modifier.fileDialogBrowserOptions(options: FileDialogBrowserOptions)
Modifier.fileDialogConfirmationLabel(label: String)
Modifier.fileDialogConfirmationLabel(labelKey: LocalizedKey)
Modifier.fileDialogCustomizationID(id: String)
Modifier.fileDialogDefaultDirectory(defaultDirectory: String?)
Modifier.fileDialogImportsUnresolvedAliases(imports: Boolean)
Modifier.fileDialogMessage(message: String)
Modifier.fileDialogMessage(messageKey: LocalizedKey)
Modifier.fileExporterFilenameLabel(label: String)
Modifier.fileExporterFilenameLabel(labelKey: LocalizedKey)
Modifier.findDisabled(isDisabled: Boolean = true)
Modifier.findNavigator(isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit)
Modifier.fixedSize()
Modifier.fixedSize(horizontal: Boolean, vertical: Boolean)
Modifier.flipsForRightToLeftLayoutDirection(enabled: Boolean)
Modifier.focusable(isFocusable: Boolean = true)
Modifier.focusable(isFocusable: Boolean = true, interactions: FocusInteractions)
Modifier.focusable(isFocusable: Boolean = true, onFocusChange: (Boolean) -> Unit = {})
Modifier.focusEffectDisabled(disabled: Boolean = true)
Modifier.focusSection()
Modifier.font(font: Font?)
Modifier.fontDesign(design: Font.Design?)
Modifier.fontWeight(weight: Font.Weight?)
Modifier.fontWidth(width: Font.Width?)
Modifier.foregroundColor(color: Color?)
Modifier.foregroundStyle(primary: ShapeStyle, secondary: ShapeStyle)
Modifier.foregroundStyle(primary: ShapeStyle, secondary: ShapeStyle, tertiary: ShapeStyle)
Modifier.foregroundStyle(style: ShapeStyle)
Modifier.formStyle(style: FormStyle)
Modifier.frame()
Modifier.frame(minWidth: Double? = null, idealWidth: Double? = null, maxWidth: Double? = null, minHeight: Double? = null, idealHeight: Double? = null, maxHeight: Double? = null, alignment: Alignment = Alignment.center)
Modifier.frame(width: Double? = null, height: Double? = null, alignment: Alignment = Alignment.center)
Modifier.fullScreenCover(isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, onDismiss: (() -> Unit)? = null, content: @Composable () -> Unit)
Modifier.gaugeStyle(style: GaugeStyle)
Modifier.geometryGroup()
Modifier.glassEffect(glass: Glass = Glass.regular, shape: Shape = DefaultGlassEffectShape())
Modifier.glassEffectTransition(transition: GlassEffectTransition)
Modifier.grayscale(amount: Double)
Modifier.gridCellAnchor(anchor: UnitPoint)
Modifier.gridCellColumns(count: Int)
Modifier.gridCellUnsizedAxes(axes: Axis.Set)
Modifier.gridColumnAlignment(guide: HorizontalAlignment)
Modifier.groupBoxStyle(style: GroupBoxStyle)
Modifier.handGestureShortcut(shortcut: HandGestureShortcut, isEnabled: Boolean = true)
Modifier.handlesExternalEvents(preferring: Set<String>, allowing: Set<String>)
Modifier.headerProminence(prominence: Prominence)
Modifier.help(text: String)
Modifier.help(textKey: LocalizedKey)
Modifier.hidden()
Modifier.horizontalRadioGroupLayout()
Modifier.hoverEffect(effect: CustomHoverEffect, isEnabled: Boolean = true)
Modifier.hoverEffect(effect: HoverEffect = HoverEffect.automatic)
Modifier.hoverEffect(effect: HoverEffect = HoverEffect.automatic, isEnabled: Boolean = true)
Modifier.hoverEffectDisabled(disabled: Boolean = true)
Modifier.hueRotation(angle: Angle)
Modifier.id(id: Any)
Modifier.ignoresSafeArea(regions: SafeAreaRegions = SafeAreaRegions.all, edges: Edge.Set = Edge.Set.all)
Modifier.ignoresSafeArea(regions: SafeAreaRegions = SafeAreaRegions.all, edges: Edge.Set = Edge.Set.all, alignment: Alignment?)
Modifier.imageScale(scale: Image.Scale)
Modifier.indexViewStyle(style: IndexViewStyle)
Modifier.inspector(isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, content: @Composable () -> Unit)
Modifier.inspectorColumnWidth(min: Double? = null, ideal: Double, max: Double? = null)
Modifier.inspectorColumnWidth(width: Double)
Modifier.interactionActivityTrackingTag(tag: String)
Modifier.interactiveDismissDisabled(isDisabled: Boolean = true)
Modifier.interpolation(interpolation: Image.Interpolation)
Modifier.invalidatableContent(invalidatable: Boolean = true)
Modifier.italic()
Modifier.italic(isActive: Boolean = true)
Modifier.kerning(kerning: Double)
Modifier.keyboardShortcut(key: KeyEquivalent, modifiers: EventModifiers = EventModifiers.command)
Modifier.keyboardShortcut(key: KeyEquivalent, modifiers: EventModifiers = EventModifiers.command, localization: KeyboardShortcut.Localization)
Modifier.keyboardShortcut(shortcut: KeyboardShortcut)
Modifier.labeledContentStyle(style: LabeledContentStyle)
Modifier.labelIconToTitleSpacing(value: Double)
Modifier.labelReservedIconWidth(value: Double)
Modifier.labelsHidden()
Modifier.labelStyle(style: LabelStyle)
Modifier.labelsVisibility(visibility: Visibility)
Modifier.layerEffect(shader: Shader, maxSampleOffset: CGSize, isEnabled: Boolean = true)
Modifier.layoutDirectionBehavior(behavior: LayoutDirectionBehavior)
Modifier.layoutPriority(value: Double)
Modifier.lineLimit(limit: Int, reservesSpace: Boolean)
Modifier.lineLimit(limit: IntRange)
Modifier.lineLimit(number: Int?)
Modifier.lineSpacing(lineSpacing: Double)
Modifier.listItemTint(tint: Color?)
Modifier.listItemTint(tint: ListItemTint?)
Modifier.listRowBackground(view: @Composable () -> Unit)
Modifier.listRowInsets(edges: Edge.Set = Edge.Set.all, length: Double?)
Modifier.listRowInsets(insets: EdgeInsets?)
Modifier.listRowPlatterColor(color: Color?)
Modifier.listRowSeparator(visibility: Visibility, edges: VerticalEdge.Set = VerticalEdge.Set.all)
Modifier.listRowSeparatorTint(color: Color?, edges: VerticalEdge.Set = VerticalEdge.Set.all)
Modifier.listRowSpacing(spacing: Double?)
Modifier.listSectionIndexVisibility(visibility: Visibility)
Modifier.listSectionMargins(edges: Edge.Set = Edge.Set.all, length: Double?)
Modifier.listSectionSeparator(visibility: Visibility, edges: VerticalEdge.Set = VerticalEdge.Set.all)
Modifier.listSectionSeparatorTint(color: Color?, edges: VerticalEdge.Set = VerticalEdge.Set.all)
Modifier.listSectionSpacing(spacing: Double)
Modifier.listSectionSpacing(spacing: ListSectionSpacing)
Modifier.listStyle(style: ListStyle)
Modifier.luminanceToAlpha()
Modifier.mask(alignment: Alignment = Alignment.center, mask: @Composable () -> Unit)
Modifier.mask(mask: @Composable () -> Unit)
Modifier.materialActiveAppearance(appearance: MaterialActiveAppearance)
Modifier.menuActionDismissBehavior(behavior: MenuActionDismissBehavior)
Modifier.menuButtonStyle(style: MenuButtonStyle)
Modifier.menuIndicator(visibility: Visibility)
Modifier.menuOrder(order: MenuOrder)
Modifier.menuStyle(style: MenuStyle)
Modifier.minimumScaleFactor(factor: Double)
Modifier.modifierKeyAlternate(modifiers: EventModifiers, alternate: @Composable () -> Unit)
Modifier.monospaced(isActive: Boolean = true)
Modifier.monospacedDigit()
Modifier.moveDisabled(isDisabled: Boolean)
Modifier.multilineTextAlignment(alignment: TextAlignment)
Modifier.multilineTextAlignment(strategy: Text.AlignmentStrategy)
Modifier.navigationBarBackButtonHidden(hidesBackButton: Boolean = true)
Modifier.navigationBarHidden(hidden: Boolean)
Modifier.navigationBarItems(leading: @Composable () -> Unit)
Modifier.navigationBarItems(leading: @Composable () -> Unit, trailing: @Composable () -> Unit)
Modifier.navigationBarItems(trailing: (@Composable () -> Unit)? = null)
Modifier.navigationBarItemsTrailing(trailing: @Composable () -> Unit)
Modifier.navigationBarTitle(title: String)
Modifier.navigationBarTitle(title: String, displayMode: NavigationBarItem.TitleDisplayMode)
Modifier.navigationBarTitle(title: String? = null, displayMode: NavigationBarItem.TitleDisplayMode? = null)
Modifier.navigationBarTitle(titleKey: LocalizedKey)
Modifier.navigationBarTitle(titleKey: LocalizedKey, displayMode: NavigationBarItem.TitleDisplayMode)
Modifier.navigationBarTitleDisplayMode(displayMode: NavigationBarItem.TitleDisplayMode)
Modifier.navigationBarTitleDisplayMode(title: String, displayMode: NavigationBarItem.TitleDisplayMode)
Modifier.navigationDestination(isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, destination: @Composable () -> Unit)
Modifier.navigationDocument(url: String)
Modifier.navigationLinkIndicatorVisibility(visibility: Visibility)
Modifier.navigationSplitViewColumnWidth(min: Double? = null, ideal: Double, max: Double? = null)
Modifier.navigationSplitViewColumnWidth(width: Double)
Modifier.navigationSplitViewStyle(style: NavigationSplitViewStyle)
Modifier.navigationSubtitle(subtitle: String)
Modifier.navigationSubtitle(subtitleKey: LocalizedKey)
Modifier.navigationTitle(title: @Composable () -> Unit)
Modifier.navigationTitle(title: String)
Modifier.navigationTitle(title: String, onTitleChange: (String) -> Unit)
Modifier.navigationTitle(titleKey: LocalizedKey)
Modifier.navigationTransition(style: NavigationTransition)
Modifier.navigationViewStyle(style: NavigationViewStyle)
Modifier.offset(offset: CGSize)
Modifier.offset(x: Double = 0.0, y: Double = 0.0)
Modifier.onAppear(perform: (() -> Unit)? = null)
Modifier.onChange(value: Any, initial: Boolean = false, action: () -> Unit)
Modifier.onChange(value: Any, initial: Boolean = false, action: (Any, Any) -> Unit)
Modifier.onChange(value: Any, perform: (Any) -> Unit)
Modifier.onContinuousHover(coordinateSpace: CoordinateSpace = CoordinateSpace.local, perform: (HoverPhase) -> Unit)
Modifier.onContinuousHover(coordinateSpace: CoordinateSpaceProtocol, perform: (HoverPhase) -> Unit)
Modifier.onDeleteCommand(perform: (() -> Unit)?)
Modifier.onDisappear(perform: (() -> Unit)? = null)
Modifier.onExitCommand(perform: (() -> Unit)?)
Modifier.onHover(perform: (Boolean) -> Unit)
Modifier.onInteractiveResizeChange(action: (Boolean) -> Unit)
Modifier.onLongPressGesture(minimumDuration: Double = 0.5, maximumDistance: Double = 10.0, inputKinds: GestureInputKinds = GestureInputKinds.all, onPressingChanged: ((Boolean) -> Unit)? = null, perform: () -> Unit)
Modifier.onLongPressGesture(minimumDuration: Double = 0.5, maximumDistance: Double = 10.0, onPressingChanged: ((Boolean) -> Unit)? = null, perform: () -> Unit)
Modifier.onLongPressGesture(minimumDuration: Double = 0.5, onPressingChanged: ((Boolean) -> Unit)? = null, perform: () -> Unit)
Modifier.onLongPressGesture(minimumDuration: Double? = null, maximumDistance: Double? = null, pressing: (((Boolean) -> Unit)?)? = null, perform: (() -> Unit)? = null)
Modifier.onLongPressGestureMinimumDuration(minimumDuration: Double = 0.5, maximumDistance: Double = 10.0, pressing: ((Boolean) -> Unit)? = null, perform: () -> Unit)
Modifier.onLongPressGestureMinimumDuration(minimumDuration: Double = 0.5, pressing: ((Boolean) -> Unit)? = null, perform: () -> Unit)
Modifier.onLongTouchGesture(minimumDuration: Double = 0.5, onTouchingChanged: ((Boolean) -> Unit)? = null, perform: () -> Unit)
Modifier.onMoveCommand(perform: ((MoveCommandDirection) -> Unit)?)
Modifier.onOpenURL(perform: (String) -> Unit)
Modifier.onOpenURL(prefersInApp: Boolean)
Modifier.onPencilSqueeze(perform: (PencilSqueezeGesturePhase) -> Unit)
Modifier.onPlayPauseCommand(perform: (() -> Unit)?)
Modifier.onScrollPhaseChange(action: (ScrollPhase, ScrollPhase) -> Unit)
Modifier.onScrollVisibilityChange(threshold: Double = 0.5, action: (Boolean) -> Unit)
Modifier.onSubmit(triggers: SubmitTriggers = SubmitTriggers.text, action: () -> Unit)
Modifier.onTapGesture(count: Int = 1, coordinateSpace: CoordinateSpace, perform: (CGPoint) -> Unit)
Modifier.onTapGesture(count: Int = 1, coordinateSpace: CoordinateSpaceProtocol, inputKinds: GestureInputKinds = GestureInputKinds.all, perform: (CGPoint) -> Unit)
Modifier.onTapGesture(count: Int = 1, coordinateSpace: CoordinateSpaceProtocol, perform: (CGPoint) -> Unit)
Modifier.onTapGesture(count: Int = 1, perform: () -> Unit)
Modifier.opacity(opacity: Double)
Modifier.overlay(alignment: Alignment = Alignment.center, content: @Composable () -> Unit)
Modifier.overlay(overlay: @Composable () -> Unit, alignment: Alignment = Alignment.center)
Modifier.overlay(style: ShapeStyle, ignoresSafeAreaEdges: Edge.Set = Edge.Set.all)
Modifier.overlay(style: ShapeStyle, shape: Shape, fillStyle: FillStyle = FillStyle())
Modifier.overlayArrangementEdge(edge: HorizontalEdge?)
Modifier.overlayArrangementEdge(edge: VerticalEdge?)
Modifier.padding(edges: Edge.Set = Edge.Set.all, length: Double? = null)
Modifier.padding(insets: EdgeInsets)
Modifier.padding(length: Double)
Modifier.pageCommand(value: Int, onValueChange: (Int) -> Unit, bounds: IntRange, step: Int = 1)
Modifier.paletteSelectionEffect(effect: PaletteSelectionEffect)
Modifier.persistentSystemOverlays(visibility: Visibility)
Modifier.pickerStyle(style: PickerStyle)
Modifier.pointerStyle(style: PointerStyle?)
Modifier.pointerVisibility(visibility: Visibility)
Modifier.popover(isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, attachmentAnchor: PopoverAttachmentAnchor? = null, arrowEdge: Edge? = null, content: @Composable () -> Unit)
Modifier.position(position: CGPoint)
Modifier.position(x: Double = 0.0, y: Double = 0.0)
Modifier.preferredColorScheme(colorScheme: ColorScheme?)
Modifier.presentationBackground(alignment: Alignment = Alignment.center, content: @Composable () -> Unit)
Modifier.presentationBackground(style: ShapeStyle)
Modifier.presentationBackgroundInteraction(interaction: PresentationBackgroundInteraction)
Modifier.presentationCompactAdaptation(adaptation: PresentationAdaptation)
Modifier.presentationCompactAdaptation(horizontal: PresentationAdaptation, vertical: PresentationAdaptation)
Modifier.presentationContentInteraction(behavior: PresentationContentInteraction)
Modifier.presentationCornerRadius(cornerRadius: Double?)
Modifier.presentationDetents(detents: Set<PresentationDetent>)
Modifier.presentationDragIndicator(visibility: Visibility)
Modifier.presentationPlacement(placement: PresentationPlacement)
Modifier.presentationPreventsAppTermination(prevents: Boolean?)
Modifier.presentationSizing(sizing: PresentationSizing)
Modifier.presentedWindowStyle(style: WindowStyle)
Modifier.presentedWindowToolbarStyle(style: WindowToolbarStyle)
Modifier.previewDevice(value: PreviewDevice?)
Modifier.previewDisplayName(value: String?)
Modifier.previewInterfaceOrientation(value: InterfaceOrientation)
Modifier.privacySensitive(sensitive: Boolean = true)
Modifier.progressViewStyle(style: ProgressViewStyle)
Modifier.projectionEffect(transform: ProjectionTransform)
Modifier.redacted(reason: RedactionReasons)
Modifier.refreshable(action: suspend () -> Unit)
Modifier.renameAction(action: () -> Unit)
Modifier.renderingMode(renderingMode: Image.TemplateRenderingMode?)
Modifier.replaceDisabled(isDisabled: Boolean = true)
Modifier.resizable(capInsets: EdgeInsets = EdgeInsets(), resizingMode: Image.ResizingMode = Image.ResizingMode.stretch)
Modifier.rotationEffect(angle: Angle, anchor: UnitPoint = UnitPoint.center)
Modifier.safeAreaBar(edge: HorizontalEdge, alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, content: @Composable () -> Unit)
Modifier.safeAreaBar(edge: VerticalEdge, alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null, content: @Composable () -> Unit)
Modifier.safeAreaInset(edge: HorizontalEdge, alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, content: @Composable () -> Unit)
Modifier.safeAreaInset(edge: VerticalEdge, alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null, content: @Composable () -> Unit)
Modifier.safeAreaPadding(edges: Edge.Set = Edge.Set.all, length: Double? = null)
Modifier.safeAreaPadding(insets: EdgeInsets)
Modifier.safeAreaPadding(length: Double)
Modifier.saturation(amount: Double)
Modifier.scaledToFill()
Modifier.scaledToFit()
Modifier.scaleEffect(s: Double, anchor: UnitPoint = UnitPoint.center)
Modifier.scaleEffect(scale: CGSize, anchor: UnitPoint = UnitPoint.center)
Modifier.scaleEffect(x: Double = 1.0, y: Double = 1.0, anchor: UnitPoint = UnitPoint.center)
Modifier.scenePadding(edges: Edge.Set = Edge.Set.all)
Modifier.scenePadding(padding: ScenePadding, edges: Edge.Set = Edge.Set.all)
Modifier.scrollBounceBehavior(behavior: ScrollBounceBehavior, axes: Axis.Set? = null)
Modifier.scrollClipDisabled(disabled: Boolean = true)
Modifier.scrollContentBackground(visibility: Visibility)
Modifier.scrollDisabled(disabled: Boolean)
Modifier.scrollDismissesKeyboard(mode: ScrollDismissesKeyboardMode)
Modifier.scrollEdgeEffectHidden(hidden: Boolean = true, edges: Edge.Set = Edge.Set.all)
Modifier.scrollEdgeEffectStyle(style: ScrollEdgeEffectStyle?, edges: Edge.Set)
Modifier.scrollIndicators(visibility: ScrollIndicatorVisibility, axes: Axis.Set? = null)
Modifier.scrollIndicatorsFlash(onAppear: Boolean)
Modifier.scrollIndicatorsFlash(trigger: Any)
Modifier.scrollInputBehavior(behavior: ScrollInputBehavior, input: ScrollInputKind)
Modifier.scrollPosition(id: Any?, onIdChange: (Any?) -> Unit, anchor: UnitPoint? = null)
Modifier.scrollTargetBehavior(behavior: ScrollTargetBehavior)
Modifier.scrollTargetLayout(isEnabled: Boolean = true)
Modifier.searchable(text: String, onTextChange: (String) -> Unit, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: LocalizedKey)
Modifier.searchable(text: String, onTextChange: (String) -> Unit, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: String)
Modifier.searchable(text: String, onTextChange: (String) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: LocalizedKey)
Modifier.searchable(text: String, onTextChange: (String) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: LocalizedKey, suggestions: @Composable () -> Unit)
Modifier.searchable(text: String, onTextChange: (String) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: String)
Modifier.searchable(text: String, onTextChange: (String) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: String, suggestions: @Composable () -> Unit)
Modifier.searchable(text: String? = null, isPresented: Boolean? = null, placement: SearchFieldPlacement? = null, prompt: String? = null, onTextChange: ((String) -> Unit)? = null, onIsPresentedChange: ((Boolean) -> Unit)? = null, suggestions: (@Composable () -> Unit)? = null)
Modifier.searchableText(text: String, onTextChange: (String) -> Unit, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: LocalizedKey)
Modifier.searchableText(text: String, onTextChange: (String) -> Unit, isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: String? = null)
Modifier.searchableText(text: String, onTextChange: (String) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: LocalizedKey)
Modifier.searchableText(text: String, onTextChange: (String) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: String? = null)
Modifier.searchableText(text: String, onTextChange: (String) -> Unit, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: String? = null, suggestions: @Composable () -> Unit)
Modifier.searchCompletion(completion: String)
Modifier.searchDictationBehavior(dictationBehavior: TextInputDictationBehavior)
Modifier.searchPresentationToolbarBehavior(behavior: SearchPresentationToolbarBehavior)
Modifier.searchScopes(scope: Any, onScopeChange: (Any) -> Unit, activation: SearchScopeActivation, scopes: @Composable () -> Unit)
Modifier.searchScopes(scope: Any, onScopeChange: (Any) -> Unit, scopes: @Composable () -> Unit)
Modifier.searchSuggestions(suggestions: @Composable () -> Unit)
Modifier.searchSuggestions(visibility: Visibility, placements: SearchSuggestionsPlacement.Set)
Modifier.searchToolbarBehavior(behavior: SearchToolbarBehavior)
Modifier.sectionActions(content: @Composable () -> Unit)
Modifier.sectionIndexLabel(label: String?)
Modifier.selectionDisabled(isDisabled: Boolean = true)
Modifier.sensoryFeedback(feedback: SensoryFeedback, trigger: Any)
Modifier.shadow(color: Color? = null, radius: Double, x: Double = 0.0, y: Double = 0.0)
Modifier.sheet(isPresented: Boolean, onIsPresentedChange: (Boolean) -> Unit, onDismiss: (() -> Unit)? = null, content: @Composable () -> Unit)
Modifier.sliderThumbVisibility(visibility: Visibility)
Modifier.speechAdjustedPitch(value: Double)
Modifier.speechAlwaysIncludesPunctuation(value: Boolean = true)
Modifier.speechAnnouncementsQueued(value: Boolean = true)
Modifier.speechSpellsOutCharacters(value: Boolean = true)
Modifier.splitArrangementFixedLayoutSize(horizontal: Boolean = true, vertical: Boolean = true)
Modifier.splitArrangementLayoutRatio(minHorizontal: Double? = null, idealHorizontal: Double? = null, maxHorizontal: Double? = null, minVertical: Double? = null, idealVertical: Double? = null, maxVertical: Double? = null)
Modifier.splitArrangementLayoutRatio(ratio: Double?)
Modifier.splitArrangementLayoutSize(minWidth: Double? = null, idealWidth: Double? = null, maxWidth: Double? = null, minHeight: Double? = null, idealHeight: Double? = null, maxHeight: Double? = null)
Modifier.springLoadingBehavior(behavior: SpringLoadingBehavior)
Modifier.statusBar(hidden: Boolean)
Modifier.statusBarHidden(hidden: Boolean = true)
Modifier.strikethrough(isActive: Boolean = true, color: Color? = null)
Modifier.strikethrough(isActive: Boolean = true, pattern: Text.LineStyle.Pattern = Text.LineStyle.Pattern.solid, color: Color? = null)
Modifier.submitLabel(submitLabel: SubmitLabel)
Modifier.submitScope(isBlocking: Boolean = true)
Modifier.swipeActions(edge: HorizontalEdge = HorizontalEdge.trailing, allowsFullSwipe: Boolean = true, content: @Composable () -> Unit)
Modifier.swipeActions(edge: HorizontalEdge = HorizontalEdge.trailing, allowsFullSwipe: Boolean = true, content: @Composable () -> Unit, onPresentationChanged: (Boolean) -> Unit)
Modifier.swipeActionsContainer()
Modifier.symbolColorRenderingMode(mode: SymbolColorRenderingMode?)
Modifier.symbolEffectsRemoved(isEnabled: Boolean = true)
Modifier.symbolRenderingMode(mode: SymbolRenderingMode?)
Modifier.symbolVariableValueMode(mode: SymbolVariableValueMode?)
Modifier.symbolVariant(variant: SymbolVariants)
Modifier.tabBarMinimizeBehavior(behavior: TabBarMinimizeBehavior)
Modifier.tabItem(label: @Composable () -> Unit)
Modifier.tableColumnHeaders(visibility: Visibility)
Modifier.tableStyle(style: TableStyle)
Modifier.tabViewBottomAccessory(content: @Composable () -> Unit)
Modifier.tabViewBottomAccessory(isEnabled: Boolean, content: @Composable () -> Unit)
Modifier.tabViewSearchActivation(activation: TabSearchActivation)
Modifier.tabViewSidebarBottomBar(content: @Composable () -> Unit)
Modifier.tabViewSidebarFooter(content: @Composable () -> Unit)
Modifier.tabViewSidebarHeader(content: @Composable () -> Unit)
Modifier.tabViewStyle(style: TabViewStyle)
Modifier.tag(tag: Any, includeOptional: Boolean = true)
Modifier.task(id: Any, name: String? = null, file: String? = null, line: Int? = null, action: suspend () -> Unit)
Modifier.task(name: String? = null, file: String? = null, line: Int? = null, action: suspend () -> Unit)
Modifier.textCase(textCase: Text.Case?)
Modifier.textEditorStyle(style: TextEditorStyle)
Modifier.textFieldStyle(style: TextFieldStyle)
Modifier.textInputAutocapitalization(autocapitalization: TextInputAutocapitalization?)
Modifier.textInputBorderShape(shape: TextInputBorderShape)
Modifier.textInputCompletion(completion: String)
Modifier.textInputFormattingControlVisibility(visibility: Visibility, placement: TextInputFormattingControlPlacement.Set)
Modifier.textInputSuggestions(suggestions: @Composable () -> Unit)
Modifier.textScale(scale: Text.Scale, isEnabled: Boolean = true)
Modifier.textSelection(selectability: TextSelectability)
Modifier.textSelectionAffinity(affinity: TextSelectionAffinity)
Modifier.tint(tint: Color?)
Modifier.toggleStyle(style: ToggleStyle)
Modifier.toolbar(content: @Composable () -> Unit)
Modifier.toolbar(removing: ToolbarDefaultItemKind?)
Modifier.toolbar(visibility: Visibility, bars: ToolbarPlacement)
Modifier.toolbarBackground(style: ShapeStyle, bars: ToolbarPlacement)
Modifier.toolbarBackground(visibility: Visibility, bars: ToolbarPlacement)
Modifier.toolbarBackgroundVisibility(visibility: Visibility, bars: ToolbarPlacement)
Modifier.toolbarColorScheme(colorScheme: ColorScheme?, bars: ToolbarPlacement)
Modifier.toolbarForegroundStyle(style: ShapeStyle, bars: ToolbarPlacement)
Modifier.toolbarItem(placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic, sharedBackgroundVisibility: Visibility = Visibility.automatic, content: @Composable () -> Unit)
Modifier.toolbarItemHidden(hidden: Boolean = true)
Modifier.toolbarMinimizationBehavior(behavior: ToolbarMinimizationBehavior, bars: ToolbarPlacement)
Modifier.toolbarMinimizationRestoration(restoration: ToolbarMinimizationRestoration, bars: ToolbarPlacement)
Modifier.toolbarMinimizationSafeAreaAdjustment(adjustment: ToolbarMinimizationSafeAreaAdjustment, bars: ToolbarPlacement)
Modifier.toolbarOverflowMenu(content: @Composable () -> Unit)
Modifier.toolbarRole(role: ToolbarRole)
Modifier.toolbarTitleAlignment(alignment: ToolbarTitleAlignment)
Modifier.toolbarTitleDisplayMode(mode: ToolbarTitleDisplayMode)
Modifier.toolbarTitleMenu(content: @Composable () -> Unit)
Modifier.toolbarVerticalBehavior(behavior: ToolbarVerticalBehavior)
Modifier.toolbarVerticalCompressionBehavior(behavior: ToolbarVerticalCompressionBehavior)
Modifier.toolbarVisibility(visibility: Visibility, bars: ToolbarPlacement)
Modifier.touchBar(content: @Composable () -> Unit)
Modifier.touchBarCustomizationLabel(label: String)
Modifier.touchBarItemPresence(presence: TouchBarItemPresence)
Modifier.touchBarItemPrincipal(principal: Boolean = true)
Modifier.tracking(tracking: Double)
Modifier.transformEffect(transform: CGAffineTransform)
Modifier.transition(t: AnyTransition)
Modifier.truncationMode(mode: Text.TruncationMode)
Modifier.typeSelectEquivalent(string: String)
Modifier.typeSelectEquivalent(stringKey: LocalizedKey)
Modifier.typesettingLanguage(language: String, isEnabled: Boolean = true)
Modifier.typesettingLanguage(language: TypesettingLanguage, isEnabled: Boolean = true)
Modifier.underline(isActive: Boolean = true, color: Color? = null)
Modifier.underline(isActive: Boolean = true, pattern: Text.LineStyle.Pattern = Text.LineStyle.Pattern.solid, color: Color? = null)
Modifier.unredacted()
Modifier.windowDismissBehavior(behavior: WindowInteractionBehavior)
Modifier.windowFullScreenBehavior(behavior: WindowInteractionBehavior)
Modifier.windowMinimizeBehavior(behavior: WindowInteractionBehavior)
Modifier.windowResizeAnchor(anchor: UnitPoint?)
Modifier.windowResizeBehavior(behavior: WindowInteractionBehavior)
Modifier.windowToolbarFullScreenVisibility(visibility: WindowToolbarFullScreenVisibility)
Modifier.writingDirection(strategy: Text.WritingDirectionStrategy)
Modifier.writingToolsAffordanceVisibility(visibility: Visibility)
Modifier.writingToolsBehavior(behavior: WritingToolsBehavior)
Modifier.zIndex(value: Double)
```

## Value types (static members, factories, constructors)

- `AccessibilityActionCategory` (class)
  - `AccessibilityActionCategory(nameResource: LocalizedKey)`
  - `AccessibilityActionCategory(name: String)`
  - `AccessibilityActionCategory.edit`
  - `AccessibilityActionCategory.default`
  - `AccessibilityActionCategory.nameKey(nameKey: LocalizedKey): AccessibilityActionCategory`
  - `AccessibilityActionCategory.name(name: String): AccessibilityActionCategory`
- `AccessibilityHeadingLevel` (class)
  - `AccessibilityHeadingLevel.unspecified`
  - `AccessibilityHeadingLevel.h1`
  - `AccessibilityHeadingLevel.h2`
  - `AccessibilityHeadingLevel.h3`
  - `AccessibilityHeadingLevel.h4`
  - `AccessibilityHeadingLevel.h5`
  - `AccessibilityHeadingLevel.h6`
- `AccessibilityTechnologies` (class)
  - `AccessibilityTechnologies.intersection(other: AccessibilityTechnologies): AccessibilityTechnologies`
  - `AccessibilityTechnologies.symmetricDifference(other: AccessibilityTechnologies): AccessibilityTechnologies`
  - `AccessibilityTechnologies.union(other: AccessibilityTechnologies): AccessibilityTechnologies`
  - `AccessibilityTechnologies()`
  - `AccessibilityTechnologies.switchControl`
  - `AccessibilityTechnologies.voiceOver`
- `AccessoryCircularCapacityGaugeStyle` (class)
  - `AccessoryCircularCapacityGaugeStyle()`
- `Alert` (class)
  - `Alert(title: String, message: String? = null, dismissButton: Alert.Button? = null)`
  - `Alert(title: String, message: String? = null, primaryButton: Alert.Button, secondaryButton: Alert.Button)`
  - `Alert.sideBySideButtons(title: String, message: String?`
- `Alert.Button` (class)
  - `Alert.Button.destructive(label: String, action: (() -> Unit)?): Alert.Button`
  - `Alert.Button.cancel(label: String, action: (() -> Unit)?): Alert.Button`
  - `Alert.Button.cancel(action: (() -> Unit)?): Alert.Button`
  - `Alert.Button.default(label: String, action: (() -> Unit)?): Alert.Button`
- `AnimationCompletionCriteria` (class)
  - `AnimationCompletionCriteria.logicallyComplete`
  - `AnimationCompletionCriteria.removed`
- `AnyShapeStyle` (class)
  - `AnyShapeStyle(style: ShapeStyle)`
- `AutomaticArrangementViewStyle` (class)
  - `AutomaticArrangementViewStyle()`
- `AutomaticLabeledContentStyle` (class)
  - `AutomaticLabeledContentStyle()`
- `AutomaticTextEditorStyle` (class)
  - `AutomaticTextEditorStyle()`
- `BlendMode` (class)
  - `BlendMode.colorDodge`
  - `BlendMode.difference`
  - `BlendMode.luminosity`
  - `BlendMode.plusDarker`
  - `BlendMode.saturation`
  - `BlendMode.sourceAtop`
  - `BlendMode.plusLighter`
  - `BlendMode.destinationOut`
  - `BlendMode.destinationOver`
  - `BlendMode.hue`
  - `BlendMode.color`
  - `BlendMode.darken`
  - `BlendMode.normal`
  - `BlendMode.screen`
  - `BlendMode.lighten`
  - `BlendMode.overlay`
  - `BlendMode.multiply`
  - `BlendMode.colorBurn`
  - `BlendMode.exclusion`
  - `BlendMode.hardLight`
  - `BlendMode.softLight`
- `BorderedTableStyle` (class)
  - `BorderedTableStyle(alternatesRowBackgrounds: Boolean)`
  - `BorderedTableStyle()`
- `ButtonBorderShape` (class)
  - `ButtonBorderShape.roundedRectangle(radius: Double): ButtonBorderShape`
  - `ButtonBorderShape.roundedRectangle`
  - `ButtonBorderShape.circle`
  - `ButtonBorderShape.capsule`
  - `ButtonBorderShape.automatic`
- `Capsule` (class)
  - `Capsule(style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
- `Circle` (class)
  - `Circle()`
- `ColorScheme` (class)
  - `ColorScheme.dark`
  - `ColorScheme.light`
- `CompactMenuControlGroupStyle` (class)
  - `CompactMenuControlGroupStyle()`
- `ContentMode` (class)
  - `ContentMode.fit`
  - `ContentMode.fill`
- `ControlGroupStyle` (interface)
  - `ControlGroupStyle.navigation`
  - `ControlGroupStyle.compactMenu`
  - `ControlGroupStyle.menu`
  - `ControlGroupStyle.palette`
  - `ControlGroupStyle.automatic`
- `CrossFadeNavigationTransition` (class)
- `DefaultFocusEvaluationPriority` (class)
  - `DefaultFocusEvaluationPriority.userInitiated`
  - `DefaultFocusEvaluationPriority.automatic`
- `DefaultMenuButtonStyle` (class)
  - `DefaultMenuButtonStyle()`
- `DefaultTextFieldStyle` (class)
  - `DefaultTextFieldStyle()`
- `DialogSeverity` (class)
  - `DialogSeverity.critical`
  - `DialogSeverity.standard`
  - `DialogSeverity.automatic`
- `DoubleColumnNavigationViewStyle` (class)
  - `DoubleColumnNavigationViewStyle()`
- `DropOperation` (class)
  - `DropOperation.copy`
  - `DropOperation.move`
  - `DropOperation.alias`
  - `DropOperation.cancel`
  - `DropOperation.delete`
  - `DropOperation.forbidden`
- `DropOperation.Set` (class)
  - `DropOperation.Set(rawValue: Int)`
  - `DropOperation.Set.copy`
  - `DropOperation.Set.move`
  - `DropOperation.Set.alias`
  - `DropOperation.Set.cancel`
  - `DropOperation.Set.delete`
  - `DropOperation.Set.forbidden`
- `EditMode` (class)
  - `EditMode.active`
  - `EditMode.inactive`
  - `EditMode.transient`
- `EmptyControlWidgetConfiguration` (class)
  - `EmptyControlWidgetConfiguration()`
- `EnvironmentValues` (class)
  - `EnvironmentValues()`
- `FillShapeStyle` (class)
  - `FillShapeStyle()`
- `ForegroundStyle` (class)
  - `ForegroundStyle()`
- `GaugeStyle` (interface)
  - `GaugeStyle.linearCapacity`
  - `GaugeStyle.accessoryLinear`
  - `GaugeStyle.accessoryCircular`
  - `GaugeStyle.accessoryLinearCapacity`
  - `GaugeStyle.accessoryCircularCapacity`
  - `GaugeStyle.linear`
  - `GaugeStyle.automatic`
  - `GaugeStyle.circular`
- `GlassProminentButtonStyle` (class)
  - `GlassProminentButtonStyle()`
- `GridLayout` (class)
  - `GridLayout(alignment: Alignment = Alignment.center, horizontalSpacing: Double? = null, verticalSpacing: Double? = null)`
- `HandGestureShortcut` (class)
  - `HandGestureShortcut.primaryAction`
- `HorizontalEdge` (class)
  - `HorizontalEdge.leading`
  - `HorizontalEdge.trailing`
- `HorizontalEdge.Set` (class)
  - `HorizontalEdge.Set(edge: HorizontalEdge)`
  - `HorizontalEdge.Set(rawValue: Int)`
  - `HorizontalEdge.Set.all`
  - `HorizontalEdge.Set.leading`
  - `HorizontalEdge.Set.trailing`
- `ImmersionStyle` (interface)
  - `ImmersionStyle.progressive(aspectRatio: ProgressiveImmersionAspectRatio): ProgressiveImmersionStyle`
  - `ImmersionStyle.progressive`
  - `ImmersionStyle.progressive(immersionRange: ClosedFloatingPointRange<Double>, initialAmount: Double?`
  - `ImmersionStyle.progressive(immersionRange: ClosedFloatingPointRange<Double>, initialAmount: Double?`
  - `ImmersionStyle.full`
  - `ImmersionStyle.automatic`
- `InsetTableStyle` (class)
  - `InsetTableStyle(alternatesRowBackgrounds: Boolean)`
  - `InsetTableStyle()`
- `KeyboardShortcut` (class)
  - `KeyboardShortcut(key: KeyEquivalent, modifiers: EventModifiers = EventModifiers.command, localization: KeyboardShortcut.Localization)`
  - `KeyboardShortcut(key: KeyEquivalent, modifiers: EventModifiers = EventModifiers.command)`
  - `KeyboardShortcut.cancelAction`
  - `KeyboardShortcut.defaultAction`
- `KeyboardShortcut.Localization` (class)
  - `KeyboardShortcut.Localization.withoutMirroring`
  - `KeyboardShortcut.Localization.custom`
  - `KeyboardShortcut.Localization.automatic`
- `LegibilityWeight` (class)
  - `LegibilityWeight.bold`
  - `LegibilityWeight.regular`
- `LinkButtonStyle` (class)
  - `LinkButtonStyle()`
- `LocalizedStringKey` (class)
  - `LocalizedStringKey(stringLiteral: String)`
  - `LocalizedStringKey(stringInterpolation: LocalizedStringKey.StringInterpolation)`
  - `LocalizedStringKey.value(value: String): LocalizedStringKey`
- `LocalizedStringKey.StringInterpolation` (class)
  - `LocalizedStringKey.StringInterpolation(literalCapacity: Int, interpolationCount: Int)`
- `MaterialActiveAppearance` (class)
  - `MaterialActiveAppearance.matchWindow`
  - `MaterialActiveAppearance.active`
  - `MaterialActiveAppearance.inactive`
  - `MaterialActiveAppearance.automatic`
- `MenuPickerStyle` (class)
  - `MenuPickerStyle()`
- `NSHostingSizingOptions` (class)
  - `NSHostingSizingOptions(rawValue: Int)`
  - `NSHostingSizingOptions.standardBounds`
  - `NSHostingSizingOptions.intrinsicContentSize`
  - `NSHostingSizingOptions.preferredContentSize`
  - `NSHostingSizingOptions.maxSize`
  - `NSHostingSizingOptions.minSize`
- `NavigationPath` (class)
  - `NavigationPath()`
- `OffsetTransition` (class)
  - `OffsetTransition(offset: CGSize)`
- `PageIndexViewStyle` (class)
  - `PageIndexViewStyle(backgroundDisplayMode: PageIndexViewStyle.BackgroundDisplayMode = PageIndexViewStyle.BackgroundDisplayMode.automatic)`
- `PageIndexViewStyle.BackgroundDisplayMode` (class)
  - `PageIndexViewStyle.BackgroundDisplayMode.interactive`
  - `PageIndexViewStyle.BackgroundDisplayMode.never`
  - `PageIndexViewStyle.BackgroundDisplayMode.always`
  - `PageIndexViewStyle.BackgroundDisplayMode.automatic`
- `PaletteSelectionEffect` (class)
  - `PaletteSelectionEffect.symbolVariant(variant: SymbolVariants): PaletteSelectionEffect`
  - `PaletteSelectionEffect.custom`
  - `PaletteSelectionEffect.automatic`
- `PinnedScrollableViews` (class)
  - `PinnedScrollableViews(rawValue: Int)`
  - `PinnedScrollableViews.sectionFooters`
  - `PinnedScrollableViews.sectionHeaders`
- `PlainWindowStyle` (class)
  - `PlainWindowStyle()`
- `PresentationContentInteraction` (class)
  - `PresentationContentInteraction.resizes`
  - `PresentationContentInteraction.scrolls`
  - `PresentationContentInteraction.automatic`
- `PrimitiveButtonStyle` (interface)
  - `PrimitiveButtonStyle.borderless`
  - `PrimitiveButtonStyle.accessoryBar`
  - `PrimitiveButtonStyle.glassProminent`
  - `PrimitiveButtonStyle.borderedProminent`
  - `PrimitiveButtonStyle.accessoryBarAction`
  - `PrimitiveButtonStyle.card`
  - `PrimitiveButtonStyle.link`
  - `PrimitiveButtonStyle.glass`
  - `PrimitiveButtonStyle.glass(glass: Glass): GlassButtonStyle`
  - `PrimitiveButtonStyle.plain`
  - `PrimitiveButtonStyle.automatic`
  - `PrimitiveButtonStyle.bordered`
- `ProminentDetailNavigationSplitViewStyle` (class)
  - `ProminentDetailNavigationSplitViewStyle()`
- `RadioGroupPickerStyle` (class)
  - `RadioGroupPickerStyle()`
- `RotateGesture` (class)
  - `RotateGesture(minimumAngleDelta: Angle? = null, inputKinds: GestureInputKinds = GestureInputKinds.all)`
  - `RotateGesture(minimumAngleDelta: Angle? = null)`
- `RoundedRectangularShapeCorners` (class)
  - `RoundedRectangularShapeCorners(topLeading: Edge.Corner.Style? = null, topTrailing: Edge.Corner.Style? = null, bottomLeading: Edge.Corner.Style? = null, bottomTrailing: Edge.Corner.Style? = null)`
  - `RoundedRectangularShapeCorners(all: Edge.Corner.Style)`
  - `RoundedRectangularShapeCorners.concentric(minimum: Edge.Corner.Style?`
  - `RoundedRectangularShapeCorners.concentric`
  - `RoundedRectangularShapeCorners.fixed(radius: Double): RoundedRectangularShapeCorners`
- `RoundedRectangularShapeCorners.AnimatableData` (class)
  - `RoundedRectangularShapeCorners.AnimatableData.zero`
- `SceneRestorationBehavior` (class)
  - `SceneRestorationBehavior.disabled`
  - `SceneRestorationBehavior.automatic`
- `ScrollGeometry` (class)
  - `ScrollGeometry(contentOffset: CGPoint, contentSize: CGSize, contentInsets: EdgeInsets, containerSize: CGSize)`
- `ScrollTargetBehavior` (interface)
  - `ScrollTargetBehavior.viewAligned(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior, anchor: UnitPoint?): ViewAlignedScrollTargetBehavior`
  - `ScrollTargetBehavior.viewAligned(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior): ViewAlignedScrollTargetBehavior`
  - `ScrollTargetBehavior.viewAligned(anchor: UnitPoint?): ViewAlignedScrollTargetBehavior`
  - `ScrollTargetBehavior.viewAligned`
  - `ScrollTargetBehavior.paging`
- `SearchScopeActivation` (class)
  - `SearchScopeActivation.onSearchPresentation`
  - `SearchScopeActivation.onTextEntry`
  - `SearchScopeActivation.automatic`
- `SeparatorShapeStyle` (class)
  - `SeparatorShapeStyle()`
- `ShapeRole` (class)
  - `ShapeRole.fill`
  - `ShapeRole.stroke`
  - `ShapeRole.separator`
- `SizeDependentTextVariant` (class)
- `SpatialEventGesture` (class)
  - `SpatialEventGesture(coordinateSpace: CoordinateSpaceProtocol = CoordinateSpaceProtocol.local)`
- `StackNavigationViewStyle` (class)
  - `StackNavigationViewStyle()`
- `SurroundingsEffect` (class)
  - `SurroundingsEffect.colorMultiply(color: Color): SurroundingsEffect`
  - `SurroundingsEffect.dim(intensity: Double): SurroundingsEffect`
  - `SurroundingsEffect.dark`
  - `SurroundingsEffect.semiDark`
  - `SurroundingsEffect.ultraDark`
- `SymbolVariants` (class)
  - `SymbolVariants.fill`
  - `SymbolVariants.none`
  - `SymbolVariants.slash`
  - `SymbolVariants.circle`
  - `SymbolVariants.square`
  - `SymbolVariants.rectangle`
- `TabPlacement` (class)
  - `TabPlacement.sidebarOnly`
  - `TabPlacement.pinned`
  - `TabPlacement.automatic`
- `TabViewStyle` (interface)
  - `TabViewStyle.verticalPage(transitionStyle: VerticalPageTabViewStyle.TransitionStyle): VerticalPageTabViewStyle`
  - `TabViewStyle.verticalPage`
  - `TabViewStyle.sidebarAdaptable`
  - `TabViewStyle.page(indexDisplayMode: PageTabViewStyle.IndexDisplayMode): PageTabViewStyle`
  - `TabViewStyle.page`
  - `TabViewStyle.automatic`
  - `TabViewStyle.grouped`
  - `TabViewStyle.carousel`
  - `TabViewStyle.tabBarOnly`
- `Text` (object)
- `Text.AlignmentStrategy` (class)
  - `Text.AlignmentStrategy.layoutBased`
  - `Text.AlignmentStrategy.writingDirectionBased`
  - `Text.AlignmentStrategy.default`
- `Text.Case` (class)
  - `Text.Case.lowercase`
  - `Text.Case.uppercase`
- `Text.DateStyle` (class)
  - `Text.DateStyle.date`
  - `Text.DateStyle.time`
  - `Text.DateStyle.timer`
  - `Text.DateStyle.offset`
  - `Text.DateStyle.relative`
- `Text.Layout` (object)
- `Text.Layout.CharacterIndex` (class)
  - `Text.Layout.CharacterIndex.advanced(n: Int): Text.Layout.CharacterIndex`
- `Text.Layout.DrawingOptions` (class)
  - `Text.Layout.DrawingOptions(rawValue: Int)`
  - `Text.Layout.DrawingOptions.disablesSubpixelQuantization`
- `Text.Layout.TypographicBounds` (class)
  - `Text.Layout.TypographicBounds()`
- `Text.LineStyle` (class)
  - `Text.LineStyle(pattern: Text.LineStyle.Pattern = Text.LineStyle.Pattern.solid, color: Color? = null)`
  - `Text.LineStyle.single`
- `Text.LineStyle.Pattern` (class)
  - `Text.LineStyle.Pattern.dashDotDot`
  - `Text.LineStyle.Pattern.dot`
  - `Text.LineStyle.Pattern.dash`
  - `Text.LineStyle.Pattern.solid`
  - `Text.LineStyle.Pattern.dashDot`
- `Text.Scale` (class)
  - `Text.Scale.default`
  - `Text.Scale.secondary`
- `Text.TruncationMode` (class)
  - `Text.TruncationMode.head`
  - `Text.TruncationMode.tail`
  - `Text.TruncationMode.middle`
- `Text.WritingDirectionStrategy` (class)
  - `Text.WritingDirectionStrategy.layoutBased`
  - `Text.WritingDirectionStrategy.contentBased`
  - `Text.WritingDirectionStrategy.default`
- `TextInputAutocapitalization` (class)
  - `TextInputAutocapitalization.characters`
  - `TextInputAutocapitalization.never`
  - `TextInputAutocapitalization.words`
  - `TextInputAutocapitalization.sentences`
- `TextSelectionAffinity` (class)
  - `TextSelectionAffinity.downstream`
  - `TextSelectionAffinity.upstream`
  - `TextSelectionAffinity.automatic`
- `TitleBarWindowStyle` (class)
  - `TitleBarWindowStyle()`
- `ToolbarDefaultItemKind` (class)
  - `ToolbarDefaultItemKind.sidebarToggle`
  - `ToolbarDefaultItemKind.title`
  - `ToolbarDefaultItemKind.search`
- `ToolbarMinimizationRestoration` (class)
  - `ToolbarMinimizationRestoration.atScrollEdge`
  - `ToolbarMinimizationRestoration.automatic`
- `ToolbarTitleDisplayMode` (class)
  - `ToolbarTitleDisplayMode.inlineLarge`
  - `ToolbarTitleDisplayMode.large`
  - `ToolbarTitleDisplayMode.inline`
  - `ToolbarTitleDisplayMode.automatic`
- `TransitionProperties` (class)
  - `TransitionProperties(hasMotion: Boolean = true)`
- `UnitCurve` (class)
  - `UnitCurve.circularEaseIn`
  - `UnitCurve.circularEaseOut`
  - `UnitCurve.circularEaseInOut`
  - `UnitCurve.bezier(startControlPoint: UnitPoint, endControlPoint: UnitPoint): UnitCurve`
  - `UnitCurve.easeIn`
  - `UnitCurve.linear`
  - `UnitCurve.easeOut`
  - `UnitCurve.easeInOut`
  - `UnitCurve.easeInEaseOut`
- `VerticalEdge` (class)
  - `VerticalEdge.top`
  - `VerticalEdge.bottom`
- `VerticalEdge.Set` (class)
  - `VerticalEdge.Set(e: VerticalEdge)`
  - `VerticalEdge.Set(rawValue: Int)`
  - `VerticalEdge.Set.all`
  - `VerticalEdge.Set.top`
  - `VerticalEdge.Set.bottom`
- `WheelDatePickerStyle` (class)
  - `WheelDatePickerStyle()`
- `WindowLevel` (class)
  - `WindowLevel.normal`
  - `WindowLevel.desktop`
  - `WindowLevel.floating`
  - `WindowLevel.automatic`
- `WindowToolbarFullScreenVisibility` (class)
  - `WindowToolbarFullScreenVisibility.onHover`
  - `WindowToolbarFullScreenVisibility.visible`
  - `WindowToolbarFullScreenVisibility.automatic`
- `AccessibilityActionKind` (class)
  - `AccessibilityActionKind(named: String)`
  - `AccessibilityActionKind.delete`
  - `AccessibilityActionKind.escape`
  - `AccessibilityActionKind.default`
  - `AccessibilityActionKind.magicTap`
  - `AccessibilityActionKind.showMenu`
- `AccessibilityLabeledPairRole` (class)
  - `AccessibilityLabeledPairRole.label`
  - `AccessibilityLabeledPairRole.content`
- `AccessibilityTextContentType` (class)
  - `AccessibilityTextContentType.fileSystem`
  - `AccessibilityTextContentType.sourceCode`
  - `AccessibilityTextContentType.spreadsheet`
  - `AccessibilityTextContentType.wordProcessing`
  - `AccessibilityTextContentType.plain`
  - `AccessibilityTextContentType.console`
  - `AccessibilityTextContentType.messaging`
  - `AccessibilityTextContentType.narrative`
- `AccessoryCircularGaugeStyle` (class)
  - `AccessoryCircularGaugeStyle()`
- `Alignment` (class)
  - `Alignment(horizontal: HorizontalAlignment, vertical: VerticalAlignment)`
  - `Alignment.topLeading`
  - `Alignment.topTrailing`
  - `Alignment.bottomLeading`
  - `Alignment.bottomTrailing`
  - `Alignment.centerLastTextBaseline`
  - `Alignment.centerFirstTextBaseline`
  - `Alignment.leadingLastTextBaseline`
  - `Alignment.leadingFirstTextBaseline`
  - `Alignment.trailingLastTextBaseline`
  - `Alignment.trailingFirstTextBaseline`
  - `Alignment.top`
  - `Alignment.bottom`
  - `Alignment.center`
  - `Alignment.leading`
  - `Alignment.trailing`
- `AnimationTimelineSchedule` (class)
  - `AnimationTimelineSchedule(minimumInterval: Double? = null, paused: Boolean = false)`
- `AnyTransition` (class)
  - `AnyTransition.combined(other: AnyTransition): AnyTransition`
  - `AnyTransition.animation(animation: Animation?): AnyTransition`
  - `AnyTransition.asymmetric(insertion: AnyTransition, removal: AnyTransition): AnyTransition`
  - `AnyTransition.move(edge: Edge): AnyTransition`
  - `AnyTransition.push(edge: Edge): AnyTransition`
  - `AnyTransition.scale`
  - `AnyTransition.scale(scale: Double, anchor: UnitPoint`
  - `AnyTransition.slide`
  - `AnyTransition.offset(x: Double`
  - `AnyTransition.offset(offset: CGSize): AnyTransition`
  - `AnyTransition.opacity`
  - `AnyTransition.identity`
- `AutomaticControlGroupStyle` (class)
- `AutomaticMenuBarExtraStyle` (class)
  - `AutomaticMenuBarExtraStyle()`
- `Axis` (class)
  - `Axis.horizontal`
  - `Axis.vertical`
- `Axis.Set` (class)
  - `Axis.Set(rawValue: Int)`
  - `Axis.Set.horizontal`
  - `Axis.Set.vertical`
- `BlurReplaceTransition` (class)
  - `BlurReplaceTransition(configuration: BlurReplaceTransition.Configuration)`
- `BlurReplaceTransition.Configuration` (class)
  - `BlurReplaceTransition.Configuration.upUp`
  - `BlurReplaceTransition.Configuration.downUp`
- `BorderedTextFieldStyle` (class)
  - `BorderedTextFieldStyle()`
- `ButtonMenuStyle` (class)
  - `ButtonMenuStyle()`
- `CardButtonStyle` (class)
  - `CardButtonStyle()`
- `CircularGaugeStyle` (class)
  - `CircularGaugeStyle(tint: Color)`
  - `CircularGaugeStyle(tint: Gradient)`
  - `CircularGaugeStyle()`
- `ColorSchemeContrast` (class)
  - `ColorSchemeContrast.standard`
  - `ColorSchemeContrast.increased`
- `ConcentricRectangle` (class)
  - `ConcentricRectangle(topLeadingCorner: Edge.Corner.Style = Edge.Corner.Style.concentric, topTrailingCorner: Edge.Corner.Style = Edge.Corner.Style.concentric, bottomLeadingCorner: Edge.Corner.Style = Edge.Corner.Style.concentric, bottomTrailingCorner: Edge.Corner.Style = Edge.Corner.Style.concentric)`
  - `ConcentricRectangle(uniformTopCorners: Edge.Corner.Style = Edge.Corner.Style.concentric, uniformBottomCorners: Edge.Corner.Style = Edge.Corner.Style.concentric)`
  - `ConcentricRectangle(uniformTopCorners: Edge.Corner.Style = Edge.Corner.Style.concentric, bottomLeadingCorner: Edge.Corner.Style = Edge.Corner.Style.concentric, bottomTrailingCorner: Edge.Corner.Style = Edge.Corner.Style.concentric)`
  - `ConcentricRectangle(corners: Edge.Corner.Style, isUniform: Boolean = false)`
  - `ConcentricRectangle()`
  - `ConcentricRectangle.uniformBottomCorners(uniformBottomCorners: Edge.Corner.Style`
  - `ConcentricRectangle.uniformLeadingCorners(uniformLeadingCorners: Edge.Corner.Style`
  - `ConcentricRectangle.uniformLeadingCorners(uniformLeadingCorners: Edge.Corner.Style`
  - `ConcentricRectangle.uniformTrailingCorners(uniformTrailingCorners: Edge.Corner.Style`
- `ConcentricRectangle.AnimatableData` (class)
  - `ConcentricRectangle.AnimatableData.zero`
- `ContentShapeKinds` (class)
  - `ContentShapeKinds(rawValue: Int)`
  - `ContentShapeKinds.dragPreview`
  - `ContentShapeKinds.focusEffect`
  - `ContentShapeKinds.hoverEffect`
  - `ContentShapeKinds.interaction`
  - `ContentShapeKinds.accessibility`
  - `ContentShapeKinds.contextMenuPreview`
- `ControlSize` (class)
  - `ControlSize.extraLarge`
  - `ControlSize.mini`
  - `ControlSize.large`
  - `ControlSize.small`
  - `ControlSize.regular`
- `CustomHoverEffect` (interface)
  - `CustomHoverEffect.lift`
  - `CustomHoverEffect.automatic`
  - `CustomHoverEffect.highlight`
- `DefaultGaugeStyle` (class)
  - `DefaultGaugeStyle()`
- `DefaultMenuStyle` (class)
  - `DefaultMenuStyle()`
- `DefaultToggleStyle` (class)
  - `DefaultToggleStyle()`
- `DigitalCrownRotationalSensitivity` (class)
  - `DigitalCrownRotationalSensitivity.low`
  - `DigitalCrownRotationalSensitivity.high`
  - `DigitalCrownRotationalSensitivity.medium`
- `DragConfiguration` (class)
  - `DragConfiguration(operationsWithinApp: DragConfiguration.OperationsWithinApp = DragConfiguration.OperationsWithinApp(), operationsOutsideApp: DragConfiguration.OperationsOutsideApp = DragConfiguration.OperationsOutsideApp())`
  - `DragConfiguration(allowMove: Boolean, allowDelete: Boolean)`
  - `DragConfiguration(allowMove: Boolean)`
- `DragConfiguration.OperationsOutsideApp` (class)
  - `DragConfiguration.OperationsOutsideApp(allowCopy: Boolean = true, allowMove: Boolean = false, allowDelete: Boolean = false)`
  - `DragConfiguration.OperationsOutsideApp(allowCopy: Boolean = true)`
- `DragConfiguration.OperationsWithinApp` (class)
  - `DragConfiguration.OperationsWithinApp(allowCopy: Boolean = true, allowMove: Boolean = false, allowDelete: Boolean = false)`
  - `DragConfiguration.OperationsWithinApp(allowMove: Boolean = false)`
- `DropProposal` (class)
  - `DropProposal(withinApplication: DropOperation, outsideApplication: DropOperation)`
  - `DropProposal(operation: DropOperation)`
- `Ellipse` (class)
  - `Ellipse()`
- `EmptyControlWidgetTemplate` (class)
  - `EmptyControlWidgetTemplate()`
- `EventModifiers` (class)
  - `EventModifiers(rawValue: Int)`
  - `EventModifiers.numericPad`
  - `EventModifiers.all`
  - `EventModifiers.shift`
  - `EventModifiers.option`
  - `EventModifiers.command`
  - `EventModifiers.control`
  - `EventModifiers.capsLock`
  - `EventModifiers.function`
- `FillStyle` (class)
  - `FillStyle(eoFill: Boolean = false, antialiased: Boolean = true)`
- `FormPresentationSizing` (class)
- `GestureInputKinds` (class)
  - `GestureInputKinds(rawValue: Int)`
  - `GestureInputKinds.directTouch`
  - `GestureInputKinds.indirectTouch`
  - `GestureInputKinds.all`
  - `GestureInputKinds.pencil`
  - `GestureInputKinds.pointer`
- `GlobalCoordinateSpace` (class)
  - `GlobalCoordinateSpace()`
- `GroupBoxStyle` (interface)
  - `GroupBoxStyle.automatic`
- `HiddenTitleBarWindowStyle` (class)
  - `HiddenTitleBarWindowStyle()`
- `HoverEffect` (class)
  - `HoverEffect(effect: CustomHoverEffect)`
  - `HoverEffect.lift`
  - `HoverEffect.automatic`
  - `HoverEffect.highlight`
- `ImportFromDevicesCommands` (class)
  - `ImportFromDevicesCommands()`
- `InsettableShape` (interface)
- `LabelStyle` (interface)
  - `LabelStyle.titleAndIcon`
  - `LabelStyle.automatic`
  - `LabelStyle.iconOnly`
  - `LabelStyle.titleOnly`
- `LiftHoverEffect` (class)
  - `LiftHoverEffect()`
- `LinkShapeStyle` (class)
  - `LinkShapeStyle()`
- `LongPressGesture` (class)
  - `LongPressGesture(minimumDuration: Double = 0.5, maximumDistance: Double = 10.0, inputKinds: GestureInputKinds = GestureInputKinds.all)`
  - `LongPressGesture(minimumDuration: Double = 0.5, maximumDistance: Double = 10.0)`
  - `LongPressGesture(minimumDuration: Double = 0.5)`
- `MenuActionDismissBehavior` (class)
  - `MenuActionDismissBehavior.enabled`
  - `MenuActionDismissBehavior.disabled`
  - `MenuActionDismissBehavior.automatic`
- `MenuStyle` (interface)
  - `MenuStyle.borderedButton`
  - `MenuStyle.borderlessButton`
  - `MenuStyle.button`
  - `MenuStyle.automatic`
- `NSUserActivity` (object)
- `NSUserActivity.TypedPayloadError` (class)
  - `NSUserActivity.TypedPayloadError.encodingError`
  - `NSUserActivity.TypedPayloadError.invalidContent`
- `NavigationSplitViewColumn` (class)
  - `NavigationSplitViewColumn.detail`
  - `NavigationSplitViewColumn.content`
  - `NavigationSplitViewColumn.sidebar`
- `OpacityTransition` (class)
  - `OpacityTransition()`
- `PagePresentationSizing` (class)
- `Path` (class)
  - `Path.strokedPath(style: StrokeStyle): Path`
  - `Path.trimmedPath(from: Double, to: Double): Path`
  - `Path.normalized(eoFill: Boolean`
  - `Path(roundedRect: CGRect, cornerSize: CGSize, style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
  - `Path(roundedRect: CGRect, cornerRadii: RectangleCornerRadii, style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
  - `Path(roundedRect: CGRect, cornerRadius: Double, style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
  - `Path.subtracting(other: Path, eoFill: Boolean`
  - `Path.intersection(other: Path, eoFill: Boolean`
  - `Path.lineSubtraction(other: Path, eoFill: Boolean`
  - `Path.lineIntersection(other: Path, eoFill: Boolean`
  - `Path.symmetricDifference(other: Path, eoFill: Boolean`
  - `Path.path(inValue: CGRect): Path`
  - `Path.union(other: Path, eoFill: Boolean`
  - `Path.applying(transform: CGAffineTransform): Path`
  - `Path.offsetBy(dx: Double, dy: Double): Path`
  - `Path(ellipseIn: CGRect)`
  - `Path()`
  - `Path.rect(rect: CGRect): Path`
- `Path.Element` (class)
  - `Path.Element.closeSubpath`
  - `Path.Element.line(to: CGPoint): Path.Element`
  - `Path.Element.move(to: CGPoint): Path.Element`
  - `Path.Element.curve(to: CGPoint, control1: CGPoint, control2: CGPoint): Path.Element`
  - `Path.Element.quadCurve(to: CGPoint, control: CGPoint): Path.Element`
- `PlaceholderTextShapeStyle` (class)
  - `PlaceholderTextShapeStyle()`
- `PointerStyle` (class)
  - `PointerStyle.grabActive`
  - `PointerStyle.frameResize(position: FrameResizePosition, directions: FrameResizeDirection.Set`
  - `PointerStyle.columnResize(directions: HorizontalDirection.Set): PointerStyle`
  - `PointerStyle.columnResize`
  - `PointerStyle.verticalText`
  - `PointerStyle.rectSelection`
  - `PointerStyle.horizontalText`
  - `PointerStyle.link`
  - `PointerStyle.zoomIn`
  - `PointerStyle.default`
  - `PointerStyle.zoomOut`
  - `PointerStyle.grabIdle`
  - `PointerStyle.rowResize(directions: VerticalDirection.Set): PointerStyle`
  - `PointerStyle.rowResize`
  - `PointerStyle.image(resource: String, hotSpot: UnitPoint): PointerStyle`
- `PresentationDetent` (class)
  - `PresentationDetent.large`
  - `PresentationDetent.height(height: Double): PresentationDetent`
  - `PresentationDetent.medium`
  - `PresentationDetent.fraction(fraction: Double): PresentationDetent`
- `ProgressViewStyle` (interface)
  - `ProgressViewStyle.linear`
  - `ProgressViewStyle.automatic`
  - `ProgressViewStyle.circular`
- `ProposedViewSize` (class)
  - `ProposedViewSize(width: Double?, height: Double?)`
  - `ProposedViewSize(size: CGSize)`
  - `ProposedViewSize.unspecified`
  - `ProposedViewSize.zero`
  - `ProposedViewSize.infinity`
- `Rectangle` (class)
  - `Rectangle()`
- `RotationGesture` (class)
  - `RotationGesture(minimumAngleDelta: Angle? = null)`
- `SafeAreaRegions` (class)
  - `SafeAreaRegions(rawValue: Int)`
  - `SafeAreaRegions.all`
  - `SafeAreaRegions.keyboard`
  - `SafeAreaRegions.container`
- `ScrollAnchorRole` (class)
  - `ScrollAnchorRole.sizeChanges`
  - `ScrollAnchorRole.initialOffset`
  - `ScrollAnchorRole.alignment`
- `ScrollIndicatorVisibility` (class)
  - `ScrollIndicatorVisibility.never`
  - `ScrollIndicatorVisibility.hidden`
  - `ScrollIndicatorVisibility.visible`
  - `ScrollIndicatorVisibility.automatic`
- `ScrollTargetBehaviorProperties` (class)
  - `ScrollTargetBehaviorProperties()`
- `SearchSuggestionsPlacement` (class)
  - `SearchSuggestionsPlacement.menu`
  - `SearchSuggestionsPlacement.content`
  - `SearchSuggestionsPlacement.automatic`
- `SearchSuggestionsPlacement.Set` (class)
  - `SearchSuggestionsPlacement.Set(rawValue: Int)`
  - `SearchSuggestionsPlacement.Set.menu`
  - `SearchSuggestionsPlacement.Set.content`
- `Shader` (class)
  - `Shader(function: ShaderFunction, arguments: List<Shader.Argument>)`
- `Shader.Argument` (class)
  - `Shader.Argument.colorArray(array: List<Color>): Shader.Argument`
  - `Shader.Argument.boundingRect`
  - `Shader.Argument.color(color: Color): Shader.Argument`
  - `Shader.Argument.float(x: Double): Shader.Argument`
  - `Shader.Argument.float2(size: CGSize): Shader.Argument`
  - `Shader.Argument.float2(point: CGPoint): Shader.Argument`
  - `Shader.Argument.float2(vector: CGVector): Shader.Argument`
  - `Shader.Argument.float2(x: Double, y: Double): Shader.Argument`
  - `Shader.Argument.float3(x: Double, y: Double, z: Double): Shader.Argument`
  - `Shader.Argument.float4(x: Double, y: Double, z: Double, w: Double): Shader.Argument`
- `Shader.UsageType` (class)
  - `Shader.UsageType.shapeStyle`
  - `Shader.UsageType.colorEffect`
  - `Shader.UsageType.layerEffect`
  - `Shader.UsageType.distortionEffect`
- `ShapeStyle` (interface)
  - `ShapeStyle.background`
  - `ShapeStyle.foreground`
  - `ShapeStyle.quaternary`
  - `ShapeStyle.primary`
  - `ShapeStyle.quinary`
  - `ShapeStyle.tertiary`
  - `ShapeStyle.secondary`
  - `ShapeStyle.placeholder`
  - `ShapeStyle.windowBackground`
  - `ShapeStyle.fill`
  - `ShapeStyle.link`
  - `ShapeStyle.tint`
  - `ShapeStyle.selection`
  - `ShapeStyle.separator`
  - `ShapeStyle.linearGradient(stops: List<Gradient.Stop>, startPoint: UnitPoint, endPoint: UnitPoint): LinearGradient`
  - `ShapeStyle.linearGradient(colors: List<Color>, startPoint: UnitPoint, endPoint: UnitPoint): LinearGradient`
  - `ShapeStyle.linearGradient(gradient: Gradient, startPoint: UnitPoint, endPoint: UnitPoint): LinearGradient`
  - `ShapeStyle.radialGradient(stops: List<Gradient.Stop>, center: UnitPoint, startRadius: Double, endRadius: Double): RadialGradient`
  - `ShapeStyle.radialGradient(colors: List<Color>, center: UnitPoint, startRadius: Double, endRadius: Double): RadialGradient`
  - `ShapeStyle.radialGradient(gradient: Gradient, center: UnitPoint, startRadius: Double, endRadius: Double): RadialGradient`
  - `ShapeStyle.conicGradient(stops: List<Gradient.Stop>, center: UnitPoint, angle: Angle`
  - `ShapeStyle.conicGradient(colors: List<Color>, center: UnitPoint, angle: Angle`
  - `ShapeStyle.conicGradient(gradient: Gradient, center: UnitPoint, angle: Angle`
  - `ShapeStyle.angularGradient(stops: List<Gradient.Stop>, center: UnitPoint, startAngle: Angle, endAngle: Angle): AngularGradient`
  - `ShapeStyle.angularGradient(colors: List<Color>, center: UnitPoint, startAngle: Angle, endAngle: Angle): AngularGradient`
  - `ShapeStyle.angularGradient(gradient: Gradient, center: UnitPoint, startAngle: Angle, endAngle: Angle): AngularGradient`
  - `ShapeStyle.ellipticalGradient(stops: List<Gradient.Stop>, center: UnitPoint`
  - `ShapeStyle.ellipticalGradient(colors: List<Color>, center: UnitPoint`
  - `ShapeStyle.ellipticalGradient(gradient: Gradient, center: UnitPoint`
  - `ShapeStyle.red`
  - `ShapeStyle.blue`
  - `ShapeStyle.cyan`
  - `ShapeStyle.gray`
  - `ShapeStyle.mint`
  - `ShapeStyle.pink`
  - `ShapeStyle.teal`
  - `ShapeStyle.black`
  - `ShapeStyle.brown`
  - `ShapeStyle.clear`
  - `ShapeStyle.green`
  - `ShapeStyle.white`
  - `ShapeStyle.indigo`
  - `ShapeStyle.orange`
  - `ShapeStyle.purple`
  - `ShapeStyle.yellow`
  - `ShapeStyle.ultraThickMaterial`
  - `ShapeStyle.thinMaterial`
  - `ShapeStyle.thickMaterial`
  - `ShapeStyle.regularMaterial`
  - `ShapeStyle.ultraThinMaterial`
  - `ShapeStyle.bar`
- `SlideTransition` (class)
  - `SlideTransition()`
- `SpatialTapGesture` (class)
  - `SpatialTapGesture(count: Int = 1, coordinateSpace: CoordinateSpaceProtocol = CoordinateSpaceProtocol.local, inputKinds: GestureInputKinds = GestureInputKinds.all)`
  - `SpatialTapGesture(count: Int = 1, coordinateSpace: CoordinateSpace = CoordinateSpace.local)`
  - `SpatialTapGesture(count: Int = 1, coordinateSpace: CoordinateSpaceProtocol = CoordinateSpaceProtocol.local)`
- `StepperFieldDatePickerStyle` (class)
  - `StepperFieldDatePickerStyle()`
- `SwitchToggleStyle` (class)
  - `SwitchToggleStyle(tint: Color)`
  - `SwitchToggleStyle()`
- `SystemFormatStyle` (object)
- `SystemFormatStyle.DateOffset` (class)
  - `SystemFormatStyle.DateOffset(anchor: Double, maxFieldCount: Int = 2)`
  - `SystemFormatStyle.DateOffset.locale(locale: String): SystemFormatStyle.DateOffset`
  - `SystemFormatStyle.DateOffset.calendar(calendar: String): SystemFormatStyle.DateOffset`
- `SystemFormatStyle.DateReference` (class)
  - `SystemFormatStyle.DateReference(date: Double, maxFieldCount: Int = 2)`
  - `SystemFormatStyle.DateReference.locale(locale: String): SystemFormatStyle.DateReference`
  - `SystemFormatStyle.DateReference.calendar(calendar: String): SystemFormatStyle.DateReference`
- `SystemFormatStyle.Stopwatch` (class)
  - `SystemFormatStyle.Stopwatch(startingAt: Double, showsHours: Boolean = true, maxFieldCount: Int = 4)`
  - `SystemFormatStyle.Stopwatch.locale(locale: String): SystemFormatStyle.Stopwatch`
- `SystemFormatStyle.Timer` (class)
  - `SystemFormatStyle.Timer.locale(locale: String): SystemFormatStyle.Timer`
- `TabRole` (class)
  - `TabRole.search`
  - `TabRole.prominent`
- `TableColumnAlignment` (class)
  - `TableColumnAlignment.center`
  - `TableColumnAlignment.leading`
  - `TableColumnAlignment.numeric`
  - `TableColumnAlignment.trailing`
  - `TableColumnAlignment.automatic`
- `TextAlignment` (class)
  - `TextAlignment.center`
  - `TextAlignment.leading`
  - `TextAlignment.trailing`
- `TextInputBorderShape` (class)
  - `TextInputBorderShape.roundedRectangle`
  - `TextInputBorderShape.capsule`
  - `TextInputBorderShape.automatic`
- `TextVariantPreference` (interface)
  - `TextVariantPreference.sizeDependent`
  - `TextVariantPreference.fixed`
- `TitleOnlyLabelStyle` (class)
  - `TitleOnlyLabelStyle()`
- `ToolbarItemAxisBehavior` (class)
  - `ToolbarItemAxisBehavior.horizontalOnly`
  - `ToolbarItemAxisBehavior.verticalPreferred`
  - `ToolbarItemAxisBehavior.automatic`
- `ToolbarMinimizationSafeAreaAdjustment` (class)
  - `ToolbarMinimizationSafeAreaAdjustment.enabled`
  - `ToolbarMinimizationSafeAreaAdjustment.disabled`
  - `ToolbarMinimizationSafeAreaAdjustment.automatic`
- `ToolbarVerticalBehavior` (class)
  - `ToolbarVerticalBehavior.disabled`
  - `ToolbarVerticalBehavior.automatic`
- `TypesettingLanguage` (class)
  - `TypesettingLanguage.automatic`
  - `TypesettingLanguage.explicit(language: String): TypesettingLanguage`
- `UnitPoint` (class)
  - `UnitPoint(x: Double, y: Double)`
  - `UnitPoint()`
  - `UnitPoint.topLeading`
  - `UnitPoint.topTrailing`
  - `UnitPoint.bottomLeading`
  - `UnitPoint.bottomTrailing`
  - `UnitPoint.top`
  - `UnitPoint.zero`
  - `UnitPoint.bottom`
  - `UnitPoint.center`
  - `UnitPoint.leading`
  - `UnitPoint.trailing`
- `VerticalPageTabViewStyle` (class)
  - `VerticalPageTabViewStyle(transitionStyle: VerticalPageTabViewStyle.TransitionStyle)`
  - `VerticalPageTabViewStyle()`
- `VerticalPageTabViewStyle.TransitionStyle` (class)
  - `VerticalPageTabViewStyle.TransitionStyle.blur`
  - `VerticalPageTabViewStyle.TransitionStyle.identity`
  - `VerticalPageTabViewStyle.TransitionStyle.automatic`
- `WheelPickerStyle` (class)
  - `WheelPickerStyle()`
- `WindowManagerRole` (class)
  - `WindowManagerRole.associated`
  - `WindowManagerRole.automatic`
  - `WindowManagerRole.principal`
- `WindowToolbarStyle` (interface)
  - `WindowToolbarStyle.unifiedCompact(showsTitle: Boolean): UnifiedCompactWindowToolbarStyle`
  - `WindowToolbarStyle.unifiedCompact`
  - `WindowToolbarStyle.automatic`
  - `WindowToolbarStyle.unified(showsTitle: Boolean): UnifiedWindowToolbarStyle`
  - `WindowToolbarStyle.unified`
  - `WindowToolbarStyle.expanded`
- `AccessibilityAdjustmentDirection` (class)
  - `AccessibilityAdjustmentDirection.decrement`
  - `AccessibilityAdjustmentDirection.increment`
- `AccessibilityQuickActionOutlineStyle` (class)
- `AccessibilityTraits` (class)
  - `AccessibilityTraits.intersection(other: AccessibilityTraits): AccessibilityTraits`
  - `AccessibilityTraits.symmetricDifference(other: AccessibilityTraits): AccessibilityTraits`
  - `AccessibilityTraits.union(other: AccessibilityTraits): AccessibilityTraits`
  - `AccessibilityTraits()`
  - `AccessibilityTraits.isSelected`
  - `AccessibilityTraits.playsSound`
  - `AccessibilityTraits.isStaticText`
  - `AccessibilityTraits.isKeyboardKey`
  - `AccessibilityTraits.isSearchField`
  - `AccessibilityTraits.causesPageTurn`
  - `AccessibilityTraits.isSummaryElement`
  - `AccessibilityTraits.updatesFrequently`
  - `AccessibilityTraits.startsMediaSession`
  - `AccessibilityTraits.allowsDirectInteraction`
  - `AccessibilityTraits.isLink`
  - `AccessibilityTraits.isImage`
  - `AccessibilityTraits.isModal`
  - `AccessibilityTraits.isButton`
  - `AccessibilityTraits.isHeader`
  - `AccessibilityTraits.isTabBar`
  - `AccessibilityTraits.isToggle`
- `AccessoryLinearCapacityGaugeStyle` (class)
  - `AccessoryLinearCapacityGaugeStyle()`
- `AlternatingRowBackgroundBehavior` (class)
  - `AlternatingRowBackgroundBehavior.enabled`
  - `AlternatingRowBackgroundBehavior.disabled`
  - `AlternatingRowBackgroundBehavior.automatic`
- `AnyGradient` (class)
  - `AnyGradient.colorSpace(space: Gradient.ColorSpace): AnyGradient`
  - `AnyGradient(gradient: Gradient)`
- `ArrangementViewStyle` (interface)
  - `ArrangementViewStyle.split`
  - `ArrangementViewStyle.overlay`
  - `ArrangementViewStyle.automatic`
- `AutomaticDisclosureGroupStyle` (class)
  - `AutomaticDisclosureGroupStyle()`
- `AutomaticNavigationSplitViewStyle` (class)
  - `AutomaticNavigationSplitViewStyle()`
- `BackgroundProminence` (class)
  - `BackgroundProminence.standard`
  - `BackgroundProminence.increased`
- `BorderedButtonMenuStyle` (class)
  - `BorderedButtonMenuStyle()`
- `BorderlessButtonMenuButtonStyle` (class)
  - `BorderlessButtonMenuButtonStyle()`
- `ButtonRepeatBehavior` (class)
  - `ButtonRepeatBehavior.enabled`
  - `ButtonRepeatBehavior.disabled`
  - `ButtonRepeatBehavior.automatic`
- `CarouselListStyle` (class)
  - `CarouselListStyle()`
- `CircularProgressViewStyle` (class)
  - `CircularProgressViewStyle(tint: Color)`
  - `CircularProgressViewStyle()`
- `ColumnNavigationViewStyle` (class)
- `ContainerBackgroundPlacement` (class)
  - `ContainerBackgroundPlacement.navigation`
  - `ContainerBackgroundPlacement.navigationSplitView`
  - `ContainerBackgroundPlacement.window`
  - `ContainerBackgroundPlacement.tabView`
- `ContentSizeCategory` (class)
  - `ContentSizeCategory.extraExtraExtraLarge`
  - `ContentSizeCategory.accessibilityExtraExtraLarge`
  - `ContentSizeCategory.accessibilityExtraExtraExtraLarge`
  - `ContentSizeCategory.extraLarge`
  - `ContentSizeCategory.extraSmall`
  - `ContentSizeCategory.extraExtraLarge`
  - `ContentSizeCategory.accessibilityLarge`
  - `ContentSizeCategory.accessibilityMedium`
  - `ContentSizeCategory.accessibilityExtraLarge`
  - `ContentSizeCategory.large`
  - `ContentSizeCategory.small`
  - `ContentSizeCategory.medium`
- `ControlWidgetConfiguration` (interface)
- `DatePickerComponents` (class)
  - `DatePickerComponents(rawValue: Int)`
  - `DatePickerComponents.hourAndMinute`
  - `DatePickerComponents.hourMinuteAndSecond`
  - `DatePickerComponents.date`
- `DefaultGlassEffectShape` (class)
  - `DefaultGlassEffectShape()`
- `DefaultNavigationViewStyle` (class)
  - `DefaultNavigationViewStyle()`
- `DefaultToolbarItem` (class)
  - `DefaultToolbarItem(kind: ToolbarDefaultItemKind, placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic)`
- `DisabledTextSelectability` (class)
- `DragDropPreviewsFormation` (class)
  - `DragDropPreviewsFormation.list`
  - `DragDropPreviewsFormation.none`
  - `DragDropPreviewsFormation.pile`
  - `DragDropPreviewsFormation.stack`
  - `DragDropPreviewsFormation.default`
- `DropSession` (object)
- `DropSession.Phase` (class)
  - `DropSession.Phase.dataTransferCompleted`
  - `DropSession.Phase.ended(p0: DropOperation): DropSession.Phase`
  - `DropSession.Phase.active`
  - `DropSession.Phase.exiting`
  - `DropSession.Phase.entering`
- `EllipticalGradient` (class)
  - `EllipticalGradient(stops: List<Gradient.Stop>, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5)`
  - `EllipticalGradient(colors: List<Color>, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5)`
  - `EllipticalGradient(gradient: Gradient, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5)`
- `EmptyModifier` (class)
  - `EmptyModifier()`
  - `EmptyModifier.identity`
- `EveryMinuteTimelineSchedule` (class)
  - `EveryMinuteTimelineSchedule()`
- `FittedPresentationSizing` (class)
- `FormStyle` (interface)
  - `FormStyle.columns`
  - `FormStyle.grouped`
  - `FormStyle.automatic`
- `GestureMask` (class)
  - `GestureMask(rawValue: Int)`
  - `GestureMask.all`
  - `GestureMask.none`
  - `GestureMask.gesture`
  - `GestureMask.subviews`
- `Gradient` (class)
  - `Gradient(stops: List<Gradient.Stop>)`
  - `Gradient(colors: List<Color>)`
- `Gradient.ColorSpace` (class)
  - `Gradient.ColorSpace.perceptual`
  - `Gradient.ColorSpace.device`
- `Gradient.Stop` (class)
  - `Gradient.Stop(color: Color, location: Double)`
- `GroupedFormStyle` (class)
  - `GroupedFormStyle()`
- `HierarchicalShapeStyle` (class)
  - `HierarchicalShapeStyle.quaternary`
  - `HierarchicalShapeStyle.primary`
  - `HierarchicalShapeStyle.quinary`
  - `HierarchicalShapeStyle.tertiary`
  - `HierarchicalShapeStyle.secondary`
- `HoverPhase` (class)
  - `HoverPhase.ended`
  - `HoverPhase.active(p0: CGPoint): HoverPhase`
- `IndexViewStyle` (interface)
  - `IndexViewStyle.page(backgroundDisplayMode: PageIndexViewStyle.BackgroundDisplayMode): PageIndexViewStyle`
  - `IndexViewStyle.page`
- `InspectorCommands` (class)
  - `InspectorCommands()`
- `LabeledContentStyle` (interface)
  - `LabeledContentStyle.automatic`
- `LinearCapacityGaugeStyle` (class)
  - `LinearCapacityGaugeStyle()`
- `ListItemTint` (class)
  - `ListItemTint.monochrome`
  - `ListItemTint.fixed(tint: Color): ListItemTint`
  - `ListItemTint.preferred(tint: Color): ListItemTint`
- `MagnificationGesture` (class)
  - `MagnificationGesture(minimumScaleDelta: Double = 0.01)`
- `MenuBarExtraStyle` (interface)
  - `MenuBarExtraStyle.window`
  - `MenuBarExtraStyle.menu`
  - `MenuBarExtraStyle.automatic`
- `MeshGradient` (object)
- `MeshGradient.Colors` (class)
  - `MeshGradient.Colors.resolvedColors(p0: List<Color.Resolved>): MeshGradient.Colors`
  - `MeshGradient.Colors.colors(p0: List<Color>): MeshGradient.Colors`
- `NamedCoordinateSpace` (class)
- `NavigationSplitViewStyle` (interface)
  - `NavigationSplitViewStyle.prominentDetail`
  - `NavigationSplitViewStyle.balanced`
  - `NavigationSplitViewStyle.automatic`
- `OpenImmersiveSpaceAction` (object)
- `OpenImmersiveSpaceAction.Result` (class)
  - `OpenImmersiveSpaceAction.Result.userCancelled`
  - `OpenImmersiveSpaceAction.Result.error`
  - `OpenImmersiveSpaceAction.Result.opened`
- `PageTabViewStyle` (class)
  - `PageTabViewStyle(indexDisplayMode: PageTabViewStyle.IndexDisplayMode = PageTabViewStyle.IndexDisplayMode.automatic)`
- `PageTabViewStyle.IndexDisplayMode` (class)
  - `PageTabViewStyle.IndexDisplayMode.never`
  - `PageTabViewStyle.IndexDisplayMode.always`
  - `PageTabViewStyle.IndexDisplayMode.automatic`
- `PencilPreferredAction` (class)
  - `PencilPreferredAction.switchEraser`
  - `PencilPreferredAction.switchPrevious`
  - `PencilPreferredAction.showColorPalette`
  - `PencilPreferredAction.runSystemShortcut`
  - `PencilPreferredAction.showInkAttributes`
  - `PencilPreferredAction.showContextualPalette`
  - `PencilPreferredAction.ignore`
- `PlainButtonStyle` (class)
  - `PlainButtonStyle()`
- `PopUpButtonPickerStyle` (class)
  - `PopUpButtonPickerStyle()`
- `PresentationPlacement` (class)
  - `PresentationPlacement.center`
  - `PresentationPlacement.leading`
  - `PresentationPlacement.trailing`
  - `PresentationPlacement.automatic`
- `ProgressiveImmersionAspectRatio` (class)
  - `ProgressiveImmersionAspectRatio.portrait`
  - `ProgressiveImmersionAspectRatio.automatic`
  - `ProgressiveImmersionAspectRatio.landscape`
- `PullDownMenuBarExtraStyle` (class)
  - `PullDownMenuBarExtraStyle()`
- `RectangleCornerInsets` (class)
  - `RectangleCornerInsets(topLeading: CGSize, topTrailing: CGSize, bottomLeading: CGSize, bottomTrailing: CGSize)`
  - `RectangleCornerInsets()`
- `RoundedBorderTextFieldStyle` (class)
  - `RoundedBorderTextFieldStyle()`
- `ScaleTransition` (class)
  - `ScaleTransition(scale: Double, anchor: UnitPoint = UnitPoint.center)`
- `ScrollBounceBehavior` (class)
  - `ScrollBounceBehavior.basedOnSize`
  - `ScrollBounceBehavior.always`
  - `ScrollBounceBehavior.automatic`
- `ScrollInputBehavior` (class)
  - `ScrollInputBehavior.enabled`
  - `ScrollInputBehavior.disabled`
  - `ScrollInputBehavior.automatic`
- `ScrollTransitionConfiguration` (class)
  - `ScrollTransitionConfiguration.animation(animation: Animation): ScrollTransitionConfiguration`
  - `ScrollTransitionConfiguration.threshold(threshold: ScrollTransitionConfiguration.Threshold): ScrollTransitionConfiguration`
  - `ScrollTransitionConfiguration.interactive(timingCurve: UnitCurve`
  - `ScrollTransitionConfiguration.interactive`
  - `ScrollTransitionConfiguration.animated`
  - `ScrollTransitionConfiguration.animated(animation: Animation`
  - `ScrollTransitionConfiguration.identity`
- `ScrollTransitionConfiguration.Threshold` (class)
  - `ScrollTransitionConfiguration.Threshold.interpolated(towards: ScrollTransitionConfiguration.Threshold, amount: Double): ScrollTransitionConfiguration.Threshold`
  - `ScrollTransitionConfiguration.Threshold.inset(distance: Double): ScrollTransitionConfiguration.Threshold`
  - `ScrollTransitionConfiguration.Threshold.hidden`
  - `ScrollTransitionConfiguration.Threshold.visible`
  - `ScrollTransitionConfiguration.Threshold.visible(amount: Double): ScrollTransitionConfiguration.Threshold`
  - `ScrollTransitionConfiguration.Threshold.centered`
- `SearchToolbarBehavior` (class)
  - `SearchToolbarBehavior.minimize`
  - `SearchToolbarBehavior.automatic`
- `ShaderFunction` (class)
  - `ShaderFunction(library: ShaderLibrary, name: String)`
- `SidebarAdaptableTabViewStyle` (class)
  - `SidebarAdaptableTabViewStyle()`
- `SnapshotData` (object)
- `SnapshotData.SnapshotReason` (class)
  - `SnapshotData.SnapshotReason.appScheduled`
  - `SnapshotData.SnapshotReason.appBackgrounded`
  - `SnapshotData.SnapshotReason.complicationUpdate`
  - `SnapshotData.SnapshotReason.returnToDefaultState`
  - `SnapshotData.SnapshotReason.prelaunch`
- `SplitArrangementViewStyle` (class)
  - `SplitArrangementViewStyle.axes(axes: Axis.Set): SplitArrangementViewStyle`
  - `SplitArrangementViewStyle()`
- `StrokeStyle` (class)
  - `StrokeStyle(lineWidth: Double = 1.0, miterLimit: Double = 10.0, dash: List<Double>, dashPhase: Double = 0.0)`
- `SymbolColorRenderingMode` (class)
  - `SymbolColorRenderingMode.flat`
  - `SymbolColorRenderingMode.gradient`
- `TabBarMinimizeBehavior` (class)
  - `TabBarMinimizeBehavior.onScrollUp`
  - `TabBarMinimizeBehavior.onScrollDown`
  - `TabBarMinimizeBehavior.never`
  - `TabBarMinimizeBehavior.automatic`
- `TabSearchActivation` (class)
  - `TabSearchActivation.searchTabSelection`
  - `TabSearchActivation.automatic`
- `TableColumnCustomizationBehavior` (class)
  - `TableColumnCustomizationBehavior.intersection(other: TableColumnCustomizationBehavior): TableColumnCustomizationBehavior`
  - `TableColumnCustomizationBehavior.symmetricDifference(other: TableColumnCustomizationBehavior): TableColumnCustomizationBehavior`
  - `TableColumnCustomizationBehavior.union(other: TableColumnCustomizationBehavior): TableColumnCustomizationBehavior`
  - `TableColumnCustomizationBehavior()`
  - `TableColumnCustomizationBehavior.visibility`
  - `TableColumnCustomizationBehavior.all`
  - `TableColumnCustomizationBehavior.resize`
  - `TableColumnCustomizationBehavior.reorder`
- `TextEditingCommands` (class)
  - `TextEditingCommands()`
- `TextInputDictationActivation` (class)
  - `TextInputDictationActivation.onLook`
  - `TextInputDictationActivation.onSelect`
- `TimelineSchedule` (interface)
  - `TimelineSchedule.everyMinute`
  - `TimelineSchedule.periodic(startDate: Double, interval: Double): PeriodicTimelineSchedule`
  - `TimelineSchedule.animation(minimumInterval: Double?`
  - `TimelineSchedule.animation`
- `ToggleStyle` (interface)
  - `ToggleStyle.button`
  - `ToggleStyle.switch`
  - `ToggleStyle.automatic`
  - `ToggleStyle.checkbox`
- `ToolbarItemPlacement` (class)
  - `ToolbarItemPlacement(id: Any)`
  - `ToolbarItemPlacement.largeTitle`
  - `ToolbarItemPlacement.navigation`
  - `ToolbarItemPlacement.accessoryBar(id: Any): ToolbarItemPlacement`
  - `ToolbarItemPlacement.largeSubtitle`
  - `ToolbarItemPlacement.primaryAction`
  - `ToolbarItemPlacement.topBarLeading`
  - `ToolbarItemPlacement.topBarTrailing`
  - `ToolbarItemPlacement.secondaryAction`
  - `ToolbarItemPlacement.destructiveAction`
  - `ToolbarItemPlacement.cancellationAction`
  - `ToolbarItemPlacement.confirmationAction`
  - `ToolbarItemPlacement.navigationBarLeading`
  - `ToolbarItemPlacement.topBarPinnedTrailing`
  - `ToolbarItemPlacement.navigationBarTrailing`
  - `ToolbarItemPlacement.title`
  - `ToolbarItemPlacement.status`
  - `ToolbarItemPlacement.keyboard`
  - `ToolbarItemPlacement.subtitle`
  - `ToolbarItemPlacement.automatic`
  - `ToolbarItemPlacement.bottomBar`
  - `ToolbarItemPlacement.principal`
- `ToolbarPlacement` (class)
  - `ToolbarPlacement(id: Any)`
  - `ToolbarPlacement.windowToolbar`
  - `ToolbarPlacement.accessoryBar(id: Any): ToolbarPlacement`
  - `ToolbarPlacement.navigationBar`
  - `ToolbarPlacement.tabBar`
  - `ToolbarPlacement.automatic`
  - `ToolbarPlacement.bottomBar`
  - `ToolbarPlacement.statusBar`
- `ToolbarVerticalCompressionBehavior` (class)
  - `ToolbarVerticalCompressionBehavior.prefersToolbarItems`
  - `ToolbarVerticalCompressionBehavior.prefersTabBar`
  - `ToolbarVerticalCompressionBehavior.automatic`
- `UIHostingControllerSizingOptions` (class)
  - `UIHostingControllerSizingOptions(rawValue: Int)`
  - `UIHostingControllerSizingOptions.intrinsicContentSize`
  - `UIHostingControllerSizingOptions.preferredContentSize`
- `UserInterfaceSizeClass` (class)
  - `UserInterfaceSizeClass.compact`
  - `UserInterfaceSizeClass.regular`
- `ViewAlignedScrollTargetBehavior` (class)
  - `ViewAlignedScrollTargetBehavior(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior, anchor: UnitPoint?)`
  - `ViewAlignedScrollTargetBehavior(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior = ViewAlignedScrollTargetBehavior.LimitBehavior.automatic)`
  - `ViewAlignedScrollTargetBehavior(anchor: UnitPoint?)`
- `ViewAlignedScrollTargetBehavior.LimitBehavior` (class)
  - `ViewAlignedScrollTargetBehavior.LimitBehavior.alwaysByFew`
  - `ViewAlignedScrollTargetBehavior.LimitBehavior.alwaysByOne`
  - `ViewAlignedScrollTargetBehavior.LimitBehavior.never`
  - `ViewAlignedScrollTargetBehavior.LimitBehavior.always`
  - `ViewAlignedScrollTargetBehavior.LimitBehavior.automatic`
- `WindowBackgroundShapeStyle` (class)
  - `WindowBackgroundShapeStyle()`
- `WindowMenuBarExtraStyle` (class)
  - `WindowMenuBarExtraStyle()`
- `WorldRecenterPhase` (class)
  - `WorldRecenterPhase.began`
  - `WorldRecenterPhase.ended`
- `AccessibilityChildBehavior` (class)
  - `AccessibilityChildBehavior.ignore`
  - `AccessibilityChildBehavior.combine`
  - `AccessibilityChildBehavior.contain`
- `AccessibilityQuickActionPromptStyle` (class)
- `AccessibilityZoomGestureAction` (object)
- `AccessibilityZoomGestureAction.Direction` (class)
  - `AccessibilityZoomGestureAction.Direction.zoomIn`
  - `AccessibilityZoomGestureAction.Direction.zoomOut`
- `AccessoryLinearGaugeStyle` (class)
  - `AccessoryLinearGaugeStyle()`
- `Angle` (class)
  - `Angle(degrees: Double)`
  - `Angle()`
  - `Angle.zero`
  - `Angle.degrees(degrees: Double): Angle`
  - `Angle.radians(radians: Double): Angle`
  - `Angle.radiansRadians(radians: Double): Angle`
- `AnyNavigationTransition` (class)
  - `AnyNavigationTransition(transition: NavigationTransition)`
- `AsyncImagePhase` (class)
  - `AsyncImagePhase.empty`
- `AutomaticFormStyle` (class)
  - `AutomaticFormStyle()`
- `AutomaticNavigationTransition` (class)
- `BackgroundStyle` (class)
  - `BackgroundStyle()`
- `BorderedButtonStyle` (class)
  - `BorderedButtonStyle(tint: Color)`
  - `BorderedButtonStyle()`
- `BorderlessButtonMenuStyle` (class)
  - `BorderlessButtonMenuStyle()`
  - `BorderlessButtonMenuStyle(showsMenuIndicator: Boolean)`
- `ButtonRole` (class)
  - `ButtonRole.destructive`
  - `ButtonRole.close`
  - `ButtonRole.cancel`
  - `ButtonRole.confirm`
- `CarouselTabViewStyle` (class)
  - `CarouselTabViewStyle()`
- `Color` (class)
  - `Color.exposureAdjust(stops: Double): Color`
  - `Color(hue: Double, saturation: Double, brightness: Double, opacity: Double = 1.0)`
  - `Color.mix(rhs: Color, fraction: Double, colorSpace: Gradient.ColorSpace`
  - `Color.opacity(opacity: Double): Color`
  - `Color.headroom(headroom: Double?): Color`
  - `Color(colorSpace: Color.RGBColorSpace = Color.RGBColorSpace.sRGB, red: Double, green: Double, blue: Double, opacity: Double = 1.0)`
  - `Color(colorSpace: Color.RGBColorSpace = Color.RGBColorSpace.sRGB, white: Double, opacity: Double = 1.0)`
  - `Color(name: String)`
  - `Color(resolved: Color.ResolvedHDR)`
  - `Color(resolved: Color.Resolved)`
  - `Color.accentColor`
  - `Color.red`
  - `Color.blue`
  - `Color.cyan`
  - `Color.gray`
  - `Color.mint`
  - `Color.pink`
  - `Color.teal`
  - `Color.black`
  - `Color.brown`
  - `Color.clear`
  - `Color.green`
  - `Color.white`
  - `Color.indigo`
  - `Color.orange`
  - `Color.purple`
  - `Color.yellow`
  - `Color.primary`
  - `Color.secondary`
  - `Color.resource(resource: String): Color`
- `Color.RGBColorSpace` (class)
  - `Color.RGBColorSpace.sRGBLinear`
  - `Color.RGBColorSpace.sRGB`
  - `Color.RGBColorSpace.displayP3`
- `Color.Resolved` (class)
  - `Color.Resolved(colorSpace: Color.RGBColorSpace = Color.RGBColorSpace.sRGB, red: Float, green: Float, blue: Float, opacity: Float = 1.0f)`
- `Color.ResolvedHDR` (class)
  - `Color.ResolvedHDR(color: Color.Resolved, headroom: Float? = null)`
- `ColumnsFormStyle` (class)
  - `ColumnsFormStyle()`
- `ContainerRelativeShape` (class)
  - `ContainerRelativeShape()`
- `ContentToolbarPlacement` (class)
  - `ContentToolbarPlacement.tabViewSidebar`
- `ControlWidgetTemplate` (interface)
- `DatePickerStyle` (interface)
  - `DatePickerStyle.stepperField`
  - `DatePickerStyle.field`
  - `DatePickerStyle.wheel`
  - `DatePickerStyle.compact`
  - `DatePickerStyle.automatic`
  - `DatePickerStyle.graphical`
- `DefaultGroupBoxStyle` (class)
  - `DefaultGroupBoxStyle()`
- `DefaultPickerStyle` (class)
  - `DefaultPickerStyle()`
- `DefaultWindowStyle` (class)
  - `DefaultWindowStyle()`
- `DisclosureGroupStyle` (interface)
  - `DisclosureGroupStyle.automatic`
- `DragGesture` (class)
  - `DragGesture(minimumDistance: Double = 10.0, coordinateSpace: CoordinateSpaceProtocol = CoordinateSpaceProtocol.local, inputKinds: GestureInputKinds = GestureInputKinds.all)`
  - `DragGesture(minimumDistance: Double = 10.0, coordinateSpace: CoordinateSpace = CoordinateSpace.local)`
  - `DragGesture(minimumDistance: Double = 10.0, coordinateSpace: CoordinateSpaceProtocol = CoordinateSpaceProtocol.local)`
- `DynamicTypeSize` (class)
  - `DynamicTypeSize.accessibility1`
  - `DynamicTypeSize.accessibility2`
  - `DynamicTypeSize.accessibility3`
  - `DynamicTypeSize.accessibility4`
  - `DynamicTypeSize.accessibility5`
  - `DynamicTypeSize.large`
  - `DynamicTypeSize.small`
  - `DynamicTypeSize.medium`
  - `DynamicTypeSize.xLarge`
  - `DynamicTypeSize.xSmall`
  - `DynamicTypeSize.xxLarge`
  - `DynamicTypeSize.xxxLarge`
- `EllipticalListStyle` (class)
  - `EllipticalListStyle()`
- `EmptyVisualEffect` (class)
  - `EmptyVisualEffect()`
- `ExpandedWindowToolbarStyle` (class)
  - `ExpandedWindowToolbarStyle()`
- `FixedTextVariant` (class)
- `FrameResizeDirection` (class)
  - `FrameResizeDirection.inward`
  - `FrameResizeDirection.outward`
- `FrameResizeDirection.Set` (class)
  - `FrameResizeDirection.Set(direction: FrameResizeDirection)`
  - `FrameResizeDirection.Set(rawValue: Int)`
  - `FrameResizeDirection.Set.all`
  - `FrameResizeDirection.Set.inward`
  - `FrameResizeDirection.Set.outward`
- `Glass` (class)
  - `Glass.interactive(isEnabled: Boolean`
  - `Glass.tint(color: Color?): Glass`
  - `Glass.clear`
  - `Glass.regular`
  - `Glass.identity`
- `GraphicalDatePickerStyle` (class)
  - `GraphicalDatePickerStyle()`
- `GroupedListStyle` (class)
  - `GroupedListStyle()`
- `HighlightHoverEffect` (class)
  - `HighlightHoverEffect()`
- `IconOnlyLabelStyle` (class)
  - `IconOnlyLabelStyle()`
- `InlinePickerStyle` (class)
  - `InlinePickerStyle()`
- `InterfaceOrientation` (class)
  - `InterfaceOrientation.landscapeLeft`
  - `InterfaceOrientation.landscapeRight`
  - `InterfaceOrientation.portraitUpsideDown`
  - `InterfaceOrientation.portrait`
- `LayoutDirection` (class)
  - `LayoutDirection.leftToRight`
  - `LayoutDirection.rightToLeft`
- `LinearGaugeStyle` (class)
  - `LinearGaugeStyle(tint: Color)`
  - `LinearGaugeStyle(tint: Gradient)`
  - `LinearGaugeStyle()`
- `ListSectionSpacing` (class)
  - `ListSectionSpacing.custom(spacing: Double): ListSectionSpacing`
  - `ListSectionSpacing.compact`
  - `ListSectionSpacing.default`
- `MagnifyGesture` (class)
  - `MagnifyGesture(minimumScaleDelta: Double = 0.01, inputKinds: GestureInputKinds = GestureInputKinds.all)`
  - `MagnifyGesture(minimumScaleDelta: Double = 0.01)`
- `MenuButtonStyle` (interface)
- `MoveCommandDirection` (class)
  - `MoveCommandDirection.up`
  - `MoveCommandDirection.down`
  - `MoveCommandDirection.left`
  - `MoveCommandDirection.right`
- `NavigationBarItem` (object)
- `NavigationBarItem.TitleDisplayMode` (class)
  - `NavigationBarItem.TitleDisplayMode.large`
  - `NavigationBarItem.TitleDisplayMode.inline`
  - `NavigationBarItem.TitleDisplayMode.automatic`
- `NavigationSplitViewVisibility` (class)
  - `NavigationSplitViewVisibility.detailOnly`
  - `NavigationSplitViewVisibility.doubleColumn`
  - `NavigationSplitViewVisibility.all`
  - `NavigationSplitViewVisibility.automatic`
- `OpenURLAction` (object)
- `OpenURLAction.Result` (class)
  - `OpenURLAction.Result.systemAction`
  - `OpenURLAction.Result.systemAction(url: String?`
  - `OpenURLAction.Result.systemAction(url: String): OpenURLAction.Result`
  - `OpenURLAction.Result.handled`
  - `OpenURLAction.Result.discarded`
- `PagingScrollTargetBehavior` (class)
  - `PagingScrollTargetBehavior()`
- `PencilSqueezeGesturePhase` (class)
  - `PencilSqueezeGesturePhase.failed`
- `PlainListStyle` (class)
  - `PlainListStyle()`
- `PopoverAttachmentAnchor` (class)
  - `PopoverAttachmentAnchor.point(p0: UnitPoint): PopoverAttachmentAnchor`
- `PresentationSizing` (interface)
  - `PresentationSizing.form`
  - `PresentationSizing.page`
  - `PresentationSizing.fitted`
  - `PresentationSizing.automatic`
- `ProgressiveImmersionStyle` (class)
  - `ProgressiveImmersionStyle(immersion: ClosedFloatingPointRange<Double>, initialAmount: Double? = null)`
  - `ProgressiveImmersionStyle()`
- `PullDownMenuButtonStyle` (class)
  - `PullDownMenuButtonStyle()`
- `RectangleCornerRadii` (class)
  - `RectangleCornerRadii(topLeading: Double = 0.0, bottomLeading: Double = 0.0, bottomTrailing: Double = 0.0, topTrailing: Double = 0.0)`
- `RoundedCornerStyle` (class)
  - `RoundedCornerStyle.continuous`
  - `RoundedCornerStyle.circular`
- `SceneLaunchBehavior` (class)
  - `SceneLaunchBehavior.suppressed`
  - `SceneLaunchBehavior.automatic`
  - `SceneLaunchBehavior.presented`
- `ScrollContentOffsetAdjustmentBehavior` (class)
  - `ScrollContentOffsetAdjustmentBehavior.disabled`
  - `ScrollContentOffsetAdjustmentBehavior.automatic`
- `ScrollInputKind` (class)
  - `ScrollInputKind.handGestureShortcut`
- `ScrollTransitionPhase` (class)
  - `ScrollTransitionPhase.topLeading`
  - `ScrollTransitionPhase.bottomTrailing`
  - `ScrollTransitionPhase.identity`
- `SegmentedPickerStyle` (class)
  - `SegmentedPickerStyle()`
- `ShaderLibrary` (class)
  - `ShaderLibrary(url: String)`
  - `ShaderLibrary.default`
- `SidebarCommands` (class)
  - `SidebarCommands()`
- `SnapshotResponse` (class)
  - `SnapshotResponse(restoredDefaultState: Boolean = false, estimatedSnapshotExpiration: Double? = null, identifier: String? = null)`
- `Spring` (class)
  - `Spring(settlingDuration: Double, dampingRatio: Double, epsilon: Double = 0.001)`
  - `Spring(mass: Double = 1.0, stiffness: Double, damping: Double, allowOverDamping: Boolean = false)`
  - `Spring(duration: Double = 0.5, bounce: Double = 0.0)`
  - `Spring.bouncy(duration: Double`
  - `Spring.bouncy`
  - `Spring.smooth(duration: Double`
  - `Spring.smooth`
  - `Spring.snappy(duration: Double`
  - `Spring.snappy`
  - `Spring.response(response: Double, dampingRatio: Double): Spring`
- `SubmitLabel` (class)
  - `SubmitLabel.go`
  - `SubmitLabel.done`
  - `SubmitLabel.join`
  - `SubmitLabel.next`
  - `SubmitLabel.send`
  - `SubmitLabel.route`
  - `SubmitLabel.`return``
  - `SubmitLabel.search`
  - `SubmitLabel.`continue``
- `SymbolEffectTransition` (class)
- `TabBarOnlyTabViewStyle` (class)
  - `TabBarOnlyTabViewStyle()`
- `TabSectionExpansion` (class)
  - `TabSectionExpansion.expanded`
  - `TabSectionExpansion.automatic`
  - `TabSectionExpansion.collapsed`
- `TableStyle` (interface)
  - `TableStyle.inset(alternatesRowBackgrounds: Boolean): InsetTableStyle`
  - `TableStyle.inset`
  - `TableStyle.bordered(alternatesRowBackgrounds: Boolean): BorderedTableStyle`
  - `TableStyle.bordered`
  - `TableStyle.automatic`
- `TextEditorStyle` (interface)
  - `TextEditorStyle.plain`
  - `TextEditorStyle.automatic`
- `TextInputDictationBehavior` (class)
  - `TextInputDictationBehavior.inline(activation: TextInputDictationActivation): TextInputDictationBehavior`
  - `TextInputDictationBehavior.automatic`
- `TimelineScheduleMode` (class)
  - `TimelineScheduleMode.lowFrequency`
  - `TimelineScheduleMode.normal`
- `ToolbarCommands` (class)
  - `ToolbarCommands()`
- `ToolbarItemVisibilityPriority` (class)
  - `ToolbarItemVisibilityPriority(higherThan: ToolbarItemVisibilityPriority)`
  - `ToolbarItemVisibilityPriority.low`
  - `ToolbarItemVisibilityPriority.high`
  - `ToolbarItemVisibilityPriority.automatic`
  - `ToolbarItemVisibilityPriority.lowerThan(lowerThan: ToolbarItemVisibilityPriority): ToolbarItemVisibilityPriority`
- `ToolbarRole` (class)
  - `ToolbarRole.navigationStack`
  - `ToolbarRole.editor`
  - `ToolbarRole.browser`
  - `ToolbarRole.automatic`
- `TouchBarItemPresence` (class)
  - `TouchBarItemPresence.default(p0: String): TouchBarItemPresence`
  - `TouchBarItemPresence.optional(p0: String): TouchBarItemPresence`
  - `TouchBarItemPresence.required(p0: String): TouchBarItemPresence`
- `UnevenRoundedRectangle` (class)
  - `UnevenRoundedRectangle(cornerRadii: RectangleCornerRadii, style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
  - `UnevenRoundedRectangle(topLeadingRadius: Double = 0.0, bottomLeadingRadius: Double = 0.0, bottomTrailingRadius: Double = 0.0, topTrailingRadius: Double = 0.0, style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
- `VStackLayout` (class)
  - `VStackLayout(alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null)`
- `ViewSpacing` (class)
  - `ViewSpacing.union(other: ViewSpacing, edges: Edge.Set`
  - `ViewSpacing()`
  - `ViewSpacing.zero`
- `WindowDragGesture` (class)
  - `WindowDragGesture()`
- `WindowPlacement` (class)
  - `WindowPlacement(x: Double? = null, y: Double? = null, width: Double? = null, height: Double? = null)`
  - `WindowPlacement(position: CGPoint? = null, size: CGSize? = null)`
  - `WindowPlacement(position: UnitPoint, size: CGSize? = null)`
  - `WindowPlacement(position: UnitPoint, width: Double? = null, height: Double? = null)`
- `WorldTrackingLimitation` (class)
  - `WorldTrackingLimitation.orientation`
  - `WorldTrackingLimitation.translation`
- `AccessibilityCustomContentKey` (class)
  - `AccessibilityCustomContentKey(label: LocalizedKey, id: String)`
  - `AccessibilityCustomContentKey(label: String, id: String)`
  - `AccessibilityCustomContentKey(label: LocalizedKey)`
  - `AccessibilityCustomContentKey.labelKey(labelKey: LocalizedKey, id: String): AccessibilityCustomContentKey`
  - `AccessibilityCustomContentKey.labelKey(labelKey: LocalizedKey): AccessibilityCustomContentKey`
- `AccessibilityQuickActionStyle` (interface)
  - `AccessibilityQuickActionStyle.prompt`
  - `AccessibilityQuickActionStyle.outline`
- `AccessoryBarActionButtonStyle` (class)
  - `AccessoryBarActionButtonStyle()`
- `ActionSheet` (class)
  - `ActionSheet(title: String, message: String? = null, buttons: List<Alert.Button>)`
- `AngularGradient` (class)
  - `AngularGradient(stops: List<Gradient.Stop>, center: UnitPoint, startAngle: Angle, endAngle: Angle)`
  - `AngularGradient(stops: List<Gradient.Stop>, center: UnitPoint, angle: Angle = Angle.zero)`
  - `AngularGradient(colors: List<Color>, center: UnitPoint, startAngle: Angle, endAngle: Angle)`
  - `AngularGradient(colors: List<Color>, center: UnitPoint, angle: Angle = Angle.zero)`
  - `AngularGradient(gradient: Gradient, center: UnitPoint, startAngle: Angle = Angle.zero, endAngle: Angle = Angle.zero)`
  - `AngularGradient(gradient: Gradient, center: UnitPoint, angle: Angle = Angle.zero)`
- `AnyScrollTargetBehavior` (class)
  - `AnyScrollTargetBehavior(base: ScrollTargetBehavior)`
- `AttributedTextFormatting` (object)
- `AttributedTextFormatting.Transferable` (class)
  - `AttributedTextFormatting.Transferable(text: String, environment: EnvironmentValues)`
- `AutomaticHoverEffect` (class)
  - `AutomaticHoverEffect()`
- `AutomaticPresentationSizing` (class)
- `BadgeProminence` (class)
  - `BadgeProminence.standard`
  - `BadgeProminence.decreased`
  - `BadgeProminence.increased`
- `BorderedListStyle` (class)
  - `BorderedListStyle(alternatesRowBackgrounds: Boolean)`
  - `BorderedListStyle()`
- `BorderlessButtonStyle` (class)
  - `BorderlessButtonStyle()`
- `ButtonSizing` (class)
  - `ButtonSizing.fitted`
  - `ButtonSizing.flexible`
  - `ButtonSizing.automatic`
- `CheckboxToggleStyle` (class)
  - `CheckboxToggleStyle()`
- `ColorMatrix` (class)
  - `ColorMatrix()`
- `CommandGroupPlacement` (class)
  - `CommandGroupPlacement.pasteboard`
  - `CommandGroupPlacement.windowList`
  - `CommandGroupPlacement.windowSize`
  - `CommandGroupPlacement.appSettings`
  - `CommandGroupPlacement.textEditing`
  - `CommandGroupPlacement.importExport`
  - `CommandGroupPlacement.appVisibility`
  - `CommandGroupPlacement.appTermination`
  - `CommandGroupPlacement.systemServices`
  - `CommandGroupPlacement.textFormatting`
  - `CommandGroupPlacement.singleWindowList`
  - `CommandGroupPlacement.windowArrangement`
  - `CommandGroupPlacement.help`
  - `CommandGroupPlacement.appInfo`
  - `CommandGroupPlacement.newItem`
  - `CommandGroupPlacement.sidebar`
  - `CommandGroupPlacement.toolbar`
  - `CommandGroupPlacement.saveItem`
  - `CommandGroupPlacement.undoRedo`
  - `CommandGroupPlacement.printItem`
- `ContentMarginGuide` (class)
  - `ContentMarginGuide.container`
- `ContentTransition` (class)
  - `ContentTransition.interpolate`
  - `ContentTransition.numericText(countsDown: Boolean`
  - `ContentTransition.numericText(value: Double): ContentTransition`
  - `ContentTransition.symbolEffect`
  - `ContentTransition.opacity`
  - `ContentTransition.identity`
- `CoordinateSpace` (class)
  - `CoordinateSpace.local`
  - `CoordinateSpace.global`
- `DefaultButtonStyle` (class)
  - `DefaultButtonStyle()`
- `DefaultLabelStyle` (class)
  - `DefaultLabelStyle()`
- `DefaultProgressViewStyle` (class)
  - `DefaultProgressViewStyle()`
- `DefaultWindowToolbarStyle` (class)
  - `DefaultWindowToolbarStyle()`
- `DismissBehavior` (class)
  - `DismissBehavior.destructive`
  - `DismissBehavior.interactive`
- `DragSession` (object)
- `DragSession.Phase` (class)
  - `DragSession.Phase.dataTransferCompleted`
  - `DragSession.Phase.ended(p0: DropOperation): DragSession.Phase`
  - `DragSession.Phase.active`
  - `DragSession.Phase.ending(p0: DropOperation): DragSession.Phase`
  - `DragSession.Phase.initial`
- `Edge` (class)
  - `Edge.top`
  - `Edge.bottom`
  - `Edge.leading`
  - `Edge.trailing`
- `Edge.Corner` (class)
  - `Edge.Corner.topLeading`
  - `Edge.Corner.topTrailing`
  - `Edge.Corner.bottomLeading`
  - `Edge.Corner.bottomTrailing`
- `Edge.Corner.Set` (class)
  - `Edge.Corner.Set(corner: Edge.Corner)`
  - `Edge.Corner.Set(rawValue: Int)`
  - `Edge.Corner.Set.topLeading`
  - `Edge.Corner.Set.topTrailing`
  - `Edge.Corner.Set.bottomLeading`
  - `Edge.Corner.Set.bottomTrailing`
  - `Edge.Corner.Set.all`
  - `Edge.Corner.Set.top`
  - `Edge.Corner.Set.none`
  - `Edge.Corner.Set.bottom`
  - `Edge.Corner.Set.leading`
  - `Edge.Corner.Set.trailing`
- `Edge.Corner.Style` (class)
  - `Edge.Corner.Style(floatLiteral: Float)`
  - `Edge.Corner.Style(integerLiteral: Int)`
  - `Edge.Corner.Style.concentric(minimum: Edge.Corner.Style?`
  - `Edge.Corner.Style.concentric`
  - `Edge.Corner.Style.fixed(radius: Double): Edge.Corner.Style`
- `Edge.Corner.Style.AnimatableData` (class)
  - `Edge.Corner.Style.AnimatableData.zero`
- `Edge.Set` (class)
  - `Edge.Set(e: Edge)`
  - `Edge.Set(rawValue: Int)`
  - `Edge.Set.horizontal`
  - `Edge.Set.all`
  - `Edge.Set.top`
  - `Edge.Set.bottom`
  - `Edge.Set.leading`
  - `Edge.Set.trailing`
  - `Edge.Set.vertical`
- `EmptyAnimatableData` (class)
  - `EmptyAnimatableData()`
  - `EmptyAnimatableData.zero`
- `EmptyWidgetConfiguration` (class)
  - `EmptyWidgetConfiguration()`
- `FieldDatePickerStyle` (class)
  - `FieldDatePickerStyle()`
- `FocusInteractions` (class)
  - `FocusInteractions(rawValue: Int)`
  - `FocusInteractions.edit`
  - `FocusInteractions.activate`
  - `FocusInteractions.automatic`
- `FrameResizePosition` (class)
  - `FrameResizePosition.topLeading`
  - `FrameResizePosition.topTrailing`
  - `FrameResizePosition.bottomLeading`
  - `FrameResizePosition.bottomTrailing`
  - `FrameResizePosition.top`
  - `FrameResizePosition.bottom`
  - `FrameResizePosition.leading`
  - `FrameResizePosition.trailing`
- `GlassButtonStyle` (class)
  - `GlassButtonStyle()`
  - `GlassButtonStyle(glass: Glass)`
- `GraphicsContext` (object)
- `GraphicsContext.BlendMode` (class)
  - `GraphicsContext.BlendMode(rawValue: Int)`
  - `GraphicsContext.BlendMode.colorDodge`
  - `GraphicsContext.BlendMode.difference`
  - `GraphicsContext.BlendMode.luminosity`
  - `GraphicsContext.BlendMode.plusDarker`
  - `GraphicsContext.BlendMode.saturation`
  - `GraphicsContext.BlendMode.sourceAtop`
  - `GraphicsContext.BlendMode.plusLighter`
  - `GraphicsContext.BlendMode.destinationIn`
  - `GraphicsContext.BlendMode.destinationOut`
  - `GraphicsContext.BlendMode.destinationAtop`
  - `GraphicsContext.BlendMode.destinationOver`
  - `GraphicsContext.BlendMode.hue`
  - `GraphicsContext.BlendMode.xor`
  - `GraphicsContext.BlendMode.copy`
  - `GraphicsContext.BlendMode.clear`
  - `GraphicsContext.BlendMode.color`
  - `GraphicsContext.BlendMode.darken`
  - `GraphicsContext.BlendMode.normal`
  - `GraphicsContext.BlendMode.screen`
  - `GraphicsContext.BlendMode.lighten`
  - `GraphicsContext.BlendMode.overlay`
  - `GraphicsContext.BlendMode.multiply`
  - `GraphicsContext.BlendMode.sourceIn`
  - `GraphicsContext.BlendMode.colorBurn`
  - `GraphicsContext.BlendMode.exclusion`
  - `GraphicsContext.BlendMode.hardLight`
  - `GraphicsContext.BlendMode.softLight`
  - `GraphicsContext.BlendMode.sourceOut`
- `GraphicsContext.BlurOptions` (class)
  - `GraphicsContext.BlurOptions(rawValue: Int)`
  - `GraphicsContext.BlurOptions.dithersResult`
  - `GraphicsContext.BlurOptions.opaque`
- `GraphicsContext.ClipOptions` (class)
  - `GraphicsContext.ClipOptions(rawValue: Int)`
  - `GraphicsContext.ClipOptions.inverse`
- `GraphicsContext.Filter` (class)
  - `GraphicsContext.Filter.brightness(amount: Double): GraphicsContext.Filter`
  - `GraphicsContext.Filter.saturation(amount: Double): GraphicsContext.Filter`
  - `GraphicsContext.Filter.colorInvert(amount: Double`
  - `GraphicsContext.Filter.colorMatrix(matrix: ColorMatrix, isPremultiplied: Boolean): GraphicsContext.Filter`
  - `GraphicsContext.Filter.colorMatrix(matrix: ColorMatrix): GraphicsContext.Filter`
  - `GraphicsContext.Filter.colorShader(shader: Shader): GraphicsContext.Filter`
  - `GraphicsContext.Filter.hueRotation(angle: Angle): GraphicsContext.Filter`
  - `GraphicsContext.Filter.layerShader(shader: Shader, maxSampleOffset: CGSize): GraphicsContext.Filter`
  - `GraphicsContext.Filter.alphaMultiply(color: Color): GraphicsContext.Filter`
  - `GraphicsContext.Filter.colorMultiply(color: Color): GraphicsContext.Filter`
  - `GraphicsContext.Filter.alphaThreshold(min: Double, max: Double`
  - `GraphicsContext.Filter.distortionShader(shader: Shader, maxSampleOffset: CGSize): GraphicsContext.Filter`
  - `GraphicsContext.Filter.luminanceToAlpha`
  - `GraphicsContext.Filter.projectionTransform(matrix: ProjectionTransform): GraphicsContext.Filter`
  - `GraphicsContext.Filter.blur(radius: Double, options: GraphicsContext.BlurOptions?`
  - `GraphicsContext.Filter.shadow(color: Color?`
  - `GraphicsContext.Filter.contrast(amount: Double): GraphicsContext.Filter`
  - `GraphicsContext.Filter.grayscale(amount: Double): GraphicsContext.Filter`
- `GraphicsContext.FilterOptions` (class)
  - `GraphicsContext.FilterOptions(rawValue: Int)`
  - `GraphicsContext.FilterOptions.linearColor`
- `GraphicsContext.GradientOptions` (class)
  - `GraphicsContext.GradientOptions(rawValue: Int)`
  - `GraphicsContext.GradientOptions.linearColor`
  - `GraphicsContext.GradientOptions.mirror`
  - `GraphicsContext.GradientOptions.repeat`
- `GraphicsContext.Shading` (class)
  - `GraphicsContext.Shading.foreground`
  - `GraphicsContext.Shading.conicGradient(gradient: AnyGradient, center: CGPoint, angle: Angle`
  - `GraphicsContext.Shading.conicGradient(gradient: Gradient, center: CGPoint, angle: Angle`
  - `GraphicsContext.Shading.linearGradient(gradient: AnyGradient, startPoint: CGPoint, endPoint: CGPoint, options: GraphicsContext.GradientOptions?`
  - `GraphicsContext.Shading.linearGradient(gradient: Gradient, startPoint: CGPoint, endPoint: CGPoint, options: GraphicsContext.GradientOptions?`
  - `GraphicsContext.Shading.radialGradient(gradient: AnyGradient, startCenter: CGPoint, startRadius: Double, endCenter: CGPoint, endRadius: Double, options: GraphicsContext.GradientOptions?`
  - `GraphicsContext.Shading.radialGradient(gradient: Gradient, startCenter: CGPoint, startRadius: Double, endCenter: CGPoint, endRadius: Double, options: GraphicsContext.GradientOptions?`
  - `GraphicsContext.Shading.radialGradient(gradient: AnyGradient, center: CGPoint, startRadius: Double, endRadius: Double, options: GraphicsContext.GradientOptions?`
  - `GraphicsContext.Shading.radialGradient(gradient: Gradient, center: CGPoint, startRadius: Double, endRadius: Double, options: GraphicsContext.GradientOptions?`
  - `GraphicsContext.Shading.color(colorSpace: Color.RGBColorSpace`
  - `GraphicsContext.Shading.color(colorSpace: Color.RGBColorSpace`
  - `GraphicsContext.Shading.color(color: Color): GraphicsContext.Shading`
  - `GraphicsContext.Shading.style(style: ShapeStyle): GraphicsContext.Shading`
  - `GraphicsContext.Shading.shader(shader: Shader, bounds: CGRect?`
  - `GraphicsContext.Shading.palette(array: List<GraphicsContext.Shading>): GraphicsContext.Shading`
  - `GraphicsContext.Shading.backdrop`
- `GraphicsContext.ShadowOptions` (class)
  - `GraphicsContext.ShadowOptions(rawValue: Int)`
  - `GraphicsContext.ShadowOptions.shadowOnly`
  - `GraphicsContext.ShadowOptions.shadowAbove`
  - `GraphicsContext.ShadowOptions.invertsAlpha`
  - `GraphicsContext.ShadowOptions.disablesGroup`
- `GroupedTabViewStyle` (class)
  - `GroupedTabViewStyle()`
- `HorizontalAlignment` (class)
  - `HorizontalAlignment.listRowSeparatorLeading`
  - `HorizontalAlignment.listRowSeparatorTrailing`
  - `HorizontalAlignment.center`
  - `HorizontalAlignment.leading`
  - `HorizontalAlignment.trailing`
- `IdentityTransition` (class)
  - `IdentityTransition()`
- `InsetGroupedListStyle` (class)
  - `InsetGroupedListStyle()`
- `KeyEquivalent` (class)
  - `KeyEquivalent(extendedGraphemeClusterLiteral: String)`
  - `KeyEquivalent.rightArrow`
  - `KeyEquivalent.deleteForward`
  - `KeyEquivalent.end`
  - `KeyEquivalent.tab`
  - `KeyEquivalent.home`
  - `KeyEquivalent.clear`
  - `KeyEquivalent.space`
  - `KeyEquivalent.delete`
  - `KeyEquivalent.escape`
  - `KeyEquivalent.pageUp`
  - `KeyEquivalent.`return``
  - `KeyEquivalent.upArrow`
  - `KeyEquivalent.pageDown`
  - `KeyEquivalent.downArrow`
  - `KeyEquivalent.leftArrow`
  - `KeyEquivalent.character(character: String): KeyEquivalent`
- `LayoutDirectionBehavior` (class)
  - `LayoutDirectionBehavior.fixed`
  - `LayoutDirectionBehavior.mirrors`
  - `LayoutDirectionBehavior.mirrors(inValue: LayoutDirection): LayoutDirectionBehavior`
- `LinearGradient` (class)
  - `LinearGradient(stops: List<Gradient.Stop>, startPoint: UnitPoint, endPoint: UnitPoint)`
  - `LinearGradient(colors: List<Color>, startPoint: UnitPoint, endPoint: UnitPoint)`
  - `LinearGradient(gradient: Gradient, startPoint: UnitPoint, endPoint: UnitPoint)`
- `ListStyle` (interface)
  - `ListStyle.elliptical`
  - `ListStyle.insetGrouped`
  - `ListStyle.inset(alternatesRowBackgrounds: Boolean): InsetListStyle`
  - `ListStyle.inset`
  - `ListStyle.plain`
  - `ListStyle.automatic`
  - `ListStyle.grouped`
  - `ListStyle.sidebar`
  - `ListStyle.bordered(alternatesRowBackgrounds: Boolean): BorderedListStyle`
  - `ListStyle.bordered`
  - `ListStyle.carousel`
- `MatchedGeometryProperties` (class)
  - `MatchedGeometryProperties(rawValue: Int)`
  - `MatchedGeometryProperties.size`
  - `MatchedGeometryProperties.frame`
  - `MatchedGeometryProperties.position`
- `MenuControlGroupStyle` (class)
  - `MenuControlGroupStyle()`
- `MoveTransition` (class)
  - `MoveTransition(edge: Edge)`
- `NavigationControlGroupStyle` (class)
  - `NavigationControlGroupStyle()`
- `NavigationTransition` (interface)
  - `NavigationTransition.automatic`
  - `NavigationTransition.crossFade`
- `OpenWindowAction` (object)
- `OpenWindowAction.SharingBehavior` (class)
  - `OpenWindowAction.SharingBehavior.required`
  - `OpenWindowAction.SharingBehavior.requested`
- `PaletteControlGroupStyle` (class)
  - `PaletteControlGroupStyle()`
- `PeriodicTimelineSchedule` (class)
  - `PeriodicTimelineSchedule(startDate: Double, interval: Double)`
- `PlainTextEditorStyle` (class)
  - `PlainTextEditorStyle()`
- `PresentationAdaptation` (class)
  - `PresentationAdaptation.fullScreenCover`
  - `PresentationAdaptation.none`
  - `PresentationAdaptation.sheet`
  - `PresentationAdaptation.popover`
  - `PresentationAdaptation.automatic`
- `PreviewDevice` (class)
  - `PreviewDevice(stringLiteral: String)`
  - `PreviewDevice.rawValue(rawValue: String): PreviewDevice`
- `ProjectionTransform` (class)
  - `ProjectionTransform.concatenating(rhs: ProjectionTransform): ProjectionTransform`
  - `ProjectionTransform.inverted(): ProjectionTransform`
  - `ProjectionTransform()`
  - `ProjectionTransform(m: CGAffineTransform)`
- `PushTransition` (class)
  - `PushTransition(edge: Edge)`
- `RedactionReasons` (class)
  - `RedactionReasons(rawValue: Int)`
  - `RedactionReasons.invalidated`
  - `RedactionReasons.placeholder`
  - `RedactionReasons.privacy`
- `RoundedRectangle` (class)
  - `RoundedRectangle(cornerSize: CGSize, style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
  - `RoundedRectangle(cornerRadius: Double, style: RoundedCornerStyle = RoundedCornerStyle.continuous)`
- `ScenePadding` (class)
  - `ScenePadding.navigationBar`
  - `ScenePadding.minimum`
- `ScrollDismissesKeyboardMode` (class)
  - `ScrollDismissesKeyboardMode.immediately`
  - `ScrollDismissesKeyboardMode.interactively`
  - `ScrollDismissesKeyboardMode.never`
  - `ScrollDismissesKeyboardMode.automatic`
- `ScrollPhase` (class)
  - `ScrollPhase.interacting`
  - `ScrollPhase.decelerating`
  - `ScrollPhase.idle`
  - `ScrollPhase.tracking`
  - `ScrollPhase.animating`
- `SearchFieldPlacement` (class)
  - `SearchFieldPlacement.toolbarPrincipal`
  - `SearchFieldPlacement.navigationBarDrawer(displayMode: SearchFieldPlacement.NavigationBarDrawerDisplayMode): SearchFieldPlacement`
  - `SearchFieldPlacement.navigationBarDrawer`
  - `SearchFieldPlacement.sidebar`
  - `SearchFieldPlacement.toolbar`
  - `SearchFieldPlacement.automatic`
- `SearchFieldPlacement.NavigationBarDrawerDisplayMode` (class)
  - `SearchFieldPlacement.NavigationBarDrawerDisplayMode.always`
  - `SearchFieldPlacement.NavigationBarDrawerDisplayMode.automatic`
- `SelectionShapeStyle` (class)
  - `SelectionShapeStyle()`
- `ShadowStyle` (class)
  - `ShadowStyle.drop(color: Color?`
  - `ShadowStyle.inner(color: Color?`
- `SidebarListStyle` (class)
  - `SidebarListStyle()`
- `SpacerSizing` (class)
  - `SpacerSizing.fixed`
  - `SpacerSizing.flexible`
- `SpringLoadingBehavior` (class)
  - `SpringLoadingBehavior.enabled`
  - `SpringLoadingBehavior.disabled`
  - `SpringLoadingBehavior.automatic`
- `SubmitTriggers` (class)
  - `SubmitTriggers(rawValue: Int)`
  - `SubmitTriggers.text`
  - `SubmitTriggers.search`
- `SymbolRenderingMode` (class)
  - `SymbolRenderingMode.monochrome`
  - `SymbolRenderingMode.multicolor`
  - `SymbolRenderingMode.hierarchical`
  - `SymbolRenderingMode.palette`
- `TabBarPlacement` (class)
  - `TabBarPlacement.topBar`
  - `TabBarPlacement.bottomBar`
  - `TabBarPlacement.pageIndicator`
  - `TabBarPlacement.sidebar`
  - `TabBarPlacement.ornament`
- `TabViewBottomAccessoryPlacement` (class)
  - `TabViewBottomAccessoryPlacement.inline`
  - `TabViewBottomAccessoryPlacement.expanded`
- `TabsPickerStyle` (class)
  - `TabsPickerStyle()`
- `TextFieldStyle` (interface)
  - `TextFieldStyle.squareBorder`
  - `TextFieldStyle.roundedBorder`
  - `TextFieldStyle.plain`
  - `TextFieldStyle.automatic`
  - `TextFieldStyle.bordered`
- `TextInputFormattingControlPlacement` (object)
- `TextInputFormattingControlPlacement.Set` (class)
  - `TextInputFormattingControlPlacement.Set(rawValue: Int)`
  - `TextInputFormattingControlPlacement.Set.contextMenu`
  - `TextInputFormattingControlPlacement.Set.accessoryBar`
  - `TextInputFormattingControlPlacement.Set.inputAssistant`
  - `TextInputFormattingControlPlacement.Set.all`
  - `TextInputFormattingControlPlacement.Set.default`
  - `TextInputFormattingControlPlacement.Set.fontPanel`
- `TintShapeStyle` (class)
  - `TintShapeStyle()`
- `ToolbarCustomizationBehavior` (class)
  - `ToolbarCustomizationBehavior.reorderable`
  - `ToolbarCustomizationBehavior.default`
  - `ToolbarCustomizationBehavior.disabled`
- `ToolbarLabelStyle` (class)
  - `ToolbarLabelStyle.titleAndIcon`
  - `ToolbarLabelStyle.iconOnly`
  - `ToolbarLabelStyle.automatic`
  - `ToolbarLabelStyle.titleOnly`
- `ToolbarSpacer` (class)
  - `ToolbarSpacer(sizing: SpacerSizing = SpacerSizing.flexible, placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic)`
- `Transaction` (class)
  - `Transaction(animation: Animation?)`
  - `Transaction()`
- `UnifiedCompactWindowToolbarStyle` (class)
  - `UnifiedCompactWindowToolbarStyle(showsTitle: Boolean)`
  - `UnifiedCompactWindowToolbarStyle()`
- `VerticalAlignment` (class)
  - `VerticalAlignment.lastTextBaseline`
  - `VerticalAlignment.firstTextBaseline`
  - `VerticalAlignment.top`
  - `VerticalAlignment.bottom`
  - `VerticalAlignment.center`
- `Visibility` (class)
  - `Visibility.hidden`
  - `Visibility.visible`
  - `Visibility.automatic`
- `WindowIdealSize` (class)
  - `WindowIdealSize.fitToContent`
  - `WindowIdealSize.maximum`
  - `WindowIdealSize.automatic`
- `WindowResizability` (class)
  - `WindowResizability.contentSize`
  - `WindowResizability.contentMinSize`
  - `WindowResizability.automatic`
- `WritingToolsBehavior` (class)
  - `WritingToolsBehavior.limited`
  - `WritingToolsBehavior.complete`
  - `WritingToolsBehavior.disabled`
  - `WritingToolsBehavior.automatic`
- `AccessibilityDirectTouchOptions` (class)
  - `AccessibilityDirectTouchOptions(rawValue: Int)`
  - `AccessibilityDirectTouchOptions.silentOnTouch`
  - `AccessibilityDirectTouchOptions.requiresActivation`
- `AccessibilitySystemRotor` (class)
  - `AccessibilitySystemRotor.italicText`
  - `AccessibilitySystemRotor.textFields`
  - `AccessibilitySystemRotor.underlineText`
  - `AccessibilitySystemRotor.misspelledWords`
  - `AccessibilitySystemRotor.links(visited: Boolean): AccessibilitySystemRotor`
  - `AccessibilitySystemRotor.links`
  - `AccessibilitySystemRotor.lists`
  - `AccessibilitySystemRotor.images`
  - `AccessibilitySystemRotor.tables`
  - `AccessibilitySystemRotor.boldText`
  - `AccessibilitySystemRotor.headings(level: AccessibilityHeadingLevel): AccessibilitySystemRotor`
  - `AccessibilitySystemRotor.headings`
  - `AccessibilitySystemRotor.landmarks`
- `AccessoryBarButtonStyle` (class)
  - `AccessoryBarButtonStyle()`
- `AdaptableTabBarPlacement` (class)
  - `AdaptableTabBarPlacement.tabBar`
  - `AdaptableTabBarPlacement.sidebar`
  - `AdaptableTabBarPlacement.automatic`
- `Animation` (class)
  - `Animation.repeatCount(repeatCount: Int, autoreverses: Boolean`
  - `Animation.repeatForever(autoreverses: Boolean`
  - `Animation.logicallyComplete(after: Double): Animation`
  - `Animation.delay(delay: Double): Animation`
  - `Animation.speed(speed: Double): Animation`
  - `Animation.timingCurve(curve: UnitCurve, duration: Double): Animation`
  - `Animation.timingCurve(p1x: Double, p1y: Double, p2x: Double, p2y: Double, duration: Double`
  - `Animation.interactiveSpring(duration: Double`
  - `Animation.interactiveSpringResponse(response: Double`
  - `Animation.interactiveSpring`
  - `Animation.interpolatingSpring(mass: Double`
  - `Animation.interpolatingSpring(duration: Double`
  - `Animation.interpolatingSpring`
  - `Animation.interpolatingSpring(spring: Spring, initialVelocity: Double`
  - `Animation.bouncy(duration: Double`
  - `Animation.bouncy`
  - `Animation.easeIn(duration: Double): Animation`
  - `Animation.easeIn`
  - `Animation.linear(duration: Double): Animation`
  - `Animation.linear`
  - `Animation.smooth(duration: Double`
  - `Animation.smooth`
  - `Animation.snappy(duration: Double`
  - `Animation.snappy`
  - `Animation.spring(duration: Double`
  - `Animation.springResponse(response: Double`
  - `Animation.spring`
  - `Animation.spring(spring: Spring, blendDuration: Double`
  - `Animation.default`
  - `Animation.easeOut(duration: Double): Animation`
  - `Animation.easeOut`
  - `Animation.easeInOut(duration: Double): Animation`
  - `Animation.easeInOut`
- `AnyShape` (class)
  - `AnyShape(shape: Shape)`
- `AttributedTextSelection` (class)
  - `AttributedTextSelection()`
- `AutomaticImmersionStyle` (class)
  - `AutomaticImmersionStyle()`
- `AutomaticTableStyle` (class)
- `BalancedNavigationSplitViewStyle` (class)
  - `BalancedNavigationSplitViewStyle()`
- `BorderedProminentButtonStyle` (class)
  - `BorderedProminentButtonStyle()`
- `BorderlessPullDownMenuButtonStyle` (class)
  - `BorderlessPullDownMenuButtonStyle()`
- `ButtonToggleStyle` (class)
  - `ButtonToggleStyle()`
- `Chirality` (class)
  - `Chirality.left`
  - `Chirality.right`
- `ColorRenderingMode` (class)
  - `ColorRenderingMode.extendedLinear`
  - `ColorRenderingMode.linear`
  - `ColorRenderingMode.nonLinear`
- `CompactDatePickerStyle` (class)
  - `CompactDatePickerStyle()`
- `ContentMarginPlacement` (class)
  - `ContentMarginPlacement.scrollContent`
  - `ContentMarginPlacement.scrollIndicators`
  - `ContentMarginPlacement.automatic`
- `ControlActiveState` (class)
  - `ControlActiveState.key`
  - `ControlActiveState.active`
  - `ControlActiveState.inactive`
- `CoordinateSpaceProtocol` (interface)
  - `CoordinateSpaceProtocol.local`
  - `CoordinateSpaceProtocol.scrollView(axis: Axis): NamedCoordinateSpace`
  - `CoordinateSpaceProtocol.scrollView`
  - `CoordinateSpaceProtocol.named(name: Any): NamedCoordinateSpace`
  - `CoordinateSpaceProtocol.global`
- `DefaultDatePickerStyle` (class)
  - `DefaultDatePickerStyle()`
- `DefaultListStyle` (class)
  - `DefaultListStyle()`
- `DefaultTabViewStyle` (class)
  - `DefaultTabViewStyle()`
- `DeviceHinge` (object)
- `DeviceHinge.Status` (class)
  - `DeviceHinge.Status.partiallyOpen`
  - `DeviceHinge.Status.closed`
  - `DeviceHinge.Status.fullyOpen`
- `DocumentCreationSource` (class)
  - `DocumentCreationSource(id: String)`
- `DropConfiguration` (class)
  - `DropConfiguration(operation: DropOperation)`
- `EdgeInsets` (class)
  - `EdgeInsets(top: Double, leading: Double, bottom: Double, trailing: Double)`
  - `EdgeInsets.inset(corners: RectangleCornerInsets, edges: Edge.Set`
  - `EdgeInsets()`
- `EmptyCommands` (class)
  - `EmptyCommands()`
- `EnabledTextSelectability` (class)
- `FileDialogBrowserOptions` (class)
  - `FileDialogBrowserOptions(rawValue: Int)`
  - `FileDialogBrowserOptions.displayFileExtensions`
  - `FileDialogBrowserOptions.enumeratePackages`
  - `FileDialogBrowserOptions.includeHiddenFiles`
- `Font` (class)
  - `Font.monospaced(): Font`
  - `Font.monospaced(isActive: Boolean): Font`
  - `Font.monospacedDigit(): Font`
  - `Font.lowercaseSmallCaps(): Font`
  - `Font.lowercaseSmallCaps(isActive: Boolean): Font`
  - `Font.uppercaseSmallCaps(): Font`
  - `Font.uppercaseSmallCaps(isActive: Boolean): Font`
  - `Font.bold(): Font`
  - `Font.bold(isActive: Boolean): Font`
  - `Font.width(width: Font.Width): Font`
  - `Font.italic(): Font`
  - `Font.italic(isActive: Boolean): Font`
  - `Font.scaled(factor: Double): Font`
  - `Font.weight(weight: Font.Weight): Font`
  - `Font.leading(leading: Font.Leading): Font`
  - `Font.pointSize(size: Double): Font`
  - `Font.smallCaps(): Font`
  - `Font.smallCaps(isActive: Boolean): Font`
  - `Font.largeTitle`
  - `Font.subheadline`
  - `Font.body`
  - `Font.title`
  - `Font.custom(name: String, size: Double, relativeTo: Font.TextStyle): Font`
  - `Font.custom(name: String, size: Double): Font`
  - `Font.customName(name: String, fixedSize: Double): Font`
  - `Font.system(size: Double, weight: Font.Weight`
  - `Font.systemSize(size: Double, weight: Font.Weight?`
  - `Font.system(style: Font.TextStyle, design: Font.Design?`
  - `Font.system(style: Font.TextStyle, design: Font.Design`
  - `Font.title2`
  - `Font.title3`
  - `Font.callout`
  - `Font.caption`
  - `Font.default`
  - `Font.caption2`
  - `Font.footnote`
  - `Font.headline`
- `Font.Design` (class)
  - `Font.Design.monospaced`
  - `Font.Design.serif`
  - `Font.Design.default`
  - `Font.Design.rounded`
- `Font.Leading` (class)
  - `Font.Leading.loose`
  - `Font.Leading.tight`
  - `Font.Leading.standard`
- `Font.TextStyle` (class)
  - `Font.TextStyle.largeTitle`
  - `Font.TextStyle.subheadline`
  - `Font.TextStyle.body`
  - `Font.TextStyle.title`
  - `Font.TextStyle.title2`
  - `Font.TextStyle.title3`
  - `Font.TextStyle.callout`
  - `Font.TextStyle.caption`
  - `Font.TextStyle.caption2`
  - `Font.TextStyle.footnote`
  - `Font.TextStyle.headline`
- `Font.Weight` (class)
  - `Font.Weight.ultraLight`
  - `Font.Weight.bold`
  - `Font.Weight.thin`
  - `Font.Weight.black`
  - `Font.Weight.heavy`
  - `Font.Weight.light`
  - `Font.Weight.medium`
  - `Font.Weight.regular`
  - `Font.Weight.semibold`
- `Font.Width` (class)
  - `Font.Width(value: Double)`
  - `Font.Width.compressed`
  - `Font.Width.expanded`
  - `Font.Width.standard`
  - `Font.Width.condensed`
- `FullImmersionStyle` (class)
  - `FullImmersionStyle()`
- `GlassEffectTransition` (class)
  - `GlassEffectTransition.materialize`
  - `GlassEffectTransition.matchedGeometry`
  - `GlassEffectTransition.identity`
- `GridItem` (class)
  - `GridItem(size: GridItem.Size = GridItem.Size.flexible(), spacing: Double? = null, alignment: Alignment? = null)`
- `GridItem.Size` (class)
  - `GridItem.Size.fixed(p0: Double): GridItem.Size`
  - `GridItem.Size.adaptive(minimum: Double, maximum: Double?`
  - `GridItem.Size.flexible(minimum: Double`
- `HStackLayout` (class)
  - `HStackLayout(alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null)`
- `HorizontalDirection` (class)
  - `HorizontalDirection.leading`
  - `HorizontalDirection.trailing`
- `HorizontalDirection.Set` (class)
  - `HorizontalDirection.Set(direction: HorizontalDirection)`
  - `HorizontalDirection.Set(rawValue: Int)`
  - `HorizontalDirection.Set.all`
  - `HorizontalDirection.Set.leading`
  - `HorizontalDirection.Set.trailing`
- `Image` (object)
- `Image.DynamicRange` (class)
  - `Image.DynamicRange.constrainedHigh`
  - `Image.DynamicRange.high`
  - `Image.DynamicRange.standard`
- `Image.Interpolation` (class)
  - `Image.Interpolation.low`
  - `Image.Interpolation.high`
  - `Image.Interpolation.none`
  - `Image.Interpolation.medium`
- `Image.Orientation` (class)
  - `Image.Orientation.upMirrored`
  - `Image.Orientation.downMirrored`
  - `Image.Orientation.leftMirrored`
  - `Image.Orientation.rightMirrored`
  - `Image.Orientation.up`
  - `Image.Orientation.down`
  - `Image.Orientation.left`
  - `Image.Orientation.right`
- `Image.ResizingMode` (class)
  - `Image.ResizingMode.tile`
  - `Image.ResizingMode.stretch`
- `Image.Scale` (class)
  - `Image.Scale.large`
  - `Image.Scale.small`
  - `Image.Scale.medium`
- `Image.TemplateRenderingMode` (class)
  - `Image.TemplateRenderingMode.original`
  - `Image.TemplateRenderingMode.template`
- `InsetListStyle` (class)
  - `InsetListStyle(alternatesRowBackgrounds: Boolean)`
  - `InsetListStyle()`
- `KeyPress` (object)
- `KeyPress.Phases` (class)
  - `KeyPress.Phases(rawValue: Int)`
  - `KeyPress.Phases.up`
  - `KeyPress.Phases.all`
  - `KeyPress.Phases.down`
  - `KeyPress.Phases.repeat`
- `KeyPress.Result` (class)
  - `KeyPress.Result.handled`
  - `KeyPress.Result.ignored`
- `LayoutProperties` (class)
  - `LayoutProperties()`
- `LinearProgressViewStyle` (class)
  - `LinearProgressViewStyle(tint: Color)`
  - `LinearProgressViewStyle()`
- `LocalCoordinateSpace` (class)
  - `LocalCoordinateSpace()`
- `Material` (class)
  - `Material.materialActiveAppearance(appearance: MaterialActiveAppearance): Material`
  - `Material.ultraThick`
  - `Material.bar`
  - `Material.thin`
  - `Material.thick`
  - `Material.regular`
  - `Material.ultraThin`
- `MenuOrder` (class)
  - `MenuOrder.fixed`
  - `MenuOrder.priority`
  - `MenuOrder.automatic`
- `NSHostingSceneBridgingOptions` (class)
  - `NSHostingSceneBridgingOptions(rawValue: Int)`
  - `NSHostingSceneBridgingOptions.all`
  - `NSHostingSceneBridgingOptions.title`
  - `NSHostingSceneBridgingOptions.toolbars`
- `NavigationLinkPickerStyle` (class)
  - `NavigationLinkPickerStyle()`
- `NavigationViewStyle` (interface)
  - `NavigationViewStyle.stack`
  - `NavigationViewStyle.columns`
  - `NavigationViewStyle.automatic`
- `OverlayArrangementViewStyle` (class)
  - `OverlayArrangementViewStyle.axes(axes: Axis.Set): OverlayArrangementViewStyle`
  - `OverlayArrangementViewStyle()`
- `PalettePickerStyle` (class)
  - `PalettePickerStyle()`
- `PickerStyle` (interface)
  - `PickerStyle.radioGroup`
  - `PickerStyle.navigationLink`
  - `PickerStyle.menu`
  - `PickerStyle.tabs`
  - `PickerStyle.wheel`
  - `PickerStyle.inline`
  - `PickerStyle.automatic`
  - `PickerStyle.palette`
  - `PickerStyle.segmented`
- `PlainTextFieldStyle` (class)
  - `PlainTextFieldStyle()`
- `PresentationBackgroundInteraction` (class)
  - `PresentationBackgroundInteraction.enabled(upThrough: PresentationDetent): PresentationBackgroundInteraction`
  - `PresentationBackgroundInteraction.enabled`
  - `PresentationBackgroundInteraction.disabled`
  - `PresentationBackgroundInteraction.automatic`
- `PreviewPlatform` (class)
  - `PreviewPlatform.iOS`
  - `PreviewPlatform.tvOS`
  - `PreviewPlatform.macOS`
  - `PreviewPlatform.watchOS`
- `Prominence` (class)
  - `Prominence.standard`
  - `Prominence.increased`
- `RadialGradient` (class)
  - `RadialGradient(stops: List<Gradient.Stop>, center: UnitPoint, startRadius: Double, endRadius: Double)`
  - `RadialGradient(colors: List<Color>, center: UnitPoint, startRadius: Double, endRadius: Double)`
  - `RadialGradient(gradient: Gradient, center: UnitPoint, startRadius: Double, endRadius: Double)`
- `ReservedRegion` (object)
- `ReservedRegion.Kind` (class)
  - `ReservedRegion.Kind.division`
  - `ReservedRegion.Kind.occlusion`
- `ReservedRegion.QueryOptions` (class)
  - `ReservedRegion.QueryOptions(rawValue: Int)`
  - `ReservedRegion.QueryOptions.includeInactive`
- `RoundedRectangularShape` (interface)
- `ScenePhase` (class)
  - `ScenePhase.background`
  - `ScenePhase.active`
  - `ScenePhase.inactive`
- `ScrollEdgeEffectStyle` (class)
  - `ScrollEdgeEffectStyle.hard`
  - `ScrollEdgeEffectStyle.soft`
  - `ScrollEdgeEffectStyle.automatic`
- `ScrollPosition` (class)
  - `ScrollPosition(x: Double, y: Double)`
  - `ScrollPosition(x: Double)`
  - `ScrollPosition(edge: Edge)`
  - `ScrollPosition(point: CGPoint)`
  - `ScrollPosition()`
  - `ScrollPosition.y(y: Double): ScrollPosition`
- `SearchPresentationToolbarBehavior` (class)
  - `SearchPresentationToolbarBehavior.avoidHidingContent`
  - `SearchPresentationToolbarBehavior.automatic`
- `SensoryFeedback` (class)
  - `SensoryFeedback.levelChange`
  - `SensoryFeedback.pathComplete`
  - `SensoryFeedback.stop`
  - `SensoryFeedback.error`
  - `SensoryFeedback.press(feedback: SensoryFeedback.PressFeedback): SensoryFeedback`
  - `SensoryFeedback.start`
  - `SensoryFeedback.impact(flexibility: SensoryFeedback.Flexibility, intensity: Double`
  - `SensoryFeedback.impact(weight: SensoryFeedback.Weight`
  - `SensoryFeedback.impact`
  - `SensoryFeedback.release(feedback: SensoryFeedback.ReleaseFeedback): SensoryFeedback`
  - `SensoryFeedback.success`
  - `SensoryFeedback.warning`
  - `SensoryFeedback.decrease`
  - `SensoryFeedback.increase`
  - `SensoryFeedback.alignment`
  - `SensoryFeedback.selection`
  - `SensoryFeedback.selection(feedback: SensoryFeedback.SelectionFeedback): SensoryFeedback`
- `SensoryFeedback.Flexibility` (class)
  - `SensoryFeedback.Flexibility.soft`
  - `SensoryFeedback.Flexibility.rigid`
  - `SensoryFeedback.Flexibility.solid`
- `SensoryFeedback.PressFeedback` (class)
  - `SensoryFeedback.PressFeedback.buttonIconOnly`
  - `SensoryFeedback.PressFeedback.tab`
  - `SensoryFeedback.PressFeedback.button`
  - `SensoryFeedback.PressFeedback.slider`
  - `SensoryFeedback.PressFeedback.toggle`
- `SensoryFeedback.ReleaseFeedback` (class)
  - `SensoryFeedback.ReleaseFeedback.slider`
- `SensoryFeedback.SelectionFeedback` (class)
  - `SensoryFeedback.SelectionFeedback.on`
  - `SensoryFeedback.SelectionFeedback.off`
  - `SensoryFeedback.SelectionFeedback.maximum`
  - `SensoryFeedback.SelectionFeedback.minimum`
- `SensoryFeedback.Weight` (class)
  - `SensoryFeedback.Weight.heavy`
  - `SensoryFeedback.Weight.light`
  - `SensoryFeedback.Weight.medium`
- `Shape` (interface)
  - `Shape.buttonBorder`
  - `Shape.textInputBorder`
  - `Shape.containerRelative`
  - `Shape.rect(cornerSize: CGSize, style: RoundedCornerStyle`
  - `Shape.rect(cornerRadius: Double, style: RoundedCornerStyle`
  - `Shape.rect(topLeadingCorner: Edge.Corner.Style, topTrailingCorner: Edge.Corner.Style, bottomLeadingCorner: Edge.Corner.Style, bottomTrailingCorner: Edge.Corner.Style): ConcentricRectangle`
  - `Shape.rect(uniformTopCorners: Edge.Corner.Style, uniformBottomCorners: Edge.Corner.Style): ConcentricRectangle`
  - `Shape.rect(uniformTopCorners: Edge.Corner.Style, bottomLeadingCorner: Edge.Corner.Style, bottomTrailingCorner: Edge.Corner.Style): ConcentricRectangle`
  - `Shape.rectUniformBottomCorners(uniformBottomCorners: Edge.Corner.Style, topLeadingCorner: Edge.Corner.Style, topTrailingCorner: Edge.Corner.Style): ConcentricRectangle`
  - `Shape.rectUniformLeadingCorners(uniformLeadingCorners: Edge.Corner.Style, uniformTrailingCorners: Edge.Corner.Style): ConcentricRectangle`
  - `Shape.rectUniformLeadingCorners(uniformLeadingCorners: Edge.Corner.Style, topTrailingCorner: Edge.Corner.Style, bottomTrailingCorner: Edge.Corner.Style): ConcentricRectangle`
  - `Shape.rectUniformTrailingCorners(uniformTrailingCorners: Edge.Corner.Style, topLeadingCorner: Edge.Corner.Style, bottomLeadingCorner: Edge.Corner.Style): ConcentricRectangle`
  - `Shape.rect(corners: Edge.Corner.Style, isUniform: Boolean`
  - `Shape.rect(cornerRadii: RectangleCornerRadii, style: RoundedCornerStyle`
  - `Shape.rect(topLeadingRadius: Double`
  - `Shape.circle`
  - `Shape.capsule(style: RoundedCornerStyle): Capsule`
  - `Shape.capsule`
  - `Shape.ellipse`
  - `Shape.rect`
- `SidebarRowSize` (class)
  - `SidebarRowSize.large`
  - `SidebarRowSize.small`
  - `SidebarRowSize.medium`
- `SpatialEventCollection` (object)
- `SpatialEventCollection.Event` (object)
- `SpatialEventCollection.Event.Kind` (class)
  - `SpatialEventCollection.Event.Kind.indirectPinch`
  - `SpatialEventCollection.Event.Kind.touch`
  - `SpatialEventCollection.Event.Kind.pencil`
  - `SpatialEventCollection.Event.Kind.pointer`
- `SpatialEventCollection.Event.Phase` (class)
  - `SpatialEventCollection.Event.Phase.ended`
  - `SpatialEventCollection.Event.Phase.active`
  - `SpatialEventCollection.Event.Phase.cancelled`
- `SquareBorderTextFieldStyle` (class)
  - `SquareBorderTextFieldStyle()`
- `Subview` (object)
- `Subview.ContainerSizingOptions` (class)
  - `Subview.ContainerSizingOptions.uniform(axis: Axis.Set): Subview.ContainerSizingOptions`
  - `Subview.ContainerSizingOptions.variable`
- `SymbolVariableValueMode` (class)
  - `SymbolVariableValueMode.draw`
  - `SymbolVariableValueMode.color`
- `TabCustomizationBehavior` (class)
  - `TabCustomizationBehavior.reorderable`
  - `TabCustomizationBehavior.disabled`
  - `TabCustomizationBehavior.automatic`
- `TabViewCustomization` (class)
  - `TabViewCustomization()`
- `TapGesture` (class)
  - `TapGesture(count: Int = 1, inputKinds: GestureInputKinds = GestureInputKinds.all)`
  - `TapGesture(count: Int = 1)`
- `TextFormattingCommands` (class)
  - `TextFormattingCommands()`
- `TextSelectability` (interface)
  - `TextSelectability.enabled`
  - `TextSelectability.disabled`
- `TitleAndIconLabelStyle` (class)
  - `TitleAndIconLabelStyle()`
- `ToolbarCustomizationOptions` (class)
  - `ToolbarCustomizationOptions(rawValue: Int)`
  - `ToolbarCustomizationOptions.alwaysAvailable`
- `ToolbarMinimizationBehavior` (class)
  - `ToolbarMinimizationBehavior.onScrollUp`
  - `ToolbarMinimizationBehavior.onScrollDown`
  - `ToolbarMinimizationBehavior.never`
  - `ToolbarMinimizationBehavior.automatic`
- `ToolbarTitleAlignment` (class)
  - `ToolbarTitleAlignment.center`
  - `ToolbarTitleAlignment.leading`
  - `ToolbarTitleAlignment.automatic`
- `TransitionPhase` (class)
  - `TransitionPhase.willAppear`
  - `TransitionPhase.didDisappear`
  - `TransitionPhase.identity`
- `UnifiedWindowToolbarStyle` (class)
  - `UnifiedWindowToolbarStyle(showsTitle: Boolean)`
  - `UnifiedWindowToolbarStyle()`
- `VerticalDirection` (class)
  - `VerticalDirection.up`
  - `VerticalDirection.down`
- `VerticalDirection.Set` (class)
  - `VerticalDirection.Set(direction: VerticalDirection)`
  - `VerticalDirection.Set(rawValue: Int)`
  - `VerticalDirection.Set.up`
  - `VerticalDirection.Set.all`
  - `VerticalDirection.Set.down`
- `VisualEffect` (interface)
- `WindowInteractionBehavior` (class)
  - `WindowInteractionBehavior.enabled`
  - `WindowInteractionBehavior.disabled`
  - `WindowInteractionBehavior.automatic`
- `WindowStyle` (interface)
  - `WindowStyle.hiddenTitleBar`
  - `WindowStyle.plain`
  - `WindowStyle.automatic`
  - `WindowStyle.titleBar`
- `ZStackLayout` (class)
  - `ZStackLayout(alignment: Alignment = Alignment.center)`
