package com.example.app

/**
 * Günlük soru periyodu ve cache anahtarı.
 * [PERIOD_MS] = 24 saat; periyot anahtarı ve geri sayım buna göre hesaplanır.
 */
object DailyQuestionPeriod {
    const val PREFS_NAME = "daily_question_prefs"
    const val FIRESTORE_COLLECTION = "dailyQuestion"

    /** Bir günlük soru setinin yenilenme aralığı (24 saat). */
    const val PERIOD_MS = 24 * 60 * 60 * 1000L

    const val QUESTIONS_PER_PERIOD = 3

    /** Yanlış cevap sonrası aynı soruya devam için harcanan anahtar. */
    const val KEY_CONTINUE_COST = 1

    /** Kırık kalp animasyonunda yanlış cevap sonrası durulacak kare. */
    const val BROKEN_HEART_HOLD_FRAME = 116

    /** true = periyot boyunca aynı soru dizisi (prefs + Firestore). */
    const val USE_STABLE_SEQUENCE = true

    fun currentPeriodKey(): String {
        val bucket = System.currentTimeMillis() / PERIOD_MS
        return periodKeyForBucket(bucket)
    }

    fun periodKeyForBucket(bucket: Long): String = "period_$bucket"

    fun periodBucketFromKey(key: String): Long? {
        if (!key.startsWith("period_")) return null
        return key.removePrefix("period_").toLongOrNull()
    }

    /**
     * Son çözüm periyodu ile [currentPeriodKey] arasında en az bir boş periyot varsa seri kırılmış sayılır.
     */
    fun isFishingStreakBroken(
        lastStreakPeriodKey: String,
        currentPeriodKey: String = currentPeriodKey(),
    ): Boolean {
        if (lastStreakPeriodKey.isEmpty()) return false
        val last = periodBucketFromKey(lastStreakPeriodKey) ?: return true
        val current = periodBucketFromKey(currentPeriodKey) ?: return true
        return current > last + 1
    }

    fun isSamePeriod(a: String, b: String): Boolean = a.isNotEmpty() && a == b

    /** [lastStreakPeriodKey] hemen önceki periyot ve [currentPeriodKey] art arda ise true. */
    fun isConsecutivePeriodAfter(lastStreakPeriodKey: String, currentPeriodKey: String): Boolean {
        if (lastStreakPeriodKey.isEmpty()) return false
        val last = periodBucketFromKey(lastStreakPeriodKey) ?: return false
        val current = periodBucketFromKey(currentPeriodKey) ?: return false
        return current == last + 1
    }

    /** Mevcut periyot bitene kadar kalan süre (ms). */
    fun millisUntilCurrentPeriodEnds(): Long {
        val now = System.currentTimeMillis()
        val bucket = now / PERIOD_MS
        val periodEnd = (bucket + 1) * PERIOD_MS
        return (periodEnd - now).coerceAtLeast(0L)
    }

    /** Kart geri sayımı: `02:00` gibi; 1 saat+ için `1:04:32`. */
    fun formatCountdown(remainingMs: Long): String {
        val totalSec = ((remainingMs + 999) / 1000).toInt().coerceAtLeast(0)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
