import SwiftUI

// Kural tablosu ve kural metni alanı (ekranın sağ üstü; adım metni bunun altından başlar).
//
// Android: fragment_tutorial.xml'deki yedi kural tablosu (hepsi parent'ın sağ üstüne bağlı, margin 1)
// ve rulesStepText (tablonun altında; table_cell_border zemin, 20 yazı, üst 10, yatay 10),
// TutorialFragment.showStep (`when (tutorialNumber)`: öğreticinin tablosu her adımda
// rulesPanelVisibility ile; ötekiler widgetVisibilityMap ile), updateRulesStepText + markedRuleText
// ("[köşeli parantez]" içi #00BFFF), updateTutorialTextAnchor (tutorialText görünen ilk tablonun ya da
// kural metninin altına bağlanır).
//
// SAHİPLİK: bu dosya (yerleşim) ve OgreticiKuralTablolari1.swift Paket B'nin; tabloların kendileri üç
// dosyadadır (OgreticiKuralTablolari1/2/3.swift), bkz. ios/docs/OGRETICI_PLANI.md.
// Eksikler (Paket B): kural metninin `guide_line_2_7`'ye (ekran yüksekliğinin %22,57'si) bağlandığı
// adımlar (kuralMetniCapasi), 105'te kural metninin tenRuleTableLinearLayout'a bağlanması, tablo
// ölçeği (applyTutorialViewScaling: 0,94; kısa ekranda 0,58; çıkış düğmesine sığdırma: fitRulesTable).

struct OgreticiKuralAlani: View {
    @ObservedObject var model: OgreticiGorunumModeli

    /// Android updateTutorialTextAnchor'daki sıra.
    static let tablolar: [String] = [
        "fiveRuleTable", "extractionFiveRuleTable", "tenRuleTable", "tenRuleTableLinearLayout",
        "BeadRuleTable", "tenRuleExtractionTableLayout", "BeadRuleExtractionTable",
    ]

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            if let ad = gorunenTablo {
                OgreticiKuralTablosu(ad: ad)
                    .padding(1)
            }
            if let metin = model.adim.kuralMetni, model.adim.kuralMetniGorunurlugu.yerKaplar {
                OgreticiKuralMetni(isaretli: metin)
                    .opacity(model.adim.kuralMetniGorunurlugu.cizilir ? 1 : 0)
                    .padding(.top, 10)
                    .padding(.horizontal, 10)
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .allowsHitTesting(false)
    }

    /// Görünen ilk tablo. Öğreticinin kendi tablosu her adımda rulesPanelVisibility'yi alır;
    /// ötekilerin görünürlüğü işlemlerle (widgetVisibilityMap) değişir, varsayılanı GONE.
    private var gorunenTablo: String? {
        let sahne: OgreticiSahneVerisi = model.sahne
        for ad in OgreticiKuralAlani.tablolar {
            var g: OgreticiGorunurlukTuru = sahne.gorunurluk(ad, varsayilan: .yok)
            if ad == sahne.kuralTablosu {
                g = sahne.kuralTablosuGorunurlugu
            }
            if g.cizilir {
                return ad
            }
        }
        return nil
    }
}

/// Tablo seçici: her grup kendi paketinin dosyasındadır.
struct OgreticiKuralTablosu: View {
    let ad: String

    var body: some View {
        switch ad {
        case "fiveRuleTable", "tenRuleTable", "tenRuleTableLinearLayout":
            OgreticiTabloGrubu1(ad: ad)
        case "BeadRuleTable", "extractionFiveRuleTable", "tenRuleExtractionTableLayout":
            OgreticiTabloGrubu2(ad: ad)
        default:
            OgreticiTabloGrubu3(ad: ad)
        }
    }
}

/// rulesStepText: "[köşeli parantez]" içi #00BFFF, parantezler atılır (markedRuleText).
struct OgreticiKuralMetni: View {
    let isaretli: String

    /// RULE_HIGHLIGHT_COLOR #00BFFF
    static let vurgu = Color(red: 0x00 / 255.0, green: 0xBF / 255.0, blue: 0xFF / 255.0)

    var body: some View {
        metin
            .font(Tema.yazi(OgreticiOlcu.metinYazi))
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 2)
            // table_cell_border (Paket B: çözülmüş değerleri ios/tools/gorsel-donustur/rapor.json'dan alın).
            .overlay(Rectangle().stroke(Color.white.opacity(0.6), lineWidth: 1))
    }

    private var metin: Text {
        var sonuc: Text = Text("")
        var parca: String = ""
        var vurgulu: Bool = false
        for harf in isaretli {
            if harf == "[" {
                sonuc = sonuc + Text(parca).foregroundColor(Color.white)
                parca = ""
                vurgulu = true
            } else if harf == "]" && vurgulu {
                sonuc = sonuc + Text(parca).foregroundColor(OgreticiKuralMetni.vurgu)
                parca = ""
                vurgulu = false
            } else {
                parca.append(harf)
            }
        }
        // Kapanmamış parantez: Android'de de boyanmaz.
        sonuc = sonuc + Text(parca).foregroundColor(Color.white)
        return sonuc
    }
}
