package com.example.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    
    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager()
        authManager.initialize(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        // Öğrenci girişi - Google hesap seçimi ile başlar
        binding.btnLogin.setOnClickListener {
            signInWithGoogle()
        }
        
        // Öğretmen girişi
        binding.btnTeacherLogin.setOnClickListener {
            startActivity(Intent(this, TeacherLoginActivity::class.java))
        }
        
        // Google Sign-In (alternatif buton - aynı işlevi yapar)
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
        
        // Kayıt ol
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun signInWithGoogle() {
        android.util.Log.d("LoginActivity", "signInWithGoogle() çağrıldı")
        
        // Önce mevcut Google Sign-In oturumunu kapat (hesap seçim ekranının görünmesi için)
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            .addOnCompleteListener {
                android.util.Log.d("LoginActivity", "Mevcut Google Sign-In oturumu kapatıldı")
                
                try {
                    val signInIntent = authManager.getGoogleSignInIntent()
                    android.util.Log.d("LoginActivity", "Google Sign-In Intent alındı, activity başlatılıyor...")
                    if (signInIntent != null) {
                        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
                        android.util.Log.d("LoginActivity", "Google Sign-In activity başlatıldı")
                    } else {
                        android.util.Log.e("LoginActivity", "Google Sign-In Intent null!")
                        showError("Google girişi başlatılamadı")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LoginActivity", "Google Sign-In başlatılırken hata oluştu", e)
                    showError("Google girişi başlatılamadı: ${e.localizedMessage}")
                }
            }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            // Kullanıcı iptal ettiyse hiçbir şey yapma
            if (resultCode != RESULT_OK) {
                android.util.Log.d("LoginActivity", "Google Sign-In iptal edildi veya başarısız")
                return
            }
            
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            authManager.handleGoogleSignInResult(task) { success, error ->
                if (success) {
                    // Başarılı giriş - MainActivity'ye git
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Eğer hesap kayıtlı değilse kayıt ekranına yönlendir
                    if (error == "ACCOUNT_NOT_REGISTERED") {
                        val intent = Intent(this, RegisterActivity::class.java)
                        intent.putExtra("google_sign_in", true)
                        // Seçilen Google hesabının email'ini de gönder
                        try {
                            val account = task.result
                            if (account != null) {
                                intent.putExtra("google_email", account.email)
                                intent.putExtra("google_name", account.displayName)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LoginActivity", "Google account bilgisi alınamadı", e)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        // Sadece gerçek hataları göster, kullanıcı iptal ettiyse sessizce dön
                        if (error != "Kullanıcı girişi iptal edildi") {
                            showError(error ?: "Google girişi başarısız")
                        }
                    }
                }
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
