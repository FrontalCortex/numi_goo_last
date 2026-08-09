package com.example.app

import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.os.Handler
import android.os.Looper
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentNewChestBinding
import kotlin.random.Random

class NewChestFragment : Fragment() {


    private val hintHandler = Handler(Looper.getMainLooper())
    private val hintRunnable = Runnable {
        if (isAdded && _binding != null) {
            binding.chestHintText.visibility = View.VISIBLE
            binding.chestHintText.alpha = 0f
            binding.chestHintText.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun resetHintTimer() {
        hintHandler.removeCallbacks(hintRunnable)
        if (_binding != null) {
            binding.chestHintText.visibility = View.INVISIBLE
        }
        hintHandler.postDelayed(hintRunnable, 5000)
    }
    private var _binding: FragmentNewChestBinding? = null
    private val binding get() = _binding!!

    data class RarityTheme(
        val bgColor: Int,
        val platformColor: Int,
        val starColor: Int,
        val stepActiveColor: Int,
        val stepFutureColor: Int
    )

    // Sandık rarilik seviyeleri
    enum class ChestRarity(
        val drawableRes: Int, 
        val openDrawableRes: Int, 
        val label: String, 
        val labelColor: Int,
        val theme: RarityTheme
    ) {
        COMMON(R.drawable.new_chest_close_ic1, R.drawable.new_chest_open_ic1, "SIRADAN",  0xFFFFB300.toInt(),
            RarityTheme(0xFF141F23.toInt(), 0xFF263238.toInt(), 0xFFFFD54F.toInt(), 0xFFFFB300.toInt(), 0xFF3A3010.toInt())
        ),
        RARE(R.drawable.new_chest_close_ic2, R.drawable.new_chest_open_ic2, "ENDER",     0xFFFFFFFF.toInt(),
            RarityTheme(0xFF4AC3F6.toInt(), 0xFF2A9FE5.toInt(), 0xFFBCE5FB.toInt(), 0xFFBCE5FB.toInt(), 0xFF2A9FE5.toInt())
        ),
        EPIC(R.drawable.new_chest_close_ic3, R.drawable.new_chest_open_ic3, "DESTANSI",  0xFFFFFFFF.toInt(),
            RarityTheme(0xFFAB47BC.toInt(), 0xFF8E24AA.toInt(), 0xFFE1BEE7.toInt(), 0xFFE1BEE7.toInt(), 0xFF8E24AA.toInt())
        ),
        LEGENDARY(R.drawable.new_chest_close_ic4, R.drawable.new_chest_open_ic4, "EFSANEVİ", 0xFFFFFFFF.toInt(),
            RarityTheme(0xFFFF9800.toInt(), 0xFFF57C00.toInt(), 0xFFFFE0B2.toInt(), 0xFFFFE0B2.toInt(), 0xFFF57C00.toInt())
        ),
    }

    companion object {
        private const val ARG_START_RARITY = "start_rarity"

        fun newInstance(startRarity: ChestRarity = ChestRarity.COMMON): NewChestFragment {
            return NewChestFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_START_RARITY, startRarity.name)
                }
            }
        }
    }

    // Oyun durumu
    private var canClose = false
    private var currentRarity = ChestRarity.COMMON
    // 0 = hiç tıklanmadı, 1..3 = adımlar, 4 = açılmaya hazır, 5 = açıldı
    private var tapCount = 0

    // Sallanma animasyonu
    private var idleAnim: ObjectAnimator? = null
    private var activeStepAnim: ObjectAnimator? = null

    // Yıldız animasyonları için
    private val starHandler = Handler(Looper.getMainLooper())
    private val starSpawner = object : Runnable {
        override fun run() {
            if (isAdded) {
                spawnStar()
                starHandler.postDelayed(this, Random.nextLong(200, 500))
            }
        }
    }

    private var originalStatusBarColor: Int? = null
    private var originalNavigationBarColor: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewChestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        originalStatusBarColor = requireActivity().window.statusBarColor
        originalNavigationBarColor = requireActivity().window.navigationBarColor

        val startRarityName = arguments?.getString(ARG_START_RARITY) ?: ChestRarity.COMMON.name
        currentRarity = try {
            ChestRarity.valueOf(startRarityName)
        } catch (e: Exception) {
            ChestRarity.COMMON
        }

        // Başlangıç durumunu uygula
        applyRarity(currentRarity, animate = false)
        updateStepIndicators()
        startIdleAnimation()

        
        binding.chestHintText.visibility = View.INVISIBLE
        resetHintTimer()
        // Ekrana tıklama
        binding.newChestRoot.setOnClickListener {
            onScreenTapped()
        }

        // Geri tuşu -> hicbir sey yapma
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Kullanıcı sadece ekrana dokunarak kapatabilir
            }
        })
        
        // Yıldızları başlat
        starHandler.post(starSpawner)
    }

    override fun onStart() {
        super.onStart()
        // MainActivity'deki when bloğu NewChestFragment'i görecek; insets yeniden hesaplansın.
        requireActivity().findViewById<View>(R.id.bottomNavigationID)?.requestApplyInsets()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().findViewById<View>(R.id.bottomNavigationID)?.requestApplyInsets()
    }

    private fun onScreenTapped() {
        resetHintTimer()
        when (tapCount) {
            0, 1, 2 -> {
                // Adım tıklaması: rarity yükseltme şansı
                tapCount++
                val newRarity = rollRarity(currentRarity)
                val rarityChanged = newRarity != currentRarity
                currentRarity = newRarity
                updateStepIndicators()
                if (rarityChanged) {
                    applyRarity(currentRarity, animate = true)
                } else {
                    playBounceAnimation()
                }
                // 3. adımdan sonra ipucu güncelle
                if (tapCount == 3) {
                    binding.chestHintText.text = "Sandığı açmak için dokun!"
                }
            }
            3 -> {
                // Sandığı aç
                tapCount++
                stopIdleAnimation()
                openChest()
            }
            4 -> {
                // Açıldıktan sonra kapat
                if (canClose) {
                    tapCount++
                    closeFragment()
                }
            }
        }
    }

    /** Mevcut raritye göre ihtimalleri hesaplar ve yeni rarity döner. */
    private fun rollRarity(current: ChestRarity): ChestRarity {
        val rand = (1..100).random()
        return when (current) {
            ChestRarity.COMMON -> when {
                rand <= 10 -> ChestRarity.EPIC   // %10
                rand <= 40 -> ChestRarity.RARE   // %30
                else -> ChestRarity.COMMON       // %60
            }
            ChestRarity.RARE -> when {
                rand <= 5 -> ChestRarity.LEGENDARY // %5
                rand <= 40 -> ChestRarity.EPIC      // %35
                else -> ChestRarity.RARE            // %60
            }
            ChestRarity.EPIC -> when {
                rand <= 20 -> ChestRarity.LEGENDARY // %20
                else -> ChestRarity.EPIC            // %80
            }
            ChestRarity.LEGENDARY -> ChestRarity.LEGENDARY // %100 aynı kalır
        }
    }

    /** Sandık görselini ve nadirlik etiketini günceller. */
    private fun applyRarity(rarity: ChestRarity, animate: Boolean) {
        binding.chestImage.setImageResource(rarity.drawableRes)
        if (!isAdded) return
        
        // Temayı uygula
        binding.newChestRoot.setBackgroundColor(rarity.theme.bgColor)
        binding.chestPlatform.backgroundTintList = android.content.res.ColorStateList.valueOf(rarity.theme.platformColor)
        
        activity?.window?.let { w ->
            w.statusBarColor = rarity.theme.bgColor
            w.navigationBarColor = rarity.theme.bgColor
        }

        binding.chestRarityLabel.text = rarity.label
        binding.chestRarityLabel.setTextColor(rarity.labelColor)

        // Eski yıldızların rengini yeni temanın rengine güncelle
        for (i in 0 until binding.starContainer.childCount) {
            val starView = binding.starContainer.getChildAt(i) as? ImageView
            starView?.imageTintList = android.content.res.ColorStateList.valueOf(rarity.theme.starColor)
        }

        if (animate) {
            playBounceAnimation()
        }
    }

    /** 3 adım göstergesini tapCount'a göre günceller. */
    private fun updateStepIndicators() {
        activeStepAnim?.cancel()
        activeStepAnim = null
        
        val stepBgs = listOf(binding.step1Bg, binding.step2Bg, binding.step3Bg)
        val stepContainers = listOf(binding.step1, binding.step2, binding.step3)
        
        for (i in stepBgs.indices) {
            val stepBg = stepBgs[i]
            val stepContainer = stepContainers[i]
            
            stepContainer.translationY = 0f
            
            when {
                i < tapCount -> {
                    stepBg.setBackgroundResource(R.drawable.step_circle_done)
                    stepBg.backgroundTintList = null
                }
                i == tapCount && tapCount < 3 -> {
                    stepBg.setBackgroundResource(R.drawable.step_circle_active)
                    stepBg.backgroundTintList = android.content.res.ColorStateList.valueOf(currentRarity.theme.stepActiveColor)
                    activeStepAnim = ObjectAnimator.ofFloat(stepContainer, "translationY", 0f, -15f).apply {
                        duration = 400
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.REVERSE
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }
                else -> {
                    stepBg.setBackgroundResource(R.drawable.step_circle_future)
                    stepBg.backgroundTintList = android.content.res.ColorStateList.valueOf(currentRarity.theme.stepFutureColor)
                }
            }
        }
    }

    /** Sandığın sürekli hafif sallanma (idle) animasyonunu başlatır. */
    private fun startIdleAnimation() {
        stopIdleAnimation()
        if (tapCount >= 4) return
        
        val durationMs: Long
        val transY: PropertyValuesHolder
        val rot: PropertyValuesHolder

        when (currentRarity) {
            ChestRarity.COMMON -> {
                durationMs = 2000L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)
                val ty2 = Keyframe.ofFloat(0.30f, -35f)
                val ty3 = Keyframe.ofFloat(0.35f, 0f)
                val ty4 = Keyframe.ofFloat(0.38f, -10f)
                val ty5 = Keyframe.ofFloat(0.41f, 0f)
                val ty6 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1)
            }
            ChestRarity.RARE -> {
                durationMs = 1750L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)   
                val ty2 = Keyframe.ofFloat(0.30f, -35f) 
                val ty3 = Keyframe.ofFloat(0.35f, 0f)   
                val ty4 = Keyframe.ofFloat(0.38f, -10f) 
                val ty5 = Keyframe.ofFloat(0.41f, 0f)   
                val ty6 = Keyframe.ofFloat(1f, 0f)      
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1)
            }
            ChestRarity.EPIC -> {
                durationMs = 2000L // Beklemeli (2 saniye döngü)
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)
                val ty2 = Keyframe.ofFloat(0.30f, -70f) // Zıplama tepe
                val ty3 = Keyframe.ofFloat(0.35f, 0f)   
                val ty4 = Keyframe.ofFloat(0.38f, -20f) 
                val ty5 = Keyframe.ofFloat(0.41f, 0f)
                val ty6 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(0.25f, 0f)
                val ry2 = Keyframe.ofFloat(0.28f, 5f)
                val ry3 = Keyframe.ofFloat(0.32f, -5f)
                val ry4 = Keyframe.ofFloat(0.35f, 0f)
                val ry5 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1, ry2, ry3, ry4, ry5)
            }
            ChestRarity.LEGENDARY -> {
                durationMs = 2000L // Destansı ile tamamen aynı
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)
                val ty2 = Keyframe.ofFloat(0.30f, -70f) // Zıplama tepe
                val ty3 = Keyframe.ofFloat(0.35f, 0f)   
                val ty4 = Keyframe.ofFloat(0.38f, -20f) 
                val ty5 = Keyframe.ofFloat(0.41f, 0f)
                val ty6 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(0.25f, 0f)
                val ry2 = Keyframe.ofFloat(0.28f, 5f)
                val ry3 = Keyframe.ofFloat(0.32f, -5f)
                val ry4 = Keyframe.ofFloat(0.35f, 0f)
                val ry5 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1, ry2, ry3, ry4, ry5)
            }
        }

        idleAnim = ObjectAnimator.ofPropertyValuesHolder(binding.chestImage, transY, rot).apply {
            duration = durationMs
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    private fun stopIdleAnimation() {
        idleAnim?.cancel()
        idleAnim = null
        binding.chestImage.rotation = 0f
        binding.chestImage.translationX = 0f
        binding.chestImage.translationY = 0f
        binding.chestImage.scaleX = 1f
        binding.chestImage.scaleY = 1f
    }

    /** Ekrana tıklandığında sandığa hafif zıplama animasyonu uygular. */
    private fun playBounceAnimation() {
        stopIdleAnimation()
        val scaleUpX = ObjectAnimator.ofFloat(binding.chestImage, "scaleX", 1f, 1.18f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.chestImage, "scaleY", 1f, 1.18f)
        val scaleDownX = ObjectAnimator.ofFloat(binding.chestImage, "scaleX", 1.18f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.chestImage, "scaleY", 1.18f, 1f)

        val platUpX = ObjectAnimator.ofFloat(binding.chestPlatform, "scaleX", 1f, 1.18f)
        val platUpY = ObjectAnimator.ofFloat(binding.chestPlatform, "scaleY", 1f, 1.18f)
        val platDownX = ObjectAnimator.ofFloat(binding.chestPlatform, "scaleX", 1.18f, 1f)
        val platDownY = ObjectAnimator.ofFloat(binding.chestPlatform, "scaleY", 1.18f, 1f)

        val up = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY, platUpX, platUpY)
            duration = 130
            interpolator = AccelerateDecelerateInterpolator()
        }
        val down = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, platDownX, platDownY)
            duration = 200
            interpolator = OvershootInterpolator(2.5f)
        }
        AnimatorSet().apply {
            playSequentially(up, down)
            start()
        }
        // Zıplama bittikten sonra idle animasyonunu yeniden başlat
        binding.chestImage.postDelayed({ if (isAdded && tapCount < 4) startIdleAnimation() }, 350)
    }

    /** Sandığı açma animasyonu ve görsel değişimi. */
    private fun openChest() {
        hintHandler.removeCallbacks(hintRunnable)
        
        binding.chestImage.setImageResource(currentRarity.openDrawableRes)
        val viewsToHide = listOf(
            binding.chestImage, 
            binding.stepsContainer, 
            binding.chestRarityLabel, 
            binding.chestHintText, 
            binding.chestPlatform, 
            binding.starContainer
        )
        
        val hideAnims = mutableListOf<android.animation.Animator>()
        viewsToHide.forEach { view ->
            val ty = android.animation.ObjectAnimator.ofFloat(view, "translationY", view.translationY, view.translationY + 500f)
            val alpha = android.animation.ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f)
            hideAnims.add(ty)
            hideAnims.add(alpha)
        }

        binding.chestOverlay.visibility = android.view.View.VISIBLE
        binding.chestOverlay.alpha = 0f
        
        binding.chestOverlay.post {
            val overlayAlpha = android.animation.ObjectAnimator.ofFloat(binding.chestOverlay, "alpha", 0f, 1f)
            hideAnims.add(overlayAlpha)

            val bgColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.background_color)
            activity?.window?.let { w ->
                val colorAnim = android.animation.ValueAnimator.ofArgb(w.statusBarColor, bgColor)
                colorAnim.addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    w.statusBarColor = color
                    w.navigationBarColor = color
                }
                hideAnims.add(colorAnim)
            }
            
            val phase1 = android.animation.AnimatorSet().apply {
                playTogether(hideAnims)
                duration = 700
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            
            phase1.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    showReward()
                }
            })
            
            phase1.start()
        }
    }

    private fun showReward() {
        var textValue = ""
        var coinDrawable = 0
        var textDrawable = 0
        var earnedGold = 0
        var earnedKey = 0

        val rand = (1..100).random()
        when (currentRarity) {
            ChestRarity.COMMON -> {
                val amount = (50..100).random()
                earnedGold = amount
                textValue = "$amount"
                coinDrawable = R.drawable.shop_coin_ic1
                textDrawable = R.drawable.gold_ic
            }
            ChestRarity.RARE -> {
                if (rand <= 50) {
                    val amount = (150..200).random()
                    earnedGold = amount
                    textValue = "$amount"
                    coinDrawable = R.drawable.shop_coin_ic1
                    textDrawable = R.drawable.gold_ic
                } else {
                    earnedKey = 1
                    textValue = "1"
                    coinDrawable = R.drawable.key
                    textDrawable = R.drawable.key
                }
            }
            ChestRarity.EPIC -> {
                if (rand <= 50) {
                    val amount = (300..400).random()
                    earnedGold = amount
                    textValue = "$amount"
                    coinDrawable = R.drawable.shop_coin_ic2
                    textDrawable = R.drawable.gold_ic
                } else {
                    earnedKey = 2
                    textValue = "2"
                    coinDrawable = R.drawable.key
                    textDrawable = R.drawable.key
                }
            }
            ChestRarity.LEGENDARY -> {
                if (rand <= 50) {
                    val amount = (1000..1500).random()
                    earnedGold = amount
                    textValue = "$amount"
                    coinDrawable = R.drawable.shop_coin_ic3
                    textDrawable = R.drawable.gold_ic
                } else {
                    earnedKey = 5
                    textValue = "5"
                    coinDrawable = R.drawable.key
                    textDrawable = R.drawable.key
                }
            }
        }
        
        val ctx = context
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (ctx != null && uid != null) {
            if (earnedGold > 0) {
                UserWalletFirestore.applyCurrencyDelta(ctx, uid, earnedGold)
            }
            if (earnedKey > 0) {
                UserWalletFirestore.applyKeyDelta(ctx, uid, earnedKey)
            }
        }

        binding.chestRewardText.text = textValue
        binding.chestRewardText.setCompoundDrawablesWithIntrinsicBounds(textDrawable, 0, 0, 0)
        binding.chestRewardCoin.setImageResource(coinDrawable)

        binding.chestRewardCoin.visibility = android.view.View.VISIBLE
        binding.chestRewardText.visibility = android.view.View.VISIBLE
        
        val coinScaleX = android.animation.ObjectAnimator.ofFloat(binding.chestRewardCoin, "scaleX", 0f, 1f)
        val coinScaleY = android.animation.ObjectAnimator.ofFloat(binding.chestRewardCoin, "scaleY", 0f, 1f)
        val coinAlpha = android.animation.ObjectAnimator.ofFloat(binding.chestRewardCoin, "alpha", 0f, 1f)
        val textAlpha = android.animation.ObjectAnimator.ofFloat(binding.chestRewardText, "alpha", 0f, 1f)
        
        val rewardAnim = android.animation.AnimatorSet().apply {
            playTogether(coinScaleX, coinScaleY, coinAlpha, textAlpha)
            duration = 500
            interpolator = android.view.animation.OvershootInterpolator(1.5f)
        }
        
        rewardAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                canClose = true
            }
        })
        
        rewardAnim.start()
    }

    private fun closeFragment() {
        parentFragmentManager.setFragmentResult("chest_closed", android.os.Bundle())
        val fm = parentFragmentManager
        val topName = if (fm.backStackEntryCount > 0) fm.getBackStackEntryAt(fm.backStackEntryCount - 1).name else null
        
        if (topName == "mission_chest" || topName == "map_chest" || topName == "shop_chest") {
            fm.popBackStack(topName, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } else {
            val main = activity as? MainActivity
            if (main != null) {
                main.finishTasksOverlayAnimated("NewChestFragment.close")
            } else {
                fm.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.let { w ->
            originalStatusBarColor?.let { w.statusBarColor = it }
            originalNavigationBarColor?.let { w.navigationBarColor = it }
        }
        hintHandler.removeCallbacks(hintRunnable)
        starHandler.removeCallbacks(starSpawner)
        stopIdleAnimation()
        activeStepAnim?.cancel()
        _binding = null
    }

    /** Sandığın etrafında yukarı süzülen yıldızlar oluşturur. */
    private fun spawnStar() {
        val container = binding.starContainer
        if (container.width == 0 || container.height == 0) return

        val starDrawables = listOf(R.drawable.ic_star_diamond, R.drawable.ic_star_four_point)
        val star = ImageView(requireContext()).apply {
            setImageResource(starDrawables.random())
            imageTintList = android.content.res.ColorStateList.valueOf(currentRarity.theme.starColor)
            alpha = 0f
        }

        val size = Random.nextInt(35, 75)
        val params = FrameLayout.LayoutParams(size, size)
        
        // X koordinatı: ekranın tamamından rastgele (full genişlik)
        val startX = Random.nextInt(0, container.width - size).toFloat()
        
        // Y koordinatı: stepsContainer top ve chestPlatform top arasında
        val topLimit = binding.chestPlatform.y
        val bottomLimit = binding.stepsContainer.y
        if (bottomLimit <= topLimit || topLimit == 0f) return 
        
        val startY = topLimit + Random.nextFloat() * (bottomLimit - topLimit)
        
        star.x = startX
        star.y = startY
        container.addView(star, params)

        val endY = startY - Random.nextInt(200, 400)
        val animDuration = Random.nextLong(2000, 3500)

        val moveAnim = ObjectAnimator.ofFloat(star, "y", startY, endY).apply {
            duration = animDuration
            interpolator = LinearInterpolator()
        }

        val fade0 = Keyframe.ofFloat(0f, 0f)
        val fade1 = Keyframe.ofFloat(0.3f, 1f)
        val fade2 = Keyframe.ofFloat(0.7f, 1f)
        val fade3 = Keyframe.ofFloat(1f, 0f)
        val alphaHolder = PropertyValuesHolder.ofKeyframe("alpha", fade0, fade1, fade2, fade3)
        
        val alphaAnim = ObjectAnimator.ofPropertyValuesHolder(star, alphaHolder).apply {
            duration = animDuration
        }

        AnimatorSet().apply {
            playTogether(moveAnim, alphaAnim)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    container.removeView(star)
                }
            })
            start()
        }
    }
}
