package sample

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class CounterViewModel : ViewModel() {
    val count = MutableStateFlow(0)
    fun increment() = count.update { it + 1 }
}

fun counterScreen() = composeSwiftUI {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    NavigationStack {
        VStack(spacing = 16.0, modifier = Modifier.navigationTitle("Sample")) {
            Image(systemName = "swift", modifier = Modifier.font(Font.system(size = 64.0)).foregroundStyle(Color.orange))
            Text("Count: $count", modifier = Modifier.font(Font.title).monospacedDigit())
            Button("Increment", action = vm::increment)
        }
    }
}
