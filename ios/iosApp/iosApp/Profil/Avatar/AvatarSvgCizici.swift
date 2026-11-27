import Foundation
import SwiftUI

// Avatarın SVG belgesini (ortak kodun `AvatarRules.svg` çıktısı) okuyup SwiftUI `Canvas` ile çizen küçük
// SVG çizici. Android: AvatarView → AndroidSVG 1.4 (`SVG.getFromString(...).renderToPicture`).
//
// Yalnızca Personas parçalarının kullandığı alt küme desteklenir (parça dosyası tarandı):
//   öğeler   : svg (viewBox), g, path, circle, ellipse, rect, mask, clipPath, defs, linearGradient, stop
//   nitelikler: fill (renk / url(#id) / none, kalıtımlı), fill-rule (kalıtımlı), fill-opacity (kalıtımlı),
//               opacity, transform (translate / scale / matrix), mask, clip-path, maskUnits ve x/y/width/height,
//               gradientUnits, x1/y1/x2/y2, offset, stop-color, stop-opacity
// Maske AndroidSVG'deki gibi her zaman PARLAKLIK maskesidir: AndroidSVG 1.4 `mask-type` niteliğini tanımıyor
// (kütüphanede bu ad hiç geçmiyor). Bu yüzden "fade" saçındaki `mask-type:alpha` maske (siyah degrade) Android'de
// içeriği tamamen gizliyor; burada da öyle çizilir (birebir karşılık).

/// Çözülmüş bir SVG öğesi.
final class AvatarSvgDugumu {
    let ad: String
    let nitelikler: [String: String]
    var cocuklar: [AvatarSvgDugumu] = []
    /// path / circle / ellipse / rect için önceden çözülmüş geometri.
    var sekil: Path? = nil

    init(ad: String, nitelikler: [String: String]) {
        self.ad = ad
        self.nitelikler = nitelikler
    }

    func nitelik(_ ad: String) -> String? {
        return nitelikler[ad]
    }

    func sayi(_ ad: String, _ varsayilan: CGFloat) -> CGFloat {
        guard let ham = nitelikler[ad] else { return varsayilan }
        return AvatarSvgDugumu.sayiCoz(ham) ?? varsayilan
    }

    /// "12.5", ".14", "50%" (yüzde 0...1'e çevrilir).
    static func sayiCoz(_ ham: String) -> CGFloat? {
        let temiz: String = ham.trimmingCharacters(in: .whitespaces)
        if temiz.hasSuffix("%") {
            guard let deger = Double(String(temiz.dropLast())) else { return nil }
            return CGFloat(deger / 100.0)
        }
        guard let deger = Double(temiz) else { return nil }
        return CGFloat(deger)
    }
}

/// Bir SVG belgesi: kök öğe, kimlik tablosu ve görünüm kutusu.
final class AvatarSvgBelgesi {
    let kok: AvatarSvgDugumu
    let kimlikler: [String: AvatarSvgDugumu]
    let gorunumKutusu: CGRect

    init(kok: AvatarSvgDugumu, kimlikler: [String: AvatarSvgDugumu], gorunumKutusu: CGRect) {
        self.kok = kok
        self.kimlikler = kimlikler
        self.gorunumKutusu = gorunumKutusu
    }

    /// SVG metnini çözer; okunamazsa nil.
    static func coz(_ metin: String) -> AvatarSvgBelgesi? {
        guard let veri = metin.data(using: .utf8) else { return nil }
        let okuyucu = AvatarSvgXmlOkuyucu()
        let ayristirici = XMLParser(data: veri)
        ayristirici.delegate = okuyucu
        ayristirici.shouldProcessNamespaces = false
        if !ayristirici.parse() { return nil }
        guard let kok = okuyucu.kok, kok.ad == "svg" else { return nil }
        var kutu = CGRect(x: 0, y: 0, width: 64, height: 64)
        if let ham = kok.nitelik("viewBox") {
            var sayac = AvatarSvgSayiOkuyucu(ham)
            let degerler: [CGFloat] = sayac.tumSayilar()
            if degerler.count == 4 && degerler[2] > 0 && degerler[3] > 0 {
                kutu = CGRect(x: degerler[0], y: degerler[1], width: degerler[2], height: degerler[3])
            }
        }
        return AvatarSvgBelgesi(kok: kok, kimlikler: okuyucu.kimlikler, gorunumKutusu: kutu)
    }
}

/// XMLParser temsilcisi: öğe ağacını kurar, şekillerin geometrisini bir kez çözer.
final class AvatarSvgXmlOkuyucu: NSObject, XMLParserDelegate {
    private(set) var kok: AvatarSvgDugumu? = nil
    private(set) var kimlikler: [String: AvatarSvgDugumu] = [:]
    private var yigin: [AvatarSvgDugumu] = []

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        let dugum = AvatarSvgDugumu(ad: elementName, nitelikler: attributeDict)
        dugum.sekil = AvatarSvgXmlOkuyucu.sekilCoz(dugum)
        if let kimlik = attributeDict["id"] {
            kimlikler[kimlik] = dugum
        }
        if let ust = yigin.last {
            ust.cocuklar.append(dugum)
        } else if kok == nil {
            kok = dugum
        }
        yigin.append(dugum)
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        if !yigin.isEmpty {
            yigin.removeLast()
        }
    }

    private static func sekilCoz(_ d: AvatarSvgDugumu) -> Path? {
        switch d.ad {
        case "path":
            guard let veri = d.nitelik("d") else { return nil }
            return AvatarSvgYolu.coz(veri)
        case "circle":
            let r: CGFloat = d.sayi("r", 0)
            if r <= 0 { return nil }
            let cx: CGFloat = d.sayi("cx", 0)
            let cy: CGFloat = d.sayi("cy", 0)
            return Path(ellipseIn: CGRect(x: cx - r, y: cy - r, width: 2 * r, height: 2 * r))
        case "ellipse":
            let rx: CGFloat = d.sayi("rx", 0)
            let ry: CGFloat = d.sayi("ry", 0)
            if rx <= 0 || ry <= 0 { return nil }
            let cx: CGFloat = d.sayi("cx", 0)
            let cy: CGFloat = d.sayi("cy", 0)
            return Path(ellipseIn: CGRect(x: cx - rx, y: cy - ry, width: 2 * rx, height: 2 * ry))
        case "rect":
            let w: CGFloat = d.sayi("width", 0)
            let h: CGFloat = d.sayi("height", 0)
            if w <= 0 || h <= 0 { return nil }
            let kutu = CGRect(x: d.sayi("x", 0), y: d.sayi("y", 0), width: w, height: h)
            var rx: CGFloat = d.sayi("rx", -1)
            var ry: CGFloat = d.sayi("ry", -1)
            if rx < 0 { rx = max(0, ry) }
            if ry < 0 { ry = rx }
            if rx <= 0 && ry <= 0 {
                return Path(kutu)
            }
            return Path(roundedRect: kutu, cornerSize: CGSize(width: min(rx, w / 2), height: min(ry, h / 2)))
        default:
            return nil
        }
    }
}

/// Kalıtımlı dolgu özellikleri.
struct AvatarSvgStil {
    /// "none", "#rrggbb", "url(#id)" …
    var dolgu: String = "#000000"
    var ciftTek: Bool = false
    var dolguOpakligi: Double = 1.0

    /// Öğenin kendi nitelikleriyle güncellenmiş kopya.
    func uygula(_ d: AvatarSvgDugumu) -> AvatarSvgStil {
        var yeni: AvatarSvgStil = self
        if let dolgu = d.nitelik("fill") {
            yeni.dolgu = dolgu.trimmingCharacters(in: .whitespaces)
        }
        if let kural = d.nitelik("fill-rule") {
            yeni.ciftTek = kural.trimmingCharacters(in: .whitespaces) == "evenodd"
        }
        if let ham = d.nitelik("fill-opacity"), let deger = AvatarSvgDugumu.sayiCoz(ham) {
            yeni.dolguOpakligi = Double(min(1, max(0, deger)))
        }
        return yeni
    }
}

/// SVG belgesini bir `GraphicsContext`'e çizer.
struct AvatarSvgCizici {
    let belge: AvatarSvgBelgesi

    /// Android AvatarView.onDraw: görünüm kutusu alana orantılı sığdırılır ve ortalanır (xMidYMid meet).
    func ciz(_ baglam: GraphicsContext, boyut: CGSize) {
        let kutu: CGRect = belge.gorunumKutusu
        if boyut.width <= 0 || boyut.height <= 0 { return }
        let olcek: CGFloat = min(boyut.width / kutu.width, boyut.height / kutu.height)
        let solBosluk: CGFloat = (boyut.width - kutu.width * olcek) / 2
        let ustBosluk: CGFloat = (boyut.height - kutu.height * olcek) / 2
        var c: GraphicsContext = baglam
        c.translateBy(x: solBosluk, y: ustBosluk)
        c.scaleBy(x: olcek, y: olcek)
        c.translateBy(x: -kutu.minX, y: -kutu.minY)
        // Bir öğede nitelik yoksa SVG'nin başlangıç dolgusu siyahtır; kök `fill="none"` bunu ezer.
        let stil: AvatarSvgStil = AvatarSvgStil().uygula(belge.kok)
        cocuklariCiz(belge.kok, c, stil, maske: false)
    }

    private func cocuklariCiz(_ d: AvatarSvgDugumu, _ c: GraphicsContext, _ stil: AvatarSvgStil, maske: Bool) {
        for cocuk in d.cocuklar {
            ogeCiz(cocuk, c, stil, maske: maske)
        }
    }

    private func ogeCiz(_ d: AvatarSvgDugumu, _ baglam: GraphicsContext, _ ustStil: AvatarSvgStil, maske: Bool) {
        switch d.ad {
        case "g", "path", "circle", "ellipse", "rect":
            break
        default:
            // defs, mask, clipPath, linearGradient, stop ve tanınmayan öğeler doğrudan çizilmez.
            return
        }
        let stil: AvatarSvgStil = ustStil.uygula(d)
        var c: GraphicsContext = baglam
        if let donusum = d.nitelik("transform") {
            c.concatenate(AvatarSvgCizici.donusumCoz(donusum))
        }
        let opaklik: CGFloat = min(1, max(0, d.sayi("opacity", 1)))
        if opaklik <= 0 { return }
        if opaklik < 1 {
            // Grup opaklığı alt öğelere çarpılarak uygulanır (parçalarda <g opacity> yok; şekillerde birebir).
            c.opacity *= Double(opaklik)
        }
        if let kimlik = AvatarSvgCizici.referans(d.nitelik("clip-path")),
           let kirpma = belge.kimlikler[kimlik], kirpma.ad == "clipPath" {
            c.clip(to: kirpmaYolu(kirpma))
        }
        if let kimlik = AvatarSvgCizici.referans(d.nitelik("mask")),
           let maskeDugumu = belge.kimlikler[kimlik], maskeDugumu.ad == "mask" {
            maskeUygula(maskeDugumu, &c, stil)
        }
        if d.ad == "g" {
            cocuklariCiz(d, c, stil, maske: maske)
        } else if let sekil = d.sekil {
            doldur(sekil, c, stil, maske: maske)
        }
    }

    // MARK: Dolgu

    private func doldur(_ yol: Path, _ c: GraphicsContext, _ stil: AvatarSvgStil, maske: Bool) {
        let dolgu: String = stil.dolgu
        if dolgu == "none" || dolgu.isEmpty { return }
        let kural = FillStyle(eoFill: stil.ciftTek, antialiased: true)
        if let kimlik = AvatarSvgCizici.referans(dolgu) {
            guard let degrade = belge.kimlikler[kimlik], degrade.ad == "linearGradient" else { return }
            let golgeleme: GraphicsContext.Shading = degradeGolgelemesi(degrade, yol, stil.dolguOpakligi, maske: maske)
            c.fill(yol, with: golgeleme, style: kural)
            return
        }
        guard let renk = AvatarSvgRenk.coz(dolgu) else { return }
        let alfa: Double = renk.alfa * stil.dolguOpakligi
        c.fill(yol, with: .color(AvatarSvgRenk.renk(renk, alfa: alfa, maske: maske)), style: kural)
    }

    private func degradeGolgelemesi(
        _ d: AvatarSvgDugumu, _ yol: Path, _ dolguOpakligi: Double, maske: Bool
    ) -> GraphicsContext.Shading {
        var duraklar: [Gradient.Stop] = []
        var oncekiKonum: CGFloat = 0
        for durak in d.cocuklar where durak.ad == "stop" {
            var konum: CGFloat = min(1, max(0, durak.sayi("offset", 0)))
            // SVG: bir durak öncekinden geride olamaz.
            konum = max(konum, oncekiKonum)
            oncekiKonum = konum
            let renk: AvatarSvgRenk = AvatarSvgRenk.coz(durak.nitelik("stop-color") ?? "#000000") ?? AvatarSvgRenk.siyah
            let durakOpakligi: Double = Double(min(1, max(0, durak.sayi("stop-opacity", 1))))
            let alfa: Double = renk.alfa * durakOpakligi * dolguOpakligi
            duraklar.append(Gradient.Stop(color: AvatarSvgRenk.renk(renk, alfa: alfa, maske: maske), location: konum))
        }
        if duraklar.isEmpty {
            return .color(Color.clear)
        }
        if duraklar.count == 1 {
            return .color(duraklar[0].color)
        }
        var bas = CGPoint(x: d.sayi("x1", 0), y: d.sayi("y1", 0))
        var son = CGPoint(x: d.sayi("x2", 1), y: d.sayi("y2", 0))
        if d.nitelik("gradientUnits") != "userSpaceOnUse" {
            // objectBoundingBox (varsayılan): 0...1 şeklin sınır kutusuna göre.
            let k: CGRect = yol.boundingRect
            bas = CGPoint(x: k.minX + bas.x * k.width, y: k.minY + bas.y * k.height)
            son = CGPoint(x: k.minX + son.x * k.width, y: k.minY + son.y * k.height)
        }
        return .linearGradient(Gradient(stops: duraklar), startPoint: bas, endPoint: son, options: [])
    }

    // MARK: Kırpma ve maske

    private func kirpmaYolu(_ kirpma: AvatarSvgDugumu) -> Path {
        var yol = Path()
        for cocuk in kirpma.cocuklar {
            guard let sekil = cocuk.sekil else { continue }
            if let donusum = cocuk.nitelik("transform") {
                yol.addPath(sekil, transform: AvatarSvgCizici.donusumCoz(donusum))
            } else {
                yol.addPath(sekil)
            }
        }
        return yol
    }

    /// Parlaklık maskesi: maske içeriği beyaz, alfası (parlaklık x opaklık) olacak şekilde bir katmana çizilir
    /// ve bağlam o katmanın alfasıyla kırpılır. maskUnits="userSpaceOnUse" ise x/y/width/height dışı da kırpılır.
    private func maskeUygula(_ m: AvatarSvgDugumu, _ c: inout GraphicsContext, _ stil: AvatarSvgStil) {
        if m.nitelik("maskUnits") == "userSpaceOnUse",
           let w = m.nitelik("width").flatMap({ AvatarSvgDugumu.sayiCoz($0) }),
           let h = m.nitelik("height").flatMap({ AvatarSvgDugumu.sayiCoz($0) }) {
            let x: CGFloat = m.sayi("x", 0)
            let y: CGFloat = m.sayi("y", 0)
            c.clip(to: Path(CGRect(x: x, y: y, width: w, height: h)))
        }
        let maskeStili: AvatarSvgStil = stil.uygula(m)
        c.clipToLayer(opacity: 1, options: []) { katman in
            self.cocuklariCiz(m, katman, maskeStili, maske: true)
        }
    }

    // MARK: Yardımcılar

    /// "url(#kimlik)" → "kimlik".
    static func referans(_ deger: String?) -> String? {
        guard let ham = deger?.trimmingCharacters(in: .whitespaces), ham.hasPrefix("url(") else { return nil }
        var ic: String = String(ham.dropFirst(4))
        if let kapanis = ic.firstIndex(of: ")") {
            ic = String(ic[ic.startIndex..<kapanis])
        }
        ic = ic.trimmingCharacters(in: CharacterSet(charactersIn: " '\""))
        if ic.hasPrefix("#") {
            ic = String(ic.dropFirst())
        }
        return ic.isEmpty ? nil : ic
    }

    /// "translate(20 24)", "scale(2)", "matrix(a b c d e f)" (birden çoğu soldan sağa).
    static func donusumCoz(_ metin: String) -> CGAffineTransform {
        var sonuc: CGAffineTransform = .identity
        var kalan: Substring = Substring(metin)
        while let acilis = kalan.firstIndex(of: "("), let kapanis = kalan.firstIndex(of: ")") {
            if kapanis < acilis { break }
            let ad: String = kalan[kalan.startIndex..<acilis].trimmingCharacters(in: CharacterSet(charactersIn: " ,\t\n"))
            let icerik: String = String(kalan[kalan.index(after: acilis)..<kapanis])
            var okuyucu = AvatarSvgSayiOkuyucu(icerik)
            let s: [CGFloat] = okuyucu.tumSayilar()
            var bu: CGAffineTransform = .identity
            if ad == "translate" && !s.isEmpty {
                bu = CGAffineTransform(translationX: s[0], y: s.count > 1 ? s[1] : 0)
            } else if ad == "scale" && !s.isEmpty {
                bu = CGAffineTransform(scaleX: s[0], y: s.count > 1 ? s[1] : s[0])
            } else if ad == "matrix" && s.count == 6 {
                bu = CGAffineTransform(a: s[0], b: s[1], c: s[2], d: s[3], tx: s[4], ty: s[5])
            } else if ad == "rotate" && !s.isEmpty {
                let aci: CGFloat = s[0] * .pi / 180
                if s.count == 3 {
                    bu = CGAffineTransform(translationX: s[1], y: s[2])
                        .rotated(by: aci)
                        .translatedBy(x: -s[1], y: -s[2])
                } else {
                    bu = CGAffineTransform(rotationAngle: aci)
                }
            }
            // SVG'de listedeki dönüşümler soldan sağa uygulanır: önce sonuç, sonra yeni dönüşüm (iç koordinat).
            sonuc = bu.concatenating(sonuc)
            kalan = kalan[kalan.index(after: kapanis)...]
        }
        return sonuc
    }
}

/// SVG rengi (sRGB, 0...1).
struct AvatarSvgRenk {
    let kirmizi: Double
    let yesil: Double
    let mavi: Double
    let alfa: Double

    static let siyah = AvatarSvgRenk(kirmizi: 0, yesil: 0, mavi: 0, alfa: 1)

    /// "#rgb", "#rrggbb", "black", "white"; diğerleri nil.
    static func coz(_ ham: String) -> AvatarSvgRenk? {
        let metin: String = ham.trimmingCharacters(in: .whitespaces).lowercased()
        if metin == "black" { return siyah }
        if metin == "white" { return AvatarSvgRenk(kirmizi: 1, yesil: 1, mavi: 1, alfa: 1) }
        guard metin.hasPrefix("#") else { return nil }
        let onaltilik: String = String(metin.dropFirst())
        guard let deger = UInt32(onaltilik, radix: 16) else { return nil }
        if onaltilik.count == 3 {
            let r: Double = Double((deger >> 8) & 0xF) / 15.0
            let g: Double = Double((deger >> 4) & 0xF) / 15.0
            let b: Double = Double(deger & 0xF) / 15.0
            return AvatarSvgRenk(kirmizi: r, yesil: g, mavi: b, alfa: 1)
        }
        if onaltilik.count == 6 {
            let r: Double = Double((deger >> 16) & 0xFF) / 255.0
            let g: Double = Double((deger >> 8) & 0xFF) / 255.0
            let b: Double = Double(deger & 0xFF) / 255.0
            return AvatarSvgRenk(kirmizi: r, yesil: g, mavi: b, alfa: 1)
        }
        return nil
    }

    /// Çizim rengi. Maske içinde renk beyaza, alfa da (parlaklık x alfa)'ya çevrilir (parlaklık maskesi).
    static func renk(_ r: AvatarSvgRenk, alfa: Double, maske: Bool) -> Color {
        let sinirli: Double = min(1, max(0, alfa))
        if maske {
            let parlaklik: Double = 0.2125 * r.kirmizi + 0.7154 * r.yesil + 0.0721 * r.mavi
            return Color(.sRGB, red: 1, green: 1, blue: 1, opacity: min(1, max(0, parlaklik * sinirli)))
        }
        return Color(.sRGB, red: r.kirmizi, green: r.yesil, blue: r.mavi, opacity: sinirli)
    }
}
