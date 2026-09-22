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

    /** Seri ekranında gösterilecek durum. */
    data class StreakState(
        /** Güncel seri; kırıldıysa 0. */
        val current: Int,
        val longest: Int,
        val goalMinutes: Int,
        val challengeDays: Int,
        val secondsToday: Int,
        /** Hedefi tutturulmuş günler (`yyyy-MM-dd`), hafta şeridi için. */
        val achievedDays: Set<String>,
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

    fun challengeDays(context: Context): Int =
        prefs(context)?.getInt(KEY_CHALLENGE_DAYS, DEFAULT_CHALLENGE_DAYS) ?: DEFAULT_CHALLENGE_DAYS

    fun setChallengeDays(context: Context, days: Int) {
        val value = if (days in CHALLENGE_OPTIONS) days else DEFAULT_CHALLENGE_DAYS
        prefs(context)?.edit()?.putInt(KEY_CHALLENGE_DAYS, value)?.apply()
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
        prefs(context)?.edit()
            ?.putString(KEY_PENDING_SYNC_DAYS, remaining.joinToString(","))
            ?.putInt(KEY_SERVER_CURRENT, current)
            ?.putInt(KEY_SERVER_LONGEST, longest)
            ?.putString(KEY_SERVER_CLAIMED, claimed.sorted().joinToString(","))
            ?.apply()
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
        val challenge = challengeDays(context)
        val seconds = StudyTimeTracker.secondsToday(context)
        val today = StudyTimeTracker.dayId()
        val yesterday = StudyTimeTracker.dayId(-1)

        var current = p?.getInt(KEY_CURRENT, 0) ?: 0
        var longest = p?.getInt(KEY_LONGEST, 0) ?: 0
        var lastDay = p?.getString(KEY_LAST_DAY, "").orEmpty()
        var achieved = readAchievedDays(context)

        if (seconds >= goal * 60 && lastDay != today) {
            // Dün de tutturulmuşsa seri devam eder, yoksa bugünden yeniden başlar.
            current = if (lastDay == yesterday) current + 1 else 1
            longest = maxOf(longest, current)
            lastDay = today
            achieved = achieved + today
            writeState(context, current, longest, lastDay, achieved)

            // Bu dal günde yalnızca bir kez çalışıyor (koşuldaki `lastDay != today` onu
            // garanti ediyor), yani hem olay hem kutlama tam olarak bir kez tetikleniyor.
            AnalyticsLogger.logStreakDayDone(current, goal)
            queueCelebration(context, current, today)
            logChallengeDoneOnce(context, current, challenge)
            // Ödüller sunucudaki sayaca bakıyor; gün oraya da bildirilmeli. Kuyruğa
            // alınıyor çünkü tam o anda internet olmayabilir.
            queueSyncDay(context, today)
        }

        // ── Kırılma ──
        //
        // Kullanıcı uygulamayı hiç açmadan da seriyi kırabilir; kırıldığı anda çalışan bir
        // kodumuz yok. Bu yüzden kırılma OKUNURKEN hesaplanıyor.
        //
        // Ama artık hesaplayıp geçmiyoruz, YAZIYORUZ: yazılmasaydı `streak_broken` olayı
        // ekran her tazelendiğinde tekrar gönderilirdi ve kaç serinin kırıldığı değil kaç kez
        // ekrana bakıldığı ölçülürdü.
        if (current > 0 && lastDay.isNotEmpty() && lastDay != today && lastDay != yesterday) {
            AnalyticsLogger.logStreakBroken(current, daysSince(lastDay))
            current = 0
            writeState(context, current, longest, lastDay, achieved)
        }

        return StreakState(
            current = current,
            longest = longest,
            goalMinutes = goal,
            challengeDays = challenge,
            secondsToday = seconds,
            achievedDays = achieved,
        )
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
