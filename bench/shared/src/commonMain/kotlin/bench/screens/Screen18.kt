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

// Screen 18 — Banking: gradient account card, quick actions, savings goals, transactions grouped by day.

data class Screen18Action(val id: Int, val title: String, val icon: String)

data class Screen18Goal(val id: Int, val name: String, val icon: String, val tint: Rgb, val saved: String, val progress: Double)

data class Screen18Transaction(
    val id: Int,
    val merchant: String,
    val detail: String,
    val icon: String,
    val tint: Rgb,
    val amount: String,
    val isCredit: Boolean,
)

data class Screen18Day(val id: Int, val title: String, val total: String, val transactions: List<Screen18Transaction>)

data class Screen18State(
    val accountName: String,
    val balance: String,
    val cardNumber: String,
    val income: String,
    val spent: String,
    val actions: List<Screen18Action>,
    val goals: List<Screen18Goal>,
    val days: List<Screen18Day>,
    val tick: Int,
)

private fun screen18Days(tick: Int): List<Screen18Day> = listOf(
    Screen18Day(
        101, "Today", "-$${54 + tick}.20",
        listOf(
            Screen18Transaction(11, "Blue Bottle Coffee", "Food & Drink · 08:42", "cup.and.saucer.fill", Rgb(0.6, 0.4, 0.2), "-$${6 + tick}.50", false),
            Screen18Transaction(12, "Metro Transit", "Transport · 08:15", "tram.fill", Rgb(0.0, 0.48, 1.0), "-$2.90", false),
            Screen18Transaction(13, "Whole Foods", "Groceries · 07:58", "cart.fill", Rgb(0.2, 0.68, 0.35), "-$44.80", false),
        ),
    ),
    Screen18Day(
        102, "Yesterday", "+$2,318.00",
        listOf(
            Screen18Transaction(21, "Acme Corp Payroll", "Salary · 17:00", "building.columns.fill", Rgb(0.2, 0.78, 0.35), "+$2,450.00", true),
            Screen18Transaction(22, "Netflix", "Subscriptions · 12:00", "play.tv.fill", Rgb(0.9, 0.1, 0.1), "-$15.99", false),
            Screen18Transaction(23, "Shell", "Fuel · 09:31", "fuelpump.fill", Rgb(1.0, 0.75, 0.0), "-$116.01", false),
        ),
    ),
    Screen18Day(
        103, "Thursday, 12 November", "-$89.40",
        listOf(
            Screen18Transaction(31, "Apple Store", "Electronics · 18:22", "applelogo", Rgb(0.2, 0.2, 0.22), "-$79.00", false),
            Screen18Transaction(32, "Refund · Zara", "Shopping · 11:05", "arrow.uturn.backward", Rgb(0.69, 0.32, 0.87), "+$29.60", true),
            Screen18Transaction(33, "City Parking", "Transport · 10:12", "parkingsign", Rgb(0.0, 0.48, 1.0), "-$40.00", false),
        ),
    ),
)

class Screen18ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen18State(
            accountName = "Everyday Checking",
            balance = "$12,480.55",
            cardNumber = "•••• •••• •••• 4821",
            income = "+$4,900.00",
            spent = "-$2,135.42",
            actions = listOf(
                Screen18Action(1, "Send", "arrow.up.right"),
                Screen18Action(2, "Request", "arrow.down.left"),
                Screen18Action(3, "Top up", "plus"),
                Screen18Action(4, "More", "ellipsis"),
            ),
            goals = listOf(
                Screen18Goal(1, "Vacation", "airplane", Rgb(0.0, 0.6, 0.9), "$1,840 of $3,000", 0.61),
                Screen18Goal(2, "Emergency fund", "cross.case.fill", Rgb(1.0, 0.23, 0.19), "$6,200 of $8,000", 0.78),
            ),
            days = screen18Days(0),
            tick = 0,
        )
    )
    val state: StateFlow<Screen18State> = _state

    override fun mutate() = _state.update {
        val t = it.tick + 1
        it.copy(
            tick = t,
            days = screen18Days(t),
            balance = "$12,${(480 - t).toString().padStart(3, '0')}.55",
            spent = "-$2,${135 + t}.42",
            goals = it.goals.map { g -> if (g.id == 1) g.copy(saved = "$${1840 + t * 10} of $3,000", progress = (1840 + t * 10) / 3000.0) else g },
        )
    }
}

@Composable
fun Screen18(vm: Screen18ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Wallet")) {
            Section {
                VStack(
                    modifier = Modifier.padding(20.0)
                        .foregroundStyle(Color.white)
                        .background(
                            LinearGradient(
                                colors = listOf(Color(red = 0.1, green = 0.2, blue = 0.55), Color(red = 0.35, green = 0.25, blue = 0.85)),
                                startPoint = UnitPoint.topLeading,
                                endPoint = UnitPoint.bottomTrailing,
                            ),
                        )
                        .listRowInsets(EdgeInsets()),
                    alignment = HorizontalAlignment.leading,
                    spacing = 14.0,
                ) {
                    HStack {
                        Text(state.accountName, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)).opacity(0.85))
                        Spacer()
                        ImageSystemName("creditcard.fill", modifier = Modifier.font(Font.title3))
                    }
                    Text(state.balance, modifier = Modifier.font(Font.system(size = 36.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                    Text(state.cardNumber, modifier = Modifier.font(Font.footnote).monospaced().opacity(0.8))
                    HStack(spacing = 24.0) {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                            Label("Income", systemImage = "arrow.down.circle.fill", modifier = Modifier.font(Font.caption).opacity(0.8))
                            Text(state.income, modifier = Modifier.font(Font.headline).monospacedDigit())
                        }
                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                            Label("Spent", systemImage = "arrow.up.circle.fill", modifier = Modifier.font(Font.caption).opacity(0.8))
                            Text(state.spent, modifier = Modifier.font(Font.headline).monospacedDigit())
                        }
                    }
                }
                HStack(modifier = Modifier.padding(Edge.Set.vertical, 4.0)) {
                    for (action in state.actions) key(action.id) {
                        VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 6.0) {
                            ImageSystemName(
                                action.icon,
                                modifier = Modifier.font(Font.system(size = 17.0, weight = Font.Weight.semibold))
                                    .foregroundStyle(Color.blue)
                                    .frame(width = 44.0, height = 44.0)
                                    .background(Color.blue.opacity(0.12), Circle()),
                            )
                            Text(action.title, modifier = Modifier.font(Font.caption))
                        }
                    }
                }
            }
            Section(title = "Savings goals") {
                for (goal in state.goals) key(goal.id) {
                    HStack(spacing = 12.0) {
                        ImageSystemName(
                            goal.icon,
                            modifier = Modifier.foregroundStyle(Color.white)
                                .frame(width = 32.0, height = 32.0)
                                .background(goal.tint.color(), RoundedRectangle(cornerRadius = 8.0)),
                        )
                        VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) {
                            HStack {
                                Text(goal.name, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.medium)))
                                Spacer()
                                Text(goal.saved, modifier = Modifier.font(Font.caption).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                            }
                            ProgressView(value = goal.progress, modifier = Modifier.tint(goal.tint.color()))
                        }
                    }
                }
            }
            for (day in state.days) key(day.id) {
                Section(
                    header = {
                        HStack {
                            Text(day.title)
                            Spacer()
                            Text(day.total, modifier = Modifier.monospacedDigit())
                        }
                    },
                    content = {
                        for (tx in day.transactions) key(tx.id) {
                            HStack(spacing = 12.0) {
                                ImageSystemName(
                                    tx.icon,
                                    modifier = Modifier.font(Font.system(size = 15.0, weight = Font.Weight.semibold))
                                        .foregroundStyle(tx.tint.color())
                                        .frame(width = 36.0, height = 36.0)
                                        .background(tx.tint.color().opacity(0.14), Circle()),
                                )
                                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                    Text(tx.merchant, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.medium)))
                                    Text(tx.detail, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                }
                                Spacer()
                                Text(
                                    tx.amount,
                                    modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold))
                                        .monospacedDigit()
                                        .foregroundStyle(if (tx.isCredit) Color.green else Color.primary),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

val screen18 = BenchScreen("18 · Banking", ::Screen18ViewModel) { Screen18(it as Screen18ViewModel) }
