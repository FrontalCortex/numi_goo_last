// KAYNAK: app/src/main/java/com/example/app/MapFragment.kt — companion object içindeki
// getLessonOperations, resolveLessonOperationsId, resolveLessonOperationsBlindingId ve
// getLessonOperationsBlinding (Android dosyasında satır 57-1135).
//
// Bu dosya tek seferlik bir betikle, mekanik dönüştürmeyle üretildi: fonksiyon gövdeleri
// Android'dekiyle KARAKTERİ KARAKTERİNE aynıdır (yalnızca bir girinti düzeyi azaltıldı, satır sonu
// boşlukları kırpıldı). Dal sırası, yorum satırına alınmış dallar ve tekrar eden değerler aynen korunur.
//
// Android'den bilinçli farklar:
//  - Android'de bu fonksiyonlar `MapFragment.Companion` üyesidir; Fragment ortak koda taşınamadığı için
//    burada [LessonOperations] nesnesinde durur (çağrı: `LessonOperations.getLessonOperations(id)`).
//  - Rastgelelik [com.sorobit.shared.matematik.RastgeleKaynagi] üzerinden gelir (üreteçler öyle taşındı);
//    varsayılanı tohumsuzdur, yani Android'deki davranıştır.
package com.sorobit.shared.dersadim

import com.sorobit.shared.matematik.GlobalValues.randomNumberChangeToString
import com.sorobit.shared.matematik.GlobalValues.randomUniqueNumberStrings
import com.sorobit.shared.matematik.MathOperation
import com.sorobit.shared.matematik.MathOperationGenerator
import com.sorobit.shared.matematik.MathOperationGenerator.generateRandomMathOperation1
import com.sorobit.shared.matematik.MathOperationGenerator.generateRelatedNumbers2

/** Ders adımı (lessonOperationsId) → soru listesi tabloları. */
object LessonOperations {
    fun getLessonOperations(lessonId: Int): List<MathOperation> {
        return when (lessonId) {
            1 -> listOf(
                MathOperation(null,"2", null),
                MathOperation(null,"5", null),
                MathOperation(null,"3", null),
                MathOperation(null,"8", null),
                MathOperation(null,"4", null),
                MathOperation(null,"1", null),
                MathOperation(null,"9", null),
                MathOperation(null,"6", null),
                MathOperation(null,"7", null),
                MathOperation(null,"8", null)
            )
            2 -> randomUniqueNumberStrings(1, 7).map { n ->
                MathOperation(null, n, null)
            }
            1000 -> randomUniqueNumberStrings(2, 7).map { n ->
                MathOperation(null, n, null)
            }
            1003 -> listOf(
                MathOperation(null,randomNumberChangeToString(3), null),
                MathOperation(null,randomNumberChangeToString(4), null),
                MathOperation(null,randomNumberChangeToString(5), null),
                MathOperation(null,randomNumberChangeToString(4), null),
                MathOperation(null,randomNumberChangeToString(3), null),
                MathOperation(null,randomNumberChangeToString(4), null),
                MathOperation(null,randomNumberChangeToString(5), null),
                MathOperation(null,randomNumberChangeToString(4), null),
            )
            1005 -> listOf(
                1 to 2,
                2 to 2,
                3 to 3,
                4 to 2,
                5 to 3,
            ).flatMap { (digitCount, count) ->
                randomUniqueNumberStrings(digitCount, count).map { n ->
                    MathOperation(null, n, null)
                }
            }
            4 -> listOf(MathOperation(4, "+", 5)) +
                    MathOperationGenerator.generateRelatedNumbersList(6, 1, 1)
            5 ->  MathOperationGenerator.generateRelatedNumbersList(8, 1, 1)
            1007 -> listOf(
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),

            )
            1010 -> MathOperationGenerator.generateRelatedNumbersList(6, 3, 3)

            1013 -> listOf(
                MathOperationGenerator.generateRelatedNumbers(4, 4),
                MathOperationGenerator.generateRelatedNumbers(5, 5),
                MathOperationGenerator.generateRelatedNumbers(4, 4),
                MathOperationGenerator.generateRelatedNumbers(5, 5),
                MathOperationGenerator.generateRelatedNumbers(4, 4),
                MathOperationGenerator.generateRelatedNumbers(5, 5),
            )
            7 -> listOf(
                MathOperationGenerator.generateRelatedNumbers0(1, 1),
                MathOperationGenerator.generateRelatedNumbers0(1, 1),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(2, 2),
                MathOperationGenerator.generateRelatedNumbers(3, 3),
                MathOperationGenerator.generateRelatedNumbers(3, 3),
                MathOperationGenerator.generateRelatedNumbers(3, 3),
                MathOperationGenerator.generateRelatedNumbers(4, 4),
                MathOperationGenerator.generateRelatedNumbers(4, 4),
                MathOperationGenerator.generateRelatedNumbers(4, 4),
                MathOperationGenerator.generateRelatedNumbers(5, 5),
                MathOperationGenerator.generateRelatedNumbers(5, 5),
                MathOperationGenerator.generateRelatedNumbers(5, 5),
                )
            8-> listOf(
                MathOperation(3,"+", 4),
                MathOperation(2,"+", 3),
                MathOperation(4,"+", 1),
                MathOperation(3,"+", 2),
                MathOperation(1,"+", 4),
                MathOperation(4,"+", 2),
                MathOperation(2,"+", 3),
                MathOperation(3,"+", 3))
            9-> listOf(
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRandomMathOperation1()
            )
            12 -> listOf(
                generateRelatedNumbers2(2, 3),
                generateRelatedNumbers2(2, 2),
                generateRelatedNumbers2(3, 2),
                generateRelatedNumbers2(2, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(2, 2),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 2),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(2, 2),
                )
            13 -> listOf(
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                )
            16 -> listOf(
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(4, 4)
            )
            17 -> listOf(
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(5, 4),
                generateRelatedNumbers2(4, 4),
                generateRelatedNumbers2(4, 5),
                generateRelatedNumbers2(5, 5),
                generateRelatedNumbers2(4, 4),
                )
            19 -> listOf(
                generateRandomMathOperation1(),
                generateRandomMathOperation1(),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 3),
                generateRelatedNumbers2(3, 4),
                generateRelatedNumbers2(4, 3),
                generateRelatedNumbers2(5, 5),
                generateRelatedNumbers2(5, 4),
                generateRelatedNumbers2(5, 5),
            )
            20 -> listOf(
                MathOperation(7,"+", 3),
                MathOperation(8,"+", 2),
                MathOperation(9,"+", 2),
                MathOperation(6,"+", 4),
                MathOperation(7,"+", 5),
                MathOperation(9,"+", 1),
                MathOperation(80,"+", 50),
                MathOperation(17,"+", 4),
                MathOperation(19,"+", 1),
                MathOperation(29,"+", 3),
                MathOperation(3,"+", 2),
                MathOperation(4,"+", 4))
            21 -> MathOperationGenerator.generateMathOperationList(9)
            24 -> listOf(
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
                MathOperationGenerator.generateMathOperation2(),
            )
            28 -> listOf(
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
            )
            32 -> listOf(
                MathOperationGenerator.generateMathOperationWithDigits(4,4),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(4,4),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
            )
            36 -> listOf(
                MathOperationGenerator.generateMathOperation(),
                MathOperationGenerator.generateMathOperation(),
                MathOperationGenerator.generateMathOperationWithDigits(2,2),
                MathOperationGenerator.generateMathOperationWithDigits(2,2),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
                MathOperationGenerator.generateMathOperationWithDigits(3,3),
                MathOperationGenerator.generateMathOperationWithDigits(4,4),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
                MathOperationGenerator.generateMathOperationWithDigits(4,4),
                MathOperationGenerator.generateMathOperationWithDigits(4,3),
                MathOperationGenerator.generateMathOperationWithDigits(4,4)
                )
            37 -> listOf(
                MathOperation(4, "+", 6),
                MathOperation(4, "+", 9),
                MathOperation(17, "+", 8),
                MathOperation(3, "+", 7),
                MathOperation(3, "+", 8),
                MathOperation(4, "+", 7),
                MathOperation(4, "+", 6),
                MathOperation(2, "+", 8),
                MathOperation(8, "+", 9)
            )
            38 -> MathOperationGenerator.generateMathOperationList3(8)
            41 -> MathOperationGenerator.generateMathOperationList4(8)
            44 -> listOf(
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                MathOperationGenerator.generateMathOperationWithDigits2(3,3),
            )
            48 -> listOf(
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
            )
            52 -> listOf(
                MathOperationGenerator.generateMathOperation3(),
                MathOperationGenerator.generateMathOperation3(),
                MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                )
            53 -> listOf(
                MathOperation(6,"+", 6),
                MathOperation(7,"+", 7),
                MathOperation(5,"+", 8),
                MathOperation(5,"+", 9),
                MathOperation(8,"+", 6),
                MathOperation(6,"+", 7),
                MathOperation(6,"+", 8),
                MathOperation(5,"+", 9),
                MathOperation(16,"+", 7),
                MathOperation(28,"+", 6)
            )
            54 -> listOf(
                MathOperation(5,"+", 9),
                MathOperation(8,"+", 6),
                MathOperation(5,"+", 6),
                MathOperation(16,"+", 7),
                MathOperation(16,"+", 18),
                MathOperation(5,"+", 8),
                MathOperation(50,"+", 90),
                MathOperation(17,"+", 7),
                MathOperation(5,"+", 8),
                MathOperation(15,"+", 9)
            )
            55 -> listOf(
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
            )
            57 -> listOf(
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
            )
            61 -> listOf(
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
            )
            65 -> listOf(
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationBeadRule(),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),

                )
            66 -> listOf(
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2)
            )
            70 -> listOf(
                MathOperationGenerator.irregularExtraction(3,3),
                MathOperationGenerator.irregularExtraction(4,4),
                MathOperationGenerator.irregularExtraction(5,5),
                MathOperationGenerator.irregularExtraction(4,4),
                MathOperationGenerator.irregularExtraction(3,3),
                MathOperationGenerator.irregularExtraction(4,4),
                MathOperationGenerator.irregularExtraction(5,5),
                MathOperationGenerator.irregularExtraction(4,4),
                )
            74 -> listOf(
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(2,2),
                MathOperationGenerator.irregularExtraction(3,3),
                MathOperationGenerator.irregularExtraction(3,3),
                MathOperationGenerator.irregularExtraction(4,4),
                MathOperationGenerator.irregularExtraction(4,4),
                MathOperationGenerator.irregularExtraction(4,4),
                MathOperationGenerator.irregularExtraction(4,4),
                MathOperationGenerator.irregularExtraction(5,5),
                MathOperationGenerator.irregularExtraction(5,5),
                MathOperationGenerator.irregularExtraction(5,5),
                MathOperationGenerator.irregularExtraction(5,5),

            )
            75 -> listOf(
                MathOperation(5, "-", 2),
                MathOperation(7, "-", 3),
                MathOperation(8, "-", 4),
                MathOperation(60, "-", 20),
                MathOperation(5, "-", 1),
                MathOperation(6, "-", 2),
                MathOperation(5, "-", 3),
                MathOperation(50, "-", 10),
                MathOperation(6, "-", 4),
                MathOperation(6, "-", 3),
                MathOperation(5, "-", 2),
                MathOperation(5, "-", 1),
            )
            76 -> listOf(
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),

                )
            79 -> listOf(
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                )
            83 -> listOf(
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),

                )
            87 -> listOf(
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(2,2),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRules(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),


                )
            88 -> listOf(
                MathOperation(10, "-", 1),
                MathOperation(11, "-", 2),
                MathOperation(12, "-", 3),
                MathOperation(13, "-", 4),
                MathOperation(14, "-", 5),
                MathOperation(15, "-", 6),
                MathOperation(16, "-", 7),
                MathOperation(17, "-", 8),
                MathOperation(18, "-", 9)
            )
            89 -> listOf(
                MathOperation(100, "-", 10),
                MathOperation(10, "-", 2),
                MathOperation(11, "-", 3),
                MathOperation(12, "-", 4),
                MathOperation(13, "-", 5),
                MathOperation(10, "-", 6),
                MathOperation(15, "-", 7),
                MathOperation(17, "-", 8),
                MathOperation(10, "-", 9)
            )
            90 -> MathOperationGenerator.extractionGenerateMathOperationList(9)
            91 -> MathOperationGenerator.extractionGenerateMathOperationList(12)
            92 -> listOf(
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
            )
            96 -> listOf(
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
            )
            100 -> listOf(
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme()
            )
            104 -> listOf(
                MathOperationGenerator.extractionGenerateMathOperation(),
                MathOperationGenerator.extractionGenerateMathOperation(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTen(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
            )
            105 -> listOf(
                MathOperation(11,"-", 6),
                MathOperation(22,"-", 6),
                MathOperation(13,"-", 7),
                MathOperation(12,"-", 7),
                MathOperation(23,"-", 8),
                MathOperation(34,"-", 8),
                MathOperation(24,"-", 9),
                MathOperation(14,"-", 9),
                MathOperation(34,"-", 6),
                MathOperation(24,"-", 7)
            )
            106 -> MathOperationGenerator.extractionBeadRulesList(8)
            109 -> listOf(
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
            )
            113 -> listOf(
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree()
            )
            117 -> listOf(
                MathOperationGenerator.extractionBeadRules(),
                MathOperationGenerator.extractionBeadRules(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesThreeTwo(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),
                MathOperationGenerator.extractionBeadRulesFourThree(),

                )
            118 -> listOf(
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                )
            122 -> listOf(
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
            )
            126 -> listOf(
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
            )
            130 -> listOf(
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),

                )
            131 -> listOf(
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
            )
            135 -> listOf(
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
            )
            139 -> listOf(
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                )
            140 -> listOf(
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
            )
            144 -> listOf(
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),
                MathOperationGenerator.multiplicationThreeFull(),

                )
            145 -> listOf(
                MathOperationGenerator.multiplicationThreeTwoFive(),
                MathOperationGenerator.multiplicationThreeTwoFive(),
                MathOperationGenerator.multiplicationThreeTwoFive(),
                MathOperationGenerator.multiplicationThreeTwoFive(),
                MathOperationGenerator.multiplicationThreeTwoFive(),
                MathOperationGenerator.multiplicationThreeTwoFive(),
                MathOperationGenerator.multiplicationThreeTwoFive(),
                MathOperationGenerator.multiplicationThreeTwoFive(),
            )
            149 -> listOf(
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
            )
            153 -> listOf(
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
                MathOperationGenerator.multiplicationThreeTwoFull(),
            )
            else -> emptyList()
        }
    }

    /**
     * [requestedId] için operasyon yoksa [minLessonId]'ye kadar birer azaltarak ilk dolu id'yi döner.
     */
    fun resolveLessonOperationsId(requestedId: Int, minLessonId: Int): Int {
        var id = requestedId
        while (getLessonOperations(id).isEmpty() && id > minLessonId) {
            id--
        }
        return id
    }

    fun resolveLessonOperationsBlindingId(requestedId: Int, minLessonId: Int): Int {
        var id = requestedId
        while (getLessonOperationsBlinding(id).isEmpty() && id > minLessonId) {
            id--
        }
        return id
    }

    fun getLessonOperationsBlinding(lessonId: Int): List<Any>{
        return when (lessonId) {
            1 -> listOf(
                listOf(10,3,5,10),
                listOf(7,20,10,11),
                listOf(12,10,5,50),
                listOf(2,5,2,40),
                listOf(5,3,20,10)
            )
            2 -> listOf(
                listOf(13,10,1,20),
                listOf(4,20,50,10),
                listOf(13,10,5,50),
                listOf(6,1,10,10),
                listOf(10,5,10,11)
            )
            3 -> listOf(
                listOf(10,3,5,10),
                listOf(7,20,10,11),
                listOf(12,10,5,50),
                listOf(2,5,2,40),
                listOf(5,3,20,10)
            )
            4 -> listOf(
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
            )
            5 -> listOf(
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
            )
            9 -> listOf(
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
            )
            13 -> listOf(
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
                MathOperationGenerator.generateSequence1(3),
            )
            14 -> listOf(
                MathOperationGenerator.generateSequence5RulesNew(3),
                MathOperationGenerator.generateSequence5RulesNew(3),
                MathOperationGenerator.generateSequence5RulesNew(3),
                MathOperationGenerator.generateSequence5RulesNew(3),
                MathOperationGenerator.generateSequence5RulesNew(3),
                MathOperationGenerator.generateSequence5RulesNew(3)
            )
            19 -> listOf(
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
            )
            22 -> listOf(
                MathOperationGenerator.generateSequence10RulesEasyOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOneCount(4),
                )
            23 -> listOf(
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                MathOperationGenerator.generateSequence10RulesEasyNew(3),
                )
            1024 -> listOf(
                MathOperationGenerator.generateSequence10RulesEasyOldOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOldOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOldOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOldOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOldOneCount(4),
                MathOperationGenerator.generateSequence10RulesEasyOldOneCount(4),
                )
            24 -> listOf(
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
            )
            28 -> listOf(
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
                MathOperationGenerator.generateSequence10RulesEasyOld(3),
            )
            29 -> listOf(
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
            )
            1029 -> listOf(
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
                MathOperationGenerator.generateSequenceBeadRules(3),
            )
            34 -> listOf(
                MathOperationGenerator.generateSequenceExtraction(3),
                MathOperationGenerator.generateSequenceExtraction(3),
                MathOperationGenerator.generateSequenceExtraction(3),
                MathOperationGenerator.generateSequenceExtraction(3),
                MathOperationGenerator.generateSequenceExtraction(3),
                MathOperationGenerator.generateSequenceExtraction(3),
            )
            39 -> listOf(
                MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                MathOperationGenerator.generateSequenceExtractionFiveRules(3),
            )
            44 -> listOf(
                MathOperationGenerator.generateSequenceExtractionTenRules(3),
                MathOperationGenerator.generateSequenceExtractionTenRules(3),
                MathOperationGenerator.generateSequenceExtractionTenRules(3),
                MathOperationGenerator.generateSequenceExtractionTenRules(3),
                MathOperationGenerator.generateSequenceExtractionTenRules(3),
                MathOperationGenerator.generateSequenceExtractionTenRules(3),
            )
            49 -> listOf(
                MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                MathOperationGenerator.generateSequenceExtractionBeadRules(3),
            )
            54 -> listOf(
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
            )
            1054 -> listOf(
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
                MathOperationGenerator.multiplicationLessFive(),
            )
            58 -> listOf(
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
                MathOperationGenerator.multiplicationLessFiveFull(),
            )
            63 -> listOf(
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
                MathOperationGenerator.multiplicationFull(),
            )
            68 -> listOf(
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
            )
            72 -> listOf(
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
                MathOperationGenerator.multiplicationTwo(),
            )
            73 -> listOf(
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
            )
            77 -> listOf(
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
                MathOperationGenerator.multiplicationTwoFull(),
            )
            78 -> listOf(
                MathOperationGenerator.generalCollectionOneDigits(4),
                MathOperationGenerator.generalCollectionOneDigits(4),
                MathOperationGenerator.generalCollectionOneDigits(4),
            )
            1000 -> listOf(
                MathOperationGenerator.generalCollectionOneDigits(6),
                MathOperationGenerator.generalCollectionOneDigits(6),
                MathOperationGenerator.generalCollectionOneDigits(6),
            )
            79 -> listOf(
                MathOperationGenerator.generalCollectionTwoDigits(4),
                MathOperationGenerator.generalCollectionTwoDigits(4),
                MathOperationGenerator.generalCollectionTwoDigits(4),
            )
            80 -> listOf(
                MathOperationGenerator.generalCollectionTwoDigits(4),
                MathOperationGenerator.generalCollectionTwoDigits(4),
                MathOperationGenerator.generalCollectionTwoDigits(4),
            )
            1001 -> listOf(
                MathOperationGenerator.generalCollectionTwoDigits(6),
                MathOperationGenerator.generalCollectionTwoDigits(6),
                MathOperationGenerator.generalCollectionTwoDigits(6),
                )
            81 -> listOf(
                MathOperationGenerator.generalCollectionThreeDigits(4),
                MathOperationGenerator.generalCollectionThreeDigits(4),
                MathOperationGenerator.generalCollectionThreeDigits(4),
            )
            1002 -> listOf(
                MathOperationGenerator.generalCollectionThreeDigits(6),
                MathOperationGenerator.generalCollectionThreeDigits(6),
                MathOperationGenerator.generalCollectionThreeDigits(6),
            )
            82 -> listOf(
                MathOperationGenerator.generalCollectionThreeDigits(7),
                MathOperationGenerator.generalCollectionThreeDigits(7),
                MathOperationGenerator.generalCollectionThreeDigits(7),
            )
            83 -> listOf(
                //MathOperationGenerator.generalCollectionFourDigits(4),
                //MathOperationGenerator.generalCollectionFourDigits(4),
                //MathOperationGenerator.generalCollectionFourDigits(4),
            )
            1003 -> listOf(
                MathOperationGenerator.generalCollectionOneDigits(15),
                MathOperationGenerator.generalCollectionOneDigits(15),
                MathOperationGenerator.generalCollectionOneDigits(15)
            )
            1004 -> listOf(
                MathOperationGenerator.generalCollectionTwoDigits(10),
                MathOperationGenerator.generalCollectionTwoDigits(10),
                MathOperationGenerator.generalCollectionTwoDigits(10)
            )
            1005 -> listOf(
                MathOperationGenerator.generalCollectionThreeDigits(10),
                MathOperationGenerator.generalCollectionThreeDigits(10),
                MathOperationGenerator.generalCollectionThreeDigits(10)
            )
            1006 -> listOf(
                MathOperationGenerator.generalCollectionOneDigits(20),
                MathOperationGenerator.generalCollectionOneDigits(20),
                MathOperationGenerator.generalCollectionOneDigits(20)
            )
            84 -> listOf(
                MathOperationGenerator.generalCollectionFourDigits(10),
                MathOperationGenerator.generalCollectionFourDigits(10),
                MathOperationGenerator.generalCollectionFourDigits(10),
            )
            85 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRace(4),
                MathOperationGenerator.generateSequenceExtractionRace(4),
                MathOperationGenerator.generateSequenceExtractionRace(4),
            )
            1007 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRace(6),
                MathOperationGenerator.generateSequenceExtractionRace(6),
                MathOperationGenerator.generateSequenceExtractionRace(6),
            )
            86 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
            )
            1008 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(6),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(6),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(6)
            )
            87 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
            )
            88 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
            )
            1009 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceHard(15),
                MathOperationGenerator.generateSequenceExtractionRaceHard(15),
                MathOperationGenerator.generateSequenceExtractionRaceHard(15),
            )
            1010 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigitsHard(10),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigitsHard(10),
                MathOperationGenerator.generateSequenceExtractionRaceTwoDigitsHard(10),
            )
            1011 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceFourDigits(10),
                MathOperationGenerator.generateSequenceExtractionRaceFourDigits(10),
                MathOperationGenerator.generateSequenceExtractionRaceFourDigits(10),
            )
            89 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7)
            )
            90 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigitsHard(10),
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigitsHard(10),
                MathOperationGenerator.generateSequenceExtractionRaceThreeDigitsHard(10),
            )
            91 -> listOf(
                MathOperationGenerator.generateSequenceExtractionRaceHard(20),
                MathOperationGenerator.generateSequenceExtractionRaceHard(20),
                MathOperationGenerator.generateSequenceExtractionRaceHard(20),
            )
            100-> listOf(

            )
            else -> emptyList()
        }
    }
}
