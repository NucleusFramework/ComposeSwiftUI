import SwiftUI

// Screen 16 — Kanban board (pure SwiftUI twin of bench/screens/Screen16.kt).
struct Screen16View: View {
    let vm: Screen16ViewModel
    @StateObject private var state: FlowState<Screen16State>

    init(vm: Screen16ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Label(s.boardName, systemImage: "square.grid.3x2.fill").font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                    Spacer()
                    Image(systemName: "line.3.horizontal.decrease.circle").font(.title3).foregroundStyle(Color.blue)
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(alignment: .top, spacing: 12) {
                        ForEach(s.columns, id: \.id) { column in
                            VStack(alignment: .leading, spacing: 10) {
                                HStack(spacing: 8) {
                                    Circle().fill(column.tint.color).frame(width: 8, height: 8)
                                    Text(column.title).font(.headline)
                                    Text(column.count)
                                        .font(.caption.weight(.bold))
                                        .monospacedDigit()
                                        .padding(.horizontal, 7)
                                        .padding(.vertical, 2)
                                        .background(.background, in: Capsule())
                                    Spacer()
                                    Image(systemName: "ellipsis").foregroundStyle(.secondary)
                                }
                                ForEach(column.cards, id: \.id) { card in
                                    VStack(alignment: .leading, spacing: 8) {
                                        HStack(spacing: 4) {
                                            ForEach(card.tags, id: \.label) { tag in
                                                Text(tag.label)
                                                    .font(.caption2.weight(.semibold))
                                                    .foregroundStyle(tag.tint.color)
                                                    .padding(.horizontal, 8)
                                                    .padding(.vertical, 3)
                                                    .background(tag.tint.color.opacity(0.15), in: Capsule())
                                            }
                                        }
                                        Text(card.title).font(.subheadline.weight(.semibold)).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading)
                                        HStack(spacing: 8) {
                                            ProgressView(value: card.progress).tint(column.tint.color)
                                            Label(card.checklist, systemImage: "checklist").font(.caption2).monospacedDigit().foregroundStyle(.secondary).fixedSize()
                                        }
                                        HStack(spacing: 10) {
                                            Label(card.due, systemImage: "calendar")
                                            Label(card.comments, systemImage: "bubble.left")
                                            Spacer()
                                            HStack(spacing: -8) {
                                                ForEach(card.avatars, id: \.initials) { avatar in
                                                    Text(avatar.initials)
                                                        .font(.system(size: 9, weight: .bold))
                                                        .foregroundStyle(Color.white)
                                                        .frame(width: 24, height: 24)
                                                        .background(avatar.tint.color, in: Circle())
                                                        .overlay(alignment: .center) { Circle().stroke(.background, lineWidth: 2) }
                                                }
                                            }
                                        }
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                        .labelStyle(.titleAndIcon)
                                        .lineLimit(1)
                                    }
                                    .padding(10)
                                    .background(.background, in: RoundedRectangle(cornerRadius: 12))
                                    .shadow(color: Color.black.opacity(0.06), radius: 3, y: 1)
                                }
                                Label("Add card", systemImage: "plus").font(.subheadline).foregroundStyle(.secondary)
                            }
                            .padding(12)
                            .frame(width: 260)
                            .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
                        }
                    }
                    .padding()
                }
                Spacer()
            }
            .navigationTitle("Board")
        }
    }
}
