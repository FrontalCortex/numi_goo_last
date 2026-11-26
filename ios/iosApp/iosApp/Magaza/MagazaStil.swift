import SwiftUI

// Mağaza klasörünün ortak görsel yardımcıları: fragment_shop.xml'de doğrudan yazılmış
// (kaynak adı olmayan) renkler, ölçüler ve küçük görünümler.
enum MagazaStil {
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

    /// Kart zemini: cardBackgroundColor="#1E2A30".
    static let kartZemini: Color = MagazaStil.renk(0x1E2A30)
    /// Açıklama metni: #B0BEC5.
    static let aciklamaRengi: Color = MagazaStil.renk(0xB0BEC5)
    /// Eylem ve fiyat metni: #29B6F6.
    static let vurguMavi: Color = MagazaStil.renk(0x29B6F6)
    /// Pro fiyatı: #E91E63.
    static let proFiyatRengi: Color = MagazaStil.renk(0xE91E63)
    /// Üst çubuk: can #FF5252, altın #FFEB3B, anahtar #AEEA00.
    static let canYaziRengi: Color = MagazaStil.renk(0xFF5252)
    static let altinYaziRengi: Color = MagazaStil.renk(0xFFEB3B)
    static let anahtarYaziRengi: Color = MagazaStil.renk(0xAEEA00)
    /// Süper kart alt yazısı: #E0E0E0.
    static let superAltYaziRengi: Color = MagazaStil.renk(0xE0E0E0)

    // MARK: Ölçüler (fragment_shop.xml; dp → pt)

    static let ustCubukYuksekligi: CGFloat = 56
    static let kartKosesi: CGFloat = 14
    static let kartIcBosluk: CGFloat = 16
    static let icerikYatayBosluk: CGFloat = 16
    static let icerikUstBosluk: CGFloat = 20
    static let icerikAltBosluk: CGFloat = 24
    static let buyukSimge: CGFloat = 64
    static let paketSimge: CGFloat = 72

    /// bg_shop_super_card.xml: 315° doğrusal gradyan (sol üst → sağ alt), köşe 16.
    static let superKartGradyani = LinearGradient(
        colors: [MagazaStil.renk(0x051230), MagazaStil.renk(0x8028B4)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    /// bg_shop_super_badge.xml: 0° gradyan (soldan sağa), köşe 6.
    static let superRozetGradyani = LinearGradient(
        colors: [MagazaStil.renk(0x1DF0A4), MagazaStil.renk(0x3A82FF), MagazaStil.renk(0xD558FF)],
        startPoint: .leading,
        endPoint: .trailing
    )
}

/// Görseli kutuya sığdırarak çizer (Android ImageView, fitCenter).
struct MagazaGorsel: View {
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

/// Bölüm başlığı (beyaz, kalın).
struct MagazaBolumBasligi: View {
    let metin: String
    let boyut: CGFloat
    let altBosluk: CGFloat

    var body: some View {
        Text(metin)
            .font(Tema.yazi(boyut))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, altBosluk)
    }
}

/// Kısa süreli bilgi balonu (Android: Toast).
struct MagazaBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14, kalin: false))
            .foregroundColor(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(MagazaStil.renk(0x323232)))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
    }
}

/// Basılınca hafifçe soluklaşan düğme stili (Android: selectableItemBackground yerine).
struct MagazaBasmaStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.7 : 1.0)
    }
}

// MARK: Fırlayan simgeler

/// Bir düğmeden ya da karttan fırlayan simgelerin tanımı.
/// Android: ShopFragment.playHeartFlyAnimation (1 kalp) ve playItemFlyAnimation (30 simge).
struct MagazaUcusIstegi: Identifiable, Equatable {
    let id = UUID()
    let simgeAdi: String
    let adet: Int
    /// Simgelerin doğuşunun yayıldığı süre (sn).
    let yayilma: Double

    static func == (sol: MagazaUcusIstegi, sag: MagazaUcusIstegi) -> Bool {
        sol.id == sag.id
    }
}

/// İstekteki bütün simgeleri bulunduğu görünümün ortasından fırlatır.
struct MagazaUcusKatmani: View {
    let istek: MagazaUcusIstegi

    var body: some View {
        ZStack {
            ForEach(0..<istek.adet, id: \.self) { sira in
                MagazaUcanSimge(
                    simgeAdi: istek.simgeAdi,
                    gecikme: istek.adet > 1 ? istek.yayilma * Double(sira) / Double(istek.adet) : 0
                )
            }
        }
        .allowsHitTesting(false)
    }
}

/// Tek simge: önce büyüyerek belirir, sonra yana ve yukarı fırlar, dönerek düşer ve solar.
/// Süreler ve mesafe aralıkları Android'deki animasyonla aynıdır (px değerleri pt alınmıştır;
/// Android'de bunlar yoğunlukla ölçeklenmeyen ham pikseldir, bu yüzden üçe bölünür).
private struct MagazaUcanSimge: View {
    let simgeAdi: String
    let gecikme: Double

    @State private var olcek: CGFloat = 0.2
    @State private var opaklik: Double = 0
    @State private var yatay: CGFloat = 0
    @State private var dikey: CGFloat = 0
    @State private var donus: Double = 0

    // Rastgele değerler görünüm oluşurken bir kez seçilir.
    private let yon: CGFloat = Bool.random() ? 1 : -1
    private let yatayMesafe: CGFloat = CGFloat.random(in: 100...350) / 3
    private let tepe: CGFloat = CGFloat.random(in: 200...500) / 3
    private let dusus: CGFloat = 800 / 3
    private let donusAcisi: Double = Double.random(in: 180...540)

    var body: some View {
        Image(simgeAdi)
            .renderingMode(.original)
            .resizable()
            .scaledToFit()
            .frame(width: 30, height: 30)
            .scaleEffect(olcek)
            .rotationEffect(.degrees(donus))
            .offset(x: yatay, y: dikey)
            .opacity(opaklik)
            .onAppear(perform: baslat)
    }

    private func baslat() {
        let belirme: Double = 0.15
        let cikis: Double = 0.35
        let inis: Double = 0.5
        let toplam: Double = cikis + inis

        // 1. aşama: büyüyerek belir.
        withAnimation(.spring(response: belirme, dampingFraction: 0.5).delay(gecikme)) {
            olcek = 1.0
            opaklik = 1.0
        }
        // 2. aşama: yana doğrusal, yukarı yavaşlayarak.
        let firlatma: Double = gecikme + belirme
        withAnimation(.linear(duration: toplam).delay(firlatma)) {
            yatay = yon * yatayMesafe
            donus = Double(yon) * donusAcisi
        }
        withAnimation(.easeOut(duration: cikis).delay(firlatma)) {
            dikey = -tepe
        }
        // 3. aşama: hızlanarak düş ve sol.
        withAnimation(.easeIn(duration: inis).delay(firlatma + cikis)) {
            dikey = -tepe + dusus
        }
        withAnimation(.linear(duration: 0.3).delay(firlatma + toplam - 0.3)) {
            opaklik = 0
        }
    }
}
