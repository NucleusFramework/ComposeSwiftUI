import SwiftUI

// Screen 06 — Shopping cart (pure SwiftUI twin of bench/screens/Screen06.kt).
struct Screen06View: View {
    let vm: Screen06ViewModel
    @StateObject private var state: FlowState<Screen06State>

    init(vm: Screen06ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    ForEach(s.items, id: \.id) { item in
                        HStack(spacing: 12) {
                            Image(systemName: item.icon)
                                .font(.title2)
                                .foregroundStyle(Color.white)
                                .frame(width: 58, height: 58)
                                .background(
                                    LinearGradient(colors: [item.from.color, item.to.color], startPoint: .top, endPoint: .bottom),
                                    in: RoundedRectangle(cornerRadius: 12)
                                )
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(alignment: .firstTextBaseline) {
                                    Text(item.name).font(.headline).lineLimit(1)
                                    Spacer()
                                    Text(verbatim: item.price).font(.subheadline.bold()).monospacedDigit()
                                }
                                Text(item.variant).font(.caption).foregroundStyle(.secondary)
                                if item.lowStock {
                                    Label("Only 2 left", systemImage: "exclamationmark.triangle.fill").font(.caption2).foregroundStyle(Color.orange)
                                }
                                Stepper(value: Binding(get: { Int(item.quantity) }, set: { vm.setQuantity(id: item.id, value: Int32($0)) }), in: 1...9) {
                                    Text(item.quantityLabel)
                                }
                                .font(.subheadline)
                            }
                        }
                    }
                } header: {
                    HStack {
                        Text("Bag")
                        Spacer()
                        Text(s.itemCount)
                    }
                }
                Section("Promotions") {
                    HStack {
                        Image(systemName: "tag.fill").foregroundStyle(Color.green)
                        Text(s.promoCode).font(.body.monospaced())
                        Spacer()
                        Text(verbatim: s.promoAmount).foregroundStyle(Color.green)
                    }
                    Toggle("Gift wrap ($4.99)", isOn: Binding(get: { s.giftWrap }, set: { vm.setGiftWrap(value: $0) }))
                }
                Section {
                    LabeledContent("Subtotal", value: s.subtotal)
                    LabeledContent("Shipping", value: s.shipping)
                    LabeledContent("Estimated tax", value: s.tax)
                    LabeledContent("Total") {
                        Text(verbatim: s.total).font(.title3.bold()).foregroundStyle(Color.primary)
                    }
                } header: {
                    Text("Summary")
                } footer: {
                    Label(s.delivery, systemImage: "shippingbox")
                }
                Section {
                    Button(action: {}) {
                        Label("Check Out", systemImage: "lock.fill").font(.headline).frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .tint(Color.indigo)
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Cart")
        }
    }
}
