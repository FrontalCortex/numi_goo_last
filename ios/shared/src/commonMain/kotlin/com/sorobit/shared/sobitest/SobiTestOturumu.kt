// Kaynak: app/src/main/java/com/example/app/homework/HomeworkPlayerFragment.kt — Sobi testi kipinin saf
// kısmı: soruların taşınması (newSobiTest → startSobiTest), sayı etiketleri (startFlash: çıkarmada "−n"),
// cevabın değerlendirilmesi ve kural sayımı (onKontrol → countRules), "Sobi ile analiz et" kaydı
// (reviewLog, bindSobiReview'daki işaretli sayılar) ve testin sonu (finishSobiTest: satırlar, yeni
// seviyeler, cesaret cümlesi). HomeworkText.questionLine da burada (işlem satırı).
// Taşınmayan (platform işi): ekran, sayıların zamanlanması, ses, sandık, reklam kapısı,
// MissionsProgressStore sayaçları ve SobiDailyTest.saveLevels'ın sunucu çağrısı.
package com.sorobit.shared.sobitest

import com.sorobit.shared.sobianaliz.HomeworkOp
import com.sorobit.shared.sobianaliz.SobiMetinCozucu
import com.sorobit.shared.sobianaliz.SobiRuleStats
import com.sorobit.shared.sobianaliz.SobiRuleStatsStore

/**
 * Bir Sobi testinin oynanışı. [stats] doluysa her cevabın kural sayımı oraya da yazılır (Android:
 * SobiRuleStats.add(tally, question = correct); dersler ve Sobi ile aynı toplam).
 * Soru sırası [index] 0'dan; geçersiz sıra verilmemelidir (Swift'te Kotlin istisnası çöker), bu yüzden
 * her soru işlevi sırayı [questionCount] içine kırpar.
 */
class SobiTestOturumu(val plan: SobiTestPlanner.Plan, private val stats: SobiRuleStatsStore?) {

    /** Abaküssüz kural "m_" önekiyle: sonda seviyesi abaküslüden ayrı hesaplanıp yazılır. */
    val weakKeys: List<String> = (plan.weak + plan.mentalWeak).map { it.key }

    val questionCount: Int get() = plan.questions.size

    var correctCount: Int = 0
        private set

    var answeredCount: Int = 0
        private set

    private val blockTally = LinkedHashMap<String, IntArray>()

    private fun q(index: Int): SobiTestPlanner.Question =
        plan.questions[index.coerceIn(0, plan.questions.size - 1)]

    fun isMultiplication(index: Int): Boolean = q(index).question.op == HomeworkOp.MUL

    fun isSubtraction(index: Int): Boolean = q(index).question.op == HomeworkOp.SUB

    /** Soru abaküslü mü (Android questionWithAbacus: sobiMental[i] != true). */
    fun withAbacus(index: Int): Boolean = !q(index).mental

    /** Sayıların arası (seviyeden; çarpmada kullanılmaz). */
    fun intervalMs(index: Int): Long = q(index).intervalMs

    fun answer(index: Int): Long = q(index).question.answer

    fun numbers(index: Int): List<Long> = q(index).question.numbers

    /** Tahtada sırayla gösterilen etiketler: çıkarmada başlangıçtan sonrakiler "−n". */
    fun flashLabels(index: Int): List<String> {
        val question = q(index).question
        return question.numbers.mapIndexed { i, n -> if (question.op == HomeworkOp.SUB && i > 0) "−$n" else n.toString() }
    }

    /** HomeworkText.questionLine: "12 + 7 + 5", "48 − 3", "23 × 4". */
    fun questionLine(index: Int): String = questionLineOf(q(index).question)

    /** Cevaptan sonra tahtada kalan: çarpmada "23 × 4 = 92", ötekilerde yalnızca cevap. */
    fun revealText(index: Int): String {
        val question = q(index).question
        return if (question.op == HomeworkOp.MUL) questionLineOf(question) + " = " + question.answer else question.answer.toString()
    }

    /** Yanlışta panelde işlemin tamamı: "12 + 7 + 5 = 24". */
    fun fullLine(index: Int): String {
        val question = q(index).question
        return questionLineOf(question) + " = " + question.answer
    }

    /**
     * Cevap (Android onKontrol + countRules). [logTimesMs]/[logValues] abaküs değer kaydı (aynı uzunlukta;
     * ilk kayıt sorunun başındaki değer), [shownAtMs] her sayının göründüğü an. Abaküssüzde kayıtlar boş.
     * Döner: doğru mu.
     */
    fun submit(index: Int, given: Long, logTimesMs: List<Long>, logValues: List<Long>, shownAtMs: List<Long>): Boolean {
        val item = q(index)
        val question = item.question
        val correct = given == question.answer
        answeredCount++
        if (correct) correctCount++
        val withAbacus = !item.mental
        val tally: Map<String, IntArray> = runCatching {
            if (question.op == HomeworkOp.MUL) {
                val key = SobiTestPlanner.mulKeyOf(question.numbers[0], question.numbers[1], withAbacus)
                mapOf(key to if (correct) intArrayOf(1, 0) else intArrayOf(0, 1))
            } else if (!withAbacus) {
                SobiRuleStats.mentalTally(SobiTestPlanner.rulesOf(question), correct)
            } else {
                val n = minOf(logTimesMs.size, logValues.size)
                val log = (0 until n).map { RuleAnalysis.Sample(logTimesMs[it], logValues[it]) }
                RuleAnalysis.tally(RuleAnalysis.abacus(question, log, shownAtMs, given))
            }
        }.getOrDefault(emptyMap())
        for ((key, cw) in tally) {
            val sum = blockTally.getOrPut(key) { IntArray(2) }
            sum[0] += cw[0]
            sum[1] += cw[1]
        }
        stats?.add(tally, correct)
        return correct
    }

    /** "Sobi ile analiz et" düğmesi bu soruda var mı (Android bindSobiReview: yalnızca yanlışta, çarpmada yok). */
    fun canReview(index: Int, correct: Boolean): Boolean = !correct && !isMultiplication(index)

    /** Analiz penceresinin sayıları: çıkarılanlar eksi işaretli. */
    fun reviewNumbers(index: Int): List<Int> {
        val question = q(index).question
        return question.numbers.mapIndexed { i, n -> if (question.op == HomeworkOp.SUB && i > 0) -n.toInt() else n.toInt() }
    }

    /**
     * Android reviewLog: abaküsün değer kaydı ve her sayının göründüğü anki değerin sırası (`values[marks[k]]`
     * k. sayı göründüğünde). İşaretler sayı adediyle eşleşmiyorsa boş verilir (Android: marks.size == numbers.size).
     */
    fun review(index: Int, logTimesMs: List<Long>, logValues: List<Long>, shownAtMs: List<Long>): SobiTestInceleme {
        if (!withAbacus(index)) return SobiTestInceleme(emptyList(), emptyList())
        val n = minOf(logTimesMs.size, logValues.size)
        val values = ArrayList<Int>()
        val marks = ArrayList<Int>()
        var si = 0
        for (t in shownAtMs) {
            while (si < n && logTimesMs[si] <= t) values += logValues[si++].toInt()
            values += values.lastOrNull() ?: 0
            marks += values.lastIndex
        }
        while (si < n) values += logValues[si++].toInt()
        val numberCount = q(index).question.numbers.size
        return SobiTestInceleme(values, if (marks.size == numberCount) marks else emptyList())
    }

    /** Test bitti (Android finishSobiTest): kural satırları, yeni seviyeler, cesaret cümlesi. */
    fun finish(metin: SobiMetinCozucu): SobiTestSonucu {
        val newLevels = LinkedHashMap<String, Int>()
        val lines = weakKeys.mapNotNull { key ->
            val cw = blockTally[key] ?: return@mapNotNull null
            val total = cw[0] + cw[1]
            if (total == 0) return@mapNotNull null
            val perQuestion = SobiTestPlanner.isMentalKey(key) || SobiTestPlanner.isMulKey(key)
            val line = metin.metin(
                if (perQuestion) "sobi_test_rule_line_questions" else "sobi_test_rule_line",
                listOf(SobiDailyTest.ruleLabel(key, metin), total, cw[0]),
            )
            if (!SobiTestPlanner.hasLevel(key)) return@mapNotNull line
            val before = plan.current[key] ?: 1
            val played = plan.played[key] ?: 1
            val after = SobiTestLevels.next(before, played, cw[0] * 100 / total)
            newLevels[key] = after
            val levelText = when {
                after > before -> metin.metin("sobi_test_level_up", listOf(before, after))
                after < before -> metin.metin("sobi_test_level_down", listOf(after))
                else -> metin.metin("sobi_test_level_same", listOf(after))
            }
            line + "\n" + levelText
        }
        val total = plan.questions.size
        val pct = if (total == 0) 0 else correctCount * 100 / total
        val good = pct >= 80
        val cheer = metin.metin(if (good) "sobi_test_cheer_good" else "sobi_test_cheer_try", emptyList())
        return SobiTestSonucu(
            correct = correctCount,
            total = total,
            message = (lines + cheer).joinToString("\n"),
            newLevels = newLevels,
            perfect = total > 0 && correctCount == total,
        )
    }

    companion object {
        fun questionLineOf(q: HomeworkQuestion): String = when (q.op) {
            HomeworkOp.ADD -> q.numbers.joinToString(" + ")
            HomeworkOp.SUB -> q.numbers.joinToString(" − ")
            HomeworkOp.MUL -> "${q.numbers[0]} × ${q.numbers[1]}"
        }
    }
}

/** "Sobi ile analiz et" kaydı: [values] abaküs değerleri, [marks] sayıların göründüğü sıra (boş olabilir). */
data class SobiTestInceleme(val values: List<Int>, val marks: List<Int>)

/**
 * Testin sonu. [message]: kural satırları (seviye değişimiyle) ve cesaret cümlesi, satır satır.
 * [newLevels]: cihaza ve sunucuya (recordSobiLevels) yazılacak seviyeler. [perfect]: hepsi doğru
 * (Android: MissionsProgressStore.recordPerfectStepIncrementProgress).
 */
data class SobiTestSonucu(
    val correct: Int,
    val total: Int,
    val message: String,
    val newLevels: Map<String, Int>,
    val perfect: Boolean,
)
