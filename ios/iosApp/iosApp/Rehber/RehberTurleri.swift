import SwiftUI

// Ders içi Sobi rehberinin ortak türleri.
//
// Android karşılıkları:
//   GuideHelper.kt  → GuideContent (rehberin bir sayfası)      → RehberAdimi
//   GuidePanelView.kt / view_guide_panel.xml (düzen ölçüleri)    → RehberDuzeni
//   Rehberin işaret ettiği ekran öğeleri (firstNumberText, kontrolButton, rulesBookButton …) → RehberHedefi
//
// Ekrandaki bir öğeyi işaret etmek için öğenin üstüne `.rehberHedefi(.kontrolDugmesi)` yazılır; öğenin
// çerçevesi bir `anchorPreference` ile yukarı taşınır ve ekranın `overlayPreferenceValue`'sundaki
// `RehberKatmani` tarafından okunur. Öğe ekranda yoksa (ya da henüz yazılmadıysa; örneğin kural kitabı
// düğmesi) rehber o sayfayı işaretsiz gösterir ve ileri düğmesi her zaman çalışır.

/// Rehberin işaret edebildiği ekran öğeleri.
enum RehberHedefi: Hashable {
    /// firstNumberText (sayı kartındaki ilk sayı).
    case ilkSayi
    /// secondNumberText (sayı kartındaki ikinci sayı).
    case ikinciSayi
    /// kontrolButton ("Kontrol Et" / kör derste "Başla" ile aynı yer).
    case kontrolDugmesi
    /// rulesPanelButton (sihirli değnek, kural tablosu).
    case kuralPaneliDugmesi
    /// rulesBookButton (kurallar kitabı).
    case kuralKitabiDugmesi
    /// skipStepButton (kör derste "sayıyı geç" oku).
    case adimAtlaDugmesi
    /// abacusModeButton (abaküs ölçekleme / ayar düğmesi).
    case abakusAyarDugmesi
    /// abacusContainer: rehber panelinin altı bu çerçevenin üstüne oturur (yerleşim için; vurgulanmaz).
    case abakusAlani
    /// Ders panelindeki "Rekor" satırı (recordLayout; maraton rehberinin son hedefi).
    case rekorSatiri
    /// Haritadaki ders panelinin kartı (lessonPanelCard): maraton rehberi balonu bunun altına oturur.
    case dersPaneli
}

/// Hedef çerçevelerini yukarı taşıyan tercih anahtarı.
struct RehberHedefAnahtari: PreferenceKey {
    static var defaultValue: [RehberHedefi: Anchor<CGRect>] = [:]

    static func reduce(value: inout [RehberHedefi: Anchor<CGRect>], nextValue: () -> [RehberHedefi: Anchor<CGRect>]) {
        value.merge(nextValue(), uniquingKeysWith: { _, yeni in yeni })
    }
}

extension View {
    /// Bu görünümün çerçevesini rehber için `hedef` olarak bildirir.
    func rehberHedefi(_ hedef: RehberHedefi) -> some View {
        return anchorPreference(key: RehberHedefAnahtari.self, value: .bounds) {
            (cerceve: Anchor<CGRect>) -> [RehberHedefi: Anchor<CGRect>] in
            return [hedef: cerceve]
        }
    }
}

/// Rehberin bir sayfası (Android: GuideContent).
struct RehberAdimi {
    /// Sobi'nin hâli (Android: BunnyMascotView.Emote adı: "TEACH_TALK", "TEACH_POINT", "TEACH_WARN").
    let emote: String
    let metin: String
    /// Bu sayfada işaret edilen öğe (Android: bubbleAnimationTarget / requiredClickTarget).
    var hedef: RehberHedefi? = nil
    /// Android: bubbleAnimationColor / bubbleAnimationTintLight.
    var vurguRengi: Color? = nil
    var vurguAcikRengi: Color? = nil
    /// Android: bubbleAnimationMaxScale.
    var vurguOlcegi: CGFloat = 1.4
    /// Sayfa gösterilirken oynayan boncuklar, örn. "rod4BottomBead4", "rod4TopBead" (Android: beadIds).
    var boncuklar: [String] = []
    /// Geri dönülürken oynayanlar; nil ise `boncuklar` (Android: backBeadIds).
    var geriBoncuklari: [String]? = nil
    /// İleri düğmesine basılınca bu sayfadan çıkarken oynayanlar (Android: finishBeadIds).
    var bitisBoncuklari: [String] = []
    /// Kullanıcı hedefe dokunmadan ilerleyemez (Android: requiredClickTarget != null).
    var tiklamaGerekli: Bool = false
    /// Hedefe dokununca rehber bir sonraki sayfaya geçer; değilse rehber kapanır
    /// (Android: requiredClickAdvancesGuide).
    var tiklamaIlerletir: Bool = false
    /// Kural tablosundan bir kural seçilene kadar ilerlenmez (Android: waitForRulesTableSelection).
    var kuralSecimiBekler: Bool = false
}

/// Panelin yatay konumu: giriş tarafında bekler, içeri kayar, sola çıkar.
enum RehberPanelKonumu {
    case bekliyor
    case icerde
    case cikti
}

/// Panelin ekrandaki yerleşim ölçüleri (dp → pt birebir).
struct RehberDuzeni {
    /// Panelin soldan ve sağdan ekran kenarına uzaklığı.
    let yatayBosluk: CGFloat
    /// panelContent alt dolgusu (üst ve yanlar her zaman 10).
    let altDolgu: CGFloat
    /// Sobi'nin üstündeki boşluk (view_guide_panel.xml: 16; abaküs dersinin kendi düzeninde 0).
    let maskotUstBosluk: CGFloat
    /// Yazının sağ boşluğu (view_guide_panel.xml: 16; abaküs dersinin kendi düzeninde 0).
    let metinSonBosluk: CGFloat
    /// GuidePanelView: tek sayfada nokta göstergesi gizlenir.
    let tekAdimdaNoktaGizli: Bool
    /// Harita rehberi sağdan girer (translationX = +ekran genişliği); ders rehberleri soldan.
    let girisSagdan: Bool
    /// Giriş ve çıkış kayma süreleri (sn).
    let girisSuresi: Double
    let cikisSuresi: Double

    /// AbacusFragment: fragment_abacus.xml içindeki panelContent (soldan 300 ms girer, sola 300 ms çıkar).
    static let abakusDersi: RehberDuzeni = RehberDuzeni(
        yatayBosluk: 10, altDolgu: 10, maskotUstBosluk: 0, metinSonBosluk: 0,
        tekAdimdaNoktaGizli: false, girisSagdan: false, girisSuresi: 0.3, cikisSuresi: 0.3
    )

    /// BlindingLessonFragment: view_guide_panel.xml include'u (soldan girer, sola çıkar).
    static let korDersi: RehberDuzeni = RehberDuzeni(
        yatayBosluk: 10, altDolgu: 20, maskotUstBosluk: 16, metinSonBosluk: 16,
        tekAdimdaNoktaGizli: false, girisSagdan: false, girisSuresi: 0.3, cikisSuresi: 0.3
    )

    /// MapFragment: GuidePanelView (sağdan 500 ms girer, sola 300 ms çıkar).
    static let harita: RehberDuzeni = RehberDuzeni(
        yatayBosluk: 0, altDolgu: 20, maskotUstBosluk: 16, metinSonBosluk: 16,
        tekAdimdaNoktaGizli: true, girisSagdan: true, girisSuresi: 0.5, cikisSuresi: 0.3
    )
}

/// Panelin dikeyde nereye oturduğu.
enum RehberYeri {
    /// Panelin alt kenarı abaküs alanının üst kenarının `aralik` üstüne oturur (Android: layout_constraintBottom_toTopOf
    /// abacusContainer; kör derste marginBottom 16).
    case abakusUstu(aralik: CGFloat)
    /// Panelin üst kenarı `hedef` çerçevesinin `aralik` altına oturur; hedef yoksa ekranın altına dayanır
    /// (Android: MapFragment.placeGuideBalloonBelow).
    case hedefAltinda(hedef: RehberHedefi, aralik: CGFloat)
}

/// Rehberde kullanılan renkler.
enum RehberRenk {
    /// 0xRRGGBB → Color.
    static func renk(_ rgb: UInt32) -> Color {
        let kirmizi: Double = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil: Double = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi: Double = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: 1.0)
    }

    /// guide_panel_background: #222.
    static let panel: Color = renk(0x222222)
}
