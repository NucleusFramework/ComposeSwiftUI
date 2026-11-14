import SwiftUI

// Screen 27 — Order tracking (pure SwiftUI twin of bench/screens/Screen27.kt).
struct Screen27View: View {
    let vm: Screen27ViewModel
    @StateObject private var state: FlowState<Screen27State>

    init(vm: Screen27ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    HStack(spacing: 14) {
                        Image(systemName: "shippingbox.fill")
                            .font(.title2)
                            .foregroundStyle(Color.white)
                            .frame(width: 52, height: 52)
                            .background(
                                LinearGradient(colors: [Color.orange, Color.brown], startPoint: .topLeading, endPoint: .bottomTrailing),
                                in: RoundedRectangle(cornerRadius: 14)
                            )
                        VStack(alignment: .leading, spacing: 3) {
                            Text(s.order).font(.headline)
                            Text(s.eta).font(.subheadline).foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(s.status)
                                .font(.caption.bold())
                                .foregroundStyle(s.statusTint.color)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(s.statusTint.color.opacity(0.12), in: Capsule())
                            Spacer()
                            Text(s.progressLabel).font(.caption).foregroundStyle(.secondary)
                        }
                        ProgressView(value: s.progress).tint(s.statusTint.color)
                    }
                    timeline(s.steps)
                    courier(s)
                }
                .padding()
            }
            .navigationTitle("Tracking")
        }
    }

    private func timeline(_ steps: [Screen27Step]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(steps, id: \.id) { step in
                HStack(alignment: .top, spacing: 14) {
                    VStack(spacing: 0) {
                        ZStack {
                            Circle().fill(step.circleTint.color).frame(width: 26, height: 26)
                            if step.isCurrent {
                                Circle().stroke(step.circleTint.color.opacity(0.3), lineWidth: 4).frame(width: 34, height: 34)
                            }
                            Image(systemName: step.icon).font(.system(size: 11, weight: .bold)).foregroundStyle(step.iconTint.color)
                        }
                        if !step.isLast {
                            Rectangle().fill(step.lineTint.color).frame(width: 3, height: 34)
                        }
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(step.title).font(.subheadline.weight(step.isCurrent ? .bold : .medium))
                        Text(step.detail).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(step.time).font(.caption2.monospacedDigit()).foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .background(Color.gray.opacity(0.08), in: RoundedRectangle(cornerRadius: 16))
    }

    private func courier(_ s: Screen27State) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 44)).symbolRenderingMode(.hierarchical).foregroundStyle(Color.indigo)
            VStack(alignment: .leading, spacing: 3) {
                Text(s.courier).font(.headline)
                Text(s.vehicle).font(.caption).foregroundStyle(.secondary)
                HStack(spacing: 3) {
                    Image(systemName: "star.fill").foregroundStyle(Color.yellow)
                    Text(s.rating).monospacedDigit()
                }
            }
            Spacer()
            Button(action: {}) {
                Image(systemName: "message.fill")
            }
            .buttonStyle(.bordered).buttonBorderShape(.circle)
            Button(action: {}) {
                Image(systemName: "phone.fill")
            }
            .buttonStyle(.borderedProminent).buttonBorderShape(.circle).tint(Color.green)
        }
    }
}
