import SwiftUI
import UIKit

// Abaküsün görünüm özelleştirmesi ve boncuk çizimi.
//
// Android kaynakları:
//  - res/drawable/soroban_bead_layer1..3.xml, soroban_bead_selected_layer1..3.xml (boncuk yolları)
//  - abacus/AbacusBeadRenderer.buildSorobanDrawable (üç katman, her biri SRC_IN ile boyanır)
//  - abacus/AbacusPreferences.SOROBAN_DEFAULT_COLORS / SOROBAN_SEL_DEFAULT_COLORS
//  - res/drawable/abacus_frame_bg.xml, abacus_horizontal_bar.xml (çerçeve ve bariyer)
//  - abacus/AbacusFrameRenderer.buildFrameBgDrawable (yalnızca ahşap katmanı boyanır)
//
// Varsayılan (SOROBAN) boncuk burada yollardan çizilir; asset kullanılmaz. Diğer boncuk
// tipleri (hayvan, top, gradyan...) piksel düzeyinde renk değiştirme ister; onları mağaza /
// özelleştirme ekranı üretip `boncukResmi` / `yuvaResimleri` ile hazır resim olarak verir.

/// Tek bir boncuk yuvası (Android: AbacusPreferences.getBeadTypeForSlot anahtarı).
struct AbakusBoncukYuvasi: Hashable {
    /// 0 = on binler ... 4 = birler.
    let cubuk: Int
    /// true: üst (5'lik) boncuk.
    let ust: Bool
    /// Alt boncuklarda 0..3 (0 = bariyere en yakın); üst boncukta 0.
    let sira: Int

    /// Ortak koddaki anahtar (AbakusBoncukKoprusu.yuvaKodu): çubuk × 10 + (üst 0, alt 1..4).
    var kod: Int {
        return cubuk * 10 + (ust ? 0 : sira + 1)
    }
}

/// Bir yuvanın seçili / seçili olmayan hazır resimleri.
struct AbakusBoncukResimleri: Equatable {
    var normal: UIImage
    var secili: UIImage

    init(normal: UIImage, secili: UIImage) {
        self.normal = normal
        self.secili = secili
    }
}

/// Abaküsün görünüm parametreleri. Varsayılanlar Android'in özelleştirilmemiş hâlidir.
struct AbakusStili: Equatable {
    /// Seçili olmayan boncuğun üç katman rengi: dış kabuk, iç zemin, sağ yansıma.
    var boncukRenkleri: [UIColor]
    /// Seçili boncuğun üç katman rengi.
    var seciliBoncukRenkleri: [UIColor]
    /// Bütün boncuklar için hazır resim (verilirse katman renkleri kullanılmaz).
    var boncukResmi: AbakusBoncukResimleri?
    /// Yuva bazında hazır resim (Android: boncuk başına tip seçimi). `boncukResmi`'nden önce gelir.
    var yuvaResimleri: [AbakusBoncukYuvasi: AbakusBoncukResimleri]
    /// Çerçevenin ahşap rengi (abacus_frame_bg.xml 2. katman).
    var cerceveRengi: UIColor
    /// Çerçeve için hazır resim (abacus_frame_bg2..4 gibi); verilirse şekil çizilmez, resim esnetilir.
    var cerceveResmi: UIImage?
    /// Çerçevenin iç zemini (abacus_frame_bg.xml 4. katman).
    var zeminRengi: UIColor
    /// Bariyerin orta şeridi (abacus_horizontal_bar.xml).
    var bariyerRengi: UIColor
    /// Dikey çubuk rengi (fragment_abacus.xml: #1a1a1a).
    var cubukRengi: UIColor

    init(
        boncukRenkleri: [UIColor] = AbakusStili.varsayilanBoncukRenkleri,
        seciliBoncukRenkleri: [UIColor] = AbakusStili.varsayilanSeciliBoncukRenkleri,
        boncukResmi: AbakusBoncukResimleri? = nil,
        yuvaResimleri: [AbakusBoncukYuvasi: AbakusBoncukResimleri] = [:],
        cerceveRengi: UIColor = AbakusRenk.ui(0xDDA379),
        cerceveResmi: UIImage? = nil,
        zeminRengi: UIColor = AbakusRenk.ui(0x212226),
        bariyerRengi: UIColor = AbakusRenk.ui(0xDDA379),
        cubukRengi: UIColor = AbakusRenk.ui(0x1A1A1A)
    ) {
        self.boncukRenkleri = boncukRenkleri
        self.seciliBoncukRenkleri = seciliBoncukRenkleri
        self.boncukResmi = boncukResmi
        self.yuvaResimleri = yuvaResimleri
        self.cerceveRengi = cerceveRengi
        self.cerceveResmi = cerceveResmi
        self.zeminRengi = zeminRengi
        self.bariyerRengi = bariyerRengi
        self.cubukRengi = cubukRengi
    }

    /// AbacusPreferences.SOROBAN_DEFAULT_COLORS
    static let varsayilanBoncukRenkleri: [UIColor] = [
        AbakusRenk.ui(0x000000), AbakusRenk.ui(0x737373), AbakusRenk.ui(0x8C8C8C),
    ]

    /// AbacusPreferences.SOROBAN_SEL_DEFAULT_COLORS
    static let varsayilanSeciliBoncukRenkleri: [UIColor] = [
        AbakusRenk.ui(0x000000), AbakusRenk.ui(0x4D0000), AbakusRenk.ui(0x731616),
    ]

    static let varsayilan = AbakusStili()
}

/// Layout XML'lerinde kaynak adı olmadan yazılmış renkler için yardımcı.
enum AbakusRenk {
    /// 0xRRGGBB → UIColor.
    static func ui(_ rgb: UInt32, alfa: CGFloat = 1.0) -> UIColor {
        let kirmizi = CGFloat((rgb >> 16) & 0xFF) / 255.0
        let yesil = CGFloat((rgb >> 8) & 0xFF) / 255.0
        let mavi = CGFloat(rgb & 0xFF) / 255.0
        return UIColor(red: kirmizi, green: yesil, blue: mavi, alpha: alfa)
    }

    /// 0xRRGGBB → SwiftUI Color.
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }
}

/// Varsayılan soroban boncuğunu Android vektör yollarından çizer.
enum AbakusBoncukCizici {
    /// Vektörün bildirdiği boyut 120×92 dp, görüş alanı 120×72: Android yolları dikeyde
    /// 92/72 oranında esnetir. Aynı esnetme burada da uygulanır.
    static let enBoyOrani: CGFloat = 120.0 / 92.0
    private static let gorusGenisligi: CGFloat = 120
    private static let gorusYuksekligi: CGFloat = 72

    /// `genislik` pt genişliğinde boncuk resmi; `renkler` üç katmanın rengidir (eksikse varsayılan).
    static func resim(renkler: [UIColor], secili: Bool, genislik: CGFloat) -> UIImage {
        let yedek: [UIColor] = secili
            ? AbakusStili.varsayilanSeciliBoncukRenkleri
            : AbakusStili.varsayilanBoncukRenkleri
        var katmanRenkleri: [UIColor] = []
        for i in 0..<3 {
            katmanRenkleri.append(i < renkler.count ? renkler[i] : yedek[i])
        }
        let yukseklik: CGFloat = genislik / enBoyOrani
        let boyut = CGSize(width: genislik, height: yukseklik)
        let cizici = UIGraphicsImageRenderer(size: boyut)
        return cizici.image { baglam in
            let cg = baglam.cgContext
            cg.scaleBy(x: genislik / gorusGenisligi, y: yukseklik / gorusYuksekligi)
            katmanRenkleri[0].setFill()
            hap(bosluk: 0).fill()
            katmanRenkleri[1].setFill()
            hap(bosluk: 12).fill()
            katmanRenkleri[2].setFill()
            sagYansima().fill()
        }
    }

    /// Katman 1 ve 2: uçları yarım daire olan hap ("M 36,0 L 84,0 A 36,36 ...").
    private static func hap(bosluk: CGFloat) -> UIBezierPath {
        let yaricap: CGFloat = 36 - bosluk
        let yol = UIBezierPath()
        yol.move(to: CGPoint(x: 36, y: bosluk))
        yol.addLine(to: CGPoint(x: 84, y: bosluk))
        yol.addArc(
            withCenter: CGPoint(x: 84, y: 36), radius: yaricap,
            startAngle: -CGFloat.pi / 2, endAngle: CGFloat.pi / 2, clockwise: true
        )
        yol.addLine(to: CGPoint(x: 36, y: 72 - bosluk))
        yol.addArc(
            withCenter: CGPoint(x: 36, y: 36), radius: yaricap,
            startAngle: CGFloat.pi / 2, endAngle: CGFloat.pi * 1.5, clockwise: true
        )
        yol.close()
        return yol
    }

    /// Katman 3: "M 60,12 L 84,12 A 24,24 0 0,1 108,36 A 24,24 0 0,1 84,60 L 60,60 Q 40,36 60,12 Z".
    private static func sagYansima() -> UIBezierPath {
        let yol = UIBezierPath()
        yol.move(to: CGPoint(x: 60, y: 12))
        yol.addLine(to: CGPoint(x: 84, y: 12))
        yol.addArc(
            withCenter: CGPoint(x: 84, y: 36), radius: 24,
            startAngle: -CGFloat.pi / 2, endAngle: CGFloat.pi / 2, clockwise: true
        )
        yol.addLine(to: CGPoint(x: 60, y: 60))
        yol.addQuadCurve(to: CGPoint(x: 60, y: 12), controlPoint: CGPoint(x: 40, y: 36))
        yol.close()
        return yol
    }
}
