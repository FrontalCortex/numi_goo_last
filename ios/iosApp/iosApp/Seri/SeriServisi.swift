import Foundation
import Shared

// Günlük serinin (streak) dış dünyayla konuştuğu her şey bu protokolün arkasındadır.
//
// Android karşılıkları:
//   StreakRepository (yerel önbellek, SharedPreferences "streak_prefs")  → ortak kod `StreakRepository`
//   StudyTimeTracker (ders ekranlarında geçen süre, "study_time")        → ortak kod `StudyTimeTracker`
//   StreakSyncService (Firestore okuma + Cloud Functions)               → bu protokolün gerçek uygulaması
//
// Firebase henüz bağlı değil: `SahteSeriServisi` sunucuyu ortak koddaki `StreakServerModel` ile taklit
// eder (functions/index.js kurallarının kopyası). Gerçek uygulama:
//   - okuma: Firestore `users/{uid}/streak/state` → `StreakServerState.companion.fromDocument` →
//     `StreakSync.shared.onDocumentRead`
//   - gün bildirimi: callable `submitStreakDay` (gövde `StreakSync.shared.buildSubmitRequest(...).payload()`)
//     → `StreakServerState.companion.fromFunctionResult` → `StreakSync.shared.onSubmitSucceeded`;
//     hata olursa 60 sn geri çekilme (Android RETRY_BACKOFF_MS), aynı anda tek gönderim.
//   - dondurma: callable `buyStreakFreeze` { today } → `StreakSync.shared.onFreezeBought`; altın sunucuda düşer.
//   - ödül: callable `claimStreakReward` { milestone } ya da { kind: "challenge" } → `onMilestoneClaimed` /
//     `onChallengeClaimed`; anahtar/altın sunucuda eklenir.
// Mantık Swift'te yeniden yazılmaz; bütün kararlar ortak koddadır.

/// Ödül toplama ya da satın almanın sonucu (Android: onResult(success, message, keys, currency)).
struct SeriIslemSonucu {
    let basarili: Bool
    /// Kullanıcıya gösterilecek metin (Android: Toast).
    let mesaj: String
}

protocol SeriServisi: AnyObject {
    /// Android: StreakRepository.refresh — seriyi güncel süreye göre tazeler (gün burada tamamlanır).
    func durum() -> StreakState

    /// Seri ekranının bütün kararları (StreakFragment.render) ve hafta şeridi.
    func ekranVerisi() -> SeriEkranVerisi

    /// Android: StreakRepository.bindToUser + applyPendingSignup (MainActivity.refreshStreakUi).
    /// - Parameters:
    ///   - uid: Oturumdaki hesap; yoksa nil (misafir: süre birikir, gün sayılmaz).
    ///   - hesapAcilisMs: Hesabın açılış zamanı (Android: FirebaseUser.metadata.creationTimestamp).
    ///   - bekleyenKayit: Kayıt sorularının bekleyen cevapları (GirisYerelDepo); varsa uygulanır ya da atılır.
    /// - Returns: Bekleyen cevaplar için verilen karar.
    @discardableResult
    func hesabaBagla(uid: String?, hesapAcilisMs: Int64?, bekleyenKayit: StreakPendingSignup?) -> StreakSignupResult

    /// Android: NumiGooApplication → StudyTimeTracker.setActiveScreen. `ad`: Android fragment adı
    /// ("AbacusFragment", "BlindingLessonFragment", "TutorialFragment"); ders ekranı değilse nil.
    func calismaEkrani(_ ad: String?)

    /// Android: StreakSyncService.syncPendingDays + refreshFromServer. Bir şey değiştiyse dinleyiciler çağrılır.
    func esitle(tamam: @escaping () -> Void)

    var hedefDakika: Int { get }
    /// Android: StreakFragment.showGoalPicker → StreakRepository.setGoalMinutes.
    func hedefiDegistir(_ dakika: Int)

    var hatirlatmaSaati: Int { get }
    /// Android: StreakFragment.applyReminderHour → setReminderHour + syncPendingDays (saat sunucuya gider).
    func hatirlatmaSaatiniDegistir(_ saat: Int)

    /// Android: StreakRepository.needsNewStreakPrompt.
    func yeniSeriSorusuGerekli() -> Bool
    /// Android: StreakRepository.markNewStreakPromptShown (ekran AÇILDIĞINDA).
    func yeniSeriSorusuGosterildi()
    /// Android: NewStreakFragment.finishWithPick → startNewChallenge + syncPendingDays.
    func yeniMeydanOkuma(_ gun: Int)

    /// Bekleyen kutlamanın seri değerini alır ve siler; yoksa 0 (pendingCelebration + clearPendingCelebration).
    func kutlamayiAl() -> Int
    /// Android: StreakRepository.takeFreezeNotice.
    func dondurmaBildiriminiAl() -> StreakFreezeNotice?

    /// Android: StreakSyncService.claimReward (`claimStreakReward` { milestone }).
    func kilometreTasiTopla(_ tas: Int, tamam: @escaping (SeriIslemSonucu) -> Void)
    /// Android: StreakSyncService.claimChallengeReward (`claimStreakReward` { kind: "challenge" }).
    func meydanOkumaOdulunuTopla(tamam: @escaping (SeriIslemSonucu) -> Void)

    /// Profil kartındaki en uzun seri (Android: `users/{uid}.longestStreak`, sunucu yazar).
    var profilEnUzunSeri: Int { get }

    /// Seri durumu değişince (eşitleme, ödül, dondurma) çağrılır. Dönen kimlikle bırakılır.
    func dinle(_ dinleyici: @escaping () -> Void) -> UUID
    func dinlemeyiBirak(_ kimlik: UUID)
}

/// Hatırlatma bildirimi. Android'de bildirimi SUNUCU gönderir (FCM; functions/index.js →
/// sendStreakReminders, kullanıcının `reminderHourUtc` alanına göre saatte bir tarama); istemci yalnızca
/// saati `submitStreakDay` ile iletir ve Android 13+'ta bildirim iznini ister. iOS'ta APNs ya da yerel
/// bildirim (UNUserNotificationCenter) bağlanınca bu protokol doldurulacak.
protocol SeriHatirlatmaServisi: AnyObject {
    /// Saat seçildi (yerel, tam saat).
    func saatAyarlandi(_ saat: Int)
    /// Bildirim izni (Android: POST_NOTIFICATIONS). Sonuç ne olursa olsun saat kayıtlıdır.
    func izinIste(tamam: @escaping (Bool) -> Void)
}

/// Sistem çağrısı YOK: `UNUserNotificationCenter` henüz bağlanmadı (bildirim izni ve zamanlama yapılmaz).
final class SahteSeriHatirlatmaServisi: SeriHatirlatmaServisi {
    private(set) var sonSaat: Int? = nil

    func saatAyarlandi(_ saat: Int) {
        // GERÇEK UYGULAMA: UNUserNotificationCenter / APNs burada (şimdilik yalnızca bellekte tutulur).
        sonSaat = saat
    }

    func izinIste(tamam: @escaping (Bool) -> Void) {
        // GERÇEK UYGULAMA: UNUserNotificationCenter.requestAuthorization (şimdilik sistem penceresi açılmaz).
        DispatchQueue.main.async { tamam(false) }
    }
}

/// Seri ekranının girdisi: ortak koddan gelen kararlar.
struct SeriEkranVerisi {
    let ui: StreakScreenUi
    let hafta: [StreakWeekDayUi]
}

/// Uygulamanın tek seri kaynağı (üst çubuk, seri ekranı, mağaza kartı, profil aynı nesneyi okur).
/// Mağazanın sahte bağımlılıkları (`MagazaBagimliliklar.sahte.seri`) bu nesneyi kurar.
enum SeriOrtak {
    static let servis: SahteSeriServisi = {
        if let mevcut = MagazaBagimliliklar.sahte.seri as? SahteSeriServisi {
            return mevcut
        }
        // Olmaması gerekir (MagazaServisler.swift seri servisini burada kurar); yedek.
        return SahteSeriServisi(cuzdan: MagazaSahteCuzdanServisi())
    }()
}

/// Ortak kodun verdiği metin kararlarını biçimler.
enum SeriMetin {
    /// `StreakText`: anahtar + tamsayı argümanları.
    static func yaz(_ metin: StreakText) -> String {
        let argumanlar: [CVarArg] = metin.args.map { (deger: KotlinInt) -> CVarArg in
            return Int(deger.int32Value)
        }
        if argumanlar.isEmpty {
            return Metin.al(metin.key)
        }
        return Metin.bicimle(Metin.al(metin.key), argumanlar)
    }

    /// Android: appPlural(R.plurals.ui_x_gun, n, n) → "N gün".
    static func gun(_ adet: Int) -> String {
        return Metin.cogul("ui_x_gun", adet, adet)
    }

    /// Hata metni (StreakSyncService.callErrorMessage).
    static func hata(_ hata: StreakErrorText) -> String {
        if let hazir = hata.literal {
            return hazir
        }
        let anahtar: String = hata.key ?? "ui_x_birazdan_tekrar_dene"
        if let ic = hata.argKey {
            return Metin.al(anahtar, Metin.al(ic))
        }
        return Metin.al(anahtar)
    }

    /// Seçenek satırının sol metni.
    static func sol(_ satir: StreakOptionRowUi) -> String {
        if let hazir = satir.labelLiteral {
            return hazir
        }
        let anahtar: String = satir.labelKey ?? ""
        if satir.labelPluralCount >= 0 {
            let adet: Int = Int(satir.labelPluralCount)
            return Metin.cogul(anahtar, adet, adet)
        }
        return Metin.al(anahtar)
    }

    /// Seçenek satırının sağ metni.
    static func sag(_ satir: StreakOptionRowUi) -> String {
        if let hazir = satir.trailingLiteral {
            return hazir
        }
        let anahtar: String = satir.trailingKey ?? ""
        let arguman: Int = Int(satir.trailingArg)
        if satir.trailingPlural {
            return Metin.cogul(anahtar, arguman, arguman)
        }
        if arguman >= 0 {
            return Metin.al(anahtar, arguman)
        }
        return Metin.al(anahtar)
    }
}
