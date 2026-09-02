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

    // Gerçek ödüllü reklam birimi (SSV bu birim üzerinde yapılandırıldı).
    // Test etmek için AdMob konsolunda cihazınızı "Test devices" olarak işaretleyin —
    // aksi halde kendi reklamınıza tıklamak hesabın askıya alınmasına yol açabilir.
    private val adUnitId = "ca-app-pub-8436855856536384/7535644549"

    
    private var interstitialAd: InterstitialAd? = null
    // DİKKAT: Bu hâlâ Google'ın PAYLAŞIMLI TEST ID'si — henüz gerçek bir geçiş reklamı
    // (interstitial) birimi oluşturulmadı. Yayına çıkmadan önce AdMob konsolunda ayrı bir
    // interstitial birim açılıp buraya girilmeli.
    private val interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712"

    fun preloadInterstitialAd() {
        if (interstitialAd != null) {
            return // Already loaded
        }
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, interstitialAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "InterstitialAd failed to load: ${adError.message}")
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
                    
                    GlobalValues.interstitialAdShownCount++
                    
                    if (showAdSkipAfter && activity is androidx.fragment.app.FragmentActivity && GlobalValues.interstitialAdShownCount % 3 == 1) {
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
                    interstitialAd = null
                    preloadInterstitialAd() // Try to reload if it failed to show
                    onClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd showed fullscreen content.")
                    interstitialAd = null
                }
            }

            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
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
                    
                    if (showAdSkipAfter && activity is androidx.fragment.app.FragmentActivity) {
                        try {
                            AdSkipFragment().show(activity.supportFragmentManager, "AdSkip")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show: \${adError.message}")
                    rewardedAd = null
                    preloadAd() // Try to reload if it failed to show
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
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
            Toast.makeText(context, "Reklam henüz yüklenmedi, lütfen bekleyin.", Toast.LENGTH_SHORT).show()
            preloadAd() // Attempt to load it again
        }
    }
}