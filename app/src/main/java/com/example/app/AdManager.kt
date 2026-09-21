package com.example.app

import android.app.Activity
import android.content.Context
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
     * Ödüllü reklam birimi. SSV (sunucu tarafı doğrulama) GERÇEK birim üzerinde
     * yapılandırıldı; sandık ödülleri o doğrulamaya bağlı.
     */
    private val adUnitId =
        if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT else REAL_REWARDED_AD_UNIT

    private var interstitialAd: InterstitialAd? = null

    /** Ders dönüşlerinde gösterilen geçiş reklamı birimi. */
    private val interstitialAdUnitId =
        if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT else REAL_INTERSTITIAL_AD_UNIT

    fun preloadInterstitialAd() {
        if (interstitialAd != null) {
            return // Already loaded
        }
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, interstitialAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "InterstitialAd failed to load: ${adError.message}")
                AnalyticsLogger.logAdLoadFailed(AnalyticsLogger.AD_TYPE_INTERSTITIAL)
                interstitialAd = null
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "InterstitialAd was loaded.")
                interstitialAd = ad
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
        if (rewardedAd != null) {
            return // Already loaded
        }
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "Ad failed to load: \${adError.message}")
                AnalyticsLogger.logAdLoadFailed(AnalyticsLogger.AD_TYPE_REWARDED)
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
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
            Toast.makeText(context, "Odul icin giris yapmalisiniz.", Toast.LENGTH_SHORT).show()
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
                    Log.d(TAG, "Ad failed to show: \${adError.message}")
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
                Log.d(TAG, "User earned the reward: \${rewardItem.amount} \${rewardItem.type}")
                onRewarded(nonce)
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            // Çocuk canını/sandığını alamadı; ekranda "Reklam henüz yüklenmedi" yazısını görüyor.
            AnalyticsLogger.logAdNotReady(AnalyticsLogger.AD_TYPE_REWARDED)
            Toast.makeText(context, "Reklam henüz yüklenmedi, lütfen bekleyin.", Toast.LENGTH_SHORT).show()
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
         */
        private const val REAL_REWARDED_AD_UNIT = "ca-app-pub-8436855856536384/7535644549"
        private const val REAL_INTERSTITIAL_AD_UNIT = "ca-app-pub-8436855856536384/8539721523"

        /** Google'ın herkese açık örnek birimleri; tıklansa da bir yere yazılmıyor. */
        private const val TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"
    }

}