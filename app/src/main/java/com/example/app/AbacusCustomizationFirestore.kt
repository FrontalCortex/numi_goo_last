package com.example.app

import android.content.Context
import com.example.app.abacus.AbacusPreferences
import com.example.app.abacus.AbacusPreferences.BeadType
import com.example.app.abacus.AbacusPreferences.FrameType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Bir kullanıcının abaküs özelleştirmesinin (boncuk tipi/slotları/renkleri, çerçeve tipi/renkleri)
 * tam anlık görüntüsü. [AbacusPreferences] (cihaz-lokal) ile Firestore arasındaki tek ortak model.
 */
data class AbacusCustomizationSnapshot(
    val beadType: BeadType = BeadType.SOROBAN,
    val frameType: FrameType = FrameType.FRAME_BG,
    /** Anahtar: "top_{rod}" veya "bot_{rod}_{0..3}". Her zaman 25 giriş içerir. */
    val slots: Map<String, BeadType> = emptyMap(),
    /** Anahtar: "{BeadType.name}" (normal) veya "{BeadType.name}_sel" (seçili). */
    val beadColors: Map<String, IntArray> = emptyMap(),
    /** Anahtar: equipped [frameType]'a göre "wood"/"heart"/"star"/"c0".."c4". */
    val frameColors: Map<String, Int> = emptyMap(),
    /**
     * Kullanıcının profil kartında (fragment_profile.xml → abacusPreviewCardImage) gösterilmek üzere
     * seçtiği boncuk tipi — [com.example.app.ProfileFragment]'teki ok ikonlu seçiciyle belirlenir.
     * `null` ise henüz seçim yapılmamış demektir (eski davranış: kullanılan tiplerden rastgele biri).
     * NOT: [AbacusCustomizationFirestore.saveCustomization] bu alanı YAZMAZ — sadece
     * [AbacusCustomizationFirestore.setPreviewBeadType] değiştirir. Böylece
     * AbacusCustomizationFragment'teki genel özelleştirme senkronu bu seçimi asla ezmez.
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

/**
 * `users/{uid}/abacusCustomization/state` dokümanı üzerinden abaküs özelleştirmesini
 * senkronize eder. Diğer Firestore sync sınıflarıyla (BadgeProgressFirestore vb.) aynı
 * konvansiyon: düz callback'ler, coroutine yok.
 */
object AbacusCustomizationFirestore {

    private fun doc(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("abacusCustomization").document("state")

    private data class BeadColorAccessor(
        val get: (Context, Boolean) -> IntArray,
        val set: (Context, Boolean, Int, Int) -> Unit,
    )

    /** Boncuk tipi → renk get/set çift eşlemesi. buildSnapshot ve applySnapshot ikisi de bunu kullanır. */
    private val BEAD_COLOR_ACCESSORS: Map<BeadType, BeadColorAccessor> = mapOf(
        BeadType.SOROBAN to BeadColorAccessor(AbacusPreferences::getSorobanColors, AbacusPreferences::setSorobanColor),
        BeadType.ANIMAL to BeadColorAccessor(AbacusPreferences::getAnimalColors, AbacusPreferences::setAnimalColor),
        BeadType.ANIMAL2 to BeadColorAccessor(AbacusPreferences::getAnimal2Colors, AbacusPreferences::setAnimal2Color),
        BeadType.ANIMAL3 to BeadColorAccessor(AbacusPreferences::getAnimal3Colors, AbacusPreferences::setAnimal3Color),
        BeadType.ANIMAL4 to BeadColorAccessor(AbacusPreferences::getAnimal4Colors, AbacusPreferences::setAnimal4Color),
        BeadType.ANIMAL5 to BeadColorAccessor(AbacusPreferences::getAnimal5Colors, AbacusPreferences::setAnimal5Color),
        BeadType.ANIMAL6 to BeadColorAccessor(AbacusPreferences::getAnimal6Colors, AbacusPreferences::setAnimal6Color),
        BeadType.ANIMAL7 to BeadColorAccessor(AbacusPreferences::getAnimal7Colors, AbacusPreferences::setAnimal7Color),
        BeadType.ANIMAL8 to BeadColorAccessor(AbacusPreferences::getAnimal8Colors, AbacusPreferences::setAnimal8Color),
        BeadType.ANIMAL9 to BeadColorAccessor(AbacusPreferences::getAnimal9Colors, AbacusPreferences::setAnimal9Color),
        BeadType.BOWLING to BeadColorAccessor(AbacusPreferences::getBowlingColors, AbacusPreferences::setBowlingColor),
        BeadType.BALL1 to BeadColorAccessor(AbacusPreferences::getBall1Colors, AbacusPreferences::setBall1Color),
        BeadType.BALL3 to BeadColorAccessor(AbacusPreferences::getBall3Colors, AbacusPreferences::setBall3Color),
        BeadType.BALL4 to BeadColorAccessor(AbacusPreferences::getBall4Colors, AbacusPreferences::setBall4Color),
        BeadType.BALL5 to BeadColorAccessor(AbacusPreferences::getBall5Colors, AbacusPreferences::setBall5Color),
        BeadType.BALL6 to BeadColorAccessor(AbacusPreferences::getBall6Colors, AbacusPreferences::setBall6Color),
        BeadType.BALL7 to BeadColorAccessor(AbacusPreferences::getBall7Colors, AbacusPreferences::setBall7Color),
        BeadType.BALL8 to BeadColorAccessor(AbacusPreferences::getBall8Colors, AbacusPreferences::setBall8Color),
        BeadType.BALL9 to BeadColorAccessor(AbacusPreferences::getBall9Colors, AbacusPreferences::setBall9Color),
        BeadType.BALL10 to BeadColorAccessor(AbacusPreferences::getBall10Colors, AbacusPreferences::setBall10Color),
        BeadType.BALL11 to BeadColorAccessor(AbacusPreferences::getBall11Colors, AbacusPreferences::setBall11Color),
        BeadType.BALL12 to BeadColorAccessor(AbacusPreferences::getBall12Colors, AbacusPreferences::setBall12Color),
        BeadType.BALL13 to BeadColorAccessor(AbacusPreferences::getBall13Colors, AbacusPreferences::setBall13Color),
        BeadType.BALL14 to BeadColorAccessor(AbacusPreferences::getBall14Colors, AbacusPreferences::setBall14Color),
        BeadType.SOROBAN2 to BeadColorAccessor(AbacusPreferences::getSoroban2Colors, AbacusPreferences::setSoroban2Color),
        BeadType.SOROBAN6 to BeadColorAccessor(AbacusPreferences::getSoroban6Colors, AbacusPreferences::setSoroban6Color),
    )

    private fun frameColorsFromLocalPreferences(context: Context, frameType: FrameType): Map<String, Int> =
        when (frameType) {
            FrameType.FRAME_BG -> mapOf("wood" to AbacusPreferences.getFrameBgWoodColor(context))
            FrameType.FRAME_BG2 -> mapOf(
                "wood" to AbacusPreferences.getFrameBg2WoodColor(context),
                "heart" to AbacusPreferences.getFrameBg2HeartColor(context),
            )
            FrameType.FRAME_BG3 -> mapOf(
                "wood" to AbacusPreferences.getFrameBg3WoodColor(context),
                "star" to AbacusPreferences.getFrameBg3StarColor(context),
            )
            FrameType.FRAME_BG4 -> {
                val c = AbacusPreferences.getFrameBg4Colors(context)
                mapOf("c0" to c[0], "c1" to c[1], "c2" to c[2], "c3" to c[3], "c4" to c[4])
            }
        }

    /** Sadece cihazdaki oturum açmış kullanıcının [AbacusPreferences] verisinden anlık görüntü kurar. */
    fun buildSnapshotFromLocalPreferences(context: Context): AbacusCustomizationSnapshot {
        val beadType = AbacusPreferences.getBeadType(context)
        val frameType = AbacusPreferences.getFrameType(context)

        val slots = mutableMapOf<String, BeadType>()
        for (rod in 0..4) {
            slots["top_$rod"] = AbacusPreferences.getBeadTypeForSlot(context, rod, isTop = true)
            for (i in 0..3) {
                slots["bot_${rod}_$i"] = AbacusPreferences.getBeadTypeForSlot(context, rod, isTop = false, beadIndex = i)
            }
        }

        val usedTypes = slots.values.toMutableSet().apply { add(beadType) }
        val beadColors = mutableMapOf<String, IntArray>()
        for (type in usedTypes) {
            val accessor = BEAD_COLOR_ACCESSORS[type] ?: continue
            beadColors[type.name] = accessor.get(context, false)
            beadColors["${type.name}_sel"] = accessor.get(context, true)
        }

        return AbacusCustomizationSnapshot(
            beadType = beadType,
            frameType = frameType,
            slots = slots,
            beadColors = beadColors,
            frameColors = frameColorsFromLocalPreferences(context, frameType),
        )
    }

    /** [buildSnapshotFromLocalPreferences]'ın tersi: snapshot'ı cihazın lokal [AbacusPreferences]'ına yazar. */
    fun applySnapshotToLocalPreferences(context: Context, snapshot: AbacusCustomizationSnapshot) {
        AbacusPreferences.setBeadType(context, snapshot.beadType)
        for (rod in 0..4) {
            AbacusPreferences.setBeadTypeForSlot(context, rod, isTop = true, type = snapshot.beadTypeForSlot(rod, true))
            for (i in 0..3) {
                AbacusPreferences.setBeadTypeForSlot(context, rod, isTop = false, beadIndex = i, type = snapshot.beadTypeForSlot(rod, false, i))
            }
        }
        for ((type, accessor) in BEAD_COLOR_ACCESSORS) {
            snapshot.colorsFor(type, false)?.let { colors -> colors.forEachIndexed { i, c -> accessor.set(context, false, i, c) } }
            snapshot.colorsFor(type, true)?.let { colors -> colors.forEachIndexed { i, c -> accessor.set(context, true, i, c) } }
        }
        AbacusPreferences.setFrameType(context, snapshot.frameType)
        when (snapshot.frameType) {
            FrameType.FRAME_BG -> snapshot.frameColors["wood"]?.let { AbacusPreferences.setFrameBgWoodColor(context, it) }
            FrameType.FRAME_BG2 -> {
                snapshot.frameColors["wood"]?.let { AbacusPreferences.setFrameBg2WoodColor(context, it) }
                snapshot.frameColors["heart"]?.let { AbacusPreferences.setFrameBg2HeartColor(context, it) }
            }
            FrameType.FRAME_BG3 -> {
                snapshot.frameColors["wood"]?.let { AbacusPreferences.setFrameBg3WoodColor(context, it) }
                snapshot.frameColors["star"]?.let { AbacusPreferences.setFrameBg3StarColor(context, it) }
            }
            FrameType.FRAME_BG4 -> {
                for (i in 0..4) snapshot.frameColors["c$i"]?.let { AbacusPreferences.setFrameBg4Color(context, i, it) }
            }
        }
    }

    /** Firestore dokümanından pure model. Doküman yoksa/alanlar eksikse varsayılan (SOROBAN + FRAME_BG) döner. */
    fun snapshotFromDocument(doc: DocumentSnapshot): AbacusCustomizationSnapshot {
        if (!doc.exists()) return AbacusCustomizationSnapshot()

        val beadType = doc.getString("beadType")
            ?.let { runCatching { BeadType.valueOf(it) }.getOrNull() } ?: BeadType.SOROBAN
        val frameType = doc.getString("frameType")
            ?.let { runCatching { FrameType.valueOf(it) }.getOrNull() } ?: FrameType.FRAME_BG

        val slots = mutableMapOf<String, BeadType>()
        (doc.get("slots") as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val typeName = v as? String ?: return@forEach
            slots[key] = runCatching { BeadType.valueOf(typeName) }.getOrNull() ?: BeadType.SOROBAN
        }

        val beadColors = mutableMapOf<String, IntArray>()
        (doc.get("beadColors") as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val list = v as? List<*> ?: return@forEach
            beadColors[key] = IntArray(list.size) { i -> (list[i] as? Number)?.toInt() ?: 0 }
        }

        val frameColors = mutableMapOf<String, Int>()
        (doc.get("frameColors") as? Map<*, *>)?.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            val num = v as? Number ?: return@forEach
            frameColors[key] = num.toInt()
        }

        val previewBeadType = doc.getString("previewBeadType")
            ?.let { runCatching { BeadType.valueOf(it) }.getOrNull() }

        return AbacusCustomizationSnapshot(beadType, frameType, slots, beadColors, frameColors, previewBeadType)
    }

    fun loadCustomization(
        uid: String,
        onResult: (AbacusCustomizationSnapshot) -> Unit,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        doc(uid).get()
            .addOnSuccessListener { snap -> onResult(snapshotFromDocument(snap)) }
            .addOnFailureListener { e -> onFailure?.invoke(e) ?: onResult(AbacusCustomizationSnapshot()) }
    }

    /**
     * NOT: `previewBeadType`'ı kasıtlı olarak YAZMAZ — o alan yalnızca [setPreviewBeadType] ile
     * değişir. Aksi halde AbacusCustomizationFragment'teki her renk/tip düzenlemesi (bu fonksiyonu
     * [snapshot.previewBeadType]'ı bilmeden her seferinde `null` bir snapshot'la çağırır) kullanıcının
     * profil kartı için seçtiği boncuğu sessizce silerdi.
     */
    fun saveCustomization(
        uid: String,
        snapshot: AbacusCustomizationSnapshot,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {},
    ) {
        val data = hashMapOf<String, Any>(
            "beadType" to snapshot.beadType.name,
            "frameType" to snapshot.frameType.name,
            "slots" to snapshot.slots.mapValues { it.value.name },
            "beadColors" to snapshot.beadColors.mapValues { it.value.toList() },
            "frameColors" to snapshot.frameColors,
            "updatedAt" to Timestamp.now(),
        )
        doc(uid).set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Profil kartında gösterilecek boncuk seçimini kaydeder — [saveCustomization]'dan bağımsız, hedefli bir alan yazımı. */
    fun setPreviewBeadType(
        uid: String,
        type: BeadType,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {},
    ) {
        val data = mapOf(
            "previewBeadType" to type.name,
            "updatedAt" to Timestamp.now(),
        )
        doc(uid).set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // "_v2" — AbacusPreferences artık hesap başına ayrı bir SharedPreferences dosyası kullanıyor
    // (bkz. AbacusPreferences.prefs). Eski işaretçi adını değiştirerek önceki test/oturumlardan kalan
    // "zaten hydrate edildi" kayıtlarını geçersiz kılıyoruz — aksi halde yeni (ve boş) hesap-bazlı
    // dosya hiç yeniden doldurulmaz, kullanıcı eski (yanlış hesaba ait) yerel veriyi görmeye devam eder.
    private const val HYDRATION_PREFS = "abacus_customization_sync_v2"
    private const val KEY_HYDRATED_UID = "hydrated_uid"

    /**
     * Bu cihazda [uid] için daha önce hydrate edilmemişse Firestore'daki kaydı lokale uygular.
     * İkinci ve sonraki çağrılar (aynı uid) ağ isteği yapmadan hemen döner — her abaküs ekranından
     * güvenle çağrılabilir (bkz. MainActivity, AbacusFragment, AbacusPracticeFragment, AbacusCustomizationFragment).
     */
    fun ensureHydrated(context: Context, uid: String, onDone: () -> Unit = {}) {
        val marker = context.getSharedPreferences(HYDRATION_PREFS, Context.MODE_PRIVATE)
        if (marker.getString(KEY_HYDRATED_UID, null) == uid) {
            onDone()
            return
        }
        loadCustomization(
            uid = uid,
            onResult = { snapshot ->
                applySnapshotToLocalPreferences(context, snapshot)
                marker.edit().putString(KEY_HYDRATED_UID, uid).apply()
                onDone()
            },
            onFailure = { onDone() },
        )
    }
}
