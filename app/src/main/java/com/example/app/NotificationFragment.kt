package com.example.app

import android.app.Dialog
import android.media.MediaPlayer
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
    /** Havuz: kurs öğretmeninin sınıflarının soruları ya da genel havuz (bkz. CourseClasses). */
    private enum class PoolSubTab { MINE, GENERAL }
    /**
     * Sınıftaki öğrencinin ana sekmeleri: Sorularım (Bekleyen/Çözülen alt sekmeleriyle) |
     * Ödevlerim | Sınıfım. Sınıfı olmayan öğrencide yalnızca sorular (ekran eskisi gibi).
     */
    private enum class StudentMainTab { QUESTIONS, CLASS }

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
    private var poolSubTab: PoolSubTab? = null
    /**
     * Öğretmenin gördüğü havuzlar; öğretmen dokümanı okununca dolar. Havuz sorgusu buna göre
     * kuruluyor ve kurallarla birebir aynı olmalı: kurallar sorguyu süzmüyor, izinsiz bir
     * dokümana çarpabilecek sorguyu (ör. kurs öğretmeninin genel havuz sorgusu) tamamen
     * reddediyor ve liste hiçbir hata göstermeden boş kalıyor.
     */
    private var poolVisibility: CourseClasses.PoolVisibility? = null

    // Öğrencinin sınıfı ve Sınıfım sekmesi (ödevler oradaki karttan açılıyor).
    private var studentMainTab = StudentMainTab.QUESTIONS
    private var myClass: CourseClasses.MyClass? = null
    private var homeworkWatcher: com.example.app.homework.HomeworkCard.Watcher? = null
    private var classmatesListener: ListenerRegistration? = null
    private val classmateAvatars = HashMap<String, String?>()
    /** Ödev bildiriminden gelindi: sınıf okununca ödev ekranı bir kez açılacak. */
    private var openHomeworksWhenReady = false

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
        private const val KEY_POOL_SUB_TAB = "pool_sub_tab"
        private const val KEY_STUDENT_MAIN_TAB = "student_main_tab"
        private const val ARG_FORCE_STUDENT_HOMEWORK = "force_student_homework"

        /** Yeni ödev bildirimine dokunan öğrenci için: Sınıfım sekmesi ve üstünde ödev ekranı açılsın. */
        fun newForStudentHomework(): NotificationFragment = NotificationFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_FORCE_STUDENT_HOMEWORK, true) }
        }
        private const val ARG_FORCE_TEACHER_CHATS = "force_teacher_chats"
        private const val ARG_FORCE_TEACHER_POOL = "force_teacher_pool"
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
         * Havuz bildirimine dokunan öğretmen için: HAVUZ sekmesi zorla seçili açılsın.
         *
         * `teacherTab` varsayılanı zaten POOL ama bu yetmiyor — öğretmen son olarak Sohbetler
         * sekmesinde kaldıysa kaydedilmiş durum onu geri getiriyor. Bildirim "havuzda yeni
         * soru var" diyorsa açılan ekran havuz olmak zorunda.
         */
        fun newForTeacherPool(): NotificationFragment {
            return NotificationFragment().apply {
                arguments = Bundle().apply { putBoolean(ARG_FORCE_TEACHER_POOL, true) }
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
        poolSubTab?.let { outState.putString(KEY_POOL_SUB_TAB, it.name) }
        outState.putString(KEY_STUDENT_MAIN_TAB, studentMainTab.name)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.questionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.questionsRecyclerView.adapter = adapter

        val forceTeacherChats = arguments?.getBoolean(ARG_FORCE_TEACHER_CHATS) == true
        val forceTeacherPool = arguments?.getBoolean(ARG_FORCE_TEACHER_POOL) == true
        val forceStudentBekleyen = arguments?.getBoolean(ARG_FORCE_STUDENT_BEKLEYEN) == true
        val fromTeacherSelection = arguments?.getBoolean(ARG_TEACHER_SELECTION_MODE) == true

        // CreateQuestion'dan seçim modunda geldiyse her zaman CHATS ve seçim modu (saved state'e bakma)
        if (fromTeacherSelection) {
            teacherSelectionMode = true
            teacherTab = TeacherTab.CHATS
        } else if (forceTeacherPool) {
            // Havuz bildiriminden gelindi: kaydedilmiş sekme yok sayılıyor, bildirimin
            // söylediği ekran açılmalı.
            teacherTab = TeacherTab.POOL
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
            savedInstanceState.getString(KEY_POOL_SUB_TAB)?.let { name ->
                kotlin.runCatching { poolSubTab = PoolSubTab.valueOf(name) }
            }
            savedInstanceState.getString(KEY_STUDENT_MAIN_TAB)?.let { name ->
                kotlin.runCatching { studentMainTab = StudentMainTab.valueOf(name) }
            }
        } else {
            if (forceTeacherChats) teacherTab = TeacherTab.CHATS
            if (forceStudentBekleyen) studentTab = StudentTab.BEKLEYEN
        }

        // Ödev bildiriminden gelindi. Argüman tüketiliyor: profilden geri dönüşte görünüm yeniden
        // kurulduğunda öğrencinin seçtiği sekme ezilmesin.
        if (arguments?.getBoolean(ARG_FORCE_STUDENT_HOMEWORK) == true) {
            studentMainTab = StudentMainTab.CLASS
            openHomeworksWhenReady = true
            arguments?.putBoolean(ARG_FORCE_STUDENT_HOMEWORK, false)
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
                        setupPoolVisibility(CourseClasses.poolVisibilityOf(userDoc))
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
            binding.notificationTitle.text = appString(R.string.ui_sorularim)
            binding.teacherTabContainer.visibility = View.GONE
            binding.studentTabContainer.visibility = View.VISIBLE
            updateStudentTabUi()
            binding.tabBekleyen.setOnClickListener { switchToStudentTab(StudentTab.BEKLEYEN) }
            binding.tabCozulen.setOnClickListener { switchToStudentTab(StudentTab.COZULEN) }
            binding.tabStudentQuestions.setOnClickListener { showStudentMainTab(StudentMainTab.QUESTIONS) }
            binding.tabStudentClass.setOnClickListener { showStudentMainTab(StudentMainTab.CLASS) }
            // Sorular sınıf beklenmeden açılıyor (sınıfı olmayan öğrencilerin çoğu için tek ekran);
            // Sınıfım isteniyorsa sınıf okununca geçiliyor.
            if (studentMainTab == StudentMainTab.QUESTIONS) {
                if (studentTab == StudentTab.BEKLEYEN) subscribeToStudentPending() else subscribeToStudentResolved()
            }
            loadStudentClass()
        }
    }

    // ── Öğrenci: Sorularım | Sınıfım ──────────────────────────────────────

    /**
     * Öğrencinin sınıfını okur. Sınıfı varsa ana sekmeler açılıyor (başlık yerine, öğretmendeki
     * gibi); yoksa ekran yalnızca sorular. Sınıfa katılma Ayarlar → Kurs'ta kalıyor.
     */
    private fun loadStudentClass() {
        CourseClasses.loadMyClass { c, _ ->
            val b = _binding ?: return@loadMyClass
            myClass = c
            if (c == null) {
                b.studentMainTabContainer.visibility = View.GONE
                b.studentClassInfo.visibility = View.GONE
                b.notificationTitle.visibility = View.VISIBLE
                openHomeworksWhenReady = false
                if (studentMainTab != StudentMainTab.QUESTIONS) showStudentMainTab(StudentMainTab.QUESTIONS, force = true)
                return@loadMyClass
            }
            b.notificationTitle.visibility = View.GONE
            b.studentMainTabContainer.visibility = View.VISIBLE
            if (studentMainTab == StudentMainTab.QUESTIONS) updateStudentMainTabUi()
            else showStudentMainTab(studentMainTab, force = true)
            if (openHomeworksWhenReady) {
                openHomeworksWhenReady = false
                openStudentScreen(com.example.app.homework.StudentHomeworkListFragment.newInstance(c.id))
            }
        }
    }

    private fun updateStudentMainTabUi() {
        val selectedColor = android.graphics.Color.WHITE
        val unselectedColor = requireContext().getColor(R.color.dark_text_secondary)
        listOf(
            StudentMainTab.QUESTIONS to binding.tabStudentQuestions,
            StudentMainTab.CLASS to binding.tabStudentClass,
        ).forEach { (tab, view) ->
            view.setBackgroundResource(if (tab == studentMainTab) R.drawable.bg_segment_selected else 0)
            view.setTextColor(if (tab == studentMainTab) selectedColor else unselectedColor)
        }
    }

    private fun stopStudentClassListeners() {
        homeworkWatcher?.stop()
        homeworkWatcher = null
        classmatesListener?.remove()
        classmatesListener = null
    }

    private fun showStudentMainTab(tab: StudentMainTab, force: Boolean = false) {
        val b = _binding ?: return
        if (studentMainTab == tab && !force) return
        studentMainTab = tab
        updateStudentMainTabUi()
        listener?.remove()
        listener = null
        removeAllMessageListeners()
        stopStudentClassListeners()
        b.studentTabContainer.visibility = if (tab == StudentMainTab.QUESTIONS) View.VISIBLE else View.GONE
        b.studentClassInfo.visibility = if (tab == StudentMainTab.CLASS) View.VISIBLE else View.GONE
        b.emptyText.visibility = View.GONE
        val c = myClass
        when {
            tab == StudentMainTab.QUESTIONS || c == null -> {
                b.questionsRecyclerView.adapter = adapter
                if (studentTab == StudentTab.BEKLEYEN) subscribeToStudentPending() else subscribeToStudentResolved()
            }
            else -> {
                // Sınıftakiler sınıf kartının açtığı ekranda (StudentClassFragment); burada yalnızca iki kart.
                b.questionsRecyclerView.adapter = null
                b.studentClassName.text = c.name
                b.studentClassMeta.text = c.courseName
                b.studentClassMeta.visibility = if (c.courseName.isBlank()) View.GONE else View.VISIBLE
                ClassFaces.bind(b.studentClassFaces, List(minOf(c.studentCount, ClassFaces.MAX)) { null }, c.studentCount)
                b.studentClassCard.setOnClickListener { openStudentScreen(StudentClassFragment()) }
                b.studentHomeworkCard.setOnClickListener {
                    openStudentScreen(com.example.app.homework.StudentHomeworkListFragment.newInstance(c.id))
                }
                com.example.app.homework.DepthPress.attach(b.studentClassCard, b.studentHomeworkCard)
                b.studentHomeworkSubtitle.text = ""
                homeworkWatcher = com.example.app.homework.HomeworkCard.Watcher(c.id) onItems@{ items ->
                    val bb = _binding ?: return@onItems
                    val pending = items.count { !it.done }
                    bb.studentHomeworkSubtitle.text = when {
                        items.isEmpty() -> appString(R.string.hw_student_card_none)
                        pending == 0 -> appString(R.string.hw_student_card_all_done)
                        else -> appPlural(R.plurals.hw_student_card_pending, pending, pending)
                    }
                }
                classmatesListener = CourseClasses.listenMembers(c.id) { members -> showClassFaces(members.orEmpty()) }
            }
        }
    }

    /** Sınıf kartındaki avatarlar; avatarı bilinmeyenler bir kez okunuyor. */
    private fun showClassFaces(members: List<CourseClasses.ClassMember>) {
        if (_binding == null) return
        bindClassFaces(members)
        val missing = members.map { it.uid }.filterNot { classmateAvatars.containsKey(it) }
        if (missing.isNotEmpty()) {
            missing.forEach { classmateAvatars[it] = null } // okunurken yeniden istenmesin
            CourseClasses.loadAvatars(missing) { avatars ->
                classmateAvatars.putAll(avatars)
                if (_binding != null && studentMainTab == StudentMainTab.CLASS) bindClassFaces(members)
            }
        }
    }

    /** Sınıf kartındaki avatar dizisi: avatarı olanlar önce, en fazla [ClassFaces.MAX]. */
    private fun bindClassFaces(members: List<CourseClasses.ClassMember>) {
        val b = _binding ?: return
        val faces = members.sortedBy { classmateAvatars[it.uid] == null }.take(ClassFaces.MAX).map { classmateAvatars[it.uid] }
        ClassFaces.bind(b.studentClassFaces, faces, members.size)
    }

    private fun openStudentScreen(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainerID, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupTeacherApprovalPendingUi() {
        binding.teacherApprovalSupportButton.setOnClickListener {
            SupportContactHelper.openSupportEmail(this)
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
        updatePoolOptionsRow()
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
            updatePoolOptionsRow()
        }
    }

    /**
     * Öğretmen dokümanından gelen havuz görünürlüğünü kurar: hangi alt sekme açık başlar,
     * alt sekmeler ve "Sınıflarım" düğmesi görünür mü.
     */
    private fun setupPoolVisibility(visibility: CourseClasses.PoolVisibility) {
        poolVisibility = visibility
        // Kaydedilmiş sekme artık görülemeyen bir havuzu gösteriyorsa (öğretmen "genel havuza
        // yardım"ı kapattı) görülebilene düşüyor.
        poolSubTab = when {
            poolSubTab == PoolSubTab.MINE && visibility.classPool -> PoolSubTab.MINE
            poolSubTab == PoolSubTab.GENERAL && visibility.generalPool -> PoolSubTab.GENERAL
            visibility.classPool -> PoolSubTab.MINE
            else -> PoolSubTab.GENERAL
        }
        binding.poolSubTabMine.setOnClickListener { switchPoolSubTab(PoolSubTab.MINE) }
        binding.poolSubTabGeneral.setOnClickListener { switchPoolSubTab(PoolSubTab.GENERAL) }
        binding.btnTeacherClasses.setOnClickListener { openTeacherClasses() }
        updatePoolSubTabUi()
        updatePoolOptionsRow()
    }

    /** Alt sekmeler yalnızca iki havuzu da gören öğretmende ve Havuz sekmesinde; düğme her sekmede. */
    private fun updatePoolOptionsRow() {
        val v = poolVisibility ?: return
        val showSubTabs = v.classPool && v.generalPool && teacherTab == TeacherTab.POOL && !teacherSelectionMode
        val showClasses = v.isCourseTeacher && !teacherSelectionMode
        binding.poolSubTabContainer.visibility = if (showSubTabs) View.VISIBLE else View.GONE
        binding.btnTeacherClasses.visibility = if (showClasses) View.VISIBLE else View.GONE
        binding.poolOptionsRow.visibility = if (showSubTabs || showClasses) View.VISIBLE else View.GONE
    }

    private fun updatePoolSubTabUi() {
        val selectedBg = R.drawable.bg_segment_selected
        val selectedColor = android.graphics.Color.WHITE
        val unselectedColor = requireContext().getColor(R.color.dark_text_secondary)
        listOf(PoolSubTab.MINE to binding.poolSubTabMine, PoolSubTab.GENERAL to binding.poolSubTabGeneral)
            .forEach { (tab, view) ->
                view.setBackgroundResource(if (tab == poolSubTab) selectedBg else 0)
                view.setTextColor(if (tab == poolSubTab) selectedColor else unselectedColor)
            }
    }

    private fun switchPoolSubTab(tab: PoolSubTab) {
        if (poolSubTab == tab) return
        poolSubTab = tab
        updatePoolSubTabUi()
        listener?.remove()
        listener = null
        subscribeToPool()
    }

    private fun openTeacherClasses() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainerID, TeacherClassesFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun subscribeToPool() {
        val uid = auth.currentUser?.uid ?: run {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = appString(R.string.ui_giris_yapin)
            return
        }
        val mine = poolSubTab == PoolSubTab.MINE && poolVisibility?.classPool == true
        binding.emptyText.text = appString(if (mine) R.string.class_pool_empty_mine else R.string.ui_henuz_soru_yok)
        // İki sorgu da kurallardaki `teacherReaches` ile birebir (bkz. [poolVisibility]):
        // sınıf havuzu `classTeacherUids array-contains ben`, genel havuz `pool == general`.
        // İkisinin de bileşik indeksi firestore.indexes.json'da.
        val base = if (mine) {
            firestore.collection("questions").whereArrayContains("classTeacherUids", uid)
        } else {
            firestore.collection("questions").whereEqualTo("pool", CourseClasses.POOL_GENERAL)
        }
        val query = base
            .whereEqualTo("status", StudentQuestion.STATUS_PENDING)
            // Soru havuzu: yalnızca oluşturulma zamanına göre sırala.
            // DESC alıp client tarafında ASC'ye çeviriyoruz.
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
            binding.emptyText.text = appString(R.string.ui_giris_yapin)
            return
        }
        binding.emptyText.text = appString(R.string.ui_henuz_sohbet_yok)
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
        binding.emptyText.text = appString(R.string.ui_bekleyen_rapor_yok)
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
        PreviewSafeArea.apply(dialog.findViewById(R.id.dialogImageClose))
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
                timeView.text = appString(R.string.ui_ses_kaydini_dinlemek_icin_oynat)
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(requireContext(), appString(R.string.ui_ses_oynatilamadi), Toast.LENGTH_SHORT).show()
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
            .addOnSuccessListener { Toast.makeText(requireContext(), appString(R.string.ui_rapor_reddedildi), Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(requireContext(), appString(R.string.ui_islem_basarisiz_2), Toast.LENGTH_SHORT).show() }
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
            .addOnSuccessListener { Toast.makeText(requireContext(), appString(R.string.ui_kullanici_kisitlandi), Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), appString(R.string.ui_islem_basarisiz_x, e.message), Toast.LENGTH_SHORT).show() }
    }

    private fun subscribeToRestrictedUsers() {
        binding.emptyText.text = appString(R.string.ui_kisitli_kullanici_yok)
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
            .setTitle(appString(R.string.ui_kisitlamayi_kaldir))
            .setMessage(appString(R.string.ui_x_kullanicisinin_kisitlamasi_kaldirilsin_mi, user.name))
            .setPositiveButton(appString(R.string.ui_kaldir)) { _, _ -> removeRestriction(user.uid) }
            .setNegativeButton(appString(R.string.ui_iptal_2), null)
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
            .addOnSuccessListener { Toast.makeText(requireContext(), appString(R.string.ui_kisitlama_kaldirildi), Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), appString(R.string.ui_islem_basarisiz_x, e.message), Toast.LENGTH_SHORT).show() }
    }

    private fun showRestrictionDetailDialog(user: RestrictedUser) {
        val view = layoutInflater.inflate(R.layout.dialog_restriction_detail, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnDismissListener { releaseReportAudioPlayer() }

        view.findViewById<TextView>(R.id.restrictionDialogUserName).text = user.name
        val statusView = view.findViewById<TextView>(R.id.restrictionDialogStatus)
        if (user.banned) {
            statusView.text = appString(R.string.ui_kalici_ban)
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
            user.restrictionMessagePreview?.ifBlank { null } ?: appString(R.string.ui_bu_kisitlamanin_kaynagi_hakkinda_bilgi_yok)
        bindMediaPreview(view, user.restrictionMessageType, user.restrictionMediaUrl, user.restrictionThumbnailUrl)

        val metaView = view.findViewById<TextView>(R.id.restrictionDialogMeta)
        val restrictedAt = user.restrictedAt
        if (restrictedAt == null) {
            metaView.visibility = View.GONE
        } else {
            metaView.visibility = View.VISIBLE
            val dateText = android.text.format.DateFormat.format("d MMM yyyy, HH:mm", restrictedAt.toDate())
            metaView.text = appString(R.string.ui_x_tarihinde_kisitlandi, dateText)
            val byUid = user.restrictedByUid
            if (!byUid.isNullOrBlank()) {
                val cached = reportNameCache[byUid]
                if (cached != null) {
                    metaView.text = appString(R.string.ui_x_tarihinde_x_tarafindan_kisitlandi, dateText, cached)
                } else {
                    firestore.collection("users").document(byUid).get()
                        .addOnSuccessListener { doc ->
                            val teacherName = doc.getString("name")?.takeIf { it.isNotBlank() } ?: byUid.take(6)
                            reportNameCache[byUid] = teacherName
                            if (_binding != null && isAdded) {
                                metaView.text = appString(R.string.ui_x_tarihinde_x_tarafindan_kisitlandi, dateText, teacherName)
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
        if (until == null) return appString(R.string.ui_kisitli)
        val diff = until.toDate().time - System.currentTimeMillis()
        if (diff <= 0) return appString(R.string.ui_suresi_doldu)
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff) % 24
        return when {
            days >= 1 -> appPlural(R.plurals.ui_x_gun_kaldi, days, days)
            hours >= 1 -> appPlural(R.plurals.ui_x_saat_kaldi, hours, hours)
            else -> appString(R.string.ui_1_saatten_az_kaldi)
        }
    }

    private fun subscribeToStudentPending() {
        val uid = auth.currentUser?.uid ?: run {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = appString(R.string.ui_giris_yapin)
            return
        }
        binding.emptyText.text = appString(R.string.ui_bekleyen_soru_yok)
        val query = firestore.collection("questions")
            .whereEqualTo("studentUid", uid)
            // Süresi dolanlar da listede kalır: kullanıcı sorusunu ve yüklediği medyayı
            // kaybetmesin, tek dokunuşla tekrar sorabilsin (bkz. showResendQuestionDialog).
            .whereIn(
                "status",
                listOf(
                    StudentQuestion.STATUS_PENDING,
                    StudentQuestion.STATUS_CLAIMED,
                    StudentQuestion.STATUS_EXPIRED,
                )
            )
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
            binding.emptyText.text = appString(R.string.ui_giris_yapin)
            return
        }
        binding.emptyText.text = appString(R.string.ui_cozulen_soru_yok)
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
                Toast.makeText(requireContext(), appString(R.string.ui_cozuldu_olarak_isaretlenen_sorulari_silebili), Toast.LENGTH_LONG).show()
                return
            }
            AlertDialog.Builder(requireContext())
                .setTitle(appString(R.string.ui_soruyu_silmek_istedigine_emin_misin))
                .setMessage(appString(R.string.ui_bu_islem_geri_alinamaz))
                .setPositiveButton(appString(R.string.ui_kaldir)) { dialog, _ ->
                    dialog.dismiss()
                    firestore.collection("questions").document(question.id)
                        .update("deletedForUids", FieldValue.arrayUnion(uid))
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), appString(R.string.ui_listeden_kaldirildi), Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), appString(R.string.ui_kaldirilamadi), Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton(appString(R.string.ui_iptal_2)) { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle(appString(R.string.ui_soruyu_silmek_istedigine_emin_misin_2))
                .setMessage(appString(R.string.ui_bu_islem_geri_alinamaz_soruyu_sildiginde))
                .setPositiveButton(appString(R.string.ui_sil_2)) { dialog, _ ->
                    dialog.dismiss()
                    firestore.collection("questions").document(question.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), appString(R.string.ui_soru_silindi), Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), appString(R.string.ui_silinemedi), Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton(appString(R.string.ui_iptal_2)) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    /**
     * 48 saat cevapsız kalıp kredisi iade edilmiş soruyu tekrar kuyruğa almayı önerir.
     *
     * Kredi iade edildiği için tekrar sormak yeni bir kredi harcar — aksi halde
     * cevapsız kalan soru sonradan bedava cevaplanmış olurdu.
     */
    private fun showResendQuestionDialog(question: StudentQuestion) {
        val ctx = context ?: return
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(appString(R.string.ui_soru_cevaplanmadi))
            .setMessage(appString(R.string.ui_bu_soru_48_saat_icinde_cevaplanmadi))
            .setNegativeButton(appString(R.string.ui_vazgec), null)
            .setPositiveButton(appString(R.string.ui_tekrar_sor)) { _, _ -> resendQuestion(question) }
            .show()
    }

    private fun resendQuestion(question: StudentQuestion) {
        com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("resendTeacherQuestion")
            .call(hashMapOf("questionId" to question.id))
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), appString(R.string.ui_soru_tekrar_gonderildi), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                val insufficient = (e as? com.google.firebase.functions.FirebaseFunctionsException)?.code ==
                    com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION
                val text = if (insufficient) {
                    appString(R.string.ui_danisma_kredin_kalmadi_magazadan_kredi_alabi)
                } else {
                    appString(R.string.ui_gonderilemedi_x, e.message)
                }
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
            }
    }

    private fun onQuestionClick(question: StudentQuestion) {
        requireOnlineAndLoggedInOrLogin {
            // Süresi dolmuş soru sohbete açılmaz; kullanıcıya tekrar sorma seçeneği sunulur.
            if (!isTeacher && question.status == StudentQuestion.STATUS_EXPIRED) {
                showResendQuestionDialog(question)
                return@requireOnlineAndLoggedInOrLogin
            }
            if (isTeacher) {
                if (teacherSelectionMode && teacherTab == TeacherTab.CHATS) {
                    if (question.status == StudentQuestion.STATUS_RESOLVED) {
                        Toast.makeText(requireContext(), appString(R.string.ui_cozulen_sorulara_mesaj_gonderemezsin), Toast.LENGTH_SHORT).show()
                        return@requireOnlineAndLoggedInOrLogin
                    }
                    val title = if (question.previewText.isNotEmpty()) question.previewText else question.message
                    adapter.setTeacherSelectionMode(true, question.id)
                    (activity as? MainActivity)?.onTeacherChatSelectedFromNotification(question.id, title, question.studentUid)
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
        stopStudentClassListeners()
        removeRestrictedUserListeners()
        removeAllMessageListeners()
        releaseReportAudioPlayer()
        _binding = null
        super.onDestroyView()
    }
}
