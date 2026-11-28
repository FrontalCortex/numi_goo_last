import Foundation

// Kullanıcı belgesi servisi: Firestore `users/{uid}` okuma/yazmasının Swift tarafındaki sözleşmesi.
// Alan adları ve varsayılanlar ortak koddadır (ios/shared/.../model/AuthRules.kt,
// oyun/UserWalletFirestore.kt, model/SessionDeviceRules.kt); burada yeniden yazılmaz.
//
// ── Android'de kayıt sırasında yazılan belge ──────────────────────────────────────────────
// Koleksiyon: `users`, belge kimliği: Firebase Auth uid. Üç ayrı yazım yolu vardır:
//
// 1) KODLA KAYIT (öğrenci ve öğretmen; bugünkü arayüzün kullandığı yol) — belgeyi SUNUCU yazar:
//    Cloud Function `verifyRegistrationCode` (functions/index.js). İstemci yalnızca çağrı
//    gövdesini yollar (AuthRules.verifyRegistrationCodePayload: email, code, [name], [password],
//    role, [birthYear], [acquisitionSource]).
//      uid                  = Auth uid
//      userId               = 6 rakam (100000–999999) + 2 büyük harf, sunucuda üretilir
//      email                = normalleştirilmiş e-posta
//      name                 = gönderilen ad; boşsa userId
//      role                 = "STUDENT" | "TEACHER"
//      first_tutorial_shown = false
//      createdAt            = sunucu zamanı (Timestamp)
//      keys                 = 1
//      currency             = 0
//      birthYear            = yalnızca 1900 < yıl <= bu yıl ise (istemci yazımına KAPALI alan)
//      acquisitionSource    = yalnızca doluysa (en çok 64 karakter)
//      verified             = true            (yalnızca STUDENT)
//      teacherApproved      = false           (yalnızca TEACHER)
//
// 2) GOOGLE İLE OTOMATİK KAYIT (kayıt ekranındaki Google düğmesi) — belgeyi İSTEMCİ yazar:
//    AuthManager.signInWithGoogle(autoRegister = true) → AuthRules.googleStudentUserData.
//      uid, userId (istemcide üretilir; benzersizlik `users` içinde `userId ==` sorgusuyla,
//      en çok 15 deneme, sonra zaman damgalı yedek), email (hesabın e-postası ya da ""),
//      name (hesabın görünen adı ya da ""), role = "STUDENT", verified = true,
//      first_tutorial_shown = yereldeki bayrak (AppPrefs "first_tutorial_shown", varsayılan false),
//      createdAt = şimdi, [birthYear], [acquisitionSource],
//      keys = 1, currency = 0 (UserWalletFirestore.registrationWalletFields).
//    DİKKAT: kod 1. yolda `birthYear` alanının istemci yazımına kapalı olduğunu söylüyor
//    (firestore.rules); bu yolda ise istemci yazıyor. Kurallar reddederse kayıt başarısız olur —
//    Firebase bağlanırken denenmeli. Apple ile kayıt da bu yolu izleyecek.
//
// 2b) 13 YAŞ ALTI (veli onayı) — belgeyi her iki yolda da SUNUCU yazar; kurallar
//    (`noUnconsentedChild`) bu yaşla istemci yazımını reddeder. E-posta yolunda veli kodu
//    `verifyRegistrationCode` ile girilir (doğum yılı sunucudaki veli kaydından alınır); Google /
//    Apple yolunda `confirmParentalConsent` belgeyi googleStudentUserData alanlarıyla açar.
//    Bkz. KimlikServisi.veliKayitKoduGonder / saglayiciVeliOnayiniDogrula.
//
// 3) E-POSTA/ŞİFRE İLE KAYIT (AuthManager.registerStudent / registerTeacher) — arayüzde
//    kullanılmıyor; AuthRules.emailStudentUserData (verified = false) ve AuthRules.teacherUserData
//    (verified alanı yok) ile istemci yazar.
//
// Kayıttan SONRA aynı belgeye eklenen alanlar (bu klasörün kapsamı dışındakiler hariç):
//      activeDeviceId, activeSessionUpdatedAt   — tek cihaz oturumu (SessionDeviceRules)
//      skip_step_tutorial_shown                 — AuthRules.FIELD_SKIP_STEP_TUTORIAL_SHOWN
//      fcmDevices, fcmToken, fcmTokenUpdatedAt  — bildirim belirteci (FcmRules)
@MainActor
protocol KullaniciKayitServisi: AnyObject {
    /// `users/{uid}` belgesini okur; belge yoksa nil. Zaman alanları epoch ms (Int64) olarak verilir.
    func belgeOku(uid: String) async throws -> [String: Any]?

    /// `users` koleksiyonunda `userId == kullaniciKimligi` olan belge var mı
    /// (Android: generateUniqueUserId içindeki `whereEqualTo("userId", …).limit(1)` sorgusu).
    func kullaniciKimligiKullaniliyorMu(_ kullaniciKimligi: String) async throws -> Bool

    /// `users/{uid}` belgesini oluşturur (Android: `.set(userData)`). `alanlar` ortak koddaki
    /// AuthRules.*UserData fonksiyonlarından gelir; `createdAt` epoch ms'dir, Firestore'a
    /// yazarken Timestamp'e çevrilmelidir.
    func belgeOlustur(uid: String, alanlar: [String: Any]) async throws

    /// `users/{uid}` belgesinin verilen alanlarını günceller (Android: `.update(...)`).
    /// NSNull değeri alanı null yapar (SessionDeviceRules.releaseUpdate).
    func alanlariGuncelle(uid: String, alanlar: [String: Any]) async throws
}

/// Bellekte çalışan sahte belge deposu.
@MainActor
final class SahteKullaniciKayitServisi: KullaniciKayitServisi {
    /// true iken her çağrı hata fırlatır (Firestore'a ulaşılamıyor durumu).
    var hataUret: Bool

    /// Çağrı başına bekleme (saniye).
    var gecikmeSaniye: Double

    private var belgeler: [String: [String: Any]]

    nonisolated init(gecikmeSaniye: Double = 0.2) {
        self.hataUret = false
        self.gecikmeSaniye = gecikmeSaniye
        self.belgeler = [:]
    }

    func belgeOku(uid: String) async throws -> [String: Any]? {
        try await bekle()
        return belgeler[uid]
    }

    func kullaniciKimligiKullaniliyorMu(_ kullaniciKimligi: String) async throws -> Bool {
        try await bekle()
        for belge in belgeler.values {
            if let kimlik = belge["userId"] as? String, kimlik == kullaniciKimligi {
                return true
            }
        }
        return false
    }

    func belgeOlustur(uid: String, alanlar: [String: Any]) async throws {
        try await bekle()
        belgeler[uid] = alanlar
    }

    func alanlariGuncelle(uid: String, alanlar: [String: Any]) async throws {
        try await bekle()
        guard var belge = belgeler[uid] else {
            // Firestore `update` olmayan belgede hata verir.
            throw GirisHatasi("NOT_FOUND")
        }
        for (anahtar, deger) in alanlar {
            if deger is NSNull {
                belge.removeValue(forKey: anahtar)
            } else {
                belge[anahtar] = deger
            }
        }
        belgeler[uid] = belge
    }

    /// Belgeyi beklemeden okur (yalnızca sahte kimlik servisi ve denemeler için).
    func anlikBelge(uid: String) -> [String: Any]? {
        return belgeler[uid]
    }

    /// E-postası verilen ilk belgenin (uid, alanlar) çifti; sunucudaki `findUserByEmail` karşılığı.
    func epostaIleBul(_ eposta: String) -> (uid: String, alanlar: [String: Any])? {
        for (uid, belge) in belgeler {
            if let kayitli = belge["email"] as? String, kayitli == eposta {
                return (uid, belge)
            }
        }
        return nil
    }

    /// Belgeyi beklemeden yazar (örnek hesapları yüklemek için).
    func anlikYaz(uid: String, alanlar: [String: Any]) {
        belgeler[uid] = alanlar
    }

    /// Belgeyi beklemeden siler (sahte hesap silme; Android: AccountDeletionHelper `users/{uid}` silinir).
    func anlikSil(uid: String) {
        belgeler[uid] = nil
    }

    private func bekle() async throws {
        if gecikmeSaniye > 0 {
            let nanosaniye = UInt64(gecikmeSaniye * 1_000_000_000)
            try? await Task.sleep(nanoseconds: nanosaniye)
        }
        if hataUret {
            throw GirisHatasi("UNAVAILABLE")
        }
    }
}
