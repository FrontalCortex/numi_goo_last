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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class StudentEmailVerificationFragment : Fragment() {

    private var _binding: FragmentStudentEmailVerificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager
    private val firestore by lazy { FirebaseFirestore.getInstance() }

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
                Toast.makeText(requireContext(), "Kod tekrar gonderildi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), error ?: "Kod gonderilemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verify() {
        val code = binding.etCode.text?.toString()?.trim().orEmpty()
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (code.length != 6) {
            Toast.makeText(requireContext(), "Kod 6 haneli olmalidir", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Lutfen ad soyad girin", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(requireContext(), "Sifre en az 6 karakter olmali", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnVerify.isEnabled = false
        binding.btnVerify.text = "Dogrulaniyor..."

        // Pending registration bilgisini kaydet (verifyStudentCode bunu kullanacak)
        val pendingData = mapOf(
            "email" to email,
            "name" to name,
            "password" to password,
            "updatedAt" to Timestamp.now()
        )

        firestore.collection("pendingRegistrations").document(email)
            .set(pendingData, SetOptions.merge())
            .addOnSuccessListener {
                authManager.verifyStudentCode(email, code, autoLogin = true) { success, error ->
                    if (!success) {
                        binding.btnVerify.isEnabled = true
                        binding.btnVerify.text = "Dogrula ve Giris Yap"
                        Toast.makeText(requireContext(), error ?: "Kod dogrulanamadi", Toast.LENGTH_LONG).show()
                        return@verifyStudentCode
                    }

                    // Eğer verify mevcut kullanıcı içindi ise FirebaseAuth oturumu olmayabilir; şifre ile giriş yap.
                    if (FirebaseAuth.getInstance().currentUser == null) {
                        authManager.loginStudent(email, password) { loginSuccess, loginError ->
                            if (loginSuccess) {
                                goToMain()
                            } else {
                                binding.btnVerify.isEnabled = true
                                binding.btnVerify.text = "Dogrula ve Giris Yap"
                                Toast.makeText(requireContext(), loginError ?: "Giris basarisiz", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        goToMain()
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "Dogrula ve Giris Yap"
                Toast.makeText(requireContext(), e.localizedMessage ?: "Kayit bilgileri kaydedilemedi", Toast.LENGTH_LONG).show()
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

