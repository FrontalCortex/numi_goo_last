import Foundation
import SwiftUI

// Sandık ve kristal ekranlarının ortak küçük parçaları: renk ve ölçü yardımcıları, animasyon
// eğrileri, video yer tutucusu (gerçek oynatıcı: SandikVideoOynatici.swift).

enum SandikStil {
    /// 0xRRGGBB biçimindeki sabit renk.
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// Ortak kodun verdiği ARGB tam sayısı (Android renk Int'i; işaretli 32 bit).
    static func renk(argb: Int32) -> Color {
        let ham = UInt32(bitPattern: argb)
        let alfa = Double((ham >> 24) & 0xFF) / 255.0
        return renk(ham & 0x00FF_FFFF, alfa: alfa)
    }

    /// step_circle_done.xml: #555555
    static let adimTamamRengi: Color = SandikStil.renk(0x555555)

    /// Android animasyon kodundaki ham piksel değerlerini pt'ye çevirir.
    ///
    /// NewChestFragment ve CrystalBreakVideoFragment'te kayma/zıplama mesafeleri dp değil
    /// PİKSEL olarak yazılmıştır (ör. `translationY` −35f); görünen büyüklük telefonun ekran
    /// yoğunluğuna bağlıdır. Burada yoğunluk 3 (xxhdpi, yaygın telefonlar) varsayıldı.
    static func pikseldenNokta(_ piksel: CGFloat) -> CGFloat {
        return piksel / 3.0
    }

    /// AccelerateDecelerateInterpolator (Android ObjectAnimator'ın varsayılan eğrisi).
    static func hizlanYavasla(_ oran: Double) -> Double {
        return cos((oran + 1.0) * Double.pi) / 2.0 + 0.5
    }

    /// Anahtar kareler arasında doğrusal ara değer (Android: Keyframe / PropertyValuesHolder).
    /// `kareler`: (oran, değer) çiftleri, orana göre artan sırada.
    static func araKare(_ kareler: [(Double, Double)], _ oran: Double) -> Double {
        guard let ilk = kareler.first, let son = kareler.last else { return 0 }
        if oran <= ilk.0 { return ilk.1 }
        if oran >= son.0 { return son.1 }
        for sira in 1..<kareler.count {
            let onceki = kareler[sira - 1]
            let sonraki = kareler[sira]
            if oran <= sonraki.0 {
                let aralik: Double = sonraki.0 - onceki.0
                if aralik <= 0 { return sonraki.1 }
                let yerel: Double = (oran - onceki.0) / aralik
                return onceki.1 + yerel * (sonraki.1 - onceki.1)
            }
        }
        return son.1
    }

    /// NewChestFragment.startIdleAnimation: sandığın boşta zıplaması.
    /// - Returns: (dikey kayma pt, dönme derecesi)
    static func bosHareket(nadirlikSirasi: Int, gecen: Double) -> (CGFloat, Double) {
        // RARE (1) 1750 ms, diğerleri 2000 ms döngü.
        let dongu: Double = nadirlikSirasi == 1 ? 1.75 : 2.0
        // EPIC ve LEGENDARY (2, 3) iki kat yüksek zıplar ve sallanır.
        let guclu: Bool = nadirlikSirasi >= 2
        let tepe: Double = guclu ? -70 : -35
        let ikinci: Double = guclu ? -20 : -10
        let ham: Double = gecen.truncatingRemainder(dividingBy: dongu) / dongu
        let oran: Double = hizlanYavasla(ham)
        let dikeyKareler: [(Double, Double)] = [
            (0, 0), (0.25, 0), (0.30, tepe), (0.35, 0), (0.38, ikinci), (0.41, 0), (1, 0)
        ]
        let dikeyPiksel: Double = araKare(dikeyKareler, oran)
        var donme: Double = 0
        if guclu {
            let donmeKareleri: [(Double, Double)] = [
                (0, 0), (0.25, 0), (0.28, 5), (0.32, -5), (0.35, 0), (1, 0)
            ]
            donme = araKare(donmeKareleri, oran)
        }
        return (pikseldenNokta(CGFloat(dikeyPiksel)), donme)
    }

    /// updateStepIndicators: etkin adım dairesi 400 ms'de 15 piksel yukarı çıkıp iner (sonsuz).
    static func adimZiplamasi(simdi: TimeInterval) -> CGFloat {
        let yarim: Double = 0.4
        let konum: Double = simdi.truncatingRemainder(dividingBy: yarim * 2)
        let ham: Double = konum < yarim ? konum / yarim : 2.0 - konum / yarim
        let oran: Double = hizlanYavasla(ham)
        return pikseldenNokta(CGFloat(-15.0 * oran))
    }
}

/// Video dosyası pakette yokken çizilen durağan görsel.
///
/// Kristal videoları depoda yok; dosya `iosApp/Videolar/<ad>.mp4` olarak eklendiğinde ekran
/// kendiliğinden gerçek videoya (SandikVideoGorunumu) geçer. `ad` Android `res/raw/` içindeki
/// video dosyasının uzantısız adıdır (ör. "crystal_blue_blue").
struct SandikVideoYerTutucu: View {
    let ad: String

    var body: some View {
        ZStack {
            Color.black
            Image("crystal_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 180, height: 180)
        }
        .accessibilityHidden(true)
    }
}
