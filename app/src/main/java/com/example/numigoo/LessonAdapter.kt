package com.example.numigoo

import android.app.Activity
import android.widget.Button
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.example.numigoo.model.LessonItem

class LessonAdapter(
    private val context: Context,
    private val items: List<LessonItem>,
    private val onLessonClick: (LessonItem, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            descriptionText.text = "${item.currentStep}/${item.stepCount}"
            actionButton.text = "BAŞLAT +10 PUAN"
            actionButton.setBackgroundResource(R.drawable.button_background)
            actionButton.isEnabled = true
        } else {
            descriptionText.text = "Bunun kilidini açmak için yukarıdaki düzeylerin tümünü tamamla!"
            actionButton.text = "KİLİTLİ"
            actionButton.setBackgroundColor(context.getColor(R.color.gray))
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
                // Bottom sheet'i aşağı doğru kaydırarak gizle
                behavior.isHideable = true
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
                
                // Activity'yi bul ve FragmentActivity olarak cast et
                val activity = context as FragmentActivity
                
                // Fragment container'ı görünür yap
                val fragmentContainer = activity.findViewById<View>(R.id.abacusFragmentContainer)
                fragmentContainer.visibility = View.VISIBLE

                // AbacusFragment'i oluştur
                val fragment = item.fragment()
                
                // Animasyon için slide-in efekti
                val slideIn = android.R.anim.slide_in_left
                val slideOut = android.R.anim.slide_out_right
                
                // Fragment'ı container'a ekle
                activity.supportFragmentManager.beginTransaction()
                    .setCustomAnimations(slideIn, slideOut)
                    .replace(R.id.abacusFragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
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

    override fun getItemViewType(position: Int): Int = items[position].type

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

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is LessonViewHolder -> {
                holder.bind(item)
            }
            is HeaderViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): LessonItem = items[position]

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

    // ViewHolder sınıfları
    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lessonIcon: ImageView = itemView.findViewById(R.id.lessonIcon)
        private val lessonCard: CardView = itemView.findViewById(R.id.lessonCard)
        private val progressBar: CircleProgressBar = itemView.findViewById(R.id.progressBar)


        fun bind(item: LessonItem) {

            when (item.type) {
                LessonItem.TYPE_CHEST -> {
                    lessonIcon.setImageResource(R.mipmap.cup_ic)
                    lessonCard.setOnClickListener {
                        showLessonBottomSheet(item, adapterPosition)
                    }
                    // Sandık için arka plan rengi istersen burada ayarlayabilirsin
                    lessonCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.lesson_locked
                        )
                    )
                }

                LessonItem.TYPE_LESSON -> {
                    // Arka plan rengini ayarla
                    val backgroundColor = if (item.isCompleted) {
                        ContextCompat.getColor(context, R.color.lesson_completed)
                    } else {
                        ContextCompat.getColor(context, R.color.lesson_locked)
                    }
                    lessonCard.setCardBackgroundColor(backgroundColor)
                    progressBar.visibility = View.VISIBLE
                    lessonCard.setOnClickListener {
                        showLessonBottomSheet(item, adapterPosition)
                    }
                    // Burada ders ikonunu ayarlamak istersen ekleyebilirsin
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

}


