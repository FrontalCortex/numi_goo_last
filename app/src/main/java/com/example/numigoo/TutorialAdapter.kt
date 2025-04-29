package com.example.numigoo

import android.animation.ValueAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.numigoo.model.TutorialStep

class TutorialAdapter(private val steps: List<TutorialStep>) : RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_tutorial, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        holder.bind(steps[position])
    }

    override fun getItemCount() = steps.size

    inner class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tutorialText: TextView = itemView.findViewById(R.id.tutorialText)
        private var isAnimating = false

        fun bind(step: TutorialStep) {
            tutorialText.text = step.text
            if (!isAnimating) {
                when (step.text) {
                    "Örneğin 1 sayısı abaküste bu şekilde gösterilir." -> animateBeadUp(getBeadById("rod0_bead_bottom1"))
                    "2 sayısı için iki boncuk yukarı çıkar." -> animateBeadUp(getBeadById("rod0_bead_bottom2"))
                    "3 sayısı için üç boncuk yukarı çıkar." -> animateBeadUp(getBeadById("rod0_bead_bottom3"))
                    "4 sayısı için dört boncuk yukarı çıkar." -> animateBeadUp(getBeadById("rod0_bead_bottom4"))
                    "5 sayısı için üst boncuk aşağı iner." -> animateBeadDown(getBeadById("rod0_bead_top"))
                    "6 sayısı için üst boncuk aşağı iner ve bir alt boncuk yukarı çıkar." -> {
                        animateBeadDown(getBeadById("rod0_bead_top"))
                        animateBeadUp(getBeadById("rod0_bead_bottom1"))
                    }
                    "7 sayısı için üst boncuk aşağı iner ve iki alt boncuk yukarı çıkar." -> {
                        animateBeadDown(getBeadById("rod0_bead_top"))
                        animateBeadUp(getBeadById("rod0_bead_bottom2"))
                    }
                    "8 sayısı için üst boncuk aşağı iner ve üç alt boncuk yukarı çıkar." -> {
                        animateBeadDown(getBeadById("rod0_bead_top"))
                        animateBeadUp(getBeadById("rod0_bead_bottom3"))
                    }
                    "9 sayısı için üst boncuk aşağı iner ve dört alt boncuk yukarı çıkar." -> {
                        animateBeadDown(getBeadById("rod0_bead_top"))
                        animateBeadUp(getBeadById("rod0_bead_bottom4"))
                    }
                }
            }
        }

        private fun getBeadById(beadId: String): ImageView? {
            return itemView.findViewById(
                itemView.resources.getIdentifier(beadId, "id", itemView.context.packageName)
            )
        }

        private fun animateBeadUp(bead: ImageView?) {
            if (bead == null) return
            isAnimating = true
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

        private fun animateBeadDown(bead: ImageView?) {
            if (bead == null) return
            isAnimating = true
            val animationDuration = 300L
            val moveDistance = 120

            bead.animate()
                .setDuration(animationDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationY(moveDistance.toFloat())
                .withEndAction {
                    isAnimating = false
                }
                .start()
        }

        private fun animateBeadsUp(bead: ImageView?) {
            if (bead == null) return
            isAnimating = true
            val animationDuration = 300L
            val moveDistance = 120

            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin + moveDistance

            bead.animate()
                .setDuration(animationDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withStartAction {
                    // Animasyon başlamadan önce yapılacak işlemler
                }
                .withEndAction {
                    // Animasyon bittiğinde yapılacak işlemler
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f // Translation'ı sıfırla
                    isAnimating = false
                }
                .translationY(-moveDistance.toFloat())
                .start()
        }

        private fun animateBeadsDown(bead: ImageView?) {
            if (bead == null) return
            isAnimating = true
            val animationDuration = 300L
            val moveDistance = 120

            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin - moveDistance

            bead.animate()
                .setDuration(animationDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withStartAction {
                    // Animasyon başlamadan önce yapılacak işlemler
                }
                .withEndAction {
                    // Animasyon bittiğinde yapılacak işlemler
                    params.bottomMargin = endMargin
                    bead.layoutParams = params
                    bead.translationY = 0f // Translation'ı sıfırla
                    isAnimating = false
                }
                .translationY(moveDistance.toFloat())
                .start()
        }
    }
} 
