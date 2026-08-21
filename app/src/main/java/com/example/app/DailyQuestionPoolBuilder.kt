package com.example.app

import com.example.app.model.LessonItem

data class DailyQuestionSource(
    val listIndex: Int,
    val item: LessonItem,
    val finishedChestIndex: Int,
    val partId: Int,
)

object DailyQuestionPoolBuilder {

    fun buildSourcesForPart(items: List<LessonItem>, partId: Int): List<DailyQuestionSource> {
        val pool = mutableListOf<DailyQuestionSource>()
        var finishedChestIndex = 0
        items.forEachIndexed { index, item ->
            if (item.type != LessonItem.TYPE_CHEST) return@forEachIndexed
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

    fun difficultyLabel(item: LessonItem): String {
        val record = item.record
        val cupPoint1 = item.cupPoint1
        val cupPoint2 = item.cupPoint2
        return when {
            record != null && cupPoint1 != null && record >= cupPoint1 -> "Zor"
            record != null && cupPoint2 != null && record >= cupPoint2 -> "Orta"
            else -> "Kolay"
        }
    }

    private fun generatorForFinishedChest(
        partId: Int,
        finishedChestIndex: Int,
        item: LessonItem,
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
        item: LessonItem,
    ): (() -> Pair<Any, Long>)? {
        return when (finishedChestIndex) {
            1 -> null
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequence1Digits(4, 4), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateSequence1Digits(4, 3), 2500L) },
                        easy = { Pair(MathOperationGenerator.generateSequence1Digits(3, 2), 3000L) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateRelatedNumbers2Blinding(4, 4), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateRelatedNumbers2Blinding(4, 3), 2500L) },
                        easy = { Pair(MathOperationGenerator.generateRelatedNumbers2Blinding(3, 2), 3000L) },
                    )
                }
            }
            5 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequence10RulesEasyNew(5), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateSequence10RulesEasyNew(4), 2500L) },
                        easy = { Pair(MathOperationGenerator.generateSequence10RulesEasyNew(3), 3000L) },
                    )
                }
            }
            6 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequenceBeadRules(5), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateSequenceBeadRules(4), 2500L) },
                        easy = { Pair(MathOperationGenerator.generateSequenceBeadRules(3), 3000L) },
                    )
                }
            }

            4 -> null
            else -> null
        }
    }

    private fun generatorForPart2Chest(
        finishedChestIndex: Int,
        item: LessonItem,
    ): (() -> Pair<Any, Long>)? {

        return when (finishedChestIndex) {
            1 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequenceExtractionThreeDigits(4), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateSequenceExtractionThreeDigits(4), 2500L) },
                        easy = { Pair(MathOperationGenerator.generateSequenceExtraction(3), 3000L) },
                    )
                }
            }
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequenceExtractionFiveRulesThreeDigits(4), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateSequenceExtractionFiveRulesThreeDigits(3), 3500L) },
                        easy = { Pair(MathOperationGenerator.generateSequenceExtractionFiveRules(3), 3000L) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequenceExtractionTenRulesThreeDigits(4), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateSequenceExtractionTenRulesThreeDigits(3), 3500L) },
                        easy = { Pair(MathOperationGenerator.generateSequenceExtractionTenRules(3), 3000L) },
                    )
                }
            }
            4 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequenceExtractionBeadRulesThreeDigits(4), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateSequenceExtractionBeadRulesThreeDigits(3), 3500L) },
                        easy = { Pair(MathOperationGenerator.generateSequenceExtractionBeadRules(3), 3000L) },
                    )
                }
            }
            else -> null
        }
    }

    private fun generatorForPart3Chest(
        finishedChestIndex: Int,
        item: LessonItem,
    ): (() -> Pair<Any, Long>)? {
        return when (finishedChestIndex) {
            1 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.multiplicationLessFiveFull(),4000L) },
                        medium = { Pair(MathOperationGenerator.multiplicationFull(), 2500L) },
                        easy = { Pair(MathOperationGenerator.multiplicationLessFive(), 3000L) },
                    )
                }
            }
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.multiplicationTwoFull(),4000L) },
                        medium = { Pair(MathOperationGenerator.multiplicationTwo(), 2500L) },
                        easy = { Pair(MathOperationGenerator.multiplicationTwo(), 3000L) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.multiplicationThreeFull(),4000L) },
                        medium = { Pair(MathOperationGenerator.multiplicationThreeFull(), 2500L) },
                        easy = { Pair(MathOperationGenerator.multiplicationThreeFull(), 3000L) },
                    )
                }
            }
            4 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.multiplicationThreeTwoFull(),4000L) },
                        medium = { Pair(MathOperationGenerator.multiplicationThreeTwoFive(), 2500L) },
                        easy = { Pair(MathOperationGenerator.multiplicationThreeTwoFive(), 3000L) },
                    )
                }
            }
            else -> null
        }
    }

    private fun dailyQuestionFromRecord(
        item: LessonItem,
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

    private fun dailyStableIndex(
        poolSize: Int,
        periodKey: String,
        uidKey: String,
        slot: Int,
    ): Int {
        if (poolSize <= 1) return 0
        // TEST MODU: Gecici olarak havuzdan tamamen rastgele seciyoruz.
        return (0 until poolSize).random()
    }
}
