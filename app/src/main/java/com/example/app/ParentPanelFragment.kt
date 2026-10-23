package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.databinding.FragmentParentPanelBinding

/**
 * Veli paneli: çocuğun part 1-6 arasındaki ilerlemesini tek ekranda gösterir.
 *
 * Veri [ParentReportRepository]'den gelir; bu ekran hiçbir şey YAZMAZ — bir rapor ekranının
 * çocuğun ilerlemesine dokunması için bir sebep yok.
 *
 * Girişte doğrulama yok: panel çocuğun kendi ilerlemesinden başka bir şey göstermiyor.
 *
 * Aynı ekran kurs öğretmenine öğrencisinin ilerlemesi olarak da açılıyor ([newForStudent],
 * profildeki ilerleme düğmesinden). O zaman başlık "Veli Paneli" değil öğrencinin adı; boş
 * rapor metni de veliye değil öğretmene yazılmış olanı.
 */
class ParentPanelFragment : Fragment() {

    companion object {
        private const val ARG_STUDENT_UID = "student_uid"
        private const val ARG_STUDENT_NAME = "student_name"

        fun newForStudent(studentUid: String, studentName: String) = ParentPanelFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_STUDENT_UID, studentUid)
                putString(ARG_STUDENT_NAME, studentName)
            }
        }
    }

    private var _binding: FragmentParentPanelBinding? = null
    private val binding: FragmentParentPanelBinding
        get() = _binding!!

    private val adapter = ParentReportAdapter()

    /** Öğretmen öğrencisine bakıyorsa öğrencinin uid'si; veli panelinde null. */
    private val studentUid: String? get() = arguments?.getString(ARG_STUDENT_UID)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentParentPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.parentPanelList.layoutManager = LinearLayoutManager(requireContext())
        binding.parentPanelList.adapter = adapter
        if (studentUid != null) {
            binding.parentPanelHeaderTitle.text =
                appString(R.string.class_student_progress_title, arguments?.getString(ARG_STUDENT_NAME).orEmpty())
        }

        // screen_view otomatik: NumiGooApplication her fragment resume'unda logluyor.
        load()
    }

    private fun load() {
        showLoading()
        ParentReportRepository.load(studentUid) { report, error ->
            // Fragment kapanmış olabilir: Firestore geri çağrısı yaşam döngüsüne bakmaz.
            if (_binding == null || !isAdded) return@load
            when {
                report == null -> showMessage(
                    if (error != null) appString(R.string.ui_ilerleme_bilgisi_alinamadi_internet_baglanti)
                    else appString(R.string.ui_ilerleme_bilgisi_alinamadi),
                )
                report.isEmpty -> showMessage(
                    appString(
                        if (studentUid != null) R.string.class_student_progress_empty
                        else R.string.ui_henuz_baslanmis_bir_ders_yok_cocugunuz
                    )
                )
                else -> showReport(report)
            }
        }
    }

    private fun showLoading() {
        binding.parentPanelLoading.visibility = View.VISIBLE
        binding.parentPanelMessage.visibility = View.GONE
        binding.parentPanelList.visibility = View.GONE
    }

    private fun showMessage(text: String) {
        binding.parentPanelLoading.visibility = View.GONE
        binding.parentPanelMessage.text = text
        binding.parentPanelMessage.visibility = View.VISIBLE
        binding.parentPanelList.visibility = View.GONE
    }

    private fun showReport(report: ParentReportRepository.Report) {
        binding.parentPanelLoading.visibility = View.GONE
        binding.parentPanelMessage.visibility = View.GONE
        binding.parentPanelList.visibility = View.VISIBLE
        adapter.submit(report)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.parentPanelList.adapter = null
        _binding = null
    }
}
