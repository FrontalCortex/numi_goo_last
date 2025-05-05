package com.example.numigoo

import com.example.numigoo.model.LessonItem

object LessonManager {
    private var adapter: LessonAdapter? = null
    
    fun setAdapter(adapter: LessonAdapter) {
        this.adapter = adapter
    }
    
    fun getLessonItem(position: Int): LessonItem? {
        return GlobalLessonData.getLessonItem(position)
    }

    fun updateLessonItem(position: Int, item: LessonItem) {
        // Global veriyi güncelle
        GlobalLessonData.updateLessonItem(position, item)
        // Adapter'ı güncelle
        adapter?.updateLessonItem(position, item)
    }
} 