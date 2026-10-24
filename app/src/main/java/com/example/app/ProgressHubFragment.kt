package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentProgressHubBinding
import com.example.app.homework.DepthPress
import com.example.app.homework.HomeworkRuleStatsFragment

/**
 * Öğrencinin ilerleme sayfasının girişi: iki kart.
 *  - Dersler → [ParentPanelFragment] (önceden doğrudan açılan ekran, aynen)
 *  - Ödev başarısı → [HomeworkRuleStatsFragment] (ödevlerde kural kural doğru oranı)
 *
 * Veli (Ayarlar → Veli paneli) ve kurs öğretmeni (öğrencinin profilindeki ilerleme düğmesi) buradan
 * giriyor; öğretmende başlık öğrencinin adı, argümanlar iki ekrana da aynen geçiyor.
 */
class ProgressHubFragment : Fragment() {

    companion object {
        private const val ARG_STUDENT_UID = "student_uid"
        private const val ARG_STUDENT_NAME = "student_name"

        fun newForStudent(studentUid: String, studentName: String) = ProgressHubFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_STUDENT_UID, studentUid)
                putString(ARG_STUDENT_NAME, studentName)
            }
        }
    }

    private var _binding: FragmentProgressHubBinding? = null
    private val binding get() = _binding!!

    private val studentUid: String? get() = arguments?.getString(ARG_STUDENT_UID)
    private val studentName: String get() = arguments?.getString(ARG_STUDENT_NAME).orEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProgressHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        val uid = studentUid
        if (uid != null) binding.hubTitle.text = appString(R.string.class_student_progress_title, studentName)
        binding.cardLessons.setOnClickListener {
            open(if (uid != null) ParentPanelFragment.newForStudent(uid, studentName) else ParentPanelFragment())
        }
        binding.cardRules.setOnClickListener { open(HomeworkRuleStatsFragment.newInstance(uid, studentName)) }
        DepthPress.attach(binding.cardLessons, binding.cardRules)
    }

    private fun open(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainerID, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
