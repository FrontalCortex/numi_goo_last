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

        // Daha önce indirilmiş medya dosyalarının cache'ini belleğe yükle
        GlobalValues.loadDownloadedMediaCache(applicationContext)

        // Bildirimden mi geldik? questionId varsa beklemeden hemen yönlendir.
        val hasDeepLinkQuestion =
            intent?.getStringExtra(MainActivity.EXTRA_OPEN_QUESTION_ID)?.isNullOrEmpty() == false

        if (hasDeepLinkQuestion) {
            checkLoginStatus()
        } else {
            // Normal açılış: kısa bir splash animasyonu için 2 saniye bekle
            Handler(Looper.getMainLooper()).postDelayed({
                checkLoginStatus()
            }, 2000)
        }
    }
    
    private fun checkLoginStatus() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
      //prefs.edit().putBoolean("login_start_ever_shown", false).apply()
        val loginStartEverShown = prefs.getBoolean("login_start_ever_shown", false)
        val hasExistingLogin = FirebaseAuth.getInstance().currentUser != null
        val questionId = intent?.getStringExtra(MainActivity.EXTRA_OPEN_QUESTION_ID)
        val recipientUid = intent?.getStringExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID)
        if (!isOnline()) {
            // İnternet yoksa, login akışına girmeden doğrudan MainActivity'e geç;
            // MainActivity açıldığında OfflineFragment gösterecek.
            val mainIntent = Intent(this, MainActivity::class.java)
            if (!questionId.isNullOrEmpty()) {
                mainIntent.putExtra(MainActivity.EXTRA_OPEN_QUESTION_ID, questionId)
            }
            if (!recipientUid.isNullOrEmpty()) {
                mainIntent.putExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID, recipientUid)
            }
            startActivity(mainIntent)
        } else if (loginStartEverShown && !hasExistingLogin) {
            startActivity(Intent(this, LoginStartActivity::class.java))
        } else {
            val mainIntent = Intent(this, MainActivity::class.java)
            if (!questionId.isNullOrEmpty()) {
                mainIntent.putExtra(MainActivity.EXTRA_OPEN_QUESTION_ID, questionId)
            }
            if (!recipientUid.isNullOrEmpty()) {
                mainIntent.putExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID, recipientUid)
            }
            startActivity(mainIntent)
        }
        finish()
    }
}
