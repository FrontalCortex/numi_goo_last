package com.example.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadUserData()
        setupTextValidation()

        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupTextValidation() {
        binding.etName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val error = validateName(s?.toString()?.trim() ?: "")
                binding.tvNameError.text = error ?: ""
                binding.tvNameError.visibility = if (error != null) View.VISIBLE else View.GONE
            }
        })
    }

    private fun validateName(name: String): String? {
        return when {
            name.length < 1 -> appString(R.string.ui_ad_bos_birakilamaz)
            name.length > 25 -> "Ad en fazla 25 karakter olabilir."
            else -> null
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        binding.etEmail.setText(user.email ?: "")

        // Google ile giriş yapılmış ve parola sağlayıcısı eklenmemişse şifre değiştirilemez
        val providers = user.providerData.map { it.providerId }
        val hasPassword = providers.contains("password")
        val hasGoogle = providers.contains("google.com")
        val providerAllowsPasswordChange = hasPassword || !hasGoogle

        val uid = user.uid
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val name = doc.getString("name") ?: ""
                    val userId = doc.getString("userId") ?: ""
                    val role = doc.getString("role") ?: ""
                    binding.etName.setText(name)
                    binding.etUsername.setText("@$userId")

                    // Görünürlük tek bir yerde, provider ve rol bilgisi birlikte değerlendirilerek karara bağlanıyor
                    val showPasswordField = providerAllowsPasswordChange && role != "STUDENT"
                    binding.passwordContainer.visibility = if (showPasswordField) View.VISIBLE else View.GONE
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    binding.passwordContainer.visibility = if (providerAllowsPasswordChange) View.VISIBLE else View.GONE
                }
            }
    }

    private fun saveChanges() {
        val user = auth.currentUser ?: return
        val newName = binding.etName.text.toString().trim()
        val newPassword = binding.etPassword.text.toString()

        val nameError = validateName(newName)
        if (nameError != null) {
            Toast.makeText(requireContext(), nameError, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = appString(R.string.ui_kaydediliyor)

        // İsim güncellemesi
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Firestore'da ismi güncelle
                firestore.collection("users").document(user.uid)
                    .update("name", newName)
                    .addOnSuccessListener {
                        // Eğer şifre de girildiyse şifreyi güncelle
                        if (newPassword.isNotEmpty() && binding.passwordContainer.visibility == View.VISIBLE) {
                            updatePassword(newPassword)
                        } else {
                            finishSaving()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), appString(R.string.ui_firestore_guncellenemedi), Toast.LENGTH_SHORT).show()
                        resetSaveButton()
                    }
            } else {
                Toast.makeText(requireContext(), appString(R.string.ui_auth_profil_guncellenemedi), Toast.LENGTH_SHORT).show()
                resetSaveButton()
            }
        }
    }

    private fun updatePassword(newPassword: String) {
        val user = auth.currentUser ?: return
        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), appString(R.string.ui_parola_en_az_6_karakter_olmalidir), Toast.LENGTH_SHORT).show()
            resetSaveButton()
            return
        }

        user.updatePassword(newPassword).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), appString(R.string.ui_parola_basariyla_guncellendi), Toast.LENGTH_SHORT).show()
                binding.etPassword.text?.clear()
                finishSaving()
            } else {
                val exception = task.exception
                if (exception is FirebaseAuthRecentLoginRequiredException) {
                    Toast.makeText(requireContext(), appString(R.string.ui_guvenlik_nedeniyle_parolayi_degistirmek_icin), Toast.LENGTH_LONG).show()
                    logoutAndRedirect()
                } else {
                    Toast.makeText(requireContext(), appString(R.string.ui_parola_guncellenemedi_x, exception?.message), Toast.LENGTH_SHORT).show()
                    resetSaveButton()
                }
            }
        }
    }

    private fun finishSaving() {
        if (!isAdded) return
        Toast.makeText(requireContext(), appString(R.string.ui_degisiklikler_kaydedildi), Toast.LENGTH_SHORT).show()
        resetSaveButton()
        parentFragmentManager.popBackStack()
    }

    private fun resetSaveButton() {
        if (!isAdded) return
        binding.btnSave.isEnabled = true
        binding.btnSave.text = appString(R.string.ui_degisiklikleri_kaydet)
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(appString(R.string.ui_hesabi_sil))
            .setMessage(appString(R.string.ui_hesabinizi_kalici_olarak_silmek_istediginize))
            .setPositiveButton(appString(R.string.ui_sil)) { _, _ -> deleteAccount() }
            .setNegativeButton(appString(R.string.ui_iptal), null)
            .show()
    }

    private fun deleteAccount() {
        val progressDialog = android.app.ProgressDialog(requireContext()).apply {
            setMessage(appString(R.string.ui_hesabiniz_ve_iliskili_verileriniz_siliniyor))
            setCancelable(false)
            show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AccountDeletionHelper.performCompleteAccountDeletion(
                auth = auth,
                firestore = firestore,
                onSuccess = {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), appString(R.string.ui_hesabiniz_basariyla_silindi), Toast.LENGTH_SHORT).show()
                    logoutAndRedirect()
                },
                onRequiresRecentLogin = {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), appString(R.string.ui_guvenlik_nedeniyle_hesabi_silmek_icin_lutfen), Toast.LENGTH_LONG).show()
                    logoutAndRedirect()
                },
                onFailure = { e ->
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), appString(R.string.ui_hata_x, e.message), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun logoutAndRedirect() {
        // AuthManager metodlarını kullanmak veya direkt çıkış
        auth.signOut()
        val intent = Intent(requireContext(), LoginStartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
