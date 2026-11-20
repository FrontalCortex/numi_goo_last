package com.sorobit.shared.matematik

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MathOperation, RecordTimeUtils, GlobalValues (sayısal yardımcılar) ve BeadAnimation testleri. */
class KucukParcalarTest {
    private fun tohumla(tohum: Int = 4242, blok: () -> Unit) {
        RastgeleKaynagi.rastgele = Random(tohum)
        try {
            blok()
        } finally {
            RastgeleKaynagi.varsayilanaDon()
        }
    }

    // --- MathOperation ---

    @Test
    fun mathOperationVarsayilanlarVeEsitlik() {
        val bos = MathOperation()
        assertNull(bos.firstNumber)
        assertNull(bos.operator)
        assertNull(bos.secondNumber)
        assertEquals(MathOperation(12, "+", 3), MathOperation(12, "+", 3))
        assertEquals("MathOperation(firstNumber=12, operator=+, secondNumber=3)", MathOperation(12, "+", 3).toString())
        assertEquals(MathOperation(12, "-", 3), MathOperation(12, "+", 3).copy(operator = "-"))
    }

    // --- RecordTimeUtils ---

    @Test
    fun parseRecordToScore() {
        assertEquals(42, RecordTimeUtils.parseRecordToScore("42"))
        assertEquals(42, RecordTimeUtils.parseRecordToScore("  42 \n"))
        assertEquals(-7, RecordTimeUtils.parseRecordToScore("-7"))
        assertEquals(7, RecordTimeUtils.parseRecordToScore("+7"))
        assertEquals(0, RecordTimeUtils.parseRecordToScore("0"))
        assertNull(RecordTimeUtils.parseRecordToScore(""))
        assertNull(RecordTimeUtils.parseRecordToScore("   "))
        assertNull(RecordTimeUtils.parseRecordToScore("12:34"))
        assertNull(RecordTimeUtils.parseRecordToScore("1.5"))
        assertNull(RecordTimeUtils.parseRecordToScore("4 2"))
        assertNull(RecordTimeUtils.parseRecordToScore("abc"))
        assertNull(RecordTimeUtils.parseRecordToScore("99999999999")) // Int sınırını aşar
        assertEquals(Int.MAX_VALUE, RecordTimeUtils.parseRecordToScore("2147483647"))
    }

    @Test
    fun formatScore() {
        assertEquals("0", RecordTimeUtils.formatScore(null))
        assertEquals("0", RecordTimeUtils.formatScore(0))
        assertEquals("125", RecordTimeUtils.formatScore(125))
        assertEquals("-3", RecordTimeUtils.formatScore(-3))
    }

    // --- GlobalValues ---

    @Test
    fun generateRandomNumberBasamakAraligi() = tohumla {
        assertEquals(0, GlobalValues.generateRandomNumber(0))
        assertEquals(0, GlobalValues.generateRandomNumber(-4))
        val araliklar = mapOf(1 to 1..9, 2 to 10..99, 3 to 100..999, 4 to 1000..9999, 9 to 100_000_000..999_999_999)
        for ((basamak, aralik) in araliklar) {
            repeat(2000) {
                val n = GlobalValues.generateRandomNumber(basamak)
                assertTrue(n in aralik, "basamak=$basamak n=$n")
            }
        }
        // 1 basamakta 1..9'un tamamı çıkar; 0 hiç çıkmaz.
        assertEquals((1..9).toSet(), List(500) { GlobalValues.generateRandomNumber(1) }.toSet())
    }

    @Test
    fun randomNumberChangeToString() = tohumla {
        for (basamak in 1..5) repeat(300) {
            val s = GlobalValues.randomNumberChangeToString(basamak)
            assertEquals(basamak, s.length)
            assertTrue(s[0] != '0')
            assertTrue(s.all { it in '0'..'9' })
        }
        // digitCount = 0 → havuz boş (1..0), count = 1 > 0 → boş aralıktan rastgele seçim: Android'de de çöker.
        assertFailsWith<NoSuchElementException> { GlobalValues.randomNumberChangeToString(0) }
    }

    @Test
    fun randomUniqueNumberStrings() = tohumla {
        assertEquals(emptyList(), GlobalValues.randomUniqueNumberStrings(2, 0))
        assertEquals(emptyList(), GlobalValues.randomUniqueNumberStrings(2, -3))
        repeat(300) {
            // Havuz yeterli: hepsi farklı
            val bes = GlobalValues.randomUniqueNumberStrings(1, 5)
            assertEquals(5, bes.size)
            assertEquals(5, bes.toSet().size)
            assertTrue(bes.all { it.toInt() in 1..9 })
            // Havuzun tamamı: 1..9'un bir permütasyonu
            val dokuz = GlobalValues.randomUniqueNumberStrings(1, 9)
            assertEquals((1..9).map { it.toString() }.toSet(), dokuz.toSet())
            assertEquals(9, dokuz.size)
            // Havuzdan fazla: önce tüm değerler bir kez, kalanlar tekrarlı
            val onIki = GlobalValues.randomUniqueNumberStrings(1, 12)
            assertEquals(12, onIki.size)
            assertEquals((1..9).map { it.toString() }.toSet(), onIki.toSet())
            // İki basamak
            val iki = GlobalValues.randomUniqueNumberStrings(2, 20)
            assertEquals(20, iki.toSet().size)
            assertTrue(iki.all { it.toInt() in 10..99 })
        }
        // Sıra gerçekten karışıyor (hep sıralı gelmiyor)
        assertTrue(List(20) { GlobalValues.randomUniqueNumberStrings(1, 9) }.toSet().size > 1)
        assertFailsWith<NoSuchElementException> { GlobalValues.randomUniqueNumberStrings(0, 3) }
        // Negatif basamak: min = 10^0 = 1, max = 10^-1 → 0 - 1 = -1 → havuz boş → aynı çöküş
        assertFailsWith<NoSuchElementException> { GlobalValues.randomUniqueNumberStrings(-2, 1) }
    }

    // --- BeadAnimation ---

    private val mesafe = BeadMoveDistances(bottomPx = 41.5f, topPx = 27.4f)

    @Test
    fun beadAnimationPlanlari() {
        // Tür 1 (animateBeadsUp): seçili görsel, yukarı (-), bitişte marj +mesafe, translation sıfırlanır
        assertEquals(
            BeadAnimationPlan("soroban_bead_selected", -42f, 42, true, 300L),
            BeadAnimation.planFor(1, mesafe),
        )
        // Tür 2 (animateBeadsDown): normal görsel, aşağı (+), bitişte marj -mesafe
        assertEquals(
            BeadAnimationPlan("soroban_bead", 42f, -42, true, 300L),
            BeadAnimation.planFor(2, mesafe),
        )
        // Tür 3 (animateBeadDown): seçili görsel, üst mesafe kadar aşağı, marj değişmez
        assertEquals(
            BeadAnimationPlan("soroban_bead_selected", 27f, 0, false, 300L),
            BeadAnimation.planFor(3, mesafe),
        )
        // Tür 4 (animateBeadUp): normal görsel, özgün konum
        assertEquals(
            BeadAnimationPlan("soroban_bead", 0f, 0, false, 300L),
            BeadAnimation.planFor(4, mesafe),
        )
        assertNull(BeadAnimation.planFor(0, mesafe))
        assertNull(BeadAnimation.planFor(5, mesafe))
        assertNull(BeadAnimation.planFor(-1, mesafe))
    }

    @Test
    fun beadAnimationYuvarlama() {
        // roundToInt: yarımlar yukarı
        assertEquals(-11f, BeadAnimation.planFor(1, BeadMoveDistances(10.5f, 0f))!!.translationY)
        assertEquals(10, BeadAnimation.planFor(1, BeadMoveDistances(10.49f, 0f))!!.bottomMarginDelta)
        assertEquals(8f, BeadAnimation.planFor(3, BeadMoveDistances(0f, 7.5f))!!.translationY)
        // Tür 1 ardından tür 2 marjı başlangıca döndürür
        val yukari = BeadAnimation.planFor(1, mesafe)!!
        val asagi = BeadAnimation.planFor(2, mesafe)!!
        assertEquals(0, yukari.bottomMarginDelta + asagi.bottomMarginDelta)
    }

    @Test
    fun beadAnimationDurumu() {
        val a = BeadAnimation("bead_1_2", 1)
        assertEquals("bead_1_2", a.getBeadId())
        assertEquals(1, a.getAnimationType())
        assertFalse(a.isAnimating())
        val plan = a.animate(mesafe)
        assertEquals(BeadAnimation.planFor(1, mesafe), plan)
        assertTrue(a.isAnimating())
        // Oynarken ikinci çağrı atlanır
        assertNull(a.animate(mesafe))
        assertTrue(a.isAnimating())
        a.onAnimationEnd()
        assertFalse(a.isAnimating())
        assertEquals(plan, a.animate(mesafe))
    }

    @Test
    fun beadAnimationBilinmeyenTurBayragiKaldirir() {
        // Android'deki davranış: `isAnimating = true` atanır, `when` hiçbir dala girmez.
        val a = BeadAnimation("bead_x", 9)
        assertNull(a.animate(mesafe))
        assertTrue(a.isAnimating())
    }
}
