// Kaynak: AdManager.kt — yalnızca kural mantığı: geçiş reklamı sonrası "reklamı geç" ekranının
// sıklığı (showInterstitialAd), ödüllü reklamın SSV özel verisi ve oturum şartı (showRewardedAd),
// reklam birimi kimlikleri.
// Dışarıda bırakılan: Google Mobile Ads SDK çağrıları (yükleme/gösterme/geri çağrılar), Toast, Log.
// Farklar: `GlobalValues.interstitialAdShownCount` genel sayacı → [AdRules.InterstitialCounter];
// `UUID.randomUUID()` → nonce dışarıdan verilir.
// Android'deki hâliyle korunan notlar:
//  - Geçiş reklamı birimi hâlâ Google'ın PAYLAŞIMLI TEST kimliğidir (kaynakta "yayından önce
//    değiştirilmeli" notu var).
//  - Kimlikler Android reklam birimleridir; iOS için AdMob'da ayrı birimler açılmalıdır.
//  - Ödüllü reklamda "reklamı geç" ekranı HER kapanışta, geçiş reklamında ise 1., 4., 7.… kapanışta
//    gösterilir.
package com.sorobit.shared.oyun

object AdRules {
    /** Android ödüllü reklam birimi (SSV bu birim üzerinde yapılandırıldı). */
    const val ANDROID_REWARDED_AD_UNIT_ID = "ca-app-pub-8436855856536384/7535644549"

    /** Android geçiş reklamı birimi — Google'ın paylaşımlı TEST kimliği. */
    const val ANDROID_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    const val TAG_AD_SKIP = "AdSkip"
    const val TOAST_LOGIN_REQUIRED = "Odul icin giris yapmalisiniz."
    const val TOAST_AD_NOT_READY = "Reklam henüz yüklenmedi, lütfen bekleyin."

    /**
     * SSV `custom_data` değeri: "<uid>:<nonce>". AdMob bunu imzalı geri çağrıyla sunucuya iletir;
     * nonce daha sonra `openChest(adNonce = …)` / `claimAdEnergy(adNonce = …)` ile bozdurulur.
     */
    fun ssvCustomData(uid: String, nonce: String): String = "$uid:$nonce"

    /** Oturum yoksa ödül hiçbir hesaba yazılamayacağı için ödüllü reklam gösterilmez. */
    fun canShowRewardedAd(uid: String?): Boolean = uid != null

    /** Ödüllü reklam kapandıktan sonra "reklamı geç" ekranı gösterilsin mi. */
    fun shouldShowAdSkipAfterRewarded(showAdSkipAfter: Boolean, hostSupportsDialogs: Boolean): Boolean =
        showAdSkipAfter && hostSupportsDialogs

    /** Gösterilen geçiş reklamı sayacı (Android: `GlobalValues.interstitialAdShownCount`). */
    class InterstitialCounter(initialCount: Int = 0) {
        var interstitialAdShownCount: Int = initialCount
            private set

        /**
         * Geçiş reklamı kapatıldı: sayaç artar; her 3 reklamdan birincisinde (1., 4., 7.…)
         * "reklamı geç" ekranı gösterilir.
         * @return "reklamı geç" ekranı gösterilsin mi
         */
        fun onInterstitialDismissed(showAdSkipAfter: Boolean, hostSupportsDialogs: Boolean): Boolean {
            interstitialAdShownCount++
            return showAdSkipAfter && hostSupportsDialogs && interstitialAdShownCount % 3 == 1
        }
    }
}
