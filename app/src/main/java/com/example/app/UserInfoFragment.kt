package com.example.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentUserInfoBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class UserInfoFragment : Fragment() {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!

    private var forceTeacher: Boolean = false
    private var forceStudent: Boolean = false
    private var prefillEmail: String? = null
    private var googleSignIn: Boolean = false
    private var googleEmail: String? = null
    private var googleName: String? = null

    /**
     * Kayıt soruları tek bir akışta.
     *
     * Seri hedefi ve meydan okuma eskiden ayrı bir fragment'ti ve kendi ilerleme çubuğuyla
     * ikinci bir akış gibi görünüyordu. Sorular tek yerde sorulsun diye buraya alındı.
     *
     * MEET ve BRIEF tanışma ekranları ([isIntro]): maskot kendini tanıtıyor ve kaç soru
     * sorulacağını söylüyor; ilerleme çubuğu bu ikisinde gizli, sayılmıyor. ROADMAP de soru
     * değil: seçilen günlük hedefle nereye varılacağını gösteren ilerleme yolu. REMINDER son
     * soru: hatırlatma saati ve bildirim izni.
     * Sıra önemli, ilerleme çubuğu [progressFor] sıradan hesaplanıyor.
     *
     * @param isIntro Tanışma ekranı (yalnızca öğrenci kaydında).
     * @param asks Kullanıcıya bir şey soruyor; tanışmadaki "N kısa sorum var" sayısı bundan.
     */
    private enum class Step(val isIntro: Boolean = false, val asks: Boolean = false) {
        MEET(isIntro = true),
        BRIEF(isIntro = true),
        AGE(asks = true),
        SOURCE(asks = true),
        GOAL(asks = true),
        ROADMAP,
        CHALLENGE_INTRO,
        CHALLENGE(asks = true),
        REMINDER(asks = true),
    }

    /** Hatırlatma sorusunda seçili saat; ekran açılınca kayıtlı (yoksa 19:00) seçili geliyor. */
    private var reminderHour = StreakRepository.DEFAULT_REMINDER_HOUR

    /** Hatırlatma cevabı verildi, kayıt ekranına geçiliyor: ikinci dokunuş ikinci kez geçirmesin. */
    private var reminderAnswered = false

    /**
     * "Evet, hatırlat"tan sonra Android 13+ bildirim izni penceresi. Sonuç ne olursa olsun
     * kayda devam ediliyor: izin vermemek kayıt olmaya engel değil.
     */
    private val notificationPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        logReminder(
            AnalyticsLogger.REMINDER_CHOICE_YES,
            if (granted) AnalyticsLogger.NOTIF_PERMISSION_GRANTED else AnalyticsLogger.NOTIF_PERMISSION_DENIED,
        )
        if (_binding != null) finishUserInfo()
    }

    /** Bu akışta gösterilen adımlar, sırayla (öğretmene yalnızca yaş ve kaynak). */
    private val flowSteps: List<Step>
        get() = if (streakStepsEnabled) Step.entries else listOf(Step.AGE, Step.SOURCE)

    private var currentStep = Step.AGE
    private var selectedSource: String? = null
    private var validatedBirthYear: Int? = null

    /** Seri adımları bu akışta sorulacak mı; akışın başında bir kez belirleniyor. */
    private var streakStepsEnabled = false

    /**
     * Ölçüme en son bildirilen seri adımı.
     *
     * Adım ekranları her seçenek dokunuşunda yeniden çiziliyor; olay çizim fonksiyonunda
     * koşulsuz gönderilseydi huni, kaç kişinin o adımı gördüğünü değil kaç kez seçenek
     * değiştirildiğini sayardı.
     */
    private var loggedStreakStep: Step? = null

    /** Adım geçişi sürüyor; bu sırada ileri/geri girdileri yok sayılıyor. */
    private var animating = false
    /** İlerleme çubuğunun o anki yüzdesi (adımlar arası akarken ara değerler de). */
    private var progressPercent = 0f
    private var progressAnimator: ValueAnimator? = null
    /** Balonda şu an gösterilen (yazılan ya da yazılmakta olan) soru ve daktilo animasyonu. */
    private var shownQuestion: String? = null
    private var typeAnimator: ValueAnimator? = null
    private var goalMinutes = 0
    private var challengeDays = 0
    /**
     * "Bizi nereden duydun?" seçenekleri, sırayla: kaydedilen değer → ekranda görünen ad.
     *
     * Değer istatistik anahtarı (users.acquisitionSource + Analytics): değişirse eski kayıtlarla
     * yeni kayıtlar ayrı sayılır, bu yüzden "Youtube" değer olarak kaldı, yalnızca görünen adı
     * "YouTube" oldu. "Okul/Öğretmen" sonradan eklendi: abaküs çoğu çocuğa okuldan geliyor ve
     * bu cevap önceden "Arkadaş/Aile" ya da "Diğer"e karışıyordu. Sıra bilerek sabit.
     */
    private val sources = listOf(
        "Facebook" to "Facebook",
        "Instagram" to "Instagram",
        "Youtube" to "YouTube",
        "Google Araması" to "Google Araması",
        "Arkadaş/Aile" to "Arkadaş/Aile",
        "Okul/Öğretmen" to "Okul/Öğretmen",
        "TikTok" to "TikTok",
        "Uygulama Mağazası" to "Uygulama Mağazası",
        "Diğer" to "Diğer",
    )

    companion object {
        /** Adım geçişi ve ilerleme çubuğu animasyon süresi. */
        private const val STEP_ANIM_MS = 220L

        /** Balondaki sorunun daktilo hızı (harf başına). */
        private const val TYPE_MS_PER_CHAR = 22L

        /** Tanışmada tavşanın el sallama / alkış arasında beklediği süre. */
        private const val INTRO_PAUSE_MS = 2_000L
        /** Tanışma balonunun solması ve tavşanın soru başlığına kayması. */
        private const val INTRO_FADE_MS = 150L
        private const val MASCOT_MOVE_MS = 450L
        /** Hızlı çıkıp yumuşak oturan eğri (Material "standard"). */
        private val MOVE_EASING = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)

        private const val ARG_FORCE_TEACHER = "arg_force_teacher"
        private const val ARG_FORCE_STUDENT = "arg_force_student"
        private const val ARG_PREFILL_EMAIL = "arg_prefill_email"
        private const val ARG_GOOGLE_SIGN_IN = "arg_google_sign_in"
        private const val ARG_GOOGLE_EMAIL = "arg_google_email"
        private const val ARG_GOOGLE_NAME = "arg_google_name"

        fun newInstance(
            forceTeacher: Boolean = false,
            forceStudent: Boolean = false,
            prefillEmail: String? = null,
            googleSignIn: Boolean = false,
            googleEmail: String? = null,
            googleName: String? = null
        ): UserInfoFragment {
            return UserInfoFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FORCE_TEACHER, forceTeacher)
                    putBoolean(ARG_FORCE_STUDENT, forceStudent)
                    prefillEmail?.let { putString(ARG_PREFILL_EMAIL, it) }
                    putBoolean(ARG_GOOGLE_SIGN_IN, googleSignIn)
                    googleEmail?.let { putString(ARG_GOOGLE_EMAIL, it) }
                    googleName?.let { putString(ARG_GOOGLE_NAME, it) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forceTeacher = arguments?.getBoolean(ARG_FORCE_TEACHER, false) ?: false
        forceStudent = arguments?.getBoolean(ARG_FORCE_STUDENT, false) ?: false
        prefillEmail = arguments?.getString(ARG_PREFILL_EMAIL)
        googleSignIn = arguments?.getBoolean(ARG_GOOGLE_SIGN_IN, false) ?: false
        googleEmail = arguments?.getString(ARG_GOOGLE_EMAIL)
        googleName = arguments?.getString(ARG_GOOGLE_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Kayıt ekranından geri dönüldüyse hatırlatma sorusu yeniden cevaplanabilsin.
        reminderAnswered = false
    }

    /** Kayıt hunisinde bu ekranın rolü; öğretmen akışı ayrı sayılmalı. */
    private val signupRole: String
        get() = if (forceTeacher) AnalyticsLogger.SIGNUP_ROLE_TEACHER
                else AnalyticsLogger.SIGNUP_ROLE_STUDENT

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Seri adımlarının sorulup sorulmayacağı EN BAŞTA belirleniyor, çünkü ilerleme
        // çubuğu toplam adım sayısından hesaplanıyor. Sonradan karar verilseydi çubuk
        // 50 → 100 → 75 diye geri giderdi.
        //
        // Öğretmene sorulmuyor: seri bir öğrenme alışkanlığı ölçüsü, öğretmen uygulamayı
        // ders çalışmak için kullanmıyor.
        //
        // Öğrenciye HER SEFERİNDE soruluyor, "kurulum yapıldı" işaretine bakılmadan: bu ekran
        // yalnızca yeni hesap açarken açılıyor; soruların yeniden sorulması her zaman doğru.
        //
        // Cihazdaki seri verisine burada DOKUNULMUYOR (eskiden önceki hesabınki siliniyordu).
        // Cevaplar ayrı bekliyor ve giriş yapılınca, hesap yeniyse uygulanıyor
        // (StreakRepository.applyPendingSignup); kayda girip vazgeçen ya da mevcut hesabına
        // giren çocuk bugünkü dakikalarını ve gönderilmemiş günlerini kaybetmiyor. Seçimler
        // bu yüzden cihazdaki değerden değil varsayılandan başlıyor.
        streakStepsEnabled = !forceTeacher
        if (streakStepsEnabled) goalMinutes = StreakRepository.GOAL_OPTIONS.first()
        if (streakStepsEnabled) reminderHour = StreakRepository.DEFAULT_REMINDER_HOUR

        // Huni: öğrenci önce tanışmayı görüyor, yaş adımı oraya varınca sayılıyor (leaveIntro).
        AnalyticsLogger.logSignupStep(
            if (streakStepsEnabled) AnalyticsLogger.SIGNUP_INTRO else AnalyticsLogger.SIGNUP_AGE,
            signupRole,
        )

        // Soru başlığı: havuç kalemle yazan maskot + konuşma balonu (metni bindStep'te).
        val density = resources.displayMetrics.density
        fun bubble(side: SpeechBubbleDrawable.TailSide) = SpeechBubbleDrawable(
            fillColor = ContextCompat.getColor(requireContext(), R.color.background_color),
            strokeColor = ContextCompat.getColor(requireContext(), R.color.missions_track),
            strokeWidth = 2f * density,
            cornerRadius = 16f * density,
            tailLength = if (side == SpeechBubbleDrawable.TailSide.LEFT) 12f * density else 16f * density,
            tailBase = 16f * density,
            tailSide = side,
        )
        binding.questionBubble.background = bubble(SpeechBubbleDrawable.TailSide.LEFT)
        // Tanışmada balon tavşanın üstünde, kuyruğu aşağıda ona bakıyor.
        binding.introBubble.background = bubble(SpeechBubbleDrawable.TailSide.BOTTOM)
        // Cevap beklenirken defter ve havuç elde bekliyor; "Devam Et"te yazıyor (aşağıda).
        binding.questionMascot.play(BunnyMascotView.Emote.NOTEBOOK_HOLD)

        // Düğme ilk adımın bindStep'inde açılıyor (tanışmada açık, yaş sorusunda kapalı).
        updateContinueButton(enabled = false)
        showStep(flowSteps.first(), animate = false)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackStep()
                }
            },
        )

        if (!prefillEmail.isNullOrEmpty()) {
            Snackbar.make(
                binding.root,
                "Bu e-posta adresi kayıtlı değil. Kayıt sayfasına yönlendirildin.",
                Snackbar.LENGTH_LONG
            ).show()
        }

        binding.etAge.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (currentStep == Step.AGE) {
                    val age = s?.toString()?.trim()?.toIntOrNull()
                    val valid = age != null && age in 1..120
                    updateContinueButton(enabled = valid)
                }
            }
        })

        binding.btnContinue.setOnClickListener {
            if (animating) return@setOnClickListener
            // Bilgi sorularında devam edilirken maskot cevabı deftere yazıyor, sonra defterle
            // beklemeye dönüyor. Geçersiz cevapta (aşağıdaki erken dönüşler) yazmasın diye
            // her dalda geçerlilikten SONRA çağrılıyor.
            fun writeAnswer() = binding.questionMascot.play(
                BunnyMascotView.Emote.NOTEBOOK_WRITE,
                then = BunnyMascotView.Emote.NOTEBOOK_HOLD,
            )
            if (currentStep == Step.MEET) {
                showStep(Step.BRIEF)

            } else if (currentStep == Step.BRIEF) {
                AnalyticsLogger.logSignupStep(AnalyticsLogger.SIGNUP_AGE, signupRole)
                showStep(Step.AGE)

            } else if (currentStep == Step.AGE) {
                val ageText = binding.etAge.text?.toString()?.trim() ?: ""
                val age = ageText.toIntOrNull()

                if (age == null || age !in 1..120) {
                    Toast.makeText(requireContext(), "Lütfen geçerli bir yaş girin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                validatedBirthYear = currentYear - age
                
                hideKeyboard()

                AnalyticsLogger.logSignupStep(AnalyticsLogger.SIGNUP_SOURCE, signupRole)

                writeAnswer()
                showStep(Step.SOURCE)

            } else if (currentStep == Step.SOURCE) {
                if (validatedBirthYear == null || selectedSource == null) return@setOnClickListener
                hideKeyboard()
                writeAnswer()
                if (streakStepsEnabled) showStep(Step.GOAL) else finishUserInfo()

            } else if (currentStep == Step.GOAL) {
                writeAnswer()
                // Kaydedilmiyor: hedef, meydan okuma ve saat kayıt sonunda birlikte bekleyen
                // cevaplara yazılıyor (answerReminder).
                AnalyticsLogger.logStreakGoalSet(
                    goalMinutes,
                    AnalyticsLogger.STREAK_SOURCE_ONBOARDING,
                )
                showStep(Step.ROADMAP)

            } else if (currentStep == Step.ROADMAP) {
                showStep(Step.CHALLENGE_INTRO)

            } else if (currentStep == Step.CHALLENGE_INTRO) {
                showStep(Step.CHALLENGE)

            } else if (currentStep == Step.CHALLENGE) {
                AnalyticsLogger.logStreakChallengeSet(
                    challengeDays,
                    AnalyticsLogger.STREAK_SOURCE_ONBOARDING,
                )
                showStep(Step.REMINDER)

            } else if (currentStep == Step.REMINDER) {
                answerReminder(optIn = true)
            }
        }

        // Hatırlatma sorusunun ikincil seçeneği: saat yine kaydediliyor, izin sorulmuyor.
        binding.btnLater.setOnClickListener {
            if (animating || currentStep != Step.REMINDER) return@setOnClickListener
            answerReminder(optIn = false)
        }
    }

    /**
     * Hatırlatma sorusunun cevabı.
     *
     * İki seçenekte de saat kaydediliyor ve sunucuya gidiyor (bkz. StreakRepository): bildirimi
     * göstermek ya da göstermemek telefonun iznine bağlı, saat ise seri ekranından sonradan
     * da açılabilsin diye hazır duruyor. Ana ekranın ilk açılıştaki bağlamsız izin isteği iki
     * durumda da kapatılıyor: "evet" diyene izin burada soruldu, "şimdi değil" diyene aynı
     * soruyu birkaç dakika sonra başka kılıkta sormak cevabını yok saymak olurdu.
     *
     * @param optIn "Evet, hatırlat": Android 13+'ta izin penceresi açılıyor, sonucu ne olursa
     *   olsun kayda devam ediliyor.
     */
    private fun answerReminder(optIn: Boolean) {
        if (reminderAnswered) return
        reminderAnswered = true
        val context = requireContext()
        // Seri cevaplarının hepsi burada, kayda geçmeden: giriş yapılınca hesap yeniyse
        // uygulanıyor, mevcut bir hesaba girildiyse atılıyor (StreakRepository.applyPendingSignup).
        StreakRepository.savePendingSignup(context, goalMinutes, challengeDays, reminderHour)
        MainActivity.markNotificationPermissionPrompted(context)

        if (!optIn) {
            logReminder(AnalyticsLogger.REMINDER_CHOICE_LATER, AnalyticsLogger.NOTIF_PERMISSION_NOT_ASKED)
            finishUserInfo()
            return
        }
        val needsPermission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            // Sonuç notificationPermission'da: ölçüm ve kayda devam orada.
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            logReminder(AnalyticsLogger.REMINDER_CHOICE_YES, AnalyticsLogger.NOTIF_PERMISSION_NOT_NEEDED)
            finishUserInfo()
        }
    }

    private fun logReminder(choice: String, permission: String) {
        AnalyticsLogger.logStreakReminderSet(
            reminderHour,
            AnalyticsLogger.STREAK_SOURCE_ONBOARDING,
            choice,
            permission,
        )
    }

    /**
     * Adımı değiştirir: görünürlük, ilerleme çubuğu ve devam butonu tek yerden.
     *
     * Eskiden bu üç iş, ileri ve geri geçişlerde ayrı ayrı yazılıydı; ilerleme çubuğu
     * eklenince aynı satırların dördüncü kopyası gerekecekti.
     */
    /**
     * Geri tuşu: bir önceki adıma döner, ilk adımdaysa ekranı kapatır.
     *
     * ## Neden hem fragment hem activity çağırıyor
     * Fragment kendi `OnBackPressedCallback`'ini kuruyor ama [LoginStartActivity] da geri
     * tuşunu yutan bir dinleyici kuruyor. Sıralama kurallarına göre fragment'inki
     * kazanmalıydı; sahada ilk adım dışındaki adımlarda geri tuşu çalışmadı, yani
     * kazanmıyor. Sebebini statik okumayla bulamadığım için davranış artık sıralamaya
     * BAĞIMLI DEĞİL: activity'nin dinleyicisi de geri tuşunu yutmadan önce buraya soruyor.
     * Hangi dinleyici önce çalışırsa çalışsın sonuç aynı.
     *
     * @return true → olay burada tüketildi, çağıran başka bir şey yapmamalı.
     */
    fun onBackStep(): Boolean {
        if (!isAdded || _binding == null) return false
        if (animating) return true

        val steps = flowSteps
        val previous = steps.getOrNull(steps.indexOf(currentStep) - 1)
        if (previous != null) {
            showStep(previous, forward = false)
            return true
        }
        // İlk adımdayız: fragment'in kendisi kapanıyor.
        parentFragmentManager.popBackStack()
        return true
    }

    /**
     * Adımı değiştirir: kaydırma animasyonu, ilerleme çubuğu, içerik ve devam butonu
     * tek yerden.
     *
     * @param forward İleri gidiliyorsa mevcut soru sola, yeni soru sağdan kayar; geri
     *   gidiliyorsa ters yön. Yön olmadan geçişler "nereye gittim" hissini kaybediyor.
     * @param animate İlk çizimde false: ekran açılırken kayma olmaz.
     */
    private fun showStep(step: Step, forward: Boolean = true, animate: Boolean = true) {
        val previous = currentStep
        val from = containerFor(previous)
        val to = containerFor(step)
        val sameStep = step == previous
        currentStep = step

        setProgress(progressFor(step), animate = animate)

        // İki tanışma ekranı arasında kayma yok: tavşan yerinde, yalnızca balondaki yazı ve
        // hâli değişiyor (karşılıklı konuşma gibi).
        if (!animate || sameStep || (previous.isIntro && step.isIntro)) {
            if (from !== to) from.visibility = View.GONE
            bindStep(step)
            to.visibility = View.VISIBLE
            return
        }
        when {
            previous.isIntro -> leaveIntro(to, step)
            step.isIntro -> returnToIntro(from, step)
            else -> slideTo(from, to, forward) { bindStep(step) }
        }
    }

    /**
     * Tanışmadan sorulara: balon kayboluyor, tavşan küçülerek soru başlığındaki maskotun yerine
     * kayıyor; varınca o maskot görünüyor ve defterini çıkarıyor, soru balona yazılıyor.
     *
     * Kayan, tanışmadaki tavşanın kendisi; başlıktakiyle aynı kare boyutta çizildiği için
     * ölçek oranı birebir oturuyor. Yer değiştirme anında ikisi de bekleme duruşunda (tanışmadaki
     * kayarken [BunnyMascotView.stop] ile oraya dönüyor), devir fark edilmiyor. Başlık tanışma
     * boyunca INVISIBLE tutuluyor, yani hedef konumu ölçülü.
     */
    private fun leaveIntro(to: View, step: Step) {
        animating = true
        val mascot = binding.introMascot
        val target = binding.questionMascot
        mascot.stop()
        finishTyping()

        val from = IntArray(2).also { mascot.getLocationInWindow(it) }
        val dest = IntArray(2).also { target.getLocationInWindow(it) }
        mascot.pivotX = 0f
        mascot.pivotY = 0f
        binding.introBubble.animate().alpha(0f).setDuration(INTRO_FADE_MS).start()
        binding.userInfoProgress.animate().alpha(1f).setDuration(MASCOT_MOVE_MS).start()
        mascot.animate()
            .setStartDelay(0L)
            .translationX((dest[0] - from[0]).toFloat())
            .translationY((dest[1] - from[1]).toFloat())
            .scaleX(target.width.toFloat() / mascot.width)
            .scaleY(target.height.toFloat() / mascot.height)
            .setDuration(MASCOT_MOVE_MS)
            .setInterpolator(MOVE_EASING)
            .withEndAction {
                if (_binding == null) return@withEndAction
                // Kap GONE değil INVISIBLE: geri dönüşte tavşanın orta konumu ölçülü kalsın.
                binding.introContainer.visibility = View.INVISIBLE
                resetIntroViews()
                binding.questionMascot.play(BunnyMascotView.Emote.NOTEBOOK_HOLD)
                bindStep(step)
                to.alpha = 0f
                to.visibility = View.VISIBLE
                to.animate().alpha(1f).setDuration(STEP_ANIM_MS)
                    .withEndAction { animating = false }
                    .start()
            }
            .start()
    }

    /**
     * Sorulardan tanışmaya (yaş sorusunda geri): soru başlığı solarken tanışmadaki tavşan onun
     * yerinde belirip ortaya kayıyor, varınca balon yeniden konuşuyor. [leaveIntro]'nun tersi.
     */
    private fun returnToIntro(from: View, step: Step) {
        animating = true
        hideKeyboard()
        finishTyping()
        val mascot = binding.introMascot
        val header = binding.questionMascot
        resetIntroViews()
        binding.introContainer.visibility = View.VISIBLE
        binding.introBubble.alpha = 0f

        val rest = IntArray(2).also { mascot.getLocationInWindow(it) }
        val start = IntArray(2).also { header.getLocationInWindow(it) }
        mascot.pivotX = 0f
        mascot.pivotY = 0f
        mascot.translationX = (start[0] - rest[0]).toFloat()
        mascot.translationY = (start[1] - rest[1]).toFloat()
        mascot.scaleX = header.width.toFloat() / mascot.width
        mascot.scaleY = header.height.toFloat() / mascot.height
        mascot.alpha = 0f

        from.animate().alpha(0f).setDuration(INTRO_FADE_MS).withEndAction {
            from.visibility = View.GONE
            from.alpha = 1f
        }.start()
        binding.questionHeader.animate().alpha(0f).setDuration(INTRO_FADE_MS).withEndAction {
            binding.questionHeader.visibility = View.INVISIBLE
            binding.questionHeader.alpha = 1f
        }.start()
        binding.userInfoProgress.animate().alpha(0f).setDuration(MASCOT_MOVE_MS).start()
        mascot.animate().alpha(1f).setDuration(INTRO_FADE_MS).start()
        mascot.animate()
            .translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
            .setStartDelay(INTRO_FADE_MS)
            .setDuration(MASCOT_MOVE_MS)
            .setInterpolator(MOVE_EASING)
            .withEndAction {
                if (_binding == null) return@withEndAction
                mascot.animate().setStartDelay(0L)
                bindStep(step)
                binding.introBubble.animate().alpha(1f).setDuration(INTRO_FADE_MS)
                    .withEndAction { animating = false }
                    .start()
            }
            .start()
    }

    /** Tanışma görünümlerini dinlenme hâline getirir (kayma/solma kalıntısı kalmasın). */
    private fun resetIntroViews() {
        val mascot = binding.introMascot
        mascot.animate().cancel()
        mascot.translationX = 0f
        mascot.translationY = 0f
        mascot.scaleX = 1f
        mascot.scaleY = 1f
        mascot.alpha = 1f
        binding.introBubble.animate().cancel()
        binding.introBubble.alpha = 1f
    }

    private fun containerFor(step: Step): View = when (step) {
        Step.MEET, Step.BRIEF -> binding.introContainer
        Step.AGE -> binding.ageContainer
        Step.SOURCE -> binding.sourceContainer
        Step.ROADMAP -> binding.roadmapContainer
        Step.CHALLENGE_INTRO -> binding.challengeIntroContainer
        // Hedef ve meydan okuma AYNI kabı kullanıyor; geçişte kap önce çıkıp sonra yeni
        // içerikle geri giriyor (bkz. slideTo).
        Step.GOAL, Step.CHALLENGE, Step.REMINDER -> binding.streakContainer
    }

    /**
     * Yüzde adım SAYISINDAN hesaplanıyor: öğretmen akışında seri adımları yok ve sabit
     * yüzdeler orada yanlış bir ilerleme gösterirdi. Tanışma ekranları sayılmıyor (çubuk
     * orada gizli); ilk soru ilk dilimi dolduruyor.
     */
    private fun progressFor(step: Step): Int {
        if (step.isIntro) return 0
        val counted = flowSteps.filter { !it.isIntro }
        return (counted.indexOf(step) + 1) * 100 / counted.size
    }

    /**
     * Çubuk zıplamasın: mevcut değerden hedefe doğru akar, geri giderken de öyle.
     *
     * İlk çizimde animasyon yok: ekran açılır açılmaz akan bir çubuk görünmesin.
     */
    private fun setProgress(target: Int, animate: Boolean) {
        progressAnimator?.cancel()
        if (!animate) {
            progressPercent = target.toFloat()
            drawProgress()
            return
        }
        progressAnimator = ValueAnimator.ofFloat(progressPercent, target.toFloat()).apply {
            duration = STEP_ANIM_MS
            addUpdateListener {
                progressPercent = it.animatedValue as Float
                drawProgress()
            }
            start()
        }
    }

    /**
     * Görevlerdeki çubukla aynı çizim (mavi dolgu + parlama). Genişlik henüz ölçülmediyse
     * (ilk çizim) ölçüm bitince uygulanıyor.
     */
    private fun drawProgress() {
        val b = _binding ?: return
        if (b.userInfoProgressTrack.width > 0) {
            applyMissionProgressOverlayNow(
                widthHost = b.userInfoProgressTrack,
                fill = b.userInfoProgressFill,
                shine = b.userInfoProgressShine,
                percent = progressPercent,
                done = false,
            )
        } else {
            applyMissionProgressOverlay(
                widthHost = b.userInfoProgressTrack,
                fill = b.userInfoProgressFill,
                shine = b.userInfoProgressShine,
                percent = progressPercent.toInt(),
                done = false,
            )
        }
    }

    /**
     * Kaydırma geçişi.
     *
     * Çıkan görünüm yönün tersine kayıp saydamlaşıyor, ardından içerik yenilenip giren
     * görünüm karşı taraftan geliyor. [from] ile [to] aynı görünüm olabilir (hedef ve
     * meydan okuma adımları aynı kabı paylaşıyor) — o durumda aynı kap çıkıp yeni içerikle
     * geri giriyor.
     */
    private fun slideTo(from: View, to: View, forward: Boolean, bind: () -> Unit) {
        val width = binding.root.width.toFloat()
        if (width <= 0f) {
            // Ölçüm bitmemiş: animasyonsuz geç, yarım bir geçiş göstermektense.
            if (from !== to) from.visibility = View.GONE
            bind()
            to.visibility = View.VISIBLE
            return
        }
        val dir = if (forward) -1f else 1f
        animating = true
        from.animate()
            .translationX(dir * width)
            .alpha(0f)
            .setDuration(STEP_ANIM_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                from.translationX = 0f
                from.alpha = 1f
                if (from !== to) from.visibility = View.GONE

                bind()
                to.visibility = View.VISIBLE
                to.translationX = -dir * width
                to.alpha = 0f
                to.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(STEP_ANIM_MS)
                    .withEndAction { animating = false }
                    .start()
            }
            .start()
    }

    /** Adımın içeriği: başlık, seçenekler, buton yazısı ve buton durumu. */
    private fun bindStep(step: Step) {
        binding.btnContinue.text =
            when (step) {
                Step.CHALLENGE -> "Hedefimi onayla"
                Step.REMINDER -> "Evet, hatırlat"
                else -> "Devam Et"
            }

        // Ölçüm adım GEÇİŞİNE bağlı, çizime değil.
        val stage = when (step) {
            Step.GOAL -> AnalyticsLogger.STREAK_STAGE_GOAL
            Step.ROADMAP -> AnalyticsLogger.STREAK_STAGE_ROADMAP
            Step.CHALLENGE_INTRO -> AnalyticsLogger.STREAK_STAGE_CHALLENGE_INTRO
            Step.CHALLENGE -> AnalyticsLogger.STREAK_STAGE_CHALLENGE
            Step.REMINDER -> AnalyticsLogger.STREAK_STAGE_REMINDER
            else -> null
        }
        if (stage != null && loggedStreakStep != step) {
            loggedStreakStep = step
            AnalyticsLogger.logStreakSetupStep(stage)
        }

        // Tanışmada ilerleme çubuğu yok: henüz soru sorulmuyor (kayan geçişlerde de solarak
        // geliyor/gidiyor, bkz. leaveIntro/returnToIntro).
        binding.userInfoProgress.alpha = if (step.isIntro) 0f else 1f

        when (step) {
            Step.MEET -> {
                // Kendini tanıtıyor, 2 sn arayla el sallıyor.
                binding.introMascot.repeatWithPause(BunnyMascotView.Emote.GREET, INTRO_PAUSE_MS)
                introSay("Selamlar! Benim adım ${getString(R.string.mascot_name)}!")
                updateContinueButton(enabled = true)
            }
            Step.BRIEF -> {
                // Kaç soru sorulacağını söylüyor, 2 sn arayla alkışlıyor.
                binding.introMascot.repeatWithPause(BunnyMascotView.Emote.CLAP, INTRO_PAUSE_MS)
                introSay(briefText())
                updateContinueButton(enabled = true)
            }
            Step.AGE -> {
                val age = binding.etAge.text?.toString()?.trim()?.toIntOrNull()
                updateContinueButton(enabled = age != null && age in 1..120)
            }
            Step.SOURCE -> renderSourceStep()
            Step.GOAL -> renderGoalStep()
            // Düğme yol çizimi bitene kadar kapalı: ekranın anlattığı şey o sıralı belirme,
            // çizim bitmeden geçilirse çocuk tabloyu hiç görmeden atlıyordu.
            Step.ROADMAP -> {
                updateContinueButton(enabled = false)
                renderRoadmap()
            }
            // Soru yok, seçim yok: düğme her zaman açık; alev gün sorusunda geliyor.
            Step.CHALLENGE_INTRO -> {
                // Kayıt akışında burada tavşan utangaç (yeni seri ekranında iki kez selam veriyor).
                // Ekran açık kaldıkça utangaç kalsın: tek seferlik hâl bitince beklemeye
                // dönüyordu. Tekrarlar arasında hâl bırakılmadığı için duruş (kızarıklık, eğik
                // kafa) korunuyor, yalnızca kıpırdanma bir an yavaşlayıp yeniden başlıyor.
                binding.streakMascot.play(BunnyMascotView.Emote.SHY, times = Int.MAX_VALUE)
                updateContinueButton(enabled = true)
            }
            Step.CHALLENGE -> {
                // Gün sorusunda maskotun elinde serinin alevi; seçilen gün arttıkça büyüyor.
                // Burada başlatılıyor, seçimde yeniden çizilen renderChallengeStep'te değil
                // (her seçimde hâl baştan başlardı).
                binding.streakMascot.streakDays = challengeDays
                binding.streakMascot.play(BunnyMascotView.Emote.FLAME)
                renderChallengeStep()
            }
            Step.REMINDER -> {
                // Tavşan yerinde, yalnızca hâli değişiyor: elinde çalar saat, kadranda seçili
                // saat; seçim değiştikçe ibreler dönüp saat çalıyor (renderReminderStep).
                binding.streakMascot.alarmHour = reminderHour
                binding.streakMascot.play(BunnyMascotView.Emote.ALARM)
                renderReminderStep()
            }
        }
        // Seri adımlarının ortak maskotu (niyet + gün sorusu + hatırlatma); kayan kapların
        // dışında, yani adımlar arasında yalnızca yazılar kayıyor, tavşan yerinde kalıyor.
        binding.streakMascot.visibility = when (step) {
            Step.CHALLENGE_INTRO, Step.CHALLENGE, Step.REMINDER -> View.VISIBLE
            else -> View.GONE
        }
        // "Şimdi değil" yalnızca hatırlatma sorusunda.
        binding.btnLater.visibility = if (step == Step.REMINDER) View.VISIBLE else View.GONE
        bindQuestionHeader(step)
    }

    /**
     * Hatırlatma sorusu: diğer sorulardaki satırların aynısı (sol açıklama, sağda saat).
     * 19:00 hazır seçili geliyor, yani düğme baştan açık.
     */
    private fun renderReminderStep() {
        binding.streakStepTitle.text = "Sobi sana her gün hatırlatsın mı?"
        binding.streakStepSubtitle.text = "Saati istediğin zaman değiştirebilirsin"
        StreakViews.buildReminderRows(
            binding.streakOptions,
            selected = reminderHour,
            onPick = ::pickReminderHour,
            // "Başka bir saat": 24 saatlik pencere (ör. sabah çalışmak isteyen çocuk için).
            onCustom = {
                StreakViews.showHourPicker(requireContext(), layoutInflater, reminderHour, ::pickReminderHour)
            },
        )
        updateContinueButton(enabled = reminderHour in 0..23)
    }

    private fun pickReminderHour(hour: Int) {
        if (_binding == null) return
        reminderHour = hour
        // İbreler yeni saate dönüyor, saat çalıyor.
        binding.streakMascot.alarmHour = hour
        renderReminderStep()
    }

    /**
     * Bilgi sorularında (yaş, kaynak, günlük hedef) soru, maskotun yanındaki balonda soruluyor
     * ve kabın kendi başlığı gizleniyor. İlerleme yolunun başlığı da balonda. Seri adımlarında
     * (niyet, gün sorusu) başlık satırı yok; o kaplar kendi maskotlarıyla geliyor.
     */
    private fun bindQuestionHeader(step: Step) {
        val question: CharSequence? = when (step) {
            Step.AGE -> "Kaç yaşındasın?"
            Step.SOURCE -> "Bizi nereden duydun?"
            Step.GOAL -> "Her gün ne kadar öğrenmek istersin?"
            Step.ROADMAP -> withGoal("Her gün ", " buraya varacaksın!")
            Step.MEET, Step.BRIEF, Step.CHALLENGE_INTRO, Step.CHALLENGE, Step.REMINDER -> null
        }
        binding.questionHeader.visibility = when {
            question != null -> View.VISIBLE
            // Tanışmada görünmüyor ama yerini koruyor: tavşan sorulara geçerken tam oraya kayıyor.
            step.isIntro -> View.INVISIBLE
            else -> View.GONE
        }
        // Gün ve hatırlatma sorularının başlığı kendi kabında (üstte ortak tavşan var); hedef
        // sorusunda balona taşındı.
        binding.streakStepTitle.visibility =
            if (step == Step.CHALLENGE || step == Step.REMINDER) View.VISIBLE else View.GONE
        // Metinle karşılaştırılıyor: hedef değişip geri gelinirse yol başlığı yeniden yazılsın.
        if (question != null && question.toString() != shownQuestion) {
            shownQuestion = question.toString()
            typeText(binding.questionBubble, question)
        }
    }

    /** Tanışmada tavşanın balonu: daktiloyla yazılıyor, yazılırken tavşanın ağzı konuşuyor. */
    private fun introSay(text: CharSequence) {
        typeText(binding.introBubble, text)
        binding.introMascot.talk(typeDurationMs(text))
    }

    /**
     * "İlk dersinden önce seni tanımak için sadece N kısa sorum var!" — N, bu akışta gerçekten
     * sorulan soru sayısı ([Step.asks]); adım eklenip çıkarılınca kendiliğinden doğru kalıyor.
     */
    private fun briefText(): CharSequence {
        val count = flowSteps.count { it.asks }
        return android.text.SpannableStringBuilder("İlk dersinden önce seni tanımak için sadece ").apply {
            val start = length
            append("$count kısa sorum")
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            append(" var!")
        }
    }

    /**
     * "Her gün N dakikayla …": kullanıcının az önce seçtiği süre vurgulu (altın, kalın), ekran
     * cevabı ona geri söylüyor.
     */
    private fun withGoal(before: String, after: String): CharSequence =
        android.text.SpannableStringBuilder(before).apply {
            val start = length
            append("$goalMinutes dakikayla")
            setSpan(
                android.text.style.ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), R.color.lesson_ring_gold),
                ),
                start, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            append(after)
        }

    /**
     * İlerleme yolu: üç adım, ikonları ve seçilen süreden hesaplanan tarihleri
     * ([LearningPathPlan]). Çizim kabın kayarak gelmesi bitince başlıyor; geri dönülünce de
     * baştan oynuyor, ekranın asıl anlattığı şey o sıralı belirme.
     */
    private fun renderRoadmap() {
        val labels = LearningPathPlan.timeLabels(goalMinutes)
        binding.learningPath.setMilestones(
            listOf(
                LearningPathView.Milestone(R.drawable.abacus_svg_ic, "Abaküsü tanı", labels[0]),
                // Süre başlıkta zaten yazıyor; burada tekrar etmiyor.
                LearningPathView.Milestone(R.drawable.streak_flame_ic, "Serini büyüt", labels[1]),
                LearningPathView.Milestone(R.drawable.brain, "Kafadan hesap ustası ol", labels[2]),
            ),
        )
        binding.learningPath.playIntro(startDelayMs = STEP_ANIM_MS + 80L) {
            // Bu arada geri dönülüp başka adıma geçildiyse o adımın düğme durumuna dokunma.
            if (_binding != null && currentStep == Step.ROADMAP) {
                updateContinueButton(enabled = true)
            }
        }
    }

    /**
     * Metni balona ([bubble]: soru balonu ya da tanışma balonu) daktilo gibi, hızlıca harf harf
     * yazar.
     *
     * Metin baştan TAMAMI konuyor, henüz yazılmamış kısmı şeffaf bir renkle boyanıyor: balon
     * içeriğe göre boyutlandığı için harf harf eklense satır kaydıkça balon büyüyüp zıplardı.
     * Metindeki vurgular (ör. yol başlığındaki altın "N dakikayla") kopyalanıyor; şeffaf boya
     * onlardan SONRA eklendiği için yazılmamış kısımda vurgu da görünmüyor.
     *
     * Önceki yazma kesilmiyor, SONUNA atlatılıyor ([finishTyping]): kesilse o balonda yarım,
     * şeffaf bir metin kalırdı ve aynı soruya dönülünce ([shownQuestion] aynı) yeniden yazılmazdı.
     */
    private fun typeText(bubble: android.widget.TextView, text: CharSequence) {
        finishTyping()
        val hidden = android.text.style.ForegroundColorSpan(android.graphics.Color.TRANSPARENT)
        fun show(count: Int) {
            val shown = android.text.SpannableString(text)
            if (count < text.length) {
                shown.setSpan(hidden, count, text.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            bubble.text = shown
        }
        show(0)
        typeAnimator = ValueAnimator.ofInt(0, text.length).apply {
            duration = typeDurationMs(text)
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { if (_binding != null) show(it.animatedValue as Int) }
            start()
        }
    }

    private fun typeDurationMs(text: CharSequence) =
        (text.length * TYPE_MS_PER_CHAR).coerceAtLeast(150L)

    /** Süren daktiloyu son harfe atlatır (balonda tam metin kalır). */
    private fun finishTyping() {
        typeAnimator?.end()
        typeAnimator = null
    }

    private fun renderGoalStep() {
        binding.streakStepTitle.text = "Her gün ne kadar öğrenmek istersin?"
        binding.streakStepSubtitle.text = "Bu hedefi istediğin zaman değiştirebilirsin"
        StreakViews.buildOptionRows(
            container = binding.streakOptions,
            values = StreakRepository.GOAL_OPTIONS,
            labels = listOf("Rahat", "Düzenli", "Ciddi"),
            trailing = StreakRepository.GOAL_OPTIONS.map { "$it dakika" },
            selected = goalMinutes,
        ) { value ->
            goalMinutes = value
            renderGoalStep()
        }
        updateContinueButton(enabled = goalMinutes in StreakRepository.GOAL_OPTIONS)
    }

    private fun renderChallengeStep() {
        binding.streakStepTitle.text = "Kaç gün üst üste öğreneceksin?"
        binding.streakStepSubtitle.text =
            "Günde $goalMinutes dakika. Bu seçim sonradan değişmiyor, iyi düşün."
        StreakViews.buildOptionRows(
            container = binding.streakOptions,
            values = StreakRepository.CHALLENGE_OPTIONS,
            labels = StreakRepository.CHALLENGE_OPTIONS.map { "$it gün" },
            // Ödül burada yazıyor çünkü seçim burada yapılıyor ve bir daha
            // değiştirilemiyor: neyin karşılığında söz verdiğini seçerken görmeli.
            trailing = StreakRepository.CHALLENGE_OPTIONS.map {
                "+${StreakMilestones.challengeReward(it)} altın"
            },
            selected = challengeDays,
            trailingColor = StreakViews.COLOR_GOLD,
        ) { value ->
            challengeDays = value
            // Alev seçilen güne göre büyüyor ve tavşan seviniyor.
            binding.streakMascot.streakDays = value
            renderChallengeStep()
        }
        // Meydan okuma bilerek ön seçimsiz: kullanıcı bu sözü kendisi vermeli.
        updateContinueButton(enabled = challengeDays in StreakRepository.CHALLENGE_OPTIONS)
    }

    /** Yaş ve kaynak kaydedilip kayıt ekranına geçilir. */
    private fun finishUserInfo() {
        val birthYear = validatedBirthYear ?: return
        val source = selectedSource ?: return
        saveBirthYearAndProceed(birthYear, source)
    }

    /**
     * Kaynak seçenekleri: günlük hedef sorusundaki satırların aynısı, alt alta. Satırlar
     * indeksle taşınıyor; ekranda [sources]'taki görünen ad, kaydedilen ise değeri.
     * Sağ tarafta açıklama yok.
     */
    private fun renderSourceStep() {
        StreakViews.buildOptionRows(
            container = binding.sourceOptions,
            values = sources.indices.toList(),
            labels = sources.map { it.second },
            trailing = emptyList(),
            selected = sources.indexOfFirst { it.first == selectedSource },
        ) { index ->
            selectedSource = sources[index].first
            renderSourceStep()
        }
        updateContinueButton(enabled = selectedSource != null)
    }

    private fun updateContinueButton(enabled: Boolean) {
        binding.btnContinue.isEnabled = enabled
        val tintColor = if (enabled) {
            ContextCompat.getColor(requireContext(), R.color.dark_primary)
        } else {
            ContextCompat.getColor(requireContext(), R.color.dark_text_secondary)
        }
        binding.btnContinue.backgroundTintList =
            android.content.res.ColorStateList.valueOf(tintColor)
    }

    private fun saveBirthYearAndProceed(birthYear: Int, source: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            AppStatisticsManager.incrementAcquisitionSource(source)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update(
                    mapOf(
                        "birthYear" to birthYear,
                        "acquisitionSource" to source
                    )
                )
                .addOnSuccessListener {
                    navigateToRegister(birthYear, source)
                }
                .addOnFailureListener {
                    navigateToRegister(birthYear, source)
                }
        } else {
            navigateToRegister(birthYear, source)
        }
    }

    private fun navigateToRegister(birthYear: Int, source: String) {
        val intent = Intent(requireContext(), RegisterActivity::class.java).apply {
            if (forceTeacher) putExtra(RegisterActivity.EXTRA_FORCE_TEACHER, true)
            if (forceStudent) putExtra(RegisterActivity.EXTRA_FORCE_STUDENT, true)
            putExtra(RegisterActivity.EXTRA_BIRTH_YEAR, birthYear)
            putExtra(RegisterActivity.EXTRA_ACQUISITION_SOURCE, source)

            prefillEmail?.let { putExtra("prefill_email", it) }
            if (googleSignIn) putExtra("google_sign_in", true)
            googleEmail?.let { putExtra("google_email", it) }
            googleName?.let { putExtra("google_name", it) }
        }
        requireActivity().startActivityForResult(
            intent,
            LoginStartActivity.RC_REGISTER
        )
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocusView = view?.findFocus() ?: binding.root
        imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
        binding.etAge.clearFocus()
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        progressAnimator = null
        typeAnimator?.cancel()
        typeAnimator = null
        shownQuestion = null
        // Kayan/solan görünümlerin bitiş işleri artık olmayan binding'e dokunmasın.
        binding.introMascot.animate().cancel()
        binding.introBubble.animate().cancel()
        binding.questionHeader.animate().cancel()
        binding.userInfoProgress.animate().cancel()
        super.onDestroyView()
        _binding = null
    }
}
