import Shared
import SwiftUI

// E-posta doğrulama kodu ekranı. Android: OtpVerificationFragment + fragment_otp_verification.xml.
// Yerleşim (iç boşluk 8dp, sola dayalı): başlık 20sp; 6dp altında e-posta (14sp, ikincil renk);
// 12dp altında açıklama (14sp); 16dp altında 6 haneli kod alanı; 16dp altında "Doğrula" (56dp, dolu);
// 12dp altında "Kodu tekrar gönder" (48dp, çerçeveli; geri sayımda metin değişir).
// Kurallar ortak kodda: kod uzunluğu, 45 sn yeniden gönderme beklemesi, 5 yanlış denemede 15 dk kilit.
//
// Android'de bu parça giriş/kayıt ekranının içine gömülüdür (girişte altında Google düğmesi
// görünmeye devam eder); burada ayrı ekrandır ve sağlayıcı düğmeleri gösterilmez.
@MainActor
struct GirisKodGorunumu: View {
    @ObservedObject var model: GirisModeli
    let eposta: String
    let tur: GirisKodTuru

    private var baslik: String {
        return LoginFlowTexts.shared.otpTitle(
            forPasswordReset: tur == .sifreSifirlama,
            isRegistration: tur == .kayit
        )
    }

    private var dogrulaMetni: String {
        let metinler = LoginFlowTexts.shared
        return model.kodDogrulaniyor ? metinler.BUTTON_VERIFYING : metinler.BUTTON_VERIFY
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                GirisGeriDugmesi {
                    model.geri()
                }
                icerik
                    .padding(8)
                    .padding(.top, 12)
            }
            .padding(GirisStil.kenarBoslugu)
        }
    }

    private var icerik: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(baslik)
                .font(Tema.yazi(20))
                .foregroundColor(Color("dark_text"))
            Text(eposta)
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .padding(.top, 6)
            Text("E-postanıza gelen 6 haneli kodu girin.")
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .padding(.top, 12)
            GirisMetinAlani(
                ipucu: "6 haneli kod",
                metin: $model.kod,
                tur: .sayi,
                azamiUzunluk: Int(OtpRules.shared.CODE_LENGTH)
            )
            .padding(.top, 16)
            GirisDoluDugme(metin: dogrulaMetni, etkin: model.dogrulaEtkin(eposta: eposta)) {
                GirisStil.klavyeyiKapat()
                Task { await model.koduDogrula(eposta: eposta, tur: tur) }
            }
            .padding(.top, 16)
            GirisCerceveliDugme(
                metin: model.yenidenGonderMetni(eposta: eposta),
                etkin: model.yenidenGonderEtkin(eposta: eposta),
                yukseklik: 48
            ) {
                Task { await model.koduYenidenGonder(eposta: eposta, tur: tur) }
            }
            .padding(.top, 12)
        }
    }
}
