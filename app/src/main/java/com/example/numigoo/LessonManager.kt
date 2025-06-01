package com.example.numigoo

import com.example.numigoo.model.LessonItem
import com.example.numigoo.model.LessonViewModel

object LessonManager {
    private var adapter: LessonAdapter? = null
    
    fun setAdapter(adapter: LessonAdapter) {
        this.adapter = adapter
    }

    fun getLessonItem(position: Int, viewModel: LessonViewModel): LessonItem? {
        return viewModel.currentLessons.value?.getOrNull(position)
    }


} 