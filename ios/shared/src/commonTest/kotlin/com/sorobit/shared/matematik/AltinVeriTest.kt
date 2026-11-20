package com.sorobit.shared.matematik

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ortak koddaki üreteçlerin, ÖZGÜN Android kaynak dosyasının aynı tohumla verdiği çıktıyı
 * birebir verdiğini doğrular (bkz. AltinVeri.kt). JVM ve Kotlin/Native'de çalışır.
 */
class AltinVeriTest {
    @Test
    fun altinVeriTumDurumlariKapsar() {
        assertEquals(UretecDurumlari.TOHUMLAR.size * UretecDurumlari.durumlar.size, ALTIN_VERI.size)
        // Tablo, üretecin 66 açık fonksiyonunun tamamını ve GlobalValues'un 3 fonksiyonunu içerir.
        val adlar = UretecDurumlari.durumlar.map { it.first.substringBefore("(") }.toSet()
        assertEquals(66, adlar.count { !it.startsWith("G.") })
        assertEquals(3, adlar.count { it.startsWith("G.") })
        assertEquals(UretecDurumlari.durumlar.size, UretecDurumlari.durumlar.map { it.first }.toSet().size)
    }

    @Test
    fun ortakKodAndroidCiktisiylaAyni() {
        val harita = UretecDurumlari.durumlar.toMap()
        var istisnaSayisi = 0
        for (satir in ALTIN_VERI) {
            val parcalar = satir.split("|", limit = 3)
            val tohum = parcalar[0].toInt()
            val anahtar = parcalar[1]
            val beklenen = parcalar[2]
            val f = harita[anahtar] ?: error("Bilinmeyen durum: $anahtar")
            assertEquals(beklenen, UretecDurumlari.calistir(tohum, f), "tohum=$tohum $anahtar")
            if (beklenen.startsWith("EXC:")) istisnaSayisi++
        }
        // Android'de istisna atan durumlar (ör. ikinci sayı basamağı > birinci) da aynen korunur.
        assertTrue(istisnaSayisi > 0)
    }

    @Test
    fun ayniTohumAyniSonuc() {
        for ((anahtar, f) in UretecDurumlari.durumlar) {
            assertEquals(UretecDurumlari.calistir(77, f), UretecDurumlari.calistir(77, f), anahtar)
        }
    }
}
