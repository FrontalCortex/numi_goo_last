package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentCupPathRoadBinding

/**
 * Kupa yolu (Trophy Road) ekranı.
 *
 * Bir kupa yolunda kullanıcının nerede olduğunu ve ileride hangi kupa sayılarında sandık
 * kazanacağını gösterir. Kupa yolu panelindeki karttan açılır.
 *
 * ## Ekranda ne yazdığına kim karar veriyor
 * Eşik matematiğinin tamamı [CupPathRewardRepository]'de; burada yalnızca çizim var. Asıl
 * doğrulama ise sunucuda (`claimCupPathChest`): ekrandaki veri eskimiş olsa bile hak
 * edilmemiş bir sandık açılamaz.
 *
 * ## Neden bütün hazır sandıklar aynı anda "HAZIR" görünüyor
 * Sunucu her çağrıda yalnızca sıradaki eşiği veriyor. Kupası çok ilerlemiş bir kullanıcıda
 * birden fazla eşik birikmiş olabilir; hangisine dokunulursa dokunulsun sıradaki alınır.
 * Sandıklar birbirinin aynısı olduğu için bu fark edilmiyor, alınan sandık sayısı da doğru
 * kalıyor.
 */
class CupPathRoadFragment : Fragment() {

    private var _binding: FragmentCupPathRoadBinding? = null
    private val binding get() = _binding!!

    private var cupField: String = CupPathRewardRepository.FIELD_ADDITION
    private val adapter = MilestoneAdapter()

    /** Liste ilk dolduğunda kullanıcının bulunduğu yere bir kez kaydırılır, sonra dokunulmaz. */
    private var scrolledToCurrent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cupField = arguments?.getString(ARG_CUP_FIELD).orEmpty()
            .takeIf { it in CupPathRewardRepository.FIELDS }
            ?: CupPathRewardRepository.FIELD_ADDITION
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCupPathRoadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { closeFragment() }
        binding.btnCupPathRoadBack.setOnClickListener { closeFragment() }

        binding.tvCupPathRoadTitle.text = CupPathRewardRepository.titleOf(cupField)
        binding.cupPathRoadList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.cupPathRoadList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Sandık açıp geri dönüldüğünde defter değişmiş olur; ekran her görünüşte tazeleniyor.
        loadState()
    }

    private fun loadState() {
        CupPathRewardRepository.fetchState(cupField) { state ->
            if (!isAdded || _binding == null) return@fetchState
            render(state)
        }
    }

    private fun render(state: CupPathRewardRepository.CupPathState) {
        binding.cupPathRoadLoading.visibility = View.GONE

        binding.tvCupPathRoadScore.text = state.cupScore.toString()
        binding.tvCupPathRoadHint.text = when {
            state.pendingChests > 1 -> "${state.pendingChests} sandık seni bekliyor"
            state.pendingChests == 1 -> "Bir sandık seni bekliyor"
            else -> "Sonraki sandık için ${state.nextMilestone - state.cupScore} kupa"
        }

        binding.tvCupPathRoadProgress.text = state.label
        binding.cupPathRoadProgressFill.setBackgroundResource(
            if (state.claimable) R.drawable.daily_question_progress_fill_complete
            else R.drawable.daily_question_progress_fill
        )
        val zone = binding.cupPathRoadProgressZone
        val fill = binding.cupPathRoadProgressFill
        zone.post {
            if (_binding == null) return@post
            val full = zone.width
            if (full <= 0) return@post
            val lp = fill.layoutParams
            lp.width = (full * state.fraction).toInt().coerceIn(0, full)
            fill.layoutParams = lp
        }

        val milestones = state.milestones()
        adapter.submit(milestones)

        if (!scrolledToCurrent) {
            // Kullanıcının bulunduğu eşik: ilk alınmamış olan. Hepsi alınmışsa liste sonu.
            val index = milestones.indexOfFirst {
                it.status != CupPathRewardRepository.MilestoneStatus.CLAIMED
            }.takeIf { it >= 0 } ?: (milestones.size - 1)
            if (index >= 0) {
                scrolledToCurrent = true
                // Sola bir öğelik pay bırakılıyor ki kullanıcı geçtiği eşiği de görsün ve
                // listenin devam ettiği anlaşılsın.
                (binding.cupPathRoadList.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(maxOf(0, index - 1), 0)
            }
        }
    }

    private fun onMilestoneTapped(milestone: CupPathRewardRepository.Milestone) {
        if (!isAdded) return
        when (milestone.status) {
            CupPathRewardRepository.MilestoneStatus.CLAIMABLE -> {
                (activity as? MainActivity)?.showAbacusOverlayFragment(
                    NewChestFragment.newInstance(
                        NewChestFragment.ChestRarity.COMMON,
                        source = AnalyticsLogger.CHEST_SOURCE_CUP_PATH,
                        cupField = cupField,
                    )
                )
            }
            CupPathRewardRepository.MilestoneStatus.CLAIMED -> {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Bu sandığı zaten aldın",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
            CupPathRewardRepository.MilestoneStatus.LOCKED -> {
                android.widget.Toast.makeText(
                    requireContext(),
                    "${milestone.cupValue} kupaya ulaşınca açılır",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun closeFragment() {
        val main = activity as? MainActivity
        if (main != null) {
            main.finishTasksOverlayAnimated("CupPathRoadFragment.close")
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.cupPathRoadList.adapter = null
        _binding = null
    }

    // ── Liste ─────────────────────────────────────────────────────────

    private inner class MilestoneAdapter : RecyclerView.Adapter<MilestoneHolder>() {

        private var items: List<CupPathRewardRepository.Milestone> = emptyList()

        fun submit(newItems: List<CupPathRewardRepository.Milestone>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MilestoneHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cup_path_milestone, parent, false)
            return MilestoneHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: MilestoneHolder, position: Int) {
            holder.bind(items[position])
        }
    }

    private inner class MilestoneHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val track: View = itemView.findViewById(R.id.milestoneTrack)
        private val chestBox: View = itemView.findViewById(R.id.milestoneChestBox)
        private val chest: ImageView = itemView.findViewById(R.id.milestoneChest)
        private val done: ImageView = itemView.findViewById(R.id.milestoneDone)
        private val cupIcon: ImageView = itemView.findViewById(R.id.milestoneCupIcon)
        private val value: TextView = itemView.findViewById(R.id.milestoneValue)
        private val status: TextView = itemView.findViewById(R.id.milestoneStatus)

        fun bind(milestone: CupPathRewardRepository.Milestone) {
            value.text = milestone.cupValue.toString()

            val reached = milestone.status != CupPathRewardRepository.MilestoneStatus.LOCKED
            track.setBackgroundColor(if (reached) COLOR_ROAD_DONE else COLOR_ROAD_TODO)
            value.setTextColor(if (reached) COLOR_TEXT else COLOR_TEXT_DIM)
            cupIcon.alpha = if (reached) 1f else 0.5f

            when (milestone.status) {
                CupPathRewardRepository.MilestoneStatus.CLAIMED -> {
                    chestBox.setBackgroundResource(0)
                    chest.setImageResource(R.drawable.new_chest_open_ic2)
                    chest.alpha = 0.6f
                    done.visibility = View.VISIBLE
                    status.text = "ALINDI"
                    status.setTextColor(COLOR_TEXT_DIM)
                }
                CupPathRewardRepository.MilestoneStatus.CLAIMABLE -> {
                    chestBox.setBackgroundResource(R.drawable.bg_cup_path_milestone_ready)
                    chest.setImageResource(R.drawable.new_chest_close_ic2)
                    chest.alpha = 1f
                    done.visibility = View.GONE
                    status.text = "HAZIR"
                    status.setTextColor(COLOR_READY)
                }
                CupPathRewardRepository.MilestoneStatus.LOCKED -> {
                    chestBox.setBackgroundResource(0)
                    chest.setImageResource(R.drawable.new_chest_close_ic2)
                    chest.alpha = 0.45f
                    done.visibility = View.GONE
                    status.text = ""
                    status.setTextColor(COLOR_TEXT_DIM)
                }
            }

            itemView.setOnClickListener { onMilestoneTapped(milestone) }
        }
    }

    companion object {
        private const val ARG_CUP_FIELD = "cup_field"

        // `const val` değil: 0xFFFFB300 Int'e sığmadığı için `.toInt()` gerekiyor ve bu bir
        // derleme zamanı sabiti sayılmıyor.
        /** Geçilmiş yol. */
        private val COLOR_ROAD_DONE = 0xFFFFB300.toInt()
        /** Henüz ulaşılmamış yol. */
        private val COLOR_ROAD_TODO = 0xFF1A3040.toInt()
        private val COLOR_TEXT = 0xFFFFFFFF.toInt()
        private val COLOR_TEXT_DIM = 0xFF93A5B3.toInt()
        private val COLOR_READY = 0xFFFFB300.toInt()

        fun newInstance(cupField: String): CupPathRoadFragment =
            CupPathRoadFragment().apply {
                arguments = Bundle().apply { putString(ARG_CUP_FIELD, cupField) }
            }
    }
}
