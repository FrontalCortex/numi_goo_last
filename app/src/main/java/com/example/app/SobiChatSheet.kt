package com.example.app

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * "Sobi'ye sor" sohbet paneli (Faz 4 ilk sürüm; docs/ANLAMADIM_YARDIM_PLANI.md).
 *
 * Panel durum tutmuyor: mesajlar, oturum ve öneri balonları [SobiStepTracker]'da. Böylece çocuk
 * "Abaküste izle"ye basıp paneli kapattığında ve sonra "Sobi" düğmesiyle yeniden açtığında sohbet
 * kaldığı yerden sürüyor.
 *
 * Abaküste gösterim panel KAPANARAK yapılıyor: panel ekranın alt yarısını kapatıyor, çocuk boncukları
 * görmeli. Oynatmayı [onShow] üzerinden barındıran fragment yapıyor.
 *
 * Henüz yok (sonraki sürüm): sesli soru ve sesli cevap, "bildir" düğmesi.
 */
class SobiChatSheet(
    private val fragment: Fragment,
    private val tracker: SobiStepTracker,
    /** Gönderim anındaki adım bağlamı; null ise bu adımda Sobi'ye sorulamaz. */
    private val contextProvider: () -> SobiContext?,
    private val onShow: (ServerSobi.Action) -> Unit,
    /** Panel kapandı (ekrandaki "Sobi" düğmesi görünsün). */
    private val onClosed: () -> Unit = {},
    /** Soru adımı mı (abaküslü soru); değilse anlatım adımı: ilk öneri balonları buna göre. */
    private val isQuestionStep: () -> Boolean = { true },
) {

    private var dialog: BottomSheetDialog? = null
    private var busy = false

    private lateinit var scroll: ScrollView
    private lateinit var messagesView: LinearLayout
    private lateinit var thinking: TextView
    private lateinit var watch: Button
    private lateinit var suggestionsView: LinearLayout
    private lateinit var input: EditText

    val isShowing: Boolean get() = dialog?.isShowing == true

    fun show() {
        if (isShowing || !fragment.isAdded) return
        val ctx = fragment.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.sheet_sobi_chat, null)
        scroll = view.findViewById(R.id.sobiScroll)
        messagesView = view.findViewById(R.id.sobiMessages)
        thinking = view.findViewById(R.id.sobiThinking)
        watch = view.findViewById(R.id.sobiWatch)
        suggestionsView = view.findViewById(R.id.sobiSuggestions)
        input = view.findViewById(R.id.sobiInput)

        // Mesaj alanı ekranın üçte biri kadar; panelin geri kalanı balonlar ve yazma kutusu.
        scroll.layoutParams = scroll.layoutParams.apply {
            height = (ctx.resources.displayMetrics.heightPixels * 0.34f).toInt()
        }

        view.findViewById<View>(R.id.sobiClose).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.sobiSend).setOnClickListener { sendFromInput() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendFromInput(); true } else false
        }
        watch.setOnClickListener {
            val action = tracker.lastShowAction ?: return@setOnClickListener
            dismiss()
            onShow(action)
        }

        if (tracker.messages.isEmpty()) {
            tracker.messages += SobiStepTracker.Message(fromChild = false, text = appString(R.string.sobi_greeting))
        }
        // Sohbet öğretici boyunca sürüyor; balon yoksa (yeni sohbet ya da son cevap hata) bu
        // adımın türüne göre başlangıç balonları.
        if (tracker.suggestions.isEmpty()) {
            tracker.suggestions = if (isQuestionStep()) INITIAL_QUESTION_SUGGESTIONS else INITIAL_EXPLANATION_SUGGESTIONS
        }
        renderMessages()
        renderSuggestions()
        watch.visibility = View.GONE

        val d = BottomSheetDialog(ctx)
        d.setContentView(view)
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.setOnShowListener {
            // Malzeme kütüphanesinin beyaz kabı görünmesin; köşeler panelin kendi arka planından.
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        d.setOnDismissListener {
            if (dialog === d) dialog = null
            onClosed()
        }
        dialog = d
        d.show()
    }

    fun dismiss() {
        hideKeyboard()
        dialog?.dismiss()
        dialog = null
    }

    // ── Gönderim ────────────────────────────────────────────────────────────

    private fun sendFromInput() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        input.setText("")
        send(text)
    }

    private fun send(text: String) {
        if (busy) return
        val context = contextProvider() ?: return
        busy = true
        tracker.messages += SobiStepTracker.Message(fromChild = true, text = text)
        tracker.suggestions = emptyList()
        watch.visibility = View.GONE
        renderMessages()
        renderSuggestions()
        thinking.visibility = View.VISIBLE

        ServerSobi.send(
            sessionId = tracker.sessionId,
            message = text,
            context = context,
            onResult = { reply ->
                busy = false
                tracker.sessionId = reply.sessionId
                val shown = when {
                    reply.turnLimitReached -> appString(R.string.sobi_turn_limit)
                    else -> reply.text?.takeIf { it.isNotBlank() }
                }
                shown?.let { tracker.messages += SobiStepTracker.Message(fromChild = false, text = it) }
                val showAction = reply.actions.lastOrNull { it.kind == ServerSobi.ACTION_SHOW }
                if (showAction != null) tracker.lastShowAction = showAction
                tracker.suggestions = if (reply.turnLimitReached) emptyList() else reply.suggestions
                if (!isShowing) return@send
                thinking.visibility = View.GONE
                renderMessages()
                renderSuggestions()
                watch.visibility = if (showAction != null) View.VISIBLE else View.GONE
            },
            onFailure = { e ->
                busy = false
                val msg = when {
                    ServerSobi.isDailyLimit(e) -> appString(R.string.sobi_error_daily_limit)
                    ServerSobi.isRestricted(e) -> appString(R.string.sobi_error_restricted)
                    else -> appString(R.string.sobi_error_network)
                }
                tracker.messages += SobiStepTracker.Message(fromChild = false, text = msg)
                if (!isShowing) return@send
                thinking.visibility = View.GONE
                renderMessages()
                renderSuggestions()
            },
        )
    }

    private fun onSuggestion(key: String) {
        // "Bir daha göster" sunucuya gitmiyor: son gösterim yerelde yeniden oynatılır.
        if (key == KEY_SHOW_AGAIN) {
            val action = tracker.lastShowAction
            if (action != null) {
                dismiss()
                onShow(action)
                return
            }
        }
        send(labelOf(key) ?: return)
    }

    // ── Görünüm ─────────────────────────────────────────────────────────────

    private fun renderMessages() {
        val ctx = fragment.context ?: return
        messagesView.removeAllViews()
        for (m in tracker.messages) messagesView.addView(bubble(ctx, m))
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun bubble(ctx: Context, m: SobiStepTracker.Message): View {
        val tv = TextView(ctx).apply {
            text = m.text
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(0f, 1.15f)
            background = ContextCompat.getDrawable(ctx, if (m.fromChild) R.drawable.chat_bubble_me else R.drawable.chat_bubble_sobi)
            val p = dp(ctx, 12)
            setPadding(p, dp(ctx, 9), p, dp(ctx, 9))
            maxWidth = (ctx.resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        return LinearLayout(ctx).apply {
            gravity = if (m.fromChild) Gravity.END else Gravity.START
            setPadding(0, dp(ctx, 4), 0, dp(ctx, 4))
            addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun renderSuggestions() {
        val ctx = fragment.context ?: return
        suggestionsView.removeAllViews()
        for (key in tracker.suggestions) {
            val label = labelOf(key) ?: continue
            val chip = TextView(ctx).apply {
                text = label
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_sobi_pill)
                setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8))
                setOnClickListener { onSuggestion(key) }
            }
            suggestionsView.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = dp(ctx, 8) })
        }
    }

    private fun hideKeyboard() {
        if (!::input.isInitialized) return
        val imm = input.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    companion object {
        private const val KEY_DONT_KNOW = "ne_yapacagim"
        private const val KEY_WHY = "neden_boyle"
        private const val KEY_SHOW = "bana_goster"
        private const val KEY_SHOW_AGAIN = "bir_daha_goster"
        private const val KEY_GOT_IT = "anladim"
        private const val KEY_DIDNT_GET = "bunu_anlamadim"
        private const val KEY_EXAMPLE = "ornek_ver"

        /** Sohbet açılırken, çocuk henüz bir şey yazmadan: soru adımında. */
        private val INITIAL_QUESTION_SUGGESTIONS = listOf(KEY_DONT_KNOW, KEY_WHY, KEY_SHOW)
        /** Anlatım adımında: gösterim yok, örnek istemek anlamlı. */
        private val INITIAL_EXPLANATION_SUGGESTIONS = listOf(KEY_DIDNT_GET, KEY_EXAMPLE, KEY_WHY)

        /** Sunucunun anahtarları (chat.suggestionsFor) ve yerel olanlar → çocuğun gördüğü ve gönderilen metin. */
        private fun labelOf(key: String): String? = when (key) {
            KEY_DONT_KNOW -> appString(R.string.sobi_suggest_dont_know)
            KEY_WHY -> appString(R.string.sobi_suggest_why)
            KEY_SHOW -> appString(R.string.sobi_suggest_show)
            KEY_SHOW_AGAIN -> appString(R.string.sobi_suggest_show_again)
            KEY_GOT_IT -> appString(R.string.sobi_suggest_got_it)
            KEY_DIDNT_GET -> appString(R.string.sobi_suggest_didnt_get)
            KEY_EXAMPLE -> appString(R.string.sobi_suggest_example)
            else -> null
        }
    }
}
