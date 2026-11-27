import SwiftUI

// YER TUTUCU (Paket C, bkz. ios/docs/OGRETICI_PLANI.md): kural tabloları, grup 2.
//
// Android: res/layout/fragment_tutorial.xml — BeadRuleTable (832-1099), extractionFiveRuleTable (217-398), tenRuleExtractionTableLayout (1100-1419).
// Kullanan öğreticiler: 8, 9 (Bead), 11, 1100 (extractionFive), 12, 13, 14 (tenRuleExtraction); 16 widgetVisibilityMap ile tenRuleExtractionTableLayout.
// Seçici (OgreticiKuralAlani.swift → OgreticiKuralTablosu) `ad` olarak android:id'yi verir.
// Hücre metinleri strings.xml'dedir (Metin.al); çerçeve/zemin değerleri ios/tools/gorsel-donustur/rapor.json.
struct OgreticiTabloGrubu2: View {
    let ad: String

    var body: some View {
        EmptyView()
    }
}
