// Kaynak: AgeConsentManager.kt — yaş/rıza kuralı: `AgeTreatment`, doğum yılından muamele seçimi
// (initializeAdMobWithAgeConsent), TFCD/TFUA bayrakları (applyTreatmentAndInit), UMP "rıza yaşının
// altında" bayrağı (requestConsentThenInitAds), ülke kodu önceliği (getCountryCode), ülkelere göre
// dijital rıza yaşı (getDigitalConsentAgeForCountry).
// Dışarıda bırakılan: MobileAds / UMP SDK çağrıları, Firestore okuması, TelephonyManager.
// Farklar: `Calendar.getInstance().get(Calendar.YEAR)` → `currentYear` parametresi;
// `uppercase(Locale.ROOT)` → `uppercase()`; Log çağrıları kaldırıldı.
// Android'deki hâliyle korunan notlar:
//  - Rıza yaşını geçmiş HERKES (yetişkinler dahil) TEEN sayılır; UNSPECIFIED hiçbir yolda üretilmez.
//  - TEEN için kod TFUA=false ayarlar; kaynak dosyadaki enum yorumu ise "sadece TFUA=true" der
//    (yorum ile kod çelişiyor — kodun davranışı taşındı).
//  - Ülke kodu büyük harfe çevrilmiş SIM/ağ/yerel ayar değeridir; tablo yalnızca listedeki kodları
//    tanır, geri kalan her şey (boş/bilinmeyen dahil) 13 kabul edilir.
package com.sorobit.shared.oyun

object AgeConsentManager {

    /**
     * Reklam muamelesi seviyeleri:
     * - CHILD   : Rıza yaşının altında → TFCD=true + TFUA=true (kişiselleştirilmiş reklam yok, tam kısıtlama)
     * - TEEN    : Yaşı biliniyor ve rıza yaşının üzerinde → TFCD=false + TFUA=false
     * - UNSPECIFIED : Yaş bilgisi belirsiz kullanıcı — CHILD gibi muamele görür
     */
    enum class AgeTreatment {
        CHILD,
        TEEN,
        UNSPECIFIED,
    }

    /** Reklam isteği yapılandırması bayrakları. */
    data class AdRequestTags(
        /** TAG_FOR_CHILD_DIRECTED_TREATMENT */
        val childDirectedTreatment: Boolean,
        /** TAG_FOR_UNDER_AGE_OF_CONSENT */
        val underAgeOfConsent: Boolean,
    )

    const val COLLECTION_USERS = "users"
    const val FIELD_BIRTH_YEAR = "birthYear"

    /** Yalnızca hata ayıklama derlemesinde eklenen AdMob test cihazı kimliği (Android cihazı). */
    const val DEBUG_TEST_DEVICE_ID = "7EE88C5BC09FB40090CCEF8FB3AF1F1D"

    /** Ülke belirlenemezse kullanılan kod. */
    const val FALLBACK_COUNTRY = "US"

    /**
     * Firestore belgesinden doğum yılı: belge yoksa, alan yoksa ya da sayı değilse -1.
     */
    fun birthYearFromDoc(docExists: Boolean, containsBirthYear: Boolean, birthYearValue: Long?): Int =
        if (docExists && containsBirthYear) {
            birthYearValue?.toInt() ?: -1
        } else -1

    /**
     * Muamele seviyesi.
     * - Oturum yoksa → CHILD (güvenli varsayılan)
     * - Doğum yılı yok/geçersiz (<= 0) → CHILD
     * - Yaş (yıl farkı; ay/gün dikkate alınmaz) rıza yaşının altındaysa CHILD, değilse TEEN
     * Firestore okuması başarısız olursa da çağıran CHILD uygular.
     */
    fun treatmentFor(isLoggedIn: Boolean, birthYear: Int, currentYear: Int, countryCode: String): AgeTreatment {
        if (!isLoggedIn) return AgeTreatment.CHILD
        if (birthYear > 0) {
            val age = currentYear - birthYear
            val consentAge = getDigitalConsentAgeForCountry(countryCode)
            return if (age < consentAge) AgeTreatment.CHILD else AgeTreatment.TEEN
        }
        return AgeTreatment.CHILD
    }

    /** CHILD / UNSPECIFIED → TFCD=true + TFUA=true; TEEN → TFCD=false + TFUA=false. */
    fun adRequestTags(treatment: AgeTreatment): AdRequestTags = when (treatment) {
        AgeTreatment.CHILD, AgeTreatment.UNSPECIFIED ->
            AdRequestTags(childDirectedTreatment = true, underAgeOfConsent = true)
        AgeTreatment.TEEN ->
            AdRequestTags(childDirectedTreatment = false, underAgeOfConsent = false)
    }

    /**
     * Android (güncel) `setAnalyticsAdConsent`: Firebase Analytics reklam izni (AD_STORAGE, AD_USER_DATA,
     * AD_PERSONALIZATION üçü birden) YALNIZCA muamele TEEN iken verilir; CHILD/UNSPECIFIED'da açıkça reddedilir
     * (izin diskte kalıcı, önceki yetişkin oturumundan kalmasın). ANALYTICS_STORAGE'a dokunulmaz.
     */
    fun analyticsAdConsentGranted(treatment: AgeTreatment): Boolean = treatment == AgeTreatment.TEEN

    /**
     * Android `watchAccountChangesForAnalyticsConsent`: hesap değişince (çıkış, farklı hesap) izin kapatılmalı mı —
     * [consentUid] iznin verildiği hesap (yoksa null); [currentUid] şimdiki oturum. İzin yalnızca aynı hesapta sürer.
     */
    fun shouldRevokeAnalyticsAdConsent(consentUid: String?, currentUid: String?): Boolean =
        currentUid == null || currentUid != consentUid

    /** Rıza (UMP) isteğine verilen "rıza yaşının altında" bayrağı. */
    fun consentUnderAge(treatment: AgeTreatment): Boolean = treatment != AgeTreatment.TEEN

    /**
     * Cihazın ülke kodu. Öncelik: SIM ülkesi → ağ ülkesi → sistem yerel ayarı → "US".
     * Sonuç büyük harflidir.
     */
    fun resolveCountryCode(simCountryIso: String?, networkCountryIso: String?, localeCountry: String?): String {
        if (!simCountryIso.isNullOrBlank()) return simCountryIso.uppercase()
        if (!networkCountryIso.isNullOrBlank()) return networkCountryIso.uppercase()
        return localeCountry.orEmpty().uppercase().ifBlank { FALLBACK_COUNTRY }
    }

    /**
     * GDPR / COPPA gereği ülkelere göre dijital rıza yaşı.
     */
    fun getDigitalConsentAgeForCountry(countryCode: String): Int {
        return when (countryCode) {
            // 16 yaş sınırı olan ülkeler (GDPR varsayılanı)
            "DE", "IE", "NL", "RO", "HR", "LU", "SK", "HU", "PL" -> 16
            // 15 yaş sınırı olan ülkeler
            "FR", "GR", "CZ", "SI" -> 15
            // 14 yaş sınırı olan ülkeler
            "AT", "BG", "CY", "ES", "IT", "LT" -> 14
            // Geri kalan EEA ülkeleri, ABD (COPPA) ve diğer ülkeler (13)
            else -> 13
        }
    }
}
