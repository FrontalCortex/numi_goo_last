package com.example.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        } else if (hasExistingLogin) {
            SessionDeviceManager.requireLoggedInAndSingleDevice(this) {
                prepareStartupAndLaunchMain(
                    questionId = questionId,
                    recipientUid = recipientUid,
                    hasExistingLogin = true,
                )
            }
            return
        } else {
            prepareStartupAndLaunchMain(
                questionId = questionId,
                recipientUid = recipientUid,
                hasExistingLogin = false,
            )
            return
        }
        finish()
    }

    private fun prepareStartupAndLaunchMain(
        questionId: String?,
        recipientUid: String?,
        hasExistingLogin: Boolean,
    ) {
        // Bildirim deep-link'i varsa başlangıç routing'ini bekletme.
        if (!questionId.isNullOrEmpty()) {
            launchMain(
                questionId = questionId,
                recipientUid = recipientUid,
                startDestination = null,
            )
            return
        }

        if (!hasExistingLogin) {
            val firstTutorialShown = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getBoolean("first_tutorial_shown", false)
            val destination = if (firstTutorialShown) {
                MainActivity.START_DESTINATION_MAP
            } else {
                MainActivity.START_DESTINATION_TUTORIAL
            }
            prepareTutorialDataIfNeededAndLaunch(destination, questionId, recipientUid)
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrEmpty()) {
            launchMain(questionId, recipientUid, MainActivity.START_DESTINATION_MAP)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val firstTutorialShown = doc.getBoolean("first_tutorial_shown") == true
                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                    .putBoolean("first_tutorial_shown", firstTutorialShown)
                    .apply()
                val destination = if (firstTutorialShown) {
                    MainActivity.START_DESTINATION_MAP
                } else {
                    MainActivity.START_DESTINATION_TUTORIAL
                }
                prepareTutorialDataIfNeededAndLaunch(destination, questionId, recipientUid)
            }
            .addOnFailureListener {
                val localFallback = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .getBoolean("first_tutorial_shown", false)
                val destination = if (localFallback) {
                    MainActivity.START_DESTINATION_MAP
                } else {
                    MainActivity.START_DESTINATION_TUTORIAL
                }
                prepareTutorialDataIfNeededAndLaunch(destination, questionId, recipientUid)
            }
    }

    private fun prepareTutorialDataIfNeededAndLaunch(
        startDestination: String,
        questionId: String?,
        recipientUid: String?,
    ) {
        if (startDestination != MainActivity.START_DESTINATION_TUTORIAL) {
            launchMain(questionId, recipientUid, startDestination)
            return
        }
        GlobalLessonData.globalPartId = 1
        GlobalLessonData.initialize(this, 1) {
            launchMain(questionId, recipientUid, MainActivity.START_DESTINATION_TUTORIAL)
        }
    }

    private fun launchMain(
        questionId: String?,
        recipientUid: String?,
        startDestination: String?,
    ) {
        val mainIntent = Intent(this, MainActivity::class.java)
        if (!questionId.isNullOrEmpty()) {
            mainIntent.putExtra(MainActivity.EXTRA_OPEN_QUESTION_ID, questionId)
        }
        if (!recipientUid.isNullOrEmpty()) {
            mainIntent.putExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID, recipientUid)
        }
        if (!startDestination.isNullOrEmpty()) {
            mainIntent.putExtra(MainActivity.EXTRA_START_DESTINATION, startDestination)
        }
        startActivity(mainIntent)
        finish()
    }
}
