package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.app.homework.DepthPress

/**
 * Haritadaki Sobi düğmesi: Sobi'nin günlük testi ([SobiDailyTest]; kullanıcı kararı 01.11.2026).
 *
 * Neden ders listesinin içinde bir kart değil: liste (LessonAdapter) ve ders sonu dönüşü uzun uğraşla
 * oturtuldu; oraya dokunmamak için düğme listenin üstünde, "Öğretmene sor"un hemen üstünde duruyor.
 * Bugünün testi bekliyorsa köşesinde nokta var ve günde bir kez "Senin için bir test hazırladım!" balonu
 * çıkıyor ([SobiNudge]). Dokununca Sobi'nin testi paneli ([SobiTestSheet]) açılıyor.
 */
class MapSobiTestButton(private val fragment: Fragment, private val root: ConstraintLayout, anchor: View) {

    private val button: View
    private val dot: View
    private var nudge: SobiNudge? = null
    private var sheet: SobiTestSheet? = null

    init {
        val ctx = root.context
        val d = ctx.resources.displayMetrics.density
        button = LayoutInflater.from(ctx).inflate(R.layout.view_map_sobi_test, root, false)
        dot = button.findViewById(R.id.mapSobiTestDot)
        root.addView(button, ConstraintLayout.LayoutParams((SIZE_DP * d).toInt(), (SIZE_DP * d).toInt()).apply {
            endToEnd = anchor.id
            startToStart = anchor.id
            bottomToTop = anchor.id
            bottomMargin = (12 * d).toInt()
        })
        // Haritadaki öbür düğmelerle aynı katmanda: liste kartlarının üstünde.
        button.elevation = anchor.elevation
        DepthPress.attach(button.findViewById(R.id.mapSobiTestFace))
        button.findViewById<View>(R.id.mapSobiTestFace).setOnClickListener { open() }
        refresh()
    }

    /** Harita her göründüğünde: bugünün testi bitti mi, balon gösterilsin mi. */
    fun refresh() {
        if (!fragment.isAdded) return
        val done = SobiDailyTest.isDoneToday(fragment.requireContext())
        dot.visibility = if (done) View.GONE else View.VISIBLE
        if (!done && SobiDailyTest.shouldNudgeToday(fragment.requireContext())) {
            button.postDelayed({
                if (!fragment.isAdded || !button.isShown) return@postDelayed
                val n = nudge ?: SobiNudge(root).also { nudge = it }
                n.show(button, appString(R.string.sobi_test_nudge)) { open() }
            }, NUDGE_DELAY_MS)
        }
    }

    fun release() {
        sheet?.dismiss()
        sheet = null
        nudge?.hide()
        nudge = null
        (button.parent as? android.view.ViewGroup)?.removeView(button)
    }

    private fun open() {
        if (!fragment.isAdded) return
        nudge?.hide()
        (sheet ?: SobiTestSheet(fragment).also { sheet = it }).show()
    }

    private companion object {
        const val SIZE_DP = 56
        const val NUDGE_DELAY_MS = 1200L
    }
}
