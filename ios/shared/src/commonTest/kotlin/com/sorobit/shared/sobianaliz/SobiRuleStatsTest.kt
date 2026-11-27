// SobiRuleStats (Android): kural sayımı, SharedPreferences anahtarları ve sunucuya gidecek yük.
// Beklenen değerler Android kodu okunarak türetildi (Android'de bu dosyanın birim testi yok).
package com.sorobit.shared.sobianaliz

import com.sorobit.shared.oyun.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SobiRuleStatsTest {

    private val write248 = listOf(0, 100, 200, 210, 220, 230, 240, 245, 246, 247, 248)

    @Test
    fun dogru_cevap_butun_hamlelerin_kurallarina_dogru() {
        val prefs = InMemoryKeyValueStore()
        val store = SobiRuleStatsStore(prefs)
        // 248 + 223: yüzler kuralsız, onlar 5'lik, birler 10'luk (elde kuralsız, sayılmaz).
        store.recordAnswer(listOf(248, 223), true, emptyList(), 471)
        assertEquals(
            listOf(
                SobiRuleStatEntry("add_direct", 1, 0),
                SobiRuleStatEntry("add_five", 1, 0),
                SobiRuleStatEntry("add_ten", 1, 0),
            ),
            store.snapshot(false),
        )
        assertEquals(1, prefs.getInt("p_q_ok", 0))
        assertEquals(1, prefs.getInt("p_ok_add_ten", 0))
    }

    @Test
    fun yanlis_cevapta_yalnizca_hatanin_hamlesi() {
        val store = SobiRuleStatsStore(InMemoryKeyValueStore())
        val log = write248 + listOf(348, 448, 498, 488, 478, 468, 463, 462, 461)
        store.recordAnswer(listOf(248, 223), false, log, 461)
        assertEquals(listOf(SobiRuleStatEntry("add_ten", 0, 1)), store.snapshot(false))
        // Kayıt yoksa kural sayılmaz, yalnızca soru.
        val bos = InMemoryKeyValueStore()
        SobiRuleStatsStore(bos).recordAnswer(listOf(248, 223), false, emptyList(), 461)
        assertEquals(emptyList(), SobiRuleStatsStore(bos).snapshot(true))
        assertEquals(1, bos.getInt("p_q_bad", 0))
    }

    @Test
    fun analiz_ekraninda_adim_soru_sayilmaz() {
        val prefs = InMemoryKeyValueStore()
        val store = SobiRuleStatsStore(prefs)
        val m = StepExplainer.explain(1953, HomeworkOp.ADD, 70)!!.moves.single()
        store.recordMove(m, false)
        assertEquals(listOf(SobiRuleStatEntry("add_ten", 0, 1), SobiRuleStatEntry("add_bead", 0, 1)).sortedBy { it.key }, store.snapshot(false).sortedBy { it.key })
        assertFalse(prefs.contains("p_q_bad"))
    }

    @Test
    fun zihinden_ve_carpma_anahtarlari() {
        val store = SobiRuleStatsStore(InMemoryKeyValueStore())
        store.recordMental(listOf(3, 4), false)
        store.recordMultiplication(23, 4, true, withAbacus = true)
        assertEquals(emptyList(), store.snapshot(false))
        assertEquals(
            listOf(SobiRuleStatEntry("m_add_five", 0, 1), SobiRuleStatEntry("mulA_2x1", 1, 0)),
            store.snapshot(true),
        )
        assertEquals("mulM_3x2", SobiRuleStats.mulKeyOf(12, 345, false))
    }

    @Test
    fun dizi_yaniginda_yalnizca_hatali_islem() {
        val store = SobiRuleStatsStore(InMemoryKeyValueStore())
        val log = listOf(0, 50, 50, 60, 70, 75, 76, 77, 78, 78, 88, 86)
        store.recordSequence(listOf(50, 28, 9), false, log, listOf(0, 2, 9), 86)
        assertEquals(listOf(SobiRuleStatEntry("add_ten", 0, 1)), store.snapshot(false))
    }

    @Test
    fun sunucu_yuku_ve_dusum() {
        val prefs = InMemoryKeyValueStore()
        val store = SobiRuleStatsStore(prefs)
        assertNull(store.pendingTally())
        store.recordAnswer(listOf(3, 4), true, emptyList(), 7)
        val yuk = store.pendingTally()!!
        assertEquals(mapOf("add_five" to mapOf("c" to 1, "w" to 0)), yuk.tally)
        assertEquals(1, yuk.questionCorrect)
        assertEquals(0, yuk.questionWrong)
        assertFalse(yuk.hasMore)
        // Arada yeni sayım gelirse o kalır.
        store.recordAnswer(listOf(3, 4), true, emptyList(), 7)
        store.markSent(yuk)
        assertEquals(1, prefs.getInt("p_ok_add_five", 0))
        assertEquals(1, prefs.getInt("p_q_ok", 0))
        // Cihazdaki toplam düşülmez.
        assertEquals(2, prefs.getInt("ok_add_five", 0))
    }

    @Test
    fun sinirdan_fazlasi_bir_sonraki_gonderime() {
        val prefs = InMemoryKeyValueStore()
        prefs.putInt("p_ok_add_ten", 250)
        val store = SobiRuleStatsStore(prefs)
        val yuk = store.pendingTally()!!
        assertEquals(200, yuk.tally.getValue("add_ten").getValue("c"))
        assertTrue(yuk.hasMore)
        store.markSent(yuk)
        assertEquals(50, prefs.getInt("p_ok_add_ten", 0))
    }

    @Test
    fun anahtar_listeleri() {
        assertEquals(8, SobiRuleStats.BASE_KEYS.size)
        assertTrue("m_sub_bead" in SobiRuleStats.KEYS)
        assertTrue("mulA_4x1" in SobiRuleStats.KEYS)
        assertTrue("mulM_6x3" in SobiRuleStats.KEYS)
        assertFalse("mulA_2x3" in SobiRuleStats.KEYS)
    }
}
