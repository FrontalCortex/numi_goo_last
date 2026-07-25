package com.example.app

import android.view.View
import android.graphics.Color

data class GuideContent(
    val imageResource: Int,
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
            1 -> listOf(
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt4,
                    text = "Bu testte toplama işlemini abaküste yapacaksın.",
                    onContentShown = { },
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide1_0,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_stick,
                    text = "Önce abaküse ilk sayıyı yazıp...",
                    onContentShown = { },
                    bubbleAnimationTarget = firstNumberText,
                    bubbleAnimationColor = Color.YELLOW,
                    beadIds = listOf("rod4BottomBead4"),
                    backBeadIds = listOf("rod4BottomBead1"),
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide1_1,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_stick,
                    text = "Sonrasında ikinci sayıyı ekle.",
                    onContentShown = { },
                    bubbleAnimationTarget = secondNumberText,
                    bubbleAnimationColor = Color.YELLOW,
                    beadIds = listOf("rod4TopBead"),
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide1_2,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_stick,
                    text = "ve işlem bitince kontrol et butonuna tıkla.",
                    bubbleAnimationTarget = kontrolButton,
                    bubbleAnimationMaxScale = 1.1F,
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide1_3,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt3,
                    text = "Sakın aklından toplayıp o sayıyı abaküse yazma. O şekilde öğrenemezsin.",
                    finishBeadIds = listOf("rod4BottomBead1","rod4TopBead"),
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide1_4,
                )
            )
            2 -> listOf(
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt4,
                    text = "Kuralları unutursan sağdaki sihirli değneye tıklayarak kural tablosunu açabilirsin.",
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide2_0,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_stick,
                    text = "Burada derste öğrendiğin sayılar ve kardeşleri gösterilir.",
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide2_1,
                    bubbleAnimationTarget = rulesPanelButton,
                    bubbleAnimationMaxScale = 1.1F,
                    bubbleAnimationColor = Color.parseColor("#8BC34A"),
                    bubbleAnimationTintLight = Color.parseColor("#DFF0D4"),
                    requiredClickTarget = rulesPanelButton,
                )
            )
            3 -> listOf(
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt4,
                    text = "Kuralları unutursan sağ üstteki kitaba tıklayarak kurallar kitabına gidebilirsin.",
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide3_0,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_stick,
                    text = "Burada öğrendiğin kurallar yer alır.",
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide3_1,
                    bubbleAnimationTarget = rulesBookButton,
                    bubbleAnimationColor = Color.parseColor("#8BC34A"),
                    bubbleAnimationTintLight = Color.parseColor("#DFF0D4"),
                    bubbleAnimationMaxScale = 1.1F,
                    requiredClickTarget = rulesBookButton,
                )
            )
            4 -> listOf(
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt4,
                    text = "Kurallar kitabına tıkladığında ekrana gelen kurallardan herhangi birisine tıklayarak tabloyu abaküsün üstüne alabilirsin.",
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide4_0,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt4,
                    text = "Kurallar kitabına tıkla.",
                    bubbleAnimationTarget = rulesBookButton,
                    bubbleAnimationMaxScale = 1.1F,
                    bubbleAnimationColor = Color.parseColor("#8BC34A"),
                    bubbleAnimationTintLight = Color.parseColor("#DFF0D4"),
                    requiredClickTarget = rulesBookButton,
                    requiredClickAdvancesGuide = true,
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide4_1,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt4,
                    text = "Ekrana gelen kurallardan herhangi birisini seç.",
                    waitForRulesTableSelection = true,
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide4_2,
                )
            )
            5 -> listOf(
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_stick,
                    text = "Eğer tahtada gösterilen sayıyı hemen geçmek istersen, sağ altta bulunan ok butonuna tıklayabilirsin.",
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    bubbleAnimationTarget = skipStepButton,
                    bubbleAnimationColor = Color.parseColor("#263C31"),
                    bubbleAnimationTintLight = Color.parseColor("#00FF8C"),
                    bubbleAnimationMaxScale = 1.1F,
                    soundResource = R.raw.guide5_0,
                ),
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_gpt4,
                    text = "Bu butona tahtada bir sayı varken basarsan, doğrudan sıradaki sayıya geçersin.",
                    onContentShown = { },
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    soundResource = R.raw.guide5_1,
                )
            )
            6 -> listOf(
                GuideContent(
                    imageResource = R.drawable.teacher_emotes_stick,
                    text = "Abaküsün boyutunu sağ alttaki ölçekleme butonunu kullanarak özelleştirebilirsin.",
                    useTypewriterEffect = true,
                    typewriterSpeed = 40L,
                    bubbleAnimationTarget = abacusModeButton,
                    bubbleAnimationColor = Color.parseColor("#263C31"),
                    bubbleAnimationTintLight = Color.parseColor("#00FF8C"),
                    bubbleAnimationMaxScale = 1.1F,
                    requiredClickTarget = abacusModeButton,
                    soundResource = R.raw.guide6_0,
                ),
            )

            else -> emptyList()
        }
    }
}
