// MistakeAnalyzer'ın İngilizce dalı (Android app/.../abacus/MistakeAnalyzer.kt, `EN ->` dalları).
// Beklenen cümleler Android kodundan elle türetildi; Android'de bu dalın testi yok. Örnekler
// MistakeAnalyzerTest'teki Türkçe örneklerle aynı kayıtlar.
package com.sorobit.shared.sobianaliz

import com.sorobit.shared.sobianaliz.MistakeAnalyzer.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MistakeAnalyzerEnTest {

    private val write248 = listOf(0, 100, 200, 210, 220, 230, 240, 245, 246, 247, 248)

    /** [SobiAnalizDili.language] nesne düzeyinde; her test sonunda Türkçe'ye geri alınır. */
    private fun <T> ingilizce(blok: () -> T): T {
        SobiAnalizDili.language = "en"
        try {
            return blok()
        } finally {
            SobiAnalizDili.language = "tr"
        }
    }

    @Test
    fun skipped_add_10_248_plus_223() = ingilizce {
        val log = write248 + listOf(348, 448, 498, 488, 478, 468, 463, 462, 461)
        val d = MistakeAnalyzer.analyze(248, "+", 223, log, 461)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertEquals(
            "In the ones place, adding 3 needed big friends: add 10, take away its big friend 7. " +
                "You skipped the \"add 10\" step. 1 bead should have been added to the tens column.",
            d.text,
        )
    }

    @Test
    fun skipped_friend_41_minus_3() = ingilizce {
        val d = MistakeAnalyzer.analyze(41, "-", 3, listOf(0, 10, 20, 30, 40, 41, 31), 31)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertEquals(
            "In the ones place, taking away 3 needed big friends: take away 10, add its friend 7. " +
                "You skipped the \"add its friend 7\" step.",
            d.text,
        )
    }

    @Test
    fun skipped_take_away_10_carry_23_minus_6() = ingilizce {
        val d = MistakeAnalyzer.analyze(23, "-", 6, listOf(0, 10, 20, 21, 22, 23, 28, 27), 27)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertTrue(
            d.text.contains("You skipped the \"take away 10\" step. 1 bead should have been taken away from the tens column."),
            d.text,
        )
    }

    @Test
    fun wrong_column() = ingilizce {
        val d = MistakeAnalyzer.analyze(21, "+", 13, listOf(0, 10, 20, 21, 31, 32, 33), 33)!!
        assertEquals(Kind.WRONG_COLUMN, d.kind)
        assertEquals(
            "In the ones place, adding 3 needed direct: add 3. The ones column should be 4, but you got 3.",
            d.text,
        )
    }

    @Test
    fun wrong_first_number() = ingilizce {
        val log = listOf(0, 100, 200, 210, 220, 230, 235, 236, 237, 238, 338, 438, 448, 458, 461)
        val d = MistakeAnalyzer.analyze(248, "+", 223, log, 461)!!
        assertEquals(Kind.WRONG_FIRST_NUMBER, d.kind)
        assertEquals(
            "When writing the first number, the tens place should have been 4, but you got 3. " +
                "You did the second number right!",
            d.text,
        )
        assertNull(d.move)
    }

    @Test
    fun at_move_iki_dilde() {
        // 468'den birlere 3: önce 463 (7 gitti, 10 gelmedi), sonra yüzlere 1 → hiçbir kalıba uymuyor.
        val log = write248 + listOf(348, 448, 458, 468, 463, 563)
        ingilizce {
            val d = MistakeAnalyzer.analyze(248, "+", 223, log, 563)!!
            assertEquals(Kind.AT_MOVE, d.kind)
            assertEquals("Here's the mistake: In the ones place, adding 3 needed big friends: add 10, take away its big friend 7.", d.text)
        }
        val tr = MistakeAnalyzer.analyze(248, "+", 223, log, 563)!!
        assertEquals("Hata burada: Birler basamağında 3 eklerken 10'luk kural gerekiyordu: 10 gelir, büyük kardeşi 7 gider.", tr.text)
    }

    @Test
    fun diagnose_write() = ingilizce {
        assertEquals("The tens place should be 4, but you have 3.", MistakeAnalyzer.diagnoseWrite(248, 238))
        assertEquals("The hundreds place should be 2, but you have 0.", MistakeAnalyzer.diagnoseWrite(248, 48))
    }

    @Test
    fun step_phrases_and_names() = ingilizce {
        val m = StepExplainer.explain(8, HomeworkOp.ADD, 6)!!.moves.single()
        assertEquals(listOf("add 1 beads", "take away 5", "add 10"), m.steps.map { MistakeAnalyzer.stepPhrase(it, m) })
        val s = StepExplainer.explain(10, HomeworkOp.SUB, 1)!!.moves.single()
        assertEquals(listOf("take away 10", "add its friend 9"), s.steps.map { MistakeAnalyzer.stepPhrase(it, s) })
        assertEquals("ten thousands", MistakeAnalyzer.columnName(4))
        assertEquals("column 6", MistakeAnalyzer.columnName(5))
        assertEquals(
            listOf("direct", "small friends", "big friends", "mixed friends"),
            listOf(AbacusRule.DIRECT, AbacusRule.FIVE, AbacusRule.TEN, AbacusRule.BEAD).map { MistakeAnalyzer.ruleName(it) },
        )
    }

    @Test
    fun sequence_mistake_78_plus_9() = ingilizce {
        val list = MistakeAnalyzer.findSequenceMistakes(
            listOf(50, 28, 9), listOf(0, 50, 50, 60, 70, 75, 76, 77, 78, 78, 88, 86), listOf(0, 2, 9), 86,
        )!!
        assertTrue(list.single().diagnosis!!.text.endsWith("The ones column should be 7, but you got 6."), list[0].diagnosis!!.text)
    }

    @Test
    fun rule_summary() = ingilizce {
        fun m(rule: AbacusRule?) = MistakeAnalyzer.SequenceMistake(1, 0, 1, 2, rule, null)
        assertEquals(
            "2 mistakes with big friends and 1 mistake with mixed friends",
            MistakeAnalyzer.ruleSummary(listOf(m(AbacusRule.BEAD), m(AbacusRule.TEN), m(AbacusRule.TEN))),
        )
        assertEquals("1 mistake with small friends", MistakeAnalyzer.ruleSummary(listOf(m(AbacusRule.FIVE))))
        assertEquals(
            "1 mistake while writing the first number, 1 mistake in direct steps and 1 mistake with small friends",
            MistakeAnalyzer.ruleSummary(listOf(m(AbacusRule.FIVE), m(null), m(AbacusRule.DIRECT))),
        )
    }

    @Test
    fun diger_diller_turkce_kalir() {
        SobiAnalizDili.language = "id"
        try {
            assertEquals("birler", MistakeAnalyzer.columnName(0))
            assertEquals("10'luk kural", MistakeAnalyzer.ruleName(AbacusRule.TEN))
        } finally {
            SobiAnalizDili.language = "tr"
        }
    }
}
