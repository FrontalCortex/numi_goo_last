// Android CupRuleEngine.kt davranışının testleri. Beklenen değerler Android kaynak kodundan elle türetilmiştir.
package com.sorobit.shared.kupa

import com.sorobit.shared.kupa.CupRuleEngine.DigitConfig
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Belirlenimci rastgele kaynak: tüm bitler 0 → `list.random()` hep ilk elemanı, `(0..1).random()` hep 0 verir.
 * [tekBit] = 1 ise yalnızca tek bitlik çekilişler (yani `(0..1).random()`) 1 döner.
 */
class SabitRandom(private val tekBit: Int = 0) : Random() {
    override fun nextBits(bitCount: Int): Int = if (bitCount == 1) tekBit else 0
}

class CupRuleEngineTest {

    private val blinding = CupRuleEngine.BLINDING_RANGES

    // ---------------------------------------------------------------- standart tablo

    @Test
    fun standartTabloAralikSayisiVeIlkSonAralik() {
        assertEquals(33, CupRuleEngine.RANGES.size)
        assertEquals(
            listOf(DigitConfig(1, listOf(3, 4, 5), listOf(3000L, 2750L, 2500L))),
            CupRuleEngine.resolveConfigs(0),
        )
        assertEquals(CupRuleEngine.resolveConfigs(0), CupRuleEngine.resolveConfigs(99))
        assertEquals(
            listOf(DigitConfig(1, listOf(3, 4, 5), listOf(2900L, 2650L, 2400L))),
            CupRuleEngine.resolveConfigs(100),
        )
        assertEquals(
            listOf(
                DigitConfig(1, listOf(16, 17, 18), listOf(1000L, 750L, 500L)),
                DigitConfig(2, listOf(10, 11, 12), listOf(1500L, 1125L, 750L)),
                DigitConfig(3, listOf(7, 8), listOf(1999L, 1666L, 1333L, 1000L)),
            ),
            CupRuleEngine.resolveConfigs(3199),
        )
    }

    @Test
    fun standartTabloIkiVeUcBasamakGecisleri() {
        assertEquals(1, CupRuleEngine.resolveConfigs(499).size)
        assertEquals(
            listOf(
                DigitConfig(1, listOf(6, 7, 8), listOf(2800L, 2550L, 2300L)),
                DigitConfig(2, listOf(4, 5, 6), listOf(4200L, 3825L, 3450L)),
            ),
            CupRuleEngine.resolveConfigs(500),
        )
        assertEquals(2, CupRuleEngine.resolveConfigs(999).size)
        assertEquals(
            listOf(
                DigitConfig(1, listOf(8, 9, 10), listOf(2500L, 2250L, 2000L)),
                DigitConfig(2, listOf(6, 7, 8), listOf(3750L, 3375L, 3000L)),
                DigitConfig(3, listOf(4, 5), listOf(4999L, 4666L, 4333L, 4000L)),
            ),
            CupRuleEngine.resolveConfigs(1000),
        )
    }

    @Test
    fun cift2400AraligindaIlkTanimKazanir() {
        // Android tablosunda 2400..2499 iki kez tanımlı; `firstOrNull` ilkini seçer (ikincisi ölü kod).
        val c = CupRuleEngine.resolveConfigs(2450)
        assertEquals(listOf(15, 16, 17), c[0].counts)
        assertEquals(listOf(6, 7), c[2].counts)
        assertEquals(2, CupRuleEngine.RANGES.count { it.minCup == 2400 })
    }

    @Test
    fun negatifSkorStandartModdaSon3100AraliginaDuser() {
        assertEquals(CupRuleEngine.resolveConfigs(3150), CupRuleEngine.resolveConfigs(-5))
    }

    @Test
    fun standart3200VeUstuAcikUcluOlcekleme() {
        assertEquals(
            listOf(
                DigitConfig(1, listOf(15, 18, 21), listOf(1000L, 750L, 500L)),
                DigitConfig(2, listOf(9, 11, 13), listOf(1500L, 1125L, 750L)),
                DigitConfig(3, listOf(7, 8), listOf(1999L, 1666L, 1333L, 1000L)),
                DigitConfig(4, listOf(3), listOf(3500L, 3000L, 2500L, 2000L, 1500L)),
            ),
            CupRuleEngine.resolveConfigs(3200),
        )
        assertEquals(CupRuleEngine.resolveConfigs(3200), CupRuleEngine.resolveConfigs(3299))
        // 4000 → step = 8; 1-2 basamak +2, 3 basamak +2, 4 basamak +2
        assertEquals(
            listOf(
                DigitConfig(1, listOf(17, 20, 23), listOf(600L, 350L, 100L)),
                DigitConfig(2, listOf(11, 13, 15), listOf(900L, 525L, 150L)),
                DigitConfig(3, listOf(9, 10), listOf(1199L, 866L, 533L, 200L)),
                DigitConfig(4, listOf(5), listOf(2300L, 1800L, 1300L, 800L, 300L)),
            ),
            CupRuleEngine.resolveConfigs(4000),
        )
        // 10000 → step = 68; tüm süreler alt sınıra dayanır
        assertEquals(
            listOf(
                DigitConfig(1, listOf(32, 35, 38), listOf(80L, 80L, 80L)),
                DigitConfig(2, listOf(26, 28, 30), listOf(100L, 100L, 100L)),
                DigitConfig(3, listOf(29, 30), listOf(100L, 100L, 100L, 100L)),
                DigitConfig(4, listOf(20), listOf(100L, 100L, 100L, 100L, 100L)),
            ),
            CupRuleEngine.resolveConfigs(10000),
        )
    }

    // ---------------------------------------------------------------- körleme

    @Test
    fun korleme0ile999Arasi() {
        assertTrue(blinding.isEmpty())
        assertEquals(
            listOf(DigitConfig(1, listOf(3), listOf(3800L, 3550L, 3300L))),
            CupRuleEngine.resolveConfigs(0, blinding),
        )
        assertEquals(
            listOf(DigitConfig(1, listOf(5), listOf(3300L, 3050L, 2800L))),
            CupRuleEngine.resolveConfigs(500, blinding),
        )
        assertEquals(
            listOf(DigitConfig(1, listOf(6), listOf(2900L, 2650L, 2400L))),
            CupRuleEngine.resolveConfigs(999, blinding),
        )
    }

    @Test
    fun korleme1000ile1999Arasi() {
        assertEquals(
            listOf(
                DigitConfig(1, listOf(5, 6, 8), listOf(2900L, 2650L, 2400L)),
                DigitConfig(2, listOf(2, 3), listOf(5800L, 5300L, 4800L)),
            ),
            CupRuleEngine.resolveConfigs(1000, blinding),
        )
        assertEquals(
            listOf(
                DigitConfig(1, listOf(10, 11, 13), listOf(2000L, 1750L, 1500L)),
                DigitConfig(2, listOf(5, 6), listOf(4000L, 3500L, 3000L)),
            ),
            CupRuleEngine.resolveConfigs(1999, blinding),
        )
    }

    @Test
    fun korleme2000ile2999Arasi() {
        assertEquals(
            listOf(
                DigitConfig(1, listOf(9, 11, 13), listOf(1700L, 1450L, 1200L)),
                DigitConfig(2, listOf(5, 6, 7), listOf(3800L, 3300L, 2800L)),
                DigitConfig(3, listOf(2), listOf(8500L, 7875L, 7250L, 6625L, 6000L)),
            ),
            CupRuleEngine.resolveConfigs(2000, blinding),
        )
        assertEquals(
            listOf(
                DigitConfig(1, listOf(14, 16, 18), listOf(800L, 550L, 300L)),
                DigitConfig(2, listOf(10, 11, 12), listOf(2000L, 1500L, 1000L)),
                DigitConfig(3, listOf(4), listOf(4900L, 4275L, 3650L, 3025L, 2400L)),
            ),
            CupRuleEngine.resolveConfigs(2999, blinding),
        )
    }

    @Test
    fun korleme3000VeUstu() {
        assertEquals(
            listOf(
                DigitConfig(1, listOf(14, 18, 20), listOf(800L, 550L, 300L)),
                DigitConfig(2, listOf(8, 10, 12), listOf(1800L, 1300L, 800L)),
                DigitConfig(3, listOf(5, 6), listOf(3875L, 3250L, 2625L, 2000L)),
                DigitConfig(4, listOf(3), listOf(6000L, 5500L, 5000L, 4500L, 4000L)),
            ),
            CupRuleEngine.resolveConfigs(3000, blinding),
        )
        // 5000 → step = 20
        assertEquals(
            listOf(
                DigitConfig(1, listOf(19, 23, 25), listOf(300L, 80L, 80L)),
                DigitConfig(2, listOf(13, 15, 17), listOf(800L, 300L, 200L)),
                DigitConfig(3, listOf(7, 8), listOf(1875L, 1250L, 625L, 400L)),
                DigitConfig(4, listOf(7), listOf(3000L, 2500L, 2000L, 1500L, 1000L)),
            ),
            CupRuleEngine.resolveConfigs(5000, blinding),
        )
        // Körleme 3200+ için de körleme dalı geçerlidir (standart 3200+ dalı değil): 3500 → step 5, +1
        assertEquals(listOf(15, 19, 21), CupRuleEngine.resolveConfigs(3500, blinding)[0].counts)
    }

    @Test
    fun korlemeNegatifSkorAndroiddekiGibiIstisnaAtar() {
        // Android: boş BLINDING_RANGES üzerinde `first { minCup == 3100 }` → NoSuchElementException.
        assertFailsWith<NoSuchElementException> { CupRuleEngine.resolveConfigs(-1, blinding) }
    }

    @Test
    fun getAvailableDigits() {
        assertEquals(listOf(1), CupRuleEngine.getAvailableDigits(0, false))
        assertEquals(listOf(1, 2), CupRuleEngine.getAvailableDigits(599, false))
        assertEquals(listOf(1, 2, 3), CupRuleEngine.getAvailableDigits(1000, false))
        assertEquals(listOf(1, 2, 3, 4), CupRuleEngine.getAvailableDigits(3200, false))
        assertEquals(listOf(1), CupRuleEngine.getAvailableDigits(500, true))
        assertEquals(listOf(1, 2), CupRuleEngine.getAvailableDigits(1500, true))
        assertEquals(listOf(1, 2, 3), CupRuleEngine.getAvailableDigits(2500, true))
        assertEquals(listOf(1, 2, 3, 4), CupRuleEngine.getAvailableDigits(3000, true))
    }

    // ---------------------------------------------------------------- resolve / selectValues

    @Test
    fun resolveZorunluBasamak() {
        val r = SabitRandom()
        assertEquals(3, CupRuleEngine.resolve(1000, forcedDigitSize = 3, random = r).digits)
        assertEquals(2, CupRuleEngine.resolve(1000, forcedDigitSize = 2, random = r).digits)
        // Geçersiz zorunlu basamak → tüm konfigürasyonlardan rastgele (sabit kaynakla ilki)
        assertEquals(1, CupRuleEngine.resolve(1000, forcedDigitSize = 4, random = r).digits)
        assertEquals(1, CupRuleEngine.resolve(1000, random = r).digits)
        // Gerçek rastgele kaynakla sonuç her zaman aralığın konfigürasyonlarından biridir
        val rnd = Random(42)
        repeat(50) {
            assertTrue(CupRuleEngine.resolve(1000, random = rnd) in CupRuleEngine.resolveConfigs(1000))
        }
    }

    @Test
    fun selectValuesSeviye0Ve4() {
        val c = DigitConfig(3, listOf(4, 5), listOf(4999L, 4666L, 4333L, 4000L))
        assertEquals(4 to 4999L, CupRuleEngine.selectValues(c, 0))
        assertEquals(4 to 4999L, CupRuleEngine.selectValues(c, -3))
        assertEquals(5 to 4000L, CupRuleEngine.selectValues(c, 4))
        assertEquals(5 to 4000L, CupRuleEngine.selectValues(c, 9))
    }

    @Test
    fun selectValuesAraSeviyelerSabitKaynakla() {
        val c = DigitConfig(1, listOf(3, 4, 5), listOf(3000L, 2750L, 2500L))
        // Hep "count" seçilir
        assertEquals(4 to 3000L, CupRuleEngine.selectValues(c, 1, SabitRandom(0)))
        assertEquals(5 to 3000L, CupRuleEngine.selectValues(c, 2, SabitRandom(0)))
        // count sonda → 3. adım mecburen interval
        assertEquals(5 to 2750L, CupRuleEngine.selectValues(c, 3, SabitRandom(0)))
        // Hep "interval" seçilir
        assertEquals(3 to 2750L, CupRuleEngine.selectValues(c, 1, SabitRandom(1)))
        assertEquals(3 to 2500L, CupRuleEngine.selectValues(c, 2, SabitRandom(1)))
        // interval sonda → 3. adım mecburen count
        assertEquals(4 to 2500L, CupRuleEngine.selectValues(c, 3, SabitRandom(1)))

        val c3 = DigitConfig(3, listOf(4, 5), listOf(4999L, 4666L, 4333L, 4000L))
        assertEquals(5 to 4333L, CupRuleEngine.selectValues(c3, 3, SabitRandom(0)))
        assertEquals(4 to 4000L, CupRuleEngine.selectValues(c3, 3, SabitRandom(1)))

        // Tek değerli listeler: iki liste de sonda → hiçbir şey artmaz
        val tek = DigitConfig(1, listOf(3), listOf(2000L))
        for (lvl in 0..4) assertEquals(3 to 2000L, CupRuleEngine.selectValues(tek, lvl, SabitRandom(1)))
    }

    @Test
    fun selectValuesAraSeviyelerdeToplamAdimSeviyeKadardir() {
        val c = DigitConfig(1, listOf(3, 4, 5), listOf(3000L, 2750L, 2500L))
        val rnd = Random(7)
        for (lvl in 1..3) {
            repeat(100) {
                val (count, interval) = CupRuleEngine.selectValues(c, lvl, rnd)
                assertEquals(lvl, c.counts.indexOf(count) + c.intervals.indexOf(interval))
            }
        }
    }

    // ---------------------------------------------------------------- buildLessonItem

    @Test
    fun buildLessonItemAlanlari() {
        val item = CupRuleEngine.buildLessonItem(1000, difficultyLevel = 2, forcedDigitSize = 3, random = SabitRandom())
        assertEquals(4666L, item.timePeriod)
        assertEquals(3, item.cupDigitSize)
        assertEquals(5, item.cupNumberCount)
        assertEquals(2, item.cupDifficultyLevel)
        assertEquals(24, item.cupWinDelta)
        assertEquals(20, item.cupLossDelta)
        assertNull(item.isBlinding)
        assertNull(item.isExtraction)

        val b = CupRuleEngine.buildLessonItem(500, isBlinding = true, isExtraction = true, random = SabitRandom())
        assertEquals(true, b.isBlinding)
        assertEquals(true, b.isExtraction)
        assertEquals(1, b.cupDigitSize)
        assertEquals(5, b.cupNumberCount)
        assertEquals(3300L, b.timePeriod)
    }

    @Test
    fun buildLessonItemKazancKayipTablosu() {
        val beklenen = listOf(10 to 30, 17 to 25, 24 to 20, 31 to 15, 38 to 10)
        for (lvl in 0..4) {
            val item = CupRuleEngine.buildLessonItem(0, difficultyLevel = lvl, random = SabitRandom())
            assertEquals(beklenen[lvl], item.cupWinDelta to item.cupLossDelta)
            assertEquals(lvl, item.cupDifficultyLevel)
        }
        val ust = CupRuleEngine.buildLessonItem(0, difficultyLevel = 7, random = SabitRandom())
        assertEquals(Triple(4, 38, 10), Triple(ust.cupDifficultyLevel, ust.cupWinDelta, ust.cupLossDelta))
        assertEquals(5 to 2500L, ust.cupNumberCount to ust.timePeriod)
        val alt = CupRuleEngine.buildLessonItem(0, difficultyLevel = -1, random = SabitRandom())
        assertEquals(Triple(0, 10, 30), Triple(alt.cupDifficultyLevel, alt.cupWinDelta, alt.cupLossDelta))
    }

    // ---------------------------------------------------------------- çarpma

    @Test
    fun carpmaSorusuSabitKaynakla() {
        val r = SabitRandom()
        assertEquals(CupMathOperation(2, "x", 2), CupRuleEngine.generateMultiplicationQuestion(0, random = r))
        // 600..699: ilk sayı 2 basamak [0,6,7,8,9] → baştaki 0 sıfır olmayanlardan yeniden çekilir → "60"
        assertEquals(CupMathOperation(60, "x", 6), CupRuleEngine.generateMultiplicationQuestion(650, random = r))
        assertEquals(CupMathOperation(100, "x", 10), CupRuleEngine.generateMultiplicationQuestion(1900, random = r))
        // Aralık dışı (çok büyük ya da negatif) → listenin SON elemanı (0..99 kuralı)
        assertEquals(CupMathOperation(2, "x", 2), CupRuleEngine.generateMultiplicationQuestion(2_000_000, random = r))
        assertEquals(CupMathOperation(2, "x", 2), CupRuleEngine.generateMultiplicationQuestion(-10, random = r))
    }

    @Test
    fun korlemeCarpmaSorusuSabitKaynakla() {
        val r = SabitRandom()
        // 3000..999999 ilk sırada olduğundan 3000..3099 satırı hiç seçilmez (Android'deki gölgeleme).
        assertEquals(
            CupMathOperation(1000, "x", 100),
            CupRuleEngine.generateMultiplicationQuestion(3050, isBlinding = true, random = r),
        )
        assertEquals(
            CupMathOperation(600, "x", 600),
            CupRuleEngine.generateMultiplicationQuestion(2450, isBlinding = true, random = r),
        )
        assertEquals(
            CupMathOperation(2, "x", 2),
            CupRuleEngine.generateMultiplicationQuestion(50, isBlinding = true, random = r),
        )
        assertEquals(32, CupRuleEngine.BLINDING_MULTIPLICATION_RANGES.size)
        assertEquals(20, CupRuleEngine.MULTIPLICATION_RANGES.size)
    }

    @Test
    fun carpmaSorusuRakamKurallari() {
        val rnd = Random(123)
        repeat(300) {
            // 1200..1299: ilk sayı 2 basamak [0,6,7,8,9], ikinci sayı 2 basamak [6,7,8,9]
            val q = CupRuleEngine.generateMultiplicationQuestion(1250, random = rnd)
            assertEquals("x", q.operator)
            val a = q.firstNumber!!.toString()
            val b = q.secondNumber!!.toString()
            assertEquals(2, a.length)
            assertEquals(2, b.length)
            assertTrue(a[0] in "6789")
            assertTrue(a[1] in "06789")
            assertTrue(b.all { it in "6789" })
        }
        repeat(100) {
            // 0..99: tek basamak [2,3,4,5]
            val q = CupRuleEngine.generateMultiplicationQuestion(42, random = rnd)
            assertTrue(q.firstNumber!! in 2..5)
            assertTrue(q.secondNumber!! in 2..5)
        }
    }
}
