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

// Screen 20 — Contacts: searchable list, "my card" header, sections by letter, avatar initials, phone/email lines.

data class Screen20Contact(
    val id: Int,
    val name: String,
    val initials: String,
    val phone: String,
    val email: String,
    val tint: Rgb,
    val favorite: Boolean,
)

data class Screen20Section(val letter: String, val contacts: List<Screen20Contact>)

data class Screen20State(
    val query: String,
    val myName: String,
    val myDetail: String,
    val sections: List<Screen20Section>,
    val countLabel: String,
)

private val screen20Palette = listOf(
    Rgb(0.0, 0.48, 1.0), Rgb(1.0, 0.58, 0.0), Rgb(0.2, 0.78, 0.35), Rgb(0.69, 0.32, 0.87),
    Rgb(1.0, 0.18, 0.33), Rgb(0.19, 0.69, 0.78), Rgb(0.35, 0.34, 0.84),
)

private val screen20Names = listOf(
    "Aiko Tanaka", "Amir Haddad", "Beatriz Souza", "Ben Carter", "Chloé Martin", "Daniel Kim",
    "Elena Petrova", "Emeka Obi", "Farah Khan", "Gabriel Silva", "Hannah Weber", "Isabel Romero",
    "Jonas Berg", "Kofi Mensah", "Lucía Fernández", "Mateo Rossi",
)

private fun screen20Contacts(favorites: Set<Int>): List<Screen20Contact> = screen20Names.mapIndexed { i, name ->
    val parts = name.split(" ")
    val initials = "${parts[0].first()}${parts[1].first()}"
    val handle = parts[0].lowercase().replace("é", "e").replace("í", "i")
    Screen20Contact(
        id = i + 1,
        name = name,
        initials = initials,
        phone = "+1 (415) 555-${(1200 + i * 37).toString().padStart(4, '0')}",
        email = "$handle@example.com",
        tint = screen20Palette[i % screen20Palette.size],
        favorite = (i + 1) in favorites,
    )
}

private fun screen20State(query: String, favorites: Set<Int>): Screen20State {
    val contacts = screen20Contacts(favorites).filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
    val sections = contacts.groupBy { it.name.first().toString() }.map { (letter, list) -> Screen20Section(letter, list) }
    return Screen20State(
        query = query,
        myName = "Ada Lovelace",
        myDetail = "My Card · ada@example.com",
        sections = sections,
        countLabel = "${contacts.size} contacts",
    )
}

class Screen20ViewModel : BenchViewModel() {
    private var favorites = setOf(2, 5, 9)
    private var tick = 0
    private val _state = MutableStateFlow(screen20State("", favorites))
    val state: StateFlow<Screen20State> = _state

    fun setQuery(value: String) = _state.update { screen20State(value, favorites) }

    override fun mutate() {
        tick += 1
        val id = tick % screen20Names.size + 1
        favorites = if (id in favorites) favorites - id else favorites + id
        _state.update { screen20State(it.query, favorites) }
    }
}

@Composable
fun Screen20(vm: Screen20ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(
            modifier = Modifier.listStyle(ListStyle.plain)
                .searchable(text = state.query, onTextChange = vm::setQuery, prompt = "Search contacts")
                .navigationTitle("Contacts"),
        ) {
            HStack(modifier = Modifier.padding(Edge.Set.vertical, 6.0), spacing = 14.0) {
                ZStack(modifier = Modifier.frame(width = 56.0, height = 56.0)) {
                    ShapeView(
                        Circle(),
                        fill = LinearGradient(
                            colors = listOf(Color(red = 0.55, green = 0.6, blue = 0.7), Color(red = 0.35, green = 0.4, blue = 0.5)),
                            startPoint = UnitPoint.top,
                            endPoint = UnitPoint.bottom,
                        ),
                    )
                    Text("AL", modifier = Modifier.font(Font.title2.weight(Font.Weight.semibold)).foregroundStyle(Color.white))
                }
                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                    Text(state.myName, modifier = Modifier.font(Font.title3.weight(Font.Weight.semibold)))
                    Text(state.myDetail, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                }
            }
            for (section in state.sections) key(section.letter) {
                Section(
                    header = { Text(section.letter, modifier = Modifier.font(Font.headline).foregroundStyle(Color.blue)) },
                    content = {
                        for (contact in section.contacts) key(contact.id) {
                            HStack(spacing = 12.0) {
                                Text(
                                    contact.initials,
                                    modifier = Modifier.font(Font.system(size = 15.0, weight = Font.Weight.semibold, design = Font.Design.rounded))
                                        .foregroundStyle(Color.white)
                                        .frame(width = 40.0, height = 40.0)
                                        .background(contact.tint.color(), Circle()),
                                )
                                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                    Text(contact.name, modifier = Modifier.font(Font.body.weight(Font.Weight.medium)))
                                    HStack(modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).lineLimit(1), spacing = 6.0) {
                                        Label(contact.phone, systemImage = SFSymbol.phoneFill)
                                        Label(contact.email, systemImage = SFSymbol.envelopeFill)
                                    }
                                }
                                Spacer(minLength = 0.0)
                                if (contact.favorite) {
                                    Image(systemName = SFSymbol.starFill, modifier = Modifier.font(Font.caption).foregroundStyle(Color.yellow))
                                }
                            }
                        }
                    },
                )
            }
            Text(state.countLabel, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary).frame(maxWidth = Double.POSITIVE_INFINITY))
        }
    }
}

val screen20 = BenchScreen("20 · Contacts", ::Screen20ViewModel) { Screen20(it as Screen20ViewModel) }
