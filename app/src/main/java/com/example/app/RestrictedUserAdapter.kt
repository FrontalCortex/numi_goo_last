package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.model.RestrictedUser
import java.util.concurrent.TimeUnit

class RestrictedUserAdapter(
    private val onItemClick: (RestrictedUser) -> Unit
) : ListAdapter<RestrictedUser, RestrictedUserAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restricted_user, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (RestrictedUser) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.restrictedUserName)
        private val status: TextView = itemView.findViewById(R.id.restrictedUserStatus)

        fun bind(user: RestrictedUser) {
            name.text = user.name
            if (user.banned) {
                status.text = "Kalıcı ban"
                status.setBackgroundResource(R.drawable.bg_status_chip_error)
                status.setTextColor(itemView.context.getColor(R.color.dark_error))
            } else {
                status.text = formatRemaining(user.restrictedUntil)
                status.setBackgroundResource(R.drawable.bg_status_chip_warning)
                status.setTextColor(itemView.context.getColor(R.color.dark_warning))
            }
            itemView.setOnClickListener { onItemClick(user) }
        }

        private fun formatRemaining(until: com.google.firebase.Timestamp?): String {
            if (until == null) return "Kısıtlı"
            val diff = until.toDate().time - System.currentTimeMillis()
            if (diff <= 0) return "Süresi doldu"
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            return when {
                days >= 1 -> "$days gün kaldı"
                hours >= 1 -> "$hours saat kaldı"
                else -> "1 saatten az kaldı"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RestrictedUser>() {
        override fun areItemsTheSame(a: RestrictedUser, b: RestrictedUser) = a.uid == b.uid
        override fun areContentsTheSame(a: RestrictedUser, b: RestrictedUser) = a == b
    }
}
