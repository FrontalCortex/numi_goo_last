package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper

class EnergyManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "energy_prefs"
        private const val KEY_CURRENT_ENERGY = "current_energy"
        private const val KEY_LAST_ENERGY_UPDATE = "last_energy_update"
        private const val MAX_ENERGY = 5
        private const val ENERGY_REFRESH_MINUTES = 1
        private const val ENERGY_REFRESH_MILLIS = 3L // 5 dakika = 5 * 60 * 1000 = 300000 milisaniye
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var energyUpdateCallback: ((Int) -> Unit)? = null
    
    init {
        initializeEnergy()
        startEnergyTimer()
    }
    
    private fun initializeEnergy() {
        val lastUpdate = prefs.getLong(KEY_LAST_ENERGY_UPDATE, 0L)
        val currentTime = System.currentTimeMillis()
        
        if (lastUpdate == 0L) {
            // İlk kez açılıyor, maksimum enerji ile başla
            setCurrentEnergy(MAX_ENERGY)
            setLastEnergyUpdate(currentTime)
        } else {
            // Son güncellemeden bu yana geçen süreyi hesapla
            val timeDiff = currentTime - lastUpdate
            val energyToAdd = (timeDiff / ENERGY_REFRESH_MILLIS).toInt()
            
            val currentEnergy = getCurrentEnergy()
            val newEnergy = minOf(currentEnergy + energyToAdd, MAX_ENERGY)
            
            setCurrentEnergy(newEnergy)
            setLastEnergyUpdate(currentTime)
        }
    }
    
    private fun startEnergyTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                addEnergy(1)
                handler.postDelayed(this, ENERGY_REFRESH_MILLIS)
            }
        }, getNextEnergyRefreshTime())
    }
    
    private fun getNextEnergyRefreshTime(): Long {
        val lastUpdate = prefs.getLong(KEY_LAST_ENERGY_UPDATE, 0L)
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastUpdate
        return maxOf(0L, ENERGY_REFRESH_MILLIS - timeSinceLastUpdate)
    }
    
    fun getCurrentEnergy(): Int {
        return prefs.getInt(KEY_CURRENT_ENERGY, MAX_ENERGY)
    }
    
    private fun setCurrentEnergy(energy: Int) {
        val previousEnergy = prefs.getInt(KEY_CURRENT_ENERGY, MAX_ENERGY)
        prefs.edit().putInt(KEY_CURRENT_ENERGY, energy).apply()
        energyUpdateCallback?.invoke(energy)
        
        // Enerji arttıysa bildirim göster
        if (energy > previousEnergy && energy <= MAX_ENERGY) {
            showEnergyNotification(energy)
        }
    }
    
    private fun setLastEnergyUpdate(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_ENERGY_UPDATE, timestamp).apply()
    }
    
    fun addEnergy(amount: Int) {
        val currentEnergy = getCurrentEnergy()
        val newEnergy = minOf(currentEnergy + amount, MAX_ENERGY)
        setCurrentEnergy(newEnergy)
        setLastEnergyUpdate(System.currentTimeMillis())
    }
    
    fun useEnergy(amount: Int = 1): Boolean {
        val currentEnergy = getCurrentEnergy()
        if (currentEnergy >= amount) {
            setCurrentEnergy(currentEnergy - amount)
            return true
        }
        return false
    }
    
    fun hasEnoughEnergy(amount: Int = 1): Boolean {
        return getCurrentEnergy() >= amount
    }
    
    fun getMaxEnergy(): Int = MAX_ENERGY
    
    fun getEnergyRefreshMinutes(): Int = ENERGY_REFRESH_MINUTES
    
    fun setEnergyUpdateCallback(callback: (Int) -> Unit) {
        energyUpdateCallback = callback
    }
    
    fun getTimeUntilNextEnergy(): Long {
        val lastUpdate = prefs.getLong(KEY_LAST_ENERGY_UPDATE, 0L)
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastUpdate
        return maxOf(0L, ENERGY_REFRESH_MILLIS - timeSinceLastUpdate)
    }
    
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }
    
    private fun showEnergyNotification(energy: Int) {
        // Basit bir Toast mesajı göster
        android.widget.Toast.makeText(
            context,
            "Enerji yenilendi! Şu anki enerji: $energy/$MAX_ENERGY",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
} 