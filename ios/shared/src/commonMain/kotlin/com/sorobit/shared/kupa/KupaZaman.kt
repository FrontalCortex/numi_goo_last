// Android'de doğrudan karşılığı olan tek bir dosya yoktur; kupa paketindeki "şimdi" bağımlılıklarını toplar:
//  - System.currentTimeMillis()                       -> [kupaNowMs]
//  - MissionsProgressStore.calendarDayId()            -> [kupaCalendarDayId]
//  - SimpleDateFormat("yyyy-MM-dd", Locale.US) + Calendar (CupHistoryRepository.todayId, CupHistoryFragment.daysAgo)
//                                                     -> [kupaDateId]
package com.sorobit.shared.kupa

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Android `System.currentTimeMillis()` karşılığı. */
fun kupaNowMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Android `MissionsProgressStore.calendarDayId()` ile aynı biçim: yerel saat dilimindeki
 * `"${Calendar.YEAR}-${Calendar.DAY_OF_YEAR}"` (ör. "2026-325"; sıfır dolgusu YOK).
 * Bu anahtar Firestore'a (`userRocketDailyDayId`) yazıldığı için biçim Android ile birebir aynı olmalıdır.
 */
fun kupaCalendarDayId(
    nowMs: Long = kupaNowMs(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val t = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone)
    return "${t.year}-${t.dayOfYear}"
}

/**
 * Yerel saat dilimindeki tarihten [daysAgo] gün öncesinin "yyyy-MM-dd" kimliği.
 * `daysAgo = 0` -> Android `CupHistoryRepository.todayId()`; `daysAgo = n` -> `CupHistoryFragment.daysAgo(n)`.
 */
fun kupaDateId(
    daysAgo: Int = 0,
    nowMs: Long = kupaNowMs(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val date = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
    val d = date.minus(daysAgo, DateTimeUnit.DAY)
    return d.year.toString().padStart(4, '0') + "-" +
        d.monthNumber.toString().padStart(2, '0') + "-" +
        d.dayOfMonth.toString().padStart(2, '0')
}
