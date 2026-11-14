import SwiftUI

// Screen 43 — Event tickets (pure SwiftUI twin of bench/screens/Screen43.kt).
struct Screen43View: View {
    let vm: Screen43ViewModel
    @StateObject private var state: FlowState<Screen43State>

    init(vm: Screen43ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(s.subtitle).font(.subheadline).foregroundStyle(.secondary)
                    ForEach(s.tickets, id: \.id) { ticket in
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 14) {
                                VStack(spacing: 0) {
                                    Text(ticket.month).font(.caption2).fontWeight(.bold)
                                    Text(ticket.day).font(.title).fontWeight(.heavy).monospacedDigit()
                                    Text(ticket.weekday).font(.caption2).opacity(0.85)
                                }
                                .foregroundStyle(Color.white).frame(width: 58, height: 70)
                                .background(ticket.tint.color, in: RoundedRectangle(cornerRadius: 12))
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(ticket.title).font(.headline).lineLimit(1)
                                    Label(ticket.venue, systemImage: "mappin.and.ellipse").font(.caption).foregroundStyle(.secondary)
                                    Label(ticket.time, systemImage: "clock").font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: ticket.icon).font(.title2).foregroundStyle(ticket.tint.color)
                            }
                            Divider()
                            HStack(alignment: .bottom) {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack(spacing: 14) {
                                        ForEach(ticket.seats, id: \.label) { seat in
                                            VStack(alignment: .leading, spacing: 2) {
                                                Text(seat.label).font(.system(size: 9, weight: .semibold)).foregroundStyle(.secondary)
                                                Text(seat.value).font(.subheadline).bold().monospacedDigit()
                                            }
                                        }
                                    }
                                    Label(ticket.status, systemImage: ticket.checkedIn ? "checkmark.seal.fill" : "qrcode.viewfinder")
                                        .font(.caption).fontWeight(.semibold)
                                        .foregroundStyle(ticket.checkedIn ? Color.green : ticket.tint.color)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background((ticket.checkedIn ? Color.green : ticket.tint.color).opacity(0.15), in: Capsule())
                                }
                                Spacer()
                                Grid(horizontalSpacing: 1, verticalSpacing: 1) {
                                    ForEach(ticket.qr, id: \.id) { row in
                                        GridRow {
                                            ForEach(row.cells, id: \.id) { cell in
                                                Rectangle().fill(cell.on ? Color.black : Color.white).frame(width: 7, height: 7)
                                            }
                                        }
                                    }
                                }
                                .padding(6).background(Color.white, in: RoundedRectangle(cornerRadius: 6))
                            }
                        }
                        .padding(14)
                        .background(ticket.tint.color.opacity(0.08), in: RoundedRectangle(cornerRadius: 18))
                        .overlay {
                            RoundedRectangle(cornerRadius: 18).stroke(ticket.tint.color.opacity(0.3), lineWidth: 1)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Tickets")
        }
    }
}
