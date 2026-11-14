package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import bench.Rgb
import dev.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 24 — Pricing tiers: billing picker, three plan cards, highlighted plan with stroked overlay, feature checklists.

data class Screen24Feature(val id: Int, val title: String, val included: Boolean)

data class Screen24Plan(
    val id: Int,
    val name: String,
    val icon: String,
    val tint: Rgb,
    val price: String,
    val period: String,
    val tagline: String,
    val features: List<Screen24Feature>,
    val cta: String,
    val highlighted: Boolean,
)

data class Screen24State(val billing: Int, val savings: String, val plans: List<Screen24Plan>, val footnote: String)

private fun screen24Plans(yearly: Boolean, highlighted: Int): List<Screen24Plan> {
    val names = listOf("Starter", "Pro", "Team")
    val features = listOf("Unlimited projects", "Cloud sync", "Priority support", "Advanced analytics", "SSO & audit log")
    val monthly = listOf(0, 12, 29)
    return names.mapIndexed { i, name ->
        val price = if (yearly) monthly[i] * 10 / 12 else monthly[i]
        Screen24Plan(
            id = i,
            name = name,
            icon = listOf("paperplane.fill", "airplane", "building.2.fill")[i],
            tint = listOf(Rgb(0.2, 0.7, 0.35), Rgb(0.0, 0.48, 1.0), Rgb(0.69, 0.32, 0.87))[i],
            price = if (price == 0) "Free" else "$$price",
            period = if (price == 0) "forever" else if (yearly) "/ mo, billed yearly" else "/ month",
            tagline = listOf("For personal side projects.", "For professionals who ship every day.", "For growing teams that need control.")[i],
            features = features.mapIndexed { j, f -> Screen24Feature(j, f, j <= i + 1 || (i == 2)) },
            cta = if (i == highlighted) "Current plan" else if (i == 0) "Get started" else "Upgrade to $name",
            highlighted = i == highlighted,
        )
    }
}

class Screen24ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen24State(billing = 0, savings = "Save 2 months", plans = screen24Plans(false, 1), footnote = "Prices in USD. Cancel anytime.")
    )
    val state: StateFlow<Screen24State> = _state

    fun setBilling(value: Int) = _state.update {
        it.copy(billing = value, plans = screen24Plans(value == 1, it.plans.first { p -> p.highlighted }.id))
    }

    override fun mutate() = _state.update {
        val billing = 1 - it.billing
        val highlighted = (it.plans.first { p -> p.highlighted }.id + 1) % 3
        it.copy(billing = billing, plans = screen24Plans(billing == 1, highlighted))
    }
}

@Composable
private fun Screen24Card(plan: Screen24Plan) {
    VStack(
        modifier = Modifier.padding(18.0)
            .background(if (plan.highlighted) plan.tint.color().opacity(0.06) else Color.gray.opacity(0.06), RoundedRectangle(cornerRadius = 20.0))
            .overlay {
                ShapeView(
                    RoundedRectangle(cornerRadius = 20.0),
                    stroke = if (plan.highlighted) plan.tint.color() else Color.gray.opacity(0.25),
                    lineWidth = if (plan.highlighted) 2.5 else 1.0,
                )
            },
        alignment = HorizontalAlignment.leading,
        spacing = 12.0,
    ) {
        HStack(spacing = 10.0) {
            Image(systemName = 
                plan.icon,
                modifier = Modifier.font(Font.system(size = 16.0, weight = Font.Weight.bold))
                    .foregroundStyle(Color.white)
                    .frame(width = 34.0, height = 34.0)
                    .background(plan.tint.color(), RoundedRectangle(cornerRadius = 9.0)),
            )
            Text(plan.name, modifier = Modifier.font(Font.title3.bold()))
            Spacer()
            if (plan.highlighted) {
                Text(
                    "POPULAR",
                    modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.heavy).tracking(0.8)
                        .foregroundStyle(Color.white)
                        .padding(Edge.Set.horizontal, 8.0)
                        .padding(Edge.Set.vertical, 4.0)
                        .background(plan.tint.color(), Capsule()),
                )
            }
        }
        HStack(alignment = VerticalAlignment.firstTextBaseline, spacing = 4.0) {
            Text(plan.price, modifier = Modifier.font(Font.system(size = 34.0, weight = Font.Weight.bold, design = Font.Design.rounded)))
            Text(plan.period, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
        }
        Text(plan.tagline, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
        Divider()
        VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) {
            for (feature in plan.features) key(feature.id) {
                HStack(spacing = 8.0) {
                    Image(systemName = 
                        if (feature.included) "checkmark.circle.fill" else "xmark.circle",
                        modifier = Modifier.foregroundStyle(if (feature.included) plan.tint.color() else Color.gray.opacity(0.5)),
                    )
                    Text(
                        feature.title,
                        modifier = Modifier.font(Font.subheadline)
                            .foregroundStyle(if (feature.included) Color.primary else Color.secondary)
                            .strikethrough(!feature.included, color = Color.gray),
                    )
                }
            }
        }
        if (plan.highlighted) {
            Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).tint(plan.tint.color()).controlSize(ControlSize.large)) {
                Text(plan.cta, modifier = Modifier.fontWeight(Font.Weight.semibold).frame(maxWidth = Double.POSITIVE_INFINITY))
            }
        } else {
            Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered).tint(plan.tint.color()).controlSize(ControlSize.large)) {
                Text(plan.cta, modifier = Modifier.fontWeight(Font.Weight.semibold).frame(maxWidth = Double.POSITIVE_INFINITY))
            }
        }
    }
}

@Composable
fun Screen24(vm: Screen24ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Plans")) {
            VStack(modifier = Modifier.padding(), spacing = 16.0) {
                VStack(spacing = 6.0) {
                    Image(systemName = 
                        "crown.fill",
                        modifier = Modifier.font(Font.system(size = 34.0)).symbolRenderingMode(SymbolRenderingMode.multicolor),
                    )
                    Text("Choose your plan", modifier = Modifier.font(Font.title2.bold()))
                    Text(
                        "Upgrade or downgrade at any time. All plans include end-to-end encryption.",
                        modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary).multilineTextAlignment(TextAlignment.center),
                    )
                }
                Picker(
                    title = "Billing",
                    selection = state.billing,
                    modifier = Modifier.pickerStyle(PickerStyle.segmented),
                    onSelectionChange = { vm.setBilling(it as Int) },
                ) {
                    Text("Monthly", modifier = Modifier.tag(0))
                    Text("Yearly", modifier = Modifier.tag(1))
                }
                Label(state.savings, systemImage = "gift.fill", modifier = Modifier.font(Font.caption.bold()).foregroundStyle(Color.orange))
                for (plan in state.plans) key(plan.id) {
                    Screen24Card(plan)
                }
                Text(state.footnote, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.tertiary))
            }
        }
    }
}

val screen24 = BenchScreen("24 · Pricing tiers", ::Screen24ViewModel) { Screen24(it as Screen24ViewModel) }
