package com.example.app.homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.CourseClasses
import com.example.app.R
import com.example.app.appPlural
import com.example.app.appString
import com.example.app.databinding.FragmentTeacherHomeworkListBinding
import com.example.app.databinding.ItemFollowUserBinding
import com.example.app.databinding.ItemTeacherClassBinding
import com.google.firebase.firestore.ListenerRegistration

private fun Fragment.openScreen(fragment: Fragment) {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
        .replace(R.id.fragmentContainerID, fragment)
        .addToBackStack(null)
        .commit()
}

private fun targetLine(hw: Homeworks.Homework): String =
    if (hw.wholeClass) appString(R.string.hw_target_whole_class)
    else appPlural(R.plurals.hw_target_students, hw.assignedUids.size, hw.assignedUids.size)

/**
 * Bir sınıfın ödevleri. Giriş sınıf ayrıntısındaki "Ödevler" düğmesinden; ödeve dokununca
 * öğrencilerin ilerlemesi ([TeacherHomeworkDetailFragment]), alttaki düğmeyle yeni ödev.
 */
class TeacherHomeworkListFragment : Fragment() {

    companion object {
        private const val ARG_CLASS_ID = "class_id"
        private const val ARG_CLASS_NAME = "class_name"

        fun newInstance(classId: String, className: String) = TeacherHomeworkListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CLASS_ID, classId)
                putString(ARG_CLASS_NAME, className)
            }
        }
    }

    private var _binding: FragmentTeacherHomeworkListBinding? = null
    private val binding get() = _binding!!
    private var listener: ListenerRegistration? = null
    private lateinit var classId: String

    private val adapter = object : ListAdapter<Homeworks.Homework, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Homeworks.Homework>() {
            override fun areItemsTheSame(a: Homeworks.Homework, b: Homeworks.Homework) = a.id == b.id
            override fun areContentsTheSame(a: Homeworks.Homework, b: Homeworks.Homework) = a == b
        },
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val b = ItemTeacherClassBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            (b.root.layoutParams as? ViewGroup.MarginLayoutParams)?.apply { marginStart = dp(16); marginEnd = dp(16) }
            return object : RecyclerView.ViewHolder(b.root) {}.also { it.itemView.tag = b }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val hw = getItem(position)
            val b = holder.itemView.tag as ItemTeacherClassBinding
            b.className.text = HomeworkText.title(hw.config)
            b.className.maxLines = 2
            b.classMeta.text = HomeworkText.details(hw.config) + "\n" + targetLine(hw)
            b.root.setOnClickListener { openScreen(TeacherHomeworkDetailFragment.newInstance(hw.id, classId)) }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherHomeworkListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        classId = requireArguments().getString(ARG_CLASS_ID).orEmpty()
        binding.screenTitle.text = appString(R.string.hw_list_title, requireArguments().getString(ARG_CLASS_NAME).orEmpty())
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.bottomButton.text = appString(R.string.hw_new_button)
        binding.bottomButton.setOnClickListener { openScreen(HomeworkCreateFragment.newInstance(classId)) }

        listener = Homeworks.listenClassHomeworks(classId) { list ->
            val b = _binding ?: return@listenClassHomeworks
            b.loading.visibility = View.GONE
            adapter.submitList(list.orEmpty())
            b.empty.text = appString(if (list == null) R.string.class_generic_error else R.string.hw_list_empty)
            b.empty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        binding.list.adapter = null
        _binding = null
        super.onDestroyView()
    }
}

/**
 * Bir ödevin ayrıntısı: ayarlar ve öğrenci başına ilerleme (adım, doğru sayısı). Sınıfa
 * verilen ödevde şu anki bütün öğrenciler, seçilerek verilende seçilenler listeleniyor; o
 * arada sınıftan ayrılanlar "Sınıftan ayrılmış" diye kalıyor (ilerlemeleri duruyor).
 */
class TeacherHomeworkDetailFragment : Fragment() {

    companion object {
        private const val ARG_HOMEWORK_ID = "homework_id"
        private const val ARG_CLASS_ID = "class_id"

        fun newInstance(homeworkId: String, classId: String) = TeacherHomeworkDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_HOMEWORK_ID, homeworkId)
                putString(ARG_CLASS_ID, classId)
            }
        }
    }

    private data class Row(val uid: String, val name: String, val status: String, val inClass: Boolean)

    private var _binding: FragmentTeacherHomeworkListBinding? = null
    private val binding get() = _binding!!
    private val listeners = mutableListOf<ListenerRegistration>()
    private var homework: Homeworks.Homework? = null
    private var members: List<CourseClasses.ClassMember> = emptyList()
    private var progress: Map<String, Homeworks.Progress> = emptyMap()

    private val adapter = object : ListAdapter<Row, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(a: Row, b: Row) = a.uid == b.uid
            override fun areContentsTheSame(a: Row, b: Row) = a == b
        },
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val b = ItemFollowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return object : RecyclerView.ViewHolder(b.root) {}.also { it.itemView.tag = b }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val row = getItem(position)
            val b = holder.itemView.tag as ItemFollowUserBinding
            b.tvUserName.text = row.name
            b.tvUserCode.text = row.status
            b.tvAvatarLetter.text = row.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            b.tvAvatarLetter.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(ContextCompat.getColor(holder.itemView.context, if (row.inClass) R.color.dark_primary else R.color.outline_button_stroke))
            }
            b.btnFollowBack.visibility = View.GONE
            b.btnUnfollow.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherHomeworkListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeworkId = requireArguments().getString(ARG_HOMEWORK_ID).orEmpty()
        val classId = requireArguments().getString(ARG_CLASS_ID).orEmpty()
        binding.screenTitle.text = appString(R.string.hw_detail_title)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.sectionTitle.visibility = View.VISIBLE
        binding.bottomButton.text = appString(R.string.hw_delete)
        binding.bottomButton.setBackgroundResource(R.drawable.bg_button_outline)
        binding.bottomButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_error))
        binding.bottomButton.setOnClickListener { confirmDelete(homeworkId) }

        Homeworks.loadHomework(homeworkId) { hw ->
            val b = _binding ?: return@loadHomework
            homework = hw
            if (hw == null) {
                b.loading.visibility = View.GONE
                b.empty.text = appString(R.string.class_generic_error)
                b.empty.visibility = View.VISIBLE
                return@loadHomework
            }
            b.headerCard.visibility = View.VISIBLE
            b.headerTitle.text = HomeworkText.title(hw.config)
            b.headerDetails.text = HomeworkText.details(hw.config)
            b.headerTarget.text = targetLine(hw)
            render()
        }
        listeners += CourseClasses.listenMembers(classId) { list -> members = list.orEmpty(); render() }
        listeners += Homeworks.listenAllProgress(homeworkId) { map -> progress = map.orEmpty(); render() }
    }

    private fun render() {
        val b = _binding ?: return
        val hw = homework ?: return
        b.loading.visibility = View.GONE
        val memberById = members.associateBy { it.uid }
        val uids = if (hw.wholeClass) members.map { it.uid } else hw.assignedUids
        // Sınıftan ayrılmış ama ödevde ilerlemesi olanlar da görünsün.
        val all = (uids + progress.keys).distinct()
        val rows = all.map { uid ->
            val m = memberById[uid]
            val p = progress[uid]
            val status = when {
                p == null || p.completedBlocks == 0 -> appString(R.string.hw_detail_not_started)
                p.completedBlocks >= hw.blockCount -> appString(R.string.hw_detail_done, p.correct, p.answered)
                else -> appString(R.string.hw_detail_progress, p.completedBlocks, hw.blockCount, p.correct, p.answered)
            }
            val name = m?.name?.ifBlank { m.userId } ?: appString(R.string.hw_left_class)
            Row(uid, name, status, inClass = m != null)
        }.sortedWith(compareBy({ !it.inClass }, { it.name.lowercase(java.util.Locale("tr")) }))
        adapter.submitList(rows)
        b.empty.text = appString(R.string.hw_target_no_students)
        b.empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(homeworkId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(appString(R.string.hw_delete_title))
            .setMessage(appString(R.string.hw_delete_message))
            .setPositiveButton(appString(R.string.hw_delete)) { _, _ ->
                binding.bottomButton.isEnabled = false
                Homeworks.delete(homeworkId) { error ->
                    if (!isAdded || _binding == null) return@delete
                    binding.bottomButton.isEnabled = true
                    Toast.makeText(requireContext(), error ?: appString(R.string.hw_deleted), Toast.LENGTH_SHORT).show()
                    if (error == null) parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton(appString(R.string.class_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        listeners.forEach { it.remove() }
        listeners.clear()
        binding.list.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
