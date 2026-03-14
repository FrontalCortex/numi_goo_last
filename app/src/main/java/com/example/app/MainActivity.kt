package com.example.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import com.example.app.databinding.ActivityMainBinding
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
        if (granted) launchMediaProjectionForQuestion() else Toast.makeText(this, "Ses kaydı için izin gerekli.", Toast.LENGTH_SHORT).show()
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            Toast.makeText(this, "Ekran kaydı başlatılamadı.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val serviceIntent = Intent(this, ScreenRecordingService::class.java).apply {
            putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, result.resultCode)
            putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, result.data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        showRecordingOverlay()
    }

    private var recordingOverlayView: View? = null
    private var recordingTimerRunnable: Runnable? = null
    private var recordingReceiver: BroadcastReceiver? = null
    private val recordingHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("MainActivity", "onCreate intent extras = ${intent?.extras}")

        // Klavye açıldığında sadece bottomNavigationID'yi gizle, diğer görünümler normal IME insets alsın
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationID) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            view.visibility = if (imeVisible) View.GONE else View.VISIBLE
            insets
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
        binding.bottomNavigationID.itemIconTintList = null

        // Geri tuşu: Sadece kökte (geri gidilecek ekran yokken) çift basınca çıkış; yoksa bir önceki ekrana dön
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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
            backCallback.isEnabled = current is MapFragment && supportFragmentManager.backStackEntryCount <= 1
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
                        if (currentFragment is TasksFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(TasksFragment())
                    R.id.profile ->
                        if (currentFragment is ProfileFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(ProfileFragment())
                    R.id.notification ->
                        if (currentFragment is NotificationFragment) return@requireOnlineAndLoggedInOrLogin
                        else changeFragment(NotificationFragment())
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

    /** AbacusFragment'tan video soru akışı için çağrılır: izinler + MediaProjection + 60sn kayıt + overlay. */
    fun requestQuestionScreenRecording() {
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
    private val maxRecordingSec = 60

    private fun showRecordingOverlay() {
        recordingStartTimeMs = System.currentTimeMillis()
        totalPausedDurationMs = 0L
        isRecordingPaused = false
        if (recordingOverlayView == null) {
            recordingOverlayView = LayoutInflater.from(this).inflate(R.layout.view_recording_overlay, binding.recordingOverlayContainer, false)
            val params = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP }
            binding.recordingOverlayContainer.addView(recordingOverlayView, params)
            binding.recordingOverlayContainer.isClickable = false
            binding.recordingOverlayContainer.setOnTouchListener { _, _ -> false }
            val btnPauseResume = recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnPauseResume)
            btnPauseResume.setImageResource(R.drawable.pause_video_ic)
            btnPauseResume.contentDescription = "Durdur"
            btnPauseResume.setOnClickListener {
                if (isRecordingPaused) {
                    startService(Intent(this, ScreenRecordingService::class.java).setAction(ScreenRecordingService.ACTION_RESUME))
                } else {
                    startService(Intent(this, ScreenRecordingService::class.java).setAction(ScreenRecordingService.ACTION_PAUSE))
                }
            }
            recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnSave).setOnClickListener {
                startService(Intent(this, ScreenRecordingService::class.java).setAction(ScreenRecordingService.ACTION_STOP_AND_SAVE))
            }
            recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnCancel).setOnClickListener {
                startService(Intent(this, ScreenRecordingService::class.java).setAction(ScreenRecordingService.ACTION_STOP_AND_DISCARD))
                hideRecordingOverlay()
            }
        } else {
            val btnPauseResume = recordingOverlayView!!.findViewById<ImageButton>(R.id.recordingBtnPauseResume)
            btnPauseResume.setImageResource(R.drawable.pause_video_ic)
            btnPauseResume.contentDescription = "Durdur"
            isRecordingPaused = false
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
                            binding.abacusFragmentContainer.visibility = View.VISIBLE
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.abacusFragmentContainer, CreateQuestionFragment.newInstanceForVideo(path))
                                .addToBackStack(null)
                                .commit()
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
                                val elapsedSec = ((System.currentTimeMillis() - recordingStartTimeMs - totalPausedDurationMs) / 1000).toInt().coerceAtMost(maxRecordingSec)
                                updateRecordingTimerText(elapsedSec)
                                if (elapsedSec < maxRecordingSec) recordingHandler.postDelayed(this, 1000L)
                            }
                        }
                        val elapsedSec = ((System.currentTimeMillis() - recordingStartTimeMs - totalPausedDurationMs) / 1000).toInt().coerceAtMost(maxRecordingSec)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(recordingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(recordingReceiver, filter)
        }
        recordingTimerRunnable = object : Runnable {
            override fun run() {
                val elapsedSec = ((System.currentTimeMillis() - recordingStartTimeMs - totalPausedDurationMs) / 1000).toInt().coerceAtMost(maxRecordingSec)
                updateRecordingTimerText(elapsedSec)
                if (elapsedSec < maxRecordingSec && !isRecordingPaused) recordingHandler.postDelayed(this, 1000L)
            }
        }
        recordingHandler.postDelayed(recordingTimerRunnable!!, 1000L)
        setQuitButtonEnabled(false)
        setAskQuestionButtonEnabled(false)
    }

    private fun updateRecordingTimerText(elapsedSec: Int) {
        recordingOverlayView?.findViewById<TextView>(R.id.recordingTimerText)?.text =
            "${String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)} / 1:00"
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
        setQuitButtonEnabled(true)
        setAskQuestionButtonEnabled(true)
    }

    private fun setAskQuestionButtonEnabled(enabled: Boolean) {
        binding.abacusFragmentContainer.findViewById<View>(R.id.askQuestionButton)?.apply {
            isEnabled = enabled
            isClickable = enabled
            alpha = if (enabled) 1f else 0.5f
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