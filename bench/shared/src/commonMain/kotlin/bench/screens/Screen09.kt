package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import bench.Rgb
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 09 — Sign-up form: text/secure fields, password strength meter, pickers, stepper, toggles, summary.

data class Screen09Option(val id: Int, val label: String)

data class Screen09Bar(val id: Int, val tint: Rgb)

data class Screen09State(
    val name: String,
    val email: String,
    val password: String,
    val confirm: String,
    val strengthBars: List<Screen09Bar>,
    val strengthLabel: String,
    val strengthTint: Rgb,
    val country: Int,
    val countries: List<Screen09Option>,
    val role: Int,
    val roles: List<Screen09Option>,
    val age: Int,
    val ageLabel: String,
    val newsletter: Boolean,
    val terms: Boolean,
    val plan: String,
    val referral: String,
    val footer: String,
)

private val screen09Off = Rgb(0.85, 0.85, 0.87)

private fun Screen09State.withStrength(level: Int): Screen09State {
    val tint = when (level) {
        1 -> Rgb(1.0, 0.23, 0.19)
        2 -> Rgb(1.0, 0.58, 0.0)
        3 -> Rgb(0.95, 0.77, 0.06)
        else -> Rgb(0.2, 0.78, 0.35)
    }
    val label = when (level) {
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        else -> "Strong"
    }
    return copy(
        strengthBars = (1..4).map { Screen09Bar(it, if (it <= level) tint else screen09Off) },
        strengthLabel = label,
        strengthTint = tint,
    )
}

class Screen09ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen09State(
            name = "Hedy Lamarr",
            email = "hedy@frequency.dev",
            password = "spread-spectrum-42",
            confirm = "spread-spectrum-42",
            strengthBars = emptyList(),
            strengthLabel = "",
            strengthTint = screen09Off,
            country = 2,
            countries = listOf(Screen09Option(1, "Austria"), Screen09Option(2, "France"), Screen09Option(3, "Japan"), Screen09Option(4, "United States")),
            role = 1,
            roles = listOf(Screen09Option(1, "Developer"), Screen09Option(2, "Designer"), Screen09Option(3, "Manager")),
            age = 29,
            ageLabel = "Age: 29",
            newsletter = true,
            terms = false,
            plan = "Free trial · 30 days",
            referral = "HEDY-2026",
            footer = "By creating an account you agree to the Terms of Service and acknowledge the Privacy Policy.",
        ).withStrength(3)
    )
    val state: StateFlow<Screen09State> = _state

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setEmail(value: String) = _state.update { it.copy(email = value) }
    fun setPassword(value: String) = _state.update { it.copy(password = value) }
    fun setConfirm(value: String) = _state.update { it.copy(confirm = value) }
    fun setCountry(value: Int) = _state.update { it.copy(country = value) }
    fun setRole(value: Int) = _state.update { it.copy(role = value) }
    fun setAge(value: Int) = _state.update { it.copy(age = value, ageLabel = "Age: $value") }
    fun setNewsletter(value: Boolean) = _state.update { it.copy(newsletter = value) }
    fun setTerms(value: Boolean) = _state.update { it.copy(terms = value) }

    override fun mutate() = _state.update { s ->
        tick++
        val age = 18 + tick % 50
        s.copy(age = age, ageLabel = "Age: $age", terms = tick % 2 == 1, role = 1 + tick % 3).withStrength(1 + tick % 4)
    }
}

@Composable
fun Screen09(vm: Screen09ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        Form(modifier = Modifier.formStyle(FormStyle.grouped).navigationTitle("Sign Up")) {
            Section(header = { Text("Account") }, content = {
                TextField(title = "Full name", text = state.name, onTextChange = vm::setName)
                TextField(title = "Email", text = state.email, onTextChange = vm::setEmail, modifier = Modifier.autocorrectionDisabled())
                SecureField(title = "Password", text = state.password, onTextChange = vm::setPassword)
                SecureField(title = "Confirm password", text = state.confirm, onTextChange = vm::setConfirm)
                HStack(spacing = 6.0) {
                    Text("Strength", modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                    for (bar in state.strengthBars) key(bar.id) {
                        ShapeView(Capsule(), fill = bar.tint.color(), modifier = Modifier.frame(height = 6.0))
                    }
                    Text(state.strengthLabel, modifier = Modifier.font(Font.footnote.bold()).foregroundStyle(state.strengthTint.color()).frame(width = 54.0, alignment = Alignment.trailing))
                }
            })
            Section(header = { Text("Profile") }, content = {
                Picker(title = "Country", selection = state.country, onSelectionChange = { vm.setCountry(it as Int) }) {
                    for (option in state.countries) key(option.id) {
                        Text(option.label, modifier = Modifier.tag(option.id))
                    }
                }
                Picker(title = "Role", selection = state.role, onSelectionChange = { vm.setRole(it as Int) }, modifier = Modifier.pickerStyle(PickerStyle.segmented)) {
                    for (option in state.roles) key(option.id) {
                        Text(option.label, modifier = Modifier.tag(option.id))
                    }
                }
                Stepper(value = state.age, bounds = 13..99, onValueChange = vm::setAge) {
                    Text(state.ageLabel)
                }
                Toggle(title = "Product newsletter", systemImage = "envelope.badge", isOn = state.newsletter, onIsOnChange = vm::setNewsletter)
            })
            Section(header = { Text("Plan") }, footer = { Text(state.footer) }, content = {
                LabeledContent(title = "Plan", value = state.plan)
                LabeledContent(title = "Referral code") {
                    Text(state.referral, modifier = Modifier.font(Font.body.monospaced()).foregroundStyle(Color.purple))
                }
                Toggle(title = "I accept the terms", isOn = state.terms, onIsOnChange = vm::setTerms)
            })
            Section {
                Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).disabled(!state.terms)) {
                    Text("Create Account", modifier = Modifier.font(Font.headline).frame(maxWidth = Double.POSITIVE_INFINITY))
                }
            }
        }
    }
}

val screen09 = BenchScreen("09 · Sign-up form", ::Screen09ViewModel) { Screen09(it as Screen09ViewModel) }
