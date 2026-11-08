package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentTutorialQuestionPanelBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Öğretici sonu kısa anketi: "Bu dersi anladın mı?" ve üç ifade (😊 / 😐 / 😕).
 *
 * TutorialFragment derse geçmeden önce bu paneli açar; yalnızca ünite başlarında ve öğretici
 * başına bir kez ([TutorialSurveyPolicy.takeEndSurveySlot]). Panel kapanınca
 * "tutorialQuestionPanelResult" fragment result'u tetiklenir ve TutorialFragment asıl geçişi yapar.
 *
 * Eskiden iki 5'li ölçek sorusu ve üç yazı kutusu vardı (08.11.2026'ya kadar). Kullanıcıların bir
 * kısmı 7–8 yaşında; "ne yeterli ne yetersizdi" gibi şıklar onlara ağırdı ve cevapların %80'i
 * zaten 4–5'ti. Şimdi bir ifade seçilir (fikir değiştirilebilir), "Devam" ile derse geçilir;
 * 😕 seçiliyken "neresi zor geldi?" kutusu açılır. (09.11.2026'ya kadar 😊/😐 dokununca 250 ms
 * sonra kendiliğinden geçiyor, seçim kilitleniyordu; kullanıcı yanlış dokununca düzeltemiyordu.)
 *
 * ## Cevaplar nereye gidiyor
 * - **İfade → Firebase Analytics**: `lesson_survey_choice`, `survey_type = tutorial_quick`,
 *   `question_no = 1`, `choice` 1 = anlamadım, 2 = biraz, 3 = anladım.
 * - **Yazı → Firestore**, eski 3. sorunun yolu: `questionPanelTutorial/{part}/{ders}/question3/text/{uid}`.
 *   Çocuğun yazdığı metin Analytics'e gönderilmez; oraya yalnızca "metin yazıldı" bilgisi gider.
 * - **Hiçbir şey seçmeden Atla / geri → `lesson_survey_skip`**. Anketin ne kadar atlandığı
 *   eskiden ölçülmüyordu.
 */
class TutorialQuestionPanelFragment : Fragment() {

    private var _binding: FragmentTutorialQuestionPanelBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** Yazı gönderilirken true: geri tuşu ve düğmeler beklemede. */
    private var isSending = false

    /** Seçilen ifade (CHOICE_*); null = henüz seçilmedi. */
    private var selectedChoice: Int? = null

    /** Son seçim Analytics'e bir kez yazılır (Devam ve geri çakışırsa iki kez gitmesin). */
    private var choiceLogged = false

    private val globalPartId: Int get() = arguments?.getInt(ARG_GLOBAL_PART_ID) ?: 0
    private val lessonId: String get() = arguments?.getString(ARG_LESSON_ID).orEmpty()

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
        hideKeyboard()

        binding.optionYesEmoji.text = EMOJI_YES
        binding.optionSomeEmoji.text = EMOJI_SOME
        binding.optionNoEmoji.text = EMOJI_NO

        binding.optionYes.setOnClickListener { onChoice(CHOICE_YES) }
        binding.optionSome.setOnClickListener { onChoice(CHOICE_SOME) }
        binding.optionNo.setOnClickListener { onChoice(CHOICE_NO) }

        binding.continueButton.setOnClickListener {
            if (!isSending) submitAndProceed()
        }
        binding.skipButton.setOnClickListener {
            if (!isSending) leave()
        }
        binding.sendBlockingOverlay.setOnClickListener { /* gönderim sırasında dokunuşu yut */ }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!isSending) leave()
                }
            },
        )
    }

    /**
     * İfade seçimi: istediği kadar değiştirebilir, geçiş yalnızca "Devam" ile olur.
     * Analytics'e burada değil, son seçim kesinleşince ([submitAndProceed]) yazılır;
     * böylece fikir değiştirmeler sayıları şişirmez.
     */
    private fun onChoice(choice: Int) {
        if (isSending || selectedChoice == choice) return
        selectedChoice = choice
        binding.optionYes.isSelected = choice == CHOICE_YES
        binding.optionSome.isSelected = choice == CHOICE_SOME
        binding.optionNo.isSelected = choice == CHOICE_NO
        binding.continueButton.visibility = View.VISIBLE
        // Seçimden sonra "Atla" anlamsız; geri tuşu yine seçimi gönderip geçer.
        binding.skipButton.visibility = View.GONE

        if (choice != CHOICE_NO) {
            // 😕'dan vazgeçtiyse kutu kapanır; yazdığı metin silinmez ama gönderilmez.
            binding.whyGroup.visibility = View.GONE
            hideKeyboard()
            return
        }
        binding.whyGroup.visibility = View.VISIBLE
        binding.questionScrollView.post {
            _binding?.let { it.questionScrollView.smoothScrollTo(0, it.whyGroup.top) }
        }
    }

    /** Atla ya da geri: hiçbir ifade seçilmediyse "atlandı" olarak ölçülür. */
    private fun leave() {
        if (selectedChoice == null) {
            AnalyticsLogger.logSurveySkip(AnalyticsLogger.SURVEY_TUTORIAL_QUICK, globalPartId, lessonId)
            proceedToLesson()
            return
        }
        submitAndProceed()
    }

    /** Son seçimi Analytics'e yazar; 😕 ise ve metin varsa Firestore'a gönderir, sonra derse geçer. */
    private fun submitAndProceed() {
        val choice = selectedChoice ?: return leave()
        if (!choiceLogged) {
            choiceLogged = true
            AnalyticsLogger.logSurveyChoice(
                AnalyticsLogger.SURVEY_TUTORIAL_QUICK, globalPartId, lessonId, 1, choice,
            )
        }
        if (choice != CHOICE_NO) {
            proceedToLesson()
            return
        }
        val text = _binding?.whyTextInput?.text?.toString()?.trim().orEmpty()
        val uid = auth.currentUser?.uid
        // Kimliksiz ya da derssiz cevabı saklayacak yer yok; akış yine de asla durmamalı.
        if (text.isEmpty() || uid == null || lessonId.isBlank()) {
            proceedToLesson()
            return
        }
        AnalyticsLogger.logSurveyText(AnalyticsLogger.SURVEY_TUTORIAL_QUICK, globalPartId, lessonId, 1)
        setSendingUi(true)
        firestore.collection("questionPanelTutorial")
            .document(globalPartId.toString())
            .collection(lessonId)
            .document("question3")
            .collection("text").document(uid)
            .set(mapOf("uid" to uid, "text" to text, "choice" to selectedChoice))
            .addOnCompleteListener {
                if (!isAdded) return@addOnCompleteListener
                setSendingUi(false)
                proceedToLesson()
            }
    }

    private fun setSendingUi(sending: Boolean) {
        isSending = sending
        val b = _binding ?: return
        b.sendBlockingOverlay.visibility = if (sending) View.VISIBLE else View.GONE
        b.continueButton.isEnabled = !sending
        b.skipButton.isEnabled = !sending
    }

    private fun hideKeyboard() {
        val v = view ?: return
        v.post {
            try {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            } catch (_: Exception) {
            }
        }
    }

    private var proceeded = false

    private fun proceedToLesson() {
        if (!isAdded || proceeded) return
        proceeded = true
        hideKeyboard()
        parentFragmentManager.setFragmentResult("tutorialQuestionPanelResult", Bundle())
        // Sola kayarak kapanıyor: ders ekranı sağdan geliyor, ikisi tek bir itme gibi okunuyor.
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(0, R.anim.queue_screen_out)
            .remove(this)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_GLOBAL_PART_ID = "globalPartId"
        const val ARG_LESSON_ID = "lessonId"

        const val CHOICE_NO = 1
        const val CHOICE_SOME = 2
        const val CHOICE_YES = 3

        private const val EMOJI_YES = "😊"   // 😊
        private const val EMOJI_SOME = "😐"  // 😐
        private const val EMOJI_NO = "😕"    // 😕

        fun newInstance(
            globalPartId: Int,
            lessonId: String,
        ): TutorialQuestionPanelFragment {
            return TutorialQuestionPanelFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_GLOBAL_PART_ID, globalPartId)
                    putString(ARG_LESSON_ID, lessonId)
                }
            }
        }
    }
}
