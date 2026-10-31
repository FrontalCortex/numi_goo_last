package com.example.app.abacus

/**
 * SOBİ'NİN GÜNLÜK TESTİ — hangi kurallar, hangi sorular (kullanıcı kararı 01.11.2026: Sobi çocuğun
 * eksiğine göre test verir; günde 1 test, 10 soru).
 *
 * Kural motoru seçiyor, yapay zekâ yok: kesin ve ücretsiz.
 *  - Zayıf kurallar: kural başarısında (dersler + ödevler + Sobi; son ~50 işlem) en az [MIN_OPS] işlemi
 *    olan kurallardan oranı en düşük olanlar; %[WEAK_PCT] altındaki en çok 2 kural, hiçbiri o kadar düşük
 *    değilse en düşüğü (çocuk yine pekiştirsin).
 *  - Sorular: [WEAK_SHARE] zayıf kurallardan, kalanı çocuğun bildiği öbür kurallardan (pekiştirme).
 *    Zayıf kuralda oran çok düşükse ([EASY_PCT]) sorular daha kolay (iki basamaklı + tek basamaklı),
 *    değilse iki basamaklı iki sayı ([ExamplePicker] düzeyleri).
 *  - Yalnızca çocuğun verisi olan kurallar: hiç görmediği bir kural çıkmaz. Verisi olan bir kuralın
 *    kuralsız hâli de serbest (onlar sütununda kuralsız hamle gibi).
 * Yeterli veri yoksa null: harita kartı "Biraz ders çöz, sana göre test hazırlayayım" der.
 */
object SobiTestPlanner {

    const val QUESTION_COUNT = 10
    const val MIN_OPS = 10
    private const val WEAK_PCT = 90
    private const val EASY_PCT = 60
    private const val WEAK_SHARE = 7

    /** [key] "add_ten" gibi; [correct]/[wrong] son penceredeki işlem sayıları. */
    data class RuleScore(val key: String, val correct: Int, val wrong: Int) {
        val total: Int get() = correct + wrong
        val percent: Int get() = if (total == 0) 0 else correct * 100 / total
    }

    /** [questions]: (ilk sayı, ±ikinci) — eksi çıkarma. */
    data class Plan(val weak: List<RuleScore>, val questions: List<Pair<Int, Int>>)

    private val KEY = Regex("""^(add|sub)_(direct|five|ten|bead)$""")

    fun plan(scores: List<RuleScore>, seed: Int): Plan? {
        val known = scores.filter { KEY.matches(it.key) && it.total > 0 }
        val rated = known.filter { it.total >= MIN_OPS }.sortedWith(compareBy({ it.percent }, { -it.total }))
        if (rated.isEmpty()) return null
        val weak = rated.filter { it.percent < WEAK_PCT }.take(2).ifEmpty { listOf(rated.first()) }
        val allowed = known.map { it.key }.toMutableSet().apply {
            if (any { it.startsWith("add_") }) add("add_direct")
            if (any { it.startsWith("sub_") }) add("sub_direct")
        }
        val strong = known.map { it.key }.filter { k -> weak.none { it.key == k } }

        // Pekiştirme soruları 3., 6. ve 9. sırada: zayıf kural soruları art arda yorucu olmasın.
        val strongSlots = if (strong.isEmpty()) emptySet() else setOf(2, 5, 8)
        val out = ArrayList<Pair<Int, Int>>()
        val seen = HashSet<Pair<Int, Int>>()
        var weakTurn = 0
        var strongTurn = 0
        for (i in 0 until QUESTION_COUNT) {
            val (key, level) = if (i in strongSlots) {
                strong[strongTurn++ % strong.size] to 2
            } else {
                val w = weak[weakTurn++ % weak.size]
                w.key to (if (w.percent < EASY_PCT) 2 else 3)
            }
            var q: Pair<Int, Int>? = null
            for (attempt in 0 until 6) {
                val p = ExamplePicker.pick(key, seed * 131 + i * 17 + attempt * 1009, level, allowed) ?: break
                if (p !in seen) { q = p; break }
            }
            if (q != null) {
                seen += q
                out += q
            }
        }
        if (out.size < QUESTION_COUNT / 2) return null
        return Plan(weak, out)
    }
}
