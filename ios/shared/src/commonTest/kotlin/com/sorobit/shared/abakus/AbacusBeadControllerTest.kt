// Android'deki AbacusBeadController / AbacusBeadMetrics davranışının ortak koddaki karşılığını
// doğrular. Beklenen değerler Android kaynağı okunarak elle türetilmiştir
// (touchSlop = 8 px, alt adım = 48 px, üst adım = 32 px, animasyon = 300 ms).
package com.sorobit.shared.abakus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Animasyonları elle bitirilebilen sahte görünüm katmanı. */
private class SahteHost : AbacusBeadHost {
    class Anim(val target: Float, val durationMs: Long, val endAction: () -> Unit)

    val animasyonlar = LinkedHashMap<AbacusBead, Anim>()
    val sesler = mutableListOf<Boolean>()
    val gorunum = HashMap<AbacusBead, Boolean>()
    val gecikmeliler = mutableListOf<Pair<Long, () -> Unit>>()
    var gorunumCagriSayisi = 0

    override fun animateTranslationY(bead: AbacusBead, targetTranslationY: Float, durationMs: Long, endAction: () -> Unit) {
        animasyonlar[bead] = Anim(targetTranslationY, durationMs, endAction)
    }

    override fun cancelAnimation(bead: AbacusBead) {
        animasyonlar.remove(bead)
    }

    override fun postDelayed(delayMs: Long, action: () -> Unit) {
        gecikmeliler.add(delayMs to action)
    }

    override fun playBeadClick(activating: Boolean) {
        sesler.add(activating)
    }

    override fun updateBeadAppearance(bead: AbacusBead, isSelected: Boolean) {
        gorunum[bead] = isSelected
        gorunumCagriSayisi++
    }

    /** Animasyonu doğal sonuna götürür (hedef translation + endAction). */
    fun bitir(bead: AbacusBead) {
        val a = animasyonlar.remove(bead) ?: error("animasyon yok: ${bead.idName}")
        bead.translationY = a.target
        a.endAction()
    }

    fun hepsiniBitir() {
        while (animasyonlar.isNotEmpty()) bitir(animasyonlar.keys.first())
    }
}

class AbacusBeadControllerTest {
    private val host = SahteHost()
    private val c = AbacusBeadController(host, touchSlopPx = 8, bottomStepPx = 48f, topStepPx = 32f).also { it.setup() }

    private fun altDokun(rod: Int, n: Int, y: Float = 500f, t: Long = 0L) {
        assertTrue(c.onBottomBeadTouch(rod, n, AbacusTouchAction.DOWN, y, t))
        assertTrue(c.onBottomBeadTouch(rod, n, AbacusTouchAction.UP, y, t + 10))
    }

    private fun ustDokun(rod: Int, y: Float = 100f, t: Long = 0L) {
        assertTrue(c.onTopBeadTouch(rod, AbacusTouchAction.DOWN, y, t))
        assertTrue(c.onTopBeadTouch(rod, AbacusTouchAction.UP, y, t + 10))
    }

    @Test
    fun baslangicDegeriSifir() {
        assertEquals(0, c.getCurrentValue())
        assertFalse(c.isBeadAnimationInProgress())
        assertFalse(c.isResetInProgress())
        assertEquals("rod3_bead_bottom2", c.bottomBeads[3][1].idName)
        assertEquals("rod0_bead_top", c.topBeads[0].idName)
    }

    @Test
    fun degerdenIcDurum() {
        c.applyInternalStateFromValue(7)
        assertEquals(7, c.getCurrentValue())
        assertEquals(2, c.getBottomCount(4)) // 7 = 5 + 2
        assertTrue(c.isTopDownLogical(4))
        for (rod in 0..3) {
            assertEquals(0, c.getBottomCount(rod))
            assertFalse(c.isTopDownLogical(rod))
        }

        c.applyInternalStateFromValue(50505)
        assertEquals(50505, c.getCurrentValue())
        assertTrue(c.isTopDownLogical(0)); assertEquals(0, c.getBottomCount(0))
        assertFalse(c.isTopDownLogical(1))
        assertTrue(c.isTopDownLogical(2))
        assertTrue(c.isTopDownLogical(4))

        c.applyInternalStateFromValue(12349)
        assertEquals(12349, c.getCurrentValue())
        assertEquals(1, c.getBottomCount(0))
        assertEquals(2, c.getBottomCount(1))
        assertEquals(3, c.getBottomCount(2))
        assertEquals(4, c.getBottomCount(3))
        assertEquals(4, c.getBottomCount(4)); assertTrue(c.isTopDownLogical(4))
    }

    @Test
    fun degerSinirlaraKirpilir() {
        c.applyInternalStateFromValue(123456)
        assertEquals(99999, c.getCurrentValue())
        c.applyInternalStateFromValue(-5)
        assertEquals(0, c.getCurrentValue())
    }

    @Test
    fun altBoncugaDokunmaYukariVeAsagi() {
        // 3. boncuğa dokun: 1..3 birlikte yukarı çıkar
        altDokun(4, 3)
        assertEquals(3, c.getCurrentValue())
        assertEquals(listOf(true), host.sesler)
        assertEquals(3, host.animasyonlar.size)
        for (i in 0..2) {
            val a = host.animasyonlar[c.bottomBeads[4][i]]!!
            assertEquals(-48f, a.target)
            assertEquals(300L, a.durationMs)
            assertEquals(true, host.gorunum[c.bottomBeads[4][i]])
        }
        assertEquals(false, host.gorunum[c.bottomBeads[4][3]])
        assertTrue(c.isBeadAnimationInProgress())

        host.hepsiniBitir()
        assertFalse(c.isBeadAnimationInProgress())
        for (i in 0..2) {
            assertEquals(48, c.bottomBeads[4][i].bottomMargin)
            assertEquals(0f, c.bottomBeads[4][i].translationY)
            assertTrue(c.isBottomBeadPhysicallyRaised(4, i))
        }
        assertFalse(c.isBottomBeadPhysicallyRaised(4, 3))
        assertEquals(3, c.countConsecutiveRaisedBottomBeads(4))

        // Yukarıdaki 2. boncuğa dokun: 2 ve 3 iner, 1 kalır
        altDokun(4, 2)
        assertEquals(1, c.getCurrentValue())
        assertEquals(listOf(true, false), host.sesler)
        assertEquals(setOf(c.bottomBeads[4][1], c.bottomBeads[4][2]), host.animasyonlar.keys.toSet())
        assertEquals(48f, host.animasyonlar[c.bottomBeads[4][1]]!!.target)
        host.hepsiniBitir()
        assertEquals(48, c.bottomBeads[4][0].bottomMargin)
        assertEquals(0, c.bottomBeads[4][1].bottomMargin)
        assertEquals(0, c.bottomBeads[4][2].bottomMargin)

        // 1. boncuğa dokun: iner → 0
        altDokun(4, 1)
        host.hepsiniBitir()
        assertEquals(0, c.getCurrentValue())

        // 4. boncuğa dokun: dördü de çıkar
        altDokun(4, 4)
        host.hepsiniBitir()
        assertEquals(4, c.getCurrentValue())
    }

    @Test
    fun cubukBasamakDegerleri() {
        altDokun(0, 1); altDokun(1, 2); altDokun(2, 3); altDokun(3, 4); altDokun(4, 1)
        assertEquals(12341, c.getCurrentValue())
    }

    @Test
    fun ustBoncukDokunma() {
        ustDokun(4)
        assertEquals(5, c.getCurrentValue())
        assertEquals(listOf(true), host.sesler)
        val a = host.animasyonlar[c.topBeads[4]]!!
        assertEquals(32f, a.target)
        assertEquals(300L, a.durationMs)
        assertEquals(true, host.gorunum[c.topBeads[4]])
        host.hepsiniBitir()
        assertTrue(c.isTopBeadDown(4))

        ustDokun(4)
        assertEquals(0, c.getCurrentValue())
        assertEquals(listOf(true, false), host.sesler)
        assertEquals(0f, host.animasyonlar[c.topBeads[4]]!!.target)
        host.hepsiniBitir()
        assertFalse(c.isTopBeadDown(4))
    }

    @Test
    fun ustBoncukTiklamaHedefiYuvarlanir() {
        // Dinamik ölçüm 31.6 px: tıklama hedefi roundToInt → 32, sürükleme hedefi 31.6
        c.computeMovementDistancesFromLayout(AbacusBeadMetrics.MoveDistancesPx(bottomPx = 47.4f, topPx = 31.6f))
        ustDokun(0)
        assertEquals(32f, host.animasyonlar[c.topBeads[0]]!!.target)
        altDokun(0, 1)
        assertEquals(-47f, host.animasyonlar[c.bottomBeads[0][0]]!!.target)
    }

    @Test
    fun altSuruklemeEsikAltindaGeriDoner() {
        assertTrue(c.onBottomBeadTouch(3, 2, AbacusTouchAction.DOWN, 500f, 0L))
        // 5 px: dokunma eşiğinin (8) altında → sürükleme başlamaz
        assertTrue(c.onBottomBeadTouch(3, 2, AbacusTouchAction.MOVE, 495f, 10L))
        assertEquals(0, c.getCurrentValue())
        assertEquals(0f, c.bottomBeads[3][0].translationY)
        // 20 px yukarı: sürükleme başlar, durum geçici olarak 2 olur
        assertTrue(c.onBottomBeadTouch(3, 2, AbacusTouchAction.MOVE, 480f, 20L))
        assertEquals(20, c.getCurrentValue())
        assertEquals(-20f, c.bottomBeads[3][0].translationY)
        assertEquals(-20f, c.bottomBeads[3][1].translationY)
        assertEquals(0f, c.bottomBeads[3][2].translationY)
        assertTrue(c.isBeadAnimationInProgress())
        // Bırak: 20 < 24 (yarım adım) → geri döner, ses yok
        assertTrue(c.onBottomBeadTouch(3, 2, AbacusTouchAction.UP, 480f, 30L))
        assertEquals(0, c.getCurrentValue())
        assertTrue(host.sesler.isEmpty())
        val a = host.animasyonlar[c.bottomBeads[3][0]]!!
        assertEquals(0f, a.target)
        // hız = 15 px / 10 ms = 1.5; beklenen = 48/300 = 0.16; çarpan 0.35'e kırpılır → 105 ms
        assertEquals(105L, a.durationMs)
        host.hepsiniBitir()
        assertFalse(c.isBeadAnimationInProgress())
        assertEquals(0, c.bottomBeads[3][0].bottomMargin)
        assertEquals(false, host.gorunum[c.bottomBeads[3][0]])
    }

    @Test
    fun altSuruklemeYarimAdimiGecinceIslenir() {
        c.onBottomBeadTouch(3, 2, AbacusTouchAction.DOWN, 500f, 0L)
        c.onBottomBeadTouch(3, 2, AbacusTouchAction.MOVE, 476f, 100L) // tam 24 px = yarım adım
        assertEquals(-24f, c.bottomBeads[3][1].translationY)
        c.onBottomBeadTouch(3, 2, AbacusTouchAction.UP, 476f, 110L)
        assertEquals(20, c.getCurrentValue())
        assertEquals(listOf(true), host.sesler)
        val a = host.animasyonlar[c.bottomBeads[3][1]]!!
        assertEquals(-48f, a.target)
        // hız = 24/100 = 0.24; çarpan = 0.16/0.24 = 0.667 → 200 ms
        assertEquals(200L, a.durationMs)
        host.hepsiniBitir()
        assertEquals(48, c.bottomBeads[3][0].bottomMargin)
        assertEquals(48, c.bottomBeads[3][1].bottomMargin)
        assertEquals(0f, c.bottomBeads[3][1].translationY)
    }

    @Test
    fun altSuruklemeIlerlemeAdimaKirpilir() {
        c.onBottomBeadTouch(2, 1, AbacusTouchAction.DOWN, 500f, 0L)
        c.onBottomBeadTouch(2, 1, AbacusTouchAction.MOVE, 300f, 50L) // 200 px yukarı
        assertEquals(-48f, c.bottomBeads[2][0].translationY)
    }

    @Test
    fun asagiSuruklemeIleIndirme() {
        altDokun(4, 2)
        host.hepsiniBitir()
        host.sesler.clear()
        // Yukarıdaki 1. boncuğu aşağı sürükle: 1 ve 2 iner
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 0L)
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.MOVE, 530f, 100L)
        assertEquals(0, c.getCurrentValue())
        assertEquals(30f, c.bottomBeads[4][0].translationY)
        assertEquals(30f, c.bottomBeads[4][1].translationY)
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 530f, 110L)
        assertEquals(listOf(false), host.sesler)
        assertEquals(48f, host.animasyonlar[c.bottomBeads[4][0]]!!.target)
        host.hepsiniBitir()
        assertEquals(0, c.bottomBeads[4][0].bottomMargin)
        assertEquals(0, c.bottomBeads[4][1].bottomMargin)
    }

    @Test
    fun yanlisYoneSuruklemeBoncuguCevirmez() {
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 0L)
        // Aşağıdaki boncuğu aşağı çekmek: ilerleme 0 → sürükleme başlamaz
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.MOVE, 530f, 10L)
        assertEquals(0f, c.bottomBeads[4][0].translationY)
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 530f, 20L)
        assertEquals(0, c.getCurrentValue())
        assertTrue(host.sesler.isEmpty())
        assertTrue(host.animasyonlar.isEmpty())

        // Üst boncuk için de aynı (yukarıdaki üst boncuğu yukarı çekmek)
        c.onTopBeadTouch(4, AbacusTouchAction.DOWN, 100f, 0L)
        c.onTopBeadTouch(4, AbacusTouchAction.MOVE, 70f, 10L)
        c.onTopBeadTouch(4, AbacusTouchAction.UP, 70f, 20L)
        assertEquals(0, c.getCurrentValue())
        assertTrue(host.sesler.isEmpty())
    }

    @Test
    fun esikIcindeKipirtiDokunmaSayilir() {
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 0L)
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.MOVE, 494f, 10L)
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 508f, 20L) // |dy| = 8 ≤ eşik
        assertEquals(1, c.getCurrentValue())
    }

    @Test
    fun ustSurukleme() {
        c.onTopBeadTouch(0, AbacusTouchAction.DOWN, 100f, 0L)
        c.onTopBeadTouch(0, AbacusTouchAction.MOVE, 120f, 100L)
        assertEquals(50000, c.getCurrentValue())
        assertEquals(20f, c.topBeads[0].translationY)
        c.onTopBeadTouch(0, AbacusTouchAction.UP, 120f, 110L) // 20 ≥ 16 → işlenir
        assertEquals(listOf(true), host.sesler)
        assertEquals(32f, host.animasyonlar[c.topBeads[0]]!!.target)
        host.hepsiniBitir()
        assertEquals(32f, c.topBeads[0].translationY)

        // Yukarı doğru yetersiz sürükleme: geri aşağı döner
        c.onTopBeadTouch(0, AbacusTouchAction.DOWN, 200f, 0L)
        c.onTopBeadTouch(0, AbacusTouchAction.MOVE, 190f, 100L)
        assertEquals(0, c.getCurrentValue()) // geçici durum
        assertEquals(22f, c.topBeads[0].translationY) // 32 − 10
        c.onTopBeadTouch(0, AbacusTouchAction.UP, 190f, 110L)
        assertEquals(50000, c.getCurrentValue())
        assertEquals(listOf(true), host.sesler)
        assertEquals(32f, host.animasyonlar[c.topBeads[0]]!!.target)
        host.hepsiniBitir()
        assertTrue(c.isTopBeadDown(0))
    }

    @Test
    fun dokunmaKapaliykenHicbirSeyOlmaz() {
        c.setEnabled(false)
        assertTrue(c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 0L))
        assertTrue(c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 500f, 10L))
        assertTrue(c.onTopBeadTouch(4, AbacusTouchAction.DOWN, 100f, 0L))
        assertTrue(c.onTopBeadTouch(4, AbacusTouchAction.UP, 100f, 10L))
        assertEquals(0, c.getCurrentValue())
        c.setEnabled(true)
        altDokun(4, 1)
        assertEquals(1, c.getCurrentValue())
    }

    @Test
    fun oturumYokkenMoveVeUpFalseDoner() {
        assertFalse(c.onBottomBeadTouch(4, 1, AbacusTouchAction.MOVE, 500f, 0L))
        assertFalse(c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 500f, 0L))
        assertFalse(c.onTopBeadTouch(4, AbacusTouchAction.MOVE, 500f, 0L))
        assertFalse(c.onTopBeadTouch(4, AbacusTouchAction.CANCEL, 500f, 0L))
        assertTrue(c.onBottomBeadTouch(4, 1, AbacusTouchAction.OTHER, 500f, 0L))
        // Alt oturum açıkken üst dinleyici MOVE'u tüketmez
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 0L)
        assertFalse(c.onTopBeadTouch(4, AbacusTouchAction.MOVE, 500f, 0L))
    }

    @Test
    fun pointerUpOturumuTemizler() {
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 0L)
        assertTrue(c.onBottomBeadTouch(4, 1, AbacusTouchAction.POINTER_UP, 500f, 5L))
        assertFalse(c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 500f, 10L))
        assertEquals(0, c.getCurrentValue())

        c.onTopBeadTouch(4, AbacusTouchAction.DOWN, 100f, 0L)
        assertTrue(c.onTopBeadTouch(4, AbacusTouchAction.OUTSIDE, 100f, 5L))
        assertFalse(c.onTopBeadTouch(4, AbacusTouchAction.UP, 100f, 10L))
        assertEquals(0, c.getCurrentValue())
    }

    @Test
    fun iptalDeBirakmaGibiIslenir() {
        c.onBottomBeadTouch(4, 2, AbacusTouchAction.DOWN, 500f, 0L)
        assertTrue(c.onBottomBeadTouch(4, 2, AbacusTouchAction.CANCEL, 500f, 10L))
        assertEquals(2, c.getCurrentValue())
    }

    @Test
    fun sureHesabi() {
        assertEquals(300L, c.computeDurationMs(0f, 48f))
        assertEquals(300L, c.computeDurationMs(-1f, 48f))
        assertEquals(300L, c.computeDurationMs(0.16f, 48f)) // beklenen hız → çarpan 1
        assertEquals(480L, c.computeDurationMs(0.01f, 48f)) // çok yavaş → 1.6
        assertEquals(105L, c.computeDurationMs(5f, 48f)) // çok hızlı → 0.35
        assertEquals(150L, c.computeDurationMs(0.32f, 48f)) // iki kat hız → 0.5

        // 50 ms'lik (sandık) denetleyicide alt sınır 60 ms
        val hizli = AbacusBeadController(SahteHost(), 8, 48f, 32f, animationDurationMs = 50L)
        assertEquals(50L, hizli.computeDurationMs(0f, 48f))
        assertEquals(60L, hizli.computeDurationMs(100f, 48f))
        assertEquals(80L, hizli.computeDurationMs(0.0001f, 48f)) // 50 × 1.6
    }

    @Test
    fun animasyonsuzSifirlama() {
        altDokun(4, 3); ustDokun(2)
        host.hepsiniBitir()
        assertEquals(503, c.getCurrentValue())
        c.reset(animate = false)
        assertEquals(0, c.getCurrentValue())
        assertFalse(c.isResetInProgress())
        assertTrue(host.gecikmeliler.isEmpty())
        for (i in 0..3) {
            assertEquals(0, c.bottomBeads[4][i].bottomMargin)
            assertEquals(0f, c.bottomBeads[4][i].translationY)
        }
        assertEquals(0f, c.topBeads[2].translationY)
        assertEquals(false, host.gorunum[c.bottomBeads[4][0]])
        assertEquals(false, host.gorunum[c.topBeads[2]])
    }

    @Test
    fun animasyonluSifirlama() {
        altDokun(4, 3); ustDokun(2)
        host.hepsiniBitir()
        host.sesler.clear()

        c.reset() // animate = true
        assertEquals(0, c.getCurrentValue()) // mantıksal durum hemen sıfırlanır
        assertTrue(c.isResetInProgress())
        assertTrue(host.sesler.isEmpty()) // sıfırlamada ses yok
        assertEquals(
            setOf(c.bottomBeads[4][0], c.bottomBeads[4][1], c.bottomBeads[4][2], c.topBeads[2]),
            host.animasyonlar.keys.toSet(),
        )
        assertEquals(48f, host.animasyonlar[c.bottomBeads[4][0]]!!.target)
        assertEquals(0f, host.animasyonlar[c.topBeads[2]]!!.target)

        // Sıfırlama sürerken dışarıdan durum yazma / eşitleme yok sayılır
        c.applyInternalStateFromValue(42)
        assertEquals(0, c.getCurrentValue())
        c.syncStateFromUi()
        assertEquals(0, c.getCurrentValue())

        assertEquals(1, host.gecikmeliler.size)
        assertEquals(350L, host.gecikmeliler[0].first) // 300 + 50
        host.hepsiniBitir()
        host.gecikmeliler[0].second()
        assertFalse(c.isResetInProgress())
        assertEquals(0, c.bottomBeads[4][0].bottomMargin)
        assertEquals(0f, c.topBeads[2].translationY)
    }

    @Test
    fun sifirlamaUstBoncukIcinGorselKonumaBakar() {
        // İç durum "yukarıda" dese de görsel olarak aşağıdaki üst boncuk yukarı animasyonlanır
        c.topBeads[1].translationY = 32f
        assertFalse(c.isTopDownLogical(1))
        c.reset()
        assertEquals(0f, host.animasyonlar[c.topBeads[1]]!!.target)
    }

    @Test
    fun sifirlamaAnimasyonOrtasindakiKonumuMargineIsler() {
        altDokun(4, 1)
        c.bottomBeads[4][0].translationY = -20f // animasyon ortası
        c.reset()
        // translation margin'e işlendi (0 − (−20) = 20), sonra aşağı animasyon başladı
        assertEquals(20, c.bottomBeads[4][0].bottomMargin)
        assertEquals(0f, c.bottomBeads[4][0].translationY)
        assertEquals(48f, host.animasyonlar[c.bottomBeads[4][0]]!!.target)
        host.hepsiniBitir()
        // DİKKAT (Android davranışı): tam adım (48) kadar inilir; margin 20 − 48 = −28 olur
        assertEquals(-28, c.bottomBeads[4][0].bottomMargin)
    }

    @Test
    fun fizikselKonumdanEsitleme() {
        c.bottomBeads[2][0].bottomMargin = 48
        c.bottomBeads[2][1].bottomMargin = 24 // tam yarım adım: yukarıda sayılır
        c.bottomBeads[2][2].bottomMargin = 0
        c.bottomBeads[2][3].bottomMargin = 48 // ardışık değil: sayılmaz
        c.topBeads[2].translationY = 16f // tam yarım adım: aşağıda
        c.topBeads[3].translationY = 15.9f
        assertEquals(2, c.countConsecutiveRaisedBottomBeads(2))
        assertTrue(c.isBottomBeadPhysicallyRaised(2, 3))
        assertTrue(c.isTopBeadDown(2))
        assertFalse(c.isTopBeadDown(3))

        c.syncStateFromUi()
        assertEquals(700, c.getCurrentValue())
        assertEquals(true, host.gorunum[c.bottomBeads[2][1]])
        assertEquals(false, host.gorunum[c.bottomBeads[2][3]])
        assertEquals(true, host.gorunum[c.topBeads[2]])
    }

    @Test
    fun esitlemeTranslationiDeltayaEkler() {
        // margin 48 ama −30 translation (yukarı animasyon ortası): delta = 18 < 24 → yukarıda değil
        c.bottomBeads[1][0].bottomMargin = 48
        c.bottomBeads[1][0].translationY = -30f
        assertFalse(c.isBottomBeadPhysicallyRaised(1, 0))
        assertEquals(0, c.countConsecutiveRaisedBottomBeads(1))
    }

    @Test
    fun esitlemeGorunumuAnimasyonSirasindaErteler() {
        altDokun(4, 1) // animasyon sürüyor
        val once = host.gorunumCagriSayisi
        c.syncStateFromUi()
        assertEquals(once, host.gorunumCagriSayisi)
        // Fiziksel konum henüz 0 olduğu için iç durum 0'a döner
        assertEquals(0, c.getCurrentValue())

        // applyAppearance = false: animasyon olmasa da görünüm güncellenmez
        host.hepsiniBitir()
        val once2 = host.gorunumCagriSayisi
        c.syncStateFromUi(applyAppearance = false)
        assertEquals(once2, host.gorunumCagriSayisi)
        assertEquals(1, c.getCurrentValue())
        c.syncStateFromUi()
        assertEquals(once2 + 25, host.gorunumCagriSayisi)
    }

    @Test
    fun disAnimasyonDenetleyicisi() {
        var dis = true
        c.setExternalBeadAnimationInProgress { dis }
        assertTrue(c.isBeadAnimationInProgress())
        val once = host.gorunumCagriSayisi
        c.syncStateFromUi()
        assertEquals(once, host.gorunumCagriSayisi)
        dis = false
        assertFalse(c.isBeadAnimationInProgress())
        c.syncStateFromUi()
        assertEquals(once + 25, host.gorunumCagriSayisi)
    }

    @Test
    fun degerYazarkenGorunumKurali() {
        c.applyInternalStateFromValue(3)
        assertEquals(0, host.gorunumCagriSayisi) // varsayılan: görünüm güncellenmez
        c.applyInternalStateFromValue(3, applyAppearance = true)
        assertEquals(25, host.gorunumCagriSayisi)
        assertEquals(true, host.gorunum[c.bottomBeads[4][2]])
        assertEquals(false, host.gorunum[c.bottomBeads[4][3]])

        // Tanılama bayrağı açıkken (Android'deki güncel durum) animasyon sırasında atlanır
        altDokun(0, 1)
        val once = host.gorunumCagriSayisi
        c.applyInternalStateFromValue(4, applyAppearance = true)
        assertEquals(once, host.gorunumCagriSayisi)
        // Bayrak kapatılırsa animasyon sırasında da güncellenir
        c.tutorialBeadDiagnosticsEnabled = false
        c.applyInternalStateFromValue(4, applyAppearance = true)
        assertEquals(once + 25, host.gorunumCagriSayisi)
    }

    @Test
    fun animasyonOrtasindaDokunmaYakinKonumaYapistirir() {
        altDokun(4, 2) // 1 ve 2 yukarı animasyonda
        // Animasyonun çoğu tamamlanmış: −30 px
        c.bottomBeads[4][0].translationY = -30f
        c.bottomBeads[4][1].translationY = -30f
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 100L)
        // margin = round(0 − (−30)) = 30 ≥ 24 → ikisi de yukarı yapıştırıldı
        assertTrue(host.animasyonlar.isEmpty())
        assertEquals(48, c.bottomBeads[4][0].bottomMargin)
        assertEquals(48, c.bottomBeads[4][1].bottomMargin)
        assertEquals(0f, c.bottomBeads[4][0].translationY)
        assertEquals(2, c.getCurrentValue())
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 500f, 110L)
        assertEquals(0, c.getCurrentValue()) // 1'e dokunmak ikisini de indirir
        assertEquals(2, host.animasyonlar.size)
    }

    @Test
    fun animasyonBasindaDokunmaAsagiYapistirir() {
        altDokun(4, 2)
        c.bottomBeads[4][0].translationY = -10f
        c.bottomBeads[4][1].translationY = -10f
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.DOWN, 500f, 100L)
        assertEquals(0, c.bottomBeads[4][0].bottomMargin)
        assertEquals(0, c.getCurrentValue())
        c.onBottomBeadTouch(4, 1, AbacusTouchAction.UP, 500f, 110L)
        assertEquals(1, c.getCurrentValue())
    }

    @Test
    fun animasyonOrtasindaUstBoncugaDokunma() {
        ustDokun(4) // aşağı animasyonda (hedef 32)
        c.topBeads[4].translationY = 20f // ≥ 16 → aşağı yapıştırılır
        c.onTopBeadTouch(4, AbacusTouchAction.DOWN, 100f, 100L)
        assertEquals(32f, c.topBeads[4].translationY)
        assertEquals(5, c.getCurrentValue())
        c.onTopBeadTouch(4, AbacusTouchAction.UP, 100f, 110L)
        assertEquals(0, c.getCurrentValue())

        // Yukarı animasyonun sonuna yakınken (4 px) dokunma: yukarı yapıştırılır, sonra aşağı çevrilir
        c.topBeads[4].translationY = 4f
        c.onTopBeadTouch(4, AbacusTouchAction.DOWN, 100f, 200L)
        assertEquals(0f, c.topBeads[4].translationY)
        c.onTopBeadTouch(4, AbacusTouchAction.UP, 100f, 210L)
        assertEquals(5, c.getCurrentValue())
    }

    @Test
    fun rehberBoncukKimlikleri() {
        c.animateGuideBead("rod2BottomBead3")
        assertEquals(300, c.getCurrentValue())
        c.animateGuideBead("rod4TopBead")
        assertEquals(305, c.getCurrentValue())
        host.hepsiniBitir()
        c.animateGuideBead("rod2BottomBead1") // yukarıdaki 1'e dokunma → hepsi iner
        assertEquals(5, c.getCurrentValue())
        host.hepsiniBitir()
        // Geçersiz kimlikler yok sayılır
        for (kotu in listOf("rod9TopBead", "rodXTopBead", "TopBead", "rod2BottomBead7", "rod2BottomBead0", "rod2BottomBeadX", "rod2", "")) {
            c.animateGuideBead(kotu)
        }
        assertEquals(5, c.getCurrentValue())
    }

    @Test
    fun hareketMesafeleri() {
        assertEquals(48f, c.getBottomMoveDistancePx())
        assertEquals(32f, c.getTopMoveDistancePx())

        // force = false ve mesafeler pozitif: ölçüme bakılmadan true
        assertTrue(c.computeMovementDistancesFromLayout(null, force = false))
        assertEquals(48f, c.getBottomMoveDistancePx())

        // Dinamik ölçüm uygulanır; 1'in altı 1'e çekilir
        assertTrue(c.computeMovementDistancesFromLayout(AbacusBeadMetrics.MoveDistancesPx(bottomPx = 0.5f, topPx = 40f)))
        assertEquals(1f, c.getBottomMoveDistancePx())
        assertEquals(40f, c.getTopMoveDistancePx())

        // Ölçüm yok: dimen yedeği − margin offset'leri
        c.setBeadMarginOffsets(bottomOffsetPx = 10f, topOffsetPx = 5f)
        assertFalse(c.computeMovementDistancesFromLayout(null))
        assertEquals(38f, c.getBottomMoveDistancePx())
        assertEquals(27f, c.getTopMoveDistancePx())

        // Negatif offset 0 sayılır; aşırı büyük offset'te mesafe 1'e çekilir
        c.setBeadMarginOffsets(-3f, 100f)
        assertFalse(c.computeMovementDistancesFromLayout(null))
        assertEquals(48f, c.getBottomMoveDistancePx())
        assertEquals(1f, c.getTopMoveDistancePx())
    }

    @Test
    fun baslangicMarginleriYenidenYakalanir() {
        // Ayar panelinde boncuk boşluğu değişince taban margin'ler yeniden yakalanır
        for (rod in 0..4) for (i in 0..3) c.bottomBeads[rod][i].bottomMargin = 100
        assertTrue(c.isBottomBeadPhysicallyRaised(0, 0)) // eski tabana (0) göre yukarıda görünür
        c.forceRecaptureInitialPositions()
        assertFalse(c.isBottomBeadPhysicallyRaised(0, 0))
        altDokun(0, 1)
        host.hepsiniBitir()
        assertEquals(148, c.bottomBeads[0][0].bottomMargin)
        c.reset(animate = false)
        assertEquals(100, c.bottomBeads[0][0].bottomMargin)
    }

    @Test
    fun gecersizCubukSorgulari() {
        assertEquals(0, c.countConsecutiveRaisedBottomBeads(5))
        assertEquals(0, c.countConsecutiveRaisedBottomBeads(-1))
        assertFalse(c.isBottomBeadPhysicallyRaised(5, 0))
        assertFalse(c.isBottomBeadPhysicallyRaised(0, 4))
        assertFalse(c.isTopBeadDown(7))
    }

    @Test
    fun tumunuYenileHerBoncuguGunceller() {
        c.applyInternalStateFromValue(6)
        c.refreshAll()
        assertEquals(25, host.gorunumCagriSayisi)
        assertEquals(true, host.gorunum[c.topBeads[4]])
        assertEquals(true, host.gorunum[c.bottomBeads[4][0]])
        assertEquals(false, host.gorunum[c.bottomBeads[4][1]])
    }

    @Test
    fun boncukDegisiklikDinleyicisi() {
        var sayac = 0
        val b = c.bottomBeads[0][0]
        b.onChanged = { sayac++ }
        b.bottomMargin = 5
        b.translationY = 2f
        assertEquals(2, sayac)
        assertNull(c.topBeads[0].onChanged)
    }
}

class AbacusBeadMetricsTest {
    private fun rod(topGap: Int, bottomGap: Int) = AbacusBeadMetrics.RodMeasurement(
        topBeadY = 100, topBeadHeight = 40, // üst boncuğun altı = 140
        barrierY = 140 + topGap, barrierHeight = 10,
        bottom1Y = 140 + topGap + 10 + bottomGap,
    )

    @Test
    fun dimenAdimlari() {
        assertEquals(48f, AbacusBeadMetrics.bottomStepPx(1f))
        assertEquals(132f, AbacusBeadMetrics.bottomStepPx(2.75f))
        assertEquals(132, AbacusBeadMetrics.bottomStepPxInt(2.75f))
        assertEquals(84f, AbacusBeadMetrics.topStepPx(2.625f))
        assertEquals(84, AbacusBeadMetrics.topStepPxInt(2.625f))
    }

    @Test
    fun bariyerAraliklarindanOrtalama() {
        val d = AbacusBeadMetrics.fromBarrierDistances(listOf(rod(32, 48), rod(30, 50)))!!
        assertEquals(31f, d.topPx)
        assertEquals(49f, d.bottomPx)

        val yari = AbacusBeadMetrics.fromBarrierDistances(listOf(rod(32, 48), rod(30, 50)), ratio = 0.5f)!!
        assertEquals(15.5f, yari.topPx)
        assertEquals(24.5f, yari.bottomPx)

        // Oran 1'e kırpılır
        val fazla = AbacusBeadMetrics.fromBarrierDistances(listOf(rod(32, 48)), ratio = 2f)!!
        assertEquals(32f, fazla.topPx)
        assertEquals(48f, fazla.bottomPx)
    }

    @Test
    fun gecersizOlcumler() {
        assertNull(AbacusBeadMetrics.fromBarrierDistances(emptyList()))
        assertNull(AbacusBeadMetrics.fromBarrierDistances(listOf(rod(32, 48)), ratio = 0f))
        assertNull(AbacusBeadMetrics.fromBarrierDistances(listOf(rod(32, 48)), ratio = -1f))
        // Pozitif olmayan aralıklar sayılmaz; bir taraf tümüyle geçersizse null
        assertNull(AbacusBeadMetrics.fromBarrierDistances(listOf(rod(0, 48), rod(-5, 50))))
        assertNull(AbacusBeadMetrics.fromBarrierDistances(listOf(rod(32, 0))))
        // Aralıklar bağımsız toplanır: üst yalnızca 1. çubuktan, alt yalnızca 2. çubuktan
        val d = AbacusBeadMetrics.fromBarrierDistances(listOf(rod(30, -1), rod(0, 44)))!!
        assertEquals(30f, d.topPx)
        assertEquals(44f, d.bottomPx)
        // Çok küçük sonuç 1'e çekilir
        val kucuk = AbacusBeadMetrics.fromBarrierDistances(listOf(rod(1, 1)), ratio = 0.5f)!!
        assertEquals(1f, kucuk.topPx)
        assertEquals(1f, kucuk.bottomPx)
    }
}
