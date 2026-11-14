import SwiftUI

// Screen 24 — Pricing tiers (pure SwiftUI twin of bench/screens/Screen24.kt).
struct Screen24View: View {
    let vm: Screen24ViewModel
    @StateObject private var state: FlowState<Screen24State>

    init(vm: Screen24ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 6) {
                        Image(systemName: "crown.fill")
                            .font(.system(size: 34)).symbolRenderingMode(.multicolor)
                        Text("Choose your plan").font(.title2.bold())
                        Text("Upgrade or downgrade at any time. All plans include end-to-end encryption.")
                            .font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    }
                    Picker("Billing", selection: Binding(get: { s.billing }, set: { vm.setBilling(value: $0) })) {
                        Text("Monthly").tag(Int32(0))
                        Text("Yearly").tag(Int32(1))
                    }
                    .pickerStyle(.segmented)
                    Label(s.savings, systemImage: "gift.fill").font(.caption.bold()).foregroundStyle(Color.orange)
                    ForEach(s.plans, id: \.id) { plan in
                        card(plan)
                    }
                    Text(s.footnote).font(.footnote).foregroundStyle(.tertiary)
                }
                .padding()
            }
            .navigationTitle("Plans")
        }
    }

    private func card(_ plan: Screen24Plan) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: plan.icon)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(Color.white)
                    .frame(width: 34, height: 34)
                    .background(plan.tint.color, in: RoundedRectangle(cornerRadius: 9))
                Text(plan.name).font(.title3.bold())
                Spacer()
                if plan.highlighted {
                    Text("POPULAR")
                        .font(.caption2).fontWeight(.heavy).tracking(0.8)
                        .foregroundStyle(Color.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(plan.tint.color, in: Capsule())
                }
            }
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(plan.price).font(.system(size: 34, weight: .bold, design: .rounded))
                Text(plan.period).font(.subheadline).foregroundStyle(.secondary)
            }
            Text(plan.tagline).font(.subheadline).foregroundStyle(.secondary)
            Divider()
            VStack(alignment: .leading, spacing: 8) {
                ForEach(plan.features, id: \.id) { feature in
                    HStack(spacing: 8) {
                        Image(systemName: feature.included ? "checkmark.circle.fill" : "xmark.circle")
                            .foregroundStyle(feature.included ? plan.tint.color : Color.gray.opacity(0.5))
                        Text(feature.title)
                            .font(.subheadline)
                            .foregroundStyle(feature.included ? Color.primary : Color.secondary)
                            .strikethrough(!feature.included, color: Color.gray)
                    }
                }
            }
            if plan.highlighted {
                Button(action: {}) {
                    Text(plan.cta).fontWeight(.semibold).frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent).tint(plan.tint.color).controlSize(.large)
            } else {
                Button(action: {}) {
                    Text(plan.cta).fontWeight(.semibold).frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered).tint(plan.tint.color).controlSize(.large)
            }
        }
        .padding(18)
        .background(plan.highlighted ? plan.tint.color.opacity(0.06) : Color.gray.opacity(0.06), in: RoundedRectangle(cornerRadius: 20))
        .overlay {
            RoundedRectangle(cornerRadius: 20)
                .stroke(plan.highlighted ? plan.tint.color : Color.gray.opacity(0.25), lineWidth: plan.highlighted ? 2.5 : 1)
        }
    }
}
