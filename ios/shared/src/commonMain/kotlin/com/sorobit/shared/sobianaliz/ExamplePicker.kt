// Kaynak: app/src/main/java/com/example/app/abacus/ExamplePicker.kt — tamamı, birebir.
// `Math.floorMod` yerine aynı sonucu veren [floorMod] yazıldı; kotlin.random.Random(seed) her
// platformda aynı (XorWow) diziyi verir, yani aynı tohumla Android'le aynı örnek seçilir.
package com.sorobit.shared.sobianaliz

import kotlin.random.Random

/**
 * ÖRNEK İŞLEM SEÇİCİ — dersin kuralını kullanan bir işlem. Sobi ekranı bu işlemi adım adım çözdürür.
 * Yapay zekâ yok: örnek kural motoruyla seçiliyor, yani her zaman o kuralı gerçekten gerektiriyor.
 *
 * ZORLUK ([pick] `level`): çocuk "Daha zor bir örnek"e dokundukça artar.
 *  1. Tek basamak, birlerde tek hamle (8 + 7).
 *  2. İki basamaklı ilk sayı, tek basamaklı ikinci (27 + 8).
 *  3. İki basamaklı iki sayı; kural en az iki sütunda (47 + 38).
 *  4. Üç basamaklı iki sayı; kural en az iki sütunda (395 + 128).
 *  5. Dört basamaklı iki sayı; kural en az iki sütunda (4867 + 3958). Sonuç en çok 5 basamak.
 * Kuralsız derste "kural en az iki sütunda" yerine bütün sütunlar kuralsız. İşlem çocuğun öğrenmediği
 * bir kural istemez ([pick] `allowed`).
 */
object ExamplePicker {

    const val MAX_LEVEL = 5
    private const val TRIES = 4000

    /**
     * (ilk sayı, ±ikinci); [seed] her istekte değişsin ki aynı örnek gelmesin. O düzeyde örnek
     * bulunamazsa bir alt düzey denenir; hiç yoksa null.
     */
    fun pick(ruleKey: String, seed: Int, level: Int = 1, allowed: Collection<String>? = null): Pair<Int, Int>? {
        for (lv in level.coerceIn(1, MAX_LEVEL) downTo 1) {
            if (lv == 1) {
                val list = candidates(ruleKey)
                if (list.isNotEmpty()) return list[floorMod(seed, list.size)]
            } else {
                search(ruleKey, seed, lv, allowed)?.let { return it }
            }
        }
        return null
    }

    /** 1. düzeyin bütün örnekleri. */
    fun candidates(ruleKey: String): List<Pair<Int, Int>> {
        val (add, rule) = parse(ruleKey) ?: return emptyList()
        val out = ArrayList<Pair<Int, Int>>()
        for (base in if (add) 1..9 else 1..18) {
            for (x in 1..9) {
                if (!add && base - x < 0) continue
                val e = StepExplainer.explain(base.toLong(), if (add) HomeworkOp.ADD else HomeworkOp.SUB, x.toLong()) ?: continue
                val m = e.moves.singleOrNull() ?: continue
                if (m.col == 0 && m.rule == rule) out += base to (if (add) x else -x)
            }
        }
        return out
    }

    private fun search(ruleKey: String, seed: Int, level: Int, allowed: Collection<String>?): Pair<Int, Int>? {
        val (add, rule) = parse(ruleKey) ?: return null
        val prefix = if (add) "add_" else "sub_"
        val (firstRange, secondRange) = when (level) {
            2 -> 10..99 to 1..9
            3 -> 10..99 to 10..99
            4 -> 100..999 to 100..999
            else -> 1000..9999 to 1000..9999
        }
        val rnd = Random(seed * 31 + level)
        repeat(TRIES) {
            val n = secondRange.random(rnd)
            val base = firstRange.random(rnd)
            if (!add && base < n) return@repeat
            val e = StepExplainer.explain(base.toLong(), if (add) HomeworkOp.ADD else HomeworkOp.SUB, n.toLong()) ?: return@repeat
            val used = e.moves.flatMap { StepExplainer.rulesUsed(it) }.toSet()
            if (rule !in used) return@repeat
            if (allowed != null && used.any { prefix + it.key !in allowed }) return@repeat
            val ok = if (rule == AbacusRule.DIRECT) {
                e.moves.all { it.rule == AbacusRule.DIRECT }
            } else {
                level == 2 || e.moves.count { it.rule != AbacusRule.DIRECT } >= 2
            }
            if (ok) return base to (if (add) n else -n)
        }
        return null
    }

    private fun parse(ruleKey: String): Pair<Boolean, AbacusRule>? {
        val add = when {
            ruleKey.startsWith("add_") -> true
            ruleKey.startsWith("sub_") -> false
            else -> return null
        }
        val rule = AbacusRule.fromKey(ruleKey.substringAfter('_')) ?: return null
        return add to rule
    }

    /** `Math.floorMod(a, b)` (b > 0). */
    private fun floorMod(a: Int, b: Int): Int {
        val r = a % b
        return if (r < 0) r + b else r
    }
}
