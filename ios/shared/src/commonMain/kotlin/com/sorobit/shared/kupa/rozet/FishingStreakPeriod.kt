// Kaynak (Android): app/src/main/java/com/example/app/DailyQuestionPeriod.kt — yalnızca rozet (Derin Avcı serisi)
// mantığının kullandığı fonksiyonlar: currentPeriodKey, periodKeyForBucket, periodBucketFromKey,
// isFishingStreakBroken, isSamePeriod, isConsecutivePeriodAfter.
// Not: DailyQuestionPeriod dosyasının tamamı `oyun` paketinin sorumluluğundadır; paketler arası bağımlılık olmasın
// diye rozet hesabının ihtiyaç duyduğu saf kısım burada ayrı adla kopyalandı (davranış birebir aynı).
package com.sorobit.shared.kupa.rozet

import com.sorobit.shared.kupa.kupaNowMs

object FishingStreakPeriod {
    /** Bir günlük soru setinin yenilenme aralığı (24 saat). */
    const val PERIOD_MS = 24 * 60 * 60 * 1000L

    fun currentPeriodKey(nowMs: Long = kupaNowMs()): String {
        val bucket = nowMs / PERIOD_MS
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
}
