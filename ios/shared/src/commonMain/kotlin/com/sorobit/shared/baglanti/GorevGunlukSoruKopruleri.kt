// Kaynak (Fragment içinde kalmış akışların kopyası):
//  - MissionsFragment.kt: updateMissionsUI (okuma sırası ve satır verisi) → [gorevEkraniDurumu]
//  - TasksFragment.kt: defaultDailyCardState → [gunlukSoruVarsayilanKart];
//    onDailyQuestionProgressClaimTapped içindeki `copy(rewardClaimed = true, isLoaded = true)` →
//    [gunlukSoruOdulAlindiKart]; loadDailyQuestionSources (part 1 + 2 + 3) → [gunlukSoruKaynaklari]
//  - DailyQuestionRepository.kt: loadOrCreateChallenge / createAndPersistChallenge /
//    loadChallengeForCard / markRewardClaimed / markPendingDiamondContinue /
//    clearPendingDiamondContinue / incrementSolvedCount → [BellekIciGunlukSoruDeposu]
//    (Firestore belgesi yerine bellekteki bir Map; adımların sırası ve sonuçları aynı)
// Android karşılığı olmayanlar (iOS'ta Firebase bağlanana dek kullanılan yer tutucular ve
// Swift'e taşınmayan varsayılan parametreleri dolduran kurucular):
//  - [BellekIciGorevBulutu], [gorevDeposuKur], [bellekIciGorevDeposuKur]
// Var olan dosyaların davranışı değişmez; buradaki her şey onların üstüne kuruludur.
package com.sorobit.shared.baglanti

import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.oyun.DailyQuestionCardUiState
import com.sorobit.shared.oyun.DailyQuestionChallenge
import com.sorobit.shared.oyun.DailyQuestionPeriod
import com.sorobit.shared.oyun.DailyQuestionPoolBuilder
import com.sorobit.shared.oyun.DailyQuestionRepository
import com.sorobit.shared.oyun.DailyQuestionSource
import com.sorobit.shared.oyun.InMemoryDailyQuestionChallengeCache
import com.sorobit.shared.oyun.InMemoryKeyValueStore
import com.sorobit.shared.oyun.MissionCloudGateway
import com.sorobit.shared.oyun.MissionCountdownLabel
import com.sorobit.shared.oyun.MissionQuestData
import com.sorobit.shared.oyun.MissionUiLogic
import com.sorobit.shared.oyun.MissionWindow
import com.sorobit.shared.oyun.MissionsProgressStore
import com.sorobit.shared.oyun.OyunKeyValueStore
import com.sorobit.shared.oyun.oyunSimdiMs
import kotlin.math.pow
import kotlin.math.roundToInt

// ── Görevler ────────────────────────────────────────────────────────────────

/**
 * Bellekte duran `users/{uid}/missionProgress/state` belgesi. Firestore bağlanana dek
 * [MissionCloudGateway] yerine geçer; [uid] null ise misafir gibi davranır (hiç okuma/yazma olmaz).
 */
class BellekIciGorevBulutu(
    private val uid: String?,
    baslangicBelgesi: Map<String, Any?>?,
) : MissionCloudGateway {
    /** Son yazılan (ya da başlangıçta verilen) belge; yoksa null. */
    var belge: Map<String, Any?>? = baslangicBelgesi
        private set

    override fun currentUid(): String? = uid

    override fun fetchState(uid: String, onSuccess: (Map<String, Any?>?) -> Unit, onFailure: () -> Unit) {
        onSuccess(belge)
    }

    override fun fetchStateBlocking(uid: String): Map<String, Any?>? = belge

    override fun setState(uid: String, payload: Map<String, Any>) {
        belge = payload
    }

    /** Başka cihaz olmadığı için dinleyici hiç tetiklenmez. */
    override fun listenState(uid: String, onSnapshot: (Map<String, Any?>?) -> Unit): () -> Unit = {}
}

/**
 * [MissionsProgressStore] kurucusu; saat ve saat dilimi Android'deki gibidir
 * (`System.currentTimeMillis()`, yerel saat dilimi). Swift varsayılan parametreleri göremediği için var.
 */
fun gorevDeposuKur(
    prefsProvider: (prefsName: String) -> OyunKeyValueStore,
    cloud: MissionCloudGateway,
    elapsedRealtimeMs: () -> Long,
): MissionsProgressStore = MissionsProgressStore(prefsProvider, cloud, elapsedRealtimeMs)

/**
 * Kalıcı depo ve bulut bağlanana dek kullanılan görev deposu: tercih dosyaları ve bulut belgesi bellekte.
 * [ornekSayaclar] boş değilse (anahtarlar [MissionsProgressStore] `KEY_*_COUNT` sabitleri) bu değerler
 * "buluttan gelmiş" gibi yüklenir; boşsa misafir kullanıcı gibi sıfırdan başlar.
 */
fun bellekIciGorevDeposuKur(
    elapsedRealtimeMs: () -> Long,
    ornekSayaclar: Map<String, Int>,
): MissionsProgressStore {
    val dosyalar = mutableMapOf<String, OyunKeyValueStore>()
    val bulut = if (ornekSayaclar.isEmpty()) {
        BellekIciGorevBulutu(uid = null, baslangicBelgesi = null)
    } else {
        BellekIciGorevBulutu(uid = "ornek", baslangicBelgesi = ornekSayaclar)
    }
    return gorevDeposuKur(
        prefsProvider = { ad -> dosyalar.getOrPut(ad) { InMemoryKeyValueStore() } },
        cloud = bulut,
        elapsedRealtimeMs = elapsedRealtimeMs,
    )
}

/** Görevler ekranının tek seferde okunan durumu (`MissionsFragment.updateMissionsUI`). */
data class GorevEkraniDurumu(
    val weekly: List<MissionQuestData>,
    val daily: List<MissionQuestData>,
    val weeklyCountdown: MissionCountdownLabel,
    val dailyCountdown: MissionCountdownLabel,
)

/** `MissionsFragment.updateMissionsUI`: okuma sırası Android'deki ile aynıdır. */
fun gorevEkraniDurumu(store: MissionsProgressStore): GorevEkraniDurumu {
    val snap = store.getSnapshot()
    val dailyHours = store.hoursUntilDailyReset()
    val weeklyMs = store.millisUntilWeeklyReset()
    val weekly = store.selectedMissionsForWeekly().map { mission ->
        MissionUiLogic.questData(
            mission = mission,
            window = MissionWindow.WEEKLY,
            snapshot = snap,
            isClaimed = store.isMissionRewardClaimed(MissionWindow.WEEKLY, mission.id),
        )
    }
    val daily = store.selectedMissionsForDaily().map { mission ->
        MissionUiLogic.questData(
            mission = mission,
            window = MissionWindow.DAILY,
            snapshot = snap,
            isClaimed = store.isMissionRewardClaimed(MissionWindow.DAILY, mission.id),
        )
    }
    return GorevEkraniDurumu(
        weekly = weekly,
        daily = daily,
        weeklyCountdown = MissionUiLogic.formatWeeklyCountdown(weeklyMs),
        dailyCountdown = MissionUiLogic.dailyCountdownLabel(dailyHours),
    )
}

// ── Günlük soru ─────────────────────────────────────────────────────────────

/** `TasksFragment.defaultDailyCardState`: veri gelmeden önce gösterilen kart (isLoaded = false). */
fun gunlukSoruVarsayilanKart(poolAvailable: Boolean, nowMs: Long): DailyQuestionCardUiState =
    DailyQuestionCardUiState(
        periodKey = DailyQuestionPeriod.currentPeriodKey(nowMs),
        solvedCount = 0,
        totalCount = DailyQuestionPeriod.QUESTIONS_PER_PERIOD,
        titleUnit = "",
        difficulty = "",
        isComplete = false,
        rewardClaimed = false,
        poolAvailable = poolAvailable,
        pendingContinueSlotIndex = null,
        isLoaded = false,
    )

/** `TasksFragment.onDailyQuestionProgressClaimTapped`: ödül alındı olarak işaretlenmiş kart. */
fun gunlukSoruOdulAlindiKart(state: DailyQuestionCardUiState): DailyQuestionCardUiState =
    state.copy(rewardClaimed = true, isLoaded = true)

/** `TasksFragment.loadDailyQuestionSources`: part 1, 2 ve 3'ün kaynakları, bu sırayla. */
fun gunlukSoruKaynaklari(
    builder: DailyQuestionPoolBuilder,
    part1Items: List<LessonItem>,
    part2Items: List<LessonItem>,
    part3Items: List<LessonItem>,
): List<DailyQuestionSource> =
    builder.buildSourcesForPart(part1Items.asOyunLessonItems(), 1) +
        builder.buildSourcesForPart(part2Items.asOyunLessonItems(), 2) +
        builder.buildSourcesForPart(part3Items.asOyunLessonItems(), 3)

/**
 * `DailyQuestionRepository`'nin Firestore'a dokunan akışları; belge (`users/{uid}/dailyQuestion/current`)
 * bellekte bir Map olarak durur. Firestore bağlanınca aynı adımlar gerçek belgeyle yapılır.
 *
 * Android'de sonuçlar geri çağrıyla döner; burada G/Ç olmadığı için doğrudan döner.
 */
class BellekIciGunlukSoruDeposu(
    /** `FirebaseAuth.currentUser?.uid`; null/boş ise Android'deki gibi hiçbir şey yüklenmez. */
    private val uid: String?,
    private val builder: DailyQuestionPoolBuilder,
    /**
     * `successRate/dailyQuestionSuccessRate/…` belgesindeki `<zorluk>_rate` yerine dönen örnek değer;
     * null ise "belge yok" gibi davranır.
     */
    private val ornekBasariOrani: Int?,
    private val nowMs: () -> Long,
) {
    /** `users/{uid}/dailyQuestion/current`; null = belge yok. */
    var belge: Map<String, Any?>? = null
        private set

    private val cache = InMemoryDailyQuestionChallengeCache()

    private fun periodKey(): String = DailyQuestionPeriod.currentPeriodKey(nowMs())

    /** `loadOrCreateChallenge` (başarı dalı) + `createAndPersistChallenge`. */
    fun loadOrCreateChallenge(sources: List<DailyQuestionSource>): DailyQuestionChallenge? {
        val u = uid
        if (u.isNullOrBlank()) return null
        val periodKey = periodKey()
        val cacheKey = DailyQuestionRepository.challengeCacheKey(u)
        val parsed = DailyQuestionRepository.resolveLoadedChallenge(belge != null, belge, periodKey)
        if (parsed != null) {
            cache.write(cacheKey, parsed)
            return parsed
        }
        val challenge = builder.buildChallenge(periodKey, u, sources) ?: return null
        cache.write(cacheKey, challenge)
        belge = DailyQuestionRepository.challengeToFirestoreMap(challenge)
        return challenge
    }

    /** `loadChallengeForCard`. */
    fun loadChallengeForCard(sources: List<DailyQuestionSource>): DailyQuestionCardUiState {
        val poolAvailable = sources.isNotEmpty()
        val challenge = loadOrCreateChallenge(sources)
            ?: return DailyQuestionRepository.cardUiStateWithoutChallenge(periodKey(), poolAvailable)
        val state = DailyQuestionRepository.toCardUiState(challenge, poolAvailable)
        val slot = challenge.slotForPlay() ?: return state
        DailyQuestionRepository.difficultyKey(slot.difficulty) ?: return state
        return state.copy(globalSuccessRate = ornekBasariOrani)
    }

    /** `markRewardClaimed`: `set({rewardClaimed: true}, merge)`. */
    fun markRewardClaimed(periodKey: String): Boolean {
        val u = uid
        if (u.isNullOrBlank()) return false
        belge = (belge ?: emptyMap()) + (DailyQuestionRepository.FIELD_REWARD_CLAIMED to true)
        DailyQuestionRepository.cacheRewardClaimed(cache, DailyQuestionRepository.challengeCacheKey(u), periodKey)
        return true
    }

    /** `markPendingDiamondContinue`: `set({pendingContinueSlotIndex: n}, merge)`. */
    fun markPendingDiamondContinue(periodKey: String, slotIndex: Int): Boolean {
        val u = uid
        if (u.isNullOrBlank()) return false
        val safeSlot = DailyQuestionRepository.safePendingContinueSlot(slotIndex)
        belge = (belge ?: emptyMap()) + (DailyQuestionRepository.FIELD_PENDING_CONTINUE_SLOT to safeSlot)
        DailyQuestionRepository.updateCachedPendingContinue(
            cache, DailyQuestionRepository.challengeCacheKey(u), periodKey, safeSlot,
        )
        return true
    }

    /** `clearPendingDiamondContinue`: `update(pendingContinueSlotIndex, delete)`; belge yoksa false. */
    fun clearPendingDiamondContinue(periodKey: String): Boolean {
        val u = uid
        if (u.isNullOrBlank()) return false
        val mevcut = belge
        if (mevcut != null) {
            belge = mevcut - DailyQuestionRepository.FIELD_PENDING_CONTINUE_SLOT
        }
        // Önbellek Firestore sonucu ne olursa olsun güncellenir.
        DailyQuestionRepository.updateCachedPendingContinue(
            cache, DailyQuestionRepository.challengeCacheKey(u), periodKey, null,
        )
        return mevcut != null
    }

    /** `incrementSolvedCount`: yeni sayaç; kullanıcı ya da belge yoksa 0. */
    fun incrementSolvedCount(periodKey: String): Int {
        val u = uid
        if (u.isNullOrBlank()) return 0
        val mevcut = belge ?: return 0
        val current = (mevcut[DailyQuestionRepository.FIELD_SOLVED_COUNT] as? Number)?.toLong()
        val newCount = DailyQuestionRepository.nextSolvedCount(current)
        belge = mevcut - DailyQuestionRepository.FIELD_PENDING_CONTINUE_SLOT +
            (DailyQuestionRepository.FIELD_SOLVED_COUNT to newCount)
        DailyQuestionRepository.cacheSolvedCount(
            cache, DailyQuestionRepository.challengeCacheKey(u), periodKey, newCount,
        )
        return newCount
    }
}

/**
 * [BellekIciGunlukSoruDeposu] kurucusu: asıl üreteçlere bağlı havuz ([gunlukSoruHavuzuKur]) ve sistem saati.
 * Kaynakları üretmek için aynı [DailyQuestionPoolBuilder] gerekir; [gunlukSoruHavuzuKur] ile alınır.
 */
fun bellekIciGunlukSoruDeposuKur(
    uid: String?,
    builder: DailyQuestionPoolBuilder,
    ornekBasariOrani: Int?,
): BellekIciGunlukSoruDeposu =
    BellekIciGunlukSoruDeposu(uid, builder, ornekBasariOrani, ::oyunSimdiMs)

/** [gunlukSoruHavuzuKur]'un parametresiz hâli (Swift varsayılan parametreyi göremez). */
fun gunlukSoruHavuzuVarsayilan(): DailyQuestionPoolBuilder = gunlukSoruHavuzuKur()

// ── Günlük soru: kart ilerleme çubuğu ve soruyu başlatma ────────────────────

/** Kart ilerleme çubuğunun o andaki görünümü (`DailyQuestionVH.applyVisualPercent`). */
enum class GunlukSoruCubukModu {
    /** Ödül alınmış ve çubuk dolu: gri dolgu, "Ödül alındı !". */
    CLAIMED,

    /** 3/3 ve çubuk dolu, ödül bekliyor: altın dolgu, "Ödülü al!". */
    CLAIM_READY,

    /** Sürüyor (ya da animasyon henüz dolmadı): turuncu dolgu, "n/3". */
    PROGRESS,
}

data class GunlukSoruCubukGorunumu(
    val mode: GunlukSoruCubukModu,
    /** Zemin drawable'ının adı. */
    val trackDrawable: String,
    /** Dolgu ve parlama; genişlik <= 0 ise null (Android hiçbir şey çizmez). */
    val overlay: com.sorobit.shared.oyun.MissionProgressOverlay?,
    /** Etiketin string kaynağı; PROGRESS modunda [labelCurrent] ve [labelTotal] ile biçimlenir. */
    val labelResKey: String,
    val labelCurrent: Int,
    val labelTotal: Int,
    /** Etiket renginin kaynak adı. */
    val labelColor: String,
)

/**
 * `TasksFragment.DailyQuestionVH.applyVisualPercent`: [pct] (0–100) görsel yüzdesinde çubuğun durumu.
 * [startInset] / [endGap]: `mission_progress_shine_inset_start` / `mission_progress_shine_gap_end`.
 */
fun gunlukSoruCubukGorunumu(
    ui: DailyQuestionCardUiState,
    pct: Float,
    trackWidth: Int,
    startInset: Int,
    endGap: Int,
): GunlukSoruCubukGorunumu {
    val total = ui.totalCount.coerceAtLeast(1)
    val curEst = (total * pct / 100f).roundToInt().coerceIn(0, total)
    val atFullVisual = pct >= com.sorobit.shared.oyun.DailyQuestionCardLogic.CLAIM_READY_VISUAL_PERCENT
    val showClaimed = ui.rewardClaimed && atFullVisual
    val showClaimReady = ui.canClaimReward && atFullVisual
    return when {
        showClaimed -> GunlukSoruCubukGorunumu(
            mode = GunlukSoruCubukModu.CLAIMED,
            trackDrawable = "mission_progress_track",
            overlay = MissionUiLogic.missionProgressOverlay(
                trackWidth = trackWidth, percent = pct, done = false, claimed = true,
                startInset = startInset, endGap = endGap,
            ),
            labelResKey = MissionUiLogic.RES_REWARD_CLAIMED_LABEL,
            labelCurrent = curEst.coerceAtMost(total),
            labelTotal = total,
            labelColor = "black",
        )
        showClaimReady -> GunlukSoruCubukGorunumu(
            mode = GunlukSoruCubukModu.CLAIM_READY,
            trackDrawable = "daily_question_progress_track",
            overlay = MissionUiLogic.dailyQuestionProgressOverlay(
                trackWidth = trackWidth, percent = pct, complete = true,
                startInset = startInset, endGap = endGap,
            ),
            labelResKey = MissionUiLogic.RES_COMPLETED_LABEL,
            labelCurrent = curEst.coerceAtMost(total),
            labelTotal = total,
            labelColor = "background_color",
        )
        else -> GunlukSoruCubukGorunumu(
            mode = GunlukSoruCubukModu.PROGRESS,
            trackDrawable = "daily_question_progress_track",
            overlay = MissionUiLogic.dailyQuestionProgressOverlay(
                trackWidth = trackWidth, percent = pct, complete = false,
                startInset = startInset, endGap = endGap,
            ),
            labelResKey = "daily_question_progress_format",
            labelCurrent = curEst.coerceAtMost(total),
            labelTotal = total,
            labelColor = "white",
        )
    }
}

/** `ValueAnimator` + `DecelerateInterpolator(1.6f)`: [t] (0..1) anındaki görsel yüzde. */
fun gunlukSoruCubukAnimasyonYuzdesi(startPct: Float, endPct: Float, t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    // DecelerateInterpolator(factor): 1 - (1 - t)^(2 * factor)
    val egri = 1f - (1f - x).toDouble().pow(2.0 * 1.6).toFloat()
    return startPct + (endPct - startPct) * egri
}

/** Kör ders ekranına verilen günlük soru (`BlindingLessonFragment.newDailyQuestionInstance` argümanları). */
data class GunlukSoruBaslatma(
    /** Sayı dizisi sorusu; çarpma sorusunda boş. */
    val sequence: List<Int>,
    /** Çarpma sorusunun alanları; dizi sorusunda üçü de null. */
    val mathFirst: Int?,
    val mathOperator: String?,
    val mathSecond: Int?,
    val periodKey: String,
    val slotIndex: Int,
    val displayIntervalMs: Long?,
    val partId: Int,
)

/** `TasksFragment.launchDailyQuestionLesson`; oynanacak slot yoksa null. */
fun gunlukSoruBaslatma(challenge: DailyQuestionChallenge): GunlukSoruBaslatma? {
    val slot = challenge.slotForPlay() ?: return null
    return baslatmaVerisi(challenge, slot)
}

/**
 * `showDailyQuestionContinuePanel` (anahtarla devam): slot bekleyen sorudur, slot dizini ise
 * bekleme temizlenmiş meydan okumadan (`copy(pendingContinueSlotIndex = null)`) hesaplanır.
 */
fun gunlukSoruDevamBaslatma(challenge: DailyQuestionChallenge): GunlukSoruBaslatma? {
    val slot = challenge.slotForPlay() ?: return null
    return baslatmaVerisi(challenge.copy(pendingContinueSlotIndex = null), slot)
}

private fun baslatmaVerisi(
    challenge: DailyQuestionChallenge,
    slot: com.sorobit.shared.oyun.DailyQuestionSlot,
): GunlukSoruBaslatma {
    val islem = slot.mathOperation
    return GunlukSoruBaslatma(
        sequence = if (islem != null) emptyList() else slot.sequence,
        mathFirst = islem?.firstNumber,
        mathOperator = islem?.operator,
        mathSecond = islem?.secondNumber,
        periodKey = challenge.periodKey,
        slotIndex = challenge.playSlotIndex(),
        displayIntervalMs = slot.displayIntervalMs,
        partId = slot.partId,
    )
}

/** Anahtarla devam sonrası kart: `clearedChallenge.toCardUiState(poolAvailable)`. */
fun gunlukSoruDevamSonrasiKart(challenge: DailyQuestionChallenge, poolAvailable: Boolean): DailyQuestionCardUiState =
    DailyQuestionRepository.toCardUiState(challenge.copy(pendingContinueSlotIndex = null), poolAvailable)

/**
 * `BlindingLessonFragment.handleDailyQuestionWrongAnswer` / `handleDailyQuestionLessonComplete`:
 * sorunun sonucu belgeye ve kırık kalp deposuna işlenir. Oturumun periyodu geçtiyse hiçbir şey
 * yazılmaz ve false döner. (Küresel başarı oranı işlemi — `recordQuestionResult` — burada yoktur;
 * hesabı [DailyQuestionRepository.successRateUpdates] içindedir.)
 */
fun gunlukSoruSonucunuIsle(
    depo: BellekIciGunlukSoruDeposu,
    kalpDeposu: com.sorobit.shared.oyun.DailyQuestionBrokenHeartStore,
    periodKey: String,
    slotIndex: Int,
    isSuccess: Boolean,
    nowMs: Long,
): Boolean {
    if (periodKey.isEmpty() || periodKey != DailyQuestionPeriod.currentPeriodKey(nowMs)) return false
    if (isSuccess) {
        depo.incrementSolvedCount(periodKey)
        kalpDeposu.clearBrokenHold116(periodKey)
    } else {
        depo.markPendingDiamondContinue(periodKey, slotIndex)
        kalpDeposu.requestPlayOnNextBind(periodKey)
    }
    return true
}

/** Yerel depolar bağlanana dek: bellekte duran kırık kalp deposu. */
fun bellekIciKalpDeposuKur(uid: String?): com.sorobit.shared.oyun.DailyQuestionBrokenHeartStore =
    com.sorobit.shared.oyun.DailyQuestionBrokenHeartStore(InMemoryKeyValueStore()) { uid }

/** Yerel depolar bağlanana dek: bellekte duran kart animasyon deposu. */
fun bellekIciKartAnimDeposuKur(uid: String?): com.sorobit.shared.oyun.DailyQuestionCardProgressAnimStore =
    com.sorobit.shared.oyun.DailyQuestionCardProgressAnimStore(InMemoryKeyValueStore()) { uid }
