package com.example.numigoo

import android.content.Context
import com.example.numigoo.model.LessonItem

object GlobalLessonData {
    private var _lessonItems: MutableList<LessonItem> = mutableListOf()
    val lessonItems: List<LessonItem> get() = _lessonItems

    fun initialize(items: List<LessonItem>) {
        _lessonItems = items.toMutableList()
    }

    /*fun updateLessonItem(position: Int, newItem: LessonItem) {
        if (position in _lessonItems.indices) {
            _lessonItems[position] = newItem
        }
    }*/

    fun getLessonItem(position: Int): LessonItem? {
        return if (position in _lessonItems.indices) {
            _lessonItems[position]
        } else {
            null
        }
    }

    private fun saveToPreferences() {
        // SharedPreferences'a kaydetme işlemi
    }

    fun loadFromPreferences(context: Context) {
        // SharedPreferences'dan yükleme işlemi
    }
}