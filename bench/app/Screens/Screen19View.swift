import SwiftUI

// Screen 19 — Travel booking (pure SwiftUI twin of bench/screens/Screen19.kt).
struct Screen19View: View {
    let vm: Screen19ViewModel
    @StateObject private var state: FlowState<Screen19State>

    init(vm: Screen19ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Picker("Trip type", selection: Binding(get: { s.tripType }, set: { vm.setTripType(value: $0) })) {
                        ForEach(s.tripTypes, id: \.self) { type in
                            Text(type).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                    VStack(spacing: 14) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("From").font(.caption).foregroundStyle(.secondary)
                                Text(s.fromCode).font(.system(size: 30, weight: .bold))
                                Text(s.fromCity).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "arrow.left.arrow.right")
                                .font(.headline)
                                .foregroundStyle(Color.white)
                                .frame(width: 40, height: 40)
                                .background(Color.blue, in: Circle())
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("To").font(.caption).foregroundStyle(.secondary)
                                Text(s.toCode).font(.system(size: 30, weight: .bold))
                                Text(s.toCity).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                        Divider()
                        HStack {
                            Label(s.dates, systemImage: "calendar")
                            Spacer()
                            Label(s.passengers, systemImage: "person.2.fill")
                        }
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    }
                    .padding()
                    .background(.background, in: RoundedRectangle(cornerRadius: 18))
                    Text(s.resultsLabel).font(.headline)
                    ForEach(s.flights, id: \.id) { flight in
                        VStack(spacing: 14) {
                            HStack(spacing: 10) {
                                Text(flight.code)
                                    .font(.caption.weight(.heavy))
                                    .foregroundStyle(Color.white)
                                    .frame(width: 32, height: 32)
                                    .background(flight.tint.color, in: RoundedRectangle(cornerRadius: 8))
                                Text(flight.airline).font(.subheadline.weight(.semibold))
                                Spacer()
                                Text(flight.badge)
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(flight.tint.color)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(flight.tint.color.opacity(0.12), in: Capsule())
                            }
                            HStack(spacing: 10) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(flight.departTime).font(.title2.bold()).monospacedDigit()
                                    Text(s.fromCode).font(.caption).foregroundStyle(.secondary)
                                }
                                VStack(spacing: 4) {
                                    Text(flight.duration).font(.caption2).foregroundStyle(.secondary)
                                    ZStack {
                                        Rectangle()
                                            .stroke(Color.gray.opacity(0.6), style: StrokeStyle(lineWidth: 1, miterLimit: 10, dash: [4, 3], dashPhase: 0))
                                            .frame(height: 1)
                                        Image(systemName: "airplane")
                                            .font(.caption)
                                            .foregroundStyle(flight.tint.color)
                                            .padding(.horizontal, 4)
                                            .background(.background)
                                    }
                                    Text(flight.stops).font(.caption2).foregroundStyle(flight.stops == "Nonstop" ? Color.green : Color.orange)
                                }
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(flight.arriveTime).font(.title2.bold()).monospacedDigit()
                                    Text(s.toCode).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            Divider()
                            HStack {
                                Label(flight.baggage, systemImage: "suitcase.rolling.fill").font(.caption).foregroundStyle(.secondary)
                                Spacer()
                                Text("per person").font(.caption2).foregroundStyle(.secondary)
                                Text(flight.price).font(.title3.bold()).monospacedDigit().foregroundStyle(Color.blue)
                            }
                        }
                        .padding()
                        .background(.background, in: RoundedRectangle(cornerRadius: 18))
                        .overlay(alignment: .center) {
                            RoundedRectangle(cornerRadius: 18).stroke(flight.isBest ? Color.blue : Color.clear, lineWidth: 2)
                        }
                    }
                }
                .padding()
            }
            .background(Color.gray.opacity(0.1))
            .navigationTitle("Flights")
        }
    }
}
