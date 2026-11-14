import SwiftUI

// Screen 31 — Social profile (pure SwiftUI twin of bench/screens/Screen31.kt).
struct Screen31View: View {
    let vm: Screen31ViewModel
    @StateObject private var state: FlowState<Screen31State>

    init(vm: Screen31ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    header(s)
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 4) {
                                Text(s.name).font(.title2.bold())
                                Image(systemName: "checkmark.seal.fill").foregroundStyle(Color.blue)
                            }
                            Text(verbatim: s.handle).font(.subheadline).foregroundStyle(.secondary)
                        }
                        Spacer()
                        if s.following {
                            Button("Following", action: {}).buttonStyle(.bordered).buttonBorderShape(.capsule)
                        } else {
                            Button("Follow", action: {}).buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
                        }
                    }
                    .padding(.horizontal, 16)
                    VStack(alignment: .leading, spacing: 6) {
                        Text(s.bio).font(.subheadline)
                        HStack(spacing: 12) {
                            Label(s.location, systemImage: "mappin.and.ellipse")
                            Label(s.link, systemImage: "link").foregroundStyle(Color.blue)
                        }
                    }
                    .padding(.horizontal, 16)
                    HStack {
                        ForEach(s.stats, id: \.id) { stat in
                            VStack(spacing: 2) {
                                Text(stat.value).font(.headline).monospacedDigit()
                                Text(stat.label).font(.caption).foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 10)
                    .background(Color.gray.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 16)
                    Picker("Content", selection: Binding(get: { s.tab }, set: { vm.setTab(value: $0) })) {
                        Label("Posts", systemImage: "square.grid.3x3").tag(Int32(0))
                        Label("Reels", systemImage: "play.rectangle").tag(Int32(1))
                        Label("Tagged", systemImage: "person.crop.square").tag(Int32(2))
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 16)
                    grid(s.posts)
                }
            }
            .navigationTitle(s.handle)
        }
    }

    private func header(_ s: Screen31State) -> some View {
        ZStack(alignment: .bottomLeading) {
            Rectangle()
                .fill(LinearGradient(colors: [Color.orange, Color.pink, Color.purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(height: 130)
            Image(systemName: "mountain.2.fill")
                .font(.system(size: 70)).foregroundStyle(Color.white.opacity(0.25)).offset(x: 250, y: -20)
            Text(s.initials)
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .foregroundStyle(Color.white)
                .frame(width: 88, height: 88)
                .background(LinearGradient(colors: [Color.indigo, Color.blue], startPoint: .top, endPoint: .bottom), in: Circle())
                .overlay { Circle().stroke(Color.white, lineWidth: 4) }
                .offset(x: 16, y: 44)
        }
        .padding(.bottom, 40)
    }

    private func grid(_ posts: [Screen31Post]) -> some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 2), GridItem(.flexible(), spacing: 2), GridItem(.flexible(), spacing: 2)], spacing: 2) {
            ForEach(posts, id: \.id) { post in
                Rectangle()
                    .fill(LinearGradient(colors: [post.top.color, post.bottom.color], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .aspectRatio(1, contentMode: .fit)
                    .overlay {
                        Image(systemName: post.symbol).font(.title).foregroundStyle(Color.white.opacity(0.9))
                    }
                    .overlay(alignment: .bottomLeading) {
                        HStack(spacing: 3) {
                            Image(systemName: "heart.fill")
                            Text(verbatim: post.likes)
                        }
                        .font(.caption2.bold()).foregroundStyle(Color.white).padding(6)
                    }
                    .overlay(alignment: .topTrailing) {
                        if post.pinned {
                            Image(systemName: "pin.fill").font(.caption).foregroundStyle(Color.white).rotationEffect(Angle(degrees: 45)).padding(6)
                        }
                    }
            }
        }
    }
}
