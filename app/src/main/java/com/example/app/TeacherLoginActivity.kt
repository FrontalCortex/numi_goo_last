package com.example.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.auth.AuthManager
import com.example.app.databinding.ActivityTeacherLoginBinding

class TeacherLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTeacherLoginBinding
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager()
        authManager.initialize(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        // Öğretmen girişi
        binding.btnLogin.setOnClickListener {
            loginTeacher()
        }
        
        // Öğrenci girişi
        binding.btnStudentLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        
        // Kayıt ol
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun loginTeacher() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        
        if (email.isEmpty() || password.isEmpty()) {
            showError("Lütfen tüm alanları doldurun")
            return
        }
        
        authManager.loginTeacher(email, password) { success, error ->
            if (success) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                showError(error ?: "Giriş başarısız")
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
