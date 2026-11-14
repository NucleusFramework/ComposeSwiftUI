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

// Screen 17 — Recipe detail: gradient header, quick facts, servings stepper, ingredients checklist, numbered steps.

data class Screen17Fact(val id: Int, val icon: String, val value: String, val label: String)

data class Screen17Ingredient(val id: Int, val name: String, val amount: String, val checked: Boolean)

data class Screen17Step(val id: Int, val number: String, val title: String, val text: String, val timer: String)

data class Screen17State(
    val category: String,
    val title: String,
    val author: String,
    val rating: String,
    val facts: List<Screen17Fact>,
    val servings: Int,
    val servingsLabel: String,
    val ingredients: List<Screen17Ingredient>,
    val checkedLabel: String,
    val steps: List<Screen17Step>,
)

private val screen17Base = listOf(
    Triple("Spaghetti", 100, "g"),
    Triple("Guanciale", 40, "g"),
    Triple("Egg yolks", 1, ""),
    Triple("Pecorino Romano", 25, "g"),
    Triple("Black pepper", 1, "tsp"),
    Triple("Sea salt", 5, "g"),
)

private fun screen17Ingredients(servings: Int, checked: Set<Int>): List<Screen17Ingredient> = screen17Base.mapIndexed { i, (name, qty, unit) ->
    val amount = if (unit.isEmpty()) "${qty * servings}" else "${qty * servings} $unit"
    Screen17Ingredient(i + 1, name, amount, (i + 1) in checked)
}

private fun screen17State(servings: Int, checked: Set<Int>): Screen17State {
    val ingredients = screen17Ingredients(servings, checked)
    return Screen17State(
        category = "Italian · Pasta",
        title = "Spaghetti alla Carbonara",
        author = "by Chef Giulia Rossi",
        rating = "4.8 (2,314)",
        facts = listOf(
            Screen17Fact(1, "clock", "25 min", "Total"),
            Screen17Fact(2, "flame", "${520 * servings} kcal", "Energy"),
            Screen17Fact(3, "chart.bar", "Easy", "Level"),
        ),
        servings = servings,
        servingsLabel = "Servings: $servings",
        ingredients = ingredients,
        checkedLabel = "${ingredients.count { it.checked }} of ${ingredients.size} ready",
        steps = listOf(
            Screen17Step(1, "1", "Boil the pasta", "Bring a large pot of salted water to a rolling boil and cook the spaghetti until al dente.", "10 min"),
            Screen17Step(2, "2", "Crisp the guanciale", "Cut into strips and render slowly in a dry pan until golden and crisp.", "6 min"),
            Screen17Step(3, "3", "Make the cream", "Whisk the yolks with grated pecorino and plenty of freshly cracked pepper.", "3 min"),
            Screen17Step(4, "4", "Bring it together", "Toss the pasta with the guanciale off the heat, add the egg mixture and a splash of pasta water.", "2 min"),
        ),
    )
}

class Screen17ViewModel : BenchViewModel() {
    private var checked = setOf(1, 3)
    private val _state = MutableStateFlow(screen17State(2, checked))
    val state: StateFlow<Screen17State> = _state
    private var tick = 0

    fun setServings(value: Int) = _state.update { screen17State(value, checked) }

    override fun mutate() {
        tick += 1
        val id = tick % screen17Base.size + 1
        checked = if (id in checked) checked - id else checked + id
        _state.update { screen17State(1 + tick % 6, checked) }
    }
}

@Composable
fun Screen17(vm: Screen17ViewModel) {
    val state by vm.state.collectAsState()
    val accent = Color(red = 0.95, green = 0.45, blue = 0.15)
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Recipe")) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
                ZStack(modifier = Modifier.frame(height = 230.0), alignment = Alignment.bottomLeading) {
                    LinearGradientView(
                        LinearGradient(
                            colors = listOf(Color(red = 1.0, green = 0.75, blue = 0.3), Color(red = 0.9, green = 0.3, blue = 0.2)),
                            startPoint = UnitPoint.top,
                            endPoint = UnitPoint.bottom,
                        ),
                    )
                    Image(systemName = 
                        "fork.knife.circle.fill",
                        modifier = Modifier.font(Font.system(size = 110.0))
                            .foregroundStyle(Color.white.opacity(0.25))
                            .frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY, alignment = Alignment.topTrailing)
                            .padding(16.0),
                    )
                    VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 6.0) {
                        Text(
                            state.category,
                            modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold))
                                .foregroundStyle(Color.white)
                                .padding(Edge.Set.horizontal, 10.0)
                                .padding(Edge.Set.vertical, 4.0)
                                .background(Color.black.opacity(0.25), Capsule()),
                        )
                        Text(state.title, modifier = Modifier.font(Font.title.bold()).foregroundStyle(Color.white))
                        HStack(modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.white.opacity(0.9)), spacing = 12.0) {
                            Text(state.author)
                            Label(state.rating, systemImage = "star.fill")
                        }
                    }
                }
                VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 20.0) {
                    HStack(modifier = Modifier.padding(Edge.Set.vertical, 12.0).background(Color.gray.opacity(0.08), RoundedRectangle(cornerRadius = 14.0))) {
                        for (fact in state.facts) key(fact.id) {
                            VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 4.0) {
                                Image(systemName = fact.icon, modifier = Modifier.font(Font.title3).foregroundStyle(accent))
                                Text(fact.value, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)).monospacedDigit())
                                Text(fact.label, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                            }
                        }
                    }
                    Stepper(title = state.servingsLabel, value = state.servings, bounds = 1..12, onValueChange = vm::setServings)
                    HStack(alignment = VerticalAlignment.firstTextBaseline) {
                        Text("Ingredients", modifier = Modifier.font(Font.title2.bold()))
                        Spacer()
                        Text(state.checkedLabel, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                    }
                    VStack(spacing = 0.0) {
                        for (ingredient in state.ingredients) key(ingredient.id) {
                            HStack(modifier = Modifier.padding(Edge.Set.vertical, 10.0), spacing = 12.0) {
                                Image(systemName = 
                                    if (ingredient.checked) "checkmark.circle.fill" else "circle",
                                    modifier = Modifier.font(Font.title3).foregroundStyle(if (ingredient.checked) Color.green else Color.gray.opacity(0.5)),
                                )
                                Text(ingredient.name, modifier = Modifier.strikethrough(isActive = ingredient.checked, pattern = Text.LineStyle.Pattern.solid, color = null).foregroundStyle(if (ingredient.checked) Color.secondary else Color.primary))
                                Spacer()
                                Text(ingredient.amount, modifier = Modifier.font(Font.subheadline).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                            }
                            Divider()
                        }
                    }
                    Text("Method", modifier = Modifier.font(Font.title2.bold()))
                    for (step in state.steps) key(step.id) {
                        HStack(alignment = VerticalAlignment.top, spacing = 14.0) {
                            Text(
                                step.number,
                                modifier = Modifier.font(Font.headline)
                                    .foregroundStyle(Color.white)
                                    .frame(width = 32.0, height = 32.0)
                                    .background(accent, Circle()),
                            )
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                                HStack {
                                    Text(step.title, modifier = Modifier.font(Font.headline))
                                    Spacer()
                                    Label(step.timer, systemImage = "timer", modifier = Modifier.font(Font.caption).foregroundStyle(accent))
                                }
                                Text(step.text, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary).fixedSize(horizontal = false, vertical = true))
                            }
                        }
                    }
                }
            }
        }
    }
}

val screen17 = BenchScreen("17 · Recipe detail", ::Screen17ViewModel) { Screen17(it as Screen17ViewModel) }
