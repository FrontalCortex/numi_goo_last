package com.example.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.Fragment

/** Destek e-postasını açan ortak yardımcı: mailto başarısız olursa Gmail web'e düşer. */
object SupportContactHelper {
    private const val SUPPORT_EMAIL = "sorobit.support@gmail.com"

    fun openSupportEmail(fragment: Fragment, subject: String? = null, body: String? = null) {
        openSupportEmail(fragment.requireContext(), subject, body)
    }

    /** Fragment'ı olmayan yerler için (ör. LessonAdapter'ın ders paneli). */
    fun openSupportEmail(context: Context, subject: String? = null, body: String? = null) {
        val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL")
            if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            if (!body.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val gmailWebUrl = buildString {
            append("https://mail.google.com/mail/?view=cm&to=$SUPPORT_EMAIL")
            if (!subject.isNullOrBlank()) append("&su=${Uri.encode(subject)}")
            if (!body.isNullOrBlank()) append("&body=${Uri.encode(body)}")
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(gmailWebUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mailtoIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent.createChooser(browserIntent, null))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "E-posta veya tarayıcı açılamadı.", Toast.LENGTH_SHORT).show()
            }
        } catch (_: SecurityException) {
            try {
                context.startActivity(Intent.createChooser(browserIntent, null))
            } catch (_: Exception) {
                Toast.makeText(context, "E-posta veya tarayıcı açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
