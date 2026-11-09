package com.example.app

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/**
 * DAVET ATIFI: haftalık kart bağlantısından (`/d/<kod>` → Play) gelen kurulumu davet edene bağlar.
 *
 * 1. İlk açılışta (cihaz başına bir kez) Play Install Referrer okunur. Sayfa Play bağlantısına
 *    `referrer=utm_source=ref&utm_medium=weekly&utm_campaign=<KOD>` koyuyor (public/d.html).
 *    Kod bulunursa cihazda bekletilir: o an çoğunlukla hesap yok (kayıt öğreticiden sonra).
 * 2. Oturum açıkken ([claimIfPending], MainActivity.onResume) sunucuya `claimReferral` gider;
 *    sunucu kodun sahibini, kendi kodunu, hesabın yeniliğini (7 gün) kendisi denetliyor.
 *    Kesin cevapta (kabul ya da ret) bekleyen kod silinir; ağ hatasında sonraki açılışta yeniden.
 *
 * GA4: `ref_install` (referrer'da kod bulundu), kullanıcı kapsamlı `ref_source`. Gerçek sayım sunucuda
 * (`referrals`), GA4 yalnızca huni için.
 */
object ReferralAttribution {

    private const val TAG = "ReferralAttribution"
    private const val PREFS = "referral_attribution"
    private const val KEY_CHECKED = "referrer_checked"
    private const val KEY_CODE = "pending_code"
    private const val KEY_SOURCE = "pending_source"

    data class Parsed(val code: String, val source: String)

    /** Davet ödülünü açan ders (sunucuda REWARD_MILESTONE_ID = "1_" + bu). */
    const val REWARD_LESSON_ID = "p1_i02_2_basamakli_sayilar"

    /** Yan etkisiz: referrer metninden kod. utm_source "ref" değilse (reklam, mağaza araması) null. */
    fun parse(raw: String?): Parsed? {
        if (raw.isNullOrBlank()) return null
        val q = runCatching { Uri.parse("x://r?$raw") }.getOrNull() ?: return null
        return parseParams(q.getQueryParameter("utm_source"), q.getQueryParameter("utm_medium"), q.getQueryParameter("utm_campaign"))
    }

    fun parseParams(source: String?, medium: String?, campaign: String?): Parsed? {
        if (source != "ref") return null
        val code = campaign?.trim()?.uppercase()?.takeIf { CODE_RE.matches(it) } ?: return null
        val src = medium?.takeIf { it in SOURCES } ?: "weekly"
        return Parsed(code, src)
    }

    private val CODE_RE = Regex("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}$")
    private val SOURCES = setOf("weekly", "invite", "badge", "challenge", "class")

    /**
     * Sahte hesapla Pro toplanmasın diye cihaz parmak izi (kullanıcı isteği 09.11.2026): ANDROID_ID'nin
     * tuzlu SHA-256'sı. Ham kimlik sunucuya gitmiyor; ANDROID_ID uygulama silinip yeniden kurulunca
     * değişmiyor (Android 8+, uygulama imzasına göre), SessionDeviceManager'ın UUID'si ise değişiyor.
     * Sunucu: bir telefon bir kez "davetle gelen" sayılır, davet edenin telefonu sayılmaz.
     */
    fun deviceFingerprint(context: Context): String? {
        @Suppress("HardwareIds")
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (id.isNullOrBlank()) return null
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest("sorobit-ref:$id".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun checkInstallReferrer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_CHECKED, false)) return
        val client = InstallReferrerClient.newBuilder(context).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(code: Int) {
                try {
                    when (code) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            val raw = client.installReferrer.installReferrer
                            val parsed = parse(raw)
                            Log.d(TAG, "referrer=$raw → $parsed")
                            val edit = prefs.edit().putBoolean(KEY_CHECKED, true)
                            if (parsed != null) edit.putString(KEY_CODE, parsed.code).putString(KEY_SOURCE, parsed.source)
                            edit.apply()
                            if (parsed != null) {
                                AnalyticsLogger.logSimpleEvent("ref_install")
                                AnalyticsLogger.setRefSource(parsed.source)
                            }
                        }
                        // Play Store yok / desteklemiyor: bir daha denemenin anlamı yok.
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                            prefs.edit().putBoolean(KEY_CHECKED, true).apply()
                        else -> Log.d(TAG, "referrer alınamadı ($code), sonraki açılışta yeniden")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "referrer okunamadı", e)
                } finally {
                    runCatching { client.endConnection() }
                }
            }

            override fun onInstallReferrerServiceDisconnected() = Unit
        })
    }

    /** Bekleyen kod varsa ve oturum açıksa sunucuya bildirir. */
    fun claimIfPending(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_CODE, null) ?: return
        if (FirebaseAuth.getInstance().currentUser == null) return
        val source = prefs.getString(KEY_SOURCE, "weekly") ?: "weekly"
        FirebaseFunctions.getInstance().getHttpsCallable("claimReferral")
            .call(mapOf("code" to code, "source" to source, "device" to deviceFingerprint(context)))
            .addOnSuccessListener { r ->
                // Kabul ya da kesin ret (kendi kodu, eski hesap…): tekrar denemek sonucu değiştirmez.
                Log.d(TAG, "claimReferral → ${r.getData()}")
                prefs.edit().remove(KEY_CODE).remove(KEY_SOURCE).apply()
            }
            .addOnFailureListener { e -> Log.w(TAG, "claimReferral başarısız, sonra yeniden", e) }
    }
}
