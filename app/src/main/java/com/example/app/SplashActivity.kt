package com.example.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // 2 saniye bekle
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, 2000)
    }
    
    private fun checkLoginStatus() {
        val authManager = AuthManager()
        authManager.initialize(this)
        
        // Kullanıcının giriş yapıp yapmadığını kontrol et
        if (authManager.isLoggedIn()) {
            // Giriş yapılmış - Firestore'da kullanıcı kaydı var mı kontrol et
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Firestore'da kullanıcı kontrolü yap
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            // Kullanıcı kayıtlı - MainActivity'ye git
                            android.util.Log.d("SplashActivity", "Kullanıcı giriş yapmış, MainActivity'ye yönlendiriliyor")
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            // Firestore'da kayıt yok - LoginActivity'ye git
                            android.util.Log.d("SplashActivity", "Firestore'da kullanıcı kaydı yok, LoginActivity'ye yönlendiriliyor")
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    }
                    .addOnFailureListener {
                        // Hata durumunda LoginActivity'ye git
                        android.util.Log.e("SplashActivity", "Firestore kontrolü başarısız", it)
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            } else {
                // Firebase Auth'da kullanıcı yok - LoginActivity'ye git
                android.util.Log.d("SplashActivity", "Firebase Auth'da kullanıcı yok, LoginActivity'ye yönlendiriliyor")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } else {
            // Giriş yapılmamış - LoginActivity'ye git
            android.util.Log.d("SplashActivity", "Kullanıcı giriş yapmamış, LoginActivity'ye yönlendiriliyor")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
