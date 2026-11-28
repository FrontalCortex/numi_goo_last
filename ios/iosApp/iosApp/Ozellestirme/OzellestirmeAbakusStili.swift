import Shared
import UIKit

// Kullanıcının kayıtlı boncuk ve çerçeve tercihlerinden abaküsün görünümünü (AbakusStili) kurar.
//
// Android karşılığı: AbacusFragment / BlindingLessonFragment `onResume → applyCustomization()`
// (çerçeve: AbacusFrameRenderer.buildFrameDrawable; boncuklar: AbacusBeadController.updateBeadAppearance →
// AbacusBeadRenderer.buildBeadForType, yuva başına tip). Hangi kaynağın ve hangi renklerin çizileceğini
// ortak kod söyler (AbacusCustomizationUiLogic.beadPaint / framePaint); çizim OzellestirmeBoyaci'ndadır.
// Özelleştirme ekranıyla AYNI depo ve AYNI resim önbelleği kullanılır, bu yüzden iki ekran aynı görünür.
//
// Android'de öğretici (TutorialFragment.setupBeads) kullanıcı tercihini bilerek yok sayar; iOS'ta da
// öğreticiye bu stil VERİLMEZ. Ders ekranları stili ekran kurulurken okur (Android: onResume).
enum OzellestirmeAbakusStili {

    /// Uygulamanın özelleştirme bağımlılıklarındaki depo ve kullanıcı kimliğiyle kurar.
    static func mevcut() -> AbakusStili {
        let servisler: OzellestirmeBagimliliklar = OzellestirmeBagimliliklar.sahte
        return kur(depo: servisler.depo, kimlik: servisler.kullaniciKimligi)
    }

    /// - Parameters:
    ///   - depo: abaküs tercihlerinin yerel deposu (`OzellestirmeYerelDepo`).
    ///   - kimlik: oturum açmış kullanıcının kimliği; yuva kayıtlarının anahtarını belirler.
    static func kur(depo: AbacusPrefsStore, kimlik: String?) -> AbakusStili {
        let ui = AbacusCustomizationUiLogic.shared
        var stil: AbakusStili = AbakusStili()

        // Boncuklar.
        let yuvalar = ui.slotBeads(store: depo, uid: kimlik)
        var hepsiSoroban: Bool = true
        for yuva in yuvalar {
            if yuva.beadType.name != "SOROBAN" {
                hepsiSoroban = false
                break
            }
        }
        if hepsiSoroban {
            // Yalnızca soroban: katman renkleri vektör çizimine verilir (varsayılan renklerde varsayılan görünüm).
            let normal = ui.beadPaint(store: depo, beadType: AbacusPreferences.BeadType.soroban, selected: false)
            let secili = ui.beadPaint(store: depo, beadType: AbacusPreferences.BeadType.soroban, selected: true)
            stil.boncukRenkleri = normal.colors.map { OzellestirmeStil.uiRenk(argb: $0.int32Value) }
            stil.seciliBoncukRenkleri = secili.colors.map { OzellestirmeStil.uiRenk(argb: $0.int32Value) }
        } else {
            var resimler: [AbakusBoncukYuvasi: AbakusBoncukResimleri] = [:]
            for yuva in yuvalar {
                guard let normal = boncukResmi(depo: depo, tip: yuva.beadType, secili: false),
                      let secili = boncukResmi(depo: depo, tip: yuva.beadType, secili: true)
                else { continue }
                let anahtar = AbakusBoncukYuvasi(
                    cubuk: Int(yuva.rod), ust: yuva.isTop, sira: Int(yuva.beadIndex)
                )
                resimler[anahtar] = AbakusBoncukResimleri(normal: normal, secili: secili)
            }
            stil.yuvaResimleri = resimler
        }

        // Çerçeve.
        cerceveyiUygula(depo: depo, stil: &stil)
        return stil
    }

    // MARK: Boncuk

    /// Bir boncuk tipinin kullanıcı renkleriyle boyanmış resmi (OzellestirmeModeli.boncukResmi ile aynı kurallar).
    private static func boncukResmi(
        depo: AbacusPrefsStore,
        tip: AbacusPreferences.BeadType,
        secili: Bool
    ) -> UIImage? {
        let ui = AbacusCustomizationUiLogic.shared
        let boya = ui.beadPaint(store: depo, beadType: tip, selected: secili)
        return OzellestirmeBoyaci.onbellekten("boncuk_" + boya.cacheKey) {
            let renkler: [UIColor] = boya.colors.map { OzellestirmeStil.uiRenk(argb: $0.int32Value) }
            if boya.kind == "TINTED_LAYERS" {
                return OzellestirmeBoyaci.katmanliResim(gorselAdlari: boya.resNames, renkler: renkler)
            }
            if boya.kind == "GRADIENT_PILL" {
                let g = ui.pillGeometry()
                return OzellestirmeBoyaci.hapResmi(
                    boyut: CGSize(width: CGFloat(g.width), height: CGFloat(g.height)),
                    disYaricap: CGSize(width: CGFloat(g.outerRadiusX), height: CGFloat(g.outerRadiusY)),
                    bosluk: CGSize(width: CGFloat(g.padX), height: CGFloat(g.padY)),
                    icYaricap: CGSize(width: CGFloat(g.innerRadiusX), height: CGFloat(g.innerRadiusY)),
                    renkler: renkler,
                    duraklar: g.stops.map { CGFloat($0.floatValue) }
                )
            }
            // REPLACED_BITMAP: renkler varsayılansa kaynak görsel olduğu gibi kullanılır.
            guard let gorselAdi = boya.resNames.first else { return nil }
            if boya.isDefaultColors {
                return OzellestirmeBoyaci.hamResim(gorselAdi)
            }
            return OzellestirmeBoyaci.pikselleriIslenmisResim(
                gorselAdi: gorselAdi,
                genislik: OzellestirmeStil.boncukPikselGenisligi,
                yukseklik: nil
            ) { pikseller, en, boy in
                ui.recolorBeadPixels(
                    store: depo, beadType: tip, selected: secili,
                    pixels: pikseller, width: en, height: boy
                )
            }
        }
    }

    // MARK: Çerçeve

    /// Kayıtlı çerçeveyi stile işler (Android: AbacusFrameRenderer.buildFrameDrawable).
    /// FRAME_BG yalnızca ahşap rengidir; FRAME_BG2/3 boyanmış resim, FRAME_BG4 tuvalde çizilen katmanlardır.
    private static func cerceveyiUygula(depo: AbacusPrefsStore, stil: inout AbakusStili) {
        let ui = AbacusCustomizationUiLogic.shared
        let boya = ui.framePaint(store: depo, frameType: nil)

        if boya.kind == ui.FRAME_KIND_INSET_LAYERS {
            if let ahsap = boya.colors.first {
                stil.cerceveRengi = OzellestirmeStil.uiRenk(argb: ahsap.int32Value)
            }
            return
        }

        if boya.kind == ui.FRAME_KIND_CANVAS_LAYERS {
            stil.cerceveResmi = OzellestirmeBoyaci.onbellekten("cerceve_" + boya.cacheKey) {
                tuvalCercevesi(boya: boya)
            }
            return
        }

        guard let gorselAdi = boya.resName else { return }
        let tip: AbacusPreferences.FrameType = boya.frameType
        let kenar: Int = Int(boya.canvasSize)
        stil.cerceveResmi = OzellestirmeBoyaci.onbellekten("cerceve_" + boya.cacheKey) {
            OzellestirmeBoyaci.pikselleriIslenmisResim(
                gorselAdi: gorselAdi, genislik: kenar, yukseklik: kenar
            ) { pikseller, _, _ in
                ui.recolorFramePixels(store: depo, frameType: tip, pixels: pikseller)
            }
        }
    }

    /// FRAME_BG4: `canvasLayers` kare tuvalde (kenar = canvasSize) sırayla çizilir; kaba gerilerek kullanılır.
    /// Katmanın sağ ve alt değerleri tuval koordinatıdır; gradyan katmanı kendi alanında sol alttan sağ üste boyanır
    /// (Özelleştirme önizlemesi `OzellestirmeCerceveGorunumu` ile aynı).
    private static func tuvalCercevesi(boya: AbacusCustomizationUiLogic.FramePaint) -> UIImage? {
        let kenar: CGFloat = CGFloat(boya.canvasSize)
        if kenar <= 0 { return nil }

        let gradyanRenkleri: [CGColor] = boya.colors.map { OzellestirmeStil.uiRenk(argb: $0.int32Value).cgColor }
        let duraklar: [CGFloat] = boya.gradientStops.map { CGFloat($0.floatValue) }
        var gradyan: CGGradient? = nil
        if gradyanRenkleri.count >= 2 && gradyanRenkleri.count == duraklar.count {
            let uzay: CGColorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()
            gradyan = CGGradient(colorsSpace: uzay, colors: gradyanRenkleri as CFArray, locations: duraklar)
        }

        let bicim = UIGraphicsImageRendererFormat()
        bicim.scale = 1
        bicim.opaque = false
        let cizici = UIGraphicsImageRenderer(size: CGSize(width: kenar, height: kenar), format: bicim)
        return cizici.image { baglamSarmali in
            let baglam: CGContext = baglamSarmali.cgContext
            for katman in boya.canvasLayers {
                let sol: CGFloat = CGFloat(katman.left)
                let ust: CGFloat = CGFloat(katman.top)
                let genislik: CGFloat = CGFloat(katman.right) - sol
                let yukseklik: CGFloat = CGFloat(katman.bottom) - ust
                if genislik <= 0 || yukseklik <= 0 { continue }
                let alan = CGRect(x: sol, y: ust, width: genislik, height: yukseklik)
                let yaricap: CGFloat = min(CGFloat(katman.radius), min(genislik, yukseklik) / 2)
                let yol: UIBezierPath = UIBezierPath(roundedRect: alan, cornerRadius: yaricap)
                if katman.isGradient, let dolgu = gradyan {
                    baglam.saveGState()
                    yol.addClip()
                    baglam.drawLinearGradient(
                        dolgu,
                        start: CGPoint(x: alan.minX, y: alan.maxY),
                        end: CGPoint(x: alan.maxX, y: alan.minY),
                        options: [.drawsBeforeStartLocation, .drawsAfterEndLocation]
                    )
                    baglam.restoreGState()
                } else {
                    OzellestirmeStil.uiRenk(argb: katman.color).setFill()
                    yol.fill()
                }
            }
        }
    }
}
