package com.example.app

import com.example.app.model.LessonItem

data class DailyQuestionSource(
    val listIndex: Int,
    val item: LessonItem,
    val finishedChestIndex: Int,
)

object DailyQuestionPoolBuilder {

    fun buildPart1Sources(items: List<LessonItem>): List<DailyQuestionSource> {
        val pool = mutableListOf<DailyQuestionSource>()
        var finishedChestIndex = 0
        items.forEachIndexed { index, item ->
            if (item.type != LessonItem.TYPE_CHEST) return@forEachIndexed
            if (!item.stepIsFinish) return@forEachIndexed
            finishedChestIndex++
            if (generatorForFinishedChest(finishedChestIndex, item) != null) {
                pool.add(
                    DailyQuestionSource(
                        listIndex = index,
                        item = item,
                        finishedChestIndex = finishedChestIndex,
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
        val generator = generatorForFinishedChest(source.finishedChestIndex, source.item) ?: return null
        val result = generator()
        val sequence = result.first
        val displayIntervalMs = result.second
        if (sequence.isEmpty()) return null
        val titleUnit = source.item.titleUnit?.takeIf { it.isNotBlank() }
            ?: source.item.title
        return DailyQuestionSlot(
            sequence = sequence,
            partId = 1,
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
        finishedChestIndex: Int,
        item: LessonItem,
    ): (() -> Pair<List<Int>, Long>)? {
        return when (finishedChestIndex) {
            1 -> null
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateSequence1Digits(4, 4), 1000L) },
                        medium = { Pair(MathOperationGenerator.generateSequence1Digits(4, 3), 2500L) },
                        easy = { Pair(MathOperationGenerator.generateSequence1Digits(3, 2), 3000L) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { Pair(MathOperationGenerator.generateRelatedNumbers2Blinding(3, 4), 1500L) },
                        medium = { Pair(MathOperationGenerator.generateRelatedNumbers2Blinding(3, 3), 2500L) },
                        easy = { Pair(MathOperationGenerator.generateRelatedNumbers2Blinding(4, 2), 3000L) },
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

    private fun dailyQuestionFromRecord(
        item: LessonItem,
        hard: () -> Pair<List<Int>, Long>,
        medium: () -> Pair<List<Int>, Long>,
        easy: () -> Pair<List<Int>, Long>,
    ): Pair<List<Int>, Long> {
        val record = item.record
        val cupPoint1 = item.cupPoint1
        val cupPoint2 = item.cupPoint2
        return when {
            //record != null && cupPoint1 != null && record >= cupPoint1 -> hard()
            //record != null && cupPoint2 != null && record >= cupPoint2 -> medium()
            else -> hard()
        }
    }

    private fun dailyStableIndex(
        poolSize: Int,
        periodKey: String,
        uidKey: String,
        slot: Int,
    ): Int {
        if (poolSize <= 1) return 0
        val seed = "$uidKey|$periodKey|$slot|$poolSize".hashCode()
        return Math.floorMod(seed, poolSize)
    }
}
