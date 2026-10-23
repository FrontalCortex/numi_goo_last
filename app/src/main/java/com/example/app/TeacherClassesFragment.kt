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
 * Kurs öğretmeninin sınıfları: liste, yeni sınıf açma ve "genel havuza da yardım et" tercihi.
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
        loadHelpGeneralPool()

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
            holder.binding.className.text = item.name
            holder.binding.classMeta.text = appPlural(R.plurals.class_student_count, item.studentCount, item.studentCount) +
                " · " + appString(R.string.class_code_label, CourseClasses.formatCode(item.joinCode))
            holder.binding.root.setOnClickListener { onClick(item) }
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
