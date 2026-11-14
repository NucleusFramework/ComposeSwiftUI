import SwiftUI

// Screen 42 — Task manager (pure SwiftUI twin of bench/screens/Screen42.kt).
struct Screen42View: View {
    let vm: Screen42ViewModel
    @StateObject private var state: FlowState<Screen42State>

    init(vm: Screen42ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    private func flagColor(_ priority: Int32) -> Color {
        switch priority {
        case 3: return .red
        case 2: return .orange
        case 1: return .blue
        default: return .gray
        }
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Label("Progress", systemImage: "chart.bar.fill").font(.headline).foregroundStyle(Color.indigo)
                            Spacer()
                            Text(s.progressText).font(.title2).bold().monospacedDigit()
                        }
                        ProgressView(value: s.progress).tint(Color.indigo)
                        Text(s.summary).font(.subheadline).foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
                ForEach(s.sections, id: \.id) { section in
                    Section {
                        ForEach(section.tasks, id: \.id) { task in
                            HStack(alignment: .top, spacing: 12) {
                                Image(systemName: task.done ? "checkmark.circle.fill" : "circle")
                                    .font(.title2).foregroundStyle(task.done ? Color.green : Color.gray)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(task.title)
                                        .font(.body).strikethrough(task.done, color: Color.gray)
                                        .foregroundStyle(task.done ? HierarchicalShapeStyle.secondary : HierarchicalShapeStyle.primary)
                                    Text(task.notes).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                                    HStack(spacing: 8) {
                                        Label(task.due, systemImage: "clock")
                                            .font(.caption2).foregroundStyle(task.overdue ? Color.red : Color.secondary)
                                        Text(task.tag)
                                            .font(.caption2).fontWeight(.semibold)
                                            .foregroundStyle(task.tagTint.color)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(task.tagTint.color.opacity(0.15), in: Capsule())
                                    }
                                }
                                Spacer()
                                if task.priority > 0 {
                                    Image(systemName: "flag.fill").font(.caption).foregroundStyle(flagColor(task.priority))
                                }
                            }
                        }
                    } header: {
                        HStack {
                            Label(section.title, systemImage: section.icon)
                            Spacer()
                            Text(section.count).font(.caption).monospacedDigit()
                        }
                    }
                }
                Section {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(Color.orange)
                        Text(s.footer).font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Tasks")
        }
    }
}
