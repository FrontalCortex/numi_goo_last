package com.example.app

object RecordTimeUtils {

    fun parseRecordToScore(record: String): Int? = record.trim().toIntOrNull()

    fun formatScore(score: Int?): String = (score ?: 0).toString()
}
