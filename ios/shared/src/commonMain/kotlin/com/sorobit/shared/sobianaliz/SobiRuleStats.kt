// Kaynak: app/src/main/java/com/example/app/SobiRuleStats.kt — saf kısım: kural sayımı (tallyOf,
// movesOf, moveRules, mentalTally), SharedPreferences'a yazılan anahtarlar (`ok_`, `bad_`, `p_ok_`,
// `p_bad_`, `p_q_ok`, `p_q_bad`; dosya "sobi_rule_stats"), snapshot ve sunucuya gidecek yükün
// hesabı. Ayrıca abacus/SobiTestPlanner.kt'den mentalKey ("m_" öneki) ve mulKeyOf
// (homework/RuleAnalysis.mulKey).
// Taşınmayan: Handler ile 4 sn sonra gönderim ve Cloud Function çağrısı (recordRuleTally). Bunun
// yerine [SobiRuleStatsStore.pendingTally] gönderilecek yükü, [SobiRuleStatsStore.markSent] başarılı
// gönderimden sonra düşülecek sayıları verir; çağrıyı platform yapar (Android'deki sıra: yük →
// çağrı → başarılıysa düş; gönderilen bir değer SEND_MAX'a eşitse yeniden gönder).
package com.sorobit.shared.sobianaliz

import com.sorobit.shared.oyun.OyunKeyValueStore
import kotlin.math.abs

/**
 * KURAL HAFIZASI — çocuğun her kuralda kaç doğru, kaç yanlış yaptığı.
 *
 * İki yere gider: Sobi'nin bağlamı ([SobiRuleStatsStore.snapshot]; bu cihazdaki toplam) ve veli
 * panelindeki "Kural başarısı" (sunucuda recordRuleTally → homeworkRuleStats/{uid}; bekleyen sayım
 * `p_` önekli anahtarlarda).
 *
 * Sayım: her basamak işlemi gerektirdiği kurala sayılır (elde zinciri dahil). Yanlış cevapta yalnızca
 * hatanın düştüğü işlem yanlış sayılır ([MistakeAnalyzer] buldu ise); bulunamazsa kural sayılmaz.
 */
object SobiRuleStats {

    /** Android: SharedPreferences dosyasının adı. */
    const val PREFS = "sobi_rule_stats"

    /** Android: FLUSH_DELAY_MS — son kayıttan bu kadar sonra gönderilir. */
    const val FLUSH_DELAY_MS = 4000L

    /** Sunucunun çağrı başına sınırı (homework.EXTRA_TALLY_MAX). */
    const val SEND_MAX = 200

    /** Cloud Function adı. */
    const val FUNCTION_NAME = "recordRuleTally"

    /** SobiTestPlanner.MENTAL_PREFIX */
    const val MENTAL_PREFIX = "m_"

    val BASE_KEYS: List<String> =
        listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")

    /**
     * Çarpma çiftleri (büyük çarpan önce): abaküslü 4×1'e, zihinden 6×1'e kadar. Sunucu süzgeci
     * mul[AM]_[1-8]x[1-8].
     */
    val MUL_KEYS: List<String>
        get() = (1..6).flatMap { d1 -> (1..minOf(d1, 3)).flatMap { d2 -> listOf("mulA_${d1}x$d2", "mulM_${d1}x$d2") } }

    /** Abaküslü ve abaküssüz ("m_add_ten"); sunucu süzgeci functions/homework.js EXTRA_TALLY_KEY. */
    val KEYS: List<String>
        get() = BASE_KEYS + BASE_KEYS.map { mentalKey(it) } + MUL_KEYS

    /** SobiTestPlanner.mentalKey */
    fun mentalKey(rule: String): String = MENTAL_PREFIX + rule

    /** SobiTestPlanner.mulKeyOf + RuleAnalysis.mulKey. */
    fun mulKeyOf(a: Long, b: Long, withAbacus: Boolean): String {
        val d = listOf(abs(a).toString().length, abs(b).toString().length).sortedDescending()
        return "${if (withAbacus) "mulA" else "mulM"}_${d[0]}x${d[1]}"
    }

    /** Hamlenin kuralları, elde zinciri dahil: "add_bead", "add_ten". */
    fun moveRules(m: StepExplainer.ColumnMove): List<String> {
        val prefix = if (m.op == HomeworkOp.ADD) "add_" else "sub_"
        return StepExplainer.rulesUsed(m).map { prefix + it.key }
    }

    /** [rules] öneksiz kurallar ("add_ten"); sonuç "m_add_ten" → (1, 0) ya da (0, 1). */
    fun mentalTally(rules: Collection<String>, correct: Boolean): Map<String, IntArray> =
        rules.associate { mentalKey(it) to if (correct) intArrayOf(1, 0) else intArrayOf(0, 1) }

    fun tallyOf(moves: List<StepExplainer.ColumnMove>, correct: Boolean): Map<String, IntArray> {
        val out = LinkedHashMap<String, IntArray>()
        for (m in moves) for (k in moveRules(m)) {
            val cw = out.getOrPut(k) { IntArray(2) }
            if (correct) cw[0]++ else cw[1]++
        }
        return out
    }

    /** Dizinin (ilk sayı yazılmış, sonra ± sayılar) hamleleri. Abaküse sığmıyorsa boş. */
    fun movesOf(numbers: List<Int>): List<StepExplainer.ColumnMove> {
        if (numbers.size < 2) return emptyList()
        val out = ArrayList<StepExplainer.ColumnMove>()
        var running = numbers[0].toLong()
        for (n in numbers.drop(1)) {
            if (n == 0) continue
            val e = StepExplainer.explain(running, if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD, abs(n).toLong())
                ?: return emptyList()
            out += e.moves
            running = e.result
        }
        return out
    }
}

/** Android: SobiRuleStats.Entry */
data class SobiRuleStatEntry(val key: String, val correct: Int, val wrong: Int)

/**
 * Sunucuya gidecek yük (Android: `hashMapOf("tally" to tally, "q" to mapOf("c" to qc, "w" to qw))`).
 * [tally]: anahtar → (c, w). [sent]: başarılı gönderimden sonra düşülecek tercih anahtarları.
 */
data class SobiRuleTallyPayload(
    val tally: Map<String, Map<String, Int>>,
    val questionCorrect: Int,
    val questionWrong: Int,
    val sent: Map<String, Int>,
) {
    /** Sınırdan fazlası birikmişse kalan da gönderilmeli (Android: sent.values.any { it == SEND_MAX }). */
    val hasMore: Boolean get() = sent.values.any { it == SobiRuleStats.SEND_MAX }
}

/** Android SobiRuleStats'ın SharedPreferences'a yazan kısmı; depo [OyunKeyValueStore]. */
class SobiRuleStatsStore(private val prefs: OyunKeyValueStore) {

    /** [tally]: kural → (doğru, yanlış). [question]: ders sorusuysa sonucu (soru sayısına), değilse null. */
    fun add(tally: Map<String, IntArray>, question: Boolean?) {
        if (tally.isEmpty() && question == null) return
        val keys = SobiRuleStats.KEYS
        for ((k, cw) in tally) {
            if (k !in keys) continue
            prefs.putInt("ok_$k", prefs.getInt("ok_$k", 0) + cw[0])
            prefs.putInt("bad_$k", prefs.getInt("bad_$k", 0) + cw[1])
            prefs.putInt("p_ok_$k", prefs.getInt("p_ok_$k", 0) + cw[0])
            prefs.putInt("p_bad_$k", prefs.getInt("p_bad_$k", 0) + cw[1])
        }
        if (question != null) {
            val qk = if (question) "p_q_ok" else "p_q_bad"
            prefs.putInt(qk, prefs.getInt(qk, 0) + 1)
        }
    }

    /** [withMental] false: yalnızca abaküslü kurallar (Sobi sohbetine giden bağlam). */
    fun snapshot(withMental: Boolean): List<SobiRuleStatEntry> =
        (if (withMental) SobiRuleStats.KEYS else SobiRuleStats.BASE_KEYS)
            .map { SobiRuleStatEntry(it, prefs.getInt("ok_$it", 0), prefs.getInt("bad_$it", 0)) }
            .filter { it.correct + it.wrong > 0 }

    /** Sobi ekranında bir adım: hamlenin kuralları doğru ya da yanlış (soru sayılmaz). */
    fun recordMove(m: StepExplainer.ColumnMove, correct: Boolean) {
        add(SobiRuleStats.tallyOf(listOf(m), correct), null)
    }

    /** İki sayılı ders sorusu ([numbers] = ilk, ±ikinci). Yanlışta hata [log] ile aranır. */
    fun recordAnswer(numbers: List<Int>, correct: Boolean, log: List<Int>, answer: Int) {
        if (correct) {
            add(SobiRuleStats.tallyOf(SobiRuleStats.movesOf(numbers), true), true)
            return
        }
        val m = if (numbers.size == 2 && log.isNotEmpty()) {
            MistakeAnalyzer.analyze(numbers[0], if (numbers[1] < 0) "-" else "+", abs(numbers[1]), log, answer)?.move
        } else null
        add(if (m != null) SobiRuleStats.tallyOf(listOf(m), false) else emptyMap(), false)
    }

    /** Flaş dizisi: yanlışta yalnızca yanlış yapılan işlemlerin kuralları (kayıt yoksa yalnızca soru). */
    fun recordSequence(numbers: List<Int>, correct: Boolean, log: List<Int>, marks: List<Int>, answer: Int) {
        if (correct) {
            add(SobiRuleStats.tallyOf(SobiRuleStats.movesOf(numbers), true), true)
            return
        }
        val mistakes = if (marks.isNotEmpty()) MistakeAnalyzer.findSequenceMistakes(numbers, log, marks, answer).orEmpty() else emptyList()
        val moves = mistakes.mapNotNull { it.diagnosis?.move }
        add(SobiRuleStats.tallyOf(moves, false), false)
    }

    /** Abaküssüz (körleme / zihinden) soru: "m_" önekli anahtarlara, soru başına. */
    fun recordMental(numbers: List<Int>, correct: Boolean) {
        val rules = SobiRuleStats.movesOf(numbers).flatMap { SobiRuleStats.moveRules(it) }.toSet()
        add(SobiRuleStats.mentalTally(rules, correct), correct)
    }

    /** Ders çarpma sorusu: basamak çifti başına, soru başına. */
    fun recordMultiplication(a: Int, b: Int, correct: Boolean, withAbacus: Boolean) {
        val key = SobiRuleStats.mulKeyOf(a.toLong(), b.toLong(), withAbacus)
        add(mapOf(key to if (correct) intArrayOf(1, 0) else intArrayOf(0, 1)), correct)
    }

    /** Android flush'ın yük kısmı: gönderilecek bir şey yoksa null. */
    fun pendingTally(): SobiRuleTallyPayload? {
        val sent = LinkedHashMap<String, Int>()
        val tally = LinkedHashMap<String, Map<String, Int>>()
        for (k in SobiRuleStats.KEYS) {
            val c = prefs.getInt("p_ok_$k", 0).coerceAtMost(SobiRuleStats.SEND_MAX)
            val w = prefs.getInt("p_bad_$k", 0).coerceAtMost(SobiRuleStats.SEND_MAX)
            if (c + w == 0) continue
            tally[k] = mapOf("c" to c, "w" to w)
            sent["p_ok_$k"] = c
            sent["p_bad_$k"] = w
        }
        val qc = prefs.getInt("p_q_ok", 0).coerceAtMost(SobiRuleStats.SEND_MAX)
        val qw = prefs.getInt("p_q_bad", 0).coerceAtMost(SobiRuleStats.SEND_MAX)
        sent["p_q_ok"] = qc
        sent["p_q_bad"] = qw
        if (tally.isEmpty() && qc + qw == 0) return null
        return SobiRuleTallyPayload(tally, qc, qw, sent)
    }

    /** Başarılı gönderimden sonra: gönderilen kadarı düşülür (arada eklenenler kalır). */
    fun markSent(payload: SobiRuleTallyPayload) {
        for ((k, v) in payload.sent) prefs.putInt(k, (prefs.getInt(k, 0) - v).coerceAtLeast(0))
    }
}
