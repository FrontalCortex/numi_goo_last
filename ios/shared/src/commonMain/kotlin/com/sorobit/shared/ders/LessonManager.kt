// KAYNAK: app/src/main/java/com/example/app/LessonManager.kt (tamamı)
//
// Android'den bilinçli farklar:
//  - `LessonAdapter` / `RaceAdapter` (RecyclerView) yerine aynı metot adlarını taşıyan köprü arayüzleri
//    ([LessonAdapterBridge], [RaceAdapterBridge]); iOS görünüm katmanı bunları uygular.
//  - Context parametreleri kaldırıldı; `SeasonClock.currentSeason()` yerine `season` parametresi.
//  - `ensureInitialized`: GlobalLessonData.initialize Firebase'e bağlı olduğundan başlatıcı dışarıdan verilir.
//  - Log / LessonProgressDiag çağrıları kaldırıldı.
package com.sorobit.shared.ders

object LessonManager {

    /** Harita listesinin (Android: LessonAdapter) LessonManager tarafından çağrılan yüzeyi. */
    interface LessonAdapterBridge {
        fun updateLessonItem(position: Int, newItem: LessonItem)
        fun updateItems(newItems: List<LessonItem>)
        fun isRacePanelOpen(): Boolean
    }

    /** Yarış paneli listesinin (Android: RaceAdapter) LessonManager tarafından çağrılan yüzeyi. */
    interface RaceAdapterBridge {
        fun updateRaceItem(position: Int, item: LessonItem)
    }

    private var adapter: LessonAdapterBridge? = null
    private var raceAdapter: RaceAdapterBridge? = null
    private var initializedPartId: Int? = null

    fun setAdapter(adapter: LessonAdapterBridge?) {
        this.adapter = adapter
    }

    fun setRaceAdapter(raceAdapter: RaceAdapterBridge?) {
        this.raceAdapter = raceAdapter
    }

    fun getLessonItem(position: Int): LessonItem? {
        return GlobalLessonData.getLessonItem(position)
    }

    /**
     * [initialize]: platformun `GlobalLessonData.initialize(partId, onReady)` akışı; onReady çağrıldığında liste
     * yenilenir. Aynı part için ikinci çağrı hiçbir şey yapmaz.
     */
    fun ensureInitialized(partId: Int, initialize: (partId: Int, onReady: () -> Unit) -> Unit) {
        if (initializedPartId != partId) {
            initialize(partId) {
                refreshLessonsFromGlobalData()
            }
            initializedPartId = partId
        }
    }

    fun updateLessonItem(position: Int, item: LessonItem, season: Int) {
        if (item.partId != null && item.partId != GlobalLessonData.globalPartId) {
            return
        }
        GlobalLessonData.updateLessonItem(position, item, season)
        adapter?.updateLessonItem(position, item)
    }

    fun updateRaceItem(position: Int, item: LessonItem, season: Int) {
        if (item.partId != null && item.partId != GlobalLessonData.globalPartId) {
            return
        }
        // Global veriyi güncelle
        GlobalLessonData.updateLessonItem(position, item, season)
        // RaceAdapter'ı da güncelle (eğer race item'ı ise)
        raceAdapter?.updateRaceItem(position, item)
    }

    fun refreshLessonsFromGlobalData() {
        if (adapter?.isRacePanelOpen() == true) {
            return
        }
        adapter?.updateItems(GlobalLessonData.lessonItems)
    }

    /** Yalnızca testler için. */
    internal fun resetForTest() {
        adapter = null
        raceAdapter = null
        initializedPartId = null
    }
}
