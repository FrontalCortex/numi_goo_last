package com.example.app

import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.example.app.databinding.FragmentBadgeDetailBinding
import com.example.app.databinding.ItemBadgeDetailAwardBinding
import com.example.app.databinding.ItemBadgeDetailRecordBinding

class BadgeDetailFragment : Fragment() {

    private var _binding: FragmentBadgeDetailBinding? = null
    private val binding: FragmentBadgeDetailBinding
        get() = _binding!!
    private val lockedBaseColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_locked_base)
    private val lockedTopColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_locked_top)
    private val unlockedValueStrokeColor = 0xFFFF8F00.toInt()
    private val bronzeToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_bronze_filter)
    private val silverToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_silver_filter)
    private val redToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_red_filter)
    private val purpleToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_purple_filter)

    private data class BadgeDetailUiState(
        val title: String,
        val subtext: String,
        val progressCount: Int,
        val showCount: Boolean,
        val targetMode: BadgeFragment.BadgeAnimMode,
        val unlocked: Boolean = true,
        val levelTone: BadgeLevelTone? = null,
        val baseAsset: String? = null,
        val topAsset: String,
        val topHoldFrame: Float,
        val topScale: Float = 1f,
        val topBackgroundRes: Int? = null,
    )

    private val personalRecordKinds = listOf(
        BadgeKind.GOLD,
        BadgeKind.SILVER,
        BadgeKind.BRONZE,
        BadgeKind.CUP,
        BadgeKind.KARATE,
    )

    private val awardKinds = listOf(
        BadgeKind.DART,
        BadgeKind.ROCKET,
        BadgeKind.BOWLING,
        BadgeKind.FISHING,
        BadgeKind.GOLF,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBadgeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBadgeDetailBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        renderPersonalRecords()
        renderAwardsGrid()
    }

    private fun renderPersonalRecords() {
        val container = binding.personalRecordsContainer
        container.removeAllViews()
        val statesByKind = BadgeProgressRepository.getStates().associateBy { it.kind }
        val recordBadges = personalRecordKinds
            .mapNotNull { kind -> statesByKind[kind] }
            .sortedWith(compareByDescending<BadgeState> { it.unlocked }.thenBy { personalRecordKinds.indexOf(it.kind) })
            .map { toDetailUiState(it) }
        recordBadges.forEach { badge ->
            val itemBinding = ItemBadgeDetailRecordBinding.inflate(layoutInflater, container, false)
            bindSingleBadge(
                container = itemBinding.recordItemContainer,
                base = itemBinding.recordBadgeBase,
                top = itemBinding.recordBadgeTop,
                valueText = itemBinding.recordBadgeValue,
                titleText = itemBinding.recordBadgeTitle,
                subtextText = itemBinding.recordBadgeSubtext,
                state = badge,
                valueHiddenVisibility = View.INVISIBLE,
            )
            container.addView(itemBinding.root)
        }
    }

    private fun renderAwardsGrid() {
        val rowsContainer = binding.awardsRowsContainer
        rowsContainer.removeAllViews()
        val statesByKind = BadgeProgressRepository.getStates().associateBy { it.kind }
        val awardBadges = awardKinds
            .mapNotNull { kind -> statesByKind[kind] }
            .sortedWith(
                compareByDescending<BadgeState> { levelSortScore(it.levelTone) }
                    .thenByDescending { it.value ?: 0 }
                    .thenBy { awardKinds.indexOf(it.kind) },
            )
            .map { toDetailUiState(it) }
        val chunked = awardBadges.chunked(3)
        chunked.forEach { rowBadges ->
            val row = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                orientation = LinearLayout.HORIZONTAL
            }

            rowBadges.forEach { badge ->
                val itemBinding = ItemBadgeDetailAwardBinding.inflate(layoutInflater, row, false)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                itemBinding.root.layoutParams = lp
                bindSingleBadge(
                    container = itemBinding.awardItemContainer,
                    base = itemBinding.awardBadgeBase,
                    top = itemBinding.awardBadgeTop,
                    valueText = itemBinding.awardBadgeValue,
                    titleText = itemBinding.awardBadgeTitle,
                    subtextText = itemBinding.awardBadgeSubtext,
                    state = badge,
                )
                row.addView(itemBinding.root)
            }

            val placeholders = 3 - rowBadges.size
            repeat(placeholders) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }
                row.addView(spacer)
            }

            rowsContainer.addView(row)
        }
    }

    private fun levelSortScore(tone: BadgeLevelTone?): Int {
        return when (tone) {
            BadgeLevelTone.PURPLE -> 5
            BadgeLevelTone.ORIGINAL -> 4
            BadgeLevelTone.RED -> 3
            BadgeLevelTone.SILVER -> 2
            BadgeLevelTone.BRONZE -> 1
            null -> 0
        }
    }

    private fun bindSingleBadge(
        container: View,
        base: LottieAnimationView,
        top: LottieAnimationView,
        valueText: TextView,
        titleText: TextView,
        subtextText: TextView,
        state: BadgeDetailUiState,
        /** [showCount] false iken değer alanı: kişisel rekorlarda yer tutmak için [View.INVISIBLE]. */
        valueHiddenVisibility: Int = View.GONE,
    ) {
        if (state.baseAsset == null) {
            base.cancelAnimation()
            base.visibility = View.INVISIBLE
        } else {
            base.visibility = View.VISIBLE
            base.setAnimation(state.baseAsset)
            base.repeatCount = 0
            base.speed = 1f
            base.addLottieOnCompositionLoadedListener {
                base.progress = 1f
                base.pauseAnimation()
            }
        }

        top.setAnimation(state.topAsset)
        top.repeatCount = 0
        top.speed = 1f
        top.scaleX = state.topScale
        top.scaleY = state.topScale
        top.translationY = -1f * resources.displayMetrics.density
        if (state.topBackgroundRes != null) {
            top.setBackgroundResource(state.topBackgroundRes)
        } else {
            top.background = null
        }
        top.addLottieOnCompositionLoadedListener { composition ->
            val progress = frameToProgress(state.topHoldFrame, composition.startFrame, composition.endFrame)
            top.progress = progress
            top.pauseAnimation()
        }

        titleText.text = state.title
        subtextText.text = state.subtext
        container.setOnClickListener {
            openBadgeFragment(state.targetMode, state.unlocked)
        }

        if (state.unlocked) {
            container.alpha = 1f
            clearLottieFlatGrayFilter(base)
            clearLottieFlatGrayFilter(top)
            applyUnlockedLevelToneToBase(base, state.levelTone)
            (valueText as? BadgeRewardTextView)?.setStrokeColorInt(unlockedValueStrokeColor)
            valueText.setTextColor(Color.WHITE)
            if (state.showCount) {
                valueText.visibility = View.VISIBLE
                valueText.text = state.progressCount.toString()
            } else {
                valueText.visibility = valueHiddenVisibility
            }
        } else {
            container.alpha = 1f
            if (base.visibility == View.VISIBLE) {
                applyLottieFlatGrayFilter(base, lockedBaseColor)
                applyLottieFlatGrayFilter(top, lockedTopColor)
            } else {
                applyLottieFlatGrayFilter(top, lockedTopColor)
            }
            if (state.showCount) {
                valueText.visibility = View.VISIBLE
                valueText.text = state.progressCount.toString()
                (valueText as? BadgeRewardTextView)?.setStrokeColorInt(lockedTopColor)
                valueText.setTextColor(lockedBaseColor)
            } else {
                valueText.visibility = valueHiddenVisibility
            }
        }
    }

    private fun toDetailUiState(state: BadgeState): BadgeDetailUiState {
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

    private fun applyUnlockedLevelToneToBase(base: LottieAnimationView, tone: BadgeLevelTone?) {
        if (base.visibility != View.VISIBLE) return
        when (tone) {
            BadgeLevelTone.BRONZE -> applyLottieToneFilter(base, bronzeToneColor)
            BadgeLevelTone.SILVER -> applyLottieToneFilter(base, silverToneColor)
            BadgeLevelTone.RED -> applyLottieToneFilter(base, redToneColor)
            BadgeLevelTone.PURPLE -> applyLottieToneFilterStrong(base, purpleToneColor)
            BadgeLevelTone.ORIGINAL, null -> clearLottieFlatGrayFilter(base)
        }
    }

    private fun applyLottieToneFilter(view: LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)),
        )
        view.invalidate()
    }

    private fun applyLottieToneFilterStrong(view: LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.OVERLAY)),
        )
        view.invalidate()
    }

    private fun getRenderConfig(kind: BadgeKind): BadgeRenderConfig {
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
                topBackgroundRes = R.drawable.bg_badge_circle_frame,
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
        }
    }

    private data class BadgeRenderConfig(
        val baseAsset: String?,
        val topAsset: String,
        val topHoldFrame: Float,
        val topScale: Float,
        val topBackgroundRes: Int?,
    )

    private fun frameToProgress(frame: Float, startFrame: Float, endFrame: Float): Float {
        if (endFrame <= startFrame) return 0f
        return ((frame - startFrame) / (endFrame - startFrame)).coerceIn(0f, 1f)
    }

    private fun applyLottieFlatGrayFilter(view: LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)),
        )
        view.invalidate()
    }

    private fun clearLottieFlatGrayFilter(view: LottieAnimationView) {
        view.clearValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
        )
        view.invalidate()
    }

    private fun openBadgeFragment(mode: BadgeFragment.BadgeAnimMode, unlocked: Boolean) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .replace(R.id.badgeFragmentContainter, BadgeFragment.newInstance(mode, unlocked))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
