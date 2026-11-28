import SwiftUI
import UIKit

// Plan karşılaştırma ekranının ortak görsel yardımcıları: Android'de layout XML'lerinde doğrudan
// yazılmış renkler, şekil çizimleri (res/drawable/bg_*.xml) ve strings.xml'de olmayan iOS metinleri.

enum PlanStil {
    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler için).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi: Double = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil: Double = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi: Double = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// bg_blue_rounded_button: düğme yüzü #3D4DFF.
    static let dugmeYuzu: Color = PlanStil.renk(0x3D4DFF)
    /// DepthButtons.darken(#3D4DFF, 0.78): dudak rengi (#2F3CC6).
    static let dugmeDudagi: Color = PlanStil.renk(0x2F3CC6)
    /// Karşılaştırma tablosunun Pro sütun zemini (bg_super_column, #F2F3FF).
    static let proSutunu: Color = PlanStil.renk(0xF2F3FF)
    /// Satır yazısı ve ikincil metinler (#666666).
    static let ikincilYazi: Color = PlanStil.renk(0x666666)
    /// Kart başlığı ve fiyat (#333333).
    static let koyuYazi: Color = PlanStil.renk(0x333333)
    /// "Ücretsiz" sütununun yazı ve tik rengi (#4A4A4A).
    static let ucretsizYazi: Color = PlanStil.renk(0x4A4A4A)
    /// Plan kartındaki tikler (#00B0FF).
    static let kartTiki: Color = PlanStil.renk(0x00B0FF)
    /// Seçili kartın köşe rozeti (bg_super_badge, #00E676).
    static let secimRozeti: Color = PlanStil.renk(0x00E676)

    /// bg_shop_super_badge: soldan sağa #1DF0A4 → #3A82FF → #D558FF.
    static var rozetDegradesi: LinearGradient {
        let renkler: [Color] = [PlanStil.renk(0x1DF0A4), PlanStil.renk(0x3A82FF), PlanStil.renk(0xD558FF)]
        return LinearGradient(colors: renkler, startPoint: .leading, endPoint: .trailing)
    }

    /// bg_plan_bireysel: seçili kartın çerçevesi, soldan sağa #00E676 → #00B0FF → #D500F9.
    static var kartCercevesi: LinearGradient {
        let renkler: [Color] = [PlanStil.renk(0x00E676), PlanStil.renk(0x00B0FF), PlanStil.renk(0xD500F9)]
        return LinearGradient(colors: renkler, startPoint: .leading, endPoint: .trailing)
    }
}

/// Yalnızca üst köşeleri yuvarlatılmış dikdörtgen (bg_bottom_sheet_white.xml: 24dp).
struct PlanUstYuvarlakSekil: Shape {
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

/// "PRO" rozeti (bg_shop_super_badge: köşe 6dp, degrade). Sağ üstte 14sp, tablo başlığında 12sp.
struct PlanProRozeti: View {
    let boyut: CGFloat
    let yatayBosluk: CGFloat
    let dikeyBosluk: CGFloat

    var body: some View {
        Text(Metin.al("fragment_pro_diffirent_super_badge"))
            .font(Tema.yazi(boyut))
            .foregroundColor(Color.white)
            .padding(.horizontal, yatayBosluk)
            .padding(.vertical, dikeyBosluk)
            .background(RoundedRectangle(cornerRadius: 6).fill(PlanStil.rozetDegradesi))
    }
}

/// Android'de karşılığı olmayan iOS metinleri. App Store kuralları (3.1.2) abonelik ekranında fiyatı ve
/// dönemi, otomatik yenilemeyi, Kullanım Şartları ve Gizlilik Politikası bağlantılarını ve satın alımları
/// geri yükleme yolunu ister; Android'de (Play) bunlar yoktur. Metinler `<dil>.lproj/IosOzel.strings`
/// içindedir (`ios_` önekli anahtarlar); dil değişiminde yeniden okunsun diye hesaplanan özelliklerdir.
enum PlanMetin {
    static var proAktif: String { Metin.al("ios_plan_pro_aktif") }
    static var aboneligiYonet: String { Metin.al("ios_plan_aboneligi_yonet") }
    static var kullanimSartlari: String { Metin.al("ios_plan_kullanim_sartlari") }
    static var gizlilikPolitikasi: String { Metin.al("ios_plan_gizlilik_politikasi") }
    static var geriYukle: String { Metin.al("ios_magaza_geri_yukle") }
    static var geriYuklendi: String { Metin.al("ios_magaza_geri_yuklendi") }
    static var geriYuklenecekYok: String { Metin.al("ios_magaza_geri_yuklenecek_yok") }
    static var zatenSahip: String { Metin.al("ios_plan_zaten_sahip") }
    /// Apple'ın abonelik yönetim sayfası (Ayarlar > Apple Kimliği > Abonelikler ile aynı yer).
    static let abonelikYonetimAdresi: String = "https://apps.apple.com/account/subscriptions"

    /// Otomatik yenileme açıklaması: dönem, fiyat, deneme ve iptal koşulu.
    /// - Parameters:
    ///   - fiyat: mağazanın yerelleştirdiği aylık fiyat ("₺99,00"); henüz gelmediyse nil.
    ///   - denemeGunu: ücretsiz deneme gün sayısı; nil = deneme yok ya da kullanıcı uygun değil.
    static func yenilemeAciklamasi(fiyat: String?, denemeGunu: Int?) -> String {
        var parcalar: [String] = []
        if let gun = denemeGunu {
            let sure: String = gun % 7 == 0
                ? Metin.al("ios_plan_deneme_haftalik", gun / 7)
                : Metin.al("ios_plan_deneme_gunluk", gun)
            let tutar: String = fiyat ?? Metin.al("ios_plan_abonelik_ucreti")
            parcalar.append(Metin.al("ios_plan_deneme_sonrasi", sure, tutar))
        } else if let tutar = fiyat {
            parcalar.append(Metin.al("ios_plan_abonelik_ucreti_ayda", tutar))
        }
        parcalar.append(Metin.al("ios_plan_otomatik_yenileme"))
        parcalar.append(Metin.al("ios_plan_iptal"))
        return parcalar.joined(separator: " ")
    }
}
