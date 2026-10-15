package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.app.databinding.DialogBugReportReviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin: tek bir hata bildirimini inceleme ve karar verme.
 *
 * Ekran görüntüsü tam çözünürlükte, iki parmakla yakınlaştırılabilir ([ZoomImageView]).
 * Onay iki kademeli ([BugReports.Tier]); büyük ödül gerçek parayla satılan kredi içerdiği
 * için onaydan önce bir kez daha soruluyor. Ret bir gerekçe seçtiriyor — kullanıcı
 * bildirimlerinde bu gerekçeyi kendi dilinde görüyor.
 */
class BugReportReviewDialogFragment : DialogFragment() {

    private var _binding: DialogBugReportReviewBinding? = null
    private val binding get() = _binding!!

    private val reportId: String get() = requireArguments().getString(ARG_ID).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_App_BugReportReview)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogBugReportReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        binding.reviewClose.setOnClickListener { dismiss() }
        binding.reviewText.text = args.getString(ARG_TEXT)

        val createdAt = args.getLong(ARG_CREATED_AT)
        val date = if (createdAt > 0) {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(createdAt))
        } else {
            ""
        }
        binding.reviewMeta.text = listOf(
            date,
            args.getString(ARG_DEVICE).orEmpty(),
            appString(R.string.bug_report_admin_user_line, args.getString(ARG_UID).orEmpty()),
        ).filter { it.isNotBlank() }.joinToString("\n")

        loadImage(args.getString(ARG_IMAGE_PATH).orEmpty())

        binding.reviewApproveBig.setOnClickListener { confirmApprove(BugReports.Tier.BIG) }
        binding.reviewApproveSmall.setOnClickListener { confirmApprove(BugReports.Tier.SMALL) }
        binding.reviewReject.setOnClickListener { pickRejectReason() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    private fun loadImage(imagePath: String) {
        BugReports.imageUrl(imagePath) { uri ->
            val b = _binding ?: return@imageUrl
            if (uri == null) {
                b.reviewImageProgress.visibility = View.GONE
                return@imageUrl
            }
            Glide.with(this)
                .load(uri)
                // Yakınlaştırınca ayrıntı kaybolmasın: görünüme göre küçültme.
                .override(Target.SIZE_ORIGINAL)
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?,
                        target: Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean,
                    ): Boolean {
                        _binding?.reviewImageProgress?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable, model: Any,
                        target: Target<android.graphics.drawable.Drawable>?, dataSource: DataSource, isFirstResource: Boolean,
                    ): Boolean {
                        _binding?.reviewImageProgress?.visibility = View.GONE
                        return false
                    }
                })
                .into(b.reviewImage)
        }
    }

    private fun confirmApprove(tier: BugReports.Tier) {
        AlertDialog.Builder(requireContext())
            .setMessage(
                appString(
                    R.string.bug_report_admin_confirm_approve,
                    BugReports.rewardText(tier.credits, tier.gold, tier.keys),
                )
            )
            .setPositiveButton(R.string.bug_report_admin_confirm) { _, _ -> submit(tier, null) }
            .setNegativeButton(R.string.bug_report_admin_cancel, null)
            .show()
    }

    private fun pickRejectReason() {
        val reasons = BugReports.RejectReason.entries
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.bug_report_admin_reject_title)
            .setItems(reasons.map { it.label }.toTypedArray()) { _, which -> submit(null, reasons[which]) }
            .setNegativeButton(R.string.bug_report_admin_cancel, null)
            .show()
    }

    private fun submit(tier: BugReports.Tier?, reason: BugReports.RejectReason?) {
        setButtonsEnabled(false)
        val note = binding.reviewNote.text.toString().trim()
        BugReports.review(reportId, tier, reason, note) { success, message ->
            val ctx = context ?: return@review
            if (success) {
                Toast.makeText(ctx, R.string.bug_report_admin_done, Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            } else {
                setButtonsEnabled(true)
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val b = _binding ?: return
        b.reviewApproveBig.isEnabled = enabled
        b.reviewApproveSmall.isEnabled = enabled
        b.reviewReject.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_UID = "uid"
        private const val ARG_TEXT = "text"
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_DEVICE = "device"
        private const val ARG_CREATED_AT = "created_at"

        fun newInstance(report: BugReports.Report) = BugReportReviewDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, report.id)
                putString(ARG_UID, report.uid)
                putString(ARG_TEXT, report.text)
                putString(ARG_IMAGE_PATH, report.imagePath)
                putString(ARG_DEVICE, report.deviceLine)
                putLong(ARG_CREATED_AT, report.createdAtMs)
            }
        }
    }
}
