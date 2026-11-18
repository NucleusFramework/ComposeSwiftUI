package sample.multi

import androidx.compose.runtime.Composable
import dev.nucleusframework.composeswiftui.*

@Composable
fun Badge(text: String) {
    HStack(modifier = Modifier.padding()) {
        Image(systemName = SFSymbol.bellFill, modifier = Modifier.foregroundStyle(Color.orange))
        Text(text)
        Spacer()
    }
}

// Rendered on the iOS simulator: the default of a module with an iOS simulator target.
@Preview(name = "Badge on iPhone", sizeThatFits = true)
@Composable
fun BadgePhonePreview() {
    Badge("5 new messages")
}

// The same module, rendered on the Mac.
@Preview(name = "Badge on Mac", device = "mac", width = 400, sizeThatFits = true)
@Composable
fun BadgeMacPreview() {
    Badge("5 new messages")
}
