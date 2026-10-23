package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentTeacherClassesBinding
import com.example.app.databinding.ItemTeacherClassBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Kurs öğretmeninin sınıfları: liste, yeni sınıf açma ve "genel havuza da yardım et" tercihi
 * (tercih kartı şimdilik gizli).
 *
 * Giriş Havuz ekranındaki "Sınıflarım" düğmesinden; düğme yalnızca kursa bağlı öğretmende
 * görünüyor (bkz. NotificationFragment). Bir sınıfa dokununca [TeacherClassDetailFragment].
 */
class TeacherClassesFragment : Fragment() {

    private var _binding: FragmentTeacherClassesBinding? = null
    private val binding get() = _binding!!

    private var classesListener: ListenerRegistration? = null
    private val adapter = ClassAdapter { openClass(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherClassesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.classList.layoutManager = LinearLayoutManager(requireContext())
        binding.classList.adapter = adapter
        binding.btnNewClass.setOnClickListener { showNewClassDialog() }
        com.example.app.homework.DepthPress.attach(binding.btnNewClass)
        // "Genel havuza da yardım et" kartı şimdilik gizli; açılınca loadHelpGeneralPool() geri çağrılacak.

        classesListener = CourseClasses.listenMyClasses { list ->
            val b = _binding ?: return@listenMyClasses
            b.classLoading.visibility = View.GONE
            val items = list.orEmpty()
            adapter.submitList(items)
            b.classEmpty.text = appString(if (list == null) R.string.class_generic_error else R.string.class_list_empty)
            b.classEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /**
     * Anahtar dokümandaki değerle açılıyor. Değişince havuz sekmeleri bir sonraki açılışta
     * yenileniyor: NotificationFragment öğretmen dokümanını her açılışta okuyor.
     */
    private fun loadHelpGeneralPool() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.switchHelpGeneral.isEnabled = false
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val b = _binding ?: return@addOnSuccessListener
                b.switchHelpGeneral.isChecked = CourseClasses.poolVisibilityOf(doc).helpsGeneralPool
                b.switchHelpGeneral.isEnabled = true
                b.switchHelpGeneral.setOnCheckedChangeListener { sw, checked ->
                    sw.isEnabled = false
                    CourseClasses.setHelpsGeneralPool(checked) { ok ->
                        val bb = _binding ?: return@setHelpsGeneralPool
                        bb.switchHelpGeneral.isEnabled = true
                        if (!ok) {
                            // Yazılamadıysa anahtar eski yerine dönüyor; dinleyici geçici olarak
                            // susturulmazsa geri dönüş yeni bir yazım denemesi tetiklerdi.
                            bb.switchHelpGeneral.setOnCheckedChangeListener(null)
                            bb.switchHelpGeneral.isChecked = !checked
                            loadHelpGeneralPool()
                            Toast.makeText(requireContext(), appString(R.string.class_generic_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun showNewClassDialog() {
        showClassInputDialog(
            fragment = this,
            title = appString(R.string.class_new_title),
            message = null,
            hint = appString(R.string.class_new_hint),
            confirmText = appString(R.string.class_new_create),
            maxLength = 40,
        ) { name, done ->
            CourseClasses.createClass(name) { code, error ->
                done(error)
                if (error == null && code != null && isAdded) {
                    Toast.makeText(requireContext(), appString(R.string.class_created, CourseClasses.formatCode(code)), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openClass(item: CourseClasses.CourseClass) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainerID, TeacherClassDetailFragment.newInstance(item.id, item.name))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        classesListener?.remove()
        classesListener = null
        binding.classList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private class ClassAdapter(
        private val onClick: (CourseClasses.CourseClass) -> Unit,
    ) : ListAdapter<CourseClasses.CourseClass, ClassAdapter.Holder>(Diff) {

        object Diff : DiffUtil.ItemCallback<CourseClasses.CourseClass>() {
            override fun areItemsTheSame(a: CourseClasses.CourseClass, b: CourseClasses.CourseClass) = a.id == b.id
            override fun areContentsTheSame(a: CourseClasses.CourseClass, b: CourseClasses.CourseClass) = a == b
        }

        class Holder(val binding: ItemTeacherClassBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemTeacherClassBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = getItem(position)
            val b = holder.binding
            b.className.text = item.name
            b.root.contentDescription = item.name + ", " + appPlural(R.plurals.class_student_count, item.studentCount, item.studentCount)
            bindFaces(b, item)
            b.root.setOnClickListener { onClick(item) }
            com.example.app.homework.DepthPress.attach(b.root)
        }

        /** Sınıf id → ilk öğrencilerin avatarları (null: avatarı yok). Her sınıf bir kez okunuyor. */
        private val faces = HashMap<String, List<String?>>()
        private val loading = HashSet<String>()

        private fun bindFaces(b: ItemTeacherClassBinding, item: CourseClasses.CourseClass) {
            val row = b.classFaces
            row.removeAllViews()
            if (item.studentCount <= 0) {
                row.visibility = View.GONE
                b.classMeta.text = appString(R.string.class_card_no_students)
                b.classMeta.visibility = View.VISIBLE
                return
            }
            row.visibility = View.VISIBLE
            b.classMeta.visibility = View.GONE
            val avatars = faces[item.id]
            if (avatars == null) {
                if (loading.add(item.id)) {
                    CourseClasses.loadMemberPreview(item.id, MAX_FACES.toLong()) { uids ->
                        CourseClasses.loadAvatars(uids) { map ->
                            loading.remove(item.id)
                            faces[item.id] = uids.map { map[it] }
                            val index = currentList.indexOfFirst { it.id == item.id }
                            if (index >= 0) notifyItemChanged(index)
                        }
                    }
                }
            }
            val ctx = row.context
            val density = ctx.resources.displayMetrics.density
            val size = (28 * density).toInt()
            val shown = (avatars ?: List(minOf(item.studentCount, MAX_FACES)) { null }).take(MAX_FACES)
            shown.forEachIndexed { i, encoded ->
                val face = android.widget.TextView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                        if (i > 0) marginStart = (-8 * density).toInt() // üst üste binsin
                    }
                    gravity = android.view.Gravity.CENTER
                    background = com.example.app.AvatarView.drawableFor(ctx, encoded, size)
                        ?: android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(android.graphics.Color.parseColor(FACE_COLORS[i % FACE_COLORS.size]))
                            setStroke((2 * density).toInt(), androidx.core.content.ContextCompat.getColor(ctx, R.color.bright_background_color))
                        }
                }
                row.addView(face)
            }
            val more = item.studentCount - shown.size
            if (more > 0) {
                row.addView(android.widget.TextView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, size,
                    ).apply { marginStart = (6 * density).toInt() }
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    text = appString(R.string.class_card_more, more)
                    setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.dark_text_secondary))
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
            }
        }

        companion object {
            private const val MAX_FACES = 4
            private val FACE_COLORS = listOf("#1CB0F6", "#CE82FF", "#FF9600", "#00CD9C")
        }
    }
}

/**
 * Sınıf ekranlarının tek alanlı giriş penceresi. [onSubmit] işi yapar ve bitince `done(hata)`
 * çağırır: hata null ise pencere kapanır, değilse mesaj pencerede gösterilir ve kullanıcı
 * yazdığını kaybetmeden düzeltebilir.
 */
internal fun showClassInputDialog(
    fragment: Fragment,
    title: String,
    message: String?,
    hint: String,
    confirmText: String,
    maxLength: Int,
    onSubmit: (text: String, done: (error: String?) -> Unit) -> Unit,
) {
    val view = fragment.layoutInflater.inflate(R.layout.dialog_class_text_input, null)
    val dialog = AlertDialog.Builder(fragment.requireContext()).setView(view).create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val field = view.findViewById<EditText>(R.id.inputField)
    val error = view.findViewById<TextView>(R.id.inputError)
    val confirm = view.findViewById<TextView>(R.id.inputConfirm)
    val progress = view.findViewById<View>(R.id.inputProgress)
    view.findViewById<TextView>(R.id.inputTitle).text = title
    view.findViewById<TextView>(R.id.inputMessage).apply {
        text = message
        visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    field.hint = hint
    field.filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
    confirm.text = confirmText
    view.findViewById<View>(R.id.inputCancel).setOnClickListener { dialog.dismiss() }

    fun submit() {
        val text = field.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        error.visibility = View.GONE
        confirm.isEnabled = false
        confirm.alpha = 0.5f
        progress.visibility = View.VISIBLE
        onSubmit(text) { err ->
            if (!dialog.isShowing) return@onSubmit
            if (err == null) {
                dialog.dismiss()
                return@onSubmit
            }
            confirm.isEnabled = true
            confirm.alpha = 1f
            progress.visibility = View.GONE
            error.text = err
            error.visibility = View.VISIBLE
        }
    }
    confirm.setOnClickListener { submit() }
    field.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
    }
    field.imeOptions = EditorInfo.IME_ACTION_DONE
    dialog.setOnShowListener {
        field.requestFocus()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
    dialog.show()
}
