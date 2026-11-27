package com.example.app.abacus

import com.example.app.homework.HomeworkOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** [SubStepExplainer]: analiz ekranında alt adıma dokununca çıkan anlatım. */
class SubStepExplainerTest {

    private fun move(base: Long, op: HomeworkOp, n: Long) = StepExplainer.explain(base, op, n)!!.moves.single()

    @Test
    fun on_gelir_zinciri_1953_arti_70() {
        // Kullanıcının vakası (01.11.2026): onlara 7, boncuk kuralı; "10 gelir"de yüzler dolu.
        val m = move(1953, HomeworkOp.ADD, 70)
        val ten = m.steps.first { it.role == StepExplainer.Role.TEN }
        assertEquals("“10 gelir” ne demek?", SubStepExplainer.question(ten, m))
        assertEquals(
            "“10 gelir”: Onlar sütununa 10 eklemek, solundaki yüzler sütununa 1 boncuk eklemek demek " +
                "(soldaki sütunun 1 boncuğu, bu sütunun 10'u eder). Ama yüzlerde 9 var, sütun dolu. " +
                "Oraya 1 eklemek de 10'luk kuralla olur: binlere 1 boncuk gelir, yüzlerdeki 9 gider.",
            SubStepExplainer.explain(ten, m),
        )
    }

    @Test
    fun on_gider_cift_zincir_2003_eksi_7() {
        val m = move(2003, HomeworkOp.SUB, 7)
        val ten = m.steps.first { it.role == StepExplainer.Role.TEN }
        val t = SubStepExplainer.explain(ten, m)
        assertTrue(t, t.contains("Ama onlarda boncuk yok. Oradan 1 çıkarmak da 10'luk kuralla olur: yüzlerden 1 boncuk gider, onlara 9 gelir."))
        assertTrue(t, t.contains("Ama yüzlerde boncuk yok. Oradan 1 çıkarmak da 10'luk kuralla olur: binlerden 1 boncuk gider, yüzlere 9 gelir."))
    }

    @Test
    fun kuralsiz_elde_zinciri_yok() {
        // 28 + 7: onlarda 2 var, 1 doğrudan gelir; zincir cümlesi eklenmez.
        val m = move(28, HomeworkOp.ADD, 7)
        val ten = m.steps.first { it.role == StepExplainer.Role.TEN }
        assertTrue(SubStepExplainer.explain(ten, m).endsWith("bu sütunun 10'u eder)."))
        val big = m.steps.first { it.role == StepExplainer.Role.BIG_FRIEND }
        assertEquals(
            "“büyük kardeşi 3 gider”: Büyük kardeşler toplamı 10 eden sayılardır: 7 ile 3. Birler sütununa 3 gider.",
            SubStepExplainer.explain(big, m),
        )
    }

    @Test
    fun kardes_ve_bes() {
        val m = move(3, HomeworkOp.ADD, 4)
        val five = m.steps.first { it.role == StepExplainer.Role.FIVE }
        assertTrue(SubStepExplainer.explain(five, m).contains("5'lik boncuk aşağı, çubuğa iner"))
        val friend = m.steps.first { it.role == StepExplainer.Role.FRIEND }
        assertEquals(
            "“kardeşi 1 gider”: Kardeşler toplamı 5 eden sayılardır: 4 ile 1. Birler sütununa alttan 1 birlik boncuk gider.",
            SubStepExplainer.explain(friend, m),
        )
    }

    @Test
    fun neden_bu_kural() {
        assertEquals(
            "Onlarda 5 var; 7 eklenince 12 olur, bir sütun en fazla 9 tutar. 10'luk kural da doğrudan olmuyor: " +
                "büyük kardeş 3 gitmeli ama alttan yalnızca 0 birlik boncuk var. 3 çıkarmak da 5 çıkarıp 2 eklemekle " +
                "aynı. Bu yüzden 2 boncuk gelir, 5 gider, 10 gelir.",
            SubStepExplainer.whyRule(move(1953, HomeworkOp.ADD, 70)),
        )
        assertTrue(SubStepExplainer.whyRule(move(2003, HomeworkOp.SUB, 7)).endsWith("Bu yüzden 10 gider, 5 gelir, kardeşi 2 gider."))
    }
}
