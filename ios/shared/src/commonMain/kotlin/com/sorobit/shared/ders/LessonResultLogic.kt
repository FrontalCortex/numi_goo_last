// KAYNAK:
//   app/src/main/java/com/example/app/LessonResult.kt       → [LessonResultLogic]
//   app/src/main/java/com/example/app/LessonResultFalse.kt  → [LessonResultFalseLogic]
//   app/src/main/java/com/example/app/ChestResult.kt        → [ChestResultLogic]
//     (record(), continueFragment() "tekrar oynama" dalı, resolveScoreCap, resolveCupPoint2Threshold,
//      maybeTriggerStarMarkers eşikleri, updateScoreProgressBar oranı, parseTimeToSeconds, formatSeconds)
//
// Sonuç ekranlarındaki görünümden bağımsız hesaplar: başarı oranı, ders puanı, "ödül ekranına mı / haritaya mı"
// kararı, başarısızlık kaydı, sandık rekoru ve yıldız kazanımı.
//
// Android'de yeni (ortak kopyada karşılığı yok, platform işi): sonuç ekranlarında Sobi maskotu sahnesi (LessonEndMascot),
// çıkış kaymasının 400 ms'ye uzaması, LessonMilestones.recordChestRun (haftalık kart için sandık süresi; arka plan
// yazımı), ses efektleri kapalıysa yıldız sesinin çalmaması ("sound_enabled" tercihi).
//
// Android'den bilinçli farklar: Fragment / animasyon / navigasyon yok; SeasonClock yerine `season` parametresi;
// depo çağrıları (LessonSuccessRateRepository, MissionsProgressStore, rozetler) karar/sonuç nesnesi olarak döner.
package com.sorobit.shared.ders

/** Başarılı ders sonucu ekranı (Android: LessonResult). */
object LessonResultLogic {

    /** Ödül al (claim) düğmesine basıldığında gidilecek yer. */
    enum class ClaimBranch {
        /** Ders zaten bitmiş (stepIsFinish): sandık ekranı atlanır, haritaya dönülür. */
        SKIP_TO_MAP,

        /** Sandık/ödül ekranına (ChestFragment) gidilir; argümanlar: successRate, dersPuani. */
        GO_TO_CHEST,
    }

    data class ClaimDecision(
        val branch: ClaimBranch,
        /** SKIP_TO_MAP dalında `LessonSuccessRateRepository.recordItemReplay(globalPartId, mapIdx)` çağrılmalı mı. */
        val recordItemReplay: Boolean,
        /**
         * SKIP_TO_MAP dalında haritaya dönüş "ders türü" dönüşü mü (Android: `isLessonTypeReturn = isLessonTypeItem`,
         * item türü TYPE_LESSON). GO_TO_CHEST dalında anlamsız (false).
         */
        val isLessonTypeReturn: Boolean = false,
    )

    /** Başarı yüzdesi: doğru / toplam * 100; toplam 0 ise 0. */
    fun successRate(correctAnswers: Int, totalQuestions: Int): Float =
        if (totalQuestions > 0) {
            (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }

    /** Ders puanı: başarı yüzdesi × 5 (aşağı yuvarlanır; en çok 500). */
    fun lessonScore(successRate: Float): Int = (successRate * 5f).toInt()

    /** Ekranda gösterilen başarı metni ("87%"). */
    fun successRateText(successRate: Float): String = "${successRate.toInt()}%"

    /**
     * Claim düğmesi kararı. Android ile aynı yan etki: [lessonItem] varsa `tutorialIsFinish = true` yapılır
     * (nesne yerinde değiştirilir; Firestore'a ancak sonraki bir güncellemeyle yazılır).
     */
    fun onClaim(lessonItem: LessonItem?): ClaimDecision {
        lessonItem?.tutorialIsFinish = true
        return if (lessonItem?.stepIsFinish == true) {
            ClaimDecision(
                branch = ClaimBranch.SKIP_TO_MAP,
                recordItemReplay = lessonItem.type == LessonItem.TYPE_LESSON || lessonItem.type == LessonItem.TYPE_CHEST,
                isLessonTypeReturn = lessonItem.type == LessonItem.TYPE_LESSON,
            )
        } else {
            ClaimDecision(branch = ClaimBranch.GO_TO_CHEST, recordItemReplay = false)
        }
    }
}

/** Başarısız ders sonucu ekranı (Android: LessonResultFalse). */
object LessonResultFalseLogic {

    /**
     * `LessonSuccessRateRepository.recordFail(partId, position, step, answerSuccessRatePercent)` argümanları.
     * [answerSuccessRatePercent]: o başarısız denemenin başarı oranı (0–100); Android bunu yalnızca part 1–6 için
     * Analytics'e gönderir (platform `partId in 1..6` ise kullanır).
     */
    data class FailRecord(val partId: Int, val position: Int, val step: Int, val answerSuccessRatePercent: Float? = null)

    /**
     * Ekran açıldığında bir kez: item TYPE_LESSON ya da TYPE_CHEST ise başarısızlık kaydı; değilse null.
     */
    fun failRecordFor(
        item: LessonItem?,
        mapFragmentStepIndex: Int,
        globalPartId: Int = GlobalLessonData.globalPartId,
        successRate: Float? = null,
    ): FailRecord? {
        if (item?.type == LessonItem.TYPE_LESSON || item?.type == LessonItem.TYPE_CHEST) {
            return FailRecord(globalPartId, mapFragmentStepIndex, item.currentStep, successRate)
        }
        return null
    }

    /**
     * Başarısızlık nedeni metni (Android: ui_puan_500_den_dusuk / ui_basari_orani_75_ten_dusuk, Türkçe):
     * sandıkta puan eşiği (500), derste başarı oranı eşiği (artık %75; eskiden %80).
     */
    fun resultTitleComment(isChestFailure: Boolean): String =
        if (isChestFailure) {
            "Puan 500'den düşük!"
        } else {
            "Başarı oranı %75'ten düşük!"
        }

    /** LessonResultFalse'tan haritaya dönüş "ders türü" dönüşü mü (Android: `isLessonTypeItem` args). */
    fun isLessonTypeReturn(item: LessonItem?): Boolean = item?.type == LessonItem.TYPE_LESSON

    /**
     * Tutorial 1'de ve giriş yapılmamışsa claim'de giriş ekranına yönlendirilir (currentTutorialNumber 0'lanır).
     * Ayrıca currentTutorialNumber == 1 ise her durumda "ilk tutorial gösterildi" işaretlenir.
     */
    fun shouldRedirectToLogin(currentTutorialNumber: Int, isLoggedIn: Boolean): Boolean =
        currentTutorialNumber == 1 && !isLoggedIn
}

/** Sandık (ünite maratonu) sonuç ekranı (Android: ChestResult). */
object ChestResultLogic {

    /** [replayFinishedChest] çıktısı. */
    data class ReplayResult(
        /** Belleğe/Firestore'a yazılan güncel sandık; [chestItemBefore] sandık değilse/null ise null. */
        val updated: LessonItem?,
        /** `MissionsProgressStore.recordChestStarGainProgress` için kazanılan yıldız (0 ise çağrılmaz). */
        val gainedStars: Int,
        /** Karate rozeti: bu koşuyla ilk kez 3 yıldıza ulaşıldı mı. */
        val shouldIncrementKarate: Boolean,
        /** Uygulama puanlama istemi bayrağı (claim öncesi sandık bitmemişse). */
        val justFinishedChestForRating: Boolean,
    )

    /**
     * Ekran açılışında (Android: record()): rekor kırıldıysa görev bayrağı döner ve [lessonItem] üzerindeki
     * record / leaderboardSeasonId / leaderboardSeasonBest alanları YERİNDE güncellenir (Android ile aynı).
     * @return `shouldIncrementChestRecordBreakMission` (önceki rekor varken ve aşıldıysa true; ilk rekorda false).
     */
    fun record(lessonItem: LessonItem, toplamPuan: Int, season: Int): Boolean {
        if (lessonItem.type != LessonItem.TYPE_CHEST) return false
        var shouldIncrementChestRecordBreakMission = false
        val onceki = lessonItem.record
        if (onceki != null && toplamPuan > onceki) {
            shouldIncrementChestRecordBreakMission = true
        }
        val m = LessonItem.mergeChestRun(lessonItem, toplamPuan, season)
        lessonItem.record = m.record
        lessonItem.leaderboardSeasonId = m.leaderboardSeasonId
        lessonItem.leaderboardSeasonBest = m.leaderboardSeasonBest
        return shouldIncrementChestRecordBreakMission
    }

    /** Devam düğmesi: zaten bitmiş sandık tekrar oynandıysa ödül ekranı (ChestFragment) atlanır. */
    fun shouldSkipChest(lessonItem: LessonItem): Boolean =
        lessonItem.type == LessonItem.TYPE_CHEST && lessonItem.stepIsFinish

    /**
     * [shouldSkipChest] dalı: tekrar oynanan sandığın rekorunu/görselini günceller
     * ([LessonManager.updateLessonItem] ile belleğe + Firestore'a yazar).
     * Ayrıca platform `LessonSuccessRateRepository.recordItemReplay(globalPartId, idx)` çağırmalıdır.
     */
    fun replayFinishedChest(idx: Int, toplamPuan: Int, season: Int): ReplayResult {
        val globalPartId = GlobalLessonData.globalPartId
        val chestItemBefore = LessonManager.getLessonItem(idx)
        val justFinished = chestItemBefore?.type == LessonItem.TYPE_CHEST && chestItemBefore.stepIsFinish != true
        var gainedStars = 0
        var updatedItem: LessonItem? = null
        val shouldIncrementKarate = if (chestItemBefore != null && chestItemBefore.type == LessonItem.TYPE_CHEST) {
            val karateFlag =
                ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(chestItemBefore, toplamPuan, globalPartId)
            val merged = LessonItem.mergeChestRun(chestItemBefore, toplamPuan, season)
            val newRecord = merged.record!!
            val newIcon = ChestTypeProgressHelper.resolvedChestIcon(chestItemBefore, newRecord, globalPartId)
            val oldStars = ChestTypeProgressHelper.starCountForChestIcon(chestItemBefore.stepCupIcon)
            val newStars = ChestTypeProgressHelper.starCountForChestIcon(newIcon)
            gainedStars = (newStars - oldStars).coerceAtLeast(0)
            val updated = merged.copy(stepCupIcon = newIcon)
            LessonManager.updateLessonItem(idx, updated, season)
            updatedItem = updated
            karateFlag
        } else {
            false
        }
        return ReplayResult(
            updated = updatedItem,
            gainedStars = gainedStars,
            shouldIncrementKarate = shouldIncrementKarate,
            justFinishedChestForRating = justFinished,
        )
    }

    /**
     * Tekrar oynama dalında Kasırga rozeti artışı.
     * DİKKAT (Android davranışı aynen korunuyor): burada koşul `globalPartId == 4`; ChestFragment'te ise 7.
     */
    fun shouldIncrementTornadoOnReplay(globalPartId: Int = GlobalLessonData.globalPartId): Boolean = globalPartId == 4

    /** Tekrar oynama dalında Yanardağ rozeti artışı (part 8). */
    fun shouldIncrementVolcanoOnReplay(globalPartId: Int = GlobalLessonData.globalPartId): Boolean = globalPartId == 8

    /**
     * Puan çubuğunun üst sınırı (3 yıldız eşiği): bellekteki item'ın cupPoint1'i; yoksa şablondaki; o da yoksa 1.
     */
    fun resolveScoreCap(
        mapFragmentStepIndex: Int,
        globalPartId: Int = GlobalLessonData.globalPartId,
    ): Int {
        val currentCupPoint = LessonManager.getLessonItem(mapFragmentStepIndex)?.cupPoint1
        if (currentCupPoint != null && currentCupPoint > 0) return currentCupPoint

        val templateCupPoint = GlobalLessonData.createLessonItems(globalPartId)
            .getOrNull(mapFragmentStepIndex)
            ?.cupPoint1
        if (templateCupPoint != null && templateCupPoint > 0) return templateCupPoint

        return 1
    }

    /** 2 yıldız eşiği: bellekteki cupPoint2; yoksa şablondaki; o da yoksa max(1, scoreCap / 2). */
    fun resolveCupPoint2Threshold(
        mapFragmentStepIndex: Int,
        scoreCap: Int,
        globalPartId: Int = GlobalLessonData.globalPartId,
    ): Int {
        val currentCupPoint2 = LessonManager.getLessonItem(mapFragmentStepIndex)?.cupPoint2
        if (currentCupPoint2 != null && currentCupPoint2 > 0) return currentCupPoint2

        val templateCupPoint2 = GlobalLessonData.createLessonItems(globalPartId)
            .getOrNull(mapFragmentStepIndex)
            ?.cupPoint2
        if (templateCupPoint2 != null && templateCupPoint2 > 0) return templateCupPoint2

        return maxOf(1, scoreCap / 2)
    }

    /** Puan çubuğunun doluluk oranı (0..1). */
    fun scoreProgress(animatedScore: Int, scoreCap: Int): Float {
        val clampedScore = animatedScore.coerceIn(0, scoreCap)
        return (clampedScore.toFloat() / scoreCap.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Sayaç [previousScore] → [currentScore] ilerlerken bu adımda geçilen yıldız eşikleri (1, 2, 3).
     * 1. yıldız: 500 puan; 2. yıldız: [cupPoint2Threshold]; 3. yıldız: [scoreCap].
     * (Android'de her yıldız ayrıca "bir kez oynatıldı" bayrağıyla korunur; o durum platformda tutulur.)
     */
    fun starTiersCrossed(previousScore: Int, currentScore: Int, cupPoint2Threshold: Int, scoreCap: Int): List<Int> {
        val out = mutableListOf<Int>()
        if (previousScore < 500 && currentScore >= 500) out.add(1)
        if (previousScore < cupPoint2Threshold && currentScore >= cupPoint2Threshold) out.add(2)
        if (previousScore < scoreCap && currentScore >= scoreCap) out.add(3)
        return out
    }

    /** "mm:ss" ya da "hh:mm:ss" metnini saniyeye çevirir; biçim uymuyorsa null. */
    fun parseTimeToSeconds(value: String): Int? {
        val parts = value.split(":").map { it.trim() }
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: return null
                val seconds = parts[1].toIntOrNull() ?: return null
                minutes * 60 + seconds
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: return null
                val minutes = parts[1].toIntOrNull() ?: return null
                val seconds = parts[2].toIntOrNull() ?: return null
                hours * 3600 + minutes * 60 + seconds
            }
            else -> null
        }
    }

    /** Saniyeyi "d:ss" biçiminde yazar (Android: String.format("%d:%02d")); negatifler 0 sayılır. */
    fun formatSeconds(seconds: Int): String {
        val safeSeconds = maxOf(0, seconds)
        val minutes = safeSeconds / 60
        val remainingSeconds = safeSeconds % 60
        return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
    }
}
