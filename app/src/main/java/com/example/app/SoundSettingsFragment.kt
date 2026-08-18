package com.example.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SoundSettingsFragment : Fragment() {

    private lateinit var switchSound: SwitchCompat
    private lateinit var switchTutorialSound: SwitchCompat
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var btnClose: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sound_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchSound = view.findViewById(R.id.switchSound)
        switchTutorialSound = view.findViewById(R.id.switchTutorialSound)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        btnClose = view.findViewById(R.id.btnClose)

        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Mevcut ayarları yükle
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        switchTutorialSound.isChecked = prefs.getBoolean("tutorial_sound_enabled", true)
        switchNotifications.isChecked = prefs.getBoolean("notifications_enabled", true)

        // Dinleyiciler
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        switchTutorialSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("tutorial_sound_enabled", isChecked).apply()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            
            // Firebase veritabanında da bildirimi kapat/aç (Cloud Functions için)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("notificationsEnabled", isChecked)
                    .addOnFailureListener {
                        // Sessizce hatayı yoksayabiliriz veya loglayabiliriz
                    }
            }
        }

        btnClose.setOnClickListener {
            closeFragment()
        }
    }

    private fun closeFragment() {
        val main = activity as? MainActivity
        if (main != null) {
            main.finishTasksOverlayAnimated("SoundSettingsFragment.close")
        } else {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_right)
                .remove(this)
                .commit()
        }
    }
}
