package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

//import com.google.android.gms.ads.MobileAds

import androidx.fragment.app.Fragment
import com.example.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), GoldUpdateListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var coin:TextView
    private lateinit var energyManager: EnergyManager
    private lateinit var adManager: AdManager
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
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
        
        // AdMob'u başlat - geçici olarak kaldırıldı
        //MobileAds.initialize(this) {}
        deleteAllLessonItems(this)
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
        
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerID,MapFragment())
            addToBackStack(null)
            commit()
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        binding.bottomNavigationID.itemIconTintList = null

        binding.bottomNavigationID.setOnItemSelectedListener {
            closeBottomSheet()
            when (it.itemId) {
                R.id.map -> changeFragment(MapFragment())
                R.id.tasks -> changeFragment(TasksFragment())
                R.id.profile -> changeFragment(ProfileFragment())
                R.id.notification -> changeFragment(NotificationFragment())
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
        val prefs = context.getSharedPreferences("LessonPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
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
        energyManager.destroy()
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