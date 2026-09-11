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
 * Girişi [ParentGate] korur (bkz. [AccountSettingsFragment]).
 */
class ParentPanelFragment : Fragment() {

    private var _binding: FragmentParentPanelBinding? = null
    private val binding: FragmentParentPanelBinding
        get() = _binding!!

    private val adapter = ParentReportAdapter()

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

        // screen_view otomatik: NumiGooApplication her fragment resume'unda logluyor.
        load()
    }

    private fun load() {
        showLoading()
        ParentReportRepository.load { report, error ->
            // Fragment kapanmış olabilir: Firestore geri çağrısı yaşam döngüsüne bakmaz.
            if (_binding == null || !isAdded) return@load
            when {
                report == null -> showMessage(
                    if (error != null) "İlerleme bilgisi alınamadı. İnternet bağlantınızı kontrol edip tekrar deneyin."
                    else "İlerleme bilgisi alınamadı.",
                )
                report.isEmpty -> showMessage(
                    "Henüz başlanmış bir ders yok. Çocuğunuz ilk dersi açtığında ilerlemesi burada görünecek."
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
