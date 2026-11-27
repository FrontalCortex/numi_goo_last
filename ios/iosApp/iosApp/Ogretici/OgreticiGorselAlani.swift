import SwiftUI
import UIKit

// Kör ders ve çarpma anlatımlarının görselleri (öğreticinin görsel alanı).
//
// Android: res/layout/fragment_tutorial.xml 1726-1772: ImageView'lar (blinding_tutorial2, rulesBookTutorial,
// blinding_tutorial1, blinding_tutorial3), hepsi parent'ın altında yatayda ortalı, alt boşluk
// tutorial_question_margin_bottom = 2 dp, varsayılan INVISIBLE. Zemin (`android:background`) olarak
// bir drawable taşırlar ve görünüm ölçüsüne esnetilir (en-boy korunmaz). DİKKAT: görünüm kimlikleri ile
// görsel adları çaprazdır:
//   blinding_tutorial2 (kimlik) -> blinding_tutorial1 (görsel)   245 x 520
//   rulesBookTutorial  (kimlik) -> rules_book_image   (görsel)   238 x 482 (tutorial_rules_book_*)
//   blinding_tutorial1 (kimlik) -> blinding_tutorial2 (görsel)   245 x 520
//   blinding_tutorial3 (kimlik) -> blinding_tutorial3 (görsel)   245 x 520
// Çizim sırası XML sırasıdır (sonraki üstte). Ölçüler tutorial_blinding_width/height (dimens.xml 91-92).
//
// TutorialFragment.applyTutorialViewScaling (359-381): dört görünüm de merkezden ölçeklenir
// (ekran yüksekliği < 700 dp ise 0,82; değilse 0,9). Yerleşim kutusu aynı kalır, yalnızca çizim küçülür.
//
// Görünürlük adım adım widgetOperations ile gelir (sahne.gorunumler; Android'de varsayılan INVISIBLE).
// 24. öğretici (TutorialFragment.kt 17153-17236): adım 7'de blinding_tutorial1 görünür ve tutorialText
// görselin ÜSTÜNE bağlanır (bottomToTop = blinding_tutorial1, bottomToBottom = UNSET); 8'de
// blinding_tutorial2 görünür / blinding_tutorial1 gizlenir; 9'da blinding_tutorial2 gizlenir; 15'te
// blinding_tutorial3 görünür; 16'da gizlenir. Metin bağı sonraki adımlarda da sürer (kalıcı).
//
// Metnin görselin üstüne alınması: adım metni OgreticiEkrani'nda (OgreticiAnaDuzen) çizilir; o görünüme
// `.modifier(OgreticiMetinGorselBagi(model:))` takılır (aşağıda). Bu düzenleyici metin kabının altına
// (görsel kutusunun yüksekliği + 2) kadar yer bırakır. Görsel alanı da metnin ALTINDA çizilir
// (Android'de görseller alt panelin altında kalır), bu yüzden OgreticiIcerigiGorunumu'nda
// OgreticiAnaDuzen'den ÖNCE yer alır.

/// Görsel kutusunun kimliği ve çizimi.
struct OgreticiGorselTanimi {
    /// fragment_tutorial.xml android:id
    let kimlik: String
    /// android:background drawable adı
    let gorsel: String
    /// dp
    let genislik: CGFloat
    let yukseklik: CGFloat
}

enum OgreticiGorselOlcu {
    /// tutorial_question_margin_bottom
    static let altBosluk: CGFloat = 2
    /// applyTutorialViewScaling: mediaScale
    static let olcek: CGFloat = 0.9
    static let kisaEkranOlcegi: CGFloat = 0.82
    /// Android screenHeightDp < 700 → kısa ekran
    static let kisaEkranSiniri: CGFloat = 700

    /// XML sırasıyla (alttan üste).
    static let gorseller: [OgreticiGorselTanimi] = [
        OgreticiGorselTanimi(kimlik: "blinding_tutorial2", gorsel: "blinding_tutorial1", genislik: 245, yukseklik: 520),
        OgreticiGorselTanimi(kimlik: "rulesBookTutorial", gorsel: "rules_book_image", genislik: 238, yukseklik: 482),
        OgreticiGorselTanimi(kimlik: "blinding_tutorial1", gorsel: "blinding_tutorial2", genislik: 245, yukseklik: 520),
        OgreticiGorselTanimi(kimlik: "blinding_tutorial3", gorsel: "blinding_tutorial3", genislik: 245, yukseklik: 520),
    ]

    /// Adım metninin görselin üstüne bağlandığı kutunun yüksekliği (nil: bağ yok).
    /// `yerlesimler`: birikmiş "görünüm:alan=değer;…" girdileri; yalnızca tutorialText'in
    /// bottomToTop bağı okunur, sonraki girdi öncekini ezer.
    static func metinBagiYuksekligi(_ yerlesimler: [String]) -> CGFloat? {
        var bag: String? = nil
        for girdi in yerlesimler {
            let onEk: String = "tutorialText:"
            if !girdi.hasPrefix(onEk) { continue }
            let govde: String = String(girdi.dropFirst(onEk.count))
            let parcalar: [String] = govde.components(separatedBy: ";")
            for parca in parcalar {
                let kv: [String] = parca.components(separatedBy: "=")
                if kv.count != 2 { continue }
                let alan: String = kv[0].trimmingCharacters(in: .whitespaces)
                let deger: String = kv[1].trimmingCharacters(in: .whitespaces)
                if alan == "bottomToTop" {
                    bag = deger
                } else if alan == "bottomToBottom" && deger != "UNSET" {
                    bag = nil
                }
            }
        }
        guard let kimlik = bag else { return nil }
        for g in gorseller where g.kimlik == kimlik {
            return g.yukseklik
        }
        return nil
    }

    /// Alt güvenli alanın yüksekliği (ana yerleşim güvenli alanın içinde çizilir).
    static func altGuvenliAlan() -> CGFloat {
        for sahne in UIApplication.shared.connectedScenes {
            guard let pencereSahnesi = sahne as? UIWindowScene else { continue }
            for pencere in pencereSahnesi.windows where pencere.isKeyWindow {
                return pencere.safeAreaInsets.bottom
            }
        }
        return 0
    }
}

struct OgreticiGorselAlani: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        GeometryReader { alan in
            gorselYigini(ekranYuksekligi: alan.size.height)
                .frame(width: alan.size.width, height: alan.size.height, alignment: .bottom)
        }
        .allowsHitTesting(false)
    }

    private func gorselYigini(ekranYuksekligi: CGFloat) -> some View {
        let olcek: CGFloat = ekranYuksekligi < OgreticiGorselOlcu.kisaEkranSiniri
            ? OgreticiGorselOlcu.kisaEkranOlcegi
            : OgreticiGorselOlcu.olcek
        return ZStack(alignment: .bottom) {
            ForEach(OgreticiGorselOlcu.gorseller, id: \.kimlik) { tanim in
                OgreticiGorselKutusu(
                    tanim: tanim,
                    gorunur: model.sahne.gorunurluk(tanim.kimlik, varsayilan: .gorunmez).cizilir,
                    olcek: olcek
                )
            }
        }
    }
}

/// Tek görsel: ölçüsüne esnetilir, merkezden ölçeklenir, alt orta.
struct OgreticiGorselKutusu: View {
    let tanim: OgreticiGorselTanimi
    let gorunur: Bool
    let olcek: CGFloat

    var body: some View {
        Image(tanim.gorsel)
            .resizable()
            .frame(width: tanim.genislik, height: tanim.yukseklik)
            .scaleEffect(olcek)
            .padding(.bottom, OgreticiGorselOlcu.altBosluk)
            .opacity(gorunur ? 1 : 0)
    }
}

// MARK: Metnin görsele bağlanması

/// Metin kabının alt kenarının (genel koordinat) ölçümü.
struct OgreticiMetinAltAnahtari: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

/// Adım metnini görsel kutusunun üstüne bağlar (Android ChangeConstraints: bottomToTop = görsel).
/// Metin kabının altına, kutunun üstüne kadar yer bırakır; bağ yoksa hiçbir şey yapmaz.
/// Kullanım (OgreticiAnaDuzen): `OgreticiMetinAlani(...).modifier(OgreticiMetinGorselBagi(model: model))`.
struct OgreticiMetinGorselBagi: ViewModifier {
    @ObservedObject var model: OgreticiGorunumModeli
    /// Metin kabının alt kenarının ekran içindeki konumu (ölçülen).
    @State private var kabAlti: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .padding(.bottom, ekBosluk)
            .background(olcer)
            .onPreferenceChange(OgreticiMetinAltAnahtari.self) { deger in
                kabAlti = deger
            }
    }

    private var olcer: some View {
        GeometryReader { alan in
            Color.clear.preference(
                key: OgreticiMetinAltAnahtari.self,
                value: alan.frame(in: .global).maxY
            )
        }
    }

    /// Kabın altı ile ekranın alt güvenli kenarı arasındaki mesafe, görsel kutusunun (yükseklik + alt
    /// boşluk) altında kalıyorsa farkı bırakır.
    private var ekBosluk: CGFloat {
        guard let yukseklik = OgreticiGorselOlcu.metinBagiYuksekligi(model.birikmisYerlesimler()) else {
            return 0
        }
        let ekranAlti: CGFloat = UIScreen.main.bounds.height - OgreticiGorselOlcu.altGuvenliAlan()
        let kabaMesafe: CGFloat = max(0, ekranAlti - kabAlti)
        let gereken: CGFloat = yukseklik + OgreticiGorselOlcu.altBosluk
        return max(0, gereken - kabaMesafe)
    }
}
