// Kaynak (Android):
//  - app/src/main/java/com/example/app/StudyTimeTracker.kt (dayId: yerel takvim günü, yyyy-MM-dd)
//  - app/src/main/java/com/example/app/StreakSyncService.kt (utcOffsetMinutes)
//  - app/src/main/java/com/example/app/StreakViews.kt (buildWeekStrip: Pazartesiden başlayan takvim haftası)
//
// Android seri için GÜVENİLİR saati (TrustedClock) DEĞİL, cihazın saatini ve saat dilimini kullanır:
// `Calendar.getInstance()` + `SimpleDateFormat("yyyy-MM-dd")`. Sunucu gün kimliğini ±1 gün payla kabul
// eder (functions/index.js → STREAK_DAY_TOLERANCE_DAYS). Bu davranış korunmuştur; saat enjekte edilebilir
// (testler sabit saat verir).
package com.sorobit.shared.seri

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.TimeSource

/** Serinin saati: duvar saati, monoton saat ve saat dilimi. */
interface StreakClock {
    /** `System.currentTimeMillis()` karşılığı. */
    fun nowMs(): Long

    /** `SystemClock.elapsedRealtime()` karşılığı (yalnızca süre ölçümü için). */
    fun elapsedMs(): Long

    /** Cihazın saat dilimi (`TimeZone.getDefault()`). */
    fun timeZone(): TimeZone
}

/** Cihaz saati. iOS'ta `TimeZone.currentSystemDefault()` NSTimeZone'un sistem dilimini verir. */
class SystemStreakClock : StreakClock {
    private val baslangic = TimeSource.Monotonic.markNow()

    override fun nowMs(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    override fun elapsedMs(): Long = baslangic.elapsedNow().inWholeMilliseconds

    override fun timeZone(): TimeZone = TimeZone.currentSystemDefault()
}

/** Gün kimliği yardımcıları (Android: StudyTimeTracker.dayId). */
object StreakDays {

    /** Bugünün yerel tarihi. */
    fun today(clock: StreakClock): LocalDate =
        Instant.fromEpochMilliseconds(clock.nowMs()).toLocalDateTime(clock.timeZone()).date

    /**
     * Yerel takvim günü, `yyyy-MM-dd` (Android: `StudyTimeTracker.dayId(offsetDays)`; Calendar'a
     * gün ekleyip biçimlemekle aynı sonuç). `LocalDate.toString()` ISO biçimidir.
     */
    fun dayId(clock: StreakClock, offsetDays: Int): String =
        today(clock).plus(offsetDays, DateTimeUnit.DAY).toString()

    /** Cihazın UTC farkı (dakika). Türkiye için +180 (Android: StreakSyncService.utcOffsetMinutes). */
    fun utcOffsetMinutes(clock: StreakClock): Int {
        val an = Instant.fromEpochMilliseconds(clock.nowMs())
        return clock.timeZone().offsetAt(an).totalSeconds / 60
    }

    /**
     * Bu takvim haftasının günleri, Pazartesiden Pazara (Android: StreakViews.buildWeekStrip —
     * `firstDayOfWeek = MONDAY`, `(DAY_OF_WEEK + 5) % 7` kadar geri).
     */
    fun weekDayIds(clock: StreakClock): List<String> {
        var pazartesi: LocalDate = today(clock)
        while (pazartesi.dayOfWeek != DayOfWeek.MONDAY) {
            pazartesi = pazartesi.minus(1, DateTimeUnit.DAY)
        }
        return (0 until 7).map { pazartesi.plus(it, DateTimeUnit.DAY).toString() }
    }
}
