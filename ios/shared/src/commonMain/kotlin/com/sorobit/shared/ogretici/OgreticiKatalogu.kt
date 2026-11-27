// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, onViewCreated içindeki
// `currentTutorialSteps = when (tutorialNumber) { ... else -> tutorialSteps }` (satır ~480-516).
//
// Her öğreticinin içeriği kendi dosyasındadır (icerik/OgreticiN.kt). Henüz taşınmamış öğretici
// null döndürür; Swift tarafı o zaman yer tutucuyu gösterir. Android'deki `else -> tutorialSteps`
// (bilinmeyen numarada 1. öğretici) BİLEREK alınmadı: yanlış öğretici göstermek yerine yer tutucu.
package com.sorobit.shared.ogretici

import com.sorobit.shared.ogretici.icerik.ogretici1
import com.sorobit.shared.ogretici.icerik.ogretici10
import com.sorobit.shared.ogretici.icerik.ogretici100
import com.sorobit.shared.ogretici.icerik.ogretici101
import com.sorobit.shared.ogretici.icerik.ogretici102
import com.sorobit.shared.ogretici.icerik.ogretici103
import com.sorobit.shared.ogretici.icerik.ogretici104
import com.sorobit.shared.ogretici.icerik.ogretici105
import com.sorobit.shared.ogretici.icerik.ogretici11
import com.sorobit.shared.ogretici.icerik.ogretici1100
import com.sorobit.shared.ogretici.icerik.ogretici12
import com.sorobit.shared.ogretici.icerik.ogretici13
import com.sorobit.shared.ogretici.icerik.ogretici14
import com.sorobit.shared.ogretici.icerik.ogretici15
import com.sorobit.shared.ogretici.icerik.ogretici16
import com.sorobit.shared.ogretici.icerik.ogretici17
import com.sorobit.shared.ogretici.icerik.ogretici18
import com.sorobit.shared.ogretici.icerik.ogretici19
import com.sorobit.shared.ogretici.icerik.ogretici2
import com.sorobit.shared.ogretici.icerik.ogretici20
import com.sorobit.shared.ogretici.icerik.ogretici21
import com.sorobit.shared.ogretici.icerik.ogretici22
import com.sorobit.shared.ogretici.icerik.ogretici23
import com.sorobit.shared.ogretici.icerik.ogretici24
import com.sorobit.shared.ogretici.icerik.ogretici25
import com.sorobit.shared.ogretici.icerik.ogretici26
import com.sorobit.shared.ogretici.icerik.ogretici3
import com.sorobit.shared.ogretici.icerik.ogretici4
import com.sorobit.shared.ogretici.icerik.ogretici5
import com.sorobit.shared.ogretici.icerik.ogretici6
import com.sorobit.shared.ogretici.icerik.ogretici7
import com.sorobit.shared.ogretici.icerik.ogretici8
import com.sorobit.shared.ogretici.icerik.ogretici9
import com.sorobit.shared.ogretici.icerik.ogretici999

object OgreticiKatalogu {
    /** Android'deki bütün öğretici numaraları (when sırasıyla). */
    val numaralar: List<Int> = listOf(
        1, 100, 101, 2, 102, 103, 3, 4, 5, 6, 104, 7, 105, 8, 9, 10, 11, 1100, 12, 13, 14, 999,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
    )

    /** [numara]'nın içeriği; taşınmamışsa, boşsa ya da numara bilinmiyorsa null. */
    fun icerik(numara: Int): OgreticiIcerigi? {
        val icerik: OgreticiIcerigi? = when (numara) {
            1 -> ogretici1()
            100 -> ogretici100()
            101 -> ogretici101()
            2 -> ogretici2()
            102 -> ogretici102()
            103 -> ogretici103()
            3 -> ogretici3()
            4 -> ogretici4()
            5 -> ogretici5()
            6 -> ogretici6()
            104 -> ogretici104()
            7 -> ogretici7()
            105 -> ogretici105()
            8 -> ogretici8()
            9 -> ogretici9()
            10 -> ogretici10()
            11 -> ogretici11()
            1100 -> ogretici1100()
            12 -> ogretici12()
            13 -> ogretici13()
            14 -> ogretici14()
            999 -> ogretici999()
            15 -> ogretici15()
            16 -> ogretici16()
            17 -> ogretici17()
            18 -> ogretici18()
            19 -> ogretici19()
            20 -> ogretici20()
            21 -> ogretici21()
            22 -> ogretici22()
            23 -> ogretici23()
            24 -> ogretici24()
            25 -> ogretici25()
            26 -> ogretici26()
            else -> null
        }
        return if (icerik != null && icerik.adimSayisi > 0) icerik else null
    }

    fun varMi(numara: Int): Boolean = icerik(numara) != null

    /**
     * Swift için tek giriş: içerik varsa oynatıcıyı kurar, yoksa null. [ilkAcilis]: hesapsız ilk
     * açılış (Android isFirstRunTutorial). [baslangicAdimi] sınırlara kırpılır.
     */
    fun oynaticiKur(numara: Int, ilkAcilis: Boolean, baslangicAdimi: Int): OgreticiOynatici? {
        val icerik = icerik(numara) ?: return null
        return OgreticiOynatici(icerik, ilkAcilis, baslangicAdimi)
    }
}
