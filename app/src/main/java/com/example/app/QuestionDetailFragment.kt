package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.app.databinding.FragmentQuestionDetailBinding
import com.example.app.model.StudentQuestion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class QuestionDetailFragment : Fragment() {

    private var _binding: FragmentQuestionDetailBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val questionId = arguments?.getString(ARG_QUESTION_ID) ?: return
        val studentView = arguments?.getBoolean(ARG_STUDENT_VIEW, false) == true

        binding.backButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        binding.claimButton.visibility = if (studentView) View.GONE else View.VISIBLE

        firestore.collection("questions").document(questionId).get()
            .addOnSuccessListener { doc ->
                val q = doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id) ?: return@addOnSuccessListener
                binding.messageText.text = q.message
                binding.contentLoadingProgress.visibility = View.GONE
                binding.backButton.visibility = View.VISIBLE
                binding.contentScrollView.visibility = View.VISIBLE
                if (!studentView) {
                    binding.claimButton.visibility = View.VISIBLE
                    binding.claimButton.setOnClickListener { claimQuestion(questionId) }
                }
                if (!q.screenshotUrl.isNullOrEmpty()) {
                    Glide.with(this).load(q.screenshotUrl).into(binding.screenshotImage)
                }
            }
            .addOnFailureListener {
                binding.contentLoadingProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Soru yüklenemedi.", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
    }

    private fun claimQuestion(questionId: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>(
            "status" to StudentQuestion.STATUS_CLAIMED,
            "claimedByTeacherUid" to uid,
            "claimedAt" to Timestamp.now()
        )
        firestore.collection("questions").document(questionId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Soruyu sahiplendiniz.", Toast.LENGTH_SHORT).show()
                val fm = requireActivity().supportFragmentManager
                fm.popBackStackImmediate()
                fm.beginTransaction()
                    .replace(R.id.fragmentContainerID, QuestionChatFragment.newInstance(questionId))
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_QUESTION_ID = "question_id"
        private const val ARG_STUDENT_VIEW = "student_view"
        fun newInstance(questionId: String, studentView: Boolean = false): QuestionDetailFragment {
            return QuestionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUESTION_ID, questionId)
                    putBoolean(ARG_STUDENT_VIEW, studentView)
                }
            }
        }
    }
}


