// SecurityUI for Compose SwiftUI: certificateSheet (runtime AppFrameworks.kt). Add this file to the app target and
// call `ComposeIntegrations.registerSecurityUI()` at launch.
import SwiftUI

#if (canImport(SecurityUI)) && compiler(>=6.4)  // SDK 27 only: absent with Xcode 26
import SecurityUI

public extension ComposeIntegrations {
    @MainActor static func registerSecurityUI() {
        guard #available(iOS 27.0, macOS 27.0, tvOS 27.0, watchOS 27.0, *) else { return }
        ComposeRegistry.modifiers[200_358] = { v, a, _ in AnyView(v.modifier(CertificateSheet(a: a))) }
    }
}

/// Keeps the SecTrust built from the Kotlin certificates, so the sheet sees one value until they change.
@available(iOS 27.0, macOS 27.0, tvOS 27.0, watchOS 27.0, *)
private struct CertificateSheet: ViewModifier {
    let a: Args
    @State private var trust: SecTrust?

    func body(content: Content) -> some View {
        let certs: [String]? = a.isNil(0) ? nil : a.list(0).compactMap { $0 as? String }
        let report = a.callback(1, box: { (_: Bool) -> Any? in NSNull() })
        content
            .certificateSheet(trust: Binding(get: { trust }, set: { t in
                trust = t
                if t == nil { report(false) }
            }), title: a.stringOpt(2), message: a.stringOpt(3), help: a.stringOpt(4).flatMap(URL.init(string:)))
            .onChange(of: certs, initial: true) { _, list in trust = list.flatMap(Self.makeTrust) }
    }

    private static func makeTrust(_ base64: [String]) -> SecTrust? {
        let certs = base64.compactMap { Data(base64Encoded: $0) }.compactMap { SecCertificateCreateWithData(nil, $0 as CFData) }
        var t: SecTrust?
        SecTrustCreateWithCertificates(certs as CFArray, SecPolicyCreateBasicX509(), &t)
        return t
    }
}
#endif
