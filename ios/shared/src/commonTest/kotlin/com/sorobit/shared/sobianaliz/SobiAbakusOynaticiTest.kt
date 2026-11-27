// SobiAbacusPlayer (Android) zamanlaması ve dokunuş sırası: başa getir (0 ms), 700 ms sonra ilk dokunuş,
// dokunuşlar 900 ms arayla, son dokunuştan 900 + 1400 ms sonra başa dönüş, 700 ms sonra bitiş.
package com.sorobit.shared.sobianaliz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SobiAbakusOynaticiTest {

    private fun degerler(p: SobiOynatmaPlani) = p.kareler.map { it.deger }
    private fun zamanlar(p: SobiOynatmaPlani) = p.kareler.map { it.zamanMs }

    @Test
    fun boncuk_kurali_8_arti_6() {
        // 1 boncuk gelir (9), 5 gider (4), 10 gelir (onlara 1: 14).
        val p = SobiAbakusOynatici.plan(8, true, 6)!!
        assertEquals(listOf(8, 9, 4, 14, 8), degerler(p))
        assertEquals(listOf(0L, 700L, 1600L, 2500L, 4800L), zamanlar(p))
        assertEquals(5500L, p.bitisMs)
        assertEquals(listOf(false, true, true, true, false), p.kareler.map { it.hamle })
    }

    @Test
    fun kuralsiz_6_iki_ayri_dokunus() {
        // Beşlik ve birlik aynı adımda: önce 5'lik iner, sonra 1 birlik gelir.
        val p = SobiAbakusOynatici.plan(0, true, 6)!!
        assertEquals(listOf(0, 5, 6, 0), degerler(p))
        assertEquals(listOf(0L, 700L, 1600L, 3900L), zamanlar(p))
        assertEquals(4600L, p.bitisMs)
    }

    @Test
    fun boncuk_cikarma_23_eksi_6() {
        // 10 gider (13), 5 gelir (18), kardeşi 1 gider (17).
        val p = SobiAbakusOynatici.plan(23, false, 6)!!
        assertEquals(listOf(23, 13, 18, 17, 23), degerler(p))
    }

    @Test
    fun elde_zinciri_99_arti_1() {
        // Birlerde 10'luk, onlarda da 10'luk: yüzlere 1 gelir (199), onlardaki 9 gider (5'lik kalkar: 149,
        // 4 birlik gider: 109), birlerdeki 9 gider (105, 100).
        val p = SobiAbakusOynatici.plan(99, true, 1)!!
        assertEquals(listOf(99, 199, 149, 109, 104, 100, 99), degerler(p))
    }

    @Test
    fun sigmayan_islem_oynamaz() {
        assertNull(SobiAbakusOynatici.plan(99_999, true, 1))
        assertNull(SobiAbakusOynatici.plan(3, false, 4))
    }

    @Test
    fun yazma_adimi_sifirdan() {
        // Analizde "sayıyı yaz" adımının gösterimi: 0'dan 248 (yüzler 2, onlar 4, birler 5 ve 3).
        val p = SobiAbakusOynatici.plan(0, true, 248)!!
        assertEquals(listOf(0, 200, 240, 245, 248, 0), degerler(p))
    }
}
