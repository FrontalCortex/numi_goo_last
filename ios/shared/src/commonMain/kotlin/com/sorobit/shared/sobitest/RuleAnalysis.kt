// Kaynak: app/src/main/java/com/example/app/homework/RuleAnalysis.kt — tamamı, birebir. Sobi testinde
// abaküslü toplama/çıkarma sorusunun kural kural sayımı (HomeworkPlayerFragment.countRules) buradan.
package com.sorobit.shared.sobitest

import com.sorobit.shared.sobianaliz.AbacusMoves
import com.sorobit.shared.sobianaliz.AbacusRule
import com.sorobit.shared.sobianaliz.HomeworkOp

/**
 * Ödevde (ve Sobi testinde) kural kural başarı: öğrenci hangi kuralı doğru, hangisini yanlış uyguladı.
 *
 * Her sayının her basamağı ayrı bir İŞLEM; işlemler abaküs değer kaydına dinamik programlamayla
 * eşleniyor (bkz. Android dosyasının uzun açıklaması). Adım adım çözülmemiş sorular sayılmıyor.
 * Çarpmada soru, basamak çiftine tek deneme. Abaküssüz toplama/çıkarmada kayıt yok, sayılmıyor.
 */
object RuleAnalysis {

    /** Abaküsün t anındaki değeri (her değişimde bir kayıt). */
    data class Sample(val tMs: Long, val value: Long)

    /** Tek deneme: [key] istatistik anahtarı ("add_five", "sub_ten", "mulA_2x1"…). */
    data class Attempt(val key: String, val correct: Boolean)

    fun ruleKey(op: HomeworkOp, rule: AbacusRule) = "${op.key}_${rule.key}"

    fun mulKey(withAbacus: Boolean, digits1: Int, digits2: Int) = "${if (withAbacus) "mulA" else "mulM"}_${digits1}x$digits2"

    /** Çarpma sorusu: basamak çiftine tek deneme. */
    fun multiplication(cfg: HomeworkConfig, correct: Boolean): List<Attempt> =
        listOf(Attempt(mulKey(cfg.withAbacus, cfg.digits, cfg.digits2), correct))

    private data class Op(val number: Int, val col: Int, val x: Int, val delta: Long)

    /** Yanlış cevaplı soruda kabul edilen en çok yanlış işlem (işlemlerin üçte biri daha çoksa o). */
    private const val MAX_SLIPS = 2

    private fun steppedThrough(q: HomeworkQuestion, log: List<Sample>): Boolean {
        val seen = log.mapTo(HashSet()) { it.value }
        val sign = if (q.op == HomeworkOp.ADD) 1 else -1
        var sum = q.numbers[0]
        val partials = ArrayList<Long>()
        for (k in 1 until q.numbers.size) { partials += sum; sum += sign * q.numbers[k] }
        val hit = partials.count { it in seen }
        val correct = log.lastOrNull()?.value == q.answer
        return if (correct) hit * 2 >= partials.size else hit >= 1
    }

    /**
     * Abaküslü toplama/çıkarma sorusu.
     * @param log abaküs değerleri, zamana göre; ilk kayıt sorunun başındaki değer
     * @param shownAtMs i. sayının tahtada göründüğü an (numbers ile aynı sırada)
     * @param answer öğrencinin cevabı (Kontrol anındaki değer)
     */
    fun abacus(q: HomeworkQuestion, log: List<Sample>, shownAtMs: List<Long>, answer: Long, capacity: Int = 5): List<Attempt> {
        if (q.op == HomeworkOp.MUL || q.numbers.size < 2) return emptyList()
        val sign = if (q.op == HomeworkOp.ADD) 1 else -1
        val ops = ArrayList<Op>()
        for (k in 1 until q.numbers.size) {
            val ds = HomeworkEngine.digitsOf(q.numbers[k], capacity)
            for (col in ds.indices.reversed()) if (ds[col] != 0) {
                ops += Op(k, col, ds[col], sign * ds[col] * HomeworkEngine.pow10(col))
            }
        }
        if (ops.isEmpty()) return emptyList()
        if (!steppedThrough(q, log)) return emptyList()

        val bases: List<Long>
        val ok: BooleanArray
        if (answer == q.answer) {
            var v = q.numbers[0]
            bases = ops.map { op -> v.also { v += op.delta } }
            ok = BooleanArray(ops.size) { true }
        } else {
            val samples = if (log.isEmpty() || log.last().value != answer) log + Sample(Long.MAX_VALUE, answer) else log
            val (b, c) = align(ops, samples, shownAtMs, q.numbers[0])
            bases = b
            ok = c
            val wrong = ok.count { !it }
            if (log.none { it.value == q.numbers[0] } || wrong > maxOf(MAX_SLIPS, (ops.size + 2) / 3)) return emptyList()
        }
        val out = ArrayList<Attempt>()
        ops.forEachIndexed { i, op ->
            for (rule in rulesOf(q.op, bases[i], op.col, op.x, capacity)) out += Attempt(ruleKey(q.op, rule), ok[i])
        }
        return out
    }

    private fun align(ops: List<Op>, s: List<Sample>, shownAtMs: List<Long>, first: Long): Pair<List<Long>, BooleanArray> {
        val m = ops.size
        val n = s.size
        val neg = Int.MIN_VALUE / 4
        val best = Array(m + 1) { IntArray(n) }
        val from = Array(m + 1) { IntArray(n) }
        val credited = Array(m + 1) { BooleanArray(n) }
        for (j in 0 until n) best[0][j] = if (s[j].value == first) 1 else 0
        for (i in 1..m) {
            val op = ops[i - 1]
            val shown = shownAtMs.getOrElse(op.number) { 0L }
            var prefBest = neg
            var prefArg = 0
            val byValue = HashMap<Long, Pair<Int, Int>>()
            for (j in 0 until n) {
                val prev = best[i - 1][j]
                if (prev >= prefBest) { prefBest = prev; prefArg = j }
                val cur = byValue[s[j].value]
                if (cur == null || prev >= cur.first) byValue[s[j].value] = prev to j
                var score = prefBest
                var arg = prefArg
                var cr = false
                if (s[j].tMs >= shown) {
                    val hit = byValue[s[j].value - op.delta]
                    if (hit != null && hit.first + 2 >= score) { score = hit.first + 2; arg = hit.second; cr = true }
                }
                best[i][j] = score
                from[i][j] = arg
                credited[i][j] = cr
            }
        }
        val pos = IntArray(m + 1)
        pos[m] = n - 1
        val ok = BooleanArray(m)
        for (i in m downTo 1) {
            ok[i - 1] = credited[i][pos[i]]
            pos[i - 1] = from[i][pos[i]]
        }
        val bases = (0 until m).map { s[pos[it]].value }
        return bases to ok
    }

    /** [base] değerine [col] basamağında x eklenirken/çıkarılırken gereken kurallar (elde zinciri dahil). */
    internal fun rulesOf(op: HomeworkOp, base: Long, col: Int, x: Int, capacity: Int): Set<AbacusRule> {
        val state = HomeworkEngine.digitsOf(base.coerceIn(0L, HomeworkEngine.pow10(capacity) - 1), capacity)
        val out = LinkedHashSet<AbacusRule>()
        var c = col
        var amount = x
        var carry = false
        while (c < capacity) {
            val d = state[c]
            val rule = if (op == HomeworkOp.ADD) AbacusMoves.classifyAdd(d, amount) else AbacusMoves.classifySub(d, amount)
            if (!(carry && rule == AbacusRule.DIRECT)) out += rule
            val overflow = if (op == HomeworkOp.ADD) d + amount >= 10 else d < amount
            if (!overflow) break
            state[c] = if (op == HomeworkOp.ADD) d + amount - 10 else d + 10 - amount
            c++
            amount = 1
            carry = true
        }
        if (out.isEmpty()) out += AbacusRule.DIRECT
        return out
    }

    /** Denemeleri anahtar başına [doğru, yanlış] sayısına çevirir. */
    fun tally(attempts: List<Attempt>): Map<String, IntArray> {
        val out = LinkedHashMap<String, IntArray>()
        for (a in attempts) out.getOrPut(a.key) { IntArray(2) }[if (a.correct) 0 else 1]++
        return out
    }
}
