package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import bench.Rgb
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 27 — Order tracking: summary header, vertical timeline (circles + connecting rectangles), courier card.

data class Screen27Step(
    val id: Int,
    val title: String,
    val detail: String,
    val time: String,
    val icon: String,
    val circleTint: Rgb,
    val iconTint: Rgb,
    val lineTint: Rgb,
    val isLast: Boolean,
    val isCurrent: Boolean,
)

data class Screen27State(
    val order: String,
    val eta: String,
    val status: String,
    val statusTint: Rgb,
    val progress: Double,
    val progressLabel: String,
    val steps: List<Screen27Step>,
    val courier: String,
    val vehicle: String,
    val rating: String,
)

private val screen27Steps = listOf(
    Triple("Order placed", "We received your order", "bag.fill"),
    Triple("Payment confirmed", "Visa ending in 4242", "creditcard.fill"),
    Triple("Packed", "Warehouse Lyon-Est", "shippingbox.fill"),
    Triple("Shipped", "Handed to carrier", "truck.box.fill"),
    Triple("Out for delivery", "Courier is on the way", "bicycle"),
    Triple("Delivered", "Leave at the front door", "house.fill"),
)
private val screen27Times = listOf("Mon 09:12", "Mon 09:13", "Tue 16:40", "Wed 07:05", "Thu 08:30", "Thu 14:00")

private fun screen27State(reached: Int): Screen27State {
    val green = Rgb(0.2, 0.7, 0.35)
    val blue = Rgb(0.0, 0.48, 1.0)
    val idle = Rgb(0.88, 0.88, 0.9)
    val white = Rgb(1.0, 1.0, 1.0)
    val gray = Rgb(0.6, 0.6, 0.63)
    val delivered = reached == screen27Steps.size - 1
    return Screen27State(
        order = "Order #A-20481",
        eta = if (delivered) "Delivered Thu at 14:00" else "Arriving Thu, 14:00 – 16:00",
        status = if (delivered) "Delivered" else screen27Steps[reached].first,
        statusTint = if (delivered) green else blue,
        progress = reached / (screen27Steps.size - 1.0),
        progressLabel = "${reached + 1} of ${screen27Steps.size} steps",
        steps = screen27Steps.mapIndexed { i, (title, detail, icon) ->
            Screen27Step(
                id = i,
                title = title,
                detail = detail,
                time = if (i <= reached) screen27Times[i] else "Pending",
                icon = if (i < reached || (delivered && i == reached)) "checkmark" else icon,
                circleTint = if (i < reached || delivered) green else if (i == reached) blue else idle,
                iconTint = if (i <= reached) white else gray,
                lineTint = if (i < reached) green else idle,
                isLast = i == screen27Steps.size - 1,
                isCurrent = i == reached,
            )
        },
        courier = "Julien Moreau",
        vehicle = "Cargo bike · AB-123-CD",
        rating = "4.9",
    )
}

class Screen27ViewModel : BenchViewModel() {
    private var reached = 3
    private val _state = MutableStateFlow(screen27State(reached))
    val state: StateFlow<Screen27State> = _state

    override fun mutate() {
        reached = (reached + 1) % screen27Steps.size
        _state.update { screen27State(reached) }
    }
}

@Composable
private fun Screen27Timeline(steps: List<Screen27Step>) {
    VStack(
        modifier = Modifier.padding(16.0).background(Color.gray.opacity(0.08), RoundedRectangle(cornerRadius = 16.0)),
        alignment = HorizontalAlignment.leading,
        spacing = 0.0,
    ) {
        for (step in steps) key(step.id) {
            HStack(alignment = VerticalAlignment.top, spacing = 14.0) {
                VStack(spacing = 0.0) {
                    ZStack {
                        ShapeView(Circle(), modifier = Modifier.frame(width = 26.0, height = 26.0), fill = step.circleTint.color())
                        if (step.isCurrent) {
                            ShapeView(Circle(), modifier = Modifier.frame(width = 34.0, height = 34.0), stroke = step.circleTint.color().opacity(0.3), lineWidth = 4.0)
                        }
                        Image(systemName = SFSymbol(step.icon), modifier = Modifier.font(Font.system(size = 11.0, weight = Font.Weight.bold)).foregroundStyle(step.iconTint.color()))
                    }
                    if (!step.isLast) {
                        ShapeView(Rectangle(), modifier = Modifier.frame(width = 3.0, height = 34.0), fill = step.lineTint.color())
                    }
                }
                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                    Text(step.title, modifier = Modifier.font(Font.subheadline.weight(if (step.isCurrent) Font.Weight.bold else Font.Weight.medium)))
                    Text(step.detail, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                }
                Spacer()
                Text(step.time, modifier = Modifier.font(Font.caption2.monospacedDigit()).foregroundStyle(ShapeStyle.secondary))
            }
        }
    }
}

@Composable
fun Screen27(vm: Screen27ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Tracking")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 18.0) {
                HStack(spacing = 14.0) {
                    Image(systemName = 
                        SFSymbol.shippingboxFill,
                        modifier = Modifier.font(Font.title2)
                            .foregroundStyle(Color.white)
                            .frame(width = 52.0, height = 52.0)
                            .background(
                                LinearGradient(colors = listOf(Color.orange, Color.brown), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                                RoundedRectangle(cornerRadius = 14.0),
                            ),
                    )
                    VStack(alignment = HorizontalAlignment.leading, spacing = 3.0) {
                        Text(state.order, modifier = Modifier.font(Font.headline))
                        Text(state.eta, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    }
                    Spacer()
                }
                VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) {
                    HStack {
                        Text(
                            state.status,
                            modifier = Modifier.font(Font.caption.bold())
                                .foregroundStyle(state.statusTint.color())
                                .padding(Edge.Set.horizontal, 10.0)
                                .padding(Edge.Set.vertical, 4.0)
                                .background(state.statusTint.color().opacity(0.12), Capsule()),
                        )
                        Spacer()
                        Text(state.progressLabel, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                    }
                    ProgressView(value = state.progress, modifier = Modifier.tint(state.statusTint.color()))
                }
                Screen27Timeline(state.steps)
                HStack(spacing = 12.0) {
                    Image(systemName = 
                        SFSymbol.personCropCircleFill,
                        modifier = Modifier.font(Font.system(size = 44.0)).symbolRenderingMode(SymbolRenderingMode.hierarchical).foregroundStyle(Color.indigo),
                    )
                    VStack(alignment = HorizontalAlignment.leading, spacing = 3.0) {
                        Text(state.courier, modifier = Modifier.font(Font.headline))
                        Text(state.vehicle, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        HStack(spacing = 3.0) {
                            Image(systemName = SFSymbol.starFill, modifier = Modifier.foregroundStyle(Color.yellow))
                            Text(state.rating, modifier = Modifier.monospacedDigit())
                        }
                    }
                    Spacer()
                    Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered).buttonBorderShape(ButtonBorderShape.circle)) {
                        Image(systemName = SFSymbol.messageFill)
                    }
                    Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).buttonBorderShape(ButtonBorderShape.circle).tint(Color.green)) {
                        Image(systemName = SFSymbol.phoneFill)
                    }
                }
            }
        }
    }
}

val screen27 = BenchScreen("27 · Order tracking", ::Screen27ViewModel) { Screen27(it as Screen27ViewModel) }
