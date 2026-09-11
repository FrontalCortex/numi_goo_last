package com.example.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * Süreç başlangıcında App Check sağlayıcısını kurar ve ekran takibini bağlar.
 *
 * Firebase'in kendisi `FirebaseInitProvider` (bir ContentProvider) ile bu noktadan ÖNCE
 * hazır hale geldiği için burada ayrıca `FirebaseApp.initializeApp` çağrısına gerek yoktur.
 *
 * Sağlayıcı derleme türüne göre değişiyor (bkz. `src/debug` ve `src/release` altındaki
 * [AppCheckInstaller]): release Play Integrity ile doğrulanır, debug Play'den dağıtılmadığı
 * için doğrulanamaz ve debug sağlayıcıyı kullanır.
 */
class NumiGooApplication : Application() {

    /**
     * Ekran adı üretmeyen fragment'ler. Bunlar kullanıcı için bir "ekran" değil, taşıyıcı
     * ya da görünmez yardımcıdır; rapora girerse gerçek ekranların sayısını bozarlar.
     */
    private val ignoredFragments = setOf(
        "SupportRequestManagerFragment",   // Glide
        "ReportFragment",                  // androidx.lifecycle
        "NavHostFragment",
    )

    // ── Çıkış ekranı ────────────────────────────────────────────────────────
    //
    // Görünen son ekran ve ona ne zaman geçildiği. `SystemClock.elapsedRealtime` kullanılıyor:
    // duvar saati kullanıcı tarafından değiştirilebilir, bu ölçüm ondan etkilenmemeli.
    //
    // Arka plana geçişi ProcessLifecycleOwner yerine "başlamış activity sayacı" ile buluyoruz;
    // yeni bir bağımlılık (lifecycle-process) getirmemek için. Sayaç sıfıra düştüğünde
    // uygulama arka plandadır — tek istisna ekran döndürme, o da isChangingConfigurations
    // ile eleniyor, yoksa her dönüşte sahte bir çıkış kaydedilirdi.

    @Volatile private var currentScreen: String? = null
    @Volatile private var currentScreenStartMs: Long = 0L
    private var startedActivityCount = 0

    /**
     * Fragment bazlı `screen_view`.
     *
     * Firebase'in otomatik ekran toplaması **Activity** adını kullanır. Bu uygulamada
     * gezinmenin neredeyse tamamı MainActivity içindeki fragment değişimleriyle olduğu için
     * otomatik kayıt yalnızca "MainActivity" der ve ders, sandık, kupa, mağaza gibi ekranlar
     * raporlarda hiç görünmez. Bu dinleyici o boşluğu kapatır.
     *
     * Fragment'lerin kendisine dokunulmaz: Activity başına bir kez, özyinelemeli
     * (`recursive = true`) kaydedilir, iç içe fragment'ler de kapsanır.
     */
    private val fragmentCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            val name = f::class.java.simpleName
            if (name.isEmpty() || name in ignoredFragments) return
            currentScreen = name
            currentScreenStartMs = SystemClock.elapsedRealtime()
            AnalyticsLogger.logScreenView(name)
        }
    }

    private val activityCallbacks = object : ActivityLifecycleCallbacksAdapter() {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is FragmentActivity) {
                activity.supportFragmentManager
                    .registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (startedActivityCount == 0) {
                // Öne dönüldü: arka planda geçen süre ekranın süresine yazılmasın.
                currentScreenStartMs = SystemClock.elapsedRealtime()
            }
            startedActivityCount++
        }

        override fun onActivityStopped(activity: Activity) {
            if (startedActivityCount > 0) startedActivityCount--
            if (startedActivityCount != 0) return
            if (activity.isChangingConfigurations) return

            val screen = currentScreen ?: return
            val elapsed = (SystemClock.elapsedRealtime() - currentScreenStartMs).coerceAtLeast(0L)
            AnalyticsLogger.logAppBackground(screen, elapsed)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // App Check kurulumu uygulamanın açılmasının önüne geçmemeli: burada atılan bir
        // istisna süreci daha ilk karede öldürür. Zorlama kapalıyken kurulumun başarısız
        // olması zaten hiçbir çağrıyı bloklamaz.
        try {
            AppCheckInstaller.install(this)
        } catch (e: Throwable) {
            Log.w(TAG, "App Check sağlayıcısı kurulamadı", e)
        }

        // Ölçüm de açılışı bloklamamalı; aynı gerekçeyle sarmalanmıştır.
        try {
            registerActivityLifecycleCallbacks(activityCallbacks)
        } catch (e: Throwable) {
            Log.w(TAG, "Ekran takibi kurulamadı", e)
        }
    }

    /** [Application.ActivityLifecycleCallbacks]'in yalnızca bir metodunu ezebilmek için boş taban. */
    private abstract class ActivityLifecycleCallbacksAdapter : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    companion object {
        const val TAG = "AppCheck"
    }
}
