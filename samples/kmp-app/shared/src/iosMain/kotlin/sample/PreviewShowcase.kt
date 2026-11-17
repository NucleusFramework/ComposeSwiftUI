package sample

import androidx.compose.runtime.Composable
import dev.nucleusframework.composeswiftui.*

// The @Preview options, rendered next to the code by the IDE plugin and in Xcode's canvas.

data class Contact(val name: String, val phone: String, val favorite: Boolean)

class ContactProvider : PreviewParameterProvider<Contact> {
    override val values = sequenceOf(
        Contact("Ada Lovelace", "+44 20 0000 0000", favorite = true),
        Contact("Grace Hopper", "+1 202 000 0000", favorite = false),
        Contact("Alan Turing with a very long name that wraps", "+44 20 1111 1111", favorite = true),
    )
}

@Composable
fun ContactRow(contact: Contact) {
    HStack(modifier = Modifier.padding()) {
        VStack(alignment = HorizontalAlignment.leading) {
            Text(contact.name, modifier = Modifier.font(Font.headline))
            Text(contact.phone, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
        }
        Spacer()
        if (contact.favorite) Image(systemName = SFSymbol.starFill, modifier = Modifier.foregroundStyle(Color.yellow))
    }
}

@Preview(name = "Contact row", group = "Contacts", sizeThatFits = true)
@Composable
fun ContactRowPreview(@PreviewParameter(ContactProvider::class) contact: Contact) {
    ContactRow(contact)
}

@PreviewLightDark
@Composable
fun SettingsPreview() {
    NavigationStack {
        Form(modifier = Modifier.navigationTitle("Settings")) {
            Toggle("Notifications", isOn = true, onIsOnChange = {})
        }
    }
}

@Preview(name = "Large text", group = "Accessibility", dynamicTypeSize = PreviewDynamicTypeSize.Accessibility3)
@Preview(name = "Right to left", group = "Accessibility", layoutDirection = PreviewLayoutDirection.RightToLeft, locale = "ar")
@Composable
fun BadgePreview() {
    HStack(modifier = Modifier.padding()) {
        Image(systemName = SFSymbol.bellFill, modifier = Modifier.foregroundStyle(Color.orange))
        Text("5 new messages")
        Spacer()
    }
}

@Preview(name = "Tablet", width = 820, height = 1180)
@Composable
fun TabletPreview() {
    NavigationSplitView(sidebar = { List { Text("Inbox"); Text("Archive") } }, detail = { Text("Select a mailbox") })
}
