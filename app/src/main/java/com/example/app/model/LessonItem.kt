package com.example.app.model

import androidx.fragment.app.Fragment
import com.example.app.R
import java.io.Serializable

data class LessonItem(
    val id: Int? = null,
    val type: Int,
    val title: String,
    var partId:Int? = null,
    var offset: Int, // -50 sol, 0 orta, 50 sağ
    var isCompleted: Boolean,
    var progressBarLevel: Int = 0,
    var stepCount: Int,      // toplam adım sayısı
    var currentStep: Int = 0,     // mevcut adım (0'dan başlar)
    @Transient var fragment: (() -> Fragment?)? = null, // Fragment oluşturma fonksiyonu
    val color: Int? = null,    // Renk değeri, varsayılanı null
    var LESSON_ID: Int? = null,
    var isBlinding: Boolean? = null,
    var stepCompletionStatus: List<Boolean> = List(stepCount) { false },// Her adımın tamamlanma durumu
    var finishStepNumber: Int? = null,
    var startStepNumber: Int? = null,
    var mapFragmentIndex: Int? = null,
    var stepIsFinish: Boolean = false,
    var finalGoldVisualUnlocked: Boolean = false,
    var tutorialNumber: Int = 0,
    var tutorialIsFinish: Boolean = false,
    var lessonHint: String? = null,
    var stepCupIcon: Int = R.drawable.chest_stars_tier0,
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
    /** Tüm zamanların en yüksek kupa skoru (harita / bottom sheet "Rekor"). */
    var record: Int? = null,
    /**
     * Yalnızca [leaderboardSeasonId] sezonunda oynanan koşuların en iyi skoru; mevcut sezon liderlik tablosuna
     * [LessonLeaderboardRepository.submitBestIfNeeded] ile yalnızca bu değer yazılır. Sezon değişince bucket sıfırlanır.
     */
    var leaderboardSeasonBest: Int? = null,
    var leaderboardSeasonId: Int? = null,
    var abacusGuideNumber: Int? = null,
    var titleUnit: String? = null,
) : Serializable {
    /** [season] ile eşleşen sezon en iyi skoru; yoksa liderliğe eski [record] yazılmaz. */
    /** [MapFragment.getLessonOperations] yoksa geri adım alt sınırı (dersin ilk adım id'si). */
    fun minLessonOperationsId(): Int {
        val finish = finishStepNumber ?: startStepNumber ?: 1
        return finish - stepCount + 1
    }

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
        const val TYPE_PART = 4
        const val TYPE_BACK_PART = 5
        // Fragment ID sabitleri

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

} //global part ıd değiştirilecek adapter'da