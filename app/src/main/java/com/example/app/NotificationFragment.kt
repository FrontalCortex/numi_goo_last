package com.example.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentNotificationBinding
import com.example.app.model.StudentQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

    private val adapter = QuestionListAdapter(
        onItemClick = { question -> onQuestionClick(question) },
        onLongClick = { question -> onQuestionLongClick(question) }
    )

    private val unreadCountByQuestionId = mutableMapOf<String, Int>()
    private var currentQuestionList: List<StudentQuestion> = emptyList()
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

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
            val uid = auth.currentUser?.uid
            if (uid == null) {
                binding.teacherApprovalPendingContainer.visibility = View.VISIBLE
                binding.headerContainer.visibility = View.GONE
                binding.questionsRecyclerView.visibility = View.GONE
                setupTeacherApprovalPendingUi()
                return@onViewCreated
            }
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    if (!isAdded || _binding == null) return@addOnSuccessListener
                    val teacherApproved = userDoc.getBoolean("teacherApproved") == true
                    if (!teacherApproved) {
                        binding.teacherApprovalPendingContainer.visibility = View.VISIBLE
                        binding.headerContainer.visibility = View.GONE
                        binding.questionsRecyclerView.visibility = View.GONE
                        setupTeacherApprovalPendingUi()
                    } else {
                        binding.teacherApprovalPendingContainer.visibility = View.GONE
                        binding.notificationTitle.visibility = View.GONE
                        binding.teacherTabContainer.visibility = View.VISIBLE
                        updateTeacherTabUi()
                        binding.tabPool.setOnClickListener { switchToTeacherTab(TeacherTab.POOL) }
                        binding.tabChats.setOnClickListener { switchToTeacherTab(TeacherTab.CHATS) }
                        if (teacherTab == TeacherTab.POOL) subscribeToPool() else subscribeToChats()
                    }
                }
                .addOnFailureListener {
                    if (!isAdded || _binding == null) return@addOnFailureListener
                    binding.teacherApprovalPendingContainer.visibility = View.VISIBLE
                    binding.headerContainer.visibility = View.GONE
                    binding.questionsRecyclerView.visibility = View.GONE
                    setupTeacherApprovalPendingUi()
                }
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

    private fun setupTeacherApprovalPendingUi() {
        binding.teacherApprovalSupportButton.setOnClickListener {
            val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:numigo.support@gmail.com")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val gmailWebUri = Uri.parse(
                "https://mail.google.com/mail/?view=cm&to=numigo.support@gmail.com"
            )
            val browserIntent = Intent(Intent.ACTION_VIEW, gmailWebUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivity(mailtoIntent)
            } catch (_: ActivityNotFoundException) {
                try {
                    startActivity(Intent.createChooser(browserIntent, null))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "E-posta veya tarayıcı açılamadı.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: SecurityException) {
                try {
                    startActivity(Intent.createChooser(browserIntent, null))
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "E-posta veya tarayıcı açılamadı.", Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun refreshUnreadCounts(list: List<StudentQuestion>, uid: String) {
        if (list.isEmpty()) {
            unreadCountByQuestionId.clear()
            adapter.setUnreadCounts(emptyMap())
            removeAllMessageListeners()
            return
        }
        val newMap = mutableMapOf<String, Int>()
        val pending = java.util.concurrent.atomic.AtomicInteger(list.size)
        list.forEach { q ->
            firestore.collection("questions").document(q.id).collection("messages")
                .limit(100)
                .get()
                .addOnSuccessListener { snap ->
                    val count = snap.documents.count { doc ->
                        doc.getString("senderUid") != uid && doc.getTimestamp("readAt") == null
                    }
                    synchronized(newMap) {
                        newMap[q.id] = count
                        if (pending.decrementAndGet() == 0) {
                            unreadCountByQuestionId.clear()
                            unreadCountByQuestionId.putAll(newMap)
                            adapter.setUnreadCounts(unreadCountByQuestionId.toMap())
                            attachMessageListeners(list, uid)
                        }
                    }
                }
                .addOnFailureListener {
                    synchronized(newMap) {
                        newMap[q.id] = 0
                        if (pending.decrementAndGet() == 0) {
                            unreadCountByQuestionId.clear()
                            unreadCountByQuestionId.putAll(newMap)
                            adapter.setUnreadCounts(unreadCountByQuestionId.toMap())
                            attachMessageListeners(list, uid)
                        }
                    }
                }
        }
    }

    private fun attachMessageListeners(list: List<StudentQuestion>, uid: String) {
        val questionIds = list.map { it.id }.toSet()
        messageListeners.keys.toList().forEach { questionId ->
            if (questionId !in questionIds) {
                messageListeners.remove(questionId)?.remove()
            }
        }
        list.forEach { q ->
            if (q.id in messageListeners) return@forEach
            val reg = firestore.collection("questions").document(q.id).collection("messages")
                .addSnapshotListener { snap, _ ->
                    if (_binding == null || !isAdded) return@addSnapshotListener
                    val count = snap?.documents?.count { doc ->
                        doc.getString("senderUid") != uid && doc.getTimestamp("readAt") == null
                    } ?: 0
                    unreadCountByQuestionId[q.id] = count
                    adapter.setUnreadCounts(unreadCountByQuestionId.toMap())
                }
            messageListeners[q.id] = reg
        }
    }

    private fun removeAllMessageListeners() {
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
    }

    private fun subscribeToPool() {
        val uid = auth.currentUser?.uid ?: run {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = "Giriş yapın."
            return
        }
        binding.emptyText.text = "Henüz soru yok"
        val query = firestore.collection("questions")
            .whereEqualTo("status", StudentQuestion.STATUS_PENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        listener = query.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val list = raw.filter { it.deletedForUids?.contains(uid) != true }
            currentQuestionList = list
            adapter.submitList(list)
            refreshUnreadCounts(list, uid)
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
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val list = raw.filter { it.deletedForUids?.contains(uid) != true }
            currentQuestionList = list
            adapter.submitList(list)
            refreshUnreadCounts(list, uid)
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
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val list = raw.filter { it.deletedForUids?.contains(uid) != true }
            currentQuestionList = list
            adapter.submitList(list)
            refreshUnreadCounts(list, uid)
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
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val list = raw.filter { it.deletedForUids?.contains(uid) != true }
            currentQuestionList = list
            adapter.submitList(list)
            refreshUnreadCounts(list, uid)
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = auth.currentUser?.uid ?: return
        if (currentQuestionList.isNotEmpty()) refreshUnreadCounts(currentQuestionList, uid)
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

    private fun onQuestionLongClick(question: StudentQuestion) {
        if (question.status != StudentQuestion.STATUS_RESOLVED) return
        val uid = auth.currentUser?.uid ?: return
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage("Bu soruyu listeden silmek istiyor musunuz?")
            .setPositiveButton("Sil") { dialog, _ ->
                dialog.dismiss()
                firestore.collection("questions").document(question.id)
                    .update("deletedForUids", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Listeden kaldırıldı.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Kaldırılamadı.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
            .show()
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
        removeAllMessageListeners()
        _binding = null
        super.onDestroyView()
    }
}
