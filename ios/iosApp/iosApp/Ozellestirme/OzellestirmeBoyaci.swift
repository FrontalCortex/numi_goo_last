import Shared
import UIKit

// Boncuk ve çerçeve resimlerini üretir. Android karşılığı: abacus/AbacusBeadRenderer.kt ve
// abacus/AbacusFrameRenderer.kt içindeki build*Drawable işlevlerinin ÇİZİM kısmı.
//
// Hangi kaynağın, hangi renklerle, hangi yöntemle çizileceğine ve piksellerin nasıl
// değiştirileceğine ortak kod karar verir (AbacusCustomizationUiLogic.beadPaint / framePaint /
// recolorBeadPixels / recolorFramePixels). Buradaki kod yalnızca tuvale çizer ve pikselleri
// ortak koda taşıyıp geri alır; renk kuralı içermez.
//
// Üretilen resimler UIImage'dır: önizlemede `Image(uiImage:)` ile gösterilir, ileride
// `AbakusStili.yuvaResimleri` / `cerceveResmi` alanlarına da aynen verilebilir.
enum OzellestirmeBoyaci {
    // MARK: Önbellek

    private static var onbellek: [String: UIImage] = [:]
    private static var anahtarSirasi: [String] = []
    /// Android'deki sınır 48'dir (MAX_BITMAP_CACHE_ENTRIES); burada seçili ve seçili olmayan
    /// resimler ile çerçeveler aynı önbellekte durduğu için iki katı tutulur.
    private static let onbellekSiniri: Int = 96

    /// Anahtar önbellekte varsa onu, yoksa `uret`'in sonucunu döndürür (ve saklar).
    /// Yalnızca ana iş parçacığından çağrılır.
    static func onbellekten(_ anahtar: String, uret: () -> UIImage?) -> UIImage? {
        if let hazir = onbellek[anahtar] {
            return hazir
        }
        guard let yeni = uret() else { return nil }
        onbellek[anahtar] = yeni
        anahtarSirasi.append(anahtar)
        if anahtarSirasi.count > onbellekSiniri {
            let eski: String = anahtarSirasi.removeFirst()
            onbellek[eski] = nil
        }
        return yeni
    }

    // MARK: Katmanlı boncuk (SOROBAN)

    /// Üç katmanı üst üste, her birini kendi rengiyle boyayarak çizer
    /// (Android: buildSorobanDrawable — LayerDrawable, katman başına SRC_IN boyama).
    static func katmanliResim(gorselAdlari: [String], renkler: [UIColor]) -> UIImage? {
        var katmanlar: [UIImage] = []
        for ad in gorselAdlari {
            guard let katman = UIImage(named: ad) else { return nil }
            katmanlar.append(katman)
        }
        guard let ilk = katmanlar.first, ilk.size.width > 0, ilk.size.height > 0 else { return nil }
        let boyut: CGSize = ilk.size
        let bicim = UIGraphicsImageRendererFormat()
        bicim.scale = 3
        bicim.opaque = false
        let cizici = UIGraphicsImageRenderer(size: boyut, format: bicim)
        return cizici.image { _ in
            let alan = CGRect(origin: .zero, size: boyut)
            for (sira, katman) in katmanlar.enumerated() {
                // Android bu yolları dikeyde esnetir (120×72 görüş alanı → 120×92 dp).
                let cizimAlani: CGRect = gerilmisAlan(alan, gorselAdi: gorselAdlari[sira])
                if sira < renkler.count {
                    katman.withTintColor(renkler[sira], renderingMode: .alwaysOriginal).draw(in: cizimAlani)
                } else {
                    katman.draw(in: cizimAlani)
                }
            }
        }
    }

    // MARK: Dikeyde esneyen vektörler

    /// `OzellestirmeStil.dikeyGerme` katsayısı 1'den büyük olan görseller için, görselin içindeki
    /// boş şeritler dışarıda kalacak biçimde dikeyde büyütülmüş (ortası sabit) çizim alanı.
    static func gerilmisAlan(_ alan: CGRect, gorselAdi: String) -> CGRect {
        let oran: CGFloat = OzellestirmeStil.dikeyGerme(gorselAdi)
        if oran <= 1.0 { return alan }
        let yeniYukseklik: CGFloat = alan.height * oran
        let tasma: CGFloat = (yeniYukseklik - alan.height) / 2
        return CGRect(x: alan.minX, y: alan.minY - tasma, width: alan.width, height: yeniYukseklik)
    }

    /// Kaynak görselin değiştirilmemiş hâli (renkler varsayılanken). Dikeyde esneyen vektörler
    /// Android'deki gibi esnetilerek yeniden çizilir; diğerleri olduğu gibi döner.
    static func hamResim(_ gorselAdi: String) -> UIImage? {
        guard let kaynak = UIImage(named: gorselAdi) else { return nil }
        if OzellestirmeStil.dikeyGerme(gorselAdi) <= 1.0 { return kaynak }
        let boyut: CGSize = kaynak.size
        if boyut.width <= 0 || boyut.height <= 0 { return kaynak }
        let bicim = UIGraphicsImageRendererFormat()
        bicim.scale = 3
        bicim.opaque = false
        let cizici = UIGraphicsImageRenderer(size: boyut, format: bicim)
        let alan = CGRect(origin: .zero, size: boyut)
        let cizimAlani: CGRect = gerilmisAlan(alan, gorselAdi: gorselAdi)
        return cizici.image { _ in
            kaynak.draw(in: cizimAlani)
        }
    }

    // MARK: Gradyan hap (SOROBAN2)

    /// Dış siyah hap + içte (0, yükseklik) → (genişlik, 0) doğrultusunda gradyan hap
    /// (Android: buildSoroban2Drawable). Ölçüler ortak koddan gelir.
    static func hapResmi(
        boyut: CGSize,
        disYaricap: CGSize,
        bosluk: CGSize,
        icYaricap: CGSize,
        renkler: [UIColor],
        duraklar: [CGFloat]
    ) -> UIImage? {
        guard boyut.width > 0, boyut.height > 0, renkler.count >= 2, renkler.count == duraklar.count else {
            return nil
        }
        let bicim = UIGraphicsImageRendererFormat()
        bicim.scale = 3
        bicim.opaque = false
        let cizici = UIGraphicsImageRenderer(size: boyut, format: bicim)
        return cizici.image { baglamSarmali in
            let baglam: CGContext = baglamSarmali.cgContext
            let disAlan = CGRect(origin: .zero, size: boyut)
            baglam.addPath(yuvarlakYol(disAlan, yaricap: disYaricap))
            baglam.setFillColor(UIColor.black.cgColor)
            baglam.fillPath()

            let icAlan: CGRect = disAlan.insetBy(dx: bosluk.width, dy: bosluk.height)
            if icAlan.width <= 0 || icAlan.height <= 0 { return }
            let cgRenkler: [CGColor] = renkler.map { $0.cgColor }
            let uzay: CGColorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()
            guard let gradyan = CGGradient(colorsSpace: uzay, colors: cgRenkler as CFArray, locations: duraklar) else {
                return
            }
            baglam.saveGState()
            baglam.addPath(yuvarlakYol(icAlan, yaricap: icYaricap))
            baglam.clip()
            baglam.drawLinearGradient(
                gradyan,
                start: CGPoint(x: 0, y: boyut.height),
                end: CGPoint(x: boyut.width, y: 0),
                options: [.drawsBeforeStartLocation, .drawsAfterEndLocation]
            )
            baglam.restoreGState()
        }
    }

    /// Yatay ve dikey yarıçapı ayrı verilebilen yuvarlatılmış dikdörtgen. Yarıçaplar kenarın
    /// yarısını aşamaz (CGPath aşan değerde çöker), bu yüzden kırpılır.
    static func yuvarlakYol(_ alan: CGRect, yaricap: CGSize) -> CGPath {
        let yatay: CGFloat = max(0, min(yaricap.width, alan.width / 2 - 0.001))
        let dikey: CGFloat = max(0, min(yaricap.height, alan.height / 2 - 0.001))
        return CGPath(roundedRect: alan, cornerWidth: yatay, cornerHeight: dikey, transform: nil)
    }

    // MARK: Piksel değiştirme (hayvan, top, bulut boncukları; kalpli ve yıldızlı çerçeve)

    /// Görseli `genislik` × `yukseklik` piksele çizer, piksellerini ARGB dizisi olarak `islem`'e
    /// verir (dizi yerinde değiştirilir) ve sonucu resim olarak döndürür.
    /// `yukseklik` verilmezse görselin en-boy oranı korunur.
    /// Android: buildReplacedBitmapDrawable / buildFrameBg2Drawable'ın bitmap kısmı.
    static func pikselleriIslenmisResim(
        gorselAdi: String,
        genislik: Int,
        yukseklik: Int?,
        islem: (KotlinIntArray, Int32, Int32) -> Void
    ) -> UIImage? {
        guard let kaynak = UIImage(named: gorselAdi) else { return nil }
        let en: Int = max(1, genislik)
        var boy: Int = 1
        if let verilen = yukseklik {
            boy = max(1, verilen)
        } else if kaynak.size.width > 0 {
            let oran: CGFloat = kaynak.size.height / kaynak.size.width
            boy = max(1, Int(CGFloat(en) * oran))
        }

        let uzay: CGColorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()
        // Bellekte her piksel küçük-sonlu 32 bit tam sayı olarak 0xAARRGGBB biçimindedir.
        let bicim: UInt32 = CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
        guard let baglam = CGContext(
            data: nil,
            width: en,
            height: boy,
            bitsPerComponent: 8,
            bytesPerRow: en * 4,
            space: uzay,
            bitmapInfo: bicim
        ) else { return nil }

        // UIKit çizimi sol üst köşeyi başlangıç alır; Core Graphics tuvali ters çevrilir.
        baglam.translateBy(x: 0, y: CGFloat(boy))
        baglam.scaleBy(x: 1, y: -1)
        UIGraphicsPushContext(baglam)
        let tamAlan = CGRect(x: 0, y: 0, width: CGFloat(en), height: CGFloat(boy))
        kaynak.draw(in: gerilmisAlan(tamAlan, gorselAdi: gorselAdi))
        UIGraphicsPopContext()

        guard let ham = baglam.data else { return nil }
        let adet: Int = en * boy
        let pikseller = ham.bindMemory(to: UInt32.self, capacity: adet)

        // Android bitmap'i alfa ile çarpılmamış ARGB verir; tuval ise çarpılmış tutar.
        let dizi = KotlinIntArray(size: Int32(adet))
        for i in 0..<adet {
            let duz: UInt32 = carpimiGeriAl(pikseller[i])
            dizi.set(index: Int32(i), value: Int32(bitPattern: duz))
        }
        islem(dizi, Int32(en), Int32(boy))
        for i in 0..<adet {
            let duz = UInt32(bitPattern: dizi.get(index: Int32(i)))
            pikseller[i] = alfaylaCarp(duz)
        }

        guard let sonuc = baglam.makeImage() else { return nil }
        return UIImage(cgImage: sonuc, scale: 1, orientation: .up)
    }

    /// Alfa ile çarpılmış ARGB → çarpılmamış ARGB.
    private static func carpimiGeriAl(_ piksel: UInt32) -> UInt32 {
        let alfa: UInt32 = piksel >> 24
        if alfa == 0 { return 0 }
        if alfa == 255 { return piksel }
        let kirmizi: UInt32 = min(255, (((piksel >> 16) & 0xFF) * 255 + alfa / 2) / alfa)
        let yesil: UInt32 = min(255, (((piksel >> 8) & 0xFF) * 255 + alfa / 2) / alfa)
        let mavi: UInt32 = min(255, ((piksel & 0xFF) * 255 + alfa / 2) / alfa)
        return (alfa << 24) | (kirmizi << 16) | (yesil << 8) | mavi
    }

    /// Çarpılmamış ARGB → alfa ile çarpılmış ARGB.
    private static func alfaylaCarp(_ piksel: UInt32) -> UInt32 {
        let alfa: UInt32 = piksel >> 24
        if alfa == 0 { return 0 }
        if alfa == 255 { return piksel }
        let kirmizi: UInt32 = (((piksel >> 16) & 0xFF) * alfa + 127) / 255
        let yesil: UInt32 = (((piksel >> 8) & 0xFF) * alfa + 127) / 255
        let mavi: UInt32 = ((piksel & 0xFF) * alfa + 127) / 255
        return (alfa << 24) | (kirmizi << 16) | (yesil << 8) | mavi
    }
}
