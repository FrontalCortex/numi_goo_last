import Shared
import SwiftUI

// E-posta doğrulama kodu ekranı. Android: OtpVerificationFragment + fragment_otp_verification.xml.
// Yerleşim (iç boşluk 8dp, sola dayalı): başlık 20sp; 6dp altında e-posta (14sp, ikincil renk);
// 12dp altında açıklama (14sp); 16dp altında 6 haneli kod alanı; 16dp altında "Doğrula" (56dp, dolu);
// 12dp altında "Kodu tekrar gönder" (48dp, çerçeveli; geri sayımda metin değişir).
// Kurallar ortak kodda: kod uzunluğu, 45 sn yeniden gönderme beklemesi, 5 yanlış denemede 15 dk kilit.
//
// Türüne göre (OtpVerificationFragment.onViewCreated):
//   şifre sıfırlama → "Şifre sıfırlama"; veli kodu → "Veli onayı", "Velinin e-postası: …", veli
//   açıklaması; çocuğun kendi adresi → "E-postanı doğrula", "Önce kendi e-postana gelen…";
//   kayıt → "Hesap oluştur"; giriş → "E-postanı doğrula".
// Android'de bu parça giriş/kayıt ekranının içine gömülüdür; burada ayrı ekrandır.
@MainActor
struct GirisKodGorunumu: View {
    @ObservedObject var model: GirisModeli
    let eposta: String
    let tur: GirisKodTuru

    private var baslik: String {
        switch tur {
        case .sifreSifirlama:
            return Metin.al("ui_sifre_sifirlama")
        case .veli:
            return Metin.al("ui_veli_onayi")
        case .cocukEpostasi, .giris:
            return Metin.al("ui_e_postani_dogrula")
        case .kayit:
            return Metin.al("ui_hesap_olustur")
        }
    }

    private var epostaSatiri: String {
        if case .veli(let veli, _) = tur {
            return Metin.al("ui_veli_e_postasi_x", veli)
        }
        return eposta
    }

    private var aciklama: String {
        switch tur {
        case .veli:
            return Metin.al("ui_veli_e_postasina_gelen_kodu_gir")
        case .cocukEpostasi:
            return Metin.al("ui_once_kendi_e_postana_gelen_kodu_gir")
        default:
            return Metin.al("fragment_otp_verification_text")
        }
    }

    private var dogrulaMetni: String {
        return model.kodDogrulaniyor ? Metin.al("ui_dogrulaniyor") : Metin.al("fragment_otp_verification_btn_verify")
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
            Text(epostaSatiri)
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .padding(.top, 6)
            Text(aciklama)
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
            GirisMetinAlani(
                ipucu: Metin.al("fragment_otp_verification_et_code_hint"),
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
