// Kaynak: app/src/main/java/com/example/app/model/BeadAnimation.kt (Android).
// Android sınıfı View/Fragment'a bağlıdır; buraya yalnızca saf veri/mantık alındı:
// animasyon türü (1..4) → hangi görsel, hangi yön, hangi mesafe, süre ve bitişteki marj güncellemesi.
// Görsel taraf (ImageView.animate, layoutParams) iOS'ta arayüz katmanında yeniden yazılacak.
// R.drawable.x → kaynak adını taşıyan String anahtarı. Log/TutorialBeadDiagnostics çağrıları yok.
package com.sorobit.shared.matematik

import kotlin.math.roundToInt

/** Android'deki `AbacusBeadMetrics.MoveDistancesPx` karşılığı: alt ve üst boncukların hareket mesafesi. */
data class BeadMoveDistances(val bottomPx: Float, val topPx: Float)

/** Bir boncuk animasyonunun platformdan bağımsız tarifi. */
data class BeadAnimationPlan(
    /** `bead.setImageResource(...)` ile atanan görselin kaynak adı. */
    val drawableKey: String,
    /** `translationY(...)` hedefi (px; eksi = yukarı). */
    val translationY: Float,
    /** Bitişte `bottomMargin`'e eklenecek fark (px); tür 3 ve 4'te marj değişmez → 0. */
    val bottomMarginDelta: Int,
    /** Bitişte `translationY` sıfırlanır mı (tür 1 ve 2: evet; tür 3 ve 4: hayır). */
    val resetTranslationAtEnd: Boolean,
    val durationMs: Long,
)

class BeadAnimation(
    private val beadId: String,
    private val animationType: Int
) {
    companion object {
        const val ANIMATION_DURATION_MS = 300L
        const val DRAWABLE_BEAD = "soroban_bead"
        const val DRAWABLE_BEAD_SELECTED = "soroban_bead_selected"

        /**
         * Android'deki `animate()` içindeki `when (animationType)` dalları ile
         * animateBeadsUp (1) / animateBeadsDown (2) / animateBeadDown (3) / animateBeadUp (4)
         * gövdelerinin saf özeti. Bilinmeyen türde Android hiçbir animasyon başlatmaz → null.
         * Mesafeler Android'deki gibi `roundToInt()` ile tam piksele yuvarlanır.
         */
        fun planFor(animationType: Int, distances: BeadMoveDistances): BeadAnimationPlan? {
            return when (animationType) {
                1 -> {
                    // Alt boncuk yukarı: seçili görsel, -mesafe; bitişte marj += mesafe.
                    val moveDistance = distances.bottomPx.roundToInt()
                    BeadAnimationPlan(DRAWABLE_BEAD_SELECTED, -moveDistance.toFloat(), moveDistance, true, ANIMATION_DURATION_MS)
                }
                2 -> {
                    // Alt boncuk aşağı: normal görsel, +mesafe; bitişte marj -= mesafe.
                    val moveDistance = distances.bottomPx.roundToInt()
                    BeadAnimationPlan(DRAWABLE_BEAD, moveDistance.toFloat(), -moveDistance, true, ANIMATION_DURATION_MS)
                }
                // Üst boncuk yukarı: normal görsel, özgün konumuna (translationY = 0) döner.
                4 -> BeadAnimationPlan(DRAWABLE_BEAD, 0f, 0, false, ANIMATION_DURATION_MS)
                3 -> {
                    // Üst boncuk aşağı: seçili görsel, +üst mesafe; marj değişmez.
                    val moveDistance = distances.topPx.roundToInt()
                    BeadAnimationPlan(DRAWABLE_BEAD_SELECTED, moveDistance.toFloat(), 0, false, ANIMATION_DURATION_MS)
                }
                else -> null
            }
        }
    }

    private var isAnimating = false

    fun getAnimationType(): Int = animationType
    fun getBeadId(): String = beadId

    fun isAnimating(): Boolean {
        return isAnimating
    }

    /**
     * Android `animate()` karşılığı: zaten oynuyorsa atlar (null). Değilse [isAnimating] bayrağını
     * kaldırır ve planı döndürür; görsel katman animasyon bitince [onAnimationEnd] çağırmalıdır.
     * Android'deki davranış aynen korundu: bilinmeyen türde (1..4 dışı) bayrak true olur ama
     * animasyon başlamadığı için bir daha inmez.
     */
    fun animate(distances: BeadMoveDistances): BeadAnimationPlan? {
        if (isAnimating) return null
        isAnimating = true
        return planFor(animationType, distances)
    }

    /** Android'deki `withEndAction { isAnimating = false ... }` karşılığı. */
    fun onAnimationEnd() {
        isAnimating = false
    }
}
