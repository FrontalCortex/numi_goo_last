import Foundation

// Kimlik doğrulama servisi: Firebase Auth + Cloud Functions çağrılarının Swift tarafındaki sözleşmesi.
// Android karşılığı: app/src/main/java/com/example/app/auth/AuthManager.kt ve
// SessionDeviceManager.kt. Firebase iOS SDK henüz bağlı olmadığı için şimdilik yalnızca
// `SahteKimlikServisi` vardır; gerçek uygulama aynı protokolü Firebase ile dolduracak.
//
// Android'de öğrenci girişi PAROLASIZDIR: e-postaya 6 haneli kod gider, kod Cloud Function ile
// doğrulanır, dönen özel belirteçle (custom token) oturum açılır. Parola yalnızca öğretmen
// hesaplarında vardır. Bu yüzden "e-posta doğrulama" adımı girişin kendisidir.
//
// Hata sözleşmesi: Android'deki `callback(false, mesaj)` çağrılarının karşılığı `GirisHatasi`
// fırlatmaktır; `mesaj` Android'in kullanıcıya gösterdiği metnin (ya da özel kodun) aynısıdır.
// Hangi mesajın ne zaman gösterileceğine ortak kod karar verir (AuthRules, OtpRules).

/// Oturumu açık kullanıcı. Android: FirebaseAuth.currentUser + `auth_prefs` önbelleği
/// (AuthManager.cacheBasicUser: user_email, user_type, user_name, user_id).
struct GirisOturumu: Equatable {
    /// Firebase Auth uid; `users/{uid}` belgesinin kimliği.
    let uid: String
    let eposta: String
    /// `users/{uid}.name`
    let ad: String
    /// `users/{uid}.role`: AuthRules.ROLE_STUDENT ya da AuthRules.ROLE_TEACHER.
    let rol: String
    /// `users/{uid}.userId`: 6 rakam + 2 harflik görünen kullanıcı kimliği (örn. "483920QK").
    let kullaniciKimligi: String
}

/// Servis hatası. `mesaj`, Android'de callback'e verilen metindir; nil ise ekran kendi
/// varsayılan metnini gösterir (Android'deki `error ?: "..."` kalıbı).
struct GirisHatasi: Error {
    let mesaj: String?
    /// Google/Apple hesabı seçildi ama kayıtlı değilse (mesaj = AuthRules.ERROR_ACCOUNT_NOT_REGISTERED)
    /// kayıt ekranını doldurmak için hesabın e-postası ve adı.
    let hesapEposta: String?
    let hesapAd: String?

    init(_ mesaj: String?, hesapEposta: String? = nil, hesapAd: String? = nil) {
        self.mesaj = mesaj
        self.hesapEposta = hesapEposta
        self.hesapAd = hesapAd
    }
}

/// `checkEmailRegistered` Cloud Function yanıtı (AuthRules.parseEmailRegisteredResult).
/// Çağrı hata verirse Android (false, null, null) döndürür; servis de öyle yapmalıdır.
struct GirisEpostaKaydi {
    let kayitli: Bool
    let uid: String?
    let rol: String?
}

/// Kod doğrulanana kadar yalnızca bellekte tutulan kayıt bilgisi
/// (Android: AuthManager.pendingRegistrations; ortak kod: AuthRules.PendingRegistration).
/// Öğrencide ad ve şifre boştur; şifreyi sunucu üretir.
struct GirisBekleyenKayit {
    let ad: String
    let sifre: String
    let rol: String
}

/// Tek cihaz kuralının girdileri: `users/{uid}` belgesinin `activeDeviceId` ve
/// `activeSessionUpdatedAt` alanları. Kararı SessionDeviceRules.decide verir.
struct GirisTekCihazVerisi {
    let belgeVar: Bool
    let aktifCihazKimligi: String?
    /// `activeSessionUpdatedAt` (Timestamp) epoch ms olarak; alan yoksa nil.
    let sonGuncellemeMs: Int64?
}

@MainActor
protocol KimlikServisi: AnyObject {
    // MARK: Oturum durumu

    /// Android: FirebaseAuth.currentUser. Uygulama yeniden açıldığında kalıcı oturumu döndürmelidir.
    var mevcutOturum: GirisOturumu? { get }

    /// Android: AuthManager.logout (auth.signOut + `auth_prefs` temizliği).
    func cikisYap() async

    // MARK: E-posta + doğrulama kodu (öğrenci girişi/kaydı, öğretmen kaydı)

    /// Cloud Function `checkEmailRegistered` (gövde: AuthRules.checkEmailRegisteredPayload).
    func epostaKayitliMi(eposta: String) async -> GirisEpostaKaydi

    /// Doğrulama e-postası (6 haneli kod) gönderir. Cloud Function `sendStudentVerificationCode`
    /// (gövde: AuthRules.sendVerificationCodePayload). `eposta` normalleştirilmiş olmalıdır;
    /// `amac`: AuthRules.OTP_PURPOSE_REGISTER / OTP_PURPOSE_LOGIN / OTP_PURPOSE_TEACHER_RESET.
    /// Hata mesajı: FirebaseFunctionsException.message, yoksa nil ("Kod gönderilemedi" gösterilir).
    /// Giriş amacında hesap yoksa sunucu "Kullanıcı bulunamadı" döndürür.
    func dogrulamaKoduGonder(eposta: String, amac: String) async throws

    /// Giriş kodunu doğrular ve oturum açar. Cloud Function `verifyLoginCode`
    /// (gövde: AuthRules.verifyLoginCodePayload) → `signInWithCustomToken` → `users/{uid}` okunur;
    /// rol öğrenci değilse (AuthRules.isOtpLoginAllowed) çıkış yapılıp
    /// AuthRules.MESSAGE_TEACHER_ON_STUDENT_LOGIN ile hata verilir. Android: verifyLoginWithOTP.
    func girisKodunuDogrula(eposta: String, kod: String) async throws -> GirisOturumu

    /// Kayıt kodunu doğrular, hesabı OLUŞTURUR ve oturum açar. Cloud Function
    /// `verifyRegistrationCode` (gövde: AuthRules.verifyRegistrationCodePayload) →
    /// `signInWithCustomToken`. Kullanıcı belgesini SUNUCU yazar (bkz. KullaniciKayitServisi.swift).
    /// Android: verifyStudentCode(autoLogin = true).
    func kayitKodunuDogrula(
        eposta: String,
        kod: String,
        bekleyen: GirisBekleyenKayit?,
        dogumYili: Int?,
        kaynak: String?
    ) async throws -> GirisOturumu

    // MARK: 13 yaş altı: veli onayı (COPPA / GDPR / KVKK)
    //
    // Hesap veli kodu girilene kadar hiçbir yerde açılmaz. Gerekip gerekmediğine
    // AuthRules.requiresParentalConsent karar verir (UTC yılı − doğum yılı ≤ 13; sunucu
    // `requiresParentalConsent` ve kurallar `noUnconsentedChild` aynı formülü kullanır).
    // Android: ParentConsentFragment, OtpVerificationFragment, AuthManager.

    /// E-posta ile kayıt: kayıt kodunu VELİNİN adresine gönderir. Cloud Function
    /// `sendStudentVerificationCode` (amaç AuthRules.OTP_PURPOSE_REGISTER_PARENTAL; gövde
    /// AuthRules.sendParentalRegistrationCodePayload). Kod sonra `kayitKodunuDogrula` ile girilir;
    /// sunucu kodun veli kodu olduğunu ve doğum yılını kendi kaydından bilir.
    /// Veli adresi çocuğunkinden farklı ve çocuğun adresi henüz doğrulanmadıysa sunucu
    /// `details.reason = child_email_unverified` döndürür; servis bunu mesajı
    /// AuthRules.ERROR_CHILD_EMAIL_NOT_VERIFIED olan `GirisHatasi` olarak fırlatmalıdır.
    /// Android: AuthManager.sendParentalRegistrationCode.
    func veliKayitKoduGonder(cocukEposta: String, veliEposta: String, dogumYili: Int, kaynak: String?) async throws

    /// Çocuğun KENDİ adresinin kayıt kodunu veli adımı için doğrular; hesap AÇILMAZ, sunucu adresin
    /// doğrulandığını 30 dakikalığına kaydeder. Cloud Function `verifyRegistrationCode` (gövde
    /// AuthRules.verifyChildEmailForParentalConsentPayload: `parentalStep = true`); yanıtta
    /// `emailVerified == true` değilse AuthRules.MESSAGE_CODE_NOT_VERIFIED.
    /// Android: AuthManager.verifyChildEmailForParentalConsent.
    func cocukEpostasiniDogrula(eposta: String, kod: String, dogumYili: Int) async throws

    /// Google / Apple ile kayıt: veli koduna istek. Aynı `sendStudentVerificationCode` çağrısı
    /// (gövde AuthRules.requestGoogleParentalConsentPayload, `google = true`); çocuğun adresini
    /// sunucu açık oturumdan alır, bu yüzden oturum açık bırakılmış olmalıdır. Oturum yoksa
    /// "Oturum açılamadı". Android: AuthManager.requestGoogleParentalConsent.
    func saglayiciVeliOnayiIste(veliEposta: String, dogumYili: Int, kaynak: String?) async throws

    /// Google / Apple ile kayıt: veli kodunu doğrular; `users/{uid}` belgesini SUNUCU yazar
    /// (kurallar bu yaşla istemci yazımını reddeder). Cloud Function `confirmParentalConsent`
    /// (gövde AuthRules.confirmParentalConsentPayload: code, firstTutorialShown).
    /// Android: AuthManager.confirmGoogleParentalConsent.
    func saglayiciVeliOnayiniDogrula(kod: String) async throws -> GirisOturumu

    /// Google / Apple kaydında veli onayı beklenirken vazgeçildi: `users` belgesi olmayan yarım
    /// Auth kaydı silinir ve oturum kapatılır (sunucudaki saatlik temizlik ikinci emniyet).
    /// Android: RegisterActivity.deleteIncompleteAuthUser.
    func yarimKaydiSil() async

    // MARK: E-posta + şifre

    /// Öğretmen girişi. Android: loginTeacher (`signInWithEmailAndPassword` → rol denetimi).
    /// Hatalar: AuthRules.MESSAGE_WRONG_PASSWORD_OR_EMAIL, AuthRules.MESSAGE_NOT_TEACHER.
    func sifreIleGiris(eposta: String, sifre: String) async throws -> GirisOturumu

    /// E-posta/şifre ile öğrenci kaydı. Android: registerStudent — arayüzde artık KULLANILMIYOR
    /// (öğrenciler kodla kayıt olur); eski hesaplarla uyum için sözleşmede durur.
    /// Kullanıcı belgesini İSTEMCİ yazar (AuthRules.emailStudentUserData, `verified = false`).
    /// Hata eşlemesi: AuthRules.registerStudentErrorMessage.
    func sifreIleKayit(eposta: String, ad: String, sifre: String) async throws -> GirisOturumu

    // MARK: Şifre sıfırlama (yalnızca öğretmen)

    /// Sıfırlama kodunu yalnızca doğrular, kullanılmış saymaz. Cloud Function
    /// `verifyTeacherPasswordResetCode`. Kodun gönderimi `dogrulamaKoduGonder` iledir
    /// (amac = OTP_PURPOSE_TEACHER_RESET).
    func sifreSifirlamaKodunuDogrula(eposta: String, kod: String) async throws

    /// Kodu doğrular ve şifreyi günceller. Cloud Function `resetTeacherPassword`.
    func sifreyiSifirla(eposta: String, kod: String, yeniSifre: String) async throws

    /// Firebase Auth'un kendi sıfırlama e-postası. Android: sendPasswordResetEmail — arayüzde
    /// KULLANILMIYOR (kodlu akış kullanılır); sözleşmede yedek olarak durur.
    func sifreSifirlamaEpostasiGonder(eposta: String) async throws

    // MARK: Üçüncü taraf girişleri (yalnızca öğrenci)

    /// Google hesap seçimi + Firebase kimlik doğrulaması. Android: signInWithGoogle.
    /// `otomatikKayit` kayıt ekranından çağrıldığında true'dur: belge yoksa oluşturulur
    /// (AuthRules.googleStudentUserData). False iken belge yoksa çıkış yapılıp
    /// AuthRules.ERROR_ACCOUNT_NOT_REGISTERED ile hata verilir (hesapEposta/hesapAd dolu).
    /// Kullanıcı vazgeçerse mesaj AuthRules.MESSAGE_GOOGLE_CANCELLED olur.
    /// 13 yaş altı otomatik kayıtta (AuthRules.requiresParentalConsent(dogumYili)) belge YAZILMAZ:
    /// oturum açık bırakılır ve AuthRules.ERROR_PARENTAL_CONSENT_REQUIRED ile hata verilir
    /// (hesapEposta dolu); hesap veli onayıyla `saglayiciVeliOnayiniDogrula` üzerinden açılır.
    func googleIleGiris(otomatikKayit: Bool, dogumYili: Int?, kaynak: String?) async throws -> GirisOturumu

    /// Apple ile giriş (App Store kuralı 4.8). Android'de karşılığı yok; Google ile aynı
    /// sözleşmeyi izler. Apple e-postayı gizleyebilir (relay adresi) ve adı yalnızca ilk girişte verir.
    func appleIleGiris(otomatikKayit: Bool, dogumYili: Int?, kaynak: String?) async throws -> GirisOturumu

    // MARK: Tek cihaz oturumu

    /// `users/{uid}` belgesinden tek cihaz alanlarını okur. Okuma hata verirse fırlatır
    /// (Android: izin verilmez, giriş ekranına dönülür). Android: requireLoggedInAndSingleDevice.
    func tekCihazVerisi() async throws -> GirisTekCihazVerisi

    /// Oturumu bu cihaza alır: SessionDeviceRules.claimUpdate alanlarını `users/{uid}` belgesine
    /// yazar. Android: claimActiveSessionIfLoggedIn (hata yutulur).
    func oturumuBuCihazaAl(cihazKimligi: String) async
}
