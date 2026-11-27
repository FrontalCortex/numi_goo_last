// Kaynak (Android): app/src/main/java/com/example/app/StreakMilestones.kt — tamamı.
//
// Seri kilometre taşları ve ödülleri. DİKKAT: ikizi sunucuda (functions/index.js → streakRewardFor,
// STREAK_CHALLENGE_REWARDS). Buradaki tablo YALNIZCA gösterim için; ödülü sunucu verir.
package com.sorobit.shared.seri

object StreakMilestones {

    /** Sabit basamaklar. Sonrası [REPEAT_STEP] günde bir devam eder. */
    private val FIXED = listOf(3, 7, 14, 30)

    const val REPEAT_STEP = 30

    /** Üst sınır: sonsuz döngüye karşı. Sunucudaki karşılığıyla aynı (STREAK_MAX_MILESTONE). */
    const val MAX_MILESTONE = 3600

    /** Ekranda en fazla kaç taş gösterilir (Android: visible(count = 3)). */
    const val VISIBLE_COUNT = 3

    /** Bir kilometre taşının ödülü (ikisi birden dolu olabilir; şu anki merdivende her basamak tek tür). */
    data class Reward(val keys: Int, val gold: Int)

    /** Meydan okuma ödülleri (altın). İkizi sunucuda (`STREAK_CHALLENGE_REWARDS`). */
    fun challengeReward(days: Int): Int = when (days) {
        3 -> 500
        5 -> 1000
        7 -> 1500
        else -> 0
    }

    fun rewardFor(milestone: Int): Reward? = when {
        milestone == 3 -> Reward(keys = 3, gold = 0)
        milestone == 7 -> Reward(keys = 0, gold = 2000)
        milestone == 14 -> Reward(keys = 5, gold = 0)
        milestone <= 0 || milestone > MAX_MILESTONE -> null
        milestone % REPEAT_STEP != 0 -> null
        // 30 altın, 60 anahtar, 90 altın, 120 anahtar…
        (milestone / REPEAT_STEP) % 2 == 0 -> Reward(keys = 10, gold = 0)
        else -> Reward(keys = 0, gold = 5000)
    }

    /** [milestone]'dan sonraki taş; üst sınırı aşarsa null. */
    fun next(milestone: Int): Int? {
        val nextFixed = FIXED.firstOrNull { it > milestone }
        val value = nextFixed ?: (milestone / REPEAT_STEP + 1) * REPEAT_STEP
        return if (value > MAX_MILESTONE) null else value
    }

    private fun first(): Int = FIXED.first()

    /** Ekranda gösterilecek taşlar: önce hak edilip toplanmamış olanlar, sonra sıradakiler. */
    fun visible(streak: Int, claimed: Set<Int>, count: Int): List<Int> {
        val result = mutableListOf<Int>()
        var m: Int? = first()
        while (m != null && m <= streak) {
            if (m !in claimed) result += m
            m = next(m)
        }
        while (m != null && result.size < count) {
            result += m
            m = next(m)
        }
        return result
    }

    /** Hak edilmiş ama toplanmamış taşlar. */
    fun claimable(streak: Int, claimed: Set<Int>): List<Int> {
        val result = mutableListOf<Int>()
        var m: Int? = first()
        while (m != null && m <= streak) {
            if (m !in claimed) result += m
            m = next(m)
        }
        return result
    }
}
