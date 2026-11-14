import SwiftUI

// Screen 40 — Restaurant menu (pure SwiftUI twin of bench/screens/Screen40.kt).
struct Screen40View: View {
    let vm: Screen40ViewModel
    @StateObject private var state: FlowState<Screen40State>

    init(vm: Screen40ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 14) {
                        Image(systemName: "fork.knife.circle.fill")
                            .font(.system(size: 44)).foregroundStyle(Color.white, Color.red).symbolRenderingMode(.palette)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(verbatim: s.restaurant).font(.title2).bold()
                            Text(verbatim: s.cuisine).font(.subheadline).foregroundStyle(.secondary)
                            HStack(spacing: 10) {
                                Label(s.rating, systemImage: "star.fill")
                                Label(s.eta, systemImage: "bicycle")
                                Label("Open", systemImage: "clock")
                            }
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 6)
                }
                ForEach(s.courses, id: \.id) { course in
                    Section {
                        ForEach(course.dishes, id: \.id) { dish in
                            HStack(alignment: .top, spacing: 12) {
                                VStack(alignment: .leading, spacing: 5) {
                                    HStack(spacing: 6) {
                                        Text(verbatim: dish.name).font(.headline)
                                        if dish.popular {
                                            Image(systemName: "flame.fill").font(.caption).foregroundStyle(Color.orange)
                                        }
                                    }
                                    Text(verbatim: dish.blurb).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                                    HStack(spacing: 6) {
                                        Text(verbatim: dish.price).font(.subheadline).fontWeight(.semibold).monospacedDigit()
                                        ForEach(dish.tags, id: \.id) { tag in
                                            Text(verbatim: tag.label)
                                                .font(.caption2).fontWeight(.medium).foregroundStyle(tag.tint.color)
                                                .padding(.horizontal, 7).padding(.vertical, 2)
                                                .background(tag.tint.color.opacity(0.14), in: Capsule())
                                        }
                                    }
                                }
                                Spacer()
                                ZStack(alignment: .topTrailing) {
                                    Image(systemName: dish.icon)
                                        .font(.title2).foregroundStyle(Color.red.opacity(0.7))
                                        .frame(width: 64, height: 64)
                                        .background(Color.orange.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
                                    if dish.quantity > 0 {
                                        Text(verbatim: dish.quantityLabel).font(.caption2).bold().foregroundStyle(Color.white).padding(5).background(Color.red, in: Circle()).offset(x: 6, y: -6)
                                    }
                                }
                                .frame(width: 64, height: 64)
                            }
                            .padding(.vertical, 4)
                        }
                    } header: {
                        Text(verbatim: course.title).font(.title3).bold().foregroundStyle(Color.primary).textCase(nil)
                    }
                }
            }
            .listStyle(.plain)
            .navigationTitle("Menu")
            .toolbar {
                ToolbarItem(placement: .navigation) {
                    Button("Info", systemImage: "info.circle") {}
                }
            }
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: {}) {
                        Label { Text(verbatim: s.cartLabel).monospacedDigit() } icon: { Image(systemName: "bag.fill") }
                            .labelStyle(.titleAndIcon)
                    }
                }
            }
        }
    }
}
