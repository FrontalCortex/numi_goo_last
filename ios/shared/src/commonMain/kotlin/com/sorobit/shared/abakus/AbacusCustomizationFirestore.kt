// Kaynak (Android): app/src/main/java/com/example/app/AbacusCustomizationFirestore.kt
//
// `users/{uid}/abacusCustomization/state` dokümanının modeli, alan adları ve Map ↔ model
// eşlemesi; ayrıca yerel tercihler ([AbacusPreferences]) ↔ anlık görüntü dönüşümü.
// Firestore çağrıları (get/set) platform katmanında kalır.
//
// Android'den farklar:
//  - `DocumentSnapshot` yerine `exists` + alan Map'i alınır.
//  - `doc.getString(alan)` alan String değilse Android'de istisna atar; burada `as? String`
//    ile null sayılır (bozuk veri → varsayılan).
//  - `updatedAt` alanı (`Timestamp.now()`, cihaz saati) yazma Map'ine platform katmanı ekler.
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.BeadType
import com.sorobit.shared.abakus.AbacusPreferences.FrameType

/**
 * Bir kullanıcının abaküs özelleştirmesinin (boncuk tipi/yuvaları/renkleri, çerçeve tipi/renkleri)
 * tam anlık görüntüsü. [AbacusPreferences] (cihaz-yerel) ile Firestore arasındaki tek ortak model.
 */
data class AbacusCustomizationSnapshot(
    val beadType: BeadType = BeadType.SOROBAN,
    val frameType: FrameType = FrameType.FRAME_BG,
    /** Anahtar: "top_{rod}" veya "bot_{rod}_{0..3}". Yerelden kurulduğunda 25 giriş içerir. */
    val slots: Map<String, BeadType> = emptyMap(),
    /** Anahtar: "{BeadType.name}" (normal) veya "{BeadType.name}_sel" (seçili). */
    val beadColors: Map<String, IntArray> = emptyMap(),
    /** Anahtar: [frameType]'a göre "wood"/"heart"/"star"/"c0".."c4". */
    val frameColors: Map<String, Int> = emptyMap(),
    /**
     * Profil kartında gösterilmek üzere seçilen boncuk tipi. `null` = henüz seçim yok.
     * NOT: [AbacusCustomizationFirestore.customizationData] bu alanı YAZMAZ — yalnızca
     * [AbacusCustomizationFirestore.previewBeadTypeData] değiştirir.
     */
    val previewBeadType: BeadType? = null,
) {
    fun beadTypeForSlot(rod: Int, isTop: Boolean, beadIndex: Int = 0): BeadType {
        val key = if (isTop) "top_$rod" else "bot_${rod}_$beadIndex"
        return slots[key] ?: BeadType.SOROBAN
    }

    fun colorsFor(type: BeadType, selected: Boolean): IntArray? {
        val key = if (selected) "${type.name}_sel" else type.name
        return beadColors[key]
    }

    /** [frameType]'a uygun sırada renk dizisi (AbacusFrameRenderer'ın beklediği sıra). */
    fun frameColorArray(): IntArray? = when (frameType) {
        FrameType.FRAME_BG -> frameColors["wood"]?.let { intArrayOf(it) }
        FrameType.FRAME_BG2 -> {
            val wood = frameColors["wood"]; val heart = frameColors["heart"]
            if (wood != null && heart != null) intArrayOf(wood, heart) else null
        }
        FrameType.FRAME_BG3 -> {
            val wood = frameColors["wood"]; val star = frameColors["star"]
            if (wood != null && star != null) intArrayOf(wood, star) else null
        }
        FrameType.FRAME_BG4 -> {
            val c = (0..4).map { frameColors["c$it"] }
            if (c.all { it != null }) c.map { it!! }.toIntArray() else null
        }
    }
}

object AbacusCustomizationFirestore {

    // ── Yol ve alan adları ────────────────────────────────────────────────────
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ABACUS_CUSTOMIZATION = "abacusCustomization"
    const val DOC_STATE = "state"

    const val FIELD_BEAD_TYPE = "beadType"
    const val FIELD_FRAME_TYPE = "frameType"
    const val FIELD_SLOTS = "slots"
    const val FIELD_BEAD_COLORS = "beadColors"
    const val FIELD_FRAME_COLORS = "frameColors"
    const val FIELD_PREVIEW_BEAD_TYPE = "previewBeadType"
    const val FIELD_UPDATED_AT = "updatedAt"

    /** `users/{uid}/abacusCustomization/state` */
    fun docPath(uid: String): String = "$COLLECTION_USERS/$uid/$COLLECTION_ABACUS_CUSTOMIZATION/$DOC_STATE"

    // "_v2" — AbacusPreferences hesap başına ayrı dosya kullanmaya başlayınca eski işaretçi geçersiz kılındı.
    const val HYDRATION_PREFS = "abacus_customization_sync_v2"
    const val KEY_HYDRATED_UID = "hydrated_uid"

    /**
     * Android: ensureHydrated'in erken dönüş koşulu. İşaretçideki uid aynıysa ağ isteği yapılmaz.
     * (Yükleme başarısız olursa işaretçi YAZILMAZ; sonraki çağrı yeniden dener.)
     */
    fun isHydrated(markerUid: String?, uid: String): Boolean = markerUid == uid

    private fun frameColorsFromLocalPreferences(store: AbacusPrefsStore, frameType: FrameType): Map<String, Int> =
        when (frameType) {
            FrameType.FRAME_BG -> mapOf("wood" to AbacusPreferences.getFrameBgWoodColor(store))
            FrameType.FRAME_BG2 -> mapOf(
                "wood" to AbacusPreferences.getFrameBg2WoodColor(store),
                "heart" to AbacusPreferences.getFrameBg2HeartColor(store),
            )
            FrameType.FRAME_BG3 -> mapOf(
                "wood" to AbacusPreferences.getFrameBg3WoodColor(store),
                "star" to AbacusPreferences.getFrameBg3StarColor(store),
            )
            FrameType.FRAME_BG4 -> {
                val c = AbacusPreferences.getFrameBg4Colors(store)
                mapOf("c0" to c[0], "c1" to c[1], "c2" to c[2], "c3" to c[3], "c4" to c[4])
            }
        }

    /** Yalnızca cihazdaki oturum açmış kullanıcının [AbacusPreferences] verisinden anlık görüntü kurar. */
    fun buildSnapshotFromLocalPreferences(store: AbacusPrefsStore, uid: String?): AbacusCustomizationSnapshot {
        val beadType = AbacusPreferences.getBeadType(store, uid)
        val frameType = AbacusPreferences.getFrameType(store)

        val slots = mutableMapOf<String, BeadType>()
        for (rod in 0..4) {
            slots["top_$rod"] = AbacusPreferences.getBeadTypeForSlot(store, uid, rod, isTop = true)
            for (i in 0..3) {
                slots["bot_${rod}_$i"] = AbacusPreferences.getBeadTypeForSlot(store, uid, rod, isTop = false, beadIndex = i)
            }
        }

        // Yalnızca kullanılan tiplerin (yuvalar + genel tip) renkleri eklenir.
        val usedTypes = slots.values.toMutableSet().apply { add(beadType) }
        val beadColors = mutableMapOf<String, IntArray>()
        for (type in usedTypes) {
            beadColors[type.name] = AbacusPreferences.getBeadColors(store, type, false)
            beadColors["${type.name}_sel"] = AbacusPreferences.getBeadColors(store, type, true)
        }

        return AbacusCustomizationSnapshot(
            beadType = beadType,
            frameType = frameType,
            slots = slots,
            beadColors = beadColors,
            frameColors = frameColorsFromLocalPreferences(store, frameType),
        )
    }

    /** [buildSnapshotFromLocalPreferences]'ın tersi: anlık görüntüyü yerel [AbacusPreferences]'a yazar. */
    fun applySnapshotToLocalPreferences(store: AbacusPrefsStore, uid: String?, snapshot: AbacusCustomizationSnapshot) {
        AbacusPreferences.setBeadType(store, uid, snapshot.beadType)
        for (rod in 0..4) {
            AbacusPreferences.setBeadTypeForSlot(store, uid, rod, isTop = true, type = snapshot.beadTypeForSlot(rod, true))
            for (i in 0..3) {
                AbacusPreferences.setBeadTypeForSlot(store, uid, rod, isTop = false, beadIndex = i, type = snapshot.beadTypeForSlot(rod, false, i))
            }
        }
        // DİKKAT (Android davranışı): renk dizisi tipin beklediğinden uzunsa önekli tiplerde fazla
        // dizinler de yazılır ("prefix5" gibi, okunmaz); SOROBAN2/SOROBAN6'da dizi sınırı hatası olur.
        for (type in BeadType.entries) {
            snapshot.colorsFor(type, false)?.let { colors -> colors.forEachIndexed { i, c -> AbacusPreferences.setBeadColor(store, type, false, i, c) } }
            snapshot.colorsFor(type, true)?.let { colors -> colors.forEachIndexed { i, c -> AbacusPreferences.setBeadColor(store, type, true, i, c) } }
        }
        AbacusPreferences.setFrameType(store, snapshot.frameType)
        when (snapshot.frameType) {
            FrameType.FRAME_BG -> snapshot.frameColors["wood"]?.let { AbacusPreferences.setFrameBgWoodColor(store, it) }
            FrameType.FRAME_BG2 -> {
                snapshot.frameColors["wood"]?.let { AbacusPreferences.setFrameBg2WoodColor(store, it) }
                snapshot.frameColors["heart"]?.let { AbacusPreferences.setFrameBg2HeartColor(store, it) }
            }
            FrameType.FRAME_BG3 -> {
                snapshot.frameColors["wood"]?.let { AbacusPreferences.setFrameBg3WoodColor(store, it) }
                snapshot.frameColors["star"]?.let { AbacusPreferences.setFrameBg3StarColor(store, it) }
            }
            FrameType.FRAME_BG4 -> {
                for (i in 0..4) snapshot.frameColors["c$i"]?.let { AbacusPreferences.setFrameBg4Color(store, i, it) }
            }
        }
    }

    /**
     * Firestore dokümanından saf model. Doküman yoksa/alanlar eksikse varsayılan
     * (SOROBAN + FRAME_BG) döner. Firestore sayıları Long olarak gelir; `toInt()` ile kırpılır.
     */
    fun snapshotFromDocument(exists: Boolean, data: Map<String, Any?>?): AbacusCustomizationSnapshot {
        if (!exists || data == null) return AbacusCustomizationSnapshot()

        val beadType = (data[FIELD_BEAD_TYPE] as? String)
            ?.let { AbacusPreferences.beadTypeFromName(it) } ?: BeadType.SOROBAN
        val frameType = (data[FIELD_FRAME_TYPE] as? String)
            ?.let { AbacusPreferences.frameTypeFromName(it) } ?: FrameType.FRAME_BG

        val slots = mutableMapOf<String, BeadType>()
        (data[FIELD_SLOTS] as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val typeName = v as? String ?: return@forEach
            slots[key] = AbacusPreferences.beadTypeFromName(typeName) ?: BeadType.SOROBAN
        }

        val beadColors = mutableMapOf<String, IntArray>()
        (data[FIELD_BEAD_COLORS] as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val list = v as? List<*> ?: return@forEach
            beadColors[key] = IntArray(list.size) { i -> (list[i] as? Number)?.toInt() ?: 0 }
        }

        val frameColors = mutableMapOf<String, Int>()
        (data[FIELD_FRAME_COLORS] as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val num = v as? Number ?: return@forEach
            frameColors[key] = num.toInt()
        }

        val previewBeadType = (data[FIELD_PREVIEW_BEAD_TYPE] as? String)
            ?.let { AbacusPreferences.beadTypeFromName(it) }

        return AbacusCustomizationSnapshot(beadType, frameType, slots, beadColors, frameColors, previewBeadType)
    }

    /**
     * saveCustomization'ın yazdığı veri (`SetOptions.merge()` ile). `previewBeadType` kasıtlı olarak
     * YAZILMAZ. Platform katmanı ayrıca `updatedAt` = şimdiki zaman damgasını ekler.
     */
    fun customizationData(snapshot: AbacusCustomizationSnapshot): Map<String, Any> = mapOf(
        FIELD_BEAD_TYPE to snapshot.beadType.name,
        FIELD_FRAME_TYPE to snapshot.frameType.name,
        FIELD_SLOTS to snapshot.slots.mapValues { it.value.name },
        FIELD_BEAD_COLORS to snapshot.beadColors.mapValues { it.value.toList() },
        FIELD_FRAME_COLORS to snapshot.frameColors,
    )

    /** setPreviewBeadType'ın yazdığı veri (merge; platform `updatedAt` ekler). */
    fun previewBeadTypeData(type: BeadType): Map<String, Any> = mapOf(
        FIELD_PREVIEW_BEAD_TYPE to type.name,
    )
}
