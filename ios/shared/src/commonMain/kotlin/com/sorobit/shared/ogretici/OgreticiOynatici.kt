// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt — adım geçiş kuralları:
//   handleTutorialTapToAdvance (~561)          → dokunuldu
//   setupBackButton (~1220)                    → geri
//   showStep (~607; ileri / geri boncuk dalı)  → adimGoster
//   showResultPanel / stepAnswerAlgorithm (~17999, ~17624) → kontrol
//   checkOptionsAnswer (~17708)                → secenekKontrol
//   showResultPanelForOptions: continueButton (~18198) → dogruDevam
//   dismissIncorrectResultPanel → incorrectButtonClick (~18382, ~18488) → yanlisTamam
//
// Oynatıcı görünüm bilmez: her olay için sırayla yapılacak işlerin listesini ([OgreticiEtki])
// döndürür. Swift tarafı listeyi SIRAYLA yürütür; `bekle` işaretli işte (sayı yazma, bekleme)
// iş bitene kadar sıradakine geçmez.
//
// Ölçüm (AnalyticsLogger, TutorialStepAnalytics), Sobi dürtmeleri, öğretici sonu anketi
// (TutorialSurveyPolicy) ve "Ne zorladı?" anketi buraya alınmadı.
package com.sorobit.shared.ogretici

/** Yapılacak tek iş. Türler `OgreticiEtki.Companion` sabitleridir (Swift: `OgreticiEtki.companion.ADIM_GOSTER`). */
class OgreticiEtki(
    val tur: Int,
    /** ADIM_GOSTER: gösterilecek adım. */
    val adim: Int = -1,
    /** SAYI_YAZ / DEGER_KUR: sayı. SONUC_GOSTER: 1 doğru, 0 yanlış. */
    val deger: Int = 0,
    /** BEKLE: süre. */
    val gecikmeMs: Long = 0L,
    /** SAYI_YAZ: true ise yazma (animasyon) bitmeden sıradaki işe geçilmez. */
    val bekle: Boolean = false,
    /** ADIM_GOSTER: true ise ileri gidiliyor (daktilo, ses, boncuklar). */
    val ileri: Boolean = true,
    /** ADIM_GOSTER: oynatılacak boncuk hareketleri (geri giderken ters çevrilmiş olarak). */
    val hareketler: List<OgreticiBoncukHareketi> = emptyList(),
) {
    companion object {
        /** Adımı göster: metin (daktilo), ses, soru, sahne, boncuk hareketleri. */
        const val ADIM_GOSTER = 1
        /** Daktilo sürerken dokunuldu: metnin tamamını hemen göster. */
        const val METNI_TAMAMLA = 2
        /** Abaküsü animasyonla sıfırla (Android resetAbacus: 400 ms dokunma kilidi dahil). */
        const val ABAKUS_SIFIRLA = 3
        /** Abaküse [deger]'i animasyonla yaz (Android writeAnswerNumber). */
        const val SAYI_YAZ = 4
        /** Abaküse [deger]'i animasyonsuz yerleştir (ortadan açılış). */
        const val DEGER_KUR = 5
        /** [gecikmeMs] bekle. */
        const val BEKLE = 6
        /** Doğru (deger = 1) ya da yanlış (0) panelini aç; sesini çal. */
        const val SONUC_GOSTER = 7
        /** Açık sonuç panelini kapat. */
        const val SONUC_GIZLE = 8
        /** Öğreticiyi kapat, haritaya dön (Android closeFragment). */
        const val KAPAT = 9
        /** "Önce bir seçenek seç" uyarısı (Android Toast: tutorial_options_pick_first). */
        const val SECIM_UYARISI = 10
    }
}

/**
 * Bir öğreticinin oynatıcısı. [baslangicAdimi]: ekran galerisi ya da ortadan açılış için
 * (olağan açılışta 0).
 */
class OgreticiOynatici(
    val icerik: OgreticiIcerigi,
    val ilkAcilis: Boolean,
    baslangicAdimi: Int,
) {
    /** Android currentStep. */
    var adim: Int = baslangicAdimi.coerceIn(0, maxOf(0, icerik.adimSayisi - 1))
        private set

    /** Geri düğmesi "Eğitimi atla"yı göstermiş mi (setupBackButton; 1. öğretici hariç). */
    var atlaZorla: Boolean = false
        private set

    /** Açık sonuç paneli: -1 yok, 1 doğru, 0 yanlış. */
    var sonucPaneli: Int = -1
        private set

    val adimSayisi: Int get() = icerik.adimSayisi

    /** Boş içerik oynatılamaz (Swift tarafı yer tutucuya düşer). */
    val oynatilabilir: Boolean get() = icerik.adimSayisi > 0

    fun mevcutAdim(): OgreticiAdimi? = icerik.adim(adim)

    fun sahne(): OgreticiSahnesi = OgreticiKurallari.sahne(icerik, adim, ilkAcilis, atlaZorla)

    /** İlk gösterim. Ortadan açılışta abaküs önce o adıma gelinceki sayıya kurulur. */
    fun basla(): List<OgreticiEtki> {
        if (!oynatilabilir) return emptyList()
        val isler = ArrayList<OgreticiEtki>()
        if (adim > 0) {
            val deger = OgreticiKurallari.baslangicDegeri(icerik, adim)
            if (deger != 0) isler.add(OgreticiEtki(OgreticiEtki.DEGER_KUR, deger = deger))
        }
        isler.add(adimGoster(adim, ileri = true, animasyonlariAtla = false))
        return isler
    }

    /**
     * Ekrana (ya da kilitli abaküse) dokunuldu. [mesgul]: boncuk animasyonu, sıfırlama, sayı
     * yazma ya da sonuç paneli sürüyor (Android isAnyAnimationRunning + sonuç dokunma kilidi).
     */
    fun dokunuldu(mesgul: Boolean, daktiloSuruyor: Boolean): List<OgreticiEtki> {
        if (mesgul || sonucPaneli >= 0 || !oynatilabilir) return emptyList()
        if (daktiloSuruyor) return listOf(OgreticiEtki(OgreticiEtki.METNI_TAMAMLA))
        val mevcut = mevcutAdim() ?: return emptyList()
        if (mevcut.secenekler.isNotEmpty()) return emptyList()
        if (adim >= adimSayisi - 1 || !mevcut.ilerlenebilir) return emptyList()
        adim++
        val isler = ArrayList<OgreticiEtki>()
        val yeni = mevcutAdim()
        if (yeni?.gelinceSifirla == true) isler.add(OgreticiEtki(OgreticiEtki.ABAKUS_SIFIRLA))
        isler.add(adimGoster(adim, ileri = true, animasyonlariAtla = false))
        // Android sırası: önce yeni adım gösterilir, sonra önceki adımın nextStepAbacusReset'i.
        if (mevcut.sonrakindeSifirla) isler.add(OgreticiEtki(OgreticiEtki.ABAKUS_SIFIRLA))
        return isler
    }

    /** Geri düğmesi. [mesgul]: [dokunuldu] ile aynı; seçenek paneli kilitliyse de true verilir. */
    fun geri(mesgul: Boolean): List<OgreticiEtki> {
        if (mesgul || sonucPaneli >= 0 || !oynatilabilir) return emptyList()
        if (icerik.numara != 1) atlaZorla = true
        if (adim <= 0) return emptyList()
        adim--
        val inilen = mevcutAdim() ?: return emptyList()
        val sonraki = icerik.adim(adim + 1)
        // writeAnswerNumber ile mutlak durum kurulacaksa sonraki adımın hareketleri ters oynatılmaz.
        val hareketsiz = inilen.geriCevap >= 0
        val isler = ArrayList<OgreticiEtki>()
        if (inilen.geriCevap >= 0) isler.add(OgreticiEtki(OgreticiEtki.SAYI_YAZ, deger = inilen.geriCevap, bekle = false))
        if (inilen.abakusSifirla) isler.add(OgreticiEtki(OgreticiEtki.ABAKUS_SIFIRLA))
        if (sonraki?.sifirlaVeBekle == true) {
            isler.add(OgreticiEtki(OgreticiEtki.ABAKUS_SIFIRLA))
            isler.add(OgreticiEtki(OgreticiEtki.BEKLE, gecikmeMs = 800L))
        }
        isler.add(adimGoster(adim, ileri = false, animasyonlariAtla = hareketsiz))
        return isler
    }

    /** "Kontrol Et": abaküsteki [deger] adımın cevabıyla karşılaştırılır. */
    fun kontrol(deger: Int): List<OgreticiEtki> {
        val mevcut = mevcutAdim() ?: return emptyList()
        if (sonucPaneli >= 0 || !mevcut.abakusTiklanabilir) return emptyList()
        return sonucAc(deger == mevcut.cevap)
    }

    /** Seçenek panelindeki "Kontrol Et". [secilenler]: işaretli şıkların sırası. */
    fun secenekKontrol(secilenler: List<Int>): List<OgreticiEtki> {
        val mevcut = mevcutAdim() ?: return emptyList()
        if (sonucPaneli >= 0 || !mevcut.gecerliSecenekVar()) return emptyList()
        if (secilenler.isEmpty()) return listOf(OgreticiEtki(OgreticiEtki.SECIM_UYARISI))
        val secim = secilenler.toSet()
        val dogru = secim.size == mevcut.dogruSecenekler.size && secim.containsAll(mevcut.dogruSecenekler)
        return sonucAc(dogru)
    }

    private fun sonucAc(dogru: Boolean): List<OgreticiEtki> {
        sonucPaneli = if (dogru) 1 else 0
        return listOf(OgreticiEtki(OgreticiEtki.SONUC_GOSTER, deger = sonucPaneli))
    }

    /** Doğru panelindeki "Devam et". */
    fun dogruDevam(): List<OgreticiEtki> {
        if (sonucPaneli != 1) return emptyList()
        sonucPaneli = -1
        val ayrilan = mevcutAdim() ?: return listOf(OgreticiEtki(OgreticiEtki.SONUC_GIZLE))
        val isler = ArrayList<OgreticiEtki>()
        isler.add(OgreticiEtki(OgreticiEtki.SONUC_GIZLE))
        if (adim >= adimSayisi - 1) {
            // Android: son adım abaküslü bir soruysa currentStep dizinin dışına çıkar ve
            // devametFragment çağrılır. Burada adım son adımda kalır; sahne zaten "Devam Et"i gösterir.
            isler.add(adimGoster(adim, ileri = true, animasyonlariAtla = true))
            return isler
        }
        adim++
        if (ayrilan.sonrakindeSifirla) isler.add(OgreticiEtki(OgreticiEtki.ABAKUS_SIFIRLA))
        val yeni = mevcutAdim()
        if (yeni != null && !yeni.abakusTiklanabilir && yeni.gelinceSifirla) isler.add(OgreticiEtki(OgreticiEtki.ABAKUS_SIFIRLA))
        isler.add(adimGoster(adim, ileri = true, animasyonlariAtla = false))
        return isler
    }

    /** Yanlış panelindeki "Tekrar Dene" (ya da çarpı simgesi). */
    fun yanlisTamam(): List<OgreticiEtki> {
        if (sonucPaneli != 0) return emptyList()
        sonucPaneli = -1
        val isler = ArrayList<OgreticiEtki>()
        isler.add(OgreticiEtki(OgreticiEtki.SONUC_GIZLE))
        if (adim <= 0) {
            isler.add(OgreticiEtki(OgreticiEtki.KAPAT))
            return isler
        }
        val mevcut = mevcutAdim() ?: return isler
        val geriCevap = mevcut.geriCevap
        if (mevcut.abakusSifirla) isler.add(OgreticiEtki(OgreticiEtki.ABAKUS_SIFIRLA))
        if (geriCevap >= 0) {
            // Sıfırlama animasyonu sürerken yazılırsa boncuklar çakışır: önce 350 ms beklenir.
            if (mevcut.abakusSifirla) isler.add(OgreticiEtki(OgreticiEtki.BEKLE, gecikmeMs = 350L))
            isler.add(OgreticiEtki(OgreticiEtki.SAYI_YAZ, deger = geriCevap, bekle = true))
        }
        isler.add(adimGoster(adim, ileri = true, animasyonlariAtla = true))
        return isler
    }

    /**
     * Android showStep'in boncuk dalı: ileri giderken adımın kendi hareketleri; geri giderken
     * bir SONRAKİ adımın hareketleri ters çevrilmiş olarak.
     */
    private fun adimGoster(sira: Int, ileri: Boolean, animasyonlariAtla: Boolean): OgreticiEtki {
        val hareketler: List<OgreticiBoncukHareketi> = when {
            animasyonlariAtla -> emptyList()
            ileri -> icerik.adim(sira)?.hareketler ?: emptyList()
            else -> icerik.adim(sira + 1)?.hareketler?.map { it.ters() } ?: emptyList()
        }
        return OgreticiEtki(OgreticiEtki.ADIM_GOSTER, adim = sira, ileri = ileri, hareketler = hareketler)
    }
}
