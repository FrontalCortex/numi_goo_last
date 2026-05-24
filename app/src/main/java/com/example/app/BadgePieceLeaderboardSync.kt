package com.example.app

/**
 * Liderlik tabanlı kupa / madalya rozet satırları sezon bittiğinde Cloud Function
 * [finalizeSeasonLeaderboardMedals] tarafından `users/{uid}/badgeProgress/state` içine yazılır.
 * Bu nesne yalnızca Firestore alanlarını istemcide parse etmek için kullanılır.
 */
object BadgePieceLeaderboardSync {

    fun parseMedalPieceList(value: Any?): List<MedalPieceRow> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { row ->
            when (row) {
                is Map<*, *> -> {
                    val rawTitle = row["titleUnit"] ?: return@mapNotNull null
                    val title = rawTitle.toString().trim().ifEmpty { return@mapNotNull null }
                    val season = row["season"].firestoreIntOrNull() ?: return@mapNotNull null
                    MedalPieceRow(title, season)
                }
                is List<*> -> {
                    val rawTitle = row.getOrNull(0) ?: return@mapNotNull null
                    val title = rawTitle.toString().trim().ifEmpty { return@mapNotNull null }
                    val season = row.getOrNull(1).firestoreIntOrNull() ?: return@mapNotNull null
                    MedalPieceRow(title, season)
                }
                else -> null
            }
        }
    }

    fun parseCupPieceList(value: Any?): List<CupPieceRow> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { row ->
            when (row) {
                is Map<*, *> -> {
                    val rawTitle = row["titleUnit"] ?: return@mapNotNull null
                    val title = rawTitle.toString().trim().ifEmpty { return@mapNotNull null }
                    val rank = row["rank"].firestoreIntOrNull() ?: return@mapNotNull null
                    val season = row["season"].firestoreIntOrNull() ?: return@mapNotNull null
                    CupPieceRow(title, rank, season)
                }
                is List<*> -> {
                    val rawTitle = row.getOrNull(0) ?: return@mapNotNull null
                    val title = rawTitle.toString().trim().ifEmpty { return@mapNotNull null }
                    val rank = row.getOrNull(1).firestoreIntOrNull() ?: return@mapNotNull null
                    val season = row.getOrNull(2).firestoreIntOrNull() ?: return@mapNotNull null
                    CupPieceRow(title, rank, season)
                }
                else -> null
            }
        }
    }
}
