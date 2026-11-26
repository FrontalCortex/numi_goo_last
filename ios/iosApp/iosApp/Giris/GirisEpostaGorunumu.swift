import Shared
import SwiftUI

// Öğrenci girişi, e-posta adımı. Android: LoginActivity + activity_login.xml.
// Yerleşim (kenar boşluğu 24dp): geri düğmesi 44dp; 24dp altında başlık "E-posta ile devam et"
// (22sp, ortada); 24dp altında e-posta alanı; 16dp altında "Devam Et" (56dp); hemen altında
// "Google ile Giriş Yap" (56dp). Apple düğmesi iOS'a özgüdür, Google'ın altına eklenir.
@MainActor
struct GirisEpostaGorunumu: View {
    @ObservedObject var model: GirisModeli

    private var devamMetni: String {
        if model.girisGonderiliyor {
            return LoginFlowTexts.shared.BUTTON_SENDING
        }
        let kalan: Int32 = model.devamKalanSaniye(eposta: model.girisEposta)
        if model.devamBekliyor(eposta: model.girisEposta) {
            // strings.xml: otp_resend_cooldown_try_again
            return OtpRules.shared.resendCooldownTryAgainLabel(sec: kalan)
        }
        return LoginFlowTexts.shared.BUTTON_CONTINUE
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

                Text("E-posta ile devam et")
                    .font(Tema.yazi(22))
                    .foregroundColor(Color("dark_text"))
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 24)

                GirisMetinAlani(ipucu: "E-posta", metin: $model.girisEposta, tur: .eposta)
                    .padding(.top, 24)

                GirisDoluDugme(metin: devamMetni, etkin: devamEtkin) {
                    GirisStil.klavyeyiKapat()
                    Task { await model.girisDevam() }
                }
                .padding(.top, 16)

                GirisGoogleDugmesi(metin: "Google ile Giriş Yap") {
                    GirisStil.klavyeyiKapat()
                    Task { await model.saglayiciIleGiris(apple: false) }
                }
                .disabled(model.girisGonderiliyor)

                GirisAppleDugmesi(metin: GirisMetin.appleIleGirisYap) {
                    GirisStil.klavyeyiKapat()
                    Task { await model.saglayiciIleGiris(apple: true) }
                }
                .disabled(model.girisGonderiliyor)
            }
            .padding(GirisStil.kenarBoslugu)
        }
    }
}
