// Kaynak: MissionQuestCatalog.kt (tamamı) + MissionsProgressStore.kt sonundaki `MissionWindow` enum'u.
// Fark: `titleResId: Int` (R.string.*) → kaynak adını taşıyan String (ör. "mission_finish_two_lessons").
package com.sorobit.shared.oyun

enum class MissionCounterType {
    STEP_FINISH,
    STEP_INCREMENT,
    PERFECT_STEP_INCREMENT,
    CHEST_RECORD_BREAK,
    CHEST_STAR_GAIN,
    LEARN_MINUTES,
}

enum class MissionWindow {
    DAILY,
    WEEKLY,
}

data class MissionQuestDefinition(
    val id: String,
    /** Android'de `R.string.*` kimliği; burada string kaynağının adı. */
    val titleResId: String,
    val target: Int,
    val counterType: MissionCounterType,
    val availableWindows: Set<MissionWindow> = setOf(MissionWindow.DAILY, MissionWindow.WEEKLY),
)

object MissionQuestCatalog {
    // DİKKAT: Liste SIRASI görev seçimini belirler (tohumlu karıştırma bu sıraya uygulanır);
    // sıra ya da kimlikler değişirse Android ile iOS farklı görev seçer.
    val all: List<MissionQuestDefinition> = listOf(
        MissionQuestDefinition(
            id = "finish_step_2",
            titleResId = "mission_finish_two_lessons",
            target = 2,
            counterType = MissionCounterType.STEP_FINISH,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_5",
            titleResId = "mission_finish_five_lessons",
            target = 5,
            counterType = MissionCounterType.STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_perfect_2",
            titleResId = "mission_finish_two_lessons_perfect",
            target = 2,
            counterType = MissionCounterType.PERFECT_STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "chest_record_break_1",
            titleResId = "mission_break_chest_record",
            target = 1,
            counterType = MissionCounterType.CHEST_RECORD_BREAK,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "chest_star_gain_1",
            titleResId = "mission_gain_one_star_in_unit_eval",
            target = 5,
            counterType = MissionCounterType.CHEST_STAR_GAIN,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "chest_star_gain_2_daily",
            titleResId = "mission_gain_two_stars",
            target = 2,
            counterType = MissionCounterType.CHEST_STAR_GAIN,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "learn_for_10_minutes",
            titleResId = "mission_learn_for_10_minutes",
            target = 10,
            counterType = MissionCounterType.LEARN_MINUTES,
            availableWindows = setOf(MissionWindow.DAILY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_20",
            titleResId = "mission_finish_twenty_lessons",
            target = 20,
            counterType = MissionCounterType.STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "finish_step_5_weekly",
            titleResId = "mission_finish_five_steps",
            target = 5,
            counterType = MissionCounterType.STEP_FINISH,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
        MissionQuestDefinition(
            id = "finish_lessons_perfect_10_weekly",
            titleResId = "mission_finish_ten_lessons_perfect",
            target = 10,
            counterType = MissionCounterType.PERFECT_STEP_INCREMENT,
            availableWindows = setOf(MissionWindow.WEEKLY),
        ),
    )
}
