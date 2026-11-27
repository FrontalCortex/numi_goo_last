import SwiftUI

private typealias S = SobiMaskotSabit
private typealias K = SobiMaskotKanal
private typealias R = SobiMaskotRenk
private typealias V = SobiMaskotCizimVerisi
private typealias M = SobiMaskotMotoru
private typealias X = SobiMaskotMat

// Hâllerin eşyaları ve süsleri: Android BunnyMascotView'daki drawPhone, drawChatBubbles,
// drawTorchFlame, drawAdProps (TV, Atla düğmesi, tabela, billboard), drawBoard, drawNotebook,
// drawCarrot, drawJetpack, drawCape, drawProBadge, drawGlasses, drawCrown, drawFlame,
// drawCalendar, drawAlarmClock, drawAbacus, drawExtras, drawStar, drawHeart birebir.
extension SobiMaskotMotoru {

    // MARK: Telefon ve balonlar

    func drawPhone(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.phoneA)
        c.alfaIle(a) {
            c.tasi(0, v(K.frontDy) - 10 + (1 - a) * 25)
            c.dondur(v(K.frontRot) * 0.4, 395, 222)
            c.doldur(X.yuvarlak(X.dortgen(378, 196, 412, 246), 7), R.phoneEdge)
            c.doldur(X.yuvarlak(X.dortgen(380, 198, 410, 244), 5.5), R.phoneBack)
            c.doldur(X.yuvarlak(X.dortgen(382, 200.5, 394, 216.5), 4), R.phoneBody)
            for ly in R.phoneLensY {
                c.daireDoldur(388, ly, 2.6, R.eye)
                c.daireCiz(388, ly, 2.6, R.bubbleEdge, 0.8)
            }
            c.daireDoldur(391.8, 208.5, 1, R.star)
            c.daireDoldur(395, 228, 4, R.bubbleEdge)
        }
    }

    /// Konuşma balonu: hap biçimli gövde + kuyruk. Android iki şekli birleştirip (Path.op)
    /// kenarlıyor; burada önce iki katı kalınlıkta kenar, üstüne dolgu çiziliyor — iç yarıyı
    /// dolgu örttüğü için yalnızca birleşimin dış kenarı kalıyor.
    private func drawBubble(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ w: CGFloat, _ h: CGFloat, _ s: CGFloat,
                            _ fill: UInt32, _ edge: UInt32, tailRight: Bool, alpha: CGFloat) {
        if s <= 0 || alpha <= 0 { return }
        c.alfaIle(X.sinirla(alpha, 0, 1)) {
            c.tasi(cx, cy)
            c.olcekle(s, s)
            let govde: Path = X.yuvarlak(X.dortgen(-w / 2, -h / 2, w / 2, h / 2), h / 2)
            let tx: CGFloat = tailRight ? w * 0.22 : -w * 0.22
            var kuyruk = Path()
            kuyruk.move(to: CGPoint(x: tx - 6, y: h / 2 - 2))
            kuyruk.addLine(to: CGPoint(x: tx + (tailRight ? 10 : -10), y: h / 2 + 9))
            kuyruk.addLine(to: CGPoint(x: tx + 6, y: h / 2 - 2))
            kuyruk.closeSubpath()
            c.ciz(govde, edge, 3.2)
            c.ciz(kuyruk, edge, 3.2)
            c.doldur(govde, fill)
            c.doldur(kuyruk, fill)
        }
    }

    private func drawChatBubbles(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let outP: CGFloat = X.sinirla((e - S.chatFadeS) / 0.4, 0, 1)
        let alpha: CGFloat = 1 - outP
        if alpha <= 0 { return }
        let rise: CGFloat = 8 * outP

        if e >= S.chatAskS {
            let p: CGFloat = X.sinirla((e - S.chatAskS) / 0.45, 0, 1)
            let ease: CGFloat = 1 - (1 - p) * (1 - p)
            let x: CGFloat = 395 + (S.askBubbleX - 395) * ease
            let y: CGFloat = 196 + (S.askBubbleY - 196) * ease - rise
            let s: CGFloat = 0.3 + 0.7 * ease
            drawBubble(c, x, y, 44, 32, s, R.white, R.bubbleEdge, tailRight: true, alpha: alpha)
            c.yazi("?", x, y + 8 * s, 22 * s, R.eye, alpha)
        }

        if e >= S.chatReplyS {
            let s: CGFloat = M.popScale(e - S.chatReplyS)
            let x: CGFloat = S.replyBubbleX
            let y: CGFloat = S.replyBubbleY - rise
            drawBubble(c, x, y, 48, 32, s, R.star, R.starEdge, tailRight: false, alpha: alpha)
            if e < S.chatAnswerS {
                for i in 0...2 {
                    let bounce: CGFloat = 3 * max(0, sin(e * S.tau * 2 - CGFloat(i) * 0.8))
                    c.daireDoldur(x + CGFloat(i - 1) * 9 * s, y - bounce * s, 3 * s, R.eye, alpha)
                }
            } else {
                let q: CGFloat = s * M.popScale(e - S.chatAnswerS)
                var ok = Path()
                ok.move(to: CGPoint(x: x - 5 * q, y: y - 8 * q))
                ok.addLine(to: CGPoint(x: x + 9 * q, y: y))
                ok.addLine(to: CGPoint(x: x - 5 * q, y: y + 8 * q))
                ok.closeSubpath()
                c.doldur(ok, R.eye, alpha)
            }
        }
    }

    // MARK: Alev

    /// Sağ elde tutulan seri alevi (kolun dönüşü burada tekrarlanıyor, alev dik).
    func drawTorchFlame(_ c: SobiMaskotTuval, _ t: CGFloat) {
        let a: CGFloat = v(K.flameA)
        if a <= 0.01 { return }
        let rightUp: CGFloat = v(K.catR) - (1 - v(K.catRA)) * 50
        c.kaydet()
        c.dondur(-rightUp, S.cheerShoulderRX, S.cheerShoulderRY)
        c.tasi(S.catDx, 0)
        c.tasi(S.catHandX, S.catHandY - 3)
        c.dondur(rightUp)
        drawFlame(c, 0, 0, v(K.flameH) * (0.4 + 0.6 * a), t, a)
        c.geriAl()
    }

    /// Uygulamanın seri alevi (streak_flame_ic) + arkasında sıcak bir hale.
    func drawFlame(_ c: SobiMaskotTuval, _ cx: CGFloat, _ bottom: CGFloat, _ h: CGFloat, _ t: CGFloat, _ alpha: CGFloat) {
        if h <= 0 || alpha <= 0.01 { return }
        let a: CGFloat = X.sinirla(alpha, 0, 1)
        c.kaydet()
        c.tasi(cx, bottom - h * 0.45)
        let r: CGFloat = h * 0.9 * (1 + 0.06 * sin(t * S.tau * 2.3))
        c.olcekle(r, r)
        let hale = Gradient(colors: [X.renk(R.glowInner, a), X.renk(R.glowOuter, a)])
        c.ctx.fill(X.daire(0, 0, 1), with: .radialGradient(hale, center: .zero, startRadius: 0, endRadius: 1))
        c.geriAl()

        c.kaydet()
        c.tasi(cx, bottom)
        c.dondur(3 * sin(t * S.tau * 1.7))
        let sx: CGFloat = 1 - 0.04 * sin(t * S.tau * 4.3)
        let sy: CGFloat = 1 + 0.07 * sin(t * S.tau * 4.3) + 0.03 * sin(t * S.tau * 9.1)
        c.olcekle(sx, sy)
        let s: CGFloat = h / S.flameIconH
        c.olcekle(s, s)
        c.tasi(-S.flameIconCX, -S.flameIconBottom)
        c.cizim(V.seriAlevi, alani: V.seriAleviAlani, boyut: CGSize(width: 100, height: 100), a)
        c.geriAl()
    }

    // MARK: Reklam hâllerinin sahnesi

    func drawAdProps(_ c: SobiMaskotTuval, _ e: CGFloat) {
        guard let h = hal else { return }
        switch h {
        case .AD_JUMP:
            let x: CGFloat = M.adTvX(e)
            if x > 200 && x < 590 { drawAdTv(c, x) }
        case .AD_BUTTON:
            drawSkipButton(c, e)
        case .AD_ZAP:
            drawBillboardScene(c, e, zap: true)
        case .AD_MAGIC:
            drawBillboardScene(c, e, zap: false)
        case .AD_PUSH:
            let appear: CGFloat = M.popScale(e - 0.1)
            if appear <= 0 { return }
            let p: CGFloat = X.sinirla((e - S.adPushStartS) / (S.adPushEndS - S.adPushStartS), 0, 1)
            let cx: CGFloat = S.adSignX + 120 * p * p
            if cx > 590 { return }
            let wobble: CGFloat = (p > 0 && p < 1) ? 4 * sin(e * S.tau * 3) : 0
            drawAdSign(c, cx, appear, wobble)
        default:
            return
        }
    }

    /// Şövaleli kara tahta; cevap tebeşirle soldan sağa beliriyor, altı çiziliyor, siliniyor.
    func drawBoard(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.boardA)
        c.alfaIle(a) {
            let g: CGFloat = 0.6 + 0.4 * a
            c.olcekle(g, g, (S.boardX0 + S.boardX1) / 2, 305)
            c.cizgi(S.boardX0 + 12, S.boardY1, S.boardX0 + 5, 305, R.wood, 4)
            c.cizgi(S.boardX1 - 12, S.boardY1, S.boardX1 - 5, 305, R.wood, 4)
            c.doldur(X.yuvarlak(X.dortgen(S.boardX0, S.boardY0, S.boardX1, S.boardY1), 4), R.wood)
            c.doldur(X.yuvarlak(X.dortgen(S.boardX0 + 4, S.boardY0 + 4, S.boardX1 - 4, S.boardY1 - 4), 2), R.boardGreen)
            if hal != .BOARD { return }

            let text: String = S.boardSamples[turSayisi % S.boardSamples.count]
            let tw: CGFloat = c.yaziGenisligi(text, S.boardTextSize)
            let cx: CGFloat = (S.boardX0 + S.boardX1) / 2 + 4
            let left: CGFloat = cx - tw / 2
            let reveal: CGFloat = X.sinirla((e - S.boardWriteS) / (S.boardDoneS - S.boardWriteS), 0, 1)
            let keep: CGFloat = 1 - X.sinirla((e - S.boardEraseS) / 0.5, 0, 1)
            if reveal > 0 && keep > 0 {
                c.kaydet()
                c.kirp(Path(X.dortgen(left - 1, S.boardY0, left + tw * reveal + 1, S.boardY1)))
                c.yazi(text, cx, S.boardTextY, S.boardTextSize, R.chalk, keep)
                c.geriAl()
                let under: CGFloat = X.sinirla((e - S.boardDoneS) / 0.3, 0, 1)
                if under > 0 {
                    c.cizgi(left, S.boardTextY + 5, left + tw * under, S.boardTextY + 5, R.chalk, 1.6, keep)
                }
            }
        }
    }

    /// Defterin ARKA kapağı: kırmızı kapak, spiral, beyaz etiket, yıldız çıkartması.
    func drawNotebook(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.noteA)
        c.alfaIle(a) {
            c.tasi(0, (1 - a) * 25)
            c.doldur(X.yuvarlak(X.dortgen(S.noteX0, S.noteY0, S.noteX1, S.noteY1), 3.5), R.noteCover)
            c.doldur(X.yuvarlak(X.dortgen(S.noteX0, S.noteY1 - 4, S.noteX1, S.noteY1), 3.5), R.noteCoverDark)
            c.doldur(X.yuvarlak(X.dortgen(S.noteX0 + 16, S.noteY0 + 14, S.noteX1 - 16, S.noteY0 + 27), 2.5), R.white)
            c.cizgi(S.noteX0 + 20, S.noteY0 + 19, S.noteX1 - 20, S.noteY0 + 19, R.metalGray, 1)
            c.cizgi(S.noteX0 + 20, S.noteY0 + 23, S.noteX1 - 28, S.noteY0 + 23, R.metalGray, 1)
            var x: CGFloat = S.noteX0 + 6
            while x <= S.noteX1 - 4 {
                c.daireCiz(x, S.noteY0, 1.8, R.metalGray, 1.2)
                x += 6
            }
            drawStar(c, S.noteX1 - 11, S.noteY1 - 12, 5, 1)
        }
    }

    /// Havuç kalem (carrot_ic): sağ elin tuttuğu yerden ucu aşağı-sola.
    func drawCarrot(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.noteA)
        if a <= 0.01 { return }
        let fa: CGFloat = v(K.frontA)
        let pawX: CGFloat = S.frontRPawX + v(K.frontRX)
        let pawY: CGFloat = S.frontRPawY + v(K.frontRY) + v(K.frontDy) + (1 - fa) * 25
        c.kaydet()
        c.tasi(pawX, pawY)
        c.olcekle(S.carrotScale, S.carrotScale)
        c.tasi(-S.carrotGripX, -S.carrotGripY)
        c.cizim(V.havuc, alani: V.havucAlani, boyut: CGSize(width: 512, height: 512), min(a, 1))
        c.geriAl()
    }

    /// Havuç: tutulan yer ([x], [y]), ucu [tipDeg] yönünde; [bites] kadar ısırık oyuluyor.
    func drawCarrotAt(_ c: SobiMaskotTuval, _ x: CGFloat, _ y: CGFloat, _ scale: CGFloat, _ tipDeg: CGFloat, _ alpha: CGFloat, _ bites: Int) {
        if alpha <= 0.01 { return }
        c.kaydet()
        c.tasi(x, y)
        c.dondur(tipDeg - S.carrotTipDeg)
        c.olcekle(scale, scale)
        c.tasi(-S.carrotGripX, -S.carrotGripY)
        if bites > 0 {
            var isirik: Path = X.daire(S.bite1X, S.bite1Y, S.bite1R)
            if bites > 1 { isirik.addPath(X.daire(S.bite2X, S.bite2Y, S.bite2R)) }
            c.kirpDisi(isirik)
        }
        c.cizim(V.havuc, alani: V.havucAlani, boyut: CGSize(width: 512, height: 512), min(alpha, 1))
        c.geriAl()
    }

    /// Değnek hâllerinin sahnesi: billboard; yıldırımla kül oluyor ya da yasak işaretiyle tozlaşıyor.
    private func drawBillboardScene(_ c: SobiMaskotTuval, _ e: CGFloat, zap: Bool) {
        let appear: CGFloat = M.popScale(e - 0.05)
        if appear <= 0 { return }
        if zap {
            billboardYildirim(c, e, appear)
        } else {
            billboardBuyu(c, e, appear)
        }
    }

    private func billboardYildirim(_ c: SobiMaskotTuval, _ e: CGFloat, _ appear: CGFloat) {
        let char: CGFloat = X.sinirla((e - S.zapStrikeS) / S.zapCharS, 0, 1)
        let crumble: CGFloat = X.sinirla((e - S.zapStrikeS - S.zapCharS) / S.zapCrumbleS, 0, 1)
        if crumble < 1 {
            let hit: CGFloat = e - S.zapStrikeS
            let shake: CGFloat = (hit >= 0 && hit <= 0.3) ? 2.5 * sin(hit * S.tau * 25) : 0
            c.kaydet()
            c.tasi(shake, 0)
            c.olcekle(appear, appear * (1 - 0.7 * crumble), S.bbX, 305)
            let k: CGFloat = (255 * (1 - 0.78 * char)).rounded()
            drawBillboard(c, 1 - crumble, char > 0 ? k / 255 : 1)
            c.geriAl()
        }
        let ashFade: CGFloat = 1 - X.sinirla((e - S.zapFadeS) / 0.4, 0, 1)
        if crumble > 0 && ashFade > 0 { drawAshPile(c, crumble, ashFade, e) }
        // Duman
        let smokeT: CGFloat = e - S.zapStrikeS - S.zapCharS
        if smokeT > 0 {
            for i in 0...2 {
                let fi: CGFloat = CGFloat(i)
                let p: CGFloat = smokeT / 1.3 - fi * 0.18
                if p <= 0 || p >= 1 { continue }
                let a: CGFloat = (110 * (1 - p) * ashFade).rounded() / 255
                let x: CGFloat = S.bbX + (fi - 1) * 10 + 6 * sin(p * 6 + fi)
                c.daireDoldur(x, 284 - 55 * p, 6 + 10 * p, R.smoke, a)
            }
        }
        // Yıldırım: birkaç kez yanıp sönerek.
        let bolt: CGFloat = e - S.zapStrikeS + 0.08
        if bolt >= 0 && bolt <= 0.32 {
            let on: Bool = (Int(bolt * 22) % 3) != 2
            if on { drawBolt(c) }
            let a: CGFloat = (110 * (1 - bolt / 0.32)).rounded() / 255
            c.daireDoldur(S.bbX, S.bbBoardCY, 44, R.bolt, a)
        }
    }

    private func billboardBuyu(_ c: SobiMaskotTuval, _ e: CGFloat, _ appear: CGFloat) {
        let stamp: CGFloat = e - S.magicStampS
        let poof: CGFloat = X.sinirla((e - S.magicPoofS) / 0.4, 0, 1)
        if poof < 1 {
            let shake: CGFloat = (stamp >= 0 && stamp <= 0.3) ? 2 * sin(stamp * S.tau * 22) : 0
            c.kaydet()
            c.tasi(shake, 0)
            c.olcekle(appear * (1 - poof), appear * (1 - poof), S.bbX, S.bbBoardCY)
            drawBillboard(c, 1 - poof, 1)
            if stamp >= 0 {
                let p: CGFloat = X.sinirla(stamp / 0.18, 0, 1)
                let s: CGFloat = 1 + 0.8 * (1 - p) * (1 - p)
                drawNoSign(c, S.bbX, S.bbBoardCY, 29 * s, p * (1 - poof))
            }
            c.geriAl()
        }
        // Değnekten billboarda uçan parıltılar
        let tip: CGPoint = M.wandTip(S.wandPointUp)
        for k in 0...2 {
            let q: CGFloat = (e - S.wandWaveEndS - CGFloat(k) * 0.06) / (S.magicStampS - S.wandWaveEndS)
            if q <= 0 || q >= 1 { continue }
            let x: CGFloat = tip.x + (S.bbX - tip.x) * q
            let y: CGFloat = tip.y + (S.bbBoardCY - tip.y) * q - 30 * sin(CGFloat.pi * q)
            drawStar(c, x, y, 4.5, 1)
        }
        // Yıldız tozu
        let dust: CGFloat = e - S.magicPoofS
        if dust >= 0 && dust <= 0.7 {
            let p: CGFloat = dust / 0.7
            for i in 0..<10 {
                let ang: CGFloat = CGFloat(i) * S.tau / 10 + 0.3
                let r: CGFloat = 8 + 46 * p
                drawStar(c, S.bbX + cos(ang) * r, S.bbBoardCY + sin(ang) * r, 4.5 * (1 - p) + 1, 1 - p)
            }
        }
    }

    private func drawBillboard(_ c: SobiMaskotTuval, _ alpha: CGFloat, _ renkCarpani: CGFloat) {
        c.kaydet()
        c.tasi(S.bbX - 256 * S.bbScale, S.bbTop)
        c.olcekle(S.bbScale, S.bbScale)
        c.cizim(V.reklamPanosu, alani: V.reklamPanosuAlani, boyut: CGSize(width: 512, height: 512), X.sinirla(alpha, 0, 1), renkCarpani: renkCarpani)
        c.geriAl()
    }

    private func drawNoSign(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ alpha: CGFloat) {
        if alpha <= 0 { return }
        let a: CGFloat = X.sinirla(alpha, 0, 1)
        c.daireCiz(cx, cy, r, R.noSignRed, r * 0.23, a)
        let d: CGFloat = r * 0.7071
        c.cizgi(cx - d, cy - d, cx + d, cy + d, R.noSignRed, r * 0.23, a)
    }

    private func drawBolt(_ c: SobiMaskotTuval) {
        var p = Path()
        p.move(to: CGPoint(x: S.bbX + 12, y: 6))
        p.addLine(to: CGPoint(x: S.bbX - 8, y: 80))
        p.addLine(to: CGPoint(x: S.bbX + 7, y: 84))
        p.addLine(to: CGPoint(x: S.bbX - 12, y: 160))
        p.addLine(to: CGPoint(x: S.bbX + 4, y: 164))
        p.addLine(to: CGPoint(x: S.bbX - 2, y: S.bbTop + 8))
        c.ciz(p, R.bolt, 5, 1, keskin: true)
        c.ciz(p, R.white, 2, 1, keskin: true)
    }

    private func drawAshPile(_ c: SobiMaskotTuval, _ grow: CGFloat, _ alpha: CGFloat, _ e: CGFloat) {
        let h: CGFloat = 18 * grow
        let tepe: Path = X.yay(X.dortgen(S.bbX - 32, 305 - h, S.bbX + 32, 305 + h), 180, 180, merkezli: true)
        c.doldur(tepe, R.ash, alpha)
        for i in 0..<S.ashSpeckX.count {
            c.daireDoldur(S.bbX + S.ashSpeckX[i], 305 - h * S.ashSpeckY[i], 1.6, R.ashDark, alpha)
        }
        for i in 0...2 {
            let fi: CGFloat = CGFloat(i)
            let blink: CGFloat = 0.5 + 0.5 * sin(e * S.tau * 3 + fi * 2)
            let a: CGFloat = (200 * blink * alpha * grow).rounded() / 255
            c.daireDoldur(S.bbX - 10 + fi * 10, 305 - h * 0.45, 1.4, R.ember, a)
        }
    }

    /// Küçük, eski usul TV: koyu gövde, açık ekranda kırmızı "REKLAM", iki anten, ayaklar.
    private func drawAdTv(_ c: SobiMaskotTuval, _ cx: CGFloat) {
        let top: CGFloat = S.adTvTop
        c.doldur(X.oval(cx - 22, 302, cx + 22, 308), R.shadowBlack)
        c.cizgi(cx - 3, top, cx - 8, top - 4.5, R.metalGray, 1.6)
        c.cizgi(cx + 3, top, cx + 8, top - 4.5, R.metalGray, 1.6)
        c.doldur(Path(X.dortgen(cx - 9, top + 20, cx - 6, 305)), R.phoneBody)
        c.doldur(Path(X.dortgen(cx + 6, top + 20, cx + 9, 305)), R.phoneBody)
        c.doldur(X.yuvarlak(X.dortgen(cx - S.adTvW / 2, top, cx + S.adTvW / 2, top + 21), 4), R.phoneBody)
        c.doldur(X.yuvarlak(X.dortgen(cx - S.adTvW / 2 + 3, top + 3, cx + S.adTvW / 2 - 3, top + 18), 2), R.light)
        c.yazi(Metin.al("ui_reklam_tv"), cx, top + 14.2, 9.5, R.adRed, 1)
    }

    /// "Atla ⏭" düğmesi: beliriyor, basılınca gömülüp maviye dönüyor, sağa kayıp kayboluyor.
    private func drawSkipButton(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let appear: CGFloat = M.popScale(e - 0.1)
        if appear <= 0 { return }
        let gone: CGFloat = X.sinirla((e - S.adBtnPressS - 0.3) / 0.4, 0, 1)
        if gone >= 1 { return }
        let pressed: Bool = e >= S.adBtnPressS
        let basma: CGFloat = X.sinirla((e - S.adBtnPressS) / 0.25, 0, 1)
        let press: CGFloat = pressed ? 1 - 0.08 * sin(CGFloat.pi * basma) : 1
        let alpha: CGFloat = 1 - gone
        let cx: CGFloat = S.adBtnX + 40 * gone
        let cy: CGFloat = S.adBtnY
        c.kaydet()
        c.tasi(cx, cy)
        c.olcekle(appear * press, appear * press)
        let kutu: Path = X.yuvarlak(X.dortgen(-S.adBtnW / 2, -12, S.adBtnW / 2, 12), 4)
        c.doldur(kutu, pressed ? R.bubbleEdge : R.phoneBody, alpha)
        c.ciz(kutu, R.white, 1.2, alpha)
        c.yazi(Metin.al("ui_atla"), -6, 4, 11, R.white, alpha)
        var ok = Path()
        ok.move(to: CGPoint(x: 11, y: -4))
        ok.addLine(to: CGPoint(x: 15, y: 0))
        ok.addLine(to: CGPoint(x: 11, y: 4))
        ok.closeSubpath()
        ok.move(to: CGPoint(x: 15, y: -4))
        ok.addLine(to: CGPoint(x: 19, y: 0))
        ok.addLine(to: CGPoint(x: 15, y: 4))
        ok.closeSubpath()
        c.doldur(ok, R.white, alpha)
        c.doldur(Path(X.dortgen(19.5, -4, 21, 4)), R.white, alpha)
        c.geriAl()
        if pressed {
            let s: CGFloat = M.popScale(e - S.adBtnPressS)
            let fade: CGFloat = X.sinirla(1 - (e - S.adBtnPressS) / 0.6, 0, 1)
            drawStar(c, cx - 18, cy - 18, 4 * s, fade)
            drawStar(c, cx + 22, cy - 16, 5 * s, fade)
            drawStar(c, cx + 6, cy + 20, 3.5 * s, fade)
        }
    }

    /// İki direkli REKLAM tabelası; itilirken tabanı etrafında sallanıyor.
    private func drawAdSign(_ c: SobiMaskotTuval, _ cx: CGFloat, _ scale: CGFloat, _ wobble: CGFloat) {
        c.kaydet()
        c.dondur(wobble, cx, 305)
        c.olcekle(scale, scale, cx, 305)
        c.doldur(X.oval(cx - 26, 302, cx + 26, 308), R.shadowBlack)
        c.doldur(Path(X.dortgen(cx - 17, 240, cx - 13, 305)), R.wood)
        c.doldur(Path(X.dortgen(cx + 13, 240, cx + 17, 305)), R.wood)
        let pano: Path = X.yuvarlak(X.dortgen(cx - 25, 213, cx + 25, 246), 4)
        c.doldur(pano, R.white)
        c.ciz(pano, R.adRed, 2.4)
        c.yazi(Metin.al("ui_reklam_tv"), cx + 4, 233, 10, R.adRed, 1)
        c.geriAl()
    }

    // MARK: Pro hâlleri

    /// Jet çantası: iki gümüş tüp, memeler, aşağı yanan alev, düşen kıvılcımlar.
    func drawJetpack(_ c: SobiMaskotTuval, _ t: CGFloat) {
        let a: CGFloat = v(K.jetA)
        c.alfaIle(a) {
            for (i, x) in S.jetTankX.enumerated() {
                let fi: CGFloat = CGFloat(i)
                c.doldur(X.yuvarlak(X.dortgen(x, 196, x + 18, 248), 8), R.jetSilver)
                c.doldur(Path(X.dortgen(x, 210, x + 18, 215)), R.jetBand)
                c.doldur(X.yuvarlak(X.dortgen(x + 3, 200, x + 7, 240), 2), R.jetShine)
                var meme = Path()
                meme.move(to: CGPoint(x: x + 3, y: 248))
                meme.addLine(to: CGPoint(x: x + 15, y: 248))
                meme.addLine(to: CGPoint(x: x + 13, y: 256))
                meme.addLine(to: CGPoint(x: x + 5, y: 256))
                meme.closeSubpath()
                c.doldur(meme, R.jetNozzle)

                let cx: CGFloat = x + 9
                let titreme: CGFloat = 0.85 + 0.15 * sin(t * S.tau * 7 + fi * 2)
                let h: CGFloat = 26 * titreme * (0.4 + 0.6 * a)
                c.kaydet()
                c.tasi(cx, 255)
                c.olcekle(1, -1)
                drawFlame(c, 0, 0, h, t + fi * 0.37, 1)
                c.geriAl()

                for k in 0...2 {
                    let p: CGFloat = X.kalan(t * 1.4 + CGFloat(k) / 3 + fi * 0.17, 1)
                    let ka: CGFloat = (255 * (1 - p) * 0.8).rounded() / 255
                    let kx: CGFloat = cx + 3 * sin(p * 7 + CGFloat(k))
                    c.daireDoldur(kx, 258 + h * 0.6 + 30 * p, 2.2 * (1 - p), R.star, ka)
                }
            }
        }
    }

    /// Süper kahraman pelerini; yanları rüzgârla açılıp kapanıyor, alt kenarı dalgalanıyor.
    func drawCape(_ c: SobiMaskotTuval, _ t: CGFloat) {
        let a: CGFloat = v(K.capeA)
        c.alfaIle(a) {
            c.olcekle(1, 0.3 + 0.7 * a, 395, 200)
            let w: CGFloat = 4 * sin(t * S.tau * 1.3)
            let wave: CGFloat = 8 * sin(t * S.tau * 1.8)
            let l: CGFloat = 330 - w
            let r: CGFloat = 460 + w
            var dis = Path()
            dis.move(to: CGPoint(x: 368, y: 200))
            dis.addCurve(to: CGPoint(x: l, y: 288), control1: CGPoint(x: 352, y: 215), control2: CGPoint(x: l + 6, y: 250))
            dis.addQuadCurve(to: CGPoint(x: l + 44, y: 288), control: CGPoint(x: l + 22, y: 296 + wave))
            dis.addQuadCurve(to: CGPoint(x: 395, y: 290), control: CGPoint(x: l + 66, y: 280 - wave))
            dis.addQuadCurve(to: CGPoint(x: r - 44, y: 288), control: CGPoint(x: r - 66, y: 298 + wave))
            dis.addQuadCurve(to: CGPoint(x: r, y: 288), control: CGPoint(x: r - 22, y: 280 - wave))
            dis.addCurve(to: CGPoint(x: 422, y: 200), control1: CGPoint(x: r - 6, y: 250), control2: CGPoint(x: 438, y: 215))
            dis.closeSubpath()
            c.doldur(dis, R.capeRed)
            var ic = Path()
            ic.move(to: CGPoint(x: 374, y: 204))
            ic.addCurve(to: CGPoint(x: l + 12, y: 284), control1: CGPoint(x: 362, y: 220), control2: CGPoint(x: l + 16, y: 252))
            ic.addLine(to: CGPoint(x: r - 12, y: 284))
            ic.addCurve(to: CGPoint(x: 416, y: 204), control1: CGPoint(x: r - 16, y: 252), control2: CGPoint(x: 428, y: 220))
            ic.closeSubpath()
            c.doldur(ic, R.capeInner)
        }
    }

    /// Göğüste PRO rozeti (Pro ekranındaki rozetin renk geçişiyle).
    func drawProBadge(_ c: SobiMaskotTuval) {
        let a: CGFloat = v(K.capeA)
        if a <= 0.01 { return }
        let k: CGFloat = min(a, 1)
        let renkler: [Color] = R.badgeGradient.map { X.renk($0, k) }
        let gecis = Gradient(colors: renkler)
        let rozet: Path = X.yuvarlak(X.dortgen(380, 239, 410, 252), 3.5)
        c.ctx.fill(rozet, with: .linearGradient(gecis, startPoint: CGPoint(x: 380, y: 0), endPoint: CGPoint(x: 410, y: 0)))
        c.yazi("PRO", 395, 249, 9, R.white, a)
    }

    /// Havalı güneş gözlüğü (cool_glasses), camların ortası gözlerin üstünde.
    func drawGlasses(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.glassesA)
        if a <= 0.01 { return }
        c.kaydet()
        c.tasi(S.glassesLeft, S.glassesTop + gozlukDususu)
        c.olcekle(S.glassesScale, S.glassesScale)
        c.cizim(V.havaliGozluk, alani: V.havaliGozlukAlani, boyut: CGSize(width: S.glassesW, height: S.glassesH), min(a, 1))
        c.geriAl()
        let t: CGFloat = gozlukParilti
        if t > 0 && t < 0.6 {
            drawStar(c, 404, 136, 6 * M.popScale(t), 1 - t / 0.6)
        }
    }

    /// Başta yan yatık altın taç.
    func drawCrown(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.crownA)
        c.alfaIle(a) {
            c.tasi(0, -(1 - a) * 20 + tacDususu)
            c.dondur(-10, 389, 113)
            var tac = Path()
            tac.move(to: CGPoint(x: 368, y: 112))
            tac.addLine(to: CGPoint(x: 370, y: 92))
            tac.addLine(to: CGPoint(x: 380, y: 103))
            tac.addLine(to: CGPoint(x: 389, y: 87))
            tac.addLine(to: CGPoint(x: 398, y: 103))
            tac.addLine(to: CGPoint(x: 408, y: 92))
            tac.addLine(to: CGPoint(x: 410, y: 112))
            tac.closeSubpath()
            c.doldur(tac, R.crownGold)
            c.doldur(X.yuvarlak(X.dortgen(367, 108, 411, 118), 2.5), R.crownBand)
            c.daireDoldur(378, 113, 2.2, R.tongue)
            c.daireDoldur(400, 113, 2.2, R.tongue)
            c.daireDoldur(389, 113, 2.4, R.bubbleEdge)
            c.daireDoldur(370, 92, 2.2, R.crownTip)
            c.daireDoldur(389, 87, 2.4, R.crownTip)
            c.daireDoldur(408, 92, 2.2, R.crownTip)
        }
    }

    // MARK: Seri takvimi, çalar saat, abaküs

    func drawCalendar(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.calA)
        c.alfaIle(a) {
            let grow: CGFloat = 0.8 + 0.2 * a
            c.olcekle(grow, grow, 395, 234)
            c.doldur(X.yuvarlak(X.dortgen(349, 204, 441, 264), 7), R.white)
            c.doldur(X.yuvarlak(X.dortgen(349, 204, 441, 218), 7), R.bubbleEdge)
            c.doldur(Path(X.dortgen(349, 211, 441, 218)), R.bubbleEdge)
            c.daireDoldur(370, 204, 2.8, R.phoneBody)
            c.daireDoldur(420, 204, 2.8, R.phoneBody)

            let n: Int = calendarDays()
            let boxes: Int = n > 0 ? n : (S.demoDays.last ?? 7)
            let playing: Bool = hal == .CALENDAR && n > 0
            let keep: CGFloat = 1 - X.sinirla((e - S.calResetS) / 0.5, 0, 1)
            let w: CGFloat = CGFloat(boxes) * S.calBox + CGFloat(boxes - 1) * S.calGap
            let x0: CGFloat = 395 - w / 2
            for i in 0..<boxes {
                let x: CGFloat = x0 + CGFloat(i) * (S.calBox + S.calGap)
                let kutu: Path = X.yuvarlak(X.dortgen(x, 228, x + S.calBox, 228 + S.calBox), 2.5)
                c.doldur(kutu, R.calBoxFill)
                c.ciz(kutu, R.bubbleEdge, 1.2)
                let tick: CGFloat = playing ? M.popScale(e - S.calFirstTickS - CGFloat(i) * S.calTickS) * keep : 0
                if tick > 0 {
                    c.kaydet()
                    c.olcekle(tick, tick, x + S.calBox / 2, 228 + S.calBox / 2)
                    var isaret = Path()
                    isaret.move(to: CGPoint(x: x + 2.2, y: 233.2))
                    isaret.addLine(to: CGPoint(x: x + 4.4, y: 235.6))
                    isaret.addLine(to: CGPoint(x: x + 8, y: 230.6))
                    c.ciz(isaret, R.checkGreen, 2)
                    c.geriAl()
                }
            }
            let yazi: String = n > 0 ? Metin.cogul("ui_x_gun", n, n) : Metin.al("ui_gun")
            c.yazi(yazi, 395, 255, 11, R.eye, 1)
            if playing {
                let f: CGFloat = M.popScale(e - M.calendarDoneS(n)) * keep
                if f > 0 { drawFlame(c, 437, 212, 18 * f, e, 1) }
            }
        }
    }

    /// İki çanlı çalar saat; çalarken sallanıyor, çekiç titriyor, ses çizgileri çıkıyor.
    func drawAlarmClock(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.alarmA)
        if a <= 0.01 { return }
        let ring: CGFloat = hal == .ALARM ? M.alarmRingEnv(alarmRingAge(e, simdiMs)) : 0
        let cx0: CGFloat = S.clockX
        let cy0: CGFloat = S.clockY
        let cr: CGFloat = S.clockR
        c.alfaIle(a) {
            c.tasi(0, (1 - a) * 25)
            let grow: CGFloat = 0.8 + 0.2 * a
            c.olcekle(grow, grow, cx0, cy0)

            c.kaydet()
            c.dondur(ring * 7 * sin(e * S.tau * S.alarmShakeHz), cx0, cy0 + cr)
            for dir in [CGFloat(-1), CGFloat(1)] {
                c.cizgi(cx0 + dir * 12, cy0 + cr - 6, cx0 + dir * 17, cy0 + cr + 4, R.alarmRedDark, 4)
            }
            for dir in [CGFloat(-1), CGFloat(1)] {
                let bx: CGFloat = cx0 + dir * S.alarmBellSin * (cr + 3)
                let by: CGFloat = cy0 - S.alarmBellCos * (cr + 3)
                c.kaydet()
                c.dondur(dir * S.alarmBellDeg, bx, by)
                c.doldur(X.yay(X.dortgen(bx - 9, by - 6, bx + 9, by + 12), 180, 180, merkezli: true), R.alarmBell)
                c.doldur(X.yuvarlak(X.dortgen(bx - 9, by + 2, bx + 9, by + 4.6), 1.3), R.alarmBellDark)
                c.geriAl()
            }
            c.kaydet()
            c.dondur(ring * 22 * sin(e * S.tau * S.alarmShakeHz * 2), cx0, cy0 - cr)
            c.cizgi(cx0, cy0 - cr, cx0, cy0 - cr - 6, R.alarmRedDark, 2.4)
            c.daireDoldur(cx0, cy0 - cr - 7.5, 3, R.alarmBellDark)
            c.geriAl()
            c.daireDoldur(cx0, cy0, cr, R.alarmRed)
            c.daireCiz(cx0, cy0, cr, R.alarmRedDark, 2.5)
            c.daireDoldur(cx0, cy0, cr - 5, R.white)
            for i in 0..<12 {
                let rad: CGFloat = X.radyan(CGFloat(i * 30 - 90))
                let ux: CGFloat = cos(rad)
                let uy: CGFloat = sin(rad)
                if i % 3 == 0 {
                    c.cizgi(cx0 + ux * (cr - 9), cy0 + uy * (cr - 9), cx0 + ux * (cr - 6), cy0 + uy * (cr - 6), R.eye, 1.6)
                } else {
                    c.daireDoldur(cx0 + ux * (cr - 7.2), cy0 + uy * (cr - 7.2), 0.8, R.eye)
                }
            }
            drawClockHand(c, alarmAcisi, cr * 0.45, 3)
            drawClockHand(c, X.kalan(alarmAcisi * 12, 360), cr * 0.66, 2)
            c.daireDoldur(cx0, cy0, 2, R.alarmRed)
            c.geriAl()

            if ring > 0.01 {
                let cy: CGFloat = cy0 - cr * 0.9
                for dir in [CGFloat(-1), CGFloat(1)] {
                    let cx: CGFloat = cx0 + dir * (cr + 12)
                    let start: CGFloat = dir > 0 ? -35 : 145
                    let yaylar: [(CGFloat, CGFloat)] = [(6, 1), (11, 0.7)]
                    for (r, op) in yaylar {
                        let yol: Path = X.yay(X.dortgen(cx - r, cy - r, cx + r, cy + r), start, 70, merkezli: false)
                        c.ciz(yol, R.white, 2, ring * op)
                    }
                }
            }
        }
    }

    private func drawClockHand(_ c: SobiMaskotTuval, _ deg: CGFloat, _ len: CGFloat, _ width: CGFloat) {
        let rad: CGFloat = X.radyan(deg - 90)
        c.cizgi(S.clockX, S.clockY, S.clockX + cos(rad) * len, S.clockY + sin(rad) * len, R.eye, width)
    }

    /// Küçük bir soroban; boncuklar sayılıyormuş gibi kayıyor.
    func drawAbacus(_ c: SobiMaskotTuval, _ e: CGFloat) {
        let a: CGFloat = v(K.abacusA)
        c.alfaIle(a) {
            let grow: CGFloat = 0.8 + 0.2 * a
            c.olcekle(grow, grow, 395, 236)
            c.doldur(X.yuvarlak(X.dortgen(357, 214, 433, 258), 6), R.wood)
            c.doldur(X.yuvarlak(X.dortgen(362, 219, 428, 253), 3), R.woodLight)
            for x in S.abacusRodX {
                c.cizgi(x, 219, x, 253, R.woodDark, 1.4)
            }
            c.doldur(Path(X.dortgen(362, 228, 428, 230.6)), R.wood)
            for i in 0..<S.abacusRodX.count {
                let x: CGFloat = S.abacusRodX[i]
                let fi: CGFloat = CGFloat(i)
                let renk: UInt32 = R.abacusBeads[i]
                let heaven: CGFloat = 0.5 + 0.5 * sin(e * S.tau * 0.5 + fi * 2.1)
                let earth: CGFloat = 0.5 + 0.5 * sin(e * S.tau * 0.7 + fi * 1.3)
                c.doldur(beadPath(x, 222 + 3 * heaven), renk)
                c.doldur(beadPath(x, 234 + 10 * (1 - earth)), renk)
                c.doldur(beadPath(x, 240 + 10 * (1 - earth)), renk)
            }
        }
    }

    /// Soroban boncuğu: iki ucu sivri altıgen.
    func beadPath(_ cx: CGFloat, _ cy: CGFloat) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: cx - 5.5, y: cy))
        p.addLine(to: CGPoint(x: cx - 2, y: cy - 3))
        p.addLine(to: CGPoint(x: cx + 2, y: cy - 3))
        p.addLine(to: CGPoint(x: cx + 5.5, y: cy))
        p.addLine(to: CGPoint(x: cx + 2, y: cy + 3))
        p.addLine(to: CGPoint(x: cx - 2, y: cy + 3))
        p.closeSubpath()
        return p
    }

    // MARK: Süsler

    /// Hâle özgü süsler: Z'ler, düşünce balonu, alkış yıldızları, kalp, parıltı, ✓ balonu.
    func drawExtras(_ c: SobiMaskotTuval, _ e: CGFloat) {
        guard let em = hal else { return }
        let dur: CGFloat = CGFloat(em.sureMs / 1000)
        let fade: CGFloat = X.sinirla(e / 0.2, 0, 1) * X.sinirla((dur - e) / 0.25, 0, 1)
        switch em {
        case .SLEEP:
            if e >= dur - S.sleepWakeS { return }
            for k in 0...2 {
                let p: CGFloat = X.kalan(e / 1.8 + CGFloat(k) / 3, 1)
                let alpha: CGFloat = sin(CGFloat.pi * p) * fade
                c.yazi("Z", 440 + 30 * p, 112 - 60 * p, 12 + 12 * p, R.light, alpha)
            }
        case .THINK:
            popCircle(c, 444, 120, 5, e - 0.4, fade, R.white)
            popCircle(c, 461, 100, 8, e - 0.7, fade, R.white)
            popCircle(c, 488, 63, 22, e - 1.0, fade, R.white)
            let q: CGFloat = M.popScale(e - 1.15)
            if q > 0 { c.yazi("?", 488, 73, 28 * q, R.eye, fade) }
        case .CLAP:
            let phase: CGFloat = X.kalan(e * S.clapHz, 1)
            let alpha: CGFloat = X.sinirla(1 - phase * 2.5, 0, 1) * fade
            if alpha > 0 {
                let s: CGFloat = 2.5 + 4 * phase
                let dy: CGFloat = v(K.frontDy)
                drawStar(c, 368, 190 + dy, s, alpha)
                drawStar(c, 408, 188 + dy, s, alpha)
                drawStar(c, 389, 178 + dy, s * 0.8, alpha)
            }
        case .HEART:
            let s: CGFloat = M.popScale(e - 0.25) * (1 + 0.08 * sin(e * S.tau * 1.8))
            if s > 0 { drawHeart(c, 389, 76 - 4 * e, 17 * s, fade) }
        case .WINK:
            let s: CGFloat = M.popScale(e - 0.15)
            if s > 0 { drawStar(c, 347, 128, 5 * s, fade) }
        case .CHAT:
            drawChatBubbles(c, e)
        case .LISTEN:
            let start: CGFloat = S.listenAnswerS + 0.1
            let s: CGFloat = M.popScale(e - start)
            let alpha: CGFloat = 1 - X.sinirla((e - start - 1.2) / 0.4, 0, 1)
            if s > 0 && alpha > 0 {
                c.kaydet()
                c.tasi(S.listenCheckX, S.listenCheckY)
                c.olcekle(s, s)
                c.daireDoldur(0, 0, 14, R.white, alpha)
                c.daireCiz(0, 0, 14, R.checkGreen, 2, alpha)
                var isaret = Path()
                isaret.move(to: CGPoint(x: -6, y: 0))
                isaret.addLine(to: CGPoint(x: -1.5, y: 4.5))
                isaret.addLine(to: CGPoint(x: 6.5, y: -4.5))
                c.ciz(isaret, R.checkGreen, 3, alpha)
                c.geriAl()
            }
        default:
            return
        }
    }

    private func popCircle(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ e: CGFloat, _ alpha: CGFloat, _ color: UInt32) {
        let s: CGFloat = M.popScale(e)
        if s <= 0 { return }
        let a: CGFloat = X.sinirla(alpha, 0, 1)
        c.daireDoldur(cx, cy, r * s, color, a)
        c.daireCiz(cx, cy, r * s, R.bubbleEdge, 1.6, a)
    }

    /// Dört köşeli parıltı yıldızı.
    func drawStar(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ alpha: CGFloat) {
        if r <= 0 { return }
        let k: CGFloat = 0.15 * r
        var p = Path()
        p.move(to: CGPoint(x: cx, y: cy - r))
        p.addQuadCurve(to: CGPoint(x: cx + r, y: cy), control: CGPoint(x: cx + k, y: cy - k))
        p.addQuadCurve(to: CGPoint(x: cx, y: cy + r), control: CGPoint(x: cx + k, y: cy + k))
        p.addQuadCurve(to: CGPoint(x: cx - r, y: cy), control: CGPoint(x: cx - k, y: cy + k))
        p.addQuadCurve(to: CGPoint(x: cx, y: cy - r), control: CGPoint(x: cx - k, y: cy - k))
        p.closeSubpath()
        c.doldur(p, R.star, X.sinirla(alpha, 0, 1))
    }

    /// Kalp: uygulamanın kalp ikonu (heart_ic), [r] yarı genişlik.
    private func drawHeart(_ c: SobiMaskotTuval, _ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ alpha: CGFloat) {
        c.kaydet()
        c.tasi(cx - r, cy - r)
        let s: CGFloat = 2 * r / S.heartBounds
        c.olcekle(s, s)
        c.cizim(V.kalp, alani: V.kalpAlani, boyut: CGSize(width: S.heartBounds, height: S.heartBounds), X.sinirla(alpha, 0, 1))
        c.geriAl()
    }
}
