// Android app/src/test/java/com/example/app/abacus/StepExplainerTest.kt'in kopyası: ders örnekleri,
// sığmayan işlemler ve bağımsız boncuk simülasyonu. Ortak tablo (shared/step_explainer_cases.json)
// dosya okuduğu için jvmTest'te (StepExplainerTabloTest). RuleAnalysis.rulesOf karşılaştırması
// taşınmadı (ödev motoru ortak kodda yok).
package com.sorobit.shared.sobianaliz

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class StepExplainerTest {

    private fun plan(base: Long, op: HomeworkOp, number: Long) =
        StepExplainer.encode(StepExplainer.explain(base, op, number))

    private val ADD = HomeworkOp.ADD
    private val SUB = HomeworkOp.SUB

    @Test
    fun kuralsiz_toplama() {
        assertEquals("c1+6=direct[direct@1u+1l+1]>60 c0+8=direct[direct@0u+1l+3]>68", plan(0, ADD, 68))
    }

    @Test
    fun besli_toplama_5_gelir_kardes_gider() {
        assertEquals("c0+4=five[five@0u+1 friend@0l-1]>7", plan(3, ADD, 4))
    }

    @Test
    fun onluk_toplama_10_gelir_buyuk_kardes_gider() {
        assertEquals("c0+4=ten[ten{c1+1=direct[direct@1l+1]>28} big_friend@0u-1l-1]>22", plan(18, ADD, 4))
    }

    @Test
    fun boncuk_kurali_toplama() {
        assertEquals("c0+6=bead[beads@0l+1 five@0u-1 ten{c1+1=direct[direct@1l+1]>14}]>14", plan(8, ADD, 6))
        assertEquals(
            "c1+7=bead[beads@1l+2 five@1u-1 ten{c2+1=direct[direct@2l+1]>138}]>138 " +
                "c0+6=bead[beads@0l+1 five@0u-1 ten{c1+1=direct[direct@1l+1]>144}]>144",
            plan(68, ADD, 76),
        )
    }

    @Test
    fun elde_zinciri_10_gelirken_5lik() {
        assertEquals("c0+1=ten[ten{c1+1=five[five@1u+1 friend@1l-4]>59} big_friend@0u-1l-4]>50", plan(49, ADD, 1))
        assertEquals(
            "c0+1=ten[ten{c1+1=ten[ten{c2+1=direct[direct@2l+1]>199} big_friend@1u-1l-4]>109} big_friend@0u-1l-4]>100",
            plan(99, ADD, 1),
        )
    }

    @Test
    fun besli_cikarma_5_gider_kardes_gelir() {
        assertEquals("c0-1=five[five@0u-1 friend@0l+4]>4", plan(5, SUB, 1))
    }

    @Test
    fun onluk_cikarma_10_gider_kardes_gelir() {
        assertEquals("c0-1=ten[ten{c1-1=direct[direct@1l-1]>0} big_friend@0u+1l+4]>9", plan(10, SUB, 1))
    }

    @Test
    fun boncuk_cikarma() {
        assertEquals("c0-6=bead[ten{c1-1=direct[direct@1l-1]>13} five@0u+1 friend@0l-1]>17", plan(23, SUB, 6))
    }

    @Test
    fun sigmayan_islem_null() {
        assertNull(StepExplainer.explain(99_999, ADD, 1))
        assertNull(StepExplainer.explain(0, SUB, 1))
        assertNull(StepExplainer.explain(-1, ADD, 1))
        assertEquals("error", plan(5, SUB, 6))
    }

    @Test
    fun sifir_eklemek_hareketsiz() {
        val e = StepExplainer.explain(42, ADD, 0)
        assertNotNull(e)
        assertEquals(42L, e.result)
        assertTrue(e.moves.isEmpty())
    }

    @Test
    fun gecis_hedefe_gore_toplama_ya_da_cikarma() {
        assertEquals(plan(68, ADD, 76), StepExplainer.encode(StepExplainer.explainTransition(68, 144)))
        assertEquals(plan(23, SUB, 6), StepExplainer.encode(StepExplainer.explainTransition(23, 17)))
    }

    @Test
    fun kural_kumesi_elde_zinciri_dahil() {
        // 1953 + 70: onlarda boncuk kuralı, eldede yüzler 9 → 10'luk.
        val m = StepExplainer.explain(1953, ADD, 70)!!.moves.single()
        assertEquals(setOf(AbacusRule.BEAD, AbacusRule.TEN), StepExplainer.rulesUsed(m))
        // 18 + 4: eldenin doğrudan +1'i kural sayılmaz.
        val m2 = StepExplainer.explain(18, ADD, 4)!!.moves.single()
        assertEquals(setOf(AbacusRule.TEN), StepExplainer.rulesUsed(m2))
    }

    // ── Bağımsız boncuk simülasyonu ─────────────────────────────────────────

    private class Beads(value: Long, capacity: Int) {
        val heaven = BooleanArray(capacity)
        val earth = IntArray(capacity)

        init {
            var v = value
            for (c in 0 until capacity) {
                val d = (v % 10).toInt(); v /= 10
                heaven[c] = d >= 5; earth[c] = d % 5
            }
        }

        fun value(): Long = (heaven.indices.reversed()).fold(0L) { acc, c ->
            acc * 10 + (if (heaven[c]) 5 else 0) + earth[c]
        }

        fun apply(s: StepExplainer.Step, where: String) {
            val carry = s.carry
            if (carry != null) {
                assertEquals(0, s.upper, "$where: elde adımında bu sütunda boncuk oynamaz")
                assertEquals(0, s.lower, "$where: elde adımında bu sütunda boncuk oynamaz")
                carry.steps.forEach { apply(it, where) }
                return
            }
            assertTrue(s.upper != 0 || s.lower != 0, "$where: boş adım")
            when (s.upper) {
                1 -> { assertTrue(!heaven[s.col], "$where: beşlik zaten inik"); heaven[s.col] = true }
                -1 -> { assertTrue(heaven[s.col], "$where: gidecek beşlik yok"); heaven[s.col] = false }
                0 -> Unit
                else -> fail("$where: upper=${s.upper}")
            }
            val e = earth[s.col] + s.lower
            assertTrue(e in 0..4, "$where: birlik boncuk sınırı aşıldı ($e)")
            earth[s.col] = e
        }
    }

    private fun expectedRoles(op: HomeworkOp, rule: AbacusRule): List<StepExplainer.Role> = when (rule) {
        AbacusRule.DIRECT -> listOf(StepExplainer.Role.DIRECT)
        AbacusRule.FIVE -> listOf(StepExplainer.Role.FIVE, StepExplainer.Role.FRIEND)
        AbacusRule.TEN -> listOf(StepExplainer.Role.TEN, StepExplainer.Role.BIG_FRIEND)
        AbacusRule.BEAD -> if (op == ADD) listOf(StepExplainer.Role.BEADS, StepExplainer.Role.FIVE, StepExplainer.Role.TEN)
        else listOf(StepExplainer.Role.TEN, StepExplainer.Role.FIVE, StepExplainer.Role.FRIEND)
    }

    private fun checkMove(m: StepExplainer.ColumnMove, where: String) {
        assertEquals(expectedRoles(m.op, m.rule), m.steps.map { it.role }, "$where: adım sırası")
        if (m.isCarry) assertEquals(1, m.x, "$where: elde ±1")
        m.steps.forEach { s -> s.carry?.let { checkMove(it, where) } }
    }

    private fun verify(base: Long, op: HomeworkOp, number: Long) {
        val where = "$base ${if (op == ADD) '+' else '-'} $number"
        val e = StepExplainer.explain(base, op, number)
        val expected = if (op == ADD) base + number else base - number
        if (expected !in 0..99_999L) { assertNull(e, where); return }
        assertNotNull(e, where)
        assertEquals(expected, e.result, where)
        val beads = Beads(base, StepExplainer.DEFAULT_CAPACITY)
        var value = base
        for (m in e.moves) {
            assertEquals(value, m.valueBefore, "$where: hareket öncesi değer")
            checkMove(m, where)
            m.steps.forEach { beads.apply(it, where) }
            assertEquals(beads.value(), m.valueAfter, "$where: hareket sonrası değer")
            value = m.valueAfter
        }
        assertEquals(expected, beads.value(), where)
    }

    @Test
    fun tek_rakam_her_durumda() {
        // Android: 0..9999 taban; Native'de süre için 0..1999 (her sütun bağlamı yine kapsanıyor).
        for (base in 0L..1_999L) for (x in 1L..9L) for (op in listOf(ADD, SUB)) {
            verify(base, op, x)
            verify(base, op, x * 10)
        }
    }

    @Test
    fun rastgele_cok_basamakli() {
        val rng = Random(20261028)
        repeat(5_000) {
            val base = rng.nextLong(0, 100_000)
            val number = rng.nextLong(0, 100_000)
            verify(base, if (rng.nextBoolean()) ADD else SUB, number)
        }
    }

    @Test
    fun abakus_kurallari_siniflandirma() {
        assertEquals(AbacusRule.DIRECT, AbacusMoves.classifyAdd(1, 3))
        assertEquals(AbacusRule.FIVE, AbacusMoves.classifyAdd(3, 4))
        assertEquals(AbacusRule.TEN, AbacusMoves.classifyAdd(8, 4))
        assertEquals(AbacusRule.BEAD, AbacusMoves.classifyAdd(8, 6))
        assertEquals(AbacusRule.DIRECT, AbacusMoves.classifySub(4, 3))
        assertEquals(AbacusRule.FIVE, AbacusMoves.classifySub(5, 1))
        assertEquals(AbacusRule.TEN, AbacusMoves.classifySub(0, 1))
        assertEquals(AbacusRule.BEAD, AbacusMoves.classifySub(3, 6))
        assertEquals(AbacusRule.TEN, AbacusRule.fromKey("ten"))
        assertEquals(HomeworkOp.SUB, HomeworkOp.fromKey("sub"))
    }
}
