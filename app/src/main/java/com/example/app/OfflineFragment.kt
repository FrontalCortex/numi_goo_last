package com.example.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OfflineFragment : Fragment(R.layout.fragment_offline) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val retryButton = view.findViewById<Button>(R.id.offlineRetryButton)
        val messageText = view.findViewById<TextView>(R.id.offlineMessageText)

        retryButton.setOnClickListener {
            if (requireContext().isOnline()) {
                // Önce aktiviteye haber ver (MainActivity özel davranış uygulayabilir).
                (activity as? MainActivity)?.handleOfflineRetry()
                    ?: parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Hâlâ çevrimdışısınız. Lütfen bağlantınızı kontrol edin.",
                    Toast.LENGTH_SHORT
                ).show()
                messageText.text = getString(R.string.offline_message)
            }
        }
    }
}

