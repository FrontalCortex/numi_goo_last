package com.example.app

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentNotificationBinding
import com.example.app.model.MessageReport
import com.example.app.model.QuestionMessage
import com.example.app.model.RestrictedUser
import com.example.app.model.StudentQuestion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NotificationFragment : Fragment() {

    private enum class TeacherTab { POOL, CHATS, REPORTS }
    private enum class StudentTab { BEKLEYEN, COZULEN }
    private enum class ReportsSubTab { REPORTS, RESTRICTED }

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val authManager by lazy { AuthManager().also { it.initialize(requireContext()) } }
    private var listener: ListenerRegistration? = null
    private var isTeacher = false
    private var teacherTab = TeacherTab.POOL
    private var studentTab = StudentTab.BEKLEYEN
    private var reportsSubTab = ReportsSubTab.REPORTS

    private val adapter = QuestionListAdapter(
        onItemClick = { question -> onQuestionClick(question) },
        onLongClick = { question -> onQuestionLongClick(question) }
    )
    private val reportAdapter = ReportAdapter(
        onItemClick = { row -> showReportActionDialog(row) }
    )
    private val reportNameCache = mutableMapOf<String, String>()

    private val restrictedUserAdapter = RestrictedUserAdapter(
        onItemClick = { user -> showRestrictionDetailDialog(user) }
    )
    private var bannedListener: ListenerRegistration? = null
    private var timedRestrictionListener: ListenerRegistration? = null
    private var bannedUsersCache: List<RestrictedUser> = emptyList()
    private var timedUsersCache: List<RestrictedUser> = emptyList()

    private val unreadCountByQuestionId = mutableMapOf<String, Int>()
    private var currentQuestionList: List<StudentQuestion> = emptyList()
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    // Öğretmen için: CreateQuestion'dan gelen medya için seçim modu
    private var teacherSelectionMode: Boolean = false

    companion object {
        private const val KEY_TEACHER_TAB = "teacher_tab"
        private const val KEY_STUDENT_TAB = "student_tab"
        private const val KEY_REPORTS_SUB_TAB = "reports_sub_tab"
        private const val ARG_FORCE_TEACHER_CHATS = "force_teacher_chats"
        private const val ARG_FORCE_STUDENT_BEKLEYEN = "force_student_bekleyen"
        private const val ARG_TEACHER_SELECTION_MODE = "teacher_selection_mode"
        private const val ARG_MEDIA_TYPE = "teacher_media_type"
        private const val ARG_MEDIA_PATH = "teacher_media_path"
        private const val ARG_MEDIA_DESCRIPTION = "teacher_media_description"

        /**
         * Sohbet ekranından geri dönerken hangi tab'ın seçili olacağını zorlamak için yardımcı.
         * Öğretmen için CHATS, öğrenci için BEKLEYEN seçili olsun istiyoruz.
         */
        fun newWithReturnDefaults(fromTeacher: Boolean): NotificationFragment {
            return NotificationFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FORCE_TEACHER_CHATS, fromTeacher)
                    putBoolean(ARG_FORCE_STUDENT_BEKLEYEN, !fromTeacher)
                }
            }
        }

        /**
         * Öğretmen CreateQuestion'dan geldiğinde, CHATS sekmesinde seçim modunu açmak için factory.
         */
        fun newWithTeacherSelection(
            mediaType: String,
            mediaPath: String,
            description: String?
        ): NotificationFragment {
            return NotificationFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FORCE_TEACHER_CHATS, true)
                    putBoolean(ARG_TEACHER_SELECTION_MODE, true)
                    putString(ARG_MEDIA_TYPE, mediaType)
                    putString(ARG_MEDIA_PATH, mediaPath)
                    putString(ARG_MEDIA_DESCRIPTION, description)
                }
            }
        }
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
        outState.putString(KEY_REPORTS_SUB_TAB, reportsSubTab.name)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.questionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.questionsRecyclerView.adapter = adapter

        val forceTeacherChats = arguments?.getBoolean(ARG_FORCE_TEACHER_CHATS) == true
        val forceStudentBekleyen = arguments?.getBoolean(ARG_FORCE_STUDENT_BEKLEYEN) == true
        val fromTeacherSelection = arguments?.getBoolean(ARG_TEACHER_SELECTION_MODE) == true

        // CreateQuestion'dan seçim modunda geldiyse her zaman CHATS ve seçim modu (saved state'e bakma)
        if (fromTeacherSelection) {
            teacherSelectionMode = true
            teacherTab = TeacherTab.CHATS
        } else if (savedInstanceState != null) {
            savedInstanceState.getString(KEY_TEACHER_TAB)?.let { name ->
                kotlin.runCatching { teacherTab = TeacherTab.valueOf(name) }
            }
            savedInstanceState.getString(KEY_STUDENT_TAB)?.let { name ->
                kotlin.runCatching { studentTab = StudentTab.valueOf(name) }
            }
            savedInstanceState.getString(KEY_REPORTS_SUB_TAB)?.let { name ->
                kotlin.runCatching { reportsSubTab = ReportsSubTab.valueOf(name) }
            }
        } else {
            if (forceTeacherChats) teacherTab = TeacherTab.CHATS
            if (forceStudentBekleyen) studentTab = StudentTab.BEKLEYEN
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
                        teacherSelectionMode = fromTeacherSelection
                        if (teacherSelectionMode) {
                            binding.tabPool.visibility = View.GONE
                            binding.tabReports.visibility = View.GONE
                            adapter.setTeacherSelectionMode(true, null)
                        } else {
                            binding.tabPool.visibility = View.VISIBLE
                            binding.tabReports.visibility = View.VISIBLE
                        }
                        updateTeacherTabUi()
                        binding.reportsSubTabContainer.visibility = if (teacherTab == TeacherTab.REPORTS) View.VISIBLE else View.GONE
                        updateReportsSubTabUi()
                        binding.questionsRecyclerView.adapter = adapterForTeacherTab(teacherTab)
                        binding.tabPool.setOnClickListener { switchToTeacherTab(TeacherTab.POOL) }
                        binding.tabChats.setOnClickListener { switchToTeacherTab(TeacherTab.CHATS) }
                        binding.tabReports.setOnClickListener { switchToTeacherTab(TeacherTab.REPORTS) }
                        binding.subTabReportsList.setOnClickListener { switchReportsSubTab(ReportsSubTab.REPORTS) }
                        binding.subTabRestrictedUsers.setOnClickListener { switchReportsSubTab(ReportsSubTab.RESTRICTED) }
                        when (teacherTab) {
                            TeacherTab.POOL -> subscribeToPool()
                            TeacherTab.CHATS -> subscribeToChats()
                            TeacherTab.REPORTS -> subscribeReportsSubTab()
                        }
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
        val selectedBg = R.drawable.bg_segment_selected
        val selectedColor = android.graphics.Color.WHITE
        val unselectedColor = requireContext().getColor(R.color.dark_text_secondary)
        if (studentTab == StudentTab.BEKLEYEN) {
            binding.tabBekleyen.setBackgroundResource(selectedBg)
            binding.tabBekleyen.setTextColor(selectedColor)
            binding.tabCozulen.setBackgroundResource(0)
            binding.tabCozulen.setTextColor(unselectedColor)
        } else {
            binding.tabCozulen.setBackgroundResource(selectedBg)
            binding.tabCozulen.setTextColor(selectedColor)
            binding.tabBekleyen.setBackgroundResource(0)
            binding.tabBekleyen.setTextColor(unselectedColor)
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
        val selectedBg = R.drawable.bg_segment_selected
        val selectedColor = android.graphics.Color.WHITE
        val unselectedColor = requireContext().getColor(R.color.dark_text_secondary)
        val tabs = listOf(
            TeacherTab.POOL to binding.tabPool,
            TeacherTab.CHATS to binding.tabChats,
            TeacherTab.REPORTS to binding.tabReports
        )
        tabs.forEach { (tab, view) ->
            if (tab == teacherTab) {
                view.setBackgroundResource(selectedBg)
                view.setTextColor(selectedColor)
            } else {
                view.setBackgroundResource(0)
                view.setTextColor(unselectedColor)
            }
        }
    }

    private fun switchToTeacherTab(tab: TeacherTab) {
        if (teacherTab == tab) return
        teacherTab = tab
        updateTeacherTabUi()
        listener?.remove()
        listener = null
        removeRestrictedUserListeners()
        binding.reportsSubTabContainer.visibility = if (tab == TeacherTab.REPORTS) View.VISIBLE else View.GONE
        binding.questionsRecyclerView.adapter = adapterForTeacherTab(tab)
        when (tab) {
            TeacherTab.POOL -> subscribeToPool()
            TeacherTab.CHATS -> subscribeToChats()
            TeacherTab.REPORTS -> subscribeReportsSubTab()
        }
    }

    private fun adapterForTeacherTab(tab: TeacherTab) = when (tab) {
        TeacherTab.REPORTS -> if (reportsSubTab == ReportsSubTab.REPORTS) reportAdapter else restrictedUserAdapter
        else -> adapter
    }

    private fun updateReportsSubTabUi() {
        val selectedBg = R.drawable.bg_segment_selected
        val selectedColor = android.graphics.Color.WHITE
        val unselectedColor = requireContext().getColor(R.color.dark_text_secondary)
        if (reportsSubTab == ReportsSubTab.REPORTS) {
            binding.subTabReportsList.setBackgroundResource(selectedBg)
            binding.subTabReportsList.setTextColor(selectedColor)
            binding.subTabRestrictedUsers.setBackgroundResource(0)
            binding.subTabRestrictedUsers.setTextColor(unselectedColor)
        } else {
            binding.subTabRestrictedUsers.setBackgroundResource(selectedBg)
            binding.subTabRestrictedUsers.setTextColor(selectedColor)
            binding.subTabReportsList.setBackgroundResource(0)
            binding.subTabReportsList.setTextColor(unselectedColor)
        }
    }

    private fun switchReportsSubTab(tab: ReportsSubTab) {
        if (reportsSubTab == tab) return
        reportsSubTab = tab
        updateReportsSubTabUi()
        listener?.remove()
        listener = null
        removeRestrictedUserListeners()
        binding.questionsRecyclerView.adapter = if (tab == ReportsSubTab.REPORTS) reportAdapter else restrictedUserAdapter
        subscribeReportsSubTab()
    }

    private fun subscribeReportsSubTab() {
        when (reportsSubTab) {
            ReportsSubTab.REPORTS -> subscribeToReports()
            ReportsSubTab.RESTRICTED -> subscribeToRestrictedUsers()
        }
    }

    private fun removeRestrictedUserListeners() {
        bannedListener?.remove()
        bannedListener = null
        timedRestrictionListener?.remove()
        timedRestrictionListener = null
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

    /**
     * MainActivity seçim modundan çıkmak istediğinde çağrılır.
     * TeacherSelectionMode'u kapatır, tıklamaları normal moda döndürür.
     *
     * newWithTeacherSelection() ile oluşturulan bu fragment'ın arguments'ında
     * ARG_TEACHER_SELECTION_MODE=true kalıcı olarak duruyor. Bu fragment (örn. sohbet
     * ekranına geçilip replace() ile view'ı yok edildikten sonra) popBackStack ile geri
     * geldiğinde onViewCreated tekrar çalışıp arguments'ı okuyor; argümanı burada da
     * temizlemezsek seçim modu tamamlanmış olsa bile fragment kendini yeniden seçim
     * moduna sokar.
     */
    fun exitTeacherSelectionMode() {
        teacherSelectionMode = false
        arguments?.putBoolean(ARG_TEACHER_SELECTION_MODE, false)
        adapter.setTeacherSelectionMode(false, null)
        if (_binding != null) {
            binding.tabPool.visibility = View.VISIBLE
            binding.tabReports.visibility = View.VISIBLE
        }
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
            // Soru havuzu: yalnızca oluşturulma zamanına göre sırala.
            // Mevcut Firestore indeksini bozmayalım; DESC alıp client tarafında ASC'ye çeviriyoruz.
            .orderBy("createdAt", Query.Direction.DESCENDING)
        listener = query.addSnapshotListener { snap, e ->
            if (e != null || _binding == null || !isAdded) return@addSnapshotListener
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val visible = raw.filter { it.deletedForUids?.contains(uid) != true }
            // Firestore'dan createdAt DESC gelse de, havuzda yalnızca oluşturulma zamanına göre
            // eski → yeni sıralama istiyoruz. Ekstra mesajlar lastMessageAt'i değiştirse bile
            // bu sıralamayı etkilemeyecek.
            val sorted = visible.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            currentQuestionList = sorted
            adapter.submitList(sorted)
            // En eski sorular üstte, en yeni sorular altta.
            // Öğretmene, en yeni soruları göstermek için listeyi en alta kaydır.
            _binding?.questionsRecyclerView?.post {
                val b = _binding ?: return@post
                if (sorted.isNotEmpty()) {
                    b.questionsRecyclerView.scrollToPosition(sorted.size - 1)
                }
            }
            // Soru havuzundaki (henüz hiçbir öğretmen tarafından sahiplenilmemiş) sorular için
            // öğretmenlere unread badge göstermiyoruz.
            unreadCountByQuestionId.clear()
            adapter.setUnreadCounts(emptyMap())
            removeAllMessageListeners()
            binding.emptyText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
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
            if (e != null || _binding == null || !isAdded) return@addSnapshotListener
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val visible = raw.filter { it.deletedForUids?.contains(uid) != true }
            val sorted = visible.sortedByDescending {
                val last = it.lastMessageAt?.toDate()?.time
                val created = it.createdAt?.toDate()?.time
                last ?: created ?: 0L
            }
            currentQuestionList = sorted
            adapter.submitList(sorted)
            refreshUnreadCounts(sorted, uid)
            // Yeni mesajlarda, en güncel soru en üstte olduğu için listeyi otomatik olarak üste kaydır.
            _binding?.questionsRecyclerView?.post {
                val b = _binding ?: return@post
                if (sorted.isNotEmpty()) {
                    b.questionsRecyclerView.scrollToPosition(0)
                }
            }
            binding.emptyText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun subscribeToReports() {
        binding.emptyText.text = "Bekleyen rapor yok."
        val query = firestore.collection("messageReports")
            .whereEqualTo("status", MessageReport.STATUS_PENDING)
            .orderBy("reportedAt", Query.Direction.DESCENDING)
        listener = query.addSnapshotListener { snap, e ->
            if (e != null || _binding == null || !isAdded) return@addSnapshotListener
            val reports = snap?.documents?.mapNotNull { doc ->
                doc.toObject(MessageReport::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            resolveReportRows(reports)
        }
    }

    private fun resolveReportRows(reports: List<MessageReport>) {
        if (reports.isEmpty()) {
            reportAdapter.submitList(emptyList())
            binding.emptyText.visibility = View.VISIBLE
            return
        }
        val uidsToResolve = reports.map { it.reportedUserUid }.toSet() - reportNameCache.keys
        if (uidsToResolve.isEmpty()) {
            submitResolvedReportRows(reports)
            return
        }
        val pending = java.util.concurrent.atomic.AtomicInteger(uidsToResolve.size)
        uidsToResolve.forEach { uid ->
            firestore.collection("users").document(uid).get()
                .addOnCompleteListener {
                    val name = it.result?.getString("name")?.takeIf { n -> n.isNotBlank() }
                        ?: uid.take(6)
                    reportNameCache[uid] = name
                    if (pending.decrementAndGet() == 0) submitResolvedReportRows(reports)
                }
        }
    }

    private fun submitResolvedReportRows(reports: List<MessageReport>) {
        if (_binding == null || !isAdded) return
        val rows = reports.map { r ->
            ReportRow(r, reportNameCache[r.reportedUserUid] ?: r.reportedUserUid.take(6))
        }
        reportAdapter.submitList(rows)
        binding.emptyText.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showReportActionDialog(row: ReportRow) {
        val myUid = auth.currentUser?.uid
        val view = layoutInflater.inflate(R.layout.dialog_report_action, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val isSelfReport = row.report.reportedUserUid == myUid

        view.findViewById<TextView>(R.id.reportDialogUserName).text = row.reportedUserName
        view.findViewById<TextView>(R.id.reportDialogReason).text = MessageReport.reasonLabel(row.report.reason)
        view.findViewById<TextView>(R.id.reportDialogMessagePreview).text = row.report.messagePreview.ifEmpty { "-" }
        view.findViewById<View>(R.id.reportDialogClose).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { releaseReportAudioPlayer() }
        bindReportMedia(view, row.report)

        val restrict1 = view.findViewById<View>(R.id.reportRestrict1Day)
        val restrict3 = view.findViewById<View>(R.id.reportRestrict3Days)
        val restrict7 = view.findViewById<View>(R.id.reportRestrict7Days)
        val ban = view.findViewById<View>(R.id.reportBanPermanent)
        val dismissRow = view.findViewById<View>(R.id.reportDismiss)
        val cancel = view.findViewById<View>(R.id.reportCancel)

        if (isSelfReport) {
            listOf(restrict1, restrict3, restrict7, ban).forEach {
                it.isEnabled = false
                it.alpha = 0.4f
            }
        }

        restrict1.setOnClickListener { dialog.dismiss(); restrictReportedUser(row.report, 1) }
        restrict3.setOnClickListener { dialog.dismiss(); restrictReportedUser(row.report, 3) }
        restrict7.setOnClickListener { dialog.dismiss(); restrictReportedUser(row.report, 7) }
        ban.setOnClickListener { dialog.dismiss(); restrictReportedUser(row.report, null) }
        dismissRow.setOnClickListener { dialog.dismiss(); dismissReport(row.report) }
        cancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun bindReportMedia(dialogView: View, report: MessageReport) {
        bindMediaPreview(dialogView, report.type, report.mediaUrl, report.thumbnailUrl)
    }

    private fun bindMediaPreview(dialogView: View, type: String?, mediaUrl: String?, thumbnailUrl: String?) {
        val mediaContainer = dialogView.findViewById<View>(R.id.reportDialogMediaContainer)
        val mediaThumb = dialogView.findViewById<ImageView>(R.id.reportDialogMediaThumb)
        val playOverlay = dialogView.findViewById<View>(R.id.reportDialogPlayOverlay)
        val audioRow = dialogView.findViewById<View>(R.id.reportDialogAudioRow)
        val audioPlayPause = dialogView.findViewById<ImageView>(R.id.reportDialogAudioPlayPause)
        val audioTime = dialogView.findViewById<TextView>(R.id.reportDialogAudioTime)

        when {
            type == QuestionMessage.TYPE_IMAGE && !mediaUrl.isNullOrBlank() -> {
                mediaContainer.visibility = View.VISIBLE
                playOverlay.visibility = View.GONE
                Glide.with(this).load(mediaUrl).transform(CenterCrop(), RoundedCorners(24)).into(mediaThumb)
                mediaThumb.setOnClickListener { showReportImageDialog(mediaUrl) }
            }
            type == QuestionMessage.TYPE_VIDEO && !mediaUrl.isNullOrBlank() -> {
                mediaContainer.visibility = View.VISIBLE
                playOverlay.visibility = View.VISIBLE
                Glide.with(this).load(thumbnailUrl ?: mediaUrl).transform(CenterCrop(), RoundedCorners(24)).into(mediaThumb)
                val openVideo = View.OnClickListener {
                    VideoFullscreenDialogFragment.newInstance(mediaUrl).show(childFragmentManager, "reportVideo")
                }
                mediaThumb.setOnClickListener(openVideo)
                playOverlay.setOnClickListener(openVideo)
            }
            type == QuestionMessage.TYPE_AUDIO && !mediaUrl.isNullOrBlank() -> {
                audioRow.visibility = View.VISIBLE
                audioPlayPause.setOnClickListener { toggleReportAudio(mediaUrl, audioPlayPause, audioTime) }
            }
        }
    }

    private fun showReportImageDialog(url: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(R.layout.dialog_image)
            window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        val imageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
        Glide.with(this).load(url).into(imageView)
        dialog.findViewById<View>(R.id.dialogImageClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private var reportAudioPlayer: MediaPlayer? = null
    private var reportAudioUrl: String? = null

    private fun toggleReportAudio(url: String, playPauseView: ImageView, timeView: TextView) {
        val current = reportAudioPlayer
        if (current != null && reportAudioUrl == url) {
            if (current.isPlaying) {
                current.pause()
                playPauseView.setImageResource(android.R.drawable.ic_media_play)
            } else {
                current.start()
                playPauseView.setImageResource(android.R.drawable.ic_media_pause)
                pollReportAudioProgress(timeView)
            }
            return
        }
        releaseReportAudioPlayer()
        reportAudioUrl = url
        reportAudioPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                start()
                playPauseView.setImageResource(android.R.drawable.ic_media_pause)
                pollReportAudioProgress(timeView)
            }
            setOnCompletionListener {
                playPauseView.setImageResource(android.R.drawable.ic_media_play)
                timeView.text = "Ses kaydını dinlemek için oynat"
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(requireContext(), "Ses oynatılamadı.", Toast.LENGTH_SHORT).show()
                true
            }
            prepareAsync()
        }
    }

    private fun pollReportAudioProgress(timeView: TextView) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                val player = reportAudioPlayer ?: return
                if (!player.isPlaying) return
                val curSec = player.currentPosition / 1000
                val totalSec = player.duration / 1000
                timeView.text = "%d:%02d / %d:%02d".format(curSec / 60, curSec % 60, totalSec / 60, totalSec % 60)
                handler.postDelayed(this, 250)
            }
        })
    }

    private fun releaseReportAudioPlayer() {
        reportAudioPlayer?.release()
        reportAudioPlayer = null
        reportAudioUrl = null
    }

    private fun dismissReport(report: MessageReport) {
        val myUid = auth.currentUser?.uid ?: return
        firestore.collection("messageReports").document(report.id)
            .update(
                mapOf(
                    "status" to MessageReport.STATUS_DISMISSED,
                    "handledByUid" to myUid,
                    "handledAt" to Timestamp.now()
                )
            )
            .addOnSuccessListener { Toast.makeText(requireContext(), "Rapor reddedildi.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(requireContext(), "İşlem başarısız.", Toast.LENGTH_SHORT).show() }
    }

    private fun restrictReportedUser(report: MessageReport, days: Int?) {
        val myUid = auth.currentUser?.uid ?: return
        val reportRef = firestore.collection("messageReports").document(report.id)
        val userRef = firestore.collection("users").document(report.reportedUserUid)
        val batch = firestore.batch()
        batch.update(
            reportRef,
            mapOf(
                "status" to MessageReport.STATUS_ACTION_TAKEN,
                "handledByUid" to myUid,
                "handledAt" to Timestamp.now()
            )
        )
        // Kısıtlamanın hangi rapordan geldiğini kullanıcı dokümanına da kopyalıyoruz ki
        // "Kısıtlı Kullanıcılar" listesinde ayrı bir sorguya gerek kalmadan içerik gösterilebilsin.
        val restrictionContext = mapOf(
            "restrictionReason" to report.reason,
            "restrictionMessagePreview" to report.messagePreview,
            "restrictionMessageType" to report.type,
            "restrictionMediaUrl" to report.mediaUrl,
            "restrictionThumbnailUrl" to report.thumbnailUrl,
            "restrictedAt" to Timestamp.now(),
            "restrictedByUid" to myUid
        )
        if (days == null) {
            batch.update(userRef, restrictionContext + mapOf("banned" to true, "restrictedUntil" to FieldValue.delete()))
        } else {
            val until = Timestamp(Timestamp.now().seconds + days * 24L * 60L * 60L, 0)
            batch.update(userRef, restrictionContext + mapOf("restrictedUntil" to until, "banned" to false))
        }
        batch.commit()
            .addOnSuccessListener { Toast.makeText(requireContext(), "Kullanıcı kısıtlandı.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "İşlem başarısız: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun subscribeToRestrictedUsers() {
        binding.emptyText.text = "Kısıtlı kullanıcı yok."
        bannedListener = firestore.collection("users")
            .whereEqualTo("banned", true)
            .addSnapshotListener { snap, e ->
                if (e != null || _binding == null || !isAdded) return@addSnapshotListener
                bannedUsersCache = snap?.documents?.map { doc -> toRestrictedUser(doc) } ?: emptyList()
                submitRestrictedUserRows()
            }
        timedRestrictionListener = firestore.collection("users")
            .whereGreaterThan("restrictedUntil", Timestamp.now())
            .addSnapshotListener { snap, e ->
                if (e != null || _binding == null || !isAdded) return@addSnapshotListener
                timedUsersCache = snap?.documents?.map { doc -> toRestrictedUser(doc) } ?: emptyList()
                submitRestrictedUserRows()
            }
    }

    private fun toRestrictedUser(doc: DocumentSnapshot): RestrictedUser {
        val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: doc.id.take(6)
        return RestrictedUser(
            uid = doc.id,
            name = name,
            banned = doc.getBoolean("banned") == true,
            restrictedUntil = doc.getTimestamp("restrictedUntil"),
            restrictionReason = doc.getString("restrictionReason"),
            restrictionMessagePreview = doc.getString("restrictionMessagePreview"),
            restrictionMessageType = doc.getString("restrictionMessageType"),
            restrictionMediaUrl = doc.getString("restrictionMediaUrl"),
            restrictionThumbnailUrl = doc.getString("restrictionThumbnailUrl"),
            restrictedAt = doc.getTimestamp("restrictedAt"),
            restrictedByUid = doc.getString("restrictedByUid")
        )
    }

    private fun submitRestrictedUserRows() {
        if (_binding == null || !isAdded) return
        val now = System.currentTimeMillis()
        val merged = (timedUsersCache + bannedUsersCache)
            .filter { it.banned || (it.restrictedUntil?.toDate()?.time ?: 0L) > now }
            .associateBy { it.uid }
            .values
            .sortedWith(
                compareByDescending<RestrictedUser> { it.banned }
                    .thenBy { it.restrictedUntil?.toDate()?.time ?: Long.MAX_VALUE }
            )
        restrictedUserAdapter.submitList(merged)
        binding.emptyText.visibility = if (merged.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showUnbanConfirmDialog(user: RestrictedUser) {
        AlertDialog.Builder(requireContext())
            .setTitle("Kısıtlamayı kaldır")
            .setMessage("${user.name} kullanıcısının kısıtlaması kaldırılsın mı?")
            .setPositiveButton("Kaldır") { _, _ -> removeRestriction(user.uid) }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun removeRestriction(uid: String) {
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "banned" to false,
                    "restrictedUntil" to FieldValue.delete(),
                    "restrictionReason" to FieldValue.delete(),
                    "restrictionMessagePreview" to FieldValue.delete(),
                    "restrictionMessageType" to FieldValue.delete(),
                    "restrictionMediaUrl" to FieldValue.delete(),
                    "restrictionThumbnailUrl" to FieldValue.delete(),
                    "restrictedAt" to FieldValue.delete(),
                    "restrictedByUid" to FieldValue.delete()
                )
            )
            .addOnSuccessListener { Toast.makeText(requireContext(), "Kısıtlama kaldırıldı.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "İşlem başarısız: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun showRestrictionDetailDialog(user: RestrictedUser) {
        val view = layoutInflater.inflate(R.layout.dialog_restriction_detail, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnDismissListener { releaseReportAudioPlayer() }

        view.findViewById<TextView>(R.id.restrictionDialogUserName).text = user.name
        val statusView = view.findViewById<TextView>(R.id.restrictionDialogStatus)
        if (user.banned) {
            statusView.text = "Kalıcı ban"
            statusView.setBackgroundResource(R.drawable.bg_status_chip_error)
            statusView.setTextColor(requireContext().getColor(R.color.dark_error))
        } else {
            statusView.text = formatRestrictionUntil(user.restrictedUntil)
            statusView.setBackgroundResource(R.drawable.bg_status_chip_warning)
            statusView.setTextColor(requireContext().getColor(R.color.dark_warning))
        }

        val reason = user.restrictionReason
        val reasonView = view.findViewById<TextView>(R.id.restrictionDialogReason)
        if (reason.isNullOrBlank()) {
            reasonView.visibility = View.GONE
        } else {
            reasonView.visibility = View.VISIBLE
            reasonView.text = MessageReport.reasonLabel(reason)
        }

        view.findViewById<TextView>(R.id.restrictionDialogMessagePreview).text =
            user.restrictionMessagePreview?.ifBlank { null } ?: "Bu kısıtlamanın kaynağı hakkında bilgi yok."
        bindMediaPreview(view, user.restrictionMessageType, user.restrictionMediaUrl, user.restrictionThumbnailUrl)

        val metaView = view.findViewById<TextView>(R.id.restrictionDialogMeta)
        val restrictedAt = user.restrictedAt
        if (restrictedAt == null) {
            metaView.visibility = View.GONE
        } else {
            metaView.visibility = View.VISIBLE
            val dateText = android.text.format.DateFormat.format("d MMM yyyy, HH:mm", restrictedAt.toDate())
            metaView.text = "$dateText tarihinde kısıtlandı"
            val byUid = user.restrictedByUid
            if (!byUid.isNullOrBlank()) {
                val cached = reportNameCache[byUid]
                if (cached != null) {
                    metaView.text = "$dateText tarihinde $cached tarafından kısıtlandı"
                } else {
                    firestore.collection("users").document(byUid).get()
                        .addOnSuccessListener { doc ->
                            val teacherName = doc.getString("name")?.takeIf { it.isNotBlank() } ?: byUid.take(6)
                            reportNameCache[byUid] = teacherName
                            if (_binding != null && isAdded) {
                                metaView.text = "$dateText tarihinde $teacherName tarafından kısıtlandı"
                            }
                        }
                }
            }
        }

        view.findViewById<View>(R.id.restrictionDialogRemove).setOnClickListener {
            dialog.dismiss()
            showUnbanConfirmDialog(user)
        }
        view.findViewById<View>(R.id.restrictionDialogClose).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.restrictionDialogCloseText).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun formatRestrictionUntil(until: Timestamp?): String {
        if (until == null) return "Kısıtlı"
        val diff = until.toDate().time - System.currentTimeMillis()
        if (diff <= 0) return "Süresi doldu"
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff) % 24
        return when {
            days >= 1 -> "$days gün kaldı"
            hours >= 1 -> "$hours saat kaldı"
            else -> "1 saatten az kaldı"
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
            if (e != null || _binding == null || !isAdded) return@addSnapshotListener
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val visible = raw.filter { it.deletedForUids?.contains(uid) != true }
            val previousSize = currentQuestionList.size
            val sorted = visible.sortedByDescending {
                val last = it.lastMessageAt?.toDate()?.time
                val created = it.createdAt?.toDate()?.time
                last ?: created ?: 0L
            }
            currentQuestionList = sorted
            adapter.submitList(sorted)
            refreshUnreadCounts(sorted, uid)
            // Öğrenci bekleyen listesinde: yalnızca yeni soru eklendiğinde veya kapsam genişlediğinde
            // (boyut azalmamışsa) listeyi en üste kaydır. Böylece soru silindiğinde scroll yapılmaz.
            _binding?.questionsRecyclerView?.post {
                val b = _binding ?: return@post
                if (sorted.isNotEmpty() && sorted.size >= previousSize) {
                    b.questionsRecyclerView.scrollToPosition(0)
                }
            }
            binding.emptyText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
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
            if (e != null || _binding == null || !isAdded) return@addSnapshotListener
            val raw = snap?.documents?.mapNotNull { doc ->
                doc.toObject(StudentQuestion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            val visible = raw.filter { it.deletedForUids?.contains(uid) != true }
            val sorted = visible.sortedByDescending {
                val last = it.lastMessageAt?.toDate()?.time
                val created = it.createdAt?.toDate()?.time
                last ?: created ?: 0L
            }
            currentQuestionList = sorted
            adapter.submitList(sorted)
            refreshUnreadCounts(sorted, uid)
            binding.emptyText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = auth.currentUser?.uid ?: return
        if (currentQuestionList.isNotEmpty()) refreshUnreadCounts(currentQuestionList, uid)
    }

    private fun openChatAfterPreload(questionId: String) {
        val b = _binding ?: return
        // Loading göster, list tıklamalarını geçici olarak kapat
        b.chatLoading.visibility = View.VISIBLE
        b.questionsRecyclerView.isEnabled = false

        firestore.collection("questions")
            .document(questionId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener {
                val b = _binding ?: return@addOnSuccessListener
                b.chatLoading.visibility = View.GONE
                b.questionsRecyclerView.isEnabled = true

                val fragment = QuestionChatFragment.newInstance(questionId)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerID, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener {
                val b = _binding ?: return@addOnFailureListener
                b.chatLoading.visibility = View.GONE
                b.questionsRecyclerView.isEnabled = true
                // Hata durumda şimdilik sadece boş bırakıyoruz; istersen Toast ekleyebilirsin.
            }
    }

    private fun onQuestionLongClick(question: StudentQuestion) {
        val uid = auth.currentUser?.uid ?: return
        if (isTeacher) {
            if (question.status != StudentQuestion.STATUS_RESOLVED) {
                Toast.makeText(requireContext(), "Çözüldü olarak işaretlenen soruları silebilirsin.", Toast.LENGTH_LONG).show()
                return
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Soruyu silmek istediğine emin misin")
                .setMessage("Bu işlem geri alınamaz.")
                .setPositiveButton("Kaldır") { dialog, _ ->
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
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Soruyu silmek istediğine emin misin?")
                .setMessage("Bu işlem geri alınamaz. Soruyu sildiğinde ona bir daha erişemezsin.")
                .setPositiveButton("Sil") { dialog, _ ->
                    dialog.dismiss()
                    firestore.collection("questions").document(question.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Soru silindi.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Silinemedi.", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun onQuestionClick(question: StudentQuestion) {
        requireOnlineAndLoggedInOrLogin {
            if (isTeacher) {
                if (teacherSelectionMode && teacherTab == TeacherTab.CHATS) {
                    if (question.status == StudentQuestion.STATUS_RESOLVED) {
                        Toast.makeText(requireContext(), "Çözülen sorulara mesaj gönderemezsin.", Toast.LENGTH_SHORT).show()
                        return@requireOnlineAndLoggedInOrLogin
                    }
                    val title = if (question.previewText.isNotEmpty()) question.previewText else question.message
                    adapter.setTeacherSelectionMode(true, question.id)
                    (activity as? MainActivity)?.onTeacherChatSelectedFromNotification(question.id, title)
                } else {
                    openChatAfterPreload(question.id)
                }
            } else {
                openChatAfterPreload(question.id)
            }
        }
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        removeRestrictedUserListeners()
        removeAllMessageListeners()
        releaseReportAudioPlayer()
        _binding = null
        super.onDestroyView()
    }
}
