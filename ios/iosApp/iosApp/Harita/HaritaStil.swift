import SwiftUI
import UIKit

// Harita klasörünün ortak görsel yardımcıları: ölçüler (Android dimens.xml ve layout XML'leri),
// yazı tipi, kaynak adı olmayan renkler, bilgi balonu ve yer tutucu.

enum HaritaStil {
    // MARK: Ölçüler (dp → pt birebir)

    /// map_sticky_padding_horizontal (item_header.xml yatay iç boşluğu)
    static let baslikYatayBosluk: CGFloat = 32
    /// map_item_header_padding
    static let baslikDikeyBosluk: CGFloat = 14
    /// map_sticky_recycler_padding_top (fragment_map.xml liste üst boşluğu)
    static let listeUstBosluk: CGFloat = 16
    /// map_list_padding_bottom
    static let listeAltBosluk: CGFloat = 92
    /// map_lesson_card_margin_horizontal (kart ve panel yatay boşluğu)
    static let kartYatayBosluk: CGFloat = 16
    /// map_lesson_card_margin_vertical
    static let kartDikeyBosluk: CGFloat = 6
    /// item_lesson_card.xml: cardCornerRadius
    static let kartKoseYaricapi: CGFloat = 16
    /// item_lesson_card.xml: strokeWidth
    static let kartCizgi: CGFloat = 2
    /// item_lesson_card.xml: lessonIcon
    static let kartSimgeBoyu: CGFloat = 40
    /// item_lesson_card.xml: lessonStateIcon, chestStar1..3
    static let kartDurumSimgesi: CGFloat = 22
    /// item_lesson_card.xml: lessonStepBar yüksekliği
    static let kartCubukYuksekligi: CGFloat = 10
    /// SegmentBarView: dilimler arası 4dp
    static let dilimBoslugu: CGFloat = 4
    /// item_race.xml: map_lesson_item_padding
    static let yarisSatirBoslugu: CGFloat = 17
    /// map_lesson_card_size
    static let yarisKartBoyu: CGFloat = 85
    /// map_race_icon_size
    static let yarisSimgeBoyu: CGFloat = 64
    /// main_toolbar_icon_back (kabuksuz açılışta geri düğmesinin kutusu)
    static let geriDugmesiBoyu: CGFloat = 30
    /// ic_back_arrow.xml: 24dp'lik vektör
    static let geriOkuBoyu: CGFloat = 24

    // Ders paneli (lesson_popover.xml, LessonAdapter.placeLessonPanel)

    /// lessonPanelCard: padding
    static let panelIcBosluk: CGFloat = 16
    /// GradientDrawable cornerRadius 18dp
    static let panelKoseYaricapi: CGFloat = 18
    /// setStroke(2dp)
    static let panelCizgi: CGFloat = 2
    /// lessonPanelArrow
    static let okGenisligi: CGFloat = 22
    static let okYuksekligi: CGFloat = 11
    /// placeLessonPanel: kart ile ok arası
    static let okKartArasi: CGFloat = 6
    /// placeLessonPanel: ekran kenarına en az uzaklık
    static let panelKenarPayi: CGFloat = 12
    /// placeLessonPanel: okun panelin kenarını örttüğü miktar
    static let okBindirme: CGFloat = 2
    /// actionButton: layout_height 56dp (dudak dahil), cornerRadius 14dp
    static let dugmeYuksekligi: CGFloat = 56
    static let dugmeKoseYaricapi: CGFloat = 14
    /// DepthButtons.DEPTH_DP
    static let dugmeDerinligi: CGFloat = 4
    /// LESSON_PANEL_DIM_ALPHA
    static let karartma: Double = 0.7

    // MARK: Yazı

    /// Android temasında bütün yazılar kalındır (Widget.NumiGoo.TextView: textStyle=bold).
    /// Uygulamanın ortak yazı tipi yardımcısına (Baloo 2) bağlıdır.
    static func yazi(_ boyut: CGFloat, kalin: Bool = true) -> Font {
        return Tema.yazi(boyut, kalin: kalin)
    }

    // MARK: Renk

    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler için).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// Göçen düğmenin dudağı: yüz renginin koyusu (Android: DepthButtons.darken, f = 0.78).
    static func dudak(_ renkAdi: String) -> Color {
        guard let kaynak = UIColor(named: renkAdi) else {
            return Color.black.opacity(0.3)
        }
        var k: CGFloat = 0
        var y: CGFloat = 0
        var m: CGFloat = 0
        var a: CGFloat = 0
        _ = kaynak.getRed(&k, green: &y, blue: &m, alpha: &a)
        let f: CGFloat = 0.78
        return Color(.sRGB, red: Double(k * f), green: Double(y * f), blue: Double(m * f), opacity: Double(a))
    }

    /// item_header.xml: #44A89C9C (alfa 0x44), çizgi ve yazı.
    static let baslikRengi: Color = HaritaStil.renk(0xA89C9C, alfa: Double(0x44) / 255.0)
    /// Toast / Snackbar zemini (Material: #323232).
    static let balonZemini: Color = HaritaStil.renk(0x323232)
}

/// Geri düğmesini (Android: lessonPartBackButton) uygulama kabuğu kendi üst çubuğunda mı çiziyor.
/// Android'de düğme MainActivity'nin üst çubuğundadır (activity_main.xml: currencyPanel);
/// kabuk bu değeri true yapar ve düğmeyi Kabuk/UstCubuk çizer. Harita tek başına açıldığında
/// (ekran galerisi) değer false kalır ve geri dönüş yolu kaybolmasın diye düğmeyi liste çizer.
struct HaritaGeriKabuktaAnahtari: EnvironmentKey {
    static let defaultValue: Bool = false
}

extension EnvironmentValues {
    var haritaGeriKabukta: Bool {
        get { return self[HaritaGeriKabuktaAnahtari.self] }
        set { self[HaritaGeriKabuktaAnahtari.self] = newValue }
    }
}

/// Ders kartına basınca hafif küçülme (lesson_card_press_animator.xml: basınca 150 ms'de 0,97;
/// bırakınca 250 ms'de taşarak 1).
struct HaritaKartBasmaStili: ButtonStyle {
    let olcek: CGFloat

    init(olcek: CGFloat = 0.97) {
        self.olcek = olcek
    }

    func makeBody(configuration: Configuration) -> some View {
        let basili: Bool = configuration.isPressed
        let animasyon: Animation = basili
            ? Animation.easeOut(duration: 0.15)
            : Animation.spring(response: 0.25, dampingFraction: 0.55)
        return configuration.label
            .scaleEffect(basili ? olcek : 1.0)
            .animation(animasyon, value: basili)
    }
}

/// Kısa süreli bilgi balonu. Toast: ortalanmış kapsül; Snackbar (LockedHint): alt kenarda,
/// tam genişlikte, 4pt köşeli, 5 satıra kadar.
struct HaritaBildirimGorunumu: View {
    let bildirim: HaritaBildirimi

    var body: some View {
        if bildirim.uzun {
            snackbar
        } else {
            toast
        }
    }

    private var toast: some View {
        Text(bildirim.metin)
            .font(HaritaStil.yazi(14, kalin: false))
            .foregroundColor(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(HaritaStil.balonZemini))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
    }

    private var snackbar: some View {
        Text(bildirim.metin)
            .font(HaritaStil.yazi(14, kalin: false))
            .foregroundColor(.white)
            .lineLimit(5)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(RoundedRectangle(cornerRadius: 4).fill(HaritaStil.balonZemini))
            .padding(8)
    }
}

/// Henüz yazılmamış ekranların (abaküs dersi, öğretici, yarış paneli, liderlik tablosu, mağaza)
/// yerine gösterilen yer tutucu (yalnızca harita kabuksuz açıldığında). Ortak kuralın verdiği
/// kararı metin olarak gösterir.
struct HaritaYerTutucuEkrani: View {
    let hedef: HaritaHedefi
    @Environment(\.dismiss) private var kapat

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            VStack(spacing: 16) {
                Text(hedef.baslik)
                    .font(HaritaStil.yazi(24))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                Text(hedef.aciklama)
                    .font(HaritaStil.yazi(17))
                    .foregroundColor(Color("lesson_completed"))
                    .multilineTextAlignment(.center)
                ayrintilar
                Text("Bu ekran henüz hazır değil.")
                    .font(HaritaStil.yazi(14, kalin: false))
                    .foregroundColor(Color.white.opacity(0.6))
                kapatDugmesi
            }
            .padding(24)
        }
    }

    private var ayrintilar: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(Array(hedef.ayrintilar.enumerated()), id: \.offset) { satir in
                Text(satir.element)
                    .font(HaritaStil.yazi(15, kalin: false))
                    .foregroundColor(.white)
            }
        }
    }

    private var kapatDugmesi: some View {
        Button(action: { kapat() }) {
            Text("Kapat")
                .font(HaritaStil.yazi(16))
                .foregroundColor(Color("background_color"))
                .padding(.horizontal, 32)
                .padding(.vertical, 12)
                .background(RoundedRectangle(cornerRadius: 8).fill(Color("lesson_completed")))
        }
        .buttonStyle(.plain)
        .padding(.top, 8)
    }
}
