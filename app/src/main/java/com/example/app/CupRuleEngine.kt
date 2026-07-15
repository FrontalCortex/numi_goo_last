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
            minCup = 0, maxCup = 99,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(3, 4, 5),         intervals = listOf(3000L, 2750L, 2500L)),
            ),
        ),
        CupRange(
            minCup = 100, maxCup = 199,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(3, 4, 5),         intervals = listOf(2900L, 2650L, 2400L)),
            ),
        ),
        CupRange(
            minCup = 200, maxCup = 299,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(4, 5, 6),         intervals = listOf(2900L, 2650L, 2400L)),
            ),
        ),
        CupRange(
            minCup = 300, maxCup = 399,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(4, 5, 6),         intervals = listOf(2800L, 2550L, 2300L)),
            ),
        ),
        CupRange(
            minCup = 400, maxCup = 499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(5, 6, 7),         intervals = listOf(2800L, 2550L, 2300L)),
            ),
        ),
        CupRange(
            minCup = 500, maxCup = 599,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(6, 7, 8),         intervals = listOf(2800L, 2550L, 2300L)),
                DigitConfig(digits = 2, counts = listOf(4, 5, 6),          intervals = listOf(4200L, 3825L, 3450L)),
            ),
        ),
        CupRange(
            minCup = 600, maxCup = 699,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(6, 7, 8),         intervals = listOf(2700L, 2450L, 2200L)),
                DigitConfig(digits = 2, counts = listOf(4, 5, 6),          intervals = listOf(4050L, 3675L, 3300L)),
            ),
        ),
        CupRange(
            minCup = 700, maxCup = 799,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(7, 8, 9),         intervals = listOf(2700L, 2450L, 2200L)),
                DigitConfig(digits = 2, counts = listOf(5, 6, 7),          intervals = listOf(4050L, 3675L, 3300L)),
            ),
        ),
        CupRange(
            minCup = 800, maxCup = 899,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(7, 8, 9),         intervals = listOf(2600L, 2350L, 2100L)),
                DigitConfig(digits = 2, counts = listOf(5, 6, 7),          intervals = listOf(3900L, 3525L, 3150L)),
            ),
        ),
        CupRange(
            minCup = 900, maxCup = 999,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(7, 8, 9),         intervals = listOf(2500L, 2250L, 2000L)),
                DigitConfig(digits = 2, counts = listOf(5, 6, 7),          intervals = listOf(3750L, 3375L, 3000L)),
            ),
        ),
        CupRange(
            minCup = 1000, maxCup = 1099,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(8, 9, 10),         intervals = listOf(2500L, 2250L, 2000L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(3750L, 3375L, 3000L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(4999L, 4666L, 4333L, 4000L)),
            ),
        ),
        CupRange(
            minCup = 1100, maxCup = 1199,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(8, 9, 10),         intervals = listOf(2400L, 2150L, 1900L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(3600L, 3225L, 2850L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(4799L, 4466L, 4133L, 3800L)),
            ),
        ),
        CupRange(
            minCup = 1200, maxCup = 1299,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(8, 9, 10),         intervals = listOf(2300L, 2050L, 1800L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(3450L, 3075L, 2700L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(4599L, 4266L, 3933L, 3600L)),
            ),
        ),
        CupRange(
            minCup = 1300, maxCup = 1399,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(9, 10, 11),         intervals = listOf(2300L, 2050L, 1800L)),
                DigitConfig(digits = 2, counts = listOf(6, 7, 8),          intervals = listOf(3450L, 3075L, 2700L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(4599L, 4266L, 3933L, 3600L)),
            ),
        ),
        CupRange(
            minCup = 1400, maxCup = 1499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(10, 11, 12),         intervals = listOf(2300L, 2050L, 1800L)),
                DigitConfig(digits = 2, counts = listOf(7, 8, 9),          intervals = listOf(3450L, 3075L, 2700L)),
                DigitConfig(digits = 3, counts = listOf(4, 5),             intervals = listOf(4599L, 4266L, 3933L, 3600L)),
            ),
        ),
        CupRange(
            minCup = 1500, maxCup = 1599,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(10, 11, 12),         intervals = listOf(2200L, 1950L, 1700L)),
                DigitConfig(digits = 2, counts = listOf(7, 8, 9),          intervals = listOf(3300L, 2925L, 2550L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(4399L, 4066L, 3733L, 3400L)),
            ),
        ),
        CupRange(
            minCup = 1600, maxCup = 1699,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(10, 11, 12),         intervals = listOf(2100L, 1850L, 1600L)),
                DigitConfig(digits = 2, counts = listOf(7, 8, 9),          intervals = listOf(3150L, 2775L, 2400L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(4199L, 3866L, 3533L, 3200L)),
            ),
        ),
        CupRange(
            minCup = 1700, maxCup = 1799,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(11, 12, 13),         intervals = listOf(2100L, 1850L, 1600L)),
                DigitConfig(digits = 2, counts = listOf(8, 9, 10),          intervals = listOf(3150L, 2775L, 2400L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(4199L, 3866L, 3533L, 3200L)),
            ),
        ),
        CupRange(
            minCup = 1800, maxCup = 1899,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(12, 13, 14),         intervals = listOf(2100L, 1850L, 1600L)),
                DigitConfig(digits = 2, counts = listOf(8, 9, 10),          intervals = listOf(3150L, 2775L, 2400L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(4199L, 3866L, 3533L, 3200L)),
            ),
        ),
        CupRange(
            minCup = 1900, maxCup = 1999,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(12, 13, 14),         intervals = listOf(2000L, 1750L, 1500L)),
                DigitConfig(digits = 2, counts = listOf(8, 9, 10),          intervals = listOf(3000L, 2625L, 2250L)),
                DigitConfig(digits = 3, counts = listOf(5, 6),             intervals = listOf(3999L, 3666L, 3333L, 3000L)),
            ),
        ),
        CupRange(
            minCup = 2000, maxCup = 2099,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(13, 14, 15),         intervals = listOf(1900L, 1650L, 1400L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(2850L, 2475L, 2100L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(3799L, 3466L, 3133L, 2800L)),
            ),
        ),
        CupRange(
            minCup = 2100, maxCup = 2199,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(14, 15, 16),         intervals = listOf(1900L, 1650L, 1400L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(2850L, 2475L, 2100L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(3799L, 3466L, 3133L, 2800L)),
            ),
        ),
        CupRange(
            minCup = 2200, maxCup = 2299,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(15, 16, 17),         intervals = listOf(1900L, 1650L, 1400L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(2850L, 2475L, 2100L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(3799L, 3466L, 3133L, 2800L)),
            ),
        ),
        CupRange(
            minCup = 2300, maxCup = 2399,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(15, 16, 17),         intervals = listOf(1800L, 1550L, 1300L)),
                DigitConfig(digits = 2, counts = listOf(9, 10, 11),          intervals = listOf(2700L, 2325L, 1950L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(3599L, 3266L, 2933L, 2600L)),
            ),
        ),
        CupRange(
            minCup = 2400, maxCup = 2499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(15, 16, 17),         intervals = listOf(1700L, 1450L, 1200L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(2550L, 2175L, 1800L)),
                DigitConfig(digits = 3, counts = listOf(6, 7),             intervals = listOf(3399L, 3066L, 2733L, 2400L)),
            ),
        ),
        CupRange(
            minCup = 2400, maxCup = 2499,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1700L, 1450L, 1200L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(2550L, 2175L, 1800L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(3399L, 3066L, 2733L, 2400L)),
            ),
        ),
        CupRange(
            minCup = 2500, maxCup = 2599,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1600L, 1350L, 1100L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(2400L, 2025L, 1650L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(3199L, 2866L, 2533L, 2200L)),
            ),
        ),
        CupRange(
            minCup = 2600, maxCup = 2699,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1500L, 1250L, 1000L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(2250L, 1875L, 1500L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(2999L, 2666L, 2333L, 2000L)),
            ),
        ),
        CupRange(
            minCup = 2700, maxCup = 2799,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1400L, 1150L, 900L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(2100L, 1725L, 1350L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(2799L, 2466L, 2133L, 1800L)),
            ),
        ),
        CupRange(
            minCup = 2800, maxCup = 2899,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1300L, 1050L, 800L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(1950L, 1575L, 1200L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(2599L, 2266L, 1933L, 1600L)),
            ),
        ),
        CupRange(
            minCup = 2900, maxCup = 2999,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1200L, 950L, 700L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(1800L, 1425L, 1050L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(2400L, 2066L, 1733L, 1400L)),
            ),
        ),
        CupRange(
            minCup = 3000, maxCup = 3099,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1100L, 850L, 600L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(1650L, 1275L, 900L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(2199L, 1866L, 1533L, 1200L)),
            ),
        ),
        CupRange(
            minCup = 3100, maxCup = 3199,
            configs = listOf(
                DigitConfig(digits = 1, counts = listOf(16, 17, 18),         intervals = listOf(1000L, 750L, 500L)),
                DigitConfig(digits = 2, counts = listOf(10, 11, 12),          intervals = listOf(1500L, 1125L, 750L)),
                DigitConfig(digits = 3, counts = listOf(7, 8),             intervals = listOf(1999L, 1666L, 1333L, 1000L)),
            ),
        ),
    )

    /**
     * Körleme kupa modu (card4View) için kural tablosu.
     * RANGES ile aynı format; istediğiniz gibi farklı sayı/süre değerleri tanımlayabilirsiniz.
     * Şu an RANGES ile birebir aynı değerleri içerir; bu listeden bağımsız olarak düzenleyebilirsiniz.
     */
    private val BLINDING_RANGES = emptyList<CupRange>()

    /**
     * Verilen [ranges] listesinden kupa skoruna göre DigitConfig seçer.
     * 3200+ kupa için açık uçlu ölçekleme aynı mantıkla çalışır.
     *
     * @param cupScore Kullanıcının güncel kupa skoru
     * @param ranges   Kullanılacak kural tablosu (RANGES veya BLINDING_RANGES)
     */
    fun resolve(cupScore: Int, ranges: List<CupRange> = RANGES): DigitConfig {
        // Körleme kupa modu için 0-999 arası dinamik hesaplama (900 baz alınarak aşağı inilir)
        if (ranges === BLINDING_RANGES && cupScore in 0..999) {
            val step = (999 - cupScore) / 100
            val countDrop1d = step / 3

            val c1 = DigitConfig(
                digits = 1,
                counts = listOf(maxOf(1, 6 - countDrop1d)),
                intervals = listOf(2900L + step * 100L, 2650L + step * 100L, 2400L + step * 100L)
            )
            return c1
        }

        // Körleme kupa modu için 1000-1999 arası dinamik hesaplama (2000 baz alınarak aşağı inilir)
        if (ranges === BLINDING_RANGES && cupScore in 1000..1999) {
            val step = (2099 - cupScore) / 100
            val countDrop1d = step / 2
            val countDrop2d = step / 3

            val c1 = DigitConfig(
                digits = 1,
                counts = listOf(10 - countDrop1d, 11 - countDrop1d, 13 - countDrop1d),
                intervals = listOf(1900L + step * 100L, 1650L + step * 100L, 1400L + step * 100L)
            )
            val c2 = DigitConfig(
                digits = 2,
                counts = listOf(5 - countDrop2d, 6 - countDrop2d),
                intervals = listOf(3800L + step * 200L, 3300L + step * 200L, 2800L + step * 200L)
            )
            return listOf(c1, c2).random()
        }

        // Körleme kupa modu için 2000-2999 arası dinamik hesaplama (3000 baz alınarak aşağı inilir)
        if (ranges === BLINDING_RANGES && cupScore in 2000..2999) {
            val step = (3099 - cupScore) / 100
            val countDrop1d2d = step / 2
            val countDrop3d = step / 5

            val c1 = DigitConfig(
                digits = 1,
                counts = listOf(14 - countDrop1d2d, 16 - countDrop1d2d, 18 - countDrop1d2d),
                intervals = listOf(700L + step * 100L, 450L + step * 100L, 200L + step * 100L)
            )
            val c2 = DigitConfig(
                digits = 2,
                counts = listOf(10 - countDrop1d2d, 11 - countDrop1d2d, 12 - countDrop1d2d),
                intervals = listOf(1800L + step * 200L, 1300L + step * 200L, 800L + step * 200L)
            )
            val c3 = DigitConfig(
                digits = 3,
                counts = listOf(4 - countDrop3d),
                intervals = listOf(4500L + step * 400L, 3875L + step * 400L, 3250L + step * 400L, 2625L + step * 400L, 2000L + step * 400L)
            )
            return listOf(c1, c2, c3).random()
        }

        // Körleme kupa modu için 3000-sonsuz arası dinamik hesaplama (açık uçlu ölçekleme)
        if (ranges === BLINDING_RANGES && cupScore >= 3000) {
            val step = (cupScore - 3000) / 100
            val countInc1d2d = step / 4
            val countInc3d = step / 8
            val countInc4d = step / 5

            val c1 = DigitConfig(
                digits = 1,
                counts = listOf(14 + countInc1d2d, 18 + countInc1d2d, 20 + countInc1d2d),
                intervals = listOf(
                    maxOf(80L, 800L - step * 25L),
                    maxOf(80L, 550L - step * 25L),
                    maxOf(80L, 300L - step * 25L)
                )
            )
            val c2 = DigitConfig(
                digits = 2,
                counts = listOf(8 + countInc1d2d, 10 + countInc1d2d, 12 + countInc1d2d),
                intervals = listOf(
                    maxOf(200L, 1800L - step * 50L),
                    maxOf(200L, 1300L - step * 50L),
                    maxOf(200L, 800L - step * 50L)
                )
            )
            val c3 = DigitConfig(
                digits = 3,
                counts = listOf(5 + countInc3d, 6 + countInc3d),
                intervals = listOf(
                    maxOf(400L, 3875L - step * 100L),
                    maxOf(400L, 3250L - step * 100L),
                    maxOf(400L, 2625L - step * 100L),
                    maxOf(400L, 2000L - step * 100L)
                )
            )
            val c4 = DigitConfig(
                digits = 4,
                counts = listOf(3 + countInc4d),
                intervals = listOf(
                    maxOf(400L, 6000L - step * 150L),
                    maxOf(400L, 5500L - step * 150L),
                    maxOf(400L, 5000L - step * 150L),
                    maxOf(400L, 4500L - step * 150L),
                    maxOf(400L, 4000L - step * 150L)
                )
            )
            return listOf(c1, c2, c3, c4).random()
        }

        // Standart kupa modu için 3200-sonsuz arası dinamik hesaplama (açık uçlu ölçekleme)
        if (cupScore >= 3200) {
            val step = (cupScore - 3200) / 100
            val countInc1d2d = step / 4
            val countInc3d = step / 3
            val countInc4d = step / 4

            val c1 = DigitConfig(
                digits = 1,
                counts = listOf(15 + countInc1d2d, 18 + countInc1d2d, 21 + countInc1d2d),
                intervals = listOf(
                    maxOf(80L, 1000L - step * 50L),
                    maxOf(80L, 750L - step * 50L),
                    maxOf(80L, 500L - step * 50L)
                )
            )
            val c2 = DigitConfig(
                digits = 2,
                counts = listOf(9 + countInc1d2d, 11 + countInc1d2d, 13 + countInc1d2d),
                intervals = listOf(
                    maxOf(100L, 1500L - step * 75L),
                    maxOf(100L, 1125L - step * 75L),
                    maxOf(100L, 750L - step * 75L)
                )
            )
            val c3 = DigitConfig(
                digits = 3,
                counts = listOf(7 + countInc3d, 8 + countInc3d),
                intervals = listOf(
                    maxOf(100L, 1999L - step * 100L),
                    maxOf(100L, 1666L - step * 100L),
                    maxOf(100L, 1333L - step * 100L),
                    maxOf(100L, 1000L - step * 100L)
                )
            )
            val c4 = DigitConfig(
                digits = 4,
                counts = listOf(3 + countInc4d),
                intervals = listOf(
                    maxOf(100L, 3500L - step * 150L),
                    maxOf(100L, 3000L - step * 150L),
                    maxOf(100L, 2500L - step * 150L),
                    maxOf(100L, 2000L - step * 150L),
                    maxOf(100L, 1500L - step * 150L)
                )
            )
            return listOf(c1, c2, c3, c4).random()
        }

        val range = ranges.firstOrNull { cupScore in it.minCup..it.maxCup }
            ?: ranges.first { it.minCup == 3100 } // Fallback
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
    fun buildLessonItem(cupScore: Int, difficultyLevel: Int = 0, isBlinding: Boolean = false, isExtraction: Boolean = false): LessonItem {
        val ranges = if (isBlinding) BLINDING_RANGES else RANGES
        val config = resolve(cupScore, ranges)
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
            cupLossDelta = lossDelta,
            isBlinding = if (isBlinding) true else null,
            isExtraction = if (isExtraction) true else null
        )
    }

    /**
     * Çarpma kupa modu için kupa skoruna göre çarpma sorusu üretir.
     * Kupa aralığına göre 1. ve 2. sayının basamak sayısı ve kullanılabilecek rakamlar belirlenir.
     */
    data class MultiplicationConfig(
        val minCup: Int,
        val maxCup: Int,
        val firstDigitCount: Int,
        val firstAllowedDigits: List<Int>,
        val secondDigitCount: Int,
        val secondAllowedDigits: List<Int>
    )
    data class BlindingMultiplicationConfig(
        val minCup: Int,
        val maxCup: Int,
        val firstDigitCount: Int,
        val firstAllowedDigits: List<Int>,
        val secondDigitCount: Int,
        val secondAllowedDigits: List<Int>
    )

    private val MULTIPLICATION_RANGES = listOf(
        MultiplicationConfig(1900, 999999, 3, listOf(0,1,2,3,4,5,6,7,8,9), 2, listOf(0,1,2,3,4,5,6,7,8,9)),
        MultiplicationConfig(1800, 1899, 3, listOf(0,6,7,8,9), 2, listOf(0,6,7,8,9)),
        MultiplicationConfig(1700, 1799, 3, listOf(0,5,6,7,8), 2, listOf(0,5,6,7,8)),
        MultiplicationConfig(1600, 1699, 3, listOf(4,5,6,7), 2, listOf(4,5,6,7)),
        MultiplicationConfig(1500, 1599, 3, listOf(3,4,5,6), 2, listOf(3,4,5,6)),
        MultiplicationConfig(1400, 1499, 3, listOf(2,3,4,5), 2, listOf(2,3,4,5)),
        MultiplicationConfig(1300, 1399, 3, listOf(1,2,3), 2, listOf(1,2,3)),
        MultiplicationConfig(1200, 1299, 2, listOf(0,6,7,8,9), 2, listOf(6,7,8,9)),
        MultiplicationConfig(1100, 1199, 2, listOf(0,5,6,7,8), 2, listOf(5,6,7,8)),
        MultiplicationConfig(1000, 1099, 2, listOf(4,5,6,7), 2, listOf(4,5,6,7)),
        MultiplicationConfig(900, 999, 2, listOf(3,4,5,6), 2, listOf(3,4,5,6)),
        MultiplicationConfig(800, 899, 2, listOf(2,3,4,5), 2, listOf(2,3,4,5)),
        MultiplicationConfig(700, 799, 2, listOf(1,2,3), 2, listOf(1,2,3)),
        MultiplicationConfig(600, 699, 2, listOf(0,6,7,8,9), 1, listOf(6,7,8,9)),
        MultiplicationConfig(500, 599, 2, listOf(0,5,6,7,8), 1, listOf(5,6,7,8)),
        MultiplicationConfig(400, 499, 2, listOf(4,5,6,7), 1, listOf(4,5,6,7)),
        MultiplicationConfig(300, 399, 2, listOf(3,4,5,6), 1, listOf(3,4,5,6)),
        MultiplicationConfig(200, 299, 2, listOf(1,2,3,4), 1, listOf(2,3,4)),
        MultiplicationConfig(100, 199, 1, listOf(6,7,8,9), 1, listOf(6,7,8,9)),
        MultiplicationConfig(0, 99, 1, listOf(2,3,4,5), 1, listOf(2,3,4,5)),

    )
    private val BLINDING_MULTIPLICATION_RANGES = listOf(
        BlindingMultiplicationConfig(3000, 999999, 4, listOf(0,1,2,3,4,5,6,7,8,9), 3, listOf(0,1,2,3,4,5,6,7,8,9)),
        BlindingMultiplicationConfig(3000, 3099, 4, listOf(0,6,7,8,9), 3, listOf(0,6,7,8,9)),
        BlindingMultiplicationConfig(2900, 2999, 4, listOf(0,5,6,7,8), 3, listOf(0,5,6,7,8)),
        BlindingMultiplicationConfig(2800, 2899, 4, listOf(4,5,6,7), 3, listOf(4,5,6,7)),
        BlindingMultiplicationConfig(2700, 2799, 4, listOf(3,4,5,6), 3, listOf(3,4,5,6)),
        BlindingMultiplicationConfig(2600, 2699, 4, listOf(2,3,4,5), 3, listOf(2,3,4,5)),
        BlindingMultiplicationConfig(2500, 2599, 4, listOf(1,2,3), 3, listOf(1,2,3)),
        BlindingMultiplicationConfig(2400, 2499, 3, listOf(0,6,7,8,9), 3, listOf(0,6,7,8,9)),
        BlindingMultiplicationConfig(2300, 2399, 3, listOf(0,5,6,7,8), 3, listOf(0,5,6,7,8)),
        BlindingMultiplicationConfig(2200, 2299, 3, listOf(4,5,6,7), 3, listOf(4,5,6,7)),
        BlindingMultiplicationConfig(2100, 2199, 3, listOf(3,4,5,6), 3, listOf(3,4,5,6)),
        BlindingMultiplicationConfig(2000, 2099, 3, listOf(2,3,4,5), 3, listOf(2,3,4,5)),
        BlindingMultiplicationConfig(1900, 1999, 3, listOf(1,2,3), 3, listOf(1,2,3)),
        BlindingMultiplicationConfig(1800, 1899, 3, listOf(0,6,7,8,9), 2, listOf(0,6,7,8,9)),
        BlindingMultiplicationConfig(1700, 1799, 3, listOf(0,5,6,7,8), 2, listOf(0,5,6,7,8)),
        BlindingMultiplicationConfig(1600, 1699, 3, listOf(4,5,6,7), 2, listOf(4,5,6,7)),
        BlindingMultiplicationConfig(1500, 1599, 3, listOf(3,4,5,6), 2, listOf(3,4,5,6)),
        BlindingMultiplicationConfig(1400, 1499, 3, listOf(2,3,4,5), 2, listOf(2,3,4,5)),
        BlindingMultiplicationConfig(1300, 1399, 3, listOf(1,2,3), 2, listOf(1,2,3)),
        BlindingMultiplicationConfig(1200, 1299, 2, listOf(0,6,7,8,9), 2, listOf(6,7,8,9)),
        BlindingMultiplicationConfig(1100, 1199, 2, listOf(0,5,6,7,8), 2, listOf(5,6,7,8)),
        BlindingMultiplicationConfig(1000, 1099, 2, listOf(4,5,6,7), 2, listOf(4,5,6,7)),
        BlindingMultiplicationConfig(900, 999, 2, listOf(3,4,5,6), 2, listOf(3,4,5,6)),
        BlindingMultiplicationConfig(800, 899, 2, listOf(2,3,4,5), 2, listOf(2,3,4,5)),
        BlindingMultiplicationConfig(700, 799, 2, listOf(1,2,3), 2, listOf(1,2,3)),
        BlindingMultiplicationConfig(600, 699, 2, listOf(0,6,7,8,9), 1, listOf(6,7,8,9)),
        BlindingMultiplicationConfig(500, 599, 2, listOf(0,5,6,7,8), 1, listOf(5,6,7,8)),
        BlindingMultiplicationConfig(400, 499, 2, listOf(4,5,6,7), 1, listOf(4,5,6,7)),
        BlindingMultiplicationConfig(300, 399, 2, listOf(3,4,5,6), 1, listOf(3,4,5,6)),
        BlindingMultiplicationConfig(200, 299, 2, listOf(1,2,3,4), 1, listOf(2,3,4)),
        BlindingMultiplicationConfig(100, 199, 1, listOf(6,7,8,9), 1, listOf(6,7,8,9)),
        BlindingMultiplicationConfig(0, 99, 1, listOf(2,3,4,5), 1, listOf(2,3,4,5)),

    )

    fun generateMultiplicationQuestion(cupScore: Int, isBlinding: Boolean = false): MathOperation {
        val (firstDigitCount, firstAllowedDigits, secondDigitCount, secondAllowedDigits) = if (isBlinding) {
            val config = BLINDING_MULTIPLICATION_RANGES.firstOrNull { cupScore in it.minCup..it.maxCup }
                ?: BLINDING_MULTIPLICATION_RANGES.last()
            listOf(config.firstDigitCount, config.firstAllowedDigits, config.secondDigitCount, config.secondAllowedDigits)
        } else {
            val config = MULTIPLICATION_RANGES.firstOrNull { cupScore in it.minCup..it.maxCup }
                ?: MULTIPLICATION_RANGES.last()
            listOf(config.firstDigitCount, config.firstAllowedDigits, config.secondDigitCount, config.secondAllowedDigits)
        }

        fun buildNumber(digitCount: Int, allowedDigits: List<Int>): Int {
            val sb = StringBuilder()
            for (i in 0 until digitCount) {
                var digit = allowedDigits.random()
                // En büyük basamak (ilk rakam) 0 olmamalı (eğer basamak sayısı 1'den büyükse)
                if (i == 0 && digitCount > 1 && digit == 0) {
                    val nonZeroDigits = allowedDigits.filter { it != 0 }
                    if (nonZeroDigits.isNotEmpty()) {
                        digit = nonZeroDigits.random()
                    }
                }
                sb.append(digit)
            }
            return sb.toString().toInt()
        }

        val first = buildNumber(firstDigitCount as Int, firstAllowedDigits as List<Int>)
        val second = buildNumber(secondDigitCount as Int, secondAllowedDigits as List<Int>)
        return MathOperation(first, "x", second)
    }
}
