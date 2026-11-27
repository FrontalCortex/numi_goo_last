import Foundation
import SwiftUI

// Sonuç ekranının küçük parçaları: renk yardımcıları, Lottie görünümü (yalnızca sandık yıldızları),
// Sobi sahnesi, sonuç kutusu, göçen düğme.

enum SonucStil {
    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// Üç sonuç layout'unun kök arka planı: `@color/background_color` (#141F23).
    /// (Eski tasarımda #101820 idi; güncel Android hepsinde background_color kullanır.)
    static let zemin: Color = Color("background_color")
    /// "Ders Bitti!" başlığı: #FFF200
    static let baslikSarisi: Color = SonucStil.renk(0xFFF200)
    /// Sarı kutunun yazıları ve çerçevesi (result_box_yellow.xml): #FFD600
    static let kutuSarisi: Color = SonucStil.renk(0xFFD600)
    /// Yeşil kutunun yazıları: #7ED321
    static let kutuYesilYazi: Color = SonucStil.renk(0x7ED321)
    /// Yeşil kutunun çerçevesi (result_box_green.xml): #21D07A
    static let kutuYesilCerceve: Color = SonucStil.renk(0x21D07A)
    /// Kutuların dolgusu (#222)
    static let kutuDolgu: Color = SonucStil.renk(0x222222)
    /// "Ders Geçilemedi!" başlığı: #E93939
    static let hataBaslik: Color = SonucStil.renk(0xE93939)
    /// Başarısızlık nedeni satırı: #E00000
    static let hataAciklama: Color = SonucStil.renk(0xE00000)
    /// Devam / Çıkış göçen düğmesinin yüzü: bg_depth_result_continue.xml, #8CC63F.
    static let devamDugmesi: Color = SonucStil.renk(0x8CC63F)
    /// Aynı düğmenin dudağı (alttaki koyu şerit): #62A12E.
    static let devamDudagi: Color = SonucStil.renk(0x62A12E)
    /// Başarısız ekranındaki düğme yazısı: #101820 (layout `textColor`).
    static let cikisYazisi: Color = SonucStil.renk(0x101820)
    /// Puan çubuğunun zemini (#333333) ve boş kısmı (#444444)
    static let cubukZemin: Color = SonucStil.renk(0x333333)
    static let cubukBos: Color = SonucStil.renk(0x444444)
    /// Puan çubuğunun dolu kısmı: scoreProgressBarFill `backgroundTint="@color/button_enabled"` (#8CC63F).
    static let cubukDolu: Color = SonucStil.renk(0x8CC63F)
    /// Android'de dolu kısmın rengi kodla (ChestResult.updateScoreProgressBar → GradientDrawable.setColor)
    /// değiştirilir, ama görünümde XML'den gelen `backgroundTint` durduğu için (SRC_IN) çizilen renk
    /// hep tint rengidir. `true`: Android'de GÖRÜNEN sabit renk; `false`: kodun hesapladığı geçişli
    /// renk (SonucModeli.cubukRengi, ortak koddan). Android cihazda doğrulanamadı; tek satırla döner.
    static let cubukSabitRenkli: Bool = true
    /// TextAppearance.MaterialComponents.Button harf aralığı: 0.0892857143 em.
    static let dugmeHarfAraligi: CGFloat = 0.0892857143

    /// "#RRGGBB" metnini 0…1 aralığındaki bileşenlere çevirir; bozuksa beyaz.
    static func bilesenler(_ onaltilik: String) -> (Double, Double, Double) {
        var metin = onaltilik
        if metin.hasPrefix("#") { metin.removeFirst() }
        guard metin.count == 6, let sayi = UInt32(metin, radix: 16) else { return (1.0, 1.0, 1.0) }
        let kirmizi = Double((sayi >> 16) & 0xFF) / 255.0
        let yesil = Double((sayi >> 8) & 0xFF) / 255.0
        let mavi = Double(sayi & 0xFF) / 255.0
        return (kirmizi, yesil, mavi)
    }

    /// İki rengi verilen oranla karıştırır. Android ArgbEvaluator gibi doğrusal ışık uzayında
    /// (üs 2.2) ara değer alır. Hangi iki rengin hangi oranla karışacağı ortak koddan gelir.
    static func karisim(_ ilk: String, _ son: String, oran: Double) -> Color {
        let a = bilesenler(ilk)
        let b = bilesenler(son)
        let t: Double = min(1.0, max(0.0, oran))
        let kirmizi: Double = kanalKarisimi(a.0, b.0, t)
        let yesil: Double = kanalKarisimi(a.1, b.1, t)
        let mavi: Double = kanalKarisimi(a.2, b.2, t)
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: 1.0)
    }

    /// Lottie dosya adından ".json" uzantısını atar (sarmalayıcı uzantısız ad ister).
    static func lottieAdi(_ dosya: String) -> String {
        let uzanti: String = ".json"
        if dosya.hasSuffix(uzanti) {
            return String(dosya.dropLast(uzanti.count))
        }
        return dosya
    }

    /// Android animasyon kodunda dp yerine ham PİKSEL yazılmış değerleri pt'ye çevirir
    /// (yoğunluk 3 = xxhdpi varsayıldı; Sandik/SandikStil.pikseldenNokta ile aynı kabul).
    static func pikseldenNokta(_ piksel: CGFloat) -> CGFloat {
        return piksel / 3.0
    }

    private static func kanalKarisimi(_ ilk: Double, _ son: Double, _ oran: Double) -> Double {
        let dogrusalIlk: Double = pow(ilk, 2.2)
        let dogrusalSon: Double = pow(son, 2.2)
        let ara: Double = dogrusalIlk + oran * (dogrusalSon - dogrusalIlk)
        return pow(ara, 1.0 / 2.2)
    }
}

/// Android `LottieAnimationView` karşılığı: ortak sarmalayıcıyı (Ortak/LottieGorunumu.swift) çağırır.
///
/// Güncel Android'de yalnızca sandık sonucunun üç yıldızı (star.json) Lottie'dir; ders sonu
/// animasyonları Sobi maskotuyla (`SonucMaskotu`) değişti.
///
/// - `ad`: Android `assets/` klasöründeki dosya adı; ".json" uzantılı da verilebilir, uzantı atılır.
/// - `dongu`: Android `lottie_loop`.
/// - `oynat`: `false` iken ilk karede bekler; `true` olunca oynar (döngüsüzse son karede kalır).
struct SonucAnimasyonu: View {
    let ad: String
    var dongu: Bool = true
    var oynat: Bool = true

    var body: some View {
        LottieGorunumu(SonucStil.lottieAdi(ad), dongu: dongu, hiz: 1.0, oynat: oynat, bitti: nil)
            .accessibilityHidden(true)
    }
}

/// Ders sonu Sobi sahnesi (Android: `BunnyMascotView` `mascotView`).
///
/// Android'de ekran sağdan kayarak girerken Sobi bekler; giriş bitince (`ENTER_DELAY_MS`, 450 ms)
/// `play(emote)` ile sahne başlar. Burada `basladi` false iken `animasyonlu: false`, sonra true olur.
/// Boyutu çağıran `.frame` ile verir (derste kalan bütün yer, sandık sonucunda 200×200).
struct SonucMaskotu: View {
    /// `BunnyMascotView.Emote` enum adı ("GUITAR", "DETERMINED" ...).
    let emote: String
    let basladi: Bool

    var body: some View {
        SobiMaskotGorunumu(emote: emote, animasyonlu: basladi)
            .accessibilityHidden(true)
    }
}

/// result_box_yellow.xml / result_box_green.xml: #222 dolgu, 2dp çerçeve, 16dp köşe; 80dp yükseklik.
struct SonucKutusu: View {
    let baslik: String
    let deger: String
    let yaziRengi: Color
    let cerceveRengi: Color
    var baslikBoyu: CGFloat = 14

    var body: some View {
        VStack(spacing: 0) {
            Text(baslik)
                .font(Tema.yazi(baslikBoyu))
                .foregroundColor(yaziRengi)
            Text(deger)
                .font(Tema.yazi(22))
                .foregroundColor(yaziRengi)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 80)
        .background(RoundedRectangle(cornerRadius: 16).fill(SonucStil.kutuDolgu))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(cerceveRengi, lineWidth: 2))
    }
}

/// Alt düğme: göçen (derinlikli) Devam / Çıkış düğmesi.
///
/// Android: `bg_depth_result_continue` arka planı + `DepthPress`; `layout_height` dudak (4dp)
/// DAHİL toplam yüksekliktir, etiket yüzde ortalanır (paddingBottom 4dp). Çizimi ortak
/// `GocenDugmeStili` yapar. Yazı 18sp kalın; harf aralığı MaterialButton'dan gelir.
struct SonucDugmesi: View {
    let metin: String
    let yaziRengi: Color
    let yukseklik: CGFloat
    let eylem: () -> Void

    private let yaziBoyu: CGFloat = 18

    var body: some View {
        Button(action: eylem) {
            etiket
        }
        .buttonStyle(
            GocenDugmeStili(
                zemin: SonucStil.devamDugmesi,
                golge: SonucStil.devamDudagi,
                koseYaricapi: 15,
                derinlik: 4
            )
        )
        .frame(height: yukseklik)
    }

    private var etiket: some View {
        let aralik: CGFloat = yaziBoyu * SonucStil.dugmeHarfAraligi
        return Text(metin)
            .kerning(aralik)
            .font(Tema.yazi(yaziBoyu))
            .foregroundColor(yaziRengi)
            .lineLimit(1)
    }
}

/// Sandık sonucunda görünümlerin ekran içindeki yerini yukarı taşıyan tercih anahtarı
/// (çarpan balonunu süre kutusunun üstüne koymak ve puan yazısına uçurmak için).
struct SonucCerceveAnahtari: PreferenceKey {
    static var defaultValue: [Int: CGRect] = [:]

    static func reduce(value: inout [Int: CGRect], nextValue: () -> [Int: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, yeni in yeni })
    }
}

extension View {
    /// Görünümün "sonucAlani" koordinat uzayındaki çerçevesini `kimlik` ile bildirir.
    func sonucCercevesiniBildir(_ kimlik: Int) -> some View {
        background(
            GeometryReader { geo in
                Color.clear.preference(
                    key: SonucCerceveAnahtari.self,
                    value: [kimlik: geo.frame(in: .named("sonucAlani"))]
                )
            }
        )
    }
}
