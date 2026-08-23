package com.example.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentCupHistoryBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Bir kupa tipinin (ör. "addition_abacus_cup") zaman içindeki değişimini
 * Haftalık/Aylık/Yıllık/Tüm Zamanlar sekmeleriyle grafik olarak gösterir.
 *
 * Her sekme, adının işaret ettiği tek bir sabit pencereyi gösterir (birikimli/genişleyen bir
 * aralık değil):
 * - **Haftalık**: son 7 günün her birinin son değeri (≤7 nokta)
 * - **Aylık**: son 30 günün her birinin son değeri (≤30 nokta)
 * - **Yıllık**: son 12 ayın her birinin son değeri (≤12 nokta)
 * - **Tüm Zamanlar**: hesabın tüm geçmişi (çok noktalıysa aylık gruplanarak seyreltilir)
 *
 * "Günlük" sekmesi bilinçli olarak yok: veri günde tek nokta (günün son değeri) olarak
 * tutulduğundan, "Günlük" başlığı kullanıcıda yanlışlıkla o gün içindeki saatlerin
 * listeleneceği izlenimini veriyordu.
 *
 * Veri [CupHistoryRepository] üzerinden `users/{uid}/cupHistory` koleksiyonundan okunur.
 */
class CupHistoryFragment : Fragment() {

    private var _binding: FragmentCupHistoryBinding? = null
    private val binding get() = _binding!!

    private var uid: String = ""
    private var field: String = ""
    private var title: String = ""
    private var animFile: String = ""

    /** Sekme başına önbelleklenmiş noktalar — aynı sekmeye tekrar geçişte yeniden Firestore okuması yapılmaz. */
    private val cache = mutableMapOf<RangeTab, List<Pair<String, Int>>>()

    private enum class RangeTab { WEEKLY, MONTHLY, YEARLY, ALL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uid = arguments?.getString(ARG_UID).orEmpty()
        field = arguments?.getString(ARG_FIELD).orEmpty()
        title = arguments?.getString(ARG_TITLE).orEmpty()
        animFile = arguments?.getString(ARG_ANIM).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCupHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCupHistoryBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.tvCupHistoryTitle.text = title
        if (animFile.isNotEmpty()) {
            binding.cupHistoryLottie.setAnimation(animFile)
            binding.cupHistoryLottie.playAnimation()
        }

        setupChart()
        setupTabs()
        loadCurrentScore()
        loadForTab(RangeTab.WEEKLY)
    }

    private fun setupTabs() {
        // "Günlük" sekmesi kaldırıldı: başlık, o gün içindeki saatlerin listeleneceği izlenimi
        // veriyordu — oysa yalnızca günün son değerini tutuyoruz. En küçük birim artık "Haftalık".
        val labels = listOf("Haftalık", "Aylık", "Yıllık", "Tüm Zamanlar")
        labels.forEach { label ->
            binding.tabLayoutCupHistory.addTab(binding.tabLayoutCupHistory.newTab().setText(label))
        }
        binding.tabLayoutCupHistory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                loadForTab(RangeTab.values()[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupChart() {
        val chart = binding.cupHistoryChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = Color.parseColor("#93A5B3")
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = Color.parseColor("#1A3040")
        chart.axisLeft.granularity = 1f
        chart.axisLeft.isGranularityEnabled = true
        chart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = value.toInt().toString()
        }
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = Color.parseColor("#93A5B3")
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.setNoDataText("")
    }

    private fun loadCurrentScore() {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("cupWayProgress").document("progress")
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val score = (doc?.get(field) as? Number)?.toInt() ?: 200
                binding.tvCupHistoryScore.text = score.toString()
            }
    }

    /**
     * Seçilen sekme için veriyi getirir. Haftalık/Aylık/Yıllık, hesabın tüm ömrünü değil yalnızca
     * sekmenin kendi sabit penceresini (son 7/30/365 gün) sorgular
     * ([CupHistoryRepository.fetchHistorySince]) — okuma maliyeti hesap yaşından bağımsız sabit
     * kalır. Yalnızca Tüm Zamanlar tüm geçmişi gerektirdiği için tam koleksiyonu okur, ve bu da
     * kullanıcı o sekmeye gittiğinde (tembel) yapılır.
     */
    private fun loadForTab(tab: RangeTab) {
        cache[tab]?.let { renderChart(tab, it); return }

        binding.cupHistoryProgress.visibility = View.VISIBLE
        binding.tvCupHistoryEmpty.visibility = View.GONE

        val onFetched: (List<Pair<String, Int>>) -> Unit = { points ->
            if (isAdded) {
                binding.cupHistoryProgress.visibility = View.GONE
                cache[tab] = points

                val stillSelected = RangeTab.values()[binding.tabLayoutCupHistory.selectedTabPosition.coerceAtLeast(0)]
                if (stillSelected == tab) renderChart(tab, points)
            }
        }

        when (tab) {
            RangeTab.WEEKLY -> CupHistoryRepository.fetchHistorySince(uid, field, daysAgo(7), onFetched)
            RangeTab.MONTHLY -> CupHistoryRepository.fetchHistorySince(uid, field, daysAgo(30), onFetched)
            RangeTab.YEARLY -> CupHistoryRepository.fetchHistorySince(uid, field, daysAgo(365), onFetched)
            RangeTab.ALL -> CupHistoryRepository.fetchHistory(uid, field, onFetched)
        }
    }

    private fun renderChart(tab: RangeTab, points: List<Pair<String, Int>>) {
        if (!isAdded) return
        val bucketed = bucket(points, tab)
        if (bucketed.isEmpty()) {
            binding.cupHistoryChart.clear()
            binding.tvCupHistoryEmpty.visibility = View.VISIBLE
            binding.tvCupHistorySingleHint.visibility = View.GONE
            return
        }
        binding.tvCupHistoryEmpty.visibility = View.GONE

        // Tek nokta varsa çizgi çizilemez (en az 2 nokta gerekir) — o noktayı görünür bir daire
        // olarak işaretleyip kullanıcıya kısa bir açıklama gösteriyoruz, yoksa grafik bomboş görünür.
        val isSinglePoint = bucketed.size == 1
        binding.tvCupHistorySingleHint.visibility = if (isSinglePoint) View.VISIBLE else View.GONE

        val entries = bucketed.mapIndexed { index, (_, value) -> Entry(index.toFloat(), value.toFloat()) }
        val labels = bucketed.map { formatLabel(tab, it.first) }

        val dataSet = LineDataSet(entries, "Kupa").apply {
            mode = LineDataSet.Mode.STEPPED
            color = Color.parseColor("#8B7CD9")
            setDrawFilled(true)
            fillColor = Color.parseColor("#8B7CD9")
            fillAlpha = 90
            setDrawCircles(isSinglePoint)
            setCircleColor(Color.parseColor("#8B7CD9"))
            circleRadius = 5f
            setDrawCircleHole(false)
            setDrawValues(false)
            lineWidth = 2f
            setDrawHighlightIndicators(false)
        }

        binding.cupHistoryChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.cupHistoryChart.xAxis.labelCount = minOf(labels.size, 6).coerceAtLeast(1)

        val (axisMin, axisMax) = niceAxisRange(bucketed.map { it.second })
        binding.cupHistoryChart.axisLeft.axisMinimum = axisMin.toFloat()
        binding.cupHistoryChart.axisLeft.axisMaximum = axisMax.toFloat()

        binding.cupHistoryChart.data = LineData(dataSet)
        binding.cupHistoryChart.invalidate()
    }

    /**
     * Y ekseni için küsüratsız, geniş ve 10'un katlarına yuvarlanmış bir aralık üretir.
     * Tek nokta ya da dar değer aralıklarında (MPAndroidChart'ın otomatik ölçeklemesi
     * gibi) grafiğin gereksiz yere yakınlaştırılıp virgüllü etiketler göstermesini engeller.
     */
    private fun niceAxisRange(values: List<Int>): Pair<Int, Int> {
        val dataMin = values.min()
        val dataMax = values.max()
        val span = dataMax - dataMin
        val padding = maxOf((span * 0.3f).toInt(), 20)
        val step = 10
        val axisMin = (((dataMin - padding).coerceAtLeast(0)) / step) * step
        val rawMax = dataMax + padding
        val axisMax = ((rawMax + step - 1) / step) * step
        return axisMin to axisMax
    }

    // --------------------------------------------------------------------------------------------
    // Aralığa göre gruplama (bucket'lama)
    // --------------------------------------------------------------------------------------------

    /**
     * [points] artık ilgili sekmeye göre zaten Firestore sorgusuyla sınırlandırılmış durumda
     * ([loadForTab]) — burada yalnızca görüntüleme için gruplama yapılır, tekrar tarih filtresi
     * uygulanmaz. Haftalık/Aylık zaten günlük çözünürlükte geldiği için ek gruplama gerekmez;
     * Yıllık, son 365 günü aya göre gruplayarak 12 aya indirger.
     */
    private fun bucket(points: List<Pair<String, Int>>, tab: RangeTab): List<Pair<String, Int>> {
        if (points.isEmpty()) return emptyList()
        return when (tab) {
            RangeTab.WEEKLY -> points
            RangeTab.MONTHLY -> points
            RangeTab.YEARLY -> groupLastPerKey(points) { it.substring(0, 7) }
            RangeTab.ALL -> if (points.size <= 60) points else groupLastPerKey(points) { it.substring(0, 7) }
        }
    }

    private fun groupLastPerKey(points: List<Pair<String, Int>>, keyFn: (String) -> String): List<Pair<String, Int>> {
        val grouped = LinkedHashMap<String, Pair<String, Int>>()
        for ((date, value) in points) {
            grouped[keyFn(date)] = date to value
        }
        return grouped.values.toList()
    }

    private fun daysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return DATE_FORMAT.format(cal.time)
    }

    private fun formatLabel(tab: RangeTab, dateStr: String): String {
        val date = DATE_FORMAT.parse(dateStr) ?: return dateStr
        val formatter = when (tab) {
            RangeTab.WEEKLY, RangeTab.MONTHLY -> SimpleDateFormat("d MMM", Locale("tr"))
            RangeTab.YEARLY, RangeTab.ALL -> SimpleDateFormat("MMM yy", Locale("tr"))
        }
        return formatter.format(date)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_UID = "uid"
        private const val ARG_FIELD = "field"
        private const val ARG_TITLE = "title"
        private const val ARG_ANIM = "anim"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun newInstance(uid: String, field: String, title: String, animFile: String): CupHistoryFragment {
            return CupHistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_UID, uid)
                    putString(ARG_FIELD, field)
                    putString(ARG_TITLE, title)
                    putString(ARG_ANIM, animFile)
                }
            }
        }
    }
}
