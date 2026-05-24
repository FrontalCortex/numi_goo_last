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

    private fun logFirstTutorial(event: String, details: String = "") {
        val msg = if (details.isEmpty()) event else "$event | $details"
        Log.d(MainActivity.FIRST_TUTORIAL_LOG_TAG, msg)
    }

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
        val loginStartEverShown = prefs.getBoolean("login_start_ever_shown", false)
        val hasExistingLogin = FirebaseAuth.getInstance().currentUser != null
        val firstTutorialShownLocal = FirstTutorialShownStore.readLocal(this)
        val questionId = intent?.getStringExtra(MainActivity.EXTRA_OPEN_QUESTION_ID)
        val recipientUid = intent?.getStringExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID)
        logFirstTutorial(
            "Splash.checkLoginStatus",
            "online=${isOnline()} loginStartEverShown=$loginStartEverShown hasAuth=$hasExistingLogin " +
                "first_tutorial_shown(local)=$firstTutorialShownLocal questionId=${questionId?.take(8)}",
        )
        if (!isOnline()) {
            logFirstTutorial("Splash.route", "offline -> MainActivity (no start_destination)")
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
            logFirstTutorial("Splash.route", "LoginStartActivity")
            startActivity(Intent(this, LoginStartActivity::class.java))
        } else if (hasExistingLogin) {
            logFirstTutorial("Splash.route", "hasAuth -> prepareStartup hasExistingLogin=true")
            SessionDeviceManager.requireLoggedInAndSingleDevice(this) {
                prepareStartupAndLaunchMain(
                    questionId = questionId,
                    recipientUid = recipientUid,
                    hasExistingLogin = true,
                )
            }
            return
        } else {
            logFirstTutorial("Splash.route", "guest -> prepareStartup hasExistingLogin=false")
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
            val firstTutorialShown = FirstTutorialShownStore.readLocal(this)
            val destination = if (firstTutorialShown) {
                MainActivity.START_DESTINATION_MAP
            } else {
                MainActivity.START_DESTINATION_TUTORIAL
            }
            logFirstTutorial(
                "Splash.prepareStartup.guest",
                "first_tutorial_shown=$firstTutorialShown destination=$destination",
            )
            prepareTutorialDataIfNeededAndLaunch(destination, questionId, recipientUid)
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrEmpty()) {
            logFirstTutorial("Splash.prepareStartup", "uid empty -> MAP")
            launchMain(questionId, recipientUid, MainActivity.START_DESTINATION_MAP)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val firestoreRaw = if (doc.exists()) doc.getBoolean("first_tutorial_shown") else null
                val firstTutorialShown = FirstTutorialShownStore.resolveShown(
                    this@SplashActivity,
                    firestoreRaw,
                    "Splash.firestore",
                )
                if (firstTutorialShown) {
                    FirstTutorialShownStore.repairFirestoreIfLocalShown(this@SplashActivity, "Splash.firestore")
                }
                val destination = if (firstTutorialShown) {
                    MainActivity.START_DESTINATION_MAP
                } else {
                    MainActivity.START_DESTINATION_TUTORIAL
                }
                logFirstTutorial(
                    "Splash.prepareStartup.firestore",
                    "uid=${uid.take(8)} exists=${doc.exists()} firestoreRaw=$firestoreRaw " +
                        "resolved=$firstTutorialShown destination=$destination",
                )
                prepareTutorialDataIfNeededAndLaunch(destination, questionId, recipientUid)
            }
            .addOnFailureListener { e ->
                val firstTutorialShown = FirstTutorialShownStore.resolveShown(
                    this@SplashActivity,
                    firestoreValue = null,
                    logSource = "Splash.firestore.FAIL",
                )
                val destination = if (firstTutorialShown) {
                    MainActivity.START_DESTINATION_MAP
                } else {
                    MainActivity.START_DESTINATION_TUTORIAL
                }
                logFirstTutorial(
                    "Splash.prepareStartup.firestore",
                    "FAIL resolved=$firstTutorialShown destination=$destination err=${e.message}",
                )
                prepareTutorialDataIfNeededAndLaunch(destination, questionId, recipientUid)
            }
    }

    private fun prepareTutorialDataIfNeededAndLaunch(
        startDestination: String,
        questionId: String?,
        recipientUid: String?,
    ) {
        if (startDestination != MainActivity.START_DESTINATION_TUTORIAL) {
            logFirstTutorial("Splash.prepareTutorialData", "skip init -> launchMain dest=$startDestination")
            launchMain(questionId, recipientUid, startDestination)
            return
        }
        logFirstTutorial("Splash.prepareTutorialData", "GlobalLessonData.initialize partId=1")
        GlobalLessonData.globalPartId = 1
        GlobalLessonData.initialize(this, 1) {
            logFirstTutorial(
                "Splash.prepareTutorialData",
                "init done lessonItems=${GlobalLessonData.lessonItems.size} item1=${GlobalLessonData.getLessonItem(1)?.tutorialNumber}",
            )
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
        logFirstTutorial(
            "Splash.launchMain",
            "start_destination=${startDestination ?: "null"} questionId=${questionId?.take(8)}",
        )
        startActivity(mainIntent)
        finish()
    }
}
