package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AppRatingManager {
    private const val PREFS_NAME = "AppRatingPrefs"
    
    private var lastPromptAttemptTime = 0L

    private fun getHasRatedKey(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        return "has_rated_$uid"
    }

    fun checkAndShowRatingPrompt(activity: FragmentActivity, completedLessonCount: Int): Boolean {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 1. If already rated by this account, never show again
        if (prefs.getBoolean(getHasRatedKey(), false)) {
            return false
        }

        // 2. Check if enough lessons are completed
        // Trigger condition: User has completed at least 1 lesson (for testing)
        if (completedLessonCount < 1) {
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastPromptAttemptTime < 2000L) {
            return true // Zaten son 2 saniye içinde gösterme tetiklendi
        }
        lastPromptAttemptTime = now

        // Check if AdSkipFragment is currently showing
        if (activity.supportFragmentManager.findFragmentByTag("AdSkip") != null) {
            return false
        }

        // Check if TasksFragment redirection (Cup Path) is pending
        if (GlobalValues.pendingCupPathRevealPartId != null) {
            return false
        }

        // Check if it's already shown to prevent duplicate dialogs
        if (activity.supportFragmentManager.findFragmentByTag("RatingDialog") != null) {
            return true
        }

        // If all checks pass, show the custom rating dialog
        showCustomRatingDialog(activity, prefs)
        return true
    }

    private fun showCustomRatingDialog(activity: FragmentActivity, prefs: SharedPreferences) {
        val dialog = RatingDialogFragment()
        dialog.show(activity.supportFragmentManager, "RatingDialog")
    }

    fun markAsRated(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(getHasRatedKey(), true).apply()
    }
}

class RatingDialogFragment : DialogFragment() {

    private var selectedRating = 0
    private lateinit var stars: List<ImageView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.dialog_rating, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stars = listOf(
            view.findViewById(R.id.star1),
            view.findViewById(R.id.star2),
            view.findViewById(R.id.star3),
            view.findViewById(R.id.star4),
            view.findViewById(R.id.star5)
        )

        val llFeedback = view.findViewById<LinearLayout>(R.id.llFeedback)
        val tvClose = view.findViewById<TextView>(R.id.tvClose)
        val tvNoThanks = view.findViewById<TextView>(R.id.tvNoThanks)
        val btnSubmitFeedback = view.findViewById<Button>(R.id.btnSubmitFeedback)
        val etFeedback = view.findViewById<EditText>(R.id.etFeedback)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)

        tvClose.setOnClickListener {
            dismiss()
        }
        
        tvNoThanks.setOnClickListener {
            AppRatingManager.markAsRated(requireContext())
            dismiss()
        }

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedRating = index + 1
                updateStars(selectedRating)

                // Give it a tiny delay to show the star animation/color
                it.postDelayed({
                    if (selectedRating >= 4) {
                        // Launch Google Play API
                        launchGooglePlayReview()
                        dismiss()
                    } else {
                        // Ask for feedback
                        tvTitle.text = "Üzgünüz \uD83D\uDE14"
                        tvSubtitle.text = "Deneyimini geliştirmek için geri bildirimine ihtiyacımız var."
                        llFeedback.visibility = View.VISIBLE
                    }
                }, 300)
            }
        }

        btnSubmitFeedback.setOnClickListener {
            val feedbackText = etFeedback.text.toString().trim()
            if (feedbackText.isNotEmpty()) {
                submitFeedbackToFirestore(feedbackText, selectedRating)
                Toast.makeText(context, "Geri bildiriminiz için teşekkürler!", Toast.LENGTH_SHORT).show()
                AppRatingManager.markAsRated(requireContext())
                dismiss()
            } else {
                Toast.makeText(context, "Lütfen bir şeyler yazın.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStars(rating: Int) {
        for (i in stars.indices) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.star_on_ic)
            } else {
                stars[i].setImageResource(R.drawable.star_off_ic)
            }
        }
    }

    private fun launchGooglePlayReview() {
        val currentActivity = activity ?: return
        val currentContext = context ?: return

        val manager = ReviewManagerFactory.create(currentContext)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(currentActivity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished.
                    AppRatingManager.markAsRated(currentContext)
                }
            }
        }
    }

    private fun submitFeedbackToFirestore(feedback: String, stars: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val feedbackData = hashMapOf(
            "uid" to uid,
            "stars" to stars,
            "feedback" to feedback,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        android.util.Log.d("AppRatingManager", "Submitting feedback for UID: $uid")
        FirebaseFirestore.getInstance().collection("ratingFeedback")
            .document(uid).set(feedbackData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("AppRatingManager", "Feedback successfully written to Firestore!")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AppRatingManager", "Error writing feedback to Firestore", e)
            }
    }
}
