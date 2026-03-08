package com.example.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.Timestamp

/**
 * Handles FCM messages: shows notification with app icon, sender name (title), message preview (body).
 * Saves FCM token to Firestore on token refresh so Cloud Functions can send to the device.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: from=${remoteMessage.from}, data=${remoteMessage.data}")
        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.d(TAG, "onMessageReceived: empty data, skipping")
            return
        }

        val title = data["title"] ?: data["senderName"] ?: getString(R.string.app_name)
        val body = data["body"] ?: data["messagePreview"] ?: ""
        val questionId = data["questionId"] ?: run {
            Log.w(TAG, "onMessageReceived: missing questionId in data")
            return
        }

        if (shouldSkipNotification(questionId)) {
            Log.d(TAG, "onMessageReceived: skipping (user already on this chat)")
            return
        }

        showNotification(questionId, title, body)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: token length=${token.length}")
        saveFcmTokenToFirestore(token)
    }

    private fun shouldSkipNotification(questionId: String): Boolean {
        val activity = com.example.app.MainActivity.currentActivity ?: return false
        val frag = activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        if (frag is QuestionChatFragment && frag.getQuestionIdOrNull() == questionId) return true
        return false
    }

    private fun showNotification(questionId: String, title: String, body: String) {
        val channelId = CHANNEL_ID_MESSAGES
        createChannelIfNeeded(channelId)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_QUESTION_ID, questionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            questionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(questionId.hashCode() and 0x7FFFFFFF, notification)
    }

    private fun createChannelIfNeeded(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notification_channel_messages_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_messages_desc)
            setShowBadge(true)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun saveFcmTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdatedAt" to Timestamp.now()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { Log.d(TAG, "FCM token saved to Firestore") }
            .addOnFailureListener { e -> Log.w(TAG, "Failed to save FCM token", e) }
    }

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID_MESSAGES = "messages"

        /**
         * Call from app to persist current FCM token (e.g. after login or app start).
         */
        @JvmStatic
        fun saveCurrentTokenToFirestore() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Log.d(TAG, "saveCurrentTokenToFirestore: not logged in, skip")
                return
            }
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "FCM token fetch failed (emulator needs Google Play image)", task.exception)
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    if (token.isNullOrEmpty()) {
                        Log.w(TAG, "FCM token is null/empty")
                        return@addOnCompleteListener
                    }
                    Log.d(TAG, "FCM token obtained, saving to Firestore for uid=$uid")
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(
                            mapOf(
                                "fcmToken" to token,
                                "fcmTokenUpdatedAt" to Timestamp.now()
                            ),
                            SetOptions.merge()
                        )
                        .addOnSuccessListener { Log.d(TAG, "FCM token saved to Firestore (uid=$uid)") }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to save FCM token to Firestore", e)
                        }
                }
        }
    }
}

