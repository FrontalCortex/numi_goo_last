import SwiftUI

// Kabuğun ölçüleri ve sabit renkleri. Değerler Android res/values/dimens.xml (main_*),
// activity_main.xml ve drawable/bottom_nav_item_bg.xml'den birebir alınmıştır (dp -> pt, sp -> pt).
enum KabukOlculeri {
    // Üst çubuk (currencyPanel)
    static let ustDikeyBosluk: CGFloat = 6        // main_currency_panel_padding_v
    static let ustYatayBosluk: CGFloat = 12       // main_currency_panel_padding_h
    static let elmasYukseklik: CGFloat = 30       // main_diamond_height (genişlik wrap_content)
    static let gostergeSimge: CGFloat = 28        // main_energy_icon_size
    static let gostergeYazi: CGFloat = 17         // main_currency_text_size / main_energy_text_size
    static let gostergeYaziBosluk: CGFloat = 3    // layout_marginStart="3dp"
    static let seriSagBosluk: CGFloat = 6         // streakContainer paddingEnd
    static let sonsuzYazi: CGFloat = 13           // energyInfiniteBadge textSize
    static let geriDugmesi: CGFloat = 30          // main_toolbar_icon_back
    static let geriOku: CGFloat = 24              // ic_back_arrow.xml (vektörün kendi boyu)

    // Alt çubuk (bottomNavigationID)
    static let altYukseklik: CGFloat = 64         // main_bottom_nav_min_height
    static let altSimge: CGFloat = 30             // main_bottom_nav_icon_size
    static let altSimgeUstBosluk: CGFloat = 11    // main_bottom_nav_item_padding_top

    // Seçili sekme kutusu (bottom_nav_item_bg.xml, state_checked)
    static let seciliKutuYatayPay: CGFloat = 7    // insetLeft / insetRight
    static let seciliKutuDikeyPay: CGFloat = 5    // insetTop / insetBottom
    static let seciliKutuKose: CGFloat = 14       // corners radius
    static let seciliKutuCizgi: CGFloat = 2       // stroke width

    // Ayırıcı çizgi (currencyPanelDivider ve alt çubuğun üstündeki çizgi)
    static let ayiriciKalinlik: CGFloat = 1

    /// Ders paneli açıkken üst ve alt çubuğun karartması (LessonAdapter.LESSON_PANEL_DIM_ALPHA).
    static let karartma: Double = 0.7

    // activity_main.xml'de doğrudan yazılmış metin renkleri
    // #FFEB3B
    static let elmasYaziRengi = KabukOlculeri.renk(0xFFEB3B)
    // #F44336
    static let enerjiYaziRengi = KabukOlculeri.renk(0xF44336)
    // #FFD600
    static let anahtarYaziRengi = KabukOlculeri.renk(0xFFD600)
    // #FF9800 (seri sürüyorken) / #78909C (seri kırık: alev söner; MainActivity.refreshStreakUi)
    static let seriYaziRengi = KabukOlculeri.renk(0xFF9800)
    static let seriSonukRengi = KabukOlculeri.renk(0x78909C)

    static func renk(_ rgb: UInt32) -> Color {
        let k: Double = Double((rgb >> 16) & 0xFF) / 255.0
        let y: Double = Double((rgb >> 8) & 0xFF) / 255.0
        let m: Double = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: k, green: y, blue: m, opacity: 1.0)
    }
}

// 1 pt kalınlığında yatay ayırıcı (Android: @color/circleBackground_color).
struct KabukAyirici: View {
    var body: some View {
        Color("circleBackground_color")
            .frame(maxWidth: .infinity)
            .frame(height: KabukOlculeri.ayiriciKalinlik)
    }
}
