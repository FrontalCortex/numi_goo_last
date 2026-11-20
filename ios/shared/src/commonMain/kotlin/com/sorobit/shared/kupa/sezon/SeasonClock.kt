// Kaynak (Android): app/src/main/java/com/example/app/SeasonClock.kt — TAMAMI.
// Farklar: Handler/Looper ile 60 sn'lik yoklama döngüsü platforma bırakıldı; döngünün her turda yaptığı iş
// [pollSeasonChange] fonksiyonudur (platform zamanlayıcısı SEASON_POLL_INTERVAL_MS aralığıyla çağırır).
// System.currentTimeMillis() -> kupaNowMs(). CopyOnWriteArrayList -> düz liste (yalnızca ana iş parçacığından
// kullanılmalı; bildirim sırasında listenin kopyası üzerinde dönülür).
package com.sorobit.shared.kupa.sezon

import com.sorobit.shared.kupa.kupaNowMs

/**
 * HAFTALIK SEZONLAR
 * Başlangıç noktası (Anchor): 21 Ağustos 2026 12:05:00 UTC
 */
object SeasonClock {

    // 21 Ağustos 2026, 12:05:00 UTC -> 1787313900000 ms
    val SEASON_ANCHOR_UTC_MS: Long = 1787313900000L
    val SEASON_DURATION_MS: Long = 7L * 24 * 60 * 60 * 1000L // 1 Hafta (7 gün)

    // Kontrol sıklığı: 60 saniye
    const val SEASON_POLL_INTERVAL_MS = 60_000L

    private val seasonChangeListeners = mutableListOf<(Int, Int) -> Unit>()
    private var lastObservedSeason: Int = currentSeason()

    /** 1 tabanlı sezon indeksi */
    fun currentSeason(nowUtcMs: Long = kupaNowMs()): Int {
        val elapsed = nowUtcMs - SEASON_ANCHOR_UTC_MS
        if (elapsed < 0) return 1
        return (elapsed / SEASON_DURATION_MS).toInt() + 1
    }

    /** Mevcut sezonun bitişine kalan süre (ms), en az 0. */
    fun millisUntilCurrentSeasonEnds(nowUtcMs: Long = kupaNowMs()): Long {
        val elapsed = nowUtcMs - SEASON_ANCHOR_UTC_MS
        if (elapsed < 0) return -elapsed
        val remainder = elapsed % SEASON_DURATION_MS
        return (SEASON_DURATION_MS - remainder).coerceAtLeast(0L)
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
    fun pollSeasonChange(now: Long = kupaNowMs()): Boolean {
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
        lastObservedSeason = currentSeason(now)
    }
}
