package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app.ParentReportRepository.LessonRow
import com.example.app.ParentReportRepository.Report
import com.example.app.ParentReportRepository.StepRow
import com.example.app.ParentReportRepository.StepState
import com.example.app.databinding.ItemParentDayBarBinding
import com.example.app.databinding.ItemParentLessonBinding
import com.example.app.databinding.ItemParentPartBinding
import com.example.app.databinding.ItemParentSectionBinding
import com.example.app.databinding.ItemParentStepBinding
import com.example.app.databinding.ItemParentSummaryBinding

/**
 * Veli panelinin listesi.
 *
 * ## Neden ExpandableListView değil
 * `ExpandableListView` iki seviyeyle sınırlı, `RecyclerView` ekosisteminin dışında ve özet kartı
 * gibi ekstra satır tiplerini taşıyamıyor. Bunun yerine ağaç **düz bir listeye** açılır ve satır
 * tipi `viewType` ile ayrılır; ders satırına dokunulunca yalnızca o dersin adımları listeye
 * girer/çıkar. Yeni bir satır tipi eklemek (ör. haftalık grafik) tek bir `when` dalı demek.
 *
 * ## Yeni ders eklendiğinde
 * Burada hiçbir şey değişmez. Adapter yalnızca [ParentReportRepository]'nin verdiği satırları
 * çizer; müfredata ders eklemek o listeyi otomatik büyütür.
 */
class ParentReportAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_SUMMARY = 0
        const val TYPE_PART = 1
        const val TYPE_SECTION = 2
        const val TYPE_LESSON = 3
        const val TYPE_STEP = 4
    }

    /** Ekrana çizilen düz satır listesi; [rebuild] ile üretilir. */
    private sealed class Row {
        data class Summary(val report: Report) : Row()
        /** "Bölüm 1" — part başına bir kez. */
        data class Part(val partId: Int) : Row()
        /** "Kuralsız Toplama" — bölümün altındaki ünite. */
        data class Section(val title: String) : Row()
        data class Lesson(val lesson: LessonRow, val expanded: Boolean) : Row()
        data class Step(val step: StepRow) : Row()
    }

    private var report: Report? = null
    /** Açık ders kimlikleri. stableId kullanılır: liste değişse de açık kalan ders kaymaz. */
    private val expanded = HashSet<String>()
    private var rows: List<Row> = emptyList()

    fun submit(report: Report) {
        this.report = report
        expanded.clear()
        rebuild()
        notifyDataSetChanged()
    }

    private fun rebuild() {
        val r = report
        if (r == null) {
            rows = emptyList()
            return
        }
        val out = ArrayList<Row>(r.lessons.size + 8)
        out += Row.Summary(r)
        var lastSection: String? = null
        var lastPart = -1
        r.lessons.forEach { lesson ->
            val section = lesson.sectionTitle
            // Bölüm başlığı part başına BİR kez; ünite başlıkları onun altına girer.
            if (lesson.partId != lastPart) {
                out += Row.Part(lesson.partId)
                lastPart = lesson.partId
                lastSection = null
            }
            if (section != lastSection) {
                // Üniteyi olmayan (başlıksız) ders için boş satır basmanın anlamı yok.
                if (!section.isNullOrBlank()) out += Row.Section(section.uppercase(TR))
                lastSection = section
            }
            val isOpen = lesson.stableId in expanded
            out += Row.Lesson(lesson, isOpen)
            if (isOpen) lesson.steps.forEach { out += Row.Step(it) }
        }
        rows = out
    }

    private fun toggle(stableId: String) {
        val index = rows.indexOfFirst { it is Row.Lesson && it.lesson.stableId == stableId }
        if (index < 0) return
        val stepCount = (rows[index] as Row.Lesson).lesson.steps.size

        val wasOpen = expanded.remove(stableId)
        if (!wasOpen) expanded.add(stableId)
        rebuild()

        notifyItemChanged(index) // ok yönü
        if (wasOpen) notifyItemRangeRemoved(index + 1, stepCount)
        else notifyItemRangeInserted(index + 1, stepCount)
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Summary -> TYPE_SUMMARY
        is Row.Part -> TYPE_PART
        is Row.Section -> TYPE_SECTION
        is Row.Lesson -> TYPE_LESSON
        is Row.Step -> TYPE_STEP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SUMMARY -> SummaryHolder(ItemParentSummaryBinding.inflate(inflater, parent, false))
            TYPE_PART -> PartHolder(ItemParentPartBinding.inflate(inflater, parent, false))
            TYPE_SECTION -> SectionHolder(ItemParentSectionBinding.inflate(inflater, parent, false))
            TYPE_LESSON -> LessonHolder(ItemParentLessonBinding.inflate(inflater, parent, false))
            else -> StepHolder(ItemParentStepBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Summary -> (holder as SummaryHolder).bind(row.report)
            is Row.Part -> (holder as PartHolder).bind(row.partId)
            is Row.Section -> (holder as SectionHolder).bind(row.title)
            is Row.Lesson -> (holder as LessonHolder).bind(row.lesson, row.expanded)
            is Row.Step -> (holder as StepHolder).bind(row.step)
        }
    }

    // ── ViewHolder'lar ──────────────────────────────────────────────────────

    private class SummaryHolder(val b: ItemParentSummaryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(report: Report) {
            val s = report.summary
            b.summaryFinishedValue.text = s.finishedLessons.toString()
            b.summaryTimeValue.text = formatDuration(s.todaySeconds)
            b.summaryStreakValue.text = s.streakDays.toString()

            bindWeekStrip(s.lastWeek)

            val note = buildString {
                append("Müfredattaki ${s.totalLessons} dersten ${s.finishedLessons} tanesi tamamlandı.")
                append(" Toplam çalışma süresi ${formatDuration(s.totalTimeSeconds)}.")
                s.hardestLesson?.let {
                    append(" En çok denemeyi gerektiren ders: ${it.title} (${it.totalKnownAttempts} deneme).")
                }
            }
            b.summaryNote.text = note
        }

        /**
         * 7 sütunu tek tek kurar. Sütunlar `layout_weight` ile eşit paylaşıldığı için genişlik
         * ekrana göre kendini ayarlar; yükseklik o haftanın EN YÜKSEK gününe göre ölçeklenir —
         * mutlak bir eksen olmadığı için "az mı çok mu" değil, "hangi gün daha çok" okunur.
         */
        private fun bindWeekStrip(week: List<ParentReportRepository.DayTime>) {
            val strip = b.summaryWeekStrip
            val inflater = LayoutInflater.from(strip.context)
            strip.removeAllViews()
            if (week.isEmpty()) return

            val maxSeconds = week.maxOf { it.seconds }
            val density = strip.resources.displayMetrics.density
            val trackPx = (TRACK_HEIGHT_DP * density).toInt()
            val minPx = (MIN_BAR_HEIGHT_DP * density).toInt()

            week.forEachIndexed { index, day ->
                val bar = ItemParentDayBarBinding.inflate(inflater, strip, false)
                bar.dayBarLabel.text = day.label
                // 60 saniyenin altındaki bir gün "0dk" yazmasın: kısa da olsa çalışılmış.
                bar.dayBarValue.text =
                    if (day.seconds > 0) "${(day.seconds / 60).coerceAtLeast(1L)}dk" else ""

                val fillParams = bar.dayBarFill.layoutParams
                fillParams.height = when {
                    day.seconds <= 0L || maxSeconds <= 0L -> minPx
                    else -> minPx + ((trackPx - minPx) * day.seconds / maxSeconds).toInt()
                }
                bar.dayBarFill.layoutParams = fillParams

                val ctx = strip.context
                val isToday = index == week.lastIndex
                val colorRes = when {
                    day.seconds <= 0L -> R.color.parent_step_not_reached
                    isToday -> R.color.lesson_header_blue
                    else -> R.color.parent_step_passed
                }
                bar.dayBarFill.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))

                strip.addView(
                    bar.root,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
            }
        }
    }

    private class PartHolder(val b: ItemParentPartBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(partId: Int) {
            b.partTitle.text = "Bölüm $partId"
        }
    }

    private class SectionHolder(val b: ItemParentSectionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(title: String) {
            b.root.text = title
        }
    }

    private inner class LessonHolder(val b: ItemParentLessonBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(lesson: LessonRow, isExpanded: Boolean) {
            b.lessonTitle.text = lesson.title
            b.lessonSubtitle.text = lessonSubtitle(lesson)

            val dotColor = if (lesson.isFinished) R.color.parent_step_passed else R.color.parent_step_in_progress
            tintDot(b.lessonStatusDot, ContextCompat.getColor(b.root.context, dotColor))

            // Kapalıyken sağa, açıkken aşağı bakar: durum tek bakışta anlaşılsın.
            b.lessonChevron.rotation = if (isExpanded) 90f else 0f
            b.root.setOnClickListener { toggle(lesson.stableId) }
        }

        private fun lessonSubtitle(lesson: LessonRow): String {
            val stepWord = "${lesson.stepCount} adım"
            if (!lesson.isFinished) {
                return "Devam ediyor · ${lesson.passedSteps}/${lesson.stepCount} adım geçildi"
            }
            val known = lesson.steps.count { it.attempts != null }
            return when {
                // Adımlardan biri bile ölçümsüzse toplam deneme sayısı eksik kalır; eksik bir
                // sayı söylemektense hiç söylememek daha dürüst.
                known < lesson.stepCount -> "Bitti · $stepWord"
                lesson.totalKnownAttempts == lesson.stepCount ->
                    "Bitti · $stepWord, hepsi ilk denemede"
                else -> "Bitti · $stepWord, toplam ${lesson.totalKnownAttempts} deneme"
            }
        }
    }

    private class StepHolder(val b: ItemParentStepBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(step: StepRow) {
            val ctx = b.root.context
            b.stepLabel.text = "Adım ${step.step}"

            val colorRes = when (step.state) {
                StepState.PASSED -> R.color.parent_step_passed
                StepState.IN_PROGRESS -> R.color.parent_step_in_progress
                StepState.NOT_REACHED -> R.color.parent_step_not_reached
            }
            val color = ContextCompat.getColor(ctx, colorRes)
            tintDot(b.stepDot, color)
            b.stepDetail.setTextColor(color)
            b.stepDetail.text = stepDetail(step)

            // Ulaşılmamış adım soluk: "yapılmadı" değil, "sırası gelmedi".
            val reached = step.state != StepState.NOT_REACHED
            b.stepLabel.alpha = if (reached) 1f else 0.55f
            b.root.alpha = if (reached) 1f else 0.75f
        }

        private fun stepDetail(step: StepRow): String = when (step.state) {
            StepState.PASSED -> when (step.attempts) {
                null -> "Tamamlandı"
                1 -> "İlk denemede geçti"
                else -> "${step.attempts}. denemede geçti"
            }
            StepState.IN_PROGRESS -> when {
                step.attempts == null || step.attempts == 0 -> "Başlandı"
                else -> "Deniyor · ${step.attempts} başarısız deneme"
            }
            StepState.NOT_REACHED -> "Sırası gelmedi"
        }
    }
}

/** Gün sütununun ray yüksekliği; item_parent_day_bar.xml ile aynı olmalı. */
private const val TRACK_HEIGHT_DP = 52

/** Sıfır dakikalık gün de görünsün diye taban yükseklik. */
private const val MIN_BAR_HEIGHT_DP = 4

/** Türkçe büyük harf dönüşümü için locale (I/İ sorunu). */
private val TR = java.util.Locale("tr", "TR")

/**
 * Ortak durum noktasını renklendirir. Drawable [android.graphics.drawable.Drawable.mutate] ile
 * kopyalanır; aksi halde tüm satırlar aynı sabit durumu paylaşır ve son renk hepsine yayılır.
 */
private fun tintDot(view: View, color: Int) {
    val d = view.background?.mutate() ?: return
    d.setTint(color)
    view.background = d
}

/** `12345 sn` → `3s 25dk`. Veli için saat/dakika tek başına yeterli; saniye gürültü. */
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0dk"
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    val rem = minutes % 60
    return when {
        hours <= 0L -> "${minutes}dk"
        rem == 0L -> "${hours}s"
        else -> "${hours}s ${rem}dk"
    }
}
