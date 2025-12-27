package com.example.app

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.TextView
import com.example.app.model.GuidePanelData

class GuidePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var guidePanelRoot: View
    private lateinit var panelContent: ConstraintLayout
    private lateinit var ivGuideImage: ImageView
    private lateinit var tvGuideText: TextView
    private lateinit var btnBack: ImageButton
    
    private var currentIndex = 0
    private var guideDataList: List<GuidePanelData> = emptyList()
    private var onBackClickListener: (() -> Unit)? = null
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
        ivGuideImage = view.findViewById(R.id.ivGuideImage)
        tvGuideText = view.findViewById(R.id.tvGuideText)
        btnBack = view.findViewById(R.id.btnBack)
        
        // Root view'a touch event geldiğinde consume et (arka plandaki view'lar tıklanamaz)
        guidePanelRoot.setOnTouchListener { view, motionEvent ->
            // Touch event'i consume et, arka plandaki view'lara gitmesin
            true
        }
        
        // Root view'a tıklandığında (panel içeriği hariç) hiçbir şey yapma
        guidePanelRoot.setOnClickListener { 
            // Sadece event'i consume et, hiçbir şey yapma
        }
        
        // Back butonu - bir önceki içeriğe dön
        btnBack.setOnClickListener {
            if (!isAnimating) {
                showPreviousContent()
            }
        }
        
        // Panel tıklama
        panelContent.setOnClickListener { view ->
            if (!isAnimating) {
                onPanelClickListener?.invoke()
                showNextContent()
            }
        }
    }

    fun setGuideData(dataList: List<GuidePanelData>) {
        guideDataList = dataList
        currentIndex = 0
        if (dataList.isNotEmpty()) {
            // View hazır olduğunda içeriği güncelle
            post {
                updateContent()
            }
        }
    }

    fun setOnBackClickListener(listener: () -> Unit) {
        onBackClickListener = listener
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
    
    /**
     * Son adımdayken belirtilen view'a tıklandığında paneli kapatır
     * @param view Son adımdayken tıklanabilir olacak view
     * @param onTargetClicked View'a tıklandığında çağrılacak ek callback (opsiyonel)
     */
    fun setTargetViewForLastStep(view: View?, onTargetClicked: (() -> Unit)? = null) {
        // Önceki target view'ın listener'ını temizle
        targetViewForLastStep?.setOnClickListener(null)
        
        targetViewForLastStep = view
        onTargetViewClickedListener = onTargetClicked
        
        // Eğer şu an son adımdaysa listener'ı hemen ekle
        if (isOnLastStep() && view != null) {
            setupTargetViewClickListener(view)
        }
    }
    
    private fun setupTargetViewClickListener(view: View) {
        view.setOnClickListener {
            // Target view'a tıklandığında önce callback'i çağır
            onTargetViewClickedListener?.invoke()
            // Sonra paneli kapat
            hideToLeft()
        }
    }

    private fun showNextContent() {
        if (guideDataList.isEmpty()) return
        
        // Son adımdaysa ve target view varsa, panelContent'e tıklanınca hiçbir şey yapma
        // Sadece target view'a tıklandığında panel kapanacak
        if (currentIndex == guideDataList.size - 1) {
            // Eğer target view tanımlanmışsa, panelContent'e tıklanınca hiçbir şey yapma
            if (targetViewForLastStep != null) {
                return
            }
            // Eğer target view yoksa (eski davranış), paneli kapat
            hideToLeft()
            return
        }
        
        // Bir sonraki içeriğe geç
        currentIndex++
        updateContent()
    }
    
    fun showPreviousContent() {
        if (guideDataList.isEmpty()) return
        
        // İlk adımdaysa bir şey yapma (panel kapanmaz, sadece ilk adımda kalır)
        if (currentIndex == 0) return
        
        // Bir önceki içeriğe dön
        currentIndex--
        updateContent()
    }

    private fun updateContent() {
        if (guideDataList.isEmpty() || currentIndex !in guideDataList.indices) {
            return
        }
        
        val currentData = guideDataList[currentIndex]

        ivGuideImage.setImageResource(currentData.imageResId)
        tvGuideText.text = currentData.text
        
        // Text görünürlüğünü kontrol et ve göster
        tvGuideText.visibility = View.VISIBLE
        
        // Eğer son adıma gelindiyse callback'i tetikle ve target view listener'ını ayarla
        if (currentIndex == guideDataList.size - 1) {
            onLastStepReachedListener?.invoke()
            // Target view varsa listener'ını ayarla
            targetViewForLastStep?.let { view ->
                setupTargetViewClickListener(view)
            }
        } else {
            // Son adım değilse target view listener'ını temizle
            targetViewForLastStep?.setOnClickListener(null)
        }
    }

    fun show() {
        visibility = View.VISIBLE
        
        // İçeriği güncelle (eğer henüz güncellenmediyse)
        if (guideDataList.isNotEmpty() && currentIndex in guideDataList.indices) {
            updateContent()
        }
        
        // Animasyon sırasında tıklamaları engelle
        isAnimating = true
        panelContent.isClickable = false
        btnBack.isClickable = false
        
        // Sağdan kayarak gelme animasyonu
        val screenWidth = resources.displayMetrics.widthPixels
        val translateX = ObjectAnimator.ofFloat(this, "translationX", screenWidth.toFloat(), 0f)
        translateX.duration = 500
        translateX.interpolator = AccelerateDecelerateInterpolator()
        
        // Animasyon tamamlandığında tıklamaları tekrar aktif et
        translateX.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                panelContent.isClickable = true
                btnBack.isClickable = true
            }
        })
        
        translateX.start()
    }

    fun hide() {
        // Animasyon sırasında tıklamaları engelle
        isAnimating = true
        panelContent.isClickable = false
        btnBack.isClickable = false
        
        // Sağa kayarak kaybol (normal kapanma)
        val screenWidth = resources.displayMetrics.widthPixels
        val translateX = ObjectAnimator.ofFloat(this, "translationX", 0f, screenWidth.toFloat())
        translateX.duration = 300
        translateX.interpolator = AccelerateDecelerateInterpolator()
        
        translateX.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                visibility = View.GONE
                // Panel kapandığında callback çağır
                onPanelHideListener?.invoke()
            }
        })
        
        translateX.start()
    }
    
    fun hideToLeft() {
        // Animasyon sırasında tıklamaları engelle
        isAnimating = true
        panelContent.isClickable = false
        btnBack.isClickable = false
        
        // Sola kayarak kaybol (son adımda)
        val screenWidth = resources.displayMetrics.widthPixels
        val translateX = ObjectAnimator.ofFloat(this, "translationX", 0f, -screenWidth.toFloat())
        translateX.duration = 300
        translateX.interpolator = AccelerateDecelerateInterpolator()
        
        translateX.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                visibility = View.GONE
                // Panel kapandığında callback çağır
                onPanelHideListener?.invoke()
            }
        })
        
        translateX.start()
    }
    
    fun isOnLastStep(): Boolean {
        return guideDataList.isNotEmpty() && currentIndex == guideDataList.size - 1
    }
}

