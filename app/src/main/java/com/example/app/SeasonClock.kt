package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import java.util.concurrent.CopyOnWriteArrayList

/**
 * HAFTALIK SEZONLAR
 * Başlangıç noktası (Anchor): 21 Ağustos 2026 12:05:00 UTC
 *
 * ## Neden cihaz saatine doğrudan güvenilmiyor
 * Skor yazarken sezonu SUNUCU hesaplıyor (functions/index.js → submitLeaderboardScore).
 * Liderlik tablosunu OKURKEN ise sezon buradan geliyor ve tahta kimliğine giriyor
 * (`part_1_lesson_..._season_7`). İkisi ayrışırsa kullanıcı yazdığı tahtadan BAŞKA bir
 * tahtayı okur: ekran sonsuza kadar boş kalır, hiçbir hata da görünmez. Çocuk
 * tabletlerinde yanlış tarih nadir değil.
 *
 * Bu yüzden burada sunucu saatiyle cihaz saati arasındaki fark tutuluyor
 * ([serverOffsetMs]) ve bütün sezon hesapları düzeltilmiş saatten yapılıyor. Fark
 * sunucudan [refreshFromServer] ile öğreniliyor; hiç öğrenilemediyse davranış eskisiyle
 * aynı, yani düz cihaz saati.
 */
object SeasonClock {

    private const val TAG = "SeasonClock"

    // 21 Ağustos 2026, 12:05:00 UTC -> 1787313900000 ms
    val SEASON_ANCHOR_UTC_MS: Long = 1787313900000L
    val SEASON_DURATION_MS: Long = 7L * 24 * 60 * 60 * 1000L // 1 Hafta (7 gün)

    // Kontrol sıklığı: 60 saniye
    private const val SEASON_POLL_INTERVAL_MS = 60_000L

    private const val PREFS = "season_clock"
    private const val KEY_OFFSET_MS = "server_offset_ms"

    /** Sunucudan en fazla bu sıklıkta saat isteriz. Sezon haftalık; daha sık sormak israf. */
    private const val SYNC_MIN_INTERVAL_MS = 60L * 60 * 1000L

    /** Sezonun 2023 öncesine ya da 2096 sonrasına düşmesi bozuk cevap demektir. */
    private const val PLAUSIBLE_MIN_UTC_MS = 1_700_000_000_000L
    private const val PLAUSIBLE_MAX_UTC_MS = 4_000_000_000_000L

    /**
     * Sunucu saati eksi cihaz saati. Sunucudan hiç haber alınmadıysa 0 — yani düz cihaz saati,
     * eski davranış.
     */
    @Volatile private var serverOffsetMs: Long = 0L

    private var prefs: SharedPreferences? = null

    /**
     * Son eşitlemenin zamanı, MONOTON saatle. Duvar saatiyle tutulsaydı kullanıcı saati
     * değiştirdiğinde kısıtlama ya sonsuza kadar açık ya sonsuza kadar kapalı kalırdı.
     * -1 = hiç eşitlenmedi.
     */
    @Volatile private var lastSyncElapsedMs: Long = -1L

    @Volatile private var syncInFlight = false

    private val seasonChangeListeners = CopyOnWriteArrayList<(Int, Int) -> Unit>()
    private var lastObservedSeason: Int = currentSeason()

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    init {
        startSeasonPolling()
    }

    /**
     * Kalıcı sapmayı yükler. [NumiGooApplication.onCreate] içinden çağrılır.
     *
     * Sapma diske yazılıyor çünkü asıl düzeltmek istediğimiz durum "cihazın tarihi yanlış" —
     * o cihazda uygulama her açılışta sunucu cevabını beklemek zorunda kalmasın. Cevap
     * geldiğinde değer zaten tazeleniyor; kullanıcı saatini düzeltirse eski sapma en fazla
     * bir saniye yaşar.
     */
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        serverOffsetMs = p.getLong(KEY_OFFSET_MS, 0L)
        notifySeasonIfChanged()
    }

    /** Sezon hesaplarında kullanılan düzeltilmiş "şimdi". */
    fun nowUtcMs(): Long = System.currentTimeMillis() + serverOffsetMs

    /** 1 tabanlı sezon indeksi */
    fun currentSeason(nowUtcMs: Long = nowUtcMs()): Int {
        val elapsed = nowUtcMs - SEASON_ANCHOR_UTC_MS
        if (elapsed < 0) return 1
        return (elapsed / SEASON_DURATION_MS).toInt() + 1
    }

    /** Mevcut sezonun bitişine kalan süre (ms), en az 0. */
    fun millisUntilCurrentSeasonEnds(nowUtcMs: Long = nowUtcMs()): Long {
        val elapsed = nowUtcMs - SEASON_ANCHOR_UTC_MS
        if (elapsed < 0) return -elapsed
        val remainder = elapsed % SEASON_DURATION_MS
        return (SEASON_DURATION_MS - remainder).coerceAtLeast(0L)
    }

    /**
     * Sunucunun bildirdiği anı işler: sapmayı günceller, diske yazar ve sezon değiştiyse
     * dinleyicileri uyarır (böylece açık olan liderlik ekranı doğru tahtaya yeniden bağlanır).
     *
     * Akla yatkın olmayan değer yok sayılır: bozuk bir cevap bütün sezon mantığını
     * kaydırabilirdi.
     */
    fun onServerTime(serverNowUtcMs: Long) {
        if (serverNowUtcMs < PLAUSIBLE_MIN_UTC_MS || serverNowUtcMs > PLAUSIBLE_MAX_UTC_MS) {
            Log.w(TAG, "Sunucu saati akla yatkın değil, yok sayıldı: $serverNowUtcMs")
            return
        }
        val offset = serverNowUtcMs - System.currentTimeMillis()
        serverOffsetMs = offset
        lastSyncElapsedMs = SystemClock.elapsedRealtime()
        prefs?.edit()?.putLong(KEY_OFFSET_MS, offset)?.apply()
        Log.d(TAG, "Sunucu saati eşitlendi: sapma=${offset}ms sezon=${currentSeason()}")
        notifySeasonIfChanged()
    }

    /**
     * Sunucudan sezon bilgisini ister ve sapmayı tazeler.
     *
     * Serbestçe çağrılabilir: [SYNC_MIN_INTERVAL_MS] içinde ikinci kez istek atılmaz.
     * Başarısızlık sessizdir — cevap gelmezse cihaz saatiyle devam edilir, yani eski davranış.
     */
    fun refreshFromServer(force: Boolean = false) {
        if (syncInFlight) return
        val last = lastSyncElapsedMs
        if (!force && last >= 0 && SystemClock.elapsedRealtime() - last < SYNC_MIN_INTERVAL_MS) {
            return
        }
        syncInFlight = true
        try {
            FirebaseFunctions.getInstance()
                .getHttpsCallable("getSeasonInfo")
                .call()
                .addOnSuccessListener { result ->
                    syncInFlight = false
                    val map = result.data as? Map<*, *>
                    val serverNow = (map?.get("serverNowMs") as? Number)?.toLong()
                    if (serverNow != null) {
                        onServerTime(serverNow)
                    } else {
                        Log.w(TAG, "getSeasonInfo cevabında serverNowMs yok")
                    }
                }
                .addOnFailureListener { e ->
                    syncInFlight = false
                    Log.w(TAG, "getSeasonInfo başarısız; cihaz saatiyle devam", e)
                }
        } catch (e: Throwable) {
            syncInFlight = false
            Log.w(TAG, "getSeasonInfo çağrılamadı", e)
        }
    }

    fun addSeasonChangeListener(listener: (oldSeason: Int, newSeason: Int) -> Unit) {
        seasonChangeListeners.add(listener)
    }

    fun removeSeasonChangeListener(listener: (oldSeason: Int, newSeason: Int) -> Unit) {
        seasonChangeListeners.remove(listener)
    }

    private fun notifySeasonIfChanged() {
        val current = currentSeason()
        if (current == lastObservedSeason) return
        val old = lastObservedSeason
        lastObservedSeason = current
        seasonChangeListeners.forEach { runCatching { it(old, current) } }
    }

    private fun startSeasonPolling() {
        if (pollRunnable != null) return
        pollRunnable = object : Runnable {
            override fun run() {
                notifySeasonIfChanged()
                handler.postDelayed(this, SEASON_POLL_INTERVAL_MS)
            }
        }
        handler.post(pollRunnable!!)
    }
}
