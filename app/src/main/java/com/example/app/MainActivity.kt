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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

//import com.google.android.gms.ads.MobileAds

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

class MainActivity : AppCompatActivity(), GoldUpdateListener {

    companion object {
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
            append(" firstShown=").append(FirstTutorialShownStore.readLocal(this@MainActivity))
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
    private lateinit var adManager: AdManager
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
    private var teacherSelectedQuestionId: String? = null
    private var teacherSelectedQuestionTitle: String? = null
    private var notificationPermissionRequestInFlight = false

    private var seasonLeaderboardPendingListener: ListenerRegistration? = null
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
            view.visibility = when {
                current is AbacusPracticeFragment -> View.GONE
                imeVisible -> View.GONE
                teacherPendingMediaPath != null -> View.GONE
                else -> View.VISIBLE
            }
            insets
        }

        supportFragmentManager.addOnBackStackChangedListener {
            // Re-evaluate bottom nav visibility with current fragment state.
            binding.bottomNavigationID.requestApplyInsets()
            updateCurrencyPanelVisibility()

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
                    "first_tutorial_shown=${FirstTutorialShownStore.readLocal(this)}",
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
        
        // Enerji sistemini başlat
        energyManager = EnergyManager(this)
        energyManager.setEnergyUpdateCallback { energy ->
            updateEnergyDisplay(energy)
        }
        updateEnergyDisplay(energyManager.getCurrentEnergy())
        
        // Reklam yöneticisini başlat
        adManager = AdManager(this)
        
        // Süre takibini initialize et (onResume'da başlatılacak)
        TimeTracker.initialize(this)
        
        // Abonelik durumunu kontrol et ve enerji gösterimini güncelle
        checkSubscriptionAndUpdateEnergy()

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
                        replace(R.id.fragmentContainerID, MapFragment())
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
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        val firstTutorialShown = FirstTutorialShownStore.readLocal(this)
                        if (firstTutorialShown) {
                            logFirstTutorial("onCreate.route", "guest first_tutorial_shown=true -> Map only")
                            supportFragmentManager.beginTransaction().apply {
                                replace(R.id.fragmentContainerID, MapFragment())
                                addToBackStack(null)
                                commit()
                            }
                        } else {
                            logFirstTutorial("onCreate.route", "guest first_tutorial_shown=false -> showFirstTutorial")
                            showFirstTutorial()
                        }
                    } else {
                        logFirstTutorial("onCreate.route", "uid=${currentUser.uid.take(8)} -> Firestore first_tutorial_shown")
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                val firestoreRaw =
                                    if (doc.exists()) doc.getBoolean("first_tutorial_shown") else null
                                val firstTutorialShown = FirstTutorialShownStore.resolveShown(
                                    this@MainActivity,
                                    firestoreRaw,
                                    "Main.onCreate.firestore",
                                )
                                if (firstTutorialShown) {
                                    FirstTutorialShownStore.repairFirestoreIfLocalShown(
                                        this@MainActivity,
                                        "Main.onCreate.firestore",
                                    )
                                }
                                logFirstTutorial(
                                    "onCreate.firestore",
                                    "exists=${doc.exists()} firestoreRaw=$firestoreRaw resolved=$firstTutorialShown",
                                )
                                if (firstTutorialShown) {
                                    supportFragmentManager.beginTransaction().apply {
                                        replace(R.id.fragmentContainerID, MapFragment())
                                        addToBackStack(null)
                                        commit()
                                    }
                                } else {
                                    showFirstTutorial()
                                }
                            }
                            .addOnFailureListener { e ->
                                logFirstTutorial("onCreate.firestore", "FAIL -> showFirstTutorial err=${e.message}")
                                showFirstTutorial()
                            }
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
                //    sistem geri tuşu bottom bar'daki teacherSendBackButton ile aynı davranışı göstersin.
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
                if (teacherPendingMediaPath != null && currentFragment is NotificationFragment) {
                    onTeacherSelectionBackFromNotification()
                    return
                }

                // 3) Kökte değilsek normal backstack davranışı
                val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
                val atRoot = supportFragmentManager.backStackEntryCount <= 1 && current is MapFragment
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
                (current is MapFragment && supportFragmentManager.backStackEntryCount <= 1) ||
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
        logFirstTutorial("showFirstTutorial", "initialize partId=1")
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
            .replace(R.id.abacusFragmentContainer, TutorialFragment(item.tutorialNumber))
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
                    R.id.map ->
                        if (currentFragment is MapFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(MapFragment())
                    R.id.tasks ->
                        if (currentFragment is MissionsFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(MissionsFragment())
                    R.id.explore ->
                        if (currentFragment is TasksFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(TasksFragment())
                    R.id.profile ->
                        if (currentFragment is ProfileFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(ProfileFragment())
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
        
        // Enerji test için uzun basma
        binding.energyText.setOnLongClickListener {
            // Test için enerjiyi sıfırla
            energyManager.useEnergy(energyManager.getCurrentEnergy())
            true
        }
        
        // Enerji paneli tıklama
        binding.energyText.setOnClickListener {
            showEnergyRefillDialog()
        }
        
        // Enerji ikonu tıklama
        binding.energyIcon.setOnClickListener {
            showEnergyRefillDialog()
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
        binding.teacherSendBackButton.visibility = View.VISIBLE
        binding.bottomNavigationID.visibility = View.GONE
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
        binding.teacherSendBackButton.visibility = View.GONE
        binding.bottomNavigationID.visibility = View.VISIBLE
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

        // 1) Medya mesajını (varsa açıklama ile tek mesaj olarak) kuyruğa al
        val mediaClientId = "pending_${System.currentTimeMillis()}"
        Intent(this, QuestionUploadForegroundService::class.java).apply {
            putExtra(QuestionUploadForegroundService.KEY_QUESTION_ID, questionId)
            putExtra(QuestionUploadForegroundService.KEY_TYPE, if (mediaType == StudentQuestion.MEDIA_TYPE_VIDEO) QuestionMessage.TYPE_VIDEO else QuestionMessage.TYPE_IMAGE)
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

        // Temizlik: overlay, bar, geri butonu, alt nav tekrar göster, geçici state (dosyayı servis silecek)
        supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer)?.let {
            supportFragmentManager.popBackStack()
        }
        binding.teacherSendToQuestionBar.visibility = View.GONE
        binding.teacherSendBackButton.visibility = View.GONE
        binding.bottomNavigationID.visibility = View.VISIBLE
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
        recordingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
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
            addAction(ScreenRecordingService.ACTION_RECORDING_FINISHED)
            addAction(ScreenRecordingService.ACTION_RECORDING_FAILED)
            addAction(ScreenRecordingService.ACTION_RECORDING_PAUSED)
            addAction(ScreenRecordingService.ACTION_RECORDING_RESUMED)
        }
        ContextCompat.registerReceiver(this, recordingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
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

    private fun updateRecordingTimerText(elapsedSec: Int) {
        val maxM = maxRecordingSecForSession / 60
        val maxS = maxRecordingSecForSession % 60
        recordingOverlayView?.findViewById<TextView>(R.id.recordingTimerText)?.text =
            "${String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)} / $maxM:${String.format("%02d", maxS)}"
    }

    private fun hideRecordingOverlay() {
        recordingTimerRunnable?.let { recordingHandler.removeCallbacks(it) }
        recordingTimerRunnable = null
        recordingReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) { /* zaten kaldırılmış */ }
            recordingReceiver = null
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
        binding.currencyPanel.visibility = if (current is MapFragment) View.VISIBLE else View.GONE
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
        val firstTutorialShown = FirstTutorialShownStore.readLocal(this)

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
        findViewById<CoordinatorLayout>(R.id.coordinator_layout)?.findViewWithTag<View>("bottom_sheet")
            ?.let { sheet -> (sheet.parent as? ViewGroup)?.removeView(sheet) }
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
        if (!FirstTutorialShownStore.readLocal(this)) return false
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
        GlobalLessonData.clearCurrentUserLessonData(context)
        // Kullanıcı lesson verilerini Firestore'den siler (uid geç gelirse bekleyip tekrar dener)
        deleteLessonProgressFromFirestoreWithAuthWait()
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
    fun saveCurrency(context: Context, value: Int) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("currency", value).apply()
    }

    fun getCurrency(context: Context): Int = UserWalletFirestore.getCachedCurrency(context)

    private fun refreshWalletFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        UserWalletFirestore.loadWallet(
            context = this,
            uid = uid,
            onResult = { wallet ->
                if (::binding.isInitialized) {
                    applyWalletToUi(wallet)
                }
            },
        )
    }

    private fun applyWalletToUi(wallet: UserWallet) {
        binding.currencyText.text = wallet.currency.toString()
        binding.keyText.text = wallet.keys.toString()
    }

    override fun onGoldUpdated(amount: Int) {
        updateGoldAmount(amount)
    }

    override fun onKeysUpdated(amount: Int) {
        updateKeysAmount(amount)
    }

    fun updateGoldAmount(amount: Int) {
        if (amount == 0) return
        val currentGold = binding.currencyText.text.toString().toIntOrNull() ?: 0
        val newGold = currentGold + amount
        binding.currencyText.text = newGold.toString()
        saveCurrency(this, newGold)
        auth.currentUser?.uid?.let { uid ->
            UserWalletFirestore.applyCurrencyDelta(
                context = this,
                uid = uid,
                delta = amount,
                onSuccess = { wallet -> applyWalletToUi(wallet) },
            )
        }
    }

    fun updateKeysAmount(amount: Int) {
        if (amount == 0) return
        val currentKeys = binding.keyText.text.toString().toIntOrNull() ?: 0
        val newKeys = currentKeys + amount
        binding.keyText.text = newKeys.toString()
        auth.currentUser?.uid?.let { uid ->
            UserWalletFirestore.applyKeyDelta(
                context = this,
                uid = uid,
                delta = amount,
                onSuccess = { wallet -> applyWalletToUi(wallet) },
            )
        }
    }

    /** Üst paneldeki elmas bakiyesinden düşer (Firestore `users.currency`). */
    fun spendDiamonds(amount: Int): Boolean {
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
                onSuccess = { wallet -> applyWalletToUi(wallet) },
            )
        }
        return true
    }

    /** Üst paneldeki anahtar bakiyesinden düşer (Firestore `users.keys`). */
    fun spendKeys(amount: Int): Boolean {
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
                onSuccess = { wallet -> applyWalletToUi(wallet) },
            )
        }
        return true
    }

    private fun updateEnergyDisplay(energy: Int) {
        // Abonelik durumunu kontrol et
        checkSubscriptionAndUpdateEnergy()
    }
    
    private fun checkSubscriptionAndUpdateEnergy() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Kullanıcı giriş yapmamış, normal gösterim
            val energy = energyManager.getCurrentEnergy()
            binding.energyText.text = "$energy/${energyManager.getMaxEnergy()}"
            return
        }
        
        // Firestore'dan abonelik durumunu kontrol et
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val plan = doc.getString("plan") ?: "Free"
                    val isPremium = doc.getBoolean("isPremium") ?: false
                    
                    // Pro veya Premium ise sonsuz işareti göster
                    if (plan == "Pro" || plan == "Premium" || isPremium) {
                        binding.energyText.text = "∞"
                    } else {
                        // Free plan - normal gösterim
                        val energy = energyManager.getCurrentEnergy()
                        binding.energyText.text = "$energy/${energyManager.getMaxEnergy()}"
                    }
                } else {
                    // Firestore'da kayıt yok, normal gösterim
                    val energy = energyManager.getCurrentEnergy()
                    binding.energyText.text = "$energy/${energyManager.getMaxEnergy()}"
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Abonelik durumu kontrol edilemedi", e)
                // Hata durumunda normal gösterim
                val energy = energyManager.getCurrentEnergy()
                binding.energyText.text = "$energy/${energyManager.getMaxEnergy()}"
            }
    }
    
    fun getEnergyManager(): EnergyManager {
        return energyManager
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
        is RecordFragment,
        is DailyQuestionRewardFragment,
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
            is AbacusPracticeFragment, is BlindingLessonFragment -> overlay
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
        if (!::binding.isInitialized) return
        forcingAbacusOverlayDismissForSeasonGate = false
        binding.abacusFragmentContainer.visibility = View.GONE
        dismissMapLessonOverlayChrome()
        releasePracticeTouchBlocker()
        releaseLessonActionTouchBlocker()
        ensureChromeUnlockedAfterOverlayDismiss(caller)
        logTouchDiag("completeTasksOverlayDismiss:$caller")
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
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainerID) is MapFragment) {
            purgeAbacusOverlayHosts("prepareMapReturnAfterLessonClaim")
            restoreMapUiAfterLessonOverlayDismiss()
        }
    }

    /** [prepareMapReturnAfterLessonClaim] sonrası: chrome + sezon reconcile (tek kaynak). */
    fun finalizeMapReturnAfterLessonClaim(caller: String) {
        logMapTouchDiag("finalizeMapReturn", "BEFORE", "caller=$caller")
        logTouchDiag("finalizeMapReturnAfterLessonClaim.BEFORE:$caller")
        ensureChromeUnlockedAfterMapReturn(caller)
        scheduleSeasonGateAfterAbacusOverlayDismissed()
        binding.root.post {
            logMapTouchDiag("finalizeMapReturn", "AFTER_POST", "caller=$caller")
            logTouchDiag("finalizeMapReturnAfterLessonClaim.AFTER:$caller")
            notifyMapVisibleAfterLessonClaim("finalizeMapReturn:$caller")
            tryShowPendingMarathonGuideOnMap("finalizeMapReturn:$caller")
        }
    }

    /**
     * Reconcile atlanırsa bile harita listesini ve progress animasyon tüketimini günceller.
     * [MapFragment.notifyVisibleAfterOverlayDismiss] tek kaynak.
     */
    fun notifyMapVisibleAfterLessonClaim(caller: String) {
        if (!::binding.isInitialized) return
        val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
        if (map == null || !map.isAdded) {
            LessonProgressDiag.log("MainActivity.notifyMapVisible", "SKIP caller=$caller map=null or not added")
            return
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

    fun tryShowPendingMarathonGuideOnMap(caller: String) {
        if (!::binding.isInitialized) {
            Log.d(MarathonGuideStore.LOG_TAG, "tryShow SKIP | caller=$caller reason=binding_not_initialized")
            return
        }
        MarathonGuideStore.logPrefsSnapshot(this, "tryShow:$caller")
        val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
        if (map == null) {
            Log.d(MarathonGuideStore.LOG_TAG, "tryShow SKIP | caller=$caller reason=map_fragment_missing")
            return
        }
        if (!map.isAdded) {
            Log.d(MarathonGuideStore.LOG_TAG, "tryShow SKIP | caller=$caller reason=map_not_added")
            return
        }
        if (map.view == null) {
            Log.d(MarathonGuideStore.LOG_TAG, "tryShow WAIT | caller=$caller reason=map_view_null → post")
        }
        map.view?.post {
            if (map.isAdded) {
                map.maybeShowPendingMarathonGuide(caller)
            } else {
                Log.d(MarathonGuideStore.LOG_TAG, "tryShow SKIP | caller=$caller reason=map_detached_before_post")
            }
        } ?: map.maybeShowPendingMarathonGuide(caller)
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
        if (fm.findFragmentById(R.id.fragmentContainerID) !is MapFragment) return
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
    
    override fun onResume() {
        super.onResume()
        currentActivity = this
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
        seasonLeaderboardPendingListener = null
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
    
    fun showEnergyRefillDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_energy_refill, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        val currentEnergyText = dialogView.findViewById<android.widget.TextView>(R.id.currentEnergyText)
        val timeUntilNextText = dialogView.findViewById<android.widget.TextView>(R.id.timeUntilNextText)
        val cancelButton = dialogView.findViewById<android.widget.Button>(R.id.cancelButton)
        val watchAdButton = dialogView.findViewById<android.widget.Button>(R.id.watchAdButton)
        
        // Mevcut enerjiyi göster
        currentEnergyText.text = "${energyManager.getCurrentEnergy()}/${energyManager.getMaxEnergy()}"
        
        // Timer için Handler
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateTimer = object : Runnable {
            override fun run() {
                val timeUntilNext = energyManager.getTimeUntilNextEnergy()
                val minutes = (timeUntilNext / 60000).toInt()
                val seconds = ((timeUntilNext % 60000) / 1000).toInt()
                timeUntilNextText.text = "Bir sonraki enerji: ${minutes}:${String.format("%02d", seconds)}"
                
                // Eğer dialog hala açıksa, 1 saniye sonra tekrar güncelle
                if (dialog.isShowing) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        
        // Timer'ı başlat
        handler.post(updateTimer)
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        watchAdButton.setOnClickListener {
            if (adManager.isAdReady()) {
                dialog.dismiss()
                adManager.showRewardedAd(this) {
                    // Reklam tamamlandı, 1 enerji ver
                    energyManager.addEnergy(1)
                    android.widget.Toast.makeText(this, "Reklam izlendi! +1 Enerji kazandınız!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(this, "Reklam yükleniyor, lütfen bekleyin...", android.widget.Toast.LENGTH_SHORT).show()
                adManager.preloadAd()
            }
        }
        
        dialog.show()
    }
}