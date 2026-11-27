// Kaynak (Android): app/src/main/java/com/example/app/StreakSyncService.kt — istek gövdeleri, yanıt
// ayrıştırma ve başarı sonrası yerel işlemler (Firebase çağrıları platformda).
//
// Sunucu sözleşmesi (functions/index.js; DOKUNULMADI, yalnızca okundu):
//  - Firestore okuma: `users/{uid}/streak/state` (sahibine açık). Alanlar: current, longest, lastDay,
//    claimed[], recentDays[], goalMinutes, challengeDays, challengeClaimed, freezes, freezeDay,
//    frozenDays[], reminderLocalHour.
//  - Callable `submitStreakDay` { days[], goalMinutes, challengeDays, utcOffsetMinutes, today,
//    reminderHour? } → { success, current, longest, lastDay, claimed[], recentDays[], goalMinutes,
//    challengeDays, challengeClaimed, freezes, freezeDay, frozenDays[], reminderLocalHour }.
//    Sunucu ayrıca `users/{uid}.longestStreak` yazar (profil kartı, publicProfiles aynası).
//  - Callable `buyStreakFreeze` { today } → { success, currency, keys, freezes, freezeDay, frozenDays[], lastDay }.
//  - Callable `claimStreakReward` { milestone } ya da { kind: "challenge" } → { success, milestone,
//    challengeDays, rewardKeys, rewardGold, keys, currency }.
package com.sorobit.shared.seri

/** `submitStreakDay` isteği (Android: StreakSyncService.syncPendingDays payload). */
data class StreakSubmitRequest(
    val days: List<String>,
    val goalMinutes: Int,
    val challengeDays: Int,
    val utcOffsetMinutes: Int,
    val today: String,
    /** Yalnızca bu cihazda seçildiyse; yoksa null (alan gönderilmez). */
    val reminderHour: Int?,
) {
    /** Cloud Function gövdesi. */
    fun payload(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "days" to days,
            "goalMinutes" to goalMinutes,
            "challengeDays" to challengeDays,
            "utcOffsetMinutes" to utcOffsetMinutes,
            "today" to today,
        )
        if (reminderHour != null) map["reminderHour"] = reminderHour
        return map
    }
}

/** Sunucunun seri durumu: `submitStreakDay` yanıtı ya da `streak/state` dokümanı. */
data class StreakServerState(
    val current: Int,
    val longest: Int,
    val lastDay: String,
    val claimed: Set<Int>,
    val recentDays: Set<String>,
    val goalMinutes: Int,
    val challengeDays: Int,
    val challengeClaimed: Int,
    /** Yanıtta yoksa [StreakRules.FREEZES_UNKNOWN]. */
    val freezes: Int,
    val freezeDay: String,
    val frozenDays: Set<String>,
    /** Kayıtlı hatırlatma saati; yoksa null. */
    val reminderLocalHour: Int?,
) {
    companion object {
        private fun int(raw: Any?): Int? = (raw as? Number)?.toInt()

        private fun ints(raw: Any?): Set<Int> =
            (raw as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet().orEmpty()

        private fun strings(raw: Any?): Set<String> =
            (raw as? List<*>)?.mapNotNull { it as? String }?.toSet().orEmpty()

        /**
         * `submitStreakDay` yanıtı. `current` yoksa null (Android: `?: return@addOnSuccessListener`).
         * Dondurma alanı yoksa FREEZES_UNKNOWN (sunucunun eski sürümü).
         */
        fun fromFunctionResult(data: Map<String, Any?>): StreakServerState? {
            val current = int(data["current"]) ?: return null
            return StreakServerState(
                current = current,
                longest = int(data["longest"]) ?: current,
                lastDay = (data["lastDay"] as? String).orEmpty(),
                claimed = ints(data["claimed"]),
                recentDays = strings(data["recentDays"]),
                goalMinutes = int(data["goalMinutes"]) ?: 0,
                challengeDays = int(data["challengeDays"]) ?: 0,
                challengeClaimed = int(data["challengeClaimed"]) ?: 0,
                freezes = int(data["freezes"]) ?: StreakRules.FREEZES_UNKNOWN,
                freezeDay = (data["freezeDay"] as? String).orEmpty(),
                frozenDays = strings(data["frozenDays"]),
                reminderLocalHour = int(data["reminderLocalHour"]),
            )
        }

        /** `users/{uid}/streak/state` dokümanı (Android: refreshFromServer). Dokümanda dondurma yoksa 0. */
        fun fromDocument(doc: Map<String, Any?>): StreakServerState =
            StreakServerState(
                current = int(doc["current"]) ?: 0,
                longest = int(doc["longest"]) ?: 0,
                lastDay = (doc["lastDay"] as? String).orEmpty(),
                claimed = ints(doc["claimed"]),
                recentDays = strings(doc["recentDays"]),
                goalMinutes = int(doc["goalMinutes"]) ?: 0,
                challengeDays = int(doc["challengeDays"]) ?: 0,
                challengeClaimed = int(doc["challengeClaimed"]) ?: 0,
                freezes = int(doc["freezes"]) ?: 0,
                freezeDay = (doc["freezeDay"] as? String).orEmpty(),
                frozenDays = strings(doc["frozenDays"]),
                reminderLocalHour = int(doc["reminderLocalHour"]),
            )
    }
}

/** Eşitlemenin istemci tarafı (Android: StreakSyncService'in Firebase dışı kısmı). */
object StreakSync {

    /**
     * Gönderilecek istek; gönderilecek gün yok ve bugün "buradayım" denmişse null
     * (Android: syncPendingDays'in erken dönüşü).
     */
    fun buildSubmitRequest(repo: StreakRepository, clock: StreakClock): StreakSubmitRequest? {
        val days = repo.pendingSyncDays()
        if (days.isEmpty() && !repo.needsDailyPing()) return null
        val reminder: Int? = if (repo.reminderHourPending()) repo.reminderHour() else null
        return StreakSubmitRequest(
            days = days,
            goalMinutes = repo.goalMinutes(),
            challengeDays = repo.chosenChallengeDays(),
            utcOffsetMinutes = StreakDays.utcOffsetMinutes(clock),
            today = StreakDays.dayId(clock, 0),
            reminderHour = reminder,
        )
    }

    /** `submitStreakDay` başarılı döndü (Android: addOnSuccessListener gövdesi). */
    fun onSubmitSucceeded(repo: StreakRepository, request: StreakSubmitRequest, response: StreakServerState) {
        repo.markDailyPing()
        val sent = request.reminderHour
        if (sent != null) repo.onReminderHourSynced(sent)
        response.reminderLocalHour?.let { repo.adoptServerReminderHour(it) }
        repo.onSyncAccepted(request.days, response.current, response.longest, response.claimed)
        adopt(repo, response)
    }

    /** `streak/state` dokümanı okundu (Android: refreshFromServer). */
    fun onDocumentRead(repo: StreakRepository, doc: StreakServerState) {
        doc.reminderLocalHour?.let { repo.adoptServerReminderHour(it) }
        adopt(repo, doc)
    }

    private fun adopt(repo: StreakRepository, s: StreakServerState) {
        repo.adoptServerState(
            current = s.current,
            longest = s.longest,
            lastDay = s.lastDay,
            claimed = s.claimed,
            recentDays = s.recentDays,
            goalMinutes = s.goalMinutes,
            challengeDays = s.challengeDays,
            challengeClaimed = s.challengeClaimed,
            freezes = s.freezes,
            freezeDay = s.freezeDay,
            frozenDays = s.frozenDays,
        )
    }

    /** `buyStreakFreeze` başarılı döndü; `freezes` yoksa geçersiz yanıt (Android: ui_satin_alinamadi_birazdan_tekrar_dene). */
    fun onFreezeBought(repo: StreakRepository, freezes: Int, freezeDay: String) {
        repo.markFreezePurchased(freezes, freezeDay)
    }

    /** `claimStreakReward` (kilometre taşı) başarılı döndü. */
    fun onMilestoneClaimed(repo: StreakRepository, milestone: Int) {
        repo.markMilestoneClaimed(milestone)
    }

    /** `claimStreakReward` (meydan okuma) başarılı döndü; sunucunun döndürdüğü gün yoksa yerel seçim. */
    fun onChallengeClaimed(repo: StreakRepository, serverChallengeDays: Int) {
        val days = if (serverChallengeDays > 0) serverChallengeDays else repo.chosenChallengeDays()
        repo.markChallengeClaimed(days)
    }

    /** Ödül toplama gövdeleri. */
    fun milestonePayload(milestone: Int): Map<String, Any> = mapOf("milestone" to milestone)

    fun challengePayload(): Map<String, Any> = mapOf("kind" to "challenge")

    fun freezePayload(clock: StreakClock): Map<String, Any> = mapOf("today" to StreakDays.dayId(clock, 0))
}
