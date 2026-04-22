package com.example.app

import android.content.Context
import java.util.Calendar

/**
 * Günlük / haftalık görev sayaçları (ChestFragment sandık talebi = +1 her iki sayaç için).
 */
object MissionsProgressStore {

    private const val PREFS = "missions_progress"
    private const val KEY_DAY_ID = "day_id"
    private const val KEY_WEEK_ID = "week_id"
    private const val KEY_DAILY_COUNT = "daily_count"
    private const val KEY_WEEKLY_COUNT = "weekly_count"

    data class Snapshot(
        val dailyCount: Int,
        val weeklyCount: Int,
    )

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun todayId(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }

    /** Bu haftanın Pazartesi tarihini (yerel) benzersiz anahtar olarak kullanır */
    private fun weekIdNow(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    fun ensureResets(context: Context) {
        val p = prefs(context)
        val ed = p.edit()
        val t = todayId()
        if (p.getString(KEY_DAY_ID, null) != t) {
            ed.putString(KEY_DAY_ID, t)
            ed.putInt(KEY_DAILY_COUNT, 0)
        }
        val w = weekIdNow()
        if (p.getString(KEY_WEEK_ID, null) != w) {
            ed.putString(KEY_WEEK_ID, w)
            ed.putInt(KEY_WEEKLY_COUNT, 0)
        }
        ed.apply()
    }

    fun getSnapshot(context: Context): Snapshot {
        ensureResets(context)
        val p = prefs(context)
        return Snapshot(
            dailyCount = p.getInt(KEY_DAILY_COUNT, 0),
            weeklyCount = p.getInt(KEY_WEEKLY_COUNT, 0),
        )
    }

    /** Sandık talebinden sonra çubukta görünür bir değişim oldu mu (tam dolu sayaçlarda artış görünmez). */
    fun hasVisibleMissionProgress(before: Snapshot, after: Snapshot): Boolean {
        val d1b = minOf(before.dailyCount, 1)
        val d1a = minOf(after.dailyCount, 1)
        val d2b = minOf(before.dailyCount, 2)
        val d2a = minOf(after.dailyCount, 2)
        val w1b = minOf(before.weeklyCount, 1)
        val w1a = minOf(after.weeklyCount, 1)
        val w2b = minOf(before.weeklyCount, 2)
        val w2a = minOf(after.weeklyCount, 2)
        return d1b != d1a || d2b != d2a || w1b != w1a || w2b != w2a
    }

    /** ChestFragment ödül talep / ilerleme işlendiğinde */
    fun recordChestClaim(context: Context) {
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putInt(KEY_DAILY_COUNT, p.getInt(KEY_DAILY_COUNT, 0) + 1)
            .putInt(KEY_WEEKLY_COUNT, p.getInt(KEY_WEEKLY_COUNT, 0) + 1)
            .apply()
    }

    /** Günlük ve haftalık görev sayaçlarını sıfırlar. */
    fun resetAllProgress(context: Context) {
        prefs(context).edit()
            .putString(KEY_DAY_ID, todayId())
            .putString(KEY_WEEK_ID, weekIdNow())
            .putInt(KEY_DAILY_COUNT, 0)
            .putInt(KEY_WEEKLY_COUNT, 0)
            .apply()
    }

    /** Bir sonraki yerel gece yarısına kalan tam saat (en az 1) */
    fun hoursUntilDailyReset(): Int {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = next.timeInMillis - now.timeInMillis
        return (diff / (1000 * 60 * 60)).toInt().coerceAtLeast(1)
    }

    /**
     * Haftalık pencere sonu: bu haftanın Pazartesi 00:00 + 7 gün.
     * Kalan süre 24 saatten fazlaysa gün, değilse saat metni için kullanılır.
     */
    fun millisUntilWeeklyReset(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        cal.add(Calendar.DAY_OF_YEAR, 7)
        return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }
}
