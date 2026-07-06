package com.example.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentTutorialQuestionPanelBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Tutorial sonu anket ekranı.
 * TutorialFragment, Abacus veya BlindingLesson'a geçmeden önce bu paneli açar.
 * Kullanıcı gönder veya atla'ya bastıktan sonra "tutorialQuestionPanelResult"
 * fragment result'u tetiklenir ve TutorialFragment asıl geçişi yapar.
 *
 * Firestore hiyerarşisi:
 *   questionPanelTutorial/{globalPartId}/{mapFragmentIndex}/question{N}/
 *     - choice{1–5}  : FieldValue.increment(1) ile sayaç
 *     - text/{uid}   : { uid, text } sub-collection
 */
class TutorialQuestionPanelFragment : Fragment() {

    private var _binding: FragmentTutorialQuestionPanelBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** Gönderim devam ederken true — geri tuşu ve overlay tıklaması bloklanır. */
    private var isSending = false

    // Seçili seçenek indexleri (null = seçilmedi, 1–5 arası)
    private var q1SelectedChoice: Int? = null
    private var q2SelectedChoice: Int? = null

    private lateinit var backCallback: OnBackPressedCallback

    // ── Soru 1 row'ları ve checkbox'ları ──
    private val q1Rows: List<View> by lazy {
        listOf(
            binding.q1Option1Row, binding.q1Option2Row, binding.q1Option3Row,
            binding.q1Option4Row, binding.q1Option5Row
        )
    }
    private val q1Checks: List<AppCompatCheckBox> by lazy {
        listOf(
            binding.q1Option1Check, binding.q1Option2Check, binding.q1Option3Check,
            binding.q1Option4Check, binding.q1Option5Check
        )
    }

    // ── Soru 2 row'ları ve checkbox'ları ──
    private val q2Rows: List<View> by lazy {
        listOf(
            binding.q2Option1Row, binding.q2Option2Row, binding.q2Option3Row,
            binding.q2Option4Row, binding.q2Option5Row
        )
    }
    private val q2Checks: List<AppCompatCheckBox> by lazy {
        listOf(
            binding.q2Option1Check, binding.q2Option2Check, binding.q2Option3Check,
            binding.q2Option4Check, binding.q2Option5Check
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialQuestionPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Önceki ekrandan açık kalan bir klavye varsa kapat
        view.post {
            try {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            } catch (e: Exception) {}
        }

        setupBackPressBlock()
        setupQ1Options()
        setupQ2Options()
        setupTextWatchers()
        updateSendButtonState()

        binding.skipButton.setOnClickListener {
            if (isSending) return@setOnClickListener
            proceedToLesson()
        }

        binding.sendButton.setOnClickListener {
            if (isSending) return@setOnClickListener
            submitAndProceed()
        }

        // Overlay tıklaması: gönderim sırasında hiçbir şey yapılmaz
        binding.sendBlockingOverlay.setOnClickListener { /* blok */ }
    }

    // ────────────────────────────────────────────────────────────
    // Geri tuşu kontrolü
    // ────────────────────────────────────────────────────────────

    private fun setupBackPressBlock() {
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSending) return
                proceedToLesson()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    // ────────────────────────────────────────────────────────────
    // Seçenek mantığı — sadece 1 seçenek işaretlenebilir (radio)
    // ────────────────────────────────────────────────────────────

    private fun setupQ1Options() {
        q1Rows.forEachIndexed { index, row ->
            row.setOnClickListener {
                val choiceNumber = index + 1
                q1SelectedChoice = if (q1SelectedChoice == choiceNumber) null else choiceNumber
                updateQ1CheckboxVisuals()
                updateSendButtonState()
            }
        }
    }

    private fun setupQ2Options() {
        q2Rows.forEachIndexed { index, row ->
            row.setOnClickListener {
                val choiceNumber = index + 1
                q2SelectedChoice = if (q2SelectedChoice == choiceNumber) null else choiceNumber
                updateQ2CheckboxVisuals()
                updateSendButtonState()
            }
        }
    }

    private fun updateQ1CheckboxVisuals() {
        q1Checks.forEachIndexed { index, cb ->
            cb.isChecked = (q1SelectedChoice == index + 1)
        }
        q1Rows.forEachIndexed { index, row ->
            row.isSelected = (q1SelectedChoice == index + 1)
        }
    }

    private fun updateQ2CheckboxVisuals() {
        q2Checks.forEachIndexed { index, cb ->
            cb.isChecked = (q2SelectedChoice == index + 1)
        }
        q2Rows.forEachIndexed { index, row ->
            row.isSelected = (q2SelectedChoice == index + 1)
        }
    }

    // ────────────────────────────────────────────────────────────
    // Text watcher — herhangi bir editText dolu olursa Gönder aktif
    // ────────────────────────────────────────────────────────────

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.q1TextInput.addTextChangedListener(watcher)
        binding.q2TextInput.addTextChangedListener(watcher)
        binding.q3TextInput.addTextChangedListener(watcher)
    }

    // ────────────────────────────────────────────────────────────
    // Gönder butonu aktif/pasif durumu
    // ────────────────────────────────────────────────────────────

    private fun updateSendButtonState() {
        val hasAnyAnswer = q1SelectedChoice != null
                || q2SelectedChoice != null
                || binding.q1TextInput.text.isNotBlank()
                || binding.q2TextInput.text.isNotBlank()
                || binding.q3TextInput.text.isNotBlank()

        binding.sendButton.isEnabled = hasAnyAnswer
        binding.sendButton.backgroundTintList = if (hasAnyAnswer) {
            requireContext().getColorStateList(R.color.lesson_completed)
        } else {
            requireContext().getColorStateList(R.color.button_disabled)
        }
        binding.sendButton.setTextColor(
            if (hasAnyAnswer) requireContext().getColor(R.color.button_text_enabled)
            else requireContext().getColor(R.color.button_text_disabled)
        )
    }

    // ────────────────────────────────────────────────────────────
    // Gönderim
    // ────────────────────────────────────────────────────────────

    private fun submitAndProceed() {
        val globalPartId = arguments?.getInt(ARG_GLOBAL_PART_ID) ?: return
        val mapFragmentIndex = arguments?.getInt(ARG_MAP_FRAGMENT_INDEX) ?: return
        val uid = auth.currentUser?.uid

        val hasAnyAnswer = q1SelectedChoice != null
                || q2SelectedChoice != null
                || binding.q1TextInput.text.isNotBlank()
                || binding.q2TextInput.text.isNotBlank()
                || binding.q3TextInput.text.isNotBlank()
        if (!hasAnyAnswer) {
            proceedToLesson()
            return
        }

        setSendingUi(true)

        // questionPanelTutorial koleksiyonuna yaz
        val basePath = firestore
            .collection("questionPanelTutorial")
            .document(globalPartId.toString())
            .collection(mapFragmentIndex.toString())

        val tasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

        // ── Soru 1 ──
        val q1Choice = q1SelectedChoice
        val q1Text = binding.q1TextInput.text.toString().trim()
        if (q1Choice != null || q1Text.isNotEmpty()) {
            val q1Doc = basePath.document("question1")
            if (q1Choice != null) {
                tasks.add(
                    q1Doc.set(
                        mapOf("choice$q1Choice" to FieldValue.increment(1)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                )
            }
            if (q1Text.isNotEmpty() && uid != null) {
                tasks.add(
                    q1Doc.collection("text").document(uid)
                        .set(mapOf("uid" to uid, "text" to q1Text))
                )
            }
        }

        // ── Soru 2 ──
        val q2Choice = q2SelectedChoice
        val q2Text = binding.q2TextInput.text.toString().trim()
        if (q2Choice != null || q2Text.isNotEmpty()) {
            val q2Doc = basePath.document("question2")
            if (q2Choice != null) {
                tasks.add(
                    q2Doc.set(
                        mapOf("choice$q2Choice" to FieldValue.increment(1)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                )
            }
            if (q2Text.isNotEmpty() && uid != null) {
                tasks.add(
                    q2Doc.collection("text").document(uid)
                        .set(mapOf("uid" to uid, "text" to q2Text))
                )
            }
        }

        // ── Soru 3 (sadece metin) ──
        val q3Text = binding.q3TextInput.text.toString().trim()
        if (q3Text.isNotEmpty() && uid != null) {
            tasks.add(
                basePath.document("question3")
                    .collection("text").document(uid)
                    .set(mapOf("uid" to uid, "text" to q3Text))
            )
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                if (!isAdded) return@addOnCompleteListener
                setSendingUi(false)
                proceedToLesson()
            }
    }

    // ────────────────────────────────────────────────────────────
    // Loading UI
    // ────────────────────────────────────────────────────────────

    private fun setSendingUi(sending: Boolean) {
        isSending = sending
        binding.sendBlockingOverlay.visibility = if (sending) View.VISIBLE else View.GONE
        binding.sendButton.isEnabled = !sending
        binding.skipButton.isEnabled = !sending
    }

    // ────────────────────────────────────────────────────────────
    // Derse geçiş — TutorialFragment'e sinyal gönder
    // ────────────────────────────────────────────────────────────

    private fun proceedToLesson() {
        if (!isAdded) return
        parentFragmentManager.setFragmentResult("tutorialQuestionPanelResult", Bundle())
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_GLOBAL_PART_ID = "globalPartId"
        const val ARG_MAP_FRAGMENT_INDEX = "mapFragmentIndex"

        fun newInstance(
            globalPartId: Int,
            mapFragmentIndex: Int,
        ): TutorialQuestionPanelFragment {
            return TutorialQuestionPanelFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_GLOBAL_PART_ID, globalPartId)
                    putInt(ARG_MAP_FRAGMENT_INDEX, mapFragmentIndex)
                }
            }
        }
    }
}
