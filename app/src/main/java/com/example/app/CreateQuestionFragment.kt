package com.example.app

import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentCreateQuestionBinding
import com.example.app.model.QuestionMessage
import com.example.app.model.StudentQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class CreateQuestionFragment : Fragment() {

    private var _binding: FragmentCreateQuestionBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    private var exoPlayer: ExoPlayer? = null
    /** Gönder'e basıldıysa (veya gönderim başlatıldıysa) true; çıkışta medya dosyasını silmeyiz. */
    private var didSendOrIsSending = false

    /** Öğrenci: yükleme overlay'i açıkken (Gönder sonrası beklerken) true — sistem geri tuşu bloklanır. */
    private var studentSendInProgress = false

    fun isStudentSendingInProgress(): Boolean = studentSendInProgress

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isTeacher = arguments?.getBoolean(ARG_IS_TEACHER, false) == true

        // Öğretmen CreateQuestion akışında başlık alanını gizle (hem video hem screenshot için)
        if (isTeacher) {
            binding.headerLabel.visibility = View.GONE
            binding.headerInput.visibility = View.GONE
        } else {
            binding.headerLabel.visibility = View.VISIBLE
            binding.headerInput.visibility = View.VISIBLE
        }

        val videoPath = arguments?.getString(ARG_VIDEO_PATH)
        val screenshotPath = arguments?.getString(ARG_SCREENSHOT_PATH)

        if (!videoPath.isNullOrEmpty()) {
            val file = File(videoPath)
            if (!file.exists()) {
                Toast.makeText(requireContext(), "Video bulunamadı.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return
            }
            binding.screenshotImage.visibility = View.GONE
            binding.videoPreview.visibility = View.VISIBLE
            binding.videoFullscreenButton.visibility = View.VISIBLE
            exoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
                binding.videoPreview.player = player
                player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                player.prepare()
                player.playWhenReady = false
            }
            parentFragmentManager.setFragmentResultListener(
                REQUEST_VIDEO_FULLSCREEN_DISMISSED,
                viewLifecycleOwner
            ) { _, _ ->
                exoPlayer?.playWhenReady = true
            }
            binding.videoFullscreenButton.setOnClickListener {
                exoPlayer?.playWhenReady = false
                VideoFullscreenDialogFragment.newInstance(
                    Uri.fromFile(file).toString(),
                    REQUEST_VIDEO_FULLSCREEN_DISMISSED
                ).show(parentFragmentManager, "VideoFullscreen")
            }
            binding.descriptionInput.filters = arrayOf(InputFilter.LengthFilter(700))
            arguments?.getString(ARG_DESCRIPTION)?.let { binding.descriptionInput.setText(it) }
            binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
            binding.sendButton.setOnClickListener {
                requireOnlineAndLoggedInOrLogin { sendVideoQuestion(videoPath) }
            }
            return
        }

        if (screenshotPath.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Ekran görüntüsü alınamadı.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }
        val file = File(screenshotPath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "Ekran görüntüsü bulunamadı.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }
        parentFragmentManager.setFragmentResultListener(
            ImageAnnotateDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            // Aynı dosya üzerine yazıyoruz; yeniden yükle.
            Glide.with(this)
                .load(file)
                .apply(
                    RequestOptions()
                        .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                        .dontTransform()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .signature(ObjectKey(file.lastModified()))
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.screenshotImage)
        }
        Glide.with(this)
            .load(file)
            .apply(
                RequestOptions()
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                    .dontTransform()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .signature(ObjectKey(file.lastModified()))
            )
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.screenshotImage)

        // Screenshot ise sağ üstte çizim butonu göster
        binding.screenshotAnnotateButton.visibility = View.VISIBLE
        binding.screenshotAnnotateButton.setOnClickListener {
            ImageAnnotateDialogFragment.newInstance(file.absolutePath)
                .show(parentFragmentManager, "ImageAnnotate")
        }

        binding.descriptionInput.filters = arrayOf(InputFilter.LengthFilter(700))
        arguments?.getString(ARG_DESCRIPTION)?.let { binding.descriptionInput.setText(it) }
        binding.backButton.setOnClickListener {
            if (arguments?.getBoolean(ARG_FROM_TEACHER_SELECTION_BACK, false) == true) {
                (activity as? MainActivity)?.onTeacherCreateQuestionDismissedByBack()
            }
            parentFragmentManager.popBackStack()
        }
        binding.sendButton.setOnClickListener {
            requireOnlineAndLoggedInOrLogin { sendQuestion(screenshotPath) }
        }
    }

    private fun sendQuestion(screenshotPath: String) {
        val header = binding.headerInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val isTeacher = arguments?.getBoolean(ARG_IS_TEACHER, false) == true
        if (!isTeacher) {
            if (header.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen başlık yazın.", Toast.LENGTH_SHORT).show()
                return
            }
            if (description.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen açıklama yazın.", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Oturum açık değil.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isTeacher) {
            // Öğretmen: yeni soru oluşturmak yerine mevcut sahiplenilmiş soruya gönder akışını başlat.
            // Pop işlemini MainActivity yapar; burada popBackStack çağırma (TeacherSelect ekranını geri alır).
            didSendOrIsSending = true
            (activity as? MainActivity)?.onTeacherSubmitQuestionMedia(
                mediaType = StudentQuestion.MEDIA_TYPE_IMAGE,
                mediaPath = screenshotPath,
                description = description.ifEmpty { null }
            )
            return
        }
        didSendOrIsSending = true
        setSendingUi(true)

        val fileName = "question_screenshots/${uid}_${System.currentTimeMillis()}.jpg"
        val ref = storage.child(fileName)
        val file = File(screenshotPath)
        ref.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val screenshotUrl = uri.toString()
                    val previewText = header.take(80)
                    // Soru ve ilk mesaj sunucuda, kredi düşümüyle aynı transaction'da oluşur.
                    ServerQuestions.ask(
                        mediaType = ServerQuestions.MEDIA_IMAGE,
                        storagePath = fileName,
                        mediaUrl = screenshotUrl,
                        message = header,
                        previewText = previewText,
                        description = description,
                        onResult = { _, _ ->
                            runStudentGolfBadgeThenClose("Soru gönderildi.")
                        },
                        onFailure = { e ->
                            setSendingUi(false)
                            didSendOrIsSending = false
                            showSendFailure(e)
                        },
                    )
                }
            }
            .addOnFailureListener { e ->
                setSendingUi(false)
                Toast.makeText(requireContext(), "Yükleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendVideoQuestion(videoPath: String) {
        val header = binding.headerInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val isTeacher = arguments?.getBoolean(ARG_IS_TEACHER, false) == true
        if (!isTeacher && header.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen başlık yazın.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Oturum açık değil.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isTeacher) {
            didSendOrIsSending = true
            (activity as? MainActivity)?.onTeacherSubmitQuestionMedia(
                mediaType = StudentQuestion.MEDIA_TYPE_VIDEO,
                mediaPath = videoPath,
                description = description.ifEmpty { null }
            )
            return
        }
        didSendOrIsSending = true
        setSendingUi(true)
        val durationSec = exoPlayer?.duration?.let { if (it > 0) (it / 1000).toInt() else null } ?: 0
        val fileName = "question_videos/${uid}_${System.currentTimeMillis()}.mp4"
        val ref = storage.child(fileName)
        val file = File(videoPath)
        ref.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val videoUrl = uri.toString()
                    val previewText = header.take(80)
                    // Soru ve ilk mesaj sunucuda, kredi düşümüyle aynı transaction'da oluşur.
                    ServerQuestions.ask(
                        mediaType = ServerQuestions.MEDIA_VIDEO,
                        storagePath = fileName,
                        mediaUrl = videoUrl,
                        message = header,
                        previewText = previewText,
                        description = description.ifEmpty { null },
                        videoDurationSec = durationSec,
                        onResult = { _, _ ->
                            // Offline yok: gönderim başarılıysa yerel cache videosunu temizle.
                            runCatching { File(videoPath).delete() }
                            runStudentGolfBadgeThenClose("Soru gönderildi.")
                        },
                        onFailure = { e ->
                            setSendingUi(false)
                            didSendOrIsSending = false
                            // Soru oluşturulamadıysa videoyu cache'te tutmayalım (offline yok).
                            runCatching { File(videoPath).delete() }
                            showSendFailure(e)
                        },
                    )
                }
                .addOnFailureListener {
                    setSendingUi(false)
                    // Download URL alınamadıysa da videoyu temizle (offline yok).
                    runCatching { File(videoPath).delete() }
                    Toast.makeText(requireContext(), "Video yükleme hatası.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                setSendingUi(false)
                // Upload başarısızsa videoyu cache'te tutmayalım (offline yok).
                runCatching { File(videoPath).delete() }
                Toast.makeText(requireContext(), "Video yükleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setSendingUi(sending: Boolean) {
        val isTeacher = arguments?.getBoolean(ARG_IS_TEACHER, false) == true
        if (!isTeacher) studentSendInProgress = sending
        binding.sendButton.isEnabled = !sending
        binding.sendBlockingOverlay.visibility = if (sending) View.VISIBLE else View.GONE
    }

    /**
     * Gönderim hatasını kullanıcıya anlatır.
     *
     * Kredi yetersizliği ayrı ele alınır: bu bir arıza değil, kullanıcının çözebileceği
     * bir durum — genel "gönderilemedi" mesajı burada yanıltıcı olurdu.
     */
    private fun showSendFailure(e: Exception) {
        if (!isAdded) return
        val text = if (ServerQuestions.isInsufficientCredits(e)) {
            "Danışma kredin kalmadı. Mağazadan kredi alabilirsin."
        } else {
            "Gönderilemedi: ${e.message}"
        }
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
    }

    /** Soru kaydı tamam; golf +1 Firestore, gerekirse [BadgeFragment] kutlaması (Chest akışıyla aynı container). */
    private fun runStudentGolfBadgeThenClose(toastText: String) {
        val fm = activity?.supportFragmentManager
        BadgeProgressFirestore.incrementBadgeProgressAndDetectLevelUp(
            incrementDart = false,
            incrementBowlingBy = 0,
            incrementKarate = false,
            incrementRocketDailyLessons = false,
            incrementGolf = true,
        ) { payloads ->
            if (!isAdded) return@incrementBadgeProgressAndDetectLevelUp
            setSendingUi(false)
            Toast.makeText(requireContext(), toastText, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            if (payloads.isNotEmpty() && fm != null) {
                BadgeProgressFirestore.openBadgeCelebration(fm, payloads)
            }
        }
    }

    override fun onDestroyView() {
        if (!didSendOrIsSending) {
            arguments?.getString(ARG_VIDEO_PATH)?.let { path ->
                File(path).takeIf { it.exists() }?.delete()
            }
            arguments?.getString(ARG_SCREENSHOT_PATH)?.let { path ->
                File(path).takeIf { it.exists() }?.delete()
            }
        }
        exoPlayer?.release()
        exoPlayer = null
        binding.videoPreview.player = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SCREENSHOT_PATH = "screenshot_path"
        const val ARG_VIDEO_PATH = "video_path"
        const val ARG_IS_TEACHER = "is_teacher"
        const val ARG_DESCRIPTION = "description"
        const val ARG_FROM_TEACHER_SELECTION_BACK = "from_teacher_selection_back"
        private const val REQUEST_VIDEO_FULLSCREEN_DISMISSED = "CreateQuestionFragment_video_fullscreen_dismissed"
        fun newInstance(screenshotPath: String): CreateQuestionFragment {
            return CreateQuestionFragment().apply {
                arguments = Bundle().apply { putString(ARG_SCREENSHOT_PATH, screenshotPath) }
            }
        }
        fun newInstanceForVideo(videoPath: String): CreateQuestionFragment {
            return CreateQuestionFragment().apply {
                arguments = Bundle().apply { putString(ARG_VIDEO_PATH, videoPath) }
            }
        }
    }
}


