// Kaynak: app/src/main/java/com/example/app/SobiAbacusPlayer.kt — saf kısım: hamlelerin dokunuşlara
// düzleştirilmesi (flatten), her dokunuştan sonra abaküsün değeri ve zamanlama (START_PAUSE_MS 700,
// STEP_MS 900, END_PAUSE_MS 1400).
//
// Android her dokunuşu AbacusBeadController.animateGuideBead ile yapıyor (beşlik boncuğa dokunmak onu
// çevirir; n. birlik boncuğa dokunmak hedefe kadar olanları birlikte oynatır). Burada her dokunuşun
// SONUCU (abaküsün yeni değeri) verilir; platform o değeri animasyonla yazar. Bir dokunuşta yalnızca
// tek sütunun beşliği ya da birlikleri değiştiği için oynayan boncuklar Android'dekiyle aynıdır.
package com.sorobit.shared.sobianaliz

/** Oynatmanın tek karesi: [zamanMs] anında abaküs [deger]'e getirilir. [hamle]: false ise başa dönüş. */
data class SobiOynatmaKaresi(val zamanMs: Long, val deger: Int, val hamle: Boolean)

/** Bütün oynatma: kareler ve [bitisMs] anında bitiş (Android: onDone). */
data class SobiOynatmaPlani(val kareler: List<SobiOynatmaKaresi>, val bitisMs: Long)

object SobiAbakusOynatici {
    const val START_PAUSE_MS = 700L
    const val STEP_MS = 900L
    const val END_PAUSE_MS = 1400L

    /**
     * Android play(action): önce abaküs [baslangic]'a, sonra hamleler dersteki sırayla, en sonda yine
     * [baslangic]'a (çocuk kendisi denesin). İşlem abaküse sığmıyorsa null (Android: false döner).
     */
    fun plan(baslangic: Int, toplama: Boolean, sayi: Int): SobiOynatmaPlani? {
        val op = if (toplama) HomeworkOp.ADD else HomeworkOp.SUB
        val e = StepExplainer.explain(baslangic.toLong(), op, sayi.toLong()) ?: return null
        val dokunuslar = ArrayList<Dokunus>()
        e.moves.forEach { m -> duzlestir(m.steps, dokunuslar) }

        val kareler = ArrayList<SobiOynatmaKaresi>()
        var t = 0L
        kareler += SobiOynatmaKaresi(t, baslangic, false)
        t += START_PAUSE_MS
        var deger = baslangic
        for (d in dokunuslar) {
            deger = uygula(deger, d)
            kareler += SobiOynatmaKaresi(t, deger, true)
            t += STEP_MS
        }
        t += END_PAUSE_MS
        kareler += SobiOynatmaKaresi(t, baslangic, false)
        t += START_PAUSE_MS
        return SobiOynatmaPlani(kareler, t)
    }

    /** Tek dokunuş: [ust] beşliği çevir; değilse birlikleri [fark] kadar oynat (0..4'e kırpılır). */
    private data class Dokunus(val sutun: Int, val ust: Boolean, val fark: Int)

    /** Adımları sırayla düzleştirir; elde adımında sol sütunun hareketi kendi adımlarıyla. */
    private fun duzlestir(steps: List<StepExplainer.Step>, out: MutableList<Dokunus>) {
        for (s in steps) {
            val carry = s.carry
            if (carry != null) {
                duzlestir(carry.steps, out)
                continue
            }
            // Beşlik ve birlik aynı adımdaysa (kuralsız 6 gibi) ikisi ayrı dokunuş: çocuk ikisini de görsün.
            if (s.upper != 0) out += Dokunus(s.col, true, 0)
            if (s.lower != 0) out += Dokunus(s.col, false, s.lower)
        }
    }

    private fun uygula(deger: Int, d: Dokunus): Int {
        val basamak = usAl(d.sutun)
        val rakam = (deger / basamak) % 10
        val yeni = if (d.ust) {
            if (rakam >= 5) rakam - 5 else rakam + 5
        } else {
            val birlik = rakam % 5
            val hedef = (birlik + d.fark).coerceIn(0, 4)
            rakam - birlik + hedef
        }
        return deger + (yeni - rakam) * basamak
    }

    private fun usAl(n: Int): Int {
        var r = 1
        repeat(n) { r *= 10 }
        return r
    }
}
