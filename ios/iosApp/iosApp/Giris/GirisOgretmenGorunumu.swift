import Shared
import SwiftUI

// Öğretmen girişi ve şifre sıfırlama. Android: TeacherLoginActivity + activity_teacher_login.xml,
// TeacherForgotPasswordEmailFragment, TeacherNewPasswordFragment.
// Öğretmenler e-posta + şifre ile girer; Google/Apple ile giriş öğretmenlere kapalıdır.

/// Giriş formu. Yerleşim (kenar boşluğu 24dp): geri düğmesi; 60dp altında 120dp daire
/// (dark_primary) içinde logo; 24dp altında başlık (24sp); 8dp altında alt başlık (16sp);
/// 48dp altında dark_surface kart (köşe 16dp, iç boşluk 24dp): e-posta, 16dp, şifre, 12dp,
/// "Şifremi unuttum" (14sp, altı çizili, dark_primary), 16dp, "Giriş Yap" (56dp).
@MainActor
struct GirisOgretmenGorunumu: View {
    @ObservedObject var model: GirisModeli

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                GirisGeriDugmesi {
                    model.geri()
                }
                .disabled(model.ogretmenGirisSuruyor)
                .frame(maxWidth: .infinity, alignment: .leading)

                Image("ic_education_logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 120, height: 120)
                    .background(Circle().fill(Color("dark_primary")))
                    .padding(.top, 60)
                Text("Eğitim Uygulaması")
                    .font(Tema.yazi(24))
                    .foregroundColor(Color("dark_text"))
                    .padding(.top, 24)
                Text("Öğretmen Girişi")
                    .font(Tema.yazi(16))
                    .foregroundColor(Color("dark_text_secondary"))
                    .padding(.top, 8)
                form
                    .padding(.top, 48)
            }
            .padding(GirisStil.kenarBoslugu)
        }
    }

    private var form: some View {
        VStack(alignment: .leading, spacing: 0) {
            GirisMetinAlani(ipucu: "E-posta", metin: $model.ogretmenGirisEposta, tur: .eposta)
            GirisMetinAlani(ipucu: "Şifre", metin: $model.ogretmenGirisSifre, tur: .sifre)
                .padding(.top, 16)
            Button {
                GirisStil.klavyeyiKapat()
                model.sifremiUnuttumAc()
            } label: {
                Text("Şifremi unuttum")
                    .font(Tema.yazi(14))
                    .underline()
                    .foregroundColor(Color("dark_primary"))
            }
            .buttonStyle(.plain)
            .disabled(model.ogretmenGirisSuruyor)
            .padding(.top, 12)
            GirisDoluDugme(metin: "Giriş Yap", etkin: !model.ogretmenGirisSuruyor) {
                GirisStil.klavyeyiKapat()
                Task { await model.ogretmenGiris() }
            }
            .padding(.top, 16)
        }
        .padding(24)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color("dark_surface")))
        .shadow(color: Color.black.opacity(0.4), radius: 8, x: 0, y: 4)
    }
}

/// Şifremi unuttum, e-posta adımı (fragment_teacher_forgot_password_email.xml).
/// İç boşluk 8dp: başlık 20sp; 8dp altında açıklama (14sp); 16dp altında e-posta alanı;
/// 24dp altında "Devam Et" (56dp).
@MainActor
struct GirisSifremiUnuttumGorunumu: View {
    @ObservedObject var model: GirisModeli

    var body: some View {
        let metinler = LoginFlowTexts.shared
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                GirisGeriDugmesi {
                    model.geri()
                }
                VStack(alignment: .leading, spacing: 0) {
                    Text("Şifremi unuttum")
                        .font(Tema.yazi(20))
                        .foregroundColor(Color("dark_text"))
                    Text("E-posta adresinizi girin. Size doğrulama kodu göndereceğiz.")
                        .font(Tema.yazi(14))
                        .foregroundColor(Color("dark_text_secondary"))
                        .padding(.top, 8)
                    GirisMetinAlani(ipucu: "E-posta", metin: $model.sifirlamaEposta, tur: .eposta)
                        .padding(.top, 16)
                    GirisDoluDugme(
                        metin: model.sifirlamaGonderiliyor ? metinler.BUTTON_SENDING : metinler.BUTTON_CONTINUE,
                        etkin: !model.sifirlamaGonderiliyor
                    ) {
                        GirisStil.klavyeyiKapat()
                        Task { await model.sifirlamaDevam() }
                    }
                    .padding(.top, 24)
                }
                .padding(8)
                .padding(.top, 12)
            }
            .padding(GirisStil.kenarBoslugu)
        }
    }
}

/// Yeni şifre adımı (fragment_teacher_new_password.xml).
/// İç boşluk 8dp: başlık 20sp; 8dp altında açıklama (14sp); 16dp altında yeni şifre;
/// 12dp altında şifre tekrar; 24dp altında "Şifreyi güncelle" (56dp).
@MainActor
struct GirisYeniSifreGorunumu: View {
    @ObservedObject var model: GirisModeli
    let eposta: String
    let kod: String

    var body: some View {
        let metinler = LoginFlowTexts.shared
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                GirisGeriDugmesi {
                    model.geri()
                }
                .disabled(model.yeniSifreGuncelleniyor)
                VStack(alignment: .leading, spacing: 0) {
                    Text("Yeni şifre")
                        .font(Tema.yazi(20))
                        .foregroundColor(Color("dark_text"))
                    Text("Yeni şifrenizi girin.")
                        .font(Tema.yazi(14))
                        .foregroundColor(Color("dark_text_secondary"))
                        .padding(.top, 8)
                    GirisMetinAlani(ipucu: "Yeni şifre", metin: $model.yeniSifre, tur: .sifre)
                        .padding(.top, 16)
                    GirisMetinAlani(ipucu: "Şifre tekrar", metin: $model.yeniSifreTekrar, tur: .sifre)
                        .padding(.top, 12)
                    GirisDoluDugme(
                        metin: model.yeniSifreGuncelleniyor ? metinler.BUTTON_UPDATING : metinler.BUTTON_UPDATE_PASSWORD,
                        etkin: !model.yeniSifreGuncelleniyor
                    ) {
                        GirisStil.klavyeyiKapat()
                        Task { await model.sifreyiGuncelle(eposta: eposta, kod: kod) }
                    }
                    .padding(.top, 24)
                }
                .padding(8)
                .padding(.top, 12)
            }
            .padding(GirisStil.kenarBoslugu)
        }
    }
}
