package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AvatarPickerFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AvatarPickerFragment"
        const val REQUEST_KEY = "avatar_picker_request"
        const val KEY_AVATAR_INDEX = "avatar_index"
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_avatar_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAvatarCards(view)
    }

    private fun setupAvatarCards(view: View) {
        val cardIds = listOf(
            R.id.avatarCard1,
            R.id.avatarCard2,
            R.id.avatarCard3,
            R.id.avatarCard4,
            R.id.avatarCard5,
            R.id.avatarCard6,
            R.id.avatarCard7,
            R.id.avatarCard8,
            R.id.avatarCard9,
            R.id.avatarCard10,
            R.id.avatarCard11,
            R.id.avatarCard12,
        )

        cardIds.forEachIndexed { index, cardId ->
            val avatarIndex = index + 1  // 1..12
            val card = view.findViewById<CardView>(cardId)
            card.setOnClickListener {
                onAvatarSelected(avatarIndex)
            }
        }
    }

    private fun onAvatarSelected(avatarIndex: Int) {
        val currentUser = auth.currentUser ?: run {
            dismiss()
            return
        }

        // Firestore'a kaydet
        firestore.collection("users").document(currentUser.uid)
            .update("selectedAvatar", avatarIndex)
            .addOnSuccessListener {
                // ProfileFragment'e sonucu ilet
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(KEY_AVATAR_INDEX to avatarIndex)
                )
                dismiss()
            }
            .addOnFailureListener {
                // Firestore hatası olsa bile UI'ı güncelle, kayıt başarısız olabilir
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(KEY_AVATAR_INDEX to avatarIndex)
                )
                dismiss()
            }
    }
}
