package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkConfig
import com.example.app.homework.HomeworkEngine
import com.example.app.homework.HomeworkOp
import com.example.app.homework.HomeworkQuestion
import kotlin.random.Random

/**
 * SOBİ'NİN TESTİ — hangi kurallar, hangi sorular (kullanıcı kararı 01.11.2026: Sobi çocuğun eksiğine
 * göre test verir; günde 1 test + istenirse ek test, 10 soru; zorluk çocuğun seviyesine göre).
 *
 * Kural motoru seçiyor, yapay zekâ yok: kesin ve ücretsiz.
 *  - Zayıf kurallar: kural başarısında (dersler + ödevler + Sobi; son ~50 işlem) en az [MIN_OPS] işlemi
 *    olan kurallardan oranı en düşük olanlar; %[WEAK_PCT] altındaki en çok 2 kural, hiçbiri o kadar düşük
 *    değilse en düşüğü (çocuk yine pekiştirsin).
 *  - Sorular: [WEAK_SHARE] zayıf kurallardan, kalanı bildiği öbür kurallardan (pekiştirme, 3./6./9. sıra).
 *  - Zorluk: her soru kendi kuralının seviyesinde ([SobiTestLevels]: basamak, sayı adedi, ara). Seviye
 *    kayıtlı değilse kural başarısından tahmin. [offset] çocuğun seçimi (Kolay −1, Benim seviyem 0, Zor +1);
 *    pekiştirme soruları bir seviye kolay.
 *  - Sorular ödev motoruyla ([HomeworkEngine]) üretiliyor: yalnızca izin verilen kurallarla hamle seçiyor,
 *    çok sayılı soruyu kurabiliyor. Üretilen soru çalışılan kuralı gerçekten içermiyorsa atılıyor. O
 *    seviyede kurulamıyorsa (ör. yalnızca 5'lik kuralla çok sayı) bir alt seviye deneniyor.
 *  - Yalnızca çocuğun verisi olan kurallar: hiç görmediği bir kural çıkmaz.
 * Yeterli veri yoksa null.
 */
object SobiTestPlanner {

    const val QUESTION_COUNT = 10
    const val MIN_OPS = 10
    private const val WEAK_PCT = 90
    private const val WEAK_SHARE = 7
    private const val TRIES_PER_LEVEL = 40

    /** [key] "add_ten" gibi; [correct]/[wrong] son penceredeki işlem sayıları. */
    data class RuleScore(val key: String, val correct: Int, val wrong: Int) {
        val total: Int get() = correct + wrong
        val percent: Int get() = if (total == 0) 0 else correct * 100 / total
    }

    /** Testin bir sorusu: çalıştırdığı kural ve oynandığı seviye (sayıların arası buradan). */
    data class Question(val question: HomeworkQuestion, val rule: String, val level: Int) {
        val intervalMs: Long get() = SobiTestLevels.spec(level).intervalMs
    }

    /**
     * [weak]: testin çalıştırdığı kurallar. [current]: kuralların teste girerken kayıtlı (ya da tahmin)
     * seviyesi; [played]: zayıf kuralların bu testte oynanan seviyesi (seçilen zorlukla).
     */
    data class Plan(
        val weak: List<RuleScore>,
        val current: Map<String, Int>,
        val played: Map<String, Int>,
        val questions: List<Question>,
    )

    private val KEY = Regex("""^(add|sub)_(direct|five|ten|bead)$""")

    /** Kuralın kayıtlı seviyesi, yoksa kural başarısından tahmin. */
    fun levelOf(score: RuleScore, levels: Map<String, Int>): Int =
        levels[score.key]?.coerceIn(1, SobiTestLevels.MAX) ?: SobiTestLevels.initial(score.percent)

    /** Testin kuralları ve seviyeleri (sorular üretilmeden; zorluk seçim penceresi için). */
    data class Choice(
        val weak: List<RuleScore>,
        val strong: List<RuleScore>,
        val allowed: Set<String>,
        val current: Map<String, Int>,
    )

    fun choose(scores: List<RuleScore>, levels: Map<String, Int>): Choice? {
        val known = scores.filter { KEY.matches(it.key) && it.total > 0 }
        val rated = known.filter { it.total >= MIN_OPS }.sortedWith(compareBy({ it.percent }, { -it.total }))
        if (rated.isEmpty()) return null
        val weak = rated.filter { it.percent < WEAK_PCT }.take(2).ifEmpty { listOf(rated.first()) }
        val allowed = known.map { it.key }.toMutableSet().apply {
            if (any { it.startsWith("add_") }) add("add_direct")
            if (any { it.startsWith("sub_") }) add("sub_direct")
        }
        val strong = known.filter { s -> weak.none { it.key == s.key } }
        return Choice(weak, strong, allowed, (weak + strong).associate { it.key to levelOf(it, levels) })
    }

    fun plan(scores: List<RuleScore>, levels: Map<String, Int>, offset: Int, seed: Int): Plan? {
        val (weak, strong, allowed, current) = choose(scores, levels) ?: return null
        val played = weak.associate { it.key to (current.getValue(it.key) + offset).coerceIn(1, SobiTestLevels.MAX) }

        val rnd = Random(seed)
        val strongSlots = if (strong.isEmpty()) emptySet() else setOf(2, 5, 8)
        val out = ArrayList<Question>()
        val seen = HashSet<List<Long>>()
        var weakTurn = 0
        var strongTurn = 0
        for (i in 0 until QUESTION_COUNT) {
            val (key, level) = if (i in strongSlots) {
                val s = strong[strongTurn++ % strong.size]
                s.key to (current.getValue(s.key) - 1 + offset).coerceIn(1, SobiTestLevels.MAX)
            } else {
                val w = weak[weakTurn++ % weak.size]
                w.key to played.getValue(w.key)
            }
            var q: Question? = null
            for (attempt in 0 until 4) {
                val c = question(key, level, allowed, rnd) ?: break
                if (c.question.numbers !in seen) { q = c; break }
            }
            if (q != null) {
                seen += q.question.numbers
                out += q
            }
        }
        if (out.size < QUESTION_COUNT / 2) return null
        return Plan(weak, current, played, out)
    }

    /** [key] kuralını içeren, [level] seviyesinde (kurulamazsa daha aşağıda) bir soru. */
    fun question(key: String, level: Int, allowed: Set<String>, rnd: Random): Question? {
        val add = key.startsWith("add_")
        val op = if (add) HomeworkOp.ADD else HomeworkOp.SUB
        val prefix = if (add) "add_" else "sub_"
        val rules = allowed.filter { it.startsWith(prefix) }.mapNotNull { AbacusRule.fromKey(it.substringAfter('_')) }.toSet()
        var lv = level.coerceIn(1, SobiTestLevels.MAX)
        while (lv >= 1) {
            val s = SobiTestLevels.spec(lv)
            val want = if (add) s.count else s.count - 1
            val n = minOf(want, HomeworkEngine.maxNumberCount(op, rules, s.digits, withAbacus = true))
            if (n >= HomeworkEngine.minNumberCount(op)) {
                val cfg = HomeworkConfig(op, rules, withAbacus = true, digits = s.digits, numberCount = n, intervalMs = s.intervalMs, questionCount = 1)
                repeat(TRIES_PER_LEVEL) {
                    val q = runCatching { HomeworkEngine.generateQuestion(cfg, rnd) }.getOrNull() ?: return@repeat
                    if (key in rulesOf(q)) return Question(q, key, lv)
                }
            }
            lv--
        }
        // Tek basamaklı çıkarmada 10'luk ve boncuk kuralı kurulamıyor (başlangıç tek basamak): örnek
        // seçicinin 18'e kadar başlangıçlı soruları.
        val (base, n) = ExamplePicker.pick(key, rnd.nextInt(), 1, allowed) ?: return null
        return Question(HomeworkQuestion(op, listOf(base.toLong(), Math.abs(n).toLong())), key, 1)
    }

    /** Sorunun kullandığı kurallar ("add_ten"…): ilk sayı yazılmış, sonrakiler eklenir ya da çıkarılır. */
    fun rulesOf(q: HomeworkQuestion): Set<String> {
        if (q.op == HomeworkOp.MUL || q.numbers.size < 2) return emptySet()
        val prefix = if (q.op == HomeworkOp.ADD) "add_" else "sub_"
        val out = HashSet<String>()
        var running = q.numbers[0]
        for (n in q.numbers.drop(1)) {
            val e = StepExplainer.explain(running, q.op, n) ?: return emptySet()
            e.moves.forEach { m -> StepExplainer.rulesUsed(m).forEach { out += prefix + it.key } }
            running = e.result
        }
        return out
    }
}
