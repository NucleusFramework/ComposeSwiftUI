import SwiftUI
import Accessibility
import CoreText
import ImageIO
import UniformTypeIdentifiers

// Round-trip check of the rich TextEditor codec (Completions.swift, RichTextCodec): every attribute the native
// editor can put on a run survives editor → Kotlin runs → a Kotlin restyle → editor. Run: scripts/check_rich_text.sh

@main
struct RichTextCheck {
    typealias UI = AttributeScopes.SwiftUIAttributes

    @MainActor static func main() {
        guard #available(macOS 26.0, iOS 26.0, *) else { print("needs macOS 26 / iOS 26"); exit(1) }
        run()
    }

    @available(macOS 26.0, iOS 26.0, *)
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
        original[AttributeScopes.FoundationAttributes.LanguageIdentifierAttribute.self] = "fr"
        original[AttributeScopes.FoundationAttributes.InlinePresentationIntentAttribute.self] = .code
        original[AttributeScopes.AccessibilityAttributes.HeadingLevelAttribute.self] = .h2
        original[AttributeScopes.AccessibilityAttributes.SpellOutAttribute.self] = true
        original[AttributeScopes.AccessibilityAttributes.IPANotationAttribute.self] = "ɛlo"
        original[AttributeScopes.AccessibilityAttributes.TextCustomAttribute.self] = ["greeting"]
        original[AttributeScopes.AccessibilityAttributes.TextualContextAttribute.self] = .sourceCode
        original[AttributeScopes.AccessibilityAttributes.IncludesPunctuationAttribute.self] = true
        original[AttributeScopes.AccessibilityAttributes.AdjustedPitchAttribute.self] = 1.2
        original[AttributeScopes.AccessibilityAttributes.QueueAnnouncementAttribute.self] = true
        original[AttributeScopes.AccessibilityAttributes.AnnouncementPriorityAttribute.self] = .high
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
            ("languageIdentifier", first[AttributeScopes.FoundationAttributes.LanguageIdentifierAttribute.self] == "fr"),
            ("inlinePresentationIntent", first[AttributeScopes.FoundationAttributes.InlinePresentationIntentAttribute.self] == .code),
            ("accessibilityHeadingLevel", first[AttributeScopes.AccessibilityAttributes.HeadingLevelAttribute.self] == .h2),
            ("accessibilitySpeechSpellsOutCharacters", first[AttributeScopes.AccessibilityAttributes.SpellOutAttribute.self] == true),
            ("accessibilitySpeechPhoneticNotation", first[AttributeScopes.AccessibilityAttributes.IPANotationAttribute.self] == "ɛlo"),
        ] { check(same, "\(name) lost after a Kotlin restyle") }
        // Every attribute but the font, compared as a whole: nothing set above may be lost or altered
        var kept = first, expected = original
        kept[UI.FontAttribute.self] = nil
        expected[UI.FontAttribute.self] = nil
        check(kept == expected, "attributes other than the font changed after a Kotlin restyle")
        if let font = first[UI.FontAttribute.self] {
            let r = font.resolve(in: env.fontResolutionContext)
            check(r.isItalic, "font not italic after the restyle")
            check((CTFontCopyFamilyName(r.ctFont) as String) == "Georgia", "font family lost: \(CTFontCopyFamilyName(r.ctFont))")
            check(abs(r.pointSize - 20) < 0.01, "font size changed: \(r.pointSize)")
        } else {
            failures.append("font lost after a Kotlin restyle")
        }
        check(back.runs.count == 2 && String(back.characters) == "Hello plain", "text or runs changed")

        // 4. A Genmoji (an adaptive image glyph on U+FFFC) survives the round trip and a Kotlin restyle
        let glyph = AttributedString.AdaptiveImageGlyph(imageContent: Self.glyphImage())
        var glyphRun = AttributedString("\u{FFFC}")
        glyphRun[UI.AdaptiveImageGlyphAttribute.self] = glyph
        let withGlyph = AttributedString("Hi ") + glyphRun
        let glyphCodec = RichTextCodec()
        var glyphRuns = glyphCodec.encode(withGlyph, in: env)
        check(glyphCodec.load(glyphRuns, in: env) == withGlyph, "Genmoji: unchanged round trip differs")
        let glyphStart = 15  // second run
        glyphRuns[glyphStart + 6] = KBox.int(1)  // Kotlin makes the Genmoji run italic
        glyphRuns[glyphStart + 14] = KBox.int(2)
        let glyphBack = glyphCodec.load(glyphRuns, in: env)
        check(glyphBack.runs.last?.attributes[UI.AdaptiveImageGlyphAttribute.self] == glyph, "Genmoji lost after a Kotlin restyle")
        check(String(glyphBack.characters) == "Hi \u{FFFC}", "Genmoji character changed")

        // 3. Kotlin removes the underline (FIELD_UNDERLINE): it goes, the rest stays
        var noUnderline = runs
        noUnderline[6] = KBox.int(((noUnderline[6] as? NSNumber)?.intValue ?? 0) & ~2)
        noUnderline[14] = KBox.int(4)
        let back2 = codec.load(noUnderline, in: env).runs.first!.attributes
        check(back2[UI.UnderlineStyleAttribute.self] == nil, "underline not removed")
        check(back2[AttributeScopes.CoreTextAttributes.LineHeightAttribute.self] == .loose, "lineHeight lost after removing underline")

        if failures.isEmpty { print("rich text round trip: OK (23 attributes + Genmoji)"); exit(0) }
        failures.forEach { print("FAIL: \($0)") }
        exit(1)
    }

    /// Image data in the adaptive image glyph format (a HEIC image, the type AdaptiveImageGlyph.contentType names).
    @available(macOS 26.0, iOS 26.0, *)
    static func glyphImage() -> Data {
        let ctx = CGContext(data: nil, width: 64, height: 64, bitsPerComponent: 8, bytesPerRow: 0,
                            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        ctx.setFillColor(CGColor(red: 1, green: 0.8, blue: 0, alpha: 1))
        ctx.fillEllipse(in: CGRect(x: 4, y: 4, width: 56, height: 56))
        let data = NSMutableData()
        let type = AttributedString.AdaptiveImageGlyph.contentType.identifier as CFString
        let dest = CGImageDestinationCreateWithData(data, type, 1, nil) ?? CGImageDestinationCreateWithData(data, UTType.heic.identifier as CFString, 1, nil)!
        CGImageDestinationAddImage(dest, ctx.makeImage()!, nil)
        CGImageDestinationFinalize(dest)
        return data as Data
    }
}
