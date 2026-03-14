package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class RegisterActivity : AppCompatActivity(), OnOtpVerifyProgressListener {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authManager: AuthManager
    private var resendCooldownTimer: CountDownTimer? = null
    private var isGoogleFlowInProgress: Boolean = false
    
    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
        const val EXTRA_FORCE_TEACHER = "extra_force_teacher"
        const val EXTRA_FORCE_STUDENT = "extra_force_student"
    }

    private var currentForcedRole: ForcedRole = ForcedRole.STUDENT
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager()
        authManager.initialize(this)
        
        currentForcedRole = when {
            intent.getBooleanExtra(EXTRA_FORCE_TEACHER, false) -> ForcedRole.TEACHER
            intent.getBooleanExtra(EXTRA_FORCE_STUDENT, false) -> ForcedRole.STUDENT
            else -> ForcedRole.STUDENT // Varsayılan öğrenci
        }

        // Eğer Google Sign-In'den geldiyse veya girişte "kayıtlı değil" ile yönlendirildiyse email'i doldur
        val googleEmail = intent.getStringExtra("google_email")
        val prefillEmail = intent.getStringExtra("prefill_email")
        val emailToFill = when {
            !googleEmail.isNullOrEmpty() -> googleEmail
            !prefillEmail.isNullOrEmpty() -> prefillEmail
            else -> null
        }
        if (emailToFill != null) {
            binding.etEmail.setText(emailToFill)
            if (!prefillEmail.isNullOrEmpty()) {
                Snackbar.make(binding.root, "Bu e-posta adresi kayıtlı değil. Kayıt sayfasına yönlendirildin.", Snackbar.LENGTH_LONG).show()
            }
        }
        
        setupUI()
        updateUIForRole(currentForcedRole)
    }

    override fun onResume() {
        super.onResume()
        if (binding.emailStepContainer.visibility == android.view.View.VISIBLE) {
            updateContinueButtonForResendCooldown()
        }
    }

    override fun onPause() {
        super.onPause()
        resendCooldownTimer?.cancel()
        resendCooldownTimer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eğer bu activity kapatılırken FirebaseAuth'ta bir kullanıcı varsa
        // ve Firestore'da buna ait tam bir profil yoksa, bu yarım oluşmuş hesabı sil.
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        val usersRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        usersRef.get()
            .addOnSuccessListener { snap ->
                val data = snap.data
                val email = data?.get("email") as? String
                val role = data?.get("role") as? String
                if (data == null || email.isNullOrBlank() || role.isNullOrBlank()) {
                    Log.d("RegisterActivity", "onDestroy: deleting incomplete FirebaseAuth user uid=$uid")
                    currentUser.delete()
                        .addOnCompleteListener {
                            auth.signOut()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("RegisterActivity", "onDestroy: failed to read user doc for cleanup", e)
            }
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                val listener = object : androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                    override fun onBackStackChanged() {
                        if (supportFragmentManager.backStackEntryCount == 0) {
                            supportFragmentManager.removeOnBackStackChangedListener(this)
                            // Pop exit animasyonu (slide_out_right, 300ms) bitene kadar bekle
                            binding.root.postDelayed({
                                if (currentForcedRole == ForcedRole.TEACHER) {
                                    showTeacherStep()
                                } else {
                                    showEmailStep()
                                }
                            }, 350)
                        }
                    }
                }
                supportFragmentManager.addOnBackStackChangedListener(listener)
                supportFragmentManager.popBackStack()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        
        // Öğrenci kaydı: OTP akışı
        binding.btnContinue.setOnClickListener {
            requireOnlineOrShowOffline {
                if (isGoogleFlowInProgress) return@requireOnlineOrShowOffline
                val email = binding.etEmail.text?.toString()?.trim() ?: ""

                if (email.isEmpty()) {
                    showError("Lütfen e-posta girin")
                    return@requireOnlineOrShowOffline
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showError("Geçerli bir e-posta adresi giriniz.")
                    return@requireOnlineOrShowOffline
                }

                sendOtpAndShowCodeStep(email)
            }
        }
        
        // Öğretmen kaydı: Kullanıcı adı + e-posta + şifre + OTP akışı
        binding.btnRegister.setOnClickListener {
            requireOnlineOrShowOffline {
                if (isGoogleFlowInProgress) return@requireOnlineOrShowOffline
                registerTeacherWithOtp()
            }
        }
        
        // Google Sign-In yalnızca öğrenci kaydında kullanılır; öğretmen kaydında hiçbir zaman kullanılmaz
        if (currentForcedRole != ForcedRole.TEACHER) {
            binding.btnGoogleSignIn.setOnClickListener {
                requireOnlineOrShowOffline {
                    if (isGoogleFlowInProgress) return@requireOnlineOrShowOffline
                    setScreenEnabled(false)
                    isGoogleFlowInProgress = true
                    signInWithGoogle()
                }
            }
        }
    }

    override fun onOtpVerifyStarted() {
        setScreenEnabled(false)
    }

    override fun onOtpVerifyFinished() {
        setScreenEnabled(true)
    }

    private enum class ForcedRole { STUDENT, TEACHER }

    private fun updateUIForRole(role: ForcedRole) {
        when (role) {
            ForcedRole.STUDENT -> {
                // Öğrenci kaydı: OTP akışı
                binding.emailStepContainer.visibility = android.view.View.VISIBLE
                binding.cardTeacherForm.visibility = android.view.View.GONE
                binding.btnGoogleSignIn.visibility = android.view.View.VISIBLE
            }
            ForcedRole.TEACHER -> {
                // Öğretmen kaydı: Kullanıcı adı + email + şifre + OTP akışı
                binding.emailStepContainer.visibility = android.view.View.GONE
                binding.cardTeacherForm.visibility = android.view.View.VISIBLE
                // Öğretmen kaydında Google ile kayıt butonu görünmesin
                binding.btnGoogleSignIn.visibility = android.view.View.GONE
            }
        }
    }

    private fun showEmailStep() {
        binding.emailStepContainer.visibility = android.view.View.VISIBLE
        binding.fragmentContainer.visibility = android.view.View.GONE
        binding.ivLogo.visibility = android.view.View.VISIBLE
        binding.tvTitle.visibility = android.view.View.VISIBLE
        binding.tvSubtitle.visibility = android.view.View.VISIBLE
        updateContinueButtonForResendCooldown()
    }

    private fun showTeacherStep() {
        binding.emailStepContainer.visibility = android.view.View.GONE
        binding.cardTeacherForm.visibility = android.view.View.VISIBLE
        binding.fragmentContainer.visibility = android.view.View.GONE
        binding.ivLogo.visibility = android.view.View.VISIBLE
        binding.tvTitle.visibility = android.view.View.VISIBLE
        binding.tvSubtitle.visibility = android.view.View.VISIBLE
    }

    private fun updateContinueButtonForResendCooldown() {
        if (binding.emailStepContainer.visibility != android.view.View.VISIBLE) return
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val remainingMs = OtpVerificationFragment.getResendCooldownRemainingMs(this, email)
        resendCooldownTimer?.cancel()
        resendCooldownTimer = null
        if (remainingMs > 0) {
            binding.btnContinue.isEnabled = false
            resendCooldownTimer = object : CountDownTimer(remainingMs, 1_000) {
                override fun onTick(millisUntilFinished: Long) {
                    val sec = (millisUntilFinished / 1_000).toInt().coerceAtLeast(0)
                    binding.btnContinue.text = getString(R.string.otp_resend_cooldown_try_again, sec)
                }
                override fun onFinish() {
                    resendCooldownTimer = null
                    binding.btnContinue.isEnabled = true
                    binding.btnContinue.text = "Devam Et"
                }
            }.start()
        } else {
            binding.btnContinue.isEnabled = true
            binding.btnContinue.text = "Devam Et"
        }
    }

    private fun showCodeStep() {
        // Sadece OTP fragment'i göster, diğer tüm kayıt formlarını gizle
        binding.emailStepContainer.visibility = android.view.View.GONE
        binding.cardTeacherForm.visibility = android.view.View.GONE
        binding.fragmentContainer.visibility = android.view.View.VISIBLE
        binding.ivLogo.visibility = android.view.View.GONE
        binding.tvTitle.visibility = android.view.View.GONE
        binding.tvSubtitle.visibility = android.view.View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        currentFocus?.clearFocus()
    }

    private fun sendOtpAndShowCodeStep(email: String) {
        val normalizedEmail = email.trim().lowercase()
        val remainingMin = OtpVerificationFragment.getActiveWrongAttemptCooldown(this, normalizedEmail)
        if (remainingMin != null) {
            Toast.makeText(this, getString(R.string.otp_wrong_cooldown_try_later, remainingMin), Toast.LENGTH_LONG).show()
            return
        }
        val resendRemainingMs = OtpVerificationFragment.getResendCooldownRemainingMs(this, normalizedEmail)
        if (resendRemainingMs > 0) {
            updateContinueButtonForResendCooldown()
            return
        }

        binding.btnContinue.isEnabled = false
        binding.btnContinue.text = "Gönderiliyor..."
        binding.btnBack.isEnabled = false
        binding.btnGoogleSignIn.isEnabled = false

        authManager.isEmailRegistered(email) { alreadyRegistered, uid, role ->
            fun reenableBackAndContinue() {
                binding.btnContinue.isEnabled = true
                binding.btnContinue.text = "Devam Et"
                binding.btnBack.isEnabled = true
                binding.btnGoogleSignIn.isEnabled = true
            }
            if (alreadyRegistered && !uid.isNullOrEmpty()) {
                if (role == AuthManager.ROLE_TEACHER) {
                    reenableBackAndContinue()
                    showError("Bu e-posta ile öğrenci hesabı açılamaz.")
                    return@isEmailRegistered
                }
                // Zaten kayıtlı (öğrenci): giriş için kod gönder
                authManager.sendLoginCode(email.trim().lowercase(), uid) { success, error ->
                    reenableBackAndContinue()
                    if (success) {
                        OtpVerificationFragment.startResendCooldownInPrefs(this, normalizedEmail)
                        hideKeyboard()
                        showCodeStep()
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.fragmentContainer, OtpVerificationFragment.newInstance(email.trim().lowercase(), isRegistration = false))
                            .addToBackStack("otp")
                            .commit()
                    } else {
                        showError(error ?: "Kod gönderilemedi")
                    }
                }
            } else if (alreadyRegistered) {
                reenableBackAndContinue()
                showError("Kod gönderilemedi")
            } else {
                // Yeni kayıt: pending oluştur, kod gönder, isRegistration = true
                authManager.createPendingRegistrationForOTP(email) { createSuccess, createError ->
                    if (!createSuccess) {
                        reenableBackAndContinue()
                        showError(createError ?: "İşlem başarısız")
                        return@createPendingRegistrationForOTP
                    }
                    authManager.resendStudentVerificationCode(email) { success, error ->
                        reenableBackAndContinue()
                if (success) {
                            OtpVerificationFragment.startResendCooldownInPrefs(this, normalizedEmail)
                            hideKeyboard()
                            showCodeStep()
                            supportFragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.fragmentContainer, OtpVerificationFragment.newInstance(email, isRegistration = true))
                                .addToBackStack("otp")
                                .commit()
                } else {
                    showError(error ?: "Kod gönderilemedi")
                        }
                    }
                }
            }
        }
    }
    
    private fun signInWithGoogle() {
        android.util.Log.d("RegisterActivity", "signInWithGoogle() çağrıldı - kayıt için")
        
        // Önce mevcut Google Sign-In oturumunu kapat (hesap seçim ekranının görünmesi için)
        GoogleSignIn.getClient(this, com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            .addOnCompleteListener {
                android.util.Log.d("RegisterActivity", "Mevcut Google Sign-In oturumu kapatıldı")
                
                try {
                    val signInIntent = authManager.getGoogleSignInIntent()
                    android.util.Log.d("RegisterActivity", "Google Sign-In Intent alındı, activity başlatılıyor...")
                    if (signInIntent != null) {
                        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
                        android.util.Log.d("RegisterActivity", "Google Sign-In activity başlatıldı")
                    } else {
                        android.util.Log.e("RegisterActivity", "Google Sign-In Intent null!")
                        setScreenEnabled(true)
                        isGoogleFlowInProgress = false
                        showError("Google kaydı başlatılamadı")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RegisterActivity", "Google Sign-In başlatılırken hata oluştu", e)
                    setScreenEnabled(true)
                    isGoogleFlowInProgress = false
                    showError("Google kaydı başlatılamadı: ${e.localizedMessage}")
                }
            }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            // Kullanıcı hesap seçim ekranından geri döndüyse ve iptal ettiyse:
            if (resultCode != RESULT_OK) {
                android.util.Log.d("RegisterActivity", "Google Sign-In iptal edildi veya başarısız")
                setScreenEnabled(true)
                isGoogleFlowInProgress = false
                return
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            // autoRegister = true parametresi ile çağır (kayıt ekranından çağrıldığı için)
            authManager.handleGoogleSignInResult(task, autoRegister = true) { success, error ->
                // Google akışı bu callback'te tamamlanıyor: burada kilidi kaldır.
                setScreenEnabled(true)
                isGoogleFlowInProgress = false

                if (success) {
                    // Google Sign-In ile gelen kullanıcılar otomatik öğrenci olarak kaydedilir
                    android.util.Log.d("RegisterActivity", "Google ile kayıt başarılı, MainActivity'ye yönlendiriliyor")
                    setResult(RESULT_OK)
                    startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_LOGIN, true))
                    finish()
                } else {
                    // Sadece gerçek hataları göster, kullanıcı iptal ettiyse sessizce dön
                    if (error != "Kullanıcı girişi iptal edildi") {
                        showError(error ?: "Google kaydı başarısız")
                    }
                }
            }
        }
    }
    
    
    private fun registerTeacherWithOtp() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etTeacherEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val passwordConfirm = binding.etApprovalCode.text.toString()
        
        if (email.isEmpty() || name.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            showError("Lütfen tüm alanları doldurun")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Geçerli bir e-posta adresi giriniz.")
            return
        }
        if (password.length < 6) {
            showError("Şifre en az 6 karakter olmalı")
            return
        }
        if (password != passwordConfirm) {
            showError("Şifreler eşleşmiyor")
            return
        }
        
        // Önce bu e-posta ile zaten bir öğretmen hesabı var mı kontrol et
        authManager.isEmailRegistered(email) { alreadyRegistered, _, role ->
            if (alreadyRegistered) {
                if (role == AuthManager.ROLE_TEACHER) {
                    showError("Bu e-posta zaten bir öğretmen hesabı ile kayıtlı. Lütfen öğretmen giriş ekranını kullanın.")
                } else {
                    showError("Bu e-posta başka bir hesapta kullanılıyor.")
                }
                return@isEmailRegistered
        }

            binding.btnBack.isEnabled = false
            binding.btnRegister.isEnabled = false
        
            authManager.createPendingTeacherRegistrationForOTP(email, name, password) { success, error ->
                if (!success) {
                    binding.btnBack.isEnabled = true
                    binding.btnRegister.isEnabled = true
                    showError(error ?: "İşlem başarısız")
                    return@createPendingTeacherRegistrationForOTP
                }
                authManager.resendStudentVerificationCode(email) { codeSuccess, codeError ->
                    binding.btnBack.isEnabled = true
                    binding.btnRegister.isEnabled = true
                    if (codeSuccess) {
                        hideKeyboard()
                        showCodeStep()
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.fragmentContainer, OtpVerificationFragment.newInstance(email, isRegistration = true))
                            .addToBackStack("otp")
                            .commit()
            } else {
                        showError(codeError ?: "Kod gönderilemedi")
                    }
                }
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setScreenEnabled(enabled: Boolean) {
        binding.touchBlockOverlay.visibility = if (enabled) View.GONE else View.VISIBLE
    }
}
