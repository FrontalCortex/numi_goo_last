package com.example.app

import com.example.app.GlobalLessonData
import com.example.app.model.LessonItem

/**
 * Kupa modunda (globalPartId == 9) kullanıcının kupa skoruna göre soru parametrelerini belirler.
 *
 * Kupa aralıkları hardcoded olarak tanımlıdır. Her aralık için bir veya daha fazla [DigitConfig]
 * tanımlanabilir. Başlat butonuna basıldığında aktif aralıktan eşit olasılıkla rastgele bir
 * [DigitConfig] seçilir.
 *
 * Kural değiştirmek için [RANGES] listesini düzenlemek yeterlidir.
 */
object CupRuleEngine {

    /**
     * Tek bir basamak konfigürasyonu.
     * @param digits  Basamak sayısı (1 = 1..9, 2 = 10..99, 3 = 100..999)
     * @param count   Kullanıcıya gösterilecek sayı adedi
     * @param intervalMs Sayılar arasındaki süre (milisaniye)
     */
    data class DigitConfig(
        val digits: Int,
        val count: Int,
        val intervalMs: Long,
    )

    /** Bir kupa puan aralığı ve bu aralıktaki aktif basamak konfigürasyonları. */
    data class CupRange(
        val minCup: Int,
        val maxCup: Int,
        val configs: List<DigitConfig>,
    )

    /**
     * Kupa aralığı → kural tablosu.
     * Aralıklar birbirini kapsamamalı; listedeki ilk eşleşen aralık kullanılır.
     * En son aralık "2000+" gibi açık uçlu olabilir (maxCup = Int.MAX_VALUE).
     */
    private val RANGES = listOf(
        CupRange(
            minCup = 0, maxCup = 499,
            configs = listOf(
                DigitConfig(digits = 1, count = 3, intervalMs = 2000),
            ),
        ),
        CupRange(
            minCup = 500, maxCup = 999,
            configs = listOf(
                DigitConfig(digits = 1, count = 3, intervalMs = 1500),
                DigitConfig(digits = 2, count = 2, intervalMs = 1500),
            ),
        ),
        CupRange(
            minCup = 1000, maxCup = 1499,
            configs = listOf(
                DigitConfig(digits = 1, count = 3, intervalMs = 1000),
                DigitConfig(digits = 2, count = 2, intervalMs = 1000),
                DigitConfig(digits = 3, count = 2, intervalMs = 500),
            ),
        ),
        CupRange(
            minCup = 1500, maxCup = 1999,
            configs = listOf(
                DigitConfig(digits = 2, count = 3, intervalMs = 800),
                DigitConfig(digits = 3, count = 2, intervalMs = 500),
            ),
        ),
        CupRange(
            minCup = 2000, maxCup = Int.MAX_VALUE,
            configs = listOf(
                DigitConfig(digits = 3, count = 3, intervalMs = 500),
            ),
        ),
    )

    /**
     * Verilen kupa skoruna karşılık gelen kural aralığından eşit olasılıkla bir [DigitConfig] seçer.
     * Eşleşen aralık bulunamazsa son aralık kullanılır.
     */
    fun resolve(cupScore: Int): DigitConfig {
        val range = RANGES.firstOrNull { cupScore in it.minCup..it.maxCup }
            ?: RANGES.last()
        return range.configs.random()
    }

    /**
     * Kupa skoruna göre çalışma-zamanı [LessonItem] üretir.
     * Placeholder'ı (createLessonItems(9)'ın ilk elemanı) override eder.
     */
    fun buildLessonItem(cupScore: Int): LessonItem {
        val config = resolve(cupScore)
        val placeholder = GlobalLessonData.createLessonItems(9).first()
        return placeholder.copy(
            timePeriod = config.intervalMs,
            cupDigitSize = config.digits,
            cupNumberCount = config.count,
        )
    }
}
