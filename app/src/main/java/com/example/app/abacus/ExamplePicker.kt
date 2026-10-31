package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp
import kotlin.random.Random

/**
 * ÖRNEK İŞLEM SEÇİCİ — öğreticide "Bir örnek ver": dersin kuralını kullanan bir işlem (kullanıcı kararı
 * 01.11.2026). Sobi ekranı bu işlemi adım adım çözdürür.
 *
 * Yapay zekâ yok: örnek kural motoruyla seçiliyor, yani her zaman o kuralı gerçekten gerektiriyor
 * (10'luk toplama dersinde 8 + 7, 5'lik çıkarmada 7 − 4) ve ücretsiz.
 *
 * ZORLUK ([pick] `level`): çocuk "Daha zor bir örnek"e dokundukça artar, kendiliğinden değişmez
 * (kullanıcı kararı: örnek hep dersin konusundan).
 *  1. Tek basamak, birlerde tek hamle (8 + 7).
 *  2. İki basamaklı ilk sayı, tek basamaklı ikinci (27 + 8).
 *  3. İki basamaklı iki sayı; kural en az iki sütunda (47 + 38).
 *  4. Üç basamaklı iki sayı; kural en az iki sütunda (395 + 128).
 * Kuralsız derste "kural en az iki sütunda" yerine bütün sütunlar kuralsız. İşlem çocuğun öğrenmediği
 * bir kural istemez ([pick] `allowed`). Kural anahtarları [com.example.app.SobiCurriculum] ile aynı.
 */
object ExamplePicker {

    const val MAX_LEVEL = 4
    private const val TRIES = 4000

    /**
     * (ilk sayı, ±ikinci); [seed] her istekte değişsin ki aynı örnek gelmesin. O düzeyde örnek
     * bulunamazsa bir alt düzey denenir; hiç yoksa null.
     */
    fun pick(ruleKey: String, seed: Int, level: Int = 1, allowed: Collection<String>? = null): Pair<Int, Int>? {
        for (lv in level.coerceIn(1, MAX_LEVEL) downTo 1) {
            if (lv == 1) {
                val list = candidates(ruleKey)
                if (list.isNotEmpty()) return list[Math.floorMod(seed, list.size)]
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
            else -> 100..999 to 100..999
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
}
