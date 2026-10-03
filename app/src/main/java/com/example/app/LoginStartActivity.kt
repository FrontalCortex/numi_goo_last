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
        // Gece temasında gezinme çubuğu `message_topbar` (mesaj ekranlarına göre seçilmiş)
        // geliyor ve ekranın zemininden kopuk duruyordu; Splash ve MainActivity de aynı
        // şekilde ekranın zemin rengine çekiyor. API 35+ bu çağrıyı yok sayıp pencere
        // zeminini gösteriyor, o da temada zaten `background_color`.
        window.navigationBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.background_color)
        binding = ActivityLoginStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putBoolean("login_start_ever_shown", true).apply()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Kayıt soruları açıksa geri tuşu ona ait: bir önceki adıma dönmeli.
                // Bu dinleyici geri tuşunu KOŞULSUZ yutuyordu ve fragment'in kendi
                // dinleyicisinin her zaman önce çalışacağına güveniliyordu; sahada
                // çalışmadı. Artık yutmadan önce buraya soruluyor, yani davranış
                // dinleyici sırasına bağımlı değil.
                val userInfo = supportFragmentManager
                    .findFragmentByTag(TAG_USER_INFO) as? UserInfoFragment
                if (userInfo != null && userInfo.isVisible && userInfo.onBackStep()) return
                /* aksi halde geri tuşu işlevsiz */
            }
        })

        val isTeacherMode = intent.getBooleanExtra(EXTRA_TEACHER_MODE, false)
        // Kayıt hunisinin paydası. Öğretmen/öğrenci geçişi bu activity'yi yeniden başlattığı
        // için rol değiştirildiğinde tekrar düşer — istenen davranış: kullanıcı diğer akışa
        // geçmiş oluyor.
        AnalyticsLogger.logSignupStep(
            stage = AnalyticsLogger.SIGNUP_START,
            role = if (isTeacherMode) AnalyticsLogger.SIGNUP_ROLE_TEACHER
                   else AnalyticsLogger.SIGNUP_ROLE_STUDENT,
        )
        setupUI(isTeacherMode)
    }

    private fun setupUI(isTeacherMode: Boolean) {
        if (isTeacherMode) {
            binding.tvTitle.text = getString(R.string.login_start_teacher_title)
            binding.tvBody.text = getString(R.string.login_start_teacher_subtitle)
        } else {
            binding.tvTitle.text = getString(R.string.login_start_student_title)
            binding.tvBody.text = getString(R.string.login_start_student_subtitle)
        }

        // "Zaten hesabım var" → giriş ekranı
        binding.btnLogin.setOnClickListener {
            if (isTeacherMode) {
                loginOrRegisterLauncher.launch(Intent(this, TeacherLoginActivity::class.java))
            } else {
                loginOrRegisterLauncher.launch(Intent(this, LoginActivity::class.java))
            }
        }

        // "Başla" → Önce UserInfoFragment'i aç
        binding.btnStart.setOnClickListener {
            showUserInfoFragment(isTeacherMode)
        }

        // Maskot: ekran açılış geçişi bitince el sallayıp "merhaba" der; dokununca sevinir.
        binding.mascotView.postDelayed({ binding.mascotView.greet() }, 400)
        // Şimdilik beklerken de 5 saniyede bir selam veriyor (kullanıcı isteği).
        binding.mascotView.autoGreetIntervalMs = 5_000L
        binding.mascotView.setOnClickListener { binding.mascotView.cheer() }

        // Sağ üstteki "Öğretmen girişi" / "Öğrenci girişi" butonu
        binding.btnTeacherMode.setOnClickListener {
            val intent = Intent(this, LoginStartActivity::class.java)
                .putExtra(EXTRA_TEACHER_MODE, !isTeacherMode)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Sağ üst köşe buton metni
        binding.btnTeacherMode.text = if (isTeacherMode) {
            getString(R.string.login_start_student_mode_button)
        } else {
            getString(R.string.login_start_teacher_mode_button)
        }
    }

    private fun showUserInfoFragment(isTeacherMode: Boolean) {
        // Fragment container'ı göster, ana içeriği gizle
        // Kap artık kökteki dolgunun dışında ve XML'de zaten match_parent; boyutu elle
        // ayarlamaya gerek yok (eski kod ConstraintLayout.LayoutParams'a cast ediyordu,
        // yeni kökte o cast çalışmaz).
        binding.userInfoFragmentContainer.visibility = android.view.View.VISIBLE

        // Ana görünümü gizle
        setMainContentVisible(false)

        val fragment = UserInfoFragment.newInstance(
            forceTeacher = isTeacherMode,
            forceStudent = !isTeacherMode
        )

        // Geçiş animasyonu yok: soru ekranı "Başla"ya basılınca doğrudan geliyor (kullanıcı
        // isteği; kayarak gelmesi ekranı yavaş hissettiriyordu).
        supportFragmentManager.beginTransaction()
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
        // Görünümleri tek tek saymak yerine bütün katman gizleniyor: tasarıma yeni bir
        // öğe eklendiğinde buraya eklemeyi unutma riski kalmıyor.
        binding.loginStartContent.visibility =
            if (visible) android.view.View.VISIBLE else android.view.View.GONE
        // Maskotun durdurulmasına gerek yok: katman gizlenince BunnyMascotView kare
        // çizmeyi kendisi bırakıyor.
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
