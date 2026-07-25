package com.example.app

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.app.abacus.AbacusBeadController
import com.example.app.abacus.AbacusBeadRenderer
import com.example.app.abacus.AbacusFrameRenderer
import com.example.app.abacus.AbacusPreferences
import com.example.app.abacus.AbacusPreferences.BeadType
import com.example.app.abacus.AbacusPreferences.FrameType
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment that lets the user customise:
 *  Tab 1 — bead type (soroban / rabbit)
 *  Tab 2 — per-path colour via Hue + Brightness seekbars (with gradient backgrounds)
 *  Tab 3 — frame/container background type
 *  Tab 4 — frame colour(s)
 *
 * Rabbit rendering runs on [Dispatchers.Default] with 300 ms debounce so the
 * main thread stays responsive while the user drags seekbars.
 */
class AbacusCustomizationFragment : Fragment() {

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var previewContainer: FrameLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var tabIndicator: View
    private lateinit var tabContent: FrameLayout

    private val tabViews = arrayOfNulls<ImageView>(4)

    private var previewController: AbacusBeadController? = null
    private var previewAbacusRoot: View? = null

    private var currentTab = 0

    // Debounce job for async rabbit rendering
    private var renderJob: Job? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_abacus_customization, container, false)

        previewContainer = v.findViewById(R.id.previewContainer)
        tabStrip = v.findViewById(R.id.tabStrip)
        tabIndicator = v.findViewById(R.id.tabIndicator)
        tabContent = v.findViewById(R.id.tabContent)

        for (i in 0..3) {
            val id = when (i) { 0 -> R.id.tab1; 1 -> R.id.tab2; 2 -> R.id.tab3; else -> R.id.tab4 }
            tabViews[i] = v.findViewById(id)
            val idx = i
            tabViews[i]?.setOnClickListener { selectTab(idx) }
        }
        v.findViewById<ImageButton>(R.id.customizationBackButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        inflatePreviewAbacus()
        selectTab(0)
        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        previewController = null
        previewAbacusRoot = null
    }

    // ── Preview Abacus ────────────────────────────────────────────────────────

    private fun inflatePreviewAbacus() {
        val preview = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_customization_preview, previewContainer, false)
        previewContainer.removeAllViews()
        previewContainer.addView(preview)
        previewAbacusRoot = preview

        applyPreviewFrameBackground(preview.findViewById(R.id.abacusContainer))

        previewController = AbacusBeadController(requireContext(), preview, 150L).also { it.setup() }
        preview.post { previewController?.computeMovementDistancesFromLayout(ratio = 1.0f, force = true) }
        scheduleBeadRefresh(debounce = false)
    }

    private fun applyPreviewFrameBackground(target: View) {
        target.background = AbacusFrameRenderer.buildFrameDrawable(requireContext())
    }

    private fun refreshPreviewFrameBackground() {
        val root = previewAbacusRoot ?: return
        applyPreviewFrameBackground(root.findViewById(R.id.abacusContainer))
    }

    /**
     * Refreshes bead drawables in the preview.
     * For RABBIT: runs rendering on Default dispatcher to avoid blocking the main thread.
     * [debounce] = true adds a 300 ms delay (used while seekbar is being dragged).
     */
    private fun scheduleBeadRefresh(debounce: Boolean = true) {
        val ctx = requireContext().applicationContext
        renderJob?.cancel()
        renderJob = viewLifecycleOwner.lifecycleScope.launch {
            if (debounce) delay(300)
            // Build drawables off main thread
            withContext(Dispatchers.Default) {
                AbacusBeadRenderer.buildCurrentBead(ctx, false)  // warm-up cache (soroban is fast)
            }
            // Refresh the controller on main thread
            if (isAdded) previewController?.refreshAll()
        }
    }

    // ── Tab management ────────────────────────────────────────────────────────

    private fun selectTab(index: Int) {
        currentTab = index
        for (i in 0..3) {
            tabViews[i]?.clearColorFilter()
            tabViews[i]?.alpha = if (i == index) 1.0f else 0.4f
        }
        tabStrip.post {
            val tabW = tabStrip.width / 4
            val lp = tabIndicator.layoutParams as? ViewGroup.MarginLayoutParams ?: return@post
            lp.marginStart = tabW * index
            lp.width = tabW
            tabIndicator.layoutParams = lp
        }
        tabContent.removeAllViews()
        tabContent.addView(when (index) {
            0 -> inflateTab1()
            1 -> inflateTab2()
            2 -> inflateTab3()
            else -> inflateTab4()
        })
    }

    // ── Tab 1: Bead type ──────────────────────────────────────────────────────

    private fun inflateTab1(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab1_bead_options, tabContent, false)

        val cardSoroban = v.findViewById<MaterialCardView>(R.id.cardBeadSoroban)
        val cardAnimal = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal)
        val cardAnimal2 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal2)
        val cardAnimal3 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal3)
        val cardAnimal4 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal4)
        val cardAnimal5 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal5)
        val cardAnimal6 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal6)
        val cardAnimal7 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal7)
        val cardAnimal8 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal8)
        val cardAnimal9 = v.findViewById<MaterialCardView>(R.id.cardBeadAnimal9)
        // We keep the original colors in Tab 1 by NOT overriding their ImageDrawables

        fun updateSelection() {
            val type = AbacusPreferences.getBeadType(requireContext())
            val sel = Color.parseColor("#FFFFD600")
            cardSoroban.strokeColor = if (type == BeadType.SOROBAN) sel else Color.TRANSPARENT
            cardSoroban.strokeWidth = if (type == BeadType.SOROBAN) 6 else 0
            cardAnimal.strokeColor = if (type == BeadType.ANIMAL) sel else Color.TRANSPARENT
            cardAnimal.strokeWidth = if (type == BeadType.ANIMAL) 6 else 0
            cardAnimal2.strokeColor = if (type == BeadType.ANIMAL2) sel else Color.TRANSPARENT
            cardAnimal2.strokeWidth = if (type == BeadType.ANIMAL2) 6 else 0
            cardAnimal3.strokeColor = if (type == BeadType.ANIMAL3) sel else Color.TRANSPARENT
            cardAnimal3.strokeWidth = if (type == BeadType.ANIMAL3) 6 else 0
            cardAnimal4.strokeColor = if (type == BeadType.ANIMAL4) sel else Color.TRANSPARENT
            cardAnimal4.strokeWidth = if (type == BeadType.ANIMAL4) 6 else 0
            cardAnimal5.strokeColor = if (type == BeadType.ANIMAL5) sel else Color.TRANSPARENT
            cardAnimal5.strokeWidth = if (type == BeadType.ANIMAL5) 6 else 0
            cardAnimal6.strokeColor = if (type == BeadType.ANIMAL6) sel else Color.TRANSPARENT
            cardAnimal6.strokeWidth = if (type == BeadType.ANIMAL6) 6 else 0
            cardAnimal7.strokeColor = if (type == BeadType.ANIMAL7) sel else Color.TRANSPARENT
            cardAnimal7.strokeWidth = if (type == BeadType.ANIMAL7) 6 else 0
            cardAnimal8.strokeColor = if (type == BeadType.ANIMAL8) sel else Color.TRANSPARENT
            cardAnimal8.strokeWidth = if (type == BeadType.ANIMAL8) 6 else 0
            cardAnimal9.strokeColor = if (type == BeadType.ANIMAL9) sel else Color.TRANSPARENT
            cardAnimal9.strokeWidth = if (type == BeadType.ANIMAL9) 6 else 0
        }
        updateSelection()

        cardSoroban.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.SOROBAN)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal2.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL2)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal3.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL3)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal4.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL4)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal5.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL5)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal6.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL6)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal7.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL7)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal8.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL8)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        cardAnimal9.setOnClickListener {
            AbacusPreferences.setBeadType(requireContext(), BeadType.ANIMAL9)
            updateSelection()
            scheduleBeadRefresh(debounce = false)
        }
        return v
    }

    // ── Tab 2: Bead colour ────────────────────────────────────────────────────

    private fun inflateTab2(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab2_bead_color, tabContent, false)

        val previewNormal = v.findViewById<ImageView>(R.id.previewBeadNormal)
        val previewSelected = v.findViewById<ImageView>(R.id.previewBeadSelected)
        val columnNormal = v.findViewById<LinearLayout>(R.id.columnNormal)
        val columnSelected = v.findViewById<LinearLayout>(R.id.columnSelected)
        val ctx = requireContext()
        val beadType = AbacusPreferences.getBeadType(ctx)

        fun refreshHeaders() {
            previewNormal.setImageDrawable(AbacusBeadRenderer.buildCurrentBead(ctx, false))
            previewSelected.setImageDrawable(AbacusBeadRenderer.buildCurrentBead(ctx, true))
        }
        refreshHeaders()

        val labels: Array<String>
        val pathCount: Int
        when (beadType) {
            BeadType.SOROBAN -> { labels = AbacusPreferences.SOROBAN_PATH_LABELS; pathCount = 3 }
            BeadType.ANIMAL  -> { labels = AbacusPreferences.ANIMAL_COLOR_LABELS; pathCount = 4 }
            BeadType.ANIMAL2 -> { labels = AbacusPreferences.ANIMAL2_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL3 -> { labels = AbacusPreferences.ANIMAL3_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL4 -> { labels = AbacusPreferences.ANIMAL4_COLOR_LABELS; pathCount = 4 }
            BeadType.ANIMAL5 -> { labels = AbacusPreferences.ANIMAL5_COLOR_LABELS; pathCount = 6 }
            BeadType.ANIMAL6 -> { labels = AbacusPreferences.ANIMAL6_COLOR_LABELS; pathCount = 3 }
            BeadType.ANIMAL7 -> { labels = AbacusPreferences.ANIMAL7_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL8 -> { labels = AbacusPreferences.ANIMAL8_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL9 -> { labels = AbacusPreferences.ANIMAL9_COLOR_LABELS; pathCount = 4 }
        }

        /**
         * Builds a column of H+V dual-seekbar rows for one bead state (normal or selected).
         * Each seekbar row:
         *   - Hue seekbar:        rainbow gradient background, progress 0–359
         *   - Brightness seekbar: black→current-hue gradient,  progress 0–100
         * Changes are saved immediately; rabbit rendering is debounced 300 ms.
         */
        fun buildColorRows(parent: LinearLayout, isSelected: Boolean) {
            parent.removeAllViews()
            val currentColors = when (beadType) {
                BeadType.SOROBAN -> AbacusPreferences.getSorobanColors(ctx, isSelected).copyOf()
                BeadType.ANIMAL  -> AbacusPreferences.getAnimalColors(ctx, isSelected).copyOf()
                BeadType.ANIMAL2 -> AbacusPreferences.getAnimal2Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL3 -> AbacusPreferences.getAnimal3Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL4 -> AbacusPreferences.getAnimal4Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL5 -> AbacusPreferences.getAnimal5Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL6 -> AbacusPreferences.getAnimal6Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL7 -> AbacusPreferences.getAnimal7Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL8 -> AbacusPreferences.getAnimal8Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL9 -> AbacusPreferences.getAnimal9Colors(ctx, isSelected).copyOf()
            }

            for (i in 0 until pathCount) {
                val color = currentColors[i]
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 4.dp, 0, 10.dp)
                }

                // ── Label + swatch ──────────────────────────────────────────
                val headerRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                val swatch = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp).also { it.marginEnd = 4.dp }
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
                }
                val label = TextView(ctx).apply {
                    text = labels[i]; textSize = 9f; setTextColor(0xAAFFFFFF.toInt())
                }
                headerRow.addView(swatch); headerRow.addView(label)
                row.addView(headerRow)

                // ── Hue seekbar (rainbow gradient) ──────────────────────────
                val hueLabel = TextView(ctx).apply {
                    text = "Renk"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(hueLabel)

                val hueBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 359
                    progress = AbacusBeadRenderer.getHue(color)
                    progressDrawable = rainbowDrawable()
                    thumb = roundThumb(color)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // ── Brightness seekbar (black → colour) ─────────────────────
                val briLabel = TextView(ctx).apply {
                    text = "Parlaklık"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(briLabel)

                val briBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 100
                    progress = AbacusBeadRenderer.getLightness100(color)
                    progressDrawable = lightnessGradientDrawable(AbacusBeadRenderer.getHue(color))
                    thumb = roundThumb(color)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val pathIdx = i
                val swatchRef = swatch

                // Shared update: recalculate colour from current H + V values
                fun onAnyChange() {
                    val hue = hueBar.progress
                    val bri = briBar.progress
                    val newColor = AbacusBeadRenderer.makeColor(hue, bri)
                    currentColors[pathIdx] = newColor
                    // Update swatch and thumbs immediately
                    swatchRef.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(newColor) }
                    hueBar.thumb = roundThumb(newColor)
                    briBar.thumb = roundThumb(newColor)
                    briBar.progressDrawable = lightnessGradientDrawable(hue)
                    // Persist
                    when (beadType) {
                        BeadType.SOROBAN -> AbacusPreferences.setSorobanColor(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL  -> AbacusPreferences.setAnimalColor(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL2 -> AbacusPreferences.setAnimal2Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL3 -> AbacusPreferences.setAnimal3Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL4 -> AbacusPreferences.setAnimal4Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL5 -> AbacusPreferences.setAnimal5Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL6 -> AbacusPreferences.setAnimal6Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL7 -> AbacusPreferences.setAnimal7Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL8 -> AbacusPreferences.setAnimal8Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL9 -> AbacusPreferences.setAnimal9Color(ctx, isSelected, pathIdx, newColor)
                    }
                    refreshHeaders()
                    scheduleBeadRefresh(debounce = true)
                }

                hueBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })
                briBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })

                row.addView(hueBar)
                row.addView(briBar)
                parent.addView(row)
            }

            // Reset button
            val resetBtn = ImageButton(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(56.dp, 56.dp).also {
                    it.topMargin = 8.dp
                    it.gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
                setImageResource(R.drawable.special_reset_ic)
                background = null; scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = null
            }
            resetBtn.setOnClickListener {
                when (beadType) {
                    BeadType.SOROBAN -> AbacusPreferences.resetSorobanColors(ctx, isSelected)
                    BeadType.ANIMAL  -> AbacusPreferences.resetAnimalColors(ctx, isSelected)
                    BeadType.ANIMAL2 -> AbacusPreferences.resetAnimal2Colors(ctx, isSelected)
                    BeadType.ANIMAL3 -> AbacusPreferences.resetAnimal3Colors(ctx, isSelected)
                    BeadType.ANIMAL4 -> AbacusPreferences.resetAnimal4Colors(ctx, isSelected)
                    BeadType.ANIMAL5 -> AbacusPreferences.resetAnimal5Colors(ctx, isSelected)
                    BeadType.ANIMAL6 -> AbacusPreferences.resetAnimal6Colors(ctx, isSelected)
                    BeadType.ANIMAL7 -> AbacusPreferences.resetAnimal7Colors(ctx, isSelected)
                    BeadType.ANIMAL8 -> AbacusPreferences.resetAnimal8Colors(ctx, isSelected)
                    BeadType.ANIMAL9 -> AbacusPreferences.resetAnimal9Colors(ctx, isSelected)
                }
                buildColorRows(parent, isSelected)
                refreshHeaders()
                scheduleBeadRefresh(debounce = false)
            }
            parent.addView(resetBtn)
        }

        buildColorRows(columnNormal, false)
        buildColorRows(columnSelected, true)
        return v
    }

    // ── Tab 3: Frame type ─────────────────────────────────────────────────────

    private fun inflateTab3(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab3_frame_options, tabContent, false)
        val cardBg = v.findViewById<MaterialCardView>(R.id.cardFrameBg)
        val cardBg2 = v.findViewById<MaterialCardView>(R.id.cardFrameBg2)

        fun updateSelection() {
            val type = AbacusPreferences.getFrameType(requireContext())
            val sel = Color.parseColor("#FFFFD600")
            cardBg.strokeColor  = if (type == FrameType.FRAME_BG)  sel else Color.TRANSPARENT
            cardBg.strokeWidth  = if (type == FrameType.FRAME_BG)  6 else 0
            cardBg2.strokeColor = if (type == FrameType.FRAME_BG2) sel else Color.TRANSPARENT
            cardBg2.strokeWidth = if (type == FrameType.FRAME_BG2) 6 else 0
        }
        updateSelection()

        cardBg.setOnClickListener  { AbacusPreferences.setFrameType(requireContext(), FrameType.FRAME_BG);  updateSelection(); refreshPreviewFrameBackground() }
        cardBg2.setOnClickListener { AbacusPreferences.setFrameType(requireContext(), FrameType.FRAME_BG2); updateSelection(); refreshPreviewFrameBackground() }
        return v
    }

    // ── Tab 4: Frame colour ───────────────────────────────────────────────────

    private fun inflateTab4(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab4_frame_color, tabContent, false)

        val rowsContainer = v.findViewById<LinearLayout>(R.id.frameColorRows)
        val resetBtn = v.findViewById<ImageButton>(R.id.resetFrameColors)
        val ctx = requireContext()

        data class ColorEntry(
            val label: String,
            val currentColor: Int,
            val onChanged: (Int) -> Unit
        )

        fun buildRows() {
            rowsContainer.removeAllViews()

            val entries: List<ColorEntry> = when (AbacusPreferences.getFrameType(ctx)) {
                FrameType.FRAME_BG -> listOf(
                    ColorEntry("Ahşap rengi", AbacusPreferences.getFrameBgWoodColor(ctx))
                    { c -> AbacusPreferences.setFrameBgWoodColor(ctx, c); refreshPreviewFrameBackground() }
                )
                FrameType.FRAME_BG2 -> listOf(
                    ColorEntry("Ahşap rengi", AbacusPreferences.getFrameBg2WoodColor(ctx))
                    { c -> AbacusPreferences.setFrameBg2WoodColor(ctx, c); refreshPreviewFrameBackground() },
                    ColorEntry("Kalp rengi", AbacusPreferences.getFrameBg2HeartColor(ctx))
                    { c -> AbacusPreferences.setFrameBg2HeartColor(ctx, c); refreshPreviewFrameBackground() }
                )
            }

            for (entry in entries) {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 4.dp, 0, 14.dp)
                }

                // ── Label + swatch ──────────────────────────────────────────
                val headerRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                val swatch = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp).also { it.marginEnd = 6.dp }
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(entry.currentColor) }
                }
                val label = TextView(ctx).apply {
                    text = entry.label; textSize = 10f; setTextColor(0xAAFFFFFF.toInt())
                }
                headerRow.addView(swatch); headerRow.addView(label)
                row.addView(headerRow)

                // ── Hue seekbar (rainbow gradient) ──────────────────────────
                val hueLabel = TextView(ctx).apply {
                    text = "Renk"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(hueLabel)

                val hueBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 359
                    progress = AbacusBeadRenderer.getHue(entry.currentColor)
                    progressDrawable = rainbowDrawable()
                    thumb = roundThumb(entry.currentColor)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // ── Brightness seekbar (black → colour) ─────────────────────
                val briLabel = TextView(ctx).apply {
                    text = "Parlaklık"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(briLabel)

                val briBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 100
                    progress = AbacusBeadRenderer.getLightness100(entry.currentColor)
                    progressDrawable = lightnessGradientDrawable(AbacusBeadRenderer.getHue(entry.currentColor))
                    thumb = roundThumb(entry.currentColor)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val onChanged = entry.onChanged
                val swatchRef = swatch

                fun onAnyChange() {
                    val hue = hueBar.progress
                    val bri = briBar.progress
                    val newColor = AbacusBeadRenderer.makeColor(hue, bri)
                    swatchRef.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(newColor) }
                    hueBar.thumb = roundThumb(newColor)
                    briBar.thumb = roundThumb(newColor)
                    briBar.progressDrawable = lightnessGradientDrawable(hue)
                    onChanged(newColor)
                }

                hueBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })
                briBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })

                row.addView(hueBar)
                row.addView(briBar)
                rowsContainer.addView(row)
            }
        }

        buildRows()
        resetBtn.setOnClickListener {
            AbacusPreferences.resetAllFrameColors(ctx)
            buildRows()
            refreshPreviewFrameBackground()
        }
        return v
    }

    // ── Seekbar gradient helpers ───────────────────────────────────────────────

    /**
     * Returns a drawable that paints a full rainbow gradient across the seekbar track.
     * Uses 7-stop GradientDrawable (hue: 0→60→120→180→240→300→360).
     */
    private fun rainbowDrawable(): GradientDrawable {
        val colors = intArrayOf(
            Color.HSVToColor(floatArrayOf(0f,   1f, 1f)),
            Color.HSVToColor(floatArrayOf(60f,  1f, 1f)),
            Color.HSVToColor(floatArrayOf(120f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(180f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(240f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(300f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(360f, 1f, 1f))
        )
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = 8f
        }
    }

    /**
     * Returns a gradient from black → full-brightness colour → white at [hueDegrees].
     * This visually shows what the lightness seekbar does.
     */
    private fun lightnessGradientDrawable(hueDegrees: Int): GradientDrawable {
        val fullColor = Color.HSVToColor(floatArrayOf(hueDegrees.toFloat(), AbacusBeadRenderer.FIXED_SATURATION, 1f))
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.BLACK, fullColor, Color.WHITE)).apply {
            cornerRadius = 8f
        }
    }

    /**
     * Returns a circular thumb drawable filled with [color].
     * Gives instant visual feedback of the chosen colour.
     */
    private fun roundThumb(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(2.dp, 0xFFFFFFFF.toInt())
        val sz = 20.dp
        setSize(sz, sz)
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    /** Convenience: only calls [onChange] on fromUser=true progress changes. */
    private fun simpleSeekListener(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) { if (fromUser) onChange() }
        override fun onStartTrackingTouch(bar: SeekBar) {}
        override fun onStopTrackingTouch(bar: SeekBar) {}
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        fun newInstance() = AbacusCustomizationFragment()
    }
}
