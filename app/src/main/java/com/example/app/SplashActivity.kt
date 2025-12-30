package com.example.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

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
        // İlk açılış kontrolü MainActivity'de yapılacak
        // Bu yüzden her zaman MainActivity'ye yönlendir (MainActivity'de ilk açılışsa TutorialFragment gösterilecek)
        // Kayıt olsun olmasın, ilk açılışta TutorialFragment gösterilecek
        android.util.Log.d("SplashActivity", "MainActivity'ye yönlendiriliyor (ilk açılış kontrolü MainActivity'de yapılacak)")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
