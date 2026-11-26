import Foundation
import SwiftUI

// Sonuç ekranının küçük parçaları: renk yardımcıları, Lottie yer tutucusu, sonuç kutusu, düğme.

enum SonucStil {
    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// fragment_lesson_result.xml / fragment_chest_result.xml kök arka planı: #101820
    static let zemin: Color = SonucStil.renk(0x101820)
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
    /// "Devam et" düğmesi: #8CC63F
    static let devamDugmesi: Color = SonucStil.renk(0x8CC63F)
    /// "Çıkış" düğmesi: #29B6F6
    static let cikisDugmesi: Color = SonucStil.renk(0x29B6F6)
    /// Puan çubuğunun zemini (#333333) ve boş kısmı (#444444)
    static let cubukZemin: Color = SonucStil.renk(0x333333)
    static let cubukBos: Color = SonucStil.renk(0x444444)

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

    private static func kanalKarisimi(_ ilk: Double, _ son: Double, _ oran: Double) -> Double {
        let dogrusalIlk: Double = pow(ilk, 2.2)
        let dogrusalSon: Double = pow(son, 2.2)
        let ara: Double = dogrusalIlk + oran * (dogrusalSon - dogrusalIlk)
        return pow(ara, 1.0 / 2.2)
    }
}

/// Lottie animasyonunun yeri.
///
/// `lottie-ios` paketi henüz ekli değil; bu görünüm şimdilik durağan bir simge çizer. Paket
/// eklendiğinde yalnızca bu yapının `body`'si değişecek: `ad` Android `assets/` klasöründeki
/// dosya adıdır (ör. "animation_one.json", "star.json").
///
/// - `dongu`: Android `lottie_loop`.
/// - `oynat`: `false` iken ilk karede bekler; `true` olunca oynar (döngüsüzse son karede kalır).
struct SonucAnimasyonYerTutucu: View {
    let ad: String
    var dongu: Bool = true
    var oynat: Bool = true

    var body: some View {
        Group {
            if ad == "star.json" {
                yildiz
            } else {
                kutlama
            }
        }
        .accessibilityHidden(true)
    }

    /// Sandık sonucundaki yıldız işareti: yanmadan önce sönük, yanınca dolu yıldız.
    private var yildiz: some View {
        Image(oynat ? "star_on_ic" : "star_off_ic")
            .resizable()
            .scaledToFit()
            .frame(width: 34, height: 34)
            .scaleEffect(oynat ? 1.0 : 0.8)
            .animation(.spring(response: 0.35, dampingFraction: 0.45), value: oynat)
    }

    /// Ekranın üstündeki kutlama animasyonunun yeri.
    private var kutlama: some View {
        Image(systemName: "sparkles")
            .resizable()
            .scaledToFit()
            .foregroundColor(SonucStil.kutuSarisi)
            .padding(50)
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

/// Alt düğme (Android: MaterialButton).
struct SonucDugmesi: View {
    let metin: String
    let zemin: Color
    let yaziRengi: Color
    let yukseklik: CGFloat
    let kose: CGFloat
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(18))
                .foregroundColor(yaziRengi)
                .frame(maxWidth: .infinity)
                .frame(height: yukseklik)
                .background(RoundedRectangle(cornerRadius: kose).fill(zemin))
        }
        .buttonStyle(.plain)
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
