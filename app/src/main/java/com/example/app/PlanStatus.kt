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

    /** Süresi geçmişse "Free", değilse dokümandaki plan. */
    fun effectivePlan(doc: DocumentSnapshot): String {
        val stored = doc.getString("plan") ?: "Free"
        val expiresAt = doc.getLong("planExpiresAt") ?: 0L
        val expired = expiresAt > 0L && expiresAt < System.currentTimeMillis()
        return if (expired) "Free" else stored
    }

    /** Pro veya Premium (ikisi de sınırsız enerji ve reklamsızlık verir). */
    fun isPro(doc: DocumentSnapshot): Boolean = isProPlan(effectivePlan(doc))

    /** Plan adı elde varken kullanılır (ör. dokümandan bir kez okunup taşınmışsa). */
    fun isProPlan(plan: String): Boolean =
        plan.equals("Pro", ignoreCase = true) || plan.equals("Premium", ignoreCase = true)
}
