// FinanceKitUI for Compose SwiftUI: AddOrderToWalletButton and TransactionPicker (runtime AppFrameworks.kt). Add
// this file to the app target and call `ComposeIntegrations.registerFinanceKitUI()` at launch.
import SwiftUI

#if os(iOS)
import FinanceKit
import FinanceKitUI

public extension ComposeIntegrations {
    @MainActor static func registerFinanceKitUI() {
        guard #available(iOS 17.4, *) else { return }
        ComposeRegistry.views[200_347] = { m in
            let a = m.args
            let report = a.callback(1, box: { (s: String) -> Any? in s })
            return ModChain.apply(AddOrderToWalletButton(signedArchive: Data(base64Encoded: a.string(0)) ?? Data()) { result in
                switch result {
                case .success(let r): report("\(r)")
                case .failure(let e): report(e.localizedDescription)
                }
            }, m, 0)
        }
        ComposeRegistry.modifiers[200_351] = { v, a, _ in
            AnyView(v.addOrderToWalletButtonStyle(a.int(0) == 1 ? .blackOutline : .black))
        }
        if #available(iOS 18.0, *) {
            ComposeRegistry.views[200_348] = { m in ModChain.apply(KotlinTransactionPicker(m: m), m, 0) }
            ComposeRegistry.modifiers[200_352] = { v, a, _ in AnyView(v.modifier(TransactionPickerSheet(a: a))) }
        }
    }
}

@available(iOS 18.0, *)
enum TransactionEncoding {
    static func encode(_ list: [FinanceKit.Transaction]) -> NSArray {
        list.map { t -> NSArray in
            [t.id.uuidString, t.transactionDescription, (t.merchantName as Any?) ?? NSNull(),
             KotlinDouble(value: NSDecimalNumber(decimal: t.transactionAmount.amount).doubleValue), t.transactionAmount.currencyCode,
             KotlinDouble(value: t.transactionDate.timeIntervalSince1970), KotlinBoolean(value: t.creditDebitIndicator == .credit)] as NSArray
        } as NSArray
    }
}

@available(iOS 18.0, *)
private struct KotlinTransactionPicker: View {
    let m: NodeModel
    @State private var selection: [FinanceKit.Transaction] = []

    var body: some View {
        let a = m.args
        TransactionPicker(selection: $selection) { SlotView(model: a.slotModel(1)) }
            .onChange(of: selection.map(\.id)) { _, _ in a.callback(0, box: { (x: NSArray) -> Any? in x })(TransactionEncoding.encode(selection)) }
    }
}

@available(iOS 18.0, *)
private struct TransactionPickerSheet: ViewModifier {
    let a: Args
    @State private var selection: [FinanceKit.Transaction] = []

    func body(content: Content) -> some View {
        content.transactionPicker(isPresented: a.boolBinding(0), selection: $selection)
            .onChange(of: selection.map(\.id)) { _, _ in a.callback(2, box: { (x: NSArray) -> Any? in x })(TransactionEncoding.encode(selection)) }
    }
}
#endif
