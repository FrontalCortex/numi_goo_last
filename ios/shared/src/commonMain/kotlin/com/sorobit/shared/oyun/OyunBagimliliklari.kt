// Kaynak: MathOperation.kt (tamamı), model/LessonItem.kt (yalnızca `oyun` paketinin okuduğu alanlar),
// MathOperationGenerator.kt (yalnızca DailyQuestionPoolBuilder'ın çağırdığı fonksiyon imzaları).
//
// Bu üç tür başka paketlerin (`matematik`, `ders`, `abakus`) sorumluluğunda. Yalıtılmış derleme için
// burada en küçük hâlleriyle tanımlandı; koordinatör asıl türlere bağlarken:
//   - [MathOperation]            → asıl MathOperation için `typealias` yapılabilir (alanlar birebir aynı),
//   - [OyunLessonItem]           → asıl LessonItem bu arayüzü uygular (ya da küçük bir sarmalayıcı yazılır),
//   - [DailyQuestionGenerators]  → MathOperationGenerator nesnesi bu arayüzü uygular (ad ve imzalar aynı).
package com.sorobit.shared.oyun

/** Android `MathOperation` ile birebir aynı alanlar. */
data class MathOperation(
    val firstNumber: Int? = null,
    val operator: String? = null,
    val secondNumber: Int? = null,
)

/** Android `LessonItem`'ın `oyun` paketinde okunan alanları. Adlar Android'dekiyle aynı. */
interface OyunLessonItem {
    val type: Int
    val title: String
    val titleUnit: String?
    val stepIsFinish: Boolean
    val record: Int?
    val cupPoint1: Int?
    val cupPoint2: Int?
    val isBlinding: Boolean?

    /** Android'de `R.drawable.*` kimliği; burada kaynak adı (ör. "chest_stars_tier0"). */
    val stepCupIcon: String
    val mapFragmentIndex: Int?

    companion object {
        // LessonItem.TYPE_* sabitleri
        const val TYPE_LESSON = 0
        const val TYPE_HEADER = 1
        const val TYPE_CHEST = 2
        const val TYPE_RACE = 3
    }
}

/** [OyunLessonItem]'ın düz veri uygulaması (testler ve geçici bağlama için). */
data class OyunLessonItemData(
    override val type: Int,
    override val title: String,
    override val titleUnit: String? = null,
    override val stepIsFinish: Boolean = false,
    override val record: Int? = null,
    override val cupPoint1: Int? = null,
    override val cupPoint2: Int? = null,
    override val isBlinding: Boolean? = null,
    override val stepCupIcon: String = "chest_stars_tier0",
    override val mapFragmentIndex: Int? = null,
) : OyunLessonItem

/**
 * Günlük soru havuzunun kullandığı soru üreteçleri.
 * Ad ve imzalar Android `MathOperationGenerator` nesnesindekilerle aynıdır.
 */
interface DailyQuestionGenerators {
    fun generateSequence1Digits(count: Int, digitsOne: Int): List<Int>
    fun generateRelatedNumbers2Blinding(count: Int, digitsOne: Int): List<Int>
    fun generateSequence10RulesEasyNew(count: Int): List<Int>
    fun generateSequence10RulesEasyOld(count: Int): List<Int>
    fun generateSequenceBeadRules(count: Int): List<Int>
    fun generateSequenceExtraction(count: Int): List<Int>
    fun generateSequenceExtractionThreeDigits(count: Int): List<Int>
    fun generateSequenceExtractionFiveRules(count: Int): List<Int>
    fun generateSequenceExtractionFiveRulesThreeDigits(count: Int): List<Int>
    fun generateSequenceExtractionTenRules(count: Int): List<Int>
    fun generateSequenceExtractionTenRulesThreeDigits(count: Int): List<Int>
    fun generateSequenceExtractionBeadRules(count: Int): List<Int>
    fun generateSequenceExtractionBeadRulesThreeDigits(count: Int): List<Int>
    fun multiplicationLessFive(): MathOperation
    fun multiplicationLessFiveFull(): MathOperation
    fun multiplicationFull(): MathOperation
    fun multiplicationTwo(): MathOperation
    fun multiplicationTwoFull(): MathOperation
    fun multiplicationThreeFull(): MathOperation
    fun multiplicationThreeTwoFive(): MathOperation
    fun multiplicationThreeTwoFull(): MathOperation
}
