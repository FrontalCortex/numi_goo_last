package com.example.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.app.databinding.FragmentTasksBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


class TasksFragment : Fragment() {
    private lateinit var binding: FragmentTasksBinding
    companion object {
        private const val PRACTICE_TOUCH_BLOCKER_TAG = "practice_touch_blocker"
        private const val DAILY_QUESTION_PREFS = "daily_question_prefs"
        private const val FIRESTORE_DAILY_QUESTION = "dailyQuestion"
    }

    private data class BulletinCard(
        val id: String,
        val title: String,
        val subtitle: String
    )

    private class BulletinAdapter(
        private val onClick: (BulletinCard) -> Unit
    ) : ListAdapter<BulletinCard, BulletinAdapter.VH>(
        object : DiffUtil.ItemCallback<BulletinCard>() {
            override fun areItemsTheSame(oldItem: BulletinCard, newItem: BulletinCard): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BulletinCard, newItem: BulletinCard): Boolean =
                oldItem == newItem
        }
    ) {
        class VH(itemView: View, private val onClick: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.bulletinCardTitle)
            val subtitle: TextView = itemView.findViewById(R.id.bulletinCardSubtitle)
            init {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onClick(pos)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bulletin_card, parent, false)
            return VH(v) { pos -> onClick(getItem(pos)) }
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.title.text = item.title
            holder.subtitle.text = item.subtitle
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTasksBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BulletinAdapter { card ->
            // Geçiş animasyonu boyunca ekrandaki tüm dokunuşları engelle.
            val content = requireActivity().findViewById<ViewGroup>(android.R.id.content)
            content.findViewWithTag<View>(PRACTICE_TOUCH_BLOCKER_TAG)?.let { content.removeView(it) }
            val blocker = View(requireContext()).apply {
                tag = PRACTICE_TOUCH_BLOCKER_TAG
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = true
                isFocusable = true
                setOnTouchListener { _, _ -> true }
                elevation = 1000f
            }
            content.addView(blocker)

            when (card.id) {
                "daily_question_card" -> startDailyQuestionFlow()
                else -> openAbacusContainerFragment(AbacusPracticeFragment())
            }
        }

        binding.tasksRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.tasksRecycler.adapter = adapter

        adapter.submitList(
            listOf(
                BulletinCard(
                    id = "daily_card",
                    title = "Abaküs",
                    subtitle = "Abaküste pratik yaparak kendini geliştir."
                ),
                BulletinCard(
                    id = "daily_question_card",
                    title = "Günlük Soru",
                    subtitle = "Ders ilerlemene göre her gün yeni abaküs sorusu."
                )
            )
        )
    }

    private fun startDailyQuestionFlow() {
        isDailyQuestionAlreadySolved { alreadySolved ->
            if (!isAdded) return@isDailyQuestionAlreadySolved
            if (alreadySolved) {
                Toast.makeText(
                    requireContext(),
                    "Günlük soruyu bugün zaten çözdün.",
                    Toast.LENGTH_SHORT,
                ).show()
                releaseLaunchTouchBlocker()
                return@isDailyQuestionAlreadySolved
            }

            GlobalLessonData.getFinishedChestItemsAcrossParts(
                context = requireContext(),
            ) { chestRefs ->
                if (!isAdded) return@getFinishedChestItemsAcrossParts
                val generatorPool = chestRefs.map { chestRef ->
                    {
                        val record = chestRef.item.record
                        val cupPoint1 = chestRef.item.cupPoint1
                        val cupPoint2 = chestRef.item.cupPoint2
                        when {
                            record != null && cupPoint1 != null && record >= cupPoint1 ->
                                MathOperationGenerator.generateSequence1Digits(4, 4)
                            record != null && cupPoint2 != null && record >= cupPoint2 ->
                                MathOperationGenerator.generateSequence1Digits(4, 3)
                            else ->
                                MathOperationGenerator.generateSequence1Digits(3, 2)
                        }
                    }
                }

                if (generatorPool.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Burayı açmak için daha fazla ders tamamla.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    releaseLaunchTouchBlocker()
                    return@getFinishedChestItemsAcrossParts
                }

                resolveDailyStableSequence(generatorPool) { generatedSequence ->
                    if (!isAdded) return@resolveDailyStableSequence
                    val safeSequence = if (generatedSequence.isEmpty()) {
                        MathOperationGenerator.generateSequence1Digits(3, 2)
                    } else {
                        generatedSequence
                    }

                    openAbacusContainerFragment(
                        BlindingLessonFragment.newDailyQuestionInstance(listOf(safeSequence)),
                    )
                }
            }
        }
    }

    private fun isDailyQuestionAlreadySolved(onResult: (Boolean) -> Unit) {
        val prefs = requireContext().getSharedPreferences(DAILY_QUESTION_PREFS, android.content.Context.MODE_PRIVATE)
        val dayKey = currentDayKey()
        val uidKey = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val localSolved = prefs.getBoolean("daily_solved_${uidKey}_$dayKey", false)
        if (localSolved) {
            onResult(true)
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResult(false)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(FIRESTORE_DAILY_QUESTION)
            .document(dayKey)
            .get()
            .addOnSuccessListener { doc ->
                val solved = doc.getBoolean("solved") == true
                if (solved) {
                    prefs.edit().putBoolean("daily_solved_${uidKey}_$dayKey", true).apply()
                }
                onResult(solved)
            }
            .addOnFailureListener {
                onResult(localSolved)
            }
    }

    private fun resolveDailyStableSequence(
        generatorPool: List<() -> List<Int>>,
        onResult: (List<Int>) -> Unit,
    ) {
        val prefs = requireContext().getSharedPreferences(DAILY_QUESTION_PREFS, android.content.Context.MODE_PRIVATE)
        val dayKey = currentDayKey()
        val uidKey = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val storageKey = "daily_sequence_${uidKey}_$dayKey"

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection(FIRESTORE_DAILY_QUESTION)
                .document(dayKey)
                .get()
                .addOnSuccessListener { doc ->
                    val remoteCsv = doc.getString("sequence")
                    if (!remoteCsv.isNullOrBlank()) {
                        prefs.edit().putString(storageKey, remoteCsv).apply()
                        val parsedRemote = remoteCsv.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                        if (parsedRemote.isNotEmpty()) {
                            onResult(parsedRemote)
                            return@addOnSuccessListener
                        }
                    }
                    resolveSequenceFromLocalOrGenerate(
                        prefs = prefs,
                        storageKey = storageKey,
                        dayKey = dayKey,
                        uid = uid,
                        generatorPool = generatorPool,
                        onResult = onResult,
                    )
                }
                .addOnFailureListener {
                    resolveSequenceFromLocalOrGenerate(
                        prefs = prefs,
                        storageKey = storageKey,
                        dayKey = dayKey,
                        uid = uid,
                        generatorPool = generatorPool,
                        onResult = onResult,
                    )
                }
            return
        }

        resolveSequenceFromLocalOrGenerate(
            prefs = prefs,
            storageKey = storageKey,
            dayKey = dayKey,
            uid = null,
            generatorPool = generatorPool,
            onResult = onResult,
        )
    }

    private fun resolveSequenceFromLocalOrGenerate(
        prefs: android.content.SharedPreferences,
        storageKey: String,
        dayKey: String,
        uid: String?,
        generatorPool: List<() -> List<Int>>,
        onResult: (List<Int>) -> Unit,
    ) {
        val cached = prefs.getString(storageKey, null)
        if (!cached.isNullOrBlank()) {
            val parsed = cached.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
            if (parsed.isNotEmpty()) {
                onResult(parsed)
                return
            }
        }

        val selectedIndex = dailyStableIndex(generatorPool.size)
        val generated = generatorPool[selectedIndex].invoke()
        if (generated.isNotEmpty()) {
            val csv = generated.joinToString(",")
            prefs.edit().putString(storageKey, csv).apply()
            if (uid != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection(FIRESTORE_DAILY_QUESTION)
                    .document(dayKey)
                    .set(
                        mapOf(
                            "sequence" to csv,
                            "solved" to false,
                        ),
                    )
            }
        }
        onResult(generated)
    }

    private fun currentDayKey(): String {
        val now = Calendar.getInstance()
        return "${now.get(Calendar.YEAR)}-${now.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun dailyStableIndex(poolSize: Int): Int {
        if (poolSize <= 1) return 0
        val dayKey = currentDayKey()
        val uidKey = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val seed = "$uidKey|$dayKey|$poolSize".hashCode()
        return Math.floorMod(seed, poolSize)
    }

    private fun openAbacusContainerFragment(targetFragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,   // enter
                R.anim.slide_out_left,   // exit
                R.anim.slide_in_left,    // popEnter
                R.anim.slide_out_right   // popExit
            )
            .replace(R.id.abacusFragmentContainer, targetFragment)
            .hide(this@TasksFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun releaseLaunchTouchBlocker() {
        val content = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.findViewWithTag<View>(PRACTICE_TOUCH_BLOCKER_TAG)?.let { blocker ->
            content.removeView(blocker)
        }
    }
}