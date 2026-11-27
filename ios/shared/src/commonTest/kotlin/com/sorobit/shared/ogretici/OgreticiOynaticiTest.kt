// Öğretici oynatıcısı ve sahne hesabı: beklenenler TutorialFragment.kt (createTutorialSteps1,
// handleTutorialTapToAdvance, setupBackButton, showStep, showResultPanelForOptions,
// incorrectButtonClick) okunarak elle türetildi.
package com.sorobit.shared.ogretici

import com.sorobit.shared.ogretici.OgreticiEtki.Companion.ABAKUS_SIFIRLA
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.ADIM_GOSTER
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.BEKLE
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.DEGER_KUR
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.KAPAT
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.METNI_TAMAMLA
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.SAYI_YAZ
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.SECIM_UYARISI
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.SONUC_GIZLE
import com.sorobit.shared.ogretici.OgreticiEtki.Companion.SONUC_GOSTER
import com.sorobit.shared.ogretici.OgreticiSabit.GORUNMEZ
import com.sorobit.shared.ogretici.OgreticiSabit.GORUNUR
import com.sorobit.shared.ogretici.OgreticiSabit.YOK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OgreticiOynaticiTest {
    private val ilk: OgreticiIcerigi = OgreticiKatalogu.icerik(1)!!

    private fun turler(etkiler: List<OgreticiEtki>): List<Int> = etkiler.map { it.tur }

    /** Oynatıcıyı [adim]'a dokunarak getirir (soru adımlarında doğru cevapla). */
    private fun adimaGetir(o: OgreticiOynatici, adim: Int) {
        while (o.adim < adim) {
            val a = o.mevcutAdim()!!
            if (a.abakusTiklanabilir) {
                o.kontrol(a.cevap)
                o.dogruDevam()
            } else {
                o.dokunuldu(mesgul = false, daktiloSuruyor = false)
            }
        }
    }

    @Test
    fun katalog() {
        assertEquals(34, OgreticiKatalogu.numaralar.size)
        assertTrue(OgreticiKatalogu.varMi(1))
        assertNull(OgreticiKatalogu.icerik(12345))
        assertNull(OgreticiKatalogu.oynaticiKur(12345, false, 0))
        assertNotNull(OgreticiKatalogu.oynaticiKur(1, true, 999))
        assertEquals(29, OgreticiKatalogu.oynaticiKur(1, true, 999)!!.adim)
    }

    @Test
    fun ilkOgreticininIcerigiAndroidIleAyni() {
        assertEquals(1, ilk.numara)
        assertNull(ilk.kuralTablosu)
        assertEquals(30, ilk.adimSayisi)
        assertEquals("tutorial1_1", ilk.adim(0)!!.metin)
        assertEquals("tutorial1_21", ilk.adim(29)!!.metin)
        assertTrue(ilk.adimlar.all { it.daktilo && it.daktiloHiziMs == 40L && it.ses == it.metin })
        // Beş soru adımı: 1, 2, 5, 6, 8 (adım 25-29).
        val sorular = ilk.adimlar.filter { it.abakusTiklanabilir }
        assertEquals(listOf(1, 2, 5, 6, 8), sorular.map { it.cevap })
        assertEquals(listOf("1", "2", "5", "6", "8"), sorular.map { it.soru })
        assertTrue(sorular.all { !it.ilerlenebilir && it.abakusSifirla && it.sonrakindeSifirla && it.soruGorunur() })
        assertEquals(listOf("t1_1_asagidaki", "t1_2_yaz", "t1_5_yaz", "t1_6_yaz", "t1_8_yaz"), sorular.map { it.kimlik })
        assertEquals("tutorial_ipucu_001", sorular[0].ipucu)
        // Adım 24 (tutorial1_108): dört alt boncuk aşağı, üst boncuk yukarı; abacusReset.
        val a24 = ilk.adim(23)!!
        assertEquals("tutorial1_108", a24.metin)
        assertEquals(5, a24.hareketler.size)
        assertTrue(a24.abakusSifirla)
        assertEquals(4, a24.hareketler.last().tur)
        assertTrue(a24.hareketler.last().ust)
    }

    @Test
    fun kimliklerinBicimiAndroidDenetimiyleAyni() {
        // Android buildStepAnalyticsInfo: soru adımında kimlik zorunlu, t<no>_[a-z0-9_]+, en çok 40, tekil.
        for (no in OgreticiKatalogu.numaralar) {
            val icerik = OgreticiKatalogu.icerik(no) ?: continue
            val desen = Regex("t${no}_[a-z0-9_]+")
            val gorulen = HashSet<String>()
            icerik.adimlar.forEachIndexed { i, a ->
                if (!a.abakusTiklanabilir && !a.gecerliSecenekVar()) return@forEachIndexed
                val k = a.kimlik
                assertNotNull(k, "öğretici $no adım ${i + 1}: kimlik yok")
                assertTrue(desen.matches(k) && k.length <= 40, "öğretici $no adım ${i + 1}: $k")
                assertTrue(gorulen.add(k), "öğretici $no: $k iki kez")
            }
        }
    }

    /** Taşınmış her öğretici, doğru cevaplarla baştan sona çıkmaza girmeden oynanabilmeli. */
    @Test
    fun herOgreticiBastanSonaOynanir() {
        for (no in OgreticiKatalogu.numaralar) {
            val icerik = OgreticiKatalogu.icerik(no) ?: continue
            val o = OgreticiOynatici(icerik, ilkAcilis = false, baslangicAdimi = 0)
            o.basla()
            var tur = 0
            while (o.adim < o.adimSayisi - 1 && tur < 10_000) {
                tur++
                val a = o.mevcutAdim()!!
                val onceki = o.adim
                when {
                    a.abakusTiklanabilir -> {
                        assertEquals(1, o.kontrol(a.cevap).single().deger, "öğretici $no adım ${onceki + 1}")
                        o.dogruDevam()
                    }
                    a.gecerliSecenekVar() -> {
                        assertEquals(1, o.secenekKontrol(a.dogruSecenekler).single().deger, "öğretici $no adım ${onceki + 1}")
                        o.dogruDevam()
                    }
                    else -> o.dokunuldu(mesgul = false, daktiloSuruyor = false)
                }
                assertEquals(onceki + 1, o.adim, "öğretici $no: adım ${onceki + 1}'den ilerlenemedi (çıkmaz)")
            }
            assertTrue(o.sahne().devamGorunur, "öğretici $no: son adıma varılamadı")
        }
    }

    @Test
    fun ilkAcilistaAtlaVeCikisGizli() {
        val o = OgreticiOynatici(ilk, ilkAcilis = true, baslangicAdimi = 0)
        val s = o.sahne()
        assertEquals(GORUNMEZ, s.abakusGorunurlugu)
        assertEquals(GORUNMEZ, s.geriGorunurlugu)
        assertEquals(GORUNMEZ, s.atlaGorunurlugu)
        assertFalse(s.cikisGorunur)
        assertFalse(s.kontrolGorunur)
        assertTrue(s.abakusKilitli)
        assertFalse(s.devamGorunur)
        assertFalse(s.odakGorunur)
        // Haritadan açılan 1. öğretici (hesap var): atla ve çıkış görünür.
        val h = OgreticiOynatici(ilk, ilkAcilis = false, baslangicAdimi = 0).sahne()
        assertEquals(GORUNUR, h.atlaGorunurlugu)
        assertTrue(h.cikisGorunur)
    }

    @Test
    fun dokunmaIlerletirDaktiloyuTamamlar() {
        val o = OgreticiOynatici(ilk, ilkAcilis = true, baslangicAdimi = 0)
        val bas = o.basla()
        assertEquals(listOf(ADIM_GOSTER), turler(bas))
        assertEquals(0, bas[0].adim)
        assertEquals(listOf(METNI_TAMAMLA), turler(o.dokunuldu(mesgul = false, daktiloSuruyor = true)))
        assertEquals(0, o.adim)
        assertTrue(o.dokunuldu(mesgul = true, daktiloSuruyor = false).isEmpty())
        val ileri = o.dokunuldu(mesgul = false, daktiloSuruyor = false)
        assertEquals(listOf(ADIM_GOSTER), turler(ileri))
        assertEquals(1, o.adim)
        assertTrue(ileri[0].ileri)
        // 2. adım: geri düğmesi görünür olur; abaküs 3. adımda gelir.
        assertEquals(GORUNUR, o.sahne().geriGorunurlugu)
        assertEquals(GORUNMEZ, o.sahne().abakusGorunurlugu)
        o.dokunuldu(mesgul = false, daktiloSuruyor = false)
        assertEquals(GORUNUR, o.sahne().abakusGorunurlugu)
        // Ekranın sahnesi hesaplandığı için geri dönünce abaküs yeniden gizlenir.
        o.geri(mesgul = false)
        assertEquals(GORUNMEZ, o.sahne().abakusGorunurlugu)
        // 1. öğreticide geri "Eğitimi atla"yı göstermez.
        assertEquals(GORUNMEZ, o.sahne().atlaGorunurlugu)
    }

    @Test
    fun odakCercevesiSutunSutunKayar() {
        val o = OgreticiOynatici(ilk, ilkAcilis = false, baslangicAdimi = 5)
        var s = o.sahne()
        assertTrue(s.odakGorunur)
        assertEquals(19f, s.odakSagBoslukDp)
        assertEquals(54f, s.odakGenislikDp)
        assertEquals(255f, s.odakYukseklikDp)
        assertEquals(OgreticiSabit.CAPA_ORTA, s.odakCapa)
        val sutunlar = (6..9).map { OgreticiKurallari.sahne(ilk, it, false, false).odakSagBoslukDp }
        assertEquals(listOf(64f, 109f, 154f, 199f), sutunlar)
        s = OgreticiKurallari.sahne(ilk, 10, false, false)
        assertEquals(0f, s.odakSagBoslukDp)
        assertEquals(270f, s.odakGenislikDp)
        assertEquals(280f, s.odakYukseklikDp)
        s = OgreticiKurallari.sahne(ilk, 11, false, false)
        assertEquals(OgreticiSabit.CAPA_UST_KILAVUZ, s.odakCapa)
        assertEquals(60f, s.odakYukseklikDp)
        s = OgreticiKurallari.sahne(ilk, 12, false, false)
        assertEquals(OgreticiSabit.CAPA_ALT_KILAVUZ, s.odakCapa)
        assertEquals(180f, s.odakYukseklikDp)
        assertFalse(OgreticiKurallari.sahne(ilk, 13, false, false).odakGorunur)
    }

    @Test
    fun baslangicDegeriBoncukHareketlerindenGelir() {
        assertEquals(0, OgreticiKurallari.baslangicDegeri(ilk, 13))
        assertEquals(1, OgreticiKurallari.baslangicDegeri(ilk, 14))
        assertEquals(4, OgreticiKurallari.baslangicDegeri(ilk, 17))
        assertEquals(0, OgreticiKurallari.baslangicDegeri(ilk, 18))
        assertEquals(5, OgreticiKurallari.baslangicDegeri(ilk, 19))
        assertEquals(9, OgreticiKurallari.baslangicDegeri(ilk, 23))
        assertEquals(0, OgreticiKurallari.baslangicDegeri(ilk, 24))
        assertEquals(0, OgreticiKurallari.baslangicDegeri(ilk, 26))
        // Ortadan açılış: önce sayı kurulur, sonra adım gösterilir.
        val o = OgreticiOynatici(ilk, ilkAcilis = false, baslangicAdimi = 16)
        val bas = o.basla()
        assertEquals(listOf(DEGER_KUR, ADIM_GOSTER), turler(bas))
        assertEquals(3, bas[0].deger)
        assertEquals(1, bas[1].hareketler.size)
    }

    @Test
    fun geriGidinceSonrakiAdiminHareketleriTersOynar() {
        val o = OgreticiOynatici(ilk, ilkAcilis = false, baslangicAdimi = 0)
        adimaGetir(o, 15)
        val geri = o.geri(mesgul = false)
        assertEquals(14, o.adim)
        assertEquals(listOf(ADIM_GOSTER), turler(geri))
        assertFalse(geri[0].ileri)
        val h = geri[0].hareketler.single()
        // 15. adım (tutorial1_101) bottom3'ü yukarı (1) almıştı; geri: aşağı (2).
        assertEquals(4, h.cubuk)
        assertEquals(2, h.sira)
        assertEquals(2, h.tur)
        // Başka bir öğreticide geri "Eğitimi atla"yı görünür yapar.
        assertTrue(o.sahne().atlaGorunurlugu == GORUNUR)
    }

    @Test
    fun soruAdimindaDokunmaIlerletmezKontrolPaneliAcar() {
        val o = OgreticiOynatici(ilk, ilkAcilis = true, baslangicAdimi = 0)
        adimaGetir(o, 24)
        assertEquals(24, o.adim)
        val s = o.sahne()
        assertTrue(s.kontrolGorunur)
        assertFalse(s.abakusKilitli)
        assertTrue(o.dokunuldu(mesgul = false, daktiloSuruyor = false).isEmpty())
        assertEquals(24, o.adim)

        // Yanlış: panel açılır; açıkken dokunma / geri / ikinci kontrol yok sayılır.
        val yanlis = o.kontrol(3)
        assertEquals(listOf(SONUC_GOSTER), turler(yanlis))
        assertEquals(0, yanlis[0].deger)
        assertTrue(o.dokunuldu(mesgul = false, daktiloSuruyor = false).isEmpty())
        assertTrue(o.geri(mesgul = false).isEmpty())
        assertTrue(o.kontrol(1).isEmpty())
        // Tekrar Dene: abacusReset → sıfırla, adım animasyonsuz yeniden gösterilir.
        val tekrar = o.yanlisTamam()
        assertEquals(listOf(SONUC_GIZLE, ABAKUS_SIFIRLA, ADIM_GOSTER), turler(tekrar))
        assertTrue(tekrar[2].hareketler.isEmpty())
        assertEquals(24, o.adim)

        // Doğru: Devam → nextStepAbacusReset sıfırlar, sonraki soru.
        val dogru = o.kontrol(1)
        assertEquals(1, dogru[0].deger)
        val devam = o.dogruDevam()
        assertEquals(listOf(SONUC_GIZLE, ABAKUS_SIFIRLA, ADIM_GOSTER), turler(devam))
        assertEquals(25, o.adim)
    }

    @Test
    fun sonAdimdaDevamGorunurAbakusGizli() {
        val o = OgreticiOynatici(ilk, ilkAcilis = true, baslangicAdimi = 0)
        adimaGetir(o, 29)
        val s = o.sahne()
        assertTrue(s.sonAdim)
        assertTrue(s.devamGorunur)
        assertEquals(GORUNMEZ, s.abakusGorunurlugu)
        assertEquals(GORUNMEZ, s.atlaGorunurlugu)
        // Son adımdan ileri dokunma bir şey yapmaz.
        assertTrue(o.dokunuldu(mesgul = false, daktiloSuruyor = false).isEmpty())
        // Geri: son soru adımı; abacusReset → sıfırla.
        val geri = o.geri(mesgul = false)
        assertEquals(listOf(ABAKUS_SIFIRLA, ADIM_GOSTER), turler(geri))
        assertFalse(o.sahne().devamGorunur)
    }

    @Test
    fun ilkAdimdaGeriBirSeyYapmaz() {
        val o = OgreticiOynatici(ilk, ilkAcilis = true, baslangicAdimi = 0)
        assertTrue(o.geri(mesgul = false).isEmpty())
        assertEquals(0, o.adim)
    }

    // --- Elle kurulan içerikle: geri cevap, sıfırla ve bekle, seçenekler -------------------------

    private val ornek = OgreticiIcerigi(
        numara = 7,
        kuralTablosu = "tenRuleTableLinearLayout",
        adimlar = listOf(
            OgreticiAdimi(metin = "a"),
            OgreticiAdimi(
                metin = "b", abakusTiklanabilir = true, ilerlenebilir = false, cevap = 7, geriCevap = 3,
                abakusSifirla = true, kimlik = "t7_b", kuralTablosuGorunurlugu = YOK,
            ),
            OgreticiAdimi(metin = "c", sifirlaVeBekle = true, hareketler = listOf(OgreticiBoncukHareketi(4, true, 0, 3))),
            OgreticiAdimi(metin = "d", secenekler = listOf("s1", "s2", "s3"), dogruSecenekler = listOf(0, 2), cokluSecim = true, kimlik = "t7_d"),
            OgreticiAdimi(metin = "e"),
        ),
    )

    @Test
    fun yanlistanSonraGeriCevapYazilir() {
        val o = OgreticiOynatici(ornek, ilkAcilis = false, baslangicAdimi = 1)
        assertEquals(YOK, o.sahne().kuralTablosuGorunurlugu)
        assertEquals(GORUNUR, OgreticiKurallari.sahne(ornek, 0, false, false).kuralTablosuGorunurlugu)
        o.kontrol(5)
        val tekrar = o.yanlisTamam()
        assertEquals(listOf(SONUC_GIZLE, ABAKUS_SIFIRLA, BEKLE, SAYI_YAZ, ADIM_GOSTER), turler(tekrar))
        assertEquals(350L, tekrar[2].gecikmeMs)
        assertEquals(3, tekrar[3].deger)
        assertTrue(tekrar[3].bekle)
    }

    @Test
    fun geriGelinceSifirlaVeBekle() {
        val o = OgreticiOynatici(ornek, ilkAcilis = false, baslangicAdimi = 2)
        val geri = o.geri(mesgul = false)
        // İnilen adım (b): geri cevap yazılır, abacusReset; c'nin resetAndWaith'i: sıfırla + 800 ms.
        assertEquals(listOf(SAYI_YAZ, ABAKUS_SIFIRLA, ABAKUS_SIFIRLA, BEKLE, ADIM_GOSTER), turler(geri))
        assertFalse(geri[0].bekle)
        assertEquals(800L, geri[3].gecikmeMs)
        // Geri cevap yazıldığı için c'nin hareketleri ters oynatılmaz.
        assertTrue(geri[4].hareketler.isEmpty())
    }

    @Test
    fun secenekler() {
        val o = OgreticiOynatici(ornek, ilkAcilis = false, baslangicAdimi = 3)
        assertTrue(o.mevcutAdim()!!.gecerliSecenekVar())
        // Seçenekli adımda dokunma ilerletmez.
        assertTrue(o.dokunuldu(mesgul = false, daktiloSuruyor = false).isEmpty())
        assertEquals(listOf(SECIM_UYARISI), turler(o.secenekKontrol(emptyList())))
        assertEquals(0, o.secenekKontrol(listOf(0))[0].deger)
        o.yanlisTamam()
        assertEquals(1, o.secenekKontrol(listOf(2, 0))[0].deger)
        val devam = o.dogruDevam()
        assertEquals(listOf(SONUC_GIZLE, ADIM_GOSTER), turler(devam))
        assertEquals(4, o.adim)
    }

    @Test
    fun ilkAdimdaYanlisKapatir() {
        val icerik = OgreticiIcerigi(1000, null, listOf(OgreticiAdimi(metin = "x", abakusTiklanabilir = true, cevap = 1)))
        val o = OgreticiOynatici(icerik, ilkAcilis = false, baslangicAdimi = 0)
        o.kontrol(2)
        assertEquals(listOf(SONUC_GIZLE, KAPAT), turler(o.yanlisTamam()))
    }

    @Test
    fun bosIcerikOynatilmaz() {
        val o = OgreticiOynatici(OgreticiIcerigi(5, null, emptyList()), ilkAcilis = false, baslangicAdimi = 0)
        assertFalse(o.oynatilabilir)
        assertTrue(o.basla().isEmpty())
        assertTrue(o.dokunuldu(mesgul = false, daktiloSuruyor = false).isEmpty())
        assertTrue(o.geri(mesgul = false).isEmpty())
    }
}
