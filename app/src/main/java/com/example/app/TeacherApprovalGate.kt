package com.example.app

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth

/**
 * Onaylanmamış öğretmen hesabının uygulamadaki tek uyarı penceresi.
 *
 * Bu hesap hiçbir şey yapamıyor; nereye dokunursa dokunsun (öğretmene sor, ders başlat)
 * aynı mesajı görmeli. Pencere iki ayrı yerde elle kurulsaydı birine destek butonu
 * eklenip öbürü unutulurdu — kullanıcının bu durumda yapabileceği tek şey de destekle
 * yazışmak.
 *
 * "Onaysız öğretmen mi" kararı burada değil, [MainActivity.isUnapprovedTeacher]'da.
 */
object TeacherApprovalGate {

    /**
     * Onaysız öğretmense uyarıyı gösterir ve true döner; çağıran işi orada bırakır.
     * [context] MainActivity değilse (ör. giriş ekranları) kapı hiçbir şeyi engellemez.
     */
    fun blockIfUnapproved(context: Context?): Boolean {
        val main = context as? MainActivity ?: return false
        if (!main.isUnapprovedTeacher()) return false
        showNotApprovedDialog(main)
        return true
    }

    /**
     * Mağaza ve satın alma kapısı: bütün öğretmen hesaplarında kapalı.
     *
     * Onaysız öğretmen hiçbir şey yapamıyor; onaylı öğretmenin altını, anahtarı ve canı
     * sınırsız. İkisinin de satın alacağı bir şey yok — bir satın alma ancak yanlışlıkla
     * olur ve iade talebiyle biter. Uyarılar farklı: onaysıza onay ve destek, onaylıya
     * neden gerek olmadığı.
     */
    fun blockPurchasesForTeacher(context: Context?): Boolean {
        val main = context as? MainActivity ?: return false
        if (blockIfUnapproved(main)) return true
        if (!main.isApprovedTeacher()) return false
        AlertDialog.Builder(main)
            .setMessage(R.string.teacher_shop_unlimited)
            .setPositiveButton(R.string.ask_question_alert_ok, null)
            .show()
        return true
    }

    fun showNotApprovedDialog(context: Context) {
        AlertDialog.Builder(context)
            .setMessage(R.string.ask_question_teacher_not_approved)
            .setPositiveButton(R.string.ask_question_alert_ok, null)
            .setNegativeButton(R.string.teacher_not_approved_contact_support) { _, _ ->
                // Kullanıcı ID'si gövdeye yazılıyor: destek hangi hesabı onaylayacağını
                // sormak zorunda kalmasın. E-posta adresi Firestore'da aranabiliyor ama
                // öğretmen başka bir adresten yazabilir.
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                SupportContactHelper.openSupportEmail(
                    context,
                    subject = context.getString(R.string.teacher_not_approved_support_subject),
                    body = if (uid != null) "Merhaba,\n\n\n\nKullanıcı ID: $uid\n" else null,
                )
            }
            .show()
    }
}
