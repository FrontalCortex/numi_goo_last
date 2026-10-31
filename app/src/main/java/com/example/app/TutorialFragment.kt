package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.activity.OnBackPressedCallback
import com.google.firebase.auth.FirebaseAuth
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.GlobalValues.lessonStep
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.databinding.FragmentTutorialBinding
import com.example.app.abacus.AbacusBeadController
import com.example.app.abacus.AbacusBeadMetrics
import com.example.app.abacus.AbacusBeadRenderer
import com.example.app.abacus.AbacusPreferences
import com.example.app.model.BeadAnimation
import com.example.app.model.LessonItem
import com.example.app.auth.AuthManager
import kotlin.math.roundToInt

class TutorialFragment : Fragment() {
    /**
     * Hangi öğreticinin gösterileceği. Constructor parametresi DEĞİL, [arguments] üzerinden taşınır:
     * sistem fragment'ı kendisi yeniden oluşturduğunda (arka planda süreç öldürülüp geri dönülmesi,
     * "Etkinlikleri saklama" açıkken ekran değişimi) parametresiz constructor çağrılır ve
     * constructor'a verilen değer kaybolurdu; kullanıcı yanlış öğreticinin adımlarını görür,
     * ölçüm de yanlış tutorial numarasına yazılırdı. arguments ise kayıtla birlikte geri gelir.
     */
    private val tutorialNumber: Int
        get() = arguments?.getInt(ARG_TUTORIAL_NUMBER, DEFAULT_TUTORIAL_NUMBER)
            ?: DEFAULT_TUTORIAL_NUMBER
    private lateinit var currentTutorialSteps: List<TutorialStep>
    private var lessonItem: LessonItem? = null

    /**
     * Hesap yokken açılan ilk eğitim (uygulamaya ilk girişteki zorunlu akış). Hesapsız
     * kullanıcı yalnızca bu akışta ders açabildiği için hesap yokluğu yeterli bir işaret;
     * bu akışta "eğitimi atla" gizlenir ve geri tuşu haritaya döndürmez.
     */
    private val isFirstRunTutorial: Boolean
        get() = lessonItem?.tutorialNumber == 1 && FirebaseAuth.getInstance().currentUser == null
    /** Soru adımlarının index → ölçüm kimliği eşlemesi; [buildStepAnalyticsInfo] doldurur. */
    private var stepAnalyticsInfo: Map<Int, StepAnalyticsInfo> = emptyMap()
    private lateinit var controlButton: View
    private lateinit var correctPanel:View
    private lateinit var incorrectPanel:View
    // Sonuç panellerini gezinme çubuğunun arkasına kadar uzatır (ekranın en altından kayarak gelsin)
    private var navBarPanelExtension: NavBarPanelExtension? = null
    private lateinit var questionText:View
    private var controlNumber=0
    private var answerNumber: Int?= null
    private var abacusController: AbacusBeadController? = null
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
    private var optionsContentTextSyncRetries: Int = 0

    /** Quit: girişteki slide_in_left ile uyumlu sağa kayma süresi (slide_out_right). */
    private val tutorialDismissSlideMs = 400L
    private var isTutorialClosing = false

    /** okay / continue / kontrol için çift basım koruması (ms, uptime). */
    private val tutorialResultButtonsCooldownMs = 500L
    private var tutorialResultButtonsCooldownEndUptime = 0L
    private var tutorialResultButtonsCooldownClearRunnable: Runnable? = null

    private fun isTutorialResultButtonsCooldownActive(): Boolean =
        android.os.SystemClock.uptimeMillis() < tutorialResultButtonsCooldownEndUptime

    private fun startTutorialResultButtonsCooldown() {
        tutorialResultButtonsCooldownEndUptime =
            android.os.SystemClock.uptimeMillis() + tutorialResultButtonsCooldownMs
        tutorialResultButtonsCooldownClearRunnable?.let { view?.removeCallbacks(it) }
        val r = Runnable {
            tutorialResultButtonsCooldownEndUptime = 0L
            tutorialResultButtonsCooldownClearRunnable = null
        }
        tutorialResultButtonsCooldownClearRunnable = r
        view?.postDelayed(r, tutorialResultButtonsCooldownMs)
    }
    
    // Ses çalma için MediaPlayer
    private var mediaPlayer: MediaPlayer? = null
    /** Arka plana geçince pause() ile durdurulduysa true; onResume'da kaldığı yerden devam etmek için. */
    private var wasPausedByLifecycle = false

    // Typewriter effect için
    private var typewriterRunnable: Runnable? = null
    /** showTextWithTypewriter()'daki textView.post{} kurulum bloğu - hızlı art arda adım
     * geçişlerinde önceki çağrının kurulum bloğunu iptal edebilmek için saklanır. */
    private var typewriterSetupRunnable: Runnable? = null
    /** Arka plana geçince duraklatıldıysa true; onResume'da kaldığı yerden devam etmek için. */
    private var typewriterPausedByLifecycle = false
    private var typewriterText: String? = null
    private var typewriterCurrentIndex: Int = 0
    private var typewriterSpeed: Long = 40L
    private var learningSessionStartMs: Long? = null


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
    private var infoRequestBreathAnimator: ValueAnimator? = null
    /** "Sobi'ye sor": adımın sohbet durumu, panel, abaküste gösterim ve ekrandaki düğme. */
    private val sobiTracker = SobiStepTracker()
    private var sobiSheet: SobiChatSheet? = null
    private var sobiPlayer: SobiAbacusPlayer? = null
    private var sobiFloatingButton: View? = null
    private var sobiNudge: SobiNudge? = null
    /** "Anlamadığın yeri bana sor" balonu öğreticide bir kez. */
    private var sobiIdleNudgeShown = false
    /** "Birlikte bakalım mı?" adım başına bir kez. */
    private var sobiWrongNudgeStep = -1
    private val sobiIdleNudgeRunnable = Runnable { maybeShowSobiIdleNudge() }
    /** [resetAndWaith] sonrası planlanan [showStep]; üst üste geri basışta iptal edilir. */
    private var pendingBackShowStepRunnable: Runnable? = null
    private var backStepTransitionPending = false
    private var _binding: FragmentTutorialBinding? = null
    private val binding get() = _binding!!
    private var currentStep = 0
    private var backOrFront = true
    private lateinit var focusView: View
    private var requestPanelView: View? = null
    /** [showRequestPanel] post gecikmesinde eski açılışları iptal etmek için. */
    private var requestPanelShowGeneration = 0
    private var originalRootClickListener: View.OnClickListener? = null
    private var tutorialSteps: List<TutorialStep> = emptyList()
    private var tutorialSteps100: List<TutorialStep> = emptyList()
    private var tutorialSteps101: List<TutorialStep> = emptyList()
    private var tutorialSteps102: List<TutorialStep> = emptyList()
    private var tutorialSteps103: List<TutorialStep> = emptyList()
    private var tutorialSteps2: List<TutorialStep> = emptyList()
    private var tutorialSteps3: List<TutorialStep> = emptyList()
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
    private var tutorialSteps1100: List<TutorialStep> = emptyList()
    private var tutorialSteps12: List<TutorialStep> = emptyList()
    private var tutorialSteps13: List<TutorialStep> = emptyList()
    private var tutorialSteps14: List<TutorialStep> = emptyList()
    private var tutorialSteps999: List<TutorialStep> = emptyList()
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
    private var tutorialAbacusMetricsInitialized = false

    // Tutorial için gerekli state'ler
    private var tutorialControlNumber: Int = 0
    private var tutorialRod4FourIsUp: Boolean = false

    companion object {
        /**
         * Sobi'ye bağlam olarak giden önceki adım sayısı: pratikte dersin başından bu adıma kadar
         * hepsi (en uzun öğretici ~80 adım). Çocuk "baştaki kardeş olayı" gibi önceki bir konuyu da
         * sorabiliyor; eskiden 6'ydı. Sunucu en çok 150 adım ve 8.000 karakter alıyor.
         */
        private const val SOBI_RECENT_STEPS = 150
        /** Bir adımda bu kadar kalınınca "Anlamadığın yeri bana sor!" (öğreticide bir kez). */
        private const val SOBI_IDLE_NUDGE_MS = 7000L
        /** Ekrandaki Sobi düğmesi (yalnızca kafa) kare, bu kadar dp. */
        private const val SOBI_BUTTON_SIZE_DP = 48
        /** Öğreticide öğretmene sor düğmesi; kapalı, yerini Sobi aldı. */
        private const val ASK_TEACHER_IN_TUTORIAL = false
        private val INFO_REQUEST_TINT = Color.parseColor("#8BC34A")
        private val INFO_REQUEST_TINT_LIGHT = Color.parseColor("#DFF0D4")
        private const val INFO_REQUEST_BREATH_SCALE_DELTA = 0.14f

        /** Kural metninde [köşeli parantez] ile işaretli kısmın rengi; bkz. markedRuleText. */
        private val RULE_HIGHLIGHT_COLOR = Color.parseColor("#00BFFF")

        private const val ARG_TUTORIAL_NUMBER = "tutorial_number"
        private const val DEFAULT_TUTORIAL_NUMBER = 1

        /** TutorialFragment'ı her zaman bununla oluştur; doğrudan constructor tutorialNumber taşımaz. */
        fun newInstance(tutorialNumber: Int): TutorialFragment {
            val fragment = TutorialFragment()
            fragment.arguments = Bundle().apply { putInt(ARG_TUTORIAL_NUMBER, tutorialNumber) }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalValues.shouldShowAdOnReturn = true
        // Bazı açılış senaryolarında (özellikle login sonrası) GlobalLessonData henüz dolmamış olabiliyor.
        // Burada güvenli bir fallback ile crash'i engelleyip doğru item'ı bulmaya çalışıyoruz.
        LessonManager.ensureInitialized(requireContext(), GlobalLessonData.globalPartId)

        val byPosition = LessonManager.getLessonItem(mapFragmentStepIndex)
        val byTutorialNumber = GlobalLessonData.lessonItems.firstOrNull {
            it.type == LessonItem.TYPE_LESSON && it.tutorialNumber == tutorialNumber
        }
        val firstLesson = GlobalLessonData.lessonItems.firstOrNull { it.type == LessonItem.TYPE_LESSON }

        lessonItem = byPosition ?: byTutorialNumber ?: firstLesson ?: LessonItem(
            // Kalıcı değil: hiçbir ders bulunamadığında UI çökmesin diye yedek.
            stableId = "transient_tutorial_fallback",
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

        // Süreç ölümünden sonra geri yüklenen ekran yeni bir başlangıç değil. onCreate fragment
        // örneği başına bir kez çalıştığı için geri yığınından dönüşte de tekrar gönderilmez.
        if (savedInstanceState == null) {
            AnalyticsLogger.logTutorialStarted(
                tutorialNumber = tutorialNumber,
                lessonId = lessonItem?.stableId,
                firstTime = TutorialStepAnalytics.markStartedOnce(requireContext(), tutorialNumber),
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Tutorial layout'ında çok sayıda hardcoded dp/sp var; küçük ekranlarda devasa görünümü
     * hızlı ve güvenli biçimde azaltmak için ana grupları ölçekler.
     * Metin boyutlarını burada değiştirmeyiz.
     */
    private fun applyTutorialViewScaling() {
        val hDp = resources.configuration.screenHeightDp
        val isShort = hDp in 1..699

        val bottomPanelScale = if (isShort) 0.86f else 1f
        val mediaScale = if (isShort) 0.82f else 0.9f
        val rulesTableScale = if (isShort) 0.58f else 0.94f

        binding.bottomPanelID.apply {
            scaleX = bottomPanelScale
            scaleY = bottomPanelScale
        }

        listOf(
            R.id.blinding_tutorial1,
            R.id.blinding_tutorial2,
            R.id.blinding_tutorial3,
            R.id.rulesBookTutorial,
        ).forEach { id ->
            binding.root.findViewById<View>(id)?.apply {
                scaleX = mediaScale
                scaleY = mediaScale
            }
        }

        listOf(
            R.id.fiveRuleTable,
            R.id.tenRuleTable,
            R.id.tenRuleTableLinearLayout,
            R.id.BeadRuleTable,
            R.id.extractionFiveRuleTable,
            R.id.tenRuleExtractionTableLayout,
            R.id.BeadRuleExtractionTable
        ).forEach { id ->
            binding.root.findViewById<View>(id)?.let { table ->
                // Eskiden ölçek bir kez post{} içinde veriliyordu; ama tablolar o an GONE (genişlik 0)
                // olduğu için pivotX=0 kalıyor, tablo sağ-üst yerine sol-üstten küçülüyordu. Artık her
                // yerleşimde (görünür olunca, içerik/yazı boyutu değişince) yeniden hesaplanıyor.
                table.scaleX = rulesTableScale
                table.scaleY = rulesTableScale
                table.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                    fitRulesTable(v, rulesTableScale)
                }
            }
        }
    }

    /**
     * Kural tablosunu sağ-üst köşesi sabit kalacak şekilde ölçekler; tablo quitButton'un sağındaki
     * alana sığmıyorsa [baseScale]'in de altına küçültür. Yazı boyutu sınırı (FontScaleLimiter)
     * "Ekran boyutu / Yakınlaştırma" ayarını ve dar telefonları kapsamadığı için tablolar hâlâ
     * genişleyip quitButton'un üstüne binebiliyordu. Ölçek yerleşimi değiştirmez, yalnızca çizimi
     * küçültür; böylece hücre metinleri alt satıra kırılmaz.
     */
    private fun fitRulesTable(table: View, baseScale: Float) {
        val tableWidth = table.width
        if (tableWidth <= 0) return
        val parentWidth = (table.parent as? View)?.width ?: return
        val gapPx = 4f * resources.displayMetrics.density
        val safeLeft = binding.quitButton.right + gapPx
        // Taşan tablolarda (genişlik > ekran) sağ kenar ekranın dışında kalabiliyor; görünen sağ
        // kenarı ekranla sınırla ve pivotu ona göre kaydır.
        val visibleRight = minOf(table.right, parentWidth).toFloat()
        val pivot = visibleRight - table.left
        val fitScale = (visibleRight - safeLeft) / pivot
        val scale = minOf(baseScale, fitScale).coerceAtLeast(0.3f)
        table.pivotX = pivot
        table.pivotY = 0f
        table.scaleX = scale
        table.scaleY = scale
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tutorialAbacusMetricsInitialized = false
        applyTutorialViewScaling()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if ((activity as? MainActivity)?.isTeacherSelectingQuestionToSend() == true) return
                if (isFirstRunTutorial) return
                closeFragment()
            }
        })
        findIDs()
        focusView = binding.focusView
        controlButton = binding.kontrolButton
        // Ödev panelindeki gibi içe göçen düğmeler (dudak arka planda: bg_depth_tutorial_*, bg_hw_chip).
        // Kontrol Et, Tamam ve Devam kendi dokunma dinleyicilerinde DepthPress.down/up çağırıyor.
        com.example.app.homework.DepthPress.attach(binding.backButton, binding.skipTutorialButton, binding.devamButton)
        correctPanel = binding.correctPanel
        incorrectPanel = binding.incorrectPanel
        navBarPanelExtension = NavBarPanelExtension(binding.root, correctPanel, incorrectPanel).also { it.attach() }
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
            1100 -> tutorialSteps1100
            12 -> tutorialSteps12
            13 -> tutorialSteps13
            14 -> tutorialSteps14
            999 -> tutorialSteps999
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
        buildStepAnalyticsInfo()
        setupTutorial()
        setupBackButton()
        setupQuitButton()
        setupAskQuestionButton()
        setupInfoRequest()
        tutorialSkipSetup()
        // Tutorial 24 için özel kontrol
        if (tutorialNumber == 24 || tutorialNumber == 25 || tutorialNumber == 26) {
            binding.abacusLinear.visibility = View.INVISIBLE
            binding.abacusContainer.visibility = View.INVISIBLE
        }
        if (tutorialNumber == 1) {
            // Çıkış düğmesi, hesabı olmayan kullanıcının ilk tutorial'inde gizleniyor:
            // akışın sonundaki kayıt adımına ulaşması gerekiyor. Şart eskiden
            // `first_tutorial_shown` idi; o bayrak artık yazılmıyor (bkz. DeviceAccountStore).
            val firstTutorialShown = DeviceAccountStore.hasEverHadAccount(requireContext())
            Log.d(
                MainActivity.FIRST_TUTORIAL_LOG_TAG,
                "TutorialFragment.onViewCreated | tutorialNumber=1 hadAccount=$firstTutorialShown " +
                    "isAdded=$isAdded view=${view != null}",
            )
            if (!firstTutorialShown) {
                binding.quitButton.visibility = View.INVISIBLE
            }
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
        binding.root.setOnClickListener { handleTutorialTapToAdvance() }

        binding.abacusTapBlocker.setOnClickListener { handleTutorialTapToAdvance() }
    }

    /** Root veya abacusTapBlocker: typewriter / seçenekler / sonraki adım (setupTutorial ile aynı kurallar). */
    private fun handleTutorialTapToAdvance() {
        if (isAnyAnimationRunning()) return
        // typewriterRunnable henüz null olabilir ama showTextWithTypewriter()'ın textView.post{}
        // kurulum bloğu (typewriterSetupRunnable) hâlâ kuyrukta olabilir - bu durumda da "yazma
        // animasyonunu atla, tam metni göster" davranışı uygulanmalı. Aksi halde bu dar zaman
        // aralığında gelen bir tıklama, henüz başlamamış bir adımı atlayıp ilerletebiliyor ya da
        // (nextStepAvailable=false ise) hiçbir şey yapmıyor - iki durumda da metin hiç yazılmadan
        // kayboluyor.
        if (typewriterRunnable != null || typewriterSetupRunnable != null) {
            typewriterRunnable?.let { binding.tutorialText.removeCallbacks(it) }
            typewriterRunnable = null
            typewriterSetupRunnable?.let { binding.tutorialText.removeCallbacks(it) }
            typewriterSetupRunnable = null
            val step = getCurrentStep()
            binding.tutorialText.text = step.text
            binding.tutorialText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            return
        }
        if (!getCurrentStep().options.isNullOrEmpty()) return

        if (currentStep < currentTutorialSteps.size - 1 && getCurrentStep().nextStepAvailable) {
            currentStep++
            getCurrentStep().onStepComplete?.invoke()
            backOrFront = true
            showStep(currentStep)
            if (getPlusIndexCurrentStep(-1).nextStepAbacusReset == true) {
                resetAbacus("TAP_ADVANCE_nextStepAbacusReset")
            }
        } else {
            backOrFront = true
        }
    }

    private fun updateAbacusTapBlockerVisibility(step: TutorialStep) {
        val abacusVisible = binding.abacusLinear.visibility == View.VISIBLE
        binding.abacusTapBlocker.visibility =
            if (!step.abacusClickable && abacusVisible) View.VISIBLE else View.GONE
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
        onSobiStepShown(position)
        logTutorialStepReachedIfFirstTime(position)
        logTutorialAbacusResetShowStep(position, "BEFORE_setupBeads")
        // #region agent log
        AgentDebugLog.log(
            hypothesisId = "H3",
            location = "TutorialFragment.showStep",
            message = "showStep_enter",
            data = mapOf(
                "position" to position,
                "currentStep" to currentStep,
                "tutorialNumber" to tutorialNumber,
                "backOrFront" to backOrFront,
                "abacusClickable" to step.abacusClickable,
                "answerNumber" to step.answerNumber,
                "backAnswerNumber" to step.backAnswerNumber,
                "text" to step.text.take(40),
            ),
        )
        // #endregion
        
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
        playSound(step.soundResource, step.soundStoragePath)
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
        updateRulesStepText(step)
        when (tutorialNumber) {
            3 -> binding.fiveRuleTable.visibility = step.rulesPanelVisibility
            4 -> binding.fiveRuleTable.visibility = step.rulesPanelVisibility
            5 -> binding.tenRuleTable.visibility = step.rulesPanelVisibility
            6 -> binding.tenRuleTable.visibility = step.rulesPanelVisibility
            7 -> binding.tenRuleTableLinearLayout.visibility = step.rulesPanelVisibility
            8 -> binding.BeadRuleTable.visibility = step.rulesPanelVisibility
            9 -> binding.BeadRuleTable.visibility = step.rulesPanelVisibility
            11 -> binding.extractionFiveRuleTable.visibility = step.rulesPanelVisibility
            1100 -> binding.extractionFiveRuleTable.visibility = step.rulesPanelVisibility
            12 -> binding.tenRuleExtractionTableLayout.visibility = step.rulesPanelVisibility
            13 -> binding.tenRuleExtractionTableLayout.visibility = step.rulesPanelVisibility
            14 -> binding.tenRuleExtractionTableLayout.visibility = step.rulesPanelVisibility
            17 -> binding.BeadRuleExtractionTable.visibility = step.rulesPanelVisibility
            105 ->binding.tenRuleTableLinearLayout.visibility = step.rulesPanelVisibility
            else -> View.GONE
        }
        updateTutorialTextAnchor()

        // Seçenek paneli yönetimi tamamen updateOptionsPanelForStep içinde yapılır
        updateOptionsPanelForStep(step)
        answerNumber=step.answerNumber
        setupBeads()
        logTutorialAbacusResetShowStep(position, "AFTER_setupBeads")

        if(getCurrentStep().abacusClickable){
            controlButton.visibility = View.VISIBLE
            controlButtonListener = View.OnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (isTutorialResultButtonsCooldownActive()) return@OnTouchListener true
                        com.example.app.homework.DepthPress.down(v)
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isTutorialResultButtonsCooldownActive()) return@OnTouchListener true
                        com.example.app.homework.DepthPress.up(v)

                        // Tıklama işlemini gerçekleştir
                        showResultPanel()
                        controlNumber = 0
                        true
                    }

                    // Kayınca basılı durum ve göçme birlikte değişsin; olay burada tüketilir ki görünümün kendi
                    // onTouchEvent'i basılı durumu ayrıca bırakıp düğmeyi aşağıda unutmasın.
                    MotionEvent.ACTION_MOVE -> {
                        com.example.app.homework.DepthPress.move(v, event)
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
        ensureTutorialAbacusMetricsIfVisible()
        updateAbacusTapBlockerVisibility(step)
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

    private fun setupAskQuestionButton() {
        // Öğreticide öğretmene danışma yerine Sobi'ye sor kullanılıyor (kullanıcı kararı 28.10.2026);
        // düğme gizli, kod ileride yeniden açılabilsin diye duruyor.
        if (!ASK_TEACHER_IN_TUTORIAL) {
            binding.askQuestionButton.visibility = View.GONE
            return
        }
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
    
    private fun setupInfoRequest() {
        binding.infoRequest.setOnClickListener {
            if (hasActiveRequestPanel()) {
                dismissAllRequestPanels(animated = true)
            } else {
                val requestText = getCurrentStep().requestText
                if (requestText != null && requestText.isNotEmpty()) {
                    showRequestPanel(requestText)
                }
            }
        }
    }

    // ── Sobi'ye sor ─────────────────────────────────────────────────────────
    //
    // Durum SobiStepTracker'da, panel SobiChatSheet'te, gösterim SobiAbacusPlayer'da; burada
    // yalnızca öğretici adımına bağlanma var. İki giriş: ekranın sağ üstünde hep duran "Sobi'ye sor"
    // düğmesi (anlatımın her yerinde soru sorulabilsin; kullanıcı kararı 28.10.2026) ve yanlış cevap
    // panelindeki düğme ([bindIncorrectPanelHelp]). Sohbet öğretici boyunca sürer; abaküste gösterim
    // yalnızca soru adımında.

    /**
     * Yanlış cevap panelinin içi: adımın ipucu (requestText) ve "Sobi'ye sor". Eskiden ipucu bilgi
     * düğmesiyle yukarıdan inen panelde açılıyordu; kullanıcı kararıyla (28.10.2026) hepsi yanlış
     * cevap panelinde, bilgi düğmesi gizli.
     */
    private fun bindIncorrectPanelHelp() {
        val text = getCurrentStep().requestText
        incorrectPanel.findViewById<TextView>(R.id.incorrectHintText).apply {
            this.text = text.orEmpty()
            visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        val sobiButton = incorrectPanel.findViewById<View>(R.id.incorrectSobiButton).apply {
            visibility = if (isSobiChatAvailable()) View.VISIBLE else View.GONE
            setOnClickListener { if (!isTutorialResultButtonsCooldownActive()) openSobi() }
            com.example.app.homework.DepthPress.attach(this)
        }
        // Aynı adımda ikinci yanlış: Sobi kendiliğinden "Birlikte bakalım mı?" der (adım başına bir
        // kez). Panel 200 ms'de kayarak geliyor; balonun yeri panel yerine oturunca hesaplanmalı.
        val step = sobiTracker.stepIndex
        if (isSobiChatAvailable() && isSobiQuestionStep() && sobiTracker.attempts >= 2 && sobiWrongNudgeStep != step) {
            sobiWrongNudgeStep = step
            sobiButton.postDelayed({
                if (_binding == null || incorrectPanel.visibility != View.VISIBLE || sobiTracker.stepIndex != step) return@postDelayed
                sobiNudge()?.show(sobiButton, appString(R.string.sobi_nudge_wrong)) { openSobi() }
            }, 450L)
        }
    }

    /** Sohbet açılabilir mi: oturum açık olmalı (sunucu oturumsuz çağrıyı reddeder). */
    private fun isSobiChatAvailable(): Boolean =
        _binding != null && FirebaseAuth.getInstance().currentUser != null

    /** Abaküslü soru adımı mı: abaküs bağlamı, hata bulma ve gösterim yalnızca burada. */
    private fun isSobiQuestionStep(): Boolean {
        if (_binding == null || abacusController == null) return false
        val step = getCurrentStep()
        return step.abacusClickable && step.answerNumber != null && step.options.isNullOrEmpty()
    }

    private fun onSobiStepShown(position: Int) {
        if (position == sobiTracker.stepIndex) return // aynı adım yeniden kuruldu (ör. yanlış sonrası)
        // Sohbet sürer (panel açık kalabilir); adım durumu ve bir önceki adımın gösterimi biter.
        sobiPlayer?.cancel()
        sobiTracker.startStep(position)
        updateSobiFloatingButton()
        scheduleSobiBaseCapture(position, attempt = 0)
        sobiNudge?.hide()
        _binding?.root?.let { root ->
            root.removeCallbacks(sobiIdleNudgeRunnable)
            if (!sobiIdleNudgeShown) root.postDelayed(sobiIdleNudgeRunnable, SOBI_IDLE_NUDGE_MS)
        }
    }

    private fun sobiNudge(): SobiNudge? {
        val root = _binding?.root as? ViewGroup ?: return null
        return sobiNudge ?: SobiNudge(root).also { sobiNudge = it }
    }

    /**
     * Çocuk bir adımda 7 sn'den fazla kaldıysa sağ üstteki Sobi kafasının yanında "Anlamadığın yeri
     * bana sor!" (öğreticide bir kez; kullanıcı kararı 28.10.2026). Sonuç paneli, Sobi paneli ya da
     * gösterim açıksa bu adımda açılmaz, sonraki adımda yine denenir.
     */
    private fun maybeShowSobiIdleNudge() {
        if (sobiIdleNudgeShown || _binding == null) return
        val button = sobiFloatingButton ?: return
        val busy = sobiSheet?.isShowing == true || sobiPlayer?.isPlaying == true ||
            incorrectPanel.visibility == View.VISIBLE || correctPanel.visibility == View.VISIBLE
        if (busy || button.visibility != View.VISIBLE) return
        sobiIdleNudgeShown = true
        sobiNudge()?.show(button, appString(R.string.sobi_nudge_idle)) { openSobi() }
    }

    /**
     * Adımın başlangıç değeri: adımın boncuk animasyonları ve cevap yazımı bitince. Hemen okunsa
     * ara bir değer (yarım kalmış animasyon) alınırdı; çocuk o arada dokunursa da en geç 10 sn'de alınır.
     */
    private fun scheduleSobiBaseCapture(position: Int, attempt: Int) {
        val root = _binding?.root ?: return
        root.postDelayed({
            if (_binding == null || sobiTracker.stepIndex != position) return@postDelayed
            val controller = abacusController
            val busy = controller == null || isWritingAnswerNumber || controller.isResetInProgress() ||
                controller.isBeadAnimationInProgress() || currentAnimations.any { it.isAnimating() }
            if (busy && attempt < 25) {
                scheduleSobiBaseCapture(position, attempt + 1)
            } else if (controller != null) {
                sobiTracker.captureBase(controller.getCurrentValue())
            }
        }, 400L)
    }

    /** Gönderim anındaki bağlam: soru adımında abaküs alanları, her adımda ders ve son adımlar. */
    private fun sobiContext(): SobiContext? {
        if (!isSobiChatAvailable()) return null
        val step = getCurrentStep()
        val recent = currentTutorialSteps
            .subList(maxOf(0, currentStep - SOBI_RECENT_STEPS), currentStep)
            .map { it.text }
            .filter { it.isNotBlank() }
        val lessonTitle = lessonItem?.title
        val rules = SobiCurriculum.taughtRules(tutorialNumber)
        val ctx = if (isSobiQuestionStep()) {
            sobiTracker.questionContext(
                stepId = step.id, stepText = step.text, questionText = step.questionText,
                target = step.answerNumber, current = abacusController?.getCurrentValue(),
                lessonTitle = lessonTitle, recentSteps = recent, taughtRules = rules,
            )
        } else {
            sobiTracker.explanationContext(
                stepId = step.id, stepText = step.text, questionText = step.questionText,
                lessonTitle = lessonTitle, recentSteps = recent, taughtRules = rules,
            )
        }
        // Sunucu bu adımın hazır açıklamasını bulsun (öğretici + adım sırası, metinle denetleniyor).
        return ctx.copy(tutorialNumber = tutorialNumber, stepIndex = currentStep)
    }

    private fun openSobi() {
        if (!isSobiChatAvailable()) return
        sobiNudge?.hide()
        // Yanlış cevap paneli açıksa kapat: adım yeniden deneme durumuna dönsün, abaküs görünsün.
        if (incorrectPanel.visibility == View.VISIBLE) dismissIncorrectResultPanel()
        dismissAllRequestPanels(animated = true)
        val sheet = sobiSheet ?: SobiChatSheet(
            fragment = this,
            tracker = sobiTracker,
            contextProvider = { sobiContext() },
            onShow = { playSobiAction(it) },
            onClosed = { updateSobiFloatingButton() },
            isQuestionStep = { isSobiQuestionStep() },
        ).also { sobiSheet = it }
        sheet.show()
        updateSobiFloatingButton()
    }

    private fun playSobiAction(action: ServerSobi.Action) {
        // Sunucu anlatım adımında gösterim aracını vermiyor; yine de gelirse dersin abaküsüne dokunma.
        if (!isSobiQuestionStep()) return
        val controller = abacusController ?: return
        if (incorrectPanel.visibility == View.VISIBLE) dismissIncorrectResultPanel()
        val player = sobiPlayer ?: SobiAbacusPlayer(controller).also { sobiPlayer = it }
        sobiTracker.paused = true
        controller.setEnabled(false)
        val restore = {
            sobiTracker.paused = false
            if (_binding != null) controller.setEnabled(getCurrentStep().abacusClickable && !isWritingAnswerNumber)
        }
        val started = player.play(action) {
            if (_binding == null) return@play
            restore()
            Toast.makeText(requireContext(), appString(R.string.sobi_after_show), Toast.LENGTH_SHORT).show()
            updateSobiFloatingButton()
        }
        if (!started) {
            restore()
            Toast.makeText(requireContext(), appString(R.string.sobi_show_failed), Toast.LENGTH_SHORT).show()
        }
        updateSobiFloatingButton()
    }

    /**
     * Ekranın sağ üstünde, öğretmene sor düğmesinin altında hep duran "Sobi'ye sor". Panel açıkken
     * ve gösterim sürerken gizli. Sonuç panelleri açıkken önlerine konan katman onu zaten örtüyor.
     */
    private fun updateSobiFloatingButton() {
        val root = _binding?.root as? ConstraintLayout ?: return
        val visible = isSobiChatAvailable() && sobiSheet?.isShowing != true && sobiPlayer?.isPlaying != true
        val button = sobiFloatingButton ?: if (!visible) return else LayoutInflater.from(requireContext())
            .inflate(R.layout.view_sobi_chip, root, false).apply {
                setOnClickListener { openSobi() }
                com.example.app.homework.DepthPress.attach(this)
                val d = resources.displayMetrics.density
                root.addView(this, ConstraintLayout.LayoutParams(
                    (SOBI_BUTTON_SIZE_DP * d).toInt(), (SOBI_BUTTON_SIZE_DP * d).toInt(),
                ).apply {
                    // Öğretmene sor düğmesi gizliyken (GONE) onun yerine oturur.
                    topToBottom = R.id.askQuestionButton
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topMargin = (8 * d).toInt()
                    marginEnd = (8 * d).toInt()
                })
            }.also { sobiFloatingButton = it }
        button.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun collectRequestPanels(): List<View> {
        val root = binding.root as ViewGroup
        return (0 until root.childCount)
            .map { root.getChildAt(it) }
            .filter { it.id == R.id.requestPanel }
    }

    private fun hasActiveRequestPanel(): Boolean = collectRequestPanels().isNotEmpty()

    private fun removeOrphanRequestPanels(except: View? = null) {
        val root = binding.root as ViewGroup
        collectRequestPanels()
            .filter { it !== except }
            .forEach { panel ->
                panel.animate().cancel()
                root.removeView(panel)
            }
    }

    private fun dismissRequestPanelsImmediate() {
        requestPanelShowGeneration++
        val root = binding.root as ViewGroup
        collectRequestPanels().forEach { panel ->
            panel.animate().cancel()
            root.removeView(panel)
        }
        requestPanelView = null
        binding.overlay.animate().cancel()
        binding.overlay.visibility = View.GONE
        restoreOriginalRootClickListener()
    }

    private fun dismissAllRequestPanels(animated: Boolean) {
        val panels = collectRequestPanels()
        if (panels.isEmpty()) {
            requestPanelView = null
            return
        }
        if (animated && panels.size == 1) {
            val panel = panels.single()
            requestPanelView = panel
            hideRequestPanel(panel)
        } else {
            dismissRequestPanelsImmediate()
        }
    }
    
    private fun requestPanelSlideOffset(panelView: View): Float {
        if (panelView.height > 0) return panelView.height.toFloat()
        val parentWidth = (panelView.parent as? View)?.width
            ?: (binding.root as ViewGroup).width
        panelView.measure(
            View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        return panelView.measuredHeight.toFloat().coerceAtLeast(1f)
    }

    private fun showRequestPanel(text: String) {
        dismissRequestPanelsImmediate()

        val panelView = LayoutInflater.from(requireContext())
            .inflate(R.layout.request_panel, binding.root as ViewGroup, false)

        val requestTextView = panelView.findViewById<TextView>(R.id.requestText)
        requestTextView.text = text

        panelView.setOnClickListener { }

        // Final konumda bir kare görünmesin: eklemeden önce gizle ve sıfırla.
        panelView.visibility = View.INVISIBLE
        panelView.alpha = 0f
        panelView.translationX = 0f
        panelView.translationY = 0f

        (binding.root as ViewGroup).addView(panelView)
        requestPanelView = panelView

        val generation = ++requestPanelShowGeneration
        panelView.post {
            if (generation != requestPanelShowGeneration || requestPanelView !== panelView) {
                panelView.animate().cancel()
                (binding.root as ViewGroup).removeView(panelView)
                return@post
            }

            val slideOffset = requestPanelSlideOffset(panelView)
            panelView.translationY = -slideOffset

            binding.overlay.visibility = View.VISIBLE
            binding.overlay.alpha = 0f

            binding.overlay.setOnClickListener {
                hideRequestPanel(panelView)
            }

            val rootClickListener = View.OnClickListener { view ->
                if (view != panelView && !isViewInsidePanel(view, panelView)) {
                    hideRequestPanel(panelView)
                }
            }
            binding.root.setOnClickListener(rootClickListener)

            binding.overlay.animate()
                .alpha(0.5f)
                .setDuration(200)
                .start()

            panelView.visibility = View.VISIBLE
            panelView.animate()
                .alpha(1f)
                .translationY(0f)
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
        requestPanelShowGeneration++
        panelView.animate().cancel()
        binding.overlay.animate().cancel()

        if (panelView.parent == null) {
            if (requestPanelView === panelView) {
                requestPanelView = null
            }
            removeOrphanRequestPanels()
            if (!hasActiveRequestPanel()) {
                binding.overlay.visibility = View.GONE
                restoreOriginalRootClickListener()
            }
            return
        }

        binding.overlay.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!hasActiveRequestPanel()) {
                        binding.overlay.visibility = View.GONE
                    }
                }
            })
            .start()

        val slideOffset = requestPanelSlideOffset(panelView)
        panelView.animate()
            .alpha(0f)
            .translationY(-slideOffset)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    (binding.root as ViewGroup).removeView(panelView)
                    if (requestPanelView === panelView) {
                        requestPanelView = null
                    }
                    removeOrphanRequestPanels()
                    if (!hasActiveRequestPanel()) {
                        binding.overlay.visibility = View.GONE
                        restoreOriginalRootClickListener()
                    }
                }
            })
            .start()
    }
    
    private fun restoreOriginalRootClickListener() {
        binding.root.setOnClickListener { handleTutorialTapToAdvance() }
        binding.abacusTapBlocker.setOnClickListener { handleTutorialTapToAdvance() }
    }
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            logTutorialBackPress("press")
            // Seçenek paneli açık ve kilitliyken geri butonu çalışmasın
            if (optionsInteractionLocked && optionsPanel.visibility == View.VISIBLE) {
                logTutorialBackPress("ignored", "optionsLocked")
                return@setOnClickListener
            }
            // Herhangi bir animasyon devam ediyorsa geri gitmeyi engelle
            if (isAnyAnimationRunning()) {
                logTutorialBackPress("ignored", "isAnyAnimationRunning")
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
                // writeAnswerNumber ile mutlak durum kurulduktan sonra showStep'in geri dalında
                // (position+1).animation tersine çevrilirse boncuklar tekrar oynar ve hedef bozulur.
                val skipReverseBeadAnims = getCurrentStep().backAnswerNumber != null

                if(binding.abacusLinear.visibility == View.INVISIBLE && tutorialNumber<24){
                    binding.abacusLinear.visibility = View.VISIBLE
                    binding.abacusContainer.visibility = View.VISIBLE
                }
                if(binding.abacusLinear.visibility == View.INVISIBLE && tutorialNumber>99){
                    binding.abacusLinear.visibility = View.VISIBLE
                    binding.abacusContainer.visibility = View.VISIBLE
                }
                if(getCurrentStep().backAnswerNumber != null){
                    writeAnswerNumber(getCurrentStep().backAnswerNumber!!)
                }
                if(getCurrentStep().abacusReset==true){
                    logTutorialAbacusResetBack("abacusReset_on_landed_step")
                    resetAbacus("BACK_BUTTON_abacusReset")
                }
                if (getPlusIndexCurrentStep(1).resetAndWaith == true) {
                    logTutorialBackPress("resetAndWaith", "resetAbacus then wait 800ms")
                    resetAbacus("BACK_BUTTON_resetAndWaith")
                    scheduleBackShowStep(delayMs = 800L, skipReverseBeadAnims = skipReverseBeadAnims)
                } else {
                    logTutorialBackPress("immediateShowStep")
                    showStep(currentStep, skipAnimations = skipReverseBeadAnims)
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
    /**
     * Hedef basamak değerine göre alt boncukları günceller.
     * Animasyon yalnızca fiziksel konumu hedefle uyuşmayan boncuklarda (boolean/sync değil).
     */
    private fun updateBeadForDigit(
        digit: Int,
        beadViews: List<ImageView>,
        beadStates: MutableList<Boolean>,
        rod: Int,
    ) {
        val controller = abacusController ?: return
        val targetBottom = digit % 5

        for (i in 0..3) {
            val shouldBeUp = (i + 1) <= targetBottom
            val physicallyUp = controller.isBottomBeadPhysicallyRaised(rod, i)

            if (TutorialAbacusResetDiagnostics.ENABLED && rod == 3) {
                TutorialAbacusResetDiagnostics.log(
                    "updateBeadForDigit rod=$rod bead=${i + 1} digit=$digit " +
                        "shouldBeUp=$shouldBeUp physicallyUp=$physicallyUp " +
                        "willAnimate=${shouldBeUp != physicallyUp}",
                )
            }

            if (shouldBeUp == physicallyUp) {
                beadStates[i] = shouldBeUp
                updateBeadAppearance(beadViews[i], shouldBeUp)
                continue
            }

            if (shouldBeUp) {
                animateBeadsUp(beadViews[i])
                updateBeadAppearance(beadViews[i], true)
            } else {
                animateBeadsDown(beadViews[i])
                updateBeadAppearance(beadViews[i], false)
            }
            beadStates[i] = shouldBeUp
        }
    }

    private fun logWriteAnswerNumber(phase: String, number: Int, extra: String = "") {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        TutorialAbacusResetDiagnostics.log(
            "WRITE_ANSWER_NUMBER $phase target=$number currentStep=$currentStep " +
                "isWriting=$isWritingAnswerNumber controllerValue=${abacusController?.getCurrentValue()} " +
                "rod3Up=($rod3OneIsUp,$rod3TwoIsUp,$rod3ThreeIsUp,$rod3FourIsUp) $extra",
        )
        abacusController?.logInternalState("WRITE_ANSWER_NUMBER $phase")
    }

    private fun writeAnswerNumber(number: Int, onComplete: (() -> Unit)? = null) {
        logWriteAnswerNumber("START", number)
        isWritingAnswerNumber = true
        abacusController?.setEnabled(false)
        abacusController?.logInternalState("writeAnswerNumber START")

        val numberStr = number.toString().padStart(5, '0')

        var tenThousands = numberStr[0].toString().toInt()    // On binler basamağı
        var thousands = numberStr[1].toString().toInt()    // Binler basamağı
        var hundreds = numberStr[2].toString().toInt()    // Yüzler basamağı
        var tens = numberStr[3].toString().toInt()    // Onlar basamağı
        var ones = numberStr[4].toString().toInt()    // Birler basamağı


        val ctrl = abacusController

        // Birler basamağı kontrolleri
        if (ones < 5) {
            if (ctrl?.isTopBeadDown(4) == true) {
                animateBeadUp(rod4TopBead)
            }
            rod4TopIsDown = false
            updateBeadAppearance(rod4TopBead, false)
        }
        if (ones >= 5) {
            if (ctrl?.isTopBeadDown(4) != true) {
                animateBeadDown(rod4TopBead)
            }
            ones -= 5
            rod4TopIsDown = true
            updateBeadAppearance(rod4TopBead, true)
        }
            val rod4BeadViews = listOf(rod4BottomBead1, rod4BottomBead2, rod4BottomBead3, rod4BottomBead4)
            val rod4BeadStates = mutableListOf(rod4OneIsUp, rod4TwoIsUp, rod4ThreeIsUp, rod4FourIsUp)
            updateBeadForDigit(ones, rod4BeadViews, rod4BeadStates, rod = 4)
            
            // Boolean değişkenleri güncelle
            rod4OneIsUp = rod4BeadStates[0]
            rod4TwoIsUp = rod4BeadStates[1]
            rod4ThreeIsUp = rod4BeadStates[2]
            rod4FourIsUp = rod4BeadStates[3]

        // Onlar basamağı kontrolleri
        if (tens < 5) {
            if (ctrl?.isTopBeadDown(3) == true) {
                animateBeadUp(rod3TopBead)
            }
            rod3TopIsDown = false
            updateBeadAppearance(rod3TopBead, false)
        }
        if (tens >= 5) {
            if (ctrl?.isTopBeadDown(3) != true) {
                animateBeadDown(rod3TopBead)
            }
            rod3TopIsDown = true
            updateBeadAppearance(rod3TopBead, true)
            tens -= 5
        }
        val rod3BeadViews = listOf(rod3BottomBead1, rod3BottomBead2, rod3BottomBead3, rod3BottomBead4)
        val rod3BeadStates = mutableListOf(rod3OneIsUp, rod3TwoIsUp, rod3ThreeIsUp, rod3FourIsUp)
        updateBeadForDigit(tens, rod3BeadViews, rod3BeadStates, rod = 3)
        
        // Boolean değişkenleri güncelle
        rod3OneIsUp = rod3BeadStates[0]
        rod3TwoIsUp = rod3BeadStates[1]
        rod3ThreeIsUp = rod3BeadStates[2]
        rod3FourIsUp = rod3BeadStates[3]
        logWriteAnswerNumber(
            "AFTER_ROD3_ANIM_SCHEDULED",
            number,
            "rod3Up=($rod3OneIsUp,$rod3TwoIsUp,$rod3ThreeIsUp,$rod3FourIsUp)",
        )
        // Yüzler basamağı kontrolleri
        if (hundreds < 5) {
            if (ctrl?.isTopBeadDown(2) == true) {
                animateBeadUp(rod2TopBead)
            }
            rod2TopIsDown = false
            updateBeadAppearance(rod2TopBead, false)
        }
        if (hundreds >= 5) {
            if (ctrl?.isTopBeadDown(2) != true) {
                animateBeadDown(rod2TopBead)
            }
            rod2TopIsDown = true
            updateBeadAppearance(rod2TopBead, true)
            hundreds -= 5
        }
        val rod2BeadViews = listOf(rod2BottomBead1, rod2BottomBead2, rod2BottomBead3, rod2BottomBead4)
        val rod2BeadStates = mutableListOf(rod2OneIsUp, rod2TwoIsUp, rod2ThreeIsUp, rod2FourIsUp)
        updateBeadForDigit(hundreds, rod2BeadViews, rod2BeadStates, rod = 2)
        
        // Boolean değişkenleri güncelle
        rod2OneIsUp = rod2BeadStates[0]
        rod2TwoIsUp = rod2BeadStates[1]
        rod2ThreeIsUp = rod2BeadStates[2]
        rod2FourIsUp = rod2BeadStates[3]
        // Binler basamağı kontrolleri
        if (thousands < 5) {
            if (ctrl?.isTopBeadDown(1) == true) {
                animateBeadUp(rod1TopBead)
            }
            rod1TopIsDown = false
            updateBeadAppearance(rod1TopBead, false)
        }
        if (thousands >= 5) {
            if (ctrl?.isTopBeadDown(1) != true) {
                animateBeadDown(rod1TopBead)
            }
            rod1TopIsDown = true
            updateBeadAppearance(rod1TopBead, true)
            thousands -= 5
        }
        val rod1BeadViews = listOf(rod1BottomBead1, rod1BottomBead2, rod1BottomBead3, rod1BottomBead4)
        val rod1BeadStates = mutableListOf(rod1OneIsUp, rod1TwoIsUp, rod1ThreeIsUp, rod1FourIsUp)
        updateBeadForDigit(thousands, rod1BeadViews, rod1BeadStates, rod = 1)
        
        // Boolean değişkenleri güncelle
        rod1OneIsUp = rod1BeadStates[0]
        rod1TwoIsUp = rod1BeadStates[1]
        rod1ThreeIsUp = rod1BeadStates[2]
        rod1FourIsUp = rod1BeadStates[3]
        // On binler basamağı kontrolleri (üst boncuk hepsi için tenThousands ile kontrol edilmeli)
        if (tenThousands < 5) {
            if (ctrl?.isTopBeadDown(0) == true) {
                animateBeadUp(rod0TopBead)
            }
            topIsDown = false
            updateBeadAppearance(rod0TopBead, false)
        }
        if (tenThousands >= 5) {
            if (ctrl?.isTopBeadDown(0) != true) {
                animateBeadDown(rod0TopBead)
            }
            topIsDown = true
            updateBeadAppearance(rod0TopBead, true)
            tenThousands -= 5
        }
        val rod0BeadViews = listOf(rod0BottomBead1, rod0BottomBead2, rod0BottomBead3, rod0BottomBead4)
        val rod0BeadStates = mutableListOf(oneIsUp, twoIsUp, threeIsUp, fourIsUp)
        updateBeadForDigit(tenThousands, rod0BeadViews, rod0BeadStates, rod = 0)
        
        // Boolean değişkenleri güncelle
        oneIsUp = rod0BeadStates[0]
        twoIsUp = rod0BeadStates[1]
        threeIsUp = rod0BeadStates[2]
        fourIsUp = rod0BeadStates[3]
        
        // Margin animasyonları 300ms; bitince sayıdan iç state + drawable hizala (margin sync değil).
        // postDelayedSafely: view yok edilmişse geri çağrı sessizce atlanır.
        postDelayedSafely(400) {
            logWriteAnswerNumber("BEFORE_POST_COMPLETE", number)
            isWritingAnswerNumber = false
            abacusController?.applyInternalStateFromValue(number, applyAppearance = true)
            syncTutorialBooleansFromController()
            logWriteAnswerNumber("AFTER_POST_COMPLETE", number)
            if (getCurrentStep().abacusClickable) {
                abacusController?.setEnabled(true)
            }
            onComplete?.invoke()
        }
    }
    private fun closeFragment() { // Fragment'i kapat ve MapFragment'e dön
        if (!isAdded || isTutorialClosing) return
        val rootView = view
        if (rootView == null) {
            performTutorialDismiss()
            return
        }
        isTutorialClosing = true
        binding.quitButton.isEnabled = false
        // Sola: ders akışındaki bütün ekranlar gibi. Sağa kayıyordu, yani geldiği yöne
        // geri dönüyor gibi duruyordu.
        val slideDistance = -resources.displayMetrics.widthPixels.toFloat()
        rootView.animate().cancel()
        rootView.animate()
            .translationX(slideDistance)
            .setDuration(tutorialDismissSlideMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (isAdded) {
                    performTutorialDismiss()
                }
            }
            .start()
    }

    /** [closeFragment] kayma animasyonu bittikten sonra: mevcut kapanış / sezon kapısı akışı. */
    private fun performTutorialDismiss() {
        val act = activity as? MainActivity
        act?.setActiveMapTutorialOverlayFromLesson(false)
        act?.logMapTouchDiag(
            "TutorialFragment.quit",
            "DISMISS_ENTER",
            "path=popBackStack+scheduleSeasonGate (prepareMapReturn YOK)",
        )
        // popBackStack öncesi: destroy callback reconcile'ı VISIBLE+Abacus ile kilitlemesin.
        act?.beginAbacusOverlayDismissForSeasonGate()
        val fm = parentFragmentManager
        // LessonAdapter replace+addToBackStack ile açıyor; yalnızca remove hayaleti geri bırakabiliyor.
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
            fm.executePendingTransactions()
        } else if (isAdded) {
            fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
                .remove(this)
                .commitNowAllowingStateLoss()
        }
        act?.scheduleSeasonGateAfterAbacusOverlayDismissed()
        // Bu yol bilinçli olarak harita dönüş akışını (prepareMapReturn/finalizeMapReturn ve
        // dolayısıyla rozet/rehber/rating/reklam) çalıştırmıyor. Türü LESSON olan bir item'dan
        // çıkıldıysa yalnızca AskQuestionOpen promosu denenir; başka hiçbir yapı tetiklenmez.
        if (lessonItem?.type == LessonItem.TYPE_LESSON) {
            act?.tryShowAskQuestionPromoOnly("TutorialFragment.quit")
        }
    }
    private fun devametFragment(fragment: Fragment) {
        // Devam butonunu ekranın altından kayarak göster
        binding.abacusLinear.visibility = View.INVISIBLE
        binding.abacusContainer.visibility = View.INVISIBLE
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
            (activity as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(false)

            // Tutorial soru panelini aç; panel bittikten sonra asıl geçişi yap
            val globalPartId = GlobalLessonData.globalPartId
            // Ders çözülemezse kimlik boş gider; panel yine açılır, yalnızca anket kaydı atlanır
            // (bkz. TutorialQuestionPanelFragment.submitAndProceed).
            val lessonId = lessonItem?.stableId.orEmpty()
            val panelFragment = TutorialQuestionPanelFragment.newInstance(
                globalPartId = globalPartId,
                lessonId = lessonId,
            )

            parentFragmentManager.setFragmentResultListener(
                "tutorialQuestionPanelResult",
                viewLifecycleOwner
            ) { _, _ ->
                if (!isAdded) return@setFragmentResultListener
                // Panel bitti — asıl derse geçiş
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.queue_screen_in,
                        R.anim.queue_screen_out
                    )
                    .replace(R.id.abacusFragmentContainer, fragment)
                    .commit()
                currentStep = 0
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .remove(this)
                    .commit()
            }

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.queue_screen_in,
                    R.anim.queue_screen_out
                )
                .add(R.id.abacusFragmentContainer, panelFragment)
                .hide(this)
                .commit()
        }
    }
    private fun isAnyAnimationRunning(): Boolean {
        return backStepTransitionPending ||
            (abacusController?.isResetInProgress() == true) ||
            (getCurrentStep().abacusClickable && abacusController?.isBeadAnimationInProgress() == true) ||
            currentAnimations.any { it.isAnimating() } ||
            widgetAnimators.any { it.isRunning }
    }

    private fun logTutorialAbacusResetInvoke(reason: String) {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        val value = abacusController?.getCurrentValue()
        TutorialAbacusResetDiagnostics.log(
            "resetAbacus INVOKE reason=$reason currentStep=$currentStep " +
                "backOrFront=$backOrFront controllerValue=$value " +
                "resetInProgress=${abacusController?.isResetInProgress() == true} " +
                "step=${TutorialAbacusResetDiagnostics.stepLabel(getCurrentStep())}",
        )
    }

    private fun logTutorialAbacusResetKontrol(
        step: TutorialStep,
        controlValue: Int,
        correct: Boolean,
    ) {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        TutorialAbacusResetDiagnostics.log(
            "KONTROL_ET step=$currentStep correct=$correct control=$controlValue " +
                "expected=${step.answerNumber} ${TutorialAbacusResetDiagnostics.stepFlags(step)} " +
                "text=${TutorialAbacusResetDiagnostics.stepLabel(step)}",
        )
    }

    private fun logTutorialAbacusResetForwardContinue(departedIndex: Int) {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        val departed = currentTutorialSteps.getOrNull(departedIndex)
        val landed = getCurrentStep()
        val willReset = getPlusIndexCurrentStep(-1).nextStepAbacusReset == true
        val controllerValue = abacusController?.getCurrentValue()
        TutorialAbacusResetDiagnostics.log(
            "FORWARD_CONTINUE departedIndex=$departedIndex landedStep=$currentStep " +
                "willCallResetAbacus=$willReset (onlyIf_nextStepAbacusReset_on_departed) " +
                "departedAbacusReset=${departed?.abacusReset} " +
                "(NOTE: abacusReset is NOT read on forward; only on BACK/INCORRECT) " +
                "controllerValue=$controllerValue",
        )
        if (departed != null) {
            TutorialAbacusResetDiagnostics.log(
                "FORWARD_CONTINUE departed: ${TutorialAbacusResetDiagnostics.stepFlags(departed)} " +
                    "text=${TutorialAbacusResetDiagnostics.stepLabel(departed)}",
            )
        }
        TutorialAbacusResetDiagnostics.log(
            "FORWARD_CONTINUE landed: ${TutorialAbacusResetDiagnostics.stepFlags(landed)} " +
                "text=${TutorialAbacusResetDiagnostics.stepLabel(landed)}",
        )
    }

    private fun logTutorialAbacusResetIncorrect(
        step: TutorialStep,
        needsReset: Boolean,
        backNum: Int?,
    ) {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        TutorialAbacusResetDiagnostics.log(
            "INCORRECT_RETRY step=$currentStep needsReset=$needsReset backAnswerNumber=$backNum " +
                "${TutorialAbacusResetDiagnostics.stepFlags(step)} " +
                "text=${TutorialAbacusResetDiagnostics.stepLabel(step)}",
        )
    }

    private fun logTutorialAbacusResetBack(phase: String) {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        val step = getCurrentStep()
        TutorialAbacusResetDiagnostics.log(
            "BACK_BUTTON phase=$phase currentStep=$currentStep " +
                "${TutorialAbacusResetDiagnostics.stepFlags(step)} " +
                "text=${TutorialAbacusResetDiagnostics.stepLabel(step)}",
        )
    }

    private fun logTutorialAbacusResetShowStep(position: Int, phase: String) {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        val value = abacusController?.getCurrentValue()
        TutorialAbacusResetDiagnostics.log(
            "SHOW_STEP $phase pos=$position currentStep=$currentStep " +
                "controllerValue=$value backOrFront=$backOrFront",
        )
    }

    /**
     * Kısmi cevap zincirinde ileri geçiş (ör. 30 → 32): margin aynı kalsın, [updateAllAppearance]
     * mevcut seçili drawable'ları ezmesin. Terk edilen adımın [answerNumber] == yeni adımın
     * [backAnswerNumber] ve abaküs değeri uyumlu olmalı.
     */
    private fun shouldPreserveBeadAppearanceOnSetup(): Boolean {
        if (!backOrFront || currentStep <= 0) return false
        val landed = getCurrentStep()
        val backNum = landed.backAnswerNumber ?: return false
        val departed = getPlusIndexCurrentStep(-1)
        val departedAnswer = departed.answerNumber ?: return false
        if (departedAnswer != backNum) return false
        val controllerValue = abacusController?.getCurrentValue()
        if (controllerValue != null && controllerValue != backNum) return false
        return true
    }

    /**
     * Ara adımda [answerNumber] yok → [shouldPreserveBeadAppearanceOnSetup] false kalır; iç state
     * [backAnswerNumber]'da olsa bile [syncStateFromUi] margin'i 0 okuyup [updateAllAppearance]
     * ile seçili renkleri silebilir. Bu durumda sayıdan kur + görünümü güncelle.
     */
    private fun shouldApplyBackAnswerFromControllerOnSetup(): Boolean {
        if (!backOrFront || currentStep <= 0) return false
        val backNum = getCurrentStep().backAnswerNumber ?: return false
        val controllerValue = abacusController?.getCurrentValue() ?: return false
        return controllerValue == backNum
    }

    private fun logTutorialPreserveBeadAppearance(preserve: Boolean) {
        if (!TutorialAbacusResetDiagnostics.ENABLED) return
        val landed = getCurrentStep()
        val departed = getPlusIndexCurrentStep(-1)
        TutorialAbacusResetDiagnostics.log(
            "PRESERVE_BEAD_APPEARANCE preserve=$preserve currentStep=$currentStep " +
                "departedAnswer=${departed.answerNumber} landedBack=${landed.backAnswerNumber} " +
                "controllerValue=${abacusController?.getCurrentValue()} " +
                "syncPath=${if (preserve) "applyInternalStateFromValue" else "syncStateFromUi"}",
        )
    }

    private fun logTutorialBackPress(phase: String, extra: String = "") {
        if (!TutorialBeadDiagnostics.ENABLED) return
        val step = getCurrentStep()
        val nextStep = currentTutorialSteps.getOrNull(currentStep + 1)
        val beadAnims = currentAnimations.count { it.isAnimating() }
        TutorialBeadDiagnostics.log(
            "[BACK $phase] currentStep=$currentStep backOrFront=$backOrFront " +
                "pendingBack=$backStepTransitionPending pendingRunnable=${pendingBackShowStepRunnable != null} " +
                "isAnyAnim=${isAnyAnimationRunning()} beadAnims=$beadAnims " +
                "controllerBeadAnim=${abacusController?.isBeadAnimationInProgress() == true} " +
                "resetInProgress=${abacusController?.isResetInProgress() == true} " +
                "step=${step.text.take(28)} resetAndWaithNext=${nextStep?.resetAndWaith} " +
                "skipReverse=${step.backAnswerNumber != null} $extra",
        )
    }

    private fun cancelPendingBackShowStep() {
        // View yok edilmiş olabilir (onDestroyView'dan da çağrılıyor); binding'e güvenle eriş.
        val b = _binding
        pendingBackShowStepRunnable?.let { b?.root?.removeCallbacks(it) }
        pendingBackShowStepRunnable = null
        backStepTransitionPending = false
        b?.overlay?.let { overlay ->
            overlay.visibility = View.GONE
            overlay.isClickable = false
            overlay.isFocusable = false
        }
    }

    private fun scheduleBackShowStep(delayMs: Long, skipReverseBeadAnims: Boolean) {
        cancelPendingBackShowStep()
        backStepTransitionPending = delayMs > 0
        if (delayMs > 0) {
            binding.overlay.visibility = View.VISIBLE
            binding.overlay.alpha = 0.01f
            binding.overlay.isClickable = true
            binding.overlay.isFocusable = true
        }
        val runnable = Runnable {
            if (!isAdded) return@Runnable
            pendingBackShowStepRunnable = null
            backStepTransitionPending = false
            binding.overlay.visibility = View.GONE
            binding.overlay.isClickable = false
            binding.overlay.isFocusable = false
            logTutorialBackPress("delayedShowStep firing")
            showStep(currentStep, skipAnimations = skipReverseBeadAnims)
            hideDevamButtonIfVisible()
        }
        pendingBackShowStepRunnable = runnable
        logTutorialBackPress("scheduleBackShowStep", "delayMs=$delayMs skipReverse=$skipReverseBeadAnims")
        binding.root.postDelayed(runnable, delayMs)
    }
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /** Bir basamak (sütun) için sağ margin adımı (px). Onlar=1, Yüzler=2, Binler=3, Onbinler=4. */
    private fun focusMarginRightPx(steps: Int): Int =
        dpToPx(19) + (if (steps <= 0) 0 else resources.getDimensionPixelSize(R.dimen.tutorial_focus_margin_right_step) * steps)

    private fun focusDimenPx(resId: Int): Int = resources.getDimensionPixelSize(resId)

    /** writeAnswerNumber / animateBeads* için: controller ile aynı ölçülmüş adım (yoksa dimen). */
    private fun tutorialBottomMoveDistancePx(): Float {
        abacusController?.computeMovementDistancesFromLayout(ratio = 1f, force = true)
        return abacusController?.getBottomMoveDistancePx()
            ?: AbacusBeadMetrics.bottomStepPx(requireContext())
    }

    private fun tutorialTopMoveDistancePx(): Float {
        abacusController?.computeMovementDistancesFromLayout(ratio = 1f, force = true)
        return abacusController?.getTopMoveDistancePx()
            ?: AbacusBeadMetrics.topStepPx(requireContext())
    }

    private fun createTutorialSteps(){
        createTutorialSteps1()
        createTutorialSteps100()
        createTutorialSteps101()
        createTutorialSteps2()
        createTutorialSteps102()
        createTutorialSteps103()
        createTutorialSteps3()
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
        createTutorialSteps1100()
        createTutorialSteps12()
        createTutorialSteps13()
        createTutorialSteps14()
        createTutorialSteps999()
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
    
    // 1–5 ve 100–103. öğreticiler Ekim 2026'ya kadar Remote Config'ten JSON olarak geliyordu
    // (tutorial_step_<no>). Sesler uygulamanın içinde olduğu için her içerik değişikliği zaten
    // sürüm gerektiriyordu; Splash ise fetch için 8 sn bekliyordu. Diğerleri gibi Kotlin'e
    // çevrildi. Konsoldaki parametreler eski sürümler kullanımdan kalkana kadar duruyor.

    private fun createTutorialSteps1(){
        tutorialSteps = listOf(
            TutorialStep(
                getString(R.string.tutorial1_1),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(binding.abacusLinear, View.INVISIBLE) },
                    { WidgetOperation.ChangeVisibility(binding.abacusContainer, View.INVISIBLE) },
                    { WidgetOperation.ChangeVisibility(binding.backButton, View.INVISIBLE) },
                    {
                        // İlk açılış akışında "eğitimi atla" hiç gösterilmez.
                        WidgetOperation.ChangeVisibility(binding.skipTutorialButton, if (isFirstRunTutorial) View.INVISIBLE else View.VISIBLE)
                    }
                ),
                soundResource = R.raw.tutorial1_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_2),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(binding.backButton, View.VISIBLE) }
                ),
                soundResource = R.raw.tutorial1_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_4),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(binding.abacusLinear, View.VISIBLE) },
                    { WidgetOperation.ChangeVisibility(binding.abacusContainer, View.VISIBLE) }
                ),
                soundResource = R.raw.tutorial1_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_5),
                soundResource = R.raw.tutorial1_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_6),
                soundResource = R.raw.tutorial1_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_7),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(0),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }
                ),
                soundResource = R.raw.tutorial1_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_8),
                widgetOperations = listOf(
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }
                ),
                soundResource = R.raw.tutorial1_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_9),
                widgetOperations = listOf(
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(2),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }
                ),
                soundResource = R.raw.tutorial1_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_10),
                widgetOperations = listOf(
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(3),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }
                ),
                soundResource = R.raw.tutorial1_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_11),
                widgetOperations = listOf(
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(4),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }
                ),
                soundResource = R.raw.tutorial1_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_12),
                widgetOperations = listOf(
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = 1,
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = focusDimenPx(R.dimen.tutorial_focus_column_highlight_width),
                            fromHeight = focusView.height,
                            toHeight = focusDimenPx(R.dimen.tutorial_focus_column_highlight_height),
                            duration = 400
                        )
                    }
                ),
                onStep = { view -> sizeHistory.add(Pair(view.width, view.height)) },
                soundResource = R.raw.tutorial1_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_13),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeMargin(focusView, 1, 0, null) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = focusDimenPx(R.dimen.tutorial_focus_column_highlight_width),
                            fromHeight = focusView.height,
                            toHeight = focusDimenPx(R.dimen.tutorial_focus_top_bead_band_height),
                            duration = 400
                        )
                    }
                ),
                onStep = { view -> sizeHistory.add(Pair(view.width, view.height)) },
                soundResource = R.raw.tutorial1_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_14),
                widgetOperations = listOf(
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = 1,
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    },
                    { WidgetOperation.ChangeConstraints(view = focusView, topToTop = R.id.guideline2) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = focusDimenPx(R.dimen.tutorial_focus_column_highlight_width),
                            fromHeight = focusView.height,
                            toHeight = focusDimenPx(R.dimen.tutorial_focus_bottom_bead_band_height),
                            duration = 400
                        )
                    }
                ),
                onStep = { view -> sizeHistory.add(Pair(view.width, view.height)) },
                soundResource = R.raw.tutorial1_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_15),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)
                ),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }
                ),
                onStep = { view -> sizeHistory.add(Pair(view.width, view.height)) },
                soundResource = R.raw.tutorial1_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_100),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1)
                ),
                soundResource = R.raw.tutorial1_100,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_101),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1)
                ),
                soundResource = R.raw.tutorial1_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_102),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial1_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_103),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                soundResource = R.raw.tutorial1_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_104),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                soundResource = R.raw.tutorial1_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_17),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)
                ),
                soundResource = R.raw.tutorial1_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_105),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1)
                ),
                soundResource = R.raw.tutorial1_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_106),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1)
                ),
                soundResource = R.raw.tutorial1_106,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_107),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial1_107,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial1_109),
                id = "t1_1_asagidaki",
                questionText = "1",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 1,
                soundResource = R.raw.tutorial1_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_001),
            ),
            TutorialStep(
                getString(R.string.tutorial1_110),
                id = "t1_2_yaz",
                questionText = "2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 2,
                soundResource = R.raw.tutorial1_110,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_002),
            ),
            TutorialStep(
                getString(R.string.tutorial1_111),
                id = "t1_5_yaz",
                questionText = "5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 5,
                soundResource = R.raw.tutorial1_111,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_003),
            ),
            TutorialStep(
                getString(R.string.tutorial1_111),
                id = "t1_6_yaz",
                questionText = "6",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 6,
                soundResource = R.raw.tutorial1_111,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_004),
            ),
            TutorialStep(
                getString(R.string.tutorial1_113),
                id = "t1_8_yaz",
                questionText = "8",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 8,
                soundResource = R.raw.tutorial1_113,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_005),
            ),
            TutorialStep(
                getString(R.string.tutorial1_21),
                soundResource = R.raw.tutorial1_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }

    private fun createTutorialSteps100(){
        tutorialSteps100 = listOf(
            TutorialStep(
                getString(R.string.tutorial3_100),
                soundResource = R.raw.tutorial3_100,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_3),
                soundResource = R.raw.tutorial1_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_101),
                soundResource = R.raw.tutorial3_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_102),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                soundResource = R.raw.tutorial3_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_103),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1)
                ),
                soundResource = R.raw.tutorial3_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_104),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 1)
                ),
                soundResource = R.raw.tutorial3_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_105),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial3_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_106),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                soundResource = R.raw.tutorial3_106,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_107),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                soundResource = R.raw.tutorial3_107,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_108),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1)
                ),
                soundResource = R.raw.tutorial3_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_109),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 1)
                ),
                soundResource = R.raw.tutorial3_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_110),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                soundResource = R.raw.tutorial3_110,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_006),
                resetAndWaith = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3_111),
                id = "t100_70_yaz",
                questionText = "70",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 70,
                soundResource = R.raw.tutorial3_111,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_006),
            ),
            TutorialStep(
                getString(R.string.tutorial3_112),
                soundResource = R.raw.tutorial3_112,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3_113),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                questionText = "12",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_113,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_114),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)
                ),
                questionText = "12",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_114,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_115),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2)
                ),
                questionText = "24",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_115,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_116),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)
                ),
                questionText = "24",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_116,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_117),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                questionText = "24",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_117,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_118),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                questionText = "32",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_118,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3_119),
                id = "t100_32_onlar_30u",
                questionText = "32",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 30,
                soundResource = R.raw.tutorial3_119,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_007),
            ),
            TutorialStep(
                getString(R.string.tutorial3_120),
                id = "t100_32_birler_2yi",
                questionText = "32",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 32,
                soundResource = R.raw.tutorial3_120,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_008),
                backAnswerNumber = 30,
            ),
            TutorialStep(
                getString(R.string.tutorial3_121),
                questionText = "78",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_121,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_122),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                questionText = "78",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_122,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_123),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)
                ),
                questionText = "78",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_123,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_124),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)
                ),
                questionText = "94",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_124,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3_125),
                id = "t100_94_onlar",
                questionText = "94",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 90,
                soundResource = R.raw.tutorial3_125,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_009),
            ),
            TutorialStep(
                getString(R.string.tutorial3_126),
                id = "t100_94_birler_4u",
                questionText = "94",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 94,
                soundResource = R.raw.tutorial3_126,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_010),
                backAnswerNumber = 90,
            ),
            TutorialStep(
                getString(R.string.tutorial3_127),
                soundResource = R.raw.tutorial3_127,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }


    private fun createTutorialSteps101(){
        tutorialSteps101 = listOf(
            TutorialStep(
                getString(R.string.tutorial4_100),
                soundResource = R.raw.tutorial4_100,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_101),
                soundResource = R.raw.tutorial4_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_102),
                questionText = "154",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_103),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)
                ),
                questionText = "154",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_104),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                questionText = "154",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_105),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                questionText = "154",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_118),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                questionText = "687",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_118,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial4_107),
                id = "t101_687_yuzler_3",
                questionText = "687",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 600,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_107,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_011),
            ),
            TutorialStep(
                getString(R.string.tutorial4_108),
                id = "t101_687_onlar_2",
                questionText = "687",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 680,
                questionTextColorPositions = listOf(1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_012),
                backAnswerNumber = 600,
            ),
            TutorialStep(
                getString(R.string.tutorial4_109),
                id = "t101_687_birler",
                questionText = "687",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 687,
                questionTextColorPositions = listOf(2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_013),
                backAnswerNumber = 680,
            ),
            TutorialStep(
                getString(R.string.tutorial4_3000),
                questionText = "7319",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_3000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_3001),
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom2", 1),
                    BeadAnimation(this, "rod1_bead_top", 3)
                ),
                questionText = "7319",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_3001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_3002),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1)
                ),
                questionText = "7319",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_3002,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2008),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                questionText = "7319",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2008,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_105),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "7319",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(3 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_118),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_bottom2", 2),
                    BeadAnimation(this, "rod1_bead_top", 4)
                ),
                questionText = "4813",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_118,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial4_3003),
                id = "t101_4813_binler_4",
                questionText = "4813",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 4000,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_3003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_014),
            ),
            TutorialStep(
                getString(R.string.tutorial4_3004),
                id = "t101_4813_yuzler_3",
                questionText = "4813",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 4800,
                questionTextColorPositions = listOf(1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_3004,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_012),
                backAnswerNumber = 4000,
            ),
            TutorialStep(
                getString(R.string.tutorial4_108),
                id = "t101_4813_onlar_2",
                questionText = "4813",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 4810,
                questionTextColorPositions = listOf(2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_015),
                backAnswerNumber = 4800,
            ),
            TutorialStep(
                getString(R.string.tutorial4_109),
                id = "t101_4813_birler",
                questionText = "4813",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 4813,
                questionTextColorPositions = listOf(3 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_016),
                backAnswerNumber = 4810,
            ),
            TutorialStep(
                getString(R.string.tutorial101_0),
                id = "t101_soru_sayilari_hangi_basamaktan",
                soundResource = R.raw.tutorial101_0,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_017),
                options = listOf(
                    getString(R.string.tutorial_sik_001),
                    getString(R.string.tutorial_sik_002),
                    getString(R.string.tutorial_sik_003)
                ),
                correctOptionIndex = listOf(2),
                multipleChoice = false,
                optionText = getString(R.string.tutorial101_0),
            ),
            TutorialStep(
                getString(R.string.tutorial4_3000),
                questionText = "39125",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_3000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_3005),
                animation = listOf(
                    BeadAnimation(this, "rod0_bead_bottom1", 1),
                    BeadAnimation(this, "rod0_bead_bottom2", 1),
                    BeadAnimation(this, "rod0_bead_bottom3", 1)
                ),
                questionText = "39125",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_3005,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2006),
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom2", 1),
                    BeadAnimation(this, "rod1_bead_bottom3", 1),
                    BeadAnimation(this, "rod1_bead_bottom4", 1),
                    BeadAnimation(this, "rod1_bead_top", 3)
                ),
                questionText = "39125",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2006,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2007),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)
                ),
                questionText = "39125",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2007,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2008),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)
                ),
                questionText = "39125",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(3 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2008,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_105),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "39125",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_118),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod1_bead_bottom2", 2),
                    BeadAnimation(this, "rod1_bead_bottom3", 2),
                    BeadAnimation(this, "rod1_bead_bottom4", 2),
                    BeadAnimation(this, "rod1_bead_top", 4),
                    BeadAnimation(this, "rod0_bead_bottom1", 2),
                    BeadAnimation(this, "rod0_bead_bottom2", 2),
                    BeadAnimation(this, "rod0_bead_bottom3", 2)
                ),
                questionText = "84192",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_106,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2009),
                id = "t101_84192_on_binler_5",
                questionText = "84192",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 80000,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2009,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_012),
            ),
            TutorialStep(
                getString(R.string.tutorial4_3003),
                id = "t101_84192_binler_4",
                questionText = "84192",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 84000,
                questionTextColorPositions = listOf(1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_3003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_014),
                backAnswerNumber = 80000,
            ),
            TutorialStep(
                getString(R.string.tutorial4_107),
                id = "t101_84192_yuzler_3",
                questionText = "84192",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 84100,
                questionTextColorPositions = listOf(2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_107,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_015),
                backAnswerNumber = 84000,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2010),
                id = "t101_84192_onlar_2",
                questionText = "84192",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 84190,
                questionTextColorPositions = listOf(3 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2010,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_018),
                backAnswerNumber = 84100,
            ),
            TutorialStep(
                getString(R.string.tutorial4_109),
                id = "t101_84192_birler",
                questionText = "84192",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 84192,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_109,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_019),
                backAnswerNumber = 84190,
            ),
            TutorialStep(
                getString(R.string.tutorial4_110),
                soundResource = R.raw.tutorial4_110,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }


    private fun createTutorialSteps2(){
        tutorialSteps2 = listOf(
            TutorialStep(
                getString(R.string.tutorial2_1),
                soundResource = R.raw.tutorial2_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_3),
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_4),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)
                ),
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_5),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_6),
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial12_9),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)
                ),
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial12_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_101),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)
                ),
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_101,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_102),
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_102,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_102),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)
                ),
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_6),
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_7),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)
                ),
                questionText = "2 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial2_8),
                id = "t2_2p6_2_ete",
                questionText = "2 + 6",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 2,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_020),
            ),
            TutorialStep(
                getString(R.string.tutorial2_9),
                id = "t2_2p6_6yi_ekleyip_ete",
                questionText = "2 + 6",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 8,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_021),
                backAnswerNumber = 2,
            ),
            TutorialStep(
                getString(R.string.tutorial2_103),
                questionText = "1 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_103,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial2_104),
                id = "t2_1p8_1_ete",
                questionText = "1 + 8",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 1,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_104,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_022),
            ),
            TutorialStep(
                getString(R.string.tutorial2_105),
                id = "t2_1p8_8i_ekleyip_ete",
                questionText = "1 + 8",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 9,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_105,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_023),
                backAnswerNumber = 1,
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }


    private fun createTutorialSteps102(){
        tutorialSteps102 = listOf(
            TutorialStep(
                getString(R.string.tutorial2_11),
                soundResource = R.raw.tutorial2_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_12),
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_13),
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_13),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1)
                ),
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_14),
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_14),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 1)
                ),
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_14),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_15),
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                soundResource = R.raw.tutorial2_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_17),
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial2_18),
                id = "t102_16p21_16_ete",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 16,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_024),
            ),
            TutorialStep(
                getString(R.string.tutorial2_19),
                id = "t102_16p21_21i_eklemek_20yi",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 36,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_025),
                backAnswerNumber = 16,
            ),
            TutorialStep(
                getString(R.string.tutorial2_20),
                id = "t102_16p21_1i_birler",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 37,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial2_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_026),
                backAnswerNumber = 36,
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial10_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial102_0),
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial102_0,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial102_0),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial102_1),
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial102_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial102_1),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial102_1),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)
                ),
                questionText = "36 + 63",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial102_2),
                soundResource = R.raw.tutorial102_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
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
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_17),
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial102_3),
                id = "t102_23p71_ilk_sayi",
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 23,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial102_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_027),
            ),
            TutorialStep(
                getString(R.string.tutorial102_4),
                id = "t102_23p71_71i_eklemek_70i",
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 93,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial102_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_028),
                backAnswerNumber = 23,
            ),
            TutorialStep(
                getString(R.string.tutorial102_5),
                id = "t102_23p71_1i_birler",
                questionText = "23 + 71",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 94,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial102_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_026),
                backAnswerNumber = 93,
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }

    private fun createTutorialSteps103(){
        tutorialSteps103 = listOf(
            TutorialStep(
                getString(R.string.tutorial103_0),
                soundResource = R.raw.tutorial103_0,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_1),
                soundResource = R.raw.tutorial103_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial10_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_2),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_2),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1)
                ),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_3),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF"), 7 to Color.parseColor("#00BFFF"), 8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_4),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom3", 1)
                ),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_5),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_6),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_7),
                questionText = "241 + 153",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial103_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
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
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial103_8),
                id = "t103_222p126_222_ete",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 222,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_029),
            ),
            TutorialStep(
                getString(R.string.tutorial103_9),
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF"), 7 to Color.parseColor("#00BFFF"), 8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_025),
                backAnswerNumber = 222,
            ),
            TutorialStep(
                getString(R.string.tutorial103_4),
                id = "t103_222p126_100",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 322,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_030),
                backAnswerNumber = 222,
            ),
            TutorialStep(
                getString(R.string.tutorial103_10),
                id = "t103_222p126_20",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 342,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_025),
                backAnswerNumber = 322,
            ),
            TutorialStep(
                getString(R.string.tutorial103_11),
                id = "t103_222p126_6",
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 348,
                questionTextColorPositions = listOf(8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_031),
                backAnswerNumber = 342,
            ),
            TutorialStep(
                getString(R.string.tutorial103_348),
                questionText = "222 + 126",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial103_348,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial10_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_12),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_12),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)
                ),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_13),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF"), 7 to Color.parseColor("#00BFFF"), 8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_14),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1)
                ),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_15),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_11),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial103_17),
                questionText = "582 + 316",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial103_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
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
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial103_18),
                id = "t103_723p266_723_ete",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 723,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_032),
            ),
            TutorialStep(
                getString(R.string.tutorial103_19),
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF"), 7 to Color.parseColor("#00BFFF"), 8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_025),
                backAnswerNumber = 723,
            ),
            TutorialStep(
                getString(R.string.tutorial103_20),
                id = "t103_723p266_200",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 923,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_033),
                backAnswerNumber = 723,
            ),
            TutorialStep(
                getString(R.string.tutorial103_21),
                id = "t103_723p266_60",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 983,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_034),
                backAnswerNumber = 923,
            ),
            TutorialStep(
                getString(R.string.tutorial103_11),
                id = "t103_723p266_6",
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 989,
                questionTextColorPositions = listOf(8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial103_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_031),
                backAnswerNumber = 983,
            ),
            TutorialStep(
                getString(R.string.tutorial103_22),
                questionText = "723 + 266",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial103_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_026),
                backAnswerNumber = 989,
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }


    private fun createTutorialSteps3(){
        tutorialSteps3 = listOf(
            TutorialStep(
                getString(R.string.tutorial3_1),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3000_1),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3000_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3000_2),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3000_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3000_3),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3000_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3000_4),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial3000_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_2),
                soundResource = R.raw.tutorial3_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_3),
                soundResource = R.raw.tutorial3_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_4),
                soundResource = R.raw.tutorial3_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_5),
                soundResource = R.raw.tutorial3_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_6),
                soundResource = R.raw.tutorial3_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3000_5),
                soundResource = R.raw.tutorial3000_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_7),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_8),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_9),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_10),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_11),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_3),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_14),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_14),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_15),
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_17),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_17),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)
                ),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_18),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_19),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_20),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_22),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_22),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_23),
                soundResource = R.raw.tutorial3_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial2_17),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)
                ),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_25),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)
                ),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_26),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_19),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_28),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)
                ),
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_23),
                soundResource = R.raw.tutorial3_31,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_23),
                id = "t3_soru_cevap_6",
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_035),
                options = listOf(
                    getString(R.string.tutorial_ipucu_035),
                    getString(R.string.tutorial_sik_004),
                    getString(R.string.tutorial_sik_005)
                ),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_001),
            ),
            TutorialStep(
                getString(R.string.tutorial3_32),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)
                ),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_33),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)
                ),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_34),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_34,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_19),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_6),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_37),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_37),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2)
                ),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_23),
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_38,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_23),
                id = "t3_soru_cevap_6_2",
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_036),
                options = listOf(
                    getString(R.string.tutorial_sik_006),
                    getString(R.string.tutorial_sik_007),
                    getString(R.string.tutorial_sik_008)
                ),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_002),
            ),
            TutorialStep(
                getString(R.string.tutorial3_39),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)
                ),
                soundResource = R.raw.tutorial3_39,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_40),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_41),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)
                ),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_41,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_42),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF"), 6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_19),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF"), 6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_43,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_6),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF"), 6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF"), 6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF"), 6 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_37),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF"), 6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_37),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 2)
                ),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF"), 6 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_44),
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_44,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)
                ),
                soundResource = R.raw.tutorial3_45,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3_46),
                id = "t3_4p4_ilk_sayi",
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 4,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_46,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_037),
            ),
            TutorialStep(
                getString(R.string.tutorial3_47),
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_47,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_48),
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_48,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_6),
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 4,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                id = "t3_4p4_5_gelir",
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 9,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_038),
                backAnswerNumber = 4,
            ),
            TutorialStep(
                getString(R.string.tutorial3_37),
                id = "t3_4p4_kardesi_1_gider",
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 8,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_039),
                backAnswerNumber = 9,
            ),
            TutorialStep(
                getString(R.string.tutorial3_49),
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_49,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                backAnswerNumber = 8,
            ),
            TutorialStep(
                getString(R.string.tutorial3000_6),
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3000_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3_46),
                id = "t3_3p2_ilk_sayi",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 3,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_46,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_040),
            ),
            TutorialStep(
                getString(R.string.tutorial3000_7),
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3000_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_48),
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_48,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_20),
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 3,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                id = "t3_3p2_5_gelir",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 8,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_038),
                backAnswerNumber = 3,
            ),
            TutorialStep(
                getString(R.string.tutorial3_22),
                id = "t3_3p2_kardesi_3_gider",
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 5,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_041),
                backAnswerNumber = 8,
            ),
            TutorialStep(
                getString(R.string.tutorial3_15),
                questionText = "3 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_50),
                soundResource = R.raw.tutorial3_50,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }
    

    private fun createTutorialSteps4(){
        tutorialSteps4 = listOf(
            TutorialStep(
                getString(R.string.tutorial4_1000),
                soundResource = R.raw.tutorial4_1000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_3),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)
                ),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_1001),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_1001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_1010),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_1010,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3)
                ),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_1002),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_1002,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_1002),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2)
                ),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_1003),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_1003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2000),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_37),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial3_37),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2001),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2001,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2001),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(8 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2002),
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_2002,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2002),
                id = "t4_soru_cevap_589",
                questionText = "243 + 346",
                questionTextVisibility = View.VISIBLE,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_042),
                options = listOf(
                    getString(R.string.tutorial_sik_009),
                    getString(R.string.tutorial_sik_010)
                ),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_003),
            ),
            TutorialStep(
                getString(R.string.tutorial1_108),
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
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                soundResource = R.raw.tutorial3_45,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3_46),
                id = "t4_423p322_ilk_sayi",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 423,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF"), 2 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial3_46,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_043),
            ),
            TutorialStep(
                getString(R.string.tutorial4_1005),
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_1005,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 423,
            ),
            TutorialStep(
                getString(R.string.tutorial4_1006),
                id = "t4_423p322_300u_ekleyip",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 723,
                questionTextColorPositions = listOf(6 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_1006,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_044),
                backAnswerNumber = 423,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2003),
                id = "t4_423p322_20yi_ekleyip",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 743,
                questionTextColorPositions = listOf(7 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2003,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_025),
                backAnswerNumber = 723,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2004),
                id = "t4_423p322_2yi_ekleyip",
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 745,
                questionTextColorPositions = listOf(8 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial4_2004,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_045),
                backAnswerNumber = 743,
            ),
            TutorialStep(
                getString(R.string.tutorial4_2005),
                questionText = "423 + 322",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial4_2005,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_039),
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }
    

    private fun createTutorialSteps5(){
        tutorialSteps5 = listOf(
            TutorialStep(
                getString(R.string.tutorial5_1),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_2),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_3),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_4),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(0),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    },
                    { WidgetOperation.ChangeConstraints(view = focusView, topToTop = R.id.guideline2) },
                    { WidgetOperation.ChangeMargin(focusView, focusMarginRightPx(0), 0, dpToPx(-10)) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    }
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_5),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeMargin(focusView, 0, 0, null) },
                    { WidgetOperation.ChangeConstraints(view = focusView, topToTop = R.id.guideline) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_6),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_7),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5000_2),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }
                ),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5000_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_8),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_9),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial5_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_10),
                soundResource = R.raw.tutorial5_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_11),
                soundResource = R.raw.tutorial5_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_12),
                soundResource = R.raw.tutorial5_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_13),
                soundResource = R.raw.tutorial5_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_14),
                soundResource = R.raw.tutorial5_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_15),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_16),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_17),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = 0,
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    },
                    { WidgetOperation.ChangeConstraints(view = focusView, topToTop = R.id.guideline2) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    },
                    { WidgetOperation.ChangeMargin(focusView, 0, 0, dpToPx(-10)) }
                ),
                onStep = { view -> sizeHistory.add(Pair(view.width, view.height)) },
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_18),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeMargin(focusView, 0, 0, null) },
                    { WidgetOperation.ChangeConstraints(view = focusView, topToTop = R.id.guideline) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_19),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_21),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_21),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_22),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5000_1),
                questionText = "9 + 1",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5000_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_23),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)
                ),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_24),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_25),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_26),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_27),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_27),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_22),
                questionText = "8 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)
                ),
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_29),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_30),
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_30),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_31),
                questionText = "9 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_31,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)
                ),
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_33),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_34),
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_34,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_34),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_35),
                questionText = "7 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)
                ),
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_36),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)
                ),
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)
                ),
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_37),
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_37),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4)
                ),
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(4 to Color.parseColor("#00BFFF")),
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_38),
                questionText = "6 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_38,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial5_46),
                id = "t5_soru_10luk_kurali_hangi",
                soundResource = R.raw.tutorial5_46,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_046),
                options = listOf(
                    getString(R.string.tutorial_sik_011),
                    getString(R.string.tutorial_sik_012),
                    getString(R.string.tutorial_sik_013)
                ),
                correctOptionIndex = listOf(0,1),
                multipleChoice = true,
                optionText = getString(R.string.tutorial5_46),
            ),
            TutorialStep(
                getString(R.string.tutorial5_39),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)
                ),
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_39,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial5_40),
                id = "t5_18p4_ilk_sayi",
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 18,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_40,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_047),
            ),
            TutorialStep(
                getString(R.string.tutorial5_41),
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_41,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 18,
            ),
            TutorialStep(
                getString(R.string.tutorial5_42),
                id = "t5_18p4_yaz",
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 22,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_048),
                backAnswerNumber = 18,
            ),
            TutorialStep(
                getString(R.string.tutorial5_43),
                questionText = "18 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_43,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial3000_6),
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3000_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
            ),
            TutorialStep(
                getString(R.string.tutorial_txt_001),
                id = "t5_17p3_ilk_sayi",
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 17,
                questionTextColorPositions = listOf(0 to Color.parseColor("#00BFFF"), 1 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial9_1000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_049),
            ),
            TutorialStep(
                getString(R.string.tutorial5000_3),
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5000_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 17,
            ),
            TutorialStep(
                getString(R.string.tutorial5_42),
                id = "t5_17p3_yaz",
                questionText = "17 + 3",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 20,
                questionTextColorPositions = listOf(5 to Color.parseColor("#00BFFF")),
                soundResource = R.raw.tutorial5_42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_050),
                backAnswerNumber = 17,
            ),
            TutorialStep(
                getString(R.string.tutorial5_45),
                id = "t5_soru_10luk_kurali_nasil",
                soundResource = R.raw.tutorial5_45,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_051),
                options = listOf(
                    getString(R.string.tutorial_ipucu_051),
                    getString(R.string.tutorial_sik_014),
                    getString(R.string.tutorial_sik_015)
                ),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = getString(R.string.tutorial5_45),
            ),
            TutorialStep(
                getString(R.string.tutorial5_44),
                soundResource = R.raw.tutorial5_44,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            )
        )
    }
    private fun createTutorialSteps6(){
        tutorialSteps6 = listOf(
            TutorialStep(
                getString(R.string.tutorial6_1),
                soundResource = R.raw.tutorial6_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_2),
                questionText = "49 + 4",
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
                getString(R.string.tutorial6000_0),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_0,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6000_1),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_1,
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
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    })

            ),TutorialStep(
                getString(R.string.tutorial6000_2),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_2,
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
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }),
            ),TutorialStep(
                getString(R.string.tutorial6000_3),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf { WidgetOperation.ChangeVisibility(focusView, View.GONE) }
            ),TutorialStep(
                getString(R.string.tutorial6000_4),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_001)
            ),TutorialStep(
                getString(R.string.tutorial6000_5),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial6000_6),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2,
                        )
                    },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(1),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial6000_7),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
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
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial6000_8),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial6_9), //sese dokunma
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf {
                    WidgetOperation.ChangeVisibility(focusView, View.GONE)},
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial3_13), //sese dokunma
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial3_14), //sese dokunma
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial6000_9),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_003)),TutorialStep(
                getString(R.string.tutorial6000_9),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom4", 2)
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_003)),TutorialStep(
                getString(R.string.tutorial6000_10),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_13), //sese dokunma
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                ),
                soundResource = R.raw.tutorial6_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_14), //sese dokunma
                questionText = "49 + 4",
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
                getString(R.string.tutorial6000_11),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6000_12),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial5_20), //sese dokunma
                questionText = "49 + 4",
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
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_002)),TutorialStep(
                getString(R.string.tutorial6000_13),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_003)),TutorialStep(
                getString(R.string.tutorial6000_13),
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_003)),TutorialStep(
                getString(R.string.tutorial6000_14), //yukardaki sesi ekle.
                questionText = "49 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_001)
            ),TutorialStep(
                getString(R.string.tutorial1_108),
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                getString(R.string.tutorial5_40), id = "t6_48p2_ilk_sayi", //Ses değişecek
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 48,
                abacusClickable = true,
                soundResource = R.raw.tutorial6000_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_052)

            ),TutorialStep(
                getString(R.string.tutorial6000_16), id = "t6_48p2_2yi_10luk_kural", //Ses değişecek
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_16,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 48,
                answerNumber = 58,
                requestText = getString(R.string.tutorial_ipucu_053),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_004)),TutorialStep(
                getString(R.string.tutorial6000_17), id = "t6_48p2_2nin_buyuk_kardesi", //Ses değişecek
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 50,
                abacusClickable = true,
                soundResource = R.raw.tutorial6000_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 58,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_054),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_005)),

            TutorialStep(
                getString(R.string.tutorial6_20),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_20,
                useTypewriterEffect = true,
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
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6000_18), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6000_1),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_1,
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
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    })

            ),TutorialStep(
                getString(R.string.tutorial_txt_002),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_2,
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
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }),
            ),TutorialStep(
                getString(R.string.tutorial6000_3),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf { WidgetOperation.ChangeVisibility(focusView, View.GONE) }
            ),TutorialStep(
                getString(R.string.tutorial6000_19), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_006)
            ),TutorialStep(
                getString(R.string.tutorial6000_5),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial6000_6),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2,
                        )
                    },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(1),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial6000_7),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
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
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial6000_20), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial6000_21), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf {
                    WidgetOperation.ChangeVisibility(focusView, View.GONE)},
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial6000_22), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial6000_23), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_008)),TutorialStep(
                getString(R.string.tutorial6000_23),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_008)),TutorialStep(
                getString(R.string.tutorial6000_24), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_13),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                soundResource = R.raw.tutorial6_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_31),
                questionText = "98 + 3",
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
                getString(R.string.tutorial6000_25), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_25,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6000_26), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "98 + 3",
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
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_007)),TutorialStep(
                getString(R.string.tutorial6000_27), //ses değişecek.
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_008)),TutorialStep(
                getString(R.string.tutorial6000_27),
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_008)),TutorialStep(
                getString(R.string.tutorial6000_28), //ses değişecek
                questionText = "98 + 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1_108),
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true
            ),TutorialStep(
                getString(R.string.tutorial5_40),
                id = "t6_96p5_ilk_sayi",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 96,
                abacusClickable = true,
                soundResource = R.raw.tutorial6000_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_055)
            ),TutorialStep(
                getString(R.string.tutorial6000_29), id = "t6_96p5_5i_10luk_kuralla", //ses değişecek
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_29,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 96,
                answerNumber = 106,
                requestText = getString(R.string.tutorial_ipucu_056),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_009)),TutorialStep(
                getString(R.string.tutorial6000_30),
                id = "t6_96p5_5in_buyuk_kardesi",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 101,
                abacusClickable = true,
                soundResource = R.raw.tutorial6000_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 106,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_057),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_010)),TutorialStep(
                getString(R.string.tutorial3_50),
                soundResource = R.raw.tutorial6_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE
            )
        )
    }

    //Bu tutorial kullanılmıyor.
    private fun createTutorialSteps104(){
        tutorialSteps104 = listOf(
            //846 + 345
            TutorialStep(
                getString(R.string.tutorial_txt_003),
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
                getString(R.string.tutorial2_101),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial_txt_004),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_4),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_5),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_6),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = focusMarginRightPx(1),
                        toMarginRight = focusMarginRightPx(1),
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
                getString(R.string.tutorial6_7),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_8),
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
                getString(R.string.tutorial6_9),
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
                getString(R.string.tutorial6_10),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L)
            ,TutorialStep(
                getString(R.string.tutorial6_11),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial6_11),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                getString(R.string.tutorial6_12),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_13),
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
                getString(R.string.tutorial6_14),
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
                getString(R.string.tutorial6_15),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_16),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial6_18),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_18),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                )
            ),TutorialStep(
                getString(R.string.tutorial6_19),
                questionText = "49 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_19,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1_108),
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
                getString(R.string.tutorial_txt_001),
                id = "t104_48p2_ilk_sayi",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 48,
                abacusClickable = true,
                soundResource = R.raw.tutorial9_1000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_052)

            ),TutorialStep(
                getString(R.string.tutorial6000_16),
                id = "t104_48p2_2yi_10luk_kural",
                questionText = "48 + 2",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1000,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 48,
                answerNumber = 58,
                requestText = getString(R.string.tutorial_ipucu_053)

            ),TutorialStep(
                getString(R.string.tutorial5_1001),
                id = "t104_48p2_2nin_buyuk_kardesini",
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
                requestText = getString(R.string.tutorial_ipucu_054)
            ),TutorialStep(
                getString(R.string.tutorial6_20),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_21),
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
                getString(R.string.tutorial6_22),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_23),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_24),
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
                        fromMarginRight = focusMarginRightPx(1),
                        toMarginRight = focusMarginRightPx(1),
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
                getString(R.string.tutorial6_25),
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
                getString(R.string.tutorial5_20),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                soundResource = R.raw.tutorial6_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_27),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_27),
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
                getString(R.string.tutorial6_28),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_28),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                getString(R.string.tutorial6_29),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_13),
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
                getString(R.string.tutorial6_31),
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
                getString(R.string.tutorial6_32),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_32,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_23),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_33,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial6_35),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_35,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_35),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                getString(R.string.tutorial6_36),
                questionText = "98 + 4",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_36,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1_108),
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
                getString(R.string.tutorial_txt_001),
                id = "t104_96p5_ilk_sayi",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 96,
                abacusClickable = true,
                soundResource = R.raw.tutorial9_1000,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_055)

            ),TutorialStep(
                getString(R.string.tutorial5_1002),
                id = "t104_96p5_5i_10luk_kural",
                questionText = "96 + 5",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_1002,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 96,
                answerNumber = 106,
                requestText = getString(R.string.tutorial_ipucu_056)

            ),TutorialStep(
                getString(R.string.tutorial5_1003),
                id = "t104_96p5_5in_buyuk_kardesini",
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
                requestText = getString(R.string.tutorial_ipucu_057)
            ),TutorialStep(
                getString(R.string.tutorial3_50),
                soundResource = R.raw.tutorial6_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE
            ),
        )

    }

    private fun createTutorialSteps7(){
        tutorialSteps7 = listOf(
            TutorialStep(
                getString(R.string.tutorial7_1),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial7_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_2),
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial7_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_3),
                soundResource = R.raw.tutorial7_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
                ),
            TutorialStep(
                getString(R.string.tutorial7_4),
                soundResource = R.raw.tutorial7_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_5),
                soundResource = R.raw.tutorial7_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_6),
                soundResource = R.raw.tutorial7_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),TutorialStep(
                getString(R.string.tutorial7_7),
                soundResource = R.raw.tutorial7_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_8),
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                soundResource = R.raw.tutorial7_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_8),
                questionText = "1 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1))

            ),TutorialStep(
                getString(R.string.tutorial7_9),
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
                getString(R.string.tutorial5_20),
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

                ),
            TutorialStep(
                getString(R.string.tutorial7_11),
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_11),
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))

                ),
            TutorialStep(
                getString(R.string.tutorial5_35),
                questionText = "4 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_12,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_13),
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
                getString(R.string.tutorial7_14),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_14,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial7_15),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

                ),
            TutorialStep(
                getString(R.string.tutorial7_17),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

                ),
            TutorialStep(
                getString(R.string.tutorial7_17),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))
                ),
            TutorialStep(
                getString(R.string.tutorial5_22),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_18,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial7_20),
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L


            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),

            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2))

                ),
            TutorialStep(
                getString(R.string.tutorial5_35),
                questionText = "3 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_23,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial7_30),
                id = "t7_soru_6_7_8",
                soundResource = R.raw.tutorial7_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                options = listOf(getString(R.string.tutorial_sik_016),getString(R.string.tutorial_sik_017),getString(R.string.tutorial_sik_018)),
                correctOptionIndex = listOf(2),
                multipleChoice = false,
                optionText = getString(R.string.tutorial7_30),
                requestText = getString(R.string.tutorial_ipucu_058)
            ),
            TutorialStep(
                getString(R.string.tutorial7_24),
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
                getString(R.string.tutorial5_40),
                id = "t7_4p9_ilk_sayi",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 4,
                soundResource = R.raw.tutorial6000_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_037)
            ),
            TutorialStep(
                getString(R.string.tutorial7_26),
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_26,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial7_27),
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 4,

            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
                id = "t7_4p9_10_gelir",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 14,
                soundResource = R.raw.tutorial7_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 4,
                requestText = getString(R.string.tutorial_ipucu_059)

            ),
            TutorialStep(
                getString(R.string.tutorial7_29),
                id = "t7_4p9_9un_kardesi_1",
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 13,
                soundResource = R.raw.tutorial7_29,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 14,
                requestText = getString(R.string.tutorial_ipucu_039)

            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                soundResource = R.raw.tutorial15_27,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            )
        )
    }

    private fun createTutorialSteps105(){
        tutorialSteps105 = listOf(
            TutorialStep(
                getString(R.string.tutorial6_1),
                soundResource = R.raw.tutorial6_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.VISIBLE
            ),TutorialStep(
                getString(R.string.tutorial_txt_005),
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
                getString(R.string.tutorial105_0),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_0,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial105_1),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial105_2),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial6_6),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(

                    {WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)},
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = focusMarginRightPx(1),
                        toMarginRight = focusMarginRightPx(1),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                    }

                ),
                soundResource = R.raw.tutorial6_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial6_7),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial6_8),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.AnimateSize(
                        view = focusView,
                        fromWidth = focusView.width,
                        toWidth = dpToPx(55),
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
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial6_9),
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
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial6_10),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout)
            ,TutorialStep(
                getString(R.string.tutorial105_3),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_012),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial105_3),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_012),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial105_7),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_012),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial6_13),
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
                getString(R.string.tutorial105_50),
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
                getString(R.string.tutorial_txt_006),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial105_5),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial5_20),
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
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_011),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial105_6),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_012),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial105_6),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_012),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),TutorialStep(
                getString(R.string.tutorial105_7),
                questionText = "48 + 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial105_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_012),
                rulesStepTextTopToBottomOf = R.id.tenRuleTableLinearLayout),
            //49 + 6

            TutorialStep(
                getString(R.string.tutorial1_108),
                questionText = "49 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_top", 4),
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),TutorialStep(
                getString(R.string.tutorial5_40),
                id = "t105_49p6_ilk_sayi",
                questionText = "49 + 6",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 49,
                abacusClickable = true,
                soundResource = R.raw.tutorial6000_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_060)

            ),TutorialStep(
                getString(R.string.tutorial1050_0), id = "t105_49p6_6yi_10luk_kural", //Ses değişecek
                questionText = "49 + 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_0,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 49,
                answerNumber = 59,
                requestText = getString(R.string.tutorial_ipucu_053),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_013)),TutorialStep(
                getString(R.string.tutorial1050_1), id = "t105_49p6_6nin_kardesi_4", //Ses değişecek
                questionText = "49 + 6",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 55,
                abacusClickable = true,
                soundResource = R.raw.tutorial1050_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 59,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_061),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_014)),

            TutorialStep(
                getString(R.string.tutorial6_20),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6_20,
                useTypewriterEffect = true,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1050_2), //ses değişecek
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1050_11),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_11,
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
                            topToTop = R.id.guideline,
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    })

            ),TutorialStep(
                getString(R.string.tutorial6000_3),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf { WidgetOperation.ChangeVisibility(focusView, View.GONE) }
            ),TutorialStep(
                getString(R.string.tutorial1050_3), //ses değişecek
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_015)
            ),TutorialStep(
                getString(R.string.tutorial6000_5),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial6000_6),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2,
                        )
                    },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(1),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial6000_7),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
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
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(100),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial6000_20),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial6000_21),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_21,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetOperations = listOf {
                    WidgetOperation.ChangeVisibility(focusView, View.GONE)},
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                },
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_20,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial6000_22),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial6000_22,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial1050_4), //ses değişecek
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_017)),TutorialStep(
                getString(R.string.tutorial1050_4),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_017)),TutorialStep(
                getString(R.string.tutorial1050_5), //ses değişecek
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial6_13),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    ),
                soundResource = R.raw.tutorial6_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1050_6), //ses değişecek
                questionText = "99 + 9",
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
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),

                    ),
                soundResource = R.raw.tutorial1050_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial1050_7), //ses değişecek
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_7,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1050_3), //yukarıdaki sesi kullan
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "99 + 9",
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
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_016)),TutorialStep(
                getString(R.string.tutorial7_29), //Yukarıdaki sesi kullan
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_017)),TutorialStep(
                getString(R.string.tutorial7_29),
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_017)),TutorialStep(
                getString(R.string.tutorial1050_5), //Yukarıdakini kullan
                questionText = "99 + 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial1_108),
                questionText = "97 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                ),
                soundResource = R.raw.tutorial1_108,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                nextStepAbacusReset = true
            ),TutorialStep(
                getString(R.string.tutorial5_40),
                id = "t105_97p8_ilk_sayi",
                questionText = "97 + 8",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 97,
                abacusClickable = true,
                soundResource = R.raw.tutorial6000_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_062)
            ),TutorialStep(
                getString(R.string.tutorial1050_8), id = "t105_97p8_8i_10luk_kuralla", //ses değişecek
                questionText = "97 + 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial1050_8,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 97,
                answerNumber = 107,
                requestText = getString(R.string.tutorial_ipucu_056),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_018)),TutorialStep(
                getString(R.string.tutorial1050_9), id = "t105_97p8_8in_kardesi_2", //ses değişecek
                questionText = "97 + 8",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                answerNumber = 105,
                abacusClickable = true,
                soundResource = R.raw.tutorial1050_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 107,
                nextStepAbacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_063),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_019)),TutorialStep(
                getString(R.string.tutorial3_50),
                soundResource = R.raw.tutorial6_37,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                rulesPanelVisibility = View.INVISIBLE
            )


        )
    }
    
    private fun createTutorialSteps8(){
        tutorialSteps8 = listOf(
            TutorialStep(
                getString(R.string.tutorial8_1),
                rulesPanelVisibility = View.INVISIBLE,
                soundResource = R.raw.tutorial8_1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial8_2),
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_2,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial8_3),
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial8_4),
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial8_5),
                questionText = "1 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                soundResource = R.raw.tutorial8_5,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial8_5),
                questionText = "1 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))

            ),TutorialStep(
                getString(R.string.tutorial8_6),
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                soundResource = R.raw.tutorial8_6,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial8_7),
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
                getString(R.string.tutorial5_20),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                soundResource = R.raw.tutorial8_8,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),TutorialStep(
                getString(R.string.tutorial3_14),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_9,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial3_14),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))

            ),TutorialStep(
                getString(R.string.tutorial5_22),
                questionText = "4 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                soundResource = R.raw.tutorial8_10,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial8_11),
                id = "t8_soru_kurallari_uygulayamadigimiz",
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                soundResource = R.raw.tutorial8_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                options = listOf(getString(R.string.tutorial_sik_011),getString(R.string.tutorial_sik_013),getString(R.string.tutorial_sik_012)),
                correctOptionIndex = listOf(0,1),
                multipleChoice = true,
                optionText = getString(R.string.tutorial_soru_004),
                requestText = getString(R.string.tutorial_ipucu_064)
            ),TutorialStep(
                getString(R.string.tutorial8_12),
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
                getString(R.string.tutorial8_13),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                soundResource = R.raw.tutorial8_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),TutorialStep(
                getString(R.string.tutorial8_14),
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
                getString(R.string.tutorial8_16),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial3_14),
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
                getString(R.string.tutorial8_17),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial8_1000),
                questionText = "8 + 6",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1000
            ),TutorialStep(
                getString(R.string.tutorial8_18),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial3_22),
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
                getString(R.string.tutorial8_19),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial8_1001),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1001
            ),
            TutorialStep(
                getString(R.string.tutorial8_1010),
                id = "t8_soru_islemde_neden_kuralini",
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1010,
                options = listOf(getString(R.string.tutorial_sik_019),getString(R.string.tutorial_sik_020),getString(R.string.tutorial_sik_021)),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = getString(R.string.tutorial8_1010),
                requestText = getString(R.string.tutorial_ipucu_065)
            ),TutorialStep(
                getString(R.string.tutorial8_100),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial3_30),
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
                getString(R.string.tutorial8_20),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial8_1002),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1002
            ),TutorialStep(
                getString(R.string.tutorial8_21),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial3_37),
                questionText = "4 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_37,
            ),TutorialStep(
                getString(R.string.tutorial8_22),
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
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial8_1003),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_1003
            ),TutorialStep(
                getString(R.string.tutorial8_23),
                rulesPanelVisibility = View.GONE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_23

            ),TutorialStep(
                getString(R.string.tutorial8_24),
                rulesPanelVisibility = View.INVISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_24

            ),TutorialStep(
                getString(R.string.tutorial8_25),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_25

            ),TutorialStep(
                getString(R.string.tutorial8_26),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_26

            ),TutorialStep(
                getString(R.string.tutorial8_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_27
            ),TutorialStep(
                getString(R.string.tutorial8_28),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_28

            ),TutorialStep(
                getString(R.string.tutorial8_29),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_29

            ),TutorialStep(
                getString(R.string.tutorial8_30),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_30

            ),TutorialStep(
                getString(R.string.tutorial8_30),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1))

            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_31


            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))

            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_32

            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

            ),TutorialStep(
                getString(R.string.tutorial5_31),
                questionText = "6 + 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_33

            ),TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial8_35),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_35
            ),TutorialStep(
                getString(R.string.tutorial8_35),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1))
            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_36
            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_37
            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))
            ),TutorialStep(
                getString(R.string.tutorial5_31),
                questionText = "5 + 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_38
            ),TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial8_40),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_40

            ),TutorialStep(
                getString(R.string.tutorial8_40),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))

            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_41

            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))

            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_42

            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

            ),TutorialStep(
                getString(R.string.tutorial8_43),
                questionText = "6 + 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_43

            ),TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial8_45),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_45

            ),TutorialStep(
                getString(R.string.tutorial8_45),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))

            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_41

            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))

            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_46

            ),TutorialStep(
                getString(R.string.tutorial5_20),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1))

            ),TutorialStep(
                getString(R.string.tutorial8_43),
                questionText = "5 + 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_47

            ),TutorialStep(
                getString(R.string.tutorial8_48),
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
                getString(R.string.tutorial9_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1
            ),
            TutorialStep(
                getString(R.string.tutorial9_2),
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_2
            ),
            TutorialStep(
                getString(R.string.tutorial9_3),
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
                getString(R.string.tutorial9_4),
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
                getString(R.string.tutorial9_5),
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
                getString(R.string.tutorial9_6),
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
                getString(R.string.tutorial9_7),
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
                getString(R.string.tutorial9_8),
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
                getString(R.string.tutorial9_9),
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
                getString(R.string.tutorial9_9),
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
                getString(R.string.tutorial8_31),
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
                getString(R.string.tutorial8_31),
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4)),
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial5_20),
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
            ),
            TutorialStep(
                getString(R.string.tutorial9_12),
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
                getString(R.string.tutorial9_13),
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
                getString(R.string.tutorial9_14),
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
                getString(R.string.tutorial9_15),
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
                getString(R.string.tutorial9_8),
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
                getString(R.string.tutorial9_17),
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
                getString(R.string.tutorial9_17),
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
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
                getString(R.string.tutorial8_31),
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4)),
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
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
                getString(R.string.tutorial5_20),
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                )
            ),
            TutorialStep(
                getString(R.string.tutorial9_20),
                questionText = "68 + 76",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_20
            ),
            TutorialStep(
                getString(R.string.tutorial9_21),
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
                getString(R.string.tutorial9_1000),
                id = "t9_58p89_ilk_sayi",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 58,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_066)

            ),
            TutorialStep(
                getString(R.string.tutorial9_1001),
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1001,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                ),
                ),TutorialStep(
                getString(R.string.tutorial9_1020),
                id = "t9_soru_hangi_kural_ekleyeceksin",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1020,
                backAnswerNumber = 58,
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                ),
                options = listOf(getString(R.string.tutorial_sik_011),getString(R.string.tutorial_sik_022),getString(R.string.tutorial_sik_023)),
                correctOptionIndex = listOf(2),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_005),
                requestText = getString(R.string.tutorial_ipucu_067)
                ),
            TutorialStep(
                getString(R.string.tutorial9_1003),
                id = "t9_58p89_8i_kuralina_onlar",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 138,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1003,
                backAnswerNumber = 58,
                requestText = getString(R.string.tutorial_ipucu_068),
                questionTextColorPositions = listOf(
                    5 to Color.BLUE
                )
                ),
            TutorialStep(
                getString(R.string.tutorial9_27),
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
                getString(R.string.tutorial9_1004),
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
                getString(R.string.tutorial9_1020),
                id = "t9_soru_hangi_kural_ekleyeceksin_2",
                questionText = "58 + 89",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1020,
                backAnswerNumber = 138,
                questionTextColorPositions = listOf(
                    6 to Color.BLUE
                ),
                options = listOf(getString(R.string.tutorial_sik_011),getString(R.string.tutorial_sik_022),getString(R.string.tutorial_sik_023)),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_006),
                requestText = getString(R.string.tutorial_ipucu_069)
            ),

            TutorialStep(
                getString(R.string.tutorial9_1005),
                id = "t9_58p89_9u_onluk_kurala",
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
                backAnswerNumber = 138,
                requestText = getString(R.string.tutorial_ipucu_070)

            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
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
                getString(R.string.tutorial10_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_1
            ),
            TutorialStep(
                getString(R.string.tutorial2_17),
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_2
            ),
            TutorialStep(
                getString(R.string.tutorial10_3),
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_3,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial10_3),
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),
            TutorialStep(
                getString(R.string.tutorial10_4),
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextColorPositions = listOf(
                    4 to Color.parseColor("#00BFFF"),
                ),
                soundResource = R.raw.tutorial10_4

            ),
            TutorialStep(
                getString(R.string.tutorial10_4),
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    4 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                getString(R.string.tutorial10_5),
                questionText = "8 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_5
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
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
                getString(R.string.tutorial10_7),
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
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    0 to Color.parseColor("#00BFFF"),
                ),
                soundResource = R.raw.tutorial10_7
            ),
            TutorialStep(
                getString(R.string.tutorial10_8),
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                    6 to Color.parseColor("#00BFFF"),
                ),
                soundResource = R.raw.tutorial10_8
            ),
            TutorialStep(
                getString(R.string.tutorial10_9),
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_9,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial10_9),
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial10_10),
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_10,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial10_10),
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial10_11),
                questionText = "78 - 63",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_11
            ),
            TutorialStep(
                getString(R.string.tutorial7_24),
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_12,
                abacusReset = true
            ),
            TutorialStep(
                getString(R.string.tutorial10_13), id = "t10_94m52_94u", //Ses değişecek
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 94,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                ),
                soundResource = R.raw.tutorial10_13,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_071)
            ),
            TutorialStep(
                getString(R.string.tutorial10_14),//ses değişecek
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_14,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                    6 to Color.parseColor("#00BFFF"),
                ),
                backAnswerNumber = 94
            ),
            TutorialStep(
                getString(R.string.tutorial10_15), id = "t10_94m52_50", //ses değişecek
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 44,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_15,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                ),
                backAnswerNumber = 94,
                requestText = getString(R.string.tutorial_ipucu_072)

            ),
            TutorialStep(
                getString(R.string.tutorial10_16), id = "t10_94m52_2_cikarip_bitir", //ses değişecek
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 42,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_16,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                backAnswerNumber = 44,
                requestText = getString(R.string.tutorial_ipucu_073)

            ),
            TutorialStep(
                getString(R.string.tutorial10_17),
                questionText = "94 - 52",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_17
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps11(){
        tutorialSteps11 = listOf(
            TutorialStep(
                getString(R.string.tutorial11_1),
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_1
            ),
            TutorialStep(
                getString(R.string.tutorial11_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_2

            ),
            TutorialStep(
                getString(R.string.tutorial11_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_3
            ),
            TutorialStep(
                getString(R.string.tutorial11_4),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_4
            ),
            TutorialStep(
                getString(R.string.tutorial11_5),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_5
            ),
            TutorialStep(
                getString(R.string.tutorial11_6),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_6
            ),
            TutorialStep(
                getString(R.string.tutorial11_7),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_7
            ),
            TutorialStep(
                getString(R.string.tutorial11_8),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_8
            ),
            TutorialStep(
                getString(R.string.tutorial11_9),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_9
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_10
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_11
            ),
            TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial11_12),
                questionText = "5 - 1",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_12
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
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
                getString(R.string.tutorial4_3),
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
                getString(R.string.tutorial11_15),
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_15
            ),
            TutorialStep(
                getString(R.string.tutorial11_16),
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_16
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_17
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                getString(R.string.tutorial11_18),
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_18
            ),
            TutorialStep(
                getString(R.string.tutorial11_18),
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial11_19),
                questionText = "6 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_19
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
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
                getString(R.string.tutorial6_21),
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
                getString(R.string.tutorial11_22),
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_22
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_23
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                getString(R.string.tutorial11_24),
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_24
            ),
            TutorialStep(
                getString(R.string.tutorial11_24),
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial11_12),
                questionText = "8 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_25
            ),TutorialStep(
                getString(R.string.tutorial7_24),
                questionText = "6 - 2",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    ),
                soundResource = R.raw.tutorial10_12,
                abacusReset = true
            ),
            TutorialStep(
                getString(R.string.tutorial1100_0), id = "t11_6m2_6yi", //Ses değişecek
                questionText = "6 - 2",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 6,
                useTypewriterEffect = true,
                abacusReset = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_0,
                requestText = getString(R.string.tutorial_ipucu_031)
            ),
            TutorialStep(
                getString(R.string.tutorial1100_1),//ses değişecek
                questionText = "6 - 2",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_1,
                backAnswerNumber = 6
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                id = "t11_6m2_5_gider",
                questionText = "6 - 2",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 1,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_31,
                backAnswerNumber = 6,
                requestText = getString(R.string.tutorial_ipucu_074)

            ),
            TutorialStep(
                getString(R.string.tutorial1100_2), id = "t11_6m2_2nin_kardesi_3", //ses değişecek
                questionText = "6 - 2",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 4,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_2,
                backAnswerNumber = 1,
                requestText = getString(R.string.tutorial_ipucu_075)

            ),
            TutorialStep(
                getString(R.string.tutorial11_12),
                questionText = "6 - 2",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_3
            ),TutorialStep(
                getString(R.string.tutorial1100_4),
                id = "t11_soru_5lik_kurali_ne",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_4,
                abacusReset = true,
                options = listOf(getString(R.string.tutorial_sik_024),getString(R.string.tutorial_sik_025),getString(R.string.tutorial_sik_009)),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = getString(R.string.tutorial1100_4),
                requestText = getString(R.string.tutorial_ipucu_076)
            ),






            TutorialStep(
                getString(R.string.tutorial8_48),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_26
            )

        )
    }

    private fun createTutorialSteps1100(){
        tutorialSteps1100 = listOf(
            TutorialStep(
                getString(R.string.tutorial11000_0),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11000_0
            ),
            TutorialStep(
                getString(R.string.tutorial9_2), //sese dokunma
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_2
            ),
            TutorialStep(
                getString(R.string.tutorial11000_1),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_top", 3),
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11000_1,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial11000_2),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11000_2,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial11000_3),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11000_3,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2,
                        )
                    },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(2),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    }
                ),

                ),
            TutorialStep(
                getString(R.string.tutorial8_31), //ses ekle
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_31,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.INVISIBLE) })
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial11000_7),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11000_7,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial11000_7),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom4", 1),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial11000_4),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11000_4,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1100_5),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_5,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2,
                        )
                    },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(1),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    }
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial1100_5),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.INVISIBLE) })
            ),
            TutorialStep(
                getString(R.string.tutorial1100_6),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_6,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                )

            ),
            TutorialStep(
                getString(R.string.tutorial1100_7),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_7,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2,
                        )
                    },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(0),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(200),
                            duration = 400
                        )
                    }
                ),

                ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial8_31,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.INVISIBLE) })

            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4)
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1100_2),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_2,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1100_2),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1100_8),
                questionText = "875 - 422",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_8
            ),
            TutorialStep(
                getString(R.string.tutorial9_21), //ses ekle
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2),
                    BeadAnimation(this, "rod2_bead_bottom4", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_21,
                abacusReset = true,

                ),
            TutorialStep(
                getString(R.string.tutorial9_1000), id = "t1100_768m323_ilk_sayi", //ses ekle
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 768,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                abacusReset = true,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1100_9),
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_9,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,
                        )
                    },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(2),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
            ),TutorialStep(
                getString(R.string.tutorial1100_10),
                id = "t1100_soru_hangi_kural_cikaracaksin",
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_10,
                backAnswerNumber = 768,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                options = listOf(getString(R.string.tutorial_sik_026),getString(R.string.tutorial_sik_027),getString(R.string.tutorial_sik_028)),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_007),
                requestText = getString(R.string.tutorial_ipucu_077)
            ),
            TutorialStep(
                getString(R.string.tutorial1100_11),
                id = "t1100_768m323_3u_5lik_cikarma",
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 468,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_11,
                backAnswerNumber = 768,
                requestText = getString(R.string.tutorial_ipucu_078),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.INVISIBLE) })
            ),
            TutorialStep(
                getString(R.string.tutorial4_1003), //ses ekle
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial4_1003,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(1),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial1100_12),
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_12,
                backAnswerNumber = 468,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
            ),TutorialStep(
                getString(R.string.tutorial1100_10), id = "t1100_soru_hangi_kural_cikaracaksin_2", //yukardaki ile aynı
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_10,
                backAnswerNumber = 468,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
                options = listOf(getString(R.string.tutorial_sik_026),getString(R.string.tutorial_sik_027),getString(R.string.tutorial_sik_028)),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_008),
                requestText = getString(R.string.tutorial_ipucu_077)
            ),

            TutorialStep(
                getString(R.string.tutorial1100_13),
                id = "t1100_768m323_2yi_5lik_kurala",
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 448,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_13,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
                backAnswerNumber = 468,
                requestText = getString(R.string.tutorial_ipucu_079),
            widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.INVISIBLE) })

        ),
            TutorialStep(
                getString(R.string.tutorial9_27), //ses ekle
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_27,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = focusMarginRightPx(0),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )},
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial1100_14),
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_14,
                backAnswerNumber = 448,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial1100_10), id = "t1100_soru_hangi_kural_cikaracaksin_3", //yukardaki ile aynı
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_10,
                backAnswerNumber = 448,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),
                options = listOf(getString(R.string.tutorial_sik_026),getString(R.string.tutorial_sik_027),getString(R.string.tutorial_sik_028)),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = getString(R.string.tutorial_soru_009),
                requestText = getString(R.string.tutorial_ipucu_080)
            ),

            TutorialStep(
                getString(R.string.tutorial1100_15),
                id = "t1100_768m323_3u_birler_basamagindan",
                questionText = "768 - 323",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                answerNumber = 445,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_15,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),
                backAnswerNumber = 448,
                requestText = getString(R.string.tutorial_ipucu_041), widgetOperations = listOf( { WidgetOperation.ChangeVisibility(focusView, View.INVISIBLE) })

            ),
            TutorialStep(
                getString(R.string.tutorial1100_16),
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1100_16
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }


    private fun createTutorialSteps12(){
        tutorialSteps12 = listOf(
            TutorialStep(
                getString(R.string.tutorial12_1),
                rulesPanelVisibility = View.GONE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_1
            ),
            TutorialStep(
                getString(R.string.tutorial12_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_2
            ),
            TutorialStep(
                getString(R.string.tutorial12_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_3
            ),
            TutorialStep(
                getString(R.string.tutorial12_4),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_4
            ),
            TutorialStep(
                getString(R.string.tutorial12_5),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_5
            ),
            TutorialStep(
                getString(R.string.tutorial12_6),
                questionTextVisibility = View.VISIBLE,
                questionText = "8 - 6",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_6
            ),
            TutorialStep(
                getString(R.string.tutorial12_7),
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
                getString(R.string.tutorial12_7),
                questionTextVisibility = View.VISIBLE,
                questionText = "8 - 6",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                getString(R.string.tutorial10_5),
                questionTextVisibility = View.VISIBLE,
                questionText = "8 - 6",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_8
            ),
            TutorialStep(
                getString(R.string.tutorial12_9),
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
                getString(R.string.tutorial12_10),
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_10
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_11
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                getString(R.string.tutorial12_12),
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_12
            ),
            TutorialStep(
                getString(R.string.tutorial12_12),
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial10_5),
                questionTextVisibility = View.VISIBLE,
                questionText = "5 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_13
            ),
            TutorialStep(
                getString(R.string.tutorial12_14),
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
                getString(R.string.tutorial12_15),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_15
            ),
            TutorialStep(
                getString(R.string.tutorial12_16),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_16
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                getString(R.string.tutorial12_18),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_18
            ),
            TutorialStep(
                getString(R.string.tutorial12_18),
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
                getString(R.string.tutorial12_30),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 1",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_30
            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                getString(R.string.tutorial12_21),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_21
            ),
            TutorialStep(
                getString(R.string.tutorial12_21),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),
            TutorialStep(
                getString(R.string.tutorial2_6),
                questionTextVisibility = View.VISIBLE,
                questionText = "10 - 2",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_22
            ),

            TutorialStep(
                getString(R.string.tutorial12_9),
                questionTextVisibility = View.VISIBLE,
                questionText = "7 - 4",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_9
            ),
            TutorialStep(
                getString(R.string.tutorial12_10),
                questionTextVisibility = View.VISIBLE,
                questionText = "7 - 4",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_10
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionTextVisibility = View.VISIBLE,
                questionText = "7 - 4",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_11
            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionTextVisibility = View.VISIBLE,
                questionText = "7 - 4",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4))
            ),
            TutorialStep(
                getString(R.string.tutorial1200_0),
                questionTextVisibility = View.VISIBLE,
                questionText = "7 - 4",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1200_0
            ),
            TutorialStep(
                getString(R.string.tutorial1200_0),
                questionTextVisibility = View.VISIBLE,
                questionText = "7 - 4",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial11_19),
                questionTextVisibility = View.VISIBLE,
                questionText = "7 - 4",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_19
            ),

            TutorialStep(
                getString(R.string.tutorial1200_1), id = "t12_soru_10luk_cikarma_kuralini", //yukardaki ile aynı
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1200_1,
                questionTextColorPositions = listOf(
                    8 to Color.parseColor("#00BFFF"),
                ),
                options = listOf(getString(R.string.tutorial_sik_028),getString(R.string.tutorial_sik_026),getString(R.string.tutorial_sik_027)),
                correctOptionIndex = listOf(1,2),
                multipleChoice = true,
                optionText = getString(R.string.tutorial1200_1),
                requestText = getString(R.string.tutorial_ipucu_081)
            )




            ,
            TutorialStep(
                getString(R.string.tutorial5_28),
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_26
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                getString(R.string.tutorial12_24),
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_24
            ),
            TutorialStep(
                getString(R.string.tutorial12_24),
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))
            ),
            TutorialStep(
                getString(R.string.tutorial12_30),
                questionTextVisibility = View.VISIBLE,
                questionText = "12 - 3",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_30
            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                getString(R.string.tutorial11_24),
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_27
            ),
            TutorialStep(
                getString(R.string.tutorial11_24),
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial3_23),
                questionTextVisibility = View.VISIBLE,
                questionText = "15 - 9",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_28
            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                getString(R.string.tutorial11_18),
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_29
            ),
            TutorialStep(
                getString(R.string.tutorial11_18),
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial12_30),
                questionTextVisibility = View.VISIBLE,
                questionText = "17 - 8",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_30
            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
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
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2))
            ),
            TutorialStep(
                getString(R.string.tutorial12_31),
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_31
            ),
            TutorialStep(
                getString(R.string.tutorial12_31),
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial11_12),
                questionTextVisibility = View.VISIBLE,
                questionText = "11 - 7",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_32
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            ),

            )
    }
    
    private fun createTutorialSteps13(){
        tutorialSteps13 = listOf(
            TutorialStep(
                getString(R.string.tutorial13_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_1
            ),
            TutorialStep(
                getString(R.string.tutorial13_2),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                getString(R.string.tutorial13_3),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_3,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_4),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_4,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_5),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(55),
                            fromHeight = 0,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_5
            ),
            TutorialStep(
                getString(R.string.tutorial13_6),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_6,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_7),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_7,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_8),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
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
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(70),
                            duration = 200
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_8
            ),
            TutorialStep(
                getString(R.string.tutorial13_9),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_9,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_10,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2)),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_11),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_11,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_11),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_12),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_12,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_13),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_13,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_10,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 2)),
            ),
            TutorialStep(
                getString(R.string.tutorial13_14),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_14,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_14),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_15),
                questionText = "128 - 39",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_15
            ),
            TutorialStep(
                getString(R.string.tutorial7_24),
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
                soundResource = R.raw.tutorial13_16,
                abacusReset = true

            ),
            TutorialStep(
                getString(R.string.tutorial9_1000),
                id = "t13_165m96_ilk_sayi",
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 165,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                ),
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_082)
            ),
            TutorialStep(
                getString(R.string.tutorial13_18),
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_18,
                backAnswerNumber = 165,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial13_19),
                id = "t13_165m96_onlar_basamagindan_9u",
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 75,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_19,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                backAnswerNumber = 165,
                requestText = getString(R.string.tutorial_ipucu_083)
            ),
            TutorialStep(
                getString(R.string.tutorial13_20),
                id = "t13_165m96_birler_basamagindan_6yi",
                answerNumber = 69,
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_20,
                backAnswerNumber = 75,
                requestText = getString(R.string.tutorial_ipucu_084)
            ),
            TutorialStep(
                getString(R.string.tutorial13_21),
                questionText = "165 - 96",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_21
            ),
            TutorialStep(
                getString(R.string.tutorial13_22),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_22
            ),


        )
    }
    
    private fun createTutorialSteps14(){
        tutorialSteps14 = listOf(
            TutorialStep(
                getString(R.string.tutorial14_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_1
            ),
            TutorialStep(
                getString(R.string.tutorial13_2),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                getString(R.string.tutorial6_21),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_2,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_3),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_3,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_4),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_4,
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_5),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(55),
                            fromHeight = 0,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_5,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_6),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_6,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_7),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
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
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(70),
                            duration = 200
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_7,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_8),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_8,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_9,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )            ),
            TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4)),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )            ),
            TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_10,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                rulesPanelVisibility = View.INVISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial14_11),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_11,
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial14_12),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_12,
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_021),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_12),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_021),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_13),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_13,
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_021),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial6_13),
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
                getString(R.string.tutorial14_15),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_15,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_16),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_16,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_17),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_17,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
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
                soundResource = R.raw.tutorial14_18,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_020),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_19),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_19,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_021),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_19),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_021),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_13),
                questionText = "50 - 5",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_13,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_021),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout
            ),
            TutorialStep(
                getString(R.string.tutorial14_20),
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
                getString(R.string.tutorial13_2),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_21
            ),
            TutorialStep(
                getString(R.string.tutorial6_21),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_22,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_23),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_23,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_24),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_24,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
            ),
            TutorialStep(
                getString(R.string.tutorial14_5),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(55),
                            fromHeight = 0,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_25,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

            ),
            TutorialStep(
                getString(R.string.tutorial14_26),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
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
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(70),
                            duration = 200
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_26,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

            ),
            TutorialStep(
                getString(R.string.tutorial14_27),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_27,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial12_16),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_28,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_29),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_29,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_30,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_31),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_31,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_31),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_32),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_32,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_33),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_33,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_023),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_33),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_023),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_34),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_34,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_35),
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
                getString(R.string.tutorial14_36),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_36,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_37),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_37,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_38),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_38,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
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
                soundResource = R.raw.tutorial14_39,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_022),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
            ),
            TutorialStep(
                getString(R.string.tutorial12_24),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_40,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_023),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,

            ),
            TutorialStep(
                getString(R.string.tutorial12_24),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_023),
                rulesStepTextTopToBottomOf = R.id.tenRuleExtractionTableLayout,
            ),
            TutorialStep(
                getString(R.string.tutorial14_34),
                questionText = "100 - 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_34
            ),
            TutorialStep(
                getString(R.string.tutorial7_24),
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
                soundResource = R.raw.tutorial14_41,
                abacusReset = true

            ),
            TutorialStep(
                getString(R.string.tutorial9_1000),
                id = "t14_100m9_ilk_sayi",
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 100,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_085),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1400_0), id = "t14_100m9_9u_10luk_kural", //ses değişecek
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_0,
                answerNumber = 90,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 100,
                requestText = getString(R.string.tutorial_ipucu_086),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_024),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial1400_1), id = "t14_100m9_9un_kardesi_1", //ses değişecek
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_1,
                answerNumber = 91,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 90,
                requestText = getString(R.string.tutorial_ipucu_087),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_025),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial14_45),
                questionText = "100 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_45,
                abacusReset = true
            ),
            TutorialStep(
                getString(R.string.tutorial14_46),
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                onStepComplete = { resetAbacus("onStepComplete_callback") },
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_46,
                abacusReset = true

            ),
            TutorialStep(
                getString(R.string.tutorial9_1000),
                id = "t14_530m41_ilk_sayi",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 530,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_088),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1400_2),
                id = "t14_530m41_40i_10luk_kural",
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_2,
                answerNumber = 430,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 530,
                requestText = getString(R.string.tutorial_ipucu_089),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

                ),
            TutorialStep(
                getString(R.string.tutorial1400_3), id = "t14_530m41_4un_kardesi_6", //ses değişecek
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_3,
                answerNumber = 490,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 430,
                requestText = getString(R.string.tutorial_ipucu_090),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_027),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial1400_4),
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_4,
                backAnswerNumber = 490,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1400_5),
            id = "t14_530m41_1i_10luk_kural",
            questionText = "530 - 41",
            questionTextVisibility = View.VISIBLE,
            useTypewriterEffect = true,
            typewriterSpeed = 40L,
            soundResource = R.raw.tutorial1400_5,
            answerNumber = 480,
            nextStepAvailable = false,
            abacusClickable = true,
            backAnswerNumber = 490,
                requestText = getString(R.string.tutorial_ipucu_091),
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_028),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

            ),
            TutorialStep(
                getString(R.string.tutorial1400_6), id = "t14_530m41_1in_kardesi_9", //ses değişecek
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_6,
                answerNumber = 489,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 480,
                requestText = getString(R.string.tutorial_ipucu_092),
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_029),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
        ),
            TutorialStep(
                getString(R.string.tutorial14_53),
                questionText = "530 - 41",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_53
            ),TutorialStep(
                getString(R.string.tutorial1400_7), id = "t14_soru_10luk_cikarmada_10", //yukardaki ile aynı
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_7,
                options = listOf(getString(R.string.tutorial_sik_029),getString(R.string.tutorial_sik_030),getString(R.string.tutorial_sik_031)),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                optionText = getString(R.string.tutorial1400_7),
                requestText = getString(R.string.tutorial_ipucu_093)
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }

    private fun createTutorialSteps999(){
        tutorialSteps999 = listOf(
            TutorialStep(
            getString(R.string.tutorial14_1),
            useTypewriterEffect = true,
            typewriterSpeed = 40L,
            soundResource = R.raw.tutorial14_1
        ),TutorialStep(
        getString(R.string.tutorial14_1),
        useTypewriterEffect = true,
        typewriterSpeed = 40L,
        soundResource = R.raw.tutorial14_1
        )
        )
    }
    
    private fun createTutorialSteps15(){
        tutorialSteps15 = listOf(
            TutorialStep(
                getString(R.string.tutorial10_6),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6
            ),TutorialStep(
                getString(R.string.tutorial15_2),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(55),
                            fromHeight = 0,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_2,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )

            ),TutorialStep(
                getString(R.string.tutorial15_3),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_3,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_4),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_4,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_5),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(55),
                            fromHeight = 0,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_5,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

            ),TutorialStep(
                getString(R.string.tutorial15_6),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_6,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_7),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_7,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_8),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_8,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_9),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(2),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = 0,
                            toWidth = dpToPx(55),
                            fromHeight = 0,
                            toHeight = dpToPx(270),
                            duration = 400
                        )
                    }
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_9,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

            ),TutorialStep(
                getString(R.string.tutorial15_10),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_10,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_11),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_11,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial8_31),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_12,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_13,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom4", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_14),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_14,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_14),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_15), //Ses değişecek
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_15,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_027),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_15),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_027),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_16),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_16
            ),TutorialStep(
                getString(R.string.tutorial14_35),
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
                getString(R.string.tutorial15_18),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_18,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
            ),TutorialStep(
                getString(R.string.tutorial15_19),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_19,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_20),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_20,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_8),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_21,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial12_17),
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
                soundResource = R.raw.tutorial15_22,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial12_18),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_23,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial12_18),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_026),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_25), //ses  değişecek
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_25,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_027),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_25),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_027),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial15_16),
                questionText = "500 - 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_26
            ),TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps16(){
        tutorialSteps16 = listOf(
            TutorialStep(
                getString(R.string.tutorial16_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1

            ),
            TutorialStep(
                getString(R.string.tutorial16_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_2

            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6

            ),
            TutorialStep(
                getString(R.string.tutorial16_7),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_7,
                widgetVisibilityMap = mapOf(
                    R.id.tenRuleExtractionTableLayout to View.VISIBLE,
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE

            ),
            TutorialStep(
                getString(R.string.tutorial16_8),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_8

            ),
            TutorialStep(
                getString(R.string.tutorial16_9),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_9,
                widgetVisibilityMap = mapOf(
                    R.id.tenRuleExtractionTableLayout to View.GONE,
                    R.id.fiveRuleTable to View.VISIBLE,
                    )

            ),
            TutorialStep(
                getString(R.string.tutorial16_10),//ses değişecek
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_10

            ),
            TutorialStep(
                getString(R.string.tutorial_txt_007), //ses değişecek
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    ),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_13

            ),
            TutorialStep(
                getString(R.string.tutorial16_11),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial16_11,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial16_11),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE

            ),
            TutorialStep(
                getString(R.string.tutorial3_15),
                questionText = "11 - 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial5_28),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)),
                soundResource = R.raw.tutorial5_28,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                widgetVisibilityMap = mapOf(
                    R.id.tenRuleExtractionTableLayout to View.VISIBLE,
                    R.id.fiveRuleTable to View.GONE,
                    )

            ),
            TutorialStep(
                getString(R.string.tutorial16_13),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_13

            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                getString(R.string.tutorial16_14),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_14

            ),
            TutorialStep(
                getString(R.string.tutorial16_15),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_15,
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.VISIBLE,
                    R.id.tenRuleExtractionTableLayout to View.GONE,
                )

            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_13,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE

            ),
            TutorialStep(
                getString(R.string.tutorial3_15),
                questionText = "12 - 7",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial_txt_008),
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
                typewriterSpeed = 40L,
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.GONE,
                    R.id.tenRuleExtractionTableLayout to View.VISIBLE,
                )

            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                getString(R.string.tutorial16_16),
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_16,
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.VISIBLE,
                    R.id.tenRuleExtractionTableLayout to View.GONE,
                )

            ),
            TutorialStep(
                getString(R.string.tutorial16_16),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2)),
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,

            ),
            TutorialStep(
                getString(R.string.tutorial3_15),
                questionText = "13 - 8",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L

            ),
            TutorialStep(
                getString(R.string.tutorial_txt_008),
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
                typewriterSpeed = 40L,
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.GONE,
                    R.id.tenRuleExtractionTableLayout to View.VISIBLE,
                )


            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17

            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
            ),
            TutorialStep(
                getString(R.string.tutorial16_17),
                questionText = "14 - 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_17,
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.VISIBLE,
                    R.id.tenRuleExtractionTableLayout to View.GONE,
                )

            ),
            TutorialStep(
                getString(R.string.tutorial16_17),
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
                getString(R.string.tutorial15_29),
                id = "t16_soru_cikarmanin_10luk_cikarmadan",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_29,
                options = listOf(getString(R.string.tutorial_ipucu_094),getString(R.string.tutorial_sik_032),getString(R.string.tutorial_sik_033)),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                optionText = getString(R.string.tutorial15_29),
                requestText = getString(R.string.tutorial_ipucu_094)
            ),
            TutorialStep(
                getString(R.string.tutorial7_24),
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_41,
                abacusReset = true,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4)),
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.GONE,
                    R.id.tenRuleExtractionTableLayout to View.GONE,
                )
                ),
            TutorialStep(
                getString(R.string.tutorial9_1000),
                id = "t16_23m6_ilk_sayi",
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 23,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_095),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial1400_10),
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_10,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial1400_11), //ses değişecek
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 23,
                soundResource = R.raw.tutorial1400_11,
                requestText = getString(R.string.tutorial_ipucu_087),
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.GONE,
                    R.id.tenRuleExtractionTableLayout to View.GONE,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_030),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),TutorialStep(
                getString(R.string.tutorial1400_12), id = "t16_23m6_10_gider", //ses değişecek
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_12,
                answerNumber = 13,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 23,
                requestText = getString(R.string.tutorial_ipucu_096),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_031),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),TutorialStep(
                getString(R.string.tutorial1400_13), //ses değişecek
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_13,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_032),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                widgetVisibilityMap = mapOf(
                    R.id.fiveRuleTable to View.VISIBLE,
                    R.id.tenRuleExtractionTableLayout to View.GONE,
                )
            ),TutorialStep(
                getString(R.string.tutorial1400_14), id = "t16_23m6_5_gelir_4un", //ses değişecek
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial1400_14,
                answerNumber = 17,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 13,
                requestText = getString(R.string.tutorial_ipucu_097),
                rulesPanelVisibility = View.INVISIBLE,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_032),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                questionTextColorPositions = listOf(
                    5 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial14_455),
                questionText = "23 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_455,
            ),



            TutorialStep(
                getString(R.string.tutorial16_1000),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1000,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4)),
                widgetVisibilityMap = mapOf(
                    R.id.BeadRuleExtractionTable to View.GONE,
                )
            ),TutorialStep(
                getString(R.string.tutorial16_1003),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1003,
                widgetVisibilityMap = mapOf(
                    R.id.BeadRuleExtractionTable to View.VISIBLE,
                ),
            ),TutorialStep(
                getString(R.string.tutorial13_2),
                questionText = "13 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_21,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1)),
            ),TutorialStep(
                getString(R.string.tutorial16_1001),
                questionText = "13 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1001,
            ),TutorialStep(
                getString(R.string.tutorial16_11),
                questionText = "13 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_11,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom3", 2)),
            ),TutorialStep(
                getString(R.string.tutorial3_13),
                questionText = "13 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_13,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3)),
            ),TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "13 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
            ),TutorialStep(
                getString(R.string.tutorial16_1005),
                questionText = "13 - 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1005,
            ),TutorialStep(
                getString(R.string.tutorial5_39),
                questionText = "33 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                abacusReset = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial5_39,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    ),
            ),TutorialStep(
                getString(R.string.tutorial9_1000),
                id = "t16_33m8_ilk_sayi",
                questionText = "33 - 8",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 33,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_098),
            ),
            TutorialStep(
                getString(R.string.tutorial16_1002),
                questionText = "33 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1002,
                backAnswerNumber = 33
            ),
            TutorialStep(
                getString(R.string.tutorial16_1006),
                id = "t16_33m8_3_gider",
                questionText = "33 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1006,
                answerNumber = 30,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 33,
                requestText = getString(R.string.tutorial_ipucu_041),
            ),
            TutorialStep(
                getString(R.string.tutorial3_13),
                id = "t16_33m8_5_gelir",
                questionText = "33 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial3_13,
                answerNumber = 35,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 30,
                requestText = getString(R.string.tutorial_ipucu_099),
            ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                id = "t16_33m8_10_gider",
                questionText = "33 - 8",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17,
                answerNumber = 25,
                nextStepAvailable = false,
                abacusClickable = true,
                backAnswerNumber = 35,
                requestText = getString(R.string.tutorial_ipucu_096),
            ),TutorialStep(
                getString(R.string.tutorial16_1004),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial16_1004

            )











            ,TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27

            )
        )
    }
    
    private fun createTutorialSteps17(){
        tutorialSteps17 = listOf(
            TutorialStep(
                getString(R.string.tutorial10_6),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial10_6,
                rulesPanelVisibility = View.GONE
                ),
            TutorialStep(
                getString(R.string.tutorial6_21),
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
                typewriterSpeed = 40L,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                )
                ),

            TutorialStep(
                getString(R.string.tutorial17_1),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_1,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial17_2),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_2,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom4", 2)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial11_18),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_18,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial11_18),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_top", 3)),
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial17_3),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_3,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial12_17,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial12_17),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1),
                    BeadAnimation(this, "rod3_bead_top", 4)),
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial11_11,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial11_11),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial103_348),
                questionText = "434 - 86",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_4,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial17_5),
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
                soundResource = R.raw.tutorial17_5,
                abacusReset = true
                ),
            TutorialStep(
                getString(R.string.tutorial9_1000),
                id = "t17_544m87_ilk_sayi",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 544,
                nextStepAvailable = false,
                abacusReset = true,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1000,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    1 to Color.parseColor("#00BFFF"),
                    2 to Color.parseColor("#00BFFF"),
                ),
                requestText = getString(R.string.tutorial_ipucu_100)
                ),
            TutorialStep(
                getString(R.string.tutorial17_6),
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_6,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),
                backAnswerNumber = 544
                ),
            TutorialStep(
                getString(R.string.tutorial17_7), id = "t17_544m87_10_gider", //
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 444,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                backAnswerNumber = 544,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_7,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_033),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                requestText = getString(R.string.tutorial_ipucu_101)
                ),
            TutorialStep(
                getString(R.string.tutorial17_8),
                id = "t17_544m87_8in_kardesi_2",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 464,
                backAnswerNumber = 444,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_8,
                questionTextColorPositions = listOf(
                    6 to Color.parseColor("#00BFFF"),
                ),rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_034),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                requestText = getString(R.string.tutorial_ipucu_102)
                ),
            TutorialStep(
                getString(R.string.tutorial17_10),
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_10,
                backAnswerNumber = 464,
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial17_11),
                id = "t17_544m87_10_gider_yap",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 454,
                backAnswerNumber = 464,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_11,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_035),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                requestText = getString(R.string.tutorial_ipucu_103),
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial17_12),
                id = "t17_544m87_7nin_kardesi_3",
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 457,
                nextStepAvailable = false,
                backAnswerNumber = 454,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_12,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_036),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                requestText = getString(R.string.tutorial_ipucu_104),
                questionTextColorPositions = listOf(
                    7 to Color.parseColor("#00BFFF"),
                )
                ),
            TutorialStep(
                getString(R.string.tutorial17_13),
                questionText = "544 - 87",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial17_13
                ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
                )
        )
    }
    
    private fun createTutorialSteps18(){
        tutorialSteps18 = listOf(
            TutorialStep(
                getString(R.string.tutorial18_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_1
            ),
            TutorialStep(
                getString(R.string.tutorial18_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_3
            ),
            TutorialStep(
                getString(R.string.tutorial18_4),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_4
            ),
            TutorialStep(
                getString(R.string.tutorial18_5),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_5
            ),
            TutorialStep(
                getString(R.string.tutorial2_17),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial2_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                getString(R.string.tutorial18_6),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_6,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial18_7),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_7,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_037),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_7),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_037),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_8),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_8,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_038),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_9),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_9,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_038),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_10),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(95),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(280),
                            duration = 0
                        )
                    }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_10,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_038),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_10),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_038),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

            ),
            TutorialStep(
                getString(R.string.tutorial9_20),
                questionText = "24 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_20
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
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
                getString(R.string.tutorial18_11),
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_11,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_039),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_11),
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom1", 1)),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_039),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_12),
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_12,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_040),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_13),
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_13,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_040),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_14),
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_14,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_040),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_14),
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)),
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_040),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_15),
                questionText = "12 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_15
            ),
            TutorialStep(
                getString(R.string.tutorial18_16),
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
                getString(R.string.tutorial18_17),
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_17,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_041),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_17),
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_041),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_18),
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_18,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_042),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_18),
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod2_bead_bottom1", 1)),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_042),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial18_19),
                questionText = "58 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_19
            ),
            //Bu işlemi beraber yapalım
            TutorialStep(
                getString(R.string.tutorial7_24),
                questionText = "46 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial14_41, animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2),
                    ),
                abacusReset = true,

                ),
            TutorialStep(
                getString(R.string.tutorial18_20),
                id = "t18_46x3_3_6yi_carpip",
                questionText = "46 x 3",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 18,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_20,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_105),
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF"),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_043),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),TutorialStep(
                getString(R.string.tutorial18_21),
                questionText = "46 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_21,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
            ),TutorialStep(
                getString(R.string.tutorial18_22),
                id = "t18_soru_birler_basamagi_onlar",
                questionText = "46 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_22,
                options = listOf(getString(R.string.tutorial_sik_034),getString(R.string.tutorial_sik_035),getString(R.string.tutorial_sik_036)),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                backAnswerNumber = 18,
                optionText = getString(R.string.tutorial18_22),
                requestText = getString(R.string.tutorial_ipucu_106),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial18_23),
                id = "t18_46x3_onlar_4_3u",
                questionText = "46 x 3",
                questionTextVisibility = View.VISIBLE,
                abacusClickable = true,
                nextStepAvailable = false,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                answerNumber = 138,
                backAnswerNumber = 18,
                soundResource = R.raw.tutorial18_23,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF"),
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_044),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                requestText = getString(R.string.tutorial_ipucu_107)
            ),
            TutorialStep(
                getString(R.string.tutorial18_24), //ses değişecek
                questionText = "46 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial18_24,
            )

            ,
            TutorialStep(
                getString(R.string.tutorial15_27),
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps19(){
        tutorialSteps19 = listOf(
            TutorialStep(
                getString(R.string.tutorial19_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_1
            ),
            TutorialStep(
                getString(R.string.tutorial13_2),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                getString(R.string.tutorial19_2),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_2,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_045),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_2),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod3_bead_bottom4", 1)),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_045),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_3),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_3,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_046),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_4),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_4,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_046),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_4),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 3)),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_046),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_5),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = focusMarginRightPx(1),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 0
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(55),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(280),
                            duration = 0
                        )
                    }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_5,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_046),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_6),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_6,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_046),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_7),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_7,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_046),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_7),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom4", 2)),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_046),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_8),
                questionText = "97 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_8
            ),
            TutorialStep(
                getString(R.string.tutorial4_2),
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
                getString(R.string.tutorial19_9),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_9,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_037),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,

            ),
            TutorialStep(
                getString(R.string.tutorial19_9),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1)),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_037),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_10),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_10,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_11),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_11,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_11),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom4", 1)),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_12),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_12,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_13),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                widgetOperations = listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.GONE) }),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_13,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_14),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_14,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial5_20),
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
                typewriterSpeed = 40L,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial3_30,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial3_30),
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_047),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial19_15),
                questionText = "84 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_15
            ),
            TutorialStep(
                getString(R.string.tutorial19_22),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_22,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))
            )

            ,
            TutorialStep(
                getString(R.string.tutorial7_24),
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial7_24,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true
            ),
            TutorialStep(
                getString(R.string.tutorial19_16),
                id = "t19_48x9_9_8i_carpip",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 72,
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_16,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_108)
            ),
            TutorialStep(
                getString(R.string.tutorial19_17),
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 72,
                soundResource = R.raw.tutorial19_17,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial19_18),
                id = "t19_48x9_9_x_4",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 372,
                backAnswerNumber = 72,
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_18,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                requestText = getString(R.string.tutorial_ipucu_109)
            ),
            TutorialStep(
                getString(R.string.tutorial19_19),
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_19,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                )
            ),TutorialStep(
                getString(R.string.tutorial19_23),
                id = "t19_soru_hangi_toplama_kurali",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_23,
                options = listOf(getString(R.string.tutorial_sik_012),getString(R.string.tutorial_sik_013),getString(R.string.tutorial_sik_023)),
                backAnswerNumber = 372,
                correctOptionIndex = listOf(2),
                multipleChoice = false,
                optionText = getString(R.string.tutorial19_23),
                requestText = getString(R.string.tutorial_ipucu_110),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial9_1002),
                id = "t19_48x9_6yi_kurali_onlar",
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 432,
                backAnswerNumber = 372,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial9_1002,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_048),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                ),
                requestText = getString(R.string.tutorial_ipucu_111)
            ),
            TutorialStep(
                getString(R.string.tutorial19_21),
                questionText = "48 x 9",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial19_21
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps20(){
        tutorialSteps20 = listOf(
            TutorialStep(
                getString(R.string.tutorial20_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_1
            ),
            TutorialStep(
                getString(R.string.tutorial20_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_2
            ),
            TutorialStep(
                getString(R.string.tutorial20_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_3
            ),
            TutorialStep(
                getString(R.string.tutorial5_3),
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                getString(R.string.tutorial20_4),
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_4
            ),
            TutorialStep(
                getString(R.string.tutorial20_5),
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_5
            ),
            TutorialStep(
                getString(R.string.tutorial20_5),
                questionText = "5 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3),
                    BeadAnimation(this, "rod3_bead_bottom1", 1)),
            ),
            TutorialStep(
                getString(R.string.tutorial20_6),
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_6
            ),
            TutorialStep(
                getString(R.string.tutorial5_3),
                questionText = "40 x 3",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L            ),

            TutorialStep(
                getString(R.string.tutorial20_8),
                questionText = "40 x 3",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_8,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial20_8),
                questionText = "40 x 3",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW
                ),
            ),
            TutorialStep(
                getString(R.string.tutorial20_9),
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2)),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_9
            ),
            TutorialStep(
                getString(R.string.tutorial5_3),
                questionText = "30 x 60",
                questionTextVisibility = View.VISIBLE,
                soundResource = R.raw.tutorial5_3,
                useTypewriterEffect = true,
                typewriterSpeed = 40L
            ),
            TutorialStep(
                getString(R.string.tutorial20_10),
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
                getString(R.string.tutorial20_11),
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
                getString(R.string.tutorial20_11),
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
                getString(R.string.tutorial10_6),
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
                getString(R.string.tutorial20_12),
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
                getString(R.string.tutorial20_13),
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_13,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_040),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_13),
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_040),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_14),
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_14,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_049),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_14),
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
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_049),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_15),
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_15
            ),
            TutorialStep(
                getString(R.string.tutorial20_16),
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_16,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_050),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_16),
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
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_050),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_17),
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_17,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_051),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_17),
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
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_051),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_18),
                questionText = "43 x 21",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_18
            ),
            TutorialStep(
                getString(R.string.tutorial10_6),
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
                getString(R.string.tutorial20_12),
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
                getString(R.string.tutorial20_20),
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_20,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_052),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_20),
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
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_052),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_21),
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_21,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_053),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_21),
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1)),
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_053),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_22),
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_22,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_054),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_22),
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
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_054),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_23),
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_23,
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_055),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_23),
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
                ),
                rulesStepTextVisibility = View.VISIBLE,
                rulesStepTextContent = getString(R.string.tutorial_kural_055),
                rulesStepTextTopToBottomOf = R.id.guide_line_2_7,
            ),
            TutorialStep(
                getString(R.string.tutorial20_100),
                questionText = "78 x 43",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_100
            ),
            TutorialStep(
                getString(R.string.tutorial7_24),
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
                getString(R.string.tutorial20_24),
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
                getString(R.string.tutorial20_30), //ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF"),
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_30
            ),TutorialStep(
                getString(R.string.tutorial20_31),
                id = "t20_soru_hangi_basamaga_ekleyeceksin",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_31,
                options = listOf(getString(R.string.tutorial_sik_034),getString(R.string.tutorial_sik_035),getString(R.string.tutorial_sik_036)),
                correctOptionIndex = listOf(0),
                multipleChoice = false,
                abacusReset = true,
                optionText = getString(R.string.tutorial20_31),
                requestText = getString(R.string.tutorial_ipucu_112),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial20_25), id = "t20_69x54_4_9u_carpip",//aynı ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 36,
                abacusReset = true,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_25,
                requestText = getString(R.string.tutorial_ipucu_113)
            ),
            TutorialStep(
                getString(R.string.tutorial20_32), //ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF"),
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_32
            ),TutorialStep(
                getString(R.string.tutorial20_31),
                id = "t20_soru_hangi_basamaga_ekleyeceksin_2",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_31,
                options = listOf(getString(R.string.tutorial_sik_034),getString(R.string.tutorial_sik_035),getString(R.string.tutorial_sik_036)),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                backAnswerNumber = 36,
                optionText = getString(R.string.tutorial20_31),
                requestText = getString(R.string.tutorial_ipucu_114),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial20_33), id = "t20_69x54_4_6yi_carpip",//ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 276,
                backAnswerNumber = 36,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    6 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_33,
                requestText = getString(R.string.tutorial_ipucu_115)
            ),
            TutorialStep(
                getString(R.string.tutorial20_34), //ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF"),
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_34
            ),TutorialStep(
                getString(R.string.tutorial20_31),
                id = "t20_soru_hangi_basamaga_ekleyeceksin_3",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_31,
                options = listOf(getString(R.string.tutorial_sik_034),getString(R.string.tutorial_sik_035),getString(R.string.tutorial_sik_036)),
                correctOptionIndex = listOf(1),
                multipleChoice = false,
                backAnswerNumber = 276,
                optionText = getString(R.string.tutorial20_31),
                requestText = getString(R.string.tutorial_ipucu_114),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial20_35), id = "t20_69x54_5_9u_carpip",//ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 726,
                backAnswerNumber = 276,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    1 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_35,
                requestText = getString(R.string.tutorial_ipucu_116)
            ),
            TutorialStep(
                getString(R.string.tutorial20_36), //ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF"),
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_36
            ),TutorialStep(
                getString(R.string.tutorial20_31),
                id = "t20_soru_hangi_basamaga_ekleyeceksin_4",
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_31,
                options = listOf(getString(R.string.tutorial_sik_034),getString(R.string.tutorial_sik_035),getString(R.string.tutorial_sik_036)),
                correctOptionIndex = listOf(2),
                multipleChoice = false,
                backAnswerNumber = 726,
                optionText = getString(R.string.tutorial20_31),
                requestText = getString(R.string.tutorial_ipucu_117),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    5 to Color.YELLOW,
                )
            ),
            TutorialStep(
                getString(R.string.tutorial20_37), id = "t20_69x54_5_6yi_carpip",//ses
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 3726,
                backAnswerNumber = 726,
                nextStepAvailable = false,
                abacusClickable = true,
                questionTextColorPositions = listOf(
                    5 to Color.YELLOW,
                    0 to Color.parseColor("#00BFFF")
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_37,
                requestText = getString(R.string.tutorial_ipucu_118)


            ),
            TutorialStep(
                getString(R.string.tutorial20_29),
                questionText = "69 x 54",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial20_29
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps21(){
        tutorialSteps21 = listOf(
            TutorialStep(
                getString(R.string.tutorial21_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_1
            ),
            TutorialStep(
                getString(R.string.tutorial21_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_2
            ),
            TutorialStep(
                getString(R.string.tutorial21_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_3
            ),
            TutorialStep(
                getString(R.string.tutorial13_2),
                questionText = "300 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                getString(R.string.tutorial20_10),
                questionText = "300 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_4
            ),
            TutorialStep(
                getString(R.string.tutorial21_5),
                questionText = "300 x 6",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_5
            ),
            TutorialStep(
                getString(R.string.tutorial21_5),
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
                getString(R.string.tutorial21_6),
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
                getString(R.string.tutorial21_7),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_7
            ),
            TutorialStep(
                getString(R.string.tutorial21_8),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_8
            ),
            TutorialStep(
                getString(R.string.tutorial21_9),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_9
            ),
            TutorialStep(
                getString(R.string.tutorial21_10),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_10,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_11),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_11,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_11),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3)),
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_12),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_12,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_12),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_bottom3", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 1),
                    BeadAnimation(this, "rod2_bead_bottom3", 1)),
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_13),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_13,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_13),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod2_bead_bottom2", 2),
                    BeadAnimation(this, "rod2_bead_bottom3", 2)),
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_14),
                questionText = "284 x 4",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_14
            ),
            TutorialStep(
                getString(R.string.tutorial5_39),
                soundResource = R.raw.tutorial5_39,
                animation = listOf(
                    BeadAnimation(this, "rod1_bead_bottom1", 2),
                    BeadAnimation(this, "rod2_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 4),
                    ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                questionText = "723 x 6",
                questionTextVisibility = View.VISIBLE,
                abacusReset = true
            ),
            TutorialStep(
                getString(R.string.tutorial21_15),
                id = "t21_723x6_6_3u_carpip",
                questionTextVisibility = View.VISIBLE,
                questionText = "723 x 6",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 18,
                soundResource = R.raw.tutorial21_15,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                requestText = getString(R.string.tutorial_ipucu_119),
                questionTextColorPositions = listOf(
                    2 to Color.YELLOW,
                    6 to Color.parseColor("#00BFFF"),
                )
            ),TutorialStep(
                getString(R.string.tutorial21_16),
                id = "t21_723x6_6_onlar_basamaginda",
                questionText = "723 x 6",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 138,
                backAnswerNumber = 18,
                abacusClickable = true,
                nextStepAvailable = false,
                soundResource = R.raw.tutorial21_16,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                requestText = getString(R.string.tutorial_ipucu_120),
                questionTextColorPositions = listOf(
                    1 to Color.YELLOW,
                    6 to Color.parseColor("#00BFFF"),
                )
            ),TutorialStep(
                getString(R.string.tutorial21_17),
                id = "t21_723x6_6_yuzler_basamaginda",
                questionText = "723 x 6",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 4338,
                abacusClickable = true,
                nextStepAvailable = false,
                soundResource = R.raw.tutorial21_17,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                backAnswerNumber = 138,
                requestText = getString(R.string.tutorial_ipucu_121),
                questionTextColorPositions = listOf(
                    0 to Color.YELLOW,
                    6 to Color.parseColor("#00BFFF"),
                )
            ),
            TutorialStep(
                getString(R.string.tutorial21_18),
                questionTextVisibility = View.VISIBLE,
                questionText = "723 x 6",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial21_18
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )
        )
    }
    
    private fun createTutorialSteps22(){
        tutorialSteps22 = listOf(
            TutorialStep(
                getString(R.string.tutorial22_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_1
            ),
            TutorialStep(
                getString(R.string.tutorial22_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_2
            ),
            TutorialStep(
                getString(R.string.tutorial22_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_3
            ),
            TutorialStep(
                getString(R.string.tutorial13_2),
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial13_2
            ),
            TutorialStep(
                getString(R.string.tutorial22_4),
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_4
            ),
            TutorialStep(
                getString(R.string.tutorial22_5),
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_5
            ),
            TutorialStep(
                getString(R.string.tutorial22_5),
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod0_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom1", 1),
                    BeadAnimation(this, "rod1_bead_bottom2", 1))
            ),
            TutorialStep(
                getString(R.string.tutorial22_25),
                questionText = "600 x 20",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_25
            ),
            TutorialStep(
                getString(R.string.tutorial22_6),
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
                getString(R.string.tutorial22_7),
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
                getString(R.string.tutorial22_8),
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
                getString(R.string.tutorial22_8),
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
                getString(R.string.tutorial22_100),
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
                getString(R.string.tutorial22_100),
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
                getString(R.string.tutorial22_10),
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
                getString(R.string.tutorial22_10),
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
                getString(R.string.tutorial22_11),
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_11
            ),
            TutorialStep(
                getString(R.string.tutorial22_12),
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
                getString(R.string.tutorial22_12),
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
                getString(R.string.tutorial22_13),
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
                getString(R.string.tutorial22_13),
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
                getString(R.string.tutorial22_14),
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
                getString(R.string.tutorial22_14),
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
                getString(R.string.tutorial22_15),
                questionText = "321 x 23",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_15
            ),
            TutorialStep(
                getString(R.string.tutorial5_39),
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
                typewriterSpeed = 40L,
            ),
            TutorialStep(
                getString(R.string.tutorial22_16),
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                abacusReset = true,
                soundResource = R.raw.tutorial22_16,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                ),

            ),
            TutorialStep(
                getString(R.string.tutorial22_17), id = "t22_312x42_2_2yi_carpip", //ses değişecek
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 4,
                abacusReset = true,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_17,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),  // "24" içindeki "4" (1. pozisyon)
                    7 to Color.YELLOW   // "32" içindeki "2" (5. pozisyon)
                ),
                requestText = getString(R.string.tutorial_ipucu_122)

            ),
            TutorialStep(
                getString(R.string.tutorial22_18), id = "t22_312x42_2_1i_carpip", //ses değişecek
                answerNumber = 24,
                backAnswerNumber = 4,
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
                ),
                requestText = getString(R.string.tutorial_ipucu_123)

            ),
            TutorialStep(
                getString(R.string.tutorial22_19), id = "t22_312x42_2_3u_carpip", //ses değişecek
                answerNumber = 624,
                backAnswerNumber = 24,
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
                ),
                requestText = getString(R.string.tutorial_ipucu_124)

            ),
            TutorialStep(
                getString(R.string.tutorial22_20),
                questionText = "312 x 42",
                backAnswerNumber = 624,
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_20
            ),
            TutorialStep(
                getString(R.string.tutorial22_21), id = "t22_312x42_4_2yi_carpip", //ses değişecek
                answerNumber = 704,
                backAnswerNumber = 624,
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
                ),
                requestText = getString(R.string.tutorial_ipucu_125)

            ),
            TutorialStep(
                getString(R.string.tutorial22_22), id = "t22_312x42_4_1i_carpip", //ses değişecek
                answerNumber = 1104,
                backAnswerNumber = 704,
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
                ),
                requestText = getString(R.string.tutorial_ipucu_126)

            ),
            TutorialStep(
                getString(R.string.tutorial22_23), id = "t22_312x42_4_3u_carpip", //ses değişecek
                answerNumber = 13104,
                backAnswerNumber = 1104,
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
                ),
                requestText = getString(R.string.tutorial_ipucu_127)

            ),
            TutorialStep(
                getString(R.string.tutorial22_24),
                questionText = "312 x 42",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_24
            ),TutorialStep(
                getString(R.string.tutorial22_26),
                id = "t22_soru_asagidaki_seceneklerden_hangisi",
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_26,
                options = listOf(getString(R.string.tutorial_sik_037),getString(R.string.tutorial_sik_038),getString(R.string.tutorial_sik_039)),
                correctOptionIndex = listOf(2),
                multipleChoice = false,
                optionText = getString(R.string.tutorial22_26),
                requestText = getString(R.string.tutorial_ipucu_128),
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            ),
        )
    }
    
    private fun createTutorialSteps23(){
        tutorialSteps23 = listOf(
            TutorialStep(
                getString(R.string.tutorial22_6),
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_6
            ),
            TutorialStep(
                getString(R.string.tutorial22_7),
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
                getString(R.string.tutorial23_1),
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
                getString(R.string.tutorial23_1),
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
                getString(R.string.tutorial23_2),
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
                getString(R.string.tutorial23_2),
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
                getString(R.string.tutorial23_3),
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
                getString(R.string.tutorial23_3),
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
                getString(R.string.tutorial22_11),
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_11
            ),
            TutorialStep(
                getString(R.string.tutorial23_4),
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
                getString(R.string.tutorial23_4),
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
                getString(R.string.tutorial23_5),
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
                getString(R.string.tutorial23_5),
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
                getString(R.string.tutorial23_6),
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
                getString(R.string.tutorial23_6),
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
                getString(R.string.tutorial23_7),
                questionText = "586 x 97",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_7
            ),
            TutorialStep(
                getString(R.string.tutorial5_39),
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
                getString(R.string.tutorial23_8),//ses değişecek
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_8,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),
                    7 to Color.YELLOW
                ),
                abacusReset = true
            ),
            TutorialStep(
                getString(R.string.tutorial23_9),
                id = "t23_765x84_4_5i_carpip",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 20,
                abacusReset = true,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_9,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),
                    7 to Color.YELLOW
                ),
                requestText = getString(R.string.tutorial_ipucu_129)
            ),
            TutorialStep(
                getString(R.string.tutorial23_10),
                id = "t23_765x84_4_6yi_carpip",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 260,
                backAnswerNumber = 20,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_10,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    7 to Color.YELLOW
                ),
                requestText = getString(R.string.tutorial_ipucu_130)

            ),
            TutorialStep(
                getString(R.string.tutorial23_12),
                id = "t23_765x84_4_7yi_carpip",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 3060,
                backAnswerNumber = 260,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_12,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    7 to Color.YELLOW
                ),
                requestText = getString(R.string.tutorial_ipucu_131)

            ),
            TutorialStep(
                getString(R.string.tutorial22_20),
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial22_20,
                backAnswerNumber = 3060
            ),
            TutorialStep(
                getString(R.string.tutorial23_13),
                id = "t23_765x84_8_5i_carpip",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 3460,
                backAnswerNumber = 3060,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_13,
                questionTextColorPositions = listOf(
                    2 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                ),
                requestText = getString(R.string.tutorial_ipucu_132)

            ),
            TutorialStep(
                getString(R.string.tutorial23_14),
                id = "t23_765x84_8_6yi_carpip",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 8260,
                backAnswerNumber = 3460,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_14,
                questionTextColorPositions = listOf(
                    1 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                ),
                requestText = getString(R.string.tutorial_ipucu_133)
            ),
            TutorialStep(
                getString(R.string.tutorial23_15),
                id = "t23_765x84_8_7yi_carpip",
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 64260,
                backAnswerNumber = 8260,
                nextStepAvailable = false,
                abacusClickable = true,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_15,
                questionTextColorPositions = listOf(
                    0 to Color.parseColor("#00BFFF"),
                    6 to Color.YELLOW
                ),
                requestText = getString(R.string.tutorial_ipucu_134)

            ),
            TutorialStep(
                getString(R.string.tutorial23_16),
                questionText = "765 x 84",
                questionTextVisibility = View.VISIBLE,
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_16
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial15_27
            )

        )
    }
    
    private fun createTutorialSteps24(){
        tutorialSteps24 = listOf(
            TutorialStep(
                getString(R.string.tutorial24_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_1

            ),
            TutorialStep(
                getString(R.string.tutorial24_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_2
            ),
            TutorialStep(
                getString(R.string.tutorial24_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_3

            ),
            TutorialStep(
                getString(R.string.tutorial24_4),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_4
            ),
            TutorialStep(
                getString(R.string.tutorial24_5),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_5
            ),
            TutorialStep(
                getString(R.string.tutorial24_6),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_6
            ),
            TutorialStep(
                getString(R.string.tutorial24_7),
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
                getString(R.string.tutorial24_8),
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial2, View.VISIBLE) },
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial1, View.INVISIBLE) },
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_8
            ),
            TutorialStep(
                getString(R.string.tutorial24_9),
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial2, View.INVISIBLE) },
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_9
            ),
            TutorialStep(
                getString(R.string.tutorial103_6),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_10
            ),
            TutorialStep(
                getString(R.string.tutorial23_99),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial23_99
            ),
            TutorialStep(
                getString(R.string.tutorial24_12),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_12
            ),
            TutorialStep(
                getString(R.string.tutorial24_13),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_13
            ),
            TutorialStep(
                getString(R.string.tutorial24_14),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_14
            ),
            TutorialStep(
                getString(R.string.tutorial24_15),
                widgetOperations =listOf(
                    { WidgetOperation.ChangeVisibility(binding.blindingTutorial3, View.VISIBLE) },
                ),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_15
            ),
            TutorialStep(
                getString(R.string.tutorial24_16),
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
                getString(R.string.tutorial25_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_1
            ),
            TutorialStep(
                getString(R.string.tutorial25_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_2
            ),
            TutorialStep(
                getString(R.string.tutorial25_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_3
            ),
            TutorialStep(
                getString(R.string.tutorial25_4),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_4
            ),
            TutorialStep(
                getString(R.string.tutorial25_5),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_5
            ),
            TutorialStep(
                getString(R.string.tutorial25_6),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_6
            ),
            TutorialStep(
                getString(R.string.tutorial25_7),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial25_7
            ),
            TutorialStep(
                getString(R.string.tutorial24_16),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial24_16
            )
        )
    }
    
    private fun createTutorialSteps26(){
        tutorialSteps26 = listOf(
            TutorialStep(
                getString(R.string.tutorial26_3),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial26_3
            ),
            TutorialStep(
                getString(R.string.tutorial26_2),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial26_2
            ),
            TutorialStep(
                getString(R.string.tutorial26_1),
                useTypewriterEffect = true,
                typewriterSpeed = 40L,
                soundResource = R.raw.tutorial26_1
            ),
            TutorialStep(
                getString(R.string.tutorial15_27),
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

    /**
     * Kural metnindeki `[köşeli parantez]` içini maviye boyar, parantezleri atar:
     * `"10 gider -> [4 gelir]"` → "10 gider -> 4 gelir", "4 gelir" mavi.
     *
     * Eskiden renkli harfler sıra numarasıyla veriliyordu (rulesStepTextColorPositions: 12, 14,
     * 15…). Çeviride harf sıraları değiştiği için yanlış harfler boyanırdı; işaret metnin
     * içinde durunca çevirmen neyin vurgulandığını görüp aynı kısmı işaretliyor.
     */
    private fun markedRuleText(marked: String): CharSequence {
        if (!marked.contains('[')) return marked
        val out = SpannableStringBuilder()
        var start = -1
        for (c in marked) {
            when (c) {
                '[' -> start = out.length
                ']' -> if (start >= 0) {
                    out.setSpan(ForegroundColorSpan(RULE_HIGHLIGHT_COLOR), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    start = -1
                }
                else -> out.append(c)
            }
        }
        return out
    }

    private fun updateRulesStepText(step: TutorialStep) {
        val rulesTextView = binding.rulesStepText
        val params = rulesTextView.layoutParams as ConstraintLayout.LayoutParams

        params.topToTop = ConstraintLayout.LayoutParams.UNSET
        params.topToBottom = when {
            tutorialNumber == 105 -> R.id.tenRuleTableLinearLayout
            step.rulesStepTextTopToBottomOf != null -> step.rulesStepTextTopToBottomOf
            else -> R.id.tenRuleTable
        }
        rulesTextView.layoutParams = params

        rulesTextView.text = markedRuleText(step.rulesStepTextContent.orEmpty())

        rulesTextView.visibility = step.rulesStepTextVisibility ?: View.GONE
    }

    /**
     * tutorialText'i, o an gerçekten görünen kural içeriğinin (rulesStepText görünürse onun,
     * yoksa görünen 7 tablodan hangisiyse onun) altına zincirler. Barrier bu senaryoda
     * (referans view'ların constraint'i runtime'da elle değiştiriliyor) güvenilir çalışmadığı
     * için pozisyon burada da imperatif olarak, showStep içindeki görünürlük güncellemesinden
     * SONRA hesaplanıyor.
     */
    private fun updateTutorialTextAnchor() {
        val tutorialTextParams = binding.tutorialText.layoutParams as ConstraintLayout.LayoutParams
        val visibleTableId = listOf(
            R.id.fiveRuleTable to binding.fiveRuleTable,
            R.id.extractionFiveRuleTable to binding.extractionFiveRuleTable,
            R.id.tenRuleTable to binding.tenRuleTable,
            R.id.tenRuleTableLinearLayout to binding.tenRuleTableLinearLayout,
            R.id.BeadRuleTable to binding.BeadRuleTable,
            R.id.tenRuleExtractionTableLayout to binding.tenRuleExtractionTableLayout,
            R.id.BeadRuleExtractionTable to binding.BeadRuleExtractionTable
        ).firstOrNull { (_, view) -> view.visibility == View.VISIBLE }?.first

        tutorialTextParams.topToTop = ConstraintLayout.LayoutParams.UNSET
        tutorialTextParams.topToBottom = when {
            binding.rulesStepText.visibility == View.VISIBLE -> R.id.rulesStepText
            visibleTableId != null -> visibleTableId
            else -> ConstraintLayout.LayoutParams.UNSET
        }
        ensureTutorialTextTopConstraint(tutorialTextParams)
        binding.tutorialText.layoutParams = tutorialTextParams
    }

    /**
     * tutorialText'in yüksekliği 0dp (match constraint) olduğu için üst bağı hiç boş kalmamalı;
     * aksi halde boyu belirsizleşir. Görünür kural içeriği yoksa (ya da bir adımın
     * ChangeConstraints'i üst bağı sildiyse) ekranın üstüne bağlanır - gravity=bottom sayesinde
     * metin yine alttan hizalı görünür.
     */
    private fun ensureTutorialTextTopConstraint(params: ConstraintLayout.LayoutParams) {
        if (params.topToTop == ConstraintLayout.LayoutParams.UNSET &&
            params.topToBottom == ConstraintLayout.LayoutParams.UNSET
        ) {
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
    }

    private fun applyWidgetOperations(operations: List<WidgetOperation>) {
        operations.forEach { operation ->
            when (operation) {
                is WidgetOperation.ChangeVisibility -> {
                    operation.view.visibility = operation.visibility
                }
                is WidgetOperation.ChangeMargin -> {
                    val params = operation.view.layoutParams as ViewGroup.MarginLayoutParams
                    params.rightMargin = if (operation.marginRight == 0) focusMarginRightPx(0) else operation.marginRight
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
                    if (operation.view === binding.tutorialText) {
                        ensureTutorialTextTopConstraint(params)
                    }

                    operation.view.layoutParams = params
                }
                is WidgetOperation.AnimateMargin -> {
                    val params = operation.view.layoutParams as ViewGroup.MarginLayoutParams

                    val targetRightMargin = if (operation.toMarginRight == 0) focusMarginRightPx(0) else operation.toMarginRight
                    // Sağ margin animasyonu
                    ValueAnimator.ofInt(operation.fromMarginRight, targetRightMargin).apply {
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
        val rulesStepTextVisibility: Int? = null,
        val rulesStepTextTopToBottomOf: Int? = null,
        val rulesStepTextContent: String? = null,
        val questionTextColorPositions: List<Pair<Int, Int>>? = null,
        val soundResource: Int? = null,  // Ses dosyası resource ID'si
        val soundStoragePath: String? = null,  // Gömülü değilse: Firebase Storage'daki yolu (RemoteAudioCache ile indirilip çalınır)
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
        val optionText: String? = null, // Seçenek panelinin üstünde gösterilecek açıklama metni
        /**
         * Adımın DEĞİŞMEZ kimliği (`t16_23m6_5_gelir_4un` gibi). Soru adımlarında zorunlu:
         * analytics adımı bununla tanır (bkz. [buildStepAnalyticsInfo]).
         *
         * Kurallar [LessonItem.stableId] ile aynı: bir kez verilir, metin değişse ya da adım
         * taşınsa da DEĞİŞMEZ; silinen adımın kimliği başkasına verilmez. Biçim
         * `t<öğretici no>_` ile başlar, yalnızca a-z 0-9 _ içerir, en fazla 40 karakter —
         * çeviride aynı kimlik `strings.xml` anahtarı ve ses dosyası adı olacak, Android bu
         * adlarda başka karakter kabul etmiyor. Denetim: [buildStepAnalyticsInfo].
         */
        val id: String? = null,
    )
    private fun stepAnswerAlgorithm(): Boolean {
        // Tutorial'da abaküs state'i artık ortak controller'da tutuluyor.
        val controller = abacusController ?: return false
        controlNumber = controller.getCurrentValue()
        val step = getCurrentStep()
        val correct = controlNumber == answerNumber
        logTutorialAbacusResetKontrol(step, controlNumber, correct)

        return if (correct) {
            controlNumber = 0
            true
        } else {
            controlNumber = 0
            false
        }
    }

    private fun syncTutorialBooleansFromController() {
        val value = abacusController?.getCurrentValue() ?: 0

        val tenThousands = value / 10000
        val thousands = (value / 1000) % 10
        val hundreds = (value / 100) % 10
        val tens = (value / 10) % 10
        val ones = value % 10

        val applyRod: (digit: Int, setOne: (Boolean) -> Unit, setTwo: (Boolean) -> Unit, setThree: (Boolean) -> Unit, setFour: (Boolean) -> Unit, setTopDown: (Boolean) -> Unit) -> Unit =
            { digit, setOne, setTwo, setThree, setFour, setTopDown ->
                val bottom = digit % 5
                val topDown = digit >= 5
                setOne(bottom >= 1)
                setTwo(bottom >= 2)
                setThree(bottom >= 3)
                setFour(bottom >= 4)
                setTopDown(topDown)
            }

        applyRod(
            tenThousands,
            { oneIsUp = it },
            { twoIsUp = it },
            { threeIsUp = it },
            { fourIsUp = it },
            { topIsDown = it }
        )

        applyRod(
            thousands,
            { rod1OneIsUp = it },
            { rod1TwoIsUp = it },
            { rod1ThreeIsUp = it },
            { rod1FourIsUp = it },
            { rod1TopIsDown = it }
        )

        applyRod(
            hundreds,
            { rod2OneIsUp = it },
            { rod2TwoIsUp = it },
            { rod2ThreeIsUp = it },
            { rod2FourIsUp = it },
            { rod2TopIsDown = it }
        )

        applyRod(
            tens,
            { rod3OneIsUp = it },
            { rod3TwoIsUp = it },
            { rod3ThreeIsUp = it },
            { rod3FourIsUp = it },
            { rod3TopIsDown = it }
        )

        applyRod(
            ones,
            { rod4OneIsUp = it },
            { rod4TwoIsUp = it },
            { rod4ThreeIsUp = it },
            { rod4FourIsUp = it },
            { rod4TopIsDown = it }
        )
    }

    // Options panelindeki seçimleri değerlendir
    private fun checkOptionsAnswer() {
        val step = getCurrentStep()
        val opts = step.options
        val correctIndices = step.correctOptionIndex
        if (opts.isNullOrEmpty() || correctIndices.isNullOrEmpty()) return

        val selected = optionsAdapter?.getSelectedPositions() ?: emptySet()
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.tutorial_options_pick_first, Toast.LENGTH_SHORT)
                .show()
            return
        }

        val isCorrect =
            selected.size == correctIndices.size &&
                    selected.containsAll(correctIndices)

        // Kontrol yapıldıktan sonra geri ve "eğitimi atla" tekrar aktif olsun
        optionsInteractionLocked = false
        showResultPanelForOptions(
            isCorrect = isCorrect,
            givenAnswer = describeSelectedOptions(selected, opts),
        )
    }

    /**
     * Seçilen şıkları ölçüme yazılacak tek bir metne çevirir: `"2 · Şık metni"`.
     *
     * Index metnin ÖNÜNDE duruyor: GA4 parametre değerini 100 karakterde kesiyor ve uzun bir şık
     * metni kırpılsa bile hangi şık olduğu baştaki index'ten okunabiliyor. Çoklu seçimli
     * adımlarda hepsi sıralı olarak birleştirilir (`"0,2 · ... + ..."`).
     */
    private fun describeSelectedOptions(selected: Set<Int>, options: List<String>): String {
        val ordered = selected.sorted()
        val indices = ordered.joinToString(",")
        val texts = ordered.mapNotNull { options.getOrNull(it) }.joinToString(" + ")
        return if (texts.isBlank()) indices else "$indices · $texts"
    }

    /**
     * Seçenek panelinin açılabilmesi için gereken koşul.
     *
     * `options != null` tek başına YETMEZ: doğru şık listesi de dolu olmalı ve tüm index'leri
     * seçenek listesinin sınırları içinde kalmalı. Bunlardan biri eksikse panel hiç açılmaz,
     * kontrol butonu gelmez ve [handleTutorialTapToAdvance] de `options` dolu olduğu için
     * ekrana dokunarak ilerlemeyi engeller — yani adım çıkmaza girer. Koşul iki yerden
     * (panel gösterimi ve ölçüm) okunduğu için tek bir yerde durur.
     */
    private fun TutorialStep.hasValidOptions(): Boolean {
        val opts = options ?: return false
        val correct = correctOptionIndex ?: return false
        return opts.isNotEmpty() && correct.isNotEmpty() && correct.all { it in opts.indices }
    }

    /**
     * Adım kullanıcıdan cevap bekliyorsa ölçüm türünü, beklemiyorsa `null` döner.
     *
     * Uygulamada cevap beklenen tam olarak iki adım türü var: abaküse sayı yazıp "Kontrol Et"e
     * basılanlar ve çoktan seçmeli olanlar. İkisi de [showResultPanelForOptions] üzerinden
     * doğru/yanlış panelini açar.
     */
    private fun TutorialStep.answerStepKind(): String? = when {
        abacusClickable -> AnalyticsLogger.STEP_KIND_ABACUS
        hasValidOptions() -> AnalyticsLogger.STEP_KIND_OPTIONS
        else -> null
    }

    private data class StepAnalyticsInfo(
        val key: String,
        val title: String,
        val kind: String,
        val questionText: String?,
    )

    /**
     * Soru adımlarının ölçüm kimliğini [TutorialStep.id]'den alır ve denetler;
     * [currentTutorialSteps] atandıktan hemen sonra, ilk adım gösterilmeden önce bir kez çalışır.
     *
     * Kimlik eskiden başlıktan üretiliyordu: başlıktaki bir harfi düzeltmek yeni bir satır
     * açıyor, aynı başlıklı bir soru araya eklenince sonrakilerin verisi sessizce başka adıma
     * kayıyordu. Artık kimlik elle verilir ve burada denetlenir; denetim, adım eklerken
     * akılda tutulması gereken kuralların yerini alıyor.
     *
     * Hatalı ya da eksik kimlik debug derlemesinde çökertir (geliştirici hemen görsün);
     * release'te çökmez, Crashlytics'e bildirilir ve adım eski yöntemle, başlıktan
     * üretilen yedek kimlikle ölçülür.
     */
    private fun buildStepAnalyticsInfo() {
        val occurrences = HashMap<String, Int>()
        val seenIds = HashSet<String>()
        val idPattern = Regex("t${tutorialNumber}_[a-z0-9_]+")
        val info = HashMap<Int, StepAnalyticsInfo>()
        currentTutorialSteps.forEachIndexed { index, step ->
            val kind = step.answerStepKind() ?: return@forEachIndexed
            val occurrence = (occurrences[step.text] ?: 0) + 1
            occurrences[step.text] = occurrence
            val id = step.id
            val problem = when {
                id == null -> "kimliği yok"
                !idPattern.matches(id) || id.length > 40 ->
                    "kimlik biçimi hatalı: '$id' (t${tutorialNumber}_ ile başlamalı, a-z 0-9 _, en fazla 40)"
                !seenIds.add(id) -> "kimlik bu öğreticide iki kez kullanılmış: '$id'"
                else -> null
            }
            if (problem != null) reportStepIdProblem("Tutorial $tutorialNumber, adım ${index + 1} ('${step.text}'): $problem")
            info[index] = StepAnalyticsInfo(
                key = if (problem == null) id!! else TutorialStepAnalytics.legacyStepKey(tutorialNumber, occurrence, step.text),
                title = step.text,
                kind = kind,
                questionText = step.questionText,
            )
        }
        stepAnalyticsInfo = info
    }

    private fun reportStepIdProblem(message: String) {
        if (BuildConfig.DEBUG) throw IllegalStateException("Öğretici adım kimliği: $message")
        AnalyticsLogger.recordNonFatal("Öğretici adım kimliği", IllegalStateException(message))
    }

    /** Kullanıcı bu soru adımını bu cihazda ilk kez gördüyse ölçüme işler. */
    private fun logTutorialStepReachedIfFirstTime(position: Int) {
        val info = stepAnalyticsInfo[position] ?: return
        val ctx = context ?: return
        if (!TutorialStepAnalytics.markReachedOnce(ctx, info.key)) return
        AnalyticsLogger.logTutorialStepReached(
            tutorialNumber = tutorialNumber,
            stepKey = info.key,
            stepTitle = info.title,
            stepKind = info.kind,
            lessonId = lessonItem?.stableId,
        )
    }

    /**
     * Verilen cevabı ölçüme işler.
     *
     * Kontrol butonu da şık paneli de [showResultPanelForOptions]'dan geçtiği için tek çağrı
     * yeri yeterli. Adım bir kez doğru geçildikten sonra verilen cevaplar
     * [TutorialStepAnalytics.recordAttempt] tarafından `null` ile geri çevrilir: kullanıcı geri
     * tuşuyla dönüp yeniden (bilerek yanlış da olabilir) cevaplasa bile ölçüme girmez.
     */
    private fun recordTutorialStepAnswer(isCorrect: Boolean, givenAnswer: String?) {
        val info = stepAnalyticsInfo[currentStep] ?: return
        val ctx = context ?: return
        val attemptNo = TutorialStepAnalytics.recordAttempt(ctx, info.key, isCorrect) ?: return
        AnalyticsLogger.logTutorialStepAnswer(
            tutorialNumber = tutorialNumber,
            stepKey = info.key,
            stepTitle = info.title,
            stepKind = info.kind,
            questionText = info.questionText,
            lessonId = lessonItem?.stableId,
            attemptNo = attemptNo,
            isCorrect = isCorrect,
            wrongAnswer = givenAnswer,
        )
    }

    private fun updateOptionsPanelForStep(step: TutorialStep) {
        val stepOptions = step.options

        // Her yeni adımda daha önce planlanmış panel gösterimlerini iptal et
        optionsPanelShowRunnable?.let { runnable ->
            optionsPanel.removeCallbacks(runnable)
        }
        optionsPanelShowRunnable = null

        if (!step.hasValidOptions()) {
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
                        resetOptionsSharedTextSizeToDimens()
                        optionsAdapter?.submitOptions(emptyList(), false)
                    }
                    .start()
            } else {
                optionsPanel.visibility = View.GONE
                resetOptionsSharedTextSizeToDimens()
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
            resetOptionsSharedTextSizeToDimens()
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

            // Artık yeni seçenekleri yükle — önce görünmez + yerinde ölç (metin boyutu animasyonla eş zamanlı)
            optionsAdapter?.submitOptions(stepOptions!!, step.multipleChoice!!)
            optionsPanel.visibility = View.VISIBLE
            optionsPanel.translationY = 0f
            optionsPanel.alpha = 0f

            optionsPanel.post {
                if (!isAdded) return@post
                scheduleOptionsContentTextSizeSync {
                    optionsPanel.translationY = startY
                    optionsPanel.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            }
        }
        optionsPanelShowRunnable = runnable
        optionsPanel.postDelayed(runnable, 1000L)
    }

    private fun showResultPanel() {
        // DİKKAT: değeri stepAnswerAlgorithm()'den ÖNCE oku — o fonksiyon karşılaştırmayı
        // yaptıktan sonra controlNumber'ı sıfırlıyor, sonrasında abaküse ne yazıldığı kaybolur.
        val writtenValue = abacusController?.getCurrentValue()
        val isCorrect = stepAnswerAlgorithm()
        sobiTracker.onAnswerChecked()
        showResultPanelForOptions(isCorrect = isCorrect, givenAnswer = writtenValue?.toString())
    }

    private fun resetOptionsSharedTextSizeToDimens() {
        optionsTitleText.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.tutorial_options_title_text_size)
        )
        optionsAdapter?.setSharedOptionTextSp(null)
    }

    private fun spFromDimen(@DimenRes resId: Int): Float =
        resources.getDimension(resId) / resources.displayMetrics.scaledDensity

    /** Başlık + seçenek metinleri aynı sp; liste alanına sığmazsa küçülür, fazla boşlukta dimen tabanına kadar büyür. */
    private fun scheduleOptionsContentTextSizeSync(onComplete: (() -> Unit)? = null) {
        if (!isAdded) return
        optionsContentTextSyncRetries = 0
        syncOptionsContentTextSizeStep(onComplete)
    }

    private fun syncOptionsContentTextSizeStep(onComplete: (() -> Unit)? = null) {
        if (!isAdded) return
        if (optionsPanel.visibility != View.VISIBLE) return
        val adapter = optionsAdapter ?: return
        if (adapter.itemCount == 0) {
            onComplete?.invoke()
            return
        }

        val rv = optionsRecyclerView
        if (rv.width <= 0 || rv.height <= 0) {
            if (optionsContentTextSyncRetries++ < 32) {
                rv.post { syncOptionsContentTextSizeStep(onComplete) }
            } else {
                onComplete?.invoke()
            }
            return
        }

        val minSp = spFromDimen(R.dimen.tutorial_options_auto_text_min_sp)
        val baseSp = maxOf(
            spFromDimen(R.dimen.tutorial_options_title_text_size),
            spFromDimen(R.dimen.tutorial_options_text_size)
        )

        fun applySp(sp: Float) {
            optionsTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            adapter.setSharedOptionTextSp(sp)
            adapter.notifyDataSetChanged()
        }

        fun listOverflows(): Boolean =
            rv.computeVerticalScrollRange() > rv.height + 2

        fun growLoop(curFit: Float, onDone: () -> Unit) {
            var s = curFit
            if (s >= baseSp - 0.01f) {
                onDone()
                return
            }
            fun tryGrowStep() {
                if (s >= baseSp - 0.01f) {
                    onDone()
                    return
                }
                val next = (s + 0.5f).coerceAtMost(baseSp)
                applySp(next)
                rv.post {
                    if (!isAdded) return@post
                    if (listOverflows()) {
                        applySp(s)
                        adapter.notifyDataSetChanged()
                        onDone()
                    } else {
                        s = next
                        if (s >= baseSp - 0.01f) {
                            onDone()
                        } else {
                            tryGrowStep()
                        }
                    }
                }
            }
            tryGrowStep()
        }

        fun shrinkLoop(cur: Float, onDone: () -> Unit) {
            applySp(cur)
            rv.post {
                if (!isAdded) return@post
                when {
                    !listOverflows() -> growLoop(cur.coerceAtMost(baseSp), onDone)
                    cur <= minSp -> onDone()
                    else -> shrinkLoop((cur - 0.5f).coerceAtLeast(minSp), onDone)
                }
            }
        }

        shrinkLoop(baseSp) {
            val cb = onComplete
            if (cb != null) cb.invoke()
        }
    }

    /** Seçenek sheet'i elevation ile sonuç panellerinin üstünde kalıyordu; sonuç görünmeden önce kapat. */
    private fun dismissOptionsPanelBeforeResult() {
        optionsPanelShowRunnable?.let { optionsPanel.removeCallbacks(it) }
        optionsPanelShowRunnable = null
        optionsPanel.clearAnimation()
        optionsPanel.visibility = View.GONE
        optionsPanel.alpha = 1f
        optionsPanel.translationY = 0f
        optionsTitleText.visibility = View.GONE
        optionsCheckButton.visibility = View.GONE
        resetOptionsSharedTextSizeToDimens()
        optionsAdapter?.submitOptions(emptyList(), false)
    }

    /** Sonuç paneli açıkken arkadaki abacus / root / soru sor butonuna tıklanmasın. */
    private fun enableTutorialResultTouchBlock(activePanel: View) {
        binding.overlay.apply {
            visibility = View.VISIBLE
            alpha = 0.01f
            isClickable = true
            isFocusable = true
            setOnClickListener { /* tıklamayı yut */ }
            bringToFront()
        }
        activePanel.apply {
            isClickable = true
            isFocusable = true
            bringToFront()
        }
        binding.askQuestionButton.isClickable = false
        binding.quitButton.isClickable = false
        binding.backButton.isClickable = false
        binding.skipTutorialButton.isClickable = false
        binding.devamButton.isClickable = false
        binding.abacusTapBlocker.isClickable = false
        binding.root.setOnClickListener(null)
    }

    private fun disableTutorialResultTouchBlock() {
        binding.overlay.apply {
            visibility = View.GONE
            setOnClickListener(null)
        }
        incorrectPanel.isClickable = false
        incorrectPanel.isFocusable = false
        correctPanel.isClickable = false
        correctPanel.isFocusable = false
        binding.askQuestionButton.isClickable = true
        binding.quitButton.isClickable = true
        binding.backButton.isClickable = true
        binding.skipTutorialButton.isClickable = true
        binding.devamButton.isClickable = true
        binding.abacusTapBlocker.isClickable = true
        binding.root.setOnClickListener { handleTutorialTapToAdvance() }
    }

    /**
     * @param givenAnswer Kullanıcının verdiği cevap: abaküs adımlarında abaküse yazdığı sayı,
     *   şık adımlarında seçtiği şık(lar). Yalnızca ölçüme gider, panelin görünümünü etkilemez.
     */
    private fun showResultPanelForOptions(isCorrect: Boolean, givenAnswer: String? = null) {
        recordTutorialStepAnswer(isCorrect, givenAnswer)
        dismissOptionsPanelBeforeResult()

        if (isCorrect) {
            // Doğru cevap durumu

            playCorretSound(R.raw.correct_answer_sound, volume = 0.3f)

            correctPanel.translationY = resultPanelHiddenY(correctPanel)
            correctPanel.visibility = View.VISIBLE
            correctPanel.alpha = 0f

            enableTutorialResultTouchBlock(correctPanel)

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
                            if (isTutorialResultButtonsCooldownActive()) return@setOnTouchListener true
                            com.example.app.homework.DepthPress.down(v)
                            val departedIndex = currentStep
                            currentStep++
                            logTutorialAbacusResetForwardContinue(departedIndex)
                            // #region agent log
                            AgentDebugLog.log(
                                hypothesisId = "H5",
                                location = "TutorialFragment.correctContinue",
                                message = "forward_after_correct",
                                data = mapOf(
                                    "departedIndex" to departedIndex,
                                    "landedStep" to currentStep,
                                    "tutorialNumber" to tutorialNumber,
                                    "landedClickable" to getCurrentStep().abacusClickable,
                                    "departedAnswer" to currentTutorialSteps.getOrNull(departedIndex)?.answerNumber,
                                    "landedBack" to getCurrentStep().backAnswerNumber,
                                ),
                            )
                            // #endregion

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
                                startTutorialResultButtonsCooldown()
                                return@setOnTouchListener true
                            }

                            if(getPlusIndexCurrentStep(-1).nextStepAbacusReset == true){
                                resetAbacus("CORRECT_CONTINUE_nextStepAbacusReset")
                            }
                            if(!getCurrentStep().abacusClickable){
                                getCurrentStep().onStepComplete?.invoke()
                            }
                            controlButton.visibility= View.INVISIBLE
                            backOrFront = true // İleri gidiyoruz
                            showStep(currentStep)
                            startTutorialResultButtonsCooldown()
                            true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            com.example.app.homework.DepthPress.up(v)
                            disableTutorialResultTouchBlock()
                            
                            // Control button'u tekrar aktif hale getir
                            controlButton.isClickable = true
                            controlButton.isFocusable = true
                            controlButtonListener?.let { listener ->
                                controlButton.setOnTouchListener(listener)
                            }
                            
                            correctPanel.animate()
                                .translationY(resultPanelHiddenY(correctPanel))
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    correctPanel.visibility = View.GONE
                                }
                                .start()
                            true
                        }

                        // Kayınca basılı durum ve göçme birlikte değişsin; olay burada tüketilir ki görünümün kendi
                        // onTouchEvent'i basılı durumu ayrıca bırakıp düğmeyi aşağıda unutmasın.
                        MotionEvent.ACTION_MOVE -> {
                            com.example.app.homework.DepthPress.move(v, event)
                            true
                        }

                        else -> false
                    }
                }
        } else {
            // Yanlış cevap durumu
            playCorretSound(R.raw.incorrect_answer_sound)

            incorrectPanel.translationY = resultPanelHiddenY(incorrectPanel)
            incorrectPanel.visibility = View.VISIBLE
            incorrectPanel.alpha = 0f
            incorrectPanel.findViewById<TextView>(R.id.correctAnswerText).visibility = View.GONE
            bindIncorrectPanelHelp()

            enableTutorialResultTouchBlock(incorrectPanel)
            
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

            val okayButton = incorrectPanel.findViewById<Button>(R.id.okayButton)
            okayButton.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (isTutorialResultButtonsCooldownActive()) return@setOnTouchListener true
                        com.example.app.homework.DepthPress.down(v)
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isTutorialResultButtonsCooldownActive()) return@setOnTouchListener true
                        com.example.app.homework.DepthPress.up(v)
                        dismissIncorrectResultPanel()
                        true
                    }

                    // Kayınca basılı durum ve göçme birlikte değişsin; olay burada tüketilir ki görünümün kendi
                    // onTouchEvent'i basılı durumu ayrıca bırakıp düğmeyi aşağıda unutmasın.
                    MotionEvent.ACTION_MOVE -> {
                        com.example.app.homework.DepthPress.move(v, event)
                        true
                    }

                    else -> false
                }
            }

            incorrectPanel.findViewById<ImageView>(R.id.incorrectIcon).apply {
                isClickable = true
                isFocusable = true
                setOnClickListener { dismissIncorrectResultPanel() }
            }
        }
    }

    /** Sonuç panelinin gizliyken durduğu öteleme: gezinme çubuğunun da altında. */
    private fun resultPanelHiddenY(panel: View): Float =
        navBarPanelExtension?.hiddenTranslation(panel) ?: panel.height.toFloat()

    private fun startInfoRequestBreathAnimation() {
        stopInfoRequestBreathAnimation()
        val icon = binding.infoRequest
        icon.scaleX = 1f
        icon.scaleY = 1f
        icon.imageTintList = ColorStateList.valueOf(INFO_REQUEST_TINT)
        val evaluator = ArgbEvaluator()
        infoRequestBreathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val scale = 1f + INFO_REQUEST_BREATH_SCALE_DELTA * t
                icon.scaleX = scale
                icon.scaleY = scale
                // Büyürken (t↑) renk açılır; küçülürken (t↓) normal tona döner.
                val color = evaluator.evaluate(t, INFO_REQUEST_TINT, INFO_REQUEST_TINT_LIGHT) as Int
                icon.imageTintList = ColorStateList.valueOf(color)
            }
            start()
        }
    }

    private fun stopInfoRequestBreathAnimation() {
        infoRequestBreathAnimator?.cancel()
        infoRequestBreathAnimator = null
        if (_binding == null) return
        binding.infoRequest.scaleX = 1f
        binding.infoRequest.scaleY = 1f
        binding.infoRequest.imageTintList = ColorStateList.valueOf(INFO_REQUEST_TINT)
    }

    private fun dismissIncorrectResultPanel() {
        if (isTutorialResultButtonsCooldownActive()) return
        if (incorrectPanel.visibility != View.VISIBLE) return
        sobiNudge?.hide() // "Birlikte bakalım mı?" panelin düğmesine bağlı

        stopInfoRequestBreathAnimation()

        dismissAllRequestPanels(animated = true)

        disableTutorialResultTouchBlock()

        controlButton.isClickable = true
        controlButton.isFocusable = true
        controlButtonListener?.let { listener ->
            controlButton.setOnTouchListener(listener)
        }

        incorrectButtonClick()
        incorrectPanel.animate()
            .translationY(resultPanelHiddenY(incorrectPanel))
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                incorrectPanel.visibility = View.GONE
            }
            .start()

        startTutorialResultButtonsCooldown()
    }
    private fun tutorialSkipSetup(){
        binding.skipTutorialButton.setOnClickListener {
            // Seçenek paneli açık ve kilitliyken "Eğitimi atla" çalışmasın
            if (optionsInteractionLocked && optionsPanel.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            // DİKKAT: bu bayrak "haritada bilinçli bir tutorial oturumu var" demek ve
            // burada sıfırlanmıyordu — "Eğitimi atla" ile geçilen her derste true kalıyordu.
            // O yüzden [MainActivity.isStaleTutorialGhostOverlay] hayalet TutorialFragment'ı
            // hayalet saymıyor, [MainActivity.purgeAbacusOverlayHosts] de onu kaldırmadan
            // döngüden çıkıyordu. devamButton ile aynı: tutorial oturumu burada bitiyor.
            (activity as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(false)
            val abacusFragment = AbacusFragment()
            val blindingLessonFragment = BlindingLessonFragment()
            val bundle = Bundle()
            bundle.putSerializable("lessonItem", lessonItem) // item Serializable olmalı!
            abacusFragment.arguments = bundle
            if(lessonItem!!.isBlinding == true){
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.queue_screen_in,  // Giriş animasyonu
                        R.anim.queue_screen_out  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, blindingLessonFragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.queue_screen_in,  // Giriş animasyonu
                        R.anim.queue_screen_out  // Çıkış animasyonu
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
                        R.anim.queue_screen_in,  // Giriş animasyonu
                        R.anim.queue_screen_out  // Çıkış animasyonu
                    )

                    .replace(R.id.abacusFragmentContainer, abacusFragment)  // fragment_container, ana layout'taki container ID'si
                    .commit()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.queue_screen_in,  // Giriş animasyonu
                        R.anim.queue_screen_out  // Çıkış animasyonu
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
            val step = getCurrentStep()
            val backNum = step.backAnswerNumber
            val needsReset = step.abacusReset == true
            logTutorialAbacusResetIncorrect(step, needsReset, backNum)

            if (needsReset) {
                resetAbacus("INCORRECT_RETRY_abacusReset")
            }

            fun finishIncorrectStepUi() {
                showStep(currentStep, skipAnimations = true)
                binding.devamButton.visibility = View.GONE
            }

            // reset() animasyonu sürerken writeAnswerNumber margin/translation ile çakışır;
            // önce sıfırlama bitsin, sonra hedef sayıyı yaz.
            // showStep, writeAnswerNumber bitmeden çağrılırsa setupBeads sync atlanır; 400ms sonra
            // syncStateFromUi drawable'ı ezer → aktiflik kaybı. UI'yı onComplete'te güncelle.
            if (needsReset && backNum != null) {
                postDelayedSafely(350L) {
                    writeAnswerNumber(backNum) { finishIncorrectStepUi() }
                }
            } else {
                if (backNum != null) {
                    writeAnswerNumber(backNum) { finishIncorrectStepUi() }
                } else {
                    finishIncorrectStepUi()
                }
            }

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

    private fun ensureTutorialAbacusMetricsIfVisible() {
        if (tutorialAbacusMetricsInitialized) return
        if (binding.abacusLinear.visibility != View.VISIBLE) return
        val controller = abacusController ?: return
        val scheduledAtStep = currentStep
        binding.abacusLinear.post {
            if (!isAdded || view == null) return@post
            if (tutorialAbacusMetricsInitialized) return@post
            if (binding.abacusLinear.visibility != View.VISIBLE) return@post
            val ok = controller.computeMovementDistancesFromLayout(ratio = 1.0f, force = true)
            if (ok) {
                tutorialAbacusMetricsInitialized = true
                if (!isWritingAnswerNumber) {
                    // #region agent log
                    val rod3b1 = binding.root.findViewById<ImageView>(
                        binding.root.resources.getIdentifier(
                            "rod3_bead_bottom1",
                            "id",
                            requireContext().packageName,
                        ),
                    )
                    AgentDebugLog.log(
                        hypothesisId = "H4",
                        location = "TutorialFragment.ensureTutorialAbacusMetrics",
                        message = "delayed_post_syncStateFromUi",
                        data = mapOf(
                            "scheduledAtStep" to scheduledAtStep,
                            "currentStepNow" to currentStep,
                            "staleStep" to (scheduledAtStep != currentStep),
                            "abacusClickableNow" to getCurrentStep().abacusClickable,
                            "controllerValue" to controller.getCurrentValue(),
                            "rod3b1Drawable" to TutorialBeadDiagnostics.drawableLabel(
                                requireContext(),
                                rod3b1,
                            ),
                        ),
                    )
                    // #endregion
                    controller.syncStateFromUi()
                    syncTutorialBooleansFromController()
                    // #region agent log
                    AgentDebugLog.log(
                        hypothesisId = "H4",
                        location = "TutorialFragment.ensureTutorialAbacusMetrics",
                        message = "delayed_post_sync_done",
                        data = mapOf(
                            "currentStepNow" to currentStep,
                            "rod3b1DrawableAfter" to TutorialBeadDiagnostics.drawableLabel(
                                requireContext(),
                                rod3b1,
                            ),
                        ),
                    )
                    // #endregion
                }
            }
        }
    }

    private fun setupBeads() {
        val step = getCurrentStep()
        val wouldPreserve = shouldPreserveBeadAppearanceOnSetup()
        val rod3b1 = binding.root.findViewById<ImageView>(
            binding.root.resources.getIdentifier(
                "rod3_bead_bottom1",
                "id",
                requireContext().packageName,
            ),
        )

        if (!step.abacusClickable) {
            disableAllClickable(binding.abacusLinear)
            abacusController?.setEnabled(false)
            controlButton.visibility = View.INVISIBLE
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H1",
                location = "TutorialFragment.setupBeads",
                message = "early_return_non_clickable",
                data = mapOf(
                    "currentStep" to currentStep,
                    "tutorialNumber" to tutorialNumber,
                    "wouldPreserve" to wouldPreserve,
                    "backOrFront" to backOrFront,
                    "answerNumber" to step.answerNumber,
                    "backAnswerNumber" to step.backAnswerNumber,
                    "controllerValue" to abacusController?.getCurrentValue(),
                    "tapBlockerVisible" to (binding.abacusTapBlocker.visibility == View.VISIBLE),
                    "rod3b1Drawable" to TutorialBeadDiagnostics.drawableLabel(requireContext(), rod3b1),
                    "isTutorial100OnlyBranch" to false,
                ),
            )
            // #endregion
            return
        }

        enableAllClickable(binding.abacusLinear)
        controlButton.visibility = View.VISIBLE
        binding.abacusLinear.isClickable = true
        binding.abacusLinear.isFocusable = true

        // Tap/drag etkileşimi artık ortak controller'da.
        // Öğreticide boncuk teması her zaman varsayılan soroban görünümünde kalır;
        // kullanıcının kişisel özelleştirmesi (AbacusPreferences) burada yok sayılır.
        val controller = abacusController ?: AbacusBeadController(
            requireContext(),
            binding.root,
            animationDurationMs = 300L
        ) { _, _, _, isSelected ->
            AbacusBeadRenderer.buildSorobanDrawable(
                requireContext(),
                isSelected,
                if (isSelected) AbacusPreferences.SOROBAN_SEL_DEFAULT_COLORS else AbacusPreferences.SOROBAN_DEFAULT_COLORS
            )
        }.also { created ->
            abacusController = created
            // Sobi'nin "çocuk bu adımda ne yaptı" kaydı. Cevap yazımı (geri/ileri) çocuğun hareketi değil.
            created.onValueChanged = {
                if (!isWritingAnswerNumber) sobiTracker.record(created.getCurrentValue())
            }
        }

        controller.setExternalBeadAnimationInProgress {
            currentAnimations.any { it.isAnimating() }
        }
        controller.setEnabled(step.abacusClickable && !isWritingAnswerNumber)
        controller.setup()
        ensureTutorialAbacusMetricsIfVisible()
        // writeAnswerNumber() (geri / backAnswerNumber) margin animasyonları sürerken
        // syncStateFromUi henüz güncellenmemiş margin + translationY ile yanlış sayar;
        // syncTutorialBooleansFromController() da fragment boolean'larını bu yanlış değerle ezer.
        // Sonraki writeAnswerNumber yanlış "mevcut durumdan" hareket eder → giderek bozulur.
        if (!isWritingAnswerNumber) {
            val preserveAppearance = shouldPreserveBeadAppearanceOnSetup()
            val applyBackAnswer = !preserveAppearance && shouldApplyBackAnswerFromControllerOnSetup()
            val syncPath = when {
                preserveAppearance -> "applyInternal_noAppearance"
                applyBackAnswer -> "applyBackAnswer_withAppearance"
                else -> "syncStateFromUi"
            }
            logTutorialPreserveBeadAppearance(preserveAppearance)
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H2",
                location = "TutorialFragment.setupBeads",
                message = "clickable_sync_path",
                data = mapOf(
                    "currentStep" to currentStep,
                    "tutorialNumber" to tutorialNumber,
                    "preserveAppearance" to preserveAppearance,
                    "applyBackAnswer" to applyBackAnswer,
                    "syncPath" to syncPath,
                    "controllerValue" to controller.getCurrentValue(),
                    "backAnswerNumber" to step.backAnswerNumber,
                    "rod3b1DrawableBefore" to TutorialBeadDiagnostics.drawableLabel(requireContext(), rod3b1),
                ),
            )
            when {
                preserveAppearance -> {
                    val value = controller.getCurrentValue()
                    controller.logInternalState("setupBeads BEFORE applyInternalStateFromValue")
                    controller.applyInternalStateFromValue(value, applyAppearance = false)
                    controller.logInternalState("setupBeads AFTER applyInternalStateFromValue")
                }
                applyBackAnswer -> {
                    val backNum = step.backAnswerNumber!!
                    controller.logInternalState("setupBeads BEFORE applyBackAnswer")
                    controller.applyInternalStateFromValue(backNum, applyAppearance = true)
                    controller.logInternalState("setupBeads AFTER applyBackAnswer")
                }
                else -> controller.syncStateFromUi(applyAppearance = true)
            }
            syncTutorialBooleansFromController()
            // #region agent log
            AgentDebugLog.log(
                hypothesisId = "H2",
                location = "TutorialFragment.setupBeads",
                message = "clickable_sync_done",
                data = mapOf(
                    "preserveAppearance" to preserveAppearance,
                    "rod3b1DrawableAfter" to TutorialBeadDiagnostics.drawableLabel(requireContext(), rod3b1),
                ),
            )
            // #endregion
        }

        // Legacy click-listener block'u artık kullanılmıyor.
        return

        /* Boncuklara tıklama işlemleri (legacy)
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
        */
    }

    private fun animateBeadsUp(vararg beads: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L // milisaniye cinsinden
        val moveDistance = tutorialBottomMoveDistancePx().roundToInt()

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
        val moveDistance = tutorialTopMoveDistancePx().roundToInt()

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
        val moveDistance = tutorialBottomMoveDistancePx().roundToInt()

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

    private fun resetAbacus(reason: String) {
        logTutorialAbacusResetInvoke(reason)
        // Ekrana tıklanmasını engellemek için overlay'i görünür yap
        binding.overlay.visibility = View.VISIBLE
        binding.overlay.alpha = 0.01f // Neredeyse görünmez ama tıklanabilir
        binding.overlay.isClickable = true
        binding.overlay.isFocusable = true

        // 0.4 saniye sonra overlay'i kapat.
        // (Eskiden korumasızdı: view yok edildikten sonra tetiklenirse ölü bir view'a yazıyordu.)
        postDelayedSafely(400) {
            binding.overlay.visibility = View.GONE
            binding.overlay.isClickable = false
            binding.overlay.isFocusable = false
        }

        // Controller üzerinden abaküsü sıfırla (animasyonlu)
        abacusController?.reset(animate = true)
        syncTutorialBooleansFromController()
        if (TutorialAbacusResetDiagnostics.ENABLED) {
            val duration = abacusController?.let { 350L } ?: 0L
            binding.root.postDelayed({
                if (!isAdded) return@postDelayed
                TutorialAbacusResetDiagnostics.log(
                    "resetAbacus DONE reason=$reason controllerValue=${abacusController?.getCurrentValue()}",
                )
            }, duration)
        }
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
    override fun onPause() {
        stopLearningSessionTracking()
        super.onPause()
        // Uygulama arka plana geçince sesi duraklat (geri dönünce kaldığı yerden devam edebilsin)
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                wasPausedByLifecycle = true
            }
        } catch (_: Exception) { }
        // Typewriter'ı duraklat (geri dönünce kaldığı yerden devam edebilsin)
        if (typewriterRunnable != null || typewriterSetupRunnable != null) {
            typewriterRunnable?.let { binding.tutorialText.removeCallbacks(it) }
            typewriterRunnable = null
            typewriterSetupRunnable?.let { binding.tutorialText.removeCallbacks(it) }
            typewriterSetupRunnable = null
            typewriterPausedByLifecycle = true
        }
    }

    override fun onResume() {
        super.onResume()
        startLearningSessionTracking()
        // Geri dönünce ses kaldığı yerden devam etsin
        if (wasPausedByLifecycle && mediaPlayer != null) {
            try {
                mediaPlayer?.start()
            } catch (_: Exception) { }
            wasPausedByLifecycle = false
        }
        // Geri dönünce typewriter kaldığı yerden devam etsin
        if (typewriterPausedByLifecycle && typewriterText != null) {
            resumeTypewriter()
        }
    }

    override fun onDestroyView() {
        cancelPendingBackShowStep()
        stopInfoRequestBreathAnimation()
        sobiPlayer?.cancel()
        sobiPlayer = null
        sobiSheet?.dismiss()
        sobiSheet = null
        sobiFloatingButton = null
        _binding?.root?.removeCallbacks(sobiIdleNudgeRunnable)
        sobiNudge?.hide()
        sobiNudge = null
        if (_binding != null) {
            dismissRequestPanelsImmediate()
        }
        stopLearningSessionTracking()
        navBarPanelExtension?.detach()
        navBarPanelExtension = null
        super.onDestroyView()
        // Bellek sızıntısı olmaması için bırak
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding?.let { b ->
            typewriterRunnable?.let { b.tutorialText.removeCallbacks(it) }
            typewriterSetupRunnable?.let { b.tutorialText.removeCallbacks(it) }
        }
        typewriterRunnable = null
        typewriterSetupRunnable = null
        typewriterText = null
        typewriterPausedByLifecycle = false
        // View hiyerarşisini bırak: fragment geri yığınında beklerken tüm view ağacının
        // bellekte kalmasına yol açıyordu.
        _binding = null
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
    private fun playCorretSound(soundResId: Int, volume: Float = 1f) {
        val prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return
        mediaPlayer?.release() // Önceki sesi serbest bırak
        mediaPlayer = MediaPlayer.create(requireContext(), soundResId)
        mediaPlayer?.setVolume(volume, volume)
        mediaPlayer?.start()
    }
    // Ses çalma fonksiyonu. soundResource: gömülü (res/raw) ses. soundStoragePath doluysa ve
    // soundResource yoksa: adımda soundStoragePath ile verilmiş, Firebase Storage'dan RemoteAudioCache
    // tarafından indirilmiş sesi çalar. Dosya henüz inmemişse (ilk sefer/internet yok) sessizce
    // atlanır ve indirme tekrar tetiklenir - hiçbir zaman çökme veya bekleme olmaz.
    private fun playSound(soundResource: Int?, soundStoragePath: String? = null) {
        val prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("tutorial_sound_enabled", true)) return
        if (soundResource == null && soundStoragePath == null) return
        try {
            // Önceki ses varsa durdur
            mediaPlayer?.release()

            if (soundResource != null) {
                // MediaPlayer.create() zaten hazırlanmış (prepared) döner.
                mediaPlayer = MediaPlayer.create(requireContext(), soundResource)
            } else {
                val localFile = RemoteAudioCache.localFile(requireContext(), soundStoragePath!!)
                if (!localFile.exists()) {
                    Log.w("TutorialFragment", "Uzak ses henüz inmedi, atlanıyor: $soundStoragePath")
                    RemoteAudioCache.prefetch(requireContext(), soundStoragePath)
                    mediaPlayer = null
                } else {
                    // Yerel dosyadan manuel oluşturma senkron prepare() ister.
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(localFile.absolutePath)
                        prepare()
                    }
                }
            }
            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("TutorialFragment", "Ses çalma hatası: ${e.message}")
        }
    }


    // Typewriter effect fonksiyonu (durum sınıf değişkenlerinde tutulur; arka plana geçince duraklatılıp geri dönünce devam edebilir)
    private fun showTextWithTypewriter(text: String, textView: TextView, speed: Long) {
        // Hem harf-yazma döngüsünü hem de aşağıdaki kurulum bloğunu iptal et - hızlı art arda
        // adım geçişlerinde önceki çağrıdan kalan hiçbir şey çalışmaya devam etmesin.
        typewriterRunnable?.let { textView.removeCallbacks(it) }
        typewriterSetupRunnable?.let { textView.removeCallbacks(it) }
        typewriterText = text
        typewriterCurrentIndex = 0
        typewriterSpeed = speed
        typewriterPausedByLifecycle = false

        textView.visibility = View.INVISIBLE
        setTypewriterProgress(textView, text, 0)
        typewriterSetupRunnable = Runnable {
            typewriterSetupRunnable = null
            typewriterRunnable = object : Runnable {
                override fun run() {
                    if (typewriterCurrentIndex < (typewriterText?.length ?: 0)) {
                        textView.visibility = View.VISIBLE
                        setTypewriterProgress(textView, typewriterText!!, typewriterCurrentIndex + 1)
                        typewriterCurrentIndex++
                        textView.postDelayed(this, typewriterSpeed)
                    } else {
                        typewriterRunnable = null
                    }
                }
            }
            textView.post(typewriterRunnable!!)
        }
        textView.post(typewriterSetupRunnable!!)
    }

    private fun resumeTypewriter() {
        val text = typewriterText ?: return
        val tv = binding.tutorialText
        if (typewriterCurrentIndex >= text.length) {
            typewriterPausedByLifecycle = false
            typewriterRunnable = null
            return
        }
        tv.visibility = View.VISIBLE
        setTypewriterProgress(tv, text, typewriterCurrentIndex)
        typewriterRunnable = object : Runnable {
            override fun run() {
                if (typewriterCurrentIndex < (typewriterText?.length ?: 0)) {
                    setTypewriterProgress(tv, typewriterText!!, typewriterCurrentIndex + 1)
                    typewriterCurrentIndex++
                    tv.postDelayed(this, typewriterSpeed)
                } else {
                    typewriterRunnable = null
                }
            }
        }
        tv.post(typewriterRunnable!!)
        typewriterPausedByLifecycle = false
    }

    /**
     * Daktilo efektinde metin harf harf eklenmez; TAM metin baştan yazılır ve henüz gösterilmeyen
     * kısım saydam boyanır. Böylece tutorialText'in autoSize'ı yazı boyutunu ilk karede tam metne
     * göre bir kez belirler (harf eklendikçe yazı büyüyüp küçülmez) ve satır kırılımları da
     * yazma sırasında kaymaz.
     */
    private fun setTypewriterProgress(textView: TextView, fullText: String, visibleCount: Int) {
        val spannable = SpannableString(fullText)
        if (visibleCount < fullText.length) {
            spannable.setSpan(
                ForegroundColorSpan(Color.TRANSPARENT),
                visibleCount.coerceAtLeast(0),
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = spannable
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
        typewriterSetupRunnable?.let {
            binding.tutorialText.removeCallbacks(it)
        }
        typewriterSetupRunnable = null
    }
} 