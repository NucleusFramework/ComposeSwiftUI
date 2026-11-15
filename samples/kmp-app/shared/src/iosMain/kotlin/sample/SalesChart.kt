package sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController

private val sales = listOf(12f, 18f, 9f, 24f, 30f, 21f, 27f)

/** A Compose Multiplatform screen, wrapped in a UIViewController. */
fun salesChartController() = ComposeUIViewController {
    run {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            BasicText("Weekly sales (Compose Multiplatform)", style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
            Canvas(Modifier.fillMaxWidth().padding(top = 16.dp).fillMaxSize()) {
                val max = sales.max()
                val barWidth = size.width / (sales.size * 1.6f)
                sales.forEachIndexed { i, value ->
                    val height = size.height * value / max
                    drawRoundRect(
                        color = Color(0xFF3478F6),
                        topLeft = Offset(i * barWidth * 1.6f, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(8f, 8f),
                    )
                }
            }
        }
    }
}
