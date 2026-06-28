package com.example.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentFeedbackBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackFragment : Fragment() {

    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Önceki ekrandan açık kalan bir klavye varsa kapat
        view.post {
            try {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            } catch (e: Exception) {}
        }

        binding.closeButton.setOnClickListener {
            closeFragment()
        }

        binding.sendFeedbackButton.setOnClickListener {
            if (!canSendFeedback()) {
                Toast.makeText(requireContext(), "Son 1 saat içinde çok fazla bildirim gönderdiniz. Lütfen daha sonra tekrar deneyin.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val feedbackText = binding.feedbackInput.text.toString().trim()
            if (feedbackText.isNotEmpty()) {
                sendFeedbackToFirestore(feedbackText)
            }
        }

        // EditText doluysa gönder butonunu aktif et
        binding.feedbackInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.sendFeedbackButton.isEnabled = !s.isNullOrBlank()
            }
        })
    }

    private fun sendFeedbackToFirestore(message: String) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val user = auth.currentUser

        binding.sendBlockingOverlay.visibility = View.VISIBLE

        val feedbackData = hashMapOf(
            "userId" to (user?.uid ?: "anonymous"),
            "email" to (user?.email ?: "No Email"),
            "message" to message,
            "createdAt" to FieldValue.serverTimestamp(),
            "appVersion" to getAppVersion(),
            "androidVersion" to Build.VERSION.RELEASE,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}"
        )

        db.collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                binding.sendBlockingOverlay.visibility = View.GONE
                recordFeedbackTimestamp()
                Toast.makeText(requireContext(), "Görüşleriniz başarıyla iletildi. Teşekkürler!", Toast.LENGTH_LONG).show()
                closeFragment()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.sendBlockingOverlay.visibility = View.GONE
                Toast.makeText(requireContext(), "Gönderilemedi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun closeFragment() {
        // Klavyeyi kapat
        try {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
        } catch (e: Exception) {}

        val main = activity as? MainActivity
        if (main != null) {
            main.finishTasksOverlayAnimated("FeedbackFragment.close")
        } else {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_right)
                .remove(this)
                .commit()
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun canSendFeedback(): Boolean {
        val prefs = requireContext().getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE)
        val savedTimestamps = prefs.getString("timestamps", "") ?: ""
        
        val currentTime = System.currentTimeMillis()
        val oneHourMs = 60 * 60 * 1000L

        // Kayıtlı zamanları Long listesine çevir
        val timestamps = if (savedTimestamps.isEmpty()) {
            mutableListOf<Long>()
        } else {
            savedTimestamps.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        }

        // 1 saatten eski olanları temizle
        timestamps.removeAll { currentTime - it > oneHourMs }

        // Temizlenmiş listeyi geri kaydet (gereksiz şişmeyi önler)
        prefs.edit().putString("timestamps", timestamps.joinToString(",")).apply()

        // Eğer son 1 saatte 5 veya daha fazla gönderim yapıldıysa izin verme
        return timestamps.size < 5
    }

    private fun recordFeedbackTimestamp() {
        val prefs = requireContext().getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE)
        val savedTimestamps = prefs.getString("timestamps", "") ?: ""
        val currentTime = System.currentTimeMillis()

        val timestamps = if (savedTimestamps.isEmpty()) {
            mutableListOf<Long>()
        } else {
            savedTimestamps.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        }
        
        timestamps.add(currentTime)
        prefs.edit().putString("timestamps", timestamps.joinToString(",")).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
