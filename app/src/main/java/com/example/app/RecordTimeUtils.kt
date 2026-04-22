package com.example.app

/**
 * Ders rekoru metnini (ör. "3:45", "1:02:05") sıralama için saniyeye çevirir.
 */
object RecordTimeUtils {

    fun parseRecordToSeconds(record: String): Int? {
        val s = record.trim()
        if (s.isEmpty()) return null
        val parts = s.split(":").map { it.trim() }.filter { it.isNotEmpty() }
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: return null
                val seconds = parts[1].toIntOrNull() ?: return null
                if (seconds !in 0..59) return null
                minutes * 60 + seconds
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: return null
                val minutes = parts[1].toIntOrNull() ?: return null
                val seconds = parts[2].toIntOrNull() ?: return null
                if (minutes !in 0..59 || seconds !in 0..59) return null
                hours * 3600 + minutes * 60 + seconds
            }
            else -> null
        }
    }
}
