import SwiftUI

// Kural tabloları, grup 1 (Paket B): toplama tabloları.
//
// Android: res/layout/fragment_tutorial.xml — fiveRuleTable (36-216), tenRuleTable (399-617),
// tenRuleTableLinearLayout (651-831). Kullanan öğreticiler: 3, 4 (five), 5, 6 (ten), 7, 105 (tenLinear);
// 16 widgetVisibilityMap ile fiveRuleTable.
// Seçici (OgreticiKuralAlani.swift → OgreticiKuralTablosu) `ad` olarak android:id'yi verir; ölçekleme
// seçicide yapılır. Hücre/başlık/açıklama çizimi OgreticiToplamaTabloParcalari.swift'tedir.
struct OgreticiTabloGrubu1: View {
    let ad: String

    var body: some View {
        if let veri = OgreticiToplamaTabloVerisi.bul(ad) {
            OgreticiToplamaTablosu(veri: veri)
        }
    }
}
