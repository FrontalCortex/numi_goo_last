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
//
// 13 yaş altı (veli onayı): yaş adımında 13 ya da daha küçük yaş girilince kayıt veli e-postası
// ekranına gider. Veli adresi çocuğunkinden farklıysa önce çocuğun kendi adresine kod gider
// (sunucu kuralı). Veli adresi "hata…" ile başlarsa veli kodu gönderilemez.

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

/// Sunucudaki veli onayı kaydının (`parentalConsents`) sahte karşılığı: kod hangi veliye, hangi
/// doğum yılıyla gitti. Doğum yılını hesap açılırken sunucu buradan alır (çocuk değiştiremesin).
struct SahteVeliKaydi {
    let veliEposta: String
    let dogumYili: Int
    let kaynak: String?
    /// Google / Apple kaydı mı (hesap confirmParentalConsent ile açılır).
    let saglayici: Bool
}

/// 13 yaş altı Google / Apple kaydında açık kalan, belgesi olmayan oturum.
struct SahteBekleyenSaglayici {
    let uid: String
    let eposta: String
    let ad: String
}

@MainActor
final class SahteKimlikServisi: KimlikServisi {
    var ayarlar: SahteKimlikAyarlari

    let kayit: SahteKullaniciKayitServisi

    private var oturum: GirisOturumu? {
        didSet {
            // Son giriş zamanı (Android: FirebaseUser.metadata.lastSignInTimestamp; hesap silmenin
            // "yakın zamanda giriş" denetimi bunu okur). Kurucudaki atama gözlemciyi tetiklemez.
            if let yeni = oturum, yeni.uid != oldValue?.uid {
                oturumAcilisMs = GirisYerelDepo.simdiMs()
            } else if oturum == nil {
                oturumAcilisMs = nil
            }
        }
    }
    /// Açık oturumun açıldığı an (epoch ms); oturum yoksa nil.
    private(set) var oturumAcilisMs: Int64? = nil
    private var sifreler: [String: String]
    /// Gönderilmiş kodun amacı (e-posta → amaç); sunucudaki `studentVerificationCodes` karşılığı.
    private var gonderilenKodlar: [String: String]
    private var yanlisDenemeler: [String: Int]
    private var hazirlandi: Bool
    /// Çocuk e-postası → veli onayı kaydı.
    private var veliKayitlari: [String: SahteVeliKaydi]
    /// Veli adımı için adresi doğrulanmış çocuk e-postaları (sunucuda 30 dk geçerli).
    private var dogrulanmisCocuklar: Set<String>
    private var bekleyenSaglayici: SahteBekleyenSaglayici?

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
        self.veliKayitlari = [:]
        self.dogrulanmisCocuklar = []
        self.bekleyenSaglayici = nil
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

    /// Beklemeden oturumu kapatır (Android: AuthManager.logout → auth.signOut). Kayıtlı hesaplar, şifreler
    /// ve belgeler KALIR; aynı hesapla yeniden giriş yapılabilir.
    func oturumuKapat() {
        oturum = nil
    }

    /// Sahte hesap silme (Android: AccountDeletionHelper → `users/{uid}` ve Auth kaydı silinir): belge,
    /// şifre ve bekleyen kodlar gider, açık oturumsa kapanır. Aynı e-postayla artık giriş yapılamaz
    /// ("kayıtlı değil"); yeniden kayıt olunabilir.
    func hesabiSil(uid: String) {
        hazirla()
        let kurallar = AuthRules.shared
        if let belge = kayit.anlikBelge(uid: uid), let eposta = belge[kurallar.FIELD_EMAIL] as? String {
            let normal: String = kurallar.normalizeEmail(email: eposta)
            sifreler[eposta] = nil
            sifreler[normal] = nil
            gonderilenKodlar[normal] = nil
            yanlisDenemeler[normal] = nil
        }
        kayit.anlikSil(uid: uid)
        if oturum?.uid == uid {
            oturum = nil
        }
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
        try koduDenetle(eposta: eposta, kod: kod, amaclar: [amac])
    }

    /// `amaclar`: kabul edilen gönderim amaçları (kayıt kodu ile veli kodu aynı çağrıyla doğrulanır).
    private func koduDenetle(eposta: String, kod: String, amaclar: [String]) throws {
        let azami = Int(OtpRules.shared.MAX_WRONG_ATTEMPTS)
        let sayac = yanlisDenemeler[eposta] ?? 0
        if sayac >= azami {
            throw GirisHatasi(OtpRules.shared.wrongCooldownTryLaterMessage(remainingMin: 15))
        }
        guard let gonderilen = gonderilenKodlar[eposta], kod == ayarlar.gecerliKod else {
            yanlisDenemeler[eposta] = sayac + 1
            throw GirisHatasi("Geçersiz kod")
        }
        if !amaclar.contains(gonderilen) {
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
        // Veli kodu (e-posta yolu) da buradan doğrulanır: sunucu kodun veli kodu olduğunu kendi
        // kaydından bilir, doğum yılını ve kaynağı da oradan alır.
        try koduDenetle(
            eposta: normal,
            kod: kod,
            amaclar: [kurallar.OTP_PURPOSE_REGISTER, kurallar.OTP_PURPOSE_REGISTER_PARENTAL]
        )
        let veliKodu: Bool = gonderilenKodlar[normal] == kurallar.OTP_PURPOSE_REGISTER_PARENTAL
        gonderilenKodlar[normal] = nil
        let veliKaydi: SahteVeliKaydi? = veliKodu ? veliKayitlari[normal] : nil
        if !veliKodu {
            // Sunucu: 13 yaş altı doğum yılıyla gelen olağan kayıt kodu reddedilir.
            let kutuluYil: KotlinInt? = dogumYili.map { KotlinInt(int: Int32($0)) }
            if kurallar.requiresParentalConsent(birthYear: kutuluYil, utcYear: SahteKimlikServisi.utcYili()) {
                throw GirisHatasi("Veli onayı gerekiyor")
            }
        }
        veliKayitlari[normal] = nil
        dogrulanmisCocuklar.remove(normal)
        let yil: Int? = veliKaydi?.dogumYili ?? dogumYili
        let kaynakDegeri: String? = veliKaydi?.kaynak ?? kaynak

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
        if let dogum = yil {
            alanlar[kurallar.FIELD_BIRTH_YEAR] = dogum
        }
        if let kaynakAdi = kaynakDegeri, !kaynakAdi.isEmpty {
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

    // MARK: 13 yaş altı: veli onayı

    /// Sahte sunucunun "bu yıl"ı (sunucu ve kurallar UTC yılı kullanır).
    static func utcYili() -> Int32 {
        var takvim = Calendar(identifier: .gregorian)
        takvim.timeZone = TimeZone(identifier: "UTC") ?? TimeZone.current
        return Int32(takvim.component(.year, from: Date()))
    }

    func veliKayitKoduGonder(cocukEposta: String, veliEposta: String, dogumYili: Int, kaynak: String?) async throws {
        try await bekle()
        let kurallar = AuthRules.shared
        // Gerçek çağrının gövdesi (functions/index.js sendStudentVerificationCode).
        let govde: [String: Any] = kurallar.sendParentalRegistrationCodePayload(
            childEmail: cocukEposta,
            parentEmail: veliEposta,
            birthYear: Int32(dogumYili),
            acquisitionSource: kaynak,
            lang: Metin.dil
        )
        let cocuk: String = (govde["email"] as? String) ?? kurallar.normalizeEmail(email: cocukEposta)
        let veli: String = (govde["parentEmail"] as? String) ?? kurallar.normalizeEmail(email: veliEposta)
        if veli.hasPrefix("hata") {
            throw GirisHatasi("Doğrulama kodu gönderilemedi.")
        }
        if kayit.epostaIleBul(cocuk) != nil {
            throw GirisHatasi("Bu e-posta zaten kayıtlı. Lütfen giriş yapın.")
        }
        // Veli adresi farklıysa önce çocuğun kendi adresi doğrulanmış olmalı
        // (sunucu: details.reason = child_email_unverified).
        if veli != cocuk && !dogrulanmisCocuklar.contains(cocuk) {
            throw GirisHatasi(kurallar.ERROR_CHILD_EMAIL_NOT_VERIFIED)
        }
        veliKayitlari[cocuk] = SahteVeliKaydi(veliEposta: veli, dogumYili: dogumYili, kaynak: kaynak, saglayici: false)
        gonderilenKodlar[cocuk] = kurallar.OTP_PURPOSE_REGISTER_PARENTAL
    }

    func cocukEpostasiniDogrula(eposta: String, kod: String, dogumYili: Int) async throws {
        try await bekle()
        let kurallar = AuthRules.shared
        let govde: [String: Any] = kurallar.verifyChildEmailForParentalConsentPayload(
            email: eposta,
            code: kod,
            birthYear: Int32(dogumYili)
        )
        let normal: String = (govde["email"] as? String) ?? kurallar.normalizeEmail(email: eposta)
        try koduDenetle(eposta: normal, kod: kod, amac: kurallar.OTP_PURPOSE_REGISTER)
        gonderilenKodlar[normal] = nil
        dogrulanmisCocuklar.insert(normal)
    }

    func saglayiciVeliOnayiIste(veliEposta: String, dogumYili: Int, kaynak: String?) async throws {
        try await bekle()
        let kurallar = AuthRules.shared
        guard let bekleyen = bekleyenSaglayici else {
            throw GirisHatasi(Metin.al("ui_oturum_acilamadi"))
        }
        let govde: [String: Any] = kurallar.requestGoogleParentalConsentPayload(
            childEmail: bekleyen.eposta,
            parentEmail: veliEposta,
            birthYear: Int32(dogumYili),
            acquisitionSource: kaynak,
            lang: Metin.dil
        )
        let veli: String = (govde["parentEmail"] as? String) ?? kurallar.normalizeEmail(email: veliEposta)
        if veli.hasPrefix("hata") {
            throw GirisHatasi("Doğrulama kodu gönderilemedi.")
        }
        // Sağlayıcı hesabının adresi doğrulanmış sayılır; çocuk adresi adımı yok.
        veliKayitlari[bekleyen.eposta] = SahteVeliKaydi(veliEposta: veli, dogumYili: dogumYili, kaynak: kaynak, saglayici: true)
        gonderilenKodlar[bekleyen.eposta] = kurallar.OTP_PURPOSE_REGISTER_PARENTAL
    }

    func saglayiciVeliOnayiniDogrula(kod: String) async throws -> GirisOturumu {
        try await bekle()
        let kurallar = AuthRules.shared
        guard let bekleyen = bekleyenSaglayici else {
            throw GirisHatasi(Metin.al("ui_oturum_acilamadi"))
        }
        try koduDenetle(eposta: bekleyen.eposta, kod: kod, amac: kurallar.OTP_PURPOSE_REGISTER_PARENTAL)
        gonderilenKodlar[bekleyen.eposta] = nil
        let veliKaydi: SahteVeliKaydi? = veliKayitlari[bekleyen.eposta]
        veliKayitlari[bekleyen.eposta] = nil

        // Belgeyi sunucu yazar (functions/index.js confirmParentalConsent); alanlar Google ile
        // otomatik kayıttakilerle aynı.
        let kimlik = await benzersizKullaniciKimligi()
        let kutuluYil: KotlinInt? = veliKaydi.map { KotlinInt(int: Int32($0.dogumYili)) }
        let alanlar: [String: Any] = kurallar.googleStudentUserData(
            uid: bekleyen.uid,
            userId: kimlik,
            email: bekleyen.eposta,
            displayName: bekleyen.ad,
            firstTutorialShown: false,
            nowMs: GirisYerelDepo.simdiMs(),
            birthYear: kutuluYil,
            acquisitionSource: veliKaydi?.kaynak,
            walletFields: UserWalletFirestore.shared.registrationWalletFields()
        )
        kayit.anlikYaz(uid: bekleyen.uid, alanlar: alanlar)
        bekleyenSaglayici = nil
        let yeni = oturumOlustur(uid: bekleyen.uid, alanlar: alanlar, eposta: bekleyen.eposta)
        oturum = yeni
        return yeni
    }

    func yarimKaydiSil() async {
        if let bekleyen = bekleyenSaglayici {
            veliKayitlari[bekleyen.eposta] = nil
            gonderilenKodlar[bekleyen.eposta] = nil
        }
        bekleyenSaglayici = nil
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

        let kutuluYil: KotlinInt? = dogumYili.map { KotlinInt(int: Int32($0)) }
        if kurallar.requiresParentalConsent(birthYear: kutuluYil, utcYear: SahteKimlikServisi.utcYili()) {
            // 13 yaş altı: belge YAZILMAZ, oturum açık kalır (veli onayı çağrıları kimliği
            // oradan alır). Android: AuthManager.signInWithGoogle → PARENTAL_CONSENT_REQUIRED.
            bekleyenSaglayici = SahteBekleyenSaglayici(uid: uid, eposta: kurallar.normalizeEmail(email: eposta), ad: ad)
            throw GirisHatasi(kurallar.ERROR_PARENTAL_CONSENT_REQUIRED, hesapEposta: eposta, hesapAd: ad)
        }

        let kimlik = await benzersizKullaniciKimligi()
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
