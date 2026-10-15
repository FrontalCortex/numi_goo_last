package com.example.app

import com.example.app.databinding.ViewBugReportRewardsBinding
import java.text.NumberFormat
import java.util.Locale

/** Büyük ödülün üç parçasını (view_bug_report_rewards) [BugReports.Tier.BIG]'den doldurur. */
object BugReportRewardViews {
    fun bind(row: ViewBugReportRewardsBinding) {
        val tier = BugReports.Tier.BIG
        val format = NumberFormat.getIntegerInstance(Locale("tr", "TR"))
        row.rewardCreditsText.text = format.format(tier.credits)
        row.rewardGoldText.text = format.format(tier.gold)
        row.rewardKeysText.text = format.format(tier.keys)
    }
}
