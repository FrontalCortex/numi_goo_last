import SwiftUI
import UIKit

// Yarış paneli ("Ustalık Yolu": kısım 7 Toplama Ustalığı, kısım 8 Çıkarma Ustalığı) ortak ölçü ve renkleri.
// Android kaynakları: race_bottom_sheet.xml, item_race_list.xml, race_lesson_bottom_sheet.xml,
// panel_race_fast_forward.xml, dimens.xml (race_sheet_*, lesson_sheet_*), drawable/bottom_sheet_background.xml,
// race_button_background.xml, button_background.xml, bg_panel_daily_question_continue.xml.

enum YarisStil {
    // MARK: Üst başlık (race_bottom_sheet.xml)

    /// race_sheet_header_padding
    static let baslikIcBosluk: CGFloat = 17
    /// race_sheet_title_size (sp → pt)
    static let baslikYazisi: CGFloat = 21
    /// closeButton: 24dp, metin "✕" 18sp
    static let kapatKutusu: CGFloat = 24
    static let kapatYazisi: CGFloat = 18
    /// race_sheet_rv_padding
    static let listeIcBosluk: CGFloat = 17

    // MARK: Satır (item_race_list.xml)

    static let satirIcBosluk: CGFloat = 16
    static let satirKoseYaricapi: CGFloat = 12
    static let satirAltBosluk: CGFloat = 12
    static let satirSimgeBoyu: CGFloat = 48
    static let satirSimgeBosluk: CGFloat = 16
    static let satirAdYazisi: CGFloat = 16
    static let satirDurumYazisi: CGFloat = 14
    static let durumYatayBosluk: CGFloat = 12
    static let durumDikeyBosluk: CGFloat = 6
    static let durumKoseYaricapi: CGFloat = 8
    static let kilitBoyu: CGFloat = 20
    static let kilitSagBosluk: CGFloat = 6

    // MARK: Alt panel (race_lesson_bottom_sheet.xml)

    /// bottom_sheet_background: üst köşeler 16dp
    static let altPanelKoseYaricapi: CGFloat = 16
    /// lesson_sheet_container_padding
    static let altPanelIcBosluk: CGFloat = 14
    /// lesson_sheet_content_start_margin / end_margin
    static let altPanelYatayBosluk: CGFloat = 8
    /// lesson_sheet_title_size, lesson_sheet_body_size
    static let altPanelBaslikYazisi: CGFloat = 21
    static let altPanelGovdeYazisi: CGFloat = 17
    /// lesson_sheet_title_padding_top, lesson_sheet_title_margin_bottom
    static let altPanelBaslikUstBosluk: CGFloat = 4
    static let altPanelBaslikAltBosluk: CGFloat = 6
    /// lesson_sheet_middle_row_margin_bottom
    static let altPanelOrtaAltBosluk: CGFloat = 14
    /// lesson_sheet_button_text_size
    static let altPanelDugmeYazisi: CGFloat = 14
    /// Düğme köşesi 15dp. MaterialButton'ın varsayılan 6dp üst/alt insetiyle kilitli (60dp) düğmenin
    /// görünen yüksekliği 48dp'dir; BAŞLAT / Tekrar dene düğmesi için de aynı değer alındı (yaklaşık).
    static let dugmeKoseYaricapi: CGFloat = 15
    static let dugmeGorunenYukseklik: CGFloat = 48
    /// race_sheet_key_button_width
    static let anahtarDugmeGenisligi: CGFloat = 120
    /// applyRaceKeyUnlockButtonStyle: simge 32dp
    static let anahtarSimgeBoyu: CGFloat = 32

    // MARK: İleri sar penceresi (panel_race_fast_forward.xml)

    static let pencereGenislikOrani: CGFloat = 0.88
    static let pencereKoseYaricapi: CGFloat = 18
    static let pencereIcBosluk: CGFloat = 20
    static let pencereKapatKutusu: CGFloat = 40
    static let pencereBaslikYazisi: CGFloat = 20
    static let pencereAnimasyonBoyu: CGFloat = 120
    static let pencereMesajYazisi: CGFloat = 14
    static let pencereDugmeYuksekligi: CGFloat = 60
    static let pencereDugmeEnaz: CGFloat = 160
    static let pencereDugmeYazisi: CGFloat = 18
    /// Android Lottie dosyası: mouse_trap_anim.json (autoPlay + loop)
    static let pencereAnimasyonu: String = "mouse_trap_anim"

    // MARK: Renkler (kaynak adı olmayanlar)

    /// 0xRRGGBB biçimindeki sabit renk.
    static func renk(_ deger: Int) -> Color {
        let k: Double = Double((deger >> 16) & 0xFF) / 255.0
        let y: Double = Double((deger >> 8) & 0xFF) / 255.0
        let m: Double = Double(deger & 0xFF) / 255.0
        return Color(.sRGB, red: k, green: y, blue: m, opacity: 1.0)
    }

    /// race_button_background (KİLİTLİ rozetinin zemini)
    static let kilitliRozetZemini: Color = YarisStil.renk(0x484A52)
    /// item_race_list.xml: lock simgesi tint
    static let kilitSimgesi: Color = YarisStil.renk(0xB0B0B0)
    /// panel_race_fast_forward.xml: mesaj rengi
    static let mesajRengi: Color = YarisStil.renk(0xFFECB3)
    /// Toast zemini (Android varsayılanı)
    static let toastZemini: Color = YarisStil.renk(0x323232)

    static func yazi(_ boyut: CGFloat) -> Font {
        return Tema.yazi(boyut)
    }
}

/// bottom_sheet_background: yalnızca üst iki köşesi yuvarlak dikdörtgen.
struct YarisUstKoseSekli: Shape {
    let yaricap: CGFloat

    func path(in rect: CGRect) -> Path {
        let koseler: UIRectCorner = [.topLeft, .topRight]
        let boyut: CGSize = CGSize(width: yaricap, height: yaricap)
        let yol: UIBezierPath = UIBezierPath(roundedRect: rect, byRoundingCorners: koseler, cornerRadii: boyut)
        return Path(yol.cgPath)
    }
}

/// Basılınca hafifçe koyulaşan düz düğme (Android: ripple yerine).
struct YarisBasmaStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.85 : 1.0)
    }
}
