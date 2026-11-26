import SwiftUI
import UIKit

// Harita klasörünün ortak görsel yardımcıları: ölçüler (Android dimens.xml), yazı tipi,
// layout XML'lerinde doğrudan yazılmış (kaynak adı olmayan) renkler ve küçük şekiller.

enum HaritaStil {
    // MARK: Ölçüler (dimens.xml; dp → pt birebir)

    /// map_sticky_padding_horizontal
    static let yapiskanYatayBosluk: CGFloat = 32
    /// map_sticky_recycler_padding_top
    static let listeUstBosluk: CGFloat = 16
    /// map_lesson_item_padding
    static let kartSatirBoslugu: CGFloat = 17
    /// map_lesson_card_size
    static let kartBoyu: CGFloat = 85
    /// map_lesson_outer_ring_size
    static let halkaBoyu: CGFloat = 102
    /// map_lesson_progress_thickness
    static let halkaKalinligi: CGFloat = 7
    /// LessonAdapter.applyStepSegments: setSegmentGapAngle(16f)
    static let dilimBoslugu: Double = 16
    /// map_lesson_icon_size
    static let simgeBoyu: CGFloat = 38
    /// map_lesson_chest_stars_size
    static let yildizAlani: CGFloat = 76
    /// map_race_icon_size
    static let yarisSimgeBoyu: CGFloat = 64
    /// map_fab_size
    static let soruDugmesiBoyu: CGFloat = 60
    /// main_toolbar_icon_back
    static let geriDugmesiBoyu: CGFloat = 30

    // MARK: Yazı

    /// Android temasında bütün yazılar kalındır (Widget.NumiGoo.TextView: textStyle=bold).
    /// Uygulamanın yazı tipi yardımcısı (Tema.yazi) hazır olunca buradan ona bağlanacak.
    static func yazi(_ boyut: CGFloat, kalin: Bool = true) -> Font {
        return Font.system(size: boyut, weight: kalin ? .bold : .regular, design: .rounded)
    }

    // MARK: Renk

    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler için).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// item_header.xml: #44A89C9C (alfa 0x44).
    static let baslikCizgiRengi: Color = HaritaStil.renk(0xA89C9C, alfa: Double(0x44) / 255.0)
    /// fragment_map.xml stickySectionUnit: #C7C5C5.
    static let yapiskanUstYaziRengi: Color = HaritaStil.renk(0xC7C5C5)
    /// fab_background.xml: #263C31.
    static let soruDugmesiZemini: Color = HaritaStil.renk(0x263C31)
    /// bottom_sheet_background.xml: #222.
    static let altPanelZemini: Color = HaritaStil.renk(0x222222)
    /// record_background.xml: #FFB703.
    static let rekorZemini: Color = HaritaStil.renk(0xFFB703)
}

/// Yalnızca üst köşeleri yuvarlatılmış dikdörtgen (bottom_sheet_background.xml).
struct HaritaUstYuvarlakSekil: Shape {
    let yaricap: CGFloat

    func path(in rect: CGRect) -> Path {
        let yol = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.topLeft, .topRight],
            cornerRadii: CGSize(width: yaricap, height: yaricap)
        )
        return Path(yol.cgPath)
    }
}

/// Kart basma animasyonu (lesson_state_animator.xml: basınca 0.80'e küçül, bırakınca geri yaylan).
struct HaritaKartBasmaStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.80 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.5), value: configuration.isPressed)
    }
}

/// Kısa süreli bilgi balonu (Android: Toast).
struct HaritaBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(HaritaStil.yazi(14, kalin: false))
            .foregroundColor(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(HaritaStil.renk(0x323232)))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
    }
}

/// Henüz yazılmamış ekranların (abaküs dersi, öğretici, yarış paneli, liderlik tablosu, mağaza)
/// yerine gösterilen yer tutucu. Ortak kuralın verdiği kararı metin olarak gösterir.
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
                    .foregroundColor(HaritaStil.yapiskanUstYaziRengi)
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
