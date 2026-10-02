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
    private val screenStack = mutableListOf<ScreenEntry>()

    /**
     * Yığındaki bir ekran: adı ve o adı yığına koyan fragment örneği.
     *
     * Örnek de tutuluyor çünkü aynı sınıftan iki fragment aynı anda yaşayabiliyor: kapanmış
     * ama görünümü henüz yok edilmemiş bir ders ekranı ile yeni açılanı (bkz. [onScreenGone]).
     * Yalnızca ad tutulsaydı eskisinin gecikmiş "gittim" haberi yenisinin kaydını silerdi.
     * Zayıf referans: bu liste süreç boyu yaşıyor, fragment'ları bellekte tutmamalı.
     */
    private class ScreenEntry(val name: String, fragment: Fragment) {
        val owner = java.lang.ref.WeakReference(fragment)
    }

    /**
     * Bir fragment ekrandan gidiyor: yığından çıkarılır, üstteki oysa altındaki geri yüklenir.
     *
     * İki ayrı haberden geliyor ([fragmentCallbacks]): fragment kaldırılırken ve görünümü yok
     * edilirken. İkincisi tek başına yetmiyordu — sebebi `onFragmentPaused`'ın açıklamasında.
     * Aynı fragment için iki haber de gelir; ikincisi yığında kaydı bulamaz ve hiçbir şey
     * yapmaz.
     */
    private fun onScreenGone(f: Fragment) {
        val name = f::class.java.simpleName
        if (name.isEmpty() || name in ignoredFragments) return
        // Yalnızca BU örneğin koyduğu kayıt: aynı adı artık başka bir örnek taşıyorsa (yeni
        // açılan ders) ona dokunulmuyor.
        val removed = screenStack.removeAll { it.name == name && it.owner.get() === f }
        if (!removed || currentScreen != name) return
        val restored = screenStack.lastOrNull()?.name ?: return
        // Altındaki ekrana "yeniden gelinmiş" sayılır; üsttekinin açık kaldığı süre o ekranın
        // süresine eklenmemeli.
        setCurrentScreen(restored)
    }

    /** [f] ya da üstündeki bir fragment kaldırılıyor mu (iç içe fragment'lar kendileri kaldırılmıyor). */
    private fun isLeaving(f: Fragment): Boolean {
        var current: Fragment? = f
        while (current != null) {
            if (current.isRemoving) return true
            current = current.parentFragment
        }
        return false
    }

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
            screenStack.removeAll { it.name == name }
            screenStack.add(ScreenEntry(name, f))
            setCurrentScreen(name)
            AnalyticsLogger.logScreenView(name)
        }

        /**
         * Fragment KALDIRILIRKEN pause oldu: kapatıldı, yerine başkası geldi ya da geri
         * yığınına gitti. Görünümü henüz duruyor olabilir ama ekran artık o değil.
         *
         * ## Neden gerekli
         * Eskiden yalnızca [onFragmentViewDestroyed] dinleniyordu. Çıkış animasyonuyla
         * kaldırılan bir fragment'ın görünümü ise animasyon BİTİNCE yok ediliyor — ve
         * animasyon, kabı o sırada gizlenirse hiç bitmiyor. Görevler'den açılan ekranlarda
         * (günlük soru, abaküs pratiği) çıkışla kapanışta tam olarak bu oluyor: kap ~20 ms
         * sonra gizleniyor, görünüm aynı kaba bir sonraki ekran konana kadar yok edilmiyor.
         *
         * O sürede ekran takibi kapanmış ders ekranında takılı kalıyor, [StudyTimeTracker]
         * da çalışma süresi yazmaya devam ediyordu: çocuk Görevler ekranında dolaşırken günlük
         * hedef doluyordu (cihazda görüldü — günlük soru 18:43:40'ta kapandı, sayaç 18:43:44'e
         * kadar işledi).
         *
         * ## Neden yalnızca kaldırılanlar
         * Uygulama arka plana geçerken de fragment'lar pause olur ama ekran değişmemiştir.
         * Pause'da yığını koşulsuz boşaltsaydık arka plana geçişte üstteki ekranı düşürür ve
         * `app_exit_screen`'i alttaki ekrana yazardık. `isRemoving` ikisini ayırıyor.
         */
        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            if (isLeaving(f)) onScreenGone(f)
        }

        /**
         * Fragment görünümü yok edildi: kapatıldı ya da yerine başkası geldi. Üstteki ekran
         * gidiyorsa altındaki geri yüklenir.
         *
         * Çoğu durumda [onFragmentPaused] bu işi çoktan yapmış oluyor; burası, kaldırılmadan
         * görünümü yok edilen fragment'lar için duruyor.
         *
         * Uygulama gerçekten kapanırken sıralama `onPause → onStop → onDestroyView`; çıkış
         * olayı `onActivityStopped`'ta, yani yığın bozulmadan ÖNCE kaydedilir.
         */
        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            onScreenGone(f)
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
