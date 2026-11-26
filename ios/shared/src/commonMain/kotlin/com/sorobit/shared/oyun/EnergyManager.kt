// Kaynak: EnergyManager.kt (tamamı: plan/rol kuralları, enerji hesabı, harcama/ekleme, bir sonraki
// tık süresi, eski veriden geçiş, Firestore belgesinin benimsenmesi).
// Farklar:
//  - SharedPreferences ("energy_prefs") → [OyunKeyValueStore]; `System.currentTimeMillis()` → [nowMs].
//  - Handler zamanlayıcısı, Firestore okuması ve `spendEnergy` çağrısı → [EnergyPlatform] arayüzü.
//    Zamanlayıcı tetiklenince platform [onTimerTick] çağırır; Firestore okuması bitince
//    [onUserDocLoaded] / [onUserDocLoadFailed]; `spendEnergy` bitince [onSpendEnergyResult] /
//    [onSpendEnergyFailed].
//  - `android.util.Log` çağrısı kaldırıldı.
// Android'deki hâliyle korunan notlar:
//  - Kaynak dosyadaki bazı yorumlar "30 saniyelik dilim"den söz eder; gerçek yenilenme süresi
//    plana göre 10 dk (Lite: 7 dk) — hesap bu değerlerle yapılır.
//  - `addEnergy` Android'de `private` ve hiçbir yerden çağrılmıyor (ölü kod); eşleşme için taşındı.
//  - Enerji dolu görünürken Lite → Free geçişi gibi durumlarda `getFullTime` üst sınırı aşan değeri
//    kırpar ve yerelde kalıcılaştırır.
package com.sorobit.shared.oyun

/** EnergyManager'ın platforma bıraktığı işler (zamanlayıcı ve ağ). */
interface EnergyPlatform {
    /** `handler.removeCallbacks(timerRunnable)` — bekleyen tık varsa iptal eder. */
    fun cancelTick()

    /** `handler.postDelayed(timerRunnable, delayMs)` — süre dolunca [EnergyManager.onTimerTick] çağrılır. */
    fun scheduleTick(delayMs: Long)

    /** Oturum açık mı (`auth.currentUser != null`). */
    fun isSignedIn(): Boolean

    /**
     * `spendEnergy` callable'ını `{amount}` ile çağırır. Yanıt gelince
     * [EnergyManager.onSpendEnergyResult], hata olursa [EnergyManager.onSpendEnergyFailed] çağrılır.
     */
    fun callSpendEnergy(payload: Map<String, Any>)

    /**
     * `users/{uid}` belgesini okur. Oturum yoksa hiçbir şey yapmaz. Sonuçta
     * [EnergyManager.onUserDocLoaded] ya da [EnergyManager.onUserDocLoadFailed] çağrılır.
     */
    fun loadUserDoc()
}

/**
 * "Hedef Zaman" mimarisi.
 *
 * Firestore'da tutulan tek veri: energy_full_time
 *   → enerjinin tam dolacağı Unix zaman damgası (ms)
 *
 * Mevcut enerji: MAX − ceil((fullTime − now) / REFRESH).
 * Arkada sürekli yazan zamanlayıcı yoktur; yalnızca UI yenilemesi yapılır.
 * Enerji harcandığında fullTime uzatılır; sunucu yetkili değeri döndürür.
 */
class EnergyManager(
    /** Android: "energy_prefs" adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
    private val platform: EnergyPlatform,
    private val nowMs: () -> Long = ::oyunSimdiMs,
) {

    companion object {
        const val PREFS_NAME = "energy_prefs"
        const val KEY_USER_PLAN = "user_plan"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_TEACHER_APPROVED = "teacher_approved"

        const val KEY_ENERGY_FULL_TIME = "energy_full_time"

        /** Firestore `users/{uid}` alanı. İstemci yazımına kapalıdır. */
        const val FIELD_ENERGY_FULL_TIME = "energy_full_time"

        // Eski alanlardan geçiş için
        const val LEGACY_KEY_CURRENT_ENERGY = "current_energy"
        const val LEGACY_KEY_LAST_ENERGY_UPDATE = "last_energy_update"
        const val LEGACY_FIELD_ENERGY = "energy"
        const val LEGACY_FIELD_LAST_UPDATE = "last_energy_update"

        const val PLAN_FREE = "Free"
        const val PLAN_LITE = "Lite"
        const val PLAN_PRO = "Pro"
        const val PLAN_PREMIUM = "Premium"
        const val ROLE_TEACHER = "TEACHER"
    }

    private var energyUpdateCallback: ((Int) -> Unit)? = null

    init {
        // Bu cihazda Firestore henüz eşitlenmediyse eski yerel veriden geçiş yap ya da
        // dolu başlat (Firestore yanıtı gelene kadar).
        migrateLocalIfNeeded()

        // Firestore'dan gerçek değeri çek (cihazlar arası eşitleme için kritik)
        syncFromFirestore()
    }

    /** UI zamanlayıcısı tetiklendi: yalnızca geri çağrıyı çalıştırır, veritabanına ASLA yazmaz. */
    fun onTimerTick() {
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Genel API
    // ─────────────────────────────────────────────────────────────────────────

    fun getUserPlan(): String = prefs.getString(KEY_USER_PLAN) ?: PLAN_FREE

    fun setUserPlan(plan: String) {
        prefs.putString(KEY_USER_PLAN, plan)
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE) ?: ""

    fun isTeacherApproved(): Boolean = prefs.getBoolean(KEY_TEACHER_APPROVED, false)

    /**
     * Firestore'daki role/teacherApproved verisini yerel olarak günceller.
     * Onaysız öğretmen (role=TEACHER, teacherApproved=false) hesaplar için enerji her zaman 0'dır;
     * teacherApproved=true olan hesaplar için (plandan bağımsız) enerji sonsuzdur.
     */
    fun setUserRoleApproval(role: String, teacherApproved: Boolean) {
        prefs.putString(KEY_USER_ROLE, role)
        prefs.putBoolean(KEY_TEACHER_APPROVED, teacherApproved)
        energyUpdateCallback?.invoke(getCurrentEnergy())
        scheduleNextTick()
    }

    /** Onaylanmamış öğretmen hesabı: enerji her zaman 0, plan ne olursa olsun. */
    fun isEnergyBlocked(): Boolean = getUserRole() == ROLE_TEACHER && !isTeacherApproved()

    /** teacherApproved=true veya plan Pro/Premium ise enerji sonsuzdur (onaysız öğretmen hariç). */
    fun isInfiniteEnergy(): Boolean {
        if (isEnergyBlocked()) return false
        return isTeacherApproved() || getUserPlan() == PLAN_PRO || getUserPlan() == PLAN_PREMIUM
    }

    fun getMaxEnergy(): Int {
        return if (getUserPlan() == PLAN_LITE) 10 else 5
    }

    fun getEnergyRefreshMinutes(): Int {
        return if (getUserPlan() == PLAN_LITE) 7 else 10
    }

    fun getEnergyRefreshMillis(): Long {
        return getEnergyRefreshMinutes() * 60 * 1000L
    }

    /**
     * Eksik enerji = dolmasına kalan sürenin yenilenme dilimine bölümünün TAVANI.
     *   kalan = 1 ms      → 1 eksik (henüz dolmamış)
     *   kalan = 1 dilim   → 1 eksik
     *   kalan = 2 dilim   → 2 eksik
     */
    fun getCurrentEnergy(): Int {
        if (isEnergyBlocked()) return 0

        val now = nowMs()
        val fullTime = getFullTime()

        if (fullTime <= now) return getMaxEnergy()

        val timeUntilFull = fullTime - now
        val missingSlots = ((timeUntilFull + getEnergyRefreshMillis() - 1) / getEnergyRefreshMillis()).toInt()
        return maxOf(0, getMaxEnergy() - missingSlots)
    }

    /**
     * Enerji harca. Başarılı olursa true döner, yetmezse false.
     * fullTime'ı uzatır ve sunucuya bildirir.
     */
    fun useEnergy(amount: Int = 1): Boolean {
        if (amount <= 0) return true
        if (isEnergyBlocked()) return false
        if (isInfiniteEnergy()) return true

        val currentEnergy = getCurrentEnergy()
        if (currentEnergy < amount) return false

        val now = nowMs()
        val currentFullTime = getFullTime()

        // Enerji zaten doluysa (fullTime geçmişte), sayacı şimdiden başlat.
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
     * DİKKAT: Can kazanmanın tek meşru yolları sunucudadır: reklam için `claimAdEnergy`,
     * anahtar karşılığı için `buyEnergyWithKeys`. Bu metot Android'de `private`tir ve
     * kullanılmaz; yalnızca yerel değeri değiştirir (sunucu değeri bir sonraki eşitlemede ezer).
     */
    internal fun addEnergy(amount: Int) {
        if (amount <= 0) return
        if (isEnergyBlocked() || isInfiniteEnergy()) return

        val now = nowMs()
        val currentFullTime = getFullTime()

        if (currentFullTime <= now) {
            // Zaten doluysa eklenecek bir şey yok
            return
        }

        // Kazanılan her enerji, dolum hedefini bir yenilenme dilimi kadar geri (geçmişe) çeker.
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
        // Geri çağrı ayarlandığında hemen UI'ı tetikle ve zamanlayıcıyı başlat
        callback(getCurrentEnergy())
        scheduleNextTick()
    }

    /** Bir sonraki enerji tıkına kaç ms kaldığını döndürür (0 = zaten dolu). */
    fun getTimeUntilNextEnergy(): Long {
        val now = nowMs()
        val fullTime = getFullTime()

        if (fullTime <= now) return 0L

        val timeUntilFull = fullTime - now
        // Bir sonraki artış için kalan süre = timeUntilFull mod REFRESH
        // Tam bölünüyorsa (tam sınırda) → bir dilim daha bekle.
        val remainder = timeUntilFull % getEnergyRefreshMillis()
        return if (remainder == 0L) getEnergyRefreshMillis() else remainder
    }

    fun destroy() {
        platform.cancelTick()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // İç yardımcılar
    // ─────────────────────────────────────────────────────────────────────────

    private fun getFullTime(): Long {
        val saved = prefs.getLong(KEY_ENERGY_FULL_TIME, nowMs())
        val now = nowMs()
        val maxAllowed = now + getMaxEnergy() * getEnergyRefreshMillis()
        if (saved > maxAllowed) {
            persistFullTime(maxAllowed)
            return maxAllowed
        }
        return saved
    }

    private fun setFullTimeLocally(timestamp: Long) {
        prefs.putLong(KEY_ENERGY_FULL_TIME, timestamp)
    }

    /** Yalnızca YEREL yazar ve UI'ı günceller. Sunucu yazımı ayrı çağrıyla yapılır. */
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
        if (!platform.isSignedIn()) return
        platform.callSpendEnergy(ServerEnergy.spendEnergyPayload(amount))
    }

    /** `spendEnergy` başarıyla döndü: yanıtta `energyFullTime` varsa benimsenir. */
    fun onSpendEnergyResult(data: Any?) {
        if (data !is Map<*, *>) return
        ServerEnergy.readFullTime(data)?.let { adoptServerFullTime(it) }
    }

    /** Sunucu reddettiyse (ör. gerçekte can yokmuş) yerel iyimser değeri düzelt. */
    fun onSpendEnergyFailed() {
        syncFromFirestore()
    }

    private fun scheduleNextTick() {
        platform.cancelTick()
        val timeToNext = getTimeUntilNextEnergy()
        if (timeToNext > 0) {
            platform.scheduleTick(timeToNext)
        }
    }

    /**
     * Firestore'dan gerçek değeri çeker ve yerel değerin üzerine yazar.
     * Bu, farklı cihazdan girişlerde eşitlemeyi sağlar.
     */
    private fun syncFromFirestore() {
        if (!platform.isSignedIn()) return
        platform.loadUserDoc()
    }

    /**
     * `users/{uid}` belgesi okundu.
     * @param exists belge var mı
     * @param serverFullTime `energy_full_time` alanı (yoksa null)
     * @param legacyEnergy eski `energy` alanı
     * @param legacyLastUpdate eski `last_energy_update` alanı
     */
    fun onUserDocLoaded(exists: Boolean, serverFullTime: Long?, legacyEnergy: Int?, legacyLastUpdate: Long?) {
        if (!exists) {
            // Kullanıcı belgesi yoksa veya silinmişse yeni belge yaratma.
            scheduleNextTick()
            return
        }

        if (serverFullTime != null) {
            // Yeni sistem verisi var → doğrudan al, yerelin üzerine yaz
            setFullTimeLocally(serverFullTime)
            energyUpdateCallback?.invoke(getCurrentEnergy())
            scheduleNextTick()
        } else {
            // Eski sistem verisi (energy + last_update) → dönüştür
            val derivedFullTime = deriveFullTimeFromLegacy(legacyEnergy, legacyLastUpdate)
            // energy_full_time istemci yazımına kapalı; yerelde tut, sunucu tarafı ilk
            // spendEnergy/claimAdEnergy çağrısında yeni biçime geçecek.
            setFullTimeLocally(derivedFullTime)
            energyUpdateCallback?.invoke(getCurrentEnergy())
            scheduleNextTick()
        }
    }

    /** Çevrimdışı: yerel saate güvenmeye devam et, zamanlayıcıyı yenile. */
    fun onUserDocLoadFailed() {
        scheduleNextTick()
    }

    /**
     * Eski sistem verisinden (energy int + last_update zaman damgası) yeni fullTime türetir.
     */
    internal fun deriveFullTimeFromLegacy(legacyEnergy: Int?, legacyLastUpdate: Long?): Long {
        val now = nowMs()
        if (legacyEnergy == null) return now // Veri yoksa şimdi dolu

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
     * Cihazda eski sistem verisi (energy ve last_update) varsa yeni biçime çevirir.
     * Yalnızca bir kez çalışır.
     */
    private fun migrateLocalIfNeeded() {
        if (prefs.contains(KEY_ENERGY_FULL_TIME)) return // Zaten geçirilmiş

        val legacyLastUpdate = prefs.getLong(LEGACY_KEY_LAST_ENERGY_UPDATE, 0L)
        val legacyEnergy = prefs.getInt(LEGACY_KEY_CURRENT_ENERGY, -1)

        val derivedFullTime = if (legacyLastUpdate != 0L && legacyEnergy >= 0) {
            deriveFullTimeFromLegacy(legacyEnergy, legacyLastUpdate)
        } else {
            // Hiç yerel geçmiş yok → Firestore yanıtı gelene kadar dolu göster.
            nowMs()
        }

        setFullTimeLocally(derivedFullTime)
    }
}
