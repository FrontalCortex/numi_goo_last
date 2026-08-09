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

        loadStatesAndSetup(view)
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
                    val part3ChestComplete = items3.filter { it.type == LessonItem.TYPE_CHEST }.lastOrNull()?.stepIsFinish == true
                    
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        finalizeSetup(view, "Free", part1ChestComplete, part2ChestComplete, part3ChestComplete)
                    } else {
                        firestore.collection("users").document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                val plan = if (doc.exists()) doc.getString("plan") ?: "Free" else "Free"
                                finalizeSetup(view, plan, part1ChestComplete, part2ChestComplete, part3ChestComplete)
                            }
                            .addOnFailureListener {
                                finalizeSetup(view, "Free", part1ChestComplete, part2ChestComplete, part3ChestComplete)
                            }
                    }
                }
            }
        }
    }
    
    private fun finalizeSetup(view: View, plan: String, part1ChestComplete: Boolean, part2ChestComplete: Boolean, part3ChestComplete: Boolean) {
        if (!isAdded) return
        
        val hasProPlan = plan.equals("Pro", ignoreCase = true) || plan.equals("Premium", ignoreCase = true)
        
        val getPartState = { partId: Int ->
            when (partId) {
                1 -> PartState(1, visualActive = true, functionalActive = true, inactiveMessage = "")
                2 -> PartState(2, visualActive = part1ChestComplete, functionalActive = part1ChestComplete, inactiveMessage = "Abaküsün Temeli ve Toplama kısmını bitir.")
                3 -> PartState(3, visualActive = part1ChestComplete, functionalActive = part1ChestComplete, inactiveMessage = "Abaküsün Temeli ve Toplama kısmını bitir.")
                4 -> PartState(4, visualActive = part1ChestComplete, functionalActive = part1ChestComplete, inactiveMessage = "Abaküsün Temeli ve Toplama kısmını bitir.")
                5 -> PartState(5, visualActive = part2ChestComplete, functionalActive = part2ChestComplete, inactiveMessage = "Abaküste Çıkarma kısmını bitir.")
                6 -> PartState(6, visualActive = part3ChestComplete, functionalActive = part3ChestComplete, inactiveMessage = "Abaküste Çarpma kısmını bitir.")
                7 -> {
                    val functional = hasProPlan && part1ChestComplete
                    val msg = if (!hasProPlan) "Bu kısmı açmak için planı Pro'ya yükselt." else "Abaküste Toplama kısmını bitir."
                    PartState(7, visualActive = hasProPlan, functionalActive = functional, inactiveMessage = msg)
                }
                8 -> {
                    val functional = hasProPlan && part2ChestComplete
                    val msg = if (!hasProPlan) "Bu kısmı açmak için planı Pro'ya yükselt." else "Abaküste Çıkarma kısmını bitir."
                    PartState(8, visualActive = hasProPlan, functionalActive = functional, inactiveMessage = msg)
                }
                else -> PartState(partId, visualActive = true, functionalActive = true, inactiveMessage = "")
            }
        }
        
        setupRecyclerView(view, R.id.additionRecyclerView, listOf(1, 4, 7).map { getPartState(it) })
        setupRecyclerView(view, R.id.subtractionRecyclerView, listOf(2, 5, 8).map { getPartState(it) })
        setupRecyclerView(view, R.id.multiplicationRecyclerView, listOf(3, 6).map { getPartState(it) })
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
                type = LessonItem.TYPE_RACE,
                title = if (partId == 7) "Toplama Ustalığı" else "Çıkarma Ustalığı",
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
