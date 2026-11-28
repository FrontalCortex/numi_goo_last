import Foundation
import Shared
import SwiftUI

// Giriş akışının durumu ve adımlar arası geçişler.
// Android karşılıkları: SplashActivity, LoginStartActivity, LoginActivity, UserInfoFragment,
// RegisterActivity, ParentConsentFragment, OtpVerificationFragment, TeacherLoginActivity,
// TeacherForgotPasswordEmailFragment, TeacherNewPasswordFragment.
// Doğrulama kuralları, mesajlar ve kararlar ortak koddan çağrılır (AuthRules, OtpRules,
// SessionDeviceRules, SplashRules, UserInfoRules, SignupFlowRules, TeacherLoginRules, LoginFlowTexts).
//
// Bu dosya: genel durum, açılış, başlangıç, öğrenci girişi ve kayıt sorularının açılması.
// GirisModeliKayit.swift: kayıt, veli onayı (13 yaş altı) ve doğrulama kodu.
// GirisModeliOgretmen.swift: öğretmen girişi ve şifre sıfırlama.
// Uzantılar ayrı dosyalarda olduğu için durum alanları `private` değildir.

/// Doğrulama kodu ekranının türü (OtpVerificationFragment argümanları).
enum GirisKodTuru: Hashable {
    /// Kayıtlı öğrencinin giriş kodu (isRegistration = false).
    case giris
    /// Kayıt kodu (isRegistration = true).
    case kayit
    /// Öğretmen şifre sıfırlama (forPasswordReset).
    case sifreSifirlama
    /// 13 yaş altı veli onay kodu (parentEmail): kod veliye gitti. `saglayici` true ise Google /
    /// Apple kaydıdır ve hesap confirmParentalConsent ile açılır (googleParental).
    case veli(veliEposta: String, saglayici: Bool)
    /// 13 yaş altı, veli adresi farklı: önce çocuğun KENDİ adresinin kodu (pendingParentEmail);
    /// doğrulanınca veliye kod gider ve veli kodu ekranına geçilir.
    case cocukEpostasi(veliEposta: String)
}

/// Akıştaki ekranlar. Yığının tepesindeki ekran gösterilir.
enum GirisEkran: Hashable {
    /// SplashActivity
    case acilis
    /// LoginStartActivity
    case baslangic
    /// LoginActivity (e-posta adımı)
    case ogrenciGiris
    /// UserInfoFragment (kayıt soruları; durumu `GirisModeli.sorular`)
    case sorular
    /// RegisterActivity
    case kayit
    /// ParentConsentFragment (bağlamı `GirisModeli.veliBaglami`)
    case veliOnayi
    /// OtpVerificationFragment
    case kod(eposta: String, tur: GirisKodTuru)
    /// TeacherLoginActivity
    case ogretmenGiris
    /// TeacherForgotPasswordEmailFragment
    case sifremiUnuttum
    /// TeacherNewPasswordFragment
    case yeniSifre(eposta: String, kod: String)
}

/// Hesapsız ilk açılışta (SplashRules.Route.GUEST_MAIN) misafirin gideceği yer
/// (Android: MainActivity.START_DESTINATION_TUTORIAL / START_DESTINATION_MAP).
enum GirisMisafirHedefi {
    /// Bu cihazda hiç hesap açılmadı: ilk öğretici (SplashRules.showsFirstTutorial).
    case ogretici
    /// Daha önce hesap açılmış (oturum kapalı): harita.
    case harita
}

/// Veli onayı ekranının bağlamı (ParentConsentFragment argümanları).
struct GirisVeliBaglami {
    /// Normalleştirilmiş çocuk e-postası (Google / Apple'da hesabın adresi).
    let cocukEposta: String
    /// Google / Apple ile kayıt mı (isGoogle).
    let saglayici: Bool
    let dogumYili: Int
    let kaynak: String?
}

/// Kısa bildirim (Android: Toast / Snackbar).
struct GirisBildirim: Identifiable, Equatable {
    let id: UUID
    let metin: String
}

@MainActor
final class GirisModeli: ObservableObject {
    let servis: KimlikServisi
    let depo: GirisYerelDepo
    let bildirimServisi: GirisBildirimServisi
    let surumServisi: GirisSurumServisi
    let girisYapildi: () -> Void
    let misafirOlarakDevam: (GirisMisafirHedefi) -> Void
    /// Ekran galerisi: açılış yönlendirme yapmaz, bayraklar yazılmaz.
    var ornekKipi: Bool = false

    // MARK: Genel durum

    @Published var yigin: [GirisEkran]
    /// LoginStartActivity.EXTRA_TEACHER_MODE
    @Published var ogretmenModu: Bool = false
    /// Android: touchBlockOverlay — doğrulama ve Google/Apple işlemi sürerken bütün dokunuşlar kapalı.
    @Published var mesgul: Bool = false
    @Published var bildirim: GirisBildirim? = nil
    /// Geri sayımlar için saat (saniyede bir güncellenir).
    @Published var simdiMs: Int64 = GirisYerelDepo.simdiMs()
    /// Zorunlu güncelleme kilidi (ForceUpdateManager.showBlockingDialog).
    @Published var surumKilitli: Bool = false

    // MARK: Öğrenci girişi (LoginActivity)

    @Published var girisEposta: String = ""
    @Published var girisGonderiliyor: Bool = false

    // MARK: Kayıt soruları (UserInfoFragment)

    /// Açık soru akışı; kayıt ekranından geri dönüldüğünde aynı durumla görünür.
    @Published var sorular: GirisSoruModeli? = nil

    // MARK: Kayıt (RegisterActivity)

    @Published var kayitEposta: String = ""
    @Published var kayitGonderiliyor: Bool = false
    @Published var ogretmenAd: String = ""
    @Published var ogretmenEposta: String = ""
    @Published var ogretmenSifre: String = ""
    @Published var ogretmenSifreTekrar: String = ""
    /// RegisterActivity.currentForcedRole == TEACHER
    @Published var kayitOgretmen: Bool = false
    var kayitDogumYili: Int? = nil
    var kayitKaynak: String? = nil
    /// AuthManager.pendingRegistrations karşılığı: kod doğrulanana kadar yalnızca bellekte.
    var bekleyenKayit: GirisBekleyenKayit? = nil

    // MARK: Veli onayı (ParentConsentFragment)

    @Published var veliEposta: String = ""
    @Published var veliGonderiliyor: Bool = false
    var veliBaglami: GirisVeliBaglami? = nil

    // MARK: Doğrulama kodu (OtpVerificationFragment)

    @Published var kod: String = ""
    @Published var kodDogrulaniyor: Bool = false
    @Published var kodYenidenGonderiliyor: Bool = false
    var yanlisDeneme: Int32 = 0
    var kilitEkrandaGoruldu: Bool = false

    // MARK: Öğretmen girişi ve şifre sıfırlama

    @Published var ogretmenGirisEposta: String = ""
    @Published var ogretmenGirisSifre: String = ""
    @Published var ogretmenGirisSuruyor: Bool = false
    @Published var sifirlamaEposta: String = ""
    @Published var sifirlamaGonderiliyor: Bool = false
    @Published var yeniSifre: String = ""
    @Published var yeniSifreTekrar: String = ""
    @Published var yeniSifreGuncelleniyor: Bool = false

    init(
        servis: KimlikServisi,
        depo: GirisYerelDepo,
        bildirimServisi: GirisBildirimServisi,
        surumServisi: GirisSurumServisi,
        baslangic: GirisEkran,
        girisYapildi: @escaping () -> Void,
        misafirOlarakDevam: @escaping (GirisMisafirHedefi) -> Void
    ) {
        self.servis = servis
        self.depo = depo
        self.bildirimServisi = bildirimServisi
        self.surumServisi = surumServisi
        self.yigin = [baslangic]
        self.girisYapildi = girisYapildi
        self.misafirOlarakDevam = misafirOlarakDevam
    }

    // MARK: Yığın

    var ekran: GirisEkran {
        return yigin.last ?? .baslangic
    }

    func ac(_ yeni: GirisEkran) {
        yigin.append(yeni)
    }

    /// Sol üstteki geri düğmesi (kayıt sorularında kenardan kaydırma).
    func geri() {
        if mesgul { return }
        if ekran == .sorular, let akis = sorular {
            // UserInfoFragment.onBackStep: önce bir önceki soruya; ilk adımda `kapat` ile çıkılır.
            akis.geri()
            return
        }
        if yigin.count > 1 {
            yigin.removeLast()
        }
        kayitEkraninaDonulduyse()
    }

    /// RegisterActivity.btnBack: e-posta adımına dönüldüyse veli onayı bağlamı biter; Google / Apple
    /// kaydının yarım kalan oturumu silinir (deleteIncompleteAuthUser).
    func kayitEkraninaDonulduyse() {
        guard ekran == .kayit, let baglam = veliBaglami else { return }
        veliBaglami = nil
        if baglam.saglayici {
            Task { await servis.yarimKaydiSil() }
        }
    }

    func baslangicaDon() {
        sorular = nil
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

    /// Açılış: en az SPLASH_ANIMATION_MS (1200 ms) marka; zorunlu güncelleme kontrolü animasyonla
    /// paralel çekilir ve ardından en fazla FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS bekletir; sonra
    /// SplashRules.route ile yönlendirilir.
    func acilisBasladi() async {
        if ornekKipi { return }
        surumServisi.cekmeyiBaslat()
        let bekleme: Int64 = SplashRules.shared.SPLASH_ANIMATION_MS
        try? await Task.sleep(nanoseconds: UInt64(bekleme) * 1_000_000)
        YonetimGunlugu.yaz("acilis bekleme bitti (ekran \(String(describing: ekran)))")
        guard ekran == .acilis else { return }
        let azami: Int64 = SplashRules.shared.FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS
        if await surumServisi.kilitliMi(azamiBeklemeMs: azami) {
            surumKilitli = true
            return
        }
        await acilisiYonlendir()
    }

    /// SplashActivity.checkLoginStatus.
    private func acilisiYonlendir() async {
        let oturumVar: Bool = servis.mevcutOturum != nil
        // Android burada ağ durumuna da bakar (isOnline → OFFLINE_MAIN: ana ekran çevrimdışı
        // ekranını gösterir); ağ izleme iOS'ta henüz yok, çevrimiçi sayılır.
        let yol = SplashRules.shared.route(
            isOnline: true,
            loginStartEverShown: depo.baslangicGosterildi,
            hasExistingLogin: oturumVar
        )
        YonetimGunlugu.yaz("acilis yonlendirmesi: \(yol.name) (oturum \(oturumVar))")
        if yol == SplashRules.Route.checkSingleDevice {
            await tekCihazDenetimi()
        } else if yol == SplashRules.Route.guestMain {
            // İlk açılış, oturum yok: misafir olarak ana ekran. Şart tek: bu cihazda daha önce
            // hesap açıldı mı (prepareStartupAndLaunchMain).
            let gecmis: Bool = depo.hesapGecmisiVar(oturumAcik: false)
            let ogretici: Bool = SplashRules.shared.showsFirstTutorial(hadAccount: gecmis)
            misafirOlarakDevam(ogretici ? .ogretici : .harita)
        } else {
            // LOGIN_START (OFFLINE_MAIN'e düşülmez, yukarıya bakın).
            baslangicaDon()
        }
    }

    /// Android: SessionDeviceManager.requireLoggedInAndSingleDevice. Hem açılışta hem de her
    /// başarılı girişten sonra (MainActivity.onResume) çalışır.
    func tekCihazDenetimi() async {
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
            oturumAcildi()
        } else if karar == SessionDeviceRules.Decision.claimAndAllow {
            await servis.oturumuBuCihazaAl(cihazKimligi: cihaz)
            oturumAcildi()
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

    /// Oturum açık ve bu cihaza izinli: hesap geçmişi bayrağı kalıcılaşır (DeviceAccountStore
    /// okuması oturum açıkken yazar), sonra dışarıya haber verilir.
    private func oturumAcildi() {
        _ = depo.hesapGecmisiVar(oturumAcik: true)
        girisYapildi()
    }

    /// Oturum açıldıktan sonra: tek cihaz kuralı, sonra dışarıya haber.
    func girisTamam() async {
        await tekCihazDenetimi()
    }

    // MARK: Başlangıç (LoginStartActivity)

    func baslangicGoruldu() {
        if ornekKipi { return }
        depo.baslangicGosterildi = true
    }

    /// "Zaten hesabım var"
    func baslangicGirisYap() {
        ac(ogretmenModu ? .ogretmenGiris : .ogrenciGiris)
    }

    /// "Başla": önce kayıt soruları (UserInfoFragment).
    func baslangicBasla() {
        sorulariAc(ogretmen: ogretmenModu, onDoldurmaEposta: nil, yonlendirmeBildirimi: false)
    }

    /// Sağ üstteki "Öğretmen girişi" / "Öğrenci girişi" (Android ekranı solarak yeniden açar).
    func modDegistir() {
        withAnimation(.easeInOut(duration: 0.3)) {
            ogretmenModu.toggle()
        }
    }

    // MARK: Kayıt soruları (UserInfoFragment)

    /// - Parameters:
    ///   - ogretmen: forceTeacher (öğretmene yalnızca yaş ve kaynak sorulur).
    ///   - onDoldurmaEposta: girişte kayıtlı çıkmayan e-posta ya da Google/Apple hesabının adresi;
    ///     kayıt ekranının e-posta alanına konur.
    ///   - yonlendirmeBildirimi: "Bu e-posta adresi kayıtlı değil…" (Snackbar, uzun).
    func sorulariAc(ogretmen: Bool, onDoldurmaEposta: String?, yonlendirmeBildirimi: Bool) {
        kayitOgretmen = ogretmen
        kayitEposta = onDoldurmaEposta ?? ""
        sorular = GirisSoruModeli(
            ogretmen: ogretmen,
            depo: depo,
            bildirimServisi: bildirimServisi,
            bildir: { [weak self] metin in self?.bildir(metin) },
            kapat: { [weak self] in self?.sorulariKapat() },
            bitti: { [weak self] sonuc in self?.sorularBitti(sonuc) }
        )
        ac(.sorular)
        if yonlendirmeBildirimi {
            bildir(Metin.al("ui_bu_e_posta_adresi_kayitli_degil"), uzun: true)
        }
    }

    /// İlk soruda geri: soru ekranı kapanır (parentFragmentManager.popBackStack).
    private func sorulariKapat() {
        if ekran == .sorular && yigin.count > 1 {
            yigin.removeLast()
        }
        sorular = nil
    }

    /// UserInfoFragment.saveBirthYearAndProceed → navigateToRegister. Oturum kapalıyken değerler
    /// kayıt ekranına taşınır (doğum yılı kayıt çağrısıyla sunucuya gider).
    private func sorularBitti(_ sonuc: GirisSoruSonucu) {
        kayitDogumYili = sonuc.dogumYili
        kayitKaynak = sonuc.kaynak
        ogretmenAd = ""
        ogretmenEposta = ""
        ogretmenSifre = ""
        ogretmenSifreTekrar = ""
        veliBaglami = nil
        ac(.kayit)
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
    func gonderimEngelli(normalEposta: String) -> Bool {
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

    func kodEkraniniAc(eposta: String, tur: GirisKodTuru) {
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
                // Kayıtlı değil: kayıt sorularından kayıt ekranına (LoginActivity.showUserInfoFragment:
                // forceStudent, e-posta önceden doldurulur).
                sorulariAc(ogretmen: false, onDoldurmaEposta: eposta, yonlendirmeBildirimi: true)
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
                sorulariAc(ogretmen: false, onDoldurmaEposta: hata.hesapEposta, yonlendirmeBildirimi: false)
            } else if eylem == AuthRules.GoogleLoginFailureAction.showError {
                bildir(hata.mesaj ?? (apple ? GirisMetin.appleGirisBasarisiz : Metin.al("ui_google_girisi_basarisiz")))
            }
        } catch {
            mesgul = false
            bildir(apple ? GirisMetin.appleGirisBasarisiz : Metin.al("ui_google_girisi_basarisiz"))
        }
    }
}

/// Giriş ekranlarının strings.xml metinleri (Metin.al ile okunur) ve Android'de koda gömülü ya da
/// yalnızca iOS için eklenen metinler.
enum GirisMetin {
    /// strings.xml: otp_resend_button
    static var koduTekrarGonder: String { Metin.al("otp_resend_button") }
    /// strings.xml: otp_can_retry
    static var tekrarDeneyebilirsiniz: String { Metin.al("otp_can_retry") }

    /// strings.xml: otp_resend_cooldown ("%1$d sn")
    static func yenidenGonderGeriSayim(saniye: Int) -> String {
        return Metin.al("otp_resend_cooldown", saniye)
    }

    /// strings.xml: otp_wrong_cooldown_btn ("%1$d dk %2$d sn sonra tekrar")
    static func kilitGeriSayim(dakika: Int, saniye: Int) -> String {
        return Metin.al("otp_wrong_cooldown_btn", dakika, saniye)
    }

    // Android'de koda gömülü Türkçe metinler (strings.xml'de karşılığı yok).
    /// UserInfoFragment.bindStep: meydan okuma adımının düğmesi.
    static let hedefimiOnayla = "Hedefimi onayla"
    /// StreakViews.reminderHourLabel(16) / (20).
    static let okuldanSonra = "Okuldan sonra"
    static let yemektenSonra = "Yemekten sonra"

    // Android'de karşılığı olmayan metinler (Apple ile giriş yalnızca iOS'ta var).
    static let appleIleGirisYap = "Apple ile Giriş Yap"
    static let appleIleKayitOl = "Apple ile Kayıt Ol"
    static let appleGirisBasarisiz = "Apple girişi başarısız"
    static let appleKayitBasarisiz = "Apple kaydı başarısız"
}
