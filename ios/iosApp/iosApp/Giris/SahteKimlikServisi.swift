import Foundation
import Shared

// Bellekte çalışan sahte kimlik servisi. Ağ gecikmesini taklit eder ve hata durumlarını
// üretebilir. Akış sırası Android AuthManager'daki ile aynıdır; kararlar ortak koddan gelir.
//
// Hazır hesaplar (örnek veri):
//   ogrenci@sorobit.test   — kayıtlı öğrenci (kodla giriş)
//   ogretmen@sorobit.test  — kayıtlı öğretmen, şifre "123456"
// Geçerli doğrulama kodu her zaman `ayarlar.gecerliKod` ("123456").
//
// Ayar değiştirmeden denenebilen hata durumları (e-postanın @ öncesi kısmına göre):
//   "hata…"   → kod gönderimi sunucu hatası verir ("Doğrulama kodu gönderilemedi.")
//   "sessiz…" → kod gönderimi mesajsız hata verir (ekran "Kod gönderilemedi" gösterir)

/// Google / Apple hesap seçiminin sahte sonucu.
enum SahteSaglayiciSonucu {
    /// Hesap seçildi.
    case hesap(eposta: String, ad: String)
    /// Kullanıcı vazgeçti (sessiz kalınır).
    case iptal
    /// Sağlayıcı hata verdi; metin aynen gösterilir.
    case hata(String)
}

struct SahteKimlikAyarlari {
    /// Her çağrının bekleme süresi (saniye).
    var gecikmeSaniye: Double = 0.8
    /// Geçerli sayılan 6 haneli kod.
    var gecerliKod: String = "123456"
    /// true iken her çağrı ağ hatası verir.
    var agHatasi: Bool = false
    var googleSonucu: SahteSaglayiciSonucu = .hesap(eposta: "google.ogrenci@sorobit.test", ad: "Google Öğrenci")
    var appleSonucu: SahteSaglayiciSonucu = .hesap(eposta: "apple.ogrenci@sorobit.test", ad: "Apple Öğrenci")
    /// true iken tek cihaz denetimi "başka cihazda canlı oturum var" sonucunu verir.
    var baskaCihazdaOturum: Bool = false
    /// Uygulama açılışında oturumu açık sayılacak hesabın e-postası (yoksa oturum kapalı başlar).
    var acilistaOturumEposta: String? = nil
}

@MainActor
final class SahteKimlikServisi: KimlikServisi {
    var ayarlar: SahteKimlikAyarlari

    let kayit: SahteKullaniciKayitServisi

    private var oturum: GirisOturumu?
    private var sifreler: [String: String]
    /// Gönderilmiş kodun amacı (e-posta → amaç); sunucudaki `studentVerificationCodes` karşılığı.
    private var gonderilenKodlar: [String: String]
    private var yanlisDenemeler: [String: Int]
    private var hazirlandi: Bool

    nonisolated init(
        ayarlar: SahteKimlikAyarlari = SahteKimlikAyarlari(),
        kayit: SahteKullaniciKayitServisi = SahteKullaniciKayitServisi()
    ) {
        self.ayarlar = ayarlar
        self.kayit = kayit
        self.oturum = nil
        self.sifreler = [:]
        self.gonderilenKodlar = [:]
        self.yanlisDenemeler = [:]
        self.hazirlandi = false
    }

    // MARK: Örnek veri

    private func hazirla() {
        if hazirlandi { return }
        hazirlandi = true
        let kurallar = AuthRules.shared
        let simdi = GirisYerelDepo.simdiMs()
        let cuzdan: [String: Any] = UserWalletFirestore.shared.registrationWalletFields()

        var ogrenci: [String: Any] = kurallar.emailStudentUserData(
            uid: "sahte-ogrenci",
            userId: "100001AA",
            email: "ogrenci@sorobit.test",
            name: "Örnek Öğrenci",
            firstTutorialShown: true,
            nowMs: simdi,
            walletFields: cuzdan
        )
        ogrenci[kurallar.FIELD_VERIFIED] = true
        kayit.anlikYaz(uid: "sahte-ogrenci", alanlar: ogrenci)

        let ogretmen: [String: Any] = kurallar.teacherUserData(
            uid: "sahte-ogretmen",
            userId: "100002AB",
            email: "ogretmen@sorobit.test",
            name: "Örnek Öğretmen",
            firstTutorialShown: true,
            nowMs: simdi,
            walletFields: cuzdan
        )
        kayit.anlikYaz(uid: "sahte-ogretmen", alanlar: ogretmen)
        sifreler["ogretmen@sorobit.test"] = "123456"

        if let eposta = ayarlar.acilistaOturumEposta, let bulunan = kayit.epostaIleBul(eposta) {
            oturum = oturumOlustur(uid: bulunan.uid, alanlar: bulunan.alanlar, eposta: eposta)
        }
    }

    private func oturumOlustur(uid: String, alanlar: [String: Any]?, eposta: String) -> GirisOturumu {
        let kurallar = AuthRules.shared
        let ad = alanlar?[kurallar.FIELD_NAME] as? String ?? ""
        let rol = alanlar?[kurallar.FIELD_ROLE] as? String ?? kurallar.ROLE_STUDENT
        let kimlik = alanlar?[kurallar.FIELD_USER_ID] as? String ?? ""
        return GirisOturumu(uid: uid, eposta: eposta, ad: ad, rol: rol, kullaniciKimligi: kimlik)
    }

    private func bekle() async throws {
        hazirla()
        if ayarlar.gecikmeSaniye > 0 {
            let nanosaniye = UInt64(ayarlar.gecikmeSaniye * 1_000_000_000)
            try? await Task.sleep(nanoseconds: nanosaniye)
        }
        if ayarlar.agHatasi {
            throw GirisHatasi("Ağ bağlantı hatası. İnternet bağlantınızı kontrol edin.")
        }
    }

    /// Android: AuthManager.generateUniqueUserId (15 deneme, sonra zaman damgalı yedek).
    private func benzersizKullaniciKimligi() async -> String {
        let azami = Int(AuthRules.shared.USER_ID_MAX_ATTEMPTS)
        var deneme = 0
        while deneme < azami {
            deneme += 1
            let aday: String = UserIdRules.shared.generateCandidate()
            do {
                let kullaniliyor = try await kayit.kullaniciKimligiKullaniliyorMu(aday)
                if !kullaniliyor { return aday }
            } catch {
                break
            }
        }
        return UserIdRules.shared.generateFallback(nowMs: GirisYerelDepo.simdiMs())
    }

    // MARK: Oturum durumu

    var mevcutOturum: GirisOturumu? {
        hazirla()
        return oturum
    }

    func cikisYap() async {
        oturum = nil
    }

    // MARK: E-posta + doğrulama kodu

    func epostaKayitliMi(eposta: String) async -> GirisEpostaKaydi {
        do {
            try await bekle()
        } catch {
            // Android: çağrı hata verirse (false, null, null).
            return GirisEpostaKaydi(kayitli: false, uid: nil, rol: nil)
        }
        let normal = AuthRules.shared.normalizeEmail(email: eposta)
        guard let bulunan = kayit.epostaIleBul(normal) else {
            return GirisEpostaKaydi(kayitli: false, uid: nil, rol: nil)
        }
        let rol = bulunan.alanlar[AuthRules.shared.FIELD_ROLE] as? String
        return GirisEpostaKaydi(kayitli: true, uid: bulunan.uid, rol: rol)
    }

    func dogrulamaKoduGonder(eposta: String, amac: String) async throws {
        try await bekle()
        let kurallar = AuthRules.shared
        if eposta.hasPrefix("hata") {
            throw GirisHatasi("Doğrulama kodu gönderilemedi.")
        }
        if eposta.hasPrefix("sessiz") {
            throw GirisHatasi(nil)
        }
        // Sunucu kuralları (functions/index.js sendStudentVerificationCode).
        let bulunan = kayit.epostaIleBul(eposta)
        if amac == kurallar.OTP_PURPOSE_REGISTER {
            if bulunan != nil {
                throw GirisHatasi("Bu e-posta zaten kayıtlı. Lütfen giriş yapın.")
            }
        } else {
            guard let hesap = bulunan else {
                throw GirisHatasi(kurallar.MESSAGE_USER_NOT_FOUND)
            }
            let rol = hesap.alanlar[kurallar.FIELD_ROLE] as? String
            let gereken = amac == kurallar.OTP_PURPOSE_TEACHER_RESET ? kurallar.ROLE_TEACHER : kurallar.ROLE_STUDENT
            if rol != gereken {
                throw GirisHatasi("E-posta hatalı.")
            }
        }
        gonderilenKodlar[eposta] = amac
    }

    /// Sunucudaki kod denetimi: yanlış kodda sayaç artar, 5. yanlıştan sonra 15 dk kilit mesajı döner.
    private func koduDenetle(eposta: String, kod: String, amac: String) throws {
        let azami = Int(OtpRules.shared.MAX_WRONG_ATTEMPTS)
        let sayac = yanlisDenemeler[eposta] ?? 0
        if sayac >= azami {
            throw GirisHatasi(OtpRules.shared.wrongCooldownTryLaterMessage(remainingMin: 15))
        }
        if kod != ayarlar.gecerliKod || gonderilenKodlar[eposta] == nil {
            yanlisDenemeler[eposta] = sayac + 1
            throw GirisHatasi("Geçersiz kod")
        }
        if gonderilenKodlar[eposta] != amac {
            throw GirisHatasi("Geçersiz veya süresi dolmuş kod")
        }
        yanlisDenemeler[eposta] = 0
    }

    func girisKodunuDogrula(eposta: String, kod: String) async throws -> GirisOturumu {
        try await bekle()
        let kurallar = AuthRules.shared
        let normal = kurallar.normalizeEmail(email: eposta)
        try koduDenetle(eposta: normal, kod: kod, amac: kurallar.OTP_PURPOSE_LOGIN)
        gonderilenKodlar[normal] = nil
        guard let bulunan = kayit.epostaIleBul(normal) else {
            throw GirisHatasi(kurallar.MESSAGE_SESSION_NOT_OPENED)
        }
        let rol = bulunan.alanlar[kurallar.FIELD_ROLE] as? String
        if !kurallar.isOtpLoginAllowed(roleField: rol) {
            throw GirisHatasi(kurallar.MESSAGE_TEACHER_ON_STUDENT_LOGIN)
        }
        let yeni = oturumOlustur(uid: bulunan.uid, alanlar: bulunan.alanlar, eposta: eposta)
        oturum = yeni
        return yeni
    }

    func kayitKodunuDogrula(
        eposta: String,
        kod: String,
        bekleyen: GirisBekleyenKayit?,
        dogumYili: Int?,
        kaynak: String?
    ) async throws -> GirisOturumu {
        try await bekle()
        let kurallar = AuthRules.shared
        let normal = kurallar.normalizeEmail(email: eposta)
        try koduDenetle(eposta: normal, kod: kod, amac: kurallar.OTP_PURPOSE_REGISTER)
        gonderilenKodlar[normal] = nil

        // Sunucunun yazdığı belge (functions/index.js verifyRegistrationCode); alanlar
        // KullaniciKayitServisi.swift başındaki 1. yol ile aynıdır.
        let rol: String = bekleyen?.rol ?? kurallar.ROLE_STUDENT
        let uid = "sahte-" + UUID().uuidString.lowercased()
        let kimlik = await benzersizKullaniciKimligi()
        let girilenAd = (bekleyen?.ad ?? "").trimmingCharacters(in: .whitespaces)
        let ad = girilenAd.isEmpty ? kimlik : girilenAd
        var alanlar: [String: Any] = [
            kurallar.FIELD_UID: uid,
            kurallar.FIELD_USER_ID: kimlik,
            kurallar.FIELD_EMAIL: normal,
            kurallar.FIELD_NAME: ad,
            kurallar.FIELD_ROLE: rol,
            kurallar.FIELD_FIRST_TUTORIAL_SHOWN: false,
            kurallar.FIELD_CREATED_AT: GirisYerelDepo.simdiMs(),
        ]
        let cuzdan: [String: Any] = UserWalletFirestore.shared.registrationWalletFields()
        for (anahtar, deger) in cuzdan {
            alanlar[anahtar] = deger
        }
        if let yil = dogumYili {
            alanlar[kurallar.FIELD_BIRTH_YEAR] = yil
        }
        if let kaynakAdi = kaynak, !kaynakAdi.isEmpty {
            alanlar[kurallar.FIELD_ACQUISITION_SOURCE] = kaynakAdi
        }
        if rol == kurallar.ROLE_TEACHER {
            alanlar[kurallar.FIELD_TEACHER_APPROVED] = false
            if let sifre = bekleyen?.sifre, !sifre.isEmpty {
                sifreler[normal] = sifre
            }
        } else {
            alanlar[kurallar.FIELD_VERIFIED] = true
        }
        kayit.anlikYaz(uid: uid, alanlar: alanlar)

        let yeni = oturumOlustur(uid: uid, alanlar: alanlar, eposta: eposta)
        oturum = yeni
        return yeni
    }

    // MARK: E-posta + şifre

    func sifreIleGiris(eposta: String, sifre: String) async throws -> GirisOturumu {
        let kurallar = AuthRules.shared
        do {
            try await bekle()
        } catch {
            // Android: loginTeacher her Auth hatasında aynı metni gösterir.
            throw GirisHatasi(kurallar.MESSAGE_WRONG_PASSWORD_OR_EMAIL)
        }
        let normal = kurallar.normalizeEmail(email: eposta)
        guard let kayitliSifre = sifreler[normal], kayitliSifre == sifre,
              let bulunan = kayit.epostaIleBul(normal) else {
            throw GirisHatasi(kurallar.MESSAGE_WRONG_PASSWORD_OR_EMAIL)
        }
        let rol = bulunan.alanlar[kurallar.FIELD_ROLE] as? String ?? ""
        if rol != kurallar.ROLE_TEACHER {
            throw GirisHatasi(kurallar.MESSAGE_NOT_TEACHER)
        }
        let yeni = oturumOlustur(uid: bulunan.uid, alanlar: bulunan.alanlar, eposta: eposta)
        oturum = yeni
        return yeni
    }

    func sifreIleKayit(eposta: String, ad: String, sifre: String) async throws -> GirisOturumu {
        try await bekle()
        let kurallar = AuthRules.shared
        if kayit.epostaIleBul(eposta) != nil || sifreler[eposta] != nil {
            throw GirisHatasi(
                kurallar.registerStudentErrorMessage(
                    exceptionMessage: "The email address is already in use by another account.",
                    localizedMessage: nil
                )
            )
        }
        let uid = "sahte-" + UUID().uuidString.lowercased()
        let kimlik = await benzersizKullaniciKimligi()
        let alanlar: [String: Any] = kurallar.emailStudentUserData(
            uid: uid,
            userId: kimlik,
            email: eposta,
            name: ad,
            firstTutorialShown: false,
            nowMs: GirisYerelDepo.simdiMs(),
            walletFields: UserWalletFirestore.shared.registrationWalletFields()
        )
        do {
            try await kayit.belgeOlustur(uid: uid, alanlar: alanlar)
        } catch {
            // Android: belge yazılamazsa Auth hesabı silinir.
            throw GirisHatasi("Kayıt başarısız: " + error.localizedDescription)
        }
        sifreler[eposta] = sifre
        let yeni = oturumOlustur(uid: uid, alanlar: alanlar, eposta: eposta)
        oturum = yeni
        return yeni
    }

    // MARK: Şifre sıfırlama

    func sifreSifirlamaKodunuDogrula(eposta: String, kod: String) async throws {
        try await bekle()
        let kurallar = AuthRules.shared
        let normal = kurallar.normalizeEmail(email: eposta)
        try koduDenetle(eposta: normal, kod: kod, amac: kurallar.OTP_PURPOSE_TEACHER_RESET)
    }

    func sifreyiSifirla(eposta: String, kod: String, yeniSifre: String) async throws {
        try await bekle()
        let kurallar = AuthRules.shared
        let normal = kurallar.normalizeEmail(email: eposta)
        try koduDenetle(eposta: normal, kod: kod, amac: kurallar.OTP_PURPOSE_TEACHER_RESET)
        gonderilenKodlar[normal] = nil
        sifreler[normal] = yeniSifre
    }

    func sifreSifirlamaEpostasiGonder(eposta: String) async throws {
        let duzgun = eposta.trimmingCharacters(in: .whitespaces)
        if duzgun.isEmpty {
            throw GirisHatasi(AuthRules.shared.MESSAGE_ENTER_EMAIL)
        }
        try await bekle()
    }

    // MARK: Üçüncü taraf girişleri

    func googleIleGiris(otomatikKayit: Bool, dogumYili: Int?, kaynak: String?) async throws -> GirisOturumu {
        return try await saglayiciIleGiris(
            sonuc: ayarlar.googleSonucu,
            onEk: "google",
            otomatikKayit: otomatikKayit,
            dogumYili: dogumYili,
            kaynak: kaynak
        )
    }

    func appleIleGiris(otomatikKayit: Bool, dogumYili: Int?, kaynak: String?) async throws -> GirisOturumu {
        return try await saglayiciIleGiris(
            sonuc: ayarlar.appleSonucu,
            onEk: "apple",
            otomatikKayit: otomatikKayit,
            dogumYili: dogumYili,
            kaynak: kaynak
        )
    }

    /// Android: AuthManager.signInWithGoogle'ın Firestore kısmı.
    private func saglayiciIleGiris(
        sonuc: SahteSaglayiciSonucu,
        onEk: String,
        otomatikKayit: Bool,
        dogumYili: Int?,
        kaynak: String?
    ) async throws -> GirisOturumu {
        try await bekle()
        let kurallar = AuthRules.shared
        let eposta: String
        let ad: String
        switch sonuc {
        case .iptal:
            throw GirisHatasi(kurallar.MESSAGE_GOOGLE_CANCELLED)
        case .hata(let metin):
            throw GirisHatasi(metin)
        case .hesap(let hesapEposta, let hesapAd):
            eposta = hesapEposta
            ad = hesapAd
        }
        let uid = "sahte-" + onEk + "-" + kurallar.normalizeEmail(email: eposta)

        let belge: [String: Any]?
        do {
            belge = try await kayit.belgeOku(uid: uid)
        } catch {
            throw GirisHatasi(error.localizedDescription)
        }

        if var mevcut = belge {
            let rol = mevcut[kurallar.FIELD_ROLE] as? String
            if !kurallar.isGoogleLoginAllowed(roleField: rol) {
                throw GirisHatasi(kurallar.MESSAGE_CREDENTIALS_NOT_VERIFIED)
            }
            // Eski hesaplar: userId yoksa üretilir; verified değilse true yapılır (hatalar yutulur).
            var guncelleme: [String: Any] = [:]
            let kimlik = mevcut[kurallar.FIELD_USER_ID] as? String ?? ""
            if kimlik.isEmpty {
                guncelleme[kurallar.FIELD_USER_ID] = await benzersizKullaniciKimligi()
            }
            let dogrulanmis = mevcut[kurallar.FIELD_VERIFIED] as? Bool ?? false
            if !dogrulanmis {
                guncelleme[kurallar.FIELD_VERIFIED] = true
            }
            if !guncelleme.isEmpty {
                try? await kayit.alanlariGuncelle(uid: uid, alanlar: guncelleme)
                for (anahtar, deger) in guncelleme {
                    mevcut[anahtar] = deger
                }
            }
            let yeni = oturumOlustur(uid: uid, alanlar: mevcut, eposta: eposta)
            oturum = yeni
            return yeni
        }

        if !otomatikKayit {
            // Giriş ekranından çağrıldı: oturum kapatılır, kayıt akışına yönlendirilir.
            throw GirisHatasi(kurallar.ERROR_ACCOUNT_NOT_REGISTERED, hesapEposta: eposta, hesapAd: ad)
        }

        let kimlik = await benzersizKullaniciKimligi()
        let kutuluYil: KotlinInt? = dogumYili.map { KotlinInt(int: Int32($0)) }
        let alanlar: [String: Any] = kurallar.googleStudentUserData(
            uid: uid,
            userId: kimlik,
            email: eposta,
            displayName: ad,
            firstTutorialShown: false,
            nowMs: GirisYerelDepo.simdiMs(),
            birthYear: kutuluYil,
            acquisitionSource: kaynak,
            walletFields: UserWalletFirestore.shared.registrationWalletFields()
        )
        do {
            try await kayit.belgeOlustur(uid: uid, alanlar: alanlar)
        } catch {
            throw GirisHatasi(error.localizedDescription)
        }
        let yeni = oturumOlustur(uid: uid, alanlar: alanlar, eposta: eposta)
        oturum = yeni
        return yeni
    }

    // MARK: Tek cihaz oturumu

    func tekCihazVerisi() async throws -> GirisTekCihazVerisi {
        try await bekle()
        guard let acik = oturum else {
            return GirisTekCihazVerisi(belgeVar: false, aktifCihazKimligi: nil, sonGuncellemeMs: nil)
        }
        if ayarlar.baskaCihazdaOturum {
            return GirisTekCihazVerisi(
                belgeVar: true,
                aktifCihazKimligi: "baska-cihaz",
                sonGuncellemeMs: GirisYerelDepo.simdiMs()
            )
        }
        guard let belge = kayit.anlikBelge(uid: acik.uid) else {
            return GirisTekCihazVerisi(belgeVar: false, aktifCihazKimligi: nil, sonGuncellemeMs: nil)
        }
        let kurallar = SessionDeviceRules.shared
        let cihaz = belge[kurallar.FIELD_ACTIVE_DEVICE_ID] as? String
        let zaman = (belge[kurallar.FIELD_ACTIVE_SESSION_UPDATED_AT] as? NSNumber)?.int64Value
        return GirisTekCihazVerisi(belgeVar: true, aktifCihazKimligi: cihaz, sonGuncellemeMs: zaman)
    }

    func oturumuBuCihazaAl(cihazKimligi: String) async {
        guard let acik = oturum else { return }
        let ham: [String: Any] = SessionDeviceRules.shared.claimUpdate(
            deviceId: cihazKimligi,
            nowMs: GirisYerelDepo.simdiMs()
        )
        try? await kayit.alanlariGuncelle(uid: acik.uid, alanlar: ham)
    }
}
