package com.example.app

import android.content.Context
import android.util.Log
import com.example.app.model.LessonItem

object LessonManager {
    private var adapter: LessonAdapter? = null
    private var raceAdapter: RaceAdapter? = null
    private var initializedPartId: Int? = null
    
    fun setAdapter(adapter: LessonAdapter) {
        this.adapter = adapter
    }
    
    fun setRaceAdapter(raceAdapter: RaceAdapter) {
        this.raceAdapter = raceAdapter
    }
    
    fun getLessonItem(position: Int): LessonItem? {
        return GlobalLessonData.getLessonItem(position)
    }

    fun ensureInitialized(context: Context, partId: Int) {
        if (initializedPartId != partId) {
            GlobalLessonData.initialize(context, partId) {
                refreshLessonsFromGlobalData()
            }
            initializedPartId = partId
        }
    }

    fun updateLessonItem(context: Context, position: Int, item: LessonItem) {
        LessonProgressDiag.log(
            "LessonManager.updateLessonItem",
            "idx=$position finish=${item.stepIsFinish} adapter=${adapter != null}",
        )
        GlobalLessonData.updateLessonItem(context, position, item)
        adapter?.updateLessonItem(position, item)
    }
    fun updateRaceItem(context: Context, position: Int, item: LessonItem) {
        // Global veriyi güncelle
        GlobalLessonData.updateLessonItem(context, position, item)
        // RaceAdapter'ı da güncelle (eğer race item'ı ise)
        raceAdapter?.updateRaceItem(position,item)
        Log.d("mert",item.title)
        Log.d("mert",item.raceBusyLevel.toString())
    }

    fun refreshLessonsFromGlobalData() {
        if (adapter?.isRacePanelOpen() == true) {
            LessonProgressDiag.log("LessonManager.refreshLessons", "SKIP race panel open")
            return
        }
        LessonProgressDiag.logListChestFinishSummary(
            "LessonManager.refreshLessons",
            GlobalLessonData.globalPartId,
            GlobalLessonData.lessonItems,
        )
        adapter?.updateItems(GlobalLessonData.lessonItems)
    }
} 