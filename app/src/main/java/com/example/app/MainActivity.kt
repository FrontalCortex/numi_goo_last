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
import android.view.View
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
import com.google.firebase.firestore.FirebaseFirestore

//import com.google.android.gms.ads.MobileAds

import androidx.fragment.app.Fragment
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityMainBinding
import com.example.app.model.QuestionMessage
import com.example.app.model.StudentQuestion
import java.io.File
import java.io.FileOutputStream
import android.content.SharedPreferences

class MainActivity : AppCompatActivity(), GoldUpdateListener {

    companion object {
        const val EXTRA_FROM_LOGIN = "from_login"
        /** Set by FCM notification tap; open this question chat when activity is ready. */
        const val EXTRA_OPEN_QUESTION_ID = "open_question_id"
        const val EXTRA_NOTIFICATION_RECIPIENT_UID = "notification_recipient_uid"

        @Volatile
        var currentActivity: MainActivity? = null
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
    ) { _ -> /* FCM bildirimleri için izin sonucu */ }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Bütün görevleri her açılışta sıfırlar
        MissionsProgressStore.resetAllProgress(this)

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer) == null) {
                binding.createQuestionOverlayContainer.visibility = View.GONE
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

            // Overlay (abacus/tutorial/practice) kapanınca alttaki ana fragment gizliyse tekrar göster.
            val topOverlay = supportFragmentManager.findFragmentById(R.id.abacusFragmentContainer)
            if (topOverlay == null) {
                val baseFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
                if (baseFragment != null && baseFragment.isHidden) {
                    supportFragmentManager.beginTransaction()
                        .show(baseFragment)
                        .commitAllowingStateLoss()
                }
            }
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
        if (loginStartEverShown && !hasExistingLogin) {
            Log.d("kesl",loginStartEverShown.toString())
            Log.d("kesl",hasExistingLogin.toString())
            startActivity(Intent(this, LoginStartActivity::class.java))
            finish()
            return
        }
        if (!fromLogin) {
            deleteAllLessonItems(this)
        }
        coin = binding.currencyText
        coin.text = getCurrency(this).toString()

        // Öğretmen hesabında currencyPanel içindeki diamond/coin ve enerji ikonlarını gizle
        if (authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER) {
            binding.diamondID.visibility = View.GONE
            binding.currencyText.visibility = View.GONE
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
        
        // İlk açılış kontrolü - TutorialFragment gösterilecek mi? (fromLogin yukarıda set edildi)
        val firstTutorialShown = prefs.getBoolean("first_tutorial_shown", false)

        // Eğer uygulama çevrimdışıysa, doğrudan offline fragment'ı göster.
        if (!isOnline()) {
            showOfflineFragment()
        } else if (!openedFromChatNotification) {
            // Bildirimden doğrudan bir sohbete açılmadıysak, normal akış: Map/Tutorial.
            if (firstTutorialShown) {
                // Giriş/kayıt sonrası veya tutorial zaten gösterildiyse - MapFragment (ana ekran)
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.fragmentContainerID, MapFragment())
                    addToBackStack(null)
                    commit()
                }
            } else {
                // İlk açılış - TutorialFragment göster
                showFirstTutorial()
            }
        }
        binding.fragmentContainerID.post {
            Log.d("MainActivity", "post -> handleOpenQuestionIdFromIntent() called")
            handleOpenQuestionIdFromIntent()
        }
        // Sistem çubuğu renklerini message_topbar ile eşitle
        window.statusBarColor = ContextCompat.getColor(this, R.color.message_topbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.message_topbar)
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
                // 1) CreateQuestion akışındayken geri tuşu:
                //    - Eğer öğretmen seçim akışından dönülmüş CreateQuestion ise:
                //      onTeacherCreateQuestionDismissedByBack() + fragment'i gerçekten kapat.
                //    - Diğer CreateQuestion durumlarında sadece fragment'i kapat (backButton ile aynı).
                val createQuestionOverlay = supportFragmentManager.findFragmentById(R.id.createQuestionOverlayContainer) as? CreateQuestionFragment
                val createQuestionAbacus = supportFragmentManager.findFragmentById(R.id.abacusFragmentContainer) as? CreateQuestionFragment
                if (createQuestionOverlay != null || createQuestionAbacus != null) {
                    val fragment = createQuestionOverlay ?: createQuestionAbacus
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
            backCallback.isEnabled =
                (current is MapFragment && supportFragmentManager.backStackEntryCount <= 1) ||
                createQuestionVisible ||
                teacherSelectingQuestion
        }

        // Listener'ları set et
        setupClickListeners()
    }

    /**
     * İlk açılışta TutorialFragment'ı gösterir
     */
    private fun showFirstTutorial() {
        // GlobalLessonData'yı initialize et (partId = 1)
        GlobalLessonData.globalPartId = 1
        GlobalLessonData.initialize(this, 1)
        
        // GlobalLessonData'dan 1. index'teki item'ı al (createLessonItems'den değil, initialize edilen verilerden)
        val item = GlobalLessonData.getLessonItem(1)
        item?.let {
            // Global değerleri set et (TutorialFragment onCreate'de kullanılacak)
            it.mapFragmentIndex?.let { index -> GlobalValues.mapFragmentStepIndex = index }
            it.startStepNumber?.let { step -> GlobalValues.lessonStep = step }
            
            // Önce MapFragment'i back stack'e ekle (ChestFragment'ten geri dönüş için)
            val mapFragment = MapFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerID, mapFragment)
                .addToBackStack(null)
                .commit()
            
            // TutorialFragment'ı başlat (abacusFragmentContainer'da gösterilecek)
            binding.abacusFragmentContainer.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.abacusFragmentContainer, TutorialFragment(it.tutorialNumber))
                .addToBackStack(null)
                .commit()
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
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        // Eğer mevcut fragment ile yeni fragment aynı tipteyse, işlem yapma

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerID, fragment)
            addToBackStack(null)
            commit()
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
        val firstTutorialShown = prefs.getBoolean("first_tutorial_shown", false)

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
        // Scrim'i kapat
        findViewById<View>(R.id.scrimView)?.let { scrimView ->
            scrimView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    scrimView.visibility = View.GONE
                }
                .start()
        }

        // Bottom sheet'i bul ve kapat
        findViewById<CoordinatorLayout>(R.id.coordinator_layout)?.let { coordinatorLayout ->
            coordinatorLayout.findViewWithTag<View>("bottom_sheet")?.let { bottomSheetView ->
                coordinatorLayout.removeView(bottomSheetView)
            }
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
        // Sadece şu anki kullanıcının ders verisini temizle (her kullanıcıya özel)
        //GlobalLessonData.clearCurrentUserLessonData(context)

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
    fun getCurrency(context: Context): Int {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("currency", 0)
    }

    override fun onGoldUpdated(amount: Int) {
        updateGoldAmount(amount)
    }

    fun updateGoldAmount(amount: Int) {
        val currentGold = binding.currencyText.text.toString().toIntOrNull() ?: 0
        val newGold = currentGold + amount
        binding.currencyText.text = newGold.toString()
        saveCurrency(this, newGold)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            MyFirebaseMessagingService.saveCurrentTokenToFirestore()
        }

        // Uygulama aktifken süre takibini başlat
        TimeTracker.startTracking()
        // Abonelik durumunu kontrol et (plan değişmiş olabilir)
        checkSubscriptionAndUpdateEnergy()
    }
    
    override fun onPause() {
        super.onPause()
        currentActivity = null
        // Uygulama background'a geçtiğinde süre takibini durdur
        TimeTracker.stopTracking()
    }

    override fun onStop() {
        super.onStop()
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