package com.example.app

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Kayıtta sorulan "Abaküs kullanmayı biliyor musun?" cevabı (kapalı test: soroban bilen ve
 * bilmeyen çocuk ayrı değerlendirilecek).
 *
 * ## Neden kayıt zincirinden geçmiyor
 * Yaş ve kaynak cevapları kayıt ekranlarından sunucu fonksiyonlarına kadar taşınıyor (13 yaş
 * altında hesabı sunucu açıyor). Bu cevabı o zincire eklemek dört kayıt yolunu ve fonksiyon
 * deploy'unu gerektirirdi. Onun yerine cevap cihazda bekliyor ([savePending]) ve hesap
 * açıldıktan sonra uygulama kullanıcı dokümanını ilk okuduğunda kendisi yazıyor
 * ([applyFromUserDoc]); kurallar bu alanı sahibine açık bırakıyor. Kayıt sırasında yazılamaz:
 * o anda hesap henüz yok.
 *
 * Analytics'e kayıtta da yazılıyor: kayıttan vazgeçenlerde de kırılım görünsün.
 */
object SorobanLevel {

    const val NONE = "none"
    const val SOME = "some"
    const val GOOD = "good"

    /** Sıra ekrandaki sırayla aynı: bilmeyenden bilene. */
    val VALUES = listOf(NONE, SOME, GOOD)

    /** `users/{uid}` alanı. */
    const val FIELD = "sorobanLevel"

    private const val PREFS = "soroban_level_pending"
    private const val KEY_LEVEL = "level"
    private const val KEY_SAVED_AT = "saved_at"

    /**
     * Bekleyen cevap bu süreden eskiyse atılıyor: kayıt yarıda bırakılıp günler sonra başka
     * biri bu cihazda hesap açarsa eski cevap ona yazılmasın. Veli onayı kodu 30 dakika
     * geçerli, bir gün bol bol yetiyor.
     */
    private const val PENDING_MAX_AGE_MS = 24L * 60 * 60 * 1000

    fun savePending(context: Context, level: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LEVEL, level)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
        AnalyticsLogger.setSorobanLevel(level)
    }

    /**
     * Kullanıcı dokümanı okunduğunda: alan varsa Analytics'e o yazılıyor; yoksa ve cihazda
     * taze bir kayıt cevabı bekliyorsa dokümana yazılıyor. Bekleyen cevap her durumda
     * siliniyor (bir kez uygulanır).
     *
     * Alan dolu bir hesaba bekleyen cevap YAZILMIYOR: kayıttan vazgeçip mevcut hesabına giren
     * kullanıcının eski cevabı ezilmesin.
     */
    fun applyFromUserDoc(context: Context, uid: String, doc: DocumentSnapshot) {
        val stored = doc.getString(FIELD)?.takeIf { it in VALUES }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = prefs.getString(KEY_LEVEL, null)?.takeIf { it in VALUES }
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        if (pending != null || savedAt != 0L) prefs.edit().clear().apply()

        if (stored != null) {
            AnalyticsLogger.setSorobanLevel(stored)
            return
        }
        val fresh = System.currentTimeMillis() - savedAt in 0..PENDING_MAX_AGE_MS
        if (pending == null || !fresh) {
            AnalyticsLogger.setSorobanLevel(null)
            return
        }
        AnalyticsLogger.setSorobanLevel(pending)
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(FIELD, pending)
            .addOnFailureListener {
                // Yazılamadıysa cevap bir sonraki okumada yeniden denensin.
                savePending(context, pending)
            }
    }
}
