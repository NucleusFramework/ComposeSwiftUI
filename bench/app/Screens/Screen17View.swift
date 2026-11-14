import SwiftUI

// Screen 17 — Recipe detail (pure SwiftUI twin of bench/screens/Screen17.kt).
struct Screen17View: View {
    let vm: Screen17ViewModel
    @StateObject private var state: FlowState<Screen17State>

    init(vm: Screen17ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        let accent = Color(red: 0.95, green: 0.45, blue: 0.15)
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    ZStack(alignment: .bottomLeading) {
                        LinearGradient(
                            colors: [Color(red: 1.0, green: 0.75, blue: 0.3), Color(red: 0.9, green: 0.3, blue: 0.2)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        Image(systemName: "fork.knife.circle.fill")
                            .font(.system(size: 110))
                            .foregroundStyle(Color.white.opacity(0.25))
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                            .padding(16)
                        VStack(alignment: .leading, spacing: 6) {
                            Text(s.category)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Color.white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.black.opacity(0.25), in: Capsule())
                            Text(s.title).font(.title.bold()).foregroundStyle(Color.white)
                            HStack(spacing: 12) {
                                Text(s.author)
                                Label(s.rating, systemImage: "star.fill")
                            }
                            .font(.subheadline)
                            .foregroundStyle(Color.white.opacity(0.9))
                        }
                        .padding()
                    }
                    .frame(height: 230)
                    VStack(alignment: .leading, spacing: 20) {
                        HStack {
                            ForEach(s.facts, id: \.id) { fact in
                                VStack(spacing: 4) {
                                    Image(systemName: fact.icon).font(.title3).foregroundStyle(accent)
                                    Text(fact.value).font(.subheadline.weight(.semibold)).monospacedDigit()
                                    Text(fact.label).font(.caption2).foregroundStyle(.secondary)
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }
                        .padding(.vertical, 12)
                        .background(Color.gray.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
                        Stepper(s.servingsLabel, value: Binding(get: { s.servings }, set: { vm.setServings(value: $0) }), in: 1...12)
                        HStack(alignment: .firstTextBaseline) {
                            Text("Ingredients").font(.title2.bold())
                            Spacer()
                            Text(s.checkedLabel).font(.caption).foregroundStyle(.secondary)
                        }
                        VStack(spacing: 0) {
                            ForEach(s.ingredients, id: \.id) { ingredient in
                                HStack(spacing: 12) {
                                    Image(systemName: ingredient.checked ? "checkmark.circle.fill" : "circle")
                                        .font(.title3)
                                        .foregroundStyle(ingredient.checked ? Color.green : Color.gray.opacity(0.5))
                                    Text(ingredient.name)
                                        .strikethrough(ingredient.checked, pattern: .solid, color: nil)
                                        .foregroundStyle(ingredient.checked ? Color.secondary : Color.primary)
                                    Spacer()
                                    Text(ingredient.amount).font(.subheadline).monospacedDigit().foregroundStyle(.secondary)
                                }
                                .padding(.vertical, 10)
                                Divider()
                            }
                        }
                        Text("Method").font(.title2.bold())
                        ForEach(s.steps, id: \.id) { step in
                            HStack(alignment: .top, spacing: 14) {
                                Text(step.number)
                                    .font(.headline)
                                    .foregroundStyle(Color.white)
                                    .frame(width: 32, height: 32)
                                    .background(accent, in: Circle())
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(step.title).font(.headline)
                                        Spacer()
                                        Label(step.timer, systemImage: "timer").font(.caption).foregroundStyle(accent)
                                    }
                                    Text(step.text).font(.subheadline).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                                }
                            }
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Recipe")
        }
    }
}
