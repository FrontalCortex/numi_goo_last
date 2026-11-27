package com.example.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentStudentEmailVerificationBinding
import com.google.firebase.auth.FirebaseAuth

class StudentEmailVerificationFragment : Fragment() {

    private var _binding: FragmentStudentEmailVerificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager

    private val email: String by lazy { requireArguments().getString(ARG_EMAIL).orEmpty() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentEmailVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager().also { it.initialize(requireContext()) }

        binding.tvEmail.text = email

        binding.btnVerify.setOnClickListener { verify() }
        binding.btnResend.setOnClickListener { resend() }
    }

    private fun resend() {
        binding.btnResend.isEnabled = false
        authManager.resendStudentVerificationCode(email) { success, error ->
            binding.btnResend.isEnabled = true
            if (success) {
                Toast.makeText(requireContext(), appString(R.string.ui_kod_tekrar_gonderildi_2), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), error ?: appString(R.string.ui_kod_gonderilemedi_2), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verify() {
        val code = binding.etCode.text?.toString()?.trim().orEmpty()
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (code.length != 6) {
            Toast.makeText(requireContext(), appString(R.string.ui_kod_6_haneli_olmalidir_2), Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), appString(R.string.ui_lutfen_ad_soyad_girin), Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(requireContext(), appString(R.string.ui_sifre_en_az_6_karakter_olmali_2), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnVerify.isEnabled = false
        binding.btnVerify.text = appString(R.string.ui_dogrulaniyor_2)

        // Kayıt bilgisi artık Firestore'a YAZILMIYOR (eskiden pendingRegistrations/{email}
        // dokümanına düz metin şifre yazılıyordu). Bellekte tutulup verifyStudentCode
        // çağrısının gövdesinde sunucuya gidiyor.
        AuthManager.rememberPendingRegistration(
            email = email,
            name = name,
            password = password,
            role = AuthManager.ROLE_STUDENT,
        )

        authManager.verifyStudentCode(email, code, autoLogin = true) { success, error ->
            if (!success) {
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = appString(R.string.ui_dogrula_ve_giris_yap)
                Toast.makeText(requireContext(), error ?: appString(R.string.ui_kod_dogrulanamadi_2), Toast.LENGTH_LONG).show()
                return@verifyStudentCode
            }

            // Eğer verify mevcut kullanıcı içindi ise FirebaseAuth oturumu olmayabilir; şifre ile giriş yap.
            if (FirebaseAuth.getInstance().currentUser == null) {
                authManager.loginStudent(email, password) { loginSuccess, loginError ->
                    if (loginSuccess) {
                        goToMain()
                    } else {
                        binding.btnVerify.isEnabled = true
                        binding.btnVerify.text = appString(R.string.ui_dogrula_ve_giris_yap)
                        Toast.makeText(requireContext(), loginError ?: appString(R.string.ui_giris_basarisiz), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                goToMain()
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(requireContext(), MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_FROM_LOGIN, true)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"

        fun newInstance(email: String): StudentEmailVerificationFragment {
            return StudentEmailVerificationFragment().apply {
                arguments = Bundle().apply { putString(ARG_EMAIL, email) }
            }
        }
    }
}

