// Kaynak: Android'de karşılığı yok — `System.currentTimeMillis()` ve `java.util.Calendar`
// çağrılarının ortak (saf Kotlin) karşılıkları. Takvim hesapları kotlinx-datetime ile yapılır.
package com.sorobit.shared.oyun

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** `System.currentTimeMillis()` karşılığı. */
fun oyunSimdiMs(): Long = Clock.System.now().toEpochMilliseconds()

/** Verilen ana ait yerel tarih (Calendar.getInstance().apply { timeInMillis = ms } karşılığı). */
internal fun yerelTarih(epochMs: Long, timeZone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date

/** O haftanın Pazartesi günü (Calendar ile "Pazartesi olana dek bir gün geri git" döngüsünün karşılığı). */
internal fun haftaninPazartesisi(tarih: LocalDate): LocalDate {
    var d = tarih
    while (d.dayOfWeek != DayOfWeek.MONDAY) {
        d = d.minus(1, DateTimeUnit.DAY)
    }
    return d
}

/** Yerel gün başlangıcının (00:00) epoch-ms değeri. */
internal fun gunBaslangiciMs(tarih: LocalDate, timeZone: TimeZone): Long =
    tarih.atStartOfDayIn(timeZone).toEpochMilliseconds()

internal fun LocalDate.gunEkle(gun: Int): LocalDate = this.plus(gun, DateTimeUnit.DAY)
