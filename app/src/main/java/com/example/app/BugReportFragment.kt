package com.example.app

import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.app.databinding.FragmentBugReportBinding
import com.example.app.databinding.ItemBugReportMineBinding
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hata bildirme ekranı (Görevler → "Hata Avcısı" kartı).
 *
 * Kullanıcı galeriden ekran görüntüsünü seçer (Photo Picker — depolama izni istemez),
 * isterse hatanın yerini üzerine çizerek işaretler ve ne olduğunu yazar. Altta önceki
 * bildirimleri ve durumları görünür; onaylananın ödülü buradan da toplanabilir.
 * Akışın sunucu tarafı için bkz. [BugReports] ve functions/bugReports.js.
 */
class BugReportFragment : Fragment() {

    private var _binding: FragmentBugReportBinding? = null
    private val binding get() = _binding!!

    /** Küçültülmüş ve önbelleğe yazılmış görsel; çizim ekranı aynı dosyanın üzerine yazar. */
    private var imageFile: File? = null
    private var mineListener: ListenerRegistration? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePicked(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBugReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { close() }
        binding.btnClose.setOnClickListener { close() }

        BugReportRewardViews.bind(binding.rewardsRow)

        binding.pickImageButton.setOnClickListener { launchPicker() }
        binding.changeImageButton.setOnClickListener { launchPicker() }
        binding.imagePreview.setOnClickListener { openAnnotate() }
        binding.annotateButton.setOnClickListener { openAnnotate() }
        childFragmentManager.setFragmentResultListener(
            ImageAnnotateDialogFragment.RESULT_KEY,
            viewLifecycleOwner,
        ) { _, _ -> showPreview() }

        binding.descriptionInput.filters = arrayOf(InputFilter.LengthFilter(BugReports.TEXT_MAX))
        binding.sendButton.setOnClickListener { requireOnlineAndLoggedInOrLogin { send() } }

        mineListener = BugReports.listenMine { reports -> renderMine(reports) }
    }

    private fun launchPicker() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun onImagePicked(uri: Uri) {
        val appContext = requireContext().applicationContext
        setBlocking(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) { BugReports.prepareImage(appContext, uri) }
            if (_binding == null) return@launch
            setBlocking(false)
            if (file == null) {
                toast(R.string.bug_report_image_failed)
                return@launch
            }
            imageFile = file
            showPreview()
        }
    }

    private fun showPreview() {
        val file = imageFile ?: return
        val b = _binding ?: return
        b.pickImageButton.visibility = View.GONE
        b.imagePreview.visibility = View.VISIBLE
        b.imageActions.visibility = View.VISIBLE
        // Çizimden sonra dosya aynı adla değişiyor; önbellek eski hâli göstermesin.
        Glide.with(this)
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .signature(ObjectKey(file.lastModified()))
            .into(b.imagePreview)
    }

    private fun openAnnotate() {
        val file = imageFile ?: return
        ImageAnnotateDialogFragment.newInstance(file.absolutePath)
            .show(childFragmentManager, "BugReportAnnotate")
    }

    private fun send() {
        val file = imageFile
        if (file == null || !file.exists()) {
            toast(R.string.bug_report_need_image)
            return
        }
        val text = binding.descriptionInput.text.toString().trim()
        if (text.length < BugReports.TEXT_MIN) {
            toast(R.string.bug_report_need_text)
            return
        }
        hideKeyboard()
        setBlocking(true)
        BugReports.submit(requireContext().applicationContext, file, text) { success, message ->
            if (_binding == null) return@submit
            setBlocking(false)
            if (!success) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                return@submit
            }
            toast(R.string.bug_report_sent)
            resetForm()
        }
    }

    private fun resetForm() {
        imageFile = null
        binding.descriptionInput.setText("")
        binding.imagePreview.setImageDrawable(null)
        binding.imagePreview.visibility = View.GONE
        binding.imageActions.visibility = View.GONE
        binding.pickImageButton.visibility = View.VISIBLE
    }

    private fun renderMine(reports: List<BugReports.Report>) {
        val b = _binding ?: return
        val container = b.myReportsContainer
        container.removeAllViews()
        b.myReportsTitle.visibility = if (reports.isEmpty()) View.GONE else View.VISIBLE
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        val ctx = requireContext()

        for (report in reports) {
            val row = ItemBugReportMineBinding.inflate(layoutInflater, container, false)
            row.mineDate.text = if (report.createdAtMs > 0) dateFormat.format(Date(report.createdAtMs)) else ""
            row.mineText.text = report.text

            val (statusRes, colorRes) = when {
                report.isPending -> R.string.bug_report_status_pending to R.color.lesson_header_yellow
                report.isClaimable -> R.string.bug_report_status_approved to R.color.lesson_header_green
                report.isApproved -> R.string.bug_report_status_claimed to R.color.lesson_header_green
                else -> R.string.bug_report_status_rejected to R.color.lesson_header_red
            }
            row.mineStatus.setText(statusRes)
            row.mineStatus.setTextColor(ContextCompat.getColor(ctx, colorRes))

            val detail = when {
                report.isApproved -> report.rewardText
                report.isRejected -> report.rejectReason?.let { appString(R.string.bug_report_reject_reason_line, it.label) }
                else -> null
            }
            val note = report.reviewNote.takeIf { it.isNotBlank() && !report.isPending }
            val detailText = listOfNotNull(detail, note).joinToString("\n")
            row.mineDetail.text = detailText
            row.mineDetail.visibility = if (detailText.isBlank()) View.GONE else View.VISIBLE

            row.mineClaimButton.visibility = if (report.isClaimable) View.VISIBLE else View.GONE
            row.mineClaimButton.setOnClickListener { claim(report.id) }
            container.addView(row.root)
        }
    }

    private fun claim(reportId: String) {
        setBlocking(true)
        BugReports.claimReward(reportId) { _, message ->
            if (_binding == null) return@claimReward
            setBlocking(false)
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun setBlocking(blocking: Boolean) {
        _binding?.blockingOverlay?.visibility = if (blocking) View.VISIBLE else View.GONE
    }

    private fun toast(res: Int) {
        Toast.makeText(requireContext(), res, Toast.LENGTH_LONG).show()
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(InputMethodManager::class.java) ?: return
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun close() {
        hideKeyboard()
        val main = activity as? MainActivity
        if (main != null) {
            main.finishTasksOverlayAnimated("BugReportFragment.close")
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mineListener?.remove()
        mineListener = null
        _binding = null
    }
}
