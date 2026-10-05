package com.example.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.ItemFollowUserBinding

data class FollowUser(
    val firebaseUid: String,
    val name: String,
    val userId: String,
    /** publicProfiles.avatarConfig; null → harfli daire. */
    val avatarConfig: String? = null,
    /** true → this person is NOT followed back yet → show "+" button */
    val showFollowButton: Boolean = false,
    /**
     * true → bu kişiyi takip ediyorum → "X" (takibi bırak) butonu görünsün.
     *
     * Yalnızca "Takip Edilen" sekmesinde açılır. "Takipçi" sekmesinde de açılsaydı X,
     * "bu takipçiyi listemden çıkar" gibi okunurdu — oysa yaptığı şey benim onu takip
     * etmeyi bırakmam. İki sekme, iki net anlam: X = takibi bırak, + = geri takip et.
     */
    val showUnfollowButton: Boolean = false
)

class FollowUserAdapter(
    private val onFollowClick: (FollowUser) -> Unit,
    private val onItemClick: (FollowUser) -> Unit = {},
    private val onUnfollowClick: (FollowUser) -> Unit = {}
) : ListAdapter<FollowUser, FollowUserAdapter.ViewHolder>(DiffCallback) {

    private val avatarColors = listOf(
        "#1CB0F6", "#CE82FF", "#FF9600", "#FF4B4B",
        "#00CD9C", "#2B70C9", "#FF6B97", "#8DC63F"
    )

    companion object DiffCallback : DiffUtil.ItemCallback<FollowUser>() {
        override fun areItemsTheSame(a: FollowUser, b: FollowUser) = a.firebaseUid == b.firebaseUid
        override fun areContentsTheSame(a: FollowUser, b: FollowUser) = a == b
    }

    inner class ViewHolder(val binding: ItemFollowUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FollowUser, position: Int) {
            binding.root.setOnClickListener { onItemClick(item) }
            val avatar = AvatarView.drawableFor(
                binding.root.context, item.avatarConfig, binding.tvAvatarLetter.layoutParams.width,
            )
            if (avatar != null) {
                binding.tvAvatarLetter.text = ""
                binding.tvAvatarLetter.background = avatar
            } else {
                showLetterAvatar(item, position)
            }

            binding.tvUserName.text = item.name
            binding.tvUserCode.text = "@${item.userId}"

            // Show follow-back button only when applicable
            if (item.showFollowButton) {
                binding.btnFollowBack.visibility = View.VISIBLE
                binding.btnFollowBack.isEnabled = true
                binding.btnFollowBack.alpha = 1f
                binding.btnFollowBack.setOnClickListener {
                    binding.btnFollowBack.isEnabled = false
                    binding.btnFollowBack.alpha = 0.4f
                    onFollowClick(item)
                }
            } else {
                binding.btnFollowBack.visibility = View.GONE
            }

            if (item.showUnfollowButton) {
                binding.btnUnfollow.visibility = View.VISIBLE
                // Buton burada KAPATILMIYOR ("+" butonunun aksine): araya onay diyaloğu
                // giriyor ve kullanıcı iptal ederse buton kapalı kalırdı. Diyalog zaten
                // modal, yani ikinci dokunuş satıra inmiyor.
                binding.btnUnfollow.setOnClickListener { onUnfollowClick(item) }
            } else {
                binding.btnUnfollow.visibility = View.GONE
            }
        }

        private fun showLetterAvatar(item: FollowUser, position: Int) {
            binding.tvAvatarLetter.visibility = View.VISIBLE
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
        val binding = ItemFollowUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
}
