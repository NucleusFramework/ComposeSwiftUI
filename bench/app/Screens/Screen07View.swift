import SwiftUI

// Screen 07 — Calendar agenda (pure SwiftUI twin of bench/screens/Screen07.kt).
struct Screen07View: View {
    let vm: Screen07ViewModel
    @StateObject private var state: FlowState<Screen07State>

    init(vm: Screen07ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    private func dayColor(_ kind: Int32) -> Color {
        switch kind {
        case 0: return Color.secondary
        case 2: return Color.red
        case 3: return Color.white
        default: return Color.primary
        }
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Text(s.month).font(.title2.bold())
                        Spacer()
                        HStack(spacing: 18) {
                            Image(systemName: "chevron.left")
                            Image(systemName: "chevron.right")
                        }
                        .font(.headline)
                        .foregroundStyle(Color.red)
                    }
                    Grid(horizontalSpacing: 4, verticalSpacing: 6) {
                        GridRow {
                            ForEach(Array(s.weekdays.enumerated()), id: \.offset) { _, label in
                                Text(label).font(.caption.weight(.semibold)).foregroundStyle(.secondary).frame(maxWidth: .infinity)
                            }
                        }
                        ForEach(s.weeks, id: \.id) { week in
                            GridRow {
                                ForEach(week.days, id: \.id) { day in
                                    Text(day.label)
                                        .font(.callout)
                                        .fontWeight(day.kind >= 2 ? .bold : .regular)
                                        .foregroundStyle(dayColor(day.kind))
                                        .frame(width: 36, height: 36)
                                        .background(day.kind == 3 ? Color.red : Color.clear, in: Circle())
                                        .overlay(alignment: .bottom) {
                                            Circle()
                                                .fill(day.hasEvent ? Color.gray : Color.clear)
                                                .frame(width: 4, height: 4)
                                                .offset(y: 5)
                                        }
                                        .frame(maxWidth: .infinity)
                                }
                            }
                        }
                    }
                    Divider()
                    Text(s.selectedTitle).font(.headline)
                    ForEach(s.events, id: \.id) { event in
                        HStack(spacing: 12) {
                            RoundedRectangle(cornerRadius: 2).fill(event.tint.color).frame(width: 4)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(event.title).font(.subheadline.weight(.semibold))
                                Label(event.time, systemImage: "clock").font(.caption).foregroundStyle(.secondary)
                                Label(event.location, systemImage: "mappin.and.ellipse").font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 6) {
                                Text(event.duration).font(.caption.bold()).foregroundStyle(event.tint.color)
                                HStack(spacing: 2) {
                                    Image(systemName: "person.2.fill")
                                    Text(event.attendees)
                                }
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            }
                        }
                        .padding(10)
                        .background(event.tint.color.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
                    }
                }
                .padding()
            }
            .navigationTitle("Calendar")
            .toolbarTitleDisplayMode(.inline)
        }
    }
}
