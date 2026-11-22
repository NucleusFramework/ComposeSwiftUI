// ImagePlayground for Compose SwiftUI: imagePlaygroundSheet and its styles (runtime AppFrameworks.kt). Add this file
// to the app target and call `ComposeIntegrations.registerImagePlayground()` at launch.
import SwiftUI

#if os(iOS) || os(macOS)
import ImagePlayground

public extension ComposeIntegrations {
    @MainActor static func registerImagePlayground() {
        guard #available(iOS 18.1, macOS 15.1, *) else { return }
        ComposeRegistry.modifiers[200_353] = { v, a, _ in PlaygroundModifiers.sheet(v, a) }
        if #available(iOS 18.4, macOS 15.4, *) {
            ComposeRegistry.modifiers[200_354] = { v, a, _ in PlaygroundModifiers.style(v, a) }
        }
    }
}

@available(iOS 18.1, macOS 15.1, *)
@MainActor private enum PlaygroundModifiers {
    static func sheet(_ v: AnyView, _ a: Args) -> AnyView {
        let concepts = a.list(2).compactMap { $0 as? [Any] }.map { c -> ImagePlaygroundConcept in
            let text = c.count > 1 ? c[1] as? String ?? "" : ""
            return (c.first as? NSNumber)?.intValue == 1 ? .extracted(from: text, title: c.count > 2 ? c[2] as? String : nil) : .text(text)
        }
        let done = a.callback(5, box: { (s: String) -> Any? in s })
        let cancel = a.action(6)
        let complete: (URL) -> Void = { done($0.absoluteString) }
        let presented = a.boolBinding(0)
        if !a.isNil(7), #available(iOS 18.4, macOS 15.4, *) {
            let glyph = a.callback(7, box: { (s: String) -> Any? in s })
            let onGlyph: (NSAdaptiveImageGlyph) -> Void = { g in
                let url = FileManager.default.temporaryDirectory.appendingPathComponent(g.contentIdentifier).appendingPathExtension("heic")
                try? g.imageContent.write(to: url)
                glyph(url.absoluteString)
            }
            if let s = a.stringOpt(4), let url = URL(string: s) {
                return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImageURL: url, onCompletion: complete,
                                                      onAdaptiveImageGlyphCreation: onGlyph, onCancellation: cancel))
            }
            return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImage: a.stringOpt(3).map { Image($0) },
                                                  onCompletion: complete, onAdaptiveImageGlyphCreation: onGlyph, onCancellation: cancel))
        }
        if let s = a.stringOpt(4), let url = URL(string: s) {
            return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImageURL: url, onCompletion: complete, onCancellation: cancel))
        }
        return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImage: a.stringOpt(3).map { Image($0) },
                                              onCompletion: complete, onCancellation: cancel))
    }

    @available(iOS 18.4, macOS 15.4, *)
    static func style(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        func style(_ i: Int) -> ImagePlaygroundStyle? {
            switch i {
            case 0: return .animation
            case 1: return .illustration
            case 2: return .sketch
            case 3: return .emoji
            case 4: if #available(iOS 26.0, macOS 26.0, *) { return .externalProvider } else { return nil }
            default:
                #if compiler(>=6.4)
                if #available(iOS 27.0, macOS 27.0, *) { return .any }
                #endif
                return nil
            }
        }
        if let i = a.intOpt(0), let s = style(i) {
            let allowed = a.isNil(1) ? ImagePlaygroundStyle.all : a.list(1).compactMap { ($0 as? NSNumber).flatMap { style($0.intValue) } }
            v = AnyView(v.imagePlaygroundGenerationStyle(s, in: allowed))
        }
        if let i = a.intOpt(2) {
            v = AnyView(v.imagePlaygroundPersonalizationPolicy(ImagePlaygroundPersonalizationPolicy(rawValue: i) ?? .automatic))
        }
        if #available(iOS 26.4, macOS 26.4, *), !a.isNil(3) {
            let o = a.list(3)
            func int(_ k: Int) -> Int? { k < o.count ? (o[k] as? NSNumber)?.intValue : nil }
            var options = ImagePlaygroundOptions()
            if let p = int(2) { options.personalization = .init(rawValue: p) ?? .automatic }
            #if compiler(>=6.4)
            if #available(iOS 27.0, macOS 27.0, *) {
                if let s = int(0) { options.creationStrategy = .init(rawValue: s) ?? .automatic }
                if let s = int(1) { options.creationVariety = .init(rawValue: s) ?? .automatic }
                if o.count > 3, let size = d_CGSize(o[3] as? SExpr) { options.sizeSpecification = .closest(to: size) }
            }
            #endif
            v = AnyView(v.imagePlaygroundOptions(options))
        }
        return v
    }
}
#endif
