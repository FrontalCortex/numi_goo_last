package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
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

        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""

            if (email.isEmpty()) {
                showError("Lütfen e-posta girin")
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Geçerli bir e-posta adresi girin")
                return@setOnClickListener
            }

            sendOtpAndShowCodeStep(email)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onOtpVerifyStarted() {
        binding.btnBack.isEnabled = false
        binding.btnGoogleSignIn.isEnabled = false
    }

    override fun onOtpVerifyFinished() {
        binding.btnBack.isEnabled = true
        binding.btnGoogleSignIn.isEnabled = true
    }

    private fun showEmailStep() {
        binding.emailStepContainer.visibility = android.view.View.VISIBLE
        binding.fragmentContainer.visibility = android.view.View.GONE
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
                    val intent = Intent(this, RegisterActivity::class.java)
                    intent.putExtra("prefill_email", email)
                    startActivity(intent)
                    finish()
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
                        showError("Google girişi başlatılamadı")
                    }
                } catch (e: Exception) {
                    showError("Google girişi başlatılamadı: ${e.localizedMessage}")
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN && resultCode == RESULT_OK && data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            authManager.handleGoogleSignInResult(task) { success, error ->
                if (success) {
                    setResult(RESULT_OK)
                    startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_LOGIN, true))
                    finish()
                } else {
                    if (error == "ACCOUNT_NOT_REGISTERED") {
                        val intent = Intent(this, RegisterActivity::class.java)
                        intent.putExtra("google_sign_in", true)
                        try {
                            val account = task.result
                            if (account != null) {
                                intent.putExtra("google_email", account.email)
                                intent.putExtra("google_name", account.displayName)
                            }
                        } catch (_: Exception) { }
                        startActivity(intent)
                        finish()
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
}
