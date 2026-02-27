package com.example.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityTeacherLoginBinding

class TeacherLoginActivity : AppCompatActivity(),
    OnTeacherOtpVerifiedForResetListener,
    OnOtpVerifyProgressListener {

    private lateinit var binding: ActivityTeacherLoginBinding
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager()
        authManager.initialize(this)
        
        setupUI()
        binding.tvForgotPassword.paintFlags = binding.tvForgotPassword.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            if (binding.fragmentContainer.visibility == View.VISIBLE) {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    showLoginForm()
                }
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.btnLogin.setOnClickListener {
            loginTeacher()
        }
        
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordFlow()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0 && binding.fragmentContainer.visibility == View.VISIBLE) {
                showLoginForm()
            }
        }
    }

    private fun showLoginForm() {
        binding.loginFormContainer.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
    }

    private fun showForgotPasswordFlow() {
        binding.loginFormContainer.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        val fragment = TeacherForgotPasswordEmailFragment().apply {
            onCodeSent = { email ->
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, OtpVerificationFragment.newInstance(email, isRegistration = false, forPasswordReset = true))
                    .addToBackStack("forgot_otp")
                    .commit()
        }
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("forgot_email")
            .commit()
    }

    override fun onOtpVerifiedForReset(email: String, code: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainer, TeacherNewPasswordFragment.newInstance(email, code))
            .addToBackStack("forgot_new_password")
            .commit()
    }

    override fun onOtpVerifyStarted() {
        binding.btnBack.isEnabled = false
    }

    override fun onOtpVerifyFinished() {
        binding.btnBack.isEnabled = true
    }
    
    private fun loginTeacher() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        if (email.isEmpty() || password.isEmpty()) {
            showError("Lütfen tüm alanları doldurun")
            return
        }
        binding.btnBack.isEnabled = false
        binding.btnLogin.isEnabled = false
        binding.tvForgotPassword.isEnabled = false
        
        authManager.loginTeacher(email, password) { success, error ->
            binding.btnBack.isEnabled = true
            binding.btnLogin.isEnabled = true
            binding.tvForgotPassword.isEnabled = true
            if (success) {
                setResult(RESULT_OK)
                startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_LOGIN, true))
                finish()
            } else {
                showError(error ?: "Şifre veya e-posta hatalı")
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
