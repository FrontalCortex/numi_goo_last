package com.example.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

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
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val loginStartEverShown = prefs.getBoolean("login_start_ever_shown", false)
        val hasExistingLogin = FirebaseAuth.getInstance().currentUser != null
        if (loginStartEverShown && !hasExistingLogin) {

            Log.d("kesl",loginStartEverShown.toString())
            Log.d("kesl",hasExistingLogin.toString())
            startActivity(Intent(this, LoginStartActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
