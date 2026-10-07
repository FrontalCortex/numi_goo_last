package com.example.app

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.app.model.GuidePanelData

class GuidePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var guidePanelRoot: View
    private lateinit var panelContent: ConstraintLayout
    private lateinit var stepDotsContainer: LinearLayout
    private lateinit var guideMascot: BunnyMascotView
    private lateinit var tvGuideText: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton

    private var currentIndex = 0
    private var guideDataList: List<GuidePanelData> = emptyList()
    private var onPanelClickListener: (() -> Unit)? = null
    private var onPanelHideListener: (() -> Unit)? = null
    private var onLastStepReachedListener: (() -> Unit)? = null
    private var targetViewForLastStep: View? = null
    private var onTargetViewClickedListener: (() -> Unit)? = null

    init {
        initView()
    }

    private var isAnimating = false

    private fun initView() {
        val view = LayoutInflater.from(context).inflate(R.layout.view_guide_panel, this, true)

        guidePanelRoot = view.findViewById(R.id.guidePanelRoot)
        panelContent = view.findViewById(R.id.panelContent)
        stepDotsContainer = view.findViewById(R.id.stepDotsContainer)
        guideMascot = view.findViewById(R.id.guideMascot)
        tvGuideText = view.findViewById(R.id.tvGuideText)
        btnBack = view.findViewById(R.id.btnBack)
        btnForward = view.findViewById(R.id.btnForward)

        guidePanelRoot.setOnTouchListener { _, _ -> true }

        // Geri yalnızca önceki adıma döner; ilk adımda işlevsiz. Eskiden ilk adımda paneli
        // kapatıyordu, rehber okunmadan geçilebiliyordu.
        btnBack.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            showPreviousContent()
        }

        btnForward.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            onPanelClickListener?.invoke()
            showNextContent()
        }
    }

    fun setGuideData(dataList: List<GuidePanelData>) {
        guideDataList = dataList
        currentIndex = 0
        if (dataList.isNotEmpty()) {
            post { updateContent() }
        }
    }

    fun setOnPanelClickListener(listener: () -> Unit) {
        onPanelClickListener = listener
    }

    fun setOnPanelHideListener(listener: () -> Unit) {
        onPanelHideListener = listener
    }

    fun setOnLastStepReachedListener(listener: () -> Unit) {
        onLastStepReachedListener = listener
    }

    fun setTargetViewForLastStep(view: View?, onTargetClicked: (() -> Unit)? = null) {
        targetViewForLastStep?.setOnClickListener(null)

        targetViewForLastStep = view
        onTargetViewClickedListener = onTargetClicked

        if (isOnLastStep() && view != null) {
            setupTargetViewClickListener(view)
        }
        updateNavButtons()
    }

    private fun setupTargetViewClickListener(view: View) {
        view.setOnClickListener {
            onTargetViewClickedListener?.invoke()
            hideToLeft()
        }
    }

    private fun showNextContent() {
        if (guideDataList.isEmpty()) return

        if (currentIndex == guideDataList.size - 1) {
            if (targetViewForLastStep != null) return
            hideToLeft()
            return
        }

        currentIndex++
        updateContent()
    }

    fun showPreviousContent() {
        if (guideDataList.isEmpty()) return
        if (currentIndex == 0) return

        currentIndex--
        updateContent()
    }

    private fun updateContent() {
        if (guideDataList.isEmpty() || currentIndex !in guideDataList.indices) {
            return
        }

        val currentData = guideDataList[currentIndex]

        guideMascot.play(currentData.emote)
        tvGuideText.text = currentData.text
        tvGuideText.visibility = View.VISIBLE

        updateStepIndicator()
        updateNavButtons()

        if (currentIndex == guideDataList.size - 1) {
            onLastStepReachedListener?.invoke()
            targetViewForLastStep?.let { setupTargetViewClickListener(it) }
        } else {
            targetViewForLastStep?.setOnClickListener(null)
        }
    }

    private fun updateStepIndicator() {
        stepDotsContainer.removeAllViews()
        val totalSteps = guideDataList.size
        if (totalSteps <= 1) {
            stepDotsContainer.visibility = View.GONE
            return
        }
        stepDotsContainer.visibility = View.VISIBLE
        for (i in 0 until totalSteps) {
            val dotView = TextView(context).apply {
                text = if (i == currentIndex) "●" else "○"
                textSize = 16f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (i < totalSteps - 1) {
                        marginEnd = 8.dpToPx()
                    }
                }
            }
            stepDotsContainer.addView(dotView)
        }
    }

    private fun updateNavButtons() {
        val onLastWithTarget = isOnLastStep() && targetViewForLastStep != null
        btnForward.visibility = if (onLastWithTarget) View.GONE else View.VISIBLE
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    fun show() {
        prepareShow()
        slideIn()
    }

    /**
     * Paneli görünür yapar (karartma açılır) ama ekranın sağında, dışarıda bekletir; [slideIn]
     * çağrılınca içeri kayar. Arada balon [placeBalloonTop] ile yerleştirilebilir, böylece ekranda
     * bir yerde görünüp sonra yer değiştirmez.
     */
    fun prepareShow() {
        visibility = View.VISIBLE
        translationX = resources.displayMetrics.widthPixels.toFloat()
        translationY = 0f

        dimCoordinatorLayout(true)

        if (guideDataList.isNotEmpty() && currentIndex in guideDataList.indices) {
            updateContent()
        }

        isAnimating = true
        setNavButtonsEnabled(false)
    }

    /** Balonun (panelContent) üst kenarını ebeveyndeki [top] konumuna getirir. */
    fun placeBalloonTop(top: Float) {
        translationY = top - this.top - panelContent.top
    }

    fun balloonHeight(): Int = panelContent.height

    fun slideIn() {
        val screenWidth = resources.displayMetrics.widthPixels
        val translateX = ObjectAnimator.ofFloat(this, "translationX", screenWidth.toFloat(), 0f)
        translateX.duration = 500
        translateX.interpolator = AccelerateDecelerateInterpolator()

        translateX.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                setNavButtonsEnabled(true)
            }
        })

        translateX.start()
    }

    fun hide() {
        dimCoordinatorLayout(false)

        isAnimating = true
        setNavButtonsEnabled(false)

        val screenWidth = resources.displayMetrics.widthPixels
        val translateX = ObjectAnimator.ofFloat(this, "translationX", 0f, screenWidth.toFloat())
        translateX.duration = 500
        translateX.interpolator = AccelerateDecelerateInterpolator()

        translateX.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                visibility = View.GONE
                onPanelHideListener?.invoke()
            }
        })

        translateX.start()
    }

    fun hideToLeft() {
        dimCoordinatorLayout(false)

        isAnimating = true
        setNavButtonsEnabled(false)

        val screenWidth = resources.displayMetrics.widthPixels
        val translateX = ObjectAnimator.ofFloat(this, "translationX", 0f, -screenWidth.toFloat())
        translateX.duration = 300
        translateX.interpolator = AccelerateDecelerateInterpolator()

        translateX.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                visibility = View.GONE
                onPanelHideListener?.invoke()
            }
        })

        translateX.start()
    }

    fun isOnLastStep(): Boolean {
        return guideDataList.isNotEmpty() && currentIndex == guideDataList.size - 1
    }

    private fun setNavButtonsEnabled(enabled: Boolean) {
        btnBack.isClickable = enabled
        btnForward.isClickable = enabled
    }

    private var scrim: View? = null

    /**
     * Rehberin hemen arkasına (aynı ebeveyn, daha düşük elevation) siyah bir karartma koyar.
     * Eskiden yalnızca ebeveynin zemin rengi değiştiriliyordu; ders kartları zeminin önünde
     * çizildiği için hiçbiri kararmıyordu, ekranda karartma görünmüyordu. Ders paneli aktivitenin
     * koordinatöründe (bu fragment'in üstünde) durduğu için karartmanın altında kalmıyor.
     */
    private fun dimCoordinatorLayout(dim: Boolean) {
        val parentGroup = parent as? ViewGroup ?: return
        if (dim) {
            val view = scrim ?: View(context).apply {
                setBackgroundColor(Color.BLACK)
                // Rehberin (100dp) altında, haritadaki soru düğmesinin (1dp) üstünde.
                elevation = 50f * resources.displayMetrics.density
                outlineProvider = null
                isClickable = true
            }.also { scrim = it }
            if (view.parent == null) {
                parentGroup.addView(
                    view,
                    parentGroup.indexOfChild(this),
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                view.alpha = 0f
            }
            view.animate().cancel()
            view.animate().alpha(DIM_ALPHA).setDuration(200L).start()
        } else {
            val view = scrim ?: return
            view.animate().cancel()
            view.animate().alpha(0f).setDuration(200L)
                .withEndAction { (view.parent as? ViewGroup)?.removeView(view) }
                .start()
        }
    }

    private companion object {
        // Ders panelinin karartmasıyla (LessonAdapter.LESSON_PANEL_DIM_ALPHA) aynı oran.
        const val DIM_ALPHA = 0.7f
    }
}
