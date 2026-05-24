package com.example.app

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Harita, tutorial, ders abaküsü ve practice için ortak askQuestion butonu:
 * ban/onay durumunda buton görünür kalır; tıklamada uyarı veya soru akışı.
 */
object AskQuestionButtonBinder {

    fun bind(
        fragment: Fragment,
        button: View,
        isTeacher: Boolean,
        onAllowedClick: () -> Unit,
        onVisibleChanged: ((visible: Boolean) -> Unit)? = null,
        onReadyForBounce: (() -> Unit)? = null,
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            button.visibility = View.GONE
            onVisibleChanged?.invoke(false)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!fragment.isAdded) return@addOnSuccessListener
                applyVisible(button, onVisibleChanged, onReadyForBounce)
                button.setOnClickListener {
                    SessionDeviceManager.requireLoggedInAndSingleDevice(fragment) {
                        handleClick(fragment, doc, isTeacher, onAllowedClick)
                    }
                }
            }
            .addOnFailureListener {
                if (!fragment.isAdded) return@addOnFailureListener
                applyVisible(button, onVisibleChanged, onReadyForBounce)
                button.setOnClickListener {
                    SessionDeviceManager.requireLoggedInAndSingleDevice(fragment) {
                        val main = fragment.activity as? MainActivity
                        if (main?.isQuestionRecordingInProgress() == true) return@requireLoggedInAndSingleDevice
                        onAllowedClick()
                    }
                }
            }
    }

    private fun applyVisible(
        button: View,
        onVisibleChanged: ((Boolean) -> Unit)?,
        onReadyForBounce: (() -> Unit)?,
    ) {
        button.visibility = View.VISIBLE
        onVisibleChanged?.invoke(true)
        onReadyForBounce?.invoke()
    }

    private fun handleClick(
        fragment: Fragment,
        doc: DocumentSnapshot,
        isTeacher: Boolean,
        onAllowedClick: () -> Unit,
    ) {
        when {
            UserAskQuestionRestriction.isRestricted(doc) -> {
                showMessage(fragment, R.string.ask_question_account_restricted)
            }
            isTeacher && doc.getBoolean("teacherApproved") != true -> {
                showMessage(fragment, R.string.ask_question_teacher_not_approved)
            }
            else -> {
                val main = fragment.activity as? MainActivity
                if (main?.isQuestionRecordingInProgress() == true) return
                onAllowedClick()
            }
        }
    }

    private fun showMessage(fragment: Fragment, messageResId: Int) {
        AlertDialog.Builder(fragment.requireContext())
            .setMessage(messageResId)
            .setPositiveButton(R.string.ask_question_alert_ok, null)
            .show()
    }
}
