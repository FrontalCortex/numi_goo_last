package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class LoginActivity : AppCompatActivity(), OnOtpVerifyProgressListener {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    private var resendCooldownTimer: CountDownTimer? = null
    private var isGoogleFlowInProgress: Boolean = false
    
    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
        private const val TAG_USER_INFO = "user_info"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager()
        authManager.initialize(this)
        
        setupUI()
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
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                val listener = object : androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                    override fun onBackStackChanged() {
                        if (supportFragmentManager.backStackEntryCount == 0) {
                            supportFragmentManager.removeOnBackStackChangedListener(this)
                            // Pop exit animasyonu (slide_out_right, 300ms) bitene kadar bekle
                            binding.root.postDelayed({ showEmailStep() }, 350)
                        }
                    }
                }
                supportFragmentManager.addOnBackStackChangedListener(listener)
                supportFragmentManager.popBackStack()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                if (binding.userInfoFragmentContainer.visibility == View.VISIBLE) {
                    binding.userInfoFragmentContainer.visibility = View.GONE
                    binding.emailStepContainer.visibility = View.VISIBLE
                    binding.btnGoogleSignIn.visibility = View.VISIBLE
                    binding.btnBack.visibility = View.VISIBLE
                    updateContinueButtonForResendCooldown()
                }
            }
        }

        binding.btnContinue.setOnClickListener {
            requireOnlineOrShowOffline {
                if (isGoogleFlowInProgress) return@requireOnlineOrShowOffline
                val email = binding.etEmail.text?.toString()?.trim() ?: ""

                if (email.isEmpty()) {
                    showError("Lütfen e-posta girin")
                    return@requireOnlineOrShowOffline
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showError("Geçerli bir e-posta adresi girin")
                    return@requireOnlineOrShowOffline
                }

                sendOtpAndShowCodeStep(email)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            requireOnlineOrShowOffline {
                if (isGoogleFlowInProgress) return@requireOnlineOrShowOffline
                setScreenEnabled(false)
                isGoogleFlowInProgress = true
                signInWithGoogle()
            }
        }
    }

    override fun onOtpVerifyStarted() {
        setScreenEnabled(false)
    }

    override fun onOtpVerifyFinished() {
        setScreenEnabled(true)
    }

    private fun showEmailStep() {
        binding.userInfoFragmentContainer.visibility = android.view.View.GONE
        binding.emailStepContainer.visibility = android.view.View.VISIBLE
        binding.fragmentContainer.visibility = android.view.View.GONE
        binding.btnGoogleSignIn.visibility = android.view.View.VISIBLE
        binding.btnBack.visibility = android.view.View.VISIBLE
        updateContinueButtonForResendCooldown()
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
        binding.emailStepContainer.visibility = android.view.View.GONE
        binding.fragmentContainer.visibility = android.view.View.VISIBLE
    }

    private fun showUserInfoFragment(
        prefillEmail: String? = null,
        googleSignIn: Boolean = false,
        googleEmail: String? = null,
        googleName: String? = null
    ) {
        hideKeyboard()
        binding.emailStepContainer.visibility = View.GONE
        binding.fragmentContainer.visibility = View.GONE
        binding.btnGoogleSignIn.visibility = View.GONE
        binding.btnBack.visibility = View.GONE

        binding.userInfoFragmentContainer.visibility = View.VISIBLE
        binding.userInfoFragmentContainer.layoutParams =
            (binding.userInfoFragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).also {
                it.width = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                it.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            }

        val fragment = UserInfoFragment.newInstance(
            forceTeacher = false,
            forceStudent = true,
            prefillEmail = prefillEmail,
            googleSignIn = googleSignIn,
            googleEmail = googleEmail,
            googleName = googleName
        )

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.userInfoFragmentContainer, fragment, TAG_USER_INFO)
            .addToBackStack(TAG_USER_INFO)
            .commit()
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

        authManager.sendLoginCodeOnly(email) { success, error ->
            binding.btnContinue.isEnabled = true
            binding.btnContinue.text = "Devam Et"
            binding.btnBack.isEnabled = true
            binding.btnGoogleSignIn.isEnabled = true

            if (success) {
                hideKeyboard()
                showCodeStep()
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, OtpVerificationFragment.newInstance(email, isRegistration = false))
                    .addToBackStack("otp")
                    .commit()
            } else {
                if (error == "Kullanıcı bulunamadı") {
                    showUserInfoFragment(prefillEmail = email)
                } else {
                    showError(error ?: "Kod gönderilemedi")
                }
            }
        }
    }
    
    private fun signInWithGoogle() {
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            .addOnCompleteListener {
                try {
                    val signInIntent = authManager.getGoogleSignInIntent()
                    if (signInIntent != null) {
                        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
                    } else {
                        setScreenEnabled(true)
                        isGoogleFlowInProgress = false
                        showError("Google girişi başlatılamadı")
                    }
                } catch (e: Exception) {
                    setScreenEnabled(true)
                    isGoogleFlowInProgress = false
                    showError("Google girişi başlatılamadı: ${e.localizedMessage}")
                }
            }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LoginStartActivity.RC_REGISTER && resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
            return
        }
        
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            // Kullanıcı hesap seçim ekranından geri döndüyse ve iptal ettiyse:
            if (resultCode != RESULT_OK || data == null) {
                // Play Services, imza/paket uyuşmazlığında da "iptal" gibi döner. Gerçek nedeni
                // ApiException'ın durum kodundan oku: 10 = DEVELOPER_ERROR (SHA-1 eşleşmiyor),
                // 12501 = kullanıcı gerçekten iptal etti, 7 = ağ hatası.
                android.util.Log.w(
                    "LoginActivity",
                    "Google Sign-In tamamlanmadı: ${googleSignInFailureReason(data)}"
                )
                setScreenEnabled(true)
                isGoogleFlowInProgress = false
                return
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            authManager.handleGoogleSignInResult(task) { success, error ->
                // Google akışı bu callback'te tamamlanıyor: burada kilidi kaldır.
                setScreenEnabled(true)
                isGoogleFlowInProgress = false

                if (success) {
                    setResult(RESULT_OK)
                    GlobalLessonData.seedAllLessonProgressIfMissing(applicationContext)
                    startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_LOGIN, true))
                    finish()
                } else {
                    if (error == "ACCOUNT_NOT_REGISTERED") {
                        var googleEmail: String? = null
                        var googleName: String? = null
                        try {
                            val account = task.result
                            if (account != null) {
                                googleEmail = account.email
                                googleName = account.displayName
                            }
                        } catch (_: Exception) { }
                        showUserInfoFragment(
                            googleSignIn = true,
                            googleEmail = googleEmail,
                            googleName = googleName
                        )
                    } else if (error != "Kullanıcı girişi iptal edildi") {
                        showError(error ?: "Google girişi başarısız")
                    }
                }
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setScreenEnabled(enabled: Boolean) {
        binding.touchBlockOverlay.visibility = if (enabled) View.GONE else View.VISIBLE
    }
}
