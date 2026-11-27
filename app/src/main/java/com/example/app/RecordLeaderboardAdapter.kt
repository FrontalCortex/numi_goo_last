package com.example.app

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.example.app.databinding.ItemRecordLeaderboardRowBinding
import com.example.app.LessonLeaderboardRepository.LeaderboardEntry

class RecordLeaderboardAdapter : ListAdapter<LeaderboardEntry, RecordLeaderboardAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordLeaderboardRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemRecordLeaderboardRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LeaderboardEntry) {
            val ctx = binding.root.context
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val isCurrentUser = !currentUid.isNullOrBlank() && entry.userId == currentUid
            val cardColor = if (isCurrentUser) R.color.white else R.color.panel_background
            binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, cardColor))

            val nameColor = if (isCurrentUser) ContextCompat.getColor(ctx, R.color.black)
            else ContextCompat.getColor(ctx, R.color.white)
            val timeColor = if (isCurrentUser) ContextCompat.getColor(ctx, R.color.gray)
            else ContextCompat.getColor(ctx, R.color.button_disabled)

            binding.rowName.setTextColor(nameColor)
            binding.rowTime.setTextColor(timeColor)
            binding.rowName.text = entry.displayName.ifBlank { ctx.getString(R.string.record_leaderboard_title) }
            binding.rowTime.text = entry.recordLabel
            binding.rowRankBadge.text = entry.displayRank.toString()
            binding.rowRankBadge.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            binding.rowRankBadge.background = ContextCompat.getDrawable(ctx, badgeDrawableForRank(entry.rewardRank))

            binding.rowAvatar.setImageBitmap(leaderboardAvatarBitmap(ctx, entry.avatarConfig, binding.rowAvatar.layoutParams.width))
        }

        private fun badgeDrawableForRank(rank: Int): Int = when (rank) {
            1 -> R.drawable.bg_record_rank_gold
            2 -> R.drawable.bg_record_rank_badge_purple
            3 -> R.drawable.bg_record_rank_badge_yellow
            4 -> R.drawable.bg_record_rank_badge_blue
            else -> R.drawable.bg_record_rank_badge_mint
        }
    }

    companion object {
        /** Avatarı olmayan (ya da henüz okunmamış) satırlar için de varsayılan avatar çizilir. */
        fun leaderboardAvatarBitmap(context: Context, avatarConfig: String?, sizePx: Int): Bitmap {
            val config = AvatarConfig.decode(avatarConfig) ?: AvatarConfig.default(AvatarStyle.PERSONAS)
            return AvatarView.toBitmap(context, config, sizePx.coerceAtLeast(1))
        }

        private val DIFF = object : DiffUtil.ItemCallback<LeaderboardEntry>() {
            override fun areItemsTheSame(
                oldItem: LeaderboardEntry,
                newItem: LeaderboardEntry,
            ): Boolean = oldItem.userId == newItem.userId

            override fun areContentsTheSame(
                oldItem: LeaderboardEntry,
                newItem: LeaderboardEntry,
            ): Boolean = oldItem == newItem
        }
    }
}
