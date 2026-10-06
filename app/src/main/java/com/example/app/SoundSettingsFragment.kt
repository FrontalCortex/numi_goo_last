package com.example.app

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.app.abacus.AbacusSoundPlayer

class SoundSettingsFragment : Fragment() {

    private lateinit var switchSound: SwitchCompat
    private lateinit var switchTutorialSound: SwitchCompat
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchVibration: SwitchCompat
    private lateinit var btnClose: View

    /** Tür bazlı bildirim anahtarları: anahtar → kutu. Bkz. [NotificationPrefs]. */
    private lateinit var notifySwitches: Map<String, SwitchCompat>

    /** Ana anahtar ile tür anahtarları birbirini güncellerken dinleyicileri susturuyor. */
    private var suppressNotifyListeners = false

    private lateinit var rowAbacusSoundPicker: View
    private lateinit var textAbacusSoundCurrent: TextView
    private var selectedAbacusSound: Int = AbacusSoundPlayer.DEFAULT_SOUND_INDEX

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sound_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchSound = view.findViewById(R.id.switchSound)
        switchTutorialSound = view.findViewById(R.id.switchTutorialSound)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        switchVibration = view.findViewById(R.id.switchVibration)
        btnClose = view.findViewById(R.id.btnClose)

        notifySwitches = mapOf(
            NotificationPrefs.CHAT to view.findViewById(R.id.switchNotifyChat),
            NotificationPrefs.STREAK to view.findViewById(R.id.switchNotifyStreak),
            NotificationPrefs.REWARD to view.findViewById(R.id.switchNotifyReward),
        )

        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Mevcut ayarları yükle
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        switchTutorialSound.isChecked = prefs.getBoolean("tutorial_sound_enabled", true)
        switchVibration.isChecked = Haptics.isEnabled(requireContext())

        // Dinleyiciler
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        switchTutorialSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("tutorial_sound_enabled", isChecked).apply()
        }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Haptics.KEY, isChecked).apply()
        }

        setupNotificationSwitches()
        setupAbacusSoundSelector(view)

        btnClose.setOnClickListener {
            closeFragment()
        }
    }

    /**
     * Bildirim anahtarları: bir ana anahtar ve altında üç tür.
     *
     * ANA ANAHTAR NE YAPIYOR
     *   Üçünü birden aynı değere çekiyor ([NotificationPrefs.setAll]). Yalnızca eski tek
     *   anahtarı yazmak yetmezdi: tür anahtarı varsa o kazanıyor, yani kullanıcı bir kez
     *   ince ayar yaptıktan sonra ana anahtarı kapatınca bildirim gelmeye devam ederdi.
     *
     *   Ters yönde ana anahtar türlerden TÜRETİLİYOR: en az biri açıksa açık görünüyor.
     *   Kullanıcı tek tek hepsini kapattığında ana anahtarın açık kalması tutarsız olurdu.
     *
     * DİNLEYİCİ DÖNGÜSÜ
     *   `isChecked` programatik olarak değiştirildiğinde de dinleyici tetikleniyor; iki yönlü
     *   bağ [suppressNotifyListeners] olmadan sonsuz döngüye girerdi.
     */
    private fun setupNotificationSwitches() {
        val ctx = requireContext()

        notifySwitches.forEach { (type, box) ->
            box.isChecked = NotificationPrefs.isEnabled(ctx, type)
        }
        switchNotifications.isChecked = notifySwitches.values.any { it.isChecked }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (suppressNotifyListeners) return@setOnCheckedChangeListener
            // Guard'dan SONRA: programatik değişimler (alttaki üç anahtarın eşitlenmesi)
            // kullanıcı hareketi değil, ölçüme gitmemeli.
            AnalyticsLogger.notificationPrefChanged(AnalyticsLogger.NOTIFY_TYPE_ALL, isChecked)
            NotificationPrefs.setAll(ctx, isChecked)
            suppressNotifyListeners = true
            notifySwitches.values.forEach { it.isChecked = isChecked }
            suppressNotifyListeners = false
        }

        notifySwitches.forEach { (type, box) ->
            box.setOnCheckedChangeListener { _, isChecked ->
                if (suppressNotifyListeners) return@setOnCheckedChangeListener
                AnalyticsLogger.notificationPrefChanged(type, isChecked)
                NotificationPrefs.setEnabled(ctx, type, isChecked)
                suppressNotifyListeners = true
                switchNotifications.isChecked = notifySwitches.values.any { it.isChecked }
                suppressNotifyListeners = false
            }
        }
    }

    private fun setupAbacusSoundSelector(view: View) {
        rowAbacusSoundPicker = view.findViewById(R.id.rowAbacusSoundPicker)
        textAbacusSoundCurrent = view.findViewById(R.id.textAbacusSoundCurrent)

        selectedAbacusSound = AbacusSoundPlayer.getSelectedIndex(requireContext())
        textAbacusSoundCurrent.text = "Ses $selectedAbacusSound"

        rowAbacusSoundPicker.setOnClickListener {
            showAbacusSoundPickerDialog()
        }
    }

    private fun showAbacusSoundPickerDialog() {
        val ctx = requireContext()
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_abacus_sound_picker)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (ctx.resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)

        val rows = listOf(
            dialog.findViewById<View>(R.id.rowAbacusSound1),
            dialog.findViewById<View>(R.id.rowAbacusSound2),
            dialog.findViewById<View>(R.id.rowAbacusSound3),
            dialog.findViewById<View>(R.id.rowAbacusSound4),
            dialog.findViewById<View>(R.id.rowAbacusSound5)
        )
        val checks = listOf(
            dialog.findViewById<AppCompatCheckBox>(R.id.checkAbacusSound1),
            dialog.findViewById<AppCompatCheckBox>(R.id.checkAbacusSound2),
            dialog.findViewById<AppCompatCheckBox>(R.id.checkAbacusSound3),
            dialog.findViewById<AppCompatCheckBox>(R.id.checkAbacusSound4),
            dialog.findViewById<AppCompatCheckBox>(R.id.checkAbacusSound5)
        )

        fun refreshDialogVisuals() {
            rows.forEachIndexed { index, row -> row.isSelected = (selectedAbacusSound == index + 1) }
            checks.forEachIndexed { index, checkBox -> checkBox.isChecked = (selectedAbacusSound == index + 1) }
        }
        refreshDialogVisuals()

        rows.forEachIndexed { index, row ->
            row.setOnClickListener {
                val soundNumber = index + 1
                selectedAbacusSound = soundNumber
                AbacusSoundPlayer.choose(ctx, soundNumber)
                refreshDialogVisuals()
                textAbacusSoundCurrent.text = "Ses $soundNumber"
            }
        }

        dialog.findViewById<View>(R.id.abacusSoundPickerClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun closeFragment() {
        if (arguments?.getBoolean("fromAccountSettings") == true) {
            parentFragmentManager.popBackStack()
            return
        }
        val main = activity as? MainActivity
        if (main != null) {
            main.finishTasksOverlayAnimated("SoundSettingsFragment.close")
        } else {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_right)
                .remove(this)
                .commit()
        }
    }
}
