package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(FontScaleLimiter.wrap(newBase))
    }

    companion object {
        // Açılışın en kısa süresi (onCreate'ten). Eskiden 2 sn'ydi ve bir şey yüklenmediği hâlde
        // çark dönüyordu (10.11.2026); şimdi marka bir kez okunacak kadar: sistem ikonu + yazının
        // gelişi (~450 ms) + kısa bekleme.
        private const val SPLASH_ANIMATION_MS = 1200L
        private const val BRAND_REVEAL_MS = 450L
        // Zorunlu güncelleme kontrolü animasyon bittikten sonra en fazla bu kadar bekletir;
        // çekme genelde animasyon sürerken biter. Bkz. ForceUpdateManager.runIfAllowed.
        private const val FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS = 1000L
        // Bildirimden gelen açılışta animasyon beklenmediği için süre biraz daha uzun.
        private const val FORCE_UPDATE_MAX_WAIT_DEEP_LINK_MS = 1500L
    }

    private var proceededPastSplash = false

    private fun logFirstTutorial(event: String, details: String = "") {
        val msg = if (details.isEmpty()) event else "$event | $details"
        Log.d(MainActivity.FIRST_TUTORIAL_LOG_TAG, msg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // super'den ve setContentView'dan önce: sistemin açılış ekranı (Theme.App.Starting) buradan
        // sonra Theme.NumiGoo'ya geçiyor.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Navigation bar rengini background_color yap
        window.navigationBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.background_color)
        
        setContentView(R.layout.activity_splash)
        splashScreen.setOnExitAnimationListener { provider -> revealBrand(provider) }

        // Davet bağlantısı (/d/<kod>) uygulama yüklüyken açıldı (App Links). Atıf yok: bu kişi zaten
        // kullanıcı; yalnızca "paylaşılan bağlantıya basıldı" sayılıyor. Uygulama normal akışla açılır.
        if (intent?.action == android.content.Intent.ACTION_VIEW && intent?.data?.path?.startsWith("/d/") == true) {
            AnalyticsLogger.logSimpleEvent("ref_link_open")
        }

        // Daha önce indirilmiş medya dosyalarının cache'ini belleğe yükle
        GlobalValues.loadDownloadedMediaCache(applicationContext)

        // Bildirimden mi geldik? questionId varsa beklemeden hemen yönlendir.
        val hasDeepLinkQuestion =
            intent?.getStringExtra(MainActivity.EXTRA_OPEN_QUESTION_ID)?.isNullOrEmpty() == false

        // Zorunlu güncelleme için Remote Config'i animasyonla paralel çekmeye başla.
        ForceUpdateManager.startFetch()

        if (hasDeepLinkQuestion) {
            ForceUpdateManager.runIfAllowed(this, FORCE_UPDATE_MAX_WAIT_DEEP_LINK_MS) {
                checkLoginStatus()
            }
        } else {
            // Normal açılış: splash animasyonu kadar beklenir.
            //
            // Eskiden burada öğretici içeriği için Remote Config indiriliyor ve açılış onu
            // 8 saniyeye kadar bekliyordu; öğreticiler artık uygulamanın içinden okunuyor
            // (bkz. TutorialFragment.createTutorialSteps1). Remote Config'ten yalnızca zorunlu
            // güncelleme eşiği okunuyor ve o da açılışı en fazla ~1 sn uzatabiliyor.
            Handler(Looper.getMainLooper()).postDelayed({
                if (proceededPastSplash) return@postDelayed
                proceededPastSplash = true
                ForceUpdateManager.runIfAllowed(this, FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS) {
                    checkLoginStatus()
                }
            }, SPLASH_ANIMATION_MS)
        }
    }
    
    /**
     * Sistemin açılış ekranından bizimkine geçiş: kendi ikonumuzu sistem ikonunun tam üstüne
     * (aynı yer ve boy) koyup sistem ekranını kaldırıyoruz; sonra ikon yukarı kayıyor ve altından
     * "Sorobit" beliriyor. Kullanıcı tek bir ekran görüyor.
     *
     * Sistem ekranı gösterilmediyse (ör. uygulama zaten açıkken bağlantıyla gelme) bu çağrılmıyor;
     * düzen o zaman doğrudan son hâliyle görünüyor.
     */
    private fun revealBrand(provider: SplashScreenViewProvider) {
        val brand = findViewById<View>(R.id.splashBrand)
        val icon = findViewById<View>(R.id.splashIcon)
        val wordmark = findViewById<View>(R.id.splashWordmark)
        runCatching {
            val sys = provider.iconView
            if (sys.width == 0 || icon.width == 0) return@runCatching
            val s = IntArray(2).also { sys.getLocationInWindow(it) }
            val o = IntArray(2).also { icon.getLocationInWindow(it) }
            // Android 12+ ikon kutusu görünen maske boyunda (192dp), resim onun 1,5 katı çiziliyor
            // (uyarlanabilir ikondaki 108/72 gibi); kutu genişliğiyle hesaplayınca ikonumuz ⅔ boyda
            // başlayıp büyüyordu (10.11.2026 MIUI 14 kaydı). Eski sürümlerde (kütüphanenin kendi
            // çizimi) kutu resmin boyunda.
            val drawn = sys.width * (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 1.5f else 1f)
            val scale = drawn / icon.width
            brand.pivotX = icon.left + icon.width / 2f
            brand.pivotY = icon.top + icon.height / 2f
            brand.scaleX = scale
            brand.scaleY = scale
            brand.translationX = (s[0] + sys.width / 2f) - (o[0] + icon.width / 2f)
            brand.translationY = (s[1] + sys.height / 2f) - (o[1] + icon.height / 2f)
            wordmark.alpha = 0f
            wordmark.translationY = 12 * resources.displayMetrics.density
        }
        provider.remove()
        val ease = DecelerateInterpolator(1.6f)
        brand.animate().translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(BRAND_REVEAL_MS).setInterpolator(ease).start()
        wordmark.animate().alpha(1f).translationY(0f)
            .setStartDelay(BRAND_REVEAL_MS / 3).setDuration(BRAND_REVEAL_MS).setInterpolator(ease).start()
    }

    private fun checkLoginStatus() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val loginStartEverShown = prefs.getBoolean("login_start_ever_shown", false)
        val hasExistingLogin = FirebaseAuth.getInstance().currentUser != null
        val hadAccount = DeviceAccountStore.hasEverHadAccount(this)
        val questionId = intent?.getStringExtra(MainActivity.EXTRA_OPEN_QUESTION_ID)
        val recipientUid = intent?.getStringExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID)
        logFirstTutorial(
            "Splash.checkLoginStatus",
            "online=${isOnline()} loginStartEverShown=$loginStartEverShown hasAuth=$hasExistingLogin " +
                "account_ever_created=$hadAccount questionId=${questionId?.take(8)}",
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
            // Şart tek: bu cihazda daha önce hesap açıldı mı. Açıldıysa oturum kapalı olsa
            // bile tutorial gösterilmiyor.
            val hadAccount = DeviceAccountStore.hasEverHadAccount(this)
            val destination = if (hadAccount) {
                MainActivity.START_DESTINATION_MAP
            } else {
                MainActivity.START_DESTINATION_TUTORIAL
            }
            logFirstTutorial(
                "Splash.prepareStartup.guest",
                "account_ever_created=$hadAccount destination=$destination",
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
                
                // Oturum açık: bu cihazda hesap var, yani tutorial gösterilmiyor. Firestore
                // okuması artık yalnızca yukarıdaki "hesap silinmiş mi" kontrolü için
                // yapılıyor; hedef kararı ona bağlı değil.
                DeviceAccountStore.hasEverHadAccount(this@SplashActivity)
                logFirstTutorial(
                    "Splash.prepareStartup.firestore",
                    "uid=${uid.take(8)} exists=${doc.exists()} -> MAP",
                )
                prepareTutorialDataIfNeededAndLaunch(
                    MainActivity.START_DESTINATION_MAP,
                    questionId,
                    recipientUid,
                )
            }
            .addOnFailureListener { e ->
                // Okuma başarısız oldu ama oturum açık; hesabın varlığından şüphe yok.
                DeviceAccountStore.hasEverHadAccount(this@SplashActivity)
                logFirstTutorial(
                    "Splash.prepareStartup.firestore",
                    "FAIL -> MAP err=${e.message}",
                )
                prepareTutorialDataIfNeededAndLaunch(
                    MainActivity.START_DESTINATION_MAP,
                    questionId,
                    recipientUid,
                )
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
