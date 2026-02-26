package com.example.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentTeacherNewPasswordBinding

class TeacherNewPasswordFragment : Fragment() {

    private var _binding: FragmentTeacherNewPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager

    private val email: String by lazy { requireArguments().getString(ARG_EMAIL).orEmpty() }
    private val code: String by lazy { requireArguments().getString(ARG_CODE).orEmpty() }

    companion object {
        private const val ARG_EMAIL = "arg_email"
        private const val ARG_CODE = "arg_code"
        private const val MIN_PASSWORD_LENGTH = 6

        fun newInstance(email: String, code: String): TeacherNewPasswordFragment {
            return TeacherNewPasswordFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                    putString(ARG_CODE, code)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherNewPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager().also { it.initialize(requireContext()) }

        binding.btnUpdatePassword.setOnClickListener {
            val newPassword = binding.etNewPassword.text?.toString().orEmpty()
            val confirm = binding.etNewPasswordConfirm.text?.toString().orEmpty()

            if (newPassword.length < MIN_PASSWORD_LENGTH) {
                Toast.makeText(requireContext(), "Şifre en az $MIN_PASSWORD_LENGTH karakter olmalıdır", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirm) {
                Toast.makeText(requireContext(), "Şifreler eşleşmiyor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnUpdatePassword.isEnabled = false
            binding.btnUpdatePassword.text = "Güncelleniyor..."

            authManager.resetTeacherPassword(email, code, newPassword) { success, error ->
                if (success) {
                    authManager.loginTeacher(email, newPassword) { loginSuccess, loginError ->
                        if (loginSuccess) {
                            startActivity(Intent(requireContext(), MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_LOGIN, true))
                            requireActivity().finish()
                        } else {
                            binding.btnUpdatePassword.isEnabled = true
                            binding.btnUpdatePassword.text = "Şifreyi güncelle"
                            Toast.makeText(requireContext(), "Şifre güncellendi. Giriş yapabilirsiniz.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    binding.btnUpdatePassword.isEnabled = true
                    binding.btnUpdatePassword.text = "Şifreyi güncelle"
                    Toast.makeText(requireContext(), error ?: "Şifre güncellenemedi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
