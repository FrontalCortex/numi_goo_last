package com.example.app

object ChestRewardClaimHelper {
    fun applyReward(listener: GoldUpdateListener?, reward: ChestRewardOutcome) {
        when (reward.type) {
            ChestRewardType.GOLD -> listener?.onGoldUpdated(reward.amount)
            ChestRewardType.KEY -> listener?.onKeysUpdated(reward.amount)
        }
    }
}
