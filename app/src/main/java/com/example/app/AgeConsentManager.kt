package com.example.app

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

object AgeConsentManager {

    private const val TAG = "AgeConsentManager"

    /**
     * Reklam muamelesi seviyeleri:
     * - CHILD   : Rıza yaşının altında → TFCD=true + TFUA=true (kişiselleştirilmiş reklam yok, tam kısıtlama)
     * - TEEN    : Yaşı biliniyor, rıza yaşının üzerinde ama yetişkin sayılmıyor → sadece TFUA=true (TFCD=false)
     * - UNSPECIFIED : Yaş bilgisi belirsiz kullanıcı — plan gereği bu durum CHILD gibi muamele görür
     */
    enum class AgeTreatment {
        CHILD,
        TEEN,
        UNSPECIFIED
    }

    /**
     * Firestore'dan kullanıcının birthYear'ını asenkron olarak çeker.
     * Ülkeye özgü dijital rıza yaşı sınırına göre muamele seviyesini belirler.
     * Ardından MobileAds SDK'yı doğru TFCD/TFUA konfigürasyonuyla başlatır.
     * MobileAds tamamen başladıktan sonra onComplete callback'i çağrılır;
     * ad preload işlemleri bu callback içinde yapılmalıdır.
     */
    fun initializeAdMobWithAgeConsent(context: Context, onComplete: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // Kullanıcı giriş yapmamışsa güvenli varsayılan: CHILD
            Log.d(TAG, "No logged in user → defaulting to CHILD treatment.")
            applyTreatmentAndInit(context, AgeTreatment.CHILD, onComplete)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                val birthYear = if (doc.exists() && doc.contains("birthYear")) {
                    doc.getLong("birthYear")?.toInt() ?: -1
                } else -1

                if (birthYear > 0) {
                    val age = Calendar.getInstance().get(Calendar.YEAR) - birthYear
                    val countryCode = getCountryCode(context)
                    val consentAge = getDigitalConsentAgeForCountry(countryCode)

                    Log.d(TAG, "User Age=$age, Country=$countryCode, Consent Age=$consentAge")

                    val treatment = if (age < consentAge) AgeTreatment.CHILD else AgeTreatment.TEEN
                    applyTreatmentAndInit(context, treatment, onComplete)
                } else {
                    // birthYear yok veya geçersiz → plan gereği CHILD
                    Log.d(TAG, "birthYear not found or invalid → defaulting to CHILD treatment.")
                    applyTreatmentAndInit(context, AgeTreatment.CHILD, onComplete)
                }
            }
            .addOnFailureListener { e ->
                // Firestore hatası → güvenli liman: CHILD
                Log.e(TAG, "Firestore read failed: ${e.message} → defaulting to CHILD.", e)
                applyTreatmentAndInit(context, AgeTreatment.CHILD, onComplete)
            }
    }

    private fun applyTreatmentAndInit(
        context: Context,
        treatment: AgeTreatment,
        onComplete: () -> Unit
    ) {
        Log.d(TAG, "Applying AdMob treatment: $treatment")

        val builder = RequestConfiguration.Builder()

        // Google Mobile Ads resmi dokümantasyonuna göre (TFCD ve TFUA kullanımı):
        //
        // CHILD / UNSPECIFIED → TFCD=true  + TFUA=true  : Tüm kişiselleştirme kapalı, tam koruma
        // TEEN                → TFCD=false + TFUA=true  : COPPA dışında (çocuk değil) ama rıza yaşının altında
        when (treatment) {
            AgeTreatment.CHILD, AgeTreatment.UNSPECIFIED -> {
                builder.setTagForChildDirectedTreatment(
                    RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                )
                builder.setTagForUnderAgeOfConsent(
                    RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                )
            }
            AgeTreatment.TEEN -> {
                // Rıza yaşını geçmiş, yetişkin → yalnızca TFUA false yeterli
                // (TFCD zaten false, kişiselleştirilmiş reklam gösterilebilir)
                builder.setTagForChildDirectedTreatment(
                    RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                )
                builder.setTagForUnderAgeOfConsent(
                    RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
                )
            }
        }

        MobileAds.setRequestConfiguration(builder.build())

        // SDK init, Firestore verisi alındıktan SONRA çağrılır.
        MobileAds.initialize(context) {
            Log.d(TAG, "MobileAds initialized with treatment=$treatment")
            onComplete()
        }
    }

    /**
     * Cihazın ülke kodunu belirler.
     * Öncelik: SIM ülkesi → Ağ ülkesi → Sistem Locale fallback
     */
    private fun getCountryCode(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val simCountry = tm.simCountryIso
            if (!simCountry.isNullOrBlank()) return simCountry.uppercase(Locale.ROOT)
            val networkCountry = tm.networkCountryIso
            if (!networkCountry.isNullOrBlank()) return networkCountry.uppercase(Locale.ROOT)
        } catch (e: Exception) {
            Log.e(TAG, "TelephonyManager error, falling back to Locale", e)
        }
        return Locale.getDefault().country.uppercase(Locale.ROOT).ifBlank { "US" }
    }

    /**
     * GDPR / COPPA gereği ülkelere göre dijital rıza yaşı (Digital Consent Age).
     * Kaynak: https://ec.europa.eu/info/law/law-topic/data-protection/reform/rules-business-and-organisations/legal-grounds-processing-data/consent/what-age-consent-online-services
     */
    private fun getDigitalConsentAgeForCountry(countryCode: String): Int {
        return when (countryCode) {
            // 16 yaş sınırı olan ülkeler (GDPR varsayılanı)
            "DE", "IE", "NL", "RO", "HR", "LU", "SK", "HU", "PL" -> 16
            // 15 yaş sınırı olan ülkeler
            "FR", "GR", "CZ", "SI" -> 15
            // 14 yaş sınırı olan ülkeler
            "AT", "BG", "CY", "ES", "IT", "LT" -> 14
            // Geri kalan EEA ülkeleri (Belçika, Danimarka, İsveç vb.), ABD (COPPA) ve diğer ülkeler (13)
            else -> 13
        }
    }
}
