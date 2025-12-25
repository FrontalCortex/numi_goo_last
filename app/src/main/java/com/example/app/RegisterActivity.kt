package com.example.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authManager: AuthManager
    
    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager()
        authManager.initialize(this)
        
        // Eğer Google Sign-In'den geldiyse formu doldur
        val googleEmail = intent.getStringExtra("google_email")
        val googleName = intent.getStringExtra("google_name")
        if (!googleEmail.isNullOrEmpty()) {
            binding.etEmail.setText(googleEmail)
            if (!googleName.isNullOrEmpty()) {
                binding.etName.setText(googleName)
            }
            // Öğrenci seçili olsun
            binding.rbStudent.isChecked = true
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            if (binding.rbStudent.isChecked) {
                registerStudent()
            } else {
                registerTeacher()
            }
        }
        
        // Giriş sayfasına dön
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        // Kullanıcı tipi değiştiğinde UI güncelle
        binding.rbStudent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tilApprovalCode.visibility = android.view.View.GONE
                binding.tvApprovalCode.visibility = android.view.View.GONE
                binding.btnRequestCode.visibility = android.view.View.GONE
            }
        }
        
        binding.rbTeacher.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tilApprovalCode.visibility = android.view.View.VISIBLE
                binding.tvApprovalCode.visibility = android.view.View.VISIBLE
                binding.btnRequestCode.visibility = android.view.View.VISIBLE
            }
        }

        // Öğretmen kodu iste
        binding.btnRequestCode.setOnClickListener {
            val candidateEmail = binding.etEmail.text.toString()
            if (candidateEmail.isEmpty()) {
                showError("Önce e-posta adresinizi girin")
                return@setOnClickListener
            }
            authManager.requestTeacherInviteCode(candidateEmail) { success, error ->
                if (success) {
                    showSuccess("Onay kodu e-posta ile gönderildi")
                } else {
                    showError(error ?: "Kod gönderilemedi")
                }
            }
        }
        
        // Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }
    
    private fun signInWithGoogle() {
        android.util.Log.d("RegisterActivity", "signInWithGoogle() çağrıldı - kayıt için")
        
        // Önce mevcut Google Sign-In oturumunu kapat (hesap seçim ekranının görünmesi için)
        GoogleSignIn.getClient(this, com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            .addOnCompleteListener {
                android.util.Log.d("RegisterActivity", "Mevcut Google Sign-In oturumu kapatıldı")
                
                try {
                    val signInIntent = authManager.getGoogleSignInIntent()
                    android.util.Log.d("RegisterActivity", "Google Sign-In Intent alındı, activity başlatılıyor...")
                    if (signInIntent != null) {
                        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
                        android.util.Log.d("RegisterActivity", "Google Sign-In activity başlatıldı")
                    } else {
                        android.util.Log.e("RegisterActivity", "Google Sign-In Intent null!")
                        showError("Google kaydı başlatılamadı")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RegisterActivity", "Google Sign-In başlatılırken hata oluştu", e)
                    showError("Google kaydı başlatılamadı: ${e.localizedMessage}")
                }
            }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            // Kullanıcı iptal ettiyse hiçbir şey yapma
            if (resultCode != RESULT_OK) {
                android.util.Log.d("RegisterActivity", "Google Sign-In iptal edildi veya başarısız")
                return
            }
            
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            // autoRegister = true parametresi ile çağır (kayıt ekranından çağrıldığı için)
            authManager.handleGoogleSignInResult(task, autoRegister = true) { success, error ->
                if (success) {
                    // Google Sign-In ile gelen kullanıcılar otomatik öğrenci olarak kaydedilir
                    android.util.Log.d("RegisterActivity", "Google ile kayıt başarılı, MainActivity'ye yönlendiriliyor")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Sadece gerçek hataları göster, kullanıcı iptal ettiyse sessizce dön
                    if (error != "Kullanıcı girişi iptal edildi") {
                        showError(error ?: "Google kaydı başarısız")
                    }
                }
            }
        }
    }
    
    private fun registerStudent() {
        val email = binding.etEmail.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        if (email.isEmpty() || name.isEmpty() || password.isEmpty()) {
            showError("Lütfen tüm alanları doldurun")
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Geçerli bir e-posta adresi girin")
            return
        }
        
        if (password.length < 6) {
            showError("Şifre en az 6 karakter olmalı")
            return
        }
        
        // Butonu devre dışı bırak (çift tıklamayı önle)
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Kayıt yapılıyor..."
        
        android.util.Log.d("RegisterActivity", "registerStudent çağrıldı - email: $email, name: $name")
        
        authManager.registerStudent(email, name, password) { success, error ->
            // Butonu tekrar aktif et
            binding.btnRegister.isEnabled = true
            binding.btnRegister.text = "Kayıt Ol"
            
            if (success) {
                // Başarılı - direkt MainActivity'ye yönlendir
                android.util.Log.d("RegisterActivity", "Kayıt başarılı, MainActivity'ye yönlendiriliyor")
                Toast.makeText(this, "Kayıt başarılı! Hoş geldiniz.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // Hata oluştu
                android.util.Log.e("RegisterActivity", "Kayıt başarısız: $error")
                showError(error ?: "Kayıt başarısız. Lütfen tekrar deneyin.")
            }
        }
    }
    
    private fun registerTeacher() {
        val email = binding.etEmail.text.toString()
        val name = binding.etName.text.toString()
        val password = binding.etPassword.text.toString()
        val approvalCode = binding.etApprovalCode.text.toString()
        
        if (email.isEmpty() || name.isEmpty() || password.isEmpty() || approvalCode.isEmpty()) {
            showError("Lütfen tüm alanları doldurun")
            return
        }
        
        if (password.length < 6) {
            showError("Şifre en az 6 karakter olmalı")
            return
        }
        
        authManager.registerTeacher(email, name, password, approvalCode) { success, error ->
            if (success) {
                showSuccess("Öğretmen hesabı oluşturuldu")
                startActivity(Intent(this, TeacherLoginActivity::class.java))
                finish()
            } else {
                showError(error ?: "Geçersiz onay kodu veya kayıt başarısız")
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
