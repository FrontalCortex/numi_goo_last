// KAYNAK: app/src/main/java/com/example/app/ChestFragment.kt
//   - updateMapProgress()                      → [ChestFragmentLogic.updateMapProgress]
//   - unlockNextRaceItemSkippingCompleted()    → [ChestFragmentLogic.unlockNextRaceItemSkippingCompleted]
//   - changeCupIcon() / starCountForChestIcon()→ [ChestFragmentLogic.changeCupIcon]
//   - lessonProgressKey()                      → [ChestFragmentLogic.lessonProgressKey]
//   - setupClaimRewardButton() içindeki kararlar → shouldMarkJustFinishedChestForRating,
//     shouldIncrementDartProgress, isLastChestOfCupPathPart, shouldIncrementTornado/Volcano,
//     shouldRedirectToLoginForFirstTutorial
//
// Bu dosya, ders/sandık ödülü alındığında (claim) haritadaki ilerlemenin nasıl değiştiğini belirleyen mantıktır:
// adım ilerletme, dersi bitirme, sıradaki dersin kilidini açma, yarış (race) seviyesi geçişleri.
//
// Android'den bilinçli farklar:
//  - Fragment / Context / animasyon / navigasyon kodu yok.
//  - `SeasonClock.currentSeason()` yerine `season` parametresi.
//  - Yan etkiler (LessonSuccessRateRepository.recordPass / recordItemFirstFinish, MissionsProgressStore.*,
//    GlobalValues.pendingLessonProgressAnimations) doğrudan çağrılmaz; [ChestFragmentLogic.MapProgressResult]
//    içinde "yapılacaklar" olarak döner. Platform katmanı bunları ilgili depolara uygular.
//  - LessonProgressDiag logları kaldırıldı.
package com.sorobit.shared.ders

object ChestFragmentLogic {

    /** Başarı oranı deposuna gönderilecek olay (Android: LessonSuccessRateRepository çağrıları). */
    sealed class SuccessRateEvent {
        /** `LessonSuccessRateRepository.recordPass(partId, position, step)` */
        data class Pass(val partId: Int, val position: Int, val step: Int) : SuccessRateEvent()

        /** `LessonSuccessRateRepository.recordItemFirstFinish(partId, position, chestStars)` */
        data class ItemFirstFinish(val partId: Int, val position: Int, val chestStars: Int?) : SuccessRateEvent()
    }

    /**
     * Haritadaki halka dolum animasyonu için bekleyen kayıt
     * (Android: `GlobalValues.pendingLessonProgressAnimations[key] = PendingLessonProgressAnimation(from, to)` ve
     * `GlobalValues.canConsumePendingLessonProgressAnimations = false`).
     */
    data class PendingLessonProgressAnimation(
        val key: String,
        val fromFilledSegments: Int,
        val toFilledSegments: Int,
    )

    data class MapProgressResult(
        /** `MissionsProgressStore.recordStepFinishProgress` çağrılmalı mı. */
        val shouldIncrementStepFinishMission: Boolean,
        /**
         * `MissionsProgressStore.recordStepIncrementProgress` çağrılmalı mı. Android fonksiyonunun DÖNÜŞ DEĞERİ de budur
         * (`userRocketDailyLessons` +1 bayrağı).
         */
        val shouldIncrementStepCountMission: Boolean,
        /** `MissionsProgressStore.recordPerfectStepIncrementProgress` çağrılmalı mı. */
        val shouldIncrementPerfectStepCountMission: Boolean,
        /** Sırasıyla uygulanacak başarı oranı olayları. */
        val successRateEvents: List<SuccessRateEvent>,
        /** Dolu halka sayısı arttıysa bekleyen animasyon kaydı; yoksa null. */
        val pendingAnimation: PendingLessonProgressAnimation?,
    ) {
        /** Android `updateMapProgress()` dönüş değeri. */
        val incrementRocketDailyLessons: Boolean get() = shouldIncrementStepCountMission
    }

    /** [changeCupIcon] çıktısı. */
    data class CupIconChange(
        /** Android `icon` alanı; sandık değilse Android'de 0 kalır → burada boş String. */
        val icon: String,
        /** Android `pendingChestStarGainAmount`. */
        val pendingChestStarGainAmount: Int,
    )

    fun lessonProgressKey(item: LessonItem, fallbackIndex: Int): String {
        val stableId = item.id ?: -1
        val part = item.partId ?: -1
        return "${item.type}_${stableId}_${part}_${item.title}_$fallbackIndex"
    }

    /**
     * Sandık (TYPE_CHEST) için rekora göre gösterilecek yıldız görselini ve kazanılan yıldız sayısını hesaplar.
     * Sandık değilse hiçbir şey değişmez (icon boş, kazanım 0).
     */
    fun changeCupIcon(lessonItem: LessonItem, globalPartId: Int = GlobalLessonData.globalPartId): CupIconChange {
        if (lessonItem.type != LessonItem.TYPE_CHEST) return CupIconChange(icon = "", pendingChestStarGainAmount = 0)
        val record = lessonItem.record ?: 0
        val p1 = lessonItem.cupPoint1
        val p2 = lessonItem.cupPoint2
        var resolvedIcon = when {
            p1 != null && record >= p1 -> ChestIcons.CHEST_STARS_TIER3
            p2 != null && record >= p2 -> ChestIcons.CHEST_STARS_TIER2
            record >= 500 -> ChestIcons.CHEST_STARS_TIER1
            else -> ChestIcons.CHEST_STARS_TIER0
        }
        if (lessonItem.isBlinding == true && globalPartId != 6) {
            resolvedIcon = ChestIcons.STAR_ON_IC
        }
        val currentIcon = lessonItem.stepCupIcon
        val iconChanged = currentIcon != resolvedIcon
        val currentStars = ChestTypeProgressHelper.starCountForChestIcon(currentIcon)
        val resolvedStars = ChestTypeProgressHelper.starCountForChestIcon(resolvedIcon)
        val gainedStars = (resolvedStars - currentStars).coerceAtLeast(0)
        return CupIconChange(
            icon = if (iconChanged) resolvedIcon else currentIcon,
            pendingChestStarGainAmount = if (iconChanged) gainedStars else 0,
        )
    }

    /**
     * Ödül alındığında harita ilerlemesini günceller ([GlobalLessonData] bellek durumu + [LessonManager] üzerinden).
     *
     * @param mapFragmentStepIndex tıklanan dersin listedeki indeksi (Android: GlobalValues.mapFragmentStepIndex)
     * @param recordScore bu koşunun skoru (sandıkta toplam puan, derste ders puanı)
     * @param lessonSuccessRate başarı yüzdesi (0..100)
     * @param icon [changeCupIcon] ile hesaplanan görsel (sandık bitişinde stepCupIcon olarak yazılır)
     */
    fun updateMapProgress(
        mapFragmentStepIndex: Int,
        recordScore: Int,
        lessonSuccessRate: Float,
        icon: String,
        season: Int,
    ): MapProgressResult {
        val globalPartId = GlobalLessonData.globalPartId
        val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex) // tıklanan indeksteki adım öğesi
        var index: Int = mapFragmentStepIndex + 1
        if (lessonItem?.cupPoint1 != null) {
            index += 1
        }
        val lessonItem2 = LessonManager.getLessonItem(index)
        var shouldIncrementStepFinishMission = false
        var shouldIncrementStepCountMission = false
        var shouldIncrementPerfectStepCountMission = false
        val successRateEvents = mutableListOf<SuccessRateEvent>()
        var pendingAnimation: PendingLessonProgressAnimation? = null

        lessonItem?.let { item ->
            // İlk `currentStep` adım true, diğerleri false olacak şekilde stepCompletionStatus oluştur
            val newStepCompletionStatus = List(item.stepCount) { i -> i < item.currentStep }

            when (item.raceBusyLevel) {
                1 -> {
                    // Yarış dersi bitti: kendisi TAMAMLANDI (0) olur, sıradaki kilitli yarış açılır.
                    val updatedItem = item.copy(raceBusyLevel = 0)
                    LessonManager.updateRaceItem(mapFragmentStepIndex, updatedItem, season)
                    unlockNextRaceItemSkippingCompleted(mapFragmentStepIndex, season)
                }
                2 -> {
                    // İleri sar: yalnızca kendisi TAMAMLANDI olur, sıradaki değişmez.
                    val updatedItem = item.copy(raceBusyLevel = 0)
                    LessonManager.updateRaceItem(mapFragmentStepIndex, updatedItem, season)
                }
            }

            if (item.raceBusyLevel == null) {
                if (item.stepCount == item.currentStep) {
                    // BİTİŞ dalı: son adım da tamamlandı.
                    val baseForChest = if (item.type == LessonItem.TYPE_CHEST) {
                        LessonItem.mergeChestRun(item, recordScore, season)
                    } else {
                        item
                    }
                    val updatedItem = if (baseForChest.type == LessonItem.TYPE_CHEST) {
                        baseForChest.copy(
                            stepCompletionStatus = newStepCompletionStatus,
                            stepIsFinish = true,
                            stepCupIcon = icon,
                        )
                    } else {
                        baseForChest.copy(
                            stepCompletionStatus = newStepCompletionStatus,
                            stepIsFinish = true,
                        )
                    }
                    if (item.type == LessonItem.TYPE_LESSON && !item.stepIsFinish && updatedItem.stepIsFinish) {
                        shouldIncrementStepFinishMission = true
                        shouldIncrementStepCountMission = true
                        if (lessonSuccessRate >= 100f) {
                            shouldIncrementPerfectStepCountMission = true
                        }
                    }
                    if (!item.stepIsFinish && updatedItem.stepIsFinish &&
                        (item.type == LessonItem.TYPE_LESSON || item.type == LessonItem.TYPE_CHEST)
                    ) {
                        successRateEvents.add(SuccessRateEvent.Pass(globalPartId, mapFragmentStepIndex, item.currentStep))
                        val chestStars = if (updatedItem.type == LessonItem.TYPE_CHEST) {
                            ChestTypeProgressHelper.starCountForChestIcon(updatedItem.stepCupIcon)
                        } else {
                            null
                        }
                        successRateEvents.add(
                            SuccessRateEvent.ItemFirstFinish(globalPartId, mapFragmentStepIndex, chestStars),
                        )
                    }
                    val beforeFilled = item.stepCompletionStatus.count { it }
                    val afterFilled = updatedItem.stepCompletionStatus.count { it }
                    if (afterFilled > beforeFilled) {
                        pendingAnimation = PendingLessonProgressAnimation(
                            key = lessonProgressKey(item, mapFragmentStepIndex),
                            fromFilledSegments = beforeFilled,
                            toFilledSegments = afterFilled,
                        )
                    }
                    LessonManager.updateLessonItem(mapFragmentStepIndex, updatedItem, season)

                    // Sıradaki dersin kilidini aç (sandıktan sonra araya giren başlık atlanır: index + 1).
                    lessonItem2?.let { item2 ->
                        val updatedItem2 = item2.copy(
                            isCompleted = true,
                        )
                        LessonManager.updateLessonItem(index, updatedItem2, season)
                    }
                } else {
                    // ARA ADIM dalı: currentStep bir artar, stepIsFinish false kalır.
                    if (item.type == LessonItem.TYPE_LESSON || item.type == LessonItem.TYPE_CHEST) {
                        successRateEvents.add(SuccessRateEvent.Pass(globalPartId, mapFragmentStepIndex, item.currentStep))
                    }
                    val updatedItem = item.copy(
                        stepCompletionStatus = newStepCompletionStatus,
                        currentStep = item.currentStep + 1,
                        startStepNumber = item.startStepNumber?.plus(1),
                    )
                    if (item.type == LessonItem.TYPE_LESSON && updatedItem.currentStep == item.currentStep + 1) {
                        shouldIncrementStepCountMission = true
                        if (lessonSuccessRate >= 100f) {
                            shouldIncrementPerfectStepCountMission = true
                        }
                    }
                    val beforeFilled = item.stepCompletionStatus.count { it }
                    val afterFilled = updatedItem.stepCompletionStatus.count { it }
                    if (afterFilled > beforeFilled) {
                        pendingAnimation = PendingLessonProgressAnimation(
                            key = lessonProgressKey(item, mapFragmentStepIndex),
                            fromFilledSegments = beforeFilled,
                            toFilledSegments = afterFilled,
                        )
                    }
                    LessonManager.updateLessonItem(mapFragmentStepIndex, updatedItem, season)
                }
            }
            // raceBusyLevel != null ise (0/1/2/diğer) bitiş/ara adım bloğuna girilmez; stepIsFinish burada ayarlanmaz.
        }

        return MapProgressResult(
            shouldIncrementStepFinishMission = shouldIncrementStepFinishMission,
            shouldIncrementStepCountMission = shouldIncrementStepCountMission,
            shouldIncrementPerfectStepCountMission = shouldIncrementPerfectStepCountMission,
            successRateEvents = successRateEvents,
            pendingAnimation = pendingAnimation,
        )
    }

    /**
     * Sıradaki yarış dersini açar. İleri sar ile tamamlanmış (0) item'leri atlar;
     * ilk kilitli (2) item'i BAŞLA (1) yapar. Zaten 1 olan item görülürse durur.
     * @return kilidi açılan item'in indeksi; açılan yoksa null (Android'de dönüş değeri yok).
     */
    fun unlockNextRaceItemSkippingCompleted(completedIndex: Int, season: Int): Int? {
        val items = GlobalLessonData.lessonItems
        for (j in (completedIndex + 1) until items.size) {
            val next = items[j]
            when (next.raceBusyLevel) {
                0 -> continue
                2 -> {
                    LessonManager.updateRaceItem(j, next.copy(raceBusyLevel = 1), season)
                    return j
                }
                1 -> {
                    return null
                }
                else -> continue
            }
        }
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // setupClaimRewardButton() içindeki kararlar
    // ---------------------------------------------------------------------------------------------

    /**
     * İlk tutorial dersini bitirip henüz hesap açmamış kullanıcı sandık yerine giriş ekranına yönlendirilir
     * (Android: `GlobalValues.currentTutorialNumber == 1 && FirebaseAuth.currentUser == null`).
     * Yönlendirmede `AppPrefs/tutorial1_login_flow_pending = true` yazılır ve currentTutorialNumber 0'lanır.
     */
    fun shouldRedirectToLoginForFirstTutorial(currentTutorialNumber: Int, isLoggedIn: Boolean): Boolean =
        currentTutorialNumber == 1 && !isLoggedIn

    /** Claim öncesi item henüz bitmemiş bir sandıksa uygulama puanlama istemi bayrağı set edilir. */
    fun shouldMarkJustFinishedChestForRating(lessonBeforeClaim: LessonItem?): Boolean =
        lessonBeforeClaim?.type == LessonItem.TYPE_CHEST && lessonBeforeClaim.stepIsFinish != true

    /** Dart rozeti: ilk kez bitirilen (claim öncesi bitmemiş) TYPE_LESSON ve %100 başarı. */
    fun shouldIncrementDartProgress(lessonBeforeClaim: LessonItem?, lessonSuccessRate: Float): Boolean {
        val isPerfectLesson = lessonSuccessRate >= 100f
        return when (val item = lessonBeforeClaim) {
            null -> false
            else ->
                item.type == LessonItem.TYPE_LESSON &&
                    !item.stepIsFinish &&
                    isPerfectLesson
        }
    }

    /**
     * Kupa Yolu tetikleyici: part 1-6'daki SON TYPE_CHEST tamamlandıysa true
     * (Android: `GlobalValues.pendingCupPathRevealPartId = globalPartId`).
     */
    fun isLastChestOfCupPathPart(
        lessonAfterClaim: LessonItem?,
        globalPartId: Int = GlobalLessonData.globalPartId,
        lessonItems: List<LessonItem> = GlobalLessonData.lessonItems,
    ): Boolean {
        return lessonAfterClaim?.let { afterItem ->
            if (afterItem.type == LessonItem.TYPE_CHEST && afterItem.stepIsFinish && globalPartId in 1..6) {
                val allChestsInPart = lessonItems.filter {
                    it.type == LessonItem.TYPE_CHEST
                }
                val lastChest = allChestsInPart.lastOrNull()
                lastChest != null && lastChest.mapFragmentIndex != null &&
                    lastChest.mapFragmentIndex == afterItem.mapFragmentIndex
            } else {
                false
            }
        } == true
    }

    /** ChestFragment claim: Kasırga rozeti part 7'de artar. */
    fun shouldIncrementTornado(globalPartId: Int = GlobalLessonData.globalPartId): Boolean = globalPartId == 7

    /** ChestFragment claim: Yanardağ rozeti part 8'de artar. */
    fun shouldIncrementVolcano(globalPartId: Int = GlobalLessonData.globalPartId): Boolean = globalPartId == 8
}
