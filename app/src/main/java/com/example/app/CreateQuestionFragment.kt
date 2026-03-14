package com.example.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.app.databinding.FragmentCreateQuestionBinding
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
            binding.videoFullscreenButton.setOnClickListener {
                VideoFullscreenDialogFragment.newInstance(Uri.fromFile(file).toString())
                    .show(parentFragmentManager, "VideoFullscreen")
            }
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
        Glide.with(this)
            .load(file)
            .apply(
                RequestOptions()
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                    .dontTransform()
            )
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.screenshotImage)

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.sendButton.setOnClickListener {
            requireOnlineAndLoggedInOrLogin { sendQuestion(screenshotPath) }
        }
    }

    private fun sendQuestion(screenshotPath: String) {
        val message = binding.descriptionInput.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen açıklama yazın.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Oturum açık değil.", Toast.LENGTH_SHORT).show()
            return
        }
        setSendingUi(true)

        val fileName = "question_screenshots/${uid}_${System.currentTimeMillis()}.jpg"
        val ref = storage.child(fileName)
        val file = File(screenshotPath)
        ref.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val screenshotUrl = uri.toString()
                    val previewText = message.lines().take(2).joinToString(" ").take(80)
                    val now = com.google.firebase.Timestamp.now()
                    val data = hashMapOf(
                        "studentUid" to uid,
                        "studentEmail" to auth.currentUser?.email,
                        "screenshotStoragePath" to fileName,
                        "screenshotUrl" to screenshotUrl,
                        "message" to message,
                        "previewText" to previewText,
                        "status" to StudentQuestion.STATUS_PENDING,
                        "createdAt" to now,
                        "lastMessageAt" to now
                    )
                    firestore.collection("questions")
                        .add(data)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Soru gönderildi.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            setSendingUi(false)
                            Toast.makeText(requireContext(), "Gönderilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                setSendingUi(false)
                Toast.makeText(requireContext(), "Yükleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendVideoQuestion(videoPath: String) {
        val message = binding.descriptionInput.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen açıklama yazın.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Oturum açık değil.", Toast.LENGTH_SHORT).show()
            return
        }
        setSendingUi(true)
        val durationSec = exoPlayer?.duration?.let { if (it > 0) (it / 1000).toInt() else null } ?: 0
        val fileName = "question_videos/${uid}_${System.currentTimeMillis()}.mp4"
        val ref = storage.child(fileName)
        val file = File(videoPath)
        ref.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val videoUrl = uri.toString()
                    val previewText = message.lines().take(2).joinToString(" ").take(80)
                    val now = com.google.firebase.Timestamp.now()
                    val data = hashMapOf(
                        "studentUid" to uid,
                        "studentEmail" to auth.currentUser?.email,
                        "mediaType" to StudentQuestion.MEDIA_TYPE_VIDEO,
                        "videoStoragePath" to fileName,
                        "videoUrl" to videoUrl,
                        "videoDurationSec" to durationSec,
                        "message" to message,
                        "previewText" to previewText,
                        "status" to StudentQuestion.STATUS_PENDING,
                        "createdAt" to now,
                        "lastMessageAt" to now
                    )
                    firestore.collection("questions")
                        .add(data)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Soru gönderildi.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            setSendingUi(false)
                            Toast.makeText(requireContext(), "Gönderilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                setSendingUi(false)
                Toast.makeText(requireContext(), "Video yükleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setSendingUi(sending: Boolean) {
        binding.sendButton.isEnabled = !sending
        binding.sendBlockingOverlay.visibility = if (sending) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        exoPlayer?.release()
        exoPlayer = null
        binding.videoPreview.player = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SCREENSHOT_PATH = "screenshot_path"
        private const val ARG_VIDEO_PATH = "video_path"
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


