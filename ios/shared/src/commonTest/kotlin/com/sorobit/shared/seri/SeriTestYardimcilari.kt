// Seri testlerinin ortak düzeneği: elle ilerletilen saat (+03:00) ve bellek içi depolar.
package com.sorobit.shared.seri

import com.sorobit.shared.oyun.InMemoryKeyValueStore
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

class SahteSeriSaati(baslangic: LocalDateTime) : StreakClock {
    val dilim: TimeZone = FixedOffsetTimeZone(UtcOffset(hours = 3))
    var simdi: Long = baslangic.toInstant(dilim).toEpochMilliseconds()
    var monoton: Long = 1_000_000L

    override fun nowMs(): Long = simdi
    override fun elapsedMs(): Long = monoton
    override fun timeZone(): TimeZone = dilim

    /** Duvar ve monoton saati birlikte ilerletir. */
    fun ilerlet(ms: Long) {
        simdi += ms
        monoton += ms
    }

    /** Ertesi güne geçer (aynı saat). */
    fun gunAtla(gun: Int = 1) {
        ilerlet(gun * 24L * 60 * 60 * 1000)
    }
}

/** Repository + izleyici + saat. 2026-11-25 Çarşamba 10:00 (+03:00). */
class SeriDuzenek(
    val saat: SahteSeriSaati = SahteSeriSaati(LocalDateTime(2026, 11, 25, 10, 0)),
) {
    val seriDeposu = InMemoryKeyValueStore()
    val sureDeposu = InMemoryKeyValueStore()
    val izleyici = StudyTimeTracker(sureDeposu, saat)
    val repo = StreakRepository(seriDeposu, izleyici, saat)

    fun gun(kayma: Int): String = StreakDays.dayId(saat, kayma)

    /** Bir ders ekranında [dakika] dakika çalışır (her dakika bir dokunuşla). */
    fun calis(dakika: Int) {
        izleyici.setActiveScreen("AbacusFragment")
        repeat(dakika) {
            saat.ilerlet(60_000L)
            izleyici.onUserInteraction()
        }
        izleyici.setActiveScreen(null)
    }
}
