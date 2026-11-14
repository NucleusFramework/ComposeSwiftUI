import SwiftUI

// Screen 05 — Weather (pure SwiftUI twin of bench/screens/Screen05.kt).
struct Screen05View: View {
    let vm: Screen05ViewModel
    @StateObject private var state: FlowState<Screen05State>

    init(vm: Screen05ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ZStack {
                LinearGradient(colors: [s.skyTop.color, s.skyBottom.color], startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 14) {
                        VStack(spacing: 2) {
                            Text(s.city).font(.title)
                            Text(s.temperature).font(.system(size: 88, weight: .thin))
                            Text(s.condition).font(.title3).fontWeight(.medium)
                            Text(s.highLow).font(.title3).fontWeight(.medium)
                        }
                        VStack(alignment: .leading, spacing: 10) {
                            Text(s.summary).font(.footnote)
                            Divider().overlay(Color.white.opacity(0.4))
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 22) {
                                    ForEach(s.hours, id: \.id) { hour in
                                        VStack(spacing: 8) {
                                            Text(hour.time).font(.footnote.weight(.semibold))
                                            Image(systemName: hour.icon).symbolRenderingMode(.multicolor).font(.title3).frame(height: 26)
                                            Text(verbatim: hour.rain).font(.caption2.bold()).foregroundStyle(Color.cyan)
                                            Text(verbatim: hour.temp).font(.headline)
                                        }
                                    }
                                }
                                .fixedSize()
                            }
                        }
                        .padding(14)
                        .background(Color.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 16))
                        VStack(alignment: .leading, spacing: 8) {
                            Label("7-DAY FORECAST", systemImage: "calendar").font(.caption.weight(.semibold)).opacity(0.75)
                            ForEach(s.days, id: \.id) { day in
                                Divider().overlay(Color.white.opacity(0.4))
                                HStack(spacing: 10) {
                                    Text(day.day).font(.body.weight(.medium)).frame(width: 56, alignment: .leading)
                                    Image(systemName: day.icon).symbolRenderingMode(.multicolor).frame(width: 30)
                                    Text(day.low).opacity(0.7).frame(width: 36, alignment: .trailing)
                                    ZStack(alignment: .leading) {
                                        Capsule().fill(Color.black.opacity(0.18)).frame(height: 5)
                                        Capsule()
                                            .fill(LinearGradient(colors: [Color.mint, Color.yellow, Color.orange], startPoint: .leading, endPoint: .trailing))
                                            .frame(width: day.barWidth, height: 5)
                                            .offset(x: day.barStart)
                                    }
                                    .frame(width: 100)
                                    Text(day.high).frame(width: 36, alignment: .trailing)
                                }
                            }
                        }
                        .padding(14)
                        .background(Color.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 16))
                        HStack(spacing: 10) {
                            ForEach(s.details, id: \.label) { detail in
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(detail.label).font(.caption).opacity(0.75)
                                    Text(verbatim: detail.value).font(.headline)
                                }
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 14))
                            }
                        }
                    }
                    .padding()
                    .foregroundStyle(Color.white)
                }
            }
            .navigationTitle("Weather")
            .toolbarTitleDisplayMode(.inline)
        }
    }
}
