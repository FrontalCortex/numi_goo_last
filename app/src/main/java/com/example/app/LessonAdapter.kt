package com.example.app

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import com.example.app.model.LessonItem
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.app.GlobalLessonData.globalPartId
import com.example.app.GlobalValues.lessonStep
import com.example.app.GlobalValues.mapFragmentStepIndex
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LessonAdapter(
    private val context: Context,
    private val items: MutableList<LessonItem>,
    private val onPartChange: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnProgressUpdateListener {
        fun updateProgress(position: Int, progress: Int)
    }
    private lateinit var raceAdapter: RaceAdapter // Adapter'ı tanımla
    private var progressUpdateListener: OnProgressUpdateListener? = null
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    fun setProgressUpdateListener(listener: OnProgressUpdateListener) {
        this.progressUpdateListener = listener
    }
    
    private fun getCurrentPlan(callback: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback("Free")
            return
        }
        
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val plan = doc.getString("plan") ?: "Free"
                    callback(plan)
                } else {
                    callback("Free")
                }
            }
            .addOnFailureListener {
                callback("Free")
            }
    }

    @SuppressLint("MissingInflatedId")
    fun showLessonBottomSheet(item: LessonItem, position: Int) {
        // Activity'deki view'ları bul
        val activity = context as Activity
        val coordinatorLayout = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        val scrimView = activity.findViewById<View>(R.id.scrimView)
        
        // GuidePanel açık mı kontrol et
        val guidePanel = activity.findViewById<View>(R.id.guidePanel)
        val isGuidePanelVisible = guidePanel?.visibility == View.VISIBLE

        // GuidePanel açıksa scrimView'ı gösterme (ekran karartma)
        if (!isGuidePanelVisible) {
            scrimView.visibility = View.VISIBLE
            scrimView.alpha = 0f
        } else {
            scrimView.visibility = View.GONE
            scrimView.alpha = 0f
        }

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
        val againTutorial = bottomSheetView.findViewById<TextView>(R.id.againTutorial)
        val record = bottomSheetView.findViewById<TextView>(R.id.recordText)
        val fireAnim = bottomSheetView.findViewById<LottieAnimationView>(R.id.fireAnimID)
        val recordLayout = bottomSheetView.findViewById<LinearLayout>(R.id.recordLayout)


        // İçerikleri ayarla
        titleText.text = item.title

        if (item.isCompleted) {
            if(item.tutorialNumber != 0 && item.tutorialIsFinish){
                againTutorial.visibility = View.VISIBLE
            }
            else{

                againTutorial.visibility = View.INVISIBLE
            }
            if(item.stepIsFinish){
                if(item.type == 2){
                    // Buton metnini başta boş bırak (plan kontrolü tamamlanana kadar)
                    actionButton.text = ""
                    actionButton.isEnabled = false // Plan kontrolü tamamlanana kadar devre dışı
                    recordLayout.setBackgroundResource(R.drawable.record_background)
                    // Abonelik durumuna göre buton metnini ayarla
                    record.text="Rekor: ${item.record}"
                    fireAnim.visibility = View.VISIBLE
                    
                    // GuidePanel'in son adımında animasyon başlatma işlemi MapFragment'te yapılıyor
                    // Burada animasyon başlatmıyoruz çünkü kontrol ve başlatma MapFragment'te setOnLastStepReachedListener içinde yapılıyor
                    
                    getCurrentPlan { plan ->
                        if (plan == "Free") {
                            actionButton.text = "Tekrar etmek için planı yükselt"
                        } else {
                            actionButton.text = "Tekrar dene"
                        }
                        actionButton.isEnabled = true // Plan kontrolü tamamlandı, butonu aktif et
                    }
                }
                else{
                    actionButton.text = "Gözden geçir"
                }
                descriptionText.text = "Ders Tamamlandı"
                // Progress bar rengini güncelle
            } else {
                descriptionText.text = "Ders: ${item.currentStep}/${item.stepCount}"
                bottomSheetLayout.backgroundTintList = ContextCompat.getColorStateList(context, R.color.panel_background)

                //tutorial olanlarda ve tutorialIsFinish olanlarda çıkacak.
                actionButton.apply {
                    text = "-1                           BAŞLAT"
                    textAlignment = View.TEXT_ALIGNMENT_TEXT_START  // veya
                    // Beyaz, köşeleri yuvarlatılmış
                    actionButton.setBackgroundColor(context.getColor(R.color.lesson_completed))
                    setTextColor(ContextCompat.getColor(context, R.color.panel_background))
                    isEnabled = true
                    // İkon ekle (solda)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.lighting__1_, 0, 0, 0)
                }
            }
        } else {
            descriptionText.text = "Bunun kilidini açmak için yukarıdaki düzeylerin tümünü tamamla!"
            titleText.setTextColor(ContextCompat.getColor(context, R.color.lesson_locked))
            againTutorial.visibility = View.INVISIBLE
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
        
        // GuidePanel açıksa bottom sheet'in tıklanabilirliğini engelle
        if (isGuidePanelVisible) {
            disableBottomSheetInteractions(bottomSheetView, bottomSheetLayout, actionButton, againTutorial, behavior)
        }

        // Scrim view'ı göster ve tıklama listener'ı ekle (sadece GuidePanel açık değilse)
        if (!isGuidePanelVisible) {
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
        } else {
            // GuidePanel açıkken scrimView tıklanamaz ve görünmez
            scrimView.setOnClickListener(null)
        }

        // Bottom sheet callback'i ekle
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // recordLayout animasyonunu durdur (eğer varsa)
                    val recordLayout = bottomSheetView.findViewById<LinearLayout>(R.id.recordLayout)
                    val animator = recordLayout?.tag as? ValueAnimator
                    animator?.cancel()
                    recordLayout?.tag = null
                    
                    // Bottom sheet tamamen kapandığında view'ı kaldır
                    coordinatorLayout.removeView(bottomSheetView)

                    // Scrim'i animate ederek kapat (sadece GuidePanel açık değilse)
                    if (!isGuidePanelVisible) {
                        scrimView.animate()
                            .alpha(0f)
                            .setDuration(100)
                            .withEndAction {
                                scrimView.visibility = View.GONE
                            }
                            .start()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Kaydırma sırasında arka plan transparanlığını ayarla (sadece GuidePanel açık değilse)
                if (!isGuidePanelVisible) {
                    val alpha = 0.5f * (slideOffset + 1) // 0f ile 0.5f arası
                    scrimView.alpha = alpha
                }
            }
        })

        againTutorial.setOnClickListener{
            // Bottom sheet'i aşağı doğru kaydırarak gizle
            behavior.isHideable = true
            behavior.state = BottomSheetBehavior.STATE_HIDDEN

            // Activity'yi bul ve FragmentActivity olarak cast et
            val activity = context as FragmentActivity

            // Fragment container'ı görünür yap
            val fragmentContainer = activity.findViewById<View>(R.id.abacusFragmentContainer)
            fragmentContainer.visibility = View.VISIBLE
            val slideIn = android.R.anim.slide_in_left
            val slideOut = android.R.anim.slide_out_right
            item.mapFragmentIndex.also { mapFragmentStepIndex = it!! }
            item.startStepNumber.also { lessonStep = it!! }



                    activity.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, TutorialFragment(item.tutorialNumber))
                        .addToBackStack(null)
                        .commit()




        }
        // Button tıklama
        actionButton.setOnClickListener {
            if (item.isCompleted) {
                getCurrentPlan { plan ->
                    // Eğer type == 2 ve Free plan ise, abonelik sayfasına yönlendir
                    if (item.type == 2 && item.stepIsFinish && plan == "Free") {
                        // Abonelik sayfasına yönlendir
                        val intent = Intent(context, SubscriptionActivity::class.java)
                        context.startActivity(intent)
                        // Bottom sheet'i kapat
                        behavior.isHideable = true
                        behavior.state = BottomSheetBehavior.STATE_HIDDEN
                        return@getCurrentPlan
                    }
                    
                    // Enerji kontrolü (sadece Free plan için)
                    val mainActivity = context as MainActivity
                    val energyManager = mainActivity.getEnergyManager()
                    
                    if (plan == "Free") {
                        if (!energyManager.hasEnoughEnergy(1)) {
                            // Yeterli enerji yok, kullanıcıya uyarı göster
                            showEnergyWarning(context)
                            return@getCurrentPlan
                        }
                        // Enerjiyi kullan
                        energyManager.useEnergy(1)
                    }
                    
                    // Ders başlat
                    continueWithLesson(item, behavior)
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
    
    private fun continueWithLesson(item: LessonItem, behavior: BottomSheetBehavior<LinearLayout>) {
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

    // Click listener interface
    interface OnLessonClickListener {
        fun onLessonClick(item: LessonItem, position: Int)
    }

    private var onLessonClickListener: OnLessonClickListener? = null

    fun setOnLessonClickListener(listener: OnLessonClickListener) {
        onLessonClickListener = listener
    }
    
    private fun showEnergyWarning(context: Context) {
        val mainActivity = context as MainActivity
        // Doğrudan enerji yenileme dialog'unu göster
        mainActivity.showEnergyRefillDialog()
    }

    private fun showRacePanel(item: LessonItem, position: Int) {
        // Activity'deki view'ları bul
        val activity = context as Activity
        val coordinatorLayout = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        val scrimView = activity.findViewById<View>(R.id.scrimView)

        scrimView.visibility = View.VISIBLE
        scrimView.alpha = 0f

        // Eğer daha önce oluşturulmuş bir race panel varsa kaldır
        coordinatorLayout.findViewWithTag<View>("race_panel")?.let {
            coordinatorLayout.removeView(it)
        }

        // Race panel'i inflate et
        val racePanelView = LayoutInflater.from(context)
            .inflate(R.layout.race_bottom_sheet, coordinatorLayout, false)
        racePanelView.tag = "race_panel"

        // View'ları bul
        val raceTitle = racePanelView.findViewById<TextView>(R.id.raceTitle)
        val closeButton = racePanelView.findViewById<TextView>(R.id.closeButton)
        val raceRecyclerView = racePanelView.findViewById<RecyclerView>(R.id.raceRecyclerView)
        val racePanelLayout = racePanelView.findViewById<CoordinatorLayout>(R.id.racePanelLayout)
        val raceContentLayout = racePanelView.findViewById<LinearLayout>(R.id.raceContentLayout)

        // Başlığı ayarla
        raceTitle.text = item.title

        val racePartId = item.racePartId!!
        // Part verisi asenkron yüklendiği için önce yükleyip sonra paneli kuruyoruz (ilk tıklamada ders listesi yerine race listesi gösterilir)
        raceRecyclerView.layoutManager = LinearLayoutManager(context)
        GlobalLessonData.initialize(context, racePartId) {
            (context as? Activity)?.runOnUiThread {
                globalPartId = racePartId
                onPartChange(racePartId)

                raceAdapter = RaceAdapter(
                    context,
                    raceItems = GlobalLessonData.lessonItems.toMutableList(),
                    { raceItem, clickedIndex ->
                        onRaceStartClicked(raceItem, clickedIndex)
                    },
                    onPartChange = { newPartId ->
                        globalPartId = newPartId
                        GlobalLessonData.initialize(context, newPartId) {
                            val updatedRaceItems = GlobalLessonData.lessonItems.filter {
                                it.racePartId == newPartId || it.type == LessonItem.TYPE_RACE
                            }
                            raceAdapter.raceUpdateItems(updatedRaceItems)
                        }
                    }
                )
                LessonManager.setRaceAdapter(raceAdapter)
                raceRecyclerView.adapter = raceAdapter

                coordinatorLayout.addView(racePanelView)

                val behavior = BottomSheetBehavior.from(raceContentLayout)
                scrimView.visibility = View.VISIBLE
                scrimView.animate()
                    .alpha(0.5f)
                    .setDuration(300)
                    .start()

                scrimView.setOnClickListener {
                    globalPartId = item.backRaceId!!
                    onPartChange(globalPartId)
                    behavior.isHideable = true
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                closeButton.setOnClickListener {
                    globalPartId = item.backRaceId!!
                    onPartChange(globalPartId)
                    behavior.isHideable = true
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            globalPartId = item.backRaceId!!
                            onPartChange(globalPartId)
                            coordinatorLayout.removeView(racePanelView)
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
                        val alpha = 0.5f * (slideOffset + 1)
                        scrimView.alpha = alpha
                    }
                })

                val lessonView = activity.findViewById<RecyclerView>(R.id.lessonsRecyclerView)
                    .layoutManager?.findViewByPosition(position)
                lessonView?.let {
                    val location = IntArray(2)
                    it.getLocationInWindow(location)
                    val lessonY = location[1]
                    behavior.peekHeight = lessonY + it.height
                }

                behavior.isHideable = true
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
                racePanelView.post {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    private fun onRaceStartClicked(raceItem: LessonItem, clickedIndex: Int) {
        val activity = context as FragmentActivity
        
        // Race item'ının mapFragmentIndex değerini global mapFragmentStepIndex'e ata
        raceItem.mapFragmentIndex.also { mapFragmentStepIndex = it!! }
        raceItem.startStepNumber.also { lessonStep = it!! }
        

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.abacusFragmentContainer, BlindingLessonFragment())
            .addToBackStack(null)
            .commit()
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
                LessonItem.TYPE_RACE -> RaceViewHolder(
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
            is RaceViewHolder -> holder.bind(item)
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
    
    fun refreshRacePanelIfOpen() {
        // Race panel açıksa sadece adapter'ı güncelle
        val activity = context as FragmentActivity
        val coordinatorLayout = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        coordinatorLayout?.findViewWithTag<View>("race_panel")?.let { racePanel ->
            try {
                // RaceAdapter'ı bul ve güncelle
                val raceRecyclerView = racePanel.findViewById<RecyclerView>(R.id.raceRecyclerView)
                val currentAdapter = raceRecyclerView.adapter as? RaceAdapter
                
                if (currentAdapter != null) {
                    // Race panel'deki mevcut verileri al (racePartId'den)
                    val raceTitle = racePanel.findViewById<TextView>(R.id.raceTitle)
                    val currentTitle = raceTitle.text.toString()
                    
                    // Hangi race item'ının açık olduğunu bul
                    val raceItem = GlobalLessonData.lessonItems.find { it.title == currentTitle }
                    val racePartId = raceItem?.racePartId ?: 7
                    
                    // Güncel verileri al ve adapter'ı güncelle
                    // Önce createLessonItems ile oluştur, sonra güncellenmiş verilerle değiştir
                    val baseRaceItems = GlobalLessonData.createLessonItems(racePartId)
                    val updatedRaceItems = baseRaceItems.map { baseItem ->
                        // Güncellenmiş veriyi bul
                        val updatedItem = GlobalLessonData.lessonItems.find { it.title == baseItem.title }
                        updatedItem ?: baseItem
                    }
                    currentAdapter.raceUpdateItems(updatedRaceItems)
                    Log.d("RaceAdapter", "Race adapter güncellendi: ${updatedRaceItems.size} item")
                    Log.d("RaceAdapter", "Race adapter instance: ${currentAdapter.hashCode()}")
                    Log.d("RaceAdapter", "Race partId: $racePartId")
                } else {
                    Log.d("RaceAdapter", "Race adapter bulunamadı")
                    Log.d("RaceAdapter", "RecyclerView adapter: ${raceRecyclerView.adapter?.javaClass?.simpleName}")
                }
            } catch (e: Exception) {
                Log.e("RaceAdapter", "Race panel güncellenirken hata: ${e.message}")
            }
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
                sectionTitle.text=item.sectionTitle
                sectionDescription.text=item.sectionDescription
            }
        }
    }
    inner class BackPartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fastForwardButton: Button = itemView.findViewById(R.id.fastForwardButton)
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        fun bind(item: LessonItem) {
            fastForwardButton.setOnClickListener {
                globalPartId = item.partId!!
                onPartChange(item.partId!!)
            }

            sectionTitle.text=item.sectionTitle
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

    inner class RaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lessonIcon: ImageView = itemView.findViewById(R.id.lessonIcon)
        private val lessonCard: CardView = itemView.findViewById(R.id.lessonCard)
        private val progressBar: CircleProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind(item: LessonItem) {
            // Race item'ı için özel styling
            val backgroundColor = if (item.isCompleted) {
                ContextCompat.getColor(context, R.color.lesson_completed)
            } else {
                ContextCompat.getColor(context, R.color.lesson_locked)
            }
            lessonCard.setCardBackgroundColor(backgroundColor)

            // Race item'a tıklandığında race panel'i göster
            lessonCard.setOnClickListener {
                showRacePanel(item, adapterPosition)
            }

            // Progress bar'ı ayarla
            if (item.isCompleted) {
                updateProgressBarColor(ContextCompat.getColor(context, R.color.yellow))
                updateProgress(100f)
            } else {
                updateProgressBarColor(ContextCompat.getColor(context, R.color.circleBackground_color))
                updateProgress(0f)
            }
        }

        private fun updateProgress(progress: Float) {
            val currentProgress = progressBar.progress
            val animator = ValueAnimator.ofFloat(currentProgress, progress)
            animator.duration = 500
            animator.interpolator = AccelerateDecelerateInterpolator()

            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                progressBar.setProgressValue(animatedValue)
            }
            animator.start()
        }

        private fun updateProgressBarColor(color: Int) {
            progressBar.setProgressColor(color)
        }
    }
    fun updateItems(newItems: List<LessonItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * Belirtilen view için pulse animasyonu başlatır (eğer animasyon zaten çalışmıyorsa)
     * @param view Animasyon uygulanacak view
     * @param stopAnimationOnClick View'a tıklandığında animasyonu durdursun mu? (default: true)
     *                            Eğer false ise, click listener eklenmez (setTargetViewForLastStep gibi başka bir mekanizma kullanılabilir)
     * @param onViewClicked View'a tıklandığında çağrılacak callback (opsiyonel, sadece stopAnimationOnClick true ise çalışır)
     */
    fun startPulseAnimationForView(
        view: View, 
        stopAnimationOnClick: Boolean = true,
        onViewClicked: (() -> Unit)? = null
    ) {
        // Eğer animasyon zaten çalışıyorsa başlatma
        if (view.tag is ValueAnimator) {
            return
        }
        
        // Animasyonu başlat
        val animator = startRecordLayoutPulseAnimation(view)
        view.tag = animator
        
        // View'a tıklandığında animasyonu durdur (eğer isteniyorsa)
        if (stopAnimationOnClick) {
            view.setOnClickListener {
                val currentAnimator = view.tag as? ValueAnimator
                currentAnimator?.cancel()
                view.tag = null
                view.scaleX = 1f
                view.scaleY = 1f
                // Opsiyonel callback'i çağır
                onViewClicked?.invoke()
            }
        }
    }
    
    /**
     * View için pulse animasyonu oluşturur ve başlatır
     * @param view Animasyon uygulanacak view (herhangi bir View tipi olabilir)
     * @return Başlatılan ValueAnimator
     */
    private fun startRecordLayoutPulseAnimation(view: View): ValueAnimator {
        // Scale animasyonu: 1.0 -> 1.2 -> 1.0 (balon gibi büyüyüp küçülme)
        val animator = ValueAnimator.ofFloat(1.0f, 1.05f, 1.0f)
        animator.duration = 1200 // 800ms sürecek
        animator.repeatCount = ValueAnimator.INFINITE // Sürekli tekrar et
        animator.interpolator = AccelerateDecelerateInterpolator() // Yumuşak geçiş
        
        animator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            view.scaleX = scale
            view.scaleY = scale
        }
        
        animator.start()
        return animator
    }
    
    private fun disableBottomSheetInteractions(
        bottomSheetView: View,
        bottomSheetLayout: LinearLayout,
        actionButton: Button,
        againTutorial: TextView,
        behavior: BottomSheetBehavior<LinearLayout>
    ) {
        // Bottom sheet view'ın tıklanabilirliğini kapat
        // Touch event'leri GuidePanel'e iletmek için consume etmiyoruz
        bottomSheetView.apply {
            isClickable = false
            isFocusable = false
            // Touch listener koymuyoruz, böylece touch event'ler alt view'lara (GuidePanel'e) geçebilir
        }
        
        // Bottom sheet layout'un tıklanabilirliğini kapat
        // Touch event'leri GuidePanel'e iletmek için consume etmiyoruz
        bottomSheetLayout.apply {
            isClickable = false
            isFocusable = false
            // Touch listener koymuyoruz, böylece touch event'ler alt view'lara (GuidePanel'e) geçebilir
        }
        
        // Action button'un tıklanabilirliğini kapat
        actionButton.apply {
            isClickable = false
            isEnabled = false
            setOnClickListener(null) // Click listener'ı kaldır
            setOnTouchListener { _, _ -> true } // Sadece button için touch event'leri consume et
        }
        
        // Again tutorial text'in tıklanabilirliğini kapat
        againTutorial.apply {
            isClickable = false
            isFocusable = false
            setOnClickListener(null) // Click listener'ı kaldır
            setOnTouchListener { _, _ -> true } // Sadece text için touch event'leri consume et
        }
        
        // BottomSheetBehavior'ın drag özelliğini kapat (kaydırma engellenmeli)
        behavior.isDraggable = false
        
        // BottomSheetView'in touch event'lerini GuidePanel'e iletmek için
        // Eğer touch event BottomSheet'in içindeki tıklanabilir elementlere geliyorsa consume et,
        // değilse consume etme (false döndür) ve alt view'lara (GuidePanel'e) ilet
        bottomSheetView.setOnTouchListener { view, event ->
            // Touch event'in koordinatlarını al
            val x = event.x
            val y = event.y
            
            // ActionButton veya AgainTutorial'a tıklanıyorsa consume et
            val actionButtonRect = android.graphics.Rect()
            actionButton.getHitRect(actionButtonRect)
            
            val againTutorialRect = android.graphics.Rect()
            againTutorial.getHitRect(againTutorialRect)
            
            // BottomSheetView'in koordinat sistemine göre dönüştür
            val location = IntArray(2)
            bottomSheetView.getLocationInWindow(location)
            val bottomSheetX = location[0]
            val bottomSheetY = location[1]
            
            actionButton.getLocationInWindow(location)
            val actionButtonX = location[0] - bottomSheetX
            val actionButtonY = location[1] - bottomSheetY
            
            againTutorial.getLocationInWindow(location)
            val againTutorialX = location[0] - bottomSheetX
            val againTutorialY = location[1] - bottomSheetY
            
            val actionButtonHit = x >= actionButtonX && 
                                 x <= actionButtonX + actionButton.width &&
                                 y >= actionButtonY && 
                                 y <= actionButtonY + actionButton.height
            
            val againTutorialHit = x >= againTutorialX && 
                                  x <= againTutorialX + againTutorial.width &&
                                  y >= againTutorialY && 
                                  y <= againTutorialY + againTutorial.height
            
            // Eğer tıklanabilir elementlere tıklanıyorsa consume et, değilse GuidePanel'e ilet
            actionButtonHit || againTutorialHit
        }
        
        android.util.Log.d("LessonAdapter", "BottomSheet interactions disabled")
    }

}




