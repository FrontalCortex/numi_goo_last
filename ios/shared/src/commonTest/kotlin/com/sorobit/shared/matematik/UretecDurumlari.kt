package com.sorobit.shared.matematik

import kotlin.random.Random

/**
 * Üreteçlerin tüm açık (public) fonksiyonları için tohumlu çağrı tablosu.
 * Anahtar biçimi: `fonksiyonAdı(arg1,arg2)`; GlobalValues fonksiyonları `G.` ön eki taşır.
 * Aynı tablo, Android kaynak dosyasından altın (golden) değer üretirken de kullanıldı
 * (bkz. AltinVeri.kt başındaki açıklama).
 */
object UretecDurumlari {
    private val M = MathOperationGenerator

    val TOHUMLAR = listOf(1, 2, 3)

    val durumlar: List<Pair<String, () -> Any?>> = buildList {
        fun o(ad: String, f: () -> Any?) {
            add("$ad()" to f)
        }
        fun i(ad: String, f: (Int) -> Any?, vararg p: Int) {
            p.forEach { a -> add("$ad($a)" to { f(a) }) }
        }
        fun ii(ad: String, f: (Int, Int) -> Any?, vararg p: Pair<Int, Int>) {
            p.forEach { (a, b) -> add("$ad($a,$b)" to { f(a, b) }) }
        }
        fun iii(ad: String, f: (Int, Int, Int) -> Any?, vararg p: Triple<Int, Int, Int>) {
            p.forEach { (a, b, c) -> add("$ad($a,$b,$c)" to { f(a, b, c) }) }
        }

        // --- Toplama ---
        ii("generateRelatedNumbers0", M::generateRelatedNumbers0, 1 to 1, 2 to 2)
        ii("generateRelatedNumbers", M::generateRelatedNumbers, 1 to 1, 2 to 2, 3 to 2, 2 to 3, 4 to 4, 2 to 1, 1 to 2)
        iii(
            "generateRelatedNumbersList", M::generateRelatedNumbersList,
            Triple(5, 1, 1), Triple(12, 1, 1), Triple(6, 2, 2), Triple(0, 2, 2), Triple(-1, 1, 1), Triple(4, 3, 2),
        )
        ii("generateRelatedNumbers2", M::generateRelatedNumbers2, 1 to 1, 2 to 2, 3 to 3, 2 to 3, 3 to 2)
        ii("generateRelatedNumbers2Blinding", M::generateRelatedNumbers2Blinding, 5 to 1, 5 to 2, 8 to 3, 0 to 2, 3 to 0, 1 to 4, 20 to 2)
        o("generateRandomMathOperation1", M::generateRandomMathOperation1)
        o("generateMathOperation", M::generateMathOperation)
        i("generateMathOperationList", M::generateMathOperationList, 0, 1, 5, 12, -3)
        o("generateMathOperation2", M::generateMathOperation2)
        ii("generateMathOperationWithDigits", M::generateMathOperationWithDigits, 1 to 1, 2 to 2, 3 to 2, 3 to 3, 2 to 3)
        o("generateMathOperation3", M::generateMathOperation3)
        i("generateMathOperationList3", M::generateMathOperationList3, 0, 1, 4, 10)
        o("generateMathOperation4", M::generateMathOperation4)
        i("generateMathOperationList4", M::generateMathOperationList4, 0, 1, 4, 10)
        ii("generateMathOperationWithDigits2", M::generateMathOperationWithDigits2, 1 to 1, 2 to 2, 3 to 2, 2 to 3, 3 to 3)
        ii("generateMathOperationWithDigits3", M::generateMathOperationWithDigits3, 2 to 2, 3 to 3, 3 to 1, 1 to 2)
        o("generateMathOperationBeadRule", M::generateMathOperationBeadRule)
        ii("generateMathOperationWithDigitsBeadRule", M::generateMathOperationWithDigitsBeadRule, 2 to 2, 3 to 3, 3 to 2, 1 to 1)

        // --- Çıkarma ---
        ii("irregularExtraction", M::irregularExtraction, 1 to 1, 2 to 2, 3 to 2, 3 to 3)
        ii("irregularExtractionFiveRules", M::irregularExtractionFiveRules, 2 to 2, 3 to 3, 2 to 1, 1 to 2)
        ii("irregularExtractionFiveRulesMix", M::irregularExtractionFiveRulesMix, 2 to 2, 3 to 3, 3 to 2, 1 to 1)
        i("extractionGenerateMathOperationList", M::extractionGenerateMathOperationList, 0, 1, 9, 20)
        o("extractionGenerateMathOperation", M::extractionGenerateMathOperation)
        o("extractionGenerateMathOperationTen", M::extractionGenerateMathOperationTen)
        o("extractionGenerateMathOperationTenWithOtherRules", M::extractionGenerateMathOperationTenWithOtherRules)
        o("extractionGenerateMathOperationTenWithOtherRulesExtreme", M::extractionGenerateMathOperationTenWithOtherRulesExtreme)
        o("extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton", M::extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton)
        o("extractionBeadRules", M::extractionBeadRules)
        i("extractionBeadRulesList", M::extractionBeadRulesList, 0, 1, 4, 10)
        o("extractionBeadRulesThreeTwo", M::extractionBeadRulesThreeTwo)
        o("extractionBeadRulesFourThree", M::extractionBeadRulesFourThree)

        // --- Çarpma ---
        o("multiplicationLessFive", M::multiplicationLessFive)
        o("multiplicationLessFiveFull", M::multiplicationLessFiveFull)
        o("multiplicationFull", M::multiplicationFull)
        o("multiplicationTwo", M::multiplicationTwo)
        o("multiplicationTwoFull", M::multiplicationTwoFull)
        o("multiplicationThreeFull", M::multiplicationThreeFull)
        o("multiplicationThreeTwoFive", M::multiplicationThreeTwoFive)
        o("multiplicationThreeTwoFull", M::multiplicationThreeTwoFull)

        // --- Diziler (toplama) ---
        i("generateSequence1", M::generateSequence1, 0, 1, 6, 15)
        ii("generateSequence1Digits", M::generateSequence1Digits, 0 to 2, 1 to 1, 6 to 1, 6 to 2, 10 to 3, 5 to 0)
        i("generateSequence5RulesNew", M::generateSequence5RulesNew, 0, 1, 6, 15)
        i("generateSequence10RulesEasyNew", M::generateSequence10RulesEasyNew, 0, 1, 6, 15)
        i("generateSequence10RulesEasyOneCount", M::generateSequence10RulesEasyOneCount, 0, 1, 6, 15)
        i("generateSequence10RulesEasyOldOneCount", M::generateSequence10RulesEasyOldOneCount, 0, 1, 6, 15)
        i("generateSequence10RulesEasyOld", M::generateSequence10RulesEasyOld, 0, 1, 6, 15)
        i("generateSequenceBeadRules", M::generateSequenceBeadRules, 0, 1, 6, 15)

        // --- Diziler (çıkarma) ---
        i("generateSequenceExtraction", M::generateSequenceExtraction, 0, 1, 6, 15)
        i("generateSequenceExtractionThreeDigits", M::generateSequenceExtractionThreeDigits, 0, 1, 6, 15)
        i("generateSequenceExtractionFiveRules", M::generateSequenceExtractionFiveRules, 0, 1, 6, 15)
        i("generateSequenceExtractionFiveRulesThreeDigits", M::generateSequenceExtractionFiveRulesThreeDigits, 0, 1, 6, 15)
        i("generateSequenceExtractionTenRules", M::generateSequenceExtractionTenRules, 0, 1, 2, 3, 4, 6, 15, 40)
        i("generateSequenceExtractionTenRulesThreeDigits", M::generateSequenceExtractionTenRulesThreeDigits, 0, 1, 2, 3, 4, 6, 15, 40)
        i("generateSequenceExtractionBeadRules", M::generateSequenceExtractionBeadRules, 0, 1, 2, 3, 4, 6, 15, 40)
        i("generateSequenceExtractionBeadRulesThreeDigits", M::generateSequenceExtractionBeadRulesThreeDigits, 0, 1, 2, 3, 4, 6, 15, 40)

        // --- Genel toplama / yarış ---
        i("generalCollectionOneDigits", M::generalCollectionOneDigits, 0, 1, 8)
        i("generalCollectionTwoDigits", M::generalCollectionTwoDigits, 0, 1, 8)
        i("generalCollectionThreeDigits", M::generalCollectionThreeDigits, 0, 1, 8)
        i("generalCollectionFourDigits", M::generalCollectionFourDigits, 0, 1, 8)
        i("generateSequenceExtractionRace", M::generateSequenceExtractionRace, 0, 1, 8)
        i("generateSequenceExtractionRaceHard", M::generateSequenceExtractionRaceHard, 0, 1, 8)
        i("generateSequenceExtractionRaceTwoDigits", M::generateSequenceExtractionRaceTwoDigits, 0, 1, 8)
        i("generateSequenceExtractionRaceTwoDigitsHard", M::generateSequenceExtractionRaceTwoDigitsHard, 0, 1, 8)
        i("generateSequenceExtractionRaceThreeDigits", M::generateSequenceExtractionRaceThreeDigits, 0, 1, 8)
        i("generateSequenceExtractionRaceThreeDigitsHard", M::generateSequenceExtractionRaceThreeDigitsHard, 0, 1, 8)
        i("generateSequenceExtractionRaceFourDigits", M::generateSequenceExtractionRaceFourDigits, 0, 1, 8)

        // --- GlobalValues ---
        i("G.generateRandomNumber", GlobalValues::generateRandomNumber, -1, 0, 1, 2, 3, 5)
        i("G.randomNumberChangeToString", GlobalValues::randomNumberChangeToString, 1, 2, 4)
        ii(
            "G.randomUniqueNumberStrings", GlobalValues::randomUniqueNumberStrings,
            1 to 5, 1 to 9, 1 to 12, 2 to 4, 1 to 0, 0 to 3, 3 to 6,
        )
    }

    /** Sonucu, Android tarafıyla karşılaştırılabilir kısa bir metne çevirir. */
    fun metne(sonuc: Any?): String = sonuc.toString()
        .replace("MathOperation(firstNumber=", "(")
        .replace(", operator=", " ")
        .replace(", secondNumber=", " ")

    /**
     * [f]'yi verilen tohumla çalıştırır. İstisnalar platformdan bağımsız kısa adlara indirgenir
     * (JVM'de StringIndexOutOfBoundsException, Native'de IndexOutOfBoundsException gibi farklar için).
     */
    fun calistir(tohum: Int, f: () -> Any?): String {
        RastgeleKaynagi.rastgele = Random(tohum)
        return try {
            metne(f())
        } catch (e: IndexOutOfBoundsException) {
            "EXC:IndexOutOfBounds"
        } catch (e: NumberFormatException) {
            "EXC:NumberFormat"
        } catch (e: NoSuchElementException) {
            "EXC:NoSuchElement"
        } finally {
            RastgeleKaynagi.varsayilanaDon()
        }
    }
}
