import SwiftUI

// Paket C: kural tabloları, grup 2.
//
// Android: res/layout/fragment_tutorial.xml — BeadRuleTable (832-1099), extractionFiveRuleTable (217-398),
// tenRuleExtractionTableLayout (1100-1419). Ortak parçalar: OgreticiBoncukTabloParcalari.swift.
// Kullanan öğreticiler: 8, 9 (Bead), 11, 1100 (extractionFive), 12, 13, 14 (tenRuleExtraction); 16 widgetVisibilityMap ile tenRuleExtractionTableLayout.
// Seçici (OgreticiKuralAlani.swift → OgreticiKuralTablosu) `ad` olarak android:id'yi verir.
// Basitleştirme: Android TableLayout(stretchColumns="*") sütunları doğal genişlik + eşit pay ile
// büyütür; burada sütunlar eşit genişlikte (hücre içerikleri aynı genişlikte olduğundan fark yok).
struct OgreticiTabloGrubu2: View {
    let ad: String

    var body: some View {
        switch ad {
        case "BeadRuleTable":
            OgreticiBoncukTablosu()
        case "extractionFiveRuleTable":
            OgreticiCikarmaBesTablosu()
        default:
            OgreticiCikarmaOnTablosu()
        }
    }
}

/// BeadRuleTable: "Eklenecek sayı → İşlem" + 6..9 için üçer işlem etiketi (gelir yeşil, gider kırmızı).
struct OgreticiBoncukTablosu: View {
    var body: some View {
        let satirlar: [OgreticiBoncukTabloSatiri] = OgreticiBoncukTabloSatirVeri.satirlar(
            sayilar: ["6", "7", "8", "9"], ilkNo: 7, gelirDizilimi: [true, false, true])
        OgreticiBoncukTabloKabugu {
            OgreticiBoncukTabloBasligi(sol: Metin.al("fragment_tutorial_text_5"), sag: Metin.al("fragment_tutorial_text_6"))
            OgreticiBoncukTabloSatirListesi(satirlar: satirlar)
        }
    }
}

/// extractionFiveRuleTable: "Çıkarılacak sayı → Kardeşi", 2 x 2 hücre, açıklama.
struct OgreticiCikarmaBesTablosu: View {
    var body: some View {
        let ust: [OgreticiBoncukTabloCifti] = OgreticiBoncukTabloVeri.ciftler([["1", "4"], ["2", "3"]])
        let alt: [OgreticiBoncukTabloCifti] = OgreticiBoncukTabloVeri.ciftler([["3", "2"], ["4", "1"]])
        OgreticiBoncukTabloKabugu {
            OgreticiBoncukTabloBasligi(
                sol: Metin.al("fragment_tutorial_extraction_five_text"), sag: Metin.al("fragment_tutorial_text_2"))
            OgreticiBoncukTabloHucreSatiri(ciftler: ust)
            OgreticiBoncukTabloHucreSatiri(ciftler: alt)
            OgreticiBoncukTabloAciklamasi(metin: Metin.al("fragment_tutorial_extraction_five_rule_description_text"))
        }
    }
}

/// tenRuleExtractionTableLayout: "Çıkarılacak sayı → Kardeşi", 3 x 3 kardeş çifti, açıklama.
struct OgreticiCikarmaOnTablosu: View {
    var body: some View {
        let s1: [OgreticiBoncukTabloCifti] = OgreticiBoncukTabloVeri.ciftler([["1", "9"], ["2", "8"], ["3", "7"]])
        let s2: [OgreticiBoncukTabloCifti] = OgreticiBoncukTabloVeri.ciftler([["4", "6"], ["5", "5"], ["6", "4"]])
        let s3: [OgreticiBoncukTabloCifti] = OgreticiBoncukTabloVeri.ciftler([["7", "3"], ["8", "2"], ["9", "1"]])
        OgreticiBoncukTabloKabugu {
            OgreticiBoncukTabloBasligi(sol: Metin.al("fragment_tutorial_text_19"), sag: Metin.al("fragment_tutorial_text_20"))
            OgreticiBoncukTabloHucreSatiri(ciftler: s1)
            OgreticiBoncukTabloHucreSatiri(ciftler: s2)
            OgreticiBoncukTabloHucreSatiri(ciftler: s3)
            OgreticiBoncukTabloAciklamasi(metin: Metin.al("fragment_tutorial_extraction_ten_rule_description_text"))
        }
    }
}
