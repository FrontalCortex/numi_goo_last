package com.sorobit.shared.baglanti

import com.sorobit.shared.abakus.BlindingOperation
import com.sorobit.shared.dersadim.LessonOperations
import com.sorobit.shared.matematik.MathOperation
import com.sorobit.shared.matematik.RastgeleKaynagi
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KorDersKoprusuTest {
    @AfterTest
    fun temizle() = RastgeleKaynagi.varsayilanaDon()

    private fun korDizi(vararg diziler: List<Int>, partId: Int = 4, sure: Long? = null): KorDersOturumu {
        val k = KorDersKurucu(partId, 0, false, 0)
        k.korAyarla(true)
        if (sure != null) k.sayiSuresiAyarla(sure)
        for (d in diziler) {
            k.diziBaslat()
            d.forEach { k.diziyeEkle(it) }
        }
        return k.kur()
    }

    /** Diziyi sonuna kadar oynatır; gösterilen metinleri ve gecikmeleri döndürür. */
    private fun oynat(o: KorDersOturumu): Pair<List<String>, List<Long>> {
        val metinler = mutableListOf<String>()
        val gecikmeler = mutableListOf<Long>()
        var adim: KorDersAdimi? = o.diziBaslat()
        var sonAdim: KorDersAdimi? = null
        while (adim != null) {
            sonAdim = adim
            adim.metin?.let { metinler.add(it) }
            if (adim.zamanlayici == KorDersAdimi.ZAMANLAYICI_YOK) break
            gecikmeler.add(adim.gecikmeMs)
            adim = when (adim.zamanlayici) {
                KorDersAdimi.ZAMANLAYICI_SONRAKI_SAYI -> o.sonrakiSayiAdimi()
                else -> o.sayiGosterAdimi()
            }
        }
        // Dizi oynatıldıysa son adım "bitti" olmalı (kontrol düğmesi açılır).
        assertTrue(sonAdim == null || sonAdim.bitti)
        return metinler to gecikmeler
    }

    @Test
    fun listeCevrimiAndroiddekiWhenIleAyni() {
        val cevrilen = korDersIslemlerineCevir(listOf(listOf(1, 2, 3), MathOperation(4, "x", 5), 7))
        assertEquals(BlindingOperation.Sequence(listOf(1, 2, 3)), cevrilen[0])
        assertEquals(BlindingOperation.Math(4, "x", 5), cevrilen[1])
        assertEquals(BlindingOperation.Other, cevrilen[2])
    }

    @Test
    fun diziGosterimiSureVeBoslukSirasi() {
        val o = korDizi(listOf(10, 3, 5), sure = 700L)
        assertFalse(o.diziGosteriliyor)
        val (metinler, gecikmeler) = oynat(o)
        // sayı → (süre) → boş → (200 ms) → sayı ...
        assertEquals(listOf("10", "", "3", "", "5"), metinler)
        assertEquals(listOf(700L, 200L, 700L, 200L), gecikmeler)
        assertFalse(o.diziGosteriliyor)
        assertTrue(o.diziVar)
    }

    @Test
    fun sureVerilmediyseBirSaniye() {
        val o = korDizi(listOf(1, 2))
        assertEquals(1000L, o.diziBaslat()!!.gecikmeMs)
    }

    @Test
    fun korDersDogruVeYanlisAkisi() {
        val o = korDizi(listOf(10, 3, 5), listOf(2, 2))
        assertTrue(o.klavyeyleCevap)
        assertFalse(o.abakusleCevap)
        assertFalse(o.carpmaGosterimi)
        assertEquals(2, o.soruSayisi)

        oynat(o)
        assertEquals(0.5f, o.ilerlemeBitisAgirligi())
        assertTrue(o.kontrolEt("18", 0))
        assertFalse(o.dogruIslendi())
        assertTrue(o.sonrakiSoru())

        oynat(o)
        assertEquals(1f, o.ilerlemeBitisAgirligi())
        assertFalse(o.kontrolEt("5", 0))
        assertEquals("Doğru cevap 4", o.dogruCevapEtiketi)
        assertEquals("Senin cevabın 5", o.seninCevabinMetni)
        assertEquals("NEXT_QUESTION", o.yanlisSonrasi())
        assertFalse(o.sonrakiSoru())

        assertEquals(1, o.dogruSayisi)
        assertEquals(50f, o.basariOrani())
        assertEquals(250, o.dersPuani())
        // 4. bölüm: güncel Android %75 sınırı; %50 < %75 → LESSON_RESULT_FALSE (eskiden %50 sınırıyla LESSON_RESULT)
        assertEquals("LESSON_RESULT_FALSE", o.bitis("0:00"))
    }

    @Test
    fun diziAtlamaVeKupaKapilari() {
        val o = korDizi(listOf(4, 6, 1))
        assertNull(o.diziAtla()) // gösterim başlamadan yok sayılır
        o.diziBaslat()
        val adim = o.diziAtla()!!
        assertEquals("", adim.metin)
        assertEquals(KorDersAdimi.ZAMANLAYICI_SAYI_GOSTER, adim.zamanlayici)
        assertEquals(200L, adim.gecikmeMs)
        assertNull(o.diziAtla()) // boşluk sürerken ikinci basış yok sayılır
        assertEquals("6", o.sayiGosterAdimi().metin)

        // Kapılar tur başına bir kez
        assertTrue(o.dersBitisiHakkiAl()); assertFalse(o.dersBitisiHakkiAl())
        assertTrue(o.basariOraniKapsaminda())
        // korDizi korAyarla(true) ile kurar → isBlinding = true → Android cupModeName "blinding"
        assertEquals("blinding", o.kupaModuAdi())

        val kupa = KorDersKurucu(9, 0, false, 0).also { it.korAyarla(true); it.diziBaslat(); it.diziyeEkle(1) }.kur()
        assertFalse(kupa.basariOraniKapsaminda()) // kupa modu telemetriye dahil değil
        assertTrue(kupa.kupaCikisCezasiVerilmeli(dersBasladi = true))
        assertFalse(kupa.kupaCikisCezasiVerilmeli(dersBasladi = false))
        assertTrue(kupa.kupaSonucuHakkiAl())
        assertFalse(kupa.kupaSonucuHakkiAl())
        assertFalse(kupa.kupaCikisCezasiVerilmeli(dersBasladi = true)) // cevabını verdi, terk sayılmaz
        assertTrue(kupa.kupaYarisLoguHakkiAl()); assertFalse(kupa.kupaYarisLoguHakkiAl())
    }

    @Test
    fun boncukAnimasyonuHerDersteUcYuzMs() {
        // Güncel Android: sandıkta da 300 ms (eskiden 50)
        assertEquals(300L, KorDersKurucu(1, 2, false, 0).kur().boncukAnimasyonMs)
        assertEquals(300L, KorDersKurucu(1, 0, false, 0).kur().boncukAnimasyonMs)
    }

    @Test
    fun diziGosterilmedenCevapYanlis() {
        val o = korDizi(listOf(1, 2))
        assertFalse(o.kontrolEt("3", 0))
    }

    @Test
    fun tumCevaplarDogruysaSonSorudaKonfeti() {
        val o = korDizi(listOf(1, 2))
        oynat(o)
        assertTrue(o.kontrolEt("3", 0))
        assertTrue(o.dogruIslendi())
    }

    @Test
    fun kupaModuDeltaVeYonlendirme() {
        val k = KorDersKurucu(9, 0, false, 0)
        k.korAyarla(true)
        k.cikarmaAyarla(true)
        k.kupaKazancAyarla(12)
        k.diziBaslat()
        k.diziyeEkle(5)
        k.diziyeEkle(-2)
        // Kupa modunda tablo sorulmaz.
        k.tablodanYukle(1, 0, false, 1, false)
        val o = k.kur()
        assertTrue(o.kupaModu)
        assertTrue(o.ilerlemeGizli)
        assertTrue(o.ipucuGizli)
        assertEquals(1, o.soruSayisi)
        assertEquals(12, o.kupaDeltasi(true))
        assertEquals(-30, o.kupaDeltasi(false))
        assertEquals("PENDING_BLINDING_EXTRACTION_CUP_DELTA", o.kupaDeltaHedefi())
        assertEquals("CUP_RESULT_FALSE", o.yanlisSonrasi())
        assertEquals("CUP_USE_ENERGY_AND_CLOSE", o.yanlisSonucEylemi())
        assertEquals("CLOSE", o.bitis("0:00"))
    }

    @Test
    fun carpmaGosterimiVeAbakusleCevap() {
        val k = KorDersKurucu(7, 0, false, 0)
        k.carpmaAyarla(true)
        k.islemEkle(12, "x", 3)
        val o = k.kur()
        assertTrue(o.carpmaGosterimi)
        assertTrue(o.abakusleCevap)
        assertTrue(o.adimAtlaGizli)
        assertNull(o.diziBaslat())
        assertEquals("12", o.birinciSayiMetni())
        assertEquals("x", o.islecMetni())
        assertEquals("3", o.ikinciSayiMetni())
        assertTrue(o.kontrolEt("", 36))
        assertFalse(o.kontrolEt("36", 35))
    }

    @Test
    fun korCarpmaKlavyedenOkur() {
        val k = KorDersKurucu(8, 0, false, 0)
        k.korAyarla(true)
        k.korCarpmaAyarla(true)
        k.islemEkle(6, "x", 7)
        val o = k.kur()
        assertTrue(o.kontrolEt("42", 0))
        assertFalse(o.kontrolEt("", 42))
        assertFalse(o.kontrolEt("abc", 42))
    }

    @Test
    fun tablodanYuklemeAndroiddekiAdimHesabiylaAyni() {
        RastgeleKaynagi.rastgele = Random(7)
        val beklenen = korDersIslemlerineCevir(LessonOperations.getLessonOperationsBlinding(1))
        RastgeleKaynagi.rastgele = Random(7)
        val k = KorDersKurucu(4, 0, false, 0)
        k.korAyarla(true)
        k.tablodanYukle(1, 0, false, 1, false)
        val o = k.kur()
        assertEquals(beklenen.size, o.soruSayisi)
        assertEquals(1, o.yuklenenDersAdimi)
        val ilk = (beklenen[0] as BlindingOperation.Sequence).numbers
        assertEquals(ilk[0].toString(), o.diziBaslat()!!.metin)
    }

    @Test
    fun disaridanSoruVarsaTabloSorulmaz() {
        val k = KorDersKurucu(4, 0, false, 0)
        k.diziBaslat()
        k.diziyeEkle(9)
        k.tablodanYukle(1, 0, false, 1, false)
        val o = k.kur()
        assertEquals(1, o.soruSayisi)
        assertEquals(-1, o.yuklenenDersAdimi)
    }

    @Test
    fun eksikAdimBilgisiCokmez() {
        val k = KorDersKurucu(4, 0, false, 0)
        k.tablodanYukle(3, 0, false, 2, true)
        val o = k.kur()
        assertEquals(0, o.soruSayisi)
        assertNull(o.diziBaslat())
        assertFalse(o.kontrolEt("1", 1))
        assertEquals(0f, o.ilerlemeBitisAgirligi())
    }

    @Test
    fun sandikPuaniVeSure() {
        val k = KorDersKurucu(1, 2, false, 0)
        k.korAyarla(true)
        k.enKotuKupaSuresiAyarla(120)
        k.diziBaslat()
        k.diziyeEkle(4)
        val o = k.kur()
        assertTrue(o.sureGorunur)
        assertTrue(o.sureBaslatilmali(false))
        assertFalse(o.sureBaslatilmali(true))
        assertEquals("1:05", o.sureMetni(65))
        assertEquals(120, o.enKotuKupaSuresi)
        oynat(o)
        assertTrue(o.kontrolEt("4", 0))
        o.dogruIslendi()
        // başarı 100 → ders puanı 500; süre 0:40 → çarpan 4 − 3·40/120 = 3 → 1500
        assertEquals("CHEST_RESULT", o.bitis("0:40"))
        assertEquals(3f, o.carpan)
        assertEquals(1500, o.toplamPuan)
    }

    @Test
    fun gunlukSoruYonlendirmesi() {
        val k = KorDersKurucu(3, 0, true, 0)
        k.diziBaslat()
        k.diziyeEkle(1)
        val o = k.kur()
        assertTrue(o.ilerlemeGizli)
        assertTrue(o.ipucuGizli)
        assertEquals("DAILY_QUESTION_PANEL", o.yanlisSonrasi())
        assertEquals("DAILY_QUESTION_COMPLETE", o.bitis("0:00"))
    }

    @Test
    fun sifirlamaDiziyiBastanBaslatir() {
        val o = korDizi(listOf(8, 9))
        oynat(o)
        o.diziDurdur()
        val adim = o.diziYenidenBaslat()
        assertEquals("8", adim.metin)
        assertTrue(adim.kontrolKapat)
        assertTrue(o.diziGosteriliyor)
    }
}
