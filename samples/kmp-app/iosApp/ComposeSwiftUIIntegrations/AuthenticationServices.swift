// AuthenticationServices, CoreLocationUI and LocalAuthentication for Compose SwiftUI: SignInWithAppleButton,
// authorization requests, web authentication sessions, LocationButton and LocalAuthenticationView (runtime
// Frameworks.kt). Add this file to the app target and call `ComposeIntegrations.registerAuthenticationServices()`.
import SwiftUI
import AuthenticationServices
#if os(iOS) || os(watchOS)
import CoreLocationUI
#endif
#if os(macOS)
import LocalAuthentication
#endif

public extension ComposeIntegrations {
    @MainActor static func registerAuthenticationServices() {
        ComposeRegistry.views[200_303] = { m in ModChain.apply(KotlinSignInWithApple(m: m), m, 0) }
        ComposeRegistry.modifiers[200_308] = { v, a, _ in
            let styles: [SignInWithAppleButton.Style] = [.black, .white, .whiteOutline]
            return AnyView(v.signInWithAppleButtonStyle(styles[min(max(a.int(0), 0), 2)]))
        }
        if #available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *) {
            ComposeRegistry.modifiers[200_309] = { v, a, _ in AnyView(v.modifier(AuthorizationModifier(a: a))) }
        }
        #if !os(tvOS)
        if #available(iOS 16.4, macOS 13.3, watchOS 9.4, *) {
            ComposeRegistry.modifiers[200_310] = { v, a, _ in AnyView(v.modifier(WebAuthenticationModifier(a: a))) }
        }
        #endif
        #if os(iOS) || os(watchOS)
        ComposeRegistry.views[200_304] = { m in
            let a = m.args
            let titles: [LocationButton.Title] = [.currentLocation, .sendCurrentLocation, .sendMyCurrentLocation, .shareCurrentLocation, .shareMyCurrentLocation]
            let title = a.intOpt(0).map { titles[min(max($0, 0), 4)] }
            return ModChain.apply(LocationButton(title, action: a.action(1)), m, 0)
        }
        #endif
        #if os(macOS)
        if #available(macOS 15.0, *) {
            ComposeRegistry.views[200_305] = { m in ModChain.apply(KotlinLocalAuthentication(m: m), m, 0) }
        }
        #endif
    }
}

enum AuthEncoding {
    static func string(_ d: Data?) -> Any { d.flatMap { String(data: $0, encoding: .utf8) } as Any? ?? NSNull() }

    static func appleID(_ c: ASAuthorizationAppleIDCredential) -> NSArray {
        [KotlinInt(value: 0), c.user, (c.email as Any?) ?? NSNull(), (c.fullName?.givenName as Any?) ?? NSNull(),
         (c.fullName?.familyName as Any?) ?? NSNull(), string(c.identityToken), string(c.authorizationCode)] as NSArray
    }

    static func failure(_ e: any Error) -> NSArray { [KotlinInt(value: 3), e.localizedDescription] as NSArray }

    static func credential(_ c: ASAuthorizationCredential) -> NSArray {
        if let a = c as? ASAuthorizationAppleIDCredential { return appleID(a) }
        if let p = c as? ASPasswordCredential { return [KotlinInt(value: 1), p.user, p.password] as NSArray }
        #if !os(tvOS) && !os(watchOS)
        if #available(iOS 15.0, macOS 12.0, *) {
            if let r = c as? ASAuthorizationPlatformPublicKeyCredentialRegistration {
                return [KotlinInt(value: 2), KotlinBoolean(value: true), r.credentialID.base64EncodedString(), r.rawClientDataJSON.base64EncodedString(),
                        NSNull(), NSNull(), NSNull(), (r.rawAttestationObject?.base64EncodedString() as Any?) ?? NSNull()] as NSArray
            }
            if let x = c as? ASAuthorizationPlatformPublicKeyCredentialAssertion {
                return [KotlinInt(value: 2), KotlinBoolean(value: false), x.credentialID.base64EncodedString(), x.rawClientDataJSON.base64EncodedString(),
                        x.rawAuthenticatorData.base64EncodedString(), x.signature.base64EncodedString(), x.userID.base64EncodedString(), NSNull()] as NSArray
            }
        }
        #endif
        return [KotlinInt(value: 3), "unsupported credential"] as NSArray
    }
}

private struct KotlinSignInWithApple: View {
    let m: NodeModel

    var body: some View {
        let a = m.args
        let labels: [SignInWithAppleButton.Label] = [.signIn, .continue, .signUp]
        let done = a.callback(4, box: { (x: NSArray) -> Any? in x })
        SignInWithAppleButton(labels[min(max(a.int(0), 0), 2)]) { request in
            let scopes = a.list(1).compactMap { $0 as? String }
            request.requestedScopes = scopes.map { $0 == "email" ? .email : .fullName }
            request.nonce = a.stringOpt(2)
            request.state = a.stringOpt(3)
        } onCompletion: { result in
            switch result {
            case .success(let auth): done(AuthEncoding.credential(auth.credential))
            case .failure(let e): done(AuthEncoding.failure(e))
            }
        }
    }
}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
private struct AuthorizationModifier: ViewModifier {
    let a: Args
    @Environment(\.authorizationController) private var controller

    func body(content: Content) -> some View {
        content.onChange(of: a.hashableOpt(0)) { _, _ in run() }
    }

    private func requests() -> [ASAuthorizationRequest] {
        a.list(1).compactMap { $0 as? [Any] }.compactMap { r -> ASAuthorizationRequest? in
            func s(_ i: Int) -> String { i < r.count ? r[i] as? String ?? "" : "" }
            switch (r.first as? NSNumber)?.intValue ?? -1 {
            case 0:
                let req = ASAuthorizationAppleIDProvider().createRequest()
                req.requestedScopes = (r.count > 1 ? r[1] as? [Any] ?? [] : []).compactMap { $0 as? String }.map { $0 == "email" ? .email : .fullName }
                req.nonce = r.count > 2 ? r[2] as? String : nil
                return req
            case 1:
                return ASAuthorizationPasswordProvider().createRequest()
            #if !os(tvOS) && !os(watchOS)
            case 2:
                guard #available(iOS 15.0, macOS 12.0, *) else { return nil }
                return ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier: s(1))
                    .createCredentialAssertionRequest(challenge: Data(base64Encoded: s(2)) ?? Data())
            case 3:
                guard #available(iOS 15.0, macOS 12.0, *) else { return nil }
                return ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier: s(1))
                    .createCredentialRegistrationRequest(challenge: Data(base64Encoded: s(2)) ?? Data(), name: s(3), userID: Data(base64Encoded: s(4)) ?? Data())
            #endif
            default: return nil
            }
        }
    }

    private func run() {
        let reqs = requests()
        guard !reqs.isEmpty else { return }
        let done = a.callback(4, box: { (x: NSArray) -> Any? in x })
        let autoFill = a.bool(2), immediate = a.bool(3)
        let controller = self.controller
        Task { @MainActor in
            do {
                let result: ASAuthorizationResult
                #if os(iOS) || os(visionOS)
                if autoFill, #available(iOS 16.0, *) {
                    result = try await controller.performAutoFillAssistedRequests(reqs)
                } else if immediate {
                    result = try await controller.performRequests(reqs, options: .preferImmediatelyAvailableCredentials)
                } else {
                    result = try await controller.performRequests(reqs)
                }
                #elseif os(macOS)
                result = immediate ? try await controller.performRequests(reqs, options: .preferImmediatelyAvailableCredentials)
                                   : try await controller.performRequests(reqs)
                #else
                result = try await controller.performRequests(reqs)
                #endif
                switch result {
                case .appleID(let c): done(AuthEncoding.appleID(c))
                case .password(let p): done([KotlinInt(value: 1), p.user, p.password] as NSArray)
                #if !os(tvOS) && !os(watchOS)
                case .passkeyAssertion(let x): done(AuthEncoding.credential(x))
                case .passkeyRegistration(let r): done(AuthEncoding.credential(r))
                #endif
                default: done([KotlinInt(value: 3), "unsupported credential"] as NSArray)
                }
            } catch {
                done(AuthEncoding.failure(error))
            }
        }
    }
}

#if !os(tvOS)
@available(iOS 16.4, macOS 13.3, watchOS 9.4, *)
private struct WebAuthenticationModifier: ViewModifier {
    let a: Args
    @Environment(\.webAuthenticationSession) private var session

    func body(content: Content) -> some View {
        content.onChange(of: a.hashableOpt(0)) { _, _ in
            guard let url = URL(string: a.string(1)) else { return }
            let done = a.callback(4, box: { (x: NSArray) -> Any? in x })
            let scheme = a.string(2), ephemeral = a.bool(3)
            let session = self.session
            Task { @MainActor in
                do {
                    let callback = try await session.authenticate(using: url, callbackURLScheme: scheme,
                                                                  preferredBrowserSession: ephemeral ? .ephemeral : .shared)
                    done([callback.absoluteString, NSNull()] as NSArray)
                } catch {
                    done([NSNull(), error.localizedDescription] as NSArray)
                }
            }
        }
    }
}
#endif

#if os(macOS)
@available(macOS 15.0, *)
private struct KotlinLocalAuthentication: View {
    let m: NodeModel

    var body: some View {
        let a = m.args
        let done = a.callback(1, box: { (s: String?) -> Any? in s ?? NSNull() })
        let result = { (r: Result<Void, any Error>) in
            if case .failure(let e) = r { done(e.localizedDescription) } else { done(nil) }
        }
        if let label = a.slotModel(3) {
            LocalAuthenticationView(reason: Text(a.string(0)), result: result) { NodeView(model: label) }
        } else {
            LocalAuthenticationView(a.stringOpt(2) ?? "Authenticate", reason: Text(a.string(0)), result: result)
        }
    }
}
#endif
