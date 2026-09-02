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
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_TEACHER_APPROVED = "teacher_approved"

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

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "") ?: ""

    fun isTeacherApproved(): Boolean = prefs.getBoolean(KEY_TEACHER_APPROVED, false)

    /**
     * Firestore'daki role/teacherApproved verisini yerel olarak günceller.
     * Onaysız öğretmen (role=TEACHER, teacherApproved=false) hesaplar için enerji her zaman 0'dır;
     * teacherApproved=true olan hesaplar için (plan'dan bağımsız) enerji sonsuzdur.
     */
    fun setUserRoleApproval(role: String, teacherApproved: Boolean) {
        prefs.edit()
            .putString(KEY_USER_ROLE, role)
            .putBoolean(KEY_TEACHER_APPROVED, teacherApproved)
            .apply()
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    /** Onaylanmamış öğretmen hesabı: enerji her zaman 0, plan ne olursa olsun. */
    fun isEnergyBlocked(): Boolean = getUserRole() == "TEACHER" && !isTeacherApproved()

    /** teacherApproved=true veya plan Pro/Premium ise enerji sonsuzdur (onaysız öğretmen hariç). */
    fun isInfiniteEnergy(): Boolean {
        if (isEnergyBlocked()) return false
        return isTeacherApproved() || getUserPlan() == "Pro" || getUserPlan() == "Premium"
    }

    fun getMaxEnergy(): Int {
        return if (getUserPlan() == "Lite") 10 else 5
    }

    fun getEnergyRefreshMinutes(): Int {
        return if (getUserPlan() == "Lite") 7 else 10
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
        if (isEnergyBlocked()) return 0

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
        if (isEnergyBlocked()) return false
        if (isInfiniteEnergy()) return true

        val currentEnergy = getCurrentEnergy()
        if (currentEnergy < amount) return false

        val now = System.currentTimeMillis()
        val currentFullTime = getFullTime()

        // Eğer enerji zaten dolu (fullTime geçmişte), sayacı şimdiden başlat.
        val baseTime = maxOf(currentFullTime, now)
        val newFullTime = baseTime + amount * getEnergyRefreshMillis()

        // İyimser yerel güncelleme (ders anında başlasın), ardından sunucu yetkili değeri yazar.
        persistFullTime(newFullTime)
        requestSpendOnServer(amount)
        return true
    }

    /**
     * Can ekler.
     *
     * DİKKAT: Bunu doğrudan çağırmayın. Can kazanmanın tek meşru yolları sunucudadır:
     * reklam için `claimAdEnergy` (AdMob SSV ile doğrulanır), anahtar karşılığı için
     * `buyEnergyWithKeys`. Bu metot yalnızca sunucudan dönen sonucu yerel olarak
     * yansıtmak için kullanılır; `energy_full_time` zaten istemci yazımına kapalıdır.
     */
    private fun addEnergy(amount: Int) {
        if (amount <= 0) return
        if (isEnergyBlocked() || isInfiniteEnergy()) return

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

    fun hasEnoughEnergy(amount: Int = 1): Boolean {
        if (isEnergyBlocked()) return false
        if (isInfiniteEnergy()) return true
        return getCurrentEnergy() >= amount
    }

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

    /** Yalnızca YEREL yazar ve UI'ı günceller. Sunucu yazımı ayrı CF çağrısıyla yapılır. */
    private fun persistFullTime(newFullTime: Long) {
        setFullTimeLocally(newFullTime)
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    /**
     * Sunucudan dönen yetkili can durumunu yerel olarak benimser.
     *
     * `energy_full_time` firestore.rules ile istemci yazımına KAPALIDIR; can yalnızca
     * spendEnergy / claimAdEnergy / buyEnergyWithKeys fonksiyonlarıyla değişir.
     */
    fun adoptServerFullTime(fullTime: Long) {
        setFullTimeLocally(fullTime)
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    /** Sunucuya can harcamasını bildirir; sunucu yetkili değeri döndürür. */
    private fun requestSpendOnServer(amount: Int) {
        if (auth.currentUser == null) return
        com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("spendEnergy")
            .call(hashMapOf("amount" to amount))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                (data["energyFullTime"] as? Number)?.let { adoptServerFullTime(it.toLong()) }
            }
            .addOnFailureListener { e ->
                // Sunucu reddettiyse (ör. gerçekte can yokmuş) yerel iyimser değeri düzelt.
                android.util.Log.w("EnergyManager", "spendEnergy başarısız", e)
                syncFromFirestore()
            }
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
                    // Kullanıcı dökümanı yoksa veya silinmişse yeni belge yaratma.
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
                    // energy_full_time artık istemci yazımına kapalı; yerelde tut, sunucu
                    // tarafı ilk spendEnergy/claimAdEnergy çağrısında yeni formata geçecek.
                    setFullTimeLocally(derivedFullTime)
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