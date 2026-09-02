package com.example.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

/**
 * Foreground service that records the screen (480p, with mic).
 * Max duration: pass EXTRA_MAX_DURATION_MS (e.g. 180_000 for teachers, 60_000 for students).
 * When done, broadcasts ACTION_RECORDING_FINISHED with extra OUTPUT_PATH, or ACTION_RECORDING_FAILED.
 */
class ScreenRecordingService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputPath: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var isPaused = false
    private var recordingStartTimeMs: Long = 0
    private var totalPausedDurationMs: Long = 0
    private var pauseStartTimeMs: Long = 0
    private var maxDurationMs: Long = MAX_DURATION_MS

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        when (intent.action) {
            ACTION_STOP_AND_SAVE -> {
                stopRecordingInternal()
                return START_NOT_STICKY
            }
            ACTION_STOP_AND_DISCARD -> {
                stopAndDiscard()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                pauseRecording()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                resumeRecording()
                return START_NOT_STICKY
            }
        }
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            sendBroadcast(Intent(ACTION_RECORDING_FAILED))
            stopSelf()
            return START_NOT_STICKY
        }
        // API 29+ tipli ön plan servisi. Android 14'ten itibaren mikrofon tipini yalnızca
        // RECORD_AUDIO gerçekten verilmişse bildirebiliriz; aksi halde startForeground
        // SecurityException atar. İzin yoksa sadece mediaProjection tipiyle başlıyoruz
        // (setupMediaRecorder de bu durumda sesi devre dışı bırakıyor).
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                if (hasAudioPermission()) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, createNotification(), type)
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground başarısız", e)
            sendBroadcast(Intent(ACTION_RECORDING_FAILED))
            stopSelf()
            return START_NOT_STICKY
        }
        maxDurationMs = intent.getLongExtra(EXTRA_MAX_DURATION_MS, MAX_DURATION_MS)
        val outDir = File(cacheDir, "question_videos").apply { mkdirs() }
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        val file = File(outDir, "${uid}_${System.currentTimeMillis()}.mp4")
        outputPath = file.absolutePath
        try {
            // API 36'dan itibaren null dönebilir (kullanıcı izni geri çekerse); aşağıdaki
            // catch bloğu kayıt başlatılamadı akışını zaten yürütüyor.
            val projection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(resultCode, data)
                ?: throw IllegalStateException("MediaProjection alınamadı")
            mediaProjection = projection
            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    stopRecordingInternal()
                }
            }
            projection.registerCallback(projectionCallback!!, handler)
            setupMediaRecorder(file.absolutePath)
            createVirtualDisplay(projection)
            mediaRecorder?.start()
            recordingStartTimeMs = System.currentTimeMillis()
            totalPausedDurationMs = 0
            isPaused = false
            stopRunnable = Runnable {
                stopRecordingInternal()
            }
            handler.postDelayed(stopRunnable!!, maxDurationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            sendBroadcast(Intent(ACTION_RECORDING_FAILED))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createVirtualDisplay(projection: MediaProjection) {
        val metrics = resources.displayMetrics
        val density = metrics.densityDpi
        val width = 1280
        val height = 720
        virtualDisplay = projection.createVirtualDisplay(
            "QuestionRecord",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface,
            null,
            handler
        )
    }

    /**
     * RECORD_AUDIO verilmiş mi? Android 14'ten (API 34) itibaren mikrofonlu ön plan servisi
     * yalnızca izin verilmişse başlatılabilir; verilmemişse sessiz kayda düşüyoruz ki
     * özellik tamamen çökmek yerine çalışmaya devam etsin.
     */
    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun setupMediaRecorder(path: String) {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        val withAudio = hasAudioPermission()
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        if (withAudio) recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (withAudio) recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setVideoSize(1280, 720)
        // Ders ekranları büyük ölçüde durağan (akıcı hareket yerine dokunma/geçiş);
        // 30'dan 20 fps'e düşürmek aynı bitrate'i daha az kareye bölüp kare başına
        // netliği artırıyor, dosya boyutunu BÜYÜTMÜYOR (bkz. bitrate açıklaması altta).
        recorder.setVideoFrameRate(20)
        // 2.5 Mbps'te ekran içeriği (ince metin, ikon kenarları) gözle görülür şekilde
        // pikselleşiyordu; 4 Mbps'e çıkarıldı (bkz. storage.rules'daki 150 MB üst sınırı —
        // 180 sn * 4 Mbps + ses ~93 MB, hâlâ rahat bir payla altında).
        recorder.setVideoEncodingBitRate(4_000_000)
        if (withAudio) {
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
        } else {
            Log.w(TAG, "RECORD_AUDIO verilmedi; ekran kaydı sessiz yapılacak.")
        }
        recorder.setOutputFile(path)
        try {
            recorder.prepare()
        } catch (e: IOException) {
            throw RuntimeException("MediaRecorder prepare failed", e)
        }
        mediaRecorder = recorder
    }

    private fun stopRecordingInternal() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }
        mediaRecorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        projectionCallback?.let { mediaProjection?.unregisterCallback(it) }
        projectionCallback = null
        mediaProjection?.stop()
        mediaProjection = null
        val path = outputPath
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (path != null && File(path).exists()) {
            sendBroadcast(Intent(ACTION_RECORDING_FINISHED).apply { putExtra(EXTRA_OUTPUT_PATH, path) })
        } else {
            sendBroadcast(Intent(ACTION_RECORDING_FAILED))
        }
    }

    private fun pauseRecording() {
        if (isPaused || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        isPaused = true
        pauseStartTimeMs = System.currentTimeMillis()
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        try {
            mediaRecorder?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "pause failed", e)
        }
        sendBroadcast(Intent(ACTION_RECORDING_PAUSED))
    }

    private fun resumeRecording() {
        if (!isPaused || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            mediaRecorder?.resume()
        } catch (e: Exception) {
            Log.e(TAG, "resume failed", e)
        }
        totalPausedDurationMs += System.currentTimeMillis() - pauseStartTimeMs
        isPaused = false
        val elapsedMs = System.currentTimeMillis() - recordingStartTimeMs - totalPausedDurationMs
        val remainingMs = (maxDurationMs - elapsedMs).coerceAtLeast(0L)
        stopRunnable = Runnable { stopRecordingInternal() }
        handler.postDelayed(stopRunnable!!, remainingMs)
        sendBroadcast(Intent(ACTION_RECORDING_RESUMED))
    }

    private fun stopAndDiscard() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }
        mediaRecorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        projectionCallback?.let { mediaProjection?.unregisterCallback(it) }
        projectionCallback = null
        mediaProjection?.stop()
        mediaProjection = null
        outputPath?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) { }
        }
        outputPath = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val channelId = "screen_recording"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_upload),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Ekran kaydı yapılıyor...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ScreenRecordingService"
        const val ACTION_RECORDING_FINISHED = "com.example.app.RECORDING_FINISHED"
        const val ACTION_RECORDING_FAILED = "com.example.app.RECORDING_FAILED"
        const val ACTION_STOP_AND_SAVE = "com.example.app.STOP_AND_SAVE"
        const val ACTION_STOP_AND_DISCARD = "com.example.app.STOP_AND_DISCARD"
        const val ACTION_PAUSE = "com.example.app.PAUSE"
        const val ACTION_RESUME = "com.example.app.RESUME"
        const val ACTION_RECORDING_PAUSED = "com.example.app.RECORDING_PAUSED"
        const val ACTION_RECORDING_RESUMED = "com.example.app.RECORDING_RESUMED"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_MAX_DURATION_MS = "extra_max_duration_ms"
        const val MAX_DURATION_MS = 60_000L
        private const val NOTIFICATION_ID = 9002
    }
}
