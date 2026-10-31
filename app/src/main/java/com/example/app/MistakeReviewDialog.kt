package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.app.abacus.AbacusBeadController
import com.example.app.abacus.MistakeAnalyzer
import com.example.app.abacus.StepExplainer
import com.example.app.homework.DepthPress
import com.example.app.homework.HomeworkOp
import com.google.firebase.auth.FirebaseAuth

/**
 * "Sobi ile analiz et" ekranı: yanlış cevaplanan soruyu Sobi anlatarak, adım adım yeniden çözdürür
 * (kullanıcı kararı 31.10.2026; docs/ANLAMADIM_YARDIM_PLANI.md "B aşaması").
 *
 * Soru bir sayı dizisi: ilk sayı, sonra her biri eklenen (+) ya da çıkarılan (−) sayılar. Ders
 * ekranında `[ilk, ±ikinci]`, abaküssüz derste flaş dizisi (23 + 33 + 12 + 22).
 *
 * Akış: Sobi önce hatayı söyler ([MistakeAnalyzer] teşhis ettiyse: yalnızca abaküslü iki sayılı
 * soruda, değer kaydı varken), sonra soru adımlara bölünür: ilk sayıyı yazmak, ardından her sayının
 * her basamağı ([StepExplainer] hamleleri). Çocuk her adımı abaküste yapıp "Kontrol Et"e basar;
 * yanlışsa Sobi o adımda ne yaptığını söyler, "Bana göster" hamleyi oynatır ([SobiAbacusPlayer]).
 * Asıl soruda yanıldığı adıma gelince uyarılır. Teşhis yoksa (abaküssüz ders) çocuğun burada ilk
 * yanıldığı adım hatanın yeri sayılır.
 *
 * Anlatım yapay zekâsız (kural motoru): kesin ve ücretsiz. Sağ üstteki Sobi kafası yapay zekâ
 * sohbetini o anki adımın bağlamıyla açar (oturum açıksa).
 *
 * Tam ekran DialogFragment: ders ekranı altta olduğu gibi kalır, kapanınca çocuk yanlış cevap
 * panelindeki "Tamam" ile devam eder.
 */
class MistakeReviewDialog : DialogFragment() {

    /** [numberIndex]: işlenen sayının dizideki yeri (0 = ilk sayıyı yazmak). */
    private data class Step(
        val base: Int,
        val target: Int,
        val move: StepExplainer.ColumnMove?,
        val text: String,
        val numberIndex: Int,
    )

    private var numbers: List<Int> = emptyList()
    private var answer = 0
    private var lessonTitle: String? = null

    private var steps: List<Step> = emptyList()
    private var diagnosis: MistakeAnalyzer.Diagnosis? = null
    private var stepIndex = 0
    private var finished = false
    /** Analiz ekranında çocuğun ilk yanıldığı adım (teşhis yoksa hatanın yeri sayılır). */
    private var firstReviewMistake = -1

    private var controller: AbacusBeadController? = null
    private var player: SobiAbacusPlayer? = null
    private val stepLog = ArrayList<Int>()
    private var recording = true

    private val tracker = SobiStepTracker()
    private var sheet: SobiChatSheet? = null

    private lateinit var sobiText: TextView
    private lateinit var stepLabel: TextView
    private lateinit var showButton: TextView
    private lateinit var checkButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val a = requireArguments()
        numbers = a.getIntArray(ARG_NUMBERS)?.toList().orEmpty()
        answer = a.getInt(ARG_ANSWER)
        lessonTitle = a.getString(ARG_LESSON)
        val log = a.getIntArray(ARG_LOG)?.toList().orEmpty()
        diagnosis = if (log.isNotEmpty() && numbers.size == 2) {
            MistakeAnalyzer.analyze(numbers[0], if (numbers[1] < 0) "-" else "+", Math.abs(numbers[1]), log, answer)
        } else null
        steps = buildSteps(numbers)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_mistake_review, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sobiText = view.findViewById(R.id.reviewSobiText)
        stepLabel = view.findViewById(R.id.reviewStepLabel)
        showButton = view.findViewById(R.id.reviewShow)
        checkButton = view.findViewById(R.id.reviewCheck)
        view.findViewById<TextView>(R.id.reviewQuestion).text = questionText(numbers)
        view.findViewById<TextView>(R.id.reviewYourAnswer).text = appString(R.string.review_your_answer, answer)
        view.findViewById<View>(R.id.reviewClose).setOnClickListener { dismissAllowingStateLoss() }

        val ask = view.findViewById<View>(R.id.reviewAskSobi)
        ask.visibility = if (FirebaseAuth.getInstance().currentUser != null) View.VISIBLE else View.GONE
        ask.setOnClickListener { openSobi() }
        showButton.setOnClickListener { showCurrentStep() }
        checkButton.setOnClickListener { onCheck() }
        DepthPress.attach(ask, showButton, checkButton)

        // Abaküs: ödev oynatıcısıyla aynı kurulum (boncukları kimlikleriyle bulur).
        val c = AbacusBeadController(context = requireContext(), root = view, animationDurationMs = 300L)
        c.setup()
        c.onValueChanged = {
            if (recording && !c.isResetInProgress()) {
                val v = c.getCurrentValue()
                if (stepLog.lastOrNull() != v && stepLog.size < 400) stepLog += v
                tracker.record(v)
            }
        }
        controller = c
        player = SobiAbacusPlayer(c)
        view.findViewById<View>(R.id.abacusLinear).post {
            if (isAdded && c.computeMovementDistancesFromLayout(ratio = 1.0f, force = true)) c.syncStateFromUi()
        }

        if (steps.isEmpty()) {
            sobiText.text = appString(R.string.review_intro_unknown)
            finish()
            return
        }
        startStep(0, intro = diagnosis?.let { appString(R.string.review_intro_found, it.text) }
            ?: appString(R.string.review_intro_unknown))
    }

    override fun onDestroyView() {
        player?.cancel()
        sheet?.dismiss()
        sheet = null
        super.onDestroyView()
    }

    // ── Adımlar ─────────────────────────────────────────────────────────────

    private fun startStep(i: Int, intro: String? = null) {
        stepIndex = i
        val step = steps[i]
        stepLabel.text = if (numbers.size > 2 && step.numberIndex > 0) {
            appString(R.string.review_step_label_number, i + 1, steps.size, signed(numbers[step.numberIndex]))
        } else {
            appString(R.string.review_step_label, i + 1, steps.size)
        }
        val m = step.move
        val wasHere = m != null && diagnosis?.move?.let { it.col == m.col && it.valueAfter == m.valueAfter } == true
        val parts = listOfNotNull(intro, if (wasHere) appString(R.string.review_was_here) else null, step.text)
        sobiText.text = parts.joinToString("\n\n")
        stepLog.clear()
        stepLog += controller?.getCurrentValue() ?: step.base
        tracker.startStep(i)
        tracker.captureBase(step.base)
        showButton.visibility = View.VISIBLE
        setButtonsEnabled(true)
    }

    private fun onCheck() {
        if (finished) { dismissAllowingStateLoss(); return }
        val c = controller ?: return
        val step = steps[stepIndex]
        val value = c.getCurrentValue()
        tracker.onAnswerChecked()
        if (value == step.target) {
            sobiText.text = appString(R.string.review_correct)
            setButtonsEnabled(false)
            sobiText.postDelayed({
                if (!isAdded) return@postDelayed
                if (stepIndex + 1 < steps.size) startStep(stepIndex + 1) else finish()
            }, 900L)
            return
        }
        val detail = step.move?.let { MistakeAnalyzer.diagnoseStep(it, stepLog, value).text }
            ?: MistakeAnalyzer.diagnoseWrite(step.target, value)
        val found = if (diagnosis == null && firstReviewMistake == -1) {
            firstReviewMistake = stepIndex
            appString(R.string.review_found_now) + " "
        } else ""
        sobiText.text = found + appString(R.string.review_wrong, detail)
    }

    /** "Bana göster": bu adımın hamlesini adımın başından oynatır; sonunda abaküs yine başa döner. */
    private fun showCurrentStep() {
        val step = steps.getOrNull(stepIndex) ?: return
        val action = if (step.move == null) {
            ServerSobi.Action(ServerSobi.ACTION_SHOW, 0, "add", step.target, step.target)
        } else {
            val amount = Math.abs(step.target - step.base)
            ServerSobi.Action(ServerSobi.ACTION_SHOW, step.base, if (step.target >= step.base) "add" else "sub", amount, step.target)
        }
        play(action)
    }

    private fun play(action: ServerSobi.Action) {
        val c = controller ?: return
        val p = player ?: return
        recording = false
        tracker.paused = true
        c.setEnabled(false)
        setButtonsEnabled(false)
        val restore = {
            recording = true
            tracker.paused = false
            c.setEnabled(true)
            setButtonsEnabled(true)
        }
        val started = p.play(action) {
            if (!isAdded) return@play
            restore()
            stepLog.clear()
            stepLog += c.getCurrentValue()
        }
        if (!started) restore()
    }

    private fun finish() {
        finished = true
        val tip = diagnosis?.move?.let { m ->
            appString(
                R.string.review_done_tip,
                MistakeAnalyzer.columnName(m.col),
                m.steps.joinToString(", ") { "\"${MistakeAnalyzer.stepPhrase(it, m)}\"" },
            )
        }.orEmpty()
        stepLabel.text = ""
        sobiText.text = appString(R.string.review_done, questionText(numbers), numbers.sum()) + tip
        showButton.visibility = View.GONE
        checkButton.text = appString(R.string.review_back)
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        showButton.isEnabled = enabled
        checkButton.isEnabled = enabled
        showButton.alpha = if (enabled) 1f else 0.5f
        checkButton.alpha = if (enabled) 1f else 0.5f
    }

    // ── Sobi sohbeti ────────────────────────────────────────────────────────

    private fun openSobi() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        val s = sheet ?: SobiChatSheet(
            fragment = this,
            tracker = tracker,
            contextProvider = { sobiContext() },
            onShow = { if (!finished) play(it) },
            isQuestionStep = { !finished },
        ).also { sheet = it }
        s.show()
    }

    /** O anki adım: soru adımı bağlamı; Sobi teşhisi ve sorunun kurallarını da bilsin. */
    private fun sobiContext(): SobiContext {
        val step = steps.getOrNull(stepIndex)
        val notes = listOfNotNull(
            "Çocuk soruyu yanlış cevapladı: ${questionText(numbers)} = $answer (doğrusu ${numbers.sum()}).",
            diagnosis?.text?.let { "Teşhis: $it" },
            "Şimdi soruyu abaküste adım adım birlikte yeniden çözüyorsunuz.",
        )
        return tracker.questionContext(
            stepId = null,
            stepText = if (finished) null else step?.text,
            questionText = questionText(numbers),
            target = if (finished) null else step?.target,
            current = controller?.getCurrentValue(),
            lessonTitle = lessonTitle,
            recentSteps = notes,
            taughtRules = rulesOf(steps),
        )
    }

    companion object {
        const val TAG = "MistakeReview"
        private const val ARG_NUMBERS = "numbers"
        private const val ARG_ANSWER = "answer"
        private const val ARG_LOG = "log"
        private const val ARG_LESSON = "lesson"

        /** "23 + 33 − 12": ilk sayı, sonra işaretleriyle. */
        fun questionText(numbers: List<Int>): String =
            numbers.mapIndexed { i, n -> if (i == 0) "$n" else if (n < 0) "- ${-n}" else "+ $n" }.joinToString(" ")

        private fun signed(n: Int) = if (n < 0) "−${-n}" else "+$n"

        /** Adımlar: ilk sayıyı yazmak, sonra her sayının her basamağı. Abaküse sığmayan dizide boş. */
        private fun buildSteps(numbers: List<Int>): List<Step> {
            if (numbers.size < 2 || numbers[0] !in 0..99999) return emptyList()
            val out = ArrayList<Step>()
            out += Step(0, numbers[0], null, appString(R.string.review_step_write, numbers[0]), 0)
            var running = numbers[0].toLong()
            for (k in 1 until numbers.size) {
                val n = numbers[k]
                if (n == 0) continue
                val hop = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
                val plan = StepExplainer.explain(running, hop, Math.abs(n).toLong()) ?: return emptyList()
                for (m in plan.moves) {
                    val column = MistakeAnalyzer.columnName(m.col).replaceFirstChar { it.uppercase() }
                    val rule = MistakeAnalyzer.ruleName(m.rule).replaceFirstChar { it.uppercase() }
                    val moves = m.steps.joinToString(", ") { MistakeAnalyzer.stepPhrase(it, m) }
                    val text = if (m.op == HomeworkOp.ADD) appString(R.string.review_step_add, column, m.x, rule, moves)
                    else appString(R.string.review_step_sub, column, m.x, rule, moves)
                    out += Step(m.valueBefore.toInt(), m.valueAfter.toInt(), m, text, k)
                }
                running = plan.result
            }
            return out
        }

        /** Sorunun kullandığı kurallar (Sobi başka kural anlatmasın). */
        private fun rulesOf(steps: List<Step>): List<String> =
            (listOf("add_direct") + steps.mapNotNull { it.move }.flatMap { m ->
                val prefix = if (m.op == HomeworkOp.ADD) "add_" else "sub_"
                StepExplainer.rulesUsed(m).map { prefix + it.key }
            }).distinct()

        /** Yalnızca toplama/çıkarma ve abaküse sığan diziler. */
        fun canReview(numbers: List<Int>?): Boolean = numbers != null && buildSteps(numbers).isNotEmpty()

        /** [log]: abaküslü derste bu sorudaki değer kaydı (teşhis için); abaküssüzde boş. */
        fun newInstance(numbers: List<Int>, answer: Int, log: List<Int>, lessonTitle: String?) =
            MistakeReviewDialog().apply {
                arguments = Bundle().apply {
                    putIntArray(ARG_NUMBERS, numbers.toIntArray())
                    putInt(ARG_ANSWER, answer)
                    putIntArray(ARG_LOG, log.toIntArray())
                    putString(ARG_LESSON, lessonTitle)
                }
            }
    }
}
