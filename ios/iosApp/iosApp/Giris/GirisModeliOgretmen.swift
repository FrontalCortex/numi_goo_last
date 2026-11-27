import Foundation
import Shared

// Öğretmen girişi (TeacherLoginActivity) ve şifre sıfırlama (TeacherForgotPasswordEmailFragment,
// TeacherNewPasswordFragment). Öğretmenler e-posta + şifre ile girer; Google/Apple öğretmene kapalı.

extension GirisModeli {
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
