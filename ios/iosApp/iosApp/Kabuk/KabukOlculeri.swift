import SwiftUI

// Kabuğun ölçüleri ve sabit renkleri. Değerler Android res/values/dimens.xml (main_*)
// ve activity_main.xml'den birebir alınmıştır (dp -> pt, sp -> pt).
enum KabukOlculeri {
    // Üst çubuk (currencyPanel)
    static let ustDikeyBosluk: CGFloat = 6        // main_currency_panel_padding_v
    static let ustYatayBosluk: CGFloat = 12       // main_currency_panel_padding_h
    static let elmasYukseklik: CGFloat = 30       // main_diamond_height
    static let gostergeSimge: CGFloat = 28        // main_energy_icon_size
    static let gostergeYazi: CGFloat = 17         // main_currency_text_size / main_energy_text_size
    static let gostergeYaziBosluk: CGFloat = 3    // layout_marginStart="3dp"

    // Alt çubuk (bottomNavigationID)
    static let altYukseklik: CGFloat = 64         // main_bottom_nav_min_height
    static let altSimge: CGFloat = 30             // main_bottom_nav_icon_size
    static let altSimgeUstBosluk: CGFloat = 11    // main_bottom_nav_item_padding_top

    // Ayırıcı çizgi (currencyPanelDivider ve alt çubuğun üstündeki çizgi)
    static let ayiriciKalinlik: CGFloat = 1

    // Android'de alt çubukta seçili sekmenin görsel vurgusu yoktur (itemIconTint=@null,
    // başlık yok). Vurgu istenirse bu değer 1'den küçük yapılır.
    static let seciliOlmayanOpaklik: Double = 1.0

    // activity_main.xml'de doğrudan yazılmış metin renkleri
    // #FFEB3B
    static let elmasYaziRengi = Color(red: 1.0, green: 235.0 / 255.0, blue: 59.0 / 255.0)
    // #F44336
    static let enerjiYaziRengi = Color(red: 244.0 / 255.0, green: 67.0 / 255.0, blue: 54.0 / 255.0)
    // #FFD600
    static let anahtarYaziRengi = Color(red: 1.0, green: 214.0 / 255.0, blue: 0.0)
}

// 1 pt kalınlığında yatay ayırıcı (Android: @color/circleBackground_color).
struct KabukAyirici: View {
    var body: some View {
        Color("circleBackground_color")
            .frame(maxWidth: .infinity)
            .frame(height: KabukOlculeri.ayiriciKalinlik)
    }
}
