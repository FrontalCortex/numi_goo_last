// Kaynak: DailyQuestionBrokenHeartStore.kt (tamamı), DailyQuestionCardProgressAnimStore.kt (tamamı),
// TasksFragment.kt içindeki günlük soru kartı kuralları (DailyQuestionViewHolder.bind /
// bindBrokenHeart / isProgressClaimTapReady / persistProgressShown / startCountdownTicker,
// handleDailyQuestionCardClick, onDailyQuestionPeriodRolledOver, startDailyQuestionRewardFlow).
// Farklar: Context + FirebaseAuth yerine depo ([OyunKeyValueStore], Android'de
// `daily_question_prefs` dosyası) ve `uid` sağlayıcısı dışarıdan verilir; görünüm/animasyon kodu yok.
package com.sorobit.shared.oyun

import kotlin.math.abs

/** Günlük soru kartındaki kırık kalp Lottie durumu (periyot + kullanıcı). */
class DailyQuestionBrokenHeartStore(
    /** Android: `DailyQuestionPeriod.PREFS_NAME` adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
    private val uid: () -> String?,
) {
    /** Yanlış cevap sonrası 0→116 animasyonu bir sonraki kart bind'de. */
    fun requestPlayOnNextBind(periodKey: String) {
        val u = uid() ?: return
        prefs.putBoolean(playKey(u, periodKey), true)
    }

    fun consumePlayRequest(periodKey: String): Boolean {
        val u = uid() ?: return false
        val key = playKey(u, periodKey)
        if (!prefs.getBoolean(key, false)) return false
        prefs.putBoolean(key, false)
        return true
    }

    fun setBrokenHold116(periodKey: String, hold: Boolean) {
        val u = uid() ?: return
        prefs.putBoolean(hold116Key(u, periodKey), hold)
    }

    fun isBrokenHold116(periodKey: String): Boolean {
        val u = uid() ?: return false
        return prefs.getBoolean(hold116Key(u, periodKey), false)
    }

    fun clearBrokenHold116(periodKey: String) {
        setBrokenHold116(periodKey, false)
    }

    /** Elmasla devam sonrası 116→son kare→0 animasyonu. */
    fun requestHealPlay(periodKey: String) {
        val u = uid() ?: return
        prefs.putBoolean(healPlayKey(u, periodKey), true)
    }

    fun consumeHealPlay(periodKey: String): Boolean {
        val u = uid() ?: return false
        val key = healPlayKey(u, periodKey)
        if (!prefs.getBoolean(key, false)) return false
        prefs.putBoolean(key, false)
        return true
    }

    fun resetForPeriod(periodKey: String) {
        val u = uid() ?: return
        prefs.remove(playKey(u, periodKey))
        prefs.remove(hold116Key(u, periodKey))
        prefs.remove(healPlayKey(u, periodKey))
    }

    companion object {
        fun playKey(uid: String, periodKey: String) =
            "daily_broken_heart_play_${uid}_$periodKey"

        fun hold116Key(uid: String, periodKey: String) =
            "daily_broken_heart_hold116_${uid}_$periodKey"

        fun healPlayKey(uid: String, periodKey: String) =
            "daily_broken_heart_heal_${uid}_$periodKey"
    }
}

/** Kart progress animasyonunun son gösterildiği [DailyQuestionCardUiState.solvedCount] (periyot + kullanıcı). */
class DailyQuestionCardProgressAnimStore(
    /** Android: `DailyQuestionPeriod.PREFS_NAME` adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
    private val uid: () -> String?,
) {
    fun getLastShown(periodKey: String): Int? {
        val u = uid() ?: return null
        val stored = prefs.getInt(prefsKey(u, periodKey), -1)
        return stored.takeIf { it >= 0 }
    }

    fun setLastShown(periodKey: String, solved: Int) {
        val u = uid() ?: return
        prefs.putInt(prefsKey(u, periodKey), solved.coerceAtLeast(0))
    }

    companion object {
        fun prefsKey(uid: String, periodKey: String): String =
            "daily_card_anim_shown_${uid}_$periodKey"
    }
}

/** Kırık kalp Lottie'sinin kart bağlanırken ne yapacağı. */
enum class DailyQuestionBrokenHeartAction {
    /** 116 → son kare → 0 (elmasla devam sonrası). */
    PLAY_HEAL,

    /** 0 → 116 (yanlış cevap sonrası). */
    PLAY_BREAK_TO_HOLD_116,

    /** 116. karede sabit dur. */
    SHOW_HOLD_116,

    /** İlk kare (0). */
    SHOW_FIRST_FRAME,
}

/** Kart tıklamasının ilk aşaması (meydan okuma yüklenmeden önce, eldeki kart durumuna göre). */
enum class DailyQuestionCardClickAction {
    SHOW_CHALLENGE_COMPLETE_TOAST,
    IGNORE,
    LOAD_CHALLENGE,
}

/** Kart tıklamasının ikinci aşaması (meydan okuma yüklendikten sonra). */
enum class DailyQuestionChallengeClickAction {
    /** Ödül zaten alınmış ya da oynanacak slot yok: yalnızca dokunma kilidini bırak. */
    IGNORE,
    SHOW_CHALLENGE_COMPLETE_TOAST,
    SHOW_CONTINUE_PANEL,
    LAUNCH_LESSON,
}

/** Kart ilerleme çubuğunun bağlanırken izleyeceği plan. */
data class DailyQuestionProgressPlan(
    val solved: Int,
    val fromSolved: Int,
    val startPct: Float,
    val endPct: Float,
    val shouldAnimate: Boolean,
)

/** TasksFragment içindeki günlük soru kartı kuralları (görünümden bağımsız). */
object DailyQuestionCardLogic {
    const val DAILY_PROGRESS_ANIM_DURATION_MS = 2800L
    const val CLAIM_READY_VISUAL_PERCENT = 99.5f

    /** Periyot devrinde art arda yenilemeyi engelleyen en kısa aralık. */
    const val PERIOD_ROLLOVER_REFRESH_MIN_INTERVAL_MS = 400L

    /** Günlük soru ödül sandığının başlangıç nadirliği (`NewChestFragment.ChestRarity.RARE`). */
    val REWARD_CHEST_START_RARITY = ChestRarity.RARE

    /**
     * `DailyQuestionViewHolder.bind` içindeki ilerleme hesabı.
     * [lastShown]: [DailyQuestionCardProgressAnimStore.getLastShown]; kart yüklenmemişse (isLoaded=false)
     * Android depoyu hiç okumaz ve doğrudan [DailyQuestionProgressPlan.endPct] uygular — o durumda
     * `shouldAnimate=false`, `startPct=endPct` döner.
     */
    fun progressPlan(state: DailyQuestionCardUiState, lastShown: Int?): DailyQuestionProgressPlan {
        val total = state.totalCount.coerceAtLeast(1)
        val solved = state.solvedCount.coerceIn(0, total)
        val endPct = (solved * 100f) / total

        if (!state.isLoaded) {
            return DailyQuestionProgressPlan(
                solved = solved,
                fromSolved = solved,
                startPct = endPct,
                endPct = endPct,
                shouldAnimate = false,
            )
        }

        val fromSolved = when {
            lastShown == null -> solved
            lastShown > solved -> solved
            else -> lastShown
        }
        val startPct = (fromSolved * 100f) / total
        val shouldAnimate = lastShown != null &&
            solved > fromSolved &&
            abs(endPct - startPct) >= 0.01f
        return DailyQuestionProgressPlan(
            solved = solved,
            fromSolved = fromSolved,
            startPct = startPct,
            endPct = endPct,
            shouldAnimate = shouldAnimate,
        )
    }

    /** `persistProgressShown`: depoya yazılacak değer; kart yüklenmemişse null (yazılmaz). */
    fun solvedToPersist(ui: DailyQuestionCardUiState): Int? {
        if (!ui.isLoaded) return null
        return ui.solvedCount.coerceIn(0, ui.totalCount.coerceAtLeast(1))
    }

    fun isProgressClaimTapReady(
        ui: DailyQuestionCardUiState?,
        lastVisualPercent: Float,
        animatorRunning: Boolean,
    ): Boolean {
        if (ui == null) return false
        return ui.canClaimReward &&
            lastVisualPercent >= CLAIM_READY_VISUAL_PERCENT &&
            !animatorRunning
    }

    /** `updateProgressClaimTapEnabled`: ödül alınmadıysa ilerleme bölgesi tıklanabilir. */
    fun canTapProgress(ui: DailyQuestionCardUiState?): Boolean = ui?.rewardClaimed != true

    /**
     * Odak etiketi (`R.string.daily_question_focus_format` içine konacak metin).
     * Tamamlandıysa ya da zorluk boşsa null → etiket gizlenir.
     */
    fun focusLabelArgument(state: DailyQuestionCardUiState): String? {
        if (state.isComplete || state.difficulty.isBlank()) return null
        val rateText = state.globalSuccessRate?.let { " - Başarı: %$it" } ?: ""
        return state.difficulty + rateText
    }

    /** Geri sayım tıkı: süre varsa 1 sn, bittiyse (devir bekleniyor) 250 ms sonra yeniden. */
    fun countdownTickDelayMs(remainingMs: Long): Long = if (remainingMs > 0L) 1000L else 250L

    /** `bindBrokenHeart`: depodaki istekleri Android'deki sırayla tüketir. */
    fun brokenHeartAction(
        state: DailyQuestionCardUiState,
        store: DailyQuestionBrokenHeartStore,
    ): DailyQuestionBrokenHeartAction {
        if (!state.isLoaded) return DailyQuestionBrokenHeartAction.SHOW_FIRST_FRAME
        return when {
            store.consumeHealPlay(state.periodKey) -> DailyQuestionBrokenHeartAction.PLAY_HEAL
            store.consumePlayRequest(state.periodKey) -> DailyQuestionBrokenHeartAction.PLAY_BREAK_TO_HOLD_116
            store.isBrokenHold116(state.periodKey) -> DailyQuestionBrokenHeartAction.SHOW_HOLD_116
            else -> DailyQuestionBrokenHeartAction.SHOW_FIRST_FRAME
        }
    }

    /** `handleDailyQuestionCardClick` — ilk aşama. */
    fun cardClickAction(state: DailyQuestionCardUiState): DailyQuestionCardClickAction = when {
        state.shouldShowChallengeCompleteToast() -> DailyQuestionCardClickAction.SHOW_CHALLENGE_COMPLETE_TOAST
        state.rewardClaimed -> DailyQuestionCardClickAction.IGNORE
        else -> DailyQuestionCardClickAction.LOAD_CHALLENGE
    }

    /** `handleDailyQuestionCardClick` — meydan okuma yüklendikten sonraki karar. */
    fun challengeClickAction(challenge: DailyQuestionChallenge): DailyQuestionChallengeClickAction = when {
        challenge.rewardClaimed -> DailyQuestionChallengeClickAction.IGNORE
        challenge.isComplete ||
            challenge.solvedCount >= DailyQuestionPeriod.QUESTIONS_PER_PERIOD ->
            DailyQuestionChallengeClickAction.SHOW_CHALLENGE_COMPLETE_TOAST
        challenge.needsDiamondContinue -> DailyQuestionChallengeClickAction.SHOW_CONTINUE_PANEL
        challenge.slotForPlay() == null -> DailyQuestionChallengeClickAction.IGNORE
        else -> DailyQuestionChallengeClickAction.LAUNCH_LESSON
    }

    /**
     * `onDailyQuestionPeriodRolledOver`: kart yenilensin mi?
     * (Çağıran bundan önce tamamlama bildirimi bayrağını sıfırlar ve eski periyodun kırık kalp
     * anahtarlarını [DailyQuestionBrokenHeartStore.resetForPeriod] ile temizler.)
     */
    fun shouldRefreshOnPeriodRollover(
        cardPeriodKey: String,
        currentPeriodKey: String,
        nowMs: Long,
        lastRefreshMs: Long,
    ): Boolean {
        if (cardPeriodKey == currentPeriodKey) return false
        if (nowMs - lastRefreshMs < PERIOD_ROLLOVER_REFRESH_MIN_INTERVAL_MS) return false
        return true
    }
}
