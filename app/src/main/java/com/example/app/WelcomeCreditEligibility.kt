package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/**
 * "Bu CİHAZ Pro hoş geldin kredisini alabilir mi?" sorusunun önbelleklenmiş cevabı.
 *
 * NEDEN
 *   AskQuestionOpenFragment'ın vaadi öğretmene soru sormak. Cihaz hoş geldin kredisini daha
 *   önce tükettiyse (aynı telefonda ikinci uygulama hesabı), kullanıcı denemeyi başlatsa bile
 *   kredi almaz: Pro olur ama soru soramaz. O kullanıcıya bu tanıtımı otomatik açmak, ekranın
 *   tek işlevini teslim etmemek demek — bu yüzden promo kapısına ek koşul olarak giriyor.
 *
 * NEDEN ÖNBELLEK
 *   Promo kararı ders dönüşünde SENKRON veriliyor (bkz. MainActivity.maybeShowAskQuestionPromo);
 *   o anda ağ çağrısı bekleyemez. Değer uygulama açılışında ve abonelik doğrulandıktan sonra
 *   tazeleniyor.
 *
 * CİHAZ BAŞINA, HESAP BAŞINA DEĞİL
 *   Sunucudaki kapı cihaz özetine bakıyor; bu yüzden önbellek de uid'den bağımsız tutuluyor —
 *   hesap değiştirmek cevabı değiştirmez.
 *
 * BİLİNMİYORSA GÖSTER
 *   Hiç tazelenmediyse (ilk açılış, çevrimdışı) `true` varsayılıyor: mevcut davranış bu ve
 *   uygun bir kullanıcıdan promoyu saklamak, uygun olmayana göstermekten daha büyük bir
 *   gerileme olur.
 */
object WelcomeCreditEligibility {

    private const val PREFS = "welcome_credit_eligibility"
    private const val KEY_ELIGIBLE = "eligible"
    private const val FN_CHECK = "checkWelcomeCreditEligibility"
    private const val TAG = "WelcomeCredit"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEligible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ELIGIBLE, true)

    /**
     * Sunucuya sorar ve sonucu saklar. Oturum yoksa ya da cihaz kimliği okunamazsa hiçbir
     * şey yapmaz — mevcut önbellek korunur.
     */
    fun refresh(context: Context) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        val deviceKey = deviceKey(context) ?: return

        FirebaseFunctions.getInstance()
            .getHttpsCallable(FN_CHECK)
            .call(hashMapOf("deviceKey" to deviceKey))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                val eligible = data["eligible"] as? Boolean ?: return@addOnSuccessListener
                prefs(context).edit().putBoolean(KEY_ELIGIBLE, eligible).apply()
                Log.d(TAG, "Cihaz hoş geldin kredisine uygun mu: $eligible (checked=${data["checked"]})")
            }
            .addOnFailureListener { e ->
                // Ağ/izin hatası: önbelleği bozma, mevcut değerle devam et.
                Log.w(TAG, "Uygunluk sorgulanamadı: ${e.message}")
            }
    }

    /**
     * Sunucudaki cihaz kapısının kullandığı tanımlayıcının aynısı (bkz.
     * BillingManager.deviceKeyForWelcomeCredit). Ham değer yalnızca çağrıda gönderilir.
     */
    private fun deviceKey(context: Context): String? = runCatching {
        android.provider.Settings.Secure.getString(
            context.applicationContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
