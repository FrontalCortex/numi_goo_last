package com.example.app.homework

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.app.CourseClasses
import com.example.app.R
import com.example.app.appString
import com.google.firebase.firestore.ListenerRegistration

/**
 * Görevler → "Ödevlerim" kartı: çizim ve verisi. TasksFragment yalnızca satırı ekliyor ve
 * dokunuşu oynatıcıya bağlıyor; o dosya zaten çok iş yapıyor, ödev mantığı orada büyümesin.
 */
object HomeworkCard {

    data class Item(val homework: Homeworks.Homework, val progress: Homeworks.Progress) {
        val done: Boolean get() = progress.completedBlocks >= homework.blockCount
    }

    /** Bitmemiş ödevler önce (en yeni üstte), bitenler en altta. */
    fun sorted(items: List<Item>): List<Item> =
        items.sortedWith(compareBy<Item> { it.done }.thenByDescending { it.homework.createdAtMs })

    fun bind(root: View, items: List<Item>, onClick: (Item) -> Unit) {
        val container = root.findViewById<LinearLayout>(R.id.homeworkRows)
        container.removeAllViews()
        val inflater = LayoutInflater.from(root.context)
        items.forEach { item ->
            val row = inflater.inflate(R.layout.item_homework_row, container as ViewGroup, false)
            val hw = item.homework
            row.findViewById<TextView>(R.id.hwTitle).text = HomeworkText.title(hw.config)
            row.findViewById<TextView>(R.id.hwDetails).text = HomeworkText.details(hw.config)
            row.findViewById<TextView>(R.id.hwFrom).text = appString(R.string.hw_card_from, hw.teacherName)
            row.findViewById<ProgressBar>(R.id.hwProgressBar).apply {
                max = hw.blockCount
                progress = item.progress.completedBlocks.coerceAtMost(hw.blockCount)
            }
            row.findViewById<TextView>(R.id.hwProgressText).text =
                if (item.done) appString(R.string.hw_card_done)
                else appString(R.string.hw_card_progress, item.progress.completedBlocks, hw.blockCount)
            row.setOnClickListener { onClick(item) }
            container.addView(row)
        }
    }

    /**
     * Öğrencinin ödevlerini ve her birinin ilerlemesini canlı izler. Sınıfı yoksa liste boş.
     * Sınıf değişirse ([refreshClass], Görevler'e her dönüşte) dinleyiciler yeniden kuruluyor.
     */
    class Watcher(private val onChange: (List<Item>) -> Unit) {
        private var classId: String? = null
        private var homeworkListeners: List<ListenerRegistration> = emptyList()
        private val progressListeners = HashMap<String, ListenerRegistration>()
        private var homeworks: List<Homeworks.Homework> = emptyList()
        private val progress = HashMap<String, Homeworks.Progress>()
        private var stopped = false

        fun refreshClass() {
            CourseClasses.loadMyClass { c, error ->
                if (stopped || error) return@loadMyClass
                val id = c?.id
                if (id == classId && (id == null || homeworkListeners.isNotEmpty())) return@loadMyClass
                classId = id
                clearListeners()
                homeworks = emptyList()
                if (id == null) {
                    onChange(emptyList())
                    return@loadMyClass
                }
                homeworkListeners = Homeworks.listenMyHomeworks(id) { list ->
                    if (stopped) return@listenMyHomeworks
                    homeworks = list
                    syncProgressListeners()
                    emit()
                }
            }
        }

        private fun syncProgressListeners() {
            val ids = homeworks.map { it.id }.toSet()
            progressListeners.keys.filter { it !in ids }.forEach { progressListeners.remove(it)?.remove() }
            ids.filter { it !in progressListeners }.forEach { id ->
                Homeworks.listenMyProgress(id) { p ->
                    if (stopped) return@listenMyProgress
                    progress[id] = p
                    emit()
                }?.let { progressListeners[id] = it }
            }
        }

        private fun emit() {
            onChange(sorted(homeworks.map { Item(it, progress[it.id] ?: Homeworks.Progress.EMPTY) }))
        }

        private fun clearListeners() {
            homeworkListeners.forEach { it.remove() }
            homeworkListeners = emptyList()
            progressListeners.values.forEach { it.remove() }
            progressListeners.clear()
            progress.clear()
        }

        fun stop() {
            stopped = true
            clearListeners()
        }
    }
}
