// Kaynak (sunucu, DOKUNULMADI — yalnızca okundu): functions/index.js →
//   streakDayNumber, settleStreakFreeze, applyStreakDays, challengePatch, submitStreakDay,
//   buyStreakFreeze, claimStreakReward (kural kısmı; cüzdan, günlük tavan ve Firestore yazımı dışarıda).
//
// NEDEN VAR: Firebase iOS'ta henüz bağlı değil. Swift'teki sahte seri servisi sunucuyu bu modelle
// taklit eder; böylece "gün bildirildi → ödül toplanabilir" akışı uçtan uca denenebilir. GERÇEK
// uygulamada bu sınıf KULLANILMAZ: kararları Cloud Function verir. Sunucu kuralı değişirse burası
// eskir (yalnızca sahte akışı etkiler).
//
// Bilerek dışarıda kalanlar: hatırlatma alanları (reminderHourUtc, lastSeenAt), günlük ödül tavanı
// (STREAK_CLAIM_DAILY_LIMIT = 10), onaysız öğretmen engeli, `users/{uid}.longestStreak` yazımı.
package com.sorobit.shared.seri

import kotlinx.datetime.LocalDate

/** Sunucu çağrısının hata kodu (HttpsError kodu) ve Türkçe mesajı. */
data class StreakServerError(val code: String, val message: String)

/** Ödül toplama sonucu: başarıda ödül, başarısızlıkta [error]. */
data class StreakClaimResult(
    val error: StreakServerError?,
    val milestone: Int,
    val challengeDays: Int,
    val rewardKeys: Int,
    val rewardGold: Int,
)

/** Dondurma satın alma sonucu. */
data class StreakFreezeBuyResult(
    val error: StreakServerError?,
    val freezes: Int,
    val freezeDay: String,
)

class StreakServerModel(private val clock: StreakClock) {

    companion object {
        const val DAY_TOLERANCE_DAYS = 1
        const val MAX_DAYS_PER_CALL = 7
        const val RECENT_DAYS_KEPT = 21

        /** `yyyy-MM-dd` → epoch gün; geçersizse null (sunucu: streakDayNumber). */
        fun dayNumber(day: String): Int? {
            if (day.length != 10) return null
            return try {
                LocalDate.parse(day).toEpochDays()
            } catch (e: Exception) {
                null
            }
        }

        fun dayIdOf(dayNo: Int): String = LocalDate.fromEpochDays(dayNo).toString()
    }

    var current: Int = 0
        private set
    var longest: Int = 0
        private set
    var lastDay: String = ""
        private set
    var claimed: List<Int> = emptyList()
        private set
    var recentDays: List<String> = emptyList()
        private set
    var goalMinutes: Int = 0
        private set
    var challengeDays: Int = 0
        private set
    var challengeClaimed: Int = 0
        private set
    var freezes: Int = 0
        private set
    var freezeDay: String = ""
        private set
    var frozenDays: List<String> = emptyList()
        private set
    var reminderLocalHour: Int? = null
        private set

    /** Doküman var mı (hiç gün bildirilmediyse yok). */
    var exists: Boolean = false
        private set

    private fun serverDayNo(): Int = clock.nowMs().floorDiv(86_400_000L).toInt()

    /** Galeri/sahte akış: dokümanı doğrudan kurar. */
    fun seed(current: Int, longest: Int, lastDay: String, claimed: List<Int>, challengeDays: Int, challengeClaimed: Int, freezes: Int, freezeDay: String) {
        this.current = current
        this.longest = maxOf(longest, current)
        this.lastDay = lastDay
        this.claimed = claimed
        this.challengeDays = challengeDays
        this.challengeClaimed = challengeClaimed
        this.freezes = freezes
        this.freezeDay = freezeDay
        exists = true
    }

    /** Hesap değişimi. */
    fun reset() {
        current = 0; longest = 0; lastDay = ""; claimed = emptyList(); recentDays = emptyList()
        goalMinutes = 0; challengeDays = 0; challengeClaimed = 0; freezes = 0; freezeDay = ""
        frozenDays = emptyList(); reminderLocalHour = null; exists = false
    }

    // ── settleStreakFreeze ──

    private data class FreezeState(val current: Int, val lastDay: String, val freezes: Int, val freezeDay: String, val frozenDays: List<String>)

    private fun settle(s: FreezeState, refDayNo: Int?): FreezeState {
        val f = maxOf(0, s.freezes)
        if (f <= 0 || s.current <= 0 || refDayNo == null) return s
        val lastNo = dayNumber(s.lastDay) ?: return s
        if (refDayNo - lastNo < 2) return s
        val missedNo = lastNo + 1
        val boughtNo = dayNumber(s.freezeDay)
        if (boughtNo != null && missedNo < boughtNo) return s
        val missedDay = dayIdOf(missedNo)
        val frozen = (s.frozenDays + missedDay).distinct().sorted().takeLast(RECENT_DAYS_KEPT)
        return FreezeState(s.current, missedDay, f - 1, "", frozen)
    }

    private fun clientTodayNo(raw: String): Int? {
        val no = dayNumber(raw) ?: return null
        if (kotlin.math.abs(no - serverDayNo()) > DAY_TOLERANCE_DAYS) return null
        return no
    }

    private fun applyChallenge(runDay: Int, requested: Int) {
        if (requested <= 0 || requested > 400) return
        if (runDay > 1) return
        if (challengeDays == requested && challengeClaimed == 0) return
        challengeDays = requested
        challengeClaimed = 0
    }

    /** `submitStreakDay`. Geçersiz gün varsa hata (invalid-argument); yoksa güncel durum. */
    fun submit(request: StreakSubmitRequest): StreakServerError? {
        val sdn = serverDayNo()
        val days = mutableListOf<String>()
        for (raw in request.days) {
            val no = dayNumber(raw) ?: return StreakServerError("invalid-argument", "Geçersiz gün.")
            if (no > sdn + DAY_TOLERANCE_DAYS) continue
            if (no < sdn - MAX_DAYS_PER_CALL) continue
            days += raw
        }
        if (days.size > MAX_DAYS_PER_CALL) return StreakServerError("invalid-argument", "Çok fazla gün.")
        days.sort()
        val todayNo = clientTodayNo(request.today)
        if (request.reminderHour != null && request.reminderHour in 0..23) {
            reminderLocalHour = request.reminderHour
        }

        if (days.isEmpty()) {
            applyChallenge(current, request.challengeDays)
            val settled = settle(FreezeState(current, lastDay, freezes, freezeDay, frozenDays), todayNo)
            lastDay = settled.lastDay; freezes = settled.freezes; freezeDay = settled.freezeDay; frozenDays = settled.frozenDays
            return null
        }

        // applyStreakDays
        var c = current
        var l = longest
        var last = lastDay
        var cl = claimed
        var fz = FreezeState(c, last, freezes, freezeDay, frozenDays)
        for (day in days) {
            if (last == day) continue
            val dayNo = dayNumber(day) ?: continue
            val settled = settle(FreezeState(c, last, fz.freezes, fz.freezeDay, fz.frozenDays), dayNo)
            last = settled.lastDay
            fz = settled
            val lastNo = dayNumber(last)
            if (lastNo != null && dayNo == lastNo + 1) {
                c += 1
            } else if (lastNo != null && dayNo <= lastNo) {
                continue
            } else {
                c = 1
                cl = emptyList()
            }
            l = maxOf(l, c)
            last = day
        }
        val next = settle(FreezeState(c, last, fz.freezes, fz.freezeDay, fz.frozenDays), todayNo)
        val runDay = c
        recentDays = (recentDays + days).distinct().sorted().takeLast(RECENT_DAYS_KEPT)
        current = c
        longest = l
        lastDay = next.lastDay
        claimed = cl
        freezes = next.freezes
        freezeDay = next.freezeDay
        frozenDays = next.frozenDays
        if (request.goalMinutes in 1..600) goalMinutes = request.goalMinutes
        applyChallenge(runDay, request.challengeDays)
        exists = true
        return null
    }

    /** Yanıt/doküman görünümü. */
    fun snapshot(): StreakServerState = StreakServerState(
        current = current,
        longest = longest,
        lastDay = lastDay,
        claimed = claimed.toSet(),
        recentDays = recentDays.toSet(),
        goalMinutes = goalMinutes,
        challengeDays = challengeDays,
        challengeClaimed = challengeClaimed,
        freezes = freezes,
        freezeDay = freezeDay,
        frozenDays = frozenDays.toSet(),
        reminderLocalHour = reminderLocalHour,
    )

    /** `buyStreakFreeze` (altın kontrolü dahil; altını çağıran düşer). */
    fun buyFreeze(today: String, currency: Int): StreakFreezeBuyResult {
        val todayNo = clientTodayNo(today)
            ?: return StreakFreezeBuyResult(StreakServerError("invalid-argument", "Geçersiz gün."), freezes, freezeDay)
        val settled = settle(FreezeState(current, lastDay, freezes, freezeDay, frozenDays), todayNo)
        lastDay = settled.lastDay; freezes = settled.freezes; freezeDay = settled.freezeDay; frozenDays = settled.frozenDays
        if (freezes >= StreakRules.FREEZE_MAX_HELD) {
            return StreakFreezeBuyResult(StreakServerError("already-exists", "Zaten bir seri dondurman var."), freezes, freezeDay)
        }
        if (currency < StreakRules.FREEZE_COST_GOLD) {
            return StreakFreezeBuyResult(StreakServerError("failed-precondition", "Yetersiz altın."), freezes, freezeDay)
        }
        freezes += 1
        freezeDay = dayIdOf(todayNo)
        return StreakFreezeBuyResult(null, freezes, freezeDay)
    }

    /** `claimStreakReward` { milestone }. */
    fun claimMilestone(milestone: Int): StreakClaimResult {
        val reward = StreakMilestones.rewardFor(milestone)
            ?: return StreakClaimResult(StreakServerError("invalid-argument", "Geçersiz kilometre taşı."), milestone, 0, 0, 0)
        if (current < milestone) {
            return StreakClaimResult(StreakServerError("failed-precondition", "Bu ödül için serin yeterli değil."), milestone, 0, 0, 0)
        }
        if (milestone in claimed) {
            return StreakClaimResult(StreakServerError("already-exists", "Bu ödülü zaten aldın."), milestone, 0, 0, 0)
        }
        claimed = claimed + milestone
        return StreakClaimResult(null, milestone, 0, reward.keys, reward.gold)
    }

    /** `claimStreakReward` { kind: "challenge" }. */
    fun claimChallenge(): StreakClaimResult {
        val days = challengeDays
        val gold = StreakMilestones.challengeReward(days)
        if (gold <= 0) {
            return StreakClaimResult(StreakServerError("failed-precondition", "Meydan okuma seçilmemiş."), 0, 0, 0, 0)
        }
        if (current < days) {
            return StreakClaimResult(StreakServerError("failed-precondition", "Meydan okuma henüz tamamlanmadı."), 0, 0, 0, 0)
        }
        if (challengeClaimed > 0) {
            return StreakClaimResult(StreakServerError("already-exists", "Bu ödülü zaten aldın."), 0, 0, 0, 0)
        }
        challengeClaimed = days
        return StreakClaimResult(null, 0, days, 0, gold)
    }
}
