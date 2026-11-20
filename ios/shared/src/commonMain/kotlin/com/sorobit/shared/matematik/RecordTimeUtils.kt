// Kaynak: app/src/main/java/com/example/app/RecordTimeUtils.kt (Android) — birebir.
package com.sorobit.shared.matematik

object RecordTimeUtils {

    fun parseRecordToScore(record: String): Int? = record.trim().toIntOrNull()

    fun formatScore(score: Int?): String = (score ?: 0).toString()
}
