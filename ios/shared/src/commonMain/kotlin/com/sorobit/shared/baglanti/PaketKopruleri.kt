// Android karşılığı yok: paketler ayrı ayrı (birbirine bağımlı olmadan) kopyalandığı için
// bazı paketler komşu türlerin küçük yerel kopyalarını tanımladı. Bu dosya onları asıl türlere bağlar.
// Android'de bu bağlantılar doğrudan çağrıdır (ör. DailyQuestionPoolBuilder → MathOperationGenerator).
package com.sorobit.shared.baglanti

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.kupa.CupMathOperation
import com.sorobit.shared.kupa.CupRuleEngine
import com.sorobit.shared.matematik.MathOperation
import com.sorobit.shared.matematik.MathOperationGenerator
import com.sorobit.shared.matematik.RastgeleKaynagi
import com.sorobit.shared.oyun.DailyQuestionGenerators
import com.sorobit.shared.oyun.DailyQuestionPoolBuilder
import com.sorobit.shared.oyun.OyunLessonItem
import kotlin.random.Random
import com.sorobit.shared.oyun.MathOperation as OyunMathOperation

/** `matematik.MathOperation` → `oyun` paketindeki eş türe (alanlar birebir aynı). */
fun MathOperation.toOyun(): OyunMathOperation = OyunMathOperation(firstNumber, operator, secondNumber)

/** `kupa.CupMathOperation` → asıl `MathOperation`. */
fun CupMathOperation.toMathOperation(): MathOperation = MathOperation(firstNumber, operator, secondNumber)

/** Günlük soru havuzunun üreteç arayüzünü asıl [MathOperationGenerator] nesnesine bağlar. */
object MatematikDailyQuestionGenerators : DailyQuestionGenerators {
    private val g = MathOperationGenerator

    override fun generateSequence1Digits(count: Int, digitsOne: Int) = g.generateSequence1Digits(count, digitsOne)
    override fun generateRelatedNumbers2Blinding(count: Int, digitsOne: Int) = g.generateRelatedNumbers2Blinding(count, digitsOne)
    override fun generateSequence10RulesEasyNew(count: Int) = g.generateSequence10RulesEasyNew(count)
    override fun generateSequence10RulesEasyOld(count: Int) = g.generateSequence10RulesEasyOld(count)
    override fun generateSequenceBeadRules(count: Int) = g.generateSequenceBeadRules(count)
    override fun generateSequenceExtraction(count: Int) = g.generateSequenceExtraction(count)
    override fun generateSequenceExtractionThreeDigits(count: Int) = g.generateSequenceExtractionThreeDigits(count)
    override fun generateSequenceExtractionFiveRules(count: Int) = g.generateSequenceExtractionFiveRules(count)
    override fun generateSequenceExtractionFiveRulesThreeDigits(count: Int) = g.generateSequenceExtractionFiveRulesThreeDigits(count)
    override fun generateSequenceExtractionTenRules(count: Int) = g.generateSequenceExtractionTenRules(count)
    override fun generateSequenceExtractionTenRulesThreeDigits(count: Int) = g.generateSequenceExtractionTenRulesThreeDigits(count)
    override fun generateSequenceExtractionBeadRules(count: Int) = g.generateSequenceExtractionBeadRules(count)
    override fun generateSequenceExtractionBeadRulesThreeDigits(count: Int) = g.generateSequenceExtractionBeadRulesThreeDigits(count)
    override fun multiplicationLessFive() = g.multiplicationLessFive().toOyun()
    override fun multiplicationLessFiveFull() = g.multiplicationLessFiveFull().toOyun()
    override fun multiplicationFull() = g.multiplicationFull().toOyun()
    override fun multiplicationTwo() = g.multiplicationTwo().toOyun()
    override fun multiplicationTwoFull() = g.multiplicationTwoFull().toOyun()
    override fun multiplicationThreeFull() = g.multiplicationThreeFull().toOyun()
    override fun multiplicationThreeTwoFive() = g.multiplicationThreeTwoFive().toOyun()
    override fun multiplicationThreeTwoFull() = g.multiplicationThreeTwoFull().toOyun()
}

/**
 * Android'deki `DailyQuestionPoolBuilder` nesnesinin karşılığı: asıl üreteçlere bağlı kurucu.
 * Varsayılan rastgele kaynak, üreteçle aynı kaynaktır ([RastgeleKaynagi.rastgele]).
 */
fun gunlukSoruHavuzuKur(random: Random = RastgeleKaynagi.rastgele): DailyQuestionPoolBuilder =
    DailyQuestionPoolBuilder(MatematikDailyQuestionGenerators, random)

/** Asıl [LessonItem]'ı `oyun` paketinin okuduğu arayüze sarar (canlı görünüm; kopya almaz). */
class LessonItemOyunGorunumu(val item: LessonItem) : OyunLessonItem {
    override val type get() = item.type
    override val title get() = item.title
    override val titleUnit get() = item.titleUnit
    override val stepIsFinish get() = item.stepIsFinish
    override val record get() = item.record
    override val cupPoint1 get() = item.cupPoint1
    override val cupPoint2 get() = item.cupPoint2
    override val isBlinding get() = item.isBlinding
    override val stepCupIcon get() = item.stepCupIcon
    override val mapFragmentIndex get() = item.mapFragmentIndex
}

fun LessonItem.asOyunLessonItem(): OyunLessonItem = LessonItemOyunGorunumu(this)

fun List<LessonItem>.asOyunLessonItems(): List<OyunLessonItem> = map { it.asOyunLessonItem() }

/**
 * Android `CupRuleEngine.buildLessonItem` son adımı: part 9 yer tutucusunu alıp
 * motorun hesapladığı 8 alanı `copy` ile ezer.
 */
fun CupRuleEngine.CupLessonItemOverride.applyToPlaceholder(
    placeholder: LessonItem = GlobalLessonData.createLessonItems(9).first(),
): LessonItem = placeholder.copy(
    timePeriod = timePeriod,
    cupDigitSize = cupDigitSize,
    cupNumberCount = cupNumberCount,
    cupDifficultyLevel = cupDifficultyLevel,
    cupWinDelta = cupWinDelta,
    cupLossDelta = cupLossDelta,
    isBlinding = isBlinding,
    isExtraction = isExtraction,
)
