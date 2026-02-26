package com.example.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentOtpVerificationBinding

/** Öğretmen şifre sıfırlama akışında OTP doğrulandığında activity'ye bildirim. */
interface OnTeacherOtpVerifiedForResetListener {
    fun onOtpVerifiedForReset(email: String, code: String)
}

/** OTP doğrula butonuna basıldığında / doğrulama bittiğinde activity'nin geri ve Google butonlarını kilitlemesi için. */
interface OnOtpVerifyProgressListener {
    fun onOtpVerifyStarted()
    fun onOtpVerifyFinished()
}

class OtpVerificationFragment : Fragment() {

    private var _binding: FragmentOtpVerificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager

    private val email: String by lazy { requireArguments().getString(ARG_EMAIL).orEmpty() }
    private val isRegistration: Boolean by lazy { requireArguments().getBoolean(ARG_IS_REGISTRATION, false) }
    private val forPasswordReset: Boolean by lazy { requireArguments().getBoolean(ARG_FOR_PASSWORD_RESET, false) }

    companion object {
        private const val ARG_EMAIL = "arg_email"
        private const val ARG_IS_REGISTRATION = "arg_is_registration"
        private const val ARG_FOR_PASSWORD_RESET = "arg_for_password_reset"
        private const val RESEND_COOLDOWN_MS = 45_000L   // 45 saniye
        private const val MAX_WRONG_ATTEMPTS = 5
        private const val WRONG_ATTEMPT_COOLDOWN_MS = 15 * 60 * 1000L // 15 dk

        const val OTP_COOLDOWN_PREFS = "otp_cooldown_prefs"
        const val OTP_COOLDOWN_END_MS = "otp_wrong_cooldown_end_ms"
        const val OTP_COOLDOWN_EMAIL = "otp_wrong_cooldown_email"
        private const val OTP_RESEND_COOLDOWN_END_MS = "otp_resend_cooldown_end_ms"
        private const val OTP_RESEND_COOLDOWN_EMAIL = "otp_resend_cooldown_email"

        /** 45 sn tekrar-gönder cooldown'ı için kalan süre (ms). 0 = yok veya farklı email. */
        fun getResendCooldownRemainingMs(context: Context, forEmail: String): Long {
            val prefs = context.getSharedPreferences(OTP_COOLDOWN_PREFS, Context.MODE_PRIVATE)
            val endMs = prefs.getLong(OTP_RESEND_COOLDOWN_END_MS, 0)
            val savedEmail = prefs.getString(OTP_RESEND_COOLDOWN_EMAIL, null) ?: return 0L
            if (savedEmail != forEmail.trim().lowercase()) return 0L
            if (endMs <= System.currentTimeMillis()) return 0L
            return (endMs - System.currentTimeMillis()).coerceAtLeast(0L)
        }

        /** E-posta ekranında kod başarıyla gönderildiğinde cooldown'ı prefs'e yazar (Kayıt→Giriş için). */
        fun startResendCooldownInPrefs(context: Context, email: String) {
            val endMs = System.currentTimeMillis() + RESEND_COOLDOWN_MS
            context.getSharedPreferences(OTP_COOLDOWN_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(OTP_RESEND_COOLDOWN_END_MS, endMs)
                .putString(OTP_RESEND_COOLDOWN_EMAIL, email.trim().lowercase())
                .apply()
        }

        /** E-posta ekranında Devam Et tıklanınca cooldown varsa (kalan dakika, kayıtlı email) döner; yoksa null. */
        fun getActiveWrongAttemptCooldown(context: Context, forEmail: String): Int? {
            val prefs = context.getSharedPreferences(OTP_COOLDOWN_PREFS, Context.MODE_PRIVATE)
            val endMs = prefs.getLong(OTP_COOLDOWN_END_MS, 0)
            val savedEmail = prefs.getString(OTP_COOLDOWN_EMAIL, null) ?: return null
            if (endMs <= System.currentTimeMillis()) return null
            if (savedEmail != forEmail.trim().lowercase()) return null
            val remainingMin = ((endMs - System.currentTimeMillis()) / 60_000).toInt().coerceAtLeast(1)
            return remainingMin
        }

        fun newInstance(email: String, isRegistration: Boolean, forPasswordReset: Boolean = false): OtpVerificationFragment {
            return OtpVerificationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                    putBoolean(ARG_IS_REGISTRATION, isRegistration)
                    putBoolean(ARG_FOR_PASSWORD_RESET, forPasswordReset)
                }
            }
        }
    }

    private var wrongAttempts = 0
    private var resendCooldownTimer: CountDownTimer? = null
    private var wrongAttemptCooldownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* geri tuşu işlevsiz */ }
        })
        authManager = AuthManager().also { it.initialize(requireContext()) }
        binding.tvTitle.text = when {
            forPasswordReset -> "Şifre sıfırlama"
            isRegistration -> "Hesap oluştur"
            else -> "E-postanı doğrula"
        }
        binding.tvEmail.text = email
        binding.btnVerify.setOnClickListener { verify() }
        binding.btnResend.setOnClickListener { resend() }
        startResendCooldown()
    }

    private fun startResendCooldown() {
        if (wrongAttemptCooldownTimer != null) return // 15 dk kilit varsa resend cooldown başlatma
        resendCooldownTimer?.cancel()
        binding.btnResend.isEnabled = false
        val endMs = System.currentTimeMillis() + RESEND_COOLDOWN_MS
        requireContext().getSharedPreferences(OTP_COOLDOWN_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(OTP_RESEND_COOLDOWN_END_MS, endMs)
            .putString(OTP_RESEND_COOLDOWN_EMAIL, email.trim().lowercase())
            .apply()
        resendCooldownTimer = object : CountDownTimer(RESEND_COOLDOWN_MS, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1_000).toInt()
                binding.btnResend.text = getString(R.string.otp_resend_cooldown, sec)
            }
            override fun onFinish() {
                resendCooldownTimer = null
                requireContext().getSharedPreferences(OTP_COOLDOWN_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .remove(OTP_RESEND_COOLDOWN_END_MS)
                    .remove(OTP_RESEND_COOLDOWN_EMAIL)
                    .apply()
                updateResendButtonState()
            }
        }.start()
    }

    private fun updateResendButtonState() {
        if (wrongAttemptCooldownTimer != null) return
        binding.btnResend.isEnabled = true
        binding.btnResend.text = getString(R.string.otp_resend_button)
    }

    private fun startWrongAttemptCooldown() {
        wrongAttemptCooldownTimer?.cancel()
        resendCooldownTimer?.cancel()
        resendCooldownTimer = null
        binding.btnVerify.isEnabled = false
        binding.btnResend.isEnabled = false
        val endMs = System.currentTimeMillis() + WRONG_ATTEMPT_COOLDOWN_MS
        requireContext().getSharedPreferences(OTP_COOLDOWN_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(OTP_COOLDOWN_END_MS, endMs)
            .putString(OTP_COOLDOWN_EMAIL, email.trim().lowercase())
            .apply()
        wrongAttemptCooldownTimer = object : CountDownTimer(WRONG_ATTEMPT_COOLDOWN_MS, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSec = (millisUntilFinished / 1_000).toInt()
                val min = totalSec / 60
                val sec = totalSec % 60
                binding.btnResend.text = getString(R.string.otp_wrong_cooldown_btn, min, sec)
            }
            override fun onFinish() {
                wrongAttemptCooldownTimer = null
                wrongAttempts = 0
                context?.getSharedPreferences(OTP_COOLDOWN_PREFS, Context.MODE_PRIVATE)
                    ?.edit()
                    ?.remove(OTP_COOLDOWN_END_MS)
                    ?.remove(OTP_COOLDOWN_EMAIL)
                    ?.apply()
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "Doğrula"
                updateResendButtonState()
                Toast.makeText(requireContext(), getString(R.string.otp_can_retry), Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun resend() {
        if (wrongAttemptCooldownTimer != null) return
        binding.btnResend.isEnabled = false
        val sendCode: (Boolean, String?) -> Unit = { success, error ->
            if (success) {
                Toast.makeText(requireContext(), "Kod tekrar gönderildi", Toast.LENGTH_SHORT).show()
                startResendCooldown()
            } else {
                updateResendButtonState()
                Toast.makeText(requireContext(), error ?: "Kod gönderilemedi", Toast.LENGTH_LONG).show()
            }
        }
        if (forPasswordReset) {
            authManager.sendTeacherPasswordResetCode(email, sendCode)
        } else {
            authManager.resendStudentVerificationCode(email, sendCode)
        }
    }

    private fun verify() {
        if (wrongAttemptCooldownTimer != null) return
        if (wrongAttempts >= MAX_WRONG_ATTEMPTS) {
            startWrongAttemptCooldown()
            Toast.makeText(requireContext(), getString(R.string.otp_too_many_wrong), Toast.LENGTH_LONG).show()
            return
        }
        val code = binding.etCode.text?.toString()?.trim().orEmpty()
        if (code.length != 6) {
            Toast.makeText(requireContext(), "Kod 6 haneli olmalıdır", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnVerify.isEnabled = false
        binding.btnVerify.text = "Doğrulanıyor..."
        // Sadece öğretmen şifre sıfırlama akışında (forPasswordReset) geri ve resend kilitlensin
        if (forPasswordReset) {
            binding.btnResend.isEnabled = false
            (requireActivity() as? OnOtpVerifyProgressListener)?.onOtpVerifyStarted()
        }

        val onVerifyFailed: (String?) -> Unit = { error ->
            if (forPasswordReset) {
                (requireActivity() as? OnOtpVerifyProgressListener)?.onOtpVerifyFinished()
                // 15 dk yanlış deneme kilidi yoksa resend butonunu tekrar uygun hale getir
                if (wrongAttemptCooldownTimer == null) {
                    updateResendButtonState()
                }
            }
            wrongAttempts++
            binding.btnVerify.text = "Doğrula"
            binding.btnVerify.isEnabled = wrongAttempts < MAX_WRONG_ATTEMPTS
            if (wrongAttempts >= MAX_WRONG_ATTEMPTS) {
                startWrongAttemptCooldown()
                Toast.makeText(requireContext(), getString(R.string.otp_too_many_wrong), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), error ?: "Kod doğrulanamadı", Toast.LENGTH_LONG).show()
            }
            // Sunucu 15 dk cooldown döndüyse istemcide de kilidi başlat
            if (error != null && (error.contains("15 dakika") || error.contains("Çok fazla yanlış"))) {
                wrongAttempts = MAX_WRONG_ATTEMPTS
                startWrongAttemptCooldown()
            }
        }

        when {
            forPasswordReset -> {
                authManager.verifyTeacherPasswordResetCode(email, code) { success, error ->
                    if (success) {
                        (requireActivity() as? OnOtpVerifyProgressListener)?.onOtpVerifyFinished()
                        (requireActivity() as? OnTeacherOtpVerifiedForResetListener)?.onOtpVerifiedForReset(email, code)
                    } else {
                        onVerifyFailed(error)
                    }
                }
            }
            isRegistration -> {
                authManager.verifyStudentCode(email, code, autoLogin = true) { success, error ->
                    if (success) {
                        requireActivity().setResult(Activity.RESULT_OK)
                        startActivity(Intent(requireContext(), MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_LOGIN, true))
                        requireActivity().finish()
                    } else {
                        onVerifyFailed(error)
                    }
                }
            }
            else -> {
                authManager.verifyLoginWithOTP(email, code) { success, error ->
                    if (success) {
                        requireActivity().setResult(Activity.RESULT_OK)
                        startActivity(Intent(requireContext(), MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_LOGIN, true))
                        requireActivity().finish()
                    } else {
                        onVerifyFailed(error)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendCooldownTimer?.cancel()
        resendCooldownTimer = null
        wrongAttemptCooldownTimer?.cancel()
        wrongAttemptCooldownTimer = null
        _binding = null
    }
}
