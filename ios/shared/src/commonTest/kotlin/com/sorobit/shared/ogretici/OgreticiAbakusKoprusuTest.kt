// OgreticiAbakusKoprusu: öğretici boncuk animasyonu (Android BeadAnimation) ve sayı yazma
// (TutorialFragment.writeAnswerNumber). Yoğunluk 3: 1 dp = 3 px; ölçüler AbakusBoncukKoprusuTest ile aynı.
package com.sorobit.shared.ogretici

import com.sorobit.shared.abakus.AbacusBead
import com.sorobit.shared.abakus.AbacusBeadHost
import com.sorobit.shared.abakus.AbakusBoncukKoprusu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class OgreticiSahteHost : AbacusBeadHost {
    class Anim(val target: Float, val endAction: () -> Unit)

    val animasyonlar = LinkedHashMap<AbacusBead, Anim>()
    val gorunum = HashMap<AbacusBead, Boolean>()
    val gecikmeliler = ArrayList<() -> Unit>()

    override fun animateTranslationY(bead: AbacusBead, targetTranslationY: Float, durationMs: Long, endAction: () -> Unit) {
        animasyonlar[bead] = Anim(targetTranslationY, endAction)
    }

    override fun cancelAnimation(bead: AbacusBead) {
        animasyonlar.remove(bead)
    }

    override fun postDelayed(delayMs: Long, action: () -> Unit) {
        gecikmeliler.add(action)
    }

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
        val bekleyen = ArrayList(gecikmeliler)
        gecikmeliler.clear()
        bekleyen.forEach { it() }
    }
}

class OgreticiAbakusKoprusuTest {
    private val host = OgreticiSahteHost()
    private val c = AbakusBoncukKoprusu.denetleyiciOlustur(host, 3f, 300L).also {
        AbakusBoncukKoprusu.mesafeleriOlc(it, 0, 120, 384, 201, 48)
    }
    private val k = OgreticiAbakusKoprusu

    @Test
    fun tekBoncukYukariVeAsagi() {
        var bitti = 0
        val alt1 = c.bottomBeads[4][0]
        val bas = alt1.bottomMargin
        assertTrue(k.oynat(c, host, 4, false, 0, 1) { bitti++ })
        assertEquals(true, host.gorunum[alt1])
        assertEquals(0, c.getCurrentValue())
        host.hepsiniBitir()
        assertEquals(1, bitti)
        assertEquals(bas + 135, alt1.bottomMargin)
        assertEquals(0f, alt1.translationY)
        assertEquals(1, c.getCurrentValue())
        // Üst boncuk aşağı: +5.
        k.oynat(c, host, 4, true, 0, 3) { bitti++ }
        host.hepsiniBitir()
        assertEquals(6, c.getCurrentValue())
        assertEquals(true, host.gorunum[c.topBeads[4]])
        // Geri: üst yukarı, alt aşağı.
        k.oynat(c, host, 4, true, 0, 4) { bitti++ }
        k.oynat(c, host, 4, false, 0, 2) { bitti++ }
        host.hepsiniBitir()
        assertEquals(0, c.getCurrentValue())
        assertEquals(bas, alt1.bottomMargin)
        assertEquals(false, host.gorunum[alt1])
        assertEquals(4, bitti)
    }

    @Test
    fun gecersizHareketHemenBiter() {
        var bitti = false
        assertFalse(k.oynat(c, host, 7, false, 0, 1) { bitti = true })
        assertTrue(bitti)
        bitti = false
        assertFalse(k.oynat(c, host, 4, false, 0, 9) { bitti = true })
        assertTrue(bitti)
        assertTrue(host.animasyonlar.isEmpty())
    }

    @Test
    fun sayiyiYazYalnizcaGerekenBoncuklariOynatir() {
        var bitti = false
        k.sayiyiYaz(c, host, 7) { bitti = true }
        // 7: birlerde üst boncuk + iki alt boncuk.
        assertEquals(3, host.animasyonlar.size)
        assertFalse(bitti)
        host.hepsiniBitir()
        assertTrue(bitti)
        assertEquals(7, c.getCurrentValue())
        // 7 → 12: birlerde üst yukarı (1 hareket, iki alt yerinde), onlarda bir alt yukarı.
        k.sayiyiYaz(c, host, 12) { }
        assertEquals(2, host.animasyonlar.size)
        host.hepsiniBitir()
        assertEquals(12, c.getCurrentValue())
        assertEquals(false, host.gorunum[c.topBeads[4]])
        assertEquals(true, host.gorunum[c.bottomBeads[3][0]])
    }
}
