// KAYNAK: app/src/main/java/com/example/app/model/LessonItem.kt (tamamı)
//
// Android'den bilinçli farklar:
//  - `fragment: (() -> Fragment?)?` alanı platforma özgüdür; yerine String anahtarı taşıyan [fragment] alanı var
//    (katalogda hiçbir item bu alanı doldurmuyor, yani pratikte hep null).
//  - `color: Int?` (R.color kimliği) → kaynak ADI ("lesson_header_blue" gibi) taşıyan String.
//  - `stepCupIcon: Int` (R.drawable kimliği) → kaynak ADI ("chest_stars_tier0" gibi) taşıyan String.
//    Android'de Gson ile eksik alan 0 olarak okunabiliyordu; buradaki karşılığı boş String ("").
//  - `java.io.Serializable` kaldırıldı.
package com.sorobit.shared.ders

data class LessonItem(
    val id: Int? = null,
    val type: Int,
    val title: String,
    var partId: Int? = null,
    var offset: Int, // -50 sol, 0 orta, 50 sağ
    var isCompleted: Boolean,
    var progressBarLevel: Int = 0,
    var stepCount: Int,      // toplam adım sayısı
    var currentStep: Int = 0,     // mevcut adım (0'dan başlar)
    /** Android'deki Fragment oluşturma fonksiyonunun yerine geçen anahtar (katalogda kullanılmıyor). */
    var fragment: String? = null,
    /** Renk kaynağının adı (Android: R.color.x → "x"); varsayılanı null. */
    val color: String? = null,
    var LESSON_ID: Int? = null,
    var isBlinding: Boolean? = null,
    var isExtraction: Boolean? = null,
    var isMultiplication: Boolean? = null,
    var stepCompletionStatus: List<Boolean> = List(stepCount) { false }, // Her adımın tamamlanma durumu
    var finishStepNumber: Int? = null,
    var startStepNumber: Int? = null,
    var mapFragmentIndex: Int? = null,
    var stepIsFinish: Boolean = false,
    var finalGoldVisualUnlocked: Boolean = false,
    var tutorialNumber: Int = 0,
    var tutorialIsFinish: Boolean = false,
    var lessonHint: String? = null,
    /** Görsel kaynağının adı (Android: R.drawable.chest_stars_tier0). */
    var stepCupIcon: String = ChestIcons.CHEST_STARS_TIER0,
    var cupPoint1: Int? = null,
    var cupPoint2: Int? = null,
    var worstCupTime: Int? = null,
    var sectionTitle: String? = null,
    var backPart: Boolean? = null,
    var sectionDescription: String? = null,
    var timePeriod: Long? = null,
    var blindingMultiplication: Boolean? = null,
    var racePartId: Int? = null,  // Race item'ının hangi partId'den veri göstereceği
    var backRaceId: Int? = null,
    var raceBusyLevel: Int? = null,

    // --- Kupa Modu Dinamik Zorluk ---
    var cupDifficultyLevel: Int? = null, // 0-4
    var cupWinDelta: Int? = null,        // Doğru cevapta kazanılacak kupa
    var cupLossDelta: Int? = null,       // Yanlış cevapta kaybedilecek kupa

    /** Tüm zamanların en yüksek kupa skoru (harita / bottom sheet "Rekor"). */
    var record: Int? = null,
    /**
     * Yalnızca [leaderboardSeasonId] sezonunda oynanan koşuların en iyi skoru; mevcut sezon liderlik tablosuna
     * yalnızca bu değer yazılır. Sezon değişince bucket sıfırlanır.
     */
    var leaderboardSeasonBest: Int? = null,
    var leaderboardSeasonId: Int? = null,
    var abacusGuideNumber: Int? = null,
    var titleUnit: String? = null,
    var raceTitle: String? = null,
    /** Kupa modunda (globalPartId == 9) seçilen basamak sayısı (1, 2 veya 3). */
    var cupDigitSize: Int? = null,
    /** Kupa modunda kullanıcıya gösterilecek sayı adedi. */
    var cupNumberCount: Int? = null,
) {
    /** Ders operasyonları yoksa geri adım alt sınırı (dersin ilk adım id'si). */
    fun minLessonOperationsId(): Int {
        val finish = finishStepNumber ?: startStepNumber ?: 1
        return finish - stepCount + 1
    }

    /** [season] ile eşleşen sezon en iyi skoru; yoksa liderliğe eski [record] yazılmaz. */
    fun leaderboardSubmitScore(season: Int): Int? {
        if (type != TYPE_CHEST) return null
        val sid = leaderboardSeasonId ?: return null
        if (sid != season) return null
        val b = leaderboardSeasonBest ?: return null
        return b.takeIf { it > 0 }
    }

    companion object {
        const val TYPE_LESSON = 0
        const val TYPE_HEADER = 1
        const val TYPE_CHEST = 2
        const val TYPE_RACE = 3

        /**
         * Kupa koşusu [runScore] ile bittiğinde: [record] tüm zamanların max'ı;
         * sezon alanları yalnızca [currentSeason] tahtası için (önceki sezon skoru taşınmaz).
         */
        fun mergeChestRun(item: LessonItem, runScore: Int, currentSeason: Int): LessonItem {
            require(item.type == TYPE_CHEST)
            val prevRecord = item.record
            val newAllTime = when {
                prevRecord == null -> runScore
                runScore > prevRecord -> runScore
                else -> prevRecord
            }
            val prevSeasonId = item.leaderboardSeasonId
            val prevSeasonBest = item.leaderboardSeasonBest
            val newSeasonBest = when {
                prevSeasonId == null || prevSeasonId != currentSeason ->
                    runScore
                else ->
                    kotlin.math.max(prevSeasonBest ?: 0, runScore)
            }
            return item.copy(
                record = newAllTime,
                leaderboardSeasonId = currentSeason,
                leaderboardSeasonBest = newSeasonBest,
            )
        }
    }
}

/**
 * Sandık/yıldız görsellerinin kaynak adları (Android'deki R.drawable.* karşılıkları).
 * iOS tarafı aynı adlarla görsel varlığı (asset) eşlemelidir.
 */
object ChestIcons {
    const val CHEST_STARS_TIER0 = "chest_stars_tier0"
    const val CHEST_STARS_TIER1 = "chest_stars_tier1"
    const val CHEST_STARS_TIER2 = "chest_stars_tier2"
    const val CHEST_STARS_TIER3 = "chest_stars_tier3"
    const val STAR_ON_IC = "star_on_ic"
    const val STAR_OFF_IC = "star_off_ic"
    const val CUP_IC = "cup_ic"
    const val CUP_IC2 = "cup_ic2"
    const val CUP_IC3 = "cup_ic3"
}
