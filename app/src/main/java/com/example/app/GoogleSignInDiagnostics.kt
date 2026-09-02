package com.example.app

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

/**
 * Google Sign-In yarıda kaldığında gerçek nedeni okunabilir biçimde döndürür.
 *
 * Play Services, hesap seçim ekranından hem gerçek iptalde hem de yapılandırma
 * hatasında (imza/paket uyuşmazlığı) RESULT_CANCELED ile döner. İkisini yalnızca
 * ApiException'ın durum kodu ayırt eder:
 *
 *   10    DEVELOPER_ERROR — SHA-1 parmak izi veya paket adı Firebase'dekiyle eşleşmiyor
 *   12501 kullanıcı hesap seçmeden çıktı (gerçek iptal)
 *   12500 giriş başarısız (genel)
 *   7     ağ hatası
 */
internal fun googleSignInFailureReason(data: Intent?): String {
    if (data == null) return "sonuç verisi yok (kullanıcı geri tuşuna basmış olabilir)"
    return try {
        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        "hesap döndü ama sonuç RESULT_OK değildi"
    } catch (e: ApiException) {
        "statusCode=${e.statusCode} — ${e.status}"
    }
}
