package com.example.app

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.example.app.GlobalLessonData.globalPartId
import com.example.app.GlobalValues.lessonStep
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.MapFragment.Companion.getLessonOperationsBlinding
import com.example.app.abacus.AbacusBeadController
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentBlindingLessonBinding
import com.example.app.model.LessonItem
import com.example.app.model.RulesFragment
import androidx.core.view.isInvisible

class BlindingLessonFragment : Fragment() {
    companion object {
        private const val ARG_DAILY_MODE = "daily_mode"
        private const val ARG_DAILY_PERIOD_KEY = "daily_period_key"
        private const val ARG_DAILY_SLOT_INDEX = "daily_slot_index"
        private const val ARG_DAILY_INTERVAL_MS = "daily_interval_ms"
        private const val ARG_DAILY_PART_ID = "daily_part_id"
        private const val PRACTICE_TOUCH_BLOCKER_TAG = "practice_touch_blocker"

        fun newDailyQuestionInstance(
            operations: List<Any>,
            periodKey: String,
            slotIndex: Int,
            displayIntervalMs: Long?,
            partId: Int,
        ): BlindingLessonFragment {
            return BlindingLessonFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_DAILY_MODE, true)
                    putSerializable("operations", ArrayList(operations))
                    putString(ARG_DAILY_PERIOD_KEY, periodKey)
                    putInt(ARG_DAILY_SLOT_INDEX, slotIndex)
                    putInt(ARG_DAILY_PART_ID, partId)
                    displayIntervalMs?.let { putLong(ARG_DAILY_INTERVAL_MS, it) }
                }
            }
        }
    }
    private lateinit var abacusController: AbacusBeadController
    private var abacusMetricsInitialized = false
    private var originalTargetElevation: Float = 0f

    private var mediaPlayer: MediaPlayer? = null
    private var learningSessionStartMs: Long? = null
    private var operations: List<Any> = emptyList()
    private lateinit var numberText: TextView
    private var currentIndex = 0
    private var answerNumber = 0
    private var lastClickTime = 0L
    private var isResultPanelAnimating = false
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
    private lateinit var rulesPanelButton: ImageView

    private enum class RulesPanelTableType {
        NONE, FIVE, TEN_FIVE, TEN, BEAD, BEAD_EXTRACTION, TEN_EXTRACTION, MULTIPLICATION
    }

    private var activeRulesPanelTable = RulesPanelTableType.NONE
    private var selectedMultiplicationDigit: Int? = null
    private var inflatedMultiplicationDigit: Int? = null

    private fun multiplicationLayoutForDigit(digit: Int): Int = when (digit) {
        1 -> R.layout.multiplication_table_1
        2 -> R.layout.multiplication_table_2
        3 -> R.layout.multiplication_table_3
        4 -> R.layout.multiplication_table_4
        5 -> R.layout.multiplication_table_5
        6 -> R.layout.multiplication_table_6
        7 -> R.layout.multiplication_table_7
        8 -> R.layout.multiplication_table_8
        9 -> R.layout.multiplication_table_9
        else -> R.layout.multiplication_table_1
    }

    private fun usesRulesTablePicker(): Boolean = globalPartId in 4..9 || isDailyQuestionMode
    private var resultDialog: Dialog? = null

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private lateinit var timerTextView: TextView
    private var isTimerStarted = false
    private lateinit var binding: FragmentBlindingLessonBinding
    private var currentTime: String = "0:00"
    private var isDailyQuestionMode = false
    private var isAbacusSettingsPanelOpen = false
    
    // Guide UI Variables
    private lateinit var panelContent: View
    private lateinit var ivGuideImage: ImageView
    private lateinit var tvGuideText: TextView
    private lateinit var stepDotsContainer: LinearLayout
    private var currentBubbleAnimator: android.animation.ValueAnimator? = null
    private var currentAnimatedView: View? = null
    private var originalTextColor: Int? = null
    private var originalImageTintList: android.content.res.ColorStateList? = null
    private var originalBackgroundTintList: android.content.res.ColorStateList? = null
    private var bubbleImageTintBreathApplied = false
    private var guideTypewriterRunnable: Runnable? = null
    private var controlButtonListener: View.OnTouchListener? = null
    private var fabHintTouchAreaListener: View.OnClickListener? = null
    private val guideContentList = mutableListOf<GuideContent>()
    private var currentGuideIndex = 0
    private var savedAbacusScaleX = 1.0f
    private var savedAbacusScaleY = 1.0f
    private var savedAbacusMarginBottomDp = 0f
    private var savedBeadScaleX = 1.0f
    private var savedBeadScaleY = 1.0f
    private var savedBeadMarginTopDp = 0f
    private var savedBeadMarginBottomDp = 0f
    /** Kart açılırken kilitlenen periyot; ödül bu anahtarla eşleşmeli. */
    private var dailyQuestionSessionPeriodKey: String? = null
    /** Bu oturumdaki soru indeksi (0..2). */
    private var dailyQuestionSlotIndex: Int = 0
    private var dailyQuestionPartId: Int = -1
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
        GlobalValues.shouldShowAdOnReturn = true
        isDailyQuestionMode = arguments?.getBoolean(ARG_DAILY_MODE, false) == true
        val intervalMs = if (arguments?.containsKey(ARG_DAILY_INTERVAL_MS) == true) {
            arguments?.getLong(ARG_DAILY_INTERVAL_MS)
        } else {
            null
        }
        if (isDailyQuestionMode) {
            dailyQuestionSessionPeriodKey = arguments?.getString(ARG_DAILY_PERIOD_KEY)
                ?: DailyQuestionPeriod.currentPeriodKey()
            dailyQuestionSlotIndex = arguments?.getInt(ARG_DAILY_SLOT_INDEX, 0)?.coerceIn(0, 2) ?: 0
            dailyQuestionPartId = arguments?.getInt(ARG_DAILY_PART_ID, -1) ?: -1
        }
        
        // Initialize operations first so we can check it
        operations = arguments?.getSerializable("operations") as? List<Any> ?: emptyList()
        
        lessonItem = if (isDailyQuestionMode) {
            val hasMathOperation = operations.firstOrNull() is MathOperation
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Günlük Soru",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                isBlinding = null,
                blindingMultiplication = hasMathOperation,
                isMultiplication = hasMathOperation,
                timePeriod = intervalMs,
            )
        } else if (globalPartId == 9) {
            arguments?.getSerializable("cup_lesson_item") as? LessonItem 
                ?: LessonManager.getLessonItem(0) ?: LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kupa Modu",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    raceBusyLevel = 1,
                    raceTitle = "Kupa Zorluğu",
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
        abacusMetricsInitialized = false

        findsId()
        if (isDailyQuestionMode) {
            binding.progressBarContainer.visibility = View.GONE
            binding.hintContainer.visibility = View.GONE
            binding.fabHintTouchArea.visibility = View.GONE
            binding.rulesBookButton.visibility = View.GONE
            binding.rulesPanelButton.visibility = View.GONE
        }
        setupAskQuestionButton()
        controlButtonAnim()
        setupStartButton()
        setupQuitButton()
        setupSkipStepButton()
        setupBackPressHandler()
        rulesBookButtonClick()
        rulesPanelButtonClick()
        updateActiveRulesPanelTable()
        rulesBookVisibility()
        initializeGuideSystem()
        resetClickListener()
        blindingOrRace()
        setupAbacusSettingsPanel()
        setupAbacusController()
        setupKeyboardVisibilityListener()
        abacusModeVisibility()
        cupWay()
    }

    private fun initializeGuideSystem() {
        panelContent = binding.guidePanelInclude.panelContent
        ivGuideImage = binding.guidePanelInclude.ivGuideImage
        tvGuideText = binding.guidePanelInclude.tvGuideText
        stepDotsContainer = binding.guidePanelInclude.stepDotsContainer

        // Başlangıçta paneli gizle ki Firestore'dan cevap gelene kadar ekranda yanıp sönmesin
        panelContent.visibility = View.GONE
        setGuideNavButtonsVisibility(View.GONE)

        if (globalPartId == 6) {
            binding.skipStepButton.visibility = View.GONE
            binding.resetButton.visibility = View.GONE
        }
        if (lessonItem.isMultiplication == true) {
            binding.skipStepButton.visibility = View.GONE
        }

        val isSkipStepButtonVisible = (binding.skipStepButton.visibility == View.VISIBLE)
        
        if (isSkipStepButtonVisible) {
            val authManager = AuthManager().also { it.initialize(requireContext()) }
            authManager.checkSkipStepTutorialShown { shown ->
                if (!shown) {
                    startGuideNumber(5)
                } else {
                    startNormalGuide()
                }
            }
        } else {
            startNormalGuide()
        }
    }

    private fun startNormalGuide() {
        if (lessonItem.abacusGuideNumber == null || lessonItem.abacusGuideNumber == 5) {
            panelContent.visibility = View.GONE
            setGuideNavButtonsVisibility(View.GONE)
            return
        }
        if (lessonItem.currentStep == 1) {
            startGuideNumber(lessonItem.abacusGuideNumber!!)
        } else {
            panelContent.visibility = View.GONE
            setGuideNavButtonsVisibility(View.GONE)
        }
    }

    private fun startGuideNumber(guideNumber: Int) {
        val guideContents = SharedGuideHelper.getGuideContentsForNumber(
            guideNumber, binding.firstNumberText, binding.secondNumberText, 
            binding.kontrolButton, rulesPanelButton, rulesBookButton, binding.skipStepButton, binding.abacusModeButton
        )
        panelContent.visibility = View.GONE
        setGuideNavButtonsVisibility(View.GONE)
        if (guideContents.isNotEmpty()) {
            setGuideContents(guideContents)
            binding.overlay.visibility = View.VISIBLE
            binding.overlay.isClickable = true
            binding.overlay.isFocusable = true
            binding.overlay.alpha = 0.01f
            binding.overlay.setOnClickListener { }
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isAdded) return@postDelayed
                enableGuidePanelMode()
                showGuidePanelWithAnimation()
                if (guideNumber == 5) {
                    AuthManager().also { it.initialize(requireContext()) }.setSkipStepTutorialShown()
                }
            }, 500)
        }
    }
    private fun setupAskQuestionButton() {
        val authManager = AuthManager().also { it.initialize(requireContext()) }
        AskQuestionButtonBinder.bind(
            fragment = this,
            button = binding.askQuestionButton,
            isTeacher = authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER,
            onAllowedClick = {
                (activity as? MainActivity)?.startQuestionFlow(R.id.abacusFragmentContainer) { view }
            },
        )
    }


    private fun uploadLessonData(){
        operations = arguments?.getSerializable("operations") as? List<Any> ?: emptyList()

        // Kupa modu: sayılar zaten bundle'dan geldi, ek işlem gerekmez
        if (globalPartId == 9) return

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
        if(globalPartId == 4 || globalPartId == 5){
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
            binding.startButton.visibility = View.INVISIBLE
            controlButton.visibility = View.VISIBLE
            startTimerIfNeeded()
            showCurrentOperation()
        }
    }
    private var rulesPanelVisibleBeforeKeyboard = false

    private fun setupKeyboardVisibilityListener() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (binding.numberInput.visibility == View.VISIBLE) {
                if (imeVisible) {
                    rulesPanelVisibleBeforeKeyboard =
                        binding.rulesPanelScrollView.visibility == View.VISIBLE
                    binding.rulesPanelScrollView.visibility = View.GONE
                    rulesPanelButton.isEnabled = false
                } else {
                    if (rulesPanelVisibleBeforeKeyboard) {
                        binding.rulesPanelScrollView.visibility = View.VISIBLE
                    }
                    rulesPanelButton.isEnabled = true
                }
            }
            insets
        }
    }

    private fun blindingOrRace() {
        if (lessonItem.isBlinding == true) {
            binding.abacusLinear.visibility = View.INVISIBLE
            binding.numberInput.visibility = View.VISIBLE
            binding.abacusContainer.visibility = View.INVISIBLE

            val params = binding.rulesPanelScrollView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.height = (160 * resources.displayMetrics.density).toInt()
            params.bottomToTop = R.id.numberInput
            binding.rulesPanelScrollView.layoutParams = params
            params.bottomMargin = (5 * resources.displayMetrics.density).toInt()
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
        rulesPanelButton = binding.rulesPanelButton
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

        binding.abacusModeButton.setOnClickListener {
            abacusModeButtonClick()
        }
    }
    private fun abacusModeButtonClick() {
        if (!isAbacusSettingsPanelOpen) {
            // Panelı aç: 0 → 25. frame
            binding.abacusModeButton.setMinAndMaxFrame(0, 25)
            binding.abacusModeButton.playAnimation()
            showAbacusSettingsPanel()
        } else {
            // Panelı kapat: 25 → 45. frame
            binding.abacusModeButton.setMinAndMaxFrame(25, 45)
            binding.abacusModeButton.playAnimation()
            hideAbacusSettingsPanel()
        }
    }

    private fun abacusModeVisibility(){
        if(globalPartId in listOf(7,8,9)){
            binding.abacusModeButton.visibility= View.VISIBLE
            if(lessonItem.isBlinding == true){
                binding.abacusModeButton.visibility= View.GONE

            }
        }
        else{
            binding.abacusModeButton.visibility = View.GONE
        }
    }

    private fun showAbacusSettingsPanel() {
        resetAbacus()
        abacusController.setEnabled(false)
        isAbacusSettingsPanelOpen = true
        binding.abacusSettingsPanel.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun hideAbacusSettingsPanel() {
        abacusController.setEnabled(true)
        isAbacusSettingsPanelOpen = false
        val offset = binding.abacusSettingsPanel.width.toFloat() + (100f * resources.displayMetrics.density)
        binding.abacusSettingsPanel.animate()
            .translationX(offset)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .start()
    }

    private fun setAllBeadsScale(scaleX: Float?, scaleY: Float?) {
        val allViews = getAllChildren(binding.abacusContainer)
        for (v in allViews) {
            if (v.id != View.NO_ID) {
                try {
                    val idName = resources.getResourceEntryName(v.id)
                    if (idName.contains("bead") && v is ImageView) {
                        scaleX?.let { v.scaleX = it }
                        scaleY?.let { v.scaleY = it }
                    }
                } catch (e: Exception) {
                    // Ignore views without resource names
                }
            }
        }
    }

    private fun setAllBeadsMargins(marginTopDp: Float?, marginBottomDp: Float?) {
        val density = resources.displayMetrics.density
        val allViews = getAllChildren(binding.abacusContainer)
        for (v in allViews) {
            if (v.id != View.NO_ID) {
                try {
                    val idName = resources.getResourceEntryName(v.id)
                    if (idName.contains("bead") && v is ImageView) {
                        if (marginTopDp != null && idName.contains("top")) {
                            val params = v.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                            params?.topMargin = (marginTopDp * density).toInt()
                            v.layoutParams = params
                        }
                        if (marginBottomDp != null && idName.contains("bottom")) {
                            val params = v.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                            val baseMarginDp = when {
                                idName.contains("bottom1") -> 82f
                                idName.contains("bottom2") -> 58f
                                idName.contains("bottom3") -> 34f
                                idName.contains("bottom4") -> 10f
                                else -> 0f
                            }
                            params?.bottomMargin = ((baseMarginDp + marginBottomDp) * density).toInt()
                            v.layoutParams = params
                        }
                    }
                } catch (e: Exception) {
                    // Ignore views without resource names
                }
            }
        }
    }


    private fun getAllChildren(v: View): List<View> {
        val visited = ArrayList<View>()
        val unvisited = ArrayList<View>()
        unvisited.add(v)
        while (unvisited.isNotEmpty()) {
            val child = unvisited.removeAt(0)
            visited.add(child)
            if (child is ViewGroup) {
                for (i in 0 until child.childCount) {
                    unvisited.add(child.getChildAt(i))
                }
            }
        }
        return visited
    }


    private fun loadAbacusSettings() {
        val prefs = requireContext().getSharedPreferences("AbacusUISettings", android.content.Context.MODE_PRIVATE)
        savedAbacusScaleX = prefs.getFloat("savedAbacusScaleX", 1.0f)
        savedAbacusScaleY = prefs.getFloat("savedAbacusScaleY", 1.0f)
        savedAbacusMarginBottomDp = prefs.getFloat("savedAbacusMarginBottomDp", 0f)
        savedBeadScaleX = prefs.getFloat("savedBeadScaleX", 1.0f)
        savedBeadScaleY = prefs.getFloat("savedBeadScaleY", 1.0f)
        savedBeadMarginTopDp = prefs.getFloat("savedBeadMarginTopDp", 0f)
        savedBeadMarginBottomDp = prefs.getFloat("savedBeadMarginBottomDp", 0f)
    }

    private fun saveAbacusSettings() {
        val prefs = requireContext().getSharedPreferences("AbacusUISettings", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("savedAbacusScaleX", savedAbacusScaleX)
            putFloat("savedAbacusScaleY", savedAbacusScaleY)
            putFloat("savedAbacusMarginBottomDp", savedAbacusMarginBottomDp)
            putFloat("savedBeadScaleX", savedBeadScaleX)
            putFloat("savedBeadScaleY", savedBeadScaleY)
            putFloat("savedBeadMarginTopDp", savedBeadMarginTopDp)
            putFloat("savedBeadMarginBottomDp", savedBeadMarginBottomDp)
            apply()
        }
    }

    private fun clearAbacusSettings() {
        val prefs = requireContext().getSharedPreferences("AbacusUISettings", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        savedAbacusScaleX = 1.0f
        savedAbacusScaleY = 1.0f
        savedAbacusMarginBottomDp = 0f
        savedBeadScaleX = 1.0f
        savedBeadScaleY = 1.0f
        savedBeadMarginTopDp = 0f
        savedBeadMarginBottomDp = 0f
    }

    private fun setupAbacusSettingsPanel() {
        loadAbacusSettings()
        val density = resources.displayMetrics.density

        binding.abacusContainer.scaleX = savedAbacusScaleX
        binding.abacusContainer.scaleY = savedAbacusScaleY
        val initParams = binding.abacusContainer.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (savedAbacusMarginBottomDp > 0f) {
            initParams?.bottomMargin = (savedAbacusMarginBottomDp * density).toInt()
            binding.abacusContainer.layoutParams = initParams
        }

        if (savedAbacusMarginBottomDp == 0f) {
            val params = binding.abacusContainer.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            savedAbacusMarginBottomDp = (params?.bottomMargin ?: 0) / density
        }

        // Abacus boyutları (0.5x ile 2.0x aralığında ayarlansın, progress 0-100)
        binding.seekBarScaleX.progress = ((savedAbacusScaleX - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)
        binding.seekBarScaleY.progress = ((savedAbacusScaleY - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)
        binding.seekBarMarginBottom.progress = savedAbacusMarginBottomDp.toInt().coerceIn(0, 100)
        
        // Boncuk boyutları (0.5x ile 2.0x aralığında ayarlansın, progress 0-100)
        binding.seekBarBeadScaleX.progress = ((savedBeadScaleX - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)
        binding.seekBarBeadScaleY.progress = ((savedBeadScaleY - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)
        
        binding.seekBarBeadMarginTop.progress = savedBeadMarginTopDp.toInt().coerceIn(0, 30)
        binding.seekBarBeadMarginBottom.progress = savedBeadMarginBottomDp.toInt().coerceIn(0, 50)

        // Panel açıldığında kaydedilmiş boncuk boyutlarını ve boşluklarını uygula
        setAllBeadsScale(savedBeadScaleX, savedBeadScaleY)
        setAllBeadsMargins(savedBeadMarginTopDp, savedBeadMarginBottomDp)

        binding.seekBarScaleX.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 0.5f + (progress / 100f) * 1.5f
                binding.abacusContainer.scaleX = scale
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        binding.seekBarScaleY.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 0.5f + (progress / 100f) * 1.5f
                binding.abacusContainer.scaleY = scale
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        binding.seekBarMarginBottom.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val params = binding.abacusContainer.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                params?.bottomMargin = (progress * density).toInt()
                binding.abacusContainer.layoutParams = params
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        binding.seekBarBeadScaleX.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                // 0-100 -> 0.5 - 2.0
                val scale = 0.5f + (progress / 100f) * 1.5f
                setAllBeadsScale(scale, null)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        binding.seekBarBeadScaleY.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 0.5f + (progress / 100f) * 1.5f
                setAllBeadsScale(null, scale)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
        
        binding.seekBarBeadMarginTop.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                setAllBeadsMargins(progress.toFloat(), null)
                binding.abacusContainer.post {
                    if (::abacusController.isInitialized) {
                        val topOffsetPx = progress * density
                        val bottomOffsetPx = binding.seekBarBeadMarginBottom.progress * density
                        abacusController.setBeadMarginOffsets(bottomOffsetPx, topOffsetPx)
                        abacusController.computeMovementDistancesFromLayout(force = true)
                    }
                }
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        binding.seekBarBeadMarginBottom.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                setAllBeadsMargins(null, progress.toFloat())
                binding.abacusContainer.post {
                    if (::abacusController.isInitialized) {
                        val topOffsetPx = binding.seekBarBeadMarginTop.progress * density
                        val bottomOffsetPx = progress * density
                        abacusController.setBeadMarginOffsets(bottomOffsetPx, topOffsetPx)
                        abacusController.computeMovementDistancesFromLayout(force = true)
                    }
                }
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
        
        binding.abacusSettingsResetButton.setOnClickListener {
            // Default progress for 1.0f on 0.5-2.0 scale is 33
            val defaultProgress = ((1.0f - 0.5f) * (100f / 1.5f)).toInt()
            
            // Abacus default: 1.0f, 1.0f
            binding.seekBarScaleX.progress = defaultProgress
            binding.seekBarScaleY.progress = defaultProgress
            
            // Margin bottom default
            val defaultMarginPx = resources.getDimensionPixelSize(R.dimen.tutorial_abacus_linear_margin_bottom)
            binding.seekBarMarginBottom.progress = (defaultMarginPx / density).toInt()
            
            // Bead default: 1.0f, 1.0f
            binding.seekBarBeadScaleX.progress = defaultProgress
            binding.seekBarBeadScaleY.progress = defaultProgress
            
            binding.seekBarBeadMarginTop.progress = 0
            binding.seekBarBeadMarginBottom.progress = 0
            
            binding.abacusContainer.scaleX = 1.0f
            binding.abacusContainer.scaleY = 1.0f
            
            val params = binding.abacusContainer.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            params?.bottomMargin = defaultMarginPx
            binding.abacusContainer.layoutParams = params
            
            setAllBeadsScale(1.0f, 1.0f)
            setAllBeadsMargins(0f, 0f)
            
            clearAbacusSettings()
            
            binding.abacusContainer.post {
                if (::abacusController.isInitialized) {
                    abacusController.setBeadMarginOffsets(0f, 0f)
                    abacusController.computeMovementDistancesFromLayout(force = true)
                }
            }
        }

        binding.abacusSettingsSaveButton.setOnClickListener {
            // Mevcut değerleri kaydet
            savedAbacusScaleX = binding.abacusContainer.scaleX
            savedAbacusScaleY = binding.abacusContainer.scaleY
            val params = binding.abacusContainer.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            savedAbacusMarginBottomDp = ((params?.bottomMargin ?: 0) / density)
            
            savedBeadScaleX = 0.5f + (binding.seekBarBeadScaleX.progress / 100f) * 1.5f
            savedBeadScaleY = 0.5f + (binding.seekBarBeadScaleY.progress / 100f) * 1.5f
            
            savedBeadMarginTopDp = binding.seekBarBeadMarginTop.progress.toFloat()
            savedBeadMarginBottomDp = binding.seekBarBeadMarginBottom.progress.toFloat()
            
            // Offset'leri controller'a kaydet
            if (::abacusController.isInitialized) {
                abacusController.setBeadMarginOffsets(
                    savedBeadMarginBottomDp * density,
                    savedBeadMarginTopDp * density
                )
                abacusController.computeMovementDistancesFromLayout(force = true)
                abacusController.forceRecaptureInitialPositions()
            }
            
            saveAbacusSettings()
            
            hideAbacusSettingsPanel()
            binding.abacusModeButton.setMinAndMaxFrame(25, 45)
            binding.abacusModeButton.playAnimation()
        }
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
        closeRulesPanelIfOpen()
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

    private fun cupWay(){
        if(globalPartId == 9){
            binding.progressBarContainer.visibility = View.GONE
            binding.hintContainer.visibility = View.GONE
            binding.fabHintTouchArea.visibility = View.GONE
        }
        if (globalPartId in listOf(4, 5, 6, 7, 8, 9)) {
            binding.hintContainer.visibility = View.GONE
            binding.fabHintTouchArea.visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun controlButtonAnim() {
        controlButtonListener = View.OnTouchListener { v, event ->
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
                        
                        if (globalPartId == 9) {
                            val winDelta = lessonItem.cupWinDelta ?: 10
                            val lossDelta = lessonItem.cupLossDelta ?: 30
                            val delta = if (isCorrect) winDelta else -lossDelta
                            if (lessonItem.isMultiplication == true && lessonItem.isBlinding == true) {
                                GlobalValues.pendingBlindingImpactCupDelta = delta
                            } else if (lessonItem.isMultiplication == true) {
                                GlobalValues.pendingImpactCupDelta = delta
                            } else if (lessonItem.isExtraction == true && lessonItem.isBlinding == true) {
                                GlobalValues.pendingBlindingExtractionCupDelta = delta
                            } else if (lessonItem.isExtraction == true) {
                                GlobalValues.pendingExtractionCupDelta = delta
                            } else if (lessonItem.isBlinding == true) {
                                GlobalValues.pendingBlindingCupDelta = delta
                            } else {
                                GlobalValues.pendingCupDelta = delta
                            }
                            BadgePrecalcHelper.executeCupDeltaUpdateAsync(lessonItem)
                            showResultPanel(isCorrect)
                            controlNumber = 0
                            binding.numberInput.setText("")
                            return@OnTouchListener true
                        }
                        
                        showResultPanel(isCorrect)
                        controlNumber = 0
                        binding.numberInput.setText("")
                    }
                    true
                }

                else -> false
            }
        }
        controlButton.setOnTouchListener(controlButtonListener)
    }
    //Sorudaki sayıları gösterir
    private fun showCurrentOperation() {
        if (operations.isEmpty()) {
            Log.e("AbacusFragment", "Operations list is empty")
            return
        }

        if (lessonItem.blindingMultiplication == true || lessonItem.isMultiplication == true) {
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
    private fun setupSkipStepButton() {
        binding.skipStepButton.setMinFrame(10) // Animasyonun 10. frame'den başlamasını sağla
        
        binding.skipStepButton.setOnClickListener {
            if (!isShowingSequence) return@setOnClickListener
            binding.skipStepButton.playAnimation()
            handler.removeCallbacks(showNextNumberRunnable)
            handler.removeCallbacks(sequenceRevealRunnable)
            onSequenceRevealStep()
        }
    }

    private fun setupQuitButton() {
        binding.quitButton.setOnClickListener {
            if (isDailyQuestionMode && lessonStarted) {
                handleDailyQuestionWrongAnswer()
                return@setOnClickListener
            }
            if (globalPartId == 9 && lessonStarted) {
                val lossDelta = lessonItem.cupLossDelta ?: 30
                if (lessonItem.isMultiplication == true && lessonItem.isBlinding == true) {
                    GlobalValues.pendingBlindingImpactCupDelta = -lossDelta
                } else if (lessonItem.isMultiplication == true) {
                    GlobalValues.pendingImpactCupDelta = -lossDelta
                } else if (lessonItem.isExtraction == true && lessonItem.isBlinding == true) {
                    GlobalValues.pendingBlindingExtractionCupDelta = -lossDelta
                } else if (lessonItem.isExtraction == true) {
                    GlobalValues.pendingExtractionCupDelta = -lossDelta
                } else if (lessonItem.isBlinding == true) {
                    GlobalValues.pendingBlindingCupDelta = -lossDelta
                } else {
                    GlobalValues.pendingCupDelta = -lossDelta
                }
                BadgePrecalcHelper.executeCupDeltaUpdateAsync(lessonItem)
                (activity as? MainActivity)?.getEnergyManager()?.useEnergy(1)
            }
            closeFragment()
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if ((activity as? MainActivity)?.isTeacherSelectingQuestionToSend() == true) return

                    val rulesFragment = childFragmentManager.findFragmentByTag("rules_fragment")
                    if (rulesFragment is RulesFragment && rulesFragment.isVisible) {
                        rulesFragment.closeWithAnimation()
                        return
                    }
                    if (isDailyQuestionMode && lessonStarted) {
                        handleDailyQuestionWrongAnswer()
                        return
                    }
                    if (globalPartId == 9 && lessonStarted) {
                        val lossDelta = lessonItem.cupLossDelta ?: 30
                        if (lessonItem.isMultiplication == true && lessonItem.isBlinding == true) {
                            GlobalValues.pendingBlindingImpactCupDelta = -lossDelta
                        } else if (lessonItem.isMultiplication == true) {
                            GlobalValues.pendingImpactCupDelta = -lossDelta
                        } else if (lessonItem.isExtraction == true && lessonItem.isBlinding == true) {
                            GlobalValues.pendingBlindingExtractionCupDelta = -lossDelta
                        } else if (lessonItem.isExtraction == true) {
                            GlobalValues.pendingExtractionCupDelta = -lossDelta
                        } else if (lessonItem.isBlinding == true) {
                            GlobalValues.pendingBlindingCupDelta = -lossDelta
                        } else {
                            GlobalValues.pendingCupDelta = -lossDelta
                        }
                        BadgePrecalcHelper.executeCupDeltaUpdateAsync(lessonItem)
                        (activity as? MainActivity)?.getEnergyManager()?.useEnergy(1)
                    }
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
        if (globalPartId == 9) {
            parentFragmentManager.setFragmentResult("cupModeResult", android.os.Bundle())
            parentFragmentManager.popBackStack()
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
    private fun rulesBookButtonClick() {
        rulesBookButton.setOnClickListener {
            openRulesBook()
        }
    }

    private fun rulesPanelButtonClick() {
        rulesPanelButton.setOnClickListener {
            onRulesPanelButtonClicked()
        }
    }

    private fun onRulesPanelButtonClicked() {
        updateActiveRulesPanelTable()
        if (usesRulesTablePicker() && !hasRulesPanelContentToShow()) {
            closeHintIfVisible()
            openRulesBook()
            return
        }
        toggleRulesPanelTable()
    }

    private fun hasRulesPanelContentToShow(): Boolean {
        if (activeRulesPanelTable == RulesPanelTableType.NONE) return false
        if (activeRulesPanelTable == RulesPanelTableType.MULTIPLICATION && selectedMultiplicationDigit == null) {
            return false
        }
        return true
    }

    private fun isRulesPanelVisible(): Boolean =
        binding.rulesPanelScrollView.visibility == View.VISIBLE

    private fun closeRulesPanelIfOpen() {
        if (isRulesPanelVisible()) {
            hideAllRulesPanelTables()
        }
    }

    private fun closeHintIfVisible() {
        if (isHintVisible) {
            hideHint()
        }
    }

    private fun updateActiveRulesPanelTable() {
        if (usesRulesTablePicker()) return
        val index = lessonItem.mapFragmentIndex ?: return
        activeRulesPanelTable = when {
            index > 28 && globalPartId == 1 -> RulesPanelTableType.BEAD
            index > 22 && globalPartId == 1 -> RulesPanelTableType.TEN
            index > 16 && globalPartId == 1 -> RulesPanelTableType.TEN_FIVE
            index >= 12 && globalPartId == 1 -> RulesPanelTableType.FIVE
            index > 10 && globalPartId == 2 -> RulesPanelTableType.TEN_EXTRACTION
            index > 5 && globalPartId == 2 -> RulesPanelTableType.FIVE
            else -> RulesPanelTableType.NONE
        }
    }

    private fun applyRulesTableSelection(selection: RulesFragment.RulesTableSelection) {
        when (selection) {
            RulesFragment.RulesTableSelection.FIVE -> activeRulesPanelTable = RulesPanelTableType.FIVE
            RulesFragment.RulesTableSelection.EXTRACTION_FIVE -> activeRulesPanelTable = RulesPanelTableType.FIVE
            RulesFragment.RulesTableSelection.TEN_FIVE -> activeRulesPanelTable = RulesPanelTableType.TEN_FIVE
            RulesFragment.RulesTableSelection.TEN -> activeRulesPanelTable = RulesPanelTableType.TEN
            RulesFragment.RulesTableSelection.TEN_EXTRACTION -> activeRulesPanelTable = RulesPanelTableType.TEN_EXTRACTION
            RulesFragment.RulesTableSelection.BEAD -> activeRulesPanelTable = RulesPanelTableType.BEAD
            RulesFragment.RulesTableSelection.BEAD_EXTRACTION -> activeRulesPanelTable = RulesPanelTableType.BEAD_EXTRACTION
            is RulesFragment.RulesTableSelection.MULTIPLICATION -> {
                activeRulesPanelTable = RulesPanelTableType.MULTIPLICATION
                selectedMultiplicationDigit = selection.digit
            }
        }
        showActiveRulesPanelTable()
    }

    private fun ensureMultiplicationInSlot(digit: Int) {
        val slot = binding.rulesPanelMultiplicationSlot
        if (inflatedMultiplicationDigit != digit) {
            slot.removeAllViews()
            layoutInflater.inflate(multiplicationLayoutForDigit(digit), slot, true)
            inflatedMultiplicationDigit = digit
        }
    }

    private fun hideAllRulesPanelTables() {
        binding.rulesPanelScrollView.visibility = View.GONE
        binding.fiveRuleTableLinearLayout.visibility = View.GONE
        binding.tenRuleFiveTableLayout.visibility = View.GONE
        binding.tenRuleTableLinearLayout.visibility = View.GONE
        binding.BeadRuleTable.visibility = View.GONE
        binding.BeadRuleExtractionTable.visibility = View.GONE
        binding.tenRuleExtractionTableLayout.visibility = View.GONE
        binding.rulesPanelMultiplicationSlot.visibility = View.GONE
    }

    private fun applyRulesPanelFiveText() {
        val index = lessonItem.mapFragmentIndex ?: return
        if ((globalPartId == 2 && index > 5) || globalPartId == 5) {
            binding.fiveText.text = "Çıkarılacak Sayı"
            binding.fiveRuleDescriptionText.text = "5 gider. Kardeş gelir."
        } else {
            binding.fiveText.text = "Eklenecek sayı"
            binding.fiveRuleDescriptionText.text = "5 gelir. Kardeş gider."
        }
    }

    private fun showActiveRulesPanelTable() {
        closeHintIfVisible()
        hideAllRulesPanelTables()
        val activeTable = when (activeRulesPanelTable) {
            RulesPanelTableType.FIVE -> {
                applyRulesPanelFiveText()
                binding.fiveRuleTableLinearLayout
            }
            RulesPanelTableType.TEN_FIVE -> binding.tenRuleFiveTableLayout
            RulesPanelTableType.TEN -> binding.tenRuleTableLinearLayout
            RulesPanelTableType.BEAD -> binding.BeadRuleTable
            RulesPanelTableType.BEAD_EXTRACTION -> binding.BeadRuleExtractionTable
            RulesPanelTableType.TEN_EXTRACTION -> binding.tenRuleExtractionTableLayout
            RulesPanelTableType.MULTIPLICATION -> {
                val digit = selectedMultiplicationDigit
                if (digit != null) {
                    ensureMultiplicationInSlot(digit)
                    binding.rulesPanelMultiplicationSlot
                } else {
                    null
                }
            }
            RulesPanelTableType.NONE -> null
        }
        activeTable?.let {
            binding.rulesPanelScrollView.visibility = View.VISIBLE
            it.visibility = View.VISIBLE
            binding.rulesPanelScrollView.bringToFront()
        }
    }

    private fun toggleRulesPanelTable() {
        if (activeRulesPanelTable == RulesPanelTableType.NONE) return
        if (binding.rulesPanelScrollView.visibility == View.VISIBLE) {
            hideAllRulesPanelTables()
        } else {
            showActiveRulesPanelTable()
        }
    }

    private fun rulesBookVisibility() {
        if (usesRulesTablePicker()) {
            rulesBookButton.visibility = View.VISIBLE
            rulesPanelButton.visibility = View.VISIBLE
            return
        }
    }

    private fun openRulesBook() {
        childFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_down, 0)
            .replace(R.id.rulesFragmentContainer, RulesFragment(), "rules_fragment")
            .commit()
        rulesBookSetup()
        changeRulesTableText()
    }

    private fun rulesBookSetup() {
        childFragmentManager.executePendingTransactions()
        val rulesFragment = childFragmentManager.findFragmentByTag("rules_fragment") as? RulesFragment


        if (usesRulesTablePicker()) {
            rulesBookButton.visibility = View.VISIBLE
            rulesPanelButton.visibility = View.VISIBLE
            rulesFragment?.setActiveRulesContentSection(RulesFragment.RulesContentSection.ADDITION)
            rulesFragment?.updateFiveRuleTableVisibility(View.VISIBLE)
            rulesFragment?.updateTenRuleTableVisibility(View.VISIBLE)
            rulesFragment?.updateTenRuleFiveTableVisibility(View.VISIBLE)
            rulesFragment?.updateBeadRuleTableVisibility(View.VISIBLE)
            rulesFragment?.updateMultiplicationTablesVisibility(View.GONE)
            rulesFragment?.setRulesTablePickerEnabled(true)
            rulesFragment?.setOnRulesTableSelectedListener { selection ->
                applyRulesTableSelection(selection)
            }
        } else {
            rulesFragment?.setRulesTablePickerEnabled(false)
            rulesFragment?.setOnRulesTableSelectedListener(null)
        }
        applyRulesDividersVisibility(rulesFragment)
        if (!usesRulesTablePicker()) {
            updateActiveRulesPanelTable()
        }
        if(globalPartId == 5 || globalPartId == 8 || (isDailyQuestionMode && dailyQuestionPartId == 2)){
            rulesFragment?.setActiveRulesContentSection(RulesFragment.RulesContentSection.EXTRACTION)
            rulesFragment?.updateExtractionFiveRuleTableVisibility(View.VISIBLE)
            rulesFragment?.updateTenRuleExtractionTableLayout(View.VISIBLE)
            rulesFragment?.updateBeadRuleExtractionTableLayout(View.VISIBLE)
        }
        if(globalPartId == 6 || (isDailyQuestionMode && (dailyQuestionPartId == 1 || dailyQuestionPartId == 3))){
            rulesFragment?.setActiveRulesContentSection(RulesFragment.RulesContentSection.ADDITION)
            rulesFragment?.updateFiveRuleTableVisibility(View.VISIBLE)
            rulesFragment?.updateTenRuleFiveTableVisibility(View.VISIBLE)
            rulesFragment?.updateTenRuleTableVisibility(View.VISIBLE)
            rulesFragment?.updateBeadRuleTableVisibility(View.VISIBLE)
            rulesFragment?.updateRulesDividerVisibilities(View.VISIBLE, View.VISIBLE, View.VISIBLE)
            rulesFragment?.updateMultiplicationTablesVisibility(View.VISIBLE)
            Log.d("kol","work")
        }
    }

    private fun applyRulesDividersVisibility(rulesFragment: RulesFragment?) {
        if (usesRulesTablePicker()) {
            rulesFragment?.updateRulesDividerVisibilities(View.VISIBLE, View.VISIBLE, View.VISIBLE)
            return
        }
        val index = lessonItem.mapFragmentIndex ?: return
        val (view1, view2, view3) = when {
            globalPartId == 1 && index > 11 -> Triple(
                if (index > 16) View.VISIBLE else View.INVISIBLE,
                if (index > 22) View.VISIBLE else View.INVISIBLE,
                if (index > 28) View.VISIBLE else View.INVISIBLE
            )
            globalPartId == 2 && index in 11..15 -> Triple(
                View.VISIBLE,
                View.VISIBLE,
                View.INVISIBLE
            )
            globalPartId == 2 && index in 16..20 -> Triple(
                View.VISIBLE,
                View.VISIBLE,
                View.VISIBLE
            )
            else -> Triple(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE)
        }
        rulesFragment?.updateRulesDividerVisibilities(view1, view2, view3)
    }

    private fun changeRulesTableText() {
        val rulesFragment = childFragmentManager.findFragmentByTag("rules_fragment") as? RulesFragment
        rulesFragment?.applyRulesTableHeaderTexts(globalPartId == 2)
    }


    private fun playSound(soundResId: Int) {
        val prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return
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

            controlButton.isClickable = false
            controlButton.isFocusable = false
            controlButton.setOnTouchListener(null)

            isResultPanelAnimating = true

            correctPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    isResultPanelAnimating = false
                }
                .start()

            // Geri tuşu dinleyicisi
            correctPanel.findViewById<Button>(R.id.continueButton)
                .setOnTouchListener { v, event ->
                    if (isResultPanelAnimating) return@setOnTouchListener true
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
                            if (globalPartId == 9) {
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
                                        finishLessonAfterLastQuestion()
                                    }
                                    .start()
                                return@setOnTouchListener true
                            }
                            if (isResultPanelAnimating) return@setOnTouchListener true
                            isResultPanelAnimating = true
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

                            binding.root.postDelayed({
                                if (isAdded) {
                                    controlButton.isClickable = true
                                    controlButton.isFocusable = true
                                    controlButtonListener?.let { listener ->
                                        controlButton.setOnTouchListener(listener)
                                    }
                                }
                            }, 200)

                            correctPanel.animate()
                                .translationY(correctPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    correctPanel.visibility = View.GONE
                                    isResultPanelAnimating = false
                                }
                                .start()
                            if(lessonItem.isBlinding == null){
                                resetAbacus()
                            }
                            if (currentIndex <= operations.size - 1) {
                                showCurrentOperation()
                            } else {
                                showQuestionPanel()
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

            controlButton.isClickable = false
            controlButton.isFocusable = false
            controlButton.setOnTouchListener(null)

            isResultPanelAnimating = true

            incorrectPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    isResultPanelAnimating = false
                }
                .start()

            incorrectPanel.findViewById<Button>(R.id.okayButton)
                .setOnTouchListener { v, event ->
                    if (isResultPanelAnimating) return@setOnTouchListener true
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
                            if (globalPartId == 9) {
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
                                          showQuestionPanel(isWrongDailyQuestion = true)
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
                                          showQuestionPanel(isWrongRaceLesson = true)
                                      }
                                    .start()
                                return@setOnTouchListener true
                            }
                            if (isResultPanelAnimating) return@setOnTouchListener true
                            isResultPanelAnimating = true
                            currentIndex++
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(BounceInterpolator())
                                .start()
                            binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE

                            binding.root.postDelayed({
                                if (isAdded) {
                                    controlButton.isClickable = true
                                    controlButton.isFocusable = true
                                    controlButtonListener?.let { listener ->
                                        controlButton.setOnTouchListener(listener)
                                    }
                                }
                            }, 700)

                            incorrectPanel.animate()
                                .translationY(incorrectPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    incorrectPanel.visibility = View.GONE
                                    isResultPanelAnimating = false
                                }
                                .start()
                            if(lessonItem.isBlinding == null){
                                resetAbacus()
                            }
                            if (currentIndex <= operations.size - 1) {
                                showCurrentOperation()
                            } else {
                                showQuestionPanel()
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
        val density = resources.displayMetrics.density
        abacusController.setBeadMarginOffsets(
            savedBeadMarginBottomDp * density,
            savedBeadMarginTopDp * density,
        )
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
                if (lessonItem.blindingMultiplication == true || lessonItem.isMultiplication == true) {
                    answerNumber = currentOperation.firstNumber?.times(currentOperation.secondNumber!!) ?: 0
                    
                    if (lessonItem.isMultiplication == true && lessonItem.isBlinding != true) {
                        controlNumber = abacusController.getCurrentValue()
                        if (controlNumber == answerNumber) {
                            controlNumber = 0
                            return true
                        } else {
                            fillIncorrectPanelAnswers()
                            controlNumber = 0
                            return false
                        }
                    } else {
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

    private fun showQuestionPanel(
        isWrongDailyQuestion: Boolean = false,
        isWrongRaceLesson: Boolean = false
    ) {
        releaseLaunchTouchBlocker()
        try {
            if (::runnable.isInitialized) {
                handler.removeCallbacks(runnable)
            }
        } catch (e: Exception) {}

        val successRate = if (totalQuestions > 0) {
            (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }
        val dersPuani = (successRate * 5f).toInt()
        val worstCupTime = com.example.app.LessonManager.getLessonItem(mapFragmentStepIndex)?.worstCupTime ?: 0

        val fragment = QuestionPanelFragment.newInstance(
            correctAnswers = correctAnswer,
            totalQuestions = totalQuestions,
            successRate = successRate,
            dersPuani = dersPuani,
            globalPartId = globalPartId,
            mapFragmentIndex = mapFragmentStepIndex,
            lessonType = lessonItem.type,
            currentTime = currentTime,
            worstCupTime = worstCupTime
        )

        parentFragmentManager.setFragmentResultListener("questionPanelResult", viewLifecycleOwner) { _, _ ->
            if (isWrongDailyQuestion) {
                handleDailyQuestionWrongAnswer()
            } else if (isWrongRaceLesson) {
                showLessonResultFalse()
            } else {
                finishLessonAfterLastQuestion()
            }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            .add(R.id.abacusFragmentContainer, fragment)
            .hide(this)
            .commit()
    }

    private fun finishLessonAfterLastQuestion() {
        when {
            isDailyQuestionMode -> handleDailyQuestionLessonComplete()
            // Kupa modu: doğru tamamlandı — +5 delta bırak ve kapat
            globalPartId == 9 -> {
                // Delta ve Firestore yazma işlemi controlButtonListener'da optimistic UI olarak zaten yapıldı.
                closeFragment()
            }
            isRacePanelLesson() || (lessonItem.type == LessonItem.TYPE_CHEST && globalPartId !in setOf(4, 5)) -> {
                if (lessonItem.raceBusyLevel == 0) {
                    closeFragment()
                    return
                }
                val successRate = if (totalQuestions > 0) (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100 else 0f
                val (carpan, toplamPuan) = calculateChestScore(successRate, currentTime)
                if (toplamPuan >= 500) {
                    showChestResult(carpan, toplamPuan)
                } else {
                    showLessonResultFalse(true)
                }
            }
            globalPartId in setOf(4, 5) -> {
                // Başarı oranını hesapla
                val successRate = if (totalQuestions > 0) (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100 else 0f

                // Eğer başarı %50'den düşükse başarısız ekranına, değilse sandığa gönder
                if (successRate < 50f) {
                    showLessonResultFalse()
                } else {
                    showLessonResult()
                }
            }
            else -> showLessonResult()
        }
    }

    private fun lessonResultArgs(isChestFailure: Boolean = false): Bundle {
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
            putBoolean("isChestFailure", isChestFailure)
        }
    }

    private fun showLessonResultFalse(isChestFailure: Boolean = false) {
        // Kupa modu: yanlış yapıldı — -5 delta bırak ve kapat
        if (globalPartId == 9) {
            // Delta ve Firestore yazma işlemi controlButtonListener'da optimistic UI olarak zaten yapıldı.
            (activity as? MainActivity)?.getEnergyManager()?.useEnergy(1)
            closeFragment()
            return
        }
        if (lessonItem.raceBusyLevel == 0) {
            closeFragment()
            return
        }
        val lessonResultFalse = LessonResultFalse()
        lessonResultFalse.arguments = lessonResultArgs(isChestFailure)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .replace(R.id.abacusFragmentContainer, lessonResultFalse)
            .commit()
    }

    private fun showChestResult(carpan: Float, toplamPuan: Int) {
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
        val worstCupTime = resolveWorstCupTimeFallback()

        val args = Bundle().apply {
            putInt("correctAnswers", correctAnswer)
            putInt("totalQuestions", totalQuestions)
            putFloat("successRate", successRate)
            putString("time", currentTime)
            putInt("dersPuani", dersPuani)
            putInt("worstCupTime", worstCupTime)
            putFloat("carpan", carpan)
            putInt("toplamPuan", toplamPuan)
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

    private fun parseTimeToSeconds(value: String): Int {
        val parts = value.split(":").map { it.trim() }
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: 0
                val seconds = parts[1].toIntOrNull() ?: 0
                minutes * 60 + seconds
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: 0
                val minutes = parts[1].toIntOrNull() ?: 0
                val seconds = parts[2].toIntOrNull() ?: 0
                hours * 3600 + minutes * 60 + seconds
            }
            else -> 0
        }
    }

    private fun resolveWorstCupTimeFallback(): Int {
        val fromCurrent = LessonManager.getLessonItem(mapFragmentStepIndex)?.worstCupTime
        if (fromCurrent != null && fromCurrent > 0) return fromCurrent

        val fromTemplate = GlobalLessonData.createLessonItems(GlobalLessonData.globalPartId)
            .getOrNull(mapFragmentStepIndex)
            ?.worstCupTime
        if (fromTemplate != null && fromTemplate > 0) return fromTemplate

        return 240
    }

    private fun calculateChestScore(successRate: Float, timeStr: String): Pair<Float, Int> {
        val dersPuani = (successRate * 5f).toInt()
        val targetTimeSec = parseTimeToSeconds(timeStr)
        val worstCupTime = resolveWorstCupTimeFallback()

        val carpan = if (worstCupTime <= 0) 1f else {
            val rawCarpan = 4f - ((targetTimeSec * 3f) / worstCupTime.toFloat())
            rawCarpan.coerceAtLeast(1f)
        }
        val toplamPuan = kotlin.math.ceil(dersPuani * carpan).toInt()
        return Pair(carpan, toplamPuan)
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
        
        DailyQuestionRepository.recordQuestionResult(requireContext(), periodKey, dailyQuestionSlotIndex, isSuccess = false)
        
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
        
        DailyQuestionRepository.recordQuestionResult(requireContext(), periodKey, dailyQuestionSlotIndex, isSuccess = true)
        
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
        applyCustomization()
        if (isDailyQuestionMode) {
            binding.root.postDelayed({ releaseLaunchTouchBlocker() }, 320)
        }
    }

    override fun onPause() {
        stopLearningSessionTracking()
        super.onPause()
    }

    /**
     * Applies abacus customization preferences (frame background + bead colours).
     * Called in onResume so changes from [AbacusCustomizationFragment] take effect
     * when the user navigates back.
     */
    private fun applyCustomization() {
        if (!::binding.isInitialized) return
        binding.abacusContainer.background =
            com.example.app.abacus.AbacusFrameRenderer.buildFrameDrawable(requireContext())
        if (::abacusController.isInitialized) abacusController.refreshAll()
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


    
    /**
     * abacusGuideNumber'a göre rehber içeriklerini döndürür
     * @param guideNumber Rehber numarası
     * @return Rehber içerikleri listesi
     */
    
    
    /**
     * Rehber içeriklerini ayarlar
     * @param contents Rehber içerikleri listesi
     */
    fun setGuideContents(contents: List<GuideContent>) {
        guideContentList.clear()
        guideContentList.addAll(contents)
        currentGuideIndex = 0
        
        // Adım göstergesini oluştur
        updateStepIndicator()
        
        // İlk içeriği göster
        if (guideContentList.isNotEmpty()) {
            showGuideContent(0)
        }
    }
    
    /**
     * Adım göstergesini günceller (noktalar)
     */
    private fun updateStepIndicator() {
        val totalSteps = guideContentList.size
        
        // Nokta göstergelerini oluştur
        stepDotsContainer.removeAllViews()
        for (i in 0 until totalSteps) {
            val dotView = TextView(requireContext()).apply {
                text = if (i == currentGuideIndex) "●" else "○"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = if (i < totalSteps - 1) 8.dpToPx() else 0
                }
            }
            stepDotsContainer.addView(dotView)
        }
    }
    
    /**
     * dp değerini px'e çevirir
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    /**
     * Bir sonraki rehber içeriğini gösterir
     */
    private fun showNextGuideContent() {
        if (guideContentList.isEmpty() || panelContent.visibility != View.VISIBLE) return

        // Mevcut adımın finishBeadIds'ini kontrol et ve animasyonları çalıştır
        val currentContent = guideContentList[currentGuideIndex]
        currentContent.finishBeadIds?.forEach { beadId ->
            animateGuideBead(beadId)
        }

        // Son adıma geldiysek guide panel'i kapat ve normal ders akışına geç
        if (currentGuideIndex >= guideContentList.size - 1) {
            stopGuideTypewriter()
            // Guide panel'i kapat
            disableGuidePanelMode()
            // Panel'i sola kayarak gizle
            hideGuidePanelWithAnimation()
            setGuideNavButtonsVisibility(View.GONE)
            return
        }

        currentGuideIndex++
        showGuideContent(currentGuideIndex)
    }
    
    private fun setGuideNavButtonsVisibility(visibility: Int) {
        binding.guidePanelInclude.btnBack.visibility = visibility
        binding.guidePanelInclude.btnForward.visibility = visibility
    }

    private fun setupGuideNavButtonListeners() {
        binding.guidePanelInclude.btnBack.setOnClickListener {
            showPreviousGuideContent()
        }
        binding.guidePanelInclude.btnForward.setOnClickListener {
            val content = guideContentList.getOrNull(currentGuideIndex)
            if (content?.requiredClickTarget != null) return@setOnClickListener
            if (content?.waitForRulesTableSelection == true) return@setOnClickListener
            showNextGuideContent()
        }
    }

    /** Guide ilerlemesi yalnızca btnForward ile; overlay/panel tıklaması engellenir. */
    private fun applyGuideForwardBlockOverlay() {
        binding.overlay.setOnTouchListener(null)
        binding.overlay.setOnClickListener { }
        panelContent.setOnClickListener(null)
    }

    /** Adım 3: RulesFragment tıklanabilir, abaküs overlay ile engelli. */
    private fun applyGuideWaitForRulesSelectionOverlay() {
        binding.overlay.visibility = View.VISIBLE
        binding.overlay.isClickable = true
        binding.overlay.isFocusable = true
        binding.overlay.alpha = 0.01f
        binding.overlay.setOnTouchListener { _, _ -> true }
        panelContent.setOnClickListener(null)
        binding.overlay.bringToFront()
        binding.rulesFragmentContainer.bringToFront()
        panelContent.bringToFront()
        binding.guidePanelInclude.btnBack.bringToFront()
        binding.guidePanelInclude.btnForward.bringToFront()
    }



    private fun closeRulesBookIfOpen() {
        val rulesFragment = childFragmentManager.findFragmentByTag("rules_fragment") as? RulesFragment
        if (rulesFragment != null && rulesFragment.isVisible) {
            rulesFragment.closeWithAnimation()
        }
    }

    private fun completeGuideIfWaitingForRulesSelection() {
        val content = guideContentList.getOrNull(currentGuideIndex) ?: return
        if (!content.waitForRulesTableSelection || panelContent.visibility != View.VISIBLE) return
        showNextGuideContent()
    }

    private fun openRulesPanelTableForGuide() {
        updateActiveRulesPanelTable()
        showActiveRulesPanelTable()
        rulesPanelButton.bringToFront()
    }

    private fun handleRequiredClickTarget(content: GuideContent) {
        stopBubbleAnimation()
        content.requiredClickTarget?.elevation = originalTargetElevation
        when (content.requiredClickTarget) {
            rulesBookButton -> openRulesBook()
            rulesPanelButton -> openRulesPanelTableForGuide()
            binding.skipStepButton -> {
                if (isShowingSequence) {
                    binding.skipStepButton.playAnimation()
                    handler.removeCallbacks(showNextNumberRunnable)
                    handler.removeCallbacks(sequenceRevealRunnable)
                    onSequenceRevealStep()
                }
            }
        }
        if (content.requiredClickAdvancesGuide) {
            when (content.requiredClickTarget) {
                rulesBookButton -> rulesBookButtonClick()
                rulesPanelButton -> rulesPanelButtonClick()
                binding.skipStepButton -> setupSkipStepButton()
                else -> rulesBookButtonClick()
            }
            showNextGuideContent()
        } else {
            disableGuidePanelMode()
            hideGuidePanelWithAnimation()
            setGuideNavButtonsVisibility(View.GONE)
        }
    }

    private fun applyGuideContentOverlay(content: GuideContent) {
        if (content.requiredClickTarget != null) {
            binding.overlay.visibility = View.VISIBLE
            binding.overlay.isClickable = true
            binding.overlay.isFocusable = true
            binding.overlay.alpha = 0.01f
            content.requiredClickTarget.bringToFront()
            originalTargetElevation = content.requiredClickTarget.elevation
            content.requiredClickTarget.elevation = 15f * resources.displayMetrics.density
            binding.overlay.setOnTouchListener { _, _ -> true }
            panelContent.setOnClickListener(null)
            content.requiredClickTarget.isClickable = true
            content.requiredClickTarget.isFocusable = true
            content.requiredClickTarget.bringToFront()
            content.requiredClickTarget.setOnClickListener {
                handleRequiredClickTarget(content)
            }
        } else if (content.waitForRulesTableSelection) {
            applyGuideWaitForRulesSelectionOverlay()
        } else {
            binding.overlay.visibility = View.VISIBLE
            binding.overlay.isClickable = true
            binding.overlay.isFocusable = true
            binding.overlay.alpha = 0.01f
            applyGuideForwardBlockOverlay()
        }
    }
    
    /**
     * Bir önceki rehber içeriğini gösterir
     */
    private fun showPreviousGuideContent() {
        if (guideContentList.isEmpty() || panelContent.visibility != View.VISIBLE) return
        
        // İlk adımdaysa hiçbir şey yapma
        if (currentGuideIndex == 0) return

        // Geri gidildiğinde backBeadIds (yoksa beadIds) ile boncuk animasyonu
        // Not: finishBeadIds geri gidildiğinde çalıştırılmaz
        val currentContent = guideContentList[currentGuideIndex]
        val beadsOnBack = currentContent.backBeadIds ?: currentContent.beadIds
        beadsOnBack?.let { beadIds ->
            optimizeBeadIdsForReverse(beadIds).forEach { beadId ->
                animateGuideBead(beadId)
            }
        }

        if (currentContent.waitForRulesTableSelection) {
            closeRulesBookIfOpen()
        }

        // Bir önceki adıma git
        currentGuideIndex--
        
        // Önceki adımı göster ama beadIds'ini çalıştırma (çünkü geri dönüşte zaten ters yönde çalıştırdık)
        showGuideContentWithoutBeads(currentGuideIndex)
    }
    
    /**
     * Geri dönüş için boncuk ID'lerini optimize eder
     * Aynı rod'un alt boncuklarından sadece en yüksek numaralı olanı bırakır
     * (Çünkü en yüksek numaralı boncuk zaten tüm boncukları hareket ettirir)
     * @param beadIds Boncuk ID'leri listesi
     * @return Optimize edilmiş boncuk ID'leri listesi
     */
    private fun optimizeBeadIdsForReverse(beadIds: List<String>): List<String> {
        val optimized = mutableListOf<String>()
        val rodGroups = mutableMapOf<String, MutableList<String>>()
        
        // Boncukları rod'lara göre grupla
        beadIds.forEach { beadId ->
            when {
                beadId.startsWith("rod0BottomBead") -> {
                    rodGroups.getOrPut("rod0") { mutableListOf() }.add(beadId)
                }
                beadId.startsWith("rod1BottomBead") -> {
                    rodGroups.getOrPut("rod1") { mutableListOf() }.add(beadId)
                }
                beadId.startsWith("rod2BottomBead") -> {
                    rodGroups.getOrPut("rod2") { mutableListOf() }.add(beadId)
                }
                beadId.startsWith("rod3BottomBead") -> {
                    rodGroups.getOrPut("rod3") { mutableListOf() }.add(beadId)
                }
                beadId.startsWith("rod4BottomBead") -> {
                    rodGroups.getOrPut("rod4") { mutableListOf() }.add(beadId)
                }
                else -> {
                    // Top boncuklar veya diğer boncuklar için direkt ekle
                    optimized.add(beadId)
                }
            }
        }
        
        // Her rod için en yüksek numaralı alt boncuğu ekle
        rodGroups.forEach { (_, beads) ->
            if (beads.isNotEmpty()) {
                // En yüksek numaralı boncuğu bul (Bead4 > Bead3 > Bead2 > Bead1)
                val sortedBeads = beads.sortedByDescending { it ->
                    when {
                        it.contains("Bead4") -> 4
                        it.contains("Bead3") -> 3
                        it.contains("Bead2") -> 2
                        it.contains("Bead1") -> 1
                        else -> 0
                    }
                }
                optimized.add(sortedBeads.first())
            }
        }
        
        return optimized
    }
    
    /**
     * Guide panel aktifken tıklanabilirliği ayarlar
     */
    private fun enableGuidePanelMode() {
        // Overlay'i görünür ve tıklanabilir yap
        binding.overlay.visibility = View.VISIBLE
        binding.overlay.isClickable = true
        binding.overlay.isFocusable = true
        binding.overlay.alpha = 0.01f // Neredeyse görünmez ama tıklanabilir
        
        binding.guidePanelInclude.root.bringToFront()
        binding.guidePanelInclude.root.translationZ = 20f * resources.displayMetrics.density
        
        applyGuideForwardBlockOverlay()
        setupGuideNavButtonListeners()
        
        // Diğer view'ları tıklanamaz yap
        disableOtherViews()
    }
    
    /**
     * Guide panel aktifken diğer view'ları tıklanamaz yapar
     */
    private fun disableOtherViews() {
        // Abaküs ve tüm alt view'lerini tıklanamaz yap
        disableAllClickable(binding.abacusLinear)
        
        // Kontrol butonunu tıklanamaz yap
        binding.kontrolButton.isClickable = false
        binding.kontrolButton.isFocusable = false
        // Control button'un listener'ını geçici olarak kaldır
        binding.kontrolButton.setOnTouchListener(null)
        
        // Hint touch area'yı tıklanamaz yap
        binding.fabHintTouchArea.isClickable = false
        binding.fabHintTouchArea.isFocusable = false
        // Hint touch area'nın listener'ını geçici olarak kaldır
        binding.fabHintTouchArea.setOnClickListener(null)
        
        // Diğer butonları da tıklanamaz yap
        binding.quitButton.isClickable = false
        binding.rulesBookButton.isClickable = false
        binding.askQuestionButton.isClickable = false
        binding.askQuestionButton.isFocusable = false
        binding.askQuestionButton.isEnabled = false
        binding.abacusModeButton.isClickable = false
        binding.abacusModeButton.isFocusable = false
        binding.abacusModeButton.isEnabled = false
    }
    
    /**
     * Guide panel kapatıldığında tıklanabilirliği geri yükler
     */
    private fun disableGuidePanelMode() {
        // Overlay'i gizle
        binding.overlay.visibility = View.GONE
        binding.overlay.isClickable = false
        binding.overlay.isFocusable = false
        binding.overlay.setOnTouchListener(null)
        
        binding.guidePanelInclude.root.translationZ = 0f
        
        // Elevation değerlerini güvenli bir şekilde sıfırla
        rulesBookButton.elevation = 0f
        rulesPanelButton.elevation = 10f * resources.displayMetrics.density
        binding.skipStepButton.elevation = 10f * resources.displayMetrics.density
        binding.abacusModeButton.elevation = 10f * resources.displayMetrics.density
        binding.resetButton.elevation = 10f * resources.displayMetrics.density
        
        // Diğer view'ları tekrar tıklanabilir yap
        enableOtherViews()
    }
    
    /**
     * Diğer view'ları tekrar tıklanabilir yapar
     */
    private fun enableOtherViews() {
        // Abaküs ve tüm alt view'lerini tekrar tıklanabilir yap
        enableAllClickable(binding.abacusLinear)
        
        // Kontrol butonunu tekrar tıklanabilir yap
        binding.kontrolButton.isClickable = true
        binding.kontrolButton.isFocusable = true
        // Control button'un listener'ını geri yükle
        controlButtonListener?.let { listener ->
            binding.kontrolButton.setOnTouchListener(listener)
        }
        
        // Hint touch area'yı tekrar tıklanabilir yap
        binding.fabHintTouchArea.isClickable = true
        binding.fabHintTouchArea.isFocusable = true
        // Hint touch area'nın listener'ını geri yükle
        fabHintTouchAreaListener?.let { listener ->
            binding.fabHintTouchArea.setOnClickListener(listener)
        }
        
        // Diğer butonları da tekrar tıklanabilir yap
        binding.quitButton.isClickable = true
        binding.rulesBookButton.isClickable = true
        rulesPanelButton.isClickable = true
        rulesPanelButton.isFocusable = true
        rulesPanelButtonClick()
        binding.askQuestionButton.isClickable = true
        binding.askQuestionButton.isFocusable = true
        binding.askQuestionButton.isEnabled = true
        binding.abacusModeButton.isClickable = true
        binding.abacusModeButton.isFocusable = true
        binding.abacusModeButton.isEnabled = true
    }
    
    /**
     * Bir view ve tüm alt view'lerini tıklanamaz yapar
     */
    private fun disableAllClickable(view: View) {
        view.isClickable = false
        view.isFocusable = false
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                disableAllClickable(view.getChildAt(i))
            }
        }
    }
    
    /**
     * Bir view ve tüm alt view'lerini tıklanabilir yapar
     */
    private fun enableAllClickable(view: View) {
        view.isClickable = true
        view.isFocusable = true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                enableAllClickable(view.getChildAt(i))
            }
        }
    }
    
    /**
     * Belirli bir index'teki rehber içeriğini gösterir
     * @param index Gösterilecek içeriğin index'i
     */
    private fun showGuideContent(index: Int) {
        if (index < 0 || index >= guideContentList.size) return
        
        // Önceki baloncuk animasyonunu durdur
        stopBubbleAnimation()
        stopGuideTypewriter()
        
        val content = guideContentList[index]
        
        // ImageView ve TextView içeriğini güncelle
        ivGuideImage.setImageResource(content.imageResource)
        applyGuideTextAndSound(content)
        
        // Adım göstergesini güncelle
        updateStepIndicator()
        
        // Callback fonksiyonunu çağır
        content.onContentShown?.invoke()
        
        // Eğer bu içerik için baloncuk animasyonu hedefi varsa animasyonu başlat
        content.bubbleAnimationTarget?.let { target ->
            animateBubbleEffect(
                target,
                content.bubbleAnimationColor,
                content.bubbleAnimationTintLight,
                maxScale = content.bubbleAnimationMaxScale,
            )
        }
        
        // Eğer bu içerik için hareket ettirilecek boncuklar varsa animasyonu başlat
        content.beadIds?.forEach { beadId ->
            animateGuideBead(beadId)
        }
        
        applyGuideContentOverlay(content)
    }
    
    /**
     * Belirli bir index'teki rehber içeriğini gösterir ama boncuk animasyonlarını çalıştırmaz
     * (Geri dönüş için kullanılır)
     * @param index Gösterilecek içeriğin index'i
     */
    private fun showGuideContentWithoutBeads(index: Int) {
        if (index < 0 || index >= guideContentList.size) return
        
        // Önceki baloncuk animasyonunu durdur
        stopBubbleAnimation()
        stopGuideTypewriter()
        
        val content = guideContentList[index]
        
        // ImageView ve TextView içeriğini güncelle
        ivGuideImage.setImageResource(content.imageResource)
        applyGuideTextAndSound(content)
        
        // Adım göstergesini güncelle
        updateStepIndicator()
        
        // Callback fonksiyonunu çağır
        content.onContentShown?.invoke()
        
        // Eğer bu içerik için baloncuk animasyonu hedefi varsa animasyonu başlat
        content.bubbleAnimationTarget?.let { target ->
            animateBubbleEffect(
                target,
                content.bubbleAnimationColor,
                content.bubbleAnimationTintLight,
                maxScale = content.bubbleAnimationMaxScale,
            )
        }
        
        // Boncuk animasyonlarını çalıştırma (geri dönüş için)
        
        applyGuideContentOverlay(content)
    }
    
    /**
     * Guide içeriğinde belirtilen boncuğu hareket ettirir
     * Eğer boncuk daha önce hareket ettirildiyse ters yönde hareket ettirir
     * @param beadId Hareket ettirilecek boncuk ID'si (örn: "rod1BottomBead4", "rod0TopBead")
     */
    private fun animateGuideBead(beadId: String) {
        // New shared controller handles all rod ids (rod0..rod4).
        abacusController.animateGuideBead(beadId)
    }

    
    /**
     * Bir widget'e baloncuk animasyonu uygular (1.0 -> 1.4 -> 1.0) ve renk geçişi yapar
     * @param view Animasyon uygulanacak widget
     * @param targetColor Animasyon sırasında kullanılacak hedef renk (null ise sadece scale animasyonu)
     */
    private fun animateBubbleEffect(
        view: View,
        targetColor: Int?,
        tintLight: Int? = null,
        maxScale: Float = 1.4f,
    ) {
        // Önceki animasyonu durdur (güvenlik için)
        stopBubbleAnimation()
        
        // Önceki animasyonun scale'ini sıfırla (eğer varsa)
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        
        // Orijinal rengi al ve sakla (renk animasyonu için)
        var originalColor: Int? = null
        var isTextView = false
        val useImageTintBreath = view is ImageView && targetColor != null && tintLight != null
        
        if (targetColor != null) {
            when (view) {
                is TextView -> {
                    isTextView = true
                    originalColor = view.currentTextColor
                    originalTextColor = originalColor
                }
                is ImageView -> {
                    if (useImageTintBreath) {
                        bubbleImageTintBreathApplied = true
                        originalBackgroundTintList = view.backgroundTintList
                        view.backgroundTintList = android.content.res.ColorStateList.valueOf(targetColor)
                    } else {
                        originalColor = Color.WHITE
                    }
                }
            }
        }
        
        currentAnimatedView = view
        val colorEvaluator = ArgbEvaluator()
        
        // Scale animasyonu (maxScale parametresine göre)
        val scaleAnimator = ValueAnimator.ofFloat(1.0f, maxScale, 1.0f).apply {
            duration = 600 // 0.6 saniye
            repeatCount = ValueAnimator.INFINITE // Sonsuz tekrar
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                view.scaleX = scale
                view.scaleY = scale
                
                if (useImageTintBreath) {
                    val scaleRange = maxScale - 1.0f
                    val fraction = ((scale - 1.0f) / scaleRange).coerceIn(0f, 1f)
                    val currentColor = colorEvaluator.evaluate(fraction, targetColor, tintLight) as Int
                    view.backgroundTintList = ColorStateList.valueOf(currentColor)
                } else if (targetColor != null && originalColor != null) {
                    val scaleRange = maxScale - 1.0f
                    val fraction = ((scale - 1.0f) / scaleRange).coerceIn(0f, 1f)
                    val currentColor = colorEvaluator.evaluate(fraction, originalColor, targetColor) as Int
                    
                    if (isTextView) {
                        (view as TextView).setTextColor(currentColor)
                    } else if (view is ImageView) {
                        view.setColorFilter(currentColor)
                    }
                }
            }
            
            start()
        }
        
        currentBubbleAnimator = scaleAnimator
    }
    
    /**
     * Mevcut baloncuk animasyonunu durdurur
     */
    private fun stopBubbleAnimation() {
        currentBubbleAnimator?.let { animator ->
            if (animator.isRunning) {
                animator.cancel()
            }
            animator.removeAllUpdateListeners()
        }
        currentBubbleAnimator = null
        
        // Önceki view'in scale'ini ve rengini sıfırla
        currentAnimatedView?.let { view ->
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            
            originalTextColor?.let { originalColor ->
                when (view) {
                    is TextView -> view.setTextColor(originalColor)
                    is ImageView -> view.clearColorFilter()
                }
            }
            if (bubbleImageTintBreathApplied && view is ImageView) {
                view.backgroundTintList = originalBackgroundTintList
            }
        }
        
        currentAnimatedView = null
        originalTextColor = null
        originalImageTintList = null
        originalBackgroundTintList = null
        bubbleImageTintBreathApplied = false
    }

    private fun stopGuideTypewriter() {
        guideTypewriterRunnable?.let { tvGuideText.removeCallbacks(it) }
        guideTypewriterRunnable = null
    }

    private fun applyGuideTextAndSound(content: GuideContent) {
        if (content.useTypewriterEffect) {
            showGuideTextWithTypewriter(content.text, tvGuideText, content.typewriterSpeed)
        } else {
            tvGuideText.visibility = View.VISIBLE
            tvGuideText.text = content.text
        }
        playGuideSound(content.soundResource)
    }

    private fun playGuideSound(soundResource: Int?) {
        soundResource?.let { resourceId ->
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(requireContext(), resourceId)
                mediaPlayer?.setOnCompletionListener {
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e("AbacusFragment", "Rehber sesi çalma hatası: ${e.message}")
            }
        }
    }

    private fun showGuideTextWithTypewriter(text: String, textView: TextView, speed: Long) {
        guideTypewriterRunnable?.let { textView.removeCallbacks(it) }
        textView.visibility = View.INVISIBLE
        textView.text = text
        textView.post {
            textView.text = ""
            var currentIndex = 0
            guideTypewriterRunnable = object : Runnable {
                override fun run() {
                    if (currentIndex < text.length) {
                        textView.visibility = View.VISIBLE
                        val currentText = if (currentIndex == 0) {
                            text[0].toString()
                        } else {
                            textView.text.toString() + text[currentIndex]
                        }
                        textView.text = currentText
                        currentIndex++
                        textView.postDelayed(this, speed)
                    } else {
                        guideTypewriterRunnable = null
                    }
                }
            }
            textView.post(guideTypewriterRunnable!!)
        }
    }
    
    /**
     * Rehber panelini gösterir veya gizler
     * @param visible true ise gösterir, false ise gizler
     */
    fun setGuidePanelVisibility(visible: Boolean) {
        if (visible) {
            showGuidePanelWithAnimation()
        } else {
            hideGuidePanelWithAnimation()
        }
    }
    
    /**
     * Guide panel'i soldan kayarak gösterir
     */
    private fun showGuidePanelWithAnimation() {
        // Önce panel'i INVISIBLE yaparak genişliğini ölçebilmek için görünür yap (ama görünmez)
        panelContent.visibility = View.INVISIBLE
        panelContent.alpha = 1f
        
        // Panel'in genişliğini ölçmek için layout'u zorla
        panelContent.post {
            // Panel'in genişliğini al
            val panelWidth = panelContent.width
            // Eğer genişlik hala 0 ise, parent'ın genişliğini kullan
            val widthToUse = if (panelWidth > 0) panelWidth else panelContent.rootView.width
            
            // Panel'i ekranın soluna taşı (genişliği kadar sola)
            panelContent.translationX = -widthToUse.toFloat()
            
            // Şimdi panel'i görünür yap
            panelContent.visibility = View.VISIBLE
            
            // Soldan sağa kayarak göster
            panelContent.animate()
                .translationX(0f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    setGuideNavButtonsVisibility(View.VISIBLE)
                }
                .start()
        }
    }
    
    /**
     * Guide panel'i sola kayarak gizler
     */
    private fun hideGuidePanelWithAnimation() {
        // Panel'in genişliğini al
        val panelWidth = panelContent.width
        if (panelWidth == 0) {
            // Eğer genişlik henüz ölçülmemişse, direkt gizle
            panelContent.visibility = View.GONE
            return
        }
        
        // Sola kayarak gizle
        panelContent.animate()
            .translationX(-panelWidth.toFloat())
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                panelContent.visibility = View.GONE
                panelContent.translationX = 0f // Translation'ı sıfırla
            }
            .start()
    }
    
    /**
     * Rehber içeriğini sıfırlar (ilk içeriğe döner)
     */
    fun resetGuideContent() {
        currentGuideIndex = 0
        if (guideContentList.isNotEmpty()) {
            showGuideContent(0)
        }
    }
    
}
