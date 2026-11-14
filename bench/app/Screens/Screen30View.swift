import SwiftUI

// Screen 30 — Pull request review (pure SwiftUI twin of bench/screens/Screen30.kt).
struct Screen30View: View {
    let vm: Screen30ViewModel
    @StateObject private var state: FlowState<Screen30State>

    init(vm: Screen30ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 8) {
                        Label("Open", systemImage: "arrow.triangle.pull")
                            .font(.caption.bold())
                            .foregroundStyle(Color.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color.green, in: Capsule())
                        Text(verbatim: s.number).font(.subheadline.monospacedDigit()).foregroundStyle(.secondary)
                        Spacer()
                        Text(verbatim: s.additions).font(.caption.monospaced()).foregroundStyle(Color.green)
                        Text(verbatim: s.deletions).font(.caption.monospaced()).foregroundStyle(Color.red)
                        Text(verbatim: s.filesChanged).font(.caption).foregroundStyle(.secondary)
                    }
                    Text(s.title).font(.title3.bold())
                    Text(verbatim: s.branchInfo).font(.footnote).foregroundStyle(.secondary)
                    ForEach(s.files, id: \.id) { file in
                        diff(file)
                    }
                    summary(s)
                }
                .padding()
            }
            .navigationTitle("Review")
        }
    }

    private func diff(_ file: Screen30File) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                Image(systemName: file.icon).foregroundStyle(.secondary)
                Text(verbatim: file.path).font(.system(.footnote, design: .monospaced)).bold().lineLimit(1).truncationMode(.middle)
                Spacer()
                Text(verbatim: file.stats).font(.caption.monospaced()).foregroundStyle(.secondary)
            }
            .padding(10)
            .background(Color.gray.opacity(0.12))
            ForEach(file.lines, id: \.id) { line in
                HStack(spacing: 0) {
                    Text(verbatim: line.oldNo).foregroundStyle(.tertiary).frame(width: 26, alignment: .trailing)
                    Text(verbatim: line.newNo).foregroundStyle(.tertiary).frame(width: 26, alignment: .trailing)
                    Text(verbatim: line.marker).bold().frame(width: 18)
                    Text(verbatim: line.code).lineLimit(1)
                    Spacer(minLength: 0)
                }
                .font(.system(size: 11, weight: .regular, design: .monospaced))
                .foregroundStyle(line.fg.color)
                .padding(.vertical, 3)
                .background(line.bg.color)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay { RoundedRectangle(cornerRadius: 10).stroke(Color.gray.opacity(0.3), lineWidth: 1) }
    }

    private func summary(_ s: Screen30State) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label(s.approvals, systemImage: "checkmark.seal.fill").font(.subheadline.weight(.semibold)).foregroundStyle(Color.green)
                Spacer()
                HStack(spacing: -8) {
                    ForEach(s.reviewers, id: \.id) { r in
                        Text(r.initials)
                            .font(.caption2.bold())
                            .foregroundStyle(Color.white)
                            .frame(width: 28, height: 28)
                            .background(r.tint.color, in: Circle())
                            .overlay { Circle().stroke(Color.white, lineWidth: 2) }
                    }
                }
            }
            Label(s.changesRequested, systemImage: "exclamationmark.bubble.fill").font(.subheadline).foregroundStyle(s.mergeable ? Color.secondary : Color.orange)
            Label(s.checks, systemImage: "checkmark.circle.fill").font(.subheadline).foregroundStyle(.secondary)
            Button(action: {}) {
                Label("Merge pull request", systemImage: "arrow.triangle.merge").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent).tint(Color.green).controlSize(.large).disabled(!s.mergeable)
        }
        .padding(14)
        .background(Color.gray.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
    }
}
