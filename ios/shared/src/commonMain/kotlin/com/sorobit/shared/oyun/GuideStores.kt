// Kaynak: MarathonGuideStore.kt (bayraklar + uygunluk kuralı), FirstTutorialShownStore.kt
// (yerel/bulut birleştirme kuralı).
// Farklar:
//  - Android'de `object` + Context idi; burada depo ([OyunKeyValueStore]) alan sınıflar.
//    MarathonGuideStore → "GuidePanelPrefs", FirstTutorialShownStore → "AppPrefs" dosyası.
//  - `LessonLeaderboardRepository.firstChestLessonIndexForPart(1)` → yapıcı parametresi
//    (`ders`/`kupa` paketinden bağlanacak).
//  - Log çağrıları ve yalnızca günlük yazan yardımcılar (logPrefsSnapshot, logLessonSnapshot) kaldırıldı.
//  - FirstTutorialShownStore'un Firestore yazımı ([markShown] / [repairFirestoreIfLocalShown]) geri
//    çağrıya dönüştü; çağıran `users/{uid}.first_tutorial_shown = true` yazar.
package com.sorobit.shared.oyun

/**
 * Part 1 ilk sandık adımı (Ünite Maratonu) tamamlandığında gösterilecek rehber paneli.
 * Bayrak ödül talebi anında kurulur; panel harita üstü temizken açılır.
 */
class MarathonGuideStore(
    /** Android: "GuidePanelPrefs" adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
    /** `LessonLeaderboardRepository.firstChestLessonIndexForPart(1)`; bulunamazsa null. */
    private val firstChestLessonIndexForGuidePart: () -> Int?,
) {
    fun isShown(): Boolean =
        prefs.getBoolean(KEY_SHOWN, false) || prefs.getBoolean(LEGACY_SHOWN, false)

    fun isPending(): Boolean = prefs.getBoolean(KEY_PENDING, false)

    fun clearPending() {
        prefs.putBoolean(KEY_PENDING, false)
    }

    fun markShown() {
        prefs.putBoolean(KEY_SHOWN, true)
        prefs.putBoolean(KEY_PENDING, false)
    }

    /**
     * @return bekleme bayrağı kurulduysa true
     */
    fun scheduleIfEligible(partId: Int, lessonIndex: Int, item: OyunLessonItem?): Boolean {
        if (item == null) {
            return false
        }

        val rejectReason = eligibilityRejectReason(partId, lessonIndex, item)
        if (rejectReason != null) {
            return false
        }
        prefs.putBoolean(KEY_PENDING, true)
        return true
    }

    fun firstMarathonLessonIndex(): Int =
        firstChestLessonIndexForGuidePart() ?: DEFAULT_FIRST_MARATHON_INDEX

    /** Uygun değilse nedenini (Android'deki günlük metniyle aynı), uygunsa null döndürür. */
    fun eligibilityRejectReason(partId: Int, lessonIndex: Int, item: OyunLessonItem): String? {
        if (item.type != OyunLessonItem.TYPE_CHEST) return "not_type_chest(type=${item.type})"
        if (item.stepIsFinish) return "step_already_finish_before_claim"
        if (partId != GUIDE_PART_ID) return "wrong_part(part=$partId need=$GUIDE_PART_ID)"
        val expected = firstMarathonLessonIndex()
        if (lessonIndex != expected) {
            return "index_mismatch(mapFragmentStepIndex=$lessonIndex expectedListIdx=$expected " +
                "item.mapFragmentIndex=${item.mapFragmentIndex})"
        }
        return null
    }

    companion object {
        const val LOG_TAG = "MarathonGuide"

        const val PREFS = "GuidePanelPrefs"
        const val KEY_SHOWN = "marathon_guide_shown"
        const val KEY_PENDING = "marathon_guide_pending"

        /** Eski anahtar — bir kez gösterildiyse tekrar gösterme. */
        const val LEGACY_SHOWN = "cupFragment_guide_shown"

        /** Rehber yalnızca Part 1 ilk maratonu için. */
        const val GUIDE_PART_ID = 1

        /** İlk sandığın dizini bulunamazsa kullanılan değer. */
        const val DEFAULT_FIRST_MARATHON_INDEX = 4
    }
}

/**
 * İlk öğretici tamamlandı bayrağı: yerel + Firestore `users/{uid}.first_tutorial_shown`.
 * Açılışta okurken bulut false olsa bile yerel true asla ezilmez.
 */
class FirstTutorialShownStore(
    /** Android: "AppPrefs" adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
) {
    /** [resolveShown] sonucu. */
    data class Resolution(
        /** Öğretici gösterilmiş sayılır mı. */
        val shown: Boolean,
        /** Yerel true, bulut değil → çağıran [repairFirestoreIfLocalShown] ile bulutu onarmalı. */
        val cloudNeedsRepair: Boolean,
    )

    fun readLocal(): Boolean = prefs.getBoolean(KEY, false)

    /**
     * @param firestoreValue belgeden okunan değer; belge/alan yoksa null.
     * Bulut true ise yerel de true'ya yükseltilir.
     */
    fun resolveShown(firestoreValue: Boolean?): Resolution {
        val local = prefs.getBoolean(KEY, false)
        val cloud = firestoreValue == true
        val resolved = local || cloud
        if (resolved && !local) {
            prefs.putBoolean(KEY, true)
        }
        return Resolution(shown = resolved, cloudNeedsRepair = local && !cloud)
    }

    /**
     * Öğretici tamamlandı: yerel yazılır; oturum açıksa ([uid] boş değilse) [writeCloud] ile
     * Firestore'a da yazılır (misafirde atlanır).
     */
    fun markShown(uid: String?, writeCloud: (uid: String) -> Unit) {
        prefs.putBoolean(KEY, true)
        if (uid.isNullOrBlank()) {
            return
        }
        writeCloud(uid)
    }

    /** Yerel true iken bulutta false kaldıysa birleştir (açılış / ana ekran girişi). */
    fun repairFirestoreIfLocalShown(uid: String?, writeCloud: (uid: String) -> Unit) {
        if (!readLocal()) return
        val u = uid ?: return
        writeCloud(u)
    }

    companion object {
        const val PREFS_NAME = "AppPrefs"

        /** Hem yerel anahtar hem Firestore alan adı. */
        const val KEY = "first_tutorial_shown"
        const val COLLECTION_USERS = "users"
    }
}
