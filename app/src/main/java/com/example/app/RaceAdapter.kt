package com.example.app

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
import com.example.app.model.LessonItem

class RaceAdapter(
    private val context: Context,
    private val raceItems: MutableList<LessonItem>,
    private val onRaceItemClick: (LessonItem, Int) -> Unit,
    private val onPartChange: (Int) -> Unit

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

            // Reset ALL visual state to avoid recycling artifacts
            lock.visibility = View.GONE
            raceStatus.isEnabled = true
            raceStatus.alpha = 1f
            buttonBackground.setBackgroundResource(0)
            raceStatus.setBackgroundResource(0)
            raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))

            // Apply state per raceBusyLevel (set everything explicitly)
            when (item.raceBusyLevel) {
                0 -> {
                    raceStatus.text = "TAMAMLANDI"
                    raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_completed))
                    lock.visibility = View.GONE
                }
                2 -> {
                    raceStatus.text = "KİLİTLİ"
                    lock.visibility = View.VISIBLE
                    buttonBackground.setBackgroundResource(R.drawable.race_button_background)
                    raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_locked))
                    raceStatus.isEnabled = false
                    raceStatus.alpha = 0.8f
                }
                1 -> {
                    raceStatus.text = "BAŞLA"
                    lock.visibility = View.GONE
                    buttonBackground.setBackgroundResource(R.drawable.button_background)
                    raceStatus.setBackgroundResource(R.drawable.button_background)
                    raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }
                else -> {
                    raceStatus.text = "BAŞLA"
                    lock.visibility = View.GONE
                    buttonBackground.setBackgroundResource(R.drawable.button_background)
                    raceStatus.setBackgroundResource(R.drawable.button_background)
                    raceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
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
    fun raceUpdateItems(newItems: List<LessonItem>) {
        raceItems.clear()
        raceItems.addAll(newItems)
        notifyDataSetChanged()
    }
    fun updateRaceItem(position: Int, newItem: LessonItem) {
        if (position in raceItems.indices) {
            raceItems[position] = newItem
            notifyItemChanged(position)
        }
    }
}
