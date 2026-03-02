package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.model.StudentQuestion
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

class QuestionListAdapter(
    private val onItemClick: (StudentQuestion) -> Unit
) : ListAdapter<StudentQuestion, QuestionListAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (StudentQuestion) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.questionThumbnail)
        private val preview: TextView = itemView.findViewById(R.id.questionPreview)
        private val timeAgo: TextView = itemView.findViewById(R.id.questionTimeAgo)
        private val status: TextView = itemView.findViewById(R.id.questionStatus)

        fun bind(q: StudentQuestion) {
            if (q.screenshotUrl.isNotEmpty()) {
                Glide.with(itemView).load(q.screenshotUrl).centerCrop().into(thumbnail)
            }
            preview.text = q.previewText.ifEmpty { q.message.take(80) }
            timeAgo.text = formatTimeAgo(q.createdAt)
            status.text = when (q.status) {
                StudentQuestion.STATUS_PENDING -> "Cevaplanmadı"
                StudentQuestion.STATUS_CLAIMED -> "Cevaplanıyor"
                StudentQuestion.STATUS_RESOLVED -> "Çözüldü"
                else -> ""
            }
            status.visibility = if (status.text.isEmpty()) View.GONE else View.VISIBLE
            itemView.setOnClickListener { onItemClick(q) }
        }

        private fun formatTimeAgo(ts: Timestamp?): String {
            if (ts == null) return ""
            val now = System.currentTimeMillis()
            val then = ts.toDate().time
            val diff = now - then
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                minutes < 1 -> "Az önce"
                minutes < 60 -> "${minutes} dakika önce"
                hours < 24 -> "${hours} saat önce"
                else -> "${days} gün önce"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StudentQuestion>() {
        override fun areItemsTheSame(a: StudentQuestion, b: StudentQuestion) = a.id == b.id
        override fun areContentsTheSame(a: StudentQuestion, b: StudentQuestion) = a == b
    }
}


