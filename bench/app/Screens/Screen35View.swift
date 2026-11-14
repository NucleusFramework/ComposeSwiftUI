import SwiftUI

// Screen 35 — Online course (pure SwiftUI twin of bench/screens/Screen35.kt).
struct Screen35View: View {
    let vm: Screen35ViewModel
    @StateObject private var state: FlowState<Screen35State>

    init(vm: Screen35ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 14) {
                        Image(systemName: "swift")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(Color.white)
                            .frame(width: 60, height: 60)
                            .background(
                                LinearGradient(colors: [s.accent.color, Color.pink], startPoint: .top, endPoint: .bottom),
                                in: RoundedRectangle(cornerRadius: 14)
                            )
                        VStack(alignment: .leading, spacing: 4) {
                            Text(verbatim: s.course).font(.headline)
                            Text(verbatim: s.instructor).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        ProgressView(value: s.progress).tint(s.accent.color)
                        Text(verbatim: s.progressLabel).font(.caption).foregroundStyle(.secondary)
                    }
                }
                Section("Curriculum") {
                    ForEach(s.modules, id: \.id) { module in
                        DisclosureGroup(isExpanded: Binding(get: { module.expanded }, set: { vm.setExpanded(id: module.id, value: $0) })) {
                            ForEach(module.lessons, id: \.id) { lesson in
                                HStack(spacing: 12) {
                                    Image(systemName: lesson.kind).foregroundStyle(.secondary).frame(width: 22)
                                    VStack(alignment: .leading, spacing: 1) {
                                        Text(verbatim: lesson.title).font(.subheadline).foregroundStyle(lesson.locked ? HierarchicalShapeStyle.secondary : HierarchicalShapeStyle.primary)
                                        Text(verbatim: lesson.duration).font(.caption2).foregroundStyle(.tertiary)
                                    }
                                    Spacer()
                                    Image(systemName: lesson.locked ? "lock.fill" : (lesson.done ? "checkmark.circle.fill" : "circle"))
                                        .foregroundStyle(lesson.locked ? Color.gray : (lesson.done ? Color.green : Color.gray.opacity(0.4)))
                                }
                            }
                        } label: {
                            HStack(spacing: 10) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(verbatim: module.title).font(.body).fontWeight(.semibold)
                                    ProgressView(value: module.progress).tint(s.accent.color).frame(width: 120)
                                }
                                Spacer()
                                Text(verbatim: module.count).font(.caption).monospacedDigit().foregroundStyle(.secondary).padding(.horizontal, 8).padding(.vertical, 3).background(Color.gray.opacity(0.15), in: Capsule())
                            }
                        }
                    }
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Course")
        }
    }
}
