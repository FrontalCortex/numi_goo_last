import SwiftUI

// YER TUTUCU (Paket B, bkz. ios/docs/OGRETICI_PLANI.md): kural tabloları, grup 1.
//
// Android: res/layout/fragment_tutorial.xml — fiveRuleTable (fragment_tutorial.xml 36-216), tenRuleTable (399-617), tenRuleTableLinearLayout (651-831).
// Kullanan öğreticiler: 3, 4 (five), 5, 6 (ten), 7, 105 (tenLinear); 16 widgetVisibilityMap ile fiveRuleTable.
// Seçici (OgreticiKuralAlani.swift → OgreticiKuralTablosu) `ad` olarak android:id'yi verir.
// Hücre metinleri strings.xml'dedir (Metin.al); çerçeve/zemin değerleri ios/tools/gorsel-donustur/rapor.json.
struct OgreticiTabloGrubu1: View {
    let ad: String

    var body: some View {
        EmptyView()
    }
}
