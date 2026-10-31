package com.example.app.abacus

import com.example.app.abacus.SobiTestPlanner.RuleScore
import com.example.app.homework.HomeworkOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [SobiTestPlanner]: Sobi'nin günlük testi çocuğun en zayıf kuralını çalıştırmalı, görmediği kuralı sormamalı. */
class SobiTestPlannerTest {

    private fun rules(q: Pair<Int, Int>): Set<String> {
        val op = if (q.second < 0) HomeworkOp.SUB else HomeworkOp.ADD
        val e = StepExplainer.explain(q.first.toLong(), op, Math.abs(q.second).toLong())!!
        val prefix = if (op == HomeworkOp.ADD) "add_" else "sub_"
        return e.moves.flatMap { StepExplainer.rulesUsed(it) }.map { prefix + it.key }.toSet()
    }

    @Test
    fun en_zayif_kural_cogunlukta_gorulmemis_kural_yok() {
        val scores = listOf(
            RuleScore("add_direct", 40, 2),
            RuleScore("add_five", 30, 3),
            RuleScore("add_ten", 12, 10), // %54: en zayıf
        )
        val plan = SobiTestPlanner.plan(scores, seed = 7)!!
        assertEquals(listOf("add_ten"), plan.weak.map { it.key })
        assertEquals(SobiTestPlanner.QUESTION_COUNT, plan.questions.size)
        assertEquals("sorular tekrar etmesin", plan.questions.size, plan.questions.toSet().size)
        val withTen = plan.questions.count { "add_ten" in rules(it) }
        assertTrue("10'luk soru: $withTen", withTen >= 7)
        // Görülmemiş kurallar (boncuk, bütün çıkarma) yok.
        for (q in plan.questions) assertTrue("$q ${rules(q)}", rules(q).all { it in setOf("add_direct", "add_five", "add_ten") })
    }

    @Test
    fun iki_zayif_kural_ve_cikarma() {
        val scores = listOf(
            RuleScore("add_direct", 50, 0), RuleScore("add_five", 20, 15), RuleScore("add_ten", 20, 4),
            RuleScore("add_bead", 10, 2), RuleScore("sub_direct", 30, 1), RuleScore("sub_five", 6, 9),
        )
        val plan = SobiTestPlanner.plan(scores, seed = 3)!!
        assertEquals(listOf("sub_five", "add_five"), plan.weak.map { it.key })
        assertTrue(plan.questions.any { it.second < 0 && "sub_five" in rules(it) })
        assertTrue(plan.questions.any { it.second > 0 && "add_five" in rules(it) })
    }

    @Test
    fun yeterli_veri_yoksa_test_yok() {
        assertNull(SobiTestPlanner.plan(emptyList(), 1))
        assertNull(SobiTestPlanner.plan(listOf(RuleScore("add_ten", 3, 2)), 1))
    }

    @Test
    fun hepsi_iyiyse_en_dusugu_pekistirilir() {
        val plan = SobiTestPlanner.plan(listOf(RuleScore("add_five", 50, 1), RuleScore("add_ten", 40, 3)), 5)!!
        assertEquals(listOf("add_ten"), plan.weak.map { it.key })
    }
}
