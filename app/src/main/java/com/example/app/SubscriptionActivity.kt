package com.example.app

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.app.databinding.ActivitySubscriptionBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SubscriptionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySubscriptionBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupClickListeners()
        loadCurrentPlan()
    }
    
    private fun setupUI() {
        // Toolbar ayarları
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Abonelik Planları"
    }
    
    private fun loadCurrentPlan() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            highlightPlan("Free")
            return
        }
        
        // Firestore'dan mevcut planı al
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val isPremium = doc.getBoolean("isPremium") ?: false
                    val plan = doc.getString("plan") ?: "Free"
                    
                    // Premium kontrolü
                    val currentPlan = if (isPremium) {
                        // Premium ise, plan tipine göre belirle
                        when (plan) {
                            "Pro" -> "Pro"
                            "Premium" -> "Premium"
                            else -> "Premium" // Varsayılan olarak Premium
                        }
                    } else {
                        "Free"
                    }
                    
                    highlightPlan(currentPlan)
                } else {
                    highlightPlan("Free")
                }
            }
            .addOnFailureListener { e ->
                Log.e("SubscriptionActivity", "Plan yüklenemedi", e)
                highlightPlan("Free")
            }
    }
    
    private fun setupClickListeners() {
        // Free Plan
        binding.cardFree.setOnClickListener {
            selectPlan("Free")
        }
        
        // Pro Plan
        binding.cardPro.setOnClickListener {
            selectPlan("Pro")
        }
        
        // Premium Plan
        binding.cardPremium.setOnClickListener {
            selectPlan("Premium")
        }
        
        // Butonlar
        binding.btnFree.setOnClickListener {
            selectPlan("Free")
        }
        
        binding.btnPro.setOnClickListener {
            selectPlan("Pro")
        }
        
        binding.btnPremium.setOnClickListener {
            selectPlan("Premium")
        }
        
        // Toolbar geri butonu
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun highlightPlan(planName: String) {
        // Tüm kartları sıfırla
        resetAllCards()
        
        // Seçili planı highlight et
        when (planName) {
            "Free" -> {
                binding.cardFree.elevation = 12f
                binding.btnFree.text = "Mevcut Plan"
                binding.btnFree.isEnabled = false
                binding.btnFree.backgroundTintList = ContextCompat.getColorStateList(this, R.color.lesson_completed)
            }
            "Pro" -> {
                binding.cardPro.elevation = 12f
                binding.btnPro.text = "Mevcut Plan"
                binding.btnPro.isEnabled = false
                binding.btnPro.backgroundTintList = ContextCompat.getColorStateList(this, R.color.lesson_header_orange)
            }
            "Premium" -> {
                binding.cardPremium.elevation = 12f
                binding.btnPremium.text = "Mevcut Plan"
                binding.btnPremium.isEnabled = false
                binding.btnPremium.backgroundTintList = ContextCompat.getColorStateList(this, R.color.lesson_header_purple)
            }
        }
    }
    
    private fun resetAllCards() {
        binding.cardFree.elevation = 6f
        binding.cardPro.elevation = 6f
        binding.cardPremium.elevation = 6f
        
        binding.btnFree.text = "Seç"
        binding.btnPro.text = "Seç"
        binding.btnPremium.text = "Seç"
        
        binding.btnFree.isEnabled = true
        binding.btnPro.isEnabled = true
        binding.btnPremium.isEnabled = true
        
        binding.btnFree.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        binding.btnPro.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.white)
        binding.btnPremium.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
    }
    
    private fun selectPlan(planName: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Firestore'da planı güncelle
        val updates = hashMapOf<String, Any>(
            "plan" to planName,
            "isPremium" to (planName != "Free")
        )
        
        // Premium planlar için subscriptionEndDate ekle (30 gün sonra)
        if (planName != "Free") {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 30)
            updates["subscriptionEndDate"] = com.google.firebase.Timestamp(calendar.time)
        } else {
            // Free plan için subscriptionEndDate'i kaldır
            updates["subscriptionEndDate"] = com.google.firebase.firestore.FieldValue.delete()
        }
        
        firestore.collection("users").document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "$planName planına geçiş yapıldı", Toast.LENGTH_SHORT).show()
                highlightPlan(planName)
                
                // Activity'yi kapat ve MainActivity'ye dön
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("SubscriptionActivity", "Plan güncellenemedi", e)
                Toast.makeText(this, "Plan güncellenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

