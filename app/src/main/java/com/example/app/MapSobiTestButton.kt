package com.example.app

import android.content.SharedPreferences
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 *
 * Part kartları ekranında da ([PartSelectionFragment]; kullanıcı kararı 01.11.2026) aynı düğme: orada
 * [anchor] yok, düğme kökün sağ alt köşesinde. Balonun günlük hakkı iki ekranda ortak
 * ([SobiDailyTest.shouldNudgeToday]); hangisinde önce görünürse öbüründe o gün çıkmıyor.
 *
 * @param root ConstraintLayout ya da FrameLayout.
 * @param anchor Haritadaki "Öğretmene sor" düğmesi: Sobi onun üstünde duruyor. Yoksa sağ alt köşe.
 */
class MapSobiTestButton(private val fragment: Fragment, private val root: ViewGroup, anchor: View? = null) {

    private val button: View
    private val dot: View
    private var nudge: SobiNudge? = null
    private var sheet: SobiTestSheet? = null
    /**
     * Test bitince nokta hemen sönsün. Test başka bir katmanda (abacusFragmentContainer) açılıyor,
     * alttaki ekran durmuyor: dönüşte onResume gelmiyor. SharedPreferences dinleyiciyi zayıf
     * tuttuğu için alan olarak saklanıyor.
     */
    private val testsListener: SharedPreferences.OnSharedPreferenceChangeListener

    init {
        val ctx = root.context
        val d = ctx.resources.displayMetrics.density
        button = LayoutInflater.from(ctx).inflate(R.layout.view_map_sobi_test, root, false)
        dot = button.findViewById(R.id.mapSobiTestDot)
        val size = (SIZE_DP * d).toInt()
        val margin = (16 * d).toInt()
        val params = if (root !is ConstraintLayout) {
            FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.END).apply {
                marginEnd = margin
                bottomMargin = margin
            }
        } else ConstraintLayout.LayoutParams(size, size).apply {
            if (anchor == null) {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = margin
                bottomMargin = margin
            } else if (TeacherQuestionsFeature.ENABLED) {
                endToEnd = anchor.id
                startToStart = anchor.id
                bottomToTop = anchor.id
                bottomMargin = (12 * d).toInt()
            } else {
                // "Öğretmene sor" gizli (GONE): ConstraintLayout onu sağ alt köşede bir noktaya
                // indiriyor, ona ortalanan düğme yarı yarıya ekran dışına taşardı. Onun yerine
                // geçiyor: aynı alt boşluk, aynı dikey eksen (fragment_map.xml: 60dp, 16dp boşluk).
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                val anchorSize = ctx.resources.getDimensionPixelSize(R.dimen.map_fab_size)
                marginEnd = margin + (anchorSize - size) / 2
                bottomMargin = margin
            }
        }
        // Part 1'in 6. dersi bitene dek gizli ([SobiDailyTest.checkUnlocked]; refresh açıyor).
        button.visibility = View.GONE
        root.addView(button, params)
        // Haritadaki öbür düğmelerle aynı katmanda: liste kartlarının üstünde.
        if (anchor != null) button.elevation = anchor.elevation
        DepthPress.attach(button.findViewById(R.id.mapSobiTestFace))
        button.findViewById<View>(R.id.mapSobiTestFace).setOnClickListener { open() }
        testsListener = SobiDailyTest.addTestsListener(ctx) { updateDot() }
        refresh()
    }

    /**
     * Ekran her göründüğünde: düğme açıldı mı, bugünün testi bitti mi, balon gösterilsin mi.
     * @param nudge false: yalnızca görünürlük ve nokta (haritada ders dönüşü; o anda ekranlar kuyrukta).
     */
    fun refresh(nudge: Boolean = true) {
        if (!fragment.isAdded) return
        SobiDailyTest.checkUnlocked(root.context) { unlocked ->
            if (fragment.isAdded) apply(unlocked, nudge)
        }
    }

    private fun apply(unlocked: Boolean, nudge: Boolean) {
        button.visibility = if (unlocked) View.VISIBLE else View.GONE
        if (!unlocked) {
            this.nudge?.hide()
            return
        }
        val done = updateDot()
        if (nudge && !done && SobiDailyTest.shouldNudgeToday(fragment.requireContext())) {
            button.postDelayed({
                if (!fragment.isAdded || !button.isShown) return@postDelayed
                val n = this.nudge ?: SobiNudge(root).also { this.nudge = it }
                n.show(button, appString(R.string.sobi_test_nudge)) { open() }
            }, NUDGE_DELAY_MS)
        }
    }

    private fun updateDot(): Boolean {
        val done = SobiDailyTest.isDoneToday(root.context)
        dot.visibility = if (done) View.GONE else View.VISIBLE
        return done
    }

    fun release() {
        SobiDailyTest.removeTestsListener(root.context, testsListener)
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
