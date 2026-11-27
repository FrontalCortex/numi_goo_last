import SwiftUI

// SVG yol verisinin ("d" niteliği) SwiftUI `Path`'e çevrilmesi.
//
// Android'de avatar SVG'si AndroidSVG kütüphanesiyle çiziliyor (AvatarView). iOS'ta hazır bir SVG
// çizici yok; `Maskot/SobiMaskotTuval.swift` içindeki yol çözücü yalnızca maskot üreticisinin yalın
// biçimini ("M x y", "L x y", "C …", "Z" boşlukla ayrılmış) okuyor. Personas parçaları ise tam SVG yol
// söz dizimi kullanıyor (göreli komutlar, yaylar, bitişik sayılar: "a5 5 0 0 1-10 0", "1.5.5"), bu yüzden
// burada ayrı bir çözücü var.
//
// Desteklenen komutlar: M m L l H h V v C c S s Q q T t A a Z z (SVG 1.1, 8.3).

enum AvatarSvgYolu {
    /// "d" metnini çözer. Bozuk bir yerde durur, o ana kadarki yolu döndürür (AndroidSVG de böyle yapar).
    static func coz(_ d: String) -> Path {
        var okuyucu = AvatarSvgSayiOkuyucu(d)
        var yol = Path()
        var simdiki = CGPoint.zero
        var altYolBasi = CGPoint.zero
        // S / T için bir önceki eğrinin son denetim noktası.
        var sonKubikDenetim: CGPoint? = nil
        var sonKareselDenetim: CGPoint? = nil
        var komut: UInt8 = 0
        var yolBasladi: Bool = false

        while true {
            okuyucu.ayiraclariAtla()
            if okuyucu.bitti { break }
            if let yeni = okuyucu.komutOku() {
                komut = yeni
            } else if komut == 0 {
                break
            }
            // Bir komut harfinden sonra gelen sayılar komutu tekrarlar; M/m'den sonra L/l olur.
            let goreli: Bool = komut >= 97
            let buyuk: UInt8 = goreli ? komut - 32 : komut
            let taban: CGPoint = goreli ? simdiki : .zero

            switch buyuk {
            case 77: // M
                guard let x = okuyucu.sayi(), let y = okuyucu.sayi() else { return yol }
                let p = CGPoint(x: taban.x + x, y: taban.y + y)
                yol.move(to: p)
                yolBasladi = true
                simdiki = p
                altYolBasi = p
                sonKubikDenetim = nil
                sonKareselDenetim = nil
                komut = goreli ? 108 : 76 // l / L
            case 76: // L
                guard let x = okuyucu.sayi(), let y = okuyucu.sayi() else { return yol }
                let p = CGPoint(x: taban.x + x, y: taban.y + y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                yol.addLine(to: p)
                simdiki = p
                sonKubikDenetim = nil
                sonKareselDenetim = nil
            case 72: // H
                guard let x = okuyucu.sayi() else { return yol }
                let p = CGPoint(x: (goreli ? simdiki.x : 0) + x, y: simdiki.y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                yol.addLine(to: p)
                simdiki = p
                sonKubikDenetim = nil
                sonKareselDenetim = nil
            case 86: // V
                guard let y = okuyucu.sayi() else { return yol }
                let p = CGPoint(x: simdiki.x, y: (goreli ? simdiki.y : 0) + y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                yol.addLine(to: p)
                simdiki = p
                sonKubikDenetim = nil
                sonKareselDenetim = nil
            case 67: // C
                guard let x1 = okuyucu.sayi(), let y1 = okuyucu.sayi(),
                      let x2 = okuyucu.sayi(), let y2 = okuyucu.sayi(),
                      let x = okuyucu.sayi(), let y = okuyucu.sayi() else { return yol }
                let d1 = CGPoint(x: taban.x + x1, y: taban.y + y1)
                let d2 = CGPoint(x: taban.x + x2, y: taban.y + y2)
                let p = CGPoint(x: taban.x + x, y: taban.y + y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                yol.addCurve(to: p, control1: d1, control2: d2)
                simdiki = p
                sonKubikDenetim = d2
                sonKareselDenetim = nil
            case 83: // S
                guard let x2 = okuyucu.sayi(), let y2 = okuyucu.sayi(),
                      let x = okuyucu.sayi(), let y = okuyucu.sayi() else { return yol }
                var d1: CGPoint = simdiki
                if let onceki = sonKubikDenetim {
                    d1 = CGPoint(x: 2 * simdiki.x - onceki.x, y: 2 * simdiki.y - onceki.y)
                }
                let d2 = CGPoint(x: taban.x + x2, y: taban.y + y2)
                let p = CGPoint(x: taban.x + x, y: taban.y + y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                yol.addCurve(to: p, control1: d1, control2: d2)
                simdiki = p
                sonKubikDenetim = d2
                sonKareselDenetim = nil
            case 81: // Q
                guard let x1 = okuyucu.sayi(), let y1 = okuyucu.sayi(),
                      let x = okuyucu.sayi(), let y = okuyucu.sayi() else { return yol }
                let d1 = CGPoint(x: taban.x + x1, y: taban.y + y1)
                let p = CGPoint(x: taban.x + x, y: taban.y + y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                yol.addQuadCurve(to: p, control: d1)
                simdiki = p
                sonKareselDenetim = d1
                sonKubikDenetim = nil
            case 84: // T
                guard let x = okuyucu.sayi(), let y = okuyucu.sayi() else { return yol }
                var d1: CGPoint = simdiki
                if let onceki = sonKareselDenetim {
                    d1 = CGPoint(x: 2 * simdiki.x - onceki.x, y: 2 * simdiki.y - onceki.y)
                }
                let p = CGPoint(x: taban.x + x, y: taban.y + y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                yol.addQuadCurve(to: p, control: d1)
                simdiki = p
                sonKareselDenetim = d1
                sonKubikDenetim = nil
            case 65: // A
                guard let rx = okuyucu.sayi(), let ry = okuyucu.sayi(), let aci = okuyucu.sayi(),
                      let buyukYay = okuyucu.bayrak(), let tarama = okuyucu.bayrak(),
                      let x = okuyucu.sayi(), let y = okuyucu.sayi() else { return yol }
                let p = CGPoint(x: taban.x + x, y: taban.y + y)
                if !yolBasladi { yol.move(to: simdiki); yolBasladi = true }
                AvatarSvgYolu.yayEkle(
                    &yol, baslangic: simdiki, bitis: p, rx: rx, ry: ry,
                    xEkseniAcisi: aci, buyukYay: buyukYay, taramaPozitif: tarama
                )
                simdiki = p
                sonKubikDenetim = nil
                sonKareselDenetim = nil
            case 90: // Z
                yol.closeSubpath()
                simdiki = altYolBasi
                sonKubikDenetim = nil
                sonKareselDenetim = nil
                // Z'den sonra yeni bir komut harfi beklenir. M gelmeden çizim komutu gelirse yeni alt yol
                // kapatılan alt yolun başından başlar (CGPath kapatmadan sonra şimdiki noktayı oraya alır).
                komut = 0
            default:
                return yol
            }
        }
        return yol
    }

    /// SVG yayını (uç nokta biçimi) kübik Bézier parçalarına çevirir (SVG 1.1, F.6.5 ve F.6.6).
    static func yayEkle(
        _ yol: inout Path,
        baslangic: CGPoint,
        bitis: CGPoint,
        rx rxHam: CGFloat,
        ry ryHam: CGFloat,
        xEkseniAcisi: CGFloat,
        buyukYay: Bool,
        taramaPozitif: Bool
    ) {
        if baslangic == bitis { return }
        var rx: CGFloat = abs(rxHam)
        var ry: CGFloat = abs(ryHam)
        if rx == 0 || ry == 0 {
            yol.addLine(to: bitis)
            return
        }
        let phi: CGFloat = xEkseniAcisi * .pi / 180
        let cosPhi: CGFloat = cos(phi)
        let sinPhi: CGFloat = sin(phi)

        // 1. adım: (x1', y1')
        let dx2: CGFloat = (baslangic.x - bitis.x) / 2
        let dy2: CGFloat = (baslangic.y - bitis.y) / 2
        let x1p: CGFloat = cosPhi * dx2 + sinPhi * dy2
        let y1p: CGFloat = -sinPhi * dx2 + cosPhi * dy2

        // Yarıçaplar yetmiyorsa büyütülür.
        let lambda: CGFloat = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if lambda > 1 {
            let kok: CGFloat = sqrt(lambda)
            rx *= kok
            ry *= kok
        }

        // 2. adım: (cx', cy')
        let rx2: CGFloat = rx * rx
        let ry2: CGFloat = ry * ry
        let pay: CGFloat = rx2 * ry2 - rx2 * y1p * y1p - ry2 * x1p * x1p
        let payda: CGFloat = rx2 * y1p * y1p + ry2 * x1p * x1p
        var katsayi: CGFloat = 0
        if payda != 0 {
            katsayi = sqrt(max(0, pay / payda))
        }
        if buyukYay == taramaPozitif {
            katsayi = -katsayi
        }
        let cxp: CGFloat = katsayi * (rx * y1p / ry)
        let cyp: CGFloat = katsayi * -(ry * x1p / rx)

        // 3. adım: merkez
        let ortaX: CGFloat = (baslangic.x + bitis.x) / 2
        let ortaY: CGFloat = (baslangic.y + bitis.y) / 2
        let cx: CGFloat = cosPhi * cxp - sinPhi * cyp + ortaX
        let cy: CGFloat = sinPhi * cxp + cosPhi * cyp + ortaY

        // 4. adım: başlangıç açısı ve tarama
        let ux: CGFloat = (x1p - cxp) / rx
        let uy: CGFloat = (y1p - cyp) / ry
        let vx: CGFloat = (-x1p - cxp) / rx
        let vy: CGFloat = (-y1p - cyp) / ry
        let teta1: CGFloat = AvatarSvgYolu.vektorAcisi(1, 0, ux, uy)
        var deltaTeta: CGFloat = AvatarSvgYolu.vektorAcisi(ux, uy, vx, vy)
        if !taramaPozitif && deltaTeta > 0 {
            deltaTeta -= 2 * .pi
        } else if taramaPozitif && deltaTeta < 0 {
            deltaTeta += 2 * .pi
        }

        // En çok 90 derecelik parçalar.
        let parcaSayisi: Int = max(1, Int(ceil(abs(deltaTeta) / (.pi / 2) - 0.000001)))
        let parcaAcisi: CGFloat = deltaTeta / CGFloat(parcaSayisi)
        let t: CGFloat = 4.0 / 3.0 * tan(parcaAcisi / 4)
        var aci1: CGFloat = teta1
        for sira in 0..<parcaSayisi {
            let aci2: CGFloat = aci1 + parcaAcisi
            let cos1: CGFloat = cos(aci1)
            let sin1: CGFloat = sin(aci1)
            let cos2: CGFloat = cos(aci2)
            let sin2: CGFloat = sin(aci2)
            // Birim çemberde denetim noktaları, sonra elipse ve döndürmeye taşınır.
            let e1 = CGPoint(x: cos1 - t * sin1, y: sin1 + t * cos1)
            let e2 = CGPoint(x: cos2 + t * sin2, y: sin2 - t * cos2)
            let e3 = CGPoint(x: cos2, y: sin2)
            let d1: CGPoint = AvatarSvgYolu.elipsNoktasi(e1, cx, cy, rx, ry, cosPhi, sinPhi)
            let d2: CGPoint = AvatarSvgYolu.elipsNoktasi(e2, cx, cy, rx, ry, cosPhi, sinPhi)
            var uc: CGPoint = AvatarSvgYolu.elipsNoktasi(e3, cx, cy, rx, ry, cosPhi, sinPhi)
            if sira == parcaSayisi - 1 {
                // Yuvarlama hatası birikmesin; son nokta tam bitiş noktası.
                uc = bitis
            }
            yol.addCurve(to: uc, control1: d1, control2: d2)
            aci1 = aci2
        }
    }

    private static func elipsNoktasi(
        _ p: CGPoint, _ cx: CGFloat, _ cy: CGFloat, _ rx: CGFloat, _ ry: CGFloat, _ cosPhi: CGFloat, _ sinPhi: CGFloat
    ) -> CGPoint {
        let x: CGFloat = p.x * rx
        let y: CGFloat = p.y * ry
        return CGPoint(x: cosPhi * x - sinPhi * y + cx, y: sinPhi * x + cosPhi * y + cy)
    }

    /// İki vektör arasındaki işaretli açı (radyan).
    private static func vektorAcisi(_ ux: CGFloat, _ uy: CGFloat, _ vx: CGFloat, _ vy: CGFloat) -> CGFloat {
        let nokta: CGFloat = ux * vx + uy * vy
        let uzunluk: CGFloat = sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy)
        if uzunluk == 0 { return 0 }
        var oran: CGFloat = nokta / uzunluk
        oran = min(1, max(-1, oran))
        var aci: CGFloat = acos(oran)
        if ux * vy - uy * vx < 0 {
            aci = -aci
        }
        return aci
    }
}

/// Yol verisini ve sayı listelerini ("translate(20 24)", "0 0 64 64") okuyan tarayıcı.
struct AvatarSvgSayiOkuyucu {
    private let baytlar: [UInt8]
    private var i: Int = 0

    init(_ metin: String) {
        baytlar = Array(metin.utf8)
    }

    var bitti: Bool { i >= baytlar.count }

    /// Boşluk ve virgülleri atlar.
    mutating func ayiraclariAtla() {
        while i < baytlar.count {
            let c: UInt8 = baytlar[i]
            if c == 32 || c == 9 || c == 10 || c == 13 || c == 12 || c == 44 {
                i += 1
            } else {
                break
            }
        }
    }

    /// Sıradaki karakter bir komut harfiyse okur.
    mutating func komutOku() -> UInt8? {
        ayiraclariAtla()
        guard i < baytlar.count else { return nil }
        let c: UInt8 = baytlar[i]
        let harf: Bool = (c >= 65 && c <= 90) || (c >= 97 && c <= 122)
        // "e"/"E" sayının üssü olabilir; ama bir sayının başında görülmez, burada komut değildir.
        if harf && c != 101 && c != 69 {
            i += 1
            return c
        }
        return nil
    }

    /// Yay bayrağı: tek '0' ya da '1' (ayırıcısız yazılabilir: "a2 2 0 0110 0").
    mutating func bayrak() -> Bool? {
        ayiraclariAtla()
        guard i < baytlar.count else { return nil }
        let c: UInt8 = baytlar[i]
        if c == 48 { i += 1; return false }
        if c == 49 { i += 1; return true }
        return nil
    }

    /// Bir sayı okur ("-10", ".5", "1e-3"); sayı yoksa nil.
    mutating func sayi() -> CGFloat? {
        ayiraclariAtla()
        let bas: Int = i
        var j: Int = i
        if j < baytlar.count && (baytlar[j] == 43 || baytlar[j] == 45) { j += 1 }
        var rakamVar: Bool = false
        while j < baytlar.count && baytlar[j] >= 48 && baytlar[j] <= 57 { j += 1; rakamVar = true }
        if j < baytlar.count && baytlar[j] == 46 {
            j += 1
            while j < baytlar.count && baytlar[j] >= 48 && baytlar[j] <= 57 { j += 1; rakamVar = true }
        }
        if !rakamVar { return nil }
        if j < baytlar.count && (baytlar[j] == 101 || baytlar[j] == 69) {
            var k: Int = j + 1
            if k < baytlar.count && (baytlar[k] == 43 || baytlar[k] == 45) { k += 1 }
            var usRakami: Bool = false
            while k < baytlar.count && baytlar[k] >= 48 && baytlar[k] <= 57 { k += 1; usRakami = true }
            if usRakami { j = k }
        }
        let parca: [UInt8] = Array(baytlar[bas..<j])
        guard let metin = String(bytes: parca, encoding: .ascii), let deger = Double(metin) else { return nil }
        i = j
        return CGFloat(deger)
    }

    /// Kalan bütün sayılar.
    mutating func tumSayilar() -> [CGFloat] {
        var sonuc: [CGFloat] = []
        while let deger = sayi() {
            sonuc.append(deger)
        }
        return sonuc
    }
}
