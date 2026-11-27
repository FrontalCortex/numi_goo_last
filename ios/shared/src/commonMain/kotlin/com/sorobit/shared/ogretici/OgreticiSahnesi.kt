// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt
//   showStep (satır ~607-822): görünürlük ve odak işlemlerinin uygulanması, kural tablosu `when`'i,
//     son adımda devametFragment (abaküs gizlenir, "Eğitimi atla" kayar, "Devam Et" gelir)
//   onViewCreated (~525-544): 24-26'da abaküs hiç görünmez; hesapsız ilk açılışta çıkış düğmesi gizli
//   setupBeads (~18696): abaküs yalnızca abacusClickable adımlarda dokunulur; "Kontrol Et" de
//     yalnızca o adımlarda görünür (diğerlerinde INVISIBLE, yer kaplar)
//
// BİLİNÇLİ FARK: Android işlemleri adım adım biriktirir ve geri giderken yalnızca bazılarını
// tersine çevirir (focusView görünürlüğü, AnimateMargin, sizeHistory). Burada bir adımın ekranı,
// baştan o adıma kadar bütün işlemler sırayla uygulanarak HESAPLANIR; ileri gidişte sonuç Android
// ile aynıdır, geri gidişte Android'in kaçırdığı durumlar (ör. 1. öğreticide 3. adımdan 2.'ye
// dönünce abaküsün gizlenmesi) da doğru çıkar.
package com.sorobit.shared.ogretici

/** Bir adımın ekranı: hangi parça görünür, odak çerçevesi nerede. Swift salt okur. */
class OgreticiSahnesi(
    val adim: Int,
    val sonAdim: Boolean,
    /** abacusLinear + abacusContainer */
    val abakusGorunurlugu: Int,
    /** false ise abaküse dokunulamaz (dokunuş ekrana gider ve adımı ilerletir: abacusTapBlocker). */
    val abakusKilitli: Boolean,
    val geriGorunurlugu: Int,
    val atlaGorunurlugu: Int,
    val cikisGorunur: Boolean,
    /** "Kontrol Et" (GORUNUR değilse GORUNMEZ: yer kaplar) */
    val kontrolGorunur: Boolean,
    /** Son adımda alttan gelen "Devam Et" */
    val devamGorunur: Boolean,
    val odakGorunur: Boolean,
    val odakSagBoslukDp: Float,
    val odakGenislikDp: Float,
    val odakYukseklikDp: Float,
    val odakCapa: Int,
    val odakUstBoslukDp: Float,
    /** Öğreticinin kural tablosunun bu adımdaki görünürlüğü; tablo yoksa YOK. */
    val kuralTablosuGorunurlugu: Int,
    private val gorunurlukler: Map<String, Int>,
) {
    /**
     * Görünürlüğü işlemlerle değişen herhangi bir görünüm (android:id). Hiç değişmediyse
     * [varsayilan] (fragment_tutorial.xml'deki değer) döner.
     */
    fun gorunurluk(gorunum: String, varsayilan: Int): Int = gorunurlukler[gorunum] ?: varsayilan

    /** Görünürlüğü bu adıma kadar en az bir kez değişen görünümler (android:id), sıralı. */
    fun degisenGorunumler(): List<String> = gorunurlukler.keys.sorted()
}

/** Saf hesaplar. */
object OgreticiKurallari {
    // fragment_tutorial.xml / dimens.xml varsayılanları (dp).
    const val ODAK_GENISLIK_DP = 54f
    const val ODAK_YUKSEKLIK_DP = 255f
    /** focusView layout_marginRight="0dp" */
    const val ODAK_SAG_BOSLUK_DP = 0f
    /** Kör ders anlatımları (24-26): abaküs hiç görünmez (onViewCreated). */
    private val ABAKUSSUZ = setOf(24, 25, 26)

    /** Android showStep geri dalı: tür 1↔2, 3↔4. */
    fun tersTur(tur: Int): Int = when (tur) {
        1 -> 2
        2 -> 1
        3 -> 4
        4 -> 3
        else -> tur
    }

    /**
     * [adim]'daki ekranı hesaplar. [ilkAcilis]: hesapsız ilk açılış (Android isFirstRunTutorial).
     * [atlaZorla]: geri düğmesi "Eğitimi atla"yı görünür yaptı (setupBackButton).
     */
    fun sahne(icerik: OgreticiIcerigi, adim: Int, ilkAcilis: Boolean, atlaZorla: Boolean): OgreticiSahnesi {
        val sinirli = adim.coerceIn(0, maxOf(0, icerik.adimSayisi - 1))
        val gorunurlukler = HashMap<String, Int>()
        var sag = ODAK_SAG_BOSLUK_DP
        var genislik = ODAK_GENISLIK_DP
        var yukseklik = ODAK_YUKSEKLIK_DP
        var capa = OgreticiSabit.CAPA_ORTA
        var ust = 0f
        val boyGecmisi = ArrayList<Pair<Float, Float>>()
        for (i in 0..sinirli) {
            val a = icerik.adim(i) ?: break
            for (g in a.gorunurlukler) gorunurlukler[g.gorunum] = cozGorunurluk(g.deger, ilkAcilis)
            // Android showStep her adımın başında focusView'in üst/alt boşluğunu sıfırlar.
            ust = 0f
            for (o in a.odak) {
                if (o.sagBoslukDp != OgreticiSabit.DEGISMEZ) sag = o.sagBoslukDp
                if (o.genislikDp != OgreticiSabit.DEGISMEZ || o.yukseklikDp != OgreticiSabit.DEGISMEZ) {
                    if (o.genislikDp == OgreticiSabit.ONCEKI || o.yukseklikDp == OgreticiSabit.ONCEKI) {
                        val onceki = boyGecmisi.removeLastOrNull()
                        if (onceki != null) {
                            if (o.genislikDp == OgreticiSabit.ONCEKI) genislik = onceki.first
                            if (o.yukseklikDp == OgreticiSabit.ONCEKI) yukseklik = onceki.second
                        }
                    } else {
                        boyGecmisi.add(genislik to yukseklik)
                    }
                    if (o.genislikDp >= 0f) genislik = o.genislikDp
                    if (o.yukseklikDp >= 0f) yukseklik = o.yukseklikDp
                }
                if (o.capa >= 0) capa = o.capa
                if (o.ustBoslukDp != OgreticiSabit.DEGISMEZ) ust = o.ustBoslukDp
            }
        }
        val son = sinirli == icerik.adimSayisi - 1
        val mevcut = icerik.adim(sinirli)
        val tiklanabilir = mevcut?.abakusTiklanabilir == true

        var abakus = gorunurlukler["abacusLinear"] ?: gorunurlukler["abacusContainer"] ?: OgreticiSabit.GORUNUR
        if (icerik.numara in ABAKUSSUZ || son) abakus = OgreticiSabit.GORUNMEZ

        var atla = gorunurlukler["skipTutorialButton"] ?: OgreticiSabit.GORUNUR
        if (atlaZorla && !ilkAcilis) atla = OgreticiSabit.GORUNUR
        if (son) atla = OgreticiSabit.GORUNMEZ

        val tablo = if (icerik.kuralTablosu != null && mevcut != null) mevcut.kuralTablosuGorunurlugu else OgreticiSabit.YOK
        return OgreticiSahnesi(
            adim = sinirli,
            sonAdim = son,
            abakusGorunurlugu = abakus,
            abakusKilitli = !tiklanabilir,
            geriGorunurlugu = gorunurlukler["backButton"] ?: OgreticiSabit.GORUNUR,
            atlaGorunurlugu = atla,
            // onViewCreated: tutorialNumber == 1 ve cihazda hiç hesap açılmadıysa quitButton INVISIBLE.
            cikisGorunur = !(icerik.numara == 1 && ilkAcilis),
            kontrolGorunur = tiklanabilir,
            devamGorunur = son,
            odakGorunur = gorunurlukler["focusView"] == OgreticiSabit.GORUNUR,
            odakSagBoslukDp = sag,
            odakGenislikDp = genislik,
            odakYukseklikDp = yukseklik,
            odakCapa = capa,
            odakUstBoslukDp = ust,
            kuralTablosuGorunurlugu = tablo,
            gorunurlukler = gorunurlukler,
        )
    }

    private fun cozGorunurluk(deger: Int, ilkAcilis: Boolean): Int =
        if (deger == OgreticiSabit.ILK_ACILISTA_GORUNMEZ) {
            if (ilkAcilis) OgreticiSabit.GORUNMEZ else OgreticiSabit.GORUNUR
        } else {
            deger
        }

    /**
     * Öğreticiyi baştan oynayıp [adim]'a İLERİ gelindiğinde abaküste duran sayı (ekran galerisi ve
     * ortadan açılış için). Her adımın boncuk hareketleri uygulanır; abaküslü soru adımından
     * çıkarken kullanıcının doğru cevabı yazdığı varsayılır; sonrakindeSifirla / gelinceSifirla 0'lar.
     * Hareketler soroban kuralıyla sayılır (alt boncuk yukarı: o boncuk ve altındakiler yukarıda).
     */
    fun baslangicDegeri(icerik: OgreticiIcerigi, adim: Int): Int {
        val alt = IntArray(5)
        val ustAsagi = BooleanArray(5)
        val hedef = adim.coerceIn(0, maxOf(0, icerik.adimSayisi - 1))
        for (i in 0 until hedef) {
            val a = icerik.adim(i) ?: break
            if (a.gelinceSifirla) sifirla(alt, ustAsagi)
            for (h in a.hareketler) uygula(h, alt, ustAsagi)
            if (a.abakusTiklanabilir && a.cevap >= 0) yaz(a.cevap, alt, ustAsagi)
            if (a.sonrakindeSifirla) sifirla(alt, ustAsagi)
        }
        if (icerik.adim(hedef)?.gelinceSifirla == true) sifirla(alt, ustAsagi)
        var deger = 0
        for (c in 0..4) deger = deger * 10 + alt[c] + if (ustAsagi[c]) 5 else 0
        return deger
    }

    private fun uygula(h: OgreticiBoncukHareketi, alt: IntArray, ustAsagi: BooleanArray) {
        val c = h.cubuk
        if (c !in 0..4) return
        when (h.tur) {
            OgreticiSabit.HAREKET_ALT_YUKARI -> alt[c] = maxOf(alt[c], h.sira + 1)
            OgreticiSabit.HAREKET_ALT_ASAGI -> alt[c] = minOf(alt[c], h.sira)
            OgreticiSabit.HAREKET_UST_ASAGI -> ustAsagi[c] = true
            OgreticiSabit.HAREKET_UST_YUKARI -> ustAsagi[c] = false
        }
    }

    private fun yaz(deger: Int, alt: IntArray, ustAsagi: BooleanArray) {
        val metin = deger.coerceIn(0, 99999).toString().padStart(5, '0')
        for (c in 0..4) {
            val rakam = metin[c].digitToInt()
            alt[c] = rakam % 5
            ustAsagi[c] = rakam >= 5
        }
    }

    private fun sifirla(alt: IntArray, ustAsagi: BooleanArray) {
        for (c in 0..4) {
            alt[c] = 0
            ustAsagi[c] = false
        }
    }
}
