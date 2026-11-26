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
