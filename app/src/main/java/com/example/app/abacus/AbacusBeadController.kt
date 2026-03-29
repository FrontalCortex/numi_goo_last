package com.example.app.abacus

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.example.app.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared controller for the 5-rod soroban UI used in AbacusFragment and AbacusPracticeFragment.
 *
 * Rod mapping matches existing ids and number conversion logic:
 * - rod0 = ten-thousands, rod1 = thousands, rod2 = hundreds, rod3 = tens, rod4 = ones
 */
class AbacusBeadController(
    private val context: Context,
    private val root: View,
    private val animationDurationMs: Long = 300L
) {
    private val animatingBeads = mutableSetOf<ImageView>()
    private var touchEnabled: Boolean = true
    private var resetInProgress: Boolean = false

    private val DEBUG_TOUCH = true
    private val TAG = "AbacusBeadController"

    private fun d(msg: String) {
        if (DEBUG_TOUCH) Log.d(TAG, msg)
    }

    private val touchSlopPx: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var bottomMoveDistancePx: Float = AbacusBeadMetrics.bottomStepPx(context)
    private var topMoveDistancePx: Float = AbacusBeadMetrics.topStepPx(context)

    private var initialPositionsCaptured = false
    private val initialBottomMargins: Array<IntArray> = Array(5) { IntArray(4) }

    /** bottomCount[rod] in 0..4 (how many bottom beads are up) */
    private val bottomCount = IntArray(5) { 0 }
    /** topDown[rod] true if top bead is down (adds 5 for that rod) */
    private val topDown = BooleanArray(5) { false }

    private val bottomBeads: Array<Array<ImageView>> = Array(5) { rod ->
        arrayOf(
            findBead("rod${rod}_bead_bottom1"),
            findBead("rod${rod}_bead_bottom2"),
            findBead("rod${rod}_bead_bottom3"),
            findBead("rod${rod}_bead_bottom4")
        )
    }
    private val topBeads: Array<ImageView> = Array(5) { rod ->
        findBead("rod${rod}_bead_top")
    }

    private sealed class DragSession

    private data class BottomDragSession(
        val rod: Int,
        val beadNumber: Int,
        val startCount: Int,
        val targetCount: Int,
        val directionUp: Boolean,
        val affected: Array<ImageView>,
        val downRawY: Float,
        var isDragging: Boolean,
        var provisionalApplied: Boolean,
        var lastRawY: Float,
        var lastEventTimeMs: Long,
        var currentProgressPx: Float,
        var currentVelocityAbsPxPerMs: Float
    ) : DragSession()

    private data class TopDragSession(
        val rod: Int,
        val bead: ImageView,
        val startDown: Boolean,
        val targetDown: Boolean,
        val downRawY: Float,
        var isDragging: Boolean,
        var provisionalApplied: Boolean,
        var lastRawY: Float,
        var lastEventTimeMs: Long,
        var currentProgressPx: Float,
        var currentVelocityAbsPxPerMs: Float
    ) : DragSession()

    private var activeDrag: DragSession? = null

    private fun isAnyBottomAnimating(rod: Int): Boolean {
        return bottomBeads[rod].any { it in animatingBeads }
    }

    fun setup() {
        captureInitialPositionsIfNeeded()
        // Bottom beads (1..4) + top bead: click davranışını koruyarak drag ekliyoruz.
        for (rod in 0..4) {
            for (i in 1..4) {
                val beadView = bottomBeads[rod][i - 1]
                bindBottomBeadTouchListener(rod = rod, beadNumber = i, beadView = beadView)
            }
            val top = topBeads[rod]
            bindTopBeadTouchListener(rod = rod, beadView = top)
        }
    }

    fun setEnabled(enabled: Boolean) {
        touchEnabled = enabled
    }

    /**
     * Calculates runtime movement distances from barrier spacing and applies %100 ratio.
     * Returns true if dynamic measurement succeeded, false if fallback values remain in use.
     */
    fun computeMovementDistancesFromLayout(
        ratio: Float = 1.0f,
        force: Boolean = true,
    ): Boolean {
        if (!force && bottomMoveDistancePx > 0f && topMoveDistancePx > 0f) return true
        val dynamic = AbacusBeadMetrics.fromBarrierDistances(root, ratio)
        if (dynamic != null) {
            bottomMoveDistancePx = dynamic.bottomPx
            topMoveDistancePx = dynamic.topPx
            d("Dynamic distances applied bottom=$bottomMoveDistancePx top=$topMoveDistancePx")
            return true
        }
        // Keep dimen fallback if measurement fails.
        bottomMoveDistancePx = AbacusBeadMetrics.bottomStepPx(context)
        topMoveDistancePx = AbacusBeadMetrics.topStepPx(context)
        d("Dynamic distances unavailable; using fallback bottom=$bottomMoveDistancePx top=$topMoveDistancePx")
        return false
    }

    /** Alt boncuk adım mesafesi (px); dokunma/sürükleme ile aynı değer. */
    fun getBottomMoveDistancePx(): Float = bottomMoveDistancePx

    /** Üst boncuk adım mesafesi (px). */
    fun getTopMoveDistancePx(): Float = topMoveDistancePx

    /**
     * Sync controller internal state from current UI.
     * This is needed because tutorial/guide animations (e.g., BeadAnimation) can move beads
     * without going through this controller's click/drag handlers.
     */
    fun syncStateFromUi() {
        // During reset animations, syncStateFromUi can be called again from the fragment
        // (e.g., setupBeads()). If we re-read visual state mid-animation, it can reintroduce
        // selected/default desync. So we ignore sync while reset is in progress.
        if (resetInProgress) return

        captureInitialPositionsIfNeeded()

        val raisedThreshold = bottomMoveDistancePx / 2f

        for (rod in 0..4) {
            // Bottom count from *visual* position (margin + any in-flight translation).
            // Tutorial BeadAnimation / widget margin animasyonları boncukları oynatır ama drawable
            // (soroban_bead_selected) güncellemez; sadece drawable okumak bottomCount'u 0 yapıp
            // her showStep/setupBeads'te tutorial boolean'larını yanlış sıfırlıyordu.
            var count = 0
            for (i in 0..3) {
                val bead = bottomBeads[rod][i]
                val params = bead.layoutParams as ViewGroup.MarginLayoutParams
                val delta =
                    params.bottomMargin - initialBottomMargins[rod][i] + bead.translationY
                if (delta >= raisedThreshold) {
                    count = i + 1
                } else {
                    break
                }
            }
            bottomCount[rod] = count

            // Top bead state.
            val top = topBeads[rod]
            val topByTranslation = top.translationY >= (topMoveDistancePx / 2f)
            // Key fix: infer top state from visual position, not drawable.
            // Otherwise "selected" tint can stay while the bead is reset/moving.
            topDown[rod] = topByTranslation
        }

        updateAllAppearance()
    }

    private fun bindBottomBeadTouchListener(rod: Int, beadNumber: Int, beadView: ImageView) {
        beadView.isClickable = true
        beadView.isLongClickable = false
        beadView.setOnTouchListener { _, event ->
            if (!touchEnabled) return@setOnTouchListener true
            val action = event.actionMasked
            val actionDown = action == MotionEvent.ACTION_DOWN

            d("Bottom onTouch action=$actionDown?DOWN:${event.actionMasked} active=${activeDrag!=null} rod=$rod bead=$beadNumber")

            // If a previous session got stuck (no UP delivered), allow a new DOWN to restart.
            val active = activeDrag
            if (active != null && actionDown) {
                // Stale session cleanup.
                activeDrag = null
            }

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    d("Bottom ACTION_DOWN rod=$rod bead=$beadNumber rawY=${event.rawY} t=${event.eventTime}")
                    if (beadView in animatingBeads) return@setOnTouchListener true
                    // Rule: if any bottom bead in this rod is currently animating,
                    // block starting a new interaction on other bottom beads.
                    // This prevents "bottom2 dragging while bottom1 animation is running".
                    if (isAnyBottomAnimating(rod)) return@setOnTouchListener true

                    val startCount = bottomCount[rod]
                    val target = if (startCount >= beadNumber) beadNumber - 1 else beadNumber
                    val targetCount = target.coerceIn(0, 4)
                    val directionUp = targetCount > startCount
                    val clampedTarget = targetCount
                    if (clampedTarget == startCount) {
                        // No change; treat as tap.
                        activeDrag = BottomDragSession(
                            rod = rod,
                            beadNumber = beadNumber,
                            startCount = startCount,
                            targetCount = startCount,
                            directionUp = false,
                            affected = emptyArray(),
                            downRawY = event.rawY,
                            isDragging = false,
                            provisionalApplied = false,
                            lastRawY = event.rawY,
                            lastEventTimeMs = event.eventTime,
                            currentProgressPx = 0f,
                            currentVelocityAbsPxPerMs = 0f
                        )
                        return@setOnTouchListener true
                    }

                    val from = min(startCount, clampedTarget) + 1
                    val to = max(startCount, clampedTarget)
                    val affected = (from..to)
                        .map { n -> bottomBeads[rod][n - 1] }
                        .toTypedArray()

                    activeDrag = BottomDragSession(
                        rod = rod,
                        beadNumber = beadNumber,
                        startCount = startCount,
                        targetCount = clampedTarget,
                        directionUp = directionUp,
                        affected = affected,
                        downRawY = event.rawY,
                        isDragging = false,
                        provisionalApplied = false,
                        lastRawY = event.rawY,
                        lastEventTimeMs = event.eventTime,
                        currentProgressPx = 0f,
                        currentVelocityAbsPxPerMs = 0f
                    )
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val session = activeDrag as? BottomDragSession ?: return@setOnTouchListener false
                    if (session.affected.isEmpty()) {
                        // Nothing to drag; still wait for UP to perform tap.
                        return@setOnTouchListener true
                    }

                    val dy = event.rawY - session.downRawY
                    val progress = if (session.directionUp) {
                        (-dy).coerceIn(0f, bottomMoveDistancePx)
                    } else {
                        (dy).coerceIn(0f, bottomMoveDistancePx)
                    }

                    d("Bottom ACTION_MOVE rod=${session.rod} bead=$beadNumber dy=$dy progress=$progress dragging=${session.isDragging}")
                    val dtMs = (event.eventTime - session.lastEventTimeMs).coerceAtLeast(1L)
                    val deltaRawY = event.rawY - session.lastRawY
                    val velocityAbs = abs(deltaRawY) / dtMs.toFloat()
                    session.currentVelocityAbsPxPerMs = velocityAbs
                    session.lastRawY = event.rawY
                    session.lastEventTimeMs = event.eventTime

                    if (!session.isDragging && abs(dy) > touchSlopPx && progress > 0f) {
                        session.isDragging = true
                        // Provisional state: mimic click behavior where appearance updates immediately.
                        bottomCount[session.rod] = session.targetCount
                        updateRodAppearance(session.rod)

                        // Cancel current animations and take control of translation.
                        for (b in session.affected) {
                            b.animate().cancel()
                            animatingBeads.add(b)
                        }
                        session.provisionalApplied = true
                    }

                    if (session.isDragging) {
                        val translation = if (session.directionUp) -progress else progress
                        for (b in session.affected) {
                            b.translationY = translation
                        }
                        session.currentProgressPx = progress
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val session = activeDrag as? BottomDragSession ?: return@setOnTouchListener false

                    d("Bottom ACTION_UP rod=${session.rod} bead=${session.beadNumber} dragging=${session.isDragging} progress=${session.currentProgressPx}")
                    if (!session.isDragging) {
                        // If user didn't start a valid drag (progress stayed 0), don't always treat it as a tap.
                        // This prevents "forbidden direction drags" from causing tap toggles.
                        val totalDyAbs = abs(event.rawY - session.downRawY)
                        if (totalDyAbs <= touchSlopPx) {
                            // True tap-like behavior
                            onBottomBeadClicked(session.rod, session.beadNumber)
                        } else {
                            // Gesture was a drag attempt in the wrong direction; cancel (no toggle).
                            // Ensure no leftover translation.
                            for (b in session.affected) {
                                b.animate().cancel()
                                b.translationY = 0f
                            }
                        }
                    } else {
                        val commit = session.currentProgressPx >= (bottomMoveDistancePx / 2f)
                        val durationMs = computeDurationMs(
                            velocityAbsPxPerMs = session.currentVelocityAbsPxPerMs,
                            moveDistancePx = bottomMoveDistancePx
                        )

                        if (commit) {
                            // Final state: ensure logical count is targetCount, then animate physical movement.
                            if (!session.provisionalApplied) {
                                bottomCount[session.rod] = session.targetCount
                                updateRodAppearance(session.rod)
                            }
                            if (session.directionUp) {
                                animateBeadsUpWithDuration(durationMs, *session.affected)
                            } else {
                                animateBeadsDownWithDuration(durationMs, *session.affected)
                            }
                        } else {
                            // Snap back to start (no margin changes; only translation back to 0).
                            if (session.provisionalApplied) {
                                bottomCount[session.rod] = session.startCount
                                updateRodAppearance(session.rod)
                            }

                            for (b in session.affected) {
                                b.animate().cancel()
                                b.animate()
                                    .translationY(0f)
                                    .setDuration(durationMs)
                                    .setInterpolator(AccelerateDecelerateInterpolator())
                                    .withEndAction {
                                        b.translationY = 0f
                                        animatingBeads.remove(b)
                                    }
                                    .start()
                            }
                        }
                    }

                    activeDrag = null
                    true
                }

                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_OUTSIDE -> {
                    // Touch sequence got interrupted; ensure we don't get stuck in activeDrag state.
                    activeDrag = null
                    true
                }

                else -> true
            }
        }
    }

    private fun bindTopBeadTouchListener(rod: Int, beadView: ImageView) {
        beadView.isClickable = true
        beadView.isLongClickable = false
        beadView.setOnTouchListener { _, event ->
            if (!touchEnabled) return@setOnTouchListener true
            d("Top onTouch action=${event.actionMasked} active=${activeDrag!=null} rod=$rod")
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    d("Top ACTION_DOWN rod=$rod rawY=${event.rawY} t=${event.eventTime}")
                    if (beadView in animatingBeads) return@setOnTouchListener true
                    // Allow top interactions even if bottom is animating, because top translation is independent.
                    // (Bottom taps/drags are gated separately.)

                    val startDown = topDown[rod]
                    val targetDown = !startDown

                    activeDrag = TopDragSession(
                        rod = rod,
                        bead = beadView,
                        startDown = startDown,
                        targetDown = targetDown,
                        downRawY = event.rawY,
                        isDragging = false,
                        provisionalApplied = false,
                        lastRawY = event.rawY,
                        lastEventTimeMs = event.eventTime,
                        currentProgressPx = 0f,
                        currentVelocityAbsPxPerMs = 0f
                    )
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val session = activeDrag as? TopDragSession ?: return@setOnTouchListener false

                    val dy = event.rawY - session.downRawY
                    val progress = if (directionTowardsDown(session.targetDown)) {
                        dy.coerceIn(0f, topMoveDistancePx)
                    } else {
                        (-dy).coerceIn(0f, topMoveDistancePx)
                    }

                    d("Top ACTION_MOVE rod=${session.rod} dy=$dy progress=$progress dragging=${session.isDragging}")
                    val dtMs = (event.eventTime - session.lastEventTimeMs).coerceAtLeast(1L)
                    val deltaRawY = event.rawY - session.lastRawY
                    val velocityAbs = abs(deltaRawY) / dtMs.toFloat()
                    session.currentVelocityAbsPxPerMs = velocityAbs
                    session.lastRawY = event.rawY
                    session.lastEventTimeMs = event.eventTime

                    if (!session.isDragging && abs(dy) > touchSlopPx && progress > 0f) {
                        session.isDragging = true
                        topDown[session.rod] = session.targetDown
                        updateTopAppearance(session.rod)
                        session.bead.animate().cancel()
                        animatingBeads.add(session.bead)
                        session.provisionalApplied = true
                    }

                    if (session.isDragging) {
                        val translation = if (session.targetDown) progress else (topMoveDistancePx - progress)
                        session.bead.translationY = translation
                        session.currentProgressPx = progress
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val session = activeDrag as? TopDragSession ?: return@setOnTouchListener false

                    d("Top ACTION_UP rod=${session.rod} dragging=${session.isDragging} progress=${session.currentProgressPx}")
                    if (!session.isDragging) {
                        val totalDyAbs = abs(event.rawY - session.downRawY)
                        if (totalDyAbs <= touchSlopPx) {
                            toggleTopBead(session.rod)
                        } else {
                            // Drag attempt in a forbidden direction; cancel (no toggle).
                            // No visual translation changes happened because isDragging never became true.
                        }
                    } else {
                        val commit = session.currentProgressPx >= (topMoveDistancePx / 2f)
                        val durationMs = computeDurationMs(
                            velocityAbsPxPerMs = session.currentVelocityAbsPxPerMs,
                            moveDistancePx = topMoveDistancePx
                        )

                        if (commit) {
                            // State already updated when drag started; still ensure if provisional was not applied.
                            if (!session.provisionalApplied) {
                                topDown[session.rod] = session.targetDown
                                updateTopAppearance(session.rod)
                            }
                            if (session.targetDown) {
                                animateTopBeadDownWithDuration(durationMs, session.bead)
                            } else {
                                animateTopBeadUpWithDuration(durationMs, session.bead)
                            }
                        } else {
                            // Snap back to start state.
                            if (session.provisionalApplied) {
                                topDown[session.rod] = session.startDown
                                updateTopAppearance(session.rod)
                            }

                            val targetTranslation = if (session.startDown) topMoveDistancePx else 0f
                            session.bead.animate().cancel()
                            session.bead.animate()
                                .translationY(targetTranslation)
                                .setDuration(durationMs)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .withEndAction {
                                    session.bead.translationY = targetTranslation
                                    animatingBeads.remove(session.bead)
                                }
                                .start()
                        }
                    }

                    activeDrag = null
                    true
                }

                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_OUTSIDE -> {
                    activeDrag = null
                    true
                }

                else -> true
            }
        }
    }

    private fun directionTowardsDown(targetDown: Boolean): Boolean = targetDown

    private fun computeDurationMs(
        velocityAbsPxPerMs: Float,
        moveDistancePx: Float
    ): Long {
        if (velocityAbsPxPerMs <= 0f) return animationDurationMs
        val expected = moveDistancePx / animationDurationMs.toFloat()
        val factor = (expected / velocityAbsPxPerMs).coerceIn(0.35f, 1.6f)
        return (animationDurationMs.toFloat() * factor).roundToInt().coerceAtLeast(60).toLong()
    }

    fun reset(animate: Boolean = true) {
        resetInProgress = true
        captureInitialPositionsIfNeeded()

        // If something is mid-animation, commit its current translation into margins so
        // subsequent animations start from the correct visual position.
        animatingBeads.clear()
        // Stop top-bead animations as well so animatingBeads state is consistent.
        for (rod in 0..4) {
            val top = topBeads[rod]
            top.animate().cancel()
        }
        for (rod in 0..4) {
            for (i in 0..3) {
                commitBottomTranslationToMargin(bottomBeads[rod][i])
            }
        }

        if (animate) {
            // Animate back to zero state like "tapping back".
            for (rod in 0..4) {
                val count = bottomCount[rod]
                if (count > 0) {
                    val beads = bottomBeads[rod].take(count).toTypedArray()
                    if (beads.any { it in animatingBeads }) continue
                    animateBeadsDown(*beads)
                }
                // Top bead state sometimes desyncs (e.g. tutorial animations update visuals
                // without updating internal `topDown` flags). For reset, rely on *visual*
                // position instead of internal state.
                val top = topBeads[rod]
                val topVisuallyDown = top.translationY >= (topMoveDistancePx / 2f)
                if (topVisuallyDown && top !in animatingBeads) {
                    animateTopBeadUp(top)
                }
            }
        } else {
            // Hard reset (no animation)
            for (rod in 0..4) {
                for (i in 0..3) {
                    val b = bottomBeads[rod][i]
                    b.animate().cancel()
                    val params = b.layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin = initialBottomMargins[rod][i]
                    b.layoutParams = params
                    b.translationY = 0f
                }
                val top = topBeads[rod]
                top.animate().cancel()
                top.translationY = 0f
            }
            animatingBeads.clear()
        }

        for (i in 0..4) {
            bottomCount[i] = 0
            topDown[i] = false
        }
        updateAllAppearance()

        if (!animate) {
            resetInProgress = false
        } else {
            // Reset animations run asynchronously; clear flag after they likely finish.
            val clearDelay = animationDurationMs + 50L
            root.postDelayed({ resetInProgress = false }, clearDelay)
        }
    }

    fun getCurrentValue(): Int {
        var value = 0
        // rod4 = ones, rod3 = tens, ..., rod0 = ten-thousands
        for (rod in 0..4) {
            val digit = bottomCount[rod] + (if (topDown[rod]) 5 else 0)
            val multiplier = when (rod) {
                4 -> 1
                3 -> 10
                2 -> 100
                1 -> 1000
                else -> 10000
            }
            value += digit * multiplier
        }
        return value.coerceIn(0, 99999)
    }

    /**
     * Used by AbacusFragment guide system. Accepts ids like "rod2BottomBead3" / "rod4TopBead".
     * This keeps AbacusFragment call sites unchanged.
     */
    fun animateGuideBead(beadId: String) {
        val rod = beadId.substringAfter("rod", "").takeWhile { it.isDigit() }.toIntOrNull() ?: return
        if (rod !in 0..4) return
        when {
            beadId.contains("TopBead") -> toggleTopBead(rod)
            beadId.contains("BottomBead") -> {
                val n = beadId.substringAfter("BottomBead", "").toIntOrNull() ?: return
                if (n in 1..4) onBottomBeadClicked(rod, n)
            }
        }
    }

    private fun onBottomBeadClicked(rod: Int, beadNumber: Int) {
        val current = bottomCount[rod]
        val target = if (current >= beadNumber) beadNumber - 1 else beadNumber
        val clampedTarget = target.coerceIn(0, 4)
        if (clampedTarget == current) return

        val from = min(current, clampedTarget) + 1
        val to = max(current, clampedTarget)
        val affected = (from..to)
            .map { n -> bottomBeads[rod][n - 1] }
            .toTypedArray()

        if (affected.any { it in animatingBeads }) return

        if (clampedTarget > current) {
            animateBeadsUp(*affected)
        } else {
            animateBeadsDown(*affected)
        }
        bottomCount[rod] = clampedTarget
        updateRodAppearance(rod)
    }

    private fun toggleTopBead(rod: Int) {
        val bead = topBeads[rod]
        if (bead in animatingBeads) return
        if (!topDown[rod]) {
            animateTopBeadDown(bead)
            topDown[rod] = true
        } else {
            animateTopBeadUp(bead)
            topDown[rod] = false
        }
        updateTopAppearance(rod)
    }

    private fun animateBeadsUp(vararg beads: ImageView) {
        val moveDistance = bottomMoveDistancePx.roundToInt()
        beads.forEach { animatingBeads.add(it) }
        beads.forEach { bead ->
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin + moveDistance
            bead.animate()
                .setDuration(animationDurationMs)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f
                    animatingBeads.remove(bead)
                }
                .translationY(-moveDistance.toFloat())
                .start()
        }
    }

    private fun animateBeadsDown(vararg beads: ImageView) {
        val moveDistance = bottomMoveDistancePx.roundToInt()
        beads.forEach { animatingBeads.add(it) }
        beads.forEach { bead ->
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin - moveDistance
            bead.animate()
                .setDuration(animationDurationMs)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f
                    animatingBeads.remove(bead)
                }
                .translationY(moveDistance.toFloat())
                .start()
        }
    }

    private fun animateTopBeadDown(bead: ImageView) {
        val moveDistance = topMoveDistancePx.roundToInt()
        animatingBeads.add(bead)
        bead.animate()
            .setDuration(animationDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { animatingBeads.remove(bead) }
            .translationY(moveDistance.toFloat())
            .start()
    }

    private fun animateTopBeadUp(bead: ImageView) {
        animatingBeads.add(bead)
        bead.animate()
            .setDuration(animationDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { animatingBeads.remove(bead) }
            .translationY(0f)
            .start()
    }

    // Same animations as above, but with custom duration (used for drag velocity).
    private fun animateBeadsUpWithDuration(durationMs: Long, vararg beads: ImageView) {
        val moveDistanceInt = bottomMoveDistancePx.roundToInt()
        val moveDistance = moveDistanceInt
        beads.forEach { animatingBeads.add(it) }
        beads.forEach { bead ->
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin + moveDistance
            bead.animate()
                .setDuration(durationMs)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f
                    animatingBeads.remove(bead)
                }
                .translationY(-moveDistance.toFloat())
                .start()
        }
    }

    private fun animateBeadsDownWithDuration(durationMs: Long, vararg beads: ImageView) {
        val moveDistanceInt = bottomMoveDistancePx.roundToInt()
        val moveDistance = moveDistanceInt
        beads.forEach { animatingBeads.add(it) }
        beads.forEach { bead ->
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin - moveDistance
            bead.animate()
                .setDuration(durationMs)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f
                    animatingBeads.remove(bead)
                }
                .translationY(moveDistance.toFloat())
                .start()
        }
    }

    private fun animateTopBeadDownWithDuration(durationMs: Long, bead: ImageView) {
        animatingBeads.add(bead)
        bead.animate()
            .setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { animatingBeads.remove(bead) }
            .translationY(topMoveDistancePx)
            .start()
    }

    private fun animateTopBeadUpWithDuration(durationMs: Long, bead: ImageView) {
        animatingBeads.add(bead)
        bead.animate()
            .setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { animatingBeads.remove(bead) }
            .translationY(0f)
            .start()
    }

    private fun updateAllAppearance() {
        for (rod in 0..4) {
            updateRodAppearance(rod)
            updateTopAppearance(rod)
        }
    }

    private fun updateRodAppearance(rod: Int) {
        val count = bottomCount[rod]
        for (i in 1..4) {
            val selected = i <= count
            updateBeadAppearance(bottomBeads[rod][i - 1], selected)
        }
    }

    private fun updateTopAppearance(rod: Int) {
        updateBeadAppearance(topBeads[rod], topDown[rod])
    }

    private fun updateBeadAppearance(bead: ImageView, isSelected: Boolean) {
        bead.setImageResource(if (isSelected) R.drawable.soroban_bead_selected else R.drawable.soroban_bead)
    }

    private fun commitBottomTranslationToMargin(bead: ImageView) {
        // Cancel first to stop further updates, then commit current translation into margin.
        bead.animate().cancel()
        animatingBeads.remove(bead)
        val dy = bead.translationY
        if (dy != 0f) {
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            // translationY > 0 means bead visually moved down; margin must decrease accordingly.
            // translationY < 0 means bead visually moved up; margin must increase accordingly.
            params.bottomMargin = (params.bottomMargin - dy).roundToInt()
            bead.layoutParams = params
            bead.translationY = 0f
        }
    }

    private fun captureInitialPositionsIfNeeded() {
        if (initialPositionsCaptured) return
        for (rod in 0..4) {
            for (i in 0..3) {
                val params = bottomBeads[rod][i].layoutParams as ViewGroup.MarginLayoutParams
                initialBottomMargins[rod][i] = params.bottomMargin
            }
        }
        initialPositionsCaptured = true
    }

    private fun findBead(idName: String): ImageView {
        val id = context.resources.getIdentifier(idName, "id", context.packageName)
        require(id != 0) { "Missing view id: $idName" }
        return root.findViewById(id)
    }
}

