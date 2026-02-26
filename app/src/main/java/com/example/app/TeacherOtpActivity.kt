package com.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TeacherOtpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_otp)

        if (savedInstanceState == null) {
            val email = intent.getStringExtra("email") ?: ""
            val fragment = OtpVerificationFragment.newInstance(email, isRegistration = false)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}

