package com.example.numigoo

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat

import androidx.fragment.app.Fragment
import com.example.numigoo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), GoldUpdateListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var coin:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        deleteAllLessonItems(this)
        coin = binding.currencyText
        coin.text = getCurrency(this).toString()
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
}