package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityEmailVerificationBinding
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(FontScaleLimiter.wrap(newBase))
    }

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
        binding.btnResendEmail.text = appString(R.string.ui_gonderiliyor)
        
        // Eğer pending registration varsa, registerStudent'ı tekrar çağır
        // Yoksa resendStudentVerificationCode'u kullan
        authManager.resendStudentVerificationCode(email) { success, error ->
            binding.btnResendEmail.isEnabled = true
            binding.btnResendEmail.text = appString(R.string.ui_e_postayi_tekrar_gonder)
            
            if (success) {
                Toast.makeText(this, appString(R.string.ui_dogrulama_kodu_tekrar_gonderildi), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, error ?: appString(R.string.ui_kod_gonderilemedi), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun verifyCode(email: String) {
        val code = binding.etVerificationCode.text.toString().trim()
        
        if (code.isEmpty()) {
            Toast.makeText(this, appString(R.string.ui_lutfen_dogrulama_kodunu_girin), Toast.LENGTH_SHORT).show()
            binding.etVerificationCode.requestFocus()
            return
        }
        
        if (code.length != 6) {
            Toast.makeText(this, appString(R.string.ui_kod_6_haneli_olmalidir), Toast.LENGTH_SHORT).show()
            binding.etVerificationCode.requestFocus()
            return
        }
        
        // Butonu devre dışı bırak
        binding.btnCheckVerification.isEnabled = false
        binding.btnCheckVerification.text = appString(R.string.ui_dogrulaniyor)
        
        authManager.verifyStudentCode(email, code) { success, error ->
            // Butonu tekrar aktif et
            binding.btnCheckVerification.isEnabled = true
            binding.btnCheckVerification.text = appString(R.string.ui_kodu_dogrula)
            
            if (success) {
                // Başarılı - kayıt tamamlandı
                Toast.makeText(this, appString(R.string.ui_kayit_basarili_giris_yapabilirsiniz), Toast.LENGTH_LONG).show()
                // Kısa bir gecikme sonrası login ekranına yönlendir
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }, 1500)
            } else {
                // Hata - kod yanlış veya süresi dolmuş
                Toast.makeText(this, error ?: appString(R.string.ui_kod_dogrulanamadi_lutfen_tekrar_deneyin), Toast.LENGTH_LONG).show()
                binding.etVerificationCode.setText("")
                binding.etVerificationCode.requestFocus()
            }
        }
    }
}
