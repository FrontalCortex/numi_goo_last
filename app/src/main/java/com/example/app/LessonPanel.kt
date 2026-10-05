package com.example.app

import android.app.Activity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout

/**
 * Haritadaki ders panelini ([LessonAdapter.showLessonBottomSheet], lesson_popover) dışarıdan
 * kapatmak için. Panel kökü coordinator_layout'ta "bottom_sheet" etiketiyle duruyor; kapatma
 * işi (animasyon, karartma, rekor nabzını durdurma) kökün [R.id.lessonPanelCard] anahtarlı
 * etiketinde saklı.
 */
object LessonPanel {
    const val TAG = "bottom_sheet"

    fun dismiss(activity: Activity) {
        val coordinator = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout) ?: return
        val root = coordinator.findViewWithTag<View>(TAG) ?: return
        @Suppress("UNCHECKED_CAST")
        (root.getTag(R.id.lessonPanelCard) as? () -> Unit)?.invoke()
            ?: coordinator.removeView(root)
    }
}
