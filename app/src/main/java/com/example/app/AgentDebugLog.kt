package com.example.app

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Debug session NDJSON ingest (emulator: 10.0.2.2 → host 127.0.0.1:7913).
 * Logcat: AgentDebug188640
 */
object AgentDebugLog {
    private const val TAG = "AgentDebug188640"
    private const val ENDPOINT =
        "http://10.0.2.2:7913/ingest/8a0b1fc3-fae1-418f-bbbd-80b43a829b14"
    private const val SESSION_ID = "188640"
    private val executor = Executors.newSingleThreadExecutor()

    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "pre-fix",
    ) {
        val payload = JSONObject()
            .put("sessionId", SESSION_ID)
            .put("hypothesisId", hypothesisId)
            .put("location", location)
            .put("message", message)
            .put("timestamp", System.currentTimeMillis())
            .put("runId", runId)
        val dataObj = JSONObject()
        data.forEach { (k, v) -> dataObj.put(k, v) }
        payload.put("data", dataObj)
        val line = payload.toString()
        Log.d(TAG, line)
        executor.execute {
            try {
                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Debug-Session-Id", SESSION_ID)
                    doOutput = true
                    connectTimeout = 2000
                    readTimeout = 2000
                }
                conn.outputStream.use { it.write(line.toByteArray(Charsets.UTF_8)) }
                conn.inputStream.close()
                conn.disconnect()
            } catch (_: Exception) {
                // Logcat fallback only
            }
        }
    }
}
