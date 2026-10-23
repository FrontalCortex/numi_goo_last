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

/**
 * Sorularım → Sınıfım: sınıf arkadaşları. Takipçi listesinin satırı (item_follow_user) aynen;
 * düğme yok, satıra dokununca profil. Öğrencinin kendisi "(sen)" diye işaretli.
 */
class ClassmateAdapter(
    private val myUid: String?,
    private val onClick: (CourseClasses.ClassMember) -> Unit,
) : ListAdapter<Pair<CourseClasses.ClassMember, String?>, ClassmateAdapter.Holder>(Diff) {

    /** Listenin başındaki öğretmenler: kullanıcı adı yerine "Öğretmen" yazıyor. */
    var teacherUids: Set<String> = emptySet()

    private val avatarColors = listOf("#1CB0F6", "#CE82FF", "#FF9600", "#FF4B4B", "#00CD9C", "#2B70C9", "#FF6B97", "#8DC63F")

    object Diff : DiffUtil.ItemCallback<Pair<CourseClasses.ClassMember, String?>>() {
        override fun areItemsTheSame(a: Pair<CourseClasses.ClassMember, String?>, b: Pair<CourseClasses.ClassMember, String?>) =
            a.first.uid == b.first.uid
        override fun areContentsTheSame(a: Pair<CourseClasses.ClassMember, String?>, b: Pair<CourseClasses.ClassMember, String?>) = a == b
    }

    class Holder(val binding: ItemFollowUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemFollowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (member, avatarConfig) = getItem(position)
        val b = holder.binding
        val name = member.name.ifBlank { member.userId }
        b.tvUserName.text = if (member.uid == myUid) appString(R.string.hw_classmate_me, name) else name
        b.tvUserCode.text = when {
            member.uid in teacherUids -> appString(R.string.hw_classmate_teacher)
            member.userId.isNotBlank() -> "@${member.userId}"
            else -> ""
        }
        val avatar = AvatarView.drawableFor(b.root.context, avatarConfig, b.tvAvatarLetter.layoutParams.width)
        if (avatar != null) {
            b.tvAvatarLetter.text = ""
            b.tvAvatarLetter.background = avatar
        } else {
            b.tvAvatarLetter.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            b.tvAvatarLetter.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(avatarColors[position % avatarColors.size]))
            }
        }
        b.btnFollowBack.visibility = View.GONE
        b.btnUnfollow.visibility = View.GONE
        b.root.setOnClickListener { onClick(member) }
    }
}
