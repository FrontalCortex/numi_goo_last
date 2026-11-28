// Kaynak (Android): app/src/main/java/com/example/app/CupHistoryFragment.kt — görünümden bağımsız kısım:
// RangeTab, sekme başlıkları, sekme → sorgu penceresi (loadForTab), loadCurrentScore varsayılanı, niceAxisRange,
// bucket, groupLastPerKey, daysAgo, formatLabel, grafik noktası/etiket sayısı kuralları.
// Grafik çizimi (MPAndroidChart), sekme arayüzü ve Firestore okumaları taşınmadı.
package com.sorobit.shared.kupa

import kotlinx.datetime.TimeZone

object CupHistoryLogic {

    /** Android'de `private enum class RangeTab`; sekme konumu = ordinal. */
    enum class RangeTab { WEEKLY, MONTHLY, YEARLY, ALL }

    /** Sekme başlıkları ([RangeTab] sırasıyla). "Günlük" sekmesi bilinçli olarak yok. */
    val TAB_LABELS: List<String> = listOf("Haftalık", "Aylık", "Yıllık", "Tüm Zamanlar")

    /** Android `loadCurrentScore`: alan yoksa gösterilen varsayılan skor. */
    const val DEFAULT_CURRENT_SCORE = 200

    /** Android `loadCurrentScore`: `(doc?.get(field) as? Number)?.toInt() ?: 200`. */
    fun currentScoreFromField(rawFieldValue: Any?): Int =
        (rawFieldValue as? Number)?.toInt() ?: DEFAULT_CURRENT_SCORE

    /**
     * Android `loadForTab`: Haftalık/Aylık/Yıllık için `fetchHistorySince(daysAgo(7/30/365))`, Tüm Zamanlar için
     * `fetchHistory` (tüm koleksiyon). Dönüş: sorgu penceresinin gün sayısı; null = tüm geçmiş.
     */
    fun windowDaysForTab(tab: RangeTab): Int? = when (tab) {
        RangeTab.WEEKLY -> 7
        RangeTab.MONTHLY -> 30
        RangeTab.YEARLY -> 365
        RangeTab.ALL -> null
    }

    /** Sekmenin `fetchHistorySince` başlangıç kimliği ("yyyy-MM-dd"); [RangeTab.ALL] için null. */
    fun sinceDateIdForTab(
        tab: RangeTab,
        nowMs: Long = kupaNowMs(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String? = windowDaysForTab(tab)?.let { daysAgo(it, nowMs, timeZone) }

    /** Android `daysAgo`: yerel takvimde bugünden [days] gün öncesi, "yyyy-MM-dd". */
    fun daysAgo(
        days: Int,
        nowMs: Long = kupaNowMs(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String = kupaDateId(daysAgo = days, nowMs = nowMs, timeZone = timeZone)

    /**
     * Y ekseni için küsüratsız, geniş ve 10'un katlarına yuvarlanmış bir aralık üretir.
     * Tek nokta ya da dar değer aralıklarında grafiğin gereksiz yere yakınlaştırılıp virgüllü etiketler
     * göstermesini engeller. ([values] boşsa Android gibi NoSuchElementException atar.)
     */
    fun niceAxisRange(values: List<Int>): Pair<Int, Int> {
        val dataMin = values.min()
        val dataMax = values.max()
        val span = dataMax - dataMin
        val padding = maxOf((span * 0.3f).toInt(), 20)
        val step = 10
        val axisMin = (((dataMin - padding).coerceAtLeast(0)) / step) * step
        val rawMax = dataMax + padding
        val axisMax = ((rawMax + step - 1) / step) * step
        return axisMin to axisMax
    }

    /**
     * [points] ilgili sekmeye göre zaten Firestore sorgusuyla sınırlandırılmış durumdadır — burada yalnızca
     * görüntüleme için gruplama yapılır. Haftalık/Aylık günlük çözünürlükte kalır; Yıllık aya göre gruplanır;
     * Tüm Zamanlar 60 noktadan fazlaysa aya göre gruplanır.
     */
    fun bucket(points: List<Pair<String, Int>>, tab: RangeTab): List<Pair<String, Int>> {
        if (points.isEmpty()) return emptyList()
        return when (tab) {
            RangeTab.WEEKLY -> points
            RangeTab.MONTHLY -> points
            RangeTab.YEARLY -> groupLastPerKey(points) { it.substring(0, 7) }
            RangeTab.ALL -> if (points.size <= 60) points else groupLastPerKey(points) { it.substring(0, 7) }
        }
    }

    /** Her anahtar için SON noktayı tutar; sıra, anahtarın ilk görülme sırasıdır (LinkedHashMap). */
    fun groupLastPerKey(points: List<Pair<String, Int>>, keyFn: (String) -> String): List<Pair<String, Int>> {
        val grouped = LinkedHashMap<String, Pair<String, Int>>()
        for ((date, value) in points) {
            grouped[keyFn(date)] = date to value
        }
        return grouped.values.toList()
    }

    /** Android `SimpleDateFormat("MMM", Locale("tr"))` kısa ay adları (CLDR). */
    val TR_SHORT_MONTHS: List<String> =
        listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")

    /**
     * [formatLabel] kalıbı (Android `SimpleDateFormat` deseni): Haftalık/Aylık "d MMM", Yıllık/Tüm Zamanlar
     * "MMM yy". iOS, Türkçe dışındaki dillerde etiketi bu kalıpla ve seçili dilin yereliyle biçimler.
     */
    fun labelPattern(tab: RangeTab): String = when (tab) {
        RangeTab.WEEKLY, RangeTab.MONTHLY -> "d MMM"
        RangeTab.YEARLY, RangeTab.ALL -> "MMM yy"
    }

    /**
     * Android `formatLabel`: Haftalık/Aylık → "d MMM" (ör. "5 Kas"), Yıllık/Tüm Zamanlar → "MMM yy" (ör. "Kas 26"),
     * Türkçe yerel.
     *
     * Fark: Android'de ayrıştırılamayan tarih `ParseException` atar (ve SimpleDateFormat gevşek ayrıştırır);
     * burada "yyyy-MM-dd" biçimine uymayan girdi olduğu gibi döndürülür. Doküman kimlikleri hep bu biçimdedir.
     */
    fun formatLabel(tab: RangeTab, dateStr: String): String {
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        val year = parts[0].toIntOrNull() ?: return dateStr
        val month = parts[1].toIntOrNull() ?: return dateStr
        val day = parts[2].toIntOrNull() ?: return dateStr
        if (month !in 1..12 || day !in 1..31) return dateStr
        val mmm = TR_SHORT_MONTHS[month - 1]
        return when (tab) {
            RangeTab.WEEKLY, RangeTab.MONTHLY -> "$day $mmm"
            RangeTab.YEARLY, RangeTab.ALL -> "$mmm ${(year % 100).toString().padStart(2, '0')}"
        }
    }

    /** Android `renderChart`: tek nokta varsa çizgi çizilemez; nokta daire olarak gösterilir + ipucu metni. */
    fun isSinglePoint(bucketed: List<Pair<String, Int>>): Boolean = bucketed.size == 1

    /** Android `renderChart`: `xAxis.labelCount = minOf(labels.size, 6).coerceAtLeast(1)`. */
    fun xAxisLabelCount(labelCount: Int): Int = minOf(labelCount, 6).coerceAtLeast(1)
}
