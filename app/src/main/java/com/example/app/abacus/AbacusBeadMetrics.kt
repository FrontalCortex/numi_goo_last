package com.example.app.abacus

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.example.app.R
import kotlin.math.roundToInt

/** Ekran yoğunluğuna göre boncuk adım mesafesi (px). Layout @dimen ile uyumlu. */
object AbacusBeadMetrics {
    data class MoveDistancesPx(
        val bottomPx: Float,
        val topPx: Float,
    )

    fun bottomStepPx(context: Context): Float =
        context.resources.getDimension(R.dimen.abacus_bead_step_bottom)

    fun topStepPx(context: Context): Float =
        context.resources.getDimension(R.dimen.abacus_bead_step_top)

    fun bottomStepPxInt(context: Context): Int = bottomStepPx(context).roundToInt()

    fun topStepPxInt(context: Context): Int = topStepPx(context).roundToInt()

    /**
     * Calculates runtime movement distances based on barrier spacing.
     *
     * - Top bead move = 100% of (barrierTop - topBeadBottom)
     * - Bottom bead move = 100% of (bottom1Top - barrierBottom)
     *
     * Returns null if layout is not ready / ids are missing / distances invalid.
     */
    fun fromBarrierDistances(
        root: View,
        ratio: Float = 1.0f,
    ): MoveDistancesPx? {
        val safeRatio = ratio.coerceIn(0f, 1f)
        if (safeRatio <= 0f) return null

        val topDistances = mutableListOf<Float>()
        val bottomDistances = mutableListOf<Float>()

        val tmpTop = IntArray(2)
        val tmpBottom = IntArray(2)
        val tmpBarrier = IntArray(2)

        for (rod in 0..4) {
            val topBead = findImageView(root, "rod${rod}_bead_top") ?: continue
            val bottom1 = findImageView(root, "rod${rod}_bead_bottom1") ?: continue
            val barrier = findView(root, "rod${rod}_barrier") ?: continue

            if (!topBead.isLaidOut || !bottom1.isLaidOut || !barrier.isLaidOut) continue

            topBead.getLocationOnScreen(tmpTop)
            bottom1.getLocationOnScreen(tmpBottom)
            barrier.getLocationOnScreen(tmpBarrier)

            val topBeadBottom = tmpTop[1] + topBead.height
            val bottom1Top = tmpBottom[1]
            val barrierTop = tmpBarrier[1]
            val barrierBottom = tmpBarrier[1] + barrier.height

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

    private fun findView(root: View, name: String): View? {
        val id = root.resources.getIdentifier(name, "id", root.context.packageName)
        return if (id != 0) root.findViewById(id) else null
    }

    private fun findImageView(root: View, name: String): ImageView? {
        return findView(root, name) as? ImageView
    }
}
