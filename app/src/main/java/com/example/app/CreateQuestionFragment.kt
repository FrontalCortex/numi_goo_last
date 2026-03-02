package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.app.databinding.FragmentCreateQuestionBinding
import com.example.app.model.StudentQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import android.net.Uri

class CreateQuestionFragment : Fragment() {

    private var _binding: FragmentCreateQuestionBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

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
        val screenshotPath = arguments?.getString(ARG_SCREENSHOT_PATH) ?: run {
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
        binding.sendButton.setOnClickListener { sendQuestion(screenshotPath) }
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
        binding.sendButton.isEnabled = false

        val fileName = "question_screenshots/${uid}_${System.currentTimeMillis()}.jpg"
        val ref = storage.child(fileName)
        val file = File(screenshotPath)
        ref.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val screenshotUrl = uri.toString()
                    val previewText = message.lines().take(2).joinToString(" ").take(80)
                    val data = hashMapOf(
                        "studentUid" to uid,
                        "studentEmail" to auth.currentUser?.email,
                        "screenshotStoragePath" to fileName,
                        "screenshotUrl" to screenshotUrl,
                        "message" to message,
                        "previewText" to previewText,
                        "status" to StudentQuestion.STATUS_PENDING,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    firestore.collection("questions")
                        .add(data)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Soru gönderildi.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            binding.sendButton.isEnabled = true
                            Toast.makeText(requireContext(), "Gönderilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.sendButton.isEnabled = true
                Toast.makeText(requireContext(), "Yükleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SCREENSHOT_PATH = "screenshot_path"
        fun newInstance(screenshotPath: String): CreateQuestionFragment {
            return CreateQuestionFragment().apply {
                arguments = Bundle().apply { putString(ARG_SCREENSHOT_PATH, screenshotPath) }
            }
        }
    }
}


