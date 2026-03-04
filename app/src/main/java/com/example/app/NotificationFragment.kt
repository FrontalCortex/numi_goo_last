package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentNotificationBinding
import com.example.app.model.StudentQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NotificationFragment : Fragment() {

    private enum class TeacherTab { POOL, CHATS }
    private enum class StudentTab { BEKLEYEN, COZULEN }

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val authManager by lazy { AuthManager().also { it.initialize(requireContext()) } }
    private var listener: ListenerRegistration? = null
    private var isTeacher = false
    private var teacherTab = TeacherTab.POOL
    private var studentTab = StudentTab.BEKLEYEN

    private val adapter = QuestionListAdapter { question -> onQuestionClick(question) }

    private companion object {
        private const val KEY_TEACHER_TAB = "teacher_tab"
        private const val KEY_STUDENT_TAB = "student_tab"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TEACHER_TAB, teacherTab.name)
        outState.putString(KEY_STUDENT_TAB, studentTab.name)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.questionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.questionsRecyclerView.adapter = adapter

        if (savedInstanceState != null) {
            savedInstanceState.getString(KEY_TEACHER_TAB)?.let { name ->
                kotlin.runCatching { teacherTab = TeacherTab.valueOf(name) }
            }
            savedInstanceState.getString(KEY_STUDENT_TAB)?.let { name ->
                kotlin.runCatching { studentTab = StudentTab.valueOf(name) }
            }
        }

        isTeacher = authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER
        if (isTeacher) {
            binding.notificationTitle.visibility = View.GONE
            binding.teacherTabContainer.visibility = View.VISIBLE
            updateTeacherTabUi()
            binding.tabPool.setOnClickListener { switchToTeacherTab(TeacherTab.POOL) }
            binding.tabChats.setOnClickListener { switchToTeacherTab(TeacherTab.CHATS) }
            if (teacherTab == TeacherTab.POOL) subscribeToPool() else subscribeToChats()
        } else {
            binding.notificationTitle.visibility = View.VISIBLE
            binding.notificationTitle.text = "Sorularım"
            binding.teacherTabContainer.visibility = View.GONE
            binding.studentTabContainer.visibility = View.VISIBLE
            updateStudentTabUi()
            binding.tabBekleyen.setOnClickListener { switchToStudentTab(StudentTab.BEKLEYEN) }
            binding.tabCozulen.setOnClickListener { switchToStudentTab(StudentTab.COZULEN) }
            if (studentTab == StudentTab.BEKLEYEN) subscribeToStudentPending() else subscribeToStudentResolved()
        }
    }

    private fun updateStudentTabUi() {
        val selectedBg = R.drawable.hint_background
        val selectedColor = android.graphics.Color.WHITE
        val unselectedColor = 0xFFAAAAAA.toInt()
        if (studentTab == StudentTab.BEKLEYEN) {
            binding.tabBekleyen.setBackgroundResource(selectedBg)
            binding.tabBekleyen.setTextColor(selectedColor)
            binding.tabBekleyen.paint.isFakeBoldText = true
            binding.tabCozulen.setBackgroundResource(0)
            binding.tabCozulen.setTextColor(unselectedColor)
            binding.tabCozulen.paint.isFakeBoldText = false
        } else {
            binding.tabCozulen.setBackgroundResource(selectedBg)
            binding.tabCozulen.setTextColor(selectedColor)
            binding.tabCozulen.paint.isFakeBoldText = true
            binding.tabBekleyen.setBackgroundResource(0)
            binding.tabBekleyen.setTextColor(unselectedColor)
            binding.tabBekleyen.paint.isFakeBoldText = false
        }
    }

    private fun switchToStudentTab(tab: StudentTab) {
        if (studentTab == tab) return
        studentTab = tab
        updateStudentTabUi()
        listener?.remove()
        listener = null
        if (tab == StudentTab.BEKLEYEN) subscribeToStudentPending() else subscribeToStudentResolved()
    }

    private fun updateTeacherTabUi() {
        val selectedBg = R.drawable.hint_background
        val selectedColor = android.graphics.Color.WHITE
        val unselectedColor = 0xFFAAAAAA.toInt()
        if (teacherTab == TeacherTab.POOL) {
            binding.tabPool.setBackgroundResource(selectedBg)
            binding.tabPool.setTextColor(selectedColor)
            binding.tabPool.paint.isFakeBoldText = true
            binding.tabChats.setBackgroundResource(0)
            binding.tabChats.setTextColor(unselectedColor)
            binding.tabChats.paint.isFakeBoldText = false
        } else {
            binding.tabChats.setBackgroundResource(selectedBg)
            binding.tabChats.setTextColor(selectedColor)
            binding.tabChats.paint.isFakeBoldText = true
            binding.tabPool.setBackgroundResource(0)
            binding.tabPool.setTextColor(unselectedColor)
            binding.tabPool.paint.isFakeBoldText = false
        }
    }

    private fun switchToTeacherTab(tab: TeacherTab) {
        if (teacherTab == tab) return
        teacherTab = tab
        updateTeacherTabUi()
        listener?.remove()
        listener = null
        if (tab == TeacherTab.POOL) subscribeToPool() else subscribeToChats()
    }

    private fun subscribeToPool() {
        val uid = auth.currentUser?.uid ?: run {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = "Giriş yapın."
            return
        }
        binding.emptyText.text = "Henüz soru yok."
        val query = firestore.collection("questions")
            .whereEqualTo("status", StudentQuestion.STATUS_PENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        listener = query.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            adapter.submitList(list)
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun subscribeToChats() {
        val uid = auth.currentUser?.uid ?: run {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = "Giriş yapın."
            return
        }
        binding.emptyText.text = "Henüz sohbet yok."
        val query = firestore.collection("questions")
            .whereEqualTo("claimedByTeacherUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        listener = query.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            adapter.submitList(list)
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun subscribeToStudentPending() {
        val uid = auth.currentUser?.uid ?: run {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = "Giriş yapın."
            return
        }
        binding.emptyText.text = "Bekleyen soru yok."
        val query = firestore.collection("questions")
            .whereEqualTo("studentUid", uid)
            .whereIn("status", listOf(StudentQuestion.STATUS_PENDING, StudentQuestion.STATUS_CLAIMED))
            .orderBy("createdAt", Query.Direction.DESCENDING)
        listener = query.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            adapter.submitList(list)
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun subscribeToStudentResolved() {
        val uid = auth.currentUser?.uid ?: run {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = "Giriş yapın."
            return
        }
        binding.emptyText.text = "Çözülen soru yok."
        val query = firestore.collection("questions")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", StudentQuestion.STATUS_RESOLVED)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        listener = query.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            adapter.submitList(list)
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openChatAfterPreload(questionId: String) {
        // Loading göster, list tıklamalarını geçici olarak kapat
        binding.chatLoading.visibility = View.VISIBLE
        binding.questionsRecyclerView.isEnabled = false

        firestore.collection("questions")
            .document(questionId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener {
                binding.chatLoading.visibility = View.GONE
                binding.questionsRecyclerView.isEnabled = true

                val fragment = QuestionChatFragment.newInstance(questionId)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerID, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener {
                binding.chatLoading.visibility = View.GONE
                binding.questionsRecyclerView.isEnabled = true
                // Hata durumda şimdilik sadece boş bırakıyoruz; istersen Toast ekleyebilirsin.
            }
    }

    private fun onQuestionClick(question: StudentQuestion) {
        if (isTeacher) {
            if (teacherTab == TeacherTab.POOL) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerID, QuestionDetailFragment.newInstance(question.id))
                    .addToBackStack(null)
                    .commit()
            } else {
                openChatAfterPreload(question.id)
            }
        } else {
            if (question.status == StudentQuestion.STATUS_PENDING) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerID, QuestionDetailFragment.newInstance(question.id, studentView = true))
                    .addToBackStack(null)
                    .commit()
            } else {
                openChatAfterPreload(question.id)
            }
        }
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        _binding = null
        super.onDestroyView()
    }
}
