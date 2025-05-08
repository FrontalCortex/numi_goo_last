package com.example.numigoo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.numigoo.GlobalValues.lessonStep
import com.example.numigoo.databinding.FragmentTutorialBinding
import com.example.numigoo.model.BeadAnimation

class TutorialFragment(private val tutorialNumber: Int = 1) : Fragment() {
    private lateinit var currentTutorialSteps: List<TutorialStep>
    private lateinit var controlButton: View
    private lateinit var correctPanel:View
    private lateinit var incorrectPanel:View
    private var controlNumber=0
    private var answerNumber=0
    private var isAnimating2 = false


    private var oneIsUp = false
    private var twoIsUp = false
    private var threeIsUp = false
    private var fourIsUp = false
    private var topIsDown = false

    // 2. sütun için boolean değişkenler
    private var rod1OneIsUp = false
    private var rod1TwoIsUp = false
    private var rod1ThreeIsUp = false
    private var rod1FourIsUp = false
    private var rod1TopIsDown = false

    // 3. sütun için boolean değişkenler
    private var rod2OneIsUp = false
    private var rod2TwoIsUp = false
    private var rod2ThreeIsUp = false
    private var rod2FourIsUp = false
    private var rod2TopIsDown = false

    // 4. sütun için boolean değişkenler
    private var rod3OneIsUp = false
    private var rod3TwoIsUp = false
    private var rod3ThreeIsUp = false
    private var rod3FourIsUp = false
    private var rod3TopIsDown = false

    // 5. sütun için boolean değişkenler
    private var rod4OneIsUp = false
    private var rod4TwoIsUp = false
    private var rod4ThreeIsUp = false
    private var rod4FourIsUp = false
    private var rod4TopIsDown = false

    private lateinit var rod0BottomBead4: ImageView
    private lateinit var rod0BottomBead3: ImageView
    private lateinit var rod0BottomBead2: ImageView
    private lateinit var rod0BottomBead1: ImageView
    private lateinit var rod0TopBead: ImageView

    // 2. sütun için boncuklar
    private lateinit var rod1BottomBead4: ImageView
    private lateinit var rod1BottomBead3: ImageView
    private lateinit var rod1BottomBead2: ImageView
    private lateinit var rod1BottomBead1: ImageView
    private lateinit var rod1TopBead: ImageView

    // 3. sütun için boncuklar
    private lateinit var rod2BottomBead4: ImageView
    private lateinit var rod2BottomBead3: ImageView
    private lateinit var rod2BottomBead2: ImageView
    private lateinit var rod2BottomBead1: ImageView
    private lateinit var rod2TopBead: ImageView

    // 4. sütun için boncuklar
    private lateinit var rod3BottomBead4: ImageView
    private lateinit var rod3BottomBead3: ImageView
    private lateinit var rod3BottomBead2: ImageView
    private lateinit var rod3BottomBead1: ImageView
    private lateinit var rod3TopBead: ImageView

    // 5. sütun için boncuklar
    private lateinit var rod4BottomBead4: ImageView
    private lateinit var rod4BottomBead3: ImageView
    private lateinit var rod4BottomBead2: ImageView
    private lateinit var rod4BottomBead1: ImageView
    private lateinit var rod4TopBead: ImageView

    private var currentAnimations: MutableList<BeadAnimation> = mutableListOf()
    private val widgetAnimators = mutableListOf<ValueAnimator>()
    private lateinit var binding: FragmentTutorialBinding
    private var currentStep = 0
    private var backOrFront = true
    private lateinit var focusView: View
    private var tutorialSteps: List<TutorialStep> = emptyList()
    private var tutorialSteps2: List<TutorialStep> = emptyList()
    private var tutorialSteps3: List<TutorialStep> = emptyList()
    private var sizeHistory = mutableListOf<Pair<Int, Int>>()
    
    // Tutorial için gerekli state'ler
    private var tutorialControlNumber: Int = 0
    private var tutorialRod4FourIsUp: Boolean = false

    companion object {
        fun newInstance(tutorialNumber: Int): TutorialFragment {
            return TutorialFragment(tutorialNumber)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        findIDs()
        focusView = binding.focusView
        controlButton = binding.kontrolButton
        correctPanel = binding.correctPanel
        incorrectPanel = binding.incorrectPanel
        createTutorialSteps()
        currentTutorialSteps = when (tutorialNumber) {
            1 -> tutorialSteps
            2 -> tutorialSteps2
            else -> tutorialSteps
        }
        setupTutorial()
        setupBackButton()
        setupQuitButton()
    }

    private fun setupTutorial() {
        if(currentStep == currentTutorialSteps.size){
            Log.d("pervane","çal")
            return
        }
        // İlk adımı göster
        showStep(currentStep)

        // Ekrana tıklama ile ilerleme
        binding.root.setOnClickListener {
            //tıklandıktan sonra index'lerin devam etmemesi için buraya || currentStep == tutorialStep.size
            if (isAnyAnimationRunning()) {
                return@setOnClickListener
            }

            //ekrana tıkladıktan sonra sıradaki index'in nextStepAvailable değeri false ise index artışı kontol ile
            //gerçekleşecek


            if (currentStep < currentTutorialSteps.size - 1 && getCurrentStep().nextStepAvailable) {
                currentStep++
                getCurrentStep().onStepComplete?.invoke()
                Log.d("Fonk","Çalıştı")
                backOrFront = true
                showStep(currentStep)
            }
            else{
                backOrFront = true
                showStep(currentStep)
        }
        }
    }
    //mevcut adımdaki TutorialStep'i verir
    private fun getCurrentStep(): TutorialStep {
        return currentTutorialSteps[currentStep]
    }

    private fun showStep(position: Int) {
        val step = currentTutorialSteps[position]
        binding.tutorialText.text = step.text
        binding.questionText.text = step.questionText
        binding.questionText.visibility = step.questionTextVisibility
        answerNumber=step.answerNumber
        setupBeads()
        if(getCurrentStep().abacusClickable){
            controlButton.visibility = View.VISIBLE
            controlButton.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(0.85f)
                            .scaleY(0.85f)
                            .setDuration(100)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(400)
                            .setInterpolator(BounceInterpolator())
                            .start()

                        // Tıklama işlemini gerçekleştir
                        showResultPanel()
                        controlNumber = 0
                        true
                    }

                    else -> false
                }
            }
        }
        step.widgetOperations?.let { operations ->
            applyWidgetOperations(operations.map { it() })
        }
        //tutorialStep bittiyse lessonStep değerine sahip Abacus'ü yükler
        if (position == currentTutorialSteps.size - 1) {
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
            currentTutorialSteps[position+1].animation?.forEach { originalAnimation ->
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
            currentTutorialSteps[position].widgetOperations?.forEach { opLambda ->
                val op = opLambda()
                if (op is WidgetOperation.AnimateMargin) {
                    applyWidgetOperations(listOf(op))
                }
            }
            if (sizeHistory.size > 0 && currentTutorialSteps[position+1].onStep != null) {
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
            val nextStep = currentTutorialSteps.getOrNull(position + 1)

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
                disableAllClickable(binding.abacusLinear)
                currentStep--
                backOrFront = false
                resetAbacus()
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
            currentStep=0
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
        tutorialSteps2 = listOf(
            TutorialStep(
            "Bu derste çok büyük sayıları bile ne kadar kolay toplayabileceğini öğreneceksin.",
            null
        ),
            TutorialStep(
                "Kuralsız Toplama",
                null
            ),
            TutorialStep(
                text = "Bu işlemle başlayalım.",
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE
            ),
            TutorialStep(
                "ilk önce abaküse 3 yazıyoruz.",
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(BeadAnimation(this, "rod4_bead_bottom1", 1),
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1))

            ),
            TutorialStep(
                "Sonrasında toplamak için 5'i ekleriz.",
                questionText = "3 + 5",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 3))

            ),
            TutorialStep(
                "Cevap 8."
            ),
            TutorialStep(
                "Şimdi beraber bu örneği yapalım.",
                questionText = "2 + 6",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_top", 4),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2))

            ),
            TutorialStep(
                "ilk önce abaküse 2 yazalım.",
                questionTextVisibility = View.VISIBLE,
                questionText = "2 + 6",
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 2
            ),TutorialStep(
                "Şimdi de 6'yı ekleyelim.",
                questionText = "2 + 6",
                questionTextVisibility = View.VISIBLE,
                answerNumber = 8,
                abacusClickable = true,
                nextStepAvailable = false,


            ),TutorialStep(
                "Çabuk öğreniyorsun.",
                onStepComplete = { resetAbacus() },

            ),
            TutorialStep(
                "İki basamaklı sayılar toplanırken, sayılar en büyük basamaktan başlanarak toplanır."
            ),TutorialStep(
                "Mesela bu işleme bakalım.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,


            ),TutorialStep(
                "ilk önce 21 yazıyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE

            ),TutorialStep(
                "ilk önce 21 yazıyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(BeadAnimation(this, "rod3_bead_bottom1", 1),
                    BeadAnimation(this, "rod3_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom1", 1))


            ),TutorialStep(
                "Sonrasında, 13'ü eklemek için önce 10'u onlar basamağına, sonra da 3'ü birler basamağına ekliyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Sonrasında, 13'ü eklemek için önce 10'u onlar basamağına, sonra da 3'ü birler basamağına ekliyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE,
                animation = listOf(BeadAnimation(this, "rod3_bead_bottom3", 1))

        ),TutorialStep(
                "Sonrasında, 13'ü eklemek için önce 10'u onlar basamağına, sonra da 3'ü birler basamağına ekliyoruz.",
                questionText = "21 + 13",
                questionTextVisibility = View.VISIBLE ,
                animation = listOf(
                    BeadAnimation(this, "rod4_bead_bottom2", 1),
                    BeadAnimation(this, "rod4_bead_bottom3", 1),
                    BeadAnimation(this, "rod4_bead_bottom4", 1))
            ), TutorialStep(
                "Cevap 34."
            ),
            TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
                animation = listOf(
                    BeadAnimation(this, "rod3_bead_bottom3", 2),
                    BeadAnimation(this, "rod3_bead_bottom1", 2),
                    BeadAnimation(this, "rod3_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom1", 2),
                    BeadAnimation(this, "rod4_bead_bottom2", 2),
                    BeadAnimation(this, "rod4_bead_bottom3", 2),
                    BeadAnimation(this, "rod4_bead_bottom4", 2))

            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "ilk önce 16 yazalım.",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 16


            ),TutorialStep(
                "Sonrasında, 21'i eklemek için önce 20'yi onlar basamağına ekleyelim",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 36
            ),TutorialStep(
                "sonra da 1'i birler basamağına ekleyelim.",
                questionText = "16 + 21",
                questionTextVisibility = View.VISIBLE,
                nextStepAvailable = false,
                abacusClickable = true,
                answerNumber = 37
            ),
            TutorialStep(
                "Çok güzel. Hemen kaptın."

            ),TutorialStep(
                "Şimdi kendini deneme zamanı."
            ),TutorialStep(
                "Bakalım senin için hazırladığım testi çözebilecek misin ?"
            ))
        tutorialSteps3 = listOf(
            TutorialStep(
                "Bu derste kurallı toplamanın ilk dersi olan 5'lik toplamayı öğreneceksin.",

            ),
            TutorialStep(
                "5'lik toplama",

            ),TutorialStep(
                "5’lik toplama kuralında ekleyeceğimiz sayıların 5’e tamamlayan kardeşleri vardır.",

            ),TutorialStep(
                "1’in kardeşi 4’tür.",

            ),TutorialStep(
                "2’nin kardeşi 3",

            ),TutorialStep(
                "3’ün kardeşi 2",

            ),TutorialStep(
                "4’ün kardeşi 1’dir",

            ),TutorialStep(
                "Örneğin bu işlemi yapmaya çalışalım.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE

            ),TutorialStep(
                "İlk olarak abaküse ilk sayıyı yazıyoruz.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE

            ),TutorialStep(
                "Sonrasında ekleyeceğimiz ikinci sayıyı yazacağız.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Ama 1'i doğrudan ekleyemiyoruz.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "O yüzden burada 5'lik toplama kuralını uygulayacağız.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "1'in kardeşi 4'tür",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 4 gider.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 4 gider.",
                questionText = "4 + 1",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Cevap 5.",
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Önce abaküse 4'ü yazıyoruz.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Sonrasında 2'yi ekleyeceğiz. Ama 2'yi doğrudan ekleyemiyoruz.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "2'nin kardeşi 3'tür.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 3 gider.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 3 gider.",questionText = "4 + 2",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Cevap 6.",
            ),TutorialStep(
                "Bu işlemi ele alalım.",
                questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Önce abaküse  3'ü yazıyoruz.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Sonrasında 3'ü ekleyeceğiz. Ama 3'ü doğrudan ekleyemiyoruz.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "3'nin kardeşi 2'dir.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 2 gider.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 2 gider.",questionText = "3 + 3",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Cevap 6.",
            ),TutorialStep(
                "Son olarak bu işlemi ele alalım.",
                questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Önce abaküse  2'yi yazıyoruz.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Sonrasında 4'ü ekleyeceğiz. Ama 4'ü doğrudan ekleyemiyoruz.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "4'ün kardeşi 1'dir.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Cevap 6.",questionText = "2 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Bu kural her basamak için aynıdır.",
            ),TutorialStep(
                "Bu işlemde de aynı kuralı uygularız.",
                questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE

            ),TutorialStep(
                "30'u abaküse yazıyoruz.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "40'ı doğrudan ekleyemiyoruz.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Bu yüzden 5'lik kuralı uygulayacağız.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "4'ün kardeşi 1'dir.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Cevap 70.",questionText = "30 + 40",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Beraber örnek yaparak pekiştirelim.",
            ),TutorialStep(
                "Önce abaküse ilk sayıyı yazalım.",
                questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE

            ),TutorialStep(
                "Sonrasında 4'ü ekleyelim.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Doğrudan ekleyemediğimiz için 5'lik kural uygulayacağız.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "4'ün kardeşi 1'dir.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "5 gelir.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Kardeşi 1 gider.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Ve cevap 8.",questionText = "4 + 4",
                questionTextVisibility = View.VISIBLE
            ),TutorialStep(
                "Şimdi öğrendiklerini uygulama zamanı.",
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
        val onStep: ((View) -> Unit)? = null,
        val questionText: String? = null,
        val questionTextVisibility: Int = View.INVISIBLE,
        var nextStepAvailable: Boolean = true,
        var abacusClickable:Boolean = false,
        val answerNumber: Int = 0,
        val onStepComplete: (() -> Unit)? = null  // Yeni eklenen fonksiyon parametresi
    )
    private fun stepAnswerAlgorithm(): Boolean {

        abacusNumberReturn()
        return if (controlNumber == answerNumber) {
            controlNumber = 0
            true
        } else {
            controlNumber = 0
            false
        }
    }

    private fun showResultPanel() {



        if (stepAnswerAlgorithm()) {
            // Doğru cevap durumu


            correctPanel.translationY = correctPanel.height.toFloat()
            correctPanel.visibility = View.VISIBLE
            correctPanel.alpha = 0f

            // Overlay'i görünür yap
            binding.root.findViewById<View>(R.id.overlay).visibility = View.VISIBLE

            correctPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            correctPanel.findViewById<Button>(R.id.continueButton)
                .setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate()
                                .scaleX(0.85f)
                                .scaleY(0.85f)
                                .setDuration(100)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                            currentStep++
                            if(!getCurrentStep().abacusClickable){
                                Log.d("fonk","sososos")
                                getCurrentStep().onStepComplete?.invoke()

                            }
                            controlButton.visibility= View.GONE
                            showStep(currentStep)
                            true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(BounceInterpolator())
                                .start()
                            binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                            correctPanel.animate()
                                .translationY(correctPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    correctPanel.visibility = View.GONE
                                }
                                .start()
                            true
                        }

                        else -> false
                    }
                }
        } else {
            // Yanlış cevap durumu
            incorrectPanel.translationY = incorrectPanel.height.toFloat()
            incorrectPanel.visibility = View.VISIBLE
            incorrectPanel.alpha = 0f
            binding.root.findViewById<View>(R.id.overlay).visibility = View.VISIBLE
            incorrectPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            incorrectPanel.findViewById<Button>(R.id.okayButton)
                .setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate()
                                .scaleX(0.85f)
                                .scaleY(0.85f)
                                .setDuration(100)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                            true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(BounceInterpolator())
                                .start()
                            binding.root.findViewById<View>(R.id.overlay).visibility = View.GONE
                            incorrectPanel.animate()
                                .translationY(incorrectPanel.height.toFloat())
                                .setDuration(200)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction {
                                    incorrectPanel.visibility = View.GONE
                                }
                                .start()

                            true
                        }

                        else -> false
                    }
                }
        }
    }


    private fun findIDs() {
        // 1. sütun için boncukları bul
        rod0BottomBead4 = binding.rod0BeadBottom4
        rod0BottomBead3 = binding.rod0BeadBottom3
        rod0BottomBead2 = binding.rod0BeadBottom2
        rod0BottomBead1 = binding.rod0BeadBottom1
        rod0TopBead = binding.rod0BeadTop

        // 2. sütun için boncukları bul
        rod1BottomBead4 = binding.rod1BeadBottom4
        rod1BottomBead3 = binding.rod1BeadBottom3
        rod1BottomBead2 = binding.rod1BeadBottom2
        rod1BottomBead1 = binding.rod1BeadBottom1
        rod1TopBead = binding.rod1BeadTop

        // 3. sütun için boncukları bul
        rod2BottomBead4 = binding.rod2BeadBottom4
        rod2BottomBead3 = binding.rod2BeadBottom3
        rod2BottomBead2 = binding.rod2BeadBottom2
        rod2BottomBead1 = binding.rod2BeadBottom1
        rod2TopBead = binding.rod2BeadTop

        // 4. sütun için boncukları bul
        rod3BottomBead4 = binding.rod3BeadBottom4
        rod3BottomBead3 = binding.rod3BeadBottom3
        rod3BottomBead2 = binding.rod3BeadBottom2
        rod3BottomBead1 = binding.rod3BeadBottom1
        rod3TopBead = binding.rod3BeadTop

        // 5. sütun için boncukları bul
        rod4BottomBead4 = binding.rod4BeadBottom4
        rod4BottomBead3 = binding.rod4BeadBottom3
        rod4BottomBead2 = binding.rod4BeadBottom2
        rod4BottomBead1 = binding.rod4BeadBottom1
        rod4TopBead = binding.rod4BeadTop
    }

    private fun abacusNumberReturn() {
        if (rod4FourIsUp) {
            controlNumber += 4
        } else if (rod4ThreeIsUp) {
            controlNumber += 3
        } else if (rod4TwoIsUp) {
            controlNumber += 2
        } else if (rod4OneIsUp) {
            controlNumber += 1
        }
        if (rod4TopIsDown) {
            controlNumber += 5
        }
        if (rod3FourIsUp) {
            controlNumber += 40
        } else if (rod3ThreeIsUp) {
            controlNumber += 30
        } else if (rod3TwoIsUp) {
            controlNumber += 20
        } else if (rod3OneIsUp) {
            controlNumber += 10
        }
        if (rod3TopIsDown) {
            controlNumber += 50
        }
        if (rod2FourIsUp) {
            controlNumber += 400
        } else if (rod2ThreeIsUp) {
            controlNumber += 300
        } else if (rod2TwoIsUp) {
            controlNumber += 200
        } else if (rod2OneIsUp) {
            controlNumber += 100
        }
        if (rod2TopIsDown) {
            controlNumber += 500
        }
        if (rod1FourIsUp) {
            controlNumber += 4000
        } else if (rod1ThreeIsUp) {
            controlNumber += 3000
        } else if (rod1TwoIsUp) {
            controlNumber += 2000
        } else if (rod1OneIsUp) {
            controlNumber += 1000
        }
        if (rod1TopIsDown) {
            controlNumber += 5000
        }
        if (fourIsUp) {
            controlNumber += 40000
        } else if (threeIsUp) {
            controlNumber += 30000
        } else if (twoIsUp) {
            controlNumber += 20000
        } else if (oneIsUp) {
            controlNumber += 10000
        }
        if (topIsDown) {
            controlNumber += 50000
        }

    }

    private fun setupBeads() {
        if (!getCurrentStep().abacusClickable) {
            Log.d("üşüdüm","ses")
            // Tüm alt view'lerin tıklanabilirliğini engelle
            disableAllClickable(binding.abacusLinear)
            // Kontrol butonunu gizle
            controlButton.visibility = View.GONE
            return
        }

        // Tüm alt view'lerin tıklanabilirliğini etkinleştir
        enableAllClickable(binding.abacusLinear)
        controlButton.visibility = View.VISIBLE
        binding.abacusLinear.isClickable = true
        binding.abacusLinear.isFocusable = true

        // Boncuklara tıklama işlemleri
        rod0BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!fourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (!threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (!twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (!oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        fourIsUp = true
                        threeIsUp = true
                        twoIsUp = true
                        oneIsUp = true
                        updateRod0BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod0BottomBead4)
                    fourIsUp = false
                    updateRod0BeadsAppearance()
                }
            }
        }

        rod0BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!threeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (!twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (!oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        threeIsUp = true
                        twoIsUp = true
                        oneIsUp = true
                        updateRod0BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    // Yukarıda olan boncukları kontrol et ve aşağı indir
                    if (fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (threeIsUp) beadsToAnimate.add(rod0BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (fourIsUp) fourIsUp = false
                        threeIsUp = false
                        updateRod0BeadsAppearance()
                    }
                }
            }
        }

        rod0BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!twoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (!oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        twoIsUp = true
                        oneIsUp = true
                        updateRod0BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    // Yukarıda olan boncukları kontrol et ve aşağı indir
                    if (fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (twoIsUp) beadsToAnimate.add(rod0BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (fourIsUp) fourIsUp = false
                        if (threeIsUp) threeIsUp = false
                        twoIsUp = false
                        updateRod0BeadsAppearance()
                    }
                }
            }
        }

        rod0BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!oneIsUp) {
                    animateBeadsUp(rod0BottomBead1)
                    oneIsUp = true
                    updateRod0BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (fourIsUp) beadsToAnimate.add(rod0BottomBead4)
                    if (threeIsUp) beadsToAnimate.add(rod0BottomBead3)
                    if (twoIsUp) beadsToAnimate.add(rod0BottomBead2)
                    if (oneIsUp) beadsToAnimate.add(rod0BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (fourIsUp) fourIsUp = false
                        if (threeIsUp) threeIsUp = false
                        if (twoIsUp) twoIsUp = false
                        oneIsUp = false
                        updateRod0BeadsAppearance()
                    }
                }
            }
        }

        // 1. sütun üst boncuk
        rod0TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!topIsDown) {
                    animateBeadDown(rod0TopBead)
                    topIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod0TopBead)
                    topIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 2. sütun için click listener'lar
        rod1BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (!rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (!rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (!rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod1FourIsUp = true
                        rod1ThreeIsUp = true
                        rod1TwoIsUp = true
                        rod1OneIsUp = true
                        updateRod1BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod1BottomBead4)
                    rod1FourIsUp = false
                    updateRod1BeadsAppearance()
                }
            }
        }

        rod1BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (!rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (!rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod1ThreeIsUp = true
                        rod1TwoIsUp = true
                        rod1OneIsUp = true
                        updateRod1BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod1FourIsUp) rod1FourIsUp = false
                        rod1ThreeIsUp = false
                        updateRod1BeadsAppearance()
                    }
                }
            }
        }

        rod1BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (!rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod1TwoIsUp = true
                        rod1OneIsUp = true
                        updateRod1BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod1FourIsUp) rod1FourIsUp = false
                        if (rod1ThreeIsUp) rod1ThreeIsUp = false
                        rod1TwoIsUp = false
                        updateRod1BeadsAppearance()
                    }
                }
            }
        }

        rod1BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1OneIsUp) {
                    animateBeadsUp(rod1BottomBead1)
                    rod1OneIsUp = true
                    updateRod1BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod1FourIsUp) beadsToAnimate.add(rod1BottomBead4)
                    if (rod1ThreeIsUp) beadsToAnimate.add(rod1BottomBead3)
                    if (rod1TwoIsUp) beadsToAnimate.add(rod1BottomBead2)
                    if (rod1OneIsUp) beadsToAnimate.add(rod1BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod1FourIsUp) rod1FourIsUp = false
                        if (rod1ThreeIsUp) rod1ThreeIsUp = false
                        if (rod1TwoIsUp) rod1TwoIsUp = false
                        rod1OneIsUp = false
                        updateRod1BeadsAppearance()
                    }
                }
            }
        }

        // 2. sütun üst boncuk
        rod1TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod1TopIsDown) {
                    animateBeadDown(rod1TopBead)
                    rod1TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod1TopBead)
                    rod1TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 3. sütun için click listener'lar
        rod2BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (!rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (!rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (!rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod2FourIsUp = true
                        rod2ThreeIsUp = true
                        rod2TwoIsUp = true
                        rod2OneIsUp = true
                        updateRod2BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod2BottomBead4)
                    rod2FourIsUp = false
                    updateRod2BeadsAppearance()
                }
            }
        }

        rod2BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (!rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (!rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod2ThreeIsUp = true
                        rod2TwoIsUp = true
                        rod2OneIsUp = true
                        updateRod2BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod2FourIsUp) rod2FourIsUp = false
                        rod2ThreeIsUp = false
                        updateRod2BeadsAppearance()
                    }
                }
            }
        }

        rod2BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (!rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod2TwoIsUp = true
                        rod2OneIsUp = true
                        updateRod2BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod2FourIsUp) rod2FourIsUp = false
                        if (rod2ThreeIsUp) rod2ThreeIsUp = false
                        rod2TwoIsUp = false
                        updateRod2BeadsAppearance()
                    }
                }
            }
        }

        rod2BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2OneIsUp) {
                    animateBeadsUp(rod2BottomBead1)
                    rod2OneIsUp = true
                    updateRod2BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod2FourIsUp) beadsToAnimate.add(rod2BottomBead4)
                    if (rod2ThreeIsUp) beadsToAnimate.add(rod2BottomBead3)
                    if (rod2TwoIsUp) beadsToAnimate.add(rod2BottomBead2)
                    if (rod2OneIsUp) beadsToAnimate.add(rod2BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod2FourIsUp) rod2FourIsUp = false
                        if (rod2ThreeIsUp) rod2ThreeIsUp = false
                        if (rod2TwoIsUp) rod2TwoIsUp = false
                        rod2OneIsUp = false
                        updateRod2BeadsAppearance()
                    }
                }
            }
        }

        // 3. sütun üst boncuk
        rod2TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod2TopIsDown) {
                    animateBeadDown(rod2TopBead)
                    rod2TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod2TopBead)
                    rod2TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 4. sütun için click listener'lar
        rod3BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (!rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (!rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (!rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod3FourIsUp = true
                        rod3ThreeIsUp = true
                        rod3TwoIsUp = true
                        rod3OneIsUp = true
                        updateRod3BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod3BottomBead4)
                    rod3FourIsUp = false
                    updateRod3BeadsAppearance()
                }
            }
        }

        rod3BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (!rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (!rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod3ThreeIsUp = true
                        rod3TwoIsUp = true
                        rod3OneIsUp = true
                        updateRod3BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod3FourIsUp) rod3FourIsUp = false
                        rod3ThreeIsUp = false
                        updateRod3BeadsAppearance()
                    }
                }
            }
        }

        rod3BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (!rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod3TwoIsUp = true
                        rod3OneIsUp = true
                        updateRod3BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod3FourIsUp) rod3FourIsUp = false
                        if (rod3ThreeIsUp) rod3ThreeIsUp = false
                        rod3TwoIsUp = false
                        updateRod3BeadsAppearance()
                    }
                }
            }
        }

        rod3BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3OneIsUp) {
                    animateBeadsUp(rod3BottomBead1)
                    rod3OneIsUp = true
                    updateRod3BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod3FourIsUp) beadsToAnimate.add(rod3BottomBead4)
                    if (rod3ThreeIsUp) beadsToAnimate.add(rod3BottomBead3)
                    if (rod3TwoIsUp) beadsToAnimate.add(rod3BottomBead2)
                    if (rod3OneIsUp) beadsToAnimate.add(rod3BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod3FourIsUp) rod3FourIsUp = false
                        if (rod3ThreeIsUp) rod3ThreeIsUp = false
                        if (rod3TwoIsUp) rod3TwoIsUp = false
                        rod3OneIsUp = false
                        updateRod3BeadsAppearance()
                    }
                }
            }
        }

        // 4. sütun üst boncuk
        rod3TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod3TopIsDown) {
                    animateBeadDown(rod3TopBead)
                    rod3TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod3TopBead)
                    rod3TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }

        // 5. sütun için click listener'lar
        rod4BottomBead4.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4FourIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (!rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (!rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (!rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod4FourIsUp = true
                        rod4ThreeIsUp = true
                        rod4TwoIsUp = true
                        rod4OneIsUp = true
                        updateRod4BeadsAppearance()
                    }
                } else {
                    animateBeadsDown(rod4BottomBead4)
                    rod4FourIsUp = false
                    updateRod4BeadsAppearance()
                }
            }
        }

        rod4BottomBead3.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4ThreeIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (!rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (!rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod4ThreeIsUp = true
                        rod4TwoIsUp = true
                        rod4OneIsUp = true
                        updateRod4BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod4FourIsUp) rod4FourIsUp = false
                        rod4ThreeIsUp = false
                        updateRod4BeadsAppearance()
                    }
                }
            }
        }

        rod4BottomBead2.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4TwoIsUp) {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (!rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (!rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsUp(*beadsToAnimate.toTypedArray())
                        rod4TwoIsUp = true
                        rod4OneIsUp = true
                        updateRod4BeadsAppearance()
                    }
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod4FourIsUp) rod4FourIsUp = false
                        if (rod4ThreeIsUp) rod4ThreeIsUp = false
                        rod4TwoIsUp = false
                        updateRod4BeadsAppearance()
                    }
                }
            }
        }

        rod4BottomBead1.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4OneIsUp) {
                    animateBeadsUp(rod4BottomBead1)
                    rod4OneIsUp = true
                    updateRod4BeadsAppearance()
                } else {
                    val beadsToAnimate = mutableListOf<ImageView>()
                    if (rod4FourIsUp) beadsToAnimate.add(rod4BottomBead4)
                    if (rod4ThreeIsUp) beadsToAnimate.add(rod4BottomBead3)
                    if (rod4TwoIsUp) beadsToAnimate.add(rod4BottomBead2)
                    if (rod4OneIsUp) beadsToAnimate.add(rod4BottomBead1)

                    if (beadsToAnimate.isNotEmpty()) {
                        animateBeadsDown(*beadsToAnimate.toTypedArray())
                        if (rod4FourIsUp) rod4FourIsUp = false
                        if (rod4ThreeIsUp) rod4ThreeIsUp = false
                        if (rod4TwoIsUp) rod4TwoIsUp = false
                        rod4OneIsUp = false
                        updateRod4BeadsAppearance()
                    }
                }
            }
        }

        // 5. sütun üst boncuk
        rod4TopBead.setOnClickListener {
            if (!isAnimating2) {
                if (!rod4TopIsDown) {
                    animateBeadDown(rod4TopBead)
                    rod4TopIsDown = true
                    updateTopBeadsAppearance()
                } else {
                    animateBeadUp(rod4TopBead)
                    rod4TopIsDown = false
                    updateTopBeadsAppearance()
                }
            }
        }
    }

    private fun animateBeadsUp(vararg beads: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L // milisaniye cinsinden
        val moveDistance = 135 // piksel cinsinden

        beads.forEach { bead ->
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

                    // Son boncuk animasyonu bittiğinde isAnimating'i false yap
                    if (bead == beads.last()) {
                        isAnimating2 = false
                    }
                }
                .translationY(-moveDistance.toFloat())
                .start()
        }
    }

    private fun animateBeadDown(bead: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L
        val moveDistance = 90

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withStartAction {
                // Animasyon başlamadan önce yapılacak işlemler
            }
            .withEndAction {
                // Animasyon bittiğinde isAnimating'i false yap
                isAnimating2 = false
            }
            .translationY(moveDistance.toFloat())
            .start()
    }

    private fun animateBeadUp(bead: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L
        val moveDistance = 90

        bead.animate()
            .setDuration(animationDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withStartAction {
                // Animasyon başlamadan önce yapılacak işlemler
            }
            .withEndAction {
                // Animasyon bittiğinde isAnimating'i false yap
                isAnimating2 = false
            }
            .translationY(0f)  // Orijinal konumuna dön
            .start()
    }

    private fun animateBeadsDown(vararg beads: ImageView) {
        isAnimating2 = true
        val animationDuration = 300L
        val moveDistance = 135

        beads.forEach { bead ->
            val params = bead.layoutParams as ViewGroup.MarginLayoutParams
            val startMargin = params.bottomMargin
            val endMargin = startMargin - moveDistance  // Yukarı çıktığı mesafe kadar aşağı in

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

                    // Son boncuk animasyonu bittiğinde isAnimating'i false yap
                    if (bead == beads.last()) {
                        isAnimating2 = false
                    }
                }
                .translationY(moveDistance.toFloat())  // Aşağı doğru hareket
                .start()
        }
    }

    // Boncukların görünümünü güncelleyen fonksiyon
    private fun updateBeadAppearance(bead: ImageView, isSelected: Boolean) {
        val resourceId = if (isSelected) {
            Log.d("debugs","123")
            resources.getIdentifier(
                "soroban_bead_selected",
                "drawable",
                requireContext().packageName
            )
        }
         else{
            Log.d("debugs","1234")

            resources.getIdentifier("soroban_bead", "drawable", requireContext().packageName)

        }
        bead.setImageResource(resourceId)
    }

    // 1. sütun için görünüm güncellemeleri
    private fun updateRod0BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod0_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod0_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod0_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod0_bead_bottom4)


            updateBeadAppearance(bottomBead1, oneIsUp)
            updateBeadAppearance(bottomBead2, twoIsUp)
            updateBeadAppearance(bottomBead3, threeIsUp)
            updateBeadAppearance(bottomBead4, fourIsUp)
        }
    }

    // 2. sütun için görünüm güncellemeleri
    private fun updateRod1BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod1_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod1_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod1_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod1_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod1OneIsUp)
            updateBeadAppearance(bottomBead2, rod1TwoIsUp)
            updateBeadAppearance(bottomBead3, rod1ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod1FourIsUp)
        }
    }

    // 3. sütun için görünüm güncellemeleri
    private fun updateRod2BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod2_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod2_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod2_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod2_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod2OneIsUp)
            updateBeadAppearance(bottomBead2, rod2TwoIsUp)
            updateBeadAppearance(bottomBead3, rod2ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod2FourIsUp)
        }
    }

    // 4. sütun için görünüm güncellemeleri
    private fun updateRod3BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod3_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod3_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod3_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod3_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod3OneIsUp)
            updateBeadAppearance(bottomBead2, rod3TwoIsUp)
            updateBeadAppearance(bottomBead3, rod3ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod3FourIsUp)
        }
    }

    // 5. sütun için görünüm güncellemeleri
    private fun updateRod4BeadsAppearance() {
        view?.let { view ->
            val bottomBead1 = view.findViewById<ImageView>(R.id.rod4_bead_bottom1)
            val bottomBead2 = view.findViewById<ImageView>(R.id.rod4_bead_bottom2)
            val bottomBead3 = view.findViewById<ImageView>(R.id.rod4_bead_bottom3)
            val bottomBead4 = view.findViewById<ImageView>(R.id.rod4_bead_bottom4)

            updateBeadAppearance(bottomBead1, rod4OneIsUp)
            updateBeadAppearance(bottomBead2, rod4TwoIsUp)
            updateBeadAppearance(bottomBead3, rod4ThreeIsUp)
            updateBeadAppearance(bottomBead4, rod4FourIsUp)
        }
    }

    // Üst boncukların görünümünü güncelleyen fonksiyonlar
    private fun updateTopBeadsAppearance() {

            updateBeadAppearance(rod0TopBead, topIsDown)
            updateBeadAppearance(rod1TopBead, rod1TopIsDown)
            updateBeadAppearance(rod2TopBead, rod2TopIsDown)
            updateBeadAppearance(rod3TopBead, rod3TopIsDown)
            updateBeadAppearance(rod4TopBead, rod4TopIsDown)

    }

    private fun resetAbacus() {
        // 1. sütun için
        Log.d("fonk","buda")
        if (fourIsUp) {
            animateBeadsDown(rod0BottomBead4)
            fourIsUp = false
        }
        if (threeIsUp) {
            animateBeadsDown(rod0BottomBead3)
            threeIsUp = false
        }
        if (twoIsUp) {
            animateBeadsDown(rod0BottomBead2)
            twoIsUp = false
        }
        if (oneIsUp) {
            animateBeadsDown(rod0BottomBead1)
            oneIsUp = false
        }
        if (topIsDown) {
            animateBeadUp(rod0TopBead)
            topIsDown = false
        }
        updateRod0BeadsAppearance()

        // 2. sütun için
        if (rod1FourIsUp) {
            animateBeadsDown(rod1BottomBead4)
            rod1FourIsUp = false
        }
        if (rod1ThreeIsUp) {
            animateBeadsDown(rod1BottomBead3)
            rod1ThreeIsUp = false
        }
        if (rod1TwoIsUp) {
            animateBeadsDown(rod1BottomBead2)
            rod1TwoIsUp = false
        }
        if (rod1OneIsUp) {
            animateBeadsDown(rod1BottomBead1)
            rod1OneIsUp = false
        }
        if (rod1TopIsDown) {
            animateBeadUp(rod1TopBead)
            rod1TopIsDown = false
        }
        updateRod1BeadsAppearance()

        // 3. sütun için
        if (rod2FourIsUp) {
            animateBeadsDown(rod2BottomBead4)
            rod2FourIsUp = false
        }
        if (rod2ThreeIsUp) {
            animateBeadsDown(rod2BottomBead3)
            rod2ThreeIsUp = false
        }
        if (rod2TwoIsUp) {
            animateBeadsDown(rod2BottomBead2)
            rod2TwoIsUp = false
        }
        if (rod2OneIsUp) {
            animateBeadsDown(rod2BottomBead1)
            rod2OneIsUp = false
        }
        if (rod2TopIsDown) {
            animateBeadUp(rod2TopBead)
            rod2TopIsDown = false
        }
        updateRod2BeadsAppearance()

        // 4. sütun için
        if (rod3FourIsUp) {
            animateBeadsDown(rod3BottomBead4)
            rod3FourIsUp = false
        }
        if (rod3ThreeIsUp) {
            animateBeadsDown(rod3BottomBead3)
            rod3ThreeIsUp = false
        }
        if (rod3TwoIsUp) {
            animateBeadsDown(rod3BottomBead2)
            rod3TwoIsUp = false
        }
        if (rod3OneIsUp) {
            animateBeadsDown(rod3BottomBead1)
            rod3OneIsUp = false
        }
        if (rod3TopIsDown) {
            animateBeadUp(rod3TopBead)
            rod3TopIsDown = false
        }
        updateRod3BeadsAppearance()

        // 5. sütun için
        if (rod4FourIsUp) {
            animateBeadsDown(rod4BottomBead4)
            rod4FourIsUp = false
        }
        if (rod4ThreeIsUp) {
            animateBeadsDown(rod4BottomBead3)
            rod4ThreeIsUp = false
        }
        if (rod4TwoIsUp) {
            animateBeadsDown(rod4BottomBead2)
            rod4TwoIsUp = false
        }
        if (rod4OneIsUp) {
            animateBeadsDown(rod4BottomBead1)
            rod4OneIsUp = false
        }
        if (rod4TopIsDown) {
            animateBeadUp(rod4TopBead)
            rod4TopIsDown = false
        }
        updateRod4BeadsAppearance()
        updateTopBeadsAppearance()
    }

    private fun disableAllClickable(view: View) {
        view.isClickable = false
        view.isFocusable = false
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                disableAllClickable(view.getChildAt(i))
            }
        }
    }

    private fun enableAllClickable(view: View) {
        view.isClickable = true
        view.isFocusable = true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                enableAllClickable(view.getChildAt(i))
            }
        }
    }
} 