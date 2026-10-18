package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentParentConsentBinding

/**
 * 13 yaş altı kayıtta velinin e-postasını ister ve onay kodunu VELİYE gönderir.
 *
 * Hesap bu ekranda açılmaz; kod girilene kadar hiçbir yerde açılmaz (COPPA / GDPR / KVKK:
 * doğrulanabilir veli onayı). İki yol var:
 *   • E-posta ile kayıt: kod `register_parental` amacıyla gider, sonra normal kayıt
 *     koduyla aynı [AuthManager.verifyStudentCode] çağrısıyla girilir.
 *   • Google ile kayıt: Google oturumu açık, `users` dokümanı yok; kod
 *     [AuthManager.requestGoogleParentalConsent] ile istenir, onay sunucuda hesabı açar.
 */
class ParentConsentFragment : Fragment() {

    private var _binding: FragmentParentConsentBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager

    private val childEmail: String by lazy { requireArguments().getString(ARG_CHILD_EMAIL).orEmpty() }
    private val isGoogle: Boolean by lazy { requireArguments().getBoolean(ARG_IS_GOOGLE, false) }
    private val birthYear: Int by lazy { requireArguments().getInt(ARG_BIRTH_YEAR, -1) }
    private val acquisitionSource: String? by lazy { requireArguments().getString(ARG_ACQUISITION_SOURCE) }

    companion object {
        private const val ARG_CHILD_EMAIL = "arg_child_email"
        private const val ARG_IS_GOOGLE = "arg_is_google"
        private const val ARG_BIRTH_YEAR = "arg_birth_year"
        private const val ARG_ACQUISITION_SOURCE = "arg_acquisition_source"

        fun newInstance(
            childEmail: String,
            isGoogle: Boolean,
            birthYear: Int,
            acquisitionSource: String?,
        ): ParentConsentFragment = ParentConsentFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CHILD_EMAIL, childEmail.trim().lowercase())
                putBoolean(ARG_IS_GOOGLE, isGoogle)
                putInt(ARG_BIRTH_YEAR, birthYear)
                acquisitionSource?.let { putString(ARG_ACQUISITION_SOURCE, it) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParentConsentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Kod ekranındaki gibi: sistem geri tuşu yığını boşaltınca e-posta adımı gizli
        // kalıyordu; geri dönüş yalnızca ekrandaki geri düğmesiyle (RegisterActivity).
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* geri tuşu işlevsiz */ }
        })
        authManager = AuthManager().also { it.initialize(requireContext()) }
        AnalyticsLogger.logSignupStep(
            AnalyticsLogger.SIGNUP_PARENT_CONSENT,
            AnalyticsLogger.SIGNUP_ROLE_STUDENT,
        )
        binding.btnSendParentCode.setOnClickListener { sendCode() }
    }

    private fun sendCode() {
        val parentEmail = binding.etParentEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(parentEmail).matches()) {
            toast(appString(R.string.ui_gecerli_bir_e_posta_adresi_giriniz))
            return
        }
        // Hesap e-postasıyla aynı olabilir: hesabı veli kendi adresiyle açıyor olabilir.

        binding.btnSendParentCode.isEnabled = false
        binding.btnSendParentCode.text = appString(R.string.ui_gonderiliyor)
        val onSent: (Boolean, String?) -> Unit = onSent@{ success, error ->
            if (_binding == null || !isAdded) return@onSent
            binding.btnSendParentCode.isEnabled = true
            binding.btnSendParentCode.text = getString(R.string.fragment_parent_consent_btn_send)
            if (error == AuthManager.CHILD_EMAIL_NOT_VERIFIED) {
                // Veli adresi farklı: önce çocuğun KENDİ adresi doğrulanmalı (yanlış yazılan
                // adresle açılan hesaba bir daha girilemezdi). Veli kodu o adımdan sonra gider.
                sendChildEmailCode(parentEmail)
                return@onSent
            }
            if (!success) {
                toast(error ?: appString(R.string.ui_kod_gonderilemedi))
                return@onSent
            }
            AnalyticsLogger.logSignupStep(
                AnalyticsLogger.SIGNUP_OTP_SENT,
                AnalyticsLogger.SIGNUP_ROLE_STUDENT,
            )
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(
                    id,
                    OtpVerificationFragment.newInstance(
                        email = childEmail,
                        isRegistration = true,
                        birthYear = birthYear,
                        acquisitionSource = acquisitionSource,
                        parentEmail = parentEmail,
                        googleParental = isGoogle,
                    )
                )
                .addToBackStack("otp")
                .commit()
        }
        if (isGoogle) {
            authManager.requestGoogleParentalConsent(parentEmail, birthYear, acquisitionSource, onSent)
        } else {
            authManager.sendParentalRegistrationCode(childEmail, parentEmail, birthYear, acquisitionSource, onSent)
        }
    }

    /** Çocuğun kendi adresine normal kayıt kodu; kod ekranı doğrulayınca veli kodunu gönderir. */
    private fun sendChildEmailCode(parentEmail: String) {
        binding.btnSendParentCode.isEnabled = false
        binding.btnSendParentCode.text = appString(R.string.ui_gonderiliyor)
        authManager.resendStudentVerificationCode(childEmail, isRegistration = true) sent@{ success, error ->
            if (_binding == null || !isAdded) return@sent
            binding.btnSendParentCode.isEnabled = true
            binding.btnSendParentCode.text = getString(R.string.fragment_parent_consent_btn_send)
            if (!success) {
                toast(error ?: appString(R.string.ui_kod_gonderilemedi))
                return@sent
            }
            OtpVerificationFragment.startResendCooldownInPrefs(requireContext(), childEmail)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(
                    id,
                    OtpVerificationFragment.newInstance(
                        email = childEmail,
                        isRegistration = true,
                        birthYear = birthYear,
                        acquisitionSource = acquisitionSource,
                        pendingParentEmail = parentEmail,
                    )
                )
                .addToBackStack("otp_child")
                .commit()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
