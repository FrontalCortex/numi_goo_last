import Foundation

// Giriş akışının kimlik dışı iki dış bağımlılığı: bildirim izni ve zorunlu güncelleme denetimi.
// İkisi de şimdilik protokol arkasında sahte uygulamayla çalışır (GELISTIRME.md: ağ ve sistem
// çağrıları protokol arkasında).

// MARK: - Bildirim izni (hatırlatma sorusu)

/// Bildirim izni isteğinin sonucu (Android ölçüm değerleri: AnalyticsLogger.NOTIF_PERMISSION_*).
enum GirisBildirimIzniSonucu {
    /// İzin penceresi gösterildi, kullanıcı izin verdi.
    case verildi
    /// İzin penceresi gösterildi, kullanıcı reddetti.
    case reddedildi
    /// İzin zaten vardı; pencere gösterilmedi.
    case gerekmedi
}

/// Hatırlatma sorusundaki "Evet, hatırlat"tan sonra bildirim izni.
/// Android: UserInfoFragment.answerReminder → Android 13+ POST_NOTIFICATIONS izin penceresi
/// (`registerForActivityResult(RequestPermission)`). Sonuç ne olursa olsun kayda devam edilir:
/// izin vermemek kayıt olmaya engel değil.
@MainActor
protocol GirisBildirimServisi: AnyObject {
    /// Gerçek uygulamada: `UNUserNotificationCenter.current().getNotificationSettings` →
    /// `.notDetermined` ise `requestAuthorization(options: [.alert, .sound, .badge])`, zaten
    /// izinliyse `.gerekmedi`. Yalnızca "Evet, hatırlat" seçilince çağrılır.
    func izinIste() async -> GirisBildirimIzniSonucu
}

/// Sahte bildirim servisi: sistem izin penceresini AÇMAZ, verilen sonucu döndürür.
/// YAPILACAK (APNs bağlanırken): gerçek `UNUserNotificationCenter` uygulaması yazılacak; izin
/// istemi şimdilik bilerek çağrılmıyor.
@MainActor
final class SahteGirisBildirimServisi: GirisBildirimServisi {
    var sonuc: GirisBildirimIzniSonucu
    /// Kaç kez izin istendi (denemeler için).
    private(set) var istekSayisi: Int = 0

    nonisolated init(sonuc: GirisBildirimIzniSonucu = .verildi) {
        self.sonuc = sonuc
    }

    func izinIste() async -> GirisBildirimIzniSonucu {
        istekSayisi += 1
        return sonuc
    }
}

// MARK: - Zorunlu güncelleme (açılış)

/// Bu sürüm uzaktan kilitlendi mi. Android: ForceUpdateManager (Remote Config
/// `min_version_code` > bu derlemenin versionCode'u ise kapatılamayan "Güncelleme gerekli"
/// penceresi). Açılış animasyonuyla paralel çekilir; açılış bittikten sonra en fazla
/// SplashRules.FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS beklenir, süre dolarsa bir önceki
/// açılışta etkinleşen değerle karar verilir.
@MainActor
protocol GirisSurumServisi: AnyObject {
    /// Arka planda çekmeyi başlatır (Android: ForceUpdateManager.startFetch).
    func cekmeyiBaslat()

    /// Çekme bitene ya da `azamiBeklemeMs` dolana kadar bekler; kilitliyse true
    /// (Android: runIfAllowed → isBlocked).
    func kilitliMi(azamiBeklemeMs: Int64) async -> Bool
}

/// Sahte sürüm servisi: Remote Config yok, hiçbir sürüm kilitli değil.
@MainActor
final class SahteGirisSurumServisi: GirisSurumServisi {
    var kilitli: Bool

    nonisolated init(kilitli: Bool = false) {
        self.kilitli = kilitli
    }

    func cekmeyiBaslat() {}

    func kilitliMi(azamiBeklemeMs: Int64) async -> Bool {
        return kilitli
    }
}
