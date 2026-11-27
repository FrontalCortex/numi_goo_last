// Kaynak (Android): app/src/main/java/com/example/app/
//   ProfileFragment.kt        — loadUserData (ad, kullanıcı satırı, süre, plan), updateTotalTimeDisplay,
//                               getBadgeLevel, getBadgeSortingWeight, bindRandomProfileBadges,
//                               getProfileBadgeRenderConfig, loadBadgeProgressFromFirestore (varsayılan alanlar),
//                               loadCupPathScores / loadCupPathScoresForUser (kart tablosu),
//                               followTargetUser (hedef kimliğin metinden ayıklanması),
//                               setupPreviewBeadSelector (sahip olunan boncuklar arasında gezinme)
//   EditProfileFragment.kt    — parola alanının görünürlüğü
//   AccountSettingsFragment.kt— "Ödemeler ve abonelikler" bölümünün görünürlüğü
//   AddFriendFragment.kt      — arama eşikleri, sayfa boyutu, önek aralığı, tekilleştirme
//   FollowersFollowingFragment.kt / FollowUserAdapter.kt / FriendSearchAdapter.kt — boş durum metni,
//                               "+" düğmesi kuralı, harf avatarı
//   FeedbackFragment.kt       — saatlik gönderim sınırı, `feedback` dokümanı
//
// Firestore/Auth çağrıları taşınmadı; yalnızca saf hesaplar ve sabitler. Android'de bu kuralların hepsi
// Fragment/Adapter içinde gömülüdür. Türler Swift'ten kolay kullanılsın diye üst düzeyde ve yalın alanlıdır.
//
// Android'de göze çarpan davranışlar (aynen korundu):
// - TURTLE rozeti ne açık rozet sıralamasında (secondGroupDefaultOrder) ne kilitli sıralamada
//   (lockedPriorityOrder) vardır; profilde hiç gösterilmez.
// - Rozet seviye tabloları ProfileFragment içinde ayrıca yazılmıştır (ROCKET burada [3,5,10,15,25]);
//   BadgeProgressEngine'deki tablolarla birebir aynı olmak zorunda değildir.
// - Başkasını profilinden takip ederken hedefin kullanıcı kimliği ekrandaki "@kimlik • 2024 YILINDA KATILDI"
//   metninden ayıklanır; kimliği olmayan kullanıcıda bu "2024 YILINDA KATILDI" metninin tamamı olur.
// - Arkadaş aramada 2 karakterde sonuçlar temizlenmez ama arama da başlamaz (arama eşiği 3).
// - Geri bildirim dokümanındaki "userId" alanı Firebase uid'sidir (kullanıcı kodu değil); alan adları
//   "androidVersion" / "deviceModel" platformdan bağımsız aynı kalır.
package com.sorobit.shared.profil

import com.sorobit.shared.abakus.AbacusPreferences
import com.sorobit.shared.kupa.rozet.BadgeKind
import com.sorobit.shared.kupa.rozet.BadgeLevelTone
import com.sorobit.shared.kupa.rozet.BadgeProgressEngine
import com.sorobit.shared.kupa.rozet.BadgeProgressFirestore
import com.sorobit.shared.kupa.rozet.BadgeState
import com.sorobit.shared.kupa.rozet.UserBadgeProgress

/** Profildeki "Kupa yolu" kartlarından biri (Android: setupCupCard parametreleri). */
data class ProfileCupCard(
    /** Kart başlığı. */
    val title: String,
    /** Lottie dosyası (assets). */
    val animFile: String,
    /** Zihinden (abaküssüz) kupalarda başlığın yanında `forbidden_abacus_ic` görünür. */
    val showForbiddenIcon: Boolean,
    /** `users/{uid}/cupWayProgress/progress` dokümanındaki alan. */
    val field: String,
)

/** Profil rozetinin çizim ayarı (Android: ProfileBadgeRenderConfig). */
data class ProfileBadgeRenderConfig(
    /** Alt katman Lottie dosyası; null ise alt katman gizlenir. */
    val baseAsset: String?,
    /** Üst katman Lottie dosyası. */
    val topAsset: String,
    /** Üst katmanın durdurulacağı kare (9999 = son kare). */
    val topHoldFrame: Float,
    /** Üst katmanın ölçeği. */
    val topScale: Float,
    /** Üst katmanın arka plan drawable adı; null ise yok. */
    val topBackgroundRes: String?,
)

/** Profildeki dört rozet yuvasından birinin çizime hazır hali. */
data class ProfileBadgeSlotUi(
    /** [BadgeKind] adı (ör. "DART"). */
    val kindName: String,
    val unlocked: Boolean,
    val showValue: Boolean,
    /** `(state.value ?: 0).toString()`. */
    val valueText: String,
    /** Alt katman Lottie dosyası; yoksa boş metin. */
    val baseAsset: String,
    val topAsset: String,
    val topHoldFrame: Float,
    val topScale: Float,
    /** Üst katmanın arkasında `bg_badge_circle_frame` var mı. */
    val hasCircleFrame: Boolean,
    /** Açık rozette alt katmana uygulanan ton: "BRONZE", "SILVER", "RED", "PURPLE", "ORIGINAL" ya da boş. */
    val toneName: String,
)

/** Geri bildirim gönderim sınırı denetiminin sonucu. */
data class FeedbackGate(
    /** Gönderime izin var mı. */
    val allowed: Boolean,
    /** Bir saatten eski kayıtlar atılmış liste; her denetimde geri yazılır. */
    val prunedTimestamps: String,
)

object ProfileFragmentLogic {

    // ── loadUserData ──────────────────────────────────────────────────────────

    const val COLLECTION_USERS = "users"
    const val COLLECTION_PUBLIC_PROFILES = "publicProfiles"

    const val FIELD_SELECTED_AVATAR = "selectedAvatar"
    const val FIELD_NAME = "name"
    const val FIELD_USER_ID = "userId"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_TOTAL_TIME_SPENT = "totalTimeSpent"
    const val FIELD_PLAN = "plan"
    const val FIELD_FOLLOWERS_COUNT = "followersCount"
    const val FIELD_FOLLOWING_COUNT = "followingCount"

    const val DEFAULT_DISPLAY_NAME = "Kullanıcı"
    const val DEFAULT_AVATAR_DRAWABLE = "non_user"
    const val AVATAR_COUNT = 12

    const val TEXT_COMPLETION_LOADING = "Hesaplanıyor..."
    const val TEXT_FOLLOW = "TAKİP ET"
    const val TEXT_FOLLOWING = "TAKİP EDİLİYOR"
    const val TOAST_FOLLOWED = "Takip edildi!"
    const val TOAST_SINGLE_BEAD = "Sadece 1 adet boncuğun var"

    /**
     * Başkasının profilinde `users` dokümanı okunamaz (yalnızca sahibi ve onaylı öğretmenler);
     * görüntülenecek alanlar `publicProfiles` aynasından gelir.
     */
    fun profileCollection(isOtherUser: Boolean): String =
        if (isOtherUser) COLLECTION_PUBLIC_PROFILES else COLLECTION_USERS

    fun isValidAvatar(selectedAvatar: Int): Boolean = selectedAvatar in 1..AVATAR_COUNT

    /** 1..12 → `avatar_icN`; diğer her değer → `non_user`. */
    fun avatarDrawableName(selectedAvatar: Int): String =
        if (isValidAvatar(selectedAvatar)) "avatar_ic$selectedAvatar" else DEFAULT_AVATAR_DRAWABLE

    /** Doküman adı → (yalnızca kendi profilinde) Auth görünen adı → "Kullanıcı". */
    fun displayName(docName: String?, authDisplayName: String?, isOtherUser: Boolean): String =
        if (!docName.isNullOrBlank()) {
            docName
        } else if (!isOtherUser && !authDisplayName.isNullOrBlank()) {
            authDisplayName
        } else {
            DEFAULT_DISPLAY_NAME
        }

    /** "@kimlik • 2024 YILINDA KATILDI"; kimlik boşsa yalnızca "2024 YILINDA KATILDI". */
    fun usernameAndDateText(userId: String, year: Int): String =
        if (userId.isNotEmpty()) "@$userId • $year YILINDA KATILDI" else "$year YILINDA KATILDI"

    fun completionText(percentage: Int): String = "%$percentage tamamlandı"

    /** Android: updateTotalTimeDisplay. */
    fun totalTimeText(totalSeconds: Long): String {
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            days > 0 -> "$days Gün $hours saat $minutes dakika"
            hours > 0 -> "$hours saat $minutes dakika"
            else -> "$minutes dakika"
        }
    }

    /** Plan alanı → gösterilen metin ("PRO", "Lite", diğer her şey "Free"). */
    fun subscriptionText(plan: String?): String =
        when ((plan ?: "Free").lowercase()) {
            "pro" -> "PRO"
            "lite" -> "Lite"
            else -> "Free"
        }

    /** Profil dokümanındaki en uzun günlük seri alanı (Android: `doc.getLong("longestStreak") ?: 0L`). */
    const val FIELD_LONGEST_STREAK = "longestStreak"

    fun longestStreak(rawFieldValue: Any?): Long = (rawFieldValue as? Number)?.toLong() ?: 0L

    /**
     * Takibi bırakma (Android: unfollowTargetUser): iki takip kaydı da tek batch'te silinir; sayaçları sunucu
     * tetikleyicileri düşürür. Dönüş: silinecek doküman yolları (önce hedefin takipçi kaydı, sonra kendi
     * takip kaydım).
     */
    fun unfollowDeletePaths(myUid: String, targetUid: String): List<String> = listOf(
        "users/$targetUid/followers/$myUid",
        "users/$myUid/following/$targetUid",
    )

    /**
     * Takip düğmesi (Android: applyFollowButtonState): takip edilirken düğme KAPALI değildir, ikinci dokunuş
     * onay sorup takibi bırakır; yarı saydamlık yoktur. Dönüş true → dokunuş [unfollowDeletePaths] akışını
     * (önce onay) başlatır, false → takip eder.
     */
    fun followButtonTapUnfollows(following: Boolean): Boolean = following

    /** Takip/bırakma sonrası ekrandaki takipçi sayısı anında düzeltilir (Android: nudgeFollowersCount); negatife inmez. */
    fun nudgedFollowersCount(current: Int, delta: Int): Int = (current + delta).coerceAtLeast(0)

    /** Android: followTargetUser — hedefin kullanıcı kimliği ekrandaki satırdan ayıklanır. */
    fun followTargetUserId(usernameAndDateText: String): String =
        usernameAndDateText.removePrefix("@").substringBefore(" •")

    // ── Kupa yolu kartları ────────────────────────────────────────────────────

    const val CUP_PROGRESS_SUBCOLLECTION = "cupWayProgress"
    const val CUP_PROGRESS_DOCUMENT = "progress"
    const val DEFAULT_CUP_SCORE = 200

    /** Android: loadCupPathScores — soldan sağa altı kart. */
    val CUP_CARDS: List<ProfileCupCard> = listOf(
        ProfileCupCard("Toplama", "dinosaur_anim.json", false, "addition_abacus_cup"),
        ProfileCupCard("Çıkarma", "crocodile_anim.json", false, "extraction_abacus_cup"),
        ProfileCupCard("Çarpma", "goat_anim.json", false, "impact_abacus_cup"),
        ProfileCupCard("Toplama", "eagle_anim.json", true, "blinding_addition_abacus_cup"),
        ProfileCupCard("Çıkarma", "fly_anim.json", true, "blinding_extraction_abacus_cup"),
        ProfileCupCard("Çarpma", "turtle_anim.json", true, "blinding_impact_abacus_cup"),
    )

    /** Android: `(doc?.get(fieldName) as? Number)?.toInt() ?: 200` (okuma hatasında da 200). */
    fun cupScore(rawFieldValue: Any?): Int = (rawFieldValue as? Number)?.toInt() ?: DEFAULT_CUP_SCORE

    // ── Rozetler ──────────────────────────────────────────────────────────────

    const val BADGE_PROGRESS_SUBCOLLECTION = "badgeProgress"
    const val BADGE_PROGRESS_DOCUMENT = "state"
    const val PROFILE_BADGE_SLOT_COUNT = 4

    /** Android: loadBadgeProgressFromFirestore — dokümanda yoksa (yalnızca kendi profilinde) yazılan alanlar. */
    val BADGE_PROGRESS_DEFAULTS: Map<String, Any> = mapOf(
        "userDartProgress" to 0L,
        "userKarateProgress" to 0L,
        "userGolfProgress" to 0L,
        "userBowlingProgress" to 0L,
        "userFishingProgress" to 0L,
        "userFishingStreak" to 0L,
        "userFishingStreakPeriodKey" to "",
        "userRocketProgress" to 0L,
        "userRocketDailyLessons" to 0L,
        "userRocketDailyDayId" to "",
    )

    /** Dokümanda bulunmayan varsayılan alanlar; boş değilse `set(..., merge)` ile yazılır. */
    fun missingBadgeDefaults(existingFields: Set<String>): Map<String, Any> =
        BADGE_PROGRESS_DEFAULTS.filterKeys { key -> !existingFields.contains(key) }

    private fun rawProgress(kind: BadgeKind, progress: UserBadgeProgress): Int? = when (kind) {
        BadgeKind.DART -> progress.userDartProgress
        BadgeKind.FISHING -> progress.userFishingProgress
        BadgeKind.GOLF -> progress.userGolfProgress
        BadgeKind.TORNADO -> progress.userTornadoProgress
        BadgeKind.VOLCANO -> progress.userVolcanoProgress
        BadgeKind.DINO -> progress.userDinoProgress
        BadgeKind.CROCODILE -> progress.userCrocodileProgress
        BadgeKind.GOAT -> progress.userGoatProgress
        BadgeKind.EAGLE -> progress.userEagleProgress
        BadgeKind.FLY -> progress.userFlyProgress
        BadgeKind.TURTLE -> progress.userTurtleProgress
        BadgeKind.ROCKET -> progress.userRocketProgress
        BadgeKind.BOWLING -> progress.userBowlingProgress
        else -> null
    }

    /**
     * (kademe eşikleri, 5. kademeden sonraki adım). Kademesiz rozetlerde null. Güncel Android'de eşikler
     * tek kaynaktan gelir ([BadgeProgressEngine.levelSpec]); profil eskiden kendi kopyasını tutuyordu ve
     * roket rozetinde ızgaradan farklı bir tablo kullanıyordu. Bu kopya da artık motoru kullanır.
     */
    fun levelSpec(kind: BadgeKind): Pair<List<Int>, Int>? = BadgeProgressEngine.levelSpec(kind)

    /** Android: getBadgeLevel — tam sayı kademe (5. kademeden sonra tam bölme). */
    fun badgeLevel(kind: BadgeKind, progress: UserBadgeProgress): Int {
        val raw = rawProgress(kind, progress) ?: 0
        val (levels, step) = levelSpec(kind) ?: return 0
        val rawCapped = raw.coerceAtLeast(0)
        val first = levels.first()
        if (rawCapped < first) return 0
        val last = levels.last()
        if (rawCapped <= last) {
            return levels.count { it <= rawCapped }
        }
        val extra = (rawCapped - last) / step
        return levels.size + extra
    }

    /** Android: getBadgeSortingWeight — 5. kademeden sonra kesirli devam eder. */
    fun badgeSortingWeight(kind: BadgeKind, progress: UserBadgeProgress): Double {
        val raw = rawProgress(kind, progress) ?: 0
        val (levels, step) = levelSpec(kind) ?: return 0.0
        val rawCapped = raw.coerceAtLeast(0)
        val first = levels.first()
        if (rawCapped < first) return 0.0
        val last = levels.last()
        if (rawCapped <= last) {
            return levels.count { it <= rawCapped }.toDouble()
        }
        // 5. seviyeden sonra: (Mevcut İlerleme - Son Eleman) / levelUp
        val extra = (rawCapped - last).toDouble() / step.toDouble()
        return levels.size.toDouble() + extra
    }

    /** Açık rozetlerin ilk grubu (sabit öncelikli). */
    val FIRST_GROUP_ORDER: List<BadgeKind> = listOf(
        BadgeKind.GOLD,
        BadgeKind.SILVER,
        BadgeKind.BRONZE,
        BadgeKind.CUP,
        BadgeKind.KARATE,
    )

    /** Açık rozetlerin ikinci grubu; kademeye göre azalan, eşitlikte bu sırayla. */
    val SECOND_GROUP_DEFAULT_ORDER: List<BadgeKind> = listOf(
        BadgeKind.DINO,
        BadgeKind.CROCODILE,
        BadgeKind.GOAT,
        BadgeKind.EAGLE,
        BadgeKind.FLY,
        BadgeKind.TORNADO,
        BadgeKind.VOLCANO,
        BadgeKind.BOWLING,
        BadgeKind.DART,
        BadgeKind.FISHING,
        BadgeKind.GOLF,
        BadgeKind.ROCKET,
    )

    /** Kilitli rozetler, açık olanlar yuvaları doldurmadıysa bu sırayla eklenir. */
    val LOCKED_PRIORITY_ORDER: List<BadgeKind> = listOf(
        BadgeKind.ROCKET,
        BadgeKind.GOLF,
        BadgeKind.FISHING,
        BadgeKind.DART,
        BadgeKind.BOWLING,
        BadgeKind.VOLCANO,
        BadgeKind.TORNADO,
        BadgeKind.DINO,
        BadgeKind.CROCODILE,
        BadgeKind.GOAT,
        BadgeKind.EAGLE,
        BadgeKind.FLY,
        BadgeKind.KARATE,
        BadgeKind.BRONZE,
        BadgeKind.SILVER,
        BadgeKind.GOLD,
        BadgeKind.CUP,
    )

    /** Android: bindRandomProfileBadges — gösterilecek (en çok [slotCount]) rozet, soldan sağa. */
    fun pickProfileBadges(
        states: List<BadgeState>,
        progress: UserBadgeProgress,
        slotCount: Int = PROFILE_BADGE_SLOT_COUNT,
    ): List<BadgeState> {
        val badgesByType = states.associateBy { it.kind }

        val firstGroupPart = FIRST_GROUP_ORDER.mapNotNull { type ->
            badgesByType[type]?.takeIf { it.unlocked }
        }
        val secondGroupPart = SECOND_GROUP_DEFAULT_ORDER.mapNotNull { type ->
            badgesByType[type]?.takeIf { it.unlocked }
        }.sortedWith(
            compareByDescending<BadgeState> { badgeSortingWeight(it.kind, progress) }
                .thenBy { SECOND_GROUP_DEFAULT_ORDER.indexOf(it.kind) },
        )
        val orderedUnlocked = firstGroupPart + secondGroupPart

        val orderedLocked = LOCKED_PRIORITY_ORDER.mapNotNull { type ->
            badgesByType[type]?.takeIf { !it.unlocked }
        }
        return (orderedUnlocked + orderedLocked).take(slotCount)
    }

    private const val BADGE_BASE_ASSET = "daily_tasks_complite_badge2.json"
    private const val BADGE_CIRCLE_FRAME = "bg_badge_circle_frame"

    private fun plain(topAsset: String) = ProfileBadgeRenderConfig(null, topAsset, 0f, 1f, null)

    private fun layered(topAsset: String, hold: Float, scale: Float, framed: Boolean) =
        ProfileBadgeRenderConfig(BADGE_BASE_ASSET, topAsset, hold, scale, if (framed) BADGE_CIRCLE_FRAME else null)

    /** Android: getProfileBadgeRenderConfig (dosya adlarındaki çift ".json" Android'deki gibidir). */
    fun profileBadgeRenderConfig(type: BadgeKind): ProfileBadgeRenderConfig = when (type) {
        BadgeKind.CUP -> plain("cup_google_anim.json")
        BadgeKind.KARATE -> plain("karate_anim.json")
        BadgeKind.GOLD -> plain("gold_medal_anim.json")
        BadgeKind.SILVER -> plain("silver_medal_anim.json.json")
        BadgeKind.BRONZE -> plain("bronze_medal_anim.json.json")
        BadgeKind.DART -> layered("dart_anim.json", 60f, 0.65f, false)
        BadgeKind.ROCKET -> layered("rocket_badge_anim2.json", 60f, 1f, false)
        BadgeKind.BOWLING -> layered("bowling_anim.json", 47f, 0.68f, true)
        BadgeKind.FISHING -> layered("fishing_pole_anim.json", 24f, 0.65f, false)
        BadgeKind.GOLF -> layered("golf_anim.json", 30f, 0.60f, false)
        BadgeKind.TORNADO -> layered("tornado_anim.json", 9999f, 0.65f, true)
        BadgeKind.VOLCANO -> layered("volcano_anim.json", 9999f, 0.65f, true)
        BadgeKind.DINO -> layered("dinosaur_anim.json", 9999f, 0.5f, true)
        BadgeKind.CROCODILE -> layered("crocodile_anim.json", 9999f, 0.50f, true)
        BadgeKind.GOAT -> layered("goat_anim.json", 9999f, 0.50f, true)
        BadgeKind.EAGLE -> layered("eagle_anim.json", 9999f, 0.50f, true)
        BadgeKind.FLY -> layered("fly_anim.json", 9999f, 0.50f, true)
        BadgeKind.TURTLE -> layered("turtle_anim.json", 9999f, 0.50f, true)
    }

    /** Android: frameToProgress. */
    fun frameToProgress(frame: Float, startFrame: Float, endFrame: Float): Float {
        if (endFrame <= startFrame) return 0f
        return ((frame - startFrame) / (endFrame - startFrame)).coerceIn(0f, 1f)
    }

    /** Seçim + çizim ayarı + metin: profil rozet satırının çizime hazır hali. */
    fun profileBadgeSlots(progress: UserBadgeProgress): List<ProfileBadgeSlotUi> {
        val states = BadgeProgressEngine.calculate(progress)
        return pickProfileBadges(states, progress).map { state ->
            val config = profileBadgeRenderConfig(state.kind)
            val tone: BadgeLevelTone? = state.levelTone
            ProfileBadgeSlotUi(
                kindName = state.kind.name,
                unlocked = state.unlocked,
                showValue = state.showValue,
                valueText = (state.value ?: 0).toString(),
                baseAsset = config.baseAsset ?: "",
                topAsset = config.topAsset,
                topHoldFrame = config.topHoldFrame,
                topScale = config.topScale,
                hasCircleFrame = config.topBackgroundRes != null,
                toneName = tone?.name ?: "",
            )
        }
    }

    /**
     * `users/{uid}/badgeProgress/state` dokümanından rozet satırı. Doküman yoksa ya da okunamadıysa
     * (null) boş ilerlemeyle hesaplanır (Android: `BadgeProgressRepository.update(UserBadgeProgress())`).
     */
    fun profileBadgeSlotsFromStateDoc(doc: Map<String, Any?>?): List<ProfileBadgeSlotUi> =
        profileBadgeSlots(BadgeProgressFirestore.userBadgeProgressFromStateSnapshot(doc))

    // ── Abaküs paneli: profil kartındaki boncuğu seçme ───────────────────────

    const val PREVIEW_BEAD_SYNC_DELAY_MS = 500L
    const val ALWAYS_OWNED_BEAD = "SOROBAN"

    /** Bütün boncuk tiplerinin adları, `AbacusPreferences.BeadType` sırasıyla. */
    val ALL_BEAD_TYPE_NAMES: List<String> = AbacusPreferences.BeadType.entries.map { it.name }

    /**
     * Android: setupPreviewBeadSelector — sahip olunan boncuk tipleri ([allTypeNames] sırasıyla):
     * SOROBAN her zaman, diğerleri `haveBead/{tip}.count > 0` ise. [initialType] listede yoksa başa eklenir.
     */
    fun ownedPreviewBeadTypes(
        allTypeNames: List<String>,
        ownedCounts: Map<String, Int>,
        initialType: String,
    ): List<String> {
        val ownedTypes = allTypeNames.filter { type ->
            type == ALWAYS_OWNED_BEAD || (ownedCounts[type] ?: 0) > 0
        }.toMutableList()
        if (!ownedTypes.contains(initialType)) ownedTypes.add(0, initialType)
        return ownedTypes
    }

    /**
     * Ok düğmesiyle sonraki/önceki boncuğun sırası (döngüsel). Tek boncuk varsa -1 döner:
     * seçim değişmez ve [TOAST_SINGLE_BEAD] gösterilir.
     */
    fun previewBeadStep(index: Int, size: Int, forward: Boolean): Int {
        if (size <= 1) return -1
        return if (forward) (index + 1) % size else (index - 1 + size) % size
    }
}

/** Profil düzenleme (EditProfileFragment). Ad/parola doğrulaması `model/AuthRules.kt` içindedir. */
object EditProfileRules {
    const val PROVIDER_PASSWORD = "password"
    const val PROVIDER_GOOGLE = "google.com"
    const val ROLE_STUDENT = "STUDENT"

    const val TEXT_SAVE = "DEĞİŞİKLİKLERİ KAYDET"
    const val TEXT_SAVING = "KAYDEDİLİYOR..."
    const val TOAST_SAVED = "Değişiklikler kaydedildi."
    const val TOAST_FIRESTORE_FAILED = "Firestore güncellenemedi."
    const val TOAST_AUTH_FAILED = "Auth profil güncellenemedi."
    const val TOAST_PASSWORD_UPDATED = "Parola başarıyla güncellendi."
    const val TOAST_PASSWORD_RECENT_LOGIN =
        "Güvenlik nedeniyle parolayı değiştirmek için lütfen tekrar giriş yapın."
    const val TOAST_DELETE_RECENT_LOGIN =
        "Güvenlik nedeniyle hesabı silmek için lütfen tekrar giriş yapıp hemen silmeyi deneyin."
    const val TOAST_DELETED = "Hesabınız başarıyla silindi."
    const val DELETE_DIALOG_TITLE = "Hesabı Sil"
    const val DELETE_DIALOG_MESSAGE =
        "Hesabınızı kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz."
    const val DELETE_PROGRESS_MESSAGE = "Hesabınız ve ilişkili verileriniz siliniyor..."

    /** Google ile girilmiş ve parola sağlayıcısı eklenmemişse parola değiştirilemez. */
    fun providerAllowsPasswordChange(providerIds: List<String>): Boolean {
        val hasPassword = providerIds.contains(PROVIDER_PASSWORD)
        val hasGoogle = providerIds.contains(PROVIDER_GOOGLE)
        return hasPassword || !hasGoogle
    }

    /**
     * "Yeni Parola" alanı görünür mü. Kullanıcı dokümanı okunduysa ([userDocLoaded]) rol de hesaba
     * katılır: öğrencide gizlidir. Okunamadıysa yalnızca sağlayıcıya bakılır.
     */
    fun showPasswordField(providerIds: List<String>, role: String?, userDocLoaded: Boolean): Boolean {
        val allowed = providerAllowsPasswordChange(providerIds)
        return if (userDocLoaded) allowed && (role ?: "") != ROLE_STUDENT else allowed
    }

    /** Parola yalnızca alan doluysa VE görünürse güncellenir. */
    fun shouldUpdatePassword(newPassword: String, passwordFieldVisible: Boolean): Boolean =
        newPassword.isNotEmpty() && passwordFieldVisible

    fun usernameFieldText(userId: String): String = "@$userId"
}

/** Hesap ayarları (AccountSettingsFragment). */
object AccountSettingsRules {
    /** Android'de abonelik iptali Play Store sayfasını açar. */
    const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"

    /** "Ödemeler ve abonelikler" bölümü yalnızca plan "Free" değilse görünür (alan yoksa "Free"). */
    fun showPayments(plan: String?): Boolean = !(plan ?: "Free").equals("Free", ignoreCase = true)
}

/** Arkadaş arama (AddFriendFragment) ve takip listesi (FollowersFollowingFragment) kuralları. */
object FriendSearchRules {
    const val SEARCH_DELAY_MS = 300L

    /** Bundan kısa sorguda sonuçlar temizlenir ve arama planlanmaz. */
    const val MIN_QUERY_LENGTH_TO_KEEP = 2

    /** Bundan kısa sorguda arama yapılmaz. */
    const val MIN_QUERY_LENGTH_TO_SEARCH = 3

    const val FIRST_PAGE_SIZE = 20L
    const val LOAD_MORE_PAGE_SIZE = 10L

    const val TEXT_LOAD_MORE = "DAHA FAZLA YÜKLE"
    const val TEXT_LOADING_MORE = "YÜKLENİYOR..."
    const val TEXT_NOT_FOUND = "Kullanıcı bulunamadı"
    const val TOAST_NO_SESSION = "Oturum bilgisi alınamadı."
    const val TOAST_ALREADY_FOLLOWING = "Zaten takip ediyorsunuz."

    const val TEXT_EMPTY_FOLLOWING = "Henüz kimseyi takip etmiyorsun"
    const val TEXT_EMPTY_FOLLOWERS = "Henüz takipçin yok"

    /** Yazarken: sorgu (trim edilmiş) çok kısaysa sonuçlar temizlenir. */
    fun shouldClearResults(query: String): Boolean = query.length < MIN_QUERY_LENGTH_TO_KEEP

    fun shouldSearch(query: String): Boolean = query.length >= MIN_QUERY_LENGTH_TO_SEARCH

    fun pageSize(isLoadMore: Boolean): Long = if (isLoadMore) LOAD_MORE_PAGE_SIZE else FIRST_PAGE_SIZE

    /** Önek aramasının üst sınırı: `name >= query && name <= query + ""`. */
    fun queryEnd(query: String): String = query + ""

    /** İki sorgunun (ad, kullanıcı kimliği) birleşik sonucunda uid'ye göre tekilleştirme; ilk görülen kalır. */
    fun dedupeUids(uids: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return uids.filter { seen.add(it) }
    }

    /** Bu turda iki sorgudan da hiç sonuç gelmediyse "daha fazla" düğmesi gizlenir. */
    fun hasMoreResults(batchResultCount: Int): Boolean = batchResultCount > 0

    fun resultCountText(count: Int): String = "$count sonuç gösteriliyor"

    fun followedToast(name: String): String = "$name takip edildi."

    /** FollowersFollowingFragment'teki bildirimde nokta yoktur. */
    fun followedBackToast(name: String): String = "$name takip edildi"

    fun emptyListText(tab: Int): String = if (tab == 0) TEXT_EMPTY_FOLLOWING else TEXT_EMPTY_FOLLOWERS

    /** "+" (geri takip et) düğmesi yalnızca Takipçi sekmesinde ve henüz takip etmediklerimizde görünür. */
    fun showFollowBackButton(tab: Int, alreadyFollowing: Boolean): Boolean = tab == 1 && !alreadyFollowing

    /** Takip listesindeki harf avatarı renkleri (FollowUserAdapter). */
    val FOLLOW_AVATAR_COLORS: List<String> = listOf(
        "#1CB0F6", "#CE82FF", "#FF9600", "#FF4B4B",
        "#00CD9C", "#2B70C9", "#FF6B97", "#8DC63F",
    )

    /** Arama sonuçlarındaki harf avatarı renkleri (FriendSearchAdapter; sıra farklıdır). */
    val SEARCH_AVATAR_COLORS: List<String> = listOf(
        "#1CB0F6", "#FF4B4B", "#CE82FF", "#FF9600",
        "#00CD9C", "#2B70C9", "#FF6B97", "#8DC63F",
    )

    fun followAvatarColor(position: Int): String = FOLLOW_AVATAR_COLORS[position.mod(FOLLOW_AVATAR_COLORS.size)]

    fun searchAvatarColor(position: Int): String = SEARCH_AVATAR_COLORS[position.mod(SEARCH_AVATAR_COLORS.size)]

    /** Avatar seçilmemişse gösterilen harf: adın ilk karakteri büyük harfle; ad boşsa "?". */
    fun avatarLetter(name: String): String = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}

/** Geri bildirim (FeedbackFragment). */
object FeedbackRules {
    const val COLLECTION_FEEDBACK = "feedback"
    const val FIELD_USER_ID = "userId"
    const val FIELD_EMAIL = "email"
    const val FIELD_MESSAGE = "message"

    /** Sunucu zaman damgası (`FieldValue.serverTimestamp()`); [feedbackDoc] bu alanı içermez. */
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_APP_VERSION = "appVersion"
    const val FIELD_OS_VERSION = "androidVersion"
    const val FIELD_DEVICE_MODEL = "deviceModel"

    const val ANONYMOUS_USER = "anonymous"
    const val NO_EMAIL = "No Email"
    const val UNKNOWN_VERSION = "Unknown"

    const val PREFS_NAME = "feedback_prefs"
    const val PREF_TIMESTAMPS = "timestamps"
    const val WINDOW_MS = 60 * 60 * 1000L
    const val MAX_PER_WINDOW = 5

    const val TOAST_RATE_LIMITED =
        "Son 1 saat içinde çok fazla bildirim gönderdiniz. Lütfen daha sonra tekrar deneyin."
    const val TOAST_SENT = "Görüşleriniz başarıyla iletildi. Teşekkürler!"

    fun sendFailedToast(message: String?): String = "Gönderilemedi: $message"

    private fun parse(savedTimestamps: String): MutableList<Long> =
        if (savedTimestamps.isEmpty()) {
            mutableListOf()
        } else {
            savedTimestamps.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        }

    /**
     * Android: canSendFeedback — bir saatten eski kayıtlar atılır, kalan liste geri yazılır;
     * son bir saatte 5 ya da daha fazla gönderim varsa izin verilmez.
     */
    fun canSendFeedback(savedTimestamps: String, nowMs: Long): FeedbackGate {
        val timestamps = parse(savedTimestamps)
        timestamps.removeAll { nowMs - it > WINDOW_MS }
        return FeedbackGate(
            allowed = timestamps.size < MAX_PER_WINDOW,
            prunedTimestamps = timestamps.joinToString(","),
        )
    }

    /** Android: recordFeedbackTimestamp — başarılı gönderimden sonra saklanacak yeni liste. */
    fun recordFeedbackTimestamp(savedTimestamps: String, nowMs: Long): String {
        val timestamps = parse(savedTimestamps)
        timestamps.add(nowMs)
        return timestamps.joinToString(",")
    }

    /** Gönder düğmesi yalnızca metin boş değilken etkindir. */
    fun canSubmit(text: String): Boolean = text.isNotBlank()

    /** `feedback` koleksiyonuna eklenen doküman ([FIELD_CREATED_AT] hariç; onu yazan taraf ekler). */
    fun feedbackDoc(
        uid: String?,
        email: String?,
        message: String,
        appVersion: String?,
        osVersion: String,
        deviceModel: String,
    ): Map<String, Any> = mapOf(
        FIELD_USER_ID to (uid ?: ANONYMOUS_USER),
        FIELD_EMAIL to (email ?: NO_EMAIL),
        FIELD_MESSAGE to message,
        FIELD_APP_VERSION to (appVersion ?: UNKNOWN_VERSION),
        FIELD_OS_VERSION to osVersion,
        FIELD_DEVICE_MODEL to deviceModel,
    )
}
