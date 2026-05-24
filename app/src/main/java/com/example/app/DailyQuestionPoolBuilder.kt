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
        val sequence = generator()
        if (sequence.isEmpty()) return null
        val titleUnit = source.item.titleUnit?.takeIf { it.isNotBlank() }
            ?: source.item.title
        return DailyQuestionSlot(
            sequence = sequence,
            partId = 1,
            itemIndex = source.listIndex,
            titleUnit = titleUnit,
            difficulty = difficultyLabel(source.item),
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
    ): (() -> List<Int>)? {
        return when (finishedChestIndex) {
            1 -> null
            2 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { MathOperationGenerator.generateSequence1Digits(2, 1) },//test için ileride 4 4
                        medium = { MathOperationGenerator.generateSequence1Digits(4, 3) },
                        easy = { MathOperationGenerator.generateSequence1Digits(3, 2) },
                    )
                }
            }
            3 -> {
                {
                    dailyQuestionFromRecord(
                        item,
                        hard = { MathOperationGenerator.generateRelatedNumbers2Blinding(2, 1) },//test için ileride 4 4
                        medium = { MathOperationGenerator.generateRelatedNumbers2Blinding(4, 3) },
                        easy = { MathOperationGenerator.generateRelatedNumbers2Blinding(4, 2) },
                    )
                }
            }
            4, 5, 6 -> null
            else -> null
        }
    }

    private fun dailyQuestionFromRecord(
        item: LessonItem,
        hard: () -> List<Int>,
        medium: () -> List<Int>,
        easy: () -> List<Int>,
    ): List<Int> {
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
        val seed = "$uidKey|$periodKey|$slot|$poolSize".hashCode()
        return Math.floorMod(seed, poolSize)
    }
}
