package com.example.app

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

/**
 * Veli panelinin önündeki yetişkin kapısı.
 *
 * ## Neden çarpma/toplama sorusu değil
 * Bu uygulamayı kullanan çocuk zaten zihinden çarpma yapıyor — "7 × 8" kapı değil, davet olurdu.
 * Bunun yerine engel **okuma akıcılığı**: sayı Türkçe yazıyla veriliyor ("dört yüz yirmi sekiz"),
 * rakamla yazılması isteniyor. Yetişkin için bir saniye, 7 yaşındaki için gerçek bir eşik.
 *
 * Bu mutlak bir güvenlik sınırı değildir ve öyle olduğu iddia edilmiyor: panelde yalnızca
 * çocuğun kendi ilerleme verisi var, sır yok. Amaç "burası sana göre değil" sürtünmesi.
 */
object ParentGate {

    private val ONES = arrayOf("", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
    private val TENS = arrayOf(
        "", "on", "yirmi", "otuz", "kırk", "elli", "altmış", "yetmiş", "seksen", "doksan",
    )

    /**
     * Kapıyı gösterir; yalnızca doğru cevapta [onPass] çağrılır. Vazgeçilirse hiçbir şey olmaz.
     */
    fun show(context: Context, onPass: () -> Unit) {
        val view = View.inflate(context, R.layout.dialog_parent_gate, null)
        val dialog = AlertDialog.Builder(context).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val challengeText = view.findViewById<TextView>(R.id.parentGateChallenge)
        val input = view.findViewById<EditText>(R.id.parentGateInput)
        val error = view.findViewById<TextView>(R.id.parentGateError)
        val confirm = view.findViewById<MaterialButton>(R.id.parentGateConfirm)
        val cancel = view.findViewById<MaterialButton>(R.id.parentGateCancel)

        var answer = newChallenge()
        challengeText.text = spellTurkish(answer)

        confirm.setOnClickListener {
            if (input.text.toString().trim().toIntOrNull() == answer) {
                dialog.dismiss()
                onPass()
                return@setOnClickListener
            }
            // Yanlışta yeni sayı: aynı soruyu deneme yanılmayla geçmek kapıyı anlamsız kılardı.
            error.visibility = View.VISIBLE
            input.setText("")
            answer = newChallenge()
            challengeText.text = spellTurkish(answer)
        }
        cancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /** Basamakları 1-9 olan üç basamaklı sayı: "sıfır"/"yüz" gibi kısa okumalar elenir. */
    private fun newChallenge(): Int =
        Random.nextInt(1, 10) * 100 + Random.nextInt(1, 10) * 10 + Random.nextInt(1, 10)

    /** 111..999 aralığı için yeterli. Yüzler basamağı 1 ise "bir yüz" değil sadece "yüz". */
    internal fun spellTurkish(value: Int): String {
        val hundreds = value / 100
        val tens = (value / 10) % 10
        val ones = value % 10
        val parts = mutableListOf<String>()
        when {
            hundreds == 1 -> parts += "yüz"
            hundreds > 1 -> { parts += ONES[hundreds]; parts += "yüz" }
        }
        if (tens > 0) parts += TENS[tens]
        if (ones > 0) parts += ONES[ones]
        return parts.joinToString(" ")
    }
}
