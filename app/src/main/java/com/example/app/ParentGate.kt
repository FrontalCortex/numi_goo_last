package com.example.app

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlin.random.Random

/**
 * VELİ KAPISI: dışarıya paylaşım gibi yetişkin işlerinden önce.
 *
 * Neden: Play Aile politikası, çocuklara yönelik uygulamada dış bağlantı / paylaşım gibi işlerin bir
 * yetişkin tarafından açılmasını istiyor. Kullanıcı kararı (08.11.2026): yazıyla çarpma sorusu.
 * Sayılar YAZIYLA ("on yedi çarpı üç"): soroban öğrencisi rakamla çarpmayı bilir, ama yazıyla
 * verilmiş iki basamaklı çarpımı okuyup çözmek küçük çocuğu ayırıyor. Her açılışta farklı soru.
 */
object ParentGate {

    /** Yan etkisiz: soru (a × b) ve beklenen cevap; test ve önizleme için ayrı. */
    data class Question(val a: Int, val b: Int) {
        val answer get() = a * b
    }

    fun newQuestion(rand: Random = Random.Default) = Question(11 + rand.nextInt(9), 3 + rand.nextInt(7))

    fun isCorrect(q: Question, typed: String): Boolean = typed.trim().toIntOrNull() == q.answer

    fun show(context: Context, onPass: () -> Unit) {
        val q = newQuestion()
        val words = context.resources.getStringArray(R.array.parent_gate_numbers)
        val pad = (20 * context.resources.displayMetrics.density).toInt()
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            textSize = 22f
        }
        val error = TextView(context).apply {
            text = appString(R.string.parent_gate_wrong)
            setTextColor(0xFFE57373.toInt())
            visibility = android.view.View.GONE
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(TextView(context).apply {
                text = appString(R.string.parent_gate_question, words[q.a], words[q.b])
                textSize = 17f
            })
            addView(input)
            addView(error)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(appString(R.string.parent_gate_title))
            .setView(box)
            .setPositiveButton(appString(R.string.parent_gate_continue), null)
            .setNegativeButton(appString(R.string.parent_gate_cancel), null)
            .create()
        dialog.setOnShowListener {
            // Yanlışta pencere kapanmasın: tekrar denesin (yeni soru yok; çocuk deneme yanılmayla
            // bulamasın diye üç yanlışta kapanıyor).
            var wrong = 0
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (isCorrect(q, input.text.toString())) {
                    dialog.dismiss()
                    onPass()
                } else if (++wrong >= 3) {
                    dialog.dismiss()
                } else {
                    error.visibility = android.view.View.VISIBLE
                    input.text.clear()
                }
            }
        }
        dialog.show()
    }
}
