package com.example.app

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ShopFragment : Fragment() {

    private var currencyText: TextView? = null
    private var keyText: TextView? = null
    private var energyText: TextView? = null

    /**
     * Altın/anahtar bakiyesinin CANLI dinleyicisi.
     *
     * Satın alma tamamlandığında ([BillingManager.onPurchaseGranted]) bakiyeyi SUNUCU yazıyor;
     * yerel önbelleğe kimse dokunmuyor. Önbelleği ancak bir Firestore anlık görüntüsü tazeliyor
     * (bkz. [UserWalletFirestore.listenToWallet] -> `cacheLocally`). Bu yüzden satın alma
     * geri çağrısında önbellekten okumak yarış demek: Play'in "tüketildi" geri çağrısı
     * anlık görüntüden önce gelirse başlıkta ESKİ sayı kalır ve onu tazeleyen başka bir şey
     * olmaz. Dinleyici bu yarışı tamamen ortadan kaldırıyor.
     */
    private var walletListener: ListenerRegistration? = null

    /** Canlı dinleyiciden en az bir değer geldi mi? Geç gelen tek seferlik okumayı eler. */
    private var walletLiveValueSeen = false

    /** Anahtar karşılığı can alımı sürerken tekrar tıklamayı engeller. */
    private var buyLifeInProgress = false

    /** Altın karşılığı seri dondurma alımı sürerken tekrar tıklamayı engeller. */
    private var buyFreezeInProgress = false

    /**
     * Dondurma teklifi bu ekran ömründe ölçüme bildirildi mi.
     *
     * [renderStreakFreezeCard] her bakiye/dondurma değişiminde yeniden çalışıyor; olay
     * her çizimde gönderilse "kaç kez görüldü" sayısı çizim sayısına dönüşür ve dönüşüm
     * oranı anlamsızlaşırdı.
     */
    private var freezeOfferLogged = false

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
                if (walletLiveValueSeen) return@loadWallet
                currencyText?.text = wallet.currency.toString()
                keyText?.text = wallet.keys.toString()
            })
            walletListener?.remove()
            walletListener = UserWalletFirestore.listenToWallet(
                context = ctx.applicationContext,
                uid = uid,
                onUpdate = { wallet ->
                    walletLiveValueSeen = true
                    currencyText?.text = wallet.currency.toString()
                    keyText?.text = wallet.keys.toString()
                },
            )
        }

        // --- Can (Energy) Bölümü ---
        // Pro kartı doğrudan satın alma başlatmaz; önce plan karşılaştırma ekranını açar
        // (shopSuperCard ile aynı davranış). Kullanıcı neyi satın aldığını görmeden ödeme
        // akışına girmemeli.
        v.findViewById<View>(R.id.shopProCard).setOnClickListener {
            ProDiffirentFragment
                .newInstance(AnalyticsLogger.PRO_ENTRY_SHOP)
                .show(requireActivity().supportFragmentManager, "ProDiffirent")
        }

        val buyButton = v.findViewById<View>(R.id.shopBuyLifeButton)
        buyButton.setOnClickListener { buyLifeWithKeys(buyButton) }

        // --- SUPER CARD ---
        val superCard = v.findViewById<View>(R.id.shopSuperCard)
        superCard?.setOnClickListener {
            ProDiffirentFragment
                .newInstance(AnalyticsLogger.PRO_ENTRY_SHOP)
                .show(requireActivity().supportFragmentManager, "ProDiffirent")
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
                        NewChestFragment.newInstance(
                            NewChestFragment.ChestRarity.COMMON,
                            adNonce,
                            source = AnalyticsLogger.CHEST_SOURCE_SHOP_AD,
                        ),
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
                        AnalyticsLogger.logEnergyRefill(
                            refillSource = AnalyticsLogger.ENERGY_REFILL_AD,
                            energyAfter = mainActivity.getEnergyManager().getCurrentEnergy(),
                        )
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

        // Seri dondurma: altınla alınır. Kartın görünümü (fiyat düğmesi / "HAZIR" rozeti)
        // onViewCreated'da çiziliyor — burada `view` henüz yok.
        val buyFreezeButton = v.findViewById<View>(R.id.shopBuyStreakFreezeButton)
        buyFreezeButton.setOnClickListener { confirmBuyStreakFreeze(buyFreezeButton) }

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
        // "1 HAFTA ÜCRETSİZ DENE" layout'ta sabitti; denemeye uygun olmayan kullanıcıya
        // yanlış vaat oluyordu.
        SubscriptionCta.apply(billing, view.findViewById(R.id.superButton))
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
                AnalyticsLogger.logEnergyRefill(
                    refillSource = AnalyticsLogger.ENERGY_REFILL_KEYS,
                    energyAfter = mainActivity.getEnergyManager().getCurrentEnergy(),
                )
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

    /**
     * Mağazayı seri dondurma kartına kaydırılmış açar ve kartı bir kez vurgular.
     *
     * Seri ekranındaki "Seri dondurma: Yok" satırından gelindiğinde kullanılıyor: kart
     * mağazanın ortalarında, tepeden açılsaydı çocuk gönderildiği şeyi aramak zorunda kalırdı.
     *
     * Kaydırma ANINDA (yumuşak değil): mağaza zaten kayarak açılıyor, üstüne ikinci bir
     * hareket binince ekran çalkalanıyor. Vurgu ise açılış animasyonu bittikten sonra
     * oynuyor, yoksa görülmeden biterdi.
     */
    private fun focusStreakFreezeCard(root: View) {
        val scroll = root.findViewById<ScrollView>(R.id.shopScrollView) ?: return
        val card = root.findViewById<View>(R.id.shopStreakFreezeCard) ?: return
        // Kartın konumu ölçüm bitmeden bilinmiyor.
        scroll.doOnLayout {
            val bounds = Rect()
            card.getDrawingRect(bounds)
            scroll.offsetDescendantRectToMyCoords(card, bounds)
            val gap = (FOCUS_TOP_GAP_DP * resources.displayMetrics.density).toInt()
            scroll.scrollTo(0, (bounds.top - gap).coerceAtLeast(0))

            card.postDelayed({
                if (!isAdded) return@postDelayed
                card.animate()
                    .scaleX(FOCUS_PULSE_SCALE).scaleY(FOCUS_PULSE_SCALE)
                    .setDuration(FOCUS_PULSE_HALF_MS)
                    .withEndAction {
                        card.animate().scaleX(1f).scaleY(1f).setDuration(FOCUS_PULSE_HALF_MS).start()
                    }
                    .start()
            }, FOCUS_PULSE_DELAY_MS)
        }
    }

    /**
     * Seri dondurma kartını çizer: elde yoksa fiyat düğmesi, varsa "✓ HAZIR" rozeti.
     *
     * Dondurma eldeyken düğme griye boyanıp bırakılmıyor: gri bir fiyat düğmesi "altınım
     * yetmiyor" ya da "bozuk" diye de okunuyor. Rozet ve değişen açıklama, bunun bir engel
     * değil sahip olunan bir şey olduğunu söylüyor ve neden yenisinin alınamadığını da
     * açıklıyor (aynı anda yalnızca bir tane tutulabiliyor).
     */
    private fun renderStreakFreezeCard() {
        val view = view ?: return
        val ctx = context ?: return
        val held = StreakRepository.freezesHeld(ctx) > 0

        val button = view.findViewById<CardView>(R.id.shopBuyStreakFreezeButton)
        val icon = view.findViewById<ImageView>(R.id.shopBuyStreakFreezeIcon)
        val text = view.findViewById<TextView>(R.id.shopBuyStreakFreezeText)
        val desc = view.findViewById<TextView>(R.id.shopStreakFreezeDesc)

        if (held) {
            desc.text = "Serin korunuyor. Kullanınca yenisini alabilirsin."
            button.setCardBackgroundColor(Color.parseColor("#17394B"))
            icon.visibility = View.GONE
            text.text = "✓  HAZIR"
            text.setTextColor(Color.parseColor("#4FC3F7"))
            button.isClickable = false
        } else {
            // Yalnızca ALINABİLİR hâl bir teklif; "✓ HAZIR" değil. Satın almanın paydası:
            // streak_freeze_offer_shown → gold_spent[item_id=streak_freeze].
            if (!freezeOfferLogged) {
                freezeOfferLogged = true
                AnalyticsLogger.logStreakFreezeOfferShown(
                    canAfford = UserWalletFirestore.getCachedCurrency(ctx) >=
                        StreakRepository.FREEZE_COST_GOLD,
                )
            }
            desc.text = "Serini 1 gün boyunca korur. Serin bozulsa bile ilerlemeye devam et!"
            button.setCardBackgroundColor(Color.parseColor("#37474F"))
            icon.visibility = View.VISIBLE
            text.text = StreakRepository.FREEZE_COST_GOLD.toString()
            text.setTextColor(Color.parseColor("#FFFFFF"))
            button.isClickable = true
        }
    }

    /**
     * Seri dondurma için onay penceresi; onaylanırsa [buyStreakFreeze].
     *
     * Can alımında onay yok çünkü bedeli 1 anahtar. Bu ise 4000 altın — bir çocuğun günlerce
     * biriktirdiği miktar — ve geri alınamıyor; kaydırırken yanlışlıkla dokunmak yeterli
     * olmamalı. Pencere seri ekranındakiyle aynı ([StreakFragment] hedef seçimi), yeni bir
     * görsel dil eklenmiyor.
     */
    private fun confirmBuyStreakFreeze(buyButton: View) {
        if (buyFreezeInProgress) return
        val ctx = context ?: return
        if (StreakRepository.freezesHeld(ctx) > 0) return

        val cost = StreakRepository.FREEZE_COST_GOLD
        val gold = currencyText?.text?.toString()?.toIntOrNull()
            ?: UserWalletFirestore.getCachedCurrency(ctx)
        if (gold < cost) {
            Toast.makeText(ctx, "Yetersiz altın!", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_streak_options, null)
        val dialog = AlertDialog.Builder(ctx).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<TextView>(R.id.streakOptionsTitle).text = "Seri Dondurma"
        dialogView.findViewById<TextView>(R.id.streakOptionsSubtitle).text =
            "Bir gün çalışamazsan serin bozulmaz. $cost altın harcanacak."
        StreakViews.buildOptionRows(
            container = dialogView.findViewById(R.id.streakOptionsContainer),
            values = listOf(CONFIRM_BUY, CONFIRM_CANCEL),
            labels = listOf("Satın al", "Vazgeç"),
            trailing = listOf("$cost altın", ""),
            selected = CONFIRM_BUY,
            trailingColor = StreakViews.COLOR_GOLD,
        ) { choice ->
            dialog.dismiss()
            if (choice == CONFIRM_BUY) buyStreakFreeze(buyButton)
        }
        dialog.show()
    }

    /**
     * [StreakRepository.FREEZE_COST_GOLD] altın karşılığında bir seri dondurma alır.
     *
     * Bakiye iyimser düşülmüyor (can alımındaki gibi): rakam büyük, sunucu reddederse
     * çocuk 4000 altınının bir an için gittiğini görmüş olur. Yeni bakiye yanıttan yazılıyor.
     */
    private fun buyStreakFreeze(buyButton: View) {
        if (buyFreezeInProgress) return
        val ctx = context ?: return
        buyFreezeInProgress = true
        val appContext = ctx.applicationContext
        StreakSyncService.buyFreeze(appContext) { success, message, currency ->
            buyFreezeInProgress = false
            if (!isAdded) return@buyFreeze
            if (success) {
                currencyText?.text = currency.toString()
                // Üst bar da aynı cüzdanı gösteriyor; mağaza kapanınca eski sayı kalmasın.
                (activity as? MainActivity)?.refreshWalletUi()
                playItemFlyAnimation(buyButton, 1, R.drawable.streak_freeze_ic)
                renderStreakFreezeCard()
                // Mağaza seri ekranının üstünde açılmış olabilir; o ekran kendiliğinden
                // yeniden çizilmiyor (bkz. StreakFragment.onViewCreated).
                parentFragmentManager.setFragmentResult(RESULT_STREAK_FREEZE_BOUGHT, Bundle())
            } else {
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                // Reddin sebebi bayat önbellek olabilir (dondurma başka bir cihazda alınmış
                // ya da araya eski bir okuma girmiş): doğrusu sunucudan çekilip kart ona göre
                // çiziliyor, yoksa düğme açık kalır ve her dokunuş aynı hatayı verir.
                StreakSyncService.refreshFromServer(appContext) {
                    if (isAdded) renderStreakFreezeCard()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        renderStreakFreezeCard()
        // Kart yerel önbellekten çizildi; dondurma dün harcanmış ya da başka bir cihazda
        // alınmış olabilir, o yüzden sunucudaki durum da okunup yeniden çiziliyor.
        StreakSyncService.refreshFromServer(requireContext().applicationContext) {
            if (isAdded) renderStreakFreezeCard()
        }
        // Yalnızca ilk açılışta: ekran yeniden kurulduğunda (döndürme, süreç ölümü) sistem
        // kaydırma konumunu kendisi geri yüklüyor, bir daha karta sıçramak onu ezerdi.
        if (savedInstanceState == null &&
            arguments?.getBoolean(ARG_FOCUS_STREAK_FREEZE) == true
        ) {
            focusStreakFreezeCard(view)
        }

        (activity as? MainActivity)?.billingManager?.let { billing ->
            billing.onPricesReady = { if (isAdded) applyStorePrices() }
            billing.onPurchaseGranted = { productId ->
                if (isAdded) {
                    refreshCurrencyUi()
                    refreshCreditBalance()
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
        refreshCreditBalance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        walletListener?.remove()
        walletListener = null
        walletLiveValueSeen = false
        updateRunnable?.let { handler.removeCallbacks(it) }
        // Geri çağrıları MainActivity'ye devret; mağaza kapalıyken tamamlanan satın almalar
        // da işlensin ve bu fragment'e sızıntı kalmasın.
        (activity as? MainActivity)?.installDefaultBillingCallbacks()
    }

    /** Satın alınan pakete uygun kutlama animasyonunu oynatır. */
    private fun animatePurchasedItem(productId: String) {
        val view = view ?: return
        // Kredi paketlerinde fırlatılan ikon sayısı verilen kredi adedini birebir yansıtır
        // (1 / 5 / 10). Altın ve anahtarda adetler çok yüksek olduğu için orada sabit bir
        // görsel yoğunluk (PURCHASE_ANIMATION_ITEM_COUNT) kullanılıyor.
        val (cardId, iconRes, itemCount) = when (productId) {
            BillingCatalog.GOLD_SMALL ->
                Triple(R.id.shopGoldCard1, R.drawable.gold_ic, PURCHASE_ANIMATION_ITEM_COUNT)
            BillingCatalog.GOLD_MEDIUM ->
                Triple(R.id.shopGoldCard2, R.drawable.gold_ic, PURCHASE_ANIMATION_ITEM_COUNT)
            BillingCatalog.GOLD_LARGE ->
                Triple(R.id.shopGoldCard3, R.drawable.gold_ic, PURCHASE_ANIMATION_ITEM_COUNT)
            BillingCatalog.KEYS_SMALL ->
                Triple(R.id.shopKeyCard1, R.drawable.key, PURCHASE_ANIMATION_ITEM_COUNT)
            BillingCatalog.KEYS_MEDIUM ->
                Triple(R.id.shopKeyCard2, R.drawable.key, PURCHASE_ANIMATION_ITEM_COUNT)
            BillingCatalog.KEYS_LARGE ->
                Triple(R.id.shopKeyCard3, R.drawable.key, PURCHASE_ANIMATION_ITEM_COUNT)
            BillingCatalog.CREDITS_SMALL ->
                Triple(R.id.shopCreditCard1, R.drawable.chat_credit_ic3, 1)
            BillingCatalog.CREDITS_MEDIUM ->
                Triple(R.id.shopCreditCard2, R.drawable.chat_credit_ic3, 5)
            BillingCatalog.CREDITS_LARGE ->
                Triple(R.id.shopCreditCard3, R.drawable.chat_credit_ic3, 10)
            else -> return
        }
        val card = view.findViewById<View>(cardId) ?: return
        playItemFlyAnimation(card, itemCount, iconRes)
    }

    private fun updateEnergyUi() {
        val view = view ?: return
        val activity = activity as? MainActivity ?: return
        val em = activity.getEnergyManager()
        val isInfinite = activity.isInfiniteEnergy()
        
        val currentEnergy = em.getCurrentEnergy()
        EnergyDisplay.apply(
            text = energyText,
            infiniteBadge = view.findViewById(R.id.shopEnergyInfiniteBadge),
            icon = view.findViewById(R.id.shopEnergyIcon),
            isInfinite = isInfinite,
            isPremium = PlanStatus.isProPlan(em.getUserPlan()),
            value = currentEnergy.toString(),
        )
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

    /**
     * Danışma kredisi bakiyesini gösterir.
     *
     * Bakiye cüzdan önbelleğinde tutulmuyor (o yalnızca altın/anahtar için); alan
     * sunucuya ait ve seyrek değiştiği için doğrudan tek seferlik okunuyor.
     */
    private fun refreshCreditBalance() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val credits = doc.getLong("questionCredits")?.toInt() ?: 0
                view?.findViewById<TextView>(R.id.shopCreditText)?.text = credits.toString()
                applyProCreditBonusUi(doc)
            }
    }

    /**
     * Kredi kartlarinda Pro bonusunu gosterir.
     *
     * Pro uyeye alacagi GERCEK adet yazilir (6 / 12) ve bonusun dahil oldugu belirtilir;
     * digerlerine baz adet kalir ve Pro'ya gecince ne kazanacagi gosterilir. Boylece tek
     * bir yerlesim hem odulu hem yukseltme gerekcesini anlatir.
     *
     * Buradaki bonus degerleri functions/index.js -> PRO_CREDIT_BONUS ile ayni olmalidir.
     * 1'lik pakette bonus yoktur, o karta rozet konmaz.
     */
    private fun applyProCreditBonusUi(doc: DocumentSnapshot) {
        val v = view ?: return
        val isPro = PlanStatus.isPro(doc)

        fun apply(amountId: Int, bonusId: Int, base: Int, bonus: Int) {
            v.findViewById<TextView>(amountId)?.text =
                (if (isPro) base + bonus else base).toString()
            v.findViewById<TextView>(bonusId)?.apply {
                text = if (isPro) "bonus dahil" else "Pro'da +$bonus"
                visibility = View.VISIBLE
            }
        }
        apply(R.id.shopCreditAmount2, R.id.shopCreditBonus2, 5, 1)
        apply(R.id.shopCreditAmount3, R.id.shopCreditBonus3, 10, 2)
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

        /** Seri dondurma onay penceresindeki iki satırın değerleri. */
        private const val CONFIRM_BUY = 1
        private const val CONFIRM_CANCEL = 0

        /** Seri dondurma satın alındı; dinleyen: [StreakFragment]. */
        const val RESULT_STREAK_FREEZE_BOUGHT = "streak_freeze_bought"

        private const val ARG_FOCUS_STREAK_FREEZE = "focus_streak_freeze"

        /** Karta kaydırıldığında kartın üstünde bırakılan boşluk; başlık kartın tepesine yapışmasın. */
        private const val FOCUS_TOP_GAP_DP = 16

        /** Vurgu, mağazanın kayarak açılması bittikten sonra başlıyor. */
        private const val FOCUS_PULSE_DELAY_MS = 450L
        private const val FOCUS_PULSE_HALF_MS = 160L
        private const val FOCUS_PULSE_SCALE = 1.04f

        /**
         * @param focusStreakFreeze true ise mağaza seri dondurma kartında açılır
         *   (bkz. [focusStreakFreezeCard]).
         */
        fun newInstance(focusStreakFreeze: Boolean = false): ShopFragment =
            ShopFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FOCUS_STREAK_FREEZE, focusStreakFreeze)
                }
            }

        /** Kart -> Play ürün kimliği. */
        private val CARD_PRODUCTS = listOf(
            R.id.shopGoldCard1 to BillingCatalog.GOLD_SMALL,
            R.id.shopGoldCard2 to BillingCatalog.GOLD_MEDIUM,
            R.id.shopGoldCard3 to BillingCatalog.GOLD_LARGE,
            R.id.shopKeyCard1 to BillingCatalog.KEYS_SMALL,
            R.id.shopKeyCard2 to BillingCatalog.KEYS_MEDIUM,
            R.id.shopKeyCard3 to BillingCatalog.KEYS_LARGE,
            R.id.shopCreditCard1 to BillingCatalog.CREDITS_SMALL,
            R.id.shopCreditCard2 to BillingCatalog.CREDITS_MEDIUM,
            R.id.shopCreditCard3 to BillingCatalog.CREDITS_LARGE,
        )

        /** Fiyat etiketi -> Play ürün kimliği. */
        private val CARD_PRICE_LABELS = listOf(
            R.id.shopGoldPrice1 to BillingCatalog.GOLD_SMALL,
            R.id.shopGoldPrice2 to BillingCatalog.GOLD_MEDIUM,
            R.id.shopGoldPrice3 to BillingCatalog.GOLD_LARGE,
            R.id.shopKeyPrice1 to BillingCatalog.KEYS_SMALL,
            R.id.shopKeyPrice2 to BillingCatalog.KEYS_MEDIUM,
            R.id.shopKeyPrice3 to BillingCatalog.KEYS_LARGE,
            R.id.shopCreditPrice1 to BillingCatalog.CREDITS_SMALL,
            R.id.shopCreditPrice2 to BillingCatalog.CREDITS_MEDIUM,
            R.id.shopCreditPrice3 to BillingCatalog.CREDITS_LARGE,
        )
    }
}
