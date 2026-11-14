import SwiftUI

// Screen 44 — Job listings (pure SwiftUI twin of bench/screens/Screen44.kt).
struct Screen44View: View {
    let vm: Screen44ViewModel
    @StateObject private var state: FlowState<Screen44State>

    init(vm: Screen44ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(s.chips, id: \.id) { chip in
                                Label(chip.title, systemImage: chip.icon)
                                    .font(.subheadline).fontWeight(.medium)
                                    .foregroundStyle(chip.selected ? Color.white : Color.primary)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 7)
                                    .background(chip.selected ? Color.blue : Color.gray.opacity(0.15), in: Capsule())
                            }
                        }
                    }
                    HStack {
                        Text(s.resultCount).font(.footnote).foregroundStyle(.secondary)
                        Spacer()
                        Label(s.saved, systemImage: "bookmark.fill").font(.footnote).foregroundStyle(Color.blue)
                    }
                }
                ForEach(s.jobs, id: \.id) { job in
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(alignment: .top, spacing: 12) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 12).fill(job.logo.color.opacity(0.18))
                                Image(systemName: job.logoIcon).font(.title3).foregroundStyle(job.logo.color)
                            }
                            .frame(width: 48, height: 48)
                            VStack(alignment: .leading, spacing: 3) {
                                HStack(spacing: 6) {
                                    Text(job.company).font(.subheadline).foregroundStyle(.secondary)
                                    if job.featured {
                                        Text("FEATURED")
                                            .font(.system(size: 9, weight: .bold))
                                            .foregroundStyle(Color.orange)
                                            .padding(.horizontal, 5)
                                            .padding(.vertical, 2)
                                            .background(Color.orange.opacity(0.15), in: Capsule())
                                    }
                                }
                                Text(job.title).font(.headline)
                                Label(job.location, systemImage: "mappin").font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: job.bookmarked ? "bookmark.fill" : "bookmark")
                                .font(.title3).foregroundStyle(job.bookmarked ? Color.blue : Color.gray)
                        }
                        HStack(spacing: 6) {
                            ForEach(job.tags, id: \.id) { tag in
                                Text(tag.text)
                                    .font(.caption).foregroundStyle(job.logo.color)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 3)
                                    .background(job.logo.color.opacity(0.12), in: Capsule())
                            }
                        }
                        HStack {
                            Text(job.salary).font(.subheadline).fontWeight(.semibold).monospacedDigit()
                            Spacer()
                            Text(job.applicants).font(.caption).foregroundStyle(.secondary)
                            Text("·").foregroundStyle(.tertiary)
                            Text(job.posted).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 6)
                }
            }
            .listStyle(.plain)
            .navigationTitle("Jobs")
        }
    }
}
