import SwiftUI

// Screen 47 — Book library (pure SwiftUI twin of bench/screens/Screen47.kt).
struct Screen47View: View {
    let vm: Screen47ViewModel
    @StateObject private var state: FlowState<Screen47State>

    init(vm: Screen47ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    private func cover(_ book: Screen47Book, _ width: CGFloat, _ height: CGFloat) -> some View {
        ZStack {
            LinearGradient(colors: [book.coverTop.color, book.coverBottom.color], startPoint: .top, endPoint: .bottom)
            VStack(spacing: 6) {
                Image(systemName: book.symbol).font(.title2).foregroundStyle(Color.white.opacity(0.9))
                Text(book.title)
                    .font(.system(size: 10, weight: .bold, design: .serif))
                    .foregroundStyle(Color.white).multilineTextAlignment(.center).padding(.horizontal, 6)
            }
        }
        .frame(width: width, height: height)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .shadow(color: Color.black.opacity(0.2), radius: 4, x: 0, y: 2)
    }

    private func stars(_ stars: [Screen47Star]) -> some View {
        HStack(spacing: 1) {
            ForEach(stars, id: \.id) { star in
                Image(systemName: star.icon).font(.system(size: 9)).foregroundStyle(Color.orange)
            }
        }
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    HStack(alignment: .top, spacing: 14) {
                        cover(s.current, 78, 116)
                        VStack(alignment: .leading, spacing: 6) {
                            Text("CURRENTLY READING").font(.caption2).fontWeight(.bold).foregroundStyle(Color.indigo)
                            Text(s.current.title).font(.headline)
                            Text(s.current.author).font(.subheadline).foregroundStyle(.secondary)
                            stars(s.current.stars)
                            Gauge(value: s.progress, in: 0...1) {
                                Text(s.pagesText).font(.caption).monospacedDigit()
                            } currentValueLabel: {
                                Text(s.progressText).font(.caption).monospacedDigit()
                            }
                            .gaugeStyle(.linearCapacity).tint(Color.indigo)
                        }
                    }
                    .padding(14).background(Color.indigo.opacity(0.08), in: RoundedRectangle(cornerRadius: 18))
                    HStack(spacing: 16) {
                        Gauge(value: s.goal, in: 0...24) {
                            Text(s.goalText).font(.headline)
                        }
                        .gaugeStyle(.accessoryCircularCapacity).tint(Color.orange)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("2026 Reading Goal").font(.headline)
                            Text(s.goalCaption).font(.subheadline).foregroundStyle(.secondary)
                            Label(s.streak, systemImage: "flame.fill").font(.caption).foregroundStyle(Color.orange)
                        }
                        Spacer()
                    }
                    ForEach(s.shelves, id: \.id) { shelf in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(alignment: .firstTextBaseline) {
                                Text(shelf.title).font(.title3).bold()
                                Spacer()
                                Text(shelf.count).font(.subheadline).foregroundStyle(.secondary)
                            }
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(alignment: .top, spacing: 14) {
                                    ForEach(shelf.books, id: \.id) { book in
                                        VStack(alignment: .leading, spacing: 4) {
                                            cover(book, 92, 136)
                                            Text(book.title).font(.caption).fontWeight(.semibold).lineLimit(1)
                                            Text(book.author).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                                            HStack(spacing: 4) {
                                                stars(book.stars)
                                                Text(book.rating).font(.caption2).foregroundStyle(.secondary)
                                            }
                                        }
                                        .frame(width: 92)
                                    }
                                }
                            }
                            Rectangle().fill(Color.brown).frame(height: 6).opacity(0.25)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Library")
        }
    }
}
