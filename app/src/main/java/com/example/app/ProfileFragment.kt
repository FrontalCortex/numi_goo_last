package com.example.app

import android.content.Intent
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentProfileBinding
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var authManager: AuthManager
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var isDataLoaded = false
    private var isUserDataLoading = false
    private val lockedBaseColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_locked_base)
    private val lockedTopColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_locked_top)
    private val unlockedValueStrokeColor = 0xFFFF8F00.toInt()
    private val bronzeToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_bronze_filter)
    private val silverToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_silver_filter)
    private val redToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_red_filter)
    private val purpleToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_purple_filter)
    private val badgeProgressCollection = "badgeProgress"
    private val badgeProgressStateDoc = "state"

    private data class ProfileBadgeSlot(
        val container: View,
        val base: LottieAnimationView,
        val top: LottieAnimationView,
        val valueText: TextView,
    )

    private data class ProfileBadgeRenderConfig(
        val baseAsset: String?,
        val topAsset: String,
        val topHoldFrame: Float,
        val topScale: Float,
        val topBackgroundRes: Int?,
    )

    // unlocked=true rozetler soldan sağa bu sıraya göre önceliklidir.
    private val unlockedPriorityOrder = listOf(
        BadgeKind.CUP,
        BadgeKind.GOLD,
        BadgeKind.SILVER,
        BadgeKind.BRONZE,
        BadgeKind.KARATE,
        BadgeKind.BOWLING,
        BadgeKind.DART,
        BadgeKind.FISHING,
        BadgeKind.GOLF,
        BadgeKind.ROCKET,
    )

    // unlocked=false rozetler, unlocked slotları dolduktan sonra bu sırayla eklenir.
    private val lockedPriorityOrder = listOf(
        BadgeKind.ROCKET,
        BadgeKind.GOLF,
        BadgeKind.FISHING,
        BadgeKind.DART,
        BadgeKind.BOWLING,
        BadgeKind.KARATE,
        BadgeKind.BRONZE,
        BadgeKind.SILVER,
        BadgeKind.GOLD,
        BadgeKind.CUP,
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val googleReauthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                // Google hesabı ile yeniden kimlik doğrulama yap
                reauthenticateWithGoogleAccount(account)
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google ile kimlik doğrulama başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val subscriptionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Plan değişti, kullanıcı bilgilerini yeniden yükle
            isDataLoaded = false // Verileri yeniden yükle
            showLoadingState() // Loading state göster
            loadUserData() // Verileri yükle
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        authManager = AuthManager()
        authManager.initialize(requireContext())
        
        // Önce widget'ları gizle (veriler yüklenene kadar)
        // Layout'ta visibility="gone" olarak ayarlandı, burada da emin olalım
        showLoadingState()
        binding.profileBadgesCard.visibility = View.GONE
        
        setupClickListeners()
        
        // Verileri yükle
        loadUserData()
    }
    
    override fun onResume() {
        super.onResume()
        // İlk yükleme yapılmadıysa veya e-posta doğrulama kontrolü gerekiyorsa yeniden yükle
        if (!isDataLoaded && !isUserDataLoading) {
            reloadUserData()
        } else {
            // Sadece e-posta doğrulama durumunu kontrol et (hızlı kontrol)
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.reload()
                    .addOnSuccessListener {
                        // E-posta doğrulama durumunu kontrol et
                        if (currentUser.isEmailVerified) {
                            firestore.collection("users").document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { doc ->
                                    val verified = doc.getBoolean("verified") ?: false
                                    if (!verified) {
                                        // E-posta doğrulanmış ama Firestore'da güncellenmemiş
                                        firestore.collection("users").document(currentUser.uid)
                                            .update("verified", true)
                                            .addOnSuccessListener {
                                                binding.cardEmailVerification.visibility = View.GONE
                                            }
                                    }
                                }
                        }
                    }
            }
        }
    }
    
    private fun showLoadingState() {
        // Widget'ları gizle veya loading göster
        binding.tvMembershipStatus.visibility = View.GONE
        binding.tvCompletedLessons.visibility = View.GONE
        binding.tvTotalTime.visibility = View.GONE
        binding.tvSubscriptionInfo.visibility = View.GONE
    }
    
    private fun hideLoadingState() {
        // Widget'ları göster
        binding.tvMembershipStatus.visibility = View.VISIBLE
        binding.tvCompletedLessons.visibility = View.VISIBLE
        binding.tvTotalTime.visibility = View.VISIBLE
        binding.tvSubscriptionInfo.visibility = View.VISIBLE
    }
    
    private fun reloadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Firebase Auth'da kullanıcı bilgilerini yeniden yükle
            currentUser.reload()
                .addOnSuccessListener {
                    // Fragment hala ekliyse reload başarılı - kullanıcı bilgilerini tekrar yükle
                    if (!isAdded || view == null) return@addOnSuccessListener
                    loadUserData()
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Kullanıcı bilgileri yeniden yüklenemedi", e)
                }
        }
    }
    
    private fun loadUserData() {
        if (isUserDataLoading) return
        isUserDataLoading = true
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Kullanıcı giriş yapmamış
            isUserDataLoading = false
            return
        }

         // Fragment veya view artık bağlı değilse Glide ile yükleme yapma
        if (!isAdded || view == null) {
            isUserDataLoading = false
            return
        }
        
        // Profil fotoğrafı yükle (Google hesabı avatarı)
        val photoUrl = currentUser.photoUrl
        if (photoUrl != null) {
            Glide.with(requireContext())
                .load(photoUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.imgProfilePhoto)
        } else {
            // Profil fotoğrafı yoksa varsayılan avatar göster
            binding.imgProfilePhoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        // Kullanıcı adı ve e-posta (isim boşsa varsayılan "Kullanıcı")
        binding.tvUserName.text = currentUser.displayName.takeIf { !it.isNullOrBlank() } ?: "Kullanıcı"
        binding.tvUserEmail.text = currentUser.email ?: ""
        
        // Firestore'dan kullanıcı bilgilerini yükle
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) {
                    isUserDataLoading = false
                    return@addOnSuccessListener
                }
                if (doc.exists()) {
                    loadBadgeProgressFromFirestore(currentUser.uid)

                    // Kullanıcı ID'sini göster
                    val userId = doc.getString("userId") ?: ""
                    if (userId.isNotEmpty()) {
                        // userId'yi email'in altında göster (eğer layout'ta bir TextView varsa)
                        // Şimdilik log'a yazıyoruz, layout'a ekleyebiliriz
                        android.util.Log.d("ProfileFragment", "User ID: $userId")
                    }
                    
                    // E-posta doğrulama durumu - Firebase Auth ve Firestore'u senkronize et
                    val firestoreVerified = doc.getBoolean("verified") ?: false
                    val firebaseAuthVerified = currentUser.isEmailVerified
                    
                    // Firebase Auth'da doğrulanmış ama Firestore'da değilse güncelle
                    if (firebaseAuthVerified && !firestoreVerified) {
                        firestore.collection("users").document(currentUser.uid)
                            .update("verified", true)
                            .addOnSuccessListener {
                                if (!isAdded) return@addOnSuccessListener
                                binding.cardEmailVerification.visibility = View.GONE
                            }
                    } else if (!firebaseAuthVerified && !firestoreVerified) {
                        // E-posta doğrulanmamış - uyarı göster
                        binding.cardEmailVerification.visibility = View.VISIBLE
                    } else {
                        binding.cardEmailVerification.visibility = View.GONE
                    }
                    
                    // Üyelik durumu - plan alanını kontrol et
                    val plan = doc.getString("plan") ?: "Free"
                    val isPremium = doc.getBoolean("isPremium") ?: false
                    val subscriptionEndDate = doc.getTimestamp("subscriptionEndDate")
                    
                    // Plan adını göster
                    when (plan) {
                        "Pro" -> {
                            if (subscriptionEndDate != null) {
                                val dateFormat = SimpleDateFormat("d MMMM", Locale("tr", "TR"))
                                binding.tvMembershipStatus.text = "Pro Plan"
                            } else {
                                binding.tvMembershipStatus.text = "Pro Plan"
                            }
                            binding.tvMembershipStatus.setTextColor(requireContext().getColor(R.color.background_color))
                        }
                        "Premium" -> {
                            if (subscriptionEndDate != null) {
                                val dateFormat = SimpleDateFormat("d MMMM", Locale("tr", "TR"))
                                binding.tvMembershipStatus.text = "Premium Plan"
                            } else {
                                binding.tvMembershipStatus.text = "Premium Plan"
                            }
                            binding.tvMembershipStatus.setTextColor(requireContext().getColor(R.color.background_color))
                        }
                        else -> {
                            // Free plan veya plan belirtilmemiş
                            binding.tvMembershipStatus.text = "Free Plan"
                            binding.tvMembershipStatus.setTextColor(requireContext().getColor(R.color.background_color))
                        }
                    }
                    
                    // Kayıt tarihi
                    val createdAt = doc.getTimestamp("createdAt")
                    if (createdAt != null) {
                        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))
                        val createdDateStr = dateFormat.format(createdAt.toDate())
                        // Kayıt tarihini aktivite bilgilerine ekleyebiliriz
                    }
                    
                    // Tamamlanan ders sayısı - GlobalLessonData'dan stepIsFinish kontrolü ile hesapla
                    val completedCount = calculateCompletedLessons()
                    binding.tvCompletedLessons.text = "📚 $completedCount Ders"
                    
                    // Toplam geçirilen süre - TimeTracker'dan al (güncel değer)
                    val totalTimeSpent = TimeTracker.getTotalTimeSeconds()
                    updateTotalTimeDisplay(totalTimeSpent)
                    
                    // Firestore'daki değerle senkronize et (eğer farklıysa)
                    val firestoreTimeSpent = doc.getLong("totalTimeSpent") ?: 0L
                    if (totalTimeSpent != firestoreTimeSpent) {
                        // TimeTracker'daki değer daha güncel, Firestore'u güncelle
                        firestore.collection("users").document(currentUser.uid)
                            .update("totalTimeSpent", totalTimeSpent)
                            .addOnFailureListener { e ->
                                Log.e("ProfileFragment", "Firestore'a süre güncellenemedi", e)
                            }
                    }
                    
                    // Abonelik bitiş tarihi
                    if (subscriptionEndDate != null) {
                        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))
                        val endDateStr = dateFormat.format(subscriptionEndDate.toDate())
                        binding.tvSubscriptionInfo.text = "⏳ Abonelik: Aktif (Son gün: $endDateStr)"
                    } else {
                        binding.tvSubscriptionInfo.text = "⏳ Abonelik: Aktif değil"
                    }
                    
                    // Veriler yüklendi, widget'ları göster
                    hideLoadingState()
                    isDataLoaded = true
                    isUserDataLoading = false
                } else {
                    BadgeProgressRepository.update(UserBadgeProgress())
                    bindRandomProfileBadges()
                    // Firestore'da kayıt yok
                    binding.tvMembershipStatus.text = "Free Üye"
                    binding.tvSubscriptionInfo.text = "⏳ Abonelik: Aktif değil"
                    
                    // Veriler yüklendi (varsayılan değerlerle), widget'ları göster
                    hideLoadingState()
                    isDataLoaded = true
                    isUserDataLoading = false
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) {
                    isUserDataLoading = false
                    return@addOnFailureListener
                }
                BadgeProgressRepository.update(UserBadgeProgress())
                bindRandomProfileBadges()
                Log.e("ProfileFragment", "Firestore'dan kullanıcı bilgileri yüklenemedi", e)
                // Hata durumunda da widget'ları göster (varsayılan değerlerle)
                hideLoadingState()
                isDataLoaded = true
                isUserDataLoading = false
            }
    }

    private fun badgeProgressDoc(uid: String) = firestore.collection("users")
        .document(uid)
        .collection(badgeProgressCollection)
        .document(badgeProgressStateDoc)

    private fun loadBadgeProgressFromFirestore(uid: String) {
        binding.profileBadgesCard.visibility = View.GONE
        val defaults = mapOf(
            "userDartProgress" to 0L,
            "userKarateProgress" to 0L,
            "userGolfProgress" to 0L,
            "userBowlingProgress" to 0L,
            "userFishingProgress" to 0L,
            "userFishingStreak" to 0L,
            "userFishingStreakPeriodKey" to "",
            "userRocketProgress" to 0L,
            "userRocketDailyLessons" to 0L,
            "userRocketDailyDayId" to "",
        )
        badgeProgressDoc(uid).get()
            .addOnSuccessListener { badgeDoc ->
                if (!isAdded) return@addOnSuccessListener
                val missing = defaults.filterKeys { key -> !badgeDoc.contains(key) }
                if (missing.isNotEmpty()) {
                    badgeProgressDoc(uid).set(missing, SetOptions.merge())
                }
                BadgeProgressRepository.update(BadgeProgressFirestore.userBadgeProgressFromStateSnapshot(badgeDoc))
                bindRandomProfileBadges()
                binding.profileBadgesCard.visibility = View.VISIBLE
                (activity as? MainActivity)?.requestSeasonLeaderboardRewardGateIfPending()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                BadgeProgressRepository.update(UserBadgeProgress())
                bindRandomProfileBadges()
                binding.profileBadgesCard.visibility = View.VISIBLE
            }
    }
    
    private fun updateTotalTimeDisplay(totalSeconds: Long) {
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        
        val timeText = when {
            days > 0 -> "🔥 $days Gün $hours saat $minutes dakika"
            hours > 0 -> "🔥 $hours saat $minutes dakika"
            else -> "🔥 $minutes dakika"
        }
        binding.tvTotalTime.text = timeText
    }

    private fun bindRandomProfileBadges() {
        val slots = listOf(
            ProfileBadgeSlot(binding.badgeItemDart, binding.badgeDartBase, binding.badgeDartTop, binding.badgeDartValue),
            ProfileBadgeSlot(binding.badgeItemRocket, binding.badgeRocketBase, binding.badgeRocketTop, binding.badgeRocketValue),
            ProfileBadgeSlot(binding.badgeItemBowling, binding.badgeBowlingBase, binding.badgeBowlingTop, binding.badgeBowlingValue),
            ProfileBadgeSlot(binding.badgeItemGolf, binding.badgeGolfBase, binding.badgeGolfTop, binding.badgeGolfValue),
        )

        val badgesByType = BadgeProgressRepository.getStates().associateBy { it.kind }
        val orderedUnlocked = unlockedPriorityOrder.mapNotNull { type ->
            badgesByType[type]?.takeIf { it.unlocked }
        }
        val orderedLocked = lockedPriorityOrder.mapNotNull { type ->
            badgesByType[type]?.takeIf { !it.unlocked }
        }
        val pickedBadges = (orderedUnlocked + orderedLocked).take(slots.size)

        // Önce tüm slotları kapat; sonra soldan sağa 4 slotu sıralı doldur.
        slots.forEach { slot ->
            slot.container.visibility = View.GONE
        }

        pickedBadges.zip(slots).forEach { (badge, slot) ->
            slot.container.visibility = View.VISIBLE
            val config = getProfileBadgeRenderConfig(badge.kind)
            bindSingleProfileBadge(
                container = slot.container,
                base = slot.base,
                top = slot.top,
                valueText = slot.valueText,
                state = badge,
                baseAsset = config.baseAsset,
                topAsset = config.topAsset,
                topHoldFrame = config.topHoldFrame,
                topScale = config.topScale,
                topBackgroundRes = config.topBackgroundRes,
            )
        }
    }

    private fun getProfileBadgeRenderConfig(type: BadgeKind): ProfileBadgeRenderConfig {
        return when (type) {
            BadgeKind.CUP -> ProfileBadgeRenderConfig(
                baseAsset = null,
                topAsset = "cup_google_anim.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.KARATE -> ProfileBadgeRenderConfig(
                baseAsset = null,
                topAsset = "karate_anim.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.GOLD -> ProfileBadgeRenderConfig(
                baseAsset = null,
                topAsset = "gold_medal_anim.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.SILVER -> ProfileBadgeRenderConfig(
                baseAsset = null,
                topAsset = "silver_medal_anim.json.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.BRONZE -> ProfileBadgeRenderConfig(
                baseAsset = null,
                topAsset = "bronze_medal_anim.json.json",
                topHoldFrame = 0f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.DART -> ProfileBadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "dart_anim.json",
                topHoldFrame = 60f,
                topScale = 0.65f,
                topBackgroundRes = null,
            )

            BadgeKind.ROCKET -> ProfileBadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "rocket_badge_anim2.json",
                topHoldFrame = 60f,
                topScale = 1f,
                topBackgroundRes = null,
            )

            BadgeKind.BOWLING -> ProfileBadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "bowling_anim.json",
                topHoldFrame = 47f,
                topScale = 0.68f,
                topBackgroundRes = R.drawable.bg_badge_circle_frame,
            )

            BadgeKind.FISHING -> ProfileBadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "fishing_pole_anim.json",
                topHoldFrame = 24f,
                topScale = 0.65f,
                topBackgroundRes = null,
            )

            BadgeKind.GOLF -> ProfileBadgeRenderConfig(
                baseAsset = "daily_tasks_complite_badge2.json",
                topAsset = "golf_anim.json",
                topHoldFrame = 30f,
                topScale = 0.60f,
                topBackgroundRes = null,
            )
        }
    }

    private fun bindSingleProfileBadge(
        container: View,
        base: LottieAnimationView,
        top: LottieAnimationView,
        valueText: TextView,
        state: BadgeState,
        baseAsset: String?,
        topAsset: String,
        topHoldFrame: Float,
        topScale: Float,
        topBackgroundRes: Int?,
    ) {
        if (baseAsset == null) {
            base.cancelAnimation()
            base.visibility = View.INVISIBLE
        } else {
            base.visibility = View.VISIBLE
            base.setAnimation(baseAsset)
            base.repeatCount = 0
            base.speed = 1f
            base.addLottieOnCompositionLoadedListener {
                base.progress = 1f
                base.pauseAnimation()
            }
        }

        top.setAnimation(topAsset)
        top.repeatCount = 0
        top.speed = 1f
        top.scaleX = topScale
        top.scaleY = topScale
        top.translationY = -1f * resources.displayMetrics.density
        if (topBackgroundRes != null) {
            top.setBackgroundResource(topBackgroundRes)
        } else {
            top.background = null
        }
        top.addLottieOnCompositionLoadedListener { composition ->
            val progress = frameToProgress(topHoldFrame, composition.startFrame, composition.endFrame)
            top.progress = progress
            top.pauseAnimation()
        }

        if (state.unlocked) {
            container.alpha = 1f
            clearLottieFlatGrayFilter(base)
            clearLottieFlatGrayFilter(top)
            applyUnlockedLevelToneToBase(base, state.levelTone)
            (valueText as? BadgeRewardTextView)?.setStrokeColorInt(unlockedValueStrokeColor)
            valueText.setTextColor(Color.WHITE)
            if (state.showValue) {
                valueText.visibility = View.VISIBLE
                valueText.text = (state.value ?: 0).toString()
            } else {
                valueText.visibility = View.GONE
            }
        } else {
            container.alpha = 1f
            if (base.visibility == View.VISIBLE) {
                // Çift katmanlı rozetlerde base koyu, top açık gri.
                applyLottieFlatGrayFilter(base, lockedBaseColor)
                applyLottieFlatGrayFilter(top, lockedTopColor)
            } else {
                applyLottieFlatGrayFilter(top, lockedTopColor)
            }
            if (state.showValue) {
                valueText.visibility = View.VISIBLE
                valueText.text = (state.value ?: 0).toString()
                (valueText as? BadgeRewardTextView)?.setStrokeColorInt(lockedTopColor)
                valueText.setTextColor(lockedBaseColor)
            } else {
                valueText.visibility = View.GONE
            }
        }
    }

    private fun applyUnlockedLevelToneToBase(base: LottieAnimationView, tone: BadgeLevelTone?) {
        if (base.visibility != View.VISIBLE) return
        when (tone) {
            BadgeLevelTone.BRONZE -> applyLottieToneFilter(base, bronzeToneColor)
            BadgeLevelTone.SILVER -> applyLottieToneFilter(base, silverToneColor)
            BadgeLevelTone.RED -> applyLottieToneFilter(base, redToneColor)
            BadgeLevelTone.PURPLE -> applyLottieToneFilterStrong(base, purpleToneColor)
            BadgeLevelTone.ORIGINAL, null -> clearLottieFlatGrayFilter(base)
        }
    }

    private fun applyLottieToneFilter(view: LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)),
        )
        view.invalidate()
    }

    private fun applyLottieToneFilterStrong(view: LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.OVERLAY)),
        )
        view.invalidate()
    }

    private fun applyLottieFlatGrayFilter(view: LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)),
        )
        view.invalidate()
    }

    private fun clearLottieFlatGrayFilter(view: LottieAnimationView) {
        view.clearValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
        )
        view.invalidate()
    }

    private fun frameToProgress(frame: Float, startFrame: Float, endFrame: Float): Float {
        if (endFrame <= startFrame) return 0f
        return ((frame - startFrame) / (endFrame - startFrame)).coerceIn(0f, 1f)
    }

    private fun setupClickListeners() {
        binding.btnAccountSettings.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                )
                .replace(R.id.fragmentContainerID, AccountSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.profileBadgesCard.setOnClickListener {
            openBadgeDetailFragment()
        }

        binding.btnProfileBadgesArrow.setOnClickListener {
            openBadgeDetailFragment()
        }

        // Üyelik Durumu - Abonelik sayfasına git
        binding.tvMembershipStatus.setOnClickListener {
            val intent = Intent(requireContext(), SubscriptionActivity::class.java)
            subscriptionLauncher.launch(intent)
        }

        // E-posta Doğrula
        binding.btnVerifyEmail.setOnClickListener {
            verifyEmail()
        }
    }

    private fun openBadgeDetailFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .replace(R.id.fragmentContainerID, BadgeDetailFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun verifyEmail() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // E-posta doğrulama linki gönder
        currentUser.sendEmailVerification()
            .addOnSuccessListener {
                Toast.makeText(context, "Doğrulama e-postası gönderildi. E-postanızı doğruladıktan sonra bu ekrana geri dönün.", Toast.LENGTH_LONG).show()
                
                // E-posta gönderildikten sonra, kullanıcı bilgilerini periyodik olarak kontrol et
                // (Kullanıcı e-postayı doğruladıktan sonra uygulamaya geri döndüğünde otomatik güncellenir)
                startEmailVerificationCheck()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "E-posta gönderilemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun startEmailVerificationCheck() {
        // Her 3 saniyede bir kullanıcı bilgilerini kontrol et (e-posta doğrulandı mı?)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var checkCount = 0
        val maxChecks = 20 // 20 kez kontrol et (toplam 60 saniye)
        
        val checkRunnable = object : Runnable {
            override fun run() {
                if (checkCount >= maxChecks) {
                    // 60 saniye sonra kontrolü durdur
                    return
                }
                
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Kullanıcı bilgilerini yeniden yükle
                    currentUser.reload()
                        .addOnSuccessListener {
                            // E-posta doğrulandı mı kontrol et
                            if (currentUser.isEmailVerified) {
                                // E-posta doğrulanmış - Firestore'u güncelle ve UI'ı yenile
                                firestore.collection("users").document(currentUser.uid)
                                    .update("verified", true)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "E-posta başarıyla doğrulandı!", Toast.LENGTH_SHORT).show()
                                        loadUserData() // UI'ı yenile
                                    }
                                // Kontrolü durdur
                                return@addOnSuccessListener
                            }
                            
                            // Henüz doğrulanmamış - tekrar kontrol et
                            checkCount++
                            handler.postDelayed(this, 3000) // 3 saniye sonra tekrar kontrol et
                        }
                        .addOnFailureListener {
                            // Hata oluştu - kontrolü durdur
                        }
                }
            }
        }
        
        // İlk kontrolü 3 saniye sonra yap
        handler.postDelayed(checkRunnable, 3000)
    }
    
    private fun showEditProfileDialog() {
        val currentUser = auth.currentUser
        val currentName = currentUser?.displayName ?: ""
        
        val input = android.widget.EditText(requireContext())
        input.setText(currentName)
        input.hint = "Kullanıcı Adı"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Profili Düzenle")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUserName(newName)
                } else {
                    Toast.makeText(context, "Kullanıcı adı boş olamaz", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun updateUserName(newName: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) return
        
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()
        
        currentUser.updateProfile(profileUpdates)
            .addOnSuccessListener {
                binding.tvUserName.text = newName
                
                // Firestore'da da güncelle
                firestore.collection("users").document(currentUser.uid)
                    .update("name", newName)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Kullanıcı adı güncellendi", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Kullanıcı adı güncellenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showChangePasswordDialog() {
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Yeni Şifre"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Şifre Değiştir")
            .setMessage("Yeni şifrenizi girin (en az 6 karakter)")
            .setView(input)
            .setPositiveButton("Değiştir") { _, _ ->
                val newPassword = input.text.toString().trim()
                if (newPassword.length >= 6) {
                    changePassword(newPassword)
                } else {
                    Toast.makeText(context, "Şifre en az 6 karakter olmalı", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun changePassword(newPassword: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Google Sign-In ile giriş yapıldıysa şifre değiştirilemez
        val providers = currentUser.providerData
        val isGoogleUser = providers.any { it.providerId == "google.com" }
        
        if (isGoogleUser) {
            Toast.makeText(context, "Google hesabı ile giriş yaptınız. Şifre değiştirilemez.", Toast.LENGTH_LONG).show()
            return
        }
        
        currentUser.updatePassword(newPassword)
            .addOnSuccessListener {
                Toast.makeText(context, "Şifre başarıyla değiştirildi", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Şifre değiştirilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Çıkış yapmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                logout()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }
    
    private fun logout() {
        // Önce bu cihazdaki FCM token'ını temizle
        MyFirebaseMessagingService.clearCurrentTokenFromFirestore()
        authManager.logout()
        // Çıkış yapıldıktan sonra LoginStartActivity'den (öğrenci/öğretmen seçim ekranı) başlat
        val intent = Intent(requireContext(), LoginStartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
    
    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hesabı Sil")
            .setMessage("Hesabınızı silmek istediğinize emin misiniz? Bu işlem geri alınamaz!")
            .setPositiveButton("Evet, Sil") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun calculateCompletedLessons(): Int {
        // GlobalLessonData'dan stepIsFinish değeri true olan dersleri say
        // Her bir LessonItem'ı kontrol et, stepIsFinish == true ise sayacı 1 artır
        // 0'dan başlayarak her biten ders için 1 artır
        return try {
            var completedCount = 0
            
            // GlobalLessonData'yı initialize et (eğer initialize edilmemişse)
            try {
                GlobalLessonData.initialize(requireContext(), GlobalLessonData.globalPartId) {
                    // Count is calculated immediately below; async init is only to avoid
                    // default-overwrite risk and keep future reads in sync.
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "GlobalLessonData initialize edilemedi", e)
            }
            
            // Tüm lessonItems'ı kontrol et (createLessonItems içerisindeki tüm item'ler)
            for (item in GlobalLessonData.lessonItems) {
                // stepIsFinish değeri true ise tamamlanmış say
                if (item.stepIsFinish == true) {
                    completedCount++
                }
            }
            
            Log.d("ProfileFragment", "Tamamlanan ders sayısı: $completedCount (toplam item: ${GlobalLessonData.lessonItems.size}, stepIsFinish=true olanlar)")
            completedCount
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Tamamlanan ders sayısı hesaplanamadı", e)
            0
        }
    }
    
    private fun deleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Kullanıcının hangi provider ile giriş yaptığını kontrol et
        val providers = currentUser.providerData
        val isGoogleUser = providers.any { it.providerId == "google.com" }
        
        if (isGoogleUser) {
            // Google Sign-In ile giriş yapıldıysa Google ile yeniden kimlik doğrulama yap
            reauthenticateWithGoogle()
            } else {
            // Email/Password ile giriş yapıldıysa şifre ile yeniden kimlik doğrulama yap
            showPasswordReauthDialog()
        }
    }
    
    private fun reauthenticateWithGoogle() {
        // Google Sign-In ile yeniden kimlik doğrulama
        val webClientId = requireContext().getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        
        // Önce mevcut oturumu kapat
        googleSignInClient.signOut().addOnCompleteListener {
            // Google Sign-In intent'ini başlat
            val signInIntent = googleSignInClient.signInIntent
            googleReauthLauncher.launch(signInIntent)
        }
    }
    
    private fun showPasswordReauthDialog() {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Şifrenizi girin"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Hesabı Sil")
            .setMessage("Hesabınızı silmek için şifrenizi girin")
            .setView(input)
            .setPositiveButton("Onayla") { _, _ ->
                val password = input.text.toString().trim()
                if (password.isNotEmpty()) {
                    reauthenticateWithPassword(password)
                } else {
                    Toast.makeText(context, "Şifre boş olamaz", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun reauthenticateWithPassword(password: String) {
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email == null) {
            Toast.makeText(context, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Email/Password ile yeniden kimlik doğrulama
        val credential = EmailAuthProvider.getCredential(currentUser.email!!, password)
        
        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // Yeniden kimlik doğrulama başarılı - hesabı sil
                performAccountDeletion()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Kimlik doğrulama başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun reauthenticateWithGoogleAccount(account: GoogleSignInAccount) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(account.idToken, null)
        
        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // Yeniden kimlik doğrulama başarılı - hesabı sil
                performAccountDeletion()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Kimlik doğrulama başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun performAccountDeletion() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Firestore'dan kullanıcı verilerini sil
        firestore.collection("users").document(currentUser.uid)
            .delete()
            .addOnSuccessListener {
                // Firebase Auth'dan kullanıcıyı sil
                currentUser.delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Hesap başarıyla silindi", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Hesap silinemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Kullanıcı verileri silinemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
}
