package bench

import dev.nucleusframework.composeswiftui.Color

/** RGB color shared by both implementations (Swift: `Color(red:green:blue:)`). */
data class Rgb(val r: Double, val g: Double, val b: Double) {
    fun color(): Color = Color(red = r, green = g, blue = b)
}
