import SwiftUI

// Paket D: kural tabloları, grup 3.
//
// Android: res/layout/fragment_tutorial.xml — BeadRuleExtractionTable (1420-1686).
// Kullanan öğreticiler: 17; 16 widgetVisibilityMap ile. Ortak parçalar: OgreticiBoncukTabloParcalari.swift.
// Seçici (OgreticiKuralAlani.swift → OgreticiKuralTablosu) `ad` olarak android:id'yi verir.
struct OgreticiTabloGrubu3: View {
    let ad: String

    var body: some View {
        OgreticiBoncukCikarmaTablosu()
    }
}

/// BeadRuleExtractionTable: "Çıkarılacak sayı → İşlem" + 6..9 için üçer etiket
/// (gider kırmızı, gelir yeşil, gider kırmızı).
struct OgreticiBoncukCikarmaTablosu: View {
    var body: some View {
        let satirlar: [OgreticiBoncukTabloSatiri] = OgreticiBoncukTabloSatirVeri.satirlar(
            sayilar: ["6", "7", "8", "9"], ilkNo: 23, gelirDizilimi: [false, true, false])
        OgreticiBoncukTabloKabugu {
            OgreticiBoncukTabloBasligi(sol: Metin.al("fragment_tutorial_text_21"), sag: Metin.al("fragment_tutorial_text_22"))
            OgreticiBoncukTabloSatirListesi(satirlar: satirlar)
        }
    }
}
