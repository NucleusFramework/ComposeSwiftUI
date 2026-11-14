import SwiftUI

// Screen 14 — News feed (pure SwiftUI twin of bench/screens/Screen14.kt).
struct Screen14View: View {
    let vm: Screen14ViewModel
    @StateObject private var state: FlowState<Screen14State>

    init(vm: Screen14ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(s.categories, id: \.self) { category in
                                let selected = category == s.selectedCategory
                                Text(category)
                                    .font(.subheadline.weight(.medium))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 7)
                                    .foregroundStyle(selected ? Color.white : Color.primary)
                                    .background(selected ? AnyShapeStyle(Color.blue) : AnyShapeStyle(.background), in: Capsule())
                            }
                        }
                        .padding(.horizontal)
                    }
                    ForEach(s.articles, id: \.id) { article in
                        VStack(alignment: .leading, spacing: 0) {
                            ZStack(alignment: .bottomLeading) {
                                LinearGradient(colors: article.gradient.map { $0.color }, startPoint: .topLeading, endPoint: .bottomTrailing)
                                Image(systemName: article.symbol)
                                    .font(.system(size: 64, weight: .light))
                                    .foregroundStyle(Color.white.opacity(0.35))
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                                Text(article.category)
                                    .font(.caption.weight(.bold))
                                    .textCase(.uppercase)
                                    .foregroundStyle(article.categoryTint.color)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(.background, in: Capsule())
                                    .padding(12)
                            }
                            .frame(height: 150)
                            VStack(alignment: .leading, spacing: 8) {
                                Text(article.headline).font(.headline).lineLimit(2)
                                Text(article.summary).font(.subheadline).foregroundStyle(.secondary).lineLimit(3)
                                HStack(spacing: 8) {
                                    Text(article.initials)
                                        .font(.caption2.weight(.bold))
                                        .foregroundStyle(Color.white)
                                        .frame(width: 26, height: 26)
                                        .background(article.categoryTint.color, in: Circle())
                                    Text(article.author).font(.caption.weight(.semibold))
                                    Text("·").foregroundStyle(.tertiary)
                                    Text(article.time).font(.caption).foregroundStyle(.secondary)
                                    Spacer()
                                    Label(article.readTime, systemImage: "clock").font(.caption2).foregroundStyle(.secondary)
                                    Image(systemName: article.bookmarked ? "bookmark.fill" : "bookmark")
                                        .foregroundStyle(article.bookmarked ? Color.orange : Color.gray)
                                }
                                .padding(.top, 4)
                            }
                            .padding(14)
                        }
                        .background(.background)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .shadow(color: Color.black.opacity(0.08), radius: 6, y: 2)
                        .padding(.horizontal)
                    }
                }
                .padding(.bottom, 16)
            }
            .background(Color.gray.opacity(0.1))
            .navigationTitle("News")
        }
    }
}
