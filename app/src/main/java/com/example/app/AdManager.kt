package com.example.app

import android.app.Activity
import android.content.Context
// AdMob imports - geçici olarak kaldırıldı
//import com.google.android.gms.ads.AdError
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.FullScreenContentCallback
//import com.google.android.gms.ads.LoadAdError
//import com.google.android.gms.ads.rewarded.RewardedAd
//import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {
    
    // AdMob geçici olarak kaldırıldı - Firebase çakışması nedeniyle
    
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        // Geçici olarak direkt ödülü ver
        onRewarded()
    }
    
    fun isAdReady(): Boolean {
        return true // Geçici olarak her zaman hazır
    }
    
    fun preloadAd() {
        // Geçici olarak boş
    }
} 