import Foundation
import Shared

// Kayıt (RegisterActivity), veli onayı (ParentConsentFragment) ve doğrulama kodu
// (OtpVerificationFragment) adımları.
//
// 13 yaş altı (AuthRules.requiresParentalConsent, UTC yılı − doğum yılı ≤ 13) hesap veli kodu
// girilene kadar açılmaz:
//   E-posta yolu: kayıt → veli e-postası → (veli adresi farklı ve çocuğun adresi doğrulanmamışsa:
//     çocuğun kendi kodu → doğrulanınca veliye kod) → veli kodu → hesap (verifyRegistrationCode).
//   Google / Apple yolu: hesap seçilir, belge yazılmaz (ERROR_PARENTAL_CONSENT_REQUIRED) → veli
//     e-postası → veli kodu → hesap sunucuda açılır (confirmParentalConsent). Vazgeçilirse yarım
//     Auth kaydı silinir.

extension GirisModeli {
    /// AuthRules.requiresParentalConsent; yıl UTC (sunucu ve Firestore kuralları da UTC kullanır).
    func veliOnayiGerekir(_ dogumYili: Int?) -> Bool {
        var takvim = Calendar(identifier: .gregorian)
        takvim.timeZone = TimeZone(identifier: "UTC") ?? TimeZone.current
        let utcYili = Int32(takvim.component(.year, from: Date()))
        let kutulu: KotlinInt? = dogumYili.map { KotlinInt(int: Int32($0)) }
        return AuthRules.shared.requiresParentalConsent(birthYear: kutulu, utcYear: utcYili)
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
        if !zatenKayitli {
            // AuthManager.createPendingRegistrationForOTP: öğrencide ad ve şifre boş.
            bekleyenKayit = GirisBekleyenKayit(ad: "", sifre: "", rol: kurallar.ROLE_STUDENT)
            if let yil = kayitDogumYili, veliOnayiGerekir(yil) {
                // 13 yaş altı: kod çocuğa değil veliye gidecek; önce velinin e-postası sorulur.
                veliOnayiniAc(cocukEposta: normal, saglayici: false, dogumYili: yil)
                return
            }
        }
        let amac: String = kurallar.resendPurpose(isRegistration: !zatenKayitli)
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

    /// Kayıt ekranındaki Google / Apple düğmesi (otomatik kayıt; RegisterActivity.onActivityResult).
    func saglayiciIleKayit(apple: Bool) async {
        if mesgul { return }
        mesgul = true
        let varsayilan: String = apple ? GirisMetin.appleKayitBasarisiz : Metin.al("ui_google_kaydi_basarisiz")
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
            if hata.mesaj == AuthRules.shared.ERROR_PARENTAL_CONSENT_REQUIRED {
                // 13 yaş altı: oturum açık, hesap henüz yok; velinin onayıyla sunucu açacak.
                let cocuk: String = (hata.hesapEposta ?? "").trimmingCharacters(in: .whitespaces)
                if let yil = kayitDogumYili, !cocuk.isEmpty {
                    let normal = AuthRules.shared.normalizeEmail(email: cocuk)
                    veliOnayiniAc(cocukEposta: normal, saglayici: true, dogumYili: yil)
                } else {
                    await servis.yarimKaydiSil()
                    bildir(varsayilan)
                }
                return
            }
            // Kullanıcı vazgeçtiyse sessiz kalınır.
            if hata.mesaj != AuthRules.shared.MESSAGE_GOOGLE_CANCELLED {
                bildir(hata.mesaj ?? varsayilan)
            }
        } catch {
            mesgul = false
            bildir(varsayilan)
        }
    }

    // MARK: Veli onayı (ParentConsentFragment)

    func veliOnayiniAc(cocukEposta: String, saglayici: Bool, dogumYili: Int) {
        GirisStil.klavyeyiKapat()
        veliBaglami = GirisVeliBaglami(
            cocukEposta: cocukEposta,
            saglayici: saglayici,
            dogumYili: dogumYili,
            kaynak: kayitKaynak
        )
        veliEposta = ""
        veliGonderiliyor = false
        ac(.veliOnayi)
    }

    /// "Veliye kod gönder" (ParentConsentFragment.sendCode). Veli adresi hesap adresiyle aynı
    /// olabilir: hesabı veli kendi adresiyle açıyor olabilir.
    func veliyeKodGonder() async {
        if mesgul || veliGonderiliyor { return }
        guard let baglam = veliBaglami else { return }
        let kurallar = AuthRules.shared
        let veli: String = kurallar.normalizeEmail(email: veliEposta)
        if !kurallar.isValidEmail(email: veli) {
            bildir(Metin.al("ui_gecerli_bir_e_posta_adresi_giriniz"), uzun: true)
            return
        }
        veliGonderiliyor = true
        defer { veliGonderiliyor = false }
        do {
            if baglam.saglayici {
                try await servis.saglayiciVeliOnayiIste(veliEposta: veli, dogumYili: baglam.dogumYili, kaynak: baglam.kaynak)
            } else {
                try await servis.veliKayitKoduGonder(
                    cocukEposta: baglam.cocukEposta,
                    veliEposta: veli,
                    dogumYili: baglam.dogumYili,
                    kaynak: baglam.kaynak
                )
            }
            kodEkraniniAc(eposta: baglam.cocukEposta, tur: .veli(veliEposta: veli, saglayici: baglam.saglayici))
        } catch let hata as GirisHatasi {
            if hata.mesaj == kurallar.ERROR_CHILD_EMAIL_NOT_VERIFIED {
                // Veli adresi farklı: önce çocuğun KENDİ adresi doğrulanmalı (yanlış yazılan adresle
                // açılan hesaba bir daha girilemezdi). Veli kodu o adımdan sonra gider.
                await cocugaKodGonder(baglam: baglam, veli: veli)
                return
            }
            bildir(hata.mesaj ?? Metin.al("ui_kod_gonderilemedi"), uzun: true)
        } catch {
            bildir(Metin.al("ui_kod_gonderilemedi"), uzun: true)
        }
    }

    /// ParentConsentFragment.sendChildEmailCode: çocuğun adresine olağan kayıt kodu.
    private func cocugaKodGonder(baglam: GirisVeliBaglami, veli: String) async {
        let kurallar = AuthRules.shared
        do {
            try await servis.dogrulamaKoduGonder(eposta: baglam.cocukEposta, amac: kurallar.resendPurpose(isRegistration: true))
            kodEkraniniAc(eposta: baglam.cocukEposta, tur: .cocukEpostasi(veliEposta: veli))
        } catch let hata as GirisHatasi {
            bildir(hata.mesaj ?? Metin.al("ui_kod_gonderilemedi"), uzun: true)
        } catch {
            bildir(Metin.al("ui_kod_gonderilemedi"), uzun: true)
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

    func kilidiBaslat(eposta: String) {
        let simdi = GirisYerelDepo.simdiMs()
        depo.yanlisDenemeBaslat(eposta: eposta, simdiMs: simdi)
        depo.yenidenGondermeTemizle()
        kilitEkrandaGoruldu = true
        simdiMs = simdi
    }

    /// OtpVerificationFragment.resend: veli kodu yeniden VELİYE gider (normal kayıt koduna düşseydi
    /// sunucu 13 yaş altı kaydı reddederdi).
    func koduYenidenGonder(eposta: String, tur: GirisKodTuru) async {
        if !yenidenGonderEtkin(eposta: eposta) { return }
        let kurallar = AuthRules.shared
        kodYenidenGonderiliyor = true
        defer { kodYenidenGonderiliyor = false }
        do {
            switch tur {
            case .sifreSifirlama:
                try await servis.dogrulamaKoduGonder(
                    eposta: kurallar.normalizeEmail(email: eposta),
                    amac: kurallar.OTP_PURPOSE_TEACHER_RESET
                )
            case .veli(let veli, let saglayici):
                guard let baglam = veliBaglami else { return }
                if saglayici {
                    try await servis.saglayiciVeliOnayiIste(veliEposta: veli, dogumYili: baglam.dogumYili, kaynak: baglam.kaynak)
                } else {
                    try await servis.veliKayitKoduGonder(
                        cocukEposta: eposta,
                        veliEposta: veli,
                        dogumYili: baglam.dogumYili,
                        kaynak: baglam.kaynak
                    )
                }
            case .kayit, .cocukEpostasi:
                try await servis.dogrulamaKoduGonder(
                    eposta: kurallar.normalizeEmail(email: eposta),
                    amac: kurallar.resendPurpose(isRegistration: true)
                )
            case .giris:
                try await servis.dogrulamaKoduGonder(
                    eposta: kurallar.normalizeEmail(email: eposta),
                    amac: kurallar.resendPurpose(isRegistration: false)
                )
            }
            bildir(Metin.al("ui_kod_tekrar_gonderildi"))
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
                dogrulamaBitti()
                yeniSifre = ""
                yeniSifreTekrar = ""
                ac(.yeniSifre(eposta: eposta, kod: girilen))
            case .veli(_, let saglayici) where saglayici:
                // 13 yaş altı Google / Apple kaydı: hesabı sunucu açar.
                _ = try await servis.saglayiciVeliOnayiniDogrula(kod: girilen)
                veliBaglami = nil
                await girisTamam()
                dogrulamaBitti()
            case .cocukEpostasi(let veli):
                try await cocukKodunuDogrulaVeVeliyeGonder(eposta: eposta, kod: girilen, veli: veli)
            case .kayit, .veli:
                // Veli kodu (e-posta yolu) da buradan doğrulanır: sunucu kodun veli kodu olduğunu
                // kendi kaydından bilir, doğum yılını da oradan alır.
                _ = try await servis.kayitKodunuDogrula(
                    eposta: eposta,
                    kod: girilen,
                    bekleyen: bekleyenKayit,
                    dogumYili: kayitDogumYili,
                    kaynak: kayitKaynak
                )
                bekleyenKayit = nil
                veliBaglami = nil
                await girisTamam()
                dogrulamaBitti()
            case .giris:
                _ = try await servis.girisKodunuDogrula(eposta: eposta, kod: girilen)
                await girisTamam()
                dogrulamaBitti()
            }
        } catch let hata as GirisHatasi {
            dogrulamaBasarisiz(eposta: eposta, mesaj: hata.mesaj)
        } catch {
            dogrulamaBasarisiz(eposta: eposta, mesaj: nil)
        }
    }

    private func dogrulamaBitti() {
        kodDogrulaniyor = false
        mesgul = false
    }

    /// Çocuğun adresi doğrulandı; şimdi veli kodu. Kullanılan kodun ekranı yığından çıkar: geri
    /// dönüş bu ekrana değil veli e-postası ekranına olmalı.
    private func cocukKodunuDogrulaVeVeliyeGonder(eposta: String, kod girilen: String, veli: String) async throws {
        guard let baglam = veliBaglami else {
            dogrulamaBitti()
            return
        }
        try await servis.cocukEpostasiniDogrula(eposta: eposta, kod: girilen, dogumYili: baglam.dogumYili)
        var hataMetni: String? = nil
        do {
            try await servis.veliKayitKoduGonder(
                cocukEposta: eposta,
                veliEposta: veli,
                dogumYili: baglam.dogumYili,
                kaynak: baglam.kaynak
            )
        } catch let hata as GirisHatasi {
            hataMetni = hata.mesaj ?? Metin.al("ui_kod_gonderilemedi")
        } catch {
            hataMetni = Metin.al("ui_kod_gonderilemedi")
        }
        dogrulamaBitti()
        if yigin.count > 1 {
            yigin.removeLast()
        }
        if let metin = hataMetni {
            bildir(metin, uzun: true)
            return
        }
        kodEkraniniAc(eposta: eposta, tur: .veli(veliEposta: veli, saglayici: false))
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
}
