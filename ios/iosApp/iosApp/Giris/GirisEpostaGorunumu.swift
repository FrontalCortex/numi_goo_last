import Shared
import SwiftUI

// Öğrenci girişi, e-posta adımı. Android: LoginActivity + activity_login.xml.
// Yerleşim (kenar boşluğu 24dp, zemin background_color): geri düğmesi 44dp; 12dp + 12dp altında
// başlık "E-posta ile devam et" (22sp, ortada); 24dp altında e-posta alanı; 16dp altında
// "Devam Et" (56dp, göçen); hemen altında "Google ile Giriş Yap" (56dp, göçen beyaz). Apple düğmesi
// iOS'a özgüdür, Google'ın altına eklenir (6dp).
// Kayıtlı olmayan e-postada (sunucu "Kullanıcı bulunamadı") kayıt sorularına yönlendirilir.
@MainActor
struct GirisEpostaGorunumu: View {
    @ObservedObject var model: GirisModeli

    private var devamMetni: String {
        if model.girisGonderiliyor {
            return Metin.al("ui_gonderiliyor")
        }
        if model.devamBekliyor(eposta: model.girisEposta) {
            let kalan: Int32 = model.devamKalanSaniye(eposta: model.girisEposta)
            // strings.xml: otp_resend_cooldown_try_again
            return OtpRules.shared.resendCooldownTryAgainLabel(sec: kalan)
        }
        return Metin.al("activity_login_btn_continue")
    }

    private var devamEtkin: Bool {
        return !model.girisGonderiliyor && !model.devamBekliyor(eposta: model.girisEposta)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                GirisGeriDugmesi {
                    model.geri()
                }
                .disabled(model.girisGonderiliyor)

                Text(Metin.al("activity_login_text"))
                    .font(Tema.yazi(22))
                    .foregroundColor(Color("dark_text"))
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 24)

                GirisMetinAlani(ipucu: Metin.al("activity_login_et_email_hint"), metin: $model.girisEposta, tur: .eposta)
                    .padding(.top, 24)

                GirisGocenDugme(metin: devamMetni, etkin: devamEtkin) {
                    GirisStil.klavyeyiKapat()
                    Task { await model.girisDevam() }
                }
                .padding(.top, 16)

                GirisGoogleDugmesi(metin: Metin.al("activity_login_btn_google_sign_in"), etkin: !model.girisGonderiliyor) {
                    GirisStil.klavyeyiKapat()
                    Task { await model.saglayiciIleGiris(apple: false) }
                }

                GirisAppleDugmesi(metin: GirisMetin.appleIleGirisYap, etkin: !model.girisGonderiliyor) {
                    GirisStil.klavyeyiKapat()
                    Task { await model.saglayiciIleGiris(apple: true) }
                }
                .padding(.top, 6)
            }
            .padding(GirisStil.kenarBoslugu)
        }
    }
}
