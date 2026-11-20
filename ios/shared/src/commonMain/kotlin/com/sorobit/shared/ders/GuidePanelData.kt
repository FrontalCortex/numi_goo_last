// KAYNAK: app/src/main/java/com/example/app/model/GuidePanelData.kt (tamamı)
//         + app/src/main/java/com/example/app/MapFragment.kt → showGuidePanel() içindeki rehber verisi.
//
// Android'den bilinçli fark: `imageResId: Int` (R.drawable kimliği) → kaynak adını taşıyan String.
// Alan adı eşleştirme kolay olsun diye aynı bırakıldı.
package com.sorobit.shared.ders

data class GuidePanelData(
    /** Görsel kaynağının adı (Android: R.drawable.x → "x"). */
    val imageResId: String,
    val text: String,
)

/** Ünite maratonu rehber panelinin adımları (MapFragment.showGuidePanel ile aynı sıra ve metinler). */
fun marathonGuidePanelData(): List<GuidePanelData> = listOf(
    GuidePanelData(
        imageResId = "teacher_emotes_gpt4",
        text = "Ünite maratonunda hatasız en hızlı çözümler kaydedilir.",
    ),
    GuidePanelData(
        imageResId = "teacher_emotes_gpt3",
        text = "Ünite maratonu panelindeki Rekor'a tıklayarak liderlik tablosunu ve kendi sıranı görebilirsin.",
    ),
)
