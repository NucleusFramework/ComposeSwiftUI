package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import bench.Rgb
import dev.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 06 — Shopping cart: item rows with artwork, steppers and prices, promo row, order summary, checkout button.

data class Screen06Item(
    val id: Int,
    val name: String,
    val variant: String,
    val icon: String,
    val from: Rgb,
    val to: Rgb,
    val unitCents: Int,
    val quantity: Int,
    val quantityLabel: String,
    val price: String,
    val lowStock: Boolean,
)

data class Screen06State(
    val items: List<Screen06Item>,
    val promoCode: String,
    val promoAmount: String,
    val subtotal: String,
    val shipping: String,
    val tax: String,
    val total: String,
    val itemCount: String,
    val giftWrap: Boolean,
    val delivery: String,
)

private fun screen06Money(cents: Int): String = "$" + (cents / 100).toString() + "." + (cents % 100).toString().padStart(2, '0')

private fun screen06Item(id: Int, name: String, variant: String, icon: String, from: Rgb, to: Rgb, unitCents: Int, quantity: Int, lowStock: Boolean) =
    Screen06Item(id, name, variant, icon, from, to, unitCents, quantity, "", "", lowStock).withQuantity(quantity)

private fun Screen06Item.withQuantity(q: Int) = copy(quantity = q, quantityLabel = "Qty $q", price = screen06Money(unitCents * q))

private fun Screen06State.recomputed(): Screen06State {
    val sub = items.sumOf { it.unitCents * it.quantity }
    val discount = 1000
    val shippingCents = if (sub > 10000) 0 else 599
    val taxCents = (sub - discount) * 8 / 100
    return copy(
        subtotal = screen06Money(sub),
        shipping = if (shippingCents == 0) "Free" else screen06Money(shippingCents),
        tax = screen06Money(taxCents),
        total = screen06Money(sub - discount + shippingCents + taxCents + if (giftWrap) 499 else 0),
        itemCount = "${items.sumOf { it.quantity }} items",
    )
}

class Screen06ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen06State(
            items = listOf(
                screen06Item(1, "Wireless Headphones", "Midnight · Over-ear", "headphones", Rgb(0.25, 0.25, 0.35), Rgb(0.05, 0.05, 0.1), 19900, 1, false),
                screen06Item(2, "Running Shoes", "Size 42 · Coral", "shoeprint.fill", Rgb(1.0, 0.5, 0.45), Rgb(0.9, 0.25, 0.35), 12950, 1, true),
                screen06Item(3, "Coffee Beans", "Ethiopia · 500 g", "cup.and.saucer.fill", Rgb(0.6, 0.4, 0.25), Rgb(0.4, 0.25, 0.15), 1890, 3, false),
                screen06Item(4, "Desk Lamp", "Brushed aluminium", "lamp.desk.fill", Rgb(0.95, 0.8, 0.3), Rgb(0.95, 0.55, 0.1), 7900, 1, false),
            ),
            promoCode = "AUTUMN10",
            promoAmount = "-$10.00",
            subtotal = "", shipping = "", tax = "", total = "", itemCount = "",
            giftWrap = false,
            delivery = "Arrives Tue, Nov 17",
        ).recomputed()
    )
    val state: StateFlow<Screen06State> = _state

    fun setQuantity(id: Int, value: Int) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == id) it.withQuantity(value) else it }).recomputed()
    }

    fun setGiftWrap(value: Boolean) = _state.update { it.copy(giftWrap = value).recomputed() }

    override fun mutate() = _state.update { s ->
        tick++
        s.copy(
            items = s.items.map { if (it.id == 3) it.withQuantity(1 + tick % 6) else it },
            giftWrap = tick % 2 == 1,
        ).recomputed()
    }
}

@Composable
fun Screen06(vm: Screen06ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Cart")) {
            Section(
                header = {
                    HStack {
                        Text("Bag")
                        Spacer()
                        Text(state.itemCount)
                    }
                },
                content = {
                    for (item in state.items) key(item.id) {
                        HStack(spacing = 12.0) {
                            ImageSystemName(
                                item.icon,
                                modifier = Modifier.font(Font.title2).foregroundStyle(Color.white)
                                    .frame(width = 58.0, height = 58.0)
                                    .background(
                                        LinearGradient(colors = listOf(item.from.color(), item.to.color()), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom),
                                        RoundedRectangle(cornerRadius = 12.0),
                                    ),
                            )
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                                HStack(alignment = VerticalAlignment.firstTextBaseline) {
                                    Text(item.name, modifier = Modifier.font(Font.headline).lineLimit(1))
                                    Spacer()
                                    Text(verbatim = item.price, modifier = Modifier.font(Font.subheadline.bold()).monospacedDigit())
                                }
                                Text(item.variant, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                if (item.lowStock) {
                                    Label(title = "Only 2 left", systemImage = "exclamationmark.triangle.fill", modifier = Modifier.font(Font.caption2).foregroundStyle(Color.orange))
                                }
                                Stepper(value = item.quantity, bounds = 1..9, onValueChange = { vm.setQuantity(item.id, it) }, modifier = Modifier.font(Font.subheadline)) {
                                    Text(item.quantityLabel)
                                }
                            }
                        }
                    }
                },
            )
            Section(title = "Promotions") {
                HStack {
                    ImageSystemName("tag.fill", modifier = Modifier.foregroundStyle(Color.green))
                    Text(state.promoCode, modifier = Modifier.font(Font.body.monospaced()))
                    Spacer()
                    Text(verbatim = state.promoAmount, modifier = Modifier.foregroundStyle(Color.green))
                }
                Toggle(title = "Gift wrap ($4.99)", isOn = state.giftWrap, onIsOnChange = vm::setGiftWrap)
            }
            Section(
                header = { Text("Summary") },
                footer = { Label(title = state.delivery, systemImage = "shippingbox") },
                content = {
                    LabeledContent(title = "Subtotal", value = state.subtotal)
                    LabeledContent(title = "Shipping", value = state.shipping)
                    LabeledContent(title = "Estimated tax", value = state.tax)
                    LabeledContent(title = "Total") {
                        Text(verbatim = state.total, modifier = Modifier.font(Font.title3.bold()).foregroundStyle(Color.primary))
                    }
                },
            )
            Section {
                Button(
                    action = {},
                    modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).controlSize(ControlSize.large).tint(Color.indigo),
                ) {
                    Label(title = "Check Out", systemImage = "lock.fill", modifier = Modifier.font(Font.headline).frame(maxWidth = Double.POSITIVE_INFINITY))
                }
            }
        }
    }
}

val screen06 = BenchScreen("06 · Shopping cart", ::Screen06ViewModel) { Screen06(it as Screen06ViewModel) }
