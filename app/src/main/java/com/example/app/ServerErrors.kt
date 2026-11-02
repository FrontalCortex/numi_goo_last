package com.example.app

import androidx.annotation.StringRes
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctionsException.Code

/**
 * Sunucu (callable) hatasının kullanıcıya gösterilecek metni.
 *
 * Sunucunun HttpsError mesajları Türkçe yazılı ("Çok fazla yanlış deneme…"). Türkçede eskisi gibi
 * sunucunun mesajı; başka dilde hatanın KODUNDAN genel bir metin, kod bir şey söylemiyorsa
 * çağıranın [fallback]'i. Sunucu mesajlarını dillere çevirmek her callable'a dil göndermeyi
 * gerektiriyordu; giriş akışı için kod yeterli.
 */
fun serverErrorText(e: Exception, @StringRes fallback: Int): String {
    val fe = e as? FirebaseFunctionsException
    if (AppLanguage.isTurkish()) return fe?.message ?: e.localizedMessage ?: appString(fallback)
    return when (fe?.code) {
        Code.RESOURCE_EXHAUSTED -> appString(R.string.ui_server_too_many)
        Code.UNAUTHENTICATED -> appString(R.string.ui_oturum_acik_degil)
        Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED -> appString(R.string.ui_sunucuya_su_an_ulasilamiyor_birazdan_tekrar)
        else -> appString(fallback)
    }
}
