import SwiftUI

// Kör ders ekranının ölçüleri ve renkleri.
// Kaynak: res/layout/fragment_blinding_lesson.xml, res/values/dimens.xml (güncel, versionCode 26).
// Sayı kartı, yan düğme, göçen düğme ve sonuç panelleri abaküs dersiyle ortaktır: bkz.
// Abakus/AbakusDersOrtak.swift (AbakusDersOlcu, AbakusDersRenk).

enum KorDersStil {
    // MARK: Ölçüler (dp → pt birebir)

    /// Kök ConstraintLayout: padding 10dp.
    static let kokBosluk: CGFloat = 10
    /// quitButton: 48dp.
    static let cikisBoyu: CGFloat = 48
    /// progressBarContainer: 250×16dp, margin 16dp.
    static let ilerlemeGenislik: CGFloat = 250
    static let ilerlemeYukseklik: CGFloat = 16
    static let ilerlemeUstBosluk: CGFloat = 16
    /// Kartın üstündeki sayılar: marginBottom 4dp (dikey ortadan 2dp yukarı), padding 16dp, 40sp.
    static let tahtaYaziAltBosluk: CGFloat = 4
    static let tahtaYaziBosluk: CGFloat = 16
    static let tahtaYaziBoyu: CGFloat = 40
    /// timerTextView: 18sp.
    static let sureYaziBoyu: CGFloat = 18
    /// numberInput: 180×98dp, 32sp; rounded_edittext: köşe 12dp, çerçeve 10dp.
    static let kutuGenislik: CGFloat = 180
    static let kutuYukseklik: CGFloat = 98
    static let kutuYaziBoyu: CGFloat = 32
    static let kutuKose: CGFloat = 12
    static let kutuCerceve: CGFloat = 10
    /// Kısa ekranda (ya da klavye açıkken) sayı kutusunun üstünde ve altında bırakılan en az boşluk.
    static let kutuEnAzBosluk: CGFloat = 8
    /// abacusContainer: layout_marginBottom (tutorial_abacus_linear_margin_bottom 10dp).
    static let abakusAltBosluk: CGFloat = 10
    /// kontrolButton / startButton: yükseklik 48dp (tutorial_control_button_height; dudak dahil), yan
    /// boşluk 24dp, alt boşluk 15dp, köşe 10dp (tutorial_control_button_corner_radius), 18sp.
    static let anaDugmeYanBosluk: CGFloat = 24
    static let anaDugmeAltBosluk: CGFloat = 15
    static let dugmeKose: CGFloat = 10
    static let dugmeYaziBoyu: CGFloat = 18
    /// Yan düğmeler: abacus_side_button_size 48dp; aralarında abacus_fragment_ask_button_margin_top 4dp.
    /// fabHint ve resetButton: padding 5dp → 38dp'lik animasyon; skipStepButton ve abacusModeButton:
    /// padding 2dp → 44dp.
    static let yanDugmeLottieBoyu: CGFloat = 38
    static let adimAtlaLottieBoyu: CGFloat = 44
    /// tvHint (hint_bubble_background): köşe 16dp, kuyruk için sağda 9dp; iç boşluk başta 16, sonda 25
    /// (kuyruk dahil), dikeyde 10; en az 48dp yükseklik; satır aralığı 2dp; 16sp.
    static let ipucuYaziBoyu: CGFloat = 16
    static let ipucuKose: CGFloat = 16
    static let ipucuKuyrukPayi: CGFloat = 9
    static let ipucuBasBosluk: CGFloat = 16
    static let ipucuSonBosluk: CGFloat = 25
    static let ipucuDikeyBosluk: CGFloat = 10
    static let ipucuSatirAraligi: CGFloat = 2
    /// Balonun sağ kenarı ile ampul düğmesinin sol kenarı arası: tvHint marginEnd 26 − (düğmenin
    /// kapsayıcıdan 25 taşması − 48) = 3dp.
    static let ipucuDugmeAraligi: CGFloat = 3

    // MARK: Renkler

    /// 0xRRGGBB biçimindeki sabit renk.
    static func renk(_ rgb: UInt32) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: 1.0)
    }

    /// progressBarContainer zemini (#333) ve boş kısmı (#444).
    static let ilerlemeZemin: Color = KorDersStil.renk(0x333333)
    static let ilerlemeBos: Color = KorDersStil.renk(0x444444)
    /// updateProgressBar renkleri: #7CFC00, #FFFF99, #990000.
    static let ilerlemeYesil: Color = KorDersStil.renk(0x7CFC00)
    static let ilerlemeSari: Color = KorDersStil.renk(0xFFFF99)
    static let ilerlemeKoyuKirmizi: Color = KorDersStil.renk(0x990000)
    /// rounded_edittext: dolgu #BAB9B9, çerçeve siyah; yazı #222222.
    static let kutuDolgu: Color = KorDersStil.renk(0xBAB9B9)
    static let kutuYazi: Color = KorDersStil.renk(0x222222)

    /// numberInput ipucu metni (layout XML'inde sabit "?").
    static let kutuIpucu = "?"
}
