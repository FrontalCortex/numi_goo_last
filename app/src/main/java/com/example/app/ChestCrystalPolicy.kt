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
    private const val KEY_REWARD_ROLL_PERCENT = 50 //anahtarın çıkma ihtimali %de olarak

    private val keyEligibleVideos = setOf(
        "crystal_purple_yellow",
        "crystal_red_yellow",
        "crystal_purple_purple",
        "crystal_blue_purple",
        "crystal_red_purple",
    )

    fun resolveVideoName(): String {
        val roll = Random.nextInt(1000) // 0..999
        return when {
            roll < 500 -> "crystal_blue_blue"         // %50
            roll < 600 -> "crystal_blue_red"          // %10
            roll < 700 -> "crystal_red_red"           // %10
            roll < 766 -> "crystal_red_purple"        // %6.6
            roll < 816 -> "crystal_red_yellow"        // %5
            roll < 882 -> "crystal_blue_purple"       // %6.6
            roll < 948 -> "crystal_purple_purple"     // %6.6
            else -> "crystal_purple_yellow"           // %5.1
        }
    }

    fun resolveRewardForVideo(videoName: String): ChestRewardOutcome {
        if (videoName in keyEligibleVideos && Random.nextInt(100) < KEY_REWARD_ROLL_PERCENT) {
            val keyAmount = resolveKeyAmountForVideo(videoName)
            return ChestRewardOutcome(
                type = ChestRewardType.KEY,
                amount = keyAmount,
                iconRes = R.drawable.key,
                label = "${keyAmount}x",
            )
        }
        return resolveGoldRewardForVideo(videoName)
    }

    private fun resolveKeyAmountForVideo(videoName: String): Int = when (videoName) {
        "crystal_purple_yellow", "crystal_red_yellow" -> 2
        "crystal_purple_purple", "crystal_blue_purple", "crystal_red_purple" -> 1
        else -> 1
    }

    private fun resolveGoldRewardForVideo(videoName: String): ChestRewardOutcome {
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
