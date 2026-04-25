package com.example.app

import kotlin.random.Random

enum class ChestRewardType {
    GOLD,
    KEY,
}

data class ChestRewardOutcome(
    val type: ChestRewardType,
    val amount: Int,
    val iconRes: Int,
    val label: String,
)

object ChestCrystalPolicy {
    fun resolveVideoName(): String {
        val roll = Random.nextInt(1000) // 0..999
        return when {
            roll < 600 -> "crystal_blue_blue"         // %60
            roll < 700 -> "crystal_blue_red"          // %10
            roll < 800 -> "crystal_red_red"           // %10
            roll < 850 -> "crystal_red_purple"        // %5
            roll < 875 -> "crystal_red_yellow"        // %2.5
            roll < 925 -> "crystal_blue_purple"       // %5
            roll < 975 -> "crystal_purple_purple"     // %5
            else -> "crystal_purple_yellow"           // %2.5
        }
    }

    fun resolveRewardForVideo(videoName: String): ChestRewardOutcome {
        val goldRange = when (videoName) {
            "crystal_blue_blue" -> 50..100
            "crystal_blue_red" -> 150..200
            "crystal_red_red" -> 150..200
            "crystal_red_purple" -> 300..400
            "crystal_red_yellow" -> 1000..1500
            "crystal_blue_purple" -> 300..400
            "crystal_purple_purple" -> 300..400
            "crystal_purple_yellow" -> 1000..1500
            else -> 50..100
        }
        val amount = goldRange.random()
        return ChestRewardOutcome(
            type = ChestRewardType.GOLD,
            amount = amount,
            iconRes = R.drawable.open_chest,
            label = "$amount altın",
        )
    }
}
