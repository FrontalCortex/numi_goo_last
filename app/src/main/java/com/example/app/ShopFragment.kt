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

    /** Anahtar karşılığı can alımı sürerken tekrar tıklamayı engeller. */
    private var buyLifeInProgress = false

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

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            UserWalletFirestore.loadWallet(ctx, uid, onResult = { wallet ->
                currencyText?.text = wallet.currency.toString()
                keyText?.text = wallet.keys.toString()
            })
        }

        // --- Can (Energy) Bölümü ---
        v.findViewById<View>(R.id.shopProCard).setOnClickListener {
            val mainActivity = activity as? MainActivity ?: return@setOnClickListener
            mainActivity.billingManager.launchPurchase(mainActivity, BillingCatalog.SUB_PRO)
        }

        val buyButton = v.findViewById<View>(R.id.shopBuyLifeButton)
        buyButton.setOnClickListener { buyLifeWithKeys(buyButton) }

        // --- SUPER CARD ---
        val superCard = v.findViewById<View>(R.id.shopSuperCard)
        superCard?.setOnClickListener {
            ProDiffirentFragment().show(requireActivity().supportFragmentManager, "ProDiffirent")
        }

        // --- Özel Teklifler ---
        // Kristal reklam butonu
        val watchAdButton = v.findViewById<View>(R.id.shopWatchAdButton)
        watchAdButton.setOnClickListener {
            val mainActivity = activity as? MainActivity ?: return@setOnClickListener
            mainActivity.adManager.showRewardedAd(mainActivity, showAdSkipAfter = false) { adNonce ->
                if (!isAdded) return@showRewardedAd
                // Sunucu isteğini fragment eklenmeden önce başlat — ilk açılıştaki gecikmeyi gizler.
                ServerRewards.prefetchChest(NewChestFragment.ChestRarity.COMMON.name, adNonce)
                mainActivity.findViewById<View>(R.id.abacusFragmentContainer)?.visibility = View.VISIBLE

                // "chest_closed" anahtarını bu FragmentManager'da başka ekranlar da kullanıyor ve
                // her biri tetiklenince dinleyicisini temizliyor (aynı anahtara yalnızca tek
                // dinleyici kayıtlı kalabilir). Aynı kalıbı burada da koruyoruz.
                parentFragmentManager.setFragmentResultListener("chest_closed", viewLifecycleOwner) { _, _ ->
                    parentFragmentManager.clearFragmentResultListener("chest_closed")
                    if (isAdded) refreshCurrencyUi()
                }

                parentFragmentManager.beginTransaction()
                    .add(
                        R.id.abacusFragmentContainer,
                        NewChestFragment.newInstance(NewChestFragment.ChestRarity.COMMON, adNonce),
                    )
                    .commit()
            }
        }

        val watchAdButton2 = v.findViewById<View>(R.id.shopWatchAdButton2)
        watchAdButton2.setOnClickListener {
            val mainActivity = activity as? MainActivity ?: return@setOnClickListener
            mainActivity.adManager.showRewardedAd(mainActivity, showAdSkipAfter = false) { adNonce ->
                // Can sunucuda eklenir: reklamın gerçekten izlendiği AdMob SSV ile doğrulanır.
                ServerEnergy.claimAdEnergy(
                    adNonce = adNonce,
                    onResult = { fullTime ->
                        mainActivity.getEnergyManager().adoptServerFullTime(fullTime)
                        if (!isAdded) return@claimAdEnergy
                        playHeartFlyAnimation(watchAdButton2)
                        updateEnergyUi()
                    },
                    onFailure = {
                        if (!isAdded) return@claimAdEnergy
                        Toast.makeText(requireContext(), "Can alınamadı. Tekrar deneyin.", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        // --- Altın ve Anahtar Paketleri ---
        // Gerçek para ile satın alınır. Verilecek miktarı SUNUCU belirler; burada yalnızca
        // hangi kartın hangi Play ürününü açtığı bilgisi var. Bkz. docs/SATIN_ALMA_ENTEGRASYONU.md
        CARD_PRODUCTS.forEach { (cardId, productId) ->
            v.findViewById<View>(cardId).setOnClickListener {
                val mainActivity = activity as? MainActivity ?: return@setOnClickListener
                mainActivity.billingManager.launchPurchase(mainActivity, productId)
            }
        }

        v.isClickable = true; v.isFocusable = true; return v
    }

    /**
     * Fiyat etiketlerini Play'den gelen yerelleştirilmiş değerlerle günceller.
     *
     * Play fiyatı kullanıcının ülkesine ve para birimine göre belirler; layout'taki sabit
     * ₺ değerleri yalnızca ürün bilgisi henüz gelmediğinde görünen yedektir.
     */
    private fun applyStorePrices() {
        val view = view ?: return
        val billing = (activity as? MainActivity)?.billingManager ?: return
        CARD_PRICE_LABELS.forEach { (labelId, productId) ->
            val price = billing.formattedPrice(productId) ?: return@forEach
            view.findViewById<TextView>(labelId)?.text = price
        }
        billing.formattedPrice(BillingCatalog.SUB_PRO)?.let { price ->
            view.findViewById<TextView>(R.id.shopProPriceText)?.text = "aylık $price"
        }
    }

    /**
     * [LIFE_KEY_COST] anahtar karşılığında 1 can verir.
     *
     * Can, ancak anahtar sunucuda gerçekten düşüldükten sonra eklenir; aksi halde çağrı
     * reddedildiğinde (yetersiz bakiye, ağ hatası) kullanıcı bedava can kazanırdı.
     */
    private fun buyLifeWithKeys(buyButton: View) {
        if (buyLifeInProgress) return
        val ctx = context ?: return
        val mainActivity = activity as? MainActivity ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val currentKey = keyText?.text?.toString()?.toIntOrNull() ?: UserWalletFirestore.getCachedKeys(ctx)
        if (currentKey < LIFE_KEY_COST) {
            Toast.makeText(ctx, "Yetersiz anahtar!", Toast.LENGTH_SHORT).show()
            return
        }

        buyLifeInProgress = true
        // Anlık (iyimser) gösterim — sunucu reddederse aşağıda önbellekteki değere dönülür.
        keyText?.text = (currentKey - LIFE_KEY_COST).toString()

        // Anahtar düşümü ve can eklemesi sunucuda AYNI transaction'da yapılır; önceki iki
        // adımlı akışta çağrılar arasında uygulama kapanırsa anahtar gidip can gelmiyordu.
        ServerEnergy.buyWithKeys(
            onResult = { fullTime, keys ->
                buyLifeInProgress = false
                mainActivity.getEnergyManager().adoptServerFullTime(fullTime)
                if (!isAdded) return@buyWithKeys
                keyText?.text = keys.toString()
                playHeartFlyAnimation(buyButton)
                updateEnergyUi()
            },
            onFailure = {
                buyLifeInProgress = false
                if (!isAdded) return@buyWithKeys
                refreshCurrencyUi()
                Toast.makeText(ctx, "İşlem tamamlanamadı. Tekrar deneyin.", Toast.LENGTH_SHORT).show()
            },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.billingManager?.let { billing ->
            billing.onPricesReady = { if (isAdded) applyStorePrices() }
            billing.onPurchaseGranted = { productId ->
                if (isAdded) {
                    refreshCurrencyUi()
                    animatePurchasedItem(productId)
                }
                (activity as? MainActivity)?.refreshWalletUi()
            }
            billing.onError = { message ->
                if (isAdded) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            // Ürün bilgisi bu ekran açılmadan önce gelmiş olabilir.
            applyStorePrices()
        }

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
        // Geri çağrıları MainActivity'ye devret; mağaza kapalıyken tamamlanan satın almalar
        // da işlensin ve bu fragment'e sızıntı kalmasın.
        (activity as? MainActivity)?.installDefaultBillingCallbacks()
    }

    /** Satın alınan pakete uygun kutlama animasyonunu oynatır. */
    private fun animatePurchasedItem(productId: String) {
        val view = view ?: return
        val (cardId, iconRes) = when (productId) {
            BillingCatalog.GOLD_SMALL -> R.id.shopGoldCard1 to R.drawable.gold_ic
            BillingCatalog.GOLD_MEDIUM -> R.id.shopGoldCard2 to R.drawable.gold_ic
            BillingCatalog.GOLD_LARGE -> R.id.shopGoldCard3 to R.drawable.gold_ic
            BillingCatalog.KEYS_SMALL -> R.id.shopKeyCard1 to R.drawable.key
            BillingCatalog.KEYS_MEDIUM -> R.id.shopKeyCard2 to R.drawable.key
            BillingCatalog.KEYS_LARGE -> R.id.shopKeyCard3 to R.drawable.key
            else -> return
        }
        val card = view.findViewById<View>(cardId) ?: return
        playItemFlyAnimation(card, PURCHASE_ANIMATION_ITEM_COUNT, iconRes)
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
        buyText.text = LIFE_KEY_COST.toString()

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

    /** Bir karttan çok sayıda altın/anahtar fırlatan kutlama animasyonu. */
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

    companion object {
        /** 1 can satın almanın anahtar bedeli. Arayüzdeki etiket de bu değerden yazılır. */
        private const val LIFE_KEY_COST = 1

        /** Satın alma sonrası fırlatılan ikon sayısı (yalnızca görsel). */
        private const val PURCHASE_ANIMATION_ITEM_COUNT = 30

        /** Kart -> Play ürün kimliği. */
        private val CARD_PRODUCTS = listOf(
            R.id.shopGoldCard1 to BillingCatalog.GOLD_SMALL,
            R.id.shopGoldCard2 to BillingCatalog.GOLD_MEDIUM,
            R.id.shopGoldCard3 to BillingCatalog.GOLD_LARGE,
            R.id.shopKeyCard1 to BillingCatalog.KEYS_SMALL,
            R.id.shopKeyCard2 to BillingCatalog.KEYS_MEDIUM,
            R.id.shopKeyCard3 to BillingCatalog.KEYS_LARGE,
        )

        /** Fiyat etiketi -> Play ürün kimliği. */
        private val CARD_PRICE_LABELS = listOf(
            R.id.shopGoldPrice1 to BillingCatalog.GOLD_SMALL,
            R.id.shopGoldPrice2 to BillingCatalog.GOLD_MEDIUM,
            R.id.shopGoldPrice3 to BillingCatalog.GOLD_LARGE,
            R.id.shopKeyPrice1 to BillingCatalog.KEYS_SMALL,
            R.id.shopKeyPrice2 to BillingCatalog.KEYS_MEDIUM,
            R.id.shopKeyPrice3 to BillingCatalog.KEYS_LARGE,
        )
    }
}
