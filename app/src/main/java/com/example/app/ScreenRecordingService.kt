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
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
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
            broadcastToApp(ACTION_RECORDING_FAILED)
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
            broadcastToApp(ACTION_RECORDING_FAILED)
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
            val (recorder, config) = prepareMediaRecorder(file.absolutePath)
            mediaRecorder = recorder
            createVirtualDisplay(projection, config)
            recorder.start()
            recordingStartTimeMs = System.currentTimeMillis()
            totalPausedDurationMs = 0
            isPaused = false
            stopRunnable = Runnable {
                stopRecordingInternal()
            }
            handler.postDelayed(stopRunnable!!, maxDurationMs)
            // Panel, kaydın GERÇEKTEN başladığını yalnızca bu sinyalle öğreniyor. Gelmezse
            // MainActivity paneli kapatıp hata gösteriyor; aksi halde kayıt hiç başlamamışken
            // sayaç işlemeye devam edip kullanıcıya "kaydediliyor" izlenimi veriyordu.
            broadcastToApp(ACTION_RECORDING_STARTED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorderAndDisplay()
            releaseProjection()
            try {
                file.delete()
            } catch (_: Exception) { }
            outputPath = null
            broadcastToApp(ACTION_RECORDING_FAILED)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /**
     * Servis ile MainActivity aynı uygulamada; yayını pakete kilitliyoruz.
     * Paketsiz (implicit) yayınlar, alıcı RECEIVER_NOT_EXPORTED ile kayıtlı olduğunda
     * Android 14+ ve bazı üretici ROM'larında (HyperOS/MIUI) teslim edilmiyor; kaydet/duraklat
     * butonları basılıyor ama arayüz hiç tepki vermiyordu.
     */
    private fun broadcastToApp(action: String, extras: (Intent.() -> Unit)? = null) {
        val intent = Intent(action).setPackage(packageName)
        extras?.invoke(intent)
        sendBroadcast(intent)
    }

    private fun createVirtualDisplay(projection: MediaProjection, config: VideoConfig) {
        val density = resources.displayMetrics.densityDpi
        virtualDisplay = projection.createVirtualDisplay(
            "QuestionRecord",
            config.width,
            config.height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface,
            null,
            handler
        )
    }

    /** Hata yolunda yarım kalmış recorder/display'i bırakır (stop() çağırmadan; kayıt başlamamış olabilir). */
    private fun releaseRecorderAndDisplay() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) { }
        mediaRecorder = null
        try {
            virtualDisplay?.release()
        } catch (_: Exception) { }
        virtualDisplay = null
    }

    private fun releaseProjection() {
        projectionCallback?.let { cb ->
            try {
                mediaProjection?.unregisterCallback(cb)
            } catch (_: Exception) { }
        }
        projectionCallback = null
        try {
            mediaProjection?.stop()
        } catch (_: Exception) { }
        mediaProjection = null
    }

    /**
     * RECORD_AUDIO verilmiş mi? Android 14'ten (API 34) itibaren mikrofonlu ön plan servisi
     * yalnızca izin verilmişse başlatılabilir; verilmemişse sessiz kayda düşüyoruz ki
     * özellik tamamen çökmek yerine çalışmaya devam etsin.
     */
    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Kaydın çözünürlük/bitrate/fps üçlüsü; cihazın H264 encoder'ına göre seçilir. */
    private data class VideoConfig(val width: Int, val height: Int, val bitRate: Int, val frameRate: Int) {
        override fun toString() = "${width}x$height@${frameRate}fps ${bitRate / 1000}kbps"
    }

    /**
     * Adayları sırayla prepare() ederek cihazın gerçekten kabul ettiği ilk profili döner.
     *
     * Sabit 1280x720 + 20 fps + 5 Mbps kombinasyonu her donanım encoder'ında geçerli değil;
     * reddedildiğinde kayıt hiç başlamıyor, kullanıcı bunu ancak butonlar tepki vermeyince
     * fark ediyordu. Önce MediaCodecList'ten okunan yeteneklere göre değerleri kırpıyor,
     * yine de olmazsa daha düşük çözünürlüklere iniyoruz.
     */
    private fun prepareMediaRecorder(path: String): Pair<MediaRecorder, VideoConfig> {
        var lastError: Exception? = null
        for (config in videoConfigCandidates()) {
            val recorder = newRecorder()
            try {
                configureRecorder(recorder, path, config)
                recorder.prepare()
                Log.i(TAG, "Kayıt profili: $config")
                return recorder to config
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Profil reddedildi ($config), sonraki aday deneniyor", e)
                try {
                    recorder.reset()
                    recorder.release()
                } catch (_: Exception) { }
            }
        }
        throw RuntimeException("MediaRecorder hiçbir video profiliyle hazırlanamadı", lastError)
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun configureRecorder(recorder: MediaRecorder, path: String, config: VideoConfig) {
        val withAudio = hasAudioPermission()
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        if (withAudio) recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (withAudio) recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setVideoSize(config.width, config.height)
        // Ders ekranları büyük ölçüde durağan (akıcı hareket yerine dokunma/geçiş);
        // 30'dan 20 fps'e düşürmek aynı bitrate'i daha az kareye bölüp kare başına
        // netliği artırıyor, dosya boyutunu BÜYÜTMÜYOR (bkz. bitrate açıklaması altta).
        recorder.setVideoFrameRate(config.frameRate)
        // 2.5 Mbps'te, sonra 4 Mbps'te bile ekran içeriği (ince metin, ikon kenarları)
        // gözle görülür şekilde pikselleşiyordu; 5 Mbps'e çıkarıldı (bkz. storage.rules'daki
        // 150 MB üst sınırı — 180 sn * 5 Mbps + ses ~115 MB, hâlâ rahat bir payla altında).
        recorder.setVideoEncodingBitRate(config.bitRate)
        if (withAudio) {
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
        } else {
            Log.w(TAG, "RECORD_AUDIO verilmedi; ekran kaydı sessiz yapılacak.")
        }
        recorder.setOutputFile(path)
    }

    /**
     * İstenen boyutlar, cihazın AVC encoder'ının desteklediği aralığa/hizalamaya çekilerek
     * aday listesine dönüştürülür. Yetenekler okunamazsa ham adaylarla devam edilir.
     */
    private fun videoConfigCandidates(): List<VideoConfig> {
        val caps = avcVideoCapabilities()
        val out = LinkedHashSet<VideoConfig>()
        for ((w, h) in PREFERRED_SIZES) {
            val size = supportedSize(caps, w, h) ?: continue
            val bitRate = caps?.bitrateRange?.clamp(TARGET_BITRATE) ?: TARGET_BITRATE
            val frameRate = caps
                ?.let { runCatching { it.getSupportedFrameRatesFor(size.first, size.second) }.getOrNull() }
                ?.clamp(TARGET_FRAME_RATE.toDouble())?.toInt()
                ?: TARGET_FRAME_RATE
            out += VideoConfig(size.first, size.second, bitRate, frameRate.coerceAtLeast(1))
        }
        // Yetenek sorgusu tüm adayları elediyse en azından klasik 720p ile bir kez denensin.
        if (out.isEmpty()) out += VideoConfig(1280, 720, TARGET_BITRATE, TARGET_FRAME_RATE)
        return out.toList()
    }

    private fun avcVideoCapabilities(): MediaCodecInfo.VideoCapabilities? = try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .asSequence()
            .filter { it.isEncoder }
            .filter { info -> info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) } }
            .mapNotNull {
                runCatching { it.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).videoCapabilities }
                    .getOrNull()
            }
            .firstOrNull()
    } catch (e: Exception) {
        Log.w(TAG, "AVC encoder yetenekleri okunamadı; varsayılan profillerle denenecek", e)
        null
    }

    /** [w]x[h]'yi encoder'ın hizalama kuralına ve desteklediği aralığa çeker; mümkün değilse null. */
    private fun supportedSize(caps: MediaCodecInfo.VideoCapabilities?, w: Int, h: Int): Pair<Int, Int>? {
        if (caps == null) return w to h
        return try {
            val wAlign = caps.widthAlignment.coerceAtLeast(1)
            val hAlign = caps.heightAlignment.coerceAtLeast(1)
            val aw = (caps.supportedWidths.clamp(w) / wAlign) * wAlign
            val ah = (caps.supportedHeights.clamp(h) / hAlign) * hAlign
            if (aw > 0 && ah > 0 && caps.isSizeSupported(aw, ah)) aw to ah else null
        } catch (_: Exception) {
            null
        }
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
        // Encoder hiç kare yazamadan durursa dosya oluşur ama 0 bayt kalır; bunu başarı sayıp
        // CreateQuestion'ı açmak, oynatılamayan bir videoyla ilerlemek demek olurdu.
        if (path != null && File(path).length() > 0L) {
            broadcastToApp(ACTION_RECORDING_FINISHED) { putExtra(EXTRA_OUTPUT_PATH, path) }
        } else {
            broadcastToApp(ACTION_RECORDING_FAILED)
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
        broadcastToApp(ACTION_RECORDING_PAUSED)
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
        broadcastToApp(ACTION_RECORDING_RESUMED)
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
        const val ACTION_RECORDING_STARTED = "com.example.app.RECORDING_STARTED"
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
        private const val TARGET_BITRATE = 5_000_000
        private const val TARGET_FRAME_RATE = 20
        /** İstenen sırayla denenecek çözünürlükler; ilki kabul edilen kullanılır. */
        private val PREFERRED_SIZES = listOf(
            1280 to 720,
            960 to 540,
            854 to 480,
            640 to 360
        )
    }
}
