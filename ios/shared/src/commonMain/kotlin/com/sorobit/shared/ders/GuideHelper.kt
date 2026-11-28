// KAYNAK (app/src/main/java/com/example/app/):
//  - GuideHelper.kt: GuideContent, SharedGuideHelper.getGuideContentsForNumber (sayfa tablosu birebir),
//    GuideShowTracker (MAX_SHOW_COUNT = 2)
//  - AbacusFragment.kt: setupGuidePanel (abacusGuideNumber + currentStep == 1 + rehber 6 sınırı),
//    optimizeBeadIdsForReverse
//  - BlindingLessonFragment.kt: initializeGuideSystem / startNormalGuide / startGuideNumber
//    (görünen "sayıyı geç" düğmesi + gösterilmemiş öğretici → rehber 5 önceliği)
//
// Android'den bilinçli farklar:
//  - `emote: BunnyMascotView.Emote` → enum ADI (String), GuidePanelData ile aynı yaklaşım.
//  - `text = appString(R.string.x)` → metin anahtarı [GuideContent.textKey] ("x"); metni ekran çözer.
//  - `View` hedefleri → [GuideTarget]; `onContentShown`, ses ve yazma efekti alanları (hiçbir sayfada
//    kullanılmıyor) taşınmadı.
//  - Renkler `Color.parseColor`/`Color.YELLOW` yerine 0xRRGGBB (alfa her zaman tam).
//  - GuideShowTracker'ın SharedPreferences sayacı burada yok; gösterim sayısını çağıran verir.
package com.sorobit.shared.ders

/** Rehberin işaret ettiği ekran öğesi (Android: getGuideContentsForNumber'ın View parametreleri). */
enum class GuideTarget {
    FIRST_NUMBER_TEXT,
    SECOND_NUMBER_TEXT,
    KONTROL_BUTTON,
    RULES_PANEL_BUTTON,
    RULES_BOOK_BUTTON,
    SKIP_STEP_BUTTON,
    ABACUS_MODE_BUTTON,
}

/** Rehber panelinin bir sayfası (Android: GuideContent). */
data class GuideContent(
    /** BunnyMascotView.Emote adı ("TEACH_TALK", "TEACH_POINT", "TEACH_WARN"). */
    val emote: String,
    /** strings.xml anahtarı. */
    val textKey: String,
    val bubbleAnimationTarget: GuideTarget? = null,
    /** 0xRRGGBB; null: vurgu rengi yok. */
    val bubbleAnimationColor: Int? = null,
    /** 0xRRGGBB; null: açık ton yok. */
    val bubbleAnimationTintLight: Int? = null,
    val bubbleAnimationMaxScale: Float = 1.4f,
    val beadIds: List<String>? = null,
    val backBeadIds: List<String>? = null,
    val finishBeadIds: List<String>? = null,
    val requiredClickTarget: GuideTarget? = null,
    val requiredClickAdvancesGuide: Boolean = false,
    val waitForRulesTableSelection: Boolean = false,
)

object GuideHelper {
    /** GuideShowTracker.MAX_SHOW_COUNT. */
    const val MAX_SHOW_COUNT: Int = 2

    /** Gösterim sınırı olan tek rehber (abaküs boyutu ipucu). */
    const val LIMITED_GUIDE_NUMBER: Int = 6

    /** Kör derste "sayıyı geç" oku rehberi. */
    const val SKIP_STEP_GUIDE_NUMBER: Int = 5

    /** Color.YELLOW. */
    private const val YELLOW = 0xFFFF00
    private const val RULES_GREEN = 0x8BC34A
    private const val RULES_GREEN_LIGHT = 0xDFF0D4
    private const val ARROW_DARK = 0x263C31
    private const val ARROW_LIGHT = 0x00FF8C

    /** SharedGuideHelper.getGuideContentsForNumber; bilinmeyen numara → boş liste. */
    fun getGuideContentsForNumber(guideNumber: Int): List<GuideContent> = when (guideNumber) {
        // Toplama rehberi: Sobi anlatıyor; yazı düz geliyor (yazma efekti ve ses yok).
        1 -> listOf(
            GuideContent(
                emote = "TEACH_TALK",
                textKey = "ui_bu_testte_toplama_islemini_abakuste_yapacaks",
            ),
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_once_abakuse_ilk_sayiyi_yazip",
                bubbleAnimationTarget = GuideTarget.FIRST_NUMBER_TEXT,
                bubbleAnimationColor = YELLOW,
                beadIds = listOf("rod4BottomBead4"),
                backBeadIds = listOf("rod4BottomBead1"),
            ),
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_sonrasinda_ikinci_sayiyi_ekle",
                bubbleAnimationTarget = GuideTarget.SECOND_NUMBER_TEXT,
                bubbleAnimationColor = YELLOW,
                beadIds = listOf("rod4TopBead"),
            ),
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_ve_islem_bitince_kontrol_et_butonuna",
                bubbleAnimationTarget = GuideTarget.KONTROL_BUTTON,
                bubbleAnimationMaxScale = 1.1f,
            ),
            GuideContent(
                emote = "TEACH_WARN",
                textKey = "ui_sakin_aklindan_toplayip_o_sayiyi_abakuse",
                finishBeadIds = listOf("rod4BottomBead1", "rod4TopBead"),
            ),
        )
        // Sihirli değnek / kural tablosu rehberi.
        2 -> listOf(
            GuideContent(
                emote = "TEACH_TALK",
                textKey = "ui_kurallari_unutursan_sagdaki_sihirli_degneye",
            ),
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_burada_derste_ogrendigin_sayilar_ve_kardesle",
                bubbleAnimationTarget = GuideTarget.RULES_PANEL_BUTTON,
                bubbleAnimationMaxScale = 1.1f,
                bubbleAnimationColor = RULES_GREEN,
                bubbleAnimationTintLight = RULES_GREEN_LIGHT,
                requiredClickTarget = GuideTarget.RULES_PANEL_BUTTON,
            ),
        )
        // Kurallar kitabı rehberi.
        3 -> listOf(
            GuideContent(
                emote = "TEACH_TALK",
                textKey = "ui_kurallari_unutursan_sag_ustteki_kitaba_tikla",
            ),
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_burada_ogrendigin_kurallar_yer_alir",
                bubbleAnimationTarget = GuideTarget.RULES_BOOK_BUTTON,
                bubbleAnimationColor = RULES_GREEN,
                bubbleAnimationTintLight = RULES_GREEN_LIGHT,
                bubbleAnimationMaxScale = 1.1f,
                requiredClickTarget = GuideTarget.RULES_BOOK_BUTTON,
            ),
        )
        // Kural tablosunu abaküse alma rehberi.
        4 -> listOf(
            GuideContent(
                emote = "TEACH_TALK",
                textKey = "ui_kurallar_kitabina_tikladiginda_ekrana_gelen",
            ),
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_kurallar_kitabina_tikla",
                bubbleAnimationTarget = GuideTarget.RULES_BOOK_BUTTON,
                bubbleAnimationMaxScale = 1.1f,
                bubbleAnimationColor = RULES_GREEN,
                bubbleAnimationTintLight = RULES_GREEN_LIGHT,
                requiredClickTarget = GuideTarget.RULES_BOOK_BUTTON,
                requiredClickAdvancesGuide = true,
            ),
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_ekrana_gelen_kurallardan_herhangi_birisini_s",
                waitForRulesTableSelection = true,
            ),
        )
        // Sayıyı geçme oku rehberi (kör ders).
        5 -> listOf(
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_eger_tahtada_gosterilen_sayiyi_hemen_gecmek",
                bubbleAnimationTarget = GuideTarget.SKIP_STEP_BUTTON,
                bubbleAnimationColor = ARROW_DARK,
                bubbleAnimationTintLight = ARROW_LIGHT,
                bubbleAnimationMaxScale = 1.1f,
            ),
            GuideContent(
                emote = "TEACH_TALK",
                textKey = "ui_bu_butona_tahtada_bir_sayi_varken",
            ),
        )
        // Abaküs boyutu rehberi.
        6 -> listOf(
            GuideContent(
                emote = "TEACH_POINT",
                textKey = "ui_abakusun_boyutunu_sag_alttaki_olcekleme_buto",
                bubbleAnimationTarget = GuideTarget.ABACUS_MODE_BUTTON,
                bubbleAnimationColor = ARROW_DARK,
                bubbleAnimationTintLight = ARROW_LIGHT,
                bubbleAnimationMaxScale = 1.1f,
                requiredClickTarget = GuideTarget.ABACUS_MODE_BUTTON,
            ),
        )
        else -> emptyList()
    }

    /**
     * Fragment'lardaki `guideNumber == 6 && !GuideShowTracker.canShow(...)` denetimi: yalnızca rehber 6
     * [MAX_SHOW_COUNT] kez gösterilir. [guide6ShownCount]: "guide_shown_count_6" sayacı.
     */
    fun canShow(guideNumber: Int, guide6ShownCount: Int): Boolean =
        guideNumber != LIMITED_GUIDE_NUMBER || guide6ShownCount < MAX_SHOW_COUNT

    /** Panel gösterilince GuideShowTracker.recordShown çağrılır mı (yalnızca rehber 6). */
    fun recordsShown(guideNumber: Int): Boolean = guideNumber == LIMITED_GUIDE_NUMBER

    /**
     * AbacusFragment.setupGuidePanel: abacusGuideNumber yoksa ya da currentStep != 1 ise rehber yok;
     * rehber 6 sınırı aşıldıysa ya da sayfa listesi boşsa yok. Açılacak rehber numarası ya da null.
     */
    fun abacusLessonGuideNumber(abacusGuideNumber: Int?, currentStep: Int, guide6ShownCount: Int): Int? {
        val guideNumber = abacusGuideNumber ?: return null
        if (currentStep != 1) return null
        if (!canShow(guideNumber, guide6ShownCount)) return null
        if (getGuideContentsForNumber(guideNumber).isEmpty()) return null
        return guideNumber
    }

    /**
     * BlindingLessonFragment.initializeGuideSystem: "sayıyı geç" düğmesi görünüyorsa ve öğretici
     * gösterilmediyse rehber 5 (startGuideNumber(5)); değilse startNormalGuide (abacusGuideNumber yok ya da
     * 5 ise rehber yok; yalnızca currentStep == 1). Ardından startGuideNumber'ın rehber 6 sınırı.
     * Sonuç 5 ise atlama öğreticisi yolundan gelinmiştir (normal yol 5'i açmaz).
     */
    fun blindingLessonGuideNumber(
        abacusGuideNumber: Int?,
        currentStep: Int,
        skipStepButtonVisible: Boolean,
        skipStepTutorialShown: Boolean,
        guide6ShownCount: Int,
    ): Int? {
        val guideNumber: Int = if (skipStepButtonVisible && !skipStepTutorialShown) {
            SKIP_STEP_GUIDE_NUMBER
        } else {
            val normal = abacusGuideNumber ?: return null
            if (normal == SKIP_STEP_GUIDE_NUMBER) return null
            if (currentStep != 1) return null
            normal
        }
        if (!canShow(guideNumber, guide6ShownCount)) return null
        if (getGuideContentsForNumber(guideNumber).isEmpty()) return null
        return guideNumber
    }

    /**
     * AbacusFragment/BlindingLessonFragment.optimizeBeadIdsForReverse: aynı çubuğun alt boncuklarından
     * yalnızca en yüksek numaralısı kalır (o zaten bütün boncukları hareket ettirir); üst boncuklar ve
     * diğerleri önce, aynı sırayla; çubuk grupları ilk görüldükleri sırayla sona eklenir.
     */
    fun optimizeBeadIdsForReverse(beadIds: List<String>): List<String> {
        val optimized = mutableListOf<String>()
        val rodGroups = linkedMapOf<String, MutableList<String>>()
        beadIds.forEach { beadId ->
            val rod = (0..4).firstOrNull { beadId.startsWith("rod${it}BottomBead") }
            if (rod != null) {
                rodGroups.getOrPut("rod$rod") { mutableListOf() }.add(beadId)
            } else {
                optimized.add(beadId)
            }
        }
        rodGroups.forEach { (_, beads) ->
            if (beads.isNotEmpty()) {
                val sortedBeads = beads.sortedByDescending {
                    when {
                        it.contains("Bead4") -> 4
                        it.contains("Bead3") -> 3
                        it.contains("Bead2") -> 2
                        it.contains("Bead1") -> 1
                        else -> 0
                    }
                }
                optimized.add(sortedBeads.first())
            }
        }
        return optimized
    }
}
