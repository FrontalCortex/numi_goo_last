package com.example.app.abacus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.example.app.R
import kotlin.math.abs

/**
 * Builds the abacusContainer background drawable with user colour overrides.
 *
 * FRAME_BG  (abacus_frame_bg — LayerDrawable):
 *   Layer 1 (the wooden fill) is recoloured via [GradientDrawable.setColor].
 *
 * FRAME_BG2 (abacus_frame_bg2 — VectorDrawable with clip-path hearts):
 *   Rasterised to a bitmap at device DPI, then colours are replaced using a
 *   **two-nearest-neighbour interpolation** strategy so that anti-aliased edge
 *   pixels between two original colours are replaced with an equivalent blend
 *   of the two new colours.  This keeps heart edges perfectly smooth.
 */
object AbacusFrameRenderer {

    // ── Known colours in abacus_frame_bg2 ────────────────────────────────────
    private val ORIG_WOOD  = Color.parseColor("#DDA379")
    private val ORIG_HEART = Color.parseColor("#E63946")
    private val ORIG_BLACK = Color.parseColor("#000000")
    private val ORIG_DARK  = Color.parseColor("#212226")

    /** All four original colours, in order.  Index matches [NEW_COLORS] below. */
    private val ALL_ORIG = intArrayOf(ORIG_WOOD, ORIG_HEART, ORIG_BLACK, ORIG_DARK)

    // ── Public API ────────────────────────────────────────────────────────────

    fun buildFrameDrawable(context: Context): Drawable =
        when (AbacusPreferences.getFrameType(context)) {
            AbacusPreferences.FrameType.FRAME_BG  -> buildFrameBgDrawable(context)
            AbacusPreferences.FrameType.FRAME_BG2 -> buildFrameBg2Drawable(context)
        }

    // ── FRAME_BG ─────────────────────────────────────────────────────────────

    private fun buildFrameBgDrawable(context: Context): Drawable {
        val original = ContextCompat.getDrawable(context, R.drawable.abacus_frame_bg)!!
        val layerDrawable = (original.mutate() as? LayerDrawable) ?: return original
        val woodColor = AbacusPreferences.getFrameBgWoodColor(context)
        val woodLayer = layerDrawable.getDrawable(1).mutate()
        if (woodLayer is android.graphics.drawable.GradientDrawable) {
            woodLayer.setColor(woodColor)
        } else {
            woodLayer.setColorFilter(woodColor, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        return layerDrawable
    }

    // ── FRAME_BG2 ────────────────────────────────────────────────────────────

    /**
     * Rasterises abacus_frame_bg2 at device DPI and replaces colours using
     * two-nearest-neighbour blending so anti-aliased heart edges stay smooth.
     *
     * Algorithm per pixel:
     *  1. Find the two closest original colours (d0 ≤ d1).
     *  2. Compute blend ratio: t = d0 / (d0 + d1).
     *     - t ≈ 0  → pixel is almost entirely the closest original colour.
     *     - t ≈ 0.5 → pixel is halfway between the two.
     *  3. New pixel = lerp(newColor0, newColor1, t).
     *
     * This means anti-aliased edge pixels (e.g. a half-heart, half-wood pixel)
     * are blended proportionally between the new heart colour and new wood colour,
     * preserving the smooth edge appearance of the VectorDrawable.
     */
    private fun buildFrameBg2Drawable(context: Context): Drawable {
        val src = ContextCompat.getDrawable(context, R.drawable.abacus_frame_bg2)!!

        // Fixed 400 px render — good enough for sharp hearts with the interpolation
        // algorithm below.  Do NOT scale by density here: BitmapDrawable derives its
        // intrinsic size from the bitmap's pixel count, so a density-scaled bitmap
        // would make the view's wrap_content swell to fill the screen.
        val size = 400

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        src.setBounds(0, 0, size, size)
        src.draw(Canvas(bitmap))

        // Tell the bitmap it was rendered at screen density so BitmapDrawable
        // reports a correct intrinsic size in dp (≈ 400/density dp) rather than
        // treating every pixel as one dp.
        bitmap.density = context.resources.displayMetrics.densityDpi

        // Build the "new colours" array — same order as ALL_ORIG
        val woodNew  = AbacusPreferences.getFrameBg2WoodColor(context)
        val heartNew = AbacusPreferences.getFrameBg2HeartColor(context)
        val newColors = intArrayOf(woodNew, heartNew, ORIG_BLACK, ORIG_DARK) // black/dark unchanged

        val rOrig = IntArray(4); val gOrig = IntArray(4); val bOrig = IntArray(4)
        for(k in 0..3) {
            rOrig[k] = (ALL_ORIG[k] ushr 16) and 0xFF
            gOrig[k] = (ALL_ORIG[k] ushr 8) and 0xFF
            bOrig[k] = ALL_ORIG[k] and 0xFF
        }

        // The 3 valid edges (pairs of colours blending into each other)
        // 0: Wood(0)-Heart(1),  1: Wood(0)-Black(2),  2: Black(2)-Dark(3)
        val edges = intArrayOf(0, 1,  0, 2,  2, 3)

        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)

        for (i in pixels.indices) {
            val alpha = (pixels[i] ushr 24) and 0xFF
            if (alpha < 10) continue

            val pr = (pixels[i] ushr 16) and 0xFF
            val pg = (pixels[i] ushr 8) and 0xFF
            val pb = pixels[i] and 0xFF

            var bestDist2 = Float.MAX_VALUE
            var bestT = 0f
            var bestNc0 = 0
            var bestNc1 = 0

            // Find which physical edge (line segment in RGB space) the pixel is closest to
            for (e in 0..2) {
                val idxA = edges[e * 2]
                val idxB = edges[e * 2 + 1]

                val rA = rOrig[idxA]; val gA = gOrig[idxA]; val bA = bOrig[idxA]
                val rB = rOrig[idxB]; val gB = gOrig[idxB]; val bB = bOrig[idxB]

                val dr = rB - rA; val dg = gB - gA; val db = bB - bA
                val len2 = (dr * dr + dg * dg + db * db).toFloat()
                val dPr = pr - rA; val dPg = pg - gA; val dPb = pb - bA

                // Project pixel onto line segment to find interpolation parameter t (0 to 1)
                var t = if (len2 == 0f) 0f else (dPr * dr + dPg * dg + dPb * db) / len2
                if (t < 0f) t = 0f else if (t > 1f) t = 1f

                val projR = rA + t * dr
                val projG = gA + t * dg
                val projB = bA + t * db

                val dist2 = (pr - projR) * (pr - projR) + (pg - projG) * (pg - projG) + (pb - projB) * (pb - projB)

                if (dist2 < bestDist2) {
                    bestDist2 = dist2
                    bestT = t
                    bestNc0 = newColors[idxA]
                    bestNc1 = newColors[idxB]
                }
            }

            // Interpolate new colours using the exact ratio t
            val r = lerp((bestNc0 ushr 16) and 0xFF, (bestNc1 ushr 16) and 0xFF, bestT)
            val g = lerp((bestNc0 ushr 8)  and 0xFF, (bestNc1 ushr 8)  and 0xFF, bestT)
            val b = lerp(bestNc0 and 0xFF,            bestNc1 and 0xFF,            bestT)

            pixels[i] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return BitmapDrawable(context.resources, bitmap)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun lerp(a: Int, b: Int, t: Float): Int =
        (a + (b - a) * t + 0.5f).toInt().coerceIn(0, 255)
}
