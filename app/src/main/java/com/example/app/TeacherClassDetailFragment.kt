package com.example.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentTeacherClassDetailBinding
import com.example.app.databinding.ItemFollowUserBinding
import com.google.firebase.firestore.ListenerRegistration

/**
 * Bir sınıfın ayrıntısı: katılma kodu (kopyala / paylaş / yenile), kullanıcı adıyla davet ve
 * öğrenci listesi. Öğrenciye dokununca profili açılıyor; oradaki ilerleme düğmesi veli
 * panelini o öğrenci için açıyor (bkz. ProfileFragment).
 *
 * Bütün değişiklikler sunucuda (CourseClasses → callable); liste `members` dinleyicisinden
 * kendiliğinden yenileniyor, ekran sonucu elle listeye eklemiyor.
 */
class TeacherClassDetailFragment : Fragment() {

    companion object {
        private const val ARG_CLASS_ID = "class_id"
        private const val ARG_CLASS_NAME = "class_name"

        fun newInstance(classId: String, className: String) = TeacherClassDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CLASS_ID, classId)
                putString(ARG_CLASS_NAME, className)
            }
        }
    }

    private var _binding: FragmentTeacherClassDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var classId: String
    private var current: CourseClasses.CourseClass? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private val avatarCache = HashMap<String, String?>()
    private val adapter = StudentAdapter(
        onClick = { openProfile(it) },
        onRemove = { confirmRemove(it) },
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherClassDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        classId = requireArguments().getString(ARG_CLASS_ID).orEmpty()
        binding.classTitle.text = requireArguments().getString(ARG_CLASS_NAME).orEmpty()
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.studentList.layoutManager = LinearLayoutManager(requireContext())
        binding.studentList.adapter = adapter

        binding.btnCopyCode.setOnClickListener { copyCode() }
        binding.btnShareCode.setOnClickListener { shareCode() }
        binding.btnRegenerateCode.setOnClickListener { confirmRegenerate() }
        binding.btnInviteStudent.setOnClickListener { showInviteDialog() }

        listeners += CourseClasses.listenClass(classId) { c ->
            val b = _binding ?: return@listenClass
            current = c
            if (c == null) return@listenClass
            b.classTitle.text = c.name
            b.joinCode.text = CourseClasses.formatCode(c.joinCode)
        }
        listeners += CourseClasses.listenMembers(classId) { members -> showMembers(members) }
        CourseClasses.listenPendingInvites(classId) { names ->
            val b = _binding ?: return@listenPendingInvites
            b.pendingInvites.text = appString(R.string.class_pending_invites, names.joinToString(", "))
            b.pendingInvites.visibility = if (names.isEmpty()) View.GONE else View.VISIBLE
        }?.let { listeners += it }
    }

    private fun showMembers(members: List<CourseClasses.ClassMember>?) {
        val b = _binding ?: return
        b.studentLoading.visibility = View.GONE
        val list = members.orEmpty()
        b.studentsTitle.text = if (list.isEmpty()) appString(R.string.class_students_title)
        else "${appString(R.string.class_students_title)} (${list.size})"
        b.studentEmpty.text = appString(if (members == null) R.string.class_generic_error else R.string.class_students_empty)
        b.studentEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(list.map { it to avatarCache[it.uid] })

        // Avatarı henüz bilinmeyenler bir kez okunuyor; liste değiştikçe herkes yeniden okunmasın.
        val missing = list.map { it.uid }.filterNot { avatarCache.containsKey(it) }
        if (missing.isNotEmpty()) {
            CourseClasses.loadAvatars(missing) { avatars ->
                avatarCache.putAll(avatars)
                if (_binding == null) return@loadAvatars
                adapter.submitList(adapter.currentList.map { (m, _) -> m to avatarCache[m.uid] })
            }
        }
    }

    private fun copyCode() {
        val code = current?.joinCode?.takeIf { it.isNotBlank() } ?: return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
        Toast.makeText(requireContext(), appString(R.string.class_copied), Toast.LENGTH_SHORT).show()
    }

    private fun shareCode() {
        val c = current?.takeIf { it.joinCode.isNotBlank() } ?: return
        val text = appString(R.string.class_share_text, c.name, CourseClasses.formatCode(c.joinCode))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, null))
    }

    private fun confirmRegenerate() {
        AlertDialog.Builder(requireContext())
            .setTitle(appString(R.string.class_regenerate_title))
            .setMessage(appString(R.string.class_regenerate_message))
            .setPositiveButton(appString(R.string.class_regenerate_confirm)) { _, _ ->
                CourseClasses.regenerateCode(classId) { code, error ->
                    if (!isAdded) return@regenerateCode
                    val msg = error ?: appString(R.string.class_regenerated, CourseClasses.formatCode(code.orEmpty()))
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(appString(R.string.class_cancel), null)
            .show()
    }

    private fun showInviteDialog() {
        showClassInputDialog(
            fragment = this,
            title = appString(R.string.class_invite_title),
            message = appString(R.string.class_invite_message),
            hint = appString(R.string.class_invite_hint),
            confirmText = appString(R.string.class_invite_send),
            maxLength = 40,
        ) { userId, done ->
            CourseClasses.inviteStudent(classId, userId) { name, alreadyMember, error ->
                done(error)
                if (error != null || !isAdded) return@inviteStudent
                val shown = name?.takeIf { it.isNotBlank() } ?: userId
                val msg = if (alreadyMember) appString(R.string.class_invite_already_member, shown)
                else appString(R.string.class_invite_sent, shown)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmRemove(member: CourseClasses.ClassMember) {
        val shown = member.name.ifBlank { member.userId }
        AlertDialog.Builder(requireContext())
            .setTitle(appString(R.string.class_remove_title))
            .setMessage(appString(R.string.class_remove_message, shown))
            .setPositiveButton(appString(R.string.class_remove_confirm)) { _, _ ->
                CourseClasses.removeStudent(classId, member.uid) { error ->
                    if (!isAdded) return@removeStudent
                    Toast.makeText(requireContext(), error ?: appString(R.string.class_removed, shown), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(appString(R.string.class_cancel), null)
            .show()
    }

    private fun openProfile(member: CourseClasses.ClassMember) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainerID, ProfileFragment.newInstance(member.uid))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        listeners.forEach { it.remove() }
        listeners.clear()
        binding.studentList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    /**
     * Öğrenci satırı: takipçi listesinin satırı (item_follow_user) aynen. Sağdaki X orada
     * "takibi bırak", burada "sınıftan çıkar" — açıklaması kodda değiştiriliyor.
     */
    private class StudentAdapter(
        private val onClick: (CourseClasses.ClassMember) -> Unit,
        private val onRemove: (CourseClasses.ClassMember) -> Unit,
    ) : ListAdapter<Pair<CourseClasses.ClassMember, String?>, StudentAdapter.Holder>(Diff) {

        private val avatarColors = listOf("#1CB0F6", "#CE82FF", "#FF9600", "#FF4B4B", "#00CD9C", "#2B70C9", "#FF6B97", "#8DC63F")

        object Diff : DiffUtil.ItemCallback<Pair<CourseClasses.ClassMember, String?>>() {
            override fun areItemsTheSame(a: Pair<CourseClasses.ClassMember, String?>, b: Pair<CourseClasses.ClassMember, String?>) =
                a.first.uid == b.first.uid
            override fun areContentsTheSame(a: Pair<CourseClasses.ClassMember, String?>, b: Pair<CourseClasses.ClassMember, String?>) = a == b
        }

        class Holder(val binding: ItemFollowUserBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemFollowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (member, avatarConfig) = getItem(position)
            val b = holder.binding
            val name = member.name.ifBlank { member.userId }
            b.tvUserName.text = name
            b.tvUserCode.text = if (member.userId.isNotBlank()) "@${member.userId}" else ""
            val avatar = AvatarView.drawableFor(b.root.context, avatarConfig, b.tvAvatarLetter.layoutParams.width)
            if (avatar != null) {
                b.tvAvatarLetter.text = ""
                b.tvAvatarLetter.background = avatar
            } else {
                b.tvAvatarLetter.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                b.tvAvatarLetter.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(avatarColors[position % avatarColors.size]))
                }
            }
            b.btnFollowBack.visibility = View.GONE
            b.btnUnfollow.visibility = View.VISIBLE
            b.btnUnfollow.contentDescription = appString(R.string.class_remove_confirm)
            b.btnUnfollow.setOnClickListener { onRemove(member) }
            b.root.setOnClickListener { onClick(member) }
        }
    }
}
