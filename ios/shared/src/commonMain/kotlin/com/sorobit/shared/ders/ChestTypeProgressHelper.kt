// KAYNAK: app/src/main/java/com/example/app/ChestTypeProgressHelper.kt (tamamı)
//
// Android'den bilinçli farklar:
//  - R.drawable.* kimlikleri yerine kaynak adı taşıyan String ([ChestIcons]).
//  - Android `GlobalLessonData.globalPartId` global'ini doğrudan okuyordu; burada aynı değer varsayılan
//    parametre olarak geliyor (test edilebilirlik için), davranış aynı.
//  - Log.d("guguk", ...) kaldırıldı.
package com.sorobit.shared.ders

/**
 * TYPE_CHEST satırında kayıt skoruna göre kupa görseli ve yıldız sayısı.
 */
object ChestTypeProgressHelper {

    fun resolvedChestIcon(
        item: LessonItem,
        recordValue: Int,
        globalPartId: Int = GlobalLessonData.globalPartId,
    ): String {
        if (item.isBlinding == true && globalPartId != 6) {
            return ChestIcons.STAR_ON_IC
        }

        val record = recordValue.coerceAtLeast(0)
        val p1 = item.cupPoint1
        val p2 = item.cupPoint2
        return when {
            p1 != null && record >= p1 -> ChestIcons.CHEST_STARS_TIER3
            p2 != null && record >= p2 -> ChestIcons.CHEST_STARS_TIER2
            record >= 500 -> ChestIcons.CHEST_STARS_TIER1
            else -> ChestIcons.CHEST_STARS_TIER0
        }
    }

    fun starCountForChestIcon(iconResId: String): Int = when (iconResId) {
        ChestIcons.CHEST_STARS_TIER3 -> 3
        ChestIcons.CHEST_STARS_TIER2 -> 2
        ChestIcons.CHEST_STARS_TIER1 -> 1
        ChestIcons.CHEST_STARS_TIER0 -> 0
        ChestIcons.STAR_ON_IC -> 0
        else -> 0
    }

    /**
     * Bu oyun sonunda (recordScoreThisRun ile) ilk kez 3 yıldıza ulaşılıyor mu?
     * [item] tıklanmadan önceki harita durumu; [recordScoreThisRun] bu seferki ChestResult/ChestFragment skoru.
     */
    fun shouldIncrementKarateForFirstThreeStars(
        item: LessonItem,
        recordScoreThisRun: Int,
        globalPartId: Int = GlobalLessonData.globalPartId,
    ): Boolean {
        if (item.type != LessonItem.TYPE_CHEST) return false
        val oldStars = starCountForChestIcon(item.stepCupIcon)
        val mergedRecord = maxOf(item.record ?: 0, recordScoreThisRun)
        val newIcon = resolvedChestIcon(item, mergedRecord, globalPartId)
        val newStars = starCountForChestIcon(newIcon)
        return oldStars < 3 && newStars >= 3
    }
}
