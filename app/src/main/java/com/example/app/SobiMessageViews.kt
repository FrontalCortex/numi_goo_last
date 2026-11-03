package com.example.app

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Sobi sohbet balonları ve altlarındaki beğen / beğenme / bildir (Sobi ekranı, [MistakeReviewDialog]):
 * analizde ve öğreticide aynı görünsün diye tek yerde.
 *
 * Oy iki türlü mesaja verilebiliyor:
 *  - Sunucudan gelen cevap ([SobiStepTracker.Message.serverIndex]): sohbet kaydındaki satıra bağlı.
 *  - Kural motorunun yazdığı mesaj ([SobiStepTracker.Message.localId]): sohbet kaydı yok, metin
 *    oyla birlikte gider (functions/sobi/feedback.js, "YEREL MESAJ"). Analiz ekranındaki adım
 *    talimatları ve hata teşhisi böyle: hangi anlatımın çocuğa zor geldiğini görmek için.
 * İkisi de yoksa (hata metni, "Bildirdin, teşekkürler!") balon oy satırsız.
 *
 * Görünüm durumu tutmuyor: oy, yorum ve bildirim mesajın kendisinde; değişince [onChanged] ile
 * barındıran ekran yeniden çizer.
 */
class SobiMessageViews(
    private val fragment: Fragment,
    private val sessionId: () -> String?,
    /** Yerel mesajın hangi ekranda ve hangi soruda yazıldığı (haftalık rapor için). */
    private val localScreen: String = "sohbet",
    private val localQuestion: () -> String? = { null },
    private val onChanged: () -> Unit,
    /** Çocuk bir mesajı bildirdi: ekran "Bildirdin, teşekkürler!" balonunu kendi listesine ekler. */
    private val onReported: () -> Unit,
    /** 🔊: Sobi'nin mesajını sesli okur ([SobiSpeaker]); null ya da ses yoksa düğme yok. */
    private val onSpeak: ((String) -> Unit)? = null,
    /** Ses motoru hazır mı (Türkçe ses yoksa 🔊 gösterilmez). */
    private val canSpeak: () -> Boolean = { true },
) {

    fun bubble(ctx: Context, m: SobiStepTracker.Message): View {
        val tv = TextView(ctx).apply {
            text = RtlSupport.isolateEquations(m.text)
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(0f, 1.15f)
            background = ContextCompat.getDrawable(ctx, if (m.fromChild) R.drawable.chat_bubble_me else R.drawable.chat_bubble_sobi)
            val p = dp(ctx, 12)
            setPadding(p, dp(ctx, 9), p, dp(ctx, 9))
            maxWidth = (ctx.resources.displayMetrics.widthPixels * 0.74f).toInt()
        }
        val row = LinearLayout(ctx).apply {
            gravity = if (m.fromChild) Gravity.END else Gravity.START
            setPadding(0, dp(ctx, 4), 0, dp(ctx, 4))
            addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        val speak = onSpeak?.takeIf { canSpeak() }
        if (m.fromChild || (!canRate(m) && speak == null)) return row
        // Oy verilince yanında "Bize daha fazla bilgi ver": nedenini öğrenmek için.
        val rating = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPaddingRelative(dp(ctx, 6), 0, 0, 0)
            if (speak != null) addView(speakButton(ctx, m, speak))
            if (canRate(m)) {
                addView(rateButton(ctx, m, 1, "👍", R.string.sobi_rate_up))
                addView(rateButton(ctx, m, -1, "👎", R.string.sobi_rate_down))
                addView(reportButton(ctx, m))
                if (m.rating != 0) addView(moreInfoLink(ctx, m))
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(row)
            addView(rating)
        }
    }

    private fun speakButton(ctx: Context, m: SobiStepTracker.Message, speak: (String) -> Unit): View =
        TextView(ctx).apply {
            text = "🔊"
            contentDescription = appString(R.string.sobi_speak)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2))
            setOnClickListener { speak(m.text) }
        }

    private fun canRate(m: SobiStepTracker.Message): Boolean =
        m.localId != null || (m.serverIndex != null && sessionId() != null)

    /** Aynı düğmeye yeniden dokunmak oyu geri alır. */
    private fun rate(m: SobiStepTracker.Message, value: Int) {
        if (!canRate(m)) return
        m.rating = if (m.rating == value) 0 else value
        send(m, value = m.rating)
        onChanged()
    }

    private fun send(m: SobiStepTracker.Message, value: Int? = null, report: Boolean = false, comment: String? = null) {
        val localId = m.localId
        if (localId != null) {
            ServerSobi.sendLocalFeedback(localId, m.text, value, report, comment, localScreen, localQuestion())
            return
        }
        val id = sessionId() ?: return
        val index = m.serverIndex ?: return
        if (report) ServerSobi.sendReport(id, index, comment)
        else if (value != null) ServerSobi.sendFeedback(id, index, value, comment)
    }

    private fun reportButton(ctx: Context, m: SobiStepTracker.Message): View =
        TextView(ctx).apply {
            text = "🚩"
            contentDescription = appString(R.string.sobi_report)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            alpha = if (m.reported) 1f else 0.45f
            setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2))
            if (!m.reported) setOnClickListener { askReport(m) }
        }

    private fun moreInfoLink(ctx: Context, m: SobiStepTracker.Message): View =
        TextView(ctx).apply {
            text = appString(if (m.commented) R.string.sobi_feedback_thanks else R.string.sobi_feedback_more)
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            if (!m.commented) paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            alpha = 0.8f
            setPaddingRelative(dp(ctx, 6), dp(ctx, 2), dp(ctx, 4), dp(ctx, 2))
            if (!m.commented) setOnClickListener { askComment(m) }
        }

    private fun rateButton(ctx: Context, m: SobiStepTracker.Message, value: Int, glyph: String, label: Int): View =
        TextView(ctx).apply {
            text = glyph
            contentDescription = appString(label)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            // Seçili oy tam görünür, diğeri soluk; hiç oy yoksa ikisi de yarı soluk.
            alpha = when (m.rating) { value -> 1f; 0 -> 0.55f; else -> 0.25f }
            setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2))
            setOnClickListener { rate(m, value) }
        }

    /** Kısa yorum kutusu; [onSend] kutudaki metinle (boş olabilir) çağrılır. */
    private fun textDialog(title: String, message: String?, positive: String, onSend: (String) -> Unit) {
        val ctx = fragment.context ?: return
        val box = EditText(ctx).apply {
            hint = appString(R.string.sobi_feedback_hint)
            maxLines = 4
            filters = arrayOf(android.text.InputFilter.LengthFilter(300))
        }
        val wrap = LinearLayout(ctx).apply {
            setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), 0)
            addView(box, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(title)
            .apply { if (message != null) setMessage(message) }
            .setView(wrap)
            .setPositiveButton(positive) { _, _ -> onSend(box.text?.toString()?.trim().orEmpty()) }
            .setNegativeButton(appString(R.string.sobi_feedback_cancel), null)
            .show()
    }

    private fun askComment(m: SobiStepTracker.Message) {
        textDialog(appString(R.string.sobi_feedback_title), null, appString(R.string.sobi_feedback_send)) { text ->
            if (text.isEmpty() || m.rating == 0) return@textDialog
            send(m, value = m.rating, comment = text)
            m.commented = true
            onChanged()
        }
    }

    /** Bildirmek için yorum şart değil: boş da gönderilebilir. */
    private fun askReport(m: SobiStepTracker.Message) {
        textDialog(appString(R.string.sobi_report), appString(R.string.sobi_report_message), appString(R.string.sobi_report_send)) { text ->
            send(m, report = true, comment = text)
            m.reported = true
            onReported()
            onChanged()
        }
    }

    private fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    companion object {
        /** Yerel mesaj kimliği: sunucunun kabul ettiği biçimde (8–64 harf/rakam). */
        fun newLocalId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(20)
    }
}
