package com.example.app

import com.example.app.GlobalLessonData
import com.example.app.model.LessonItem

/**
 * Kupa modunda (globalPartId == 9) kullanıcının kupa skoruna ve zorluk seviyesine göre
 * soru parametrelerini belirler.
 *
 * Kupa aralıkları hardcoded olarak tanımlıdır. Her aralık için bir veya daha fazla [DigitConfig]
 * tanımlanabilir. Başlat butonuna basıldığında aktif aralıktan eşit olasılıkla rastgele bir
 * [DigitConfig] seçilir.
 *
 * [DigitConfig] içindeki [counts] ve [intervals] listeleri, zorluk seviyesine (0–4) göre
 * [selectValues] algoritmasıyla değer seçer.
 *
 * Kural değiştirmek için [RANGES] listesini düzenlemek yeterlidir.
 */
object CupRuleEngine {

    /**
     * Tek bir basamak konfigürasyonu.
     * @param digits    Basamak sayısı (1 = 1..9, 2 = 10..99, 3 = 100..999)
     * @param counts    Kullanıcıya gösterilecek sayı adedi seçenekleri (zorlukla artar)
     * @param intervals Sayılar arasındaki süre seçenekleri ms cinsinden (zorlukla azalır)
     *
     * Tek değerli aralıklar için tekli liste kullanılır: counts = listOf(3), intervals = listOf(2000)
     */
    data class DigitConfig(
        val digits: Int,
        val counts: List<Int>,
        val intervals: List<Long>,
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
     *
     * 1000..1499 aralığı zorluk seviyeleriyle tam liste destekler.
     * Diğer aralıklar şimdilik tek değerli listedir.
     */
    private val RANGES = listOf(
        CupRange(
            minCup = 0, maxCup = 499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(3), intervals = listOf(2000L)),
            ),
        ),
        CupRange(
            minCup = 500, maxCup = 999,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(3), intervals = listOf(1500L)),
                DigitConfig(digits = 2, counts = listOf(2), intervals = listOf(1500L)),
            ),
        ),
        CupRange(
            minCup = 1000, maxCup = 1099,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(8, 9, 10),         intervals = listOf(3000L, 2500L, 2000L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(6000L, 5000L, 4000L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(9000L, 8000L, 7000L, 6000L)),
            ),
        ),
        CupRange(
            minCup = 1100, maxCup = 1199,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(8, 9, 10),         intervals = listOf(2900L, 2400L, 1900L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(5800L, 4800L, 3800L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(8700L, 7700L, 6700L, 5700L)),
            ),
        ),
        CupRange(
            minCup = 1200, maxCup = 1299,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(8, 9, 10),         intervals = listOf(2800L, 2300L, 1800L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(5600L, 4600L, 3600L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(8400L, 7400L, 6400L, 5400L)),
            ),
        ),
        CupRange(
            minCup = 1300, maxCup = 1399,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(9, 10, 11),         intervals = listOf(2800L, 2300L, 1800L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(5600L, 4600L, 3600L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(8400L, 7400L, 6400L, 5400L)),
            ),
        ),
        CupRange(
            minCup = 1400, maxCup = 1499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(10, 11, 12),         intervals = listOf(2800L, 2300L, 1800L)),
                DigitConfig(digits = 2, counts = listOf(7, 8, 9),          intervals = listOf(5600L, 4600L, 3600L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(8400L, 7400L, 6400L, 5400L)),
            ),
        ),
        CupRange(
            minCup = 1500, maxCup = 1599,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(10, 11, 12),         intervals = listOf(2700L, 2200L, 1700L)),
                DigitConfig(digits = 2, counts = listOf(7, 8, 9),          intervals = listOf(5400L, 4400L, 3400L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(8100L, 7100L, 6100L, 5100L)),
            ),
        ),
        CupRange(
            minCup = 1600, maxCup = 1699,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(10, 11, 12),         intervals = listOf(2600L, 2100L, 1600L)),
                DigitConfig(digits = 2, counts = listOf(7, 8, 9),          intervals = listOf(5200L, 4200L, 3200L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(7800L, 6800L, 5800L, 4800L)),
            ),
        ),
        CupRange(
            minCup = 1700, maxCup = 1799,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(11, 12, 13),         intervals = listOf(2600L, 2100L, 1600L)),
                DigitConfig(digits = 2, counts = listOf(8, 9, 10),          intervals = listOf(5200L, 4200L, 3200L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(7800L, 6800L, 5800L, 4800L)),
            ),
        ),
        CupRange(
            minCup = 1800, maxCup = 1899,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(12, 13, 14),         intervals = listOf(2600L, 2100L, 1600L)),
                DigitConfig(digits = 2, counts = listOf(8, 9, 10),          intervals = listOf(5200L, 4200L, 3200L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(7800L, 6800L, 5800L, 4800L)),
            ),
        ),
        CupRange(
            minCup = 1900, maxCup = 1999,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(12, 13, 14),         intervals = listOf(2500L, 2000L, 1500L)),
                DigitConfig(digits = 2, counts = listOf(8, 9, 10),          intervals = listOf(5000L, 4000L, 3000L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(7500L, 6500L, 5500L, 4500L)),
            ),
        ),
        CupRange(
            minCup = 2000, maxCup = 2099,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(13, 14, 15),         intervals = listOf(2400L, 1900L, 1400L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(4800L, 3800L, 2800L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(7200L, 6200L, 5200L, 4200L)),
            ),
        ),
        CupRange(
            minCup = 2100, maxCup = 2199,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(14, 15, 16),         intervals = listOf(2400L, 1900L, 1400L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(4800L, 3800L, 2800L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(7200L, 6200L, 5200L, 4200L)),
            ),
        ),
        CupRange(
            minCup = 2200, maxCup = 2299,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(15, 16, 17),         intervals = listOf(2400L, 1900L, 1400L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(4800L, 3800L, 2800L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(7200L, 6200L, 5200L, 4200L)),
            ),
        ),
        CupRange(
            minCup = 2300, maxCup = 2399,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(15, 16, 17),         intervals = listOf(2300L, 1800L, 1300L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(4600L, 3600L, 2600L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(6900L, 5900L, 4900L, 3900L)),
            ),
        ),
        CupRange(
            minCup = 2400, maxCup = 2499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(15, 16, 17),         intervals = listOf(2200L, 1700L, 1200L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(4400L, 3400L, 2400L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(6600L, 5600L, 4600L, 3600L)),
            ),
        ),
        CupRange(
            minCup = 2400, maxCup = 2499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(2200L, 1700L, 1200L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(4400L, 3400L, 2400L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(6600L, 5600L, 4600L, 3600L)),
            ),
        ),
        CupRange(
            minCup = 6000, maxCup = Int.MAX_VALUE,
            configs = listOf(
                DigitConfig(digits = 3, counts = listOf(3), intervals = listOf(500L)),
            ),
        ),
    )

    //3 basamaklı 5 adımda 1 artacak.
    //2 basamaklı 4-3-3 adımda 1 artacak
    //1 basamaklı sürenin aynı kaldığında artacak.
    //Süre 2 düşecek 2 aynı kalacak.
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
     * Zorluk seviyesine (0–4) göre [DigitConfig]'teki count ve interval listelerinden değer seçer.
     *
     * - Seviye 0  → her iki listeden 0. index
     * - Seviye 4  → her iki listeden son index (en zor)
     * - Seviye 1–3 → başlangıçta her iki liste 0. indexte; her adımda count veya interval
     *                listelerinden biri rastgele seçilip 1 index arttırılır. Bir liste sona
     *                ulaştıysa diğeri arttırılır.
     *
     * @return Pair(count, intervalMs)
     */
    fun selectValues(config: DigitConfig, difficultyLevel: Int): Pair<Int, Long> {
        val level = difficultyLevel.coerceIn(0, 4)
        val countMax  = config.counts.size - 1
        val intervalMax = config.intervals.size - 1

        // Seviye 0: her zaman en kolay
        if (level == 0) return Pair(config.counts[0], config.intervals[0])

        // Seviye 4: her zaman en zor
        if (level == 4) return Pair(config.counts[countMax], config.intervals[intervalMax])

        // Seviye 1–3: rastgele adım arttırma
        var countIdx    = 0
        var intervalIdx = 0

        repeat(level) {
            val countAtMax    = countIdx    >= countMax
            val intervalAtMax = intervalIdx >= intervalMax

            when {
                countAtMax && intervalAtMax -> return@repeat   // her ikisi de sonda, dur
                countAtMax                  -> intervalIdx++    // count sonda → mecbur interval
                intervalAtMax               -> countIdx++       // interval sonda → mecbur count
                else                        -> {                // ikisi de ilerleyebilir → rastgele
                    if ((0..1).random() == 0) countIdx++ else intervalIdx++
                }
            }
        }

        return Pair(config.counts[countIdx], config.intervals[intervalIdx])
    }

    /**
     * Kupa skoruna ve zorluk seviyesine göre çalışma-zamanı [LessonItem] üretir.
     * Placeholder'ı (createLessonItems(9)'ın ilk elemanı) override eder.
     *
     * @param cupScore       Kullanıcının güncel kupa skoru
     * @param difficultyLevel SeekBar kademesi (0–4 → %0, %25, %50, %75, %100)
     */
    fun buildLessonItem(cupScore: Int, difficultyLevel: Int = 0): LessonItem {
        val config = resolve(cupScore)
        val level = difficultyLevel.coerceIn(0, 4)
        val (count, intervalMs) = selectValues(config, level)
        val placeholder = GlobalLessonData.createLessonItems(9).first()

        val winDelta = when (level) {
            0 -> 10
            1 -> 17
            2 -> 24
            3 -> 31
            else -> 38
        }

        val lossDelta = when (level) {
            0 -> 30
            1 -> 25
            2 -> 20
            3 -> 15
            else -> 10
        }

        return placeholder.copy(
            timePeriod    = intervalMs,
            cupDigitSize  = config.digits,
            cupNumberCount = count,
            cupDifficultyLevel = level,
            cupWinDelta = winDelta,
            cupLossDelta = lossDelta
        )
    }
}
