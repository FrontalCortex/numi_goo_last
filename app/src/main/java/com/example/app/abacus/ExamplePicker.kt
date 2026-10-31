package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp

/**
 * ÖRNEK İŞLEM SEÇİCİ — öğreticide "Bir örnek ver": dersin kuralını kullanan küçük bir işlem
 * (kullanıcı kararı 01.11.2026). Sobi ekranı bu işlemi adım adım çözdürür.
 *
 * Yapay zekâ yok: örnek kural motoruyla seçiliyor, yani her zaman o kuralı gerçekten gerektiriyor
 * (10'luk toplama dersinde 8 + 7, 5'lik çıkarmada 7 − 4) ve ücretsiz. Birler basamağında tek hamle:
 * çocuk kuralı tek sütunda görsün; çıkarmada ilk sayı 18'e kadar (10'luk ve boncuk çıkarması onlar
 * basamağı ister).
 *
 * Kural anahtarları [com.example.app.SobiCurriculum] ile aynı ("add_ten", "sub_bead").
 */
object ExamplePicker {

    /** (ilk sayı, ±ikinci); [seed] her istekte değişsin ki aynı örnek gelmesin. Uygun örnek yoksa null. */
    fun pick(ruleKey: String, seed: Int): Pair<Int, Int>? {
        val list = candidates(ruleKey)
        if (list.isEmpty()) return null
        return list[Math.floorMod(seed, list.size)]
    }

    fun candidates(ruleKey: String): List<Pair<Int, Int>> {
        val add = when {
            ruleKey.startsWith("add_") -> true
            ruleKey.startsWith("sub_") -> false
            else -> return emptyList()
        }
        val rule = AbacusRule.fromKey(ruleKey.substringAfter('_')) ?: return emptyList()
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
}
