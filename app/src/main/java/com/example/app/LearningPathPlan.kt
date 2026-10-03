package com.example.app

import com.example.app.model.LessonItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

/**
 * Kayıttaki ilerleme yolu ekranının ([LearningPathView]) zaman etiketleri.
 *
 * Etiketler kullanıcının az önce seçtiği günlük süreden hesaplanıyor: ekranın amacı "bu hedefle
 * nereye varırsın" demek, sabit bir tarih herkese aynı sözü verirdi.
 *
 * ## Tahmin nasıl yapılıyor
 * "Kafadan hesap ustası" eşiği körleme toplamanın (4. bölüm) bitişi. O bölüme kadarki ders
 * adımları müfredattan ([GlobalLessonData.createLessonItems]) sayılıyor; ders eklenince tarih
 * kendiliğinden kayıyor. Adım başına süre ve haftalık çalışma günü VARSAYIM: bilerek temkinli
 * seçildi, çünkü çocuğa (ve veliye) tutmayacak kadar yakın bir tarih vermek, geç bir tarihten
 * daha kötü. Gerçek değer Analytics'teki `lesson_step_pass` olayının `elapsedMs` ortancasından
 * çıkarılıp [MINUTES_PER_STEP] ona göre düzeltilmeli.
 */
object LearningPathPlan {

    /** Kafadan hesap eşiği: bu bölümün sonu (4 = Körleme Toplama). */
    private const val MENTAL_MATH_LAST_PART = 4

    /** Bir ders adımının ortalama süresi (dakika); anlatım, hatalar ve tekrarlar dahil. Varsayım. */
    private const val MINUTES_PER_STEP = 4.0

    /** Haftada kaç gün çalışıldığı; herkes her gün çalışmıyor. Varsayım. */
    private const val PRACTICE_DAYS_PER_WEEK = 5.0

    /** Seri adımının etiketi: ilk haftanın sonu. */
    private const val STREAK_STEP_DAYS = 7

    private val TR = Locale("tr", "TR")

    /** Eşiğe kadarki ders adımları (sandık dersleri dahil). */
    private fun stepsToMentalMath(): Int =
        (1..MENTAL_MATH_LAST_PART).sumOf { part ->
            GlobalLessonData.createLessonItems(part)
                .filter { it.type == LessonItem.TYPE_LESSON || it.type == LessonItem.TYPE_CHEST }
                .sumOf { it.stepCount.coerceAtLeast(1) }
        }

    /** Günde [goalMinutes] dakikayla eşiğe kaç takvim gününde varılır. */
    fun daysToMentalMath(goalMinutes: Int): Int {
        val practiceDays = stepsToMentalMath() * MINUTES_PER_STEP / goalMinutes.coerceAtLeast(1)
        return ceil(practiceDays * 7.0 / PRACTICE_DAYS_PER_WEEK).toInt()
    }

    /** Üç adımın zaman etiketleri: "BUGÜN", "10 EKİM", "ARALIK 2026". */
    fun timeLabels(goalMinutes: Int, today: Calendar = Calendar.getInstance()): List<String> {
        val week = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, STREAK_STEP_DAYS) }
        val target = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, daysToMentalMath(goalMinutes))
        }
        return listOf(
            "BUGÜN",
            SimpleDateFormat("d MMMM", TR).format(week.time).uppercase(TR),
            // LLLL: ayın tek başına kullanılan adı (bazı dillerde "d MMMM"deki hâlinden farklı).
            SimpleDateFormat("LLLL yyyy", TR).format(target.time).uppercase(TR),
        )
    }
}
