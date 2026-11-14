package dev.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/** Owner of view models for the current composition. Provided by [ComposeSwiftUIHost]. */
val LocalViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("No ViewModelStoreOwner: content must run inside a ComposeSwiftUIHost")
}

/**
 * Same contract as `androidx.lifecycle.viewmodel.compose.viewModel { }`: returns the existing
 * [VM] for ([VM], [key]) in the current owner or creates it with [initializer].
 * Existing shared view models are reused unchanged.
 */
@Composable
inline fun <reified VM : ViewModel> viewModel(
    key: String? = null,
    owner: ViewModelStoreOwner = LocalViewModelStoreOwner.current,
    noinline initializer: () -> VM,
): VM = viewModel(VM::class, key, owner, initializer)

@Composable
fun <VM : ViewModel> viewModel(
    modelClass: KClass<VM>,
    key: String?,
    owner: ViewModelStoreOwner,
    initializer: () -> VM,
): VM = remember(owner, modelClass, key) {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = initializer() as T
    }
    val provider = ViewModelProvider.create(owner.viewModelStore, factory)
    if (key != null) provider[key, modelClass] else provider[modelClass]
}
