package com.example.app

import android.content.Context
import android.view.View
import android.graphics.Color

/** Rehber panelinin bir sayfası: Sobi'nin o sayfadaki hâli ([emote]) ve yazı. */
data class GuideContent(
    val emote: BunnyMascotView.Emote,
    val text: String,
    val onContentShown: (() -> Unit)? = null,
    val bubbleAnimationTarget: View? = null,
    val bubbleAnimationColor: Int? = null,
    val bubbleAnimationTintLight: Int? = null,
    val bubbleAnimationMaxScale: Float = 1.4f,
    val beadIds: List<String>? = null,
    val backBeadIds: List<String>? = null,
    val finishBeadIds: List<String>? = null,
    val requiredClickTarget: View? = null,
    val requiredClickAdvancesGuide: Boolean = false,
    val waitForRulesTableSelection: Boolean = false,
    val soundResource: Int? = null,
    val useTypewriterEffect: Boolean = false,
    val typewriterSpeed: Long = 40L,
    )

object SharedGuideHelper {
    fun getGuideContentsForNumber(
        guideNumber: Int,
        firstNumberText: View? = null,
        secondNumberText: View? = null,
        kontrolButton: View? = null,
        rulesPanelButton: View? = null,
        rulesBookButton: View? = null,
        skipStepButton: View? = null,
        abacusModeButton: View? = null
    ): List<GuideContent> {
        return when (guideNumber) {
            // Toplama rehberi: Sobi anlatıyor; yazı düz geliyor (yazma efekti ve ses yok).
            1 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_TALK,
                    text = appString(R.string.ui_bu_testte_toplama_islemini_abakuste_yapacaks),
                    onContentShown = { },
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_once_abakuse_ilk_sayiyi_yazip),
                    onContentShown = { },
                    bubbleAnimationTarget = firstNumberText,
                    bubbleAnimationColor = Color.YELLOW,
                    beadIds = listOf("rod4BottomBead4"),
                    backBeadIds = listOf("rod4BottomBead1"),
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_sonrasinda_ikinci_sayiyi_ekle),
                    onContentShown = { },
                    bubbleAnimationTarget = secondNumberText,
                    bubbleAnimationColor = Color.YELLOW,
                    beadIds = listOf("rod4TopBead"),
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_ve_islem_bitince_kontrol_et_butonuna),
                    bubbleAnimationTarget = kontrolButton,
                    bubbleAnimationMaxScale = 1.1F,
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_WARN,
                    text = appString(R.string.ui_sakin_aklindan_toplayip_o_sayiyi_abakuse),
                    finishBeadIds = listOf("rod4BottomBead1","rod4TopBead"),
                )
            )
            // Sihirli değnek / kural tablosu rehberi: Sobi anlatıyor; yazma efekti ve ses yok.
            2 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_TALK,
                    text = appString(R.string.ui_kurallari_unutursan_sagdaki_sihirli_degneye),
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_burada_derste_ogrendigin_sayilar_ve_kardesle),
                    bubbleAnimationTarget = rulesPanelButton,
                    bubbleAnimationMaxScale = 1.1F,
                    bubbleAnimationColor = Color.parseColor("#8BC34A"),
                    bubbleAnimationTintLight = Color.parseColor("#DFF0D4"),
                    requiredClickTarget = rulesPanelButton,
                )
            )
            // Kurallar kitabı rehberi: Sobi anlatıyor; yazma efekti ve ses yok.
            3 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_TALK,
                    text = appString(R.string.ui_kurallari_unutursan_sag_ustteki_kitaba_tikla),
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_burada_ogrendigin_kurallar_yer_alir),
                    bubbleAnimationTarget = rulesBookButton,
                    bubbleAnimationColor = Color.parseColor("#8BC34A"),
                    bubbleAnimationTintLight = Color.parseColor("#DFF0D4"),
                    bubbleAnimationMaxScale = 1.1F,
                    requiredClickTarget = rulesBookButton,
                )
            )
            // Kural tablosunu abaküse alma rehberi: Sobi anlatıyor; yazma efekti ve ses yok.
            4 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_TALK,
                    text = appString(R.string.ui_kurallar_kitabina_tikladiginda_ekrana_gelen),
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_kurallar_kitabina_tikla),
                    bubbleAnimationTarget = rulesBookButton,
                    bubbleAnimationMaxScale = 1.1F,
                    bubbleAnimationColor = Color.parseColor("#8BC34A"),
                    bubbleAnimationTintLight = Color.parseColor("#DFF0D4"),
                    requiredClickTarget = rulesBookButton,
                    requiredClickAdvancesGuide = true,
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_ekrana_gelen_kurallardan_herhangi_birisini_s),
                    waitForRulesTableSelection = true,
                )
            )
            // Sayıyı geçme oku rehberi: Sobi anlatıyor; yazma efekti ve ses yok.
            5 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_eger_tahtada_gosterilen_sayiyi_hemen_gecmek),
                    bubbleAnimationTarget = skipStepButton,
                    bubbleAnimationColor = Color.parseColor("#263C31"),
                    bubbleAnimationTintLight = Color.parseColor("#00FF8C"),
                    bubbleAnimationMaxScale = 1.1F,
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_TALK,
                    text = appString(R.string.ui_bu_butona_tahtada_bir_sayi_varken),
                    onContentShown = { },
                )
            )
            // Abaküs boyutu rehberi: Sobi gösteriyor; yazma efekti ve ses yok.
            6 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = appString(R.string.ui_abakusun_boyutunu_sag_alttaki_olcekleme_buto),
                    bubbleAnimationTarget = abacusModeButton,
                    bubbleAnimationColor = Color.parseColor("#263C31"),
                    bubbleAnimationTintLight = Color.parseColor("#00FF8C"),
                    bubbleAnimationMaxScale = 1.1F,
                    requiredClickTarget = abacusModeButton,
                ),
            )

            else -> emptyList()
        }
    }
}

/**
 * Belirli bir rehber (guide) numarasının kullanıcıya kaç kez gösterildiğini SharedPreferences'ta
 * tutar. Bazı rehberler (örn. guide 6: abaküs boyutu ölçekleme ipucu) her uygun ekran açıldığında
 * tekrar tetiklenebiliyor; [MAX_SHOW_COUNT] sınırıyla belirli bir sayıdan sonra bir daha gösterilmez.
 */
object GuideShowTracker {
    private const val PREFS_NAME = "AppPrefs"
    private const val MAX_SHOW_COUNT = 2

    private fun key(guideNumber: Int) = "guide_shown_count_$guideNumber"

    fun canShow(context: Context, guideNumber: Int): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(key(guideNumber), 0) < MAX_SHOW_COUNT
    }

    fun recordShown(context: Context, guideNumber: Int) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(key(guideNumber), 0)
        prefs.edit().putInt(key(guideNumber), current + 1).apply()
    }
}
