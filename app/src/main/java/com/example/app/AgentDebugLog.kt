package com.example.app

import android.util.Log
import org.json.JSONObject

/**
 * Teşhis logu — yalnızca Logcat'e yazar.
 *
 * Not: Bu sınıf eskiden her çağrıda bir geliştirme makinesindeki HTTP ingest ucuna
 * (10.0.2.2:7913) POST atıyordu. O yol üretim derlemesinde hiçbir işe yaramıyor,
 * arka planda bir thread havuzunu ayakta tutuyor ve kullanıcının ağına istek çıkarıyordu;
 * bu yüzden kaldırıldı. İmza korundu, çağrı yerleri değişmedi.
 *
 * Logcat: `adb logcat -s AgentDebug188640`
 */
object AgentDebugLog {
    private const val TAG = "AgentDebug188640"
    private const val SESSION_ID = "188640"

    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "pre-fix",
    ) {
        if (!BuildConfig.DEBUG) return
        try {
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
            Log.d(TAG, payload.toString())
        } catch (e: Exception) {
            Log.d(TAG, "$location | $message")
        }
    }
}
