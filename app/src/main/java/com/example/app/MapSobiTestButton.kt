package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.app.abacus.SobiTestLevels
import com.example.app.abacus.SobiTestPlanner
import com.example.app.homework.DepthPress

/**
 * Haritadaki Sobi düğmesi: Sobi'nin günlük testi ([SobiDailyTest]; kullanıcı kararı 01.11.2026).
 *
 * Neden ders listesinin içinde bir kart değil: liste (LessonAdapter) ve ders sonu dönüşü uzun uğraşla
 * oturtuldu; oraya dokunmamak için düğme listenin üstünde, "Öğretmene sor"un hemen üstünde duruyor.
 * Bugünün testi bekliyorsa köşesinde nokta var ve günde bir kez "Senin için bir test hazırladım!" balonu
 * çıkıyor ([SobiNudge]). Dokununca hangi kuralların (seviyeleriyle) çalışılacağı ve zorluk seçimi
 * gösteriliyor; "Başla" soruları üretip oynatıcıyı açıyor. Bugünün testi bittiyse "Bir test daha"
 * (kullanıcı kararı: ek test de sandıklı). Veri yoksa "Biraz ders çöz".
 */
class MapSobiTestButton(private val fragment: Fragment, private val root: ConstraintLayout, anchor: View) {

    private val button: View
    private val dot: View
    private var nudge: SobiNudge? = null
    private var opening = false

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
        nudge?.hide()
        nudge = null
        (button.parent as? android.view.ViewGroup)?.removeView(button)
    }

    private fun open() {
        if (opening || !fragment.isAdded) return
        nudge?.hide()
        val ctx = fragment.requireContext()
        opening = true
        val loading = AlertDialog.Builder(ctx).setMessage(appString(R.string.sobi_test_loading)).setCancelable(true).show()
        SobiDailyTest.load(ctx) { info ->
            opening = false
            loading.dismiss()
            if (!fragment.isAdded) return@load
            val choice = SobiTestPlanner.choose(info.scores, info.levels)
            if (choice == null) {
                message(appString(R.string.sobi_test_no_data))
                return@load
            }
            chooseDifficulty(info, choice)
        }
    }

    /**
     * Başlamadan zorluk: Kolay (seviyenin bir altı), Benim seviyem, Zor (bir üstü). Her seçeneğin altında
     * ne demek olduğu (testin ilk kuralının seviyesiyle: "2 basamaklı 3 sayı, 2,4 sn"). Bugünün testi
     * bittiyse "Bir test daha" (kullanıcı kararı: ek testlerde de sandık).
     */
    private fun chooseDifficulty(info: SobiDailyTest.Info, choice: SobiTestPlanner.Choice) {
        val ctx = fragment.requireContext()
        val d = ctx.resources.displayMetrics.density
        val extra = SobiDailyTest.isDoneToday(ctx)
        val rules = choice.weak.map { appString(R.string.sobi_test_rule_level, SobiDailyTest.ruleLabel(it.key), choice.current.getValue(it.key)) }
        val joined = if (rules.size > 1) rules.dropLast(1).joinToString(", ") + " ve " + rules.last() else rules.first()
        val base = choice.current.getValue(choice.weak.first().key)

        val box = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((24 * d).toInt(), (8 * d).toInt(), (24 * d).toInt(), 0)
        }
        box.addView(android.widget.TextView(ctx).apply {
            text = appString(if (extra) R.string.sobi_test_dialog_extra else R.string.sobi_test_dialog_message, joined)
            textSize = 15f
        })
        val group = android.widget.RadioGroup(ctx).apply { setPadding(0, (10 * d).toInt(), 0, 0) }
        val options = listOf(
            -1 to R.string.sobi_test_diff_easy,
            0 to R.string.sobi_test_diff_mine,
            1 to R.string.sobi_test_diff_hard,
        )
        for ((offset, label) in options) {
            val level = (base + offset).coerceIn(1, SobiTestLevels.MAX)
            group.addView(android.widget.RadioButton(ctx).apply {
                id = View.generateViewId()
                tag = offset
                text = appString(R.string.sobi_test_diff_option, appString(label), level, SobiDailyTest.specText(level))
                isChecked = offset == 0
                // Seviye 1'de "Kolay" ve 20'de "Zor" aynı seviyeye düşüyor: anlamsız seçenek gösterme.
                isEnabled = offset == 0 || level != base
            })
        }
        box.addView(group)

        AlertDialog.Builder(ctx)
            .setTitle(appString(if (extra) R.string.sobi_test_dialog_extra_title else R.string.sobi_test_dialog_title))
            .setView(box)
            .setPositiveButton(appString(R.string.sobi_test_dialog_start)) { _, _ ->
                val offset = group.findViewById<View>(group.checkedRadioButtonId)?.tag as? Int ?: 0
                startTest(info, offset)
            }
            .setNegativeButton(appString(R.string.sobi_test_dialog_later), null)
            .show()
    }

    private fun startTest(info: SobiDailyTest.Info, offset: Int) {
        val ctx = fragment.context ?: return
        val loading = AlertDialog.Builder(ctx).setMessage(appString(R.string.sobi_test_loading)).setCancelable(false).show()
        SobiDailyTest.planAsync(ctx, info, offset) { plan ->
            loading.dismiss()
            if (!fragment.isAdded) return@planAsync
            if (plan == null) {
                message(appString(R.string.sobi_test_no_data))
                return@planAsync
            }
            (fragment.activity as? MainActivity)?.let { SobiDailyTest.start(it, plan) }
        }
    }

    private fun message(text: String) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(appString(R.string.sobi_test_dialog_title))
            .setMessage(text)
            .setPositiveButton(appString(R.string.sobi_test_ok), null)
            .show()
    }

    private companion object {
        const val SIZE_DP = 56
        const val NUDGE_DELAY_MS = 1200L
    }
}
