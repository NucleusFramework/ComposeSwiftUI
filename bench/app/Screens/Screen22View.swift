import SwiftUI

// Screen 22 — Map overview (pure SwiftUI twin of bench/screens/Screen22.kt).
struct Screen22View: View {
    let vm: Screen22ViewModel
    @StateObject private var state: FlowState<Screen22State>

    init(vm: Screen22ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ZStack(alignment: .bottom) {
                canvas(s)
                card(s.place)
            }
            .navigationTitle("Explore")
        }
    }

    @ViewBuilder private func land(_ land: Screen22Land) -> some View {
        if land.oval {
            Ellipse().fill(land.tint.color)
                .frame(width: land.width, height: land.height).offset(x: land.x, y: land.y)
        } else {
            RoundedRectangle(cornerRadius: 28).fill(land.tint.color)
                .overlay(RoundedRectangle(cornerRadius: 28).stroke(Color.white.opacity(0.6), lineWidth: 2))
                .frame(width: land.width, height: land.height).offset(x: land.x, y: land.y)
        }
    }

    private func canvas(_ s: Screen22State) -> some View {
        ZStack(alignment: .topLeading) {
            ForEach(s.lands, id: \.id) { land in
                self.land(land)
            }
            ForEach(s.roads, id: \.id) { road in
                Capsule().fill(Color.white)
                    .frame(width: road.length, height: 8)
                    .rotationEffect(Angle(degrees: road.angle))
                    .offset(x: road.x, y: road.y)
            }
            ForEach(s.pins, id: \.id) { pin in
                VStack(spacing: 4) {
                    Image(systemName: pin.icon)
                        .font(.system(size: pin.selected ? 18 : 13, weight: .semibold))
                        .foregroundStyle(Color.white)
                        .frame(width: pin.selected ? 44 : 30, height: pin.selected ? 44 : 30)
                        .background(pin.tint.color, in: Circle())
                        .overlay { Circle().stroke(Color.white, lineWidth: 3) }
                        .shadow(color: Color.black.opacity(0.25), radius: 3, y: 2)
                    Text(pin.label)
                        .font(.caption2).fontWeight(.semibold)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.white, in: Capsule())
                }
                .offset(x: pin.x, y: pin.y)
            }
            HStack(spacing: 6) {
                Rectangle().fill(Color.black).frame(width: 40, height: 3)
                Text(s.scaleLabel).font(.caption2).monospacedDigit()
            }
            .padding(8)
            .background(Color.white.opacity(0.85), in: RoundedRectangle(cornerRadius: 6))
            .offset(x: 12, y: 12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color(red: 0.67, green: 0.82, blue: 0.95))
        .clipShape(Rectangle())
    }

    private func card(_ place: Screen22Place) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(place.title).font(.title3).bold()
                    Text(place.category).font(.subheadline).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "xmark.circle.fill").font(.title2).symbolRenderingMode(.hierarchical).foregroundStyle(Color.gray)
            }
            HStack(spacing: 14) {
                Label(place.rating, systemImage: "star.fill").foregroundStyle(Color.orange)
                Label(place.distance, systemImage: "location.fill")
                Label(place.eta, systemImage: "figure.walk")
            }
            Text(place.open).font(.footnote).foregroundStyle(Color.green)
            Text(place.address).font(.footnote).foregroundStyle(.secondary)
            HStack(spacing: 10) {
                Button(action: {}) {
                    Label("Directions", systemImage: "arrow.triangle.turn.up.right.diamond.fill").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                Button(action: {}) {
                    Image(systemName: "phone.fill")
                }
                .buttonStyle(.bordered)
                Button(action: {}) {
                    Image(systemName: "square.and.arrow.up")
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(16)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.15), radius: 10, y: 4)
        .padding(12)
    }
}
