package com.example.app

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.example.app.GlobalValues.lessonStep
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.MapFragment.Companion.getLessonOperationsBlinding
import com.example.app.abacus.AbacusBeadController
import com.example.app.databinding.FragmentBlindingLessonBinding
import com.example.app.model.LessonItem
import com.example.app.model.RulesFragment
class BlindingLessonFragment : Fragment() {
    companion object {
        private const val ARG_DAILY_MODE = "daily_mode"
        private const val ARG_DAILY_PERIOD_KEY = "daily_period_key"
        private const val ARG_DAILY_SLOT_INDEX = "daily_slot_index"
        private const val PRACTICE_TOUCH_BLOCKER_TAG = "practice_touch_blocker"

        fun newDailyQuestionInstance(
            operations: List<Any>,
            periodKey: String,
            slotIndex: Int,
        ): BlindingLessonFragment {
            return BlindingLessonFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_DAILY_MODE, true)
                    putSerializable("operations", ArrayList(operations))
                    putString(ARG_DAILY_PERIOD_KEY, periodKey)
                    putInt(ARG_DAILY_SLOT_INDEX, slotIndex)
                }
            }
        }
    }
    private lateinit var abacusController: AbacusBeadController
    private var abacusMetricsInitialized = false

    private var mediaPlayer: MediaPlayer? = null
    private var learningSessionStartMs: Long? = null
    private var operations: List<Any> = emptyList()
    private lateinit var numberText: TextView
    private var currentIndex = 0
    private var answerNumber = 0
    private var lastClickTime = 0L
    private var controlNumber = 0
    private var correctAnswer = 0
    private var totalQuestions = 0
    private lateinit var firstNumberText: TextView
    private lateinit var firstNumberText2: TextView
    private lateinit var operatorText: TextView
    private lateinit var secondNumberText: TextView
    private lateinit var correctAnswerText: TextView
    private lateinit var correctAnswerLabel: TextView
    private lateinit var controlButton: Button
    private lateinit var incorrectPanel: View
    private lateinit var correctPanel: View
    private lateinit var lottieView: LottieAnimationView

    private lateinit var fabHint: LottieAnimationView
    private lateinit var tvHint: TextView
    private var isHintVisible = false
    private lateinit var fabHintTouchArea: View
    private lateinit var lessonItem : LessonItem
    private lateinit var rulesBookButton: ImageView
    private var resultDialog: Dialog? = null

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private lateinit var timerTextView: TextView
    private var isTimerStarted = false
    private lateinit var binding: FragmentBlindingLessonBinding
    private var currentTime: String = "0:00"
    private var isDailyQuestionMode = false
    /** Kart açılırken kilitlenen periyot; ödül bu anahtarla eşleşmeli. */
    private var dailyQuestionSessionPeriodKey: String? = null
    /** Bu oturumdaki soru indeksi (0..2). */
    private var dailyQuestionSlotIndex: Int = 0
    private var lessonStarted = false

    private var isShowingSequence = false
    private var currentSequenceIndex = 0
    private var currentSequence: List<Int> = emptyList()
    private val sequenceRevealRunnable: Runnable = Runnable { onSequenceRevealStep() }
    private val showNextNumberRunnable: Runnable = Runnable { onShowNextNumberStep() }
    private val restartSequenceRunnable: Runnable = Runnable { onRestartSequenceAfterReset() }

    private fun onSequenceRevealStep() {
        if (!isShowingSequence) return
        if (currentSequenceIndex >= currentSequence.size) {
            finishSequencePlayback()
            return
        }
        numberText.text = currentSequence[currentSequenceIndex].toString()
        currentSequenceIndex++
        if (currentSequenceIndex < currentSequence.size) {
            handler.postDelayed(showNextNumberRunnable, lessonItem.timePeriod?.toLong() ?: 1000L)
        } else {
            finishSequencePlayback()
        }
    }

    private fun onShowNextNumberStep() {
        if (!isShowingSequence) return
        if (currentSequenceIndex >= currentSequence.size) {
            finishSequencePlayback()
            return
        }
        numberText.text = ""
        handler.postDelayed(sequenceRevealRunnable, 200L)
    }

    private fun onRestartSequenceAfterReset() {
        if (!isAdded) return
        startShowingSequence(currentSequence)
    }

    private fun stopSequencePlayback() {
        handler.removeCallbacks(showNextNumberRunnable)
        handler.removeCallbacks(sequenceRevealRunnable)
        handler.removeCallbacks(restartSequenceRunnable)
        isShowingSequence = false
    }

    private fun finishSequencePlayback() {
        isShowingSequence = false
        controlButton.isEnabled = true
        controlButton.setBackgroundColor(resources.getColor(R.color.button_enabled, null))
        controlButton.setTextColor(resources.getColor(R.color.button_text_enabled, null))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDailyQuestionMode = arguments?.getBoolean(ARG_DAILY_MODE, false) == true
        if (isDailyQuestionMode) {
            dailyQuestionSessionPeriodKey = arguments?.getString(ARG_DAILY_PERIOD_KEY)
                ?: DailyQuestionPeriod.currentPeriodKey()
            dailyQuestionSlotIndex = arguments?.getInt(ARG_DAILY_SLOT_INDEX, 0)?.coerceIn(0, 2) ?: 0
        }
        lessonItem = if (isDailyQuestionMode) {
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Günlük Soru",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                isBlinding = null,
                blindingMultiplication = false,
                timePeriod = 2000L,
            )
        } else {
            LessonManager.getLessonItem(mapFragmentStepIndex)!!
        }

        uploadLessonData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlindingLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findsId()
        if (isDailyQuestionMode) {
            binding.progressBarContainer.visibility = View.GONE
            binding.hintContainer.visibility = View.GONE
            binding.fabHintTouchArea.visibility = View.GONE
            binding.rulesBookButton.visibility = View.GONE
        }
        controlButtonAnim()
        setupStartButton()
        setupQuitButton()
        setupBackPressHandler()
        rulesBookButtonClick()
        resetClickListener()
        blindingOrRace()
        setupAbacusController()
    }

    private fun uploadLessonData(){
        operations = arguments?.getSerializable("operations") as? List<Any> ?: emptyList()

        if (operations.isEmpty()) {
            lessonStep = lessonItem.startStepNumber!!
            // lessonStep değerini kontrol et ve güvenli bir şekilde kullan
            val currentLessonStep = if (lessonStep > 0) lessonStep else 1
            val minLessonId = lessonItem.minLessonOperationsId()
            val requestedLessonId = if (lessonItem.stepIsFinish) {
                lessonItem.finishStepNumber!!
            } else {
                currentLessonStep
            }
            val resolvedLessonId = MapFragment.resolveLessonOperationsBlindingId(
                requestedLessonId,
                minLessonId,
            )
            operations = getLessonOperationsBlinding(resolvedLessonId)

        }
    }
    private fun timeStarter(){
        if(lessonItem.type == 2){
            timerTextView.visibility = View.VISIBLE
        }
        else{
            timerTextView.visibility = View.INVISIBLE
        }
    }

    private fun startTimerIfNeeded() {
        if (lessonItem.type == 2 && !isTimerStarted) {
            startTimer()
            isTimerStarted = true
        }
    }

    private fun setupStartButton() {
        binding.startButton.setOnClickListener {
            if (lessonStarted) return@setOnClickListener
            lessonStarted = true
            binding.startButton.visibility = View.GONE
            controlButton.visibility = View.VISIBLE
            startTimerIfNeeded()
            showCurrentOperation()
        }
    }
    private fun blindingOrRace() {
        if (lessonItem.isBlinding == true) {
            binding.abacusLinear.visibility = View.INVISIBLE
            binding.numberInput.visibility = View.VISIBLE
        } else {
            binding.numberInput.visibility = View.INVISIBLE
            binding.abacusLinear.visibility = View.VISIBLE
        }
        syncAbacusTouchEnabled()
    }
    private fun startTimer() {
        runnable = object : Runnable {
            override fun run() {
                seconds++
                val minutes = seconds / 60
                val secs = seconds % 60
                currentTime = String.format("%d:%02d", minutes, secs)
                timerTextView.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }
    private fun findsId(){
        timerTextView = binding.timerTextView
        binding.resetButton.frame = 20
        rulesBookButton = binding.rulesBookButton
        fabHint = binding.fabHint
        tvHint = binding.tvHint
        fabHintTouchArea = binding.fabHintTouchArea
        fubHintClickListener()
        lottieView = binding.lottieView
        correctAnswerText = binding.correctAnswerText
        correctAnswerLabel = binding.correctAnswerLabel
        incorrectPanel = binding.incorrectPanel
        correctPanel = binding.correctPanel
        numberText = binding.firstNumberText
        controlButton = binding.kontrolButton
        totalQuestions = operations.size
        firstNumberText = binding.firstNumberText
        firstNumberText2 = binding.firstNumberText2
        operatorText = binding.operator
        secondNumberText = binding.secondNumberText
        timeStarter()

    }
    private fun resetClickListener() {
        // İlk başta 20. frame'de başlat

        binding.resetButton.setOnClickListener {
            binding.resetButton.playAnimation()
            controlNumber = 0
            when {
                lessonItem.isBlinding == null -> resetAbacus()
                lessonItem.isBlinding == true -> binding.numberInput.setText("")
            }
            if (currentSequence.isNotEmpty()) {
                stopSequencePlayback()
                numberText.text = ""
                handler.postDelayed(restartSequenceRunnable, 200L)
            }
        }

        // Animasyon bitince tekrar 20. frame'e getir
        binding.resetButton.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.resetButton.frame = 20
                }, 1000) // 1000 ms = 1 saniye
            }
        })
    }
    private fun fubHintClickListener(){
        fabHintTouchArea.setOnClickListener {
            if (!isHintVisible) {
                showHint()
            } else {
                hideHint()
            }
            // veya doğrudan fabHint'in tıklama fonksiyonunu çağır
        }
    }
    private fun showHint() {
        tvHint.text = lessonItem.lessonHint?.let { splitTextEqually(it) }

        // Play Lottie animation
        fabHint.playAnimation()
        fabHint.setFrame(25)

        // Show and animate TextView
        tvHint.visibility = View.VISIBLE
        tvHint.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right))

        isHintVisible = true
    }
    private fun hideHint() {
        // Sağa kayma animasyonunu başlat
        fabHint.cancelAnimation()
        fabHint.setFrame(0)

        val slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right_hint)

        // Animasyon bittiğinde TextView'ı gizle
        slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                tvHint.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Animasyonu başlat
        tvHint.startAnimation(slideOutAnimation)
        isHintVisible = false
    }
    private fun splitTextEqually(text: String): String {
        val words = text.split(" ")
        val totalWords = words.size

        // Kelimeleri iki gruba böl
        val firstLine = words.take(totalWords / 2).joinToString(" ")
        val secondLine = words.drop(totalWords / 2).joinToString(" ")

        return "$firstLine\n$secondLine"
    }

    private fun fillIncorrectPanelAnswers() {
        correctAnswerLabel.text = "Doğru cevap $answerNumber"
        correctAnswerText.text = "Senin cevabın $controlNumber"
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun controlButtonAnim() {
        controlButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.85f)
                        .scaleY(0.85f)
                        .setDuration(100)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime >= 100) {
                        lastClickTime = currentTime

                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(400)
                            .setInterpolator(BounceInterpolator())
                            .start()

                        // Tıklama işlemini gerçekleştir (cevabı tek kez hesapla)
                        val isCorrect = stepAnswerAlgorithm()
                        updateProgressBar(isCorrect)
                        showResultPanel(isCorrect)
                        controlNumber = 0
                        binding.numberInput.setText("")
                    }
                    true
                }

                else -> false
            }
        }

    }
    //Sorudaki sayıları gösterir
    private fun showCurrentOperation() {
        if (operations.isEmpty()) {
            Log.e("AbacusFragment", "Operations list is empty")
            return
        }

        if(lessonItem.blindingMultiplication == true){
            if (currentIndex < operations.size) {
                val currentOperation = operations[currentIndex] as MathOperation
                currentOperation.firstNumber?.let { number ->
                    firstNumberText2.text = number.toString()
                }

                currentOperation.operator?.let { op ->
                    operatorText.text = op
                }

                currentOperation.secondNumber?.let { number ->
                    secondNumberText.text = number.toString()
                }
            }
        }
        else{
            if (currentIndex < operations.size) {
                val currentOperation = operations[currentIndex]
                when (currentOperation) {
                    is List<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        startShowingSequence(currentOperation as List<Int>)
                    }
                    is Int -> {
                        // Eğer operations bir Int listesi ise, direkt olarak sequence olarak göster
                        startShowingSequence(operations as List<Int>)
                    }
                    is MathOperation -> {
                        currentOperation.firstNumber?.let { number ->
                            numberText.text = number.toString()
                        }
                    }
                }
            }
        }
    }
    private fun setupQuitButton() {
        binding.quitButton.setOnClickListener {
            closeFragment()
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    closeFragment()
                }
            },
        )
    }
    private fun closeFragment() {
        if (isDailyQuestionMode) {
            (activity as? MainActivity)?.finishTasksOverlayAnimated("dailyQuestion.close")
                ?: parentFragmentManager.popBackStack()
            return
        }
        val main = activity as? MainActivity
        val fm = parentFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
            fm.executePendingTransactions()
        } else if (isAdded) {
            fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
                .remove(this@BlindingLessonFragment)
                .commitNowAllowingStateLoss()
        }
        main?.prepareMapReturnAfterLessonClaim()
        main?.finalizeMapReturnAfterLessonClaim("BlindingLessonFragment.quit")
    }
    private fun rulesBookButtonClick(){
        rulesBookButton.setOnClickListener{
            childFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_down,
                    0
                )
                .replace(R.id.rulesFragmentContainer, RulesFragment())
                .commit()
        }

    }
    private fun playSound(soundResId: Int) {
        mediaPlayer?.release() // Önceki sesi serbest bırak
        mediaPlayer = MediaPlayer.create(requireContext(), soundResId)
        mediaPlayer?.start()
    }

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    private fun showResultPanel(isCorrect: Boolean) {
        // Eğer dialog zaten gösteriliyorsa, yeni dialog oluşturma
        if (resultDialog?.isShowing == true) {
            return
        }


        if (isCorrect) {
            // Doğru cevap durumu

            correctAnswer++
            playSound(R.raw.correct_answer_sound)

            if (currentIndex == operations.size - 1 && correctAnswer == totalQuestions) {
                lottieView.visibility = View.VISIBLE
                lottieView.playAnimation()
            }
            correctPanel.translationY = correctPanel.height.toFloat()
            correctPanel.visibility = View.VISIBLE
            correctPanel.alpha = 0f

            // Overlay'i görünür yap
            binding.root.findViewById<View>(R.id.overlay).visibility = View.VISIBLE

            correctPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            // Geri tuşu dinleyicisi
            correctPanel.findViewById<Button>(R.id.continueButton)
                .setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate()
                                .scaleX(0.85f)
                                .scaleY(0.85f)
                                .setDuration(100)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            val willFinishLesson = currentIndex + 1 > operations.size - 1
                            if (isDailyQuestionMode && willFinishLesson) {
                                v.isEnabled = false
                                addExitTouchBlocker()
                            }
                            currentIndex++
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(BounceInterpolator())
                                .start()
                            binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                            correctPanel.animate()
                                .translationY(correctPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    correctPanel.visibility = View.GONE
                                }
                                .start()
                            if(lessonItem.isBlinding == null){
                                resetAbacus()
                            }
                            if (currentIndex <= operations.size - 1) {
                                showCurrentOperation()
                            } else {
                                finishLessonAfterLastQuestion()
                            }
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                            true
                        }

                        else -> false
                    }
                }
        } else {
            // Yanlış cevap durumu
            playSound(R.raw.incorrect_answer_sound)
            incorrectPanel.translationY = incorrectPanel.height.toFloat()
            incorrectPanel.visibility = View.VISIBLE
            incorrectPanel.alpha = 0f
            binding.root.findViewById<View>(R.id.overlay).visibility = View.VISIBLE
            incorrectPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            incorrectPanel.findViewById<Button>(R.id.okayButton)
                .setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate()
                                .scaleX(0.85f)
                                .scaleY(0.85f)
                                .setDuration(100)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (isDailyQuestionMode) {
                                v.isEnabled = false
                                addExitTouchBlocker()
                                v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(400)
                                    .setInterpolator(BounceInterpolator())
                                    .start()
                                binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                                incorrectPanel.animate()
                                    .translationY(incorrectPanel.height.toFloat())
                                    .setDuration(200)
                                    .setInterpolator(AccelerateInterpolator())
                                    .withEndAction {
                                        incorrectPanel.visibility = View.GONE
                                        handleDailyQuestionWrongAnswer()
                                    }
                                    .start()
                                return@setOnTouchListener true
                            }
                            if (isRacePanelLesson()) {
                                v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(400)
                                    .setInterpolator(BounceInterpolator())
                                    .start()
                                binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                                incorrectPanel.animate()
                                    .translationY(incorrectPanel.height.toFloat())
                                    .setDuration(200)
                                    .setInterpolator(AccelerateInterpolator())
                                    .withEndAction {
                                        incorrectPanel.visibility = View.GONE
                                        showLessonResultFalse()
                                    }
                                    .start()
                                return@setOnTouchListener true
                            }
                            currentIndex++
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(BounceInterpolator())
                                .start()
                            binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                            incorrectPanel.animate()
                                .translationY(incorrectPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    incorrectPanel.visibility = View.GONE
                                }
                                .start()
                            if(lessonItem.isBlinding == null){
                                resetAbacus()
                            }
                            if (currentIndex <= operations.size - 1) {
                                showCurrentOperation()
                            } else {
                                finishLessonAfterLastQuestion()
                            }
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                            true
                        }

                        else -> false
                    }
                }
        }
    }

    private fun updateProgressBar(isCorrect: Boolean) {
        val fill = requireView().findViewById<View>(R.id.progressBarFill)
        val empty = requireView().findViewById<View>(R.id.progressBarEmpty)

        val startWeight = (fill.layoutParams as LinearLayout.LayoutParams).weight
        val endWeight = (currentIndex + 1).toFloat() / operations.size

        // Renkler: yeşil -> sarı -> yeşil
        val colorGreen = Color.parseColor("#7CFC00")    // Yeşil
        val colorYellow = Color.parseColor("#FFFF99")    // Açık sarı
        val colorDarkRed = Color.parseColor("#990000")

        if (isCorrect) {

            val animator = ValueAnimator.ofFloat(startWeight, endWeight)
            animator.duration = 700
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                (fill.layoutParams as LinearLayout.LayoutParams).weight = value
                (empty.layoutParams as LinearLayout.LayoutParams).weight = 1 - value
                fill.requestLayout()
                empty.requestLayout()

                // Renk geçişi: ilk yarıda yeşilden sarıya, ikinci yarıda sarıdan yeşile
                val fraction = (value - startWeight) / (endWeight - startWeight)
                val color = when {
                    fraction <= 0.5f -> {
                        // İlk yarı: turuncu -> açık sarı
                        ArgbEvaluator().evaluate(fraction * 2, colorGreen, colorYellow) as Int
                    }

                    else -> {
                        // İkinci yarı: açık sarı -> yeşil
                        ArgbEvaluator().evaluate(
                            (fraction - 0.5f) * 2,
                            colorYellow,
                            colorGreen
                        ) as Int
                    }
                }

                // Drawable'ın rengini güncelle
                val background = fill.background
                if (background is GradientDrawable) {
                    background.setColor(color)
                }
            }

            animator.start()
        } else {
            val animator = ValueAnimator.ofFloat(startWeight, endWeight)
            animator.duration = 700
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { animation ->
                // Weight (genişlik) animasyonu
                val value = animation.animatedValue as Float
                (fill.layoutParams as LinearLayout.LayoutParams).weight = value
                (empty.layoutParams as LinearLayout.LayoutParams).weight = 1 - value
                fill.requestLayout()
                empty.requestLayout()

                // Basit renk geçişi: yeşilden kırmızıya
                val fraction = (value - startWeight) / (endWeight - startWeight)
                val color = ArgbEvaluator().evaluate(fraction, colorGreen, colorDarkRed) as Int

                // Drawable'ın rengini güncelle
                val background = fill.background
                if (background is GradientDrawable) {
                    background.setColor(color)
                }
            }
            animator.start()
        }

    }


    private fun setupAbacusController() {
        abacusController = AbacusBeadController(
            context = requireContext(),
            root = binding.root,
            animationDurationMs = if (lessonItem.type == 2) 50L else 300L,
        )
        abacusController.setup()
        ensureAbacusMetricsIfVisible()
        syncAbacusTouchEnabled()
    }

    private fun syncAbacusTouchEnabled() {
        if (!::abacusController.isInitialized) return
        val abacusVisible = binding.abacusLinear.visibility == View.VISIBLE
        abacusController.setEnabled(abacusVisible)
        if (abacusVisible) {
            ensureAbacusMetricsIfVisible()
        }
    }

    private fun ensureAbacusMetricsIfVisible() {
        if (abacusMetricsInitialized) return
        if (binding.abacusLinear.visibility != View.VISIBLE) return
        binding.abacusLinear.post {
            if (!isAdded || view == null) return@post
            if (abacusMetricsInitialized) return@post
            if (binding.abacusLinear.visibility != View.VISIBLE) return@post
            val ok = abacusController.computeMovementDistancesFromLayout(ratio = 1.0f, force = true)
            if (ok) {
                abacusMetricsInitialized = true
                abacusController.syncStateFromUi()
            }
        }
    }

    private fun resetAbacus() {
        abacusController.reset()
    }

    private fun stepAnswerAlgorithm(): Boolean {
        if (operations.isEmpty()) {
            return false
        }
        val currentOperation = operations[currentIndex]
        return when (currentOperation) {
            is MathOperation -> {
                if (lessonItem.blindingMultiplication == true) {
                    answerNumber = currentOperation.firstNumber?.times(currentOperation.secondNumber!!) ?: 0
                    val inputText = binding.numberInput.text.toString()
                    if (inputText.isNotEmpty()) {
                        try {
                            controlNumber = inputText.toInt()
                            if (controlNumber == answerNumber) {
                                controlNumber = 0
                                true
                            } else {
                                fillIncorrectPanelAnswers()
                                controlNumber = 0
                                false
                            }
                        } catch (e: NumberFormatException) {
                            fillIncorrectPanelAnswers()
                            false
                        }
                    } else {
                        fillIncorrectPanelAnswers()
                        false
                    }
                } else {
                    // Normal MathOperation işlemi
                    false
                }
            }
            is List<*> -> {

                @Suppress("UNCHECKED_CAST")
                val sequence = currentOperation as List<Int>
                val result = sequence.sum()
                answerNumber = result
                if (currentSequenceIndex > 0) {
                    Log.d("dorumula1", controlNumber.toString())
                    val inputText = binding.numberInput.text.toString()
                    if (inputText.isNotEmpty() || lessonItem.isBlinding == null) {
                        Log.d("dorumula3", controlNumber.toString())
                        try {
                            if (lessonItem.isBlinding == true){
                                controlNumber = inputText.toInt()
                            }
                            Log.d("dorumula", controlNumber.toString())
                            if (lessonItem.isBlinding == null) {
                                controlNumber = abacusController.getCurrentValue()
                            }
                            Log.d("dorumula2", controlNumber.toString())
                            if (controlNumber == answerNumber) {
                                controlNumber = 0
                                true
                            } else {
                                fillIncorrectPanelAnswers()
                                controlNumber = 0
                                false
                            }
                        } catch (e: NumberFormatException) {
                            fillIncorrectPanelAnswers()
                            false
                        }
                    } else {
                        fillIncorrectPanelAnswers()
                        false
                    }
                } else {
                    fillIncorrectPanelAnswers()
                    false
                }
            }
            else -> false
        }
    }
    private fun isRacePanelLesson(): Boolean = lessonItem.raceBusyLevel != null

    private fun raceLessonPassed(): Boolean =
        totalQuestions > 0 && correctAnswer == totalQuestions

    private fun finishLessonAfterLastQuestion() {
        when {
            isDailyQuestionMode -> handleDailyQuestionLessonComplete()
            isRacePanelLesson() -> {
                if (raceLessonPassed()) showChestResult() else showLessonResultFalse()
            }
            lessonItem.type == 2 -> showChestResult()
            else -> showLessonResult()
        }
    }

    private fun lessonResultArgs(): Bundle {
        val successRate = if (totalQuestions > 0) {
            (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }
        val dersPuani = (successRate * 5f).toInt()
        return Bundle().apply {
            putInt("correctAnswers", correctAnswer)
            putInt("totalQuestions", totalQuestions)
            putFloat("successRate", successRate)
            putInt("dersPuani", dersPuani)
        }
    }

    private fun showLessonResultFalse() {
        val lessonResultFalse = LessonResultFalse()
        lessonResultFalse.arguments = lessonResultArgs()
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .replace(R.id.abacusFragmentContainer, lessonResultFalse)
            .commit()
    }

    private fun showChestResult() {
        val chestResultFragment = if (lessonItem.raceBusyLevel != null) {
            ChestFragment()
        } else {
            ChestResult()
        }
        // Başarı oranını hesapla
        val successRate = if (totalQuestions > 0) {
            (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }
        val dersPuani = (successRate * 5f).toInt()

        val args = Bundle().apply {
            putInt("correctAnswers", correctAnswer)
            putInt("totalQuestions", totalQuestions)
            putFloat("successRate", successRate)
            putString("time", currentTime)
            putInt("dersPuani", dersPuani)
            putInt(
                "worstCupTime",
                LessonManager.getLessonItem(mapFragmentStepIndex)?.worstCupTime ?: 0
            )
        }
        chestResultFragment.arguments = args

        // Yeni fragment'ı abacus container'a ekle
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.abacusFragmentContainer, chestResultFragment)
            .commit()
    }

    private fun isDailyQuestionSessionPeriodValid(): Boolean {
        val sessionKey = dailyQuestionSessionPeriodKey ?: return false
        return sessionKey == DailyQuestionPeriod.currentPeriodKey()
    }

    private fun handleDailyQuestionWrongAnswer() {
        if (!isAdded) return
        val periodKey = dailyQuestionSessionPeriodKey
        if (periodKey.isNullOrEmpty() || !isDailyQuestionSessionPeriodValid()) {
            closeFragment()
            return
        }
        DailyQuestionRepository.markPendingDiamondContinue(
            requireContext(),
            periodKey,
            dailyQuestionSlotIndex,
        ) {
            if (!isAdded) return@markPendingDiamondContinue
            DailyQuestionBrokenHeartStore.requestPlayOnNextBind(requireContext(), periodKey)
            closeFragment()
        }
    }

    private fun handleDailyQuestionLessonComplete() {
        if (!isAdded) return
        if (!isDailyQuestionSessionPeriodValid()) {
            closeFragment()
            return
        }
        val periodKey = dailyQuestionSessionPeriodKey ?: run {
            closeFragment()
            return
        }
        DailyQuestionRepository.incrementSolvedCount(requireContext(), periodKey) { _ ->
            if (!isAdded) return@incrementSolvedCount
            DailyQuestionBrokenHeartStore.clearBrokenHold116(requireContext(), periodKey)
            closeFragment()
        }
    }

    private fun showLessonResult() {
        val lessonResultFragment = LessonResult()
        lessonResultFragment.arguments = lessonResultArgs()
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .replace(R.id.abacusFragmentContainer, lessonResultFragment)
            .commit()
    }
    private fun startShowingSequence(sequence: List<Int>) {
        stopSequencePlayback()
        currentSequence = sequence
        if (currentSequence.isEmpty()) return

        currentSequenceIndex = 0
        isShowingSequence = true
        controlButton.isEnabled = false
        controlButton.setBackgroundColor(resources.getColor(R.color.button_disabled, null))
        controlButton.setTextColor(resources.getColor(R.color.button_text_disabled, null))

        numberText.text = currentSequence[0].toString()
        currentSequenceIndex = 1
        handler.postDelayed(showNextNumberRunnable, lessonItem.timePeriod ?: 1000L)
    }

    override fun onDestroyView() {
        stopLearningSessionTracking()
        releaseLaunchTouchBlocker()
        super.onDestroyView()
        stopSequencePlayback()
    }

    override fun onResume() {
        super.onResume()
        startLearningSessionTracking()
        if (isDailyQuestionMode) {
            binding.root.postDelayed({ releaseLaunchTouchBlocker() }, 320)
        }
    }

    override fun onPause() {
        stopLearningSessionTracking()
        super.onPause()
    }

    private fun startLearningSessionTracking() {
        if (learningSessionStartMs == null) {
            learningSessionStartMs = System.currentTimeMillis()
        }
    }

    private fun stopLearningSessionTracking() {
        val startMs = learningSessionStartMs ?: return
        learningSessionStartMs = null
        val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(0L)
        val ctx = context ?: return
        if (elapsedMs > 0L) {
            MissionsProgressStore.recordLearningDurationMs(ctx, elapsedMs)
        }
    }

    private fun addExitTouchBlocker() {
        val content = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(PRACTICE_TOUCH_BLOCKER_TAG) != null) return
        val blocker = View(requireContext()).apply {
            tag = PRACTICE_TOUCH_BLOCKER_TAG
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
            elevation = 1000f
        }
        content.addView(blocker)
    }

    private fun releaseLaunchTouchBlocker() {
        val content = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.findViewWithTag<View>(PRACTICE_TOUCH_BLOCKER_TAG)?.let { blocker ->
            content.removeView(blocker)
        }
    }
}
