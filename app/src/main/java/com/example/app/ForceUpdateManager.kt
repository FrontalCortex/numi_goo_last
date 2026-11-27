package com.example.app

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.lang.ref.WeakReference

/**
 * Zorunlu güncelleme: eski bir sürümü uzaktan kullanılamaz hale getirir.
 *
 * Play Console'da kurulu sürümü durdurmanın yolu yok; otomatik güncellemesi kapalı bir
 * kullanıcı eski sürümde aylarca kalabilir. Bu yüzden karar uygulamanın içinde veriliyor:
 * Remote Config'teki [KEY_MIN_VERSION_CODE] değeri bu derlemenin `versionCode`'undan
 * büyükse uygulama kapatılamayan bir "Güncelleme gerekli" penceresi gösterir.
 *
 * ## Console'da kullanımı
 * Firebase Console → Remote Config → `min_version_code` parametresi (sayı). Örn. 14 yapılırsa
 * versionCode'u 13 ve altı olan her kurulum kilitlenir. Parametre hiç yoksa varsayılan 0'dır,
 * yani kimse kilitlenmez.
 *
 * DİKKAT: Değeri yalnızca o sürüm Play'de %100 yayına çıktıktan SONRA yükselt. Kademeli
 * yayında (ör. %20) sürümü alamayan kullanıcılar güncelleyemeyeceği bir pencereye kilitlenir.
 * Henüz yayınlanmamış bir versionCode yazmak herkesi kilitler.
 *
 * ## Yalnızca bu sınıfı içeren sürümden itibaren çalışır
 * Bundan önceki sürümler bu kontrolü bilmiyor; onları uzaktan durdurmanın yolu yok.
 *
 * ## Güncelleme düğmesi
 * Önce Play'in uygulama içi güncellemesi (IMMEDIATE, tam ekran) denenir. Play yeni sürümü
 * henüz görmüyorsa (yayından sonraki ilk saatlerde olabiliyor) ya da uygulama Play dışından
 * kurulduysa mağaza sayfası açılır. Kullanıcı güncellemeden dönerse pencere yerinde kalır.
 */
object ForceUpdateManager {
    private const val TAG = "ForceUpdate"
    private const val KEY_MIN_VERSION_CODE = "min_version_code"
    private const val REQUEST_CODE_IMMEDIATE_UPDATE = 7311

    // Remote Config varsayılanı 12 saat; acil bir kilidin o kadar geç yayılması işe yaramaz.
    // Debug'da her açılışta çekilir ki Console'daki değişiklik hemen denenebilsin.
    private val FETCH_INTERVAL_SECONDS = if (BuildConfig.DEBUG) 0L else 15 * 60L

    private var fetchTask: Task<Boolean>? = null
    private var shownDialog: WeakReference<AlertDialog>? = null

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
                    .build()
            )
            setDefaultsAsync(mapOf(KEY_MIN_VERSION_CODE to 0L))
        }
    }

    /**
     * Remote Config'i arka planda çekmeye başlar; zaten sürüyorsa aynısını döner.
     * Aralık dolmadıysa SDK ağa çıkmadan önbelleği etkinleştirir.
     */
    fun startFetch(): Task<Boolean> {
        fetchTask?.let { if (!it.isComplete) return it }
        return remoteConfig.fetchAndActivate().also { fetchTask = it }
    }

    /** Son etkinleştirilen değere göre bu sürüm kilitli mi. Ağ beklemez. */
    fun isBlocked(): Boolean {
        val minVersionCode = remoteConfig.getLong(KEY_MIN_VERSION_CODE)
        return minVersionCode > BuildConfig.VERSION_CODE
    }

    /**
     * Açılış ekranı için: çekme bitene ya da [maxWaitMs] dolana kadar bekler, sonra ya
     * [onAllowed]'ı çalıştırır ya da kilit penceresini gösterir. Süre dolarsa (ağ yavaş /
     * yok) bir önceki açılışta etkinleşen değerle karar verilir; açılış uzamaz.
     */
    fun runIfAllowed(activity: AppCompatActivity, maxWaitMs: Long, onAllowed: () -> Unit) {
        val task = startFetch()
        val handler = Handler(Looper.getMainLooper())
        var decided = false
        val decide = Runnable {
            if (decided) return@Runnable
            decided = true
            if (activity.isFinishing || activity.isDestroyed) return@Runnable
            if (isBlocked()) {
                Log.w(TAG, "Sürüm ${BuildConfig.VERSION_CODE} kilitli, açılış durduruldu")
                showBlockingDialog(activity)
            } else {
                onAllowed()
            }
        }
        if (task.isComplete) {
            decide.run()
            return
        }
        handler.postDelayed(decide, maxWaitMs)
        task.addOnCompleteListener(activity) {
            handler.removeCallbacks(decide)
            decide.run()
        }
    }

    /**
     * Uygulama öne geldiğinde (MainActivity.onResume): açılış ekranından geçmeden arka
     * plandan dönen kullanıcı da kilitlensin. Hiçbir şeyi bekletmez.
     */
    fun checkOnResume(activity: AppCompatActivity) {
        if (isBlocked()) {
            showBlockingDialog(activity)
            return
        }
        startFetch().addOnSuccessListener(activity) {
            if (isBlocked()) showBlockingDialog(activity)
        }
    }

    private fun showBlockingDialog(activity: AppCompatActivity) {
        if (activity.isFinishing || activity.isDestroyed) return
        shownDialog?.get()?.let { if (it.isShowing && it.ownerActivity === activity) return }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.ui_guncelleme_gerekli)
            .setMessage(R.string.ui_bu_surum_artik_kullanilamiyor)
            .setCancelable(false)
            // Dinleyici setOnShowListener'da bağlanıyor: buradaki varsayılan davranış
            // düğmeye basınca pencereyi kapatırdı.
            .setPositiveButton(R.string.ui_guncelle, null)
            .setNegativeButton(R.string.ui_cik) { _, _ -> activity.finishAffinity() }
            .create()
        dialog.setOwnerActivity(activity)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                startUpdate(activity)
            }
        }
        dialog.show()
        shownDialog = WeakReference(dialog)
    }

    private fun startUpdate(activity: AppCompatActivity) {
        val manager = AppUpdateManagerFactory.create(activity)
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                val availability = info.updateAvailability()
                val canUpdateInApp =
                    (availability == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) ||
                        // Önceki bir denemede başlamış ve yarım kalmış güncelleme.
                        availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                if (!canUpdateInApp || activity.isFinishing) {
                    openPlayStore(activity)
                    return@addOnSuccessListener
                }
                val started = runCatching {
                    manager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        REQUEST_CODE_IMMEDIATE_UPDATE,
                    )
                }.getOrDefault(false)
                if (!started) openPlayStore(activity)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "appUpdateInfo alınamadı, mağaza açılıyor", e)
                openPlayStore(activity)
            }
    }

    private fun openPlayStore(activity: AppCompatActivity) {
        val packageName = activity.packageName
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            )
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                )
            )
        }
    }
}
