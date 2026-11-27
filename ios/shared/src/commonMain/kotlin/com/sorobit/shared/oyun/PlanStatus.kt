// Kaynak (Android): app/src/main/java/com/example/app/PlanStatus.kt (YENİ) — saf kısım ([DocumentSnapshot] yerine alan
// değerleri alınır; `System.currentTimeMillis()` -> [nowMs] parametresi).
package com.sorobit.shared.oyun

/**
 * `users/{uid}` dokümanından geçerli abonelik planı.
 *
 * `plan` alanına tek başına bakmak yetmez: süresi dolmuş bir abonelik Firestore'da "Pro" olarak kalabilir (kullanıcı
 * aboneliği tamamen iptal ettiyse sunucuya gönderilecek token kalmaz ve alan donar). Okuyan her yer `planExpiresAt`
 * kontrolünü de yapmak zorundadır; kural burada tek yerde toplandı. Sunucudaki `effectivePlan` (functions/index.js) ile
 * aynı kuralı uygular.
 */
object PlanStatus {

    /** Yayın hediyesi: ilk 100 öğrenciye 1 ay Pro (sunucuda functions/launchGift.js yazar). */
    const val FIELD_GIFT_PRO_UNTIL = "giftProUntil"
    const val FIELD_PLAN = "plan"
    const val FIELD_PLAN_EXPIRES_AT = "planExpiresAt"

    /**
     * Süresi geçmişse "Free", değilse dokümandaki plan. Hediye sürüyorsa ve abonelik Pro'dan düşükse "Pro".
     *
     * @param storedPlan `plan` alanı (yoksa null → "Free")
     * @param planExpiresAt `planExpiresAt` (ms; yoksa/0 → süresiz)
     * @param giftProUntil `giftProUntil` (ms; yoksa null)
     */
    fun effectivePlan(storedPlan: String?, planExpiresAt: Long?, giftProUntil: Long?, nowMs: Long = oyunSimdiMs()): String {
        val stored = storedPlan ?: "Free"
        val expiresAt = planExpiresAt ?: 0L
        val expired = expiresAt > 0L && expiresAt < nowMs
        val subscription = if (expired) "Free" else stored
        val giftActive = (giftProUntil ?: 0L) > nowMs
        return if (giftActive && rank(subscription) < rank("Pro")) "Pro" else subscription
    }

    /** Hediye sürüyorsa bitiş zamanı (ms), yoksa null. */
    fun giftProUntil(giftProUntil: Long?, nowMs: Long = oyunSimdiMs()): Long? =
        giftProUntil?.takeIf { it > nowMs }

    /** Sunucudaki PLAN_RANK ile aynı sıra. */
    private fun rank(plan: String): Int = when {
        plan.equals("Premium", ignoreCase = true) -> 3
        plan.equals("Pro", ignoreCase = true) -> 2
        plan.equals("Lite", ignoreCase = true) -> 1
        else -> 0
    }

    /** Pro veya Premium (ikisi de sınırsız enerji ve reklamsızlık verir). */
    fun isProPlan(plan: String): Boolean =
        plan.equals("Pro", ignoreCase = true) || plan.equals("Premium", ignoreCase = true)
}
