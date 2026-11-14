import SwiftUI

// Screen 21 — Notification center (pure SwiftUI twin of bench/screens/Screen21.kt).
struct Screen21View: View {
    let vm: Screen21ViewModel
    @StateObject private var state: FlowState<Screen21State>

    init(vm: Screen21ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(spacing: 10) {
                    VStack(spacing: 0) {
                        Text(s.date).font(.headline).opacity(0.9)
                        Text(s.clock).font(.system(size: 72, weight: .semibold, design: .rounded)).monospacedDigit()
                    }
                    .foregroundStyle(Color.white)
                    .padding(.vertical, 12)
                    HStack(spacing: 8) {
                        Image(systemName: "moon.fill").foregroundStyle(Color.indigo)
                        Text(s.focus).font(.subheadline.weight(.medium))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(.ultraThinMaterial, in: Capsule())
                    HStack {
                        Text("Notification Centre").font(.title3.bold()).foregroundStyle(Color.white)
                        Spacer()
                        Image(systemName: "xmark")
                            .font(.caption.weight(.bold))
                            .frame(width: 28, height: 28)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                    .padding(.top, 6)
                    ForEach(s.groups, id: \.id) { group in
                        VStack(spacing: 0) {
                            VStack(alignment: .leading, spacing: 10) {
                                HStack(spacing: 8) {
                                    Image(systemName: group.icon)
                                        .font(.system(size: 11, weight: .semibold))
                                        .foregroundStyle(Color.white)
                                        .frame(width: 22, height: 22)
                                        .background(group.tint.color, in: RoundedRectangle(cornerRadius: 6))
                                    Text(group.app).font(.caption.weight(.semibold)).textCase(.uppercase).foregroundStyle(.secondary)
                                    Spacer()
                                }
                                ForEach(Array(group.notifications.enumerated()), id: \.element.id) { index, item in
                                    if index > 0 {
                                        Divider()
                                    }
                                    VStack(alignment: .leading, spacing: 2) {
                                        HStack(alignment: .firstTextBaseline) {
                                            Text(item.title).font(.subheadline.weight(.semibold))
                                            Spacer()
                                            Text(item.time).font(.caption).foregroundStyle(.secondary)
                                        }
                                        Text(item.text).font(.subheadline).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading)
                                    }
                                }
                                if !group.moreLabel.isEmpty {
                                    Text(group.moreLabel).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            .padding(14)
                            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22))
                            if group.stacked {
                                UnevenRoundedRectangle(bottomLeadingRadius: 14, bottomTrailingRadius: 14)
                                    .fill(.thinMaterial)
                                    .frame(height: 8)
                                    .padding(.horizontal, 14)
                                    .opacity(0.8)
                            }
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 20)
            }
            .background(
                LinearGradient(
                    colors: [Color(red: 0.25, green: 0.2, blue: 0.55), Color(red: 0.85, green: 0.4, blue: 0.5), Color(red: 1.0, green: 0.7, blue: 0.45)],
                    startPoint: .top,
                    endPoint: .bottom
                ),
                ignoresSafeAreaEdges: .all
            )
            .navigationTitle("Notifications")
        }
    }
}
