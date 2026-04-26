package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import java.util.UUID

object SessionDeviceManager {
    private const val PREFS_DEVICE = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val HEARTBEAT_INTERVAL_MS = 15_000L
    private const val SESSION_STALE_MS = 45_000L
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null

    private fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun claimActiveSessionIfLoggedIn(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = getOrCreateDeviceId(context)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "activeDeviceId" to deviceId,
                    "activeSessionUpdatedAt" to Timestamp.now(),
                ),
                SetOptions.merge(),
            )
    }

    fun startSessionHeartbeat(context: Context) {
        stopSessionHeartbeat()
        val appContext = context.applicationContext
        heartbeatRunnable = object : Runnable {
            override fun run() {
                claimActiveSessionIfLoggedIn(appContext)
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            }
        }.also {
            it.run() // immediate claim
        }
    }

    fun stopSessionHeartbeat() {
        heartbeatRunnable?.let { heartbeatHandler.removeCallbacks(it) }
        heartbeatRunnable = null
    }

    fun releaseActiveSessionIfOwned(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = getOrCreateDeviceId(context)
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        userRef.get()
            .addOnSuccessListener { doc ->
                val activeDeviceId = doc.getString("activeDeviceId")
                if (activeDeviceId == deviceId) {
                    userRef.set(
                        mapOf(
                            "activeDeviceId" to null,
                            "activeSessionUpdatedAt" to Timestamp.now(),
                        ),
                        SetOptions.merge(),
                    )
                }
            }
    }

    fun requireLoggedInAndSingleDevice(
        activity: AppCompatActivity,
        onAllowed: () -> Unit,
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            redirectToLogin(activity)
            return
        }
        val deviceId = getOrCreateDeviceId(activity)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val activeDeviceId = doc.getString("activeDeviceId")
                val lastUpdated = doc.getTimestamp("activeSessionUpdatedAt")
                val nowMs = System.currentTimeMillis()
                val lastUpdatedMs = lastUpdated?.toDate()?.time ?: 0L
                val isStale = lastUpdated == null || (nowMs - lastUpdatedMs) > SESSION_STALE_MS

                if (activeDeviceId.isNullOrBlank() || activeDeviceId == deviceId || isStale) {
                    if (activeDeviceId.isNullOrBlank() || activeDeviceId != deviceId) {
                        claimActiveSessionIfLoggedIn(activity)
                    }
                    onAllowed()
                } else {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(activity, "Bu hesap başka bir cihazda aktif.", Toast.LENGTH_SHORT).show()
                    redirectToLogin(activity)
                }
            }
            .addOnFailureListener {
                // Fail-open yerine güvenli davran: tek cihaz kuralı doğrulanamadıysa izin verme.
                redirectToLogin(activity)
            }
    }

    fun requireLoggedInAndSingleDevice(
        fragment: Fragment,
        onAllowed: () -> Unit,
    ) {
        val act = fragment.activity as? AppCompatActivity ?: return
        requireLoggedInAndSingleDevice(act, onAllowed)
    }

    private fun redirectToLogin(activity: AppCompatActivity) {
        val intent = Intent(activity, LoginStartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
    }
}

