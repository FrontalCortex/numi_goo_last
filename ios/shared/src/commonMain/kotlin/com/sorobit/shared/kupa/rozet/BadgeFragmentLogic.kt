// Kaynak (Android): app/src/main/java/com/example/app/BadgeFragment.kt — görünümden bağımsız kısım:
//  - companion sabitleri (tutma kareleri), unlockedDetailTextOverrides / lockedDetailTextOverrides metinleri
//  - newLevelUpSequenceInstance: kutlama kuyruğu öğesinin ("MODE|from|to|target") ayrıştırılması
//  - defaultRewardAmountForMode, formatUnlockedDetailText, configureMedalPieceNamesList / togglePieceRowsPanel'in
//    parça (madalya/kupa satırı) filtreleme-sayma kuralları, updateLockedProgressUi / updateCelebrationProgressUi
//    yüzde hesabı, startLevelUpCelebration'ın değer/ton/ilerleme hesabı, hasNextCelebrationInQueue,
//    levelUpAnimConfig, resolveShownValueFallback, shouldShowProgressForMode, frameToProgress
// Lottie/animasyon/görünüm kodu taşınmadı. Lottie dosya adları Android assets klasöründeki adlardır.
package com.sorobit.shared.kupa.rozet

object BadgeFragmentLogic {

    const val TOP_HOLD_FRAME_DART = 60f
    const val TOP_HOLD_FRAME_ROCKET = 60f
    const val TOP_HOLD_FRAME_BOWLING = 47f
    const val TOP_HOLD_FRAME_GOLF = 9999f
    const val TOP_HOLD_FRAME_FISHING = 24f
    const val TOP_HOLD_FRAME_TORNADO = 9999f
    const val TOP_HOLD_FRAME_VOLCANO = 9999f
    const val CUP_GOOGLE_HOLD_FRAME = 135f
    const val KARATE_HOLD_FRAME = 1f
    const val KARATE_REPLAY_START_FRAME = 40f


    /**
     * unlocked=true olduğunda gösterilecek metinler.
     */
    val unlockedDetailTextOverrides = mapOf(
        BadgeAnimMode.DAILY_ONLY to "%1\$s dersi hatasız tamamlayarak Kusursuz Odak rozetini kazandın.",
        BadgeAnimMode.ROCKET_WITH_BASE to "%1\$s dersi 1 günde tamamlayarak Sınır Tanımaz rozetini kazandın.",
        BadgeAnimMode.BOWLING_WITH_BASE to "%1\$s günlük görev tamamlayarak Strike Ustası rozetini kazandın.",
        BadgeAnimMode.GOLF_WITH_BASE to "%1\$s kere eğitmene danışarak Nokta Atışı rozetini kazandın.",
        BadgeAnimMode.FISHING_WITH_BASE to "%1\$s gün üst üste Günlük Soru mücadelesini tamamlayarak Derin Avcı rozetini kazandın.",
        BadgeAnimMode.DINO to "Toplama Kupa Yolunda %1\$s kupa kazanarak Dino rozetini kazandın.",
        BadgeAnimMode.TORNADO to "%1\$s Toplama Ustalık Yolu adımını tamamlayarak Kasırga rozetini kazandın.",
        BadgeAnimMode.VOLCANO to "%1\$s Çıkarma Ustalık Yolu adımını tamamlayarak Kasırga rozetini kazandın.",
        BadgeAnimMode.CROCODILE to "Çıkarma Kupa Yolunda %1\$s kupa kazanarak Timo rozetini kazandın.",
        BadgeAnimMode.GOAT to "Çarpma Kupa Yolunda %1\$s kupa kazanarak Keço rozetini kazandın.",
        BadgeAnimMode.EAGLE to "Toplama Kupa Yolu - Körleme'de %1\$s kupa kazanarak Karti rozetini kazandın.",
        BadgeAnimMode.FLY to "Çıkarma Kupa Yolu - Körleme'de %1\$s kupa kazanarak Sino rozetini kazandın.",
        BadgeAnimMode.TURTLE to "Çarpma Kupa Yolu - Körleme'de %1\$s kupa kazanarak Kapi rozetini kazandın.",
        BadgeAnimMode.CUP_GOOGLE to "Ünite Maratonunda %1\$s. olarak Elit Seviye kupasını kazandın.",
        BadgeAnimMode.KARATE to "%1\$s Ünite Maratonunu 3 yıldız ile tamamlayarak Siyah Kuşak rozetini kazandın.",
        BadgeAnimMode.GOLD_MEDAL to "Ünite maratonunda 1. olarak Altın Madalya kazandın.",
        BadgeAnimMode.SILVER_MEDAL to "Ünite maratonunda 2. olarak Gümüş Madalya kazandın.",
        BadgeAnimMode.BRONZE_MEDAL to "Ünite maratonunda 3. olarak Bronz Madalya kazandın.",
    )

    /**
     * unlocked=false olduğunda gösterilecek metinler.
     */
    val lockedDetailTextOverrides = mapOf(
        BadgeAnimMode.DAILY_ONLY to "Bu rozeti kazanmak için 3 dersi hatasız tamamlayarak başarı elde et.",
        BadgeAnimMode.ROCKET_WITH_BASE to "Bu rozeti kazanmak için 3 dersi 1 günde tamamlayarak başarı elde et.",
        BadgeAnimMode.BOWLING_WITH_BASE to "Bu rozeti kazanmak için 5 günlük görevi tamamlayarak başarı elde et.",
        BadgeAnimMode.GOLF_WITH_BASE to "Bu rozeti kazanmak için 3 kere öğretmene danış.",
        BadgeAnimMode.FISHING_WITH_BASE to "Bu rozeti kazanmak için 3 gün üst üste Günlük Soru mücadelesini çözerek başarı elde et.",
        BadgeAnimMode.DINO to "Bu rozeti kazanmak için Toplama Kupa Yolunda 500 kupaya ulaş.",
        BadgeAnimMode.TORNADO to "Bu rozeti kazanmak için Toplama Ustalık Yolunda 1 adımı tamamla.",
        BadgeAnimMode.VOLCANO to "Bu rozeti kazanmak için Çıkarma Ustalık Yolunda 1 adımı tamamla.",
        BadgeAnimMode.CROCODILE to "Bu rozeti kazanmak için Çıkarma Kupa Yolunda 500 kupa kazan.",
        BadgeAnimMode.GOAT to "Bu rozeti kazanmak için Çarpma Kupa Yolunda 500 kupa kazan.",
        BadgeAnimMode.EAGLE to "Bu rozeti kazanmak için Toplama Kupa Yolu - Körleme'de 500 kupa kazan.",
        BadgeAnimMode.FLY to "Bu rozeti kazanmak için Çıkarma Kupa Yolu - Körleme'de 500 kupa kazan.",
        BadgeAnimMode.TURTLE to "Bu rozeti kazanmak için Çarpma Kupa Yolu - Körleme'de 500 kupa kazan.",
        BadgeAnimMode.CUP_GOOGLE to "Bu kupayı kazanmak için ünite maratonunda ilk 100'e girerek başarı elde et.",
        BadgeAnimMode.KARATE to "Bu rozeti kazanmak için ünite maratonunu 3 yıldız ile tamamlayarak başarı elde et.",
        BadgeAnimMode.GOLD_MEDAL to "Bu madalyayı kazanmak için bir ünite maratonu liderliğinde birinci ol.",
        BadgeAnimMode.SILVER_MEDAL to "Bu madalyayı kazanmak için bir ünite maratonu liderliğinde ikinci ol.",
        BadgeAnimMode.BRONZE_MEDAL to "Bu madalyayı kazanmak için bir ünite maratonu liderliğinde üçüncü ol.",
    )


    /**
     * Android'de [unlockedDetailTextOverrides] metinlerinin okunduğu string kaynak adları
     * (`appString(R.string.…)`). iOS, Türkçe dışındaki dillerde metni bu adla seçili dilden okur.
     */
    val unlockedDetailTextKeys = mapOf(
        BadgeAnimMode.DAILY_ONLY to "ui_x_dersi_hatasiz_tamamlayarak_kusursuz_odak",
        BadgeAnimMode.ROCKET_WITH_BASE to "ui_x_dersi_1_gunde_tamamlayarak_sinir",
        BadgeAnimMode.BOWLING_WITH_BASE to "ui_x_gunluk_gorev_tamamlayarak_strike_ustasi",
        BadgeAnimMode.GOLF_WITH_BASE to "ui_x_kere_egitmene_danisarak_nokta_atisi",
        BadgeAnimMode.FISHING_WITH_BASE to "ui_x_gun_ust_uste_gunluk_soru",
        BadgeAnimMode.DINO to "ui_toplama_kupa_yolunda_x_kupa_kazanarak",
        BadgeAnimMode.TORNADO to "ui_x_toplama_ustalik_yolu_adimini_tamamlayarak",
        BadgeAnimMode.VOLCANO to "ui_x_cikarma_ustalik_yolu_adimini_tamamlayarak",
        BadgeAnimMode.CROCODILE to "ui_cikarma_kupa_yolunda_x_kupa_kazanarak",
        BadgeAnimMode.GOAT to "ui_carpma_kupa_yolunda_x_kupa_kazanarak",
        BadgeAnimMode.EAGLE to "ui_toplama_kupa_yolu_korleme_de_x",
        BadgeAnimMode.FLY to "ui_cikarma_kupa_yolu_korleme_de_x",
        BadgeAnimMode.TURTLE to "ui_carpma_kupa_yolu_korleme_de_x",
        BadgeAnimMode.CUP_GOOGLE to "ui_unite_maratonunda_x_olarak_elit_seviye",
        BadgeAnimMode.KARATE to "ui_x_unite_maratonunu_3_yildiz_ile",
        BadgeAnimMode.GOLD_MEDAL to "ui_unite_maratonunda_1_olarak_altin_madalya",
        BadgeAnimMode.SILVER_MEDAL to "ui_unite_maratonunda_2_olarak_gumus_madalya",
        BadgeAnimMode.BRONZE_MEDAL to "ui_unite_maratonunda_3_olarak_bronz_madalya",
    )

    /** [lockedDetailTextOverrides] metinlerinin Android string kaynak adları. */
    val lockedDetailTextKeys = mapOf(
        BadgeAnimMode.DAILY_ONLY to "ui_bu_rozeti_kazanmak_icin_3_dersi",
        BadgeAnimMode.ROCKET_WITH_BASE to "ui_bu_rozeti_kazanmak_icin_3_dersi_2",
        BadgeAnimMode.BOWLING_WITH_BASE to "ui_bu_rozeti_kazanmak_icin_5_gunluk",
        BadgeAnimMode.GOLF_WITH_BASE to "ui_bu_rozeti_kazanmak_icin_3_kere",
        BadgeAnimMode.FISHING_WITH_BASE to "ui_bu_rozeti_kazanmak_icin_3_gun",
        BadgeAnimMode.DINO to "ui_bu_rozeti_kazanmak_icin_toplama_kupa",
        BadgeAnimMode.TORNADO to "ui_bu_rozeti_kazanmak_icin_toplama_ustalik",
        BadgeAnimMode.VOLCANO to "ui_bu_rozeti_kazanmak_icin_cikarma_ustalik",
        BadgeAnimMode.CROCODILE to "ui_bu_rozeti_kazanmak_icin_cikarma_kupa",
        BadgeAnimMode.GOAT to "ui_bu_rozeti_kazanmak_icin_carpma_kupa",
        BadgeAnimMode.EAGLE to "ui_bu_rozeti_kazanmak_icin_toplama_kupa_2",
        BadgeAnimMode.FLY to "ui_bu_rozeti_kazanmak_icin_cikarma_kupa_2",
        BadgeAnimMode.TURTLE to "ui_bu_rozeti_kazanmak_icin_carpma_kupa_2",
        BadgeAnimMode.CUP_GOOGLE to "ui_bu_kupayi_kazanmak_icin_unite_maratonunda",
        BadgeAnimMode.KARATE to "ui_bu_rozeti_kazanmak_icin_unite_maratonunu",
        BadgeAnimMode.GOLD_MEDAL to "ui_bu_madalyayi_kazanmak_icin_bir_unite",
        BadgeAnimMode.SILVER_MEDAL to "ui_bu_madalyayi_kazanmak_icin_bir_unite_2",
        BadgeAnimMode.BRONZE_MEDAL to "ui_bu_madalyayi_kazanmak_icin_bir_unite_3",
    )

    /** [badgeDetailText] metninin Android string kaynak adı; bilinmiyorsa boş. */
    fun badgeDetailKey(mode: BadgeAnimMode, unlocked: Boolean = true): String =
        (if (unlocked) unlockedDetailTextKeys else lockedDetailTextKeys)[mode].orEmpty()

    /**
     * [badgeDetailText]'in şablondaki "%1\$s" yerine koyduğu ilerleme değeri ([formatUnlockedDetailText] ile aynı
     * kural); şablon değişmeden kalacaksa null (kilitli rozet, sabit metinli mod ya da ilerleme yok).
     */
    fun badgeDetailProgress(mode: BadgeAnimMode, unlocked: Boolean = true, celebrateTarget: Int? = null): Int? {
        if (!unlocked || mode !in modesWithDynamicProgress) return null
        return celebrateTarget ?: BadgeProgressRepository.getProgressValueByMode(mode)
    }

    /** Android: `unlockedDetailTextOverrides[mode].orEmpty()` şablonu ("x" yer tutucusu ilerleme ile değişir). */
    fun unlockedDetailTemplate(mode: BadgeAnimMode): String = unlockedDetailTextOverrides[mode].orEmpty()

    /** Android: `lockedDetailTextOverrides[mode].orEmpty()`. */
    fun lockedDetailText(mode: BadgeAnimMode): String = lockedDetailTextOverrides[mode].orEmpty()

    /** Kutlama kuyruğu öğesi ([BadgeProgressFirestore.payloadToQueueItem] çıktısının ayrıştırılmış hâli). */
    data class CelebrationQueueItem(
        val mode: BadgeAnimMode,
        val from: Int,
        val to: Int,
        val target: Int,
    )

    /**
     * Android `newLevelUpSequenceInstance` ayrıştırması: indeks liste dışındaysa "DAILY_ONLY|0|0|0";
     * bilinmeyen mod → DAILY_ONLY; sayı olmayan/eksik parça → 0.
     */
    fun parseQueueItem(payloads: List<String>, index: Int = 0): CelebrationQueueItem {
        val current = payloads.getOrNull(index) ?: "DAILY_ONLY|0|0|0"
        val parts = current.split("|")
        val mode = parts.getOrNull(0)?.let { name -> BadgeAnimMode.entries.firstOrNull { it.name == name } }
            ?: BadgeAnimMode.DAILY_ONLY
        val from = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val to = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val target = parts.getOrNull(3)?.toIntOrNull() ?: 0
        return CelebrationQueueItem(mode, from, to, target)
    }

    /** Android `hasNextCelebrationInQueue`. */
    fun hasNextCelebrationInQueue(queueSize: Int?, index: Int): Boolean {
        if (queueSize == null) return false
        return (index + 1) < queueSize
    }

    /** Android `defaultRewardAmountForMode`. */
    fun defaultRewardAmountForMode(mode: BadgeAnimMode): Int? {
        return when (mode) {
            BadgeAnimMode.DAILY_ONLY -> 50
            BadgeAnimMode.ROCKET_WITH_BASE -> 100
            BadgeAnimMode.BOWLING_WITH_BASE -> 150
            BadgeAnimMode.FISHING_WITH_BASE -> 3
            BadgeAnimMode.GOLF_WITH_BASE -> 225
            BadgeAnimMode.CUP_GOOGLE -> 87
            BadgeAnimMode.KARATE -> 1
            BadgeAnimMode.TORNADO -> 1
            BadgeAnimMode.VOLCANO -> 1
            BadgeAnimMode.DINO -> 1
            BadgeAnimMode.CROCODILE -> 1
            BadgeAnimMode.GOAT -> 1
            BadgeAnimMode.EAGLE -> 1
            BadgeAnimMode.FLY -> 1
            BadgeAnimMode.TURTLE -> 0
            BadgeAnimMode.GOLD_MEDAL,
            BadgeAnimMode.SILVER_MEDAL,
            BadgeAnimMode.BRONZE_MEDAL -> null
        }
    }

    private val modesWithDynamicProgress = setOf(
        BadgeAnimMode.DAILY_ONLY,
        BadgeAnimMode.ROCKET_WITH_BASE,
        BadgeAnimMode.BOWLING_WITH_BASE,
        BadgeAnimMode.FISHING_WITH_BASE,
        BadgeAnimMode.GOLF_WITH_BASE,
        BadgeAnimMode.CUP_GOOGLE,
        BadgeAnimMode.KARATE,
        BadgeAnimMode.TORNADO,
        BadgeAnimMode.VOLCANO,
        BadgeAnimMode.DINO,
        BadgeAnimMode.CROCODILE,
        BadgeAnimMode.GOAT,
        BadgeAnimMode.EAGLE,
        BadgeAnimMode.FLY,
        BadgeAnimMode.TURTLE,
    )

    /**
     * Android `formatUnlockedDetailText`: şablondaki "%1\$s" yer tutucusu ilerleme değeriyle değiştirilir
     * (Android güncel: eskiden "x" harfiydi).
     *
     * @param celebrateTarget kutlama akışındaysa ve ARG_CELEBRATE_TARGET varsa o değer; yoksa null
     *   (o zaman [BadgeProgressRepository.getProgressValueByMode] kullanılır; o da null ise şablon aynen döner).
     */
    fun formatUnlockedDetailText(
        mode: BadgeAnimMode,
        template: String,
        celebrateTarget: Int? = null,
    ): String {
        if (mode !in modesWithDynamicProgress) return template
        val progress = celebrateTarget ?: BadgeProgressRepository.getProgressValueByMode(mode) ?: return template
        return template.replace("%1\$s", progress.toString())
    }

    /** Android `applyBadgeDetailText` metni. */
    fun badgeDetailText(mode: BadgeAnimMode, unlocked: Boolean = true, celebrateTarget: Int? = null): String =
        if (unlocked) {
            formatUnlockedDetailText(mode, unlockedDetailTemplate(mode), celebrateTarget)
        } else {
            lockedDetailText(mode)
        }

    /** Parça (madalya/kupa satırı) paneli gösterilebilen modlar. */
    val pieceModes: Set<BadgeAnimMode> = setOf(
        BadgeAnimMode.GOLD_MEDAL,
        BadgeAnimMode.SILVER_MEDAL,
        BadgeAnimMode.BRONZE_MEDAL,
        BadgeAnimMode.CUP_GOOGLE,
    )

    /** [pieceCountBadge] sonucu: "3x" etiketi ve panelin kupa mı madalya mı olduğu. */
    data class PieceCountBadge(val count: Int, val isCup: Boolean) {
        val text: String get() = "${count}x"
    }

    /**
     * Android `configureMedalPieceNamesList`: "Nx" etiketi gösterilecekse sayısı; gizlenecekse null.
     * Gizlenme koşulları: kutlama akışında sezon filtresi yoksa; kilitliyse; mod parça modu değilse; sayı 0 ise.
     *
     * @param seasonLbPieceFilter kutlama akışında ARG_SEASON_LB_ACK_SEASON değeri (yoksa null).
     */
    fun pieceCountBadge(
        mode: BadgeAnimMode,
        unlocked: Boolean,
        progress: UserBadgeProgress,
        isCelebrationFlow: Boolean,
        seasonLbPieceFilter: Int?,
    ): PieceCountBadge? {
        if (isCelebrationFlow && seasonLbPieceFilter == null) return null
        if (!unlocked || mode !in pieceModes) return null
        val (count, isCup) = when (mode) {
            BadgeAnimMode.GOLD_MEDAL -> {
                val n = progress.goldMedalPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to false
            }
            BadgeAnimMode.SILVER_MEDAL -> {
                val n = progress.silverMedalPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to false
            }
            BadgeAnimMode.BRONZE_MEDAL -> {
                val n = progress.bronzeMedalPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to false
            }
            BadgeAnimMode.CUP_GOOGLE -> {
                val n = progress.cupPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to true
            }
            else -> 0 to false
        }
        if (count <= 0) return null
        return PieceCountBadge(count, isCup)
    }

    /** Android `togglePieceRowsPanel` anahtarı (aynı anahtarla tekrar basınca panel kapanır). */
    fun piecePanelKey(mode: BadgeAnimMode, isCup: Boolean, pieceSeasonFilter: Int? = null): String =
        if (pieceSeasonFilter != null) {
            "${mode.name}|$isCup|s=$pieceSeasonFilter"
        } else {
            "${mode.name}|$isCup"
        }

    /** Android `togglePieceRowsPanel` (isCup dalı): gösterilecek kupa satırları (Firestore sırasıyla). */
    fun cupPieceRows(progress: UserBadgeProgress, pieceSeasonFilter: Int? = null): List<CupPieceRow> =
        progress.cupPiece.filter { pieceSeasonFilter == null || it.season == pieceSeasonFilter }

    /** Android `togglePieceRowsPanel` (madalya dalı): gösterilecek madalya satırları; madalya modu değilse boş. */
    fun medalPieceRows(
        mode: BadgeAnimMode,
        progress: UserBadgeProgress,
        pieceSeasonFilter: Int? = null,
    ): List<MedalPieceRow> = when (mode) {
        BadgeAnimMode.GOLD_MEDAL -> progress.goldMedalPiece.filter {
            pieceSeasonFilter == null || it.season == pieceSeasonFilter
        }
        BadgeAnimMode.SILVER_MEDAL -> progress.silverMedalPiece.filter {
            pieceSeasonFilter == null || it.season == pieceSeasonFilter
        }
        BadgeAnimMode.BRONZE_MEDAL -> progress.bronzeMedalPiece.filter {
            pieceSeasonFilter == null || it.season == pieceSeasonFilter
        }
        else -> emptyList()
    }

    /** Parça satırındaki küçük madalya Lottie dosyası (dosya adlarındaki çift ".json" Android'deki gibidir). */
    fun medalPieceAsset(mode: BadgeAnimMode): String? = when (mode) {
        BadgeAnimMode.GOLD_MEDAL -> "gold_medal_anim.json"
        BadgeAnimMode.SILVER_MEDAL -> "silver_medal_anim.json.json"
        BadgeAnimMode.BRONZE_MEDAL -> "bronze_medal_anim.json.json"
        else -> null
    }

    /** Parça satırındaki küçük kupa Lottie dosyası ([CUP_GOOGLE_HOLD_FRAME] karesinde durur). */
    const val CUP_PIECE_ASSET = "cup_google_anim.json"

    /** İlerleme çubuğu durumu: "current/target" metni ve 0..100 yüzde. */
    data class ProgressUi(val text: String, val percent: Int)

    /** Android `updateLockedProgressUi`: current en az 0, target en az 1; yüzde aşağı yuvarlanır ve 0..100'e kırpılır. */
    fun lockedProgressUi(lockedProgressCurrent: Int, lockedProgressTarget: Int): ProgressUi {
        val current = lockedProgressCurrent.coerceAtLeast(0)
        val target = lockedProgressTarget.coerceAtLeast(1)
        val percent = ((current.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100)
        return ProgressUi("$current/$target", percent)
    }

    /** Android `updateCelebrationProgressUi`: yüzde animasyon kesrinden gelir (current/target kırpılmaz). */
    fun celebrationProgressUi(fraction: Float, current: Int, target: Int): ProgressUi =
        ProgressUi("$current/$target", (fraction * 100f).toInt().coerceIn(0, 100))

    /** Kutlama sayaç animasyonunun süresi (ms) — Android `ValueAnimator.duration = 3000L`. */
    const val CELEBRATION_COUNT_DURATION_MS = 3000L

    /**
     * Android `startLevelUpCelebration` içindeki ValueAnimator güncellemesi: animasyon kesrine göre sayaç değeri.
     */
    fun celebrationAnimatedCurrent(fromProgress: Int, reachedTarget: Int, animatedFraction: Float): Int {
        val fraction = animatedFraction.coerceIn(0f, 1f)
        val span = (reachedTarget - fromProgress).coerceAtLeast(1)
        return (fromProgress + (span * fraction)).toInt().coerceAtMost(reachedTarget)
    }

    /** [levelUpCelebrationPlan] sonucu. */
    data class LevelUpCelebrationPlan(
        val oldShownValue: Int,
        val newShownValue: Int,
        val lockedProgressCurrent: Int,
        val lockedProgressTarget: Int,
        val previousTone: BadgeLevelTone?,
        val newTone: BadgeLevelTone?,
    )

    /**
     * Android `startLevelUpCelebration` başındaki hesaplar: eski/yeni gösterilen değer, ilerleme çubuğunun
     * başlangıcı/hedefi ve eski/yeni ton.
     */
    fun levelUpCelebrationPlan(
        mode: BadgeAnimMode,
        fromProgress: Int,
        toProgress: Int,
        reachedTarget: Int,
    ): LevelUpCelebrationPlan {
        val oldShownValue = BadgeProgressRepository.resolveShownValueByMode(mode, fromProgress)
            ?: resolveShownValueFallback(mode, fromProgress)
        val newShownValue = BadgeProgressRepository.resolveShownValueByMode(mode, toProgress)
            ?: resolveShownValueFallback(mode, toProgress)
        return LevelUpCelebrationPlan(
            oldShownValue = oldShownValue,
            newShownValue = newShownValue,
            lockedProgressCurrent = fromProgress.coerceAtLeast(0),
            lockedProgressTarget = reachedTarget.coerceAtLeast(1),
            previousTone = BadgeProgressRepository.resolveToneByMode(mode, fromProgress),
            newTone = BadgeProgressRepository.resolveToneByMode(mode, toProgress),
        )
    }

    /** Android'de `private data class LevelUpAnimConfig`. */
    data class LevelUpAnimConfig(
        val topAsset: String,
        val topHoldFrame: Float,
        val topScale: Float,
        val topSpeed: Float = 1f,
        val withCircleFrame: Boolean = false,
        val topRepeatCount: Int = 0,
        val growFromZero: Boolean = false,
    )

    fun levelUpAnimConfig(mode: BadgeAnimMode): LevelUpAnimConfig = when (mode) {
        BadgeAnimMode.DAILY_ONLY -> LevelUpAnimConfig("dart_anim.json", TOP_HOLD_FRAME_DART, 0.65f)
        BadgeAnimMode.ROCKET_WITH_BASE -> LevelUpAnimConfig("rocket_badge_anim2.json", TOP_HOLD_FRAME_ROCKET, 1f)
        BadgeAnimMode.BOWLING_WITH_BASE -> LevelUpAnimConfig("bowling_anim.json", TOP_HOLD_FRAME_BOWLING, 0.68f, topSpeed = 0.5f, withCircleFrame = true)
        BadgeAnimMode.FISHING_WITH_BASE -> LevelUpAnimConfig("fishing_pole_anim.json", TOP_HOLD_FRAME_FISHING, 0.65f)
        BadgeAnimMode.GOLF_WITH_BASE -> LevelUpAnimConfig("golf_anim.json", TOP_HOLD_FRAME_GOLF, 0.60f)
        BadgeAnimMode.KARATE -> LevelUpAnimConfig("karate_anim.json", KARATE_HOLD_FRAME, 1f)
        BadgeAnimMode.TORNADO -> LevelUpAnimConfig("tornado_anim.json", TOP_HOLD_FRAME_TORNADO, 0.65f, topSpeed = 1f, withCircleFrame = true)
        BadgeAnimMode.VOLCANO -> LevelUpAnimConfig("volcano_anim.json", TOP_HOLD_FRAME_VOLCANO, 0.65f, topSpeed = 1f, withCircleFrame = true, topRepeatCount = 1)
        BadgeAnimMode.DINO -> LevelUpAnimConfig("dinosaur_anim.json", TOP_HOLD_FRAME_GOLF, 0.5f, topSpeed = 1f, growFromZero = true)
        BadgeAnimMode.CROCODILE -> LevelUpAnimConfig("crocodile_anim.json", TOP_HOLD_FRAME_GOLF, 0.50f, topSpeed = 1f, growFromZero = true)
        BadgeAnimMode.GOAT -> LevelUpAnimConfig("goat_anim.json", TOP_HOLD_FRAME_GOLF, 0.50f, topSpeed = 1f, growFromZero = true, topRepeatCount = 1)
        BadgeAnimMode.EAGLE -> LevelUpAnimConfig("eagle_anim.json", TOP_HOLD_FRAME_GOLF, 0.50f, topSpeed = 1f, growFromZero = true, topRepeatCount = 1)
        BadgeAnimMode.FLY -> LevelUpAnimConfig("fly_anim.json", TOP_HOLD_FRAME_GOLF, 0.50f, topSpeed = 1f, growFromZero = true, topRepeatCount = 1)
        BadgeAnimMode.TURTLE -> LevelUpAnimConfig("turtle_anim.json", TOP_HOLD_FRAME_GOLF, 0.50f, topSpeed = 1f, growFromZero = true)
        else -> LevelUpAnimConfig("dart_anim.json", TOP_HOLD_FRAME_DART, 0.65f)
    }


    /** Android `resolveShownValueFallback`. */
    fun resolveShownValueFallback(mode: BadgeAnimMode, progress: Int): Int {
        val spec = BadgeProgressRepository.getLevelSpecByMode(mode) ?: return progress.coerceAtLeast(0)
        val levels = spec.first
        val step = spec.second
        val safe = progress.coerceAtLeast(0)
        if (safe < levels.first()) return levels.first()
        if (safe <= levels.last()) return levels.lastOrNull { it <= safe } ?: levels.first()
        val last = levels.last()
        val extra = (safe - last) / step
        return last + (extra * step)
    }

    /** Android `shouldShowProgressForMode`: ilerleme çubuğu gösterilen modlar. */
    fun shouldShowProgressForMode(mode: BadgeAnimMode): Boolean {
        return mode == BadgeAnimMode.DAILY_ONLY ||
            mode == BadgeAnimMode.ROCKET_WITH_BASE ||
            mode == BadgeAnimMode.BOWLING_WITH_BASE ||
            mode == BadgeAnimMode.FISHING_WITH_BASE ||
            mode == BadgeAnimMode.GOLF_WITH_BASE ||
            mode == BadgeAnimMode.TORNADO ||
            mode == BadgeAnimMode.VOLCANO ||
            mode == BadgeAnimMode.DINO ||
            mode == BadgeAnimMode.CROCODILE ||
            mode == BadgeAnimMode.GOAT ||
            mode == BadgeAnimMode.EAGLE ||
            mode == BadgeAnimMode.FLY ||
            mode == BadgeAnimMode.TURTLE
    }

    /** Android `BadgeFragment.frameToProgress`: kare → 0..1 Lottie ilerlemesi (aralık en az 1 kare sayılır). */
    fun frameToProgress(targetFrame: Float, startFrame: Float, endFrame: Float): Float {
        val span = (endFrame - startFrame).coerceAtLeast(1f)
        return ((targetFrame - startFrame) / span).coerceIn(0f, 1f)
    }
}
