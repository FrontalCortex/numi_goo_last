// Kaynak (Android):
//  - app/src/main/java/com/example/app/StreakRepository.kt (REMINDER_HOUR_OPTIONS)
//  - app/src/main/java/com/example/app/StreakViews.kt (hourText, HOUR_GRID_START, showHourPicker ızgarası)
//  - app/src/main/java/com/example/app/StreakMilestones.kt (challengeReward: yalnızca gösterim, ikizi sunucuda)
//  - app/src/main/java/com/example/app/LearningPathPlan.kt (daysToMentalMath; tarih biçimleme platformda)
//
// Kayıt sorularının (UserInfoFragment) seri adımlarında kullanılan saf kurallar. Hedef ve meydan okuma
// seçenekleri ile varsayılan saat SignupFlowRules'tadır (LoginFlowRules.kt); burada yinelenmez.
package com.sorobit.shared.model

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem
import kotlin.math.ceil

object SignupStreakRules {
    /** Hatırlatma sorusundaki hazır saatler (Android: StreakRepository.REMINDER_HOUR_OPTIONS). */
    val REMINDER_HOUR_OPTIONS: List<Int> = listOf(16, 18, 19, 20)

    /** "Başka bir saat" penceresinin ilk saati (sabah); gece saatleri en sonda. */
    const val HOUR_GRID_START = 6

    /** Saat penceresinin 24 saati, ekrandaki sırayla (06:00 … 05:00). 4 sütunlu ızgara, 6 satır. */
    fun hourGrid(): List<Int> = (0 until 24).map { (HOUR_GRID_START + it) % 24 }

    /** "19:00" biçimi (Android: String.format(Locale.US, "%02d:00", hour)). */
    fun hourText(hour: Int): String = hour.toString().padStart(2, '0') + ":00"

    /** Seçili saat hazır saatlerden biri mi; değilse "Başka bir saat" satırı seçili görünür. */
    fun isPresetReminderHour(hour: Int): Boolean = hour in REMINDER_HOUR_OPTIONS

    /** Meydan okuma ödülü (altın). YALNIZCA gösterim: ödülü veren sunucu (`STREAK_CHALLENGE_REWARDS`). */
    fun challengeReward(days: Int): Int = when (days) {
        3 -> 500
        5 -> 1000
        7 -> 1500
        else -> 0
    }

    // ── İlerleme yolu (LearningPathPlan) ─────────────────────────────────────

    /** Kafadan hesap eşiği: bu bölümün sonu (4 = Körleme Toplama). */
    const val MENTAL_MATH_LAST_PART = 4

    /** Bir ders adımının ortalama süresi (dakika). Varsayım. */
    const val MINUTES_PER_STEP = 4.0

    /** Haftada kaç gün çalışıldığı. Varsayım. */
    const val PRACTICE_DAYS_PER_WEEK = 5.0

    /** İlerleme yolunun ikinci adımının etiketi: ilk haftanın sonu (gün). */
    const val STREAK_STEP_DAYS = 7

    /** Eşiğe kadarki ders adımları (sandık dersleri dahil), müfredattan sayılır. */
    fun stepsToMentalMath(): Int =
        (1..MENTAL_MATH_LAST_PART).sumOf { part ->
            GlobalLessonData.createLessonItems(part)
                .filter { it.type == LessonItem.TYPE_LESSON || it.type == LessonItem.TYPE_CHEST }
                .sumOf { it.stepCount.coerceAtLeast(1) }
        }

    /** [steps] adım, günde [goalMinutes] dakikayla kaç takvim gününde biter. */
    fun daysForSteps(steps: Int, goalMinutes: Int): Int {
        val practiceDays = steps * MINUTES_PER_STEP / goalMinutes.coerceAtLeast(1)
        return ceil(practiceDays * 7.0 / PRACTICE_DAYS_PER_WEEK).toInt()
    }

    /** Günde [goalMinutes] dakikayla kafadan hesap eşiğine kaç takvim gününde varılır. */
    fun daysToMentalMath(goalMinutes: Int): Int = daysForSteps(stepsToMentalMath(), goalMinutes)
}
