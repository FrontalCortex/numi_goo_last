package com.example.app

import android.os.Handler
import android.os.Looper
import com.example.app.abacus.AbacusBeadController
import com.example.app.abacus.StepExplainer
import com.example.app.homework.HomeworkOp

/**
 * Sobi'nin "abaküste göster" işini çocuğun abaküsünde oynatır: önce abaküsü işlemin başına
 * getirir, sonra [StepExplainer]'ın hamlelerini dersteki sırayla, boncuk boncuk yapar, en sonda
 * abaküsü yine başa döndürür ki çocuk kendisi denesin (cevabı onun yerine bırakmıyoruz).
 *
 * NEDEN animateGuideBead
 *   Boncuklar [AbacusBeadController.animateGuideBead] ile, yani çocuğun dokunuşuyla aynı yoldan
 *   oynuyor: denetleyicinin değeri, görünüm ve ses birlikte güncelleniyor. Öğreticinin kendi
 *   BeadAnimation sistemi denetleyicinin dışından boncuk kaydırıyor ve sonra durumu ayrıca eşitlemek
 *   gerekiyor; o yol burada kullanılmıyor.
 *
 * Hamleleri sunucu da hesaplıyor ama burada yeniden hesaplanıyor: iki kopya aynı ortak tabloyla
 * sınanıyor (shared/step_explainer_cases.json), sonuç aynı.
 */
class SobiAbacusPlayer(private val controller: AbacusBeadController) {

    private val handler = Handler(Looper.getMainLooper())
    private var playing = false

    val isPlaying: Boolean get() = playing

    /**
     * @return oynatılamıyorsa (işlem abaküse sığmıyor ya da zaten oynuyor) false; [onDone] çağrılmaz.
     */
    fun play(action: ServerSobi.Action, onDone: () -> Unit): Boolean {
        if (playing) return false
        val op = if (action.op == "add") HomeworkOp.ADD else if (action.op == "sub") HomeworkOp.SUB else return false
        val e = StepExplainer.explain(action.base.toLong(), op, action.number.toLong()) ?: return false

        val moves = ArrayList<() -> Unit>()
        e.moves.forEach { m -> flatten(m.steps, moves) }

        playing = true
        var t = 0L
        handler.postDelayed({ setValue(action.base) }, t)
        t += START_PAUSE_MS
        for (move in moves) {
            handler.postDelayed(move, t)
            t += STEP_MS
        }
        t += END_PAUSE_MS
        handler.postDelayed({ setValue(action.base) }, t)
        t += START_PAUSE_MS
        handler.postDelayed({
            playing = false
            onDone()
        }, t)
        return true
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(null)
        playing = false
    }

    /** Adımları sırayla düzleştirir; elde adımında sol sütunun hareketi kendi adımlarıyla. */
    private fun flatten(steps: List<StepExplainer.Step>, out: MutableList<() -> Unit>) {
        for (s in steps) {
            val carry = s.carry
            if (carry != null) {
                flatten(carry.steps, out)
                continue
            }
            val rod = rodOf(s.col)
            // Beşlik ve birlik aynı adımdaysa (kuralsız 6 gibi) ikisi ayrı dokunuş: çocuk ikisini de
            // görsün.
            if (s.upper != 0 && s.lower != 0) {
                out += { toggleTop(rod) }
                out += { moveBottom(rod, s.lower) }
            } else if (s.upper != 0) {
                out += { toggleTop(rod) }
            } else if (s.lower != 0) {
                out += { moveBottom(rod, s.lower) }
            }
        }
    }

    /** Abaküsü [value]'ya getirir; her sütun kendi dokunuşuyla, hepsi aynı anda. */
    private fun setValue(value: Int) {
        val current = controller.getCurrentValue()
        for (rod in 0..4) {
            val want = digitAt(value, rod)
            val have = digitAt(current, rod)
            if ((want >= 5) != (have >= 5)) toggleTop(rod)
            if (want % 5 != have % 5) setBottom(rod, have % 5, want % 5)
        }
    }

    private fun toggleTop(rod: Int) = controller.animateGuideBead("rod${rod}TopBead")

    private fun moveBottom(rod: Int, delta: Int) {
        val have = digitAt(controller.getCurrentValue(), rod) % 5
        setBottom(rod, have, (have + delta).coerceIn(0, 4))
    }

    /**
     * Denetleyicide n. birlik boncuğa dokunmak: boncuk aşağıdaysa 1..n yukarı gelir, yukarıdaysa
     * n..4 aşağı iner (AbacusBeadController.onBottomBeadClicked). Bu yüzden hedefe çıkarken hedef
     * boncuğa, inerken hedefin bir üstündekine dokunuluyor.
     */
    private fun setBottom(rod: Int, from: Int, to: Int) {
        if (from == to) return
        val bead = if (to > from) to else to + 1
        controller.animateGuideBead("rod${rod}BottomBead$bead")
    }

    private companion object {
        const val START_PAUSE_MS = 700L
        const val STEP_MS = 900L
        const val END_PAUSE_MS = 1400L

        /** StepExplainer sütunu (0 = birler) → denetleyici çubuğu (rod4 = birler, rod0 = on binler). */
        fun rodOf(col: Int) = 4 - col

        fun digitAt(value: Int, rod: Int): Int {
            var v = value
            repeat(4 - rod) { v /= 10 }
            return v % 10
        }
    }
}
