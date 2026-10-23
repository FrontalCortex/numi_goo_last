package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentStudentClassBinding
import com.example.app.databinding.ItemStudentClassInviteBinding
import com.google.firebase.firestore.ListenerRegistration

/**
 * Öğrencinin kurs ve sınıf ekranı: sınıf koduyla katılma, öğretmenden gelen davetler ve şu
 * anki sınıf (ayrılma dahil).
 *
 * Giriş Ayarlar'daki "Kurs" satırından ve sınıf daveti bildiriminden (MainActivity
 * [MainActivity.EXTRA_OPEN_STUDENT_CLASS]). Bütün değişiklikler sunucuda (CourseClasses →
 * callable); ekran her işlemden sonra durumu baştan okuyor, sonucu kendisi uydurmuyor.
 *
 * Kod alanı yalnızca sınıfı olmayan öğrencide: sınıftaki öğrencinin kod girip sessizce sınıf
 * değiştirmesi yerine önce ayrılması ya da davetle (onay sorularak) geçmesi isteniyor.
 */
class StudentClassFragment : Fragment() {

    private var _binding: FragmentStudentClassBinding? = null
    private val binding get() = _binding!!

    private var invitesListener: ListenerRegistration? = null
    private var myClass: CourseClasses.MyClass? = null
    private var invites: List<CourseClasses.ClassInvite> = emptyList()

    // Sınıftakiler: önce öğretmen(ler), sonra öğrenciler (Sorularım → Sınıfım kartından gelindi).
    private var membersListener: ListenerRegistration? = null
    private var membersClassId: String? = null
    private var classTeachers: List<CourseClasses.ClassMember> = emptyList()
    private var lastMembers: List<CourseClasses.ClassMember> = emptyList()
    private val avatars = HashMap<String, String?>()
    private val classmateAdapter by lazy {
        ClassmateAdapter(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid) { member ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainerID, ProfileFragment.newInstance(member.uid))
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentClassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnJoin.setOnClickListener { joinWithCode() }
        binding.joinCodeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { joinWithCode(); true } else false
        }
        binding.btnLeaveClass.setOnClickListener { confirmLeave() }
        binding.classmateList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.classmateList.adapter = classmateAdapter

        invitesListener = CourseClasses.listenMyInvites { list ->
            invites = list
            if (_binding != null) renderInvites()
        }
        reload()
    }

    private fun reload() {
        val b = _binding ?: return
        b.studentClassLoading.visibility = View.VISIBLE
        b.studentClassError.visibility = View.GONE
        CourseClasses.loadMyClass { c, error ->
            val bb = _binding ?: return@loadMyClass
            bb.studentClassLoading.visibility = View.GONE
            if (error) {
                bb.studentClassError.visibility = View.VISIBLE
                bb.studentClassContent.visibility = View.GONE
                return@loadMyClass
            }
            myClass = c
            bb.studentClassContent.visibility = View.VISIBLE
            renderClass()
            renderInvites()
        }
    }

    private fun renderClass() {
        val b = _binding ?: return
        val c = myClass
        b.currentClassCard.visibility = if (c != null) View.VISIBLE else View.GONE
        b.joinCard.visibility = if (c == null) View.VISIBLE else View.GONE
        b.screenTitle.text = c?.name ?: appString(R.string.class_student_title)
        b.btnLeaveClass.visibility = if (c != null) View.VISIBLE else View.INVISIBLE
        if (c == null) {
            stopMembers()
            return
        }
        b.currentCourseName.text = c.courseName
        b.currentCourseName.visibility = if (c.courseName.isBlank()) View.GONE else View.VISIBLE
        if (membersClassId == c.id) return
        stopMembers()
        membersClassId = c.id
        CourseClasses.loadTeachers(c.teacherUids) { teachers ->
            classTeachers = teachers
            showMembers(lastMembers)
        }
        membersListener = CourseClasses.listenMembers(c.id) { list -> showMembers(list.orEmpty()) }
    }

    private fun stopMembers() {
        membersListener?.remove()
        membersListener = null
        membersClassId = null
        classTeachers = emptyList()
        lastMembers = emptyList()
        classmateAdapter.submitList(emptyList())
    }

    private fun showMembers(members: List<CourseClasses.ClassMember>) {
        val b = _binding ?: return
        lastMembers = members
        val rows = classTeachers + members.filter { m -> classTeachers.none { it.uid == m.uid } }
        classmateAdapter.teacherUids = classTeachers.map { it.uid }.toSet()
        classmateAdapter.submitList(rows.map { it to avatars[it.uid] })
        // Yalnızca kendisi varsa "henüz başka öğrenci yok".
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        b.classmatesEmpty.visibility = if (members.none { it.uid != myUid }) View.VISIBLE else View.GONE
        val missing = rows.map { it.uid }.filterNot { avatars.containsKey(it) }
        if (missing.isNotEmpty()) {
            missing.forEach { avatars[it] = null } // okunurken yeniden istenmesin
            CourseClasses.loadAvatars(missing) { loaded ->
                avatars.putAll(loaded)
                if (_binding != null) classmateAdapter.submitList(rows.map { it to avatars[it.uid] })
            }
        }
    }

    private fun renderInvites() {
        val b = _binding ?: return
        // Sınıf durumu okunmadan davet kartı çizilmiyor: "Katıl"ın sınıf değiştirme onayı
        // sorup sormayacağı şu anki sınıfa bağlı.
        if (b.studentClassContent.visibility != View.VISIBLE) return
        b.invitesContainer.removeAllViews()
        val visible = invites.filter { it.className.isNotBlank() || it.teacherName.isNotBlank() }
        b.invitesTitle.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
        b.invitesContainer.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
        visible.forEach { invite ->
            val row = ItemStudentClassInviteBinding.inflate(layoutInflater, b.invitesContainer, false)
            row.inviteText.text = appString(R.string.class_student_invite_text, invite.teacherName, invite.className)
            row.inviteCourse.text = invite.courseName
            row.inviteCourse.visibility = if (invite.courseName.isBlank()) View.GONE else View.VISIBLE
            row.inviteAccept.setOnClickListener { acceptInvite(invite, row) }
            row.inviteDecline.setOnClickListener { respond(invite, accept = false, row = row) }
            b.invitesContainer.addView(row.root)
        }
    }

    private fun acceptInvite(invite: CourseClasses.ClassInvite, row: ItemStudentClassInviteBinding) {
        val current = myClass
        if (current == null) {
            respond(invite, accept = true, row = row)
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(appString(R.string.class_student_switch_title))
            .setMessage(appString(R.string.class_student_switch_message, current.name, invite.className))
            .setPositiveButton(appString(R.string.class_student_switch_confirm)) { _, _ -> respond(invite, accept = true, row = row) }
            .setNegativeButton(appString(R.string.class_cancel), null)
            .show()
    }

    private fun respond(invite: CourseClasses.ClassInvite, accept: Boolean, row: ItemStudentClassInviteBinding) {
        setRowBusy(row, true)
        CourseClasses.respondInvite(invite.id, accept) { error ->
            if (!isAdded || _binding == null) return@respondInvite
            if (error != null) {
                setRowBusy(row, false)
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                return@respondInvite
            }
            if (accept) {
                Toast.makeText(requireContext(), appString(R.string.class_student_joined, invite.className), Toast.LENGTH_LONG).show()
            }
            // Davet listesi dinleyiciden kendiliğinden düşüyor; sınıf durumu ise elle okunuyor.
            reload()
        }
    }

    private fun setRowBusy(row: ItemStudentClassInviteBinding, busy: Boolean) {
        row.inviteProgress.visibility = if (busy) View.VISIBLE else View.GONE
        listOf(row.inviteAccept, row.inviteDecline).forEach {
            it.isEnabled = !busy
            it.alpha = if (busy) 0.5f else 1f
        }
    }

    private fun joinWithCode() {
        val b = _binding ?: return
        // Boşluk ve tireyi sunucu da atıyor; burada yalnızca boş/kısa girişi ağa göndermemek için.
        val code = b.joinCodeInput.text?.toString().orEmpty().replace(" ", "").replace("-", "").uppercase()
        if (code.length != 6) {
            showJoinError(appString(R.string.class_student_code_length))
            return
        }
        b.joinError.visibility = View.GONE
        setJoinBusy(true)
        hideKeyboard(b.joinCodeInput)
        CourseClasses.joinByCode(code) { className, error ->
            if (!isAdded || _binding == null) return@joinByCode
            setJoinBusy(false)
            if (error != null) {
                showJoinError(error)
                return@joinByCode
            }
            binding.joinCodeInput.text = null
            Toast.makeText(requireContext(), appString(R.string.class_student_joined, className.orEmpty()), Toast.LENGTH_LONG).show()
            reload()
        }
    }

    private fun showJoinError(text: String) {
        binding.joinError.text = text
        binding.joinError.visibility = View.VISIBLE
    }

    private fun setJoinBusy(busy: Boolean) {
        binding.btnJoin.isEnabled = !busy
        binding.btnJoin.text = if (busy) "" else appString(R.string.class_student_join_button)
        binding.joinProgress.visibility = if (busy) View.VISIBLE else View.GONE
        binding.joinCodeInput.isEnabled = !busy
    }

    private fun confirmLeave() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(appString(R.string.class_student_leave_title))
            .setMessage(appString(R.string.class_student_leave_message))
            .setPositiveButton(appString(R.string.class_student_leave)) { _, _ ->
                binding.btnLeaveClass.isEnabled = false
                CourseClasses.leaveMyClass { error ->
                    if (!isAdded || _binding == null) return@leaveMyClass
                    binding.btnLeaveClass.isEnabled = true
                    Toast.makeText(requireContext(), error ?: appString(R.string.class_student_left), Toast.LENGTH_SHORT).show()
                    if (error == null) reload()
                }
            }
            .setNegativeButton(appString(R.string.class_cancel), null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_error))
    }

    private fun hideKeyboard(view: TextView) {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroyView() {
        invitesListener?.remove()
        stopMembers()
        invitesListener = null
        _binding = null
        super.onDestroyView()
    }
}
