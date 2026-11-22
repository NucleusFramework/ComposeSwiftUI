package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner as AndroidxLocalViewModelStoreOwner

/**
 * Owner of view models for the current composition, provided by [ComposeSwiftUIHost]. The host also provides the
 * official `androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner`, which libraries (`koinViewModel()`,
 * Navigation) read: prefer it.
 */
@Deprecated(
    "Use the official androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner",
    ReplaceWith("LocalViewModelStoreOwner", "androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner"),
)
public val LocalViewModelStoreOwner: androidx.compose.runtime.ProvidableCompositionLocal<ViewModelStoreOwner> =
    staticCompositionLocalOf { error("No ViewModelStoreOwner: content must run inside a ComposeSwiftUIHost") }

/**
 * Same contract as `androidx.lifecycle.viewmodel.compose.viewModel { }`: returns the existing [VM] for ([VM], [key])
 * in the current owner or creates it with [initializer]. The official `viewModel { }` works too, and gives the
 * initializer the `CreationExtras` (`createSavedStateHandle()`).
 */
@Composable
public inline fun <reified VM : ViewModel> viewModel(
    key: String? = null,
    owner: ViewModelStoreOwner = checkNotNull(AndroidxLocalViewModelStoreOwner.current) { "No ViewModelStoreOwner" },
    noinline initializer: () -> VM,
): VM = viewModel(VM::class, key, owner, initializer)

@Composable
public fun <VM : ViewModel> viewModel(
    modelClass: KClass<VM>,
    key: String?,
    owner: ViewModelStoreOwner,
    initializer: () -> VM,
): VM = remember(owner, modelClass, key) {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = initializer() as T
    }
    val extras = (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    val provider = ViewModelProvider.create(owner.viewModelStore, factory, extras)
    if (key != null) provider[key, modelClass] else provider[modelClass]
}
