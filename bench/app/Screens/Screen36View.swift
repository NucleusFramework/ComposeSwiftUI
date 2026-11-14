import SwiftUI

// Screen 36 — Hotel detail (pure SwiftUI twin of bench/screens/Screen36.kt).
struct Screen36View: View {
    let vm: Screen36ViewModel
    @StateObject private var state: FlowState<Screen36State>

    init(vm: Screen36ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    ZStack(alignment: .topTrailing) {
                        Rectangle().fill(LinearGradient(colors: [s.heroFrom.color, s.heroTo.color], startPoint: .top, endPoint: .bottom))
                        VStack(spacing: 8) {
                            Image(systemName: "building.2.fill").font(.system(size: 56)).foregroundStyle(Color.white.opacity(0.9))
                            Image(systemName: "water.waves").font(.title).foregroundStyle(Color.white.opacity(0.6))
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        Image(systemName: s.favorite ? "heart.fill" : "heart")
                            .font(.title3).foregroundStyle(s.favorite ? Color.red : Color.white)
                            .padding(10)
                            .background(Color.black.opacity(0.25), in: Circle())
                            .padding(14)
                    }
                    .frame(height: 200)
                    VStack(alignment: .leading, spacing: 6) {
                        Text(verbatim: s.name).font(.title).bold()
                        Label(s.location, systemImage: "mappin.and.ellipse").font(.subheadline).foregroundStyle(.secondary)
                        HStack(spacing: 2) {
                            ForEach(s.stars, id: \.id) { star in
                                Image(systemName: star.symbol).foregroundStyle(Color.yellow)
                            }
                            Text(verbatim: s.rating).fontWeight(.semibold).padding(.leading, 6)
                            Text(verbatim: "(\(s.reviewsCount))").foregroundStyle(.secondary)
                        }
                    }
                    .padding(.horizontal)
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Amenities").font(.headline)
                        LazyVGrid(columns: [GridItem(), GridItem()], alignment: .leading, spacing: 12) {
                            ForEach(s.amenities, id: \.id) { amenity in
                                Label(amenity.title, systemImage: amenity.icon)
                                    .font(.subheadline)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(10)
                                    .background(Color.blue.opacity(0.08), in: RoundedRectangle(cornerRadius: 10))
                            }
                        }
                    }
                    .padding(.horizontal)
                    VStack(alignment: .leading, spacing: 8) {
                        Text("About").font(.headline)
                        Text(verbatim: s.about).font(.body).foregroundStyle(.secondary).lineSpacing(3)
                    }
                    .padding(.horizontal)
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Guest ratings").font(.headline)
                        ForEach(s.reviews, id: \.id) { review in
                            HStack(spacing: 12) {
                                Text(verbatim: review.label).font(.subheadline).frame(width: 100, alignment: .leading)
                                ProgressView(value: review.value).tint(Color.orange)
                                Text(verbatim: review.score).font(.subheadline).monospacedDigit().foregroundStyle(.secondary)
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
            }
            .navigationTitle("Hotel")
            .safeAreaInset(edge: .bottom) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(verbatim: s.price).font(.title2).bold().monospacedDigit()
                        Text(verbatim: s.priceDetail).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button("Book now") {}
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                        .tint(Color.orange)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(.bar)
            }
        }
    }
}
