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
import android.graphics.drawable.GradientDrawable
import androidx.core.view.drawToBitmap
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
import com.google.android.material.card.MaterialCardView
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val LESSON_TOUCH_BLOCKER_TAG = "lesson_action_touch_blocker"
        // Ders paneli açıkken üst paneli ve alt menüyü örten karartmalar (addChromeDims)
        const val CHROME_DIM_TAG = "lesson_panel_chrome_dim"
        // Ders paneli karartma oranı: harita (scrimView), üst panel/alt menü ve sistem şeritleri
        // hep aynı oranda kararmalı, yoksa ekran parça parça görünüyor.
        private const val LESSON_PANEL_DIM_ALPHA = 0.7f

        /** Ders paneli açılmadan önceki üst ve alt sistem şeridi renkleri (dimSystemBars). */
        var savedBarColors: IntArray? = null
        private val lastSeenFilledSegments = mutableMapOf<String, Int>()
        private val playedFinalGoldAnimationKeys = mutableSetOf<String>()

        /**
         * Oynamakta olan ilerleme artışları, kart anahtarına göre.
         *
         * ## Neden ViewHolder'da değil
         * Animasyon eskiden yalnızca ViewHolder'ın animator'ında yaşıyordu ve listenin
         * herhangi bir yenilenmesi onu öldürüyordu: `notifyDataSetChanged` (stabil id yok)
         * bütün holder'ları geri dönüşüme atıyor → [onViewRecycled] animasyonu son hâline
         * atlatıyor; `notifyItemChanged` ise değişim animasyonu için kartı YENİ bir holder'a
         * bağlıyor. Haritaya dönüşte liste 2 sn içinde birkaç kez yenileniyor (dönüş, onResume,
         * kilit açılan sıradaki ders, Firestore senkronu) ve kullanıcı çubuğu dolmuş görüyordu.
         *
         * Başlangıç anı burada tutulduğu için kart hangi holder'a bağlanırsa bağlansın
         * animasyon KALDIĞI YERDEN sürüyor.
         */
        private data class RunningProgressIncrease(
            val fromFilled: Int,
            val toFilled: Int,
            val stepCount: Int,
            val startedAtMs: Long,
            val durationMs: Long,
        )
        private val runningProgressIncreases = mutableMapOf<String, RunningProgressIncrease>()

        private fun callerTrace(): String =
            Thread.currentThread().stackTrace
                .drop(4)
                .take(5)
                .joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
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

        // Önceki panel açıksa kaldır.
        coordinatorLayout.findViewWithTag<View>(LessonPanel.TAG)?.let {
            coordinatorLayout.removeView(it)
        }

        // Panel (lesson_popover): tıklanan karta bağlı, ekranla aynı zeminli, durum renginde çerçeveli.
        val panelRoot = LayoutInflater.from(context)
            .inflate(R.layout.lesson_popover, coordinatorLayout, false)
        panelRoot.tag = LessonPanel.TAG

        val panelCard = panelRoot.findViewById<LinearLayout>(R.id.lessonPanelCard)
        val panelArrow = panelRoot.findViewById<PanelPointerView>(R.id.lessonPanelArrow)
        val cardSnapshot = panelRoot.findViewById<ImageView>(R.id.lessonPanelCardSnapshot)
        val kindText = panelRoot.findViewById<TextView>(R.id.lessonKindText)
        val panelIcon = panelRoot.findViewById<ImageView>(R.id.lessonPanelIcon)
        val titleText = panelRoot.findViewById<TextView>(R.id.lessonTitle)
        val stepRow = panelRoot.findViewById<View>(R.id.lessonPanelStepRow)
        val stepText = panelRoot.findViewById<TextView>(R.id.lessonPanelStepText)
        val stepBar = panelRoot.findViewById<SegmentBarView>(R.id.lessonPanelStepBar)
        val descriptionText = panelRoot.findViewById<TextView>(R.id.lessonDescription)
        val actionButton = panelRoot.findViewById<MaterialButton>(R.id.actionButton)
        val againTutorial = panelRoot.findViewById<TextView>(R.id.againTutorial)
        val record = panelRoot.findViewById<TextView>(R.id.recordText)
        val recordLayout = panelRoot.findViewById<LinearLayout>(R.id.recordLayout)

        // Kapatma: panel solup kaldırılıyor, karartma kapanıyor, rekor nabzı duruyor.
        // Dışarıdan (MapFragment, maraton rehberi) LessonPanel.dismiss ile çağrılıyor.
        var dismissed = false
        val dismissPanel: () -> Unit = {
            if (!dismissed) {
                dismissed = true
                (recordLayout.tag as? ValueAnimator)?.cancel()
                recordLayout.tag = null
                panelRoot.animate().alpha(0f).setDuration(140L)
                    .withEndAction { coordinatorLayout.removeView(panelRoot) }
                    .start()
                if (!isGuidePanelVisible) {
                    scrimView.animate().alpha(0f).setDuration(140L)
                        .withEndAction { scrimView.visibility = View.GONE }
                        .start()
                }
                removeChromeDims(activity)
            }
        }
        panelRoot.setTag(R.id.lessonPanelCard, dismissPanel)

        // İçerik ve renkler: açık (mavi), bitmiş (altın), kilitli (gri) — haritadaki kartla aynı dil.
        val isChest = item.type == LessonItem.TYPE_CHEST
        val stepCount = item.stepCount.coerceAtLeast(1)
        val doneSteps = if (item.stepIsFinish) stepCount
            else item.stepCompletionStatus.count { it }.coerceIn(0, stepCount)
        val state = when {
            !item.isCompleted -> PanelState.LOCKED
            item.stepIsFinish -> PanelState.DONE
            else -> PanelState.OPEN
        }
        val palette = panelPalette(state)
        val density = context.resources.displayMetrics.density
        val screenBg = ContextCompat.getColor(context, R.color.background_color)
        panelCard.background = GradientDrawable().apply {
            cornerRadius = 18f * density
            setColor(screenBg)
            setStroke((2f * density).toInt(), palette.border)
        }
        panelArrow.setColors(screenBg, palette.border)
        kindText.text = if (isChest) "ÜNİTE MARATONU" else "DERS"
        kindText.setTextColor(palette.accentText)
        titleText.text = item.title
        titleText.setTextColor(
            if (state == PanelState.LOCKED) ContextCompat.getColor(context, R.color.lesson_card_locked_text)
            else android.graphics.Color.WHITE,
        )
        panelIcon.setImageResource(
            if (isChest && globalPartId in setOf(1, 2, 3, 6)) R.drawable.podium_ic2 else R.drawable.profile_book_ic3
        )
        if (state == PanelState.LOCKED) {
            panelIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            panelIcon.alpha = 0.6f
        }
        stepRow.visibility = if (isChest || state == PanelState.LOCKED) View.GONE else View.VISIBLE
        stepBar.fillColor = palette.fill
        stepBar.trackColor = ContextCompat.getColor(context, R.color.lesson_panel_track)
        stepBar.setSegmentState(stepCount, doneSteps)
        stepText.text = "$doneSteps/$stepCount"
        actionButton.backgroundTintList = android.content.res.ColorStateList.valueOf(palette.buttonBg)
        actionButton.setTextColor(palette.buttonText)

        if (item.isCompleted) {
            againTutorial.visibility =
                if (item.tutorialNumber != 0 && item.tutorialIsFinish) View.VISIBLE else View.GONE
            if (item.stepIsFinish) {
                if (isChest) {
                    // Rekor satırı (4. ve 5. bölümde yok). Maraton rehberinin son adımı bunu gösteriyor.
                    if (globalPartId !in setOf(4, 5)) {
                        recordLayout.visibility = View.VISIBLE
                        record.text = "Rekor: ${item.record ?: "—"}"
                    }
                    actionButton.text = "Tekrar dene"
                } else {
                    actionButton.text = "Gözden geçir"
                }
            } else {
                actionButton.text = if (doneSteps > 0) "Devam et" else "Başla"
            }
            actionButton.isEnabled = true
        } else {
            descriptionText.visibility = View.VISIBLE
            descriptionText.text = "Kilidi açmak için önceki dersleri tamamla."
            againTutorial.visibility = View.GONE
            actionButton.text = "Kilitli"
            actionButton.isEnabled = false
        }

        // Paneli ekle; konumu ölçüldükten sonra (aşağıda) veriliyor.
        coordinatorLayout.addView(panelRoot)

        // GuidePanel açıksa panelin tıklanabilirliğini engelle (rehberin hedefi Rekor satırı)
        if (isGuidePanelVisible) {
            disableBottomSheetInteractions(panelRoot, panelCard, actionButton, againTutorial)
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
                                R.anim.queue_screen_in,
                                R.anim.queue_screen_out,
                                // popEnter/popExit: geri tuşu/X ile kapatılırken de kayarak
                                // kapansın — bunlar verilmezse pop varsayılan olarak animasyonsuz.
                                // android.R.anim'de karşılığı olmadığı için uygulamanın kendi
                                // res/anim/slide_in_right.xml ve slide_out_left.xml'i kullanılıyor.
                                R.anim.queue_screen_in,
                                R.anim.queue_screen_out,
                            )
                            .replace(
                                R.id.abacusFragmentContainer,
                                RecordFragment.newInstance(globalPartId, item.stableId, item.title),
                            )
                            .addToBackStack(null)
                            .commitAllowingStateLoss()
                    }
                    (act as? MainActivity)?.runAbacusOverlayTransaction("record") { openRecord() }
                        ?: openRecord()
                    dismissPanel()
                }
            }
        } else {
            recordLayout.isClickable = false
            if (globalPartId in setOf(4, 5)) {
                recordLayout.visibility = View.GONE
            }
        }

        // Karartma: panel dışına dokununca panel kapanıyor (rehber açıkken karartma yok).
        if (!isGuidePanelVisible) {
            scrimView.visibility = View.VISIBLE
            scrimView.animate()
                .alpha(LESSON_PANEL_DIM_ALPHA)
                .setDuration(200)
                .start()
            scrimView.setOnClickListener { dismissPanel() }
            addChromeDims(activity, dismissPanel)
        } else {
            scrimView.setOnClickListener(null)
        }

        againTutorial.setOnClickListener{
            dismissPanel()
            if (TeacherApprovalGate.blockIfUnapproved(context)) return@setOnClickListener

            // Anlatım bitince doğrudan dersin testine (Abacus/Blinding) geçiyor; enerji
            // harcanmasaydı "Eğitimi tekrarla" testi bedavaya açan bir arka kapı olurdu.
            spendLessonEnergyThen(item, dismissPanel) {
                // Activity'yi bul ve FragmentActivity olarak cast et
                val activity = context as FragmentActivity

                // Fragment container'ı görünür yap
                val fragmentContainer = activity.findViewById<View>(R.id.abacusFragmentContainer)
                fragmentContainer.visibility = View.VISIBLE
                // Sağdan girer, sola çıkar — uygulamadaki bütün ekran geçişleriyle aynı.
                // android.R.anim.slide_in_left SOLDAN getiriyordu, yani zincirin tersi yöne.
                val slideIn = R.anim.queue_screen_in
                val slideOut = R.anim.queue_screen_out
                item.mapFragmentIndex.also { mapFragmentStepIndex = it!! }
                item.startStepNumber.also { lessonStep = it!! }

                (activity as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(true)
                activity.supportFragmentManager.beginTransaction()
                    .setCustomAnimations(slideIn, slideOut, slideIn, slideOut)
                    .replace(R.id.abacusFragmentContainer, TutorialFragment.newInstance(item.tutorialNumber))
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        }
        // Button tıklama
        actionButton.setOnClickListener {
            // Onaysız öğretmenin canı her zaman 0; bu kontrol olmasa çocuğa yazılmış "canın
            // bitti, bekle ya da satın al" uyarısını görürdü. Asıl sebep onay; ona yönlendir.
            if (TeacherApprovalGate.blockIfUnapproved(context)) {
                dismissPanel()
                return@setOnClickListener
            }
            // Tıklamanın ilk anından itibaren 0.4 sn tüm ekranı kilitle.
            blockAllTouchesForActionTransition()
            if (item.isCompleted) {
                spendLessonEnergyThen(item, dismissPanel) {
                    continueWithLesson(item, dismissPanel)
                }
            }
            // Kilitli: düğme zaten devre dışı.
        }

        // Konum: tıklanan kartın altında (yer yoksa üstünde); panel ölçüldükten sonra.
        val recycler = activity.findViewById<RecyclerView>(R.id.lessonsRecyclerView)
        val itemView = recycler?.layoutManager?.findViewByPosition(position)
        val cardView = itemView?.findViewById<View>(R.id.lessonCard) ?: itemView
        // Kartın görüntüsü rehber açıkken de karartmanın üstüne konuyor: rehberin kendi
        // karartması (GuidePanelView) maraton kartını da örtüyor, kart aydınlık kalmalı.
        panelRoot.post {
            placeLessonPanel(
                coordinatorLayout, panelCard, panelArrow, cardSnapshot, cardView,
            )
        }
    }

    /**
     * Ders paneli açıkken karartma yalnızca harita alanını (coordinator) örtüyordu; üstteki
     * para paneli ve alttaki menü aydınlık ve tıklanabilir kalıyordu. Bu ikisinin üstüne de
     * aynı karartma konuyor (pencere kökünde, görünüm sınırlarına); dokununca panel kapanıyor.
     */
    private fun addChromeDims(activity: Activity, onTap: () -> Unit) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        removeChromeDims(activity)
        val contentLoc = IntArray(2).also { content.getLocationInWindow(it) }
        for (id in intArrayOf(R.id.currencyPanel, R.id.bottomNavigationID)) {
            val target = activity.findViewById<View>(id) ?: continue
            if (!target.isShown || target.width == 0 || target.height == 0) continue
            val loc = IntArray(2).also { target.getLocationInWindow(it) }
            val dim = View(activity).apply {
                tag = CHROME_DIM_TAG
                layoutParams = ViewGroup.LayoutParams(target.width, target.height)
                x = (loc[0] - contentLoc[0]).toFloat()
                y = (loc[1] - contentLoc[1]).toFloat()
                setBackgroundColor(android.graphics.Color.BLACK)
                alpha = 0f
                elevation = 999f
                isClickable = true
                setOnClickListener { onTap() }
            }
            content.addView(dim)
            dim.animate().alpha(LESSON_PANEL_DIM_ALPHA).setDuration(200L).start()
        }
        dimSystemBars(activity, dim = true)
    }

    /**
     * Telefonun üst (saat, pil) ve alt (gezinme tuşları) şeritleri de aynı oranda (%70 siyah)
     * koyulaşıyor, panel kapanınca eski renklerine dönüyor; ekran tek parça kararmış görünsün.
     * Eski renkler ilk karartmada saklanıyor. (Android 15+ şerit rengini yok sayıyor; orada etkisiz.)
     */
    private fun dimSystemBars(activity: Activity, dim: Boolean) {
        val window = activity.window ?: return
        if (dim && savedBarColors == null) {
            savedBarColors = intArrayOf(window.statusBarColor, window.navigationBarColor)
        }
        val saved = savedBarColors ?: return
        val evaluator = ArgbEvaluator()
        val fromStatus = window.statusBarColor
        val fromNav = window.navigationBarColor
        val toStatus = if (dim) evaluator.evaluate(LESSON_PANEL_DIM_ALPHA, saved[0], android.graphics.Color.BLACK) as Int else saved[0]
        val toNav = if (dim) evaluator.evaluate(LESSON_PANEL_DIM_ALPHA, saved[1], android.graphics.Color.BLACK) as Int else saved[1]
        if (!dim) savedBarColors = null
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (dim) 200L else 140L
            addUpdateListener { va ->
                val p = va.animatedValue as Float
                window.statusBarColor = evaluator.evaluate(p, fromStatus, toStatus) as Int
                window.navigationBarColor = evaluator.evaluate(p, fromNav, toNav) as Int
            }
            start()
        }
    }

    private fun removeChromeDims(activity: Activity) {
        dimSystemBars(activity, dim = false)
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        while (true) {
            val dim = content.findViewWithTag<View>(CHROME_DIM_TAG) ?: break
            dim.tag = null
            dim.isClickable = false
            dim.animate().alpha(0f).setDuration(140L).withEndAction { content.removeView(dim) }.start()
        }
    }

    private enum class PanelState { OPEN, DONE, LOCKED }

    private class PanelPalette(
        val border: Int,
        val accentText: Int,
        val fill: Int,
        val buttonBg: Int,
        val buttonText: Int,
    )

    /** Ders panelinin durum renkleri (kartlarla aynı: açık mavi, bitmiş altın, kilitli gri). */
    private fun panelPalette(state: PanelState): PanelPalette {
        fun c(id: Int) = ContextCompat.getColor(context, id)
        return when (state) {
            PanelState.OPEN -> PanelPalette(
                border = c(R.color.lesson_center_blue),
                accentText = c(R.color.lesson_panel_open_accent),
                fill = c(R.color.lesson_center_blue),
                buttonBg = c(R.color.lesson_center_blue),
                buttonText = android.graphics.Color.WHITE,
            )
            PanelState.DONE -> PanelPalette(
                border = c(R.color.lesson_center_gold),
                accentText = c(R.color.lesson_center_gold),
                fill = c(R.color.lesson_center_gold),
                buttonBg = c(R.color.lesson_center_gold),
                buttonText = c(R.color.lesson_card_done_text),
            )
            PanelState.LOCKED -> PanelPalette(
                border = c(R.color.lesson_locked),
                accentText = c(R.color.lesson_card_locked_fill),
                fill = c(R.color.lesson_card_locked_fill),
                buttonBg = c(R.color.lesson_panel_track),
                buttonText = c(R.color.lesson_card_locked_fill),
            )
        }
    }

    /**
     * Paneli tıklanan kartın altına (sığmazsa üstüne) yerleştirir, oku kartın ortasına hizalar,
     * karartmanın üstüne kartın bir görüntüsünü koyar ve paneli karttan büyüyerek açar. Kart
     * listede görünmüyorsa panel ekranın ortasında, oksuz açılıyor.
     */
    private fun placeLessonPanel(
        coordinator: View,
        panel: View,
        arrow: PanelPointerView,
        snapshot: ImageView,
        card: View?,
    ) {
        val density = context.resources.displayMetrics.density
        val gap = 6f * density
        val edge = 12f * density
        val overlap = 2f * density
        val arrowH = arrow.height.toFloat()
        val panelH = panel.height.toFloat()
        val coordH = coordinator.height.toFloat()
        val coordLoc = IntArray(2).also { coordinator.getLocationInWindow(it) }
        var pointUp = true
        var panelY = (coordH - panelH) / 2f
        var arrowCenterX = panel.left + panel.width / 2f
        val hasCard = card != null && card.isAttachedToWindow && card.height > 0
        if (hasCard) {
            val loc = IntArray(2).also { card!!.getLocationInWindow(it) }
            val cardTop = (loc[1] - coordLoc[1]).toFloat()
            val cardBottom = cardTop + card!!.height
            val cardLeft = (loc[0] - coordLoc[0]).toFloat()
            val fitsBelow = cardBottom + gap + arrowH + panelH + edge <= coordH
            val fitsAbove = cardTop - gap - arrowH - panelH - edge >= 0f
            pointUp = fitsBelow || !fitsAbove
            panelY = if (pointUp) cardBottom + gap + arrowH - overlap
                else cardTop - gap - arrowH + overlap - panelH
            panelY = panelY.coerceIn(edge, (coordH - panelH - edge).coerceAtLeast(edge))
            arrowCenterX = cardLeft + card.width / 2f
            arrow.pointUp = pointUp
            arrow.x = arrowCenterX - arrow.width / 2f
            arrow.y = if (pointUp) panelY - arrowH + overlap else panelY + panelH - overlap
            arrow.alpha = 0f
            arrow.visibility = View.VISIBLE
            arrow.animate().alpha(1f).setStartDelay(60L).setDuration(160L).start()
            runCatching { card.drawToBitmap() }.getOrNull()?.let { bmp ->
                snapshot.setImageBitmap(bmp)
                snapshot.x = cardLeft
                snapshot.y = cardTop
                snapshot.visibility = View.VISIBLE
            }
        }
        panel.y = panelY
        panel.pivotX = (arrowCenterX - panel.left).coerceIn(0f, panel.width.toFloat())
        panel.pivotY = if (pointUp) 0f else panelH
        panel.scaleX = 0.92f
        panel.scaleY = 0.92f
        panel.alpha = 0f
        panel.visibility = View.VISIBLE
        panel.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(180L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    /**
     * Ders paneli üzerinden başlatılan her akış (Başla/Devam et/Gözden geçir ve Eğitimi tekrarla)
     * 1 enerji harcar. Enerji yoksa panel kapanıp mağaza açılır ve [onGranted] çağrılmaz.
     * Sonsuz enerjide (Pro/Premium plan veya onaylı öğretmen) harcama yapılmaz.
     */
    private fun spendLessonEnergyThen(item: LessonItem, dismissPanel: () -> Unit, onGranted: () -> Unit) {
        getCurrentPlan { _ ->
            val mainActivity = context as MainActivity
            val energyManager = mainActivity.getEnergyManager()

            if (!energyManager.isInfiniteEnergy()) {
                if (!energyManager.hasEnoughEnergy(1)) {
                    // Yeterli enerji yok, kullanıcıya uyarı göster
                    AnalyticsLogger.logEnergyBlocked(
                        blockSource = AnalyticsLogger.ENERGY_BLOCK_LESSON,
                        waitSeconds = energyManager.getTimeUntilNextEnergy() / 1000L,
                        lessonsThisSession = EnergySessionCounter.bucket(),
                        partId = globalPartId,
                        lessonId = item.stableId,
                    )
                    dismissPanel(); showEnergyWarning(context)
                    return@getCurrentPlan
                }
                // Enerjiyi kullan
                energyManager.useEnergy(1)
                AnalyticsLogger.logEnergySpent(
                    spendSource = AnalyticsLogger.ENERGY_SPEND_LESSON,
                    energyLeft = energyManager.getCurrentEnergy(),
                    lessonsThisSession = EnergySessionCounter.bucket(),
                    partId = globalPartId,
                    lessonId = item.stableId,
                )
            }

            // Sayaç, olaylar gönderildikten SONRA artar: parametredeki değer "bu dersten
            // ÖNCE kaç ders yapılmıştı" olmalı.
            EnergySessionCounter.onLessonStarted()

            onGranted()
        }
    }

    private fun continueWithLesson(item: LessonItem, dismissPanel: () -> Unit) {
        dismissPanel()

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

            // Sağdan girer, sola çıkar — uygulamadaki bütün ekran geçişleriyle aynı.
            // android.R.anim.slide_in_left SOLDAN getiriyordu, yani zincirin tersi yöne.
            val slideIn = R.anim.queue_screen_in
            val slideOut = R.anim.queue_screen_out
            item.mapFragmentIndex.also { mapFragmentStepIndex = it!! }
            item.startStepNumber.also { lessonStep = it!! }
            if (item.isBlinding == true) {
                if (item.tutorialIsFinish) {
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut, slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, BlindingLessonFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                } else {
                    (main as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(true)
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut, slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, TutorialFragment.newInstance(item.tutorialNumber))
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                }
            } else {
                if (item.tutorialIsFinish) {
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut, slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, AbacusFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                } else {
                    (main as? MainActivity)?.setActiveMapTutorialOverlayFromLesson(true)
                    fm.beginTransaction()
                        .setCustomAnimations(slideIn, slideOut, slideIn, slideOut)
                        .replace(R.id.abacusFragmentContainer, TutorialFragment.newInstance(item.tutorialNumber))
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
        mainActivity.openShopFragment()
    }

    fun showRacePanel(item: LessonItem, position: Int, isFromPartSelection: Boolean = false) {
        // Activity'deki view'ları bul
        val activity = context as Activity
        val coordinatorLayout = activity.findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        val scrimView = activity.findViewById<View>(R.id.scrimView)

        scrimView.visibility = View.VISIBLE
        scrimView.alpha = 0.5f
        
        // Race panel açıldığında alt menüyü gizle
        activity.findViewById<View>(R.id.bottomNavigationID)?.visibility = View.GONE

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
            if (!isFromPartSelection) globalPartId = item.backRaceId!!
            behavior.isHideable = true
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        closeButton.setOnClickListener {
            if (!isFromPartSelection) globalPartId = item.backRaceId!!
            behavior.isHideable = true
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (!isFromPartSelection) {
                        globalPartId = item.backRaceId!!
                        GlobalLessonData.initialize(context, globalPartId) {
                            (context as? Activity)?.runOnUiThread { updateItems(GlobalLessonData.lessonItems) }
                        }
                    }
                    // Race panel kapandığında alt menüyü geri getir
                    activity.findViewById<View>(R.id.bottomNavigationID)?.visibility = View.VISIBLE
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
            ?.layoutManager?.findViewByPosition(position)
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
            if (main?.spendKeys(1, AnalyticsLogger.ITEM_RACE_FAST_FORWARD) != true) {
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
                    actionButton.text = "BAŞLAT"
                    actionButton.isAllCaps = true
                    actionButton.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    actionButton.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.lesson_completed)
                    actionButton.setTextColor(ContextCompat.getColor(context, R.color.panel_background))
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
            if (TeacherApprovalGate.blockIfUnapproved(context)) {
                dismissSheet()
                return@setOnClickListener
            }
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

            // Sağdan girer, sola çıkar — uygulamadaki bütün ekran geçişleriyle aynı.
            // android.R.anim.slide_in_left SOLDAN getiriyordu, yani zincirin tersi yöne.
            val slideIn = R.anim.queue_screen_in
            val slideOut = R.anim.queue_screen_out
            fm.beginTransaction()
                .setCustomAnimations(slideIn, slideOut, slideIn, slideOut)
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

    override fun getItemViewType(position: Int): Int = items.getOrNull(position)?.type ?: LessonItem.TYPE_LESSON

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            LessonItem.TYPE_LESSON -> LessonViewHolder(
                inflater.inflate(R.layout.item_lesson_card, parent, false)
            )
            LessonItem.TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_header, parent, false)
            )
            LessonItem.TYPE_CHEST -> LessonViewHolder(
                inflater.inflate(R.layout.item_lesson_card, parent, false)
            )
            LessonItem.TYPE_RACE -> RaceViewHolder(
                inflater.inflate(R.layout.item_race, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        when (holder) {
            is LessonViewHolder -> { holder.bind(item) }
            is HeaderViewHolder -> holder.bind(item)
            is RaceViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): LessonItem = items.getOrNull(position) ?: GlobalLessonData.getLessonItem(position) ?: error("No item at position $position")

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
            if (runningProgressIncreases.isNotEmpty()) {
                LessonProgressDiag.log(
                    "LessonAdapter.updateLessonItem",
                    "ilerleme animasyonu surerken idx=$position yenileniyor running=${runningProgressIncreases.keys} | ${callerTrace()}",
                )
            }
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
    /**
     * Ders ve sandık kartı ([R.layout.item_lesson_card]): solda kitap ikonu, ortada başlık ve
     * altında ilerleme — derste adım dilimleri ve "2/5", sandıkta (hep tek adımlı) kazanılan
     * yıldızlar —, sağda durum ikonu.
     *
     * Durumlar ([LessonItem.isCompleted] = açık mı):
     *  - kilitli: gri kart, soluk ikon, kilit;
     *  - açık: mavi kart, beyaz kenar, ok; bitmemişse kart hafifçe "nefes alıyor";
     *  - bitmiş: bütün kart altın, yazılar ve dilimler koyu kahve, onay.
     * Dersten dönünce yeni biten dilim animasyonla doluyor; son dilimde kart altına dönüyor
     * (bir kez; sonra kalıcı — eski daire halkasındaki mantığın aynısı).
     */
    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lessonCard: MaterialCardView = itemView.findViewById(R.id.lessonCard)
        private val lessonIcon: ImageView = itemView.findViewById(R.id.lessonIcon)
        private val lessonTitle: TextView = itemView.findViewById(R.id.lessonTitle)
        private val stepRow: View = itemView.findViewById(R.id.lessonStepRow)
        private val stepBar: SegmentBarView = itemView.findViewById(R.id.lessonStepBar)
        private val stepText: TextView = itemView.findViewById(R.id.lessonStepText)
        private val chestStarsRow: View = itemView.findViewById(R.id.chestStarsRow)
        private val chestStars: List<ImageView> = listOf(
            itemView.findViewById(R.id.chestStar1),
            itemView.findViewById(R.id.chestStar2),
            itemView.findViewById(R.id.chestStar3),
        )
        private val stateIcon: ImageView = itemView.findViewById(R.id.lessonStateIcon)
        private var progressBreathingAnimator: ValueAnimator? = null
        private var progressIncreaseAnimator: ValueAnimator? = null
        private var progressIncreaseStepCount: Int = 0
        private var progressIncreaseTargetFilled: Int = 0
        private var finalGoldAnimator: AnimatorSet? = null

        private val white = ContextCompat.getColor(context, android.R.color.white)
        private val openBg = ContextCompat.getColor(context, R.color.lesson_center_blue)
        private val doneBg = ContextCompat.getColor(context, R.color.lesson_center_gold)
        private val doneText = ContextCompat.getColor(context, R.color.lesson_card_done_text)
        private val doneFill = ContextCompat.getColor(context, R.color.lesson_card_done_fill)
        private val doneTrack = ContextCompat.getColor(context, R.color.lesson_card_done_track)
        private var isChestCard = false
        private var chestEarned = 0
        private val openTrack = ContextCompat.getColor(context, R.color.lesson_card_open_track)
        private val lockedText = ContextCompat.getColor(context, R.color.lesson_card_locked_text)
        private val lockedTrack = ContextCompat.getColor(context, R.color.lesson_card_locked_track)
        private val lockedFill = ContextCompat.getColor(context, R.color.lesson_card_locked_fill)
        private val iconLockedFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

        private fun cancelProgressIncreaseAnimation(applyFinalState: Boolean = true) {
            finalGoldAnimator?.cancel()
            finalGoldAnimator = null

            val animator = progressIncreaseAnimator ?: return
            if (applyFinalState && progressIncreaseStepCount > 0) {
                setSegments(
                    progressIncreaseStepCount,
                    progressIncreaseTargetFilled.coerceIn(0, progressIncreaseStepCount),
                )
            }
            animator.cancel()
            progressIncreaseAnimator = null
        }

        /** Dilimleri ve "dolu/toplam" yazısını birlikte günceller. */
        private fun setSegments(count: Int, filled: Int) {
            stepBar.setSegmentState(count, filled)
            stepText.text = "$filled/$count"
        }

        /**
         * Yerel (SharedPreferences) görsel durum anahtarı.
         *
         * Eskiden `item.id` kullanılıyordu ama o alan hiçbir zaman doldurulmuyordu (hep null →
         * -1) ve anahtar liste sırasını içerdiği için müfredata ders eklendiğinde kayıyordu.
         * Artık [LessonItem.stableId] kullanılıyor: ders nereye taşınırsa taşınsın anahtar aynı.
         */
        private fun lessonProgressKey(item: LessonItem): String =
            "${item.type}_${item.stableId}"

        /** Açık / kilitli kartın temel görünümü (bitmiş hâlin altın dokunuşları ayrı). */
        private fun applyBaseLook(unlocked: Boolean) {
            if (unlocked) {
                lessonCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_center_blue))
                lessonCard.strokeColor = white
                lessonTitle.setTextColor(white)
                stepText.setTextColor(white)
                stepBar.fillColor = white
                stepBar.trackColor = openTrack
                lessonIcon.colorFilter = null
                lessonIcon.alpha = 1f
                stateIcon.setImageResource(R.drawable.ic_lesson_card_chevron)
                stateIcon.imageTintList = android.content.res.ColorStateList.valueOf(white)
            } else {
                lessonCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_locked))
                lessonCard.strokeColor = android.graphics.Color.TRANSPARENT
                lessonTitle.setTextColor(lockedText)
                stepText.setTextColor(lockedText)
                stepBar.fillColor = lockedFill
                stepBar.trackColor = lockedTrack
                lessonIcon.colorFilter = iconLockedFilter
                lessonIcon.alpha = 0.6f
                stateIcon.setImageResource(R.drawable.lock_ic)
                stateIcon.imageTintList = android.content.res.ColorStateList.valueOf(lockedText)
            }
            stateIcon.scaleX = 1f
            stateIcon.scaleY = 1f
        }

        /** Bitmiş kart: bütün kart altın, yazılar ve dolu dilimler koyu kahve, sağda onay. */
        private fun applyPersistentFinalGoldState() {
            lessonCard.setCardBackgroundColor(doneBg)
            lessonCard.strokeColor = android.graphics.Color.TRANSPARENT
            lessonTitle.setTextColor(doneText)
            stepText.setTextColor(doneText)
            stepBar.fillColor = doneFill
            stepBar.trackColor = doneTrack
            stateIcon.setImageResource(R.drawable.ic_lesson_card_check)
            stateIcon.imageTintList = android.content.res.ColorStateList.valueOf(doneText)
            stateIcon.scaleX = 1f
            stateIcon.scaleY = 1f
            if (isChestCard) bindChestStars(chestEarned, unlocked = true, done = true)
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

        /** Son dilim dolunca: mavi kart altına dönüyor, yazılar ve dilimler koyu kahveye, ok onaya. */
        private fun playFinalGoldMergeAnimation(item: LessonItem, key: String) {
            if (playedFinalGoldAnimationKeys.contains(key) || item.finalGoldVisualUnlocked) {
                persistFinalGoldVisualState(item, key)
                applyPersistentFinalGoldState()
                return
            }
            stopProgressBreathingAnimation()

            val argb = ArgbEvaluator()
            val colorAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 520L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { va ->
                    val f = va.animatedValue as Float
                    lessonCard.setCardBackgroundColor(argb.evaluate(f, openBg, doneBg) as Int)
                    lessonCard.strokeColor = argb.evaluate(f, white, android.graphics.Color.TRANSPARENT) as Int
                    val text = argb.evaluate(f, white, doneText) as Int
                    lessonTitle.setTextColor(text)
                    stepText.setTextColor(text)
                    stepBar.fillColor = argb.evaluate(f, white, doneFill) as Int
                }
            }

            // Ok küçülüp kayboluyor, yerine altın onay büyüyerek geliyor.
            val iconSwap = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 520L
                var swapped = false
                addUpdateListener { va ->
                    val p = va.animatedValue as Float
                    if (p < 0.5f) {
                        val s = 1f - p * 2f
                        stateIcon.scaleX = s
                        stateIcon.scaleY = s
                    } else {
                        if (!swapped) {
                            swapped = true
                            stateIcon.setImageResource(R.drawable.ic_lesson_card_check)
                            stateIcon.imageTintList = android.content.res.ColorStateList.valueOf(doneText)
                        }
                        val q = (p - 0.5f) * 2f
                        val s = q * (1f + 0.25f * kotlin.math.sin(Math.PI * q).toFloat())
                        stateIcon.scaleX = s
                        stateIcon.scaleY = s
                    }
                }
            }

            finalGoldAnimator = AnimatorSet().apply {
                playTogether(colorAnim, iconSwap)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        persistFinalGoldVisualState(item, key)
                        applyPersistentFinalGoldState()
                        if (finalGoldAnimator == this@apply) {
                            finalGoldAnimator = null
                        }
                    }
                })
                start()
            }
        }

        private fun applyStepSegmentsWithIncreaseAnimation(item: LessonItem) {
            // Bu holder başka bir kartın animasyonunu oynatıyor olabilir; durduruluyor ama
            // [runningProgressIncreases] kaydı silinmiyor — o kart bağlanınca kaldığı yerden sürer.
            cancelProgressIncreaseAnimation(applyFinalState = true)
            val safeStepCount = item.stepCount.coerceAtLeast(1)
            val completedSteps = item.stepCompletionStatus.count { it }
            val targetFilled = if (item.stepIsFinish) safeStepCount else completedSteps.coerceIn(0, safeStepCount)
            val key = lessonProgressKey(item)
            if ((playedFinalGoldAnimationKeys.contains(key) || item.finalGoldVisualUnlocked) && targetFilled == safeStepCount) {
                runningProgressIncreases.remove(key)
                setSegments(safeStepCount, targetFilled)
                persistFinalGoldVisualState(item, key)
                applyPersistentFinalGoldState()
                lastSeenFilledSegments[key] = targetFilled
                return
            }

            // Animasyon sürerken liste yenilendi: baştan değil, kaldığı yerden devam.
            runningProgressIncreases[key]?.let { run ->
                val elapsed = android.os.SystemClock.uptimeMillis() - run.startedAtMs
                if (run.toFilled == targetFilled && run.stepCount == safeStepCount && elapsed < run.durationMs) {
                    LessonProgressDiag.log(
                        "LessonAdapter.progress",
                        "SURDUR key=$key ${run.fromFilled}->${run.toFilled} gecen=${elapsed}ms/${run.durationMs}ms pos=$bindingAdapterPosition",
                    )
                    startProgressIncreaseAnimator(item, key, run, elapsed)
                    return
                }
                // Süresi doldu (kart o sırada ekranda değildi) ya da hedef değişti: normal yol.
                runningProgressIncreases.remove(key)
                LessonProgressDiag.log(
                    "LessonAdapter.progress",
                    "SURE_DOLDU key=$key gecen=${elapsed}ms hedef=${run.toFilled}->$targetFilled",
                )
            }

            val pending = GlobalValues.pendingLessonProgressAnimations[key]
            val shouldConsumePending = GlobalValues.canConsumePendingLessonProgressAnimations && pending != null
            val previousFilled = when {
                shouldConsumePending -> pending!!.fromFilledSegments.coerceIn(0, safeStepCount)
                pending != null -> pending.fromFilledSegments.coerceIn(0, safeStepCount)
                else -> lastSeenFilledSegments[key]?.coerceIn(0, safeStepCount) ?: targetFilled
            }
            if (pending != null) {
                LessonProgressDiag.log(
                    "LessonAdapter.progress",
                    "BIND key=$key pos=$bindingAdapterPosition pending=${pending.fromFilledSegments}->${pending.toFilledSegments} " +
                        "hedef=$targetFilled onceki=$previousFilled tuket=$shouldConsumePending " +
                        "gorunur=${itemView.isShown} ekli=${itemView.isAttachedToWindow}",
                )
            }

            if (targetFilled > previousFilled) {
                if (shouldConsumePending) {
                    val run = RunningProgressIncrease(
                        fromFilled = previousFilled,
                        toFilled = targetFilled,
                        stepCount = safeStepCount,
                        startedAtMs = android.os.SystemClock.uptimeMillis(),
                        durationMs = ((targetFilled - previousFilled) * 900L).coerceAtLeast(1800L),
                    )
                    runningProgressIncreases[key] = run
                    LessonProgressDiag.log(
                        "LessonAdapter.progress",
                        "BASLAT key=$key ${run.fromFilled}->${run.toFilled} sure=${run.durationMs}ms pos=$bindingAdapterPosition",
                    )
                    startProgressIncreaseAnimator(item, key, run, 0L)
                    GlobalValues.pendingLessonProgressAnimations.remove(key)
                    lastSeenFilledSegments[key] = targetFilled
                } else if (pending != null) {
                    setSegments(safeStepCount, previousFilled)
                    lastSeenFilledSegments[key] = previousFilled
                } else {
                    setSegments(safeStepCount, targetFilled)
                    lastSeenFilledSegments[key] = targetFilled
                }
            } else {
                setSegments(safeStepCount, targetFilled)
                if (shouldConsumePending) {
                    GlobalValues.pendingLessonProgressAnimations.remove(key)
                }
                if (
                    targetFilled == safeStepCount &&
                    !playedFinalGoldAnimationKeys.contains(key) &&
                    !item.finalGoldVisualUnlocked
                ) {
                    playFinalGoldMergeAnimation(item, key)
                }
                lastSeenFilledSegments[key] = targetFilled
            }
        }

        /**
         * İlerleme artışını [elapsedMs]'ten itibaren oynatır (0 = baştan).
         *
         * İptal (holder geri dönüşüme gitti / başka karta bağlandı) kaydı SİLMİYOR; yalnızca
         * gerçek bitiş siliyor. Kart yeniden bağlandığında kayıt sayesinde devam ediyor.
         */
        private fun startProgressIncreaseAnimator(
            item: LessonItem,
            key: String,
            run: RunningProgressIncrease,
            elapsedMs: Long,
        ) {
            val stepCount = run.stepCount
            val targetFilled = run.toFilled
            progressIncreaseStepCount = stepCount
            progressIncreaseTargetFilled = targetFilled
            setSegments(stepCount, run.fromFilled)
            progressIncreaseAnimator = ValueAnimator.ofFloat(run.fromFilled.toFloat(), targetFilled.toFloat()).apply {
                duration = run.durationMs
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    val current = animator.animatedValue as Float
                    stepBar.setSegmentProgress(current)
                    stepText.text = "${current.toInt()}/$stepCount"
                }
                addListener(object : AnimatorListenerAdapter() {
                    var isCancelled = false

                    override fun onAnimationCancel(animation: Animator) {
                        isCancelled = true
                        if (progressIncreaseAnimator == animation) progressIncreaseAnimator = null
                        setSegments(stepCount, targetFilled)
                        LessonProgressDiag.log(
                            "LessonAdapter.progress",
                            "IPTAL key=$key (kayit duruyor, yeniden baglaninca surer) | ${callerTrace()}",
                        )
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (isCancelled) return
                        if (progressIncreaseAnimator == animation) progressIncreaseAnimator = null
                        // Aynı kart iki holder'da oynuyor olabilir (değişim çapraz geçişi);
                        // yalnızca bu kaydın sahibi siliyor.
                        if (runningProgressIncreases[key] == run) runningProgressIncreases.remove(key)
                        setSegments(stepCount, targetFilled)
                        LessonProgressDiag.log("LessonAdapter.progress", "BITTI key=$key")
                        if (targetFilled == stepCount) {
                            playFinalGoldMergeAnimation(item, key)
                        }
                    }
                })
                start()
                if (elapsedMs > 0L) currentPlayTime = elapsedMs
            }
        }

        /** Açık ve bitmemiş kart hafifçe büyüyüp küçülüyor ("sıradaki ders"). */
        private fun startProgressBreathingAnimation() {
            progressBreathingAnimator?.cancel()
            lessonCard.scaleX = 1f
            lessonCard.scaleY = 1f
            progressBreathingAnimator = ValueAnimator.ofFloat(1f, 1.02f, 1f).apply {
                duration = 2400L
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val scale = animation.animatedValue as Float
                    lessonCard.scaleX = scale
                    lessonCard.scaleY = scale
                }
                start()
            }
        }

        fun stopProgressBreathingAnimation() {
            progressBreathingAnimator?.cancel()
            progressBreathingAnimator = null
            lessonCard.scaleX = 1f
            lessonCard.scaleY = 1f
            cancelProgressIncreaseAnimation(applyFinalState = true)
        }

        /** Sandığın kazandığı yıldız sayısı (bitmemişse 0); eski kupa ikonları da sayılıyor. */
        private fun chestStarCount(item: LessonItem): Int {
            if (!item.stepIsFinish) return 0
            return when (item.stepCupIcon) {
                R.drawable.chest_stars_tier3, R.drawable.cup_ic3 -> 3
                R.drawable.chest_stars_tier2, R.drawable.cup_ic2 -> 2
                R.drawable.chest_stars_tier1, R.drawable.cup_ic -> 1
                else -> 0
            }
        }

        /**
         * Sandık yıldızları. Bitmiş (altın) kartta açık sarı yıldız zeminde kayboluyordu: orada
         * kazanılanlar koyu kahve dolu, kazanılmayanlar soluk.
         */
        private fun bindChestStars(earned: Int, unlocked: Boolean, done: Boolean = false) {
            chestStars.forEachIndexed { i, iv ->
                if (i < earned) {
                    iv.setImageResource(R.drawable.star_on_ic)
                    iv.imageTintList = if (done) android.content.res.ColorStateList.valueOf(doneFill) else null
                    iv.alpha = 1f
                } else {
                    iv.setImageResource(R.drawable.star_off_ic)
                    iv.imageTintList = if (done) android.content.res.ColorStateList.valueOf(doneFill) else null
                    iv.alpha = when {
                        done -> 0.3f
                        unlocked -> 1f
                        else -> 0.55f
                    }
                }
            }
        }

        fun bind(item: LessonItem) {
            stopProgressBreathingAnimation()
            val isChest = item.type == LessonItem.TYPE_CHEST
            isChestCard = isChest
            val unlocked = item.isCompleted
            // 1, 2, 3 ve 6. bölümde sandıklar kürsü ikonuyla ayrışıyor; diğerleri kitap.
            lessonIcon.setImageResource(
                if (isChest && globalPartId in setOf(1, 2, 3, 6)) R.drawable.podium_ic2 else R.drawable.profile_book_ic3
            )

            if (isChest && adapterPosition == MarathonGuideStore.firstMarathonLessonIndex()) {
                LessonProgressDiag.logItem(
                    "LessonAdapter.bind",
                    GlobalLessonData.globalPartId,
                    adapterPosition,
                    item,
                    "marathonCardUI",
                )
            }

            lessonTitle.text = item.title
            stepRow.visibility = if (isChest) View.GONE else View.VISIBLE
            chestStarsRow.visibility = if (isChest) View.VISIBLE else View.GONE
            if (isChest) {
                // Eski kayıtlardaki tanınmayan kupa ikonu sıfırlanıyor (daire sürümündeki gibi).
                if (item.stepCupIcon == 0) item.stepCupIcon = R.drawable.chest_stars_tier0
                chestEarned = chestStarCount(item)
                bindChestStars(chestEarned, unlocked)
            }
            applyBaseLook(unlocked)

            lessonCard.setOnClickListener {
                // Karta tıklanınca internet + giriş kontrolü, sonra ders paneli.
                (itemView.context as? MainActivity)?.requireOnlineAndLoggedInOrLogin {
                    showLessonBottomSheet(item, adapterPosition)
                }
            }

            applyStepSegmentsWithIncreaseAnimation(item)
            val key = lessonProgressKey(item)
            val goldDone = (playedFinalGoldAnimationKeys.contains(key) || item.finalGoldVisualUnlocked) && item.stepIsFinish
            if (unlocked && !item.stepIsFinish && !goldDone) {
                startProgressBreathingAnimation()
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
        if (runningProgressIncreases.isNotEmpty()) {
            LessonProgressDiag.log(
                "LessonAdapter.updateItems",
                "ilerleme animasyonu surerken liste yenileniyor running=${runningProgressIncreases.keys} | ${callerTrace()}",
            )
        }
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




