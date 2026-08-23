package com.example.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class SplashActivity : AppCompatActivity() {

    companion object {
        // Splash animasyonunun minimum süresi (mevcut davranış).
        private const val SPLASH_ANIMATION_MS = 2000L
        // Remote Config fetch'i bu süreden uzun sürerse, daha fazla bekletmeden devam edilir
        // (uygulama zaten internetsiz çalışmadığı için burada güncel veriyi garantilemek adına
        // yüksek bir üst sınır tutuluyor; süre dolunca yine de pes edip devam eder, sonsuza kadar beklemez).
        private const val SPLASH_MAX_EXTRA_WAIT_MS = 8000L
    }

    private var splashTimerDone = false
    private var remoteConfigFetchDone = false
    private var proceededPastSplash = false

    private fun logFirstTutorial(event: String, details: String = "") {
        val msg = if (details.isEmpty()) event else "$event | $details"
        Log.d(MainActivity.FIRST_TUTORIAL_LOG_TAG, msg)
    }

    // Tutorial içeriği artık Remote Config'ten geliyor (bkz. TutorialFragment.createTutorialSteps1).
    // Kullanıcı map'ten bir tutorial'a girdiğinde (LessonAdapter, MainActivity) güncel veriyi ilk
    // seferde görebilsin diye, uygulama açılışında (burada, tek merkezi yerde) bir kez fetch
    // ediyoruz. fetchAndActivate() session boyunca cache'lendiği için, buradan sonraki her okuma
    // (hangi ekrandan olursa olsun) zaten güncel değeri kullanır.
    private fun startTutorialRemoteConfigFetch() {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            // setConfigSettingsAsync() de asenkron - fetchAndActivate()'i onun bitmesini BEKLEMEDEN
            // çağırmak (eskiden burada bir race vardı), o anki fetch'in hangi throttle ayarıyla
            // çalışacağını belirsizleştiriyordu. Şimdi sıraya koyuyoruz: önce ayar, o bitince fetch.
            remoteConfig.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (isDebuggable) 0L else 3600L)
                    .build()
            ).addOnCompleteListener {
                remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                    logFirstTutorial(
                        "Splash.remoteConfig",
                        "fetch tamamlandı başarılı=${task.isSuccessful} sonuç=${task.result} " +
                            "lastFetchStatus=${remoteConfig.info.lastFetchStatus} " +
                            "fetchTimeMillis=${remoteConfig.info.fetchTimeMillis}",
                    )
                    remoteConfigFetchDone = true
                    maybeProceedPastSplash()
                }
            }
        } catch (e: Exception) {
            Log.e(MainActivity.FIRST_TUTORIAL_LOG_TAG, "Splash Remote Config başlatılamadı", e)
            remoteConfigFetchDone = true
            maybeProceedPastSplash()
        }
    }

    private fun maybeProceedPastSplash() {
        if (proceededPastSplash) return
        if (splashTimerDone && remoteConfigFetchDone) {
            proceededPastSplash = true
            checkLoginStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Navigation bar rengini background_color yap
        window.navigationBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.background_color)
        
        setContentView(R.layout.activity_splash)

        // Daha önce indirilmiş medya dosyalarının cache'ini belleğe yükle
        GlobalValues.loadDownloadedMediaCache(applicationContext)

        // Bildirimden mi geldik? questionId varsa beklemeden hemen yönlendir.
        val hasDeepLinkQuestion =
            intent?.getStringExtra(MainActivity.EXTRA_OPEN_QUESTION_ID)?.isNullOrEmpty() == false

        if (hasDeepLinkQuestion) {
            checkLoginStatus()
        } else {
            // Tutorial Remote Config fetch'i, splash animasyonuyla paralel başlatılır.
            startTutorialRemoteConfigFetch()

            // Normal açılış: kısa bir splash animasyonu için minimum bekleme
            Handler(Looper.getMainLooper()).postDelayed({
                splashTimerDone = true
                maybeProceedPastSplash()
            }, SPLASH_ANIMATION_MS)

            // Güvenlik ağı: fetch bu süreye kadar bitmezse (yavaş/yok internet), daha fazla
            // bekletmeden devam et.
            Handler(Looper.getMainLooper()).postDelayed({
                splashTimerDone = true
                remoteConfigFetchDone = true
                maybeProceedPastSplash()
            }, SPLASH_ANIMATION_MS + SPLASH_MAX_EXTRA_WAIT_MS)
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
                if (!doc.exists()) {
                    // Hesap Authentication'da varsa ama Firestore'da silinmişse, oturumu kapat ve girişe at.
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this@SplashActivity, LoginStartActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                    return@addOnSuccessListener
                }
                
                val firestoreRaw = doc.getBoolean("first_tutorial_shown")
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
