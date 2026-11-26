// Kaynak (yalnızca gömülü saf hesaplar; görünüm kodu taşınmadı):
//  - MissionsFragment.kt: updateMissionsUI (ilerleme = min(sayaç, hedef)), createQuestItemView
//    (yüzde / durum / etiket), onQuestClicked (talep kuralı, sandık nadirliği), formatWeeklyCountdown
//  - MissionChestRewardFragment.kt: renderRewardList (değişen görevlerin süzülmesi, sıra dizini),
//    createAnimatedQuestView (başlangıç/bitiş yüzdesi, kare başına durum), formatWeeklyCountdownForReward
//  - MissionProgressLayout.kt: applyMissionProgressOverlayNow / applyDailyQuestionProgressOverlayNow
//    (dolgu ve parlama genişliği, kaynak seçimi)
//  - ChestFragment.kt: countCompletedMissions, missionCounterDelta, isPerfectLesson,
//    shouldIncrementDartProgress
// Farklar: R.drawable / R.string → kaynak adı (String). Metinler kaynak anahtarı + argüman olarak döner.
package com.sorobit.shared.oyun

import kotlin.math.roundToInt

/** Görev satırının durumu (başlık rengi ve ilerleme etiketi buna göre seçilir). */
enum class MissionQuestState {
    /** Ödül alınmış: etiket `mission_reward_claimed_label`. */
    CLAIMED,

    /** Hedefe ulaşılmış, ödül bekliyor: etiket `mission_completed_label`, başlık altın. */
    DONE,

    /** Sürüyor: etiket "ilerleme / hedef". */
    PENDING,
}

/** Kaynak anahtarı + tek sayı argümanı (örn. "missions_hours_short", 5 → "5 SA."). */
data class MissionCountdownLabel(val resKey: String, val value: Int)

data class MissionQuestData(
    val missionId: String,
    /** String kaynağının adı (Android: getString(mission.titleResId)). */
    val titleResId: String,
    val progress: Int,
    val target: Int,
    /** Drawable kaynağının adı. */
    val iconRes: String,
    val window: MissionWindow,
    val isClaimed: Boolean,
) {
    val done: Boolean get() = progress >= target

    /** `createQuestItemView`: çubuğa verilen tam sayı yüzde. */
    val percent: Int
        get() = ((progress.coerceAtMost(target) * 100f) / target.coerceAtLeast(1)).coerceIn(0f, 100f).toInt()

    val state: MissionQuestState
        get() = when {
            isClaimed -> MissionQuestState.CLAIMED
            done -> MissionQuestState.DONE
            else -> MissionQuestState.PENDING
        }

    /** PENDING durumundaki "%d / %d" etiketinin argümanları. */
    val progressLabelArgs: Pair<Int, Int> get() = progress.coerceAtMost(target) to target

    /** Satır tıklanabilir mi / ödül sandığı açılabilir mi. */
    val canClaim: Boolean get() = done && !isClaimed
}

data class AnimatedQuest(
    val missionId: String,
    val window: MissionWindow,
    val isClaimed: Boolean,
    val titleResId: String,
    val iconRes: String,
    val target: Int,
    val fromCount: Int,
    val toCount: Int,
    val staggerIndex: Int,
) {
    private val safeTarget: Int get() = target.coerceAtLeast(1)

    val startPct: Float get() = (fromCount.coerceAtMost(safeTarget) * 100f) / safeTarget
    val endPct: Float get() = (toCount.coerceAtMost(safeTarget) * 100f) / safeTarget
    val canClaim: Boolean get() = toCount >= safeTarget && !isClaimed

    /** Animasyon başlamadan önceki bekleme (satırlar sırayla dolsun diye). */
    val startDelayMs: Long get() = staggerIndex * MissionUiLogic.STAGGER_DELAY_MS

    /** Başlangıç ve bitiş neredeyse aynıysa animasyon oynatılmaz. */
    val shouldAnimate: Boolean get() = kotlin.math.abs(endPct - startPct) >= 0.01f

    /** Animasyonun [pct] yüzdesindeki tahmini sayaç değeri (0..hedef). */
    fun estimatedCountAt(pct: Float): Int = (safeTarget * pct / 100f).roundToInt().coerceIn(0, safeTarget)

    /** Animasyonun [pct] yüzdesinde satırın durumu. */
    fun stateAt(pct: Float): MissionQuestState = when {
        isClaimed -> MissionQuestState.CLAIMED
        estimatedCountAt(pct) >= safeTarget -> MissionQuestState.DONE
        else -> MissionQuestState.PENDING
    }
}

/** İlerleme çubuğu katmanının ölçüleri ve kaynakları. */
data class MissionProgressOverlay(
    val fillWidth: Int,
    val fillVisible: Boolean,
    val shineWidth: Int,
    val shineVisible: Boolean,
    val fillDrawable: String,
    val shineDrawable: String,
)

object MissionUiLogic {
    const val ICON_WEEKLY = "new_chest_close_ic2"
    const val ICON_DAILY = "new_chest_close_ic1"
    const val RES_HOURS_SHORT = "missions_hours_short"
    const val RES_DAYS_SHORT = "missions_days_short"
    const val RES_PROGRESS_FORMAT = "mission_progress_format"
    const val RES_COMPLETED_LABEL = "mission_completed_label"
    const val RES_REWARD_CLAIMED_LABEL = "mission_reward_claimed_label"

    /** Görev ilerleme animasyonu süresi ve satırlar arası gecikme. */
    const val PROGRESS_ANIM_DURATION_MS = 2800L
    const val STAGGER_DELAY_MS = 120L

    /** Görev ödül sandığı kapanınca yayınlanan fragment sonucu anahtarı. */
    const val RESULT_CHEST_CLOSED = "chest_closed"

    private const val HOUR_MS = 1000L * 60 * 60
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * Haftalık geri sayım: 24 saat ve üzeri → gün (yukarı yuvarlanır), altı → saat (aşağı, en az 1).
     */
    fun formatWeeklyCountdown(ms: Long): MissionCountdownLabel {
        val hoursTotal = (ms / HOUR_MS).toInt().coerceAtLeast(1)
        return if (ms >= DAY_MS) {
            val days = ((ms + DAY_MS - 1) / DAY_MS).toInt().coerceAtLeast(1)
            MissionCountdownLabel(RES_DAYS_SHORT, days)
        } else {
            MissionCountdownLabel(RES_HOURS_SHORT, hoursTotal)
        }
    }

    fun dailyCountdownLabel(hoursUntilDailyReset: Int): MissionCountdownLabel =
        MissionCountdownLabel(RES_HOURS_SHORT, hoursUntilDailyReset)

    fun iconForWindow(window: MissionWindow): String = when (window) {
        MissionWindow.WEEKLY -> ICON_WEEKLY
        MissionWindow.DAILY -> ICON_DAILY
    }

    /** `updateMissionsUI`: bir görev satırının verisi. */
    fun questData(
        mission: MissionQuestDefinition,
        window: MissionWindow,
        snapshot: MissionsProgressStore.Snapshot,
        isClaimed: Boolean,
    ): MissionQuestData = MissionQuestData(
        missionId = mission.id,
        titleResId = mission.titleResId,
        progress = minOf(MissionsProgressStore.missionProgress(snapshot, window, mission), mission.target),
        target = mission.target,
        iconRes = iconForWindow(window),
        window = window,
        isClaimed = isClaimed,
    )

    /** Görev ödül sandığının başlangıç nadirliği: haftalık → ENDER, günlük → SIRADAN. */
    fun chestStartRarityForWindow(window: MissionWindow): ChestRarity =
        if (window == MissionWindow.WEEKLY) ChestRarity.RARE else ChestRarity.COMMON

    /**
     * `MissionChestRewardFragment.renderRewardList`: ilerlemesi (hedefe kırpılmış hâliyle) değişen
     * görevler. Sıra dizini önce haftalıklara, sonra günlüklere verilir.
     * @return (haftalık liste, günlük liste)
     */
    fun animatedQuests(
        weeklyMissions: List<MissionQuestDefinition>,
        dailyMissions: List<MissionQuestDefinition>,
        before: MissionsProgressStore.Snapshot,
        after: MissionsProgressStore.Snapshot,
        isClaimed: (MissionWindow, String) -> Boolean,
    ): Pair<List<AnimatedQuest>, List<AnimatedQuest>> {
        var stagger = 0
        fun collect(missions: List<MissionQuestDefinition>, window: MissionWindow): List<AnimatedQuest> {
            val out = mutableListOf<AnimatedQuest>()
            missions.forEach { mission ->
                val beforeCount = minOf(
                    MissionsProgressStore.missionProgress(before, window, mission),
                    mission.target,
                )
                val afterCount = minOf(
                    MissionsProgressStore.missionProgress(after, window, mission),
                    mission.target,
                )
                if (beforeCount != afterCount) {
                    val claimed = isClaimed(window, mission.id)
                    out.add(
                        AnimatedQuest(
                            missionId = mission.id,
                            window = window,
                            isClaimed = claimed,
                            titleResId = mission.titleResId,
                            iconRes = iconForWindow(window),
                            target = mission.target,
                            fromCount = beforeCount,
                            toCount = afterCount,
                            staggerIndex = stagger++,
                        ),
                    )
                }
            }
            return out
        }
        val weekly = collect(weeklyMissions, MissionWindow.WEEKLY)
        val daily = collect(dailyMissions, MissionWindow.DAILY)
        return weekly to daily
    }

    /** `ChestFragment.countCompletedMissions`: bu talepte hedefi İLK KEZ aşan görev sayısı. */
    fun countCompletedMissions(
        dailyMissions: List<MissionQuestDefinition>,
        weeklyMissions: List<MissionQuestDefinition>,
        before: MissionsProgressStore.Snapshot,
        after: MissionsProgressStore.Snapshot,
    ): Int {
        val dailyCompleted = dailyMissions.count { mission ->
            val beforeCount = MissionsProgressStore.missionProgress(before, MissionWindow.DAILY, mission)
            val afterCount = MissionsProgressStore.missionProgress(after, MissionWindow.DAILY, mission)
            beforeCount < mission.target && afterCount >= mission.target
        }
        val weeklyCompleted = weeklyMissions.count { mission ->
            val beforeCount = MissionsProgressStore.missionProgress(before, MissionWindow.WEEKLY, mission)
            val afterCount = MissionsProgressStore.missionProgress(after, MissionWindow.WEEKLY, mission)
            beforeCount < mission.target && afterCount >= mission.target
        }
        return dailyCompleted + weeklyCompleted
    }

    /** `ChestFragment`: herhangi bir görev sayacı değişti mi (data class eşitliğinin tersi). */
    fun missionCounterDelta(before: MissionsProgressStore.Snapshot, after: MissionsProgressStore.Snapshot): Boolean =
        before.dailyStepFinishCount != after.dailyStepFinishCount ||
            before.weeklyStepFinishCount != after.weeklyStepFinishCount ||
            before.dailyStepIncrementCount != after.dailyStepIncrementCount ||
            before.weeklyStepIncrementCount != after.weeklyStepIncrementCount ||
            before.dailyPerfectStepIncrementCount != after.dailyPerfectStepIncrementCount ||
            before.weeklyPerfectStepIncrementCount != after.weeklyPerfectStepIncrementCount ||
            before.dailyChestRecordBreakCount != after.dailyChestRecordBreakCount ||
            before.weeklyChestRecordBreakCount != after.weeklyChestRecordBreakCount ||
            before.dailyChestStarGainCount != after.dailyChestStarGainCount ||
            before.weeklyChestStarGainCount != after.weeklyChestStarGainCount ||
            before.dailyLearnMinutesCount != after.dailyLearnMinutesCount ||
            before.weeklyLearnMinutesCount != after.weeklyLearnMinutesCount

    /** `ChestFragment`: ders hatasız mı. */
    fun isPerfectLesson(lessonSuccessRate: Float): Boolean = lessonSuccessRate >= 100f

    /** `ChestFragment`: dart rozeti ilerlesin mi (ilk kez bitirilen, hatasız normal ders). */
    fun shouldIncrementDartProgress(lessonBeforeClaim: OyunLessonItem?, lessonSuccessRate: Float): Boolean =
        when (val item = lessonBeforeClaim) {
            null -> false
            else ->
                item.type == OyunLessonItem.TYPE_LESSON &&
                    !item.stepIsFinish &&
                    isPerfectLesson(lessonSuccessRate)
        }

    /** `ChestFragment`: sandık adımı ilk kez bitiyorsa puanlama isteği için bayrak kalkar. */
    fun justFinishedChestForRating(lessonBeforeClaim: OyunLessonItem?): Boolean =
        lessonBeforeClaim?.type == OyunLessonItem.TYPE_CHEST && lessonBeforeClaim.stepIsFinish != true

    /**
     * `applyMissionProgressOverlayNow`. [trackWidth] <= 0 ise Android hiçbir şey yapmaz → null.
     * [startInset] / [endGap]: `mission_progress_shine_inset_start` / `mission_progress_shine_gap_end` (px).
     */
    fun missionProgressOverlay(
        trackWidth: Int,
        percent: Float,
        done: Boolean,
        claimed: Boolean,
        startInset: Int,
        endGap: Int,
    ): MissionProgressOverlay? {
        val w = trackWidth
        if (w <= 0) return null
        val p = percent.coerceIn(0f, 100f)
        val fillDrawable = when {
            claimed -> "mission_progress_fill_claimed"
            done -> "mission_progress_fill_gold"
            else -> "mission_progress_fill_blue"
        }
        val shineDrawable = when {
            claimed -> "mission_progress_shine_claimed"
            done -> "mission_progress_shine_gold"
            else -> "mission_progress_shine_blue"
        }
        val fillW = (w * p / 100f).toInt().coerceIn(0, w)
        val shineW = (fillW - startInset - endGap).coerceAtLeast(0)
        return MissionProgressOverlay(
            fillWidth = fillW,
            fillVisible = fillW > 0,
            // Ödül alındıysa parlama gizlenir (genişliği de güncellenmez; burada hesaplanan değer bilgi amaçlı).
            shineWidth = shineW,
            shineVisible = if (claimed) false else shineW > 0,
            fillDrawable = fillDrawable,
            shineDrawable = shineDrawable,
        )
    }

    /** `applyDailyQuestionProgressOverlayNow`: turuncu dolgu; [complete] ise altın (3/3). */
    fun dailyQuestionProgressOverlay(
        trackWidth: Int,
        percent: Float,
        complete: Boolean,
        startInset: Int,
        endGap: Int,
    ): MissionProgressOverlay? {
        val w = trackWidth
        if (w <= 0) return null
        val p = percent.coerceIn(0f, 100f)
        val fillW = (w * p / 100f).toInt().coerceIn(0, w)
        val shineW = (fillW - startInset - endGap).coerceAtLeast(0)
        return MissionProgressOverlay(
            fillWidth = fillW,
            fillVisible = fillW > 0,
            shineWidth = shineW,
            shineVisible = shineW > 0,
            fillDrawable = if (complete) "daily_question_progress_fill_complete" else "daily_question_progress_fill",
            shineDrawable = if (complete) "daily_question_progress_shine_complete" else "daily_question_progress_shine",
        )
    }
}
