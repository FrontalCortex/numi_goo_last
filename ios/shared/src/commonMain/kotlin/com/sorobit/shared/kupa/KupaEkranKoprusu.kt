// iOS kupa / rozet / liderlik ekranlarının (ios/iosApp/iosApp/Kupa/) ortak mantığı çağırdığı ince katman.
//
// Yeni kural İÇERMEZ; var olan ortak mantığı (CupHistoryLogic, RecordLogic, SeasonClock, LessonLeaderboardRepository,
// BadgeDetailLogic, BadgeFragmentLogic, BadgeProgressRepository) Swift'ten güvenle çağrılabilecek biçime sokar:
// varsayılan parametre yok, kutulanan null'lu ilkel tip yok, Pair / iç içe sınıf / enum yok — yalnızca
// String, Int, Long, Float, Boolean ve bunların listeleri.
//
// Tek bileşim kuralı [KupaEkranKoprusu.rozetEkrani]'dir; kaynağı:
//   app/src/main/java/com/example/app/BadgeFragment.kt — onViewCreated (kutlama akışı DIŞINDAKİ dal),
//   showLockedStaticBadge (mod → Lottie tablosu), updateProgressContainerVisibility, applyBadgeDetailText,
//   configureMedalPieceNamesList, togglePieceRowsPanel.
// Liderlik satırı bileşimi: RecordFragment.bindLeaderboard + RecordLeaderboardAdapter.VH.bind.
package com.sorobit.shared.kupa

import com.sorobit.shared.kupa.rozet.BadgeAnimMode
import com.sorobit.shared.kupa.rozet.BadgeDetailLogic
import com.sorobit.shared.kupa.rozet.BadgeFragmentLogic
import com.sorobit.shared.kupa.rozet.BadgeProgressFirestore
import com.sorobit.shared.kupa.rozet.BadgeProgressRepository
import com.sorobit.shared.kupa.sezon.LessonLeaderboardRepository
import com.sorobit.shared.kupa.sezon.RecordLogic
import com.sorobit.shared.kupa.sezon.SeasonClock

/** `users/{uid}/cupHistory/{yyyy-MM-dd}` dokümanından okunan bir nokta. */
data class KupaGecmisNoktasi(val dateId: String, val value: Int)

/** Grafikte çizilecek nokta: x ekseni etiketi + değer. */
data class KupaGrafikNoktasi(val label: String, val value: Int)

/** Android `CupHistoryFragment.renderChart` girdileri. */
data class KupaGrafikVerisi(
    /** true ise "Henüz geçmiş veri yok" gösterilir. */
    val isEmpty: Boolean,
    /** true ise çizgi yerine tek daire + ipucu metni gösterilir. */
    val isSinglePoint: Boolean,
    val points: List<KupaGrafikNoktasi>,
    val axisMin: Int,
    val axisMax: Int,
    /** x ekseninde gösterilecek en çok etiket sayısı. */
    val xAxisLabelCount: Int,
    /**
     * [points] ile aynı sırada, her noktanın "yyyy-MM-dd" tarih kimliği. iOS, Türkçe dışındaki dillerde
     * etiketi bu tarihten seçili dilin yereliyle yeniden biçimler (Android: Locale.getDefault()).
     */
    val dateIds: List<String> = emptyList(),
    /** Etiketin tarih kalıbı ([CupHistoryLogic.labelPattern]: "d MMM" ya da "MMM yy"); boş grafikte boş. */
    val labelPattern: String = "",
)

/** Sezon başlığı: indeks, "s14" etiketi ve geri sayım metni. */
data class SezonVerisi(
    val season: Int,
    val seasonLabel: String,
    val countdownText: String,
    val remainingMs: Long,
)

/** `lessonLeaderboards/{board}/entries/{uid}` dokümanının ekranda kullanılan alanları. */
data class LiderHamKayit(
    val userId: String,
    val recordScore: Int,
    val displayName: String,
    val recordLabel: String,
    /** Android avatarı (AvatarConfig.encode biçimi); boş = avatar yok. Eski `photoUrl` alanı kaldırıldı. */
    val avatarConfig: String,
)

/** Liderlik tablosunun bir satırı (birinci dahil). */
data class LiderSatirVerisi(
    val userId: String,
    /** Rozet üzerinde yazan sıra (1, 2, 3 …). */
    val displayRank: Int,
    /** Rozet rengini belirleyen ödül sırası (aynı puanda paylaşılır). */
    val rewardRank: Int,
    /** Boşsa "Liderlik tablosu" ile doldurulmuş ad. */
    val displayName: String,
    val recordLabel: String,
    /** Boş = varsayılan avatar çizilir (Android `leaderboardAvatarBitmap`). */
    val avatarConfig: String,
    val hasAvatar: Boolean,
    val isCurrentUser: Boolean,
    /** Android drawable adı (bg_record_rank_gold / bg_record_rank_badge_purple …). */
    val rankBadgeDrawable: String,
    /** R.color adları. */
    val cardColorRes: String,
    val nameColorRes: String,
    val timeColorRes: String,
)

/** "Başarılar" ekranındaki bir rozet kutusu (Android `BadgeDetailUiState`). */
data class RozetKutuVerisi(
    /** [BadgeAnimMode] adı; [KupaEkranKoprusu.rozetEkrani]'ne verilir. */
    val modeName: String,
    val title: String,
    val subtext: String,
    val valueText: String,
    val showCount: Boolean,
    val unlocked: Boolean,
    /** BadgeLevelTone adı ("BRONZE", "SILVER", "ORIGINAL", "RED", "PURPLE"); ton yoksa boş. */
    val toneName: String,
    /** Alt (taban) Lottie dosyası; yoksa boş. */
    val baseAsset: String,
    val topAsset: String,
    val topHoldFrame: Float,
    val topScale: Float,
    val withCircleFrame: Boolean,
)

/** Rozet ekranındaki madalya/kupa parça satırı. */
data class RozetParcaVerisi(
    val titleUnit: String,
    /** Kupa satırında sıra; madalya satırında boş. */
    val rankText: String,
    val seasonText: String,
)

/** Tek rozet ekranı (Android `BadgeFragment`, kutlama akışı dışı). */
data class RozetEkranVerisi(
    val modeName: String,
    val unlocked: Boolean,
    /** Rozetin ortasındaki sayı; [showRewardAmount] false ise boş. */
    val rewardAmountText: String,
    val showRewardAmount: Boolean,
    val detailText: String,
    val toneName: String,
    val baseAsset: String,
    val topAsset: String,
    val topHoldFrame: Float,
    val topScale: Float,
    val withCircleFrame: Boolean,
    val showProgress: Boolean,
    /** "2/10". */
    val progressText: String,
    /** 0..100. */
    val progressPercent: Int,
    /** "3x"; boş = parça düğmesi gizli. */
    val pieceCountText: String,
    val pieceIsCup: Boolean,
    val pieceRows: List<RozetParcaVerisi>,
    /** Parça satırındaki küçük Lottie dosyası; yoksa boş. */
    val pieceAsset: String,
    /** Açık rozette ekrana dokununca üst animasyon yeniden oynar. */
    val replayOnTap: Boolean,
    /** [detailText]'in Android string kaynak adı ([BadgeFragmentLogic.badgeDetailKey]); iOS çevirisi için. */
    val detailKey: String = "",
    /** [detailText]'te "%1\$s" yerine konan değer; şablon değişmiyorsa boş. */
    val detailValue: String = "",
)

object KupaEkranKoprusu {

    // ---------------------------------------------------------------- kupa geçmişi

    private fun tab(tabIndex: Int): CupHistoryLogic.RangeTab =
        CupHistoryLogic.RangeTab.entries[tabIndex.coerceIn(0, CupHistoryLogic.RangeTab.entries.size - 1)]

    /** Sekme başlıkları ("Haftalık", "Aylık", "Yıllık", "Tüm Zamanlar"). */
    fun gecmisSekmeBasliklari(): List<String> = CupHistoryLogic.TAB_LABELS

    /** Sekmenin sorgu penceresi (gün); 0 = tüm geçmiş. */
    fun gecmisPencereGunu(tabIndex: Int): Int = CupHistoryLogic.windowDaysForTab(tab(tabIndex)) ?: 0

    /** Sekmenin `documentId() >= …` başlangıç kimliği ("yyyy-MM-dd"); boş = tüm geçmiş. */
    fun gecmisBaslangicKimligi(tabIndex: Int): String = CupHistoryLogic.sinceDateIdForTab(tab(tabIndex)) ?: ""

    /** Alan yoksa gösterilen güncel skor (200). */
    fun gecmisVarsayilanSkor(): Int = CupHistoryLogic.DEFAULT_CURRENT_SCORE

    /** Android `renderChart`: gruplama, etiketler, y ekseni aralığı. [noktalar] tarihe göre artan sırada olmalıdır. */
    fun gecmisGrafigi(tabIndex: Int, noktalar: List<KupaGecmisNoktasi>): KupaGrafikVerisi {
        val t = tab(tabIndex)
        val bucketed = CupHistoryLogic.bucket(noktalar.map { it.dateId to it.value }, t)
        if (bucketed.isEmpty()) {
            return KupaGrafikVerisi(
                isEmpty = true, isSinglePoint = false, points = emptyList(),
                axisMin = 0, axisMax = 0, xAxisLabelCount = 1,
            )
        }
        val (axisMin, axisMax) = CupHistoryLogic.niceAxisRange(bucketed.map { it.second })
        return KupaGrafikVerisi(
            isEmpty = false,
            isSinglePoint = CupHistoryLogic.isSinglePoint(bucketed),
            points = bucketed.map { KupaGrafikNoktasi(CupHistoryLogic.formatLabel(t, it.first), it.second) },
            axisMin = axisMin,
            axisMax = axisMax,
            xAxisLabelCount = CupHistoryLogic.xAxisLabelCount(bucketed.size),
            dateIds = bucketed.map { it.first },
            labelPattern = CupHistoryLogic.labelPattern(t),
        )
    }

    // ---------------------------------------------------------------- sezon ve liderlik

    fun guncelSezon(): Int = SeasonClock.currentSeason()

    /** @param boundSeason dinleyicinin bağlı olduğu sezon; 1'den küçükse güncel sezon kullanılır. */
    fun sezonVerisi(boundSeason: Int): SezonVerisi = sezonVerisi(boundSeason, kupaNowMs())

    internal fun sezonVerisi(boundSeason: Int, nowMs: Long): SezonVerisi {
        val remaining = SeasonClock.millisUntilCurrentSeasonEnds(nowMs)
        return SezonVerisi(
            season = SeasonClock.currentSeason(nowMs),
            seasonLabel = RecordLogic.seasonIndexLabel(boundSeason, nowMs),
            countdownText = RecordLogic.formatSeasonCountdown(remaining).text,
            remainingMs = remaining,
        )
    }

    /** Geri sayım metninin yenilenme aralığı (ms). */
    fun sezonSayacAraligiMs(): Long = RecordLogic.COUNTDOWN_TICK_MS

    /** Sezon değişimi yoklama aralığı (ms). */
    fun sezonYoklamaAraligiMs(): Long = SeasonClock.SEASON_POLL_INTERVAL_MS

    /** [lessonKey]: dersin kalıcı kimliği (Android `LessonItem.stableId`), liste konumu değil. */
    fun liderlikTahtaKimligi(partId: Int, lessonKey: String, season: Int): String =
        LessonLeaderboardRepository.leaderboardDocumentId(partId, lessonKey, season)

    fun liderlikSorguLimiti(): Int = LessonLeaderboardRepository.LEADERBOARD_LIST_QUERY_LIMIT.toInt()

    /**
     * `orderBy recordScore DESC` sırasıyla gelen kayıtlardan ekran satırları.
     * Android: ilk satır üstte ayrı gösterilir, kalanı listede ([RecordLogic.splitLeaderboard]).
     *
     * @param currentUid oturum açmış kullanıcının kimliği; yoksa boş.
     */
    fun liderSatirlari(kayitlar: List<LiderHamKayit>, currentUid: String): List<LiderSatirVerisi> {
        val entries = LessonLeaderboardRepository.mapLeaderboardEntries(
            kayitlar.map {
                it.userId to mapOf<String, Any?>(
                    LessonLeaderboardRepository.F_SCORE to it.recordScore,
                    LessonLeaderboardRepository.F_NAME to it.displayName,
                    LessonLeaderboardRepository.F_LABEL to it.recordLabel,
                )
            },
            avatarCache = kayitlar.associate { it.userId to it.avatarConfig },
        )
        return entries.map { e ->
            val mine = RecordLogic.isCurrentUser(e.userId, currentUid.ifBlank { null })
            val colors = RecordLogic.rowColors(mine)
            LiderSatirVerisi(
                userId = e.userId,
                displayRank = e.displayRank,
                rewardRank = e.rewardRank,
                displayName = RecordLogic.displayNameOrFallback(e.displayName),
                recordLabel = e.recordLabel,
                avatarConfig = e.avatarConfig ?: "",
                hasAvatar = RecordLogic.hasAvatar(e.avatarConfig),
                isCurrentUser = mine,
                rankBadgeDrawable = RecordLogic.badgeDrawableForRank(e.rewardRank),
                cardColorRes = colors.cardColor,
                nameColorRes = colors.nameColor,
                timeColorRes = colors.timeColor,
            )
        }
    }

    /** Android `listenLeaderboard.onError` metni. */
    fun liderlikHataMetni(yerelIleti: String, yetkiHatasi: Boolean): String =
        RecordLogic.leaderboardErrorMessage(yerelIleti.ifBlank { null }, yetkiHatasi)

    // ---------------------------------------------------------------- rozetler

    /**
     * `users/{uid}/badgeProgress/state` dokümanını rozet deposuna yükler
     * (Android: MainActivity.refreshUserBadgeProgressFromFirestore → BadgeProgressRepository.update).
     */
    fun rozetDurumunuYukle(doc: Map<String, Any?>?) {
        BadgeProgressRepository.update(BadgeProgressFirestore.userBadgeProgressFromStateSnapshot(doc))
    }

    private fun kutu(s: BadgeDetailLogic.BadgeDetailUiState): RozetKutuVerisi = RozetKutuVerisi(
        modeName = s.targetMode.name,
        title = s.title,
        subtext = s.subtext,
        valueText = s.progressCount.toString(),
        showCount = s.showCount,
        unlocked = s.unlocked,
        toneName = s.levelTone?.name ?: "",
        baseAsset = s.baseAsset ?: "",
        topAsset = s.topAsset,
        topHoldFrame = s.topHoldFrame,
        topScale = s.topScale,
        withCircleFrame = s.topBackgroundRes != null,
    )

    /** "Kişisel Rekorlar" şeridi (madalyalar, kupa, siyah kuşak). */
    fun kisiselRekorlar(): List<RozetKutuVerisi> = BadgeDetailLogic.personalRecordBadges().map { kutu(it) }

    /** "Rozetler" ızgarası (satır başına [rozetSatirBoyu] kutu). */
    fun rozetler(): List<RozetKutuVerisi> = BadgeDetailLogic.awardBadges().map { kutu(it) }

    fun rozetSatirBoyu(): Int = BadgeDetailLogic.AWARDS_PER_ROW

    private data class StatikGorunum(
        val baseAsset: String?,
        val topAsset: String,
        val holdFrame: Float,
        val topScale: Float,
        val withCircleFrame: Boolean,
    )

    private const val TABAN = "daily_tasks_complite_badge2.json"

    /** Android `showLockedStaticBadge` tablosu (mod → taban, üst animasyon, tutma karesi, ölçek, daire çerçeve). */
    private fun statikGorunum(mode: BadgeAnimMode): StatikGorunum = when (mode) {
        BadgeAnimMode.DAILY_ONLY ->
            StatikGorunum(TABAN, "dart_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_DART, 0.65f, false)
        BadgeAnimMode.ROCKET_WITH_BASE ->
            StatikGorunum(TABAN, "rocket_badge_anim2.json", BadgeFragmentLogic.TOP_HOLD_FRAME_ROCKET, 1f, false)
        BadgeAnimMode.BOWLING_WITH_BASE ->
            StatikGorunum(TABAN, "bowling_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_BOWLING, 0.68f, true)
        BadgeAnimMode.FISHING_WITH_BASE ->
            StatikGorunum(TABAN, "fishing_pole_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_FISHING, 0.65f, false)
        BadgeAnimMode.GOLF_WITH_BASE ->
            StatikGorunum(TABAN, "golf_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_GOLF, 0.60f, false)
        BadgeAnimMode.CUP_GOOGLE ->
            StatikGorunum(null, "cup_google_anim.json", BadgeFragmentLogic.CUP_GOOGLE_HOLD_FRAME, 1f, false)
        BadgeAnimMode.KARATE ->
            StatikGorunum(null, "karate_anim.json", BadgeFragmentLogic.KARATE_HOLD_FRAME, 1f, false)
        BadgeAnimMode.TORNADO ->
            StatikGorunum(TABAN, "tornado_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_TORNADO, 0.65f, true)
        BadgeAnimMode.VOLCANO ->
            StatikGorunum(TABAN, "volcano_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_VOLCANO, 0.65f, true)
        BadgeAnimMode.DINO ->
            StatikGorunum(TABAN, "dinosaur_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_GOLF, 0.5f, false)
        BadgeAnimMode.CROCODILE ->
            StatikGorunum(TABAN, "crocodile_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_GOLF, 0.50f, false)
        BadgeAnimMode.GOAT ->
            StatikGorunum(TABAN, "goat_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_GOLF, 0.50f, false)
        BadgeAnimMode.EAGLE ->
            StatikGorunum(TABAN, "eagle_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_GOLF, 0.50f, false)
        BadgeAnimMode.FLY ->
            StatikGorunum(TABAN, "fly_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_GOLF, 0.50f, false)
        BadgeAnimMode.TURTLE ->
            StatikGorunum(TABAN, "turtle_anim.json", BadgeFragmentLogic.TOP_HOLD_FRAME_GOLF, 0.50f, false)
        BadgeAnimMode.GOLD_MEDAL -> StatikGorunum(null, "gold_medal_anim.json", 0f, 1f, false)
        BadgeAnimMode.SILVER_MEDAL -> StatikGorunum(null, "silver_medal_anim.json.json", 0f, 1f, false)
        BadgeAnimMode.BRONZE_MEDAL -> StatikGorunum(null, "bronze_medal_anim.json.json", 0f, 1f, false)
    }

    /** Android'de `lockedProgressCurrent = 2`, `lockedProgressTarget = 10` alan varsayılanları. */
    private const val VARSAYILAN_ILERLEME = 2
    private const val VARSAYILAN_HEDEF = 10

    /**
     * Tek rozet ekranının durumu (Android `BadgeFragment.newInstance(mode, unlocked)` ile açılan görünüm).
     * Android'deki gibi açık/kilitli bilgisi argümandan değil rozet deposundan okunur.
     *
     * @param modeName [BadgeAnimMode] adı; bilinmiyorsa DAILY_ONLY.
     */
    fun rozetEkrani(modeName: String): RozetEkranVerisi {
        val mode = BadgeAnimMode.entries.firstOrNull { it.name == modeName } ?: BadgeAnimMode.DAILY_ONLY
        val state = BadgeProgressRepository.getStateByMode(mode)
        val unlocked = state?.unlocked ?: true
        val rewardAmount: Int? = if (mode == BadgeAnimMode.KARATE && !unlocked) {
            1
        } else {
            state?.value ?: BadgeFragmentLogic.defaultRewardAmountForMode(mode)
        }
        val gorunum = statikGorunum(mode)

        val showProgress = BadgeFragmentLogic.shouldShowProgressForMode(mode)
        val window = if (showProgress) BadgeProgressRepository.getProgressWindowByMode(mode) else null
        val progressUi = BadgeFragmentLogic.lockedProgressUi(
            window?.current ?: VARSAYILAN_ILERLEME,
            window?.target ?: VARSAYILAN_HEDEF,
        )

        val progress = BadgeProgressRepository.getUserBadgeProgress()
        val piece = BadgeFragmentLogic.pieceCountBadge(
            mode = mode,
            unlocked = unlocked,
            progress = progress,
            isCelebrationFlow = false,
            seasonLbPieceFilter = null,
        )
        val pieceRows: List<RozetParcaVerisi> = when {
            piece == null -> emptyList()
            piece.isCup -> BadgeFragmentLogic.cupPieceRows(progress).map {
                RozetParcaVerisi(it.titleUnit, it.rank.toString(), it.season.toString())
            }
            else -> BadgeFragmentLogic.medalPieceRows(mode, progress).map {
                RozetParcaVerisi(it.titleUnit, "", it.season.toString())
            }
        }
        val pieceAsset = when {
            piece == null -> ""
            piece.isCup -> BadgeFragmentLogic.CUP_PIECE_ASSET
            else -> BadgeFragmentLogic.medalPieceAsset(mode) ?: ""
        }

        return RozetEkranVerisi(
            modeName = mode.name,
            unlocked = unlocked,
            rewardAmountText = rewardAmount?.toString() ?: "",
            showRewardAmount = rewardAmount != null,
            detailText = BadgeFragmentLogic.badgeDetailText(mode, unlocked),
            toneName = if (unlocked) state?.levelTone?.name ?: "" else "",
            baseAsset = gorunum.baseAsset ?: "",
            topAsset = gorunum.topAsset,
            topHoldFrame = gorunum.holdFrame,
            topScale = gorunum.topScale,
            withCircleFrame = gorunum.withCircleFrame,
            showProgress = showProgress,
            progressText = progressUi.text,
            progressPercent = progressUi.percent,
            pieceCountText = piece?.text ?: "",
            pieceIsCup = piece?.isCup ?: false,
            pieceRows = pieceRows,
            pieceAsset = pieceAsset,
            replayOnTap = unlocked,
            detailKey = BadgeFragmentLogic.badgeDetailKey(mode, unlocked),
            detailValue = BadgeFragmentLogic.badgeDetailProgress(mode, unlocked)?.toString() ?: "",
        )
    }
}
