package com.example.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityEmailVerificationBinding
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerificationBinding
    private lateinit var authManager: AuthManager
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager()
        authManager.initialize(this)
        auth = FirebaseAuth.getInstance()
        
        setupUI()
    }
    
    private fun setupUI() {
        // E-posta adresini göster
        val email = intent.getStringExtra("email") ?: ""
        binding.tvEmail.text = email
        
        // Tekrar gönder butonu
        binding.btnResendEmail.setOnClickListener {
            resendVerificationCode(email)
        }
        
        // Doğrulama kontrolü butonu
        binding.btnCheckVerification.setOnClickListener {
            verifyCode(email)
        }
        
        // Giriş sayfasına dön
        binding.btnBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        // Kod giriş alanına odaklan
        binding.etVerificationCode.requestFocus()
    }
    
    private fun resendVerificationCode(email: String) {
        // Butonu devre dışı bırak
        binding.btnResendEmail.isEnabled = false
        binding.btnResendEmail.text = "Gönderiliyor..."
        
        // Eğer pending registration varsa, registerStudent'ı tekrar çağır
        // Yoksa resendStudentVerificationCode'u kullan
        authManager.resendStudentVerificationCode(email) { success, error ->
            binding.btnResendEmail.isEnabled = true
            binding.btnResendEmail.text = "E-postayı Tekrar Gönder"
            
            if (success) {
                Toast.makeText(this, "Doğrulama kodu tekrar gönderildi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, error ?: "Kod gönderilemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun verifyCode(email: String) {
        val code = binding.etVerificationCode.text.toString().trim()
        
        if (code.isEmpty()) {
            Toast.makeText(this, "Lütfen doğrulama kodunu girin", Toast.LENGTH_SHORT).show()
            binding.etVerificationCode.requestFocus()
            return
        }
        
        if (code.length != 6) {
            Toast.makeText(this, "Kod 6 haneli olmalıdır", Toast.LENGTH_SHORT).show()
            binding.etVerificationCode.requestFocus()
            return
        }
        
        // Butonu devre dışı bırak
        binding.btnCheckVerification.isEnabled = false
        binding.btnCheckVerification.text = "Doğrulanıyor..."
        
        authManager.verifyStudentCode(email, code) { success, error ->
            // Butonu tekrar aktif et
            binding.btnCheckVerification.isEnabled = true
            binding.btnCheckVerification.text = "Kodu Doğrula"
            
            if (success) {
                // Başarılı - kayıt tamamlandı
                Toast.makeText(this, "Kayıt başarılı! Giriş yapabilirsiniz.", Toast.LENGTH_LONG).show()
                // Kısa bir gecikme sonrası login ekranına yönlendir
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }, 1500)
            } else {
                // Hata - kod yanlış veya süresi dolmuş
                Toast.makeText(this, error ?: "Kod doğrulanamadı. Lütfen tekrar deneyin.", Toast.LENGTH_LONG).show()
                binding.etVerificationCode.setText("")
                binding.etVerificationCode.requestFocus()
            }
        }
    }
}
