// Kaynak (Android): app/src/main/java/com/example/app/StreakRepository.kt — tamamı.
//
// Günlük seri: kullanıcının kaç gün üst üste günlük hedefini tutturduğu.
//
// ## Serinin kuralı
// Bir gün "tutturulmuş" sayılır: o gün ders ekranlarında geçen süre ([StudyTimeTracker.secondsToday])
// hedefe ulaşırsa VE yerel veri bir hesaba bağlıysa. Seri, tutturulan günler ARDIŞIK olduğu sürece
// büyür; bir gün atlanırsa sıfırlanır. Kırılma yazıldığı anda değil, OKUNDUĞU anda hesaplanır.
//
// ## Yerel ↔ sunucu
// Yerel sayaç arayüzün hızlı yoludur (kayıttan önce ve çevrimdışı da çalışır). ÖDÜL kararları
// yalnızca sunucudan gelen değere bakar ([serverCurrent]). Tutturulan günler kuyruğa yazılır ve
// `submitStreakDay` ile gönderilir (bkz. StreakSync.kt).
//
// Farklar:
//  - Android `object` + iki SharedPreferences dosyası ("streak_prefs", "streak_signup_pending");
//    burada sınıf + [OyunKeyValueStore] (anahtar adları aynı). Bekleyen kayıt cevapları iOS'ta giriş
//    akışının deposunda (GirisYerelDepo) durduğu için [applyPendingSignup] onları parametre olarak alır.
//  - AnalyticsLogger, StreakDiag ve Log çağrıları kaldırıldı (ölçüm olayları: streak_day_done,
//    streak_broken, streak_challenge_done, streak_freeze_used).
//  - NewStreakPromptDebug.forceShow kaldırıldı.
//  - `System.currentTimeMillis()` ve gün kimliği enjekte edilen saatten gelir.
package com.sorobit.shared.seri

import com.sorobit.shared.oyun.OyunKeyValueStore

class StreakRepository(
    private val store: OyunKeyValueStore,
    private val tracker: StudyTimeTracker,
    private val clock: StreakClock,
) {
    companion object {
        const val KEY_GOAL_MINUTES = "goal_minutes"
        const val KEY_CHALLENGE_DAYS = "challenge_days"
        const val KEY_CURRENT = "current"
        const val KEY_LONGEST = "longest"
        const val KEY_LAST_DAY = "last_goal_day"
        const val KEY_ACHIEVED_DAYS = "achieved_days"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_CELEBRATION_DAY = "celebration_day"
        const val KEY_CELEBRATION_STREAK = "celebration_streak"
        const val KEY_CHALLENGE_LOGGED = "challenge_logged"
        const val KEY_PENDING_SYNC_DAYS = "pending_sync_days"
        const val KEY_SERVER_CURRENT = "server_current"
        const val KEY_SERVER_LONGEST = "server_longest"
        const val KEY_SERVER_CLAIMED = "server_claimed"
        const val KEY_LAST_PING_DAY = "last_ping_day"
        const val KEY_OWNER_UID = "owner_uid"
        const val KEY_CHALLENGE_CLAIMED = "challenge_claimed"
        const val KEY_CHALLENGE_CLAIMED_DAY = "challenge_claimed_day"
        const val KEY_NEW_STREAK_PROMPT_DAY = "new_streak_prompt_day"
        const val KEY_FREEZES = "freezes"
        const val KEY_FREEZE_DAY = "freeze_day"
        const val KEY_FROZEN_DAYS = "frozen_days"
        const val KEY_FREEZE_NOTICE = "freeze_notice"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_HOUR_PENDING = "reminder_hour_pending"
    }

    private fun dayId(offset: Int): String = StreakDays.dayId(clock, offset)
    private fun today(): String = dayId(0)

    private fun str(key: String): String = store.getString(key).orEmpty()

    // ── Hedefler ────────────────────────────────────────────────────────

    fun goalMinutes(): Int = store.getInt(KEY_GOAL_MINUTES, StreakRules.DEFAULT_GOAL_MINUTES)

    fun setGoalMinutes(minutes: Int) {
        val value = if (minutes in StreakRules.GOAL_OPTIONS) minutes else StreakRules.DEFAULT_GOAL_MINUTES
        store.putInt(KEY_GOAL_MINUTES, value)
    }

    // ── Hatırlatma saati ────────────────────────────────────────────────
    // Bildirimi sunucu gönderir (functions/index.js → sendStreakReminders); burada yalnızca seçilen
    // yerel saat ve "henüz sunucuya iletilmedi" işareti var.

    fun reminderHour(): Int = store.getInt(KEY_REMINDER_HOUR, StreakRules.DEFAULT_REMINDER_HOUR)

    /** Kullanıcı saati seçti (kayıtta ya da seri ekranında). Günlük "buradayım" işareti de silinir. */
    fun setReminderHour(hour: Int) {
        store.putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
        store.putBoolean(KEY_REMINDER_HOUR_PENDING, true)
        store.remove(KEY_LAST_PING_DAY)
    }

    fun reminderHourPending(): Boolean = store.getBoolean(KEY_REMINDER_HOUR_PENDING, false)

    /** Sunucu [sentHour]'u kabul etti; bu arada saat yeniden değiştiyse işaret kalır. */
    fun onReminderHourSynced(sentHour: Int) {
        if (reminderHour() != sentHour) return
        store.putBoolean(KEY_REMINDER_HOUR_PENDING, false)
    }

    /** Sunucudaki seçimi yerele alır; bu cihazda bekleyen bir seçim varsa o kazanır. */
    fun adoptServerReminderHour(hour: Int) {
        if (hour !in 0..23 || reminderHourPending()) return
        store.putInt(KEY_REMINDER_HOUR, hour)
    }

    /** Kullanıcının seçtiği meydan okuma; hiç seçmemişse 0 (varsayılana DÜŞÜLMÜYOR). */
    fun chosenChallengeDays(): Int = store.getInt(KEY_CHALLENGE_DAYS, 0)

    /** Meydan okumayı yazar (yalnızca kayıt akışı). Kilit sunucuda (`challengePatch`). */
    fun setChallengeDays(days: Int) {
        val value = if (days in StreakRules.CHALLENGE_OPTIONS) days else StreakRules.DEFAULT_CHALLENGE_DAYS
        store.putInt(KEY_CHALLENGE_DAYS, value)
    }

    /** Meydan okuma ödülü alındıysa alınan gün sayısı; alınmadıysa 0. */
    fun challengeClaimed(): Int = store.getInt(KEY_CHALLENGE_CLAIMED, 0)

    /** Ödülün alındığı gün; alınmadıysa boş. Kart o günün sonuna kadar "TAMAMLANDI" durur. */
    fun challengeClaimedDay(): String = str(KEY_CHALLENGE_CLAIMED_DAY)

    fun markChallengeClaimed(days: Int) {
        store.putInt(KEY_CHALLENGE_CLAIMED, days)
        store.putString(KEY_CHALLENGE_CLAIMED_DAY, today())
    }

    /** Ders dönüşünde yeni seri sorusu sorulsun mu: seri YOK ve bugün henüz sorulmadı. */
    fun needsNewStreakPrompt(): Boolean {
        if (store.getInt(KEY_CURRENT, 0) > 0) return false
        return str(KEY_NEW_STREAK_PROMPT_DAY) != today()
    }

    fun markNewStreakPromptShown() {
        store.putString(KEY_NEW_STREAK_PROMPT_DAY, today())
    }

    /** Seri kırıldıktan sonra seçilen yeni meydan okuma; ödül işareti ve günlük bildirim işareti silinir. */
    fun startNewChallenge(days: Int) {
        val value = if (days in StreakRules.CHALLENGE_OPTIONS) days else StreakRules.DEFAULT_CHALLENGE_DAYS
        store.putInt(KEY_CHALLENGE_DAYS, value)
        store.remove(KEY_CHALLENGE_CLAIMED)
        store.remove(KEY_CHALLENGE_CLAIMED_DAY)
        store.remove(KEY_LAST_PING_DAY)
    }

    // ── Seri dondurma ───────────────────────────────────────────────────

    /** Elde harcanmamış dondurma sayısı (şimdilik 0 ya da 1). */
    fun freezesHeld(): Int = store.getInt(KEY_FREEZES, 0)

    /** Satın alma sunucuda tamamlandı; yerel önbelleğe de işlenir. */
    fun markFreezePurchased(freezes: Int, freezeDay: String) {
        store.putInt(KEY_FREEZES, freezes.coerceAtLeast(0))
        store.putString(KEY_FREEZE_DAY, freezeDay)
    }

    /** Bekleyen dondurma bildirimini döner ve siler; yoksa ya da bayatsa null. */
    fun takeFreezeNotice(): StreakFreezeNotice? {
        val raw = str(KEY_FREEZE_NOTICE)
        if (raw.isEmpty()) return null
        store.remove(KEY_FREEZE_NOTICE)
        val parts = raw.split("|")
        val kind = parts.getOrElse(0) { "" }
        val day = parts.getOrElse(1) { "" }
        if (day != today()) return null
        return StreakFreezeNotice.entries.firstOrNull { it.name == kind }
    }

    /** Kaçan günü eldeki dondurmayla kapatır; kapattıysa serinin yeni son gününü döner. */
    private fun applyFreeze(current: Int, lastDay: String): String? {
        val freezes = store.getInt(KEY_FREEZES, 0)
        if (freezes <= 0) return null
        val settled = StreakFreezeRules.settle(
            current = current,
            lastDay = lastDay,
            freezes = freezes,
            freezeDay = str(KEY_FREEZE_DAY),
            scanDays = StreakRules.FREEZE_SCAN_DAYS,
        ) { dayId(it) } ?: return null

        val saved = settled.missedDay == dayId(-1)
        val notice = if (saved) StreakFreezeNotice.SAVED else StreakFreezeNotice.LOST
        val frozen = recentOnly(readFrozenDays() + settled.missedDay)
        store.putInt(KEY_FREEZES, freezes - 1)
        store.remove(KEY_FREEZE_DAY)
        store.putString(KEY_LAST_DAY, settled.missedDay)
        store.putString(KEY_FROZEN_DAYS, frozen)
        store.putString(KEY_FREEZE_NOTICE, "${notice.name}|${today()}")
        return settled.missedDay
    }

    private fun readFrozenDays(): Set<String> =
        str(KEY_FROZEN_DAYS).split(",").filter { it.isNotBlank() }.toSet()

    /** Hafta şeridinin arşiv penceresine sığan günler, saklanacak biçimde. */
    private fun recentOnly(days: Set<String>): String {
        val cutoff = (0 until StreakRules.ACHIEVED_HISTORY_DAYS).map { dayId(-it) }.toSet()
        return days.intersect(cutoff).sorted().joinToString(",")
    }

    // ── Kutlama kuyruğu ─────────────────────────────────────────────────

    private fun queueCelebration(streak: Int, day: String) {
        store.putString(KEY_CELEBRATION_DAY, day)
        store.putInt(KEY_CELEBRATION_STREAK, streak)
    }

    /** Bekleyen kutlamanın seri değeri; yoksa ya da bayatsa 0. Okumak temizlemez. */
    fun pendingCelebration(): Int {
        if (str(KEY_CELEBRATION_DAY) != today()) return 0
        return store.getInt(KEY_CELEBRATION_STREAK, 0)
    }

    fun clearPendingCelebration() {
        store.remove(KEY_CELEBRATION_DAY)
        store.remove(KEY_CELEBRATION_STREAK)
    }

    // ── Hesap sahipliği ─────────────────────────────────────────────────

    /** Yerel veriyi oturumdaki hesaba bağlar; sahibi başka bir hesapsa her şeyi (süre dahil) siler. */
    fun bindToUser(uid: String?) {
        if (uid.isNullOrBlank()) return
        val owner = str(KEY_OWNER_UID)
        if (owner == uid) return
        if (owner.isNotEmpty()) {
            wipe()
        }
        store.putString(KEY_OWNER_UID, uid)
    }

    /** Yerel veri bir hesaba bağlı mı (hesap yokken gün sayılmaz). */
    fun isAccountBound(): Boolean = str(KEY_OWNER_UID).isNotEmpty()

    // ── Kayıt cevapları (bekleyen) ──────────────────────────────────────

    /**
     * Oturum açıldıktan sonra ([bindToUser]'dan hemen sonra): bekleyen kayıt cevapları hesap yeniyse
     * uygulanır, değilse atılır. ÇAĞIRAN, sonuç ne olursa olsun bekleyen cevapları siler
     * (Android: `pending.edit().clear()`).
     *
     * @param accountCreatedAtMs Hesabın açılış zamanı (Android: FirebaseUser.metadata.creationTimestamp);
     *   bilinmiyorsa null → mevcut hesap sayılır.
     */
    fun applyPendingSignup(
        pending: StreakPendingSignup?,
        uid: String,
        accountCreatedAtMs: Long?,
    ): StreakSignupResult {
        if (pending == null || pending.savedAtMs == 0L) return StreakSignupResult.NO_PENDING
        val markedCreatedAt: Long? = pending.createdAtMs
            .takeIf { it > 0L && pending.createdUid == uid }
        val now = clock.nowMs()
        val fresh = now - pending.savedAtMs in 0..StreakRules.PENDING_SIGNUP_MAX_AGE_MS
        val createdAt = markedCreatedAt ?: accountCreatedAtMs
        val newAccount = createdAt != null && now - createdAt in 0..StreakRules.NEW_ACCOUNT_WINDOW_MS
        if (!fresh) return StreakSignupResult.DISCARDED_STALE
        if (!newAccount) return StreakSignupResult.DISCARDED_EXISTING_ACCOUNT
        if (pending.goalMinutes > 0) setGoalMinutes(pending.goalMinutes)
        if (pending.challengeDays > 0) setChallengeDays(pending.challengeDays)
        if (pending.reminderHour in 0..23) setReminderHour(pending.reminderHour)
        markOnboardingDone()
        // Yeni kullanıcının serisi 0; az önce cevapladığı soru ilk dersinin sonunda yeniden sorulmasın.
        markNewStreakPromptShown()
        return StreakSignupResult.APPLIED
    }

    private fun wipe() {
        store.clear()
        tracker.clearAll()
    }

    // ── Sunucu eşitlemesi ───────────────────────────────────────────────

    private fun queueSyncDay(day: String) {
        val days = (pendingSyncDays() + day).distinct().sorted().takeLast(StreakRules.MAX_PENDING_SYNC_DAYS)
        store.putString(KEY_PENDING_SYNC_DAYS, days.joinToString(","))
    }

    /** Gönderilmeyi bekleyen günler (bir haftadan eskiler elenir). */
    fun pendingSyncDays(): List<String> {
        val recent = (0..StreakRules.MAX_PENDING_SYNC_DAYS).map { dayId(-it) }.toSet()
        return str(KEY_PENDING_SYNC_DAYS)
            .split(",")
            .filter { it.isNotBlank() && it in recent }
            .sorted()
    }

    /** Sunucu [sentDays]'i kabul etti; kuyruktan yalnızca onlar silinir. */
    fun onSyncAccepted(sentDays: List<String>, current: Int, longest: Int, claimed: Set<Int>) {
        val remaining = pendingSyncDays() - sentDays.toSet()
        store.putString(KEY_PENDING_SYNC_DAYS, remaining.joinToString(","))
        store.putInt(KEY_SERVER_CURRENT, current)
        store.putInt(KEY_SERVER_LONGEST, longest)
        store.putString(KEY_SERVER_CLAIMED, claimed.sorted().joinToString(","))
    }

    /**
     * Sunucudan okunan durumu yerel duruma işler (Android: adoptServerState). Yerel sayaç yalnızca
     * "sunucu ilerideyse ve sunucunun son günü tazeyse" sunucunun değerini alır.
     *
     * @param freezes Sunucudaki dondurma sayısı; yanıtta yoksa [StreakRules.FREEZES_UNKNOWN].
     */
    fun adoptServerState(
        current: Int,
        longest: Int,
        lastDay: String,
        claimed: Set<Int>,
        recentDays: Set<String>,
        goalMinutes: Int,
        challengeDays: Int,
        challengeClaimed: Int,
        freezes: Int,
        freezeDay: String,
        frozenDays: Set<String>,
    ) {
        // Önce yerel taraf kapatılıyor (dondurma köprüsü), sonra sunucunun değerleri.
        applyFreeze(store.getInt(KEY_CURRENT, 0), str(KEY_LAST_DAY))

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
                scanDays = StreakRules.FREEZE_SCAN_DAYS,
            ) { dayId(it) }?.let { settled ->
                serverLastDay = settled.missedDay
                serverFreezes = freezes - 1
                serverFreezeDay = ""
                serverFrozen = frozenDays + settled.missedDay
            }
        }

        // Hedef YALNIZCA bu cihazda kurulum akışı hiç görülmediyse sunucudan alınır.
        if (!isOnboardingDone() && goalMinutes > 0) {
            store.putInt(KEY_GOAL_MINUTES, goalMinutes)
            markOnboardingDone()
        }
        if (challengeClaimed > 0) {
            store.putInt(KEY_CHALLENGE_CLAIMED, challengeClaimed)
        } else if (current <= 1) {
            // Sunucu ödül hakkını iade etti: yeni tur başladı.
            store.remove(KEY_CHALLENGE_CLAIMED)
            store.remove(KEY_CHALLENGE_CLAIMED_DAY)
        }
        // Sunucuda kayıtlı bir meydan okuma varsa yerel de ONA uyar.
        if (challengeDays > 0) {
            store.putInt(KEY_CHALLENGE_DAYS, challengeDays)
        }
        store.putInt(KEY_SERVER_CURRENT, current)
        store.putInt(KEY_SERVER_LONGEST, longest)
        store.putString(KEY_SERVER_CLAIMED, claimed.sorted().joinToString(","))

        // Pencere üç gün: dün, bugün ve YARIN (sunucu gün kimliğini ±1 gün payla kabul eder).
        val fresh = setOf(dayId(1), dayId(0), dayId(-1))
        val serverFresh = serverLastDay in fresh
        val localCurrent = store.getInt(KEY_CURRENT, 0)
        val adopt = current > localCurrent && serverFresh
        if (adopt) {
            store.putInt(KEY_CURRENT, current)
            store.putString(KEY_LAST_DAY, serverLastDay)
        }
        if (freezes != StreakRules.FREEZES_UNKNOWN) {
            store.putInt(KEY_FREEZES, serverFreezes.coerceAtLeast(0))
            if (serverFreezes > 0 && serverFreezeDay.isNotEmpty()) {
                store.putString(KEY_FREEZE_DAY, serverFreezeDay)
            } else {
                store.remove(KEY_FREEZE_DAY)
            }
            // Donmuş günler BİRLEŞTİRİLİR.
            store.putString(KEY_FROZEN_DAYS, recentOnly(readFrozenDays() + serverFrozen))
        }
        // Rekor, güncel seriden küçük olamaz.
        val adoptedCurrent = if (adopt) current else 0
        val targetLongest = maxOf(longest, adoptedCurrent)
        val localLongest = store.getInt(KEY_LONGEST, 0)
        if (targetLongest > localLongest) store.putInt(KEY_LONGEST, targetLongest)

        // Hafta şeridi: sunucunun bildiği günler yerel arşivle BİRLEŞTİRİLİR.
        if (recentDays.isNotEmpty()) {
            val cutoff = (0 until StreakRules.ACHIEVED_HISTORY_DAYS).map { dayId(-it) }.toSet()
            val merged = (readAchievedDays() + recentDays).intersect(cutoff)
            store.putString(KEY_ACHIEVED_DAYS, merged.sorted().joinToString(","))
        }
    }

    /** Bugün sunucuya "buradayım" denmiş mi (akşam hatırlatması saat dilimini bilsin diye). */
    fun needsDailyPing(): Boolean = str(KEY_LAST_PING_DAY) != today()

    fun markDailyPing() {
        store.putString(KEY_LAST_PING_DAY, today())
    }

    /** Sunucunun bildiği seri. Ödül satırları buna bakar; hiç eşitlenmediyse 0. */
    fun serverCurrent(): Int = store.getInt(KEY_SERVER_CURRENT, 0)

    fun serverLongest(): Int = store.getInt(KEY_SERVER_LONGEST, 0)

    fun claimedMilestones(): Set<Int> =
        str(KEY_SERVER_CLAIMED).split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    /** Toplanan taşı yerel önbelleğe de işler. */
    fun markMilestoneClaimed(milestone: Int) {
        val updated = claimedMilestones() + milestone
        store.putString(KEY_SERVER_CLAIMED, updated.sorted().joinToString(","))
    }

    /** Seri kurulum akışı (hedef + meydan okuma) tamamlandı mı. */
    fun isOnboardingDone(): Boolean = store.getBoolean(KEY_ONBOARDING_DONE, false)

    fun markOnboardingDone() {
        store.putBoolean(KEY_ONBOARDING_DONE, true)
    }

    // ── Seri ────────────────────────────────────────────────────────────

    /** Seriyi güncel süreye göre tazeler ve son durumu döner. Gün en fazla bir kez ilerletilir. */
    fun refresh(): StreakState {
        val goal = goalMinutes()
        val challenge = chosenChallengeDays()
        val seconds = tracker.secondsToday()
        val today = today()
        val yesterday = dayId(-1)

        var current = store.getInt(KEY_CURRENT, 0)
        var longest = store.getInt(KEY_LONGEST, 0)
        var lastDay = str(KEY_LAST_DAY)
        var achieved = readAchievedDays()

        // İmkânsız derecede ileri bir son gün (yarından sonrası): bozuk veri, atılır.
        val tomorrow = dayId(1)
        if (lastDay > tomorrow) {
            lastDay = ""
            current = 0
            achieved = achieved.filterTo(mutableSetOf()) { it <= today }
            writeState(current, longest, lastDay, achieved)
        }

        // Son gün bugünden ilerideyse (saat dilimi payı) ne ilerletilir ne kırılır.
        val lastDayInFuture = lastDay > today

        // Seri dondurma: ilerleme ve kırılma dallarından ÖNCE.
        applyFreeze(current, lastDay)?.let { lastDay = it }

        // Hesap yokken gün SAYILMIYOR, yalnızca süre birikiyor.
        val accountBound = isAccountBound()

        if (seconds >= goal * 60 && lastDay != today && !lastDayInFuture && accountBound) {
            current = if (lastDay == yesterday) current + 1 else 1
            longest = maxOf(longest, current)
            lastDay = today
            achieved = achieved + today
            writeState(current, longest, lastDay, achieved)
            queueCelebration(current, today)
            markChallengeDoneOnce(current, challenge)
            queueSyncDay(today)
        }

        // Kırılma: okunurken hesaplanır ve YAZILIR.
        if (current > 0 && lastDay.isNotEmpty() && !lastDayInFuture &&
            lastDay != today && lastDay != yesterday
        ) {
            current = 0
            writeState(current, longest, lastDay, achieved)
        }

        val frozen = readFrozenDays()
        return StreakState(
            current = current,
            longest = longest,
            goalMinutes = goal,
            secondsToday = seconds,
            achievedDays = achieved + daysOfStreak(current, lastDay, frozen),
            frozenDays = frozen,
            freezeHeld = freezesHeld() > 0,
        )
    }

    /** Serinin kapsadığı günler (N günlük seri, son günde biten N günün tutturulduğu anlamına gelir). */
    private fun daysOfStreak(current: Int, lastDay: String, frozen: Set<String>): Set<String> {
        if (current <= 0 || lastDay.isEmpty()) return emptySet()
        val lastOffset = (1 downTo -StreakRules.ACHIEVED_HISTORY_DAYS)
            .firstOrNull { dayId(it) == lastDay }
            ?: return emptySet()
        val span = minOf(current, StreakRules.ACHIEVED_HISTORY_DAYS)
        val today = today()
        val days = mutableSetOf<String>()
        var offset = lastOffset
        var stepsLeft = span + frozen.size
        while (days.size < span && stepsLeft-- > 0) {
            val day = dayId(offset--)
            if (day !in frozen) days += day
        }
        return days.filterTo(mutableSetOf()) { it <= today }
    }

    /** Meydan okuma tamamlandığında bir kez işaretlenir (Android'de ölçüm olayı gönderiliyordu). */
    private fun markChallengeDoneOnce(current: Int, challenge: Int) {
        if (challenge <= 0 || current < challenge) return
        if (store.getInt(KEY_CHALLENGE_LOGGED, 0) == challenge) return
        store.putInt(KEY_CHALLENGE_LOGGED, challenge)
    }

    private fun writeState(current: Int, longest: Int, lastDay: String, achieved: Set<String>) {
        val cutoff = (0 until StreakRules.ACHIEVED_HISTORY_DAYS).map { dayId(-it) }.toSet()
        store.putInt(KEY_CURRENT, current)
        store.putInt(KEY_LONGEST, longest)
        store.putString(KEY_LAST_DAY, lastDay)
        store.putString(KEY_ACHIEVED_DAYS, achieved.intersect(cutoff).sorted().joinToString(","))
    }

    private fun readAchievedDays(): Set<String> =
        str(KEY_ACHIEVED_DAYS).split(",").filter { it.isNotBlank() }.toSet()

    /** Yalnızca hata ayıklama/test için. */
    fun resetForDebug() {
        store.clear()
    }

    /**
     * Yalnızca ekran galerisi ve sahte akış: yerel durumu doğrudan kurar (Android'de karşılığı yok;
     * Android'de bu durumlar ancak gerçek günler beklenerek kurulabiliyor).
     *
     * @param lastDayOffset Son tutturulan günün bugüne göre kayması (0 = bugün, -1 = dün); seri yoksa yok sayılır.
     */
    fun seedForPreview(
        ownerUid: String,
        current: Int,
        longest: Int,
        lastDayOffset: Int,
        goalMinutes: Int,
        challengeDays: Int,
        challengeClaimed: Int,
        freezes: Int,
        serverCurrent: Int,
        claimed: List<Int>,
    ) {
        store.clear()
        store.putString(KEY_OWNER_UID, ownerUid)
        store.putInt(KEY_GOAL_MINUTES, goalMinutes)
        if (challengeDays > 0) store.putInt(KEY_CHALLENGE_DAYS, challengeDays)
        if (challengeClaimed > 0) {
            store.putInt(KEY_CHALLENGE_CLAIMED, challengeClaimed)
            store.putString(KEY_CHALLENGE_CLAIMED_DAY, today())
        }
        store.putBoolean(KEY_ONBOARDING_DONE, true)
        store.putInt(KEY_CURRENT, current)
        store.putInt(KEY_LONGEST, maxOf(longest, current))
        if (current > 0) {
            store.putString(KEY_LAST_DAY, dayId(lastDayOffset))
            val days = (0 until minOf(current, StreakRules.ACHIEVED_HISTORY_DAYS)).map { dayId(lastDayOffset - it) }
            store.putString(KEY_ACHIEVED_DAYS, days.sorted().joinToString(","))
        }
        if (freezes > 0) {
            store.putInt(KEY_FREEZES, freezes)
            store.putString(KEY_FREEZE_DAY, today())
        }
        store.putInt(KEY_SERVER_CURRENT, serverCurrent)
        store.putInt(KEY_SERVER_LONGEST, maxOf(longest, serverCurrent))
        store.putString(KEY_SERVER_CLAIMED, claimed.sorted().joinToString(","))
        // Galeride yeni seri sorusu kendiliğinden açılmasın.
        store.putString(KEY_NEW_STREAK_PROMPT_DAY, today())
    }
}
