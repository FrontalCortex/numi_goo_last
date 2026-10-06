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

    /**
     * Gelen her bildirim buradan geçiyor.
     *
     * Sunucu data-only gönderiyor (bkz. functions/index.js → sendUserNotification), yani
     * gösterme kararının son adımı burada: bildirim başka bir hesabın cihazına düşmemeli ve
     * kullanıcının kapattığı tür gösterilmemeli. Sunucu da aynı kontrolleri yapıyor; buradaki
     * tekrar gereksiz değil, çünkü tercih değişikliği Firestore'a ulaşmadan önce gönderilmiş
     * bir bildirim hâlâ yolda olabilir.
     *
     * TÜRÜ BULMA
     *   Güncel sunucu `notifyType` gönderiyor. Eski alanlara düşme yolu korunuyor çünkü
     *   kuyrukta bekleyen bir bildirim eski biçimde gelebilir: `type == "streak_reminder"`
     *   seri hatırlatması, `questionId` varsa sohbet.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: from=${remoteMessage.from}, data=${remoteMessage.data}")
        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.d(TAG, "onMessageReceived: empty data, skipping")
            return
        }

        val title = data["title"] ?: data["senderName"] ?: getString(R.string.app_name)
        val body = data["body"] ?: data["messagePreview"] ?: ""
        val recipientUid = data["recipientUid"]

        val type = data["notifyType"] ?: when {
            data["type"] == TYPE_STREAK_REMINDER -> NotificationPrefs.STREAK
            data["questionId"] != null -> NotificationPrefs.CHAT
            else -> null
        }
        if (type == null) {
            Log.w(TAG, "onMessageReceived: tür belirlenemedi, atlanıyor")
            return
        }

        // Bildirim yalnızca alıcının oturum açtığı cihazda gösterilir. Aile içinde cihaz
        // paylaşılıyor olabilir; başka bir çocuğun öğretmen sohbeti görünmemeli.
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null || currentUid != recipientUid) {
            Log.d(TAG, "onMessageReceived: alıcı bu cihazdaki hesap değil ($type), atlanıyor")
            return
        }

        // `account` türü uygulama içinden kapatılamıyor (bkz. NotificationPrefs.ALL); onun
        // dışındaki her tür kullanıcının tercihine tabi.
        if (type in NotificationPrefs.ALL && !NotificationPrefs.isEnabled(this, type)) {
            Log.d(TAG, "onMessageReceived: $type kullanıcı tarafından kapatılmış, atlanıyor")
            return
        }

        // Sunucu hangi kanalı istediğini söylüyor: sessiz saatte sohbet bildirimi sessiz
        // varyanta düşüyor ve o karar sunucuda veriliyor (kullanıcının saat dilimi orada).
        val channelId = data["channel"] ?: defaultChannelFor(type)
        // Açılma ölçümünün etiketi; sunucudaki defterle aynı anahtar.
        val topic = data["topic"] ?: type

        if (type == NotificationPrefs.CHAT) {
            val questionId = data["questionId"] ?: run {
                Log.w(TAG, "onMessageReceived: sohbet bildiriminde questionId yok")
                return
            }
            if (shouldSkipNotification(questionId)) {
                Log.d(TAG, "onMessageReceived: skipping (user already on this chat)")
                return
            }
            showNotification(questionId, data["messageId"], recipientUid, title, body, channelId, topic)
            return
        }

        showSimpleNotification(type, topic, channelId, title, body)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: token length=${token.length}")
        saveFcmTokenToFirestore(token)
    }

    /**
     * Soru başlığı (questionId) altında gelen bildirimleri kalıcı olarak saklamak için kullanılan SharedPreferences.
     * Her questionId için:
     * {
     *   "title": "...",
     *   "messages": ["...", "...", ...]
     * }
     */
    private fun appendMessageToThread(
        questionId: String,
        incomingTitle: String,
        body: String
    ): Pair<String, List<String>> {
        val prefs = getSharedPreferences(PREFS_NAME_THREADS, Context.MODE_PRIVATE)
        val existing = prefs.getString(questionId, null)

        var title = incomingTitle
        val messages = mutableListOf<String>()

        if (existing != null) {
            try {
                val obj = org.json.JSONObject(existing)
                val storedTitle = obj.optString("title")
                if (!storedTitle.isNullOrBlank()) {
                    title = storedTitle
                }
                val arr = obj.optJSONArray("messages")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val msg = arr.optString(i)
                        if (!msg.isNullOrBlank()) {
                            messages.add(msg)
                        }
                    }
                }
            } catch (_: Exception) {
                // Eski / bozuk veri okunamazsa, sessizce sıfırdan başla.
            }
        }

        if (body.isNotBlank()) {
            messages.add(body)
        }

        // Sadece son N mesajı tut.
        val maxLines = MAX_INBOX_LINES
        val trimmed = if (messages.size > maxLines) {
            messages.takeLast(maxLines)
        } else {
            messages
        }

        try {
            val obj = org.json.JSONObject()
            obj.put("title", title)
            val arr = org.json.JSONArray()
            trimmed.forEach { arr.put(it) }
            obj.put("messages", arr)
            prefs.edit().putString(questionId, obj.toString()).apply()
        } catch (_: Exception) {
            // Yazarken hata olursa, bildirim yine de gösterilecek; sadece kalıcılık kaybolur.
        }

        return title to trimmed
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
        body: String,
        channelId: String,
        topic: String
    ) {
        createChannelIfNeeded(channelId)

        // Aynı soru (questionId) için tek bir bildirim ID'si kullan:
        // Böylece aynı başlık altındaki tüm mesajlar tek bildirimde toplanır.
        val rawId = questionId
        val notificationId = rawId.hashCode() and 0x7FFFFFFF

        // Başlık altındaki mesajları kalıcı olarak sakla (maximum N satır).
        val (finalTitle, lines) = appendMessageToThread(questionId, title, body)
        val latestBody = lines.lastOrNull() ?: body

        // Bildirimden her zaman doğrudan MainActivity'e git ve questionId'yi ilet.
        // MainActivity, EXTRA_OPEN_QUESTION_ID ile gelen durumlarda ilgili sohbet fragment'ını açıyor.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_QUESTION_ID, questionId)
            putExtra(MainActivity.EXTRA_NOTIFICATION_RECIPIENT_UID, recipientUid)
            putExtra(MainActivity.EXTRA_NOTIFICATION_TOPIC, topic)
            Log.d(TAG, "Building notification intent with questionId=$questionId")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Birden fazla okunmamış mesaj varsa, başlıkta sayıyı da göster.
        val displayTitle = if (lines.size > 1) {
            "$finalTitle (${lines.size} yeni mesaj)"
        } else {
            finalTitle
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(latestBody)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        // Sadece bir mesaj varsa normal tek satırlı bildirim göster
        // Birden fazla mesaj varsa InboxStyle ile satır satır göster
        if (lines.size > 1) {

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(displayTitle)
            lines.forEach { line ->
                inboxStyle.addLine(line)
            }
            builder.setStyle(inboxStyle)
        }

        val notification = builder.build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    /**
     * Sohbet dışındaki bütün bildirimler: seri hatırlatması, ödüller, hesap/ödeme.
     *
     * Sohbet yolundan ayrı çünkü ihtiyaçları farklı: biriktirme (InboxStyle) yok ve her
     * türün SABİT tek bildirim kimliği var — aynı türden ikinci bildirim birikmek yerine
     * birincinin yerini alıyor. Bildirim çekmecesinde üst üste yığılmak, tavan koymakla
     * önlemeye çalıştığımız şeyin kendisi.
     */
    private fun showSimpleNotification(
        type: String,
        topic: String,
        channelId: String,
        title: String,
        body: String
    ) {
        createChannelIfNeeded(channelId)
        val notificationId = notificationIdFor(type)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_TOPIC, topic)
            // Havuz bildirimi doğrudan Havuz sekmesini açıyor; öteki türlerde uygulamanın
            // normal açılışı doğru yer.
            if (type == TYPE_POOL) putExtra(MainActivity.EXTRA_OPEN_TEACHER_POOL, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, notification)
    }

    /**
     * Kanalı gerekiyorsa oluşturur.
     *
     * KANALLAR NEDEN AYRI
     *   Kullanıcı Android ayarlarından seri hatırlatmasını kapatırken öğretmen mesajlarını
     *   kapatmak zorunda kalmamalı. Uygulama içindeki tercihin yanında bu da duruyor, çünkü
     *   `account` türünü yalnızca buradan susturabiliyor.
     *
     *   `_quiet` varyantları ayrı bir kanal olmak ZORUNDA: Android'de bir kanalın önem
     *   derecesi oluşturulduktan sonra uygulama tarafından değiştirilemiyor, yani "gece
     *   gelirse sessiz olsun" tek kanalla kurulamıyor.
     */
    private fun createChannelIfNeeded(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val spec = CHANNELS[channelId] ?: CHANNELS.getValue(CHANNEL_ID_MESSAGES)
        val channel = NotificationChannel(channelId, getString(spec.nameRes), spec.importance).apply {
            description = getString(spec.descRes)
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

                // En fazla MAX_FCM_DEVICES cihaz: en son kaydedilenleri tut.
                val trimmed = if (filtered.size > MAX_FCM_DEVICES) filtered.takeLast(MAX_FCM_DEVICES) else filtered
                val lastToken = trimmed.lastOrNull()?.get("token") as? String

                val updateMap = mutableMapOf<String, Any?>(
                    "fcmDevices" to trimmed,
                    "fcmTokenUpdatedAt" to Timestamp.now(),
                    // Sessiz saatler sunucuda bu alana bakıyor (functions/notifications.js).
                    // Token ile birlikte yazılıyor: ikisi de "bu cihaz şu an burada" bilgisi
                    // ve ikisinin tazeliği de aynı anda gerekiyor.
                    "utcOffsetMinutes" to deviceUtcOffsetMinutes()
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

    /** Bir Android bildirim kanalının görünen adı, açıklaması ve önem derecesi. */
    private data class ChannelSpec(val nameRes: Int, val descRes: Int, val importance: Int)

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID_MESSAGES = "messages"
        const val CHANNEL_ID_MESSAGES_QUIET = "messages_quiet"
        const val CHANNEL_ID_STREAK = "streak_reminder"
        const val CHANNEL_ID_REWARDS = "rewards"
        const val CHANNEL_ID_ACCOUNT = "account"
        const val CHANNEL_ID_POOL = "pool"
        const val CHANNEL_ID_POOL_QUIET = "pool_quiet"

        /**
         * Kanal kataloğu. Sunucunun gönderdiği `channel` alanı buradaki anahtarlarla
         * eşleşiyor (functions/notifications.js → NOTIFICATION_TYPES).
         */
        private val CHANNELS = mapOf(
            CHANNEL_ID_MESSAGES to ChannelSpec(
                R.string.notification_channel_messages_name,
                R.string.notification_channel_messages_desc,
                NotificationManager.IMPORTANCE_HIGH,
            ),
            // Gece gelen sohbet mesajı: görünür ama ses çıkarmıyor. Çocuğun telefonunu
            // 03:00'te çaldırmak, bildirimin tamamen kapatılmasıyla sonuçlanan yol.
            CHANNEL_ID_MESSAGES_QUIET to ChannelSpec(
                R.string.notification_channel_messages_quiet_name,
                R.string.notification_channel_messages_quiet_desc,
                NotificationManager.IMPORTANCE_LOW,
            ),
            // Günlük hatırlatma, sohbet mesajı değil: sesle/titreşimle araya girmesin.
            CHANNEL_ID_STREAK to ChannelSpec(
                R.string.notification_channel_streak_name,
                R.string.notification_channel_streak_desc,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            CHANNEL_ID_REWARDS to ChannelSpec(
                R.string.notification_channel_rewards_name,
                R.string.notification_channel_rewards_desc,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            CHANNEL_ID_ACCOUNT to ChannelSpec(
                R.string.notification_channel_account_name,
                R.string.notification_channel_account_desc,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            // Öğretmenin işi: havuza düşen soru. Yüksek önem, çünkü cevaplanmayan soru
            // 48 saat sonra öğrenciye iade ediliyor.
            CHANNEL_ID_POOL to ChannelSpec(
                R.string.notification_channel_pool_name,
                R.string.notification_channel_pool_desc,
                NotificationManager.IMPORTANCE_HIGH,
            ),
            CHANNEL_ID_POOL_QUIET to ChannelSpec(
                R.string.notification_channel_pool_quiet_name,
                R.string.notification_channel_pool_quiet_desc,
                NotificationManager.IMPORTANCE_LOW,
            ),
        )

        /** Sunucudaki tür adı → o türün varsayılan kanalı (`channel` alanı gelmezse). */
        private fun defaultChannelFor(type: String): String = when (type) {
            NotificationPrefs.CHAT -> CHANNEL_ID_MESSAGES
            NotificationPrefs.STREAK -> CHANNEL_ID_STREAK
            NotificationPrefs.REWARD -> CHANNEL_ID_REWARDS
            TYPE_POOL -> CHANNEL_ID_POOL
            else -> CHANNEL_ID_ACCOUNT
        }

        /** Eski sürümlerin ayrıştırma anahtarı (functions/index.js: sendStreakReminders). */
        private const val TYPE_STREAK_REMINDER = "streak_reminder"

        /** Öğretmen havuzu bildirimi (functions/index.js: notifyTeacherPool). */
        private const val TYPE_POOL = "pool"

        /**
         * Tür başına sabit bildirim kimliği: aynı türden ikinci bildirim birikmek yerine
         * birincinin yerini alıyor.
         */
        private fun notificationIdFor(type: String): Int = when (type) {
            NotificationPrefs.STREAK -> 90_001
            NotificationPrefs.REWARD -> 90_002
            TYPE_POOL -> 90_004
            else -> 90_003
        }

        private const val PREFS_NAME_THREADS = "notification_threads"
        private const val MAX_INBOX_LINES = 7

        /**
         * Hesap başına kaç cihaz bildirim alır.
         *
         * İkizi sunucuda: functions/index.js → `FCM_MAX_DEVICES`. İkisi birlikte değişmeli;
         * sunucu daha azını okursa buraya kaydedilen cihaz sessizce bildirim almaz.
         *
         * 2'den 3'e çıkarıldı (05.10.2026): tipik kurulum ailede tablet + çocuğun telefonu +
         * ebeveyn telefonu ve 2 sınırıyla en eskisi listeden sessizce düşüyordu.
         */
        private const val MAX_FCM_DEVICES = 3

        /**
         * Cihazın UTC farkı (dakika). Türkiye için +180.
         *
         * İkizi StreakSyncService.utcOffsetMinutes — oradaki seri hatırlatma saatini, buradaki
         * sessiz saatleri besliyor. Yaz saati uygulayan yerlerde yılda iki kez değişiyor;
         * token her uygulama açılışında yazıldığı için kendiliğinden düzeliyor.
         */
        private fun deviceUtcOffsetMinutes(): Int =
            java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

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
                            val trimmed = if (filtered.size > MAX_FCM_DEVICES) filtered.takeLast(MAX_FCM_DEVICES) else filtered
                            val lastToken = trimmed.lastOrNull()?.get("token") as? String
                            val updateMap = mutableMapOf<String, Any?>(
                                "fcmDevices" to trimmed,
                                "fcmTokenUpdatedAt" to Timestamp.now(),
                                "utcOffsetMinutes" to deviceUtcOffsetMinutes()
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

        /**
         * Bir soruya ait birikmiş notification satırlarını temizler.
         * Kullanıcı ilgili sohbeti açtığında çağrılırsa, yeni bildirimler sadece o andan SONRA gelen mesajları gösterir.
         */
        @JvmStatic
        fun clearNotificationThread(context: Context, questionId: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME_THREADS, Context.MODE_PRIVATE)
            prefs.edit().remove(questionId).apply()
        }
    }
}


