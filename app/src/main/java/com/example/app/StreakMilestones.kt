package com.example.app

/**
 * Seri kilometre taşları ve ödülleri.
 *
 * ## DİKKAT: bu tablonun ikizi sunucuda
 * Ödülü gerçekten veren taraf `functions/index.js` içindeki `streakRewardFor`. Buradaki
 * tablo YALNIZCA gösterim için: kullanıcı "3 gün → 3 anahtar" yazısını toplamadan önce
 * görebilsin diye. İkisi birbirinden ayrılırsa kullanıcı bir şey görüp başka bir şey alır,
 * o yüzden biri değişirse diğeri de değişmeli.
 *
 * Sunucu tek yetkili olduğu için buradaki bir hata ödülü DEĞİŞTİRMEZ, yalnızca yanlış
 * gösterir — bu bilinçli bir tercih.
 */
object StreakMilestones {

    /** Sabit basamaklar. Sonrası [REPEAT_STEP] günde bir devam ediyor. */
    private val FIXED = listOf(3, 7, 14, 30)

    private const val REPEAT_STEP = 30

    /** Üst sınır: sonsuz döngüye karşı. Sunucudaki karşılığıyla aynı. */
    private const val MAX_MILESTONE = 3600

    /**
     * Bir kilometre taşının ödülü.
     *
     * İkisi birden dolu olabilir diye duruyor: şu anki merdivende her basamak tek tür
     * veriyor ama tablo değiştiğinde çizim tarafını da değiştirmek zorunda kalmayalım
     * (bkz. [StreakViews.buildMilestoneRows]).
     */
    data class Reward(val keys: Int, val gold: Int)

    /**
     * Meydan okuma ödülleri (altın).
     *
     * Kilometre taşlarından ayrı: taşlar herkese aynı, meydan okuma kullanıcının kayıt
     * sırasında kendi verdiği söz. Beş günlük söz hiçbir taşa denk gelmiyordu ve tutulan
     * bir taahhüt karşılıksız kalıyordu.
     *
     * DİKKAT: ikizi sunucuda (`STREAK_CHALLENGE_REWARDS`). Buradaki yalnızca gösterim.
     */
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
        // 30 altın, 60 anahtar, 90 altın, 120 anahtar… Merdiven 30'da bitseydi altmışıncı
        // günündeki çocuk için serinin bir karşılığı kalmazdı.
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

    /**
     * Ekranda gösterilecek taşlar: önce hak edilip toplanmamış olanlar, sonra sıradakiler.
     *
     * Toplanmışlar listelenmiyor — ekranda yer kaplayıp ilgiyi asıl işten (toplanacak ödül
     * ve sıradaki hedef) uzaklaştırırlardı.
     */
    fun visible(streak: Int, claimed: Set<Int>, count: Int = 3): List<Int> {
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

    /** Hak edilmiş ama toplanmamış taşlar — seri ekranındaki uyarı noktası için. */
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
