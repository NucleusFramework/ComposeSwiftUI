import SwiftUI

// Round-trip check of the rich TextEditor codec (Completions.swift, RichTextCodec): every attribute the native
// editor can put on a run survives editor → Kotlin runs → a Kotlin restyle → editor. Run: scripts/check_rich_text.sh

@main
struct RichTextCheck {
    typealias UI = AttributeScopes.SwiftUIAttributes

    @MainActor static func main() {
        guard #available(macOS 26.0, *) else { print("needs macOS 26"); exit(1) }
        run()
    }

    @available(macOS 26.0, *)
    @MainActor static func run() {
        let env = EnvironmentValues()
        var original = AttributeContainer()
        original[UI.FontAttribute.self] = .custom("Georgia", size: 20)
        original[UI.ForegroundColorAttribute.self] = .red
        original[UI.BackgroundColorAttribute.self] = .yellow
        original[UI.UnderlineStyleAttribute.self] = .single
        original[UI.StrikethroughStyleAttribute.self] = .single
        original[UI.KerningAttribute.self] = 1.5
        original[UI.TrackingAttribute.self] = 0.5
        original[UI.BaselineOffsetAttribute.self] = 2
        original[AttributeScopes.CoreTextAttributes.TextAlignmentAttribute.self] = .center
        original[AttributeScopes.CoreTextAttributes.LineHeightAttribute.self] = .loose
        original[AttributeScopes.FoundationAttributes.WritingDirectionAttribute.self] = .rightToLeft
        original[AttributeScopes.FoundationAttributes.LinkAttribute.self] = URL(string: "https://example.com")
        let text = AttributedString("Hello", attributes: original) + AttributedString(" plain")

        var failures: [String] = []
        func check(_ ok: Bool, _ what: String) { if !ok { failures.append(what) } }

        // 1. Unchanged round trip: the editor gets back exactly what it reported
        let codec = RichTextCodec()
        let runs = codec.encode(text, in: env)
        check(codec.load(runs, in: env) == text, "unchanged round trip differs")

        // 2. Kotlin makes the first run italic (flags bit 1, changed = FIELD_FONT): only the font changes
        var restyled = runs
        restyled[6] = KBox.int(((restyled[6] as? NSNumber)?.intValue ?? 0) | 1)
        restyled[14] = KBox.int(2)
        let back = codec.load(restyled, in: env)
        let first = back.runs.first!.attributes
        for (name, same) in [
            ("foregroundColor", first[UI.ForegroundColorAttribute.self] == original[UI.ForegroundColorAttribute.self]),
            ("backgroundColor", first[UI.BackgroundColorAttribute.self] == original[UI.BackgroundColorAttribute.self]),
            ("underline", first[UI.UnderlineStyleAttribute.self] == original[UI.UnderlineStyleAttribute.self]),
            ("strikethrough", first[UI.StrikethroughStyleAttribute.self] == original[UI.StrikethroughStyleAttribute.self]),
            ("kern", first[UI.KerningAttribute.self] == original[UI.KerningAttribute.self]),
            ("tracking", first[UI.TrackingAttribute.self] == original[UI.TrackingAttribute.self]),
            ("baselineOffset", first[UI.BaselineOffsetAttribute.self] == original[UI.BaselineOffsetAttribute.self]),
            ("alignment", first[AttributeScopes.CoreTextAttributes.TextAlignmentAttribute.self] == .center),
            ("lineHeight", first[AttributeScopes.CoreTextAttributes.LineHeightAttribute.self] == .loose),
            ("writingDirection", first[AttributeScopes.FoundationAttributes.WritingDirectionAttribute.self] == .rightToLeft),
            ("link", first[AttributeScopes.FoundationAttributes.LinkAttribute.self] == original[AttributeScopes.FoundationAttributes.LinkAttribute.self]),
        ] { check(same, "\(name) lost after a Kotlin restyle") }
        if let font = first[UI.FontAttribute.self] {
            let r = font.resolve(in: env.fontResolutionContext)
            check(r.isItalic, "font not italic after the restyle")
            check((CTFontCopyFamilyName(r.ctFont) as String) == "Georgia", "font family lost: \(CTFontCopyFamilyName(r.ctFont))")
            check(abs(r.pointSize - 20) < 0.01, "font size changed: \(r.pointSize)")
        } else {
            failures.append("font lost after a Kotlin restyle")
        }
        check(back.runs.count == 2 && String(back.characters) == "Hello plain", "text or runs changed")

        // 3. Kotlin removes the underline (FIELD_UNDERLINE): it goes, the rest stays
        var noUnderline = runs
        noUnderline[6] = KBox.int(((noUnderline[6] as? NSNumber)?.intValue ?? 0) & ~2)
        noUnderline[14] = KBox.int(4)
        let back2 = codec.load(noUnderline, in: env).runs.first!.attributes
        check(back2[UI.UnderlineStyleAttribute.self] == nil, "underline not removed")
        check(back2[AttributeScopes.CoreTextAttributes.LineHeightAttribute.self] == .loose, "lineHeight lost after removing underline")

        if failures.isEmpty { print("rich text round trip: OK (12 attributes)"); exit(0) }
        failures.forEach { print("FAIL: \($0)") }
        exit(1)
    }
}
