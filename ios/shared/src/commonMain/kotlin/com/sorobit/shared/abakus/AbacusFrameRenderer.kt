// Kaynak (Android): app/src/main/java/com/example/app/abacus/AbacusFrameRenderer.kt
//
// Çerçeve çiziminin görünümden bağımsız kısmı: tip → kaynak adı, özgün renkler, renk seçimi
// (override → kayıtlı tercih) ve piksel düzeyinde renk değiştirme algoritması.
// Bitmap/Drawable/Canvas üretimi platform katmanında kalır.
//
// Android'den farklar: `R.drawable.x` → "x"; `Color.parseColor` → ARGB sabiti.
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.FrameType

object AbacusFrameRenderer {

    // ── abacus_frame_bg2 / bg3 içindeki bilinen renkler ───────────────────────
    const val ORIG_WOOD = 0xFFDDA379.toInt()
    const val ORIG_HEART = 0xFFE63946.toInt()
    const val ORIG_STAR = 0xFFDDFF33.toInt()
    const val ORIG_BLACK = 0xFF000000.toInt()
    const val ORIG_DARK = 0xFF212226.toInt()

    /** FRAME_BG2 özgün renkleri (sıra yeni renk dizisiyle aynı). */
    val ALL_ORIG = intArrayOf(ORIG_WOOD, ORIG_HEART, ORIG_BLACK, ORIG_DARK)

    /** FRAME_BG3 özgün renkleri. */
    val ALL_ORIG_BG3 = intArrayOf(ORIG_WOOD, ORIG_STAR, ORIG_BLACK, ORIG_DARK)

    /**
     * Birbirine karışan 3 geçerli kenar (renk çifti):
     * 0: Ahşap(0)-Kalp/Yıldız(1), 1: Ahşap(0)-Siyah(2), 2: Siyah(2)-Koyu(3)
     */
    val EDGES = intArrayOf(0, 1, 0, 2, 2, 3)

    /** FRAME_BG2/BG3/BG4 sabit raster boyutu (px; yoğunlukla ÖLÇEKLENMEZ). */
    const val RENDER_SIZE = 400

    /** Çerçeve kaynak adı; FRAME_BG4 kaynaksız çizilir (null). */
    fun frameResName(type: FrameType): String? = when (type) {
        FrameType.FRAME_BG -> "abacus_frame_bg"
        FrameType.FRAME_BG2 -> "abacus_frame_bg2"
        FrameType.FRAME_BG3 -> "abacus_frame_bg3"
        FrameType.FRAME_BG4 -> null
    }

    /** Android: `frameTypeOverride ?: AbacusPreferences.getFrameType(context)`. */
    fun resolveFrameType(store: AbacusPrefsStore, frameTypeOverride: FrameType? = null): FrameType =
        frameTypeOverride ?: AbacusPreferences.getFrameType(store)

    /**
     * Çizimde kullanılacak kullanıcı renkleri ([type]'a göre sıra: FRAME_BG → [ahşap];
     * FRAME_BG2 → [ahşap, kalp]; FRAME_BG3 → [ahşap, yıldız]; FRAME_BG4 → 5 gradyan rengi).
     *
     * DİKKAT (Android davranışı): BG/BG2/BG3'te override dizisi eleman eleman `getOrNull` ile
     * okunur (eksik eleman kayıtlı tercihe düşer); BG4'te override varsa TAMAMI kullanılır
     * (kısa dizi verilirse Android'de gradyan oluşturma hata verir).
     */
    fun resolveFrameColors(store: AbacusPrefsStore, type: FrameType, colorsOverride: IntArray? = null): IntArray =
        when (type) {
            FrameType.FRAME_BG -> intArrayOf(
                colorsOverride?.getOrNull(0) ?: AbacusPreferences.getFrameBgWoodColor(store),
            )
            FrameType.FRAME_BG2 -> intArrayOf(
                colorsOverride?.getOrNull(0) ?: AbacusPreferences.getFrameBg2WoodColor(store),
                colorsOverride?.getOrNull(1) ?: AbacusPreferences.getFrameBg2HeartColor(store),
            )
            FrameType.FRAME_BG3 -> intArrayOf(
                colorsOverride?.getOrNull(0) ?: AbacusPreferences.getFrameBg3WoodColor(store),
                colorsOverride?.getOrNull(1) ?: AbacusPreferences.getFrameBg3StarColor(store),
            )
            FrameType.FRAME_BG4 -> colorsOverride ?: AbacusPreferences.getFrameBg4Colors(store)
        }

    /** FRAME_BG2/BG3 için yeni renk dizisi: [ahşap, kalp/yıldız, siyah, koyu] (son ikisi değişmez). */
    fun newColorsFor(woodNew: Int, accentNew: Int): IntArray = intArrayOf(woodNew, accentNew, ORIG_BLACK, ORIG_DARK)

    /**
     * FRAME_BG2 / FRAME_BG3 piksel renk değiştirme ([pixels] ARGB; YERİNDE değişir).
     *
     * Piksel başına: RGB uzayında en yakın kenar (doğru parçası) bulunur, piksel o parçaya
     * izdüşürülerek t (0..1) hesaplanır ve yeni piksel = lerp(yeniRenkA, yeniRenkB, t) olur.
     * Böylece kenar yumuşatma pikselleri de orantılı harmanlanır. Alfa < 10 pikseller atlanır.
     */
    fun replaceFramePixels(pixels: IntArray, origColors: IntArray, newColors: IntArray) {
        val rOrig = IntArray(4); val gOrig = IntArray(4); val bOrig = IntArray(4)
        for (k in 0..3) {
            rOrig[k] = (origColors[k] ushr 16) and 0xFF
            gOrig[k] = (origColors[k] ushr 8) and 0xFF
            bOrig[k] = origColors[k] and 0xFF
        }

        val edges = EDGES

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

            for (e in 0..2) {
                val idxA = edges[e * 2]
                val idxB = edges[e * 2 + 1]

                val rA = rOrig[idxA]; val gA = gOrig[idxA]; val bA = bOrig[idxA]
                val rB = rOrig[idxB]; val gB = gOrig[idxB]; val bB = bOrig[idxB]

                val dr = rB - rA; val dg = gB - gA; val db = bB - bA
                val len2 = (dr * dr + dg * dg + db * db).toFloat()
                val dPr = pr - rA; val dPg = pg - gA; val dPb = pb - bA

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

            val r = lerp((bestNc0 ushr 16) and 0xFF, (bestNc1 ushr 16) and 0xFF, bestT)
            val g = lerp((bestNc0 ushr 8) and 0xFF, (bestNc1 ushr 8) and 0xFF, bestT)
            val b = lerp(bestNc0 and 0xFF, bestNc1 and 0xFF, bestT)

            pixels[i] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    /** Android'de private. */
    fun lerp(a: Int, b: Int, t: Float): Int =
        (a + (b - a) * t + 0.5f).toInt().coerceIn(0, 255)

    // ── FRAME_BG4 (kaynaksız çizim) ───────────────────────────────────────────

    /** 400×400 tuval üzerinde yuvarlatılmış dikdörtgen katmanı. */
    data class RoundRectLayer(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val radius: Float,
        /** Düz renk (ARGB); gradyan katmanında null. */
        val color: Int?,
    )

    /**
     * FRAME_BG4 katmanları, çizim sırasıyla: dış siyah sınır, gradyan çerçeve (color = null;
     * (0,400) → (400,0) doğrultusunda [BG4_GRADIENT_STOPS] durakları, CLAMP), iç siyah sınır,
     * iç arka plan (#212226).
     */
    val BG4_LAYERS: List<RoundRectLayer> = listOf(
        RoundRectLayer(0f, 0f, 400f, 400f, 40f, 0xFF000000.toInt()),
        RoundRectLayer(5.6f, 5.6f, 394.4f, 394.4f, 34.4f, null),
        RoundRectLayer(28f, 28f, 372f, 372f, 12f, 0xFF000000.toInt()),
        RoundRectLayer(33.6f, 33.6f, 366.4f, 366.4f, 6.4f, ORIG_DARK),
    )

    val BG4_GRADIENT_STOPS = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
}
