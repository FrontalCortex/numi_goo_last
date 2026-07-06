package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView

data class PartState(
    val partId: Int,
    val visualActive: Boolean,
    val functionalActive: Boolean,
    val inactiveMessage: String
)

class PartCardAdapter(
    private val parts: List<PartState>,
    private val onCardClick: (partId: Int) -> Unit
) : RecyclerView.Adapter<PartCardAdapter.PartCardViewHolder>() {

    inner class PartCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.partCardView)
        private val label: TextView = itemView.findViewById(R.id.partCardLabel)
        private val lottieView: LottieAnimationView = itemView.findViewById(R.id.partCardLottie)
        private var animatorRunnable: Runnable? = null

        fun bind(state: PartState) {
            animatorRunnable?.let { itemView.removeCallbacks(it) }
            
            val context = itemView.context
            val partId = state.partId
            
            // Set Title
            val title = when (partId) {
                1 -> "Abaküsün Temeli ve Toplama"
                2 -> "Abaküste Çıkarma"
                3 -> "Abaküste Çarpma"
                4 -> "Körleme Toplama"
                5 -> "Körleme Çıkarma"
                6 -> "Körleme Çarpma"
                7 -> "Ustalık Yolu - Toplama"
                8 -> "Ustalık Yolu - Çıkarma"
                else -> "Part : $partId"
            }
            label.text = title
            
            // Set Animation Name
            val animName = when (partId) {
                1 -> "roller_coaster_anim.json"
                2 -> "rocket_anim.json"
                3 -> "sailboat_anim.json"
                4 -> "ferris_wheel_anim.json"
                5 -> "piata_anim.json"
                6 -> "flying_saucer_anim.json"
                7 -> "violin_anim.json"
                8 -> "skateboard_anim.json"
                else -> "dinosaur_anim.json"
            }
            lottieView.setAnimation(animName)
            
            if (state.visualActive) {
                // Active Colors
                val colorRes = when (partId) {
                    2, 5, 8 -> R.color.lesson_header_green
                    3, 6 -> R.color.lesson_header_blue
                    else -> R.color.lesson_header_orange
                }
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
                label.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                
                lottieView.speed = 1f
                lottieView.clearColorFilter()
                lottieView.clearValueCallback(
                    com.airbnb.lottie.model.KeyPath("**"),
                    com.airbnb.lottie.LottieProperty.COLOR_FILTER
                )
                lottieView.addValueCallback(
                    com.airbnb.lottie.model.KeyPath("**"),
                    com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                    com.airbnb.lottie.value.LottieValueCallback<android.graphics.ColorFilter?>(null)
                )
                
                if (partId == 2) {
                    lottieView.repeatCount = 2 // 1 normal + 2 repeat = 3 kez oynatır
                } else {
                    lottieView.repeatCount = 0
                }
                
                val runnable = object : Runnable {
                    override fun run() {
                        lottieView.playAnimation()
                        itemView.postDelayed(this, 10000L)
                    }
                }
                animatorRunnable = runnable
                itemView.post(runnable)
            } else {
                // Inactive Colors
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background_color))
                val lockedColor = ContextCompat.getColor(context, R.color.lesson_locked)
                label.setTextColor(lockedColor)
                
                lottieView.speed = 0f
                lottieView.pauseAnimation()
                lottieView.progress = 0f
                val filter = android.graphics.PorterDuffColorFilter(lockedColor, android.graphics.PorterDuff.Mode.SRC_ATOP)
                lottieView.addValueCallback(
                    com.airbnb.lottie.model.KeyPath("**"),
                    com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                    com.airbnb.lottie.value.LottieValueCallback<android.graphics.ColorFilter>(filter)
                )
            }
            
            cardView.setOnClickListener { 
                if (state.functionalActive) {
                    onCardClick(partId)
                } else {
                    Toast.makeText(context, state.inactiveMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_part_card, parent, false)
        return PartCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartCardViewHolder, position: Int) {
        holder.bind(parts[position])
    }

    override fun getItemCount(): Int = parts.size
}
