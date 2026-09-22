package com.example.app

import android.content.Context
import android.util.Log

/**
 * Günlük seri (streak): kullanıcının kaç gün üst üste günlük hedefini tutturduğu.
 *
 * ## Serinin kuralı
 * Bir gün "tutturulmuş" sayılır: o gün ders ekranlarında geçen süre
 * ([StudyTimeTracker.secondsToday]) hedefe ulaşırsa. Seri, tutturulan günler ARDIŞIK olduğu
 * sürece büyür; bir gün atlanırsa sıfırlanır.
 *
 * ## Neden "dün"e de bakılıyor
 * Seri okunurken [current] doğrudan dönülmüyor: son tutturulan gün dünden eskiyse seri çoktan
 * kırılmış demektir ama bunu yazan bir olay yok (kullanıcı uygulamayı hiç açmamış olabilir).
 * Bu yüzden kırılma yazıldığı anda değil, OKUNDUĞU anda hesaplanıyor.
 *
 * ## Neden şimdilik yerel
 * Seri, kayıt olmadan önce — ilk ders sırasında — başlıyor; o anda kullanıcının uid'i yok.
 * Bu yüzden kaynak burası, Firestore senkronu bunun üstüne gelecek.
 */
object StreakRepository {

    private const val TAG = "StreakRepository"
    private const val PREFS = "streak_prefs"

    private const val KEY_GOAL_MINUTES = "goal_minutes"
    private const val KEY_CHALLENGE_DAYS = "challenge_days"
    private const val KEY_CURRENT = "current"
    private const val KEY_LONGEST = "longest"
    private const val KEY_LAST_DAY = "last_goal_day"
    private const val KEY_ACHIEVED_DAYS = "achieved_days"

    /** Onboarding'de sunulan günlük hedefler (dakika). */
    val GOAL_OPTIONS = listOf(5, 10, 20)

    /** Onboarding'de sunulan "kaç gün üst üste" hedefleri. */
    val CHALLENGE_OPTIONS = listOf(3, 5, 7)

    /** Hedef hiç seçilmediyse. En düşük seçenek: kimseyi ilk günden kaybetmeyelim. */
    private const val DEFAULT_GOAL_MINUTES = 5
    private const val DEFAULT_CHALLENGE_DAYS = 3

    /** Hafta şeridi için saklanan gün sayısı. */
    private const val ACHIEVED_HISTORY_DAYS = 21

    /** Seri ekranında gösterilecek durum. */
    data class StreakState(
        /** Güncel seri; kırıldıysa 0. */
        val current: Int,
        val longest: Int,
        val goalMinutes: Int,
        val challengeDays: Int,
        val secondsToday: Int,
        /** Hedefi tutturulmuş günler (`yyyy-MM-dd`), hafta şeridi için. */
        val achievedDays: Set<String>,
    ) {
        val goalSeconds: Int get() = goalMinutes * 60
        val goalReachedToday: Boolean get() = secondsToday >= goalSeconds

        /** Bugünkü ilerleme, 0f–1f. */
        val todayFraction: Float
            get() = if (goalSeconds <= 0) 1f
            else (secondsToday.toFloat() / goalSeconds).coerceIn(0f, 1f)

        /** Hedefe kalan dakika, en az 1 (0 kalan "tamamlandı" demek, o ayrı durum). */
        val minutesLeft: Int
            get() = ((goalSeconds - secondsToday + 59) / 60).coerceAtLeast(1)
    }

    // ── Hedefler ────────────────────────────────────────────────────────

    fun goalMinutes(context: Context): Int =
        prefs(context)?.getInt(KEY_GOAL_MINUTES, DEFAULT_GOAL_MINUTES) ?: DEFAULT_GOAL_MINUTES

    fun setGoalMinutes(context: Context, minutes: Int) {
        val value = if (minutes in GOAL_OPTIONS) minutes else DEFAULT_GOAL_MINUTES
        prefs(context)?.edit()?.putInt(KEY_GOAL_MINUTES, value)?.apply()
    }

    fun challengeDays(context: Context): Int =
        prefs(context)?.getInt(KEY_CHALLENGE_DAYS, DEFAULT_CHALLENGE_DAYS) ?: DEFAULT_CHALLENGE_DAYS

    fun setChallengeDays(context: Context, days: Int) {
        val value = if (days in CHALLENGE_OPTIONS) days else DEFAULT_CHALLENGE_DAYS
        prefs(context)?.edit()?.putInt(KEY_CHALLENGE_DAYS, value)?.apply()
    }

    // ── Seri ────────────────────────────────────────────────────────────

    /**
     * Seriyi güncel süreye göre tazeler ve son durumu döner.
     *
     * Aynı gün içinde kaç kez çağrıldığı önemsiz: gün bir kez ilerletiliyor. Ekran her
     * açıldığında ve süre değiştikçe çağrılabilir.
     */
    fun refresh(context: Context): StreakState {
        val p = prefs(context)
        val goal = goalMinutes(context)
        val seconds = StudyTimeTracker.secondsToday(context)
        val today = StudyTimeTracker.dayId()
        val yesterday = StudyTimeTracker.dayId(-1)

        var current = p?.getInt(KEY_CURRENT, 0) ?: 0
        var longest = p?.getInt(KEY_LONGEST, 0) ?: 0
        var lastDay = p?.getString(KEY_LAST_DAY, "").orEmpty()
        var achieved = readAchievedDays(context)

        if (seconds >= goal * 60 && lastDay != today) {
            // Dün de tutturulmuşsa seri devam eder, yoksa bugünden yeniden başlar.
            current = if (lastDay == yesterday) current + 1 else 1
            longest = maxOf(longest, current)
            lastDay = today
            achieved = achieved + today
            writeState(context, current, longest, lastDay, achieved)
        }

        // Kırılma yazılmıyor, okunurken hesaplanıyor: kullanıcı uygulamayı hiç açmadan da
        // seriyi kırabilir, o anda çalışan bir kodumuz yok.
        val effectiveCurrent = if (lastDay == today || lastDay == yesterday) current else 0

        return StreakState(
            current = effectiveCurrent,
            longest = longest,
            goalMinutes = goal,
            challengeDays = challengeDays(context),
            secondsToday = seconds,
            achievedDays = achieved,
        )
    }

    private fun writeState(
        context: Context,
        current: Int,
        longest: Int,
        lastDay: String,
        achieved: Set<String>,
    ) {
        val cutoff = (0 until ACHIEVED_HISTORY_DAYS).map { StudyTimeTracker.dayId(-it) }.toSet()
        prefs(context)?.edit()
            ?.putInt(KEY_CURRENT, current)
            ?.putInt(KEY_LONGEST, longest)
            ?.putString(KEY_LAST_DAY, lastDay)
            ?.putString(KEY_ACHIEVED_DAYS, achieved.intersect(cutoff).sorted().joinToString(","))
            ?.apply()
    }

    private fun readAchievedDays(context: Context): Set<String> =
        prefs(context)?.getString(KEY_ACHIEVED_DAYS, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .toSet()

    private fun prefs(context: Context) = try {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    } catch (e: Throwable) {
        Log.w(TAG, "SharedPreferences açılamadı", e)
        null
    }

    /** Yalnızca hata ayıklama/test için. */
    fun resetForDebug(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
    }
}
