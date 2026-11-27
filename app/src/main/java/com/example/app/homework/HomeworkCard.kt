package com.example.app.homework

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.CourseClasses
import com.example.app.R
import com.example.app.appString
import com.google.firebase.firestore.ListenerRegistration

/**
 * Öğrencinin ödev listesi (Sorularım → Ödevlerim): satır çizimi, liste adaptörü ve verisi.
 * NotificationFragment yalnızca sekmeyi kuruyor; ödev mantığı orada büyümesin.
 */
object HomeworkCard {

    data class Item(val homework: Homeworks.Homework, val progress: Homeworks.Progress) {
        val done: Boolean get() = progress.completedBlocks >= homework.blockCount
    }

    /** Bitmemiş ödevler önce (en yeni üstte), bitenler en altta. */
    fun sorted(items: List<Item>): List<Item> =
        items.sortedWith(compareBy<Item> { it.done }.thenByDescending { it.homework.createdAtMs })

    /** item_homework_row'u doldurur. */
    fun bindRow(row: View, item: Item) {
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
    }

    class Adapter(private val onClick: (Item) -> Unit) : ListAdapter<Item, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(a: Item, b: Item) = a.homework.id == b.homework.id
            override fun areContentsTheSame(a: Item, b: Item) = a == b
        },
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            object : RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_student_homework, parent, false),
            ) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            bindRow(holder.itemView.findViewById(R.id.row), item)
            holder.itemView.setOnClickListener { onClick(item) }
        }
    }

    /**
     * Öğrencinin ödevlerini ve her birinin ilerlemesini canlı izler. [classId] öğrencinin sınıfı
     * ([CourseClasses.loadMyClass]); sınıf değişirse çağıran yeni bir Watcher kuruyor.
     */
    class Watcher(classId: String, private val onChange: (List<Item>) -> Unit) {
        private val homeworkListeners: List<ListenerRegistration>
        private val progressListeners = HashMap<String, ListenerRegistration>()
        private var homeworks: List<Homeworks.Homework> = emptyList()
        private val progress = HashMap<String, Homeworks.Progress>()
        private var stopped = false

        init {
            homeworkListeners = Homeworks.listenMyHomeworks(classId) { list ->
                if (stopped) return@listenMyHomeworks
                homeworks = list
                syncProgressListeners()
                emit()
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

        fun stop() {
            stopped = true
            homeworkListeners.forEach { it.remove() }
            progressListeners.values.forEach { it.remove() }
            progressListeners.clear()
        }
    }
}
