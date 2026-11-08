package com.example.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Haftalık kartın PAYLAŞIMI: kartın özet görselini + davet bağlantısını sistem paylaşım penceresiyle
 * gönderir (WhatsApp, Instagram, …). Bkz. docs/BUYUME_DONGUSU_ARASTIRMASI.md döngü 2.
 *
 * Görselde kişisel veri yok: yalnızca kartın kendisi (adın ilk kelimesi; soyad, okul, fotoğraf yok)
 * ve altında "Sorobit". Bağlantı metinde: `https://numigo-new.web.app/d/<kod>` (sunucu `getReferralCode`).
 * Kod alınamazsa (çevrimdışı, eski sunucu) mağaza bağlantısıyla paylaşılır: paylaşım yine olsun,
 * yalnızca atıf kaybolur.
 *
 * Hangi uygulamanın seçildiği [ChosenReceiver] ile (EXTRA_CHOSEN_COMPONENT) GA4'e gidiyor; kullanıcı
 * pencereyi kapatırsa hiçbir şey gelmez.
 */
object WeeklyCardShare {

    private const val TAG = "WeeklyCardShare"
    const val LINK_BASE = "https://numigo-new.web.app/d/"
    const val STORE_URL = "https://play.google.com/store/apps/details?id=com.numigo.app"

/**
     * Davet bilgisi: bağlantı ve sayaç (davet ekranı "Davet ettiklerin: 2 · kazandığın: 14 gün Pro").
     * [source] bağlantıya yazılıyor (?k=invite), sayfa onu Play referrer'ına utm_medium olarak koyuyor;
     * haftalık kart için parametresiz (eski bağlantılarla aynı).
     */
    data class Invite(val link: String, val invited: Int, val earnedDays: Int, val hasCode: Boolean)

    suspend fun fetchInvite(context: Context, source: String = "weekly"): Invite = runCatching {
        val r = FirebaseFunctions.getInstance().getHttpsCallable("getReferralCode")
            .call(mapOf("device" to ReferralAttribution.deviceFingerprint(context))).await()
        val m = r.getData() as? Map<*, *>
        val code = m?.get("code") as? String
        if (code.isNullOrBlank()) {
            Invite(STORE_URL, 0, 0, false)
        } else {
            Invite(
                LINK_BASE + code + if (source == "weekly") "" else "?k=$source",
                (m["invited"] as? Number)?.toInt() ?: 0,
                (m["earnedDays"] as? Number)?.toInt() ?: 0,
                true,
            )
        }
    }.onFailure { Log.w(TAG, "davet kodu alınamadı", it) }.getOrDefault(Invite(STORE_URL, 0, 0, false))

    suspend fun fetchLink(context: Context): String = fetchInvite(context).link

    /** Kartın görünüşü + kenar boşluğu + altta marka satırı. */
    fun renderBitmap(context: Context, card: View): Bitmap {
        val d = context.resources.displayMetrics.density
        val pad = (20 * d).toInt()
        val footer = (56 * d).toInt()
        val w = card.width + pad * 2
        val h = card.height + pad * 2 + footer
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(ContextCompat.getColor(context, R.color.background_color))
        c.save()
        c.translate(pad.toFloat(), pad.toFloat())
        card.draw(c)
        c.restore()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8CC63F.toInt()
            textSize = 22 * d
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        c.drawText("Sorobit", w / 2f, (card.height + pad * 2 + footer * 0.55f), p)
        return bmp
    }

    fun share(context: Context, bitmap: Bitmap, text: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "haftalik.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            // Bazı uygulamalar izni yalnızca ClipData'dan alıyor.
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val chosen = PendingIntent.getBroadcast(context, 0, Intent(context, ChosenReceiver::class.java), flags)
        context.startActivity(Intent.createChooser(send, appString(R.string.weekly_share_chooser), chosen.intentSender))
    }

    /** GA4 olay adı: kırılım adda (olay kapsamlı boyut kotası dolu). */
    fun eventFor(packageName: String?): String {
        val pkg = packageName.orEmpty()
        return when {
            pkg.startsWith("com.whatsapp") -> "share_done_whatsapp"
            pkg.startsWith("com.instagram") -> "share_done_instagram"
            else -> "share_done_other"
        }
    }

    class ChosenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            val comp = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
            }
            Log.d(TAG, "seçilen: $comp")
            AnalyticsLogger.logSimpleEvent(eventFor(comp?.packageName))
        }
    }
}
