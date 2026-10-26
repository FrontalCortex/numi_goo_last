package com.example.app.homework

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.app.AppStrings
import com.example.app.R
import com.example.app.appString
import com.example.app.databinding.FragmentRuleStatsBinding
import com.example.app.databinding.ItemRuleStatBinding
import com.example.app.databinding.ViewRuleProgressBinding
import com.example.app.homework.RuleProgress.Cw
import com.example.app.homework.RuleProgress.Range
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Ödev başarısı: homeworkRuleStats/{uid} ve günlük kayıtları (sunucu topluyor, bkz. [RuleAnalysis],
 * [RuleProgress] ve functions/index.js onHomeworkProgressWritten).
 *
 * Amaç "ilerlemişim" dedirtmek (kullanıcı kararı 26.10.2026); toplam oran yüzlerce sorudan sonra
 * kımıldamıyordu. Üç parça:
 *  1. Bu hafta: son 7 günün oranı, geçen 7 günle farkı ve şimdiye kadarki toplam.
 *  2. Başarı grafiği: konu seçenekleri ("Hepsi" = sorular; kurallar = işlemler), dönemlik doğru oranı,
 *     noktaların üstünde o ana kadarki toplam, altta 1 hafta / 1 ay / 3 ay / Tümü.
 *  3. Kural ustalığı: Başladı → Gelişiyor → Usta (son ~50 işlemde %70 / %90); aşama geri düşmüyor
 *     (sunucu `level`). Satıra dokununca grafik o kurala geçiyor.
 *
 * Verisi olmayan satır ve bölüm gösterilmiyor (kalabalık yapmasın; kullanıcı kararı).
 *
 * Veli panelinden (uid null: kendi hesabı) ve kurs öğretmeninden (öğrencinin uid'si) açılıyor; ikisi
 * de aynı ekranı görüyor. Okuma izni kurallarda (öğrencinin kendisi ve sınıf öğretmeni).
 */
class HomeworkRuleStatsFragment : Fragment() {

    companion object {
        private const val ARG_STUDENT_UID = "student_uid"
        private const val ARG_STUDENT_NAME = "student_name"

        // Kullanıcının istediği sıra; listede olmayan çiftler sona, basamağa göre
        private val MUL_ABACUS = listOf("1x1", "2x1", "2x2", "3x1", "3x2", "4x1")
        private val MUL_MENTAL = listOf("1x1", "2x1", "2x2", "3x1", "3x2", "3x3", "4x1", "4x2", "4x3", "5x1", "5x2", "6x1")

        /** Kalabalık olmasın: en çok bu kadar noktanın üstüne toplam yazılıyor (son noktadan geriye). */
        private const val MAX_POINT_LABELS = 7

        private const val LINE_COLOR = 0xFF1CB0F6.toInt()
        private const val GREEN = 0xFF8CC63F.toInt()
        private const val ORANGE = 0xFFFF9600.toInt()
        private const val RED = 0xFFFF4B4B.toInt()
        private const val GRAY = 0xFF52656D.toInt()

        fun newInstance(studentUid: String?, studentName: String) = HomeworkRuleStatsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_STUDENT_UID, studentUid)
                putString(ARG_STUDENT_NAME, studentName)
            }
        }
    }

    private var _binding: FragmentRuleStatsBinding? = null
    private val binding get() = _binding!!
    private var progress: ViewRuleProgressBinding? = null

    /** Satır: istatistik anahtarı, ad, toplam doğru/yanlış. */
    private data class Row(val key: String, val label: String, val cw: Cw)

    /** Grafikte bir nokta; dokununca [detail] yazılıyor. */
    private data class Point(val axisLabel: String, val detail: String, val cw: Cw, val cumulative: Int, val isBase: Boolean)

    private var tally: Map<String, Cw> = emptyMap()
    private var recent: Map<String, List<Cw>> = emptyMap()
    private var level: Map<String, Int> = emptyMap()
    private var baseQ: Cw? = null
    /** Türkiye günü (epoch) → kayıt. */
    private var days: Map<Int, RuleProgress.Day> = emptyMap()

    /** Grafik ve Bu hafta kartının konusu; null = Hepsi (sorular). */
    private var filter: String? = null
    private var range = Range.MONTH

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRuleStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        val studentUid = arguments?.getString(ARG_STUDENT_UID)
        if (studentUid != null) {
            binding.statsTitle.text = appString(R.string.rule_stats_title, arguments?.getString(ARG_STUDENT_NAME).orEmpty())
        }
        val uid = studentUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return showMessage(appString(R.string.ui_ilerleme_bilgisi_alinamadi))
        val ref = FirebaseFirestore.getInstance().collection("homeworkRuleStats").document(uid)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                // Günlük kayıt okunamazsa (ör. eski kurallar) ekran yine toplamlarla açılsın.
                val months = async { runCatching { ref.collection("months").get().await() }.getOrNull() }
                ref.get().await() to months.await()
            }
            if (_binding == null) return@launch
            result
                .onSuccess { (doc, months) -> parse(doc, months); render() }
                .onFailure { showMessage(appString(R.string.ui_ilerleme_bilgisi_alinamadi_internet_baglanti)) }
        }
    }

    // ── Veri ────────────────────────────────────────────────────────────────

    private fun parse(doc: DocumentSnapshot, months: QuerySnapshot?) {
        fun cwOf(v: Any?): Cw? {
            val m = v as? Map<*, *> ?: return null
            return Cw((m["c"] as? Number)?.toInt() ?: 0, (m["w"] as? Number)?.toInt() ?: 0)
        }
        fun <T> keyed(v: Any?, value: (Any?) -> T?): Map<String, T> =
            (v as? Map<*, *>).orEmpty().entries.mapNotNull { (k, x) -> (k as? String)?.let { key -> value(x)?.let { key to it } } }.toMap()

        tally = keyed(doc.get("tally")) { cwOf(it) }
        recent = keyed(doc.get("recent")) { v -> (v as? List<*>)?.mapNotNull { cwOf(it) } }
        level = keyed(doc.get("level")) { (it as? Number)?.toInt() }
        baseQ = cwOf((doc.get("base") as? Map<*, *>)?.get("q"))
        val out = HashMap<Int, RuleProgress.Day>()
        for (m in months?.documents.orEmpty()) {
            // Doküman kimliği "2026-10", gün anahtarı "26".
            val y = m.id.substringBefore('-').toIntOrNull() ?: continue
            val mo = m.id.substringAfter('-').toIntOrNull() ?: continue
            for ((dayKey, v) in (m.get("days") as? Map<*, *>).orEmpty()) {
                val d = (dayKey as? String)?.toIntOrNull() ?: continue
                val day = v as? Map<*, *> ?: continue
                out[RuleProgress.epochDay(y, mo, d)] = RuleProgress.Day(cwOf(day["q"]) ?: Cw.ZERO, keyed(day["t"]) { cwOf(it) })
            }
        }
        days = out
    }

    /** Konu sırası: toplama ve çıkarma kuralları, sonra abaküslü ve abaküssüz çarpma çiftleri. */
    private fun orderedKeys(): List<String> {
        // Sayımı sıfır olan anahtar (ör. geri alınmış örnek veri) seçeneklerde boş durmasın.
        val keys = tally.filterValues { it.total > 0 }.keys +
            days.values.flatMap { day -> day.t.filterValues { it.total > 0 }.keys }
        fun mulKeys(prefix: String, order: List<String>) = keys.filter { it.startsWith(prefix + "_") }.distinct()
            .sortedWith(compareBy({ order.indexOf(it.removePrefix(prefix + "_")).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it }))
        return listOf(HomeworkOp.ADD, HomeworkOp.SUB).flatMap { op ->
            AbacusRule.values().map { RuleAnalysis.ruleKey(op, it) }.filter { it in keys }
        } + mulKeys("mulA", MUL_ABACUS) + mulKeys("mulM", MUL_MENTAL)
    }

    private fun keyLabel(key: String): String {
        val op = HomeworkOp.fromKey(key.substringBefore('_'))
        val rule = AbacusRule.fromKey(key.substringAfter('_'))
        if (op != null && rule != null) {
            return appString(R.string.hw_title, HomeworkText.ruleLabel(rule),
                appString(if (op == HomeworkOp.ADD) R.string.hw_op_add_lower else R.string.hw_op_sub_lower))
        }
        val (d1, d2) = key.substringAfter('_').split("x").map { it.toIntOrNull() ?: 0 } + listOf(0, 0)
        return appString(if (key.startsWith("mulA")) R.string.rule_progress_chip_mul_abacus else R.string.rule_progress_chip_mul_mental, d1, d2)
    }

    /** Sorular ve çarpma (her soru bir deneme) "soru"; toplama/çıkarma kuralları "işlem". */
    private fun unit(key: String?): String =
        appString(if (key == null || key.startsWith("mul")) R.string.rule_progress_unit_question else R.string.rule_progress_unit_op)

    // ── Ekran ───────────────────────────────────────────────────────────────

    private fun render() {
        val b = _binding ?: return
        b.statsLoading.visibility = View.GONE
        if (tally.isEmpty() && days.values.none { it.q.total > 0 } && baseQ == null) {
            return showMessage(appString(R.string.rule_stats_empty))
        }
        b.statsContainer.removeAllViews()
        val p = ViewRuleProgressBinding.inflate(layoutInflater, b.statsContainer, true)
        progress = p
        setupChart(p)
        mapOf(p.rangeWeek to Range.WEEK, p.rangeMonth to Range.MONTH, p.rangeQuarter to Range.QUARTER, p.rangeAll to Range.ALL)
            .forEach { (view, r) ->
                view.setOnClickListener { range = r; renderProgress() }
                DepthPress.attach(view)
            }
        renderProgress()
        renderMastery(b.statsContainer)
    }

    private fun renderProgress() {
        val p = progress ?: return
        renderChips(p)
        listOf(p.rangeWeek to Range.WEEK, p.rangeMonth to Range.MONTH, p.rangeQuarter to Range.QUARTER, p.rangeAll to Range.ALL)
            .forEach { (view, r) -> selectOption(view, r == range) }
        renderWeek(p)
        renderChart(p)
    }

    private fun renderChips(p: ViewRuleProgressBinding) {
        p.filterChips.removeAllViews()
        val density = resources.displayMetrics.density
        (listOf<String?>(null) + orderedKeys()).forEachIndexed { i, key ->
            val chip = TextView(requireContext()).apply {
                text = if (key == null) appString(R.string.rule_progress_all_rules) else keyLabel(key)
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                // Yazı dudağın üstündeki yüzde ortalansın (bg_hw_chip, DepthPress).
                setPadding((14 * density).toInt(), 0, (14 * density).toInt(), (4 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (42 * density).toInt())
                    .apply { if (i > 0) marginStart = (8 * density).toInt() }
                setOnClickListener {
                    filter = key
                    renderProgress()
                }
            }
            selectOption(chip, key == filter)
            DepthPress.attach(chip)
            p.filterChips.addView(chip)
        }
    }

    /** Son 7 gün ile önceki 7 gün; seçili konuya göre. */
    private fun renderWeek(p: ViewRuleProgressBinding) {
        val today = RuleProgress.todayTr(System.currentTimeMillis())
        val thisWeek = RuleProgress.sum(days, filter, today - 6, today)
        val lastWeek = RuleProgress.sum(days, filter, today - 13, today - 7)
        val u = unit(filter)
        if (thisWeek.total == 0) {
            p.weekPercent.text = "—"
            p.weekPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text_secondary))
            p.weekDelta.visibility = View.GONE
            p.weekSub.text = if (lastWeek.total > 0) {
                appString(R.string.rule_progress_week_none) + "\n" +
                    appString(R.string.rule_progress_week_none_sub, lastWeek.pct, lastWeek.total, u)
            } else {
                appString(R.string.rule_progress_week_none)
            }
        } else {
            p.weekPercent.text = appString(R.string.rule_stats_percent, thisWeek.pct)
            p.weekPercent.setTextColor(percentColor(thisWeek.pct))
            p.weekSub.text = if (lastWeek.total > 0) appString(R.string.rule_progress_week_sub, thisWeek.total, u, lastWeek.pct)
            else appString(R.string.rule_progress_week_sub_no_last, thisWeek.total, u)
            if (lastWeek.total > 0) {
                val delta = thisWeek.pct - lastWeek.pct
                // Düşüş kırmızı değil turuncu: geçici bir düşüş çocuğu cezalandırıyor gibi görünmesin.
                val (text, color) = when {
                    delta > 0 -> appString(R.string.rule_progress_delta_up, delta) to GREEN
                    delta < 0 -> appString(R.string.rule_progress_delta_down, -delta) to ORANGE
                    else -> appString(R.string.rule_progress_delta_same) to GRAY
                }
                p.weekDelta.text = text
                p.weekDelta.background = pill(color)
                p.weekDelta.visibility = View.VISIBLE
            } else {
                p.weekDelta.visibility = View.GONE
            }
        }
        val start = RuleProgress.start(filter, baseQ, tally, days)?.total ?: 0
        val total = start + RuleProgress.sum(days, filter, Int.MIN_VALUE, Int.MAX_VALUE).total
        p.weekTotal.text = appString(R.string.rule_progress_total, total, u)
    }

    private fun setupChart(p: ViewRuleProgressBinding) {
        val chart = p.progressChart
        val secondary = ContextCompat.getColor(requireContext(), R.color.dark_text_secondary)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)
        chart.isDragEnabled = false
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        // Noktaların üstündeki toplamlar %100'de de sığsın.
        chart.extraTopOffset = 18f
        chart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 100f
            granularity = 25f
            setLabelCount(5, true)
            textColor = secondary
            gridColor = Color.parseColor("#1A3040")
            setDrawAxisLine(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = appString(R.string.rule_stats_percent, Math.round(value))
            }
        }
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = secondary
            setDrawGridLines(false)
            setDrawAxisLine(false)
            granularity = 1f
        }
        chart.setNoDataText(appString(R.string.rule_progress_chart_empty))
        chart.setNoDataTextColor(secondary)
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                (e?.data as? Point)?.let { p.chartSelection.text = it.detail }
            }

            override fun onNothingSelected() {
                p.chartSelection.text = appString(R.string.rule_progress_chart_hint, unit(filter))
            }
        })
    }

    private fun renderChart(p: ViewRuleProgressBinding) {
        val chart = p.progressChart
        val points = buildPoints()
        p.chartSelection.text = appString(R.string.rule_progress_chart_hint, unit(filter))
        chart.highlightValues(null)
        if (points.isEmpty()) {
            chart.clear()
            // Konu bu dönemde çalışılmadıysa (ör. çocuk kuralsızdan 10'luğa geçti) yalnızca "veri yok"
            // demek, hiç çalışılmamış gibi görünüyordu: en son çalışma ve oranı.
            days.entries.filter { RuleProgress.valueOf(it.value, filter).total > 0 }.maxByOrNull { it.key }?.let { (d, day) ->
                val cw = RuleProgress.valueOf(day, filter)
                val (_, m, dd) = RuleProgress.civil(d)
                p.chartSelection.text = appString(R.string.rule_progress_chart_empty_last, "$dd ${monthNames[m - 1]}", cw.pct)
            }
            return
        }
        val entries = points.mapIndexed { i, pt -> Entry(i.toFloat(), pt.cw.pct.toFloat(), pt) }
        val cardColor = ContextCompat.getColor(requireContext(), R.color.bright_background_color)
        val labelStep = (points.size + MAX_POINT_LABELS - 1) / MAX_POINT_LABELS
        val set = LineDataSet(entries, "").apply {
            mode = LineDataSet.Mode.LINEAR
            color = LINE_COLOR
            lineWidth = 2.5f
            circleRadius = 5f
            circleHoleRadius = 2.5f
            circleHoleColor = cardColor
            // Başlangıç gri; az soruyla hesaplanan dönem soluk (oranı güvenilmez).
            setCircleColors(points.map {
                when {
                    it.isBase -> GRAY
                    it.cw.total < RuleProgress.LOW_SAMPLE -> 0x801CB0F6.toInt()
                    else -> LINE_COLOR
                }
            })
            setDrawFilled(true)
            fillColor = LINE_COLOR
            fillAlpha = 35
            setDrawValues(true)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.dark_text)
            valueTextSize = 11f
            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    val i = entry?.x?.toInt() ?: return ""
                    // Son noktadan geriye her labelStep'te bir: en güncel toplam hep yazılı.
                    if ((points.size - 1 - i) % labelStep != 0) return ""
                    return (entry.data as? Point)?.cumulative?.toString().orEmpty()
                }
            }
            highLightColor = Color.WHITE
            setDrawHorizontalHighlightIndicator(false)
        }
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(points.map { it.axisLabel })
        chart.xAxis.labelCount = points.size.coerceIn(1, 6)
        // Tek noktada eksen ortalansın.
        chart.xAxis.axisMinimum = -0.5f
        chart.xAxis.axisMaximum = points.size - 0.5f
        chart.data = LineData(set)
        chart.invalidate()
    }

    private fun buildPoints(): List<Point> {
        val today = RuleProgress.todayTr(System.currentTimeMillis())
        val u = unit(filter)
        val out = mutableListOf<Point>()
        val start = RuleProgress.start(filter, baseQ, tally, days)
        if (start != null) {
            out += Point(appString(R.string.rule_progress_base),
                appString(R.string.rule_progress_base_detail, start.total, u, start.pct), start, start.total, isBase = true)
        }
        RuleProgress.buckets(days, filter, range, today, start?.total ?: 0).forEach { bucket ->
            val label = periodLabel(bucket)
            out += Point(axisLabel(bucket), appString(R.string.rule_progress_point_detail, label, bucket.cw.total, u, bucket.cw.pct, bucket.cumulative),
                bucket.cw, bucket.cumulative, isBase = false)
        }
        return out
    }

    private val monthNames: Array<String> by lazy { AppStrings.context().resources.getStringArray(R.array.rule_progress_months) }

    /** Eksende kısa: "26 Eki", haftada başlangıç günü, ayda "Eki". */
    private fun axisLabel(bucket: RuleProgress.Bucket): String {
        val (_, m, d) = RuleProgress.civil(bucket.from)
        return if (bucket.kind == RuleProgress.BucketKind.MONTH) monthNames[m - 1] else "$d ${monthNames[m - 1]}"
    }

    /** Ayrıntıda: "26 Eki", "12–18 Eki", "28 Eki–3 Kas", "Eki 2026". */
    private fun periodLabel(bucket: RuleProgress.Bucket): String {
        val (y, m, d) = RuleProgress.civil(bucket.from)
        return when (bucket.kind) {
            RuleProgress.BucketKind.DAY -> "$d ${monthNames[m - 1]}"
            RuleProgress.BucketKind.MONTH -> "${monthNames[m - 1]} $y"
            RuleProgress.BucketKind.WEEK -> {
                val (_, m2, d2) = RuleProgress.civil(bucket.to)
                if (m == m2) "$d–$d2 ${monthNames[m - 1]}" else "$d ${monthNames[m - 1]}–$d2 ${monthNames[m2 - 1]}"
            }
        }
    }

    // ── Kural ustalığı ──────────────────────────────────────────────────────

    private fun renderMastery(container: LinearLayout) {
        val ctx = requireContext()
        fun ruleRows(op: HomeworkOp) = AbacusRule.values().map { r ->
            val key = RuleAnalysis.ruleKey(op, r)
            Row(key, HomeworkText.ruleLabel(r), tally[key] ?: Cw.ZERO)
        }
        // Sıra orderedKeys'ten (kullanıcının istediği çarpma sırası).
        fun mulRows(prefix: String): List<Row> =
            orderedKeys().filter { it.startsWith(prefix + "_") }.map { key ->
                val (d1, d2) = key.removePrefix(prefix + "_").split("x").map { it.toIntOrNull() ?: 0 } + listOf(0, 0)
                Row(key, appString(R.string.rule_stats_mul_digits, d1, d2), tally[key] ?: Cw.ZERO)
            }
        val sections = listOf(
            appString(R.string.rule_stats_add) to ruleRows(HomeworkOp.ADD),
            appString(R.string.rule_stats_sub) to ruleRows(HomeworkOp.SUB),
            appString(R.string.rule_stats_mul_abacus) to mulRows("mulA"),
            appString(R.string.rule_stats_mul_mental) to mulRows("mulM"),
        ).map { (title, rows) -> title to rows.filter { it.cw.total > 0 } }.filter { it.second.isNotEmpty() }
        if (sections.isEmpty()) return

        container.addView(TextView(ctx).apply {
            text = appString(R.string.rule_stats_mastery_title)
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(4), dp(22), dp(4), 0)
        })
        container.addView(TextView(ctx).apply {
            text = appString(R.string.rule_stats_mastery_desc)
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text_secondary))
            textSize = 13f
            setPadding(dp(4), dp(2), dp(4), 0)
        })
        sections.forEach { (title, rows) -> container.addView(sectionCard(title, rows)) }
        container.addView(TextView(ctx).apply {
            text = appString(R.string.rule_stats_note)
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text_secondary))
            textSize = 12f
            setPadding(dp(4), dp(14), dp(4), 0)
        })
    }

    private fun sectionCard(title: String, rows: List<Row>): View {
        val ctx = requireContext()
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(8))
        }
        body.addView(TextView(ctx).apply {
            text = title
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        rows.forEach { row ->
            val item = ItemRuleStatBinding.inflate(layoutInflater, body, false)
            // Son ~50 işlem (sunucunun penceresi); penceresi olmayan eski veride toplam.
            val window = recent[row.key]?.takeIf { it.isNotEmpty() }?.fold(Cw.ZERO) { acc, x -> acc + x } ?: row.cw
            val stage = maxOf(level[row.key] ?: 0, RuleProgress.stage(window))
            val pct = window.pct
            val color = percentColor(pct)
            val u = unit(row.key)
            item.ruleName.text = row.label
            item.ruleStage.text = appString(
                when (stage) {
                    2 -> R.string.rule_stats_stage_master
                    1 -> R.string.rule_stats_stage_improving
                    else -> R.string.rule_stats_stage_started
                }
            )
            item.ruleStage.background = pill(
                when (stage) {
                    2 -> GREEN
                    1 -> ORANGE
                    else -> GRAY
                }
            )
            item.rulePercent.text = appString(R.string.rule_stats_percent, pct)
            item.rulePercent.setTextColor(color)
            item.ruleBar.setIndicatorColor(color)
            item.ruleBar.progress = pct
            item.ruleCount.text = if (window.total >= RuleProgress.MASTERY_WINDOW) appString(R.string.rule_stats_recent, window.total, u, pct)
            else appString(R.string.rule_stats_recent_few, window.total, u)
            // Satıra dokununca grafik bu kurala geçiyor.
            item.root.setOnClickListener {
                filter = row.key
                renderProgress()
                _binding?.statsScroll?.smoothScrollTo(0, 0)
            }
            body.addView(item.root)
        }
        return MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) }
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.bright_background_color))
            radius = dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = 0
            addView(body)
        }
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    private fun percentColor(pct: Int) = when {
        pct >= 80 -> GREEN
        pct >= 50 -> ORANGE
        else -> RED
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10).toFloat()
    }

    /** Ödev formundaki seçenek görünümü (HomeworkCreateFragment.selectOption ile aynı). */
    private fun selectOption(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_hw_chip_selected else R.drawable.bg_hw_chip)
        view.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(requireContext(), R.color.dark_text))
    }

    private fun showMessage(text: String) {
        val b = _binding ?: return
        b.statsLoading.visibility = View.GONE
        b.statsMessage.text = text
        b.statsMessage.visibility = View.VISIBLE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        progress = null
        _binding = null
        super.onDestroyView()
    }
}
