package com.example.numigoo

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds

import androidx.fragment.app.Fragment
import com.example.numigoo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), GoldUpdateListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var coin:TextView
    private lateinit var energyManager: EnergyManager
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // AdMob'u başlat
        MobileAds.initialize(this) {}
        
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

    fun refreshRacePanel() {
        // Race panel açıksa sadece adapter'ı güncelle
        findViewById<CoordinatorLayout>(R.id.coordinator_layout)?.let { coordinatorLayout ->
            coordinatorLayout.findViewWithTag<View>("race_panel")?.let { racePanel ->
                // MapFragment'teki LessonAdapter'ı bul ve sadece adapter'ı güncelle
                val mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
                mapFragment?.refreshRacePanel()
            }
        }
    }
    
    private fun updateEnergyDisplay(energy: Int) {
        binding.energyText.text = "$energy/${energyManager.getMaxEnergy()}"
    }
    
    fun getEnergyManager(): EnergyManager {
        return energyManager
    }
    
    override fun onDestroy() {
        super.onDestroy()
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