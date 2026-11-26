// iOS kopyasına özgü test yardımcısı (Android'de karşılığı yok).
package com.sorobit.shared.dersadim

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem

/**
 * Ders kataloğundaki her item'ın adım kimlikleri için tabloların ne döndürdüğünün özeti.
 *
 * Tablo seçimi Android'deki akışa göre: `isBlinding == true` dersler ve yarış part'ları (7, 8;
 * LessonAdapter yarışı her zaman BlindingLessonFragment ile açar) kör tabloyu (B), diğerleri
 * abaküs tablosunu (O) kullanır.
 *
 * Satır biçimi: `part|index|tablo|start-finish|min|istenen>çözülen[!],...`
 * (`!`: çözülen kimlik için de tablo boş, yani Android'de de ders soru listesi boş gelir).
 */
object KatalogButunluk {
    val PARTLAR = listOf(1, 2, 3, 4, 5, 6, 7, 8)

    fun korTabloMu(partId: Int, item: LessonItem): Boolean = item.isBlinding == true || partId == 7 || partId == 8

    fun ozetSatirlari(): List<String> = buildList {
        for (p in PARTLAR) {
            GlobalLessonData.createLessonItems(p).forEachIndexed { i, item ->
                val start = item.startStepNumber ?: return@forEachIndexed
                val finish = item.finishStepNumber ?: start
                val min = item.minLessonOperationsId()
                val kor = korTabloMu(p, item)
                val istenenler = if (start <= finish) (start..finish).toList() else listOf(start, finish)
                val parcalar = istenenler.map { istenen ->
                    val cozulen = if (kor) {
                        LessonOperations.resolveLessonOperationsBlindingId(istenen, min)
                    } else {
                        LessonOperations.resolveLessonOperationsId(istenen, min)
                    }
                    val bos = if (kor) {
                        LessonOperations.getLessonOperationsBlinding(cozulen).isEmpty()
                    } else {
                        LessonOperations.getLessonOperations(cozulen).isEmpty()
                    }
                    "$istenen>$cozulen" + if (bos) "!" else ""
                }
                add("$p|$i|${if (kor) "B" else "O"}|$start-$finish|$min|${parcalar.joinToString(",")}")
            }
        }
    }
}
