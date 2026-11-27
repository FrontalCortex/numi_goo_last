import SwiftUI

private typealias S = SobiMaskotSabit
private typealias K = SobiMaskotKanal
private typealias M = SobiMaskotMotoru

// Hâllerin duruşları: Android BunnyMascotView.applyEmote birebir (yorumlar oradan kısaltıldı).
// Büyük `switch` derleyiciyi zorlamasın diye hâller gruplara bölündü.
extension SobiMaskotMotoru {
    func haliUygula(_ em: SobiMaskotHal, _ e: CGFloat, _ suan: Double) {
        let dur: CGFloat = CGFloat(em.sureMs / 1000)
        // Salınımların zarfı: başta 0,15 sn'de girip sonda 0,2 sn'de çıkıyor.
        let giris: CGFloat = SobiMaskotMat.sinirla(e / 0.15, 0, 1)
        let cikis: CGFloat = SobiMaskotMat.sinirla((dur - e) / 0.2, 0, 1)
        let env: CGFloat = giris * cikis
        switch em {
        case .GREET, .CHEER, .BALLOON, .SLEEP, .POINT, .PEACE, .SAD, .DANCE:
            temelHaller1(em, e, dur, env)
        case .THINK, .CLAP, .SURPRISED, .SHY, .WINK, .HEART, .ABACUS, .CHAT:
            temelHaller2(em, e, dur, env)
        case .FLAME, .ALARM, .CALENDAR, .JETPACK, .HERO, .CROWN:
            seriVeProHaller(em, e, env, suan)
        case .AD_JUMP, .AD_BUTTON, .AD_ZAP, .AD_MAGIC, .AD_PUSH:
            reklamHalleri(em, e, env)
        case .BOARD, .LISTEN, .NOTEBOOK, .NOTEBOOK_HOLD, .NOTEBOOK_WRITE:
            bilgiHalleri(em, e, env)
        case .WEIGHTS, .SKATE, .GUITAR, .JUGGLE, .STARS, .DETERMINED:
            dersSonuHalleri(em, e, env)
        case .PERFECT: perfect(e)
        case .COWBOY_FRONT: cowboy(e)
        case .KARATE: karate(e)
        case .NINJA: ninja(e)
        case .PIRATE: pirate(e)
        case .GLASSES: coolGlasses(e)
        case .RAPPER: rapper(e)
        case .TEACH_TALK: teachTalk(e)
        case .TEACH_POINT: teachPoint(e)
        case .TEACH_WARN: teachWarn(e)
        }
    }

    // MARK: Selam … dans

    private func temelHaller1(_ em: SobiMaskotHal, _ e: CGFloat, _ dur: CGFloat, _ env: CGFloat) {
        let tau: CGFloat = S.tau
        switch em {
        case .GREET:
            hedef[K.hangRA] = 0
            hedef[K.catRA] = 1
            hedef[K.catR] = S.waveUp
            sal[K.catR] = 12 * sin(e * tau * 2.5) * env
            hedef[K.legsOpen] = 1
            sal[K.upperRot] = 3 * sin(e * tau * 1.25) * env
            sal[K.headRot] = 3 * sin(e * tau * 1.25 - 0.9) * env
            sal[K.hangL] = 7 * sin(e * tau * 1.25 + 0.6) * env
            agiz = .mutlu
        case .CHEER:
            bothCatArms()
            let flap: CGFloat = 6 * sin(e * tau * 3) * env
            sal[K.catL] = flap
            sal[K.catR] = flap
            earsWiggle(e, env, base: 12)
            hop(e, hops: 2, hopS: S.cheerHopS, height: S.jumpHeight)
            let bend: CGFloat = sin(CGFloat.pi * e / dur)
            sal[K.upperRot] = 4 * bend * sin(e * tau * 2.2)
            sal[K.headRot] = 1.5 * bend * sin(e * tau * 2.2 - 1.2)
            goz = .mutlu
            agiz = .mutlu
        case .BALLOON:
            hedef[K.hangRA] = 0
            hedef[K.catRA] = 1
            hedef[K.catR] = 25
            hedef[K.balloonA] = 1
            hedef[K.balloonSway] = 12
            sal[K.balloonSway] = 8 * sin(e * tau / 1.6) * env
            hedef[K.gazeX] = 1.5
            hedef[K.gazeY] = -2
            hedef[K.headRot] = 4
            sal[K.lift] = 4 * abs(sin(e * tau * 0.9)) * env
            agiz = .mutlu
        case .SLEEP:
            if e < dur - S.sleepWakeS {
                hedef[K.earL] = 30
                hedef[K.earR] = 30
                hedef[K.headRot] = 9
                hedef[K.headDy] = 4
                hedef[K.bodyDy] = 2
                hedef[K.hangL] = -4
                hedef[K.hangR] = -4
                sal[K.bodyDy] = 2.5 * sin(e * tau / 2.2) * env
                goz = .kapali
            } else {
                hedef[K.eyeScale] = 0.2
            }
        case .POINT:
            let side: CGFloat = noktaSola ? -1 : 1
            if noktaSola {
                hedef[K.hangLA] = 0
                hedef[K.catLA] = 1
                hedef[K.catL] = S.pointUpL
                sal[K.catL] = 4 * sin(e * tau * 2) * env
                hedef[K.hangR] = 6
            } else {
                hedef[K.hangRA] = 0
                hedef[K.catRA] = 1
                hedef[K.catR] = S.pointUpR
                sal[K.catR] = 4 * sin(e * tau * 2) * env
                hedef[K.hangL] = 6
            }
            hedef[K.upperRot] = 5 * side
            hedef[K.headRot] = 4 * side
            hedef[K.gazeX] = 2.5 * side
            if e < 1.6 { agiz = .konusma }
        case .PEACE:
            hedef[K.hangRA] = 0
            hedef[K.peaceA] = 1
            sal[K.peace] = 6 * sin(e * tau * 1.5) * env
            hedef[K.headRot] = 6
            hop(e, hops: 1, hopS: 0.35, height: 10)
            goz = .kirpma
            agiz = .mutlu
        case .SAD:
            uzgunDurus(e, env)
        default:
            // DANCE
            bothCatArms()
            let w: CGFloat = e * tau
            sal[K.catL] = 25 * sin(w * 2) * env
            sal[K.catR] = -25 * sin(w * 2) * env
            sal[K.bodyDx] = 7 * sin(w) * env
            sal[K.lift] = 7 * abs(cos(w)) * env
            sal[K.upperRot] = 6 * sin(w) * env
            sal[K.headRot] = -3 * sin(w) * env
            sal[K.earL] = 10 * sin(w) * env
            sal[K.earR] = -10 * sin(w) * env
            goz = .mutlu
            agiz = .mutlu
        }
    }

    /// Üzgün duruş (SAD ve DETERMINED'in ilk bölümü).
    func uzgunDurus(_ e: CGFloat, _ env: CGFloat) {
        hedef[K.earL] = 35
        hedef[K.earR] = 35
        hedef[K.headDy] = 4
        hedef[K.headRot] = 3
        hedef[K.bodyDy] = 2
        hedef[K.gazeY] = 2
        hedef[K.brows] = 1
        hedef[K.hangL] = -3
        hedef[K.hangR] = -3
        sal[K.bodyDy] = 1.2 * sin(e * S.tau / 1.6) * env
        agiz = .uzgun
    }

    // MARK: Düşünme … mesajlaşma

    private func temelHaller2(_ em: SobiMaskotHal, _ e: CGFloat, _ dur: CGFloat, _ env: CGFloat) {
        let tau: CGFloat = S.tau
        switch em {
        case .THINK:
            hedef[K.hangRA] = 0
            hedef[K.thinkA] = 1
            sal[K.think] = 3 * sin(e * tau * 2.5) * env
            hedef[K.gazeX] = 1.5
            hedef[K.gazeY] = -2
            hedef[K.headRot] = 6
        case .CLAP:
            frontHands()
            hedef[K.frontDy] = 4
            sal[K.frontGap] = 7 * (0.5 - 0.5 * cos(e * tau * S.clapHz)) * env
            sal[K.bodyDy] = -1.5 * abs(sin(e * tau * S.clapHz / 2)) * env
            earsWiggle(e, env, base: 0)
            goz = .mutlu
            agiz = .mutlu
        case .SURPRISED:
            bothCatArms()
            hedef[K.catL] = 55
            hedef[K.catR] = 40
            hedef[K.eyeScale] = 0.35
            hedef[K.earL] = -10
            hedef[K.earR] = -10
            sal[K.earL] = 3 * sin(e * tau * 8) * env
            sal[K.earR] = -3 * sin(e * tau * 8) * env
            hedef[K.headDy] = -3
            hop(e, hops: 1, hopS: 0.35, height: 12)
            agiz = .o
        case .SHY:
            frontHands()
            hedef[K.frontDy] = 6
            sal[K.frontRot] = 3 * sin(e * tau * 1.5) * env
            hedef[K.blush] = 1
            hedef[K.gazeX] = -2
            hedef[K.gazeY] = 1.5
            hedef[K.headRot] = -7
            hedef[K.headDy] = 2
            hedef[K.earL] = 12
            hedef[K.earR] = 12
            sal[K.upperRot] = 2 * sin(e * tau * 0.8) * env
        case .WINK:
            hedef[K.headRot] = 8
            hedef[K.upperRot] = 3
            hedef[K.hangL] = 6
            hedef[K.hangR] = 6
            goz = .kirpma
            agiz = .mutlu
        case .HEART:
            frontHands()
            hedef[K.frontDy] = 4
            hedef[K.blush] = 0.7
            hedef[K.headRot] = 5
            sal[K.upperRot] = 2 * sin(e * tau * 0.9) * env
            goz = .mutlu
        case .ABACUS:
            frontHands()
            hedef[K.frontGap] = 22
            hedef[K.frontDy] = 10
            hedef[K.abacusA] = 1
            if e < dur - 0.8 {
                hedef[K.gazeY] = 2.5
                hedef[K.headDy] = 2
                hedef[K.headRot] = -3
            } else {
                goz = .mutlu
                agiz = .mutlu
            }
        default:
            // CHAT
            frontHands()
            hedef[K.frontDy] = 6
            hedef[K.phoneA] = 1
            if e < S.chatWaitS || e >= S.chatFadeS {
                hedef[K.gazeY] = 2.5
                hedef[K.headDy] = 2
                hedef[K.headRot] = -3
                sal[K.frontRot] = 2.5 * sin(e * tau * 6) * env
            } else if e < S.chatAnswerS {
                hedef[K.gazeX] = 2
                hedef[K.gazeY] = -1
                hedef[K.headRot] = 4
            } else {
                hedef[K.gazeX] = 1.5
                hedef[K.gazeY] = -1
                hedef[K.headRot] = 4
                hop(e - S.chatAnswerS - 0.1, hops: 1, hopS: 0.35, height: 9)
                let a: CGFloat = SobiMaskotMat.sinirla((e - S.chatAnswerS) / 0.15, 0, 1)
                let b: CGFloat = SobiMaskotMat.sinirla((S.chatFadeS - e) / 0.2, 0, 1)
                earsWiggle(e, a * b, base: 6)
                goz = .mutlu
                agiz = .mutlu
            }
        }
    }

    // MARK: Seri ve Pro hâlleri

    private func seriVeProHaller(_ em: SobiMaskotHal, _ e: CGFloat, _ env: CGFloat, _ suan: Double) {
        let tau: CGFloat = S.tau
        switch em {
        case .FLAME:
            hedef[K.hangRA] = 0
            hedef[K.catRA] = 1
            hedef[K.catR] = 25
            hedef[K.flameA] = 1
            hedef[K.flameH] = M.alevBoyu(flameDays(e))
            hedef[K.gazeX] = 2
            hedef[K.gazeY] = -1.5
            hedef[K.headRot] = 4
            let age: CGFloat = flameReactAge(e, suan)
            if age < S.flameReactS {
                hop(age, hops: 1, hopS: 0.35, height: 8)
                earsWiggle(e, 1 - age / S.flameReactS, base: 6)
                goz = .mutlu
                agiz = .mutlu
            }
        case .ALARM:
            frontHands()
            hedef[K.frontGap] = 14
            hedef[K.frontDy] = 14
            hedef[K.alarmA] = 1
            let age: CGFloat = alarmRingAge(e, suan)
            let ring: CGFloat = M.alarmRingEnv(age)
            if ring > 0.01 {
                hop(age, hops: 1, hopS: 0.3, height: 7)
                earsWiggle(e, ring, base: 8)
                sal[K.frontRot] = 2.5 * sin(e * tau * S.alarmShakeHz) * ring
                goz = .mutlu
                agiz = .mutlu
            } else {
                hedef[K.gazeY] = 2.5
                hedef[K.headDy] = 2
                hedef[K.headRot] = -3
            }
        case .CALENDAR:
            frontHands()
            hedef[K.frontGap] = 28
            hedef[K.frontDy] = 10
            hedef[K.calA] = 1
            let n: Int = calendarDays()
            let done: CGFloat = M.calendarDoneS(n)
            if n <= 0 || e < done || e >= S.calResetS {
                hedef[K.gazeY] = 2.5
                hedef[K.headDy] = 2
                hedef[K.headRot] = -3
                let since: CGFloat = e - S.calFirstTickS
                if n > 0 && since >= 0 && e < done {
                    let q: CGFloat = SobiMaskotMat.kalan(since, S.calTickS) / S.calTickS
                    sal[K.headDy] = 1.5 * sin(CGFloat.pi * q)
                }
            } else {
                hedef[K.gazeY] = -1
                hop(e - done, hops: 1, hopS: 0.35, height: 9)
                let a: CGFloat = SobiMaskotMat.sinirla((e - done) / 0.15, 0, 1)
                let b: CGFloat = SobiMaskotMat.sinirla((S.calResetS - e) / 0.2, 0, 1)
                earsWiggle(e, a * b, base: 6)
                goz = .mutlu
                agiz = .mutlu
            }
        case .JETPACK:
            bothCatArms()
            hedef[K.catL] = 20
            hedef[K.catR] = 10
            sal[K.catL] = 5 * sin(e * tau * 2) * env
            sal[K.catR] = 5 * sin(e * tau * 2) * env
            hedef[K.lift] = 14
            sal[K.lift] = 4 * sin(e * tau / 1.6) * env
            hedef[K.legsOpen] = 0.6
            sal[K.legsOpen] = 0.4 * sin(e * tau * 1.25) * env
            hedef[K.earL] = 8
            hedef[K.earR] = 8
            sal[K.earL] = 5 * sin(e * tau * 3) * env
            sal[K.earR] = 5 * sin(e * tau * 3 + 1) * env
            sal[K.upperRot] = 3 * sin(e * tau / 1.6 + 1) * env
            hedef[K.jetA] = 1
            agiz = .mutlu
        case .HERO:
            hedef[K.hangRA] = 0
            hedef[K.catRA] = 1
            hedef[K.catR] = 45
            sal[K.catR] = 4 * sin(e * tau * 1.5) * env
            hedef[K.hangL] = 12
            hedef[K.legsOpen] = 1
            hedef[K.capeA] = 1
            hedef[K.headRot] = -3
            hedef[K.earL] = 4
            hedef[K.earR] = 4
            sal[K.earL] = 4 * sin(e * tau * 2.5) * env
            sal[K.earR] = 4 * sin(e * tau * 2.5 + 0.8) * env
            agiz = .mutlu
        default:
            // CROWN
            hedef[K.crownA] = 1
            hedef[K.glassesA] = 1
            hedef[K.hangRA] = 0
            hedef[K.peaceA] = 1
            sal[K.peace] = 6 * sin(e * tau * 1.5)
            hedef[K.headRot] = 6
            agiz = .mutlu
        }
    }

    // MARK: Reklam hâlleri

    private func reklamHalleri(_ em: SobiMaskotHal, _ e: CGFloat, _ env: CGFloat) {
        let tau: CGFloat = S.tau
        switch em {
        case .AD_JUMP:
            let tvX: CGFloat = M.adTvX(e)
            let bakis: CGFloat = SobiMaskotMat.sinirla((tvX - 395) / 60, -2.5, 2.5)
            if e < S.adJumpHopS {
                hedef[K.gazeX] = bakis
                if e > S.adJumpHopS - 0.15 { hedef[K.bodyDy] = 3 }
            } else if e < S.adJumpHopS + S.adJumpHopLenS + 0.2 {
                bothCatArms()
                hedef[K.catL] = 40
                hedef[K.catR] = 30
                hedef[K.earL] = -6
                hedef[K.earR] = -6
                hedef[K.eyeScale] = 0.15
                hedef[K.gazeY] = 2
                hedef[K.gazeX] = bakis
                agiz = .mutlu
            } else if e < S.adJumpCheerS {
                hedef[K.gazeX] = bakis
            } else if e < S.adJumpCheerS + 0.8 {
                bothCatArms()
                let flap: CGFloat = 6 * sin(e * tau * 3)
                sal[K.catL] = flap
                sal[K.catR] = flap
                let a: CGFloat = SobiMaskotMat.sinirla((e - S.adJumpCheerS) / 0.15, 0, 1)
                let b: CGFloat = SobiMaskotMat.sinirla((S.adJumpCheerS + 0.8 - e) / 0.2, 0, 1)
                earsWiggle(e, a * b, base: 10)
                goz = .mutlu
                agiz = .mutlu
            }
            hop(e - S.adJumpHopS, hops: 1, hopS: S.adJumpHopLenS, height: S.adJumpHeight)
        case .AD_BUTTON:
            if e < S.adBtnReadyS {
                hedef[K.gazeX] = 2.5
            } else if e < S.adBtnPressS + 0.35 {
                hedef[K.gazeX] = 2.5
                hedef[K.hangRA] = 0
                hedef[K.catRA] = 1
                hedef[K.catR] = e < S.adBtnPressS ? 20 : -5
                if e < S.adBtnPressS { sal[K.catR] = 3 * sin(e * tau * 3) * env }
                hedef[K.upperRot] = 4
            }
            coolFinale(e, crownS: S.adBtnCrownS, fadeS: S.adBtnFadeS)
        case .AD_ZAP, .AD_MAGIC:
            let zap: Bool = em == .AD_ZAP
            let hitS: CGFloat = zap ? S.zapStrikeS : S.magicStampS
            let crownS: CGFloat = zap ? S.zapCrownS : S.magicCrownS
            if e < S.wandUpS {
                hedef[K.gazeX] = 2.5
            } else if e < hitS + 0.5 {
                hedef[K.gazeX] = 2.5
                hedef[K.hangRA] = 0
                hedef[K.catRA] = 1
                hedef[K.wandA] = 1
                if e < S.wandWaveEndS {
                    hedef[K.catR] = S.wandArmUp
                    sal[K.catR] = M.wandWave(e)
                    sal[K.upperRot] = 2 * sin((e - S.wandUpS) * tau * S.wandHz + 1)
                    if e > S.wandUpS + 0.15 { agiz = .konusma }
                } else {
                    hedef[K.catR] = S.wandPointUp
                }
                if e >= hitS && e < hitS + 0.4 { hedef[K.eyeScale] = 0.25 }
            }
            coolFinale(e, crownS: crownS, fadeS: zap ? S.zapFadeS : S.magicFadeS)
        default:
            // AD_PUSH
            if e < S.adPushStartS {
                hedef[K.gazeX] = 2
                if e > 0.4 {
                    hedef[K.hangRA] = 0
                    hedef[K.catRA] = 1
                    hedef[K.catR] = S.pointUpR
                }
            } else if e < S.adPushEndS {
                hedef[K.hangRA] = 0
                hedef[K.catRA] = 1
                hedef[K.catR] = -12
                hedef[K.upperRot] = 8
                hedef[K.bodyDx] = 8
                hedef[K.legsOpen] = 1
                hedef[K.headRot] = 5
                sal[K.upperRot] = 1.5 * sin(e * tau * 6) * env
                goz = .mutlu
                agiz = .o
            } else if e < S.adPushEndS + 0.8 {
                frontHands()
                hedef[K.frontDy] = 4
                sal[K.frontGap] = 6 * (0.5 - 0.5 * cos((e - S.adPushEndS) * tau * S.clapHz)) * env
                goz = .mutlu
                agiz = .mutlu
            }
        }
    }

    /// Reklam hâllerinin ortak finali: taç, ardından gözlük düşüyor; havalı poz.
    func coolFinale(_ e: CGFloat, crownS: CGFloat, fadeS: CGFloat) {
        let glassesS: CGFloat = crownS + S.finaleGlassesDelayS
        let poseS: CGFloat = glassesS + S.adDropS + 0.1
        if e >= crownS && e < poseS {
            hedef[K.gazeY] = -2.5
            hedef[K.headRot] = -3
        } else if e >= poseS && e < fadeS {
            frontHands()
            hedef[K.frontDy] = 2
            hedef[K.upperRot] = -4
            hedef[K.headRot] = -7
            hedef[K.legsOpen] = 1
            sal[K.headRot] = 1.5 * sin((e - poseS) * S.tau * 0.7)
        }
        if e >= crownS && e < fadeS { hedef[K.crownA] = 1 }
        if e >= glassesS && e < fadeS { hedef[K.glassesA] = 1 }
        tacDususu = M.dropOffset(e - crownS)
        gozlukDususu = M.dropOffset(e - glassesS)
        gozlukParilti = e - glassesS - S.adDropS - 0.1
        let b1: CGFloat = M.landBump(e - crownS - S.adDropS)
        let b2: CGFloat = M.landBump(e - glassesS - S.adDropS)
        sal[K.headDy] += b1 + b2
    }

    // MARK: Bilgi sorusu hâlleri

    private func bilgiHalleri(_ em: SobiMaskotHal, _ e: CGFloat, _ env: CGFloat) {
        let tau: CGFloat = S.tau
        switch em {
        case .BOARD:
            hedef[K.bodyDx] = -14
            hedef[K.hangRA] = 0
            hedef[K.catRA] = 1
            hedef[K.catR] = -2
            hedef[K.boardA] = 1
            if e < S.boardWriteS {
                sal[K.catR] = 4 * sin(e * tau * 2.5) * env
            } else if e < S.boardDoneS + 0.4 {
                hedef[K.gazeX] = 2.5
                hedef[K.gazeY] = 0.5
                sal[K.catR] = 3 * sin(e * tau * 6) * env
                if e >= S.boardDoneS {
                    sal[K.headDy] = 2.5 * sin(CGFloat.pi * ((e - S.boardDoneS) / 0.4))
                }
            } else if e < S.boardEraseS {
                hop(e - S.boardDoneS - 0.5, hops: 1, hopS: 0.35, height: 8)
                goz = .mutlu
                agiz = .mutlu
            }
        case .LISTEN:
            if e < S.listenAnswerS {
                hedef[K.hangRA] = 0
                hedef[K.thinkA] = 1
                hedef[K.think] = S.listenHandUp
                hedef[K.earR] = 26
                sal[K.earR] = 3 * sin(e * tau * 2) * env
                hedef[K.headRot] = 10
                hedef[K.gazeX] = 2
                hedef[K.gazeY] = -0.5
                sal[K.upperRot] = 1.5 * sin(e * tau * 0.8) * env
            } else if e < S.listenAnswerS + 1.6 {
                let a: CGFloat = e - S.listenAnswerS
                if a < 0.4 {
                    hedef[K.hangRA] = 0
                    hedef[K.thinkA] = 1
                    hedef[K.think] = S.listenHandUp
                }
                hedef[K.earR] = -8
                hedef[K.earL] = -4
                if a < 0.8 { sal[K.headDy] = 2.5 * abs(sin(a * tau * 1.25)) }
                if a > 0.2 {
                    goz = .mutlu
                    agiz = .mutlu
                }
            }
        case .NOTEBOOK:
            notebookHands(notebookPenTip(e))
            if e < S.noteLine2EndS + 0.1 {
                notebookLookDown()
            } else if e < S.noteEraseS {
                hedef[K.gazeY] = -0.5
                hop(e - S.noteLine2EndS - 0.2, hops: 1, hopS: 0.35, height: 7)
                goz = .mutlu
                agiz = .mutlu
            }
        case .NOTEBOOK_HOLD:
            notebookHands(notebookPenTip(0))
            sal[K.frontRY] = 1.5 * sin(e * tau * 0.8) * env
            hedef[K.gazeY] = 0.5
        default:
            // NOTEBOOK_WRITE
            let w: CGFloat = e + S.noteWriteOffsetS
            notebookHands(notebookPenTip(w))
            if w < S.noteLine2EndS + 0.05 {
                notebookLookDown()
            } else {
                let q: CGFloat = SobiMaskotMat.sinirla((w - S.noteLine2EndS) / 0.35, 0, 1)
                sal[K.headDy] = 2 * sin(CGFloat.pi * q)
                agiz = .mutlu
            }
        }
    }

    /// Havuç kalem hâllerinin ortak duruşu.
    private func notebookHands(_ tip: CGPoint) {
        frontHands()
        hedef[K.frontDy] = 0
        hedef[K.frontLX] = -14
        hedef[K.noteA] = 1
        hedef[K.frontRX] = tip.x + S.notePenHandDx - S.frontRPawX
        hedef[K.frontRY] = tip.y + S.notePenHandDy - S.frontRPawY
    }

    private func notebookLookDown() {
        hedef[K.gazeY] = 2.5
        hedef[K.headDy] = 2
        hedef[K.headRot] = -3
    }

    /// Havuç kalemin ucunun olması gereken yer (tur içinde).
    private func notebookPenTip(_ e: CGFloat) -> CGPoint {
        let x0: CGFloat = S.noteTextX
        let l0: CGFloat = S.noteLines[0]
        let l1: CGFloat = S.noteLines[1]
        if e < S.noteLine1S {
            return CGPoint(x: x0, y: l0 - 4)
        } else if e < S.noteLine1EndS {
            let q: CGFloat = (e - S.noteLine1S) / (S.noteLine1EndS - S.noteLine1S)
            return CGPoint(x: x0 + S.noteLine1W * q, y: l0)
        } else if e < S.noteLine2S {
            let q: CGFloat = (e - S.noteLine1EndS) / (S.noteLine2S - S.noteLine1EndS)
            let y: CGFloat = l0 + (l1 - l0) * q - 4 * sin(CGFloat.pi * q)
            return CGPoint(x: x0 + S.noteLine1W * (1 - q), y: y)
        } else if e < S.noteLine2EndS {
            let q: CGFloat = (e - S.noteLine2S) / (S.noteLine2EndS - S.noteLine2S)
            return CGPoint(x: x0 + S.noteLine2W * q, y: l1)
        }
        return CGPoint(x: x0 + 26, y: l0 - 4)
    }

    // MARK: Ders sonu sahneleri

    private func dersSonuHalleri(_ em: SobiMaskotHal, _ e: CGFloat, _ env: CGFloat) {
        switch em {
        case .WEIGHTS: weights(e)
        case .SKATE:
            hedef[K.skateA] = 1
            hedef[K.lift] = S.skateRideLift
            sal[K.bodyDx] = skateDx(e, S.skArriveS, S.skExitS, 4.4)
            if e >= S.skPoseS && e < S.skExitS {
                coolPose(e - S.skPoseS)
            } else {
                rideBalance(e)
            }
            hop(e - S.skOllieS, hops: 1, hopS: 0.5, height: 24)
        case .GUITAR: guitar(e, env)
        case .JUGGLE:
            bothCatArms()
            hedef[K.catL] = S.jgArmL
            hedef[K.catR] = S.jgArmR
            let beat: CGFloat = e * S.tau / (2 * S.jgThrowS)
            sal[K.catL] = 8 * max(0, sin(beat))
            sal[K.catR] = 8 * max(0, -sin(beat))
            hedef[K.juggleA] = 1
            hedef[K.jesterA] = 1
            hedef[K.gazeY] = -2.5
            sal[K.gazeX] = 1.5 * sin(beat)
            sal[K.lift] = 1.5 * abs(sin(beat))
            agiz = .mutlu
        case .STARS: stars(e)
        default:
            // DETERMINED
            if e < S.dtBreathS {
                uzgunDurus(e, env)
            } else if e < S.dtFireS {
                let p: CGFloat = M.seg(e, S.dtBreathS, S.dtFireS)
                sal[K.squash] = -0.5 * sin(CGFloat.pi * p)
                sal[K.headDy] = -3 * sin(CGFloat.pi * p)
                hedef[K.earL] = 8
                hedef[K.earR] = 8
                goz = .kapali
            } else {
                fistPump(e - S.dtFireS)
                if e >= S.dtHoldS {
                    sal[K.catR] = 4 * sin((e - S.dtHoldS) * S.tau / S.dtHoldPeriodS)
                }
            }
        }
    }

    private func weights(_ e: CGFloat) {
        frontHands()
        hedef[K.frontGap] = 16
        hedef[K.barbellA] = 1
        let up: CGFloat = weightsLift(e)
        sal[K.frontDy] = S.wLowDy + (S.wHighDy - S.wLowDy) * up
        sal[K.bodyDy] = 7 * (1 - up)
        sal[K.headDy] = 3 * (1 - up)
        if e < S.wPullS {
            hedef[K.gazeY] = 2.5
        } else if e < S.wTopS {
            goz = .kapali
            hedef[K.blush] = 1
            hedef[K.earL] = 18
            hedef[K.earR] = 18
            let shake: CGFloat = sin(e * S.tau * 11)
            sal[K.frontRot] = 2 * shake
            sal[K.upperRot] = 1 * shake
        } else if e < S.wDropS {
            goz = .mutlu
            agiz = .mutlu
            hedef[K.blush] = 0.4
            earsWiggle(e, 1, base: -4)
            hop(e - S.wTopS - 0.15, hops: 1, hopS: 0.32, height: 7)
        } else {
            let since: CGFloat = e - S.wDropS
            if since < 0.6 {
                sal[K.squash] += 0.6 * exp(-since * 9) * cos(since * S.tau * 3)
            }
            sal[K.bodyDy] += 1.5 * sin(e * S.tau / 1.2)
            hedef[K.gazeY] = 1
        }
    }

    private func weightsLift(_ e: CGFloat) -> CGFloat {
        if e < S.wPullS { return 0 }
        if e < S.wTopS {
            let p: CGFloat = M.seg(e, S.wPullS, S.wTopS)
            return M.smooth01(p - 0.11 * sin(p * S.tau))
        }
        if e < S.wDropS { return 1 }
        if e < S.wDropS + 0.22 {
            let q: CGFloat = M.seg(e, S.wDropS, S.wDropS + 0.22)
            return 1 - q * q
        }
        return 0
    }

    func skateDx(_ e: CGFloat, _ arriveS: CGFloat, _ exitS: CGFloat, _ durS: CGFloat) -> CGFloat {
        if e < arriveS {
            let q: CGFloat = 1 - M.seg(e, 0, arriveS)
            return -S.skateOffX * q * q
        }
        if e < exitS { return 0 }
        let q: CGFloat = M.seg(e, exitS, durS)
        return S.skateOffX * q * q
    }

    private func rideBalance(_ e: CGFloat) {
        bothCatArms()
        hedef[K.catL] = 14
        hedef[K.catR] = 4
        sal[K.catL] = 5 * sin(e * S.tau * 1.3)
        sal[K.catR] = -5 * sin(e * S.tau * 1.3)
        hedef[K.bodyDy] = 3
        hedef[K.tilt] = 4
        hedef[K.earL] = 16
        hedef[K.earR] = 10
        agiz = .mutlu
    }

    /// Havalı duruş ([t] = duruşun başından): gözlük düşüyor, kollar kavuşuyor.
    private func coolPose(_ t: CGFloat) {
        frontHands()
        hedef[K.frontDy] = 2
        hedef[K.upperRot] = -4
        hedef[K.headRot] = -6
        hedef[K.legsOpen] = 1
        hedef[K.glassesA] = 1
        gozlukDususu = M.dropOffset(t)
        gozlukParilti = t - S.adDropS - 0.1
        sal[K.headDy] += M.landBump(t - S.adDropS)
        agiz = .mutlu
    }

    private func guitar(_ e: CGFloat, _ env: CGFloat) {
        let tau: CGFloat = S.tau
        frontHands()
        hedef[K.guitarA] = 1
        hedef[K.frontDy] = 30
        hedef[K.frontLX] = -2
        hedef[K.frontRX] = 16
        hedef[K.frontRY] = -21
        sal[K.frontLRot] = S.gtStrumDeg * sin(e * tau * S.gtStrumHz) * env
        sal[K.frontLY] = 1.5 * cos(e * tau * S.gtStrumHz) * env
        sal[K.frontRX] = 2 * sin(e * tau) * env
        sal[K.headRot] = 4 * sin(e * tau) * env
        sal[K.upperRot] = 2.5 * sin(e * tau + 0.5) * env
        sal[K.kickR] = 7 * max(0, sin(e * tau * 2)) * env
        sal[K.earL] = 5 * sin(e * tau) * env
        sal[K.earR] = -5 * sin(e * tau) * env
        goz = .mutlu
        agiz = .mutlu
    }

    private func stars(_ e: CGFloat) {
        hedef[K.starsA] = 1
        hedef[K.gazeY] = -2.5
        let n: Int = shownStars()
        for i in 0..<n {
            let since: CGFloat = e - (S.stFirstS + CGFloat(i) * S.stGapS)
            if since >= 0 && since < 0.45 {
                hop(since, hops: 1, hopS: 0.3, height: 8)
                goz = .mutlu
                agiz = .mutlu
            }
        }
        let party: CGFloat = S.stFirstS + CGFloat(n - 1) * S.stGapS + 0.45
        if n >= 3 && e >= party && e < S.stFadeS {
            bothCatArms()
            hedef[K.catL] = 58
            hedef[K.catR] = 48
            let flap: CGFloat = 6 * sin(e * S.tau * 3)
            sal[K.catL] = flap
            sal[K.catR] = flap
            hop(e - party, hops: 2, hopS: 0.4, height: 16)
            earsWiggle(e, 1, base: 10)
            goz = .mutlu
            agiz = .mutlu
        } else if n >= 1 && n <= 2 && e >= party {
            agiz = .mutlu
        }
    }

    /// "Bir daha!": kaşlar kararlı, kulaklar dik, sağ yumruk havada.
    private func fistPump(_ t: CGFloat) {
        hedef[K.browsDet] = 1
        hedef[K.earL] = -6
        hedef[K.earR] = -6
        hedef[K.hangRA] = 0
        hedef[K.catRA] = 1
        hedef[K.catR] = 62
        sal[K.catR] = 12 * sin(t * S.tau * 2.5) * exp(-t * 1.2)
        hop(t - 0.05, hops: 1, hopS: 0.35, height: 10)
        hedef[K.headRot] = -3
        agiz = .mutlu
    }
}
