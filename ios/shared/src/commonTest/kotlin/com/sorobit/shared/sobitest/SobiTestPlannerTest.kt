// Android app/src/test/java/com/example/app/abacus/SobiTestPlannerTest.kt'in kopyası (beklentiler aynı;
// yalnızca JUnit → kotlin.test: ileti parametresi sona alındı).
package com.sorobit.shared.sobitest

import com.sorobit.shared.sobianaliz.HomeworkOp
import com.sorobit.shared.sobitest.SobiTestPlanner.RuleScore
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SobiTestPlannerTest {

    private val add = listOf(RuleScore("add_direct", 40, 2), RuleScore("add_five", 30, 3), RuleScore("add_ten", 12, 10))

    @Test
    fun en_zayif_kural_cogunlukta_gorulmemis_kural_yok() {
        val plan = SobiTestPlanner.plan(add, emptyMap(), 0, seed = 7)!!
        assertEquals(listOf("add_ten"), plan.weak.map { it.key })
        assertEquals(SobiTestPlanner.QUESTION_COUNT, plan.questions.size)
        assertEquals(plan.questions.size, plan.questions.map { it.question.numbers }.toSet().size, "sorular tekrar etmesin")
        val withTen = plan.questions.count { "add_ten" in SobiTestPlanner.rulesOf(it.question) }
        assertTrue(withTen >= 7, "10'luk soru: $withTen")
        for (q in plan.questions) {
            assertTrue(SobiTestPlanner.rulesOf(q.question).all { it in setOf("add_direct", "add_five", "add_ten") },
                "${q.question} ${SobiTestPlanner.rulesOf(q.question)}")
        }
        // Kayıtlı seviye yok: %54 başarı → 1. seviye.
        assertEquals(1, plan.played["add_ten"])
    }

    @Test
    fun seviyeye_gore_basamak_ve_sayi_adedi() {
        val plan = SobiTestPlanner.plan(add, mapOf("add_ten" to 5), 0, seed = 11)!!
        val ten = plan.questions.filter { it.rule == "add_ten" }
        assertTrue(ten.isNotEmpty())
        for (q in ten) {
            assertEquals(5, q.level)
            assertEquals(3, q.question.numbers.size)
            assertTrue(q.question.numbers.all { it in 10L..99L })
            assertEquals(2400L, q.intervalMs)
        }
        val hard = SobiTestPlanner.plan(add, mapOf("add_ten" to 5), +1, seed = 11)!!
        assertEquals(6, hard.played["add_ten"])
        assertTrue(hard.questions.filter { it.rule == "add_ten" }.all { it.question.numbers.size == 4 })
    }

    @Test
    fun yuksek_seviye_cikarma_cok_sayi() {
        val scores = listOf(RuleScore("add_direct", 50, 0), RuleScore("sub_direct", 30, 1), RuleScore("sub_five", 20, 2),
            RuleScore("sub_ten", 15, 9))
        val plan = SobiTestPlanner.plan(scores, mapOf("sub_ten" to 12), 0, seed = 5)!!
        val sub = plan.questions.filter { it.rule == "sub_ten" }
        assertTrue(sub.isNotEmpty())
        for (q in sub) {
            assertEquals(HomeworkOp.SUB, q.question.op)
            assertTrue("sub_ten" in SobiTestPlanner.rulesOf(q.question))
            assertTrue(q.question.answer >= 0, "eksiye düşmemeli")
        }
        assertTrue(sub.any { q -> q.question.numbers.drop(1).all { it in 100L..999L } }, "12. seviye: 3 basamak")
    }

    @Test
    fun her_kural_her_seviyede_soru_kurulur() {
        val all = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead").toSet()
        val rnd = Random(1)
        for (key in all) {
            for (level in listOf(1, 2, 5, 10, 15, 20)) {
                val q = SobiTestPlanner.question(key, level, all, rnd) ?: error("$key seviye $level soru yok")
                assertTrue(key in SobiTestPlanner.rulesOf(q.question), "$key $level ${q.question}")
            }
        }
    }

    @Test
    fun seviye_degisimi() {
        assertEquals(6, SobiTestLevels.next(current = 5, played = 5, percent = 85))
        assertEquals(5, SobiTestLevels.next(current = 5, played = 5, percent = 70))
        assertEquals(4, SobiTestLevels.next(current = 5, played = 5, percent = 40))
        assertEquals(5, SobiTestLevels.next(current = 5, played = 4, percent = 100))
        assertEquals(7, SobiTestLevels.next(current = 5, played = 6, percent = 90))
        assertEquals(5, SobiTestLevels.next(current = 5, played = 6, percent = 30))
        assertEquals(SobiTestLevels.MAX, SobiTestLevels.next(SobiTestLevels.MAX, SobiTestLevels.MAX, 100))
        assertEquals(1, SobiTestLevels.next(1, 1, 0))
    }

    @Test
    fun yeterli_veri_yoksa_test_yok() {
        assertNull(SobiTestPlanner.plan(emptyList(), emptyMap(), 0, 1))
        assertNull(SobiTestPlanner.plan(listOf(RuleScore("add_ten", 3, 2)), emptyMap(), 0, 1))
    }

    // ── Abaküssüz (zihinden) sorular (07.11.2026) ──────────────────────────

    @Test
    fun abakussuz_gorulmemisse_test_tamamen_abakuslu() {
        val scores = add + RuleScore("m_add_ten", 2, 9)
        val plan = SobiTestPlanner.plan(scores, emptyMap(), 0, seed = 7)!!
        assertTrue(plan.questions.none { it.mental })
        assertTrue(plan.mentalWeak.isEmpty())
    }

    @Test
    fun abakussuz_yalnizca_gorulen_kurallardan() {
        val plan = SobiTestPlanner.plan(add, emptyMap(), 0, seed = 3, lessonSeen = setOf("add_direct", "add_five"))!!
        val mental = plan.questions.filter { it.mental }
        assertEquals(3, mental.size)
        assertEquals(listOf("m_add_five"), plan.mentalWeak.map { it.key })
        assertEquals(1, plan.played["m_add_five"])
        for (q in mental) {
            val used = SobiTestPlanner.rulesOf(q.question)
            assertTrue("add_five" in used && used.all { it in setOf("add_direct", "add_five") }, "${q.question} $used")
            assertEquals("m_add_five", q.statKey)
        }
        assertTrue(plan.questions.count { !it.mental && it.rule == "add_ten" } >= 5)
    }

    @Test
    fun zihinden_cok_zayifsa_bes_soru() {
        val scores = add + RuleScore("m_add_ten", 3, 12) + RuleScore("m_add_direct", 20, 0)
        val plan = SobiTestPlanner.plan(scores, mapOf("m_add_ten" to 4), 0, seed = 9,
            lessonSeen = setOf("add_direct", "add_five", "add_ten"))!!
        assertEquals(listOf("m_add_ten"), plan.mentalWeak.map { it.key })
        val mental = plan.questions.filter { it.mental }
        assertEquals(5, mental.size)
        assertTrue(mental.all { it.level <= 4 && "add_ten" in SobiTestPlanner.rulesOf(it.question) })
        assertEquals(4, plan.current["m_add_ten"])
    }

    @Test
    fun abakussuz_her_kural_her_seviyede_kurulur() {
        val all = setOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")
        val rnd = Random(2)
        for (key in all) {
            for (level in listOf(1, 3, 8, 14, 20)) {
                val q = SobiTestPlanner.question(key, level, all, rnd, withAbacus = false) ?: error("$key seviye $level soru yok")
                assertTrue(q.mental)
                assertTrue(key in SobiTestPlanner.rulesOf(q.question), "$key $level ${q.question}")
            }
        }
    }

    @Test
    fun kormeleme_dersinden_gorulen_kurallar() {
        fun h(id: String) = SobiTestPlanner.LessonMark(id, isHeader = true, finished = false)
        fun l(id: String, done: Boolean) = SobiTestPlanner.LessonMark(id, isHeader = false, finished = done)
        val part4 = listOf(
            h("p4_i00_kuralsiz_toplama"), l("p4_i01", false), l("p4_i02", false),
            h("p4_i05_5_lik_toplama"), l("p4_i06", false), l("p4_i07", true),
            h("p4_i10_10_luk_toplama_1_2_3_4_5"), l("p4_i11", false),
            h("p4_i15_10_luk_toplama_6_7_8_9"), l("p4_i19_unite_maratonu", true),
            h("p4_yeni_bolum"), l("p4_x", true),
        )
        assertEquals(setOf("add_five", "add_ten"), SobiTestPlanner.seenFromLessons(part4))
        assertEquals(emptySet<String>(), SobiTestPlanner.seenFromLessons(listOf(l("p5_i01", true))))
    }

    // ── Çarpma (08.11.2026) ─────────────────────────────────────────────────

    @Test
    fun carpma_dersi_gorulmemisse_gelmez() {
        val plan = SobiTestPlanner.plan(add + RuleScore("mulA_2x1", 2, 20), emptyMap(), 0, seed = 4)!!
        assertTrue(plan.questions.none { it.question.op == HomeworkOp.MUL })
    }

    @Test
    fun carpma_zayifsa_kural_gibi_secilir() {
        val scores = add + RuleScore("mulA_2x1", 2, 13)
        val plan = SobiTestPlanner.plan(scores, emptyMap(), 0, seed = 6, lessonSeen = setOf("mulA_2x1", "mulA_2x2"))!!
        assertEquals(listOf("mulA_2x1", "add_ten"), plan.weak.map { it.key })
        assertNull(plan.current["mulA_2x1"])
        assertNull(plan.played["mulA_2x1"])
        val mul = plan.questions.filter { it.question.op == HomeworkOp.MUL }
        assertTrue(mul.size >= 3, "çarpma sorusu: ${mul.size}")
        for (q in mul) {
            assertTrue(!q.mental)
            assertTrue(q.rule in setOf("mulA_2x1", "mulA_2x2"))
            assertEquals(q.rule, SobiTestPlanner.mulKeyOf(q.question.numbers[0], q.question.numbers[1], withAbacus = true))
        }
        assertTrue(mul.filter { it.rule == "mulA_2x1" }.all { it.question.numbers[0] in 10L..99L && it.question.numbers[1] in 2L..9L })
    }

    @Test
    fun zihinden_carpma_part6dan() {
        val plan = SobiTestPlanner.plan(add, emptyMap(), 0, seed = 8, lessonSeen = setOf("mulM_2x1"))!!
        assertEquals(listOf("mulM_2x1"), plan.mentalWeak.map { it.key })
        val mental = plan.questions.filter { it.mental }
        assertEquals(3, mental.size)
        assertTrue(mental.all { it.question.op == HomeworkOp.MUL && it.statKey == "mulM_2x1" })
    }

    @Test
    fun carpma_bolumleri_ve_anahtar() {
        fun h(id: String) = SobiTestPlanner.LessonMark(id, isHeader = true, finished = false)
        fun l(id: String, done: Boolean) = SobiTestPlanner.LessonMark(id, isHeader = false, finished = done)
        val part3 = listOf(h("p3_i00_2_ye_1_carpma"), l("p3_i01", true), h("p3_i05_2_ye_2_carpma"), l("p3_i06", false),
            h("p3_i15_3_e_2_carpma"), l("p3_i19_unite_maratonu", true))
        assertEquals(setOf("mulA_2x1", "mulA_3x2"), SobiTestPlanner.seenFromLessons(part3))
        val part6 = listOf(h("p6_i00_2_ye_1_carpma_kolay"), l("p6_i01", false), h("p6_i05_2_ye_1_carpma_zor"), l("p6_i06", true))
        assertEquals(setOf("mulM_2x1"), SobiTestPlanner.seenFromLessons(part6))
        assertEquals("mulM_2x1", SobiTestPlanner.mulKeyOf(7, 43, withAbacus = false))
        assertEquals("mulA_3x2", SobiTestPlanner.mulKeyOf(123, 45, withAbacus = true))
    }
}
