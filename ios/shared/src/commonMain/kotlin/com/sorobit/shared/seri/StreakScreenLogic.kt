// Kaynak (Android):
//  - app/src/main/java/com/example/app/StreakFragment.kt (render, renderChallenge, renderRewards)
//  - app/src/main/java/com/example/app/StreakViews.kt (buildMilestoneRows, buildWeekStrip, buildReminderRows)
//  - app/src/main/java/com/example/app/MainActivity.kt (showStreakCelebration metinleri, refreshStreakUi
//    renkleri, takeFreezeNotice metinleri)
//
// Ekranların saf kararları: hangi metin anahtarı, hangi argümanlar, hangi satır görünür. Metin
// anahtarları Android `strings` adlarıdır; biçimleme platformda (`Metin.al(anahtar, argümanlar)`).
package com.sorobit.shared.seri

/** Biçimlenecek metin: anahtar + tamsayı argümanları (sırasıyla). */
data class StreakText(val key: String, val args: List<Int>)

/** Seri ekranının (StreakFragment) tamamı. */
data class StreakScreenUi(
    val alive: Boolean,
    val bigNumber: Int,
    val bigLabel: StreakText,
    val bigSubtitle: StreakText,
    /** Bugünkü çubuğun yazısı ("2 / 5 dakika" ya da "Hedef tamamlandı ✓"). */
    val todayText: StreakText,
    /** 0–100. */
    val todayPercent: Float,
    val todayComplete: Boolean,
    /** Meydan okuma kartı; gizliyse null. */
    val challenge: StreakChallengeUi?,
    /** Kilometre taşı satırları (SUNUCUNUN serisine göre). */
    val milestones: List<StreakMilestoneRowUi>,
    val longest: Int,
    val freezeHeld: Boolean,
    /** Seri dondurma satırı tıklanabilir mi (yalnızca elde yokken; mağazaya götürür). */
    val freezeRowClickable: Boolean,
    val reminderHour: Int,
)

/** Meydan okuma kartı (streakChallengeCard). */
data class StreakChallengeUi(
    val days: Int,
    val reward: Int,
    val title: StreakText,
    val subtitle: StreakText,
    /** Ödül rozeti ("+500" altın) görünür mü. */
    val rewardBadgeVisible: Boolean,
    /** "Ödülü topla" düğmesi görünür mü (sunucunun serisi yetiyor ve alınmamış). */
    val claimVisible: Boolean,
    /** Çubuğun üstündeki yazı: tamamlandıysa "ui_tamamlandi_2" anahtarı, değilse null ve [progressText]. */
    val progressKey: String?,
    /** "1 / 3" (RtlSupport.ltr Türkçede etkisiz). */
    val progressText: String,
    /** 0–100. */
    val percent: Float,
    val complete: Boolean,
)

/** Kilometre taşı satırı (StreakViews.buildMilestoneRows). */
data class StreakMilestoneRowUi(
    val milestone: Int,
    val keys: Int,
    val gold: Int,
    val claimable: Boolean,
    /** Kalan gün (toplanabilir değilse). */
    val daysLeft: Int,
)

/** Hafta şeridinin bir günü (StreakViews.buildWeekStrip). */
data class StreakWeekDayUi(
    val dayId: String,
    /** "ui_week_initials_monday_first" dizisindeki sıra (0 = Pazartesi). */
    val index: Int,
    val done: Boolean,
    val frozen: Boolean,
    val today: Boolean,
)

/** Seçenek penceresinin satırı (StreakViews.buildOptionRows). */
data class StreakOptionRowUi(
    /** Satırın değeri; "Başka bir saat" satırında [StreakScreenLogic.CUSTOM_HOUR_ROW]. */
    val value: Int,
    /** Sol metin: anahtar ya da (anahtarı olmayan) hazır metin. */
    val labelKey: String?,
    val labelLiteral: String?,
    /** Sol metnin argümanı (çoğul "%1$@ gün"); yoksa -1. */
    val labelPluralCount: Int,
    /** Sağ metin: anahtar (+argüman) ya da hazır metin. */
    val trailingKey: String?,
    val trailingArg: Int,
    val trailingLiteral: String?,
    /** Sağ metin çoğul anahtar mı ("ui_x_dakika"). */
    val trailingPlural: Boolean,
    val selected: Boolean,
)

object StreakScreenLogic {

    /** "Başka bir saat" satırının değeri (StreakViews.CUSTOM_HOUR_ROW). */
    const val CUSTOM_HOUR_ROW = -1

    /** Renkler (Android: StreakFragment / StreakViews sabitleri). */
    const val COLOR_ACCENT = "#FF9800"
    const val COLOR_DEAD = "#78909C"
    const val COLOR_FREEZE = "#4FC3F7"
    const val COLOR_MUTED = "#93A5B3"
    const val COLOR_GOLD = "#FFC107"

    /** Kutlama şeridinin ekranda kalma süresi (MainActivity.STREAK_CELEBRATION_MS). */
    const val CELEBRATION_MS = 4000L

    /** StreakFragment.render. */
    fun screen(
        state: StreakState,
        chosenChallengeDays: Int,
        challengeClaimed: Int,
        challengeClaimedDay: String,
        today: String,
        serverCurrent: Int,
        claimedMilestones: Set<Int>,
        reminderHour: Int,
    ): StreakScreenUi {
        val alive = state.current > 0
        val bigLabel = if (alive) StreakText("ui_gun_ust_uste", emptyList())
        else StreakText("ui_serin_su_anda_kirik", emptyList())
        val subtitle = when {
            state.goalReachedToday -> StreakText("ui_bugunku_hedefini_tamamladin_yarin_da_bekleri", emptyList())
            alive -> StreakText("ui_bugun_x_dakika_daha_calis_serin", listOf(state.minutesLeft))
            else -> StreakText("ui_bugun_x_dakika_calis_yeni_serini", listOf(state.goalMinutes))
        }
        val minutesToday = state.secondsToday / 60
        val todayText = if (state.goalReachedToday) StreakText("ui_hedef_tamamlandi", emptyList())
        else StreakText("ui_x_x_dakika", listOf(minutesToday, state.goalMinutes))
        return StreakScreenUi(
            alive = alive,
            bigNumber = state.current,
            bigLabel = bigLabel,
            bigSubtitle = subtitle,
            todayText = todayText,
            todayPercent = state.todayFraction * 100f,
            todayComplete = state.goalReachedToday,
            challenge = challenge(state, chosenChallengeDays, challengeClaimed, challengeClaimedDay, today, serverCurrent),
            milestones = milestones(serverCurrent, claimedMilestones),
            longest = state.longest,
            freezeHeld = state.freezeHeld,
            freezeRowClickable = !state.freezeHeld,
            reminderHour = reminderHour,
        )
    }

    /** StreakFragment.renderChallenge; kart gizliyse null. */
    fun challenge(
        state: StreakState,
        chosenChallengeDays: Int,
        challengeClaimed: Int,
        challengeClaimedDay: String,
        today: String,
        serverCurrent: Int,
    ): StreakChallengeUi? {
        val days = chosenChallengeDays
        val reward = StreakMilestones.challengeReward(days)
        val claimedToday = challengeClaimed > 0 && challengeClaimedDay == today
        if (days <= 0 || reward <= 0 || (challengeClaimed > 0 && !claimedToday)) return null
        val done = state.current.coerceIn(0, days)
        val complete = state.current >= days
        val claimable = challengeClaimed <= 0 && serverCurrent >= days
        val sub = when {
            challengeClaimed > 0 -> StreakText("ui_tamamlandi_buyuk", emptyList())
            claimable -> StreakText("ui_tamamlandi_odulunu_topla", emptyList())
            complete -> StreakText("ui_tamamlandi_odulun_hazirlaniyor", emptyList())
            state.current <= 0 -> StreakText("ui_bugun_basliyor", emptyList())
            else -> StreakText("ui_x_gunun_x_gunu", listOf(days, done))
        }
        val fraction = (done.toFloat() / days).coerceIn(0f, 1f)
        return StreakChallengeUi(
            days = days,
            reward = reward,
            title = StreakText("ui_x_gunluk_meydan_okuma", listOf(days)),
            subtitle = sub,
            rewardBadgeVisible = challengeClaimed <= 0,
            claimVisible = claimable,
            progressKey = if (complete) "ui_tamamlandi_2" else null,
            progressText = "$done / $days",
            percent = fraction * 100f,
            complete = complete,
        )
    }

    /** StreakViews.buildMilestoneRows (streak = SUNUCUNUN serisi). */
    fun milestones(serverStreak: Int, claimed: Set<Int>): List<StreakMilestoneRowUi> =
        StreakMilestones.visible(serverStreak, claimed, StreakMilestones.VISIBLE_COUNT).mapNotNull { m ->
            val reward = StreakMilestones.rewardFor(m) ?: return@mapNotNull null
            StreakMilestoneRowUi(
                milestone = m,
                keys = reward.keys,
                gold = reward.gold,
                claimable = m <= serverStreak,
                daysLeft = m - serverStreak,
            )
        }

    /** StreakViews.buildWeekStrip: tutturulmuş gün donmuş günden önce gelir. */
    fun weekStrip(clock: StreakClock, achievedDays: Set<String>, frozenDays: Set<String>): List<StreakWeekDayUi> {
        val today = StreakDays.dayId(clock, 0)
        return StreakDays.weekDayIds(clock).mapIndexed { i, day ->
            val done = day in achievedDays
            StreakWeekDayUi(
                dayId = day,
                index = i,
                done = done,
                frozen = !done && day in frozenDays,
                today = day == today,
            )
        }
    }

    /** Günlük hedef penceresi (StreakFragment.showGoalPicker): Rahat/Düzenli/Ciddi + "N dakika". */
    fun goalRows(selected: Int): List<StreakOptionRowUi> =
        StreakRules.GOAL_OPTIONS.mapIndexed { i, minutes ->
            StreakOptionRowUi(
                value = minutes,
                labelKey = StreakRules.GOAL_LABEL_KEYS.getOrElse(i) { "" },
                labelLiteral = null,
                labelPluralCount = -1,
                trailingKey = "ui_x_dakika",
                trailingArg = minutes,
                trailingLiteral = null,
                trailingPlural = true,
                selected = minutes == selected,
            )
        }

    /** Meydan okuma seçenekleri (NewStreakFragment.renderOptions): "N gün" + "+X altın" (altın renkli). */
    fun challengeRows(picked: Int): List<StreakOptionRowUi> =
        StreakRules.CHALLENGE_OPTIONS.map { days ->
            StreakOptionRowUi(
                value = days,
                labelKey = "ui_x_gun",
                labelLiteral = null,
                labelPluralCount = days,
                trailingKey = "ui_x_altin_2",
                trailingArg = StreakMilestones.challengeReward(days),
                trailingLiteral = null,
                trailingPlural = false,
                selected = days == picked,
            )
        }

    /** Hatırlatma satırları (StreakViews.buildReminderRows): hazır saatler + "Başka bir saat". */
    fun reminderRows(selected: Int): List<StreakOptionRowUi> {
        val hours = StreakRules.REMINDER_HOUR_OPTIONS
        val custom = selected !in hours
        val rows = hours.map { hour ->
            StreakOptionRowUi(
                value = hour,
                labelKey = StreakRules.reminderHourLabelKey(hour),
                labelLiteral = StreakRules.reminderHourHardcodedLabel(hour)
                    ?: if (StreakRules.reminderHourLabelKey(hour) == null) StreakRules.hourText(hour) else null,
                labelPluralCount = -1,
                trailingKey = null,
                trailingArg = -1,
                trailingLiteral = StreakRules.hourText(hour),
                trailingPlural = false,
                selected = !custom && hour == selected,
            )
        }
        val customRow = StreakOptionRowUi(
            value = CUSTOM_HOUR_ROW,
            labelKey = "ui_baska_bir_saat",
            labelLiteral = null,
            labelPluralCount = -1,
            trailingKey = if (custom && selected in 0..23) null else "ui_sec",
            trailingArg = -1,
            trailingLiteral = if (custom && selected in 0..23) StreakRules.hourText(selected) else null,
            trailingPlural = false,
            selected = custom,
        )
        return rows + customRow
    }

    /** Kutlama şeridinin alt metni (MainActivity.showStreakCelebration). */
    fun celebrationSubtitle(streak: Int): StreakText =
        if (streak <= 1) StreakText("ui_serin_basladi", emptyList())
        else StreakText("ui_x_gunluk_seri", listOf(streak))

    /** Dondurma harcandı bildiriminin metin anahtarı (MainActivity.refreshStreakUi Toast). */
    fun freezeNoticeKey(notice: StreakFreezeNotice): String = when (notice) {
        StreakFreezeNotice.SAVED -> "ui_dun_calisamadin_ama_seri_dondurman_serini"
        StreakFreezeNotice.LOST -> "ui_seri_dondurman_bir_gunu_kapatti_ama"
    }

    /**
     * Başarısız sunucu çağrısında gösterilecek metin (StreakSyncService.callErrorMessage). Kod
     * FirebaseFunctionsException kodudur ("unavailable", "internal", …); taşıma hatasında null.
     *
     * @param failedKey İşlemin "olmadı" cümlesi: "ui_odul_alinamadi" ya da "ui_satin_alinamadi".
     * @return Metin anahtarı ve (varsa) sunucunun Türkçe mesajı; [StreakErrorText.literal] doluysa o gösterilir.
     */
    fun errorText(code: String?, serverMessage: String?, failedKey: String, turkish: Boolean): StreakErrorText = when (code) {
        null -> StreakErrorText("ui_x_internetini_kontrol_edip_tekrar_dene", failedKey, null)
        "unavailable", "deadline-exceeded" -> StreakErrorText("ui_sunucuya_su_an_ulasilamiyor_birazdan_tekrar", null, null)
        "internal", "unknown" -> StreakErrorText("ui_x_internetini_kontrol_edip_tekrar_dene", failedKey, null)
        "not-found", "unimplemented" -> StreakErrorText("ui_x_birazdan_tekrar_dene", failedKey, null)
        else -> if (!serverMessage.isNullOrBlank() && turkish) StreakErrorText(null, null, serverMessage)
        else StreakErrorText("ui_x_birazdan_tekrar_dene", failedKey, null)
    }
}

/** Hata metni: [key] (argümanı başka bir anahtarın metni: [argKey]) ya da sunucunun hazır metni. */
data class StreakErrorText(val key: String?, val argKey: String?, val literal: String?)
