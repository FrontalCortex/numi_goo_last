package com.example.app.abacus

import com.example.app.abacus.SobiTestPlanner.RuleScore
import com.example.app.homework.HomeworkOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [SobiTestPlanner] ve [SobiTestLevels]: test çocuğun en zayıf kuralını, kendi seviyesinde çalıştırmalı. */
class SobiTestPlannerTest {

    private val add = listOf(RuleScore("add_direct", 40, 2), RuleScore("add_five", 30, 3), RuleScore("add_ten", 12, 10))

    @Test
    fun en_zayif_kural_cogunlukta_gorulmemis_kural_yok() {
        val plan = SobiTestPlanner.plan(add, emptyMap(), 0, seed = 7)!!
        assertEquals(listOf("add_ten"), plan.weak.map { it.key })
        assertEquals(SobiTestPlanner.QUESTION_COUNT, plan.questions.size)
        assertEquals("sorular tekrar etmesin", plan.questions.size, plan.questions.map { it.question.numbers }.toSet().size)
        val withTen = plan.questions.count { "add_ten" in SobiTestPlanner.rulesOf(it.question) }
        assertTrue("10'luk soru: $withTen", withTen >= 7)
        for (q in plan.questions) {
            assertTrue("${q.question} ${SobiTestPlanner.rulesOf(q.question)}",
                SobiTestPlanner.rulesOf(q.question).all { it in setOf("add_direct", "add_five", "add_ten") })
        }
        // Kayıtlı seviye yok: %54 başarı → 1. seviye.
        assertEquals(1, plan.played["add_ten"])
    }

    @Test
    fun seviyeye_gore_basamak_ve_sayi_adedi() {
        // 10'luk toplama 5. seviye: 2 basamaklı 3 sayı, 2,4 sn.
        val plan = SobiTestPlanner.plan(add, mapOf("add_ten" to 5), 0, seed = 11)!!
        val ten = plan.questions.filter { it.rule == "add_ten" }
        assertTrue(ten.isNotEmpty())
        for (q in ten) {
            assertEquals(5, q.level)
            assertEquals(3, q.question.numbers.size)
            assertTrue(q.question.numbers.all { it in 10L..99L })
            assertEquals(2400L, q.intervalMs)
        }
        // "Zor" seçimi: 6. seviye (2 basamaklı 4 sayı).
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
            assertTrue("eksiye düşmemeli", q.question.answer >= 0)
        }
        assertTrue("12. seviye: 3 basamak", sub.any { q -> q.question.numbers.drop(1).all { it in 100L..999L } })
    }

    @Test
    fun her_kural_her_seviyede_soru_kurulur() {
        val all = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead").toSet()
        val rnd = Random(1)
        for (key in all) {
            for (level in listOf(1, 2, 5, 10, 15, 20)) {
                val q = SobiTestPlanner.question(key, level, all, rnd) ?: error("$key seviye $level soru yok")
                assertTrue("$key $level ${q.question}", key in SobiTestPlanner.rulesOf(q.question))
            }
        }
    }

    @Test
    fun seviye_degisimi() {
        assertEquals(6, SobiTestLevels.next(current = 5, played = 5, percent = 85))
        assertEquals(5, SobiTestLevels.next(current = 5, played = 5, percent = 70))
        assertEquals(4, SobiTestLevels.next(current = 5, played = 5, percent = 40))
        // Kolay seçip başarmak seviyeyi şişirmez; zor seçip başarmak hızlandırır.
        assertEquals(5, SobiTestLevels.next(current = 5, played = 4, percent = 100))
        assertEquals(7, SobiTestLevels.next(current = 5, played = 6, percent = 90))
        // Zor seçip zorlanmak seviyeyi düşürmez.
        assertEquals(5, SobiTestLevels.next(current = 5, played = 6, percent = 30))
        assertEquals(SobiTestLevels.MAX, SobiTestLevels.next(SobiTestLevels.MAX, SobiTestLevels.MAX, 100))
        assertEquals(1, SobiTestLevels.next(1, 1, 0))
    }

    @Test
    fun yeterli_veri_yoksa_test_yok() {
        assertNull(SobiTestPlanner.plan(emptyList(), emptyMap(), 0, 1))
        assertNull(SobiTestPlanner.plan(listOf(RuleScore("add_ten", 3, 2)), emptyMap(), 0, 1))
    }
}
