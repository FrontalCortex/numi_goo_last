// KAYNAK: app/src/main/java/com/example/app/LessonAdapter.kt (+ RaceAdapter.kt içindeki satır durumu)
//   - showLessonBottomSheet()                 → [LessonAdapterLogic.lessonBottomSheetState]
//   - continueWithLesson() / againTutorial    → [LessonAdapterLogic.continueWithLessonTarget], [LessonAdapterLogic.lessonStartGlobals]
//   - actionButton tıklaması (enerji)         → [LessonAdapterLogic.actionButtonDecision]
//   - showRacePanel() kapanışı                → [LessonAdapterLogic.partIdAfterRacePanelClose]
//   - formatRaceTimePeriodDescription()       → [LessonAdapterLogic.formatRaceTimePeriodDescription]
//   - showRaceLessonBottomSheet()             → [LessonAdapterLogic.raceLessonBottomSheetState]
//   - RaceAdapter.RaceViewHolder.bind()       → [LessonAdapterLogic.raceRowStatus]
//   - RaceViewHolder.bind() (harita bayrağı)  → [LessonAdapterLogic.raceFlagClickAllowed]
//   - refreshRacePanelIfOpen()                → [LessonAdapterLogic.refreshedRacePanelItems]
//   - applyStepSegments() / lessonProgressKey → [LessonAdapterLogic.stepSegments], [LessonAdapterLogic.lessonProgressKey]
//   - applyStepSegmentsWithIncreaseAnimation()→ [LessonAdapterLogic.progressAnimationDecision]
//   - bindChestStarsRow() / applyCupIcon()    → [LessonAdapterLogic.chestStarsRow], [LessonAdapterLogic.normalizedCupIcon],
//                                               [LessonAdapterLogic.chestStarsForBind]
//   - bind() nefes animasyonu koşulu          → [LessonAdapterLogic.shouldStartBreathingAnimation]
//   - getItemViewType()                       → [LessonAdapterLogic.itemViewType]
//
// Harita listesinin (RecyclerView adapter) görünümden bağımsız mantığı: bir dersin kilitli mi / bitmiş mi olduğu,
// alt panelde hangi metin ve düğmenin çıkacağı, derse basınca hangi ekranın açılacağı, yıldız sırası vb.
//
// Güncel Android'de (soban-gamified-education) değişenler bu dosyaya işlendi: ders paneli artık "popover" (durum rengi:
// açık/bitmiş/kilitli), düğme metinleri (Başla / Devam et / Gözden geçir / Tekrar dene / Kilitli), kilitli dersin
// 1 anahtarla açılması, "Eğitimi tekrarla"nın da enerji harcaması, ilerleme anahtarının `{type}_{stableId}` olması,
// sandık yıldızlarının hep 3'lü satır olması, kart "nefes alma" koşulu. Platform işi olanlar (ortak kopyada yok):
// panelin karttan büyüyerek açılması, ekran/sistem şeridi karartması, ilerleme animasyonunun liste yenilenince kaldığı
// yerden sürmesi (runningProgressIncreases), TeacherApprovalGate (onaysız öğretmen engeli), Analytics olayları.
//
// Android'den bilinçli farklar: View / animasyon / renk kodu yok; R.drawable ve R.color kimlikleri yerine kaynak
// adları; metinler Android'deki sabit Türkçe metinlerle aynı (yerelleştirme anahtarına çevrilmedi çünkü Android'de
// de koda gömülü).
package com.sorobit.shared.ders

object LessonAdapterLogic {

    // ---------------------------------------------------------------------------------------------
    // Ders alt paneli (lesson_bottom_sheet)
    // ---------------------------------------------------------------------------------------------

    data class LessonBottomSheetState(
        val title: String,
        val description: String,
        /** Düğme metni. */
        val actionText: String,
        /**
         * Düğme etkin mi. Android "Gözden geçir" dalında isEnabled'a dokunmaz (yerleşim varsayılanı: etkin) → true.
         */
        val actionEnabled: Boolean,
        /** Kilitli görünüm (gri metin + koyu arka plan). */
        val locked: Boolean,
        /** "Eğitimi tekrarla" bağlantısı görünür mü. */
        val againTutorialVisible: Boolean,
        /** "Rekor: N" metni; ayarlanmıyorsa null (yerleşimde boş kalır). */
        val recordText: String?,
        /** Rekor satırı tamamen gizli mi (part 4 ve 5). */
        val recordLayoutGone: Boolean,
        /** Rekor satırına tıklanınca liderlik tablosu (RecordFragment) açılır mı. */
        val recordClickable: Boolean,
        /** Rekor satırındaki ateş animasyonu görünür mü (yeni panelde ateş animasyonu yok → hep false). */
        val fireAnimVisible: Boolean,
        /** Panel durumu (çerçeve/düğme rengi): açık mavi, bitmiş altın, kilitli gri. */
        val panelState: PanelState = PanelState.OPEN,
        /** Üst etiket: sandıkta "ÜNİTE MARATONU", derste "DERS". */
        val kindText: String = TEXT_KIND_LESSON,
        /** Panel ikonu: 1, 2, 3, 6. bölümlerde sandık "podium_ic2", diğerleri "profile_book_ic3". */
        val iconName: String = ICON_BOOK,
        /** Adım satırı (dilimli çubuk + "2/5") görünür mü: sandıkta ve kilitliyken gizli. */
        val stepRowVisible: Boolean = true,
        /** Adım satırındaki "dolu/toplam" sayıları. */
        val doneSteps: Int = 0,
        val stepCount: Int = 1,
        /** Kilitli ders 1 anahtarla açılabilir mi (düğme açık mavi, anahtar simgeli; açıklama kilit-anahtar metni). */
        val keyUnlockable: Boolean = false,
    )

    /** Ders panelinin durumu (Android: PanelState). */
    enum class PanelState { OPEN, DONE, LOCKED }

    const val TEXT_KIND_LESSON = "DERS"
    const val TEXT_KIND_CHEST = "ÜNİTE MARATONU"
    const val ICON_BOOK = "profile_book_ic3"
    const val ICON_PODIUM = "podium_ic2"
    const val TEXT_CONTINUE = "Devam et"
    const val TEXT_UNLOCK_WITH_KEY = "1 anahtarla kilidi aç"
    const val TEXT_UNLOCK_OFFER_DESCRIPTION = "Önceki dersleri tamamla ya da 1 anahtarla bu dersi hemen aç."
    /** Yarış panelindeki başlat düğmesi (Android: ui_baslat; artık simgesiz). */
    const val TEXT_RACE_START = "BAŞLAT"

    /** Kilitli dersi açmanın anahtar bedeli (Android: LESSON_UNLOCK_KEY_COST). */
    const val LESSON_UNLOCK_KEY_COST = 1

    /**
     * Kilitli bir item anahtarla açılabilir mi (Android: isKeyUnlockable): 1–6. bölümlerdeki ders ve ünite maratonu item'ları.
     * 7–8'de kilit `raceBusyLevel` ile yönetilir.
     */
    fun isKeyUnlockable(item: LessonItem, globalPartId: Int = GlobalLessonData.globalPartId): Boolean =
        !item.isCompleted && globalPartId in 1..6 &&
            (item.type == LessonItem.TYPE_LESSON || item.type == LessonItem.TYPE_CHEST)

    /**
     * Anahtar sunucuda düşüldükten SONRA yazılacak item (Android: unlockLessonWithKey): `isCompleted = true`. Can harcanmaz.
     * Platform: güncel item'ı aynı stableId ile bulup üzerine yazar ([LessonManager.updateLessonItem]).
     */
    fun unlockedWithKey(item: LessonItem): LessonItem = item.copy(isCompleted = true)

    /** Android: ui_kilidi_acmak_icin_onceki_dersleri_tamamla (eski metin: "Bunun kilidini açmak için yukarıdaki düzeylerin tümünü tamamla!"). */
    const val TEXT_LOCKED_DESCRIPTION = "Kilidi açmak için önceki dersleri tamamla."
    const val TEXT_LESSON_DONE = "Ders Tamamlandı"
    const val TEXT_RETRY = "Tekrar dene"
    const val TEXT_REVIEW = "Gözden geçir"
    /** Ders panelinde düğme (Android: ui_basla; eskiden "BAŞLAT"). */
    const val TEXT_START = "Başla"
    /** Ders panelinde düğme (Android: ui_kilitli; eskiden "KİLİTLİ"). */
    const val TEXT_LOCKED = "Kilitli"

    /** Rekor/liderlik satırını göstermeyen part'lar. */
    val RECORD_HIDDEN_PARTS: Set<Int> = setOf(4, 5)

    /**
     * Haritada bir derse dokunulduğunda açılan alt panelin içeriği.
     *
     * Güncel Android: tamamlanmış sandıkta `record` null ise metin "Rekor: —" olur (eskiden "Rekor: null").
     */
    fun lessonBottomSheetState(
        item: LessonItem,
        globalPartId: Int = GlobalLessonData.globalPartId,
    ): LessonBottomSheetState {
        val isChest = item.type == LessonItem.TYPE_CHEST
        val stepCount = item.stepCount.coerceAtLeast(1)
        val doneSteps = if (item.stepIsFinish) stepCount else item.stepCompletionStatus.count { it }.coerceIn(0, stepCount)
        val panelState = when {
            !item.isCompleted -> PanelState.LOCKED
            item.stepIsFinish -> PanelState.DONE
            else -> PanelState.OPEN
        }
        var description = ""
        val actionText: String
        val actionEnabled: Boolean
        var againTutorialVisible = false
        var recordText: String? = null
        var recordLayoutGone = true // yeni panelde rekor satırı yalnızca bitmiş sandıkta görünür
        var keyUnlockable = false

        if (item.isCompleted) {
            againTutorialVisible = item.tutorialNumber != 0 && item.tutorialIsFinish
            if (item.stepIsFinish) {
                if (isChest) {
                    // Rekor satırı (4. ve 5. bölümde yok). Maraton rehberinin son adımı bunu gösterir.
                    if (globalPartId !in RECORD_HIDDEN_PARTS) {
                        recordLayoutGone = false
                        recordText = "Rekor: ${item.record ?: "—"}"
                    }
                    actionText = TEXT_RETRY
                } else {
                    actionText = TEXT_REVIEW
                }
            } else {
                actionText = if (doneSteps > 0) TEXT_CONTINUE else TEXT_START
            }
            actionEnabled = true
        } else if (isKeyUnlockable(item, globalPartId)) {
            // Kilitli ders 1 anahtarla açılabilir; düğme "açık" mavisinde (gri kalsaydı devre dışı sanılırdı).
            keyUnlockable = true
            description = TEXT_UNLOCK_OFFER_DESCRIPTION
            actionText = TEXT_UNLOCK_WITH_KEY
            actionEnabled = true
        } else {
            description = TEXT_LOCKED_DESCRIPTION
            actionText = TEXT_LOCKED
            actionEnabled = false
        }

        val recordClickable = item.type == LessonItem.TYPE_CHEST && item.stepIsFinish && item.record != null &&
            globalPartId !in RECORD_HIDDEN_PARTS

        return LessonBottomSheetState(
            title = item.title,
            description = description,
            actionText = actionText,
            actionEnabled = actionEnabled,
            locked = panelState == PanelState.LOCKED && !keyUnlockable,
            againTutorialVisible = againTutorialVisible,
            recordText = recordText,
            recordLayoutGone = recordLayoutGone,
            recordClickable = recordClickable,
            fireAnimVisible = false,
            panelState = panelState,
            kindText = if (isChest) TEXT_KIND_CHEST else TEXT_KIND_LESSON,
            iconName = if (isChest && globalPartId in setOf(1, 2, 3, 6)) ICON_PODIUM else ICON_BOOK,
            stepRowVisible = !(isChest || panelState == PanelState.LOCKED),
            doneSteps = doneSteps,
            stepCount = stepCount,
            keyUnlockable = keyUnlockable,
        )
    }

    /** Alt paneldeki düğmeye basılınca ne olacağı. */
    enum class ActionButtonDecision {
        /** Kilitli ders: panel yalnızca daraltılır. */
        LOCKED_COLLAPSE,

        /** Enerji yetersiz: panel kapanır, mağaza (enerji yenileme) açılır. */
        NOT_ENOUGH_ENERGY,

        /** 1 enerji harcanır ve ders başlar. */
        USE_ENERGY_AND_START,

        /** Sonsuz enerji (Pro/Premium plan veya onaylı öğretmen): enerji harcanmadan ders başlar. */
        START_WITHOUT_ENERGY,

        /** Kilitli ama anahtarla açılabilir ders: 1 anahtar harcanır, item açılır ([unlockedWithKey]); anahtar yoksa mağaza. */
        UNLOCK_WITH_KEY,
    }

    /** Bir dersin enerji maliyeti. */
    const val LESSON_ENERGY_COST = 1

    /**
     * Not: Android'de "Eğitimi tekrarla" bağlantısı da aynı enerji kararını kullanır (anlatım bitince doğrudan dersin testine
     * geçtiği için bedavaya açan arka kapı olmasın): açık bir item için bu fonksiyonu çağırın.
     */
    fun actionButtonDecision(
        item: LessonItem,
        isInfiniteEnergy: Boolean,
        hasEnoughEnergy: Boolean,
        globalPartId: Int = GlobalLessonData.globalPartId,
    ): ActionButtonDecision {
        if (!item.isCompleted) {
            return if (isKeyUnlockable(item, globalPartId)) ActionButtonDecision.UNLOCK_WITH_KEY else ActionButtonDecision.LOCKED_COLLAPSE
        }
        if (!isInfiniteEnergy) {
            if (!hasEnoughEnergy) return ActionButtonDecision.NOT_ENOUGH_ENERGY
            return ActionButtonDecision.USE_ENERGY_AND_START
        }
        return ActionButtonDecision.START_WITHOUT_ENERGY
    }

    /** Derse başlanınca açılacak ekran (Android: Fragment sınıfı). */
    enum class LessonStartTarget {
        /** BlindingLessonFragment — zihinden (kör) işlem dersi. */
        BLINDING_LESSON,

        /** AbacusFragment — abaküslü ders. */
        ABACUS,

        /** TutorialFragment(item.tutorialNumber) — önce öğretici. */
        TUTORIAL,
    }

    /**
     * Android `continueWithLesson`: öğretici bitmemişse önce öğretici; bitmişse isBlinding'e göre kör ders ya da
     * abaküs dersi.
     */
    fun continueWithLessonTarget(item: LessonItem): LessonStartTarget =
        if (item.isBlinding == true) {
            if (item.tutorialIsFinish) LessonStartTarget.BLINDING_LESSON else LessonStartTarget.TUTORIAL
        } else {
            if (item.tutorialIsFinish) LessonStartTarget.ABACUS else LessonStartTarget.TUTORIAL
        }

    /** Ders başlarken ayarlanan global değerler (Android: GlobalValues.mapFragmentStepIndex / lessonStep). */
    data class LessonStartGlobals(val mapFragmentStepIndex: Int, val lessonStep: Int)

    /**
     * Android `item.mapFragmentIndex!!` ve `item.startStepNumber!!` kullanır: ikisinden biri null ise çöker.
     * Burada aynı durumda IllegalStateException atılır (katalogdaki başlık dışı tüm item'larda ikisi de dolu;
     * part 9 "Kupa Modu" yer tutucusunda startStepNumber null'dır).
     */
    fun lessonStartGlobals(item: LessonItem): LessonStartGlobals =
        LessonStartGlobals(
            mapFragmentStepIndex = item.mapFragmentIndex ?: error("mapFragmentIndex null"),
            lessonStep = item.startStepNumber ?: error("startStepNumber null"),
        )

    /**
     * Yarış başlatılırken (onRaceStartClicked) global değerler yalnızca null değilse güncellenir;
     * null alanlar için mevcut değer korunur.
     */
    fun raceStartGlobals(raceItem: LessonItem, current: LessonStartGlobals): LessonStartGlobals =
        LessonStartGlobals(
            mapFragmentStepIndex = raceItem.mapFragmentIndex ?: current.mapFragmentStepIndex,
            lessonStep = raceItem.startStepNumber ?: current.lessonStep,
        )

    // ---------------------------------------------------------------------------------------------
    // Yarış paneli
    // ---------------------------------------------------------------------------------------------

    /**
     * Yarış paneli kapanınca aktif part: part seçiminden açıldıysa değişmez; haritadaki bayraktan açıldıysa
     * `item.backRaceId!!` (null ise Android çöker → burada IllegalStateException).
     */
    fun partIdAfterRacePanelClose(item: LessonItem, isFromPartSelection: Boolean, currentGlobalPartId: Int): Int =
        if (!isFromPartSelection) item.backRaceId ?: error("backRaceId null") else currentGlobalPartId

    /** Haritadaki yarış bayrağına basılabilir mi (kilitliyse "Burayı açmak için bütün dersleri tamamla." uyarısı). */
    fun raceFlagClickAllowed(item: LessonItem): Boolean = item.isCompleted

    /** Android strings.xml: race_unlock_complete_all_lessons. */
    const val STRING_KEY_RACE_UNLOCK = "race_unlock_complete_all_lessons"

    /** Android strings.xml: daily_question_insufficient_keys ("Yeterli anahtarın yok."). */
    const val STRING_KEY_INSUFFICIENT_KEYS = "daily_question_insufficient_keys"

    /** Kilitli yarışı ileri sarmanın anahtar maliyeti. */
    const val RACE_FAST_FORWARD_KEY_COST = 1

    /**
     * "Sayı gösterilme periyodu N saniye" metni. Tam saniyeler ondalıksız ("3"), diğerleri tek ondalık basamak ve
     * virgülle ("2,5") yazılır; [timePeriodMs] null ise 1000 ms varsayılır.
     *
     * Android `String.format(Locale.US, "%.1f")` (HALF_UP yuvarlama) kullanır; burada aynı sonuç tam sayı aritmetiğiyle
     * üretilir (ms değeri 1000'e tam bölünmüyorsa onda birlik basamağa en yakın, yarımlar yukarı).
     */
    fun formatRaceTimePeriodDescription(timePeriodMs: Long?): String =
        "Sayı gösterilme periyodu ${formatRaceTimePeriodSeconds(timePeriodMs)} saniye"

    /**
     * [formatRaceTimePeriodDescription] metnindeki saniye ("3", "2,5"). Android'de bu değer
     * `ui_sayi_gosterilme_periyodu_x_saniye` kaynağına verilir; ondalık ayırıcı her dilde virgüldür
     * (`replace('.', ',')`). iOS, metni seçili dilde bu değerle kurar.
     */
    fun formatRaceTimePeriodSeconds(timePeriodMs: Long?): String {
        val ms = timePeriodMs ?: 1000L
        return if (ms % 1000L == 0L) {
            // Android: seconds.toInt() (Double → Int, Int sınırlarına sıkıştırılır)
            (ms / 1000.0).toInt().toString()
        } else {
            val negatif = ms < 0
            val mutlak = if (negatif) -ms else ms
            // Onda birlik basamağa HALF_UP yuvarla: (ms + 50) / 100
            val ondaBir = (mutlak + 50L) / 100L
            val tam = ondaBir / 10L
            val kesir = ondaBir % 10L
            (if (negatif) "-" else "") + "$tam,$kesir"
        }
    }

    data class RaceLessonBottomSheetState(
        val title: String,
        val description: String,
        /** Kilitli yarış (raceBusyLevel == 2): düğme anahtar simgesidir, metin boştur. */
        val isLockedRace: Boolean,
        /** Düğme metni; kilitliyse "". */
        val actionText: String,
        /** Düğme metni tümü büyük harf mi (Android: isAllCaps). */
        val actionAllCaps: Boolean,
        /** Düğmedeki simge kaynağı: kilitli "key"; başlat ve tekrar dene null (başlat simgesi kaldırıldı). */
        val actionIcon: String?,
    )

    fun raceLessonBottomSheetState(raceItem: LessonItem): RaceLessonBottomSheetState {
        val isLockedRace = raceItem.raceBusyLevel == 2
        val title = raceItem.raceTitle ?: raceItem.title
        val description = formatRaceTimePeriodDescription(raceItem.timePeriod)
        return if (isLockedRace) {
            RaceLessonBottomSheetState(title, description, true, "", false, "key")
        } else {
            when (raceItem.raceBusyLevel) {
                0 -> RaceLessonBottomSheetState(title, description, false, TEXT_RETRY, false, null)
                // Android: simge kaldırıldı (eskiden "lighting__1_"); metin ui_baslat (BAŞLAT, büyük harf).
                else -> RaceLessonBottomSheetState(title, description, false, TEXT_RACE_START, true, null)
            }
        }
    }

    /** Yarış panelindeki satırın durumu (Android: RaceAdapter.RaceViewHolder.bind). */
    data class RaceRowStatus(
        val statusText: String,
        val lockVisible: Boolean,
        val statusEnabled: Boolean,
    )

    fun raceRowStatus(item: LessonItem): RaceRowStatus = when (item.raceBusyLevel) {
        0 -> RaceRowStatus("TAMAMLANDI", lockVisible = false, statusEnabled = true)
        2 -> RaceRowStatus("KİLİTLİ", lockVisible = true, statusEnabled = false)
        1 -> RaceRowStatus("BAŞLA", lockVisible = false, statusEnabled = true)
        else -> RaceRowStatus("BAŞLA", lockVisible = false, statusEnabled = true)
    }

    /**
     * Android `refreshRacePanelIfOpen`: açık yarış panelinin başlığından ([currentTitle]) hangi yarış item'ının açık
     * olduğu bulunur (yoksa part 7 varsayılır); o part'ın şablonu, bellekteki [lessonItems] içinde AYNI BAŞLIKLI ilk
     * item ile değiştirilir (eşleşme başlığa göredir; Android davranışı aynen).
     */
    fun refreshedRacePanelItems(
        currentTitle: String,
        lessonItems: List<LessonItem> = GlobalLessonData.lessonItems,
    ): List<LessonItem> {
        val raceItem = lessonItems.find { it.title == currentTitle }
        val racePartId = raceItem?.racePartId ?: 7
        val baseRaceItems = GlobalLessonData.createLessonItems(racePartId)
        return baseRaceItems.map { baseItem ->
            val updatedItem = lessonItems.find { it.title == baseItem.title }
            updatedItem ?: baseItem
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Harita kartı: halka dilimleri, altın bitiş, yıldızlar
    // ---------------------------------------------------------------------------------------------

    /** Android `getItemViewType`: liste dışı pozisyonda TYPE_LESSON. */
    fun itemViewType(items: List<LessonItem>, position: Int): Int =
        items.getOrNull(position)?.type ?: LessonItem.TYPE_LESSON

    /** Kart arka planı: kilidi açık → "lesson_center_blue", kilitli → "lesson_locked" (renk kaynağı adları). */
    fun lessonCardColor(item: LessonItem): String = if (item.isCompleted) "lesson_center_blue" else "lesson_locked"

    /** Yarış bayrağı kartı: kilidi açık → "lesson_completed", kilitli → "lesson_locked". */
    fun raceCardColor(item: LessonItem): String = if (item.isCompleted) "lesson_completed" else "lesson_locked"

    data class StepSegments(val segmentCount: Int, val completedSegments: Int)

    /** İlerleme halkasının dilim sayısı ve dolu dilim sayısı (bitmiş derste tümü dolu). */
    fun stepSegments(item: LessonItem): StepSegments {
        val safeStepCount = item.stepCount.coerceAtLeast(1)
        val completedSteps = item.stepCompletionStatus.count { it }
        val filledSegments = if (item.stepIsFinish) safeStepCount else completedSteps
        return StepSegments(
            segmentCount = safeStepCount,
            completedSegments = filledSegments.coerceIn(0, safeStepCount),
        )
    }

    /** Dilimler arası boşluk açısı (derece). */
    const val SEGMENT_GAP_ANGLE = 16f

    /**
     * Android yerel görsel durum anahtarı: `{type}_{stableId}` (ChestFragment aynı biçimi üretir). Eskiden `item.id`
     * (hiç doldurulmuyordu) + başlık + liste sırası kullanılıyordu; araya ders eklenince kayıyordu. [adapterPosition]
     * imza uyumu için duruyor, anahtara girmiyor.
     */
    @Suppress("UNUSED_PARAMETER")
    fun lessonProgressKey(item: LessonItem, adapterPosition: Int): String = "${item.type}_${item.stableId}"

    /** [progressAnimationDecision] çıktısındaki eylem. */
    enum class ProgressAnimationAction {
        /** Kalıcı altın (bitmiş) görünümü uygula; animasyon yok. */
        APPLY_PERSISTENT_GOLD,

        /** `previousFilled` → `targetFilled` dolum animasyonu oynat (bitince hedef tam doluysa altın birleşme). */
        ANIMATE_INCREASE,

        /** Bekleyen animasyon var ama henüz tüketilemez: halkayı ESKİ değerde (`previousFilled`) göster. */
        SHOW_PREVIOUS,

        /** Halkayı doğrudan hedef değerde göster. */
        SHOW_TARGET,

        /** Halkayı hedefte göster ve altın birleşme animasyonunu oynat (ilk kez tam dolu görüldü). */
        SHOW_TARGET_AND_PLAY_GOLD,
    }

    data class ProgressAnimationDecision(
        val action: ProgressAnimationAction,
        val segmentCount: Int,
        val previousFilled: Int,
        val targetFilled: Int,
        /** ANIMATE_INCREASE süresi (ms): dilim başına 900 ms, en az 1800 ms; diğer eylemlerde 0. */
        val durationMs: Long,
        /** ANIMATE_INCREASE bitince altın birleşme animasyonu oynatılacak mı. */
        val playGoldAfterIncrease: Boolean,
        /** Bekleyen animasyon kaydı (`GlobalValues.pendingLessonProgressAnimations[key]`) silinmeli mi. */
        val removePending: Boolean,
        /** `lastSeenFilledSegments[key]` için yazılacak yeni değer. */
        val lastSeenFilled: Int,
        /** `persistFinalGoldVisualState` çağrılmalı mı (finalGoldVisualUnlocked = true + kalıcı yazma). */
        val persistFinalGold: Boolean,
    )

    /**
     * Android `applyStepSegmentsWithIncreaseAnimation` karar ağacı.
     *
     * @param goldAnimationPlayed `playedFinalGoldAnimationKeys.contains(key)`
     * @param pendingFromFilled `GlobalValues.pendingLessonProgressAnimations[key]?.fromFilledSegments`
     * @param canConsumePending `GlobalValues.canConsumePendingLessonProgressAnimations`
     * @param lastSeenFilled `lastSeenFilledSegments[key]`
     */
    fun progressAnimationDecision(
        item: LessonItem,
        goldAnimationPlayed: Boolean,
        pendingFromFilled: Int?,
        canConsumePending: Boolean,
        lastSeenFilled: Int?,
    ): ProgressAnimationDecision {
        val safeStepCount = item.stepCount.coerceAtLeast(1)
        val completedSteps = item.stepCompletionStatus.count { it }
        val targetFilled = if (item.stepIsFinish) safeStepCount else completedSteps.coerceIn(0, safeStepCount)
        if ((goldAnimationPlayed || item.finalGoldVisualUnlocked) && targetFilled == safeStepCount) {
            return ProgressAnimationDecision(
                action = ProgressAnimationAction.APPLY_PERSISTENT_GOLD,
                segmentCount = safeStepCount,
                previousFilled = targetFilled,
                targetFilled = targetFilled,
                durationMs = 0L,
                playGoldAfterIncrease = false,
                removePending = false,
                lastSeenFilled = targetFilled,
                persistFinalGold = !item.finalGoldVisualUnlocked,
            )
        }
        val pending = pendingFromFilled
        val shouldConsumePending = canConsumePending && pending != null
        val previousFilled = when {
            pending != null -> pending.coerceIn(0, safeStepCount)
            else -> lastSeenFilled?.coerceIn(0, safeStepCount) ?: targetFilled
        }

        if (targetFilled > previousFilled) {
            return if (shouldConsumePending) {
                ProgressAnimationDecision(
                    action = ProgressAnimationAction.ANIMATE_INCREASE,
                    segmentCount = safeStepCount,
                    previousFilled = previousFilled,
                    targetFilled = targetFilled,
                    durationMs = ((targetFilled - previousFilled) * 900L).coerceAtLeast(1800L),
                    playGoldAfterIncrease = targetFilled == safeStepCount,
                    removePending = true,
                    lastSeenFilled = targetFilled,
                    persistFinalGold = false,
                )
            } else if (pending != null) {
                ProgressAnimationDecision(
                    action = ProgressAnimationAction.SHOW_PREVIOUS,
                    segmentCount = safeStepCount,
                    previousFilled = previousFilled,
                    targetFilled = targetFilled,
                    durationMs = 0L,
                    playGoldAfterIncrease = false,
                    removePending = false,
                    lastSeenFilled = previousFilled,
                    persistFinalGold = false,
                )
            } else {
                ProgressAnimationDecision(
                    action = ProgressAnimationAction.SHOW_TARGET,
                    segmentCount = safeStepCount,
                    previousFilled = previousFilled,
                    targetFilled = targetFilled,
                    durationMs = 0L,
                    playGoldAfterIncrease = false,
                    removePending = false,
                    lastSeenFilled = targetFilled,
                    persistFinalGold = false,
                )
            }
        }
        val playGold = targetFilled == safeStepCount && !goldAnimationPlayed && !item.finalGoldVisualUnlocked
        return ProgressAnimationDecision(
            action = if (playGold) ProgressAnimationAction.SHOW_TARGET_AND_PLAY_GOLD else ProgressAnimationAction.SHOW_TARGET,
            segmentCount = safeStepCount,
            previousFilled = previousFilled,
            targetFilled = targetFilled,
            durationMs = 0L,
            playGoldAfterIncrease = false,
            removePending = shouldConsumePending,
            lastSeenFilled = targetFilled,
            persistFinalGold = false,
        )
    }

    /**
     * Kart bağlanırken "nefes alma" halka animasyonu başlatılır mı: altın bitiş görünümündeki (bitmiş + altın
     * animasyonu oynamış/kalıcı) kartlar hariç hepsinde evet.
     */
    fun shouldStartBreathingAnimation(item: LessonItem, goldAnimationPlayed: Boolean): Boolean {
        val goldDone = (goldAnimationPlayed || item.finalGoldVisualUnlocked) && item.stepIsFinish
        return item.isCompleted && !item.stepIsFinish && !goldDone
    }

    /** Ders/sandık kartının durumu (Android: LessonViewHolder): kilitli gri, açık mavi, bitmiş altın. */
    enum class CardState { LOCKED, OPEN, DONE }

    /**
     * Kart durumu. Bitmiş kartın altın olması için halka dolum animasyonunun oynamış ya da kalıcı kaydedilmiş olması
     * ([goldAnimationPlayed] / [LessonItem.finalGoldVisualUnlocked]) gerekir; ilerleme animasyonu sürerken kart hâlâ mavidir.
     */
    fun cardState(item: LessonItem, goldAnimationPlayed: Boolean = false): CardState = when {
        !item.isCompleted -> CardState.LOCKED
        item.stepIsFinish && (goldAnimationPlayed || item.finalGoldVisualUnlocked) -> CardState.DONE
        else -> CardState.OPEN
    }

    /** Sandık kartındaki yıldız satırı (yeni kart tasarımı): hep 3 yuva; [earned] kadarı dolu. */
    data class ChestCardStars(
        /** Kazanılan yıldız sayısı (0..3). */
        val earned: Int,
        /** Kart açık mı (kilitli kartta boş yıldızlar soluk, alfa 0.55). */
        val unlocked: Boolean,
        /** Bitmiş (altın) kart: kazanılanlar koyu kahve dolu, kazanılmayanlar soluk (alfa 0.3). */
        val done: Boolean,
    ) {
        /** Boş yıldızın şeffaflığı (Android: done → 0.3, açık → 1, kilitli → 0.55). */
        val emptyStarAlpha: Float get() = if (done) 0.3f else if (unlocked) 1f else 0.55f
    }

    /**
     * Sandığın kazandığı yıldız sayısı (Android: chestStarCount): bitmemişse 0; bitmişse stepCupIcon'a göre
     * tier3/cup_ic3 → 3, tier2/cup_ic2 → 2, tier1/cup_ic → 1, diğer her şey 0 (tier0 dahil: tek ortada yıldız yok artık).
     */
    fun chestStarCount(item: LessonItem): Int {
        if (!item.stepIsFinish) return 0
        return when (item.stepCupIcon) {
            ChestIcons.CHEST_STARS_TIER3, ChestIcons.CUP_IC3 -> 3
            ChestIcons.CHEST_STARS_TIER2, ChestIcons.CUP_IC2 -> 2
            ChestIcons.CHEST_STARS_TIER1, ChestIcons.CUP_IC -> 1
            else -> 0
        }
    }

    /**
     * Sandık kartı bağlanırken yıldızlar (Android: LessonViewHolder.bind → bindChestStars). Eski kayıtlardaki boş
     * ("0") görsel şablon varsayılanına (tier0) çekilir — Android'deki `if (stepCupIcon == 0) stepCupIcon = tier0` yan etkisi.
     */
    fun chestCardStars(item: LessonItem, goldDone: Boolean = false): ChestCardStars {
        if (item.stepCupIcon.isEmpty()) item.stepCupIcon = ChestIcons.CHEST_STARS_TIER0
        return ChestCardStars(earned = chestStarCount(item), unlocked = item.isCompleted, done = goldDone)
    }

    /** Kartın sol ikonu: 1, 2, 3, 6. bölümlerde sandık "podium_ic2", diğerleri "profile_book_ic3". */
    fun cardIconName(item: LessonItem, globalPartId: Int = GlobalLessonData.globalPartId): String =
        if (item.type == LessonItem.TYPE_CHEST && globalPartId in setOf(1, 2, 3, 6)) ICON_PODIUM else ICON_BOOK

    /** Sandık kartındaki tek bir yıldız yuvasının görünümü. */
    enum class ChestStarSlot {
        /** Sarı dolu yıldız (star_on_ic). */
        YellowOn,

        /** Açık gri boyalı dolu yıldız (star_on_ic + gri filtre). */
        LightGrayOn,

        /** Boş yıldız (star_off_ic). */
        Off,
    }

    data class ChestStarsRow(
        /** true: 3 yıldızlı sıra; false: ortada tek yıldız. */
        val threeStarMode: Boolean,
        /** threeStarMode ise 3, değilse 1 yuva. */
        val slots: List<ChestStarSlot>,
    )

    /**
     * Android `bindChestStarsRow(tierResId, isCompleted)`. Boş String, Android'deki `0` kimliğinin karşılığıdır.
     */
    fun chestStarsRow(tierResId: String, isCompleted: Boolean): ChestStarsRow = when (tierResId) {
        "", ChestIcons.CHEST_STARS_TIER0 ->
            ChestStarsRow(false, listOf(if (isCompleted) ChestStarSlot.YellowOn else ChestStarSlot.LightGrayOn))
        ChestIcons.CHEST_STARS_TIER3 ->
            ChestStarsRow(true, listOf(ChestStarSlot.YellowOn, ChestStarSlot.YellowOn, ChestStarSlot.YellowOn))
        ChestIcons.CHEST_STARS_TIER2 ->
            ChestStarsRow(true, listOf(ChestStarSlot.YellowOn, ChestStarSlot.YellowOn, ChestStarSlot.Off))
        ChestIcons.CHEST_STARS_TIER1 ->
            ChestStarsRow(true, listOf(ChestStarSlot.YellowOn, ChestStarSlot.Off, ChestStarSlot.Off))
        ChestIcons.CUP_IC3 ->
            ChestStarsRow(true, listOf(ChestStarSlot.YellowOn, ChestStarSlot.YellowOn, ChestStarSlot.YellowOn))
        ChestIcons.CUP_IC2 ->
            ChestStarsRow(true, listOf(ChestStarSlot.YellowOn, ChestStarSlot.YellowOn, ChestStarSlot.Off))
        ChestIcons.CUP_IC ->
            ChestStarsRow(true, listOf(ChestStarSlot.YellowOn, ChestStarSlot.Off, ChestStarSlot.Off))
        else ->
            ChestStarsRow(false, listOf(if (isCompleted) ChestStarSlot.YellowOn else ChestStarSlot.LightGrayOn))
    }

    /**
     * Android `applyCupIcon` normalleştirmesi: boş ("0") ya da bilinmeyen görsel → chest_stars_tier0.
     * (Dikkat: "star_on_ic" bilinen kümede DEĞİLDİR; kör derslerin sandığı haritada tier0 olarak görünür.)
     */
    fun normalizedCupIcon(stepCupIcon: String): String = when (stepCupIcon) {
        ChestIcons.CHEST_STARS_TIER0,
        ChestIcons.CHEST_STARS_TIER1,
        ChestIcons.CHEST_STARS_TIER2,
        ChestIcons.CHEST_STARS_TIER3,
        ChestIcons.CUP_IC,
        ChestIcons.CUP_IC2,
        ChestIcons.CUP_IC3,
        -> stepCupIcon
        else -> ChestIcons.CHEST_STARS_TIER0
    }

    /**
     * Sandık kartı bağlanırken yıldız sırası (Android: LessonViewHolder.bind → TYPE_CHEST dalı).
     * Android ile aynı yan etki: [item].stepCupIcon YERİNDE normalleştirilir — bitmemiş sandıkta tier0'a çekilir,
     * bitmiş sandıkta boş/bilinmeyen görsel tier0 yapılır.
     */
    fun chestStarsForBind(item: LessonItem): ChestStarsRow {
        return if (item.stepIsFinish) {
            val normalized = normalizedCupIcon(item.stepCupIcon)
            if (normalized != item.stepCupIcon) {
                item.stepCupIcon = normalized
            }
            chestStarsRow(normalized, item.isCompleted)
        } else {
            item.stepCupIcon = ChestIcons.CHEST_STARS_TIER0
            chestStarsRow(ChestIcons.CHEST_STARS_TIER0, item.isCompleted)
        }
    }
}
