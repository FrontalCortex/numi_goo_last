package com.example.app

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Harita, tutorial, ders abaküsü ve practice için ortak askQuestion butonu:
 * ban/onay durumunda buton görünür kalır; tıklamada uyarı veya soru akışı.
 */
object AskQuestionButtonBinder {

    fun bind(
        fragment: Fragment,
        button: View,
        isTeacher: Boolean,
        onAllowedClick: () -> Unit,
        onVisibleChanged: ((visible: Boolean) -> Unit)? = null,
        onReadyForBounce: (() -> Unit)? = null,
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            button.visibility = View.GONE
            onVisibleChanged?.invoke(false)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!fragment.isAdded) return@addOnSuccessListener
                applyVisible(button, onVisibleChanged, onReadyForBounce)
                button.setOnClickListener {
                    SessionDeviceManager.requireLoggedInAndSingleDevice(fragment) {
                        handleClick(fragment, doc, isTeacher, onAllowedClick)
                    }
                }
            }
            .addOnFailureListener {
                if (!fragment.isAdded) return@addOnFailureListener
                applyVisible(button, onVisibleChanged, onReadyForBounce)
                button.setOnClickListener {
                    SessionDeviceManager.requireLoggedInAndSingleDevice(fragment) {
                        val main = fragment.activity as? MainActivity
                        if (main?.isQuestionRecordingInProgress() == true) return@requireLoggedInAndSingleDevice
                        onAllowedClick()
                    }
                }
            }
    }

    private fun applyVisible(
        button: View,
        onVisibleChanged: ((Boolean) -> Unit)?,
        onReadyForBounce: (() -> Unit)?,
    ) {
        button.visibility = View.VISIBLE
        onVisibleChanged?.invoke(true)
        onReadyForBounce?.invoke()
    }

    private fun handleClick(
        fragment: Fragment,
        doc: DocumentSnapshot,
        isTeacher: Boolean,
        onAllowedClick: () -> Unit,
    ) {
        // Soru sorma hakkı artık plana değil, danışma kredisine bağlı: kredisi olan
        // herkes (Free dahil) soru sorabilir. Kredi bakiyesi sunucuya ait bir alandır ve
        // firestore.rules ile istemci yazımına kapalıdır; buradaki kontrol yalnızca arayüz
        // içindir, gerçek kontrolü askTeacherQuestion yapar.
        val credits = doc.getLong("questionCredits")?.toInt() ?: 0

        // Zaten Pro olan bir kullanıcıya Pro yükseltme ekranı göstermek yanlış olur; ona
        // eksik olan şey plan değil kredi. Süresi geçmiş abonelik Free sayılır (sunucudaki
        // effectivePlan ile aynı kural).
        val hasProPlan = PlanStatus.isPro(doc)

        when {
            UserAskQuestionRestriction.isRestricted(doc) -> {
                showRestrictedDialog(fragment, doc)
            }
            isTeacher && doc.getBoolean("teacherApproved") != true -> {
                showMessage(fragment, R.string.ask_question_teacher_not_approved)
            }
            !isTeacher && credits < 1 && hasProPlan -> {
                showOutOfCreditsDialog(fragment)
            }
            !isTeacher && credits < 1 -> {
                AskQuestionOpenFragment().show(fragment.requireActivity().supportFragmentManager, "AskQuestionOpen")
            }
            else -> {
                val main = fragment.activity as? MainActivity
                if (main?.isQuestionRecordingInProgress() == true) return
                onAllowedClick()
            }
        }
    }

    /**
     * Kredisi biten Pro üyesine gösterilir.
     *
     * Doğrudan mağazaya atmak yerine tek cümlelik bir açıklama veriliyor: kullanıcının
     * niyeti soru sormaktı, açıklamasız bir sıçrama hata gibi görünür ve mağazanın hangi
     * bölümüne bakması gerektiğini de bilemez.
     */
    private fun showOutOfCreditsDialog(fragment: Fragment) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Danışma kredin kalmadı")
            .setMessage(
                "Öğretmene soru sormak için krediye ihtiyacın var. " +
                    "Pro üyesi olduğun için kredi paketlerinde bonus kredi kazanıyorsun."
            )
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Mağazaya git") { _, _ ->
                (fragment.activity as? MainActivity)?.openShopFragment()
            }
            .show()
    }

    private fun showMessage(fragment: Fragment, messageResId: Int) {
        AlertDialog.Builder(fragment.requireContext())
            .setMessage(messageResId)
            .setPositiveButton(R.string.ask_question_alert_ok, null)
            .show()
    }

    private fun showRestrictedDialog(fragment: Fragment, doc: DocumentSnapshot) {
        val banned = doc.getBoolean("banned") == true
        val restrictedUntil = doc.getTimestamp("restrictedUntil")
        val message = when {
            banned -> "Hesabınız kural ihlali nedeniyle kalıcı olarak kısıtlanmıştır."
            restrictedUntil != null -> {
                val dateText = android.text.format.DateFormat.format("d MMM yyyy, HH:mm", restrictedUntil.toDate())
                "Hesabınız kural ihlali nedeniyle $dateText tarihine kadar kısıtlanmıştır."
            }
            else -> fragment.getString(R.string.ask_question_account_restricted)
        }
        AlertDialog.Builder(fragment.requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ask_question_alert_ok, null)
            .setNegativeButton("İtiraz Et") { _, _ ->
                SupportContactHelper.openSupportEmail(
                    fragment,
                    subject = "Hesap kısıtlaması itirazı",
                    body = "Merhaba,\n\nHesabımın kısıtlanmasına itiraz etmek istiyorum.\n\nKullanıcı ID: ${doc.id}\n\n"
                )
            }
            .show()
    }
}
