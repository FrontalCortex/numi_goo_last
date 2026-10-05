package com.example.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.ItemFriendResultBinding

data class FriendSearchResult(
    val firebaseUid: String,
    val name: String,
    val userId: String,
    /** publicProfiles.avatarConfig; null → harfli daire. */
    val avatarConfig: String? = null,
    var requestSent: Boolean = false
)

class FriendSearchAdapter(
    private val onAddFriendClick: (FriendSearchResult) -> Unit,
    private val onItemClick: (FriendSearchResult) -> Unit = {}
) : ListAdapter<FriendSearchResult, FriendSearchAdapter.ViewHolder>(DiffCallback) {

    private val avatarColors = listOf(
        "#1CB0F6", "#FF4B4B", "#CE82FF", "#FF9600",
        "#00CD9C", "#2B70C9", "#FF6B97", "#8DC63F"
    )

    companion object DiffCallback : DiffUtil.ItemCallback<FriendSearchResult>() {
        override fun areItemsTheSame(a: FriendSearchResult, b: FriendSearchResult) =
            a.firebaseUid == b.firebaseUid
        override fun areContentsTheSame(a: FriendSearchResult, b: FriendSearchResult) =
            a == b
    }

    inner class ViewHolder(val binding: ItemFriendResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendSearchResult, position: Int) {
            binding.root.setOnClickListener { onItemClick(item) }
            // Avatar: kullanıcının avatarı ya da harfli daire
            val avatar = AvatarView.drawableFor(
                binding.root.context, item.avatarConfig, binding.tvAvatarLetter.layoutParams.width,
            )
            if (avatar != null) {
                binding.tvAvatarLetter.text = ""
                binding.tvAvatarLetter.background = avatar
            } else {
                showLetter(item, position)
            }

            binding.tvUserName.text = item.name
            binding.tvUserCode.text = "@${item.userId}"

            // Button state
            if (item.requestSent) {
                binding.btnAddFriend.setImageResource(android.R.drawable.ic_menu_add)
                binding.btnAddFriend.setBackgroundResource(R.drawable.bg_add_friend_btn_sent)
                binding.btnAddFriend.alpha = 0.5f
                binding.btnAddFriend.isEnabled = false
            } else {
                binding.btnAddFriend.setImageResource(R.drawable.ic_person_add)
                binding.btnAddFriend.setBackgroundResource(R.drawable.bg_add_friend_btn)
                binding.btnAddFriend.alpha = 1f
                binding.btnAddFriend.isEnabled = true
                binding.btnAddFriend.setOnClickListener {
                    item.requestSent = true
                    notifyItemChanged(position)
                    onAddFriendClick(item)
                }
            }
        }

        private fun showLetter(item: FriendSearchResult, position: Int) {
            val letter = item.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            binding.tvAvatarLetter.text = letter
            val color = avatarColors[position % avatarColors.size]
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(color))
            }
            binding.tvAvatarLetter.background = drawable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
}
