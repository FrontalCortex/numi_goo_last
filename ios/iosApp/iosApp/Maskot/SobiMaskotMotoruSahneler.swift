import SwiftUI

private typealias S = SobiMaskotSabit
private typealias K = SobiMaskotKanal
private typealias M = SobiMaskotMotoru

// "Havalı ama tatlı" sahneler (kovboy, karate, ninja, korsan, gözlük, rapçi), Mükemmel ve rehber
// öğretmeni hâlleri: Android BunnyMascotView'daki cowboy/karate/ninja/pirate/perfect/coolGlasses/
// rapper/holdPointer birebir; foto flaşı hesapları da burada.
extension SobiMaskotMotoru {

    // MARK: Kovboy

    func cowboy(_ e: CGFloat) {
        hedef[K.hatA] = 1
        let kalkis: CGFloat = M.smooth01(M.seg(e, S.cbLiftS, S.cbStareS))
        let inis: CGFloat = 1 - M.smooth01(M.seg(e, S.cbTipS, S.cbTipS + 0.5))
        let up: CGFloat = kalkis * inis
        hedef[K.hatDy] = S.cbHatLowFront * (1 - up)
        hedef[K.headPitch] = 1 - up
        hedef[K.headDy] = 3 * (1 - up)
        hedef[K.gazeY] = 2.5 * (1 - up)
        hedef[K.squint] = 1
        agiz = .yanGulus
        hedef[K.holsterA] = (e < S.cbDrawS || e >= S.cbHolsterS) ? 1 : 0
        if e < S.cbDrawS {
            if e >= S.cbStareS && e < S.cbReachS {
                sal[K.gazeX] = 2.2 * sin((e - S.cbStareS) * S.tau / (S.cbReachS - S.cbStareS))
            }
            if e >= S.cbReachS {
                hedef[K.hangR] = S.cbReachDeg * M.smooth01(M.seg(e, S.cbReachS, S.cbDrawS - 0.35))
                if e > S.cbDrawS - 0.45 { sal[K.hangR] = 2.5 * sin(e * S.tau * 9) }
            }
            return
        }
        let armUp: Bool = e < S.cbHolsterS
        hedef[K.hangRA] = armUp ? 0 : 1
        hedef[K.thinkA] = armUp ? 1 : 0
        hedef[K.hcarrotA] = armUp ? 1 : 0
        if e >= S.cbBite2S {
            isirikSayisi = 2
        } else if e >= S.cbBite1S {
            isirikSayisi = 1
        } else {
            isirikSayisi = 0
        }
        if e < S.cbPoseS {
            hedef[K.think] = S.cbDrawUp
            hedef[K.hcarrotRot] = S.cbPistolDeg
            sal[K.hcarrotRot] = 360 * M.smooth01(M.seg(e, S.cbDrawS + 0.15, S.cbDrawS + 0.55))
            hop(e - S.cbDrawS, hops: 1, hopS: 0.3, height: 6)
        } else if e < S.cbBite1S {
            hedef[K.think] = S.thinkUp
            hedef[K.hcarrotRot] = S.cbMouthDeg
        } else if e < S.cbAhemS {
            hedef[K.think] = S.thinkUp - 10
            hedef[K.hcarrotRot] = S.cbMouthDeg
            hedef[K.squint] = 0
            let sinceBite: CGFloat = e >= S.cbBite2S ? e - S.cbBite2S : e - S.cbBite1S
            if sinceBite < 0.14 {
                agiz = .o
                goz = .kapali
                hedef[K.think] = S.thinkUp + 4
            } else {
                agiz = .cigneme
                goz = .mutlu
                hedef[K.cheeks] = 1
                hedef[K.blush] = 0.7
                earsWiggle(e, 1, base: 4)
                sal[K.headDy] = sin(e * S.tau * 4)
            }
        } else {
            hedef[K.think] = e < S.cbDropS ? 70 : S.cbHolsterArm
            hedef[K.hcarrotRot] = e < S.cbDropS ? S.cbPistolDeg + 30 : 90
            hedef[K.cheeks] = 0.25
            hedef[K.blush] = 0.3
        }
    }

    // MARK: Karate

    func karate(_ e: CGFloat) {
        hedef[K.headbandA] = 1
        hedef[K.kboardA] = 1
        if e < S.krStanceS {
            frontHands()
            let p: CGFloat = sin(CGFloat.pi * M.seg(e, 0.1, S.krStanceS - 0.1))
            hedef[K.headDy] = 7 * p
            hedef[K.bodyDy] = 2 * p
            if p > 0.3 { goz = .kapali }
        } else if e < S.krRaiseS {
            karateGuard()
        } else if e < S.krChopS {
            karateGuard()
            hedef[K.catR] = S.krArmHigh
            hedef[K.earL] = -6
            hedef[K.earR] = -6
            agiz = .o
        } else if e < S.krPainS {
            karateGuard()
            hedef[K.catR] = S.krArmHit
            let since: CGFloat = e - S.krChopS
            sal[K.squash] = 0.5 * exp(-since * 10) * cos(since * S.tau * 4)
            agiz = .o
        } else if e < S.krBlowS {
            bothCatArms()
            hedef[K.catL] = 10
            hedef[K.catR] = 40
            sal[K.catR] = 16 * sin(e * S.tau * 9)
            goz = .kapali
            agiz = .uzgun
            hedef[K.earL] = -10
            hedef[K.earR] = -10
            hedef[K.blush] = 0.6
            sal[K.lift] = 2 * abs(sin(e * S.tau * 4.5))
        } else if e < S.krCalmS {
            hedef[K.hangRA] = 0
            hedef[K.thinkA] = 1
            hedef[K.think] = S.thinkUp - 5
            agiz = .o
            hedef[K.blush] = 0.6
            hedef[K.earL] = 20
            hedef[K.earR] = 20
        } else if e < S.krBreakS {
            karateGuard()
            hedef[K.gazeX] = 2.5
            hedef[K.gazeY] = 1.5
        } else if e < S.krBreakS + 0.45 {
            karateGuard()
            hedef[K.squint] = 0
            hedef[K.browsDet] = 0
            hedef[K.eyeScale] = 0.35
            hedef[K.earL] = -8
            hedef[K.earR] = -8
            agiz = .o
            hedef[K.gazeX] = 2.5
            hedef[K.gazeY] = 1.5
        } else {
            frontHands()
            hedef[K.frontDy] = 2
            hedef[K.upperRot] = -4
            hedef[K.headRot] = -6
            hedef[K.legsOpen] = 1
            hedef[K.squint] = 1
            agiz = .yanGulus
        }
    }

    private func karateGuard() {
        bothCatArms()
        hedef[K.catL] = 22
        hedef[K.catR] = 30
        hedef[K.legsOpen] = 1
        hedef[K.bodyDy] = 4
        hedef[K.squint] = 1
        hedef[K.browsDet] = 1
    }

    // MARK: Ninja

    func ninja(_ e: CGFloat) {
        hedef[K.maskA] = 1
        hedef[K.squint] = 1
        if e < S.njSignS {
            sal[K.bodyDx] = -S.skateOffX * (1 - M.smooth01(M.seg(e, 0, S.njSignS - 0.1)))
            hop(e, hops: 4, hopS: (S.njSignS - 0.1) / 4, height: 5)
            frontHands()
            hedef[K.frontDy] = -8
            hedef[K.bodyDy] = 3
            sal[K.gazeX] = 2.2 * sin(e * S.tau * 1.5)
        } else if e < S.njThrowS {
            ninjaSign()
            hedef[K.browsDet] = 1
            hedef[K.gazeX] = e < (S.njSignS + S.njThrowS) / 2 ? -2.5 : 2.5
        } else if e < S.njSmokeS {
            hedef[K.hangRA] = 0
            hedef[K.catRA] = 1
            hedef[K.catR] = e < S.njThrowS + 0.12 ? 55 : -25
            hedef[K.browsDet] = 1
            hedef[K.bodyDy] = 3
        } else if e < S.njPeekS {
            ninjaSign()
            goz = .kapali
            hedef[K.headRot] = -5
        } else if e < S.njHoldS {
            ninjaSign()
            hedef[K.browsDet] = 1
            sneakyLook(-1, M.smooth01(M.seg(e, S.njPeekS, S.njHoldS)))
        } else {
            ninjaSign()
            hedef[K.browsDet] = 1
            let k: Int = Int(floor((e - S.njHoldS) / S.njGlanceS))
            let dir: CGFloat = k % 2 == 0 ? 1 : -1
            let local: CGFloat = e - S.njHoldS - CGFloat(k) * S.njGlanceS
            let p: CGFloat = M.smooth01(M.seg(local, 0, S.njSlideS))
            sneakyLook(-dir + 2 * dir * p, 1)
        }
    }

    private func sneakyLook(_ side: CGFloat, _ amount: CGFloat) {
        sal[K.gazeX] = 2.5 * side * amount
        sal[K.headRot] = 3.5 * side * amount
        sal[K.upperRot] = 1.5 * side * amount
        sal[K.bodyDx] = 3 * side * amount
    }

    private func ninjaSign() {
        frontHands()
        hedef[K.frontDy] = -14
        hedef[K.frontGap] = -3
        hedef[K.bodyDy] = 4
        hedef[K.legsOpen] = 1
    }

    // MARK: Korsan

    func pirate(_ e: CGFloat) {
        hedef[K.hatA] = 1
        hedef[K.earL] = S.prEar
        hedef[K.earR] = S.prEar
        sal[K.tilt] = 3 * sin(e * S.tau / S.prSwayS)
        sal[K.upperRot] = 2 * sin(e * S.tau / S.prSwayS + 0.8)
        hedef[K.squint] = 0.6
        agiz = .yanGulus
        durbunCevirme = M.smooth01(M.seg(e, S.prFlipS, S.prFlipS + 0.3))
        let held: Bool = e >= S.prRaiseS
        hedef[K.hangRA] = held ? 0 : 1
        hedef[K.thinkA] = held ? 1 : 0
        hedef[K.spyglassA] = held ? 1 : 0
        hedef[K.think] = S.thinkUp
        mercekKirpma = 1
        if e < S.prRaiseS {
            return
        } else if e < S.prPuzzleS {
            hedef[K.squint] = 0
            agiz = .bekleme
        } else if e < S.prLook2S {
            hedef[K.think] = S.prLower
            hedef[K.headRot] = 9
            hedef[K.squint] = 0
            hedef[K.gazeX] = 2
            hedef[K.gazeY] = 2
            agiz = .o
        } else if e < S.prHoldS {
            hedef[K.squint] = 0
            agiz = e > S.prLook2S + 0.4 ? .o : .bekleme
        } else {
            hedef[K.squint] = 0
            let b: CGFloat = (e - S.prHoldS - S.prBlinkAtS) / 0.15
            if b >= 0 && b <= 1 { mercekKirpma = 1 - 0.9 * sin(CGFloat.pi * b) }
        }
    }

    // MARK: Mükemmel ve foto flaşı

    func perfect(_ e: CGFloat) {
        let glassesS: CGFloat = S.pfCrownS + S.finaleGlassesDelayS
        let poseS: CGFloat = glassesS + S.adDropS + 0.1
        if e >= S.pfCrownS { hedef[K.crownA] = 1 }
        if e >= glassesS { hedef[K.glassesA] = 1 }
        tacDususu = M.dropOffset(e - S.pfCrownS)
        gozlukDususu = M.dropOffset(e - glassesS)
        gozlukParilti = e - glassesS - S.adDropS - 0.1
        let b1: CGFloat = M.landBump(e - S.pfCrownS - S.adDropS)
        let b2: CGFloat = M.landBump(e - glassesS - S.adDropS)
        sal[K.headDy] += b1 + b2
        if e < poseS {
            hedef[K.gazeY] = -2.5
            hedef[K.headRot] = -3
            agiz = .yanGulus
        } else {
            hedef[K.hangRA] = 0
            hedef[K.peaceA] = 1
            sal[K.peace] = 6 * sin((e - poseS) * S.tau * 1.5)
            hedef[K.headRot] = 6
            hop(e - poseS, hops: 1, hopS: 0.35, height: 8)
            agiz = .mutlu
        }
    }

    private func flashStartS() -> CGFloat {
        if hal == .PERFECT { return S.pfFlashS }
        if hal == .CROWN { return 0 }
        return -1
    }

    private func sinceLastFlash(_ e: CGFloat) -> CGFloat {
        let start: CGFloat = flashStartS()
        if start < 0 || e < start { return -1 }
        let u: CGFloat = SobiMaskotMat.kalan(e - start, S.pfFlashLoopS)
        var best: CGFloat = -1
        for ft in S.flashT where u >= ft {
            best = u - ft
        }
        return best
    }

    /// Son flaş soldan mı geldi (sırayla sol, sağ, sol …).
    func lastFlashFromLeft(_ e: CGFloat) -> Bool {
        let start: CGFloat = flashStartS()
        if start < 0 || e < start { return true }
        let u: CGFloat = SobiMaskotMat.kalan(e - start, S.pfFlashLoopS)
        var k: Int = 0
        for i in 0..<S.flashT.count where u >= S.flashT[i] {
            k = i
        }
        return k % 2 == 0
    }

    /// Foto flaşının şiddeti 0..1.
    func photoFlashTint(_ e: CGFloat) -> CGFloat {
        let since: CGFloat = sinceLastFlash(e)
        if since < 0 || since > S.flashDurS { return 0 }
        let q: CGFloat = 1 - since / S.flashDurS
        return q * q
    }

    // MARK: Gözlük

    func coolGlasses(_ e: CGFloat) {
        if e >= S.glDropS { hedef[K.glassesA] = 1 }
        let kayma: CGFloat = M.smooth01(M.seg(e, S.glSlipS, S.glSlipS + 0.5))
        let duzelme: CGFloat = 1 - M.smooth01(M.seg(e, S.glFixS, S.glFixS + 0.12))
        let slip: CGFloat = S.glSlip * kayma * duzelme
        let dusus: CGFloat = M.dropOffset(e - S.glDropS)
        gozlukDususu = dusus + slip + 2.5 * M.landBump(e - S.glFixS - 0.12)
        gozlukParilti = e >= S.glFixS ? e - S.glFixS - 0.15 : e - S.glDropS - S.adDropS - 0.1
        sal[K.headDy] += M.landBump(e - S.glDropS - S.adDropS)
        agiz = .yanGulus
        if e < S.glDropS + S.adDropS {
            hedef[K.squint] = 1
            hedef[K.gazeY] = -2.5
            return
        }
        frontHands()
        hedef[K.frontDy] = 2
        hedef[K.upperRot] = -4
        hedef[K.headRot] = -6
        hedef[K.legsOpen] = 1
        if e < S.glSlipS + 0.25 {
            return
        } else if e < S.glFixS {
            hedef[K.eyeScale] = 0.2
            hedef[K.gazeY] = -1.5
            hedef[K.blush] = 0.8
            hedef[K.earL] = 15
            hedef[K.earR] = 15
            hedef[K.headRot] = -2
            agiz = .o
        } else if e < S.glFixS + 0.3 {
            let p: CGFloat = M.seg(e, S.glFixS, S.glFixS + 0.15)
            sal[K.headDy] += -5 * sin(CGFloat.pi * p)
            hedef[K.blush] = 0.4
        } else {
            sal[K.headRot] = 1.5 * sin(e * S.tau / S.glNodS)
        }
    }

    // MARK: Rapçi

    func rapper(_ e: CGFloat) {
        hedef[K.rapA] = 1
        hedef[K.squint] = 1
        hedef[K.legsOpen] = 1
        agiz = .yanGulus
        let beat: CGFloat = SobiMaskotMat.kalan(e, S.rpBeatS) / S.rpBeatS
        let nod: CGFloat = sin(CGFloat.pi * beat)
        let big: CGFloat = (e >= S.rpOffS - S.rpBeatS && e < S.rpOffS) ? 1.8 : 1
        sal[K.headDy] = 3.5 * nod * big
        sal[K.headRot] = 2 * nod * big
        sal[K.bodyDy] = 1.5 * nod
        if e < S.rpOffS || e >= S.rpRecrossS {
            frontHands()
            hedef[K.frontDy] = S.rpArmsDy
            return
        }
        hedef[K.hangRA] = 0
        hedef[K.catRA] = 1
        if e < S.rpCatchS {
            hedef[K.catR] = S.rpCatchUp
            hedef[K.squint] = 0
            hedef[K.eyeScale] = 0.3
            hedef[K.gazeX] = 2
            hedef[K.gazeY] = 2
            agiz = .o
        } else if e < S.rpTossS {
            hedef[K.catR] = 50
            hedef[K.squint] = 0.5
            hedef[K.gazeX] = 2
            hedef[K.gazeY] = -1
        } else if e < S.rpOnS {
            hedef[K.catR] = 95
            hedef[K.gazeY] = -2.5
        } else {
            hedef[K.catR] = 40
        }
    }

    /// Rapçinin kepi başta mı.
    func rapHatOnHead(_ e: CGFloat) -> Bool {
        return e < S.rpOffS || e >= S.rpOnS
    }

    // MARK: Rehber öğretmeni (elde havuç)

    private func holdPointer(_ armDeg: CGFloat, _ tipDeg: CGFloat, _ e: CGFloat, _ swayPeriodS: CGFloat) {
        hedef[K.hangRA] = 0
        hedef[K.thinkA] = 1
        hedef[K.hcarrotA] = 1
        hedef[K.think] = armDeg
        hedef[K.hcarrotRot] = tipDeg
        let sway: CGFloat = sin(e * S.tau / swayPeriodS)
        sal[K.hcarrotRot] = S.teachSwayDeg * sway
        sal[K.headRot] = 2 * sway
        agiz = .mutlu
    }

    /// Anlatma: havuç yukarıda, sağa sola sallanıyor.
    func teachTalk(_ e: CGFloat) {
        holdPointer(S.tcArm, S.tcTip, e, 1.5)
    }

    /// Gösterme: kol öne uzanık, havucun ucu aşağıda.
    func teachPoint(_ e: CGFloat) {
        holdPointer(S.tpArm, S.tpTip, e, 2.4)
        hedef[K.gazeX] = 2
        hedef[K.gazeY] = 2.5
    }

    /// Uyarı, güler yüzle: havuç dik, baş hafif yana yatık.
    func teachWarn(_ e: CGFloat) {
        holdPointer(S.twArm, S.twTip, e, 2.4)
        hedef[K.headRot] = 5
    }
}
