package com.example.app

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment

/**
 * Fragment view'ı yok edildikten sonra çalışan gecikmeli işleri güvenli hale getiren
 * yardımcılar.
 *
 * NEDEN GEREKLİ
 *   Büyük fragment'ler (TutorialFragment, AbacusFragment, BlindingLessonFragment...)
 *   `binding`'i `lateinit var` olarak tutuyor ve `onDestroyView`'da bırakmıyordu; bu,
 *   fragment geri yığınında (back stack) beklerken tüm view hiyerarşisinin bellekte
 *   kalmasına yol açıyordu.
 *
 *   `binding`'i nullable yapıp `onDestroyView`'da bırakmak sızıntıyı kapatıyor — ama
 *   `onDestroyView` sonrasında tetiklenen bir `postDelayed` geri çağrısı artık
 *   NullPointerException'a dönüşür. (Bugün de hatalı: ölü bir view'a yazıyor, sadece
 *   sessizce.) Bu yüzden gecikmeli işler buradaki sarmalayıcılardan geçiriliyor:
 *   view yoksa iş sessizce atlanıyor.
 */
private val mainHandler = Handler(Looper.getMainLooper())

/** Fragment eklenmiş ve view'ı hâlâ hayatta mı? */
val Fragment.isViewAlive: Boolean
    get() = isAdded && view != null

/**
 * View hâlâ hayattaysa [action]'ı çalıştırır, değilse hiçbir şey yapmaz.
 * Asenkron geri çağrıların (Firestore, animasyon bitişi vb.) başına konur.
 */
inline fun Fragment.ifViewAlive(action: () -> Unit) {
    if (isViewAlive) action()
}

/**
 * [delayMs] milisaniye sonra [action]'ı çalıştırır — yalnızca fragment'in view'ı o an
 * hâlâ hayattaysa. `Handler(Looper.getMainLooper()).postDelayed { ... }` yerine kullanılır;
 * o biçim iptal edilemediği için `onDestroyView` sonrasında da tetikleniyordu.
 */
fun Fragment.postDelayedSafely(delayMs: Long, action: () -> Unit) {
    mainHandler.postDelayed({
        if (isViewAlive) action()
    }, delayMs)
}
