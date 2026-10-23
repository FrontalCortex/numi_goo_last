package com.example.app.homework

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.app.CourseClasses
import com.example.app.R
import com.example.app.appString
import com.example.app.databinding.FragmentHomeworkCreateBinding
import com.example.app.databinding.ViewHwStepperBinding
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Öğretmenin yeni ödev formu: işlem, konu, tür, basamak, sayı adedi, hız, soru adedi ve kime.
 *
 * Sınırlar motordan geliyor ([HomeworkEngine.maxNumberCount], [HomeworkEngine.recommendedNumberCount],
 * [HomeworkEngine.forcedPattern]): her değişiklikte yeniden hesaplanıyor ve sayı adedi en fazla
 * değere kırpılıyor. Formun altındaki örnek soru aynı motordan; öğretmen göndermeden ne
 * çıkacağını görüyor. Sunucu ayrıca aralıkları doğruluyor (functions/homework.js).
 */
class HomeworkCreateFragment : Fragment() {

    companion object {
        private const val ARG_CLASS_ID = "class_id"

        fun newInstance(classId: String) = HomeworkCreateFragment().apply {
            arguments = Bundle().apply { putString(ARG_CLASS_ID, classId) }
        }
    }

    private var _binding: FragmentHomeworkCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var classId: String

    // Form durumu. Varsayılanlar bir kurs öğretmeninin ilk ödevi için makul: kuralsız toplama.
    private var op = HomeworkOp.ADD
    private val rules = linkedSetOf(AbacusRule.DIRECT)
    private var withAbacus = true
    private var digits = 1
    private var digits2 = 1
    private var numberCount = 3
    private var intervalMs = 1500L
    private var questionCount = 20
    private var wholeClass = true
    private val selectedUids = linkedSetOf<String>()
    private var members: List<CourseClasses.ClassMember> = emptyList()

    private var membersListener: ListenerRegistration? = null
    private var previewJob: Job? = null
    /** Program alanlara yazarken TextWatcher'lar durumu yeniden yazmasın. */
    private var updatingFields = false

    private lateinit var digitsStepper: Stepper
    private lateinit var digits2Stepper: Stepper
    private lateinit var countStepper: Stepper
    private lateinit var intervalStepper: Stepper
    private lateinit var questionsStepper: Stepper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeworkCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        classId = requireArguments().getString(ARG_CLASS_ID).orEmpty()
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.opAdd.setOnClickListener { setOp(HomeworkOp.ADD) }
        binding.opSub.setOnClickListener { setOp(HomeworkOp.SUB) }
        binding.opMul.setOnClickListener { setOp(HomeworkOp.MUL) }
        mapOf(
            binding.ruleDirect to AbacusRule.DIRECT, binding.ruleFive to AbacusRule.FIVE,
            binding.ruleTen to AbacusRule.TEN, binding.ruleBead to AbacusRule.BEAD,
        ).forEach { (view, rule) ->
            view.setOnClickListener {
                if (!rules.remove(rule)) rules += rule
                refresh()
            }
        }
        binding.typeAbacus.setOnClickListener { withAbacus = true; refresh() }
        binding.typeMental.setOnClickListener { withAbacus = false; refresh() }
        binding.targetAll.setOnClickListener { wholeClass = true; refresh() }
        binding.targetSelect.setOnClickListener { wholeClass = false; refresh() }
        binding.btnPreviewRefresh.setOnClickListener { refreshPreview() }
        binding.btnSend.setOnClickListener { send() }
        DepthPress.attach(
            binding.opAdd, binding.opSub, binding.opMul,
            binding.ruleDirect, binding.ruleFive, binding.ruleTen, binding.ruleBead,
            binding.typeAbacus, binding.typeMental, binding.targetAll, binding.targetSelect,
            binding.btnSend,
        )

        digitsStepper = Stepper(binding.digitsStepper, decimal = false) { v -> digits = v.toInt(); refresh() }
        digits2Stepper = Stepper(binding.digits2Stepper, decimal = false) { v -> digits2 = v.toInt(); refresh() }
        countStepper = Stepper(binding.countStepper, decimal = false) { v -> numberCount = v.toInt(); refresh() }
        intervalStepper = Stepper(binding.intervalStepper, decimal = true) { v -> intervalMs = Math.round(v * 1000); refresh() }
        questionsStepper = Stepper(binding.questionsStepper, decimal = false) { v -> questionCount = v.toInt(); refresh() }
        binding.intervalStepper.stepperLabel.text = appString(R.string.hw_field_interval)
        binding.questionsStepper.stepperLabel.text = appString(R.string.hw_field_questions)

        membersListener = CourseClasses.listenMembers(classId) { list ->
            members = list.orEmpty()
            selectedUids.retainAll(members.map { it.uid }.toSet())
            if (_binding != null) renderStudents()
        }
        refresh()
    }

    private fun setOp(newOp: HomeworkOp) {
        op = newOp
        refresh()
    }

    // ── Durum → ekran ───────────────────────────────────────────────────────

    /** Sınırları uygular, alanları yazar, uyarıları ve örneği yeniler. */
    private fun refresh() {
        val b = _binding ?: return
        clampState()
        updatingFields = true

        selectOption(b.opAdd, op == HomeworkOp.ADD)
        selectOption(b.opSub, op == HomeworkOp.SUB)
        selectOption(b.opMul, op == HomeworkOp.MUL)
        val isMul = op == HomeworkOp.MUL
        b.rulesSection.visibility = if (isMul) View.GONE else View.VISIBLE
        selectOption(b.ruleDirect, AbacusRule.DIRECT in rules)
        selectOption(b.ruleFive, AbacusRule.FIVE in rules)
        selectOption(b.ruleTen, AbacusRule.TEN in rules)
        selectOption(b.ruleBead, AbacusRule.BEAD in rules)
        selectOption(b.typeAbacus, withAbacus)
        selectOption(b.typeMental, !withAbacus)

        val cap = HomeworkEngine.capacityDigits(withAbacus)
        b.digitsStepper.stepperLabel.text = appString(if (isMul) R.string.hw_field_digits1 else R.string.hw_field_digits)
        digitsStepper.set(digits.toDouble())
        b.digits2Stepper.root.visibility = if (isMul) View.VISIBLE else View.GONE
        digits2Stepper.set(digits2.toDouble())
        if (isMul) {
            digits2Stepper.hint(appString(R.string.hw_mul_capacity, cap))
            digitsStepper.hint(null)
        } else {
            digitsStepper.hint(null)
        }

        b.countStepper.root.visibility = if (isMul) View.GONE else View.VISIBLE
        b.intervalStepper.root.visibility = if (isMul) View.GONE else View.VISIBLE
        if (!isMul) renderCountField()
        intervalStepper.set(intervalMs / 1000.0)
        intervalStepper.hint(appString(R.string.hw_field_interval_hint))
        questionsStepper.set(questionCount.toDouble())
        questionsStepper.hint(appString(R.string.hw_field_questions_hint, HomeworkEngine.blockCount(questionCount)))

        selectOption(b.targetAll, wholeClass)
        selectOption(b.targetSelect, !wholeClass)
        updatingFields = false
        renderStudents()
        refreshPreview()
    }

    private fun renderCountField() {
        val b = _binding ?: return
        b.countStepper.stepperLabel.text = appString(
            if (op == HomeworkOp.SUB) R.string.hw_field_count_sub else R.string.hw_field_count_add
        )
        countStepper.set(numberCount.toDouble())
        val max = currentMax()
        countStepper.warning(null)
        when {
            rules.isEmpty() -> countStepper.hint(appString(R.string.hw_field_rules_hint))
            max == 0 -> {
                countStepper.hint(null)
                countStepper.warning(appString(R.string.hw_count_impossible, digits))
            }
            else -> {
                val rec = HomeworkEngine.recommendedNumberCount(op, rules)
                val base = if (rec != null) appString(R.string.hw_count_max_recommended, max, rec)
                else appString(R.string.hw_count_max, max)
                countStepper.hint(
                    if (op == HomeworkOp.SUB) base + "\n" + appString(R.string.hw_field_count_sub_hint) else base
                )
                HomeworkEngine.forcedPattern(op, rules, numberCount)?.let { pattern ->
                    // Basamak sayısı kadar tekrar: 1 → "111".
                    val example = pattern.take(3).joinToString(if (op == HomeworkOp.ADD) " + " else " − ") { d ->
                        d.toString().repeat(digits)
                    } + " …"
                    val digitWord = pattern.distinct().joinToString("/")
                    countStepper.warning(
                        appString(R.string.hw_count_forced, numberCount, digitWord, example, rec ?: max)
                    )
                }
            }
        }
    }

    private fun currentMax(): Int = HomeworkEngine.maxNumberCount(op, rules, digits, withAbacus)

    /** Değerleri sınırlara çeker. Sayı adedi en fazla değerin üstündeyse ona iner. */
    private fun clampState() {
        val cap = HomeworkEngine.capacityDigits(withAbacus)
        if (op == HomeworkOp.MUL) {
            digits = digits.coerceIn(1, cap - 1)
            digits2 = digits2.coerceIn(1, cap - digits)
        } else {
            digits = digits.coerceIn(1, HomeworkEngine.maxDigits(withAbacus))
            val min = HomeworkEngine.minNumberCount(op)
            val max = currentMax()
            numberCount = if (max > 0) numberCount.coerceIn(min, max) else numberCount.coerceAtLeast(min)
        }
        intervalMs = intervalMs.coerceIn(HomeworkEngine.MIN_INTERVAL_MS, HomeworkEngine.MAX_INTERVAL_MS)
        questionCount = questionCount.coerceIn(HomeworkEngine.MIN_QUESTIONS, HomeworkEngine.MAX_QUESTIONS)
    }

    private fun selectOption(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_hw_chip_selected else R.drawable.bg_hw_chip)
        view.setTextColor(
            if (selected) android.graphics.Color.WHITE
            else ContextCompat.getColor(requireContext(), R.color.dark_text)
        )
    }

    private fun renderStudents() {
        val b = _binding ?: return
        b.studentChecks.removeAllViews()
        b.studentChecks.visibility = if (!wholeClass && members.isNotEmpty()) View.VISIBLE else View.GONE
        b.targetInfo.visibility = View.GONE
        if (wholeClass) {
            if (members.isEmpty()) {
                b.targetInfo.text = appString(R.string.hw_target_no_students)
                b.targetInfo.visibility = View.VISIBLE
            }
            return
        }
        if (members.isEmpty()) {
            b.targetInfo.text = appString(R.string.hw_target_no_students)
            b.targetInfo.visibility = View.VISIBLE
            return
        }
        val textColor = ContextCompat.getColor(requireContext(), R.color.dark_text)
        val accent = ContextCompat.getColorStateList(requireContext(), R.color.dark_primary)
        members.forEach { m ->
            val box = CheckBox(requireContext()).apply {
                text = m.name.ifBlank { m.userId }
                setTextColor(textColor)
                textSize = 15f
                minHeight = (44 * resources.displayMetrics.density).toInt()
                isChecked = m.uid in selectedUids
                CompoundButtonCompat.setButtonTintList(this, accent)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedUids += m.uid else selectedUids -= m.uid
                }
            }
            b.studentChecks.addView(box)
        }
    }

    private fun currentConfig(): HomeworkConfig = HomeworkConfig(
        op = op,
        rules = if (op == HomeworkOp.MUL) emptySet() else rules.toSet(),
        withAbacus = withAbacus,
        digits = digits,
        digits2 = if (op == HomeworkOp.MUL) digits2 else 0,
        numberCount = if (op == HomeworkOp.MUL) 0 else numberCount,
        intervalMs = if (op == HomeworkOp.MUL) 0L else intervalMs,
        questionCount = questionCount,
    )

    /** Örnek soru arka planda: elde gerektiren ayarların sınırında üretim birkaç yüz ms sürebiliyor. */
    private fun refreshPreview() {
        val cfg = currentConfig()
        // Üstteki özet kartı: öğretmen ödevi göndermeden adını ve ayarlarını tek bakışta görüyor.
        binding.previewTitle.text = if (cfg.op != HomeworkOp.MUL && cfg.rules.isEmpty()) appString(R.string.hw_preview_pick_rule)
        else HomeworkText.title(cfg)
        binding.previewMeta.text = HomeworkText.details(cfg)
        previewJob?.cancel()
        val invalid = HomeworkEngine.validate(cfg)
        if (invalid != null) {
            binding.previewText.text = "—"
            return
        }
        previewJob = viewLifecycleOwner.lifecycleScope.launch {
            val q = withContext(Dispatchers.Default) {
                runCatching { HomeworkEngine.generateQuestion(cfg, Random.Default) }.getOrNull()
            }
            val b = _binding ?: return@launch
            b.previewText.text = q?.let { "${HomeworkText.questionLine(it)} = ${it.answer}" } ?: "—"
        }
    }

    // ── Gönderme ────────────────────────────────────────────────────────────

    private fun send() {
        val b = _binding ?: return
        b.root.findFocus()?.clearFocus()
        clampState()
        val cfg = currentConfig()
        val error = when {
            op != HomeworkOp.MUL && rules.isEmpty() -> appString(R.string.hw_field_rules_hint)
            HomeworkEngine.validate(cfg) != null -> appString(R.string.hw_invalid)
            !wholeClass && selectedUids.isEmpty() -> appString(R.string.hw_target_none_selected)
            else -> null
        }
        if (error != null) {
            b.formError.text = error
            b.formError.visibility = View.VISIBLE
            return
        }
        b.formError.visibility = View.GONE
        setSending(true)
        Homeworks.create(classId, cfg, if (wholeClass) null else selectedUids.toList()) { recipients, err ->
            if (!isAdded || _binding == null) return@create
            setSending(false)
            if (err != null) {
                binding.formError.text = err
                binding.formError.visibility = View.VISIBLE
                return@create
            }
            Toast.makeText(requireContext(), appString(R.string.hw_sent, recipients ?: 0), Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setSending(sending: Boolean) {
        binding.btnSend.isEnabled = !sending
        binding.btnSend.text = if (sending) "" else appString(R.string.hw_send)
        binding.sendProgress.visibility = if (sending) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        membersListener?.remove()
        membersListener = null
        previewJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    /**
     * − değer + alanı. Elle yazılan değer de kabul ediliyor; sınır dışı değer [refresh]'te
     * kırpılıp alana geri yazılıyor. Adım büyüklüğü değere göre büyüyor: 200 soruya +1 ile
     * çıkılmasın.
     */
    private inner class Stepper(
        private val v: ViewHwStepperBinding,
        private val decimal: Boolean,
        private val onChange: (Double) -> Unit,
    ) {
        private var value = 0.0

        init {
            v.stepperValue.inputType = if (decimal) {
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                android.text.InputType.TYPE_CLASS_NUMBER
            }
            v.stepperMinus.setOnClickListener { onChange(value - step(value, down = true)) }
            v.stepperPlus.setOnClickListener { onChange(value + step(value, down = false)) }
            DepthPress.attach(v.stepperMinus, v.stepperPlus)
            v.stepperValue.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (updatingFields) return
                    val parsed = s?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: return
                    value = parsed
                }
            })
            // Yazma bitince (odak kaybı) değer işleniyor; her tuşta kırpılsaydı "15" yazarken
            // "1" kırpılıp alana geri yazılırdı.
            v.stepperValue.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onChange(value) }
        }

        private fun step(current: Double, down: Boolean): Double {
            if (decimal) {
                val probe = if (down) current - 0.001 else current
                return when {
                    probe < 1.0 -> 0.1
                    probe < 5.0 -> 0.5
                    else -> 1.0
                }
            }
            val probe = if (down) current - 1 else current
            return when {
                probe < 10 -> 1.0
                probe < 50 -> 5.0
                else -> 10.0
            }
        }

        fun set(newValue: Double) {
            value = if (decimal) Math.round(newValue * 10) / 10.0 else newValue
            val text = if (decimal) HomeworkText.formatSeconds(Math.round(value * 1000)) else value.toInt().toString()
            if (v.stepperValue.text?.toString() != text) v.stepperValue.setText(text)
        }

        fun hint(text: String?) {
            v.stepperHint.text = text
            v.stepperHint.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        fun warning(text: String?) {
            v.stepperWarning.text = text
            v.stepperWarning.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }
}
