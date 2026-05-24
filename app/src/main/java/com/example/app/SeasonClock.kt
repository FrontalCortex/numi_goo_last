package com.example.app

import android.os.Handler
import android.os.Looper
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.CopyOnWriteArrayList

/**
 * UTC takvim ayı sezonları — tüm cihazlar ve [functions/seasonCalendar.js] ile aynı formül.
 * Sezon 1 = anchor ayı (Mayıs 2026); her ayın 1'i 00:00:00 UTC'de yeni sezon.
 * Tahta doküman id'leri ve rozet `season` alanı bu değerle hizalanır.
 */
object SeasonClock {

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * Sezon 1 epokunun başlangıcı: 1 Mayıs 2026 00:00:00 UTC.
     * [functions/seasonCalendar.js] SEASON_ANCHOR_UTC_MS ile aynı olmalı.
     */
    val SEASON_ANCHOR_UTC_MS: Long = GregorianCalendar(UTC).run {
        set(2026, Calendar.MAY, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private const val ANCHOR_YEAR = 2026
    private const val ANCHOR_MONTH = Calendar.MAY

    /** Aylık sezon geçişi için yeterli; ay sınırında listener tetiklenir. */
    private const val SEASON_POLL_INTERVAL_MS = 60_000L

    private val seasonChangeListeners = CopyOnWriteArrayList<(Int, Int) -> Unit>()
    private var lastObservedSeason: Int = currentSeason()

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    init {
        startSeasonPolling()
    }

    private fun monthIndexUtc(year: Int, month: Int): Int = year * 12 + month

    /** 1 tabanlı sezon indeksi; anchor ayından bu yana geçen tam UTC ay sayısı + 1. */
    fun currentSeason(nowUtcMs: Long = System.currentTimeMillis()): Int {
        val cal = GregorianCalendar(UTC).apply { timeInMillis = nowUtcMs }
        val nowIdx = monthIndexUtc(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        val anchorIdx = monthIndexUtc(ANCHOR_YEAR, ANCHOR_MONTH)
        if (nowIdx < anchorIdx) return 1
        return nowIdx - anchorIdx + 1
    }

    /** Bir sonraki ayın 1'i 00:00 UTC'ye (mevcut sezon bitişi) kalan süre (ms), en az 0. */
    fun millisUntilCurrentSeasonEnds(nowUtcMs: Long = System.currentTimeMillis()): Long {
        val cal = GregorianCalendar(UTC).apply { timeInMillis = nowUtcMs }
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return (cal.timeInMillis - nowUtcMs).coerceAtLeast(0L)
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
