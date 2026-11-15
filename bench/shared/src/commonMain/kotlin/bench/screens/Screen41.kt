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

// Screen 41 — Crypto wallet: balance header, segmented time-range Picker, bar chart, quick actions, asset rows with change percentages.

data class Screen41Bar(val id: Int, val height: Double, val up: Boolean)

data class Screen41Asset(
    val id: Int,
    val name: String,
    val symbol: String,
    val icon: String,
    val tint: Rgb,
    val holdings: String,
    val value: String,
    val change: String,
    val up: Boolean,
)

data class Screen41Action(val id: Int, val title: String, val icon: String)

data class Screen41State(
    val balance: String,
    val balanceChange: String,
    val balanceUp: Boolean,
    val ranges: List<String>,
    val range: String,
    val bars: List<Screen41Bar>,
    val actions: List<Screen41Action>,
    val assets: List<Screen41Asset>,
)

private val screen41Ranges = listOf("1D", "1W", "1M", "1Y", "All")

private fun screen41Pct(hundredths: Int): String {
    val sign = if (hundredths < 0) "−" else "+"
    val abs = if (hundredths < 0) -hundredths else hundredths
    return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}%"
}

private fun screen41Build(range: String): Screen41State {
    val r = screen41Ranges.indexOf(range)
    val changes = listOf(listOf(212, -118, 45, 530), listOf(-340, 268, 912, -77), listOf(1204, 655, -230, 1890), listOf(8820, 4410, 2105, -1250), listOf(31200, 18750, 9020, 4400))[r]
    val total = listOf(184, -92, 731, 4210, 15600)[r]
    val bars = (0 until 24).map { i ->
        val h = 20.0 + ((i * (7 + r * 3) + r * 11) % 23) * 3.0 + i * 1.5
        Screen41Bar(i, h, (i + r) % 4 != 0)
    }
    return Screen41State(
        balance = "$24,861.37",
        balanceChange = screen41Pct(total) + " · " + range,
        balanceUp = total >= 0,
        ranges = screen41Ranges,
        range = range,
        bars = bars,
        actions = listOf(
            Screen41Action(1, "Send", "arrow.up"),
            Screen41Action(2, "Receive", "arrow.down"),
            Screen41Action(3, "Swap", "arrow.left.arrow.right"),
            Screen41Action(4, "Buy", "plus"),
        ),
        assets = listOf(
            Screen41Asset(1, "Bitcoin", "BTC", "bitcoinsign", Rgb(0.97, 0.58, 0.1), "0.2481 BTC", "$16,204.90", screen41Pct(changes[0]), changes[0] >= 0),
            Screen41Asset(2, "Ethereum", "ETH", "diamond.fill", Rgb(0.4, 0.45, 0.85), "1.9032 ETH", "$6,112.45", screen41Pct(changes[1]), changes[1] >= 0),
            Screen41Asset(3, "Solana", "SOL", "sun.max.fill", Rgb(0.6, 0.3, 0.95), "9.5 SOL", "$1,722.02", screen41Pct(changes[2]), changes[2] >= 0),
            Screen41Asset(4, "USD Coin", "USDC", "dollarsign", Rgb(0.15, 0.45, 0.85), "822.00 USDC", "$822.00", screen41Pct(changes[3] / 100), changes[3] >= 0),
        ),
    )
}

class Screen41ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(screen41Build("1W"))
    val state: StateFlow<Screen41State> = _state

    fun setRange(value: String) = _state.update { screen41Build(value) }

    override fun mutate() {
        val next = screen41Ranges[(screen41Ranges.indexOf(_state.value.range) + 1) % screen41Ranges.size]
        setRange(next)
    }
}

@Composable
fun Screen41(vm: Screen41ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(
            modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Wallet")
                .toolbarItem(placement = ToolbarItemPlacement.primaryAction) { Button(title = "Scan", systemImage = SFSymbol.qrcodeViewfinder, action = {}) },
        ) {
            Section {
                VStack(modifier = Modifier.padding(Edge.Set.vertical, 6.0), alignment = HorizontalAlignment.leading, spacing = 12.0) {
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text("Total balance", modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                        Text(verbatim = state.balance, modifier = Modifier.font(Font.system(size = 34.0, weight = Font.Weight.bold)).fontDesign(Font.Design.rounded).monospacedDigit())
                        Label(
                            modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).foregroundStyle(if (state.balanceUp) Color.green else Color.red),
                            title = { Text(verbatim = state.balanceChange) },
                            icon = { Image(systemName = if (state.balanceUp) SFSymbol.arrowUpRightCircleFill else SFSymbol.arrowDownRightCircleFill) },
                        )
                    }
                    Picker(title = "Range", selection = state.range, modifier = Modifier.pickerStyle(PickerStyle.segmented).labelsHidden(), onSelectionChange = { vm.setRange(it as String) }) {
                        for (r in state.ranges) key(r) {
                            Text(verbatim = r, modifier = Modifier.tag(r))
                        }
                    }
                    HStack(modifier = Modifier.frame(height = 110.0, alignment = Alignment.bottom), alignment = VerticalAlignment.bottom, spacing = 4.0) {
                        for (bar in state.bars) key(bar.id) {
                            ShapeView(
                                RoundedRectangle(cornerRadius = 2.0),
                                modifier = Modifier.frame(height = bar.height),
                                fill = if (bar.up) Color.green.opacity(0.75) else Color.red.opacity(0.6),
                            )
                        }
                    }
                }
            }
            Section {
                HStack {
                    for (action in state.actions) key(action.id) {
                        VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 6.0) {
                            Image(systemName = 
                                SFSymbol(action.icon),
                                modifier = Modifier.font(Font.system(size = 17.0, weight = Font.Weight.semibold))
                                    .foregroundStyle(Color.white)
                                    .frame(width = 44.0, height = 44.0)
                                    .background(Color.accentColor, Circle()),
                            )
                            Text(verbatim = action.title, modifier = Modifier.font(Font.caption))
                        }
                    }
                }
            }
            Section(title = "Assets") {
                for (asset in state.assets) key(asset.id) {
                    HStack(spacing = 12.0) {
                        Image(systemName = 
                            SFSymbol(asset.icon),
                            modifier = Modifier.font(Font.system(size = 18.0, weight = Font.Weight.bold))
                                .foregroundStyle(Color.white)
                                .frame(width = 40.0, height = 40.0)
                                .background(asset.tint.color(), Circle()),
                        )
                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                            HStack(spacing = 6.0) {
                                Text(verbatim = asset.name, modifier = Modifier.font(Font.body).fontWeight(Font.Weight.medium))
                                Text(verbatim = asset.symbol, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary).padding(Edge.Set.horizontal, 4.0).background(Color.gray.opacity(0.15), RoundedRectangle(cornerRadius = 4.0)))
                            }
                            Text(verbatim = asset.holdings, modifier = Modifier.font(Font.caption).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                        }
                        Spacer()
                        VStack(alignment = HorizontalAlignment.trailing, spacing = 3.0) {
                            Text(verbatim = asset.value, modifier = Modifier.font(Font.body).monospacedDigit())
                            Text(
                                verbatim = asset.change,
                                modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).monospacedDigit()
                                    .foregroundStyle(if (asset.up) Color.green else Color.red)
                                    .padding(Edge.Set.horizontal, 6.0).padding(Edge.Set.vertical, 2.0)
                                    .background((if (asset.up) Color.green else Color.red).opacity(0.12), Capsule()),
                            )
                        }
                    }
                }
            }
        }
    }
}

val screen41 = BenchScreen("41 · Crypto wallet", ::Screen41ViewModel) { Screen41(it as Screen41ViewModel) }
