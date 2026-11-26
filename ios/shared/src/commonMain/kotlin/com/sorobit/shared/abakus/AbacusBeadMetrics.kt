// Kaynak (Android): app/src/main/java/com/example/app/abacus/AbacusBeadMetrics.kt
//
// Boncuk adım mesafesi hesapları. Android'de View konumları (getLocationOnScreen) doğrudan
// okunuyordu; burada ölçümler [RodMeasurement] olarak dışarıdan verilir, hesap aynıdır.
package com.sorobit.shared.abakus

import kotlin.math.roundToInt

object AbacusBeadMetrics {
    /** `R.dimen.abacus_bead_step_bottom` (res/values/dimens.xml). */
    const val BOTTOM_STEP_DP = 48f

    /** `R.dimen.abacus_bead_step_top` (res/values/dimens.xml). */
    const val TOP_STEP_DP = 32f

    data class MoveDistancesPx(
        val bottomPx: Float,
        val topPx: Float,
    )

    /**
     * Bir çubuğun ekran ölçümleri (px, ekran koordinatı; y aşağı doğru artar).
     * Android'de yerleşimi tamamlanmamış (isLaidOut = false) ya da görünümü bulunamayan çubuklar
     * atlanır — çağıran taraf onları listeye koymamalıdır.
     */
    data class RodMeasurement(
        /** Üst boncuğun üst kenarı (getLocationOnScreen y). */
        val topBeadY: Int,
        val topBeadHeight: Int,
        /** 1. alt boncuğun üst kenarı. */
        val bottom1Y: Int,
        /** Bariyerin üst kenarı. */
        val barrierY: Int,
        val barrierHeight: Int,
    )

    /** Android: `resources.getDimension(R.dimen.abacus_bead_step_bottom)` — dp × yoğunluk. */
    fun bottomStepPx(density: Float): Float = BOTTOM_STEP_DP * density

    fun topStepPx(density: Float): Float = TOP_STEP_DP * density

    fun bottomStepPxInt(density: Float): Int = bottomStepPx(density).roundToInt()

    fun topStepPxInt(density: Float): Int = topStepPx(density).roundToInt()

    /**
     * Çalışma zamanı hareket mesafelerini bariyer aralıklarından hesaplar.
     *
     * - Üst boncuk hareketi = (barrierTop - topBeadBottom) ortalaması × oran
     * - Alt boncuk hareketi = (bottom1Top - barrierBottom) ortalaması × oran
     *
     * Ölçüm yoksa / aralıklar geçersizse null döner.
     */
    fun fromBarrierDistances(
        rods: List<RodMeasurement>,
        ratio: Float = 1.0f,
    ): MoveDistancesPx? {
        val safeRatio = ratio.coerceIn(0f, 1f)
        if (safeRatio <= 0f) return null

        val topDistances = mutableListOf<Float>()
        val bottomDistances = mutableListOf<Float>()

        for (rod in rods) {
            val topBeadBottom = rod.topBeadY + rod.topBeadHeight
            val bottom1Top = rod.bottom1Y
            val barrierTop = rod.barrierY
            val barrierBottom = rod.barrierY + rod.barrierHeight

            val topGap = (barrierTop - topBeadBottom).toFloat()
            val bottomGap = (bottom1Top - barrierBottom).toFloat()

            if (topGap > 0f) topDistances.add(topGap)
            if (bottomGap > 0f) bottomDistances.add(bottomGap)
        }

        if (topDistances.isEmpty() || bottomDistances.isEmpty()) return null

        val avgTop = topDistances.average().toFloat()
        val avgBottom = bottomDistances.average().toFloat()

        val computedTop = (avgTop * safeRatio).coerceAtLeast(1f)
        val computedBottom = (avgBottom * safeRatio).coerceAtLeast(1f)

        return MoveDistancesPx(
            bottomPx = computedBottom,
            topPx = computedTop,
        )
    }
}
