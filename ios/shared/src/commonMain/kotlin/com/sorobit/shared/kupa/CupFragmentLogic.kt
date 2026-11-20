// Kaynak (Android): app/src/main/java/com/example/app/CupFragment.kt — görünümden bağımsız kısım:
//  - ChangeCupIcon(): puana göre kupa kademesi (ikon, ad, motivasyon metni havuzu)
//  - updateMapProgress(): stepCompletionStatus listesi ve "adım bitti mi" kararı
// R.drawable.x -> kaynak adı String'i. LessonItem güncellemesinin kendisi (copy/updateLessonItem) taşınmadı.
package com.sorobit.shared.kupa

import kotlin.random.Random

/** Kupa ekranında gösterilecek kademe. */
data class CupTier(
    /** Android `R.drawable.<ad>` kaynak adı ("cup_ic3", "cup_ic2", "cup_ic"). */
    val icon: String,
    val cupName: String,
    /** İçinden rastgele biri gösterilen motivasyon metinleri. */
    val cupMotivation: List<String>,
)

object CupFragmentLogic {

    /**
     * Android `ChangeCupIcon()` dalları (sıra önemli):
     * 1) cupPoint1 != null && toplamPuan >= cupPoint1 → Bilgelik Kupası (cup_ic3)
     * 2) cupPoint2 != null && toplamPuan >= cupPoint2 → Usta Kupası (cup_ic2)
     * 3) toplamPuan > 0 → Deneyim Kupası (cup_ic)
     * 4) aksi halde → Deneyim Kupası (cup_ic), iğneleyici metinler
     */
    fun resolveCupTier(toplamPuan: Int, cupPoint1: Int?, cupPoint2: Int?): CupTier {
        return if (cupPoint1 != null && toplamPuan >= cupPoint1) {
            CupTier(
                icon = "cup_ic3",
                cupName = "Bilgelik Kupası",
                cupMotivation = listOf("Tüm sorular senin zekânla boy ölçüştü, ama kazanan sensin",
                    "Zirve senin, bu başarı tesadüf değil!", "Mükemmel bir iş çıkardın!",
                    "Bu seviye, sadece çok çalışanların ulaşabildiği yer.",
                    "Sen artık bu konunun ustasısın – neyi başaramazsın ki?",),
            )
        } else if (cupPoint2 != null && toplamPuan >= cupPoint2) {
            CupTier(
                icon = "cup_ic2",
                cupName = "Usta Kupası",
                cupMotivation = listOf("Gerçek ustalık; bilgi, sabır ve kararlılıkla gelir. Tebrikler!",
                    "Gayet iyi!", "Biraz daha pratikle zirveye ulaşman an meselesi.",
                    "Azmin gücüyle her adım seni daha da ileri taşıyor!",
                    "Çok iyi gidiyorsun! Birkaç adım sonra zirvedesin.",
                    "Gelişimin fark ediliyor. Pes etme, devam et!"),
            )
        } else if (toplamPuan > 0) {
            CupTier(
                icon = "cup_ic",
                cupName = "Deneyim Kupası",
                cupMotivation = listOf("Her usta bir zamanlar amatördü. Sen de yoldasın!",
                    "Cesaret ettin, başladın. Bu en önemli adımdı.",
                    "Unutma, başarı tekrar tekrar denemekten geçer!",
                    "Bugün attığın adım, yarının başarısını başlatır."),
            )
        } else {
            CupTier(
                icon = "cup_ic",
                cupName = "Deneyim Kupası",
                cupMotivation = listOf("Ayıp olmasın diye bu kupayı verdik.",
                    "Biraz fazla mı yavaş oldu ne ?",
                    "Yanii.. Hiç yapmamış olmaktan iyi.",
                    "En azından ekranı açtın, bu da bir şey.",
                    "Kupayı biz de pek vermek istemedik ama...",
                    "Kupa geldi ama vicdanımız biraz sızladı."),
            )
        }
    }

    /** Android: `motivationText.text = cupMotivation.random()`. */
    fun pickMotivation(tier: CupTier, random: Random = Random.Default): String = tier.cupMotivation.random(random)

    /** Android `updateMapProgress`: `List(item.stepCount) { index -> index < item.currentStep }`. */
    fun newStepCompletionStatus(stepCount: Int, currentStep: Int): List<Boolean> =
        List(stepCount) { index -> index < currentStep }

    /**
     * Android `updateMapProgress`: `stepCount == currentStep` ise ders biter (stepIsFinish = true, stepCupIcon = ikon,
     * sonraki öğe isCompleted = true); değilse currentStep ve startStepNumber 1 artar.
     */
    fun isLastStep(stepCount: Int, currentStep: Int): Boolean = stepCount == currentStep
}
