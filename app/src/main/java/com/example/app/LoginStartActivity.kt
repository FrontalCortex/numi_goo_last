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

        // BAŞLA → Önce UserInfoFragment'i aç
        binding.btnSecondary.setOnClickListener {
            showUserInfoFragment(isTeacherMode)
        }

        // Sağ alttaki "Öğretmen girişi" / "Öğrenci girişi" butonu
        binding.btnTeacherMode.setOnClickListener {
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

    private fun showUserInfoFragment(isTeacherMode: Boolean) {
        // Fragment container'ı göster, ana içeriği gizle
        binding.userInfoFragmentContainer.visibility = android.view.View.VISIBLE
        binding.userInfoFragmentContainer.layoutParams =
            (binding.userInfoFragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).also {
                it.width = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
                it.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
            }

        // Ana görünümü gizle
        setMainContentVisible(false)

        val fragment = UserInfoFragment.newInstance(
            forceTeacher = isTeacherMode,
            forceStudent = !isTeacherMode
        )

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.userInfoFragmentContainer, fragment, TAG_USER_INFO)
            .addToBackStack(TAG_USER_INFO)
            .commit()

        // Fragment'in geri tuşu tepkisi için BackStack listener
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                // UserInfoFragment kapandı → ana içeriği geri getir
                binding.userInfoFragmentContainer.visibility = android.view.View.GONE
                setMainContentVisible(true)
            }
        }
    }

    private fun setMainContentVisible(visible: Boolean) {
        val v = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvQuestion.visibility = v
        binding.tvSubtitle.visibility = v
        binding.btnPrimary.visibility = v
        binding.btnSecondary.visibility = v
        binding.divider.visibility = v
        binding.btnTeacherMode.visibility = v
    }

    // RegisterActivity'den dönen sonucu yakala
    @Deprecated("Needed for UserInfoFragment's startActivityForResult call")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_REGISTER && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    companion object {
        const val EXTRA_TEACHER_MODE = "extra_teacher_mode"
        const val EXTRA_BLOCK_BACK = "extra_block_back"
        const val RC_REGISTER = 1001
        private const val TAG_USER_INFO = "user_info"
    }
}
