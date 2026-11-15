package bench

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import dev.nucleusframework.composeswiftui.ComposeSwiftUIHost
import dev.nucleusframework.composeswiftui.composeSwiftUI
import dev.nucleusframework.composeswiftui.viewModel

/** Base class of every benchmark screen's view model, shared by the SwiftUI and Compose versions. */
abstract class BenchViewModel : ViewModel() {
    /** Applies one deterministic state change (used by the update benchmark). */
    abstract fun mutate()
}

/** One benchmark screen: its shared view model and its Compose SwiftUI implementation. */
class BenchScreen(
    val title: String,
    val createViewModel: () -> BenchViewModel,
    val content: @Composable (BenchViewModel) -> Unit,
)

object BenchRegistry {
    fun count(): Int = benchScreens.size
    fun title(index: Int): String = benchScreens[index].title
    fun createViewModel(index: Int): BenchViewModel = benchScreens[index].createViewModel()

    /** Compose SwiftUI host for screen [index], bound to an existing (shared) view model. */
    fun composeHost(index: Int, vm: BenchViewModel): ComposeSwiftUIHost = composeSwiftUI {
        // Same API as androidx `viewModel { }`: the instance is owned by the host's store.
        val model = viewModel(key = "bench-$index") { vm }
        benchScreens[index].content(model)
    }
}
