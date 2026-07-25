package com.example.app.abacus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.app.R
import kotlin.math.abs

/**
 * Builds bead drawables that respect user colour preferences stored in [AbacusPreferences].
 *
 * - SOROBAN (3 simple paths, no clip-paths): LayerDrawable from per-path XML files.
 *   Each layer is tinted with [DrawableCompat.setTint] (SRC_IN mode).
 * - RABBIT (13 paths, uses clip-paths): original drawable is rasterised to a Bitmap,
 *   then per-pixel colour replacement maps known fill colours to user-chosen colours.
 *   Anti-aliased edge pixels are handled with a small Manhattan-distance tolerance (≤30).
 */
object AbacusBeadRenderer {


    // ── Animal colour maps ────────────────────────────────────────────────────

    private val ANIMAL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFFFFFFF.toInt() to 0,
        0xFF777873.toInt() to 1,
        0xFF4D4C48.toInt() to 2,
        0xFF32302D.toInt() to 3
    )

    private val ANIMAL_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB2B2B2.toInt() to 0,
        0xFF535450.toInt() to 1,
        0xFF353532.toInt() to 2,
        0xFF23211F.toInt() to 3
    )

    // ── Animal 1 (Dog) colour maps ──────────────────────────────────────────────────

    private val ANIMAL2_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB76057.toInt() to 0,
        0xFFFBBB8C.toInt() to 1,
        0xFF32302D.toInt() to 2,
        0xFF914844.toInt() to 3,
        0xFFE8A070.toInt() to 4
    )

    private val ANIMAL2_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF80433C.toInt() to 0,
        0xFFAF8262.toInt() to 1,
        0xFF23211F.toInt() to 2,
        0xFF65322F.toInt() to 3,
        0xFFA2704E.toInt() to 4
    )

    // ── Animal 3 colour maps ──────────────────────────────────────────────────

    private val ANIMAL3_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF99E4D.toInt() to 0,
        0xFF32302D.toInt() to 1,
        0xFFE58435.toInt() to 2,
        0xFFFDD2B9.toInt() to 3,
        0xFFD36640.toInt() to 4
    )

    private val ANIMAL3_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFAE6E35.toInt() to 0,
        0xFF23211F.toInt() to 1,
        0xFFA05C25.toInt() to 2,
        0xFFB19381.toInt() to 3,
        0xFF93472C.toInt() to 4
    )

    // ── Animal 4 colour maps ──────────────────────────────────────────────────

    private val ANIMAL4_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF3703A.toInt() to 0,
        0xFF32302D.toInt() to 1,
        0xFFFFFFFF.toInt() to 2,
        0xFFCC482E.toInt() to 3
    )

    private val ANIMAL4_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFAA4E28.toInt() to 0,
        0xFF23211F.toInt() to 1,
        0xFFB2B2B2.toInt() to 2,
        0xFF8E3220.toInt() to 3
    )

    // ── Animal 5 colour maps ──────────────────────────────────────────────────

    private val ANIMAL5_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF525D65.toInt() to 1,
        0xFF7C8B96.toInt() to 2,
        0xFF929FA9.toInt() to 3,
        0xFFA6B3BD.toInt() to 4,
        0xFFEFC9A5.toInt() to 5
    )

    private val ANIMAL5_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF394146.toInt() to 1,
        0xFF566169.toInt() to 2,
        0xFF666F76.toInt() to 3,
        0xFF747D84.toInt() to 4,
        0xFFA78C73.toInt() to 5
    )

    // ── Animal 6 colour maps ──────────────────────────────────────────────────

    private val ANIMAL6_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFFD1D3D4.toInt() to 1,
        0xFFFFFFFF.toInt() to 2
    )

    private val ANIMAL6_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF929394.toInt() to 1,
        0xFFB2B2B2.toInt() to 2
    )

    // ── Animal 7 colour maps ──────────────────────────────────────────────────

    private val ANIMAL7_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFFF3757A.toInt() to 1,
        0xFFF9E4D7.toInt() to 2,
        0xFFFFF1E7.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    private val ANIMAL7_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFFAA5155.toInt() to 1,
        0xFFAE9F96.toInt() to 2,
        0xFFB2A8A1.toInt() to 3,
        0xFFB2B2B2.toInt() to 4
    )

    // ── Animal 8 colour maps ──────────────────────────────────────────────────

    private val ANIMAL8_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF525D65.toInt() to 1,
        0xFFD69F90.toInt() to 2,
        0xFFFCD3C6.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    private val ANIMAL8_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF394146.toInt() to 1,
        0xFF956F64.toInt() to 2,
        0xFFB0938A.toInt() to 3,
        0xFFB2B2B2.toInt() to 4
    )

    // ── Animal 9 colour maps ──────────────────────────────────────────────────

    private val ANIMAL9_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF60C198.toInt() to 1,
        0xFFF2A638.toInt() to 2,
        0xFFFFC94E.toInt() to 3
    )

    private val ANIMAL9_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF43876A.toInt() to 1,
        0xFFA97427.toInt() to 2,
        0xFFB28C36.toInt() to 3
    )

    // Tolerance for pixel colour matching (Manhattan distance across R+G+B)
    private const val COLOR_TOLERANCE = 150

    // Render size cap for rabbit bitmap (dp). Keeps memory usage low.
    private const val RABBIT_RENDER_DP = 100

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds a drawable for the currently selected bead type, respecting saved colours.
     * Called from [AbacusBeadController.updateBeadAppearance].
     */
    fun buildCurrentBead(context: Context, isSelected: Boolean): Drawable =
        when (AbacusPreferences.getBeadType(context)) {
            AbacusPreferences.BeadType.SOROBAN -> buildSorobanDrawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL -> buildAnimalDrawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL2 -> buildAnimal2Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL3 -> buildAnimal3Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL4 -> buildAnimal4Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL5 -> buildAnimal5Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL6 -> buildAnimal6Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL7 -> buildAnimal7Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL8 -> buildAnimal8Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL9 -> buildAnimal9Drawable(context, isSelected)
            else -> buildSorobanDrawable(context, isSelected)
        }

    /**
     * Builds a soroban bead [LayerDrawable] with per-path tinting.
     * Layer order mirrors soroban_bead.xml: outer shell → inner fill → right highlight.
     */
    fun buildSorobanDrawable(context: Context, selected: Boolean): Drawable {
        val colors = AbacusPreferences.getSorobanColors(context, selected)
        val resIds = if (selected) {
            intArrayOf(
                R.drawable.soroban_bead_selected_layer1,
                R.drawable.soroban_bead_selected_layer2,
                R.drawable.soroban_bead_selected_layer3
            )
        } else {
            intArrayOf(
                R.drawable.soroban_bead_layer1,
                R.drawable.soroban_bead_layer2,
                R.drawable.soroban_bead_layer3
            )
        }

        val layers = Array(3) { i ->
            val d: Drawable = ContextCompat.getDrawable(context, resIds[i])!!.mutate()
            val wrapped = DrawableCompat.wrap(d)
            DrawableCompat.setTint(wrapped, colors[i])
            DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.SRC_IN)
            wrapped
        }
        return LayerDrawable(layers)
    }

    /**
     * Builds an animal head bead drawable using bitmap pixel-colour replacement.
     */
    fun buildAnimalDrawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimalColors(context, selected)
        val map = if (selected) ANIMAL_SEL_ORIGINAL_MAP else ANIMAL_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic1 else R.drawable.animal_head_ic1
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal2Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal2Colors(context, selected)
        val map = if (selected) ANIMAL2_SEL_ORIGINAL_MAP else ANIMAL2_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic2 else R.drawable.animal_head_ic2
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal3Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal3Colors(context, selected)
        val map = if (selected) ANIMAL3_SEL_ORIGINAL_MAP else ANIMAL3_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic3 else R.drawable.animal_head_ic3
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal4Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal4Colors(context, selected)
        val map = if (selected) ANIMAL4_SEL_ORIGINAL_MAP else ANIMAL4_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic4 else R.drawable.animal_head_ic4
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal5Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal5Colors(context, selected)
        val map = if (selected) ANIMAL5_SEL_ORIGINAL_MAP else ANIMAL5_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic5 else R.drawable.animal_head_ic5
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal6Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal6Colors(context, selected)
        val map = if (selected) ANIMAL6_SEL_ORIGINAL_MAP else ANIMAL6_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic6 else R.drawable.animal_head_ic6
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal7Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal7Colors(context, selected)
        val map = if (selected) ANIMAL7_SEL_ORIGINAL_MAP else ANIMAL7_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic7 else R.drawable.animal_head_ic7
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal8Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal8Colors(context, selected)
        val map = if (selected) ANIMAL8_SEL_ORIGINAL_MAP else ANIMAL8_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic8 else R.drawable.animal_head_ic8
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal9Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal9Colors(context, selected)
        val map = if (selected) ANIMAL9_SEL_ORIGINAL_MAP else ANIMAL9_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic9 else R.drawable.animal_head_ic9
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private fun buildReplacedBitmapDrawable(
        context: Context,
        srcRes: Int,
        originalMap: Map<Int, Int>,
        newColors: IntArray
    ): Drawable {
        var cacheKey = srcRes
        for (c in newColors) {
            cacheKey = 31 * cacheKey + c
        }

        bitmapCache[cacheKey]?.let { cachedBitmap ->
            return BitmapDrawable(context.resources, cachedBitmap)
        }

        val src: Drawable = ContextCompat.getDrawable(context, srcRes)!!

        val density = context.resources.displayMetrics.density
        val targetW = (RABBIT_RENDER_DP * density).toInt().coerceAtLeast(1)
        val intrinsicW = src.intrinsicWidth.coerceAtLeast(1)
        val intrinsicH = src.intrinsicHeight.coerceAtLeast(1)
        val targetH = (targetW.toFloat() * intrinsicH / intrinsicW).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        src.setBounds(0, 0, targetW, targetH)
        src.draw(canvas)

        val origEntries = originalMap.entries.toList()
        val pixels = IntArray(targetW * targetH)
        bitmap.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        for (idx in pixels.indices) {
            val alpha = pixels[idx] ushr 24
            if (alpha < 10) continue // skip near-transparent

            val pixelArgb = pixels[idx]
            val pr = (pixelArgb ushr 16) and 0xFF
            val pg = (pixelArgb ushr 8) and 0xFF
            val pb = pixelArgb and 0xFF

            var bestScore = Float.MAX_VALUE
            var bestD = Float.MAX_VALUE
            var bestC0 = -1
            var bestC1 = -1
            var bestT = 0f

            for (i in origEntries.indices) {
                val c0 = origEntries[i].key
                val r0 = ((c0 ushr 16) and 0xFF).toFloat()
                val g0 = ((c0 ushr 8) and 0xFF).toFloat()
                val b0 = (c0 and 0xFF).toFloat()

                // Check distance to solid color first
                val d0Sq = (pr - r0) * (pr - r0) + (pg - g0) * (pg - g0) + (pb - b0) * (pb - b0)
                if (d0Sq < bestScore) {
                    bestScore = d0Sq
                    bestD = d0Sq
                    bestC0 = origEntries[i].value
                    bestC1 = origEntries[i].value
                    bestT = 0f
                }

                // Check distance to segments
                for (j in i + 1 until origEntries.size) {
                    val c1 = origEntries[j].key
                    val r1 = ((c1 ushr 16) and 0xFF).toFloat()
                    val g1 = ((c1 ushr 8) and 0xFF).toFloat()
                    val b1 = (c1 and 0xFF).toFloat()

                    val abR = r1 - r0
                    val abG = g1 - g0
                    val abB = b1 - b0
                    val abLenSq = abR * abR + abG * abG + abB * abB

                    val apR = pr - r0
                    val apG = pg - g0
                    val apB = pb - b0

                    var t = if (abLenSq == 0f) 0f else (apR * abR + apG * abG + apB * abB) / abLenSq
                    t = t.coerceIn(0f, 1f)

                    val projR = r0 + t * abR
                    val projG = g0 + t * abG
                    val projB = b0 + t * abB

                    val distSq = (pr - projR) * (pr - projR) + (pg - projG) * (pg - projG) + (pb - projB) * (pb - projB)
                    
                    // Add penalty for segment length to disambiguate collinear segments (like grayscale)
                    val score = distSq + 0.001f * abLenSq

                    if (score < bestScore) {
                        bestScore = score
                        bestD = distSq
                        bestC0 = origEntries[i].value
                        bestC1 = origEntries[j].value
                        bestT = t
                    }
                }
            }

            // Apply the closest mapped color or segment unconditionally
            if (bestC0 == bestC1 || bestT <= 0f) {
                val nc = newColors[bestC0]
                pixels[idx] = (alpha shl 24) or (nc and 0x00FFFFFF)
            } else if (bestT >= 1f) {
                val nc = newColors[bestC1]
                pixels[idx] = (alpha shl 24) or (nc and 0x00FFFFFF)
            } else {
                val nc0 = newColors[bestC0]
                val nc1 = newColors[bestC1]

                val nr0 = (nc0 ushr 16) and 0xFF
                val ng0 = (nc0 ushr 8) and 0xFF
                val nb0 = nc0 and 0xFF

                val nr1 = (nc1 ushr 16) and 0xFF
                val ng1 = (nc1 ushr 8) and 0xFF
                val nb1 = nc1 and 0xFF

                val nr = (nr0 + bestT * (nr1 - nr0)).toInt().coerceIn(0, 255)
                val ng = (ng0 + bestT * (ng1 - ng0)).toInt().coerceIn(0, 255)
                val nb = (nb0 + bestT * (nb1 - nb0)).toInt().coerceIn(0, 255)

                val nc = (nr shl 16) or (ng shl 8) or nb
                pixels[idx] = (alpha shl 24) or nc
            }
        }

        // Pass 2: Flood-fill from all border pixels to mark true background transparent pixels.
        // Any semi-transparent pixel NOT reachable from the outside = internal SVG seam → boost alpha to 255.
        val isBackground = BooleanArray(targetW * targetH) { false }
        val queue = ArrayDeque<Int>()

        // Seed the queue with all transparent pixels on the image border
        for (x in 0 until targetW) {
            for (y in intArrayOf(0, targetH - 1)) {
                val idx = y * targetW + x
                if ((pixels[idx] ushr 24) < 128) { isBackground[idx] = true; queue.add(idx) }
            }
        }
        for (y in 1 until targetH - 1) {
            for (x in intArrayOf(0, targetW - 1)) {
                val idx = y * targetW + x
                if ((pixels[idx] ushr 24) < 128 && !isBackground[idx]) { isBackground[idx] = true; queue.add(idx) }
            }
        }

        // BFS: spread "background" label to connected semi-transparent neighbours
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val cx = cur % targetW
            val cy = cur / targetW
            for ((dx, dy) in arrayOf(intArrayOf(-1,0), intArrayOf(1,0), intArrayOf(0,-1), intArrayOf(0,1))) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx < 0 || nx >= targetW || ny < 0 || ny >= targetH) continue
                val nIdx = ny * targetW + nx
                if (!isBackground[nIdx] && (pixels[nIdx] ushr 24) < 128) {
                    isBackground[nIdx] = true
                    queue.add(nIdx)
                }
            }
        }

        // Any semi-transparent pixel that is NOT background is an internal seam — boost alpha
        for (i in pixels.indices) {
            if (!isBackground[i] && (pixels[i] ushr 24) in 1..254) {
                pixels[i] = (255 shl 24) or (pixels[i] and 0x00FFFFFF)
            }
        }

        bitmap.setPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        bitmapCache[cacheKey] = bitmap
        return BitmapDrawable(context.resources, bitmap)
    }

    // ── Colour-manipulation helpers (used by Tab 2 seekbar logic) ─────────────

    /**
     * Builds an ARGB colour from hue (0–359) and lightness (0–100 as int).
     * 0–50: Black to Full Color
     * 50–100: Full Color to White
     * Alpha is always 0xFF (fully opaque).
     */
    fun makeColor(hueDegrees: Int, lightness100: Int): Int {
        val l = lightness100.coerceIn(0, 100)
        val s = if (l <= 50) FIXED_SATURATION else FIXED_SATURATION * (1f - (l - 50) / 50f)
        val v = if (l <= 50) l / 50f else 1f
        val hsv = floatArrayOf((hueDegrees % 360).toFloat(), s, v)
        return Color.HSVToColor(hsv)
    }

    /**
     * Returns the hue (0–359) of [color] in HSV space.
     * Returns 0 for achromatic colours.
     */
    fun getHue(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return hsv[0].toInt()
    }

    /**
     * Returns the lightness (0–100) of [color] matching the makeColor gradient.
     */
    fun getLightness100(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val s = hsv[1]
        val v = hsv[2]
        return if (s < FIXED_SATURATION - 0.05f) {
            (50 + 50 * (1f - s / FIXED_SATURATION)).toInt().coerceIn(50, 100)
        } else {
            (50 * v).toInt().coerceIn(0, 50)
        }
    }

    /**
     * Legacy single-seekbar helper kept for frame colour seekbars.
     * Shifts hue while boosting saturation so the change is always visible.
     */
    fun shiftHue(baseColor: Int, hueDegrees: Int, minSaturation: Float = 0.65f): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        hsv[0] = (hueDegrees % 360).toFloat()
        if (hsv[1] < minSaturation) hsv[1] = minSaturation
        if (hsv[2] < 0.12f) hsv[2] = 0.35f
        val rgb = Color.HSVToColor(hsv)
        val alpha = (baseColor ushr 24) and 0xFF
        return (alpha shl 24) or (rgb and 0x00FFFFFF)
    }

    internal const val FIXED_SATURATION = 0.90f
}
