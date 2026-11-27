// KAYNAK: app/src/main/java/com/example/app/LessonSuccessRateRepository.kt
//
// Güncel Android (soban-gamified-education dalı) mimarisi: küresel sayaçlar (`successRate/lessonSuccessRate/...`) KALDIRILDI
// ve geçme/geçememe istatistikleri Firebase Analytics olaylarına taşındı. Firestore'da KALAN tek şey çift sayım koruması:
// `users/{uid}/lessonSuccessRateState/{partId}_{stableId}_{step}` (`attempted`, `passed`, `failStreak`). Eski anahtar
// biçimi `{partId}_{position}_{step}` okunurken hâlâ dikkate alınır ve ilk dokunuşta yeni anahtara taşınır (eskisi silinir).
//
// Bu dosya Android'deki transaction gövdelerinin saf (hesap) kısmıdır: "okunan belge alanları → yazılacak alanlar ve
// Analytics'e gönderilecek failStreak". Platform katmanı transaction'ı açar, hedef ve eski belgeyi okuyup düz Map
// verir (belge yoksa null), dönen alanları `SetOptions.merge()` ile yazar, eski belge varsa siler ve transaction
// BAŞARIYLA bitince (lambda içinde değil) Analytics olayını gönderir.
//
// Android'den bilinçli farklar:
//  - FirebaseAuth / FirebaseFirestore / AnalyticsLogger / Log çağrıları yok. Ön koşullar platformda korunmalı:
//    `partId in 1..8` ([isTrackedPart]) ve giriş yapmış kullanıcı (uid != null); aksi halde hiçbir şey yapılmaz.
//  - Android `getBoolean` / `getLong` alan türü uymazsa istisna atar; burada tür uymayan alan "yok" sayılır.
//  - `recordItemFirstFinish`'in `LessonMilestones.recordFinish` (haftalık kart için bitiş tarihi/örnek işlem) çağrısı ve
//    `AnalyticsLogger.logLesson*` olayları platform işidir; burada yalnızca olay parametreleri hesaplanır.
package com.sorobit.shared.ders

object LessonSuccessRateRepository {

    const val FIELD_ATTEMPTED = "attempted"
    const val FIELD_PASSED = "passed"
    const val FIELD_FAIL_STREAK = "failStreak"

    /** `users/{uid}/lessonSuccessRateState` koleksiyon yolu. */
    fun stateCollectionPath(uid: String): String = "users/$uid/lessonSuccessRateState"

    /** İstatistik tutulan part'lar (Android: `if (partId !in 1..8) return`). */
    fun isTrackedPart(partId: Int): Boolean = partId in 1..8

    /** Yeni biçim doküman yolu: `{partId}_{stableId}_{step}` ([step] en az 1'e yuvarlanır). */
    fun userStepStatePath(uid: String, partId: Int, stableId: String, step: Int): String =
        "${stateCollectionPath(uid)}/${partId}_${stableId}_${step.coerceAtLeast(1)}"

    /**
     * ESKİ biçim doküman yolu: `{partId}_{position}_{step}`. Müfredata araya ders eklenince yanlış derse bağlanabildiği
     * için yalnızca geçmişi kaybetmemek amacıyla okunur; hedef anahtar yokken kullanılır ve yazımda silinir.
     */
    fun legacyUserStepStatePath(uid: String, partId: Int, position: Int, step: Int): String =
        "${stateCollectionPath(uid)}/${partId}_${position}_${step.coerceAtLeast(1)}"

    /**
     * [position]'daki dersin kalıcı kimliği (Android: stableIdOf). Şablon dışı konumda null → çağıran eski (sıra tabanlı)
     * anahtarda kalır ("veri kaybetmektense eski biçimde tutmak yeğdir").
     */
    fun stableIdOf(partId: Int, position: Int): String? = GlobalLessonData.stableIdAt(partId, position)

    /** Platform bir adım için hangi belgelere bakacak/yazacak. */
    data class StateRefs(
        /** Yazılacak belgenin yolu (stableId biliniyorsa yeni biçim, değilse eski). */
        val targetPath: String,
        /** Okunup (varsa) silinecek eski belgenin yolu; stableId bilinmiyorsa null. */
        val legacyPath: String?,
        /** Analytics `lessonId` parametresi (stableId; bilinmiyorsa null). */
        val stableId: String?,
    )

    fun stateRefs(uid: String, partId: Int, position: Int, step: Int): StateRefs {
        val stableId = stableIdOf(partId, position)
        return if (stableId != null) {
            StateRefs(
                targetPath = userStepStatePath(uid, partId, stableId, step),
                legacyPath = legacyUserStepStatePath(uid, partId, position, step),
                stableId = stableId,
            )
        } else {
            StateRefs(
                targetPath = legacyUserStepStatePath(uid, partId, position, step),
                legacyPath = null,
                stableId = null,
            )
        }
    }

    /** Transaction çıktısı. */
    data class StepWrite(
        /** [StateRefs.targetPath]'e `merge` ile yazılacak alanlar. */
        val userState: Map<String, Any>,
        /** Analytics olayına giden başarısızlık serisi (fail: yeni değer; pass: geçişe kadarki başarısızlık sayısı, 0 = ilk denemede). */
        val failStreak: Long,
        /** Eski anahtarlı belgeden okunduysa (hedef yoktu, eski vardı) eski belge silinmeli. */
        val deleteLegacy: Boolean,
    )

    private fun Map<String, Any?>.bool(key: String): Boolean = (this[key] as? Boolean) == true
    private fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()

    /**
     * Android: `userSnap = if (targetSnap.exists()) targetSnap else legacySnap ?: targetSnap`. Belge yoksa null verilir;
     * hiçbiri yoksa boş harita. Dönüş: (kullanılacak alanlar, eski belgeden mi okundu).
     */
    fun pickUserSnap(target: Map<String, Any?>?, legacy: Map<String, Any?>?): Pair<Map<String, Any?>, Boolean> =
        when {
            target != null -> target to false
            legacy != null -> legacy to true
            else -> emptyMap<String, Any?>() to false
        }

    /**
     * Kullanıcı bu adımı bitiremeden dersten çıktığında (quiz veya chest skoru yetersiz).
     * @return yazılacak alanlar; kullanıcı bu adımı daha önce geçtiyse null (hiçbir şey yazılmaz, olay yok).
     */
    fun recordFail(target: Map<String, Any?>?, legacy: Map<String, Any?>? = null): StepWrite? {
        val (userSnap, fromLegacy) = pickUserSnap(target, legacy)
        if (userSnap.bool(FIELD_PASSED)) return null
        val newFailStreak = (userSnap.long(FIELD_FAIL_STREAK) ?: 0L) + 1
        return StepWrite(
            userState = mapOf(FIELD_ATTEMPTED to true, FIELD_FAIL_STREAK to newFailStreak),
            failStreak = newFailStreak,
            deleteLegacy = fromLegacy,
        )
    }

    /**
     * Kullanıcı bu adımı ilk kez başarıyla geçtiğinde (adım ilerlemesi veya item bitişi).
     * @return yazılacak alanlar; kullanıcı bu adımı daha önce geçtiyse null.
     */
    fun recordPass(target: Map<String, Any?>?, legacy: Map<String, Any?>? = null): StepWrite? {
        val (userSnap, fromLegacy) = pickUserSnap(target, legacy)
        if (userSnap.bool(FIELD_PASSED)) return null
        val failStreak = userSnap.long(FIELD_FAIL_STREAK) ?: 0L
        return StepWrite(
            userState = mapOf(FIELD_ATTEMPTED to true, FIELD_PASSED to true),
            failStreak = failStreak,
            deleteLegacy = fromLegacy,
        )
    }

    /**
     * `logLessonStepFail`'e giden `answerSuccessRatePercent`: yalnızca part 1–6 için anlamlı (7–8'de çağıran taraf
     * anlamlı bir oran üretmiyor), aksi halde null.
     */
    fun answerSuccessRateForAnalytics(partId: Int, answerSuccessRatePercent: Float?): Float? =
        answerSuccessRatePercent.takeIf { partId in 1..6 }

    /**
     * recordQuestionEntry / recordAbandonWithoutAnswer / recordItemFirstFinish / recordItemReplay yalnızca Analytics olayı
     * gönderir (Firestore'a yazılmaz); koşul: izlenen part ve giriş yapmış kullanıcı. Olay `lessonId` = [stableIdOf].
     */
    fun shouldLogEvent(partId: Int, isLoggedIn: Boolean): Boolean = isTrackedPart(partId) && isLoggedIn
}
