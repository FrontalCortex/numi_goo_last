// KAYNAK:
//   app/src/main/java/com/example/app/MapFragment.kt
//     - updateStickyHeader()                      → [MapFragmentLogic.stickyHeaderFor]
//     - resolveLessonOperationsId() / resolveLessonOperationsBlindingId() → [MapFragmentLogic.resolveLessonOperationsId]
//     - showGuidePanel() gecikmesi / hedef indeks → [MapFragmentLogic.GUIDE_BOTTOM_SHEET_DELAY_MS], [MarathonGuideLogic]
//   app/src/main/java/com/example/app/MarathonGuideStore.kt
//     - firstMarathonLessonIndex(), eligibilityRejectReason(), tercih anahtarları → [MarathonGuideLogic]
//   app/src/main/java/com/example/app/LessonLeaderboardRepository.kt
//     - firstChestLessonIndexForPart()            → [MarathonGuideLogic.firstChestLessonIndexForPart]
//
// ATLANAN: MapFragment.getLessonOperations() / getLessonOperationsBlinding() (ders adımı → soru listesi tablosu).
// Bu tablo MathOperation / MathOperationGenerator'a bağlıdır (başka paket: `matematik`); bu pakete alınmadı.
//
// Android'den bilinçli farklar: View / Context / Log yok; renk kimliği yerine renk kaynağı adı.
package com.sorobit.shared.ders

object MapFragmentLogic {

    /** Haritanın üstünde sabit duran ünite başlığı. */
    data class StickyHeader(
        val headerTitle: String,
        /** "1. KISIM, 1. ÜNİTE" biçimindeki üst satır. */
        val sectionUnit: String,
        /** Arka plan renk kaynağı adı; başlıkta renk yoksa null (mevcut renk korunur). */
        val color: String?,
    )

    /**
     * [firstVisible] pozisyonundan yukarı doğru ilk TYPE_HEADER aranır; bulunamazsa null (başlık gizlenir).
     *
     * Android davranışı aynen: üst satır `"{stepCount}. KISIM, {currentStep}. ÜNİTE"` olarak üretilir ve katalogdaki
     * tüm başlıklarda stepCount = currentStep = 1 olduğu için her başlıkta "1. KISIM, 1. ÜNİTE" yazar.
     *
     * [firstVisible] liste dışındaysa Android'de `adapter.getItem(i)` global listeye düşer, o da yoksa çöker;
     * burada liste dışı pozisyonlar atlanır (negatif → null).
     */
    fun stickyHeaderFor(items: List<LessonItem>, firstVisible: Int): StickyHeader? {
        for (i in firstVisible downTo 0) {
            val item = items.getOrNull(i) ?: continue
            if (item.type == LessonItem.TYPE_HEADER) {
                return StickyHeader(
                    headerTitle = item.title,
                    sectionUnit = "${item.stepCount}. KISIM, ${item.currentStep}. ÜNİTE",
                    color = item.color,
                )
            }
        }
        return null
    }

    /**
     * [requestedId] için operasyon yoksa [minLessonId]'ye kadar birer azaltarak ilk dolu id'yi döner.
     * [operationsEmpty]: verilen ders adımı id'sinin soru listesi boş mu (Android: getLessonOperations(id).isEmpty()
     * ya da getLessonOperationsBlinding(id).isEmpty()). Alt sınır için bkz. [LessonItem.minLessonOperationsId].
     */
    fun resolveLessonOperationsId(requestedId: Int, minLessonId: Int, operationsEmpty: (Int) -> Boolean): Int {
        var id = requestedId
        while (operationsEmpty(id) && id > minLessonId) {
            id--
        }
        return id
    }

    /** Rehber paneli gösterildikten sonra maraton sandığının alt panelinin açılma gecikmesi (ms). */
    const val GUIDE_BOTTOM_SHEET_DELAY_MS = 600L
}

/** Ünite maratonu rehberinin (ilk sandık bitince bir kez gösterilen panel) uygunluk mantığı. */
object MarathonGuideLogic {
    const val PREFS = "GuidePanelPrefs"
    const val KEY_SHOWN = "marathon_guide_shown"
    const val KEY_PENDING = "marathon_guide_pending"
    const val LEGACY_SHOWN = "cupFragment_guide_shown"
    const val GUIDE_PART_ID = 1

    /** [partId] şablonundaki ilk TYPE_CHEST indeksi; yoksa null. */
    fun firstChestLessonIndexForPart(partId: Int): Int? {
        val idx = GlobalLessonData.createLessonItems(partId).indexOfFirst { it.type == LessonItem.TYPE_CHEST }
        return idx.takeIf { it >= 0 }
    }

    /** Rehberin hedeflediği sandığın indeksi: part 1'in ilk sandığı (yoksa 4). */
    fun firstMarathonLessonIndex(): Int =
        firstChestLessonIndexForPart(GUIDE_PART_ID) ?: 4

    /**
     * Claim ÖNCESİ item için: rehber planlanamıyorsa nedeni, planlanabiliyorsa null.
     * (Uygunsa platform `GuidePanelPrefs/marathon_guide_pending = true` yazar.)
     */
    fun eligibilityRejectReason(partId: Int, lessonIndex: Int, item: LessonItem): String? {
        if (item.type != LessonItem.TYPE_CHEST) return "not_type_chest(type=${item.type})"
        if (item.stepIsFinish) return "step_already_finish_before_claim"
        if (partId != GUIDE_PART_ID) return "wrong_part(part=$partId need=$GUIDE_PART_ID)"
        val expected = firstMarathonLessonIndex()
        if (lessonIndex != expected) {
            return "index_mismatch(mapFragmentStepIndex=$lessonIndex expectedListIdx=$expected " +
                "item.mapFragmentIndex=${item.mapFragmentIndex})"
        }
        return null
    }

    /** Android `scheduleIfEligible` kararı: item null ya da uygun değilse false. */
    fun shouldSchedule(partId: Int, lessonIndex: Int, item: LessonItem?): Boolean {
        if (item == null) return false
        return eligibilityRejectReason(partId, lessonIndex, item) == null
    }
}
