package com.example.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentAvatarCustomBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Avatar düzenleme ekranı (Profil → avatara dokun).
 *
 * Şimdilik tek tür var (Personas); birden çok [AvatarStyle] olursa üstte tür seçimi çıkıyor ve
 * her türün son hâli ayrı tutuluyor. "Kaydet" ekrandaki türü kullanılan avatar yapar; "Kapat"
 * kaydetmeden çıkar.
 *
 * Seçenek kutularında düz bir ad yerine o seçenekle çizilmiş avatarın kendisi gösteriliyor:
 * çocuk okumadan, gördüğünü seçiyor. Göz/ağız gibi küçük parçalarda kutu yüze yaklaşıyor
 * ([AvatarStyle.faceCloseUp]); tam avatarda seçilemeyecek kadar küçük kalıyorlardı.
 */
class AvatarCustomFragment : Fragment() {

    private var _binding: FragmentAvatarCustomBinding? = null
    private val binding get() = _binding!!

    /** Her türün ekrandaki (henüz kaydedilmemiş olabilir) hâli. */
    private val drafts = HashMap<AvatarStyle, AvatarConfig>()
    private var config = AvatarConfig.default(AvatarStyle.PERSONAS)
    private var tab: AvatarTab = AvatarStyle.PERSONAS.tabs.first()
    private var options: List<String> = emptyList()
    private lateinit var adapter: OptionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAvatarCustomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { close() }
        binding.btnClose.setOnClickListener { close() }
        binding.btnSave.setOnClickListener {
            AvatarStore.save(requireContext(), config)
            (activity as? MainActivity)?.refreshProfileNavIcon()
            Toast.makeText(requireContext(), "Avatarın kaydedildi", Toast.LENGTH_SHORT).show()
            close()
        }
        binding.btnRandom.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            applyConfig(AvatarConfig.random(requireContext(), config.style))
        }

        adapter = OptionAdapter()
        binding.optionGrid.layoutManager = GridLayoutManager(requireContext(), GRID_COLUMNS)
        binding.optionGrid.adapter = adapter

        val ctx = requireContext()
        for (style in AvatarStyle.entries) drafts[style] = AvatarStore.load(ctx, style)
        val active = AvatarStore.loadActive(ctx).style
        // Tek tür varken seçim çubuğu gereksiz.
        binding.styleChips.visibility = if (AvatarStyle.entries.size > 1) View.VISIBLE else View.GONE
        addChips(binding.styleChips, AvatarStyle.entries.map { it.label }, AvatarStyle.entries.indexOf(active)) { i ->
            showStyle(AvatarStyle.entries[i])
        }
        showStyle(active)
    }

    private fun showStyle(style: AvatarStyle) {
        if (config.style != style) drafts[config.style] = config
        config = drafts[style] ?: AvatarConfig.default(style)
        binding.avatarPreview.config = config
        binding.categoryChips.removeAllViews()
        addChips(binding.categoryChips, style.tabs.map { it.label }, 0) { i -> showTab(style.tabs[i]) }
        binding.categoryScroll.scrollTo(0, 0)
        showTab(style.tabs.first())
    }

    private fun showTab(newTab: AvatarTab) {
        tab = newTab
        options = AvatarArt.options(requireContext(), config.style, newTab)
        adapter.notifyDataSetChanged()
        binding.optionGrid.scrollToPosition(0)
    }

    /** [group]'a tek seçimli çipler ekler; çipe dokununca [onSelect] sıra numarasıyla çağrılır. */
    private fun addChips(group: ChipGroup, labels: List<String>, checked: Int, onSelect: (Int) -> Unit) {
        val ctx = requireContext()
        val chipColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(ctx, R.color.dark_primary), CHIP_IDLE_COLOR),
        )
        val textColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(ctx, R.color.background_color), ContextCompat.getColor(ctx, R.color.dark_text)),
        )
        labels.forEachIndexed { i, label ->
            val chip = Chip(ctx).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = chipColors
                setTextColor(textColors)
                chipStrokeWidth = 0f
                isChecked = i == checked
                setOnClickListener { onSelect(i) }
            }
            group.addView(chip)
        }
    }

    private fun currentValue(): String {
        val t = tab
        // Desen yalnızca tişörtte görünüyor; başka kıyafette hiçbir desen seçili görünmesin.
        if (t is AvatarTab.Part && t.requires != null && config.values[t.requires.first] != t.requires.second) return "\u0000"
        return config.values[t.key].orEmpty()
    }

    private fun withValue(value: String): AvatarConfig {
        val t = tab
        var c = config.with(t.key, value)
        if (t is AvatarTab.Part && t.requires != null) c = c.with(t.requires.first, t.requires.second)
        return c
    }

    private fun select(value: String) {
        if (currentValue() == value) return
        applyConfig(withValue(value))
    }

    private fun applyConfig(newConfig: AvatarConfig) {
        config = newConfig
        binding.avatarPreview.config = newConfig
        bouncePreview()
        // Seçili çerçeve değişiyor; küçük resimler de yeni avatardan çiziliyor.
        adapter.notifyDataSetChanged()
    }

    /** Alt bardaki sekme zıplamasıyla aynı his: değişiklik görülsün diye önizleme bir kez zıplar. */
    private fun bouncePreview() {
        val v = binding.avatarPreview
        v.animate().cancel()
        v.scaleX = 0.92f
        v.scaleY = 0.92f
        v.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(3f)).start()
    }

    /** Profilden back stack'e eklenerek açılıyor; kapatmak bir geri adım. */
    private fun close() {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class OptionAdapter : RecyclerView.Adapter<OptionAdapter.VH>() {

        inner class VH(val cell: SquareFrameLayout, val avatar: AvatarView, val swatch: View, val none: ImageView) :
            RecyclerView.ViewHolder(cell)

        override fun getItemCount(): Int = options.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val dp = ctx.resources.displayMetrics.density
            val cell = SquareFrameLayout(ctx).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { setMargins((5 * dp).toInt(), (5 * dp).toInt(), (5 * dp).toInt(), (5 * dp).toInt()) }
                background = ContextCompat.getDrawable(ctx, R.drawable.avatar_option_bg)
                val pad = (4 * dp).toInt()
                setPadding(pad, pad, pad, pad)
            }
            // Yakın planda avatar kutuyu dolduruyor; köşeleri kutununkine uysun diye yuvarlak kırpılıyor.
            val corner = 18 * dp
            val avatar = AvatarView(ctx).apply {
                showBackground = false
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, corner)
                    }
                }
                clipToOutline = true
            }
            cell.addView(avatar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            val swatch = View(ctx).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            }
            val swatchSize = (40 * dp).toInt()
            cell.addView(swatch, FrameLayout.LayoutParams(swatchSize, swatchSize, Gravity.CENTER))
            // "Yok" seçeneği: avatar yerine yalnızca yasak işareti.
            val none = ImageView(ctx).apply { setImageResource(R.drawable.forbidden_ic) }
            val noneSize = (44 * dp).toInt()
            cell.addView(none, FrameLayout.LayoutParams(noneSize, noneSize, Gravity.CENTER))
            return VH(cell, avatar, swatch, none)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = tab
            val value = options[position]
            holder.cell.isSelected = currentValue() == value
            holder.cell.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                select(value)
            }
            holder.none.visibility = if (value.isEmpty()) View.VISIBLE else View.GONE
            if (value.isEmpty()) {
                holder.avatar.visibility = View.GONE
                holder.swatch.visibility = View.GONE
            } else if (t is AvatarTab.Color && t.swatch) {
                holder.avatar.visibility = View.GONE
                holder.swatch.visibility = View.VISIBLE
                (holder.swatch.background as GradientDrawable).setColor(Color.parseColor("#$value"))
            } else {
                // Ten, saç ve kıyafet renginde de kutuda o renge boyanmış avatar var: rengin
                // üstünde nasıl durduğu düz bir renk dairesinden daha anlaşılır.
                holder.avatar.visibility = View.VISIBLE
                holder.swatch.visibility = View.GONE
                holder.avatar.closeUp = t is AvatarTab.Part && t.closeUp
                holder.avatar.config = withValue(value)
            }
        }
    }

    private companion object {
        const val GRID_COLUMNS = 4
        const val CHIP_IDLE_COLOR = 0xFF22303A.toInt()
    }
}

/** Genişliği kadar yüksek kutu: ızgaradaki seçenekler kare dursun. */
class SquareFrameLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
