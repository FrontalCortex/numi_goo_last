import Foundation
import Shared
import SwiftUI

// Giriş akışının durumu ve adımlar arası geçişler.
// Android karşılıkları: SplashActivity, LoginStartActivity, LoginActivity, RegisterActivity,
// UserInfoFragment, OtpVerificationFragment, TeacherLoginActivity,
// TeacherForgotPasswordEmailFragment, TeacherNewPasswordFragment.
// Doğrulama kuralları, mesajlar ve kararlar ortak koddan çağrılır (AuthRules, OtpRules,
// SessionDeviceRules, SplashRules, UserInfoRules, TeacherLoginRules, LoginFlowTexts).

/// Doğrulama kodu ekranının türü (OtpVerificationFragment: isRegistration / forPasswordReset).
enum GirisKodTuru: Hashable {
    case giris
    case kayit
    case sifreSifirlama
}

/// Akıştaki ekranlar. Yığının tepesindeki ekran gösterilir.
enum GirisEkran: Hashable {
    /// SplashActivity
    case acilis
    /// LoginStartActivity
    case baslangic
    /// LoginActivity (e-posta adımı)
    case ogrenciGiris
    /// UserInfoFragment (yaş + kaynak)
    case kullaniciBilgisi
    /// RegisterActivity
    case kayit
    /// OtpVerificationFragment
    case kod(eposta: String, tur: GirisKodTuru)
    /// TeacherLoginActivity
    case ogretmenGiris
    /// TeacherForgotPasswordEmailFragment
    case sifremiUnuttum
    /// TeacherNewPasswordFragment
    case yeniSifre(eposta: String, kod: String)
}

/// UserInfoFragment.Step
enum GirisBilgiAdimi {
    case yas
    case kaynak
}

/// Kısa bildirim (Android: Toast / Snackbar).
struct GirisBildirim: Identifiable, Equatable {
    let id: UUID
    let metin: String
}

@MainActor
final class GirisModeli: ObservableObject {
    private let servis: KimlikServisi
    private let depo: GirisYerelDepo
    private let girisYapildi: () -> Void

    // MARK: Genel durum

    @Published var yigin: [GirisEkran] = [.acilis]
    /// LoginStartActivity.EXTRA_TEACHER_MODE
    @Published var ogretmenModu: Bool = false
    /// Android: touchBlockOverlay — doğrulama ve Google/Apple işlemi sürerken bütün dokunuşlar kapalı.
    @Published var mesgul: Bool = false
    @Published var bildirim: GirisBildirim? = nil
    /// Geri sayımlar için saat (saniyede bir güncellenir).
    @Published var simdiMs: Int64 = GirisYerelDepo.simdiMs()

    // MARK: Öğrenci girişi (LoginActivity)

    @Published var girisEposta: String = ""
    @Published var girisGonderiliyor: Bool = false

    // MARK: Yaş + kaynak (UserInfoFragment)

    @Published var yasMetni: String = ""
    @Published var bilgiAdimi: GirisBilgiAdimi = .yas
    @Published var seciliKaynak: String? = nil
    private var dogrulananDogumYili: Int? = nil

    // MARK: Kayıt (RegisterActivity)

    @Published var kayitEposta: String = ""
    @Published var kayitGonderiliyor: Bool = false
    @Published var ogretmenAd: String = ""
    @Published var ogretmenEposta: String = ""
    @Published var ogretmenSifre: String = ""
    @Published var ogretmenSifreTekrar: String = ""
    /// RegisterActivity.currentForcedRole == TEACHER
    @Published var kayitOgretmen: Bool = false
    private var kayitDogumYili: Int? = nil
    private var kayitKaynak: String? = nil
    /// AuthManager.pendingRegistrations karşılığı: kod doğrulanana kadar yalnızca bellekte.
    private var bekleyenKayit: GirisBekleyenKayit? = nil

    // MARK: Doğrulama kodu (OtpVerificationFragment)

    @Published var kod: String = ""
    @Published var kodDogrulaniyor: Bool = false
    @Published var kodYenidenGonderiliyor: Bool = false
    private var yanlisDeneme: Int32 = 0
    private var kilitEkrandaGoruldu: Bool = false

    // MARK: Öğretmen girişi ve şifre sıfırlama

    @Published var ogretmenGirisEposta: String = ""
    @Published var ogretmenGirisSifre: String = ""
    @Published var ogretmenGirisSuruyor: Bool = false
    @Published var sifirlamaEposta: String = ""
    @Published var sifirlamaGonderiliyor: Bool = false
    @Published var yeniSifre: String = ""
    @Published var yeniSifreTekrar: String = ""
    @Published var yeniSifreGuncelleniyor: Bool = false

    init(servis: KimlikServisi, depo: GirisYerelDepo, girisYapildi: @escaping () -> Void) {
        self.servis = servis
        self.depo = depo
        self.girisYapildi = girisYapildi
    }

    // MARK: Yığın

    var ekran: GirisEkran {
        return yigin.last ?? .baslangic
    }

    private func ac(_ yeni: GirisEkran) {
        yigin.append(yeni)
    }

    /// Sol üstteki geri düğmesi.
    func geri() {
        if mesgul { return }
        if ekran == .kullaniciBilgisi && bilgiAdimi == .kaynak {
            // UserInfoFragment: kaynak adımından yaş adımına dönülür.
            bilgiAdimi = .yas
            return
        }
        if yigin.count > 1 {
            yigin.removeLast()
        }
    }

    private func baslangicaDon() {
        depo.baslangicGosterildi = true
        yigin = [.baslangic]
    }

    // MARK: Bildirim

    /// Android Toast süreleri: kısa 2 sn, uzun 3,5 sn.
    func bildir(_ metin: String, uzun: Bool = false) {
        let yeni = GirisBildirim(id: UUID(), metin: metin)
        bildirim = yeni
        let sure: UInt64 = uzun ? 3_500_000_000 : 2_000_000_000
        Task { [weak self] in
            try? await Task.sleep(nanoseconds: sure)
            guard let model = self else { return }
            if model.bildirim?.id == yeni.id {
                model.bildirim = nil
            }
        }
    }

    // MARK: Saat

    /// Saniyede bir çağrılır; geri sayımları ilerletir ve biten kilidi temizler.
    func saatVurdu() {
        simdiMs = GirisYerelDepo.simdiMs()
        guard case .kod(let eposta, _) = ekran else { return }
        let kilitli = depo.yanlisDenemeKalanDakika(eposta: eposta, simdiMs: simdiMs) != nil
        if kilitli {
            kilitEkrandaGoruldu = true
        } else if kilitEkrandaGoruldu {
            // OtpVerificationFragment.wrongAttemptCooldownTimer.onFinish
            kilitEkrandaGoruldu = false
            yanlisDeneme = 0
            depo.yanlisDenemeTemizle()
            bildir(GirisMetin.tekrarDeneyebilirsiniz)
        }
    }

    // MARK: Açılış (SplashActivity)

    func acilisBasladi() async {
        let bekleme: Int64 = SplashRules.shared.SPLASH_ANIMATION_MS
        try? await Task.sleep(nanoseconds: UInt64(bekleme) * 1_000_000)
        guard ekran == .acilis else { return }

        // Android burada ağ durumuna da bakar (isOnline); ağ izleme iOS'ta henüz yok, çevrimiçi sayılır.
        let yol = SplashRules.shared.route(
            isOnline: true,
            loginStartEverShown: depo.baslangicGosterildi,
            hasExistingLogin: servis.mevcutOturum != nil
        )
        if yol == SplashRules.Route.checkSingleDevice {
            await tekCihazDenetimi()
        } else {
            // LOGIN_START ve GUEST_MAIN: Android ilk açılışta misafir olarak öğreticiye girer;
            // iOS'ta öğretici henüz olmadığı için başlangıç ekranı gösterilir.
            baslangicaDon()
        }
    }

    /// Android: SessionDeviceManager.requireLoggedInAndSingleDevice. Hem açılışta hem de her
    /// başarılı girişten sonra (MainActivity.onResume) çalışır.
    private func tekCihazDenetimi() async {
        let cihaz = depo.cihazKimligi()
        let veri: GirisTekCihazVerisi
        do {
            veri = try await servis.tekCihazVerisi()
        } catch {
            // Okuma hatası: izin verilmez, giriş ekranına dönülür.
            baslangicaDon()
            return
        }
        let kutuluZaman: KotlinLong? = veri.sonGuncellemeMs.map { KotlinLong(longLong: $0) }
        let karar = SessionDeviceRules.shared.decide(
            isLoggedIn: servis.mevcutOturum != nil,
            userDocExists: veri.belgeVar,
            activeDeviceId: veri.aktifCihazKimligi,
            lastUpdatedMs: kutuluZaman,
            deviceId: cihaz,
            nowMs: GirisYerelDepo.simdiMs()
        )
        if karar == SessionDeviceRules.Decision.allow {
            girisYapildi()
        } else if karar == SessionDeviceRules.Decision.claimAndAllow {
            await servis.oturumuBuCihazaAl(cihazKimligi: cihaz)
            girisYapildi()
        } else if karar == SessionDeviceRules.Decision.rejectOtherDevice {
            await servis.cikisYap()
            bildir(SessionDeviceRules.shared.MESSAGE_TRY_AGAIN_LATER)
            baslangicaDon()
        } else if karar == SessionDeviceRules.Decision.signOutAndRedirect {
            await servis.cikisYap()
            baslangicaDon()
        } else {
            baslangicaDon()
        }
    }

    /// Oturum açıldıktan sonra: tek cihaz kuralı, sonra dışarıya haber.
    private func girisTamam() async {
        await tekCihazDenetimi()
    }

    // MARK: Başlangıç (LoginStartActivity)

    func baslangicGoruldu() {
        depo.baslangicGosterildi = true
    }

    /// "GİRİŞ YAP"
    func baslangicGirisYap() {
        ac(ogretmenModu ? .ogretmenGiris : .ogrenciGiris)
    }

    /// "BAŞLA": önce yaş ve kaynak sorulur.
    func baslangicBasla() {
        kullaniciBilgisiniAc(epostaOnDoldurma: nil, ogretmen: ogretmenModu, yonlendirmeBildirimi: false)
    }

    func modDegistir() {
        ogretmenModu.toggle()
    }

    // MARK: Yaş + kaynak (UserInfoFragment)

    private func kullaniciBilgisiniAc(epostaOnDoldurma: String?, ogretmen: Bool, yonlendirmeBildirimi: Bool) {
        yasMetni = ""
        bilgiAdimi = .yas
        seciliKaynak = nil
        dogrulananDogumYili = nil
        kayitOgretmen = ogretmen
        kayitEposta = epostaOnDoldurma ?? ""
        ac(.kullaniciBilgisi)
        if yonlendirmeBildirimi {
            bildir(UserInfoRules.shared.MESSAGE_EMAIL_NOT_REGISTERED_REDIRECT, uzun: true)
        }
    }

    var bilgiDevamEtkin: Bool {
        switch bilgiAdimi {
        case .yas:
            return UserInfoRules.shared.isValidAge(rawAge: yasMetni)
        case .kaynak:
            return seciliKaynak != nil
        }
    }

    func kaynakSec(_ ad: String) {
        seciliKaynak = ad
    }

    func bilgiDevam() {
        switch bilgiAdimi {
        case .yas:
            let buYil = Int32(Calendar.current.component(.year, from: Date()))
            let dogumYili: Int32 = UserInfoRules.shared.birthYearFromRawAge(rawAge: yasMetni, currentYear: buYil)
            if dogumYili <= 0 {
                bildir(UserInfoRules.shared.MESSAGE_INVALID_AGE)
                return
            }
            dogrulananDogumYili = Int(dogumYili)
            bilgiAdimi = .kaynak
        case .kaynak:
            guard let yil = dogrulananDogumYili, let kaynak = seciliKaynak else { return }
            // Android burada oturum açıksa `users/{uid}` belgesine birthYear/acquisitionSource
            // yazmayı dener; kayıt akışında oturum kapalı olduğu için değerler kayıt ekranına taşınır.
            kayitDogumYili = yil
            kayitKaynak = kaynak
            ogretmenAd = ""
            ogretmenEposta = ""
            ogretmenSifre = ""
            ogretmenSifreTekrar = ""
            ac(.kayit)
        }
    }

    // MARK: Ortak: kod gönderimi öncesi beklemeler

    /// E-posta ekranındaki "Devam Et" düğmesinin geri sayımı (45 sn); 0 ise bekleme yok.
    func devamKalanSaniye(eposta: String) -> Int32 {
        let duzgun = eposta.trimmingCharacters(in: .whitespaces)
        let kalanMs = depo.yenidenGondermeKalanMs(eposta: duzgun, simdiMs: simdiMs)
        return Int32(max(0, kalanMs / 1000))
    }

    func devamBekliyor(eposta: String) -> Bool {
        let duzgun = eposta.trimmingCharacters(in: .whitespaces)
        return depo.yenidenGondermeKalanMs(eposta: duzgun, simdiMs: simdiMs) > 0
    }

    /// Kilit ya da bekleme varsa true döner (ve gerekiyorsa bildirim gösterir).
    private func gonderimEngelli(normalEposta: String) -> Bool {
        let simdi = GirisYerelDepo.simdiMs()
        if let dakika = depo.yanlisDenemeKalanDakika(eposta: normalEposta, simdiMs: simdi) {
            bildir(OtpRules.shared.wrongCooldownTryLaterMessage(remainingMin: dakika), uzun: true)
            return true
        }
        if depo.yenidenGondermeKalanMs(eposta: normalEposta, simdiMs: simdi) > 0 {
            simdiMs = simdi
            return true
        }
        return false
    }

    private func kodEkraniniAc(eposta: String, tur: GirisKodTuru) {
        kod = ""
        kodDogrulaniyor = false
        kodYenidenGonderiliyor = false
        yanlisDeneme = 0
        kilitEkrandaGoruldu = false
        // OtpVerificationFragment.onViewCreated → startResendCooldown (kilit yoksa).
        let simdi = GirisYerelDepo.simdiMs()
        if depo.yanlisDenemeKalanDakika(eposta: eposta, simdiMs: simdi) == nil {
            depo.yenidenGondermeBaslat(eposta: eposta, simdiMs: simdi)
        }
        simdiMs = simdi
        ac(.kod(eposta: eposta, tur: tur))
    }

    // MARK: Öğrenci girişi (LoginActivity)

    func girisDevam() async {
        if mesgul || girisGonderiliyor { return }
        let kurallar = AuthRules.shared
        if let hata = kurallar.validateLoginEmail(rawEmail: girisEposta) {
            bildir(hata)
            return
        }
        let eposta = girisEposta.trimmingCharacters(in: .whitespaces)
        let normal = kurallar.normalizeEmail(email: eposta)
        if gonderimEngelli(normalEposta: normal) { return }

        girisGonderiliyor = true
        defer { girisGonderiliyor = false }
        do {
            try await servis.dogrulamaKoduGonder(eposta: normal, amac: kurallar.OTP_PURPOSE_LOGIN)
            kodEkraniniAc(eposta: eposta, tur: .giris)
        } catch let hata as GirisHatasi {
            if hata.mesaj == kurallar.MESSAGE_USER_NOT_FOUND {
                // Kayıtlı değil: yaş/kaynak adımından kayıt ekranına.
                kullaniciBilgisiniAc(epostaOnDoldurma: eposta, ogretmen: false, yonlendirmeBildirimi: true)
            } else {
                bildir(hata.mesaj ?? kurallar.MESSAGE_CODE_NOT_SENT)
            }
        } catch {
            bildir(kurallar.MESSAGE_CODE_NOT_SENT)
        }
    }

    /// Giriş ekranındaki Google / Apple düğmesi (LoginActivity.onActivityResult).
    func saglayiciIleGiris(apple: Bool) async {
        if mesgul { return }
        mesgul = true
        do {
            if apple {
                _ = try await servis.appleIleGiris(otomatikKayit: false, dogumYili: nil, kaynak: nil)
            } else {
                _ = try await servis.googleIleGiris(otomatikKayit: false, dogumYili: nil, kaynak: nil)
            }
            await girisTamam()
            mesgul = false
        } catch let hata as GirisHatasi {
            mesgul = false
            let eylem = AuthRules.shared.googleLoginFailureAction(error: hata.mesaj)
            if eylem == AuthRules.GoogleLoginFailureAction.showUserInfo {
                kullaniciBilgisiniAc(epostaOnDoldurma: hata.hesapEposta, ogretmen: false, yonlendirmeBildirimi: false)
            } else if eylem == AuthRules.GoogleLoginFailureAction.showError {
                bildir(hata.mesaj ?? (apple ? GirisMetin.appleGirisBasarisiz : LoginFlowTexts.shared.MESSAGE_GOOGLE_LOGIN_FAILED))
            }
        } catch {
            mesgul = false
            bildir(apple ? GirisMetin.appleGirisBasarisiz : LoginFlowTexts.shared.MESSAGE_GOOGLE_LOGIN_FAILED)
        }
    }

    // MARK: Kayıt (RegisterActivity)

    /// Öğrenci kaydı: "Devam Et" (RegisterActivity.sendOtpAndShowCodeStep).
    func kayitDevam() async {
        if mesgul || kayitGonderiliyor { return }
        let kurallar = AuthRules.shared
        if let hata = kurallar.validateRegisterEmail(rawEmail: kayitEposta) {
            bildir(hata)
            return
        }
        let eposta = kayitEposta.trimmingCharacters(in: .whitespaces)
        let normal = kurallar.normalizeEmail(email: eposta)
        if gonderimEngelli(normalEposta: normal) { return }

        kayitGonderiliyor = true
        defer { kayitGonderiliyor = false }

        let durum = await servis.epostaKayitliMi(eposta: normal)
        let akis = kurallar.studentEmailFlow(alreadyRegistered: durum.kayitli, uid: durum.uid, role: durum.rol)

        if akis == AuthRules.StudentEmailFlow.rejectTeacher {
            bildir(kurallar.MESSAGE_TEACHER_EMAIL_FOR_STUDENT)
            return
        }
        if akis == AuthRules.StudentEmailFlow.errorCodeNotSent {
            bildir(kurallar.MESSAGE_CODE_NOT_SENT)
            return
        }
        let zatenKayitli = akis == AuthRules.StudentEmailFlow.sendLoginCode
        let amac: String = kurallar.resendPurpose(isRegistration: !zatenKayitli)
        if !zatenKayitli {
            // AuthManager.createPendingRegistrationForOTP: öğrencide ad ve şifre boş.
            bekleyenKayit = GirisBekleyenKayit(ad: "", sifre: "", rol: kurallar.ROLE_STUDENT)
        }
        do {
            try await servis.dogrulamaKoduGonder(eposta: normal, amac: amac)
            if zatenKayitli {
                // Zaten kayıtlı öğrenci: giriş kodu gönderildi.
                kodEkraniniAc(eposta: normal, tur: .giris)
            } else {
                kodEkraniniAc(eposta: eposta, tur: .kayit)
            }
        } catch let hata as GirisHatasi {
            bildir(hata.mesaj ?? kurallar.MESSAGE_CODE_NOT_SENT)
        } catch {
            bildir(kurallar.MESSAGE_CODE_NOT_SENT)
        }
    }

    /// Öğretmen kaydı: "Devam Et" (RegisterActivity.registerTeacherWithOtp).
    func ogretmenKayitDevam() async {
        if mesgul || kayitGonderiliyor { return }
        let kurallar = AuthRules.shared
        if let hata = kurallar.validateTeacherRegistration(
            rawName: ogretmenAd,
            rawEmail: ogretmenEposta,
            password: ogretmenSifre,
            passwordConfirm: ogretmenSifreTekrar
        ) {
            bildir(hata)
            return
        }
        let ad = ogretmenAd.trimmingCharacters(in: .whitespaces)
        let eposta = ogretmenEposta.trimmingCharacters(in: .whitespaces)
        let sifre = ogretmenSifre

        kayitGonderiliyor = true
        defer { kayitGonderiliyor = false }

        let durum = await servis.epostaKayitliMi(eposta: eposta)
        if durum.kayitli {
            bildir(kurallar.teacherEmailAlreadyRegisteredMessage(role: durum.rol))
            return
        }
        // AuthManager.createPendingTeacherRegistrationForOTP
        if !kurallar.isValidPendingTeacherRegistration(email: eposta, name: ad, password: sifre) {
            bildir(kurallar.MESSAGE_INVALID_INFO)
            return
        }
        bekleyenKayit = GirisBekleyenKayit(ad: ad, sifre: sifre, rol: kurallar.ROLE_TEACHER)
        do {
            try await servis.dogrulamaKoduGonder(
                eposta: kurallar.normalizeEmail(email: eposta),
                amac: kurallar.OTP_PURPOSE_REGISTER
            )
            kodEkraniniAc(eposta: eposta, tur: .kayit)
        } catch let hata as GirisHatasi {
            bildir(hata.mesaj ?? kurallar.MESSAGE_CODE_NOT_SENT)
        } catch {
            bildir(kurallar.MESSAGE_CODE_NOT_SENT)
        }
    }

    /// Kayıt ekranındaki Google / Apple düğmesi (otomatik kayıt).
    func saglayiciIleKayit(apple: Bool) async {
        if mesgul { return }
        mesgul = true
        let varsayilan: String = apple ? GirisMetin.appleKayitBasarisiz : LoginFlowTexts.shared.MESSAGE_GOOGLE_REGISTER_FAILED
        do {
            if apple {
                _ = try await servis.appleIleGiris(otomatikKayit: true, dogumYili: kayitDogumYili, kaynak: kayitKaynak)
            } else {
                _ = try await servis.googleIleGiris(otomatikKayit: true, dogumYili: kayitDogumYili, kaynak: kayitKaynak)
            }
            await girisTamam()
            mesgul = false
        } catch let hata as GirisHatasi {
            mesgul = false
            // Kullanıcı vazgeçtiyse sessiz kalınır.
            if hata.mesaj != AuthRules.shared.MESSAGE_GOOGLE_CANCELLED {
                bildir(hata.mesaj ?? varsayilan)
            }
        } catch {
            mesgul = false
            bildir(varsayilan)
        }
    }

    // MARK: Doğrulama kodu (OtpVerificationFragment)

    func kodKilitli(eposta: String) -> Bool {
        return depo.yanlisDenemeKalanDakika(eposta: eposta, simdiMs: simdiMs) != nil
    }

    /// "Kodu tekrar gönder" düğmesinin metni.
    func yenidenGonderMetni(eposta: String) -> String {
        if kodKilitli(eposta: eposta) {
            let kalanSn = Int(max(0, (depo.yanlisDenemeBitisMs() - simdiMs) / 1000))
            return GirisMetin.kilitGeriSayim(dakika: kalanSn / 60, saniye: kalanSn % 60)
        }
        let kalanMs = depo.yenidenGondermeKalanMs(eposta: eposta, simdiMs: simdiMs)
        if kalanMs > 0 {
            return GirisMetin.yenidenGonderGeriSayim(saniye: Int(kalanMs / 1000))
        }
        return GirisMetin.koduTekrarGonder
    }

    func yenidenGonderEtkin(eposta: String) -> Bool {
        if kodYenidenGonderiliyor || kodKilitli(eposta: eposta) { return false }
        return depo.yenidenGondermeKalanMs(eposta: eposta, simdiMs: simdiMs) <= 0
    }

    func dogrulaEtkin(eposta: String) -> Bool {
        if kodDogrulaniyor || kodKilitli(eposta: eposta) { return false }
        return yanlisDeneme < OtpRules.shared.MAX_WRONG_ATTEMPTS
    }

    private func kilidiBaslat(eposta: String) {
        let simdi = GirisYerelDepo.simdiMs()
        depo.yanlisDenemeBaslat(eposta: eposta, simdiMs: simdi)
        depo.yenidenGondermeTemizle()
        kilitEkrandaGoruldu = true
        simdiMs = simdi
    }

    func koduYenidenGonder(eposta: String, tur: GirisKodTuru) async {
        if !yenidenGonderEtkin(eposta: eposta) { return }
        let kurallar = AuthRules.shared
        kodYenidenGonderiliyor = true
        defer { kodYenidenGonderiliyor = false }
        let amac: String
        switch tur {
        case .sifreSifirlama:
            amac = kurallar.OTP_PURPOSE_TEACHER_RESET
        case .kayit:
            amac = kurallar.resendPurpose(isRegistration: true)
        case .giris:
            amac = kurallar.resendPurpose(isRegistration: false)
        }
        do {
            try await servis.dogrulamaKoduGonder(eposta: kurallar.normalizeEmail(email: eposta), amac: amac)
            bildir(LoginFlowTexts.shared.MESSAGE_CODE_RESENT)
            let simdi = GirisYerelDepo.simdiMs()
            depo.yenidenGondermeBaslat(eposta: eposta, simdiMs: simdi)
            simdiMs = simdi
        } catch let hata as GirisHatasi {
            bildir(hata.mesaj ?? kurallar.MESSAGE_CODE_NOT_SENT, uzun: true)
        } catch {
            bildir(kurallar.MESSAGE_CODE_NOT_SENT, uzun: true)
        }
    }

    func koduDogrula(eposta: String, tur: GirisKodTuru) async {
        if mesgul || kodDogrulaniyor || kodKilitli(eposta: eposta) { return }
        let otp = OtpRules.shared
        if yanlisDeneme >= otp.MAX_WRONG_ATTEMPTS {
            kilidiBaslat(eposta: eposta)
            bildir(otp.MESSAGE_TOO_MANY_WRONG, uzun: true)
            return
        }
        if !otp.isValidCodeLength(rawCode: kod) {
            bildir(otp.MESSAGE_CODE_LENGTH)
            return
        }
        let girilen = kod.trimmingCharacters(in: .whitespaces)

        kodDogrulaniyor = true
        mesgul = true
        do {
            switch tur {
            case .sifreSifirlama:
                try await servis.sifreSifirlamaKodunuDogrula(eposta: eposta, kod: girilen)
                kodDogrulaniyor = false
                mesgul = false
                yeniSifre = ""
                yeniSifreTekrar = ""
                ac(.yeniSifre(eposta: eposta, kod: girilen))
            case .kayit:
                _ = try await servis.kayitKodunuDogrula(
                    eposta: eposta,
                    kod: girilen,
                    bekleyen: bekleyenKayit,
                    dogumYili: kayitDogumYili,
                    kaynak: kayitKaynak
                )
                bekleyenKayit = nil
                await girisTamam()
                kodDogrulaniyor = false
                mesgul = false
            case .giris:
                _ = try await servis.girisKodunuDogrula(eposta: eposta, kod: girilen)
                await girisTamam()
                kodDogrulaniyor = false
                mesgul = false
            }
        } catch let hata as GirisHatasi {
            dogrulamaBasarisiz(eposta: eposta, mesaj: hata.mesaj)
        } catch {
            dogrulamaBasarisiz(eposta: eposta, mesaj: nil)
        }
    }

    /// OtpVerificationFragment.verify içindeki onVerifyFailed.
    private func dogrulamaBasarisiz(eposta: String, mesaj: String?) {
        kodDogrulaniyor = false
        mesgul = false
        let otp = OtpRules.shared
        let sonuc = otp.onVerifyFailed(currentWrongAttempts: yanlisDeneme, error: mesaj)
        yanlisDeneme = sonuc.wrongAttempts
        if sonuc.verifyEnabled {
            bildir(mesaj ?? AuthRules.shared.MESSAGE_CODE_NOT_VERIFIED, uzun: true)
        } else {
            bildir(otp.MESSAGE_TOO_MANY_WRONG, uzun: true)
        }
        if sonuc.startCooldown {
            kilidiBaslat(eposta: eposta)
        }
    }

    // MARK: Öğretmen girişi (TeacherLoginActivity)

    func ogretmenGiris() async {
        if mesgul || ogretmenGirisSuruyor { return }
        if let hata = TeacherLoginRules.shared.validateLogin(rawEmail: ogretmenGirisEposta, password: ogretmenGirisSifre) {
            bildir(hata)
            return
        }
        let eposta = ogretmenGirisEposta.trimmingCharacters(in: .whitespaces)
        ogretmenGirisSuruyor = true
        do {
            _ = try await servis.sifreIleGiris(eposta: eposta, sifre: ogretmenGirisSifre)
            await girisTamam()
            ogretmenGirisSuruyor = false
        } catch let hata as GirisHatasi {
            ogretmenGirisSuruyor = false
            bildir(hata.mesaj ?? AuthRules.shared.MESSAGE_WRONG_PASSWORD_OR_EMAIL)
        } catch {
            ogretmenGirisSuruyor = false
            bildir(AuthRules.shared.MESSAGE_WRONG_PASSWORD_OR_EMAIL)
        }
    }

    func sifremiUnuttumAc() {
        if ogretmenGirisSuruyor { return }
        sifirlamaEposta = ""
        ac(.sifremiUnuttum)
    }

    /// TeacherForgotPasswordEmailFragment: "Devam Et"
    func sifirlamaDevam() async {
        if mesgul || sifirlamaGonderiliyor { return }
        let kurallar = AuthRules.shared
        if let hata = TeacherLoginRules.shared.validateForgotPasswordEmail(rawEmail: sifirlamaEposta) {
            bildir(hata)
            return
        }
        let normal = kurallar.normalizeEmail(email: sifirlamaEposta)
        sifirlamaGonderiliyor = true
        mesgul = true
        defer {
            sifirlamaGonderiliyor = false
            mesgul = false
        }
        do {
            try await servis.dogrulamaKoduGonder(eposta: normal, amac: kurallar.OTP_PURPOSE_TEACHER_RESET)
            kodEkraniniAc(eposta: normal, tur: .sifreSifirlama)
        } catch let hata as GirisHatasi {
            bildir(hata.mesaj ?? kurallar.MESSAGE_CODE_NOT_SENT, uzun: true)
        } catch {
            bildir(kurallar.MESSAGE_CODE_NOT_SENT, uzun: true)
        }
    }

    /// TeacherNewPasswordFragment: "Şifreyi güncelle"
    func sifreyiGuncelle(eposta: String, kod: String) async {
        if mesgul || yeniSifreGuncelleniyor { return }
        if let hata = TeacherLoginRules.shared.validateNewPassword(newPassword: yeniSifre, confirm: yeniSifreTekrar) {
            bildir(hata)
            return
        }
        let sifre = yeniSifre
        yeniSifreGuncelleniyor = true
        defer { yeniSifreGuncelleniyor = false }
        do {
            try await servis.sifreyiSifirla(eposta: eposta, kod: kod, yeniSifre: sifre)
        } catch let hata as GirisHatasi {
            bildir(hata.mesaj ?? AuthRules.shared.MESSAGE_PASSWORD_NOT_UPDATED, uzun: true)
            return
        } catch {
            bildir(AuthRules.shared.MESSAGE_PASSWORD_NOT_UPDATED, uzun: true)
            return
        }
        // Şifre güncellendi: yeni şifreyle hemen giriş denenir.
        do {
            _ = try await servis.sifreIleGiris(eposta: eposta, sifre: sifre)
            await girisTamam()
        } catch {
            bildir(TeacherLoginRules.shared.MESSAGE_PASSWORD_UPDATED_LOGIN_MANUALLY, uzun: true)
        }
    }
}

/// Giriş ekranlarının strings.xml metinleri (Metin.al ile okunur) ve yalnızca iOS için eklenen metinler.
enum GirisMetin {
    /// strings.xml: otp_resend_button
    static var koduTekrarGonder: String { Metin.al("otp_resend_button") }
    /// strings.xml: otp_can_retry
    static var tekrarDeneyebilirsiniz: String { Metin.al("otp_can_retry") }
    /// strings.xml: login_start_student_title / login_start_teacher_title
    static var ogrenciBaslik: String { Metin.al("login_start_student_title") }
    static var ogretmenBaslik: String { Metin.al("login_start_teacher_title") }
    /// strings.xml: login_start_student_subtitle / login_start_teacher_subtitle
    static var ogrenciAltBaslik: String { Metin.al("login_start_student_subtitle") }
    static var ogretmenAltBaslik: String { Metin.al("login_start_teacher_subtitle") }
    /// strings.xml: login_start_primary_button / login_start_secondary_button
    static var girisYap: String { Metin.al("login_start_primary_button") }
    static var basla: String { Metin.al("login_start_secondary_button") }
    /// strings.xml: login_start_teacher_mode_button / login_start_student_mode_button
    static var ogretmenGirisi: String { Metin.al("login_start_teacher_mode_button") }
    static var ogrenciGirisi: String { Metin.al("login_start_student_mode_button") }

    /// strings.xml: otp_resend_cooldown ("%1$d sn")
    static func yenidenGonderGeriSayim(saniye: Int) -> String {
        return Metin.al("otp_resend_cooldown", saniye)
    }

    /// strings.xml: otp_wrong_cooldown_btn ("%1$d dk %2$d sn sonra tekrar")
    static func kilitGeriSayim(dakika: Int, saniye: Int) -> String {
        return Metin.al("otp_wrong_cooldown_btn", dakika, saniye)
    }

    // Android'de karşılığı olmayan metinler (Apple ile giriş yalnızca iOS'ta var).
    static let appleIleGirisYap = "Apple ile Giriş Yap"
    static let appleIleKayitOl = "Apple ile Kayıt Ol"
    static let appleGirisBasarisiz = "Apple girişi başarısız"
    static let appleKayitBasarisiz = "Apple kaydı başarısız"
}
