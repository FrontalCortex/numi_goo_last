package com.example.numigoo

import android.content.Context
import android.util.Log
import com.example.numigoo.model.LessonItem

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
            GlobalLessonData.initialize(context, partId)
            initializedPartId = partId
        }
    }

    fun updateLessonItem(context: Context, position: Int, item: LessonItem) {
        // Global veriyi güncelle
        GlobalLessonData.updateLessonItem(context, position, item)
        // Adapter'ı güncelle
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
} 