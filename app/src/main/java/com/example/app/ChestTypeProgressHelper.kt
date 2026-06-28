package com.example.app

import com.example.app.model.LessonItem

/**
 * TYPE_CHEST satırında kayıt skoruna göre kupa görseli ve yıldız sayısı.
 */
object ChestTypeProgressHelper {

    fun resolvedChestIcon(item: LessonItem, recordValue: Int): Int {
        if (item.isBlinding == true) {
            return R.drawable.star_on_ic
        }
        
        val record = recordValue.coerceAtLeast(0)
        val p1 = item.cupPoint1
        val p2 = item.cupPoint2
        return when {
            p1 != null && record >= p1 -> R.drawable.chest_stars_tier3
            p2 != null && record >= p2 -> R.drawable.chest_stars_tier2
            record >= 500 -> R.drawable.chest_stars_tier1
            else -> R.drawable.chest_stars_tier0
        }

    }

    fun starCountForChestIcon(iconResId: Int): Int = when (iconResId) {
        R.drawable.chest_stars_tier3 -> 3
        R.drawable.chest_stars_tier2 -> 2
        R.drawable.chest_stars_tier1 -> 1
        R.drawable.chest_stars_tier0 -> 0
        R.drawable.star_on_ic -> 0
        else -> 0
    }

    /**
     * Bu oyun sonunda (recordScoreThisRun ile) ilk kez 3 yıldıza ulaşılıyor mu?
     * [item] tıklanmadan önceki harita durumu; [recordScoreThisRun] bu seferki ChestResult/ChestFragment skoru.
     */
    fun shouldIncrementKarateForFirstThreeStars(
        item: LessonItem,
        recordScoreThisRun: Int,
    ): Boolean {
        if (item.type != LessonItem.TYPE_CHEST) return false
        val oldStars = starCountForChestIcon(item.stepCupIcon)
        val mergedRecord = maxOf(item.record ?: 0, recordScoreThisRun)
        val newIcon = resolvedChestIcon(item, mergedRecord)
        val newStars = starCountForChestIcon(newIcon)
        return oldStars < 3 && newStars >= 3
    }
}
