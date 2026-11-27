// KAYNAK: app/src/main/java/com/example/app/model/GuidePanelData.kt (tamamı)
//         + app/src/main/java/com/example/app/MapFragment.kt → showGuidePanel() içindeki rehber verisi.
//
// Android'den bilinçli fark: `emote: BunnyMascotView.Emote` (Sobi maskotunun hâli, enum) → enum ADINI taşıyan String
// ("TEACH_TALK", "TEACH_POINT"). Eskiden `imageResId: Int` (R.drawable kimliği) idi; iOS tarafı enum adını kendi Sobi
// animasyon/sahnesine eşler.
package com.sorobit.shared.ders

data class GuidePanelData(
    /** Sobi'nin hâli: `BunnyMascotView.Emote` enum adı (Android: `emote`). */
    val emote: String,
    val text: String,
)

/** Ünite maratonu rehber panelinin adımları (MapFragment.showGuidePanel ile aynı sıra ve metinler). */
fun marathonGuidePanelData(): List<GuidePanelData> = listOf(
    GuidePanelData(
        emote = "TEACH_TALK",
        text = "Ünite maratonunda hatasız en hızlı çözümler kaydedilir.",
    ),
    GuidePanelData(
        emote = "TEACH_POINT",
        text = "Ünite maratonu panelindeki Rekor'a tıklayarak liderlik tablosunu ve kendi sıranı görebilirsin.",
    ),
)
