package com.example.app

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * HAFTALIK SEZONLAR
 * Başlangıç noktası (Anchor): 21 Ağustos 2026 12:05:00 UTC
 */
object SeasonClock {

    // 21 Ağustos 2026, 12:05:00 UTC -> 1787313900000 ms
    val SEASON_ANCHOR_UTC_MS: Long = 1787313900000L
    val SEASON_DURATION_MS: Long = 7L * 24 * 60 * 60 * 1000L // 1 Hafta (7 gün)

    // Kontrol sıklığı: 60 saniye
    private const val SEASON_POLL_INTERVAL_MS = 60_000L

    private val seasonChangeListeners = CopyOnWriteArrayList<(Int, Int) -> Unit>()
    private var lastObservedSeason: Int = currentSeason()

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    init {
        startSeasonPolling()
    }

    /** 1 tabanlı sezon indeksi */
    fun currentSeason(nowUtcMs: Long = System.currentTimeMillis()): Int {
        val elapsed = nowUtcMs - SEASON_ANCHOR_UTC_MS
        if (elapsed < 0) return 1
        return (elapsed / SEASON_DURATION_MS).toInt() + 1
    }

    /** Mevcut sezonun bitişine kalan süre (ms), en az 0. */
    fun millisUntilCurrentSeasonEnds(nowUtcMs: Long = System.currentTimeMillis()): Long {
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

    private fun startSeasonPolling() {
        if (pollRunnable != null) return
        pollRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val current = currentSeason(now)
                if (current != lastObservedSeason) {
                    val old = lastObservedSeason
                    lastObservedSeason = current
                    seasonChangeListeners.forEach { runCatching { it(old, current) } }
                }
                handler.postDelayed(this, SEASON_POLL_INTERVAL_MS)
            }
        }
        handler.post(pollRunnable!!)
    }
}
