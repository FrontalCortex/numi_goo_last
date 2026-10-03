package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tavşan maskotu: hazır bir çizimin ([BunnyMascotArt]) parçalarını ayrı ayrı hareket
 * ettirerek canlandırıyor.
 *
 * Bekleme hâl değil, hiçbir hâl oynamıyorken hep o var: hafifçe nefes alır gibi iner-kalkar,
 * kafasını çok az yana eğer, göz kırpar, bakışını kaydırır, ara ara bir kulağını oynatır.
 * Üstüne [play] ile bir [Emote] oynatılır; bitince kendiliğinden beklemeye döner.
 * [autoGreetIntervalMs] verilirse beklerken o aralıkla kendiliğinden selam veriyor.
 *
 * ## Nasıl çalışıyor
 * Her karede bir "duruş" hesaplanıyor: kolların görünürlüğü ve açıları, gövde/kafa
 * kayması ve dönmesi, kulaklar, bakış, yanak... ([Ch] kanalları). Oynayan hâl yalnızca
 * kendisini ilgilendiren kanalların HEDEFİNİ yazıyor; hedefler [cur]'da yumuşatıldığı
 * için hâller arası geçiş kendiliğinden yumuşak. Hızlı salınımlar (kol çırpma, kulak
 * kıpırdatma, zıplama) yumuşatılırsa sönerdi; onlar [osc]'ye doğrudan yazılıp üstüne
 * ekleniyor ve hâlin başında/sonunda bir zarfla girip çıkıyor. Göz ve ağız türü ise
 * ayrık: o karede hangi hâl oynuyorsa onunki.
 *
 * ## Parçalar
 * Tavşanın kendi çizimine ek olarak aynı koleksiyondaki başka hayvanlardan parçalar
 * kullanılıyor (tavşan rengine boyalı): kedinin dolgun kolları ve açık ağzı, domuzun
 * balonu, köpeğin önde birleşik elleri. Kollar arasında çapraz geçiş var: giden kol dışa
 * doğru kalkarak silinir, gelen kol aşağıdan gelir — kopuk durmasın diye.
 *
 * Koordinatlar kaynak çizimin koordinatları (800×800 içinde tavşan x≈317–470,
 * y≈50–315); görünümün kısa kenarına [UNITS] birimlik bir kare sığdırılıyor.
 *
 * Görünmezken (kendisi ya da bir üstü GONE/INVISIBLE, pencereden ayrılmış) kare çizmeyi
 * bırakıyor: [onVisibilityAggregated] döngüyü durdurup yeniden başlatıyor.
 */
class BunnyMascotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Oynatılabilen hâller ve süreleri. */
    enum class Emote(val durationMs: Long, val label: String) {
        GREET(1700, "Selam"),
        CHEER(1400, "Sevinç"),
        BALLOON(3500, "Balon"),
        SLEEP(4200, "Uyuma"),
        POINT(3000, "İşaret etme"),
        PEACE(2000, "Zafer işareti"),
        SAD(3200, "Üzgün"),
        DANCE(3200, "Dans"),
        THINK(3500, "Düşünme"),
        CLAP(2500, "Alkış"),
        SURPRISED(1800, "Şaşırma"),
        SHY(3200, "Utangaç"),
        WINK(1500, "Göz kırpma"),
        HEART(3000, "Kalp"),
        ABACUS(4200, "Abaküs"),
    }

    private enum class Eyes { NORMAL, HAPPY, CLOSED, WINK }
    private enum class Mouth { IDLE, HAPPY, TALK, O, SAD }

    /** Duruş kanalları; bkz. sınıf açıklaması. Açılarda artı = dışa/yukarı. */
    private object Ch {
        const val HANG_L_A = 0      // sarkan sol kol görünürlüğü
        const val HANG_R_A = 1      // sarkan sağ kol (sol kolun aynası)
        const val HANG_L = 2        // sarkan kolların dışa açısı
        const val HANG_R = 3
        const val CAT_L_A = 4       // kedi kolları (arkada)
        const val CAT_R_A = 5
        const val CAT_L = 6         // kedi kollarının kalkışı
        const val CAT_R = 7
        const val PEACE_A = 8       // zafer işaretli kol
        const val PEACE = 9
        const val FRONT_A = 10      // köpeğin önde birleşik elleri
        const val FRONT_GAP = 11    // ellerin iki yana açılması
        const val FRONT_ROT = 12    // ellerin kıpırdaması
        const val FRONT_DY = 13
        const val THINK_A = 14      // çeneye götürülen kol (önde)
        const val THINK = 15
        const val LEGS_OPEN = 16
        const val UPPER_ROT = 17    // gövdenin kalçadan sallanması
        const val HEAD_ROT = 18
        const val HEAD_DY = 19
        const val BODY_DX = 20      // bütün tavşanın yana kayması
        const val BODY_DY = 21      // üst gövdenin çökmesi
        const val LIFT = 22         // zıplama yüksekliği
        const val SQUASH = 23       // + ezilme, − uzama
        const val EAR_L = 24
        const val EAR_R = 25
        const val EYE_SCALE = 26    // gözlerin büyümesi (0 = normal)
        const val GAZE_X = 27
        const val GAZE_Y = 28
        const val BLUSH = 29
        const val BROWS = 30        // üzgün kaşlar
        const val BALLOON_A = 31
        const val BALLOON_SWAY = 32
        const val ABACUS_A = 33
        const val COUNT = 34
    }

    private val target = FloatArray(Ch.COUNT)
    private val cur = FloatArray(Ch.COUNT)
    private val osc = FloatArray(Ch.COUNT)
    private var eyes = Eyes.NORMAL
    private var mouth = Mouth.IDLE

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()
    private val mouthPath = Path()
    private val beadPath = Path()

    private val startMs = SystemClock.uptimeMillis()
    private var lastFrameMs = 0L
    private var running = false

    private var nextBlinkMs = startMs + 1500
    private var blinkStartMs = -1L
    private var idleGaze = 0f
    private var nextGazeMs = startMs + 2200

    // Kulak oynatma: hangi kulak (-1 sol, 1 sağ) ve ne zaman başladı.
    private var earFlickSide = 0
    private var earFlickStartMs = -1L
    private var nextEarFlickMs = startMs + 3000

    private var emote: Emote? = null
    private var emoteStartMs = 0L
    private var pointToLeft = false
    private var talkUntilMs = 0L
    private var nextAutoGreetMs = Long.MAX_VALUE

    /** Bir hâl kendi süresini doldurup beklemeye dönünce çağrılır (yarıda kesilince değil). */
    var onEmoteFinished: ((Emote) -> Unit)? = null

    /**
     * Beklerken kendiliğinden selam verme aralığı (bir hâlin başından selamın başına);
     * 0 kapalı. Başka bir hâl de sayacı baştan başlatıyor, yani hemen ardından selam gelmiyor.
     */
    var autoGreetIntervalMs: Long = 0L
        set(value) {
            field = value
            nextAutoGreetMs = if (value > 0) SystemClock.uptimeMillis() + value else Long.MAX_VALUE
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        cur[Ch.HANG_L_A] = 1f
        cur[Ch.HANG_R_A] = 1f
        cur[Ch.CAT_L] = CHEER_UP_L
        cur[Ch.CAT_R] = CHEER_UP_R
        cur[Ch.PEACE] = PEACE_REST
        cur[Ch.THINK] = THINK_UP
    }

    /** [emote]'u baştan oynatır; o an başka bir hâl oynuyorsa onu keser. */
    fun play(emote: Emote) {
        val now = SystemClock.uptimeMillis()
        this.emote = emote
        emoteStartMs = now
        talkUntilMs = 0L
        if (autoGreetIntervalMs > 0) nextAutoGreetMs = now + autoGreetIntervalMs
        invalidate()
    }

    /** Sağ elini kaldırıp sallar; bu sırada ağzı mutlu ve açık. */
    fun greet() = play(Emote.GREET)

    /** İki kez zıplar, kollarını yana ve yukarı açar, gözleri gülümser. */
    fun cheer() = play(Emote.CHEER)

    /** Bir yanı işaret eder (ilk kullanım rehberi gibi yerler için); varsayılan sağ. */
    fun point(toLeft: Boolean = false) {
        pointToLeft = toLeft
        play(Emote.POINT)
    }

    /** Ağzı [durationMs] boyunca konuşuyormuş gibi açılıp kapanır. */
    fun talk(durationMs: Long) {
        talkUntilMs = SystemClock.uptimeMillis() + durationMs
        invalidate()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        running = isVisible
        if (isVisible) {
            // Uzun bir aradan sonra ilk karede yumuşatmalar bir anda sıçramasın.
            lastFrameMs = 0L
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrameMs == 0L) 0f else (now - lastFrameMs).coerceAtMost(100L) / 1000f
        lastFrameMs = now
        val t = (now - startMs) / 1000f
        updateState(now, dt)

        val contentW = (width - paddingLeft - paddingRight).toFloat()
        val contentH = (height - paddingTop - paddingBottom).toFloat()
        val size = min(contentW, contentH)
        if (size > 0f) {
            canvas.save()
            canvas.translate(paddingLeft + (contentW - size) / 2f, paddingTop + (contentH - size) / 2f)
            canvas.scale(size / UNITS, size / UNITS)
            // Tavşanı yatayda ortala; üstte zıplarken kulakların sığacağı pay kalsın.
            canvas.translate(UNITS / 2f - CENTER_X, -TOP_Y)
            drawBunny(canvas, now, t)
            canvas.restore()
        }
        if (running) postInvalidateOnAnimation()
    }

    // ---------------------------------------------------------------- durum

    private fun updateState(now: Long, dt: Float) {
        if (blinkStartMs < 0 && now >= nextBlinkMs) blinkStartMs = now
        if (now >= nextGazeMs) {
            idleGaze = listOf(-1.5f, 0f, 0f, 1.5f).random()
            nextGazeMs = now + Random.nextLong(1500, 3500)
        }
        if (earFlickStartMs < 0 && now >= nextEarFlickMs) {
            earFlickSide = if (Random.nextBoolean()) 1 else -1
            earFlickStartMs = now
        }
        if (earFlickStartMs >= 0 && now - earFlickStartMs >= EAR_FLICK_MS) {
            earFlickStartMs = -1L
            nextEarFlickMs = now + Random.nextLong(2500, 6000)
        }

        val playing = emote
        if (playing != null && now - emoteStartMs >= playing.durationMs) {
            emote = null
            onEmoteFinished?.invoke(playing)
        }
        if (emote == null && now >= nextAutoGreetMs) play(Emote.GREET)

        setIdlePose()
        emote?.let { applyEmote(it, (now - emoteStartMs) / 1000f) }
        if (mouth == Mouth.IDLE && now < talkUntilMs) mouth = Mouth.TALK

        // Üstel yumuşatma: kare hızından bağımsız, hedefe hızla yaklaşıp yavaşlayarak oturuyor.
        val k = 1f - exp(-dt * 14f)
        for (i in 0 until Ch.COUNT) cur[i] += (target[i] - cur[i]) * k
    }

    /** Bekleme duruşu; oynayan hâl bunun üstüne yazıyor. */
    private fun setIdlePose() {
        target.fill(0f)
        osc.fill(0f)
        target[Ch.HANG_L_A] = 1f
        target[Ch.HANG_R_A] = 1f
        target[Ch.CAT_L] = CHEER_UP_L
        target[Ch.CAT_R] = CHEER_UP_R
        target[Ch.PEACE] = PEACE_REST
        target[Ch.THINK] = THINK_UP
        target[Ch.GAZE_X] = idleGaze
        eyes = Eyes.NORMAL
        mouth = Mouth.IDLE
    }

    /** Değer = yumuşatılmış hedef + doğrudan salınım. */
    private fun v(ch: Int) = cur[ch] + osc[ch]

    private fun applyEmote(em: Emote, e: Float) {
        val dur = em.durationMs / 1000f
        // Salınımların zarfı: başta 0,15 sn'de girip sonda 0,2 sn'de çıkıyor.
        val env = (e / 0.15f).coerceIn(0f, 1f) * ((dur - e) / 0.2f).coerceIn(0f, 1f)
        when (em) {
            Emote.GREET -> {
                // Sağ kedi kolu kalkıp sallanıyor; gövde kalçadan, kafa geriden, sol kol da
                // aynı ritimde sallanıyor; ayaklar hafifçe açılıyor.
                target[Ch.HANG_R_A] = 0f
                target[Ch.CAT_R_A] = 1f
                target[Ch.CAT_R] = WAVE_UP
                osc[Ch.CAT_R] = 12f * sin(e * TAU * 2.5f) * env
                target[Ch.LEGS_OPEN] = 1f
                osc[Ch.UPPER_ROT] = 3f * sin(e * TAU * 1.25f) * env
                osc[Ch.HEAD_ROT] = 3f * sin(e * TAU * 1.25f - 0.9f) * env
                osc[Ch.HANG_L] = 7f * sin(e * TAU * 1.25f + 0.6f) * env
                mouth = Mouth.HAPPY
            }
            Emote.CHEER -> {
                bothCatArms()
                val flap = 6f * sin(e * TAU * 3f) * env
                osc[Ch.CAT_L] = flap
                osc[Ch.CAT_R] = flap
                earsWiggle(e, env, base = 12f)
                // Gövde: iki sekme, yere değerken ezilme, havada uzama, inişten sonra
                // yaylanma; kafa gövdeyi biraz geriden izliyor; gövde sağa-sola kıvrılıyor.
                hop(e, hops = 2, hopS = CHEER_HOP_S, height = JUMP_HEIGHT)
                val bend = sin(PI.toFloat() * e / dur)
                osc[Ch.UPPER_ROT] = 4f * bend * sin(e * TAU * 2.2f)
                osc[Ch.HEAD_ROT] = 1.5f * bend * sin(e * TAU * 2.2f - 1.2f)
                eyes = Eyes.HAPPY
                mouth = Mouth.HAPPY
            }
            Emote.BALLOON -> {
                // Sağ elde domuzun balonu: el kalkarken balon büyüyerek beliriyor, sağa
                // yatık sarkaç gibi sallanıyor; tavşan balona bakıp hafif hafif sekiyor.
                target[Ch.HANG_R_A] = 0f
                target[Ch.CAT_R_A] = 1f
                target[Ch.CAT_R] = 25f
                target[Ch.BALLOON_A] = 1f
                target[Ch.BALLOON_SWAY] = 12f
                osc[Ch.BALLOON_SWAY] = 8f * sin(e * TAU / 1.6f) * env
                target[Ch.GAZE_X] = 1.5f
                target[Ch.GAZE_Y] = -2f
                target[Ch.HEAD_ROT] = 4f
                osc[Ch.LIFT] = 4f * abs(sin(e * TAU * 0.9f)) * env
                mouth = Mouth.HAPPY
            }
            Emote.SLEEP -> {
                // Gözler kapalı, kulaklar düşük, kafa yana devrik, derin ve yavaş nefes;
                // başın üstünden "Z"ler süzülüyor. Son yarım saniyede irkilip uyanıyor.
                if (e < dur - SLEEP_WAKE_S) {
                    target[Ch.EAR_L] = 30f
                    target[Ch.EAR_R] = 30f
                    target[Ch.HEAD_ROT] = 9f
                    target[Ch.HEAD_DY] = 4f
                    target[Ch.BODY_DY] = 2f
                    target[Ch.HANG_L] = -4f
                    target[Ch.HANG_R] = -4f
                    osc[Ch.BODY_DY] = 2.5f * sin(e * TAU / 2.2f) * env
                    eyes = Eyes.CLOSED
                } else {
                    target[Ch.EYE_SCALE] = 0.2f
                }
            }
            Emote.POINT -> {
                // Kol yana uzanıp hafifçe dürtüyor, gövde ve bakış o yana; ilk bir buçuk
                // saniye konuşuyor ("şuraya bas").
                val side = if (pointToLeft) -1f else 1f
                if (pointToLeft) {
                    target[Ch.HANG_L_A] = 0f
                    target[Ch.CAT_L_A] = 1f
                    target[Ch.CAT_L] = POINT_UP_L
                    osc[Ch.CAT_L] = 4f * sin(e * TAU * 2f) * env
                    target[Ch.HANG_R] = 6f
                } else {
                    target[Ch.HANG_R_A] = 0f
                    target[Ch.CAT_R_A] = 1f
                    target[Ch.CAT_R] = POINT_UP_R
                    osc[Ch.CAT_R] = 4f * sin(e * TAU * 2f) * env
                    target[Ch.HANG_L] = 6f
                }
                target[Ch.UPPER_ROT] = 5f * side
                target[Ch.HEAD_ROT] = 4f * side
                target[Ch.GAZE_X] = 2.5f * side
                if (e < 1.6f) mouth = Mouth.TALK
            }
            Emote.PEACE -> {
                // Özgün zafer işaretli kol, göz kırpma, küçük bir zıplama.
                target[Ch.HANG_R_A] = 0f
                target[Ch.PEACE_A] = 1f
                osc[Ch.PEACE] = 6f * sin(e * TAU * 1.5f) * env
                target[Ch.HEAD_ROT] = 6f
                hop(e, hops = 1, hopS = 0.35f, height = 10f)
                eyes = Eyes.WINK
                mouth = Mouth.HAPPY
            }
            Emote.SAD -> {
                // Kulaklar sarkık, kafa önde, kaşlar kalkık, ağız ters, gövde çökük; arada
                // bir iç çekiyor. Abartılmadı: yanlış cevapta çocuğu caydırmasın.
                target[Ch.EAR_L] = 35f
                target[Ch.EAR_R] = 35f
                target[Ch.HEAD_DY] = 4f
                target[Ch.HEAD_ROT] = 3f
                target[Ch.BODY_DY] = 2f
                target[Ch.GAZE_Y] = 2f
                target[Ch.BROWS] = 1f
                target[Ch.HANG_L] = -3f
                target[Ch.HANG_R] = -3f
                osc[Ch.BODY_DY] = 1.2f * sin(e * TAU / 1.6f) * env
                mouth = Mouth.SAD
            }
            Emote.DANCE -> {
                // Sağa-sola sekme: yanlarda yere basıp ortadan havada geçiyor; kollar
                // sırayla kalkıyor, kulaklar ve kafa ters yöne sallanıyor.
                bothCatArms()
                val w = e * TAU
                osc[Ch.CAT_L] = 25f * sin(w * 2f) * env
                osc[Ch.CAT_R] = -25f * sin(w * 2f) * env
                osc[Ch.BODY_DX] = 7f * sin(w) * env
                osc[Ch.LIFT] = 7f * abs(cos(w)) * env
                osc[Ch.UPPER_ROT] = 6f * sin(w) * env
                osc[Ch.HEAD_ROT] = -3f * sin(w) * env
                osc[Ch.EAR_L] = 10f * sin(w) * env
                osc[Ch.EAR_R] = -10f * sin(w) * env
                eyes = Eyes.HAPPY
                mouth = Mouth.HAPPY
            }
            Emote.THINK -> {
                // Sağ el çenede, parmaklarla tıklıyor; gözler yukarıda, başın yanında
                // düşünce balonu beliriyor.
                target[Ch.HANG_R_A] = 0f
                target[Ch.THINK_A] = 1f
                osc[Ch.THINK] = 3f * sin(e * TAU * 2.5f) * env
                target[Ch.GAZE_X] = 1.5f
                target[Ch.GAZE_Y] = -2f
                target[Ch.HEAD_ROT] = 6f
            }
            Emote.CLAP -> {
                // Köpeğin önde birleşik elleri açılıp kapanıyor; her vuruşta yıldızlar.
                frontHands()
                target[Ch.FRONT_DY] = 4f
                osc[Ch.FRONT_GAP] = 7f * (0.5f - 0.5f * cos(e * TAU * CLAP_HZ)) * env
                osc[Ch.BODY_DY] = -1.5f * abs(sin(e * TAU * CLAP_HZ / 2f)) * env
                earsWiggle(e, env, base = 0f)
                eyes = Eyes.HAPPY
                mouth = Mouth.HAPPY
            }
            Emote.SURPRISED -> {
                // Küçük bir sıçrama, eller havada, gözler büyük, ağız "O", kulaklar dimdik.
                bothCatArms()
                target[Ch.CAT_L] = 55f
                target[Ch.CAT_R] = 40f
                target[Ch.EYE_SCALE] = 0.35f
                target[Ch.EAR_L] = -10f
                target[Ch.EAR_R] = -10f
                osc[Ch.EAR_L] = 3f * sin(e * TAU * 8f) * env
                osc[Ch.EAR_R] = -3f * sin(e * TAU * 8f) * env
                target[Ch.HEAD_DY] = -3f
                hop(e, hops = 1, hopS = 0.35f, height = 12f)
                mouth = Mouth.O
            }
            Emote.SHY -> {
                // Eller önde birleşik ve kıpır kıpır, yanaklar kızarık, bakış yere ve yana,
                // kafa eğik, kulaklar biraz düşük.
                frontHands()
                target[Ch.FRONT_DY] = 6f
                osc[Ch.FRONT_ROT] = 3f * sin(e * TAU * 1.5f) * env
                target[Ch.BLUSH] = 1f
                target[Ch.GAZE_X] = -2f
                target[Ch.GAZE_Y] = 1.5f
                target[Ch.HEAD_ROT] = -7f
                target[Ch.HEAD_DY] = 2f
                target[Ch.EAR_L] = 12f
                target[Ch.EAR_R] = 12f
                osc[Ch.UPPER_ROT] = 2f * sin(e * TAU * 0.8f) * env
            }
            Emote.WINK -> {
                // Tek göz kırpma, kafa eğik, omuzlar hafif kalkık, gözün yanında parıltı.
                target[Ch.HEAD_ROT] = 8f
                target[Ch.UPPER_ROT] = 3f
                target[Ch.HANG_L] = 6f
                target[Ch.HANG_R] = 6f
                eyes = Eyes.WINK
                mouth = Mouth.HAPPY
            }
            Emote.HEART -> {
                // Eller göğüste, gözler gülüyor, yanaklar pembe; başın üstünde kalp atıyor.
                frontHands()
                target[Ch.FRONT_DY] = 4f
                target[Ch.BLUSH] = 0.7f
                target[Ch.HEAD_ROT] = 5f
                osc[Ch.UPPER_ROT] = 2f * sin(e * TAU * 0.9f) * env
                eyes = Eyes.HAPPY
            }
            Emote.ABACUS -> {
                // Önünde küçük bir soroban, elleri iki yanında; boncuklar sayılıyor, gözler
                // abaküste. Son anda başını kaldırıp gülümsüyor.
                frontHands()
                target[Ch.FRONT_GAP] = 22f
                target[Ch.FRONT_DY] = 10f
                target[Ch.ABACUS_A] = 1f
                if (e < dur - 0.8f) {
                    target[Ch.GAZE_Y] = 2.5f
                    target[Ch.HEAD_DY] = 2f
                    target[Ch.HEAD_ROT] = -3f
                } else {
                    eyes = Eyes.HAPPY
                    mouth = Mouth.HAPPY
                }
            }
        }
    }

    private fun bothCatArms() {
        target[Ch.HANG_L_A] = 0f
        target[Ch.HANG_R_A] = 0f
        target[Ch.CAT_L_A] = 1f
        target[Ch.CAT_R_A] = 1f
    }

    private fun frontHands() {
        target[Ch.HANG_L_A] = 0f
        target[Ch.HANG_R_A] = 0f
        target[Ch.FRONT_A] = 1f
    }

    private fun earsWiggle(e: Float, env: Float, base: Float) {
        target[Ch.EAR_L] = base
        target[Ch.EAR_R] = base
        osc[Ch.EAR_L] = 6f * sin(e * TAU * 5f) * env
        osc[Ch.EAR_R] = 6f * sin(e * TAU * 5f) * env
    }

    /**
     * [hops] sekmelik zıplama: yükseklik, yere değerken ezilme/havada uzama, inişten sonra
     * sönen yaylanma; kafa gövdeyi 40 ms geriden izliyor (gecikmeli yükseklikle, sekmeler
     * arasında kopmadan).
     */
    private fun hop(e: Float, hops: Int, hopS: Float, height: Float) {
        osc[Ch.LIFT] = hopLift(e, hops, hopS, height)
        osc[Ch.HEAD_DY] += hopLift(e, hops, hopS, height) - hopLift(e - HEAD_LAG_S, hops, hopS, height)
        val p = e / hopS
        osc[Ch.SQUASH] = if (p < hops) {
            val frac = p - floor(p)
            val squash = (1f - min(frac, 1f - frac) / 0.18f).coerceAtLeast(0f)
            squash - 0.5f * sin(PI.toFloat() * frac)
        } else {
            val after = e - hops * hopS
            exp(-after * 7f) * cos(after * TAU * 3f)
        }
    }

    private fun hopLift(e: Float, hops: Int, hopS: Float, height: Float): Float {
        val p = e / hopS
        if (p < 0f || p >= hops) return 0f
        return height * sin(PI.toFloat() * (p - floor(p)))
    }

    // ---------------------------------------------------------------- çizim

    private fun drawBunny(canvas: Canvas, now: Long, t: Float) {
        val e = (now - emoteStartMs) / 1000f
        val lift = v(Ch.LIFT)
        val bodyDx = v(Ch.BODY_DX)
        val bob = sin(t * TAU / 2.4f) * 2.2f
        val upperRot = v(Ch.UPPER_ROT)
        val headRot = sin(t * TAU / 5f) * 2.5f + v(Ch.HEAD_ROT)
        val squash = v(Ch.SQUASH)

        // Gölge: tavşan yükseldikçe küçülüyor — zıplamayı gölge satıyor.
        canvas.save()
        canvas.translate(bodyDx, 0f)
        val shadowScale = 1f - lift / (JUMP_HEIGHT * 2.5f)
        canvas.scale(shadowScale, shadowScale, SHADOW_X, SHADOW_Y)
        drawShapes(canvas, BunnyMascotArt.SHADOW)
        canvas.restore()

        canvas.save()
        canvas.translate(bodyDx, -lift)
        // Ezilme/uzama ayak tabanından.
        canvas.scale(1f + 0.07f * squash, 1f - 0.10f * squash, FEET_X, FEET_Y)

        // Arkadaki kollar gövdeyle birlikte iner-kalkar ve sallanır.
        canvas.save()
        canvas.translate(0f, bob + v(Ch.BODY_DY))
        canvas.rotate(upperRot, HIP_X, HIP_Y)
        drawBackArms(canvas, t)
        canvas.restore()

        // Bacaklar yerinde, gövde ve kafa iner-kalkar: nefes alıyor gibi.
        drawLegs(canvas)
        canvas.translate(0f, bob + v(Ch.BODY_DY))
        canvas.rotate(upperRot, HIP_X, HIP_Y)
        drawShapes(canvas, BunnyMascotArt.BODY)

        canvas.save()
        canvas.translate(0f, v(Ch.HEAD_DY))
        canvas.rotate(headRot, NECK_X, NECK_Y)
        drawEars(canvas, now)
        drawShapes(canvas, BunnyMascotArt.HEAD)
        drawBlush(canvas)
        drawEyes(canvas, now)
        drawMouth(canvas, t)
        drawShapes(canvas, BunnyMascotArt.HEAD_TOP)
        canvas.restore()

        // Öndekiler: kafanın altına binen eller kafadan SONRA (köpeğin özgün çizimindeki sıra).
        drawAbacus(canvas, e)
        drawFrontHands(canvas)
        drawThinkArm(canvas)
        drawExtras(canvas, e)

        canvas.restore()
    }

    private fun drawBackArms(canvas: Canvas, t: Float) {
        val sway = sin(t * TAU / 2.4f) * 1.5f

        // Sarkan kollar. Sağdaki, solun aynası: sola ve biraz aşağı kaydırılmış (gövde hafif
        // yandan çizilmiş; tam aynada omzun sivri ucu dışarıda kalıyordu).
        val hr = v(Ch.HANG_R_A)
        withAlpha(canvas, hr, ARM_LAYER) {
            canvas.translate(0f, MIRROR_DY)
            canvas.rotate(-((1f - hr) * 50f + v(Ch.HANG_R) + sway), SHOULDER_R_X, SHOULDER_Y)
            canvas.translate(MIRROR_X2, 0f)
            canvas.scale(-1f, 1f)
            drawShapes(canvas, BunnyMascotArt.ARM)
        }
        val hl = v(Ch.HANG_L_A)
        withAlpha(canvas, hl, ARM_LAYER) {
            canvas.rotate((1f - hl) * 50f + v(Ch.HANG_L) + sway, SHOULDER_L_X, SHOULDER_Y)
            drawShapes(canvas, BunnyMascotArt.ARM)
        }

        // Zafer işaretli kol (özgün çizim)
        val pa = v(Ch.PEACE_A)
        withAlpha(canvas, pa, ARM_LAYER) {
            canvas.rotate(v(Ch.PEACE) + (1f - pa) * 40f, PEACE_SHOULDER_X, PEACE_SHOULDER_Y)
            drawShapes(canvas, BunnyMascotArt.PEACE_ARM)
        }

        // Kedi kolları. Kedinin özgün kolları asimetrik (sol hafif aşağı, sağ hafif yukarı
        // bakıyor), açılar da o yüzden farklı.
        val cl = v(Ch.CAT_L_A)
        withAlpha(canvas, cl, ARM_LAYER) {
            canvas.rotate(v(Ch.CAT_L) - (1f - cl) * 50f, SHOULDER_L_X, CHEER_SHOULDER_L_Y)
            canvas.translate(CAT_DX, 0f)
            drawShapes(canvas, BunnyMascotArt.CHEER_ARM_LEFT)
        }
        val cr = v(Ch.CAT_R_A)
        val rightUp = v(Ch.CAT_R) - (1f - cr) * 50f
        // Balon sağ elde: kol dönüşü geri alınıp dik tutuluyor; ip elin altında kalsın diye
        // koldan önce çiziliyor.
        val ba = v(Ch.BALLOON_A)
        withAlpha(canvas, ba, FULL_LAYER) {
            canvas.rotate(-rightUp, CHEER_SHOULDER_R_X, CHEER_SHOULDER_R_Y)
            canvas.translate(CAT_DX, 0f)
            canvas.translate(CAT_HAND_X, CAT_HAND_Y)
            canvas.rotate(rightUp + v(Ch.BALLOON_SWAY))
            val grow = 0.3f + 0.7f * ba
            canvas.scale(grow, grow)
            canvas.translate(-BALLOON_STRING_X, -BALLOON_STRING_Y)
            drawShapes(canvas, BunnyMascotArt.BALLOON)
        }
        withAlpha(canvas, cr, ARM_LAYER) {
            canvas.rotate(-rightUp, CHEER_SHOULDER_R_X, CHEER_SHOULDER_R_Y)
            canvas.translate(CAT_DX, 0f)
            drawShapes(canvas, BunnyMascotArt.CHEER_ARM_RIGHT)
        }
    }

    private fun drawLegs(canvas: Canvas) {
        // Selamda iki ayak hafifçe sağa-sola açılıyor: her bacak kalçasından dışa dönüyor.
        val open = v(Ch.LEGS_OPEN)
        canvas.save()
        canvas.translate(-STEP_DX * open, 0f)
        canvas.rotate(STEP_DEG * open, LEG_L_HIP_X, LEG_HIP_Y)
        drawShapes(canvas, BunnyMascotArt.LEG_LEFT)
        canvas.restore()
        canvas.save()
        canvas.translate(STEP_DX * open, 0f)
        canvas.rotate(-STEP_DEG * open, LEG_R_HIP_X, LEG_HIP_Y)
        drawShapes(canvas, BunnyMascotArt.LEG_RIGHT)
        canvas.restore()
    }

    private fun drawEars(canvas: Canvas, now: Long) {
        var flick = 0f
        if (earFlickStartMs >= 0) {
            val p = ((now - earFlickStartMs) / EAR_FLICK_MS.toFloat()).coerceIn(0f, 1f)
            flick = 12f * sin(PI.toFloat() * p)
        }
        canvas.save()
        canvas.rotate(-(v(Ch.EAR_L) + if (earFlickSide < 0) flick else 0f), EAR_L_X, EAR_Y)
        drawShapes(canvas, BunnyMascotArt.EAR_LEFT)
        canvas.restore()
        canvas.save()
        canvas.rotate(v(Ch.EAR_R) + if (earFlickSide > 0) flick else 0f, EAR_R_X, EAR_Y)
        drawShapes(canvas, BunnyMascotArt.EAR_RIGHT)
        canvas.restore()
    }

    private fun drawBlush(canvas: Canvas) {
        val a = v(Ch.BLUSH)
        if (a <= 0.01f) return
        paint.style = Paint.Style.FILL
        paint.color = BLUSH
        paint.alpha = (paint.alpha * a.coerceAtMost(1f)).roundToInt()
        for (cx in BLUSH_X) {
            rect.set(cx - 8f, 158f, cx + 8f, 168f)
            canvas.drawOval(rect, paint)
        }
    }

    private fun drawEyes(canvas: Canvas, now: Long) {
        var open = 1f
        if (blinkStartMs >= 0) {
            val p = (now - blinkStartMs) / BLINK_MS.toFloat()
            if (p >= 1f) {
                blinkStartMs = -1L
                nextBlinkMs = now + Random.nextLong(1800, 4200)
            } else {
                open = 1f - 0.9f * sin(PI.toFloat() * p)
            }
        }
        val scale = 1f + v(Ch.EYE_SCALE)
        for (i in 0..1) {
            val cx = EYE_X[i]
            val mode = when (eyes) {
                Eyes.WINK -> if (i == 0) Eyes.HAPPY else Eyes.NORMAL
                else -> eyes
            }
            when (mode) {
                Eyes.HAPPY -> eyeArc(canvas, cx, 180f)   // ters "U"
                Eyes.CLOSED -> eyeArc(canvas, cx, 0f)    // "U": uykulu, kapalı
                else -> {
                    // Göz kırpmada açık kalan göz kırpmıyor; kırpışla karışmasın.
                    val o = if (eyes == Eyes.WINK) 1f else open
                    canvas.save()
                    canvas.translate(v(Ch.GAZE_X), v(Ch.GAZE_Y))
                    canvas.scale(scale, scale * o, cx, EYE_Y)
                    drawShape(canvas, BunnyMascotArt.EYES[i])
                    canvas.restore()
                }
            }
        }
        val brows = v(Ch.BROWS)
        if (brows > 0.01f) {
            // Üzgün kaşlar: iç uçları kalkık
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.4f
            paint.color = EYE
            paint.alpha = (255 * brows.coerceAtMost(1f)).roundToInt()
            canvas.drawLine(357f, 133f, 369f, 128f, paint)
            canvas.drawLine(402f, 128f, 414f, 133f, paint)
        }
    }

    private fun eyeArc(canvas: Canvas, cx: Float, startDeg: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.6f
        paint.color = EYE
        if (startDeg == 180f) rect.set(cx - 6f, 140f, cx + 6f, 152f) else rect.set(cx - 6f, 137f, cx + 6f, 149f)
        canvas.drawArc(rect, startDeg, 180f, false, paint)
    }

    private fun drawMouth(canvas: Canvas, t: Float) {
        when (mouth) {
            Mouth.HAPPY -> {
                // Kedinin açık, mutlu ağzı: burnun hemen altına taşınıp tavşanın küçük
                // burun çevresine sığsın diye biraz küçültülüyor.
                canvas.save()
                canvas.translate(MOUTH_X, HAPPY_MOUTH_TOP)
                canvas.scale(HAPPY_MOUTH_SCALE, HAPPY_MOUTH_SCALE)
                canvas.translate(-CAT_MOUTH_X, -CAT_MOUTH_TOP)
                drawShapes(canvas, BunnyMascotArt.HAPPY_MOUTH)
                canvas.restore()
            }
            Mouth.TALK -> {
                // İki farklı hızda sinüsün çarpımı: düzenli bir "aç-kapa" yerine hecelere
                // benzeyen düzensiz bir açılma.
                val open = ((0.5f + 0.5f * sin(t * TAU * 4.2f)) *
                    (0.6f + 0.4f * sin(t * TAU * 1.3f))).coerceIn(0f, 1f)
                val ry = 1.2f + 4.3f * open
                rect.set(MOUTH_X - 5f, MOUTH_Y - ry, MOUTH_X + 5f, MOUTH_Y + ry)
                mouthPath.reset()
                mouthPath.addOval(rect, Path.Direction.CW)
                drawOpenMouth(canvas, mouthPath, MOUTH_X, MOUTH_Y + ry, 3.5f, 2.5f)
            }
            Mouth.O -> {
                paint.style = Paint.Style.FILL
                paint.color = MOUTH
                rect.set(MOUTH_X - 4f, 165f, MOUTH_X + 4f, 175f)
                canvas.drawOval(rect, paint)
            }
            Mouth.SAD -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = MOUTH
                rect.set(MOUTH_X - 6.5f, 167f, MOUTH_X + 6.5f, 177f)
                canvas.drawArc(rect, 200f, 140f, false, paint)
            }
            Mouth.IDLE -> drawShapes(canvas, BunnyMascotArt.MOUTH)
        }
    }

    /** Ağız boşluğu ve dili; dil ağzın dışına taşmasın diye ağız şekline kırpılıyor. */
    private fun drawOpenMouth(canvas: Canvas, shape: Path, tongueX: Float, tongueY: Float, rx: Float, ry: Float) {
        paint.style = Paint.Style.FILL
        paint.color = MOUTH
        canvas.drawPath(shape, paint)
        canvas.save()
        canvas.clipPath(shape)
        paint.color = TONGUE
        rect.set(tongueX - rx, tongueY - ry, tongueX + rx, tongueY + ry)
        canvas.drawOval(rect, paint)
        canvas.restore()
    }

    /**
     * Köpeğin önde birleşik elleri. Kollar gövdeye gölge düşürüyor; eller iki yana
     * açıldıkça (abaküs) gölgeler gövdenin dışına taşıp koyu leke bırakıyordu, o yüzden
     * açıldıkça siliniyorlar.
     */
    private fun drawFrontHands(canvas: Canvas) {
        val a = v(Ch.FRONT_A)
        if (a <= 0.01f) return
        val gap = v(Ch.FRONT_GAP)
        val rot = v(Ch.FRONT_ROT)
        val dy = v(Ch.FRONT_DY) + (1f - a) * 25f   // gelirken aşağıdan
        val shadow = (1f - gap / 8f).coerceIn(0f, 1f)
        withAlpha(canvas, a, FULL_LAYER) {
            if (shadow > 0.01f) {
                frontSide(canvas, -1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_SHADOW_LEFT, shadow) }
                frontSide(canvas, 1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_SHADOW_RIGHT, shadow) }
            }
            frontSide(canvas, -1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_ARM_LEFT) }
            frontSide(canvas, 1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_ARM_RIGHT) }
        }
    }

    private inline fun frontSide(canvas: Canvas, side: Float, gap: Float, rot: Float, dy: Float, block: () -> Unit) {
        canvas.save()
        canvas.translate(side * gap, dy)
        canvas.rotate(-side * rot, if (side < 0) FRONT_SHOULDER_L_X else FRONT_SHOULDER_R_X, FRONT_SHOULDER_Y)
        canvas.translate(DOG_DX, DOG_DY)
        block()
        canvas.restore()
    }

    /** Düşünürken çeneye götürülen sağ kedi kolu; kafanın ÖNÜNDE. */
    private fun drawThinkArm(canvas: Canvas) {
        val a = v(Ch.THINK_A)
        withAlpha(canvas, a, FULL_LAYER) {
            canvas.rotate(-(v(Ch.THINK) - (1f - a) * 60f), CHEER_SHOULDER_R_X, CHEER_SHOULDER_R_Y)
            canvas.translate(CAT_DX, 0f)
            drawShapes(canvas, BunnyMascotArt.CHEER_ARM_RIGHT)
        }
    }

    /** Küçük bir soroban: ahşap çerçeve, 5 çubuk, kiriş; boncuklar sayılıyormuş gibi kayıyor. */
    private fun drawAbacus(canvas: Canvas, e: Float) {
        val a = v(Ch.ABACUS_A)
        withAlpha(canvas, a, FULL_LAYER) {
            val grow = 0.8f + 0.2f * a
            canvas.scale(grow, grow, 395f, 236f)
            paint.style = Paint.Style.FILL
            paint.color = WOOD
            rect.set(357f, 214f, 433f, 258f)
            canvas.drawRoundRect(rect, 6f, 6f, paint)
            paint.color = WOOD_LIGHT
            rect.set(362f, 219f, 428f, 253f)
            canvas.drawRoundRect(rect, 3f, 3f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.4f
            paint.color = WOOD_DARK
            for (x in ABACUS_ROD_X) canvas.drawLine(x, 219f, x, 253f, paint)
            paint.style = Paint.Style.FILL
            paint.color = WOOD
            rect.set(362f, 228f, 428f, 230.6f)
            canvas.drawRect(rect, paint)
            for (i in ABACUS_ROD_X.indices) {
                val x = ABACUS_ROD_X[i]
                paint.color = ABACUS_BEADS[i]
                // 0 = kirişten uzakta, 1 = kirişe dayalı (sayılmış)
                val heaven = 0.5f + 0.5f * sin(e * TAU * 0.5f + i * 2.1f)
                val earth = 0.5f + 0.5f * sin(e * TAU * 0.7f + i * 1.3f)
                drawBead(canvas, x, 222f + 3f * heaven)
                drawBead(canvas, x, 234f + 10f * (1f - earth))
                drawBead(canvas, x, 240f + 10f * (1f - earth))
            }
        }
    }

    /** Soroban boncuğu: iki ucu sivri, yanlardan bakınca altıgen. */
    private fun drawBead(canvas: Canvas, cx: Float, cy: Float) {
        beadPath.reset()
        beadPath.moveTo(cx - 5.5f, cy)
        beadPath.lineTo(cx - 2f, cy - 3f)
        beadPath.lineTo(cx + 2f, cy - 3f)
        beadPath.lineTo(cx + 5.5f, cy)
        beadPath.lineTo(cx + 2f, cy + 3f)
        beadPath.lineTo(cx - 2f, cy + 3f)
        beadPath.close()
        canvas.drawPath(beadPath, paint)
    }

    /** Hâle özgü süsler: Z'ler, düşünce balonu, alkış yıldızları, kalp, parıltı. */
    private fun drawExtras(canvas: Canvas, e: Float) {
        val em = emote ?: return
        val dur = em.durationMs / 1000f
        val fade = (e / 0.2f).coerceIn(0f, 1f) * ((dur - e) / 0.25f).coerceIn(0f, 1f)
        when (em) {
            Emote.SLEEP -> {
                if (e >= dur - SLEEP_WAKE_S) return
                // Üç "Z" sırayla başın yanından yukarı süzülüp büyüyerek soluyor.
                for (k in 0..2) {
                    val p = ((e / 1.8f) + k / 3f) % 1f
                    val alpha = sin(PI.toFloat() * p) * fade
                    drawText(canvas, "Z", 440f + 30f * p, 112f - 60f * p, 12f + 12f * p, LIGHT, alpha)
                }
            }
            Emote.THINK -> {
                // Düşünce balonu: iki küçük kabarcık, sonra içinde "?" olan balon.
                popCircle(canvas, 440f, 118f, 3f, e - 0.4f, fade, LIGHT)
                popCircle(canvas, 452f, 103f, 4.5f, e - 0.7f, fade, LIGHT)
                popCircle(canvas, 481f, 70f, 15f, e - 1.0f, fade, LIGHT)
                val q = popScale(e - 1.15f)
                if (q > 0f) drawText(canvas, "?", 481f, 77f, 20f * q, EYE, fade)
            }
            Emote.CLAP -> {
                // Her vuruşta (eller birleşince) üç küçük yıldız çıkıp büyüyerek soluyor.
                val phase = (e * CLAP_HZ) % 1f
                val alpha = (1f - phase * 2.5f).coerceIn(0f, 1f) * fade
                if (alpha > 0f) {
                    val s = 2.5f + 4f * phase
                    val dy = v(Ch.FRONT_DY)
                    drawStar(canvas, 368f, 190f + dy, s, alpha)
                    drawStar(canvas, 408f, 188f + dy, s, alpha)
                    drawStar(canvas, 389f, 178f + dy, s * 0.8f, alpha)
                }
            }
            Emote.HEART -> {
                // Kalp kulakların arasında belirip hafif yükseliyor ve atıyor.
                val s = popScale(e - 0.25f) * (1f + 0.08f * sin(e * TAU * 1.8f))
                if (s > 0f) drawHeart(canvas, 389f, 76f - 4f * e, 15f * s, fade)
            }
            Emote.WINK -> {
                // Kırpılan gözün yanında bir parıltı
                val s = popScale(e - 0.15f)
                if (s > 0f) drawStar(canvas, 347f, 128f, 5f * s, fade)
            }
            else -> Unit
        }
    }

    /** Hafif taşan bir beliriş: 0 → ~1,2 → 1 (0,3 sn). Henüz zamanı gelmediyse 0. */
    private fun popScale(e: Float): Float {
        if (e <= 0f) return 0f
        val p = (e / 0.3f).coerceAtMost(1f)
        return p + 0.25f * sin(PI.toFloat() * p)
    }

    private fun popCircle(canvas: Canvas, cx: Float, cy: Float, r: Float, e: Float, alpha: Float, color: Int) {
        val s = popScale(e)
        if (s <= 0f) return
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        canvas.drawCircle(cx, cy, r * s, paint)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, alpha: Float) {
        textPaint.color = color
        textPaint.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        textPaint.textSize = size
        canvas.drawText(text, x, y, textPaint)
    }

    /** Dört köşeli parıltı yıldızı. */
    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Float) {
        mouthPath.reset()
        mouthPath.moveTo(cx, cy - r)
        mouthPath.quadTo(cx + 0.15f * r, cy - 0.15f * r, cx + r, cy)
        mouthPath.quadTo(cx + 0.15f * r, cy + 0.15f * r, cx, cy + r)
        mouthPath.quadTo(cx - 0.15f * r, cy + 0.15f * r, cx - r, cy)
        mouthPath.quadTo(cx - 0.15f * r, cy - 0.15f * r, cx, cy - r)
        mouthPath.close()
        paint.style = Paint.Style.FILL
        paint.color = STAR
        paint.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        canvas.drawPath(mouthPath, paint)
    }

    /** Kalp: iki daire + aşağı bakan üçgen ([r] yarı genişlik). */
    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Float) {
        mouthPath.reset()
        mouthPath.addCircle(cx - 0.5f * r, cy - 0.25f * r, 0.52f * r, Path.Direction.CW)
        mouthPath.addCircle(cx + 0.5f * r, cy - 0.25f * r, 0.52f * r, Path.Direction.CW)
        mouthPath.moveTo(cx - r, cy - 0.1f * r)
        mouthPath.lineTo(cx + r, cy - 0.1f * r)
        mouthPath.lineTo(cx, cy + r)
        mouthPath.close()
        paint.style = Paint.Style.FILL
        paint.color = TONGUE
        paint.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        canvas.drawPath(mouthPath, paint)
    }

    private fun drawShapes(canvas: Canvas, shapes: List<BunnyMascotArt.Shape>, alpha: Float = 1f) {
        for (s in shapes) drawShape(canvas, s, alpha)
    }

    private fun drawShape(canvas: Canvas, s: BunnyMascotArt.Shape, alpha: Float = 1f) {
        if (s.strokeWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = s.strokeWidth
        } else {
            paint.style = Paint.Style.FILL
        }
        paint.color = s.color
        if (alpha < 1f) paint.alpha = (paint.alpha * alpha).roundToInt()
        canvas.drawPath(s.path, paint)
    }

    /**
     * [block]'u [alpha] saydamlığında çizer; dönüşümler blok bitince geri alınır. Kısmi
     * saydamlıkta katman (saveLayerAlpha) açılıyor: parçanın şekilleri üst üste biniyor, tek
     * tek saydamlaştırılsalar arkadaki şekiller görünürdü. Tam görünür/görünmezde katman yok.
     */
    private inline fun withAlpha(canvas: Canvas, alpha: Float, bounds: RectF, block: () -> Unit) {
        if (alpha <= 0.01f) return
        val save = if (alpha >= 0.99f) canvas.save()
        else canvas.saveLayerAlpha(bounds, (alpha * 255).roundToInt())
        block()
        canvas.restoreToCount(save)
    }

    private companion object {
        const val TAU = (2 * PI).toFloat()

        // Görünür alan: kaynak çizimde CENTER_X etrafında UNITS genişliğinde, TOP_Y'den başlayan kare.
        const val UNITS = 314f
        const val CENTER_X = 393f
        const val TOP_Y = 6f

        const val SHADOW_X = 401f
        const val SHADOW_Y = 305f
        const val JUMP_HEIGHT = 22f
        const val CHEER_HOP_S = 0.45f
        const val HEAD_LAG_S = 0.04f

        // Sarkan kollar. Sağdaki, sol kolun aynası: x → MIRROR_X2 - x (eksen x=394,5), sonra
        // MIRROR_DY aşağı. Değerler önizlemede denenerek seçildi.
        const val SHOULDER_L_X = 372f
        const val SHOULDER_Y = 222f
        const val MIRROR_X2 = 789f
        const val MIRROR_DY = 2f
        const val SHOULDER_R_X = MIRROR_X2 - SHOULDER_L_X
        const val PEACE_SHOULDER_X = 420f
        const val PEACE_SHOULDER_Y = 228f
        const val PEACE_REST = -10f
        // Katman sınırları (tavşan koordinatında)
        val ARM_LAYER = RectF(290f, 120f, 510f, 300f)
        val FULL_LAYER = RectF(230f, -10f, 560f, 330f)

        // Kedi kolları kedinin yerinde duruyor; kedinin gövdesi tavşanınkiyle hemen hemen
        // aynı genişlikte, o yüzden yatayda kaydırmak omuzlara oturtmaya yetiyor.
        const val CAT_DX = -213f
        const val CHEER_SHOULDER_L_Y = 228f
        const val CHEER_SHOULDER_R_X = 418f
        const val CHEER_SHOULDER_R_Y = 225f
        const val CHEER_UP_L = 32f   // sevinçte sol kolun kalkışı
        const val CHEER_UP_R = 18f   // sağ kol zaten hafif yukarı çizilmiş
        const val WAVE_UP = 32f      // selamda sağ kol (±12° sallanıyor)
        const val POINT_UP_R = -8f   // işaret ederken sağ kol yatay
        const val POINT_UP_L = 8f    // sol kol aşağı bakık çizilmiş; yataya kaldırılıyor
        const val THINK_UP = 105f    // kol yukarı-içe dönüp eli çeneye getiriyor
        // Kedinin sağ el ucu (kedi koordinatı) ve domuz balonunun ipinin elde kalan ucu
        const val CAT_HAND_X = 681f
        const val CAT_HAND_Y = 220f
        const val BALLOON_STRING_X = 456.5f
        const val BALLOON_STRING_Y = 628.1f
        // Köpeğin önde birleşik elleri: köpeğin gövdesi tavşanınkine yakın, kaydırmak yetiyor.
        const val DOG_DX = -233f
        const val DOG_DY = -407f
        const val FRONT_SHOULDER_L_X = 355f
        const val FRONT_SHOULDER_R_X = 427f
        const val FRONT_SHOULDER_Y = 198f
        const val CLAP_HZ = 2.5f

        // Kedinin ağzı (üst orta noktası) → tavşanın burnunun altı, %85 boyut
        const val CAT_MOUTH_X = 601.1f
        const val CAT_MOUTH_TOP = 165.4f
        const val HAPPY_MOUTH_TOP = 162f
        const val HAPPY_MOUTH_SCALE = 0.85f
        const val MOUTH_X = 386.1f
        const val MOUTH_Y = 168f

        const val NECK_X = 390f
        const val NECK_Y = 212f
        // Gövde sallanması kalçadan, ezilip uzama ayak tabanından.
        const val HIP_X = 395f
        const val HIP_Y = 262f
        const val FEET_X = 397f
        const val FEET_Y = 310f
        // Selamda ayakların sağa-sola açılması (kalça eklemlerinden)
        const val LEG_L_HIP_X = 383f
        const val LEG_R_HIP_X = 414f
        const val LEG_HIP_Y = 258f
        const val STEP_DX = 1f
        const val STEP_DEG = 5f

        const val EAR_L_X = 352f
        const val EAR_R_X = 425f
        const val EAR_Y = 125f

        const val EYE_Y = 143.3f
        val EYE_X = floatArrayOf(364.6f, 406.5f)
        val BLUSH_X = floatArrayOf(352f, 420f)

        const val BLINK_MS = 140L
        const val EAR_FLICK_MS = 300L
        const val SLEEP_WAKE_S = 0.5f

        val ABACUS_ROD_X = floatArrayOf(369f, 382f, 395f, 408f, 421f)

        // Renkler kaynak çizimin paletinden.
        const val EYE = 0xFF0C2C3C.toInt()
        const val MOUTH = 0xFF430231.toInt()
        const val TONGUE = 0xFFF281AA.toInt()
        const val BLUSH = 0x8CF281AA.toInt()
        const val LIGHT = 0xFFD8F4FF.toInt()
        const val STAR = 0xFFFFDD00.toInt()
        const val WOOD = 0xFFB8743F.toInt()
        const val WOOD_LIGHT = 0xFFF7E6CC.toInt()
        const val WOOD_DARK = 0xFF9C6B3F.toInt()
        val ABACUS_BEADS = intArrayOf(
            0xFFF35720.toInt(), 0xFFFFDD00.toInt(), 0xFF8FA316.toInt(), 0xFFF281AA.toInt(), 0xFF5FBFFE.toInt(),
        )
    }
}
