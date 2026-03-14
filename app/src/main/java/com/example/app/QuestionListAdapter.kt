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
    private val onItemClick: (StudentQuestion) -> Unit,
    private val onLongClick: ((StudentQuestion) -> Unit)? = null
) : ListAdapter<StudentQuestion, QuestionListAdapter.ViewHolder>(DiffCallback()) {

    private var unreadCountByQuestionId: Map<String, Int> = emptyMap()

    fun setUnreadCounts(map: Map<String, Int>) {
        unreadCountByQuestionId = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return ViewHolder(view, onItemClick, onLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), unreadCountByQuestionId)
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (StudentQuestion) -> Unit,
        private val onLongClick: ((StudentQuestion) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.questionThumbnail)
        private val preview: TextView = itemView.findViewById(R.id.questionPreview)
        private val timeAgo: TextView = itemView.findViewById(R.id.questionTimeAgo)
        private val statusIcon: ImageView = itemView.findViewById(R.id.questionStatusIcon)
        private val status: TextView = itemView.findViewById(R.id.questionStatus)
        private val unreadBadge: TextView = itemView.findViewById(R.id.questionUnreadBadge)

        fun bind(q: StudentQuestion, unreadCountByQuestionId: Map<String, Int>) {
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
            val hasStatus = status.text.isNotEmpty()
            status.visibility = if (hasStatus) View.VISIBLE else View.GONE
            if (hasStatus) {
                when (q.status) {
                    StudentQuestion.STATUS_RESOLVED -> {
                        statusIcon.visibility = View.VISIBLE
                        statusIcon.setImageResource(R.drawable.solved)
                    }
                    StudentQuestion.STATUS_CLAIMED -> {
                        statusIcon.visibility = View.VISIBLE
                        statusIcon.setImageResource(R.drawable.clock_ic)
                    }
                    else -> statusIcon.visibility = View.GONE
                }
            } else {
                statusIcon.visibility = View.GONE
            }
            val unreadCount = unreadCountByQuestionId[q.id] ?: 0
            if (unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (unreadCount >= 100) "99+" else unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }
            itemView.setOnClickListener { onItemClick(q) }
            if (onLongClick != null) {
                itemView.setOnLongClickListener { onLongClick!!.invoke(q); true }
            } else {
                itemView.setOnLongClickListener(null)
            }
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


