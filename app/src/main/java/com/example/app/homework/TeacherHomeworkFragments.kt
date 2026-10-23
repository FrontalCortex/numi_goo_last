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
import com.example.app.databinding.ItemTeacherHomeworkBinding
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
    private var membersListener: ListenerRegistration? = null
    private lateinit var classId: String
    /** Bitiş özeti için sınıfın şu anki öğrencileri (sınıfa verilen ödevin hedefi). */
    private var memberUids: List<String>? = null
    private var homeworks: List<Homeworks.Homework>? = null

    /**
     * Bir kart: ödev ve bitirenler. Hedef ve bitirenler [Homeworks.Homework.targetUids] ve
     * [Homeworks.Homework.doneCount]: sınıftan ayrılan öğrenci sayıya girmiyor. [total] null:
     * üye listesi henüz gelmedi (çubuk gizli).
     */
    private data class Row(val hw: Homeworks.Homework, val done: Int, val total: Int?)

    private fun submit() {
        val members = memberUids
        adapter.submitList(homeworks.orEmpty().map { hw ->
            if (members == null) Row(hw, 0, null) else Row(hw, hw.doneCount(members), hw.targetUids(members).size)
        })
    }

    private val adapter = object : ListAdapter<Row, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(a: Row, b: Row) = a.hw.id == b.hw.id
            override fun areContentsTheSame(a: Row, b: Row) = a == b
        },
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val b = ItemTeacherHomeworkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return object : RecyclerView.ViewHolder(b.root) {}.also { it.itemView.tag = b }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val row = getItem(position)
            val hw = row.hw
            val b = holder.itemView.tag as ItemTeacherHomeworkBinding
            HomeworkText.bindOpBadge(b.hwOpBadge, hw.config.op)
            b.hwTitle.text = HomeworkText.title(hw.config)
            b.hwMeta.text = HomeworkText.compactDetails(hw.config)
            // Sınıfın tamamına verilen ödevde etiket yok (varsayılan); seçilenlere verildiyse kaç kişi.
            b.hwTarget.visibility = if (hw.wholeClass) View.GONE else View.VISIBLE
            b.hwTarget.text = targetLine(hw)
            val total = row.total
            b.hwDoneRow.visibility = if (total == null || total == 0) View.GONE else View.VISIBLE
            if (total != null && total > 0) {
                b.hwDoneBar.max = total
                b.hwDoneBar.progress = row.done
                b.hwDoneText.text = appString(R.string.hw_list_done_bar, row.done, total)
                b.hwDoneRow.contentDescription = when (row.done) {
                    0 -> appString(R.string.hw_list_done_none, total)
                    total -> appString(R.string.hw_list_done_all, total)
                    else -> appString(R.string.hw_list_done_summary, total, HomeworkText.withAccusative(row.done))
                }
            }
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
        binding.fabNew.visibility = View.VISIBLE
        binding.fabNew.setOnClickListener { openScreen(HomeworkCreateFragment.newInstance(classId)) }
        DepthPress.attach(binding.fabNew)
        // Liste kayarken düğme küçülüp yalnızca + kalıyor, kartların üstünü az kapatsın.
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val label = _binding?.fabNewLabel ?: return
                val show = when {
                    dy > 4 -> false
                    dy < -4 -> true
                    else -> return
                }
                if ((label.visibility == View.VISIBLE) == show) return
                android.transition.TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
                label.visibility = if (show) View.VISIBLE else View.GONE
            }
        })

        listener = Homeworks.listenClassHomeworks(classId) { list ->
            val b = _binding ?: return@listenClassHomeworks
            b.loading.visibility = View.GONE
            homeworks = list.orEmpty()
            submit()
            b.empty.text = appString(if (list == null) R.string.class_generic_error else R.string.hw_list_empty)
            b.empty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        membersListener = CourseClasses.listenMembers(classId) { list ->
            memberUids = list.orEmpty().map { it.uid }
            if (_binding != null) submit()
        }
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        membersListener?.remove()
        membersListener = null
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
        binding.btnToolbarAction.visibility = View.VISIBLE
        binding.btnToolbarAction.setOnClickListener { confirmDelete(homeworkId) }

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
                binding.btnToolbarAction.isEnabled = false
                Homeworks.delete(homeworkId) { error ->
                    if (!isAdded || _binding == null) return@delete
                    binding.btnToolbarAction.isEnabled = true
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
