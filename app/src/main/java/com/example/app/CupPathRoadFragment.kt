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

    /**
     * Sandık açılıp geri dönüldüğünde görünümün yeniden kurulması listeyi başa atıyordu.
     * Ayrılırken ilk görünen eşik buraya yazılıyor, liste dolunca oraya geri dönülüyor.
     * Konum indeks yerine EŞİK DEĞERİ olarak saklanıyor: liste penceresi kayarsa indeks
     * başka bir eşiğe denk gelirdi.
     */
    private var pendingScrollMilestone: Int? = null

    /**
     * Kapalı sandık çizimi kendi kutusunun ortasının altında duruyor; yolun şeridine göre
     * ortalanması için bu kadar yukarı kaydırılıyor. Açık sandıkta bu sorun yok.
     */
    private val closedChestLiftPx: Float by lazy {
        resources.getDimensionPixelSize(R.dimen.cup_path_road_chest_center_offset).toFloat()
    }

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

        // Sandıktan dönüldü: liste elimizdeki verilerle zaten dolu, o yüzden okuma
        // beklenmeden eski konuma dönülüyor. Beklenseydi liste bir an başa atlar, sonra
        // yerine sıçrardı. [pendingScrollMilestone] temizlenmiyor; taze veri gelince
        // [render] konumu son listeye göre bir kez daha oturtuyor.
        pendingScrollMilestone?.let { restore ->
            val index = adapter.indexOfMilestone(restore)
            if (index >= 0) {
                (binding.cupPathRoadList.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(index, 0)
            }
        }
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
        // Dolgu, parlama ve renkler karttakiyle aynı yardımcıdan geçiyor; genişlik ölçüm
        // bitmeden bilinmediği için post ile ölçüm sonrasına bırakılıyor.
        val zone = binding.cupPathRoadProgressZone
        val fill = binding.cupPathRoadProgressFill
        val shine = binding.cupPathRoadProgressShine
        zone.post {
            if (_binding == null || zone.width <= 0) return@post
            applyDailyQuestionProgressOverlayNow(
                widthHost = zone,
                fill = fill,
                shine = shine,
                percent = state.fraction * 100f,
                complete = state.claimable,
            )
        }

        val milestones = state.milestones()
        adapter.submit(milestones)

        val layoutManager = binding.cupPathRoadList.layoutManager as? LinearLayoutManager
        val restore = pendingScrollMilestone
        if (restore != null) {
            pendingScrollMilestone = null
            scrolledToCurrent = true
            val index = milestones.indexOfFirst { it.cupValue == restore }
            if (index >= 0) layoutManager?.scrollToPositionWithOffset(index, 0)
        } else if (!scrolledToCurrent) {
            // Kullanıcının bulunduğu eşik: ilk alınmamış olan. Hepsi alınmışsa liste sonu.
            val index = milestones.indexOfFirst {
                it.status != CupPathRewardRepository.MilestoneStatus.CLAIMED
            }.takeIf { it >= 0 } ?: (milestones.size - 1)
            if (index >= 0) {
                scrolledToCurrent = true
                // Sola bir öğelik pay bırakılıyor ki kullanıcı geçtiği eşiği de görsün ve
                // listenin devam ettiği anlaşılsın.
                layoutManager?.scrollToPositionWithOffset(maxOf(0, index - 1), 0)
            }
        }
    }

    private fun onMilestoneTapped(milestone: CupPathRewardRepository.Milestone) {
        if (!isAdded) return
        when (milestone.status) {
            CupPathRewardRepository.MilestoneStatus.CLAIMABLE -> {
                // Sandık geri yığına eklenerek açılıyor: kapanınca bu ekran geri geliyor,
                // kullanıcı yolun neresinde kaldıysa orada devam ediyor. Adı
                // NewChestFragment de tanıyor (görev/mağaza sandıklarındaki kalıbın aynısı).
                (activity as? MainActivity)?.showAbacusOverlayFragment(
                    NewChestFragment.newInstance(
                        NewChestFragment.ChestRarity.COMMON,
                        source = AnalyticsLogger.CHEST_SOURCE_CUP_PATH,
                        cupField = cupField,
                        cupMilestone = milestone.cupValue,
                    )
                ) {
                    addToBackStack(NewChestFragment.BACK_STACK_CUP_PATH_ROAD)
                }
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
        // Sandık açılırken görünüm yok ediliyor; geri dönüşte aynı yere dönebilmek için
        // ekrandaki ilk eşik saklanıyor.
        val first = (binding.cupPathRoadList.layoutManager as? LinearLayoutManager)
            ?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
        pendingScrollMilestone = adapter.milestoneAt(first)
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

        /** Konumdaki eşik değeri; konum geçersizse null. */
        fun milestoneAt(position: Int): Int? = items.getOrNull(position)?.cupValue

        /** Eşiğin listedeki sırası; listede yoksa -1. */
        fun indexOfMilestone(milestone: Int): Int = items.indexOfFirst { it.cupValue == milestone }

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
                    chest.setImageResource(R.drawable.new_chest_open_ic1)
                    // Açık sandık çizimi kendi kutusunda zaten ortalı; kaydırmaya gerek yok.
                    chest.translationY = 0f
                    chest.alpha = 0.6f
                    done.visibility = View.VISIBLE
                    status.text = "ALINDI"
                    status.setTextColor(COLOR_TEXT_DIM)
                }
                CupPathRewardRepository.MilestoneStatus.CLAIMABLE -> {
                    chestBox.setBackgroundResource(R.drawable.bg_cup_path_milestone_ready)
                    chest.setImageResource(R.drawable.new_chest_close_ic1)
                    chest.translationY = -closedChestLiftPx
                    chest.alpha = 1f
                    done.visibility = View.GONE
                    status.text = "HAZIR"
                    status.setTextColor(COLOR_READY)
                }
                CupPathRewardRepository.MilestoneStatus.LOCKED -> {
                    chestBox.setBackgroundResource(0)
                    chest.setImageResource(R.drawable.new_chest_close_ic1)
                    chest.translationY = -closedChestLiftPx
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
