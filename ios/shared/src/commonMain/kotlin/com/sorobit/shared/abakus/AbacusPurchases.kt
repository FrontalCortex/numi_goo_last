// Kaynak (Android):
//  - app/src/main/java/com/example/app/BeadPurchaseFirestore.kt  (BeadData, haveBead alanları)
//  - app/src/main/java/com/example/app/FramePurchaseFirestore.kt (FrameData, haveFrame alanları)
//  - app/src/main/java/com/example/app/AbacusCustomizationFragment.kt
//      (getBeadPrice, PAID_BEAD_INFO, anahtarla satılan tipler, paket/stok/yerleştirme kuralları,
//       çerçeve fiyatları, renk özelliği kilidi)
//  - app/src/main/java/com/example/app/abacus/AbacusSoundPlayer.kt (ses seçimi sabitleri)
//
// Firestore okuma-yazması ve cüzdan (UserWalletFirestore — `oyun` paketi) çağrıları taşınmadı;
// burada yalnızca modeller, alan adları, Map ↔ model eşlemesi ve satın alma kuralı hesapları var.
// `purchasedAt` alanına platform katmanı `Timestamp.now()` (cihaz saati) yazar.
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.BeadType
import com.sorobit.shared.abakus.AbacusPreferences.FrameType

data class BeadData(val count: Int, val colorFeatureActive: Boolean)

/**
 * "haveBead" alt koleksiyonu. Yol: users/{uid}/haveBead/{beadId}
 * Alanlar: { purchasedAt: Timestamp, count: Int, colorFeatureActive: Boolean }
 * "beadId" = BeadType enum adı (ör. "SOROBAN2", "BOWLING").
 */
object BeadPurchaseFirestore {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_HAVE_BEAD = "haveBead"
    const val FIELD_PURCHASED_AT = "purchasedAt"
    const val FIELD_COUNT = "count"
    const val FIELD_COLOR_FEATURE = "colorFeatureActive"

    fun collectionPath(uid: String): String = "$COLLECTION_USERS/$uid/$COLLECTION_HAVE_BEAD"
    fun docPath(uid: String, beadId: String): String = "${collectionPath(uid)}/$beadId"

    /**
     * Doküman → [BeadData] (Android: loadOwnedBeads içindeki eşleme).
     * `count` yoksa: `purchasedAt` alanı VARSA 25 (eski "tamamı alındı" kayıtları), yoksa 0.
     * (Firestore `contains` alan null olsa da true döner; bu yüzden `containsKey` kullanılır.)
     */
    fun beadDataFromDocument(data: Map<String, Any?>): BeadData {
        val count = (data[FIELD_COUNT] as? Number)?.toInt() ?: if (data.containsKey(FIELD_PURCHASED_AT)) 25 else 0
        val colorFeatureActive = data[FIELD_COLOR_FEATURE] as? Boolean ?: false
        return BeadData(count, colorFeatureActive)
    }

    /** Android: loadOwnedBeads sonucu (doküman kimliği → veri). Hata durumunda boş Map döndürülür. */
    fun ownedBeadsFromDocuments(docs: Map<String, Map<String, Any?>>): Map<String, BeadData> =
        docs.mapValues { beadDataFromDocument(it.value) }

    /** setBeadCount'un yazdığı veri (merge); platform `purchasedAt` = şimdi ekler. */
    fun beadCountData(count: Int): Map<String, Any> = mapOf(FIELD_COUNT to count)

    /** setColorFeatureActive'in yazdığı veri (merge; sayıya dokunmaz). */
    fun colorFeatureActiveData(): Map<String, Any> = mapOf(FIELD_COLOR_FEATURE to true)
}

data class FrameData(val owned: Boolean, val colorFeatureActive: Boolean)

/**
 * Çerçeve sahipliği. Yol: users/{uid}/haveFrame/{frameId} (frameId = FrameType adı)
 * Alanlar: { purchasedAt: Timestamp, colorFeatureActive: Boolean }
 */
object FramePurchaseFirestore {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_HAVE_FRAME = "haveFrame"
    const val FIELD_PURCHASED_AT = "purchasedAt"
    const val FIELD_COLOR_FEATURE = "colorFeatureActive"

    fun collectionPath(uid: String): String = "$COLLECTION_USERS/$uid/$COLLECTION_HAVE_FRAME"
    fun docPath(uid: String, frameType: FrameType): String = "${collectionPath(uid)}/${frameType.name}"

    /** Doküman → [FrameData]: `purchasedAt` alanı varsa sahip sayılır. */
    fun frameDataFromDocument(data: Map<String, Any?>): FrameData {
        val owned = data.containsKey(FIELD_PURCHASED_AT)
        val colorActive = data[FIELD_COLOR_FEATURE] as? Boolean ?: false
        return FrameData(owned, colorActive)
    }

    fun ownedFramesFromDocuments(docs: Map<String, Map<String, Any?>>): Map<String, FrameData> =
        docs.mapValues { frameDataFromDocument(it.value) }

    /** setColorFeatureActive'in yazdığı veri (merge). markFrameOwned yalnızca `purchasedAt` yazar. */
    fun colorFeatureActiveData(): Map<String, Any> = mapOf(FIELD_COLOR_FEATURE to true)
}

/** Android: AbacusCustomizationFragment içindeki satın alma / kilit / stok kuralları. */
object AbacusCustomizationRules {

    /** Bir satın almada eklenen boncuk adedi. */
    const val BEAD_PACK_SIZE = 5

    /** Bir tipten sahip olunabilecek en çok boncuk (abaküsteki 25 yuva). */
    const val MAX_BEAD_COUNT = 25

    /** Boncuk ve çerçeve için "renk özelliği" fiyatı (altın). */
    const val COLOR_FEATURE_PRICE = 2000

    /** Para birimi kaynak adları (R.drawable.key / R.drawable.gold_ic). */
    const val ICON_KEY = "key"
    const val ICON_GOLD = "gold_ic"

    const val MESSAGE_NOT_ENOUGH_KEYS = "Yetersiz anahtar"
    const val MESSAGE_NOT_ENOUGH_GOLD = "Yetersiz altın"
    const val MESSAGE_OUT_OF_STOCK = "Stokta kalmadı"
    const val MESSAGE_NO_BEADS_LEFT = "Bu boncuktan kalmadı!"
    const val MESSAGE_SAVE_ERROR_RETRY = "Kayıt hatası. Tekrar deneyin."
    const val MESSAGE_OPERATION_FAILED = "İşlem başarısız. Tekrar deneyin."
    const val MESSAGE_ERROR = "Hata oluştu"
    const val MESSAGE_SAVE_ERROR = "Kayıt hatası"

    /** Seçili kart çerçeve rengi (#FFFFD600) ve kalınlığı (px). */
    const val SELECTED_STROKE_COLOR = 0xFFFFD600.toInt()
    const val SELECTED_STROKE_WIDTH = 6

    /**
     * Ücretli boncuk kartları, ekrandaki sırayla: (tip, görsel kaynak adı).
     * SOROBAN kasıtlı olarak yok — her zaman ücretsiz.
     */
    val PAID_BEAD_INFO: List<Pair<BeadType, String>> = listOf(
        BeadType.SOROBAN2 to "soroban_bead2",
        BeadType.SOROBAN6 to "soroban_bead6",
        BeadType.BOWLING to "bowling_ball_ic",
        BeadType.BALL1 to "abacus_bead_ball1",
        BeadType.BALL3 to "abacus_bead_ball3",
        BeadType.BALL4 to "abacus_bead_ball4",
        BeadType.BALL5 to "abacus_bead_ball5",
        BeadType.BALL6 to "abacus_bead_ball6",
        BeadType.BALL7 to "abacus_bead_ball7",
        BeadType.BALL8 to "abacus_bead_ball8",
        BeadType.BALL9 to "abacus_bead_ball9",
        BeadType.BALL10 to "abacus_bead_ball10",
        BeadType.BALL11 to "abacus_bead_ball11",
        BeadType.BALL12 to "abacus_bead_ball12",
        BeadType.BALL13 to "abacus_bead_ball13",
        BeadType.BALL14 to "abacus_bead_ball14",
        BeadType.ANIMAL to "animal_head_ic1",
        BeadType.ANIMAL2 to "animal_head_ic2",
        BeadType.ANIMAL3 to "animal_head_ic3",
        BeadType.ANIMAL4 to "animal_head_ic4",
        BeadType.ANIMAL5 to "animal_head_ic5",
        BeadType.ANIMAL6 to "animal_head_ic6",
        BeadType.ANIMAL7 to "animal_head_ic7",
        BeadType.ANIMAL8 to "animal_head_ic8",
        BeadType.ANIMAL9 to "animal_head_ic9",
    )

    /** Renk özelliği satın alma panelindeki görsel: listede yoksa (SOROBAN) "soroban_bead". */
    fun beadImageResName(beadType: BeadType): String =
        PAID_BEAD_INFO.firstOrNull { it.first == beadType }?.second ?: "soroban_bead"

    /** 5'li paket fiyatı. ANIMAL3 / ANIMAL8 ANAHTARLA, diğerleri altınla satılır. */
    fun getBeadPrice(beadType: BeadType): Int {
        return when (beadType) {
            BeadType.SOROBAN -> 0
            BeadType.SOROBAN2, BeadType.SOROBAN6 -> 2000
            BeadType.BOWLING, BeadType.BALL1, BeadType.BALL3, BeadType.BALL4, BeadType.BALL5, BeadType.BALL6, BeadType.BALL7 -> 3000
            BeadType.BALL8, BeadType.BALL9, BeadType.BALL10, BeadType.BALL11, BeadType.BALL12, BeadType.BALL13, BeadType.BALL14 -> 4000
            BeadType.ANIMAL3, BeadType.ANIMAL8 -> 40
            BeadType.ANIMAL, BeadType.ANIMAL2, BeadType.ANIMAL4, BeadType.ANIMAL5, BeadType.ANIMAL6, BeadType.ANIMAL7, BeadType.ANIMAL9 -> 5000
        }
    }

    fun isKeyCurrency(beadType: BeadType): Boolean =
        (beadType == BeadType.ANIMAL3 || beadType == BeadType.ANIMAL8)

    fun currencyIconResName(beadType: BeadType): String = if (isKeyCurrency(beadType)) ICON_KEY else ICON_GOLD

    /** Kart kilidi: ücretli tipte en az bir boncuk alınmışsa açık. */
    fun isBeadOwned(ownedBeads: Map<String, BeadData>, beadType: BeadType): Boolean =
        ((ownedBeads[beadType.name]?.count ?: 0) > 0)

    /** Sahip olunan toplam adet: SOROBAN her zaman 25. */
    fun ownedCount(ownedBeads: Map<String, BeadData>, beadType: BeadType): Int =
        if (beadType == BeadType.SOROBAN) 25 else ((ownedBeads[beadType.name]?.count ?: 0))

    /** Abaküsteki 25 yuvadan kaçında [beadType] yerleştirilmiş. */
    fun placedCount(store: AbacusPrefsStore, uid: String?, beadType: BeadType): Int {
        var placedCount = 0
        for (r in 0..4) {
            if (AbacusPreferences.getBeadTypeForSlot(store, uid, r, true) == beadType) placedCount++
            for (i in 0..3) {
                if (AbacusPreferences.getBeadTypeForSlot(store, uid, r, false, i) == beadType) placedCount++
            }
        }
        return placedCount
    }

    /** Envanterde kalan = toplam − yerleştirilen (negatif olabilir; Android "x-3" gibi gösterir). */
    fun availableCount(ownedBeads: Map<String, BeadData>, store: AbacusPrefsStore, uid: String?, beadType: BeadType): Int =
        ownedCount(ownedBeads, beadType) - placedCount(store, uid, beadType)

    /** Satın al düğmesi: toplam 25'e ulaştıysa gri (ve tıklanınca "Stokta kalmadı"). */
    fun isStockMaxed(ownedBeads: Map<String, BeadData>, beadType: BeadType): Boolean =
        ownedCount(ownedBeads, beadType) >= MAX_BEAD_COUNT

    fun hasEnough(beadType: BeadType, cachedKeys: Int, cachedCurrency: Int): Boolean {
        val price = getBeadPrice(beadType)
        return if (isKeyCurrency(beadType)) cachedKeys >= price else cachedCurrency >= price
    }

    fun notEnoughMessage(beadType: BeadType): String =
        if (isKeyCurrency(beadType)) MESSAGE_NOT_ENOUGH_KEYS else MESSAGE_NOT_ENOUGH_GOLD

    /**
     * Satın alma sonrası yeni adet.
     * DİKKAT (Android davranışı): ilk satın alma panelinde (showPurchasePanel) 25 üst sınırı
     * denetlenmez; yalnızca yuva panelindeki satın al düğmesi `count >= 25` iken engeller.
     * Üst sınır kırpması da yoktur (24 → 29 olabilir; pratikte adetler 5'in katı).
     */
    fun countAfterPurchase(currentCount: Int): Int = currentCount + BEAD_PACK_SIZE

    /** Satın alma başarılı olunca yerel sahiplik Map'inin yeni hâli (renk özelliği korunur). */
    fun withBeadCount(ownedBeads: Map<String, BeadData>, beadType: BeadType, newCount: Int): Map<String, BeadData> =
        ownedBeads + (beadType.name to (ownedBeads[beadType.name]?.copy(count = newCount) ?: BeadData(newCount, false)))

    fun withBeadColorFeature(ownedBeads: Map<String, BeadData>, beadType: BeadType): Map<String, BeadData> {
        val currentData = ownedBeads[beadType.name] ?: BeadData(0, false)
        return ownedBeads + (beadType.name to currentData.copy(colorFeatureActive = true))
    }

    /** Yuvaya yerleştirme sonucu. */
    enum class PlaceResult { SAME_TYPE_IGNORED, NO_BEADS_LEFT, PLACED }

    /**
     * Android: showBeadSlotSelectPanel.placeBead. Aynı tip zaten yerleşikse yok sayılır; envanter
     * kalmadıysa reddedilir; aksi hâlde yalnızca YEREL tercihe yazılır (Firestore'daki adet toplam
     * satın alınan olarak kalır).
     */
    fun placeBead(
        ownedBeads: Map<String, BeadData>,
        store: AbacusPrefsStore,
        uid: String?,
        beadType: BeadType,
        rod: Int,
        isTop: Boolean,
        beadIndex: Int = 0,
    ): PlaceResult {
        val oldType = AbacusPreferences.getBeadTypeForSlot(store, uid, rod, isTop, beadIndex)
        if (oldType == beadType) return PlaceResult.SAME_TYPE_IGNORED

        val available = availableCount(ownedBeads, store, uid, beadType)
        if (available <= 0) return PlaceResult.NO_BEADS_LEFT

        AbacusPreferences.setBeadTypeForSlot(store, uid, rod, isTop, beadIndex, beadType)
        return PlaceResult.PLACED
    }

    /**
     * Boncuk renk sekmesi kilidi: yalnızca Firestore'da `colorFeatureActive == true` ise açık.
     * DİKKAT (Android davranışı): ücretsiz SOROBAN için de renk özelliği satın alınmalıdır.
     */
    fun isBeadColorFeatureActive(ownedBeads: Map<String, BeadData>, beadType: BeadType): Boolean =
        ownedBeads[beadType.name]?.colorFeatureActive == true

    // ── Çerçeveler ────────────────────────────────────────────────────────────

    /** Çerçeve fiyatı (altın): ahşap ücretsiz, kalpli/yıldızlı 2000, gradyanlı 5000. */
    fun getFramePrice(frameType: FrameType): Int = when (frameType) {
        FrameType.FRAME_BG -> 0
        FrameType.FRAME_BG2 -> 2000
        FrameType.FRAME_BG3 -> 2000
        FrameType.FRAME_BG4 -> 5000
    }

    /** Karta dokununca: ahşap her zaman seçilebilir; diğerleri sahipse seçilir, değilse satın alma paneli. */
    fun isFrameSelectable(ownedFrames: Map<String, FrameData>, frameType: FrameType): Boolean =
        frameType == FrameType.FRAME_BG || ownedFrames[frameType.name]?.owned == true

    /** Çerçeve renk sekmesi: ahşap için hep açık; diğerlerinde `colorFeatureActive` gerekir. */
    fun isFrameColorUnlocked(ownedFrames: Map<String, FrameData>, currentFrameType: FrameType): Boolean =
        currentFrameType == FrameType.FRAME_BG ||
            ownedFrames[currentFrameType.name]?.colorFeatureActive == true

    fun withFrameOwned(ownedFrames: Map<String, FrameData>, frameType: FrameType): Map<String, FrameData> {
        val currentData = ownedFrames[frameType.name] ?: FrameData(owned = false, colorFeatureActive = false)
        return ownedFrames + (frameType.name to currentData.copy(owned = true))
    }

    fun withFrameColorFeature(ownedFrames: Map<String, FrameData>, frameType: FrameType): Map<String, FrameData> {
        val currentData = ownedFrames[frameType.name] ?: FrameData(owned = false, colorFeatureActive = false)
        return ownedFrames + (frameType.name to currentData.copy(colorFeatureActive = true))
    }
}

/**
 * Android: AbacusSoundPlayer'ın saf kısmı (ses seçimi, perde, seviye). SoundPool yönetimi
 * platform katmanındadır.
 */
object AbacusSoundPlayer {
    const val PREFS_NAME = "AppPrefs"
    const val KEY_SOUND_ENABLED = "sound_enabled"
    const val KEY_ABACUS_SOUND_CHOICE = "abacus_sound_choice"
    const val DEFAULT_SOUND_INDEX = 1
    const val SOUND_COUNT = 5

    /** Boncuk aktif edilirken normal perde. */
    const val ACTIVATE_RATE = 1.0f

    /** Boncuk geri alınırken daha pes ton için düşük perde. */
    const val DEACTIVATE_RATE = 0.75f

    fun playbackRate(activating: Boolean): Float = if (activating) ACTIVATE_RATE else DEACTIVATE_RATE

    /** Ses dosyaları arasındaki seviye farkını dengeleyen çalma seviyesi. */
    fun playbackVolumeFor(index: Int): Float = when (index.coerceIn(1, SOUND_COUNT)) {
        4 -> 1.0f
        else -> 0.6f
    }

    /** Ses kaynak adı (R.raw.abacus_sound1..5). */
    fun soundResFor(index: Int): String = when (index.coerceIn(1, SOUND_COUNT)) {
        1 -> "abacus_sound1"
        2 -> "abacus_sound2"
        3 -> "abacus_sound3"
        4 -> "abacus_sound4"
        else -> "abacus_sound5"
    }

    /** Kayıtlı seçim (varsayılan 1) 1..5 aralığına kırpılır. */
    fun selectedIndex(storedChoice: Int?): Int = (storedChoice ?: DEFAULT_SOUND_INDEX).coerceIn(1, SOUND_COUNT)
}
