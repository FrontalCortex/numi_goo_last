package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.model.MessageReport
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

data class ReportRow(
    val report: MessageReport,
    val reportedUserName: String
)

class ReportAdapter(
    private val onItemClick: (ReportRow) -> Unit
) : ListAdapter<ReportRow, ReportAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (ReportRow) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val reportedUserName: TextView = itemView.findViewById(R.id.reportReportedUserName)
        private val messagePreview: TextView = itemView.findViewById(R.id.reportMessagePreview)
        private val reason: TextView = itemView.findViewById(R.id.reportReason)
        private val timeAgo: TextView = itemView.findViewById(R.id.reportTimeAgo)

        fun bind(row: ReportRow) {
            reportedUserName.text = row.reportedUserName
            messagePreview.text = row.report.messagePreview.ifEmpty { "-" }
            reason.text = MessageReport.reasonLabel(row.report.reason)
            timeAgo.text = formatTimeAgo(row.report.reportedAt)
            itemView.setOnClickListener { onItemClick(row) }
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

    private class DiffCallback : DiffUtil.ItemCallback<ReportRow>() {
        override fun areItemsTheSame(a: ReportRow, b: ReportRow) = a.report.id == b.report.id
        override fun areContentsTheSame(a: ReportRow, b: ReportRow) = a == b
    }
}
