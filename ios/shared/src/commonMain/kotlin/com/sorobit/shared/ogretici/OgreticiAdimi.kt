// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt — `data class TutorialStep`
// (satır ~17581), `sealed class WidgetOperation` (~17544) ve `BeadAnimation` (model/BeadAnimation.kt).
//
// Android'deki adım tanımı görünümlere (binding.x, R.id.x, R.dimen.x, lambda) bağlıdır. Burada
// aynı bilgi yalın veriye çevrilmiştir; çeviri kuralı ios/tools/ogretici-donustur/donustur.js'tedir:
//   getString(R.string.x)            → "x" (strings.xml anahtarı; Swift'te Metin.al("x"))
//   R.raw.x                          → "x" (ses dosyası adı)
//   BeadAnimation(this, "rod4_bead_bottom1", 1) → OgreticiBoncukHareketi(4, false, 0, 1)
//   WidgetOperation.ChangeVisibility(binding.v, View.X) ve widgetVisibilityMap → OgreticiGorunurluk
//     (görünüm, fragment_tutorial.xml'deki android:id adıyla)
//   focusView üzerindeki AnimateMargin / ChangeMargin / AnimateSize / ChangeConstraints
//     → OgreticiOdakIslemi (dp cinsinden)
// İçerik `icerik/OgreticiN.kt` dosyalarındadır; kaynak, OgreticiKatalogu.
package com.sorobit.shared.ogretici

/** Sabitler. Swift: `OgreticiSabit.shared.GORUNUR` (Int32). */
object OgreticiSabit {
    // Görünürlük (Android View.VISIBLE / INVISIBLE / GONE ile aynı sayılar).
    const val GORUNUR = 0
    /** Yer kaplar ama çizilmez (View.INVISIBLE). */
    const val GORUNMEZ = 4
    /** Yer de kaplamaz (View.GONE). */
    const val YOK = 8
    /**
     * Yalnızca 1. öğreticinin "Eğitimi atla" düğmesi: hesapsız ilk açılışta GORUNMEZ, değilse GORUNUR
     * (Android: `if (isFirstRunTutorial) View.INVISIBLE else View.VISIBLE`).
     */
    const val ILK_ACILISTA_GORUNMEZ = 100

    // Odak çerçevesinin üst bağı (Android: focusView'in layout_constraintTop_toTopOf'u).
    /** abacusContainer'ın üstüne ve altına bağlı: dikeyde ortalı (fragment_tutorial.xml varsayılanı). */
    const val CAPA_ORTA = 0
    /** `R.id.guideline`: abaküs çerçevesinin üst kenarı. */
    const val CAPA_UST_KILAVUZ = 1
    /** `R.id.guideline2`: abaküs çerçevesinin %30 yüksekliği (layout_constraintVertical_bias="0.3"). */
    const val CAPA_ALT_KILAVUZ = 2

    /** Odak işleminde "değişmez" (alan verilmedi). */
    const val DEGISMEZ = -1f
    /** Odak boyunda "bir önceki boy" (Android: sizeHistory'den prevWidth / prevHeight). */
    const val ONCEKI = -2f

    /** Cevap / geri cevap / kimlik yokken sayı alanlarının değeri. */
    const val SAYI_YOK = -1

    // Boncuk animasyon türleri (Android BeadAnimation.animationType).
    /** Alt boncuk yukarı (bariyere). */
    const val HAREKET_ALT_YUKARI = 1
    /** Alt boncuk aşağı. */
    const val HAREKET_ALT_ASAGI = 2
    /** Üst boncuk aşağı (bariyere). */
    const val HAREKET_UST_ASAGI = 3
    /** Üst boncuk yukarı. */
    const val HAREKET_UST_YUKARI = 4
}

/**
 * Tek bir boncuğun animasyonu (Android: `BeadAnimation(this, "rod{cubuk}_bead_{top|bottom{sira+1}}", tur)`).
 * [cubuk] 0..4 (4 = birler, en sağ); [sira] alt boncukta 0..3 (0 = bariyere en yakın, "bottom1").
 */
class OgreticiBoncukHareketi(
    val cubuk: Int,
    val ust: Boolean,
    val sira: Int,
    val tur: Int,
) {
    /** Geri giderken oynatılan ters hareket (Android showStep: 1↔2, 3↔4). */
    fun ters(): OgreticiBoncukHareketi = OgreticiBoncukHareketi(cubuk, ust, sira, OgreticiKurallari.tersTur(tur))
}

/** Bir görünümün görünürlüğü. [gorunum]: fragment_tutorial.xml'deki android:id ("abacusLinear", "focusView" …). */
class OgreticiGorunurluk(
    val gorunum: String,
    val deger: Int,
)

/**
 * Odak çerçevesine (focusView) tek işlem. Verilmeyen alan [OgreticiSabit.DEGISMEZ] kalır.
 * Ölçüler dp; çerçeve abaküs kabının (abacusContainer, 273 × 290 dp) sağ kenarına göre yerleşir.
 */
class OgreticiOdakIslemi(
    /** Kabın sağ kenarından çerçevenin sağ kenarına (Android rightMargin; 0 → 19 dp, 1 px → 0). */
    val sagBoslukDp: Float = OgreticiSabit.DEGISMEZ,
    /** [OgreticiSabit.ONCEKI] olabilir. */
    val genislikDp: Float = OgreticiSabit.DEGISMEZ,
    /** [OgreticiSabit.ONCEKI] olabilir. */
    val yukseklikDp: Float = OgreticiSabit.DEGISMEZ,
    /** `CAPA_*`; -1 = değişmez. */
    val capa: Int = -1,
    /** Yalnızca bu adımda geçerli üst boşluk (Android showStep her adımda topMargin'i sıfırlar). */
    val ustBoslukDp: Float = OgreticiSabit.DEGISMEZ,
)

/** Soru metninde tek bir harfin rengi (Android questionTextColorPositions: konum to renk). [renk] ARGB. */
class OgreticiRenkliHarf(
    val konum: Int,
    val renk: Long,
)

/**
 * Android `TutorialStep`. Metin alanları strings.xml anahtarıdır (Swift: `Metin.al(anahtar)`); soru
 * metni ([soru]) Android'de de düz metindir ("1", "4 + 3").
 */
class OgreticiAdimi(
    /** text */
    val metin: String,
    /** soundResource (res/raw adı) */
    val ses: String? = null,
    /** soundStoragePath (gömülü ses yoksa Firebase Storage yolu) */
    val uzakSes: String? = null,
    /** useTypewriterEffect */
    val daktilo: Boolean = false,
    /** typewriterSpeed (harf başına ms) */
    val daktiloHiziMs: Long = 50L,
    /** animation */
    val hareketler: List<OgreticiBoncukHareketi> = emptyList(),
    /** widgetOperations (ChangeVisibility) + widgetVisibilityMap */
    val gorunurlukler: List<OgreticiGorunurluk> = emptyList(),
    /** widgetOperations (focusView yerleşimi) */
    val odak: List<OgreticiOdakIslemi> = emptyList(),
    /**
     * widgetOperations içindeki odak dışı ChangeConstraints, ham hâliyle: "görünüm:alan=değer;…"
     * (ör. 24. öğretici: "tutorialText:bottomToTop=blinding_tutorial1;bottomToBottom=UNSET").
     */
    val yerlesimler: List<String> = emptyList(),
    /** id (soru adımlarının değişmez ölçüm kimliği) */
    val kimlik: String? = null,
    /** questionText */
    val soru: String? = null,
    /** questionTextVisibility (Android varsayılanı INVISIBLE) */
    val soruGorunurlugu: Int = OgreticiSabit.GORUNMEZ,
    /** questionTextColorPositions */
    val soruRenkleri: List<OgreticiRenkliHarf> = emptyList(),
    /** nextStepAvailable: false ise ekrana dokunmak ilerletmez */
    val ilerlenebilir: Boolean = true,
    /** abacusClickable: kullanıcı abaküse sayı yazıp "Kontrol Et"e basar */
    val abakusTiklanabilir: Boolean = false,
    /** answerNumber */
    val cevap: Int = OgreticiSabit.SAYI_YOK,
    /** backAnswerNumber: bu adıma geri dönülünce (ya da yanlıştan sonra) abaküse yazılacak sayı */
    val geriCevap: Int = OgreticiSabit.SAYI_YOK,
    /** abacusReset: bu adıma geri dönülünce ya da yanlıştan sonra abaküs sıfırlanır */
    val abakusSifirla: Boolean = false,
    /** nextStepAbacusReset: bu adımdan ileri gidilince sıfırlanır */
    val sonrakindeSifirla: Boolean = false,
    /** resetAndWaith: bu adımdan geri gelinince önce sıfırla, 800 ms bekle, sonra göster */
    val sifirlaVeBekle: Boolean = false,
    /** onStepComplete { resetAbacus() } (tek kullanım: 14. öğretici) */
    val gelinceSifirla: Boolean = false,
    /** backAnimationOff (Android'de tanımlı ama hiçbir yerde okunmuyor) */
    val geriAnimasyonuYok: Boolean = false,
    /** requestText: yanlış cevap panelindeki ipucu */
    val ipucu: String? = null,
    /** rulesPanelVisibility: öğreticinin kural tablosu ([OgreticiIcerigi.kuralTablosu]) bu adımda */
    val kuralTablosuGorunurlugu: Int = OgreticiSabit.GORUNUR,
    /** rulesStepTextContent: "[köşeli parantez]" içi mavi boyanır (markedRuleText) */
    val kuralMetni: String? = null,
    /** rulesStepTextVisibility (null → GONE) */
    val kuralMetniGorunurlugu: Int = OgreticiSabit.YOK,
    /** rulesStepTextTopToBottomOf: kural metninin altına bağlandığı görünüm (android:id) */
    val kuralMetniCapasi: String? = null,
    /** options */
    val secenekler: List<String> = emptyList(),
    /** correctOptionIndex */
    val dogruSecenekler: List<Int> = emptyList(),
    /** multipleChoice */
    val cokluSecim: Boolean = false,
    /** optionText: seçenek panelinin başlığı */
    val secenekBasligi: String? = null,
) {
    /**
     * Android `hasValidOptions`: seçenek ve doğru şık listesi dolu, bütün doğru şıklar sınırlar içinde.
     * Koşul sağlanmazsa seçenek paneli açılmaz.
     */
    fun gecerliSecenekVar(): Boolean =
        secenekler.isNotEmpty() && dogruSecenekler.isNotEmpty() && dogruSecenekler.all { it in secenekler.indices }

    /** Soru metni görünür mü (GORUNUR). GORUNMEZ ise yer kaplar. */
    fun soruGorunur(): Boolean = soruGorunurlugu == OgreticiSabit.GORUNUR
}

/** Bir öğreticinin bütün içeriği. */
class OgreticiIcerigi(
    /** tutorialNumber */
    val numara: Int,
    /**
     * Bu öğreticide adımların rulesPanelVisibility'sinin uygulandığı kural tablosu (android:id) ya da
     * null (Android: showStep içindeki `when (tutorialNumber)`).
     */
    val kuralTablosu: String?,
    val adimlar: List<OgreticiAdimi>,
) {
    val adimSayisi: Int get() = adimlar.size

    /** Sınırlar dışında null (Swift'ten güvenli okuma). */
    fun adim(sira: Int): OgreticiAdimi? = adimlar.getOrNull(sira)
}
