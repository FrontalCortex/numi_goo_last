package com.example.app

import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout.LayoutParams
import androidx.core.content.ContextCompat

/** Genişlik hazırsa anında uygular (animasyon kareleri için). */
fun applyMissionProgressOverlayNow(
    widthHost: View,
    fill: View,
    shine: View,
    percent: Int,
    done: Boolean,
    claimed: Boolean = false,
) {
    applyMissionProgressOverlayNow(widthHost, fill, shine, percent.toFloat().coerceIn(0f, 100f), done, claimed)
}

/** [percent] 0–100 arası ondalık; dolgu kare kare yumuşar. */
fun applyMissionProgressOverlayNow(
    widthHost: View,
    fill: View,
    shine: View,
    percent: Float,
    done: Boolean,
    claimed: Boolean = false,
) {
    val w = widthHost.width
    if (w <= 0) return
    val res = widthHost.resources
    val ctx = widthHost.context
    val p = percent.coerceIn(0f, 100f)
    fill.background = ContextCompat.getDrawable(
        ctx,
        when {
            claimed -> R.drawable.mission_progress_fill_claimed
            done -> R.drawable.mission_progress_fill_gold
            else -> R.drawable.mission_progress_fill_blue
        },
    )
    shine.background = ContextCompat.getDrawable(
        ctx,
        when {
            claimed -> R.drawable.mission_progress_shine_claimed
            done -> R.drawable.mission_progress_shine_gold
            else -> R.drawable.mission_progress_shine_blue
        },
    )
    val startInset = res.getDimensionPixelSize(R.dimen.mission_progress_shine_inset_start)
    val endGap = res.getDimensionPixelSize(R.dimen.mission_progress_shine_gap_end)
    val fillW = (w * p / 100f).toInt().coerceIn(0, w)
    val fillLp = fill.layoutParams as LayoutParams
    fillLp.width = fillW
    fill.layoutParams = fillLp
    fill.visibility = if (fillW > 0) View.VISIBLE else View.GONE
    if (claimed) {
        shine.visibility = View.GONE
    } else {
        val shineW = (fillW - startInset - endGap).coerceAtLeast(0)
        val shineLp = shine.layoutParams as LayoutParams
        shineLp.width = shineW
        shine.layoutParams = shineLp
        shine.visibility = if (shineW > 0) View.VISIBLE else View.GONE
    }
}

/**
 * İlk ölçümde genişlik 0 olabilir; [ViewTreeObserver] veya post ile tekrar dener.
 */
fun applyMissionProgressOverlay(
    widthHost: View,
    fill: View,
    shine: View,
    percent: Int,
    done: Boolean,
    claimed: Boolean = false,
) {
    fun layoutOverlay() {
        applyMissionProgressOverlayNow(widthHost, fill, shine, percent.toFloat(), done, claimed)
    }
    widthHost.post {
        if (widthHost.width > 0) {
            layoutOverlay()
        } else {
            val observer = widthHost.viewTreeObserver
            observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (widthHost.width <= 0) return
                    observer.removeOnGlobalLayoutListener(this)
                    layoutOverlay()
                }
            })
        }
    }
}
