// AbakusBoncukKoprusu: iOS görünüm katmanının kullandığı ince köprünün testleri.
// Yoğunluk 3 (iPhone @3x) alınır: 1 dp = 3 px.
package com.sorobit.shared.abakus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class KopruSahteHost : AbacusBeadHost {
    class Anim(val target: Float, val endAction: () -> Unit)

    val animasyonlar = LinkedHashMap<AbacusBead, Anim>()
    val gorunum = HashMap<AbacusBead, Boolean>()

    override fun animateTranslationY(bead: AbacusBead, targetTranslationY: Float, durationMs: Long, endAction: () -> Unit) {
        animasyonlar[bead] = Anim(targetTranslationY, endAction)
    }

    override fun cancelAnimation(bead: AbacusBead) {
        animasyonlar.remove(bead)
    }

    override fun postDelayed(delayMs: Long, action: () -> Unit) {}

    override fun playBeadClick(activating: Boolean) {}

    override fun updateBeadAppearance(bead: AbacusBead, isSelected: Boolean) {
        gorunum[bead] = isSelected
    }

    fun hepsiniBitir() {
        while (animasyonlar.isNotEmpty()) {
            val bead = animasyonlar.keys.first()
            val a = animasyonlar.remove(bead)!!
            bead.translationY = a.target
            a.endAction()
        }
    }
}

class AbakusBoncukKoprusuTest {
    private val host = KopruSahteHost()
    private val k = AbakusBoncukKoprusu
    private val c = k.denetleyiciOlustur(host, 3f, 300L)

    /** fragment_abacus.xml ölçüleri (dp × 3): üst boncuk 0..120, bariyer 201..249, alt1 üstü 384. */
    private fun olc() = k.mesafeleriOlc(c, 0, 120, 384, 201, 48)

    @Test
    fun baslangicMarjlariYerlesimdekiyleAyni() {
        assertEquals(82 * 3, k.altBoncuk(c, 0, 0).bottomMargin)
        assertEquals(58 * 3, k.altBoncuk(c, 2, 1).bottomMargin)
        assertEquals(34 * 3, k.altBoncuk(c, 4, 2).bottomMargin)
        assertEquals(10 * 3, k.altBoncuk(c, 4, 3).bottomMargin)
        assertSame(c.topBeads[3], k.ustBoncuk(c, 3))
        assertSame(c.bottomBeads[1][2], k.altBoncuk(c, 1, 2))
    }

    @Test
    fun yuvaKodlari() {
        assertEquals(0, k.yuvaKodu(k.ustBoncuk(c, 0)))
        assertEquals(40, k.yuvaKodu(k.ustBoncuk(c, 4)))
        assertEquals(41, k.yuvaKodu(k.altBoncuk(c, 4, 0)))
        assertEquals(24, k.yuvaKodu(k.altBoncuk(c, 2, 3)))
    }

    @Test
    fun mesafelerBariyerAraligindanGelir() {
        assertTrue(olc())
        // Üst: 201 − 120 = 81 px (27 dp); alt: 384 − 249 = 135 px (45 dp).
        assertEquals(81f, c.getTopMoveDistancePx())
        assertEquals(135f, c.getBottomMoveDistancePx())
    }

    @Test
    fun eylemKodlari() {
        assertEquals(AbacusTouchAction.DOWN, k.eylem(0))
        assertEquals(AbacusTouchAction.UP, k.eylem(1))
        assertEquals(AbacusTouchAction.MOVE, k.eylem(2))
        assertEquals(AbacusTouchAction.CANCEL, k.eylem(3))
        assertEquals(AbacusTouchAction.OTHER, k.eylem(9))
    }

    @Test
    fun dokunmaAltVeUstBoncuguDogruDinleyiciyeYollar() {
        olc()
        // Birler çubuğunda 3. alt boncuğa dokun: 3 boncuk yukarı.
        val alt3 = k.altBoncuk(c, 4, 2)
        assertTrue(k.dokunma(c, alt3, k.EYLEM_DOWN, 500f, 0L))
        assertTrue(k.dokunma(c, alt3, k.EYLEM_UP, 500f, 10L))
        assertEquals(3, c.getCurrentValue())
        // Onlar çubuğunda üst boncuk: +50.
        val ust = k.ustBoncuk(c, 3)
        assertTrue(k.dokunma(c, ust, k.EYLEM_DOWN, 100f, 20L))
        assertTrue(k.dokunma(c, ust, k.EYLEM_UP, 100f, 30L))
        assertEquals(53, c.getCurrentValue())
        host.hepsiniBitir()
        assertEquals(82 * 3 + 135, alt3.bottomMargin - (34 - 82) * 3)
        assertEquals(81f, ust.translationY)
    }

    @Test
    fun surukleme24PikseliGecinceBaslarVeYariyiGecinceTutar() {
        olc()
        val alt1 = k.altBoncuk(c, 4, 0)
        k.dokunma(c, alt1, k.EYLEM_DOWN, 500f, 0L)
        // Dokunma payı 8 dp × 3 = 24 px: 20 px'lik hareket sürükleme başlatmaz.
        k.dokunma(c, alt1, k.EYLEM_MOVE, 480f, 10L)
        assertEquals(0f, alt1.translationY)
        k.dokunma(c, alt1, k.EYLEM_MOVE, 420f, 20L)
        assertEquals(-80f, alt1.translationY)
        k.dokunma(c, alt1, k.EYLEM_UP, 420f, 30L)
        host.hepsiniBitir()
        assertEquals(1, c.getCurrentValue())
        assertEquals(82 * 3 + 135, alt1.bottomMargin)
    }

    @Test
    fun degeriYerlestirBoncuklariAnimasyonsuzKoyar() {
        olc()
        k.degeriYerlestir(c, 90473)
        assertEquals(90473, c.getCurrentValue())
        assertTrue(host.animasyonlar.isEmpty())
        // rod0 = 9: üst aşağıda + 4 alt yukarıda.
        assertEquals(81f, k.ustBoncuk(c, 0).translationY)
        assertEquals(10 * 3 + 135, k.altBoncuk(c, 0, 3).bottomMargin)
        // rod1 = 0: hiçbiri.
        assertEquals(0f, k.ustBoncuk(c, 1).translationY)
        assertEquals(82 * 3, k.altBoncuk(c, 1, 0).bottomMargin)
        // Görünüm de güncellenir.
        assertEquals(true, host.gorunum[k.ustBoncuk(c, 0)])
        assertEquals(true, host.gorunum[k.altBoncuk(c, 4, 2)])
        assertEquals(false, host.gorunum[k.altBoncuk(c, 4, 3)])
        // Yerleştirilen değer dokunmayla tutarlı: birler çubuğunda (3) 1. boncuğa dokun → 0.
        val alt1 = k.altBoncuk(c, 4, 0)
        k.dokunma(c, alt1, k.EYLEM_DOWN, 500f, 0L)
        k.dokunma(c, alt1, k.EYLEM_UP, 500f, 10L)
        host.hepsiniBitir()
        assertEquals(90470, c.getCurrentValue())
        assertEquals(82 * 3, alt1.bottomMargin)
        // Yeniden yerleştirme öncekini siler.
        k.degeriYerlestir(c, 5)
        assertEquals(5, c.getCurrentValue())
        assertEquals(0f, k.ustBoncuk(c, 0).translationY)
        assertEquals(10 * 3, k.altBoncuk(c, 0, 3).bottomMargin)
        k.degeriYerlestir(c, -4)
        assertEquals(0, c.getCurrentValue())
        assertFalse(c.isBeadAnimationInProgress())
    }
}
