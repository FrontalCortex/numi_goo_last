package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

/**
 * "Bu cihazda daha önce hiç hesap açıldı mı" bayrağı — ilk tutorial'in gösterilme şartı.
 *
 * ## Neden eski şart değiştirildi
 * Önceden şart `first_tutorial_shown` idi ve o bayrak yalnızca `currentTutorialNumber == 1`
 * iken yazılıyordu. Kullanıcı ilk tutorial'i BAŞARISIZ geçtiğinde o sayaç sıfırlanıyor,
 * dolayısıyla dersi sonradan tamamladığında koşul artık sağlanmıyor ve bayrak hiç
 * yazılmıyordu. Sonuç: tutorial tekrar tekrar gösteriliyordu.
 *
 * Yeni şart bir ders akışının iç durumuna değil, gözlenebilir tek bir gerçeğe bakıyor:
 * bu cihazda hiç hesap açıldı mı. Açıldıysa (şu an oturum kapalı olsa bile) tutorial
 * gösterilmiyor.
 *
 * ## Neden kendi kendini onaran tek okuma
 * Bayrağı "kayıt başarılı", "giriş başarılı", "Google ile giriş", "öğretmen girişi" gibi
 * her yola ayrı ayrı yazmak, yarın eklenecek bir yolu unutmak demekti. Bunun yerine okuma
 * anında oturum kontrol ediliyor: uygulama bir kez oturumlu çalıştıysa bayrak kalıcı olarak
 * yazılıyor. Kayıt zaten MainActivity'yi oturumlu açtığı için bu her yolu kapsıyor.
 */
object DeviceAccountStore {

    private const val TAG = "DeviceAccountStore"
    private const val PREFS = "AppPrefs"
    private const val KEY = "account_ever_created"

    /** Eski kurulumlardan devralınan bayrak; yalnızca geçiş için okunuyor, artık yazılmıyor. */
    private const val LEGACY_KEY = "first_tutorial_shown"

    /**
     * Bu cihazda daha önce hesap açılmış mı.
     *
     * Okuma yan etkili: oturum açıksa bayrak kalıcılaştırılıyor. Bu bilinçli — bayrağın
     * yazılması tek bir noktaya (buraya) bağlı kalsın diye.
     */
    fun hasEverHadAccount(context: Context): Boolean {
        val prefs = try {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        } catch (e: Throwable) {
            Log.w(TAG, "SharedPreferences açılamadı", e)
            return false
        }
        if (prefs.getBoolean(KEY, false)) return true

        val signedIn = try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (e: Throwable) {
            Log.w(TAG, "Oturum okunamadı", e)
            false
        }

        // Güncellemeden önce kurulmuş cihazlar: tutorial'i çoktan görmüş kullanıcıya onu
        // ikinci kez izletmemek için eski bayrak da kabul ediliyor. Yeni kurulumlarda bu
        // anahtar hiç yazılmadığı için kural saf haliyle işliyor.
        val legacy = prefs.getBoolean(LEGACY_KEY, false)

        if (signedIn || legacy) {
            prefs.edit().putBoolean(KEY, true).apply()
            Log.d(TAG, "hesap geçmişi işaretlendi (oturum=$signedIn eskiBayrak=$legacy)")
            return true
        }
        return false
    }
}
