package com.example.app.homework

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.app.MainActivity
import com.example.app.R
import com.example.app.abacus.AbacusBeadController
import com.example.app.abacus.AbacusSettingsPanel
import com.example.app.abacus.LessonRulesPanel
import com.example.app.abacus.LessonSideButtons
import com.example.app.appString
import com.example.app.databinding.FragmentHomeworkPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Öğrencinin ödev ekranı: sıradaki adımın (en fazla 10 soru) soruları.
 *
 * Toplama/çıkarmada sayılar seçilen aralıkla tahtada TEK TEK gösteriliyor; abaküslüde çocuk
 * sayılar akarken boncukları oynatıyor ve sonunda Kontrol'e basıyor (cevap abaküsün değeri),
 * abaküssüzde sayılar bitince cevabı yazıyor. Çarpmada işlem tahtada sabit duruyor.
 *
 * Sorularım → Ödevlerim'den tam ekran katmanda açılıyor ([MainActivity.showAbacusOverlayFragment]),
 * kapanışı [MainActivity.finishHomeworkOverlay]: Görevler'in dönüş akışı (reklam, seri sorusu) yok.
 *
 * Bu ekran BlindingLessonFragment'ı kullanmıyor (bilerek): o ekran harita dersi, kupa ve günlük
 * soru akışlarıyla iç içe; ödev için bir mod daha eklemek çalışan akışlara dokunmak demekti.
 * Abaküs, abaküs pratiği ekranındakiyle aynı ([AbacusBeadController]).
 *
 * Adım bitince ilerleme yazılıyor ([Homeworks.submitBlock]); yazılamazsa sonuç ekranı "Tekrar
 * kaydet" gösteriyor, çocuğun çözdüğü adım kaybolmuyor. Adımın ortasında çıkılırsa adım sayılmıyor.
 * Can (enerji) harcanmıyor, reklam yok: ödev öğretmenin verdiği iş. Sobi testinden çıkışta ise derslerdeki
 * geçiş reklamı kapısı soruluyor (bkz. [close]).
 *
 * SOBİ TESTİ KİPİ ([newSobiTest]; kullanıcı kararı 01.11.2026): Sobi'nin günlük testi aynı ekranda.
 * Ödev belgesi yok: sorular hazır gelir ([com.example.app.abacus.SobiTestPlanner]), sonuç kural
 * başarısına yazılır ([com.example.app.SobiRuleStats]; soru başına, ödevle aynı işlem sayımı).
 * Yanlışta "Sobi ile analiz et"; bitince Sobi kural kural özetler, sandık derslerdekiyle aynı sunucu
 * sandığı ([com.example.app.NewChestFragment]). Ödev akışı değişmedi; kip yalnızca bu argümanla açılır.
 * Sobi testinde abaküssüz (zihinden) sorular da var (07.11.2026): o soruda abaküs gizlenir, cevap
 * ödevin abaküssüz kipindeki gibi klavyeyle; abaküsle ilgili her karar [questionWithAbacus]'tan.
 *
 * SAĞ DÜĞMELER (kupa yolundaki BlindingLessonFragment ile aynı; görünüm ve davranış ortak yardımcılarda:
 * [LessonRulesPanel], [AbacusSettingsPanel], [LessonSideButtons]). Ödevde ve Sobi testinde aynı çalışıyor:
 * - Sıfırla: abaküs (abaküssüzde cevap kutusu) sıfırlanır; Başla'ya basıldıysa ve cevap verilmediyse
 *   sayılar baştan akar (derste de öyle; kullanıcı kararı 02.11.2026).
 * - Atla: gösterilen sayıyı hemen geçer (yalnızca çocuğu hızlandırır, puana etkisi yok); çarpmada gizli.
 * - Kural kitabı / sihirli değnek: kitapta seçilen tablo değnekle abaküsün üstünde açılıp kapanır.
 * - Abaküs ayarı (yalnızca abaküslüde): boyut paneli. Soru yarıdayken açılırsa soru Başla'ya döner,
 *   çünkü panel abaküsü sıfırlıyor.
 */
class HomeworkPlayerFragment : Fragment() {

    companion object {
        private const val ARG_HOMEWORK_ID = "homework_id"

        /** Sayının ekranda kaldığı payın dışında kalan boşluk: ardışık aynı sayılar ayırt edilsin. */
        private const val GAP_FRACTION = 0.15
        private const val MIN_GAP_MS = 40L
        private const val MAX_GAP_MS = 250L
        private const val RESULT_PANEL_ANIM_MS = 200L
        /** Oynatıcı kayarak kapanırken sandık hemen ardından açılsın (kapsayıcı gizlenmeden). */
        private const val SOBI_CHEST_DELAY_MS = 80L
        /** Sıfırla'da soru yeniden akmadan önce: abaküsün sıfırlanma animasyonu (300 ms) bitsin, kayda girmesin. */
        private const val RESTART_FLASH_DELAY_MS = 350L

        fun newInstance(homeworkId: String) = HomeworkPlayerFragment().apply {
            arguments = Bundle().apply { putString(ARG_HOMEWORK_ID, homeworkId) }
        }

        private const val ARG_SOBI_OPS = "sobi_ops"
        private const val ARG_SOBI_SIZES = "sobi_sizes"
        private const val ARG_SOBI_NUMBERS = "sobi_numbers"
        private const val ARG_SOBI_INTERVALS = "sobi_intervals"
        private const val ARG_SOBI_WEAK = "sobi_weak"
        private const val ARG_SOBI_CURRENT = "sobi_current"
        private const val ARG_SOBI_PLAYED = "sobi_played"
        private const val ARG_SOBI_MENTAL = "sobi_mental"

        /** Sobi'nin testi ([com.example.app.abacus.SobiTestPlanner.Plan]): sorular, araları, kuralların seviyeleri. */
        fun newSobiTest(plan: com.example.app.abacus.SobiTestPlanner.Plan) = HomeworkPlayerFragment().apply {
            // Abaküssüz kural "m_" önekiyle: sonda seviyesi abaküslüden ayrı hesaplanıp yazılıyor.
            val weak = (plan.weak + plan.mentalWeak).map { it.key }
            arguments = Bundle().apply {
                putBooleanArray(ARG_SOBI_MENTAL, plan.questions.map { it.mental }.toBooleanArray())
                putIntArray(ARG_SOBI_OPS, plan.questions.map { if (it.question.op == HomeworkOp.SUB) 1 else 0 }.toIntArray())
                putIntArray(ARG_SOBI_SIZES, plan.questions.map { it.question.numbers.size }.toIntArray())
                putLongArray(ARG_SOBI_NUMBERS, plan.questions.flatMap { it.question.numbers }.toLongArray())
                putLongArray(ARG_SOBI_INTERVALS, plan.questions.map { it.intervalMs }.toLongArray())
                putStringArrayList(ARG_SOBI_WEAK, ArrayList(weak))
                putIntArray(ARG_SOBI_CURRENT, weak.map { plan.current[it] ?: 1 }.toIntArray())
                putIntArray(ARG_SOBI_PLAYED, weak.map { plan.played[it] ?: 1 }.toIntArray())
            }
        }
    }

    /** Sobi'nin testi mi (ödev değil). */
    private var sobiMode = false
    private var sobiWeak: List<String> = emptyList()
    /** Sorunun sayılarının arası (seviyeye göre); ödevde hep [HomeworkConfig.intervalMs]. */
    private var sobiIntervals: LongArray = LongArray(0)
    /** Sobi testinde soru başına abaküssüz mü (kullanıcı kararı 07.11.2026: tek testte karışık). */
    private var sobiMental: BooleanArray = BooleanArray(0)

    /**
     * Şu anki soru abaküslü mü. Ödevde bütün ödev için tek ayar ([HomeworkConfig.withAbacus]); Sobi
     * testinde soru soru değişiyor. Abaküsle ilgili her karar buradan, ödev akışı aynı sonucu alıyor.
     */
    private fun questionWithAbacus(): Boolean =
        if (sobiMode) sobiMental.getOrNull(questionIndex) != true else homework?.config?.withAbacus == true


    private var _binding: FragmentHomeworkPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeworkId: String
    private var homework: Homeworks.Homework? = null
    private var progress = Homeworks.Progress.EMPTY
    private var questions: List<HomeworkQuestion> = emptyList()
    private var questionIndex = 0
    private var blockCorrect = 0
    private var blockIndex = 0
    private var flashing = false
    private var answered = false
    private var abacusController: AbacusBeadController? = null
    private var abacusMetricsReady = false
    private val handler = Handler(Looper.getMainLooper())
    /** Sonuç paneli gezinme çubuğunun arkasına kadar uzasın (derslerdeki gibi). */
    private var navBarPanelExtension: com.example.app.NavBarPanelExtension? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    /** Panel kayarken Devam'a basılıp soru atlanmasın. */
    private var resultPanelAnimating = false

    // Sağ düğmeler (bkz. sınıf açıklaması)
    private var rulesPanel: LessonRulesPanel? = null
    private var abacusSettings: AbacusSettingsPanel? = null

    // Sayıların akışı adım adım: atla o anki sayıyı kısaltabilsin, sıfırla akışı durdurabilsin.
    private var flashLabels: List<String> = emptyList()
    private var flashIndex = 0
    private var flashShowMs = 0L
    private var flashGapMs = 0L
    /** Sayı söndü, sıradaki bekleniyor: atla bu arada bir şey yapmaz (derste de öyle). */
    private var flashBlank = false
    private val flashHideRunnable = Runnable { hideFlashNumber() }
    private val flashNextRunnable = Runnable { showFlashNumber() }
    private val restartFlashRunnable = Runnable {
        if (_binding != null && !answered && questions.isNotEmpty()) startFlash(questions[questionIndex])
    }

    // Kural kural başarı (RuleAnalysis): sorudaki abaküs değerleri, sayıların göründüğü anlar, adımın sayımı
    private val abacusLog = ArrayList<RuleAnalysis.Sample>()
    private val shownAtMs = ArrayList<Long>()
    private val blockTally = LinkedHashMap<String, IntArray>()
    /** Soru akarken ve cevaplanana kadar true: abaküsün her değişimi kaydediliyor. */
    private var recordingAbacus = false
    /** Bu soruda Başla'ya basıldı (sıfırla sayıları baştan akıtsın diye). */
    private var flashStarted = false

    private fun recordAbacus() {
        if (!recordingAbacus) return
        val value = abacusController?.getCurrentValue()?.toLong() ?: return
        if (abacusLog.lastOrNull()?.value != value) abacusLog += RuleAnalysis.Sample(android.os.SystemClock.uptimeMillis(), value)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeworkPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeworkId = requireArguments().getString(ARG_HOMEWORK_ID).orEmpty()
        sobiMode = requireArguments().containsKey(ARG_SOBI_OPS)
        // Derslerdeki gibi: dönüşte geçiş reklamı sorulsun (kapı MainActivity.checkAndShowInterstitialAdIfAllowed).
        if (sobiMode && savedInstanceState == null) com.example.app.GlobalValues.shouldShowAdOnReturn = true
        binding.quitButton.setOnClickListener { requestQuit() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Önce açık kural kitabı ya da ayar paneli kapansın.
                if (LessonRulesPanel.closeRulesBookIfOpen(childFragmentManager)) return
                val settings = abacusSettings
                if (settings != null && settings.isOpen) return settings.toggle()
                requestQuit()
            }
        })
        binding.kontrolButton.setOnClickListener { onKontrol() }
        binding.answerInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { onKontrol(); true } else false
        }
        binding.resultButton.setOnClickListener { if (!resultPanelAnimating) hideResultPanel { nextQuestion() } }
        // Başla / Kontrol ve sonuç panelinin düğmesi derslerdeki gibi içe göçüyor.
        DepthPress.attach(binding.kontrolButton, binding.resultButton)
        navBarPanelExtension = com.example.app.NavBarPanelExtension(binding.root, binding.resultPanel).also { it.attach() }
        setupSideButtons()
        setPlayVisible(false)

        if (sobiMode) {
            startSobiTest()
            return
        }
        Homeworks.loadHomework(homeworkId) { hw ->
            if (_binding == null) return@loadHomework
            if (hw == null) return@loadHomework showUnavailable()
            homework = hw
            setupAbacus(hw.config.withAbacus)
            Homeworks.loadMyProgress(homeworkId) { p ->
                if (_binding == null) return@loadMyProgress
                progress = p ?: Homeworks.Progress.EMPTY
                startBlock()
            }
        }
    }

    // ── Sobi testi ──────────────────────────────────────────────────────────

    /** Ödev yerine Sobi'nin testi: tek adım, hazır sorular, abaküslü. */
    private fun startSobiTest() {
        val a = requireArguments()
        val ops = a.getIntArray(ARG_SOBI_OPS) ?: intArrayOf()
        val sizes = a.getIntArray(ARG_SOBI_SIZES) ?: intArrayOf()
        val numbers = a.getLongArray(ARG_SOBI_NUMBERS) ?: longArrayOf()
        sobiIntervals = a.getLongArray(ARG_SOBI_INTERVALS) ?: longArrayOf()
        sobiWeak = a.getStringArrayList(ARG_SOBI_WEAK).orEmpty()
        sobiMental = a.getBooleanArray(ARG_SOBI_MENTAL) ?: BooleanArray(0)
        var at = 0
        val list = ops.indices.mapNotNull { i ->
            val n = sizes.getOrNull(i) ?: return@mapNotNull null
            if (at + n > numbers.size) return@mapNotNull null
            val q = HomeworkQuestion(if (ops[i] == 1) HomeworkOp.SUB else HomeworkOp.ADD, numbers.slice(at until at + n))
            at += n
            q
        }
        // Yalnızca abaküs ve sayıların arası kullanılıyor (ara soru başına: sobiIntervals). Abaküs hep
        // kuruluyor; abaküssüz sorularda yalnızca gizleniyor (showQuestion), kurulumu tekrarlamamak için.
        val config = HomeworkConfig(op = HomeworkOp.ADD, withAbacus = true, digits = 2, numberCount = 2, intervalMs = 2000L, questionCount = list.size)
        homework = Homeworks.Homework(
            id = "", classId = "", className = "", teacherName = "", config = config, blockCount = 1,
            wholeClass = false, assignedUids = emptyList(), createdAtMs = 0L,
        )
        // Sorular hazır geliyor: ödevdeki yükleme çarkı (düzende görünür başlıyor, ödevi okuyunca
        // startBlock gizliyor) burada hiç gizlenmiyordu ve test boyunca ortada dönüyordu.
        binding.loading.visibility = View.GONE
        setupAbacus(true)
        if (list.isEmpty()) return showUnavailable()
        questions = list
        questionIndex = 0
        blockCorrect = 0
        blockTally.clear()
        setPlayVisible(true)
        showQuestion()
    }

    /**
     * Abaküsün değer kaydı ve her sayının göründüğü anki değer (Sobi ekranının flaş analizi:
     * [com.example.app.abacus.MistakeAnalyzer.findSequenceMistakes]; `log[marks[k]]` k. sayı göründüğünde).
     */
    private fun reviewLog(): Pair<List<Int>, List<Int>> {
        val values = ArrayList<Int>()
        val marks = ArrayList<Int>()
        var si = 0
        for (t in shownAtMs) {
            while (si < abacusLog.size && abacusLog[si].tMs <= t) values += abacusLog[si++].value.toInt()
            values += values.lastOrNull() ?: 0
            marks += values.lastIndex
        }
        while (si < abacusLog.size) values += abacusLog[si++].value.toInt()
        return values to marks
    }

    /** Test bitti: Sobi kural kural özetler; sandık derslerdekiyle aynı. Test bugün yapılmış sayılır. */
    private fun finishSobiTest() {
        val b = _binding ?: return
        val ctx = requireContext()
        com.example.app.SobiDailyTest.markDone(ctx)
        com.example.app.MissionsProgressStore.recordStepIncrementProgress(ctx)
        if (blockCorrect == questions.size) com.example.app.MissionsProgressStore.recordPerfectStepIncrementProgress(ctx)
        // Sandık isteği şimdiden: açılıştaki gecikme gizlensin (görev sandıklarıyla aynı).
        com.example.app.ServerRewards.prefetchChest("COMMON")
        setPlayVisible(false)
        b.resultPanel.visibility = View.GONE
        b.abacusContainer.visibility = View.GONE
        b.summaryPanel.visibility = View.VISIBLE
        b.summaryTitle.text = appString(R.string.sobi_test_done_title)
        b.summaryScore.text = appString(R.string.hw_player_block_score, blockCorrect, questions.size)
        b.summaryScore.visibility = View.VISIBLE
        // Seviye: o kuralın bu testteki işlemlerinden ([com.example.app.abacus.SobiTestLevels.next]).
        val a = requireArguments()
        val current = a.getIntArray(ARG_SOBI_CURRENT) ?: intArrayOf()
        val played = a.getIntArray(ARG_SOBI_PLAYED) ?: intArrayOf()
        val newLevels = LinkedHashMap<String, Int>()
        val lines = sobiWeak.mapIndexedNotNull { i, key ->
            val cw = blockTally[key] ?: return@mapIndexedNotNull null
            val total = cw[0] + cw[1]
            if (total == 0) return@mapIndexedNotNull null
            val before = current.getOrNull(i) ?: 1
            val after = com.example.app.abacus.SobiTestLevels.next(before, played.getOrNull(i) ?: before, cw[0] * 100 / total)
            newLevels[key] = after
            val levelText = when {
                after > before -> appString(R.string.sobi_test_level_up, before, after)
                after < before -> appString(R.string.sobi_test_level_down, after)
                else -> appString(R.string.sobi_test_level_same, after)
            }
            appString(R.string.sobi_test_rule_line, com.example.app.SobiDailyTest.ruleLabel(key), total, cw[0]) + "\n" + levelText
        }
        com.example.app.SobiDailyTest.saveLevels(ctx, newLevels)
        val pct = if (questions.isEmpty()) 0 else blockCorrect * 100 / questions.size
        val cheer = appString(if (pct >= 80) R.string.sobi_test_cheer_good else R.string.sobi_test_cheer_try)
        b.summaryMessage.text = (lines + cheer).joinToString("\n")
        b.summaryPrimary.text = appString(R.string.sobi_test_open_chest)
        b.summaryPrimary.setOnClickListener {
            b.summaryPrimary.isEnabled = false
            openSobiChest()
        }
        b.summarySecondary.visibility = View.GONE
    }

    /** Ders sandığıyla aynı sunucu sandığı; ders sonu zincirine (harita ilerlemesi) dokunmadan. */
    private fun openSobiChest() {
        val main = activity as? MainActivity ?: return close()
        // Reklamı sandığın kapanışı soruyor (bayrak kalkık kalıyor): sandığın üstüne açılmasın.
        close(showInterstitialAd = false)
        main.window.decorView.postDelayed({
            if (main.isFinishing || main.isDestroyed) return@postDelayed
            // Görev sandığıyla aynı açılış (MissionsSection): kapanışı NewChestFragment kendisi yapıyor
            // (finishTasksOverlayAnimated), çalışan bir yol.
            val container = main.findViewById<View>(R.id.abacusFragmentContainer) ?: return@postDelayed
            container.visibility = View.VISIBLE
            main.supportFragmentManager.beginTransaction()
                .add(R.id.abacusFragmentContainer, com.example.app.NewChestFragment.newInstance(source = com.example.app.AnalyticsLogger.CHEST_SOURCE_SOBI_TEST))
                .commit()
        }, SOBI_CHEST_DELAY_MS)
    }

    // ── Kurulum ─────────────────────────────────────────────────────────────

    private fun setupAbacus(withAbacus: Boolean) {
        val b = _binding ?: return
        b.abacusContainer.visibility = if (withAbacus) View.VISIBLE else View.GONE
        if (!withAbacus) return
        // Ayarlar denetleyiciden önce: kaydedilmiş boncuk boşlukları ona aktarılıyor (derste de bu sırada).
        abacusSettings = AbacusSettingsPanel(
            panel = b.abacusSettingsPanel,
            abacusContainer = b.abacusContainer,
            modeButton = b.abacusModeButton,
            sideButtons = listOf(b.resetButton, b.rulesPanelButton, b.skipStepButton, b.abacusModeButton),
            controller = { abacusController },
            onOpen = ::onAbacusSettingsOpen,
            onClose = { abacusController?.setEnabled(true) },
        ).also { it.attach() }
        val controller = AbacusBeadController(context = requireContext(), root = b.root, animationDurationMs = 300L)
        controller.setup()
        controller.onValueChanged = { recordAbacus() }
        abacusSettings?.applyBeadOffsets(controller)
        abacusController = controller
        b.abacusLinear.post {
            if (_binding == null || abacusMetricsReady) return@post
            if (controller.computeMovementDistancesFromLayout(ratio = 1.0f, force = true)) {
                abacusMetricsReady = true
                controller.syncStateFromUi()
            }
        }
    }

    private fun setPlayVisible(visible: Boolean) {
        val b = _binding ?: return
        val v = if (visible) View.VISIBLE else View.INVISIBLE
        b.flashCard.visibility = v
        b.flashText.visibility = v
        b.kontrolButton.visibility = v
        b.stepText.visibility = v
        val side = if (visible) View.VISIBLE else View.GONE
        b.rulesBookButton.visibility = side
        b.resetButton.visibility = side
        b.rulesPanelButton.visibility = side
        b.skipStepButton.visibility = side
        b.abacusModeButton.visibility = if (visible && questionWithAbacus()) View.VISIBLE else View.GONE
        if (!visible) {
            rulesPanel?.closeIfOpen()
            abacusSettings?.closeIfOpen()
        }
    }

    /** Sıradaki adımın sorularını arka planda üretir (elde gerektiren ayarlarda birkaç yüz ms sürebilir). */
    private fun startBlock() {
        val hw = homework ?: return
        if (progress.completedBlocks >= hw.blockCount) return showAllDone()
        blockIndex = progress.completedBlocks
        binding.loading.visibility = View.VISIBLE
        binding.summaryPanel.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            val generated = withContext(Dispatchers.Default) {
                runCatching { HomeworkEngine.generateBlock(hw.config, blockIndex) }.getOrNull()
            }
            val b = _binding ?: return@launch
            b.loading.visibility = View.GONE
            if (generated.isNullOrEmpty()) return@launch showUnavailable()
            questions = generated
            questionIndex = 0
            blockCorrect = 0
            blockTally.clear()
            setPlayVisible(true)
            showQuestion()
        }
    }

    // ── Soru akışı ──────────────────────────────────────────────────────────

    private fun showQuestion() {
        val b = _binding ?: return
        val hw = homework ?: return
        val q = questions[questionIndex]
        answered = false
        flashStarted = false
        b.resultPanel.visibility = View.GONE
        b.stepText.text = when {
            // Zihinden soruda abaküs birden kayboluyor: çocuk bunun bir hata olmadığını etiketten anlasın.
            sobiMode && !questionWithAbacus() -> appString(R.string.sobi_test_step_mental, questionIndex + 1, questions.size)
            sobiMode -> appString(R.string.sobi_test_step, questionIndex + 1, questions.size)
            else -> appString(R.string.hw_player_step, blockIndex + 1, hw.blockCount, questionIndex + 1, questions.size)
        }
        if (sobiMode) {
            val withAbacus = questionWithAbacus()
            if (!withAbacus) abacusSettings?.closeIfOpen()
            b.abacusContainer.visibility = if (withAbacus) View.VISIBLE else View.GONE
            b.abacusModeButton.visibility = if (withAbacus) View.VISIBLE else View.GONE
        }
        abacusController?.reset(animate = true)
        b.answerInput.text = null
        b.answerInput.visibility = View.GONE
        // Çarpmada akış yok, atlanacak sayı da (derste de çarpmada gizli).
        b.skipStepButton.visibility = if (q.op == HomeworkOp.MUL) View.GONE else View.VISIBLE
        // Tablo açıksa sorunun işlemine göre yenilensin (5'li tablonun başlığı toplama/çıkarma).
        if (rulesPanel?.isVisible == true) rulesPanel?.show()

        if (q.op == HomeworkOp.MUL) {
            setFlash(HomeworkText.questionLine(q))
            onFlashFinished()
            return
        }
        // Başla'ya basınca akıyor: çocuk hazır olmadan ilk sayı geçip gitmesin.
        setFlash("")
        b.kontrolButton.text = appString(R.string.hw_player_start)
        b.kontrolButton.isEnabled = true
        b.kontrolButton.setOnClickListener { startFlash(q) }
    }

    private fun startFlash(q: HomeworkQuestion) {
        val b = _binding ?: return
        val cfg = homework?.config ?: return
        flashing = true
        b.kontrolButton.text = appString(R.string.hw_player_check)
        b.kontrolButton.isEnabled = false
        b.kontrolButton.setOnClickListener { onKontrol() }
        val interval = if (sobiMode) sobiIntervals.getOrNull(questionIndex) ?: cfg.intervalMs else cfg.intervalMs
        val gap = (interval * GAP_FRACTION).toLong().coerceIn(MIN_GAP_MS, MAX_GAP_MS)
        val show = (interval - gap).coerceAtLeast(MIN_GAP_MS)
        // Abaküslüde değerler kaydediliyor: hangi kuralda hata yapıldığı buradan çıkıyor
        abacusLog.clear()
        shownAtMs.clear()
        flashStarted = true
        recordingAbacus = questionWithAbacus()
        recordAbacus() // sorunun başındaki değer
        // Çıkarmada başlangıçtan sonraki sayılar eksiyle gösteriliyor.
        flashLabels = q.numbers.mapIndexed { i, n -> if (q.op == HomeworkOp.SUB && i > 0) "−$n" else n.toString() }
        flashIndex = 0
        flashShowMs = show
        flashGapMs = gap
        showFlashNumber()
    }

    /**
     * Sıradaki sayı [flashShowMs] boyunca görünür. Son sayı ekranda kalır ve Kontrol hemen açılır
     * (BlindingLessonFragment.onSequenceRevealStep ile aynı; eskiden son sayı sönüp "?" geliyordu).
     */
    private fun showFlashNumber() {
        if (_binding == null || flashIndex >= flashLabels.size) return
        shownAtMs += android.os.SystemClock.uptimeMillis()
        setFlash(flashLabels[flashIndex])
        flashBlank = false
        if (flashIndex == flashLabels.lastIndex) {
            flashing = false
            onFlashFinished()
            return
        }
        handler.postDelayed(flashHideRunnable, flashShowMs)
    }

    /** Sayı söner, [flashGapMs] sonra sıradaki: ardışık aynı sayılar ayırt edilsin. */
    private fun hideFlashNumber() {
        if (_binding == null) return
        setFlash("")
        flashBlank = true
        flashIndex++
        handler.postDelayed(flashNextRunnable, flashGapMs)
    }

    private fun stopFlash() {
        handler.removeCallbacks(flashHideRunnable)
        handler.removeCallbacks(flashNextRunnable)
        handler.removeCallbacks(restartFlashRunnable)
        flashing = false
        flashBlank = false
    }

    private fun onFlashFinished() {
        val b = _binding ?: return
        val withAbacus = questionWithAbacus()
        b.kontrolButton.text = appString(R.string.hw_player_check)
        b.kontrolButton.isEnabled = true
        b.kontrolButton.setOnClickListener { onKontrol() }
        if (!withAbacus) {
            b.answerInput.visibility = View.VISIBLE
            b.answerInput.requestFocus()
            requireContext().getSystemService(InputMethodManager::class.java)
                ?.showSoftInput(b.answerInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /** Uzun sayılar (8 basamak, çarpma) karta sığsın diye yazı küçülüyor. */
    private fun setFlash(text: String) {
        val b = _binding ?: return
        val base = resources.getDimension(R.dimen.hw_flash_text_size)
        val scale = when {
            text.length <= 5 -> 1f
            text.length <= 8 -> 0.75f
            text.length <= 11 -> 0.6f
            else -> 0.5f
        }
        b.flashText.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * scale)
        b.flashText.text = text
    }

    private fun onKontrol() {
        val b = _binding ?: return
        if (flashing || answered || questions.isEmpty()) return
        val q = questions[questionIndex]
        val given: Long? = if (questionWithAbacus()) {
            abacusController?.getCurrentValue()?.toLong()
        } else {
            b.answerInput.text?.toString()?.trim()?.toLongOrNull()
        }
        if (given == null) return
        answered = true
        recordingAbacus = false
        hideKeyboard()
        // Klavye kapanınca kutu Kontrol'ün üstüne iniyor; sonuç paneliyle üst üste görünmesin.
        b.answerInput.visibility = View.GONE
        val correct = given == q.answer
        if (correct) blockCorrect++
        countRules(q, given, correct)
        setFlash(HomeworkText.questionLine(q).let { if (q.op == HomeworkOp.MUL) "$it = ${q.answer}" else q.answer.toString() })
        b.resultTitle.text = if (correct) appString(R.string.hw_player_correct)
        else appString(R.string.hw_player_wrong, q.answer.toString())
        val color = if (correct) 0xFF8CC63F.toInt() else 0xFFFF4B4B.toInt()
        b.resultTitle.setTextColor(color)
        // Yanlışta işlemin tamamı: çocuk hangi sayıda kaydığını görebilsin.
        b.resultMessage.visibility = if (correct) View.GONE else View.VISIBLE
        b.resultMessage.text = com.example.app.RtlSupport.ltr(HomeworkText.questionLine(q) + " = " + q.answer)
        // Göçen düğme (dersteki doğru/yanlış panelleriyle aynı arka planlar).
        b.resultButton.setBackgroundResource(if (correct) R.drawable.bg_depth_tutorial_continue else R.drawable.bg_depth_tutorial_okay)
        bindSobiReview(q, given, correct)
        b.kontrolButton.isEnabled = false
        // Ses ve ses düzeyi AbacusFragment.showResultPanel ile aynı.
        if (correct) playSound(R.raw.correct_answer_sound, 0.3f) else playSound(R.raw.incorrect_answer_sound, 1f)
        showResultPanel()
    }

    /** Sobi testinde yanlışta "Sobi ile analiz et": soru Sobi ekranında, hatalı işlemden adım adım. */
    private fun bindSobiReview(q: HomeworkQuestion, given: Long, correct: Boolean) {
        val button = _binding?.resultSobiAnalyze ?: return
        if (!sobiMode || correct || q.op == HomeworkOp.MUL) {
            button.visibility = View.GONE
            return
        }
        val numbers = q.numbers.mapIndexed { i, n -> if (q.op == HomeworkOp.SUB && i > 0) -n.toInt() else n.toInt() }
        // Abaküssüzde boncuk kaydı yok: Sobi soruyu baştan adım adım anlatır (körleme dersindeki gibi).
        val (log, marks) = if (questionWithAbacus()) reviewLog() else emptyList<Int>() to emptyList()
        button.visibility = View.VISIBLE
        DepthPress.attach(button)
        button.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(com.example.app.MistakeReviewDialog.TAG) != null) return@setOnClickListener
            com.example.app.MistakeReviewDialog.newInstance(
                numbers, given.toInt(), log, appString(R.string.sobi_test_title),
                marks = if (marks.size == numbers.size) marks else emptyList(),
            ).show(childFragmentManager, com.example.app.MistakeReviewDialog.TAG)
        }
    }

    /** Sorunun kural sayımını adımın sayımına ekler (bkz. [RuleAnalysis]). Hata olursa soru sayılmaz. */
    private fun countRules(q: HomeworkQuestion, given: Long, correct: Boolean) {
        val cfg = homework?.config ?: return
        // Sobi testinin abaküssüz sorusu: soru başına, "m_" anahtarlarına (SobiRuleStats.recordMental ile
        // aynı ölçü; körleme dersleriyle aynı toplama gitsin). Ödevin abaküssüz kipi eskisi gibi sayılmıyor.
        val tally = if (sobiMode && !questionWithAbacus()) {
            runCatching { com.example.app.SobiRuleStats.mentalTally(com.example.app.abacus.SobiTestPlanner.rulesOf(q), correct) }
                .getOrDefault(emptyMap())
        } else {
            val attempts = runCatching {
                when {
                    q.op == HomeworkOp.MUL -> RuleAnalysis.multiplication(cfg, correct)
                    questionWithAbacus() -> RuleAnalysis.abacus(q, abacusLog.toList(), shownAtMs.toList(), given)
                    else -> emptyList() // abaküssüz toplama/çıkarma sayılmıyor
                }
            }.getOrDefault(emptyList())
            RuleAnalysis.tally(attempts)
        }
        for ((key, cw) in tally) {
            val sum = blockTally.getOrPut(key) { IntArray(2) }
            sum[0] += cw[0]
            sum[1] += cw[1]
        }
        // Sobi testi ödev belgesine değil kural başarısına yazılır (dersler ve Sobi ile aynı toplam).
        if (sobiMode) context?.let { com.example.app.SobiRuleStats.add(it, tally, question = correct) }
    }

    /** Panel alttan kayarak geliyor: AbacusFragment / BlindingLessonFragment ile aynı süre ve eğri. */
    private fun showResultPanel() {
        val panel = _binding?.resultPanel ?: return
        resultPanelAnimating = true
        panel.animate().cancel()
        panel.alpha = 0f
        panel.visibility = View.VISIBLE
        // Yükseklik bilinmeden kaydırma mesafesi hesaplanamıyor.
        panel.post {
            if (_binding == null) return@post
            panel.translationY = resultPanelHiddenY(panel)
            panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(RESULT_PANEL_ANIM_MS)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction { resultPanelAnimating = false }
                .start()
        }
    }

    /** Devam'da panel aşağı kayıp gidiyor (abaküs pratiğindeki gibi), sonra sıradaki soru. */
    private fun hideResultPanel(then: () -> Unit) {
        val panel = _binding?.resultPanel ?: return then()
        resultPanelAnimating = true
        panel.animate()
            .translationY(resultPanelHiddenY(panel))
            .setDuration(RESULT_PANEL_ANIM_MS)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                panel.visibility = View.GONE
                panel.translationY = 0f
                resultPanelAnimating = false
                if (_binding != null) then()
            }
            .start()
    }

    private fun resultPanelHiddenY(panel: View): Float =
        navBarPanelExtension?.hiddenTranslation(panel) ?: panel.height.toFloat()

    /** Uygulamanın ses ayarı ("sound_enabled") kapalıysa çalmıyor; AbacusFragment.playSound ile aynı. */
    private fun playSound(resId: Int, volume: Float) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return
        mediaPlayer?.release()
        mediaPlayer = android.media.MediaPlayer.create(ctx, resId)?.apply {
            setVolume(volume, volume)
            start()
        }
    }

    private fun nextQuestion() {
        if (questionIndex + 1 < questions.size) {
            questionIndex++
            showQuestion()
        } else {
            finishBlock()
        }
    }

    // ── Adım sonu ───────────────────────────────────────────────────────────

    private fun finishBlock() {
        if (sobiMode) return finishSobiTest()
        val b = _binding ?: return
        val hw = homework ?: return
        setPlayVisible(false)
        b.resultPanel.visibility = View.GONE
        b.summaryPanel.visibility = View.VISIBLE
        b.summaryTitle.text = appString(R.string.hw_player_block_done)
        b.summaryScore.text = appString(R.string.hw_player_block_score, blockCorrect, questions.size)
        b.summaryScore.visibility = View.VISIBLE
        b.summaryMessage.text = HomeworkText.title(hw.config) + " · " +
            appString(R.string.hw_card_progress, blockIndex + 1, hw.blockCount)
        saveBlock()
    }

    private fun saveBlock() {
        val b = _binding ?: return
        val hw = homework ?: return
        b.summaryProgress.visibility = View.VISIBLE
        b.summaryPrimary.visibility = View.INVISIBLE
        b.summarySecondary.visibility = View.INVISIBLE
        val answeredInBlock = questions.size
        val correctInBlock = blockCorrect
        Homeworks.submitBlock(homeworkId, progress, answeredInBlock, correctInBlock, blockTally.toMap()) { ok ->
            val bb = _binding ?: return@submitBlock
            bb.summaryProgress.visibility = View.GONE
            bb.summaryPrimary.visibility = View.VISIBLE
            bb.summarySecondary.visibility = View.VISIBLE
            if (!ok) {
                bb.summaryMessage.text = appString(R.string.hw_player_save_failed)
                bb.summaryPrimary.text = appString(R.string.hw_player_retry_save)
                bb.summaryPrimary.setOnClickListener { saveBlock() }
                bb.summarySecondary.text = appString(R.string.hw_player_finish)
                bb.summarySecondary.setOnClickListener { close() }
                return@submitBlock
            }
            progress = Homeworks.Progress(
                uid = progress.uid,
                completedBlocks = progress.completedBlocks + 1,
                answered = progress.answered + answeredInBlock,
                correct = progress.correct + correctInBlock,
            )
            if (progress.completedBlocks >= hw.blockCount) {
                bb.summaryMessage.text = appString(R.string.hw_player_all_done)
                bb.summaryPrimary.text = appString(R.string.hw_player_finish)
                bb.summaryPrimary.setOnClickListener { close() }
                bb.summarySecondary.visibility = View.GONE
            } else {
                bb.summaryPrimary.text = appString(R.string.hw_player_next_block)
                bb.summaryPrimary.setOnClickListener { startBlock() }
                bb.summarySecondary.text = appString(R.string.hw_player_finish)
                bb.summarySecondary.setOnClickListener { close() }
            }
        }
    }

    private fun showAllDone() {
        val b = _binding ?: return
        val hw = homework
        b.loading.visibility = View.GONE
        setPlayVisible(false)
        b.abacusContainer.visibility = View.GONE
        b.summaryPanel.visibility = View.VISIBLE
        b.summaryTitle.text = appString(R.string.hw_card_done)
        b.summaryScore.text = appString(R.string.hw_player_block_score, progress.correct, progress.answered)
        b.summaryMessage.text = hw?.let { HomeworkText.title(it.config) }.orEmpty()
        b.summaryPrimary.text = appString(R.string.hw_player_finish)
        b.summaryPrimary.setOnClickListener { close() }
        b.summarySecondary.visibility = View.GONE
    }

    private fun showUnavailable() {
        val b = _binding ?: return
        b.loading.visibility = View.GONE
        setPlayVisible(false)
        b.abacusContainer.visibility = View.GONE
        b.summaryPanel.visibility = View.VISIBLE
        b.summaryTitle.text = appString(R.string.hw_player_unavailable)
        b.summaryScore.visibility = View.GONE
        b.summaryMessage.text = ""
        b.summaryPrimary.text = appString(R.string.hw_player_finish)
        b.summaryPrimary.setOnClickListener { close() }
        b.summarySecondary.visibility = View.GONE
    }

    // ── Sağ düğmeler ────────────────────────────────────────────────────────

    private fun setupSideButtons() {
        val b = _binding ?: return
        rulesPanel = LessonRulesPanel(b.rulesPanelScrollView, layoutInflater) {
            questions.getOrNull(questionIndex)?.op == HomeworkOp.SUB
        }
        LessonSideButtons.setupReset(b.resetButton) { onReset() }
        LessonSideButtons.setupSkip(b.skipStepButton) { onSkip() }
        b.rulesBookButton.setOnClickListener { openRulesBook() }
        b.rulesPanelButton.setOnClickListener {
            val panel = rulesPanel ?: return@setOnClickListener
            // Derste kitaptan tablo seçilen bölümlerdeki gibi: tablo seçilmediyse önce kitap açılır.
            if (panel.hasContentToShow()) panel.toggle() else openRulesBook()
        }
    }

    /** Sıfırla (bkz. sınıf açıklaması). Cevaplandıysa ya da soru yoksa bir şey yapmaz. */
    private fun onReset() {
        val b = _binding ?: return
        if (answered || questions.isEmpty() || b.summaryPanel.visibility == View.VISIBLE) return
        abacusController?.reset(animate = true)
        b.answerInput.text = null
        if (flashStarted && questions[questionIndex].op != HomeworkOp.MUL) {
            stopFlash()
            setFlash("")
            // Akış bittiyse Kontrol açılmıştı, abaküssüzde cevap kutusu da: sayılar yeniden akarken kapansın.
            b.kontrolButton.isEnabled = false
            b.answerInput.visibility = View.GONE
            hideKeyboard()
            handler.postDelayed(restartFlashRunnable, RESTART_FLASH_DELAY_MS)
        }
    }

    /** Atla: gösterilen sayı hemen söner, sıradaki gelir (BlindingLessonFragment.skipToNextSequenceNumber ile aynı). */
    private fun onSkip() {
        val b = _binding ?: return
        if (!flashing) return
        b.skipStepButton.playAnimation()
        if (flashBlank) return
        handler.removeCallbacks(flashHideRunnable)
        hideFlashNumber()
    }

    /** Ayar paneli abaküsü sıfırlıyor: yarıdaki soru Başla'ya döner (akış ve kayıt baştan). */
    private fun onAbacusSettingsOpen() {
        if (!answered && questions.isNotEmpty() && _binding?.summaryPanel?.visibility != View.VISIBLE) {
            stopFlash()
            recordingAbacus = false
            showQuestion()
        } else {
            abacusController?.reset(animate = true)
        }
        abacusController?.setEnabled(false)
    }

    /** Kural kitabı tablo seçme kipinde; bölüm sorunun işlemine göre (çıkarma / çarpım tabloları). */
    private fun openRulesBook() {
        val rules = LessonRulesPanel.openRulesBook(childFragmentManager, R.id.rulesFragmentContainer) ?: return
        LessonRulesPanel.enableTablePicker(rules) { selection ->
            rulesPanel?.select(selection)
            rulesPanel?.show()
        }
        val op = questions.getOrNull(questionIndex)?.op ?: homework?.config?.op
        when (op) {
            HomeworkOp.SUB -> {
                rules.setActiveRulesContentSection(com.example.app.model.RulesFragment.RulesContentSection.EXTRACTION)
                rules.updateExtractionFiveRuleTableVisibility(View.VISIBLE)
                rules.updateTenRuleExtractionTableLayout(View.VISIBLE)
                rules.updateBeadRuleExtractionTableLayout(View.VISIBLE)
            }
            HomeworkOp.MUL -> rules.updateMultiplicationTablesVisibility(View.VISIBLE)
            else -> Unit
        }
        rules.applyRulesTableHeaderTexts(op == HomeworkOp.SUB)
    }

    // ── Çıkış ───────────────────────────────────────────────────────────────

    /** Adımın ortasındaysa onay soruluyor: cevaplar kaydedilmiyor, adım baştan başlıyor. */
    private fun requestQuit() {
        val b = _binding ?: return close()
        val midBlock = b.summaryPanel.visibility != View.VISIBLE && questions.isNotEmpty() &&
            (questionIndex > 0 || answered || flashing)
        if (!midBlock) return close()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(appString(if (sobiMode) R.string.sobi_test_quit_title else R.string.hw_player_quit_title))
            .setMessage(appString(if (sobiMode) R.string.sobi_test_quit_message else R.string.hw_player_quit_message))
            .setPositiveButton(appString(R.string.hw_player_quit_confirm)) { _, _ -> close() }
            .setNegativeButton(appString(R.string.class_cancel), null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_error))
    }

    /** @param showInterstitialAd Sobi testinden çıkışta geçiş reklamı (ödevde hiç yok). */
    private fun close(showInterstitialAd: Boolean = sobiMode) {
        handler.removeCallbacksAndMessages(null)
        hideKeyboard()
        val main = activity as? MainActivity
        if (main != null) main.finishHomeworkOverlay("HomeworkPlayer.close", showInterstitialAd)
        else parentFragmentManager.popBackStack()
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(InputMethodManager::class.java) ?: return
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        _binding?.resultPanel?.animate()?.cancel()
        navBarPanelExtension?.detach()
        navBarPanelExtension = null
        mediaPlayer?.release()
        mediaPlayer = null
        abacusSettings?.detach()
        abacusSettings = null
        rulesPanel = null
        abacusController = null
        _binding = null
        super.onDestroyView()
    }
}
