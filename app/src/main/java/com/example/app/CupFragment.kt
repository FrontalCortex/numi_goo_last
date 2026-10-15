package com.example.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.animation.ObjectAnimator
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.databinding.FragmentCupBinding
import com.example.app.model.LessonItem

class CupFragment : Fragment() {
    private var _binding: FragmentCupBinding? = null
    private val binding get() = _binding!!
    private var targetTime: String = ""
    private var dersPuani: Int = 0
    private var toplamPuan: Int = 0
    private lateinit var claimButton: View
    private lateinit var cupImageView: ImageView
    private lateinit var cupName: TextView
    private lateinit var motivationText: TextView
    private lateinit var lessonItem : LessonItem

    private var icon: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)!!
        cupImageView = binding.cupImageView
        cupName = binding.cupNameTextView
        motivationText = binding.motivationTextView
        claimButton = binding.claimButton
        arguments?.let { bundle ->
            targetTime = bundle.getString("time", "")
            dersPuani = bundle.getInt("dersPuani", 0)
            toplamPuan = bundle.getInt("toplamPuan", 0)
        }
        ChangeCupIcon()
        record()
        showCup()
        resumeClick()

    }

    private fun resumeClick(){
        binding.claimButton.setOnClickListener {
            // Önce MapFragment'ı aç
            updateMapProgress()

            // Guide panel ile birlikte MapFragment'ı aç
            val mapFragment = MapFragment.newInstance(showGuide = true)
            Log.d("klavyiye","work")
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerID, mapFragment)
                .remove(this@CupFragment)
                .commit()

            // Sonra kupa ikonunu güncelle ve fragment'ları temizle

        }
    }
    private fun record() {
        if (lessonItem.type != LessonItem.TYPE_CHEST) return
        val m = LessonItem.mergeChestRun(lessonItem, toplamPuan, SeasonClock.currentSeason())
        lessonItem.record = m.record
        lessonItem.leaderboardSeasonId = m.leaderboardSeasonId
        lessonItem.leaderboardSeasonBest = m.leaderboardSeasonBest
    }
    private fun ChangeCupIcon(){
        val cupPoint1 = lessonItem.cupPoint1
        val cupPoint2 = lessonItem.cupPoint2
        if (cupPoint1 != null && toplamPuan >= cupPoint1) {
            icon= R.drawable.cup_ic3
            cupName.text = appString(R.string.ui_bilgelik_kupasi)
            val cupMotivation = listOf(appString(R.string.ui_tum_sorular_senin_zekanla_boy_olcustu),
                appString(R.string.ui_zirve_senin_bu_basari_tesaduf_degil), appString(R.string.ui_mukemmel_bir_is_cikardin),
                appString(R.string.ui_bu_seviye_sadece_cok_calisanlarin_ulasabildi),
                appString(R.string.ui_sen_artik_bu_konunun_ustasisin_neyi),)
            motivationText.text = cupMotivation.random()
        } else if (cupPoint2 != null && toplamPuan >= cupPoint2) {
            icon = R.drawable.cup_ic2
            cupName.text = appString(R.string.ui_usta_kupasi)
            val cupMotivation = listOf(appString(R.string.ui_gercek_ustalik_bilgi_sabir_ve_kararlilikla),
                appString(R.string.ui_gayet_iyi), appString(R.string.ui_biraz_daha_pratikle_zirveye_ulasman_an),
                appString(R.string.ui_azmin_gucuyle_her_adim_seni_daha),
                appString(R.string.ui_cok_iyi_gidiyorsun_birkac_adim_sonra),
                appString(R.string.ui_gelisimin_fark_ediliyor_pes_etme_devam))
            motivationText.text = cupMotivation.random()
        } else if (toplamPuan > 0) {
            icon = R.drawable.cup_ic
            cupName.text = appString(R.string.ui_deneyim_kupasi)
            val cupMotivation = listOf(appString(R.string.ui_her_usta_bir_zamanlar_amatordu_sen),
                appString(R.string.ui_cesaret_ettin_basladin_bu_en_onemli),
                appString(R.string.ui_unutma_basari_tekrar_tekrar_denemekten_gecer),
                appString(R.string.ui_bugun_attigin_adim_yarinin_basarisini_baslat))
            motivationText.text = cupMotivation.random()
        }else{
            icon = R.drawable.cup_ic
            cupName.text = appString(R.string.ui_deneyim_kupasi)
            val cupMotivation = listOf(appString(R.string.ui_ayip_olmasin_diye_bu_kupayi_verdik),
                appString(R.string.ui_biraz_fazla_mi_yavas_oldu_ne),
                appString(R.string.ui_yanii_hic_yapmamis_olmaktan_iyi),
                appString(R.string.ui_en_azindan_ekrani_actin_bu_da),
                appString(R.string.ui_kupayi_biz_de_pek_vermek_istemedik),
                appString(R.string.ui_kupa_geldi_ama_vicdanimiz_biraz_sizladi))
            motivationText.text = cupMotivation.random()
        }
        cupImageView.setImageResource(icon)
    }
    private fun updateMapProgress() {
        val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex) //Global verilerden tıklanan indeksteki adım öğesini alıyor
        val lessonItem2 = LessonManager.getLessonItem(mapFragmentStepIndex +1)
        lessonItem?.let { item ->
            // İlk adım true, diğerleri false olacak şekilde stepCompletionStatus oluştur
            val newStepCompletionStatus = List(item.stepCount) { index -> index < item.currentStep }

            if(item.stepCount == item.currentStep){
                val updatedItem = item.copy(
                    stepCompletionStatus = newStepCompletionStatus,
                    stepIsFinish = true,
                    stepCupIcon = icon
                )
                LessonManager.updateLessonItem(requireContext(),mapFragmentStepIndex, updatedItem)

                lessonItem2?.let { item2 ->
                    val updatedItem2 = item2.copy(
                        isCompleted = true
                    )
                    LessonManager.updateLessonItem(requireContext(),mapFragmentStepIndex +1, updatedItem2)
                }
            }
            else{
                val updatedItem = item.copy(
                    stepCompletionStatus = newStepCompletionStatus,
                    currentStep = item.currentStep + 1,
                    startStepNumber = item.startStepNumber?.plus(1)
                )
                LessonManager.updateLessonItem(requireContext(),mapFragmentStepIndex, updatedItem)
            }
        }
    }

    private fun showCup() {
        binding.timerTextView.visibility = View.GONE
        binding.cupContainer.visibility = View.VISIBLE
        binding.cupContainer.alpha = 0f
        
        // Alpha animasyonu
        val alphaAnimator = ObjectAnimator.ofFloat(
            binding.cupContainer,
            "alpha",
            0f,
            1f
        ).apply {
            duration = 1000 // 1 saniye sürecek
        }
        
        alphaAnimator.start()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // View hiyerarşisini bırak (fragment geri yığınında beklerken bellekte kalıyordu).
        _binding = null
    }
}
