package com.example.app.homework

import org.junit.Assert.assertEquals
import org.junit.Test

class RuleAnalysisTest {

    private fun add(vararg n: Long) = HomeworkQuestion(HomeworkOp.ADD, n.toList())
    private fun sub(vararg n: Long) = HomeworkQuestion(HomeworkOp.SUB, n.toList())

    /** Kayıt: (ms, değer) çiftleri. */
    private fun log(vararg p: Pair<Long, Long>) = p.map { RuleAnalysis.Sample(it.first, it.second) }

    /** "anahtar:doğru/yanlış" biçiminde, sıralı. */
    private fun summary(a: List<RuleAnalysis.Attempt>) =
        RuleAnalysis.tally(a).entries.sortedBy { it.key }.joinToString(" ") { "${it.key}:${it.value[0]}/${it.value[1]}" }

    @Test
    fun dogruCevaptaButunIslemlerDogru() {
        // 44 + 49: onlarda 4+4 (5'lik), birlerde 4+9 (10'luk)
        val l = log(0L to 0L, 300L to 44L, 1200L to 94L, 1300L to 84L, 1500L to 94L, 1600L to 93L)
        val a = RuleAnalysis.abacus(add(44, 49), l, listOf(0, 1000), answer = 93)
        assertEquals("add_five:1/0 add_ten:1/0", summary(a))
    }

    @Test
    fun besligiDogruOnluguYanlis() {
        // 44 → 94 (+5) → 84 (−1): 5'lik doğru; sonra yalnız 1 çıkarıldı (83), 10'luk yanlış
        val l = log(0L to 0L, 300L to 44L, 1200L to 94L, 1300L to 84L, 1500L to 83L)
        val a = RuleAnalysis.abacus(add(44, 49), l, listOf(0, 1000), answer = 83)
        assertEquals("add_five:1/0 add_ten:0/1", summary(a))
    }

    @Test
    fun besligiYanlisOnluguDogru() {
        // 44 → 94 (5 eklendi, 1 çıkarılmadı: 5'lik yanlış) → 104 → 103 (94 + 9 doğru: 10'luk doğru)
        val l = log(0L to 0L, 300L to 44L, 1200L to 94L, 1400L to 104L, 1500L to 103L)
        val a = RuleAnalysis.abacus(add(44, 49), l, listOf(0, 1000), answer = 103)
        assertEquals("add_five:0/1 add_ten:1/0", summary(a))
    }

    @Test
    fun oncekiHataSonrakileriEtkilemez() {
        // 44 + 21 + 12 + 98 = 175. Öğrenci 64'ten sonra 65 yerine 75 buldu (birlerde 4+1, 5'lik yanlış);
        // sonraki işlemler kendi değeri üstünden doğru: 75 → 85 → 87 → 177 → 185
        val l = log(
            0L to 0L, 300L to 44L,
            1100L to 64L, 1300L to 75L,
            2100L to 85L, 2300L to 87L,
            3100L to 177L, 3300L to 185L,
        )
        val a = RuleAnalysis.abacus(add(44, 21, 12, 98), l, listOf(0, 1000, 2000, 3000), answer = 185)
        // Yanlış yalnızca 64 + 1 (5'lik). 87 + 90: 8 + 9 → 10'luk; 177 + 8: 7 + 8 → 10'luk
        assertEquals(1, a.count { !it.correct })
        assertEquals(RuleAnalysis.Attempt("add_five", false), a.single { !it.correct })
        assertEquals("add_direct:2/0 add_five:1/1 add_ten:2/0", summary(a))
    }

    @Test
    fun sayiGorunmedenYapilanDegisiklikSayilmaz() {
        // 44 + 49'da öğrenci 49'u görmeden 84'e geçmiş gibi bir kayıt: 84, sayı gelmeden önce
        val l = log(0L to 0L, 300L to 84L, 1500L to 83L)
        val a = RuleAnalysis.abacus(add(44, 49), l, listOf(0, 1000), answer = 83)
        // 84 sayı görünmeden önce: onlardaki işlem doğru sayılamaz
        assertEquals(0, a.count { it.key == "add_five" && it.correct })
    }

    @Test
    fun dogrudanSonucGirilirseSayilmaz() {
        // 4 sayı, ara işlem yok: sonuç 300 doğrudan (önce yüzler, sonra birler oynatılmış gibi)
        val q = add(120, 80, 60, 40)
        val dogru = RuleAnalysis.abacus(q, log(0L to 0L, 4500L to 100L, 4600L to 300L), listOf(0, 1000, 2000, 3000), answer = 300)
        assertEquals(0, dogru.size)
        val yanlis = RuleAnalysis.abacus(q, log(0L to 0L, 4500L to 300L, 4600L to 310L), listOf(0, 1000, 2000, 3000), answer = 310)
        assertEquals(0, yanlis.size)
    }

    @Test
    fun geridenGelmekSorunDegil() {
        // 44 + 21 + 12 + 98: öğrenci 2. sayıyı ancak 4. sayı görünürken topluyor; hepsi doğru
        val l = log(0L to 0L, 300L to 44L, 3200L to 64L, 3300L to 65L, 3600L to 75L, 3700L to 77L, 3900L to 167L, 4100L to 175L)
        val a = RuleAnalysis.abacus(add(44, 21, 12, 98), l, listOf(0, 1000, 2000, 3000), answer = 175)
        assertEquals(0, a.count { !it.correct })
        assertEquals(true, a.isNotEmpty())
    }

    @Test
    fun cokYanlisIslemliSoruSayilmaz() {
        // 44 + 23 + 45 + 11 = 123: 23 ile 45'i aklında birleştirip 68 eklemiş, sonra 11 yerine 21 eklemiş
        // (cevap 133). Ara toplamlar 67 ve 112 yok: 23 ve 45'in işlemleri haksız yere yanlış çıkardı.
        val l = log(0L to 0L, 300L to 44L, 2300L to 104L, 2400L to 112L, 3200L to 132L, 3300L to 133L)
        val a = RuleAnalysis.abacus(add(44, 23, 45, 11), l, listOf(0, 1000, 2000, 3000), answer = 133)
        assertEquals(0, a.size)
    }

    @Test
    fun cikarma() {
        // 57 − 25: onlarda 5−2 (5'lik), birlerde 7−5 (doğrudan)
        val a = RuleAnalysis.abacus(sub(57, 25), log(0L to 0L, 300L to 57L, 1200L to 37L, 1400L to 32L), listOf(0, 1000), answer = 32)
        assertEquals("sub_direct:1/0 sub_five:1/0", summary(a))
    }

    @Test
    fun eldeZinciriIkiKurala() {
        // 49 + 1: birlerde 9+1 (10'luk), elde onlarda 4+1 (5'lik)
        assertEquals(setOf(AbacusRule.TEN, AbacusRule.FIVE), RuleAnalysis.rulesOf(HomeworkOp.ADD, 49, 0, 1, 5))
        // 99 + 1: elde onlarda 9+1 yine 10'luk; yüzlerde 0+1 doğrudan, eldenin parçası
        assertEquals(setOf(AbacusRule.TEN), RuleAnalysis.rulesOf(HomeworkOp.ADD, 99, 0, 1, 5))
    }

    @Test
    fun carpmaBasamakCiftine() {
        val cfg = HomeworkConfig(HomeworkOp.MUL, withAbacus = false, digits = 3, digits2 = 2, questionCount = 10)
        assertEquals(listOf(RuleAnalysis.Attempt("mulM_3x2", true)), RuleAnalysis.multiplication(cfg, true))
    }
}
