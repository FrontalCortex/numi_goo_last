package com.example.numigoo

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.example.numigoo.GlobalValues.lessonStep
import com.example.numigoo.GlobalValues.mapFragmentStepIndex
import com.example.numigoo.MapFragment.Companion.getLessonOperationsBlinding
import com.example.numigoo.databinding.FragmentBlindingLessonBinding
import com.example.numigoo.model.LessonItem
import com.example.numigoo.model.RulesFragment

class BlindingLessonFragment : Fragment() {
    private var operations: List<Any> = emptyList()
    private lateinit var numberText: TextView
    private var currentIndex = 0
    private var answerNumber = 0
    private var lastClickTime = 0L
    private var controlNumber = 0
    private var correctAnswer = 0
    private var totalQuestions = 0
    private lateinit var firstNumberText: TextView
    private lateinit var firstNumberText2: TextView
    private lateinit var operatorText: TextView
    private lateinit var secondNumberText: TextView
    private lateinit var correctAnswerText: TextView
    private lateinit var controlButton: Button
    private lateinit var incorrectPanel: View
    private lateinit var correctPanel: View
    private lateinit var lottieView: LottieAnimationView

    private lateinit var fabHint: LottieAnimationView
    private lateinit var tvHint: TextView
    private var isHintVisible = false
    private lateinit var fabHintTouchArea: View
    private lateinit var lessonItem : LessonItem
    private lateinit var rulesBookButton: ImageView
    private var resultDialog: Dialog? = null

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private lateinit var timerTextView: TextView
    private var isTimerStarted = false
    private lateinit var binding: FragmentBlindingLessonBinding
    private var currentTime: String = "0:00"


    private var isShowingSequence = false
    private var currentSequenceIndex = 0
    private var currentSequence: List<Int> = emptyList()
    private val showNextNumberRunnable = object : Runnable {
        override fun run() {
            if (currentSequenceIndex < currentSequence.size) {
                // Önce text'i boşalt
                numberText.text = ""

                handler.postDelayed({
                    val currentNumber = currentSequence[currentSequenceIndex]
                    numberText.text = currentNumber.toString()
                    currentSequenceIndex++

                    // Son sayı değilse bekle, son sayıysa bekleme!
                    if (currentSequenceIndex < currentSequence.size) {
                        handler.postDelayed(this, lessonItem.timePeriod?.toLong() ?: 1000L)
                    } else {
                        isShowingSequence = false
                        controlButton.isEnabled = true
                        controlButton.setBackgroundColor(resources.getColor(R.color.button_enabled, null))
                        controlButton.setTextColor(resources.getColor(R.color.button_text_enabled, null))
                    }
                }, 200)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)!!
        uploadLessonData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlindingLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findsId()
        controlButtonAnim()
        showCurrentOperation()
        setupQuitButton()
        rulesBookButtonClick()
        resetClickListener()
    }

    private fun uploadLessonData(){
        operations = arguments?.getSerializable("operations") as? List<Any> ?: emptyList()

        if (operations.isEmpty()) {
            lessonStep = lessonItem.startStepNumber!!
            // lessonStep değerini kontrol et ve güvenli bir şekilde kullan
            val currentLessonStep = if (lessonStep > 0) lessonStep else 1

            if(lessonItem.stepIsFinish){
                operations = getLessonOperationsBlinding(lessonItem.finishStepNumber!!)
            } else {

                operations = getLessonOperationsBlinding(currentLessonStep)

            }

            Log.d("AbacusFragment", "Loaded operations size: ${operations.size}")
        }
    }
    private fun timeStarter(){
        if(lessonItem.type == 2)
            timerTextView.visibility = View.VISIBLE
        if (!isTimerStarted) {
            startTimer()
            isTimerStarted = true

        }
    }
    private fun startTimer() {
        runnable = object : Runnable {
            override fun run() {
                seconds++
                val minutes = seconds / 60
                val secs = seconds % 60
                currentTime = String.format("%d:%02d", minutes, secs)
                timerTextView.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }
    private fun findsId(){
        timerTextView = binding.timerTextView
        binding.resetButton.frame = 20
        rulesBookButton = binding.rulesBookButton
        fabHint = binding.fabHint
        tvHint = binding.tvHint
        fabHintTouchArea = binding.fabHintTouchArea
        fubHintClickListener()
        lottieView = binding.lottieView
        correctAnswerText = binding.correctAnswerText
        incorrectPanel = binding.incorrectPanel
        correctPanel = binding.correctPanel
        numberText = binding.firstNumberText
        controlButton = binding.kontrolButton
        totalQuestions = operations.size
        firstNumberText = binding.firstNumberText
        firstNumberText2 = binding.firstNumberText2
        operatorText = binding.operator
        secondNumberText = binding.secondNumberText
        timeStarter()

    }
    private fun resetClickListener() {
        // İlk başta 20. frame'de başlat

        binding.resetButton.setOnClickListener {
            binding.resetButton.playAnimation()
            if (currentSequence.isNotEmpty()) {
                // Gösterimi iptal et
                handler.removeCallbacks(showNextNumberRunnable)
                isShowingSequence = false

                // Önce text'i boşalt
                numberText.text = ""

                // 0.2 saniye bekle, sonra sıralamayı başlat
                handler.postDelayed({
                    currentSequenceIndex = 0

                    // Butonu devre dışı bırak
                    controlButton.isEnabled = false
                    controlButton.setBackgroundColor(resources.getColor(R.color.button_disabled, null))
                    controlButton.setTextColor(resources.getColor(R.color.button_text_disabled, null))

                    // İlk sayıyı göster ve gösterimi başlat
                    numberText.text = currentSequence[0].toString()
                    currentSequenceIndex = 1
                    isShowingSequence = true
                    handler.postDelayed(showNextNumberRunnable, lessonItem.timePeriod ?: 1000L)
                }, 200)
            }
        }

        // Animasyon bitince tekrar 20. frame'e getir
        binding.resetButton.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.resetButton.frame = 20
                }, 1000) // 1000 ms = 1 saniye
            }
        })
    }
    private fun fubHintClickListener(){
        fabHintTouchArea.setOnClickListener {
            if (!isHintVisible) {
                showHint()
            } else {
                hideHint()
            }
            // veya doğrudan fabHint'in tıklama fonksiyonunu çağır
        }
    }
    private fun showHint() {
        tvHint.text = lessonItem.lessonHint?.let { splitTextEqually(it) }

        // Play Lottie animation
        fabHint.playAnimation()
        fabHint.setFrame(25)

        // Show and animate TextView
        tvHint.visibility = View.VISIBLE
        tvHint.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right))

        isHintVisible = true
    }
    private fun hideHint() {
        // Sağa kayma animasyonunu başlat
        fabHint.cancelAnimation()
        fabHint.setFrame(0)

        val slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right_hint)

        // Animasyon bittiğinde TextView'ı gizle
        slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                tvHint.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Animasyonu başlat
        tvHint.startAnimation(slideOutAnimation)
        isHintVisible = false
    }
    private fun splitTextEqually(text: String): String {
        val words = text.split(" ")
        val totalWords = words.size

        // Kelimeleri iki gruba böl
        val firstLine = words.take(totalWords / 2).joinToString(" ")
        val secondLine = words.drop(totalWords / 2).joinToString(" ")

        return "$firstLine\n$secondLine"
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun controlButtonAnim() {
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
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime >= 500) {
                        lastClickTime = currentTime

                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(400)
                            .setInterpolator(BounceInterpolator())
                            .start()

                        // Tıklama işlemini gerçekleştir
                        updateProgressBar()
                        showResultPanel()
                        controlNumber = 0
                        binding.numberInput.setText("")
                    }
                    true
                }

                else -> false
            }
        }

    }
    //Sorudaki sayıları gösterir
    private fun showCurrentOperation() {
        if (operations.isEmpty()) {
            Log.e("AbacusFragment", "Operations list is empty")
            return
        }

        if(lessonItem.blindingMultiplication == true){
            if (currentIndex < operations.size) {
                val currentOperation = operations[currentIndex] as MathOperation
                currentOperation.firstNumber?.let { number ->
                    firstNumberText2.text = number.toString()
                }

                currentOperation.operator?.let { op ->
                    operatorText.text = op
                }

                currentOperation.secondNumber?.let { number ->
                    secondNumberText.text = number.toString()
                }
            }
        }
        else{
            if (currentIndex < operations.size) {
                val currentOperation = operations[currentIndex]
                when (currentOperation) {
                    is List<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        startShowingSequence(currentOperation as List<Int>)
                    }
                    is Int -> {
                        // Eğer operations bir Int listesi ise, direkt olarak sequence olarak göster
                        startShowingSequence(operations as List<Int>)
                    }
                    is MathOperation -> {
                        currentOperation.firstNumber?.let { number ->
                            numberText.text = number.toString()
                        }
                    }
                }
            }
        }
    }
    private fun setupQuitButton() {
        binding.quitButton.setOnClickListener {
            closeFragment()
        }
    }
    private fun closeFragment() {
        // BackStack'i temizle ve MapFragment'e dön
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,  // Giriş animasyonu
                R.anim.slide_out_left // Çıkış animasyonu
            )
            .remove(this@BlindingLessonFragment)
            .commit()
    }
    private fun rulesBookButtonClick(){
        rulesBookButton.setOnClickListener{
            childFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_down,
                    0
                )
                .replace(R.id.rulesFragmentContainer, RulesFragment())
                .commit()
        }

    }
    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    private fun showResultPanel() {
        // Eğer dialog zaten gösteriliyorsa, yeni dialog oluşturma
        if (resultDialog?.isShowing == true) {
            return
        }


        if (stepAnswerAlgorithm()) {
            // Doğru cevap durumu

            correctAnswer++

            if (currentIndex == operations.size -1 && correctAnswer == totalQuestions) {
                lottieView.visibility=View.VISIBLE
                lottieView.playAnimation() // Animasyonu başlat
            }
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

            // Geri tuşu dinleyicisi
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
                            currentIndex++
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
                            if (currentIndex <= operations.size - 1) {
                                showCurrentOperation()
                            } else {
                                if(lessonItem.type == 2){
                                    showChestResult()
                                }
                                else{
                                    showLessonResult()
                                }
                            }
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
            correctAnswerText.text = answerNumber.toString()
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
                            currentIndex++
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
                            if (currentIndex <= operations.size - 1) {
                                showCurrentOperation()
                            } else {
                                if(lessonItem.type == 2){
                                    showChestResult()
                                }
                                else{
                                    showLessonResult()
                                }
                            }
                            true
                        }

                        else -> false
                    }
                }
        }
    }
    private fun updateProgressBar() {
        val fill = requireView().findViewById<View>(R.id.progressBarFill)
        val empty = requireView().findViewById<View>(R.id.progressBarEmpty)

        val startWeight = (fill.layoutParams as LinearLayout.LayoutParams).weight
        val endWeight = (currentIndex + 1).toFloat() / operations.size

        // Renkler: yeşil -> sarı -> yeşil
        val colorGreen = Color.parseColor("#7CFC00")    // Yeşil
        val colorYellow = Color.parseColor("#FFFF99")    // Açık sarı
        val colorDarkRed = Color.parseColor("#990000")

        if (stepAnswerAlgorithm()) {

            val animator = ValueAnimator.ofFloat(startWeight, endWeight)
            animator.duration = 700
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                (fill.layoutParams as LinearLayout.LayoutParams).weight = value
                (empty.layoutParams as LinearLayout.LayoutParams).weight = 1 - value
                fill.requestLayout()
                empty.requestLayout()

                // Renk geçişi: ilk yarıda yeşilden sarıya, ikinci yarıda sarıdan yeşile
                val fraction = (value - startWeight) / (endWeight - startWeight)
                val color = when {
                    fraction <= 0.5f -> {
                        // İlk yarı: turuncu -> açık sarı
                        ArgbEvaluator().evaluate(fraction * 2, colorGreen, colorYellow) as Int
                    }

                    else -> {
                        // İkinci yarı: açık sarı -> yeşil
                        ArgbEvaluator().evaluate(
                            (fraction - 0.5f) * 2,
                            colorYellow,
                            colorGreen
                        ) as Int
                    }
                }

                // Drawable'ın rengini güncelle
                val background = fill.background
                if (background is GradientDrawable) {
                    background.setColor(color)
                }
            }

            animator.start()
        } else {
            val animator = ValueAnimator.ofFloat(startWeight, endWeight)
            animator.duration = 700
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { animation ->
                // Weight (genişlik) animasyonu
                val value = animation.animatedValue as Float
                (fill.layoutParams as LinearLayout.LayoutParams).weight = value
                (empty.layoutParams as LinearLayout.LayoutParams).weight = 1 - value
                fill.requestLayout()
                empty.requestLayout()

                // Basit renk geçişi: yeşilden kırmızıya
                val fraction = (value - startWeight) / (endWeight - startWeight)
                val color = ArgbEvaluator().evaluate(fraction, colorGreen, colorDarkRed) as Int

                // Drawable'ın rengini güncelle
                val background = fill.background
                if (background is GradientDrawable) {
                    background.setColor(color)
                }
            }
            animator.start()
        }

    }
    private fun stepAnswerAlgorithm(): Boolean {
        if (operations.isEmpty()) {
            return false
        }

        val currentOperation = operations[currentIndex]
        return when (currentOperation) {
            is MathOperation -> {
                if (lessonItem.blindingMultiplication == true) {
                    answerNumber = currentOperation.firstNumber?.times(currentOperation.secondNumber!!) ?: 0
                    val inputText = binding.numberInput.text.toString()
                    if (inputText.isNotEmpty()) {
                        try {
                            controlNumber = inputText.toInt()
                            if (controlNumber == answerNumber) {
                                controlNumber = 0
                                true
                            } else {
                                controlNumber = 0
                                false
                            }
                        } catch (e: NumberFormatException) {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    // Normal MathOperation işlemi
                    false
                }
            }
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val sequence = currentOperation as List<Int>
                val result = sequence.sum()
                answerNumber = result
                if (currentSequenceIndex > 0) {
                    val inputText = binding.numberInput.text.toString()
                    if (inputText.isNotEmpty()) {
                        try {
                            controlNumber = inputText.toInt()
                            if (controlNumber == answerNumber) {
                                controlNumber = 0
                                true
                            } else {
                                controlNumber = 0
                                false
                            }
                        } catch (e: NumberFormatException) {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
            else -> false
        }
    }    private fun showChestResult() {
        val chestResultFragment = ChestResult()

        // Başarı oranını hesapla
        val successRate = if (totalQuestions > 0) {
            (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }

        val args = Bundle().apply {
            putInt("correctAnswers", correctAnswer)
            putInt("totalQuestions", totalQuestions)
            putFloat("successRate", successRate)
            putString("time", currentTime)
        }
        chestResultFragment.arguments = args

        // Yeni fragment'ı abacus container'a ekle
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.abacusFragmentContainer, chestResultFragment)
            .commit()
    }
    private fun showLessonResult() {
        val lessonResultFragment = LessonResult()
        val lessonResultFalse = LessonResultFalse()

        // Başarı oranını hesapla
        val successRate = if (totalQuestions > 0) {
            (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }

        val args = Bundle().apply {
            putInt("correctAnswers", correctAnswer)
            putInt("totalQuestions", totalQuestions)
            putFloat("successRate", successRate)
        }
        lessonResultFragment.arguments = args
        val argsFalse = Bundle().apply {
            putInt("correctAnswers", correctAnswer)
            putInt("totalQuestions", totalQuestions)
            putFloat("successRate", successRate)
        }
        lessonResultFalse.arguments = argsFalse

        // Yeni fragment'ı abacus container'a ekle
        if(successRate < 0) {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.abacusFragmentContainer, lessonResultFalse)
                .commit()
        } else {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.abacusFragmentContainer, lessonResultFragment)
                .commit()
        }
    }


    fun startShowingSequence(sequence: List<Int>) {
        if (isShowingSequence) return
        
        currentSequence = sequence
        currentSequenceIndex = 0
        isShowingSequence = true
        controlButton.isEnabled = false
        // Button'u devre dışı haline getir
        controlButton.setBackgroundColor(resources.getColor(R.color.button_disabled, null))
        controlButton.setTextColor(resources.getColor(R.color.button_text_disabled, null))
        
        // İlk sayıyı göster
        if (currentSequence.isNotEmpty()) {
            numberText.text = currentSequence[0].toString()
            currentSequenceIndex = 1
            handler.postDelayed(showNextNumberRunnable, lessonItem.timePeriod!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(showNextNumberRunnable)
    }
}