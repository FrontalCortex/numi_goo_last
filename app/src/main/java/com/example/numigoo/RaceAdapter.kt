package com.example.numigoo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.numigoo.model.LessonItem

class RaceAdapter(
    private val context: Context,
    private val raceItems: List<LessonItem>,
    private val onRaceItemClick: (LessonItem, Int) -> Unit
) : RecyclerView.Adapter<RaceAdapter.RaceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_race_list, parent, false)
        return RaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: RaceViewHolder, position: Int) {
        holder.bind(raceItems[position])
    }

    override fun getItemCount(): Int = raceItems.size

    inner class RaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val raceIcon: ImageView = itemView.findViewById(R.id.raceIcon)
        private val raceName: TextView = itemView.findViewById(R.id.raceName)
        private val lock: ImageView = itemView.findViewById(R.id.lock)
        private val buttonBackground: LinearLayout = itemView.findViewById(R.id.buttonBackgroundID)
        private val raceStatus: TextView = itemView.findViewById(R.id.raceStatus)
        private val raceCard: CardView = itemView.findViewById(R.id.raceCard)

        fun bind(item: LessonItem) {
            raceName.text = item.title

            // Set status and styling based on completion
            when (item.raceBusyLevel) {
                0 -> {
                    raceStatus.text = "TAMAMLANDI"
                    raceStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.lesson_completed))
                    raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_completed))
                }
                2 -> {
                    buttonBackground.setBackgroundResource(R.drawable.race_button_background)
                    raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_locked))
                    raceStatus.text = "KİLİTLİ"
                    lock.visibility = View.VISIBLE

                }
                1 -> {
                    raceStatus.text = "BAŞLA"
                    raceStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.lesson_header_green))
                    raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.button_disabled))
                    raceStatus.setBackgroundResource(R.drawable.button_background)

                }
            }

            // Set click listener
            itemView.setOnClickListener {
                if (item.raceBusyLevel == 2) return@setOnClickListener
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onRaceItemClick(item, pos)
            }
            raceStatus.setOnClickListener {
                if (item.raceBusyLevel == 2) return@setOnClickListener
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onRaceItemClick(item, pos)
            }
        }
    }
}
