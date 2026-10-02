package com.example.app

import android.content.Context
import android.util.Log

/**
 * Günlük seri (streak): kullanıcının kaç gün üst üste günlük hedefini tutturduğu.
 *
 * ## Serinin kuralı
 * Bir gün "tutturulmuş" sayılır: o gün ders ekranlarında geçen süre
 * ([StudyTimeTracker.secondsToday]) hedefe ulaşırsa. Seri, tutturulan günler ARDIŞIK olduğu
 * sürece büyür; bir gün atlanırsa sıfırlanır.
 *
 * ## Neden "dün"e de bakılıyor
 * Seri okunurken [current] doğrudan dönülmüyor: son tutturulan gün dünden eskiyse seri çoktan
 * kırılmış demektir ama bunu yazan bir olay yok (kullanıcı uygulamayı hiç açmamış olabilir).
 * Bu yüzden kırılma yazıldığı anda değil, OKUNDUĞU anda hesaplanıyor.
 *
 * ## Neden şimdilik yerel
 * Seri, kayıt olmadan önce — ilk ders sırasında — başlıyor; o anda kullanıcının uid'i yok.
 * Bu yüzden kaynak burası, Firestore senkronu bunun üstüne gelecek.
 */
object StreakRepository {

    private const val TAG = "StreakRepository"
    private const val PREFS = "streak_prefs"

    private const val KEY_GOAL_MINUTES = "goal_minutes"
    private const val KEY_CHALLENGE_DAYS = "challenge_days"
    private const val KEY_CURRENT = "current"
    private const val KEY_LONGEST = "longest"
    private const val KEY_LAST_DAY = "last_goal_day"
    private const val KEY_ACHIEVED_DAYS = "achieved_days"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_CELEBRATION_DAY = "celebration_day"
    private const val KEY_CELEBRATION_STREAK = "celebration_streak"
    private const val KEY_CHALLENGE_LOGGED = "challenge_logged"
    private const val KEY_PENDING_SYNC_DAYS = "pending_sync_days"
    private const val KEY_SERVER_CURRENT = "server_current"
    private const val KEY_SERVER_LONGEST = "server_longest"
    private const val KEY_SERVER_CLAIMED = "server_claimed"
    private const val KEY_LAST_PING_DAY = "last_ping_day"
    private const val KEY_OWNER_UID = "owner_uid"
    private const val KEY_CHALLENGE_CLAIMED = "challenge_claimed"
    private const val KEY_CHALLENGE_CLAIMED_DAY = "challenge_claimed_day"
    private const val KEY_NEW_STREAK_PROMPT_DAY = "new_streak_prompt_day"
    private const val KEY_FREEZES = "freezes"
    private const val KEY_FREEZE_DAY = "freeze_day"
    private const val KEY_FROZEN_DAYS = "frozen_days"
    private const val KEY_FREEZE_NOTICE = "freeze_notice"

    /** Onboarding'de sunulan günlük hedefler (dakika). */
    val GOAL_OPTIONS = listOf(5, 10, 20)

    /** Onboarding'de sunulan "kaç gün üst üste" hedefleri. */
    val CHALLENGE_OPTIONS = listOf(3, 5, 7)

    /** Hedef hiç seçilmediyse. En düşük seçenek: kimseyi ilk günden kaybetmeyelim. */
    private const val DEFAULT_GOAL_MINUTES = 5
    private const val DEFAULT_CHALLENGE_DAYS = 3

    /** Hafta şeridi için saklanan gün sayısı. */
    private const val ACHIEVED_HISTORY_DAYS = 21

    /** Eşitleme kuyruğunda en fazla kaç gün beklesin; sunucu da bundan fazlasını almıyor. */
    private const val MAX_PENDING_SYNC_DAYS = 7

    /** Seri dondurma için son günün kaç gün geriye kadar arandığı (bkz. [StreakFreezeRules.settle]). */
    private const val FREEZE_SCAN_DAYS = 30

    /**
     * Seri dondurmanın altın bedeli.
     *
     * Yalnızca GÖSTERİM ve ölçüm için: düşülen miktarı sunucu belirliyor
     * (`functions/index.js` → `STREAK_FREEZE_COST`). İkisi ayrılırsa kart bir fiyat yazar,
     * cüzdandan başka bir miktar gider — biri değişirse diğeri de değişmeli.
     */
    const val FREEZE_COST_GOLD = 4000

    /**
     * [adoptServerState]'e dondurma bilgisi verilmedi.
     *
     * 0 ile aynı şey değil: sunucunun eski sürümü bu alanı hiç döndürmüyor ve o yanıtı
     * "dondurman yok" diye okumak, az önce satın alınmış dondurmayı ekrandan silerdi.
     */
    const val FREEZES_UNKNOWN = -1

    /** Seri ekranında gösterilecek durum. */
    data class StreakState(
        /** Güncel seri; kırıldıysa 0. */
        val current: Int,
        val longest: Int,
        val goalMinutes: Int,
        val secondsToday: Int,
        /** Hedefi tutturulmuş günler (`yyyy-MM-dd`), hafta şeridi için. */
        val achievedDays: Set<String>,
        /** Seri dondurmanın kapattığı günler; hafta şeridinde tutturulmuş günden ayrı çiziliyor. */
        val frozenDays: Set<String> = emptySet(),
        /** Elde harcanmamış seri dondurma var mı. */
        val freezeHeld: Boolean = false,
    ) {
        val goalSeconds: Int get() = goalMinutes * 60
        val goalReachedToday: Boolean get() = secondsToday >= goalSeconds

        /** Bugünkü ilerleme, 0f–1f. */
        val todayFraction: Float
            get() = if (goalSeconds <= 0) 1f
            else (secondsToday.toFloat() / goalSeconds).coerceIn(0f, 1f)

        /** Hedefe kalan dakika, en az 1 (0 kalan "tamamlandı" demek, o ayrı durum). */
        val minutesLeft: Int
            get() = ((goalSeconds - secondsToday + 59) / 60).coerceAtLeast(1)
    }

    // ── Hedefler ────────────────────────────────────────────────────────

    fun goalMinutes(context: Context): Int =
        prefs(context)?.getInt(KEY_GOAL_MINUTES, DEFAULT_GOAL_MINUTES) ?: DEFAULT_GOAL_MINUTES

    fun setGoalMinutes(context: Context, minutes: Int) {
        val value = if (minutes in GOAL_OPTIONS) minutes else DEFAULT_GOAL_MINUTES
        prefs(context)?.edit()?.putInt(KEY_GOAL_MINUTES, value)?.apply()
    }

    /**
     * Kullanıcının seçtiği meydan okuma; hiç seçmemişse 0.
     *
     * Varsayılana DÜŞÜLMÜYOR. Meydan okuma ödül dağıtan bir söz ve yalnızca kayıt
     * akışında veriliyor; bu özellikten ÖNCE kayıt olmuş kullanıcı hiçbir söz vermedi.
     * Varsayılan dönseydi ona üç ayrı yerde yalan söylerdik: sunucuya gönderip vermediği
     * sözü kilitlerdik, kartta basıldığında hata veren bir "Topla" düğmesi gösterirdik ve
     * "meydan okumasını tamamladı" ölçümünü şişirirdik.
     */
    fun chosenChallengeDays(context: Context): Int =
        prefs(context)?.getInt(KEY_CHALLENGE_DAYS, 0) ?: 0

    /**
     * Meydan okumayı yazar. Yalnızca kayıt akışı çağırıyor; seri kırıldıktan sonraki
     * yeni tur için [startNewChallenge] var.
     *
     * ## Kilit neden burada değil
     * Meydan okuma seri YAŞARKEN değiştirilebilseydi kullanıcı 3 günlüğün 500 altınını
     * alıp hemen 7'ye çıkarak 1500'ü de alabilirdi. Ama kilidi buraya koymak yanlıştı:
     * kayıt son adımda (ör. internet kesilmesi) başarısız olup tekrarlandığında
     * kullanıcının YENİ seçimi sessizce yutuluyordu.
     *
     * Kilidi sunucu tutuyor ve şartı "bir kez" değil "tur içinde": `challengePatch`
     * yazmaya yalnızca serinin başında (0 ya da 1. gün) izin veriyor. Ödülü veren de o.
     */
    fun setChallengeDays(context: Context, days: Int) {
        val value = if (days in CHALLENGE_OPTIONS) days else DEFAULT_CHALLENGE_DAYS
        prefs(context)?.edit()?.putInt(KEY_CHALLENGE_DAYS, value)?.apply()
    }

    /** Meydan okuma ödülü alındıysa alınan gün sayısı; alınmadıysa 0. */
    fun challengeClaimed(context: Context): Int =
        prefs(context)?.getInt(KEY_CHALLENGE_CLAIMED, 0) ?: 0

    /**
     * Ödülün alındığı gün (`yyyy-MM-dd`); alınmadıysa boş.
     *
     * Kartın ne zaman kaybolacağını belirliyor: alındığı günün sonuna kadar "TAMAMLANDI"
     * olarak duruyor, ertesi gün gidiyor. Cihaz değiştiren kullanıcıda boş kalıyor ve kart
     * hemen gizleniyor — doğru olan da bu: yeni cihazda kutlanacak taze bir şey yok.
     */
    fun challengeClaimedDay(context: Context): String =
        prefs(context)?.getString(KEY_CHALLENGE_CLAIMED_DAY, "").orEmpty()

    fun markChallengeClaimed(context: Context, days: Int) {
        prefs(context)?.edit()
            ?.putInt(KEY_CHALLENGE_CLAIMED, days)
            ?.putString(KEY_CHALLENGE_CLAIMED_DAY, StudyTimeTracker.dayId())
            ?.apply()
    }

    /**
     * Ders dönüşünde yeni seri sorusu sorulsun mu.
     *
     * İki şart: seri şu anda YOK ve bugün bu soru henüz sorulmadı. Günde bir kez, çünkü
     * her ders sonunda çıkan bir ekran ödül değil engel olurdu.
     *
     * Soruyu açan iki yol da buraya soruyor: haritadaki ders dönüşü
     * (`MainActivity.finalizeMapReturnAfterLessonClaim`) ve Görevler'deki kupa testi dönüşü
     * (`MainActivity.requestNewStreakPromptOnTasks`).
     */
    fun needsNewStreakPrompt(context: Context): Boolean {
        // Yalnızca debug derlemesinde ve anahtar elle açıldıysa; bkz. [NewStreakPromptDebug].
        if (NewStreakPromptDebug.forceShow) return true
        val p = prefs(context) ?: return false
        if (p.getInt(KEY_CURRENT, 0) > 0) return false
        return p.getString(KEY_NEW_STREAK_PROMPT_DAY, "") != StudyTimeTracker.dayId()
    }

    fun markNewStreakPromptShown(context: Context) {
        prefs(context)?.edit()
            ?.putString(KEY_NEW_STREAK_PROMPT_DAY, StudyTimeTracker.dayId())
            ?.apply()
    }

    /**
     * Seri kırıldıktan sonra seçilen yeni meydan okumayı yazar.
     *
     * Ödül işareti de siliniyor: yeni tur yeni ödül hakkı demek (sunucu da aynı kuralı
     * uyguluyor, bkz. `challengePatch`). Silinmeseydi kart "TAMAMLANDI ✓" diye açılır ve
     * çocuk yeni sözünün karşılığını göremezdi.
     *
     * Günlük bildirim işareti de siliniyor: sunucu bu seçimi ancak bir çağrıyla öğreniyor
     * ve günün bildirimi çoktan yapılmış olabilir. Silinmeseydi seçim yarına kalır, o
     * zamana kadar seri 2. güne geçer ve sunucu artık kabul etmezdi.
     */
    fun startNewChallenge(context: Context, days: Int) {
        val value = if (days in CHALLENGE_OPTIONS) days else DEFAULT_CHALLENGE_DAYS
        prefs(context)?.edit()
            ?.putInt(KEY_CHALLENGE_DAYS, value)
            ?.remove(KEY_CHALLENGE_CLAIMED)
            ?.remove(KEY_CHALLENGE_CLAIMED_DAY)
            ?.remove(KEY_LAST_PING_DAY)
            ?.apply()
    }

    // ── Seri dondurma ───────────────────────────────────────────────────
    //
    // Kaç dondurma olduğunu SUNUCU biliyor (altınla alınıyor, altın sunucuya özel); buradaki
    // sayı onun önbelleği. Yine de dondurma burada da harcanıyor: çocuk çevrimdışıyken
    // uygulamayı açtığında serisinin kırık görünmemesi gerekiyor. İki taraf aynı kuralı
    // ([StreakFreezeRules.settle] ve sunucuda `settleStreakFreeze`) aynı güne göre işlettiği
    // için eşitlemede aynı sonuca varıyorlar.

    /** Elde harcanmamış dondurma sayısı (şimdilik 0 ya da 1). */
    fun freezesHeld(context: Context): Int = prefs(context)?.getInt(KEY_FREEZES, 0) ?: 0

    /**
     * Satın alma sunucuda tamamlandı; yerel önbelleğe de işlenir.
     *
     * Bir sonraki sunucu okumasını beklemeden: mağaza kartı "Hazır"a hemen dönmeli ve çocuk
     * o gün çevrimdışı kalsa bile dondurması geçerli olmalı.
     */
    fun markFreezePurchased(context: Context, freezes: Int, freezeDay: String) {
        prefs(context)?.edit()
            ?.putInt(KEY_FREEZES, freezes.coerceAtLeast(0))
            ?.putString(KEY_FREEZE_DAY, freezeDay)
            ?.apply()
        StreakDiag.log("Repo.dondurma", "SATIN_ALINDI adet=$freezes gun=${freezeDay.ifEmpty { "(bos)" }}")
    }

    /** Dondurma harcandığında kullanıcıya bir kez söylenecek şey. */
    enum class FreezeNotice {
        /** Kaçan gün kapandı, seri yaşıyor. */
        SAVED,

        /** Kaçan ilk gün kapandı ama arkasından bir gün daha kaçtı; seri yine kırıldı. */
        LOST,
    }

    /**
     * Bekleyen dondurma bildirimini döner ve siler; yoksa ya da bayatsa null.
     *
     * Bayat olan gösterilmiyor: dondurmanın harcandığı gün uygulama arka planda açılıp
     * kapanmış olabilir ve "dün serin kurtuldu" cümlesi üç gün sonra artık doğru değil.
     */
    fun takeFreezeNotice(context: Context): FreezeNotice? {
        val p = prefs(context) ?: return null
        val raw = p.getString(KEY_FREEZE_NOTICE, "").orEmpty()
        if (raw.isEmpty()) return null
        p.edit().remove(KEY_FREEZE_NOTICE).apply()
        val (kind, day) = raw.split("|").let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }
        if (day != StudyTimeTracker.dayId()) return null
        return FreezeNotice.values().firstOrNull { it.name == kind }
    }

    /**
     * Kaçan günü eldeki dondurmayla kapatır; kapattıysa serinin yeni son gününü döner.
     *
     * Serinin kendisini (kaç gün olduğunu) değiştirmiyor, kırılmayı da yazmıyor: yalnızca
     * son günü kaçan güne çekiyor. Devamını [refresh] içindeki mevcut dallar hallediyor —
     * köprüden sonra son gün dün ise seri yaşıyor, değilse aynı çağrıda kırılıyor.
     *
     * Son gün ile dondurma alanları TEK yazımda gidiyor: ayrı yazılsaydı araya giren bir
     * kapanma dondurmayı harcayıp köprüyü yazmadan bırakabilirdi (dondurma gitti, seri de).
     */
    private fun applyFreeze(context: Context, current: Int, lastDay: String): String? {
        val p = prefs(context) ?: return null
        val freezes = p.getInt(KEY_FREEZES, 0)
        if (freezes <= 0) return null
        val settled = StreakFreezeRules.settle(
            current = current,
            lastDay = lastDay,
            freezes = freezes,
            freezeDay = p.getString(KEY_FREEZE_DAY, "").orEmpty(),
            scanDays = FREEZE_SCAN_DAYS,
        ) { StudyTimeTracker.dayId(it) } ?: return null

        val saved = settled.missedDay == StudyTimeTracker.dayId(-1)
        val notice = if (saved) FreezeNotice.SAVED else FreezeNotice.LOST
        p.edit()
            .putInt(KEY_FREEZES, freezes - 1)
            .remove(KEY_FREEZE_DAY)
            .putString(KEY_LAST_DAY, settled.missedDay)
            .putString(KEY_FROZEN_DAYS, recentOnly(readFrozenDays(context) + settled.missedDay))
            .putString(KEY_FREEZE_NOTICE, "${notice.name}|${StudyTimeTracker.dayId()}")
            .apply()
        StreakDiag.log(
            "Repo.dondurma",
            "HARCANDI kapatilanGun=${settled.missedDay} eskiLastDay=$lastDay current=$current " +
                "seriKurtuldu=$saved kalanAdet=${freezes - 1}",
        )
        AnalyticsLogger.logStreakFreezeUsed(current, saved)
        return settled.missedDay
    }

    private fun readFrozenDays(context: Context): Set<String> =
        prefs(context)?.getString(KEY_FROZEN_DAYS, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .toSet()

    /** Hafta şeridinin arşiv penceresine sığan günler, saklanacak biçimde. */
    private fun recentOnly(days: Set<String>): String {
        val cutoff = (0 until ACHIEVED_HISTORY_DAYS).map { StudyTimeTracker.dayId(-it) }.toSet()
        return days.intersect(cutoff).sorted().joinToString(",")
    }

    // ── Kutlama kuyruğu ─────────────────────────────────────────────────
    //
    // Hedef ders ekranındayken doluyor. Kutlamayı oracıkta göstermek dersin ortasına
    // dalmak olurdu; bu yüzden kuyruğa alınıp güvenli bir ekrana dönüldüğünde gösteriliyor
    // (bkz. MainActivity.maybeShowStreakCelebration).
    //
    // Diskte tutuluyor: hedefi tutturup uygulamayı hemen kapatan kullanıcı kutlamayı bir
    // sonraki açılışta görür. Günü de saklanıyor ki üç gün sonra açan biri bayat bir
    // kutlamayla karşılaşmasın.

    private fun queueCelebration(context: Context, streak: Int, day: String) {
        prefs(context)?.edit()
            ?.putString(KEY_CELEBRATION_DAY, day)
            ?.putInt(KEY_CELEBRATION_STREAK, streak)
            ?.apply()
    }

    /** Bekleyen kutlamanın seri değeri; yoksa ya da bayatsa 0. Okumak temizlemez. */
    fun pendingCelebration(context: Context): Int {
        val p = prefs(context) ?: return 0
        if (p.getString(KEY_CELEBRATION_DAY, "") != StudyTimeTracker.dayId()) return 0
        return p.getInt(KEY_CELEBRATION_STREAK, 0)
    }

    // ── Hesap sahipliği ─────────────────────────────────────────────────

    /**
     * Yerel seri verisini oturumdaki hesaba bağlar; sahibi değiştiyse her şeyi siler.
     *
     * ## Neden gerekli
     * Bu dosya cihaza ait, uid'ye değil. Kullanıcı çıkış yapıp aynı telefonda başka bir hesap
     * açtığında yeni hesap öncekinin serisini, hedefini, hafta şeridini ve toplanan ödül
     * önbelleğini devralıyordu. Sunucudan okuma da kurtarmıyordu: yeni hesabın dokümanı
     * olmadığı için okuma sessizce geri dönüyor ve yerel veri olduğu gibi kalıyordu.
     *
     * ## Neden "sahipsizse devral"
     * Seri kayıttan ÖNCE, ilk ders sırasında başlıyor; o anda uid yok, dolayısıyla sahip
     * alanı boş. Kayıt biter bitmez o veri az önce açılan hesabın hakkı — silinmemeli.
     * Silinen yalnızca BAŞKA bir hesaba ait olduğu kesin olan veri.
     *
     * Çalışma süresi de siliniyor: o da güne göre anahtarlı ve uid'den bağımsız, yani
     * temizlenmeseydi yeni kullanıcı öncekinin dakikalarıyla seri ilerletirdi.
     */
    fun bindToUser(context: Context, uid: String?) {
        if (uid.isNullOrBlank()) return
        val p = prefs(context) ?: return
        val owner = p.getString(KEY_OWNER_UID, "").orEmpty()
        if (owner == uid) return

        if (owner.isNotEmpty()) {
            // Bu yol bugünün saniyelerini de siliyor ([wipe] → StudyTimeTracker.clearAll).
            // "Hedefi tutturdum ama sayaç 0" tablosunun en güçlü açıklaması bu ve
            // StreakDiag akışında hiç görünmüyordu: uid bir sebeple oynarsa (çıkış/giriş,
            // ikinci hesap) sayaç her tazelemede sıfırlanır.
            StreakDiag.log(
                "Repo.sahip",
                "SILINIYOR eskiSahip=${owner.take(8)} yeniSahip=${uid.take(8)} " +
                    "(seri + çalışma süresi sıfırlanıyor)",
            )
            Log.i(TAG, "Hesap değişti, yerel seri verisi siliniyor")
            wipe(context)
        } else {
            StreakDiag.log("Repo.sahip", "SAHIPSIZ_DEVRALINDI yeniSahip=${uid.take(8)} (silme yok)")
        }
        prefs(context)?.edit()?.putString(KEY_OWNER_UID, uid)?.apply()
    }

    /**
     * Kayıt akışına girildi: veri başka bir hesaba aitse şimdi siliniyor.
     *
     * [bindToUser] tek başına yetmiyordu, çünkü o ancak KAYIT BİTTİKTEN sonra, kullanıcı
     * MainActivity'ye geldiğinde çalışıyor. Oysa kurulum akışının gösterilip
     * gösterilmeyeceğine kayıt SIRASINDA karar veriliyor ve o anda `isOnboardingDone`
     * bayrağı hâlâ önceki kullanıcıdan kalma "true" oluyordu — yani aynı cihazda ikinci
     * hesap açan kimseye sorular yine sorulmuyordu.
     *
     * Sahip boşsa dokunulmuyor: o veri, kayıttan önce ilk derste kazanılmış demektir ve az
     * önce açılmakta olan hesabın hakkıdır.
     */
    fun prepareForNewAccount(context: Context) {
        val p = prefs(context) ?: return
        if (p.getString(KEY_OWNER_UID, "").orEmpty().isEmpty()) return
        Log.i(TAG, "Yeni hesap kaydı, önceki hesabın seri verisi siliniyor")
        wipe(context)
    }

    private fun wipe(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
        StudyTimeTracker.clearAll(context)
    }

    // ── Sunucu eşitlemesi ───────────────────────────────────────────────
    //
    // Yerel sayaç arayüzün hızlı yolu: kayıttan önce de, çevrimdışı da çalışıyor. Ama ÖDÜL
    // kararları yalnızca sunucudan gelen değere bakıyor ([serverCurrent]) — yerel sayaç
    // cihazdaki bir dosya, ödül dağıtan bir sayı olamaz.
    //
    // Tutturulan günler kuyruğa yazılıyor ve bağlantı geldiğinde toplu gönderiliyor; sunucu
    // her günü tek tek ve ardışıklık şartıyla işlediği için toplu göndermek avantaj değil.

    private fun queueSyncDay(context: Context, day: String) {
        val days = (pendingSyncDays(context) + day).distinct().sorted().takeLast(MAX_PENDING_SYNC_DAYS)
        prefs(context)?.edit()?.putString(KEY_PENDING_SYNC_DAYS, days.joinToString(","))?.apply()
        StreakDiag.log("Repo.kuyruk", "KUYRUGA_EKLENDI gun=$day kuyruk=$days")
    }

    /**
     * Gönderilmeyi bekleyen günler.
     *
     * Eskiyenler burada eleniyor: sunucu bir haftadan eski günleri seriye işlemiyor ve hepsi
     * elenirse çağrıyı hata ile reddediyor. Elenmeselerdi kuyruk asla kabul edilmeyen
     * günlerle dolu kalır ve her ekran değişiminde başarısız bir çağrı denenirdi.
     */
    fun pendingSyncDays(context: Context): List<String> {
        val recent = (0..MAX_PENDING_SYNC_DAYS).map { StudyTimeTracker.dayId(-it) }.toSet()
        return prefs(context)?.getString(KEY_PENDING_SYNC_DAYS, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() && it in recent }
            .sorted()
    }

    /**
     * Sunucu [sentDays]'i kabul etti; kuyruktan yalnızca onlar siliniyor.
     *
     * Gönderim sırasında yeni bir gün kuyruğa girmiş olabilir (gece yarısını geçen uzun bir
     * oturum); kuyruğu tamamen temizlemek o günü kaybederdi.
     */
    fun onSyncAccepted(
        context: Context,
        sentDays: List<String>,
        current: Int,
        longest: Int,
        claimed: Set<Int>,
    ) {
        val remaining = pendingSyncDays(context) - sentDays.toSet()
        StreakDiag.log(
            "Repo.kuyruk",
            "KABUL_EDILDI gonderilen=$sentDays kalan=$remaining " +
                "sunucuCurrent=$current sunucuLongest=$longest claimed=${claimed.sorted()}",
        )
        prefs(context)?.edit()
            ?.putString(KEY_PENDING_SYNC_DAYS, remaining.joinToString(","))
            ?.putInt(KEY_SERVER_CURRENT, current)
            ?.putInt(KEY_SERVER_LONGEST, longest)
            ?.putString(KEY_SERVER_CLAIMED, claimed.sorted().joinToString(","))
            ?.apply()
    }

    /**
     * Sunucudan okunan durumu yerel duruma işler.
     *
     * ## Neden gerekli
     * Eşitleme tek yönlü olsaydı (yalnızca istemci → sunucu) senkronun asıl amacı
     * karşılanmazdı: yeni bir telefona kurulum yapan kullanıcının otuz günlük serisi
     * cihazda olmadığı için sıfırdan başlardı. Ödül satırları da sunucunun bildiğini
     * göremezdi.
     *
     * ## Neden yalnızca "sunucu ilerideyse"
     * Yerel sayaç meşru olarak önde olabilir: bugün tutturuldu ama henüz bildirilmedi.
     * O durumda sunucunun eski değerini yazmak, kullanıcının bugününü silmek olurdu.
     *
     * [lastDay] de birlikte alınıyor — alınmasaydı seri 30'a yükselir ama son gün boş
     * kalırdı ve bir sonraki [refresh] "ardışık değil" deyip seriyi 1'e düşürürdü.
     *
     * ## Seri dondurma
     * Dondurma sayısının sahibi sunucu, o yüzden [freezes] her zaman yerelin yerine geçiyor.
     * Ama sunucunun durumu "bugüne göre kapatılmamış" olabilir: dondurmayı gün kaçtığı anda
     * değil, bir sonraki çağrıda harcıyor ve bu okuma o çağrıdan önce gelmiş olabilir. Ham
     * değer olduğu gibi alınsaydı az önce yerelde harcanan dondurma ekrana geri gelirdi.
     * Bu yüzden sunucunun değerlerine burada aynı kural uygulanıyor, sonra alınıyor.
     *
     * @param freezes Sunucudaki dondurma sayısı; yanıtta yoksa [FREEZES_UNKNOWN].
     */
    fun adoptServerState(
        context: Context,
        current: Int,
        longest: Int,
        lastDay: String,
        claimed: Set<Int>,
        recentDays: Set<String> = emptySet(),
        goalMinutes: Int = 0,
        challengeDays: Int = 0,
        challengeClaimed: Int = 0,
        freezes: Int = FREEZES_UNKNOWN,
        freezeDay: String = "",
        frozenDays: Set<String> = emptySet(),
    ) {
        val p = prefs(context) ?: return

        // Önce yerel taraf kapatılıyor. Bu fonksiyon günün ilk [refresh]'inden ÖNCE
        // çalışabiliyor (açılıştaki okuma erken dönerse); o zaman aşağıda sunucudan gelen
        // "dondurma harcandı" bilgisi yazılır ama yerel son gün köprülenmemiş kalır ve bir
        // sonraki [refresh] seriyi kırardı — dondurma gitmiş, seri de.
        applyFreeze(context, p.getInt(KEY_CURRENT, 0), p.getString(KEY_LAST_DAY, "").orEmpty())

        var serverLastDay = lastDay
        var serverFreezes = freezes
        var serverFreezeDay = freezeDay
        var serverFrozen = frozenDays
        if (freezes > 0) {
            StreakFreezeRules.settle(
                current = current,
                lastDay = lastDay,
                freezes = freezes,
                freezeDay = freezeDay,
                scanDays = FREEZE_SCAN_DAYS,
            ) { StudyTimeTracker.dayId(it) }?.let { settled ->
                serverLastDay = settled.missedDay
                serverFreezes = freezes - 1
                serverFreezeDay = ""
                serverFrozen = frozenDays + settled.missedDay
            }
        }

        // Hedef ve meydan okuma YALNIZCA bu cihazda kurulum akışı hiç görülmediyse
        // sunucudan alınıyor. Bu, "hesabı var, yeni cihaza kurulum yaptı" durumu: kayıt
        // akışından geçmediği için hedefi sorulmuyor, sunucudaki seçimi geri geliyor.
        //
        // Alındıktan sonra akış yapılmış sayılıyor. Bu bayrak olmasaydı kullanıcının seri
        // ekranından yaptığı değişiklik, bir sonraki okumada sunucunun eski değeriyle
        // ezilirdi — değişiklik ancak ertesi gün sunucuya gidiyor.
        if (!isOnboardingDone(context) && goalMinutes > 0) {
            p.edit().putInt(KEY_GOAL_MINUTES, goalMinutes).apply()
            markOnboardingDone(context)
        }
        if (challengeClaimed > 0) {
            p.edit().putInt(KEY_CHALLENGE_CLAIMED, challengeClaimed).apply()
        } else if (current <= 1) {
            // Sunucu ödül hakkını iade etti: yeni tur başladı. Yerel işaret silinmeseydi
            // kart "TAMAMLANDI ✓" diye açılıp yeni turun ödülünü gizlerdi.
            //
            // Şart current <= 1: ödül en az 3 günlük seri istiyor, yani 0 gelen bir yanıt
            // uzun bir seride ancak BAYAT olabilir (toplama anından önce yola çıkmış bir
            // okuma). O yanıtın yerel işareti silmesi, az önce toplanmış ödülün düğmesini
            // geri getirirdi.
            p.edit().remove(KEY_CHALLENGE_CLAIMED).remove(KEY_CHALLENGE_CLAIMED_DAY).apply()
        }
        // Sunucuda kayıtlı bir meydan okuma varsa yerel de ONA uyuyor. Sunucu bu değeri bir
        // kez yazıp bir daha değiştirmiyor, yani tek doğru kaynak o: cihaz değiştiren
        // kullanıcıya sözü geri geliyor, yerelde bir sapma olduysa da kendiliğinden düzeliyor.
        // Sunucunun değeri yokken (kayıt bitti, ilk gün henüz bildirilmedi) yerel korunuyor.
        if (challengeDays > 0) {
            p.edit().putInt(KEY_CHALLENGE_DAYS, challengeDays).apply()
        }
        val editor = p.edit()
            .putInt(KEY_SERVER_CURRENT, current)
            .putInt(KEY_SERVER_LONGEST, longest)
            .putString(KEY_SERVER_CLAIMED, claimed.sorted().joinToString(","))

        // Sunucunun son günü dünden eskiyse o seri ZATEN kırılmış. Yerel sayaca almak
        // düzeltmiyor, döngü üretiyordu: al → [refresh] kırıldı der → 0 yaz → bir sonraki
        // okumada yine al… Her turda bir `streak_broken` olayı gidiyordu. Bayat durum
        // yalnızca önbelleğe yazılıyor (ödül satırları için), yerel sayaca değil.
        // Pencere üç gün: dün, bugün ve YARIN. Yarın da dahil, çünkü sunucu gün kimliğini
        // ±1 gün toleransla kabul ediyor — başka bir saat diliminden (ya da saati ileri
        // alınmış bir cihazdan) bildirilen gün, bu cihazın bugününden bir gün ileride
        // olabilir. Dar pencere o durumda seriyi geri yüklemiyor ve kullanıcı ödülü
        // toplayabildiği halde üst barda 1 görüyordu.
        val fresh = setOf(
            StudyTimeTracker.dayId(1),
            StudyTimeTracker.dayId(),
            StudyTimeTracker.dayId(-1),
        )
        // Tazelik, dondurma uygulandıktan SONRAKİ son güne göre: dün kaçmış ama dondurmayla
        // kapanmış bir seri yaşıyor ve yeni cihaza geri yüklenmeli.
        val serverFresh = serverLastDay in fresh
        val localCurrent = p.getInt(KEY_CURRENT, 0)
        StreakDiag.log(
            "Repo.sunucuDurumu",
            "sunucuCurrent=$current yerelCurrent=$localCurrent " +
                "sunucuLastDay=${lastDay.ifEmpty { "(bos)" }} " +
                (if (serverLastDay != lastDay) "dondurmaIle=$serverLastDay " else "") +
                "taze=$serverFresh dondurma=$serverFreezes -> " +
                (if (current > localCurrent && serverFresh) "BENIMSENDI" else "yerel_korundu"),
        )
        if (current > localCurrent && serverFresh) {
            editor.putInt(KEY_CURRENT, current).putString(KEY_LAST_DAY, serverLastDay)
        }
        if (freezes != FREEZES_UNKNOWN) {
            editor.putInt(KEY_FREEZES, serverFreezes.coerceAtLeast(0))
            if (serverFreezes > 0 && serverFreezeDay.isNotEmpty()) {
                editor.putString(KEY_FREEZE_DAY, serverFreezeDay)
            } else {
                editor.remove(KEY_FREEZE_DAY)
            }
            // Donmuş günler de tutturulmuş günler gibi BİRLEŞTİRİLİYOR: yerelde harcanmış
            // ama henüz sunucuya ulaşmamış bir gün olabilir.
            editor.putString(KEY_FROZEN_DAYS, recentOnly(readFrozenDays(context) + serverFrozen))
        }
        // Rekor, güncel seriden küçük olamaz. Sunucudan 30 günlük bir seri geri yüklenip
        // rekor 2'de kalsaydı ekran "şu an 30 gün, en uzun 7 gün" gibi kendi kendisiyle
        // çelişirdi.
        val adoptedCurrent = if (current > localCurrent && serverFresh) current else 0
        val targetLongest = maxOf(longest, adoptedCurrent)
        val localLongest = p.getInt(KEY_LONGEST, 0)
        if (targetLongest > localLongest) editor.putInt(KEY_LONGEST, targetLongest)

        // Hafta şeridi: sunucunun bildiği günler yerel arşivle BİRLEŞTİRİLİYOR, onun yerine
        // geçmiyor. Yerelde bugün tutturulmuş ama henüz bildirilmemiş olabilir; sunucunun
        // listesini olduğu gibi yazmak o günü şeritten silerdi.
        if (recentDays.isNotEmpty()) {
            val cutoff = (0 until ACHIEVED_HISTORY_DAYS).map { StudyTimeTracker.dayId(-it) }.toSet()
            val merged = (readAchievedDays(context) + recentDays).intersect(cutoff)
            editor.putString(KEY_ACHIEVED_DAYS, merged.sorted().joinToString(","))
        }

        editor.apply()
    }

    /**
     * Bugün sunucuya "buradayım" denmiş mi.
     *
     * Akşam hatırlatması kullanıcının saat dilimini ve son görülme zamanını bilmek zorunda;
     * ikisi de yalnızca gün bildirimiyle güncellenseydi, hedefini hiç tutturmayan kullanıcı
     * — yani hatırlatmaya en çok ihtiyacı olan kişi — hiç kaydedilmezdi.
     *
     * Günde bir kez: bildirim saati gün içinde değişmiyor.
     */
    fun needsDailyPing(context: Context): Boolean =
        prefs(context)?.getString(KEY_LAST_PING_DAY, "") != StudyTimeTracker.dayId()

    fun markDailyPing(context: Context) {
        prefs(context)?.edit()?.putString(KEY_LAST_PING_DAY, StudyTimeTracker.dayId())?.apply()
    }

    /** Sunucunun bildiği seri. Ödül satırları buna bakıyor; hiç eşitlenmediyse 0. */
    fun serverCurrent(context: Context): Int = prefs(context)?.getInt(KEY_SERVER_CURRENT, 0) ?: 0

    fun serverLongest(context: Context): Int = prefs(context)?.getInt(KEY_SERVER_LONGEST, 0) ?: 0

    fun claimedMilestones(context: Context): Set<Int> =
        prefs(context)?.getString(KEY_SERVER_CLAIMED, "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

    /**
     * Toplanan taşı yerel önbelleğe de işler.
     *
     * Sunucu zaten yazdı; buradaki kayıt yalnızca ekranın bir sonraki eşitlemeyi beklemeden
     * doğru görünmesi için.
     */
    fun markMilestoneClaimed(context: Context, milestone: Int) {
        val updated = claimedMilestones(context) + milestone
        prefs(context)?.edit()
            ?.putString(KEY_SERVER_CLAIMED, updated.sorted().joinToString(","))
            ?.apply()
    }

    fun clearPendingCelebration(context: Context) {
        prefs(context)?.edit()
            ?.remove(KEY_CELEBRATION_DAY)
            ?.remove(KEY_CELEBRATION_STREAK)
            ?.apply()
    }

    /**
     * Seri kurulum akışı (hedef + meydan okuma) tamamlandı mı.
     *
     * Akış ilk dersten sonra, KAYITTAN ÖNCE açıldığı için kullanıcının uid'i yok; bayrak
     * bu yüzden cihazda. Kullanıcı uygulamayı silip kurarsa akışı yeniden görür — zararsız,
     * çünkü hedefini yeniden seçmiş olur.
     */
    fun isOnboardingDone(context: Context): Boolean =
        prefs(context)?.getBoolean(KEY_ONBOARDING_DONE, false) ?: false

    fun markOnboardingDone(context: Context) {
        prefs(context)?.edit()?.putBoolean(KEY_ONBOARDING_DONE, true)?.apply()
    }

    // ── Seri ────────────────────────────────────────────────────────────

    /**
     * Seriyi güncel süreye göre tazeler ve son durumu döner.
     *
     * Aynı gün içinde kaç kez çağrıldığı önemsiz: gün bir kez ilerletiliyor. Ekran her
     * açıldığında ve süre değiştikçe çağrılabilir.
     */
    fun refresh(context: Context): StreakState {
        val p = prefs(context)
        val goal = goalMinutes(context)
        val challenge = chosenChallengeDays(context)
        val seconds = StudyTimeTracker.secondsToday(context)
        val today = StudyTimeTracker.dayId()
        val yesterday = StudyTimeTracker.dayId(-1)

        var current = p?.getInt(KEY_CURRENT, 0) ?: 0
        var longest = p?.getInt(KEY_LONGEST, 0) ?: 0
        var lastDay = p?.getString(KEY_LAST_DAY, "").orEmpty()
        var achieved = readAchievedDays(context)

        // ── İmkânsız derecede ileri bir son gün: ONURLANDIRILMAZ, ATILIR ──
        //
        // Aşağıdaki [lastDayInFuture] koruması saat dilimi payı için var ve payın meşru
        // sınırı BİR GÜN: sunucu da gün kimliğini yalnızca ±1 günle kabul ediyor
        // (`STREAK_DAY_TOLERANCE_DAYS = 1`). Bir günden fazla ileride bir son gün saat
        // diliminden gelemez; bozuk veridir — cihaz saati ileri alınmış (seriyi elle test
        // ederken tipik), bozuk bir yedek geri yüklenmiş ya da takvim geri sarmış.
        //
        // Onurlandırıldığında seri O GÜNE KADAR donuyordu ve bu sessiz bir donmaydı:
        // ilerleme dalı da kırılma dalı da `!lastDayInFuture` istiyor, yani hedef
        // tutturulsa bile ne sayılıyor ne "serin kırıldı" deniyor. Gerçekten yaşandı:
        // `last_goal_day=2026-10-05` kalmış bir cihazda 30.09 (438 sn) ve 01.10 (334 sn)
        // hedefin üstünde olduğu halde seri 0 gösterdi ve kendi kendine düzelmedi.
        //
        // Atılan değerin yerine bugünden yeni bir seri kurulmasına izin veriliyor: elde
        // güvenilir bir son gün yok, en doğrusu temiz sayfa. Sunucu da aynı sonuca varıyor
        // (ardışık olmayan gün serisi 1'e çeker), yani iki taraf çelişmiyor.
        val tomorrow = StudyTimeTracker.dayId(1)
        if (lastDay > tomorrow) {
            StreakDiag.log(
                "Repo.refresh",
                "BOZUK_LASTDAY_ATILDI lastDay=$lastDay (en fazla $tomorrow olabilirdi) " +
                    "current=$current->0 — bugünden yeni seri kurulabilir",
            )
            Log.w(
                TAG,
                "lastDay imkansiz derecede ileride ($lastDay > $tomorrow); " +
                    "bozuk veri atiliyor, seri bugunden yeniden kurulacak",
            )
            lastDay = ""
            current = 0
            // Gelecek tarihli günler hafta şeridinden de düşüyor. [writeState] zaten
            // kesişim alıyor ama bu fonksiyonun DÖNDÜRDÜĞÜ durum bellekteki kümeyi
            // kullanıyor; filtrelenmezse şeritte gelecekte bir tik görünürdü.
            achieved = achieved.filterTo(mutableSetOf()) { it <= today }
            writeState(context, current, longest, lastDay, achieved)
        }

        // Son gün bugünden İLERİDEYSE o gün zaten sayılmış demektir: ne ilerletilir ne
        // kırılır, takvim yetişene kadar olduğu gibi durur.
        //
        // İleri bir son gün uydurma bir durum değil. Sunucu gün kimliğini ±1 gün toleransla
        // kabul ediyor, yani doğu saat dilimindeki bir kullanıcının kaydı bu cihazın
        // bugününden ileride olabilir; batıya uçan biri de aynı duruma düşer. Bu kontrol
        // olmadan "ardışık değil" denip seri 1'e düşüyordu — sunucudan geri yüklenen seri de
        // benimsendiği anda aynı şekilde siliniyordu.
        //
        // yyyy-MM-dd biçiminde sözlük sırası tarih sırasıyla aynı, ayrıştırmaya gerek yok.
        val lastDayInFuture = lastDay > today

        // ── Seri dondurma ──
        //
        // İlerleme ve kırılma dallarından ÖNCE: kaçan gün köprülenince aşağıdaki iki dal
        // hiç değişmeden doğru çalışıyor. Dün kaçtıysa son gün dün olur — bugün tutturulursa
        // seri +1, tutturulmazsa yaşamaya devam eder. İki gün kaçtıysa son gün hâlâ dünden
        // eskidir ve kırılma dalı aynı çağrıda çalışır (dondurma yine harcanmış olur).
        //
        // [lastDayInFuture] yeniden hesaplanmıyor: köprülenen gün en geç dün olabilir.
        applyFreeze(context, current, lastDay)?.let { lastDay = it }

        StreakDiag.log(
            "Repo.refresh",
            "bugun=$today dun=$yesterday sure=${seconds}sn hedef=${goal * 60}sn " +
                "yerelCurrent=$current longest=$longest lastDay=${lastDay.ifEmpty { "(bos)" }} " +
                "ileride=$lastDayInFuture sunucuCurrent=${serverCurrent(context)} " +
                "challengeDays=$challenge kuyruk=${pendingSyncDays(context)}",
        )

        if (seconds >= goal * 60 && lastDay != today && !lastDayInFuture) {
            // Dün de tutturulmuşsa seri devam eder, yoksa bugünden yeniden başlar.
            current = if (lastDay == yesterday) current + 1 else 1
            longest = maxOf(longest, current)
            lastDay = today
            achieved = achieved + today
            writeState(context, current, longest, lastDay, achieved)

            // Bu dal günde yalnızca bir kez çalışıyor (koşuldaki `lastDay != today` onu
            // garanti ediyor), yani hem olay hem kutlama tam olarak bir kez tetikleniyor.
            StreakDiag.log(
                "Repo.refresh",
                "GUN_TUTTURULDU current=$current lastDay=$lastDay (yazildi)",
            )
            AnalyticsLogger.logStreakDayDone(current, goal)
            queueCelebration(context, current, today)
            logChallengeDoneOnce(context, current, challenge)
            // Ödüller sunucudaki sayaca bakıyor; gün oraya da bildirilmeli. Kuyruğa
            // alınıyor çünkü tam o anda internet olmayabilir.
            queueSyncDay(context, today)
        } else {
            StreakDiag.log(
                "Repo.refresh",
                "GUN_ISLENMEDI neden=" + (
                    when {
                        seconds < goal * 60 -> "sure_yetersiz (${seconds}/${goal * 60}sn)"
                        lastDay == today -> "bugun_zaten_sayilmis"
                        else -> "lastDay_ileride ($lastDay > $today)"
                    }
                    ),
            )
        }

        // ── Kırılma ──
        //
        // Kullanıcı uygulamayı hiç açmadan da seriyi kırabilir; kırıldığı anda çalışan bir
        // kodumuz yok. Bu yüzden kırılma OKUNURKEN hesaplanıyor.
        //
        // Ama artık hesaplayıp geçmiyoruz, YAZIYORUZ: yazılmasaydı `streak_broken` olayı
        // ekran her tazelendiğinde tekrar gönderilirdi ve kaç serinin kırıldığı değil kaç kez
        // ekrana bakıldığı ölçülürdü.
        if (current > 0 && lastDay.isNotEmpty() && !lastDayInFuture &&
            lastDay != today && lastDay != yesterday
        ) {
            StreakDiag.log(
                "Repo.refresh",
                "KIRILDI current=$current->0 lastDay=$lastDay gecenGun=${daysSince(lastDay)}",
            )
            AnalyticsLogger.logStreakBroken(current, daysSince(lastDay))
            current = 0
            writeState(context, current, longest, lastDay, achieved)
        }

        val frozen = readFrozenDays(context)
        return StreakState(
            current = current,
            longest = longest,
            goalMinutes = goal,
            secondsToday = seconds,
            achievedDays = achieved + daysOfStreak(current, lastDay, frozen),
            frozenDays = frozen,
            freezeHeld = freezesHeld(context) > 0,
        )
    }

    /**
     * Serinin kapsadığı günler.
     *
     * ## Neden türetiliyor
     * Hafta şeridi önce yalnızca KAYDEDİLMİŞ günlere bakıyordu ve bu, serinin kendisiyle
     * çelişebiliyordu: alev "30 gün" derken şeritte tek bir gün işaretli olmuyordu. Kaydın
     * eksik kalması normal — cihaz değiştiren kullanıcının cihazında hiç kayıt yok, bu
     * güncellemeden önce seri tutmuş kullanıcıların sunucusunda da yok.
     *
     * Oysa kayda gerek yok: N günlük bir seri, [lastDay]'de biten N günün tutturulduğu
     * ANLAMINA GELİR. Seri zaten o günlerin kanıtı.
     *
     * Kaydedilmiş günler yine de kullanılıyor (ikisinin birleşimi alınıyor): seri kırılmadan
     * önce tutturulan günler seriye dahil değil ama aynı takvim haftasında olabilirler ve
     * şeritte görünmeleri gerekir.
     *
     * ## Neden tarih ayrıştırılmıyor
     * [StudyTimeTracker.dayId] geriye doğru taranıp [lastDay]'in bugüne göre kaydırması
     * bulunuyor. Gün kimliğini üreten kodun aynısı hesabı da yapıyor, yani ikinci bir tarih
     * biçimi yorumu ve onun hata payı hiç doğmuyor.
     *
     * ## Donmuş günler
     * Seri dondurmanın kapattığı gün seriye dahil DEĞİL ama serinin ortasında duruyor (son
     * günün kendisi bile olabilir). "N gün geriye say" hesabı onu atlamak zorunda: atlamasaydı
     * donmuş gün tutturulmuş diye işaretlenir, serinin gerçek ilk günü de şeritten düşerdi.
     */
    private fun daysOfStreak(current: Int, lastDay: String, frozen: Set<String>): Set<String> {
        if (current <= 0 || lastDay.isEmpty()) return emptySet()
        // Son gün bugünden bir gün ileride olabilir (saat dilimi); tarama oradan başlıyor.
        val lastOffset = (1 downTo -ACHIEVED_HISTORY_DAYS)
            .firstOrNull { StudyTimeTracker.dayId(it) == lastDay }
            ?: return emptySet()
        val span = minOf(current, ACHIEVED_HISTORY_DAYS)
        // Bugünden sonrası işaretlenmiyor: son gün bir gün ileride olabiliyor ve gelecekteki
        // bir güne "tamamlandı" tiki koymak hatalı görünürdü. O gün takvim yetişince gelir.
        val today = StudyTimeTracker.dayId()
        val days = mutableSetOf<String>()
        var offset = lastOffset
        // Donmuş gün yokken tam [span] adım atılıyor, yani eski davranışla birebir aynı.
        var stepsLeft = span + frozen.size
        while (days.size < span && stepsLeft-- > 0) {
            val day = StudyTimeTracker.dayId(offset--)
            if (day !in frozen) days += day
        }
        return days.filterTo(mutableSetOf()) { it <= today }
    }

    /**
     * [dayId] üzerinden kaç gün geçtiği; 30'dan eskisi için 31.
     *
     * Tarih ayrıştırmak yerine [StudyTimeTracker.dayId] geriye doğru taranıyor: gün kimliğini
     * üreten kodun aynısı karşılaştırmayı da yapıyor, yani ikinci bir tarih biçimi yorumu
     * (ve onun hata payı) hiç doğmuyor.
     */
    private fun daysSince(dayId: String): Int {
        for (i in 1..30) if (StudyTimeTracker.dayId(-i) == dayId) return i
        return 31
    }

    /**
     * Meydan okuma tamamlandığında bir kez olay gönderir.
     *
     * Hangi değer için gönderildiği saklanıyor: kullanıcı 3 günü bitirip 7'ye yükseltirse
     * 7'yi bitirdiğinde ikinci kez gönderilmeli, ama 3'te kaldığı sürece bir daha
     * gönderilmemeli.
     */
    private fun logChallengeDoneOnce(context: Context, current: Int, challenge: Int) {
        if (challenge <= 0 || current < challenge) return
        val p = prefs(context) ?: return
        if (p.getInt(KEY_CHALLENGE_LOGGED, 0) == challenge) return
        p.edit().putInt(KEY_CHALLENGE_LOGGED, challenge).apply()
        AnalyticsLogger.logStreakChallengeDone(challenge)
    }

    private fun writeState(
        context: Context,
        current: Int,
        longest: Int,
        lastDay: String,
        achieved: Set<String>,
    ) {
        val cutoff = (0 until ACHIEVED_HISTORY_DAYS).map { StudyTimeTracker.dayId(-it) }.toSet()
        prefs(context)?.edit()
            ?.putInt(KEY_CURRENT, current)
            ?.putInt(KEY_LONGEST, longest)
            ?.putString(KEY_LAST_DAY, lastDay)
            ?.putString(KEY_ACHIEVED_DAYS, achieved.intersect(cutoff).sorted().joinToString(","))
            ?.apply()
    }

    private fun readAchievedDays(context: Context): Set<String> =
        prefs(context)?.getString(KEY_ACHIEVED_DAYS, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .toSet()

    private fun prefs(context: Context) = try {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    } catch (e: Throwable) {
        Log.w(TAG, "SharedPreferences açılamadı", e)
        null
    }

    /** Yalnızca hata ayıklama/test için. */
    fun resetForDebug(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
    }
}
