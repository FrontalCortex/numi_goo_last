package com.example.numigoo

import android.animation.ValueAnimator
import android.app.Activity
import android.widget.Button
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.example.numigoo.model.LessonItem
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.numigoo.GlobalLessonData.globalPartId
import com.example.numigoo.GlobalValues.lessonStep
import com.example.numigoo.GlobalValues.mapFragmentStepIndex

class LessonAdapter(
    private val context: Context,
    private val items: MutableList<LessonItem>,
    private val onPartChange: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnProgressUpdateListener {
        fun updateProgress(position: Int, progress: Int)
    }
    private var progressUpdateListener: OnProgressUpdateListener? = null
    fun setProgressUpdateListener(listener: OnProgressUpdateListener) {
        this.progressUpdateListener = listener
    }

    private fun showLessonBottomSheet(item: LessonItem, position: Int) {
        // Activity'deki view'ları bul
        val activity = context as Activity
        val coordinatorLayout = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        val scrimView = activity.findViewById<View>(R.id.scrimView)

        scrimView.visibility = View.VISIBLE
        scrimView.alpha = 0f

        // Eğer daha önce oluşturulmuş bir bottom sheet varsa kaldır
        coordinatorLayout.findViewWithTag<View>("bottom_sheet")?.let {
            coordinatorLayout.removeView(it)
        }

        // Bottom sheet'i inflate et
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.lesson_bottom_sheet, coordinatorLayout, false)
        bottomSheetView.tag = "bottom_sheet"

        // View'ları bul
        val titleText = bottomSheetView.findViewById<TextView>(R.id.lessonTitle)
        val descriptionText = bottomSheetView.findViewById<TextView>(R.id.lessonDescription)
        val actionButton = bottomSheetView.findViewById<Button>(R.id.actionButton)
        val bottomSheetLayout = bottomSheetView.findViewById<LinearLayout>(R.id.bottomSheetLayout)

        // İçerikleri ayarla
        titleText.text = item.title

        if (item.isCompleted) {
            if(item.stepIsFinish){
                if(item.type == 2){
                    actionButton.text = "Tekrar dene"
                }
                else{
                    actionButton.text = "Gözden geçir"
                }
                descriptionText.text = "Ders Tamamlandı"
                bottomSheetLayout.backgroundTintList = ContextCompat.getColorStateList(context, R.color.lesson_completed)
                actionButton.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.lesson_completed))
                }


                // Progress bar rengini güncelle
            } else {
                descriptionText.text = "Ders: ${item.currentStep}/${item.stepCount}"
                bottomSheetLayout.backgroundTintList = ContextCompat.getColorStateList(context, R.color.lesson_completed)

                actionButton.apply {
                    text = "-5                           BAŞLAT"
                    textAlignment = View.TEXT_ALIGNMENT_TEXT_START  // veya
                    // Beyaz, köşeleri yuvarlatılmış
                    actionButton.setBackgroundColor(context.getColor(R.color.white))
                    setTextColor(ContextCompat.getColor(context, R.color.lesson_completed))
                    isEnabled = true
                    // İkon ekle (solda)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.lighting__1_, 0, 0, 0)
                }
            }
        } else {
            descriptionText.text = "Bunun kilidini açmak için yukarıdaki düzeylerin tümünü tamamla!"
            titleText.setTextColor(ContextCompat.getColor(context, R.color.lesson_locked))
            descriptionText.setTextColor(ContextCompat.getColor(context, R.color.lesson_locked))
            bottomSheetLayout.backgroundTintList = ContextCompat.getColorStateList(context, R.color.background_color)

            actionButton.text = "KİLİTLİ"
            actionButton.textAlignment = View.TEXT_ALIGNMENT_CENTER
            actionButton.setBackgroundColor(context.getColor(R.color.circleBackground_color))
            actionButton.setTextColor(ContextCompat.getColor(context, R.color.lesson_locked))
            actionButton.isEnabled = false
        }

        // Bottom sheet'i CoordinatorLayout'a ekle
        coordinatorLayout.addView(bottomSheetView)

        // BottomSheetBehavior oluştur
        val behavior = BottomSheetBehavior.from(bottomSheetLayout)

        // Scrim view'ı göster ve tıklama listener'ı ekle
        scrimView.visibility = View.VISIBLE
        scrimView.animate()
            .alpha(0.5f)
            .setDuration(300)
            .start()

        // Scrim'e tıklandığında bottom sheet'i kapat
        scrimView.setOnClickListener {
            // Bottom sheet'i gizlenebilir yap ve aşağı kaydır
            behavior.isHideable = true
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Bottom sheet callback'i ekle
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Bottom sheet tamamen kapandığında view'ı kaldır
                    coordinatorLayout.removeView(bottomSheetView)

                    // Scrim'i animate ederek kapat
                    scrimView.animate()
                        .alpha(0f)
                        .setDuration(100)
                        .withEndAction {
                            scrimView.visibility = View.GONE
                        }
                        .start()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Kaydırma sırasında arka plan transparanlığını ayarla
                val alpha = 0.5f * (slideOffset + 1) // 0f ile 0.5f arası
                scrimView.alpha = alpha
            }
        })

        // Button tıklama
        actionButton.setOnClickListener {
            if (item.isCompleted) {
                // lessonStep değerini güncelle

                // Bottom sheet'i aşağı doğru kaydırarak gizle
                behavior.isHideable = true
                behavior.state = BottomSheetBehavior.STATE_HIDDEN

                // Activity'yi bul ve FragmentActivity olarak cast et
                val activity = context as FragmentActivity

                // Fragment container'ı görünür yap
                val fragmentContainer = activity.findViewById<View>(R.id.abacusFragmentContainer)
                fragmentContainer.visibility = View.VISIBLE

                // AbacusFragment'i oluştur
                val fragment = item?.fragment?.invoke()

                // Animasyon için slide-in efekti
                val slideIn = android.R.anim.slide_in_left
                val slideOut = android.R.anim.slide_out_right
                item.mapFragmentIndex.also { mapFragmentStepIndex = it!! }
                item.startStepNumber.also { lessonStep = it!! }
                if(item.isBlinding == true){
                    if(item.tutorialIsFinish){
                        activity.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(slideIn, slideOut)
                            .replace(R.id.abacusFragmentContainer, BlindingLessonFragment())
                            .addToBackStack(null)
                            .commit()
                    } else {
                        //startStepNumber'ı global lessonStep'e atadık

                        activity.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(slideIn, slideOut)
                            .replace(R.id.abacusFragmentContainer, TutorialFragment(item.tutorialNumber))
                            .addToBackStack(null)
                            .commit()
                    }
                }

                else{


                if(item.tutorialIsFinish){
                    activity.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, AbacusFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    //startStepNumber'ı global lessonStep'e atadık

                    activity.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, TutorialFragment(item.tutorialNumber))
                        .addToBackStack(null)
                        .commit()
                    }
                }
            }
            else {
                // Kilitli durum için sadece collapse et
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        // Lesson kartının pozisyonunu al
        val lessonView = activity.findViewById<RecyclerView>(R.id.lessonsRecyclerView)
            .layoutManager?.findViewByPosition(position)

        lessonView?.let {
            val location = IntArray(2)
            it.getLocationInWindow(location)
            val lessonY = location[1]

            // Bottom sheet'in peekHeight'ını lesson kartının altına ayarla
            behavior.peekHeight = lessonY + it.height
        }

        // Bottom sheet'i göster
        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN  // Önce gizli duruma getir
        bottomSheetView.post {  // Bir sonraki frame'de göster
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    // Click listener interface
    interface OnLessonClickListener {
        fun onLessonClick(item: LessonItem, position: Int)
    }

    private var onLessonClickListener: OnLessonClickListener? = null

    fun setOnLessonClickListener(listener: OnLessonClickListener) {
        onLessonClickListener = listener
    }

    override fun getItemViewType(position: Int): Int = GlobalLessonData.getLessonItem(position)?.type ?: items[position].type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            LessonItem.TYPE_LESSON -> LessonViewHolder(
                inflater.inflate(R.layout.item_lesson, parent, false)
            )
            LessonItem.TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_header, parent, false)
            )
            LessonItem.TYPE_CHEST -> LessonViewHolder(
                inflater.inflate(R.layout.item_lesson, parent, false)
            )
            LessonItem.TYPE_RACE -> LessonViewHolder(
                inflater.inflate(R.layout.item_race, parent, false)
            )
            LessonItem.TYPE_PART -> PartViewHolder(
                inflater.inflate(R.layout.item_part, parent, false)
            )
            LessonItem.TYPE_BACK_PART -> BackPartViewHolder(
                inflater.inflate(R.layout.item_back_part, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = GlobalLessonData.getLessonItem(position) ?: items[position]
        when (holder) {
            is LessonViewHolder -> {
                holder.bind(item)
            }
            is HeaderViewHolder -> holder.bind(item)
            is PartViewHolder -> holder.bind(item)
            is BackPartViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = GlobalLessonData.lessonItems.size

    fun getItem(position: Int): LessonItem = GlobalLessonData.getLessonItem(position) ?: items[position]

    fun updateLessonOffset(position: Int, newOffset: Int) {
        if (position in items.indices) {
            items[position].let { item ->
                if (item.type == LessonItem.TYPE_LESSON) {
                    item.offset = newOffset
                    notifyItemChanged(position)
                }
            }
        }
    }

    fun updateLessonItem(position: Int, newItem: LessonItem) {
        if (position in items.indices) {
            items[position] = newItem
            notifyItemChanged(position)
        }
    }

    // ViewHolder sınıfları
    inner class PartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val sectionDescription: TextView = itemView.findViewById(R.id.sectionDescription)

        private val fastForwardButton: Button = itemView.findViewById(R.id.fastForwardButton)
        fun bind(item: LessonItem) {
            fastForwardButton.setOnClickListener {
                // Butona tıklandığında item'in partId'sini onPartChange callback'ine gönder
                globalPartId = item.partId!!
                item.partId?.let { partId ->
                    onPartChange(partId)
                }

            }
        }
    }
    inner class BackPartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


        private val fastForwardButton: Button = itemView.findViewById(R.id.fastForwardButton)
        fun bind(item: LessonItem) {
            fastForwardButton.setOnClickListener {
                globalPartId = item.partId!!
                onPartChange(item.partId!!)

            }
        }
    }

    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lessonIcon: ImageView = itemView.findViewById(R.id.lessonIcon)
        private val lessonCard: CardView = itemView.findViewById(R.id.lessonCard)
        private val progressBar: CircleProgressBar = itemView.findViewById(R.id.progressBar)

        fun updateProgress(progress: Float) {
            // Mevcut progress değerini al
            val currentProgress = progressBar.progress

            // Animasyon oluştur
            val animator = ValueAnimator.ofFloat(currentProgress, progress)
            animator.duration = 500 // 500ms sürecek
            animator.interpolator = AccelerateDecelerateInterpolator() // Yumuşak geçiş için

            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                progressBar.setProgressValue(animatedValue)
            }

            // Animasyonu başlat
            animator.start()
        }

        fun updateProgressBarColor(color: Int) {
            progressBar.setProgressColor(color)
        }
        fun bind(item: LessonItem) {
            when (item.type) {
                LessonItem.TYPE_CHEST -> {
                    val backgroundColor = if (item.isCompleted) {
                        ContextCompat.getColor(context, R.color.lesson_completed)
                    } else {
                        ContextCompat.getColor(context, R.color.lesson_locked)
                    }
                    lessonCard.setCardBackgroundColor(backgroundColor)

                    lessonCard.setOnClickListener {
                        showLessonBottomSheet(item, adapterPosition)
                    }
                    if(item.stepIsFinish){
                        updateProgressBarColor(ContextCompat.getColor(context, R.color.yellow))
                        lessonIcon.setImageResource(item.stepCupIcon)
                    }else{
                        lessonIcon.setImageResource(item.stepCupIcon)
                    }
                    val completedSteps = item.stepCompletionStatus.count { it }
                    when (completedSteps) {
                        1 -> updateProgress((1f / item.stepCount) * 100)
                        2 -> updateProgress((2f / item.stepCount) * 100)
                        3 -> updateProgress((3f / item.stepCount) * 100)
                        4 -> updateProgress((4f / item.stepCount) * 100)
                        else -> progressBar.setProgressValue(0F)
                    }
                    if(item.stepIsFinish){
                        updateProgressBarColor(ContextCompat.getColor(context, R.color.yellow))
                    }

                }

                LessonItem.TYPE_LESSON -> {
                    val backgroundColor = if (item.isCompleted) {
                        ContextCompat.getColor(context, R.color.lesson_completed)
                    } else {
                        ContextCompat.getColor(context, R.color.lesson_locked)
                    }
                    lessonCard.setCardBackgroundColor(backgroundColor)
                    lessonCard.setOnClickListener {
                        showLessonBottomSheet(item, adapterPosition)
                    }

                    // stepCompletionStatus kontrolü
                    val completedSteps = item.stepCompletionStatus.count { it }
                    when (completedSteps) {
                        1 -> updateProgress((1f / item.stepCount) * 100)
                        2 -> updateProgress((2f / item.stepCount) * 100)
                        3 -> updateProgress((3f / item.stepCount) * 100)
                        4 -> updateProgress((4f / item.stepCount) * 100)
                        else -> progressBar.setProgressValue(0F)
                    }
                    if(item.stepIsFinish){
                        updateProgressBarColor(ContextCompat.getColor(context, R.color.yellow))
                    }
                }
            }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)


        fun bind(item: LessonItem) {
            headerText.text = item.title
        }
    }
    fun updateItems(newItems: List<LessonItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}




