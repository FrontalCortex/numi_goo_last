package com.example.app

import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.example.app.abacus.AbacusBeadController
import com.example.app.abacus.ExamplePicker
import com.example.app.abacus.MistakeAnalyzer
import com.example.app.abacus.StepExplainer
import com.example.app.abacus.SubStepExplainer
import com.example.app.homework.DepthPress
import com.example.app.homework.HomeworkOp
import com.google.firebase.auth.FirebaseAuth

/**
 * SOBİ EKRANI — abaküs, adım adım çözüm ve Sobi sohbeti bir arada. İki yerden açılır:
 *
 * 1. "Sobi ile analiz et" (ders ekranında yanlış cevap; [newInstance]): yanlış cevaplanan soruyu
 *    Sobi anlatarak adım adım yeniden çözdürür (kullanıcı kararı 31.10.2026; docs/ANLAMADIM_YARDIM_PLANI.md
 *    "B aşaması"). Soru bir sayı dizisi: ilk sayı, sonra eklenen (+) ya da çıkarılan (−) sayılar.
 *    Sobi önce hatayı söyler ([MistakeAnalyzer] teşhis ettiyse), sonra soru adımlara bölünür: ilk sayıyı
 *    yazmak, ardından her sayının her basamağı ([StepExplainer] hamleleri). Flaş dizisinde (değer kaydı ve
 *    sayıların göründüğü anlar varken) yalnızca yanlış yapılan işlemler çözdürülür; açılışta kural başına
 *    hata sayısı.
 * 2. Öğreticide "Sobi'ye sor" ([forTutorial]; kullanıcı kararı 01.11.2026): alttan açılan sohbet paneli
 *    abaküsü ve adımı örtüyordu. Ekran öğreticinin o anki abaküsüyle ve adımın metniyle açılır; soru
 *    adımındaysa soru adım adım çözdürülür. "Bir örnek ver" dersin kuralıyla bir işlem seçer
 *    ([ExamplePicker]) ve onu da adım adım çözdürür. "Derse dön" öğreticiye tam bıraktığı yere döner
 *    (öğreticinin abaküsü ve adımı değişmez; çocuk soruyu orada kendisi bir daha yapar).
 *
 * SAYFALAR: her adım kendi sohbet sayfası; adımın talimatı, Kontrol Et sonuçları ve o adımda sorulanlar
 * orada kalır. Bilgi sayfaları (dersteki adım, sonuç, örneğin sonu) Kontrol Et yerine "Derse dön" gösterir.
 * ‹ › ile açılmış sayfalar arasında gezilir; bir adıma dönünce abaküs o adımın başındaki değere gelir.
 * Yanlışta Sobi o adımda ne yapıldığını söyler, abaküs adımın başına döner; "Bana göster" hamleyi
 * oynatır ([SobiAbacusPlayer]).
 *
 * Anlatım yapay zekâsız (kural motoru): kesin ve ücretsiz. Adımın parça balonları ("10 gelir" ne demek?)
 * da kural motoru ([SubStepExplainer]). Yapay zekâ yalnızca çocuk yazınca ya da yeşil balona dokununca
 * (ileride kredi ile). Sobi'nin bütün mesajlarında beğen / beğenme / bildir ([SobiMessageViews]).
 *
 * Ekran sade (kullanıcı kararı 01.11.2026): üstte yalnızca kapat ve adım gezinmesi; maskot yok, sohbet
 * tam genişlik. Kapanınca [RESULT_CLOSED] gider (sandık dersinin süresi sürer, öğreticinin Sobi düğmesi
 * yeniden görünür).
 */
class MistakeReviewDialog : DialogFragment() {

    /**
     * Kontrol Et ile çözülen adım. [label]: adım çubuğunda ne işlendiği ("+22", "78 + 9"); [intro]: adım
     * ilk açıldığında talimattan önce; [question]: Sobi'ye giden soru metni; [num]/[of]: grubundaki sırası.
     */
    private data class Step(
        val base: Int,
        val target: Int,
        val move: StepExplainer.ColumnMove?,
        val text: String,
        val label: String? = null,
        val intro: String? = null,
        val question: String? = null,
        val num: Int = 0,
        val of: Int = 0,
    )

    /**
     * Sayfa: adım ya da bilgi sayfası ([step] null). [value]: abaküsün bu sayfadaki başlangıç değeri.
     * [group]: ait olduğu adım grubu ([groups] sırası; tek bilgi sayfasında -1).
     */
    private class Page(val step: Step?, val title: String, val value: Int, val firstMessage: String, val group: Int = -1)

    /**
     * Bir adım grubu (soru ya da örnek). Örneklerin sonucu ([wrong] yanlış Kontrol Et, [done] sonuna
     * kadar çözüldü) Sobi'ye gider: zorluğu buna göre ayarlasın. [ai]: örneği Sobi açtı (sonunda
     * "Sıradaki soru").
     */
    private class Group(val question: String, val example: Boolean, val ai: Boolean, var wrong: Int = 0, var done: Boolean = false)

    private var tutorialMode = false

    // Analiz
    private var numbers: List<Int> = emptyList()
    private var answer = 0
    private var lessonTitle: String? = null
    private var steps: List<Step> = emptyList()
    private var diagnosis: MistakeAnalyzer.Diagnosis? = null
    /** Flaş dizisinde yanlış yapılan işlemler; doluysa yalnızca bunlar çözdürülür. */
    private var sequenceMistakes: List<MistakeAnalyzer.SequenceMistake> = emptyList()
    /** Analiz ekranında çocuğun ilk yanıldığı adım (teşhis yoksa hatanın yeri sayılır). */
    private var firstReviewMistake = -1

    // Öğretici
    private var tutorialStepText = ""
    private var tutorialStepId: String? = null
    private var tutorialQuestion: String? = null
    private var tutorialRecent: List<String> = emptyList()
    private var tutorialRules: List<String> = emptyList()
    private var tutorialNumber = 0
    private var tutorialStepIndex = 0
    private var tutorialValue = 0
    private var exampleRule: String? = null
    private var examples = 0
    /** Örneklerin zorluğu (1..[ExamplePicker.MAX_LEVEL]); çocuk "Daha zor bir örnek"e dokundukça artar. */
    private var exampleLevel = 1

    private val pageList = ArrayList<Page>()
    private val groups = ArrayList<Group>()
    /** Görüntülenen sayfa. */
    private var page = 0
    /** Açılmış en ileri sayfa; ileri ok buna kadar gider. */
    private var maxPage = 0
    private val pages = HashMap<Int, ArrayList<SobiStepTracker.Message>>()
    /** Sayfanın son Sobi cevabındaki "abaküste göster" işi ve öneri balonları. */
    private val pageShow = HashMap<Int, ServerSobi.Action>()
    private val pageSuggestions = HashMap<Int, List<String>>()

    private var controller: AbacusBeadController? = null
    private var player: SobiAbacusPlayer? = null
    private val stepLog = ArrayList<Int>()
    /** Abaküs Sobi'nin elinde (gösterim, geri alma, sayfa geçişi): dokunuş, düğmeler ve kayıt kapalı. */
    private var abacusBusy = false
    private var mediaPlayer: MediaPlayer? = null
    private var speaker: SobiSpeaker? = null
    private lateinit var speakToggle: View

    private val tracker = SobiStepTracker()
    /** Sobi'nin cevabı bekleniyor; cevap sorunun sorulduğu sayfaya düşer. */
    private var sendingPage = -1
    private val loggedIn: Boolean get() = FirebaseAuth.getInstance().currentUser != null

    private lateinit var messageViews: SobiMessageViews
    private lateinit var scroll: ScrollView
    private lateinit var messagesView: LinearLayout
    private lateinit var stepLabel: TextView
    private lateinit var prevButton: View
    private lateinit var nextButton: View
    private lateinit var checkButton: TextView
    private lateinit var input: EditText
    /** Bugünkü ücretsiz hak ve Sobi kredisi ([showBalance]). */
    private var balanceBar: SobiBalanceBar? = null
    private lateinit var abacusContainer: View
    private lateinit var buttonsRow: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val a = requireArguments()
        lessonTitle = a.getString(ARG_LESSON)
        tutorialMode = a.getBoolean(ARG_TUTORIAL_MODE)
        if (tutorialMode) {
            tutorialStepText = a.getString(ARG_STEP_TEXT).orEmpty()
            tutorialStepId = a.getString(ARG_STEP_ID)
            tutorialQuestion = a.getString(ARG_QUESTION)
            tutorialRecent = a.getStringArrayList(ARG_RECENT).orEmpty()
            tutorialRules = a.getStringArrayList(ARG_RULES).orEmpty()
            tutorialNumber = a.getInt(ARG_TUTORIAL)
            tutorialStepIndex = a.getInt(ARG_TUTORIAL_STEP)
            tutorialValue = a.getInt(ARG_VALUE)
            exampleRule = SobiCurriculum.exampleRule(tutorialNumber)
            return
        }
        numbers = a.getIntArray(ARG_NUMBERS)?.toList().orEmpty()
        answer = a.getInt(ARG_ANSWER)
        val log = a.getIntArray(ARG_LOG)?.toList().orEmpty()
        val marks = a.getIntArray(ARG_MARKS)?.toList().orEmpty()
        if (marks.isNotEmpty()) {
            sequenceMistakes = MistakeAnalyzer.findSequenceMistakes(numbers, log, marks, answer).orEmpty()
            buildMistakeSteps(sequenceMistakes)?.let { steps = it }
            if (steps.isEmpty()) sequenceMistakes = emptyList()
        }
        diagnosis = if (marks.isEmpty() && log.isNotEmpty() && numbers.size == 2) {
            MistakeAnalyzer.analyze(numbers[0], if (numbers[1] < 0) "-" else "+", Math.abs(numbers[1]), log, answer)
        } else null
        if (steps.isEmpty()) steps = buildSteps(numbers)
        // Analizde de soru çözülünce "Başka bir örnek" / "Daha zor bir örnek" (kullanıcı isteği 02.11.2026;
        // önceden yalnızca öğreticide vardı). Örnek sorunun en zor kuralından, zorluk sorunun büyüklüğünden
        // başlıyor: iki basamaklı soruyu çözen çocuğa 8 + 7 gelmesin.
        steps.mapNotNull { it.move }.maxByOrNull { it.rule.ordinal }?.let { m ->
            exampleRule = (if (m.op == HomeworkOp.ADD) "add_" else "sub_") + m.rule.key
        }
        val digits = numbers.map { Math.abs(it).toString().length }
        exampleLevel = when {
            digits.any { it >= 4 } -> 5
            digits.any { it >= 3 } -> 4
            digits.drop(1).any { it >= 2 } -> 3
            digits.any { it >= 2 } -> 2
            else -> 1
        }.coerceAtMost(ExamplePicker.MAX_LEVEL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_mistake_review, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scroll = view.findViewById(R.id.reviewSobiScroll)
        messagesView = view.findViewById(R.id.reviewMessages)
        stepLabel = view.findViewById(R.id.reviewStepLabel)
        prevButton = view.findViewById(R.id.reviewPrev)
        nextButton = view.findViewById(R.id.reviewNext)
        checkButton = view.findViewById(R.id.reviewCheck)
        input = view.findViewById(R.id.reviewInput)
        abacusContainer = view.findViewById(R.id.abacusContainer)
        buttonsRow = view.findViewById(R.id.reviewButtons)
        speakToggle = view.findViewById(R.id.reviewSpeak)
        speaker = SobiSpeaker(requireContext()) {
            // Ses motoru hazır: 🔊 düğmeleri ve aç/kapat görünsün; açılış mesajı okunsun.
            view.post {
                if (!isAdded) return@post
                speakToggle.visibility = View.VISIBLE
                updateSpeakToggle()
                renderPage()
                pages[page]?.lastOrNull { !it.fromChild }?.let { speaker?.speakIfAuto(it.text) }
            }
        }
        speakToggle.setOnClickListener {
            val s = speaker ?: return@setOnClickListener
            s.autoRead = !s.autoRead
            if (!s.autoRead) s.stop()
            updateSpeakToggle()
        }
        DepthPress.attach(speakToggle)
        messageViews = SobiMessageViews(
            fragment = this,
            sessionId = { tracker.sessionId },
            localScreen = if (tutorialMode) "ogretici" else "analiz",
            localQuestion = { if (tutorialMode) tutorialQuestion ?: tutorialStepText.take(100) else questionText(numbers) },
            onChanged = { renderPage() },
            onReported = { addMessage(page, SobiStepTracker.Message(fromChild = false, text = appString(R.string.sobi_report_done))) },
            onSpeak = { speaker?.speak(it) },
            canSpeak = { speaker?.available == true },
        )

        view.findViewById<View>(R.id.reviewClose).setOnClickListener { dismissAllowingStateLoss() }

        // Sobi'ye yazmak oturum ister (sunucu çağrısı).
        val inputRow = view.findViewById<View>(R.id.reviewInputRow)
        inputRow.visibility = if (loggedIn) View.VISIBLE else View.GONE
        balanceBar = SobiBalanceBar(
            balanceView = view.findViewById(R.id.reviewBalance),
            trialView = view.findViewById(R.id.reviewProTrial),
            activity = { activity as? MainActivity },
            onBuyCredits = { openSobiCredits() },
            onProTrial = { openProTrial() },
        )
        if (loggedIn) ServerSobi.balance({ b -> showBalance(b.freeLeft, b.credits) })
        val send = view.findViewById<View>(R.id.reviewSend)
        send.setOnClickListener { sendFromInput() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendFromInput(); true } else false
        }
        prevButton.setOnClickListener { if (page > 0) showPage(page - 1) }
        nextButton.setOnClickListener { if (page < maxPage) showPage(page + 1) }
        checkButton.setOnClickListener { onCheck() }
        DepthPress.attach(prevButton, nextButton, send, checkButton)
        setupKeyboard(view)

        // Abaküs: ödev oynatıcısıyla aynı kurulum (boncukları kimlikleriyle bulur).
        val c = AbacusBeadController(context = requireContext(), root = view, animationDurationMs = 300L)
        c.setup()
        c.onValueChanged = {
            if (!abacusBusy && !c.isResetInProgress()) {
                val v = c.getCurrentValue()
                if (stepLog.lastOrNull() != v && stepLog.size < 400) stepLog += v
                tracker.record(v)
            }
        }
        controller = c
        player = SobiAbacusPlayer(c)
        // İlk sayfa abaküs ölçüldükten SONRA açılır: flaş analizinde ilk adım abaküsü hemen 32'ye
        // taşıyor; ölçüden önce taşınan boncuklar sonradan "aşağıda" sayılıyor, gri kalıp dokununca
        // çerçeveden taşıyordu (01.11.2026, ekran yeniden kurulunca görüldü).
        view.findViewById<View>(R.id.abacusLinear).post {
            if (!isAdded) return@post
            if (c.computeMovementDistancesFromLayout(ratio = 1.0f, force = true)) c.syncStateFromUi()
            if (tutorialMode) startTutorial() else startReview()
        }
    }

    private fun startReview() {
        if (steps.isEmpty()) {
            val p = addInfoPage(appString(R.string.review_step_result), controller?.getCurrentValue() ?: 0, appString(R.string.review_intro_unknown))
            openPage(p)
            return
        }
        val intro = when {
            sequenceMistakes.isNotEmpty() -> appString(
                R.string.review_intro_sequence, sequenceMistakes.size, MistakeAnalyzer.ruleSummary(sequenceMistakes),
            )
            diagnosis != null -> appString(R.string.review_intro_found, diagnosis!!.text)
            else -> appString(R.string.review_intro_unknown)
        }
        // Soru üstte yazmıyor: iki sayılı soruda ilk mesajda (uzun flaş dizisinde hata başlıkları yeter).
        val question = if (numbers.size == 2) {
            appString(R.string.review_intro_question, questionText(numbers), answer, numbers.sum())
        } else null
        val tip = diagnosis?.move?.let { m ->
            appString(
                R.string.review_done_tip,
                MistakeAnalyzer.columnName(m.col),
                m.steps.joinToString(", ") { "\"${MistakeAnalyzer.stepPhrase(it, m)}\"" },
            )
        }.orEmpty()
        val done = if (sequenceMistakes.isNotEmpty()) appString(R.string.review_done_sequence, numbers.sum())
        else appString(R.string.review_done, questionText(numbers), numbers.sum()) + tip
        val first = addGroup(steps, listOfNotNull(question, intro).joinToString("\n\n"), appString(R.string.review_step_result), numbers.sum(), done)
        openPage(first)
    }

    /**
     * Öğreticiden: soru adımındaysa soru adım adım (başlangıç öğreticinin abaküsü), değilse dersteki
     * adımın sayfası (abaküs öğreticideki gibi, çocuk serbestçe oynatabilir).
     */
    private fun startTutorial() {
        val a = requireArguments()
        val base = a.getInt(ARG_BASE, -1)
        val target = a.getInt(ARG_TARGET, -1)
        val questionSteps = if (base >= 0 && target >= 0 && base != target) transitionSteps(base, target, tutorialQuestion) else null
        if (questionSteps.isNullOrEmpty()) {
            val lesson = addInfoPage(
                appString(R.string.sobi_screen_lesson_step), tutorialValue,
                appString(R.string.sobi_screen_lesson_intro, tutorialStepText),
            )
            // Derste bir işlem anlatılıyorsa (434 − 86) ekran o işlemle açılır; abaküs öğreticide hangi
            // adımdaysa o sayfa (kullanıcı kararı 01.11.2026). Çocuk işlemde takılı kalmaz: ‹ ile dersteki
            // adıma dönebilir, başka bir şey sorabilir, örnek isteyebilir.
            val nums = parseOperation(tutorialQuestion)
            val group = nums?.let { n -> buildSteps(n).map { it.copy(label = questionText(n), question = questionText(n)) } }.orEmpty()
            if (nums == null || group.isEmpty()) {
                openPage(lesson)
                return
            }
            val first = addGroup(group, null, appString(R.string.review_step_result), nums.sum(), appString(R.string.sobi_screen_lesson_op_done))
            val at = (first until first + group.size).firstOrNull { pageList[it].value == tutorialValue } ?: first
            pages.getOrPut(at) { ArrayList() } += sobiMessage(
                appString(R.string.sobi_screen_lesson_op_intro, questionText(nums), tutorialStepText) + "\n\n" + pageList[at].firstMessage,
            )
            openPage(at)
            return
        }
        val first = addGroup(
            questionSteps,
            appString(R.string.sobi_screen_question_intro, tutorialStepText),
            appString(R.string.review_step_result), target,
            appString(R.string.sobi_screen_question_done),
        )
        openPage(first)
    }

    private var stopped = false

    override fun onStop() {
        super.onStop()
        stopped = true
    }

    /**
     * Arka plandan dönüşte abaküsün durumu görünen boncuklardan yeniden okunur: dönüşte boncuklar
     * yerindeyken denetleyici onları "aşağıda" sayıp gri gösterebiliyordu (01.11.2026).
     */
    override fun onStart() {
        super.onStart()
        if (!stopped) return
        stopped = false
        val c = controller ?: return
        view?.post {
            if (!isAdded || abacusBusy || c.isBeadAnimationInProgress()) return@post
            c.syncStateFromUi()
            stepLog.clear()
            stepLog += c.getCurrentValue()
        }
    }

    private fun updateSpeakToggle() {
        speakToggle.alpha = if (speaker?.autoRead == true) 1f else 0.4f
    }

    override fun onDestroyView() {
        speaker?.shutdown()
        speaker = null
        player?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroyView()
    }

    /** Ders ekranı süreyi bu bildirimle sürdürür (sandık dersi süreli); öğretici Sobi düğmesini gösterir. */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isAdded && !closeResultHeldForShop) parentFragmentManager.setFragmentResult(RESULT_CLOSED, Bundle())
    }

    /** "Kredi al" ya da Pro denemesiyle kapandı: kapanış bildirimi o ekran kapanınca gidiyor ([openSobiCredits], [openProTrial]). */
    private var closeResultHeldForShop = false

    /**
     * Klavye açılınca abaküs ve alt düğmeler gizlenir: tam ekran pencerede yazma kutusu ile sohbete
     * yer kalsın (abaküs yüksekliği sabit, yoksa sohbet sıfıra inerdi). Pencere klavyeyi kendisi
     * yerleştirmiyor; klavyenin payı kök görünümün alt boşluğu olarak veriliyor.
     */
    private fun setupKeyboard(root: View) {
        dialog?.window?.let { w ->
            WindowCompat.setDecorFitsSystemWindows(w, false)
            @Suppress("DEPRECATION")
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val typing = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottom = if (typing) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else bars.bottom
            v.setPadding(0, bars.top, 0, bottom)
            val shown = if (typing) View.GONE else View.VISIBLE
            abacusContainer.visibility = shown
            buttonsRow.visibility = shown
            if (typing) scrollToEnd()
            WindowInsetsCompat.CONSUMED
        }
    }

    // ── Sayfalar ────────────────────────────────────────────────────────────

    /**
     * Adım grubunu sayfalara ekler: adımlar ve sonda bir bilgi sayfası ([endTitle], abaküs [endValue],
     * Sobi [endText] der). [intro] ilk adımın ilk mesajının başına. İlk adımın sayfa sırası döner.
     */
    private fun addGroup(
        group: List<Step>, intro: String?, endTitle: String, endValue: Int, endText: String,
        example: Boolean = false, ai: Boolean = false,
    ): Int {
        val first = pageList.size
        val g = groups.size
        groups += Group(group.firstOrNull()?.question ?: group.firstOrNull()?.label.orEmpty(), example, ai)
        group.forEachIndexed { i, s ->
            val step = s.copy(num = i + 1, of = group.size)
            val m = step.move
            val wasHere = m != null && diagnosis?.move?.let { it.col == m.col && it.valueAfter == m.valueAfter } == true
            val text = listOfNotNull(
                if (i == 0) intro else null,
                if (wasHere) appString(R.string.review_was_here) else null,
                step.intro, step.text,
            ).joinToString("\n\n")
            val title = if (step.label != null) appString(R.string.review_step_label_number, step.num, step.of, step.label)
            else appString(R.string.review_step_label, step.num, step.of)
            pageList += Page(step, title, step.base, text, g)
        }
        pageList += Page(null, endTitle, endValue, endText, g)
        return first
    }

    private fun addInfoPage(title: String, value: Int, text: String): Int {
        pageList += Page(null, title, value, text)
        return pageList.lastIndex
    }

    /** Sayfayı açar: ileri ok artık buraya kadar gider. */
    private fun openPage(i: Int) {
        maxPage = maxOf(maxPage, i)
        showPage(i)
    }

    /** Sayfayı gösterir (ilk gelişte Sobi'nin ilk mesajı yazılır); abaküs o sayfanın başındaki değere gelir. */
    private fun showPage(i: Int) {
        hideKeyboard()
        speaker?.stop()
        page = i
        val p = pageList[i]
        if (pages[i].isNullOrEmpty()) {
            pages.getOrPut(i) { ArrayList() } += sobiMessage(p.firstMessage)
            speaker?.speakIfAuto(p.firstMessage)
        }
        // Grubun sonuna gelindi: örnek çözüldü (Sobi'ye giden sonuç).
        if (p.step == null) groups.getOrNull(p.group)?.done = true
        stepLabel.text = p.title
        checkButton.text = appString(if (p.step == null) R.string.review_back else R.string.review_check)
        tracker.startStep(i)
        tracker.captureBase(p.value)
        renderPage()
        val moving = controller?.getCurrentValue() != p.value
        moveAbacus(p.value)
        // Talimatın sözünü ettiği sütun kısa süre parlar (çocuklar sütunları en çok burada karıştırıyor).
        p.step?.move?.let { m -> checkButton.postDelayed({ if (isAdded && page == i) highlightColumn(m.col) }, if (moving) HIGHLIGHT_AFTER_MOVE_MS else 0L) }
    }

    /** [col] sütununu (0 = birler) birkaç kez yanıp söndürür. */
    private fun highlightColumn(col: Int) {
        val root = view ?: return
        val id = resources.getIdentifier("rod${4 - col}", "id", requireContext().packageName)
        val rod = root.findViewById<android.widget.FrameLayout>(id) ?: return
        val glow = ContextCompat.getDrawable(requireContext(), R.drawable.bg_rod_highlight)?.mutate() ?: return
        glow.alpha = 0
        rod.foreground = glow
        android.animation.ValueAnimator.ofInt(0, 255, 60, 255, 0).apply {
            duration = HIGHLIGHT_MS
            addUpdateListener { glow.alpha = it.animatedValue as Int }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (rod.foreground === glow) rod.foreground = null
                }
            })
            start()
        }
    }

    private fun currentStep(): Step? = pageList.getOrNull(page)?.step

    private fun sobiMessage(text: String) = SobiStepTracker.Message(
        fromChild = false,
        text = text,
        // Kural motorunun mesajı: oy metniyle birlikte gider (oturum yoksa oy da yok).
        localId = if (loggedIn) SobiMessageViews.newLocalId() else null,
    )

    private fun addMessage(p: Int, m: SobiStepTracker.Message) {
        pages.getOrPut(p) { ArrayList() } += m
        if (p == page && view != null) {
            renderPage()
            if (!m.fromChild) speaker?.speakIfAuto(m.text)
        }
    }

    private fun renderPage() {
        val ctx = context ?: return
        messagesView.removeAllViews()
        for (m in pages[page].orEmpty()) messagesView.addView(messageViews.bubble(ctx, m))
        localChips(ctx)?.let { messagesView.addView(it) }
        if (sendingPage == page) messagesView.addView(thinkingBubble(ctx))
        else suggestionChips(ctx)?.let { messagesView.addView(it) }
        updateNav()
        scrollToEnd()
    }

    private fun scrollToEnd() {
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun updateNav() {
        val canPrev = page > 0 && !abacusBusy
        val canNext = page < maxPage && !abacusBusy
        prevButton.isEnabled = canPrev
        nextButton.isEnabled = canNext
        prevButton.alpha = if (canPrev) 1f else 0.35f
        nextButton.alpha = if (canNext) 1f else 0.35f
    }

    // ── Adım ────────────────────────────────────────────────────────────────

    private fun onCheck() {
        val step = currentStep()
        if (step == null) { dismissAllowingStateLoss(); return }
        if (abacusBusy) return
        val c = controller ?: return
        val value = c.getCurrentValue()
        tracker.onAnswerChecked()
        val correct = value == step.target
        step.move?.let { SobiRuleStats.recordMove(requireContext(), it, correct) }
        if (!correct) groups.getOrNull(pageList[page].group)?.let { it.wrong++ }
        if (correct) {
            playSound(R.raw.correct_answer_sound, 0.3f)
            addMessage(page, SobiStepTracker.Message(fromChild = false, text = appString(R.string.review_correct)))
            val from = page
            holdAbacus(true)
            checkButton.postDelayed({
                if (!isAdded || page != from) return@postDelayed
                holdAbacus(false)
                if (from + 1 < pageList.size) openPage(from + 1)
            }, NEXT_STEP_DELAY_MS)
            return
        }
        val detail = step.move?.let { MistakeAnalyzer.diagnoseStep(it, stepLog, value).text }
            ?: MistakeAnalyzer.diagnoseWrite(step.target, value)
        val found = if (!tutorialMode && diagnosis == null && sequenceMistakes.isEmpty() && firstReviewMistake == -1) {
            firstReviewMistake = page
            appString(R.string.review_found_now) + " "
        } else ""
        addMessage(page, sobiMessage(found + appString(R.string.review_wrong, detail)))
        playSound(R.raw.incorrect_answer_sound)
        rewindToStepStart(step)
    }

    /**
     * Yanlışta abaküs adımın başındaki değere döner: çocuk yanlış hamlesini kendisi geri almak zorunda
     * kalmasın, adımı temiz bir abaküsle yeniden denesin (kullanıcı kararı 31.10.2026). Kısa bir bekleme
     * var ki çocuk önce kendi yaptığını görsün; geri alma hareketi değer kaydına girmez.
     */
    private fun rewindToStepStart(step: Step) {
        val from = page
        holdAbacus(true)
        checkButton.postDelayed({
            if (!isAdded) return@postDelayed
            holdAbacus(false)
            if (page == from) moveAbacus(step.base)
        }, REWIND_DELAY_MS)
    }

    /** Abaküsü [value]'ya getirir; hareket bitene dek dokunuş ve düğmeler kapalı, kayıt durur. */
    private fun moveAbacus(value: Int) {
        val c = controller ?: return
        val p = player ?: return
        if (c.getCurrentValue() == value) {
            stepLog.clear()
            stepLog += value
            return
        }
        if (p.isPlaying) p.cancel()
        holdAbacus(true)
        p.moveTo(value)
        checkButton.postDelayed({
            if (!isAdded) return@postDelayed
            holdAbacus(false)
            stepLog.clear()
            stepLog += c.getCurrentValue()
        }, REWIND_SETTLE_MS)
    }

    private fun holdAbacus(hold: Boolean) {
        abacusBusy = hold
        tracker.paused = hold
        controller?.setEnabled(!hold)
        checkButton.isEnabled = !hold
        checkButton.alpha = if (hold) 0.5f else 1f
        updateNav()
    }

    /** "Abaküste göster": bu adımın hamlesini adımın başından oynatır; sonunda abaküs yine başa döner. */
    private fun showCurrentStep() {
        val step = currentStep() ?: return
        val action = if (step.move == null) {
            ServerSobi.Action(ServerSobi.ACTION_SHOW, 0, "add", step.target, step.target)
        } else {
            val amount = Math.abs(step.target - step.base)
            ServerSobi.Action(ServerSobi.ACTION_SHOW, step.base, if (step.target >= step.base) "add" else "sub", amount, step.target)
        }
        play(action)
    }

    /** Gösterim; Sobi başka bir örnek gösterdiyse sonunda abaküs bu sayfanın başına döner. */
    private fun play(action: ServerSobi.Action) {
        if (abacusBusy) return
        val c = controller ?: return
        val p = player ?: return
        val from = page
        holdAbacus(true)
        val started = p.play(action) {
            if (!isAdded) return@play
            holdAbacus(false)
            val base = pageList.getOrNull(from)?.value
            if (page == from && base != null && c.getCurrentValue() != base) moveAbacus(base)
            else { stepLog.clear(); stepLog += c.getCurrentValue() }
        }
        if (!started) holdAbacus(false)
    }

    /**
     * "Bir örnek ver" (öğretici): dersin kuralıyla bir işlem ([ExamplePicker]) sayfa sayfa çözdürülür;
     * sonunda "Başka örnek" ya da "Derse dön".
     */
    private fun startExample() {
        val ruleKey = exampleRule ?: return
        // Analizde öğrenilen kurallar listesi yok: örnek yalnızca sorunun kuralını gerçekten kullansın.
        val allowed = if (tutorialMode) tutorialRules else null
        val seed = examples * 7 + if (tutorialMode) tutorialNumber * 3 else numbers.sum()
        val (a, b) = ExamplePicker.pick(ruleKey, seed, exampleLevel, allowed) ?: return
        examples++
        val nums = listOf(a, b)
        val q = questionText(nums)
        val group = exampleSteps(nums, q)
        if (group.isEmpty()) return
        addMessage(page, SobiStepTracker.Message(fromChild = true, text = appString(R.string.sobi_suggest_example)))
        val rule = group.mapNotNull { it.move }.maxByOrNull { it.rule.ordinal }?.rule
        val intro = appString(R.string.sobi_screen_example_level, exampleLevel, ExamplePicker.MAX_LEVEL) + " " +
            if (rule == null || rule == com.example.app.homework.AbacusRule.DIRECT) appString(R.string.sobi_screen_example_intro_direct, q)
            else appString(R.string.sobi_screen_example_intro, q, MistakeAnalyzer.ruleName(rule))
        val first = addGroup(
            group,
            intro,
            appString(R.string.sobi_screen_example_title),
            nums.sum(),
            appString(R.string.sobi_screen_example_done, q, nums.sum()),
            example = true,
        )
        openPage(first)
    }

    /**
     * Örneğin adımları: abaküs ilk sayı yazılmış olarak gelir (çocuk doğrudan kuralı çalışır; Sobi de
     * "abaküsünde 139 var" diyebilir). İlk sayı 0 ise tek adım: sayıyı yazmak.
     */
    private fun exampleSteps(nums: List<Int>, q: String): List<Step> =
        transitionSteps(nums[0], nums[0] + nums[1], q).orEmpty().map { it.copy(label = q) }

    /**
     * Sobi'nin sohbetten açtığı örnek (alistirma_ver: "zor bir örnek ver", "3 basamaklı örnek"). Sunucu
     * işlemi kural motoruyla doğruladı ve öğrenilmemiş kural isteyeni reddetti. Sobi'nin cevabı ve ilk
     * adımın talimatı örneğin ilk sayfasına yazılır, ekran oraya geçer.
     */
    private fun openAiExample(a: ServerSobi.Action, reply: SobiStepTracker.Message?) {
        val n = if (a.op == "sub") -a.number else a.number
        val nums = listOf(a.base, n)
        val q = if (a.base == 0) "${a.number}" else questionText(nums)
        val group = exampleSteps(nums, q)
        if (group.isEmpty()) {
            reply?.let { addMessage(page, it) }
            return
        }
        examples++
        val first = addGroup(
            group, null, appString(R.string.sobi_screen_example_title), a.base + n,
            appString(R.string.sobi_screen_example_done_ai, q, a.base + n), example = true, ai = true,
        )
        val list = pages.getOrPut(first) { ArrayList() }
        reply?.let { list += it }
        list += sobiMessage(pageList[first].firstMessage)
        openPage(first)
    }

    // ── Sobi'ye sor ─────────────────────────────────────────────────────────

    /**
     * Bakiye satırı ([SobiBalanceBar]). "Kredi al": ekran kapanır, mağaza Sobi kredisi paketlerinde
     * açılır (satın almayı Play hesabı sahibi, yani ebeveyn onaylar).
     */
    private fun showBalance(freeLeft: Int, credits: Int) {
        if (view == null) return
        balanceBar?.show(freeLeft, credits)
    }

    /**
     * Pro deneme tanıtımı: kendi penceresi olduğu için dersin/öğreticinin önünde açılıyor. Kapanış
     * bildirimi ([RESULT_CLOSED]) mağaza yolundaki gibi tanıtım kapanınca gidiyor; ders süresi
     * kullanıcı Play penceresindeyken akmasın.
     */
    private fun openProTrial() {
        val activity = activity as? MainActivity ?: return
        val resultFm = parentFragmentManager
        closeResultHeldForShop = true
        dismissAllowingStateLoss()
        val promo = SobiProTrialFragment.newInstance(AnalyticsLogger.PROMO_TRIGGER_SOBI_LIMIT)
        promo.onClosed = { resultFm.setFragmentResult(RESULT_CLOSED, Bundle()) }
        promo.show(activity.supportFragmentManager, SobiProTrialFragment.TAG)
    }

    /**
     * Mağaza, dersin/öğreticinin üstünde açılıyor (MainActivity.openShopFragment). Kapanış bildirimi
     * ([RESULT_CLOSED]) sandık dersinin süresini yeniden başlatıyor; hemen gitseydi kullanıcı mağazada
     * kredi alırken arkada dersin süresi akardı. Bu yüzden bildirim mağaza kapanınca gönderiliyor.
     */
    private fun openSobiCredits() {
        val activity = activity as? MainActivity ?: return
        val resultFm = parentFragmentManager
        closeResultHeldForShop = true
        dismissAllowingStateLoss()
        val sendClosed = { resultFm.setFragmentResult(RESULT_CLOSED, Bundle()) }
        if (activity.openShopFragment(focusSobiCredits = true)) activity.doAfterShopClosed(sendClosed) else sendClosed()
    }

    private fun sendFromInput() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty() || sendingPage != -1) return
        input.setText("")
        hideKeyboard()
        send(text)
    }

    private fun send(text: String, suggestion: String? = null) {
        if (sendingPage != -1 || !loggedIn) return
        val context = sobiContext()
        val forPage = page
        sendingPage = forPage
        pageSuggestions.remove(forPage)
        pageShow.remove(forPage)
        addMessage(forPage, SobiStepTracker.Message(fromChild = true, text = text))

        ServerSobi.send(
            sessionId = tracker.sessionId,
            message = text,
            context = context,
            onResult = { reply ->
                sendingPage = -1
                tracker.sessionId = reply.sessionId
                if (reply.remainingToday != null && reply.credits != null) showBalance(reply.remainingToday, reply.credits)
                val shown = when {
                    reply.turnLimitReached -> appString(R.string.sobi_turn_limit)
                    else -> reply.text?.takeIf { it.isNotBlank() }
                }
                reply.actions.lastOrNull { it.kind == ServerSobi.ACTION_SHOW }?.let { pageShow[forPage] = it }
                pageSuggestions[forPage] = if (reply.turnLimitReached) emptyList() else reply.suggestions
                val index = if (reply.turnLimitReached) null else reply.messageIndex
                // Sobi bir örnek açtıysa ("zor bir örnek ver"): cevabı örneğin ilk sayfasına, ekran oraya.
                val example = reply.actions.lastOrNull { it.kind == ServerSobi.ACTION_PRACTICE }
                if (example != null && view != null && sendingPage == -1) {
                    renderPage()
                    openAiExample(example, shown?.let { SobiStepTracker.Message(fromChild = false, text = it, serverIndex = index) })
                    return@send
                }
                if (view == null) {
                    shown?.let { pages.getOrPut(forPage) { ArrayList() } += SobiStepTracker.Message(fromChild = false, text = it, serverIndex = index) }
                    return@send
                }
                if (shown != null) {
                    addMessage(forPage, SobiStepTracker.Message(fromChild = false, text = shown, serverIndex = index))
                } else renderPage()
                reply.actions.lastOrNull { it.kind == ServerSobi.ACTION_POINT }?.column?.let { col ->
                    if (page == forPage) highlightColumn(col)
                }
                // Sobi abaküste gösterdiyse ("sen yap") hemen oynasın; "Abaküste izle"ye ayrıca basmak
                // gerekmesin (kullanıcı isteği 02.11.2026). Çocuk bu arada başka sayfaya geçtiyse oynamaz.
                pageShow[forPage]?.let { if (page == forPage) play(it) }
            },
            onFailure = { e ->
                sendingPage = -1
                val msg = when {
                    ServerSobi.isDailyLimit(e) -> {
                        showBalance(0, 0)
                        appString(
                            if (SobiProTrialFragment.canOfferTrial(activity as? MainActivity)) R.string.sobi_error_daily_limit_trial
                            else R.string.sobi_error_daily_limit,
                        )
                    }
                    ServerSobi.isRestricted(e) -> appString(R.string.sobi_error_restricted)
                    else -> appString(R.string.sobi_error_network)
                }
                if (view == null) return@send
                addMessage(forPage, SobiStepTracker.Message(fromChild = false, text = msg))
            },
            suggestion = suggestion,
        )
    }

    /**
     * Kural motorunun cevapladığı balonlar (açık renk; yapay zekâ balonlarından ayrı ki çocuk farkı
     * görsün): adımın parçaları (“10 gelir” ne demek?, Neden bu kural? — [SubStepExplainer]) ve
     * öğreticide "Bir örnek ver". Ücretsiz (ileride kredi harcamaz), kesin, oturum gerekmez.
     */
    private fun localChips(ctx: Context): View? {
        val items = ArrayList<Pair<String, () -> Unit>>()
        // Adımı abaküste görmek için Sobi'ye yazıp cevabı beklemek uzun yoldu (kullanıcı isteği
        // 02.11.2026): adım sayfasında ilk seçenek doğrudan gösterim, ücretsiz.
        if (currentStep() != null) items += appString(R.string.sobi_suggest_show_abacus) to { showCurrentStep() }
        currentStep()?.move?.takeIf { SubStepExplainer.hasParts(it) }?.let { m ->
            val parts = m.steps.map { st -> SubStepExplainer.question(st, m) to { SubStepExplainer.explain(st, m) } }
                .distinctBy { it.first } + (appString(R.string.review_why_rule) to { SubStepExplainer.whyRule(m) })
            for ((label, answer) in parts) {
                items += label to {
                    val p = page
                    addMessage(p, SobiStepTracker.Message(fromChild = true, text = label))
                    addMessage(p, sobiMessage(answer()))
                }
            }
        }
        // Öğreticide her sayfada; analizde soru (ya da örnek) bitince, sonuç sayfasında: çocuk soruyu
        // çözdü, aynı kuralla bir tane daha.
        if (exampleRule != null && (tutorialMode || currentStep() == null)) {
            val first = tutorialMode && examples == 0
            val label = appString(if (first) R.string.sobi_suggest_example else R.string.sobi_screen_another_example)
            items += label to { startExample() }
            // Zorluğu çocuk seçer (kullanıcı kararı 01.11.2026): örnek yine dersin kuralından, yalnızca büyür.
            if (!first && exampleLevel < ExamplePicker.MAX_LEVEL) {
                items += appString(R.string.sobi_screen_harder_example) to {
                    exampleLevel++
                    startExample()
                }
            }
        }
        if (items.isEmpty()) return null
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(ctx, 6), 0, 0)
        }
        for ((label, action) in items) {
            val chip = TextView(ctx).apply {
                text = label
                setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_hw_chip)
                setPadding(dp(ctx, 12), dp(ctx, 7), dp(ctx, 12), dp(ctx, 10))
                setOnClickListener { if (!abacusBusy) action() }
            }
            DepthPress.attach(chip)
            row.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = dp(ctx, 8) })
        }
        return HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    /**
     * Yapay zekâya giden balonlar (yeşil): Sobi'nin son cevabının önerileri; henüz soru sorulmadıysa
     * başlangıç balonları. "Bana göster" ve "Bir daha göster" sunucuya gitmez (yerelde oynar).
     */
    private fun suggestionChips(ctx: Context): View? {
        if (!loggedIn) return null
        val show = pageShow[page]
        val keys = ArrayList<String>()
        // Gösterim cevapla birlikte kendiliğinden oynadı; balon yeniden oynatmak için. Sunucu zaten
        // "Bir daha göster" önerdiyse ikinci kez eklenmez.
        if (show != null && pageSuggestions[page]?.contains(SobiSuggestions.KEY_SHOW_AGAIN) != true) keys += KEY_WATCH
        val step = currentStep()
        // "Neden böyle?" parça balonlarındaki "Neden bu kural?" ile aynı işi görürdü; onlar varken yalnızca "Bunu anlamadım".
        val hasParts = step?.move?.let { SubStepExplainer.hasParts(it) } == true
        val aiExampleEnd = step == null && groups.getOrNull(pageList.getOrNull(page)?.group ?: -1)?.ai == true
        keys += pageSuggestions[page]?.let { if (aiExampleEnd) listOf(KEY_NEXT) + it else it } ?: when {
            aiExampleEnd -> listOf(KEY_NEXT)
            step == null && tutorialMode && page == 0 -> listOf(SobiSuggestions.KEY_DIDNT_GET, SobiSuggestions.KEY_WHY)
            step == null -> emptyList()
            hasParts -> listOf(SobiSuggestions.KEY_DIDNT_GET)
            else -> INITIAL_SUGGESTIONS
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(ctx, 6), 0, dp(ctx, 4))
        }
        for (key in keys.distinct()) {
            // "Bir örnek ver" yapay zekâya gitmiyor: öğreticide yerel balon, analizde yok.
            if (key == SobiSuggestions.KEY_EXAMPLE) continue
            val label = when (key) {
                KEY_WATCH -> appString(R.string.sobi_suggest_show_again)
                KEY_NEXT -> appString(R.string.sobi_next_question)
                else -> SobiSuggestions.labelOf(key) ?: continue
            }
            val chip = TextView(ctx).apply {
                text = label
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_sobi_pill)
                setPadding(dp(ctx, 12), dp(ctx, 7), dp(ctx, 12), dp(ctx, 7))
                setOnClickListener { onSuggestion(key) }
            }
            row.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = dp(ctx, 8) })
        }
        if (row.childCount == 0) return null
        return HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    private fun onSuggestion(key: String) {
        when (key) {
            KEY_WATCH, SobiSuggestions.KEY_SHOW_AGAIN -> pageShow[page]?.let { play(it) } ?: showCurrentStep()
            SobiSuggestions.KEY_SHOW -> showCurrentStep()
            // Sobi'nin açtığı örneğin sonunda: Sobi sonuca bakıp sıradakini seçer (zorlaştırır ya da kolaylaştırır).
            KEY_NEXT -> send(appString(R.string.sobi_next_question_message))
            else -> send(SobiSuggestions.labelOf(key) ?: return, suggestion = key)
        }
    }

    private fun thinkingBubble(ctx: Context): View = TextView(ctx).apply {
        text = appString(R.string.sobi_thinking)
        setTextColor(ContextCompat.getColor(ctx, R.color.dark_text_secondary))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setPadding(dp(ctx, 4), dp(ctx, 6), 0, dp(ctx, 6))
    }

    /** O anki sayfanın bağlamı: adım sayfasında soru adımı; öğreticinin bilgi sayfasında dersin anlatım adımı. */
    private fun sobiContext(): SobiContext {
        val step = currentStep()
        if (tutorialMode) {
            val ctx = if (step == null) {
                tracker.explanationContext(
                    stepId = tutorialStepId, stepText = tutorialStepText, questionText = tutorialQuestion,
                    lessonTitle = lessonTitle, recentSteps = tutorialRecent, taughtRules = tutorialRules,
                )
            } else {
                tracker.questionContext(
                    stepId = null, stepText = step.text, questionText = step.question,
                    target = step.target, current = controller?.getCurrentValue(), lessonTitle = lessonTitle,
                    recentSteps = tutorialRecent + "Dersteki adım: $tutorialStepText" +
                        "Şimdi Sobi ekranında bu işlemi abaküste adım adım birlikte çözüyorsunuz.",
                    taughtRules = tutorialRules,
                )
            }
            // Sunucu bu adımın hazır açıklamasını bulsun (öğretici + adım sırası, metinle denetleniyor).
            // practice: Sobi bu ekranda örnek açabilir (alistirma_ver).
            return ctx.copy(
                tutorialNumber = tutorialNumber, stepIndex = tutorialStepIndex, practice = true,
                practiceResults = practiceResults(), ruleStats = SobiRuleStats.snapshot(requireContext()),
            )
        }
        val notes = listOfNotNull(
            "Çocuk soruyu yanlış cevapladı: ${questionText(numbers)} = $answer (doğrusu ${numbers.sum()}).",
            diagnosis?.text?.let { "Teşhis: $it" },
            sequenceMistakes.takeIf { it.isNotEmpty() }?.joinToString("; ", prefix = "Dizide yanıldığı işlemler: ") {
                "${it.base} ${if (it.number < 0) "- ${-it.number}" else "+ ${it.number}"} (${it.value} yaptı, doğrusu ${it.base + it.number})"
            },
            if (sequenceMistakes.isNotEmpty()) "Şimdi yalnızca bu işlemleri abaküste adım adım birlikte çözüyorsunuz."
            else "Şimdi soruyu abaküste adım adım birlikte yeniden çözüyorsunuz.",
        )
        return tracker.questionContext(
            stepId = null,
            stepText = step?.text,
            questionText = questionText(numbers),
            target = step?.target,
            current = controller?.getCurrentValue(),
            lessonTitle = lessonTitle,
            recentSteps = notes,
            taughtRules = rulesOf(steps),
        ).copy(practice = true, practiceResults = practiceResults(), ruleStats = SobiRuleStats.snapshot(requireContext()))
    }

    private fun practiceResults(): List<SobiContext.PracticeResult> =
        groups.filter { it.example }.map { SobiContext.PracticeResult(it.question, it.wrong, it.done) }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    /** Ders ekranındaki sesler; ayarlarda ses kapalıysa çalmaz. */
    private fun playSound(resId: Int, volume: Float = 1f) {
        val c = context ?: return
        if (!c.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getBoolean("sound_enabled", true)) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(c, resId)?.apply {
            setVolume(volume, volume)
            start()
        }
    }

    private fun hideKeyboard() {
        if (!::input.isInitialized) return
        val imm = input.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    private fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "MistakeReview"
        /** Ekran kapanınca açan ekrana giden bildirim (setFragmentResult). */
        const val RESULT_CLOSED = "MistakeReview.closed"
        private const val REWIND_DELAY_MS = 700L
        private const val REWIND_SETTLE_MS = 500L
        private const val NEXT_STEP_DELAY_MS = 900L
        /** Yerel balon: Sobi'nin son cevabındaki gösterimi oynatır. */
        private const val KEY_WATCH = "abakuste_izle"
        /** Yerel balon: Sobi'den sıradaki soruyu ister (Sobi'nin açtığı örneğin sonunda). */
        private const val KEY_NEXT = "siradaki_soru"
        private const val HIGHLIGHT_MS = 1600L
        private const val HIGHLIGHT_AFTER_MOVE_MS = 650L
        /** Adımda henüz soru sorulmadıysa. "Bana göster" zaten alt düğme. */
        private val INITIAL_SUGGESTIONS = listOf(SobiSuggestions.KEY_WHY, SobiSuggestions.KEY_DIDNT_GET)
        private const val ARG_NUMBERS = "numbers"
        private const val ARG_ANSWER = "answer"
        private const val ARG_LOG = "log"
        private const val ARG_MARKS = "marks"
        private const val ARG_LESSON = "lesson"
        private const val ARG_TUTORIAL_MODE = "tutorialMode"
        private const val ARG_STEP_TEXT = "stepText"
        private const val ARG_STEP_ID = "stepId"
        private const val ARG_QUESTION = "question"
        private const val ARG_RECENT = "recent"
        private const val ARG_RULES = "rules"
        private const val ARG_TUTORIAL = "tutorial"
        private const val ARG_TUTORIAL_STEP = "tutorialStep"
        private const val ARG_VALUE = "value"
        private const val ARG_BASE = "base"
        private const val ARG_TARGET = "target"

        /** "434 - 86", "23 + 33 − 12" → [434, -86]; toplama/çıkarma değilse (çarpma, metin) ya da abaküse sığmıyorsa null. */
        private fun parseOperation(text: String?): List<Int>? {
            val t = text?.replace('−', '-')?.replace(Regex("""\s"""), "") ?: return null
            if (!Regex("""\d+([+-]\d+)+""").matches(t)) return null
            val out = Regex("""([+-]?)(\d+)""").findAll(t).toList().map { m ->
                val n = m.groupValues[2].toIntOrNull() ?: return null
                if (m.groupValues[1] == "-") -n else n
            }
            return out.takeIf { canReview(it) }
        }

        /** "23 + 33 − 12": ilk sayı, sonra işaretleriyle. */
        fun questionText(numbers: List<Int>): String =
            numbers.mapIndexed { i, n -> if (i == 0) "$n" else if (n < 0) "- ${-n}" else "+ $n" }.joinToString(" ")

        private fun signed(n: Int) = if (n < 0) "−${-n}" else "+$n"

        /** Adımlar: ilk sayıyı yazmak, sonra her sayının her basamağı. Abaküse sığmayan dizide boş. */
        private fun buildSteps(numbers: List<Int>): List<Step> {
            if (numbers.size < 2 || numbers[0] !in 0..99999) return emptyList()
            val out = ArrayList<Step>()
            out += Step(0, numbers[0], null, appString(R.string.review_step_write, numbers[0]))
            var running = numbers[0].toLong()
            for (k in 1 until numbers.size) {
                val n = numbers[k]
                if (n == 0) continue
                val hop = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
                val plan = StepExplainer.explain(running, hop, Math.abs(n).toLong()) ?: return emptyList()
                val label = if (numbers.size > 2) signed(n) else null
                for (m in plan.moves) out += Step(m.valueBefore.toInt(), m.valueAfter.toInt(), m, moveText(m), label)
                running = plan.result
            }
            return out
        }

        /**
         * Öğreticideki soru: abaküs [base]'den [target]'a. Sıfırdan yazmaksa tek "yaz" adımı, değilse
         * kural motorunun hamleleri. Abaküse sığmıyorsa null.
         */
        private fun transitionSteps(base: Int, target: Int, question: String?): List<Step>? {
            if (base == 0) return listOf(Step(0, target, null, appString(R.string.review_step_write, target), question = question))
            val e = StepExplainer.explainTransition(base.toLong(), target.toLong()) ?: return null
            return e.moves.map { m -> Step(m.valueBefore.toInt(), m.valueAfter.toInt(), m, moveText(m), question = question) }
        }

        private fun moveText(m: StepExplainer.ColumnMove): String {
            val column = MistakeAnalyzer.columnName(m.col).replaceFirstChar { it.uppercase() }
            val rule = MistakeAnalyzer.ruleName(m.rule).replaceFirstChar { it.uppercase() }
            val moves = m.steps.joinToString(", ") { MistakeAnalyzer.stepPhrase(it, m) }
            return if (m.op == HomeworkOp.ADD) appString(R.string.review_step_add, column, m.x, rule, moves)
            else appString(R.string.review_step_sub, column, m.x, rule, moves)
        }

        /**
         * Flaş dizisinde yalnızca yanlış yapılan işlemler: her biri abaküste çocuğun o anki değerinden
         * (78) başlar, basamak basamak çözülür. İşlemin ilk adımında hatanın başlığı ve teşhisi.
         * Hata yoksa ya da işlem abaküse sığmıyorsa null (bütün dizi çözdürülür).
         */
        private fun buildMistakeSteps(mistakes: List<MistakeAnalyzer.SequenceMistake>): List<Step>? {
            if (mistakes.isEmpty()) return null
            val out = ArrayList<Step>()
            mistakes.forEachIndexed { j, mk ->
                val target = mk.base + mk.number
                val head = when {
                    mk.index == 0 -> appString(R.string.review_mistake_write, j + 1, target, mk.value)
                    mk.number < 0 -> appString(R.string.review_mistake_sub, j + 1, mk.base, -mk.number, mk.value, target)
                    else -> appString(R.string.review_mistake_add, j + 1, mk.base, mk.number, mk.value, target)
                }
                val intro = listOfNotNull(head, mk.diagnosis?.text?.takeIf { mk.index > 0 }).joinToString(" ")
                if (mk.index == 0) {
                    out += Step(0, target, null, appString(R.string.review_step_write, target), appString(R.string.review_label_first), intro)
                    return@forEachIndexed
                }
                val hop = if (mk.number < 0) HomeworkOp.SUB else HomeworkOp.ADD
                val plan = StepExplainer.explain(mk.base.toLong(), hop, Math.abs(mk.number).toLong()) ?: return null
                val label = "${mk.base} ${if (mk.number < 0) "− ${-mk.number}" else "+ ${mk.number}"}"
                plan.moves.forEachIndexed { i, m ->
                    out += Step(m.valueBefore.toInt(), m.valueAfter.toInt(), m, moveText(m), label, if (i == 0) intro else null)
                }
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

        /**
         * "Sobi ile analiz et". [log]: abaküslü derste bu sorudaki değer kaydı (teşhis için); klavyeyle
         * cevaplanan derste boş. [marks]: flaş dizisinde her sayı göründüğünde kayda eklenen değerin
         * sırası; verilirse yalnızca yanlış yapılan işlemler çözdürülür ([MistakeAnalyzer.findSequenceMistakes]).
         */
        fun newInstance(numbers: List<Int>, answer: Int, log: List<Int>, lessonTitle: String?, marks: List<Int> = emptyList()) =
            MistakeReviewDialog().apply {
                arguments = Bundle().apply {
                    putIntArray(ARG_NUMBERS, numbers.toIntArray())
                    putInt(ARG_ANSWER, answer)
                    putIntArray(ARG_LOG, log.toIntArray())
                    putIntArray(ARG_MARKS, marks.toIntArray())
                    putString(ARG_LESSON, lessonTitle)
                }
            }

        /**
         * Öğreticide "Sobi'ye sor". [value]: öğreticinin abaküsünde görünen değer. Soru adımındaysa
         * [base] (adımın başı) ve [target] (beklenen cevap), değilse null.
         */
        fun forTutorial(
            stepText: String, stepId: String?, question: String?, lessonTitle: String?,
            recentSteps: List<String>, taughtRules: List<String>, tutorialNumber: Int, stepIndex: Int,
            value: Int, base: Int?, target: Int?,
        ) = MistakeReviewDialog().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_TUTORIAL_MODE, true)
                putString(ARG_STEP_TEXT, stepText)
                putString(ARG_STEP_ID, stepId)
                putString(ARG_QUESTION, question)
                putString(ARG_LESSON, lessonTitle)
                putStringArrayList(ARG_RECENT, ArrayList(recentSteps))
                putStringArrayList(ARG_RULES, ArrayList(taughtRules))
                putInt(ARG_TUTORIAL, tutorialNumber)
                putInt(ARG_TUTORIAL_STEP, stepIndex)
                putInt(ARG_VALUE, value)
                putInt(ARG_BASE, base ?: -1)
                putInt(ARG_TARGET, target ?: -1)
            }
        }
    }
}
