// Kaynak (Android): app/src/main/java/com/example/app/TrustedClock.kt ve TrustedClockRules.kt (YENİ).
// Farklar: SharedPreferences("trusted_clock") -> [OyunKeyValueStore]; `SystemClock.elapsedRealtime()` ->
// [elapsedRealtimeMs] (platform: iOS'ta ProcessInfo.systemUptime / mach_continuous_time ms); `Settings.Global.BOOT_COUNT`
// -> [bootCount] (okunamıyorsa 0 verilir; iOS'ta karşılığı yok, 0 geçilir); ölçüm (AnalyticsLogger.logDeviceClockSkew) ve
// Log taşınmadı.
//
// Bağlantı (Android'deki gibi): [SeasonClock.serverTimeObserver] -> [TrustedClock.onServerTime]; [EnergyManager]
// oluşturulurken `nowMs = trustedClock::nowMs` verilir ve çapa değişince [EnergyManager.onTimerTick] çağrılır
// ([addAnchorListener] ile) — Android `EnergyManager.trustedClockListener`.
package com.sorobit.shared.oyun

/**
 * [TrustedClock]'un hesabı; platformdan bağımsız birim testiyle sabitlenir (Android `TrustedClockRulesTest`).
 */
object TrustedClockRules {

    /**
     * Sunucudan öğrenilen bir an ve o anda cihazın MONOTON saatinin gösterdiği değer.
     *
     * @param serverNowMs Sunucunun bildirdiği "şimdi" (Unix ms).
     * @param elapsedMs Cevap geldiğinde monoton saat.
     * @param bootCount Cevap geldiğinde cihazın açılış sayacı; monoton saat her yeniden başlatmada sıfırlandığı için
     *   çapanın hangi açılışa ait olduğu bilinmeli.
     */
    data class Anchor(val serverNowMs: Long, val elapsedMs: Long, val bootCount: Int)

    /**
     * Cihazın duvar saati elle değiştirilse de kaymayan "şimdi".
     *
     * - Çapa bu açılışa aitse: sunucu anı + o zamandan beri GERÇEKTEN geçen süre (duvar saatine hiç bakılmaz).
     * - Çapa eski bir açılıştan kalmışsa: duvar saatine dönülür ama çapadaki andan geriye gidilmez.
     * - Çapa hiç yoksa: düz duvar saati (eski davranış).
     */
    fun trustedNow(anchor: Anchor?, wallNowMs: Long, elapsedNowMs: Long, bootCount: Int): Long {
        if (anchor == null) return wallNowMs
        val sameBoot = anchor.bootCount == bootCount && elapsedNowMs >= anchor.elapsedMs
        if (sameBoot) return anchor.serverNowMs + (elapsedNowMs - anchor.elapsedMs)
        return maxOf(wallNowMs, anchor.serverNowMs)
    }
}

/**
 * Cihaz saatinin elle değiştirilmesinden etkilenmeyen "şimdi" (can hesabı için).
 *
 * Can sunucuda tutulur (`energy_full_time`) ama "şu an kaç can var" cihaz saatiyle hesaplanıyordu; çocuk saati ileri
 * alınca uygulama canı dolu sanıp dersi başlatıyordu. Sunucu saati her açılışta ve saatte bir öğrenilir
 * ([SeasonClock.refreshFromServer] karşılığı); o an monoton saatle çapa olarak saklanır.
 *
 * Kapatamadığı: çapa yokken (cihaz yeniden başlatıldı VE internet yok) duvar saatine dönülür.
 */
class TrustedClock(
    /** Android: "trusted_clock" adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
    /** Monoton saat (ms); duvar saatinden bağımsız akmalı. */
    private val elapsedRealtimeMs: () -> Long,
    /** Cihazın açılış sayacı; okunamıyorsa 0. */
    private val bootCount: Int = 0,
    private val wallNowMs: () -> Long = ::oyunSimdiMs,
) {
    companion object {
        const val PREFS_NAME = "trusted_clock"
        const val KEY_SERVER_NOW_MS = "server_now_ms"
        const val KEY_ELAPSED_MS = "elapsed_ms"
        const val KEY_BOOT_COUNT = "boot_count"

        /** Hiçbir gerçek açılış sayacıyla eşleşmeyen değer (açılış kanıtlanamayan çapa için). */
        const val UNKNOWN_BOOT = -1
    }

    private var anchor: TrustedClockRules.Anchor? = null
    private val anchorListeners = mutableListOf<() -> Unit>()

    init {
        // Android `init(context)`: diskteki çapayı yükler; ağ çağrısı yok.
        if (prefs.contains(KEY_SERVER_NOW_MS)) {
            val savedBootCount = prefs.getInt(KEY_BOOT_COUNT, 0)
            // Açılış sayacı okunamıyorsa (0) diskteki çapanın BU açılışa ait olduğu kanıtlanamaz: hiçbir açılışla
            // eşleşmeyen sayaçla yüklenir (yalnızca alt sınır olarak kullanılır; tam değer ilk sunucu cevabıyla gelir).
            val bootKnown = bootCount != 0 && savedBootCount != 0
            anchor = TrustedClockRules.Anchor(
                serverNowMs = prefs.getLong(KEY_SERVER_NOW_MS, 0L),
                elapsedMs = prefs.getLong(KEY_ELAPSED_MS, 0L),
                bootCount = if (bootKnown) savedBootCount else UNKNOWN_BOOT,
            )
        }
    }

    /** Çapa değişince haber verilecekler (Android `addAnchorListener`); yalnızca ana iş parçacığından kullanılmalı. */
    fun addAnchorListener(listener: () -> Unit) {
        anchorListeners.add(listener)
    }

    fun removeAnchorListener(listener: () -> Unit) {
        anchorListeners.remove(listener)
    }

    /**
     * Sunucunun bildirdiği anı çapa yapar (Android `onServerTime`; [SeasonClock] çağırır).
     *
     * @return cihaz duvar saati eksi sunucu saati (ms) — Android bunu sapma olarak ölçüme yazar.
     */
    fun onServerTime(serverNowMs: Long): Long {
        val elapsed = elapsedRealtimeMs()
        anchor = TrustedClockRules.Anchor(serverNowMs, elapsed, bootCount)
        prefs.putLong(KEY_SERVER_NOW_MS, serverNowMs)
        prefs.putLong(KEY_ELAPSED_MS, elapsed)
        prefs.putInt(KEY_BOOT_COUNT, bootCount)
        val skewMs = wallNowMs() - serverNowMs
        anchorListeners.toList().forEach { runCatching { it() } }
        return skewMs
    }

    /** Duvar saati değiştirilse de kaymayan "şimdi" (Unix ms). */
    fun nowMs(): Long = TrustedClockRules.trustedNow(
        anchor = anchor,
        wallNowMs = wallNowMs(),
        elapsedNowMs = elapsedRealtimeMs(),
        bootCount = bootCount,
    )
}
