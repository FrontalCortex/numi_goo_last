import SwiftUI

private typealias S = SobiMaskotSabit
private typealias K = SobiMaskotKanal
private typealias R = SobiMaskotRenk
private typealias V = SobiMaskotCizimVerisi
private typealias M = SobiMaskotMotoru

// Tavşanın kendisi: Android BunnyMascotView.drawBunny, drawBackArms, drawLegs, drawEars,
// drawBlush, drawEyes, drawMouth, drawFrontHands, drawThinkArm. Katman sırası Android'le aynı.
extension SobiMaskotMotoru {

    /// Bütün sahne (kaynak çizimin koordinatlarında; ölçekleme görünümde).
    func sahneyiCiz(_ c: SobiMaskotTuval) {
        let e: CGFloat = eSn
        let t: CGFloat = tSn
        let lift: CGFloat = v(K.lift)
        let bodyDx: CGFloat = v(K.bodyDx)

        // Gölge: tavşan yükseldikçe küçülüyor.
        c.kaydet()
        c.tasi(bodyDx, 0)
        let golgeOlcegi: CGFloat = 1 - lift / (S.jumpHeight * 2.5)
        c.olcekle(golgeOlcegi, golgeOlcegi, S.shadowX, S.shadowY)
        c.sekiller(V.golge)
        c.geriAl()

        // Foto flaşı: tavşan ve eşyaları bir katmanda; flaş anında katman yalnızca çizili
        // yerlerinde beyaza çekiliyor (gölge ve öndeki sahne etkilenmiyor).
        let flas: CGFloat = photoFlashTint(e)
        if flas > 0.01 {
            let soldan: Bool = lastFlashFromLeft(e)
            let guc: Double = Double(S.flashTintMax * flas)
            c.atopKatmani({
                self.tavsanVeEsyalar(c, e, t)
            }, boya: { ctx in
                let beyaz: Color = SobiMaskotMat.renk(R.white)
                let uzak: Color = SobiMaskotMat.renk(S.flashFarColor)
                let yakin = CGPoint(x: S.flashNearX, y: 0)
                let uzakNokta = CGPoint(x: S.flashFarX, y: 0)
                let gecis = Gradient(colors: soldan ? [beyaz, uzak] : [uzak, beyaz])
                let bas: CGPoint = soldan ? yakin : uzakNokta
                let son: CGPoint = soldan ? uzakNokta : yakin
                ctx.opacity = guc
                let alan: CGRect = SobiMaskotMat.dortgen(230, -10, 560, 330)
                ctx.fill(Path(alan), with: .linearGradient(gecis, startPoint: bas, endPoint: son))
            })
        } else {
            tavsanVeEsyalar(c, e, t)
        }
        // Sahnenin tavşandan bağımsız, önde duran parçaları.
        drawSceneFront(c, e)
    }

    private func tavsanVeEsyalar(_ c: SobiMaskotTuval, _ e: CGFloat, _ t: CGFloat) {
        // Reklam hâllerinin sahnesi yerde duruyor; tavşandan ÖNCE (pati ve ayaklar önde kalsın).
        drawAdProps(c, e)
        drawBoard(c, e)
        drawSkateboard(c, e)

        let lift: CGFloat = v(K.lift)
        let bodyDx: CGFloat = v(K.bodyDx)
        let bob: CGFloat = sin(t * S.tau / 2.4) * 2.2
        let upperRot: CGFloat = v(K.upperRot)
        let headRot: CGFloat = sin(t * S.tau / 5) * 2.5 + v(K.headRot)
        sonKafaDonusu = headRot
        let squash: CGFloat = v(K.squash)

        c.kaydet()
        c.tasi(bodyDx, -lift)
        c.dondur(v(K.tilt), S.feetX, S.feetY)
        c.olcekle(1 + 0.07 * squash, 1 - 0.10 * squash, S.feetX, S.feetY)

        // Arkadaki kollar gövdeyle birlikte iner-kalkar ve sallanır.
        c.kaydet()
        c.tasi(0, bob + v(K.bodyDy))
        c.dondur(upperRot, S.hipX, S.hipY)
        drawBackArms(c, t)
        c.geriAl()

        // Bacaklar yerinde, gövde ve kafa iner-kalkar.
        drawLegs(c)
        c.tasi(0, bob + v(K.bodyDy))
        c.dondur(upperRot, S.hipX, S.hipY)
        c.sekiller(V.govde)
        drawProBadge(c)
        drawBelt(c)
        if hal == .RAPPER { drawChain(c) }

        kafayiCiz(c, e, t, headRot)

        // Öndekiler: kafanın altına binen eller kafadan SONRA.
        drawAbacus(c, e)
        drawCalendar(c, e)
        drawBarbell(c)
        drawGuitar(c)
        if v(K.noteA) > 0.01 {
            drawFrontHands(c, left: false, right: true)
            drawCarrot(c)
            drawNotebook(c)
            drawFrontHands(c, left: true, right: false)
        } else {
            drawFrontHands(c, left: true, right: true)
        }
        drawAlarmClock(c, e)
        drawPhone(c)
        drawThinkArm(c)
        drawTorchFlame(c, t)
        drawExtras(c, e)

        c.geriAl()
    }

    private func kafayiCiz(_ c: SobiMaskotTuval, _ e: CGFloat, _ t: CGFloat, _ headRot: CGFloat) {
        c.kaydet()
        c.tasi(0, v(K.headDy))
        c.dondur(headRot, S.neckX, S.neckY)
        // Öne eğilme: kafa boyna doğru basılıyor, yüz aşağı kayıyor, kulaklar kısalıyor.
        let pitch: CGFloat = v(K.headPitch)
        if pitch != 0 {
            c.olcekle(1 + S.pitchWiden * pitch, 1 - S.pitchSquash * pitch, S.neckX, S.neckY)
        }
        drawEars(c, pitch)
        let kafa: [SobiMaskotSekil] = V.kafa
        c.sekil(kafa[0])
        c.sekil(kafa[1])
        c.kaydet()
        c.tasi(0, S.pitchFaceDy * pitch)
        for i in 2..<kafa.count {
            c.sekil(kafa[i])
        }
        drawBlush(c)
        drawEyes(c)
        drawMouth(c, t)
        c.sekil(V.kafaUstu[0])   // burun parıltısı yüzle birlikte
        c.geriAl()
        for i in 1..<V.kafaUstu.count {
            c.sekil(V.kafaUstu[i])
        }
        drawCrown(c, e)
        drawGlasses(c, e)
        drawHeadGear(c, e, t)
        c.geriAl()
    }

    // MARK: Kollar

    private func drawBackArms(_ c: SobiMaskotTuval, _ t: CGFloat) {
        let sway: CGFloat = sin(t * S.tau / 2.4) * 1.5

        // Sırttakiler kollardan da geride: pelerin ve jet çantası.
        drawCape(c, t)
        drawJetpack(c, t)

        // Sarkan kollar; sağdaki solun aynası.
        let hr: CGFloat = v(K.hangRA)
        c.alfaIle(hr) {
            c.tasi(0, S.mirrorDy)
            let aci: CGFloat = (1 - hr) * 50 + v(K.hangR) + sway
            c.dondur(-aci, S.shoulderRX, S.shoulderY)
            c.tasi(S.mirrorX2, 0)
            c.olcekle(-1, 1)
            c.sekiller(V.kol)
        }
        let hl: CGFloat = v(K.hangLA)
        c.alfaIle(hl) {
            let aci: CGFloat = (1 - hl) * 50 + v(K.hangL) + sway
            c.dondur(aci, S.shoulderLX, S.shoulderY)
            c.sekiller(V.kol)
        }

        // Zafer işaretli kol
        let pa: CGFloat = v(K.peaceA)
        c.alfaIle(pa) {
            c.dondur(v(K.peace) + (1 - pa) * 40, S.peaceShoulderX, S.peaceShoulderY)
            c.sekiller(V.zaferKolu)
        }

        // Kedi kolları
        let cl: CGFloat = v(K.catLA)
        c.alfaIle(cl) {
            c.dondur(v(K.catL) - (1 - cl) * 50, S.shoulderLX, S.cheerShoulderLY)
            c.tasi(S.catDx, 0)
            c.sekiller(V.kediKoluSol)
        }
        let cr: CGFloat = v(K.catRA)
        let rightUp: CGFloat = v(K.catR) - (1 - cr) * 50
        // Balon sağ elde: kol dönüşü geri alınıp dik tutuluyor; ip elin altında kalsın diye koldan önce.
        let ba: CGFloat = v(K.balloonA)
        c.alfaIle(ba) {
            c.dondur(-rightUp, S.cheerShoulderRX, S.cheerShoulderRY)
            c.tasi(S.catDx, 0)
            c.tasi(S.catHandX, S.catHandY)
            c.dondur(rightUp + v(K.balloonSway))
            let grow: CGFloat = 0.3 + 0.7 * ba
            c.olcekle(grow, grow)
            c.tasi(-S.balloonStringX, -S.balloonStringY)
            c.sekiller(V.balon)
        }
        c.alfaIle(cr) {
            c.dondur(-rightUp, S.cheerShoulderRX, S.cheerShoulderRY)
            c.tasi(S.catDx, 0)
            c.sekiller(V.kediKoluSag)
            elTebesiriVeDegnek(c, t)
        }
        drawWandTrail(c)
    }

    /// Sağ kedi kolunun elindekiler (kol çerçevesinde): kara tahtada tebeşir, sihirli değnek.
    private func elTebesiriVeDegnek(_ c: SobiMaskotTuval, _ t: CGFloat) {
        let chalk: CGFloat = v(K.boardA)
        if chalk > 0.01 {
            c.kaydet()
            c.dondur(30, S.catHandX, S.catHandY)
            let r: CGRect = SobiMaskotMat.dortgen(S.catHandX - 1.5, S.catHandY - 7, S.catHandX + 1.5, S.catHandY + 2)
            c.doldur(SobiMaskotMat.yuvarlak(r, 1.2), R.white, min(chalk, 1))
            c.geriAl()
        }
        let wand: CGFloat = v(K.wandA)
        if wand > 0.01 {
            let rad: CGFloat = SobiMaskotMat.radyan(S.wandDirDeg)
            let tx: CGFloat = S.catHandX + cos(rad) * S.wandLen
            let ty: CGFloat = S.catHandY + sin(rad) * S.wandLen
            let wx: CGFloat = S.catHandX + cos(rad) * (S.wandLen - 5)
            let wy: CGFloat = S.catHandY + sin(rad) * (S.wandLen - 5)
            let a: CGFloat = min(wand, 1)
            c.cizgi(S.catHandX, S.catHandY, wx, wy, R.wandStick, 3.2, a)
            c.cizgi(wx, wy, tx, ty, R.white, 3.2, a)
            c.kaydet()
            c.dondur(t * 180, tx, ty)
            drawStar(c, tx, ty, 6.5, a)
            c.geriAl()
        }
    }

    /// "Abra kadabra" sırasında değneğin ucunun arkasında sönen yıldız izi.
    private func drawWandTrail(_ c: SobiMaskotTuval) {
        guard hal == .AD_ZAP || hal == .AD_MAGIC else { return }
        let e: CGFloat = eSn
        if e < S.wandUpS + 0.15 || e > S.wandWaveEndS { return }
        for k in 1...6 {
            let past: CGFloat = e - CGFloat(k) * 0.05
            if past < S.wandUpS { break }
            let tip: CGPoint = M.wandTip(S.wandArmUp + M.wandWave(past))
            let fade: CGFloat = 1 - CGFloat(k) / 7
            drawStar(c, tip.x, tip.y, 3.5 * fade, fade * 0.9)
        }
    }

    // MARK: Bacaklar, kulaklar

    private func drawLegs(_ c: SobiMaskotTuval) {
        let open: CGFloat = v(K.legsOpen)
        c.kaydet()
        c.tasi(-S.stepDx * open, 0)
        c.dondur(S.stepDeg * open + v(K.kickL), S.legLHipX, S.legHipY)
        c.sekiller(V.solBacak)
        c.geriAl()
        c.kaydet()
        c.tasi(S.stepDx * open, 0)
        c.dondur(-S.stepDeg * open - v(K.kickR), S.legRHipX, S.legHipY)
        c.sekiller(V.sagBacak)
        c.geriAl()
    }

    private func drawEars(_ c: SobiMaskotTuval, _ pitch: CGFloat) {
        let flick: CGFloat = kulakSeyirmesi
        let shorten: CGFloat = 1 - S.pitchEar * pitch
        let solEk: CGFloat = kulakSeyirmeYonu < 0 ? flick : 0
        let sagEk: CGFloat = kulakSeyirmeYonu > 0 ? flick : 0
        c.kaydet()
        c.dondur(-(v(K.earL) + solEk), S.earLX, S.earY)
        c.olcekle(1, shorten, S.earLX, S.earY)
        c.sekiller(V.solKulak)
        c.geriAl()
        c.kaydet()
        c.dondur(v(K.earR) + sagEk, S.earRX, S.earY)
        c.olcekle(1, shorten, S.earRX, S.earY)
        c.sekiller(V.sagKulak)
        c.geriAl()
    }

    // MARK: Yüz

    func drawBlush(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.blush)
        if a <= 0.01 { return }
        for cx in S.blushX {
            c.doldur(SobiMaskotMat.oval(cx - 8, 158, cx + 8, 168), R.blush, min(a, 1))
        }
    }

    private func drawEyes(_ c: SobiMaskotTuval) {
        let open: CGFloat = gozAcikligi
        let scale: CGFloat = 1 + v(K.eyeScale)
        let kisik: CGFloat = SobiMaskotMat.sinirla(v(K.squint), 0, 1)
        for i in 0...1 {
            let cx: CGFloat = S.eyeX[i]
            var mode: SobiMaskotGoz = goz
            if goz == .kirpma {
                mode = i == 0 ? .mutlu : .normal
            }
            switch mode {
            case .mutlu:
                eyeArc(c, cx, 180)
            case .kapali:
                eyeArc(c, cx, 0)
            default:
                // Göz kırpmada açık kalan göz kırpmıyor.
                let o: CGFloat = goz == .kirpma ? 1 : open
                c.kaydet()
                c.tasi(v(K.gazeX), v(K.gazeY))
                c.olcekle(scale, scale * o * (1 - 0.55 * kisik), cx, S.eyeY)
                c.sekil(V.gozler[i])
                c.geriAl()
            }
        }
        let brows: CGFloat = v(K.brows)
        if brows > 0.01 {
            // Üzgün kaşlar: iç uçları kalkık
            c.cizgi(357, 133, 369, 128, R.eye, 2.4, min(brows, 1))
            c.cizgi(402, 128, 414, 133, R.eye, 2.4, min(brows, 1))
        }
        let determined: CGFloat = v(K.browsDet)
        if determined > 0.01 {
            // Kararlı kaşlar: iç uçları aşağıda
            c.cizgi(356, 127, 369, 133, R.eye, 2.6, min(determined, 1))
            c.cizgi(402, 133, 415, 127, R.eye, 2.6, min(determined, 1))
        }
        let squint: CGFloat = v(K.squint)
        if squint > 0.01 && goz == .normal {
            // Kısık gözler: üstünde içe doğru inen kapak çizgisi.
            let gx: CGFloat = v(K.gazeX)
            let gy: CGFloat = v(K.gazeY)
            let a: CGFloat = min(squint, 1)
            let e0: CGFloat = S.eyeX[0]
            let e1: CGFloat = S.eyeX[1]
            let y: CGFloat = S.eyeY
            c.cizgi(e0 - 7 + gx, y - 5 + gy, e0 + 6 + gx, y - 2.5 + gy, R.eye, 2.8, a)
            c.cizgi(e1 - 6 + gx, y - 2.5 + gy, e1 + 7 + gx, y - 5 + gy, R.eye, 2.8, a)
        }
    }

    private func eyeArc(_ c: SobiMaskotTuval, _ cx: CGFloat, _ startDeg: CGFloat) {
        let r: CGRect
        if startDeg == 180 {
            r = SobiMaskotMat.dortgen(cx - 6, 140, cx + 6, 152)
        } else {
            r = SobiMaskotMat.dortgen(cx - 6, 137, cx + 6, 149)
        }
        c.ciz(SobiMaskotMat.yay(r, startDeg, 180, merkezli: false), R.eye, 2.6)
    }

    private func drawMouth(_ c: SobiMaskotTuval, _ t: CGFloat) {
        let cheeks: CGFloat = v(K.cheeks)
        if cheeks > 0.01 {
            // Çiğnerken şişen yanaklar.
            let r: CGFloat = 9 * min(cheeks, 1)
            c.daireDoldur(S.cheekLX, S.cheekY, r, R.headFill)
            c.daireDoldur(S.cheekRX, S.cheekY, r, R.headFill)
        }
        let mx: CGFloat = S.mouthX
        switch agiz {
        case .cigneme:
            let k: CGFloat = sin(t * S.tau * 4)
            var p = Path()
            p.move(to: CGPoint(x: mx - 5, y: 167 + k))
            p.addQuadCurve(to: CGPoint(x: mx + 5, y: 167 + k), control: CGPoint(x: mx, y: 170 - 1.5 * k))
            c.ciz(p, R.mouth, 2)
        case .yanGulus:
            var p = Path()
            p.move(to: CGPoint(x: mx - 6, y: 166.5))
            p.addQuadCurve(to: CGPoint(x: mx + 7, y: 163.5), control: CGPoint(x: mx + 1, y: 170.5))
            c.ciz(p, R.mouth, 2)
        case .mutlu:
            c.kaydet()
            c.tasi(mx, S.happyMouthTop)
            c.olcekle(S.happyMouthScale, S.happyMouthScale)
            c.tasi(-S.catMouthX, -S.catMouthTop)
            c.sekiller(V.mutluAgiz)
            c.geriAl()
        case .konusma:
            let a: CGFloat = 0.5 + 0.5 * sin(t * S.tau * 4.2)
            let b: CGFloat = 0.6 + 0.4 * sin(t * S.tau * 1.3)
            let open: CGFloat = SobiMaskotMat.sinirla(a * b, 0, 1)
            let ry: CGFloat = 1.2 + 4.3 * open
            let sekil: Path = SobiMaskotMat.oval(mx - 5, S.mouthY - ry, mx + 5, S.mouthY + ry)
            drawOpenMouth(c, sekil, mx, S.mouthY + ry, 3.5, 2.5)
        case .o:
            c.doldur(SobiMaskotMat.oval(mx - 4, 165, mx + 4, 175), R.mouth)
        case .uzgun:
            let r: CGRect = SobiMaskotMat.dortgen(mx - 6.5, 167, mx + 6.5, 177)
            c.ciz(SobiMaskotMat.yay(r, 200, 140, merkezli: false), R.mouth, 2)
        case .bekleme:
            c.sekiller(V.agiz)
        }
    }

    /// Ağız boşluğu ve dili; dil ağız şekline kırpılıyor.
    private func drawOpenMouth(_ c: SobiMaskotTuval, _ shape: Path, _ tx: CGFloat, _ ty: CGFloat, _ rx: CGFloat, _ ry: CGFloat) {
        c.doldur(shape, R.mouth)
        c.kaydet()
        c.kirp(shape)
        c.doldur(SobiMaskotMat.oval(tx - rx, ty - ry, tx + rx, ty + ry), R.tongue)
        c.geriAl()
    }

    // MARK: Öndeki eller

    /// Köpeğin önde birleşik elleri; eller açıldıkça gövdeye düşen gölgeleri siliniyor.
    private func drawFrontHands(_ c: SobiMaskotTuval, left: Bool, right: Bool) {
        let a: CGFloat = v(K.frontA)
        if a <= 0.01 { return }
        let gap: CGFloat = v(K.frontGap)
        let rot: CGFloat = v(K.frontRot)
        let dy: CGFloat = v(K.frontDy) + (1 - a) * 25
        let kayma1: CGFloat = abs(v(K.frontLX)) + abs(v(K.frontLY))
        let kayma2: CGFloat = abs(v(K.frontRX)) + abs(v(K.frontRY))
        let spread: CGFloat = gap + kayma1 + kayma2
        let shadow: CGFloat = SobiMaskotMat.sinirla(1 - spread / 8, 0, 1)
        c.alfaIle(a) {
            if shadow > 0.01 {
                if left { frontSide(c, -1, gap, rot, dy) { c.sekiller(V.onGolgeSol, shadow) } }
                if right { frontSide(c, 1, gap, rot, dy) { c.sekiller(V.onGolgeSag, shadow) } }
            }
            if left { frontSide(c, -1, gap, rot, dy) { c.sekiller(V.onKolSol) } }
            if right { frontSide(c, 1, gap, rot, dy) { c.sekiller(V.onKolSag) } }
        }
    }

    private func frontSide(_ c: SobiMaskotTuval, _ side: CGFloat, _ gap: CGFloat, _ rot: CGFloat, _ dy: CGFloat, _ blok: () -> Void) {
        c.kaydet()
        if side < 0 {
            c.tasi(v(K.frontLX), v(K.frontLY))
        } else {
            c.tasi(v(K.frontRX), v(K.frontRY))
        }
        c.tasi(side * gap, dy)
        let ownRot: CGFloat = side < 0 ? v(K.frontLRot) : 0
        let omuzX: CGFloat = side < 0 ? S.frontShoulderLX : S.frontShoulderRX
        c.dondur(-side * rot + ownRot, omuzX, S.frontShoulderY)
        c.tasi(S.dogDx, S.dogDy)
        blok()
        c.geriAl()
    }

    /// Düşünürken çeneye götürülen sağ kedi kolu; kafanın ÖNÜNDE. Elde havuç olabilir.
    private func drawThinkArm(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.thinkA)
        c.alfaIle(a) {
            let armDeg: CGFloat = -(v(K.think) - (1 - a) * 60)
            c.dondur(armDeg, S.cheerShoulderRX, S.cheerShoulderRY)
            c.tasi(S.catDx, 0)
            // Havuç elde: patinin arkasında, ucu ekranda istenen yönde.
            drawCarrotAt(c, S.catHandX - 3, S.catHandY, S.handCarrotScale, v(K.hcarrotRot) - armDeg, v(K.hcarrotA), isirikSayisi)
            c.sekiller(V.kediKoluSag)
        }
        drawSpyglass(c)
    }
}
