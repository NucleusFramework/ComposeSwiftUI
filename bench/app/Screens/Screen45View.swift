import SwiftUI

// Screen 45 — Real-estate listing (pure SwiftUI twin of bench/screens/Screen45.kt).
struct Screen45View: View {
    let vm: Screen45ViewModel
    @StateObject private var state: FlowState<Screen45State>

    init(vm: Screen45ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    ZStack(alignment: .bottomLeading) {
                        LinearGradient(colors: s.gradient.map { $0.color }, startPoint: .topLeading, endPoint: .bottomTrailing)
                        Image(systemName: "house.lodge.fill")
                            .font(.system(size: 72)).foregroundStyle(Color.white.opacity(0.85))
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                        HStack {
                            Text(s.status)
                                .font(.caption).fontWeight(.bold).foregroundStyle(Color.white)
                                .padding(.horizontal, 10).padding(.vertical, 5)
                                .background(Color.green, in: Capsule())
                            Spacer()
                            Label(s.photoCount, systemImage: "photo.on.rectangle")
                                .font(.caption).monospacedDigit().foregroundStyle(Color.white)
                                .padding(.horizontal, 10).padding(.vertical, 5)
                                .background(Color.black.opacity(0.35), in: Capsule())
                        }
                        .padding(12)
                    }
                    .frame(height: 210).clipShape(RoundedRectangle(cornerRadius: 20))
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 8) {
                                Text(s.price).font(.title).fontWeight(.bold).monospacedDigit()
                                if s.priceReduced {
                                    Label("Reduced", systemImage: "arrow.down").font(.caption).fontWeight(.semibold).foregroundStyle(Color.red)
                                }
                            }
                            Text(s.address).font(.headline)
                            Text(s.city).font(.subheadline).foregroundStyle(.secondary)
                            Text(s.priceNote).font(.caption).foregroundStyle(.tertiary)
                        }
                        Spacer()
                        Image(systemName: s.favorite ? "heart.fill" : "heart")
                            .font(.title2).foregroundStyle(s.favorite ? Color.pink : Color.gray)
                            .frame(width: 44, height: 44)
                            .background(Color.gray.opacity(0.12), in: Circle())
                    }
                    HStack(spacing: 0) {
                        ForEach(s.specs, id: \.id) { spec in
                            VStack(spacing: 4) {
                                Image(systemName: spec.icon).font(.title3).foregroundStyle(Color.indigo)
                                Text(spec.value).font(.headline).monospacedDigit()
                                Text(spec.label).font(.caption2).foregroundStyle(.secondary).textCase(.uppercase)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 12)
                    .background(Color.indigo.opacity(0.07), in: RoundedRectangle(cornerRadius: 14))
                    VStack(alignment: .leading, spacing: 8) {
                        Text("About this home").font(.title3).bold()
                        Text(s.about).font(.subheadline).foregroundStyle(.secondary).lineSpacing(3)
                    }
                    Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 10) {
                        ForEach(0..<((s.features.count + 1) / 2), id: \.self) { rowIndex in
                            GridRow {
                                ForEach(Array(s.features.dropFirst(rowIndex * 2).prefix(2)), id: \.id) { feature in
                                    Label(feature.title, systemImage: feature.icon).font(.subheadline).foregroundStyle(Color.primary)
                                }
                            }
                        }
                    }
                    HStack(spacing: 12) {
                        Text(s.agentInitials)
                            .font(.headline).foregroundStyle(Color.white)
                            .frame(width: 48, height: 48)
                            .background(LinearGradient(colors: [Color.teal, Color.blue], startPoint: .top, endPoint: .bottom), in: Circle())
                        VStack(alignment: .leading, spacing: 2) {
                            Text(s.agentName).font(.headline)
                            Text(s.agency).font(.caption).foregroundStyle(.secondary)
                            Label(s.rating, systemImage: "star.fill").font(.caption2).foregroundStyle(Color.orange)
                        }
                        Spacer()
                        Image(systemName: "phone.fill").foregroundStyle(Color.white).frame(width: 36, height: 36).background(Color.green, in: Circle())
                        Image(systemName: "message.fill").foregroundStyle(Color.white).frame(width: 36, height: 36).background(Color.blue, in: Circle())
                    }
                    .padding(14).background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 16))
                    Label(s.views, systemImage: "eye").font(.footnote).monospacedDigit().foregroundStyle(.secondary)
                }
                .padding()
            }
            .navigationTitle("Listing")
        }
    }
}
