package com.example.app

import com.example.app.BadgeFragment.BadgeAnimMode

/**
 * Sezon [season] için finalize ile gelen madalya/kupa satırlarına göre [BadgeFragment] kutlama kuyruğu üretir
 * ([ChestResult] / [BadgeProgressFirestore.openBadgeCelebration] ile aynı payload biçimi).
 */
object SeasonLeaderboardRewardPayloads {

    fun buildForSeason(progress: UserBadgeProgress, season: Int): List<BadgeLevelUpPayload> {
        val out = mutableListOf<BadgeLevelUpPayload>()
        if (progress.goldMedalPiece.any { it.season == season }) {
            out.add(BadgeLevelUpPayload(BadgeAnimMode.GOLD_MEDAL, 0, 1, 1))
        }
        if (progress.silverMedalPiece.any { it.season == season }) {
            out.add(BadgeLevelUpPayload(BadgeAnimMode.SILVER_MEDAL, 0, 1, 1))
        }
        if (progress.bronzeMedalPiece.any { it.season == season }) {
            out.add(BadgeLevelUpPayload(BadgeAnimMode.BRONZE_MEDAL, 0, 1, 1))
        }
        val cups = progress.cupPiece.filter { it.season == season }.sortedBy { it.rank }
        for (c in cups) {
            val r = c.rank.coerceAtLeast(1)
            out.add(BadgeLevelUpPayload(BadgeAnimMode.CUP_GOOGLE, r, r, r))
        }
        return out
    }
}
