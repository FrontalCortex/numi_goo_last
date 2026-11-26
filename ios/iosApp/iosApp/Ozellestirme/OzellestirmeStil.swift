import SwiftUI
import UIKit

// Özelleştirme klasörünün ortak görsel yardımcıları: layout XML'lerinde doğrudan yazılmış
// renkler, kaydırıcı, kilit örtüsü ve bilgi balonu.
enum OzellestirmeStil {
    // MARK: Renk

    /// 0xRRGGBB biçimindeki sabit renk.
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi: Double = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil: Double = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi: Double = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// Ortak kodun verdiği ARGB tam sayıdan renk (Kotlin Int → Swift Int32).
    static func renk(argb: Int32) -> Color {
        let deger: UInt32 = UInt32(bitPattern: argb)
        let alfa: Double = Double((deger >> 24) & 0xFF) / 255.0
        return renk(deger & 0x00FF_FFFF, alfa: alfa)
    }

    /// Ortak kodun verdiği ARGB tam sayıdan UIKit rengi.
    static func uiRenk(argb: Int32) -> UIColor {
        let deger: UInt32 = UInt32(bitPattern: argb)
        let alfa: CGFloat = CGFloat((deger >> 24) & 0xFF) / 255.0
        let kirmizi: CGFloat = CGFloat((deger >> 16) & 0xFF) / 255.0
        let yesil: CGFloat = CGFloat((deger >> 8) & 0xFF) / 255.0
        let mavi: CGFloat = CGFloat(deger & 0xFF) / 255.0
        return UIColor(red: kirmizi, green: yesil, blue: mavi, alpha: alfa)
    }

    /// Sekme şeridi zemini: #1A1F22.
    static let sekmeSeridiZemini: Color = OzellestirmeStil.renk(0x1A1F22)
    /// Sekme göstergesi: #FFD600.
    static let sekmeGostergesi: Color = OzellestirmeStil.renk(0xFFD600)
    /// Soluk beyaz metin: #AAFFFFFF.
    static let solukYazi: Color = Color.white.opacity(Double(0xAA) / 255.0)
    /// Daha soluk beyaz metin: #88FFFFFF.
    static let dahaSolukYazi: Color = Color.white.opacity(Double(0x88) / 255.0)
    /// Üst çubuk: altın #FFEB3B, anahtar #AEEA00.
    static let altinYaziRengi: Color = OzellestirmeStil.renk(0xFFEB3B)
    static let anahtarYaziRengi: Color = OzellestirmeStil.renk(0xAEEA00)

    // MARK: Ölçüler

    /// Piksel değiştirmeyle üretilen boncuk resminin genişliği (px). Android'de 120 dp × yoğunluk;
    /// burada resim en çok 60 pt genişlikte gösterildiği için 240 px yeterlidir.
    static let boncukPikselGenisligi: Int = 240
    static let kartKosesi: CGFloat = 12
    static let kartYuksekligi: CGFloat = 110
    static let kaydiriciBasi: CGFloat = 20
    static let kaydiriciYuksekligi: CGFloat = 28
    static let kaydiriciZeminYuksekligi: CGFloat = 8
}

/// Görseli kutuya sığdırarak çizer (Android ImageView, fitCenter).
struct OzellestirmeGorsel: View {
    let ad: String
    let genislik: CGFloat
    let yukseklik: CGFloat

    var body: some View {
        Image(ad)
            .renderingMode(.original)
            .resizable()
            .scaledToFit()
            .frame(width: genislik, height: yukseklik)
    }
}

/// Üretilmiş boncuk resmini kutuya sığdırarak çizer; resim yoksa boş kalır.
struct OzellestirmeBoncukGorunumu: View {
    let resim: UIImage?
    let genislik: CGFloat
    let yukseklik: CGFloat

    var body: some View {
        ZStack {
            if let hazir = resim {
                Image(uiImage: hazir)
                    .resizable()
                    .scaledToFit()
            }
        }
        .frame(width: genislik, height: yukseklik)
    }
}

/// Fiyat + para birimi simgesi (kilit örtülerinde ve satın alma düğmelerinde).
struct OzellestirmeFiyat: View {
    let metin: String
    let simgeAdi: String
    let yaziBoyu: CGFloat
    let simgeBoyu: CGFloat
    let aralik: CGFloat
    let renk: Color

    var body: some View {
        HStack(spacing: aralik) {
            Text(metin)
                .font(Tema.yazi(yaziBoyu))
                .foregroundColor(renk)
                .lineLimit(1)
            OzellestirmeGorsel(ad: simgeAdi, genislik: simgeBoyu, yukseklik: simgeBoyu)
        }
    }
}

/// Renk sekmelerinin üstünü kaplayan kilit örtüsü: yarı saydam siyah zemin, ortada fiyat.
/// Android: tab2ColorLockOverlay / tab4ColorLockOverlay.
struct OzellestirmeRenkKilidi: View {
    let fiyatMetni: String
    let zemin: Color
    let dokun: () -> Void

    var body: some View {
        ZStack {
            zemin
            OzellestirmeFiyat(
                metin: fiyatMetni,
                simgeAdi: "gold_ic",
                yaziBoyu: 20,
                simgeBoyu: 32,
                aralik: 8,
                renk: .white
            )
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(Rectangle())
        .onTapGesture(perform: dokun)
    }
}

/// Zemini gradyan olan, başı seçili rengi gösteren yatay kaydırıcı (Android: SeekBar +
/// rainbowDrawable / lightnessGradientDrawable + roundThumb).
struct OzellestirmeKaydirici: View {
    let deger: Int
    let azami: Int
    let zemin: [Color]
    let basRengi: Color
    let degisti: (Int) -> Void

    var body: some View {
        GeometryReader { geo in
            icerik(genislik: geo.size.width)
        }
        .frame(height: OzellestirmeStil.kaydiriciYuksekligi)
    }

    private func icerik(genislik: CGFloat) -> some View {
        let bas: CGFloat = OzellestirmeStil.kaydiriciBasi
        let yol: CGFloat = max(1, genislik - bas)
        let oran: CGFloat = azami > 0 ? CGFloat(min(max(deger, 0), azami)) / CGFloat(azami) : 0
        let basKonumu: CGFloat = oran * yol
        return ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 3, style: .continuous)
                .fill(LinearGradient(colors: zemin, startPoint: .leading, endPoint: .trailing))
                .frame(height: OzellestirmeStil.kaydiriciZeminYuksekligi)
                .padding(.horizontal, bas / 2)
            Circle()
                .fill(basRengi)
                .overlay(Circle().stroke(Color.white, lineWidth: 2))
                .frame(width: bas, height: bas)
                .offset(x: basKonumu)
        }
        .frame(width: genislik, height: OzellestirmeStil.kaydiriciYuksekligi, alignment: .leading)
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { hareket in
                    let yeni: Int = degerHesapla(x: hareket.location.x, yol: yol, bas: bas)
                    if yeni != deger {
                        degisti(yeni)
                    }
                }
        )
    }

    private func degerHesapla(x: CGFloat, yol: CGFloat, bas: CGFloat) -> Int {
        let konum: CGFloat = min(max(0, x - bas / 2), yol)
        let oran: CGFloat = konum / yol
        return Int((oran * CGFloat(azami)).rounded())
    }
}

/// Kısa süreli bilgi balonu (Android: Toast).
struct OzellestirmeBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14, kalin: false))
            .foregroundColor(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(OzellestirmeStil.renk(0x323232)))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
    }
}

/// Basılınca hafifçe soluklaşan düğme stili.
struct OzellestirmeBasmaStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.7 : 1.0)
    }
}
