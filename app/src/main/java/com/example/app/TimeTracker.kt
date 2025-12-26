package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object TimeTracker {
    private const val PREFS_NAME = "time_tracker_prefs"
    private const val KEY_TOTAL_TIME_SECONDS = "total_time_seconds"
    private const val KEY_LAST_START_TIME = "last_start_time"
    
    private var isTracking = false
    private var startTime: Long = 0
    private var scheduledTask: ScheduledFuture<*>? = null
    private val executor = Executors.newSingleThreadScheduledExecutor()
    
    private lateinit var prefs: SharedPreferences
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Eğer uygulama kapanmadan önce tracking başlamışsa, o süreyi de ekle
        val lastStartTime = prefs.getLong(KEY_LAST_START_TIME, 0)
        if (lastStartTime > 0) {
            val elapsedTime = (System.currentTimeMillis() - lastStartTime) / 1000
            if (elapsedTime > 0 && elapsedTime < 3600) { // 1 saatten fazla değilse (uygulama kapanmış olabilir)
                addTime(elapsedTime.toLong())
            }
            // Last start time'ı temizle
            prefs.edit().remove(KEY_LAST_START_TIME).apply()
        }
    }
    
    fun startTracking() {
        if (isTracking) {
            Log.d("TimeTracker", "Zaten tracking yapılıyor")
            return
        }
        
        isTracking = true
        startTime = System.currentTimeMillis()
        
        // SharedPreferences'a başlangıç zamanını kaydet
        prefs.edit().putLong(KEY_LAST_START_TIME, startTime).apply()
        
        // Her 10 saniyede bir süreyi kaydet
        scheduledTask = executor.scheduleAtFixedRate({
            if (isTracking) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = (currentTime - startTime) / 1000
                if (elapsedTime >= 10) { // En az 10 saniye geçmişse
                    addTime(elapsedTime)
                    startTime = currentTime // Start time'ı güncelle
                }
            }
        }, 10, 10, TimeUnit.SECONDS)
        
        Log.d("TimeTracker", "Süre takibi başlatıldı")
    }
    
    fun stopTracking() {
        if (!isTracking) {
            return
        }
        
        isTracking = false
        
        // Son süreyi ekle
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
        if (elapsedTime > 0) {
            addTime(elapsedTime)
        }
        
        // Scheduled task'ı iptal et
        scheduledTask?.cancel(false)
        scheduledTask = null
        
        // Last start time'ı temizle
        prefs.edit().remove(KEY_LAST_START_TIME).apply()
        
        Log.d("TimeTracker", "Süre takibi durduruldu. Eklenen süre: $elapsedTime saniye")
    }
    
    private fun addTime(seconds: Long) {
        if (seconds <= 0) return
        
        val currentTotal = getTotalTimeSeconds()
        val newTotal = currentTotal + seconds
        
        // SharedPreferences'a kaydet
        prefs.edit().putLong(KEY_TOTAL_TIME_SECONDS, newTotal).apply()
        
        // Firestore'a da kaydet (kullanıcı giriş yapmışsa)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .update("totalTimeSpent", newTotal)
                .addOnFailureListener { e ->
                    Log.e("TimeTracker", "Firestore'a süre kaydedilemedi", e)
                }
        }
        
        Log.d("TimeTracker", "Toplam süre güncellendi: $newTotal saniye (eklenen: $seconds)")
    }
    
    fun getTotalTimeSeconds(): Long {
        return prefs.getLong(KEY_TOTAL_TIME_SECONDS, 0)
    }
    
    fun reset() {
        prefs.edit().putLong(KEY_TOTAL_TIME_SECONDS, 0).apply()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .update("totalTimeSpent", 0)
        }
    }
}

