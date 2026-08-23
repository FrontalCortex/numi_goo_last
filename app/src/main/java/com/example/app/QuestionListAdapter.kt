package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.app.model.StudentQuestion
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

class QuestionListAdapter(
    private val onItemClick: (StudentQuestion) -> Unit,
    private val onLongClick: ((StudentQuestion) -> Unit)? = null
) : ListAdapter<StudentQuestion, QuestionListAdapter.ViewHolder>(DiffCallback()) {

    private var unreadCountByQuestionId: Map<String, Int> = emptyMap()
    private var teacherSelectionMode = false
    private var teacherSelectedQuestionId: String? = null

    fun setUnreadCounts(map: Map<String, Int>) {
        unreadCountByQuestionId = map
        notifyDataSetChanged()
    }

    fun setTeacherSelectionMode(selectionMode: Boolean, selectedQuestionId: String?) {
        teacherSelectionMode = selectionMode
        teacherSelectedQuestionId = selectedQuestionId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return ViewHolder(view, onItemClick, onLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            getItem(position),
            unreadCountByQuestionId,
            teacherSelectionMode,
            teacherSelectedQuestionId
        )
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (StudentQuestion) -> Unit,
        private val onLongClick: ((StudentQuestion) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailFrame: FrameLayout = itemView.findViewById(R.id.questionThumbnailFrame)
        private val thumbnail: ImageView = itemView.findViewById(R.id.questionThumbnail)
        private val preview: TextView = itemView.findViewById(R.id.questionPreview)
        private val timeAgo: TextView = itemView.findViewById(R.id.questionTimeAgo)
        private val statusChip: LinearLayout = itemView.findViewById(R.id.questionStatusChip)
        private val statusIcon: ImageView = itemView.findViewById(R.id.questionStatusIcon)
        private val status: TextView = itemView.findViewById(R.id.questionStatus)
        private val unreadBadge: TextView = itemView.findViewById(R.id.questionUnreadBadge)
        private val selectionRadio: RadioButton = itemView.findViewById(R.id.questionSelectionRadio)

        fun bind(
            q: StudentQuestion,
            unreadCountByQuestionId: Map<String, Int>,
            selectionMode: Boolean = false,
            selectedQuestionId: String? = null
        ) {
            if (q.screenshotUrl.isNotEmpty()) {
                thumbnailFrame.visibility = View.VISIBLE
                Glide.with(itemView).load(q.screenshotUrl)
                    .transform(CenterCrop(), RoundedCorners(24))
                    .into(thumbnail)
            } else {
                thumbnailFrame.visibility = View.GONE
            }
            preview.text = q.previewText.ifEmpty { q.message.take(80) }
            timeAgo.text = formatTimeAgo(q.createdAt)
            val context = itemView.context
            status.text = when (q.status) {
                StudentQuestion.STATUS_PENDING -> "Cevaplanmadı"
                StudentQuestion.STATUS_CLAIMED -> "Cevaplanıyor"
                StudentQuestion.STATUS_RESOLVED -> "Çözüldü"
                else -> ""
            }
            val hasStatus = status.text.isNotEmpty()
            statusChip.visibility = if (hasStatus) View.VISIBLE else View.GONE
            if (hasStatus) {
                val (chipBg, textColor, iconRes) = when (q.status) {
                    StudentQuestion.STATUS_RESOLVED ->
                        Triple(R.drawable.bg_status_chip_success, R.color.dark_success, R.drawable.solved)
                    StudentQuestion.STATUS_CLAIMED ->
                        Triple(R.drawable.bg_status_chip_info, R.color.dark_primary, R.drawable.clock_ic)
                    else ->
                        Triple(R.drawable.bg_status_chip_warning, R.color.dark_warning, null)
                }
                statusChip.setBackgroundResource(chipBg)
                val color = ContextCompat.getColor(context, textColor)
                status.setTextColor(color)
                if (iconRes != null) {
                    statusIcon.visibility = View.VISIBLE
                    statusIcon.setImageResource(iconRes)
                    ImageViewCompat.setImageTintList(statusIcon, android.content.res.ColorStateList.valueOf(color))
                } else {
                    statusIcon.visibility = View.GONE
                }
            }
            val unreadCount = unreadCountByQuestionId[q.id] ?: 0
            if (unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (unreadCount >= 100) "99+" else unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }
            if (selectionMode) {
                selectionRadio.visibility = View.VISIBLE
                selectionRadio.isChecked = (q.id == selectedQuestionId)
            } else {
                selectionRadio.visibility = View.GONE
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


