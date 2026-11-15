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

// Screen 50 — System status admin: status banner, GroupBoxes of services with status dots and uptime,
// resource ProgressView bars and an incidents list.

data class Screen50Service(val id: Int, val name: String, val uptime: String, val latency: String, val status: String, val tint: Rgb)

data class Screen50Resource(val id: Int, val name: String, val value: Double, val text: String, val tint: Rgb)

data class Screen50Group(
    val id: Int,
    val title: String,
    val icon: String,
    val region: String,
    val services: List<Screen50Service>,
    val resources: List<Screen50Resource>,
)

data class Screen50Incident(
    val id: Int,
    val title: String,
    val detail: String,
    val time: String,
    val icon: String,
    val tint: Rgb,
    val state: String,
)

data class Screen50State(
    val bannerTitle: String,
    val bannerDetail: String,
    val bannerIcon: String,
    val bannerTint: Rgb,
    val updated: String,
    val groups: List<Screen50Group>,
    val incidents: List<Screen50Incident>,
)

private val screen50Ok = Rgb(0.2, 0.78, 0.35)
private val screen50Warn = Rgb(1.0, 0.62, 0.0)
private val screen50Down = Rgb(1.0, 0.23, 0.19)

private fun screen50Service(id: Int, name: String, uptime: String, latency: String, degraded: Boolean) =
    Screen50Service(id, name, uptime, latency, if (degraded) "Degraded" else "Operational", if (degraded) screen50Warn else screen50Ok)

private fun screen50Resource(id: Int, name: String, pct: Int) =
    Screen50Resource(id, name, pct / 100.0, "$pct%", if (pct >= 85) screen50Down else if (pct >= 70) screen50Warn else Rgb(0.0, 0.48, 1.0))

private fun screen50Build(tick: Int): Screen50State {
    val degraded = tick % 3 == 1
    val groups = listOf(
        Screen50Group(
            1, "Core API", "server.rack", "eu-west-1",
            listOf(
                screen50Service(1, "Gateway", "99.99%", "42 ms", false),
                screen50Service(2, "Auth", "99.97%", "${58 + tick % 9} ms", degraded),
                screen50Service(3, "Billing", "99.95%", "77 ms", false),
            ),
            listOf(screen50Resource(1, "CPU", 48 + tick % 40), screen50Resource(2, "Memory", 72)),
        ),
        Screen50Group(
            2, "Data", "cylinder.split.1x2.fill", "us-east-2",
            listOf(
                screen50Service(4, "Postgres primary", "100.00%", "3 ms", false),
                screen50Service(5, "Search cluster", "99.81%", "120 ms", tick % 4 == 2),
            ),
            listOf(screen50Resource(3, "Disk", 86), screen50Resource(4, "IOPS", 35 + tick % 20)),
        ),
    )
    val anyDegraded = groups.any { g -> g.services.any { it.status != "Operational" } }
    return Screen50State(
        bannerTitle = if (anyDegraded) "Partial degradation" else "All systems operational",
        bannerDetail = if (anyDegraded) "Some services are responding slowly" else "5 services · 2 regions",
        bannerIcon = if (anyDegraded) "exclamationmark.triangle.fill" else "checkmark.shield.fill",
        bannerTint = if (anyDegraded) screen50Warn else screen50Ok,
        updated = "Updated ${tick % 60 + 1} min ago",
        groups = groups,
        incidents = listOf(
            Screen50Incident(1, "Elevated search latency", "Index rebalancing in progress", "Today, 09:12", "magnifyingglass", screen50Warn, "Monitoring"),
            Screen50Incident(2, "Auth token refresh errors", "Rolled back release 4.18.2", "Yesterday, 22:40", "key.fill", screen50Down, "Resolved"),
            Screen50Incident(3, "Scheduled DB maintenance", "Primary failover drill", "Nov 18, 02:00", "wrench.and.screwdriver.fill", Rgb(0.0, 0.48, 1.0), "Scheduled"),
        ),
    )
}

class Screen50ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(screen50Build(tick))
    val state: StateFlow<Screen50State> = _state

    override fun mutate() {
        tick += 1
        _state.update { screen50Build(tick) }
    }
}

@Composable
fun Screen50(vm: Screen50ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("System Status")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 16.0) {
                HStack(
                    modifier = Modifier.padding(14.0)
                        .background(state.bannerTint.color().opacity(0.14), RoundedRectangle(cornerRadius = 14.0)),
                    spacing = 12.0,
                ) {
                    Image(systemName = state.bannerIcon, modifier = Modifier.font(Font.title).foregroundStyle(state.bannerTint.color()))
                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                        Text(state.bannerTitle, modifier = Modifier.font(Font.headline))
                        Text(state.bannerDetail, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                    }
                    Spacer()
                    Text(state.updated, modifier = Modifier.font(Font.caption2).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                }
                for (group in state.groups) key(group.id) {
                    GroupBox(content = {
                        VStack(modifier = Modifier.padding(Edge.Set.top, 6.0), alignment = HorizontalAlignment.leading, spacing = 10.0) {
                            for (service in group.services) key(service.id) {
                                HStack(spacing = 10.0) {
                                    ShapeView(Circle(), modifier = Modifier.frame(width = 9.0, height = 9.0), fill = service.tint.color())
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 1.0) {
                                        Text(service.name, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.medium))
                                        Text(service.status, modifier = Modifier.font(Font.caption2).foregroundStyle(service.tint.color()))
                                    }
                                    Spacer()
                                    VStack(alignment = HorizontalAlignment.trailing, spacing = 1.0) {
                                        Text(service.uptime, modifier = Modifier.font(Font.subheadline).monospacedDigit())
                                        Text(service.latency, modifier = Modifier.font(Font.caption2).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                                    }
                                }
                            }
                            Divider()
                            for (resource in group.resources) key(resource.id) {
                                ProgressView(value = resource.value, modifier = Modifier.tint(resource.tint.color()), label = {
                                    Text(resource.name, modifier = Modifier.font(Font.caption))
                                }, currentValueLabel = {
                                    Text(resource.text, modifier = Modifier.font(Font.caption2).monospacedDigit())
                                })
                            }
                        }
                    }, label = {
                        HStack {
                            Label(title = group.title, systemImage = group.icon)
                            Spacer()
                            Text(group.region, modifier = Modifier.font(Font.caption).monospaced().foregroundStyle(ShapeStyle.secondary))
                        }
                    })
                }
                Text("Recent incidents", modifier = Modifier.font(Font.title3).bold().padding(Edge.Set.top, 4.0))
                VStack(spacing = 0.0) {
                    for (incident in state.incidents) key(incident.id) {
                        HStack(modifier = Modifier.padding(Edge.Set.vertical, 10.0), alignment = VerticalAlignment.top, spacing = 12.0) {
                            Image(systemName = 
                                incident.icon,
                                modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.white)
                                    .frame(width = 32.0, height = 32.0)
                                    .background(incident.tint.color(), RoundedRectangle(cornerRadius = 8.0)),
                            )
                            VStack(alignment = HorizontalAlignment.leading, spacing = 3.0) {
                                Text(incident.title, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold))
                                Text(incident.detail, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                Text(incident.time, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.tertiary))
                            }
                            Spacer()
                            Text(
                                incident.state,
                                modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.semibold).foregroundStyle(incident.tint.color())
                                    .padding(Edge.Set.horizontal, 8.0).padding(Edge.Set.vertical, 3.0)
                                    .background(incident.tint.color().opacity(0.15), Capsule()),
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

val screen50 = BenchScreen("50 · System status", ::Screen50ViewModel) { Screen50(it as Screen50ViewModel) }
