package com.example.app

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ShopFragment : Fragment() {

    private var currencyText: TextView? = null
    private var keyText: TextView? = null
    private var energyText: TextView? = null
    
    // Timer properties for energy section
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_shop, container, false)

        // Close button
        v.findViewById<ImageButton>(R.id.shopCloseButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Currency text — load from cache, then update from Firestore
        currencyText = v.findViewById(R.id.shopCurrencyText)
        keyText = v.findViewById(R.id.shopKeyText)
        energyText = v.findViewById(R.id.shopEnergyText)
        val ctx = requireContext()
        currencyText?.text = UserWalletFirestore.getCachedCurrency(ctx).toString()
        keyText?.text = UserWalletFirestore.getCachedKeys(ctx).toString()
        keyText?.text = UserWalletFirestore.getCachedKeys(ctx).toString()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            UserWalletFirestore.loadWallet(ctx, uid, onResult = { wallet ->
                currencyText?.text = wallet.currency.toString()
                keyText?.text = wallet.keys.toString()
            })
        }
        
        // --- Can (Energy) Bölümü ---
        v.findViewById<View>(R.id.shopProCard).setOnClickListener {
            Toast.makeText(ctx, "Pro Yakında", Toast.LENGTH_SHORT).show()
        }

        val buyButton = v.findViewById<View>(R.id.shopBuyLifeButton)
        buyButton.setOnClickListener {
            val activity = activity as? MainActivity ?: return@setOnClickListener
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            
            val currentKey = keyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedKeys(ctx)
            if (currentKey >= 3) {
                // Anlık (optimistic) UI güncellemesi
                keyText?.text = (currentKey - 3).toString()
                
                // Bakiyeyi düşür
                UserWalletFirestore.applyKeyDelta(ctx, currentUid, -3) {
                    refreshCurrencyUi()
                }
                
                // Enerji ekle
                activity.getEnergyManager().addEnergy(1)
                
                // Animasyon oynat
                playHeartFlyAnimation(buyButton)
                
                updateEnergyUi()
            } else {
                Toast.makeText(ctx, "Yetersiz anahtar!", Toast.LENGTH_SHORT).show()
            }
        }

        // --- SUPER CARD ---
        val superCard = v.findViewById<View>(R.id.shopSuperCard)
        superCard?.setOnClickListener {
            ProDiffirentFragment().show(requireActivity().supportFragmentManager, "ProDiffirent")
        }

        // --- Özel Teklifler ---
        // Kristal reklam butonu
        val watchAdButton = v.findViewById<View>(R.id.shopWatchAdButton)
        watchAdButton.setOnClickListener {
            val activity = activity as? MainActivity ?: return@setOnClickListener
            activity.adManager.showRewardedAd(activity, showAdSkipAfter = false) {
                val mainActivity = activity as? MainActivity
                mainActivity?.findViewById<View>(R.id.abacusFragmentContainer)?.visibility = android.view.View.VISIBLE
                
                parentFragmentManager.beginTransaction()
                    .add(R.id.abacusFragmentContainer, NewChestFragment.newInstance(NewChestFragment.ChestRarity.COMMON))
                    .commit()

                parentFragmentManager.setFragmentResultListener("chest_closed", viewLifecycleOwner) { _, _ ->
                    refreshCurrencyUi()
                }
            }
        }

        val watchAdButton2 = v.findViewById<View>(R.id.shopWatchAdButton2)
        watchAdButton2.setOnClickListener {
            val activity = activity as? MainActivity ?: return@setOnClickListener
            activity.adManager.showRewardedAd(activity, showAdSkipAfter = false) {
                activity.getEnergyManager().addEnergy(1)
                playHeartFlyAnimation(watchAdButton2)
                updateEnergyUi()
            }
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid        
        // --- Altın Paketleri ---
        val goldCard1 = v.findViewById<View>(R.id.shopGoldCard1)
        goldCard1.setOnClickListener {
            currentUid?.let { uid ->
                val current = currencyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedCurrency(ctx)
                currencyText?.text = (current + 1200).toString()
                
                playItemFlyAnimation(goldCard1, 20, R.drawable.gold_ic)
                UserWalletFirestore.applyCurrencyDelta(ctx, uid, 1200) { refreshCurrencyUi() }
            }
        }
        val goldCard2 = v.findViewById<View>(R.id.shopGoldCard2)
        goldCard2.setOnClickListener {
            currentUid?.let { uid ->
                val current = currencyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedCurrency(ctx)
                currencyText?.text = (current + 7000).toString()
                
                playItemFlyAnimation(goldCard2, 50, R.drawable.gold_ic)
                UserWalletFirestore.applyCurrencyDelta(ctx, uid, 7000) { refreshCurrencyUi() }
            }
        }
        val goldCard3 = v.findViewById<View>(R.id.shopGoldCard3)
        goldCard3.setOnClickListener {
            currentUid?.let { uid ->
                val current = currencyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedCurrency(ctx)
                currencyText?.text = (current + 15000).toString()
                
                playItemFlyAnimation(goldCard3, 100, R.drawable.gold_ic)
                UserWalletFirestore.applyCurrencyDelta(ctx, uid, 15000) { refreshCurrencyUi() }
            }
        }

        // --- Anahtar Paketleri ---
        val keyCard1 = v.findViewById<View>(R.id.shopKeyCard1)
        keyCard1.setOnClickListener {
            currentUid?.let { uid ->
                val current = keyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedKeys(ctx)
                keyText?.text = (current + 10).toString()
                
                playItemFlyAnimation(keyCard1, 10, R.drawable.key)
                UserWalletFirestore.applyKeyDelta(ctx, uid, 10) { refreshCurrencyUi() }
            }
        }
        val keyCard2 = v.findViewById<View>(R.id.shopKeyCard2)
        keyCard2.setOnClickListener {
            currentUid?.let { uid ->
                val current = keyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedKeys(ctx)
                keyText?.text = (current + 50).toString()
                
                playItemFlyAnimation(keyCard2, 20, R.drawable.key)
                UserWalletFirestore.applyKeyDelta(ctx, uid, 50) { refreshCurrencyUi() }
            }
        }
        val keyCard3 = v.findViewById<View>(R.id.shopKeyCard3)
        keyCard3.setOnClickListener {
            currentUid?.let { uid ->
                val current = keyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedKeys(ctx)
                keyText?.text = (current + 100).toString()
                
                playItemFlyAnimation(keyCard3, 50, R.drawable.key)
                UserWalletFirestore.applyKeyDelta(ctx, uid, 100) { refreshCurrencyUi() }
            }
        }

        v.isClickable = true; v.isFocusable = true; return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Zamanlayıcıyı başlat
        updateRunnable = object : Runnable {
            override fun run() {
                updateEnergyUi()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable!!)
        refreshCurrencyUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateEnergyUi() {
        val view = view ?: return
        val activity = activity as? MainActivity ?: return
        val em = activity.getEnergyManager()
        val isInfinite = activity.isInfiniteEnergy()
        
        val currentEnergy = em.getCurrentEnergy()
        energyText?.text = if (isInfinite) "∞" else currentEnergy.toString()
        val maxEnergy = em.getMaxEnergy()
        
        val timerText = view.findViewById<TextView>(R.id.shopLifeTimerText)
        val buyButton = view.findViewById<CardView>(R.id.shopBuyLifeButton)
        val buyIcon = view.findViewById<ImageView>(R.id.shopBuyLifeIcon)
                val buyText = view.findViewById<TextView>(R.id.shopBuyLifeText)
        val watchAdButton2 = view.findViewById<View>(R.id.shopWatchAdButton2)
        
        if (isInfinite || currentEnergy >= maxEnergy) {
            timerText.text = "DOLU"
            timerText.setTextColor(Color.parseColor("#78909C")) // Gri
            
            // Butonu gri yap
            buyButton.setCardBackgroundColor(Color.parseColor("#1E2A30"))
            buyText.setTextColor(Color.parseColor("#78909C")) // Koyu gri metin
            buyIcon.alpha = 0.5f
            buyButton.isClickable = false
            
            watchAdButton2.alpha = 0.3f
            watchAdButton2.isClickable = false
        } else {
            val timeToNext = em.getTimeUntilNextEnergy() // in millis
            val totalSeconds = timeToNext / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            timerText.text = String.format("%02d:%02d", minutes, seconds)
            timerText.setTextColor(Color.parseColor("#29B6F6")) // Mavi
            
            // Butonu aktif yap
            buyButton.setCardBackgroundColor(Color.parseColor("#37474F"))
            buyText.setTextColor(Color.parseColor("#FFFFFF"))
            buyIcon.alpha = 1.0f
            buyButton.isClickable = true
            
            watchAdButton2.alpha = 1.0f
            watchAdButton2.isClickable = true
        }
    }

    /** Called by MainActivity after a purchase / wallet update so the header stays fresh. */
    fun refreshCurrencyUi() {
        val ctx = context ?: return
        currencyText?.text = UserWalletFirestore.getCachedCurrency(ctx).toString()
        keyText?.text = UserWalletFirestore.getCachedKeys(ctx).toString()
    }

    private fun playHeartFlyAnimation(button: View) {
        val rootLayout = view as? ViewGroup ?: return
        val context = context ?: return
        
        // DP to PX (kalp ikonu için boyut)
        val sizePx = (30 * context.resources.displayMetrics.density).toInt()
        
        val buttonLoc = IntArray(2)
        button.getLocationInWindow(buttonLoc)
        val rootLoc = IntArray(2)
        rootLayout.getLocationInWindow(rootLoc)
        
        // Butonun ortası
        val startX = buttonLoc[0] - rootLoc[0] + (button.width / 2f) - (sizePx / 2f)
        val startY = buttonLoc[1] - rootLoc[1] + (button.height / 2f) - (sizePx / 2f)
        
        val heartView = ImageView(context).apply {
            setImageResource(R.drawable.heart_ic)
            layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
        }
        
        heartView.translationX = startX
        heartView.translationY = startY
        heartView.scaleX = 0.2f
        heartView.scaleY = 0.2f
        heartView.alpha = 0f
        
        rootLayout.addView(heartView)
        
        // 1. AŞAMA: POP-IN
        heartView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .withEndAction {
                // 2. AŞAMA: TAŞ GİBİ FIRLAMA
                // Sağa veya sola rastgele eğim
                val directionX = if (Math.random() > 0.5) 1f else -1f
                val dx = directionX * (100f + (Math.random() * 150f).toFloat())
                
                val animX = android.animation.ObjectAnimator.ofFloat(heartView, View.TRANSLATION_X, heartView.translationX, heartView.translationX + dx)
                animX.duration = 800
                animX.interpolator = android.view.animation.LinearInterpolator()
                
                // Yukarı fırlama (giderek yavaşlayarak)
                val peakY = heartView.translationY - (300f + Math.random() * 200f).toFloat()
                val animY1 = android.animation.ObjectAnimator.ofFloat(heartView, View.TRANSLATION_Y, heartView.translationY, peakY)
                animY1.duration = 350
                animY1.interpolator = android.view.animation.DecelerateInterpolator()
                
                // Aşağı düşme (ivmelenerek)
                val fallY = peakY + 800f
                val animY2 = android.animation.ObjectAnimator.ofFloat(heartView, View.TRANSLATION_Y, peakY, fallY)
                animY2.duration = 450
                animY2.startDelay = 350 // animY1 bittikten sonra başlasın
                animY2.interpolator = android.view.animation.AccelerateInterpolator()
                
                // Dönerken düşsün
                val animRot = android.animation.ObjectAnimator.ofFloat(heartView, View.ROTATION, 0f, directionX * (180f + Math.random().toFloat() * 180f))
                animRot.duration = 800
                
                // Yok olma efekti
                val animAlpha = android.animation.ObjectAnimator.ofFloat(heartView, View.ALPHA, 1f, 0f)
                animAlpha.duration = 300
                animAlpha.startDelay = 500
                
                val set = android.animation.AnimatorSet()
                set.playTogether(animX, animY1, animY2, animRot, animAlpha)
                set.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        rootLayout.removeView(heartView)
                    }
                })
                set.start()
            }
            .start()
    }

    private fun playItemFlyAnimation(button: View, count: Int, drawableResId: Int) {
        val rootLayout = view as? ViewGroup ?: return
        val context = context ?: return
        
        // DP to PX
        val sizePx = (30 * context.resources.displayMetrics.density).toInt()
        
        val buttonLoc = IntArray(2)
        button.getLocationInWindow(buttonLoc)
        val rootLoc = IntArray(2)
        rootLayout.getLocationInWindow(rootLoc)
        
        // Başlangıç noktası
        val startX = buttonLoc[0] - rootLoc[0] + (button.width / 2f) - (sizePx / 2f)
        val startY = buttonLoc[1] - rootLoc[1] + (button.height / 2f) - (sizePx / 2f)
        
        for (i in 0 until count) {
            button.postDelayed({
                val itemView = ImageView(context).apply {
                    setImageResource(drawableResId)
                    layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                }
                
                itemView.translationX = startX
                itemView.translationY = startY
                itemView.scaleX = 0.2f
                itemView.scaleY = 0.2f
                itemView.alpha = 0f
                
                rootLayout.addView(itemView)
                
                itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1f)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator())
                    .withEndAction {
                        // Taş gibi fırlama (Her yöne rastgele)
                        val directionX = if (Math.random() > 0.5) 1f else -1f
                        val dx = directionX * (100f + (Math.random() * 250f).toFloat())
                        
                        val animX = android.animation.ObjectAnimator.ofFloat(itemView, View.TRANSLATION_X, itemView.translationX, itemView.translationX + dx)
                        animX.duration = 700 + (Math.random() * 200).toLong()
                        animX.interpolator = android.view.animation.LinearInterpolator()
                        
                        val peakY = itemView.translationY - (200f + Math.random() * 300f).toFloat()
                        val animY1 = android.animation.ObjectAnimator.ofFloat(itemView, View.TRANSLATION_Y, itemView.translationY, peakY)
                        animY1.duration = 300 + (Math.random() * 100).toLong()
                        animY1.interpolator = android.view.animation.DecelerateInterpolator()
                        
                        val fallY = peakY + 800f
                        val animY2 = android.animation.ObjectAnimator.ofFloat(itemView, View.TRANSLATION_Y, peakY, fallY)
                        animY2.duration = 500 + (Math.random() * 100).toLong()
                        animY2.startDelay = animY1.duration
                        animY2.interpolator = android.view.animation.AccelerateInterpolator()
                        
                        val animRot = android.animation.ObjectAnimator.ofFloat(itemView, View.ROTATION, 0f, directionX * (180f + Math.random().toFloat() * 360f))
                        animRot.duration = animX.duration
                        
                        val animAlpha = android.animation.ObjectAnimator.ofFloat(itemView, View.ALPHA, 1f, 0f)
                        animAlpha.duration = 300
                        animAlpha.startDelay = animX.duration - 300
                        
                        val set = android.animation.AnimatorSet()
                        set.playTogether(animX, animY1, animY2, animRot, animAlpha)
                        set.addListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                rootLayout.removeView(itemView)
                            }
                        })
                        set.start()
                    }
                    .start()
            }, (i * (400L / count))) // Spread spawns over 400ms
        }
    }
}
