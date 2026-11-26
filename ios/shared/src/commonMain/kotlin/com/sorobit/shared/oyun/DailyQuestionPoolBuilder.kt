// Kaynak: DailyQuestionPoolBuilder.kt (tamamı).
// Farklar:
//  - Android'de `object` idi ve `MathOperationGenerator`'ı doğrudan çağırıyordu; burada üreteçler
//    [DailyQuestionGenerators] arayüzüyle, rastgelelik [random] ile dışarıdan verilir (sınıf oldu).
//  - `LessonItem` → [OyunLessonItem] (yalnızca okunan alanlar).
// Davranış notu (Android'deki hâliyle korundu): `dailyStableIndex` adına rağmen "TEST MODU"
// yorumuyla havuzdan TAMAMEN RASTGELE seçiyor; periodKey/uidKey/slot kullanılmıyor.
package com.sorobit.shared.oyun

import kotlin.random.Random

data class DailyQuestionSource(
    val listIndex: Int,
    val item: OyunLessonItem,
    val finishedChestIndex: Int,
    val partId: Int,
)

class DailyQuestionPoolBuilder(
    private val generators: DailyQuestionGenerators,
    private val random: Random = Random.Default,
) {

    fun buildSourcesForPart(items: List<OyunLessonItem>, partId: Int): List<DailyQuestionSource> {
        val pool = mutableListOf<DailyQuestionSource>()
        var finishedChestIndex = 0
        items.forEachIndexed { index, item ->
            if (item.type != OyunLessonItem.TYPE_CHEST) return@forEachIndexed
            if (!item.stepIsFinish) return@forEachIndexed
            finishedChestIndex++
            if (generatorForFinishedChest(partId, finishedChestIndex, item) != null) {
                pool.add(
                    DailyQuestionSource(
                        listIndex = index,
                        item = item,
                        finishedChestIndex = finishedChestIndex,
                        partId = partId,
                    ),
                )
            }
        }
        return pool
    }

    fun buildChallenge(
        periodKey: String,
        uidKey: String,
        sources: List<DailyQuestionSource>,
    ): DailyQuestionChallenge? {
        if (sources.isEmpty()) return null
        val questions = (0 until DailyQuestionPeriod.QUESTIONS_PER_PERIOD).mapNotNull { slot ->
            val sourceIndex = dailyStableIndex(
                poolSize = sources.size,
                periodKey = periodKey,
                uidKey = uidKey,
                slot = slot,
            )
            generateSlot(sources[sourceIndex])
        }
        if (questions.size != DailyQuestionPeriod.QUESTIONS_PER_PERIOD) return null
        return DailyQuestionChallenge(
            periodKey = periodKey,
            solvedCount = 0,
            questions = questions,
        )
    }

    private fun generateSlot(source: DailyQuestionSource): DailyQuestionSlot? {
        val generator = generatorForFinishedChest(source.partId, source.finishedChestIndex, source.item) ?: return null
        val result = generator()
        val data = result.first
        val displayIntervalMs = result.second

        var sequence: List<Int> = emptyList()
        var mathOperation: MathOperation? = null

        if (data is List<*>) {
            @Suppress("UNCHECKED_CAST")
            sequence = data as List<Int>
            if (sequence.isEmpty()) return null
        } else if (data is MathOperation) {
            mathOperation = data
        } else {
            return null
        }

        val titleUnit = source.item.titleUnit?.takeIf { it.isNotBlank() } ?: source.item.title

        return DailyQuestionSlot(
            sequence = sequence,
            mathOperation = mathOperation,
            partId = source.partId,
            itemIndex = source.listIndex,
            titleUnit = titleUnit,
            difficulty = difficultyLabel(source.item),
            displayIntervalMs = displayIntervalMs,
        )
    }

    private fun generatorForFinishedChest(
        partId: Int,
        finishedChestIndex: Int,
        item: OyunLessonItem,
    ): (() -> Pair<Any, Long>)? {
        return when (partId) {
            1 -> generatorForPart1Chest(finishedChestIndex, item)
            2 -> generatorForPart2Chest(finishedChestIndex, item)
            3 -> generatorForPart3Chest(finishedChestIndex, item)
            else -> null
        }
    }

    private fun generatorForPart1Chest(
        finishedChestIndex: Int,
        item: OyunLessonItem,
    ): (() -> Pair<Any, Long>)? {
        val g = generators
        return when (finishedChestIndex) {
            1 -> null
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequence1Digits(4, 4), 1500L) },
                        medium = { Pair(g.generateSequence1Digits(4, 3), 2500L) },
                        easy = { Pair(g.generateSequence1Digits(3, 2), 3000L) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateRelatedNumbers2Blinding(4, 4), 1500L) },
                        medium = { Pair(g.generateRelatedNumbers2Blinding(4, 3), 2500L) },
                        easy = { Pair(g.generateRelatedNumbers2Blinding(3, 2), 3000L) },
                    )
                }
            }
            4 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequence10RulesEasyNew(5), 1500L) },
                        medium = { Pair(g.generateSequence10RulesEasyNew(4), 2500L) },
                        easy = { Pair(g.generateSequence10RulesEasyNew(3), 3000L) },
                    )
                }
            }
            5 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequence10RulesEasyOld(5), 1500L) },
                        medium = { Pair(g.generateSequence10RulesEasyOld(4), 2500L) },
                        easy = { Pair(g.generateSequence10RulesEasyOld(3), 3000L) },
                    )
                }
            }
            6 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequenceBeadRules(5), 1500L) },
                        medium = { Pair(g.generateSequenceBeadRules(4), 2500L) },
                        easy = { Pair(g.generateSequenceBeadRules(3), 3000L) },
                    )
                }
            }
            else -> null
        }
    }

    private fun generatorForPart2Chest(
        finishedChestIndex: Int,
        item: OyunLessonItem,
    ): (() -> Pair<Any, Long>)? {
        val g = generators
        return when (finishedChestIndex) {
            1 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequenceExtractionThreeDigits(4), 1500L) },
                        medium = { Pair(g.generateSequenceExtractionThreeDigits(4), 2500L) },
                        easy = { Pair(g.generateSequenceExtraction(3), 3000L) },
                    )
                }
            }
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequenceExtractionFiveRulesThreeDigits(4), 1500L) },
                        medium = { Pair(g.generateSequenceExtractionFiveRulesThreeDigits(3), 3500L) },
                        easy = { Pair(g.generateSequenceExtractionFiveRules(3), 3000L) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequenceExtractionTenRulesThreeDigits(4), 1500L) },
                        medium = { Pair(g.generateSequenceExtractionTenRulesThreeDigits(3), 3500L) },
                        easy = { Pair(g.generateSequenceExtractionTenRules(3), 3000L) },
                    )
                }
            }
            4 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.generateSequenceExtractionBeadRulesThreeDigits(4), 1500L) },
                        medium = { Pair(g.generateSequenceExtractionBeadRulesThreeDigits(3), 3500L) },
                        easy = { Pair(g.generateSequenceExtractionBeadRules(3), 3000L) },
                    )
                }
            }
            else -> null
        }
    }

    private fun generatorForPart3Chest(
        finishedChestIndex: Int,
        item: OyunLessonItem,
    ): (() -> Pair<Any, Long>)? {
        val g = generators
        return when (finishedChestIndex) {
            1 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.multiplicationLessFiveFull(), 4000L) },
                        medium = { Pair(g.multiplicationFull(), 2500L) },
                        easy = { Pair(g.multiplicationLessFive(), 3000L) },
                    )
                }
            }
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.multiplicationTwoFull(), 4000L) },
                        medium = { Pair(g.multiplicationTwo(), 2500L) },
                        easy = { Pair(g.multiplicationTwo(), 3000L) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.multiplicationThreeFull(), 4000L) },
                        medium = { Pair(g.multiplicationThreeFull(), 2500L) },
                        easy = { Pair(g.multiplicationThreeFull(), 3000L) },
                    )
                }
            }
            4 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(g.multiplicationThreeTwoFull(), 4000L) },
                        medium = { Pair(g.multiplicationThreeTwoFive(), 2500L) },
                        easy = { Pair(g.multiplicationThreeTwoFive(), 3000L) },
                    )
                }
            }
            else -> null
        }
    }

    private fun dailyQuestionFromRecord(
        item: OyunLessonItem,
        hard: () -> Pair<Any, Long>,
        medium: () -> Pair<Any, Long>,
        easy: () -> Pair<Any, Long>,
    ): Pair<Any, Long> {
        val record = item.record
        val cupPoint1 = item.cupPoint1
        val cupPoint2 = item.cupPoint2
        return when {
            record != null && cupPoint1 != null && record >= cupPoint1 -> hard()
            record != null && cupPoint2 != null && record >= cupPoint2 -> medium()
            else -> easy()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun dailyStableIndex(
        poolSize: Int,
        periodKey: String,
        uidKey: String,
        slot: Int,
    ): Int {
        if (poolSize <= 1) return 0
        // TEST MODU (Android'deki hâliyle): geçici olarak havuzdan tamamen rastgele seçiliyor.
        return (0 until poolSize).random(random)
    }

    companion object {
        /** Zorluk etiketi Firestore'a da yazılır ("Zor" / "Orta" / "Kolay") — değiştirilmemeli. */
        fun difficultyLabel(item: OyunLessonItem): String {
            val record = item.record
            val cupPoint1 = item.cupPoint1
            val cupPoint2 = item.cupPoint2
            return when {
                record != null && cupPoint1 != null && record >= cupPoint1 -> "Zor"
                record != null && cupPoint2 != null && record >= cupPoint2 -> "Orta"
                else -> "Kolay"
            }
        }
    }
}
