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
                    text = "Bu testte toplama işlemini abaküste yapacaksın.",
                    onContentShown = { },
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Önce abaküse ilk sayıyı yazıp...",
                    onContentShown = { },
                    bubbleAnimationTarget = firstNumberText,
                    bubbleAnimationColor = Color.YELLOW,
                    beadIds = listOf("rod4BottomBead4"),
                    backBeadIds = listOf("rod4BottomBead1"),
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Sonrasında ikinci sayıyı ekle.",
                    onContentShown = { },
                    bubbleAnimationTarget = secondNumberText,
                    bubbleAnimationColor = Color.YELLOW,
                    beadIds = listOf("rod4TopBead"),
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "ve işlem bitince kontrol et butonuna tıkla.",
                    bubbleAnimationTarget = kontrolButton,
                    bubbleAnimationMaxScale = 1.1F,
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_WARN,
                    text = "Sakın aklından toplayıp o sayıyı abaküse yazma. O şekilde öğrenemezsin.",
                    finishBeadIds = listOf("rod4BottomBead1","rod4TopBead"),
                )
            )
            // Sihirli değnek / kural tablosu rehberi: Sobi anlatıyor; yazma efekti ve ses yok.
            2 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_TALK,
                    text = "Kuralları unutursan sağdaki sihirli değneye tıklayarak kural tablosunu açabilirsin.",
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Burada derste öğrendiğin sayılar ve kardeşleri gösterilir.",
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
                    text = "Kuralları unutursan sağ üstteki kitaba tıklayarak kurallar kitabına gidebilirsin.",
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Burada öğrendiğin kurallar yer alır.",
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
                    text = "Kurallar kitabına tıkladığında ekrana gelen kurallardan herhangi birisine tıklayarak tabloyu abaküsün üstüne alabilirsin.",
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Kurallar kitabına tıkla.",
                    bubbleAnimationTarget = rulesBookButton,
                    bubbleAnimationMaxScale = 1.1F,
                    bubbleAnimationColor = Color.parseColor("#8BC34A"),
                    bubbleAnimationTintLight = Color.parseColor("#DFF0D4"),
                    requiredClickTarget = rulesBookButton,
                    requiredClickAdvancesGuide = true,
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Ekrana gelen kurallardan herhangi birisini seç.",
                    waitForRulesTableSelection = true,
                )
            )
            // Sayıyı geçme oku rehberi: Sobi anlatıyor; yazma efekti ve ses yok.
            5 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Eğer tahtada gösterilen sayıyı hemen geçmek istersen, sağ altta bulunan ok butonuna tıklayabilirsin.",
                    bubbleAnimationTarget = skipStepButton,
                    bubbleAnimationColor = Color.parseColor("#263C31"),
                    bubbleAnimationTintLight = Color.parseColor("#00FF8C"),
                    bubbleAnimationMaxScale = 1.1F,
                ),
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_TALK,
                    text = "Bu butona tahtada bir sayı varken basarsan, doğrudan sıradaki sayıya geçersin.",
                    onContentShown = { },
                )
            )
            // Abaküs boyutu rehberi: Sobi gösteriyor; yazma efekti ve ses yok.
            6 -> listOf(
                GuideContent(
                    emote = BunnyMascotView.Emote.TEACH_POINT,
                    text = "Abaküsün boyutunu sağ alttaki ölçekleme butonunu kullanarak özelleştirebilirsin.",
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
