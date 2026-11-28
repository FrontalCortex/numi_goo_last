// Kaynak: app/src/main/java/com/example/app/homework/HomeworkEngine.kt — ödev soru motoru (HomeworkConfig,
// MoveRules, HomeworkQuestion, HomeworkEngine). Sobi'nin testi soruları bu motorla üretiyor
// (SobiTestPlanner.question). HomeworkOp, AbacusRule ve AbacusMoves ortak kodda zaten vardı
// (sobianaliz/SobiKurallar.kt); buradan onlar kullanılıyor.
// Davranış birebir aynıdır. Farklar yalnızca platformdan: `synchronized` kaldırıldı (ortak kod tek iş
// parçacığından çağrılıyor), `toSortedSet()` yerine sıralı `toSet()` (küme eşitliği aynı).
package com.sorobit.shared.sobitest

import com.sorobit.shared.sobianaliz.AbacusMoves
import com.sorobit.shared.sobianaliz.AbacusRule
import com.sorobit.shared.sobianaliz.HomeworkOp
import kotlin.math.min
import kotlin.random.Random

/**
 * Öğretmenin seçtiği ödev içeriği (Sobi testinde planlayıcı kuruyor).
 *
 * @param digits      Toplama/çıkarmada sayıların basamağı; çarpmada 1. sayının basamağı.
 * @param digits2     Yalnızca çarpma: 2. sayının basamağı.
 * @param numberCount Toplamada ekranda gösterilen sayı adedi; çıkarmada ÇIKARILAN sayı adedi
 *                    (başlangıç sayısı hariç). Çarpmada kullanılmaz.
 * @param intervalMs  Sayıların gösterilme aralığı. Çarpmada kullanılmaz.
 * @param ruleDigits  Gelişmiş ayar: kural → o kuralla sütuna eklenebilecek rakamlar.
 * @param mulDigits1  Gelişmiş ayar, yalnızca çarpma: 1. sayının basamağı (0 = birler) → rakamlar.
 * @param mulDigits2  Aynısı 2. sayı için.
 */
data class HomeworkConfig(
    val op: HomeworkOp,
    val rules: Set<AbacusRule> = emptySet(),
    val withAbacus: Boolean,
    val digits: Int,
    val digits2: Int = 0,
    val numberCount: Int = 0,
    val intervalMs: Long = 0L,
    val questionCount: Int,
    val ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    val mulDigits1: Map<Int, Set<Int>> = emptyMap(),
    val mulDigits2: Map<Int, Set<Int>> = emptyMap(),
) {
    internal val moveRules: MoveRules get() = MoveRules(rules, ruleDigits)
}

/**
 * Bir sütun hareketine izin var mı: hareketin kuralı seçilmiş olmalı ve (gelişmiş ayarda o kural
 * için rakam seçildiyse) eklenen/çıkarılan rakam o kuralda açık olmalı.
 */
internal data class MoveRules(val rules: Set<AbacusRule>, val digits: Map<AbacusRule, Set<Int>> = emptyMap()) {
    fun allows(rule: AbacusRule, x: Int): Boolean = rule in rules && digits[rule]?.contains(x) != false
}

/**
 * Tek bir soru. Toplamada [numbers] sırayla toplanan sayılar; çıkarmada ilki başlangıç, gerisi
 * çıkarılanlar; çarpmada iki çarpan.
 */
data class HomeworkQuestion(val op: HomeworkOp, val numbers: List<Long>) {
    val answer: Long
        get() = when (op) {
            HomeworkOp.ADD -> numbers.sum()
            HomeworkOp.SUB -> numbers.first() - numbers.drop(1).sum()
            HomeworkOp.MUL -> numbers[0] * numbers[1]
        }
}

object HomeworkEngine {

    const val BLOCK_SIZE = 10
    const val MIN_QUESTIONS = 1
    const val MAX_QUESTIONS = 200
    const val MIN_INTERVAL_MS = 100L
    const val MAX_INTERVAL_MS = 30_000L
    const val MAX_NUMBER_COUNT = 100

    /** Rastgele yolda bir soru için baştan kurma denemesi ve sayı başına aday sayısı. */
    private const val ATTEMPTS = 200
    private const val CANDIDATE_TRIES = 40

    /** Abaküs 5 sütun (99.999); abaküssüzde sayılar ve sonuç 8 basamağa kadar. */
    fun capacityDigits(withAbacus: Boolean): Int = if (withAbacus) 5 else 8

    fun maxDigits(withAbacus: Boolean): Int = capacityDigits(withAbacus)

    fun minNumberCount(op: HomeworkOp): Int = if (op == HomeworkOp.SUB) 1 else 2

    fun usesCarry(rules: Set<AbacusRule>): Boolean = AbacusRule.TEN in rules || AbacusRule.BEAD in rules

    fun blockCount(questionCount: Int): Int = (questionCount + BLOCK_SIZE - 1) / BLOCK_SIZE

    /** [blockIndex]. adımdaki soru sayısı; son adım 10'dan az olabilir. */
    fun blockSize(questionCount: Int, blockIndex: Int): Int =
        (questionCount - blockIndex * BLOCK_SIZE).coerceIn(0, BLOCK_SIZE)

    // ── Gelişmiş ayar: rakam seçenekleri ────────────────────────────────────

    /** Bu kuralla bir sütuna eklenebilen (çıkarmada çıkarılabilen) rakamlar. */
    fun ruleDigitOptions(op: HomeworkOp, rule: AbacusRule): List<Int> =
        (1..9).filter { x -> (0..9).any { d -> AbacusMoves.classify(op, d, x) == rule } }

    /** Çarpmada [digits] basamaklı sayının [position]. basamağının (0 = birler) alabileceği rakamlar. */
    fun mulDigitOptions(digits: Int, position: Int): List<Int> = when {
        position != digits - 1 -> (0..9).toList()
        digits == 1 -> (2..9).toList()
        else -> (1..9).toList()
    }

    /** Gelişmiş rakam seçimini temizler (bkz. Android). */
    fun normalizeRuleDigits(op: HomeworkOp, rules: Set<AbacusRule>, raw: Map<AbacusRule, Set<Int>>): Map<AbacusRule, Set<Int>> {
        if (op == HomeworkOp.MUL) return emptyMap()
        val out = LinkedHashMap<AbacusRule, Set<Int>>()
        for (rule in AbacusRule.entries) {
            if (rule !in rules) continue
            val options = ruleDigitOptions(op, rule)
            val picked = raw[rule]?.filter { it in options }?.sorted()?.toSet() ?: continue
            if (picked.isNotEmpty() && picked.size < options.size) out[rule] = picked
        }
        return out
    }

    /** Çarpmada aynı temizlik. */
    fun normalizeMulDigits(digits: Int, raw: Map<Int, Set<Int>>): Map<Int, Set<Int>> {
        val out = LinkedHashMap<Int, Set<Int>>()
        for (position in 0 until digits) {
            val options = mulDigitOptions(digits, position)
            val picked = raw[position]?.filter { it in options }?.sorted()?.toSet() ?: continue
            if (picked.isNotEmpty() && picked.size < options.size) out[position] = picked
        }
        return out
    }

    fun normalized(cfg: HomeworkConfig): HomeworkConfig = cfg.copy(
        ruleDigits = normalizeRuleDigits(cfg.op, cfg.rules, cfg.ruleDigits),
        mulDigits1 = if (cfg.op == HomeworkOp.MUL) normalizeMulDigits(cfg.digits, cfg.mulDigits1) else emptyMap(),
        mulDigits2 = if (cfg.op == HomeworkOp.MUL) normalizeMulDigits(cfg.digits2, cfg.mulDigits2) else emptyMap(),
    )

    // ── Sınırlar ────────────────────────────────────────────────────────────

    /** Bu kurallar ve basamakla seçilebilecek en fazla sayı adedi. 0: hiç soru üretilemiyor. */
    fun maxNumberCount(
        op: HomeworkOp,
        rules: Set<AbacusRule>,
        digits: Int,
        withAbacus: Boolean,
        ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    ): Int {
        if (op == HomeworkOp.MUL || rules.isEmpty()) return 0
        if (digits !in 1..maxDigits(withAbacus)) return 0
        return if (!usesCarry(rules)) {
            ColumnPlan.of(op, MoveRules(rules, ruleDigits)).maxCount()
        } else {
            HomeworkLimitTable.maxCount(op, AbacusRule.maskOf(rules), digits, withAbacus)
        }
    }

    fun needsMeasuredMax(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>): Boolean =
        op != HomeworkOp.MUL && usesCarry(rules) && ruleDigits.isNotEmpty()

    private val measuredCache = HashMap<String, Int>()

    private fun measureKey(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>, digits: Int, withAbacus: Boolean): String {
        val filter = AbacusRule.entries.filter { it in ruleDigits }.joinToString(";") { "${it.key}=${ruleDigits.getValue(it).sorted()}" }
        return "${op.key}|${AbacusRule.maskOf(rules)}|$filter|$digits|$withAbacus"
    }

    fun cachedCarryMax(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>, digits: Int, withAbacus: Boolean): Int? =
        measuredCache[measureKey(op, rules, ruleDigits, digits, withAbacus)]

    /** Gelişmiş rakam seçimiyle elde/ödünçlü kurallarda en fazla sayı adedi (ağır; bkz. Android). */
    fun measureCarryMax(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>, digits: Int, withAbacus: Boolean): Int {
        cachedCarryMax(op, rules, ruleDigits, digits, withAbacus)?.let { return it }
        val key = measureKey(op, rules, ruleDigits, digits, withAbacus)
        val upper = maxNumberCount(op, rules, digits, withAbacus)
        val min = minNumberCount(op)
        val rng = Random(key.hashCode())
        var n = if (upper < min) 0 else probeCarryMax(op, rules, digits, withAbacus, rng, questions = 20, upper = upper, ruleDigits = ruleDigits)
        while (n >= min) {
            val cfg = HomeworkConfig(op, rules, withAbacus, digits, numberCount = n, intervalMs = 1000L, questionCount = 1, ruleDigits = ruleDigits)
            if ((1..RELIABLE_QUESTIONS).all { probeGenerate(cfg, rng) }) break
            n--
        }
        val result = if (n < min) 0 else n
        measuredCache[key] = result
        return result
    }

    private const val RELIABLE_QUESTIONS = 300

    fun recommendedNumberCount(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap()): Int? {
        if (op == HomeworkOp.MUL || rules.isEmpty() || usesCarry(rules)) return null
        val max = ColumnPlan.of(op, MoveRules(rules, ruleDigits)).maxCount()
        if (max == 0) return null
        return ((max * 4 + 8) / 9).coerceIn(minNumberCount(op), max)
    }

    fun forcedPattern(
        op: HomeworkOp,
        rules: Set<AbacusRule>,
        numberCount: Int,
        ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    ): List<Int>? {
        if (op == HomeworkOp.MUL || rules.isEmpty() || usesCarry(rules)) return null
        val plan = ColumnPlan.of(op, MoveRules(rules, ruleDigits))
        var d = plan.stepOptionsAtStart(numberCount).singleOrNull() ?: return null
        val pattern = mutableListOf(d)
        val steps = if (op == HomeworkOp.ADD) numberCount - 1 else numberCount
        for (k in 0 until steps) {
            val x = plan.stepOptions(d, steps - 1 - k).singleOrNull() ?: return null
            pattern += x
            d = plan.next(d, x)
        }
        return pattern
    }

    /** Ayarlar geçerli mi; değilse sebebi. */
    fun validate(cfg: HomeworkConfig): String? {
        if (cfg.questionCount !in MIN_QUESTIONS..MAX_QUESTIONS) return "questionCount"
        val cap = capacityDigits(cfg.withAbacus)
        return when (cfg.op) {
            HomeworkOp.MUL -> when {
                cfg.digits < 1 || cfg.digits2 < 1 -> "digits"
                cfg.digits + cfg.digits2 > cap -> "digits"
                normalizeMulDigits(cfg.digits, cfg.mulDigits1) != cfg.mulDigits1 -> "mulDigits"
                normalizeMulDigits(cfg.digits2, cfg.mulDigits2) != cfg.mulDigits2 -> "mulDigits"
                else -> null
            }
            else -> when {
                cfg.rules.isEmpty() -> "rules"
                cfg.digits !in 1..maxDigits(cfg.withAbacus) -> "digits"
                cfg.intervalMs !in MIN_INTERVAL_MS..MAX_INTERVAL_MS -> "interval"
                normalizeRuleDigits(cfg.op, cfg.rules, cfg.ruleDigits) != cfg.ruleDigits -> "ruleDigits"
                cfg.numberCount < minNumberCount(cfg.op) -> "numberCount"
                cfg.numberCount > maxNumberCount(cfg.op, cfg.rules, cfg.digits, cfg.withAbacus, cfg.ruleDigits) -> "numberCount"
                else -> null
            }
        }
    }

    // ── Üretim ──────────────────────────────────────────────────────────────

    fun generateBlock(cfg: HomeworkConfig, blockIndex: Int, rng: Random = Random.Default): List<HomeworkQuestion> {
        val size = blockSize(cfg.questionCount, blockIndex)
        val seen = HashSet<List<Long>>()
        return List(size) {
            var q = generateQuestion(cfg, rng)
            var tries = 0
            while (q.numbers in seen && tries < 5) {
                q = generateQuestion(cfg, rng)
                tries++
            }
            seen += q.numbers
            q
        }
    }

    /** @throws IllegalStateException ayarlar geçersizse ya da soru üretilemediyse (Swift'ten doğrudan çağırmayın). */
    fun generateQuestion(cfg: HomeworkConfig, rng: Random = Random.Default): HomeworkQuestion {
        validate(cfg)?.let { throw IllegalStateException("Geçersiz ödev ayarı: $it") }
        return when (cfg.op) {
            HomeworkOp.MUL -> generateMultiplication(cfg, rng)
            HomeworkOp.ADD, HomeworkOp.SUB ->
                if (!usesCarry(cfg.rules)) generateWithoutCarry(cfg, rng)
                else generateWithCarry(cfg, rng) ?: throw IllegalStateException("Soru üretilemedi: $cfg")
        }
    }

    private fun generateMultiplication(cfg: HomeworkConfig, rng: Random): HomeworkQuestion {
        fun factor(digits: Int, picked: Map<Int, Set<Int>>): Long {
            var n = 0L
            for (position in digits - 1 downTo 0) {
                val options = picked[position]?.toList() ?: mulDigitOptions(digits, position)
                n = n * 10 + options.random(rng)
            }
            return n
        }
        return HomeworkQuestion(HomeworkOp.MUL, listOf(factor(cfg.digits, cfg.mulDigits1), factor(cfg.digits2, cfg.mulDigits2)))
    }

    // ── Elde/ödünç olmayan yol: sütun planı ─────────────────────────────────

    /** Tek sütunda "r hareket daha yapılabilir mi" tablosu (bkz. Android açıklaması). */
    internal class ColumnPlan private constructor(
        private val op: HomeworkOp,
        private val rules: MoveRules,
    ) {
        private val feasible = Array(MAX_NUMBER_COUNT + 1) { BooleanArray(10) }

        init {
            for (d in 0..9) feasible[0][d] = true
            for (r in 1..MAX_NUMBER_COUNT) for (d in 0..9) {
                feasible[r][d] = moves(d).any { x -> feasible[r - 1][next(d, x)] }
            }
        }

        fun moves(d: Int): List<Int> = (1..9).filter { x -> rules.allows(AbacusMoves.classify(op, d, x), x) }

        fun next(d: Int, x: Int): Int = if (op == HomeworkOp.ADD) (d + x) % 10 else (d - x + 10) % 10

        private fun movesAfterStart(numberCount: Int): Int =
            if (op == HomeworkOp.ADD) numberCount - 1 else numberCount

        fun stepOptionsAtStart(numberCount: Int): List<Int> {
            val r = movesAfterStart(numberCount)
            if (r !in 0..MAX_NUMBER_COUNT) return emptyList()
            return (1..9).filter { feasible[r][it] }
        }

        fun maxCount(): Int {
            val min = minNumberCount(op)
            return (MAX_NUMBER_COUNT downTo min).firstOrNull { stepOptionsAtStart(it).isNotEmpty() } ?: 0
        }

        fun stepOptions(d: Int, remainingAfter: Int): List<Int> =
            moves(d).filter { x -> feasible[remainingAfter][next(d, x)] }

        companion object {
            private val cache = HashMap<Pair<HomeworkOp, MoveRules>, ColumnPlan>()

            fun of(op: HomeworkOp, rules: MoveRules): ColumnPlan =
                cache.getOrPut(op to rules) { ColumnPlan(op, rules) }
        }
    }

    private fun generateWithoutCarry(cfg: HomeworkConfig, rng: Random): HomeworkQuestion {
        val plan = ColumnPlan.of(cfg.op, cfg.moveRules)
        val columns = cfg.digits
        val startOptions = plan.stepOptionsAtStart(cfg.numberCount)
        val state = IntArray(columns) { pickWeighted(startOptions, cfg.op, -1, cfg.rules, rng) }
        val numbers = mutableListOf(toNumber(state))
        val steps = if (cfg.op == HomeworkOp.ADD) cfg.numberCount - 1 else cfg.numberCount
        for (k in 0 until steps) {
            val remainingAfter = steps - 1 - k
            val digits = IntArray(columns) { col ->
                pickWeighted(plan.stepOptions(state[col], remainingAfter), cfg.op, state[col], cfg.rules, rng)
            }
            for (col in 0 until columns) state[col] = plan.next(state[col], digits[col])
            numbers += toNumber(digits)
        }
        return HomeworkQuestion(cfg.op, numbers)
    }

    // ── Elde/ödünç olan yol: rastgele aday + abaküs simülasyonu ─────────────

    private fun generateWithCarry(cfg: HomeworkConfig, rng: Random): HomeworkQuestion? {
        val cap = capacityDigits(cfg.withAbacus)
        val capMax = pow10(cap) - 1
        val minNumber = pow10(cfg.digits - 1)
        val plan = ColumnPlan.of(cfg.op, cfg.moveRules)
        repeat(ATTEMPTS) {
            val state = IntArray(cap)
            val start = if (cfg.op == HomeworkOp.ADD) randomAliveStart(cfg, plan, cfg.digits, rng)
            else randomAliveStart(cfg, plan, subtractionStartDigits(cfg), rng)
            setState(state, start)
            val numbers = mutableListOf(start)
            var value = start
            val steps = if (cfg.op == HomeworkOp.ADD) cfg.numberCount - 1 else cfg.numberCount
            var ok = true
            for (k in 0 until steps) {
                val remainingAfter = (steps - 1 - k).toLong()
                val cand = pickCandidate(cfg, plan, state, remainingAfter.toInt(), rng) { n ->
                    if (cfg.op == HomeworkOp.ADD) value + n + remainingAfter * minNumber <= capMax
                    else value - n >= remainingAfter * minNumber
                }
                if (cand == null) { ok = false; break }
                numbers += cand
                value = if (cfg.op == HomeworkOp.ADD) value + cand else value - cand
            }
            if (ok) return HomeworkQuestion(cfg.op, numbers)
        }
        return null
    }

    internal fun probeCarryMax(
        op: HomeworkOp,
        rules: Set<AbacusRule>,
        digits: Int,
        withAbacus: Boolean,
        rng: Random,
        questions: Int,
        upper: Int = MAX_NUMBER_COUNT,
        ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    ): Int {
        fun ok(n: Int): Boolean {
            val cfg = HomeworkConfig(
                op, rules, withAbacus, digits, numberCount = n, intervalMs = 1000L, questionCount = 1, ruleDigits = ruleDigits,
            )
            repeat(questions) { if (generateWithCarry(cfg, rng) == null) return false }
            return true
        }
        val min = minNumberCount(op)
        if (!ok(min)) return 0
        if (ok(upper)) return upper
        var lo = min
        var hi = upper
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (ok(mid)) lo = mid else hi = mid
        }
        return lo
    }

    internal fun probeGenerate(cfg: HomeworkConfig, rng: Random): Boolean = generateWithCarry(cfg, rng) != null

    internal fun subtractionStartDigits(cfg: HomeworkConfig): Int =
        min(capacityDigits(cfg.withAbacus), cfg.digits + cfg.numberCount.toString().length)

    private fun randomAliveStart(cfg: HomeworkConfig, plan: ColumnPlan, startDigits: Int, rng: Random): Long {
        val planned = plan.stepOptionsAtStart(cfg.numberCount)
        val digits = IntArray(startDigits) { col ->
            val leading = col == startDigits - 1
            val pool = (if (leading) 1..9 else 0..9).toList()
            val alive = when {
                col >= cfg.digits -> pool.filter { it != 0 }
                col == cfg.digits - 1 && planned.isNotEmpty() -> pool.filter { it in planned }
                else -> pool.filter { d -> plan.moves(d).isNotEmpty() }
            }
            (alive.ifEmpty { pool.filter { it != 0 } }).random(rng)
        }
        return toNumber(digits)
    }

    private fun pickCandidate(
        cfg: HomeworkConfig,
        plan: ColumnPlan,
        state: IntArray,
        remainingAfter: Int,
        rng: Random,
        fits: (Long) -> Boolean,
    ): Long? {
        repeat(CANDIDATE_TRIES) { attempt ->
            val looseZeros = attempt >= CANDIDATE_TRIES / 2
            val digits = IntArray(cfg.digits)
            for (col in cfg.digits - 1 downTo 0) {
                val d = state[col]
                val all = plan.moves(d)
                val options = plan.stepOptions(d, remainingAfter.coerceIn(0, MAX_NUMBER_COUNT)).ifEmpty { all }
                val leading = col == cfg.digits - 1
                if (leading && options.isEmpty()) return null
                digits[col] = when {
                    options.isEmpty() -> 0
                    !leading && looseZeros && rng.nextInt(4) == 0 -> 0
                    else -> pickWeighted(options, cfg.op, d, cfg.rules, rng)
                }
            }
            val n = toNumber(digits)
            if (!fits(n)) return@repeat
            val trial = state.copyOf()
            val ok = if (cfg.op == HomeworkOp.ADD) applyAdd(trial, digits, cfg.moveRules) else applySub(trial, digits, cfg.moveRules)
            if (ok) {
                trial.copyInto(state)
                return n
            }
        }
        return null
    }

    private fun pickWeighted(options: List<Int>, op: HomeworkOp, d: Int, rules: Set<AbacusRule>, rng: Random): Int {
        check(options.isNotEmpty())
        if (d < 0 || rules.size == 1 || AbacusRule.DIRECT !in rules) return options.random(rng)
        val weights = options.map { x ->
            val m = if (op == HomeworkOp.ADD) AbacusMoves.classifyAdd(d, x) else AbacusMoves.classifySub(d, x)
            if (m == AbacusRule.DIRECT) 1 else 2
        }
        var roll = rng.nextInt(weights.sum())
        for (i in options.indices) {
            roll -= weights[i]
            if (roll < 0) return options[i]
        }
        return options.last()
    }

    // ── Abaküs simülasyonu ──────────────────────────────────────────────────

    internal fun applyAdd(state: IntArray, digits: IntArray, rules: MoveRules): Boolean {
        for (col in digits.indices.reversed()) {
            val x = digits[col]
            if (x != 0 && !addAt(state, col, x, rules, isCarry = false)) return false
        }
        return true
    }

    internal fun applySub(state: IntArray, digits: IntArray, rules: MoveRules): Boolean {
        for (col in digits.indices.reversed()) {
            val x = digits[col]
            if (x != 0 && !subAt(state, col, x, rules, isBorrow = false)) return false
        }
        return true
    }

    private fun addAt(state: IntArray, col: Int, x: Int, rules: MoveRules, isCarry: Boolean): Boolean {
        if (col >= state.size) return false
        val d = state[col]
        val move = AbacusMoves.classifyAdd(d, x)
        if (!rules.allows(move, x) && !(isCarry && move == AbacusRule.DIRECT)) return false
        return if (d + x >= 10) {
            state[col] = d + x - 10
            addAt(state, col + 1, 1, rules, isCarry = true)
        } else {
            state[col] = d + x
            true
        }
    }

    private fun subAt(state: IntArray, col: Int, x: Int, rules: MoveRules, isBorrow: Boolean): Boolean {
        if (col >= state.size) return false
        val d = state[col]
        val move = AbacusMoves.classifySub(d, x)
        if (!rules.allows(move, x) && !(isBorrow && move == AbacusRule.DIRECT)) return false
        return if (d < x) {
            state[col] = d + 10 - x
            subAt(state, col + 1, 1, rules, isBorrow = true)
        } else {
            state[col] = d - x
            true
        }
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    internal fun pow10(exp: Int): Long {
        var out = 1L
        repeat(exp) { out *= 10 }
        return out
    }

    private fun toNumber(digits: IntArray): Long = digits.foldRight(0L) { d, acc -> acc * 10 + d }

    internal fun digitsOf(n: Long, size: Int): IntArray {
        var x = n
        return IntArray(size) { (x % 10).toInt().also { x /= 10 } }
    }

    private fun setState(state: IntArray, n: Long) {
        val digits = digitsOf(n, state.size)
        digits.copyInto(state)
    }
}
