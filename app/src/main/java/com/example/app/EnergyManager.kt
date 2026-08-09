package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Profesyonel "Hedef Zaman" mimarisi.
 *
 * Firestore'da tuttuğumuz tek veri: energy_full_time
 *   → enerjinin 5/5 olacağı Unix timestamp (ms)
 *
 * Mevcut enerji hesabı: floor((fullTime - now) / REFRESH) çıkarılarak MAX'tan bakılır.
 * Arkada sürekli yazan zamanlayıcı yoktur; yalnızca UI yenilemesi yapılır.
 * Enerji harcandığında fullTime uzatılır ve Firestore'a tek yazma yapılır.
 */
class EnergyManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "energy_prefs"
        private const val KEY_USER_PLAN = "user_plan"

        private const val KEY_ENERGY_FULL_TIME = "energy_full_time"
        private const val FIELD_ENERGY_FULL_TIME = "energy_full_time"

        // Eski alanlara geçiş (migration) için
        private const val LEGACY_KEY_CURRENT_ENERGY = "current_energy"
        private const val LEGACY_KEY_LAST_ENERGY_UPDATE = "last_energy_update"
        private const val LEGACY_FIELD_ENERGY = "energy"
        private const val LEGACY_FIELD_LAST_UPDATE = "last_energy_update"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var energyUpdateCallback: ((Int) -> Unit)? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * UI timer — sadece bir sonraki tick'te callback'i çalıştırır.
     * Veritabanına ASLA yazmaz.
     */
    private val timerRunnable = object : Runnable {
        override fun run() {
            energyUpdateCallback?.invoke(getCurrentEnergy())
            scheduleNextTick()
        }
    }

    init {
        // Eğer bu cihazda Firestore henüz senkronize edilmediyse,
        // eski yerel veriden geçiş yap ya da boş başlat (FS sync gelene kadar).
        migrateLocalIfNeeded()

        // Firestore'dan gerçek değeri çek (cihazlar arası senkron için kritik)
        syncFromFirestore()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Genel API
    // ─────────────────────────────────────────────────────────────────────────

    fun getUserPlan(): String = prefs.getString(KEY_USER_PLAN, "Free") ?: "Free"

    fun setUserPlan(plan: String) {
        prefs.edit().putString(KEY_USER_PLAN, plan).apply()
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    fun getMaxEnergy(): Int {
        return if (getUserPlan() == "Lite") 10 else 5
    }

    fun getEnergyRefreshMinutes(): Int {
        return if (getUserPlan() == "Lite") 10 else 15
    }

    fun getEnergyRefreshMillis(): Long {
        return getEnergyRefreshMinutes() * 60 * 1000L
    }

    /**
     * Matematiksel hesap: fullTime'dan şimdiye kadar kaç 30 saniyelik dilim geçmiş?
     * Geçen dilim sayısı = şimdiye kadar dolmuş enerji miktarı.
     *
     * Örnek: energy = 3, missing = 2 → fullTime = now + 60s
     *   t = now+10s: timeUntilFull=50s, missing = floor(50/30)=1 → energy=4  ✗ YANLIŞ
     *   t = now+30s: timeUntilFull=30s, missing = floor(30/30)=1 → energy=4  ✓
     *   t = now+60s: fullTime<=now → energy=5 ✓
     *
     * Doğru formül: missing = ceil(timeUntilFull / REFRESH) → floor yöntemi kullan.
     *   missing enerji = kaç 30 saniyelik dilim gerekiyor → floor değil, GERÇEK bölme.
     *   timeUntilFull = 30s → missing = 1 (bir dilim dolmamış)
     *   timeUntilFull = 1ms → missing = 1 (henüz dolmamış)
     *   timeUntilFull = 60s → missing = 2
     */
    fun getCurrentEnergy(): Int {
        val now = System.currentTimeMillis()
        val fullTime = getFullTime()

        if (fullTime <= now) return getMaxEnergy()

        val timeUntilFull = fullTime - now
        // Kaç slot hâlâ dolmamış? Her slot = ENERGY_REFRESH_MILLIS
        // Slot sayısı: timeUntilFull / REFRESH → TAM SAYIYA YUVARLANMIŞ (ceiling)
        val missingSlots = ((timeUntilFull + getEnergyRefreshMillis() - 1) / getEnergyRefreshMillis()).toInt()
        return maxOf(0, getMaxEnergy() - missingSlots)
    }

    /**
     * Enerji harca. Başarılı olursa true döner, yetmezse false.
     * fullTime'ı uzatır ve Firestore'a yazar.
     */
    fun useEnergy(amount: Int = 1): Boolean {
        if (amount <= 0) return true

        val currentEnergy = getCurrentEnergy()
        if (currentEnergy < amount) return false

        val now = System.currentTimeMillis()
        val currentFullTime = getFullTime()

        // Eğer enerji zaten dolu (fullTime geçmişte), sayacı şimdiden başlat.
        val baseTime = maxOf(currentFullTime, now)
        val newFullTime = baseTime + amount * getEnergyRefreshMillis()

        persistFullTime(newFullTime)
        return true
    }

    /**
     * Enerji ekle (örn. reklam sonrası bonus, refill).
     */
    fun addEnergy(amount: Int) {
        if (amount <= 0) return

        val now = System.currentTimeMillis()
        val currentFullTime = getFullTime()

        if (currentFullTime <= now) {
            // Zaten full ise eklenecek bir şey yok
            return
        }

        // Kazanılan her enerji, dolum hedefini ENERGY_REFRESH_MILLIS kadar geri (geçmişe) çeker.
        // Böylece kalan saniyeler/dakikalar sıfırlanmaz, korunur.
        val timeToSubtract = amount * getEnergyRefreshMillis()
        val newFullTime = maxOf(now, currentFullTime - timeToSubtract)

        persistFullTime(newFullTime)
    }

    fun hasEnoughEnergy(amount: Int = 1) = getCurrentEnergy() >= amount

    fun setEnergyUpdateCallback(callback: (Int) -> Unit) {
        energyUpdateCallback = callback
        // Callback ayarlandığında hemen UI'ı tetikle ve timer başlat
        callback(getCurrentEnergy())
        scheduleNextTick()
    }

    /** Bir sonraki enerji tick'ine kaç ms kaldığını döndürür (0 = zaten dolu). */
    fun getTimeUntilNextEnergy(): Long {
        val now = System.currentTimeMillis()
        val fullTime = getFullTime()

        if (fullTime <= now) return 0L

        val timeUntilFull = fullTime - now
        // Bir sonraki artış için kalan süre = timeUntilFull mod REFRESH
        // Eğer tam bölünüyorsa (tam sınırda) → REFRESH milisaniye daha bekle.
        val remainder = timeUntilFull % getEnergyRefreshMillis()
        return if (remainder == 0L) getEnergyRefreshMillis() else remainder
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // İç yardımcılar
    // ─────────────────────────────────────────────────────────────────────────

    private fun getFullTime(): Long {
        val saved = prefs.getLong(KEY_ENERGY_FULL_TIME, System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val maxAllowed = now + getMaxEnergy() * getEnergyRefreshMillis()
        if (saved > maxAllowed) {
            persistFullTime(maxAllowed)
            return maxAllowed
        }
        return saved
    }

    private fun setFullTimeLocally(timestamp: Long) {
        prefs.edit().putLong(KEY_ENERGY_FULL_TIME, timestamp).apply()
    }

    /** Hem yerel hem Firestore'a yazar; UI ve timer'ı günceller. */
    private fun persistFullTime(newFullTime: Long) {
        setFullTimeLocally(newFullTime)
        saveToFirestore(newFullTime)
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    private fun saveToFirestore(fullTime: Long) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .set(mapOf(FIELD_ENERGY_FULL_TIME to fullTime), SetOptions.merge())
    }

    private fun scheduleNextTick() {
        handler.removeCallbacks(timerRunnable)
        val timeToNext = getTimeUntilNextEnergy()
        if (timeToNext > 0) {
            handler.postDelayed(timerRunnable, timeToNext)
        }
    }

    /**
     * Firestore'dan gerçek değeri çeker ve yerel değerin üzerine yazar.
     * Bu, farklı cihazdan girişlerde senkronizasyonu sağlar.
     */
    private fun syncFromFirestore() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Kullanıcı dökümanı henüz oluşmamış → mevcut halimizi yaz
                    saveToFirestore(getFullTime())
                    scheduleNextTick()
                    return@addOnSuccessListener
                }

                val serverFullTime = doc.getLong(FIELD_ENERGY_FULL_TIME)
                if (serverFullTime != null) {
                    // Yeni sistem verisi var → direkt al, lokalin üzerine yaz
                    setFullTimeLocally(serverFullTime)
                    energyUpdateCallback?.invoke(getCurrentEnergy())
                    scheduleNextTick()
                } else {
                    // Eski sistem verisi (energy + last_update) → dönüştür
                    val serverEnergy = doc.getLong(LEGACY_FIELD_ENERGY)?.toInt()
                    val serverLastUpdate = doc.getLong(LEGACY_FIELD_LAST_UPDATE)

                    val derivedFullTime = deriveFullTimeFromLegacy(serverEnergy, serverLastUpdate)
                    setFullTimeLocally(derivedFullTime)
                    saveToFirestore(derivedFullTime) // Yeni formata geçir
                    energyUpdateCallback?.invoke(getCurrentEnergy())
                    scheduleNextTick()
                }
            }
            .addOnFailureListener {
                // Offline: yerel saate güvenmeye devam et, timer'ı yenile
                scheduleNextTick()
            }
    }

    /**
     * Eski sistem verisinden (energy int + last_update timestamp) yeni fullTime türetir.
     */
    private fun deriveFullTimeFromLegacy(legacyEnergy: Int?, legacyLastUpdate: Long?): Long {
        val now = System.currentTimeMillis()
        if (legacyEnergy == null) return now // Veri yoksa şimdi full

        val lastUpdate = legacyLastUpdate ?: now
        val elapsed = now - lastUpdate
        val gained = (elapsed / getEnergyRefreshMillis()).toInt()
        val energy = minOf(legacyEnergy + gained, getMaxEnergy())
        val missing = getMaxEnergy() - energy

        return if (missing == 0) now
        else {
            val remainder = elapsed % getEnergyRefreshMillis()
            val untilNextTick = getEnergyRefreshMillis() - remainder
            now + untilNextTick + (missing - 1) * getEnergyRefreshMillis()
        }
    }

    /**
     * Cihazda eski sistem verisi (SharedPreferences'ta energy ve last_update) varsa
     * yeni formata çevirir. Sadece bir kez çalışır.
     */
    private fun migrateLocalIfNeeded() {
        if (prefs.contains(KEY_ENERGY_FULL_TIME)) return // Zaten geçirilmiş

        val legacyLastUpdate = prefs.getLong(LEGACY_KEY_LAST_ENERGY_UPDATE, 0L)
        val legacyEnergy = prefs.getInt(LEGACY_KEY_CURRENT_ENERGY, -1)

        val derivedFullTime = if (legacyLastUpdate != 0L && legacyEnergy >= 0) {
            deriveFullTimeFromLegacy(legacyEnergy, legacyLastUpdate)
        } else {
            // Hiç yerel geçmiş yok → Firestore sync gelene kadar full göster.
            // syncFromFirestore() doğru değeri üzerine yazacak.
            System.currentTimeMillis()
        }

        setFullTimeLocally(derivedFullTime)
    }
}