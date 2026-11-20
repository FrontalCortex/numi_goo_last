// Kaynak (Android): app/src/main/java/com/example/app/BadgeDetailFragment.kt — görünümden bağımsız kısım:
// BadgeDetailUiState, personalRecordKinds / awardKinds, renderPersonalRecords ve renderAwardsGrid sıralaması,
// levelSortScore, toDetailUiState, getRenderConfig, BadgeRenderConfig, frameToProgress.
// R.drawable.bg_badge_circle_frame -> "bg_badge_circle_frame". Lottie/görünüm kodu taşınmadı.
package com.sorobit.shared.kupa.rozet

object BadgeDetailLogic {

    data class BadgeDetailUiState(
        val title: String,
        val subtext: String,
        val progressCount: Int,
        val showCount: Boolean,
        val targetMode: BadgeAnimMode,
        val unlocked: Boolean = true,
        val levelTone: BadgeLevelTone? = null,
        val baseAsset: String? = null,
        val topAsset: String,
        val topHoldFrame: Float,
        val topScale: Float = 1f,
        /** Android `R.drawable.<ad>` kaynak adı; yoksa null. */
        val topBackgroundRes: String? = null,
    )

    val personalRecordKinds = listOf(
        BadgeKind.GOLD,
        BadgeKind.SILVER,
        BadgeKind.BRONZE,
        BadgeKind.CUP,
        BadgeKind.KARATE,
    )

    val awardKinds = listOf(
        BadgeKind.DART,
        BadgeKind.ROCKET,
        BadgeKind.BOWLING,
        BadgeKind.FISHING,
        BadgeKind.GOLF,
        BadgeKind.TORNADO,
        BadgeKind.VOLCANO,
        BadgeKind.DINO,
        BadgeKind.CROCODILE,
        BadgeKind.GOAT,
        BadgeKind.EAGLE,
        BadgeKind.FLY,
        BadgeKind.TURTLE,
    )

    /** Ödül ızgarasında satır başına rozet sayısı (Android: `chunked(3)`; eksik hücreler boşlukla doldurulur). */
    const val AWARDS_PER_ROW = 3

    /**
     * Android `renderPersonalRecords` sırası: açık olanlar önce, eşitlikte [personalRecordKinds] sırası.
     * Durumlar [BadgeProgressRepository]'den okunur.
     */
    fun personalRecordBadges(): List<BadgeDetailUiState> {
        val statesByKind = BadgeProgressRepository.getStates().associateBy { it.kind }
        return personalRecordKinds
            .mapNotNull { kind -> statesByKind[kind] }
            .sortedWith(compareByDescending<BadgeState> { it.unlocked }.thenBy { personalRecordKinds.indexOf(it.kind) })
            .map { toDetailUiState(it) }
    }

    /**
     * Android `renderAwardsGrid` sırası: ton puanı (mor > özgün > kırmızı > gümüş > bronz > kilitli) azalan,
     * sonra değer azalan, sonra [awardKinds] sırası.
     */
    fun awardBadges(): List<BadgeDetailUiState> {
        val statesByKind = BadgeProgressRepository.getStates().associateBy { it.kind }
        return awardKinds
            .mapNotNull { kind -> statesByKind[kind] }
            .sortedWith(
                compareByDescending<BadgeState> { levelSortScore(it.levelTone) }
                    .thenByDescending { it.value ?: 0 }
                    .thenBy { awardKinds.indexOf(it.kind) },
            )
            .map { toDetailUiState(it) }
    }

    /** [awardBadges] listesinin 3'lü satırlara bölünmüş hâli. */
    fun awardBadgeRows(): List<List<BadgeDetailUiState>> = awardBadges().chunked(AWARDS_PER_ROW)

    fun levelSortScore(tone: BadgeLevelTone?): Int {
        return when (tone) {
            BadgeLevelTone.PURPLE -> 5
            BadgeLevelTone.ORIGINAL -> 4
            BadgeLevelTone.RED -> 3
            BadgeLevelTone.SILVER -> 2
            BadgeLevelTone.BRONZE -> 1
            null -> 0
        }
    }

    fun toDetailUiState(state: BadgeState): BadgeDetailUiState {
        val renderConfig = getRenderConfig(state.kind)
        val progress = BadgeProgressRepository.getUserBadgeProgress()
        val pieceCountSubtext = when (state.kind) {
            BadgeKind.GOLD -> progress.goldMedalPiece.size.takeIf { it > 0 }?.let { "${it}x" }
            BadgeKind.SILVER -> progress.silverMedalPiece.size.takeIf { it > 0 }?.let { "${it}x" }
            BadgeKind.BRONZE -> progress.bronzeMedalPiece.size.takeIf { it > 0 }?.let { "${it}x" }
            BadgeKind.CUP -> progress.cupPiece.size.takeIf { it > 0 }?.let { "${it}x" }
            else -> null
        }
        val subtext = when {
            pieceCountSubtext != null -> pieceCountSubtext
            state.kind in awardKinds ->
                BadgeProgressRepository.formatLeveledTierRatio(state.kind.mode) ?: state.kind.subtext
            else -> state.kind.subtext
        }
        return BadgeDetailUiState(
            title = state.kind.title,
            subtext = subtext,
            progressCount = state.value ?: 0,
            showCount = state.showValue,
            targetMode = state.kind.mode,
            unlocked = state.unlocked,
            levelTone = state.levelTone,
            baseAsset = renderConfig.baseAsset,
            topAsset = renderConfig.topAsset,
            topHoldFrame = renderConfig.topHoldFrame,
            topScale = renderConfig.topScale,
            topBackgroundRes = renderConfig.topBackgroundRes,
        )
    }

    fun getRenderConfig(kind: BadgeKind): BadgeRenderConfig {
        return when (kind) {
            BadgeKind.CUP -> BadgeRenderConfig(
                baseAsset = null,
                topAsset = "cup_google_anim.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.KARATE -> BadgeRenderConfig(
                baseAsset = null,
                topAsset = "karate_anim.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.GOLD -> BadgeRenderConfig(
                baseAsset = null,
                topAsset = "gold_medal_anim.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.SILVER -> BadgeRenderConfig(
                baseAsset = null,
                topAsset = "silver_medal_anim.json.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.BRONZE -> BadgeRenderConfig(
                baseAsset = null,
                topAsset = "bronze_medal_anim.json.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.DART -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "dart_anim.json",
                topHoldFrame = 60f,
                topScale = 0.65f,
                topBackgroundRes = null,
            )

            BadgeKind.ROCKET -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "rocket_badge_anim2.json",
                topHoldFrame = 60f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.BOWLING -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "bowling_anim.json",
                topHoldFrame = 47f,
                topScale = 0.68f,
                topBackgroundRes = "bg_badge_circle_frame",
            )

            BadgeKind.FISHING -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "fishing_pole_anim.json",
                topHoldFrame = 24f,
                topScale = 0.65f,
                topBackgroundRes = null,
            )

            BadgeKind.GOLF -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "golf_anim.json",
                topHoldFrame = 30f,
                topScale = 0.60f,
                topBackgroundRes = null,
            )

            BadgeKind.TORNADO -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "tornado_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.65f,
                topBackgroundRes = "bg_badge_circle_frame",
            )

            BadgeKind.VOLCANO -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "volcano_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.65f,
                topBackgroundRes = "bg_badge_circle_frame",
            )
            BadgeKind.DINO -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "dinosaur_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.5f,
                topBackgroundRes = "bg_badge_circle_frame",
            )
            BadgeKind.CROCODILE -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "crocodile_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.50f,
                topBackgroundRes = "bg_badge_circle_frame",
            )
            BadgeKind.GOAT -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "goat_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.50f,
                topBackgroundRes = "bg_badge_circle_frame",
            )
            BadgeKind.EAGLE -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "eagle_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.50f,
                topBackgroundRes = "bg_badge_circle_frame",
            )
            BadgeKind.FLY -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "fly_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.50f,
                topBackgroundRes = "bg_badge_circle_frame",
            )
            BadgeKind.TURTLE -> BadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "turtle_anim.json",
                topHoldFrame = 9999f,
                topScale = 0.50f,
                topBackgroundRes = "bg_badge_circle_frame",
            )
        }
    }


    data class BadgeRenderConfig(
        val baseAsset: String?,
        val topAsset: String,
        val topHoldFrame: Float,
        val topScale: Float,
        /** Android `R.drawable.<ad>` kaynak adı; yoksa null. */
        val topBackgroundRes: String?,
    )

    /** Android `BadgeDetailFragment.frameToProgress` ([BadgeFragmentLogic.frameToProgress]'ten farklıdır). */
    fun frameToProgress(frame: Float, startFrame: Float, endFrame: Float): Float {
        if (endFrame <= startFrame) return 0f
        return ((frame - startFrame) / (endFrame - startFrame)).coerceIn(0f, 1f)
    }
}
