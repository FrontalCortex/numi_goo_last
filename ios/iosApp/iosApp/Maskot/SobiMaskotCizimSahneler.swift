import SwiftUI

private typealias S = SobiMaskotSabit
private typealias K = SobiMaskotKanal
private typealias R = SobiMaskotRenk
private typealias V = SobiMaskotCizimVerisi
private typealias M = SobiMaskotMotoru
private typealias X = SobiMaskotMat

// Ders sonu ve "havalı ama tatlı" sahnelerin çizimleri: Android BunnyMascotView'daki
// drawSkateboard, drawBarbell, drawGuitar, drawSceneFront (hokkabaz, yıldızlar, notalar, toz,
// karate ve ninja sahneleri), drawChain, drawRapHatFree, drawBelt, drawHeadGear (bant, maske,
// göz bandı, şapkalar), drawSpyglass birebir.
extension SobiMaskotMotoru {

    /// 512'lik bir vektör çizimi ([ax], [ay]) noktası ([x], [y])'ye gelecek, [rot] dönük ve
    /// [s] ölçekli çizer; [sx] = -1 aynalar (Android drawAsset).
    private func drawAsset(_ c: SobiMaskotTuval, _ liste: [SobiMaskotSekil], _ alan: CGSize, _ alpha: CGFloat,
                           _ x: CGFloat, _ y: CGFloat, _ rot: CGFloat, _ s: CGFloat, _ ax: CGFloat, _ ay: CGFloat,
                           sx: CGFloat = 1) {
        if alpha <= 0.01 { return }
        c.kaydet()
        c.tasi(x, y)
        c.dondur(rot)
        c.olcekle(s * sx, s)
        c.tasi(-ax, -ay)
        c.cizim(liste, alani: alan, boyut: CGSize(width: 512, height: 512), min(alpha, 1))
        c.geriAl()
    }

    /// Sağ kedi kolunun elinin sahnedeki yeri.
    private func catHandRight() -> CGPoint {
        let up: CGFloat = v(K.catR) - (1 - v(K.catRA)) * 50
        let p: CGPoint = M.rotateAround(S.catHandX + S.catDx, S.catHandY, S.cheerShoulderRX, S.cheerShoulderRY, -up)
        return CGPoint(x: p.x + v(K.bodyDx), y: p.y + v(K.bodyDy) - v(K.lift))
    }

    /// Sol kedi kolunun eli.
    private func catHandLeft() -> CGPoint {
        let up: CGFloat = v(K.catL) - (1 - v(K.catLA)) * 50
        let p: CGPoint = M.rotateAround(S.catHandLX, S.catHandLY, S.shoulderLX, S.cheerShoulderLY, up)
        return CGPoint(x: p.x + v(K.bodyDx), y: p.y + v(K.bodyDy) - v(K.lift))
    }

    // MARK: Kaykay, halter, gitar

    func drawSkateboard(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.skateA)
        if a <= 0.01 { return }
        let cx: CGFloat = S.feetX + v(K.bodyDx)
        let top: CGFloat = S.feetY + 1 - v(K.lift)
        var rot: CGFloat = v(K.tilt)
        if hal == .SKATE {
            let hopT: CGFloat = e - S.skOllieS
            if hopT >= 0 && hopT <= 0.5 { rot -= 14 * sin(CGFloat.pi * hopT / 0.5) }
        }
        let spin: CGFloat = e * 900
        c.alfaIle(a) {
            c.dondur(rot, cx, top)
            var tahta = Path()
            tahta.move(to: CGPoint(x: cx - 38, y: top - 3))
            tahta.addQuadCurve(to: CGPoint(x: cx - 27, y: top + 2.5), control: CGPoint(x: cx - 34, y: top + 2.5))
            tahta.addLine(to: CGPoint(x: cx + 27, y: top + 2.5))
            tahta.addQuadCurve(to: CGPoint(x: cx + 38, y: top - 3), control: CGPoint(x: cx + 34, y: top + 2.5))
            c.ciz(tahta, R.skateDeck, 5)
            c.cizgi(cx - 26, top + 0.6, cx + 26, top + 0.6, R.skateGrip, 1.6)
            for dx in S.skateWheelDx {
                let wx: CGFloat = cx + dx
                let wy: CGFloat = top + 10.5
                c.doldur(Path(X.dortgen(wx - 4, top + 4.5, wx + 4, top + 7.5)), R.skateTruck)
                c.daireDoldur(wx, wy, 4.4, R.skateWheel)
                c.daireDoldur(wx, wy, 1.6, R.skateHub)
                let r: CGFloat = X.radyan(spin)
                c.cizgi(wx, wy, wx + 3.6 * cos(r), wy + 3.6 * sin(r), R.skateHub, 1.1)
            }
        }
    }

    func drawBarbell(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.barbellA)
        c.alfaIle(a) {
            let fa: CGFloat = v(K.frontA)
            let gap: CGFloat = v(K.frontGap)
            let y: CGFloat = S.frontRPawY + 3 + v(K.frontDy) + (1 - fa) * 25
            let xl: CGFloat = S.frontLPawX - gap - 30
            let xr: CGFloat = S.frontRPawX + gap + 30
            c.dondur(v(K.frontRot) * 0.5, 391, y)
            c.cizgi(xl - 14, y, xr + 14, y, R.steel, 4.5)
            drawWeightBead(c, xl, y, 20, R.abacusBeads[0])
            drawWeightBead(c, xl - 11, y, 15, R.abacusBeads[1])
            drawWeightBead(c, xr, y, 20, R.abacusBeads[0])
            drawWeightBead(c, xr + 11, y, 15, R.abacusBeads[1])
        }
    }

    private func drawWeightBead(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ h: CGFloat, _ color: UInt32) {
        var p = Path()
        p.move(to: CGPoint(x: cx - 6.5, y: cy - h * 0.35))
        p.addLine(to: CGPoint(x: cx - 2.5, y: cy - h))
        p.addLine(to: CGPoint(x: cx + 2.5, y: cy - h))
        p.addLine(to: CGPoint(x: cx + 6.5, y: cy - h * 0.35))
        p.addLine(to: CGPoint(x: cx + 6.5, y: cy + h * 0.35))
        p.addLine(to: CGPoint(x: cx + 2.5, y: cy + h))
        p.addLine(to: CGPoint(x: cx - 2.5, y: cy + h))
        p.addLine(to: CGPoint(x: cx - 6.5, y: cy + h * 0.35))
        p.closeSubpath()
        c.doldur(p, color)
        c.doldur(X.yuvarlak(X.dortgen(cx - 3.6, cy - h * 0.6, cx - 1.6, cy + h * 0.2), 1), R.beadShine)
    }

    func drawGuitar(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.guitarA)
        c.alfaIle(a) {
            c.tasi(S.gtX, S.gtY + (1 - a) * 25)
            c.dondur(S.gtAngle + v(K.frontRot) * 0.4)
            c.olcekle(S.gtScale, S.gtScale)
            c.tasi(-S.gtStrumX, -S.gtStrumY)
            c.cizim(V.gitar, alani: V.gitarAlani, boyut: CGSize(width: 512, height: 512), 1)
        }
    }

    // MARK: Öndeki sahne parçaları

    func drawSceneFront(_ c: SobiMaskotTuval, _ e: CGFloat) {
        guard let h = hal else { return }
        switch h {
        case .JUGGLE:
            drawJuggle(c, e)
        case .STARS:
            drawStarsScene(c, e)
        case .GUITAR:
            let a: CGFloat = v(K.guitarA)
            for k in 0..<3 {
                let fk: CGFloat = CGFloat(k)
                let ph: CGFloat = X.kalan(e / 2 + fk / 3, 1)
                let x: CGFloat = 330 - 40 * ph + 6 * sin(ph * S.tau * 1.5 + fk)
                let y: CGFloat = 236 - 130 * ph
                drawNote(c, x, y, a * sin(CGFloat.pi * ph))
            }
        case .SKATE:
            drawDust(c, S.feetX + v(K.bodyDx), S.feetY + 4, e - S.skOllieS - 0.5)
        case .KARATE:
            drawKarateScene(c, e)
        case .NINJA:
            drawNinjaScene(c, e)
        case .COWBOY_FRONT, .PIRATE:
            drawCoolExtras(c, e)
        case .RAPPER:
            drawRapHatFree(c, e)
        default:
            return
        }
    }

    /// Üç soroban boncuğu kaskad düzeninde.
    private func drawJuggle(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.juggleA)
        if a <= 0.01 { return }
        let sol: CGPoint = catHandLeft()
        let sag: CGPoint = catHandRight()
        let cycle: CGFloat = 3 * S.jgThrowS
        for i in 0..<3 {
            let t: CGFloat = e - CGFloat(i) * S.jgThrowS
            let k: CGFloat = floor(t / cycle)
            let local: CGFloat = t - k * cycle
            let throwNo: Int = Int(k) * 3 + i
            let fromLeft: Bool = ((throwNo % 2) + 2) % 2 == 0
            let from: CGPoint = fromLeft ? sol : sag
            let to: CGPoint = fromLeft ? sag : sol
            let fx: CGFloat = from.x
            let fy: CGFloat = from.y - 9
            let tx: CGFloat = to.x
            let ty: CGFloat = to.y - 9
            var x: CGFloat = tx
            var y: CGFloat = ty
            if local < S.jgFlightS {
                let p: CGFloat = local / S.jgFlightS
                let yay: CGFloat = S.jgHeight * 4 * p * (1 - p)
                x = fx + (tx - fx) * p
                y = fy + (ty - fy) * p - yay
            }
            c.kaydet()
            c.olcekle(1.8, 1.8, x, y)
            let renk: UInt32 = R.abacusBeads[(i * 2) % R.abacusBeads.count]
            c.doldur(beadPath(x, y), renk, min(a, 1))
            c.geriAl()
        }
    }

    /// Sandık yıldızları: üç yuva, kazanılanlar sırayla dolup parıldıyor.
    private func drawStarsScene(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.starsA)
        if a <= 0.01 { return }
        let n: Int = shownStars()
        let fade: CGFloat = a * (1 - M.seg(e, S.stFadeS, S.stFadeS + 0.4))
        for i in 0..<3 {
            let sx: CGFloat = S.stX[i]
            let sy: CGFloat = S.stY[i]
            drawBigStar(c, sx, sy, 15, fade * 0.35, hollow: true)
            if i >= n { continue }
            let since: CGFloat = e - (S.stFirstS + CGFloat(i) * S.stGapS)
            let s: CGFloat = M.popScale(since)
            if s <= 0 { continue }
            drawBigStar(c, sx, sy, 15 * s, fade, hollow: false)
            if since < 0.5 {
                let p: CGFloat = since / 0.5
                for k in 0..<4 {
                    let ang: CGFloat = CGFloat(k) * S.tau / 4 + 0.4
                    let r: CGFloat = 16 + 14 * p
                    drawStar(c, sx + r * cos(ang), sy + r * sin(ang), 3.5, (1 - p) * fade)
                }
            }
        }
    }

    private func drawBigStar(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ alpha: CGFloat, hollow: Bool) {
        if alpha <= 0.01 || r <= 0 { return }
        var p = Path()
        for k in 0..<10 {
            let ang: CGFloat = -CGFloat.pi / 2 + CGFloat(k) * CGFloat.pi / 5
            let rr: CGFloat = k % 2 == 0 ? r : r * 0.45
            let nokta = CGPoint(x: cx + rr * cos(ang), y: cy + rr * sin(ang))
            if k == 0 {
                p.move(to: nokta)
            } else {
                p.addLine(to: nokta)
            }
        }
        p.closeSubpath()
        let a: CGFloat = X.sinirla(alpha, 0, 1)
        if !hollow { c.doldur(p, R.crownGold, a) }
        c.ciz(p, hollow ? R.light : R.starEdge, 1.8, a)
    }

    private func drawNote(_ c: SobiMaskotTuval, _ x: CGFloat, _ y: CGFloat, _ alpha: CGFloat) {
        if alpha <= 0.01 { return }
        let a: CGFloat = X.sinirla(alpha, 0, 1)
        c.kaydet()
        c.dondur(-20, x, y)
        c.doldur(X.oval(x - 3.8, y - 2.8, x + 3.8, y + 2.8), R.light, a)
        c.geriAl()
        c.cizgi(x + 3.2, y - 1, x + 3.2, y - 14, R.light, 1.5, a)
        var bayrak = Path()
        bayrak.move(to: CGPoint(x: x + 3.2, y: y - 14))
        bayrak.addQuadCurve(to: CGPoint(x: x + 7.5, y: y - 6), control: CGPoint(x: x + 9, y: y - 11))
        c.ciz(bayrak, R.light, 1.5, a)
    }

    private func drawDust(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ t: CGFloat) {
        if t < 0 || t > 0.6 { return }
        let p: CGFloat = t / 0.6
        let a: CGFloat = (150 * (1 - p)).rounded() / 255
        for k in 0..<3 {
            let side: CGFloat = CGFloat(k) - 1
            c.daireDoldur(cx + side * (12 + 22 * p), cy - 4 * p, 4 + 5 * p, R.dust, a)
        }
    }

    // MARK: Rapçi

    /// Rapçinin kepi uçarken, eldeyken ve başına atılırken (sahne koordinatı).
    private func drawRapHatFree(_ c: SobiMaskotTuval, _ e: CGFloat) {
        if rapHatOnHead(e) { return }
        let headX: CGFloat = S.rpHatX + v(K.bodyDx)
        let headY: CGFloat = S.rpHatY + v(K.bodyDy) + v(K.headDy) - v(K.lift)
        let el: CGPoint = catHandRight()
        let hx: CGFloat = el.x
        let hy: CGFloat = el.y - 6
        var x: CGFloat = hx
        var y: CGFloat = hy
        var rot: CGFloat = S.rpHatRot + 300
        if e < S.rpCatchS {
            let p: CGFloat = M.seg(e, S.rpOffS, S.rpCatchS)
            let yay: CGFloat = 40 * 4 * p * (1 - p)
            x = headX + (hx - headX) * p
            y = headY + (hy - headY) * p - yay
            rot = S.rpHatRot + 300 * p
        } else if e >= S.rpTossS {
            let p: CGFloat = M.seg(e, S.rpTossS, S.rpOnS)
            let yay: CGFloat = 45 * 4 * p * (1 - p)
            x = hx + (headX - hx) * p
            y = hy + (headY - hy) * p - yay
            rot = S.rpHatRot + 300 + 420 * p
        }
        drawAsset(c, V.rapKepi, V.rapKepiAlani, v(K.rapA), x, y, rot, S.rpHatScale, S.rpHatAX, S.rpHatAY, sx: -1)
    }

    /// Altın zincir kolye (gövde çerçevesi).
    func drawChain(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.rapA)
        c.alfaIle(a) {
            let n: Int = S.chainLinks
            for i in 0...n {
                let t: CGFloat = CGFloat(i) / CGFloat(n)
                let u: CGFloat = 1 - t
                // İkinci derece Bézier eğrisi ve türevi
                let uu: CGFloat = u * u
                let tu2: CGFloat = 2 * t * u
                let tt: CGFloat = t * t
                let x: CGFloat = uu * S.chainLX + tu2 * S.chainMidX + tt * S.chainRX
                let y: CGFloat = uu * S.chainTopY + tu2 * S.chainMidY + tt * S.chainTopY
                let dx1: CGFloat = 2 * u * (S.chainMidX - S.chainLX)
                let dx2: CGFloat = 2 * t * (S.chainRX - S.chainMidX)
                let dy1: CGFloat = 2 * u * (S.chainMidY - S.chainTopY)
                let dy2: CGFloat = 2 * t * (S.chainTopY - S.chainMidY)
                let dx: CGFloat = dx1 + dx2
                let dy: CGFloat = dy1 + dy2
                let ang: CGFloat = atan2(dy, dx) * 180 / CGFloat.pi
                let rx: CGFloat = i % 2 == 0 ? 3.2 : 2.7
                let ry: CGFloat = i % 2 == 0 ? 2 : 1.4
                c.kaydet()
                c.dondur(ang, x, y)
                c.ciz(X.oval(x - rx, y - ry, x + rx, y + ry), R.buckle, 1.7)
                c.geriAl()
            }
            c.daireDoldur(S.chainMidX, S.chainPendantY, 6.5, R.buckle)
            c.daireDoldur(S.chainMidX, S.chainPendantY, 4.2, R.starEdge)
            let r: CGRect = X.dortgen(S.chainMidX - 3, S.chainPendantY - 3, S.chainMidX + 3, S.chainPendantY + 3)
            c.ciz(X.yay(r, 200, 70, merkezli: false), R.chainShine, 1)
        }
    }

    // MARK: Kovboy kemeri ve kafaya takılanlar

    /// Kovboy kemeri (gövdeye kırpılmış), tokası ve sağ kalçada havuç kılıfı.
    func drawBelt(_ c: SobiMaskotTuval) {
        if hal != .COWBOY_FRONT { return }
        let a: CGFloat = v(K.hatA)
        c.alfaIle(a) {
            c.kaydet()
            c.kirp(V.govde[0].yol)
            c.doldur(Path(X.dortgen(350, S.beltTop, 440, S.beltTop + 7)), R.belt)
            c.geriAl()
            c.doldur(X.yuvarlak(X.dortgen(388, S.beltTop - 1.5, 398, S.beltTop + 8.5), 2), R.buckle)
            c.doldur(Path(X.dortgen(391, S.beltTop + 1.5, 395, S.beltTop + 5.5)), R.belt)
            drawCarrotAt(c, S.holsterX, S.holsterY, S.holsterScale, 90, v(K.holsterA), 0)
            c.kaydet()
            c.dondur(-12, S.holsterX, S.holsterY + 4)
            let hx: CGFloat = S.holsterX
            let hy: CGFloat = S.holsterY
            c.doldur(X.yuvarlak(X.dortgen(hx - 7, hy - 4, hx + 8, hy + 15), 4), R.holster)
            c.doldur(X.yuvarlak(X.dortgen(hx - 7, hy - 4, hx + 8, hy), 2), R.holsterDark)
            c.geriAl()
        }
    }

    /// Kafaya takılanlar (kafa çerçevesinde): maske, bant, şapkalar.
    func drawHeadGear(_ c: SobiMaskotTuval, _ e: CGFloat, _ t: CGFloat) {
        let mask: CGFloat = v(K.maskA)
        if mask > 0.01 { drawNinjaMask(c, mask, t) }
        let band: CGFloat = v(K.headbandA)
        if band > 0.01 { drawKarateBand(c, band, t) }
        let hat: CGFloat = v(K.hatA)
        if hat > 0.01 {
            let drop: CGFloat = -(1 - hat) * 20
            if hal == .COWBOY_FRONT {
                drawAsset(c, V.bandana, V.bandanaAlani, hat, S.bandanaX, S.bandanaY, 0, S.bandanaScale, S.bandanaAX, S.bandanaAY)
                let y: CGFloat = S.cbFrontY + v(K.hatDy) + drop
                drawAsset(c, V.kovboySapkasi, V.kovboySapkasiAlani, hat, S.cbFrontX, y, 0, S.cbFrontScale, S.cbFrontAX, S.cbFrontAY)
            } else if hal == .PIRATE {
                drawEyePatch(c, hat)
                drawAsset(c, V.korsanSapkasi, V.korsanSapkasiAlani, hat, S.prHatX, S.prHatY + drop, 0, S.prHatScale, S.prHatAX, S.prHatAY)
            }
        }
        let jester: CGFloat = v(K.jesterA)
        if jester > 0.01 {
            let y: CGFloat = S.jesterY - (1 - jester) * 20
            drawAsset(c, V.soytariSapkasi, V.soytariSapkasiAlani, jester, S.jesterX, y, S.jesterRot, S.jesterScale, S.jesterAX, S.jesterAY)
        }
        let rap: CGFloat = v(K.rapA)
        if rap > 0.01 && hal == .RAPPER && rapHatOnHead(e) {
            let y: CGFloat = S.rpHatY - (1 - rap) * 20
            drawAsset(c, V.rapKepi, V.rapKepiAlani, rap, S.rpHatX, y, S.rpHatRot, S.rpHatScale, S.rpHatAX, S.rpHatAY, sx: -1)
        }
    }

    /// Bandın arkadaki düğümü ve rüzgârda dalgalanan iki ucu.
    private func drawKnot(_ c: SobiMaskotTuval, _ x: CGFloat, _ y: CGFloat, _ t: CGFloat, _ color: UInt32, _ dark: UInt32) {
        let w: CGFloat = 3 * sin(t * S.tau * 1.8)
        var uc1 = Path()
        uc1.move(to: CGPoint(x: x, y: y - 3))
        uc1.addQuadCurve(to: CGPoint(x: x + 30, y: y - 2 + w), control: CGPoint(x: x + 10, y: y - 10 + w))
        uc1.addQuadCurve(to: CGPoint(x: x, y: y + 3), control: CGPoint(x: x + 16, y: y + 1))
        uc1.closeSubpath()
        c.doldur(uc1, dark)
        var uc2 = Path()
        uc2.move(to: CGPoint(x: x, y: y + 1))
        uc2.addQuadCurve(to: CGPoint(x: x + 26, y: y + 24 - w), control: CGPoint(x: x + 12, y: y + 6 - w))
        uc2.addQuadCurve(to: CGPoint(x: x, y: y + 5), control: CGPoint(x: x + 12, y: y + 16))
        uc2.closeSubpath()
        c.doldur(uc2, color)
        c.daireDoldur(x, y + 1, 5.5, dark)
    }

    private func drawKarateBand(_ c: SobiMaskotTuval, _ a: CGFloat, _ t: CGFloat) {
        c.alfaIle(a) {
            c.kaydet()
            c.kirp(V.kafa[0].yol)
            var serit = Path()
            serit.move(to: CGPoint(x: 318, y: 136))
            serit.addQuadCurve(to: CGPoint(x: 462, y: 136), control: CGPoint(x: 388, y: 108))
            c.ciz(serit, R.bandRed, 11)
            var isik = Path()
            isik.move(to: CGPoint(x: 318, y: 131))
            isik.addQuadCurve(to: CGPoint(x: 462, y: 131), control: CGPoint(x: 388, y: 103))
            c.ciz(isik, R.bandLight, 2)
            c.geriAl()
            drawKnot(c, 446, 128, t, R.bandRed, R.bandDark)
        }
    }

    private func drawNinjaMask(_ c: SobiMaskotTuval, _ a: CGFloat, _ t: CGFloat) {
        c.alfaIle(a) {
            c.kaydet()
            c.kirp(V.kafa[0].yol)
            var ust = Path()
            ust.move(to: CGPoint(x: 300, y: 100))
            ust.addLine(to: CGPoint(x: 480, y: 100))
            ust.addLine(to: CGPoint(x: 480, y: S.njSlitTop))
            ust.addQuadCurve(to: CGPoint(x: 300, y: S.njSlitTop), control: CGPoint(x: 388, y: S.njSlitTop - 10))
            ust.closeSubpath()
            c.doldur(ust, R.ninjaCloth)
            var alt = Path()
            alt.move(to: CGPoint(x: 300, y: S.njSlitBottom))
            alt.addQuadCurve(to: CGPoint(x: 480, y: S.njSlitBottom), control: CGPoint(x: 388, y: S.njSlitBottom - 7))
            alt.addLine(to: CGPoint(x: 480, y: 230))
            alt.addLine(to: CGPoint(x: 300, y: 230))
            alt.closeSubpath()
            c.doldur(alt, R.ninjaCloth)
            var bant = Path()
            bant.move(to: CGPoint(x: 300, y: 124))
            bant.addQuadCurve(to: CGPoint(x: 480, y: 124), control: CGPoint(x: 388, y: 112))
            c.ciz(bant, R.bandRed, 6)
            var kivrim = Path()
            kivrim.move(to: CGPoint(x: 350, y: 175))
            kivrim.addQuadCurve(to: CGPoint(x: 426, y: 175), control: CGPoint(x: 388, y: 185))
            kivrim.move(to: CGPoint(x: 360, y: 188))
            kivrim.addQuadCurve(to: CGPoint(x: 416, y: 188), control: CGPoint(x: 388, y: 196))
            c.ciz(kivrim, R.ninjaFold, 2)
            c.geriAl()
            drawKnot(c, 447, 124, t, R.bandRed, R.bandDark)
        }
        if v(K.blush) > 0.01 { drawBlush(c) }
    }

    private func drawEyePatch(_ c: SobiMaskotTuval, _ a: CGFloat) {
        c.alfaIle(a) {
            c.kaydet()
            c.kirp(V.kafa[0].yol)
            c.cizgi(330, 118, 452, 152, R.patch, 2.4)
            c.geriAl()
            let ex: CGFloat = S.eyeX[1]
            let ey: CGFloat = S.eyeY
            c.doldur(X.oval(ex - 10, ey - 9, ex + 10, ey + 9), R.patch)
            c.doldur(X.oval(ex - 5.5, ey - 5.5, ex - 0.5, ey - 2), R.patchShine)
        }
    }

    /// Dürbün (gövde çerçevesinde): bir ucu sağ elde, öbür ucu sol gözde.
    func drawSpyglass(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.spyglassA)
        if a <= 0.01 { return }
        let ta: CGFloat = v(K.thinkA)
        let armDeg: CGFloat = -(v(K.think) - (1 - ta) * 60)
        let el: CGPoint = M.rotateAround(S.catHandX + S.catDx, S.catHandY, S.cheerShoulderRX, S.cheerShoulderRY, armDeg)
        var bx: CGFloat = el.x
        var by: CGFloat = el.y
        let goz: CGPoint = M.rotateAround(S.eyeX[0], S.eyeY, S.neckX, S.neckY, sonKafaDonusu)
        let eyeX: CGFloat = goz.x
        let eyeY: CGFloat = goz.y + v(K.headDy)
        let yapisma: CGFloat = X.sinirla((v(K.think) - S.prLower) / (S.thinkUp - S.prLower), 0, 1)
        let attach: CGFloat = yapisma * min(ta, 1)
        let freeRad: CGFloat = X.radyan(S.spyFreeDeg)
        let fx: CGFloat = bx + S.spyLen * cos(freeRad)
        let fy: CGFloat = by + S.spyLen * sin(freeRad)
        var ax: CGFloat = fx + (eyeX - fx) * attach
        var ay: CGFloat = fy + (eyeY - fy) * attach
        let flip: CGFloat = durbunCevirme
        if flip > 0 && flip < 1 {
            let mx: CGFloat = (ax + bx) / 2
            let my: CGFloat = (ay + by) / 2
            let yeniA: CGPoint = M.rotateAround(ax, ay, mx, my, 180 * flip)
            let yeniB: CGPoint = M.rotateAround(bx, by, mx, my, 180 * flip)
            ax = yeniA.x
            ay = yeniA.y
            bx = yeniB.x
            by = yeniB.y
        }
        let bigAtEye: Bool = flip >= 0.5
        let rA: CGFloat = bigAtEye ? S.spyBigR : S.spySmallR
        let rB: CGFloat = bigAtEye ? S.spySmallR : S.spyBigR
        let lensKirpma: CGFloat = mercekKirpma
        c.alfaIle(a) {
            let dx: CGFloat = bx - ax
            let dy: CGFloat = by - ay
            let len: CGFloat = max(sqrt(dx * dx + dy * dy), 0.01)
            let nx: CGFloat = -dy / len
            let ny: CGFloat = dx / len
            var boru = Path()
            boru.move(to: CGPoint(x: ax + nx * rA * 0.8, y: ay + ny * rA * 0.8))
            boru.addLine(to: CGPoint(x: bx + nx * rB * 0.8, y: by + ny * rB * 0.8))
            boru.addLine(to: CGPoint(x: bx - nx * rB * 0.8, y: by - ny * rB * 0.8))
            boru.addLine(to: CGPoint(x: ax - nx * rA * 0.8, y: ay - ny * rA * 0.8))
            boru.closeSubpath()
            c.doldur(boru, R.brass)
            for f in S.spyRingAt {
                let r: CGFloat = (rA + (rB - rA) * f) * 0.8
                let cx: CGFloat = ax + dx * f
                let cy: CGFloat = ay + dy * f
                c.cizgi(cx + nx * r, cy + ny * r, cx - nx * r, cy - ny * r, R.brassDark, 2)
            }
            c.daireDoldur(bx, by, rB * 0.8 + 1.5, R.brassDark)
            c.daireDoldur(ax, ay, rA + 2.5, R.brassDark)
            c.daireDoldur(ax, ay, rA, R.glass)
            if attach > 0.9 && (flip <= 0 || flip >= 1) {
                // Mercekte büyümüş (ya da küçülmüş) göz.
                c.kaydet()
                c.kirp(X.daire(ax, ay, rA))
                c.tasi(ax - S.eyeX[0], ay - S.eyeY)
                let k: CGFloat = bigAtEye ? S.spyEyeBig : S.spyEyeSmall
                c.olcekle(k, k * lensKirpma, S.eyeX[0], S.eyeY)
                c.sekil(V.gozler[0])
                c.geriAl()
            }
            let r: CGRect = X.dortgen(ax - rA * 0.7, ay - rA * 0.7, ax + rA * 0.7, ay + rA * 0.7)
            c.ciz(X.yay(r, 200, 60, merkezli: false), R.white, 1.5, 170.0 / 255.0)
        }
    }

    // MARK: Karate ve ninja sahneleri

    private func drawKarateScene(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let alpha: CGFloat = v(K.kboardA) * M.seg(e, 0, 0.3)
        c.alfaIle(alpha) {
            for bx in S.kbBrickX {
                for k in 0..<4 {
                    let top: CGFloat = S.kbBrickTop + CGFloat(k) * 14
                    let tugla: Path = X.yuvarlak(X.dortgen(bx - 7, top, bx + 7, top + 13), 1.5)
                    c.doldur(tugla, R.block)
                    c.ciz(tugla, R.blockEdge, 1)
                }
            }
            if e < S.krBreakS {
                let since: CGFloat = e - S.krChopS
                let wob: CGFloat = (since > 0 && since < 0.6) ? 2.5 * exp(-since * 7) * sin(since * S.tau * 8) : 0
                drawPlank(c, S.kbLeft, S.kbRight, S.kbTop + wob)
            } else {
                let p: CGFloat = M.seg(e, S.krBreakS, S.krBreakS + 0.3)
                let ang: CGFloat = 32 * p * p
                c.kaydet()
                c.dondur(ang, S.kbLeft + 4, S.kbTop)
                drawPlank(c, S.kbLeft, S.kbMid - 1, S.kbTop)
                c.geriAl()
                c.kaydet()
                c.dondur(-ang, S.kbRight - 4, S.kbTop)
                drawPlank(c, S.kbMid + 1, S.kbRight, S.kbTop)
                c.geriAl()
                let s: CGFloat = e - S.krBreakS
                if s < 0.5 {
                    for k in 0..<4 {
                        let ang2: CGFloat = CGFloat(k) * S.tau / 4 + 0.6
                        let r: CGFloat = 8 + 18 * (s / 0.5)
                        drawStar(c, S.kbMid + r * cos(ang2), S.kbTop + r * sin(ang2), 4, 1 - s / 0.5)
                    }
                }
            }
        }
        let ts: CGFloat = e - S.krChopS + 0.1
        if ts > 0 && ts < 0.9 {
            c.yazi(Metin.al("ui_hiyaa"), 300, 72, 22 * M.popScale(ts), R.crownGold, 1 - M.seg(ts, 0.6, 0.9))
        }
        if e >= S.krBlowS && e < S.krCalmS {
            for k in 0..<3 {
                let ph: CGFloat = X.kalan(e * 2.5 + CGFloat(k) / 3, 1)
                let x: CGFloat = 396 + 16 * ph
                let y: CGFloat = 168 - 6 + CGFloat(k) * 5
                let a: CGFloat = (255 * (1 - ph)).rounded() / 255
                c.ciz(X.yay(X.dortgen(x - 3, y - 3, x + 3, y + 3), -60, 120, merkezli: false), R.light, 1.6, a)
            }
        }
        let sp: CGFloat = e - S.krBreakS - 0.7
        if sp > 0 && sp < 0.6 { drawStar(c, 433, 118, 7 * M.popScale(sp), 1 - sp / 0.6) }
    }

    private func drawPlank(_ c: SobiMaskotTuval, _ x0: CGFloat, _ x1: CGFloat, _ top: CGFloat) {
        let tahta: Path = Path(X.dortgen(x0, top, x1, top + 7))
        c.doldur(tahta, R.kbWood)
        c.ciz(tahta, R.kbEdge, 1)
        c.cizgi(x0 + 4, top + 3.5, x1 - 6, top + 3.5, R.kbEdge, 1)
    }

    /// Duman bombası (elden yere) ve tavşanı tamamen örten, sonra dağılan duman.
    private func drawNinjaScene(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let tb: CGFloat = e - S.njThrowS - 0.12
        if tb > 0 && tb < 0.23 {
            let p: CGFloat = tb / 0.23
            let x: CGFloat = 455 - 40 * p
            let y: CGFloat = 225 + 75 * p * p
            c.daireDoldur(x, y, 5, R.patch)
            drawStar(c, x + 3, y - 7, 3, 1)
        }
        let s: CGFloat = e - S.njSmokeS
        if s <= 0 || e >= S.njPeekS { return }
        let grow: CGFloat = M.smooth01(s / 0.35)
        let fade: CGFloat = 1 - M.seg(e, S.njPeekS - 0.7, S.njPeekS - 0.1)
        let bodyDx: CGFloat = v(K.bodyDx)
        // Bulutlar tek katmanda opak çiziliyor, katman bütün olarak soluyor.
        c.alfaIle(fade * 0.95) {
            for shade in 0...1 {
                let renk: UInt32 = shade == 0 ? R.smokeShade : R.ninjaSmoke
                for k in 0..<S.smokeX.count {
                    let kat: CGFloat = shade == 0 ? 1 : 0.9
                    let r: CGFloat = S.smokeR[k] * grow * (1 + 0.06 * sin(e * 3 + CGFloat(k))) * kat
                    let drift: CGFloat = -10 * s
                    let ek: CGFloat = shade == 0 ? 3 : 0
                    c.daireDoldur(S.smokeX[k] + bodyDx, S.smokeY[k] + drift + ek, r, renk)
                }
            }
        }
    }

    /// Kovboyda havuç kırıntıları, korsanda "?".
    private func drawCoolExtras(_ c: SobiMaskotTuval, _ e: CGFloat) {
        if hal == .COWBOY_FRONT {
            for b in S.cbBites {
                let s: CGFloat = e - b
                if s < 0 || s > 0.6 { continue }
                let a: CGFloat = (255 * (1 - s / 0.6)).rounded() / 255
                for k in 0..<4 {
                    let dx: CGFloat = (CGFloat(k) - 1.5) * 9 * s
                    let y: CGFloat = 176 + 70 * s * s + CGFloat(k) * 1.5
                    c.daireDoldur(386 + dx + v(K.bodyDx), y, 1.8, R.crumb, a)
                }
            }
        } else if hal == .PIRATE {
            let q: CGFloat = e - S.prPuzzleS - 0.15
            if q > 0 && q < S.prLook2S - S.prPuzzleS - 0.2 {
                c.yazi("?", 450, 104, 26 * M.popScale(q), R.light, 1)
            }
        }
    }
}
