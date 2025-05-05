package com.example.numigoo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.numigoo.GlobalValues.lessonStep
import com.example.numigoo.databinding.FragmentTutorialBinding
import com.example.numigoo.model.BeadAnimation

class TutorialFragment : Fragment() {
    private var currentAnimations: MutableList<BeadAnimation> = mutableListOf()
    private val widgetAnimators = mutableListOf<ValueAnimator>()
    private lateinit var binding: FragmentTutorialBinding
    private var currentStep = 0
    private var backOrFront = true
    private lateinit var focusView: View
    private var tutorialSteps: List<TutorialStep> = emptyList()
    private var sizeHistory = mutableListOf<Pair<Int, Int>>()


    companion object {
        fun newInstance() = TutorialFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        focusView = binding.focusView
        createTutorialSteps()
        setupTutorial()
        setupBackButton()
        setupQuitButton()
    }

    private fun setupTutorial() {
        // İlk adımı göster
        showStep(currentStep)

        // Ekrana tıklama ile ilerleme
        binding.root.setOnClickListener {
            if (isAnyAnimationRunning()) {
                return@setOnClickListener
            }

            if (currentStep < tutorialSteps.size - 1) {
                currentStep++
                backOrFront = true
                showStep(currentStep)
            }
        }
    }

    private fun showStep(position: Int) {
        val step = tutorialSteps[position]
        binding.tutorialText.text = step.text

        step.widgetOperations?.let { operations ->
            applyWidgetOperations(operations.map { it() })
        }

        if (position == tutorialSteps.size - 1) {
            val operations = MapFragment.getLessonOperations(lessonStep)
            devametFragment(AbacusFragment.newInstance("+", "Kuralsız Toplama", operations))
        }

        currentAnimations.clear()



        if(backOrFront) {
            step.onStep?.invoke(focusView)
            step.animation?.let { animations ->
                // Yeni animasyonları listeye ekle
                currentAnimations.addAll(animations)
                // Animasyonları çalıştır
                animations.forEach { it.animate() }
            }
        } else {
            tutorialSteps[position+1].animation?.forEach { originalAnimation ->
                val reversedType = when(originalAnimation.getAnimationType()) {
                    1 -> 2
                    2 -> 1
                    3 -> 4
                    4 -> 3
                    else -> originalAnimation.getAnimationType()
                }
                val newAnimation = BeadAnimation(this, originalAnimation.getBeadId(), reversedType)
                currentAnimations.add(newAnimation)
                newAnimation.animate()
            }
            tutorialSteps[position].widgetOperations?.forEach { opLambda ->
                val op = opLambda()
                if (op is WidgetOperation.AnimateMargin) {
                    applyWidgetOperations(listOf(op))
                }
            }
            if (sizeHistory.size > 0 && tutorialSteps[position+1].onStep != null) {
                val (prevWidth, prevHeight) = sizeHistory[sizeHistory.size - 1]
                val animateSizeOp = WidgetOperation.AnimateSize(
                    view = focusView,
                    fromWidth = focusView.width,
                    toWidth = prevWidth,
                    fromHeight = focusView.height,
                    toHeight = prevHeight,
                    duration = 400
                )
                applyWidgetOperations(listOf(animateSizeOp))
                sizeHistory.removeAt(sizeHistory.size - 1)
            }
            val nextStep = tutorialSteps.getOrNull(position + 1)

            val hasChangeVisibility = nextStep?.widgetOperations
                ?.map { it() }
                ?.any { it is WidgetOperation.ChangeVisibility } == true

            if (hasChangeVisibility) {
                // Eğer bir sonraki adımda VISIBLE visibility değişikliği varsa, geri adımda GONE yap
                val hasVisibleOp = nextStep?.widgetOperations
                    ?.map { it() }
                    ?.any { it is WidgetOperation.ChangeVisibility && it.visibility == View.VISIBLE } == true

                if (hasVisibleOp) {
                    applyWidgetOperations(listOf(WidgetOperation.ChangeVisibility(focusView, View.GONE)))
                }

                // Eğer bir sonraki adımda GONE visibility değişikliği varsa, geri adımda VISIBLE yap
                val hasGoneOp = nextStep?.widgetOperations
                    ?.map { it() }
                    ?.any { it is WidgetOperation.ChangeVisibility && it.visibility == View.GONE } == true

                if (hasGoneOp) {
                    applyWidgetOperations(listOf(WidgetOperation.ChangeVisibility(focusView, View.VISIBLE)))
                }
            }
        }
    }

    private fun setupQuitButton(){
        binding.quitButton.setOnClickListener{
            closeFragment()
        }
    }
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            // Herhangi bir animasyon devam ediyorsa geri gitmeyi engelle
            if (isAnyAnimationRunning()) {
                return@setOnClickListener
            }

            if (currentStep > 0) {
                currentStep--
                backOrFront = false
                showStep(currentStep)
                binding.devamButton.visibility = View.GONE
                binding.abacusLinear.visibility = View.VISIBLE
            } else {
                closeFragment()
            }
        }
    }
    private fun closeFragment(){// Fragment'i kapat ve MapFragment'e dön
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,  // Giriş animasyonu
                R.anim.slide_out_left // Çıkış animasyonu
            )
            .remove(this)
            .commit()

    }
    private fun devametFragment(fragment: Fragment) {
        // Devam butonunu görünür yap
        binding.abacusLinear.visibility = View.GONE
        binding.devamButton.visibility = View.VISIBLE

        // Devam butonuna tıklama olayını ekle
        binding.devamButton.setOnClickListener {
            // Yeni fragment'i göster
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,  // Giriş animasyonu
                    R.anim.slide_out_right  // Çıkış animasyonu
                )
                .replace(R.id.abacusFragmentContainer, fragment)  // fragment_container, ana layout'taki container ID'si
                .commit()

            // TutorialFragment'i kapat
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,  // Giriş animasyonu
                    R.anim.slide_out_right  // Çıkış animasyonu
                )
                .remove(this)
                .commit()
        }
    }
    private fun isAnyAnimationRunning(): Boolean {
        return currentAnimations.any { it.isAnimating() } || widgetAnimators.any { it.isRunning }
    }
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    private fun createTutorialSteps(){
        tutorialSteps = listOf(
            TutorialStep(
                "Abaküs, sayıları temsil etmek için boncuklar kullanan bir hesap aracıdır.",
                null
            ),
            TutorialStep(
                "Her sütun bir basamağı temsil eder. Basamaklar sağdan sola doğru artarak ilerler.",
                null
            ),
            TutorialStep(
                "Birler",
                null,
                listOf(
                    { WidgetOperation.ChangeVisibility(focusView, View.VISIBLE) },
                    {
                        WidgetOperation.AnimateMargin(
                            view = focusView,
                            fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                            toMarginRight = dpToPx(0),
                            fromMarginLeft = 0,
                            toMarginLeft = 0,
                            duration = 200
                        )
                    }
                )
            ),
            TutorialStep(
                "Onlar",
                null,
                listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(45),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                }
            ),
            TutorialStep(
                "Yüzler",
                null,listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(90),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                }
            ),
            TutorialStep(
                "Binler",
                null,listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(135),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                }
            ),
            TutorialStep(
                "Ve on binler",
                null,listOf {
                    WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = dpToPx(180),
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    )
                }
            ),
            TutorialStep(
                "Her sütunda 5 boncuk vardır.",
                null,listOf(
                    { WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = 0,
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    ) },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(245),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(280),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                }

            ), TutorialStep(
                "Üstteki boncuklar 5'lik değere sahipken",
                null,
                listOf(
                    { WidgetOperation.ChangeMargin(focusView, 0, 0) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline,  // Başka bir view'e bağlamak için
                            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(245),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(60),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                }
            ),
            TutorialStep(
                "Alttaki boncuklar 1'lik değere sahiptir.",
                null,
                listOf(
                    { WidgetOperation.AnimateMargin(
                        view = focusView,
                        fromMarginRight = (focusView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
                        toMarginRight = 0,
                        fromMarginLeft = 0,
                        toMarginLeft = 0,
                        duration = 200
                    ) },
                    {
                        WidgetOperation.ChangeConstraints(
                            view = focusView,
                            topToTop = R.id.guideline2
                            // Diğer constraint parametreleri varsayılan olarak UNSET kalacak
                        )
                    },
                    {
                        WidgetOperation.AnimateSize(
                            view = focusView,
                            fromWidth = focusView.width,
                            toWidth = dpToPx(245),
                            fromHeight = focusView.height,
                            toHeight = dpToPx(180),
                            duration = 400
                        )
                    }
                ),
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                }
            ),
            TutorialStep(
                "Örneğin 1 sayısı abaküste bu şekilde gösterilir.",
                listOf(BeadAnimation(this, "rod4_bead_bottom1", 1)),
                listOf { WidgetOperation.ChangeVisibility(focusView, View.GONE) },
                onStep = { view ->
                    sizeHistory.add(Pair(view.width, view.height))
                    Log.d("Tutorial", "Eklendi: ${view.width} x ${view.height}")
                }
            ),
            TutorialStep(
                "5 sayısı için üstteki boncuk kullanılır.",
                listOf(
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_top", 3)
                    // İhtiyaca göre daha fazla animasyon eklenebilir
                )
            ),
            TutorialStep(
                "6 sayısı için üstteki boncuk ve bir alttaki boncuk kullanılır.",
                listOf(BeadAnimation(this, "rod4_bead_bottom1", 1))  // en sağdaki sütun (4), 6 boncuk
            ),
            TutorialStep(
                "17 sayısı ise böyle gösterilir.",
                listOf(BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1))  // en sağdaki sütun (4), 6 boncuk
            ),
            TutorialStep(
                "51 sayısı böyle.",
                listOf(BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_top", 3),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_top", 4))  // en sağdaki sütun (4), 6 boncuk
            ),
            TutorialStep(
                "126 sayısı ise böyle gösterilir.",
                listOf(BeadAnimation(this, "rod2_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod3_bead_top", 4),
                    BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_top", 3))  // en sağdaki sütun (4), 6 boncuk
            ),
            TutorialStep(
                "Şimdi tahtaya yazacağım sayıları abaküste göstermeye çalış.",
                null
            )

        )
    }

    private fun applyWidgetOperations(operations: List<WidgetOperation>) {
        operations.forEach { operation ->
            when (operation) {
                is WidgetOperation.ChangeVisibility -> {
                    operation.view.visibility = operation.visibility
                }
                is WidgetOperation.ChangeMargin -> {
                    val params = operation.view.layoutParams as ViewGroup.MarginLayoutParams
                    params.rightMargin = operation.marginRight
                    params.leftMargin = operation.marginLeft
                    operation.view.layoutParams = params
                }
                is WidgetOperation.ChangeConstraints -> {
                    val params = operation.view.layoutParams as ConstraintLayout.LayoutParams

                    operation.startToStart?.let { params.startToStart = it }
                    operation.startToEnd?.let { params.startToEnd = it }
                    operation.endToStart?.let { params.endToStart = it }
                    operation.endToEnd?.let { params.endToEnd = it }
                    operation.topToTop?.let { params.topToTop = it }
                    operation.topToBottom?.let { params.topToBottom = it }
                    operation.bottomToTop?.let { params.bottomToTop = it }
                    operation.bottomToBottom?.let { params.bottomToBottom = it }

                    operation.view.layoutParams = params
                }
                is WidgetOperation.AnimateMargin -> {
                    val params = operation.view.layoutParams as ViewGroup.MarginLayoutParams

                    // Sağ margin animasyonu
                    ValueAnimator.ofInt(operation.fromMarginRight, operation.toMarginRight).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.rightMargin = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()
                    }

                    // Sol margin animasyonu
                    ValueAnimator.ofInt(operation.fromMarginLeft, operation.toMarginLeft).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.leftMargin = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()                    }
                }
                is WidgetOperation.AnimateSize -> {
                    val params = operation.view.layoutParams

                    // Width animasyonu
                    ValueAnimator.ofInt(operation.fromWidth, operation.toWidth).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.width = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()                    }

                    // Height animasyonu
                    ValueAnimator.ofInt(operation.fromHeight, operation.toHeight).apply {
                        duration = operation.duration
                        addUpdateListener { animator ->
                            params.height = animator.animatedValue as Int
                            operation.view.layoutParams = params
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                widgetAnimators.remove(this@apply)
                            }
                        })
                        widgetAnimators.add(this)
                        start()                    }
                }
            }
        }
    }
    sealed class WidgetOperation {
        data class ChangeVisibility(val view: View, val visibility: Int) : WidgetOperation()
        data class ChangeMargin(
            val view: View,
            val marginRight: Int,
            val marginLeft: Int
        ) : WidgetOperation()
        data class ChangeConstraints(
            val view: View,
            val startToStart: Int? = null,
            val startToEnd: Int? = null,
            val endToStart: Int? = null,
            val endToEnd: Int? = null,
            val topToTop: Int? = null,
            val topToBottom: Int? = null,
            val bottomToTop: Int? = null,
            val bottomToBottom: Int? = null
        ) : WidgetOperation()
        data class AnimateMargin(
            val view: View,
            val fromMarginRight: Int,
            val toMarginRight: Int,
            val fromMarginLeft: Int,
            val toMarginLeft: Int,
            val duration: Long = 300 // varsayılan animasyon süresi
        ) : WidgetOperation()
        data class AnimateSize(
            val view: View,
            val fromWidth: Int,
            val toWidth: Int,
            val fromHeight: Int,
            val toHeight: Int,
            val duration: Long = 300
        ) : WidgetOperation()
    }
    data class TutorialStep(
        val text: String,
        val animation: List<BeadAnimation>? = null,
        val widgetOperations: List<() -> WidgetOperation>? = null,
        val onStep: ((View) -> Unit)? = null
    )
} 