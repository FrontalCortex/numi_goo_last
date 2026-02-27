package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TutorialOptionsAdapter : RecyclerView.Adapter<TutorialOptionsAdapter.OptionViewHolder>() {

    private val items: MutableList<String> = mutableListOf()
    private val selectedPositions: MutableSet<Int> = mutableSetOf()
    private var multipleChoice: Boolean = false

    fun submitOptions(options: List<String>, multipleChoice: Boolean) {
        this.multipleChoice = multipleChoice
        items.clear()
        items.addAll(options)
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    fun getSelectedPositions(): Set<Int> = selectedPositions.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val text = items[position]
        val isSelected = selectedPositions.contains(position)
        holder.bind(text, position, isSelected)
    }

    override fun getItemCount(): Int = items.size

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.optionCheckBox)
        private val textView: TextView = itemView.findViewById(R.id.optionText)

        fun bind(text: String, position: Int, isSelected: Boolean) {
            textView.text = text
            checkBox.isChecked = isSelected

            val clickListener = View.OnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@OnClickListener

                if (multipleChoice) {
                    if (selectedPositions.contains(pos)) {
                        selectedPositions.remove(pos)
                    } else {
                        selectedPositions.add(pos)
                    }
                } else {
                    selectedPositions.clear()
                    selectedPositions.add(pos)
                }
                notifyDataSetChanged()
            }

            itemView.setOnClickListener(clickListener)
            checkBox.setOnClickListener(clickListener)
        }
    }
}


