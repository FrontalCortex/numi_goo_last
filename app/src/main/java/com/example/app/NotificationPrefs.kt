package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Bildirim tercihlerinin tek kaynağı — türe göre açma/kapama.
 *
 * NEDEN TÜRE GÖRE
 *   Önceden tek bir anahtar vardı (`notifications_enabled`) ve üç tür bildirim de ona
 *   bağlıydı. Oyun bildiriminden rahatsız olan kullanıcının kapatabileceği tek şey
 *   ÖĞRETMEN MESAJLARI DAHİL her şeydi — oysa sohbet bildirimi ürünün çalışması için
 *   zorunlu olan tek bildirim: öğretmenin cevabını duymayan öğrenci sorusunun
 *   cevaplandığını hiç bilmiyor. Tek anahtar, en değerli bildirimi en gereksizi uğruna
 *   feda ettiriyordu.
 *
 * İKİ YERDE TUTULUYOR, SEBEBİ FARKLI
 *   • SharedPreferences — bildirim geldiği anda okunuyor. Uygulama kapalıyken Firestore'a
 *     gitmek ne hızlı ne garantili; karar yerel veriyle veriliyor.
 *   • Firestore (`users/{uid}.notificationPrefs`) — sunucu gönderip göndermeyeceğine
 *     bakıyor (bkz. functions/notifications.js). Kapalı bir türü hiç göndermemek, gönderip
 *     istemcide atmaktan iyi: boşa FCM maliyeti olmuyor ve kullanıcının ikinci cihazı da
 *     aynı tercihi uyguluyor.
 *
 * GERİYE DÖNÜK UYUM
 *   Eski `notifications_enabled` anahtarı korunuyor ve varsayılan olarak hepsinin yerine
 *   geçiyor ([isEnabled]). Sahadaki eski sürümler hâlâ onu yazıyor; sunucu tarafında aynı
 *   öncelik kuralı var (notifications.js → notificationPrefsFor).
 */
object NotificationPrefs {

    private const val TAG = "NotificationPrefs"
    private const val PREFS = "AppPrefs"

    /** Eski tek anahtar. Yeni türe özel anahtar yoksa bunun değeri geçerli. */
    const val KEY_LEGACY_ALL = "notifications_enabled"

    /**
     * Tür anahtarları. Sunucudaki katalogla birebir aynı olmak zorunda
     * (functions/notifications.js → NOTIFICATION_PREFS); biri değişirse öteki de değişmeli.
     */
    const val CHAT = "chat"
    const val STREAK = "streak"
    const val REWARD = "reward"

    /**
     * `account` türü (deneme süresi bitişi, kredi iadesi, sorunun durumu) bilerek BURADA YOK:
     * uygulama içinden kapatılamıyor. Gerekçe sunucu tarafında yazılı — kullanıcının
     * parasıyla ve verdiğimiz sözle ilgili bilgi, kaçırıldığında yerine geçecek başka bir
     * kanal olmadığı için pazarlama bildirimi gibi kapatılabilir olmamalı. Kullanıcı yine de
     * Android'in kanal ayarından susturabiliyor.
     */
    val ALL = listOf(CHAT, STREAK, REWARD)

    private fun keyOf(type: String) = "notify_$type"

    /**
     * Bu tür gösterilsin mi.
     *
     * Öncelik: türe özel anahtar > eski tek anahtar > açık. Daha özel olanın kazanması
     * önemli: yeni sürümde sohbet bildirimini kapatmış kullanıcı, evdeki eski sürümlü
     * tablette ana anahtar açıldığında kapattığı şeyi geri almamalı.
     */
    fun isEnabled(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = keyOf(type)
        if (prefs.contains(key)) return prefs.getBoolean(key, true)
        return prefs.getBoolean(KEY_LEGACY_ALL, true)
    }

    /** Tercihi yerelde ve Firestore'da günceller. */
    fun setEnabled(context: Context, type: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(keyOf(type), enabled)
            .apply()
        syncToFirestore(context)
    }

    /**
     * Üç tercihi birlikte Firestore'a yazar.
     *
     * Hepsi birden yazılıyor çünkü sunucu haritanın tamamına bakıyor ve tek alanı
     * güncellemek eksik bir harita bırakabilir (örn. yalnızca `chat` yazılmışsa sunucu
     * `reward` için eski tek anahtara düşer — kullanıcının az önce yaptığı seçim değil).
     */
    fun syncToFirestore(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val map = ALL.associateWith { isEnabled(context, it) }
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("notificationPrefs" to map), SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "Tercihler yazılamadı", e) }
    }

    /**
     * Eski tek anahtar değiştiğinde çağrılır: türe özel anahtarların hepsini aynı değere
     * çeker.
     *
     * NEDEN GEREKLİ
     *   Eski anahtar yalnızca "hiç tercih belirtilmemişse" geçerli ([isEnabled]). Kullanıcı
     *   bir kez türe özel seçim yaptıktan sonra ana anahtarı kapatsa, türe özel anahtar
     *   kazanırdı ve bildirim gelmeye devam ederdi — kullanıcının gözünde apaçık bir hata.
     */
    fun setAll(context: Context, enabled: Boolean) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_LEGACY_ALL, enabled)
        ALL.forEach { editor.putBoolean(keyOf(it), enabled) }
        editor.apply()
        syncToFirestore(context)
    }
}
