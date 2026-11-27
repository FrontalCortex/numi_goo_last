import Foundation
import Shared

// Giriş akışının cihazda sakladığı küçük değerler (Android: SharedPreferences).
// Android'de üç ayrı dosyadadır ("AppPrefs", "device_prefs", "otp_cooldown_prefs"); anahtar adları
// çakışmadığı için burada tek UserDefaults kullanılır. Anahtarlar ve hesaplar ortak koddan gelir.
final class GirisYerelDepo {
    private let depo: UserDefaults

    init(depo: UserDefaults = .standard) {
        self.depo = depo
    }

    static func simdiMs() -> Int64 {
        return Int64(Date().timeIntervalSince1970 * 1000.0)
    }

    private func uzunOku(_ anahtar: String) -> Int64 {
        let sayi = depo.object(forKey: anahtar) as? NSNumber
        return sayi?.int64Value ?? 0
    }

    private func uzunYaz(_ deger: Int64, _ anahtar: String) {
        depo.set(NSNumber(value: deger), forKey: anahtar)
    }

    // MARK: Giriş başlangıç ekranı gösterildi mi (LoginStartActivity.onCreate)

    var baslangicGosterildi: Bool {
        get { depo.bool(forKey: SplashRules.shared.PREF_LOGIN_START_EVER_SHOWN) }
        set { depo.set(newValue, forKey: SplashRules.shared.PREF_LOGIN_START_EVER_SHOWN) }
    }

    // MARK: Hesap geçmişi (DeviceAccountStore)

    /// "Bu cihazda daha önce hesap açıldı mı" (AppPrefs "account_ever_created"). İlk öğreticinin
    /// gösterilme şartı tek bu: açıldıysa oturum kapalı olsa bile öğretici gösterilmez.
    var hesapAcildi: Bool {
        get { depo.bool(forKey: SplashRules.shared.PREF_ACCOUNT_EVER_CREATED) }
        set { depo.set(newValue, forKey: SplashRules.shared.PREF_ACCOUNT_EVER_CREATED) }
    }

    /// Eski kurulumlardan devralınan bayrak (yalnızca okunur).
    var eskiOgreticiBayragi: Bool {
        return depo.bool(forKey: SplashRules.shared.PREF_LEGACY_FIRST_TUTORIAL_SHOWN)
    }

    /// Android: DeviceAccountStore.hasEverHadAccount — okuma yan etkilidir: oturum açıksa (ya da eski
    /// bayrak varsa) kalıcı bayrak yazılır.
    func hesapGecmisiVar(oturumAcik: Bool) -> Bool {
        let kurallar = SplashRules.shared
        let acildi: Bool = hesapAcildi
        let eski: Bool = eskiOgreticiBayragi
        if kurallar.shouldPersistAccountFlag(accountEverCreated: acildi, signedIn: oturumAcik, legacyFirstTutorialShown: eski) {
            hesapAcildi = true
        }
        return kurallar.hasEverHadAccount(accountEverCreated: acildi, signedIn: oturumAcik, legacyFirstTutorialShown: eski)
    }

    // MARK: Kayıt sorularının bekleyen cevapları
    //
    // Hesap henüz yokken verilen cevaplar cihazda bekler; giriş yapılınca hesap yeniyse uygulanır
    // (Android: StreakRepository.applyPendingSignup, SorobanLevel.applyFromUserDoc). Uygulama
    // tarafı bu klasörün kapsamı dışında (seri ve profil). Anahtarlar Android'deki ayrı prefs
    // dosyalarının ("streak_signup_pending", "soroban_level_pending") alan adlarıdır; çakışmasın
    // diye dosya adı ön ek olarak eklendi.

    private static let seriOnEki = "streak_signup_pending."
    private static let abakusOnEki = "soroban_level_pending."

    /// Android: StreakRepository.savePendingSignup.
    func seriCevaplariniKaydet(hedefDakika: Int, meydanGun: Int, hatirlatmaSaati: Int, simdiMs: Int64) {
        let on = GirisYerelDepo.seriOnEki
        depo.set(hedefDakika, forKey: on + "goal_minutes")
        depo.set(meydanGun, forKey: on + "challenge_days")
        depo.set(hatirlatmaSaati, forKey: on + "reminder_hour")
        uzunYaz(simdiMs, on + "saved_at")
    }

    /// Bekleyen seri cevapları; yoksa nil (Android: StreakRepository.applyPendingSignup'ın okuduğu
    /// "streak_signup_pending" alanları). Veli onayı işareti (markAccountCreated) iOS'ta yazılmıyor: boş.
    func seriCevaplari() -> StreakPendingSignup? {
        let on = GirisYerelDepo.seriOnEki
        let kayitMs: Int64 = uzunOku(on + "saved_at")
        if kayitMs == 0 {
            return nil
        }
        let hedef: Int = depo.integer(forKey: on + "goal_minutes")
        let meydan: Int = depo.integer(forKey: on + "challenge_days")
        let saat: Int = depo.object(forKey: on + "reminder_hour") == nil ? -1 : depo.integer(forKey: on + "reminder_hour")
        return StreakPendingSignup(
            goalMinutes: Int32(hedef),
            challengeDays: Int32(meydan),
            reminderHour: Int32(saat),
            savedAtMs: kayitMs,
            createdUid: nil,
            createdAtMs: 0
        )
    }

    /// Bekleyen seri cevaplarını siler (Android: applyPendingSignup → pending.edit().clear()).
    func seriCevaplariniSil() {
        let on = GirisYerelDepo.seriOnEki
        depo.removeObject(forKey: on + "goal_minutes")
        depo.removeObject(forKey: on + "challenge_days")
        depo.removeObject(forKey: on + "reminder_hour")
        depo.removeObject(forKey: on + "saved_at")
    }

    /// Android: SorobanLevel.savePending (SignupFlowRules.SOROBAN_LEVELS değerlerinden biri).
    func abakusSeviyesiniKaydet(_ seviye: String, simdiMs: Int64) {
        let on = GirisYerelDepo.abakusOnEki
        depo.set(seviye, forKey: on + "level")
        uzunYaz(simdiMs, on + "saved_at")
    }

    /// Android: MainActivity.markNotificationPermissionPrompted — ana ekranın ilk açılıştaki
    /// bağlamsız izin isteği kapatılır (kayıtta zaten soruldu ya da "şimdi değil" denildi).
    func bildirimIzniSoruldu() {
        depo.set(true, forKey: "notif_permission_prompted")
    }

    // MARK: Cihaz kimliği (SessionDeviceManager.getOrCreateDeviceId)

    func cihazKimligi() -> String {
        let anahtar: String = SessionDeviceRules.shared.KEY_DEVICE_ID
        let mevcut: String? = depo.string(forKey: anahtar)
        if !SessionDeviceRules.shared.needsNewDeviceId(existing: mevcut), let kimlik = mevcut {
            return kimlik
        }
        let yeni = UUID().uuidString.lowercased()
        depo.set(yeni, forKey: anahtar)
        return yeni
    }

    // MARK: "Kodu tekrar gönder" beklemesi (45 sn)

    /// OtpVerificationFragment.getResendCooldownRemainingMs
    func yenidenGondermeKalanMs(eposta: String, simdiMs: Int64) -> Int64 {
        let kurallar = OtpRules.shared
        return kurallar.getResendCooldownRemainingMs(
            endMs: uzunOku(kurallar.OTP_RESEND_COOLDOWN_END_MS),
            savedEmail: depo.string(forKey: kurallar.OTP_RESEND_COOLDOWN_EMAIL),
            forEmail: eposta,
            nowMs: simdiMs
        )
    }

    /// OtpVerificationFragment.startResendCooldownInPrefs
    func yenidenGondermeBaslat(eposta: String, simdiMs: Int64) {
        let kurallar = OtpRules.shared
        let cift = kurallar.startResendCooldown(email: eposta, nowMs: simdiMs)
        let bitis: Int64 = cift.first?.int64Value ?? simdiMs
        let kayitliEposta: String = (cift.second as String?) ?? eposta
        uzunYaz(bitis, kurallar.OTP_RESEND_COOLDOWN_END_MS)
        depo.set(kayitliEposta, forKey: kurallar.OTP_RESEND_COOLDOWN_EMAIL)
    }

    func yenidenGondermeTemizle() {
        let kurallar = OtpRules.shared
        depo.removeObject(forKey: kurallar.OTP_RESEND_COOLDOWN_END_MS)
        depo.removeObject(forKey: kurallar.OTP_RESEND_COOLDOWN_EMAIL)
    }

    // MARK: Yanlış deneme kilidi (15 dk)

    /// OtpVerificationFragment.getActiveWrongAttemptCooldown: kalan dakika; kilit yoksa nil.
    func yanlisDenemeKalanDakika(eposta: String, simdiMs: Int64) -> Int32? {
        let kurallar = OtpRules.shared
        let kalan = kurallar.getActiveWrongAttemptCooldown(
            endMs: uzunOku(kurallar.OTP_COOLDOWN_END_MS),
            savedEmail: depo.string(forKey: kurallar.OTP_COOLDOWN_EMAIL),
            forEmail: eposta,
            nowMs: simdiMs
        )
        return kalan?.int32Value
    }

    /// Kilidin bitiş zamanı (geri sayım metni için); kayıt yoksa 0.
    func yanlisDenemeBitisMs() -> Int64 {
        return uzunOku(OtpRules.shared.OTP_COOLDOWN_END_MS)
    }

    /// OtpVerificationFragment.startWrongAttemptCooldown'ın yazdığı değerler.
    func yanlisDenemeBaslat(eposta: String, simdiMs: Int64) {
        let kurallar = OtpRules.shared
        uzunYaz(kurallar.wrongAttemptCooldownEndMs(nowMs: simdiMs), kurallar.OTP_COOLDOWN_END_MS)
        depo.set(AuthRules.shared.normalizeEmail(email: eposta), forKey: kurallar.OTP_COOLDOWN_EMAIL)
    }

    func yanlisDenemeTemizle() {
        let kurallar = OtpRules.shared
        depo.removeObject(forKey: kurallar.OTP_COOLDOWN_END_MS)
        depo.removeObject(forKey: kurallar.OTP_COOLDOWN_EMAIL)
    }
}
