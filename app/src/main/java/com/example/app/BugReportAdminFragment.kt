package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.databinding.FragmentBugReportAdminBinding
import com.example.app.databinding.ItemBugReportAdminBinding
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin paneli: incelenmeyi bekleyen hata bildirimleri, ilk gelen en üstte (aynı hatayı ilk
 * bildiren tam ödülü alsın diye). Bir satıra dokunmak inceleme ekranını açar
 * ([BugReportReviewDialogFragment]); karar verilince bildirim listeden kendiliğinden düşer.
 *
 * Görevler'deki kartı yalnızca admin claim'li hesapta görünüyor; burada ayrıca bir kontrol
 * yok çünkü yetkisiz biri açsa bile kurallar okumayı, `reviewBugReport` kararı reddeder.
 */
class BugReportAdminFragment : Fragment() {

    private var _binding: FragmentBugReportAdminBinding? = null
    private val binding get() = _binding!!
    private var listener: ListenerRegistration? = null
    private val adapter = PendingAdapter { report -> openReview(report) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBugReportAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { close() }
        binding.btnClose.setOnClickListener { close() }
        binding.adminList.layoutManager = LinearLayoutManager(requireContext())
        binding.adminList.adapter = adapter

        listener = BugReports.listenPending(
            onChange = { reports ->
                val b = _binding ?: return@listenPending
                b.adminProgress.visibility = View.GONE
                b.adminSummary.text = if (reports.isEmpty()) {
                    appString(R.string.bug_report_admin_empty)
                } else {
                    appString(R.string.bug_report_admin_pending_count, reports.size)
                }
                adapter.submitList(reports)
            },
            onError = {
                val b = _binding ?: return@listenPending
                b.adminProgress.visibility = View.GONE
                b.adminSummary.text = appString(R.string.bug_report_admin_no_access)
            },
        )
    }

    private fun openReview(report: BugReports.Report) {
        BugReportReviewDialogFragment.newInstance(report).show(childFragmentManager, "BugReportReview")
    }

    private fun close() {
        val main = activity as? MainActivity
        if (main != null) {
            main.finishTasksOverlayAnimated("BugReportAdminFragment.close")
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        listener = null
        _binding = null
    }

    private class PendingAdapter(
        private val onClick: (BugReports.Report) -> Unit,
    ) : ListAdapter<BugReports.Report, PendingAdapter.VH>(
        object : DiffUtil.ItemCallback<BugReports.Report>() {
            override fun areItemsTheSame(a: BugReports.Report, b: BugReports.Report) = a.id == b.id
            override fun areContentsTheSame(a: BugReports.Report, b: BugReports.Report) = a == b
        },
    ) {
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemBugReportAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val report = getItem(position)
            val b = holder.b
            b.adminDate.text = if (report.createdAtMs > 0) dateFormat.format(Date(report.createdAtMs)) else ""
            b.adminText.text = report.text
            b.adminDevice.text = report.deviceLine
            b.root.setOnClickListener { onClick(report) }

            b.adminThumb.setImageDrawable(null)
            b.adminThumb.tag = report.imagePath
            // Görsel isteğe bağlı; yoksa küçük resim yeri boş kalmasın.
            b.adminThumb.visibility = if (report.imagePath.isBlank()) View.GONE else View.VISIBLE
            if (report.imagePath.isBlank()) return
            BugReports.imageUrl(report.imagePath) { uri ->
                // Satır bu arada başka bir bildirime geçmiş olabilir.
                if (uri == null || b.adminThumb.tag != report.imagePath) return@imageUrl
                Glide.with(b.adminThumb).load(uri).centerCrop().into(b.adminThumb)
            }
        }

        class VH(val b: ItemBugReportAdminBinding) : RecyclerView.ViewHolder(b.root)
    }
}
