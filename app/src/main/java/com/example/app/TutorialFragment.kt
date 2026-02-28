package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.GlobalValues.lessonStep
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.databinding.FragmentTutorialBinding
import com.example.app.model.BeadAnimation
import com.example.app.model.LessonItem

class TutorialFragment(private val tutorialNumber: Int = 1) : Fragment() {
    private lateinit var currentTutorialSteps: List<TutorialStep>
    private var lessonItem: LessonItem? = null
    private lateinit var controlButton: View
    private lateinit var correctPanel:View
    private lateinit var incorrectPanel:View
    private lateinit var questionText:View
    private var controlNumber=0
    private var answerNumber: Int?= null
    private var isAnimating2 = false
    private var isWritingAnswerNumber = false // writeAnswerNumber() çalışıyor mu kontrolü için
    private var controlButtonListener: View.OnTouchListener? = null // Control button listener'ını saklamak için

    // Seçenek paneli
    private lateinit var optionsPanel: View
    private lateinit var optionsRecyclerView: RecyclerView
    private lateinit var optionsTitleText: TextView
    private lateinit var optionsCheckButton: View
    private var optionsAdapter: TutorialOptionsAdapter? = null
    // Paneli gecikmeli göstermek için kullanılan runnable referansı
    private var optionsPanelShowRunnable: Runnable? = null
    // Panel açıkken back ve "eğitimi atla" butonlarını kilitlemek için
    private var optionsInteractionLocked: Boolean = false
    
    // Ses çalma için MediaPlayer
    private var mediaPlayer: MediaPlayer? = null
    
    // Typewriter effect için
    private var typewriterRunnable: Runnable? = null


    private var oneIsUp = false
    private var twoIsUp = false
    private var threeIsUp = false
    private var fourIsUp = false
    private var topIsDown = false

    // 2. sütun için boolean değişkenler
    private var rod1OneIsUp = false
    private var rod1TwoIsUp = false
    private var rod1ThreeIsUp = false
    private var rod1FourIsUp = false
    private var rod1TopIsDown = false

    // 3. sütun için boolean değişkenler
    private var rod2OneIsUp = false
    private var rod2TwoIsUp = false
    private var rod2ThreeIsUp = false
    private var rod2FourIsUp = false
    private var rod2TopIsDown = false

    // 4. sütun için boolean değişkenler
    private var rod3OneIsUp = false
    private var rod3TwoIsUp = false
    private var rod3ThreeIsUp = false
    private var rod3FourIsUp = false
    private var rod3TopIsDown = false

    // 5. sütun için boolean değişkenler
    private var rod4OneIsUp = false
    private var rod4TwoIsUp = false
    private var rod4ThreeIsUp = false
    private var rod4FourIsUp = false
    private var rod4TopIsDown = false

    private lateinit var rod0BottomBead4: ImageView
    private lateinit var rod0BottomBead3: ImageView
    private lateinit var rod0BottomBead2: ImageView
    private lateinit var rod0BottomBead1: ImageView
    private lateinit var rod0TopBead: ImageView

    // 2. sütun için boncuklar
    private lateinit var rod1BottomBead4: ImageView
    private lateinit var rod1BottomBead3: ImageView
    private lateinit var rod1BottomBead2: ImageView
    private lateinit var rod1BottomBead1: ImageView
    private lateinit var rod1TopBead: ImageView

    // 3. sütun için boncuklar
    private lateinit var rod2BottomBead4: ImageView
    private lateinit var rod2BottomBead3: ImageView
    private lateinit var rod2BottomBead2: ImageView
    private lateinit var rod2BottomBead1: ImageView
    private lateinit var rod2TopBead: ImageView

    // 4. sütun için boncuklar
    private lateinit var rod3BottomBead4: ImageView
    private lateinit var rod3BottomBead3: ImageView
    private lateinit var rod3BottomBead2: ImageView
    private lateinit var rod3BottomBead1: ImageView
    private lateinit var rod3TopBead: ImageView

    // 5. sütun için boncuklar
    private lateinit var rod4BottomBead4: ImageView
    private lateinit var rod4BottomBead3: ImageView
    private lateinit var rod4BottomBead2: ImageView
    private lateinit var rod4BottomBead1: ImageView
    private lateinit var rod4TopBead: ImageView

    private var currentAnimations: MutableList<BeadAnimation> = mutableListOf()
    private val widgetAnimators = mutableListOf<ValueAnimator>()
    private lateinit var binding: FragmentTutorialBinding
    private var currentStep = 0
    private var backOrFront = true
    private lateinit var focusView: View
    private var requestPanelView: View? = null
    private var originalRootClickListener: View.OnClickListener? = null
    private var tutorialSteps: List<TutorialStep> = emptyList()
    private var tutorialSteps100: List<TutorialStep> = emptyList()
    private var tutorialSteps101: List<TutorialStep> = emptyList()
    private var tutorialSteps102: List<TutorialStep> = emptyList()
    private var tutorialSteps103: List<TutorialStep> = emptyList()
    private var tutorialSteps2: List<TutorialStep> = emptyList()
    private var tutorialSteps3: List<TutorialStep> = emptyList()
    private var tutorialSteps9999: List<TutorialStep> = emptyList()
    private var tutorialSteps4: List<TutorialStep> = emptyList()
    private var tutorialSteps5: List<TutorialStep> = emptyList()
    private var tutorialSteps6: List<TutorialStep> = emptyList()
    private var tutorialSteps104: List<TutorialStep> = emptyList()
    private var tutorialSteps7: List<TutorialStep> = emptyList()
    private var tutorialSteps105: List<TutorialStep> = emptyList()
    private var tutorialSteps8: List<TutorialStep> = emptyList()
    private var tutorialSteps9: List<TutorialStep> = emptyList()
    private var tutorialSteps10: List<TutorialStep> = emptyList()
    private var tutorialSteps11: List<TutorialStep> = emptyList()
    private var tutorialSteps12: List<TutorialStep> = emptyList()
    private var tutorialSteps13: List<TutorialStep> = emptyList()
    private var tutorialSteps14: List<TutorialStep> = emptyList()
    private var tutorialSteps15: List<TutorialStep> = emptyList()
    private var tutorialSteps16: List<TutorialStep> = emptyList()
    private var tutorialSteps17: List<TutorialStep> = emptyList()
    private var tutorialSteps18: List<TutorialStep> = emptyList()
    private var tutorialSteps19: List<TutorialStep> = emptyList()
    private var tutorialSteps20: List<TutorialStep> = emptyList()
    private var tutorialSteps21: List<TutorialStep> = emptyList()
    private var tutorialSteps22: List<TutorialStep> = emptyList()
    private var tutorialSteps23: List<TutorialStep> = emptyList()
    private var tutorialSteps24: List<TutorialStep> = emptyList()
    private var tutorialSteps25: List<TutorialStep> = emptyList()
    private var tutorialSteps26: List<TutorialStep> = emptyList()
    private var sizeHistory = mutableListOf<Pair<Int, Int>>()

    // Tutorial için gerekli state'ler
    private var tutorialControlNumber: Int = 0
    private var tutorialRod4FourIsUp: Boolean = false

    companion object {
        fun newInstance(tutorialNumber: Int): TutorialFragment {
            return TutorialFragment(tutorialNumber)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bazı açılış senaryolarında (özellikle login sonrası) GlobalLessonData henüz dolmamış olabiliyor.
        // Burada güvenli bir fallback ile crash'i engelleyip doğru item'ı bulmaya çalışıyoruz.
        LessonManager.ensureInitialized(requireContext(), GlobalLessonData.globalPartId)

        val byPosition = LessonManager.getLessonItem(mapFragmentStepIndex)
        val byTutorialNumber = GlobalLessonData.lessonItems.firstOrNull {
            it.type == LessonItem.TYPE_LESSON && it.tutorialNumber == tutorialNumber
        }
        val firstLesson = GlobalLessonData.lessonItems.firstOrNull { it.type == LessonItem.TYPE_LESSON }

        lessonItem = byPosition ?: byTutorialNumber ?: firstLesson ?: LessonItem(
            type = LessonItem.TYPE_LESSON,
            title = "Tutorial",
            offset = 0,
            isCompleted = false,
            stepCount = 1,
            currentStep = 1
        )

        if (byPosition == null) {
            Log.w(
                "TutorialFragment",
                "lessonItem not found by mapFragmentStepIndex=$mapFragmentStepIndex, fallback used (tutorialNumber=$tutorialNumber)"
            )
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* geri tuşu ile hiçbir şey olmasın */ }
        })
        findIDs()
        focusView = binding.focusView
        controlButton = binding.kontrolButton
        correctPanel = binding.correctPanel
        incorrectPanel = binding.incorrectPanel
        questionText = binding.questionText

        // Seçenek paneli kurulumu
        optionsPanel = binding.optionsPanel
        optionsRecyclerView = binding.optionsRecyclerView
        optionsTitleText = binding.optionsTitleText
        optionsCheckButton = binding.optionsCheckButton
        optionsAdapter = TutorialOptionsAdapter()
        optionsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        optionsRecyclerView.adapter = optionsAdapter
        optionsCheckButton.setOnClickListener { checkOptionsAnswer() }
        optionsPanel.visibility = View.GONE
        createTutorialSteps()
        currentTutorialSteps = when (tutorialNumber) {
            1 -> tutorialSteps
            100 -> tutorialSteps100
            101 -> tutorialSteps101
            2 -> tutorialSteps2
            102 -> tutorialSteps102
            103 -> tutorialSteps103
            3 -> tutorialSteps3
            9999 -> tutorialSteps9999
            4 -> tutorialSteps4
            5 -> tutorialSteps5
            6 -> tutorialSteps6
            104 -> tutorialSteps104
            7 -> tutorialSteps7
            105 -> tutorialSteps105
            8 -> tutorialSteps8
            9 -> tutorialSteps9
            10 -> tutorialSteps10
            11 -> tutorialSteps11
            12 -> tutorialSteps12
            13 -> tutorialSteps13
            14 -> tutorialSteps14
            15 -> tutorialSteps15
            16 -> tutorialSteps16
            17 -> tutorialSteps17
            18 -> tutorialSteps18
            19 -> tutorialSteps19
            20 -> tutorialSteps20
            21 -> tutorialSteps21
            22 -> tutorialSteps22
            23 -> tutorialSteps23
            24 -> tutorialSteps24
            25 -> tutorialSteps25
            26 -> tutorialSteps26
            else -> tutorialSteps
        }
        setupTutorial()
        setupBackButton()
        setupQuitButton()
        setupInfoRequest()
        tutorialSkipSetup()
        // Tutorial 24 için özel kontrol
        if (tutorialNumber == 24 || tutorialNumber == 25 || tutorialNumber == 26) {
            binding.abacusLinear.visibility = View.INVISIBLE
        }
        if(tutorialNumber == 1){
            binding.quitButton.visibility = View.INVISIBLE
            GlobalValues.currentTutorialNumber = 1  // ChestFragment / LessonResultFalse'ta tek sefer login için
        }
    }

    private fun setupTutorial() {
        if(currentStep == currentTutorialSteps.size){
            return
        }
        // İlk adımı göster
        showStep(currentStep)

        // Ekrana tıklama ile ilerleme
        binding.root.setOnClickListener {
            //tıklandıktan sonra index'lerin devam etmemesi için buraya || currentStep == tutorialStep.size
            if (isAnyAnimationRunning()) {
                return@setOnClickListener
            }
            if (typewriterRunnable != null) {
                // Animasyonu durdur ve metni tamamen göster
                typewriterRunnable?.let {
                    binding.tutorialText.removeCallbacks(it)
                }
                typewriterRunnable = null

                // Metni tamamen göster
                val currentStep = getCurrentStep()
                binding.tutorialText.text = currentStep.text
                binding.tutorialText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                return@setOnClickListener
            }

            // Eğer bu adımda seçenekler varsa, ilerleme sadece doğru seçenekten sonra \"Devam et\" butonu ile olacak
            if (!getCurrentStep().options.isNullOrEmpty()) {
                return@setOnClickListener
            }
            //ekrana tıkladıktan sonra sıradaki index'in nextStepAvailable değeri false ise index artışı kontol ile
            //gerçekleşecek


            if (currentStep < currentTutorialSteps.size - 1 && getCurrentStep().nextStepAvailable) {
                currentStep++
                getCurrentStep().onStepComplete?.invoke()
                backOrFront = true
                showStep(currentStep)
                if(getPlusIndexCurrentStep(-1).nextStepAbacusReset == true){
                    resetAbacus()



                }
            }
            else{
                backOrFront = true
                //showStep(currentStep) her tıkladığımızda aynı adımı tekrar gösteriyordu. Gösterme bra dedim
        }
        }
    }
    //mevcut adımdaki TutorialStep'i verir
    private fun getCurrentStep(): TutorialStep {
        return currentTutorialSteps[currentStep]
    }
    private fun getPlusIndexCurrentStep(index:Int): TutorialStep {
        return currentTutorialSteps[currentStep+index]
    }

    private fun showStep(position: Int, skipAnimations: Boolean = false) {
        val step = currentTutorialSteps[position]
        
        // Her adım başında topMargin ve bottomMargin'i sıfırla (önceki adımlardan kalan değerleri temizle)
        val params = focusView.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = 0
        params.bottomMargin = 0
        focusView.layoutParams = params
        
        // Typewriter effect kullanılıp kullanılmayacağını kontrol et
        if (step.useTypewriterEffect) {
            showTextWithTypewriter(step.text, binding.tutorialText, step.typewriterSpeed)
        } else {
            binding.tutorialText.text = step.text
        }
        
        binding.questionText.text = step.questionText
        binding.questionText.visibility = step.questionTextVisibility
        
        // Ses dosyasını çal
        playSound(step.soundResource)
        if (step.questionText != null) {

            // Renklendirme varsa uygula
            step.questionTextColorPositions?.let { colorPositions ->
                setTextWithColoredPositions(
                    binding.questionText,
                    step.questionText,
                    colorPositions
                )
            }
        }
        when (tutorialNumber) {
            3 -> binding.fiveRuleTable.visibility = step.rulesPanelVisibility
            4 -> binding.fiveRuleTable.visibility = step.rulesPanelVisibility
            5 -> binding.tenRuleTable.visibility = step.rulesPanelVisibility
            6 -> binding.tenRuleTable.visibility = step.rulesPanelVisibility
            7 -> binding.tenRuleTableLinearLayout.visibility = step.rulesPanelVisibility
            8 -> binding.BeadRuleTable.visibility = step.rulesPanelVisibility
            9 -> binding.BeadRuleTable.visibility = step.rulesPanelVisibility
            11 -> binding.extractionFiveRuleTable.visibility = step.rulesPanelVisibility
            12 -> binding.tenRuleExtractionTableLayout.visibility = step.rulesPanelVisibility
            14 -> binding.tenRuleExtractionTableLayout.visibility = step.rulesPanelVisibility
            105 ->binding.tenRuleTableLinearLayout.visibility = step.rulesPanelVisibility
            else -> View.GONE
        }

        // Seçenek paneli yönetimi tamamen updateOptionsPanelForStep içinde yapılır
        updateOptionsPanelForStep(step)
        answerNumber=step.answerNumber
        setupBeads()
        if(getCurrentStep().abacusClickable){
            controlButton.visibility = View.VISIBLE
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
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(400)
                            .setInterpolator(BounceInterpolator())
                            .start()

                        // Tıklama işlemini gerçekleştir
                        showResultPanel()
                        controlNumber = 0
                        true
                    }

                    else -> false
                }
            }
            controlButton.setOnTouchListener(controlButtonListener)
        }
        step.widgetOperations?.let { operations ->
            applyWidgetOperations(operations.map { it() })
        }
        // Widget görünürlüklerini ayarla
        step.widgetVisibilityMap?.let { visibilityMap ->
            visibilityMap.forEach { (widgetId, visibility) ->
                binding.root.findViewById<View>(widgetId)?.visibility = visibility
            }
        }
        //tutorialStep bittiyse lessonStep değerine sahip Abacus'ü yükler
        if (position == currentTutorialSteps.size - 1) {
            val operations = MapFragment.getLessonOperations(lessonStep)
            val abacusFragment = AbacusFragment()
            val blindingLessonFragment = BlindingLessonFragment()
            val bundle = Bundle()
            bundle.putSerializable("lessonItem", lessonItem) // item Serializable olmalı!
            abacusFragment.arguments = bundle
            if(lessonItem!!.isBlinding == true){
                devametFragment(blindingLessonFragment)
            }else{
                devametFragment(abacusFragment)
            }
        }

        currentAnimations.clear()

        if(backOrFront) {
            step.onStep?.invoke(focusView)
            // Animasyonları atla eğer skipAnimations true ise
            if (!skipAnimations) {
                step.animation?.let { animations ->
                    // Yeni animasyonları listeye ekle
                    currentAnimations.addAll(animations)
                    // Animasyonları çalıştır
                    animations.forEach { it.animate() }
                }
            }
        }
        else {
            // Geri gidiyoruz, position+1'e erişmeden önce bounds kontrolü yap
            // Animasyonları atla eğer skipAnimations true ise
            if (!skipAnimations) {
                if (position + 1 < currentTutorialSteps.size) {
                    currentTutorialSteps[position+1].animation?.forEach { originalAnimation ->
                        val reversedType = when(originalAnimation.getAnimationType()) {
                            1 -> 2
                            2 -> 1
                            3 -> 4
                            4 -> 3
                            else -> originalAnimation.getAnimationType()
                        }
                        val newAnimation = BeadAnimation(this, originalAnimation.getBeadId(), reversedType)
                        currentAnimations.add(newAnimation)
                        newAnimation.animate()
                    }
                }
            }
            currentTutorialSteps[position].widgetOperations?.forEach { opLambda ->
                val op = opLambda()
                if (op is WidgetOperation.AnimateMargin) {
                    applyWidgetOperations(listOf(op))
                }
            }
            if (sizeHistory.size > 0 && position + 1 < currentTutorialSteps.size && currentTutorialSteps[position+1].onStep != null) {
                val (prevWidth, prevHeight) = sizeHistory[sizeHistory.size - 1]
                val animateSizeOp = WidgetOperation.AnimateSize(
                    view = focusView,
                    fromWidth = focusView.width,
                    toWidth = prevWidth,
                    fromHeight = focusView.height,
                    toHeight = prevHeight,
                    duration = 400
                )
                applyWidgetOperations(listOf(animateSizeOp))
                sizeHistory.removeAt(sizeHistory.size - 1)
            }
            val nextStep = currentTutorialSteps.getOrNull(position + 1)

            val hasChangeVisibility = nextStep?.widgetOperations
                ?.map { it() }
                ?.any { it is WidgetOperation.ChangeVisibility } == true

            if (hasChangeVisibility) {
                // Eğer bir sonraki adımda VISIBLE visibility değişikliği varsa, geri adımda GONE yap
                val hasVisibleOp = nextStep?.widgetOperations
                    ?.map { it() }
                    ?.any { it is WidgetOperation.ChangeVisibility && it.visibility == View.VISIBLE } == true

                if (hasVisibleOp) {
                    applyWidgetOperations(listOf(WidgetOperation.ChangeVisibility(focusView, View.GONE)))
                }

                // Eğer bir sonraki adımda GONE visibility değişikliği varsa, geri adımda VISIBLE yap
                val hasGoneOp = nextStep?.widgetOperations
                    ?.map { it() }
                    ?.any { it is WidgetOperation.ChangeVisibility && it.visibility == View.GONE } == true

                if (hasGoneOp) {
                    applyWidgetOperations(listOf(WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)))
                }
            }
        }
    }

    private fun setupQuitButton(){
        binding.quitButton.setOnClickListener{
            closeFragment()
        }
    }
    
    private fun setupInfoRequest() {
        binding.infoRequest.setOnClickListener {
            // Panel açıksa kapat, kapalıysa aç
            if (requestPanelView != null && requestPanelView?.visibility == View.VISIBLE) {
                hideRequestPanel(requestPanelView!!)
            } else {
                val requestText = getCurrentStep().requestText
                if (requestText != null && requestText.isNotEmpty()) {
                    showRequestPanel(requestText)
                }
            }
        }
    }
    
    private fun showRequestPanel(text: String) {
        // Eğer panel zaten açıksa, önce kapat
        if (requestPanelView != null && requestPanelView?.visibility == View.VISIBLE) {
            return
        }
        
        // Panel'i root layout'a ekle
        val panelView = LayoutInflater.from(requireContext())
            .inflate(R.layout.request_panel, binding.root as ViewGroup, false)
        
        val requestTextView = panelView.findViewById<TextView>(R.id.requestText)
        requestTextView.text = text
        
        // Panel'in kendisine tıklandığında kapanmaması için
        panelView.setOnClickListener {
            // Panel'in kendisine tıklandığında hiçbir şey yapma
        }
        
        // Panel'i root layout'a ekle
        (binding.root as ViewGroup).addView(panelView)
        
        // Panel view'ını sakla
        requestPanelView = panelView
        
        // Panel'in boyutunu ölçmek için post kullan
        panelView.post {
            // Panel'i başlangıçta görünmez yap ve solda konumlandır
            panelView.visibility = View.VISIBLE
            panelView.alpha = 0f
            val screenWidth = resources.displayMetrics.widthPixels
            panelView.translationX = -screenWidth.toFloat()
            
            // Overlay'i görünür yap
            binding.overlay.visibility = View.VISIBLE
            binding.overlay.alpha = 0f
            
            // Overlay'e tıklanınca panel'i kapat
            binding.overlay.setOnClickListener {
                hideRequestPanel(panelView)
            }
            
            // Root view'a yeni click listener ekle (panel açıkken herhangi bir yere tıklandığında kapat)
            val rootClickListener = View.OnClickListener { view ->
                // Panel'in kendisine veya içindeki view'lara tıklanmadıysa kapat
                if (view != panelView && !isViewInsidePanel(view, panelView)) {
                    hideRequestPanel(panelView)
                }
            }
            binding.root.setOnClickListener(rootClickListener)
            
            // Animasyonları başlat
            binding.overlay.animate()
                .alpha(0.5f)
                .setDuration(200)
                .start()
            
            panelView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }
    
    private fun isViewInsidePanel(view: View, panelView: View): Boolean {
        var parent = view.parent
        while (parent != null && parent is View) {
            if (parent == panelView) {
                return true
            }
            parent = (parent as View).parent
        }
        return false
    }
    
    private fun hideRequestPanel(panelView: View) {
        binding.overlay.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.overlay.visibility = View.GONE
                }
            })
            .start()
        
        val screenWidth = resources.displayMetrics.widthPixels
        panelView.animate()
            .alpha(0f)
            .translationX(-screenWidth.toFloat())
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    (binding.root as ViewGroup).removeView(panelView)
                    requestPanelView = null
                    
                    // Orijinal root click listener'ı geri yükle
                    restoreOriginalRootClickListener()
                }
            })
            .start()
    }
    
    private fun restoreOriginalRootClickListener() {
        // setupTutorial'daki root click listener'ı geri yükle
        binding.root.setOnClickListener {
            //tıklandıktan sonra index'lerin devam etmemesi için buraya || currentStep == tutorialStep.size
            if (isAnyAnimationRunning()) {
                return@setOnClickListener
            }
            if (typewriterRunnable != null) {
                // Animasyonu durdur ve metni tamamen göster
                typewriterRunnable?.let {
                    binding.tutorialText.removeCallbacks(it)
                }
                typewriterRunnable = null

                // Metni tamamen göster
                val currentStep = getCurrentStep()
                binding.tutorialText.text = currentStep.text
                binding.tutorialText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                return@setOnClickListener
            }
            //ekrana tıkladıktan sonra sıradaki index'in nextStepAvailable değeri false ise index artışı kontol ile
            //gerçekleşecek

            if (currentStep < currentTutorialSteps.size - 1 && getCurrentStep().nextStepAvailable) {
                currentStep++
                getCurrentStep().onStepComplete?.invoke()
                backOrFront = true
                showStep(currentStep)
            }
            else{
                backOrFront = true
            }
        }
    }
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            // Seçenek paneli açık ve kilitliyken geri butonu çalışmasın
            if (optionsInteractionLocked && optionsPanel.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            // Herhangi bir animasyon devam ediyorsa geri gitmeyi engelle
            if (isAnyAnimationRunning()) {
                return@setOnClickListener
            }
            // writeAnswerNumber() çalışıyorsa geri gitmeyi engelle
            if (isWritingAnswerNumber) {
                return@setOnClickListener
            }
            if(binding.skipTutorialButton.visibility == View.INVISIBLE && lessonItem?.tutorialNumber != 1){
                val screenWidth = resources.displayMetrics.widthPixels
                binding.skipTutorialButton.translationX = screenWidth.toFloat()
                binding.skipTutorialButton.visibility = View.VISIBLE
                binding.skipTutorialButton.animate()
                    .translationX(0f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            if (currentStep > 0) {
                disableAllClickable(binding.abacusLinear)
                currentStep--
                backOrFront = false

                if(binding.abacusLinear.visibility == View.GONE && tutorialNumber<24){
                    Log.d("libya1","work")
                    binding.abacusLinear.visibility = View.VISIBLE
                }
                if(binding.abacusLinear.visibility == View.GONE && tutorialNumber>99){
                    Log.d("libya2","work")
                    binding.abacusLinear.visibility = View.VISIBLE
                }
                if(getCurrentStep().backAnswerNumber != null){
                    Log.d("libya4","work")
                    writeAnswerNumber(getCurrentStep().backAnswerNumber!!)
                }
                if(getCurrentStep().abacusReset==true){
                    Log.d("libya5","work")
                    resetAbacus()
                }
                if(getPlusIndexCurrentStep(1).resetAndWaith == true){
                    Log.d("libya6","work")
                    resetAbacus()
                    // Ekrana tıklanmasını engellemek için overlay'i görünür yap
                    binding.overlay.visibility = View.VISIBLE
                    binding.overlay.alpha = 0.01f // Neredeyse görünmez ama tıklanabilir
                    binding.overlay.isClickable = true
                    binding.overlay.isFocusable = true
                    // Animasyonların tamamlanması için 0.8 saniye bekle
                    Handler(Looper.getMainLooper()).postDelayed({
                        showStep(currentStep)
                        hideDevamButtonIfVisible()
                        // Overlay'i kapat
                        binding.overlay.visibility = View.GONE
                        binding.overlay.isClickable = false
                        binding.overlay.isFocusable = false
                    }, 800)
                } else {
                    Log.d("currentValue2",currentStep.toString())
                    showStep(currentStep)
                }
                hideDevamButtonIfVisible()

            } else {
                //closeFragment()
            }
        }
    }
    private fun hideDevamButtonIfVisible() {
        if (binding.devamButton.visibility != View.VISIBLE) return
        val screenHeight = resources.displayMetrics.heightPixels
        binding.devamButton.animate()
            .translationY(screenHeight.toFloat())
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.devamButton.visibility = View.GONE
                binding.devamButton.translationY = 0f
            }
            .start()
    }
    private fun updateBeadForDigit(
        digit: Int,
        beadViews: List<ImageView>,
        beadStates: MutableList<Boolean>
    ) {
        // Her boncuk için hedef durumu belirle
        val targetStates = listOf(
            digit >= 1,  // 1. boncuk
            digit >= 2,  // 2. boncuk  
            digit >= 3,  // 3. boncuk
            digit >= 4   // 4. boncuk
        )
        
        // Her boncuk için durum değişikliğini kontrol et ve animasyon yap
        for (i in 0..3) {
            val currentState = beadStates[i]
            val targetState = targetStates[i]
            
            if (currentState != targetState) {
                if (targetState) {
                    // False'dan true'ya geçiyor - yukarı animasyon
                    animateBeadsUp(beadViews[i])
                    updateBeadAppearance(beadViews[i], true)
                } else {
                    // True'dan false'ya geçiyor - aşağı animasyon
                    animateBeadsDown(beadViews[i])
                    updateBeadAppearance(beadViews[i], false)
                }
                beadStates[i] = targetState
            }
        }
    }

    private fun writeAnswerNumber(number:Int){
        // writeAnswerNumber() başladığında flag'i true yap
        isWritingAnswerNumber = true
        
        val numberStr = number.toString().padStart(5, '0')

        var tenThousands = numberStr[0].toString().toInt()    // On binler basamağı
        var thousands = numberStr[1].toString().toInt()    // Binler basamağı
        var hundreds = numberStr[2].toString().toInt()    // Yüzler basamağı
        var tens = numberStr[3].toString().toInt()    // Onlar basamağı
        var ones = numberStr[4].toString().toInt()    // Birler basamağı


        // Birler basamağı kontrolleri
        if(ones < 5 && rod4TopIsDown){
            animateBeadUp(rod4TopBead)
            rod4TopIsDown = false
            updateBeadAppearance(rod4TopBead, false)
        }
        if (ones >= 5) {
            animateBeadDown(rod4TopBead)
            ones -= 5
            rod4TopIsDown = true
            updateBeadAppearance(rod4TopBead, true)

        }
            // Birler basamağı için boncukları güncelle
            val rod4BeadViews = listOf(rod4BottomBead1, rod4BottomBead2, rod4BottomBead3, rod4BottomBead4)
            val rod4BeadStates = mutableListOf(rod4OneIsUp, rod4TwoIsUp, rod4ThreeIsUp, rod4FourIsUp)
            updateBeadForDigit(ones, rod4BeadViews, rod4BeadStates)
            
            // Boolean değişkenleri güncelle
            rod4OneIsUp = rod4BeadStates[0]
            rod4TwoIsUp = rod4BeadStates[1]
            rod4ThreeIsUp = rod4BeadStates[2]
            rod4FourIsUp = rod4BeadStates[3]

        // Onlar basamağı kontrolleri
        if(tens < 5 && rod3TopIsDown){
            animateBeadUp(rod3TopBead)
            rod3TopIsDown = false
            updateBeadAppearance(rod3TopBead, false)
        }
        if (tens >= 5) {
            animateBeadDown(rod3TopBead)
            rod3TopIsDown = true
            updateBeadAppearance(rod3TopBead, true)
            tens -= 5
        }
        // Onlar basamağı için boncukları güncelle
        val rod3BeadViews = listOf(rod3BottomBead1, rod3BottomBead2, rod3BottomBead3, rod3BottomBead4)
        val rod3BeadStates = mutableListOf(rod3OneIsUp, rod3TwoIsUp, rod3ThreeIsUp, rod3FourIsUp)
        updateBeadForDigit(tens, rod3BeadViews, rod3BeadStates)
        
        // Boolean değişkenleri güncelle
        rod3OneIsUp = rod3BeadStates[0]
        rod3TwoIsUp = rod3BeadStates[1]
        rod3ThreeIsUp = rod3BeadStates[2]
        rod3FourIsUp = rod3BeadStates[3]
        // Yüzler basamağı kontrolleri
        if(hundreds < 5 && rod2TopIsDown){
            animateBeadUp(rod2TopBead)
            rod2TopIsDown = false
            updateBeadAppearance(rod2TopBead, false)
        }
        if (hundreds >= 5) {
            animateBeadDown(rod2TopBead)
            rod2TopIsDown = true
            updateBeadAppearance(rod2TopBead, true)
            hundreds -= 5
        }
        // Yüzler basamağı için boncukları güncelle
        val rod2BeadViews = listOf(rod2BottomBead1, rod2BottomBead2, rod2BottomBead3, rod2BottomBead4)
        val rod2BeadStates = mutableListOf(rod2OneIsUp, rod2TwoIsUp, rod2ThreeIsUp, rod2FourIsUp)
        updateBeadForDigit(hundreds, rod2BeadViews, rod2BeadStates)
        
        // Boolean değişkenleri güncelle
        rod2OneIsUp = rod2BeadStates[0]
        rod2TwoIsUp = rod2BeadStates[1]
        rod2ThreeIsUp = rod2BeadStates[2]
        rod2FourIsUp = rod2BeadStates[3]
        // Binler basamağı kontrolleri
        if(thousands < 5 && rod1TopIsDown){
            animateBeadUp(rod1TopBead)
            rod1TopIsDown = false
            updateBeadAppearance(rod1TopBead, false)
        }
        if (thousands >= 5) {
            animateBeadDown(rod1TopBead)
            rod1TopIsDown = true
            updateBeadAppearance(rod1TopBead, true)
            thousands -= 5
        }
        // Binler basamağı için boncukları güncelle
        val rod1BeadViews = listOf(rod1BottomBead1, rod1BottomBead2, rod1BottomBead3, rod1BottomBead4)
        val rod1BeadStates = mutableListOf(rod1OneIsUp, rod1TwoIsUp, rod1ThreeIsUp, rod1FourIsUp)
        updateBeadForDigit(thousands, rod1BeadViews, rod1BeadStates)
        
        // Boolean değişkenleri güncelle
        rod1OneIsUp = rod1BeadStates[0]
        rod1TwoIsUp = rod1BeadStates[1]
        rod1ThreeIsUp = rod1BeadStates[2]
        rod1FourIsUp = rod1BeadStates[3]
        // On binler basamağı kontrolleri
        if(ones < 5 && topIsDown){
            animateBeadUp(rod0TopBead)
            topIsDown = false
            updateBeadAppearance(rod0TopBead, false)
        }
        if (tenThousands >= 5) {
            animateBeadDown(rod0TopBead)
            topIsDown = true
            updateBeadAppearance(rod0TopBead, true)
            tenThousands -= 5
        }
        // On binler basamağı için boncukları güncelle
        val rod0BeadViews = listOf(rod0BottomBead1, rod0BottomBead2, rod0BottomBead3, rod0BottomBead4)
        val rod0BeadStates = mutableListOf(oneIsUp, twoIsUp, threeIsUp, fourIsUp)
        updateBeadForDigit(tenThousands, rod0BeadViews, rod0BeadStates)
        
        // Boolean değişkenleri güncelle
        oneIsUp = rod0BeadStates[0]
        twoIsUp = rod0BeadStates[1]
        threeIsUp = rod0BeadStates[2]
        fourIsUp = rod0BeadStates[3]
        
        // Animasyonların bitmesi için yeterli süre bekle (her animasyon 300ms, maksimum 5 basamak için ~2000ms)
        Handler(Looper.getMainLooper()).postDelayed({
            isWritingAnswerNumber = false
        }, 400)
    }
    private fun closeFragment(){// Fragment'i kapat ve MapFragment'e dön
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,  // Giriş animasyonu
                R.anim.slide_out_left // Çıkış animasyonu
            )
            .remove(this)
            .commit()

    }
    private fun devametFragment(fragment: Fragment) {
        // Devam butonunu ekranın altından kayarak göster
        binding.abacusLinear.visibility = View.GONE
        val screenHeight = resources.displayMetrics.heightPixels
        binding.devamButton.translationY = screenHeight.toFloat()
        binding.devamButton.visibility = View.VISIBLE
        binding.devamButton.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        // skipTutorialButton'ı sağa kaydırarak gizle (invisible yerine animasyon)
        val screenWidth = resources.displayMetrics.widthPixels
        binding.skipTutorialButton.animate()
            .translationX(screenWidth.toFloat())
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.skipTutorialButton.visibility = View.INVISIBLE
                binding.skipTutorialButton.translationX = 0f
            }
            .start()

        // Devam butonuna tıklama olayını ekle
        binding.devamButton.setOnClickListener {
            // Yeni fragment'i göster
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, fragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, fragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()


//eğer adımdaki şey true ise geri giderken ve
            // TutorialFragment'i kapat
            currentStep=0
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,  // Giriş animasyonu
                    R.anim.slide_out_right  // Çıkış animasyonu
                )
                .remove(this)
                .commit()
        }
    }
    private fun isAnyAnimationRunning(): Boolean {
        return currentAnimations.any { it.isAnimating() } || 
               widgetAnimators.any { it.isRunning }
               // || typewriterRunnable != null  // Typewriter effect devam ediyorsa true döndür
    }
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    private fun createTutorialSteps(){
        createTutorialSteps1()
        createTutorialSteps100()
        createTutorialSteps101()
        createTutorialSteps2()
        createTutorialSteps102()
        createTutorialSteps103()
        createTutorialSteps3()
        createTutorialSteps9999()
        createTutorialSteps4()
        createTutorialSteps5()
        createTutorialSteps6()
        createTutorialSteps104()
        createTutorialSteps7()
        createTutorialSteps105()
        createTutorialSteps8()
        createTutorialSteps9()
        createTutorialSteps10()
        createTutorialSteps11()
        createTutorialSteps12()
        createTutorialSteps13()
        createTutorialSteps14()
        createTutorialSteps15()
        createTutorialSteps16()
        createTutorialSteps17()
        createTutorialSteps18()
        createTutorialSteps19()
        createTutorialSteps20()
        createTutorialSteps21()
        createTutorialSteps22()
        createTutorialSteps23()
        createTutorialSteps24()
        createTutorialSteps25()
        createTutorialSteps26()
    }
    
    private fun createTutorialSteps1(){
        tutorialSteps = listOf(
            TutorialStep(
                "Numigo'ya hoş geldin. Ekrana tıklayarak eğitim adımları arasında ilerleyebilirsin.",
                null,
                listOf(
                    { WidgetOperation.ChangeVisibility(binding.abacusLinear, View.INVISIBLE) },
                    { WidgetOperation.ChangeVisibility(binding.backButton, View.INVISIBLE) },
                    { WidgetOperation.ChangeVisibility(binding.skipTutorialButton, View.VISIBLE) },//sonradan INV yapılacak
                    ),
                soundResource = R.raw.tutorial1_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Sol alttaki geri butonuna tıklayarak önceki adımlara gidebilirsin.",
                null,
                listOf(
                    { WidgetOperation.ChangeVisibility(binding.backButton, View.VISIBLE) },

                    ),
                soundResource = R.raw.tutorial1_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Şimdi başlayalım.",
                null,
                listOf(
                    { WidgetOperation.ChangeVisibility(binding.abacusLinear, View.VISIBLE) },
                    ),
                soundResource = R.raw.tutorial1_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Abaküs, sayıları temsil etmek için boncuklar kullanan bir hesap aracıdır.",
                null,
                soundResource = R.raw.tutorial1_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Her sütun bir basamağı temsil eder. Basamaklar sağdan sola doğru artarak ilerler.",
                null,
                soundResource = R.raw.tutorial1_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Birler",
                null,
                listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(0),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }
                ),
                soundResource = R.raw.tutorial1_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Onlar",
                null,
                listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(45),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                },
                soundResource = R.raw.tutorial1_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Yüzler",
                null,listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(90),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                },
                soundResource = R.raw.tutorial1_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Binler",
                null,listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(135),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                },
                soundResource = R.raw.tutorial1_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Ve on binler.",
                null,listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(180),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                },
                soundResource = R.raw.tutorial1_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Her sütunda 5 boncuk vardır.",
                null,listOf(
                    { WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = 0,
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    ) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(245),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(280),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial1_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ), TutorialStep(
                "Üstteki boncuklar beşlik değere sahipken,",
                null,
                listOf(
                    { WidgetOperation.ChangeMargin(focusView, 0, 0) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(245),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(60),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial1_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Alttaki boncuklar birlik değere sahiptir.",
                null,
                listOf(
                    { WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = 0,
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    ) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2
                            // Diğer constraint parametreleri varsayılan olarak UNSET kalacak
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(245),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(180),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial1_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Örneğin 1 sayısı abaküste bu şekilde gösterilir.",
                listOf(BeadAnimation(this, "rod4_bead_bottom1", 1)),
                listOf { WidgetOperation.ChangeVisibility(focusView, View.GONE) },
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial1_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "2 yazmak için birler basamağındaki birlik boncuklardan 2 tane kullanılır.",
                listOf(BeadAnimation(this, "rod4_bead_bottom2", 1)),
                soundResource = R.raw.tutorial1_100,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "3 için 3 tane,",
                listOf(BeadAnimation(this, "rod4_bead_bottom3", 1)),
                soundResource = R.raw.tutorial1_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4 için 4 tane birlik boncuk kullanılır.",
                listOf(BeadAnimation(this, "rod4_bead_bottom4", 1)),
                soundResource = R.raw.tutorial1_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Üstteki boncuklar beşlik değere sahiptir. Yani kullanıldığında sayımıza 5 ekler.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial1_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "5 sayısı böyle gösterilir.",
                listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                soundResource = R.raw.tutorial1_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "6 sayısı için üstteki boncuk ve bir alttaki boncuk kullanılır.",
                listOf(BeadAnimation(this, "rod4_bead_bottom1", 1)),
                soundResource = R.raw.tutorial1_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "7 böyle gösterilir.",
                listOf(BeadAnimation(this, "rod4_bead_bottom2", 1)),
                soundResource = R.raw.tutorial1_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "8 böyle,",
                listOf(BeadAnimation(this, "rod4_bead_bottom3", 1)),
                soundResource = R.raw.tutorial1_106,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "9 böyle gösterilir.",
                listOf(BeadAnimation(this, "rod4_bead_bottom4", 1)),
                soundResource = R.raw.tutorial1_107,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true

            ),TutorialStep(
                "Aşağıdaki sayıyı abaküse yazıp ‘Kontrol Et’ butonuna basalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "1",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 1,
                soundResource = R.raw.tutorial1_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = "En sağdaki birlik boncuğu kullan."
            ),TutorialStep(
                "Şimdi de bu sayıyı yazıp ‘Kontrol Et’ butonuna basalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "2",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 2,
                soundResource = R.raw.tutorial1_110,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = "En sağdaki birlik boncuklardan 2 adet kullan."


            ),TutorialStep(
                "Şimdi de bu sayıyı yazalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "5",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 5,
                soundResource = R.raw.tutorial1_111,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = "En sağ üstteki beşlik boncuğu kullan."
            ),TutorialStep(
                "Bu sayıyı yazalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "6",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 6,
                soundResource = R.raw.tutorial1_112,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = "Bir adet beşlik ve 1 adet birlik boncuğu kullan."
            ),TutorialStep(
                "Son olarak bu sayıyı yazalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "8",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 8,
                soundResource = R.raw.tutorial1_113,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = "Bir adet beşlik ve 3 adet birlik boncuğu kullan."
            ),

            TutorialStep(
                "Şimdi tahtaya yazacağım sayıları, abaküste göstermeye çalış.",
                null,
                soundResource = R.raw.tutorial1_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )

        )
    }
    
    private fun createTutorialSteps100(){
        tutorialSteps100 = listOf(
            TutorialStep(
                "Bu derste iki basamaklı sayıları göreceğiz.",
                soundResource = R.raw.tutorial3_100,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Eğer konuyu biliyorsan. Sağ alttaki eğitimi atla butonuna tıklayarak direkt teste geçebilirsin.",
                null,
                soundResource = R.raw.tutorial1_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Abaküste basamaklar normal sayılardaki gibi sola doğru artarak ilerler.",
                soundResource = R.raw.tutorial3_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 yazmak için onlar basamağındaki boncuklardan 1 adet birlik boncuk kullanılır.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "20 yazmak için 2 adet birlik boncuk kullanılır.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "30 böyle gösterilir.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "40 böyle,",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "50 yazmak istediğimizde ise yukarıdaki beşlik boncuk kullanılır.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_106,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "60 böyle gösterilir.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_107,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "70 böyle,",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "80 böyle,",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "90 ise böyle gösterilir.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_110,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu sayıyı beraber yazalım.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                ),
                soundResource = R.raw.tutorial3_111,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "70",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 70,
                abacusReset = true,
                nextStepAbacusReset = true,
                resetAndWaith = true,
                requestText = "Sağdan 2. sütundaki boncukları kullan. 1 adet beşlik 2 adet birlik boncuk ekle."
            ),TutorialStep(
                "Birden fazla basamaklı sayılar yazılırken her zaman en büyük basamaktan başlanır.",
                soundResource = R.raw.tutorial3_112,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true

            ),TutorialStep(
                "Yani 12 sayısını yazarken önce onlar basamağındaki 10 sayısı yazılır.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                soundResource = R.raw.tutorial3_113,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "12",
            ),TutorialStep(
                "Sonra ise 2 sayısı yazılır ve 12 böyle gösterilir.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                ),
                soundResource = R.raw.tutorial3_114,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "12",
            ),TutorialStep(
                "24 sayısını yazalım.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                ),
                soundResource = R.raw.tutorial3_115,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "24",
            ),TutorialStep(
                "Önce 20'yi onlar basamağına yazıyorum.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                ),
                soundResource = R.raw.tutorial3_116,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "24",
            ),TutorialStep(
                "Sonrada birler basamağındaki 4'ü yazıyorum ve işlem bitiyor.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                ),
                soundResource = R.raw.tutorial3_117,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "24",
            ),TutorialStep(
                "Bu sayıyı beraber yazalım.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                ),
                soundResource = R.raw.tutorial3_118,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "32",
                abacusReset = true
            ),TutorialStep(
                "Önce onlar basamağındaki 30’u yazıp ‘Kontrol Et’ butonuna basalım.",
                null,
                soundResource = R.raw.tutorial3_119,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "32",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 30,
                abacusReset = true,
                requestText = "Sağdan 2. sütundaki boncukları kullan. 3 adet birlik boncuk ekle."
            ),TutorialStep(
                "Sonrasında birler basamağındaki 2'yi yazıp ‘Kontrol Et’ butonuna basalım.",
                null,
                soundResource = R.raw.tutorial3_120,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,questionTextVisibility = View.VISIBLE,
                questionText = "32",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 32,
                nextStepAbacusReset = true,
                requestText = "En sağ sütundaki boncukları kullan. 2 adet birlik boncuk ekle.",
                backAnswerNumber = 30,
            ),TutorialStep(
                "78'i yazarken aynı şekilde önce büyük basamaktan başlanır.",
                null,
                soundResource = R.raw.tutorial3_121,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "78",
            ),TutorialStep(
                "Önce 70'i yazıyorum.",
                listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                soundResource = R.raw.tutorial3_122,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "78",
            ),TutorialStep(
                "Sonrada 8'i yazıyorum.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                ),
                soundResource = R.raw.tutorial3_123,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "78",
            ),TutorialStep(
                "Son olarak bu sayıyı beraber yazıp test'e geçelim.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)
                ),
                soundResource = R.raw.tutorial3_124,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "94",
                abacusReset = true
            ),TutorialStep(
                "Önce onlar basamağındaki sayıyı yazıp ‘Kontrol Et’ butonuna basalım.",
                null,
                soundResource = R.raw.tutorial3_125,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "94",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 90,
                abacusReset = true,
                requestText = "Sağdan 2. sütundaki boncukları kullan. 1 adet beşlik, 4 adet birlik boncuk ekle."
            ),TutorialStep(
                "Sonra da birler basamağındaki 4'ü yazıp ‘Kontrol Et’ butonuna basalım.",
                null,
                soundResource = R.raw.tutorial3_126,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "94",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 94,
                requestText = "En sağ sütundaki boncukları kullan. 4 adet birlik boncuk ekle.",
                backAnswerNumber = 90,
                nextStepAbacusReset = true

            ),TutorialStep(
                "Çok hızlı öğrendin. Şimdi test zamanı.",
                null,
                soundResource = R.raw.tutorial3_127,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )
        )
    }

    private fun createTutorialSteps101(){
        tutorialSteps101 = listOf(
            TutorialStep(
                "Bu derste 3 basamaklı sayıları göreceğiz.",
                soundResource = R.raw.tutorial4_100,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "3 basamaklı sayıları yazarken, iki basamaklı sayılarda olduğu gibi en büyük basamaktan başlıyoruz.",
                soundResource = R.raw.tutorial4_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Örneğin bu sayıyı yazarken.",
                soundResource = R.raw.tutorial4_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "154",
            ),TutorialStep(
                "İlk olarak yüzler basamağını abaküsteki yüzler basamağına yazıyorum.",
                listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)
                ),
                soundResource = R.raw.tutorial4_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "154",
            ),TutorialStep(
                "Sonrasında onlar basamağını,",
                listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                ),
                soundResource = R.raw.tutorial4_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "154",
            ),TutorialStep(
                "Ve son olarakta birler basamağını yazıyorum.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                ),
                soundResource = R.raw.tutorial4_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "154",
            ),TutorialStep(
                "Bu sayıyı beraber yazalım.",
                listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                ),
                soundResource = R.raw.tutorial4_106,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "687",
                abacusReset = true
            ),TutorialStep(
                "Önce yüzler basamağını abaküsteki sağdan 3. sütuna yazalım.",
                soundResource = R.raw.tutorial4_107,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "687",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 600,
                abacusReset = true,
            ),TutorialStep(
                "Sonrasında onlar basamağını 2. sütuna yazalım.",
                soundResource = R.raw.tutorial4_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "687",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 680,
                backAnswerNumber = 600
            ),TutorialStep(
                "Ve son olarak birler basamağını en sağdaki sütuna yazalım.",
                soundResource = R.raw.tutorial4_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "687",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 687,
                backAnswerNumber = 680,
                nextStepAbacusReset = true
            ),TutorialStep(
                "İşte bu kadar basit.",
                soundResource = R.raw.tutorial4_110,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Teste geç.",
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
        )
    }
    
    private fun createTutorialSteps2(){
        tutorialSteps2 = listOf(
            TutorialStep(
            "Bu ünitede, çok büyük sayıları bile ne kadar kolay toplayabileceğini öğreneceksin.",
            null,
                soundResource = R.raw.tutorial2_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Kuralsız Toplama",
                null,
                soundResource = R.raw.tutorial2_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                text = "Bu işlemle başlayalım.",
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "ilk önce abaküse 3 yazıyoruz.",
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)),
                soundResource = R.raw.tutorial2_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Sonrasında toplamak için 5'i ekleriz.",
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                soundResource = R.raw.tutorial2_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Cevap 8.",
                soundResource = R.raw.tutorial2_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                "Bu işlemde de...",
                soundResource = R.raw.tutorial2_100,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)),
            ),
            TutorialStep(
                "Önce abaküse ilk sayıyı yazıyorum.",
                soundResource = R.raw.tutorial2_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
            ),
            TutorialStep(
                "Sonrasında ekleyeceğim sayı değerinde boncuk ekliyorum.",
                soundResource = R.raw.tutorial2_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                "Sonrasında ekleyeceğim sayı değerinde boncuk ekliyorum.",
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    ),
            ),
            TutorialStep(
                "Cevap 8.",
                soundResource = R.raw.tutorial2_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                "Şimdi bu örneği beraber yapalım.",
                questionText = "2 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)),
                soundResource = R.raw.tutorial2_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true

            ),
            TutorialStep(
                "İlk önce abaküse 2 yazıp Kontrol Et'e basalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "2 + 6",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 2,
                soundResource = R.raw.tutorial2_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "En sağdaki sütundan 2 adet birlik boncuk kullan."
            ),TutorialStep(
                "Şimdi de 6'yı ekleyip Kontrol Et'e basalım.",
                questionText = "2 + 6",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 8,
                abacusClickable = true,
                nextStepAvailable = false,
                soundResource = R.raw.tutorial2_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                backAnswerNumber = 2,
                requestText = "En sağdaki sütuna 1 tane beşlik, 1 tane birlik boncuk ekle."


            ),TutorialStep(
                "Çabuk öğreniyorsun.",
                soundResource = R.raw.tutorial2_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),

            TutorialStep(
                "Son olarak bu örneği beraber yapalım.",
                soundResource = R.raw.tutorial2_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "1 + 8",
                questionTextVisibility = View.VISIBLE,
                abacusReset = true
            ),
            TutorialStep(
                "İlk önce abaküse 1 yazıp Kontrol Et'e basalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "1 + 8",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 1,
                soundResource = R.raw.tutorial2_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "En sağdaki sütundan 1 adet birlik boncuk kullan."
            ),TutorialStep(
                "Şimdi de 8'i ekleyip Kontrol Et'e basalım.",
                questionText = "1 + 8",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 9,
                abacusClickable = true,
                nextStepAvailable = false,
                soundResource = R.raw.tutorial2_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 1,
                nextStepAbacusReset = true,
                requestText = "En sağdaki sütundan 1 adet beşlik 3 adet birlik boncuk kullan."
            ),
            TutorialStep(
                "Çok güzel. Hemen kaptın.",
                soundResource = R.raw.tutorial2_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Şimdi kendini deneme zamanı.",
                soundResource = R.raw.tutorial2_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bakalım senin için hazırladığım testi çözebilecek misin?",
                soundResource = R.raw.tutorial2_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )
        )
    }

    private fun createTutorialSteps102(){
        tutorialSteps102 = listOf(
            TutorialStep(
                "İki basamaklı sayılar toplanırken, sayılar en büyük basamaktan başlanarak toplanır.",
                soundResource = R.raw.tutorial2_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Mesela bu işleme bakalım.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk önce 21 yazıyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk önce 21 yazıyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1))
            ),TutorialStep(
                "Sonrasında, 13'ü eklemek için önce 10'unu onlar basamağına, sonra da 3'ünü birler basamağına ekliyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Sonrasında, 13'ü eklemek için önce 10'unu onlar basamağına, sonra da 3'ünü birler basamağına ekliyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(BeadAnimation(this, "rod3_bead_bottom3", 1))
            ),TutorialStep(
                "Sonrasında, 13'ü eklemek için önce 10'unu onlar basamağına, sonra da 3'ünü birler basamağına ekliyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE ,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ), TutorialStep(
                "Cevap 34.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE ,
                soundResource = R.raw.tutorial2_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                soundResource = R.raw.tutorial2_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk önce 16 yazıp kontrol ete tıklayalım.",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 16,
                soundResource = R.raw.tutorial2_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağından 1 adet birlik, birler basamağından 1 adet beşlik ve 1 adet birlik boncuk kullan."
            ),TutorialStep(
                "Sonrasında, 21'i eklemek için önce 20'yi onlar basamağına ekleyelim.",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 36,
                soundResource = R.raw.tutorial2_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 16,
                requestText = "Onlar basamağına 2 adet birlik boncuk ekle."
            ),TutorialStep(
                "Sonra da 1'i birler basamağına ekleyelim.",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 37,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                backAnswerNumber = 36,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial10_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),TutorialStep(
                "İlk önce 36 yazıyorum.",
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial102_0,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),TutorialStep(
                "İlk önce 36 yazıyorum.",
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),TutorialStep(
                "Sonrasında, 63'ü eklemek için önce 60'ı onlar basamağına, sonra da 3'ü birler basamağına ekliyorum.",
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial102_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Sonrasında, 63'ü eklemek için önce 60'ı onlar basamağına, sonra da 3'ü birler basamağına ekliyorum.",
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    )
            ),TutorialStep(
                "Sonrasında, 63'ü eklemek için önce 60'ı onlar basamağına, sonra da 3'ü birler basamağına ekliyorum.",
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE ,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                )
            ), TutorialStep(
                "Cevap 99.",
                soundResource = R.raw.tutorial102_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "Önce ilk sayıyı yazıp ‘Kontrol Et’ butonuna tıkla.",
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 23,
                soundResource = R.raw.tutorial102_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağından 2 adet birlik, birler basamağından 3 adet birlik boncuk kullan."
            ),TutorialStep(
                "Sonrasında, 71'i eklemek için önce 70'i onlar basamağına ekle.",
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 93,
                soundResource = R.raw.tutorial102_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 23,
                requestText = "Onlar basamağına 1 adet beşlik, 2 adet birlik boncuk ekle."
            ),TutorialStep(
                "Sonra da 1'i birler basamağına ekle.",
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 94,
                soundResource = R.raw.tutorial102_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 93,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "Harika! Şimdi teste geç.",
                soundResource = R.raw.tutorial102_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )
        )
    }
    private fun createTutorialSteps103(){
        tutorialSteps103 = listOf(
            TutorialStep(
                "Üç basamaklı sayılar toplanırkende toplamaya en büyük basamaktan başlayacağız.",
                soundResource = R.raw.tutorial2_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu bütün toplama işlemlerinde böyle her zaman en büyük basamaktan başlayarak toplanır.",
                soundResource = R.raw.tutorial2_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işleme bakalım.",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk önce 241 yazıyoruz.",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk önce 241 yazıyoruz.",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1))
            ),TutorialStep(
                "Sonrasında 153'ü en büyük basamağından başlayarak ekliyoruz.",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "100",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(BeadAnimation(this, "rod2_bead_bottom3", 1))
            ),TutorialStep(
                "50",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE ,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3))
            ), TutorialStep(
                "3",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE ,
                soundResource = R.raw.tutorial2_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                )
            ), TutorialStep(
                "Cevap 394.",
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE ,
                soundResource = R.raw.tutorial2_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                soundResource = R.raw.tutorial2_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk önce 222 yazıp kontrol ete tıkla.",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 222,
                soundResource = R.raw.tutorial2_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağından 1 adet birlik, birler basamağından 1 adet beşlik ve 1 adet birlik boncuk kullan."
            ),TutorialStep(
                "Sonrasında, 126'yı en büyük basamağından başlayarak ekle.",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 222,
                requestText = "Onlar basamağına 2 adet birlik boncuk ekle."
            ),TutorialStep(
                "100",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 322,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 222,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "20",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 342,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 322,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "6",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 348,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 342,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "Cevap 348.",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk önce 582 yazıyoruz.",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk önce 582 yazıyoruz.",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),)
            ),TutorialStep(
                "Sonrasında 316'yı en büyük basamağından başlayarak ekliyoruz.",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "300",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    )
            ),TutorialStep(
                "10",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE ,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1))
            ), TutorialStep(
                "6",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE ,
                soundResource = R.raw.tutorial2_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    )
            ), TutorialStep(
                "Cevap 898.",
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE ,
                soundResource = R.raw.tutorial2_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk önce 723 yazıp kontrol ete tıkla.",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 723,
                soundResource = R.raw.tutorial2_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağından 1 adet birlik, birler basamağından 1 adet beşlik ve 1 adet birlik boncuk kullan."
            ),TutorialStep(
                "Sonrasında, 266'yı en büyük basamağından başlayarak ekle.",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 723,
                requestText = "Onlar basamağına 2 adet birlik boncuk ekle."
            ),TutorialStep(
                "200",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 923,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 723,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "60",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 983,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 923,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "6",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 989,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 983,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "Cevap 989.",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                backAnswerNumber = 989,
                requestText = "Birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "Harika! Şimdi teste geç.",
                null,
                soundResource = R.raw.tutorial102_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )
        )
    }
    private fun createTutorialSteps9999(){
        tutorialSteps9999 = listOf(
            TutorialStep(
                "deneme",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "deneme",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3000_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )
        )

    }

    private fun createTutorialSteps3(){
        tutorialSteps3 = listOf(
            TutorialStep(
                "Bu derste kurallı toplamanın ilk dersi olan 5'lik toplamayı öğreneceksin.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5'lik toplamayı sayıları doğrudan ekleyemediğimiz zaman kullanırız.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3000_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Örneğin bu işlemde 4'ü abaküse yazıyorum.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3000_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "4 + 1",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
            ),TutorialStep(
                "1'i eklemek istediğimde doğrudan ekleyemiyorum. Aşağıda eklenecek boncuk yok.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3000_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextVisibility = View.VISIBLE,
                questionText = "4 + 1"
            ),TutorialStep(
                "İşte böyle zamanlarda 5'lik toplama kuralını uygulayacağız.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3000_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))
            ),TutorialStep(
                "5’lik toplama kuralında ekleyeceğimiz sayıların 5’e tamamlayan kardeşleri vardır.",
                soundResource = R.raw.tutorial3_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "1’in kardeşi 4’tür.",
                soundResource = R.raw.tutorial3_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "2’nin kardeşi 3",
                soundResource = R.raw.tutorial3_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "3’ün kardeşi 2",
                soundResource = R.raw.tutorial3_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4’ün kardeşi 1’dir.",
                soundResource = R.raw.tutorial3_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu kuralı sayıyı ekleyemediğimiz zaman 5 gelir kardeşi gider. Şeklinde uygulayacağız.",
                soundResource = R.raw.tutorial3000_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Örneğin bu işlemi yapmaya çalışalım.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "İlk olarak abaküse ilk sayıyı yazıyoruz.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                soundResource = R.raw.tutorial3_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Sonrasında ekleyeceğimiz ikinci sayıyı yazacağız.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Ama 1'i doğrudan ekleyemiyoruz.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "O yüzden burada 5'lik toplama kuralını uygulayacağız.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "1'in kardeşi 4'tür.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),TutorialStep(
                "Kardeşi 4 gider.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Kardeşi 4 gider.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
            ),TutorialStep(
                "Cevap 5.",
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4)),
                soundResource = R.raw.tutorial3_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Önce abaküse 4 yazıyoruz.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                soundResource = R.raw.tutorial3_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Sonrasında 2'yi ekleyeceğiz. Ama 2'yi doğrudan ekleyemiyoruz.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "2'nin kardeşi 3'tür.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3))

            ),TutorialStep(
                "Kardeşi 3 gider.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Kardeşi 3 gider.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))
            ),TutorialStep(
                "Cevap 6.",
                soundResource = R.raw.tutorial3_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                ),
                soundResource = R.raw.tutorial3_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Önce abaküse  3'ü yazıyoruz.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)),
                soundResource = R.raw.tutorial3_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Sonrasında 3'ü ekleyeceğiz. Ama 3'ü doğrudan ekleyemiyoruz.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "3'ün kardeşi 2'dir.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "5 gelir.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),TutorialStep(
                "Kardeşi 2 gider.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Kardeşi 2 gider.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2))

            ),TutorialStep(
                "Cevap 6.",
                soundResource = R.raw.tutorial3_31,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Son olarak bu işlemi ele alalım.",
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                ),
                soundResource = R.raw.tutorial3_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Önce abaküse 2'yi yazıyoruz.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)),
                soundResource = R.raw.tutorial3_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Sonrasında 4'ü ekleyeceğiz. Ama 4'ü doğrudan ekleyemiyoruz.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_34,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4'ün kardeşi 1'dir.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2))
            ),TutorialStep(
                "Cevap 6.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_38,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu kural her basamak için aynıdır.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                ),
                soundResource = R.raw.tutorial3_39,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işlemde de aynı kuralı uygularız.",
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "30'u abaküse yazıyoruz.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)),
                soundResource = R.raw.tutorial3_41,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "40'ı doğrudan ekleyemiyoruz.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_43,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4'ün kardeşi 1'dir.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3))
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 2))
            ),TutorialStep(
                "Cevap 70.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_44,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                soundResource = R.raw.tutorial3_45,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,

                ),TutorialStep(
                "Önce abaküse ilk sayıyı yaz.",
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 4,
                abacusClickable = true,
                soundResource = R.raw.tutorial3_46,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                    requestText = "Birler basamağına 4 adet birlik boncuk ekle."
                ),TutorialStep(
                "Sonrasında 4'ü ekleyeceğiz.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_47,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Doğrudan ekleyemediğimiz için 5'lik kural uygulayacağız.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_48,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),TutorialStep(
                "4'ün kardeşi 1'dir.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 4
            ),TutorialStep(
                "5 gelir.",
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 9,
                abacusClickable = true,
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 4,
                requestText = "Birler basamağındaki beşlik boncuğu ekle."

                ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 8,
                abacusClickable = true,
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 9,
                requestText = "Birler basamağından 1 adet birlik boncuk çıkar."

            ),TutorialStep(
                "Ve cevap 8.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_49,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 8,
                nextStepAbacusReset = true
            ),
            TutorialStep(
                "Son olarak bu soruyu yapalım.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3000_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "Önce abaküse ilk sayıyı yaz.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 3,
                abacusClickable = true,
                soundResource = R.raw.tutorial3_46,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Birler basamağına 3 adet birlik boncuk ekle."

            ),TutorialStep(
                "Sonrasında 2'yi ekleyeceğiz.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3000_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Doğrudan ekleyemediğimiz için 5'lik kural uygulayacağız.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_48,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "2'nin kardeşi 3'tür.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 3
            ),TutorialStep(
                "5 gelir.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 8,
                abacusClickable = true,
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 3,
                requestText = "Birler basamağındaki beşlik boncuğu ekle."

            ),TutorialStep(
                "Kardeşi 3 gider.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 5,
                abacusClickable = true,
                soundResource = R.raw.tutorial3_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 8,
                requestText = "Birler basamağından 3 adet birlik boncuk çıkar."

            ),TutorialStep(
                "Cevap 5.",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Şimdi öğrendiklerini uygulama zamanı.",
                soundResource = R.raw.tutorial3_50,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )
        )
    }
    
    private fun createTutorialSteps4(){
        tutorialSteps4 = listOf(
            //4 adet 3 basamaklı toplama yapsak yeter. 2'sini biz. 2  sini beraber.
            TutorialStep(
                "Üç basamaklı sayılarda 5'lik toplama.",
                soundResource = R.raw.tutorial4_1000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.VISIBLE
            ),TutorialStep(
                "Bu örneğe bakalım.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "İlk sayıyı abaküse yazıyoruz.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                soundResource = R.raw.tutorial4_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Sonrasında 346 sayısını en büyük basamağından başlayarak ekliyoruz.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_1001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "300'ü doğrudan ekleyemiyoruz. Bu yüzden 5'lik kuralı uygulayacağız.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial4_1010,

                ),TutorialStep(
                "5 gelir.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_13,
            ),TutorialStep(
                "5 gelir.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3)),
            ),TutorialStep(
                "3'ün kardeşi 2 gider.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial4_1002,

                ),TutorialStep(
                "3'ün kardeşi 2 gider.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                )
            ),TutorialStep(
                "Şimdi sıra onlar basamağında.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial4_1003,

                ),TutorialStep(
                "40'ı doğrudan ekleyemiyoruz. Bu yüzden 5'lik kuralı uygulayacağız.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial4_2000,
            ),TutorialStep(
                "5 gelir.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_13,
                ),TutorialStep(
                "5 gelir.",
                questionText = "243 + 346",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                ),
                questionTextVisibility = View.VISIBLE,
            ),TutorialStep(
                "Kardeşi 1 gider.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Kardeşi 1 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 2)),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
            ),TutorialStep(
                "Son olarak birler basamağına 6 ekliyor ve işlemi bitiriyoruz.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_2001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Son olarak birler basamağına 6 ekliyor ve işlemi bitiriyoruz.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
            ),TutorialStep(
                "Cevap 589.",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_2002,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                soundResource = R.raw.tutorial3_45,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,

                ),TutorialStep(
                "Önce abaküse ilk sayıyı yaz.",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 423,
                abacusClickable = true,
                soundResource = R.raw.tutorial3_46,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Yüzler basamağına 4 adet birlik, onlar basamağına 2 adet birlik, birler basamağına 1 adet birlik boncuk ekle."
            ),TutorialStep(
                "Toplamaya yüzler basamağından başlıyoruz.",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_1005,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 423

            ),TutorialStep(
                "300'ü ekleyip ‘Kontrol et’ butonuna tıkla.",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                soundResource = R.raw.tutorial4_1006,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                answerNumber = 723,
                backAnswerNumber = 423,
                requestText = "Yüzler basamağına 1 adet beşlik boncuk ekle. 2 adet birlik boncuk çıkar."
            ),TutorialStep(
                "20'yi ekleyip ‘Kontrol et’ butonuna tıkla.",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                soundResource = R.raw.tutorial4_2003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 723,
                answerNumber = 743,
                requestText = "Onlar basamağına 2 adet birlik boncuk ekle."
            ),TutorialStep(
                "2'yi ekleyip ‘Kontrol et’ butonuna tıkla.",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                soundResource = R.raw.tutorial4_2004,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 743,
                requestText = "Birler basamağına 1 adet beşlik boncuk ekle. 3 adet birlik boncuk çıkar.",
                answerNumber = 745,

            ),TutorialStep(
                "Cevap 745.",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_2005,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = "Birler basamağından 1 adet birlik boncuk çıkar."
            ),TutorialStep(
                "Teste geç.",
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            )
        )
    }
    
    private fun createTutorialSteps5(){
        tutorialSteps5 = listOf(
            TutorialStep(
                "Bu derste 10'luk toplamayı öğreneceğiz.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10’luk toplamayı, 5'lik kuraldaki 5 gelir adımını uygulayamadığımız zaman kullanacağız.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Örneğin bu işlemde.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                ),
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "9 sayısına 1 eklemek istiyoruz ama hem aşağıda ekleyeceğim ekstra boncuk yok.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    
                    { WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = 0,
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    ) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2,
                            // Diğer constraint parametreleri varsayılan olarak UNSET kalacak
                        )
                    },
                    {
                        WidgetOperation.ChangeMargin(
                            view = focusView,
                            marginRight = 0,
                            marginLeft = 0,
                            marginTop = -dpToPx(10) // 10dp yukarı taşı
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(180),
                            duration = 400
                        )
                    })


                ),TutorialStep(
                "Hem de 5’lik kuralı uygulamak için ekleyeceğim 5’lik boncuk yok.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf({ WidgetOperation.ChangeMargin(focusView, 0, 0) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }),


                ),TutorialStep(
                "Yani, 1’in kardeşi 4. Ama “5 gelir, 4 gider” diyemiyoruz.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf { WidgetOperation.ChangeVisibility(focusView, View.GONE) }
                ),TutorialStep(
                "Bu durumda büyük kardeşler devreye giriyor.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Büyük kardeşler sayıları 10’a tamamlar.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "10 gelir, büyük kardeş gider.",
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "1’in büyük kardeşi 9’dur.",
                soundResource = R.raw.tutorial5_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "2’nin büyük kardeşi 8’dir.",
                soundResource = R.raw.tutorial5_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "3’ün büyük kardeşi 7’dir.",
                soundResource = R.raw.tutorial5_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4’ün büyük kardeşi 6’dır.",
                soundResource = R.raw.tutorial5_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5’in büyük kardeşi 5’tir.",
                soundResource = R.raw.tutorial5_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işleme tekrar bakalım.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Şimdi 1 eklemek istiyorum.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Önce, doğrudan ekleyebilir miyim diye bakıyorum, ama aşağıda ekstra boncuk yok.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },

                    { WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = 0,
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    ) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2
                            // Diğer constraint parametreleri varsayılan olarak UNSET kalacak
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(180),
                            duration = 400
                        )
                    },
                    {
                        WidgetOperation.ChangeMargin(
                            view = focusView,
                            marginRight = 0,
                            marginLeft = 0,
                            marginTop = -dpToPx(10) // 10dp yukarı taşı
                        )
                    },
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial5_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5'lik kuralı uygulamak için yukarı bakıyorum ama yukarıda da ekleyebileceğim boncuk yok.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf({ WidgetOperation.ChangeMargin(focusView, 0, 0) },
                {
                    WidgetOperation.ChangeConstraints(
                        view = focusView,
                        topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                        bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    )
                },
                {
                    WidgetOperation.AnimateSize(
                        view = focusView,
                        fromWidth = focusView.width,
                        toWidth = dpToPx(60),
                        fromHeight = focusView.height,
                        toHeight = dpToPx(100),
                        duration = 400
                    )
                }),
                soundResource = R.raw.tutorial5_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "O zaman son seçenek olan 10'luk kurala geçiyorum.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf { WidgetOperation.ChangeVisibility(focusView, View.GONE) },
                soundResource = R.raw.tutorial5_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),TutorialStep(
                "10 gelir.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),TutorialStep(
                "10 gelir.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                        BeadAnimation(this, "rod3_bead_bottom1", 1))
            ),TutorialStep(
                "1’in büyük kardeşi olan 9 gider.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "1’in büyük kardeşi olan 9 gider.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Cevap 10.",
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Aynısını bu işlem için yapacak olsaydık...",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                soundResource = R.raw.tutorial5_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "8 abaküse yazılır.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                ),
                soundResource = R.raw.tutorial5_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "2'yi doğrudan veya 5'lik kural ile ekleyebilir miyim ? Diye kontrol edilir.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İkiside yapılamadığı için 10'luk kural uygulanır.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

            ),TutorialStep(
                "2’nin büyük kardeşi 8 gider.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "2’nin büyük kardeşi 8 gider.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Cevap 10.",
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu işlem için...",
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                soundResource = R.raw.tutorial5_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "9 abaküse yazılır.",
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                ),
                soundResource = R.raw.tutorial5_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))
            ),TutorialStep(
                "3’ün büyük kardeşi 7 gider.",
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "3’ün büyük kardeşi 7 gider.",
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Cevap 12.",
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_31,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Bu işlem için...",
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                ),
                soundResource = R.raw.tutorial5_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "7 abaküse yazılır.",
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                ),
                soundResource = R.raw.tutorial5_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                )
            ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_34,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Cevap 11.",
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                )
            ,TutorialStep(
                "Bu işlem için...",
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                ),
                soundResource = R.raw.tutorial5_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "6 abaküse yazılır.",
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                ),
                soundResource = R.raw.tutorial5_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                )
            ),TutorialStep(
                "5’in büyük kardeşi 5 gider.",
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5’in büyük kardeşi 5 gider.",
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Ve cevap 11.",
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_38,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Bu örneği beraber yapalım.",
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                ),
                soundResource = R.raw.tutorial5_39,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 18,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 1 adet birlik, birler basamağına 3 adet birlik, 1 adet beşlik boncuk ekle."

            ),TutorialStep(
                "Şimdi ise, 4’ü doğrudan mı, 5’lik kuralla mı, yoksa 10’luk kuralla mı ekleyeceğimize karar verelim.",
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_41,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 18

                ),TutorialStep(
                "Ve ekleyelim.",
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 22,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 18,
                nextStepAbacusReset = true,
                requestText = "10 gelir 4'ün büyük kardeşi 6 gider."
            ),TutorialStep(
                "5'lik kuraldan tek farkı. 5 gelir kardeş gider değil. 10 gelir kardeş gider diyoruz.",
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_43,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true

            ),TutorialStep(
                "Son olarak bu soruyu yapalım.",
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3000_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 17,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 1 adet birlik, birler basamağına 2 adet birlik, 1 adet beşlik boncuk ekle."

            ),TutorialStep(
                "Şimdi ise, 4’ü doğrudan mı, 5’lik kuralla mı, yoksa 10’luk kuralla mı ekleyeceğimize karar verelim.",
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_41,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 17

            ),TutorialStep(
                "Ve ekleyelim.",
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 20,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 17,
                requestText = "10 gelir 3'ün büyük kardeşi 7 gider."

            ),TutorialStep(
                "Şimdi kendini ispatlama zamanı.",
                soundResource = R.raw.tutorial5_44,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            )
        )
    }
    
    private fun createTutorialSteps6(){
        tutorialSteps6 = listOf(
            TutorialStep(
                "10’luk toplamada “10 gelir” işlemini yaparken 5’lik veya 10’luk kuralı kullanmamız gerekebilir.",
                soundResource = R.raw.tutorial6_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Örnek olarak bu işlemde.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial6_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "49 sayısına 5 eklemek istiyorum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 sayısını doğrudan ekleyemiyorum. O zaman 10'luk kural uygulayacağım.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yapacağım işlem: “10 gelir, 5’in büyük kardeşi 5 gider.”",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Fakat onlar basamağına 10’u eklerken doğrudan ekleyemiyorum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = dpToPx(45),
                            toMarginRight = dpToPx(45),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }

                ),
                soundResource = R.raw.tutorial6_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Böyle olduğunda 5’lik veya 10’luk kurallardan yardım alacağız.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Burada 10 sayısını eklemek için 5’lik kuralı uygulayabilirim. Çünkü yukarıdaki 5’lik boncuğum boşta.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir. 1'in kardeşi 4 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                widgetOperations = listOf {
                    WidgetOperation.ChangeVisibility(focusView, View.GONE)},
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu şekilde 10’luk kuralındaki 10 gelir adımını gerçekleştirmiş oldum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L)
            ,TutorialStep(
                "Şimdi 5 gider adımını yapıp işlemi sonlandıracağız.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Şimdi 5 gider adımını yapıp işlemi sonlandıracağız.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Ve cevap 54.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yaptığımız işlemleri tekrar edelim.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                ),
                soundResource = R.raw.tutorial6_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "49’u abaküse yazıyorum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial6_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "5’i doğrudan ekleyemiyorum. Bu yüzden 10’luk kuralı uygulayacağım.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir 5’in büyük kardeşi 5 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                soundResource = R.raw.tutorial6_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Ve büyük kardeş 5 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Ve büyük kardeş 5 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Cevap 54.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 48,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 4 adet birlik, birler basamağına 3 adet birlik, 1 adet beşlik boncuk ekle."

            ),TutorialStep(
                "2'yi 10'luk kuralla ekleyeceğiz. Önce '10 gelir' adımını yapıp 'Kontrol et' butonuna tıklayalım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1000,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 48,
                answerNumber = 58,
                requestText = "Onlar basamağına 1 ekle. 5 gelir. 1'in kardeşi 4 gider."

            ),TutorialStep(
                "Ve 2'nin büyük kardeşini çıkarıp işlemi sonlandıralım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 50,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_1001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 58,
                nextStepAbacusReset = true,
                requestText = "Birler basamağından 8 çıkar."
            ),TutorialStep(
                "Bir de bu örneğe bakalım.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk sayıyı abaküse yazıyorum.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                soundResource = R.raw.tutorial6_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L


            ),TutorialStep(
                "Şimdi 4 sayısını 10’luk kuralla ekleyeceğim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir. 4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Onlar basamağına 10 gelebilmesi için bu sefer 10’luk kuralı uygulamamız gerekir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    { WidgetOperation.AnimateSize(
                        view = focusView,
                        fromWidth = focusView.width,
                        toWidth = dpToPx(60),
                        fromHeight = focusView.height,
                        toHeight = dpToPx(250),
                        duration = 400
                    )
                    },
                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = dpToPx(45),
                        toMarginRight = dpToPx(45),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                    }

                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir. 1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf (
                    {WidgetOperation.ChangeVisibility(focusView, View.GONE)}),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "10 gelir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                soundResource = R.raw.tutorial6_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),

                    )
            ),TutorialStep(
                "Şimdi de 4’ün büyük kardeşi 6’yı götüreceğiz.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Şimdi de 4’ün büyük kardeşi 6’yı götüreceğiz.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                "Ve cevap 102.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yaptığımız işlemleri tekrar edelim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2)),
                soundResource = R.raw.tutorial6_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "98’i abaküse yazıyorum.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                soundResource = R.raw.tutorial6_31,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "4 sayısını 10’luk kuralla ekleyeceğim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir. 4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                soundResource = R.raw.tutorial6_34,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                "Cevap 102.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 96,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 4 adet birlik, 1 adet beşlik, Birler basamağına 1 adet birlik, 1 adet beşlik boncuk ekle."

            ),TutorialStep(
                "5'i 10'luk kuralla ekleyeceğiz. Önce '10 gelir' adımını yapıp 'Kontrol et' butonuna tıklayalım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1002,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 96,
                answerNumber = 106,
                requestText = "Onlar basamağına 1 ekle. 10 gelir. 1'in kardeşi 9 gider."

            ),TutorialStep(
                "Ve 5'in büyük kardeşini çıkarıp işlemi sonlandıralım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 101,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_1003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 106,
                nextStepAbacusReset = true,
                requestText = "Birler basamağından 5 çıkar."
            ),TutorialStep(
                "Şimdi öğrendiklerini uygulama zamanı.",
                soundResource = R.raw.tutorial6_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE
            ),TutorialStep(
                "Onlara nasıl bir canavar olduğunu göster.",
                soundResource = R.raw.tutorial6_38,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE

            ),
        )
    }

    private fun createTutorialSteps104(){
        tutorialSteps104 = listOf(
            //846 + 345
            TutorialStep(
                "Bu işleme bakalım",
                questionText = "846 + 345",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial6_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Önce abaküse ilk sayıyı yazıyorum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Sonrasında en büyük basamaktan başlayarak toplama işlemlerini yapıyorum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 sayısını doğrudan ekleyemiyorum. O zaman 10'luk kural uygulayacağım.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yapacağım işlem: “10 gelir, 5’in büyük kardeşi 5 gider.”",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Fakat onlar basamağına 10’u eklerken doğrudan ekleyemiyorum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = dpToPx(45),
                        toMarginRight = dpToPx(45),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                    }

                ),
                soundResource = R.raw.tutorial6_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Böyle olduğunda 5’lik veya 10’luk kurallardan yardım alacağız.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Burada 10 sayısını eklemek için 5’lik kuralı uygulayabilirim. Çünkü yukarıdaki 5’lik boncuğum boşta.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.AnimateSize(
                        view = focusView,
                        fromWidth = focusView.width,
                        toWidth = dpToPx(60),
                        fromHeight = focusView.height,
                        toHeight = dpToPx(100),
                        duration = 400
                    )
                    },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir. 1'in kardeşi 4 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                widgetOperations = listOf {
                    WidgetOperation.ChangeVisibility(focusView, View.GONE)},
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu şekilde 10’luk kuralındaki 10 gelir adımını gerçekleştirmiş oldum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L)
            ,TutorialStep(
                "Şimdi 5 gider adımını yapıp işlemi sonlandıracağız.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Şimdi 5 gider adımını yapıp işlemi sonlandıracağız.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Ve cevap 54.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yaptığımız işlemleri tekrar edelim.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                ),
                soundResource = R.raw.tutorial6_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "49’u abaküse yazıyorum.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial6_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "5’i doğrudan ekleyemiyorum. Bu yüzden 10’luk kuralı uygulayacağım.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir 5’in büyük kardeşi 5 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                soundResource = R.raw.tutorial6_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Ve büyük kardeş 5 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Ve büyük kardeş 5 gider.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                "Cevap 54.",
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 48,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 4 adet birlik, birler basamağına 3 adet birlik, 1 adet beşlik boncuk ekle."

            ),TutorialStep(
                "2'yi 10'luk kuralla ekleyeceğiz. Önce '10 gelir' adımını yapıp 'Kontrol et' butonuna tıklayalım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1000,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 48,
                answerNumber = 58,
                requestText = "Onlar basamağına 1 ekle. 5 gelir. 1'in kardeşi 4 gider."

            ),TutorialStep(
                "Ve 2'nin büyük kardeşini çıkarıp işlemi sonlandıralım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 50,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_1001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 58,
                nextStepAbacusReset = true,
                requestText = "Birler basamağından 8 çıkar."
            ),TutorialStep(
                "Bir de bu örneğe bakalım.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "İlk sayıyı abaküse yazıyorum.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                soundResource = R.raw.tutorial6_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L


            ),TutorialStep(
                "Şimdi 4 sayısını 10’luk kuralla ekleyeceğim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir. 4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Onlar basamağına 10 gelebilmesi için bu sefer 10’luk kuralı uygulamamız gerekir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    { WidgetOperation.AnimateSize(
                        view = focusView,
                        fromWidth = focusView.width,
                        toWidth = dpToPx(60),
                        fromHeight = focusView.height,
                        toHeight = dpToPx(250),
                        duration = 400
                    )
                    },
                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = dpToPx(45),
                        toMarginRight = dpToPx(45),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                    }

                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir. 1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf (
                    {WidgetOperation.ChangeVisibility(focusView, View.GONE)}),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "10 gelir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                soundResource = R.raw.tutorial6_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),

                    )
            ),TutorialStep(
                "Şimdi de 4’ün büyük kardeşi 6’yı götüreceğiz.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Şimdi de 4’ün büyük kardeşi 6’yı götüreceğiz.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                "Ve cevap 102.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yaptığımız işlemleri tekrar edelim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2)),
                soundResource = R.raw.tutorial6_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "98’i abaküse yazıyorum.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                soundResource = R.raw.tutorial6_31,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "4 sayısını 10’luk kuralla ekleyeceğim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir. 4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                soundResource = R.raw.tutorial6_34,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                "Cevap 102.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 96,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 4 adet birlik, 1 adet beşlik, Birler basamağına 1 adet birlik, 1 adet beşlik boncuk ekle."

            ),TutorialStep(
                "5'i 10'luk kuralla ekleyeceğiz. Önce '10 gelir' adımını yapıp 'Kontrol et' butonuna tıklayalım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1002,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 96,
                answerNumber = 106,
                requestText = "Onlar basamağına 1 ekle. 10 gelir. 1'in kardeşi 9 gider."

            ),TutorialStep(
                "Ve 5'in büyük kardeşini çıkarıp işlemi sonlandıralım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 101,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_1003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 106,
                nextStepAbacusReset = true,
                requestText = "Birler basamağından 5 çıkar."
            ),TutorialStep(
                "Şimdi öğrendiklerini uygulama zamanı.",
                soundResource = R.raw.tutorial6_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE
            ),TutorialStep(
                "Onlara nasıl bir canavar olduğunu göster.",
                soundResource = R.raw.tutorial6_38,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE

            ),
        )

    }

    private fun createTutorialSteps7(){
        tutorialSteps7 = listOf(
            TutorialStep(
                "Bu derste 10’luk kuralın devamını göreceğiz.",
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial7_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "Aslında bir önceki derste gördüğümüz kardeşlerin tersini kullanacağız.",
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial7_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "6'nın kardeşi 4'tür.",
                soundResource = R.raw.tutorial7_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
                ),
            TutorialStep(
                "7'nin kardeşi 3",
                soundResource = R.raw.tutorial7_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "8'in kardeşi 2",
                soundResource = R.raw.tutorial7_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "9'un kardeşi 1'dir.",
                soundResource = R.raw.tutorial7_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),TutorialStep(
                "Bu kuralı abaküse 6,7,8 veya 9 sayılarından herhangi birini doğrudan ekleyemediğimiz zaman uygulayacağız.",
                soundResource = R.raw.tutorial7_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "Yani bu işlemi yaparken değil; çünkü burada 7’yi kardeşine ihtiyaç duymadan ekleyebiliyoruz.",
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                soundResource = R.raw.tutorial7_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "Yani bu işlemi yaparken değil; çünkü burada 7’yi kardeşine ihtiyaç duymadan ekleyebiliyoruz.",
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1))

            ),TutorialStep(
                "Ama bu işleme bakacak olursak. Birler basamağında 7 tane boncuğumuz olmadığı için 10'luk kuralı uygulayacağız.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                soundResource = R.raw.tutorial7_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "10 gelir.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "10 gelir.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

                ),
            TutorialStep(
                "7’nin kardeşi 3 gider.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "7’nin kardeşi 3 gider.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))

                ),
            TutorialStep(
                "Cevap 11.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "Bu işleme bakacak olursak.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                soundResource = R.raw.tutorial7_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "6'yı doğrudan ekleyemiyoruz.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Bu yüzden 10'luk kuralı uygulayacağız.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "10 gelir.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "10 gelir.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

                ),
            TutorialStep(
                "6'nın kardeşi 4 gider.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                "6'nın kardeşi 4 gider.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))
                ),
            TutorialStep(
                "Cevap 10.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Bu işlem için...",
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                soundResource = R.raw.tutorial7_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "8'i doğrudan ekleyemiyoruz.",
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L


            ),
            TutorialStep(
                "10 gelir.",
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "10 gelir.",
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),

            ),
            TutorialStep(
                "Kardeşi 2 gider.",
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Kardeşi 2 gider.",
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2))

                ),
            TutorialStep(
                "Cevap 11.",
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L


            ),
            TutorialStep(
                "Bu işlemi beraber yapalım.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                soundResource = R.raw.tutorial7_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                "Abaküse ilk sayıyı yazalım.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 4,
                soundResource = R.raw.tutorial7_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Birler basamağına 4 adet birlik boncuk ekle."


            ),
            TutorialStep(
                "Şimdi 9'u ekleyeceğiz. 9'u doğrudan mı, yoksa 10'luk kuralla mı ekleyeceğimize karar verelim.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,

            ),
            TutorialStep(
                "10'luk kuralla ekleyeceğiz.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 4,

            ),
            TutorialStep(
                "10 gelir.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 14,
                soundResource = R.raw.tutorial7_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 4,
                requestText = "Onlar basamağına 1 adet birlik boncuk ekle."

            ),
            TutorialStep(
                "9'un kardeşi 1 gider.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 13,
                soundResource = R.raw.tutorial7_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 14,
                requestText = "Birler basamağından 1 adet birlik boncuk çıkar."

            ),
            TutorialStep(
                "Teste geç.",
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            )
        )
    }

    private fun createTutorialSteps105(){
        tutorialSteps105 = listOf(
            TutorialStep(
                "10’luk toplamada “10 gelir” işlemini yaparken 5’lik veya 10’luk kuralı kullanmamız gerekebilir.",
                soundResource = R.raw.tutorial6_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.VISIBLE
            ),TutorialStep(
                "Örnek olarak bu işlemde.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial6_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "48 sayısına 7 eklemek istiyorum.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_0,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "7'yi doğrudan ekleyemiyorum. Bu yüzden 10'luk kural uygulayacağım.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yapacağım işlem: “10 gelir, 7’nin kardeşi 3 gider.”",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Fakat onlar basamağına 10’u eklerken doğrudan ekleyemiyorum.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = dpToPx(45),
                        toMarginRight = dpToPx(45),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                    }

                ),
                soundResource = R.raw.tutorial6_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Böyle olduğunda 5’lik veya 10’luk kurallardan yardım alacağız.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Burada 10 sayısını eklemek için 5’lik kuralı uygulayabilirim. Çünkü yukarıdaki 5’lik boncuğum boşta.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.AnimateSize(
                        view = focusView,
                        fromWidth = focusView.width,
                        toWidth = dpToPx(60),
                        fromHeight = focusView.height,
                        toHeight = dpToPx(100),
                        duration = 400
                    )
                    },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "5 gelir. 1'in kardeşi 4 gider.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                widgetOperations = listOf {
                    WidgetOperation.ChangeVisibility(focusView, View.GONE)},
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu şekilde 10’luk kuralındaki 10 gelir adımını gerçekleştirmiş oldum.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L)
            ,TutorialStep(
                "Şimdi 3 gider adımını yapıp işlemi sonlandıracağız.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Şimdi 3 gider adımını yapıp işlemi sonlandıracağız.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                )
            ),TutorialStep(
                "Cevap 55.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Yaptığımız işlemleri tekrar edelim.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                soundResource = R.raw.tutorial6_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "48’i abaküse yazıyorum.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial105_50,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "7'yi doğrudan ekleyemiyorum. Bu yüzden 10’luk kuralı uygulayacağım.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir 7’nin kardeşi 3 gider.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "10 gelir.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Ve kardeş 3 gider.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Ve kardeş 3 gider.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                )
            ),TutorialStep(
                "Cevap 55.",
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_top", 4),
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                rulesPanelVisibility = View.GONE,
                widgetVisibilityMap = mapOf(
                    R.id.tenRuleTable to View.VISIBLE
                )
            ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 48,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 4 adet birlik, birler basamağına 3 adet birlik, 1 adet beşlik boncuk ekle.",
                rulesPanelVisibility = View.GONE,
                ),TutorialStep(
                "2'yi 10'luk kuralla ekleyeceğiz. Önce '10 gelir' adımını yapıp 'Kontrol et' butonuna tıklayalım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1000,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 48,
                answerNumber = 58,
                requestText = "Onlar basamağına 1 ekle. 5 gelir. 1'in kardeşi 4 gider.",
                rulesPanelVisibility = View.GONE,


                ),TutorialStep(
                "Ve 2'nin büyük kardeşini çıkarıp işlemi sonlandıralım.",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 50,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_1001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 58,
                nextStepAbacusReset = true,
                requestText = "Birler basamağından 8 çıkar.",
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Bir de bu örneğe bakalım.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "İlk sayıyı abaküse yazıyorum.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                soundResource = R.raw.tutorial6_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,



                ),TutorialStep(
                "Şimdi 4 sayısını 10’luk kuralla ekleyeceğim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "10 gelir. 4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Onlar basamağına 10 gelebilmesi için bu sefer 10’luk kuralı uygulamamız gerekir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    { WidgetOperation.AnimateSize(
                        view = focusView,
                        fromWidth = focusView.width,
                        toWidth = dpToPx(60),
                        fromHeight = focusView.height,
                        toHeight = dpToPx(250),
                        duration = 400
                    )
                    },
                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = dpToPx(45),
                        toMarginRight = dpToPx(45),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                    }

                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "10 gelir. 1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf (
                    {WidgetOperation.ChangeVisibility(focusView, View.GONE)}),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                },
                soundResource = R.raw.tutorial6_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,


                ),TutorialStep(
                "10 gelir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                soundResource = R.raw.tutorial6_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "1’in büyük kardeşi 9 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),

                    ),
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Şimdi de 4’ün büyük kardeşi 6’yı götüreceğiz.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Şimdi de 4’ün büyük kardeşi 6’yı götüreceğiz.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                "Ve cevap 102.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Yaptığımız işlemleri tekrar edelim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2)),
                soundResource = R.raw.tutorial6_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "98’i abaküse yazıyorum.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                soundResource = R.raw.tutorial6_31,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,


                ),TutorialStep(
                "4 sayısını 10’luk kuralla ekleyeceğim.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "10 gelir. 4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "10 gelir.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                soundResource = R.raw.tutorial6_34,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "4’ün büyük kardeşi 6 gider.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Cevap 102.",
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "İlk sayıyı abaküse yazalım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 96,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = "Onlar basamağına 4 adet birlik, 1 adet beşlik, Birler basamağına 1 adet birlik, 1 adet beşlik boncuk ekle.",
                rulesPanelVisibility = View.GONE,


                ),TutorialStep(
                "5'i 10'luk kuralla ekleyeceğiz. Önce '10 gelir' adımını yapıp 'Kontrol et' butonuna tıklayalım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1002,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 96,
                answerNumber = 106,
                requestText = "Onlar basamağına 1 ekle. 10 gelir. 1'in kardeşi 9 gider.",
                rulesPanelVisibility = View.GONE,


                ),TutorialStep(
                "Ve 5'in büyük kardeşini çıkarıp işlemi sonlandıralım.",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 101,
                abacusClickable = true,
                soundResource = R.raw.tutorial5_1003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 106,
                nextStepAbacusReset = true,
                requestText = "Birler basamağından 5 çıkar.",
                rulesPanelVisibility = View.GONE,

                ),TutorialStep(
                "Şimdi öğrendiklerini uygulama zamanı.",
                soundResource = R.raw.tutorial6_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE,

            ),TutorialStep(
                "Onlara nasıl bir canavar olduğunu göster.",
                soundResource = R.raw.tutorial6_38,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE

            ),
        )
    }
    
    private fun createTutorialSteps8(){
        tutorialSteps8 = listOf(
            TutorialStep(
                "Bu derste abaküste toplamanın son kuralı olan boncuk kuralını göreceğiz.",
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Bu kural 6,7,8 veya 9 sayılarıyla toplama yaparken kullanılır.",
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Normalde 6 sayısı ile toplama yaparken öğrendiğimiz 2 tane yöntem vardı.",
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "1. yöntem, doğrudan ekleme.",
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Örneğin bu işlemi yaparken doğrudan ekleme yaparız. Yani hiçbir kural uygulamadan doğrudan ekleriz.",
                questionText = "1 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                soundResource = R.raw.tutorial8_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Örneğin bu işlemi yaparken doğrudan ekleme yaparız. Yani hiçbir kural uygulamadan doğrudan ekleriz.",
                questionText = "1 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))

            ),TutorialStep(
                "2. yöntem, 10’luk kural ile ekleme.",
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                soundResource = R.raw.tutorial8_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Bu işlemde 6’yı doğrudan ekleyemediğimiz için 10’luk kuralı uyguluyorduk. 10 gelir. Kardeşi 4 gider.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                soundResource = R.raw.tutorial8_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "10 gelir.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                soundResource = R.raw.tutorial8_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                "Kardeşi 4 gider.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Kardeşi 4 gider.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))

            ),TutorialStep(
                "Cevap 10.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Bu kuralları uygulayamadığımız zamanlarda boncuk kuralını uygularız.",
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                soundResource = R.raw.tutorial8_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                options = listOf("kuralsız toplama","10'luk kural","beşlik kural"),
                correctOptionIndex = listOf(0,1),
                multipleChoice = true,
                optionText = "Boncuk kuralını hangi kuralları uygulayamadığımız zaman kullanırız ?",
                requestText = "Kuralsız ve 10'luk toplama yapamadığımızda boncuk kuralını uygularız."


            ),TutorialStep(
                "Örneğin bu işlemde 6’yı doğrudan ekleyemiyoruz.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                soundResource = R.raw.tutorial8_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "10’luk kuralı uygulamak istesek 10 gelir ama kardeşi 4 gidemez.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                soundResource = R.raw.tutorial8_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                "Yani 6,7,8,9 sayılarından herhangi birini eklerken eğer sayının kardeşini götüremiyorsak. O zaman boncuk kuralını uygulayacağız.",
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_14,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Örneğin bu işlemde 10’luk kuralı uygulayabilirim; çünkü 6’nın kardeşi 4 gider diyebilirim.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_16
                ),
            TutorialStep(
                "10 gelir.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
                ),
            TutorialStep(
                "Kardeşi 4 gider.",
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_14,
                ),
            TutorialStep(
                "Bu işlemde 10’luk kuralı uygulayamam; çünkü 6’nın kardeşi 4 gider diyemem.",
                questionText = "8 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_17
            ),
            TutorialStep(
                "10 gelir.",
                questionText = "8 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
            ),
            TutorialStep(
                "Ama kardeşi 4 gidemez.",
                questionText = "8 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1000
            ),TutorialStep(
                "Bu işleminde 10’luk kuralı uygulayabilirim; çünkü 7’nin kardeşi 3 gider diyebilirim.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_18
            ),TutorialStep(
                "10 gelir.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
            ),
            TutorialStep(
                "Kardeşi 3 gider.",
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_22,
            ),TutorialStep(
                "Bu işlemde 10’luk kuralı uygulayamam; çünkü 7’nin kardeşi 3 gider diyemem.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_19


            ),TutorialStep(
                "10 gelir.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
            ),
            TutorialStep(
                "Ama kardeşi 3 gidemez.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1001
            ),
            TutorialStep(
                "Bu işlemde neden boncuk kuralını kullanmamız gerekiyordu ?",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1010,
                options = listOf("7'yi kuralsız ve 10'luk kural ile ekleyemediğimiz için","Boncuk kuralı daha kolay olduğu için","5'lik kuralı kullanamadığımız için"),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = "Bu işlemde neden boncuk kuralını kullanmamız gerekiyordu ?",
                requestText = "Sadece kuralsız ve 10'luk toplama yapamadığımızda boncuk kuralını uygularız."
            ),TutorialStep(
                "Bu işlemde 10’luk kuralı uygulayabilirim; çünkü 8’in kardeşi 2 gider diyebilirim.",
                questionText = "2 + 8",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_100
            ),TutorialStep(
                "10 gelir.",
                questionText = "2 + 8",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
            ),
            TutorialStep(
                "Kardeşi 2 gider.",
                questionText = "2 + 8",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_30,
            ),TutorialStep(
                "Bu işlemde 10’luk kuralı uygulayamam; çünkü 8’in kardeşi 2 gider diyemem.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_20
            ),TutorialStep(
                "10 gelir.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
            ),
            TutorialStep(
                "Ama kardeşi 2 gidemez.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1002
            ),TutorialStep(
                "Bu işlemde 10’luk kuralı uygulayabilirim; çünkü 9’un kardeşi 1 gider diyebilirim.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_21
            ),TutorialStep(
                "10 gelir.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
            ),
            TutorialStep(
                "Kardeşi 1 gider.",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_37,
            ),TutorialStep(
                "Bu işlemde 10’luk kuralı uygulayamam; çünkü 9’un kardeşi 1 gider diyemem.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_22
            ),TutorialStep(
                "10 gelir.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_20
            ),
            TutorialStep(
                "Ama kardeşi 1 gidemez.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1003
            ),TutorialStep(
                "İşte boncuk kuralını bunu diyemediğimiz zamanlar uygulayacağız.",
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_23

            ),TutorialStep(
                "Kural şu şekildedir.",
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_24

            ),TutorialStep(
                "6 için: 1 boncuk gelir, 5 gider, 10 gelir.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_25

            ),TutorialStep(
                "7 için: 2 boncuk gelir, 5 gider, 10 gelir.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_26

            ),TutorialStep(
                "8 için: 3 boncuk gelir, 5 gider, 10 gelir.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_27
            ),TutorialStep(
                "9 için: 4 boncuk gelir, 5 gider, 10 gelir.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_28

            ),TutorialStep(
                "Bu kurala göre bu işlemi yapacak olursak.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_29

            ),TutorialStep(
                "1 boncuk gelir.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_30

            ),TutorialStep(
                "1 boncuk gelir.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1))

            ),TutorialStep(
                "5 gider.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_31


            ),TutorialStep(
                "5 gider.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))

            ),TutorialStep(
                "10 gelir.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_32

            ),TutorialStep(
                "10 gelir.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

            ),TutorialStep(
                "Cevap 12.",
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_33

            ),TutorialStep(
                "Bu işlem için...",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_34
            ),TutorialStep(
                "2 boncuk gelir.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_35
            ),TutorialStep(
                "2 boncuk gelir.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1))
            ),TutorialStep(
                "5 gider.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_36
            ),TutorialStep(
                "5 gider.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                "10 gelir.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_37
            ),TutorialStep(
                "10 gelir.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))
            ),TutorialStep(
                "Cevap 12.",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_38
            ),TutorialStep(
                "Bu işlem için...",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_39
            ),TutorialStep(
                "3 boncuk gelir.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_40

            ),TutorialStep(
                "3 boncuk gelir.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))

            ),TutorialStep(
                "5 gider.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_41

            ),TutorialStep(
                "5 gider.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))

            ),TutorialStep(
                "10 gelir.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_42

            ),TutorialStep(
                "10 gelir.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

            ),TutorialStep(
                "Cevap 14.",
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_43

            ),TutorialStep(
                "Bu işlem için...",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_44

            ),TutorialStep(
                "4 boncuk gelir.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_45

            ),TutorialStep(
                "4 boncuk gelir.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))

            ),TutorialStep(
                "5 gider.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_41

            ),TutorialStep(
                "5 gider.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))

            ),TutorialStep(
                "10 gelir.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_46

            ),TutorialStep(
                "10 gelir.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

            ),TutorialStep(
                "Cevap 14.",
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_47

            ),TutorialStep(
                "Şimdi öğrendiklerimizi uygulayalım.",
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_48

            )
        )
    }
    
    private fun createTutorialSteps9(){
        tutorialSteps9 = listOf(
            TutorialStep(
                "Boncuk kuralını, bir örnekle iki basamaklı sayılarda nasıl kullanacağımızı görelim.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1
            ),
            TutorialStep(
                "Örneğin bu soruda...",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_2
            ),
            TutorialStep(
                "Önce 68 sayısını abaküse yazıyoruz.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_3
            ),
            TutorialStep(
                "Sonrasında, ekleyeceğimiz sayının en büyük basamağından başlayıp topluyoruz.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_4,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "Onlar basamağında 6’ya 7’yi hangi kural ile ekleyebileceğime bakıyorum.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_5,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "Doğrudan ekleyemem. Ekleyebileceğim 7 adet boncuk yok.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_6,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "10’luk kuralla da ekleyemem; çünkü “10 gelir, 7’nin kardeşi 3 gider” derken 3’ü çıkaramam.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_7,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "O zaman boncuk kuralını uygulayacağım.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_8,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "2 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_9,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "2 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)),
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "5 gider.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_10,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "5 gider.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4)),
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "10 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_11,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "10 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                "Onlar basamağını ekledim. Şimdi birler basamağını ekleyeceğim.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_12,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "Birler basamağında 8’e 6’yı hangi kural ile ekleyeceğime bakıyorum.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_13,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "Doğrudan ekleyemem. Ekleyebileceğim 6 adet boncuk yok.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_14,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "10’luk kuralla ekleyemem; çünkü “10 gelir, 6’nın kardeşi 4 gider” derken 4’ü çıkaramam.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_15,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "O zaman boncuk kuralını uygulayacağım.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_16,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "1 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_17,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "1 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "5 gider.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_18,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "5 gider.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4)),
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "10 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_19,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "10 gelir.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "Cevap 144.",
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_20
            ),
            TutorialStep(
                "Beraber bir örnek yapalım.",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_21,
                abacusReset = true,

                ),
            TutorialStep(
                "Önce ilk sayıyı abaküse yaz.",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 58,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                abacusReset = true
            ),
            TutorialStep(
                "Şimdi de 2. sayının onlar basamağını abaküsümüzdeki sayıya hangi kural ile ekleyeceğine karar ver.",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1001,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                ),
                ),TutorialStep(
                "Hangi kural ile ekleyeceksin ?",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1020,
                backAnswerNumber = 58,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                ),
                options = listOf("Kuralsız toplama","Onluk toplama","Boncuk kuralı"),
                correctOptionIndex = listOf(2),
                multipleChoice = false,
                optionText = "8'i onlar basamağına hangi kural ile ekleyeceksin ?",
                requestText = "Kuralsız ve onluk toplama yapamadığımız için boncuk kuralını uygulayacağız."
                ),
            TutorialStep(
                "8'i boncuk kuralına göre onlar basamağına ekle.",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 138,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1003,
                backAnswerNumber = 58,
                requestText = "Onlar basamağında 4 gelir, 5 gider, 10 gelir işlemlerini uygula.",
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
                ),
            TutorialStep(
                "Şimdi sıra birler basamağında.",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_27,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                "8’e 9'u hangi kuralla ekleyeceğine karar ver.",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1004,
                backAnswerNumber = 138,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                ),
            ),TutorialStep(
                "Hangi kural ile ekleyeceksin ?",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1020,
                backAnswerNumber = 138,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                ),
                options = listOf("Kuralsız toplama","Onluk toplama","Boncuk kuralı"),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                optionText = "9'u birler basamağına hangi kural ile ekleyeceksin ?",
                requestText = "Onluk toplama kuralını uygulayabildiğimiz için onluk kuralı yapıyoruz."
            ),

            TutorialStep(
                "9’u onluk kurala göre birler basamağına ekle.",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 147,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1005,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                ),
                backAnswerNumber = 138
            ),
            TutorialStep(
                "Teste geç.",
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps10(){
        tutorialSteps10 = listOf(
            TutorialStep(
                "Kuralsız çıkarma, herhangi bir kural uygulamadan abaküs üzerinde yapılan basit çıkarma işlemlerini ifade eder.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_1
            ),
            TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_2
            ),
            TutorialStep(
                "Önce 8 sayısını abaküse yazarız.",
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_3
            ),
            TutorialStep(
                "Önce 8 sayısını abaküse yazarız.",
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),
            TutorialStep(
                "Sonra 6 değere sahip boncukları çıkarırız.",
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_4

            ),
            TutorialStep(
                "Sonra 6 değere sahip boncukları çıkarırız.",
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                "Cevap 2.",
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_5
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6
            ),
            TutorialStep(
                "78 sayısını abaküse yazıyoruz.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_7
            ),
            TutorialStep(
                "Sonrasında, çıkaracağımız sayının en büyük basamağından başlayarak işlemleri yapıyoruz.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_8
            ),
            TutorialStep(
                "Önce 60'ı çıkarıyoruz.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_9
            ),
            TutorialStep(
                "Önce 60'ı çıkarıyoruz.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4))
            ),
            TutorialStep(
                "Sonra 3'ü çıkarıp işlemi sonlandırıyoruz.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_10
            ),
            TutorialStep(
                "Sonra 3'ü çıkarıp işlemi sonlandırıyoruz.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2))
            ),
            TutorialStep(
                "Cevap 15.",
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_11
            ),
            TutorialStep(
                "Bu işlemi beraber yapalım.",
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_12
            ),
            TutorialStep(
                "Önce 94'ü abaküse yazalım.",
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 94,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_13
            ),
            TutorialStep(
                "Şimdi ise 2. sayının en büyük basamağından başlayarak çıkarma işlemini yapalım.",
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_14
            ),
            TutorialStep(
                "İlk, 50 sayısını çıkaracağız.",
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 44,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_15
            ),
            TutorialStep(
                "Şimdi ise 2 sayısını çıkarıp işlemi bitireceğiz.",
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_16
            ),
            TutorialStep(
                "Ve cevap 42.",
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_17
            ),
            TutorialStep(
                "Şimdi senin sıran.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_18
            )
        )
    }
    
    private fun createTutorialSteps11(){
        tutorialSteps11 = listOf(
            TutorialStep(
                "5’lik çıkarmaya, 5’lik toplamanın tersi diyebiliriz.",
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_1
            ),
            TutorialStep(
                "Sayılarımız ve kardeşleri aynıdır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_2

            ),
            TutorialStep(
                "Tek farkı, 5’lik toplamada “5 gelir, kardeş gider” diyorduk.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_3
            ),
            TutorialStep(
                "5’lik çıkarmada “5 gider, kardeş gelir” kuralını uyguluyoruz.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_4
            ),
            TutorialStep(
                "Bu kuralı, kuralsız çıkarma yapamadığımız zaman kullanacağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_5
            ),
            TutorialStep(
                "Örneğin bu işlemi yapalım.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_6
            ),
            TutorialStep(
                "Öncelikle ilk sayıyı abaküse yazıyoruz.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_7
            ),
            TutorialStep(
                "1'i doğrudan çıkaramıyoruz, bu yüzden 5'lik çıkarma kuralını uygulayacağız.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_8
            ),
            TutorialStep(
                "1’in kardeşi 4.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_9
            ),
            TutorialStep(
                "5 gider.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_10
            ),
            TutorialStep(
                "5 gider.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_11
            ),
            TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                "Cevap 4.",
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_12
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_13
            ),
            TutorialStep(
                "İlk sayıyı abaküse yazıyoruz.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_14
            ),
            TutorialStep(
                "3'ü doğrudan çıkaramıyoruz.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_15
            ),
            TutorialStep(
                "Bu yüzden 5'lik çıkarma kuralını uygulayacağız.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_16
            ),
            TutorialStep(
                "5 gider.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_17
            ),
            TutorialStep(
                "5 gider.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                "Kardeşi 2 gelir.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_18
            ),
            TutorialStep(
                "Kardeşi 2 gelir.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1))
            ),
            TutorialStep(
                "Cevap 3.",
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_19
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_20
            ),
            TutorialStep(
                "İlk sayıyı abaküse yazıyorum.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_21
            ),
            TutorialStep(
                "8'i doğrudan çıkaramıyorum. Bu yüzden 5'lik kuralı uygulayacağım.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_22
            ),
            TutorialStep(
                "5 gider.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_23
            ),
            TutorialStep(
                "5 gider.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                "Kardeşi 1 gelir.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_24
            ),
            TutorialStep(
                "Kardeşi 1 gelir.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                "Cevap 4.",
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_25
            ),
            TutorialStep(
                "Şimdi öğrendiklerimizi uygulayalım.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_26
            )

        )
    }
    
    private fun createTutorialSteps12(){
        tutorialSteps12 = listOf(
            TutorialStep(
                "10’luk çıkarmaya, 10’luk toplamanın tersi diyebiliriz.",
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_1
            ),
            TutorialStep(
                "Sayılar ve kardeşleri 10'luk toplamadakiyle aynıdır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_2
            ),
            TutorialStep(
                "Tek farkı, 10’luk toplamada “10 gelir, kardeş gider” diyorduk.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_3
            ),
            TutorialStep(
                "10’luk çıkarmada “10 gider, kardeş gelir” kuralını uygulayacağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_4
            ),
            TutorialStep(
                "Bu kuralı, kuralsız veya 5'lik çıkarma yapamadığımız zaman uygulayacağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_5
            ),
            TutorialStep(
                "Örneğin, bu işlemde bu kuralı uygulamaya gerek yok.",
                questionTextVisibility = View.VISIBLE,
                questionText = "8 - 6",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_6
            ),
            TutorialStep(
                "Çünkü 6’yı kuralsız olarak çıkarabiliyoruz.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionTextVisibility = View.VISIBLE,
                questionText = "8 - 6",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_7
            ),
            TutorialStep(
                "Çünkü 6’yı kuralsız olarak çıkarabiliyoruz.",
                questionTextVisibility = View.VISIBLE,
                questionText = "8 - 6",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                "Cevap 2.",
                questionTextVisibility = View.VISIBLE,
                questionText = "8 - 6",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_8
            ),
            TutorialStep(
                "Bu işlemde ise...",
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_9
            ),
            TutorialStep(
                "5’lik kuralı uygulayabiliyoruz. Çünkü gidebilecek 5’lik boncuk var.",
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_10
            ),
            TutorialStep(
                "5 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_11
            ),
            TutorialStep(
                "5 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                "3’ün kardeşi 2 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_12
            ),
            TutorialStep(
                "3’ün kardeşi 2 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1))
            ),
            TutorialStep(
                "Cevap 2.",
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_13
            ),
            TutorialStep(
                "Fakat bu işlemde...",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_14
            ),
            TutorialStep(
                "Gidebilecek 5’lik boncuk olmadığı için 10’luk kuralı uygulayacağız.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_15
            ),
            TutorialStep(
                "1’in kardeşi 9.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_16
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                "Kardeşi 9 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_18
            ),
            TutorialStep(
                "Kardeşi 9 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),
            TutorialStep(
                "Cevap 9.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_30
            ),
            TutorialStep(
                "Bu işlem için...",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_26
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                "Kardeşi 8 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_21
            ),
            TutorialStep(
                "Kardeşi 8 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),
            TutorialStep(
                "Cevap 8.",
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_22
            ),
            TutorialStep(
                "Bu işlem için...",
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_26
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                "Kardeşi 7 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_24
            ),
            TutorialStep(
                "Kardeşi 7 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),
            TutorialStep(
                "Cevap 9.",
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_30
            ),
            TutorialStep(
                "Bu işlem için...",
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_26
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                "Kardeşi 1 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_27
            ),
            TutorialStep(
                "Kardeşi 1 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1))
            ),
            TutorialStep(
                "Cevap 6.",
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_28
            ),
            TutorialStep(
                "Bu işlem için...",
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_26
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                "Kardeşi 2 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_29
            ),
            TutorialStep(
                "Kardeşi 2 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                "Cevap 9.",
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_30
            ),
            TutorialStep(
                "Bu işlem için...",
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_26

            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                "10 gider.",
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                "Kardeşi 3 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_31
            ),
            TutorialStep(
                "Kardeşi 3 gelir.",
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                "Cevap 4.",
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_32
            ),
            TutorialStep(
                "Şimdi senin için hazırladığım soruları çözerek öğrendiklerimizi uygula.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_33
            ),

            )
    }
    
    private fun createTutorialSteps13(){
        tutorialSteps13 = listOf(
            TutorialStep(
                "İki basamaklı sayılarda 10'luk çıkarmaya örnek verelim.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_1
            ),
            TutorialStep(
                "Örneğin bu işlemde...",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                "Önce 128 sayısını abaküse yazıyoruz.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_3
            ),
            TutorialStep(
                "Sonrasında çıkarılacak sayının en büyük basamağından başlayarak kuralları uyguluyoruz.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_4
            ),
            TutorialStep(
                "10’lar basamağından 3 çıkarmak istiyorum.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(45),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(60),
                            fromHeight = 0,
                            toHeight = dpToPx(240),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_5
            ),
            TutorialStep(
                "3’ü doğrudan çıkaramıyoruz. Kurallara başvurmak zorundayız.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_6
            ),
            TutorialStep(
                "5’lik kuralı uygulamak için 5’lik boncuğa ihtiyacımız var.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_7
            ),
            TutorialStep(
                "Ama kullanabileceğim 5’lik boncuk yok.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                { WidgetOperation.ChangeMargin(focusView, 125, 0) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(60),
                            duration = 200
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_8
            ),
            TutorialStep(
                "Bu yüzden 10’luk kuralı uygulayacağız.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_9
            ),
            TutorialStep(
                "10 gider.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_10
            ),
            TutorialStep(
                "10 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2)),
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE
            ),
            TutorialStep(
                "3’ün kardeşi 7 gelir.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_11
            ),
            TutorialStep(
                "3’ün kardeşi 7 gelir.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE
            ),
            TutorialStep(
                "Onlar basamağını çıkardık. Şimdi sıra birler basamağında.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_12
            ),
            TutorialStep(
                "9'u doğrudan çıkaramıyorum. Bu yüzden 10'luk kuralla çıkaracağım.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_13
            ),
            TutorialStep(
                "10 gider.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_10
            ),
            TutorialStep(
                "10 gider.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 2)),
            ),
            TutorialStep(
                "9'un kardeşi 1 gelir.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_14
            ),
            TutorialStep(
                "9'un kardeşi 1 gelir.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
            ),
            TutorialStep(
                "Cevap 89.",
                questionText = " 128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_15
            ),
            TutorialStep(
                "Bu işlemi beraber yapalım.",
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_16

            ),
            TutorialStep(
                "Önce ilk sayıyı abaküse yaz.",
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 165,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_17
            ),
            TutorialStep(
                "Sonrasında, 2. sayının en büyük basamağından başlayarak çıkarma işlemini yapalım.",
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_18
            ),
            TutorialStep(
                "Önce onlar basamağından 9'u çıkaralım.",
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 75,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_19
            ),
            TutorialStep(
                "Şimdi de birler basamağından 6'yı çıkaralım.",
                answerNumber = 69,
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_20
            ),
            TutorialStep(
                "Ve cevap 69.",
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_21
            ),
            TutorialStep(
                "Şimdi test zamanı.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_22
            ),


        )
    }
    
    private fun createTutorialSteps14(){
        tutorialSteps14 = listOf(
            TutorialStep(
                "10’luk çıkarma yaparken 5’lik çıkarmayı kullanmamız gerekebilir.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_1
            ),
            TutorialStep(
                "Örneğin bu işlemde...",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                "İlk sayıyı abaküse yazıyorum.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_2
            ),
            TutorialStep(
                "Şimdi 5’i, 10’luk kural ile çıkaracağım.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_3
            ),
            TutorialStep(
                "10 gider. Kardeşi 5 gelir.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_4
            ),
            TutorialStep(
                "10 gider adımını uygularken, onlar basamağından 1’i doğrudan çıkaramıyorum.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(45),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(60),
                            fromHeight = 0,
                            toHeight = dpToPx(240),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_5

            ),
            TutorialStep(
                "Böyle durumlarda 5’lik veya 10’luk çıkarmadan yardım alacağız.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_6
            ),
            TutorialStep(
                "Bu işlemde 5’lik çıkarmadan yardım alacağız. Çünkü yukarıda 5’lik boncuğum mevcut.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    { WidgetOperation.ChangeMargin(focusView, 125, 0) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 200
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_7

            ),
            TutorialStep(
                "1’in kardeşi 4’tür.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_8
            ),
            TutorialStep(
                "5 gider.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_9
            ),
            TutorialStep(
                "5 gider.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4)),
            ),
            TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_10
            ),
            TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)),
            ),
            TutorialStep(
                "Bu şekilde 10’luk kuralımdaki 10 gider adımını gerçekleştirmiş oldum.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_11
            ),
            TutorialStep(
                "Şimdi 5 gelir adımını yapıp işlemi sonlandıracağım.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_12
            ),
            TutorialStep(
                "Şimdi 5 gelir adımını yapıp işlemi sonlandıracağım.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),
            TutorialStep(
                "Cevap 45.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_13
            ),
            TutorialStep(
                "Yaptığımız işlemleri tekrar edelim.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial6_30
            ),
            TutorialStep(
                "50’yi abaküse yazıyorum.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_15
            ),
            TutorialStep(
                "5’i doğrudan çıkaramıyorum. Bu yüzden 10’luk kuralla çıkaracağım.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_16
            ),
            TutorialStep(
                "10 gider. 5’in kardeşi 5 gelir.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_17
            ),
            TutorialStep(
                "10 gider.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_18
            ),
            TutorialStep(
                "Ve kardeş 5 gelir.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_19
            ),
            TutorialStep(
                "Ve kardeş 5 gelir.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),
            TutorialStep(
                "Cevap 45.",
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_13
            ),
            TutorialStep(
                "10 gider adımını uygularken 10’luk çıkarma da uygulamamız gerekebilir.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_20
            ),
            TutorialStep(
                "Örneğin bu işlemde...",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_21
            ),
            TutorialStep(
                "İlk sayıyı abaküse yazıyorum.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_22
            ),
            TutorialStep(
                "Şimdi 3’ü, 10’luk kural ile çıkaracağım.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_23
            ),
            TutorialStep(
                "10 gider. Kardeşi 7 gelir.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_24
            ),
            TutorialStep(
                "10 gider adımını uygularken, onlar basamağından 1’i doğrudan çıkaramıyorum.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(45),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(60),
                            fromHeight = 0,
                            toHeight = dpToPx(240),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_25

            ),
            TutorialStep(
                "Yukarıda 5’lik kural için kullanabileceğim ekstra 5’lik boncukta yok.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    { WidgetOperation.ChangeMargin(focusView, 125, 0) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(60),
                            duration = 200
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_26

            ),
            TutorialStep(
                "O yüzden 10’luk kural ile çıkaracağım.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_27
            ),
            TutorialStep(
                "1’in kardeşi 9.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_28
            ),
            TutorialStep(
                "10 gider 9 gelir.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_29
            ),
            TutorialStep(
                "10 gider.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_30
            ),
            TutorialStep(
                "9 gelir.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_31
            ),
            TutorialStep(
                "9 gelir.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
            ),
            TutorialStep(
                "3’ü çıkarırken yaptığım 10 gider adımını tamamlamış oldum.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_32
            ),
            TutorialStep(
                "Son olarak 3’ün kardeşi 7 gelir ve işlem biter.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_33
            ),
            TutorialStep(
                "Son olarak 3’ün kardeşi 7 gelir ve işlem biter.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),
            TutorialStep(
                "Cevap 97.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_34
            ),
            TutorialStep(
                "Yaptığımız işlemleri tekrarlayalım.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_35
            ),
            TutorialStep(
                "100’ü abaküse yazıyorum.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_36
            ),
            TutorialStep(
                "3’ü doğrudan veya 5’lik kural ile çıkaramıyorum. 10’luk kural ile çıkaracağım.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_37
            ),
            TutorialStep(
                "10 gider. 3’ün kardeşi 7 gelir.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_38
            ),
            TutorialStep(
                "10 gider.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_39
            ),
            TutorialStep(
                "Kardeşi 7 gelir.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_40

            ),
            TutorialStep(
                "Kardeşi 7 gelir.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),
            TutorialStep(
                "Cevap 97.",
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_34
            ),
            TutorialStep(
                "Bu işlemi beraber yapalım.",
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_41

            ),
            TutorialStep(
                "Önce ilk sayıyı abaküse yaz.",
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 100,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_42
            ),
            TutorialStep(
                "Şimdi 2. sayıyı çıkaralım.",
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_43
            ),
            TutorialStep(
                "10 gider. 9’un kardeşi 1 gelir.",
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 91,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_44
            ),
            TutorialStep(
                "Cevap 91.",
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_45
            ),
            TutorialStep(
                "Bir de bu işleme bakalım.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                onStepComplete = { resetAbacus() },
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_46

            ),
            TutorialStep(
                "Önce ilk sayıyı abaküse yaz.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 530,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_47
            ),
            TutorialStep(
                "Şimdi 2. sayının en büyük basamağından başlayarak çıkarma işlemini yapalım.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_48
            ),
            TutorialStep(
                "Önce onlar basamağını çıkaralım.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_49
            ),
            TutorialStep(
                "4’ün kardeşi 6'dır. 10 gider. 6 gelir.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 490,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_50
            ),
            TutorialStep(
                "Şimdi birler basamağını çıkaralım.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_51
            ),
            TutorialStep(
                "1’in kardeşi 9. 10 gider. 9 gelir.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 489,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_52
            ),
            TutorialStep(
                "Cevap 489.",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_53
            ),
            TutorialStep(
                "Şimdi teste geçelim.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_54
            )
        )
    }
    
    private fun createTutorialSteps15(){
        tutorialSteps15 = listOf(
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6
            ),TutorialStep(
                "4’ü doğrudan yada 5’lik kural ile çıkaramıyorum. O zaman 10’luk kural ile çıkaracağım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(60),
                            fromHeight = 0,
                            toHeight = dpToPx(240),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_2

            ),TutorialStep(
                "10 gider. Kardeşi 6 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_3
            ),TutorialStep(
                "10 gider adımını yapacağım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_4
            ),TutorialStep(
                "Onlar basamağından 1 çıkaracağım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(45),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(60),
                            fromHeight = 0,
                            toHeight = dpToPx(240),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_5

            ),TutorialStep(
                "Ama 1’i doğrudan ya da 5’lik kural ile çıkaramıyorum.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_6
            ),TutorialStep(
                "O zaman 10’luk kural ile çıkaracağım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_7
            ),TutorialStep(
                "10 gider. Kardeşi 9 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_8
            ),TutorialStep(
                "Yüzler basamağından 1 çıkaracağım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(90),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(60),
                            fromHeight = 0,
                            toHeight = dpToPx(240),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_9

            ),TutorialStep(
                "Doğrudan çıkaramıyorum. Yukarıda 5’lik boncuğum var. 5’lik kural uygulayacağım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_10
            ),TutorialStep(
                "5 gider. Kardeşi 4 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_11
            ),TutorialStep(
                "5 gider.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_12
            ),TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_13
            ),TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom4", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1)),
            ),TutorialStep(
                "Şimdi bir önceki adımdaki 9 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_14
            ),TutorialStep(
                "Şimdi bir önceki adımdaki 9 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
            ),TutorialStep(
                "Ve son olarak ilk adımdaki 4’ün kardeşi 6 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_15
            ),TutorialStep(
                "Ve son olarak ilk adımdaki 4’ün kardeşi 6 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),TutorialStep(
                "Cevap 496.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_16
            ),TutorialStep(
                "Yaptığımız işlemleri tekrarlayalım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom4", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_17
            ),TutorialStep(
                "4’ü 10’luk kural ile çıkarıyorum.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_18
            ),TutorialStep(
                "10 gider. 4’ün kardeşi 6 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_19
            ),TutorialStep(
                "10 gider adımını 10’luk kural ile çıkaracağım.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_20
            ),TutorialStep(
                "10 gider. Kardeşi 9 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_21
            ),TutorialStep(
                "10 gider.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom4", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_22
            ),TutorialStep(
                "Kardeşi 9 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_23
            ),TutorialStep(
                "Kardeşi 9 gelir.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
            ),TutorialStep(
                "İlk adımdaki 10 gider. 4’ün kardeşi 6 gelirdeki 10 gider adımını tamamladık.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_24
            ),TutorialStep(
                "En son olarak 6 geliyor ve işlem bitiyor.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_25
            ),TutorialStep(
                "En son olarak 6 geliyor ve işlem bitiyor.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),TutorialStep(
                "Cevap 496.",
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_26
            ),TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps16(){
        tutorialSteps16 = listOf(
            TutorialStep(
                "Boncuk çıkarmanın 10’luk çıkarmadan tek farkı 5’lik toplamayı kullanıyor olmamız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1

            ),
            TutorialStep(
                "5’lik toplamayı hatırlamıyorsan geri dönüp gözden geçirebilirsin.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_2

            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "12 - 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6

            ),
            TutorialStep(
                "8'i 10'luk kural ile çıkaracağım.",
                questionText = "12 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_3

            ),
            TutorialStep(
                "10 gider.",
                questionText = "12 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                "10 gider.",
                questionText = "12 - 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),

            ),
            TutorialStep(
                "8’in kardeşi 2 gelir.",
                questionText = "12 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_4
            ),
            TutorialStep(
                "8’in kardeşi 2 gelir.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)),
                questionText = "12 - 8",
                questionTextVisibility = View.VISIBLE
            ),
            TutorialStep(
                "Burada 10’luk kuralı sorunsuz uygulayabildik.",
                questionText = "12 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_5

            ),
            TutorialStep(
                "Fakat bu işlemde...",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_14

            ),
            TutorialStep(
                "6'yı 10'luk kural ile çıkaracağız.",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_7
            ),
            TutorialStep(
                "10 gider.",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                "10 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                "Kardeşi 4 kuralsız gelemez.",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_8

            ),
            TutorialStep(
                "Burada 5’lik toplama kuralını uyguluyoruz.",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_9

            ),
            TutorialStep(
                "Birler basamağına 4 eklemek için 4’ün kardeşi 1 gider, 5 gelir.",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_10

            ),
            TutorialStep(
                "1 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_11

            ),
            TutorialStep(
                "5 gelir.",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "5 gelir.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                "Cevap 5.",
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Bu işlem için...",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)),
                soundResource = R.raw.tutorial5_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "10'luk kural ile çıkaracağım.",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_13

            ),
            TutorialStep(
                "10 gider.",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                "10 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                "7’nin kardeşi 3 gelir.",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_14

            ),
            TutorialStep(
                "3’ü eklemek için 5’lik kuralı uyguluyoruz.",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_15

            ),
            TutorialStep(
                "5 gelir.",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "5 gelir.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                "Kardeşi 2 gider.",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Kardeşi 2 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE

            ),
            TutorialStep(
                "Cevap 5.",
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Bu işlem için,",
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                soundResource = R.raw.tutorial5_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "10 gider.",
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                "10 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                "8’in kardeşi 2 gelir.",
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_16

            ),
            TutorialStep(
                "8’in kardeşi 2 gelir.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                "Cevap 5.",
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "Bu işlem için,",
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)),
                soundResource = R.raw.tutorial5_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L


            ),
            TutorialStep(
                "10 gider.",
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                "10 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true

            ),
            TutorialStep(
                "9’un kardeşi 1 gelir.",
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_17

            ),
            TutorialStep(
                "9’un kardeşi 1 gelir.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27

            )
        )
    }
    
    private fun createTutorialSteps17(){
        tutorialSteps17 = listOf(
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6
                ),
            TutorialStep(
                "İlk sayıyı abaküse yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
                ),

            TutorialStep(
                "Ve çıkarılacak sayının en büyük basamağından başlayarak işlemlere başlıyorum.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_1
                ),
            TutorialStep(
                "Onlar basamağından 8’i boncuk kuralı ile çıkaracağım.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_2
                ),
            TutorialStep(
                "10 gider.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
                ),
            TutorialStep(
                "10 gider.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom4", 2))
                ),
            TutorialStep(
                "Kardeşi 2 gelir.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_18
                ),
            TutorialStep(
                "Kardeşi 2 gelir.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_top", 3))
                ),
            TutorialStep(
                "Şimdi birler basamağını aynı şekil boncuk kuralı ile çıkaracağım.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_3
                ),
            TutorialStep(
                "10 gider.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
                ),
            TutorialStep(
                "10 gider.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 4))
                ),
            TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_11
                ),
            TutorialStep(
                "Kardeşi 4 gelir.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 3))
                ),
            TutorialStep(
                "Cevap 348.",
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_4
                ),
            TutorialStep(
                "Bu işlemi adım adım beraber yapalım.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_5
                ),
            TutorialStep(
                "Önce ilk sayıyı abaküse yaz.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 544,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_22
                ),
            TutorialStep(
                "Şimdi, adım adım onlar basamağından 8’i çıkaralım.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_6
                ),
            TutorialStep(
                "Önce 10 gider adımını uygulayalım.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 444,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_7
                ),
            TutorialStep(
                "Güzel! Şimdi 8’in kardeşi 2 gelir adımını uygulayalım.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 464,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_8
                ),
            TutorialStep(
                "Harika !",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_9
                ),
            TutorialStep(
                "Şimdi de adım adım birler basamağından 7’yi çıkaralım.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_10
                ),
            TutorialStep(
                "Önce 10 gider adımını uygulayalım.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 454,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_11
                ),
            TutorialStep(
                "Güzel, şimdi de 3 gelir adımını uygulayalım.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 457,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_12
                ),
            TutorialStep(
                "Süper hemen kaptın.",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_13
                ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
                )
        )
    }
    
    private fun createTutorialSteps18(){
        tutorialSteps18 = listOf(
            TutorialStep(
                "Çarpmayı abaküste yapabilmen için çarpım tablosunu ezbere bilmelisin.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_1
            ),
            TutorialStep(
                "Eğer bilmiyorsan ezberlemelisin.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_2
            ),
            TutorialStep(
                "Test çözerken, sağ üstteki “Kurallar” kısmına tıklayarak çarpım tablosunu görüntüleyebilirsin.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_3
            ),
            TutorialStep(
                "Şimdi derse başlayalım.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_4
            ),
            TutorialStep(
                "Abaküste çarpma işlemi, normal çarpma gibi basamak basamak yapılıp toplanır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_5
            ),
            TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Yapacağımız şey, 6’yı en küçük basamaktan başlayarak sıra sıra çarpıp toplamak.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_6
            ),
            TutorialStep(
                "6 ile 4’ü çarpıp sonucu birler basamağına yazıyoruz.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_7
            ),
            TutorialStep(
                "6 ile 4’ü çarpıp sonucu birler basamağına yazıyoruz.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                "Sonrasında 6 ile 2’yi çarpacağız.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_8
            ),
            TutorialStep(
                "6 birler basamağında bulunuyor. 2 ise, onlar basamağında bulunuyor.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_9
            ),
            TutorialStep(
                "Birler basamağı ile onlar basamağı çarpılırsa, sonucu abaküsün onlar basamağına ekleriz.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(45),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(110),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(280),
                            duration = 0
                        )
                    }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_10
            ),
            TutorialStep(
                "Birler basamağı ile onlar basamağı çarpılırsa, sonucu abaküsün onlar basamağına ekleriz.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) })

            ),
            TutorialStep(
                "Cevap 144.",
                questionText = " 24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_20
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6
            ),
            TutorialStep(
                "3 ile 2’yi çarpıp birler basamağına yazıyoruz.",
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_11
            ),
            TutorialStep(
                "3 ile 2’yi çarpıp birler basamağına yazıyoruz.",
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom1", 1))
            ),
            TutorialStep(
                "Sonrasında 1 ile 3’ü çarpacağız.",
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_12
            ),
            TutorialStep(
                "1 onlar basamağında 3 ise birler basamağında bulunuyor.",
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_13
            ),
            TutorialStep(
                "Onlar basamağı ile birler basamağı çarpılırsa, sonuç onlar basamağına eklenir.",
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_14
            ),
            TutorialStep(
                "Onlar basamağı ile birler basamağı çarpılırsa, sonuç onlar basamağına eklenir.",
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) })
            ),
            TutorialStep(
                "Cevap 36.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_15
            ),
            TutorialStep(
                "Son olarak bu örneğe bakalım.",
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_16
            ),
            TutorialStep(
                "8 ile 3’ü çarpıp sonucu birler basamağına yazıyoruz.",
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_17
            ),
            TutorialStep(
                "8 ile 3’ü çarpıp sonucu birler basamağına yazıyoruz.",
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                "Şimdi de 5 ile 3’ü çarpıp onlar basamağına yazıyoruz.",
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_18
            ),
            TutorialStep(
                "Şimdi de 5 ile 3’ü çarpıp onlar basamağına yazıyoruz.",
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom1", 1))
            ),
            TutorialStep(
                "Cevap 174.",
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_19
            ),
            TutorialStep(
                "Teste geç.",
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps19(){
        tutorialSteps19 = listOf(
            TutorialStep(
                "Çarpma işlemini yaparken, toplama kurallarından yararlanmamız gerekebilir.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_1
            ),
            TutorialStep(
                "Örneğin bu işlemde...",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                "İlk olarak 6’yı 7 ile çarpıyorum ve sonucu birler basamağına yazıyorum.",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_2
            ),
            TutorialStep(
                "İlk olarak 6’yı 7 ile çarpıyorum ve sonucu birler basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE
            ),
            TutorialStep(
                "Sonrasında 6 ile 9’u çarpıp sonucu onlar basamağına yazacağım.",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_3
            ),
            TutorialStep(
                "54’ü onlar basamağına yazarken, 50’yi doğrudan ekleyebiliyorum.",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_4
            ),
            TutorialStep(
                "54’ü onlar basamağına yazarken 50’yi doğrudan ekleyebiliyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3)),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE
            ),
            TutorialStep(
                "Ama 4’ü doğrudan ekleyemiyorum.",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(45),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(60),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(280),
                            duration = 0
                        )
                    }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_5
            ),
            TutorialStep(
                "Bu yüzden kurallara başvuracağım.",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_6
            ),
            TutorialStep(
                "5’lik kuralla ekliyorum.",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_7
            ),
            TutorialStep(
                "5’lik kuralla ekliyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE
            ),
            TutorialStep(
                "Cevap 582.",
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_8
            ),
            TutorialStep(
                "Bu örneğe bakalım.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_top", 4)),
                soundResource = R.raw.tutorial4_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "6 ile 4’ü çarpıp sonucu birler basamağına yazıyorum.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_9

            ),
            TutorialStep(
                "6 ile 4’ü çarpıp sonucu birler basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                "Sonrasında 6 ile 8’i çarpıp sonucu onlar basamağına yazacağım.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_10
            ),
            TutorialStep(
                "48’i onlar basamağına yazarken 40’ı doğrudan ekleyebiliyorum.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_11
            ),
            TutorialStep(
                "48’i onlar basamağına yazarken 40’ı doğrudan ekleyebiliyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom4", 1)),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                "Ama 8’i doğrudan ekleyemiyorum.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_12
            ),
            TutorialStep(
                "10’luk kural ile ekleyeceğim.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_13
            ),
            TutorialStep(
                "10 gelir. Kardeşi 2 gider.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_14
            ),
            TutorialStep(
                "10 gelir.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_bottom4", 2)),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Kardeşi 2 gider.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "Kardeşi 2 gider.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                "Cevap 504.",
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_15
            ),
            TutorialStep(
                "Bu işlemi beraber yapalım.",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                soundResource = R.raw.tutorial7_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "9 ile 8’i çarpıp birler basamağına yazalım.",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 72,
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_16
            ),
            TutorialStep(
                "Şimdi 9 ile 4’ü çarpıp onlar basamağına ekleyeceğiz.",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_17
            ),
            TutorialStep(
                "9 x 4 = 36. Önce 30’u ekleyelim.",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 372,
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_18
            ),
            TutorialStep(
                "Güzel ! Şimdi 6’yı hangi kuralla ekleyeceğimize karar verelim.",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_19
            ),
            TutorialStep(
                "Boncuk kuralı ile ekleyeceksin.",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1002
            ),
            TutorialStep(
                "1 gelir. 5 gider. 10 gelir.",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 432,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_20
            ),
            TutorialStep(
                "Ve cevap 432. Süper !",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_21
            ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps20(){
        tutorialSteps20 = listOf(
            TutorialStep(
                "Bu derste, 2 basamaklı sayılarla 2 basamaklı sayıların çarpımını göreceğiz.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_1
            ),
            TutorialStep(
                "Dikkat edilmesi gereken kısım:",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_2
            ),
            TutorialStep(
                "Birler basamağı ile birler basamağı çarpılıyorsa, sonuç birler basamağına yazılır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_3
            ),
            TutorialStep(
                "Örneğin bu işlemde.",
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "5 ile 3’ün çarpımı 15'e eşittir.",
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_4
            ),
            TutorialStep(
                "Sonucun birler basamağı, birler basamağına gelecek şekilde yazıyoruz.",
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_5
            ),
            TutorialStep(
                "Sonucun birler basamağı, birler basamağına gelecek şekilde yazıyoruz.",
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
            ),
            TutorialStep(
                "Birler basamağı ile onlar basamağı çarpılıyorsa sonuç onlar, basamağına yazılır.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_6
            ),
            TutorialStep(
                "Örneğin bu işlemde.",
                questionText = "40 x 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L            ),
            TutorialStep(
                "4 ile 3’ün çarpımı 12'ye eşittir.",
                questionText = "40 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_7
            ),
            TutorialStep(
                "Sonucun birler basamağı, onlar basamağına gelecek şekilde yazıyoruz.",
                questionText = "40 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_8
            ),
            TutorialStep(
                "Sonucun birler basamağı, onlar basamağına gelecek şekilde yazıyoruz.",
                questionText = "40 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)),
            ),
            TutorialStep(
                "Onlar basamağı ile onlar basamağı çarpılıyorsa, sonuç yüzler basamağına yazılır.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_9
            ),
            TutorialStep(
                "Örneğin bu işlemde.",
                questionText = "30 x 60",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "3 ile 6’nın çarpımı 18'e eşittir.",
                questionText = "30 x 60",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    5 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_10
            ),
            TutorialStep(
                "Sonucun birler basamağı, yüzler basamağına gelecek şekilde yazıyoruz.",
                questionText = "30 x 60",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    5 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_11
            ),
            TutorialStep(
                "Sonucun birler basamağı, yüzler basamağına gelecek şekilde yazıyoruz.",
                questionText = "30 x 60",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    5 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6
            ),
            TutorialStep(
                "2. sayının birler basamağından çarpma işlemine başlıyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_12
            ),
            TutorialStep(
                "Önce 1 ile 3’ü çarpıp birler basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_13
            ),
            TutorialStep(
                "Önce 1 ile 3’ü çarpıp birler basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "Şimdi 1 ile 4’ü çarpıp onlar basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_14
            ),
            TutorialStep(
                "Şimdi 1 ile 4’ü çarpıp onlar basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "2. sayının onlar basamağınıda aynı şekilde ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_15
            ),
            TutorialStep(
                "2 ile 3’ü çarpıp onlar basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_16
            ),
            TutorialStep(
                "2 ile 3’ü çarpıp onlar basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2)),
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "2 ile 4’ü çarpıp yüzler basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_17
            ),
            TutorialStep(
                "2 ile 4’ü çarpıp yüzler basamağına ekliyoruz.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom4", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "Cevap 903.",
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_18
            ),
            TutorialStep(
                "Bu işleme bakalım.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom4", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6
            ),
            TutorialStep(
                "2. sayının birler basamağından çarpma işlemine başlıyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_19
            ),
            TutorialStep(
                "Önce 3 ile 8’i çarpıp birler basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_20
            ),
            TutorialStep(
                "Önce 3 ile 8’i çarpıp birler basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "Şimdi 3 ile 7’yi çarpıp onlar basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_21
            ),
            TutorialStep(
                "Şimdi 3 ile 7’yi çarpıp onlar basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "4 ile 8’i çarpıp onlar basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_22
            ),
            TutorialStep(
                "4 ile 8’i çarpıp onlar basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2)),
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "4 ile 7’yi çarpıp yüzler basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_23
            ),
            TutorialStep(
                "4 ile 7’yi çarpıp yüzler basamağına ekliyoruz.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom2", 1),
                    BeadAnimation(this, "rod1_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_top", 4)),
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                )
            ),
            TutorialStep(
                "Cevap 3354.",
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_100
            ),
            TutorialStep(
                "Bu işlemi beraber yapalım.",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_bottom2", 2),
                    BeadAnimation(this, "rod1_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2)),
                soundResource = R.raw.tutorial7_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "2. sayının birler basamağından işlemlere başlayacağız.",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_24
            ),
            TutorialStep(
                "4 ile 9'u çarpıp birler basamağına ekleyelim.",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 36,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_25
            ),
            TutorialStep(
                "4 ile 6'yı çarpıp onlar basamağına ekleyelim.",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 276,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_26
            ),
            TutorialStep(
                "5 ile 9'u çarpıp onlar basamağına ekleyelim.",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 726,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_27
            ),
            TutorialStep(
                "5 ile 6'yı çarpıp yüzler basamağına ekleyelim.",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 3726,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_28

            ),
            TutorialStep(
                "Cevap 3726.",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_29
            ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps21(){
        tutorialSteps21 = listOf(
            TutorialStep(
                "Bu derste, 3 basamaklı sayılar ile 1 basamaklı sayıların çarpımını göreceğiz.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_1
            ),
            TutorialStep(
                "Önceki öğrendiklerimize ek olarak,",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_2
            ),
            TutorialStep(
                "Yüzler basamağı ile birler basamağı çarpılıyorsa sonuç yüzler basamağına yazılır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_3
            ),
            TutorialStep(
                "Örneğin bu işlemde...",
                questionText = "300 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                "3 ile 6’nın çarpımı 18'e eşittir.",
                questionText = "300 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_4
            ),
            TutorialStep(
                "Sonucun birler basamağını, abaküsün yüzler basamağına gelecek şekilde yazıyoruz.",
                questionText = "300 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_5
            ),
            TutorialStep(
                "Sonucun birler basamağını, abaküsün yüzler basamağına gelecek şekilde yazıyoruz.",
                questionText = "300 x 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1))
            ),
            TutorialStep(
                "Eğer hangi basamağa yazacağını bilmiyorsan basamaklar yerine 1 ve 0 koyarak bulabilirsin.",
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_6
            ),
            TutorialStep(
                "Örneğin 100 x 1 = 100. Yani yüzler ile birler çarpılırsa sonuç yüzler basamağına yazılır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_7
            ),
            TutorialStep(
                "10 x 10 = 100. Onlar ile onlar çarpılırsa sonuç yüzlere yazılır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_8
            ),
            TutorialStep(
                "Bir örnek çözelim.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_9
            ),
            TutorialStep(
                "En küçük basamaktan başlayarak sıra sıra çarpıyorum.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_10
            ),
            TutorialStep(
                "4x4 = 16. Birler basamağına yazıyorum.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_11
            ),
            TutorialStep(
                "4x4 = 16. Birler basamağına yazıyorum.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),
            TutorialStep(
                "4x8 = 32. Onlar basamağına yazıyorum.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_12
            ),
            TutorialStep(
                "4x8 = 32. Onlar basamağına yazıyorum.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1))
            ),
            TutorialStep(
                "2x4 = 8. Yüzler basamağına ekliyorum.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_13
            ),
            TutorialStep(
                "2x4 = 8. Yüzler basamağına ekliyorum.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2))
            ),
            TutorialStep(
                "Cevap 1136.",
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_14
            ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps22(){
        tutorialSteps22 = listOf(
            TutorialStep(
                "Bu derste, 3 basamaklı sayılarla 2 basamaklı sayıların çarpımını göreceğiz.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_1
            ),
            TutorialStep(
                "Önceki öğrendiklerimize ek olarak:",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_2
            ),
            TutorialStep(
                "Yüzler basamağı ile onlar basamağı çarpılırsa, sonuç binler basamağına yazılır.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_3
            ),
            TutorialStep(
                "Örneğin bu işlemde...",
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                "6x2 işleminin sonucu 12'dir.",
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_4
            ),
            TutorialStep(
                "Sonucun birler basamağı abaküste binler basamağına gelecek şekilde yazılır.",
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_5
            ),
            TutorialStep(
                "Sonucun birler basamağı abaküste binler basamağına gelecek şekilde yazılır.",
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod0_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom2", 1))
            ),
            TutorialStep(
                "Bu örneği çözelim.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod0_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_bottom2", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_6

            ),
            TutorialStep(
                "2. sayının en küçük basamağından başlayarak sıra sıra çarpacağım.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_7,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "3x1 = 3. Birler basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_8,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "3x1 = 3. Birler basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)),
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "3x2 = 6. Onlar basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_100,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "3x2 = 6. Onlar basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "3x3 = 9. Yüzler basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_10,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "3x3 = 9. Yüzler basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom4", 1)),
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "Diğer basamağa geçiyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_11
            ),
            TutorialStep(
                "2x1 = 2. Onlar basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_12,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "2x1 = 2. Onlar basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)),
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "2x2 = 4. Yüzler basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_13,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "2x2 = 4. Yüzler basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom4", 2)),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "2x3 = 6. Binler basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_14,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "2x3 = 6. Binler basamağına yazıyorum.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_top", 3),
                    BeadAnimation(this, "rod1_bead_bottom2", 1)
                ),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "Ve cevap 7383.",
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_15
            ),
            TutorialStep(
                "Bu örneği beraber yapalım.",
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_top", 4),
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)
                ),
                soundResource = R.raw.tutorial5_39,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                "2. sayının en küçük basamağından başlayarak çarpıp, sıra sıra abaküse ekleyelim.",
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_16,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "2x2 = 4. Birler basamağına ekleyelim.",
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 4,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_17,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )

            ),
            TutorialStep(
                "2x1 = 2. Onlar basamağına ekleyelim.",
                answerNumber = 24,
                nextStepAvailable = false,
                abacusClickable = true,
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_18,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "2x3 = 6. Yüzler basamağına ekleyelim.",
                answerNumber = 624,
                nextStepAvailable = false,
                abacusClickable = true,
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_19,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "Diğer basamağa geçiyoruz.",
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_20
            ),
            TutorialStep(
                "4x2 = 8. Onlar basamağına ekleyelim.",
                answerNumber = 704,
                nextStepAvailable = false,
                abacusClickable = true,
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_21,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "4x1 = 4. Yüzler basamağına ekleyelim.",
                answerNumber = 1104,
                nextStepAvailable = false,
                abacusClickable = true,
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_22,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "4x3 = 12. Binler basamağına ekleyelim.",
                answerNumber = 13104,
                nextStepAvailable = false,
                abacusClickable = true,
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_23,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "Cevap 13104.",
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_24
            ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            ),
        )
    }
    
    private fun createTutorialSteps23(){
        tutorialSteps23 = listOf(
            TutorialStep(
                "Bu örneği çözelim.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_6
            ),
            TutorialStep(
                "2. sayının en küçük basamağından başlayarak sıra sıra çarpacağım.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_7,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "7x6 = 42. Birler basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_1,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "7x6 = 42. Birler basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "7x8 = 56. Onlar basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_2,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )

            ),
            TutorialStep(
                "7x8 = 56. Onlar basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )

            ),
            TutorialStep(
                "7x5 = 35. Yüzler basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_3,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "7x5 = 35. Yüzler basamağına yazıyorum.",
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom2", 1),
                    BeadAnimation(this, "rod1_bead_bottom3", 1),
                    BeadAnimation(this, "rod1_bead_bottom4", 1)),
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "Diğer basamağa geçiyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_11
            ),
            TutorialStep(
                "9x6 = 54. Onlar basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_4,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "9x6 = 54. Onlar basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "9x8 = 72. Yüzler basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_5,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "9x8 = 72. Yüzler basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod0_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom2", 2),
                    BeadAnimation(this, "rod1_bead_bottom3", 2),
                    BeadAnimation(this, "rod1_bead_bottom4", 2)),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "9x5 = 45. Binler basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_6,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "9x5 = 45. Binler basamağına yazıyorum.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod0_bead_top", 3),
                    BeadAnimation(this, "rod0_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_top", 3)),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    6 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                )
            ),
            TutorialStep(
                "Ve cevap 56842.",
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_7
            ),
            TutorialStep(
                "Bu örneği beraber yapalım.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod0_bead_top", 4),
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)),
                soundResource = R.raw.tutorial5_39,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                "2. sayının en küçük basamağından başlayarak çarpıp, sıra sıra abaküse ekleyelim.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_8
            ),
            TutorialStep(
                "4x5 = 20. Birler basamağına ekleyelim.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 20,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_9
            ),
            TutorialStep(
                "4x6 = 24. Onlar basamağına ekleyelim.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 260,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_10
            ),
            TutorialStep(
                "4x7 = 28. Yüzler basamağına ekleyelim.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 3060,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_12
            ),
            TutorialStep(
                "Diğer basamağa geçiyoruz.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_20
            ),
            TutorialStep(
                "8x5 = 40. Onlar basamağına ekleyelim.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 3460,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_13
            ),
            TutorialStep(
                "8x6 = 48. Yüzler basamağına ekleyelim.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 8260,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_14
            ),
            TutorialStep(
                "8x7= 56. Binler basamağına ekleyelim.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 64260,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_15
            ),
            TutorialStep(
                "Cevap 64260.",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_16
            ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )

        )
    }
    
    private fun createTutorialSteps24(){
        tutorialSteps24 = listOf(
            TutorialStep(
                "Bu derste abaküs kullanmadan işlemleri yapacağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_1

            ),
            TutorialStep(
                "Aklından abaküsteki boncukları hayal edip işlemleri öyle yapmalısın.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_2
            ),
            TutorialStep(
                "Abaküsü karşında hayal edip boncukları parmaklarınla hareket ettirmeyi deneyebilirsin.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_3

            ),
            TutorialStep(
                "Ama sadece abaküs üzerinden işlemleri yapmalısın.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_4
            ),
            TutorialStep(
                "Yani 3+1 işlemini yaparken 3’ü abaküse yazıp onun üzerine 1 boncuk eklemelisin. Ezbere 4 dememelisin.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_5
            ),
            TutorialStep(
                "Şimdi teste geçeceğiz.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_6
            ),
            TutorialStep(
                "Testte tahtaya sıra sıra sayılar gelecek.",
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial1, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = binding.tutorialText,
                            bottomToTop = R.id.blinding_tutorial1,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    },

                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_7
            ),
            TutorialStep(
                "Bu sayıları aklından toplayıp sonucu aşağıdaki kutucuğa yazacaksın.",
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial2, View.VISIBLE) },
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial1, View.INVISIBLE) },
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_8
            ),
            TutorialStep(
                "Örneğin sıra sıra şu sayılar gelebilir.",
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial2, View.INVISIBLE) },
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_9
            ),
            TutorialStep(
                "3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_10
            ),
            TutorialStep(
                "11",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_99
            ),
            TutorialStep(
                "1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_12
            ),
            TutorialStep(
                "5",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_13
            ),
            TutorialStep(
                "Diye 3 saniye arayla sayılar gelecek. Sayıların gelmesi durduğunda sonucu kutucuğa yazacaksın.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_14
            ),
            TutorialStep(
                "Eğer kaçırdığın bir sayı olursa, tekrar butonuna tıklayarak soruyu en baştan görebilirsin.",
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial3, View.VISIBLE) },
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_15
            ),
            TutorialStep(
                "Başarılar.",
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial3, View.INVISIBLE) },
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_16
            )
        )
    }
    
    private fun createTutorialSteps25(){
        tutorialSteps25 = listOf(
            TutorialStep(
                "Bu derste, abaküsü hayal ederek aklımızdan kuralsız çıkarma yapacağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_1
            ),
            TutorialStep(
                "İlk sayı hariç diğer sayıları çıkaracağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_2
            ),
            TutorialStep(
                "Örneğin sayılar şu şekilde gelebilir.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_3
            ),
            TutorialStep(
                "88",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_4
            ),
            TutorialStep(
                "-12",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_5
            ),
            TutorialStep(
                "-11",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_6
            ),
            TutorialStep(
                "-50",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_7
            ),
            TutorialStep(
                "Başarılar.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_16
            )
        )
    }
    
    private fun createTutorialSteps26(){
        tutorialSteps26 = listOf(
            TutorialStep(
                "Bu derste abaküsü hayal ederek aklımızdan çarpma yapacağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial26_1
            ),
            TutorialStep(
                "Çarpılacak sayılar ekrana gelecek.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial26_2
            ),
            TutorialStep(
                "Ve abaküsü hayal ederek çarpma işlemini yapacağız.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial26_3
            ),
            TutorialStep(
                "Teste geç.",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }

    private fun setTextWithColoredPositions(
        textView: TextView,
        text: String,
        colorPositions: List<Pair<Int, Int>>
    ) {
        val spannableString = SpannableString(text)

        colorPositions.forEach { (position, color) ->
            if (position >= 0 && position < text.length) {
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    position,
                    position + 1,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        textView.text = spannableString
    }

    private fun applyWidgetOperations(operations: List<WidgetOperation>) {
        operations.forEach { operation ->
            when (operation) {
                is WidgetOperation.ChangeVisibility -> {
                    operation.view.visibility = operation.visibility
                }
                is WidgetOperation.ChangeMargin -> {
                    val params = operation.view.layoutParams as ViewGroup.MarginLayoutParams
                    params.rightMargin = operation.marginRight
                    params.leftMargin = operation.marginLeft
                    operation.marginTop?.let { params.topMargin = it }
                    operation.marginBottom?.let { params.bottomMargin = it }
                    operation.view.layoutParams = params
                }
                is WidgetOperation.ChangeConstraints -> {
                    val params = operation.view.layoutParams as ConstraintLayout.LayoutParams

                    // Belirtilen constraint'leri ayarla
                    operation.startToStart?.let { params.startToStart = it }
                    operation.startToEnd?.let { params.startToEnd = it }
                    operation.endToStart?.let { params.endToStart = it }
                    operation.endToEnd?.let { params.endToEnd = it }
                    operation.topToTop?.let { params.topToTop = it }
                    operation.topToBottom?.let { params.topToBottom = it }
                    operation.bottomToTop?.let { params.bottomToTop = it }
                    operation.bottomToBottom?.let { params.bottomToBottom = it }
                    
                    // Belirtilmeyen constraint'leri UNSET yap (sadece topToTop veya bottomToBottom ayarlandıysa)
                    // Bu, önceki adımlardan kalan constraint'lerin view pozisyonunu etkilemesini önler
                    if (operation.topToTop != null && operation.bottomToBottom == null) {
                        params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                    }
                    if (operation.bottomToBottom != null && operation.topToTop == null) {
                        params.topToTop = ConstraintLayout.LayoutParams.UNSET
                        params.topToBottom = ConstraintLayout.LayoutParams.UNSET
                    }

                    operation.view.layoutParams = params
                }
                is WidgetOperation.AnimateMargin -> {
                    val params = operation.view.layoutParams as ViewGroup.MarginLayoutParams

                    // Sağ margin animasyonu
                    ValueAnimator.ofInt(operation.fromMarginRight, operation.toMarginRight).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.rightMargin = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()
                    }

                    // Sol margin animasyonu
                    ValueAnimator.ofInt(operation.fromMarginLeft, operation.toMarginLeft).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.leftMargin = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()                    }
                }
                is WidgetOperation.AnimateSize -> {
                    val params = operation.view.layoutParams

                    // Width animasyonu
                    ValueAnimator.ofInt(operation.fromWidth, operation.toWidth).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.width = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()                    }

                    // Height animasyonu
                    ValueAnimator.ofInt(operation.fromHeight, operation.toHeight).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.height = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()                    }
                }

                else -> {}
            }
        }
    }
    sealed class WidgetOperation {
        data class ChangeVisibility(val view: View, val visibility: Int) : WidgetOperation()
        data class ChangeMargin(
            val view: View,
            val marginRight: Int,
            val marginLeft: Int,
            val marginTop: Int? = null,
            val marginBottom: Int? = null
        ) : WidgetOperation()
        data class ChangeConstraints(
            val view: View,
            val startToStart: Int? = null,
            val startToEnd: Int? = null,
            val endToStart: Int? = null,
            val endToEnd: Int? = null,
            val topToTop: Int? = null,
            val topToBottom: Int? = null,
            val bottomToTop: Int? = null,
            val bottomToBottom: Int? = null
        ) : WidgetOperation()
        data class AnimateMargin(
            val view: View,
            val fromMarginRight: Int,
            val toMarginRight: Int,
            val fromMarginLeft: Int,
            val toMarginLeft: Int,
            val duration: Long = 300 // varsayılan animasyon süresi
        ) : WidgetOperation()
        data class AnimateSize(
            val view: View,
            val fromWidth: Int,
            val toWidth: Int,
            val fromHeight: Int,
            val toHeight: Int,
            val duration: Long = 300
        ) : WidgetOperation()
    }
    data class TutorialStep(
        val text: String,
        val animation: List<BeadAnimation>? = null,
        val widgetOperations: List<() -> WidgetOperation>? = null,
        val onStep: ((View) -> Unit)? = null,
        val questionText: String? = null,
        val questionTextVisibility: Int = View.INVISIBLE,
        var nextStepAvailable: Boolean = true,
        var abacusClickable:Boolean = false,
        val answerNumber: Int? = null,
        val onStepComplete: (() -> Unit)? = null,  // Yeni eklenen fonksiyon parametresi
        val rulesPanelVisibility: Int = View.VISIBLE,
        val questionTextColorPositions: List<Pair<Int, Int>>? = null,
        val soundResource: Int? = null,  // Ses dosyası resource ID'si
        val useTypewriterEffect: Boolean = false,  // Typewriter effect kullanılsın mı?
        val typewriterSpeed: Long = 50L,  // Harf başına milisaniye (varsayılan 50ms)
        val abacusReset: Boolean? = null, //Aktif olan adımda yanlış yapılıp tekrar'a basılırsa abaküsü sıfırlar, Aktif olan adıma geri dönülürse sıfırlar. Yani bu adımda abaküsü sıfırla
        val nextStepAbacusReset: Boolean? = null, //Sıradaki adıma geçildiğinde sıfırlar
        val requestText: String? = null,
        val backAnimationOff: Boolean? = null,
        val resetAndWaith: Boolean? = null, //Eğer mevcut adımda abaküse tıklanılabiliyor ve bir önceki adımda beadAnimation varsa önce abaküsü sıfırla ve 0.8 saniye bekle sonra beadAnimation'ları yükle
        val backAnswerNumber: Int? = null, //Mevcut adımda geriye tıklandığında yazdırılacak sayı.
        val widgetVisibilityMap: Map<Int, Int>? = null, //Widget ID'lerine göre görünürlük değişikliği yapar. Key: R.id.widgetId, Value: View.VISIBLE/INVISIBLE/GONE
        val options: List<String>? = null,
        val correctOptionIndex: List<Int>? = null, // Bir veya birden fazla doğru index
        val multipleChoice: Boolean? = null, // true ise birden fazla seçenek seçilebilir
        val optionText: String? = null // Seçenek panelinin üstünde gösterilecek açıklama metni

    )
    private fun stepAnswerAlgorithm(): Boolean {

        abacusNumberReturn()
        return if (controlNumber == answerNumber) {
            controlNumber = 0
            true
        } else {
            controlNumber = 0
            false
        }
    }

    // Options panelindeki seçimleri değerlendir
    private fun checkOptionsAnswer() {
        val step = getCurrentStep()
        val opts = step.options
        val correctIndices = step.correctOptionIndex
        if (opts.isNullOrEmpty() || correctIndices.isNullOrEmpty()) return
        if (isAnyAnimationRunning()) return

        val selected = optionsAdapter?.getSelectedPositions() ?: emptySet()
        if (selected.isEmpty()) return

        val isCorrect =
            selected.size == correctIndices.size &&
                    selected.containsAll(correctIndices)

        // Kontrol yapıldıktan sonra geri ve "eğitimi atla" tekrar aktif olsun
        optionsInteractionLocked = false
        showResultPanelForOptions(isCorrect = isCorrect)
    }

    private fun updateOptionsPanelForStep(step: TutorialStep) {
        val stepOptions = step.options
        val correctIndices = step.correctOptionIndex

        val hasValidOptions = stepOptions != null &&
                stepOptions.isNotEmpty() &&
                correctIndices != null &&
                correctIndices.isNotEmpty() &&
                correctIndices.all { it in stepOptions.indices }

        // Her yeni adımda daha önce planlanmış panel gösterimlerini iptal et
        optionsPanelShowRunnable?.let { runnable ->
            optionsPanel.removeCallbacks(runnable)
        }
        optionsPanelShowRunnable = null

        if (!hasValidOptions) {
            // Her ihtimale karşı kilidi kaldır
            optionsInteractionLocked = false
            optionsTitleText.visibility = View.GONE
            optionsTitleText.text = ""
            optionsCheckButton.visibility = View.GONE
            // Seçenek yoksa paneli aşağıya kaydırarak gizle
            if (optionsPanel.visibility == View.VISIBLE) {
                optionsPanel.animate()
                    .translationY(optionsPanel.height.toFloat())
                    .alpha(0f)
                    .setDuration(200)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        optionsPanel.visibility = View.GONE
                        optionsPanel.translationY = 0f
                        optionsPanel.alpha = 1f
                        optionsAdapter?.submitOptions(emptyList(), false)
                    }
                    .start()
            } else {
                optionsPanel.visibility = View.GONE
                optionsAdapter?.submitOptions(emptyList(), false)
            }
            return
        }

        // Geçerli seçenekler varsa paneli 1 sn sonra aşağıdan kayarak göster.
        // Bu 1 saniyelik beklemede panel tamamen gizli ve dokunulamaz olacak.
        optionsPanel.visibility = View.GONE
        optionsPanel.alpha = 0f
        optionsPanel.translationY = 0f
        // Eski seçenekleri hemen temizle ki panel görünmeden önce tıklanamasın
        optionsAdapter?.submitOptions(emptyList(), step.multipleChoice!!)
        optionsPanel.clearAnimation()

        val runnable = Runnable {
            // Başlık metnini güncelle
            val optionText = step.optionText
            if (!optionText.isNullOrBlank()) {
                optionsTitleText.visibility = View.VISIBLE
                optionsTitleText.text = optionText
            } else {
                optionsTitleText.visibility = View.GONE
                optionsTitleText.text = ""
            }

            optionsCheckButton.visibility = View.VISIBLE
            // Panel göründüğü andan itibaren geri ve "eğitimi atla" kilitlensin
            optionsInteractionLocked = true

            val parentView = optionsPanel.parent as? View
            val startY = parentView?.height?.toFloat()
                ?: optionsPanel.height.toFloat().takeIf { it > 0 } ?: 300f

            // Artık yeni seçenekleri yükle ve paneli göster
            optionsAdapter?.submitOptions(stepOptions!!, step.multipleChoice!!)
            optionsPanel.visibility = View.VISIBLE
            optionsPanel.translationY = startY
            optionsPanel.alpha = 0f

            optionsPanel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
        optionsPanelShowRunnable = runnable
        optionsPanel.postDelayed(runnable, 1000L)
    }

    private fun showResultPanel() {
        if (stepAnswerAlgorithm()) {
            showResultPanelForOptions(isCorrect = true)
        } else {
            showResultPanelForOptions(isCorrect = false)
        }
    }

    private fun showResultPanelForOptions(isCorrect: Boolean) {
        if (isCorrect) {
            // Doğru cevap durumu

            playCorretSound(R.raw.correct_answer_sound)

            correctPanel.translationY = correctPanel.height.toFloat()
            correctPanel.visibility = View.VISIBLE
            correctPanel.alpha = 0f

            // Overlay'i görünür yap
            binding.root.findViewById<View>(R.id.overlay).visibility = View.VISIBLE

            // Control button'u devre dışı bırak
            controlButton.isClickable = false
            controlButton.isFocusable = false
            controlButton.setOnTouchListener(null)

            correctPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

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
                            currentStep++

                            // Index kontrolü yap
                            if (currentStep >= currentTutorialSteps.size) {
                                val operations = MapFragment.getLessonOperations(lessonStep)
                                val abacusFragment = AbacusFragment()
                                val blindingLessonFragment = BlindingLessonFragment()
                                val bundle = Bundle()
                                bundle.putSerializable("lessonItem", lessonItem)
                                abacusFragment.arguments = bundle
                                if(lessonItem!!.isBlinding == true){
                                    devametFragment(blindingLessonFragment)
                                }else{
                                    devametFragment(abacusFragment)
                                }
                                return@setOnTouchListener true
                            }

                            if(getPlusIndexCurrentStep(-1).nextStepAbacusReset == true){
                                Log.d("melih","work123")
                                resetAbacus()
                            }
                            if(!getCurrentStep().abacusClickable){
                                getCurrentStep().onStepComplete?.invoke()
                            }
                            controlButton.visibility= View.INVISIBLE
                            backOrFront = true // İleri gidiyoruz
                            showStep(currentStep)
                            true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(BounceInterpolator())
                                .start()
                            binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                            
                            // Control button'u tekrar aktif hale getir
                            controlButton.isClickable = true
                            controlButton.isFocusable = true
                            controlButtonListener?.let { listener ->
                                controlButton.setOnTouchListener(listener)
                            }
                            
                            correctPanel.animate()
                                .translationY(correctPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    correctPanel.visibility = View.GONE
                                }
                                .start()
                            true
                        }

                        else -> false
                    }
                }
        } else {
            // Yanlış cevap durumu
            playCorretSound(R.raw.incorrect_answer_sound)

            incorrectPanel.translationY = incorrectPanel.height.toFloat()
            incorrectPanel.visibility = View.VISIBLE
            incorrectPanel.alpha = 0f
            binding.root.findViewById<View>(R.id.overlay).visibility = View.VISIBLE
            
            // Control button'u devre dışı bırak
            controlButton.isClickable = false
            controlButton.isFocusable = false
            controlButton.setOnTouchListener(null)
            
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

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(BounceInterpolator())
                                .start()
                            
                            // Panel açıksa kapat
                            if (requestPanelView != null && requestPanelView?.visibility == View.VISIBLE) {
                                hideRequestPanel(requestPanelView!!)
                            }
                            
                            binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                            
                            // Control button'u tekrar aktif hale getir
                            controlButton.isClickable = true
                            controlButton.isFocusable = true
                            controlButtonListener?.let { listener ->
                                controlButton.setOnTouchListener(listener)
                            }
                            
                            incorrectButtonClick()
                            incorrectPanel.animate()
                                .translationY(incorrectPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    incorrectPanel.visibility = View.GONE
                                }
                                .start()

                            true
                        }

                        else -> false
                    }
                }
        }
    }
    private fun tutorialSkipSetup(){
        binding.skipTutorialButton.setOnClickListener {
            // Seçenek paneli açık ve kilitliyken "Eğitimi atla" çalışmasın
            if (optionsInteractionLocked && optionsPanel.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            val abacusFragment = AbacusFragment()
            val blindingLessonFragment = BlindingLessonFragment()
            val bundle = Bundle()
            bundle.putSerializable("lessonItem", lessonItem) // item Serializable olmalı!
            abacusFragment.arguments = bundle
            if(lessonItem!!.isBlinding == true){
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, blindingLessonFragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, blindingLessonFragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()



                // TutorialFragment'i kapat
                currentStep=0
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )
                    .remove(this)
                    .commit()
            }else{
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, abacusFragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, abacusFragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()



                // TutorialFragment'i kapat
                currentStep=0
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_right  // Çıkış animasyonu
                    )
                    .remove(this)
                    .commit()            }
        }
    }
    private fun incorrectButtonClick(){
        if (currentStep > 0) {
            var notBackNumber = false

            if(getCurrentStep().abacusReset == true){
                resetAbacus()
            }
            if(getCurrentStep().backAnswerNumber != null){
                writeAnswerNumber(getCurrentStep().backAnswerNumber!!)
                Log.d("libya2",getCurrentStep().backAnswerNumber!!.toString())
            }
            showStep(currentStep, skipAnimations = true)
            binding.devamButton.visibility = View.GONE

        } else {
            closeFragment()
        }
    }

    private fun findIDs() {
        // 1. sütun için boncukları bul
        rod0BottomBead4 = binding.rod0BeadBottom4
        rod0BottomBead3 = binding.rod0BeadBottom3
        rod0BottomBead2 = binding.rod0BeadBottom2
        rod0BottomBead1 = binding.rod0BeadBottom1
        rod0TopBead = binding.rod0BeadTop

        // 2. sütun için boncukları bul
        rod1BottomBead4 = binding.rod1BeadBottom4
        rod1BottomBead3 = binding.rod1BeadBottom3
        rod1BottomBead2 = binding.rod1BeadBottom2
        rod1BottomBead1 = binding.rod1BeadBottom1
        rod1TopBead = binding.rod1BeadTop

        // 3. sütun için boncukları bul
        rod2BottomBead4 = binding.rod2BeadBottom4
        rod2BottomBead3 = binding.rod2BeadBottom3
        rod2BottomBead2 = binding.rod2BeadBottom2
        rod2BottomBead1 = binding.rod2BeadBottom1
        rod2TopBead = binding.rod2BeadTop

        // 4. sütun için boncukları bul
        rod3BottomBead4 = binding.rod3BeadBottom4
        rod3BottomBead3 = binding.rod3BeadBottom3
        rod3BottomBead2 = binding.rod3BeadBottom2
        rod3BottomBead1 = binding.rod3BeadBottom1
        rod3TopBead = binding.rod3BeadTop

        // 5. sütun için boncukları bul
        rod4BottomBead4 = binding.rod4BeadBottom4
        rod4BottomBead3 = binding.rod4BeadBottom3
        rod4BottomBead2 = binding.rod4BeadBottom2
        rod4BottomBead1 = binding.rod4BeadBottom1
        rod4TopBead = binding.rod4BeadTop
    }

    private fun abacusNumberReturn() {
        if (rod4FourIsUp) {
            controlNumber += 4
        } else if (rod4ThreeIsUp) {
            controlNumber += 3
        } else if (rod4TwoIsUp) {
            controlNumber += 2
        } else if (rod4OneIsUp) {
            controlNumber += 1
        }
        if (rod4TopIsDown) {
            controlNumber += 5
        }
        if (rod3FourIsUp) {
            controlNumber += 40
        } else if (rod3ThreeIsUp) {
            controlNumber += 30
        } else if (rod3TwoIsUp) {
            controlNumber += 20
        } else if (rod3OneIsUp) {
            controlNumber += 10
        }
        if (rod3TopIsDown) {
            controlNumber += 50
        }
        if (rod2FourIsUp) {
            controlNumber += 400
        } else if (rod2ThreeIsUp) {
            controlNumber += 300
        } else if (rod2TwoIsUp) {
            controlNumber += 200
        } else if (rod2OneIsUp) {
            controlNumber += 100
        }
        if (rod2TopIsDown) {
            controlNumber += 500
        }
        if (rod1FourIsUp) {
            controlNumber += 4000
        } else if (rod1ThreeIsUp) {
            controlNumber += 3000
        } else if (rod1TwoIsUp) {
            controlNumber += 2000
        } else if (rod1OneIsUp) {
            controlNumber += 1000
        }
        if (rod1TopIsDown) {
            controlNumber += 5000
        }
        if (fourIsUp) {
            controlNumber += 40000
        } else if (threeIsUp) {
            controlNumber += 30000
        } else if (twoIsUp) {
            controlNumber += 20000
        } else if (oneIsUp) {
            controlNumber += 10000
        }
        if (topIsDown) {
            controlNumber += 50000
        }

    }

    private fun setupBeads() {
        if (!getCurrentStep().abacusClickable) {
            Log.d("üşüdüm","ses")
            // Tüm alt view'lerin tıklanabilirliğini engelle
            disableAllClickable(binding.abacusLinear)
            // Kontrol butonunu gizle
            controlButton.visibility = View.INVISIBLE
            return
        }

        // Tüm alt view'lerin tıklanabilirliğini etkinleştir
        enableAllClickable(binding.abacusLinear)
        controlButton.visibility = View.VISIBLE
        binding.abacusLinear.isClickable = true
        binding.abacusLinear.isFocusable = true

        // Boncuklara tıklama işlemleri
        rod0BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!fourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (!threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (!twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (!oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        fourIsUp = true
                        threeIsUp = true
                        twoIsUp = true
                        oneIsUp = true
                        updateRod0BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod0BottomBead4)
                    fourIsUp = false
                    updateRod0BeadsAppearance()
                }
            }
        }

        rod0BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!threeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (!twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (!oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        threeIsUp = true
                        twoIsUp = true
                        oneIsUp = true
                        updateRod0BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    // Yukarıda olan boncukları kontrol et ve aşağı indir
                    if (fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (threeIsUp) beadsToAnimate.add(rod0BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (fourIsUp) fourIsUp = false
                        threeIsUp = false
                        updateRod0BeadsAppearance()
                    }
                }
            }
        }

        rod0BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!twoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (!oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        twoIsUp = true
                        oneIsUp = true
                        updateRod0BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    // Yukarıda olan boncukları kontrol et ve aşağı indir
                    if (fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (twoIsUp) beadsToAnimate.add(rod0BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (fourIsUp) fourIsUp = false
                        if (threeIsUp) threeIsUp = false
                        twoIsUp = false
                        updateRod0BeadsAppearance()
                    }
                }
            }
        }

        rod0BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!oneIsUp) {
                    animateBeadsUp(rod0BottomBead1)
                    oneIsUp = true
                    updateRod0BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (fourIsUp) fourIsUp = false
                        if (threeIsUp) threeIsUp = false
                        if (twoIsUp) twoIsUp = false
                        oneIsUp = false
                        updateRod0BeadsAppearance()
                    }
                }
            }
        }

        // 1. sütun üst boncuk
        rod0TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!topIsDown) {
                    animateBeadDown(rod0TopBead)
                    topIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod0TopBead)
                    topIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 2. sütun için click listener'lar
        rod1BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (!rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (!rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (!rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod1FourIsUp = true
                        rod1ThreeIsUp = true
                        rod1TwoIsUp = true
                        rod1OneIsUp = true
                        updateRod1BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod1BottomBead4)
                    rod1FourIsUp = false
                    updateRod1BeadsAppearance()
                }
            }
        }

        rod1BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (!rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (!rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod1ThreeIsUp = true
                        rod1TwoIsUp = true
                        rod1OneIsUp = true
                        updateRod1BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod1FourIsUp) rod1FourIsUp = false
                        rod1ThreeIsUp = false
                        updateRod1BeadsAppearance()
                    }
                }
            }
        }

        rod1BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (!rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod1TwoIsUp = true
                        rod1OneIsUp = true
                        updateRod1BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod1FourIsUp) rod1FourIsUp = false
                        if (rod1ThreeIsUp) rod1ThreeIsUp = false
                        rod1TwoIsUp = false
                        updateRod1BeadsAppearance()
                    }
                }
            }
        }

        rod1BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1OneIsUp) {
                    animateBeadsUp(rod1BottomBead1)
                    rod1OneIsUp = true
                    updateRod1BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod1FourIsUp) rod1FourIsUp = false
                        if (rod1ThreeIsUp) rod1ThreeIsUp = false
                        if (rod1TwoIsUp) rod1TwoIsUp = false
                        rod1OneIsUp = false
                        updateRod1BeadsAppearance()
                    }
                }
            }
        }

        // 2. sütun üst boncuk
        rod1TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1TopIsDown) {
                    animateBeadDown(rod1TopBead)
                    rod1TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod1TopBead)
                    rod1TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 3. sütun için click listener'lar
        rod2BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (!rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (!rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (!rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod2FourIsUp = true
                        rod2ThreeIsUp = true
                        rod2TwoIsUp = true
                        rod2OneIsUp = true
                        updateRod2BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod2BottomBead4)
                    rod2FourIsUp = false
                    updateRod2BeadsAppearance()
                }
            }
        }

        rod2BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (!rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (!rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod2ThreeIsUp = true
                        rod2TwoIsUp = true
                        rod2OneIsUp = true
                        updateRod2BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod2FourIsUp) rod2FourIsUp = false
                        rod2ThreeIsUp = false
                        updateRod2BeadsAppearance()
                    }
                }
            }
        }

        rod2BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (!rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod2TwoIsUp = true
                        rod2OneIsUp = true
                        updateRod2BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod2FourIsUp) rod2FourIsUp = false
                        if (rod2ThreeIsUp) rod2ThreeIsUp = false
                        rod2TwoIsUp = false
                        updateRod2BeadsAppearance()
                    }
                }
            }
        }

        rod2BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2OneIsUp) {
                    animateBeadsUp(rod2BottomBead1)
                    rod2OneIsUp = true
                    updateRod2BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod2FourIsUp) rod2FourIsUp = false
                        if (rod2ThreeIsUp) rod2ThreeIsUp = false
                        if (rod2TwoIsUp) rod2TwoIsUp = false
                        rod2OneIsUp = false
                        updateRod2BeadsAppearance()
                    }
                }
            }
        }

        // 3. sütun üst boncuk
        rod2TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2TopIsDown) {
                    animateBeadDown(rod2TopBead)
                    rod2TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod2TopBead)
                    rod2TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 4. sütun için click listener'lar
        rod3BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (!rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (!rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (!rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod3FourIsUp = true
                        rod3ThreeIsUp = true
                        rod3TwoIsUp = true
                        rod3OneIsUp = true
                        updateRod3BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod3BottomBead4)
                    rod3FourIsUp = false
                    updateRod3BeadsAppearance()
                }
            }
        }

        rod3BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (!rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (!rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod3ThreeIsUp = true
                        rod3TwoIsUp = true
                        rod3OneIsUp = true
                        updateRod3BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod3FourIsUp) rod3FourIsUp = false
                        rod3ThreeIsUp = false
                        updateRod3BeadsAppearance()
                    }
                }
            }
        }

        rod3BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (!rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod3TwoIsUp = true
                        rod3OneIsUp = true
                        updateRod3BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod3FourIsUp) rod3FourIsUp = false
                        if (rod3ThreeIsUp) rod3ThreeIsUp = false
                        rod3TwoIsUp = false
                        updateRod3BeadsAppearance()
                    }
                }
            }
        }

        rod3BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3OneIsUp) {
                    animateBeadsUp(rod3BottomBead1)
                    rod3OneIsUp = true
                    updateRod3BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod3FourIsUp) rod3FourIsUp = false
                        if (rod3ThreeIsUp) rod3ThreeIsUp = false
                        if (rod3TwoIsUp) rod3TwoIsUp = false
                        rod3OneIsUp = false
                        updateRod3BeadsAppearance()
                    }
                }
            }
        }

        // 4. sütun üst boncuk
        rod3TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3TopIsDown) {
                    animateBeadDown(rod3TopBead)
                    rod3TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod3TopBead)
                    rod3TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 5. sütun için click listener'lar
        rod4BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (!rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (!rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (!rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod4FourIsUp = true
                        rod4ThreeIsUp = true
                        rod4TwoIsUp = true
                        rod4OneIsUp = true
                        updateRod4BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod4BottomBead4)
                    rod4FourIsUp = false
                    updateRod4BeadsAppearance()
                }
            }
        }

        rod4BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (!rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (!rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod4ThreeIsUp = true
                        rod4TwoIsUp = true
                        rod4OneIsUp = true
                        updateRod4BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod4FourIsUp) rod4FourIsUp = false
                        rod4ThreeIsUp = false
                        updateRod4BeadsAppearance()
                    }
                }
            }
        }

        rod4BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (!rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod4TwoIsUp = true
                        rod4OneIsUp = true
                        updateRod4BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod4FourIsUp) rod4FourIsUp = false
                        if (rod4ThreeIsUp) rod4ThreeIsUp = false
                        rod4TwoIsUp = false
                        updateRod4BeadsAppearance()
                    }
                }
            }
        }

        rod4BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4OneIsUp) {
                    animateBeadsUp(rod4BottomBead1)
                    rod4OneIsUp = true
                    updateRod4BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod4FourIsUp) rod4FourIsUp = false
                        if (rod4ThreeIsUp) rod4ThreeIsUp = false
                        if (rod4TwoIsUp) rod4TwoIsUp = false
                        rod4OneIsUp = false
                        updateRod4BeadsAppearance()
                    }
                }
            }
        }

        // 5. sütun üst boncuk
        rod4TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4TopIsDown) {
                    animateBeadDown(rod4TopBead)
                    rod4TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod4TopBead)
                    rod4TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }
    }

    private fun animateBeadsUp(vararg beads: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L // milisaniye cinsinden
        val moveDistance = 135 // piksel cinsinden

        beads.forEach { bead ->
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin + moveDistance

            bead.animate()
                .setDuration(animationDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withStartAction {
                    // Animasyon başlamadan önce yapılacak işlemler
                }
                .withEndAction {
                    // Animasyon bittiğinde yapılacak işlemler
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f // Translation'ı sıfırla

                    // Son boncuk animasyonu bittiğinde isAnimating'i false yap
                    if (bead == beads.last()) {
                        isAnimating2 = false
                    }
                }
                .translationY(-moveDistance.toFloat())
                .start()
        }
    }

    private fun animateBeadDown(bead: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L
        val moveDistance = 90

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withStartAction {
                // Animasyon başlamadan önce yapılacak işlemler
            }
            .withEndAction {
                // Animasyon bittiğinde isAnimating'i false yap
                isAnimating2 = false
            }
            .translationY(moveDistance.toFloat())
            .start()
    }

    private fun animateBeadUp(bead: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L
        val moveDistance = 90

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withStartAction {
                // Animasyon başlamadan önce yapılacak işlemler
            }
            .withEndAction {
                // Animasyon bittiğinde isAnimating'i false yap
                isAnimating2 = false
            }
            .translationY(0f)  // Orijinal konumuna dön
            .start()
    }

    private fun animateBeadsDown(vararg beads: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L
        val moveDistance = 135

        beads.forEach { bead ->
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin - moveDistance  // Yukarı çıktığı mesafe kadar aşağı in

            bead.animate()
                .setDuration(animationDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withStartAction {
                    // Animasyon başlamadan önce yapılacak işlemler
                }
                .withEndAction {
                    // Animasyon bittiğinde yapılacak işlemler
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f // Translation'ı sıfırla

                    // Son boncuk animasyonu bittiğinde isAnimating'i false yap
                    if (bead == beads.last()) {
                        isAnimating2 = false
                    }
                }
                .translationY(moveDistance.toFloat())  // Aşağı doğru hareket
                .start()
        }
    }

    // Boncukların görünümünü güncelleyen fonksiyon
    private fun updateBeadAppearance(bead: ImageView, isSelected: Boolean) {
        val resourceId = if (isSelected) {
            resources.getIdentifier(
                "soroban_bead_selected",
                "drawable",
                requireContext().packageName
            )
        }
         else{

            resources.getIdentifier("soroban_bead", "drawable", requireContext().packageName)

        }
        bead.setImageResource(resourceId)
    }

    // 1. sütun için görünüm güncellemeleri
    private fun updateRod0BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod0_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod0_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod0_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod0_bead_bottom4)


            updateBeadAppearance(bottomBead1, oneIsUp)
            updateBeadAppearance(bottomBead2, twoIsUp)
            updateBeadAppearance(bottomBead3, threeIsUp)
            updateBeadAppearance(bottomBead4, fourIsUp)
        }
    }

    // 2. sütun için görünüm güncellemeleri
    private fun updateRod1BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod1_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod1_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod1_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod1_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod1OneIsUp)
            updateBeadAppearance(bottomBead2, rod1TwoIsUp)
            updateBeadAppearance(bottomBead3, rod1ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod1FourIsUp)
        }
    }

    // 3. sütun için görünüm güncellemeleri
    private fun updateRod2BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod2_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod2_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod2_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod2_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod2OneIsUp)
            updateBeadAppearance(bottomBead2, rod2TwoIsUp)
            updateBeadAppearance(bottomBead3, rod2ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod2FourIsUp)
        }
    }

    // 4. sütun için görünüm güncellemeleri
    private fun updateRod3BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod3_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod3_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod3_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod3_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod3OneIsUp)
            updateBeadAppearance(bottomBead2, rod3TwoIsUp)
            updateBeadAppearance(bottomBead3, rod3ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod3FourIsUp)
        }
    }

    // 5. sütun için görünüm güncellemeleri
    private fun updateRod4BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod4_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod4_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod4_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod4_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod4OneIsUp)
            updateBeadAppearance(bottomBead2, rod4TwoIsUp)
            updateBeadAppearance(bottomBead3, rod4ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod4FourIsUp)
        }
    }

    // Üst boncukların görünümünü güncelleyen fonksiyonlar
    private fun updateTopBeadsAppearance() {
        updateBeadAppearance(rod0TopBead, topIsDown)
        updateBeadAppearance(rod1TopBead, rod1TopIsDown)
        updateBeadAppearance(rod2TopBead, rod2TopIsDown)
        updateBeadAppearance(rod3TopBead, rod3TopIsDown)
        updateBeadAppearance(rod4TopBead, rod4TopIsDown)
    }

    private fun resetAbacus() {
        // Ekrana tıklanmasını engellemek için overlay'i görünür yap
        binding.overlay.visibility = View.VISIBLE
        binding.overlay.alpha = 0.01f // Neredeyse görünmez ama tıklanabilir
        binding.overlay.isClickable = true
        binding.overlay.isFocusable = true
        
        // 0.4 saniye sonra overlay'i kapat
        Handler(Looper.getMainLooper()).postDelayed({
            binding.overlay.visibility = View.GONE
            binding.overlay.isClickable = false
            binding.overlay.isFocusable = false
        }, 400)
        
        // 1. sütun için
        if (fourIsUp) {
            animateBeadsDown(rod0BottomBead4)
            fourIsUp = false
        }
        if (threeIsUp) {
            animateBeadsDown(rod0BottomBead3)
            threeIsUp = false
        }
        if (twoIsUp) {
            animateBeadsDown(rod0BottomBead2)
            twoIsUp = false
        }
        if (oneIsUp) {
            animateBeadsDown(rod0BottomBead1)
            oneIsUp = false
        }
        if (topIsDown) {
            animateBeadUp(rod0TopBead)
            topIsDown = false
        }
        updateRod0BeadsAppearance()

        // 2. sütun için
        if (rod1FourIsUp) {
            animateBeadsDown(rod1BottomBead4)
            rod1FourIsUp = false
        }
        if (rod1ThreeIsUp) {
            animateBeadsDown(rod1BottomBead3)
            rod1ThreeIsUp = false
        }
        if (rod1TwoIsUp) {
            animateBeadsDown(rod1BottomBead2)
            rod1TwoIsUp = false
        }
        if (rod1OneIsUp) {
            animateBeadsDown(rod1BottomBead1)
            rod1OneIsUp = false
        }
        if (rod1TopIsDown) {
            animateBeadUp(rod1TopBead)
            rod1TopIsDown = false
        }
        updateRod1BeadsAppearance()

        // 3. sütun için
        if (rod2FourIsUp) {
            animateBeadsDown(rod2BottomBead4)
            rod2FourIsUp = false
        }
        if (rod2ThreeIsUp) {
            animateBeadsDown(rod2BottomBead3)
            rod2ThreeIsUp = false
        }
        if (rod2TwoIsUp) {
            animateBeadsDown(rod2BottomBead2)
            rod2TwoIsUp = false
        }
        if (rod2OneIsUp) {
            animateBeadsDown(rod2BottomBead1)
            rod2OneIsUp = false
        }
        if (rod2TopIsDown) {
            animateBeadUp(rod2TopBead)
            rod2TopIsDown = false
        }
        updateRod2BeadsAppearance()

        // 4. sütun için
        if (rod3FourIsUp) {
            animateBeadsDown(rod3BottomBead4)
            rod3FourIsUp = false
        }
        if (rod3ThreeIsUp) {
            animateBeadsDown(rod3BottomBead3)
            rod3ThreeIsUp = false
        }
        if (rod3TwoIsUp) {
            animateBeadsDown(rod3BottomBead2)
            rod3TwoIsUp = false
        }
        if (rod3OneIsUp) {
            animateBeadsDown(rod3BottomBead1)
            rod3OneIsUp = false
        }
        if (rod3TopIsDown) {
            animateBeadUp(rod3TopBead)
            rod3TopIsDown = false
        }
        updateRod3BeadsAppearance()

        // 5. sütun için
        if (rod4FourIsUp) {
            animateBeadsDown(rod4BottomBead4)
            rod4FourIsUp = false
        }
        if (rod4ThreeIsUp) {
            animateBeadsDown(rod4BottomBead3)
            rod4ThreeIsUp = false
        }
        if (rod4TwoIsUp) {
            animateBeadsDown(rod4BottomBead2)
            rod4TwoIsUp = false
        }
        if (rod4OneIsUp) {
            animateBeadsDown(rod4BottomBead1)
            rod4OneIsUp = false
        }
        if (rod4TopIsDown) {
            animateBeadUp(rod4TopBead)
            rod4TopIsDown = false
        }
        updateRod4BeadsAppearance()
        updateTopBeadsAppearance()
    }

    private fun disableAllClickable(view: View) {
        view.isClickable = false
        view.isFocusable = false
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                disableAllClickable(view.getChildAt(i))
            }
        }
    }

    private fun enableAllClickable(view: View) {
        view.isClickable = true
        view.isFocusable = true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                enableAllClickable(view.getChildAt(i))
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Bellek sızıntısı olmaması için bırak
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    private fun playCorretSound(soundResId: Int) {
        mediaPlayer?.release() // Önceki sesi serbest bırak
        mediaPlayer = MediaPlayer.create(requireContext(), soundResId)
        mediaPlayer?.start()
    }
    // Ses çalma fonksiyonu
    private fun playSound(soundResource: Int?) {
        soundResource?.let { resourceId ->
            try {
                // Önceki ses varsa durdur
                mediaPlayer?.release()

                // Yeni ses dosyasını çal
                mediaPlayer = MediaPlayer.create(requireContext(), resourceId)
                mediaPlayer?.setOnCompletionListener {
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e("TutorialFragment", "Ses çalma hatası: ${e.message}")
            }
        }
    }


    // Typewriter effect fonksiyonu
    private fun showTextWithTypewriter(text: String, textView: TextView, speed: Long) {
        // Önceki typewriter işlemini durdur
        typewriterRunnable?.let { 
            textView.removeCallbacks(it)
        }
        
        // Önce tam metni set et ki TextView boyutu sabitlensin
        textView.visibility = View.INVISIBLE
        textView.text = text
        // TextView'in boyutunu hesaplaması için bir frame bekleyelim
        textView.post {
            // İlk harfi ortaya yerleştir ve sonraki harfleri sağa doğru ekle
            textView.text = ""
            var currentIndex = 0

            typewriterRunnable = object : Runnable {
                override fun run() {
                    if (currentIndex < text.length) {
                        // Mevcut metni al ve yeni harfi ekle
                        textView.visibility = View.VISIBLE
                        val currentText = if (currentIndex == 0) {
                            // İlk harf için boş string + harf
                            text[currentIndex].toString()

                        } else {
                            // Sonraki harfler için mevcut metin + yeni harf
                            textView.text.toString() + text[currentIndex]
                        }

                        textView.text = currentText
                        currentIndex++
                        textView.postDelayed(this, speed)
                    } else {
                        // Typewriter effect tamamlandı
                        typewriterRunnable = null
                    }
                }
            }
            
            textView.post(typewriterRunnable!!)
        }
    }
    
    // Fragment destroy olduğunda MediaPlayer'ı temizle
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        
        // Typewriter işlemini durdur
        typewriterRunnable?.let {
            binding.tutorialText.removeCallbacks(it)
        }
        typewriterRunnable = null
    }
} 