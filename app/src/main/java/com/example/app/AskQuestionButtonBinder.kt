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

        val uid = currentUser.uid
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!fragment.isAdded) return@addOnSuccessListener
                applyVisible(button, onVisibleChanged, onReadyForBounce)
                bindClick(fragment, button, uid, doc, isTeacher, onAllowedClick)
            }
            .addOnFailureListener {
                if (!fragment.isAdded) return@addOnFailureListener
                applyVisible(button, onVisibleChanged, onReadyForBounce)
                bindClick(fragment, button, uid, cachedDoc = null, isTeacher = isTeacher, onAllowedClick = onAllowedClick)
            }
    }

    /**
     * Tıklamayı bağlar ve kullanıcı dokümanını **tıklama anında** yeniden okur.
     *
     * ## Neden bağlanma anındaki kopya yetmiyor
     * [bind] fragment'ın onViewCreated'ında çalışıyor, mağaza ise `.add()` ile açılıyor — yani
     * harita altta canlı kalıyor ve geri dönüşte onViewCreated bir daha çalışmıyor. Doküman
     * tıklama kapanışında donsaydı şu olurdu: çocuk krediyi satın alır, geri döner, "öğretmene
     * sor"a basar ve donmuş kopya hâlâ "kredin yok" der. Parasını ödemiş kullanıcıya satın alma
     * tanıtımını yeniden açmak.
     *
     * Aynı bayatlık iki yerde daha yanlış karar ürettiriyordu: Free → Pro geçişinin hemen
     * ardından (kullanıcı Pro olduğu hâlde Pro tanıtımı görüyordu) ve oturum ortasında
     * kısıtlanan hesapta.
     *
     * ## Bedeli ve yedeği
     * Tıklama başına bir Firestore okuması ve onun gecikmesi. Buton nadir tıklandığı için yük
     * önemsiz. Okuma başarısız olursa [cachedDoc] kullanılır; o da yoksa eski davranışa
     * düşülüp kullanıcı akışa bırakılır — yani hiçbir durumda bugünkünden kötüye gitmiyor.
     */
    private fun bindClick(
        fragment: Fragment,
        button: View,
        uid: String,
        cachedDoc: DocumentSnapshot?,
        isTeacher: Boolean,
        onAllowedClick: () -> Unit,
    ) {
        var refreshing = false
        button.setOnClickListener {
            SessionDeviceManager.requireLoggedInAndSingleDevice(fragment) {
                // Okuma sürerken ikinci dokunuş iki pencere açardı.
                if (refreshing) return@requireLoggedInAndSingleDevice
                refreshing = true
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnCompleteListener { task ->
                        refreshing = false
                        if (!fragment.isAdded) return@addOnCompleteListener
                        val fresh = if (task.isSuccessful) task.result else null
                        val doc = fresh?.takeIf { it.exists() } ?: cachedDoc
                        if (doc == null) {
                            val main = fragment.activity as? MainActivity
                            if (main?.isQuestionRecordingInProgress() == true) {
                                return@addOnCompleteListener
                            }
                            onAllowedClick()
                            return@addOnCompleteListener
                        }
                        handleClick(fragment, doc, isTeacher, onAllowedClick)
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
                // Pro üyesine de aynı ekran açılıyor, yalnızca kredi satın alma düzeniyle.
                // Eskiden düz bir AlertDialog'du: tasarımsızdı ve hiçbir yere kaydedilmiyordu,
                // yani kaç Pro üyesinin kredisiz kaldığı hiç görülmüyordu.
                AskQuestionOpenFragment
                    .newInstance(AnalyticsLogger.PROMO_TRIGGER_PRO_OUT_OF_CREDITS)
                    .show(fragment.requireActivity().supportFragmentManager, "AskQuestionOpen")
            }
            !isTeacher && credits < 1 -> {
                AskQuestionOpenFragment
                    .newInstance(AnalyticsLogger.PROMO_TRIGGER_OUT_OF_CREDITS)
                    .show(fragment.requireActivity().supportFragmentManager, "AskQuestionOpen")
            }
            else -> {
                val main = fragment.activity as? MainActivity
                if (main?.isQuestionRecordingInProgress() == true) return
                onAllowedClick()
            }
        }
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
