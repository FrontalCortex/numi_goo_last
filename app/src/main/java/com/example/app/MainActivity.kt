package com.example.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.android.gms.ads.MobileAds
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityMainBinding
import com.example.app.model.LessonItem
import com.example.app.model.QuestionMessage
import com.example.app.model.StudentQuestion
import java.io.File
import java.io.FileOutputStream
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    fun checkAndShowInterstitialAdIfAllowed(logContext: String, onDone: () -> Unit = {}) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            Log.d("AdDiag", "Skipping Ad on $logContext: User is not logged in")
            onDone()
            return
        }

        Log.d("AdDiag", "$logContext called. shouldShowAdOnReturn=${GlobalValues.shouldShowAdOnReturn}")
        if (GlobalValues.shouldShowAdOnReturn) {
            GlobalValues.shouldShowAdOnReturn = false
            if (!isInfiniteEnergy() && !GlobalValues.isTeacherApproved) {
                val now = System.currentTimeMillis()
                // Reklam gösterim aralığı: 5 dakika (5 * 60 * 1000 ms)
                if (now - GlobalValues.lastInterstitialAdShownTime >= 5 * 60 * 1000L) {
                    GlobalLessonData.loadLessonItemsForPart(this, 1) { items ->
                        val chests = items.filter { it.type == com.example.app.model.LessonItem.TYPE_CHEST }
                        if (chests.size >= 2 && chests[1].stepIsFinish) {
                            GlobalValues.lastInterstitialAdShownTime = System.currentTimeMillis()
                            Log.d("AdDiag", "Showing Interstitial Ad on $logContext")
                            val shouldShowSkip = GlobalValues.pendingCupPathRevealPartId == null
                            adManager.showInterstitialAd(this, shouldShowSkip) {
                                onDone()
                            }
                        } else {
                            Log.d("AdDiag", "Skipping Ad on $logContext: 2nd chest in part 1 is not finished")
                            onDone()
                        }
                    }
                    return
                } else {
                    Log.d("AdDiag", "Skipping Interstitial Ad on $logContext due to 40 sec cooldown")
                }
            } else {
                Log.d("AdDiag", "Skipping Interstitial Ad on $logContext because user is Premium/Pro or Teacher Approved")
            }
        }
        onDone()
    }


    companion object {
        /**
         * Hedef kutlaması şeridinin ekranda kalma süresi.
         *
         * Okunacak kadar uzun, yolu kapatacak kadar değil; dokunulduğunda zaten seri
         * ekranına gidiyor.
         */
        private const val STREAK_CELEBRATION_MS = 4000L

        /** Kapı kapalıyken iki deneme arası. */
        private const val STREAK_CELEBRATION_RETRY_MS = 400L
        private const val TAG_QUEUE = "PostLessonQueue"

        /** Kuyruk tıkalıyken ne sıklıkta yeniden denenecek. */
        private const val QUEUE_WATCHDOG_INTERVAL_MS = 1_000L

        /**
         * Bekçinin toplam süresi. Cömert: kullanıcı rating dialog'unu ya da rozet
         * kutlamasını bir dakika açık bırakabilir ve bu bir tıkanma değil.
         */
        private const val QUEUE_WATCHDOG_BUDGET_MS = 120_000L

        /**
         * Yeniden denemelerin toplam süresi.
         *
         * Ders dönüşünde katmanların kapanması saniyeler sürebiliyor (reklam, sandık
         * animasyonu, rozet kutlaması). Bu süre dolduğunda vazgeçiliyor: kutlama diskte
         * duruyor ve bir sonraki doğal tetiklemede (ekran dönüşü, uygulama öne gelmesi)
         * yine denenecek.
         */
        private const val STREAK_CELEBRATION_RETRY_BUDGET_MS = 20_000L

        const val EXTRA_FROM_LOGIN = "from_login"
        const val EXTRA_START_DESTINATION = "start_destination"
        const val START_DESTINATION_MAP = "map"
        const val START_DESTINATION_TUTORIAL = "tutorial"
        /** Set by FCM notification tap; open this question chat when activity is ready. */
        const val EXTRA_OPEN_QUESTION_ID = "open_question_id"
        const val EXTRA_NOTIFICATION_RECIPIENT_UID = "notification_recipient_uid"

        @Volatile
        var currentActivity: MainActivity? = null
        private const val PREFS_APP = "AppPrefs"
        private const val KEY_NOTIF_PERMISSION_PROMPTED = "notif_permission_prompted"
        const val PRACTICE_TOUCH_BLOCKER_TAG = "practice_touch_blocker"
        const val LESSON_ACTION_TOUCH_BLOCKER_TAG = "lesson_action_touch_blocker"
        const val FIRST_TUTORIAL_LOG_TAG = "FirstTutorialDbg"
        /** Harita dokunma kilidi teşhisi — `adb logcat -s MapTouchDbg` */
        const val MAP_TOUCH_DIAG_LOG_TAG = MapTouchDiagnostics.LOG_TAG
        /** Görevler pratik / günlük soru overlay kapanışı — [finishOverlayReturnToTasks] ile temizlenir. */
        const val ABACUS_OVERLAY_BACK_STACK = "abacus_overlay"
        /** Kaçıncı LESSON dönüşünde AskQuestionOpen promosu gösterilir. */
        private const val ASK_QUESTION_PROMO_LESSON_RETURN_THRESHOLD = 3
        /** Harita temizlenene kadar promo denemesi bu aralıkla tekrarlanır. */
        private const val ASK_QUESTION_PROMO_RETRY_INTERVAL_MS = 100L
        private const val ASK_QUESTION_PROMO_MAX_ATTEMPTS = 40
        /** Promo hiç gösterilemezse harita kalıcı kilitli kalmasın diye son güvenlik ağı. */
        private const val ASK_QUESTION_PROMO_LOCK_WATCHDOG_MS = 15_000L
    }

    internal fun buildTouchDiagSnapshot(): String {
        if (!::binding.isInitialized) return "binding=false"
        val fm = supportFragmentManager
        val base = fm.findFragmentById(R.id.fragmentContainerID)?.javaClass?.simpleName ?: "null"
        val abacusFrag = fm.findFragmentById(R.id.abacusFragmentContainer)?.javaClass?.simpleName ?: "null"
        val abacusVis = when (binding.abacusFragmentContainer.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.GONE -> "GONE"
            else -> binding.abacusFragmentContainer.visibility.toString()
        }
        return buildString {
            append("base=").append(base)
            append(" abacusFrag=").append(abacusFrag)
            append(" abacusVis=").append(abacusVis)
            append(" forceDismiss=").append(forcingAbacusOverlayDismissForSeasonGate)
            append(" lessonSheetDepth=").append(lessonSheetOverlayNavigationDepth)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        MainActivityTouchDiag.reportTouchDownIfSuspicious(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun logFirstTutorial(event: String, details: String = "") {
        val msg = if (details.isEmpty()) event else "$event | $details"
        Log.d(FIRST_TUTORIAL_LOG_TAG, msg)
    }

    private fun overlaySnapshot(caller: String): String {
        if (!::binding.isInitialized) return "caller=$caller binding=false"
        val fm = supportFragmentManager
        val base = fm.findFragmentById(R.id.fragmentContainerID)?.javaClass?.simpleName ?: "null"
        val abacusFrag = fm.findFragmentById(R.id.abacusFragmentContainer)?.javaClass?.simpleName ?: "null"
        val abacusVis = when (binding.abacusFragmentContainer.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.GONE -> "GONE"
            else -> binding.abacusFragmentContainer.visibility.toString()
        }
        return buildString {
            append("caller=").append(caller)
            append(" dest=").append(intent?.getStringExtra(EXTRA_START_DESTINATION) ?: "null")
            append(" fromLogin=").append(intent?.getBooleanExtra(EXTRA_FROM_LOGIN, false) == true)
            append(" auth=").append(auth.currentUser?.uid?.take(8) ?: "null")
            append(" hadAccount=").append(DeviceAccountStore.hasEverHadAccount(this@MainActivity))
            append(" base=").append(base)
            append(" abacusFrag=").append(abacusFrag)
            append(" abacusVis=").append(abacusVis)
            append(" backStack=").append(fm.backStackEntryCount)
            append(" lessonSheetDepth=").append(lessonSheetOverlayNavigationDepth)
            append(" forceDismiss=").append(forcingAbacusOverlayDismissForSeasonGate)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var coin:TextView
    private lateinit var energyManager: EnergyManager
    internal lateinit var adManager: AdManager
    internal lateinit var billingManager: BillingManager
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val authManager by lazy { AuthManager().also { it.initialize(this) } }

    private var lastBackPressTime = 0L
    private val backPressToExitMillis = 2000L
    private var openedFromChatNotification: Boolean = false
    
    private val subscriptionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Plan değişti, enerji gösterimini güncelle
            checkSubscriptionAndUpdateEnergy()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        notificationPermissionRequestInFlight = false
        getSharedPreferences(PREFS_APP, MODE_PRIVATE).edit()
            .putBoolean(KEY_NOTIF_PERMISSION_PROMPTED, true)
            .apply()
    }

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchMediaProjectionForQuestion() else {
            isQuestionRecordingInProgress = false
            setAskQuestionButtonEnabled(true)
            Toast.makeText(this, "Ses kaydı için izin gerekli.", Toast.LENGTH_SHORT).show()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            isQuestionRecordingInProgress = false
            setAskQuestionButtonEnabled(true)
            Toast.makeText(this, "Ekran kaydı başlatılamadı.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        maxRecordingSecForSession = if (authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER) 180 else 60
        val serviceIntent = Intent(this, ScreenRecordingService::class.java).apply {
            putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, result.resultCode)
            putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, result.data)
            putExtra(ScreenRecordingService.EXTRA_MAX_DURATION_MS, maxRecordingSecForSession * 1000L)
        }
        // Alıcı ve zaman aşımı servisten ÖNCE kurulmalı: servis ilk saniyede patlarsa
        // gönderdiği RECORDING_FAILED yayınını kaçırmayalım (yoksa panel sessizce açık kalıyor).
        registerRecordingReceiver()
        armRecordingStartTimeout()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        showRecordingOverlay()
    }

    private var recordingOverlayView: View? = null
    private var drawingOverlayView: DrawingOverlayView? = null
    private var drawingControlsView: View? = null
    private var isDrawingPanelOpen: Boolean = false
    private var recordingTimerRunnable: Runnable? = null
    private var recordingReceiver: BroadcastReceiver? = null
    private val recordingHandler = Handler(Looper.getMainLooper())
    // Öğretmen soru medyasını mevcut sahiplendiği soruya göndermek için geçici state
    private var teacherPendingMediaType: String? = null
    private var teacherPendingMediaPath: String? = null
    private var teacherPendingDescription: String? = null
    // Seçim/gönder barı açılmadan önce abacusFragmentContainer görünür müydü? (iptalde geri yüklemek için)
    private var teacherPendingAbacusContainerWasVisible: Boolean = false
    private var teacherSelectedQuestionId: String? = null
    private var teacherSelectedQuestionTitle: String? = null
    private var notificationPermissionRequestInFlight = false

    private var seasonLeaderboardPendingListener: ListenerRegistration? = null
    private var walletListenerRegistration: ListenerRegistration? = null
    private var seasonLeaderboardGateRetryLifecycleCallbacks: FragmentManager.FragmentLifecycleCallbacks? = null

    /**
     * Quit / ChestResult sonrası overlay kapanırken true: [reconcileAbacusOverlayWhenMapIsBase] host VISIBLE
     * olsa bile pop/remove yapar (aksi halde back stack Abacus geri gelir, kapı bloklanır).
     * Lesson bottom sheet ile ders açılırken false kalır → VISIBLE iken reconcile temizlik yapmaz.
     */
    private var forcingAbacusOverlayDismissForSeasonGate = false
    private var practiceOverlayDismissRunnable: Runnable? = null
    private val practiceOverlayExitAnimMs = 320L

    /** [scheduleSeasonGateAfterAbacusOverlayDismissed] birleştirme; lesson sheet overlay açılmadan önce iptal edilir. */
    private var pendingSeasonGateReconcileRunnable: Runnable? = null

    /** Reklam gösterilirken/sonrasında açılacak rozet payloads (BadgeLevelUpPayload listesi). */
    private var pendingBadgePayloadsForAd: List<com.example.app.BadgeLevelUpPayload> = emptyList()
    /**
     * Türü LESSON olan bir item'dan harita dönüşü başladı mı? Rozet payload'larıyla aynı
     * "bekleyen" deseni: reconcile yolu notifyMapVisibleAfterLessonClaim'i parametresiz
     * çağırıp adCheckForBadgeInProgress'i true yapabildiği için, isLessonTypeReturn bilgisi
     * doğrudan parametreyle taşındığında kaybolabiliyor. Burada biriktirilip onDone'da tüketilir.
     */
    private var pendingLessonTypeReturnForPromo = false
    /**
     * AskQuestionOpen promosunun bu dönüşte gösterileceği belli, ama içerik henüz ekranda değil
     * (reklam kontrolü + harita temizlenme beklemesi). Rozet/rehberdeki desenin aynısı: bayrak
     * açıkken harita kilitli tutulur ve [MapFragment.enableMapTouchRouting] kilidi açmaz.
     */
    private var askQuestionPromoPendingLock = false
    /** Reklam gösterilirken/sonrasında açılacak rozet payloads (String listesi). */
    private var pendingBadgeStringPayloadsForAd: List<String> = emptyList()
    /** Reklam kontrolü zaten uçuştayken ikinci çağrının onDone'ını tetiklemesini önler. */
    private var adCheckForBadgeInProgress = false
    /** Sandık türü ders ilk kez tamamlandığında rating dialog tetiklenmesi için bayrak. */
    var justFinishedChestForRating = false

    /**
     * [prepareMapReturnAfterLessonClaim] claim yolunda set edilir;
     * ChestResult/ChestFragment destroy lifecycle reconcile'ı doğru sırayla tetikler.
     */
    private var claimPathScheduledSeasonGate = false

    /** Bottom sheet → Record/Abacus commit sırasında reconcile overlay'i silmesin. */
    private var lessonSheetOverlayNavigationDepth = 0

    /**
     * [renderFirstTutorial] Map + Tutorial commitleri arasında back stack listener
     * overlay'i henüz görmeden [restoreMapUiAfterLessonOverlayDismiss] çağırmasın.
     */
    private var firstTutorialOverlayBootstrapActive = false

    /**
     * Harita üzerinden ders bottom sheet ile açılan tutorial (kullanıcı bilinçli izliyor).
     * false iken [TutorialFragment] harita tabanında hayalet kalırsa purge/reconcile ile silinir.
     */
    private var activeMapTutorialOverlayFromLesson = false

    /** Aynı karede birden fazla [tryShowSeasonLeaderboardRewardGateIfNeeded] → tek replace/commit. */
    private val seasonLeaderboardGateCommitRunnable = Runnable {
        commitSeasonLeaderboardRewardGateIfNeededNow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("ScreenDebug", "screenHeightDp=${resources.configuration.screenHeightDp} screenWidthDp=${resources.configuration.screenWidthDp}")

        // adManager'ı burada init et; AgeConsentManager'ın asenkron callback'i
        // herhangi bir anda tetiklenebileceğinden lateinit crash riskine karşı önce atanmalı.
        adManager = AdManager(this)

        // Play Billing: uygulama açılışında bağlan ve Play'de duran, sunucuya işlenmemiş
        // satın almaları yeniden gönder (ödeme sonrası çökme senaryosunu kurtarır).
        billingManager = BillingManager(this)
        installDefaultBillingCallbacks()
        billingManager.start()

        // Yaşa ve ülkeye göre reklam muamelesi (TFAT) altyapısıyla AdMob'u başlat.
        // adManager preload işlemleri bu callback'in içinde yapılır;
        // böylece Firestore'dan birthYear gelmeden reklam isteği atılmaz (Claude planı).
        AgeConsentManager.initializeAdMobWithAgeConsent(this) {
            adManager.preloadAd()
            adManager.preloadInterstitialAd()
        }
        
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer) == null) {
                binding.createQuestionOverlayContainer.visibility = View.GONE
                binding.root.post {
                    if (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) is MapFragment) {
                        requestSeasonLeaderboardRewardGateIfPending()
                    }
                }
            }
        }

        Log.d("MainActivity", "onCreate intent extras = ${intent?.extras}")

        // Klavye açıldığında bottomNavigationID gizlensin; soru seçilirken (öğretmen medya gönder) de gizli kalsın
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationID) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
            val coordinator = findViewById<CoordinatorLayout>(R.id.coordinator_layout)
            val isRacePanelOpen = coordinator?.findViewWithTag<View>("race_panel") != null
            view.visibility = when {
                current is AbacusPracticeFragment -> View.GONE
                current is NewChestFragment -> View.GONE
                imeVisible -> View.GONE
                teacherPendingMediaPath != null -> View.GONE
                isRacePanelOpen -> View.GONE
                else -> View.VISIBLE
            }
            insets
        }

        supportFragmentManager.addOnBackStackChangedListener {
            // Re-evaluate bottom nav visibility with current fragment state.
            binding.bottomNavigationID.requestApplyInsets()
            updateCurrencyPanelVisibility()
            // Seri ekranında hedef değiştirilmiş olabilir: hedef düşünce bugün zaten
            // tutturulmuş sayılabiliyor ve alev orada yanıyor. Üst bar geri dönüşte
            // güncel olmalı; activity onResume'a girmediği için tek yer burası.
            refreshStreakUi()

            // FM transaction ortasında çağrılır — restore'u bir sonraki kareye ertele.
            val topOverlay = supportFragmentManager.findFragmentById(R.id.abacusFragmentContainer)
            if (topOverlay == null) {
                val baseFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
                if (baseFragment is MapFragment) {
                    if (firstTutorialOverlayBootstrapActive) {
                        logFirstTutorial(
                            "backStackChanged.skip restoreMapUi",
                            "firstTutorialOverlayBootstrapActive " +
                                overlaySnapshot("backStackChanged.skip"),
                        )
                    } else {
                        logFirstTutorial(
                            "backStackChanged->restoreMapUi SCHEDULED",
                            overlaySnapshot("backStackChanged.topOverlayNull"),
                        )
                        binding.root.post {
                            logFirstTutorial(
                                "backStackChanged->restoreMapUi RUN",
                                overlaySnapshot("backStackChanged.post"),
                            )
                            restoreMapUiAfterLessonOverlayDismiss()
                        }
                    }
                } else if (baseFragment != null && baseFragment.isHidden) {
                    binding.root.post {
                        if (baseFragment.isAdded && baseFragment.isHidden) {
                            supportFragmentManager.beginTransaction()
                                .show(baseFragment)
                                .commitAllowingStateLoss()
                        }
                    }
                }
            }
            binding.root.post { tryShowSeasonLeaderboardRewardGateIfNeeded() }
        }
        
        // Bildirimde gelen soru ID'si var mı? (sohbete deep-link)
        openedFromChatNotification =
            intent?.getStringExtra(EXTRA_OPEN_QUESTION_ID)?.isNullOrEmpty() == false

        // Bildirimde gelen soru ID'si var mı? (sohbete deep-link)
        openedFromChatNotification =
            intent?.getStringExtra(EXTRA_OPEN_QUESTION_ID)?.isNullOrEmpty() == false

        // Giriş/kayıt sonrası gelindiyse oturumu temizleme (ProfileFragment vb. güncel kullanıcıyı gösterebilsin)
        val fromLogin = intent?.getBooleanExtra(EXTRA_FROM_LOGIN, false) == true
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val loginStartEverShown = prefs.getBoolean("login_start_ever_shown", false)
        val hasExistingLogin = auth.currentUser != null
        logFirstTutorial(
            "onCreate.enter",
            overlaySnapshot("onCreate") +
                " loginStartEverShown=$loginStartEverShown online=${isOnline()} " +
                "notifOpen=$openedFromChatNotification",
        )
        if (loginStartEverShown && !hasExistingLogin) {
            logFirstTutorial("onCreate.redirect", "LoginStartActivity (login_start_ever_shown)")
            startActivity(Intent(this, LoginStartActivity::class.java))
            finish()
            return
        }
        if (!fromLogin) {
            logFirstTutorial("onCreate.deleteAllLessonItems", "before clear")
            deleteAllLessonItems(this)
            logFirstTutorial(
                "onCreate.deleteAllLessonItems",
                "after clear lessonItems=${GlobalLessonData.lessonItems.size} " +
                    "account_ever_created=${DeviceAccountStore.hasEverHadAccount(this)}",
            )
        }
        coin = binding.currencyText
        binding.currencyText.text = UserWalletFirestore.getCachedCurrency(this).toString()
        binding.keyText.text = UserWalletFirestore.getCachedKeys(this).toString()
        auth.currentUser?.uid?.let { refreshWalletFromFirestore() }

        // Öğretmen hesabında currencyPanel içindeki diamond/coin, anahtar ve enerji ikonlarını gizle
        if (authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER) {
            binding.diamondID.visibility = View.GONE
            binding.currencyText.visibility = View.GONE
            binding.keyIcon.visibility = View.GONE
            binding.keyText.visibility = View.GONE
            binding.energyIcon.visibility = View.GONE
            binding.energyText.visibility = View.GONE
        }
        
        // Enerji sistemini başlat — callback ayarlandığında zaten ilk değeri çeker
        energyManager = EnergyManager(this)
        energyManager.setEnergyUpdateCallback { energy ->
            updateEnergyDisplay(energy)
        }
        
        // Süre takibini initialize et (onResume'da başlatılacak)
        TimeTracker.initialize(this)
        
        // Abonelik durumunu kontrol et ve enerji gösterimini güncelle
        checkSubscriptionAndUpdateEnergy()

        // "Bu cihaz hoş geldin kredisini alabilir mi" cevabını tazele; promo kapısı bu
        // önbelleğe bakıyor (bkz. WelcomeCreditEligibility).
        WelcomeCreditEligibility.refresh(applicationContext)

        // Girişli kullanıcı: tüm part'larda eksik lessonProgress dokümanlarını arka planda doldur (idempotent).
        if (auth.currentUser != null) {
            GlobalLessonData.seedAllLessonProgressIfMissing(applicationContext)
        }
        
        // Eğer uygulama çevrimdışıysa, doğrudan offline fragment'ı göster.
        if (!isOnline()) {
            logFirstTutorial("onCreate.route", "offline -> OfflineFragment")
            showOfflineFragment()
        } else if (!openedFromChatNotification) {
            val preparedDestination = intent?.getStringExtra(EXTRA_START_DESTINATION)
            when {
                preparedDestination == START_DESTINATION_MAP -> {
                    logFirstTutorial("onCreate.route", "prepared=MAP -> MapFragment only")
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragmentContainerID, PartSelectionFragment())
                        addToBackStack(null)
                        commit()
                    }
                }
                preparedDestination == START_DESTINATION_TUTORIAL -> {
                    logFirstTutorial("onCreate.route", "prepared=TUTORIAL -> showFirstTutorialFromPreparedDataOrLoad")
                    showFirstTutorialFromPreparedDataOrLoad()
                }
                else -> {
                    logFirstTutorial("onCreate.route", "prepared=null -> fallback auth check")
                    // Tek şart: bu cihazda daha önce hesap açıldı mı. Girişli kullanıcıda
                    // zaten true döner, yani ayrı bir dal gerekmiyor.
                    val hadAccount = DeviceAccountStore.hasEverHadAccount(this)
                    if (hadAccount) {
                        logFirstTutorial("onCreate.route", "account_ever_created=true -> Map only")
                        supportFragmentManager.beginTransaction().apply {
                            replace(R.id.fragmentContainerID, PartSelectionFragment())
                            addToBackStack(null)
                            commit()
                        }
                    } else {
                        logFirstTutorial("onCreate.route", "account_ever_created=false -> showFirstTutorial")
                        showFirstTutorial()
                    }
                }
            }
        } else {
            logFirstTutorial("onCreate.route", "skipped startup routing (chat notification)")
        }
        binding.fragmentContainerID.post {
            Log.d("MainActivity", "post -> handleOpenQuestionIdFromIntent() called")
            handleOpenQuestionIdFromIntent()
        }
        // Sistem çubuğu renklerini message_topbar ile eşitle
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background_color)
        binding.bottomNavigationID.apply {
            itemIconTintList = null
            elevation = 0f
            // Material style overlay/tint kaynaklı açılmayı engelle
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.background_color))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@MainActivity, R.color.background_color)
            )
            itemRippleColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        }

        // Geri tuşu: Sadece kökte (geri gidilecek ekran yokken) çift basınca çıkış; yoksa bir önceki ekrana dön
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.findFragmentByTag(SeasonLeaderboardRewardGateFragment.TAG) != null) {
                    return
                }
                // 1) CreateQuestion akışındayken geri tuşu:
                //    - Eğer öğretmen seçim akışından dönülmüş CreateQuestion ise:
                //      onTeacherCreateQuestionDismissedByBack() + fragment'i gerçekten kapat.
                //    - Diğer CreateQuestion durumlarında sadece fragment'i kapat (backButton ile aynı).
                val createQuestionOverlay = supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer) as? CreateQuestionFragment
                val createQuestionAbacus = supportFragmentManager.findFragmentById(R.id.abacusFragmentContainer) as? CreateQuestionFragment
                if (createQuestionOverlay != null || createQuestionAbacus != null) {
                    val fragment = createQuestionOverlay ?: createQuestionAbacus
                    if (fragment?.isStudentSendingInProgress() == true) {
                        return
                    }
                    val fromTeacherSelectionBack =
                        fragment?.arguments?.getBoolean(CreateQuestionFragment.ARG_FROM_TEACHER_SELECTION_BACK, false) == true
                    if (fromTeacherSelectionBack) {
                        // Öğretmen seçim akışından dönüyorsa özel temizliği yap
                        onTeacherCreateQuestionDismissedByBack()
                    }
                    // Her iki durumda da CreateQuestion fragment'ini kapat (overlay veya abacus)
                    supportFragmentManager.popBackStack()
                    return
                }

                // 2) Öğretmen CreateQuestion'dan gelip soru seçme modundaysa (NotificationFragment + pending medya),
                //    sistem geri tuşu hiçbir şey yapmasın; çıkış sadece bar'daki teacherSendBackButton ile olsun.
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
                if (teacherPendingMediaPath != null && currentFragment is NotificationFragment) {
                    return
                }

                // 3) Kökte değilsek normal backstack davranışı
                val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
                val atRoot = supportFragmentManager.backStackEntryCount <= 1 && current is PartSelectionFragment
                if (!atRoot) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < backPressToExitMillis) {
                    finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(this@MainActivity, "Çıkmak için tekrar bas", Toast.LENGTH_SHORT).show()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
            val createQuestionOverlay = supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer) as? CreateQuestionFragment
            val createQuestionAbacus = supportFragmentManager.findFragmentById(R.id.abacusFragmentContainer) as? CreateQuestionFragment
            val createQuestionVisible = createQuestionOverlay != null || createQuestionAbacus != null
            val teacherSelectingQuestion = teacherPendingMediaPath != null && current is NotificationFragment
            val seasonRewardGateVisible =
                supportFragmentManager.findFragmentByTag(SeasonLeaderboardRewardGateFragment.TAG) != null
            backCallback.isEnabled = seasonRewardGateVisible ||
                (current is PartSelectionFragment && supportFragmentManager.backStackEntryCount <= 1) ||
                createQuestionVisible ||
                teacherSelectingQuestion
            binding.root.post { tryShowSeasonLeaderboardRewardGateIfNeeded() }
        }

        // Listener'ları set et
        setupClickListeners()
        binding.fragmentContainerID.post { updateCurrencyPanelVisibility() }

        // Ders overlay'i (abacus/result) kapanınca back stack her zaman değişmez; MapFragment da sürekli resumed
        // kalabilir. Sezon ödül kapısı için bloklayıcı fragment destroy olduğunda yeniden dene.
        seasonLeaderboardGateRetryLifecycleCallbacks =
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                    val cid = fragmentHostContainerIdForSeasonGateRetry(f)
                    val isLessonFlowType = fragmentBlocksSeasonLeaderboardGate(f)
                    val isOverlayHost =
                        cid == R.id.abacusFragmentContainer ||
                            cid == R.id.resultFragmentContainer ||
                            cid == R.id.createQuestionOverlayContainer
                    val isMainFm = fm === supportFragmentManager
                    if (!isLessonFlowType) return
                    // [f.id] yerine [containerId]: LessonResult gibi replace(container, …) ile gelenlerde id yanlış kalabiliyordu.
                    if (!isOverlayHost && !isMainFm) return
                    binding.root.post {
                        if (f is CreateQuestionFragment) {
                            maybeRequestSeasonGateAfterQuestionFlowStep()
                            return@post
                        }
                        if (f is QuestionMediaPickerDialogFragment) {
                            maybeRequestSeasonGateAfterQuestionFlowStep()
                            return@post
                        }
                        if (f is ChestResult || f is ChestFragment || f is MissionChestRewardFragment) {
                            if (claimPathScheduledSeasonGate) {
                                claimPathScheduledSeasonGate = false
                                // Claim yolu: reconcile + notifyVisible gerekli (yalnızca sezon kapısı yetmez).
                                scheduleSeasonGateAfterAbacusOverlayDismissed()
                            } else {
                                scheduleSeasonGateAfterAbacusOverlayDismissed()
                            }
                            return@post
                        }
                        if (forcingAbacusOverlayDismissForSeasonGate) {
                            requestSeasonLeaderboardRewardGateIfPending()
                            return@post
                        }
                        scheduleSeasonGateAfterAbacusOverlayDismissed()
                    }
                }
            }
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            seasonLeaderboardGateRetryLifecycleCallbacks!!,
            true,
        )
    }

    /**
     * İlk açılışta TutorialFragment'ı gösterir
     */
    private fun showFirstTutorial() {
        logFirstTutorial("showFirstTutorial", "initialize partId=4")
        GlobalLessonData.globalPartId = 1
        GlobalLessonData.initialize(this, 1) {
            val item = GlobalLessonData.getLessonItem(1)
            if (item == null) {
                logFirstTutorial(
                    "showFirstTutorial.ABORT",
                    "getLessonItem(1)=null items=${GlobalLessonData.lessonItems.size}",
                )
                return@initialize
            }
            logFirstTutorial(
                "showFirstTutorial.ready",
                "tutorialNumber=${item.tutorialNumber} title=${item.title.take(40)}",
            )
            renderFirstTutorial(item)
        }
    }

    private fun showFirstTutorialFromPreparedDataOrLoad() {
        val preparedItem = GlobalLessonData.getLessonItem(1)
        logFirstTutorial(
            "showFirstTutorialFromPrepared",
            "preparedItem=${preparedItem != null} lessonItems=${GlobalLessonData.lessonItems.size}",
        )
        if (preparedItem == null) {
            val immediateItem = GlobalLessonData.createLessonItems(1).getOrNull(1)
            if (immediateItem != null) {
                logFirstTutorial(
                    "showFirstTutorialFromPrepared",
                    "template fallback tutorialNumber=${immediateItem.tutorialNumber}",
                )
                renderFirstTutorial(immediateItem)
            } else {
                logFirstTutorial("showFirstTutorialFromPrepared", "template null -> showFirstTutorial()")
                showFirstTutorial()
            }
            return
        }
        renderFirstTutorial(preparedItem)
    }

    private fun renderFirstTutorial(item: LessonItem) {
        firstTutorialOverlayBootstrapActive = true
        logFirstTutorial(
            "renderFirstTutorial.START",
            "tutorialNumber=${item.tutorialNumber} ${overlaySnapshot("renderBefore")}",
        )
        item.mapFragmentIndex?.let { index -> GlobalValues.mapFragmentStepIndex = index }
        item.startStepNumber?.let { step -> GlobalValues.lessonStep = step }

        val mapFragment = MapFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainerID, mapFragment)
            .addToBackStack(null)
            .commit()
        logFirstTutorial("renderFirstTutorial", "MapFragment committed")

        binding.abacusFragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.abacusFragmentContainer, TutorialFragment.newInstance(item.tutorialNumber))
            .addToBackStack(null)
            .commit()
        logFirstTutorial(
            "renderFirstTutorial.END",
            overlaySnapshot("renderAfterCommit"),
        )
        binding.root.post {
            firstTutorialOverlayBootstrapActive = false
            logFirstTutorial(
                "renderFirstTutorial.post",
                overlaySnapshot("renderAfterPost"),
            )
        }
    }
    
    /**
     * Click listener'ları set eder (GuidePanel kapandıktan sonra yeniden aktif etmek için)
     */
    fun setupClickListeners() {
        binding.bottomNavigationID.setOnItemSelectedListener {
            requireOnlineAndLoggedInOrLogin {
                closeBottomSheet()
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
                when (it.itemId) {
                    R.id.map -> {
                        if (currentFragment is PartSelectionFragment) return@requireOnlineAndLoggedInOrLogin
                        if (currentFragment is MapFragment) {
                            supportFragmentManager.popBackStack("part_map", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            binding.lessonPartBackButton.visibility = View.GONE
                            return@requireOnlineAndLoggedInOrLogin
                        }
                        
                        var hasPartMap = false
                        for (i in 0 until supportFragmentManager.backStackEntryCount) {
                            if (supportFragmentManager.getBackStackEntryAt(i).name == "part_map") {
                                hasPartMap = true
                                break
                            }
                        }
                        
                        if (hasPartMap && GlobalLessonData.globalPartId != 9) {
                            supportFragmentManager.popBackStackImmediate("part_map", 0)
                            binding.lessonPartBackButton.visibility = View.VISIBLE
                            binding.currencyPanel.visibility = View.VISIBLE
                        } else {
                            if (hasPartMap) {
                                supportFragmentManager.popBackStackImmediate("part_map", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                                binding.lessonPartBackButton.visibility = View.GONE
                                binding.currencyPanel.visibility = View.VISIBLE
                            } else {
                                changeFragment(PartSelectionFragment())
                            }
                        }
                    }
                    R.id.tasks ->
                        if (currentFragment is MissionsFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(MissionsFragment())
                    R.id.explore ->
                        if (currentFragment is TasksFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(TasksFragment())
                    R.id.profile ->
                        if (currentFragment is ProfileFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(ProfileFragment())
                    R.id.shop ->
                        if (currentFragment is AbacusCustomizationFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(AbacusCustomizationFragment())
                    R.id.notification -> {
                        if (currentFragment is NotificationFragment) return@requireOnlineAndLoggedInOrLogin
                        // CreateQuestion'dan Gönder ile açılan seçim modu fragment'ı üzerine yazma
                        if (teacherPendingMediaPath != null) return@requireOnlineAndLoggedInOrLogin
                        changeFragment(NotificationFragment())
                    }
                }
            }
            true
        }
        
        // Can sayacına uzun basınca canı sıfırlayan test kısayolu — SADECE debug derlemesinde.
        //
        // Release'de de açıktı ve yıkıcıydı: useEnergy() sunucuya spendEnergy çağrısı gönderip
        // energy_full_time'ı yetkili olarak ileri itiyor, yani geri alınamıyor. Çocuklar ekranda
        // gördükleri her şeye uzun basar; Free planda tek bir kazayla 50 dakikalık oyun hakkı
        // gidiyordu. Üstelik bu, enerjinin gelir kaldıracı olduğu bir üründe kullanıcıya
        // "uygulama canımı yedi" dedirtecek türden bir hata.
        //
        // BuildConfig.DEBUG derleme zamanı sabiti olduğu için blok release APK'sinde hiç yer
        // almaz; çalışma zamanı kontrolü (FLAG_DEBUGGABLE) kodu APK'de bırakırdı.
        if (BuildConfig.DEBUG) {
            binding.energyText.setOnLongClickListener {
                energyManager.useEnergy(energyManager.getCurrentEnergy())
                true
            }
        }

        // lessonPartBackButton: MapFragment'i kapat, PartSelectionFragment'e dön
        binding.lessonPartBackButton.setOnClickListener {
            val fm = supportFragmentManager
            // İçinde ne kadar hayalet lesson kaydı birikmiş olursa olsun, "part_map" (MapFragment) işlemini ve üstündekileri tamamen temizle.
            fm.popBackStack("part_map", FragmentManager.POP_BACK_STACK_INCLUSIVE)
            binding.lessonPartBackButton.visibility = View.GONE
        }
        
        // Currency panel tıklamaları — ShopFragment aç
        val openShop = View.OnClickListener { openShopFragment() }
        binding.energyText.setOnClickListener(openShop)
        binding.energyIcon.setOnClickListener(openShop)
        binding.diamondID.setOnClickListener(openShop)
        binding.currencyText.setOnClickListener(openShop)
        binding.keyIcon.setOnClickListener(openShop)
        binding.keyText.setOnClickListener(openShop)
        binding.creditIcon.setOnClickListener(openShop)
        binding.creditText.setOnClickListener(openShop)

        // Alev: seri ekranını aç. Üst bardaki diğer göstergeler mağazaya gidiyor, bu
        // gitmiyor — seri bir bakiye değil, kendi ekranı var.
        binding.streakContainer.setOnClickListener { openStreakFragment() }

        // Debug kısayolu: aleve uzun basınca bugüne bir dakika eklenir. Enerji metnindeki
        // kısayolla aynı gerekçe — blok derleme zamanında elendiği için release APK'sinde
        // hiç yer almaz. Seriyi test etmek aksi halde her tur için gerçek dakikalar bekletiyor.
        if (BuildConfig.DEBUG) {
            binding.streakContainer.setOnLongClickListener {
                StudyTimeTracker.addSecondsForDebug(this, 60)
                refreshStreakUi()
                val seconds = StudyTimeTracker.secondsToday(this)
                val goal = StreakRepository.goalMinutes(this)
                Toast.makeText(
                    this,
                    "Bugün ${seconds / 60} dk ${seconds % 60} sn / $goal dk",
                    Toast.LENGTH_SHORT,
                ).show()
                true
            }
        }
    }

    fun setBottomPanelEnabled(enabled: Boolean) {
        binding.bottomPanelOverlay.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.bottomNavigationID.isEnabled = enabled
        binding.bottomNavigationID.alpha = if (enabled) 1f else 0.6f
        val menu = binding.bottomNavigationID.menu
        for (i in 0 until menu.size()) {
            menu.getItem(i).isEnabled = enabled
        }
    }

    /**
     * [fragmentContainerID] içindeki MapFragment kökündeki soru sor butonu.
     * Sandık / sonuç akışında tıklamayı keser; açılınca kayıt devam ediyorsa yine kapalı kalır.
     */
    fun setMapFragmentAskQuestionInteractionBlocked(blocked: Boolean) {
        binding.fragmentContainerID.findViewById<View>(R.id.askQuestionButton)?.apply {
            if (blocked) {
                isEnabled = false
                isClickable = false
                isFocusable = false
                setOnTouchListener { _, _ -> true }
            } else {
                val allow = !isQuestionRecordingInProgress()
                isEnabled = allow
                isClickable = allow
                isFocusable = allow
                setOnTouchListener(null)
                alpha = if (allow) 1f else 0.5f
            }
        }
    }

    /**
     * Soru sorma akışını başlatır (Abacus, Tutorial, Map gibi her yerden kullanılır).
     * @param containerId CreateQuestionFragment'ın açılacağı container (R.id.abacusFragmentContainer veya R.id.fragmentContainerID)
     * @param viewToCapture Ekran görüntüsü alınacak view (fragment root); kamera seçilince bu view capture edilir
     */
    /** Video kaydı (soru için) başlatıldığında true; overlay kapanınca false. Butonlar bu sürede tıklanmaz. */
    private var isQuestionRecordingInProgress = false

    fun isQuestionRecordingInProgress(): Boolean = isQuestionRecordingInProgress

    /** Öğretmen, CreateQuestion'dan Gönder ile NotificationFragment'te soru seçme modundaysa true.
     *  abacusFragmentContainer bu sürede gizlense de içindeki fragment (Abacus/AbacusPractice) canlı
     *  kalıp kendi geri tuşu callback'ini tetikleyebiliyor; bu callback'ler geri tuşunu yutmak için
     *  bunu kontrol ediyor. */
    fun isTeacherSelectingQuestionToSend(): Boolean = teacherPendingMediaPath != null

    fun startQuestionFlow(containerId: Int, viewToCapture: () -> View?) {
        if (isQuestionRecordingInProgress) return
        if (supportFragmentManager.findFragmentByTag(QuestionMediaPickerDialogFragment.TAG) != null) return
        supportFragmentManager.setFragmentResultListener(
            QuestionMediaPickerDialogFragment.REQUEST_KEY,
            this
        ) { _, result ->
            when (result.getString(QuestionMediaPickerDialogFragment.RESULT_PICK)) {
                QuestionMediaPickerDialogFragment.PICK_CAMERA -> {
                    val view = viewToCapture()
                    if (view != null) {
                        view.post { captureViewAndOpenCreateQuestion(view, containerId) }
                    } else {
                        Toast.makeText(this, "Görüntü alınamadı.", Toast.LENGTH_SHORT).show()
                    }
                }
                QuestionMediaPickerDialogFragment.PICK_VIDEO -> requestQuestionScreenRecording(containerId)
            }
        }
        QuestionMediaPickerDialogFragment()
            .show(supportFragmentManager, QuestionMediaPickerDialogFragment.TAG)
    }

    private fun captureViewAndOpenCreateQuestion(view: View, containerId: Int) {
        if (view.width == 0 || view.height == 0) {
            view.post { captureViewAndOpenCreateQuestion(view, containerId) }
            return
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        val file = File(cacheDir, "question_screenshot_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
        bitmap.recycle()
        openCreateQuestionInOverlay(file.absolutePath, forVideo = false)
    }

    /** CreateQuestionFragment'ı overlay'de açar. fromTeacherSelectionBack=true ise fragment back ile kapanınca nav/tab eski haline döner. */
    private fun openCreateQuestionInOverlay(path: String, forVideo: Boolean, description: String? = null, fromTeacherSelectionBack: Boolean = false) {
        binding.createQuestionOverlayContainer.visibility = View.VISIBLE
        val fragment = if (forVideo) CreateQuestionFragment.newInstanceForVideo(path) else CreateQuestionFragment.newInstance(path)
        val isTeacher = authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER
        fragment.arguments = (fragment.arguments ?: Bundle()).apply {
            putBoolean(CreateQuestionFragment.ARG_IS_TEACHER, isTeacher)
            description?.let { putString(CreateQuestionFragment.ARG_DESCRIPTION, it) }
            putBoolean(CreateQuestionFragment.ARG_FROM_TEACHER_SELECTION_BACK, fromTeacherSelectionBack)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.createQuestionOverlayContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Öğretmen CreateQuestionFragment'ta Gönder'e bastığında çağrılır.
     * Yeni soru oluşturmak yerine, NotificationFragment'in öğretmen sohbetler sekmesinde seçim modu başlatılır.
     */
    fun onTeacherSubmitQuestionMedia(mediaType: String, mediaPath: String, description: String?) {
        // 1) Pending medya bilgisini sakla
        teacherPendingMediaType = mediaType
        teacherPendingMediaPath = mediaPath
        teacherPendingDescription = description
        teacherSelectedQuestionId = null
        teacherSelectedQuestionTitle = null

        // 2) Alt panel + geri butonu; soru seçilirken alt navigasyon gizlensin
        binding.teacherSendToQuestionBar.visibility = View.VISIBLE
        binding.teacherSendTopBar.visibility = View.VISIBLE
        binding.teacherSendBackButton.visibility = View.VISIBLE
        binding.bottomNavigationID.visibility = View.GONE

        // Abaküs/ders pratik ekranı (abacusFragmentContainer) tüm ekranı kaplayıp tıklamaları yuttuğu için,
        // altta açılacak NotificationFragment + seçim barını görünmez kılıyordu. Geçici olarak kapat.
        teacherPendingAbacusContainerWasVisible = binding.abacusFragmentContainer.visibility == View.VISIBLE
        binding.abacusFragmentContainer.visibility = View.GONE
        binding.teacherSendQuestionTitle.text = ""
        binding.teacherSendButton.isEnabled = false
        binding.teacherSendButton.alpha = 0.5f
        binding.teacherSendButton.setOnClickListener {
            handleTeacherSendToSelectedQuestion()
        }
        binding.teacherSendBackButton.setOnClickListener {
            onTeacherSelectionBackFromNotification()
        }

        // 3) CreateQuestion overlay'ini kapat (varsa)
        supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer)?.let {
            supportFragmentManager.popBackStackImmediate()
        }
        binding.createQuestionOverlayContainer.visibility = View.GONE

        // 4) NotificationFragment'i öğretmen sohbetler sekmesinde, selection mode açık olacak şekilde aç
        val fragment = NotificationFragment.newWithTeacherSelection(
            mediaType = mediaType,
            mediaPath = mediaPath,
            description = description
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerID, fragment)
            .addToBackStack(null)
            .commit()
        // Transaction uygulansın ki bottom nav listener tetiklendiğinde container'da zaten bu fragment olsun
        supportFragmentManager.executePendingTransactions()
        binding.bottomNavigationID.selectedItemId = R.id.notification
    }

    /**
     * NotificationFragment'te bir sohbet seçildiğinde çağrılır.
     * Seçilen soru alt panelde gösterilir ve send butonu aktifleşir.
     */
    fun onTeacherChatSelectedFromNotification(questionId: String, title: String) {
        teacherSelectedQuestionId = questionId
        teacherSelectedQuestionTitle = title
        binding.teacherSendQuestionTitle.text = title
        binding.teacherSendButton.isEnabled = true
        binding.teacherSendButton.alpha = 1f
    }

    /**
     * Alt paneldeki back (ic_back_arrow) tıklandığında çağrılır.
     * CreateQuestionFragment'a dönülür; açıklama vs. düzenlenip tekrar gönderilebilir.
     */
    fun onTeacherSelectionBackFromNotification() {
        (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? NotificationFragment)
            ?.exitTeacherSelectionMode()

        binding.teacherSendToQuestionBar.visibility = View.GONE
        binding.teacherSendTopBar.visibility = View.GONE
        binding.teacherSendBackButton.visibility = View.GONE
        binding.bottomNavigationID.visibility = View.VISIBLE
        teacherSelectedQuestionId = null
        teacherSelectedQuestionTitle = null

        // CreateQuestion'a geri dön (medya ve açıklama korunur, düzenlenebilir)
        val mediaPath = teacherPendingMediaPath
        val mediaType = teacherPendingMediaType
        val description = teacherPendingDescription
        if (!mediaPath.isNullOrEmpty() && !mediaType.isNullOrEmpty()) {
            val forVideo = mediaType == StudentQuestion.MEDIA_TYPE_VIDEO
            openCreateQuestionInOverlay(mediaPath, forVideo, description, fromTeacherSelectionBack = true)
        }
    }

    /** CreateQuestionFragment (öğretmen geri dönüşünden açılmış) back ile kapatıldığında: state temizle, nav ve tab eski haline getir. */
    fun onTeacherCreateQuestionDismissedByBack() {
        teacherPendingMediaPath?.let { path -> runCatching { File(path).takeIf { it.exists() }?.delete() } }
        teacherPendingMediaType = null
        teacherPendingMediaPath = null
        teacherPendingDescription = null
        teacherSelectedQuestionId = null
        teacherSelectedQuestionTitle = null
        binding.teacherSendToQuestionBar.visibility = View.GONE
        binding.teacherSendTopBar.visibility = View.GONE
        binding.teacherSendBackButton.visibility = View.GONE
        binding.bottomNavigationID.visibility = View.VISIBLE
        if (teacherPendingAbacusContainerWasVisible) {
            binding.abacusFragmentContainer.visibility = View.VISIBLE
        }
        teacherPendingAbacusContainerWasVisible = false
        (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? NotificationFragment)?.exitTeacherSelectionMode()
    }

    private fun handleTeacherSendToSelectedQuestion() {
        val questionId = teacherSelectedQuestionId
        val mediaType = teacherPendingMediaType
        val mediaPath = teacherPendingMediaPath
        val description = teacherPendingDescription
        if (questionId.isNullOrEmpty() || mediaType.isNullOrEmpty() || mediaPath.isNullOrEmpty()) {
            Toast.makeText(this, "Lütfen bir soru seçin.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "Oturum açık değil.", Toast.LENGTH_SHORT).show()
            return
        }
        val role = authManager.getCurrentUserType()
        val messageType = if (mediaType == StudentQuestion.MEDIA_TYPE_VIDEO) QuestionMessage.TYPE_VIDEO else QuestionMessage.TYPE_IMAGE

        // 1) Medya mesajını (varsa açıklama ile tek mesaj olarak) kuyruğa al.
        val mediaClientId = "pending_${System.currentTimeMillis()}"

        // QuestionChatFragment bu upload'ı kendisi başlatmadığı için (fragment henüz açık değil),
        // retry meta'sını ve optimistic pending mesajı burada, QuestionChatFragment.startUploadService()
        // ile aynı şekilde kaydediyoruz. Aksi halde: (a) servisin ACTION_UPLOAD_STARTED broadcast'i
        // fragment receiver'ı register olmadan gelirse mesaj sohbette hiç görünmez, (b) upload
        // başarısız olursa ne kullanıcı fark eder ne de "tekrar gönder" seçeneği çalışır.
        GlobalValues.uploadMetaByClientId[mediaClientId] = PendingUploadMeta(
            questionId = questionId,
            type = messageType,
            filePath = mediaPath,
            textContent = null,
            caption = description
        )
        GlobalValues.activeUploadIdsByQuestion.getOrPut(questionId) { mutableSetOf() }.add(mediaClientId)
        GlobalValues.pendingQuestionMessages.getOrPut(questionId) { mutableListOf() }.add(
            QuestionMessage(
                id = mediaClientId,
                senderUid = uid,
                senderRole = role,
                type = messageType,
                textContent = description,
                createdAt = Timestamp.now()
            )
        )

        Intent(this, QuestionUploadForegroundService::class.java).apply {
            putExtra(QuestionUploadForegroundService.KEY_QUESTION_ID, questionId)
            putExtra(QuestionUploadForegroundService.KEY_TYPE, messageType)
            putExtra(QuestionUploadForegroundService.KEY_CLIENT_ID, mediaClientId)
            putExtra(QuestionUploadForegroundService.KEY_SENDER_UID, uid)
            putExtra(QuestionUploadForegroundService.KEY_SENDER_ROLE, role)
            putExtra(QuestionUploadForegroundService.KEY_FILE_PATH, mediaPath)
            if (!description.isNullOrBlank()) {
                putExtra(QuestionUploadForegroundService.KEY_CAPTION, description)
            }
        }.also {
            QuestionUploadForegroundService.start(applicationContext, it)
        }

        // 2) Sohbetin son mesaj zamanını güncelle; aksi halde sohbet listeleri (lastMessageAt'e göre
        // sıralanıyor) bu yeni mesajı yansıtmak için üste taşınmaz.
        firestore.collection("questions").document(questionId)
            .update("lastMessageAt", Timestamp.now())

        // Temizlik: overlay, bar, geri butonu, alt nav tekrar göster, geçici state (dosyayı servis silecek)
        supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer)?.let {
            supportFragmentManager.popBackStack()
        }
        // NotificationFragment az sonra QuestionChatFragment ile replace edilecek; oradan geri
        // dönüldüğünde (popBackStack) view'ı yeniden oluşturulunca seçim moduna tekrar girmesin
        // diye burada, henüz view'ı yok edilmeden seçim modundan çıkarıyoruz.
        (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? NotificationFragment)
            ?.exitTeacherSelectionMode()
        binding.teacherSendToQuestionBar.visibility = View.GONE
        binding.teacherSendTopBar.visibility = View.GONE
        binding.teacherSendBackButton.visibility = View.GONE
        binding.bottomNavigationID.visibility = View.VISIBLE
        // abacusFragmentContainer kasıtlı olarak GONE bırakılıyor: kullanıcı artık sohbet ekranına gidiyor,
        // pratik ekranı geri gelirse chat'i (elevation'ı düşük olduğu için) tamamen kaplar.
        teacherPendingAbacusContainerWasVisible = false
        teacherPendingMediaType = null
        teacherPendingMediaPath = null
        teacherPendingDescription = null
        teacherSelectedQuestionId = null
        teacherSelectedQuestionTitle = null

        Toast.makeText(this, "Sohbete gönderildi.", Toast.LENGTH_SHORT).show()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerID, QuestionChatFragment.newInstance(questionId))
            .addToBackStack(null)
            .commit()
    }

    /** Video kaydı bitince CreateQuestionFragment'ın açılacağı container (soru akışı video seçilince set edilir). */
    private var questionFlowContainerIdForRecording: Int = R.id.abacusFragmentContainer

    /** Video soru akışı: izinler + MediaProjection + 60sn kayıt + overlay. */
    fun requestQuestionScreenRecording(containerId: Int) {
        questionFlowContainerIdForRecording = containerId
        isQuestionRecordingInProgress = true
        setAskQuestionButtonEnabled(false)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        launchMediaProjectionForQuestion()
    }

    private fun launchMediaProjectionForQuestion() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    private var recordingStartTimeMs: Long = 0L
    private var recordingPausedAtMs: Long = 0L
    private var totalPausedDurationMs: Long = 0L
    private var isRecordingPaused = false
    private var maxRecordingSecForSession = 60

    /** Servis ACTION_RECORDING_STARTED gönderdi mi? Gönderilmezse panel kapatılıp hata gösterilir. */
    private var recordingStartConfirmed = false
    private var recordingStartTimeoutRunnable: Runnable? = null
    /** MediaProjection izni + encoder kurulumu için rahat bir pay; aşılırsa kayıt başlamamıştır. */
    private val RECORDING_START_TIMEOUT_MS = 8_000L

    private fun showRecordingOverlay() {
        recordingStartTimeMs = System.currentTimeMillis()
        totalPausedDurationMs = 0L
        isRecordingPaused = false
        if (recordingOverlayView == null) {
            // Önce çizim overlay'ini (tüm ekranı kaplayan şeffaf view) ekle
            drawingOverlayView = DrawingOverlayView(this).apply {
                visibility = View.GONE
                setDrawingEnabled(false)
            }
            val drawingParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            binding.recordingOverlayContainer.addView(drawingOverlayView, drawingParams)

            // Sonra kayıt barını (üst panel) ekle - ekranın üstünde
            recordingOverlayView = LayoutInflater.from(this)
                .inflate(R.layout.view_recording_overlay, binding.recordingOverlayContainer, false)
            val params = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP }
            binding.recordingOverlayContainer.addView(recordingOverlayView, params)

            // Sol kenara çizim kontrol panelini (çekmece) ekle
            drawingControlsView = LayoutInflater.from(this)
                .inflate(R.layout.view_drawing_controls_overlay, binding.recordingOverlayContainer, false)
            val controlsParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL }
            binding.recordingOverlayContainer.addView(drawingControlsView, controlsParams)

            binding.recordingOverlayContainer.isClickable = false
            binding.recordingOverlayContainer.setOnTouchListener { _, _ -> false }

            val btnPauseResume =
                recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnPauseResume)
            btnPauseResume.setImageResource(R.drawable.pause_video_ic)
            btnPauseResume.contentDescription = "Durdur"
            btnPauseResume.setOnClickListener {
                if (isRecordingPaused) {
                    startService(
                        Intent(
                            this,
                            ScreenRecordingService::class.java
                        ).setAction(ScreenRecordingService.ACTION_RESUME)
                    )
                } else {
                    startService(
                        Intent(
                            this,
                            ScreenRecordingService::class.java
                        ).setAction(ScreenRecordingService.ACTION_PAUSE)
                    )
                }
            }
            recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnSave)
                .setOnClickListener {
                    startService(
                        Intent(
                            this,
                            ScreenRecordingService::class.java
                        ).setAction(ScreenRecordingService.ACTION_STOP_AND_SAVE)
                    )
                }
            recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnCancel)
                .setOnClickListener {
                    startService(
                        Intent(
                            this,
                            ScreenRecordingService::class.java
                        ).setAction(ScreenRecordingService.ACTION_STOP_AND_DISCARD)
                    )
                    hideRecordingOverlay()
                }

            // Çizim kontrollerini bağla (ekranın solunda çekmece olarak)
            val drawerRoot =
                drawingControlsView!!.findViewById<View>(R.id.drawingControlsRoot)
            val drawerButton =
                drawingControlsView!!.findViewById<View>(R.id.drawerButton)
            val drawerButtonIcon =
                drawingControlsView!!.findViewById<android.widget.ImageView>(R.id.drawerButtonIcon)
            val pencilButton =
                drawingControlsView!!.findViewById<ImageButton>(R.id.pencilButton)
            val colorStrip =
                drawingControlsView!!.findViewById<DrawingColorStripView>(R.id.colorStrip)
            val undoButton =
                drawingControlsView!!.findViewById<ImageButton>(R.id.undoButton)
            val strokeWidthSeekBar =
                drawingControlsView!!.findViewById<VerticalSliderView>(R.id.strokeWidthSeekBar)
            val strokePreview =
                drawingControlsView!!.findViewById<StrokePreviewView>(R.id.strokePreview)

            // Panel başlangıçta kapalı olsun: tüm kartı (handle + panel) sola it,
            // drawerButton ekran kenarında, panel ise dışında kalsın.
            val panel =
                drawingControlsView!!.findViewById<View>(R.id.drawingControlsContainer)
            // İlk frame'de "açık görünüp sonra kapanma" flash'ını engelle: ölçüm bitene kadar gizle.
            drawerRoot.visibility = View.INVISIBLE
            drawerRoot.post {
                isDrawingPanelOpen = false
                drawerRoot.translationX = -panel.width.toFloat()
                drawerButtonIcon.rotationY = -180f
                drawerRoot.visibility = View.VISIBLE
            }

            // rotationY 3D dönüşünün belirgin görünmesi için cameraDistance ayarla (px).
            drawerButtonIcon.cameraDistance = 8000f * resources.displayMetrics.density

            drawerButton.setOnClickListener {
                val targetOpen = !isDrawingPanelOpen
                isDrawingPanelOpen = targetOpen
                drawerButtonIcon.animate()
                    .rotationY(if (targetOpen) 0f else -180f)
                    .setDuration(200)
                    .start()
                drawerRoot.animate()
                    .translationX(if (targetOpen) 0f else -panel.width.toFloat())
                    .setDuration(200)
                    .start()
            }

            // Renk seçici: hem kalemin rengini hem de buton görünümünü güncelle
            colorStrip.listener = object : DrawingColorStripView.OnColorSelectedListener {
                override fun onColorSelected(color: Int) {
                    drawingOverlayView?.setStrokeColor(color)
                    // Kullanıcıya seçili rengi göstermek için ikon tint'ini anlık değiştir
                    pencilButton.imageTintList =
                        android.content.res.ColorStateList.valueOf(color)
                    strokePreview.setColor(color)
                }
            }

            // Kalınlık slider'ı: min–max aralığını DrawingOverlayView sabitlerine göre ölçekle
            strokeWidthSeekBar.max = 1000
            // Varsayılan strok kalınlığını ortalara koy (örnek: 40)
            strokeWidthSeekBar.progress = 500
            strokeWidthSeekBar.listener = object : VerticalSliderView.Listener {
                override fun onProgressChanged(progress: Int, fromUser: Boolean) {
                    val fraction = progress / strokeWidthSeekBar.max.toFloat()
                    val width = DrawingOverlayView.MIN_STROKE_WIDTH +
                            fraction * (DrawingOverlayView.MAX_STROKE_WIDTH - DrawingOverlayView.MIN_STROKE_WIDTH)
                    drawingOverlayView?.setStrokeWidth(width)
                    strokePreview.setStrokeWidth(width)
                }

                override fun onStartTrackingTouch() {
                    // Dokunulurken: kalem gizli, önizleme noktası görünür
                    pencilButton.visibility = View.INVISIBLE
                    strokePreview.visibility = View.VISIBLE
                }

                override fun onStopTrackingTouch() {
                    // Dokunma bittiğinde: kalem tekrar görünür, önizleme gizlenir
                    pencilButton.visibility = View.VISIBLE
                    strokePreview.visibility = View.GONE
                }
            }

            // Kalem toggle
            pencilButton.setOnClickListener {
                val enabled = !(pencilButton.isSelected)
                pencilButton.isSelected = enabled
                // Seçili durumda gri yuvarlak arka plan; seçili değilse şeffaf yuvarlak
                if (enabled) {
                    pencilButton.setBackgroundResource(R.drawable.bg_pencil_circle_selected)
                } else {
                    pencilButton.setBackgroundResource(R.drawable.bg_pencil_circle)
                }
                if (enabled) {
                    // Çizim modunu aç: hem mevcut çizimler hem de yeni dokunuşlar aktif olsun
                    drawingOverlayView?.visibility = View.VISIBLE
                    drawingOverlayView?.setDrawingEnabled(true)
                } else {
                    // Çizim modunu kapat: yeni dokunuşları alttaki UI'ya geçir ama
                    // ekrandaki mevcut çizimleri göstermeye devam et.
                    drawingOverlayView?.setDrawingEnabled(false)
                }
            }

            // Undo: tek tıklama son stroke'u siler, uzun basma tümünü temizler
            undoButton.setOnClickListener {
                drawingOverlayView?.undoLastStroke()
            }
            undoButton.setOnLongClickListener {
                drawingOverlayView?.clearAllStrokes()
                true
            }
        } else {
            val btnPauseResume =
                recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnPauseResume)
            btnPauseResume.setImageResource(R.drawable.pause_video_ic)
            btnPauseResume.contentDescription = "Durdur"
            isRecordingPaused = false

            // Her yeni kayıtta kalem pasif ve arka plan şeffaf yuvarlak başlasın
            drawingOverlayView?.apply {
                setDrawingEnabled(false)
                visibility = View.GONE
            }
            // Çizim panelini de kapalı konuma ve ikon rotasyonuna sıfırla
            drawingControlsView?.let { rootView ->
                val drawerRoot = rootView.findViewById<View>(R.id.drawingControlsRoot)
                val panel = rootView.findViewById<View>(R.id.drawingControlsContainer)
                val drawerButtonIcon = rootView.findViewById<android.widget.ImageView>(R.id.drawerButtonIcon)
                isDrawingPanelOpen = false
                // Önceki kayıtta açık kalsa bile ilk frame'de flash olmasın.
                drawerRoot.visibility = View.INVISIBLE
                drawerRoot.post {
                    drawerRoot.translationX = -panel.width.toFloat()
                    drawerButtonIcon.rotationY = -180f
                    drawerRoot.visibility = View.VISIBLE
                }
            }
            drawingControlsView?.findViewById<ImageButton>(R.id.pencilButton)?.apply {
                isSelected = false
                visibility = View.VISIBLE
                setBackgroundResource(R.drawable.bg_pencil_circle)
            }
            drawingControlsView?.findViewById<StrokePreviewView>(R.id.strokePreview)?.apply {
                visibility = View.GONE
            }
        }
        binding.recordingOverlayContainer.visibility = View.VISIBLE
        updateRecordingTimerText(0)
        registerRecordingReceiver()
        recordingTimerRunnable = object : Runnable {
            override fun run() {
                val elapsedSec = ((System.currentTimeMillis() - recordingStartTimeMs - totalPausedDurationMs) / 1000).toInt().coerceAtMost(maxRecordingSecForSession)
                updateRecordingTimerText(elapsedSec)
                if (elapsedSec < maxRecordingSecForSession && !isRecordingPaused) recordingHandler.postDelayed(this, 1000L)
            }
        }
        recordingHandler.postDelayed(recordingTimerRunnable!!, 1000L)
        setQuitButtonEnabled(false)
        setAskQuestionButtonEnabled(false)
    }

    /**
     * Kayıt yayınlarının alıcısını kurar. Servis başlatılmadan ÖNCE çağrılır: servis daha
     * ilk saniyede patlarsa (startForeground / encoder hatası) gönderdiği RECORDING_FAILED
     * yayını, alıcı henüz kayıtlı olmadığı için kaybolabiliyordu. Zaten kayıtlıysa no-op.
     */
    private fun registerRecordingReceiver() {
        if (recordingReceiver != null) return
        recordingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ScreenRecordingService.ACTION_RECORDING_STARTED -> {
                        recordingStartConfirmed = true
                        recordingStartTimeoutRunnable?.let { recordingHandler.removeCallbacks(it) }
                        recordingStartTimeoutRunnable = null
                        // Sayaç, panelin açıldığı andan değil kaydın gerçekten başladığı
                        // andan işlesin (MediaProjection kurulumu birkaç yüz ms sürebiliyor).
                        recordingStartTimeMs = System.currentTimeMillis()
                        totalPausedDurationMs = 0L
                        updateRecordingTimerText(0)
                    }
                    ScreenRecordingService.ACTION_RECORDING_FINISHED -> {
                        val path = intent.getStringExtra(ScreenRecordingService.EXTRA_OUTPUT_PATH)
                        hideRecordingOverlay()
                        if (!path.isNullOrEmpty()) {
                            openCreateQuestionInOverlay(path, forVideo = true)
                        }
                    }
                    ScreenRecordingService.ACTION_RECORDING_FAILED -> {
                        hideRecordingOverlay()
                        Toast.makeText(this@MainActivity, "Kayıt başarısız.", Toast.LENGTH_SHORT).show()
                    }
                    ScreenRecordingService.ACTION_RECORDING_PAUSED -> {
                        isRecordingPaused = true
                        recordingPausedAtMs = System.currentTimeMillis()
                        recordingTimerRunnable?.let { recordingHandler.removeCallbacks(it) }
                        recordingTimerRunnable = null
                        recordingOverlayView?.findViewById<ImageButton>(R.id.recordingBtnPauseResume)?.apply {
                            setImageResource(R.drawable.play_video_ic)
                            contentDescription = "Devam et"
                        }
                    }
                    ScreenRecordingService.ACTION_RECORDING_RESUMED -> {
                        totalPausedDurationMs += System.currentTimeMillis() - recordingPausedAtMs
                        isRecordingPaused = false
                        recordingOverlayView?.findViewById<ImageButton>(R.id.recordingBtnPauseResume)?.apply {
                            setImageResource(R.drawable.pause_video_ic)
                            contentDescription = "Durdur"
                        }
                        recordingTimerRunnable = object : Runnable {
                            override fun run() {
                                val elapsedSec = ((System.currentTimeMillis() - recordingStartTimeMs - totalPausedDurationMs) / 1000).toInt().coerceAtMost(maxRecordingSecForSession)
                                updateRecordingTimerText(elapsedSec)
                                if (elapsedSec < maxRecordingSecForSession) recordingHandler.postDelayed(this, 1000L)
                            }
                        }
                        val elapsedSec = ((System.currentTimeMillis() - recordingStartTimeMs - totalPausedDurationMs) / 1000).toInt().coerceAtMost(maxRecordingSecForSession)
                        updateRecordingTimerText(elapsedSec)
                        recordingHandler.postDelayed(recordingTimerRunnable!!, 1000L)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ScreenRecordingService.ACTION_RECORDING_STARTED)
            addAction(ScreenRecordingService.ACTION_RECORDING_FINISHED)
            addAction(ScreenRecordingService.ACTION_RECORDING_FAILED)
            addAction(ScreenRecordingService.ACTION_RECORDING_PAUSED)
            addAction(ScreenRecordingService.ACTION_RECORDING_RESUMED)
        }
        ContextCompat.registerReceiver(this, recordingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /**
     * Servisten "kayıt başladı" sinyali gelmezse paneli kapatıp kullanıcıyı uyarır.
     * Bu olmadan, kayıt hiç başlamamışken panel açık kalıp sayaç işliyor; kaydet ve duraklat
     * butonları da yanıtı servisten bekledikleri için hiçbir şey yapmıyor gibi görünüyordu.
     */
    private fun armRecordingStartTimeout() {
        recordingStartConfirmed = false
        recordingStartTimeoutRunnable?.let { recordingHandler.removeCallbacks(it) }
        val runnable = Runnable {
            recordingStartTimeoutRunnable = null
            if (recordingStartConfirmed) return@Runnable
            startService(
                Intent(this, ScreenRecordingService::class.java)
                    .setAction(ScreenRecordingService.ACTION_STOP_AND_DISCARD)
            )
            hideRecordingOverlay()
            Toast.makeText(this, "Ekran kaydı bu cihazda başlatılamadı.", Toast.LENGTH_LONG).show()
        }
        recordingStartTimeoutRunnable = runnable
        recordingHandler.postDelayed(runnable, RECORDING_START_TIMEOUT_MS)
    }

    private fun updateRecordingTimerText(elapsedSec: Int) {
        val maxM = maxRecordingSecForSession / 60
        val maxS = maxRecordingSecForSession % 60
        recordingOverlayView?.findViewById<TextView>(R.id.recordingTimerText)?.text =
            "${String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)} / $maxM:${String.format("%02d", maxS)}"
    }

    private fun hideRecordingOverlay() {
        recordingTimerRunnable?.let { recordingHandler.removeCallbacks(it) }
        recordingTimerRunnable = null
        recordingStartTimeoutRunnable?.let { recordingHandler.removeCallbacks(it) }
        recordingStartTimeoutRunnable = null
        recordingStartConfirmed = false
        recordingReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) { /* zaten kaldırılmış */ }
        }
        recordingReceiver = null
        binding.recordingOverlayContainer.visibility = View.GONE
        // Çizim overlay state'ini de sıfırla
        drawingOverlayView?.apply {
            clearAllStrokes()
            setDrawingEnabled(false)
            visibility = View.GONE
        }
        isQuestionRecordingInProgress = false
        setQuitButtonEnabled(true)
        setAskQuestionButtonEnabled(true)
        binding.root.post { maybeRequestSeasonGateAfterQuestionFlowStep() }
    }

    private fun setAskQuestionButtonEnabled(enabled: Boolean) {
        listOf(binding.abacusFragmentContainer, binding.fragmentContainerID).forEach { container ->
            container.findViewById<View>(R.id.askQuestionButton)?.apply {
                isEnabled = enabled
                isClickable = enabled
                alpha = if (enabled) 1f else 0.5f
            }
        }
    }

    private fun setQuitButtonEnabled(enabled: Boolean) {
        binding.abacusFragmentContainer.findViewById<View>(R.id.quitButton)?.apply {
            isEnabled = enabled
            isClickable = enabled
            alpha = if (enabled) 1f else 0.5f
        }
    }

    private fun changeFragment(fragment: Fragment) {
        dismissMapLessonOverlayChrome()
        releasePracticeTouchBlocker()
        releaseLessonActionTouchBlocker()
        if (fragment !is MapFragment) {
            purgeAbacusOverlayHosts("changeFragment.leaveMap")
        }
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerID, fragment)
            addToBackStack(null)
            commit()
        }
        binding.fragmentContainerID.post {
            updateCurrencyPanelVisibility()
            when (fragment) {
                is MapFragment -> sanitizeMapTouchSurface("changeFragment.map")
                is TasksFragment -> reconcileAbacusOverlayWhenTasksIsBase("changeFragment.tasks")
                else -> {
                    purgeAbacusOverlayHosts("changeFragment.other")
                    ensureChromeUnlockedAfterMapReturn("changeFragment.tab")
                }
            }
            logTouchDiag("changeFragment.post:${fragment.javaClass.simpleName}")
        }
    }

    private fun updateCurrencyPanelVisibility() {
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        binding.currencyPanel.visibility = if (current is MapFragment || current is PartSelectionFragment || current is TasksFragment || current is MissionsFragment) View.VISIBLE else View.GONE
        
        // MapFragment'ten çıkıldıysa (başka bir tab'a vs geçildiyse) lessonPartBackButton'u gizle
        if (current !is MapFragment && current !is ShopFragment) {
            binding.lessonPartBackButton.visibility = View.GONE
        }
    }

    fun showOfflineFragment() {
        // Hangi container'ın üstünde offline göstereceğiz?
        // Eğer abacusFragmentContainer görünürse, onu kaplasın; aksi halde ana container'ı.
        val useAbacusContainer = binding.abacusFragmentContainer.visibility == View.VISIBLE
        val containerId = if (useAbacusContainer) {
            R.id.abacusFragmentContainer
        } else {
            R.id.fragmentContainerID
        }

        val current = supportFragmentManager.findFragmentById(containerId)
        if (current is OfflineFragment) return

        supportFragmentManager.beginTransaction()
            .replace(containerId, OfflineFragment())
            .addToBackStack(null)
            .commit()
    }

    /**
     * OfflineFragment içinden Retry'e basıldığında çağrılır.
     * - Eğer altında zaten bir fragment varsa (Map, Abacus, vb.) sadece backstack'ten offline'ı pop eder.
     * - Eğer hiçbir fragment yoksa (uygulama tamamen offline açılmış ve henüz Map/Tutorial yüklenmemişse),
     *   Map/Tutorial başlangıç akışını başlatır.
     */
    fun handleOfflineRetry() {
        val fm = supportFragmentManager

        // Önce OfflineFragment'i back stack'ten kaldır.
        fm.popBackStack()
        fm.executePendingTransactions()

        // Abacus veya ana container'da hâlihazırda fragment varsa, ekstra bir şey yapma.
        val hasMainFragment =
            fm.findFragmentById(R.id.fragmentContainerID) != null ||
                fm.findFragmentById(R.id.abacusFragmentContainer) != null
        if (hasMainFragment) return

        // Hiç fragment yoksa, bu muhtemelen uygulamanın offline olarak açıldığı ilk durumdur.
        // Normal başlangıç akışını tekrar uygula (Map/Tutorial).
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val firstTutorialShown = DeviceAccountStore.hasEverHadAccount(this)

        if (firstTutorialShown) {
            fm.beginTransaction().apply {
                replace(R.id.fragmentContainerID, MapFragment())
                addToBackStack(null)
                commit()
            }
        } else {
            showFirstTutorial()
        }
    }

    private fun closeBottomSheet() {
        dismissMapLessonOverlayChrome()
    }

    /**
     * Ders bottom sheet / scrim haritayı kapattıysa anında temizle (Abacus quit sonrası liste görünmez kalmasın).
     */
    fun dismissMapLessonOverlayChrome() {
        if (!::binding.isInitialized) return
        findViewById<View>(R.id.scrimView)?.apply {
            animate().cancel()
            alpha = 0f
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            setOnClickListener(null)
        }
        val coordinator = findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        coordinator?.findViewWithTag<View>("bottom_sheet")?.let { sheet -> (sheet.parent as? ViewGroup)?.removeView(sheet) }
        coordinator?.findViewWithTag<View>("race_panel")?.let { sheet -> (sheet.parent as? ViewGroup)?.removeView(sheet) }
        coordinator?.findViewWithTag<View>("race_lesson_bottom_sheet")?.let { sheet -> (sheet.parent as? ViewGroup)?.removeView(sheet) }
    }

    /** [LessonAdapter] geçiş blocker'ı (content üstü tam ekran). */
    fun releaseLessonActionTouchBlocker() {
        findViewById<ViewGroup>(android.R.id.content)
            ?.findViewWithTag<View>(LESSON_ACTION_TOUCH_BLOCKER_TAG)
            ?.let { blocker -> (blocker.parent as? ViewGroup)?.removeView(blocker) }
    }

    /**
     * Harita tabanındayken scrim / touch blocker / hayalet abacus fragment ve host'u temizler.
     * Alt sekme → harita dönüşünde dokunma kilitlenmesini giderir.
     */
    fun sanitizeMapTouchSurface(caller: String) {
        if (!::binding.isInitialized || isFinishing || isDestroyed) return
        logMapTouchDiag("sanitize.enter", "CHECKING", "caller=$caller")
        logFirstTutorial("sanitizeMapTouchSurface.enter", "caller=$caller ${overlaySnapshot("sanitize")}")
        dismissMapLessonOverlayChrome()
        releasePracticeTouchBlocker()
        releaseLessonActionTouchBlocker()
        val fm = supportFragmentManager
        if (fm.findFragmentById(R.id.fragmentContainerID) !is MapFragment) {
            logMapTouchDiag("sanitize", "SKIP_NOT_MAP", "caller=$caller")
            ensureChromeUnlockedAfterMapReturn("sanitizeMapTouchSurface.notMap:$caller")
            return
        }
        fm.executePendingTransactions()
        val active = fm.findFragmentById(R.id.abacusFragmentContainer)
        val hostVisible = binding.abacusFragmentContainer.visibility == View.VISIBLE
        if (hostVisible && active != null && fragmentBlocksSeasonLeaderboardGate(active) &&
            !forcingAbacusOverlayDismissForSeasonGate &&
            !isStaleTutorialGhostOverlay(active)
        ) {
            logMapTouchDiag(
                "sanitize",
                "SKIP_ACTIVE_OVERLAY",
                "caller=$caller active=${active.javaClass.simpleName} hostVisible=true → enableMapTouchRouting ÇAĞRILMADI",
            )
            logFirstTutorial("sanitizeMapTouchSurface.skip", "active lesson overlay caller=$caller")
            ensureChromeUnlockedAfterMapReturn("sanitizeMapTouchSurface.activeLesson:$caller")
            return
        }
        if (hostVisible && isStaleTutorialGhostOverlay(active)) {
            logMapTouchDiag(
                "sanitize",
                "PURGE_STALE_TUTORIAL",
                "caller=$caller → hayalet TutorialFragment temizlenecek",
            )
        }
        val prevForce = forcingAbacusOverlayDismissForSeasonGate
        forcingAbacusOverlayDismissForSeasonGate = true
        try {
            purgeAbacusOverlayHosts("sanitizeMapTouchSurface:$caller")
            restoreMapUiAfterLessonOverlayDismiss()
            (fm.findFragmentById(R.id.fragmentContainerID) as? MapFragment)?.let { map ->
                if (map.isAdded && map.view != null) {
                    map.enableMapTouchRouting()
                }
            }
        } finally {
            forcingAbacusOverlayDismissForSeasonGate = prevForce
        }
        ensureChromeUnlockedAfterMapReturn("sanitizeMapTouchSurface:$caller")
        logMapTouchDiag("sanitize", "SANITIZE_DONE", "caller=$caller")
        logChromeBlockerDiagnostic("sanitizeMapTouchSurface:$caller")
        logTouchDiag("sanitizeMapTouchSurface:$caller")
    }

    /** Harita tabanındayken bilinçli tutorial oturumu yoksa hayalet [TutorialFragment] sayılır. */
    private fun isStaleTutorialGhostOverlay(overlay: Fragment?): Boolean {
        if (overlay !is TutorialFragment) return false
        if (firstTutorialOverlayBootstrapActive) return false
        if (activeMapTutorialOverlayFromLesson) return false
        // İlk tutorial henüz bitmediyse (renderFirstTutorial) hayalet sayma.
        if (!DeviceAccountStore.hasEverHadAccount(this)) return false
        return true
    }

    fun setActiveMapTutorialOverlayFromLesson(active: Boolean) {
        activeMapTutorialOverlayFromLesson = active
        logFirstTutorial("setActiveMapTutorialOverlayFromLesson", "active=$active")
    }

    private fun purgeAbacusOverlayHosts(caller: String) {
        if (!::binding.isInitialized) return
        val fm = supportFragmentManager
        var steps = 0
        while (steps++ < 8) {
            fm.executePendingTransactions()
            val overlay = fm.findFragmentById(R.id.abacusFragmentContainer) ?: break
            if (firstTutorialOverlayBootstrapActive && overlay is TutorialFragment) break
            if (overlay is TutorialFragment && activeMapTutorialOverlayFromLesson) break
            if (!fragmentBlocksSeasonLeaderboardGate(overlay)) break
            logFirstTutorial("purgeAbacusOverlayHosts.remove", "caller=$caller frag=${overlay.javaClass.simpleName}")
            logMapTouchDiag("purgeAbacus", "REMOVE_FRAGMENT", "caller=$caller frag=${overlay.javaClass.simpleName}")
            fm.beginTransaction().remove(overlay).commitNowAllowingStateLoss()
        }
        binding.abacusFragmentContainer.visibility = View.GONE
        binding.resultFragmentContainer.visibility = View.GONE
    }

    /**
     * Overlay kapandıktan sonra harita container'ı, MapFragment ve lesson sheet chrome'unu görünür yap.
     */
    fun restoreMapUiAfterLessonOverlayDismiss() {
        logFirstTutorial(
            "restoreMapUiAfterLessonOverlayDismiss",
            overlaySnapshot("restoreMapUi"),
        )
        if (!::binding.isInitialized || isFinishing || isDestroyed) return
        dismissMapLessonOverlayChrome()
        binding.coordinatorLayout.visibility = View.VISIBLE
        binding.fragmentContainerID.visibility = View.VISIBLE
        binding.abacusFragmentContainer.visibility = View.GONE
        binding.resultFragmentContainer.visibility = View.GONE
        releasePracticeTouchBlocker()
        releaseLessonActionTouchBlocker()
        val fm = supportFragmentManager
        if (fm.isStateSaved) return
        val base = fm.findFragmentById(R.id.fragmentContainerID) ?: return
        if (base.isHidden) {
            fm.beginTransaction().show(base).commitAllowingStateLoss()
        }
    }
    fun deleteAllLessonItems(context: Context) {
        // Test: Her uygulama açılışında hiçbir kullanıcı kayıtlı değilmiş gibi başlat (oturumu kapat)
        //FirebaseAuth.getInstance().signOut()
        //val webClientId = context.getString(com.example.app.R.string.default_web_client_id)
        /*if (webClientId.isNotEmpty() && webClientId != "YOUR_WEB_CLIENT_ID_HERE") {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso).signOut()
        }
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        */

        // Offline kullanılmayacağı için görev local cache'ini temizle. Bu kod hep kalacak.
        //MissionsProgressStore.clearLocalCache(context)
        // 1) Mevcut görev ilerlemesini sıfırla (daily/weekly counters + claimed flags)
        //MissionsProgressStore.resetAllProgress(context)
        // 2) Günlük/haftalık görev kombinasyonunu yeniden seçtir
        //MissionsProgressStore.forceReselectMissions(context)
        // 3) Seçilen yeni görevleri hemen üretip state'e yazdır (isteğe bağlı ama önerilir)
        //MissionsProgressStore.selectedMissionsForDaily(context)
        //MissionsProgressStore.selectedMissionsForWeekly(context)
        // Cloud'daki eski mission state'in geri hydrate edilmesini engellemek için
        // resetlenmiş local state'i doğrudan cloud'a overwrite et.
        //MissionsProgressStore.forceUploadStateToCloud(context)
        // Kullanıcıya özel lesson verilerini local'den temizler.
        //GlobalLessonData.clearCurrentUserLessonData(context)
        // Kullanıcı lesson verilerini Firestore'den siler (uid geç gelirse bekleyip tekrar dener)
        //deleteLessonProgressFromFirestoreWithAuthWait()
        // GuidePanel animasyon flag'lerini temizle (test için) Yönlendirme paneli
        //val guidePanelPrefs = context.getSharedPreferences("GuidePanelPrefs", Context.MODE_PRIVATE)
        //guidePanelPrefs.edit().clear().apply()
        
        // İlk tutorial flag'ini de temizle (test için)
        val appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        appPrefs.edit()
            //f.putBoolean("first_tutorial_shown", false)
            //.putBoolean("tutorial1_login_shown", false)  // Test: claimButton'da login tekrar gösterilsin
            .apply()
    }

    private fun deleteLessonProgressFromFirestoreWithAuthWait() {
        fun deleteForUid(uid: String) {
            firestore.collection("users")
                .document(uid)
                .collection("lessonProgress")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) return@addOnSuccessListener
                    val batch = firestore.batch()
                    snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit()
                        .addOnFailureListener { e ->
                            Log.e("MainActivity", "lessonProgress batch delete failed", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "lessonProgress read before delete failed", e)
                }
        }

        val authInstance = FirebaseAuth.getInstance()
        val existingUid = authInstance.currentUser?.uid
        if (!existingUid.isNullOrBlank()) {
            deleteForUid(existingUid)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        lateinit var listener: FirebaseAuth.AuthStateListener
        val timeoutRunnable = Runnable {
            authInstance.removeAuthStateListener(listener)
        }
        listener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                authInstance.removeAuthStateListener(listener)
                handler.removeCallbacks(timeoutRunnable)
                deleteForUid(uid)
            }
        }
        authInstance.addAuthStateListener(listener)
        handler.postDelayed(timeoutRunnable, 4000L)
    }
    private fun showAbacusFragment() {
        val fragmentContainer = binding.abacusFragmentContainer
        fragmentContainer.visibility = View.VISIBLE

        // Fragment'ı oluştur
        val fragment = AbacusFragment()

        // Animasyon için slide-in efekti
        val slideIn = android.R.anim.slide_in_left
        val slideOut = android.R.anim.slide_out_right

        // Fragment'ı container'a ekle
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(slideIn, slideOut)
            .replace(R.id.abacusFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    // Önbellek dosyası kullanıcıya özeldir; adını UserWalletFirestore belirler. Buradan
    // doğrudan "app_prefs" açmak, hesap değişiminde önceki kullanıcının bakiyesini
    // gösterilmesine yol açıyordu.
    fun saveCurrency(context: Context, value: Int) {
        UserWalletFirestore.cacheCurrency(context, value)
    }

    fun saveKeys(context: Context, value: Int) {
        UserWalletFirestore.cacheKeys(context, value)
    }

    fun getCurrency(context: Context): Int = UserWalletFirestore.getCachedCurrency(context)

    private fun refreshWalletFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        walletListenerRegistration?.remove()
        walletListenerRegistration = UserWalletFirestore.listenToWallet(
            context = this,
            uid = uid,
            onUpdate = { wallet ->
                if (::binding.isInitialized) {
                    applyWalletToUi(wallet)
                }
            }
        )
    }

    private fun applyWalletToUi(wallet: UserWallet) {
        binding.currencyText.text = wallet.currency.toString()
        binding.keyText.text = wallet.keys.toString()
        // Kredi yerel önbellekte tutulmuyor; değeri yalnızca bu canlı dinleyici besliyor.
        // Böylece sunucu tarafındaki değişiklikler (satın alma, soru gönderme, 48 saatlik
        // iade) panele kendiliğinden yansıyor.
        binding.creditText.text = wallet.questionCredits.toString()

        // Plan sunucuda değişmiş olabilir (abonelik yenilendi, iptal edildi, süresi doldu
        // ya da başka bir cihazdan işlem yapıldı). Bunların hiçbiri istemcide bir satın
        // alma geri çağrısı üretmez; tek haber kanalı bu dinleyicidir. Yalnızca gerçekten
        // değiştiğinde tam güncellemeyi çalıştırıyoruz ki her snapshot'ta gereksiz iş olmasın.
        if (wallet.plan != energyManager.getUserPlan()) {
            checkSubscriptionAndUpdateEnergy()
        }
    }

    fun refreshWalletUi() {
        binding.currencyText.text = UserWalletFirestore.getCachedCurrency(this).toString()
        binding.keyText.text = UserWalletFirestore.getCachedKeys(this).toString()
    }

    /**
     * Üst bardaki günlük seri göstergesini tazeler.
     *
     * Seri kırıkken alev gri ve sayı 0: "bugün yapılacak bir şey var" mesajı, hiç
     * göstermemekten daha etkili. Gizlemek yerine söndürmek, Duolingo ve Mimo'nun da
     * yaptığı şey.
     */
    fun refreshStreakUi() {
        if (!::binding.isInitialized) return
        // Yerel seri verisi cihaza ait, uid'ye değil. Oturumdaki hesap değiştiyse önceki
        // kullanıcının verisi burada siliniyor — yoksa yeni hesap onun serisini devralırdı.
        StreakRepository.bindToUser(this, auth.currentUser?.uid)
        val state = StreakRepository.refresh(this)
        // refresh() hedefi tutturan günü burada ilerletiyor; kutlama da aynı yerden
        // kuyruğa giriyor. Tek tazeleme noktası olduğu için kutlamayı denemenin doğru yeri de
        // burası: ekran dönüşleri, geri yığını değişimi ve süre değişimi hepsi buradan geçiyor.
        maybeShowStreakCelebration()
        // Ödüller sunucudaki sayaca bakıyor. Eşitleme burada tetikleniyor çünkü gün ders
        // ekranındayken tutturuluyor ve activity o sırada onResume'a girmiyor; kuyruk boşsa
        // ya da bir deneme sürüyorsa çağrı kendini eliyor.
        StreakSyncService.syncPendingDays(this)
        binding.streakText.text = state.current.toString()
        val alive = state.current > 0
        binding.streakIcon.alpha = if (alive) 1f else 0.45f
        binding.streakIcon.colorFilter = if (alive) {
            null
        } else {
            android.graphics.PorterDuffColorFilter(
                android.graphics.Color.parseColor("#78909C"),
                android.graphics.PorterDuff.Mode.SRC_IN,
            )
        }
        binding.streakText.setTextColor(
            android.graphics.Color.parseColor(if (alive) "#FF9800" else "#78909C"),
        )
    }


    /**
     * Billing geri çağrılarını varsayılan (ekran bağımsız) haline döndürür.
     *
     * ShopFragment açıkken kendi geri çağrılarını kurup animasyon/fiyat gösterir; kapanırken
     * bu metodu çağırarak devri geri verir. Böylece mağaza kapalıyken tamamlanan bir satın alma
     * yine de bakiyeyi tazeler.
     */
    fun installDefaultBillingCallbacks() {
        if (!::billingManager.isInitialized) return
        billingManager.onPricesReady = null
        billingManager.onError = null
        billingManager.onPurchaseGranted = { resyncWalletFromServer() }
        billingManager.onSubscriptionUpdated = {
            checkSubscriptionAndUpdateEnergy()
            // Kredi bu doğrulamada verilmiş olabilir; cihaz artık uygun olmayabilir.
            WelcomeCreditEligibility.refresh(applicationContext)
        }
    }

    /**
     * Sunucu bir cüzdan işlemini reddettiğinde (ödül üst sınırı, saatlik tavan, yetersiz bakiye)
     * iyimser olarak yazdığımız değer yanlış kalır. Gerçek bakiyeyi Firestore'dan tekrar çekip
     * hem önbelleği hem arayüzü düzeltir.
     */
    private fun resyncWalletFromServer() {
        val uid = auth.currentUser?.uid ?: return
        UserWalletFirestore.loadWallet(
            context = this,
            uid = uid,
            onResult = { wallet ->
                if (::binding.isInitialized) applyWalletToUi(wallet)
            },
        )
    }





    /**
     * Üst paneldeki elmas bakiyesinden düşer (Firestore `users.currency`).
     *
     * @param itemId Neye harcandığı; ölçüme aynen gider (bkz. [AnalyticsLogger.logGoldSpent]).
     */
    fun spendDiamonds(amount: Int, itemId: String): Boolean {
        if (amount <= 0) return true
        val current = binding.currencyText.text.toString().toIntOrNull()
            ?: UserWalletFirestore.getCachedCurrency(this)
        if (current < amount) return false
        val next = current - amount
        if (::binding.isInitialized) {
            binding.currencyText.text = next.toString()
        }
        saveCurrency(this, next)
        auth.currentUser?.uid?.let { uid ->
            UserWalletFirestore.applyCurrencyDelta(
                context = this,
                uid = uid,
                delta = -amount,
                reason = WalletReason.SPEND,
                itemId = itemId,
                onSuccess = { wallet -> applyWalletToUi(wallet) },
                onFailure = { resyncWalletFromServer() },
            )
        }
        return true
    }

    /**
     * Üst paneldeki anahtar bakiyesinden düşer (Firestore `users.keys`).
     *
     * @param itemId Neye harcandığı; ölçüme aynen gider (bkz. [AnalyticsLogger.logKeySpent]).
     */
    fun spendKeys(amount: Int, itemId: String): Boolean {
        if (amount <= 0) return true
        val current = binding.keyText.text.toString().toIntOrNull()
            ?: UserWalletFirestore.getCachedKeys(this)
        if (current < amount) return false
        val next = current - amount
        if (::binding.isInitialized) {
            binding.keyText.text = next.toString()
        }
        auth.currentUser?.uid?.let { uid ->
            UserWalletFirestore.applyKeyDelta(
                context = this,
                uid = uid,
                delta = -amount,
                reason = WalletReason.SPEND,
                itemId = itemId,
                onSuccess = { wallet -> applyWalletToUi(wallet) },
                onFailure = { resyncWalletFromServer() },
            )
        }
        return true
    }

    private fun updateEnergyDisplay(energy: Int) {
        // NOT: Burada checkSubscriptionAndUpdateEnergy() ÇAĞIRMA — o fonksiyon Firestore'dan
        // veri çekip energyManager.setUserPlan()/setUserRoleApproval() çağırıyor, onlar da bu
        // callback'i (energyUpdateCallback) tekrar tetikliyor. Böyle bir çağrı sonsuz (ve her
        // adımda dallanan) bir Firestore fetch döngüsüne yol açıp uygulamayı kilitliyordu.
        // Burada sadece zaten bilinen yerel duruma göre UI'ı güncelliyoruz.
        if (!::binding.isInitialized) return
        EnergyDisplay.apply(
            text = binding.energyText,
            infiniteBadge = binding.energyInfiniteBadge,
            icon = binding.energyIcon,
            isInfinite = !energyManager.isEnergyBlocked() && energyManager.isInfiniteEnergy(),
            isPremium = PlanStatus.isProPlan(energyManager.getUserPlan()),
            value = if (energyManager.isEnergyBlocked()) {
                "0/${energyManager.getMaxEnergy()}"
            } else {
                "$energy/${energyManager.getMaxEnergy()}"
            },
        )
    }
    
    fun checkSubscriptionAndUpdateEnergy() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Kullanıcı giriş yapmamış, normal gösterim
            updateEnergyDisplay(energyManager.getCurrentEnergy())
            return
        }
        
        // Firestore'dan abonelik durumunu kontrol et
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // planExpiresAt: doğrulanmış Play aboneliğinin bitiş zamanı (ms).
                    // Süresi geçmişse kullanıcı Free'ye düşer — abonelik iptal edildiğinde
                    // sunucudan yeni bir doğrulama gelmese bile Pro süresiz kalmasın diye.
                    val storedPlan = doc.getString("plan") ?: "Free"
                    val planExpiresAt = doc.getLong("planExpiresAt")
                    val planExpired = planExpiresAt != null && planExpiresAt < System.currentTimeMillis()
                    val plan = if (planExpired) "Free" else storedPlan
                    val role = doc.getString("role") ?: ""
                    val teacherApproved = doc.getBoolean("teacherApproved") == true
                    GlobalValues.isTeacherApproved = teacherApproved
                    energyManager.setUserPlan(plan)
                    energyManager.setUserRoleApproval(role, teacherApproved)

                    // Onaysız öğretmende enerji her zaman 0'dır ve sonsuz sayılmaz;
                    // bu yüzden kilit kontrolü sonsuzdan önce gelir.
                    EnergyDisplay.apply(
                        text = binding.energyText,
                        infiniteBadge = binding.energyInfiniteBadge,
                        icon = binding.energyIcon,
                        isInfinite = !energyManager.isEnergyBlocked() && energyManager.isInfiniteEnergy(),
                        isPremium = PlanStatus.isProPlan(energyManager.getUserPlan()),
                        value = if (energyManager.isEnergyBlocked()) {
                            "0/${energyManager.getMaxEnergy()}"
                        } else {
                            "${energyManager.getCurrentEnergy()}/${energyManager.getMaxEnergy()}"
                        },
                    )
                } else {
                    // Firestore'da kayıt yok, normal gösterim
                    updateEnergyDisplay(energyManager.getCurrentEnergy())
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Abonelik durumu kontrol edilemedi", e)
                // Hata durumunda normal gösterim
                updateEnergyDisplay(energyManager.getCurrentEnergy())
            }
    }
    
    fun getEnergyManager(): EnergyManager {
        return energyManager
    }

    fun isInfiniteEnergy(): Boolean {
        return energyManager.isInfiniteEnergy()
    }

    private fun attachSeasonLeaderboardPendingListenerIfLoggedIn() {
        seasonLeaderboardPendingListener?.remove()
        seasonLeaderboardPendingListener = null
        val uid = auth.currentUser?.uid ?: return
        val docRef = firestore.collection("users").document(uid)
            .collection("badgeProgress").document("state")
        seasonLeaderboardPendingListener = docRef.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener
            if (snap == null || !snap.exists()) {
                val cur = BadgeProgressRepository.getUserBadgeProgress()
                if (cur.pendingLeaderboardRewardSeason != null) {
                    BadgeProgressRepository.update(cur.copy(pendingLeaderboardRewardSeason = null))
                }
                return@addSnapshotListener
            }
            val p = snap.pendingLeaderboardRewardSeasonFromDoc()
            val cur = BadgeProgressRepository.getUserBadgeProgress()
            if (cur.pendingLeaderboardRewardSeason != p) {
                BadgeProgressRepository.update(cur.copy(pendingLeaderboardRewardSeason = p))
            }
            if (p != null) {
                tryShowSeasonLeaderboardRewardGateIfNeeded()
            }
        }
    }

    private fun refreshPendingSeasonLeaderboardRewardFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        val docRef = firestore.collection("users").document(uid).collection("badgeProgress").document("state")
        fun applySnap(snap: com.google.firebase.firestore.DocumentSnapshot) {
            if (!snap.exists()) return
            val p = snap.pendingLeaderboardRewardSeasonFromDoc()
            val cur = BadgeProgressRepository.getUserBadgeProgress()
            BadgeProgressRepository.update(cur.copy(pendingLeaderboardRewardSeason = p))
            tryShowSeasonLeaderboardRewardGateIfNeeded()
        }
        docRef.get(Source.SERVER)
            .addOnSuccessListener { snap -> applySnap(snap) }
            .addOnFailureListener {
                docRef.get(Source.CACHE).addOnSuccessListener { snap -> applySnap(snap) }
            }
    }

    /** [Fragment.getContainerId] eski classpath'te Kotlin'den görünmeyebilir; yoksa [Fragment.getId] kullanılır. */
    private fun fragmentHostContainerIdForSeasonGateRetry(f: Fragment): Int {
        return try {
            val m = Fragment::class.java.getMethod("getContainerId")
            m.invoke(f) as Int
        } catch (_: Throwable) {
            f.id
        }
    }

    /** Ders/sandık akışı açıkken sezon liderlik ödülü kapısı gösterilmez; akış bitince tekrar denenir. */
    internal fun isBlockingLessonOverlayFragment(f: Fragment?): Boolean =
        fragmentBlocksSeasonLeaderboardGate(f)

    /**
     * Harita kartındaki progress halkası animasyonu yalnızca kullanıcı haritayı gerçekten görürken tüketilmeli.
     * Overlay (Chest, görev paneli vb.) açıkken [MapFragment.onResume] erken tüketim yapmasın.
     */
    fun shouldConsumeLessonProgressAnimationsOnMap(): Boolean {
        if (!::binding.isInitialized) return false
        val fm = supportFragmentManager
        if (fm.findFragmentById(R.id.fragmentContainerID) !is MapFragment) return false
        val abacusHostVisible = binding.abacusFragmentContainer.visibility == View.VISIBLE
        if (abacusHostVisible) {
            val abacus = fm.findFragmentById(R.id.abacusFragmentContainer)
            if (isBlockingLessonOverlayFragment(abacus)) return false
        }
        val resultHostVisible = binding.resultFragmentContainer.visibility == View.VISIBLE
        if (resultHostVisible) {
            val result = fm.findFragmentById(R.id.resultFragmentContainer)
            if (isBlockingLessonOverlayFragment(result)) return false
        }
        return true
    }

    internal fun isForcingAbacusOverlayDismiss(): Boolean =
        forcingAbacusOverlayDismissForSeasonGate

    private fun fragmentBlocksSeasonLeaderboardGate(f: Fragment?): Boolean = when (f) {
        is TutorialFragment,
        is AbacusFragment,
        is BlindingLessonFragment,
        is LessonResult,
        is LessonResultFalse,
        is ChestResult,
        is MissionChestRewardFragment,
        is ChestFragment,
        is NewChestFragment,
        is RecordFragment,
        is CreateQuestionFragment,
        -> true
        else -> false
    }

    /** Map/abacus soru akışı: medya seçici, ekran kaydı veya CreateQuestion açıkken sezon kapısı gösterilmez. */
    private fun isQuestionAskFlowBlockingSeasonLeaderboardGate(): Boolean {
        if (!::binding.isInitialized) return false
        if (isQuestionRecordingInProgress) return true
        if (binding.recordingOverlayContainer.visibility == View.VISIBLE) return true
        val picker = supportFragmentManager.findFragmentByTag(QuestionMediaPickerDialogFragment.TAG)
        return picker != null && picker.isAdded
    }

    private fun maybeRequestSeasonGateAfterQuestionFlowStep() {
        if (!::binding.isInitialized) return
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) !is MapFragment) return
        if (isQuestionAskFlowBlockingSeasonLeaderboardGate()) return
        if (isSeasonLeaderboardRewardBlockedByLessonFlow()) return
        requestSeasonLeaderboardRewardGateIfPending()
    }

    private fun isSeasonLeaderboardRewardBlockedByLessonFlow(): Boolean {
        if (!::binding.isInitialized) return false
        if (isQuestionAskFlowBlockingSeasonLeaderboardGate()) return true
        val fm = supportFragmentManager
        val base = fm.findFragmentById(R.id.fragmentContainerID)
        val abacus = fm.findFragmentById(R.id.abacusFragmentContainer)
        val result = fm.findFragmentById(R.id.resultFragmentContainer)
        val createQuestion = fm.findFragmentById(R.id.createQuestionOverlayContainer)
        // Ders overlay'i kapalıyken (GONE) FM'de hayalet Abacus kalabiliyor; haritadayken sezon kapısını kilitleme.
        val abacusHostVisible = binding.abacusFragmentContainer.visibility == View.VISIBLE
        val resultHostVisible = binding.resultFragmentContainer.visibility == View.VISIBLE
        val createQuestionHostVisible = binding.createQuestionOverlayContainer.visibility == View.VISIBLE
        val abacusBlocks = abacusHostVisible && fragmentBlocksSeasonLeaderboardGate(abacus)
        val resultBlocks = resultHostVisible && fragmentBlocksSeasonLeaderboardGate(result)
        val createQuestionBlocks =
            createQuestionHostVisible && createQuestion is CreateQuestionFragment
        val createQuestionInAbacus =
            abacusHostVisible && abacus is CreateQuestionFragment
        // activity_main'de coordinator (harita) result/abacus'tan sonra çiziliyor; sandık akışı bitip
        // haritaya dönünce ChestFragment FM'de result'ta kalabiliyor ama görünmüyor — kapıyı sürekli kilitlemesin.
        if (base is MapFragment) {
            return abacusBlocks || createQuestionBlocks || createQuestionInAbacus
        }
        return abacusBlocks || resultBlocks || createQuestionBlocks || createQuestionInAbacus
    }

    /**
     * [findFragmentById] bazen kapıyı kaçırabiliyor; [findFragmentByTag] ile yedeklenir.
     * Container GONE kalsa bile FM'de instance kalabilir; görünürlük burada kontrol edilmez.
     */
    private fun seasonLeaderboardRewardGateFragmentMatching(pending: Int): SeasonLeaderboardRewardGateFragment? {
        if (!::binding.isInitialized) return null
        val fm = supportFragmentManager
        fm.executePendingTransactions()
        val f = (
            fm.findFragmentById(R.id.seasonLeaderboardRewardGateContainer)
                ?: fm.findFragmentByTag(SeasonLeaderboardRewardGateFragment.TAG)
        ) as? SeasonLeaderboardRewardGateFragment ?: return null
        if (!f.isAdded || f.view == null) return null
        val argSeason = f.arguments?.getInt(SeasonLeaderboardRewardGateFragment.ARG_SEASON) ?: return null
        return if (argSeason == pending) f else null
    }

    private fun isSeasonLeaderboardRewardGateAlreadyShowing(pending: Int): Boolean =
        seasonLeaderboardRewardGateFragmentMatching(pending) != null

    private fun ensureSeasonLeaderboardRewardGateContainerVisible() {
        binding.seasonLeaderboardRewardGateContainer.visibility = View.VISIBLE
        (binding.root as? android.view.ViewGroup)?.bringChildToFront(binding.seasonLeaderboardRewardGateContainer)
    }

    /**
     * Tek runnable içinde [replace] + [commitNowAllowingStateLoss]: eski sezon / yarışlı add+evict döngüsünü kaldırır.
     * [isSeasonLeaderboardRewardGateAlreadyShowing] ile Map onResume + snapshot çift [replace] önlenir.
     */
    private fun commitSeasonLeaderboardRewardGateIfNeededNow() {
        if (isFinishing || !::binding.isInitialized) return
        if (auth.currentUser == null) return
        if (authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER) return
        val pending = BadgeProgressRepository.getUserBadgeProgress().pendingLeaderboardRewardSeason
        if (pending == null) return
        if (isSeasonLeaderboardRewardBlockedByLessonFlow()) return
        if (isSeasonLeaderboardRewardGateAlreadyShowing(pending)) {
            ensureSeasonLeaderboardRewardGateContainerVisible()
            return
        }
        val fm = supportFragmentManager
        fm.executePendingTransactions()
        ensureSeasonLeaderboardRewardGateContainerVisible()
        fm.beginTransaction()
            .replace(
                R.id.seasonLeaderboardRewardGateContainer,
                SeasonLeaderboardRewardGateFragment.newInstance(pending),
                SeasonLeaderboardRewardGateFragment.TAG,
            )
            .commitNowAllowingStateLoss()
    }

    private fun tryShowSeasonLeaderboardRewardGateIfNeeded() {
        if (!::binding.isInitialized) return
        if (auth.currentUser == null) {
            binding.root.removeCallbacks(seasonLeaderboardGateCommitRunnable)
            return
        }
        if (authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER) {
            binding.root.removeCallbacks(seasonLeaderboardGateCommitRunnable)
            return
        }
        val pending = BadgeProgressRepository.getUserBadgeProgress().pendingLeaderboardRewardSeason
        if (pending == null) {
            binding.root.removeCallbacks(seasonLeaderboardGateCommitRunnable)
            return
        }
        if (isSeasonLeaderboardRewardBlockedByLessonFlow()) {
            binding.root.removeCallbacks(seasonLeaderboardGateCommitRunnable)
            return
        }
        if (isSeasonLeaderboardRewardGateAlreadyShowing(pending)) {
            binding.root.removeCallbacks(seasonLeaderboardGateCommitRunnable)
            ensureSeasonLeaderboardRewardGateContainerVisible()
            return
        }
        binding.root.removeCallbacks(seasonLeaderboardGateCommitRunnable)
        binding.root.post(seasonLeaderboardGateCommitRunnable)
    }

    fun requestSeasonLeaderboardRewardGateIfPending() {
        val pending = BadgeProgressRepository.getUserBadgeProgress().pendingLeaderboardRewardSeason
        if (pending != null && isSeasonLeaderboardRewardGateAlreadyShowing(pending)) {
            ensureSeasonLeaderboardRewardGateContainerVisible()
            return
        }
        tryShowSeasonLeaderboardRewardGateIfNeeded()
    }

    /**
     * Harita [fragmentContainerID] üzerindeyken [abacusFragmentContainer] içinde kalan hayalet ders fragment'ı
     * ve üstteki ders [popBackStack] girişlerini sınırlı adımda temizler; host GONE + sezon kapısı dener.
     * ChestResult (skip sandık) ve RecordFragment kapanışında çağrılır.
     */
    /**
     * [abacusFragmentContainer] üzerinde tam ekran overlay (Görevler, ders vb.).
     * Haritadan [reconcileAbacusOverlayWhenMapIsBase] sonrası host GONE kalmış olabilir — her açılışta VISIBLE.
     */
    fun showAbacusOverlayFragment(
        fragment: Fragment,
        configure: (androidx.fragment.app.FragmentTransaction.() -> Unit)? = null,
    ) {
        if (!::binding.isInitialized) return
        practiceOverlayDismissRunnable?.let { binding.root.removeCallbacks(it) }
        practiceOverlayDismissRunnable = null
        forcingAbacusOverlayDismissForSeasonGate = false
        binding.abacusFragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .replace(R.id.abacusFragmentContainer, fragment)
            .apply { configure?.invoke(this) }
            // addToBackStack yok: harita ders stack'ine pop ile dönülmesin; kapanış → [finishOverlayReturnToTasks].
            .commitAllowingStateLoss()
        logTouchDiag("showAbacusOverlayFragment:${fragment.javaClass.simpleName}")
    }

    /** Quit / overlay kapanmadan önce çağır: popBackStack Abacus'u geri getirmeden host GONE + zorunlu reconcile. */
    fun beginAbacusOverlayDismissForSeasonGate() {
        if (!::binding.isInitialized) return
        forcingAbacusOverlayDismissForSeasonGate = true
        binding.abacusFragmentContainer.visibility = View.GONE
    }

    /** Görevler kartı / günlük soru: [android.R.id.content] üstündeki geçici tam ekran blocker. */
    fun releasePracticeTouchBlocker() {
        findViewById<android.view.ViewGroup>(android.R.id.content)
            ?.findViewWithTag<android.view.View>(PRACTICE_TOUCH_BLOCKER_TAG)
            ?.let { blocker -> (blocker.parent as? android.view.ViewGroup)?.removeView(blocker) }
    }

    /**
     * Görevler pratik abaküs overlay'i: sağa kayarak kapanış ([showAbacusOverlayFragment] girişinin tersi).
     */
    fun finishAbacusPracticeOverlayAnimated(caller: String) {
        finishTasksOverlayAnimated(caller)
    }

    /** Görevler overlay'i (abaküs pratik, günlük soru / BlindingLesson) sağa kayarak kapatır. */
    fun finishTasksOverlayAnimated(caller: String) {
        if (!::binding.isInitialized) return
        practiceOverlayDismissRunnable?.let { binding.root.removeCallbacks(it) }
        practiceOverlayDismissRunnable = null
        val fm = supportFragmentManager
        fm.executePendingTransactions()
        val overlay = fm.findFragmentById(R.id.abacusFragmentContainer)
        val tasksFragment = fm.findFragmentById(R.id.fragmentContainerID) as? TasksFragment
        val overlayToRemove = when (overlay) {
            is AbacusPracticeFragment, is BlindingLessonFragment, is FeedbackFragment, is NewChestFragment,
            is CupPathRoadFragment -> overlay
            else -> null
        }
        if (tasksFragment == null || overlayToRemove == null) {
            finishOverlayReturnToTasks(caller)
            return
        }
        logTouchDiag("finishTasksOverlayAnimated.BEFORE:$caller")
        val tx = fm.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
        if (tasksFragment.isHidden) {
            tx.show(tasksFragment)
        }
        tx.remove(overlayToRemove).commitAllowingStateLoss()
        val completeRunnable = Runnable {
            practiceOverlayDismissRunnable = null
            completeTasksOverlayDismiss("finishTasksOverlayAnimated:$caller")
        }
        practiceOverlayDismissRunnable = completeRunnable
        binding.root.postDelayed(completeRunnable, practiceOverlayExitAnimMs)
    }

    /**
     * Görevler sekmesinden açılan abacus overlay (günlük soru vb.) kapandığında:
     * host GONE, back stack, Tasks show, touch blocker ve chrome kilidi.
     */
    fun finishOverlayReturnToTasks(caller: String) {
        if (!::binding.isInitialized) return
        practiceOverlayDismissRunnable?.let { binding.root.removeCallbacks(it) }
        practiceOverlayDismissRunnable = null
        logTouchDiag("finishOverlayReturnToTasks.BEFORE:$caller")
        val fm = supportFragmentManager
        fm.executePendingTransactions()
        beginAbacusOverlayDismissForSeasonGate()
        purgeAbacusOverlayHosts("finishOverlayReturnToTasks:$caller")
        val tasks = fm.findFragmentById(R.id.fragmentContainerID)
        if (tasks is TasksFragment && tasks.isHidden) {
            fm.beginTransaction().show(tasks).commitAllowingStateLoss()
            fm.executePendingTransactions()
        }
        binding.abacusFragmentContainer.visibility = View.GONE
        binding.root.post {
            completeTasksOverlayDismiss("finishOverlayReturnToTasks:$caller")
        }
    }

    private fun completeTasksOverlayDismiss(caller: String) {
        checkAndShowInterstitialAdIfAllowed("completeTasksOverlayDismiss") {
            if (!::binding.isInitialized) return@checkAndShowInterstitialAdIfAllowed
            forcingAbacusOverlayDismissForSeasonGate = false
            binding.abacusFragmentContainer.visibility = View.GONE
            dismissMapLessonOverlayChrome()
            releasePracticeTouchBlocker()
            releaseLessonActionTouchBlocker()
            ensureChromeUnlockedAfterOverlayDismiss(caller)
            logTouchDiag("completeTasksOverlayDismiss:$caller")
        }
    }

    /** [TasksFragment.onResume] için: FM transaction bitince [reconcileAbacusOverlayWhenTasksIsBase] (Map [view.post] ile aynı mantık). */
    fun scheduleReconcileAbacusOverlayWhenTasksIsBase() {
        if (!::binding.isInitialized) return
        binding.root.post {
            reconcileAbacusOverlayWhenTasksIsBase("schedule.tasks")
        }
    }

    /** Görevler tabanındayken boş / hayalet abacus host ve blocker temizliği. */
    fun reconcileAbacusOverlayWhenTasksIsBase(caller: String = "reconcile.tasks") {
        if (!::binding.isInitialized) return
        logTouchDiag("reconcileAbacusOverlayWhenTasksIsBase.BEFORE:$caller")
        val fm = supportFragmentManager
        if (fm.findFragmentById(R.id.fragmentContainerID) !is TasksFragment) return
        fm.executePendingTransactions()
        val active = fm.findFragmentById(R.id.abacusFragmentContainer)
        val hostVisible = binding.abacusFragmentContainer.visibility == View.VISIBLE
        if (active is AbacusPracticeFragment && hostVisible) {
            Log.d(MainActivityTouchDiag.LOG_TAG, "[$caller] skip — active AbacusPractice")
            return
        }
        purgeAbacusOverlayHosts(caller)
        val tasks = fm.findFragmentById(R.id.fragmentContainerID)
        if (tasks is TasksFragment && tasks.isHidden) {
            fm.beginTransaction().show(tasks).commitAllowingStateLoss()
            fm.executePendingTransactions()
        }
        binding.abacusFragmentContainer.visibility = View.GONE
        forcingAbacusOverlayDismissForSeasonGate = false
        dismissMapLessonOverlayChrome()
        releasePracticeTouchBlocker()
        releaseLessonActionTouchBlocker()
        ensureChromeUnlockedAfterOverlayDismiss("reconcile.tasks:$caller")
        logTouchDiag("reconcileAbacusOverlayWhenTasksIsBase.AFTER:$caller")
    }

    private fun ensureChromeUnlockedAfterOverlayDismiss(caller: String) {
        MainActivityChromeBlocker.ensureUnlockedForMapReturn(this, blockingOverlayStillActive = false)
        logChromeBlockerDiagnostic(caller)
    }

    /**
     * Overlay kapanışı: abacus/result host'larını purge eder, harita UI'ını geri yükler.
     * [finalizeMapReturnAfterLessonClaim] ile eşleşir.
     *
     * Çağırmadan önce çağıran overlay fragment kendini FM'den ayırmalı (remove/pop);
     * aksi halde purge sonrası `parentFragmentManager` → IllegalStateException.
     */
    fun prepareMapReturnAfterLessonClaim() {
        if (!::binding.isInitialized) return
        logMapTouchDiag("prepareMapReturn", "ENTER", "forcingDismiss=true host→GONE hedefleniyor")
        // [canConsumePendingLessonProgressAnimations] + [LessonManager.refreshLessonsFromGlobalData] burada
        // çağrılmasın: Chest / MissionChest / LessonResult overlay altında Map RV yenilenince progress animasyonu
        // ekranda görülmeden biter. Tüketim + tam liste yenileme → [MapFragment.notifyVisibleAfterOverlayDismiss]
        // ve [MapFragment.onResume] (finalize → reconcile sonrası).
        activeMapTutorialOverlayFromLesson = false
        claimPathScheduledSeasonGate = true
        beginAbacusOverlayDismissForSeasonGate()
        binding.resultFragmentContainer.visibility = View.GONE
        val base = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        if (base is MapFragment || base is PartSelectionFragment) {
            purgeAbacusOverlayHosts("prepareMapReturnAfterLessonClaim")
            restoreMapUiAfterLessonOverlayDismiss()
        }
    }

    /**
     * Asenkron gelen rozet payloads'larını sıraya ekler.
     * Reklam açıksa reklam kapandıktan sonra açar, reklam yoksa anında açar.
     */
    fun enqueuePendingBadgePayloads(
        badgePayloads: List<com.example.app.BadgeLevelUpPayload> = emptyList(),
        badgeStringPayloads: List<String> = emptyList()
    ) {
        if (badgePayloads.isNotEmpty()) pendingBadgePayloadsForAd = badgePayloads
        if (badgeStringPayloads.isNotEmpty()) pendingBadgeStringPayloadsForAd = badgeStringPayloads
        // Eskiden rozet burada DOĞRUDAN açılıyordu ve bu, kuyruktan habersiz ikinci bir
        // yoldu — yeni seri ekranının reklamın altında kalması gibi çakışmaların
        // kaynaklarından biri. Artık yalnızca bayrak yazılıp kuyruk dürtülüyor; açma
        // kararı tek yerde.
        pumpPostLessonQueue("enqueuePendingBadgePayloads")
    }

    /** [prepareMapReturnAfterLessonClaim] sonrası: chrome + sezon reconcile (tek kaynak). */
    fun finalizeMapReturnAfterLessonClaim(
        caller: String,
        badgePayloads: List<com.example.app.BadgeLevelUpPayload> = emptyList(),
        badgeStringPayloads: List<String> = emptyList(),
        // true: bu dönüş türü LESSON olan (chest hariç) bir item'ın claim'inden geliyor —
        // öğretmene sorma tanıtımı yalnızca bu durumda deneniyor.
        isLessonTypeReturn: Boolean = false,
        // false: bu çağrı bir ders/sandık bitişinden DEĞİL, başka bir overlay'in
        // kapanışından geliyor (bkz. RecordFragment). Ayrım şart: bu fonksiyon liderlik
        // tablosu kapatılınca da çağrılıyor ve o zaman "ders bitti" ekranları açılmamalı.
        fromLessonFinish: Boolean = true,
    ) {
        // Yeni tur sorusu burada KUYRUĞA giriyor, açılmıyor. Açma kararı kuyruğun
        // ([pumpPostLessonQueue]) — sırası gelip kapı açıldığı anda.
        if (fromLessonFinish && StreakRepository.needsNewStreakPrompt(this)) {
            newStreakPromptQueued = true
        }
        BadgeDiagnostics.log("MainActivity finalizeMapReturnAfterLessonClaim received payloads: badgePayloads=${badgePayloads.size}, badgeStringPayloads=${badgeStringPayloads.size}")
        logMapTouchDiag("finalizeMapReturn", "BEFORE", "caller=$caller")
        logTouchDiag("finalizeMapReturnAfterLessonClaim.BEFORE:$caller")
        // scheduleSeasonGate'in post'u bizim post'umuzdan ÖNCE çalışıp reconcile üzerinden
        // notifyMapVisibleAfterLessonClaim'i parametresiz tetikleyebiliyor; bayrağı burada
        // (senkron) biriktir ki hangi çağrı kazanırsa kazansın kaybolmasın.
        if (isLessonTypeReturn) {
            pendingLessonTypeReturnForPromo = true
            lockTouchForPendingAskQuestionPromo("finalizeMapReturn:$caller")
        }
        ensureChromeUnlockedAfterMapReturn(caller)
        scheduleSeasonGateAfterAbacusOverlayDismissed()
        // Kuyrukta gösterilecek bir şey varsa harita ERKENDEN kilitleniyor: ekranlar
        // asenkron gecikmelerle (reklam kontrolü, Firestore) geldiği için aradaki boşlukta
        // harita tıklanabilir kalıyordu. Kilidi kuyruk boşalınca bırakıyor
        // (bkz. runPostLessonQueue).
        //
        // Şart önceden yalnızca "rozet ya da rehber var mı" idi; yeni seri ya da rating
        // gösterilecekken kilit hiç uygulanmıyor ve iki pencere arasında harita açık
        // kalıyordu. Burada TEK acquire var — ChromeBlocker sayıcılı, ikinci bir kilit
        // noktası eklemek dengesizlik üretirdi.
        acquirePostLessonQueueTouchLock()
        binding.root.post {
            logMapTouchDiag("finalizeMapReturn", "AFTER_POST", "caller=$caller")
            logTouchDiag("finalizeMapReturnAfterLessonClaim.AFTER:$caller")
            notifyMapVisibleAfterLessonClaim("finalizeMapReturn:$caller", badgePayloads, badgeStringPayloads, isLessonTypeReturn)
            tryShowPendingMarathonGuideOnMap("finalizeMapReturn:$caller")
        }
    }

    /**
     * Reconcile atlanırsa bile harita listesini ve progress animasyon tüketimini günceller.
     * [MapFragment.notifyVisibleAfterOverlayDismiss] tek kaynak.
     */
    fun notifyMapVisibleAfterLessonClaim(
        caller: String,
        badgePayloads: List<com.example.app.BadgeLevelUpPayload> = emptyList(),
        badgeStringPayloads: List<String> = emptyList(),
        isLessonTypeReturn: Boolean = false,
    ) {
        BadgeDiagnostics.log("notifyMapVisibleAfterLessonClaim CALLED! caller=$caller, badgePayloads=${badgePayloads.size}, badgeStringPayloads=${badgeStringPayloads.size}, adCheckInProgress=$adCheckForBadgeInProgress")
        // Gelen payloads'ları biriktirir — hangi onDone tetiklenirse tetiklensin rozet kaybedilmez.
        if (badgePayloads.isNotEmpty()) pendingBadgePayloadsForAd = badgePayloads
        if (badgeStringPayloads.isNotEmpty()) pendingBadgeStringPayloadsForAd = badgeStringPayloads
        // Aynı gerekçe: erken return'e takılsak bile lesson dönüşü bilgisi kaybolmasın.
        if (isLessonTypeReturn) {
            pendingLessonTypeReturnForPromo = true
            lockTouchForPendingAskQuestionPromo("notifyMapVisible:$caller")
        }
        // Eğer reklam kontrolü zaten uçuştaysa ikinci çağrı sadece payload biriktirir, onDone tekrar tetiklenmez.
        if (adCheckForBadgeInProgress) {
            BadgeDiagnostics.log("notifyMapVisibleAfterLessonClaim SKIPPING checkAndShowInterstitialAdIfAllowed (ad already in progress), caller=$caller")
            return
        }
        adCheckForBadgeInProgress = true
        checkAndShowInterstitialAdIfAllowed("notifyMapVisibleAfterLessonClaim") {
            adCheckForBadgeInProgress = false
            if (!::binding.isInitialized) return@checkAndShowInterstitialAdIfAllowed
            // Bayraklar burada TÜKETİLMİYOR: artık her birini kuyruktaki kendi adımı
            // okuyup siliyor. Burada silinselerdi kuyruk sırası gelince gösterecek bir şey
            // bulamazdı.
            val resolvedBadgePayloads = pendingBadgePayloadsForAd
            val resolvedBadgeStringPayloads = pendingBadgeStringPayloadsForAd
            val resolvedLessonTypeReturn = pendingLessonTypeReturnForPromo
            BadgeDiagnostics.log("notifyMapVisibleAfterLessonClaim onDone: resolvedBadgePayloads=${resolvedBadgePayloads.size}, resolvedStringPayloads=${resolvedBadgeStringPayloads.size}")
            // Promo kilidi: promo HİÇ çalışmayacaksa burada bırakılıyor, yoksa harita
            // kilitli kalır. Eskiden rozet ya da rating varsa da bırakılıyordu çünkü o
            // durumda promo ATLANIYORDU; kuyrukta atlanmıyor, sırasını bekliyor.
            if (!resolvedLessonTypeReturn) {
                releaseAskQuestionPromoLock("noPromoBranch:$caller")
            }
            // Ne gösterileceğine ve hangi sırayla gösterileceğine tek yer karar veriyor.
            pumpPostLessonQueue("notifyMapVisible:$caller")
            val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
            if (map == null || !map.isAdded) {
                LessonProgressDiag.log("MainActivity.notifyMapVisible", "SKIP caller=$caller map=null or not added")
                return@checkAndShowInterstitialAdIfAllowed
            }
            LessonProgressDiag.log("MainActivity.notifyMapVisible", "SCHEDULE caller=$caller")
            val runNotify = Runnable {
                if (!map.isAdded) {
                    LessonProgressDiag.log("MainActivity.notifyMapVisible", "ABORT caller=$caller map detached")
                    return@Runnable
                }
                LessonProgressDiag.log("MainActivity.notifyMapVisible", "RUN caller=$caller")
                map.notifyVisibleAfterOverlayDismiss()
            }
            map.view?.post(runNotify) ?: runNotify.run()
        }
    }

    /** Harita tabanı görünür; ders/sandık/görev/rozet/sezon kapısı overlay'i yok. */
    fun marathonGuideMapBlockReason(): String? {
        if (!::binding.isInitialized) return "binding_not_initialized"
        val fm = supportFragmentManager
        val base = fm.findFragmentById(R.id.fragmentContainerID)
        if (base !is MapFragment) {
            return "base_not_map:${base?.javaClass?.simpleName ?: "null"}"
        }

        val abacusHostVisible = binding.abacusFragmentContainer.visibility == View.VISIBLE
        val abacus = fm.findFragmentById(R.id.abacusFragmentContainer)
        if (abacusHostVisible && abacus != null && isBlockingLessonOverlayFragment(abacus)) {
            return "abacus_overlay:${abacus.javaClass.simpleName} hostVisible=$abacusHostVisible"
        }

        val resultHostVisible = binding.resultFragmentContainer.visibility == View.VISIBLE
        val result = fm.findFragmentById(R.id.resultFragmentContainer)
        if (resultHostVisible && result != null && isBlockingLessonOverlayFragment(result)) {
            return "result_overlay:${result.javaClass.simpleName} hostVisible=$resultHostVisible"
        }

        val badge = fm.findFragmentById(R.id.badgeFragmentContainter)
        if (badge != null) {
            return "badge_overlay:${badge.javaClass.simpleName}"
        }

        // Kuyruğun açtığı yeni seri ekranı da burada listeli: listelenmeseydi rehber
        // onun üstüne açılırdı — ilk denemede tam bu oldu.
        if (dialogStillShowing(NewStreakFragment.TAG)) {
            return "new_streak_prompt"
        }

        if (GlobalValues.pendingBadgeFirestoreOperation) {
            return "badge_firestore_pending"
        }

        if (adCheckForBadgeInProgress) {
            return "ad_check_in_progress"
        }

        val gateVisible = binding.seasonLeaderboardRewardGateContainer.visibility == View.VISIBLE
        val gate = fm.findFragmentById(R.id.seasonLeaderboardRewardGateContainer)
        if (gateVisible && gate != null) {
            return "season_gate:${gate.javaClass.simpleName} hostVisible=$gateVisible"
        }
        return null
    }

    fun isMapBaseReadyForMarathonGuide(): Boolean {
        val block = marathonGuideMapBlockReason()
        if (block != null) {
            Log.d(MarathonGuideStore.LOG_TAG, "mapNotReady | block=$block")
            return false
        }
        Log.d(MarathonGuideStore.LOG_TAG, "mapReady | ok")
        return true
    }

    /**
     * [marathonGuideMapBlockReason]'a ek olarak, chest akışından (guide/rating/tasks yönlendirmesi)
     * kalan gecikmeli/kuyruktaki durumları da kapsar. null → harita tamamen temiz, AskQuestionOpenFragment
     * güvenle gösterilebilir.
     */
    private fun askQuestionPromoMapBlockReason(): String? {
        marathonGuideMapBlockReason()?.let { return it }
        if (GlobalValues.pendingCupPathRevealPartId != null) return "cup_path_pending"
        if (MarathonGuideStore.isPending(this)) return "guide_pending"
        if (justFinishedChestForRating) return "rating_pending"
        if (supportFragmentManager.findFragmentByTag("AdSkip") != null) return "ad_skip_showing"
        if (supportFragmentManager.findFragmentByTag("AskQuestionOpen") != null) return "already_showing"
        val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
        if (map != null && map.isAdded && map.view != null &&
            map.requireView().findViewById<View>(R.id.guidePanel)?.visibility == View.VISIBLE
        ) {
            return "guide_panel_visible"
        }
        return null
    }

    /**
     * Yalnızca AskQuestionOpen promosunu dener — harita dönüş akışının hiçbir parçasını
     * (rozet, rehber, rating, reklam, sezon kapısı, tasks yönlendirmesi) çalıştırmaz.
     * TutorialFragment → harita gibi, [finalizeMapReturnAfterLessonClaim]'in bilinçli olarak
     * çağrılmadığı yollar için.
     */
    fun tryShowAskQuestionPromoOnly(caller: String) {
        if (!::binding.isInitialized) return
        lockTouchForPendingAskQuestionPromo(caller)
        binding.root.post { maybeShowAskQuestionPromo(caller) }
    }

    /**
     * Pro/Premium olmayan, öğretmen olmayan, oturumu açık kullanıcı — VE bu cihaz hoş geldin
     * kredisini henüz almamış olmalı.
     *
     * Son koşulun sebebi: AskQuestionOpenFragment'ın vaadi öğretmene soru sormak. Cihaz
     * krediyi daha önce tükettiyse (aynı telefonda ikinci uygulama hesabı) kullanıcı denemeyi
     * başlatsa bile kredi almıyor — Pro oluyor ama soru soramıyor. O kullanıcıya bu tanıtımı
     * OTOMATİK açmak, ekranın tek işlevini teslim etmemek olur.
     *
     * Butonla açılan yolda ekran yine gösteriliyor ama düzeni değişiyor: büyük düğme Pro değil
     * kredi satın alma oluyor (bkz. AskQuestionOpenFragment.bindActions).
     *
     * Butona basarak açma yolu (AskQuestionButtonBinder) bu koşula tabi değil: orada niyet
     * kullanıcıdan geliyor ve ekranın "BUNUN YERİNE KREDİ AL" seçeneği ona doğru yolu
     * gösteriyor.
     */
    private fun isAskQuestionPromoEligible(): Boolean {
        if (FirebaseAuth.getInstance().currentUser == null) return false
        if (energyManager.getUserRole() == "TEACHER") return false
        val plan = energyManager.getUserPlan()
        if (plan == "Pro" || plan == "Premium") return false
        return WelcomeCreditEligibility.isEligible(applicationContext)
    }

    fun isAskQuestionPromoPending(): Boolean = askQuestionPromoPendingLock

    /**
     * Bu lesson dönüşünde promo gösterileceği sayaçtan belliyse haritayı daha reklam kontrolü
     * başlamadan kilitler — rozet/rehberdeki eager lock deseninin aynısı. Promo ekrana gelene
     * (ya da gelmeyeceği netleşene) kadar kilit [releaseAskQuestionPromoLock] ile açılmaz.
     */
    fun lockTouchForPendingAskQuestionPromo(caller: String) {
        if (!::binding.isInitialized || askQuestionPromoPendingLock) return
        if (!isAskQuestionPromoEligible()) return
        val nextCount = GlobalValues.peekAskQuestionPromoLessonReturnCount(this) + 1
        if (nextCount < ASK_QUESTION_PROMO_LESSON_RETURN_THRESHOLD) return

        askQuestionPromoPendingLock = true
        logMapTouchDiag("askQuestionPromo", "LOCK", "caller=$caller nextCount=$nextCount")
        (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment)
            ?.lockTouchForPendingOverlay()
        scheduleAskQuestionPromoLockWatchdog()
    }

    private fun releaseAskQuestionPromoLock(caller: String) {
        if (!askQuestionPromoPendingLock) return
        askQuestionPromoPendingLock = false
        logMapTouchDiag("askQuestionPromo", "UNLOCK", "caller=$caller")
        (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment)
            ?.enableMapTouchRouting()
    }

    private fun scheduleAskQuestionPromoLockWatchdog() {
        binding.root.postDelayed({
            if (!askQuestionPromoPendingLock) return@postDelayed
            // Reklam açıkken kilidi bırakmak, reklam kapanışıyla promo arasında tıklanabilir
            // pencere açar; kontrol bitene kadar bekle.
            if (adCheckForBadgeInProgress) {
                scheduleAskQuestionPromoLockWatchdog()
                return@postDelayed
            }
            releaseAskQuestionPromoLock("watchdog")
        }, ASK_QUESTION_PROMO_LOCK_WATCHDOG_MS)
    }

    /**
     * Pro olmayan/öğretmen olmayan kullanıcıya, türü LESSON olan (chest hariç) bir item'dan haritaya
     * her dönüşte 1 artan sayaç 3'e ulaşınca AskQuestionOpenFragment'ı otomatik gösterir.
     * Guide/rozet/rating/tasks yönlendirmesiyle üst üste binmesin diye [askQuestionPromoMapBlockReason]
     * ile kapılanır; harita o an temiz değilse sayaç sıfırlanmaz — sonraki lesson dönüşünde tekrar denenir.
     */
    private fun maybeShowAskQuestionPromo(caller: String) {
        if (!::binding.isInitialized) return
        if (!isAskQuestionPromoEligible()) {
            releaseAskQuestionPromoLock("notEligible:$caller")
            return
        }

        val count = GlobalValues.incrementAskQuestionPromoLessonReturnCount(this)
        if (count < ASK_QUESTION_PROMO_LESSON_RETURN_THRESHOLD) {
            releaseAskQuestionPromoLock("belowThreshold:$caller")
            return
        }
        tryShowAskQuestionPromo(caller, count, attempt = 0)
    }

    /**
     * Promoyu göstermeyi dener. Blok sebebi geçici olabileceği için (rozet Firestore kontrolü,
     * reklam kontrolü, görev paneli kapanışı) kısa aralıklarla tekrar denenir; harita temizlenir
     * temizlenmez promo ~[ASK_QUESTION_PROMO_RETRY_INTERVAL_MS] içinde açılır. Her denemede kapı
     * yeniden değerlendirildiği için gerçekten bir overlay açıldıysa (rozet/rehber/rating) promo
     * hiç gösterilmez, sayaç da sıfırlanmaz.
     */
    private fun tryShowAskQuestionPromo(caller: String, count: Int, attempt: Int) {
        if (!::binding.isInitialized) return
        // Gecikmeli denemede activity kapanmış/state kaydedilmiş olabilir; show() o durumda çöker.
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) {
            releaseAskQuestionPromoLock("activityGone:$caller")
            return
        }
        val blockReason = askQuestionPromoMapBlockReason()
        if (blockReason != null) {
            if (attempt >= ASK_QUESTION_PROMO_MAX_ATTEMPTS) {
                logMapTouchDiag("askQuestionPromo", "GIVE_UP", "caller=$caller reason=$blockReason count=$count")
                releaseAskQuestionPromoLock("retriesExhausted:$caller")
                return
            }
            if (attempt == 0) {
                logMapTouchDiag("askQuestionPromo", "WAIT", "caller=$caller reason=$blockReason count=$count")
            }
            binding.root.postDelayed(
                { tryShowAskQuestionPromo(caller, count, attempt + 1) },
                ASK_QUESTION_PROMO_RETRY_INTERVAL_MS,
            )
            return
        }
        GlobalValues.resetAskQuestionPromoLessonReturnCount(this)
        logMapTouchDiag("askQuestionPromo", "SHOW", "caller=$caller count=$count attempt=$attempt")
        AskQuestionOpenFragment
            .newInstance(AnalyticsLogger.PROMO_TRIGGER_AUTO)
            .show(supportFragmentManager, "AskQuestionOpen")
        // Dialog penceresi bir sonraki frame'de öne gelir; kilidi ondan önce bırakma.
        binding.root.post { releaseAskQuestionPromoLock("shown:$caller") }
    }

    /**
     * Eskiden yalnızca maraton rehberini denerdi; artık ders sonrası kuyruğunu dürtüyor.
     *
     * Adı ve imzası korundu: bu fonksiyon DOKUZ ayrı yerden çağrılıyor (MapFragment,
     * BadgeFragment, ChestFragment, ChestResult…) ve hepsi aslında aynı şeyi söylüyor:
     * "bir şeyler yatıştı, sırada ne varsa gösterilebilir". Dokuz çağrı yerini
     * değiştirmek yerine anlamı tek yerde genişletmek hem daha az riskli hem daha doğru.
     */
    fun tryShowPendingMarathonGuideOnMap(caller: String) {
        pumpPostLessonQueue(caller)
    }

    /** LessonResult → ChestFragment: result overlay host'u görünür yap. */
    fun showResultOverlayHost() {
        if (!::binding.isInitialized) return
        binding.resultFragmentContainer.visibility = View.VISIBLE
    }

    /** Quit / ders overlay kapanınca: bir sonraki karede reconcile ve sezon kapısı. [beginAbacusOverlayDismissForSeasonGate] ayrı çağrılır. */
    fun scheduleSeasonGateAfterAbacusOverlayDismissed() {
        if (!::binding.isInitialized) return
        logMapTouchDiag("scheduleSeasonGate", "SCHEDULED", "post→reconcile")
        logChromeBlockerDiagnostic("scheduleSeasonGate.enter")
        pendingSeasonGateReconcileRunnable?.let { binding.root.removeCallbacks(it) }
        val runnable = Runnable {
            pendingSeasonGateReconcileRunnable = null
            try {
                reconcileAbacusOverlayWhenMapIsBase()
                ensureChromeUnlockedAfterMapReturn("scheduleSeasonGate.afterReconcile")
                requestSeasonLeaderboardRewardGateIfPending()
                logMapTouchDiag("scheduleSeasonGate", "POST_DONE", "reconcile bitti")
            } finally {
                forcingAbacusOverlayDismissForSeasonGate = false
                logChromeBlockerDiagnostic("scheduleSeasonGate.post.done")
            }
        }
        pendingSeasonGateReconcileRunnable = runnable
        binding.root.post(runnable)
    }

    /** Bekleyen sezon reconcile'ı iptal et (sheet'ten overlay açılırken çalıştırma — Record'u siler). */
    fun cancelPendingSeasonGateReconcile() {
        pendingSeasonGateReconcileRunnable?.let { binding.root.removeCallbacks(it) }
        pendingSeasonGateReconcileRunnable = null
    }

    /**
     * Bottom sheet'ten Record/Abacus: bekleyen gate iptal, FM boşalınca transaction.
     */
    fun runAbacusOverlayTransaction(caller: String, block: () -> Unit) {
        if (!::binding.isInitialized) {
            block()
            return
        }
        cancelPendingSeasonGateReconcile()
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) is MapFragment) {
            forcingAbacusOverlayDismissForSeasonGate = false
        }
        lessonSheetOverlayNavigationDepth++
        binding.root.post {
            try {
                supportFragmentManager.executePendingTransactions()
                android.util.Log.d("LessonFlowDbg", "[$caller] overlay tx")
                block()
            } finally {
                lessonSheetOverlayNavigationDepth =
                    (lessonSheetOverlayNavigationDepth - 1).coerceAtLeast(0)
            }
        }
    }

    fun logChromeBlockerDiagnostic(caller: String) {
        MainActivityChromeBlocker.logDiagnostic(caller, this)
    }

    /**
     * ChestResult [onDestroyView] → [release] atlanırsa alt bar kilitli kalır; Map tabanında güvenlik ağı.
     */
    fun ensureChromeUnlockedAfterMapReturn(caller: String) {
        if (!::binding.isInitialized) return
        val fm = supportFragmentManager
        val base = fm.findFragmentById(R.id.fragmentContainerID)
        if (base !is MapFragment && base !is PartSelectionFragment) return
        fm.executePendingTransactions()
        val abacus = fm.findFragmentById(R.id.abacusFragmentContainer)
        val blockingOverlay =
            binding.abacusFragmentContainer.visibility == View.VISIBLE &&
                abacus != null &&
                fragmentBlocksSeasonLeaderboardGate(abacus)
        MainActivityChromeBlocker.ensureUnlockedForMapReturn(this, blockingOverlay)
    }

    /** Map [onResume]: yalnızca overlay host açık veya abacus'ta fragment varken reconcile (soğuk açılışta Map stack'ine dokunma). */
    fun shouldReconcileAbacusOverlayOnMapResume(): Boolean {
        if (!::binding.isInitialized) return false
        if (firstTutorialOverlayBootstrapActive) return false
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) !is MapFragment) return false
        if (forcingAbacusOverlayDismissForSeasonGate) return true
        val orphan = supportFragmentManager.findFragmentById(R.id.abacusFragmentContainer)
        val result = binding.abacusFragmentContainer.visibility == View.VISIBLE ||
            (orphan != null && fragmentBlocksSeasonLeaderboardGate(orphan))
        logFirstTutorial(
            "shouldReconcileOnMapResume",
            "result=$result orphan=${orphan?.javaClass?.simpleName} ${overlaySnapshot("shouldReconcile")}",
        )
        return result
    }

    fun reconcileAbacusOverlayWhenMapIsBase() {
        logMapTouchDiag("reconcile", "ENTER", "forcingDismiss=$forcingAbacusOverlayDismissForSeasonGate")
        logFirstTutorial("reconcile.enter", overlaySnapshot("reconcile.enter"))
        if (!::binding.isInitialized) return
        val fm = supportFragmentManager
        fm.executePendingTransactions()
        if (fm.findFragmentById(R.id.fragmentContainerID) !is MapFragment) {
            logMapTouchDiag("reconcile", "SKIP_NOT_MAP", "")
            logFirstTutorial("reconcile.skip", "base is not MapFragment")
            return
        }
        if (lessonSheetOverlayNavigationDepth > 0) {
            logMapTouchDiag("reconcile", "SKIP_LESSON_SHEET_DEPTH", "depth=$lessonSheetOverlayNavigationDepth")
            logFirstTutorial("reconcile.skip", "lessonSheetOverlayNavigationDepth>0")
            return
        }
        // BUG FIX: LessonResult.claimButton'ın GO_TO_CHEST dalı, ChestFragment'i resultFragmentContainer'a
        // ekleyip LessonResult'ı abacusFragmentContainer'dan kaldırıyor (senkron commit). Bu kaldırma bazen
        // MapFragment.onResume()'u (→ bu fonksiyonu) tetikliyor; o anda abacusFragmentContainer boş olduğu için
        // aşağıdaki activeOverlay kontrolü hiçbir şey yakalamıyordu ve henüz kullanıcı görmeden/tıklamadan
        // resultFragmentContainer (ChestFragment) GONE yapılıp direkt haritaya dönülüyordu — ders ilerlemesi
        // hiç kaydedilmeden. resultFragmentContainer'ı da abacusFragmentContainer ile aynı şekilde kontrol et.
        if (!forcingAbacusOverlayDismissForSeasonGate) {
            val resultOverlay = fm.findFragmentById(R.id.resultFragmentContainer)
            val resultHostVisible = binding.resultFragmentContainer.visibility == View.VISIBLE
            if (resultOverlay != null && resultHostVisible && fragmentBlocksSeasonLeaderboardGate(resultOverlay)) {
                logMapTouchDiag(
                    "reconcile",
                    "SKIP_BLOCKING_OVERLAY",
                    "active=${resultOverlay.javaClass.simpleName} (resultFragmentContainer) hostVisible=true → notifyVisible YOK",
                )
                LessonProgressDiag.log(
                    "MainActivity.reconcile",
                    "SKIP_BLOCKING_OVERLAY active=${resultOverlay.javaClass.simpleName} (resultFragmentContainer) forcingDismiss=$forcingAbacusOverlayDismissForSeasonGate",
                )
                tryShowSeasonLeaderboardRewardGateIfNeeded()
                return
            }
        }
        val activeOverlay = fm.findFragmentById(R.id.abacusFragmentContainer)
        if (activeOverlay is RecordFragment &&
            binding.abacusFragmentContainer.visibility == View.VISIBLE
        ) {
            return
        }
        // Aktif ders overlay (host görünür) — hayalet TutorialFragment hariç atlama.
        if (!forcingAbacusOverlayDismissForSeasonGate &&
            activeOverlay != null &&
            fragmentBlocksSeasonLeaderboardGate(activeOverlay)
        ) {
            val hostVisible = binding.abacusFragmentContainer.visibility == View.VISIBLE
            val staleTutorial = isStaleTutorialGhostOverlay(activeOverlay)
            if (hostVisible && !staleTutorial) {
                logMapTouchDiag(
                    "reconcile",
                    "SKIP_BLOCKING_OVERLAY",
                    "active=${activeOverlay.javaClass.simpleName} hostVisible=true → notifyVisible YOK",
                )
                LessonProgressDiag.log(
                    "MainActivity.reconcile",
                    "SKIP_BLOCKING_OVERLAY active=${activeOverlay.javaClass.simpleName} forcingDismiss=$forcingAbacusOverlayDismissForSeasonGate",
                )
                logFirstTutorial(
                    "reconcile.skip",
                    "blocking overlay active=${activeOverlay.javaClass.simpleName}",
                )
                tryShowSeasonLeaderboardRewardGateIfNeeded()
                return
            }
            if (!hostVisible && !staleTutorial &&
                (activeOverlay is TutorialFragment || firstTutorialOverlayBootstrapActive)
            ) {
                binding.abacusFragmentContainer.visibility = View.VISIBLE
                logMapTouchDiag(
                    "reconcile",
                    "RESHOW_ABACUS_HOST",
                    "active=${activeOverlay.javaClass.simpleName} → host VISIBLE yapıldı",
                )
                logFirstTutorial(
                    "reconcile.reShowAbacusHost",
                    "active=${activeOverlay.javaClass.simpleName}",
                )
                tryShowSeasonLeaderboardRewardGateIfNeeded()
                return
            }
            if (staleTutorial) {
                logMapTouchDiag(
                    "reconcile",
                    "PURGE_STALE_TUTORIAL",
                    "active=TutorialFragment hostVisible=$hostVisible → purge devam",
                )
                logFirstTutorial("reconcile.purgeStaleTutorial", overlaySnapshot("purgeStaleTutorial"))
            } else {
                logFirstTutorial(
                    "reconcile.staleGhost",
                    "will remove active=${activeOverlay.javaClass.simpleName}",
                )
            }
        }
        logFirstTutorial(
            "reconcile.willRestoreMapUi",
            "activeOverlay=${activeOverlay?.javaClass?.simpleName ?: "null"} abacusVis=VISIBLE=${binding.abacusFragmentContainer.visibility == View.VISIBLE}",
        )
        for (step in 0 until 6) {
            fm.executePendingTransactions()
            val orphan = fm.findFragmentById(R.id.abacusFragmentContainer)
            if (orphan == null || !fragmentBlocksSeasonLeaderboardGate(orphan)) break
            fm.beginTransaction().remove(orphan).commitNowAllowingStateLoss()
            fm.executePendingTransactions()
        }
        restoreMapUiAfterLessonOverlayDismiss()
        forcingAbacusOverlayDismissForSeasonGate = false
        ensureChromeUnlockedAfterMapReturn("reconcile.done")
        notifyMapVisibleAfterLessonClaim("reconcile.done")
        logMapTouchDiag("reconcile", "RESTORE_DONE", "purge+notifyVisible tamamlandı")
        tryShowSeasonLeaderboardRewardGateIfNeeded()
    }

    fun hideSeasonLeaderboardRewardGateContainer() {
        if (!::binding.isInitialized) return
        // replace sırasında eski instance onDestroyView'da çağırınca yeni gate zaten container'da olabilir — GONE yapma.
        if (supportFragmentManager.findFragmentById(R.id.seasonLeaderboardRewardGateContainer) != null) {
            return
        }
        binding.seasonLeaderboardRewardGateContainer.visibility = View.GONE
    }

    /** Sezon ödül kapısı: yerel repo boşsa state dokümanını tekrar okur. */
    fun refreshUserBadgeProgressFromFirestore(onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete()
            return
        }
        firestore.collection("users").document(uid).collection("badgeProgress").document("state").get()
            .addOnCompleteListener { task ->
                val snap = task.result
                if (snap != null && snap.exists()) {
                    BadgeProgressRepository.update(BadgeProgressFirestore.userBadgeProgressFromStateSnapshot(snap))
                    requestSeasonLeaderboardRewardGateIfPending()
                }
                onComplete()
            }
    }

    /** [BadgeFragment] sezon liderlik kutlama kuyruğu bittiğinde pending alanını siler. */
    fun onSeasonLeaderboardBadgeCelebrationFinished(ackSeason: Int) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("badgeProgress").document("state")
            .update("pendingLeaderboardRewardSeason", FieldValue.delete())
            .addOnSuccessListener {
                val cur = BadgeProgressRepository.getUserBadgeProgress()
                BadgeProgressRepository.update(cur.copy(pendingLeaderboardRewardSeason = null))
            }
    }
    
    /**
     * Her dokunuş ve tuş olayı buradan geçiyor; çalışma süresi sayacı bunu kullanıyor.
     *
     * Ders ekranlarının içine tek tek kanca koymanın alternatifiydi: dört ekranda dört ayrı
     * doğruluk sorusu yerine tek kapı. Ders ekranlarının hepsi bu activity'de yaşıyor
     * (abacusFragmentContainer / resultFragmentContainer / fragmentContainerID), yani başka
     * bir activity'de aynı kancaya ihtiyaç yok.
     */
    override fun onUserInteraction() {
        super.onUserInteraction()
        StudyTimeTracker.onUserInteraction()
    }

    override fun onResume() {
        super.onResume()
        currentActivity = this
        // Güvenlik ağı: kuyruk reklam açıkken (activity duraklatılmışken) dürtüldüyse
        // "not_resumed" deyip durmuş olabilir. Reklam kapanınca buradan devam ediyor.
        pumpPostLessonQueue("MainActivity.onResume")
        refreshStreakUi()
        // Seri sunucuda tutuluyor (ödüller ona bakıyor). Uygulama öne geldiğinde oradan
        // tazeleniyor: cihaz değişmiş olabilir, ya da seri başka bir cihazda ilerlemiş.
        StreakSyncService.refreshFromServer(this) { runOnUiThread { refreshStreakUi() } }
        // Ders ekranından çıkıldığında çalışma süresi yazılıyor; alev o anda güncellensin.
        StudyTimeTracker.setOnChangedListener { runOnUiThread { refreshStreakUi() } }
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val loginStartEverShown = prefs.getBoolean("login_start_ever_shown", false)
        val hasExistingLogin = auth.currentUser != null
        if (loginStartEverShown && !hasExistingLogin) {
            Log.d("kesl",loginStartEverShown.toString())
            Log.d("kesl",hasExistingLogin.toString())
            startActivity(Intent(this, LoginStartActivity::class.java))
            finish()
            return
        }
        if (hasExistingLogin) {
            refreshWalletFromFirestore()
            // Sezon saatini sunucuya göre düzelt. Saatte bir defadan sık istek atmıyor,
            // bu yüzden her onResume'da çağırmak serbest.
            SeasonClock.refreshFromServer()
            SessionDeviceManager.requireLoggedInAndSingleDevice(this) {
                SessionDeviceManager.startSessionHeartbeat(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    val appPrefs = getSharedPreferences(PREFS_APP, MODE_PRIVATE)
                    val prompted = appPrefs.getBoolean(KEY_NOTIF_PERMISSION_PROMPTED, false)
                    if (!prompted && !notificationPermissionRequestInFlight) {
                        // Mark before launch to avoid duplicate prompts in repeated onResume calls.
                        appPrefs.edit().putBoolean(KEY_NOTIF_PERMISSION_PROMPTED, true).apply()
                        notificationPermissionRequestInFlight = true
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                MyFirebaseMessagingService.saveCurrentTokenToFirestore()
            }
        }

        // Uygulama aktifken süre takibini başlat
        TimeTracker.startTracking()
        // Abonelik durumunu kontrol et (plan değişmiş olabilir)
        checkSubscriptionAndUpdateEnergy()
        attachSeasonLeaderboardPendingListenerIfLoggedIn()
        refreshPendingSeasonLeaderboardRewardFromFirestore()
    }
    
    override fun onPause() {
        super.onPause()
        SessionDeviceManager.stopSessionHeartbeat()
        currentActivity = null
        // Dinleyici Activity'ye referans tutuyor; ekran arkaya geçerken bırakılıyor.
        StudyTimeTracker.setOnChangedListener(null)
        // Uygulama background'a geçtiğinde süre takibini durdur
        TimeTracker.stopTracking()
    }

    override fun onStop() {
        super.onStop()
        SessionDeviceManager.releaseActiveSessionIfOwned(this)
        if (binding.recordingOverlayContainer.visibility == View.VISIBLE) {
            startService(Intent(this, ScreenRecordingService::class.java).setAction(ScreenRecordingService.ACTION_STOP_AND_DISCARD))
            hideRecordingOverlay()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent extras = ${intent.extras}")
        handleOpenQuestionIdFromIntent()
    }

    /** Opens QuestionChatFragment when launched from FCM notification (EXTRA_OPEN_QUESTION_ID). */
    private fun handleOpenQuestionIdFromIntent() {
        val extras = intent?.extras
        Log.d("MainActivity", "handleOpenQuestionIdFromIntent extras = $extras")
        val questionId = extras?.getString(EXTRA_OPEN_QUESTION_ID)
        val recipientFromIntent = extras?.getString(EXTRA_NOTIFICATION_RECIPIENT_UID)
        val currentUid = auth.currentUser?.uid
        Log.d(
            "MainActivity",
            "handleOpenQuestionIdFromIntent questionId = $questionId, recipientFromIntent=$recipientFromIntent, currentUid=$currentUid"
        )

        if (questionId.isNullOrEmpty() || recipientFromIntent.isNullOrEmpty() || currentUid.isNullOrEmpty()) {
            clearNotificationExtras()
            return
        }

        if (currentUid != recipientFromIntent) {
            // Bildirim başka bir kullanıcı için üretilmiş; sadece extras'ı temizle, sohbet açma.
            Log.d("MainActivity", "handleOpenQuestionIdFromIntent: current user != recipient, skipping chat open")
            clearNotificationExtras()
            return
        }

        // Bildirimdeki hedef kullanıcı ile mevcut kullanıcı eşleşiyor → sohbeti aç.
        val fragment = QuestionChatFragment.newInstance(questionId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerID, fragment)
            .addToBackStack(null)
            .commit()

        clearNotificationExtras()
    }

    private fun clearNotificationExtras() {
        intent?.removeExtra(EXTRA_OPEN_QUESTION_ID)
        intent?.removeExtra(EXTRA_NOTIFICATION_RECIPIENT_UID)
    }
    
    override fun onDestroy() {
        seasonLeaderboardGateRetryLifecycleCallbacks?.let {
            supportFragmentManager.unregisterFragmentLifecycleCallbacks(it)
        }
        seasonLeaderboardGateRetryLifecycleCallbacks = null
        seasonLeaderboardPendingListener?.remove()
        walletListenerRegistration?.remove()
        seasonLeaderboardPendingListener = null
        if (::billingManager.isInitialized) billingManager.end()
        if (binding.recordingOverlayContainer.visibility == View.VISIBLE) {
            startService(Intent(this, ScreenRecordingService::class.java).setAction(ScreenRecordingService.ACTION_STOP_AND_DISCARD))
        }
        hideRecordingOverlay()
        super.onDestroy()
        // Uygulama kapanırken süre takibini durdur
        TimeTracker.stopTracking()
        if (::energyManager.isInitialized) {
            energyManager.destroy()
        }
    }
    
    // ── Günlük hedef kutlaması ──────────────────────────────────────────

    /** Şerit ekranda mı; otomatik gizleme zamanlayıcısı bununla birlikte yönetiliyor. */
    private var streakCelebrationShowing = false

    private val streakCelebrationHideRunnable = Runnable { hideStreakCelebration() }

    // ── Ders sonrası ekran kuyruğu ───────────────────────────────────────────
    //
    // NEDEN VAR
    //   Ders bitince haritaya dönüşe kadar sekiz ayrı şey açılabiliyor. Her biri kendi
    //   yolundan, kendi guard'ıyla tetikleniyordu: rozet İKİ ayrı yoldan, rating ÜÇ ayrı
    //   yerden, maraton rehberi DOKUZ ayrı yerden. Kimse kimseden haberdar değildi, yani
    //   hangisinin görüneceği "kim önce yetişirse"ye kalıyordu: ekranlar birbirinin
    //   üstüne biniyor, reklamın altında kalıyor ya da hiç görünmüyordu.
    //
    // NASIL ÇALIŞIYOR
    //   Her ekranın zaten bir "bekliyor" durumu var (rozet payload'ları,
    //   justFinishedChestForRating, MarathonGuideStore.isPending…). Kuyruk bunları okuyup
    //   SIRAYLA tek tek gösteriyor. Bir şey açıkken kapı ([postLessonQueueBlockReason])
    //   kapalı oluyor ve kuyruk bekliyor; o ekran kapanınca kuyruk yeniden dürtülüyor.
    //
    //   Tetikleme noktası ekranların zaten çağırdığı yerler: tryShowPendingMarathonGuideOnMap
    //   dokuz yerden çağrılıyordu ve artık doğrudan kuyruğu dürtüyor. Yani "işler yatıştı"
    //   sinyali bedavaya geldi, dokuz çağrı yerinin hiçbirine dokunulmadı.
    //
    // SIRA (kullanıcının belirlediği)
    //   reklam → sezon kapısı → rozet → yeni seri → öğretmene sorma → rating →
    //   maraton rehberi → kupa yolu/Tasks
    //
    //   İlk ikisi kuyruğun İÇİNDE değil, ÖNÜNDE: reklam kontrolü
    //   (adCheckForBadgeInProgress) ve sezon kapısı zaten kapıda listeli, yani ikisi de
    //   varken kuyruk hiç başlamıyor. Kendi akışları doğru çalışıyordu; sırayı bozmadan
    //   dokunmamak en az riskli yol.

    /** Ders/sandık bitti, serisi olmayan kullanıcıya yeni tur sorulacak. */
    private var newStreakPromptQueued = false

    /**
     * Kuyruk şu an bir şey gösterebilir mi; gösteremiyorsa sebebi.
     *
     * [marathonGuideMapBlockReason] üzerine kuruluyor — ders katmanları, rozet, reklam
     * kontrolü, sezon kapısı ve Firestore beklemesi orada zaten listeli. Üzerine kuyruğun
     * kendi açtığı ekranlar ekleniyor.
     *
     * DİKKAT: burada yalnızca "şu an bir şey GÖRÜNÜYOR" durumları var. "Bekliyor"
     * durumları (rating bekliyor, rehber bekliyor, kupa yolu bekliyor) BURAYA GİRMEZ —
     * onlar kuyruğun göstereceği şeyler; kapıya koysaydık kuyruk kendi işini kendisi
     * engellerdi.
     */
    private fun postLessonQueueBlockReason(): String? {
        if (!::binding.isInitialized) return "binding_not_initialized"
        // Reklam açıkken activity duraklatılıyor; bu kontrol olmadan ekranlar reklamın
        // ALTINDA açılıp reklam kapanınca ortaya çıkıyordu.
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            return "not_resumed"
        }
        if (isFinishing || isDestroyed) return "activity_gone"
        val fm = supportFragmentManager
        if (fm.isStateSaved) return "state_saved"
        marathonGuideMapBlockReason()?.let { return it }
        if (fm.findFragmentByTag("AdSkip") != null) return "ad_skip_showing"
        if (dialogStillShowing("RatingDialog")) return "rating_showing"
        if (dialogStillShowing("AskQuestionOpen")) return "promo_showing"
        val map = fm.findFragmentById(R.id.fragmentContainerID) as? MapFragment
        if (map != null && map.isAdded && map.view != null &&
            map.requireView().findViewById<View>(R.id.guidePanel)?.visibility == View.VISIBLE
        ) {
            return "guide_panel_visible"
        }
        return null
    }

    /**
     * Bu etiketli dialog GERÇEKTEN ekranda mı.
     *
     * `findFragmentByTag != null` yetmiyor: `onDismiss`, fragment'i kaldıran işlem commit
     * EDİLMEDEN ÖNCE çalışıyor. Kapanan ekran kuyruğu dürttüğünde kendisini hâlâ
     * FragmentManager'da buluyor ve "açığım" diyerek sıradakini engelliyordu.
     *
     * Pencerenin kendisine bakılıyor: dialog kapandığı anda `isShowing` false oluyor, yani
     * kare saymaya gerek kalmıyor.
     */
    private fun dialogStillShowing(tag: String): Boolean {
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: return false
        if (fragment.isRemoving) return false
        val dialog = (fragment as? androidx.fragment.app.DialogFragment)?.dialog ?: return true
        return dialog.isShowing
    }

    /** Bir sonraki karede çalışacak kuyruk turu; bkz. [pumpPostLessonQueue]. */
    private var postLessonQueuePendingCaller = ""
    private val postLessonQueueRunnable = Runnable {
        runPostLessonQueue(postLessonQueuePendingCaller)
    }

    /**
     * Kuyruğu dürter: kapı açıksa sıradaki bekleyen ekranı açar.
     *
     * "Bir şeyler yatıştı" diyen her yerden çağrılabilir; boşa çağrılması zararsız.
     *
     * ## Neden bir sonraki kare
     * Ekranlar kapanırken dürtüyor (onDismiss / onDestroyView) ama o anda FragmentManager
     * kaldırma işlemini HENÜZ yapmamış oluyor — işlem kuyruğa alındı, çalışmadı. Kapı
     * kontrolü de kapanmakta olan o ekranı "açık" sayıp kuyruğu durduruyordu ve bir daha
     * kimse dürtmediği için sıradaki ekran HİÇ gösterilmiyordu (yeni seri ekranından sonra
     * maraton rehberinin kaybolması tam olarak buydu).
     *
     * Bir kare beklemek FragmentManager'ın işlemini bitirmesine yetiyor. Aynı karedeki
     * birden fazla dürtü de tek tura indiriliyor.
     */
    fun pumpPostLessonQueue(caller: String) {
        if (!::binding.isInitialized) return
        postLessonQueuePendingCaller = caller
        binding.root.removeCallbacks(postLessonQueueRunnable)
        binding.root.post(postLessonQueueRunnable)
    }

    private fun runPostLessonQueue(caller: String) {
        // Tek satırda bütün tablo: kim bekliyor, kapı açık mı. Bir ekranın neden
        // çıkmadığı sorusunun cevabı burada — "bekleyen yok" ile "kapı kapalı" apayrı
        // sorunlar ve ikisini ayırt etmeden tahmin yürütmek zaman kaybettiriyor.
        Log.d(
            TAG_QUEUE,
            "tur | caller=$caller " +
                "rozet=${pendingBadgePayloadsForAd.size + pendingBadgeStringPayloadsForAd.size} " +
                "yeniSeri=$newStreakPromptQueued " +
                "promo=$pendingLessonTypeReturnForPromo " +
                "rating=$justFinishedChestForRating " +
                "rehber=${MarathonGuideStore.isPending(this)} " +
                "kupaYolu=${GlobalValues.pendingCupPathRevealPartId != null}",
        )
        // Kilit kararı KAPIDAN ÖNCE veriliyor.
        //
        // Eskiden kilit yalnızca kuyruk "bos" dalına ulaştığında bırakılıyordu. Son ekran
        // kapanırken kapı onu hâlâ "açık" saydığı için erken dönülüyor, bekleyen iş de
        // kalmadığı için bekçi kendini iptal ediyor ve kilidi bırakacak kimse kalmıyordu —
        // kullanıcı haritaya hiç dokunamıyordu.
        //
        // Kural tek cümle: kilit, bekleyen iş VARKEN duruyor. Kapının ne dediğinden
        // bağımsız.
        if (!hasPostLessonQueueWork()) releasePostLessonQueueTouchLock(caller)

        val block = postLessonQueueBlockReason()
        if (block != null) {
            Log.d(TAG_QUEUE, "bekliyor | caller=$caller block=$block")
            schedulePostLessonQueueWatchdog()
            return
        }
        // Bir adım çalıştıysa ilerleme var: bekçinin süresi baştan başlasın.
        postLessonQueueWatchdogDeadlineMs = 0L
        if (showBadgeStep(caller)) return
        if (showNewStreakStep(caller)) return
        if (showAskQuestionPromoStep(caller)) return
        if (showRatingStep(caller)) return
        if (showMarathonGuideStep(caller)) return
        if (showCupPathStep(caller)) return
        // Kilit yukarıda bırakıldı (bekleyen iş yoktu), burada yalnızca iz düşüyor.
        Log.d(TAG_QUEUE, "bos | caller=$caller")
    }

    /**
     * Harita dokunma kilidini bırakır ve bekçiyi durdurur.
     *
     * Ölçüt süre değil kuyruğun kendisi: kuyrukta iş varken kilit duruyor, iş bitince
     * bırakılıyor. Boşa çağrılması zararsız — [MapFragment.enableMapTouchRouting] kendi
     * guard'larını da uyguluyor ve ChromeBlocker fazladan release'i yok sayıyor.
     */
    /**
     * Kuyruğun harita kilidi şu anda BİZİM tarafımızdan tutuluyor mu.
     *
     * [MainActivityChromeBlocker] sayıcılı: acquire ve release birebir eşleşmeli. Bu bayrak
     * olmadan kuyruk, başka bir akışın (sandık, görev paneli) kilidini düşürebilirdi —
     * boş bir kuyruk turu her ekran dönüşünde yaşanabiliyor.
     */
    private var postLessonQueueLockHeld = false

    private fun acquirePostLessonQueueTouchLock() {
        if (postLessonQueueLockHeld) return
        if (!hasPostLessonQueueWork()) return
        val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
            ?: return
        postLessonQueueLockHeld = true
        Log.d(TAG_QUEUE, "kilit aliniyor")
        map.lockTouchForPendingOverlay()
    }

    /**
     * Harita dokunma kilidini bırakır ve bekçiyi durdurur.
     *
     * Ölçüt süre değil kuyruğun kendisi: kuyrukta iş varken kilit duruyor, iş bitince
     * bırakılıyor. Yalnızca kendi aldığımız kilit bırakılıyor (bkz.
     * [postLessonQueueLockHeld]).
     */
    private fun releasePostLessonQueueTouchLock(caller: String) {
        if (!::binding.isInitialized) return
        if (!postLessonQueueLockHeld) {
            binding.root.removeCallbacks(postLessonQueueWatchdogRunnable)
            postLessonQueueWatchdogDeadlineMs = 0L
            return
        }
        val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
        if (map == null) {
            Log.d(TAG_QUEUE, "kilit BIRAKILAMADI | caller=$caller reason=map_yok")
            schedulePostLessonQueueWatchdog()
            return
        }
        // enableMapTouchRouting kendi guard'larını da uyguluyor (rozet Firestore beklemesi,
        // promo kilidi…) ve bırakmayabilir. Bayrağı körü körüne düşürmek borç kaybetmek
        // olurdu: ChromeBlocker sayıcılı, bırakılmayan bir acquire haritayı kalıcı olarak
        // kilitli bırakır. Bu yüzden sayaca bakılıyor ve düşmediyse tekrar denenecek.
        val before = MainActivityChromeBlocker.currentLockDepth()
        map.enableMapTouchRouting()
        val after = MainActivityChromeBlocker.currentLockDepth()
        if (after < before) {
            postLessonQueueLockHeld = false
            Log.d(TAG_QUEUE, "kilit birakildi | caller=$caller depth=$before->$after")
            return
        }
        Log.d(TAG_QUEUE, "kilit BIRAKILAMADI | caller=$caller guard engelledi, tekrar denenecek")
        schedulePostLessonQueueWatchdog()
    }

    /**
     * Kuyrukta gösterilmeyi bekleyen bir şey var mı.
     *
     * Harita dokunma kilidi de buna bakıyor ([MapFragment.enableMapTouchRouting]): kuyrukta
     * iş varken harita tıklanabilir olmamalı, yoksa çocuk sıradaki ekran gelmeden bir derse
     * girebiliyor.
     */
    fun hasPostLessonQueueWork(): Boolean =
        pendingBadgePayloadsForAd.isNotEmpty() ||
            pendingBadgeStringPayloadsForAd.isNotEmpty() ||
            newStreakPromptQueued ||
            pendingLessonTypeReturnForPromo ||
            justFinishedChestForRating ||
            MarathonGuideStore.isPending(this) ||
            GlobalValues.pendingCupPathRevealPartId != null

    private var postLessonQueueWatchdogDeadlineMs = 0L
    private val postLessonQueueWatchdogRunnable =
        Runnable { runPostLessonQueue("watchdog") }

    /**
     * Kapı kapalıyken kuyruğu canlı tutar.
     *
     * ## Neden gerekli
     * Kuyruk, açılan ekranın kapanırken kendisini dürtmesine güveniyor. Bir ekran bunu
     * yapmayı unutursa sıradaki ekran SONSUZA KADAR asılı kalıyor — rating dialog'unda
     * tam olarak bu oldu ve maraton rehberi hiç gösterilmedi. Her ekranın kancasını tek
     * tek doğru kurmak yetmiyor; yarın eklenecek bir ekran aynı hatayı tekrarlar.
     *
     * Bekçi sıralamaya karışmıyor, yalnızca "kuyruk tıkandı mı" diye bakıyor. Bekleyen
     * iş yoksa hiç kurulmuyor; bir adım çalıştığında süresi sıfırlanıyor.
     */
    private fun schedulePostLessonQueueWatchdog() {
        if (!::binding.isInitialized) return
        // Bekçi yalnızca bekleyen ekran için değil, BİRAKILMAMIŞ KİLİT için de çalışıyor:
        // kilit askıda kalırsa kullanıcı haritaya hiç dokunamıyor ve bu, bir ekranın
        // gösterilememesinden daha kötü.
        if (!hasPostLessonQueueWork() && !postLessonQueueLockHeld) {
            postLessonQueueWatchdogDeadlineMs = 0L
            binding.root.removeCallbacks(postLessonQueueWatchdogRunnable)
            return
        }
        val now = android.os.SystemClock.elapsedRealtime()
        if (postLessonQueueWatchdogDeadlineMs == 0L) {
            postLessonQueueWatchdogDeadlineMs = now + QUEUE_WATCHDOG_BUDGET_MS
        }
        if (now >= postLessonQueueWatchdogDeadlineMs) {
            // Pes ederken haritayı kilitli bırakmamak şart: bekleyen ekran hiç açılamadıysa
            // ve kilit de açılmazsa kullanıcı hiçbir yere dokunamayan bir haritada kalır.
            Log.w(TAG_QUEUE, "bekci BIRAKTI | sure doldu, harita kilidi aciliyor")
            if (postLessonQueueLockHeld) {
                postLessonQueueLockHeld = false
                val map =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
                map?.releaseMapTouchAfterQueueGaveUp()
            }
            return
        }
        binding.root.removeCallbacks(postLessonQueueWatchdogRunnable)
        binding.root.postDelayed(postLessonQueueWatchdogRunnable, QUEUE_WATCHDOG_INTERVAL_MS)
    }

    /** B4 — rozet kutlaması. */
    private fun showBadgeStep(caller: String): Boolean {
        val payloads = pendingBadgePayloadsForAd
        val stringPayloads = pendingBadgeStringPayloadsForAd
        if (payloads.isEmpty() && stringPayloads.isEmpty()) return false
        pendingBadgePayloadsForAd = emptyList()
        pendingBadgeStringPayloadsForAd = emptyList()
        Log.d(TAG_QUEUE, "rozet | caller=$caller")
        if (payloads.isNotEmpty()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                BadgeProgressFirestore.openBadgeCelebration(supportFragmentManager, payloads)
            }
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right,
                    )
                    .replace(
                        R.id.badgeFragmentContainter,
                        BadgeFragment.newLevelUpSequenceInstance(stringPayloads, 0),
                    )
                    .commit()
            }
        }
        return true
    }

    /** B9 — seri kırıksa yeni tur sorusu. */
    private fun showNewStreakStep(caller: String): Boolean {
        if (!newStreakPromptQueued) return false
        newStreakPromptQueued = false
        if (!StreakRepository.needsNewStreakPrompt(this)) return false
        Log.d(TAG_QUEUE, "yeni seri | caller=$caller")
        NewStreakFragment().showNow(supportFragmentManager, NewStreakFragment.TAG)
        return true
    }

    /**
     * B8 — öğretmene sorma tanıtımı (yalnızca LESSON türü dönüşlerde).
     *
     * Kendi uygunluk kontrolü ve sayacı var; uygun değilse hiç açılmıyor ve harita
     * kilidini kendisi bırakıyor. Bu yüzden "açıldı mı" bilgisi burada yok: denendikten
     * sonra kuyruk bir kez daha dürtülüyor, açılmışsa kapı kapalı olduğu için zaten
     * duruyor, açılmamışsa sıradakine geçiliyor.
     */
    private fun showAskQuestionPromoStep(caller: String): Boolean {
        if (!pendingLessonTypeReturnForPromo) return false
        pendingLessonTypeReturnForPromo = false
        Log.d(TAG_QUEUE, "ogretmene sorma | caller=$caller")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            maybeShowAskQuestionPromo("queue:$caller")
            pumpPostLessonQueue("afterPromo:$caller")
        }
        return true
    }

    /** B7 — uygulamayı puanlama. Gösterilmediyse kuyruk devam ediyor. */
    private fun showRatingStep(caller: String): Boolean {
        if (!justFinishedChestForRating) return false
        justFinishedChestForRating = false
        Log.d(TAG_QUEUE, "rating | caller=$caller")
        return AppRatingManager.checkAndShowRatingPrompt(this, 1)
    }

    /** B11 — maraton rehberi (yalnızca haritada). */
    private fun showMarathonGuideStep(caller: String): Boolean {
        if (!MarathonGuideStore.isPending(this)) {
            // Bekleyen rehber YOK. Kuyruk onu engellemedi — hiç sıraya girmemiş.
            // Sebebi MarathonGuide etiketindeki "schedule REJECT" satırında yazıyor
            // (rehber tek seferlik ve yalnızca 1. bölümün ilk sandığında).
            Log.d(TAG_QUEUE, "rehber ATLANDI | caller=$caller reason=pending_degil")
            return false
        }
        MarathonGuideStore.logPrefsSnapshot(this, "queue:$caller")
        val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
        if (map == null || !map.isAdded) {
            Log.d(TAG_QUEUE, "rehber ATLANDI | caller=$caller reason=map_yok")
            return false
        }
        Log.d(TAG_QUEUE, "rehber | caller=$caller")
        val show = Runnable {
            if (map.isAdded) map.maybeShowPendingMarathonGuide(caller)
        }
        map.view?.post(show) ?: show.run()
        return true
    }

    /** B6 — kupa yolu açıldıysa Tasks sekmesine geç. Kuyruğun sonu: haritadan çıkıyoruz. */
    private fun showCupPathStep(caller: String): Boolean {
        if (GlobalValues.pendingCupPathRevealPartId == null) return false
        if (!isMapBaseReadyForMarathonGuide()) return false
        Log.d(TAG_QUEUE, "kupa yolu | caller=$caller")
        // Geçiş sırasında dokunmaları pencere düzeyinde engelle.
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        )
        binding.root.postDelayed({
            binding.bottomNavigationID.selectedItemId = R.id.explore
        }, 500L)
        return true
    }

    /** Yeni tur ekranı kapandı: kuyruğa devam. [NewStreakFragment.onDismiss] çağırıyor. */
    fun onNewStreakPromptClosed() {
        pumpPostLessonQueue("NewStreakFragment.dismiss")
    }

    /** Kapı kapalıyken yeniden deneme; bkz. [scheduleStreakCelebrationRetry]. */
    private val streakCelebrationRetryRunnable =
        Runnable { maybeShowStreakCelebration(fromRetry = true) }

    /** Yeniden denemenin biteceği an (monoton saat); 0 = deneme sürmüyor. */
    private var streakCelebrationDeadlineMs = 0L

    /**
     * Hedef tutturulduysa kutlama şeridini uygun anda gösterir.
     *
     * Kutlama ders ekranındayken hak ediliyor ama orada gösterilmiyor: soru çözen çocuğun
     * önünü kesmek, kutlamayı ödül olmaktan çıkarıp engel yapardı. Bunun yerine
     * [StreakRepository] kuyruğa alıyor, burası güvenli bir ekrana dönüldüğünde açıyor.
     */
    /**
     * @param fromRetry Yeniden deneme zamanlayıcısından geldiyse true; süre bütçesi
     *   yalnızca yeni bir tetiklemede sıfırlanmalı.
     */
    private fun maybeShowStreakCelebration(fromRetry: Boolean = false) {
        if (!::binding.isInitialized) return
        if (!fromRetry) streakCelebrationDeadlineMs = 0L

        val blocked = streakCelebrationBlockReason() != null
        if (streakCelebrationShowing) {
            // Şerit açıkken üstüne bir ders/rozet ekranı geldiyse çekilsin.
            if (blocked) hideStreakCelebration()
            return
        }
        if (blocked) {
            scheduleStreakCelebrationRetry()
            return
        }
        streakCelebrationDeadlineMs = 0L
        binding.streakCelebration.removeCallbacks(streakCelebrationRetryRunnable)

        val streak = StreakRepository.pendingCelebration(this)
        if (streak <= 0) return
        StreakRepository.clearPendingCelebration(this)
        showStreakCelebration(streak)
    }

    /**
     * Kapı kapalıyken kısa aralıklarla yeniden dener.
     *
     * ## Neden gerekli
     * Kutlama ders ekranında hak ediliyor ve dönüşte gösteriliyor. Ama ders katmanlarının
     * kapanması EŞZAMANSIZ: geri yığını değiştiğinde ders/sonuç kapları hâlâ görünür
     * oluyor, kapı kapalı kalıyor ve bir daha denenmiyordu. Kutlama ancak kullanıcı başka
     * bir ekrana gidip dönünce — yani tesadüfen yeni bir tetikleme olunca — çıkıyordu.
     *
     * ## Neden zamanlayıcı, kanca değil
     * Katmanların kapanışı tek bir yerden akmıyor (ders, sandık, rozet, sezon kapısı
     * kendi yollarından kapanıyor). Hepsine kanca takmak, yarın eklenecek bir katmanı
     * unutmak demekti. Bütçeli yeniden deneme hangi yoldan gelinirse gelinsin çalışıyor
     * ve bekleyen kutlama yoksa hiç kurulmuyor.
     */
    private fun scheduleStreakCelebrationRetry() {
        if (StreakRepository.pendingCelebration(this) <= 0) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (streakCelebrationDeadlineMs == 0L) {
            streakCelebrationDeadlineMs = now + STREAK_CELEBRATION_RETRY_BUDGET_MS
        }
        if (now >= streakCelebrationDeadlineMs) return
        binding.streakCelebration.removeCallbacks(streakCelebrationRetryRunnable)
        binding.streakCelebration.postDelayed(
            streakCelebrationRetryRunnable,
            STREAK_CELEBRATION_RETRY_MS,
        )
    }

    /**
     * Kutlamanın şu an gösterilememe sebebi; gösterilebiliyorsa null.
     *
     * İlk koşul listeyi büyük ölçüde gereksiz kılıyor: `currencyPanel` yalnızca harita,
     * bölüm seçimi, görevler ve misyonlar ekranlarında görünüyor — yani kullanıcının
     * "ev"inde. Yine de ders ve kutlama katmanları ayrıca eleniyor, çünkü bunlar taban
     * ekranın ÜSTÜNDE açılıyor ve tabanı değiştirmiyor.
     */
    private fun streakCelebrationBlockReason(): String? {
        val fm = supportFragmentManager
        if (binding.currencyPanel.visibility != View.VISIBLE) return "not_home_screen"
        if (binding.abacusFragmentContainer.visibility == View.VISIBLE) return "lesson_overlay"
        if (binding.resultFragmentContainer.visibility == View.VISIBLE) return "result_overlay"
        if (fm.findFragmentById(R.id.badgeFragmentContainter) != null) return "badge_overlay"
        if (binding.seasonLeaderboardRewardGateContainer.visibility == View.VISIBLE) return "season_gate"
        if (binding.createQuestionOverlayContainer.visibility == View.VISIBLE) return "create_question"
        return null
    }

    private fun showStreakCelebration(streak: Int) {
        val view = binding.streakCelebration
        streakCelebrationShowing = true
        binding.streakCelebrationTitle.text = "Bugünkü hedefini tamamladın!"
        binding.streakCelebrationSub.text =
            if (streak <= 1) "Serin başladı" else "$streak günlük seri"
        binding.streakCelebrationLottie.playAnimation()

        view.setOnClickListener {
            hideStreakCelebration()
            openStreakFragment()
        }

        view.animate().cancel()
        view.alpha = 0f
        view.translationY = -40f * resources.displayMetrics.density
        view.visibility = View.VISIBLE
        view.animate().alpha(1f).translationY(0f).setDuration(260).start()

        view.removeCallbacks(streakCelebrationHideRunnable)
        view.postDelayed(streakCelebrationHideRunnable, STREAK_CELEBRATION_MS)
    }

    private fun hideStreakCelebration() {
        if (!::binding.isInitialized) return
        val view = binding.streakCelebration
        view.removeCallbacks(streakCelebrationHideRunnable)
        if (!streakCelebrationShowing) return
        streakCelebrationShowing = false
        view.animate()
            .alpha(0f)
            .translationY(-40f * resources.displayMetrics.density)
            .setDuration(200)
            .withEndAction {
                // Gizlenme animasyonu biterken şerit yeniden açılmış olabilir; o durumda
                // burada gizlemek az önce gösterilen şeridi kapatırdı.
                if (!streakCelebrationShowing) {
                    view.visibility = View.GONE
                    binding.streakCelebrationLottie.cancelAnimation()
                }
            }
            .start()
    }

    /**
     * Günlük seri ekranını açar.
     *
     * Mağaza ile aynı yol: üstüne ekleniyor (altındaki ekran canlı kalsın) ve geri yığınına
     * giriyor, böylece telefonun geri tuşu doğal olarak kapatıyor.
     */
    fun openStreakFragment() {
        if (MainActivityChromeBlocker.currentLockDepth() > 0) return
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        if (current is StreakFragment) return

        dismissMapLessonOverlayChrome()

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_down, R.anim.slide_up, R.anim.slide_down, R.anim.slide_up)
            .add(R.id.fragmentContainerID, StreakFragment())
            .addToBackStack(null)
            .commit()
        binding.fragmentContainerID.post { updateCurrencyPanelVisibility() }
    }

    fun openShopFragment() {
        if (MainActivityChromeBlocker.currentLockDepth() > 0) return
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        if (current is ShopFragment) return
        
        dismissMapLessonOverlayChrome()
        
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_down, R.anim.slide_up, R.anim.slide_down, R.anim.slide_up)
            .add(R.id.fragmentContainerID, ShopFragment())
            .addToBackStack(null)
            .commit()
        binding.fragmentContainerID.post { updateCurrencyPanelVisibility() }
    }

}
