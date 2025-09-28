package com.example.numigoo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.numigoo.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private var currentAvatarUrl: String = ""
    
    // Avatar özelleştirme seçenekleri
    data class AvatarOption(
        val id: String,
        val name: String,
        val previewUrl: String
    )
    
    private val hairStyles = listOf(
        AvatarOption("shortHairShortCurly", "Kıvırcık Kısa", ""),
        AvatarOption("longHairStraight", "Düz Uzun", ""),
        AvatarOption("shortHairShortFlat", "Düz Kısa", ""),
        AvatarOption("longHairBigHair", "Büyük Saç", ""),
        AvatarOption("shortHairDreads01", "Dreadlock", ""),
        AvatarOption("longHairCurly", "Kıvırcık Uzun", ""),
        AvatarOption("shortHairFrizzle", "Kabarmış", ""),
        AvatarOption("longHairFro", "Afro", "")
    )
    
    private val eyeStyles = listOf(
        AvatarOption("happy", "Mutlu", ""),
        AvatarOption("sad", "Üzgün", ""),
        AvatarOption("wink", "Göz Kırpan", ""),
        AvatarOption("squint", "Kısık", ""),
        AvatarOption("default", "Normal", ""),
        AvatarOption("close", "Kapalı", ""),
        AvatarOption("cry", "Ağlayan", ""),
        AvatarOption("dizzy", "Baş Döndüren", "")
    )
    
    private val skinColors = listOf(
        AvatarOption("light", "Açık", ""),
        AvatarOption("brown", "Kahverengi", ""),
        AvatarOption("darkBrown", "Koyu Kahve", ""),
        AvatarOption("pale", "Soluk", ""),
        AvatarOption("black", "Siyah", ""),
        AvatarOption("tanned", "Bronz", ""),
        AvatarOption("yellow", "Sarı", ""),
        AvatarOption("red", "Kırmızı", "")
    )
    
    private val clothingColors = listOf(
        AvatarOption("blue", "Mavi", ""),
        AvatarOption("red", "Kırmızı", ""),
        AvatarOption("green", "Yeşil", ""),
        AvatarOption("yellow", "Sarı", ""),
        AvatarOption("purple", "Mor", ""),
        AvatarOption("pink", "Pembe", ""),
        AvatarOption("orange", "Turuncu", ""),
        AvatarOption("black", "Siyah", "")
    )
    
    private var currentHairStyle = 0
    private var currentEyeStyle = 0
    private var currentSkinColor = 0
    private var currentClothingColor = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAvatarArea()
        
        // Layout tamamlandıktan sonra avatar yükle
        binding.avatarImageView.post {
            loadSavedAvatar()
        }
        
        // Debug: ImageView durumunu kontrol et
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d("Avatar", "ImageView visibility: ${binding.avatarImageView.visibility}")
            Log.d("Avatar", "ImageView width: ${binding.avatarImageView.width}, height: ${binding.avatarImageView.height}")
            Log.d("Avatar", "ImageView drawable: ${binding.avatarImageView.drawable}")
        }, 2000)
    }

    private fun setupAvatarArea() {
        // Avatar alanına tıklama
        binding.avatarArea.setOnClickListener {
            showAvatarCustomizationDialog()
        }
        
        // Varsayılan avatar oluştur
        if (currentAvatarUrl.isEmpty()) {
            // Test için basit URL
            currentAvatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=test&backgroundColor=b6e3f4&size=200"
            loadAvatar(currentAvatarUrl)
        }
    }

    private fun showAvatarCustomizationDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_avatar_customization, null)
        
        val avatarPreview = dialogView.findViewById<android.widget.ImageView>(R.id.avatarPreview)
        val hairOptionsContainer = dialogView.findViewById<LinearLayout>(R.id.hairOptionsContainer)
        val eyeOptionsContainer = dialogView.findViewById<LinearLayout>(R.id.eyeOptionsContainer)
        val skinOptionsContainer = dialogView.findViewById<LinearLayout>(R.id.skinOptionsContainer)
        val clothingOptionsContainer = dialogView.findViewById<LinearLayout>(R.id.clothingOptionsContainer)
        val btnSave = dialogView.findViewById<android.widget.Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        
        // Dialog'u önce oluştur
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Avatar Özelleştir")
            .setView(dialogView)
            .create()
        
        // İlk avatar'ı yükle
        loadAvatar(currentAvatarUrl, avatarPreview)
        
        // Seçenek butonlarını oluştur
        createOptionButtons(hairOptionsContainer, hairStyles, currentHairStyle) { index ->
            currentHairStyle = index
            currentAvatarUrl = createAvatarUrl()
            loadAvatar(currentAvatarUrl, avatarPreview)
            updateButtonSelection(hairOptionsContainer, index)
        }
        
        createOptionButtons(eyeOptionsContainer, eyeStyles, currentEyeStyle) { index ->
            currentEyeStyle = index
            currentAvatarUrl = createAvatarUrl()
            loadAvatar(currentAvatarUrl, avatarPreview)
            updateButtonSelection(eyeOptionsContainer, index)
        }
        
        createOptionButtons(skinOptionsContainer, skinColors, currentSkinColor) { index ->
            currentSkinColor = index
            currentAvatarUrl = createAvatarUrl()
            loadAvatar(currentAvatarUrl, avatarPreview)
            updateButtonSelection(skinOptionsContainer, index)
        }
        
        createOptionButtons(clothingOptionsContainer, clothingColors, currentClothingColor) { index ->
            currentClothingColor = index
            currentAvatarUrl = createAvatarUrl()
            loadAvatar(currentAvatarUrl, avatarPreview)
            updateButtonSelection(clothingOptionsContainer, index)
        }
        
        // Kaydet butonu
        btnSave.setOnClickListener {
            saveAvatar()
            dialog.dismiss()
            Toast.makeText(context, "Avatar kaydedildi!", Toast.LENGTH_SHORT).show()
        }
        
        // İptal butonu
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun createOptionButtons(
        container: LinearLayout,
        options: List<AvatarOption>,
        selectedIndex: Int,
        onOptionSelected: (Int) -> Unit
    ) {
        container.removeAllViews()
        
        options.forEachIndexed { index, option ->
            val buttonLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(8, 8, 8, 8)
                background = if (index == selectedIndex) {
                    android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#6A4A4A"))
                } else {
                    android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#4A4A4A"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                }
                
                setOnClickListener {
                    onOptionSelected(index)
                }
            }
            
            // Küçük avatar önizlemesi
            val previewImage = android.widget.ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40)
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#333333"))
            }
            
            // Seçenek adı
            val textView = android.widget.TextView(requireContext()).apply {
                text = option.name
                textSize = 10f
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4
                }
            }
            
            buttonLayout.addView(previewImage)
            buttonLayout.addView(textView)
            container.addView(buttonLayout)
        }
    }
    
    private fun updateButtonSelection(container: LinearLayout, selectedIndex: Int) {
        for (i in 0 until container.childCount) {
            val buttonLayout = container.getChildAt(i) as LinearLayout
            buttonLayout.background = if (i == selectedIndex) {
                android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#6A4A4A"))
            } else {
                android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#4A4A4A"))
            }
        }
    }

    private fun createAvatarUrl(): String {
        // Önce basit bir test URL'i oluşturalım
        val url = "https://api.dicebear.com/7.x/avataaars/png?" +
               "seed=user${System.currentTimeMillis()}&" +
               "backgroundColor=b6e3f4&" +
               "size=200"
        
        Log.d("Avatar", "Created URL: $url")
        return url
    }

    private fun loadAvatar(url: String, imageView: android.widget.ImageView? = null) {
        val targetImageView = imageView ?: binding.avatarImageView
        
        Log.d("Avatar", "Loading avatar from URL: $url")
        Log.d("Avatar", "ImageView width: ${targetImageView.width}, height: ${targetImageView.height}")
        Log.d("Avatar", "ImageView layout params: ${targetImageView.layoutParams}")
        Log.d("Avatar", "ImageView visibility: ${targetImageView.visibility}")
        
        // Glide ile daha iyi hata yönetimi
        Glide.with(this)
            .load(url)
            .error(android.R.drawable.ic_menu_report_image)
            .timeout(10000) // 10 saniye timeout
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("Avatar", "Glide load failed: ${e?.message}")
                    Log.e("Avatar", "Model: $model")
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("Avatar", "Glide load successful")
                    return false
                }
            })
            .into(targetImageView)
    }

    private fun saveAvatar() {
        // SharedPreferences'a kaydet
        val sharedPref = requireActivity().getSharedPreferences("avatar_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("avatar_url", currentAvatarUrl)
            putInt("hair_style", currentHairStyle)
            putInt("eye_style", currentEyeStyle)
            putInt("skin_color", currentSkinColor)
            putInt("clothing_color", currentClothingColor)
            apply()
        }
        
        // Ana avatar'ı güncelle
        loadAvatar(currentAvatarUrl)
    }

    private fun loadSavedAvatar() {
        val sharedPref = requireActivity().getSharedPreferences("avatar_prefs", Context.MODE_PRIVATE)
        
        // Eski kayıtlı avatar'ları temizle (test için)
        sharedPref.edit().clear().apply()
        
        // Direkt gerçek avatar'ı yükle
        val realAvatarUrl = createAvatarUrl()
        currentAvatarUrl = realAvatarUrl
        loadAvatar(realAvatarUrl)
    }
}