package com.example.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentAccountSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding: FragmentAccountSettingsBinding
        get() = _binding!!

    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager().apply { initialize(requireContext()) }
        setupClickListeners()
        loadUserPlan()
    }

    private fun loadUserPlan() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Önce gizleyelim, yükleme süresince görünmesin
        binding.tvPaymentsHeader.visibility = View.GONE
        binding.paymentsCard.visibility = View.GONE
        
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val plan = doc.getString("plan") ?: "Free"
                    if (plan.equals("Free", ignoreCase = true)) {
                        binding.tvPaymentsHeader.visibility = View.GONE
                        binding.paymentsCard.visibility = View.GONE
                    } else {
                        binding.tvPaymentsHeader.visibility = View.VISIBLE
                        binding.paymentsCard.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun setupClickListeners() {
        binding.btnAccountSettingsDone.setOnClickListener {
            // "BİTTİ" geri dönüşü: Hesap Ayarları önceki fragmana (ProfileFragment) gelsin.
            parentFragmentManager.popBackStack()
        }

        binding.btnProfileSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Profil ayarları yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
        binding.btnPrivacySettings.setOnClickListener {
            Toast.makeText(requireContext(), "Gizlilik ayarları yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnCancelSubscription.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/account/subscriptions"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Play Store açılamadı", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnHelpCenter.setOnClickListener {
            Toast.makeText(requireContext(), "Yardım Merkezi yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
        binding.btnFeedback.setOnClickListener {
            Toast.makeText(requireContext(), "Geri bildirim ekranı yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
        binding.btnAccountSettingsLogout.setOnClickListener {
            MyFirebaseMessagingService.clearCurrentTokenFromFirestore()
            authManager.logout()
            val intent = Intent(requireContext(), LoginStartActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
