package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.app.OnOtpVerifyProgressListener
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentTeacherForgotPasswordEmailBinding

class TeacherForgotPasswordEmailFragment : Fragment() {

    private var _binding: FragmentTeacherForgotPasswordEmailBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager

    var onCodeSent: ((email: String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherForgotPasswordEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager().also { it.initialize(requireContext()) }

        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "E-posta adresi girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Geçerli bir e-posta adresi girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnContinue.isEnabled = false
            binding.btnContinue.text = "Gönderiliyor..."

            // Öğretmen şifre sıfırlama akışında e-posta gönderimi sırasında sol üst geri butonunu kilitle
            (activity as? OnOtpVerifyProgressListener)?.onOtpVerifyStarted()

            authManager.sendTeacherPasswordResetCode(email) { success, error ->
                binding.btnContinue.isEnabled = true
                binding.btnContinue.text = "Devam Et"
                (activity as? OnOtpVerifyProgressListener)?.onOtpVerifyFinished()
                if (success) {
                    onCodeSent?.invoke(email.trim().lowercase())
                } else {
                    Toast.makeText(requireContext(), error ?: "Kod gönderilemedi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
