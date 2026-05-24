package com.example.app

data class DailyQuestionSlot(
    val sequence: List<Int>,
    val partId: Int,
    val itemIndex: Int,
    val titleUnit: String,
    val difficulty: String,
)

data class DailyQuestionChallenge(
    val periodKey: String,
    val solvedCount: Int,
    val questions: List<DailyQuestionSlot>,
    val rewardClaimed: Boolean = false,
    /** Yanlış cevap sonrası elmas ödenmeden tekrar oynanacak slot (0..2). */
    val pendingContinueSlotIndex: Int? = null,
) {
    val isComplete: Boolean
        get() = solvedCount >= DailyQuestionPeriod.QUESTIONS_PER_PERIOD

    val needsDiamondContinue: Boolean
        get() = pendingContinueSlotIndex != null

    val currentSlotIndex: Int
        get() = solvedCount.coerceIn(0, questions.lastIndex.coerceAtLeast(0))

    fun slotForPlay(): DailyQuestionSlot? {
        pendingContinueSlotIndex?.let { idx ->
            return questions.getOrNull(idx)
        }
        return questions.getOrNull(currentSlotIndex)
    }

    fun cardTitleUnit(): String {
        if (isComplete) {
            return questions.lastOrNull()?.titleUnit.orEmpty()
        }
        return slotForPlay()?.titleUnit.orEmpty()
    }

    fun cardDifficultyLabel(): String {
        if (isComplete) return ""
        return slotForPlay()?.difficulty.orEmpty()
    }

    fun playSlotIndex(): Int = pendingContinueSlotIndex ?: currentSlotIndex
}

data class DailyQuestionCardUiState(
    val periodKey: String,
    val solvedCount: Int,
    val totalCount: Int,
    val titleUnit: String,
    val difficulty: String,
    val isComplete: Boolean,
    val rewardClaimed: Boolean,
    val poolAvailable: Boolean,
    val pendingContinueSlotIndex: Int? = null,
    /** Firestore/prefs kart verisi yüklendiyse true; placeholder bind animasyonu tetiklemez. */
    val isLoaded: Boolean = false,
) {
    val needsDiamondContinue: Boolean
        get() = pendingContinueSlotIndex != null
    val canClaimReward: Boolean
        get() = isComplete && !rewardClaimed

    /** 3/3 bitti, ödül alınmadı — kart tıklanınca tamamlama toast'ı. */
    fun shouldShowChallengeCompleteToast(): Boolean {
        if (rewardClaimed) return false
        val total = totalCount.coerceAtLeast(1)
        return solvedCount >= total || isComplete || canClaimReward
    }
}
