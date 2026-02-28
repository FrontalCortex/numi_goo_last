package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
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
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var coin:TextView
    private lateinit var energyManager: EnergyManager
    private lateinit var adManager: AdManager
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var lastBackPressTime = 0L
    private val backPressToExitMillis = 2000L
    
    private val subscriptionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Plan değişti, enerji gösterimini güncelle
            checkSubscriptionAndUpdateEnergy()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
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
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
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
            closeBottomSheet()
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
            when (it.itemId) {
                R.id.map -> if (currentFragment is MapFragment) return@setOnItemSelectedListener true else changeFragment(MapFragment())
                R.id.tasks -> if (currentFragment is TasksFragment) return@setOnItemSelectedListener true else changeFragment(TasksFragment())
                R.id.profile -> if (currentFragment is ProfileFragment) return@setOnItemSelectedListener true else changeFragment(ProfileFragment())
                R.id.notification -> if (currentFragment is NotificationFragment) return@setOnItemSelectedListener true else changeFragment(NotificationFragment())
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
    private fun changeFragment(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        // Eğer mevcut fragment ile yeni fragment aynı tipteyse, işlem yapma

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerID, fragment)
            addToBackStack(null)
            commit()
        }
    }

    /** Race paneli açıkken alt bar butonlarına tıklanmasın (overlay ile engelle) */
    fun setBottomPanelEnabled(enabled: Boolean) {
        if (enabled) {
            binding.bottomPanelOverlay.visibility = View.GONE
        } else {
            binding.bottomPanelOverlay.visibility = View.VISIBLE
            binding.bottomPanelOverlay.bringToFront()
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

        // Uygulama aktifken süre takibini başlat
        TimeTracker.startTracking()
        // Abonelik durumunu kontrol et (plan değişmiş olabilir)
        checkSubscriptionAndUpdateEnergy()
    }
    
    override fun onPause() {
        super.onPause()
        // Uygulama background'a geçtiğinde süre takibini durdur
        TimeTracker.stopTracking()
    }
    
    override fun onDestroy() {
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