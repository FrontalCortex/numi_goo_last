// Kaynak (Android): app/src/main/java/com/example/app/SeasonClock.kt — TAMAMI (sunucu saati sapması dahil).
// Farklar: Handler/Looper ile 60 sn'lik yoklama döngüsü platforma bırakıldı; döngünün her turda yaptığı iş
// [pollSeasonChange] fonksiyonudur (platform zamanlayıcısı SEASON_POLL_INTERVAL_MS aralığıyla çağırır).
// System.currentTimeMillis() -> kupaNowMs(). CopyOnWriteArrayList -> düz liste (yalnızca ana iş parçacığından
// kullanılmalı; bildirim sırasında listenin kopyası üzerinde dönülür).
package com.sorobit.shared.kupa.sezon

import com.sorobit.shared.kupa.kupaNowMs

/**
 * HAFTALIK SEZONLAR
 * Başlangıç noktası (Anchor): 21 Ağustos 2026 12:05:00 UTC
 *
 * Android (güncel): sezon hesapları düz cihaz saatinden değil, sunucu saatiyle cihaz saati arasındaki farkla
 * düzeltilmiş saatten ([nowUtcMs]) yapılır. Skor yazarken sezonu sunucu hesaplar; okurken buradaki sezon tahta
 * kimliğine girer — ikisi ayrışırsa kullanıcı yazdığı tahtadan başka bir tahtayı okur ve liste sonsuza kadar boş
 * kalır. Fark [onServerTime] ile öğrenilir; hiç öğrenilemediyse düz cihaz saati (eski davranış).
 * Sunucu çağrısını (callable [CF_GET_SEASON_INFO]), farkı diske yazmayı ve monoton saati platform yapar.
 */
object SeasonClock {

    // 21 Ağustos 2026, 12:05:00 UTC -> 1787313900000 ms
    val SEASON_ANCHOR_UTC_MS: Long = 1787313900000L
    val SEASON_DURATION_MS: Long = 7L * 24 * 60 * 60 * 1000L // 1 Hafta (7 gün)

    // Kontrol sıklığı: 60 saniye
    const val SEASON_POLL_INTERVAL_MS = 60_000L

    /** Sunucudan sezon/saat bilgisi isteyen callable ve yanıttaki alan. */
    const val CF_GET_SEASON_INFO = "getSeasonInfo"
    const val FIELD_SERVER_NOW_MS = "serverNowMs"

    /** Sunucudan en fazla bu sıklıkta saat istenir (sezon haftalık; daha sık sormak israf). */
    const val SYNC_MIN_INTERVAL_MS = 60L * 60 * 1000L

    /** Sezonun 2023 öncesine ya da 2096 sonrasına düşmesi bozuk cevap demektir. */
    const val PLAUSIBLE_MIN_UTC_MS = 1_700_000_000_000L
    const val PLAUSIBLE_MAX_UTC_MS = 4_000_000_000_000L

    /** Android `SharedPreferences("season_clock")` ve sapmanın anahtarı (platform kalıcılığı için). */
    const val PREFS_NAME = "season_clock"
    const val KEY_OFFSET_MS = "server_offset_ms"

    /**
     * Sunucu saati eksi cihaz saati. Sunucudan hiç haber alınmadıysa 0 — düz cihaz saati.
     * Platform değeri diske yazar; açılışta [restoreOffset] ile geri yükler.
     */
    var serverOffsetMs: Long = 0L
        private set

    /**
     * Sunucu saati gelince çağrılan gözlemci (Android: `TrustedClock.onServerTime` — can hesabının
     * güvenilir saati aynı cevaptan beslenir). Yalnızca akla yatkın değerlerde çağrılır.
     */
    var serverTimeObserver: ((Long) -> Unit)? = null

    private val seasonChangeListeners = mutableListOf<(Int, Int) -> Unit>()
    private var lastObservedSeason: Int = currentSeason()

    /** Sezon hesaplarında kullanılan düzeltilmiş "şimdi" (Android `nowUtcMs()`). */
    fun nowUtcMs(): Long = kupaNowMs() + serverOffsetMs

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

    /** Android `init(context)`: diskteki kalıcı sapmayı yükler ve sezon değiştiyse dinleyicileri uyarır. */
    fun restoreOffset(savedOffsetMs: Long) {
        serverOffsetMs = savedOffsetMs
        pollSeasonChange()
    }

    /** `getSeasonInfo` yanıtından `serverNowMs`; yoksa ya da sayı değilse null (Android: "cevabında serverNowMs yok"). */
    fun parseServerNow(resultData: Any?): Long? =
        ((resultData as? Map<*, *>)?.get(FIELD_SERVER_NOW_MS) as? Number)?.toLong()

    /**
     * Android `onServerTime`: sunucunun bildirdiği anı işler; sapmayı günceller ve sezon değiştiyse dinleyicileri
     * uyarır. Akla yatkın olmayan değer yok sayılır (false döner): bozuk cevap bütün sezon mantığını kaydırabilirdi.
     *
     * @return işlendiyse true; yeni sapma [serverOffsetMs]'te, platform onu diske yazmalıdır.
     */
    fun onServerTime(serverNowUtcMs: Long, deviceNowMs: Long = kupaNowMs()): Boolean {
        if (serverNowUtcMs < PLAUSIBLE_MIN_UTC_MS || serverNowUtcMs > PLAUSIBLE_MAX_UTC_MS) return false
        serverTimeObserver?.invoke(serverNowUtcMs)
        serverOffsetMs = serverNowUtcMs - deviceNowMs
        pollSeasonChange(serverNowUtcMs)
        return true
    }

    /**
     * Android `refreshFromServer` kısıtlaması: eşitleme sürüyorsa ya da son eşitlemeden beri
     * [SYNC_MIN_INTERVAL_MS] geçmediyse (ve [force] değilse) istek atılmaz.
     *
     * @param lastSyncElapsedMs son eşitlemenin MONOTON saat değeri; -1 = hiç eşitlenmedi.
     */
    fun shouldRefreshFromServer(
        force: Boolean,
        syncInFlight: Boolean,
        lastSyncElapsedMs: Long,
        elapsedNowMs: Long,
    ): Boolean {
        if (syncInFlight) return false
        if (!force && lastSyncElapsedMs >= 0 && elapsedNowMs - lastSyncElapsedMs < SYNC_MIN_INTERVAL_MS) return false
        return true
    }

    fun addSeasonChangeListener(listener: (oldSeason: Int, newSeason: Int) -> Unit) {
        seasonChangeListeners.add(listener)
    }

    fun removeSeasonChangeListener(listener: (oldSeason: Int, newSeason: Int) -> Unit) {
        seasonChangeListeners.remove(listener)
    }

    /**
     * Android `startSeasonPolling` içindeki Runnable'ın bir turu: sezon son gözlenenden farklıysa
     * dinleyicileri (old, current) ile çağırır; dinleyici istisnaları yutulur (`runCatching`).
     *
     * @return sezon değiştiyse true.
     */
    fun pollSeasonChange(now: Long = nowUtcMs()): Boolean {
        val current = currentSeason(now)
        if (current != lastObservedSeason) {
            val old = lastObservedSeason
            lastObservedSeason = current
            seasonChangeListeners.toList().forEach { runCatching { it(old, current) } }
            return true
        }
        return false
    }

    /** Yalnızca testler için: son gözlenen sezonu belirli bir ana göre sıfırlar ve dinleyicileri temizler. */
    internal fun resetForTest(now: Long) {
        seasonChangeListeners.clear()
        serverOffsetMs = 0L
        serverTimeObserver = null
        lastObservedSeason = currentSeason(now)
    }
}
