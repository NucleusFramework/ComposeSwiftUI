// PhotosUI for Compose SwiftUI: PhotosPicker, photosPicker and the picker modifiers (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerPhotosUI()` at launch.
#if os(iOS) || os(macOS) || os(visionOS)
import SwiftUI
import PhotosUI
import UniformTypeIdentifiers

public extension ComposeIntegrations {
    @MainActor static func registerPhotosUI() {
        guard #available(iOS 17.0, macOS 14.0, *) else { return }
        ComposeRegistry.views[200_300] = { m in ModChain.apply(KotlinPhotosPicker(m: m), m, 0) }
        ComposeRegistry.modifiers[200_300] = { v, a, _ in AnyView(v.modifier(PhotosPickerSheet(a: a))) }
        ComposeRegistry.modifiers[200_301] = { v, a, _ in PhotosModifiers.style(v, a) }
        #if os(iOS) || os(visionOS)
        #if compiler(>=6.4)
        if #available(iOS 27.0, *) {
            ComposeRegistry.modifiers[200_302] = { v, a, _ in PhotosModifiers.sharedAlbum(v, a) }
            ComposeRegistry.modifiers[200_303] = { v, a, _ in PhotosModifiers.referenceImage(v, a) }
        }
        #endif
        #endif
    }
}

@available(iOS 17.0, macOS 14.0, *)
@MainActor enum PhotosValues {
    static func filter(_ l: [Any]) -> PHPickerFilter? {
        let all: [PHPickerFilter] = {
            var f: [PHPickerFilter] = [.images, .videos, .livePhotos, .screenshots, .screenRecordings, .slomoVideos, .timelapseVideos,
                                       .panoramas, .bursts, .depthEffectPhotos, .cinematicVideos]
            if #available(iOS 26.0, macOS 26.0, *) { f.append(.spatialMedia) } else { f.append(.images) }
            return f
        }()
        let picked = l.compactMap { ($0 as? NSNumber)?.intValue }.filter { $0 >= 0 && $0 < all.count }.map { all[$0] }
        return picked.isEmpty ? nil : .any(of: picked)
    }

    static func behavior(_ i: Int) -> PhotosPickerSelectionBehavior {
        switch i {
        case 1: return .ordered
        case 2: return .continuous
        case 3: return .continuousAndOrdered
        default: return .default
        }
    }

    static func encoding(_ i: Int) -> PhotosPickerItem.EncodingDisambiguationPolicy {
        i == 1 ? .current : i == 2 ? .compatible : .automatic
    }

    /// Copies each item to a temporary file and reports `[identifier, file URL, content type]` for each.
    static func report(_ items: [PhotosPickerItem], _ send: @escaping (NSArray) -> Void) {
        Task {
            var out: [NSArray] = []
            for item in items {
                let type = item.supportedContentTypes.first
                var url: String?
                if let data = try? await item.loadTransferable(type: Data.self) {
                    let file = FileManager.default.temporaryDirectory
                        .appendingPathComponent(UUID().uuidString)
                        .appendingPathExtension(type?.preferredFilenameExtension ?? "dat")
                    if (try? data.write(to: file)) != nil { url = file.absoluteString }
                }
                out.append([(item.itemIdentifier as Any?) ?? NSNull(), (url as Any?) ?? NSNull(), (type?.identifier as Any?) ?? NSNull()] as NSArray)
            }
            await MainActor.run { send(out as NSArray) }
        }
    }
}

/// Selection state shared by the picker view and sheet: the Kotlin identifiers, mapped to picker items.
@available(iOS 17.0, macOS 14.0, *)
@MainActor private struct PhotosSelection {
    let a: Args
    let offset: Int

    var ids: [String] { a.list(offset).compactMap { $0 as? String } }
    var single: Bool { a.intOpt(offset + 2) == 1 }

    func binding() -> Binding<[PhotosPickerItem]> {
        let send = a.callback(offset + 1, box: { (x: NSArray) -> Any? in x })
        return Binding(get: { ids.map { PhotosPickerItem(itemIdentifier: $0) } }, set: { PhotosValues.report($0, send) })
    }

    func singleBinding() -> Binding<PhotosPickerItem?> {
        let send = a.callback(offset + 1, box: { (x: NSArray) -> Any? in x })
        return Binding(get: { ids.first.map { PhotosPickerItem(itemIdentifier: $0) } }, set: { PhotosValues.report($0.map { [$0] } ?? [], send) })
    }
}

@available(iOS 17.0, macOS 14.0, *)
private struct KotlinPhotosPicker: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let s = PhotosSelection(a: a, offset: 0)
        let filter = PhotosValues.filter(a.list(4))
        let encoding = PhotosValues.encoding(a.int(5))
        let title = a.stringOpt(6) ?? "Select Photos"
        if s.single {
            if let label = a.slotModel(7) {
                PhotosPicker(selection: s.singleBinding(), matching: filter, preferredItemEncoding: encoding, photoLibrary: PHPhotoLibrary.shared(),
                             label: { NodeView(model: label) })
            } else {
                PhotosPicker(title, selection: s.singleBinding(), matching: filter, preferredItemEncoding: encoding, photoLibrary: PHPhotoLibrary.shared())
            }
        } else if let label = a.slotModel(7) {
            PhotosPicker(selection: s.binding(), maxSelectionCount: a.intOpt(2), selectionBehavior: PhotosValues.behavior(a.int(3)),
                         matching: filter, preferredItemEncoding: encoding, photoLibrary: PHPhotoLibrary.shared()) { NodeView(model: label) }
        } else {
            PhotosPicker(title, selection: s.binding(), maxSelectionCount: a.intOpt(2), selectionBehavior: PhotosValues.behavior(a.int(3)),
                         matching: filter, preferredItemEncoding: encoding, photoLibrary: PHPhotoLibrary.shared())
        }
    }
}

@available(iOS 17.0, macOS 14.0, *)
private struct PhotosPickerSheet: ViewModifier {
    let a: Args

    func body(content: Content) -> some View {
        let s = PhotosSelection(a: a, offset: 2)
        let filter = PhotosValues.filter(a.list(6))
        let encoding = PhotosValues.encoding(a.int(7))
        if s.single {
            content.photosPicker(isPresented: a.boolBinding(0), selection: s.singleBinding(), matching: filter,
                                 preferredItemEncoding: encoding, photoLibrary: PHPhotoLibrary.shared())
        } else {
            content.photosPicker(isPresented: a.boolBinding(0), selection: s.binding(), maxSelectionCount: a.intOpt(4),
                                 selectionBehavior: PhotosValues.behavior(a.int(5)), matching: filter, preferredItemEncoding: encoding,
                                 photoLibrary: PHPhotoLibrary.shared())
        }
    }
}

@available(iOS 17.0, macOS 14.0, *)
@MainActor private enum PhotosModifiers {
    static func style(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        #if os(iOS) || os(macOS) || os(visionOS)
        if let s = a.intOpt(0) {
            v = AnyView(v.photosPickerStyle(s == 1 ? .inline : s == 2 ? .compact : .presentation))
        }
        if let vis = d_Visibility(a.expr(1)) {
            v = AnyView(v.photosPickerAccessoryVisibility(vis, edges: d_Edge_Set(a.expr(2)) ?? .all))
        }
        let caps = a.list(3).compactMap { ($0 as? NSNumber)?.intValue }
        if !caps.isEmpty {
            var c: PHPickerCapabilities = []
            for i in caps {
                switch i {
                case 0: c.insert(.search)
                case 1: c.insert(.collectionNavigation)
                case 2: c.insert(.stagingArea)
                case 3: c.insert(.selectionActions)
                default: c.insert(.sensitivityAnalysisIntervention)
                }
            }
            v = AnyView(v.photosPickerDisabledCapabilities(c))
        }
        let meta = a.list(4).compactMap { ($0 as? NSNumber)?.intValue }
        #if compiler(>=6.4)
        if !meta.isEmpty, #available(iOS 27.0, macOS 27.0, *) {
            var o: PHPickerMetadataOptions = []
            if meta.contains(0) { o.insert(.removeLocation) }
            if meta.contains(1) { o.insert(.removeCaptions) }
            v = AnyView(v.photosPickerMetadataOptions(o))
        }
        #endif
        #if compiler(>=6.4)
        if let text = a.stringOpt(5), #available(iOS 27.0, macOS 27.0, *) {
            v = AnyView(v.photosPickerSearchText(text))
        }
        #endif
        #endif
        return v
    }

    #if os(iOS) || os(visionOS)
    #if compiler(>=6.4)
    @available(iOS 27.0, *)
    static func sharedAlbum(_ v: AnyView, _ a: Args) -> AnyView {
        let done = a.callback(6, box: { (s: String?) -> Any? in s ?? NSNull() })
        let completion: ((any Error)?) -> Void = { done($0?.localizedDescription) }
        if let ids = a.isNil(3) ? nil : a.list(3).compactMap({ $0 as? String }) {
            let items = ids.map { PhotosPickerItem(itemIdentifier: $0) }
            return AnyView(v.photosSharedAlbumPostingSheet(isPresented: a.boolBinding(0), items: items, defaultAlbumIdentifier: a.stringOpt(2),
                                                           photoLibrary: PHPhotoLibrary.shared()) { result in
                if case .failure(let e) = result { completion(e) } else { completion(nil) }
            })
        }
        if let album = a.stringOpt(2) {
            return AnyView(v.photosSharedAlbumCustomizationSheet(isPresented: a.boolBinding(0), albumIdentifier: album, photoLibrary: PHPhotoLibrary.shared()) { completion($0) })
        }
        return AnyView(v.photosSharedAlbumCreationSheet(isPresented: a.boolBinding(0), defaultTitle: a.stringOpt(4), photoLibrary: PHPhotoLibrary.shared()) { _ in completion(nil) })
    }
    #endif

    #if compiler(>=6.4)
    @available(iOS 27.0, *)
    static func referenceImage(_ v: AnyView, _ a: Args) -> AnyView {
        let set = a.callback(1, box: { (s: String?) -> Any? in s ?? NSNull() })
        let done = a.callback(2, box: { (s: String?) -> Any? in s ?? NSNull() })
        let binding = Binding<URL?>(get: { a.stringOpt(0).flatMap(URL.init(string:)) }, set: { set($0?.absoluteString) })
        return AnyView(v.photosReferenceImageViewer(fileURL: binding) { result in
            if case .success(let url) = result { done(url.absoluteString) } else { done(nil) }
        })
    }
    #endif
    #endif
}
#endif
