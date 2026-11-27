package com.example.app.auth

import java.util.Calendar
import java.util.TimeZone

/**
 * 13 yaş altı için veli onayı gerekip gerekmediğini söyler (COPPA / GDPR / KVKK).
 *
 * Elimizde yalnızca doğum yılı var. "Bu yıl − doğum yılı ≤ 13" olan çocuk yılın bir
 * kısmında hâlâ 12 yaşında olabilir; temkinli tarafta kalıp onu da onaya sokuyoruz.
 *
 * Aynı formül sunucuda (`requiresParentalConsent`, functions/index.js) ve kurallarda
 * (`noUnconsentedChild`, firestore.rules) var; sunucu ve kurallar UTC yılı kullandığı için
 * burada da UTC. Yerel yıl kullansaydık yılbaşı gecesi birkaç saat boyunca uygulama "onay
 * gerekmez" deyip istemci yazımına giderdi, kurallar da onu reddederdi.
 * Biri değişirse üçü birlikte değişmeli.
 */
object ParentalConsentPolicy {
    private const val MAX_AGE_DIFF_NEEDING_CONSENT = 13

    fun requiresParentalConsent(birthYear: Int?): Boolean {
        if (birthYear == null || birthYear <= 0) return false
        val utcYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR)
        return utcYear - birthYear <= MAX_AGE_DIFF_NEEDING_CONSENT
    }
}
