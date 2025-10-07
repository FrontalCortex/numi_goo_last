package com.example.numigoo

import android.content.Context
import com.example.numigoo.model.LessonItem

object LessonManager {
    private var adapter: LessonAdapter? = null
    private var initializedPartId: Int? = null
    
    fun setAdapter(adapter: LessonAdapter) {
        this.adapter = adapter
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
} 