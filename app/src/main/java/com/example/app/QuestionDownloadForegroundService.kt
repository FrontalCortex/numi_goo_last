package com.example.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class QuestionDownloadForegroundService : Service() {

    private val storage = Firebase.storage

    private val queue = ConcurrentLinkedQueue<DownloadItem>()
    private val isProcessing = AtomicBoolean(false)
    private val activeTasks = ConcurrentHashMap<String, FileDownloadTask>()
    private val canceledMessageIds = ConcurrentHashMap.newKeySet<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_DOWNLOAD -> {
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return START_NOT_STICKY
                val questionId = intent.getStringExtra(EXTRA_QUESTION_ID) ?: return START_NOT_STICKY
                val mediaPath = intent.getStringExtra(EXTRA_MEDIA_STORAGE_PATH)

                // Önceki iptalden kalan flag varsa temizle ki tekrar indirme başlayabilsin
                canceledMessageIds.remove(messageId)

                val item = DownloadItem(
                    questionId = questionId,
                    messageId = messageId,
                    mediaStoragePath = mediaPath
                )
                queue.add(item)
                if (isProcessing.compareAndSet(false, true)) {
                    startForeground(NOTIFICATION_ID, buildBaseNotification("İndiriliyor"))
                    Thread { processQueue() }.start()
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return START_NOT_STICKY
                val questionId = intent.getStringExtra(EXTRA_QUESTION_ID)

                canceledMessageIds.add(messageId)
                // Aktif işi iptal et ve kuyruktan da temizle
                activeTasks[messageId]?.cancel()
                activeTasks.remove(messageId)
                queue.removeIf { it.messageId == messageId }

                // Fragment'e "iptal edildi" bilgisini gönder (doğru sohbeti bulabilsin diye questionId de ekleniyor)
                sendBroadcast(Intent(ACTION_DOWNLOAD_CANCELED).apply {
                    putExtra(EXTRA_MESSAGE_ID, messageId)
                    if (questionId != null) {
                        putExtra(EXTRA_QUESTION_ID, questionId)
                    }
                })

                // Şu anki tasarımda tek bildirim var; iptal isteği geldiğinde hemen kapat
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
                stopSelf()
            }
        }

        return START_STICKY
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
            min > 0 -> if (sec % 60 == 0) {
                getString(R.string.notification_eta_min, min)
            } else {
                getString(R.string.notification_eta_min_sec, min, sec % 60)
            }
            else -> getString(R.string.notification_eta_sec, sec.coerceAtLeast(1))
        }
    }

    private fun processQueue() {
        while (true) {
            val item = queue.poll() ?: break
            if (canceledMessageIds.remove(item.messageId)) continue
            startForeground(NOTIFICATION_ID, buildBaseNotification("İndiriliyor"))
            sendBroadcast(Intent(ACTION_DOWNLOAD_STARTED).apply {
                putExtra(EXTRA_MESSAGE_ID, item.messageId)
                putExtra(EXTRA_QUESTION_ID, item.questionId)
            })
            runCatching { downloadOne(item) }
        }
        isProcessing.set(false)
        if (queue.isEmpty() && activeTasks.isEmpty()) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
            stopSelf()
        }
    }

    private fun downloadOne(item: DownloadItem) {
        val path = item.mediaStoragePath ?: return
        val ref = storage.getReference(path)

        val dir = File(filesDir, "downloads").apply { mkdirs() }
        val file = File(dir, "${item.messageId}_${System.currentTimeMillis()}")

        val task = ref.getFile(file)
        activeTasks[item.messageId] = task

        val startTime = System.currentTimeMillis()

        task
            .addOnProgressListener { snap ->
                if (canceledMessageIds.contains(item.messageId)) return@addOnProgressListener
                val totalBytes = snap.totalByteCount.coerceAtLeast(1L)
                val transferredBytes = snap.bytesTransferred.coerceAtLeast(0L)

                val elapsed = System.currentTimeMillis() - startTime
                val speed = if (elapsed > 500 && transferredBytes > 0) {
                    transferredBytes.toDouble() / elapsed.toDouble()
                } else {
                    0.0
                }
                val remainingMs =
                    if (speed > 0 && totalBytes > transferredBytes) {
                        ((totalBytes - transferredBytes) / speed).toLong()
                    } else {
                        -1L
                    }

                updateProgress(transferredBytes, totalBytes, remainingMs, item.messageId, item.questionId)

                val percent = ((transferredBytes.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
                sendBroadcast(Intent(ACTION_DOWNLOAD_PROGRESS).apply {
                    putExtra(EXTRA_MESSAGE_ID, item.messageId)
                    putExtra(EXTRA_QUESTION_ID, item.questionId)
                    putExtra(EXTRA_PROGRESS, percent)
                })
            }
            .addOnSuccessListener {
                activeTasks.remove(item.messageId)
                if (!canceledMessageIds.contains(item.messageId)) {
                    // İndirme başarıyla bitti: yerel path'i global cache'e yaz ve persist et
                    GlobalValues.downloadedMediaByMessageId[item.messageId] = file.absolutePath
                    GlobalValues.persistDownloadedMediaCache(this)

                    sendBroadcast(Intent(ACTION_DOWNLOAD_COMPLETED).apply {
                        putExtra(EXTRA_MESSAGE_ID, item.messageId)
                        putExtra(EXTRA_QUESTION_ID, item.questionId)
                    })
                }
            }
            .addOnFailureListener {
                activeTasks.remove(item.messageId)
                if (!canceledMessageIds.contains(item.messageId)) {
                    sendBroadcast(Intent(ACTION_DOWNLOAD_FAILED).apply {
                        putExtra(EXTRA_MESSAGE_ID, item.messageId)
                        putExtra(EXTRA_QUESTION_ID, item.questionId)
                    })
                }
            }
            .addOnCompleteListener {
                if (queue.isEmpty() && activeTasks.isEmpty()) {
                    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
                    stopSelf()
                }
            }
    }

    private fun updateProgress(
        bytesTransferred: Long,
        totalBytes: Long,
        remainingMs: Long,
        messageId: String,
        questionId: String
    ) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val transferredStr = formatBytes(bytesTransferred)
        val totalStr = formatBytes(totalBytes)
        val progressStr = getString(R.string.notification_progress, transferredStr, totalStr)
        val etaStr = formatRemainingTime(remainingMs)
        val contentText = if (etaStr.isNotEmpty()) "$progressStr · $etaStr" else progressStr

        val progress =
            if (totalBytes > 0) (100 * bytesTransferred / totalBytes).toInt().coerceIn(0, 100) else 0

        val cancelIntent = Intent(this, QuestionDownloadForegroundService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_MESSAGE_ID, messageId)
            putExtra(EXTRA_QUESTION_ID, questionId)
        }
        val cancelPending = android.app.PendingIntent.getService(
            this,
            2001,
            cancelIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, progress, false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_download_cancel),
                cancelPending
            )
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildBaseNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_upload),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_upload_desc)
                setShowBadge(true)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    data class DownloadItem(
        val questionId: String,
        val messageId: String,
        val mediaStoragePath: String?
    )

    companion object {
        // Upload servisiyle aynı kanalı kullan (kullanıcı zaten açık).
        private const val CHANNEL_ID = "question_upload"
        private const val NOTIFICATION_ID = 3001

        const val ACTION_START_DOWNLOAD = "com.example.app.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.app.action.CANCEL_DOWNLOAD"
        const val ACTION_DOWNLOAD_STARTED = "com.example.app.action.DOWNLOAD_STARTED"
        const val ACTION_DOWNLOAD_COMPLETED = "com.example.app.action.DOWNLOAD_COMPLETED"
        const val ACTION_DOWNLOAD_FAILED = "com.example.app.action.DOWNLOAD_FAILED"
        const val ACTION_DOWNLOAD_CANCELED = "com.example.app.action.DOWNLOAD_CANCELED"
        const val ACTION_DOWNLOAD_PROGRESS = "com.example.app.action.DOWNLOAD_PROGRESS"

        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_QUESTION_ID = "extra_question_id"
        const val EXTRA_MEDIA_STORAGE_PATH = "extra_media_storage_path"
        const val EXTRA_PROGRESS = "extra_progress"

        fun startDownload(
            context: Context,
            questionId: String,
            messageId: String,
            mediaStoragePath: String?
        ) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, QuestionDownloadForegroundService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_QUESTION_ID, questionId)
                putExtra(EXTRA_MESSAGE_ID, messageId)
                putExtra(EXTRA_MEDIA_STORAGE_PATH, mediaStoragePath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                androidx.core.content.ContextCompat.startForegroundService(appContext, intent)
            } else {
                appContext.startService(intent)
            }
        }

        fun cancelDownload(
            context: Context,
            questionId: String,
            messageId: String
        ) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, QuestionDownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_QUESTION_ID, questionId)
                putExtra(EXTRA_MESSAGE_ID, messageId)
            }
            appContext.startService(intent)
        }
    }
}







