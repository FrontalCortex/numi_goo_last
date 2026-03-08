package com.example.app

import android.app.Dialog
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.widget.ImageView
import android.view.WindowManager
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ProgressBar
import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentQuestionChatBinding
import com.example.app.model.QuestionMessage
import com.example.app.model.StudentQuestion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class QuestionChatFragment : Fragment() {

    private var _binding: FragmentQuestionChatBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val authManager by lazy { AuthManager().also { it.initialize(requireContext()) } }
    private var questionId: String = ""
    private var questionStatus: String = ""
    private var isTeacher = false
    private var listener: ListenerRegistration? = null
    private var isUserRestrictedOrBanned = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var audioMediaPlayer: MediaPlayer? = null
    private var videoDialog: Dialog? = null
    private var videoExoPlayer: ExoPlayer? = null

    // Ses kaydı (gönderilecek mesaj) için durum
    private var isRecordingAudio: Boolean = false
    private var isRecordingPaused: Boolean = false
    private var recordingStartMs: Long = 0L
    private var recordedMsBeforePause: Long = 0L
    private val recordingHandler = Handler(Looper.getMainLooper())
    private val recordingRunnable = object : Runnable {
        override fun run() {
            if (!isRecordingAudio || isRecordingPaused) return
            val now = System.currentTimeMillis()
            val elapsed = (now - recordingStartMs) + recordedMsBeforePause
            val clamped = elapsed.coerceAtLeast(0L).coerceAtMost(maxAudioDurationMs)

            binding.audioBarCurrentTime.text = formatAudioTime(clamped.toInt())
            binding.audioBarTotalTime.text = formatAudioTime(maxAudioDurationMs.toInt())
            binding.audioBarWaveform.progress = clamped.toFloat() / maxAudioDurationMs.toFloat()

            if (clamped >= maxAudioDurationMs) {
                // Süre doldu, kaydı otomatik durdur; kullanıcı isterse gönderebilir.
                pauseAudioRecordingInternal()
                return
            }
            recordingHandler.postDelayed(this, 200)
        }
    }

    private var serverMessageList = listOf<QuestionMessage>()
    private val pendingMessages = mutableListOf<QuestionMessage>()
    private val revealedMediaMessageIds = mutableSetOf<String>()
    private val canceledUploadIds = mutableSetOf<String>()
    private val activeUploadIds = mutableSetOf<String>()
    private val activeDownloadIds = mutableSetOf<String>()
    private val uploadProgress = mutableMapOf<String, Int>()
    private val downloadProgress = mutableMapOf<String, Int>()
    private val locallyCanceledClientIds = mutableSetOf<String>()
    private var chatAdapter: ChatMessageAdapter? = null
    private var uploadStartedReceiverRegistered = false

    private val uploadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                QuestionUploadForegroundService.ACTION_UPLOAD_STARTED -> {
                    val qId = intent.getStringExtra(QuestionUploadForegroundService.KEY_QUESTION_ID) ?: return
                    if (qId != questionId) return
                    val clientId = intent.getStringExtra(QuestionUploadForegroundService.KEY_CLIENT_ID) ?: return
                    if (locallyCanceledClientIds.contains(clientId)) return
                    activeUploadIds.add(clientId)
                    GlobalValues.activeUploadIdsByQuestion[questionId] = activeUploadIds.toMutableSet()
                    if (pendingMessages.any { it.id == clientId }) {
                        submitMergedList()
                        return
                    }

                    val type = intent.getStringExtra(QuestionUploadForegroundService.KEY_TYPE) ?: QuestionMessage.TYPE_TEXT
                    val senderUid = intent.getStringExtra(QuestionUploadForegroundService.KEY_SENDER_UID) ?: auth.currentUser?.uid.orEmpty()
                    val senderRole = intent.getStringExtra(QuestionUploadForegroundService.KEY_SENDER_ROLE) ?: authManager.getCurrentUserType()
                    val textContent = intent.getStringExtra(QuestionUploadForegroundService.KEY_TEXT_CONTENT)
                    val caption = intent.getStringExtra(QuestionUploadForegroundService.KEY_CAPTION)

                    val pending = QuestionMessage(
                        id = clientId,
                        senderUid = senderUid,
                        senderRole = senderRole,
                        type = type,
                        textContent = caption ?: textContent,
                        createdAt = Timestamp.now()
                    )
                    addPendingMessage(pending)
                }

                QuestionUploadForegroundService.ACTION_UPLOAD_CANCELED -> {
                    val clientId = intent.getStringExtra(QuestionUploadForegroundService.KEY_CLIENT_ID) ?: return
                    if (locallyCanceledClientIds.contains(clientId)) return
                    if (pendingMessages.none { it.id == clientId }) return
                    activeUploadIds.remove(clientId)
                    canceledUploadIds.add(clientId)
                    GlobalValues.canceledUploadIdsByQuestion[questionId] = canceledUploadIds.toMutableSet()
                    GlobalValues.activeUploadIdsByQuestion[questionId] = activeUploadIds.toMutableSet()
                    submitMergedList()
                }
                QuestionUploadForegroundService.ACTION_UPLOAD_PROGRESS -> {
                    val qId = intent.getStringExtra(QuestionUploadForegroundService.KEY_QUESTION_ID) ?: return
                    if (qId != questionId) return
                    val clientId = intent.getStringExtra(QuestionUploadForegroundService.KEY_CLIENT_ID) ?: return
                    val progress = intent.getIntExtra(QuestionUploadForegroundService.KEY_UPLOAD_PROGRESS, 0)
                    uploadProgress[clientId] = progress.coerceIn(0, 100)
                    notifyMessageChanged(clientId)
                }
                QuestionDownloadForegroundService.ACTION_DOWNLOAD_PROGRESS -> {
                    val qId = intent.getStringExtra(QuestionDownloadForegroundService.EXTRA_QUESTION_ID) ?: return
                    if (qId != questionId) return
                    val messageId = intent.getStringExtra(QuestionDownloadForegroundService.EXTRA_MESSAGE_ID) ?: return
                    val progress = intent.getIntExtra(QuestionDownloadForegroundService.EXTRA_PROGRESS, 0)
                    downloadProgress[messageId] = progress.coerceIn(0, 100)
                    notifyMessageChanged(messageId)
                }
                QuestionDownloadForegroundService.ACTION_DOWNLOAD_STARTED -> {
                    val qId = intent.getStringExtra(QuestionDownloadForegroundService.EXTRA_QUESTION_ID) ?: return
                    if (qId != questionId) return
                    val messageId = intent.getStringExtra(QuestionDownloadForegroundService.EXTRA_MESSAGE_ID) ?: return
                    if (!activeDownloadIds.contains(messageId)) {
                        activeDownloadIds.add(messageId)
                        GlobalValues.activeDownloadIdsByQuestion[questionId] = activeDownloadIds.toMutableSet()
                        notifyMessageChanged(messageId)
                    }
                }
                QuestionDownloadForegroundService.ACTION_DOWNLOAD_COMPLETED,
                QuestionDownloadForegroundService.ACTION_DOWNLOAD_FAILED,
                QuestionDownloadForegroundService.ACTION_DOWNLOAD_CANCELED -> {
                    val qId = intent.getStringExtra(QuestionDownloadForegroundService.EXTRA_QUESTION_ID) ?: return
                    if (qId != questionId) return
                    val messageId = intent.getStringExtra(QuestionDownloadForegroundService.EXTRA_MESSAGE_ID) ?: return
                    val wasCompleted = intent.action == QuestionDownloadForegroundService.ACTION_DOWNLOAD_COMPLETED
                    Handler(Looper.getMainLooper()).post {
                        if (_binding == null || !isAdded) return@post
                        activeDownloadIds.remove(messageId)
                        downloadProgress.remove(messageId)
                        GlobalValues.activeDownloadIdsByQuestion[questionId] = activeDownloadIds.toMutableSet()
                        if (wasCompleted) {
                            revealedMediaMessageIds.add(messageId)
                            GlobalValues.revealedMediaIdsByQuestion[questionId] =
                                revealedMediaMessageIds.toMutableSet()
                        }
                        submitMergedList()
                        notifyMessageChanged(messageId)
                        binding.messagesRecyclerView.post {
                            submitMergedList()
                            notifyMessageChanged(messageId)
                        }
                        binding.messagesRecyclerView.postDelayed({
                            if (_binding != null && isAdded) {
                                submitMergedList()
                                notifyMessageChanged(messageId)
                            }
                        }, 80)
                    }
                }
            }
        }
    }

    private fun submitMergedList() {
        if (_binding == null || !isAdded) return
        val merged = (serverMessageList + pendingMessages).sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        chatAdapter?.submitList(merged)
        GlobalValues.pendingQuestionMessages[questionId] = pendingMessages.toMutableList()
    }

    /** Sadece belirli bir mesajın UI durumunu değiştirmek gerektiğinde kullanılır (liste yapısı aynı kalır). */
    private fun notifyMessageChanged(messageId: String) {
        if (_binding == null || !isAdded) return
        val merged = (serverMessageList + pendingMessages).sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        val index = merged.indexOfFirst { it.id == messageId }
        if (index != -1) {
            chatAdapter?.notifyItemChanged(index)
        }
    }

    private fun addPendingMessage(msg: QuestionMessage) {
        if (!isAdded) return
        pendingMessages.add(msg)
        submitMergedList()
    }

    private fun removePendingByClientId(clientId: String) {
        pendingMessages.removeAll { it.id == clientId }
        if (_binding == null || !isAdded) return
        submitMergedList()
    }

    private fun cancelPendingSend(clientId: String) {
        if (!isAdded) return
        locallyCanceledClientIds.add(clientId)
        val appContext = requireContext().applicationContext
        QuestionUploadForegroundService.cancelUpload(appContext, clientId)

        activeUploadIds.remove(clientId)
        canceledUploadIds.remove(clientId)
        GlobalValues.activeUploadIdsByQuestion[questionId] = activeUploadIds.toMutableSet()
        GlobalValues.canceledUploadIdsByQuestion[questionId] = canceledUploadIds.toMutableSet()

        GlobalValues.uploadMetaByClientId.remove(clientId)?.filePath?.let { path ->
            runCatching { File(path).delete() }
        }

        removePendingByClientId(clientId)
    }

    private fun startDownloadForMessage(message: QuestionMessage) {
        if (!isAdded) return
        // Eğer bu mesaj için daha önce indirilmiş bir dosya varsa, tekrar Firebase'den indirme.
        val existingPath = GlobalValues.downloadedMediaByMessageId[message.id]
        if (!existingPath.isNullOrBlank() && java.io.File(existingPath).exists()) {
            // Bu mesajı "revealed" kabul et ve sadece UI'ı güncelle.
            revealedMediaMessageIds.add(message.id)
            GlobalValues.revealedMediaIdsByQuestion[questionId] =
                revealedMediaMessageIds.toMutableSet()
            notifyMessageChanged(message.id)
            return
        }

        if (message.mediaStoragePath == null) return
        if (activeDownloadIds.contains(message.id)) return
        activeDownloadIds.add(message.id)
        GlobalValues.activeDownloadIdsByQuestion[questionId] = activeDownloadIds.toMutableSet()
        QuestionDownloadForegroundService.startDownload(
            requireContext(),
            questionId = questionId,
            messageId = message.id,
            mediaStoragePath = message.mediaStoragePath
        )
        notifyMessageChanged(message.id)
    }

    private fun cancelDownloadForMessage(message: QuestionMessage) {
        if (!isAdded) return
        if (!activeDownloadIds.contains(message.id)) return
        activeDownloadIds.remove(message.id)
        GlobalValues.activeDownloadIdsByQuestion[questionId] = activeDownloadIds.toMutableSet()
        QuestionDownloadForegroundService.cancelDownload(
            requireContext(),
            questionId = questionId,
            messageId = message.id
        )
        notifyMessageChanged(message.id)
    }

    /** Arka plan gönderimi için dosyayı kalıcı dizine kopyalar (Service okuyabilsin). */
    private fun copyToUploadsDir(sourceFile: File?, sourceUri: Uri?, prefix: String, extension: String): File? {
        val dir = File(requireContext().filesDir, "question_uploads").apply { mkdirs() }
        val dest = File(dir, "${prefix}_${System.currentTimeMillis()}.$extension")
        return try {
            when {
                sourceFile != null && sourceFile.exists() -> {
                    sourceFile.copyTo(dest, overwrite = true)
                    dest
                }
                sourceUri != null -> requireContext().contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                    dest
                } ?: run { dest.delete(); null }
                else -> null
            }
        } catch (_: Exception) {
            dest.delete()
            null
        }
    }

    private fun startUploadService(
        clientId: String,
        type: String,
        filePath: String? = null,
        textContent: String? = null,
        caption: String? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        val role = authManager.getCurrentUserType()
        val appContext = requireContext().applicationContext

        GlobalValues.uploadMetaByClientId[clientId] = PendingUploadMeta(
            questionId = questionId,
            type = type,
            filePath = filePath,
            textContent = textContent,
            caption = caption
        )

        val intent = Intent(appContext, QuestionUploadForegroundService::class.java).apply {
            putExtra(QuestionUploadForegroundService.KEY_QUESTION_ID, questionId)
            putExtra(QuestionUploadForegroundService.KEY_TYPE, type)
            putExtra(QuestionUploadForegroundService.KEY_CLIENT_ID, clientId)
            putExtra(QuestionUploadForegroundService.KEY_SENDER_UID, uid)
            putExtra(QuestionUploadForegroundService.KEY_SENDER_ROLE, role)
            filePath?.let { putExtra(QuestionUploadForegroundService.KEY_FILE_PATH, it) }
            textContent?.let { putExtra(QuestionUploadForegroundService.KEY_TEXT_CONTENT, it) }
            caption?.let { putExtra(QuestionUploadForegroundService.KEY_CAPTION, it) }
        }
        QuestionUploadForegroundService.start(appContext, intent)
    }

    private val maxAudioDurationMs = 5 * 60 * 1000L // 5 dakika

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadVideoAndSend(it) }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadImageUriAndSend(it) }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.RECORD_AUDIO] == true) startAudioRecording()
        else Toast.makeText(requireContext(), "Ses kaydı için izin gerekli.", Toast.LENGTH_SHORT).show()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        questionId = arguments?.getString(ARG_QUESTION_ID) ?: return
        isTeacher = authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER

        // Global buffer'lardan pending ve durum set'lerini geri yükle
        GlobalValues.pendingQuestionMessages[questionId]?.let { list ->
            pendingMessages.clear()
            pendingMessages.addAll(list)
        }
        GlobalValues.revealedMediaIdsByQuestion[questionId]?.let { set ->
            revealedMediaMessageIds.clear()
            revealedMediaMessageIds.addAll(set)
        }
        GlobalValues.canceledUploadIdsByQuestion[questionId]?.let { set ->
            canceledUploadIds.clear()
            canceledUploadIds.addAll(set)
        }
        GlobalValues.activeUploadIdsByQuestion[questionId]?.let { set ->
            activeUploadIds.clear()
            activeUploadIds.addAll(set)
        }
        GlobalValues.activeDownloadIdsByQuestion[questionId]?.let { set ->
            activeDownloadIds.clear()
            activeDownloadIds.addAll(set)
        }

        // Öğrenciler sadece metin ve ses gönderebilsin; galeri butonunu gizle.
        if (isTeacher) {
            binding.galleryButton.visibility = View.VISIBLE
        } else {
            binding.galleryButton.visibility = View.GONE
        }

        binding.backButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        binding.questionScreenshotButton.visibility = View.VISIBLE
        binding.questionScreenshotButton.setOnClickListener {
            val fragment = QuestionDetailFragment.newInstance(
                questionId = questionId,
                studentView = !isTeacher
            )
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerID, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.resolveButton.visibility = if (isTeacher) View.VISIBLE else View.GONE
        binding.recordAudioButton.visibility = View.VISIBLE

        val currentUidForRestriction = auth.currentUser?.uid
        if (currentUidForRestriction != null) {
            firestore.collection("users").document(currentUidForRestriction).get()
                .addOnSuccessListener { userDoc ->
                    val banned = userDoc.getBoolean("banned") == true
                    val restrictedUntil = userDoc.getTimestamp("restrictedUntil")
                    val now = Timestamp.now()
                    if (banned || (restrictedUntil != null && restrictedUntil.compareTo(now) > 0)) {
                        this@QuestionChatFragment.isUserRestrictedOrBanned = true
                        binding.inputBar.visibility = View.GONE
                        binding.restrictionMessage.visibility = View.VISIBLE
                    }
                }
        }

        firestore.collection("questions").document(questionId).get()
            .addOnSuccessListener { doc ->
                questionStatus = doc.getString("status") ?: StudentQuestion.STATUS_CLAIMED
                if (isTeacher) applyResolveButtonIcon(questionStatus)
            }

        val currentUid = auth.currentUser?.uid ?: ""
        chatAdapter = ChatMessageAdapter(
            currentUserUid = currentUid,
            revealedMessageIds = revealedMediaMessageIds,
            canceledUploadIds = canceledUploadIds,
            activeUploadIds = activeUploadIds,
            activeDownloadIds = activeDownloadIds,
            uploadProgress = uploadProgress,
            downloadProgress = downloadProgress,
            onRevealMedia = { msg ->
                revealedMediaMessageIds.add(msg.id)
                GlobalValues.revealedMediaIdsByQuestion[questionId] =
                    revealedMediaMessageIds.toMutableSet()
                submitMergedList()
            },
            onStartDownload = { msg -> startDownloadForMessage(msg) },
            onCancelDownload = { msg -> cancelDownloadForMessage(msg) },
            onCancelUpload = { msg ->
                canceledUploadIds.add(msg.id)
                GlobalValues.canceledUploadIdsByQuestion[questionId] =
                    canceledUploadIds.toMutableSet()
                QuestionUploadForegroundService.cancelUpload(
                    requireContext().applicationContext,
                    msg.id
                )
                submitMergedList()
            },
            onRetryUpload = { msg ->
                val meta = GlobalValues.uploadMetaByClientId[msg.id]
                if (meta != null && meta.questionId == questionId) {
                    canceledUploadIds.remove(msg.id)
                    GlobalValues.canceledUploadIdsByQuestion[questionId] =
                        canceledUploadIds.toMutableSet()
                    startUploadService(
                        clientId = msg.id,
                        type = meta.type,
                        filePath = meta.filePath,
                        textContent = meta.textContent,
                        caption = meta.caption
                    )
                    submitMergedList()
                } else {
                    Toast.makeText(requireContext(), "Tekrar gönderilemedi.", Toast.LENGTH_SHORT).show()
                }
            },
            onPlayVideo = { url: String -> showVideoDialog(url) },
            onPlayAudio = { url: String -> playAudioInApp(url) },
            onImageClick = { url: String -> showImageDialog(url) },
            onMessageLongClick = { msg -> showDeleteMessageDialog(msg) }
        )
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.messagesRecyclerView.adapter = chatAdapter

        listener = firestore.collection("questions").document(questionId).collection("messages")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val rawList = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(QuestionMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                val uid = auth.currentUser?.uid
                val list = if (uid != null) {
                    rawList.filter { m -> m.deletedForUids?.contains(uid) != true }
                } else rawList
                if (uid != null) {
                    for (m in rawList) {
                        if (m.senderUid != uid && m.id.isNotEmpty() && !m.id.startsWith("pending_") && m.deletedForUids?.contains(uid) != true) {
                            val updates = mutableMapOf<String, Any>()
                            if (m.deliveredAt == null) updates["deliveredAt"] = Timestamp.now()
                            if (m.readAt == null) updates["readAt"] = Timestamp.now()
                            if (updates.isNotEmpty()) {
                                firestore.collection("questions").document(questionId)
                                    .collection("messages").document(m.id).update(updates)
                            }
                        }
                    }
                }
                serverMessageList = list
                pendingMessages.removeAll { pending -> list.any { it.clientId == pending.id } }
                submitMergedList()

                // Sadece zaten alta yakınsak otomatik en alta kaydır.
                val lm = binding.messagesRecyclerView.layoutManager as? LinearLayoutManager
                if (lm != null) {
                    val lastVisible = lm.findLastVisibleItemPosition()
                    val totalCount = (serverMessageList + pendingMessages).size
                    // Kullanıcı son 3 mesaj içinde ise alta kaydır, yoksa pozisyonu koru.
                    if (lastVisible >= totalCount - 4) {
                        binding.messagesRecyclerView.post {
                            binding.messagesRecyclerView.smoothScrollToPosition(totalCount)
                        }
                    }
                }
            }

        // Başlangıç durumu: sadece mikrofon görünür
        binding.recordAudioButton.visibility = View.VISIBLE
        binding.recordAudioButton.scaleX = 1f
        binding.recordAudioButton.scaleY = 1f
        binding.recordAudioButton.alpha = 1f
        binding.sendTextButton.visibility = View.GONE
        binding.sendTextButton.scaleX = 0f
        binding.sendTextButton.scaleY = 0f
        binding.sendTextButton.alpha = 0f

        binding.sendTextButton.setOnClickListener { handleResolvedBlockOrRun { sendTextMessage() } }
        binding.recordAudioButton.setOnClickListener { handleResolvedBlockOrRun { toggleAudioRecording() } }
        binding.galleryButton.setOnClickListener { handleResolvedBlockOrRun { openGalleryBottomSheet() } }
        binding.resolveButton.setOnClickListener {
            if (questionStatus == StudentQuestion.STATUS_RESOLVED) showUnresolveConfirmationDialog()
            else showResolveConfirmationDialog()
        }

        binding.audioBarPlayPause.setOnClickListener {
            if (isRecordingAudio) {
                // Kayıt modunda: duraklat / devam et
                if (isRecordingPaused) resumeAudioRecordingInternal() else pauseAudioRecordingInternal()
            } else {
                // Oynatma modunda: play / pause
                if (audioMediaPlayer?.isPlaying == true) {
                    audioMediaPlayer?.pause()
                    binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    audioMediaPlayer?.start()
                    binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    audioProgressHandler.post(audioProgressRunnable)
                }
            }
        }
        binding.audioBarStop.setOnClickListener {
            if (isRecordingAudio) {
                // Kaydı sil ve paneli kapat
                finishAudioRecording(send = false)
            } else {
                releaseAudioAndHideBar()
            }
        }
        binding.audioBarClose.setOnClickListener { releaseAudioAndHideBar() }
        binding.audioBarSend.setOnClickListener {
            if (isRecordingAudio) {
                finishAudioRecording(send = true)
            }
        }

        binding.captionBarThumbContainer.setOnClickListener { clearSelectedMediaAndHideCaptionBar() }
        binding.captionSendButton.setOnClickListener { handleResolvedBlockOrRun { sendSelectedMediaWithCaption() } }

        // Metin duruma göre mic / send animasyonlu geçiş
        binding.textInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = !s.isNullOrEmpty()
                if (hasText && binding.sendTextButton.visibility != View.VISIBLE) {
                    // Mic -> Send
                    binding.recordAudioButton.animate().cancel()
                    binding.sendTextButton.animate().cancel()

                    binding.recordAudioButton.animate()
                        .scaleX(0f).scaleY(0f).alpha(0f)
                        .setDuration(250)
                        .withEndAction {
                            binding.recordAudioButton.visibility = View.GONE
                        }
                        .start()

                    binding.sendTextButton.visibility = View.VISIBLE
                    binding.sendTextButton.scaleX = 0f
                    binding.sendTextButton.scaleY = 0f
                    binding.sendTextButton.alpha = 0f
                    binding.sendTextButton.animate()
                        .scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(250)
                        .start()
                } else if (!hasText && binding.recordAudioButton.visibility != View.VISIBLE) {
                    // Send -> Mic
                    binding.recordAudioButton.animate().cancel()
                    binding.sendTextButton.animate().cancel()

                    binding.sendTextButton.animate()
                        .scaleX(0f).scaleY(0f).alpha(0f)
                        .setDuration(250)
                        .withEndAction {
                            binding.sendTextButton.visibility = View.GONE
                        }
                        .start()

                    binding.recordAudioButton.visibility = View.VISIBLE
                    binding.recordAudioButton.scaleX = 0f
                    binding.recordAudioButton.scaleY = 0f
                    binding.recordAudioButton.alpha = 0f
                    binding.recordAudioButton.animate()
                        .scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(250)
                        .start()
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        // Chat ekranındayken üstteki currency paneli gizle
        activity?.findViewById<View>(R.id.currencyPanel)?.visibility = View.GONE
        if (!uploadStartedReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(QuestionUploadForegroundService.ACTION_UPLOAD_STARTED)
                addAction(QuestionUploadForegroundService.ACTION_UPLOAD_CANCELED)
                addAction(QuestionDownloadForegroundService.ACTION_DOWNLOAD_STARTED)
                addAction(QuestionDownloadForegroundService.ACTION_DOWNLOAD_COMPLETED)
                addAction(QuestionDownloadForegroundService.ACTION_DOWNLOAD_FAILED)
                addAction(QuestionDownloadForegroundService.ACTION_DOWNLOAD_CANCELED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(
                    uploadReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                requireContext().registerReceiver(uploadReceiver, filter)
            }
            uploadStartedReceiverRegistered = true
        }
    }

    override fun onStop() {
        if (uploadStartedReceiverRegistered) {
            requireContext().unregisterReceiver(uploadReceiver)
            uploadStartedReceiverRegistered = false
        }
        // Chat'ten çıkınca currency panelini geri göster
        activity?.findViewById<View>(R.id.currencyPanel)?.visibility = View.VISIBLE
        super.onStop()
    }

    private fun showDeleteMessageDialog(message: QuestionMessage) {
        val view = layoutInflater.inflate(R.layout.dialog_delete_message, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        val deleteForEveryoneBtn = view.findViewById<View>(R.id.deleteForEveryone)
        val reportMessageBtn = view.findViewById<View>(R.id.reportMessage)
        val currentUid = auth.currentUser?.uid
        val isMyMessage = currentUid != null && message.senderUid == currentUid
        deleteForEveryoneBtn.visibility = if (isMyMessage) View.VISIBLE else View.GONE
        reportMessageBtn.visibility = if (isMyMessage) View.GONE else View.VISIBLE
        deleteForEveryoneBtn.setOnClickListener {
            dialog.dismiss()
            deleteMessageForEveryone(message.id)
        }
        view.findViewById<View>(R.id.deleteForMe).setOnClickListener {
            dialog.dismiss()
            deleteMessageForMe(message.id)
        }
        reportMessageBtn.setOnClickListener {
            dialog.dismiss()
            reportMessageToFirestore(message)
            Toast.makeText(requireContext(), "Bildiriminiz alındı.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.deleteCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun deleteMessageForEveryone(messageId: String) {
        if (messageId.startsWith("pending_")) {
            cancelPendingSend(messageId)
            Toast.makeText(requireContext(), "Gönderim iptal edildi.", Toast.LENGTH_SHORT).show()
            return
        }
        firestore.collection("questions").document(questionId)
            .collection("messages").document(messageId).delete()
            .addOnSuccessListener { Toast.makeText(requireContext(), "Mesaj herkesten silindi.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Silinemedi: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun deleteMessageForMe(messageId: String) {
        if (messageId.startsWith("pending_")) {
            cancelPendingSend(messageId)
            Toast.makeText(requireContext(), "Gönderim iptal edildi.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("questions").document(questionId)
            .collection("messages").document(messageId)
            .update("deletedForUids", FieldValue.arrayUnion(uid))
            .addOnSuccessListener { Toast.makeText(requireContext(), "Mesaj sizden silindi.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Silinemedi: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun reportMessageToFirestore(message: QuestionMessage) {
        val reportedByUid = auth.currentUser?.uid ?: return
        val messagePreview = when (message.type) {
            QuestionMessage.TYPE_TEXT -> (message.textContent?.take(100) ?: "").ifEmpty { "-" }
            QuestionMessage.TYPE_IMAGE -> "Fotoğraf"
            QuestionMessage.TYPE_VIDEO -> "Video"
            QuestionMessage.TYPE_AUDIO -> "Ses"
            else -> message.textContent?.take(100) ?: message.type
        }
        val report = hashMapOf(
            "questionId" to questionId,
            "messageId" to message.id,
            "reportedByUid" to reportedByUid,
            "reportedUserUid" to message.senderUid,
            "messagePreview" to messagePreview,
            "reportedAt" to Timestamp.now(),
            "status" to "pending"
        )
        firestore.collection("messageReports").add(report)
    }

    private var pendingMediaUri: Uri? = null
    private var pendingMediaType: String? = null

    private fun openGalleryBottomSheet() {
        if (isUserRestrictedOrBanned) {
            Toast.makeText(requireContext(), "Hesabınız kısıtlanmıştır. Mesaj gönderemezsiniz.", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO), 100)
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
                return
            }
        }
        val sheet = GalleryBottomSheetFragment.newInstance()
        sheet.onMediaSelected = { uri, type ->
            pendingMediaUri = uri
            pendingMediaType = type
            Glide.with(this).load(uri).centerCrop().into(binding.captionBarThumb)
            binding.captionInput.text.clear()
            binding.captionBar.visibility = View.VISIBLE
            binding.inputBar.visibility = View.GONE
        }
        sheet.show(parentFragmentManager, "gallery")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            openGalleryBottomSheet()
        } else if (requestCode == 100) {
            Toast.makeText(requireContext(), "Galeriye erişim izni gerekli.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearSelectedMediaAndHideCaptionBar() {
        pendingMediaUri = null
        pendingMediaType = null
        binding.captionBar.visibility = View.GONE
        binding.inputBar.visibility = View.VISIBLE
    }

    private fun sendSelectedMediaWithCaption() {
        val uri = pendingMediaUri ?: return
        val type = pendingMediaType ?: return
        val caption = binding.captionInput.text.toString().trim()
        clearSelectedMediaAndHideCaptionBar()
        binding.messagesRecyclerView.post {
            val last = (chatAdapter?.itemCount ?: 1) - 1
            if (last >= 0) binding.messagesRecyclerView.smoothScrollToPosition(last)
        }
        when (type) {
            GalleryMediaItem.TYPE_IMAGE -> uploadImageUriAndSendWithCaption(uri, caption, null)
            GalleryMediaItem.TYPE_VIDEO -> uploadVideoAndSendWithCaption(uri, caption, null)
            else -> { }
        }
    }

    private fun showVideoDialog(url: String) {
        if (url.isBlank()) {
            Toast.makeText(requireContext(), "Video adresi bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }
        videoDialog?.dismiss()
        releaseVideoPlayer()
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(R.layout.dialog_video_player)
            window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        val playerView = dialog.findViewById<PlayerView>(R.id.dialogPlayerView)
        val player = ExoPlayer.Builder(requireContext()).build().also {
            videoExoPlayer = it
        }
        playerView.player = player
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(requireContext(), "Video oynatılamadı.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        })
        dialog.findViewById<View>(R.id.dialogVideoClose).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            releaseVideoPlayer()
            playerView.player = null
        }
        dialog.show()
        videoDialog = dialog
    }

    private fun releaseVideoPlayer() {
        videoExoPlayer?.release()
        videoExoPlayer = null
    }

    private fun showImageDialog(url: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(R.layout.dialog_image)
            window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        val imageView = dialog.findViewById<android.widget.ImageView>(R.id.dialogImageView)
        Glide.with(this).load(url).transition(DrawableTransitionOptions.withCrossFade()).into(imageView)
        dialog.findViewById<View>(R.id.dialogImageClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun playAudioInApp(url: String) {
        if (audioMediaPlayer != null) {
            if (playingAudioUrl == url) {
                if (audioMediaPlayer?.isPlaying == true) {
                    audioMediaPlayer?.pause()
                    binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    audioMediaPlayer?.start()
                    binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                }
                return
            }
            releaseAudioAndHideBar()
        }
        playingAudioUrl = url
        isRecordingAudio = false
        isRecordingPaused = false
        recordingHandler.removeCallbacks(recordingRunnable)

        binding.audioBar.visibility = View.VISIBLE
        // Oynatma panelinde inputBar görünür kalsın (sadece kayıtta gizlenir)
        binding.audioBarCurrentTime.text = "0:00"
        binding.audioBarTotalTime.text = "0:00"
        binding.audioBarWaveform.progress = 0f
        binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        // Oynatma panelinde X (kapat) butonu görünsün, stop görünmesin
        binding.audioBarStop.visibility = View.GONE
        binding.audioBarClose.visibility = View.VISIBLE
        binding.audioBarSend.visibility = View.GONE
        audioMediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), Uri.parse(url))
            isLooping = false
            setOnPreparedListener { mp ->
                binding.audioBarTotalTime.text = formatAudioTime(mp.duration)
                start()
                audioProgressHandler.post(audioProgressRunnable)
            }
            setOnCompletionListener { releaseAudioAndHideBar() }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(requireContext(), "Ses oynatılamadı.", Toast.LENGTH_SHORT).show()
                releaseAudioAndHideBar()
                true
            }
            prepareAsync()
        }
    }

    private var playingAudioUrl: String? = null

    private val audioProgressHandler = Handler(Looper.getMainLooper())
    private val audioProgressRunnable = object : Runnable {
        override fun run() {
            val mp = audioMediaPlayer ?: return
            val dur = mp.duration
            val pos = mp.currentPosition
            if (dur > 0) {
                binding.audioBarCurrentTime.text = formatAudioTime(pos)
                binding.audioBarTotalTime.text = formatAudioTime(dur)
                binding.audioBarWaveform.progress = pos / dur.toFloat()
            }
            if (mp.isPlaying) audioProgressHandler.postDelayed(this, 200)
        }
    }

    private fun formatAudioTime(ms: Int): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%d:%02d".format(m, s)
    }

    private fun releaseAudioAndHideBar() {
        audioProgressHandler.removeCallbacks(audioProgressRunnable)
        try {
            audioMediaPlayer?.release()
        } catch (_: Exception) { }
        audioMediaPlayer = null
        playingAudioUrl = null
        binding.audioBar.visibility = View.GONE
        binding.audioBarSend.visibility = View.GONE
        binding.audioBarClose.visibility = View.GONE
        binding.inputBar.visibility = View.VISIBLE
    }

    private fun sendTextMessage() {
        val text = binding.textInput.text.toString().trim()
        if (text.isEmpty()) return
        val uid = auth.currentUser?.uid ?: return
        val role = authManager.getCurrentUserType()
        binding.textInput.text.clear()
        val clientId = "pending_${System.currentTimeMillis()}"
        val pending = QuestionMessage(
            id = clientId,
            senderUid = uid,
            senderRole = role,
            type = QuestionMessage.TYPE_TEXT,
            textContent = text,
            createdAt = Timestamp.now()
        )
        addPendingMessage(pending)
        binding.messagesRecyclerView.post {
            val last = (chatAdapter?.itemCount ?: 1) - 1
            if (last >= 0) binding.messagesRecyclerView.smoothScrollToPosition(last)
        }
        startUploadService(clientId, QuestionMessage.TYPE_TEXT, textContent = text)
    }

    private fun toggleAudioRecording() {
        if (isUserRestrictedOrBanned) {
            Toast.makeText(requireContext(), "Hesabınız kısıtlanmıştır. Mesaj gönderemezsiniz.", Toast.LENGTH_SHORT).show()
            return
        }
        // Kayıt paneli varken kontrol panelden yapılır; mic'e tekrar basmayı yok say.
        if (isRecordingAudio) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return
        }
        startAudioRecording()
    }

    private fun startAudioRecording() {
        val dir = File(requireContext().cacheDir, "audio").apply { mkdirs() }
        audioFile = File(dir, "msg_${System.currentTimeMillis()}.m4a")
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            setMaxDuration(maxAudioDurationMs.toInt())
            prepare()
            start()
        }
        isRecordingAudio = true
        isRecordingPaused = false
        recordedMsBeforePause = 0L
        recordingStartMs = System.currentTimeMillis()

        // Kayıt panelini göster
        binding.audioBar.visibility = View.VISIBLE
        binding.inputBar.visibility = View.GONE
        binding.audioBarCurrentTime.text = "0:00"
        binding.audioBarTotalTime.text = formatAudioTime(maxAudioDurationMs.toInt())
        binding.audioBarWaveform.progress = 0f
        binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        // Kayıt panelinde stop butonu görünsün, X (kapat) görünmesin
        binding.audioBarStop.visibility = View.VISIBLE
        binding.audioBarClose.visibility = View.GONE
        binding.audioBarSend.visibility = View.VISIBLE

        recordingHandler.post(recordingRunnable)
    }

    private fun pauseAudioRecordingInternal() {
        if (!isRecordingAudio || isRecordingPaused) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
            }
        } catch (_: Exception) { }
        recordedMsBeforePause += System.currentTimeMillis() - recordingStartMs
        isRecordingPaused = true
        binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun resumeAudioRecordingInternal() {
        if (!isRecordingAudio || !isRecordingPaused) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
            }
        } catch (_: Exception) { }
        recordingStartMs = System.currentTimeMillis()
        isRecordingPaused = false
        binding.audioBarPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        recordingHandler.post(recordingRunnable)
    }

    private fun finishAudioRecording(send: Boolean) {
        recordingHandler.removeCallbacks(recordingRunnable)
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) { }
        mediaRecorder?.release()
        mediaRecorder = null

        isRecordingAudio = false
        isRecordingPaused = false
        binding.audioBarSend.visibility = View.GONE
        binding.audioBarClose.visibility = View.GONE
        binding.audioBar.visibility = View.GONE
        binding.inputBar.visibility = View.VISIBLE

        if (send) {
            audioFile?.let { uploadAudioAndSend(it) }
        } else {
            audioFile?.let { runCatching { it.delete() } }
        }
        audioFile = null
    }

    private fun uploadAudioAndSend(file: File) {
        if (!file.exists()) return
        val uid = auth.currentUser?.uid ?: return
        val role = authManager.getCurrentUserType()
        val clientId = "pending_${System.currentTimeMillis()}"
        val pending = QuestionMessage(
            id = clientId,
            senderUid = uid,
            senderRole = role,
            type = QuestionMessage.TYPE_AUDIO,
            createdAt = Timestamp.now()
        )
        addPendingMessage(pending)
        val dest = copyToUploadsDir(file, null, "audio", "m4a") ?: run {
            removePendingByClientId(clientId)
            Toast.makeText(requireContext(), "Ses kopyalanamadı.", Toast.LENGTH_SHORT).show()
            return
        }
        startUploadService(clientId, QuestionMessage.TYPE_AUDIO, filePath = dest.absolutePath)
    }

    private fun uploadVideoAndSend(uri: Uri) {
        uploadVideoUriAndSend(uri, "")
    }

    private fun uploadVideoUriAndSend(uri: Uri, caption: String, onSuccess: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        val role = authManager.getCurrentUserType()
        val clientId = "pending_${System.currentTimeMillis()}"
        val pending = QuestionMessage(
            id = clientId,
            senderUid = uid,
            senderRole = role,
            type = QuestionMessage.TYPE_VIDEO,
            textContent = caption.takeIf { it.isNotEmpty() },
            createdAt = Timestamp.now()
        )
        addPendingMessage(pending)
        val dest = copyToUploadsDir(null, uri, "video", "mp4") ?: run {
            removePendingByClientId(clientId)
            Toast.makeText(requireContext(), "Video açılamadı.", Toast.LENGTH_SHORT).show()
            return
        }
        startUploadService(clientId, QuestionMessage.TYPE_VIDEO, filePath = dest.absolutePath, caption = caption.takeIf { it.isNotEmpty() })
        onSuccess?.invoke()
    }

    private fun uploadImageUriAndSend(uri: Uri) {
        uploadImageUriAndSendWithCaption(uri, "")
    }

    private fun uploadImageUriAndSendWithCaption(uri: Uri, caption: String, onSuccess: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        val role = authManager.getCurrentUserType()
        val clientId = "pending_${System.currentTimeMillis()}"
        val pending = QuestionMessage(
            id = clientId,
            senderUid = uid,
            senderRole = role,
            type = QuestionMessage.TYPE_IMAGE,
            mediaUrl = uri.toString(),
            textContent = caption.takeIf { it.isNotEmpty() },
            createdAt = Timestamp.now()
        )
        addPendingMessage(pending)
        val dest = copyToUploadsDir(null, uri, "img", "jpg") ?: run {
            removePendingByClientId(clientId)
            Toast.makeText(requireContext(), "Resim açılamadı.", Toast.LENGTH_SHORT).show()
            return
        }
        startUploadService(clientId, QuestionMessage.TYPE_IMAGE, filePath = dest.absolutePath, caption = caption.takeIf { it.isNotEmpty() })
        onSuccess?.invoke()
    }

    private fun uploadVideoAndSendWithCaption(uri: Uri, caption: String, onSuccess: (() -> Unit)? = null) {
        uploadVideoUriAndSend(uri, caption, onSuccess)
    }

    private fun applyResolveButtonIcon(status: String) {
        binding.resolveButton.setImageResource(
            if (status == StudentQuestion.STATUS_RESOLVED) R.drawable.lock_ic
            else R.drawable.unlock_ic
        )
    }

    private fun showResolveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("Bu soruyu çözüldü olarak işaretlemek istiyor musun? Eğer işaretlersen öğrenci mesaj gönderemeyecek.")
            .setNegativeButton("İşaretle") { dialog, _ ->
                dialog.dismiss()
                markResolved()
            }
            .setPositiveButton("İptal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showUnresolveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("Bu soru çözüldü olarak işaretli. Tekrar aktifleştirmek istiyor musunuz?")
            .setNegativeButton("Aktifleştir") { dialog, _ ->
                dialog.dismiss()
                markUnresolved()
            }
            .setPositiveButton("İptal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** Çözülmüş soruda mesaj/gönder butonlarına basıldığında: mesaj göster, öğretmense tekrar aktifleştir paneli aç. */
    private fun handleResolvedBlockOrRun(action: () -> Unit) {
        if (questionStatus != StudentQuestion.STATUS_RESOLVED) {
            action()
            return
        }
        Toast.makeText(requireContext(), "Çözülen sorulara mesaj gönderemezsin.", Toast.LENGTH_SHORT).show()
        if (isTeacher) showUnresolveConfirmationDialog()
    }

    private fun markResolved() {
        val updates = hashMapOf<String, Any>(
            "status" to StudentQuestion.STATUS_RESOLVED,
            "resolvedAt" to Timestamp.now()
        )
        firestore.collection("questions").document(questionId).update(updates)
            .addOnSuccessListener {
                questionStatus = StudentQuestion.STATUS_RESOLVED
                applyResolveButtonIcon(questionStatus)
                Toast.makeText(requireContext(), "Soru çözüldü olarak işaretlendi.", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
    }

    private fun markUnresolved() {
        firestore.collection("questions").document(questionId).update("status", StudentQuestion.STATUS_CLAIMED)
            .addOnSuccessListener {
                questionStatus = StudentQuestion.STATUS_CLAIMED
                applyResolveButtonIcon(questionStatus)
                Toast.makeText(requireContext(), "Soru tekrar aktifleştirildi.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        if (isRecordingAudio) {
            // Görünür fragment kapatılırken aktif kaydı sessizce iptal et
            finishAudioRecording(send = false)
        }
        releaseAudioAndHideBar()
        videoDialog?.dismiss()
        videoDialog = null
        releaseVideoPlayer()
        listener?.remove()
        chatAdapter = null
        _binding = null
        super.onDestroyView()
    }

    /** Used by FCM service to skip notification when user is already on this chat. */
    fun getQuestionIdOrNull(): String? = questionId.takeIf { it.isNotEmpty() }

    companion object {
        private const val ARG_QUESTION_ID = "question_id"
        fun newInstance(questionId: String) = QuestionChatFragment().apply {
            arguments = Bundle().apply { putString(ARG_QUESTION_ID, questionId) }
        }
    }
}

private fun formatFileSize(bytes: Long?): String {
    val b = bytes ?: return "—"
    if (b <= 0) return "—"
    return when {
        b >= 1024L * 1024 * 1024 -> "%.1f GB".format(b / (1024.0 * 1024 * 1024))
        b >= 1024L * 1024 -> "%.1f MB".format(b / (1024.0 * 1024))
        b >= 1024 -> "%.0f KB".format(b / 1024.0)
        else -> "$b B"
    }
}

private class ChatMessageAdapter(
    private val currentUserUid: String,
    private val revealedMessageIds: Set<String>,
    private val canceledUploadIds: Set<String>,
    private val activeUploadIds: Set<String>,
    private val activeDownloadIds: Set<String>,
    private val uploadProgress: Map<String, Int>,
    private val downloadProgress: Map<String, Int>,
    private val onRevealMedia: (QuestionMessage) -> Unit,
    private val onCancelUpload: (QuestionMessage) -> Unit,
    private val onRetryUpload: (QuestionMessage) -> Unit,
    private val onPlayVideo: (String) -> Unit,
    private val onPlayAudio: (String) -> Unit,
    private val onImageClick: (String) -> Unit,
    private val onMessageLongClick: (QuestionMessage) -> Unit,
    private val onStartDownload: (QuestionMessage) -> Unit,
    private val onCancelDownload: (QuestionMessage) -> Unit
) : RecyclerView.Adapter<ChatMessageAdapter.VH>() {
    private var items = listOf<QuestionMessage>()

    fun submitList(list: List<QuestionMessage>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(
        items[position],
        currentUserUid,
        revealedMessageIds,
        canceledUploadIds,
        activeUploadIds,
        activeDownloadIds,
        uploadProgress,
        downloadProgress,
        onRevealMedia,
        onCancelUpload,
        onRetryUpload,
        onPlayVideo,
        onPlayAudio,
        onImageClick,
        onMessageLongClick,
        onStartDownload,
        onCancelDownload
    )
    override fun getItemCount() = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContainer = itemView.findViewById<View>(R.id.messageContainer)
        private val bubbleContainer = itemView.findViewById<View>(R.id.bubbleContainer)
        private val messageText = itemView.findViewById<TextView>(R.id.messageText)
        private val audioRow = itemView.findViewById<View>(R.id.audioRow)
        private val audioLabel = itemView.findViewById<TextView>(R.id.audioLabel)
        private val videoRow = itemView.findViewById<View>(R.id.videoRow)
        private val videoThumb = itemView.findViewById<ImageView>(R.id.videoThumb)
        private val videoMetaContainer = itemView.findViewById<View>(R.id.videoMetaContainer)
        private val videoTimeView = itemView.findViewById<TextView>(R.id.videoTimeView)
        private val videoTickView = itemView.findViewById<ImageView>(R.id.videoTickView)
        private val videoBlurOverlay = itemView.findViewById<View>(R.id.videoBlurOverlay)
        private val videoCenterActionContainer = itemView.findViewById<View>(R.id.videoCenterActionContainer)
        private val videoDownloadIcon = itemView.findViewById<ImageView>(R.id.videoDownloadIcon)
        private val videoSizeText = itemView.findViewById<TextView>(R.id.videoSizeText)
        private val videoCornerActionContainer = itemView.findViewById<View>(R.id.videoCornerActionContainer)
        private val videoCornerProgress = itemView.findViewById<ProgressBar>(R.id.videoCornerProgress)
        private val videoCornerActionIcon = itemView.findViewById<ImageView>(R.id.videoCornerActionIcon)
        private val videoCornerActionText = itemView.findViewById<TextView>(R.id.videoCornerActionText)
        private val imageContainer = itemView.findViewById<View>(R.id.imageContainer)
        private val imageThumb = itemView.findViewById<android.widget.ImageView>(R.id.imageThumb)
        private val imageMetaContainer = itemView.findViewById<View>(R.id.imageMetaContainer)
        private val imageTimeView = itemView.findViewById<TextView>(R.id.imageTimeView)
        private val imageTickView = itemView.findViewById<ImageView>(R.id.imageTickView)
        private val imageBlurOverlay = itemView.findViewById<View>(R.id.imageBlurOverlay)
        private val imageCenterActionContainer = itemView.findViewById<View>(R.id.imageCenterActionContainer)
        private val imageDownloadIcon = itemView.findViewById<ImageView>(R.id.imageDownloadIcon)
        private val imageSizeText = itemView.findViewById<TextView>(R.id.imageSizeText)
        private val imageCornerActionContainer = itemView.findViewById<View>(R.id.imageCornerActionContainer)
        private val imageCornerProgress = itemView.findViewById<ProgressBar>(R.id.imageCornerProgress)
        private val imageCornerActionIcon = itemView.findViewById<ImageView>(R.id.imageCornerActionIcon)
        private val imageCornerActionText = itemView.findViewById<TextView>(R.id.imageCornerActionText)
        private val timeView = itemView.findViewById<TextView>(R.id.timeView)
        private val tickView = itemView.findViewById<ImageView>(R.id.tickView)
        private val captionText = itemView.findViewById<TextView>(R.id.captionText)

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(
            m: QuestionMessage,
            currentUserUid: String,
            revealedMessageIds: Set<String>,
            canceledUploadIds: Set<String>,
            activeUploadIds: Set<String>,
            activeDownloadIds: Set<String>,
            uploadProgress: Map<String, Int>,
            downloadProgress: Map<String, Int>,
            onRevealMedia: (QuestionMessage) -> Unit,
            onCancelUpload: (QuestionMessage) -> Unit,
            onRetryUpload: (QuestionMessage) -> Unit,
            onPlayVideo: (String) -> Unit,
            onPlayAudio: (String) -> Unit,
            onImageClick: (String) -> Unit,
            onMessageLongClick: (QuestionMessage) -> Unit,
            onStartDownload: (QuestionMessage) -> Unit,
            onCancelDownload: (QuestionMessage) -> Unit
        ) {
            bubbleContainer.setOnLongClickListener { onMessageLongClick(m); true }
            val isFromMe = m.senderUid == currentUserUid
            val maxWidthPx = itemView.resources.getDimensionPixelSize(R.dimen.chat_bubble_max_width)
            val params = messageContainer.layoutParams as? android.widget.FrameLayout.LayoutParams
            if (params != null) {
                params.width = maxWidthPx
                params.gravity = if (isFromMe) android.view.Gravity.END else android.view.Gravity.START
                messageContainer.layoutParams = params
            }

            val bubbleBg = if (isFromMe) R.drawable.chat_bubble_me else R.drawable.hint_background
            val textColor = if (isFromMe) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            bubbleContainer.setBackgroundResource(bubbleBg)
            messageText.setTextColor(textColor)
            audioLabel.setTextColor(textColor)
            captionText.setTextColor(textColor)

            val timeText = m.createdAt?.toDate()?.let { timeFormat.format(it) } ?: ""
            timeView.text = timeText
            timeView.setTextColor(if (isFromMe) 0xFF666666.toInt() else 0xCCFFFFFF.toInt())
            videoTimeView.text = timeText
            imageTimeView.text = timeText

            val isPending = m.isPending()
            val isCanceled = m.id in canceledUploadIds
            val isDownloading = !isFromMe && m.id in activeDownloadIds
            val currentUploadProgress = uploadProgress[m.id] ?: 0
            val downloadedLocalPath = GlobalValues.downloadedMediaByMessageId[m.id]?.takeIf { path ->
                !path.isNullOrBlank() && java.io.File(path).exists()
            }
            val isDownloaded = !isFromMe && downloadedLocalPath != null
            val currentDownloadProgress = downloadProgress[m.id] ?: 0

            if (isFromMe) {
                tickView.visibility = View.VISIBLE
                videoTickView.visibility = View.VISIBLE
                imageTickView.visibility = View.VISIBLE
                if (isPending && !isCanceled) {
                    tickView.setImageResource(R.drawable.ic_schedule_clock)
                    videoTickView.setImageResource(R.drawable.ic_schedule_clock)
                    imageTickView.setImageResource(R.drawable.ic_schedule_clock)
                } else if (isPending && isCanceled) {
                    tickView.visibility = View.GONE
                    videoTickView.visibility = View.GONE
                    imageTickView.visibility = View.GONE
                } else when {
                    m.readAt != null -> {
                        tickView.setImageResource(R.drawable.message_read_status_read)
                        videoTickView.setImageResource(R.drawable.message_read_status_read)
                        imageTickView.setImageResource(R.drawable.message_read_status_read)
                    }
                    m.deliveredAt != null -> {
                        tickView.setImageResource(R.drawable.message_read_status_delivered)
                        videoTickView.setImageResource(R.drawable.message_read_status_delivered)
                        imageTickView.setImageResource(R.drawable.message_read_status_delivered)
                    }
                    else -> {
                        tickView.setImageResource(R.drawable.message_read_status_sent)
                        videoTickView.setImageResource(R.drawable.message_read_status_sent)
                        imageTickView.setImageResource(R.drawable.message_read_status_sent)
                    }
                }
            } else {
                tickView.visibility = View.GONE
                videoTickView.visibility = View.GONE
                imageTickView.visibility = View.GONE
            }

            when (m.type) {
                QuestionMessage.TYPE_TEXT -> {
                    messageText.visibility = View.VISIBLE
                    messageText.text = m.textContent
                    audioRow.visibility = View.GONE
                    videoRow.visibility = View.GONE
                    imageContainer.visibility = View.GONE
                    captionText.visibility = View.GONE
                    videoMetaContainer.visibility = View.GONE
                    imageMetaContainer.visibility = View.GONE
                    timeView.visibility = View.VISIBLE
                    // tickView visibility handled above
                }
                QuestionMessage.TYPE_AUDIO -> {
                    messageText.visibility = View.GONE
                    audioRow.visibility = View.VISIBLE
                    videoRow.visibility = View.GONE
                    imageContainer.visibility = View.GONE
                    captionText.visibility = View.GONE
                    videoMetaContainer.visibility = View.GONE
                    imageMetaContainer.visibility = View.GONE
                    timeView.visibility = View.VISIBLE
                    audioRow.setOnClickListener {
                        m.mediaUrl?.let { url -> onPlayAudio(url) }
                    }
                    audioRow.setOnLongClickListener { onMessageLongClick(m); true }
                }
                QuestionMessage.TYPE_VIDEO -> {
                    messageText.visibility = View.GONE
                    audioRow.visibility = View.GONE
                    videoRow.visibility = View.VISIBLE
                    imageContainer.visibility = View.GONE
                    videoMetaContainer.visibility = View.VISIBLE
                    imageMetaContainer.visibility = View.GONE
                    timeView.visibility = View.GONE
                    tickView.visibility = View.GONE
                    videoCornerActionContainer.visibility = View.GONE
                    videoCornerProgress.visibility = View.GONE
                    val isRevealed = m.id in revealedMessageIds || isDownloaded
                    val isCanceledUpload = isFromMe && m.isPending() && m.id in canceledUploadIds
                    // Gönderen için: pending olduğu sürece (iptal edilmedikçe) upload devam ediyor kabul et
                    val isPendingUpload = isFromMe && m.isPending() && !isCanceledUpload
                    val showBlurOverlay =
                        (!isFromMe && !isRevealed && !isDownloaded) || isPendingUpload || isCanceledUpload || isDownloading
                    videoBlurOverlay.visibility = if (showBlurOverlay) View.VISIBLE else View.GONE
                    videoSizeText.text = formatFileSize(m.mediaSizeBytes)

                    // Gösterilecek kapak görselini seç (önce local dosya, sonra thumbnailUrl/mediaUrl).
                    val localMeta = GlobalValues.uploadMetaByClientId[m.id]
                    val localFilePath = localMeta?.filePath
                    val remoteThumbUrl = downloadedLocalPath ?: (m.thumbnailUrl ?: m.mediaUrl)

                    // Önce önceki bind'dan kalan click listener'ları temizle
                    videoRow.setOnClickListener(null)
                    videoCornerActionContainer.setOnClickListener(null)

                    if (showBlurOverlay) {
                        videoCenterActionContainer.visibility = View.GONE
                        videoCornerActionContainer.visibility = View.GONE
                        videoCornerActionText.visibility = View.GONE
                        videoCornerProgress.visibility = View.GONE

                        // Blur varken de kapak göster:
                        when {
                            // Önce bizim gönderdiğimiz ve henüz Firestore'a gitmemiş local video dosyası
                            !localFilePath.isNullOrBlank() -> {
                                Glide.with(itemView.context)
                                    .load(java.io.File(localFilePath))
                                    .centerCrop()
                                    .into(videoThumb)
                            }
                            // Karşıdan gelen, henüz reveal edilmemiş video
                            !isFromMe && !isRevealed && !remoteThumbUrl.isNullOrBlank() -> {
                                Glide.with(itemView.context)
                                    .load(remoteThumbUrl)
                                    .centerCrop()
                                    .into(videoThumb)
                            }
                            else -> {
                                videoThumb.setImageDrawable(null)
                            }
                        }

                        if (!isFromMe && isDownloading) {
                            // Karşı taraf indiriyor: sol altta X, etrafında progress bar
                            videoCornerActionContainer.visibility = View.VISIBLE
                            videoCornerActionIcon.setImageResource(R.drawable.ic_close_black)
                            videoCornerProgress.visibility = View.VISIBLE
                            videoCornerProgress.isIndeterminate = false
                            videoCornerProgress.max = 100
                            videoCornerProgress.progress = currentDownloadProgress
                            videoBlurOverlay.setOnClickListener { onCancelDownload(m) }
                            videoCornerActionContainer.setOnClickListener { onCancelDownload(m) }
                        } else if (!isFromMe && !isRevealed) {
                            // Karşıdan gelen medya: sadece ortadaki download ikonu, köşe aksiyon yok
                            videoCenterActionContainer.visibility = View.VISIBLE
                            videoDownloadIcon.setImageResource(R.drawable.ic_download)
                            videoBlurOverlay.setOnClickListener { onStartDownload(m) }
                        } else if (isPendingUpload) {
                            // Gönderen için: yükleme devam ediyorsa X ikonu + etrafında progress bar
                            videoCornerActionContainer.visibility = View.VISIBLE
                            videoCornerActionIcon.setImageResource(R.drawable.ic_close_black)
                            videoCornerProgress.visibility = View.VISIBLE
                            videoCornerProgress.isIndeterminate = false
                            videoCornerProgress.max = 100
                            videoCornerProgress.progress = currentUploadProgress
                            videoBlurOverlay.setOnClickListener { onCancelUpload(m) }
                            videoCornerActionContainer.setOnClickListener { onCancelUpload(m) }
                        } else if (isCanceledUpload) {
                            // Gönderen için: iptal edilmiş gönderi, export ikonu, tıklayınca tekrar gönder
                            videoCornerActionContainer.visibility = View.VISIBLE
                            videoCornerActionIcon.setImageResource(R.drawable.export_ic)
                            videoCornerActionText.text = itemView.context.getString(R.string.chat_retry_upload)
                            videoCornerActionText.visibility = View.VISIBLE
                            videoBlurOverlay.setOnClickListener { onRetryUpload(m) }
                            videoCornerActionContainer.setOnClickListener { onRetryUpload(m) }
                        }
                        videoBlurOverlay.setOnLongClickListener { onMessageLongClick(m); true }
                    } else {
                        val thumbSource = when {
                            !localFilePath.isNullOrBlank() -> localFilePath
                            else -> remoteThumbUrl
                        }
                        if (!thumbSource.isNullOrBlank()) {
                            if (!localFilePath.isNullOrBlank() && thumbSource == localFilePath) {
                                Glide.with(itemView.context)
                                    .load(java.io.File(thumbSource))
                                    .centerCrop()
                                    .transition(DrawableTransitionOptions.withCrossFade(150))
                                    .into(videoThumb)
                            } else {
                                Glide.with(itemView.context)
                                    .load(thumbSource)
                                    .centerCrop()
                                    .transition(DrawableTransitionOptions.withCrossFade(150))
                                    .placeholder(android.R.drawable.ic_media_play)
                                    .error(android.R.drawable.ic_menu_gallery)
                                    .into(videoThumb)
                            }
                        } else {
                            videoThumb.setImageDrawable(null)
                        }
                        videoRow.setOnClickListener {
                            val playSource = downloadedLocalPath ?: m.mediaUrl
                            playSource?.let { urlOrPath ->
                                val finalUrl = if (downloadedLocalPath != null) {
                                    android.net.Uri.fromFile(java.io.File(urlOrPath)).toString()
                                } else {
                                    urlOrPath
                                }
                                onPlayVideo(finalUrl)
                            }
                        }
                        videoBlurOverlay.setOnClickListener(null)
                        videoBlurOverlay.setOnLongClickListener(null)
                        videoCornerActionContainer.setOnClickListener(null)
                        // Blur kalktığında X / export alanını da tamamen gizle
                        videoCornerActionContainer.visibility = View.GONE
                        videoCornerProgress.visibility = View.GONE
                    }
                    videoRow.setOnLongClickListener { onMessageLongClick(m); true }
                    val hasCaption = !m.textContent.isNullOrEmpty()
                    captionText.visibility = if (hasCaption) View.VISIBLE else View.GONE
                    captionText.text = m.textContent
                }
                QuestionMessage.TYPE_IMAGE -> {
                    messageText.visibility = View.GONE
                    audioRow.visibility = View.GONE
                    videoRow.visibility = View.GONE
                    imageContainer.visibility = View.VISIBLE
                    videoMetaContainer.visibility = View.GONE
                    imageMetaContainer.visibility = View.VISIBLE
                    timeView.visibility = View.GONE
                    tickView.visibility = View.GONE
                    imageCornerActionContainer.visibility = View.GONE
                    imageCornerProgress.visibility = View.GONE
                    val isRevealed = m.id in revealedMessageIds || isDownloaded
                    val isCanceledUpload = isFromMe && m.isPending() && m.id in canceledUploadIds
                    val isPendingUpload = isFromMe && m.isPending() && !isCanceledUpload
                    val showBlurOverlay =
                        (!isFromMe && !isRevealed && !isDownloaded) || isPendingUpload || isCanceledUpload || isDownloading
                    imageBlurOverlay.visibility = if (showBlurOverlay) View.VISIBLE else View.GONE
                    imageSizeText.text = formatFileSize(m.mediaSizeBytes)

                    val localMeta = GlobalValues.uploadMetaByClientId[m.id]
                    val localImagePath = localMeta?.filePath
                    // Eski tıklama davranışlarını temizle
                    imageThumb.setOnClickListener(null)
                    imageCornerActionContainer.setOnClickListener(null)

                    if (showBlurOverlay) {
                        imageCenterActionContainer.visibility = View.GONE
                        imageCornerActionContainer.visibility = View.GONE
                        imageCornerActionText.visibility = View.GONE
                        imageCornerProgress.visibility = View.GONE

                        // Blur varken kapak göster:
                        when {
                            // Bizim gönderdiğimiz, henüz upload aşamasındaki local resim
                            !localImagePath.isNullOrBlank() -> {
                                Glide.with(itemView.context)
                                    .load(java.io.File(localImagePath))
                                    .centerCrop()
                                    .into(imageThumb)
                            }
                            // Karşıdan gelen ve henüz reveal edilmemiş resim
                            !isFromMe && !isRevealed && !isDownloaded && !m.mediaUrl.isNullOrBlank() -> {
                                Glide.with(itemView.context)
                                    .load(m.mediaUrl)
                                    .centerCrop()
                                    .into(imageThumb)
                            }
                            else -> {
                                imageThumb.setImageDrawable(null)
                            }
                        }
                        when {
                            !isFromMe && isDownloading -> {
                                imageCornerActionContainer.visibility = View.VISIBLE
                                imageCornerActionIcon.setImageResource(R.drawable.ic_close_black)
                                imageCornerProgress.visibility = View.VISIBLE
                                imageCornerProgress.isIndeterminate = false
                                imageCornerProgress.max = 100
                                imageCornerProgress.progress = currentDownloadProgress
                                imageBlurOverlay.setOnClickListener { onCancelDownload(m) }
                                imageCornerActionContainer.setOnClickListener { onCancelDownload(m) }
                            }
                            !isFromMe && !isRevealed && !isDownloaded -> {
                                // Karşıdan gelen medya: sadece ortadaki download ikonu
                                imageCenterActionContainer.visibility = View.VISIBLE
                                imageDownloadIcon.setImageResource(R.drawable.ic_download)
                                imageBlurOverlay.setOnClickListener { onStartDownload(m) }
                            }
                            isPendingUpload -> {
                                imageCornerActionContainer.visibility = View.VISIBLE
                                imageCornerActionIcon.setImageResource(R.drawable.ic_close_black)
                                imageCornerProgress.visibility = View.VISIBLE
                                imageCornerProgress.isIndeterminate = false
                                imageCornerProgress.max = 100
                                imageCornerProgress.progress = currentUploadProgress
                                imageBlurOverlay.setOnClickListener { onCancelUpload(m) }
                                imageCornerActionContainer.setOnClickListener { onCancelUpload(m) }
                            }
                            isCanceledUpload -> {
                                imageCornerActionContainer.visibility = View.VISIBLE
                                imageCornerActionIcon.setImageResource(R.drawable.export_ic)
                                imageCornerActionText.text = itemView.context.getString(R.string.chat_retry_upload)
                                imageCornerActionText.visibility = View.VISIBLE
                                imageBlurOverlay.setOnClickListener { onRetryUpload(m) }
                                imageCornerActionContainer.setOnClickListener { onRetryUpload(m) }
                            }
                            else -> {
                                imageCenterActionContainer.visibility = View.VISIBLE
                                imageDownloadIcon.setImageResource(R.drawable.ic_download)
                                imageBlurOverlay.setOnClickListener { onStartDownload(m) }
                            }
                        }
                        imageBlurOverlay.setOnLongClickListener { onMessageLongClick(m); true }
                        imageThumb.setOnClickListener(null)
                        imageThumb.setOnLongClickListener(null)
                    } else {
                        val source = localImagePath ?: downloadedLocalPath ?: m.mediaUrl
                        source?.let { urlOrPath ->
                            if (localImagePath != null && urlOrPath == localImagePath) {
                                Glide.with(itemView.context)
                                    .load(java.io.File(urlOrPath))
                                    .centerCrop()
                                    .transition(DrawableTransitionOptions.withCrossFade(150))
                                    .into(imageThumb)
                            } else {
                                Glide.with(itemView.context)
                                    .load(urlOrPath)
                                    .centerCrop()
                                    .transition(DrawableTransitionOptions.withCrossFade(150))
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_gallery)
                                    .into(imageThumb)
                            }
                            val clickSource = downloadedLocalPath ?: m.mediaUrl
                            clickSource?.let { urlOrPath ->
                                val finalUrl = if (downloadedLocalPath != null) {
                                    android.net.Uri.fromFile(java.io.File(urlOrPath)).toString()
                                } else {
                                    urlOrPath
                                }
                                imageThumb.setOnClickListener { onImageClick(finalUrl) }
                            } ?: run {
                                imageThumb.setOnClickListener(null)
                            }
                        } ?: run {
                            imageThumb.setImageDrawable(null)
                            imageThumb.setOnClickListener(null)
                        }
                        imageThumb.setOnLongClickListener { onMessageLongClick(m); true }
                        imageBlurOverlay.setOnClickListener(null)
                        imageBlurOverlay.setOnLongClickListener(null)
                        imageCornerActionContainer.visibility = View.GONE
                        imageCornerProgress.visibility = View.GONE
                    }
                    val hasCaption = !m.textContent.isNullOrEmpty()
                    captionText.visibility = if (hasCaption) View.VISIBLE else View.GONE
                    captionText.text = m.textContent
                }
                else -> {
                    messageText.visibility = View.VISIBLE
                    messageText.text = m.textContent ?: "-"
                    audioRow.visibility = View.GONE
                    videoRow.visibility = View.GONE
                    imageContainer.visibility = View.GONE
                    captionText.visibility = View.GONE
                    videoMetaContainer.visibility = View.GONE
                    imageMetaContainer.visibility = View.GONE
                    timeView.visibility = View.VISIBLE
                }
            }
        }
    }
}


