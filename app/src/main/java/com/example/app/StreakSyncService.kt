package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/**
 * Seri sayacını sunucuyla eşitler (`submitStreakDay`) ve kilometre taşı ödülünü toplar
 * (`claimStreakReward`).
 *
 * ## Neden sayaç sunucuda
 * Seri artık ödül dağıtıyor ve cüzdan alanları kurallarda sunucuya özel. Ödülü veren
 * fonksiyonun doğrulayacak bir şeye ihtiyacı var; bu yüzden sayacı sunucu tutuyor. İstemci
 * yalnızca "şu günlerde hedefimi tutturdum" diyebiliyor.
 *
 * ## Yerel sayaç neden duruyor
 * Seri kayıttan ÖNCE, ilk ders sırasında başlıyor — o anda uid yok. Ayrıca çevrimdışı da
 * işlemeli. Bu yüzden yerel sayaç arayüzün hızlı yolu olarak kaldı; ÖDÜL kararları ise
 * yalnızca sunucudan gelen değere bakıyor (bkz. [StreakRepository.serverCurrent]).
 */
object StreakSyncService {

    private const val TAG = "StreakSyncService"

    /** Aynı anda iki eşitleme gitmesin; ikisi de aynı günleri gönderirdi. */
    @Volatile private var syncing = false

    /**
     * Başarısız denemeden sonra yeniden denemenin serbest olduğu an (monoton saat).
     *
     * Eşitleme her ekran değişiminde tetikleniyor. Bu olmadan, internetin olmadığı bir
     * cihazda ekranlar arası her geçiş yeni bir başarısız çağrı başlatırdı.
     */
    @Volatile private var retryAfterMs = 0L

    private const val RETRY_BACKOFF_MS = 60_000L

    private fun uid(): String? = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (e: Throwable) {
        Log.w(TAG, "Oturum okunamadı", e)
        null
    }

    /**
     * Bekleyen günleri gönderir. Bekleyen gün yoksa hiçbir şey yapmaz.
     *
     * @param onDone Sunucudan dönen güncel durum yazıldıktan sonra çalışır (arayüz tazelensin
     *   diye). Başarısızlıkta çağrılmaz; bekleyen günler diskte kalır ve sonraki denemede
     *   yeniden gönderilir.
     */
    fun syncPendingDays(context: Context, onDone: (() -> Unit)? = null) {
        if (uid() == null) return
        if (syncing) return
        if (android.os.SystemClock.elapsedRealtime() < retryAfterMs) return
        val days = StreakRepository.pendingSyncDays(context)
        if (days.isEmpty()) return

        syncing = true
        val payload = hashMapOf(
            "days" to days,
            "goalMinutes" to StreakRepository.goalMinutes(context),
            "challengeDays" to StreakRepository.challengeDays(context),
        )
        FirebaseFunctions.getInstance()
            .getHttpsCallable("submitStreakDay")
            .call(payload)
            .addOnSuccessListener { result ->
                syncing = false
                retryAfterMs = 0L
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                val current = (data["current"] as? Number)?.toInt() ?: return@addOnSuccessListener
                val longest = (data["longest"] as? Number)?.toInt() ?: current
                val claimed = (data["claimed"] as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toInt() }
                    ?.toSet()
                    .orEmpty()
                // Gönderilen günler kabul edildi; kuyruktan yalnızca ONLAR siliniyor.
                // Arada yeni bir gün eklenmiş olabilir, o gitmemeli.
                StreakRepository.onSyncAccepted(context, days, current, longest, claimed)
                onDone?.invoke()
            }
            .addOnFailureListener { e ->
                syncing = false
                retryAfterMs = android.os.SystemClock.elapsedRealtime() + RETRY_BACKOFF_MS
                // Sessiz: seri yerelde çalışmaya devam ediyor, yalnızca ödüller bekliyor.
                Log.w(TAG, "submitStreakDay başarısız", e)
            }
    }

    /**
     * Kilometre taşı ödülünü toplar.
     *
     * @param onResult Başarıda kazanılan anahtar/altın ve yeni bakiyeler; başarısızlıkta
     *   kullanıcıya gösterilecek mesaj.
     */
    fun claimReward(
        context: Context,
        milestone: Int,
        onResult: (success: Boolean, message: String, keys: Int, currency: Int) -> Unit,
    ) {
        if (uid() == null) {
            onResult(false, "Ödülü almak için giriş yapman gerekiyor.", 0, 0)
            return
        }
        FirebaseFunctions.getInstance()
            .getHttpsCallable("claimStreakReward")
            .call(hashMapOf("milestone" to milestone))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val keys = (data?.get("keys") as? Number)?.toInt() ?: 0
                val currency = (data?.get("currency") as? Number)?.toInt() ?: 0
                val rewardKeys = (data?.get("rewardKeys") as? Number)?.toInt() ?: 0
                val rewardGold = (data?.get("rewardGold") as? Number)?.toInt() ?: 0
                StreakRepository.markMilestoneClaimed(context, milestone)
                val what = listOfNotNull(
                    if (rewardKeys > 0) "$rewardKeys anahtar" else null,
                    if (rewardGold > 0) "$rewardGold altın" else null,
                ).joinToString(" + ")
                onResult(true, "$what kazandın!", keys, currency)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "claimStreakReward başarısız", e)
                onResult(false, "Ödül alınamadı. İnternetini kontrol edip tekrar dene.", 0, 0)
            }
    }
}
