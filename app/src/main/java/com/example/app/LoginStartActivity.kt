package com.example.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.app.databinding.ActivityLoginStartBinding

class LoginStartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginStartBinding

    private val loginOrRegisterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putBoolean("login_start_ever_shown", true).apply()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* geri tuşu işlevsiz */ }
        })

        val isTeacherMode = intent.getBooleanExtra(EXTRA_TEACHER_MODE, false)
        setupUI(isTeacherMode)
    }

    private fun setupUI(isTeacherMode: Boolean) {
        if (isTeacherMode) {
            binding.tvSubtitle.text = getString(R.string.login_start_teacher_subtitle)
            binding.tvQuestion.text = getString(R.string.login_start_teacher_title)
        } else {
            binding.tvSubtitle.text = getString(R.string.login_start_student_subtitle)
            binding.tvQuestion.text = getString(R.string.login_start_student_title)
        }

        // GİRİŞ YAP
        binding.btnPrimary.setOnClickListener {
            if (isTeacherMode) {
                loginOrRegisterLauncher.launch(Intent(this, TeacherLoginActivity::class.java))
            } else {
                loginOrRegisterLauncher.launch(Intent(this, LoginActivity::class.java))
            }
        }

        // BAŞLA
        binding.btnSecondary.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            if (isTeacherMode) {
                intent.putExtra(RegisterActivity.EXTRA_FORCE_TEACHER, true)
            } else {
                intent.putExtra(RegisterActivity.EXTRA_FORCE_STUDENT, true)
            }
            loginOrRegisterLauncher.launch(intent)
        }

        // Sağ alttaki "Öğretmen girişi" / "Öğrenci girişi" butonu
        binding.btnTeacherMode.setOnClickListener {
            // Modu tersine çevirip aynı activity'yi yeniden aç
            val intent = Intent(this, LoginStartActivity::class.java)
                .putExtra(EXTRA_TEACHER_MODE, !isTeacherMode)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Alt köşe buton metni
        binding.btnTeacherMode.text = if (isTeacherMode) {
            getString(R.string.login_start_student_mode_button)
        } else {
            getString(R.string.login_start_teacher_mode_button)
        }
    }

    companion object {
        const val EXTRA_TEACHER_MODE = "extra_teacher_mode"
        const val EXTRA_BLOCK_BACK = "extra_block_back"
    }
}

