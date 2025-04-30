package com.example.numigoo.model

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.numigoo.R

class BeadAnimation(
    private val fragment: Fragment,
    private val beadId: String,
    private val animationType: Int
) {
    private var isAnimating = false

    fun getAnimationType(): Int = animationType
    fun getBeadId(): String = beadId

    fun isAnimating(): Boolean {
        return isAnimating
    }

    fun animate() {
        if (isAnimating) return

        val rootView = fragment.requireView()
        val bead = rootView.findViewById<ImageView>(
            rootView.resources.getIdentifier(beadId, "id", rootView.context.packageName)
        )
        if (bead == null) {
            Log.e("BeadAnimation", "Bead not found: $beadId")
            return
        }

        isAnimating = true
        when (animationType) {
            1 -> animateBeadsUp(bead)
            2 -> animateBeadsDown(bead)
            4 -> animateBeadUp(bead)
            3 -> animateBeadDown(bead)
        }
    }

    private fun animateBeadUp(bead: ImageView) {
        isAnimating = true
        val animationDuration = 300L
        val moveDistance = 90
        bead.setImageResource(R.drawable.soroban_bead)

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
            }
            .translationY(0f)  // Orijinal konumuna dön
            .start()
    }

    private fun animateBeadDown(bead: ImageView) {
        isAnimating = true
        val animationDuration = 300L
        val moveDistance = 90

        bead.setImageResource(R.drawable.soroban_bead_selected)
        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
            }
            .translationY(moveDistance.toFloat())
            .start()
    }

    private fun animateBeadsUp(bead: ImageView) {
        isAnimating = true
        val animationDuration = 300L
        val moveDistance = 135

        val params = bead.layoutParams as ViewGroup.MarginLayoutParams
        val startMargin = params.bottomMargin
        val endMargin = startMargin + moveDistance
        bead.setImageResource(R.drawable.soroban_bead_selected)
        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                params.bottomMargin = endMargin
                bead.layoutParams = params
                bead.translationY = 0f // Translation'ı sıfırla
                isAnimating = false
            }
            .translationY(-moveDistance.toFloat())
            .start()
    }

    private fun animateBeadsDown(bead: ImageView) {
        isAnimating = true
        val animationDuration = 300L
        val moveDistance = 135

        val params = bead.layoutParams as ViewGroup.MarginLayoutParams
        val startMargin = params.bottomMargin
        val endMargin = startMargin - moveDistance  // Yukarı çıktığı mesafe kadar aşağı in
        bead.setImageResource(R.drawable.soroban_bead)

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                params.bottomMargin = endMargin
                bead.layoutParams = params
                bead.translationY = 0f // Translation'ı sıfırla
                isAnimating = false
            }
            .translationY(moveDistance.toFloat())  // Aşağı doğru hareket
            .start()
    }
}