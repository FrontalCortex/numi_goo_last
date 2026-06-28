package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.widget.Button
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class LessonAdapter(
    private val context: Context,
    private val items: MutableList<LessonItem>,
    private val onPartChange: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val LESSON_TOUCH_BLOCKER_TAG = "lesson_action_touch_blocker"
        private val lastSeenFilledSegments = mutableMapOf<String, Int>()
        private val playedFinalGoldAnimationKeys = mutableSetOf<String>()
    }

    interface OnProgressUpdateListener {
        fun updateProgress(position: Int, progress: Int)
    }
    private lateinit var raceAdapter: RaceAdapter // Adapter'ı tanımla
    private var progressUpdateListener: OnProgressUpdateListener? = null
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /** bind/layout sırasında notifyItemChanged patlamasın diye tutulur. */
    private var attachedRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (attachedRecyclerView === recyclerView) {
            attachedRecyclerView = null
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    /**
     * RecyclerView layout/bind döngüsü içindeyken [notifyItemChanged] IllegalStateException fırlatır;
     * bir sonraki frame'e ertelenir ([persistFinalGoldVisualState] → [LessonManager.updateLessonItem] zinciri).
     */
    private fun notifyItemChangedSafe(position: Int) {
        if (position !in items.indices) return
        val rv = attachedRecyclerView
        if (rv != null) {
            rv.post {
                if (position in items.indices) {
                    notifyItemChanged(position)
                }
            }
        } else {
            notifyItemChanged(position)
        }
    }
    
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
        // Önce internet ve login durumunu kontrol et
        val activity = context as Activity
        if (!activity.isOnline()) {
            (activity as? MainActivity)?.showOfflineFragment()
            return
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            val appCompatActivity = activity as? AppCompatActivity ?: return
            SessionDeviceManager.requireLoggedInAndSingleDevice(appCompatActivity) {
                showLessonBottomSheet(item, position)
            }
            return
        }

        // Activity'deki view'ları bul
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
                    if (globalPartId in setOf(4, 5)) {
                        recordLayout.visibility = View.GONE
                    } else {
                        recordLayout.setBackgroundResource(R.drawable.record_background)
                        // Abonelik durumuna göre buton metnini ayarla
                        record.text = "Rekor: ${item.record}"
                        fireAnim.visibility = View.VISIBLE
                    }
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

        // Rekor alanı: GuidePanel kapalıyken liderlik tablosu (RecordFragment).
        // Guide açıkken tıklanabilir kalmalı (son adım hedefi); listener sadece guide kapalıyken.
        recordLayout.setOnClickListener(null)
        if (item.type == LessonItem.TYPE_CHEST && item.stepIsFinish && item.record != null &&
            globalPartId !in setOf(4, 5)
        ) {
            recordLayout.isClickable = true
            if (!isGuidePanelVisible) {
                recordLayout.setOnClickListener {
                    blockAllTouchesForActionTransition()
                    val act = context as FragmentActivity
                    val openRecord = {
                        act.findViewById<View>(R.id.abacusFragmentContainer).visibility = View.VISIBLE
                        act.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                android.R.anim.slide_in_left,
                                android.R.anim.slide_out_right,
                            )
                            .replace(
                                R.id.abacusFragmentContainer,
                                RecordFragment.newInstance(globalPartId, position, item.title),
                            )
                            .addToBackStack(null)
                            .commitAllowingStateLoss()
                    }
                    (act as? MainActivity)?.runAbacusOverlayTransaction("record") { openRecord() }
                        ?: openRecord()
                    behavior.isHideable = true
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        } else {
            recordLayout.isClickable = false
            if (globalPartId in setOf(4, 5)) {
                recordLayout.visibility = View.GONE
            }
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

            (activity as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(true)
                    activity.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, TutorialFragment(item.tutorialNumber))
                        .addToBackStack(null)
                        .commitAllowingStateLoss()




        }
        // Button tıklama
        actionButton.setOnClickListener {
            // Tıklamanın ilk anından itibaren 0.4 sn tüm ekranı kilitle.
            blockAllTouchesForActionTransition()
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
        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        val activity = context as FragmentActivity
        (activity as? MainActivity)?.dismissMapLessonOverlayChrome()
        val main = activity as? MainActivity
        val startLesson = {
            val fragmentContainer = activity.findViewById<View>(R.id.abacusFragmentContainer)
            val fm = activity.supportFragmentManager
            fm.executePendingTransactions()
            val existingOverlay = fm.findFragmentById(R.id.abacusFragmentContainer)
            if (existingOverlay is RecordFragment) {
                if (fm.backStackEntryCount > 0) {
                    fm.popBackStack()
                    fm.executePendingTransactions()
                } else {
                    fm.beginTransaction().remove(existingOverlay).commitNowAllowingStateLoss()
                    fm.executePendingTransactions()
                }
            }
            fragmentContainer.visibility = View.VISIBLE

            val slideIn = android.R.anim.slide_in_left
            val slideOut = android.R.anim.slide_out_right
            item.mapFragmentIndex.also { mapFragmentStepIndex = it!! }
            item.startStepNumber.also { lessonStep = it!! }
            if (item.isBlinding == true) {
                if (item.tutorialIsFinish) {
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, BlindingLessonFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                } else {
                    (main as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(true)
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, TutorialFragment(item.tutorialNumber))
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                }
            } else {
                if (item.tutorialIsFinish) {
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, AbacusFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                } else {
                    (main as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(true)
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, TutorialFragment(item.tutorialNumber))
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                }
            }
        }
        main?.runAbacusOverlayTransaction("continueWithLesson") { startLesson() } ?: startLesson()
    }

    private fun blockAllTouchesForActionTransition() {
        val activity = context as? Activity ?: return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.findViewWithTag<View>(LESSON_TOUCH_BLOCKER_TAG)?.let { content.removeView(it) }

        val blocker = View(activity).apply {
            tag = LESSON_TOUCH_BLOCKER_TAG
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
            elevation = 1000f
        }
        content.addView(blocker)
        blocker.postDelayed({
            content.findViewWithTag<View>(LESSON_TOUCH_BLOCKER_TAG)?.let { content.removeView(it) }
        }, 400)
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
        scrimView.alpha = 0.5f

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
        raceRecyclerView.layoutManager = LinearLayoutManager(context)
        val behavior = BottomSheetBehavior.from(raceContentLayout)

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

        // Global initialize kullanma: _lessonItems + realtime listener harita adapter'ını yeniler.
        GlobalLessonData.loadLessonItemsForPart(context, racePartId) { raceItems ->
            (context as? Activity)?.runOnUiThread {
                raceAdapter = RaceAdapter(
                    context,
                    raceItems = raceItems.toMutableList(),
                    { raceItem, clickedIndex ->
                        showRaceLessonBottomSheet(raceItem, clickedIndex, racePartId)
                    },
                    onPartChange = { newPartId ->
                        GlobalLessonData.loadLessonItemsForPart(context, newPartId) { partItems ->
                            (context as? Activity)?.runOnUiThread {
                                raceAdapter.raceUpdateItems(partItems.toMutableList())
                            }
                        }
                    },
                )
                LessonManager.setRaceAdapter(raceAdapter)
                raceRecyclerView.adapter = raceAdapter

                coordinatorLayout.addView(racePanelView)
                behavior.isHideable = true
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
                racePanelView.post {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    fun isRacePanelOpen(): Boolean {
        val activity = context as? Activity ?: return false
        val coordinator = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        return coordinator?.findViewWithTag<View>("race_panel") != null
    }

    private fun formatRaceTimePeriodDescription(timePeriodMs: Long?): String {
        val seconds = (timePeriodMs ?: 1000L) / 1000.0
        val secondsText = if (seconds % 1.0 == 0.0) {
            seconds.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", seconds).replace('.', ',')
        }
        return "Sayı gösterilme periyodu $secondsText saniye"
    }

    private fun applyRaceKeyUnlockButtonStyle(button: MaterialButton) {
        val res = context.resources
        val buttonWidth = res.getDimensionPixelSize(R.dimen.race_sheet_key_button_width)
        val buttonHeight = res.getDimensionPixelSize(R.dimen.race_sheet_key_button_height)
        val lp = button.layoutParams as LinearLayout.LayoutParams
        lp.width = buttonWidth
        lp.height = buttonHeight
        lp.gravity = Gravity.CENTER_HORIZONTAL
        button.layoutParams = lp
        button.minWidth = 0
        button.minHeight = 0
        button.text = ""
        button.isAllCaps = false
        button.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        button.backgroundTintList = ContextCompat.getColorStateList(context, R.color.button_enabled)
        button.textAlignment = View.TEXT_ALIGNMENT_CENTER
        button.icon = ContextCompat.getDrawable(context, R.drawable.key)
        button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        button.iconPadding = 0
        button.iconSize = (32 * context.resources.displayMetrics.density).toInt()
        button.iconTint = null
        button.cornerRadius = (15 * context.resources.displayMetrics.density).toInt()
        button.elevation = 0f
        button.setPadding(0, 0, 0, 0)
    }

    private fun showRaceFastForwardPanel(
        raceItem: LessonItem,
        clickedIndex: Int,
        racePartId: Int,
        onLessonStart: () -> Unit,
    ) {
        val activity = context as Activity
        val main = activity as? MainActivity
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.panel_race_fast_forward)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)

        val closeButton = dialog.findViewById<View>(R.id.raceFastForwardClose)
        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<MaterialButton>(R.id.raceFastForwardDiamond).setOnClickListener {
            if (main?.spendKeys(1) != true) {
                Toast.makeText(
                    context,
                    R.string.daily_question_insufficient_keys,
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            onLessonStart()
        }
        dialog.setOnDismissListener {
            dialog.findViewById<LottieAnimationView>(R.id.raceFastForwardAnimation)?.cancelAnimation()
        }
        dialog.show()
    }

    private fun applyRaceStartButtonStyle(button: MaterialButton) {
        val lp = button.layoutParams as LinearLayout.LayoutParams
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.gravity = Gravity.NO_GRAVITY
        button.layoutParams = lp
        button.icon = null
        button.minWidth = 0
        button.cornerRadius = (15 * context.resources.displayMetrics.density).toInt()
        button.elevation = 0f
    }

    private fun restoreRacePanelScrimIfNeeded(activity: Activity, scrimView: View) {
        if (!isRacePanelOpen()) {
            scrimView.setOnClickListener(null)
            return
        }
        val coordinator = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        val racePanel = coordinator.findViewWithTag<View>("race_panel") ?: return
        val closeButton = racePanel.findViewById<TextView>(R.id.closeButton)
        scrimView.setOnClickListener { closeButton.performClick() }
    }

    private fun showRaceLessonBottomSheet(raceItem: LessonItem, clickedIndex: Int, racePartId: Int) {
        val activity = context as Activity
        val coordinatorLayout = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        val scrimView = activity.findViewById<View>(R.id.scrimView)
        val isLockedRace = raceItem.raceBusyLevel == 2

        coordinatorLayout.findViewWithTag<View>("race_lesson_bottom_sheet")?.let {
            coordinatorLayout.removeView(it)
        }

        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.race_lesson_bottom_sheet, coordinatorLayout, false)
        bottomSheetView.tag = "race_lesson_bottom_sheet"

        val titleText = bottomSheetView.findViewById<TextView>(R.id.raceLessonTitle)
        val descriptionText = bottomSheetView.findViewById<TextView>(R.id.raceLessonDescription)
        val actionButton = bottomSheetView.findViewById<MaterialButton>(R.id.raceActionButton)
        val bottomSheetLayout = bottomSheetView.findViewById<LinearLayout>(R.id.raceBottomSheetLayout)

        titleText.text = raceItem.raceTitle ?: raceItem.title
        descriptionText.text = formatRaceTimePeriodDescription(raceItem.timePeriod)

        if (isLockedRace) {
            titleText.setTextColor(ContextCompat.getColor(context, R.color.lesson_locked))
            descriptionText.setTextColor(ContextCompat.getColor(context, R.color.lesson_locked))
            bottomSheetLayout.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.background_color)
            applyRaceKeyUnlockButtonStyle(actionButton)
        } else {
            titleText.setTextColor(ContextCompat.getColor(context, R.color.lesson_completed))
            descriptionText.setTextColor(ContextCompat.getColor(context, R.color.lesson_completed))
            bottomSheetLayout.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.panel_background)
            applyRaceStartButtonStyle(actionButton)
            when (raceItem.raceBusyLevel) {
                0 -> {
                    actionButton.text = "Tekrar dene"
                    actionButton.isAllCaps = false
                    actionButton.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    actionButton.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.lesson_completed)
                    actionButton.setTextColor(ContextCompat.getColor(context, R.color.panel_background))
                }
                else -> {
                    actionButton.text = "                           BAŞLAT"
                    actionButton.isAllCaps = true
                    actionButton.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                    actionButton.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.lesson_completed)
                    actionButton.setTextColor(ContextCompat.getColor(context, R.color.panel_background))
                    actionButton.icon = ContextCompat.getDrawable(context, R.drawable.lighting__1_)
                    actionButton.iconGravity = MaterialButton.ICON_GRAVITY_START
                    actionButton.iconPadding = (8 * context.resources.displayMetrics.density).toInt()
                    actionButton.iconTint = null
                }
            }
        }
        actionButton.isEnabled = true

        val behavior = BottomSheetBehavior.from(bottomSheetLayout)

        val dismissSheet = {
            behavior.isHideable = true
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        scrimView.visibility = View.VISIBLE
        scrimView.alpha = 0f
        scrimView.animate()
            .alpha(0.5f)
            .setDuration(300)
            .start()
        // Yarış paneli scrim'in üstünde olduğu için önce scrim'i öne al (lesson_bottom_sheet gibi karartma)
        scrimView.bringToFront()
        coordinatorLayout.addView(bottomSheetView)

        scrimView.setOnClickListener { dismissSheet() }
        bottomSheetView.setOnClickListener { dismissSheet() }
        bottomSheetLayout.setOnClickListener { }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    coordinatorLayout.removeView(bottomSheetView)
                    if (isRacePanelOpen()) {
                        coordinatorLayout.findViewWithTag<View>("race_panel")?.bringToFront()
                        restoreRacePanelScrimIfNeeded(activity, scrimView)
                    } else {
                        scrimView.animate()
                            .alpha(0f)
                            .setDuration(100)
                            .withEndAction { scrimView.visibility = View.GONE }
                            .start()
                        scrimView.setOnClickListener(null)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                scrimView.alpha = 0.5f * (slideOffset + 1)
            }
        })

        actionButton.setOnClickListener {
            if (isLockedRace) {
                showRaceFastForwardPanel(raceItem, clickedIndex, racePartId) {
                    dismissSheet()
                    onRaceStartClicked(raceItem, clickedIndex, racePartId)
                }
                return@setOnClickListener
            }
            dismissSheet()
            onRaceStartClicked(raceItem, clickedIndex, racePartId)
        }

        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetView.post {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun onRaceStartClicked(raceItem: LessonItem, clickedIndex: Int, racePartId: Int) {
        val activity = context as FragmentActivity
        val main = activity as? MainActivity

        val startRaceLesson = {
            val fragmentContainer = activity.findViewById<View>(R.id.abacusFragmentContainer)
            val fm = activity.supportFragmentManager
            fm.executePendingTransactions()
            fragmentContainer.visibility = View.VISIBLE

            raceItem.mapFragmentIndex?.let { mapFragmentStepIndex = it }
            raceItem.startStepNumber?.let { lessonStep = it }

            val slideIn = android.R.anim.slide_in_left
            val slideOut = android.R.anim.slide_out_right
            fm.beginTransaction()
                .setCustomAnimations(slideIn, slideOut)
                .replace(R.id.abacusFragmentContainer, BlindingLessonFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        GlobalLessonData.initialize(context, racePartId) {
            (activity as? Activity)?.runOnUiThread {
                main?.runAbacusOverlayTransaction("onRaceStartClicked") { startRaceLesson() }
                    ?: startRaceLesson()
            }
        }
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
                    notifyItemChangedSafe(position)
                }
            }
        }
    }

    fun updateLessonItem(position: Int, newItem: LessonItem) {
        if (position in items.indices) {
            items[position] = newItem
            notifyItemChangedSafe(position)
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
                (itemView.context as? MainActivity)?.requireOnlineAndLoggedInOrLogin {
                    // Butona tıklandığında item'in partId'sini onPartChange callback'ine gönder
                    globalPartId = item.partId!!
                    item.partId?.let { partId ->
                        onPartChange(partId)
                    }
                    sectionTitle.text = item.sectionTitle
                    sectionDescription.text = item.sectionDescription
                }
            }
        }
    }
    inner class BackPartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fastForwardButton: Button = itemView.findViewById(R.id.fastForwardButton)
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        fun bind(item: LessonItem) {
            fastForwardButton.setOnClickListener {
                (itemView.context as? MainActivity)?.requireOnlineAndLoggedInOrLogin {
                    globalPartId = item.partId!!
                    onPartChange(item.partId!!)
                }
            }

            sectionTitle.text = item.sectionTitle
        }
    }

    private enum class ChestStarSlot {
        YellowOn,
        LightGrayOn,
        Off,
    }

    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chestStarLightGrayFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, R.color.chest_star_light_gray),
            PorterDuff.Mode.SRC_IN,
        )

        private val lessonIcon: ImageView = itemView.findViewById(R.id.lessonIcon)
        private val chestStarsRow: LinearLayout = itemView.findViewById(R.id.chestStarsRow)
        private val chestStar1: ImageView = itemView.findViewById(R.id.chestStar1)
        private val chestStar2: ImageView = itemView.findViewById(R.id.chestStar2)
        private val chestStar3: ImageView = itemView.findViewById(R.id.chestStar3)
        private val lessonCard: CardView = itemView.findViewById(R.id.lessonCard)
        private val progressBar: CircleProgressBar = itemView.findViewById(R.id.progressBar)
        private val lessonGoldShinePrimary: View = itemView.findViewById(R.id.lessonGoldShinePrimary)
        private val lessonGoldShineSecondary: View = itemView.findViewById(R.id.lessonGoldShineSecondary)
        private var progressBreathingAnimator: ValueAnimator? = null
        private var progressIncreaseAnimator: ValueAnimator? = null
        private var progressIncreaseStepCount: Int = 0
        private var progressIncreaseTargetFilled: Int = 0

        private fun cancelProgressIncreaseAnimation(applyFinalState: Boolean = true) {
            val animator = progressIncreaseAnimator ?: return
            if (applyFinalState && progressIncreaseStepCount > 0) {
                progressBar.setSegmentState(
                    progressIncreaseStepCount,
                    progressIncreaseTargetFilled.coerceIn(0, progressIncreaseStepCount),
                )
            }
            animator.cancel()
            progressIncreaseAnimator = null
        }

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
        private fun applyStepSegments(item: LessonItem) {
            val safeStepCount = item.stepCount.coerceAtLeast(1)
            val completedSteps = item.stepCompletionStatus.count { it }
            val filledSegments = if (item.stepIsFinish) safeStepCount else completedSteps
            progressBar.setSegmentGapAngle(16f)
            progressBar.setSegmentState(
                segmentCount = safeStepCount,
                completedSegments = filledSegments.coerceIn(0, safeStepCount),
            )
        }

        private fun lessonProgressKey(item: LessonItem): String {
            val stableId = item.id ?: -1
            val part = item.partId ?: -1
            val idx = item.mapFragmentIndex ?: bindingAdapterPosition
            return "${item.type}_${stableId}_${part}_${item.title}_$idx"
        }

        private fun applyPersistentFinalGoldState() {
            lessonCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_center_gold))
            progressBar.setProgressColor(ContextCompat.getColor(context, R.color.lesson_ring_gold))
            progressBar.setBackgroundRingColor(ContextCompat.getColor(context, R.color.lesson_ring_gold))
            progressBar.scaleX = 0.35f
            progressBar.scaleY = 0.35f
            progressBar.alpha = 0f
            lessonGoldShinePrimary.alpha = 0.75f
            lessonGoldShineSecondary.alpha = 0.55f
        }

        private fun resetGoldEffectVisuals(baseCardColor: Int) {
            lessonCard.setCardBackgroundColor(baseCardColor)
            progressBar.scaleX = 1f
            progressBar.scaleY = 1f
            progressBar.alpha = 1f
            lessonGoldShinePrimary.alpha = 0f
            lessonGoldShineSecondary.alpha = 0f
            lessonGoldShinePrimary.translationX = 0f
            lessonGoldShineSecondary.translationX = 0f
        }

        private fun persistFinalGoldVisualState(item: LessonItem, key: String) {
            if (item.finalGoldVisualUnlocked) return
            item.finalGoldVisualUnlocked = true
            playedFinalGoldAnimationKeys.add(key)
            val index = item.mapFragmentIndex ?: bindingAdapterPosition
            if (index in items.indices) {
                LessonManager.updateLessonItem(context, index, item.copy(finalGoldVisualUnlocked = true))
            }
        }

        private fun playFinalGoldMergeAnimation(item: LessonItem, baseCardColor: Int, key: String) {
            if (playedFinalGoldAnimationKeys.contains(key) || item.finalGoldVisualUnlocked) {
                persistFinalGoldVisualState(item, key)
                applyPersistentFinalGoldState()
                return
            }
            stopProgressBreathingAnimation()
            val goldCardColor = ContextCompat.getColor(context, R.color.lesson_center_gold)
            val goldRingColor = ContextCompat.getColor(context, R.color.lesson_ring_gold)
            progressBar.setProgressColor(goldRingColor)
            progressBar.setBackgroundRingColor(goldRingColor)

            val colorAnim = ValueAnimator.ofObject(
                ArgbEvaluator(),
                baseCardColor,
                goldCardColor,
            ).apply {
                duration = 340L
                addUpdateListener { va ->
                    lessonCard.setCardBackgroundColor(va.animatedValue as Int)
                }
            }

            val ringMerge = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(progressBar, View.SCALE_X, 1f, 0.35f),
                    ObjectAnimator.ofFloat(progressBar, View.SCALE_Y, 1f, 0.35f),
                    ObjectAnimator.ofFloat(progressBar, View.ALPHA, 1f, 0f),
                )
                duration = 520L
                interpolator = AccelerateDecelerateInterpolator()
            }

            val shinePrimary = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(lessonGoldShinePrimary, View.ALPHA, 0f, 0.95f, 0.75f),
                    ObjectAnimator.ofFloat(lessonGoldShinePrimary, View.TRANSLATION_X, -8f, 8f),
                )
                duration = 430L
                startDelay = 140L
                interpolator = AccelerateDecelerateInterpolator()
            }
            val shineSecondary = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(lessonGoldShineSecondary, View.ALPHA, 0f, 0.85f, 0.55f),
                    ObjectAnimator.ofFloat(lessonGoldShineSecondary, View.TRANSLATION_X, 7f, -6f),
                )
                duration = 470L
                startDelay = 170L
                interpolator = AccelerateDecelerateInterpolator()
            }

            AnimatorSet().apply {
                playTogether(colorAnim, ringMerge, shinePrimary, shineSecondary)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        persistFinalGoldVisualState(item, key)
                        applyPersistentFinalGoldState()
                    }
                })
                start()
            }
        }

        private fun applyStepSegmentsWithIncreaseAnimation(item: LessonItem, baseCardColor: Int) {
            cancelProgressIncreaseAnimation(applyFinalState = true)
            val safeStepCount = item.stepCount.coerceAtLeast(1)
            val completedSteps = item.stepCompletionStatus.count { it }
            val targetFilled = if (item.stepIsFinish) safeStepCount else completedSteps.coerceIn(0, safeStepCount)
            val key = lessonProgressKey(item)
            progressBar.setSegmentGapAngle(16f)
            if ((playedFinalGoldAnimationKeys.contains(key) || item.finalGoldVisualUnlocked) && targetFilled == safeStepCount) {
                persistFinalGoldVisualState(item, key)
                applyPersistentFinalGoldState()
                lastSeenFilledSegments[key] = targetFilled
                return
            } else {
                resetGoldEffectVisuals(baseCardColor)
            }
            val pending = GlobalValues.pendingLessonProgressAnimations[key]
            val shouldConsumePending = GlobalValues.canConsumePendingLessonProgressAnimations && pending != null
            val previousFilled = when {
                shouldConsumePending -> pending!!.fromFilledSegments.coerceIn(0, safeStepCount)
                pending != null -> pending.fromFilledSegments.coerceIn(0, safeStepCount)
                else -> lastSeenFilledSegments[key]?.coerceIn(0, safeStepCount) ?: targetFilled
            }

            if (targetFilled > previousFilled) {
                if (shouldConsumePending) {
                    progressIncreaseStepCount = safeStepCount
                    progressIncreaseTargetFilled = targetFilled
                    progressBar.setSegmentState(safeStepCount, previousFilled)
                    progressIncreaseAnimator = ValueAnimator.ofFloat(previousFilled.toFloat(), targetFilled.toFloat()).apply {
                        duration = ((targetFilled - previousFilled) * 900L).coerceAtLeast(1800L)
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener { animator ->
                            val current = animator.animatedValue as Float
                            progressBar.setSegmentProgress(current)
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                progressIncreaseAnimator = null
                                progressBar.setSegmentState(safeStepCount, targetFilled)
                                if (targetFilled == safeStepCount) {
                                    playFinalGoldMergeAnimation(item, baseCardColor, key)
                                }
                            }

                            override fun onAnimationCancel(animation: Animator) {
                                progressIncreaseAnimator = null
                                progressBar.setSegmentState(safeStepCount, targetFilled)
                            }
                        })
                        start()
                    }
                    GlobalValues.pendingLessonProgressAnimations.remove(key)
                    lastSeenFilledSegments[key] = targetFilled
                } else if (pending != null) {
                    progressBar.setSegmentState(safeStepCount, previousFilled)
                    lastSeenFilledSegments[key] = previousFilled
                } else {
                    progressBar.setSegmentState(safeStepCount, targetFilled)
                    lastSeenFilledSegments[key] = targetFilled
                }
            } else {
                progressBar.setSegmentState(safeStepCount, targetFilled)
                if (shouldConsumePending) {
                    GlobalValues.pendingLessonProgressAnimations.remove(key)
                }
                if (
                    shouldConsumePending &&
                    targetFilled == safeStepCount &&
                    !playedFinalGoldAnimationKeys.contains(key) &&
                    !item.finalGoldVisualUnlocked
                ) {
                    playFinalGoldMergeAnimation(item, baseCardColor, key)
                }
                lastSeenFilledSegments[key] = targetFilled
            }
        }

        private fun startProgressBreathingAnimation() {
            progressBreathingAnimator?.cancel()
            progressBar.scaleX = 1f
            progressBar.scaleY = 1f
            progressBreathingAnimator = ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
                duration = 2200L
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val scale = animation.animatedValue as Float
                    progressBar.scaleX = scale
                    progressBar.scaleY = scale
                }
                start()
            }
        }

        fun stopProgressBreathingAnimation() {
            progressBreathingAnimator?.cancel()
            progressBreathingAnimator = null
            progressBar.scaleX = 1f
            progressBar.scaleY = 1f
            cancelProgressIncreaseAnimation(applyFinalState = true)
        }

        private fun applyChestStarSlot(iv: ImageView, slot: ChestStarSlot) {
            when (slot) {
                ChestStarSlot.YellowOn -> {
                    iv.setImageResource(R.drawable.star_on_ic)
                    iv.colorFilter = null
                }
                ChestStarSlot.LightGrayOn -> {
                    iv.setImageResource(R.drawable.star_on_ic)
                    iv.colorFilter = chestStarLightGrayFilter
                }
                ChestStarSlot.Off -> {
                    iv.setImageResource(R.drawable.star_off_ic)
                    iv.colorFilter = null
                }
            }
        }

        private fun setChestStarsRowThreeStarMode(three: Boolean) {
            if (three) {
                chestStarsRow.gravity = Gravity.CENTER_VERTICAL
                listOf(chestStar1, chestStar2, chestStar3).forEach { iv ->
                    iv.scaleX = 1f
                    iv.scaleY = 1f
                }
                chestStar2.visibility = View.VISIBLE
                chestStar3.visibility = View.VISIBLE
                listOf(chestStar1, chestStar2, chestStar3).forEach { iv ->
                    val lp = iv.layoutParams as LinearLayout.LayoutParams
                    lp.width = 0
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                    lp.weight = 1f
                    iv.layoutParams = lp
                }
            } else {
                chestStar2.visibility = View.GONE
                chestStar3.visibility = View.GONE
                chestStarsRow.gravity = Gravity.CENTER
                val singleStarSize =
                    itemView.resources.getDimensionPixelSize(R.dimen.map_lesson_icon_size)
                val lp1 = chestStar1.layoutParams as LinearLayout.LayoutParams
                lp1.width = singleStarSize
                lp1.height = singleStarSize
                lp1.weight = 0f
                chestStar1.layoutParams = lp1
                chestStar1.scaleX = 1f
                chestStar1.scaleY = 1f
            }
        }

        private fun bindChestStarsRow(tierResId: Int, isCompleted: Boolean) {
            when (tierResId) {
                0, R.drawable.chest_stars_tier0 -> {
                    setChestStarsRowThreeStarMode(false)
                    val slot = if (isCompleted) ChestStarSlot.YellowOn else ChestStarSlot.LightGrayOn
                    applyChestStarSlot(chestStar1, slot)
                }
                R.drawable.chest_stars_tier3 -> {
                    setChestStarsRowThreeStarMode(true)
                    val t = Triple(
                        ChestStarSlot.YellowOn,
                        ChestStarSlot.YellowOn,
                        ChestStarSlot.YellowOn,
                    )
                    applyChestStarSlot(chestStar1, t.first)
                    applyChestStarSlot(chestStar2, t.second)
                    applyChestStarSlot(chestStar3, t.third)
                }
                R.drawable.chest_stars_tier2 -> {
                    setChestStarsRowThreeStarMode(true)
                    applyChestStarSlot(chestStar1, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar2, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar3, ChestStarSlot.Off)
                }
                R.drawable.chest_stars_tier1 -> {
                    setChestStarsRowThreeStarMode(true)
                    applyChestStarSlot(chestStar1, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar2, ChestStarSlot.Off)
                    applyChestStarSlot(chestStar3, ChestStarSlot.Off)
                }
                R.drawable.cup_ic3 -> {
                    setChestStarsRowThreeStarMode(true)
                    applyChestStarSlot(chestStar1, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar2, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar3, ChestStarSlot.YellowOn)
                }
                R.drawable.cup_ic2 -> {
                    setChestStarsRowThreeStarMode(true)
                    applyChestStarSlot(chestStar1, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar2, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar3, ChestStarSlot.Off)
                }
                R.drawable.cup_ic -> {
                    setChestStarsRowThreeStarMode(true)
                    applyChestStarSlot(chestStar1, ChestStarSlot.YellowOn)
                    applyChestStarSlot(chestStar2, ChestStarSlot.Off)
                    applyChestStarSlot(chestStar3, ChestStarSlot.Off)
                }
                else -> {
                    setChestStarsRowThreeStarMode(false)
                    val slot = if (isCompleted) ChestStarSlot.YellowOn else ChestStarSlot.LightGrayOn
                    applyChestStarSlot(chestStar1, slot)
                }
            }
        }

        fun bind(item: LessonItem) {
            fun applyCupIcon() {
                var resId = item.stepCupIcon
                if (resId == 0) {
                    resId = R.drawable.chest_stars_tier0
                    item.stepCupIcon = resId
                }
                val normalized = when (resId) {
                    R.drawable.chest_stars_tier0,
                    R.drawable.chest_stars_tier1,
                    R.drawable.chest_stars_tier2,
                    R.drawable.chest_stars_tier3,
                    R.drawable.cup_ic,
                    R.drawable.cup_ic2,
                    R.drawable.cup_ic3 -> resId
                    else -> {
                        item.stepCupIcon = R.drawable.chest_stars_tier0
                        R.drawable.chest_stars_tier0
                    }
                }
                bindChestStarsRow(normalized, item.isCompleted)
            }

            when (item.type) {
                LessonItem.TYPE_CHEST -> {
                    if (adapterPosition == MarathonGuideStore.firstMarathonLessonIndex()) {
                        LessonProgressDiag.logItem(
                            "LessonAdapter.bind",
                            GlobalLessonData.globalPartId,
                            adapterPosition,
                            item,
                            "marathonCardUI",
                        )
                    }
                    val backgroundColor = if (item.isCompleted) {
                        ContextCompat.getColor(context, R.color.lesson_center_blue)
                    } else {
                        ContextCompat.getColor(context, R.color.lesson_locked)
                    }
                    lessonCard.setCardBackgroundColor(backgroundColor)
                    progressBar.setProgressColor(ContextCompat.getColor(context, R.color.lesson_ring_active_blue))
                    progressBar.setBackgroundRingColor(ContextCompat.getColor(context, R.color.lesson_ring_inactive_dark))

                    lessonCard.setOnClickListener {
                        // TYPE_CHEST kartına tıklandığında da internet + login kontrolü yap
                        (itemView.context as? MainActivity)?.requireOnlineAndLoggedInOrLogin {
                            showLessonBottomSheet(item, adapterPosition)
                        }
                    }

                    lessonIcon.visibility = View.GONE
                    chestStarsRow.visibility = View.VISIBLE
                    // Bitmemişse 3 kapalı yıldız; bitmişse stepCupIcon (tier0–3).
                    if (item.stepIsFinish) {
                        applyCupIcon()
                    } else {
                        item.stepCupIcon = R.drawable.chest_stars_tier0
                        bindChestStarsRow(R.drawable.chest_stars_tier0, item.isCompleted)
                    }
                    applyStepSegmentsWithIncreaseAnimation(item, backgroundColor)
                    val key = lessonProgressKey(item)
                    if (!((playedFinalGoldAnimationKeys.contains(key) || item.finalGoldVisualUnlocked) && item.stepIsFinish)) {
                        startProgressBreathingAnimation()
                    }

                }

                LessonItem.TYPE_LESSON -> {
                    lessonIcon.visibility = View.VISIBLE
                    chestStarsRow.visibility = View.GONE
                    lessonIcon.setImageResource(R.drawable.book_icon)
                    val backgroundColor = if (item.isCompleted) {
                        ContextCompat.getColor(context, R.color.lesson_center_blue)
                    } else {
                        ContextCompat.getColor(context, R.color.lesson_locked)
                    }
                    lessonCard.setCardBackgroundColor(backgroundColor)
                    progressBar.setProgressColor(ContextCompat.getColor(context, R.color.lesson_ring_active_blue))
                    progressBar.setBackgroundRingColor(ContextCompat.getColor(context, R.color.lesson_ring_inactive_dark))
                    lessonCard.setOnClickListener {
                        // TYPE_LESSON kartına tıklandığında da internet + login kontrolü yap
                        (itemView.context as? MainActivity)?.requireOnlineAndLoggedInOrLogin {
                            showLessonBottomSheet(item, adapterPosition)
                        }
                    }

                    applyStepSegmentsWithIncreaseAnimation(item, backgroundColor)
                    val key = lessonProgressKey(item)
                    if (!((playedFinalGoldAnimationKeys.contains(key) || item.finalGoldVisualUnlocked) && item.stepIsFinish)) {
                        startProgressBreathingAnimation()
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

    /** Kilitli race bayrağı: orijinal renk ayrımını koruyan gri tonlar ([BadgeFragment] kilitli rozet mantığına yakın). */
    private fun applyRaceFlagLockedTone(icon: ImageView) {
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        val coolGray = ColorMatrix().apply {
            setScale(0.72f, 0.76f, 0.80f, 1f)
        }
        matrix.postConcat(coolGray)
        icon.colorFilter = ColorMatrixColorFilter(matrix)
        icon.imageAlpha = 230
    }

    private fun applyRaceFlagUnlockedTone(icon: ImageView) {
        icon.clearColorFilter()
        icon.imageAlpha = 255
    }

    inner class RaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lessonIcon: ImageView = itemView.findViewById(R.id.lessonIcon)
        private val lessonCard: CardView = itemView.findViewById(R.id.lessonCard)
        private val progressBar: CircleProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind(item: LessonItem) {
            progressBar.visibility = View.GONE
            lessonIcon.setImageResource(R.drawable.flag_ic)
            if (item.isCompleted) {
                applyRaceFlagUnlockedTone(lessonIcon)
            } else {
                applyRaceFlagLockedTone(lessonIcon)
            }

            val backgroundColor = if (item.isCompleted) {
                ContextCompat.getColor(context, R.color.lesson_completed)
            } else {
                ContextCompat.getColor(context, R.color.lesson_locked)
            }
            lessonCard.setCardBackgroundColor(backgroundColor)

            lessonCard.setOnClickListener {
                if (!item.isCompleted) {
                    Toast.makeText(
                        context,
                        R.string.race_unlock_complete_all_lessons,
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@setOnClickListener
                }
                (itemView.context as? MainActivity)?.requireOnlineAndLoggedInOrLogin {
                    showRacePanel(item, adapterPosition)
                }
            }
        }
    }
    fun updateItems(newItems: List<LessonItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is LessonViewHolder) {
            holder.stopProgressBreathingAnimation()
        }
        super.onViewRecycled(holder)
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




