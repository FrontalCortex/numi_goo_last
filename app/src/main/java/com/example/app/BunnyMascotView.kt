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
import androidx.core.content.ContextCompat
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

    /**
     * Oynatılabilen hâller ve süreleri. [loops] olan hâl süresi dolunca bitmiyor, başka bir
     * hâl oynatılana kadar baştan dönüyor (bir ekranın sürekli canlandırması için).
     */
    enum class Emote(val durationMs: Long, val label: String, val loops: Boolean = false) {
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
        CHAT(6000, "Mesajlaşma", loops = true),
        FLAME(6000, "Alev (seri)", loops = true),
        CALENDAR(5500, "Takvim (seri)", loops = true),
        JETPACK(4000, "Jetpack (Pro)", loops = true),
        HERO(4000, "Süper kahraman (Pro)", loops = true),
        CROWN(3600, "Taç (Pro)", loops = true),
        AD_JUMP(3200, "Reklamı atla", loops = true),
        AD_BUTTON(4400, "Atla düğmesi", loops = true),
        AD_PUSH(3600, "Reklamı itme", loops = true),
        AD_ZAP(5400, "Değnek + yıldırım", loops = true),
        AD_MAGIC(5600, "Değnek + yasak işareti", loops = true),
        BOARD(5000, "Kara tahta", loops = true),
        LISTEN(4400, "Dinleyen tavşan", loops = true),
        NOTEBOOK(4600, "Havuç kalem", loops = true),
        NOTEBOOK_HOLD(3000, "Havuç kalem (bekleme)", loops = true),
        NOTEBOOK_WRITE(2300, "Havuç kalem (yazma)"),
        ALARM(4500, "Çalar saat (hatırlatma)", loops = true),
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
        const val PHONE_A = 34
        const val FLAME_A = 35      // elde tutulan seri alevi
        const val FLAME_H = 36      // alevin yüksekliği (seçilen güne göre büyüyor)
        const val CAL_A = 37        // seri takvimi
        const val JET_A = 38        // sırttaki jet çantası
        const val CAPE_A = 39       // pelerin + göğüsteki PRO rozeti
        const val CROWN_A = 40      // taç
        const val FRONT_L_X = 41    // önde ellerin tek tek kayması (defter tutma, yazma)
        const val FRONT_R_X = 42
        const val FRONT_R_Y = 43
        const val BOARD_A = 44      // kara tahta
        const val NOTE_A = 45       // defter + havuç kalem
        const val GLASSES_A = 46    // havalı gözlük
        const val WAND_A = 47       // sihirli değnek (sağ elde)
        const val ALARM_A = 48      // önde tutulan çalar saat
        const val COUNT = 49
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
    private val bubblePath = Path()
    private val tailPath = Path()
    // mutate(): alpha bu kopyaya yazılsın, ikonu kullanan başka ekranlar etkilenmesin.
    private val heartDrawable = ContextCompat.getDrawable(context, R.drawable.heart_ic)?.mutate()?.apply {
        setBounds(0, 0, HEART_BOUNDS.toInt(), HEART_BOUNDS.toInt())
    }
    private val billboardDrawable = ContextCompat.getDrawable(context, R.drawable.billboard)?.mutate()?.apply {
        setBounds(0, 0, 512, 512)
    }
    private val carrotDrawable = ContextCompat.getDrawable(context, R.drawable.carrot_ic)?.mutate()?.apply {
        setBounds(0, 0, 512, 512)
    }
    private val glassesDrawable = ContextCompat.getDrawable(context, R.drawable.cool_glasses)?.mutate()?.apply {
        setBounds(0, 0, GLASSES_W.toInt(), GLASSES_H.toInt())
    }
    private val flameDrawable = ContextCompat.getDrawable(context, R.drawable.streak_flame_ic)?.mutate()?.apply {
        setBounds(0, 0, HEART_BOUNDS.toInt(), HEART_BOUNDS.toInt())
    }
    // Göğüsteki PRO rozeti: Pro ekranındaki rozetle (bg_shop_super_badge) aynı renk geçişi.
    // Rozet gövdenin içinde sabit yerde, geçiş bir kez kuruluyor.
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(
            380f, 0f, 410f, 0f,
            intArrayOf(0xFF1DF0A4.toInt(), 0xFF3A82FF.toInt(), 0xFFD558FF.toInt()),
            null,
            android.graphics.Shader.TileMode.CLAMP,
        )
    }
    // Alevin halesi: birim yarıçaplı radyal geçiş, boyu tuvalin ölçeğiyle veriliyor (her
    // karede yeni gölgelendirici yaratmamak için).
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.RadialGradient(
            0f, 0f, 1f,
            intArrayOf(0x73FFB300, 0x00FFB300),
            null,
            android.graphics.Shader.TileMode.CLAMP,
        )
    }
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
    // Yukarıdan düşen taç ve gözlüğün o anki dikey kayması (Atla düğmesi hâli; yoksa 0).
    private var crownDrop = 0f
    private var glassesDrop = 0f
    private var glassesSparkleT = -1f   // gözlük inişinden bu yana (sn); parıltı için

    private var loopIndex = 0          // döngülü hâlin kaçıncı turu (takvimin deneme sırası)
    private var repeatsLeft = 0        // [play]'in times'ından kalan tekrar
    private var nextEmote: Emote? = null   // [play]'in then'i: bitince geçilecek hâl
    private var streakChangedMs = 0L

    /**
     * Seri hâllerinin ([Emote.FLAME], [Emote.CALENDAR]) gün sayısı:
     *  - -1: kendi kendine 3 → 5 → 7 dönüyor (deneme ekranı; seçim yapan biri yok),
     *  - 0: henüz seçim yok (küçük alev, boş takvim),
     *  - 3/5/7: seçilen gün; değişince alev büyüyor ve tavşan seviniyor, takvim baştan
     *    işaretleniyor.
     */
    var streakDays: Int = -1
        set(value) {
            if (field == value) return
            field = value
            streakChangedMs = SystemClock.uptimeMillis()
            if (emote == Emote.CALENDAR) emoteStartMs = streakChangedMs
            invalidate()
        }

    /**
     * Çalar saat hâlinin ([Emote.ALARM]) kadranındaki saat (0–23, tam saat):
     *  - -1: kendi kendine 16 → 18 → 19 → 20 dönüyor (deneme ekranı),
     *  - diğer: seçilen hatırlatma saati; değişince akrep ve yelkovan yeni saate dönüyor
     *    (yelkovan aradaki her saat için bir tur atıyor) ve saat hemen çalıyor.
     */
    var alarmHour: Int = -1
        set(value) {
            if (field == value) return
            field = value
            alarmChangedMs = SystemClock.uptimeMillis()
            invalidate()
        }
    private var alarmChangedMs = 0L
    /** Akrebin o anki açısı (derece, 12 = 0); hedefe yaylı yaklaşıyor, bkz. [updateAlarmHands]. */
    private var alarmAngle = 0f

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

    /** [autoGreetIntervalMs] dolunca kendiliğinden oynayan hâl; varsayılan selam. */
    private var autoEmote = Emote.GREET

    /**
     * [emote]'u hemen oynatır, sonra her bitişinde [pauseMs] bekleyip yeniden oynatır (ör. kayıt
     * tanışmasında 2 sn arayla el sallama). Selamın kendiliğinden tekrarıyla aynı düzenek, yalnızca
     * hâl ve aralık farklı; araya giren bir [play] sayacı baştan başlatıyor. Durdurmak: [stop].
     */
    fun repeatWithPause(emote: Emote, pauseMs: Long) {
        autoEmote = emote
        autoGreetIntervalMs = emote.durationMs + pauseMs
        play(emote)
    }

    /** Oynayan hâli ve kendiliğinden tekrarı bırakır; tavşan yumuşakça bekleme duruşuna döner. */
    fun stop() {
        emote = null
        nextEmote = null
        repeatsLeft = 0
        talkUntilMs = 0L
        autoEmote = Emote.GREET
        autoGreetIntervalMs = 0L
        invalidate()
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        cur[Ch.HANG_L_A] = 1f
        cur[Ch.HANG_R_A] = 1f
        cur[Ch.CAT_L] = CHEER_UP_L
        cur[Ch.CAT_R] = CHEER_UP_R
        cur[Ch.PEACE] = PEACE_REST
        cur[Ch.THINK] = THINK_UP
        cur[Ch.FLAME_H] = flameHeight(0)
    }

    /**
     * [emote]'u baştan oynatır; o an başka bir hâl oynuyorsa onu keser. [times] kez üst üste
     * oynuyor (döngülü hâllerde anlamsız); tekrarlar arasında beklemeye dönülmüyor. [then]
     * verilirse hâl bitince beklemeye değil ona geçiliyor (ör. yazma bitince defterle bekleme).
     */
    fun play(emote: Emote, times: Int = 1, then: Emote? = null) {
        val now = SystemClock.uptimeMillis()
        this.emote = emote
        emoteStartMs = now
        repeatsLeft = times.coerceAtLeast(1) - 1
        nextEmote = then
        loopIndex = 0
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
            if (playing.loops) {
                // Görünmezken geçen süre birden çok tura denk gelebilir; hepsi atlanıyor.
                val turns = (now - emoteStartMs) / playing.durationMs
                emoteStartMs += turns * playing.durationMs
                loopIndex += turns.toInt()
            } else if (repeatsLeft > 0) {
                repeatsLeft--
                emoteStartMs = now
            } else {
                val next = nextEmote
                emote = null
                onEmoteFinished?.invoke(playing)
                if (next != null && emote == null) play(next)
            }
        }
        if (emote == null && now >= nextAutoGreetMs) play(autoEmote)

        setIdlePose()
        emote?.let { applyEmote(it, (now - emoteStartMs) / 1000f, now) }
        if (mouth == Mouth.IDLE && now < talkUntilMs) mouth = Mouth.TALK

        // Üstel yumuşatma: kare hızından bağımsız, hedefe hızla yaklaşıp yavaşlayarak oturuyor.
        val k = 1f - exp(-dt * 14f)
        for (i in 0 until Ch.COUNT) cur[i] += (target[i] - cur[i]) * k
        updateAlarmHands(dt)
    }

    /**
     * Çalar saatin akrebi. Kanallardan ayrı ve daha yavaş yumuşatılıyor: kanal hızında (0,2 sn)
     * dönüş fark edilmiyordu, oysa saatin seçime "kurulması" bu hâlin asıl gösterisi.
     * Hâl dışındayken açı korunuyor; yeniden açılınca 12'den değil kaldığı yerden dönüyor.
     */
    private fun updateAlarmHands(dt: Float) {
        if (emote != Emote.ALARM) return
        val target = (displayedAlarmHour() % 12) * 30f
        alarmAngle += (target - alarmAngle) * (1f - exp(-dt * ALARM_HAND_SPEED))
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
        target[Ch.FLAME_H] = cur[Ch.FLAME_H]   // alev sönerken boyu değişmesin
        target[Ch.GAZE_X] = idleGaze
        crownDrop = 0f
        glassesDrop = 0f
        glassesSparkleT = -1f
        eyes = Eyes.NORMAL
        mouth = Mouth.IDLE
    }

    /** Değer = yumuşatılmış hedef + doğrudan salınım. */
    private fun v(ch: Int) = cur[ch] + osc[ch]

    private fun applyEmote(em: Emote, e: Float, now: Long) {
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
            Emote.CHAT -> {
                // Telefonda öğretmenle yazışma döngüsü: yazıyor → soru balonu telefondan
                // çıkıyor → öğretmenin balonu ("yazıyor..." sonra video ▶) → seviniyor →
                // balonlar sönüyor, yeniden yazmaya başlıyor. Balonlar [drawExtras]'ta.
                frontHands()
                target[Ch.FRONT_DY] = 6f
                target[Ch.PHONE_A] = 1f
                when {
                    e < CHAT_WAIT_S || e >= CHAT_FADE_S -> {
                        // Yazıyor: telefona bakıyor, parmaklar kıpır kıpır.
                        target[Ch.GAZE_Y] = 2.5f
                        target[Ch.HEAD_DY] = 2f
                        target[Ch.HEAD_ROT] = -3f
                        osc[Ch.FRONT_ROT] = 2.5f * sin(e * TAU * 6f) * env
                    }
                    e < CHAT_ANSWER_S -> {
                        // Cevabı bekliyor: öğretmenin balonuna bakıyor.
                        target[Ch.GAZE_X] = 2f
                        target[Ch.GAZE_Y] = -1f
                        target[Ch.HEAD_ROT] = 4f
                    }
                    else -> {
                        // Cevap geldi: seviniyor.
                        target[Ch.GAZE_X] = 1.5f
                        target[Ch.GAZE_Y] = -1f
                        target[Ch.HEAD_ROT] = 4f
                        hop(e - CHAT_ANSWER_S - 0.1f, hops = 1, hopS = 0.35f, height = 9f)
                        val k = ((e - CHAT_ANSWER_S) / 0.15f).coerceIn(0f, 1f) *
                            ((CHAT_FADE_S - e) / 0.2f).coerceIn(0f, 1f)
                        earsWiggle(e, k, base = 6f)
                        eyes = Eyes.HAPPY
                        mouth = Mouth.HAPPY
                    }
                }
            }
            Emote.FLAME -> {
                // Serinin alevi sağ elde, meşale gibi; tavşan ona bakıyor. Seçilen gün
                // arttıkça alev büyüyor, her büyümede tavşan zıplayıp seviniyor.
                target[Ch.HANG_R_A] = 0f
                target[Ch.CAT_R_A] = 1f
                target[Ch.CAT_R] = 25f
                target[Ch.FLAME_A] = 1f
                target[Ch.FLAME_H] = flameHeight(flameDays(e))
                target[Ch.GAZE_X] = 2f
                target[Ch.GAZE_Y] = -1.5f
                target[Ch.HEAD_ROT] = 4f
                val age = flameReactAge(e, now)
                if (age < FLAME_REACT_S) {
                    hop(age, hops = 1, hopS = 0.35f, height = 8f)
                    earsWiggle(e, 1f - age / FLAME_REACT_S, base = 6f)
                    eyes = Eyes.HAPPY
                    mouth = Mouth.HAPPY
                }
            }
            Emote.ALARM -> {
                // Önünde iki çanlı çalar saat, patiler yanlardan tutuyor; kadranda seçilen
                // hatırlatma saati. Bekleme kısmında saate bakıyor; her turda bir kez (ve saat
                // değişince hemen) saat çalıyor: titriyor, çekiç çanlara vuruyor, tavşan küçük
                // bir zıplamayla gülüyor. Çizim [drawAlarmClock].
                frontHands()
                target[Ch.FRONT_GAP] = 14f
                target[Ch.FRONT_DY] = 14f
                target[Ch.ALARM_A] = 1f
                val age = alarmRingAge(e, now)
                val ring = alarmRingEnv(age)
                if (ring > 0.01f) {
                    hop(age, hops = 1, hopS = 0.3f, height = 7f)
                    earsWiggle(e, ring, base = 8f)
                    osc[Ch.FRONT_ROT] = 2.5f * sin(e * TAU * ALARM_SHAKE_HZ) * ring
                    eyes = Eyes.HAPPY
                    mouth = Mouth.HAPPY
                } else {
                    target[Ch.GAZE_Y] = 2.5f
                    target[Ch.HEAD_DY] = 2f
                    target[Ch.HEAD_ROT] = -3f
                }
            }
            Emote.JETPACK -> {
                // Sırtında jet çantası, havada süzülüyor: kollar uçar gibi açık, ayaklar
                // boşlukta sallanıyor, kulaklar rüzgârda. Çanta ve egzoz [drawJetpack].
                bothCatArms()
                target[Ch.CAT_L] = 20f
                target[Ch.CAT_R] = 10f
                osc[Ch.CAT_L] = 5f * sin(e * TAU * 2f) * env
                osc[Ch.CAT_R] = 5f * sin(e * TAU * 2f) * env
                target[Ch.LIFT] = 14f
                osc[Ch.LIFT] = 4f * sin(e * TAU / 1.6f) * env
                target[Ch.LEGS_OPEN] = 0.6f
                osc[Ch.LEGS_OPEN] = 0.4f * sin(e * TAU * 1.25f) * env
                target[Ch.EAR_L] = 8f
                target[Ch.EAR_R] = 8f
                osc[Ch.EAR_L] = 5f * sin(e * TAU * 3f) * env
                osc[Ch.EAR_R] = 5f * sin(e * TAU * 3f + 1f) * env
                osc[Ch.UPPER_ROT] = 3f * sin(e * TAU / 1.6f + 1f) * env
                target[Ch.JET_A] = 1f
                mouth = Mouth.HAPPY
            }
            Emote.HERO -> {
                // Süper kahraman: arkada dalgalanan pelerin, göğüste PRO rozeti, sağ kol
                // havada, sol kol kalçada, geniş duruş. Pelerin [drawCape], rozet [drawProBadge].
                target[Ch.HANG_R_A] = 0f
                target[Ch.CAT_R_A] = 1f
                target[Ch.CAT_R] = 45f
                osc[Ch.CAT_R] = 4f * sin(e * TAU * 1.5f) * env
                target[Ch.HANG_L] = 12f
                target[Ch.LEGS_OPEN] = 1f
                target[Ch.CAPE_A] = 1f
                target[Ch.HEAD_ROT] = -3f
                target[Ch.EAR_L] = 4f
                target[Ch.EAR_R] = 4f
                osc[Ch.EAR_L] = 4f * sin(e * TAU * 2.5f) * env
                osc[Ch.EAR_R] = 4f * sin(e * TAU * 2.5f + 0.8f) * env
                mouth = Mouth.HAPPY
            }
            Emote.CROWN -> {
                // Başında yan yatık altın taç, zafer işareti ve göz kırpma; çenesi hafif
                // havada. Taç ve parıltıları [drawCrown].
                target[Ch.HANG_R_A] = 0f
                target[Ch.PEACE_A] = 1f
                osc[Ch.PEACE] = 5f * sin(e * TAU * 1.2f) * env
                target[Ch.HEAD_ROT] = -4f
                target[Ch.GAZE_Y] = -1f
                target[Ch.CROWN_A] = 1f
                eyes = Eyes.WINK
                mouth = Mouth.HAPPY
            }
            Emote.AD_JUMP -> {
                // "Reklamları atla": sağdan kayarak gelen REKLAM yazılı TV'nin üstünden
                // zıplıyor. Önce ona bakıp çömeliyor, havada kolları açık, inince TV'yi
                // gözüyle uğurlayıp seviniyor. TV [drawAdProps]'ta, zamanlaması AD_JUMP_* sabitleri.
                val tvX = adTvX(e)
                when {
                    e < AD_JUMP_HOP_S -> {
                        target[Ch.GAZE_X] = ((tvX - 395f) / 60f).coerceIn(-2.5f, 2.5f)
                        if (e > AD_JUMP_HOP_S - 0.15f) target[Ch.BODY_DY] = 3f   // hazırlanma
                    }
                    e < AD_JUMP_HOP_S + AD_JUMP_HOP_LEN_S + 0.2f -> {
                        bothCatArms()
                        target[Ch.CAT_L] = 40f
                        target[Ch.CAT_R] = 30f
                        target[Ch.EAR_L] = -6f
                        target[Ch.EAR_R] = -6f
                        target[Ch.EYE_SCALE] = 0.15f
                        target[Ch.GAZE_Y] = 2f
                        target[Ch.GAZE_X] = ((tvX - 395f) / 60f).coerceIn(-2.5f, 2.5f)
                        mouth = Mouth.HAPPY
                    }
                    e < AD_JUMP_CHEER_S -> {
                        target[Ch.GAZE_X] = ((tvX - 395f) / 60f).coerceIn(-2.5f, 2.5f)
                    }
                    e < AD_JUMP_CHEER_S + 0.8f -> {
                        bothCatArms()
                        val flap = 6f * sin(e * TAU * 3f)
                        osc[Ch.CAT_L] = flap
                        osc[Ch.CAT_R] = flap
                        val k = ((e - AD_JUMP_CHEER_S) / 0.15f).coerceIn(0f, 1f) *
                            ((AD_JUMP_CHEER_S + 0.8f - e) / 0.2f).coerceIn(0f, 1f)
                        earsWiggle(e, k, base = 10f)
                        eyes = Eyes.HAPPY
                        mouth = Mouth.HAPPY
                    }
                }
                hop(e - AD_JUMP_HOP_S, hops = 1, hopS = AD_JUMP_HOP_LEN_S, height = AD_JUMP_HEIGHT)
            }
            Emote.AD_BUTTON -> {
                // "Atla" düğmesi: sağında video oynatıcılardaki gibi bir düğme beliriyor,
                // patisini hazırlayıp basıyor; düğme gömülüp parlıyor ve kayıp gidiyor.
                // Ardından yukarıdan önce taç, sonra havalı gözlük düşüyor (her inişte kafa
                // hafifçe sarsılıyor) ve tavşan kollarını kavuşturup arkaya yaslanarak
                // havalı bir poz veriyor. Düğme [drawAdProps]'ta, taç [drawCrown], gözlük
                // [drawGlasses].
                when {
                    e < AD_BTN_READY_S -> target[Ch.GAZE_X] = 2.5f
                    e < AD_BTN_PRESS_S + 0.35f -> {
                        target[Ch.GAZE_X] = 2.5f
                        target[Ch.HANG_R_A] = 0f
                        target[Ch.CAT_R_A] = 1f
                        // Önce hazırda (yukarıda), basma anında yataya iniyor.
                        target[Ch.CAT_R] = if (e < AD_BTN_PRESS_S) 20f else -5f
                        if (e < AD_BTN_PRESS_S) osc[Ch.CAT_R] = 3f * sin(e * TAU * 3f) * env
                        target[Ch.UPPER_ROT] = 4f
                    }
                }
                coolFinale(e, crownS = AD_BTN_CROWN_S, fadeS = AD_BTN_FADE_S)
            }
            Emote.AD_ZAP, Emote.AD_MAGIC -> {
                // Reklam billboardını sihirli değnekle yok ediyor: "abra kadabra" diye
                // değneği daire çizerek sallıyor (ağzı büyü sözü söylüyor), sonra
                //  - AD_ZAP: billboarda yıldırım düşüyor, kararıp kül oluyor;
                //  - AD_MAGIC: parıltılar uçuyor, üstüne yasak işareti mühürleniyor ve
                //    yıldız tozuna dönüşüp kayboluyor.
                // Ardından "Atla düğmesi"ndeki gibi taç + gözlük ve havalı poz. Billboard ve
                // efektler [drawBillboardScene]'de, değnek [drawBackArms]'ta.
                val zap = em == Emote.AD_ZAP
                val hitS = if (zap) ZAP_STRIKE_S else MAGIC_STAMP_S
                val crownS = if (zap) ZAP_CROWN_S else MAGIC_CROWN_S
                when {
                    e < WAND_UP_S -> target[Ch.GAZE_X] = 2.5f
                    e < hitS + 0.5f -> {
                        target[Ch.GAZE_X] = 2.5f
                        target[Ch.HANG_R_A] = 0f
                        target[Ch.CAT_R_A] = 1f
                        target[Ch.WAND_A] = 1f
                        if (e < WAND_WAVE_END_S) {
                            // Değnek daire çiziyor: kol açısı ve gövde birlikte salınıyor.
                            target[Ch.CAT_R] = WAND_ARM_UP
                            osc[Ch.CAT_R] = wandWave(e)
                            osc[Ch.UPPER_ROT] = 2f * sin((e - WAND_UP_S) * TAU * WAND_HZ + 1f)
                            if (e > WAND_UP_S + 0.15f) mouth = Mouth.TALK   // "abra kadabra"
                        } else {
                            // Değneği billboarda doğrultuyor.
                            target[Ch.CAT_R] = WAND_POINT_UP
                        }
                        // Vuruş anında gözler büyüyor.
                        if (e >= hitS && e < hitS + 0.4f) target[Ch.EYE_SCALE] = 0.25f
                    }
                }
                coolFinale(e, crownS = crownS, fadeS = if (zap) ZAP_FADE_S else MAGIC_FADE_S)
            }
            Emote.AD_PUSH -> {
                // REKLAM tabelasını yaslanarak itip ekrandan çıkarıyor (gözler sıkılı,
                // "hıhh"), sonra ellerini çırpıp seviniyor. Tabela [drawAdProps]'ta.
                when {
                    e < AD_PUSH_START_S -> {
                        target[Ch.GAZE_X] = 2f
                        if (e > 0.4f) {
                            target[Ch.HANG_R_A] = 0f
                            target[Ch.CAT_R_A] = 1f
                            target[Ch.CAT_R] = POINT_UP_R
                        }
                    }
                    e < AD_PUSH_END_S -> {
                        target[Ch.HANG_R_A] = 0f
                        target[Ch.CAT_R_A] = 1f
                        target[Ch.CAT_R] = -12f
                        target[Ch.UPPER_ROT] = 8f
                        target[Ch.BODY_DX] = 8f
                        target[Ch.LEGS_OPEN] = 1f
                        target[Ch.HEAD_ROT] = 5f
                        osc[Ch.UPPER_ROT] = 1.5f * sin(e * TAU * 6f) * env   // zorlanma titremesi
                        eyes = Eyes.HAPPY
                        mouth = Mouth.O
                    }
                    e < AD_PUSH_END_S + 0.8f -> {
                        // Elleri çırpıyor: "hallettim".
                        frontHands()
                        target[Ch.FRONT_DY] = 4f
                        osc[Ch.FRONT_GAP] = 6f * (0.5f - 0.5f * cos((e - AD_PUSH_END_S) * TAU * CLAP_HZ)) * env
                        eyes = Eyes.HAPPY
                        mouth = Mouth.HAPPY
                    }
                }
            }
            Emote.BOARD -> {
                // Kara tahta: yanındaki şövaleli tahtaya tebeşirle cevabı yazıyor (deneme
                // ekranında örnek cevaplar sırayla), altını çizip başını sallıyor, seviniyor;
                // sonra yazı siliniyor. Tahta [drawAdProps]'ta, tebeşir elde [drawBackArms].
                target[Ch.BODY_DX] = -14f
                target[Ch.HANG_R_A] = 0f
                target[Ch.CAT_R_A] = 1f
                target[Ch.CAT_R] = -2f
                target[Ch.BOARD_A] = 1f
                when {
                    e < BOARD_WRITE_S -> osc[Ch.CAT_R] = 4f * sin(e * TAU * 2.5f) * env   // tebeşiri tıklatıyor
                    e < BOARD_DONE_S + 0.4f -> {
                        target[Ch.GAZE_X] = 2.5f
                        target[Ch.GAZE_Y] = 0.5f
                        osc[Ch.CAT_R] = 3f * sin(e * TAU * 6f) * env   // yazma hareketi
                        if (e >= BOARD_DONE_S) {
                            osc[Ch.HEAD_DY] = 2.5f * sin(PI.toFloat() * ((e - BOARD_DONE_S) / 0.4f))
                        }
                    }
                    e < BOARD_ERASE_S -> {
                        hop(e - BOARD_DONE_S - 0.5f, hops = 1, hopS = 0.35f, height = 8f)
                        eyes = Eyes.HAPPY
                        mouth = Mouth.HAPPY
                    }
                }
            }
            Emote.LISTEN -> {
                // Dinleyen tavşan: eli yüzünün yanında, kocaman kulağını cevaba doğru eğmiş.
                // Cevap "gelince" kulak dikiliyor, iki kez başını sallıyor, ✓ balonu çıkıyor.
                when {
                    e < LISTEN_ANSWER_S -> {
                        target[Ch.HANG_R_A] = 0f
                        target[Ch.THINK_A] = 1f
                        target[Ch.THINK] = LISTEN_HAND_UP
                        target[Ch.EAR_R] = 26f
                        osc[Ch.EAR_R] = 3f * sin(e * TAU * 2f) * env
                        target[Ch.HEAD_ROT] = 10f
                        target[Ch.GAZE_X] = 2f
                        target[Ch.GAZE_Y] = -0.5f
                        osc[Ch.UPPER_ROT] = 1.5f * sin(e * TAU * 0.8f) * env
                    }
                    e < LISTEN_ANSWER_S + 1.6f -> {
                        val a = e - LISTEN_ANSWER_S
                        if (a < 0.4f) {
                            target[Ch.HANG_R_A] = 0f
                            target[Ch.THINK_A] = 1f
                            target[Ch.THINK] = LISTEN_HAND_UP
                        }
                        target[Ch.EAR_R] = -8f
                        target[Ch.EAR_L] = -4f
                        if (a < 0.8f) osc[Ch.HEAD_DY] = 2.5f * abs(sin(a * TAU * 1.25f))   // iki kez baş sallama
                        if (a > 0.2f) {
                            eyes = Eyes.HAPPY
                            mouth = Mouth.HAPPY
                        }
                    }
                }
            }
            Emote.NOTEBOOK -> {
                // Havuç kalem (deneme ekranı): iki satır yazıyor (gözler defterde), sonra başını
                // kaldırıp seviniyor; döngü. Defter ve kalem [drawNotebook]/[drawCarrot].
                notebookHands(notebookPenTip(e))
                if (e < NOTE_LINE2_END_S + 0.1f) {
                    notebookLookDown()
                } else if (e < NOTE_ERASE_S) {
                    target[Ch.GAZE_Y] = -0.5f
                    hop(e - NOTE_LINE2_END_S - 0.2f, hops = 1, hopS = 0.35f, height = 7f)
                    eyes = Eyes.HAPPY
                    mouth = Mouth.HAPPY
                }
            }
            Emote.NOTEBOOK_HOLD -> {
                // Kayıt sorularında cevap beklerken: defter ve havuç elde, kalem satırın
                // başında havada, sana bakıp gülümsüyor; kalem arada hafifçe kıpırdıyor.
                notebookHands(notebookPenTip(0f))
                osc[Ch.FRONT_R_Y] = 1.5f * sin(e * TAU * 0.8f) * env
                target[Ch.GAZE_Y] = 0.5f
            }
            Emote.NOTEBOOK_WRITE -> {
                // "Devam Et"e basılınca: cevabı iki satır halinde deftere yazıp başını sallıyor
                // (bitince çağıran genellikle NOTEBOOK_HOLD'a döndürüyor, bkz. play'in then'i).
                val w = e + NOTE_WRITE_OFFSET_S
                notebookHands(notebookPenTip(w))
                if (w < NOTE_LINE2_END_S + 0.05f) {
                    notebookLookDown()
                } else {
                    osc[Ch.HEAD_DY] = 2f * sin(PI.toFloat() * ((w - NOTE_LINE2_END_S) / 0.35f).coerceIn(0f, 1f))
                    mouth = Mouth.HAPPY
                }
            }
            Emote.CALENDAR -> {
                // Önünde seri takvimi, elleri iki yanında. Kutular tek tek işaretleniyor
                // (her işarette küçük bir baş sallama); hepsi dolunca köşede alev beliriyor
                // ve tavşan seviniyor; sonra işaretler silinip baştan. Çizim [drawCalendar].
                frontHands()
                target[Ch.FRONT_GAP] = 28f
                target[Ch.FRONT_DY] = 10f
                target[Ch.CAL_A] = 1f
                val n = calendarDays()
                val done = calendarDoneS(n)
                if (n <= 0 || e < done || e >= CAL_RESET_S) {
                    target[Ch.GAZE_Y] = 2.5f
                    target[Ch.HEAD_DY] = 2f
                    target[Ch.HEAD_ROT] = -3f
                    val since = e - CAL_FIRST_TICK_S
                    if (n > 0 && since >= 0f && e < done) {
                        osc[Ch.HEAD_DY] = 1.5f * sin(PI.toFloat() * ((since % CAL_TICK_S) / CAL_TICK_S))
                    }
                } else {
                    target[Ch.GAZE_Y] = -1f
                    hop(e - done, hops = 1, hopS = 0.35f, height = 9f)
                    val k = ((e - done) / 0.15f).coerceIn(0f, 1f) * ((CAL_RESET_S - e) / 0.2f).coerceIn(0f, 1f)
                    earsWiggle(e, k, base = 6f)
                    eyes = Eyes.HAPPY
                    mouth = Mouth.HAPPY
                }
            }
        }
    }

    /**
     * Havuç kalem hâllerinin ortak duruşu: sol el defteri tutuyor, sağ el kalemin ucu [tip]'te
     * olacak yerde (el, kalemin ucundan [NOTE_PEN_HAND_DX], [NOTE_PEN_HAND_DY] uzakta).
     */
    private fun notebookHands(tip: FloatArray) {
        frontHands()
        target[Ch.FRONT_DY] = 0f
        target[Ch.FRONT_L_X] = -14f
        target[Ch.NOTE_A] = 1f
        target[Ch.FRONT_R_X] = tip[0] + NOTE_PEN_HAND_DX - FRONT_R_PAW_X
        target[Ch.FRONT_R_Y] = tip[1] + NOTE_PEN_HAND_DY - FRONT_R_PAW_Y
    }

    private fun notebookLookDown() {
        target[Ch.GAZE_Y] = 2.5f
        target[Ch.HEAD_DY] = 2f
        target[Ch.HEAD_ROT] = -3f
    }

    private val penTip = FloatArray(2)

    /**
     * Havuç kalemin ucunun olması gereken yer (tur içinde): birinci satır, ikinci satıra
     * geçiş (kalem kalkarak), ikinci satır, sonra yukarıda bekleme. El bu noktayı izliyor.
     */
    private fun notebookPenTip(e: Float): FloatArray {
        val x0 = NOTE_TEXT_X
        when {
            e < NOTE_LINE1_S -> { penTip[0] = x0; penTip[1] = NOTE_LINES[0] - 4f }
            e < NOTE_LINE1_END_S -> {
                val q = (e - NOTE_LINE1_S) / (NOTE_LINE1_END_S - NOTE_LINE1_S)
                penTip[0] = x0 + NOTE_LINE1_W * q; penTip[1] = NOTE_LINES[0]
            }
            e < NOTE_LINE2_S -> {
                val q = (e - NOTE_LINE1_END_S) / (NOTE_LINE2_S - NOTE_LINE1_END_S)
                penTip[0] = x0 + NOTE_LINE1_W * (1f - q)
                penTip[1] = NOTE_LINES[0] + (NOTE_LINES[1] - NOTE_LINES[0]) * q - 4f * sin(PI.toFloat() * q)
            }
            e < NOTE_LINE2_END_S -> {
                val q = (e - NOTE_LINE2_S) / (NOTE_LINE2_END_S - NOTE_LINE2_S)
                penTip[0] = x0 + NOTE_LINE2_W * q; penTip[1] = NOTE_LINES[1]
            }
            else -> { penTip[0] = x0 + 26f; penTip[1] = NOTE_LINES[0] - 4f }
        }
        return penTip
    }

    private val wandTipPt = FloatArray(2)

    /**
     * Değnek ucunun gövde koordinatındaki yeri, kol açısı [up] iken (kolun çizimiyle aynı
     * dönüşüm: el → değnek ucu, sonra omuz etrafında −[up] derece).
     */
    private fun wandTip(up: Float): FloatArray {
        val rad = Math.toRadians(WAND_DIR_DEG.toDouble())
        val lx = CAT_HAND_X + CAT_DX + cos(rad).toFloat() * WAND_LEN
        val ly = CAT_HAND_Y + sin(rad).toFloat() * WAND_LEN
        val a = Math.toRadians(-up.toDouble())
        val dx = lx - CHEER_SHOULDER_R_X
        val dy = ly - CHEER_SHOULDER_R_Y
        wandTipPt[0] = CHEER_SHOULDER_R_X + (dx * cos(a) - dy * sin(a)).toFloat()
        wandTipPt[1] = CHEER_SHOULDER_R_Y + (dx * sin(a) + dy * cos(a)).toFloat()
        return wandTipPt
    }

    /** "Abra kadabra" sırasında değneğin ucunun arkasında sönen yıldız izi. */
    private fun drawWandTrail(canvas: Canvas, t: Float) {
        val em = emote
        if (em != Emote.AD_ZAP && em != Emote.AD_MAGIC) return
        val e = (SystemClock.uptimeMillis() - emoteStartMs) / 1000f
        if (e < WAND_UP_S + 0.15f || e > WAND_WAVE_END_S) return
        for (k in 1..6) {
            val past = e - k * 0.05f
            if (past < WAND_UP_S) break
            val tip = wandTip(WAND_ARM_UP + wandWave(past))
            val fade = 1f - k / 7f
            drawStar(canvas, tip[0], tip[1], 3.5f * fade, fade * 0.9f)
        }
    }

    /**
     * Reklam hâllerinin ortak finali: yukarıdan önce taç ([crownS]), 0,25 sn sonra havalı
     * gözlük hızlanarak düşüyor (inişte sekme + kafa sarsılması); tavşan önce yukarı bakıyor,
     * gözlük inince kollarını kavuşturup arkaya yaslanarak havalı poz veriyor; [fadeS]'te
     * taç ve gözlük sönüyor.
     */
    private fun coolFinale(e: Float, crownS: Float, fadeS: Float) {
        val glassesS = crownS + FINALE_GLASSES_DELAY_S
        val poseS = glassesS + AD_DROP_S + 0.1f
        if (e >= crownS && e < poseS) {
            target[Ch.GAZE_Y] = -2.5f
            target[Ch.HEAD_ROT] = -3f
        } else if (e >= poseS && e < fadeS) {
            frontHands()
            target[Ch.FRONT_DY] = 2f
            target[Ch.UPPER_ROT] = -4f
            target[Ch.HEAD_ROT] = -7f
            target[Ch.LEGS_OPEN] = 1f
            osc[Ch.HEAD_ROT] = 1.5f * sin((e - poseS) * TAU * 0.7f)
        }
        if (e >= crownS && e < fadeS) target[Ch.CROWN_A] = 1f
        if (e >= glassesS && e < fadeS) target[Ch.GLASSES_A] = 1f
        crownDrop = dropOffset(e - crownS)
        glassesDrop = dropOffset(e - glassesS)
        glassesSparkleT = e - glassesS - AD_DROP_S - 0.1f
        osc[Ch.HEAD_DY] += landBump(e - crownS - AD_DROP_S) + landBump(e - glassesS - AD_DROP_S)
    }

    /** Değneğin "abra kadabra" salınımı: kol açısına eklenen sinüs (daire hissi için iki frekans). */
    private fun wandWave(e: Float): Float {
        val t = e - WAND_UP_S
        return 14f * sin(t * TAU * WAND_HZ) + 4f * sin(t * TAU * WAND_HZ * 2f)
    }

    /**
     * Yukarıdan düşen nesnenin dikey kayması (negatif = yukarıda): [t] < 0 iken henüz
     * düşmedi (en yukarıda), [AD_DROP_S] içinde hızlanarak iniyor, sonra küçük sönen sekmeler.
     */
    private fun dropOffset(t: Float): Float {
        if (t < 0f) return -AD_DROP_HEIGHT
        if (t < AD_DROP_S) {
            val p = t / AD_DROP_S
            return -AD_DROP_HEIGHT * (1f - p * p)
        }
        val q = t - AD_DROP_S
        return -5f * abs(sin(q * TAU * 2f)) * exp(-q * 6f)
    }

    /** İnişte kafanın kısa süre aşağı sarsılması (iniş anından [t] sn sonra). */
    private fun landBump(t: Float): Float =
        if (t < 0f || t > 0.3f) 0f else 2.5f * sin(PI.toFloat() * t / 0.3f)

    /** "Reklamı atla"da TV'nin yatay konumu (sağdan girip sola çıkıyor). */
    private fun adTvX(e: Float) = AD_TV_START_X - AD_TV_SPEED * (e - AD_TV_ENTER_S)

    /** Alevin gün sayısı; deneme modunda (−1) tur içinde 2'şer saniyede 3 → 5 → 7. */
    private fun flameDays(e: Float): Int =
        if (streakDays >= 0) streakDays else DEMO_DAYS[(e / FLAME_STEP_S).toInt().coerceIn(0, 2)]

    /** Alevin son büyümesinden bu yana geçen süre (sn); tepki (zıplama, gülümseme) buna göre. */
    private fun flameReactAge(e: Float, now: Long): Float = when {
        // Deneme modunda tur başa dönünce alev 7'den 3'e küçülüyor; orada sevinmesin.
        streakDays < 0 && e < FLAME_STEP_S && loopIndex > 0 -> Float.MAX_VALUE
        streakDays < 0 -> e % FLAME_STEP_S
        streakChangedMs == 0L -> Float.MAX_VALUE
        else -> (now - streakChangedMs) / 1000f
    }

    /** Kadranda gösterilen saat; deneme modunda (−1) her turda sıradaki. */
    private fun displayedAlarmHour(): Int =
        if (alarmHour >= 0) alarmHour else DEMO_ALARM_HOURS[loopIndex % DEMO_ALARM_HOURS.size]

    /**
     * Saatin bu çalışının başından beri geçen süre (sn); çalmıyorsa negatif. Saat yeni
     * değiştiyse o çalış önce geliyor (seçime anında tepki), yoksa turun içindeki düzenli çalış.
     */
    private fun alarmRingAge(e: Float, now: Long): Float {
        if (alarmChangedMs != 0L && alarmHour >= 0) {
            val react = (now - alarmChangedMs) / 1000f
            if (react <= ALARM_RING_S) return react
            // Tepkinin hemen ardından turun düzenli çalışı başlamasın: ekran açılırken ikisi
            // birleşip 2,4 sn kesintisiz çalıyordu.
            if (react < ALARM_RING_S + ALARM_QUIET_AFTER_S) return -1f
        }
        return e - ALARM_RING_AT_S
    }

    /** Çalmanın şiddeti 0..1: hızla başlıyor, sonunda kısa bir sönme. */
    private fun alarmRingEnv(age: Float): Float =
        if (age < 0f || age > ALARM_RING_S) 0f
        else (age / 0.08f).coerceAtMost(1f) * ((ALARM_RING_S - age) / 0.15f).coerceAtMost(1f)

    private fun flameHeight(days: Int) = when {
        days >= 7 -> 46f
        days >= 5 -> 38f
        days >= 3 -> 30f
        else -> 24f
    }

    /** Takvimin gün sayısı; deneme modunda (−1) her turda sıradaki (3 → 5 → 7). */
    private fun calendarDays(): Int = if (streakDays >= 0) streakDays else DEMO_DAYS[loopIndex % DEMO_DAYS.size]

    /** Son kutunun işaretlenip kutlamanın başladığı an (tur içinde, sn). */
    private fun calendarDoneS(n: Int) = CAL_FIRST_TICK_S + (n - 1).coerceAtLeast(0) * CAL_TICK_S + 0.3f

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
        if (e < 0f) return   // zıplama henüz başlamadı (gecikmeli çağrılar)
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

        // Reklam hâllerinin sahnesi (TV, düğme, tabela): yerde duruyor, tavşanla birlikte
        // zıplamıyor; tavşandan ÖNCE çiziliyor ki pati ve ayaklar önde kalsın.
        drawAdProps(canvas, e)
        drawBoard(canvas, e)

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
        drawProBadge(canvas)

        canvas.save()
        canvas.translate(0f, v(Ch.HEAD_DY))
        canvas.rotate(headRot, NECK_X, NECK_Y)
        drawEars(canvas, now)
        drawShapes(canvas, BunnyMascotArt.HEAD)
        drawBlush(canvas)
        drawEyes(canvas, now)
        drawMouth(canvas, t)
        drawShapes(canvas, BunnyMascotArt.HEAD_TOP)
        drawCrown(canvas, e)
        drawGlasses(canvas, e)
        canvas.restore()

        // Öndekiler: kafanın altına binen eller kafadan SONRA (köpeğin özgün çizimindeki sıra).
        drawAbacus(canvas, e)
        drawCalendar(canvas, e)
        if (v(Ch.NOTE_A) > 0.01f) {
            // Defteri arkasından görüyoruz: yazan sağ el ve havuç kalem defterin ARKASINDA
            // (yalnızca üstleri görünüyor), defteri tutan sol el önde. Havuç elin önünde:
            // elin arkasında kalınca turuncu gövdesi hiç görünmüyordu.
            drawFrontHands(canvas, left = false, right = true)
            drawCarrot(canvas)
            drawNotebook(canvas)
            drawFrontHands(canvas, left = true, right = false)
        } else {
            drawFrontHands(canvas)
        }
        // Çalar saat patilerin ÖNÜNDE: arkada kalınca sağ pati kadranı kapatıyordu.
        drawAlarmClock(canvas, e, now)
        drawPhone(canvas)
        drawThinkArm(canvas)
        drawTorchFlame(canvas, t)
        drawExtras(canvas, e)

        canvas.restore()
    }

    private fun drawBackArms(canvas: Canvas, t: Float) {
        val sway = sin(t * TAU / 2.4f) * 1.5f

        // Sırttakiler kollardan da geride: pelerin ve jet çantası.
        drawCape(canvas, t)
        drawJetpack(canvas, t)

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
            // Kara tahtada elde tebeşir.
            val chalk = v(Ch.BOARD_A)
            if (chalk > 0.01f) {
                canvas.save()
                canvas.rotate(30f, CAT_HAND_X, CAT_HAND_Y)
                paint.style = Paint.Style.FILL
                paint.color = WHITE
                paint.alpha = (255 * chalk.coerceAtMost(1f)).roundToInt()
                rect.set(CAT_HAND_X - 1.5f, CAT_HAND_Y - 7f, CAT_HAND_X + 1.5f, CAT_HAND_Y + 2f)
                canvas.drawRoundRect(rect, 1.2f, 1.2f, paint)
                canvas.restore()
            }
            // Sihirli değnek: elden yukarı-sağa koyu çubuk, beyaz uç, ucunda dönen yıldız.
            val wand = v(Ch.WAND_A)
            if (wand > 0.01f) {
                val rad = Math.toRadians(WAND_DIR_DEG.toDouble())
                val tx = CAT_HAND_X + cos(rad).toFloat() * WAND_LEN
                val ty = CAT_HAND_Y + sin(rad).toFloat() * WAND_LEN
                val wx = CAT_HAND_X + cos(rad).toFloat() * (WAND_LEN - 5f)
                val wy = CAT_HAND_Y + sin(rad).toFloat() * (WAND_LEN - 5f)
                val alpha = (255 * wand.coerceAtMost(1f)).roundToInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3.2f
                paint.color = WAND_STICK
                paint.alpha = alpha
                canvas.drawLine(CAT_HAND_X, CAT_HAND_Y, wx, wy, paint)
                paint.color = WHITE
                paint.alpha = alpha
                canvas.drawLine(wx, wy, tx, ty, paint)
                canvas.save()
                canvas.rotate(t * 180f, tx, ty)
                drawStar(canvas, tx, ty, 6.5f, wand.coerceAtMost(1f))
                canvas.restore()
            }
        }
        drawWandTrail(canvas, t)
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
     *
     * [left]/[right]: ellerin yalnızca biri çizilebiliyor — havuç kalemde yazan sağ el
     * defterin arkasında, tutan sol el önünde kalsın diye ikisi ayrı sırada çiziliyor.
     */
    private fun drawFrontHands(canvas: Canvas, left: Boolean = true, right: Boolean = true) {
        val a = v(Ch.FRONT_A)
        if (a <= 0.01f) return
        val gap = v(Ch.FRONT_GAP)
        val rot = v(Ch.FRONT_ROT)
        val dy = v(Ch.FRONT_DY) + (1f - a) * 25f   // gelirken aşağıdan
        // Eller ayrıldıkça/kaydıkça gölgeleri gövdenin dışına taşıyor; o yüzden siliniyor.
        val spread = gap + abs(v(Ch.FRONT_L_X)) + abs(v(Ch.FRONT_R_X)) + abs(v(Ch.FRONT_R_Y))
        val shadow = (1f - spread / 8f).coerceIn(0f, 1f)
        withAlpha(canvas, a, FULL_LAYER) {
            if (shadow > 0.01f) {
                if (left) frontSide(canvas, -1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_SHADOW_LEFT, shadow) }
                if (right) frontSide(canvas, 1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_SHADOW_RIGHT, shadow) }
            }
            if (left) frontSide(canvas, -1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_ARM_LEFT) }
            if (right) frontSide(canvas, 1f, gap, rot, dy) { drawShapes(canvas, BunnyMascotArt.FRONT_ARM_RIGHT) }
        }
    }

    private inline fun frontSide(canvas: Canvas, side: Float, gap: Float, rot: Float, dy: Float, block: () -> Unit) {
        canvas.save()
        // Ellerin tek tek kayması (defteri tutan sol el, yazan sağ el)
        if (side < 0) canvas.translate(v(Ch.FRONT_L_X), 0f)
        else canvas.translate(v(Ch.FRONT_R_X), v(Ch.FRONT_R_Y))
        canvas.translate(side * gap, dy)
        canvas.rotate(-side * rot, if (side < 0) FRONT_SHOULDER_L_X else FRONT_SHOULDER_R_X, FRONT_SHOULDER_Y)
        canvas.translate(DOG_DX, DOG_DY)
        block()
        canvas.restore()
    }

    /**
     * Mesajlaşmada ellerin arasındaki telefon. Ekran tavşana dönük, yani biz ARKASINI
     * görüyoruz: kenarlı kılıf, sol üstte kamera adası (iki lens + flaş), ortada logo.
     * Ellerle birlikte iner-kalkar ve kıpırdar (tasarım FRONT_DY = 8'de yapıldı).
     */
    private fun drawPhone(canvas: Canvas) {
        val a = v(Ch.PHONE_A)
        withAlpha(canvas, a, FULL_LAYER) {
            canvas.translate(0f, v(Ch.FRONT_DY) - 10f + (1f - a) * 25f)
            canvas.rotate(v(Ch.FRONT_ROT) * 0.4f, 395f, 222f)
            paint.style = Paint.Style.FILL
            paint.color = PHONE_EDGE
            rect.set(378f, 196f, 412f, 246f)
            canvas.drawRoundRect(rect, 7f, 7f, paint)
            paint.color = PHONE_BACK
            rect.set(380f, 198f, 410f, 244f)
            canvas.drawRoundRect(rect, 5.5f, 5.5f, paint)
            paint.color = PHONE_BODY
            rect.set(382f, 200.5f, 394f, 216.5f)
            canvas.drawRoundRect(rect, 4f, 4f, paint)
            for (ly in PHONE_LENS_Y) {
                paint.style = Paint.Style.FILL
                paint.color = EYE
                canvas.drawCircle(388f, ly, 2.6f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                paint.color = BUBBLE_EDGE
                canvas.drawCircle(388f, ly, 2.6f, paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = STAR
            canvas.drawCircle(391.8f, 208.5f, 1f, paint)
            paint.color = BUBBLE_EDGE
            canvas.drawCircle(395f, 228f, 4f, paint)
        }
    }

    /**
     * Konuşma balonu: hap biçimli gövde + alt köşede kuyruk, tek yol olarak birleştirilip
     * ([Path.op]) dolduruluyor ve kenarlanıyor — ayrı çizilse kuyrukla gövde arasında kenar
     * çizgisi kalırdı. [s] ölçek, merkez ([cx], [cy]).
     */
    private fun drawBubble(
        canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, s: Float,
        fill: Int, edge: Int, tailRight: Boolean, alpha: Float,
    ) {
        if (s <= 0f || alpha <= 0f) return
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(s, s)
        bubblePath.reset()
        rect.set(-w / 2f, -h / 2f, w / 2f, h / 2f)
        bubblePath.addRoundRect(rect, h / 2f, h / 2f, Path.Direction.CW)
        val tx = if (tailRight) w * 0.22f else -w * 0.22f
        tailPath.reset()
        tailPath.moveTo(tx - 6f, h / 2f - 2f)
        tailPath.lineTo(tx + if (tailRight) 10f else -10f, h / 2f + 9f)
        tailPath.lineTo(tx + 6f, h / 2f - 2f)
        tailPath.close()
        bubblePath.op(tailPath, Path.Op.UNION)
        val a = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        paint.style = Paint.Style.FILL
        paint.color = fill
        paint.alpha = a
        canvas.drawPath(bubblePath, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f
        paint.color = edge
        paint.alpha = a
        canvas.drawPath(bubblePath, paint)
        canvas.restore()
    }

    /** Mesajlaşmanın balonları; zaman çizelgesi [applyEmote]'taki CHAT ile aynı. */
    private fun drawChatBubbles(canvas: Canvas, e: Float) {
        // CHAT_FADE_S'den sonra 0,4 sn'de hafifçe yükselerek sönüyorlar.
        val outP = ((e - CHAT_FADE_S) / 0.4f).coerceIn(0f, 1f)
        val alpha = 1f - outP
        if (alpha <= 0f) return
        val rise = 8f * outP

        // Tavşanın sorusu: telefondan çıkıp sola yükseliyor.
        if (e >= CHAT_ASK_S) {
            val p = ((e - CHAT_ASK_S) / 0.45f).coerceIn(0f, 1f)
            val ease = 1f - (1f - p) * (1f - p)
            val x = 395f + (ASK_BUBBLE_X - 395f) * ease
            val y = 196f + (ASK_BUBBLE_Y - 196f) * ease - rise
            val s = 0.3f + 0.7f * ease
            drawBubble(canvas, x, y, 44f, 32f, s, WHITE, BUBBLE_EDGE, tailRight = true, alpha = alpha)
            drawText(canvas, "?", x, y + 8f * s, 22f * s, EYE, alpha)
        }

        // Öğretmenin cevabı: önce zıplayan "yazıyor" noktaları, sonra video ▶.
        if (e >= CHAT_REPLY_S) {
            val s = popScale(e - CHAT_REPLY_S)
            val x = REPLY_BUBBLE_X
            val y = REPLY_BUBBLE_Y - rise
            drawBubble(canvas, x, y, 48f, 32f, s, STAR, STAR_EDGE, tailRight = false, alpha = alpha)
            paint.style = Paint.Style.FILL
            paint.color = EYE
            paint.alpha = (255 * alpha).roundToInt()
            if (e < CHAT_ANSWER_S) {
                for (i in 0..2) {
                    val bounce = 3f * sin(e * TAU * 2f - i * 0.8f).coerceAtLeast(0f)
                    canvas.drawCircle(x + (i - 1) * 9f * s, y - bounce * s, 3f * s, paint)
                }
            } else {
                val q = s * popScale(e - CHAT_ANSWER_S)
                tailPath.reset()
                tailPath.moveTo(x - 5f * q, y - 8f * q)
                tailPath.lineTo(x + 9f * q, y)
                tailPath.lineTo(x - 5f * q, y + 8f * q)
                tailPath.close()
                canvas.drawPath(tailPath, paint)
            }
        }
    }

    /**
     * Sağ elde tutulan seri alevi. El kolun ucunda; kol arkada ama alev kafanın ÖNÜNDE
     * çizilmeli (kafanın sağ kenarına biniyor), o yüzden kolun dönüşü burada tekrarlanıyor
     * (balondaki gibi) ve alev dik tutuluyor.
     */
    private fun drawTorchFlame(canvas: Canvas, t: Float) {
        val a = v(Ch.FLAME_A)
        if (a <= 0.01f) return
        val rightUp = v(Ch.CAT_R) - (1f - v(Ch.CAT_R_A)) * 50f
        canvas.save()
        canvas.rotate(-rightUp, CHEER_SHOULDER_R_X, CHEER_SHOULDER_R_Y)
        canvas.translate(CAT_DX, 0f)
        canvas.translate(CAT_HAND_X, CAT_HAND_Y - 3f)
        canvas.rotate(rightUp)
        drawFlame(canvas, 0f, 0f, v(Ch.FLAME_H) * (0.4f + 0.6f * a), t, a)
        canvas.restore()
    }

    /** Reklam hâllerinin sahne nesneleri; zaman çizelgeleri [applyEmote]'takiyle aynı. */
    private fun drawAdProps(canvas: Canvas, e: Float) {
        when (emote) {
            Emote.AD_JUMP -> {
                val x = adTvX(e)
                if (x > 200f && x < 590f) drawAdTv(canvas, x)
            }
            Emote.AD_BUTTON -> drawSkipButton(canvas, e)
            Emote.AD_ZAP -> drawBillboardScene(canvas, e, zap = true)
            Emote.AD_MAGIC -> drawBillboardScene(canvas, e, zap = false)
            Emote.AD_PUSH -> {
                val appear = popScale(e - 0.1f)
                if (appear <= 0f) return
                val p = ((e - AD_PUSH_START_S) / (AD_PUSH_END_S - AD_PUSH_START_S)).coerceIn(0f, 1f)
                val cx = AD_SIGN_X + 120f * p * p   // yavaş başlayıp hızlanarak gidiyor
                if (cx > 590f) return
                val wobble = if (p > 0f && p < 1f) 4f * sin(e * TAU * 3f) else 0f
                drawAdSign(canvas, cx, appear, wobble)
            }
            else -> Unit
        }
    }

    /**
     * Şövaleli kara tahta: ahşap çerçeve, yeşil yüz; cevap tebeşirle soldan sağa beliriyor
     * (kırpma ile), sonra altı çiziliyor; tur sonunda siliniyor. Tahta tavşanın yanında, yerde.
     */
    private fun drawBoard(canvas: Canvas, e: Float) {
        val a = v(Ch.BOARD_A)
        withAlpha(canvas, a, FULL_LAYER) {
            canvas.scale(0.6f + 0.4f * a, 0.6f + 0.4f * a, (BOARD_X0 + BOARD_X1) / 2f, 305f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = WOOD
            canvas.drawLine(BOARD_X0 + 12f, BOARD_Y1, BOARD_X0 + 5f, 305f, paint)
            canvas.drawLine(BOARD_X1 - 12f, BOARD_Y1, BOARD_X1 - 5f, 305f, paint)
            paint.style = Paint.Style.FILL
            rect.set(BOARD_X0, BOARD_Y0, BOARD_X1, BOARD_Y1)
            canvas.drawRoundRect(rect, 4f, 4f, paint)
            paint.color = BOARD_GREEN
            rect.set(BOARD_X0 + 4f, BOARD_Y0 + 4f, BOARD_X1 - 4f, BOARD_Y1 - 4f)
            canvas.drawRoundRect(rect, 2f, 2f, paint)
            if (emote != Emote.BOARD) return@withAlpha

            val text = BOARD_SAMPLES[loopIndex % BOARD_SAMPLES.size]
            textPaint.textSize = BOARD_TEXT_SIZE
            val tw = textPaint.measureText(text)
            val cx = (BOARD_X0 + BOARD_X1) / 2f + 4f
            val left = cx - tw / 2f
            val reveal = ((e - BOARD_WRITE_S) / (BOARD_DONE_S - BOARD_WRITE_S)).coerceIn(0f, 1f)
            val keep = 1f - ((e - BOARD_ERASE_S) / 0.5f).coerceIn(0f, 1f)
            if (reveal > 0f && keep > 0f) {
                canvas.save()
                canvas.clipRect(left - 1f, BOARD_Y0, left + tw * reveal + 1f, BOARD_Y1)
                drawText(canvas, text, cx, BOARD_TEXT_Y, BOARD_TEXT_SIZE, CHALK, keep)
                canvas.restore()
                // Bitince altı çiziliyor.
                val under = ((e - BOARD_DONE_S) / 0.3f).coerceIn(0f, 1f)
                if (under > 0f) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.6f
                    paint.color = CHALK
                    paint.alpha = (255 * keep).roundToInt()
                    canvas.drawLine(left, BOARD_TEXT_Y + 5f, left + tw * under, BOARD_TEXT_Y + 5f, paint)
                }
            }
        }
    }

    /**
     * Defterin ARKA kapağı: tavşan yazdığı yüzü kendine dönük tutuyor, biz yazıyı değil
     * kapağı görüyoruz. Kırmızı kapak, üstte spiral, beyaz etiket ve yıldız çıkartması.
     */
    private fun drawNotebook(canvas: Canvas) {
        val a = v(Ch.NOTE_A)
        withAlpha(canvas, a, FULL_LAYER) {
            canvas.translate(0f, (1f - a) * 25f)
            paint.style = Paint.Style.FILL
            paint.color = NOTE_COVER
            rect.set(NOTE_X0, NOTE_Y0, NOTE_X1, NOTE_Y1)
            canvas.drawRoundRect(rect, 3.5f, 3.5f, paint)
            // Kapağın altında koyu bir şerit: kalınlık hissi
            paint.color = NOTE_COVER_DARK
            rect.set(NOTE_X0, NOTE_Y1 - 4f, NOTE_X1, NOTE_Y1)
            canvas.drawRoundRect(rect, 3.5f, 3.5f, paint)
            // Etiket
            paint.color = WHITE
            rect.set(NOTE_X0 + 16f, NOTE_Y0 + 14f, NOTE_X1 - 16f, NOTE_Y0 + 27f)
            canvas.drawRoundRect(rect, 2.5f, 2.5f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = METAL_GRAY
            canvas.drawLine(NOTE_X0 + 20f, NOTE_Y0 + 19f, NOTE_X1 - 20f, NOTE_Y0 + 19f, paint)
            canvas.drawLine(NOTE_X0 + 20f, NOTE_Y0 + 23f, NOTE_X1 - 28f, NOTE_Y0 + 23f, paint)
            // Spiral
            paint.strokeWidth = 1.2f
            var x = NOTE_X0 + 6f
            while (x <= NOTE_X1 - 4f) {
                canvas.drawCircle(x, NOTE_Y0, 1.8f, paint)
                x += 6f
            }
            // Köşede yıldız çıkartması
            drawStar(canvas, NOTE_X1 - 11f, NOTE_Y1 - 12f, 5f, 1f)
        }
    }

    /**
     * Havuç kalem (`carrot_ic`): sağ elin tuttuğu yerden ucu aşağı-sola, defterin arkasına
     * uzanıyor; ikon zaten bu açıda çizilmiş. Elden SONRA, defterden ÖNCE çiziliyor: ucu
     * kapağın arkasında kalıyor, üst gövdesi ve yaprakları kapağın üstünden görünüyor. Ellerle
     * birlikte aşağıdan geliyor.
     */
    private fun drawCarrot(canvas: Canvas) {
        val a = v(Ch.NOTE_A)
        val carrot = carrotDrawable ?: return
        if (a <= 0.01f) return
        val fa = v(Ch.FRONT_A)
        val pawX = FRONT_R_PAW_X + v(Ch.FRONT_R_X)
        val pawY = FRONT_R_PAW_Y + v(Ch.FRONT_R_Y) + v(Ch.FRONT_DY) + (1f - fa) * 25f
        canvas.save()
        canvas.translate(pawX, pawY)
        canvas.scale(CARROT_SCALE, CARROT_SCALE)
        canvas.translate(-CARROT_GRIP_X, -CARROT_GRIP_Y)
        carrot.alpha = (255 * a.coerceAtMost(1f)).roundToInt()
        carrot.draw(canvas)
        canvas.restore()
    }

    /**
     * Değnek hâllerinin sahnesi: sağda reklam billboardı (`billboard`).
     *  - [zap]: yıldırım düşüyor (yumuşak bir hale, ekranı bembeyaz yapan flaş YOK), billboard
     *    kararıyor, sonra çöküp kül yığınına dönüşüyor, kül tütüyor.
     *  - değilse: değnekten parıltılar billboarda uçuyor, üstüne kırmızı yasak işareti
     *    mühür gibi basılıyor, billboard yıldız tozuna dönüşüp kayboluyor.
     */
    private fun drawBillboardScene(canvas: Canvas, e: Float, zap: Boolean) {
        val bb = billboardDrawable ?: return
        val appear = popScale(e - 0.05f)
        if (appear <= 0f) return
        if (zap) {
            val char = ((e - ZAP_STRIKE_S) / ZAP_CHAR_S).coerceIn(0f, 1f)
            val crumble = ((e - ZAP_STRIKE_S - ZAP_CHAR_S) / ZAP_CRUMBLE_S).coerceIn(0f, 1f)
            if (crumble < 1f) {
                val hit = e - ZAP_STRIKE_S
                val shake = if (hit in 0f..0.3f) 2.5f * sin(hit * TAU * 25f) else 0f
                canvas.save()
                canvas.translate(shake, 0f)
                // Çökerken tabandan basıklaşıyor.
                canvas.scale(appear, appear * (1f - 0.7f * crumble), BB_X, 305f)
                val k = (255 * (1f - 0.78f * char)).roundToInt()
                bb.colorFilter = if (char > 0f) android.graphics.LightingColorFilter(android.graphics.Color.rgb(k, k, k), 0) else null
                drawBillboard(canvas, bb, 1f - crumble)
                bb.colorFilter = null
                canvas.restore()
            }
            val ashFade = 1f - ((e - ZAP_FADE_S) / 0.4f).coerceIn(0f, 1f)
            if (crumble > 0f && ashFade > 0f) drawAshPile(canvas, crumble, ashFade, e)
            // Duman: kül oluşurken üç bulut yükselip genişleyerek soluyor.
            val smokeT = e - ZAP_STRIKE_S - ZAP_CHAR_S
            if (smokeT > 0f) {
                paint.style = Paint.Style.FILL
                paint.color = SMOKE
                for (i in 0..2) {
                    val p = (smokeT / 1.3f - i * 0.18f)
                    if (p <= 0f || p >= 1f) continue
                    paint.alpha = (110 * (1f - p) * ashFade).roundToInt()
                    canvas.drawCircle(BB_X + (i - 1) * 10f + 6f * sin(p * 6f + i), 284f - 55f * p, 6f + 10f * p, paint)
                }
            }
            // Yıldırım: kararmadan hemen önce, birkaç kez yanıp sönerek.
            val bolt = e - ZAP_STRIKE_S + 0.08f
            if (bolt in 0f..0.32f) {
                val on = ((bolt * 22f).toInt() % 3) != 2
                if (on) drawBolt(canvas)
                paint.style = Paint.Style.FILL
                paint.color = BOLT
                paint.alpha = (110 * (1f - bolt / 0.32f)).roundToInt()
                canvas.drawCircle(BB_X, BB_BOARD_CY, 44f, paint)
            }
        } else {
            val stamp = e - MAGIC_STAMP_S
            val poof = ((e - MAGIC_POOF_S) / 0.4f).coerceIn(0f, 1f)
            if (poof < 1f) {
                val shake = if (stamp in 0f..0.3f) 2f * sin(stamp * TAU * 22f) else 0f
                canvas.save()
                canvas.translate(shake, 0f)
                canvas.scale(appear * (1f - poof), appear * (1f - poof), BB_X, BB_BOARD_CY)
                drawBillboard(canvas, bb, 1f - poof)
                // Yasak işareti: büyükten küçülerek "pat" diye basılıyor.
                if (stamp >= 0f) {
                    val p = (stamp / 0.18f).coerceIn(0f, 1f)
                    val s = 1f + 0.8f * (1f - p) * (1f - p)
                    drawNoSign(canvas, BB_X, BB_BOARD_CY, 29f * s, p * (1f - poof))
                }
                canvas.restore()
            }
            // Değnekten billboarda uçan parıltılar
            val tip = wandTip(WAND_POINT_UP)
            for (k in 0..2) {
                val q = (e - WAND_WAVE_END_S - k * 0.06f) / (MAGIC_STAMP_S - WAND_WAVE_END_S)
                if (q <= 0f || q >= 1f) continue
                val x = tip[0] + (BB_X - tip[0]) * q
                val y = tip[1] + (BB_BOARD_CY - tip[1]) * q - 30f * sin(PI.toFloat() * q)
                drawStar(canvas, x, y, 4.5f, 1f)
            }
            // Yıldız tozu: billboard kaybolurken dışa doğru saçılıyor.
            val dust = e - MAGIC_POOF_S
            if (dust in 0f..0.7f) {
                val p = dust / 0.7f
                for (i in 0 until 10) {
                    val ang = i * TAU / 10f + 0.3f
                    val r = 8f + 46f * p
                    drawStar(canvas, BB_X + cos(ang) * r, BB_BOARD_CY + sin(ang) * r, 4.5f * (1f - p) + 1f, 1f - p)
                }
            }
        }
    }

    private fun drawBillboard(canvas: Canvas, bb: android.graphics.drawable.Drawable, alpha: Float) {
        canvas.save()
        canvas.translate(BB_X - 256f * BB_SCALE, BB_TOP)
        canvas.scale(BB_SCALE, BB_SCALE)
        bb.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        bb.draw(canvas)
        canvas.restore()
    }

    /** Kırmızı yasak işareti: halka + sol üstten sağ alta çapraz çizgi. */
    private fun drawNoSign(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Float) {
        if (alpha <= 0f) return
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.23f
        paint.color = NO_SIGN_RED
        paint.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        canvas.drawCircle(cx, cy, r, paint)
        val d = r * 0.7071f
        canvas.drawLine(cx - d, cy - d, cx + d, cy + d, paint)
    }

    /** Zikzak yıldırım: ekranın tepesinden billboardın üstüne; sarı dış, beyaz iç çizgi. */
    private fun drawBolt(canvas: Canvas) {
        tailPath.reset()
        tailPath.moveTo(BB_X + 12f, 6f)
        tailPath.lineTo(BB_X - 8f, 80f)
        tailPath.lineTo(BB_X + 7f, 84f)
        tailPath.lineTo(BB_X - 12f, 160f)
        tailPath.lineTo(BB_X + 4f, 164f)
        tailPath.lineTo(BB_X - 2f, BB_TOP + 8f)
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.MITER
        paint.strokeWidth = 5f
        paint.color = BOLT
        canvas.drawPath(tailPath, paint)
        paint.strokeWidth = 2f
        paint.color = WHITE
        canvas.drawPath(tailPath, paint)
        paint.strokeJoin = Paint.Join.ROUND
    }

    /** Kül yığını: billboardın yerinde büyüyen koyu tepe, üstünde benekler ve sönen közler. */
    private fun drawAshPile(canvas: Canvas, grow: Float, alpha: Float, e: Float) {
        val h = 18f * grow
        paint.style = Paint.Style.FILL
        paint.color = ASH
        paint.alpha = (255 * alpha).roundToInt()
        rect.set(BB_X - 32f, 305f - h, BB_X + 32f, 305f + h)
        canvas.drawArc(rect, 180f, 180f, true, paint)
        paint.color = ASH_DARK
        paint.alpha = (255 * alpha).roundToInt()
        for (i in ASH_SPECK_X.indices) {
            canvas.drawCircle(BB_X + ASH_SPECK_X[i], 305f - h * ASH_SPECK_Y[i], 1.6f, paint)
        }
        // Közler: yanıp sönen turuncu noktalar
        paint.color = 0xFFFF7043.toInt()
        for (i in 0..2) {
            val blink = 0.5f + 0.5f * sin(e * TAU * 3f + i * 2f)
            paint.alpha = (200 * blink * alpha * grow).roundToInt()
            canvas.drawCircle(BB_X - 10f + i * 10f, 305f - h * 0.45f, 1.4f, paint)
        }
    }

    /** Küçük, eski usul TV: koyu gövde, açık ekranda kırmızı "REKLAM", iki anten, ayaklar. */
    private fun drawAdTv(canvas: Canvas, cx: Float) {
        paint.style = Paint.Style.FILL
        paint.color = 0x33000000
        rect.set(cx - 22f, 302f, cx + 22f, 308f)
        canvas.drawOval(rect, paint)
        // Antenler kısa ve yatık: tavşan üstünden geçerken ayakları değmesin (bkz. AD_JUMP_*).
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f
        paint.color = METAL_GRAY
        canvas.drawLine(cx - 3f, AD_TV_TOP, cx - 8f, AD_TV_TOP - 4.5f, paint)
        canvas.drawLine(cx + 3f, AD_TV_TOP, cx + 8f, AD_TV_TOP - 4.5f, paint)
        paint.style = Paint.Style.FILL
        paint.color = PHONE_BODY
        rect.set(cx - 9f, AD_TV_TOP + 20f, cx - 6f, 305f)
        canvas.drawRect(rect, paint)
        rect.set(cx + 6f, AD_TV_TOP + 20f, cx + 9f, 305f)
        canvas.drawRect(rect, paint)
        rect.set(cx - AD_TV_W / 2f, AD_TV_TOP, cx + AD_TV_W / 2f, AD_TV_TOP + 21f)
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.color = LIGHT
        rect.set(cx - AD_TV_W / 2f + 3f, AD_TV_TOP + 3f, cx + AD_TV_W / 2f - 3f, AD_TV_TOP + 18f)
        canvas.drawRoundRect(rect, 2f, 2f, paint)
        drawText(canvas, "REKLAM", cx, AD_TV_TOP + 14.2f, 9.5f, AD_RED, 1f)
    }

    /**
     * Video oynatıcılardaki "Atla ⏭" düğmesi: beliriyor, basılınca gömülüp maviye dönüyor
     * ve yıldızlar saçıyor, sonra sağa kayıp kayboluyor.
     */
    private fun drawSkipButton(canvas: Canvas, e: Float) {
        val appear = popScale(e - 0.1f)
        if (appear <= 0f) return
        val gone = ((e - AD_BTN_PRESS_S - 0.3f) / 0.4f).coerceIn(0f, 1f)
        if (gone >= 1f) return
        val pressed = e >= AD_BTN_PRESS_S
        val press = if (pressed) 1f - 0.08f * sin(PI.toFloat() * ((e - AD_BTN_PRESS_S) / 0.25f).coerceIn(0f, 1f)) else 1f
        val alpha = 1f - gone
        val cx = AD_BTN_X + 40f * gone
        val cy = AD_BTN_Y
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(appear * press, appear * press)
        rect.set(-AD_BTN_W / 2f, -12f, AD_BTN_W / 2f, 12f)
        paint.style = Paint.Style.FILL
        paint.color = if (pressed) BUBBLE_EDGE else PHONE_BODY
        paint.alpha = (255 * alpha).roundToInt()
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = WHITE
        paint.alpha = (255 * alpha).roundToInt()
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        drawText(canvas, "Atla", -6f, 4f, 11f, WHITE, alpha)
        // ⏭ simgesi: iki üçgen + çizgi
        paint.style = Paint.Style.FILL
        paint.color = WHITE
        paint.alpha = (255 * alpha).roundToInt()
        tailPath.reset()
        tailPath.moveTo(11f, -4f); tailPath.lineTo(15f, 0f); tailPath.lineTo(11f, 4f); tailPath.close()
        tailPath.moveTo(15f, -4f); tailPath.lineTo(19f, 0f); tailPath.lineTo(15f, 4f); tailPath.close()
        canvas.drawPath(tailPath, paint)
        rect.set(19.5f, -4f, 21f, 4f)
        canvas.drawRect(rect, paint)
        canvas.restore()
        if (pressed) {
            val s = popScale(e - AD_BTN_PRESS_S)
            val fade = (1f - (e - AD_BTN_PRESS_S) / 0.6f).coerceIn(0f, 1f)
            drawStar(canvas, cx - 18f, cy - 18f, 4f * s, fade)
            drawStar(canvas, cx + 22f, cy - 16f, 5f * s, fade)
            drawStar(canvas, cx + 6f, cy + 20f, 3.5f * s, fade)
        }
    }

    /** İki direkli REKLAM tabelası; itilirken tabanı etrafında sallanıyor. */
    private fun drawAdSign(canvas: Canvas, cx: Float, scale: Float, wobble: Float) {
        canvas.save()
        canvas.rotate(wobble, cx, 305f)
        canvas.scale(scale, scale, cx, 305f)
        paint.style = Paint.Style.FILL
        paint.color = 0x33000000
        rect.set(cx - 26f, 302f, cx + 26f, 308f)
        canvas.drawOval(rect, paint)
        paint.color = WOOD
        rect.set(cx - 17f, 240f, cx - 13f, 305f)
        canvas.drawRect(rect, paint)
        rect.set(cx + 13f, 240f, cx + 17f, 305f)
        canvas.drawRect(rect, paint)
        rect.set(cx - 25f, 213f, cx + 25f, 246f)
        paint.color = WHITE
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.4f
        paint.color = AD_RED
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        drawText(canvas, "REKLAM", cx + 4f, 233f, 10f, AD_RED, 1f)   // pati sol kenara yaslanıyor
        canvas.restore()
    }

    /**
     * Jet çantası: gövdenin iki yanından görünen iki gümüş tüp, altlarında meme ve aşağı
     * doğru yanan alev (seri alevinin tersi), arkada sönerek düşen kıvılcımlar.
     */
    private fun drawJetpack(canvas: Canvas, t: Float) {
        val a = v(Ch.JET_A)
        withAlpha(canvas, a, FULL_LAYER) {
            for ((i, x) in JET_TANK_X.withIndex()) {
                paint.style = Paint.Style.FILL
                paint.color = JET_SILVER
                rect.set(x, 196f, x + 18f, 248f)
                canvas.drawRoundRect(rect, 8f, 8f, paint)
                paint.color = JET_BAND
                rect.set(x, 210f, x + 18f, 215f)
                canvas.drawRect(rect, paint)
                paint.color = JET_SHINE
                rect.set(x + 3f, 200f, x + 7f, 240f)
                canvas.drawRoundRect(rect, 2f, 2f, paint)
                paint.color = JET_NOZZLE
                tailPath.reset()
                tailPath.moveTo(x + 3f, 248f)
                tailPath.lineTo(x + 15f, 248f)
                tailPath.lineTo(x + 13f, 256f)
                tailPath.lineTo(x + 5f, 256f)
                tailPath.close()
                canvas.drawPath(tailPath, paint)

                // Egzoz: ters çevrilmiş alev; boyu iki tüpte farklı fazda titriyor.
                val cx = x + 9f
                val h = 26f * (0.85f + 0.15f * sin(t * TAU * 7f + i * 2f)) * (0.4f + 0.6f * a)
                canvas.save()
                canvas.translate(cx, 255f)
                canvas.scale(1f, -1f)
                drawFlame(canvas, 0f, 0f, h, t + i * 0.37f, 1f)
                canvas.restore()

                // Kıvılcımlar alevin ucundan düşüp sönüyor.
                paint.style = Paint.Style.FILL
                paint.color = STAR
                for (k in 0..2) {
                    val p = ((t * 1.4f) + k / 3f + i * 0.17f) % 1f
                    paint.alpha = (255 * (1f - p) * 0.8f).roundToInt()
                    canvas.drawCircle(cx + 3f * sin(p * 7f + k), 258f + h * 0.6f + 30f * p, 2.2f * (1f - p), paint)
                }
            }
        }
    }

    /**
     * Süper kahraman pelerini: omuzlardan aşağı genişleyen kırmızı pelerin, iç yüzü koyu.
     * Yanları rüzgârla açılıp kapanıyor, alt kenarı dalgalanıyor. Belirirken omuzlardan
     * aşağı doğru uzuyor.
     */
    private fun drawCape(canvas: Canvas, t: Float) {
        val a = v(Ch.CAPE_A)
        withAlpha(canvas, a, FULL_LAYER) {
            canvas.scale(1f, 0.3f + 0.7f * a, 395f, 200f)
            val w = 4f * sin(t * TAU * 1.3f)
            val wave = 8f * sin(t * TAU * 1.8f)
            val l = 330f - w
            val r = 460f + w
            bubblePath.reset()
            bubblePath.moveTo(368f, 200f)
            bubblePath.cubicTo(352f, 215f, l + 6f, 250f, l, 288f)
            bubblePath.quadTo(l + 22f, 296f + wave, l + 44f, 288f)
            bubblePath.quadTo(l + 66f, 280f - wave, 395f, 290f)
            bubblePath.quadTo(r - 66f, 298f + wave, r - 44f, 288f)
            bubblePath.quadTo(r - 22f, 280f - wave, r, 288f)
            bubblePath.cubicTo(r - 6f, 250f, 438f, 215f, 422f, 200f)
            bubblePath.close()
            paint.style = Paint.Style.FILL
            paint.color = CAPE_RED
            canvas.drawPath(bubblePath, paint)
            tailPath.reset()
            tailPath.moveTo(374f, 204f)
            tailPath.cubicTo(362f, 220f, l + 16f, 252f, l + 12f, 284f)
            tailPath.lineTo(r - 12f, 284f)
            tailPath.cubicTo(r - 16f, 252f, 428f, 220f, 416f, 204f)
            tailPath.close()
            paint.color = CAPE_INNER
            canvas.drawPath(tailPath, paint)
        }
    }

    /** Pelerinle birlikte göğüste PRO rozeti (Pro ekranındaki rozetin renk geçişiyle). */
    private fun drawProBadge(canvas: Canvas) {
        val a = v(Ch.CAPE_A)
        if (a <= 0.01f) return
        val alpha = (255 * a.coerceAtMost(1f)).roundToInt()
        badgePaint.alpha = alpha
        rect.set(380f, 239f, 410f, 252f)
        canvas.drawRoundRect(rect, 3.5f, 3.5f, badgePaint)
        drawText(canvas, "PRO", 395f, 249f, 9f, WHITE, a)
    }

    /**
     * Başta yan yatık altın taç: üç sivri uç, mücevherli bant; uçlarda sırayla parıltılar.
     * Kafayla birlikte hareket ediyor (kafanın dönüşü içinde çiziliyor); belirirken yukarıdan
     * iniyor.
     */
    /**
     * Havalı güneş gözlüğü (`cool_glasses`): camların ortası gözlerin üstüne oturacak şekilde
     * ölçekli; kafayla birlikte hareket ediyor. Atla düğmesi hâlinde yukarıdan düşüyor ve
     * inince sağ camda bir parıltı çakıyor.
     */
    private fun drawGlasses(canvas: Canvas, e: Float) {
        val a = v(Ch.GLASSES_A)
        val glasses = glassesDrawable ?: return
        if (a <= 0.01f) return
        canvas.save()
        canvas.translate(GLASSES_LEFT, GLASSES_TOP + glassesDrop)
        canvas.scale(GLASSES_SCALE, GLASSES_SCALE)
        glasses.alpha = (255 * a.coerceAtMost(1f)).roundToInt()
        glasses.draw(canvas)
        canvas.restore()
        // Gözlük inince sağ camda parıltı ([coolFinale] zamanlıyor).
        val t = glassesSparkleT
        if (t > 0f && t < 0.6f) {
            drawStar(canvas, 404f, 136f, 6f * popScale(t), 1f - t / 0.6f)
        }
    }

    private fun drawCrown(canvas: Canvas, e: Float) {
        val a = v(Ch.CROWN_A)
        withAlpha(canvas, a, FULL_LAYER) {
            canvas.translate(0f, -(1f - a) * 20f + crownDrop)
            canvas.rotate(-10f, 389f, 113f)
            paint.style = Paint.Style.FILL
            paint.color = CROWN_GOLD
            tailPath.reset()
            tailPath.moveTo(368f, 112f)
            tailPath.lineTo(370f, 92f)
            tailPath.lineTo(380f, 103f)
            tailPath.lineTo(389f, 87f)
            tailPath.lineTo(398f, 103f)
            tailPath.lineTo(408f, 92f)
            tailPath.lineTo(410f, 112f)
            tailPath.close()
            canvas.drawPath(tailPath, paint)
            paint.color = CROWN_BAND
            rect.set(367f, 108f, 411f, 118f)
            canvas.drawRoundRect(rect, 2.5f, 2.5f, paint)
            paint.color = TONGUE
            canvas.drawCircle(378f, 113f, 2.2f, paint)
            canvas.drawCircle(400f, 113f, 2.2f, paint)
            paint.color = BUBBLE_EDGE
            canvas.drawCircle(389f, 113f, 2.4f, paint)
            paint.color = CROWN_TIP
            canvas.drawCircle(370f, 92f, 2.2f, paint)
            canvas.drawCircle(389f, 87f, 2.4f, paint)
            canvas.drawCircle(408f, 92f, 2.2f, paint)
            if (emote == Emote.CROWN) {
                for (k in CROWN_SPARKLE_X.indices) {
                    val p = ((e / 1.2f) + k / 3f) % 1f
                    drawStar(canvas, CROWN_SPARKLE_X[k], CROWN_SPARKLE_Y[k], 2.5f + 3f * p, sin(PI.toFloat() * p))
                }
            }
        }
    }

    /**
     * Uygulamanın seri alevi (`streak_flame_ic`) + arkasında sıcak bir hale. Alev tabanından
     * esneyip hafifçe sallanarak titriyor. ([cx], [bottom]) alevin tabanı, [h] yüksekliği.
     */
    private fun drawFlame(canvas: Canvas, cx: Float, bottom: Float, h: Float, t: Float, alpha: Float) {
        val flame = flameDrawable ?: return
        if (h <= 0f || alpha <= 0.01f) return
        canvas.save()
        canvas.translate(cx, bottom - h * 0.45f)
        val r = h * 0.9f * (1f + 0.06f * sin(t * TAU * 2.3f))
        canvas.scale(r, r)
        glowPaint.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        canvas.drawCircle(0f, 0f, 1f, glowPaint)
        canvas.restore()

        canvas.save()
        canvas.translate(cx, bottom)
        canvas.rotate(3f * sin(t * TAU * 1.7f))
        canvas.scale(
            1f - 0.04f * sin(t * TAU * 4.3f),
            1f + 0.07f * sin(t * TAU * 4.3f) + 0.03f * sin(t * TAU * 9.1f),
        )
        val s = h / FLAME_ICON_H
        canvas.scale(s, s)
        canvas.translate(-FLAME_ICON_CX, -FLAME_ICON_BOTTOM)
        flame.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        flame.draw(canvas)
        canvas.restore()
    }

    /**
     * Seri takvimi: beyaz kart, mavi başlık bandı ve halkalar, seçilen gün kadar kutu, altında
     * "N gün". Kutular sırayla işaretleniyor; hepsi dolunca köşede alev beliriyor. Eller
     * kartın iki yanında (önde) tutuyor.
     */
    private fun drawCalendar(canvas: Canvas, e: Float) {
        val a = v(Ch.CAL_A)
        withAlpha(canvas, a, FULL_LAYER) {
            val grow = 0.8f + 0.2f * a
            canvas.scale(grow, grow, 395f, 234f)
            paint.style = Paint.Style.FILL
            paint.color = WHITE
            rect.set(349f, 204f, 441f, 264f)
            canvas.drawRoundRect(rect, 7f, 7f, paint)
            paint.color = BUBBLE_EDGE
            rect.set(349f, 204f, 441f, 218f)
            canvas.drawRoundRect(rect, 7f, 7f, paint)
            rect.set(349f, 211f, 441f, 218f)
            canvas.drawRect(rect, paint)
            paint.color = PHONE_BODY
            canvas.drawCircle(370f, 204f, 2.8f, paint)
            canvas.drawCircle(420f, 204f, 2.8f, paint)

            val n = calendarDays()
            val boxes = if (n > 0) n else DEMO_DAYS.last()
            val playing = emote == Emote.CALENDAR && n > 0
            // İşaretler turun sonunda sönüyor, sonra boş kutularla baştan.
            val keep = 1f - ((e - CAL_RESET_S) / 0.5f).coerceIn(0f, 1f)
            val w = boxes * CAL_BOX + (boxes - 1) * CAL_GAP
            val x0 = 395f - w / 2f
            for (i in 0 until boxes) {
                val x = x0 + i * (CAL_BOX + CAL_GAP)
                rect.set(x, 228f, x + CAL_BOX, 228f + CAL_BOX)
                paint.style = Paint.Style.FILL
                paint.color = CAL_BOX_FILL
                canvas.drawRoundRect(rect, 2.5f, 2.5f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.2f
                paint.color = BUBBLE_EDGE
                canvas.drawRoundRect(rect, 2.5f, 2.5f, paint)
                val tick = if (playing) popScale(e - CAL_FIRST_TICK_S - i * CAL_TICK_S) * keep else 0f
                if (tick > 0f) {
                    canvas.save()
                    canvas.scale(tick, tick, x + CAL_BOX / 2f, 228f + CAL_BOX / 2f)
                    paint.strokeWidth = 2f
                    paint.color = CHECK_GREEN
                    tailPath.reset()
                    tailPath.moveTo(x + 2.2f, 233.2f)
                    tailPath.lineTo(x + 4.4f, 235.6f)
                    tailPath.lineTo(x + 8f, 230.6f)
                    canvas.drawPath(tailPath, paint)
                    canvas.restore()
                }
            }
            drawText(canvas, if (n > 0) "$n gün" else "? gün", 395f, 255f, 11f, EYE, 1f)
            if (playing) {
                val f = popScale(e - calendarDoneS(n)) * keep
                if (f > 0f) drawFlame(canvas, 437f, 212f, 18f * f, e, 1f)
            }
        }
    }

    /**
     * İki çanlı çalar saat: kırmızı gövde, beyaz kadran, üstte iki altın çan ve aralarında
     * çekiç, altta iki ayak. Akrep [alarmAngle]'da, yelkovan onun 12 katında (gerçek saatteki
     * gibi: akrep bir saat ilerlerken yelkovan bir tur atıyor, saat değişince hızla dönüyor).
     * Çalarken saat alt ortasından sallanıyor, çekiç titriyor, iki yanda ses çizgileri çıkıyor.
     * Ellerle birlikte aşağıdan geliyor.
     */
    private fun drawAlarmClock(canvas: Canvas, e: Float, now: Long) {
        val a = v(Ch.ALARM_A)
        if (a <= 0.01f) return
        val ring = if (emote == Emote.ALARM) alarmRingEnv(alarmRingAge(e, now)) else 0f
        withAlpha(canvas, a, FULL_LAYER) {
            canvas.translate(0f, (1f - a) * 25f)
            val grow = 0.8f + 0.2f * a
            canvas.scale(grow, grow, CLOCK_X, CLOCK_Y)

            canvas.save()
            canvas.rotate(ring * 7f * sin(e * TAU * ALARM_SHAKE_HZ), CLOCK_X, CLOCK_Y + CLOCK_R)
            // Ayaklar
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = ALARM_RED_DARK
            for (dir in intArrayOf(-1, 1)) {
                canvas.drawLine(
                    CLOCK_X + dir * 12f, CLOCK_Y + CLOCK_R - 6f,
                    CLOCK_X + dir * 17f, CLOCK_Y + CLOCK_R + 4f, paint,
                )
            }
            // Çanlar: gövdenin üst yanlarında, dışa eğik kubbeler (gövdenin arkasında).
            paint.style = Paint.Style.FILL
            for (dir in intArrayOf(-1, 1)) {
                val bx = CLOCK_X + dir * ALARM_BELL_SIN * (CLOCK_R + 3f)
                val by = CLOCK_Y - ALARM_BELL_COS * (CLOCK_R + 3f)
                canvas.save()
                canvas.rotate(dir * ALARM_BELL_DEG, bx, by)
                paint.color = ALARM_BELL
                rect.set(bx - 9f, by - 6f, bx + 9f, by + 12f)
                canvas.drawArc(rect, 180f, 180f, true, paint)
                paint.color = ALARM_BELL_DARK
                rect.set(bx - 9f, by + 2f, bx + 9f, by + 4.6f)
                canvas.drawRoundRect(rect, 1.3f, 1.3f, paint)
                canvas.restore()
            }
            // Çekiç: çalarken iki çanın arasında hızla sağa sola vuruyor.
            canvas.save()
            canvas.rotate(ring * 22f * sin(e * TAU * ALARM_SHAKE_HZ * 2f), CLOCK_X, CLOCK_Y - CLOCK_R)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.4f
            paint.color = ALARM_RED_DARK
            canvas.drawLine(CLOCK_X, CLOCK_Y - CLOCK_R, CLOCK_X, CLOCK_Y - CLOCK_R - 6f, paint)
            paint.style = Paint.Style.FILL
            paint.color = ALARM_BELL_DARK
            canvas.drawCircle(CLOCK_X, CLOCK_Y - CLOCK_R - 7.5f, 3f, paint)
            canvas.restore()
            // Gövde ve kadran
            paint.style = Paint.Style.FILL
            paint.color = ALARM_RED
            canvas.drawCircle(CLOCK_X, CLOCK_Y, CLOCK_R, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = ALARM_RED_DARK
            canvas.drawCircle(CLOCK_X, CLOCK_Y, CLOCK_R, paint)
            paint.style = Paint.Style.FILL
            paint.color = WHITE
            canvas.drawCircle(CLOCK_X, CLOCK_Y, CLOCK_R - 5f, paint)
            // Saat işaretleri: 12-3-6-9 çizgi, diğerleri nokta.
            paint.color = EYE
            paint.strokeWidth = 1.6f
            for (i in 0 until 12) {
                val rad = Math.toRadians((i * 30 - 90).toDouble())
                val cx = cos(rad).toFloat()
                val cy = sin(rad).toFloat()
                if (i % 3 == 0) {
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(
                        CLOCK_X + cx * (CLOCK_R - 9f), CLOCK_Y + cy * (CLOCK_R - 9f),
                        CLOCK_X + cx * (CLOCK_R - 6f), CLOCK_Y + cy * (CLOCK_R - 6f), paint,
                    )
                } else {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(CLOCK_X + cx * (CLOCK_R - 7.2f), CLOCK_Y + cy * (CLOCK_R - 7.2f), 0.8f, paint)
                }
            }
            // Akrep ve yelkovan
            paint.style = Paint.Style.STROKE
            drawClockHand(canvas, alarmAngle, CLOCK_R * 0.45f, 3f)
            drawClockHand(canvas, (alarmAngle * 12f) % 360f, CLOCK_R * 0.66f, 2f)
            paint.style = Paint.Style.FILL
            paint.color = ALARM_RED
            canvas.drawCircle(CLOCK_X, CLOCK_Y, 2f, paint)
            canvas.restore()

            // Ses çizgileri: iki yanda iç içe iki yay, saatle birlikte sallanmıyor.
            if (ring > 0.01f) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = WHITE
                val cy = CLOCK_Y - CLOCK_R * 0.9f
                for (dir in intArrayOf(-1, 1)) {
                    val cx = CLOCK_X + dir * (CLOCK_R + 12f)
                    val start = if (dir > 0) -35f else 145f
                    for ((r, op) in listOf(6f to 1f, 11f to 0.7f)) {
                        paint.alpha = (255 * ring * op).roundToInt()
                        rect.set(cx - r, cy - r, cx + r, cy + r)
                        canvas.drawArc(rect, start, 70f, false, paint)
                    }
                }
            }
        }
    }

    /** Kadranın ortasından [deg] açısına (12 = 0, saat yönünde) [len] uzunluğunda ibre. */
    private fun drawClockHand(canvas: Canvas, deg: Float, len: Float, width: Float) {
        val rad = Math.toRadians((deg - 90f).toDouble())
        paint.strokeWidth = width
        paint.color = EYE
        canvas.drawLine(
            CLOCK_X, CLOCK_Y,
            CLOCK_X + cos(rad).toFloat() * len, CLOCK_Y + sin(rad).toFloat() * len, paint,
        )
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
                // Düşünce balonu: iki kabarcık, sonra içinde "?" olan balon. Beyaz ve mavi
                // kenarlı: koyu zeminde ve tavşanın açık mavisinin yanında seçilsin diye.
                popCircle(canvas, 444f, 120f, 5f, e - 0.4f, fade, WHITE)
                popCircle(canvas, 461f, 100f, 8f, e - 0.7f, fade, WHITE)
                popCircle(canvas, 488f, 63f, 22f, e - 1.0f, fade, WHITE)
                val q = popScale(e - 1.15f)
                if (q > 0f) drawText(canvas, "?", 488f, 73f, 28f * q, EYE, fade)
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
                if (s > 0f) drawHeart(canvas, 389f, 76f - 4f * e, 17f * s, fade)
            }
            Emote.WINK -> {
                // Kırpılan gözün yanında bir parıltı
                val s = popScale(e - 0.15f)
                if (s > 0f) drawStar(canvas, 347f, 128f, 5f * s, fade)
            }
            Emote.CHAT -> drawChatBubbles(canvas, e)
            Emote.LISTEN -> {
                // Cevap duyulunca sağ üstte ✓ balonu belirip bir süre kalıyor.
                val start = LISTEN_ANSWER_S + 0.1f
                val s = popScale(e - start)
                val alpha = 1f - ((e - start - 1.2f) / 0.4f).coerceIn(0f, 1f)
                if (s > 0f && alpha > 0f) {
                    canvas.save()
                    canvas.translate(LISTEN_CHECK_X, LISTEN_CHECK_Y)
                    canvas.scale(s, s)
                    paint.style = Paint.Style.FILL
                    paint.color = WHITE
                    paint.alpha = (255 * alpha).roundToInt()
                    canvas.drawCircle(0f, 0f, 14f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = CHECK_GREEN
                    paint.alpha = (255 * alpha).roundToInt()
                    canvas.drawCircle(0f, 0f, 14f, paint)
                    paint.strokeWidth = 3f
                    tailPath.reset()
                    tailPath.moveTo(-6f, 0f)
                    tailPath.lineTo(-1.5f, 4.5f)
                    tailPath.lineTo(6.5f, -4.5f)
                    canvas.drawPath(tailPath, paint)
                    canvas.restore()
                }
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
        val a = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = a
        canvas.drawCircle(cx, cy, r * s, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f
        paint.color = BUBBLE_EDGE
        paint.alpha = a
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

    /**
     * Kalp: uygulamanın kendi kalp ikonu (`heart_ic`), [r] yarı genişlik. Drawable'ın
     * sınırları tam sayı olduğu için boyut sınırlarla değil tuvalin ölçeğiyle veriliyor;
     * yoksa atarken boyut piksel piksel zıplardı.
     */
    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, r: Float, alpha: Float) {
        val heart = heartDrawable ?: return
        canvas.save()
        canvas.translate(cx - r, cy - r)
        canvas.scale(2f * r / HEART_BOUNDS, 2f * r / HEART_BOUNDS)
        heart.alpha = (255 * alpha.coerceIn(0f, 1f)).roundToInt()
        heart.draw(canvas)
        canvas.restore()
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

        // Mesajlaşma döngüsünün zaman çizelgesi (sn, 6 sn'lik tur içinde)
        const val CHAT_ASK_S = 1.6f      // tavşanın soru balonu telefondan çıkıyor
        const val CHAT_WAIT_S = 1.9f     // yazmayı bırakıp cevabı bekliyor
        const val CHAT_REPLY_S = 2.3f    // öğretmenin balonu: "yazıyor..."
        const val CHAT_ANSWER_S = 3.6f   // noktalar ▶ oluyor, tavşan seviniyor
        const val CHAT_FADE_S = 5.0f     // balonlar sönüyor, yeniden yazıyor
        const val ASK_BUBBLE_X = 305f
        const val ASK_BUBBLE_Y = 150f
        const val REPLY_BUBBLE_X = 482f
        const val REPLY_BUBBLE_Y = 128f

        // Seri hâlleri
        val DEMO_DAYS = intArrayOf(3, 5, 7)  // StreakRepository.CHALLENGE_OPTIONS ile aynı

        // Çalar saat ([Emote.ALARM]): göğüs hizasında, patiler yanlardan tutuyor.
        val DEMO_ALARM_HOURS = intArrayOf(16, 18, 19, 20)  // StreakRepository.REMINDER_HOUR_OPTIONS ile aynı
        const val CLOCK_X = 395f
        const val CLOCK_Y = 240f
        const val CLOCK_R = 26f
        const val ALARM_BELL_DEG = 42f       // çanların dikeyden dışa açısı
        const val ALARM_BELL_SIN = 0.6691f   // sin 42°
        const val ALARM_BELL_COS = 0.7431f   // cos 42°
        const val ALARM_SHAKE_HZ = 14f
        const val ALARM_RING_AT_S = 1.2f     // turun içinde çalmanın başladığı an
        const val ALARM_RING_S = 1.2f        // bir çalışın süresi
        const val ALARM_QUIET_AFTER_S = 2f   // seçime tepkiden sonra düzenli çalışa kadar sessizlik
        const val ALARM_HAND_SPEED = 5f      // akrebin yaylı dönüş hızı (1/sn)
        const val FLAME_STEP_S = 2f          // deneme modunda alev 2 sn'de bir büyüyor
        const val FLAME_REACT_S = 0.8f       // büyümeden sonraki sevinme süresi
        // streak_flame_ic 24'lük çizimde: alev x=12 ortalı, tabanı y=20,5, boyu 18,5;
        // drawable 100'lük sınırlarla çiziliyor.
        const val FLAME_ICON_CX = 50f
        const val FLAME_ICON_BOTTOM = 20.5f / 24f * 100f
        const val FLAME_ICON_H = 18.5f / 24f * 100f
        const val CAL_FIRST_TICK_S = 0.6f    // ilk kutunun işaretlendiği an
        const val CAL_TICK_S = 0.35f         // işaretler arası
        const val CAL_RESET_S = 4.6f         // işaretler siliniyor
        const val CAL_BOX = 10f
        const val CAL_GAP = 2f

        val ABACUS_ROD_X = floatArrayOf(369f, 382f, 395f, 408f, 421f)

        // Renkler kaynak çizimin paletinden.
        const val EYE = 0xFF0C2C3C.toInt()
        const val MOUTH = 0xFF430231.toInt()
        const val TONGUE = 0xFFF281AA.toInt()
        const val BLUSH = 0x8CF281AA.toInt()
        const val LIGHT = 0xFFD8F4FF.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val ALARM_RED = 0xFFEF5350.toInt()
        const val ALARM_RED_DARK = 0xFFC62828.toInt()
        const val ALARM_BELL = 0xFFFFC107.toInt()
        const val ALARM_BELL_DARK = 0xFFE0A100.toInt()
        const val BUBBLE_EDGE = 0xFF5FBFFE.toInt()
        const val STAR_EDGE = 0xFFE0A800.toInt()
        const val PHONE_BODY = 0xFF22323A.toInt()   // kamera adası
        const val PHONE_EDGE = 0xFF2E4350.toInt()   // kılıf kenarı
        const val PHONE_BACK = 0xFF3A5566.toInt()   // kılıfın arka yüzü
        val PHONE_LENS_Y = floatArrayOf(205f, 212f)
        // Reklam hâlleri (sn, tur içinde; birim: tavşan koordinatı)
        // Reklamı atla: TV sağdan girip sola çıkıyor; tavşan TV tam altından geçerken
        // havada olacak şekilde zıplıyor. Ayakların tabanı 311; TV gövdesinin tepesi 279
        // (yükseklik > 32 gerekiyor, ayaklar altından 112 birimde geçiyor), anten uçları
        // ~274 (> 37 gerekiyor, 82 birimde geçiyor). Zıplama yüksekliği ve süresi bu iki
        // pencereden de uzun kalacak şekilde seçildi; kulaklar tepede görünümden taşmıyor.
        const val AD_TV_W = 50f
        const val AD_TV_TOP = 279f
        const val AD_TV_START_X = 580f
        const val AD_TV_ENTER_S = 0.15f
        const val AD_TV_SPEED = 260f
        const val AD_JUMP_HOP_LEN_S = 1.1f
        // Zıplamanın ortası TV'nin tavşanın ortasından (x=395) geçtiği an
        const val AD_JUMP_HOP_S = AD_TV_ENTER_S + (AD_TV_START_X - 395f) / AD_TV_SPEED - AD_JUMP_HOP_LEN_S / 2f
        const val AD_JUMP_HEIGHT = 42f
        const val AD_JUMP_CHEER_S = 1.6f
        // Atla düğmesi
        const val AD_BTN_X = 494f
        const val AD_BTN_Y = 225f
        const val AD_BTN_W = 56f
        const val AD_BTN_READY_S = 0.6f
        const val AD_BTN_PRESS_S = 1.05f
        // Düğmeye basıldıktan sonra: taç, ardından gözlük düşüyor; inince havalı poz.
        const val AD_BTN_CROWN_S = 1.45f
        const val AD_BTN_FADE_S = 4.0f
        // Ortak final ([coolFinale]): gözlük taçtan bu kadar sonra düşüyor.
        const val FINALE_GLASSES_DELAY_S = 0.25f
        const val AD_DROP_S = 0.4f
        const val AD_DROP_HEIGHT = 120f
        // Sihirli değnek (yıldırım / yasak işareti)
        const val WAND_UP_S = 0.4f          // değnek elde beliriyor
        const val WAND_WAVE_END_S = 1.5f    // "abra kadabra" bitti, billboarda doğrultuyor
        const val WAND_HZ = 1.6f
        const val WAND_ARM_UP = 30f
        const val WAND_POINT_UP = 5f
        const val WAND_LEN = 30f
        const val WAND_DIR_DEG = -60f       // değnek elden yukarı-sağa
        const val ZAP_STRIKE_S = 1.6f       // yıldırım
        const val ZAP_CHAR_S = 0.3f         // kararma süresi
        const val ZAP_CRUMBLE_S = 0.5f      // kül olma süresi
        const val ZAP_CROWN_S = 2.6f
        const val ZAP_FADE_S = 4.9f
        const val MAGIC_STAMP_S = 1.8f      // yasak işareti mühürleniyor (parıltılar 1,5'te yola çıkıyor)
        const val MAGIC_POOF_S = 2.2f       // yıldız tozuna dönüşme
        const val MAGIC_CROWN_S = 2.8f
        const val MAGIC_FADE_S = 5.1f
        // billboard.xml (512'lik): tabela üst 316 birim, direk alta kadar; sahnede yerde,
        // tavşanın sağında ~87 birim boyunda.
        const val BB_X = 494f
        const val BB_SCALE = 0.17f
        const val BB_TOP = 305f - 512f * BB_SCALE
        const val BB_BOARD_CY = BB_TOP + 158f * BB_SCALE
        const val NO_SIGN_RED = 0xFFE53935.toInt()
        const val ASH = 0xFF4A4A4A.toInt()
        const val ASH_DARK = 0xFF2E2E2E.toInt()
        const val SMOKE = 0xFF9E9E9E.toInt()
        const val BOLT = 0xFFFFE54C.toInt()
        const val WAND_STICK = 0xFF3E3550.toInt()
        val ASH_SPECK_X = floatArrayOf(-16f, -6f, 4f, 13f, -1f)
        val ASH_SPECK_Y = floatArrayOf(0.35f, 0.7f, 0.5f, 0.3f, 0.2f)
        // cool_glasses: 512×196 alanda camların ortaları x≈126 ve 386, y≈106; göz arası
        // (41,9) / cam arası (260) ölçeği ve camların ortası gözlerin ortasına oturacak yer.
        const val GLASSES_W = 512f
        const val GLASSES_H = 196f
        const val GLASSES_SCALE = 41.9f / 260f
        const val GLASSES_LEFT = 385.55f - 256f * GLASSES_SCALE
        const val GLASSES_TOP = 143.3f - 106f * GLASSES_SCALE
        // Reklamı itme
        const val AD_SIGN_X = 492f
        const val AD_PUSH_START_S = 0.9f
        const val AD_PUSH_END_S = 2.0f
        const val AD_RED = 0xFFE8453C.toInt()
        const val METAL_GRAY = 0xFF8B9DAB.toInt()

        // Bilgi sorusu hâlleri (sn, tur içinde; birim: tavşan koordinatı)
        // Kara tahta: tavşan 14 sola kayıyor, tahta sağında; tebeşirli el tahtanın sol kenarında.
        const val BOARD_X0 = 444f
        const val BOARD_X1 = 516f
        const val BOARD_Y0 = 190f
        const val BOARD_Y1 = 236f
        const val BOARD_TEXT_Y = 218f
        const val BOARD_TEXT_SIZE = 13f
        const val BOARD_WRITE_S = 0.8f
        const val BOARD_DONE_S = 2.1f
        const val BOARD_ERASE_S = 4.2f
        val BOARD_SAMPLES = arrayOf("9 yaş", "YouTube", "10 dk")   // deneme ekranı örnekleri
        const val BOARD_GREEN = 0xFF2F594E.toInt()
        const val CHALK = 0xFFEAF6FF.toInt()
        // Dinleyen tavşan: el yüzün yanında (düşünme kolunun daha alçak açısı)
        const val LISTEN_HAND_UP = 72f
        const val LISTEN_ANSWER_S = 2.0f
        const val LISTEN_CHECK_X = 482f
        const val LISTEN_CHECK_Y = 112f
        // Havuç kalem: defter (arka kapağı bize dönük) göğüste, sol el sol kenarında önde;
        // sağ el kalemi defterin arkasında tutuyor. Kalemin ucu kapağın arkasındaki iki
        // "satırda" gidip geliyor (yazı görünmüyor, el ve havucun üstü oynuyor).
        const val NOTE_X0 = 354f
        const val NOTE_X1 = 424f
        const val NOTE_Y0 = 212f
        const val NOTE_Y1 = 256f
        val NOTE_LINES = floatArrayOf(222f, 230f)
        const val NOTE_TEXT_X = 378f
        const val NOTE_LINE1_W = 32f
        const val NOTE_LINE2_W = 22f
        const val NOTE_LINE1_S = 0.5f
        const val NOTE_LINE1_END_S = 1.4f
        const val NOTE_LINE2_S = 1.5f
        const val NOTE_LINE2_END_S = 2.3f
        const val NOTE_ERASE_S = 3.9f        // sevinç bitişi
        // NOTEBOOK_WRITE yazma çizelgesini 0,1 sn'den başlatıyor (NOTE_LINE1_S - 0,1 kaydırma)
        const val NOTE_WRITE_OFFSET_S = NOTE_LINE1_S - 0.1f
        const val NOTE_COVER = 0xFFE8453C.toInt()
        const val NOTE_COVER_DARK = 0xFFB3261E.toInt()
        // Sağ elin (köpek patisi) merkezi
        const val FRONT_R_PAW_X = 408f
        const val FRONT_R_PAW_Y = 217f
        // carrot_ic (512'lik): ucu ≈(55, 505), elin tuttuğu yer gövdenin ortası ≈(180, 330);
        // boyu ~50 birim. Tutma noktası gövdenin ortasında ki elin üstünden turuncu gövde ve
        // yapraklar kapağın üstünde görünsün. Kalem ucunun ele göre yeri:
        // ((55-180), (505-330)) × ölçek. (Önizleme: scratchpad proto27.)
        const val CARROT_SCALE = 0.08f
        const val CARROT_GRIP_X = 180f
        const val CARROT_GRIP_Y = 330f
        const val NOTE_PEN_HAND_DX = (CARROT_GRIP_X - 55f) * CARROT_SCALE
        const val NOTE_PEN_HAND_DY = (CARROT_GRIP_Y - 505f) * CARROT_SCALE

        // Pro hâlleri
        val JET_TANK_X = floatArrayOf(338f, 434f)
        const val JET_SILVER = 0xFFB9C6D0.toInt()
        const val JET_BAND = 0xFF8B9DAB.toInt()
        const val JET_SHINE = 0xFFE4ECF1.toInt()
        const val JET_NOZZLE = 0xFF5F6B75.toInt()
        const val CAPE_RED = 0xFFE8453C.toInt()
        const val CAPE_INNER = 0x8CB3261E.toInt()
        const val CROWN_GOLD = 0xFFFFC93C.toInt()
        const val CROWN_BAND = 0xFFF2A900.toInt()
        const val CROWN_TIP = 0xFFFFE082.toInt()
        val CROWN_SPARKLE_X = floatArrayOf(364f, 389f, 414f)
        val CROWN_SPARKLE_Y = floatArrayOf(86f, 78f, 86f)
        const val CAL_BOX_FILL = 0xFFEAF6FF.toInt()
        const val CHECK_GREEN = 0xFF51B848.toInt()
        const val HEART_BOUNDS = 100f
        const val STAR = 0xFFFFDD00.toInt()
        const val WOOD = 0xFFB8743F.toInt()
        const val WOOD_LIGHT = 0xFFF7E6CC.toInt()
        const val WOOD_DARK = 0xFF9C6B3F.toInt()
        val ABACUS_BEADS = intArrayOf(
            0xFFF35720.toInt(), 0xFFFFDD00.toInt(), 0xFF8FA316.toInt(), 0xFFF281AA.toInt(), 0xFF5FBFFE.toInt(),
        )
    }
}
