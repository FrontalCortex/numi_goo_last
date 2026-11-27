import SwiftUI

// Sobi maskotunun çizim temeli: üretilmiş yol verisinin türü, yol çözücü ve Android `Canvas`
// karşılığı küçük bir sarmalayıcı (save/restore, translate/rotate/scale, saveLayerAlpha, kırpma).
//
// Android: BunnyMascotView her şeyi `Canvas` üzerine, kaynak çizimin koordinatlarında çiziyor.
// Burada aynı koordinatlarda SwiftUI `GraphicsContext` kullanılıyor. `GraphicsContext` bir değer
// türü: kopyası durum (dönüşüm, kırpma, saydamlık) anlık görüntüsüdür, çizim aynı hedefe gider.
// `kaydet()` / `geriAl()` bu kopyaları bir yığında tutuyor.

/// Üretilmiş tek bir şekil (bkz. SobiMaskotCizimVerisi.swift). Yol ilk kullanımda çözülüyor.
struct SobiMaskotSekil {
    let yol: Path
    /// Dolgu rengi 0xAARRGGBB; 0 = dolgu yok.
    let dolgu: UInt32
    /// Çizgi rengi 0xAARRGGBB; 0 = çizgi yok.
    let cizgi: UInt32
    let kalinlik: CGFloat
    let ciftTek: Bool

    init(d: String, dolgu: UInt32, cizgi: UInt32, kalinlik: CGFloat, ciftTek: Bool) {
        self.yol = SobiMaskotSekil.yolCoz(d)
        self.dolgu = dolgu
        self.cizgi = cizgi
        self.kalinlik = kalinlik
        self.ciftTek = ciftTek
    }

    /// Üreticinin yazdığı yalın biçimi okur: "M x y", "L x y", "C x1 y1 x2 y2 x y", "Z".
    static func yolCoz(_ d: String) -> Path {
        var p = Path()
        let parcalar: [Substring] = d.split(separator: " ")
        var i: Int = 0
        func sayi() -> CGFloat {
            if i >= parcalar.count { return 0 }
            let ham: Substring = parcalar[i]
            i += 1
            let deger: Double = Double(ham) ?? 0
            return CGFloat(deger)
        }
        while i < parcalar.count {
            let komut: Substring = parcalar[i]
            i += 1
            if komut == "M" {
                let x: CGFloat = sayi()
                let y: CGFloat = sayi()
                p.move(to: CGPoint(x: x, y: y))
            } else if komut == "L" {
                let x: CGFloat = sayi()
                let y: CGFloat = sayi()
                p.addLine(to: CGPoint(x: x, y: y))
            } else if komut == "C" {
                let x1: CGFloat = sayi()
                let y1: CGFloat = sayi()
                let x2: CGFloat = sayi()
                let y2: CGFloat = sayi()
                let x: CGFloat = sayi()
                let y: CGFloat = sayi()
                p.addCurve(
                    to: CGPoint(x: x, y: y),
                    control1: CGPoint(x: x1, y: y1),
                    control2: CGPoint(x: x2, y: y2)
                )
            } else if komut == "Z" {
                p.closeSubpath()
            }
        }
        return p
    }
}

/// Küçük matematik yardımcıları (Kotlin `coerceIn`, `%` vb.).
enum SobiMaskotMat {
    static func sinirla(_ x: CGFloat, _ alt: CGFloat, _ ust: CGFloat) -> CGFloat {
        if x < alt { return alt }
        if x > ust { return ust }
        return x
    }

    /// Kotlin'in kayan noktalı `%` işlemi (bölünenin işaretini korur).
    static func kalan(_ a: CGFloat, _ b: CGFloat) -> CGFloat {
        return a.truncatingRemainder(dividingBy: b)
    }

    static func radyan(_ derece: CGFloat) -> CGFloat {
        return derece * CGFloat.pi / 180
    }

    /// 0xAARRGGBB → Color; [alfa] rengin kendi alfasıyla çarpılıyor.
    static func renk(_ argb: UInt32, _ alfa: CGFloat = 1) -> Color {
        let a: Double = Double((argb >> 24) & 0xFF) / 255.0
        let r: Double = Double((argb >> 16) & 0xFF) / 255.0
        let g: Double = Double((argb >> 8) & 0xFF) / 255.0
        let b: Double = Double(argb & 0xFF) / 255.0
        let k: Double = Double(sinirla(alfa, 0, 1))
        return Color(.sRGB, red: r, green: g, blue: b, opacity: a * k)
    }

    /// Android LightingColorFilter(mul = (k,k,k), add = 0) karşılığı: RGB'yi [carpan] ile çarpar.
    static func renkCarp(_ argb: UInt32, _ carpan: CGFloat) -> UInt32 {
        if carpan >= 1 { return argb }
        let a: UInt32 = (argb >> 24) & 0xFF
        let r: CGFloat = CGFloat((argb >> 16) & 0xFF) * carpan
        let g: CGFloat = CGFloat((argb >> 8) & 0xFF) * carpan
        let b: CGFloat = CGFloat(argb & 0xFF) * carpan
        let ri: UInt32 = UInt32(max(0, min(255, r.rounded())))
        let gi: UInt32 = UInt32(max(0, min(255, g.rounded())))
        let bi: UInt32 = UInt32(max(0, min(255, b.rounded())))
        return (a << 24) | (ri << 16) | (gi << 8) | bi
    }

    /// Android `drawArc(oval, start, sweep, useCenter)` yolu: açılar derece, saat yönü artı
    /// (y aşağı), elips üzerinde. Kübik eğrilerle (en çok 90°'lik parçalar).
    static func yay(_ r: CGRect, _ baslangic: CGFloat, _ tarama: CGFloat, merkezli: Bool) -> Path {
        var p = Path()
        let cx: CGFloat = r.midX
        let cy: CGFloat = r.midY
        let rx: CGFloat = r.width / 2
        let ry: CGFloat = r.height / 2
        let adet: Int = max(1, Int((abs(tarama) / 90).rounded(.up)))
        let parca: CGFloat = tarama / CGFloat(adet)
        let k: CGFloat = 4.0 / 3.0 * tan(radyan(parca) / 4)
        let ilkAci: CGFloat = radyan(baslangic)
        let ilk = CGPoint(x: cx + rx * cos(ilkAci), y: cy + ry * sin(ilkAci))
        if merkezli {
            p.move(to: CGPoint(x: cx, y: cy))
            p.addLine(to: ilk)
        } else {
            p.move(to: ilk)
        }
        for i in 0..<adet {
            let a: CGFloat = radyan(baslangic + parca * CGFloat(i))
            let b: CGFloat = radyan(baslangic + parca * CGFloat(i + 1))
            let p0 = CGPoint(x: cx + rx * cos(a), y: cy + ry * sin(a))
            let p3 = CGPoint(x: cx + rx * cos(b), y: cy + ry * sin(b))
            let c1 = CGPoint(x: p0.x - k * rx * sin(a), y: p0.y + k * ry * cos(a))
            let c2 = CGPoint(x: p3.x + k * rx * sin(b), y: p3.y - k * ry * cos(b))
            p.addCurve(to: p3, control1: c1, control2: c2)
        }
        if merkezli { p.closeSubpath() }
        return p
    }

    static func daire(_ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat) -> Path {
        return Path(ellipseIn: CGRect(x: cx - r, y: cy - r, width: 2 * r, height: 2 * r))
    }

    /// Android RectF(sol, üst, sağ, alt) karşılığı.
    static func dortgen(_ sol: CGFloat, _ ust: CGFloat, _ sag: CGFloat, _ alt: CGFloat) -> CGRect {
        return CGRect(x: sol, y: ust, width: sag - sol, height: alt - ust)
    }

    static func oval(_ sol: CGFloat, _ ust: CGFloat, _ sag: CGFloat, _ alt: CGFloat) -> Path {
        return Path(ellipseIn: dortgen(sol, ust, sag, alt))
    }

    /// Android drawRoundRect; yarıçap kenarın yarısını aşmasın diye kırpılıyor.
    static func yuvarlak(_ r: CGRect, _ yaricap: CGFloat) -> Path {
        let w: CGFloat = max(0, min(yaricap, r.width / 2))
        let h: CGFloat = max(0, min(yaricap, r.height / 2))
        return Path(roundedRect: r, cornerSize: CGSize(width: w, height: h))
    }

    static func cizgiYolu(_ x0: CGFloat, _ y0: CGFloat, _ x1: CGFloat, _ y1: CGFloat) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: x0, y: y0))
        p.addLine(to: CGPoint(x: x1, y: y1))
        return p
    }
}

/// Android `Canvas` karşılığı. Bir kare boyunca yaşıyor.
final class SobiMaskotTuval {
    var ctx: GraphicsContext
    private var yigin: [GraphicsContext] = []

    init(_ ctx: GraphicsContext) {
        self.ctx = ctx
    }

    // MARK: Durum

    func kaydet() {
        yigin.append(ctx)
    }

    func geriAl() {
        if let onceki = yigin.popLast() {
            ctx = onceki
        }
    }

    func tasi(_ x: CGFloat, _ y: CGFloat) {
        ctx.translateBy(x: x, y: y)
    }

    /// Android `rotate(deg)`: saat yönü artı (y aşağı eksende).
    func dondur(_ derece: CGFloat) {
        ctx.rotate(by: Angle.degrees(Double(derece)))
    }

    /// Android `rotate(deg, px, py)`.
    func dondur(_ derece: CGFloat, _ px: CGFloat, _ py: CGFloat) {
        ctx.translateBy(x: px, y: py)
        ctx.rotate(by: Angle.degrees(Double(derece)))
        ctx.translateBy(x: -px, y: -py)
    }

    func olcekle(_ sx: CGFloat, _ sy: CGFloat) {
        ctx.scaleBy(x: sx, y: sy)
    }

    /// Android `scale(sx, sy, px, py)`.
    func olcekle(_ sx: CGFloat, _ sy: CGFloat, _ px: CGFloat, _ py: CGFloat) {
        ctx.translateBy(x: px, y: py)
        ctx.scaleBy(x: sx, y: sy)
        ctx.translateBy(x: -px, y: -py)
    }

    func kirp(_ yol: Path) {
        ctx.clip(to: yol)
    }

    /// Android `clipOutPath`: yolun içi çizime kapanıyor.
    func kirpDisi(_ yol: Path) {
        ctx.clip(to: yol, style: FillStyle(), options: .inverse)
    }

    /// Android `withAlpha` (BunnyMascotView): tam görünürse yalnızca kaydet/geri al, kısmi
    /// saydamlıkta bütün blok bir katmanda çizilip o saydamlıkla bindiriliyor (saveLayerAlpha).
    func alfaIle(_ alfa: CGFloat, _ blok: () -> Void) {
        if alfa <= 0.01 { return }
        if alfa >= 0.99 {
            kaydet()
            blok()
            geriAl()
            return
        }
        katman(alfa, blok)
    }

    /// Bloğu ayrı bir saydam katmanda çizip [alfa] ile bindirir.
    func katman(_ alfa: CGFloat, _ blok: () -> Void) {
        let onceki: GraphicsContext = ctx
        let oncekiYigin: [GraphicsContext] = yigin
        var dis: GraphicsContext = ctx
        dis.opacity = dis.opacity * Double(SobiMaskotMat.sinirla(alfa, 0, 1))
        dis.drawLayer { ic in
            // Katmanın içi tam opak çizilir; saydamlık katman bindirilirken uygulanır.
            ic.opacity = 1
            self.ctx = ic
            self.yigin = []
            blok()
        }
        ctx = onceki
        yigin = oncekiYigin
    }

    /// Foto flaşı katmanı: [blok] çizilir, ardından katman yalnızca çizili yerlerinde
    /// [boya] ile boyanır (Android PorterDuff SRC_ATOP).
    func atopKatmani(_ blok: () -> Void, boya: (inout GraphicsContext) -> Void) {
        let onceki: GraphicsContext = ctx
        let oncekiYigin: [GraphicsContext] = yigin
        let dis: GraphicsContext = ctx
        dis.drawLayer { ic in
            self.ctx = ic
            self.yigin = []
            blok()
            var son: GraphicsContext = ic
            son.blendMode = .sourceAtop
            boya(&son)
        }
        ctx = onceki
        yigin = oncekiYigin
    }

    // MARK: Çizim

    func doldur(_ yol: Path, _ argb: UInt32, _ alfa: CGFloat = 1, ciftTek: Bool = false) {
        if alfa <= 0 { return }
        ctx.fill(yol, with: .color(SobiMaskotMat.renk(argb, alfa)), style: FillStyle(eoFill: ciftTek, antialiased: true))
    }

    func ciz(_ yol: Path, _ argb: UInt32, _ kalinlik: CGFloat, _ alfa: CGFloat = 1, keskin: Bool = false) {
        if alfa <= 0 { return }
        let stil = StrokeStyle(
            lineWidth: kalinlik,
            lineCap: .round,
            lineJoin: keskin ? .miter : .round
        )
        ctx.stroke(yol, with: .color(SobiMaskotMat.renk(argb, alfa)), style: stil)
    }

    func cizgi(_ x0: CGFloat, _ y0: CGFloat, _ x1: CGFloat, _ y1: CGFloat, _ argb: UInt32, _ kalinlik: CGFloat, _ alfa: CGFloat = 1) {
        ciz(SobiMaskotMat.cizgiYolu(x0, y0, x1, y1), argb, kalinlik, alfa)
    }

    func daireDoldur(_ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ argb: UInt32, _ alfa: CGFloat = 1) {
        if r <= 0 { return }
        doldur(SobiMaskotMat.daire(cx, cy, r), argb, alfa)
    }

    func daireCiz(_ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ argb: UInt32, _ kalinlik: CGFloat, _ alfa: CGFloat = 1) {
        if r <= 0 { return }
        ciz(SobiMaskotMat.daire(cx, cy, r), argb, kalinlik, alfa)
    }

    /// Üretilmiş şekil listesi; [renkCarpani] < 1 ise RGB koyulaştırılıyor (billboard kararması).
    func sekiller(_ liste: [SobiMaskotSekil], _ alfa: CGFloat = 1, renkCarpani: CGFloat = 1) {
        for s in liste {
            sekil(s, alfa, renkCarpani: renkCarpani)
        }
    }

    func sekil(_ s: SobiMaskotSekil, _ alfa: CGFloat = 1, renkCarpani: CGFloat = 1) {
        if s.dolgu != 0 {
            doldur(s.yol, SobiMaskotMat.renkCarp(s.dolgu, renkCarpani), alfa, ciftTek: s.ciftTek)
        }
        if s.cizgi != 0 && s.kalinlik > 0 {
            ciz(s.yol, SobiMaskotMat.renkCarp(s.cizgi, renkCarpani), s.kalinlik, alfa)
        }
    }

    /// Vektör çizimi (Android Drawable) [boyut] sınırlarına sığdırıp [alfa] ile çizer.
    /// Android'de Drawable alfası bütün çizime uygulanıyor; o yüzden kısmi alfada katman.
    func cizim(_ liste: [SobiMaskotSekil], alani: CGSize, boyut: CGSize, _ alfa: CGFloat = 1, renkCarpani: CGFloat = 1) {
        if alfa <= 0.01 { return }
        let sx: CGFloat = boyut.width / alani.width
        let sy: CGFloat = boyut.height / alani.height
        alfaIle(alfa) {
            self.olcekle(sx, sy)
            self.sekiller(liste, 1, renkCarpani: renkCarpani)
        }
    }

    /// Android `drawText` (Typeface.DEFAULT_BOLD, ortaya hizalı): ([x], [y]) yazının taban
    /// çizgisinin ortası.
    func yazi(_ metin: String, _ x: CGFloat, _ y: CGFloat, _ boyut: CGFloat, _ argb: UInt32, _ alfa: CGFloat) {
        let a: CGFloat = SobiMaskotMat.sinirla(alfa, 0, 1)
        if a <= 0 || boyut <= 0.1 { return }
        let t: Text = Text(verbatim: metin)
            .font(Font.system(size: boyut, weight: .bold))
            .foregroundColor(SobiMaskotMat.renk(argb, a))
        let cozulen: GraphicsContext.ResolvedText = ctx.resolve(t)
        let olcu: CGSize = cozulen.measure(in: CGSize(width: 10_000, height: 10_000))
        let taban: CGFloat = cozulen.firstBaseline(in: olcu)
        let kutu = CGRect(x: x - olcu.width / 2, y: y - taban, width: olcu.width, height: olcu.height)
        ctx.draw(cozulen, in: kutu)
    }

    /// Android `measureText` karşılığı (aynı yazı tipiyle).
    func yaziGenisligi(_ metin: String, _ boyut: CGFloat) -> CGFloat {
        let t: Text = Text(verbatim: metin).font(Font.system(size: boyut, weight: .bold))
        let cozulen: GraphicsContext.ResolvedText = ctx.resolve(t)
        let olcu: CGSize = cozulen.measure(in: CGSize(width: 10_000, height: 10_000))
        return olcu.width
    }
}
