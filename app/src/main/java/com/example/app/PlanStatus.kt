package com.example.app

import com.google.firebase.firestore.DocumentSnapshot

/**
 * `users/{uid}` dokümanından geçerli abonelik planını okur.
 *
 * NEDEN AYRI BİR YER
 *   `plan` alanına tek başına bakmak yetmez: süresi dolmuş bir abonelik Firestore'da
 *   "Pro" olarak kalabilir. Sunucu bunu yalnızca istemci elindeki Play token'ını
 *   doğrulattığında düzeltir; kullanıcı aboneliği tamamen iptal ettiyse gönderilecek bir
 *   token kalmaz ve alan "Pro" olarak donar. Bu yüzden okuyan her yer `planExpiresAt`
 *   kontrolünü de yapmak zorundadır.
 *
 *   Bu kontrol daha önce her çağrı yerinde elle tekrarlanıyordu ve bazı yerlerde
 *   (ders bölümü kilidi, profil rozeti) atlanmıştı. Tek yerde toplandı.
 *
 * Sunucudaki `effectivePlan` (functions/index.js) ile aynı kuralı uygular.
 */
object PlanStatus {

    /**
     * Süresi geçmişse "Free", değilse dokümandaki plan. Yayın hediyesi (`giftProUntil`,
     * ilk 100 öğrenciye 1 ay Pro; sunucuda functions/launchGift.js yazar) sürüyorsa ve
     * abonelik Pro'dan düşükse "Pro".
     */
    fun effectivePlan(doc: DocumentSnapshot): String =
        effectivePlan(
            storedPlan = doc.getString("plan"),
            planExpiresAt = doc.getLong("planExpiresAt"),
            giftProUntil = doc.getLong(FIELD_GIFT_PRO_UNTIL),
            nowMs = System.currentTimeMillis(),
        )

    /** Saf hâli; birim testi bunu kullanıyor. */
    fun effectivePlan(storedPlan: String?, planExpiresAt: Long?, giftProUntil: Long?, nowMs: Long): String {
        val stored = storedPlan ?: "Free"
        val expiresAt = planExpiresAt ?: 0L
        val expired = expiresAt > 0L && expiresAt < nowMs
        val subscription = if (expired) "Free" else stored
        val giftActive = (giftProUntil ?: 0L) > nowMs
        return if (giftActive && rank(subscription) < rank("Pro")) "Pro" else subscription
    }

    /** Hediye sürüyorsa bitiş zamanı (ms), yoksa null. */
    fun giftProUntil(doc: DocumentSnapshot): Long? =
        doc.getLong(FIELD_GIFT_PRO_UNTIL)?.takeIf { it > System.currentTimeMillis() }

    const val FIELD_GIFT_PRO_UNTIL = "giftProUntil"

    // Sunucudaki PLAN_RANK ile aynı sıra.
    private fun rank(plan: String): Int = when {
        plan.equals("Premium", ignoreCase = true) -> 3
        plan.equals("Pro", ignoreCase = true) -> 2
        plan.equals("Lite", ignoreCase = true) -> 1
        else -> 0
    }

    /** Pro veya Premium (ikisi de sınırsız enerji ve reklamsızlık verir). */
    fun isPro(doc: DocumentSnapshot): Boolean = isProPlan(effectivePlan(doc))

    /** Plan adı elde varken kullanılır (ör. dokümandan bir kez okunup taşınmışsa). */
    fun isProPlan(plan: String): Boolean =
        plan.equals("Pro", ignoreCase = true) || plan.equals("Premium", ignoreCase = true)
}
