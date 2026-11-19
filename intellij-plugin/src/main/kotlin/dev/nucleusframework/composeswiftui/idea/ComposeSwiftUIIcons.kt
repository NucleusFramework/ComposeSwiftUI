package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.util.IconLoader

/** The plugin's icons: a phone outline, like Android Studio's for its devices. */
object ComposeSwiftUIIcons {
    /** The *iOS App* configuration and the simulators of the toolbar. */
    @JvmField
    val Simulator = IconLoader.getIcon("/icons/iosSimulator.svg", ComposeSwiftUIIcons::class.java)

    /** The *iOS Simulator* tool window. */
    @JvmField
    val SimulatorToolWindow = IconLoader.getIcon("/icons/iosSimulatorToolWindow.svg", ComposeSwiftUIIcons::class.java)
}
