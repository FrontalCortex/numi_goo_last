package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Kayıttaki ilerleme yolu (günlük hedef sorusundan sonra): çocuğun seçtiği hedefle nereye
 * varacağını üç adımda gösteriyor.
 *
 * ## Görünüm
 *  - Soldan sağa yükselen bir abaküs teli; adımlar telin üstündeki yuvarlak boncuklar (içinde
 *    ikon), yazılar boncukların altında. Tel boncuklarda yatay, aralarda yükseliyor (basamak
 *    gibi): yazılar telin yükselen kısmıyla çakışmasın diye. Telin ucunda altın bir ok başı.
 *  - Renk soldan sağa maviden yeşile, yeşilden altına; boncuk halkaları ve zaman etiketleri
 *    telin o noktadaki rengini alıyor.
 *  - Altta zaman çizgisi; adımların hizasında noktalar, altlarında zaman etiketleri.
 *
 * ## Animasyon ([playIntro])
 * Tel ve zaman çizgisi soldan sağa çiziliyor, ok başı telin ucunda beliriyor; sonra her adım için
 * sırayla boncuk → yazı → zaman etiketi balon gibi (0 → %120 → %100) beliriyor. Etiket
 * belirirken o adımın noktası da renkleniyor. Animasyon oynamıyorsa son kare çiziliyor.
 *
 * Mimo'nun benzer ekranından esinlenildi ama kopyası değil: metinler, ikonlar ve renkler bizim;
 * ok düz bir eğri değil, boncuklarda düzleşen basamaklı bir abaküs teli.
 */
class LearningPathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Yoldaki bir adım. */
    class Milestone(
        @DrawableRes val iconRes: Int,
        val text: CharSequence,
        val timeLabel: String,
    )

    private val dp = resources.displayMetrics.density
    private fun sp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private var milestones: List<Milestone> = emptyList()
    private var icons: List<Drawable?> = emptyList()

    // ── Ölçüler (computeLayout'ta, genişliğe göre) ──────────────────────────
    private var laidOutWidth = -1
    private var desiredHeight = 0
    private val xs = FloatArray(STEPS)
    private val ys = FloatArray(STEPS)
    private val textTops = FloatArray(STEPS)
    private var textLayouts: List<StaticLayout> = emptyList()
    private var textWidth = 0
    private val rodPath = Path()
    private val rodPart = Path()
    private val rodMeasure = PathMeasure()
    private var rodLength = 0f
    private var rodEndX = 0f
    private var rodEndY = 0f
    /** Ok başı, yerel koordinatta: tabanı (telin ucu) 0,0'da, ucu +x yönünde. */
    private val arrowHead = Path()
    private var timelineY = 0f
    private var timelineStart = 0f
    private var timelineEnd = 0f
    private var labelBaseline = 0f
    private val stepColors = IntArray(STEPS)

    // ── Boyalar ─────────────────────────────────────────────────────────────
    private val rodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f * dp
        strokeCap = Paint.Cap.ROUND
    }
    /** Telin üstündeki ince parlama: düz bir çizgi değil, yuvarlak bir tel gibi görünsün. */
    private val rodShine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * dp
        strokeCap = Paint.Cap.ROUND
        color = 0x55FFFFFF
    }
    /**
     * Boncuğun içi açık: ikonların (abaküs, alev, kristal) siyah dış çizgileri koyu zeminde
     * kayboluyordu, açık bir dairede çıkartma gibi okunuyorlar.
     */
    private val holderFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EEF2.toInt()
    }
    /** Ok başı telin son rengiyle (altın); köşeler yuvarlak, sivri üçgen sert duruyordu. */
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3f * dp
        strokeJoin = Paint.Join.ROUND
        color = ROD_COLORS.last()
    }
    private val holderRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * dp
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * dp
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.missions_track)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dark_text)
        textSize = sp(15f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    /** Animasyonun başından beri geçen süre (ms); animasyon yokken son kare. */
    private var elapsed = TOTAL_MS.toFloat()
    private var animator: ValueAnimator? = null

    /** Üç adımı verir; çizim ve ölçüler yenilenir, animasyon başlatılmaz. */
    fun setMilestones(list: List<Milestone>) {
        require(list.size == STEPS) { "İlerleme yolu $STEPS adım bekliyor, ${list.size} geldi" }
        milestones = list
        icons = list.map { ContextCompat.getDrawable(context, it.iconRes)?.mutate() }
        // Ekran okuyucu çizimi göremiyor; adımları sırayla okusun.
        contentDescription = list.joinToString(". ") { "${it.text}: ${it.timeLabel}" }
        laidOutWidth = -1
        requestLayout()
        invalidate()
    }

    /**
     * Çizimi baştan oynatır.
     *
     * @param startDelayMs Ekrana kayarak geliyorsa kayma bitince başlasın diye: kayarken çizilen
     *   ilk kareler görünmüyor, tel yarıda başlamış gibi duruyordu.
     * @param onEnd Çizim sonuna kadar oynayınca çağrılır. Yarıda kesilirse (yeniden oynatma,
     *   görünümün ekrandan kalkması) çağrılmaz: yeniden oynatmada yalnızca yenisinin bitişi
     *   sayılmalı, yoksa eskisi düğmeyi erkenden açardı.
     */
    fun playIntro(startDelayMs: Long = 0L, onEnd: (() -> Unit)? = null) {
        animator?.cancel()
        elapsed = 0f
        invalidate()
        animator = ValueAnimator.ofFloat(0f, TOTAL_MS.toFloat()).apply {
            duration = TOTAL_MS
            startDelay = startDelayMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                elapsed = it.animatedValue as Float
                invalidate()
            }
            if (onEnd != null) {
                addListener(object : AnimatorListenerAdapter() {
                    private var cancelled = false
                    override fun onAnimationCancel(animation: Animator) {
                        cancelled = true
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        if (!cancelled) onEnd()
                    }
                })
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        elapsed = TOTAL_MS.toFloat()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        if (w > 0 && w != laidOutWidth) computeLayout(w)
        setMeasuredDimension(w, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && w != laidOutWidth) computeLayout(w)
    }

    /**
     * Yerleşim. Yukarıdan aşağı: ok başı payı, 3. boncuk, 2. ve 1. boncuk (her biri bir basamak
     * aşağıda), boncukların altında yazılar, en altta zaman çizgisi ve etiketler.
     */
    private fun computeLayout(w: Int) {
        laidOutWidth = w
        val width = w.toFloat()
        val pad = 4f * dp
        val r = HOLDER_R * dp
        val rise = STEP_RISE * dp

        textWidth = min(width * 0.29f, 132f * dp).toInt()
        textLayouts = milestones.map { m ->
            StaticLayout.Builder.obtain(m.text, 0, m.text.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(2f * dp, 1f)
                .build()
        }

        xs[0] = width * 0.16f
        xs[1] = width * 0.49f
        xs[2] = width * 0.78f

        // Ok başı tabanından (telin ucu) büyüyor; %120'ye taştığında ucu kesilmesin diye
        // sağda ve üstte o kadar pay var.
        val arrowLen = ARROW_LEN * dp
        val halfW = ARROW_HALF_W * dp
        val back = ARROW_BACK * dp
        arrowHead.reset()
        arrowHead.moveTo(arrowLen, 0f)
        arrowHead.lineTo(-back, -halfW)
        arrowHead.lineTo(ARROW_NOTCH * dp, 0f)  // arkadaki çentik: tel ok başına giriyor
        arrowHead.lineTo(-back, halfW)
        arrowHead.close()
        val edge = arrowPaint.strokeWidth / 2f
        rodEndX = width - pad - arrowLen * cos(ARROW_ANGLE) * OVERSHOOT - edge
        rodEndY = pad + arrowLen * sin(ARROW_ANGLE) * OVERSHOOT + edge
        ys[2] = rodEndY + TAIL_RISE * dp
        ys[1] = ys[2] + rise
        ys[0] = ys[1] + rise

        var textBottom = ys[0] + r
        for (i in 0 until STEPS) {
            textTops[i] = ys[i] + r + TEXT_GAP * dp
            textLayouts.getOrNull(i)?.let { textBottom = max(textBottom, textTops[i] + it.height) }
        }
        timelineY = textBottom + 20f * dp
        timelineStart = pad + trackPaint.strokeWidth
        timelineEnd = width - pad - trackPaint.strokeWidth
        labelBaseline = timelineY + 24f * dp
        desiredHeight = (labelBaseline + labelPaint.descent() + pad + 4f * dp).roundToInt()

        rodPath.reset()
        val sx = pad + rodPaint.strokeWidth / 2f
        val sy = ys[0] + START_DROP * dp
        rodPath.moveTo(sx, sy)
        stepCurve(sx, sy, xs[0], ys[0])
        stepCurve(xs[0], ys[0], xs[1], ys[1])
        stepCurve(xs[1], ys[1], xs[2], ys[2])
        // Son parça boncuktan yatay çıkıp ok başının eğimiyle ucuna varıyor.
        val tdx = rodEndX - xs[2]
        val len = tdx * 0.55f
        rodPath.cubicTo(
            xs[2] + tdx * 0.5f, ys[2],
            rodEndX - len * cos(ARROW_ANGLE), rodEndY + len * sin(ARROW_ANGLE),
            rodEndX, rodEndY,
        )
        rodMeasure.setPath(rodPath, false)
        rodLength = rodMeasure.length

        rodPaint.shader = LinearGradient(0f, 0f, width, 0f, ROD_COLORS, null, Shader.TileMode.CLAMP)
        for (i in 0 until STEPS) stepColors[i] = colorAt(xs[i] / width)
    }

    /** Basamak eğrisi: iki uçta da yatay, arada yumuşakça yükseliyor. */
    private fun stepCurve(x0: Float, y0: Float, x1: Float, y1: Float) {
        val half = (x1 - x0) / 2f
        rodPath.cubicTo(x0 + half, y0, x1 - half, y1, x1, y1)
    }

    /** Telin gradyanındaki renk; [f] soldan sağa 0..1. */
    private fun colorAt(f: Float): Int {
        val c = f.coerceIn(0f, 1f)
        return if (c < 0.5f) ColorUtils.blendARGB(ROD_COLORS[0], ROD_COLORS[1], c / 0.5f)
        else ColorUtils.blendARGB(ROD_COLORS[1], ROD_COLORS[2], (c - 0.5f) / 0.5f)
    }

    override fun onDraw(canvas: Canvas) {
        if (milestones.size != STEPS || laidOutWidth <= 0) return
        val t = elapsed

        // Tel ve zaman çizgisi birlikte soldan sağa.
        val drawP = smooth((t / ROD_MS).coerceIn(0f, 1f))
        val lineEnd = timelineStart + (timelineEnd - timelineStart) * drawP
        if (drawP > 0f) {
            rodPart.reset()
            rodMeasure.getSegment(0f, rodLength * drawP, rodPart, true)
            canvas.drawPath(rodPart, rodPaint)
            canvas.save()
            canvas.translate(0f, -1.8f * dp)
            canvas.drawPath(rodPart, rodShine)
            canvas.restore()

            canvas.drawLine(timelineStart, timelineY, lineEnd, timelineY, trackPaint)
        }
        drawArrow(canvas, progress(t, ARROW_AT))

        for (i in 0 until STEPS) {
            val base = STEP_START + i * STEP_GAP
            val labelP = progress(t, base + 2 * ITEM_GAP)
            // Nokta çizgi ona varınca gri beliriyor, etiketi gelince adımın rengine dönüyor.
            if (drawP > 0f && lineEnd >= xs[i]) drawDot(canvas, i, labelP)
            drawHolder(canvas, i, progress(t, base))
            drawText(canvas, i, progress(t, base + ITEM_GAP))
            drawLabel(canvas, i, labelP)
        }
    }

    /** Ok başı telin ucunda, telin son eğimiyle; tabanından büyüyerek beliriyor. */
    private fun drawArrow(canvas: Canvas, p: Float) {
        if (p <= 0f) return
        val s = pop(p)
        canvas.save()
        canvas.translate(rodEndX, rodEndY)
        canvas.rotate(-ARROW_ANGLE_DEG)  // ekranda y aşağı: yukarı-sağa bakması için eksi
        canvas.scale(s, s)
        arrowPaint.alpha = alphaOf(p)
        canvas.drawPath(arrowHead, arrowPaint)
        canvas.restore()
    }

    private fun drawDot(canvas: Canvas, i: Int, litP: Float) {
        dotPaint.color = ColorUtils.blendARGB(trackPaint.color, stepColors[i], litP)
        val radius = 5f * dp * max(1f, pop(litP))
        canvas.drawCircle(xs[i], timelineY, radius, dotPaint)
    }

    private fun drawHolder(canvas: Canvas, i: Int, p: Float) {
        if (p <= 0f) return
        val s = pop(p)
        val a = alphaOf(p)
        val r = HOLDER_R * dp
        canvas.save()
        canvas.translate(xs[i], ys[i])
        canvas.scale(s, s)
        holderFill.alpha = a
        canvas.drawCircle(0f, 0f, r, holderFill)
        holderRing.color = stepColors[i]
        holderRing.alpha = a
        canvas.drawCircle(0f, 0f, r - holderRing.strokeWidth / 2f, holderRing)
        icons.getOrNull(i)?.let { icon ->
            val half = (ICON_DP * dp / 2f).roundToInt()
            icon.setBounds(-half, -half, half, half)
            icon.alpha = a
            icon.draw(canvas)
        }
        canvas.restore()
    }

    /** Yazı boncuğun hemen altından, üst ortasından büyüyerek çıkıyor. */
    private fun drawText(canvas: Canvas, i: Int, p: Float) {
        val layout = textLayouts.getOrNull(i) ?: return
        if (p <= 0f) return
        val s = pop(p)
        val count = canvas.save()
        canvas.translate(xs[i] - textWidth / 2f, textTops[i])
        canvas.scale(s, s, textWidth / 2f, 0f)
        // Renkli kısımlar (span) boyanın saydamlığını almıyor; katmanla birlikte soluyor.
        val a = alphaOf(p)
        if (a < 255) {
            canvas.saveLayerAlpha(0f, 0f, textWidth.toFloat(), layout.height.toFloat(), a)
        }
        layout.draw(canvas)
        canvas.restoreToCount(count)
    }

    private fun drawLabel(canvas: Canvas, i: Int, p: Float) {
        if (p <= 0f) return
        val s = pop(p)
        labelPaint.color = stepColors[i]
        labelPaint.alpha = alphaOf(p)
        canvas.save()
        canvas.scale(s, s, xs[i], labelBaseline - labelPaint.textSize / 3f)
        canvas.drawText(milestones[i].timeLabel, xs[i], labelBaseline, labelPaint)
        canvas.restore()
    }

    private fun progress(t: Float, start: Float) = ((t - start) / POP_MS).coerceIn(0f, 1f)

    /** Balon gibi belirme: 0 → %120 (hızlı) → %100 (yerine oturma). */
    private fun pop(p: Float): Float = when {
        p <= 0f -> 0f
        p >= 1f -> 1f
        p < POP_PEAK -> {
            val x = p / POP_PEAK
            OVERSHOOT * (1f - (1f - x) * (1f - x))
        }
        else -> OVERSHOOT - (OVERSHOOT - 1f) * smooth((p - POP_PEAK) / (1f - POP_PEAK))
    }

    private fun alphaOf(p: Float) = (255f * (p / 0.3f).coerceIn(0f, 1f)).roundToInt()

    private fun smooth(x: Float) = x * x * (3f - 2f * x)

    private companion object {
        const val STEPS = 3

        const val HOLDER_R = 26f       // boncuk yarıçapı (dp)
        const val ICON_DP = 32f
        const val STEP_RISE = 56f      // basamak yüksekliği (dp)
        const val TEXT_GAP = 12f       // boncukla yazı arası (dp)
        const val START_DROP = 22f     // telin başı 1. boncuğun ne kadar altında (dp)
        const val TAIL_RISE = 28f      // ok başının tabanı 3. boncuğun ne kadar üstünde (dp)

        // Ok başı (dp): tabandan uca uzunluk, yarı genişlik, arka köşelerin tabandan geriliği,
        // arkadaki çentiğin derinliği. Eğim telin son parçasıyla aynı.
        const val ARROW_LEN = 16f
        const val ARROW_HALF_W = 11f
        const val ARROW_BACK = 4f
        const val ARROW_NOTCH = 1f
        const val ARROW_ANGLE_DEG = 40f
        val ARROW_ANGLE = Math.toRadians(ARROW_ANGLE_DEG.toDouble()).toFloat()

        const val OVERSHOOT = 1.2f
        const val POP_PEAK = 0.55f

        // Zamanlama (ms)
        const val ROD_MS = 900f
        const val ARROW_AT = 800f      // tel ucuna varmak üzereyken
        const val STEP_START = 1000f
        const val STEP_GAP = 560f
        const val ITEM_GAP = 150f
        const val POP_MS = 360f
        val TOTAL_MS = (STEP_START + 2 * STEP_GAP + 2 * ITEM_GAP + POP_MS).toLong()

        /** Mavi (başlangıç) → yeşil → altın (hedef). */
        val ROD_COLORS = intArrayOf(0xFF58A6FF.toInt(), 0xFF8CC63F.toInt(), 0xFFFFD54F.toInt())
    }
}
