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
     * Açık ekranı değiştirir.
     *
     * Ekran ölçümü ile çalışma süresi ölçümü ([StudyTimeTracker]) aynı geçişleri dinliyor.
     * İkinci bir yaşam döngüsü dinleyicisi kurmak yerine tek kapı: böylece "arka plana
     * geçince süre işlememeli" gibi kurallar iki yerde ayrı ayrı doğrulanmak zorunda kalmıyor.
     */
    private fun setCurrentScreen(name: String?) {
        currentScreen = name
        currentScreenStartMs = SystemClock.elapsedRealtime()
        StudyTimeTracker.setActiveScreen(this, name)
    }

    /**
     * Görünür ekranların yığını.
     *
     * Dialog'lar yüzünden gerekli: bir [androidx.fragment.app.DialogFragment] kapandığında
     * altındaki fragment yeniden `onResume`'a GİRMEZ — dialog host'u hiç pause etmediği için.
     * Yığın olmadan [currentScreen] kapanan dialog'un adında takılı kalıyordu ve kullanıcı
     * paneli kapatıp on dakika daha oynayıp çıksa bile çıkış o dialog'a yazılıyordu.
     *
     * Uygulamada dokuz DialogFragment var; en çok zarar gören ikisi ProDiffirentFragment ve
     * PlanFragment, yani Pro hunisinin tamamı.
     *
     * Yalnızca ana iş parçacığından okunup yazılır (fragment yaşam döngüsü geri çağırımları).
     */
    private val screenStack = mutableListOf<String>()

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
            // Aynı ad yığında birden fazla kez durmasın: arka plandan dönüşte görünür
            // fragment'ların hepsi yeniden resume oluyor.
            screenStack.remove(name)
            screenStack.add(name)
            setCurrentScreen(name)
            AnalyticsLogger.logScreenView(name)
        }

        /**
         * Fragment görünümü yok edildi: kapatıldı ya da yerine başkası geldi. Üstteki ekran
         * gidiyorsa altındaki geri yüklenir.
         *
         * **Neden `onFragmentPaused` değil:** uygulama arka plana geçerken fragment'lar da
         * pause olur ama görünümleri yok edilmez. Pause'da yığını boşaltsaydık, arka plana
         * geçişte üstteki ekranı düşürür ve `app_exit_screen`'i alttaki ekrana yazardık —
         * düzeltmeye çalıştığımız hatanın aynısını ters yönde üretirdik.
         *
         * Uygulama gerçekten kapanırken sıralama `onPause → onStop → onDestroyView`; çıkış
         * olayı `onActivityStopped`'ta, yani yığın bozulmadan ÖNCE kaydedilir.
         */
        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            val name = f::class.java.simpleName
            if (name.isEmpty() || name in ignoredFragments) return
            screenStack.remove(name)
            if (currentScreen != name) return
            val restored = screenStack.lastOrNull() ?: return
            // Altındaki ekrana "yeniden gelinmiş" sayılır; dialog'un açık kaldığı süre
            // o ekranın süresine eklenmemeli.
            setCurrentScreen(restored)
        }
    }

    private val activityCallbacks = object : ActivityLifecycleCallbacksAdapter() {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is FragmentActivity) {
                activity.supportFragmentManager
                    .registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
            }
        }

        override fun onActivityResumed(activity: Activity) {
            // Reklam, rıza formu, satın alma gibi ekranlar KENDİ Activity'lerinde açılır ve
            // fragment içermezler; onFragmentResumed onlar için hiç tetiklenmez. Bu yüzden
            // burada activity adı yazılıyor.
            //
            // Kendi ekranlarımızda bu değer hemen ardından fragment adıyla eziliyor:
            // dispatchActivityResumed Activity.onResume içinden çağrılıyor, fragment'lar ise
            // onPostResume/onResumeFragments ile DAHA SONRA resume oluyor. Sıra garantili.
            //
            // Bu olmadan reklam açıkken uygulamadan çıkılınca exit_screen bir önceki DERSİN
            // adını gösteriyordu — reklam terkleri derslerin üstüne yazılıyordu.
            setCurrentScreen(activity::class.java.simpleName)
        }

        override fun onActivityStarted(activity: Activity) {
            if (startedActivityCount == 0) {
                // Öne dönüldü: arka planda geçen süre ekranın süresine yazılmasın.
                // Aynı ekrana dönülüyor, o yüzden ad değişmiyor — yalnızca sayaçlar yeniden
                // başlatılıyor.
                setCurrentScreen(currentScreen)
                // 30 dakikadan uzun sürdüyse yeni oturum sayılır; oturum içi ders sayacı sıfırlanır.
                EnergySessionCounter.onAppForegrounded()
            }
            startedActivityCount++
        }

        override fun onActivityStopped(activity: Activity) {
            if (startedActivityCount > 0) startedActivityCount--
            if (startedActivityCount != 0) return
            if (activity.isChangingConfigurations) return

            EnergySessionCounter.onAppBackgrounded()

            // Arka planda çalışma süresi işlemez; açık parça burada kapanıyor. Ekran ADI
            // korunuyor ki öne dönüldüğünde aynı yerden devam edilebilsin.
            StudyTimeTracker.setActiveScreen(this@NumiGooApplication, null)

            val screen = currentScreen ?: return
            val elapsed = (SystemClock.elapsedRealtime() - currentScreenStartMs).coerceAtLeast(0L)
            AnalyticsLogger.logAppExitScreen(screen, elapsed)
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

        // Sezon saatinin kalıcı sapmasını yükle. Ağ çağrısı yok, yalnızca disk okuması;
        // sunucuyla eşitleme oturum açıldıktan sonra MainActivity'de yapılıyor.
        try {
            SeasonClock.init(this)
        } catch (e: Throwable) {
            Log.w(TAG, "Sezon saati kurulamadı", e)
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
