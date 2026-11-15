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

// Screen 13 — Stocks watchlist: portfolio summary, range chips, rows with bar sparklines and change pills.

data class Screen13Stock(
    val id: Int,
    val symbol: String,
    val name: String,
    val price: String,
    val change: String,
    val isUp: Boolean,
    val bars: List<Double>,
)

data class Screen13State(
    val portfolioValue: String,
    val portfolioChange: String,
    val portfolioUp: Boolean,
    val ranges: List<String>,
    val selectedRange: String,
    val stocks: List<Screen13Stock>,
    val marketNote: String,
    val tick: Int,
)

private val screen13Seeds = listOf(
    Triple("AAPL", "Apple Inc.", 18943),
    Triple("MSFT", "Microsoft Corp.", 41272),
    Triple("NVDA", "NVIDIA Corp.", 12850),
    Triple("TSLA", "Tesla, Inc.", 24117),
    Triple("AMZN", "Amazon.com, Inc.", 18533),
    Triple("GOOG", "Alphabet Inc.", 16721),
    Triple("META", "Meta Platforms", 56390),
)

private fun screen13Money(cents: Int): String {
    val whole = cents / 100
    val frac = cents % 100
    val wholeText = if (whole >= 1000) "${whole / 1000},${(whole % 1000).toString().padStart(3, '0')}" else "$whole"
    return "$" + wholeText + "." + frac.toString().padStart(2, '0')
}

private fun screen13Stocks(tick: Int): List<Screen13Stock> = screen13Seeds.mapIndexed { i, (symbol, name, base) ->
    val delta = ((i * 271 + 120 + tick * 53) % 900) - 400
    val up = delta >= 0
    val abs = if (up) delta else -delta
    val pct = "${if (up) "+" else "-"}${abs / 100}.${(abs % 100).toString().padStart(2, '0')}%"
    val bars = (0 until 16).map { j -> 6.0 + ((i * 7 + j * 5 + tick * 3) % 11) * 2.0 }
    Screen13Stock(i + 1, symbol, name, screen13Money(base + delta * 3), pct, up, bars)
}

class Screen13ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen13State(
            portfolioValue = "$48,215.37",
            portfolioChange = "+612.48 (+1.29%) today",
            portfolioUp = true,
            ranges = listOf("1D", "1W", "1M", "3M", "1Y", "ALL"),
            selectedRange = "1D",
            stocks = screen13Stocks(0),
            marketNote = "Market closed · Prices delayed 15 min",
            tick = 0,
        )
    )
    val state: StateFlow<Screen13State> = _state

    override fun mutate() = _state.update {
        val t = it.tick + 1
        val up = t % 3 != 0
        it.copy(
            tick = t,
            stocks = screen13Stocks(t),
            selectedRange = it.ranges[t % it.ranges.size],
            portfolioUp = up,
            portfolioValue = screen13Money(4_821_537 + (if (up) 1 else -1) * t * 1_311),
            portfolioChange = if (up) "+${612 + t}.48 (+1.${29 + t % 60}%) today" else "-${180 + t}.12 (-0.${37 + t % 60}%) today",
        )
    }
}

@Composable
fun Screen13(vm: Screen13ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Watchlist")) {
            Section {
                VStack(modifier = Modifier.padding(Edge.Set.vertical, 4.0), alignment = HorizontalAlignment.leading, spacing = 6.0) {
                    Text("Portfolio Value", modifier = Modifier.font(Font.caption).textCase(Text.Case.uppercase).foregroundStyle(ShapeStyle.secondary))
                    Text(state.portfolioValue, modifier = Modifier.font(Font.system(size = 34.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                    HStack(modifier = Modifier.foregroundStyle(if (state.portfolioUp) Color.green else Color.red), spacing = 4.0) {
                        Image(systemName = if (state.portfolioUp) "arrow.up.right" else "arrow.down.right", modifier = Modifier.font(Font.caption.bold()))
                        Text(verbatim = state.portfolioChange, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.medium)).monospacedDigit())
                    }
                    HStack(modifier = Modifier.padding(Edge.Set.top, 8.0), spacing = 6.0) {
                        for (range in state.ranges) key(range) {
                            val selected = range == state.selectedRange
                            Text(
                                range,
                                modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold))
                                    .foregroundStyle(if (selected) Color.white else Color.secondary)
                                    .frame(maxWidth = Double.POSITIVE_INFINITY)
                                    .padding(Edge.Set.vertical, 6.0)
                                    .background(if (selected) Color.blue else Color.gray.opacity(0.12), Capsule()),
                            )
                        }
                    }
                }
            }
            Section(header = { Text("Symbols") }, footer = { Text(state.marketNote) }, content = {
                for (stock in state.stocks) key(stock.id) {
                    val tint = if (stock.isUp) Color(red = 0.2, green = 0.78, blue = 0.35) else Color(red = 1.0, green = 0.23, blue = 0.19)
                    HStack(spacing = 12.0) {
                        VStack(modifier = Modifier.frame(width = 96.0, alignment = Alignment.leading), alignment = HorizontalAlignment.leading, spacing = 2.0) {
                            Text(stock.symbol, modifier = Modifier.font(Font.headline))
                            Text(stock.name, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).lineLimit(1))
                        }
                        HStack(modifier = Modifier.frame(height = 28.0, alignment = Alignment.bottom), alignment = VerticalAlignment.bottom, spacing = 2.0) {
                            for ((index, height) in stock.bars.withIndex()) key(index) {
                                ShapeView(
                                    RoundedRectangle(cornerRadius = 1.0),
                                    modifier = Modifier.frame(width = 3.0, height = height),
                                    fill = tint.opacity(0.35 + index * 0.04),
                                )
                            }
                        }
                        Spacer()
                        VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) {
                            Text(stock.price, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)).monospacedDigit())
                            Text(
                                verbatim = stock.change,
                                modifier = Modifier.font(Font.caption.weight(Font.Weight.bold))
                                    .monospacedDigit()
                                    .foregroundStyle(Color.white)
                                    .padding(Edge.Set.horizontal, 6.0)
                                    .padding(Edge.Set.vertical, 3.0)
                                    .frame(minWidth = 64.0)
                                    .background(tint, Capsule()),
                            )
                        }
                    }
                }
            })
        }
    }
}

val screen13 = BenchScreen("13 · Stocks watchlist", ::Screen13ViewModel) { Screen13(it as Screen13ViewModel) }
