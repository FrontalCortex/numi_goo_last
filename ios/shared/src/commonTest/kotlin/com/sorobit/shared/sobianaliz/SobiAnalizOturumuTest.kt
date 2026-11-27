// SobiAnalizOturumu: MistakeReviewDialog'un analiz kipi (adımlar, sayfalar, Kontrol Et, balonlar, örnek).
// Metin çözücü anahtarı ve argümanları "anahtar(a|b)" olarak yazar; böylece hangi strings.xml
// anahtarının hangi argümanlarla istendiği doğrudan sınanır. Beklenen değerler Android kodu
// okunarak türetildi.
package com.sorobit.shared.sobianaliz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object AnahtarCozucu : SobiMetinCozucu {
    override fun metin(anahtar: String, argumanlar: List<Any>): String =
        if (argumanlar.isEmpty()) "$anahtar()" else "$anahtar(${argumanlar.joinToString("|")})"
}

class SobiAnalizOturumuTest {

    private val write248 = listOf(0, 100, 200, 210, 220, 230, 240, 245, 246, 247, 248)
    private val log248 = write248 + listOf(348, 448, 498, 488, 478, 468, 463, 462, 461)

    private fun oturum(numbers: List<Int>, answer: Int, log: List<Int> = emptyList(), marks: List<Int> = emptyList()) =
        SobiAnalizOturumu(numbers, answer, log, marks, AnahtarCozucu)

    @Test
    fun analiz_yapilabilir_mi() {
        assertTrue(SobiAnalizAdimlari.canReview(listOf(3, 4)))
        assertTrue(SobiAnalizAdimlari.canReview(listOf(50, 28, -9)))
        assertTrue(SobiAnalizAdimlari.canReview(listOf(12, 0)))
        assertFalse(SobiAnalizAdimlari.canReview(listOf(3, -4)))
        assertFalse(SobiAnalizAdimlari.canReview(listOf(99_999, 1)))
        assertFalse(SobiAnalizAdimlari.canReview(listOf(100_000, 1)))
        assertFalse(SobiAnalizAdimlari.canReview(listOf(5)))
        assertFalse(SobiAnalizAdimlari.canReview(null))
    }

    @Test
    fun soru_metni() {
        assertEquals("23 + 33 - 12", SobiAnalizAdimlari.questionText(listOf(23, 33, -12)))
        assertEquals("248 + 223", SobiAnalizAdimlari.questionText(listOf(248, 223)))
    }

    @Test
    fun toplama_hatasi_teshisli_sayfalar() {
        val o = oturum(listOf(248, 223), 461, log248)
        assertEquals(MistakeAnalyzer.Kind.MISSING_STEP, o.teshis!!.kind)
        val ilk = o.baslat(0)!!
        // Yazma + yüzler + onlar + birler, sonda sonuç sayfası.
        assertEquals(5, o.sayfaSayisi)
        assertEquals(0, o.sayfa)
        assertEquals("review_step_label(1|4)", o.baslik())
        assertEquals(0, o.sayfaDegeri())
        assertTrue(o.adimSayfasi())
        assertEquals(-1, o.vurguSutunu())
        assertEquals(
            "review_intro_question(248 + 223|461|471)\n\nreview_intro_found(${o.teshis!!.text})\n\nreview_step_write(248)",
            ilk,
        )
        assertEquals(listOf(ilk), o.mesajlar().map { it.metin })
        assertFalse(o.geriVar())
        assertFalse(o.ileriVar())

        // İlk sayı doğru yazıldı.
        val k1 = o.kontrolEt(248, listOf(0, 248))
        assertTrue(k1.dogru)
        assertFalse(k1.kapat)
        assertEquals(1, k1.sonrakiSayfa)
        assertNull(k1.hamle)
        assertEquals("review_correct()", k1.mesaj)
        o.ac(k1.sonrakiSayfa)
        assertEquals(248, o.sayfaDegeri())
        assertEquals(2, o.vurguSutunu())
        assertTrue(o.geriVar())
        assertEquals(
            listOf("review_step_add(Yüzler|2|Kuralsız|2 gelir)"),
            o.mesajlar().map { it.metin },
        )

        // Birler adımı: "Az önce burada şaşmıştın" uyarısı.
        o.ac(3)
        assertEquals(
            "review_was_here()\n\nreview_step_add(Birler|3|10'luk kural|10 gelir, büyük kardeşi 7 gider)",
            o.mesajlar().single().metin,
        )
        assertEquals(468, o.sayfaDegeri())
        // Yine 10'u unuttu: adım teşhisi, abaküs 468'e döner.
        val k2 = o.kontrolEt(461, listOf(468, 463, 462, 461))
        assertFalse(k2.dogru)
        assertEquals(468, k2.geriDeger)
        assertEquals(AbacusRule.TEN, k2.hamle!!.rule)
        assertTrue(k2.mesaj!!.startsWith("review_wrong(Birler basamağında 3 eklerken"), k2.mesaj)
        assertTrue(k2.mesaj!!.contains("Sen \"10 gelir\" adımını yapmamışsın."), k2.mesaj)

        // Doğru yapınca sonuç sayfası; orada Kontrol Et "Derse dön": kapatır.
        val k3 = o.kontrolEt(471, listOf(468, 478, 471))
        assertTrue(k3.dogru)
        assertEquals(4, k3.sonrakiSayfa)
        o.ac(4)
        assertFalse(o.adimSayfasi())
        assertEquals("review_step_result()", o.baslik())
        assertEquals(471, o.sayfaDegeri())
        assertEquals(
            "review_done(248 + 223|471)review_done_tip(birler|\"10 gelir\", \"büyük kardeşi 7 gider\")",
            o.mesajlar().single().metin,
        )
        assertTrue(o.kontrolEt(471, emptyList()).kapat)
        // Önceki sayfaya dönüş: mesajlar korunur, yeni ilk mesaj eklenmez.
        assertNull(o.git(3))
        assertEquals(3, o.mesajlar().size)
        assertTrue(o.ileriVar())
    }

    @Test
    fun teshissiz_ilk_yanlis_hata_burada() {
        // Klavyeyle cevaplanmış gibi (kayıt yok): teşhis yok, ilk yanlış adımda "hata burada".
        val o = oturum(listOf(3, 4), 8)
        assertNull(o.teshis)
        val ilk = o.baslat(0)!!
        assertEquals("review_intro_question(3 + 4|8|7)\n\nreview_intro_unknown()\n\nreview_step_write(3)", ilk)
        assertEquals(3, o.sayfaSayisi)
        o.kontrolEt(3, listOf(0, 3))
        o.ac(1)
        val k = o.kontrolEt(8, listOf(3, 8))
        assertTrue(k.mesaj!!.startsWith("review_found_now() review_wrong("), k.mesaj)
        // İkinci yanlışta önek yok.
        val k2 = o.kontrolEt(2, listOf(3, 2))
        assertTrue(k2.mesaj!!.startsWith("review_wrong("), k2.mesaj)
        // Yazma adımında yanlış: sütun teşhisi.
        o.git(0)
        val k3 = o.kontrolEt(4, listOf(0, 4))
        assertEquals("review_wrong(Birler basamağına 3 yazılmalı, sende 4 var.)", k3.mesaj)
    }

    @Test
    fun balonlar_parcalar_ve_gosterim() {
        val o = oturum(listOf(248, 223), 461, log248)
        o.baslat(0)
        // Yazma adımı: yalnızca "Abaküste göster" (sıfırdan 248 yazılır).
        assertEquals(listOf("sobi_suggest_show_abacus()"), o.balonlar().map { it.etiket })
        assertEquals(SobiGosterim(0, true, 248), o.balonaDokun(0).gosterim)
        // Onlar adımı (5'lik): göster + iki parça + "Neden bu kural?".
        o.ac(2)
        val b = o.balonlar()
        assertEquals(
            listOf("sobi_suggest_show_abacus()", "“5 gelir” ne demek?", "“kardeşi 3 gider” ne demek?", "review_why_rule()"),
            b.map { it.etiket },
        )
        assertTrue(b.none { it.ucretli })
        assertEquals(SobiGosterim(448, true, 20), o.gosterim())
        val sonuc = o.balonaDokun(3)
        assertEquals(SubStepExplainer.whyRule(StepExplainer.explain(448, HomeworkOp.ADD, 20)!!.moves.single()), sonuc.mesaj)
        val m = o.mesajlar()
        assertEquals(3, m.size)
        assertTrue(m[1].cocuktan)
        assertEquals("review_why_rule()", m[1].metin)
        assertFalse(m[2].cocuktan)
        // Geçersiz kod.
        assertNull(o.balonaDokun(99).mesaj)
    }

    @Test
    fun sonuc_sayfasinda_ornek_secimi_ve_acilisi() {
        val o = oturum(listOf(248, 223), 461, log248)
        o.baslat(0)
        assertEquals("add_ten", o.ornekKurali)
        assertEquals(4, o.ornekDuzeyi)
        o.ac(4)
        val b = o.balonlar()
        assertEquals(listOf("sobi_screen_another_example()", "sobi_screen_harder_example()"), b.map { it.etiket })
        assertTrue(b.all { it.ucretli })
        val h = o.balonaDokun(1).ornek!!
        assertEquals(5, h.duzey)
        assertTrue(h.ilkSayi in 1000..9999)
        // Ödeme başarılı: çocuğun balonu sonuç sayfasında, örnek yeni grupta açılır.
        val ilk = o.ornegiAc(h)!!
        assertEquals(5, o.ornekDuzeyi)
        assertEquals(5, o.sayfa)
        assertEquals(h.ilkSayi, o.sayfaDegeri())
        assertTrue(ilk.startsWith("sobi_screen_example_level(5|5) sobi_screen_example_intro("), ilk)
        // En zor düzeyde "Daha zor bir örnek" yok.
        o.ac(o.sayfaSayisi - 1)
        assertEquals("sobi_screen_example_title()", o.baslik())
        assertEquals(listOf("sobi_screen_another_example()"), o.balonlar().map { it.etiket })
        val q = SobiAnalizAdimlari.questionText(listOf(h.ilkSayi, h.ikinciSayi))
        assertEquals("sobi_screen_example_done($q|${h.ilkSayi + h.ikinciSayi})", o.mesajlar().single().metin)
        o.git(4)
        assertEquals("sobi_suggest_example()", o.mesajlar().last().metin)
        assertTrue(o.mesajlar().last().cocuktan)
    }

    @Test
    fun cikarma_10_luk_kural_ornegi() {
        // 41 - 3: çocuk 10'u götürdü, kardeşi 7'yi getirmedi (31).
        val o = oturum(listOf(41, -3), 31, listOf(0, 10, 20, 30, 40, 41, 31))
        assertEquals(MistakeAnalyzer.Kind.MISSING_STEP, o.teshis!!.kind)
        assertEquals("sub_ten", o.ornekKurali)
        assertEquals(2, o.ornekDuzeyi)
        o.baslat(0)
        assertEquals(3, o.sayfaSayisi)
        o.ac(1)
        assertEquals(
            "review_was_here()\n\nreview_step_sub(Birler|3|10'luk kural|10 gider, kardeşi 7 gelir)",
            o.mesajlar().single().metin,
        )
        assertEquals(SobiGosterim(41, false, 3), o.gosterim())
        assertEquals(
            listOf("sobi_suggest_show_abacus()", "“10 gider” ne demek?", "“kardeşi 7 gelir” ne demek?", "review_why_rule()"),
            o.balonlar().map { it.etiket },
        )
    }

    @Test
    fun flas_dizisinde_yalnizca_hatali_islem() {
        val log = listOf(0, 50, 50, 60, 70, 75, 76, 77, 78, 78, 88, 86)
        val o = oturum(listOf(50, 28, 9), 86, log, listOf(0, 2, 9))
        assertNull(o.teshis)
        assertEquals(1, o.diziHatalari.size)
        val ilk = o.baslat(0)!!
        assertEquals(2, o.sayfaSayisi)
        assertEquals("review_step_label_number(1|1|78 + 9)", o.baslik())
        assertEquals(78, o.sayfaDegeri())
        val teshis = o.diziHatalari[0].diagnosis!!.text
        assertEquals(
            "review_intro_sequence(1|10'luk kuralda 1 hata)\n\n" +
                "review_mistake_add(1|78|9|86|87) $teshis\n\n" +
                "review_step_add(Birler|9|10'luk kural|10 gelir, büyük kardeşi 1 gider)",
            ilk,
        )
        val k = o.kontrolEt(87, listOf(78, 88, 87))
        assertTrue(k.dogru)
        o.ac(k.sonrakiSayfa)
        assertEquals("review_done_sequence(87)", o.mesajlar().single().metin)
    }

    @Test
    fun flas_dizisi_kayitsiz_butun_dizi() {
        // Kayıt yoksa bütün dizi: yazma + her sayının basamakları; etiket "+28", "−9".
        val o = oturum(listOf(50, 28, -9), 70)
        o.baslat(0)
        // 50 yaz; +28: onlar +2, birler +8; −9: birler 10'luk.
        assertEquals(5, o.sayfaSayisi)
        o.ac(1)
        assertEquals("review_step_label_number(2|4|+28)", o.baslik())
        o.ac(3)
        assertEquals("review_step_label_number(4|4|−9)", o.baslik())
        // Eldenin doğrudan −1'i kural sayılmaz.
        assertEquals(listOf("add_direct", "sub_ten"), o.kurallar())
    }

    @Test
    fun adimsiz_soruda_bilgi_sayfasi() {
        val o = oturum(listOf(99_999, 1), 0)
        val ilk = o.baslat(7)
        assertEquals("review_intro_unknown()", ilk)
        assertEquals(1, o.sayfaSayisi)
        assertFalse(o.adimSayfasi())
        assertEquals(7, o.sayfaDegeri())
        assertNull(o.ornekKurali)
        assertTrue(o.balonlar().isEmpty())
        assertTrue(o.kontrolEt(0, emptyList()).kapat)
        assertNull(o.gosterim())
    }

    @Test
    fun odeme_basarisiz_mesaji() {
        val o = oturum(listOf(3, 4), 8)
        o.baslat(0)
        o.ac(2)
        assertEquals("sobi_error_network()", o.ornekOdenemedi())
        assertNotNull(o.mesajlar().lastOrNull { it.metin == "sobi_error_network()" })
    }
}
