package com.example.numigoo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.numigoo.model.TutorialStep
import com.example.numigoo.model.BeadAnimation

class TutorialAdapter(
    private val tutorialSteps: List<TutorialStep>,
    private val onStepComplete: () -> Unit
) : RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial_step, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        holder.bind(tutorialSteps[position])
    }

    override fun getItemCount() = tutorialSteps.size

    inner class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tutorialText: TextView = itemView.findViewById(R.id.tutorialText)
        private val abacusLinear: LinearLayout = itemView.findViewById(R.id.abacusLinear)
        private var isAnimating = false

        fun bind(step: TutorialStep) {
            tutorialText.text = step.text
            
            // Boncuk animasyonları
            when (val animation = step.animation) {
                is BeadAnimation -> performBeadAnimation(animation)
                is List<*> -> (animation as List<BeadAnimation>).forEach { performBeadAnimation(it) }
            }

            // Ekrana tıklama
            itemView.setOnClickListener {
                if (!isAnimating) {
                    onStepComplete()
                }
            }
        }

        private fun performBeadAnimation(animation: BeadAnimation) {
            isAnimating = true
            val bead = getBeadByPosition(animation.rod, animation.count)
            animateBeadsUp(bead)
        }

        private fun getBeadByPosition(rod: Int, count: Int): ImageView {
            val beadId = itemView.resources.getIdentifier(
                "rod${rod}_bead_bottom$count",
                "id",
                itemView.context.packageName
            )
            return itemView.findViewById(beadId)
        }

        private fun animateBeadsUp(bead: ImageView) {
            val animationDuration = 300L
            val moveDistance = 120

            bead.animate()
                .setDuration(animationDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationY(-moveDistance.toFloat())
                .withEndAction {
                    isAnimating = false
                }
                .start()
        }
    }
} 