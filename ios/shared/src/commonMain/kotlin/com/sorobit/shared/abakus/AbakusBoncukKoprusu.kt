// Kaynak (Android): yok — iOS görünüm katmanı için ince köprü.
//
// [AbacusBeadController]'ın Swift'ten çağrılması zor olan parçalarını (iç içe diziler, enum,
// iç içe veri sınıfları, varsayılan parametreler) yalın imzalı fonksiyonlara indirir. Yeni kural
// içermez; her fonksiyonun Android'deki karşılığı yanında yazılıdır:
//  - denetleyiciOlustur → AbacusFragment.onViewCreated / AbacusPracticeFragment.onViewCreated
//    (AbacusBeadController(...) + setup()) ve fragment_abacus.xml'deki alt boncuk marjları
//  - mesafeleriOlc      → ensureAbacusMetricsIfVisible (computeMovementDistancesFromLayout +
//    syncStateFromUi)
//  - dokunma            → bindBottomBeadTouchListener / bindTopBeadTouchListener
//  - degeriYerlestir    → Android'de karşılığı yok (mağaza önizlemesi ve başlangıç değeri için):
//    boncukları animasyonsuz yerleştirir, iç durumu Android'in öğreticide yaptığı gibi
//    syncStateFromUi ile fiziksel konumdan okur.
package com.sorobit.shared.abakus

import kotlin.math.roundToInt

object AbakusBoncukKoprusu {
    /** [dokunma] eylem kodları (MotionEvent.ACTION_* ile aynı sayılar). */
    const val EYLEM_DOWN = 0
    const val EYLEM_UP = 1
    const val EYLEM_MOVE = 2
    const val EYLEM_CANCEL = 3

    /** Android: `ViewConfiguration.scaledTouchSlop` varsayılanı (config_viewConfigurationTouchSlop = 8dp). */
    const val TOUCH_SLOP_DP = 8f

    fun eylem(kod: Int): AbacusTouchAction = when (kod) {
        EYLEM_DOWN -> AbacusTouchAction.DOWN
        EYLEM_UP -> AbacusTouchAction.UP
        EYLEM_MOVE -> AbacusTouchAction.MOVE
        EYLEM_CANCEL -> AbacusTouchAction.CANCEL
        else -> AbacusTouchAction.OTHER
    }

    /**
     * Denetleyiciyi kurar: alt boncukların başlangıç marjlarını (82/58/34/10 dp) piksele çevirip
     * yazar ve `setup()` çağırır. [yogunluk] = ekran ölçeği (px / dp).
     */
    fun denetleyiciOlustur(host: AbacusBeadHost, yogunluk: Float, animasyonSuresiMs: Long): AbacusBeadController {
        val denetleyici = AbacusBeadController(
            host = host,
            touchSlopPx = (TOUCH_SLOP_DP * yogunluk).roundToInt(),
            bottomStepPx = AbacusBeadMetrics.bottomStepPx(yogunluk),
            topStepPx = AbacusBeadMetrics.topStepPx(yogunluk),
            animationDurationMs = animasyonSuresiMs,
        )
        for (rod in 0..4) {
            for (i in 0..3) {
                val boncuk = denetleyici.bottomBeads[rod][i]
                boncuk.bottomMargin = AbacusUiSettings.beadBottomMarginPx(boncuk.idName, 0f, yogunluk)
            }
        }
        denetleyici.setup()
        return denetleyici
    }

    /** `bottomBeads[rod][beadIndex]` (beadIndex 0 = bariyere en yakın). */
    fun altBoncuk(denetleyici: AbacusBeadController, rod: Int, beadIndex: Int): AbacusBead =
        denetleyici.bottomBeads[rod.coerceIn(0, 4)][beadIndex.coerceIn(0, 3)]

    /** `topBeads[rod]`. */
    fun ustBoncuk(denetleyici: AbacusBeadController, rod: Int): AbacusBead =
        denetleyici.topBeads[rod.coerceIn(0, 4)]

    /** Boncuğun görünüm katmanındaki anahtarı: rod × 10 + (üst boncuk 0, alt boncuklar 1..4). */
    fun yuvaKodu(boncuk: AbacusBead): Int =
        boncuk.rod * 10 + (if (boncuk.isTop) 0 else boncuk.beadIndex + 1)

    /**
     * Hareket mesafelerini çubuk ölçülerinden hesaplar (beş çubuk da aynı ölçüdedir) ve iç durumu
     * fiziksel konumdan okur. Ölçüler px, y aşağı doğru artar. Dinamik ölçüm başarılıysa true.
     */
    fun mesafeleriOlc(
        denetleyici: AbacusBeadController,
        topBeadY: Int,
        topBeadHeight: Int,
        bottom1Y: Int,
        barrierY: Int,
        barrierHeight: Int,
    ): Boolean {
        val olcum = AbacusBeadMetrics.RodMeasurement(topBeadY, topBeadHeight, bottom1Y, barrierY, barrierHeight)
        val dinamik = AbacusBeadMetrics.fromBarrierDistances(List(5) { olcum }, 1.0f)
        val tamam = denetleyici.computeMovementDistancesFromLayout(dinamik, true)
        if (tamam) denetleyici.syncStateFromUi(true)
        return tamam
    }

    /** Boncuğa gelen dokunma olayı; [eylemKodu] yukarıdaki EYLEM_* sabitlerinden biridir. */
    fun dokunma(
        denetleyici: AbacusBeadController,
        boncuk: AbacusBead,
        eylemKodu: Int,
        rawY: Float,
        eventTimeMs: Long,
    ): Boolean {
        val e = eylem(eylemKodu)
        return if (boncuk.isTop) {
            denetleyici.onTopBeadTouch(boncuk.rod, e, rawY, eventTimeMs)
        } else {
            denetleyici.onBottomBeadTouch(boncuk.rod, boncuk.beadIndex + 1, e, rawY, eventTimeMs)
        }
    }

    /**
     * Abaküsü animasyonsuz olarak [deger]'i (0..99999) gösterecek biçimde yerleştirir.
     * Sıfırlama animasyonu sürerken çağrılırsa iç durum okunamaz (syncStateFromUi atlar); bu
     * yüzden önce animasyonsuz sıfırlanır.
     */
    fun degeriYerlestir(denetleyici: AbacusBeadController, deger: Int) {
        denetleyici.reset(false)
        val metin = deger.coerceIn(0, 99999).toString().padStart(5, '0')
        val altAdim = denetleyici.getBottomMoveDistancePx().roundToInt()
        for (rod in 0..4) {
            val rakam = metin[rod].digitToInt()
            val yukaridaki = rakam % 5
            for (i in 0 until yukaridaki) {
                val boncuk = denetleyici.bottomBeads[rod][i]
                boncuk.bottomMargin = boncuk.bottomMargin + altAdim
            }
            if (rakam >= 5) {
                denetleyici.topBeads[rod].translationY = denetleyici.getTopMoveDistancePx()
            }
        }
        denetleyici.syncStateFromUi(true)
    }
}
