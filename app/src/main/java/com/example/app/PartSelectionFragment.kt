package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.GlobalLessonData.globalPartId
import com.example.app.model.LessonItem

class PartSelectionFragment : Fragment() {

    /** Sobi'nin günlük testi düğmesi; haritadakinin aynısı (bkz. [MapSobiTestButton]). */
    private var sobiTestButton: MapSobiTestButton? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_part_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // lessonPartBackButton kapat (part secim ekraninda geri buton olmayacak)
        requireActivity().findViewById<ImageButton>(R.id.lessonPartBackButton)?.visibility = View.GONE

        sobiTestButton = MapSobiTestButton(this, view.findViewById(R.id.partSelectionRoot))

        loadStatesAndSetup(view)
    }

    override fun onResume() {
        super.onResume()
        sobiTestButton?.refresh()
    }

    override fun onDestroyView() {
        sobiTestButton?.release()
        sobiTestButton = null
        super.onDestroyView()
    }
    
    private fun loadStatesAndSetup(view: View) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val context = requireContext()
        
        GlobalLessonData.loadLessonItemsForPart(context, 1) { items1 ->
            val part1ChestComplete = items1.filter { it.type == LessonItem.TYPE_CHEST }.lastOrNull()?.stepIsFinish == true
            GlobalLessonData.loadLessonItemsForPart(context, 2) { items2 ->
                val part2ChestComplete = items2.filter { it.type == LessonItem.TYPE_CHEST }.lastOrNull()?.stepIsFinish == true
                GlobalLessonData.loadLessonItemsForPart(context, 3) { items3 ->
                    // 2–6. kartların kapısı: önceki bölümün SON DERSİ (maraton değil). Son ders anahtarla da
                    // açılabildiği için bölümler anahtar yoluyla da açılabiliyor.
                    val lastLessonDone = mapOf(1 to lastLessonFinished(items1), 2 to lastLessonFinished(items2), 3 to lastLessonFinished(items3))
                    
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        finalizeSetup(view, "Free", false, part1ChestComplete, part2ChestComplete, lastLessonDone)
                    } else {
                        firestore.collection("users").document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                // Süresi dolmuş abonelik Free sayılır; aksi halde aboneliği
                                // biten kullanıcı Pro derslerine erişmeye devam ederdi.
                                val plan = if (doc.exists()) PlanStatus.effectivePlan(doc) else "Free"
                                val teacherApproved = doc.getBoolean("teacherApproved") == true
                                finalizeSetup(view, plan, teacherApproved, part1ChestComplete, part2ChestComplete, lastLessonDone)
                            }
                            .addOnFailureListener {
                                finalizeSetup(view, "Free", false, part1ChestComplete, part2ChestComplete, lastLessonDone)
                            }
                    }
                }
            }
        }
    }
    
    /** Bölümün son DERS item'ı (maraton/başlık değil) bitmiş mi. */
    private fun lastLessonFinished(items: List<LessonItem>): Boolean =
        items.lastOrNull { it.type == LessonItem.TYPE_LESSON }?.stepIsFinish == true

    private fun lessonGatedState(partId: Int, prerequisitePart: Int, lastLessonDone: Map<Int, Boolean>): PartState {
        val open = lastLessonDone[prerequisitePart] == true
        return PartState(
            partId,
            visualActive = open,
            functionalActive = open,
            inactiveMessage = appString(R.string.ui_x_bolumunun_son_dersini_bitir, partTitle(prerequisitePart)),
        )
    }

    private fun partTitle(partId: Int): String = when (partId) {
        1 -> appString(R.string.ui_abakusun_temeli_ve_toplama)
        2 -> appString(R.string.ui_abakuste_cikarma)
        else -> appString(R.string.ui_abakuste_carpma)
    }

    private fun finalizeSetup(view: View, plan: String, teacherApproved: Boolean, part1ChestComplete: Boolean, part2ChestComplete: Boolean, lastLessonDone: Map<Int, Boolean>) {
        if (!isAdded) return

        val hasProPlan = PlanStatus.isProPlan(plan) || teacherApproved

        val getPartState = getPartState@{ partId: Int ->
            // Onaylı öğretmende bölüm sırası yok: dersleri tek seferde açılıyor
            // (GlobalLessonData.unlockLessonsForApprovedTeacherOnce) ama sandıkları bitmiş
            // sayılmıyor, yani aşağıdaki "önceki bölümü bitir" kuralı 2–8'i kapalı tutardı.
            if (teacherApproved) {
                return@getPartState PartState(partId, visualActive = true, functionalActive = true, inactiveMessage = "")
            }
            when (partId) {
                // Kapı önceki bölümün SON DERSİ (kullanıcı kararı 04.11.2026). Eskiden ünite
                // maratonuydu; maraton anahtarla açılamıyor, son ders açılabiliyor
                // (LessonAdapter.unlockLessonWithKey), yani bölüm de anahtar yoluyla açılabiliyor.
                1 -> PartState(1, visualActive = true, functionalActive = true, inactiveMessage = "")
                2, 3, 4 -> lessonGatedState(partId, prerequisitePart = 1, lastLessonDone)
                5 -> lessonGatedState(5, prerequisitePart = 2, lastLessonDone)
                6 -> lessonGatedState(6, prerequisitePart = 3, lastLessonDone)
                7 -> {
                    val functional = hasProPlan && part1ChestComplete
                    val msg = if (!hasProPlan) appString(R.string.ui_bu_kismi_acmak_icin_plani_pro) else appString(R.string.ui_abakuste_toplama_kismini_bitir)
                    PartState(7, visualActive = hasProPlan, functionalActive = functional, inactiveMessage = msg)
                }
                8 -> {
                    val functional = hasProPlan && part2ChestComplete
                    val msg = if (!hasProPlan) appString(R.string.ui_bu_kismi_acmak_icin_plani_pro) else appString(R.string.ui_abakuste_cikarma_kismini_bitir)
                    PartState(8, visualActive = hasProPlan, functionalActive = functional, inactiveMessage = msg)
                }
                else -> PartState(partId, visualActive = true, functionalActive = true, inactiveMessage = "")
            }
        }
        
        setupRecyclerView(view, R.id.additionRecyclerView, listOf(1, 4, 7).map { getPartState(it) })
        setupRecyclerView(view, R.id.subtractionRecyclerView, listOf(2, 5, 8).map { getPartState(it) })
        setupRecyclerView(view, R.id.multiplicationRecyclerView, listOf(3, 6).map { getPartState(it) })

        // Sobi'nin "… derslerine git" düğmesinden gelindi: bölüm açıksa haritası açılır, kapalıysa kartın
        // kendi kilit mesajı (karta dokunmakla aynı kural). Bir kez; geri dönünce yeniden açılmasın.
        val openPart = arguments?.getInt(ARG_OPEN_PART, 0) ?: 0
        if (openPart > 0) {
            arguments?.remove(ARG_OPEN_PART)
            val state = getPartState(openPart)
            if (state.functionalActive) openMapForPart(openPart)
            else if (state.inactiveMessage.isNotBlank()) {
                android.widget.Toast.makeText(requireContext(), state.inactiveMessage, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val ARG_OPEN_PART = "open_part"

        /** Ana ekran, açılınca [partId] bölümünün ders haritasına geçer (Sobi sekmesi, derse_git). */
        fun openingPart(partId: Int) = PartSelectionFragment().apply {
            arguments = Bundle().apply { putInt(ARG_OPEN_PART, partId) }
        }
    }

    private fun setupRecyclerView(root: View, recyclerViewId: Int, parts: List<PartState>) {
        val rv = root.findViewById<RecyclerView>(recyclerViewId)
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = PartCardAdapter(parts) { partId ->
            openMapForPart(partId)
        }
    }

    private fun openMapForPart(partId: Int) {
        if (partId == 7 || partId == 8) {
            val dummyItem = LessonItem(
                // Kalıcı değil: yalnızca yarış haritasını açmak için taşıyıcı.
                stableId = "transient_race_part$partId",
                type = LessonItem.TYPE_RACE,
                title = if (partId == 7) appString(R.string.ui_toplama_ustaligi) else appString(R.string.ui_cikarma_ustaligi),
                offset = 0,
                isCompleted = false,
                stepCount = 0,
                racePartId = partId,
                backRaceId = globalPartId // Güvenli olması adına mevcut ID'yi veriyoruz
            )
            val dummyAdapter = LessonAdapter(requireActivity(), mutableListOf())
            dummyAdapter.showRacePanel(dummyItem, -1, isFromPartSelection = true)
            return
        }

        globalPartId = partId
        GlobalLessonData.initialize(requireContext(), partId) {
            activity?.runOnUiThread {
                // lessonPartBackButton goster
                requireActivity().findViewById<ImageButton>(R.id.lessonPartBackButton)?.visibility = View.VISIBLE

                val mapFragment = MapFragment()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right,
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                    .replace(R.id.fragmentContainerID, mapFragment)
                    .addToBackStack("part_map")
                    .commitAllowingStateLoss()
            }
        }
    }
}
