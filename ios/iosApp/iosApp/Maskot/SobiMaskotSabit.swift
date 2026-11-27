import SwiftUI

// Sobi maskotunun sabitleri: zamanlar (sn), ölçüler (kaynak çizimin koordinatı) ve renkler.
// Kaynak: app/src/main/java/com/example/app/BunnyMascotView.kt, `companion object` — adlar
// Android'deki sabitlerin camelCase hâli (CB_LIFT_S → cbLiftS), değerler birebir.

enum SobiMaskotSabit {
    static let tau: CGFloat = 2 * CGFloat.pi

    // ---- Kovboy
    static let cbLiftS: CGFloat = 0.7
    static let cbStareS: CGFloat = 1.6
    static let cbReachS: CGFloat = 2.5
    static let cbDrawS: CGFloat = 3.5
    static let cbPoseS: CGFloat = 4.3
    static let cbBite1S: CGFloat = 4.75
    static let cbBite2S: CGFloat = 5.15
    static let cbAhemS: CGFloat = 6.1
    static let cbTipS: CGFloat = 6.3
    static let cbDropS: CGFloat = 6.35
    static let cbHolsterS: CGFloat = 6.6
    static let cbBites: [CGFloat] = [4.75, 5.15]
    static let cbReachDeg: CGFloat = 16
    static let cbDrawUp: CGFloat = 35
    static let cbPistolDeg: CGFloat = -95
    static let cbMouthDeg: CGFloat = -150
    static let cbHolsterArm: CGFloat = -35
    static let cbFrontX: CGFloat = 388
    static let cbFrontY: CGFloat = 129
    static let cbFrontScale: CGFloat = 0.33
    static let cbFrontAX: CGFloat = 266
    static let cbFrontAY: CGFloat = 290
    static let cbHatLowFront: CGFloat = 30
    static let bandanaX: CGFloat = 390
    static let bandanaY: CGFloat = 203
    static let bandanaScale: CGFloat = 0.28
    static let bandanaAX: CGFloat = 262
    static let bandanaAY: CGFloat = 384
    static let beltTop: CGFloat = 254
    static let holsterX: CGFloat = 430
    static let holsterY: CGFloat = 258
    static let holsterScale: CGFloat = 0.075
    static let carrotTipDeg: CGFloat = 125.54
    static let handCarrotScale: CGFloat = 0.09
    static let bite1X: CGFloat = 60
    static let bite1Y: CGFloat = 500
    static let bite1R: CGFloat = 70
    static let bite2X: CGFloat = 112
    static let bite2Y: CGFloat = 445
    static let bite2R: CGFloat = 78

    // ---- Karate
    static let krStanceS: CGFloat = 0.8
    static let krRaiseS: CGFloat = 1.5
    static let krChopS: CGFloat = 2.2
    static let krPainS: CGFloat = 2.45
    static let krBlowS: CGFloat = 3.1
    static let krCalmS: CGFloat = 3.8
    static let krBreakS: CGFloat = 4.4
    static let krArmHigh: CGFloat = 80
    static let krArmHit: CGFloat = -32
    static let kbLeft: CGFloat = 441
    static let kbRight: CGFloat = 499
    static let kbMid: CGFloat = 470
    static let kbTop: CGFloat = 247
    static let kbBrickTop: CGFloat = 254
    static let kbBrickX: [CGFloat] = [448, 492]

    // ---- Ninja
    static let njSignS: CGFloat = 1.0
    static let njThrowS: CGFloat = 2.0
    static let njSmokeS: CGFloat = 2.35
    static let njPeekS: CGFloat = 4.1
    static let njHoldS: CGFloat = 4.6
    static let njGlanceS: CGFloat = 1.2
    static let njSlideS: CGFloat = 0.6
    static let njSlitTop: CGFloat = 131
    static let njSlitBottom: CGFloat = 153
    static let smokeX: [CGFloat] = [393, 340, 446, 393, 330, 456, 370, 420, 393, 345, 441]
    static let smokeY: [CGFloat] = [280, 250, 250, 210, 190, 190, 140, 140, 90, 95, 95]
    static let smokeR: [CGFloat] = [48, 40, 40, 52, 38, 38, 44, 44, 42, 32, 32]

    // ---- Korsan
    static let prRaiseS: CGFloat = 0.9
    static let prPuzzleS: CGFloat = 2.5
    static let prFlipS: CGFloat = 3.1
    static let prLook2S: CGFloat = 3.6
    static let prHoldS: CGFloat = 4.6
    static let prSwayS: CGFloat = 2.2
    static let prBlinkAtS: CGFloat = 1.0
    static let prLower: CGFloat = 70
    static let prEar: CGFloat = 38
    static let prHatX: CGFloat = 388
    static let prHatY: CGFloat = 120
    static let prHatScale: CGFloat = 0.25
    static let prHatAX: CGFloat = 256
    static let prHatAY: CGFloat = 345
    static let spyLen: CGFloat = 34
    static let spyFreeDeg: CGFloat = -135
    static let spyBigR: CGFloat = 12
    static let spySmallR: CGFloat = 6
    static let spyEyeBig: CGFloat = 2.1
    static let spyEyeSmall: CGFloat = 0.55
    static let spyRingAt: [CGFloat] = [0.35, 0.7]

    static let cheekLX: CGFloat = 334
    static let cheekRX: CGFloat = 440
    static let cheekY: CGFloat = 170

    // ---- Rehber öğretmeni
    static let tcArm: CGFloat = 40
    static let tcTip: CGFloat = -100
    static let tpArm: CGFloat = -5
    static let tpTip: CGFloat = 80
    static let twArm: CGFloat = 70
    static let twTip: CGFloat = -90
    static let teachSwayDeg: CGFloat = 15

    // ---- Kafanın öne eğilmesi
    static let pitchSquash: CGFloat = 0.07
    static let pitchWiden: CGFloat = 0.02
    static let pitchFaceDy: CGFloat = 9
    static let pitchEar: CGFloat = 0.3

    // ---- Gözlük
    static let glDropS: CGFloat = 0.3
    static let glSlipS: CGFloat = 1.7
    static let glFixS: CGFloat = 2.6
    static let glSlip: CGFloat = 17
    static let glNodS: CGFloat = 2.0

    // ---- Rapçi
    static let rpBeatS: CGFloat = 0.5
    static let rpOffS: CGFloat = 2.0
    static let rpCatchS: CGFloat = 2.4
    static let rpTossS: CGFloat = 2.9
    static let rpOnS: CGFloat = 3.25
    static let rpRecrossS: CGFloat = 3.5
    static let rpArmsDy: CGFloat = 34
    static let rpCatchUp: CGFloat = -10
    static let rpHatX: CGFloat = 388
    static let rpHatY: CGFloat = 120
    static let rpHatRot: CGFloat = -30
    static let rpHatScale: CGFloat = 0.24
    static let rpHatAX: CGFloat = 313
    static let rpHatAY: CGFloat = 340
    static let chainLinks: Int = 14
    static let jesterX: CGFloat = 386
    static let jesterY: CGFloat = 124
    static let jesterRot: CGFloat = -3
    static let jesterScale: CGFloat = 0.17
    static let jesterAX: CGFloat = 266
    static let jesterAY: CGFloat = 455
    static let chainLX: CGFloat = 368
    static let chainRX: CGFloat = 414
    static let chainMidX: CGFloat = 391
    static let chainTopY: CGFloat = 207
    static let chainMidY: CGFloat = 250
    static let chainPendantY: CGFloat = 232

    // ---- Ders sonu sahneleri
    static let catHandLX: CGFloat = 334
    static let catHandLY: CGFloat = 232
    static let frontLPawX: CGFloat = 374
    static let wLowDy: CGFloat = 16
    static let wHighDy: CGFloat = -10
    static let wPullS: CGFloat = 0.6
    static let wTopS: CGFloat = 1.6
    static let wDropS: CGFloat = 2.9
    static let skateRideLift: CGFloat = 10
    static let skateOffX: CGFloat = 250
    static let skArriveS: CGFloat = 1.1
    static let skOllieS: CGFloat = 1.2
    static let skPoseS: CGFloat = 2.0
    static let skExitS: CGFloat = 3.9
    static let gtX: CGFloat = 372
    static let gtY: CGFloat = 252
    static let gtAngle: CGFloat = 15
    static let gtScale: CGFloat = 0.30
    static let gtStrumX: CGFloat = 135
    static let gtStrumY: CGFloat = 285
    static let gtStrumDeg: CGFloat = 12
    static let gtStrumHz: CGFloat = 2.5
    static let jgArmL: CGFloat = 20
    static let jgArmR: CGFloat = 10
    static let jgThrowS: CGFloat = 0.3
    static let jgFlightS: CGFloat = 0.6
    static let jgHeight: CGFloat = 120
    static let pfCrownS: CGFloat = 0.3
    static let pfFlashS: CGFloat = 1.6
    static let pfFlashLoopS: CGFloat = 2.0
    static let flashDurS: CGFloat = 0.18
    static let flashTintMax: CGFloat = 0.5
    static let flashNearX: CGFloat = 330
    static let flashFarX: CGFloat = 455
    static let flashFarColor: UInt32 = 0x26FFFFFF
    static let flashT: [CGFloat] = [0.05, 0.35, 0.55, 0.95, 1.25, 1.6]
    static let stFirstS: CGFloat = 0.5
    static let stGapS: CGFloat = 0.7
    static let stFadeS: CGFloat = 3.9
    static let stX: [CGFloat] = [292, 393, 494]
    static let stY: [CGFloat] = [70, 30, 70]
    static let dtBreathS: CGFloat = 1.5
    static let dtFireS: CGFloat = 2.1
    static let dtHoldS: CGFloat = 3.3
    static let dtHoldPeriodS: CGFloat = 1.0
    static let skateWheelDx: [CGFloat] = [-21, 21]

    // ---- Görünür alan: kaynak çizimde centerX etrafında units genişliğinde, topY'den başlayan kare.
    static let units: CGFloat = 314
    static let centerX: CGFloat = 393
    static let topY: CGFloat = 6

    static let shadowX: CGFloat = 401
    static let shadowY: CGFloat = 305
    static let jumpHeight: CGFloat = 22
    static let cheerHopS: CGFloat = 0.45
    static let headLagS: CGFloat = 0.04

    static let shoulderLX: CGFloat = 372
    static let shoulderY: CGFloat = 222
    static let mirrorX2: CGFloat = 789
    static let mirrorDy: CGFloat = 2
    static let shoulderRX: CGFloat = 789 - 372
    static let peaceShoulderX: CGFloat = 420
    static let peaceShoulderY: CGFloat = 228
    static let peaceRest: CGFloat = -10

    static let catDx: CGFloat = -213
    static let cheerShoulderLY: CGFloat = 228
    static let cheerShoulderRX: CGFloat = 418
    static let cheerShoulderRY: CGFloat = 225
    static let cheerUpL: CGFloat = 32
    static let cheerUpR: CGFloat = 18
    static let waveUp: CGFloat = 32
    static let pointUpR: CGFloat = -8
    static let pointUpL: CGFloat = 8
    static let thinkUp: CGFloat = 105
    static let catHandX: CGFloat = 681
    static let catHandY: CGFloat = 220
    static let balloonStringX: CGFloat = 456.5
    static let balloonStringY: CGFloat = 628.1
    static let dogDx: CGFloat = -233
    static let dogDy: CGFloat = -407
    static let frontShoulderLX: CGFloat = 355
    static let frontShoulderRX: CGFloat = 427
    static let frontShoulderY: CGFloat = 198
    static let clapHz: CGFloat = 2.5

    static let catMouthX: CGFloat = 601.1
    static let catMouthTop: CGFloat = 165.4
    static let happyMouthTop: CGFloat = 162
    static let happyMouthScale: CGFloat = 0.85
    static let mouthX: CGFloat = 386.1
    static let mouthY: CGFloat = 168

    static let neckX: CGFloat = 390
    static let neckY: CGFloat = 212
    static let hipX: CGFloat = 395
    static let hipY: CGFloat = 262
    static let feetX: CGFloat = 397
    static let feetY: CGFloat = 310
    static let legLHipX: CGFloat = 383
    static let legRHipX: CGFloat = 414
    static let legHipY: CGFloat = 258
    static let stepDx: CGFloat = 1
    static let stepDeg: CGFloat = 5

    static let earLX: CGFloat = 352
    static let earRX: CGFloat = 425
    static let earY: CGFloat = 125

    static let eyeY: CGFloat = 143.3
    static let eyeX: [CGFloat] = [364.6, 406.5]
    static let blushX: [CGFloat] = [352, 420]

    static let blinkMs: Double = 140
    static let earFlickMs: Double = 300
    static let sleepWakeS: CGFloat = 0.5

    // ---- Mesajlaşma
    static let chatAskS: CGFloat = 1.6
    static let chatWaitS: CGFloat = 1.9
    static let chatReplyS: CGFloat = 2.3
    static let chatAnswerS: CGFloat = 3.6
    static let chatFadeS: CGFloat = 5.0
    static let askBubbleX: CGFloat = 305
    static let askBubbleY: CGFloat = 150
    static let replyBubbleX: CGFloat = 482
    static let replyBubbleY: CGFloat = 128

    static let demoDays: [Int] = [3, 5, 7]

    // ---- Çalar saat
    static let demoAlarmHours: [Int] = [16, 18, 19, 20]
    static let clockX: CGFloat = 395
    static let clockY: CGFloat = 240
    static let clockR: CGFloat = 26
    static let alarmBellDeg: CGFloat = 42
    static let alarmBellSin: CGFloat = 0.6691
    static let alarmBellCos: CGFloat = 0.7431
    static let alarmShakeHz: CGFloat = 14
    static let alarmRingAtS: CGFloat = 1.2
    static let alarmRingS: CGFloat = 1.2
    static let alarmQuietAfterS: CGFloat = 2
    static let alarmHandSpeed: CGFloat = 5
    static let flameStepS: CGFloat = 2
    static let flameReactS: CGFloat = 0.8
    // streak_flame_ic 24'lük çizimde: alev x=12 ortalı, tabanı y=20,5, boyu 18,5; 100'lük sınırlar.
    static let flameIconCX: CGFloat = 50
    static let flameIconBottom: CGFloat = 20.5 / 24 * 100
    static let flameIconH: CGFloat = 18.5 / 24 * 100
    static let calFirstTickS: CGFloat = 0.6
    static let calTickS: CGFloat = 0.35
    static let calResetS: CGFloat = 4.6
    static let calBox: CGFloat = 10
    static let calGap: CGFloat = 2

    static let abacusRodX: [CGFloat] = [369, 382, 395, 408, 421]

    // ---- Reklam hâlleri
    static let adTvW: CGFloat = 50
    static let adTvTop: CGFloat = 279
    static let adTvStartX: CGFloat = 580
    static let adTvEnterS: CGFloat = 0.15
    static let adTvSpeed: CGFloat = 260
    static let adJumpHopLenS: CGFloat = 1.1
    static let adJumpHopS: CGFloat = 0.15 + (580 - 395) / 260 - 1.1 / 2
    static let adJumpHeight: CGFloat = 42
    static let adJumpCheerS: CGFloat = 1.6
    static let adBtnX: CGFloat = 494
    static let adBtnY: CGFloat = 225
    static let adBtnW: CGFloat = 56
    static let adBtnReadyS: CGFloat = 0.6
    static let adBtnPressS: CGFloat = 1.05
    static let adBtnCrownS: CGFloat = 1.45
    static let adBtnFadeS: CGFloat = 4.0
    static let finaleGlassesDelayS: CGFloat = 0.25
    static let adDropS: CGFloat = 0.4
    static let adDropHeight: CGFloat = 120
    static let wandUpS: CGFloat = 0.4
    static let wandWaveEndS: CGFloat = 1.5
    static let wandHz: CGFloat = 1.6
    static let wandArmUp: CGFloat = 30
    static let wandPointUp: CGFloat = 5
    static let wandLen: CGFloat = 30
    static let wandDirDeg: CGFloat = -60
    static let zapStrikeS: CGFloat = 1.6
    static let zapCharS: CGFloat = 0.3
    static let zapCrumbleS: CGFloat = 0.5
    static let zapCrownS: CGFloat = 2.6
    static let zapFadeS: CGFloat = 4.9
    static let magicStampS: CGFloat = 1.8
    static let magicPoofS: CGFloat = 2.2
    static let magicCrownS: CGFloat = 2.8
    static let magicFadeS: CGFloat = 5.1
    static let bbX: CGFloat = 494
    static let bbScale: CGFloat = 0.17
    static let bbTop: CGFloat = 305 - 512 * 0.17
    static let bbBoardCY: CGFloat = 305 - 512 * 0.17 + 158 * 0.17
    static let ashSpeckX: [CGFloat] = [-16, -6, 4, 13, -1]
    static let ashSpeckY: [CGFloat] = [0.35, 0.7, 0.5, 0.3, 0.2]
    static let glassesW: CGFloat = 512
    static let glassesH: CGFloat = 196
    static let glassesScale: CGFloat = 41.9 / 260
    static let glassesLeft: CGFloat = 385.55 - 256 * (41.9 / 260)
    static let glassesTop: CGFloat = 143.3 - 106 * (41.9 / 260)
    static let adSignX: CGFloat = 492
    static let adPushStartS: CGFloat = 0.9
    static let adPushEndS: CGFloat = 2.0

    // ---- Bilgi sorusu hâlleri
    static let boardX0: CGFloat = 444
    static let boardX1: CGFloat = 516
    static let boardY0: CGFloat = 190
    static let boardY1: CGFloat = 236
    static let boardTextY: CGFloat = 218
    static let boardTextSize: CGFloat = 13
    static let boardWriteS: CGFloat = 0.8
    static let boardDoneS: CGFloat = 2.1
    static let boardEraseS: CGFloat = 4.2
    /// Android BOARD_SAMPLES (deneme ekranı örnekleri; Android'de de sabit Türkçe).
    static let boardSamples: [String] = ["9 yaş", "YouTube", "10 dk"]
    static let listenHandUp: CGFloat = 72
    static let listenAnswerS: CGFloat = 2.0
    static let listenCheckX: CGFloat = 482
    static let listenCheckY: CGFloat = 112
    static let noteX0: CGFloat = 354
    static let noteX1: CGFloat = 424
    static let noteY0: CGFloat = 212
    static let noteY1: CGFloat = 256
    static let noteLines: [CGFloat] = [222, 230]
    static let noteTextX: CGFloat = 378
    static let noteLine1W: CGFloat = 32
    static let noteLine2W: CGFloat = 22
    static let noteLine1S: CGFloat = 0.5
    static let noteLine1EndS: CGFloat = 1.4
    static let noteLine2S: CGFloat = 1.5
    static let noteLine2EndS: CGFloat = 2.3
    static let noteEraseS: CGFloat = 3.9
    static let noteWriteOffsetS: CGFloat = 0.5 - 0.1
    static let frontRPawX: CGFloat = 408
    static let frontRPawY: CGFloat = 217
    static let carrotScale: CGFloat = 0.08
    static let carrotGripX: CGFloat = 180
    static let carrotGripY: CGFloat = 330
    static let notePenHandDx: CGFloat = (180 - 55) * 0.08
    static let notePenHandDy: CGFloat = (330 - 505) * 0.08

    // ---- Pro hâlleri
    static let jetTankX: [CGFloat] = [338, 434]
    static let heartBounds: CGFloat = 100
}

/// Renkler (0xAARRGGBB), kaynak çizimin paletinden; Android sabit adlarının camelCase hâli.
enum SobiMaskotRenk {
    static let belt: UInt32 = 0xFF7A4A24
    static let buckle: UInt32 = 0xFFF2C14E
    static let holster: UInt32 = 0xFF8A5A2B
    static let holsterDark: UInt32 = 0xFF6B4220
    static let bandRed: UInt32 = 0xFFE8453C
    static let bandLight: UInt32 = 0xFFFF7A6E
    static let bandDark: UInt32 = 0xFFC9302A
    static let ninjaCloth: UInt32 = 0xFF2E3A57
    static let ninjaFold: UInt32 = 0xFF45547A
    static let patch: UInt32 = 0xFF1E2230
    static let patchShine: UInt32 = 0xFF3A4258
    static let brass: UInt32 = 0xFFE0A84A
    static let brassDark: UInt32 = 0xFFA8742A
    static let glass: UInt32 = 0xFFCFEFFF
    static let kbWood: UInt32 = 0xFFE0A96D
    static let kbEdge: UInt32 = 0xFFB57A3E
    static let block: UInt32 = 0xFFA7AFB8
    static let blockEdge: UInt32 = 0xFF7D858F
    static let ninjaSmoke: UInt32 = 0xFFE3E7EE
    static let smokeShade: UInt32 = 0xFFBFC6D1
    static let crumb: UInt32 = 0xFFFF8A3D
    static let skateDeck: UInt32 = 0xFFFF8A3D
    static let skateGrip: UInt32 = 0xFF3A3A48
    static let skateTruck: UInt32 = 0xFF9AA3AD
    static let skateWheel: UInt32 = 0xFFFFD84A
    static let skateHub: UInt32 = 0xFF8A6D1F
    static let steel: UInt32 = 0xFF8C96A3
    static let dust: UInt32 = 0xFFC9C2B8
    static let eye: UInt32 = 0xFF0C2C3C
    static let mouth: UInt32 = 0xFF430231
    static let tongue: UInt32 = 0xFFF281AA
    static let blush: UInt32 = 0x8CF281AA
    static let light: UInt32 = 0xFFD8F4FF
    static let white: UInt32 = 0xFFFFFFFF
    static let alarmRed: UInt32 = 0xFFEF5350
    static let alarmRedDark: UInt32 = 0xFFC62828
    static let alarmBell: UInt32 = 0xFFFFC107
    static let alarmBellDark: UInt32 = 0xFFE0A100
    static let bubbleEdge: UInt32 = 0xFF5FBFFE
    static let starEdge: UInt32 = 0xFFE0A800
    static let phoneBody: UInt32 = 0xFF22323A
    static let phoneEdge: UInt32 = 0xFF2E4350
    static let phoneBack: UInt32 = 0xFF3A5566
    static let phoneLensY: [CGFloat] = [205, 212]
    static let noSignRed: UInt32 = 0xFFE53935
    static let ash: UInt32 = 0xFF4A4A4A
    static let ashDark: UInt32 = 0xFF2E2E2E
    static let smoke: UInt32 = 0xFF9E9E9E
    static let bolt: UInt32 = 0xFFFFE54C
    static let wandStick: UInt32 = 0xFF3E3550
    static let adRed: UInt32 = 0xFFE8453C
    static let metalGray: UInt32 = 0xFF8B9DAB
    static let boardGreen: UInt32 = 0xFF2F594E
    static let chalk: UInt32 = 0xFFEAF6FF
    static let noteCover: UInt32 = 0xFFE8453C
    static let noteCoverDark: UInt32 = 0xFFB3261E
    static let jetSilver: UInt32 = 0xFFB9C6D0
    static let jetBand: UInt32 = 0xFF8B9DAB
    static let jetShine: UInt32 = 0xFFE4ECF1
    static let jetNozzle: UInt32 = 0xFF5F6B75
    static let capeRed: UInt32 = 0xFFE8453C
    static let capeInner: UInt32 = 0x8CB3261E
    static let crownGold: UInt32 = 0xFFFFC93C
    static let crownBand: UInt32 = 0xFFF2A900
    static let crownTip: UInt32 = 0xFFFFE082
    static let calBoxFill: UInt32 = 0xFFEAF6FF
    static let checkGreen: UInt32 = 0xFF51B848
    static let star: UInt32 = 0xFFFFDD00
    static let wood: UInt32 = 0xFFB8743F
    static let woodLight: UInt32 = 0xFFF7E6CC
    static let woodDark: UInt32 = 0xFF9C6B3F
    static let shadowBlack: UInt32 = 0x33000000
    static let ember: UInt32 = 0xFFFF7043
    static let chainShine: UInt32 = 0xFFFFF3C4
    static let beadShine: UInt32 = 0x55FFFFFF
    static let abacusBeads: [UInt32] = [0xFFF35720, 0xFFFFDD00, 0xFF8FA316, 0xFFF281AA, 0xFF5FBFFE]
    /// Göğüsteki PRO rozetinin renk geçişi (Android badgePaint).
    static let badgeGradient: [UInt32] = [0xFF1DF0A4, 0xFF3A82FF, 0xFFD558FF]
    /// Alevin halesi (Android glowPaint): 0x73FFB300 → 0x00FFB300.
    static let glowInner: UInt32 = 0x73FFB300
    static let glowOuter: UInt32 = 0x00FFB300
    /// Kafanın rengi (BunnyMascotArt.HEAD[0]); şişen yanaklar bununla.
    static let headFill: UInt32 = 0xFF91D8FF
}
