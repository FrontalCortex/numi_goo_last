// Kaynak: app/src/main/java/com/example/app/GlobalValues.kt (Android) — yalnızca saf sayısal
// yardımcılar: randomNumberChangeToString, randomUniqueNumberStrings, digitRangeMin, digitRangeMax,
// generateRandomNumber. Arayüz durumu (lessonStep, pending* alanları vb.) ve Context/Gson/File'a
// bağlı kısımlar (loadDownloadedMediaCache, persistDownloadedMediaCache) bu pakete alınmadı.
// Fark: `.random()` / `.shuffle()` çağrıları enjekte edilebilir [RastgeleKaynagi.rastgele] ile yapılır
// (varsayılan kotlin.random.Random.Default = Android'deki davranış).
package com.sorobit.shared.matematik

import kotlin.math.pow
import kotlin.random.Random

object GlobalValues {
    // iOS kopyasına özgü: rastgelelik tek bir enjekte edilebilir kaynaktan gelir (bkz. RastgeleKaynagi).
    private val rastgele: Random get() = RastgeleKaynagi.rastgele

    fun randomNumberChangeToString(digitCount: Int): String {
        return randomUniqueNumberStrings(digitCount, 1).single()
    }

    /**
     * [digitCount] basamaklı aralıktan [count] adet sayı string'i üretir.
     * Havuz yeterliyse hepsi birbirinden farklı; [count] havuzdan büyükse önce tüm değerler
     * bir kez (karışık), kalanlar rastgele (tekrarlı olabilir).
     */
    fun randomUniqueNumberStrings(digitCount: Int, count: Int): List<String> {
        if (count <= 0) return emptyList()
        val min = digitRangeMin(digitCount)
        val max = digitRangeMax(digitCount)
        val pool = (min..max).toMutableList()
        if (count <= pool.size) {
            pool.shuffle(rastgele)
            return pool.take(count).map { it.toString() }
        }
        pool.shuffle(rastgele)
        val result = pool.map { it.toString() }.toMutableList()
        while (result.size < count) {
            result.add((min..max).random(rastgele).toString())
        }
        result.shuffle(rastgele)
        return result
    }

    private fun digitRangeMin(digitCount: Int): Int =
        10.0.pow((digitCount - 1).coerceAtLeast(0)).toInt()

    private fun digitRangeMax(digitCount: Int): Int =
        10.0.pow(digitCount).toInt() - 1

    fun generateRandomNumber(digitCount: Int): Int {
        if (digitCount < 1) return 0
        return (digitRangeMin(digitCount)..digitRangeMax(digitCount)).random(rastgele)
    }
}
