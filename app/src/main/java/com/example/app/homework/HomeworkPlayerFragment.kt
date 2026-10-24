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
 * Can (enerji) harcanmıyor, reklam yok: ödev öğretmenin verdiği iş.
 */
class HomeworkPlayerFragment : Fragment() {

    companion object {
        private const val ARG_HOMEWORK_ID = "homework_id"

        /** Sayının ekranda kaldığı payın dışında kalan boşluk: ardışık aynı sayılar ayırt edilsin. */
        private const val GAP_FRACTION = 0.15
        private const val MIN_GAP_MS = 40L
        private const val MAX_GAP_MS = 250L
        private const val RESULT_PANEL_ANIM_MS = 200L
        /** Abaküs değeri bu aralıkla okunuyor (kural çözümlemesi için, bkz. [RuleAnalysis]). */
        private const val ABACUS_SAMPLE_MS = 30L

        fun newInstance(homeworkId: String) = HomeworkPlayerFragment().apply {
            arguments = Bundle().apply { putString(ARG_HOMEWORK_ID, homeworkId) }
        }
    }

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

    // Kural kural başarı (RuleAnalysis): sorudaki abaküs değerleri, sayıların göründüğü anlar, adımın sayımı
    private val abacusLog = ArrayList<RuleAnalysis.Sample>()
    private val shownAtMs = ArrayList<Long>()
    private val blockTally = LinkedHashMap<String, IntArray>()
    private val abacusSampler = object : Runnable {
        override fun run() {
            val value = abacusController?.getCurrentValue()?.toLong() ?: return
            if (abacusLog.lastOrNull()?.value != value) abacusLog += RuleAnalysis.Sample(android.os.SystemClock.uptimeMillis(), value)
            handler.postDelayed(this, ABACUS_SAMPLE_MS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeworkPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeworkId = requireArguments().getString(ARG_HOMEWORK_ID).orEmpty()
        binding.quitButton.setOnClickListener { requestQuit() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = requestQuit()
        })
        binding.kontrolButton.setOnClickListener { onKontrol() }
        binding.answerInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { onKontrol(); true } else false
        }
        binding.resultButton.setOnClickListener { if (!resultPanelAnimating) hideResultPanel { nextQuestion() } }
        navBarPanelExtension = com.example.app.NavBarPanelExtension(binding.root, binding.resultPanel).also { it.attach() }
        setPlayVisible(false)

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

    // ── Kurulum ─────────────────────────────────────────────────────────────

    private fun setupAbacus(withAbacus: Boolean) {
        val b = _binding ?: return
        b.abacusContainer.visibility = if (withAbacus) View.VISIBLE else View.GONE
        if (!withAbacus) return
        val controller = AbacusBeadController(context = requireContext(), root = b.root, animationDurationMs = 300L)
        controller.setup()
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
        b.blackboardImage.visibility = v
        b.flashText.visibility = v
        b.kontrolButton.visibility = v
        b.stepText.visibility = v
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
        b.resultPanel.visibility = View.GONE
        b.stepText.text = appString(R.string.hw_player_step, blockIndex + 1, hw.blockCount, questionIndex + 1, questions.size)
        abacusController?.reset(animate = true)
        b.answerInput.text = null
        b.answerInput.visibility = View.GONE

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
        val gap = (cfg.intervalMs * GAP_FRACTION).toLong().coerceIn(MIN_GAP_MS, MAX_GAP_MS)
        val show = (cfg.intervalMs - gap).coerceAtLeast(MIN_GAP_MS)
        // Abaküslüde değerler kaydediliyor: hangi kuralda hata yapıldığı buradan çıkıyor
        abacusLog.clear()
        shownAtMs.clear()
        handler.removeCallbacks(abacusSampler)
        if (cfg.withAbacus) handler.post(abacusSampler)
        var t = 0L
        q.numbers.forEachIndexed { i, n ->
            // Çıkarmada başlangıçtan sonraki sayılar eksiyle gösteriliyor.
            val label = if (q.op == HomeworkOp.SUB && i > 0) "−$n" else n.toString()
            handler.postDelayed({ shownAtMs += android.os.SystemClock.uptimeMillis(); setFlash(label) }, t)
            t += show
            handler.postDelayed({ setFlash("") }, t)
            t += gap
        }
        handler.postDelayed({
            flashing = false
            onFlashFinished()
        }, t)
    }

    private fun onFlashFinished() {
        val b = _binding ?: return
        val withAbacus = homework?.config?.withAbacus == true
        if (questions[questionIndex].op != HomeworkOp.MUL) setFlash("?")
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

    /** Uzun sayılar (8 basamak, çarpma) tahtaya sığsın diye yazı küçülüyor. */
    private fun setFlash(text: String) {
        val b = _binding ?: return
        val base = resources.getDimension(R.dimen.abacus_fragment_equation_text_size)
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
        val given: Long? = if (homework?.config?.withAbacus == true) {
            abacusController?.getCurrentValue()?.toLong()
        } else {
            b.answerInput.text?.toString()?.trim()?.toLongOrNull()
        }
        if (given == null) return
        answered = true
        handler.removeCallbacks(abacusSampler)
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
        b.resultMessage.text = HomeworkText.questionLine(q) + " = " + q.answer
        b.resultButton.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        b.kontrolButton.isEnabled = false
        // Ses ve ses düzeyi AbacusFragment.showResultPanel ile aynı.
        if (correct) playSound(R.raw.correct_answer_sound, 0.3f) else playSound(R.raw.incorrect_answer_sound, 1f)
        showResultPanel()
    }

    /** Sorunun kural sayımını adımın sayımına ekler (bkz. [RuleAnalysis]). Hata olursa soru sayılmaz. */
    private fun countRules(q: HomeworkQuestion, given: Long, correct: Boolean) {
        val cfg = homework?.config ?: return
        val attempts = runCatching {
            when {
                q.op == HomeworkOp.MUL -> RuleAnalysis.multiplication(cfg, correct)
                cfg.withAbacus -> RuleAnalysis.abacus(q, abacusLog.toList(), shownAtMs.toList(), given)
                else -> emptyList() // abaküssüz toplama/çıkarma sayılmıyor
            }
        }.getOrDefault(emptyList())
        for ((key, cw) in RuleAnalysis.tally(attempts)) {
            val sum = blockTally.getOrPut(key) { IntArray(2) }
            sum[0] += cw[0]
            sum[1] += cw[1]
        }
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

    // ── Çıkış ───────────────────────────────────────────────────────────────

    /** Adımın ortasındaysa onay soruluyor: cevaplar kaydedilmiyor, adım baştan başlıyor. */
    private fun requestQuit() {
        val b = _binding ?: return close()
        val midBlock = b.summaryPanel.visibility != View.VISIBLE && questions.isNotEmpty() &&
            (questionIndex > 0 || answered || flashing)
        if (!midBlock) return close()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(appString(R.string.hw_player_quit_title))
            .setMessage(appString(R.string.hw_player_quit_message))
            .setPositiveButton(appString(R.string.hw_player_quit_confirm)) { _, _ -> close() }
            .setNegativeButton(appString(R.string.class_cancel), null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_error))
    }

    private fun close() {
        handler.removeCallbacksAndMessages(null)
        hideKeyboard()
        val main = activity as? MainActivity
        if (main != null) main.finishHomeworkOverlay("HomeworkPlayer.close")
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
        abacusController = null
        _binding = null
        super.onDestroyView()
    }
}
