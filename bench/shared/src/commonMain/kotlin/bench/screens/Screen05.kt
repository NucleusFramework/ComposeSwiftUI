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

// Screen 05 — Weather: gradient background, big temperature, hourly strip, 7-day rows with capsule range bars.

data class Screen05Hour(val id: Int, val time: String, val icon: String, val temp: String, val rain: String)

data class Screen05Day(
    val id: Int,
    val day: String,
    val icon: String,
    val low: String,
    val high: String,
    val barStart: Double,
    val barWidth: Double,
)

data class Screen05Detail(val label: String, val value: String)

data class Screen05State(
    val city: String,
    val temperature: String,
    val condition: String,
    val highLow: String,
    val skyTop: Rgb,
    val skyBottom: Rgb,
    val summary: String,
    val hours: List<Screen05Hour>,
    val days: List<Screen05Day>,
    val details: List<Screen05Detail>,
)

class Screen05ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen05State(
            city = "Cupertino",
            temperature = "21°",
            condition = "Mostly Sunny",
            highLow = "H:24°  L:13°",
            skyTop = Rgb(0.2, 0.45, 0.85),
            skyBottom = Rgb(0.45, 0.72, 0.95),
            summary = "Sunny conditions will continue all day. Wind gusts are up to 18 km/h.",
            hours = listOf(
                Screen05Hour(1, "Now", "sun.max.fill", "21°", ""),
                Screen05Hour(2, "11", "sun.max.fill", "22°", ""),
                Screen05Hour(3, "12", "cloud.sun.fill", "23°", ""),
                Screen05Hour(4, "13", "cloud.sun.fill", "24°", "10%"),
                Screen05Hour(5, "14", "cloud.fill", "23°", "20%"),
                Screen05Hour(6, "15", "cloud.drizzle.fill", "21°", "40%"),
                Screen05Hour(7, "16", "cloud.rain.fill", "19°", "60%"),
            ),
            days = listOf(
                Screen05Day(1, "Today", "sun.max.fill", "13°", "24°", 20.0, 60.0),
                Screen05Day(2, "Sun", "cloud.sun.fill", "12°", "22°", 14.0, 52.0),
                Screen05Day(3, "Mon", "cloud.rain.fill", "10°", "17°", 4.0, 36.0),
                Screen05Day(4, "Tue", "cloud.bolt.rain.fill", "9°", "16°", 0.0, 34.0),
                Screen05Day(5, "Wed", "cloud.fill", "11°", "19°", 9.0, 42.0),
                Screen05Day(6, "Thu", "sun.max.fill", "14°", "26°", 24.0, 66.0),
                Screen05Day(7, "Fri", "wind", "15°", "25°", 28.0, 54.0),
            ),
            details = listOf(Screen05Detail("UV Index", "6 High"), Screen05Detail("Humidity", "48%"), Screen05Detail("Wind", "12 km/h")),
        )
    )
    val state: StateFlow<Screen05State> = _state

    override fun mutate() = _state.update { s ->
        tick++
        val t = 18 + tick % 8
        s.copy(
            temperature = "$t°",
            condition = if (tick % 2 == 0) "Mostly Sunny" else "Partly Cloudy",
            hours = s.hours.map { if (it.id == 1) it.copy(temp = "$t°", icon = if (tick % 2 == 0) "sun.max.fill" else "cloud.sun.fill") else it },
            days = s.days.map { if (it.id == 1) it.copy(barWidth = 40.0 + tick % 30) else it },
        )
    }
}

@Composable
fun Screen05(vm: Screen05ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ZStack(modifier = Modifier.navigationTitle("Weather").toolbarTitleDisplayMode(ToolbarTitleDisplayMode.inline)) {
            LinearGradientView(
                LinearGradient(colors = listOf(state.skyTop.color(), state.skyBottom.color()), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom),
                modifier = Modifier.ignoresSafeArea(),
            )
            ScrollView {
                VStack(spacing = 14.0, modifier = Modifier.padding().foregroundStyle(Color.white)) {
                    VStack(spacing = 2.0) {
                        Text(state.city, modifier = Modifier.font(Font.title))
                        Text(state.temperature, modifier = Modifier.font(Font.system(size = 88.0, weight = Font.Weight.thin)))
                        Text(state.condition, modifier = Modifier.font(Font.title3).fontWeight(Font.Weight.medium))
                        Text(state.highLow, modifier = Modifier.font(Font.title3).fontWeight(Font.Weight.medium))
                    }
                    VStack(alignment = HorizontalAlignment.leading, spacing = 10.0, modifier = Modifier.padding(14.0)
                        .background(Color.white.opacity(0.16), RoundedRectangle(cornerRadius = 16.0))) {
                        Text(state.summary, modifier = Modifier.font(Font.footnote))
                        Divider(modifier = Modifier.overlay(Color.white.opacity(0.4)))
                        ScrollView(axes = Axis.Set.horizontal, showsIndicators = false) {
                            HStack(spacing = 22.0, modifier = Modifier.fixedSize()) {
                                for (hour in state.hours) key(hour.id) {
                                    VStack(spacing = 8.0) {
                                        Text(hour.time, modifier = Modifier.font(Font.footnote.weight(Font.Weight.semibold)))
                                        Image(systemName = hour.icon, modifier = Modifier.symbolRenderingMode(SymbolRenderingMode.multicolor).font(Font.title3).frame(height = 26.0))
                                        Text(verbatim = hour.rain, modifier = Modifier.font(Font.caption2.bold()).foregroundStyle(Color.cyan))
                                        Text(verbatim = hour.temp, modifier = Modifier.font(Font.headline))
                                    }
                                }
                            }
                        }
                    }
                    VStack(alignment = HorizontalAlignment.leading, spacing = 8.0, modifier = Modifier.padding(14.0)
                        .background(Color.white.opacity(0.16), RoundedRectangle(cornerRadius = 16.0))) {
                        Label(title = "7-DAY FORECAST", systemImage = "calendar", modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold)).opacity(0.75))
                        for (day in state.days) key(day.id) {
                            Divider(modifier = Modifier.overlay(Color.white.opacity(0.4)))
                            HStack(spacing = 10.0) {
                                Text(day.day, modifier = Modifier.font(Font.body.weight(Font.Weight.medium)).frame(width = 56.0, alignment = Alignment.leading))
                                Image(systemName = day.icon, modifier = Modifier.symbolRenderingMode(SymbolRenderingMode.multicolor).frame(width = 30.0))
                                Text(day.low, modifier = Modifier.opacity(0.7).frame(width = 36.0, alignment = Alignment.trailing))
                                ZStack(alignment = Alignment.leading, modifier = Modifier.frame(width = 100.0)) {
                                    ShapeView(Capsule(), fill = Color.black.opacity(0.18), modifier = Modifier.frame(height = 5.0))
                                    ShapeView(
                                        Capsule(),
                                        fill = LinearGradient(colors = listOf(Color.mint, Color.yellow, Color.orange), startPoint = UnitPoint.leading, endPoint = UnitPoint.trailing),
                                        modifier = Modifier.frame(width = day.barWidth, height = 5.0).offset(x = day.barStart),
                                    )
                                }
                                Text(day.high, modifier = Modifier.frame(width = 36.0, alignment = Alignment.trailing))
                            }
                        }
                    }
                    HStack(spacing = 10.0) {
                        for (detail in state.details) key(detail.label) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0, modifier = Modifier.padding(12.0)
                                .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                                .background(Color.white.opacity(0.16), RoundedRectangle(cornerRadius = 14.0))) {
                                Text(detail.label, modifier = Modifier.font(Font.caption).opacity(0.75))
                                Text(verbatim = detail.value, modifier = Modifier.font(Font.headline))
                            }
                        }
                    }
                }
            }
        }
    }
}

val screen05 = BenchScreen("05 · Weather", ::Screen05ViewModel) { Screen05(it as Screen05ViewModel) }
