// Kaynak (Android): app/src/main/java/com/example/app/SeasonLeaderboardRewardPayloads.kt — TAMAMI (zaten saf).
package com.sorobit.shared.kupa.sezon

import com.sorobit.shared.kupa.rozet.BadgeAnimMode
import com.sorobit.shared.kupa.rozet.BadgeLevelUpPayload
import com.sorobit.shared.kupa.rozet.UserBadgeProgress

/**
 * Sezon [season] için finalize ile gelen madalya/kupa satırlarına göre rozet kutlama kuyruğu üretir
 * (sandık sonucu / rozet kutlaması ile aynı payload biçimi).
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
