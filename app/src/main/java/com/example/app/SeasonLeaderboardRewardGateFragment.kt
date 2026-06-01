package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentSeasonLeaderboardRewardGateBinding

/**
 * Sezon sonu liderlik ödülü finalize edildikten sonra [pendingLeaderboardRewardSeason] dolu iken gösterilir;
 * kullanıcı "Topla" ile [BadgeFragment] kutlama kuyruğunu başlatır, kuyruk bitince pending Firestore'dan silinir.
 */
class SeasonLeaderboardRewardGateFragment : Fragment() {

    companion object {
        const val TAG = "SeasonLeaderboardRewardGate"
        const val ARG_SEASON = "arg_season"

        fun newInstance(season: Int): SeasonLeaderboardRewardGateFragment =
            SeasonLeaderboardRewardGateFragment().apply {
                arguments = Bundle().apply { putInt(ARG_SEASON, season) }
            }
    }

    private var _binding: FragmentSeasonLeaderboardRewardGateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSeasonLeaderboardRewardGateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivityChromeBlocker.acquire(requireActivity())
        val season = arguments?.getInt(ARG_SEASON) ?: return
        binding.seasonRewardGateSubtitle.text =
            getString(R.string.season_leaderboard_reward_gate_season_line, season)
        binding.seasonRewardGateCollectButton.setOnClickListener { onCollect(season) }
    }

    override fun onDestroyView() {
        MainActivityChromeBlocker.release(activity)
        (activity as? MainActivity)?.hideSeasonLeaderboardRewardGateContainer()
        (activity as? MainActivity)?.tryShowPendingMarathonGuideOnMap("SeasonLeaderboardRewardGate.onDestroyView")
        _binding = null
        super.onDestroyView()
    }

    private fun onCollect(season: Int) {
        val act = activity as? MainActivity ?: return
        val fm = act.supportFragmentManager
        binding.seasonRewardGateCollectButton.isEnabled = false

        fun removeGateAndThen(block: () -> Unit) {
            fm.beginTransaction().remove(this@SeasonLeaderboardRewardGateFragment).commitNowAllowingStateLoss()
            block()
        }

        fun tryOpenCelebrationOrAck() {
            val progress = BadgeProgressRepository.getUserBadgeProgress()
            var payloads = SeasonLeaderboardRewardPayloads.buildForSeason(progress, season)
            if (payloads.isNotEmpty()) {
                removeGateAndThen {
                    BadgeProgressFirestore.openBadgeCelebration(fm, payloads, season)
                }
                return
            }
            act.refreshUserBadgeProgressFromFirestore {
                if (!isAdded) return@refreshUserBadgeProgressFromFirestore
                payloads = SeasonLeaderboardRewardPayloads.buildForSeason(
                    BadgeProgressRepository.getUserBadgeProgress(),
                    season,
                )
                removeGateAndThen {
                    if (payloads.isEmpty()) {
                        act.onSeasonLeaderboardBadgeCelebrationFinished(season)
                    } else {
                        BadgeProgressFirestore.openBadgeCelebration(fm, payloads, season)
                    }
                }
            }
        }

        tryOpenCelebrationOrAck()
    }
}
