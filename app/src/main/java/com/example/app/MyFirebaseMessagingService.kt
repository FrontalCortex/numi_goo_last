package com.example.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.Timestamp
import java.util.UUID

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
        val messageId = data["messageId"]
        val recipientUid = data["recipientUid"] ?: run {
            Log.w(TAG, "onMessageReceived: missing recipientUid in data")
            return
        }

        // Ek güvenlik: cihazda şu an hangi hesapla oturum açık?
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            Log.d(TAG, "onMessageReceived: no logged-in user, skipping notification")
            return
        }
        if (currentUid != recipientUid) {
            Log.d(
                TAG,
                "onMessageReceived: currentUid($currentUid) != recipientUid($recipientUid), skipping notification"
            )
            return
        }

        if (shouldSkipNotification(questionId)) {
            Log.d(TAG, "onMessageReceived: skipping (user already on this chat)")
            return
        }

        showNotification(questionId, messageId, recipientUid, title, body)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: token length=${token.length}")
        saveFcmTokenToFirestore(token)
    }

    private fun shouldSkipNotification(questionId: String): Boolean {
        val activity = com.example.app.MainActivity.currentActivity ?: return false
        val frag = activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerID)
        // Sadece ŞU an açık olan sohbetin mesajlarını bastır.
        if (frag is QuestionChatFragment && frag.getQuestionIdOrNull() == questionId) {
            return true
        }
        return false
    }

    private fun showNotification(
        questionId: String,
        messageId: String?,
        recipientUid: String,
        title: String,
        body: String
    ) {
        val channelId = CHANNEL_ID_MESSAGES
        createChannelIfNeeded(channelId)

        // Her mesaj için benzersiz bir notificationId üret (messageId varsa onu kullan).
        val rawId = messageId ?: "$questionId-${System.currentTimeMillis()}"
        val notificationId = rawId.hashCode() and 0x7FFFFFFF

        // Bildirimden her zaman doğrudan MainActivity'e git ve questionId'yi ilet.
        // MainActivity, EXTRA_OPEN_QUESTION_ID ile gelen durumlarda ilgili sohbet fragment'ını açıyor.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_QUESTION_ID, questionId)
            putExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID, recipientUid)
            Log.d(TAG, "Building notification intent with questionId=$questionId")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
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
        nm.notify(notificationId, notification)
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
        val deviceId = getOrCreateDeviceId()
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        userRef.get()
            .addOnSuccessListener { snap ->
                val snapshotData = snap.data
                val email = snapshotData?.get("email") as? String
                val role = snapshotData?.get("role") as? String
                if (snapshotData == null || email.isNullOrBlank() || role.isNullOrBlank()) {
                    Log.d(TAG, "saveFcmTokenToFirestore: user profile incomplete, skip saving token (uid=$uid)")
                    return@addOnSuccessListener
                }
                val existingDevices = (snap.get("fcmDevices") as? List<*>)?.mapNotNull { it as? Map<*, *> }
                    ?.toMutableList() ?: mutableListOf()

                // Aynı deviceId'ye ait eski kayıtları kaldır.
                val filtered = existingDevices.filterNot { it["deviceId"] == deviceId }.toMutableList()

                // Bu cihaz için yeni/ güncel kayıt ekle.
                filtered.add(
                    mapOf(
                        "deviceId" to deviceId,
                        "token" to token,
                        "updatedAt" to Timestamp.now()
                    )
                )

                // En fazla 2 cihaz: son 2 kaydı tut.
                val trimmed = if (filtered.size > 2) filtered.takeLast(2) else filtered
                val lastToken = trimmed.lastOrNull()?.get("token") as? String

                val updateMap = mutableMapOf<String, Any?>(
                    "fcmDevices" to trimmed,
                    "fcmTokenUpdatedAt" to Timestamp.now()
                )
                updateMap["fcmToken"] = lastToken

                userRef.set(updateMap, SetOptions.merge())
                    .addOnSuccessListener { Log.d(TAG, "FCM device tokens saved to Firestore") }
                    .addOnFailureListener { e -> Log.w(TAG, "Failed to save FCM device tokens", e) }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to read user doc for saving FCM device tokens", e)
            }
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
            val deviceId = getOrCreateDeviceId()
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
                    Log.d(TAG, "FCM token obtained, saving to Firestore for uid=$uid, deviceId=$deviceId")
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                    userRef.get()
                        .addOnSuccessListener { snap ->
                            val snapshotData = snap.data
                            val email = snapshotData?.get("email") as? String
                            val role = snapshotData?.get("role") as? String
                            if (snapshotData == null || email.isNullOrBlank() || role.isNullOrBlank()) {
                                Log.d(TAG, "saveCurrentTokenToFirestore: user profile incomplete, skip saving token (uid=$uid)")
                                return@addOnSuccessListener
                            }
                            val existingDevices =
                                (snap.get("fcmDevices") as? List<*>)?.mapNotNull { it as? Map<*, *> }
                                    ?.toMutableList() ?: mutableListOf()
                            val filtered = existingDevices.filterNot { it["deviceId"] == deviceId }.toMutableList()
                            filtered.add(
                                mapOf(
                                    "deviceId" to deviceId,
                                    "token" to token,
                                    "updatedAt" to Timestamp.now()
                                )
                            )
                            val trimmed = if (filtered.size > 2) filtered.takeLast(2) else filtered
                            val lastToken = trimmed.lastOrNull()?.get("token") as? String
                            val updateMap = mutableMapOf<String, Any?>(
                                "fcmDevices" to trimmed,
                                "fcmTokenUpdatedAt" to Timestamp.now()
                            )
                            updateMap["fcmToken"] = lastToken
                            userRef.set(updateMap, SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d(TAG, "FCM device tokens saved to Firestore (uid=$uid)")
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Failed to save FCM device tokens to Firestore", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to read user doc for saving FCM device tokens", e)
                        }
                }
        }

        @JvmStatic
        fun clearCurrentTokenFromFirestore() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val deviceId = getOrCreateDeviceId()
            val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            userRef.get()
                .addOnSuccessListener { snap ->
                    val existingDevices =
                        (snap.get("fcmDevices") as? List<*>)?.mapNotNull { it as? Map<*, *> }
                            ?: emptyList()
                    val filtered = existingDevices.filter { it["deviceId"] != deviceId }
                    val lastToken = (filtered.lastOrNull()?.get("token") as? String)
                    val data = mutableMapOf<String, Any?>(
                        "fcmDevices" to filtered,
                        "fcmTokenUpdatedAt" to Timestamp.now()
                    )
                    data["fcmToken"] = lastToken
                    userRef.set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM device entry removed for this device (uid=$uid, deviceId=$deviceId)")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to update FCM devices in Firestore", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to read user doc for clearing FCM device tokens", e)
                }
        }

        private fun getOrCreateDeviceId(): String {
            val appContext = try {
                FirebaseApp.getInstance().applicationContext
            } catch (e: IllegalStateException) {
                // FirebaseApp henüz initialize edilmediyse, burada bir fallback deneyebiliriz.
                return "unknown-device"
            }
            val prefs = appContext.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
            var id = prefs.getString("device_id", null)
            if (id.isNullOrEmpty()) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("device_id", id).apply()
            }
            return id
        }
    }
}

