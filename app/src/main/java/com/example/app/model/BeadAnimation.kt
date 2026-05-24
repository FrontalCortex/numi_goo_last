package com.example.app.model

import android.util.Log
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.app.R
import com.example.app.TutorialBeadDiagnostics
import com.example.app.abacus.AbacusBeadMetrics
import java.util.WeakHashMap
import kotlin.math.roundToInt

class BeadAnimation(
    private val fragment: Fragment,
    private val beadId: String,
    private val animationType: Int
) {
    companion object {
        private val cachedDistancesByRoot = WeakHashMap<android.view.View, AbacusBeadMetrics.MoveDistancesPx>()
    }

    private var isAnimating = false

    fun getAnimationType(): Int = animationType
    fun getBeadId(): String = beadId

    fun isAnimating(): Boolean {
        return isAnimating
    }

    fun animate() {
        if (isAnimating) {
            TutorialBeadDiagnostics.log(
                "SKIP already animating beadId=$beadId type=$animationType",
            )
            return
        }

        val rootView = fragment.requireView()
        val bead = rootView.findViewById<ImageView>(
            rootView.resources.getIdentifier(beadId, "id", rootView.context.packageName)
        )
        if (bead == null) {
            Log.e("BeadAnimation", "Bead not found: $beadId")
            return
        }

        val ctx = rootView.context
        TutorialBeadDiagnostics.log(
            "START beadId=$beadId type=$animationType | ${TutorialBeadDiagnostics.beadState(ctx, rootView, beadId)}",
        )

        isAnimating = true
        val moveDistances = resolveMoveDistances(rootView)
        when (animationType) {
            1 -> animateBeadsUp(bead, moveDistances.bottomPx.roundToInt())
            2 -> animateBeadsDown(bead, moveDistances.bottomPx.roundToInt())
            4 -> animateBeadUp(bead)
            3 -> animateBeadDown(bead, moveDistances.topPx.roundToInt())
        }
    }

    private fun resolveMoveDistances(rootView: android.view.View): AbacusBeadMetrics.MoveDistancesPx {
        val cached = cachedDistancesByRoot[rootView]
        if (cached != null) return cached
        val computed = AbacusBeadMetrics.fromBarrierDistances(rootView, ratio = 1.0f)
            ?: AbacusBeadMetrics.MoveDistancesPx(
                bottomPx = AbacusBeadMetrics.bottomStepPx(fragment.requireContext()),
                topPx = AbacusBeadMetrics.topStepPx(fragment.requireContext())
            )
        cachedDistancesByRoot[rootView] = computed
        return computed
    }

    private fun animateBeadUp(bead: ImageView) {
        isAnimating = true
        val animationDuration = 300L
        val rootView = fragment.requireView()
        bead.setImageResource(R.drawable.soroban_bead)
        TutorialBeadDiagnostics.log(
            "type4 setDrawable=normal | ${TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId)}",
        )

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
                TutorialBeadDiagnostics.log(
                    "END type4 beadId=$beadId | ${TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId)}",
                )
            }
            .translationY(0f)  // Orijinal konumuna dön
            .start()
    }

    private fun animateBeadDown(bead: ImageView, moveDistance: Int) {
        isAnimating = true
        val animationDuration = 300L
        val rootView = fragment.requireView()

        bead.setImageResource(R.drawable.soroban_bead_selected)
        TutorialBeadDiagnostics.log(
            "type3 setDrawable=SELECTED dy=+$moveDistance | " +
                TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId),
        )
        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
                TutorialBeadDiagnostics.log(
                    "END type3 beadId=$beadId | ${TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId)}",
                )
            }
            .translationY(moveDistance.toFloat())
            .start()
    }

    private fun animateBeadsUp(bead: ImageView, moveDistance: Int) {
        isAnimating = true
        val animationDuration = 300L
        val rootView = fragment.requireView()

        val params = bead.layoutParams as ViewGroup.MarginLayoutParams
        val startMargin = params.bottomMargin
        val endMargin = startMargin + moveDistance
        bead.setImageResource(R.drawable.soroban_bead_selected)
        TutorialBeadDiagnostics.log(
            "type1 setDrawable=SELECTED margin $startMargin->$endMargin dy=-$moveDistance | " +
                TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId),
        )
        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                params.bottomMargin = endMargin
                bead.layoutParams = params
                bead.translationY = 0f // Translation'ı sıfırla
                isAnimating = false
                TutorialBeadDiagnostics.log(
                    "END type1 beadId=$beadId | ${TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId)}",
                )
            }
            .translationY(-moveDistance.toFloat())
            .start()
    }

    private fun animateBeadsDown(bead: ImageView, moveDistance: Int) {
        isAnimating = true
        val animationDuration = 300L
        val rootView = fragment.requireView()

        val params = bead.layoutParams as ViewGroup.MarginLayoutParams
        val startMargin = params.bottomMargin
        val endMargin = startMargin - moveDistance  // Yukarı çıktığı mesafe kadar aşağı in
        bead.setImageResource(R.drawable.soroban_bead)
        TutorialBeadDiagnostics.log(
            "type2 setDrawable=normal margin $startMargin->$endMargin dy=+$moveDistance | " +
                TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId),
        )

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                params.bottomMargin = endMargin
                bead.layoutParams = params
                bead.translationY = 0f // Translation'ı sıfırla
                isAnimating = false
                TutorialBeadDiagnostics.log(
                    "END type2 beadId=$beadId | ${TutorialBeadDiagnostics.beadState(rootView.context, rootView, beadId)}",
                )
            }
            .translationY(moveDistance.toFloat())  // Aşağı doğru hareket
            .start()
    }
}