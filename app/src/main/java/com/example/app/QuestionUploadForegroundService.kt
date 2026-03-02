package com.example.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.app.model.QuestionMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/**
 * Soru sohbetine mesaj/resim/video/ses gönderimini bildirimle (foreground) yapar.
 * Kota olmadan hızlı ve güvenilir yükleme.
 */
class QuestionUploadForegroundService : Service() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val queue = ConcurrentLinkedQueue<UploadItem>()
    private val executor = Executors.newSingleThreadExecutor()
    private var processing = false
    private val activeTasks = ConcurrentHashMap<String, UploadTask>()
    private val canceledClientIds = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var currentUploadClientId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        if (intent.action == ACTION_CANCEL_UPLOAD) {
            val clientId = intent.getStringExtra(KEY_CLIENT_ID)
            if (clientId != null) {
                // Cancel intent'i, upload intent'inden önce gelirse ilerideki enqueue'u da engelle.
                canceledClientIds.add(clientId)
                cancelUploadInternal(clientId)
            }
            return START_NOT_STICKY
        }

        val questionId = intent.getStringExtra(KEY_QUESTION_ID) ?: return START_NOT_STICKY
        val type = intent.getStringExtra(KEY_TYPE) ?: return START_NOT_STICKY
        val clientId = intent.getStringExtra(KEY_CLIENT_ID) ?: return START_NOT_STICKY

        // Kullanıcı "herkesten sil" diyerek iptal ettiyse (cancel intent'i daha önce geldiyse),
        // bu upload intent'ini kuyruğa hiç sokma.
        if (canceledClientIds.remove(clientId)) {
            intent.getStringExtra(KEY_FILE_PATH)?.let { path ->
                runCatching { File(path).delete() }
            }
            return START_NOT_STICKY
        }
        val senderUid = intent.getStringExtra(KEY_SENDER_UID) ?: return START_NOT_STICKY
        val senderRole = intent.getStringExtra(KEY_SENDER_ROLE) ?: return START_NOT_STICKY
        val item = UploadItem(
            questionId = questionId,
            type = type,
            clientId = clientId,
            senderUid = senderUid,
            senderRole = senderRole,
            filePath = intent.getStringExtra(KEY_FILE_PATH),
            textContent = intent.getStringExtra(KEY_TEXT_CONTENT),
            caption = intent.getStringExtra(KEY_CAPTION)
        )
        val wasIdle = !processing
        if (wasIdle) {
            processing = true
            startForeground(NOTIFICATION_ID, buildNotification(null, 0))
        }
        queue.add(item)
        if (wasIdle) executor.execute { processQueue() }
        return START_NOT_STICKY
    }

    private fun processQueue() {
        while (true) {
            val item = queue.poll() ?: break
            if (canceledClientIds.remove(item.clientId)) {
                item.filePath?.let { path -> runCatching { File(path).delete() } }
                continue
            }
            currentUploadClientId = item.clientId
            updateNotification(item.type, queue.size)
            runBlocking {
                runCatching {
                    when (item.type) {
                        QuestionMessage.TYPE_TEXT -> sendText(item)
                        QuestionMessage.TYPE_IMAGE -> sendImage(item)
                        QuestionMessage.TYPE_VIDEO -> sendVideo(item)
                        QuestionMessage.TYPE_AUDIO -> sendAudio(item)
                    }
                }
            }
        }
        processing = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        return when {
            bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun formatRemainingTime(remainingMs: Long): String {
        if (remainingMs < 0) return ""
        val sec = (remainingMs / 1000).toInt()
        val min = sec / 60
        val hour = min / 60
        return when {
            hour > 0 -> getString(R.string.notification_eta_hour, hour)
            min > 0 -> if (sec % 60 == 0) getString(R.string.notification_eta_min, min) else getString(
                R.string.notification_eta_min_sec,
                min,
                sec % 60
            )

            else -> getString(R.string.notification_eta_sec, sec.coerceAtLeast(1))
        }
    }

    private fun uploadFileWithProgress(
        ref: com.google.firebase.storage.StorageReference,
        file: File,
        item: UploadItem,
        typeLabel: String
    ): UploadTask {
        val clientId = item.clientId
        val startTime = System.currentTimeMillis()

        // Upload gerçekten başlarken, ilgili sohbet fragment'ına haber ver.
        val startedIntent = Intent(ACTION_UPLOAD_STARTED).apply {
            putExtra(KEY_QUESTION_ID, item.questionId)
            putExtra(KEY_TYPE, item.type)
            putExtra(KEY_CLIENT_ID, clientId)
            putExtra(KEY_SENDER_UID, item.senderUid)
            putExtra(KEY_SENDER_ROLE, item.senderRole)
            item.textContent?.let { putExtra(KEY_TEXT_CONTENT, it) }
            item.caption?.let { putExtra(KEY_CAPTION, it) }
        }
        sendBroadcast(startedIntent)

        // Global buffer'da da aktif upload'lar listesine ekle
        GlobalValues.activeUploadIdsByQuestion
            .getOrPut(item.questionId) { mutableSetOf() }
            .add(clientId)

        val task = ref.putFile(Uri.fromFile(file))
        activeTasks[clientId] = task
        currentUploadClientId = clientId
        task.addOnProgressListener { snapshot ->
            // Kullanıcı bu clientId için iptal verdiyse hayalet bildirim güncellemeyelim.
            if (canceledClientIds.contains(clientId)) return@addOnProgressListener
            val transferred = snapshot.bytesTransferred
            val total = snapshot.totalByteCount
            val elapsed = System.currentTimeMillis() - startTime
            val speed = if (elapsed > 500 && transferred > 0) transferred.toDouble() / elapsed else 0.0
            val remainingMs =
                if (speed > 0 && total > transferred) ((total - transferred) / speed).toLong() else -1L
            updateNotificationProgress(typeLabel, transferred, total, remainingMs, clientId)
        }.addOnCompleteListener {
            activeTasks.remove(clientId)
            if (currentUploadClientId == clientId) {
                currentUploadClientId = null
            }
            // Başarılı veya hatalı tamamlansa da global aktif listesinden çıkar
            val meta = GlobalValues.uploadMetaByClientId[clientId]
            meta?.let {
                GlobalValues.activeUploadIdsByQuestion[it.questionId]?.remove(clientId)
            }
        }
        return task
    }

    private suspend fun sendText(item: UploadItem) {
        val msg = hashMapOf(
            "senderUid" to item.senderUid,
            "senderRole" to item.senderRole,
            "type" to QuestionMessage.TYPE_TEXT,
            "textContent" to (item.textContent ?: ""),
            "createdAt" to Timestamp.now(),
            "clientId" to item.clientId
        )
        firestore.collection("questions").document(item.questionId).collection("messages").add(msg).await()
    }

    private fun cancelUploadInternal(clientId: String) {
        canceledClientIds.add(clientId)
        activeTasks[clientId]?.cancel()
        activeTasks.remove(clientId)
        queue.removeIf { it.clientId == clientId }

        // Global aktif listeden çıkar
        val meta = GlobalValues.uploadMetaByClientId[clientId]
        meta?.let {
            GlobalValues.activeUploadIdsByQuestion[it.questionId]?.remove(clientId)
        }

        // İptal edilen upload'ı sohbete bildirmek için broadcast gönder.
        val intent = Intent(ACTION_UPLOAD_CANCELED).apply {
            putExtra(KEY_CLIENT_ID, clientId)
        }
        sendBroadcast(intent)

        // Kuyrukta ve aktif görevlerde başka upload kalmadıysa bildirimi de kapat.
        if (queue.isEmpty() && activeTasks.isEmpty()) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
        }
    }

    private suspend fun sendImage(item: UploadItem) {
        val file = item.filePath?.let { File(it) }
        if (file == null || !file.exists()) return
        val ref = storage.child("question_media/${item.questionId}/img_${System.currentTimeMillis()}.jpg")
        uploadFileWithProgress(ref, file, item, getString(R.string.notification_upload_type_image)).await()
        val downloadUri = ref.downloadUrl.await()
        val msg = hashMapOf(
            "senderUid" to item.senderUid,
            "senderRole" to item.senderRole,
            "type" to QuestionMessage.TYPE_IMAGE,
            "mediaUrl" to downloadUri.toString(),
            "mediaStoragePath" to ref.path,
            "createdAt" to Timestamp.now(),
            "clientId" to item.clientId,
            "mediaSizeBytes" to file.length()
        )
        if (!item.caption.isNullOrBlank()) msg["textContent"] = item.caption
        firestore.collection("questions").document(item.questionId).collection("messages").add(msg).await()
        file.delete()
    }

    private suspend fun sendVideo(item: UploadItem) {
        val file = item.filePath?.let { File(it) }
        if (file == null || !file.exists()) return
        var thumbnailUrl: String? = null
        getVideoThumbnail(file)?.let { bitmap ->
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val thumbRef = storage.child("question_media/${item.questionId}/thumb_${System.currentTimeMillis()}.jpg")
            thumbRef.putBytes(baos.toByteArray()).await()
            thumbnailUrl = thumbRef.downloadUrl.await().toString()
        }
        val ref = storage.child("question_media/${item.questionId}/video_${System.currentTimeMillis()}.mp4")
        uploadFileWithProgress(ref, file, item, getString(R.string.notification_upload_type_video)).await()
        val downloadUri = ref.downloadUrl.await()
        val msg = hashMapOf(
            "senderUid" to item.senderUid,
            "senderRole" to item.senderRole,
            "type" to QuestionMessage.TYPE_VIDEO,
            "mediaUrl" to downloadUri.toString(),
            "mediaStoragePath" to ref.path,
            "createdAt" to Timestamp.now(),
            "clientId" to item.clientId,
            "mediaSizeBytes" to file.length()
        )
        val thumb = thumbnailUrl
        if (thumb != null) msg["thumbnailUrl"] = thumb
        if (!item.caption.isNullOrBlank()) msg["textContent"] = item.caption
        firestore.collection("questions").document(item.questionId).collection("messages").add(msg).await()
        file.delete()
    }

    private suspend fun sendAudio(item: UploadItem) {
        val file = item.filePath?.let { File(it) }
        if (file == null || !file.exists()) return
        val ref = storage.child("question_media/${item.questionId}/${file.name}")
        uploadFileWithProgress(ref, file, item, getString(R.string.notification_upload_type_audio)).await()
        val downloadUri = ref.downloadUrl.await()
        val msg = hashMapOf(
            "senderUid" to item.senderUid,
            "senderRole" to item.senderRole,
            "type" to QuestionMessage.TYPE_AUDIO,
            "mediaUrl" to downloadUri.toString(),
            "mediaStoragePath" to ref.path,
            "createdAt" to Timestamp.now(),
            "clientId" to item.clientId
        )
        firestore.collection("questions").document(item.questionId).collection("messages").add(msg).await()
        file.delete()
    }

    private fun getVideoThumbnail(file: File): Bitmap? {
        if (!file.exists()) return null
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            var b = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (b == null) b = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
            if (b == null) b = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
            retriever.release()
            b
        } catch (_: Exception) {
            null
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_upload),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
                description = getString(R.string.notification_channel_upload_desc)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(typeLabel: String?, remaining: Int): android.app.Notification {
        val contentText = when {
            typeLabel != null && remaining > 0 -> getString(R.string.notification_uploading_with_queue, typeLabel, remaining)
            typeLabel != null -> getString(R.string.notification_uploading, typeLabel)
            else -> getString(R.string.notification_uploading_generic)
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun buildNotificationWithProgress(
        typeLabel: String,
        bytesTransferred: Long,
        totalBytes: Long,
        remainingMs: Long,
        clientId: String?
    ): android.app.Notification {
        val transferredStr = formatBytes(bytesTransferred)
        val totalStr = formatBytes(totalBytes)
        val progressStr = getString(R.string.notification_progress, transferredStr, totalStr)
        val etaStr = formatRemainingTime(remainingMs)
        val contentText = if (etaStr.isNotEmpty()) "$progressStr · $etaStr" else progressStr
        val progress =
            if (totalBytes > 0) (100 * bytesTransferred / totalBytes).toInt().coerceIn(0, 100) else 0
        val pending = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("$typeLabel: $contentText")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, progress, false)

        if (clientId != null) {
            val cancelIntent = Intent(this, QuestionUploadForegroundService::class.java).apply {
                action = ACTION_CANCEL_UPLOAD
                putExtra(KEY_CLIENT_ID, clientId)
            }
            val cancelPending = PendingIntent.getService(
                this,
                1001,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(android.R.string.cancel),
                cancelPending
            )
        }

        return builder.build()
    }

    private fun updateNotificationProgress(
        typeLabel: String,
        bytesTransferred: Long,
        totalBytes: Long,
        remainingMs: Long,
        clientId: String
    ) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID,
            buildNotificationWithProgress(typeLabel, bytesTransferred, totalBytes, remainingMs, clientId)
        )
    }

    private fun updateNotification(type: String, remaining: Int) {
        val label = when (type) {
            QuestionMessage.TYPE_TEXT -> getString(R.string.notification_upload_type_text)
            QuestionMessage.TYPE_IMAGE -> getString(R.string.notification_upload_type_image)
            QuestionMessage.TYPE_VIDEO -> getString(R.string.notification_upload_type_video)
            QuestionMessage.TYPE_AUDIO -> getString(R.string.notification_upload_type_audio)
            else -> getString(R.string.notification_uploading_generic)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification(label, remaining))
    }

    private data class UploadItem(
        val questionId: String,
        val type: String,
        val clientId: String,
        val senderUid: String,
        val senderRole: String,
        val filePath: String?,
        val textContent: String?,
        val caption: String?
    )

    companion object {
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "question_upload"
        const val KEY_QUESTION_ID = "question_id"
        const val KEY_TYPE = "type"
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_SENDER_UID = "sender_uid"
        const val KEY_SENDER_ROLE = "sender_role"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_TEXT_CONTENT = "text_content"
        const val KEY_CAPTION = "caption"

        const val ACTION_CANCEL_UPLOAD = "com.example.app.action.CANCEL_UPLOAD"
        const val ACTION_UPLOAD_STARTED = "com.example.app.action.UPLOAD_STARTED"
        const val ACTION_UPLOAD_CANCELED = "com.example.app.action.UPLOAD_CANCELED"

        fun start(context: android.content.Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelUpload(context: android.content.Context, clientId: String) {
            val intent = Intent(context, QuestionUploadForegroundService::class.java).apply {
                action = ACTION_CANCEL_UPLOAD
                putExtra(KEY_CLIENT_ID, clientId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}


