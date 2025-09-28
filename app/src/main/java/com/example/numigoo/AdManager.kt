package com.example.numigoo

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {
    
    companion object {
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Test ID - Gerçek ID ile değiştirin
    }
    
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var onAdRewarded: (() -> Unit)? = null
    
    init {
        loadRewardedAd()
    }
    
    private fun loadRewardedAd() {
        if (isLoading) return
        
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
                
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }
            }
        )
    }
    
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        onAdRewarded = onRewarded
        
        if (rewardedAd == null) {
            // Reklam yüklenmemişse, yükle ve tekrar dene
            loadRewardedAd()
            return
        }
        
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd() // Yeni reklam yükle
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd()
            }
            
            override fun onAdShowedFullScreenContent() {
                // Reklam gösterildi
            }
        }
        
        rewardedAd?.show(activity) { 
            // Kullanıcı reklamı tamamladı, ödülü ver
            onAdRewarded?.invoke()
        }
    }
    
    fun isAdReady(): Boolean {
        return rewardedAd != null
    }
    
    fun preloadAd() {
        if (rewardedAd == null && !isLoading) {
            loadRewardedAd()
        }
    }
} 