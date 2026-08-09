package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object TimeTracker {
    private const val PREFS_NAME = "time_tracker_prefs"
    private const val KEY_UNSYNCED_TIME = "unsynced_time_seconds"
    private const val KEY_LAST_START_TIME = "last_start_time"
    
    private var isTracking = false
    private var startTime: Long = 0
    private var scheduledTask: ScheduledFuture<*>? = null
    private val executor = Executors.newSingleThreadScheduledExecutor()
    
    private lateinit var prefs: SharedPreferences
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _unsyncedTimeFlow = MutableStateFlow(0L)
    val unsyncedTimeFlow: StateFlow<Long> = _unsyncedTimeFlow.asStateFlow()
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _unsyncedTimeFlow.value = prefs.getLong(KEY_UNSYNCED_TIME, 0)
        
        // Eğer uygulama kapanmadan önce tracking başlamışsa, o süreyi de ekle
        val lastStartTime = prefs.getLong(KEY_LAST_START_TIME, 0)
        if (lastStartTime > 0) {
            val elapsedTime = (System.currentTimeMillis() - lastStartTime) / 1000
            if (elapsedTime > 0 && elapsedTime < 3600) { // 1 saatten fazla değilse (uygulama kapanmış olabilir)
                addUnsyncedTime(elapsedTime)
            }
            // Last start time'ı temizle
            prefs.edit().remove(KEY_LAST_START_TIME).apply()
        }
        
        // Başlangıçta senkronize edilmemiş süre varsa buluta yolla
        syncToFirestore()
    }
    
    fun startTracking() {
        if (isTracking) {
            Log.d("TimeTracker", "Zaten tracking yapılıyor")
            return
        }
        
        isTracking = true
        startTime = System.currentTimeMillis()
        
        // SharedPreferences'a başlangıç zamanını kaydet (Crash durumları için)
        prefs.edit().putLong(KEY_LAST_START_TIME, startTime).apply()
        
        // Her 10 saniyede bir süreyi sadece lokale ekle
        scheduledTask = executor.scheduleAtFixedRate({
            if (isTracking) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = (currentTime - startTime) / 1000
                if (elapsedTime >= 10) { // En az 10 saniye geçmişse
                    addUnsyncedTime(elapsedTime)
                    startTime = currentTime // Start time'ı güncelle
                    prefs.edit().putLong(KEY_LAST_START_TIME, startTime).apply()
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
            addUnsyncedTime(elapsedTime)
        }
        
        // Scheduled task'ı iptal et
        scheduledTask?.cancel(false)
        scheduledTask = null
        
        // Last start time'ı temizle
        prefs.edit().remove(KEY_LAST_START_TIME).apply()
        
        // Uygulama arka plana geçtiği için Firestore'a tek seferlik senkronize et
        syncToFirestore()
        
        Log.d("TimeTracker", "Süre takibi durduruldu. Firestore'a senkronizasyon tetiklendi.")
    }
    
    private fun addUnsyncedTime(seconds: Long) {
        if (seconds <= 0) return
        val currentUnsynced = prefs.getLong(KEY_UNSYNCED_TIME, 0)
        val newUnsynced = currentUnsynced + seconds
        prefs.edit().putLong(KEY_UNSYNCED_TIME, newUnsynced).apply()
        _unsyncedTimeFlow.value = newUnsynced
    }
    
    private fun syncToFirestore() {
        val unsynced = prefs.getLong(KEY_UNSYNCED_TIME, 0)
        if (unsynced <= 0) return
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Firestore işlemini başlattığımız an lokaldeki sayacı sıfırlıyoruz.
            // Firestore çevrimdışı olsa bile arkaplanda bu isteği kuyruğa alır ve internet gelince gönderir.
            // Böylece double-count (çift sayma) ihtimalini sıfırlıyoruz.
            prefs.edit().putLong(KEY_UNSYNCED_TIME, 0).apply()
            _unsyncedTimeFlow.value = 0L
            
            firestore.collection("users").document(currentUser.uid)
                .update("totalTimeSpent", FieldValue.increment(unsynced))
                .addOnSuccessListener {
                    Log.d("TimeTracker", "Firestore'a $unsynced saniye başarıyla eklendi.")
                }
                .addOnFailureListener { e ->
                    Log.e("TimeTracker", "Firestore'a süre eklenirken hata: ${e.message}")
                    // İsteğe bağlı olarak hata durumunda süreyi geri yükleyebiliriz ama offline desteği olduğu için gerek yok.
                }
        }
    }
    
    fun reset() {
        prefs.edit().putLong(KEY_UNSYNCED_TIME, 0).apply()
        _unsyncedTimeFlow.value = 0L
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .update("totalTimeSpent", 0)
        }
    }
}
