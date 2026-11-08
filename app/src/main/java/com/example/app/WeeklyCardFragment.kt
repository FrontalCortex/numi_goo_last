package com.example.app

import android.app.Dialog
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.app.WeeklyCardRules.Item
import com.example.app.abacus.SobiTestLevels
import com.example.app.databinding.FragmentWeeklyCardBinding
import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp
import com.example.app.homework.HomeworkText
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat

/**
 * HAFTALIK İLERLEME KARTI: "geçen hafta → bu hafta", somut önce/sonra (bkz. [WeeklyCardRules]).
 *
 * Önce özet (ilk ekran; ileride paylaşılacak görsel yalnızca bu kart), aşağı kaydırınca ayrıntı.
 * Özet: başlık + tarih, büyük punto ana satır (haftanın en büyük tek ilerlemesi, önce/sonra ve iki
 * çubuk), 2–3 küçük satır, emek satırı, Sobi'nin tek cümlelik önerisi. Ayrıntıda yalnızca ilerleme
 * olan bölümler.
 *
 * İki yoldan açılıyor (kullanıcı kararı 08.11.2026):
 *  - İlerleme sayfası ([ProgressHubFragment]) → normal sayfa, geri tuşu yığından çıkarır.
 *  - Haftanın ilk açılışında haritada bir kez ([showAuto], MainActivity) → tam ekran pencere. Ayrı
 *    pencere, çünkü haritanın sayfa yığınına girmek harita dönüşü ve ders sonu akışının varsaydığı
 *    "tabanda MapFragment var" durumunu bozardı.
 *
 * "Paylaş" düğmesi görünür ama devre dışı ("yakında"; kullanıcı kararı); paylaşım ayrı iş. İlerleme
 * yoksa düğme hiç görünmüyor: kötü hafta paylaşılmaz.
 */
class WeeklyCardFragment : DialogFragment() {

    companion object {
        const val TAG = "WeeklyCard"

        /** Haritadan otomatik açılış; veri önceden okunmuş (ilerleme olduğu bilindiği için açılıyor). */
        fun showAuto(fm: FragmentManager, data: WeeklyCardRepository.Data) {
            WeeklyCardFragment().apply { preloaded = data }.show(fm, TAG)
        }

        private const val ACCENT = 0xFF8CC63F.toInt()
        private const val GOLD = 0xFFFFC94D.toInt()
        private const val TRACK = 0xFF3D4A50.toInt()
    }

    private var _binding: FragmentWeeklyCardBinding? = null
    private val binding get() = _binding!!

    /** Otomatik açılışta okunmuş veri; ekran döndürülünce kaybolursa yeniden okunur. */
    private var preloaded: WeeklyCardRepository.Data? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pencere olarak açıldığında da uygulama teması (yazı tipi, renkler); tam ekran ve çerçevesiz.
        setStyle(STYLE_NO_FRAME, R.style.Theme_NumiGoo)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState).apply {
        window?.setBackgroundDrawableResource(R.color.background_color)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeeklyCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            if (showsDialog) dismissAllowingStateLoss() else parentFragmentManager.popBackStack()
        }
        val data = preloaded
        if (data != null) {
            render(data)
            AnalyticsLogger.logWeeklyCardShown(auto = true, hasProgress = data.card.hasProgress)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { WeeklyCardRepository.load() }
            if (_binding == null) return@launch
            result
                .onSuccess { d ->
                    if (d == null) showMessage(appString(R.string.weekly_card_empty))
                    else {
                        render(d)
                        AnalyticsLogger.logWeeklyCardShown(auto = false, hasProgress = d.card.hasProgress)
                    }
                }
                .onFailure { showMessage(appString(R.string.ui_ilerleme_bilgisi_alinamadi_internet_baglanti)) }
        }
    }

    private fun showMessage(text: String) {
        binding.weeklyLoading.visibility = View.GONE
        binding.weeklyMessage.text = text
        binding.weeklyMessage.visibility = View.VISIBLE
    }

    // ── Çizim ───────────────────────────────────────────────────────────────

    private fun render(d: WeeklyCardRepository.Data) {
        binding.weeklyLoading.visibility = View.GONE
        val root = binding.weeklyContent
        root.removeAllViews()
        val card = d.card
        val self = d.speaksToChild

        // Özet kartı.
        val summary = panel()
        summary.addView(text(
            when {
                self -> appString(R.string.weekly_card_title_self)
                d.firstName.isNotBlank() -> appString(R.string.weekly_card_title_other, d.firstName)
                else -> appString(R.string.weekly_card_title_fallback)
            }, 22f, bold = true,
        ))
        summary.addView(text(dates(card.week), 13f, secondary = true, top = 2))

        val head = card.headline
        if (head != null) {
            headline(summary, head, d)
            if (card.extras.isNotEmpty()) {
                summary.addView(divider())
                card.extras.forEach { summary.addView(text(smallLine(it, d), 15f, top = 6)) }
            }
        } else {
            summary.addView(text(appString(if (self) R.string.weekly_card_no_progress_self else R.string.weekly_card_no_progress_other), 16f, top = 16))
        }

        summary.addView(divider())
        summary.addView(text(effortLine(card.effort, self), 15f, bold = true))
        summary.addView(text(suggestion(d), 14f, color = GOLD, top = 8))
        root.addView(summary)

        if (head != null) {
            // Paylaşım ayrı iş; yeri şimdiden ayrılmış, basılamaz (kullanıcı kararı: görünür, devre dışı).
            root.addView(TextView(requireContext()).apply {
                text = appString(R.string.weekly_card_share_soon)
                gravity = Gravity.CENTER
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.dark_text_secondary))
                background = rounded(TRACK, 24)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                isEnabled = false
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(12) }
            })
        }

        details(root, d)
    }

    /** Büyük punto ana satır: ne, önce, sonra ve iki çubuk. */
    private fun headline(parent: LinearLayout, item: Item, d: WeeklyCardRepository.Data) {
        when (item) {
            is Item.CupStep -> {
                parent.addView(text(cupTitle(item.field), 15f, color = ACCENT, bold = true, top = 18))
                beforeAfter(parent, cupLevelText(item.before), cupLevelText(item.after),
                    item.beforeScore.toFloat() / item.afterScore, 1f, big = true)
                parent.addView(text(appString(R.string.weekly_card_cup_points, item.beforeScore, item.afterScore), 12f, secondary = true, top = 4))
            }
            is Item.SobiStep -> {
                parent.addView(text(appString(R.string.weekly_card_sobi_title, ruleLabel(item.key)), 15f, color = ACCENT, bold = true, top = 18))
                beforeAfter(parent, sobiSpecText(item.before), sobiSpecText(item.after),
                    item.before.toFloat() / SobiTestLevels.MAX, item.after.toFloat() / SobiTestLevels.MAX, big = true)
                parent.addView(text(appString(R.string.weekly_card_sobi_points, item.before, item.after), 12f, secondary = true, top = 4))
            }
            is Item.ChestStep -> {
                parent.addView(text(appString(R.string.weekly_card_chest_title, unitOf(d, item.partId, item.stableId)), 15f, color = ACCENT, bold = true, top = 18))
                // Süre kısaldıkça çubuk da kısalıyor: "daha az zaman" gözle görülsün.
                beforeAfter(parent, WeeklyCardRules.mmss(item.firstSec), WeeklyCardRules.mmss(item.bestSec),
                    1f, item.bestSec.toFloat() / item.firstSec, big = true,
                    beforeLabel = R.string.weekly_card_chest_before, afterLabel = R.string.weekly_card_chest_after)
            }
            is Item.Learned -> {
                parent.addView(text(learnedText(item, d), 22f, bold = true, color = ACCENT, top = 18))
                sampleText(item, d.speaksToChild)?.let { parent.addView(text(it, 15f, top = 6)) }
                val more = d.card.learned.size - 1
                if (more > 0) parent.addView(text(appString(R.string.weekly_card_learned_more, more), 13f, secondary = true, top = 4))
            }
        }
    }

    /** "Geçen hafta  [██░░]  1 basamaklı 4–6 sayı" / "Bu hafta [████] 2 BASAMAKLI…". */
    private fun beforeAfter(
        parent: LinearLayout,
        before: String,
        after: String,
        beforeFrac: Float,
        afterFrac: Float,
        big: Boolean,
        beforeLabel: Int = R.string.weekly_card_before,
        afterLabel: Int = R.string.weekly_card_after,
    ) {
        parent.addView(text(appString(beforeLabel), 12f, secondary = true, top = 10))
        parent.addView(text(before, if (big) 16f else 14f, secondary = true))
        parent.addView(bar(beforeFrac, TRACK + 0x00202020))
        parent.addView(text(appString(afterLabel), 12f, secondary = true, top = 10))
        parent.addView(text(after, if (big) 22f else 16f, bold = true))
        parent.addView(bar(afterFrac, ACCENT))
    }

    private fun bar(frac: Float, color: Int): View {
        val f = frac.coerceIn(0.04f, 1f)
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)).apply { topMargin = dp(4) }
            background = rounded(TRACK, 4)
            addView(View(context).apply {
                background = rounded(color, 4)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f)
            })
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - f) })
        }
    }

    /** Özetteki küçük satır: tek satırda ne + önce → sonra. */
    private fun smallLine(item: Item, d: WeeklyCardRepository.Data): String = when (item) {
        is Item.CupStep -> "🏆 ${cupTitle(item.field)}: ${cupLevelText(item.after)}"
        is Item.SobiStep -> "▲ " + appString(R.string.weekly_card_sobi_title, ruleLabel(item.key)) + ": " +
            appString(R.string.weekly_card_sobi_points, item.before, item.after)
        is Item.ChestStep -> "⏱ " + appString(R.string.weekly_card_chest_title, unitOf(d, item.partId, item.stableId)) +
            ": ${WeeklyCardRules.mmss(item.firstSec)} → ${WeeklyCardRules.mmss(item.bestSec)}"
        is Item.Learned -> "★ " + learnedText(item, d)
    }

    // ── Ayrıntı ─────────────────────────────────────────────────────────────

    private fun details(root: LinearLayout, d: WeeklyCardRepository.Data) {
        val card = d.card
        if (!card.hasProgress) return
        root.addView(text(appString(R.string.weekly_card_details), 18f, bold = true, top = 28))

        if (card.cups.isNotEmpty()) section(root, R.string.weekly_card_section_cups) { p ->
            val max = card.cups.maxOf { it.afterScore }.toFloat()
            card.cups.forEachIndexed { i, c ->
                p.addView(text(cupTitle(c.field), 15f, bold = true, color = ACCENT, top = if (i == 0) 0 else 14))
                beforeAfter(p, cupLevelText(c.before), cupLevelText(c.after), c.beforeScore / max, c.afterScore / max, big = false)
                p.addView(text(appString(R.string.weekly_card_cup_points, c.beforeScore, c.afterScore), 12f, secondary = true, top = 4))
            }
        }

        if (card.learned.isNotEmpty()) section(root, R.string.weekly_card_section_lessons) { p ->
            card.learned.forEachIndexed { i, l ->
                p.addView(text(learnedText(l, d), 15f, bold = true, top = if (i == 0) 0 else 12))
                d.lessons[WeeklyCardRepository.key(l.partId, l.stableId)]?.title?.let { p.addView(text(it, 13f, secondary = true)) }
                sampleText(l, d.speaksToChild)?.let { p.addView(text(it, 14f, top = 2)) }
            }
            d.parts.forEach { part ->
                p.addView(text(appString(R.string.weekly_card_part_progress, part.name ?: "", part.finishedLessons, part.totalLessons), 13f, secondary = true, top = 12))
                p.addView(bar(part.finishedLessons.toFloat() / part.totalLessons.coerceAtLeast(1), ACCENT))
            }
        }

        if (card.chests.isNotEmpty()) section(root, R.string.weekly_card_section_chests) { p ->
            card.chests.forEachIndexed { i, c ->
                p.addView(text(appString(R.string.weekly_card_chest_title, unitOf(d, c.partId, c.stableId)), 15f, bold = true, color = ACCENT, top = if (i == 0) 0 else 14))
                beforeAfter(p, WeeklyCardRules.mmss(c.firstSec), WeeklyCardRules.mmss(c.bestSec), 1f, c.bestSec.toFloat() / c.firstSec,
                    big = false, beforeLabel = R.string.weekly_card_chest_before, afterLabel = R.string.weekly_card_chest_after)
            }
        }

        if (card.sobi.isNotEmpty()) section(root, R.string.weekly_card_section_sobi) { p ->
            card.sobi.forEachIndexed { i, s ->
                p.addView(text(ruleLabel(s.key), 15f, bold = true, color = ACCENT, top = if (i == 0) 0 else 14))
                beforeAfter(p, sobiSpecText(s.before), sobiSpecText(s.after),
                    s.before.toFloat() / SobiTestLevels.MAX, s.after.toFloat() / SobiTestLevels.MAX, big = false)
                p.addView(text(appString(R.string.weekly_card_sobi_points, s.before, s.after), 12f, secondary = true, top = 4))
            }
        }

        // Kural ustalığı en altta, Sobi'nin önerisi olarak (kullanıcı kararı).
        if (d.suggestion != null) section(root, R.string.weekly_card_section_rules) { p ->
            p.addView(text(suggestion(d), 15f, color = GOLD))
        }
    }

    private fun section(root: LinearLayout, title: Int, fill: (LinearLayout) -> Unit) {
        val p = panel()
        p.addView(text(appString(title), 17f, bold = true).apply { setPadding(0, 0, 0, dp(10)) })
        fill(p)
        root.addView(p)
    }

    // ── Metinler ────────────────────────────────────────────────────────────

    private fun cupTitle(field: String): String = appString(
        when (WeeklyCardRules.CUP_FIELDS.indexOf(field)) {
            0 -> R.string.panel_cup_path_card1_title
            1 -> R.string.panel_cup_path_card2_title
            2 -> R.string.panel_cup_path_card3_title
            3 -> R.string.panel_cup_path_card4_title
            4 -> R.string.panel_cup_path_card5_title
            else -> R.string.panel_cup_path_card6_title
        }
    )

    private fun seconds(ms: Long): String =
        NumberFormat.getNumberInstance(locale()).apply { maximumFractionDigits = 1; minimumFractionDigits = 1 }.format(ms / 1000.0)

    private fun cupLevelText(l: WeeklyCardRules.CupLevel): String {
        l.mulSecondDigits?.let { return appString(R.string.weekly_card_level_mul, l.digits, it) }
        val base = appString(R.string.weekly_card_level_digits, l.digits, WeeklyCardRules.countRange(l.countMin, l.countMax))
        return if (l.intervalMs > 0) "$base · ${appString(R.string.weekly_card_level_interval, seconds(l.intervalMs))}" else base
    }

    private fun sobiSpecText(level: Int): String {
        val s = SobiTestLevels.spec(level)
        return appString(R.string.weekly_card_level_digits, s.digits, s.count.toString()) + " · " +
            appString(R.string.weekly_card_level_interval, seconds(s.intervalMs))
    }

    /** Ödev başarısı ekranındaki kural adıyla aynı ("5'lik toplama", "… · zihinden"). */
    private fun ruleLabel(key: String): String {
        if (key.startsWith(WeeklyCardRules.MENTAL_PREFIX)) {
            return appString(R.string.sobi_test_rule_mental, ruleLabel(key.removePrefix(WeeklyCardRules.MENTAL_PREFIX)))
        }
        val op = HomeworkOp.fromKey(key.substringBefore('_'))
        val rule = AbacusRule.fromKey(key.substringAfter('_'))
        if (op == null || rule == null) return key
        return appString(R.string.hw_title, HomeworkText.ruleLabel(rule),
            appString(if (op == HomeworkOp.ADD) R.string.hw_op_add_lower else R.string.hw_op_sub_lower))
    }

    private fun unitOf(d: WeeklyCardRepository.Data, partId: Int, stableId: String): String {
        val info = d.lessons[WeeklyCardRepository.key(partId, stableId)]
        return info?.unit ?: info?.title ?: ""
    }

    private fun learnedText(l: Item.Learned, d: WeeklyCardRepository.Data): String =
        appString(if (d.speaksToChild) R.string.weekly_card_learned_self else R.string.weekly_card_learned_other, unitOf(d, l.partId, l.stableId))

    private fun sampleText(l: Item.Learned, self: Boolean): String? {
        val op = l.sampleOp ?: return null
        val shown = if (l.blinding) appString(R.string.weekly_card_sample_blinding, op) else op
        return appString(if (self) R.string.weekly_card_sample_self else R.string.weekly_card_sample_other, shown)
    }

    private fun effortLine(e: WeeklyCardRules.Effort, self: Boolean): String = listOfNotNull(
        appString(if (self) R.string.weekly_card_effort_days_self else R.string.weekly_card_effort_days_other, e.days),
        appString(R.string.weekly_card_effort_minutes, e.minutes),
        e.streak.takeIf { it > 0 }?.let { appString(R.string.weekly_card_effort_streak, it) },
    ).joinToString(" · ")

    private fun suggestion(d: WeeklyCardRepository.Data): String =
        d.suggestion?.let { appString(R.string.weekly_card_suggest_next, ruleLabel(it)) }
            ?: appString(R.string.weekly_card_suggest_generic)

    private fun locale() = resources.configuration.locales[0]

    private fun dates(s: WeeklyCardRules.Snapshot): String {
        val fmt = SimpleDateFormat("d MMM", locale()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val parse = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        return runCatching {
            appString(R.string.weekly_card_dates, fmt.format(parse.parse(s.weekStart)!!), fmt.format(parse.parse(s.weekEnd)!!))
        }.getOrDefault("${s.weekStart} – ${s.weekEnd}")
    }

    // ── Görünüm yardımcıları ────────────────────────────────────────────────

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun panel() = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(ContextCompat.getColor(context, R.color.bright_background_color), 20)
        setPadding(dp(18), dp(18), dp(18), dp(18))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(12) }
    }

    private fun divider() = View(requireContext()).apply {
        setBackgroundColor(TRACK)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(16); bottomMargin = dp(10)
        }
    }

    private fun text(
        s: String,
        sizeSp: Float,
        bold: Boolean = false,
        secondary: Boolean = false,
        color: Int? = null,
        top: Int = 0,
    ) = TextView(requireContext()).apply {
        text = s
        textSize = sizeSp
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setTextColor(color ?: ContextCompat.getColor(context, if (secondary) R.color.dark_text_secondary else R.color.dark_text))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(top) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
