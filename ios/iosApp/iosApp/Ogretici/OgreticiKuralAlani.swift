import SwiftUI
import UIKit

// Kural tablosu ve kural metni alanı (ekranın sağ üstü; adım metni bunun altından başlar).
//
// Android: fragment_tutorial.xml'deki yedi kural tablosu (hepsi parent'ın sağ üstüne bağlı, margin 1)
// ve rulesStepText (table_cell_border zemin, 20 yazı, üst 10, yatay 10, satır arası +8 dp, dikey padding 2),
// TutorialFragment.showStep (`when (tutorialNumber)`: öğreticinin tablosu her adımda
// rulesPanelVisibility ile; ötekiler widgetVisibilityMap ile), updateRulesStepText + markedRuleText
// ("[köşeli parantez]" içi #00BFFF), updateTutorialTextAnchor (tutorialText görünen ilk tablonun ya da
// kural metninin altına bağlanır), applyTutorialViewScaling / fitRulesTable (tablo ölçeği).
//
// Yerleşim kuralları (Android → burada):
//  - Kural metninin üstü `kuralMetniCapasi`na (rulesStepTextTopToBottomOf) bağlıdır; boşsa tenRuleTable,
//    105. öğreticide her zaman tenRuleTableLinearLayout. Çapa görünen tabloysa metin tablonun
//    (ölçeklenmemiş) altındadır; çapa GONE bir tabloysa ConstraintLayout GONE görünümü sıfır boyutlu
//    bir noktaya indirdiği için metin ekranın üstüne (üst boşluk 10) yaslanır; `guide_line_2_7` ise
//    üst kenardan ekran yüksekliğinin %22,5714'üdür.
//  - Tablo ölçeği yalnızca çizimi küçültür, yerleşim boyutunu değiştirmez (View.scaleX/Y gibi);
//    sağ üst köşe sabit. Taban 0,94; ekran yüksekliği < 700 ise 0,58; çıkış düğmesinin sağına sığmıyorsa
//    daha da küçülür (en az 0,3).
//  - Kaydırma yok: Android'de de tablolar ve metin kaydırılmaz; kısa ekranda yalnızca ölçek küçülür.
//
// SAHİPLİK: bu dosya (yerleşim) ve OgreticiKuralTablolari1.swift Paket B'nin; tabloların kendileri üç
// dosyadadır (OgreticiKuralTablolari1/2/3.swift), bkz. ios/docs/OGRETICI_PLANI.md.

/// Ekran ölçüleri (Android resources.configuration.screenHeightDp ve parent boyutu karşılığı).
enum OgreticiKuralOlcu {
    /// guide_line_2_7: layout_constraintGuide_percent
    static let kilavuzOrani: CGFloat = 0.225714
    /// Tablo ölçeği (applyTutorialViewScaling)
    static let tabloOlcegi: CGFloat = 0.94
    static let tabloOlcegiKisa: CGFloat = 0.58
    /// screenHeightDp < 700 → kısa ekran
    static let kisaEkranEsigi: CGFloat = 700
    /// fitRulesTable: en küçük ölçek
    static let enKucukOlcek: CGFloat = 0.3
    /// fitRulesTable: quitButton'un sağından bırakılan boşluk (4 dp) ve tablonun sağ kenar boşluğu (1 dp)
    static let cikisBosluk: CGFloat = 4
    static let tabloKenar: CGFloat = 1

    /// Güvenli alan düşülmüş ekran boyutu (Android'de sistem çubukları düşülmüş screenHeightDp).
    static var kullanilabilirAlan: CGSize {
        let ekran: CGRect = UIScreen.main.bounds
        let sahneler: [UIWindowScene] = UIApplication.shared.connectedScenes.compactMap { (s: UIScene) -> UIWindowScene? in
            return s as? UIWindowScene
        }
        let pencereler: [UIWindow] = sahneler.flatMap { (s: UIWindowScene) -> [UIWindow] in
            return s.windows
        }
        let anahtar: UIWindow? = pencereler.first(where: { (p: UIWindow) -> Bool in
            return p.isKeyWindow
        })
        let ek: UIEdgeInsets = anahtar?.safeAreaInsets ?? UIEdgeInsets.zero
        return CGSize(width: ekran.width, height: ekran.height - ek.top - ek.bottom)
    }
}

struct OgreticiKuralAlani: View {
    @ObservedObject var model: OgreticiGorunumModeli

    /// Android updateTutorialTextAnchor'daki sıra.
    static let tablolar: [String] = [
        "fiveRuleTable", "extractionFiveRuleTable", "tenRuleTable", "tenRuleTableLinearLayout",
        "BeadRuleTable", "tenRuleExtractionTableLayout", "BeadRuleExtractionTable",
    ]

    /// Kural metninin kılavuza bağlandığı çapa adı (android:id).
    static let kilavuzAdi: String = "guide_line_2_7"

    var body: some View {
        let tablo: String? = gorunenTablo
        let capa: String = metinCapasi
        let tabloAltinda: Bool = (tablo != nil) && (tablo == capa)
        return Group {
            if tabloAltinda {
                // Çapa görünen tablo: metin tablonun hemen altında.
                VStack(alignment: .trailing, spacing: 0) {
                    tabloParcasi(tablo)
                    metinParcasi
                }
            } else {
                // Çapa kılavuz ya da görünmeyen bir tablo: metin kendi yüksekliğine konur, tablodan bağımsız.
                ZStack(alignment: .top) {
                    tabloParcasi(tablo)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    metinParcasi
                        .padding(.top, metinUstBosluk(capa))
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .allowsHitTesting(false)
    }

    @ViewBuilder
    private func tabloParcasi(_ tablo: String?) -> some View {
        if let ad = tablo {
            OgreticiKuralTablosu(ad: ad)
                .padding(OgreticiKuralOlcu.tabloKenar)
        }
    }

    @ViewBuilder
    private var metinParcasi: some View {
        if let metin = model.adim.kuralMetni, model.adim.kuralMetniGorunurlugu.yerKaplar {
            OgreticiKuralMetni(isaretli: metin)
                .opacity(model.adim.kuralMetniGorunurlugu.cizilir ? 1 : 0)
                .padding(.top, 10)
                .padding(.horizontal, 10)
        }
    }

    /// updateRulesStepText: 105'te hep tenRuleTableLinearLayout, yoksa adımın çapası, yoksa tenRuleTable.
    private var metinCapasi: String {
        if model.baslangic.ogreticiNo == 105 {
            return "tenRuleTableLinearLayout"
        }
        return model.adim.kuralMetniCapasi ?? "tenRuleTable"
    }

    /// Çapa tablo değilse metnin üst kenarının ofseti: kılavuzda ekran yüksekliğinin %22,5714'ü,
    /// görünmeyen (GONE) tabloda 0.
    private func metinUstBosluk(_ capa: String) -> CGFloat {
        if capa == OgreticiKuralAlani.kilavuzAdi {
            let yukseklik: CGFloat = OgreticiKuralOlcu.kullanilabilirAlan.height
            return yukseklik * OgreticiKuralOlcu.kilavuzOrani
        }
        return 0
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

/// Tablo seçici: her grup kendi paketinin dosyasındadır. Yedi tablonun hepsi aynı ölçeklemeden geçer.
struct OgreticiKuralTablosu: View {
    let ad: String

    var body: some View {
        OgreticiTabloOlcekleyici {
            tablo
        }
    }

    @ViewBuilder
    private var tablo: some View {
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

/// Tablonun genişliğini ölçmek için.
struct OgreticiTabloGenislikAnahtari: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

/// applyTutorialViewScaling + fitRulesTable: sağ üst köşe sabit, yalnızca çizim küçülür (yerleşim boyutu aynı kalır).
struct OgreticiTabloOlcekleyici<Icerik: View>: View {
    let icerik: Icerik
    @State private var genislik: CGFloat = 0

    init(@ViewBuilder _ icerik: () -> Icerik) {
        self.icerik = icerik()
    }

    var body: some View {
        icerik
            .background(
                GeometryReader { (g: GeometryProxy) in
                    Color.clear.preference(key: OgreticiTabloGenislikAnahtari.self, value: g.size.width)
                }
            )
            .onPreferenceChange(OgreticiTabloGenislikAnahtari.self) { (w: CGFloat) in
                genislik = w
            }
            .scaleEffect(olcek, anchor: .topTrailing)
    }

    private var olcek: CGFloat {
        let alan: CGSize = OgreticiKuralOlcu.kullanilabilirAlan
        let kisa: Bool = alan.height < OgreticiKuralOlcu.kisaEkranEsigi
        let taban: CGFloat = kisa ? OgreticiKuralOlcu.tabloOlcegiKisa : OgreticiKuralOlcu.tabloOlcegi
        if genislik <= 0 {
            return taban
        }
        // fitRulesTable: (görünen sağ kenar - çıkış düğmesinin sağı - 4) / tablo genişliği
        let gorunenSag: CGFloat = alan.width - OgreticiKuralOlcu.tabloKenar
        let guvenliSol: CGFloat = OgreticiOlcu.cikisBaslangic + OgreticiOlcu.cikisBoyu + OgreticiKuralOlcu.cikisBosluk
        let sigan: CGFloat = (gorunenSag - guvenliSol) / genislik
        return max(min(taban, sigan), OgreticiKuralOlcu.enKucukOlcek)
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
            .lineSpacing(8)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 2)
            // table_cell_border: #222 zemin, 3 dp siyah çerçeve, 3 dp köşe
            .background(OgreticiToplamaCerceve())
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
