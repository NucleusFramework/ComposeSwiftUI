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

// Screen 33 — Expense splitting: trip header, participants with avatars and balances, expenses, totals card.

data class Screen33Person(
    val id: Int,
    val name: String,
    val initials: String,
    val tint: Rgb,
    val paid: String,
    val balance: String,
    val owes: Boolean,
)

data class Screen33Expense(val id: Int, val title: String, val icon: String, val tint: Rgb, val detail: String, val amount: String)

data class Screen33Total(val label: String, val value: String)

data class Screen33State(
    val trip: String,
    val dates: String,
    val total: String,
    val people: List<Screen33Person>,
    val expenses: List<Screen33Expense>,
    val totals: List<Screen33Total>,
)

private data class Screen33RawExpense(val id: Int, val title: String, val icon: String, val tint: Rgb, val payer: Int, val cents: Int)

private fun screen33Money(cents: Int): String {
    val abs = if (cents < 0) -cents else cents
    return "$" + (abs / 100) + "." + (abs % 100).toString().padStart(2, '0')
}

private val screen33Names = listOf("Alice Martin", "Bruno Diaz", "Chloé Wong", "David Kim")
private val screen33Tints = listOf(Rgb(0.0, 0.48, 1.0), Rgb(1.0, 0.58, 0.0), Rgb(0.69, 0.32, 0.87), Rgb(0.2, 0.7, 0.45))

private fun screen33State(raw: List<Screen33RawExpense>): Screen33State {
    val total = raw.sumOf { it.cents }
    val share = total / screen33Names.size
    val people = screen33Names.mapIndexed { i, name ->
        val paid = raw.filter { it.payer == i }.sumOf { it.cents }
        val balance = paid - share
        Screen33Person(
            id = i,
            name = name,
            initials = name.split(' ').joinToString("") { it.take(1) },
            tint = screen33Tints[i],
            paid = "Paid " + screen33Money(paid),
            balance = (if (balance < 0) "owes " else "gets ") + screen33Money(balance),
            owes = balance < 0,
        )
    }
    return Screen33State(
        trip = "Lisbon Weekend",
        dates = "8 – 11 Nov · 4 people",
        total = screen33Money(total),
        people = people,
        expenses = raw.map {
            Screen33Expense(it.id, it.title, it.icon, it.tint, "Paid by " + screen33Names[it.payer].substringBefore(' '), screen33Money(it.cents))
        },
        totals = listOf(
            Screen33Total("Total spent", screen33Money(total)),
            Screen33Total("Per person", screen33Money(share)),
            Screen33Total("Expenses", raw.size.toString()),
        ),
    )
}

class Screen33ViewModel : BenchViewModel() {
    private var raw = listOf(
        Screen33RawExpense(1, "Apartment", "house.fill", Rgb(0.35, 0.34, 0.84), 0, 48000),
        Screen33RawExpense(2, "Seafood dinner", "fork.knife", Rgb(1.0, 0.45, 0.2), 1, 13650),
        Screen33RawExpense(3, "Tram tickets", "tram.fill", Rgb(1.0, 0.8, 0.0), 2, 2240),
        Screen33RawExpense(4, "Museum", "building.columns.fill", Rgb(0.2, 0.7, 0.65), 3, 6000),
        Screen33RawExpense(5, "Groceries", "cart.fill", Rgb(0.2, 0.78, 0.35), 1, 4785),
    )
    private val _state = MutableStateFlow(screen33State(raw))
    val state: StateFlow<Screen33State> = _state
    private var tick = 0

    override fun mutate() {
        val target = tick++ % raw.size
        raw = raw.mapIndexed { i, e -> if (i == target) e.copy(cents = e.cents + 525) else e }
        _state.update { screen33State(raw) }
    }
}

@Composable
fun Screen33(vm: Screen33ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Split")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 20.0) {
                VStack(
                    modifier = Modifier.padding(20.0)
                        .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                        .background(
                            LinearGradient(colors = listOf(Color.teal, Color.blue), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                            RoundedRectangle(cornerRadius = 18.0),
                        ),
                    alignment = HorizontalAlignment.leading,
                    spacing = 6.0,
                ) {
                    Label(state.trip, systemImage = "airplane", modifier = Modifier.font(Font.headline).foregroundStyle(Color.white))
                    Text(verbatim = state.total, modifier = Modifier.font(Font.system(size = 38.0, weight = Font.Weight.bold)).monospacedDigit().foregroundStyle(Color.white))
                    Text(verbatim = state.dates, modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.white.opacity(0.8)))
                }
                Text("Balances", modifier = Modifier.font(Font.title3).bold())
                VStack(
                    modifier = Modifier.padding(Edge.Set.vertical, 6.0).background(Color.gray.opacity(0.1), RoundedRectangle(cornerRadius = 14.0)),
                    spacing = 0.0,
                ) {
                    for (person in state.people) key(person.id) {
                        if (person.id > 0) Divider(modifier = Modifier.padding(Edge.Set.leading, 64.0))
                        HStack(modifier = Modifier.padding(Edge.Set.horizontal, 14.0).padding(Edge.Set.vertical, 8.0), spacing = 12.0) {
                            ZStack(modifier = Modifier.frame(width = 40.0, height = 40.0)) {
                                ShapeView(Circle(), fill = person.tint.color().opacity(0.2))
                                Text(verbatim = person.initials, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).foregroundStyle(person.tint.color()))
                            }
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                Text(verbatim = person.name, modifier = Modifier.font(Font.body))
                                Text(verbatim = person.paid, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            }
                            Spacer()
                            Text(
                                verbatim = person.balance,
                                modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).monospacedDigit()
                                    .foregroundStyle(if (person.owes) Color.red else Color.green)
                                    .padding(Edge.Set.horizontal, 8.0)
                                    .padding(Edge.Set.vertical, 4.0)
                                    .background((if (person.owes) Color.red else Color.green).opacity(0.12), Capsule()),
                            )
                        }
                    }
                }
                Text("Expenses", modifier = Modifier.font(Font.title3).bold())
                VStack(spacing = 12.0) {
                    for (expense in state.expenses) key(expense.id) {
                        HStack(spacing = 12.0) {
                            Image(systemName = 
                                expense.icon,
                                modifier = Modifier.font(Font.system(size = 15.0, weight = Font.Weight.semibold))
                                    .foregroundStyle(Color.white)
                                    .frame(width = 34.0, height = 34.0)
                                    .background(expense.tint.color(), Circle()),
                            )
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                Text(verbatim = expense.title)
                                Text(verbatim = expense.detail, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            }
                            Spacer()
                            Text(verbatim = expense.amount, modifier = Modifier.monospacedDigit().fontWeight(Font.Weight.medium))
                        }
                    }
                }
                VStack(
                    modifier = Modifier.padding(16.0).background {
                        ShapeView(RoundedRectangle(cornerRadius = 14.0), stroke = Color.gray.opacity(0.3), lineWidth = 1.0)
                    },
                    spacing = 10.0,
                ) {
                    for (total in state.totals) key(total.label) {
                        HStack {
                            Text(verbatim = total.label, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                            Spacer()
                            Text(verbatim = total.value, modifier = Modifier.monospacedDigit().bold())
                        }
                    }
                    Divider()
                    Button(title = "Settle up", systemImage = "arrow.left.arrow.right", modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.trailing), action = {})
                }
            }
        }
    }
}

val screen33 = BenchScreen("33 · Expense splitting", ::Screen33ViewModel) { Screen33(it as Screen33ViewModel) }
