package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Seri sayacını sunucuyla eşitler (`submitStreakDay`) ve ödülleri toplar
 * (`claimStreakReward`: hem kilometre taşları hem meydan okuma).
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

    /**
     * Cihazın UTC farkı (dakika). Türkiye için +180.
     *
     * Yaz saati uygulayan yerlerde yılda iki kez değişiyor; günlük bildirim bu yüzden her gün
     * yeniden gönderiliyor ve hatırlatma saati kendiliğinden düzeliyor.
     */
    private fun utcOffsetMinutes(): Int =
        java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    private fun uid(): String? = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (e: Throwable) {
        Log.w(TAG, "Oturum okunamadı", e)
        null
    }

    /**
     * Sunucudaki seri durumunu okur ve yerel duruma işler.
     *
     * Eşitlemenin okuma yönü. Yazma yönü tek başına yetmiyordu: yeni bir telefona kurulum
     * yapan kullanıcının serisi cihazda olmadığı için sıfırdan başlardı — oysa senkronun
     * varlık sebebi tam olarak buydu. Ödül satırları da sunucunun bildiğini göremezdi.
     *
     * Doğrudan Firestore okuması yapılıyor, fonksiyon çağrısı değil: kurallar bu dokümanı
     * sahibine okumaya zaten açıyor ve okuma yazmadan ucuz.
     */
    fun refreshFromServer(context: Context, onDone: (() -> Unit)? = null) {
        val uid = uid() ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("streak").document("state")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    StreakDiag.log("Sync.oku", "SUNUCUDA_DOKUMAN_YOK (hiç gün bildirilmemiş)")
                    return@addOnSuccessListener
                }
                val current = (doc.get("current") as? Number)?.toInt() ?: 0
                val longest = (doc.get("longest") as? Number)?.toInt() ?: 0
                val lastDay = doc.getString("lastDay").orEmpty()
                StreakDiag.log(
                    "Sync.oku",
                    "SUNUCU_DOKUMANI current=$current longest=$longest " +
                        "lastDay=${lastDay.ifEmpty { "(bos)" }} " +
                        "goalMinutes=${(doc.get("goalMinutes") as? Number)?.toInt()} " +
                        "challengeDays=${(doc.get("challengeDays") as? Number)?.toInt()} " +
                        "challengeClaimed=${(doc.get("challengeClaimed") as? Number)?.toInt()}",
                )
                val claimed = (doc.get("claimed") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toInt() }
                    ?.toSet()
                    .orEmpty()
                val recentDays = (doc.get("recentDays") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()
                    .orEmpty()
                // Hatırlatma saati de geri geliyor: başka cihazda seçilmiş olabilir.
                (doc.get("reminderLocalHour") as? Number)?.toInt()?.let {
                    StreakRepository.adoptServerReminderHour(context, it)
                }
                StreakRepository.adoptServerState(
                    context, current, longest, lastDay, claimed, recentDays,
                    // Hedef/meydan okuma da geri geliyor: hesabı olup yeni cihaza kurulum
                    // yapan kullanıcı kayıt akışından geçmiyor, yani hedefi sorulmuyor.
                    // Bunlar okunmasaydı varsayılan 5 dakikaya düşerdi.
                    goalMinutes = (doc.get("goalMinutes") as? Number)?.toInt() ?: 0,
                    challengeDays = (doc.get("challengeDays") as? Number)?.toInt() ?: 0,
                    // Ödül alındı bilgisi de sunucudan: cihaz değiştiren kullanıcıya aynı
                    // meydan okuma ödülünü ikinci kez toplatmaya çalıştırmamak için.
                    challengeClaimed = (doc.get("challengeClaimed") as? Number)?.toInt() ?: 0,
                    // Dokümanda alan yoksa 0: doküman var ama hiç dondurma alınmamış demek.
                    // (Fonksiyon yanıtlarında durum farklı, bkz. [freezesOf].)
                    freezes = (doc.get("freezes") as? Number)?.toInt() ?: 0,
                    freezeDay = doc.getString("freezeDay").orEmpty(),
                    frozenDays = dayStrings(doc.get("frozenDays")),
                )
                onDone?.invoke()
            }
            .addOnFailureListener { e ->
                // Sessiz: seri yerelde çalışmaya devam ediyor.
                StreakDiag.log("Sync.oku", "OKUNAMADI mesaj=${e.message}")
                Log.w(TAG, "Seri durumu okunamadı", e)
            }
    }

    /**
     * Bekleyen günleri gönderir. Bekleyen gün yoksa hiçbir şey yapmaz.
     *
     * @param onDone Sunucudan dönen güncel durum yazıldıktan sonra çalışır (arayüz tazelensin
     *   diye). Başarısızlıkta çağrılmaz; bekleyen günler diskte kalır ve sonraki denemede
     *   yeniden gönderilir.
     */
    fun syncPendingDays(context: Context, onDone: (() -> Unit)? = null) {
        // Dört sessiz erken dönüş var ve hepsi "gün kaydedilmedi" gibi görünüyor.
        // Hangisinin çalıştığı yazılmazsa zincirin burada mı koptuğu anlaşılamıyor.
        if (uid() == null) {
            StreakDiag.log("Sync.gonder", "ATLANDI neden=oturum_yok")
            return
        }
        if (syncing) {
            StreakDiag.log("Sync.gonder", "ATLANDI neden=zaten_gonderiliyor")
            return
        }
        val nowMs = android.os.SystemClock.elapsedRealtime()
        if (nowMs < retryAfterMs) {
            StreakDiag.log(
                "Sync.gonder",
                "ATLANDI neden=geri_cekilme kalan=${(retryAfterMs - nowMs) / 1000}sn",
            )
            return
        }
        val days = StreakRepository.pendingSyncDays(context)
        // Gönderilecek gün yoksa bile günde bir kez gidiliyor: akşam hatırlatması kullanıcının
        // saat dilimini ve son görülme zamanını bilmek zorunda ve bunlar yalnızca gün
        // bildirimiyle güncellenseydi, hedefini hiç tutturmayan kullanıcı — hatırlatmaya en
        // çok ihtiyacı olan kişi — sunucuda hiç görünmezdi.
        if (days.isEmpty() && !StreakRepository.needsDailyPing(context)) {
            StreakDiag.log("Sync.gonder", "ATLANDI neden=gonderilecek_gun_yok_ve_ping_yapilmis")
            return
        }

        syncing = true
        val payload = hashMapOf(
            "days" to days,
            "goalMinutes" to StreakRepository.goalMinutes(context),
            "challengeDays" to StreakRepository.chosenChallengeDays(context),
            // Hatırlatmanın yerel saate denk gelmesi için; sunucu bundan UTC saatini üretiyor.
            "utcOffsetMinutes" to utcOffsetMinutes(),
            // Seri dondurma için: "dün kaçtı mı" sorusunu sunucu da bu cihazın bugününe göre
            // soruyor. Kendi saatine baksaydı bu cihazda harcanmış bir dondurmayı duruyor
            // sayabilir ve mağazadaki yeni alımı "zaten var" diye reddedebilirdi.
            "today" to StudyTimeTracker.dayId(),
        )
        // Hatırlatma saati yalnızca bu cihazda seçildiyse gidiyor (bkz. StreakRepository):
        // her seferinde gönderilseydi yeni cihazın varsayılanı sunucudaki seçimi ezerdi.
        val sentReminderHour =
            if (StreakRepository.reminderHourPending(context)) StreakRepository.reminderHour(context) else null
        if (sentReminderHour != null) payload["reminderHour"] = sentReminderHour
        StreakDiag.log(
            "Sync.gonder",
            "GONDERILIYOR gunler=$days hedef=${StreakRepository.goalMinutes(context)} " +
                "challengeDays=${StreakRepository.chosenChallengeDays(context)} " +
                "offset=${utcOffsetMinutes()} pingGerekli=${StreakRepository.needsDailyPing(context)}",
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
                val recentDays = (data["recentDays"] as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()
                    .orEmpty()
                val lastDay = (data["lastDay"] as? String).orEmpty()
                StreakDiag.log(
                    "Sync.cevap",
                    "BASARILI current=$current longest=$longest " +
                        "lastDay=${lastDay.ifEmpty { "(bos)" }} recentDays=${recentDays.sorted()} " +
                        "claimed=${claimed.sorted()} " +
                        "goalMinutes=${(data["goalMinutes"] as? Number)?.toInt()} " +
                        "challengeDays=${(data["challengeDays"] as? Number)?.toInt()} " +
                        "challengeClaimed=${(data["challengeClaimed"] as? Number)?.toInt()}",
                )
                StreakRepository.markDailyPing(context)
                if (sentReminderHour != null) StreakRepository.onReminderHourSynced(context, sentReminderHour)
                (data["reminderLocalHour"] as? Number)?.toInt()?.let {
                    StreakRepository.adoptServerReminderHour(context, it)
                }
                // Gönderilen günler kabul edildi; kuyruktan yalnızca ONLAR siliniyor.
                // Arada yeni bir gün eklenmiş olabilir, o gitmemeli.
                StreakRepository.onSyncAccepted(context, days, current, longest, claimed)
                StreakRepository.adoptServerState(
                    context, current, longest, lastDay, claimed, recentDays,
                    // Okuma yolu burada da açık: seri ekranı açılmasa bile cihaz değiştiren
                    // kullanıcının hedefi ve meydan okuması ilk eşitlemede geri geliyor.
                    goalMinutes = (data["goalMinutes"] as? Number)?.toInt() ?: 0,
                    challengeDays = (data["challengeDays"] as? Number)?.toInt() ?: 0,
                    challengeClaimed = (data["challengeClaimed"] as? Number)?.toInt() ?: 0,
                    freezes = freezesOf(data),
                    freezeDay = (data["freezeDay"] as? String).orEmpty(),
                    frozenDays = dayStrings(data["frozenDays"]),
                )
                onDone?.invoke()
            }
            .addOnFailureListener { e ->
                syncing = false
                retryAfterMs = android.os.SystemClock.elapsedRealtime() + RETRY_BACKOFF_MS
                // Sessiz: seri yerelde çalışmaya devam ediyor, yalnızca ödüller bekliyor.
                // Teşhis satırına KOD da yazılıyor: "başarısız" tek başına INTERNAL
                // (sunucu çöktü) ile UNAVAILABLE (internet yok) arasını ayırmıyordu ve
                // aylarca yanlış yere bakılmasına yol açtı.
                StreakDiag.log(
                    "Sync.cevap",
                    "BASARISIZ kod=${(e as? FirebaseFunctionsException)?.code ?: "(taşıma)"} " +
                        "mesaj=${e.message} gonderilen=$days " +
                        "kuyrukta_kaliyor=${StreakRepository.pendingSyncDays(context)}",
                )
                Log.w(TAG, "submitStreakDay başarısız", e)
            }
    }

    /**
     * Fonksiyon yanıtındaki dondurma sayısı; alan yoksa [StreakRepository.FREEZES_UNKNOWN].
     *
     * Yokluk "sıfır" demek değil: sunucunun dondurmadan önceki sürümü alanı hiç döndürmüyor
     * ve onu 0 diye okumak yerel dondurmayı silerdi.
     */
    private fun freezesOf(data: Map<*, *>): Int =
        (data["freezes"] as? Number)?.toInt() ?: StreakRepository.FREEZES_UNKNOWN

    private fun dayStrings(raw: Any?): Set<String> =
        (raw as? List<*>)?.mapNotNull { it as? String }?.toSet().orEmpty()

    /**
     * Altın karşılığı seri dondurma satın alır.
     *
     * Altın düşümü de dondurmanın yazılması da sunucuda, tek transaction'da: cüzdan
     * alanları kurallarda sunucuya özel ve iki adımlı bir akışta araya giren kapanma altını
     * götürüp dondurmayı vermeyebilirdi.
     *
     * @param onResult Başarıda yeni altın bakiyesi; başarısızlıkta kullanıcıya gösterilecek
     *   mesaj.
     */
    fun buyFreeze(
        context: Context,
        onResult: (success: Boolean, message: String, currency: Int) -> Unit,
    ) {
        if (uid() == null) {
            onResult(false, "Seri dondurma almak için giriş yapman gerekiyor.", 0)
            return
        }
        FirebaseFunctions.getInstance()
            .getHttpsCallable("buyStreakFreeze")
            // Sunucu dondurmanın hangi günden itibaren koruduğunu buna göre yazıyor.
            .call(hashMapOf("today" to StudyTimeTracker.dayId()))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val currency = (data?.get("currency") as? Number)?.toInt()
                val freezes = (data?.get("freezes") as? Number)?.toInt()
                if (currency == null || freezes == null) {
                    Log.w(TAG, "buyStreakFreeze geçersiz yanıt döndü: $data")
                    onResult(false, "Satın alınamadı. Birazdan tekrar dene.", 0)
                    return@addOnSuccessListener
                }
                StreakRepository.markFreezePurchased(
                    context, freezes, (data["freezeDay"] as? String).orEmpty(),
                )
                // Bakiye önbelleği yalnızca Firestore dinleyicisiyle tazeleniyor; o gelene
                // kadar üst barda eski sayı kalmasın.
                UserWalletFirestore.cacheCurrency(context, currency)
                AnalyticsLogger.logGoldSpent(
                    AnalyticsLogger.ITEM_STREAK_FREEZE,
                    StreakRepository.FREEZE_COST_GOLD,
                )
                onResult(true, "", currency)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "buyStreakFreeze başarısız", e)
                onResult(false, callErrorMessage(e, "Satın alınamadı."), 0)
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
        claim(hashMapOf("milestone" to milestone), onResult) {
            StreakRepository.markMilestoneClaimed(context, milestone)
        }
    }

    /**
     * Meydan okuma ödülünü toplar.
     *
     * Hangi gün sayısının ödülü olduğu GÖNDERİLMİYOR: sunucu kendi kaydındaki `challengeDays`
     * değerine bakıyor. Gönderilseydi "7 günün ödülünü ver" diyen bir istek kurulabilirdi.
     */
    fun claimChallengeReward(
        context: Context,
        onResult: (success: Boolean, message: String, keys: Int, currency: Int) -> Unit,
    ) {
        val days = StreakRepository.chosenChallengeDays(context)
        claim(hashMapOf("kind" to "challenge"), onResult) { data ->
            // Sunucu hangi günü ödüllendirdiğini döndürüyor; yerelde de onu işaretliyoruz ki
            // düğme, bir sonraki sunucu okumasını beklemeden kaybolsun.
            val claimed = (data?.get("challengeDays") as? Number)?.toInt() ?: days
            StreakRepository.markChallengeClaimed(context, claimed)
        }
    }

    /**
     * Başarısız toplama için kullanıcıya gösterilecek mesaj.
     *
     * Eskiden her hata "İnternetini kontrol et" diyordu. Sunucu aslında ne olup bittiğini
     * söylüyor — "bu ödülü zaten aldın", "serin yeterli değil", "günlük sınıra ulaştın" —
     * ve bunları internete bağlamak kullanıcıyı olmayan bir sorunu kovalamaya gönderiyordu.
     *
     * Taşıma katmanına ait kodlar ayrı tutuluyor: onlarda sunucunun mesajı ya yok ya da
     * İngilizce bir yığın izi oluyor, çocuğa gösterilecek bir şey değil.
     */
    private fun claimErrorMessage(e: Exception): String = callErrorMessage(e, "Ödül alınamadı.")

    /**
     * [claimErrorMessage]'ın genel hâli; ödül toplama ve satın alma aynı ayrımı kullanıyor.
     *
     * @param failed İşlemin kendi "olmadı" cümlesi ("Ödül alınamadı.", "Satın alınamadı.").
     */
    private fun callErrorMessage(e: Exception, failed: String): String {
        val code = (e as? FirebaseFunctionsException)?.code
            ?: return "$failed İnternetini kontrol edip tekrar dene."
        return when (code) {
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
            -> "Sunucuya şu an ulaşılamıyor. Birazdan tekrar dene."
            FirebaseFunctionsException.Code.INTERNAL,
            FirebaseFunctionsException.Code.UNKNOWN,
            -> "$failed İnternetini kontrol edip tekrar dene."
            // Fonksiyon sunucuda yok (henüz deploy edilmemiş): mesajı İngilizce "NOT_FOUND".
            FirebaseFunctionsException.Code.NOT_FOUND,
            FirebaseFunctionsException.Code.UNIMPLEMENTED,
            -> "$failed Birazdan tekrar dene."
            // Geri kalanı sunucunun kendi Türkçe açıklaması (HttpsError mesajı).
            else -> e.message?.takeIf { it.isNotBlank() }
                ?: "$failed Birazdan tekrar dene."
        }
    }

    /**
     * İki ödül türünün ortak yolu: çağrı, kazanılanın metne dönüşü ve hata mesajı aynı.
     *
     * @param markClaimed Başarıda yerel işaret; sunucudan dönen gövdeyi alıyor.
     */
    private fun claim(
        payload: Map<String, Any>,
        onResult: (success: Boolean, message: String, keys: Int, currency: Int) -> Unit,
        markClaimed: (data: Map<*, *>?) -> Unit,
    ) {
        if (uid() == null) {
            onResult(false, "Ödülü almak için giriş yapman gerekiyor.", 0, 0)
            return
        }
        FirebaseFunctions.getInstance()
            .getHttpsCallable("claimStreakReward")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val keys = (data?.get("keys") as? Number)?.toInt() ?: 0
                val currency = (data?.get("currency") as? Number)?.toInt() ?: 0
                val rewardKeys = (data?.get("rewardKeys") as? Number)?.toInt() ?: 0
                val rewardGold = (data?.get("rewardGold") as? Number)?.toInt() ?: 0
                markClaimed(data)
                val what = listOfNotNull(
                    if (rewardKeys > 0) "$rewardKeys anahtar" else null,
                    if (rewardGold > 0) "$rewardGold altın" else null,
                ).joinToString(" + ")
                onResult(true, "$what kazandın!", keys, currency)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "claimStreakReward başarısız", e)
                onResult(false, claimErrorMessage(e), 0, 0)
            }
    }
}
