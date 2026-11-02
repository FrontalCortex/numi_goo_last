package com.example.app

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback


class AdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private val TAG = "AdManager"

    /**
     * Ödüllü reklam birimi — debug'da da GERÇEK birim. Geçiş reklamındaki gibi bir
     * debug/release ayrımı burada YAPILAMAZ.
     *
     * Sebebi SSV: ödül, AdMob'un sunucumuzu imzalı olarak çağırmasıyla doğrulanıyor
     * (`admobRewardCallback` → `adRewards/{nonce}`) ve bu geri arama adresi AdMob konsolunda
     * REKLAM BİRİMİ BAŞINA ayarlanıyor. Google'ın paylaşımlı test birimi bize ait olmadığı
     * için adresi oraya yazamıyoruz; test birimiyle reklam izlendiğinde geri arama hiç
     * gelmiyor, `adRewards` kaydı oluşmuyor ve ödül "alınamadı" diye reddediliyor.
     *
     * Geliştirme sırasındaki koruma bu yüzden AdMob konsolundaki "test cihazı" kaydı:
     * geliştirdiğin her cihazı oraya ekle, yoksa kendi reklamına tıklamak geçersiz trafik
     * sayılır.
     */
    private val adUnitId = REAL_REWARDED_AD_UNIT

    private var interstitialAd: InterstitialAd? = null

    /** Ders dönüşlerinde gösterilen geçiş reklamı birimi. */
    private val interstitialAdUnitId =
        if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT else REAL_INTERSTITIAL_AD_UNIT

    /**
     * Başarısız yüklemeyi arka planda yeniden dener — reklamı GÖSTERMEZ, yalnızca indirir.
     *
     * ## Neden gerekli
     * Açılıştaki yükleme başarısız olunca bir daha denenmiyordu; yeni deneme ancak çocuk
     * mağazadaki düğmeye basınca başlıyor, o an da "Reklam henüz yüklenmedi" görüyordu
     * (`ad_not_ready`). Arada deneyince düğmeye basıldığında reklam çoğu zaman hazır oluyor.
     *
     * ## Aralıklar ve sınır
     * 30 sn, 1, 2, 4 dk; sonra durur. Dolgu yokken sürekli istek atmak hem pil hem AdMob
     * açısından kötü. Zincir başarıyla ya da [release] ile biter; kullanıcı reklamı hazır
     * değilken isterse bekleyen deneme beklenmeden hemen yüklenir. Hatalı istek gibi
     * kendiliğinden düzelmeyecek hatalarda denenmez.
     *
     * ## Neden yalnızca ilk hata olaya yazılıyor
     * Her denemenin hatası `ad_load_failed_*` olarak sayılsaydı, ön yüklemenin şişirdiği oranı
     * bu kez yeniden denemeler şişirirdi. Olay "bu yükleme zinciri başarısız başladı" demek.
     */
    private inner class LoadRetry(private val adType: String, private val load: () -> Unit) {
        var isLoading = false
        private var attempt = 0
        private val retryRunnable = Runnable { load() }

        /** Yükleme başlamadan çağrılır; bekleyen bir deneme varsa yerini alır. */
        fun onLoadStarted() {
            handler.removeCallbacks(retryRunnable)
            isLoading = true
        }

        fun onLoaded() {
            isLoading = false
            attempt = 0
        }

        fun onFailed(errorCode: Int) {
            isLoading = false
            if (attempt == 0) AnalyticsLogger.logAdLoadFailed(adType, errorCode)
            if (!isRetryable(errorCode) || attempt >= RETRY_DELAYS_MS.size) {
                attempt = 0
                return
            }
            handler.postDelayed(retryRunnable, RETRY_DELAYS_MS[attempt])
            attempt++
        }

        fun cancel() {
            handler.removeCallbacks(retryRunnable)
            attempt = 0
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val interstitialRetry = LoadRetry(AnalyticsLogger.AD_TYPE_INTERSTITIAL) { preloadInterstitialAd() }
    private val rewardedRetry = LoadRetry(AnalyticsLogger.AD_TYPE_REWARDED) { preloadAd() }

    /** Activity kapanırken bekleyen yeniden denemeleri iptal eder. */
    fun release() {
        interstitialRetry.cancel()
        rewardedRetry.cancel()
    }

    fun preloadInterstitialAd() {
        if (interstitialAd != null || interstitialRetry.isLoading) {
            return // Already loaded / loading
        }
        interstitialRetry.onLoadStarted()
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, interstitialAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "InterstitialAd failed to load: ${adError.code} ${adError.message}")
                interstitialAd = null
                interstitialRetry.onFailed(adError.code)
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "InterstitialAd was loaded.")
                interstitialAd = ad
                interstitialRetry.onLoaded()
            }
        })
    }

    fun showInterstitialAd(activity: Activity, showAdSkipAfter: Boolean = true, onClosed: () -> Unit = {}) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd was dismissed.")
                    interstitialAd = null
                    preloadInterstitialAd() // Preload the next ad
                    
                    // Panelin sıklığı [AdSkipPolicy]'de; sayaç diskte tutuluyor ki uygulama
                    // kapatılıp açıldığında sıfırlanmasın.
                    if (activity is androidx.fragment.app.FragmentActivity &&
                        AdSkipPolicy.onAdClosed(activity, eligible = showAdSkipAfter)
                    ) {
                        try {
                            AdSkipFragment().show(activity.supportFragmentManager, "AdSkip")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "InterstitialAd failed to show: ${adError.message}")
                    AnalyticsLogger.logAdShowFailed(AnalyticsLogger.AD_TYPE_INTERSTITIAL)
                    interstitialAd = null
                    preloadInterstitialAd() // Try to reload if it failed to show
                    onClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd showed fullscreen content.")
                    AnalyticsLogger.logAdShown(AnalyticsLogger.AD_TYPE_INTERSTITIAL)
                    interstitialAd = null
                }
            }

            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            AnalyticsLogger.logAdNotReady(AnalyticsLogger.AD_TYPE_INTERSTITIAL)
            preloadInterstitialAd() // Attempt to load it again
            onClosed()
        }
    }

    fun preloadAd() {
        if (rewardedAd != null || rewardedRetry.isLoading) {
            return // Already loaded / loading
        }
        rewardedRetry.onLoadStarted()
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "Ad failed to load: ${adError.code} ${adError.message}")
                rewardedAd = null
                rewardedRetry.onFailed(adError.code)
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
                rewardedRetry.onLoaded()
            }
        })
    }

    fun isAdReady(): Boolean {
        return rewardedAd != null
    }

    /**
     * Odullu reklam gosterir.
     *
     * Reklam tamamlandiginda AdMob sunucumuzu imzali olarak cagirir (SSV) ve tek kullanimlik
     * bir sandik hakki yazilir. [onRewarded] o hakki bozdurmak icin gereken nonce ile cagrilir;
     * nonce sunucuya openChest(adNonce = ...) olarak iletilmelidir.
     *
     * Oturum yoksa odul hicbir hesaba yazilamayacagi icin reklam gosterilmez.
     */
    fun showRewardedAd(
        activity: Activity,
        showAdSkipAfter: Boolean = true,
        onRewarded: (adNonce: String) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "Oturum acik degil; odullu reklam gosterilmiyor.")
            Toast.makeText(context, appString(R.string.ui_odul_icin_giris_yapmalisiniz), Toast.LENGTH_SHORT).show()
            return
        }
        // custom_data = "<uid>:<nonce>" - AdMob bunu imzali callback ile geri gonderir.
        val nonce = UUID.randomUUID().toString()
        rewardedAd?.setServerSideVerificationOptions(
            ServerSideVerificationOptions.Builder()
                .setCustomData(uid + ":" + nonce)
                .build()
        )

        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    rewardedAd = null
                    preloadAd() // Preload the next ad
                    
                    // Ödüllü reklam da aynı sayaca giriyor: kullanıcı açısından ikisi de
                    // "izlediğim reklam". Burada hiç sayaç yoktu, yani showAdSkipAfter true
                    // verildiğinde panel her ödüllü reklamdan sonra çıkardı.
                    if (activity is androidx.fragment.app.FragmentActivity &&
                        AdSkipPolicy.onAdClosed(activity, eligible = showAdSkipAfter)
                    ) {
                        try {
                            AdSkipFragment().show(activity.supportFragmentManager, "AdSkip")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show: ${adError.code} ${adError.message}")
                    AnalyticsLogger.logAdShowFailed(AnalyticsLogger.AD_TYPE_REWARDED)
                    rewardedAd = null
                    preloadAd() // Try to reload if it failed to show
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    AnalyticsLogger.logAdShown(AnalyticsLogger.AD_TYPE_REWARDED)
                    // Ad is showing, we can nullify the current reference
                    rewardedAd = null
                }
            }

            rewardedAd?.show(activity) { rewardItem ->
                // Reward the user
                Log.d(TAG, "User earned the reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewarded(nonce)
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            // Çocuk canını/sandığını alamadı; ekranda "Reklam henüz yüklenmedi" yazısını görüyor.
            AnalyticsLogger.logAdNotReady(AnalyticsLogger.AD_TYPE_REWARDED)
            Toast.makeText(context, appString(R.string.ui_reklam_henuz_yuklenmedi_lutfen_bekleyin), Toast.LENGTH_SHORT).show()
            preloadAd() // Attempt to load it again
        }
    }

    companion object {
        /**
         * GERÇEK reklam birimleri yalnızca release derlemelerinde kullanılıyor.
         *
         * ## Neden ayrım kodda
         * Geçiş reklamı aylarca Google'ın paylaşımlı test kimliğiyle kaldı; o hâliyle yayına
         * çıkılsaydı reklamlar görünür, sayılır ama hiç gelir getirmezdi ve AdMob panelinde
         * de bir şey görünmediği için fark etmek haftalar alırdı.
         *
         * Ters yön de önemli: geliştirme sırasında gerçek birime dokunmak istemiyoruz. Kendi
         * reklamına tıklamak "geçersiz trafik" sayılıyor ve tekrarlarsa hesap askıya
         * alınabiliyor. Cihazı AdMob'da "test cihazı" olarak işaretlemek de koruyor ama o
         * koruma cihaz başına; yeni bir telefonda derleme aldığında yoktur. Ayrım kodda
         * olunca hangi cihazda çalıştığından bağımsız.
         *
         * Bu ayrım YALNIZCA geçiş reklamında mümkün; ödüllü reklamın neden dışarıda kaldığı
         * [adUnitId]'de yazıyor.
         */
        private const val REAL_REWARDED_AD_UNIT = "ca-app-pub-8436855856536384/7535644549"
        private const val REAL_INTERSTITIAL_AD_UNIT = "ca-app-pub-8436855856536384/8539721523"

        /**
         * Google'ın herkese açık örnek birimi; tıklansa da bir yere yazılmıyor.
         *
         * Yalnızca geçiş reklamı için var. Ödüllü reklamda kullanılamaz — sebebi [adUnitId].
         */
        private const val TEST_INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"

        /** Başarısız yüklemeden sonraki yeniden deneme beklemeleri; bkz. [LoadRetry]. */
        private val RETRY_DELAYS_MS = longArrayOf(30_000L, 60_000L, 120_000L, 240_000L)

        /** Dolgu yokluğu ve ağ hataları zamanla düzelebilir; hatalı istek ve kimlik hataları düzelmez. */
        private fun isRetryable(errorCode: Int): Boolean = when (errorCode) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR,
            AdRequest.ERROR_CODE_NETWORK_ERROR,
            AdRequest.ERROR_CODE_NO_FILL,
            AdRequest.ERROR_CODE_MEDIATION_NO_FILL -> true
            else -> false
        }
    }

}