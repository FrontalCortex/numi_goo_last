// Kaynak: AdManager.kt — yalnızca kural mantığı: geçiş reklamı sonrası "reklamı geç" ekranının
// sıklığı (showInterstitialAd), ödüllü reklamın SSV özel verisi ve oturum şartı (showRewardedAd),
// reklam birimi kimlikleri.
// Dışarıda bırakılan: Google Mobile Ads SDK çağrıları (yükleme/gösterme/geri çağrılar), Toast, Log.
// Farklar: `GlobalValues.interstitialAdShownCount` genel sayacı → [AdRules.InterstitialCounter];
// `UUID.randomUUID()` → nonce dışarıdan verilir.
// Android'deki hâliyle korunan notlar:
//  - (ESKİ not kalktı) Android güncel: geçiş reklamı release'te GERÇEK birimdir, debug'da Google'ın test birimi
//    ([interstitialAdUnitId]); ödüllü reklam debug'da da gerçek birimdir (SSV geri arama adresi birim başına).
//  - Kimlikler Android reklam birimleridir; iOS için AdMob'da ayrı birimler açılmalıdır.
//  - Android güncel: "reklamı geç" ekranının sıklığı [AdSkipPolicy]'dedir (üç kapı: panelden beri 4 reklam, iki panel
//    arası 2 saat, günde en çok 2 panel; sayaç kalıcı). Ödüllü ve geçiş reklamı aynı sayaca girer. Eski
//    [InterstitialCounter] (1., 4., 7. kapanış) yalnızca eski davranışın belgesi/testi olarak durur.
package com.sorobit.shared.oyun

object AdRules {
    /** Android ödüllü reklam birimi (SSV bu birim üzerinde yapılandırıldı). */
    const val ANDROID_REWARDED_AD_UNIT_ID = "ca-app-pub-8436855856536384/7535644549"

    /** Android geçiş reklamı birimi — GERÇEK birim (yalnızca release derlemelerinde kullanılır). */
    const val ANDROID_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-8436855856536384/8539721523"

    /** Google'ın herkese açık örnek geçiş reklamı birimi; yalnızca debug derlemelerinde (ödüllüde kullanılamaz). */
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /** Android `interstitialAdUnitId`: debug'da test, release'te gerçek birim. */
    fun interstitialAdUnitId(isDebug: Boolean): String =
        if (isDebug) TEST_INTERSTITIAL_AD_UNIT_ID else ANDROID_INTERSTITIAL_AD_UNIT_ID

    /** Başarısız yüklemeden sonraki arka plan yeniden deneme beklemeleri (Android `RETRY_DELAYS_MS`): 30 sn, 1, 2, 4 dk. */
    val RETRY_DELAYS_MS: List<Long> = listOf(30_000L, 60_000L, 120_000L, 240_000L)

    // AdMob hata kodları (Android `AdRequest.ERROR_CODE_*`)
    const val ERROR_CODE_INTERNAL_ERROR = 0
    const val ERROR_CODE_INVALID_REQUEST = 1
    const val ERROR_CODE_NETWORK_ERROR = 2
    const val ERROR_CODE_NO_FILL = 3
    const val ERROR_CODE_MEDIATION_NO_FILL = 9

    /** Dolgu yokluğu ve ağ hataları zamanla düzelebilir; hatalı istek ve kimlik hataları düzelmez. */
    fun isRetryable(errorCode: Int): Boolean = when (errorCode) {
        ERROR_CODE_INTERNAL_ERROR, ERROR_CODE_NETWORK_ERROR, ERROR_CODE_NO_FILL, ERROR_CODE_MEDIATION_NO_FILL -> true
        else -> false
    }

    /**
     * Android `LoadRetry.onFailed`: [attempt] (0 tabanlı, başarısız denemeden ÖNCEKİ sayaç) için bir sonraki deneme
     * gecikmesi; yeniden denenmeyecekse null (kendiliğinden düzelmeyen hata ya da bütçe doldu).
     */
    fun nextRetryDelayMs(errorCode: Int, attempt: Int): Long? {
        if (!isRetryable(errorCode) || attempt >= RETRY_DELAYS_MS.size) return null
        return RETRY_DELAYS_MS[attempt]
    }

    /** Yalnızca zincirdeki İLK hata olaya yazılır (Android `if (attempt == 0) logAdLoadFailed`). */
    fun shouldLogLoadFailure(attempt: Int): Boolean = attempt == 0

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

    /** ESKİ (Android'de kaldırıldı): gösterilen geçiş reklamı sayacı (`GlobalValues.interstitialAdShownCount`). */
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

    /**
     * Android `AdSkipPolicy`: "reklamı geç" panelinin (Pro satış ekranı) ne sıklıkta çıkacağına karar verir.
     *
     * Sayaç "kaçıncı reklam" değil "panelden beri kaç reklam"dır ve DİSKTE tutulur (uygulama kapanıp açılınca
     * sıfırlanmaz). Üç kapı birden açık olmalı: panelden beri en az [ADS_PER_PANEL] reklam, son panelden en az
     * [MIN_GAP_MS], o gün en çok [MAX_PANELS_PER_DAY] panel. Saat geri alınırsa (negatif süre) kapı kilitlenmesin
     * diye süre dolmuş sayılır. Ayarlar okunamazsa Android panel göstermez; burada depo her zaman okunur.
     *
     * @param store Android: "ad_skip_policy" adlı SharedPreferences.
     * @param debugEveryAd Android `AdSkipDebug.everyAd` (release'de kapalı): uygun her reklamdan sonra panel.
     */
    class AdSkipPolicy(
        private val store: OyunKeyValueStore,
        private val nowMs: () -> Long = ::oyunSimdiMs,
        private val timeZone: kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.currentSystemDefault(),
        private val debugEveryAd: Boolean = false,
    ) {
        companion object {
            const val PREFS_NAME = "ad_skip_policy"
            const val KEY_ADS_SINCE_PANEL = "ads_since_panel"
            const val KEY_LAST_PANEL_MS = "last_panel_ms"
            const val KEY_DAY_ID = "panel_day_id"
            const val KEY_DAY_COUNT = "panel_day_count"

            /** İlk üç reklamda panel yok; yeni kullanıcı uygulamayı tanımadan satış ekranıyla karşılaşmaz. */
            const val ADS_PER_PANEL = 4

            /** İki panel arasındaki en kısa süre (2 saat). */
            const val MIN_GAP_MS = 2L * 60 * 60 * 1000

            /** Bir günde en fazla kaç panel. */
            const val MAX_PANELS_PER_DAY = 2
        }

        /**
         * Bir reklam kapandı. Panelin şimdi çıkması gerekiyorsa true döner ve sayacı sıfırlar.
         *
         * @param eligible bu reklamdan sonra panel çıkabilir mi (çağıran akışın kararı); false ise panel çıkmaz ama
         *   reklam yine sayılır.
         */
        fun onAdClosed(eligible: Boolean): Boolean {
            if (eligible && debugEveryAd) {
                markShown()
                return true
            }
            val since = store.getInt(KEY_ADS_SINCE_PANEL, 0) + 1
            // Sıra gelmediyse ya da süre/gün kapısı kapalıysa sayaç EŞİKTE bekletilir, sıfırlanmaz.
            return if (!eligible || since < ADS_PER_PANEL || !withinBudget()) {
                store.putInt(KEY_ADS_SINCE_PANEL, since)
                false
            } else {
                markShown()
                true
            }
        }

        /** Android `resetForDebug`. */
        fun resetForDebug() = store.clear()

        private fun withinBudget(): Boolean {
            if (panelsToday() >= MAX_PANELS_PER_DAY) return false
            val last = store.getLong(KEY_LAST_PANEL_MS, 0L)
            if (last <= 0L) return true
            val elapsed = nowMs() - last
            // Negatif: cihaz saati geri alınmış. Kilitli kalmasın diye süre dolmuş sayılır.
            return elapsed < 0L || elapsed >= MIN_GAP_MS
        }

        /** Bugün kaç panel gösterildi. Kayıtlı gün bugün değilse sayaç sıfırdan başlar. */
        private fun panelsToday(): Int =
            if (store.getString(KEY_DAY_ID) == todayId()) store.getInt(KEY_DAY_COUNT, 0) else 0

        private fun markShown() {
            val count = panelsToday() + 1
            store.putInt(KEY_ADS_SINCE_PANEL, 0)
            store.putLong(KEY_LAST_PANEL_MS, nowMs())
            store.putString(KEY_DAY_ID, todayId())
            store.putInt(KEY_DAY_COUNT, count)
        }

        /** Android: `SimpleDateFormat("yyyy-MM-dd", Locale.US)` (yerel saat dilimi). */
        private fun todayId(): String = yerelTarih(nowMs(), timeZone).toString()
    }
}
