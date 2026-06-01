package com.example.app

import com.example.app.R

enum class MissionCounterType {
    STEP_FINISH,
    STEP_INCREMENT,
    PERFECT_STEP_INCREMENT,
    CHEST_RECORD_BREAK,
    CHEST_STAR_GAIN,
    LEARN_MINUTES,
}

data class MissionQuestDefinition(
    val id: String,
    val titleResId: Int,
    val target: Int,
    val counterType: MissionCounterType,
    val availableWindows: Set<MissionWindow> = setOf(MissionWindow.DAILY, MissionWindow.WEEKLY),
)

object MissionQuestCatalog {
    val all: List<MissionQuestDefinition> = listOf(
        MissionQuestDefinition(
            id = "finish_step_2",
            titleResId = R.string.mission_finish_two_lessons,
            target = 2,
            counterType = MissionCounterType.STEP_FINISH,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_5",
            titleResId = R.string.mission_finish_five_lessons,
            target = 5,
            counterType = MissionCounterType.STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_perfect_2",
            titleResId = R.string.mission_finish_two_lessons_perfect,
            target = 2,
            counterType = MissionCounterType.PERFECT_STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "chest_record_break_1",
            titleResId = R.string.mission_break_chest_record,
            target = 1,
            counterType = MissionCounterType.CHEST_RECORD_BREAK,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "chest_star_gain_1",
            titleResId = R.string.mission_gain_one_star_in_unit_eval,
            target = 5,
            counterType = MissionCounterType.CHEST_STAR_GAIN,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "chest_star_gain_2_daily",
            titleResId = R.string.mission_gain_two_stars,
            target = 2,
            counterType = MissionCounterType.CHEST_STAR_GAIN,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "learn_for_10_minutes",
            titleResId = R.string.mission_learn_for_10_minutes,
            target = 10,
            counterType = MissionCounterType.LEARN_MINUTES,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_20",
            titleResId = R.string.mission_finish_twenty_lessons,
            target = 20,
            counterType = MissionCounterType.STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "finish_step_5_weekly",
            titleResId = R.string.mission_finish_five_steps,
            target = 5,
            counterType = MissionCounterType.STEP_FINISH,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_perfect_10_weekly",
            titleResId = R.string.mission_finish_ten_lessons_perfect,
            target = 10,
            counterType = MissionCounterType.PERFECT_STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
    )
}
