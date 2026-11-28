// SubStepExplainer'ın İngilizce dalı (SubStepExplainerEn; Android app/.../abacus/SubStepExplainerEn.kt kopyası).
// Beklenen cümleler Android kodundan elle türetildi; Android'de bu dalın testi yok.
// jvmTest ile doğrulandı (28.11.2026).
package com.sorobit.shared.sobianaliz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubStepExplainerEnTest {

    private fun move(base: Long, op: HomeworkOp, n: Long) = StepExplainer.explain(base, op, n)!!.moves.single()

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
    fun ten_chain_1953_plus_70() = ingilizce {
        val m = move(1953, HomeworkOp.ADD, 70)
        val ten = m.steps.first { it.role == StepExplainer.Role.TEN }
        assertEquals("What does “add 10” mean?", SubStepExplainer.question(ten, m))
        assertEquals(
            "“add 10”: Adding 10 to the tens column means adding 1 bead to the hundreds column on its left " +
                "(1 bead in the left column is worth 10 in this one). But the hundreds column has 9, it's full. " +
                "Adding 1 there also uses big friends: 1 bead goes into the thousands, 9 comes out of the hundreds.",
            SubStepExplainer.explain(ten, m),
        )
    }

    @Test
    fun why_rule_mixed_friends_sub_2003_minus_7() = ingilizce {
        val t = SubStepExplainer.whyRule(move(2003, HomeworkOp.SUB, 7))
        assertTrue(t.startsWith("The ones column has 3; you can't take 7 out of 3."), t)
        assertTrue(t.endsWith("So: take away 10, add 5, take away its friend 2."), t)
    }

    @Test
    fun diger_diller_turkce_kalir() {
        val m = move(1953, HomeworkOp.ADD, 70)
        val ten = m.steps.first { it.role == StepExplainer.Role.TEN }
        SobiAnalizDili.language = "id"
        try {
            assertEquals("“10 gelir” ne demek?", SubStepExplainer.question(ten, m))
        } finally {
            SobiAnalizDili.language = "tr"
        }
        assertEquals("“10 gelir” ne demek?", SubStepExplainer.question(ten, m))
    }
}
