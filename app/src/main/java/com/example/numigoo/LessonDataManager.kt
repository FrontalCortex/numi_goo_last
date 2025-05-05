package com.example.numigoo

import android.content.Context
import com.google.gson.Gson
import com.example.numigoo.model.LessonItem

object LessonDataManager {
    private const val PREFS_NAME = "lesson_prefs"
    private const val KEY_LESSON_ITEMS = "lesson_items"
    private val gson = Gson()

    fun saveLessonItems(context: Context, items: List<LessonItem>) {
        val json = gson.toJson(items)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LESSON_ITEMS, json)
            .apply()
    }

    fun getLessonItems(context: Context): List<LessonItem> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LESSON_ITEMS, null)
        return if (json != null) {
            gson.fromJson(json, Array<LessonItem>::class.java).toList()
        } else {
            emptyList()
        }
    }
}