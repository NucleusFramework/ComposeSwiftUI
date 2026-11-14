package bench

import androidx.compose.runtime.*
import dev.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow

class CounterViewModel : androidx.lifecycle.ViewModel() {
    val count = MutableStateFlow(0)
    val name = MutableStateFlow("")
    fun increment() { count.value++ }
}

fun spikeScreen() = composeSwiftUI {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    val name by vm.name.collectAsState()
    var dark by remember { mutableStateOf(false) }
    NavigationStack {
        VStack(spacing = 12.0, modifier = Modifier.padding(length = 20.0).navigationTitle("Spike")) {
            ImageSystemName("star.fill", modifier = Modifier.font(Font.largeTitle).foregroundStyle(Color.yellow))
            Text("Count: $count", modifier = Modifier.font(Font.title).bold())
            Button(action = vm::increment) { Text("Increment") }
            TextField("Name", text = name, onTextChange = { vm.name.value = it })
            Text("Hello ${name.ifEmpty { "nobody" }}")
            Toggle("Dark", isOn = dark, onIsOnChange = { dark = it })
            for (i in 0 until count % 5) key(i) {
                Text("Row $i", modifier = Modifier.padding(length = 8.0).background(Color.blue.opacity(0.2), RoundedRectangle(cornerRadius = 8.0)))
            }
        }
    }
}
