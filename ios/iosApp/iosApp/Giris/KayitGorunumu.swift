import Shared
import SwiftUI

// Kayıt ekranı. Android: RegisterActivity + activity_register.xml.
// Yerleşim (kenar boşluğu 24dp): geri düğmesi 44dp; üstten 54dp'de başlık "Hesap Oluştur" (24sp);
// 8dp altında alt başlık (16sp, dark_text_secondary); 32dp altında
//   - öğrenci: e-posta alanı, 16dp altında "Devam Et" (56dp), 6dp altında "Google ile Kayıt Ol"
//   - öğretmen: dark_surface kart (köşe 16dp, iç boşluk 24dp): kullanıcı adı, e-posta, şifre,
//     şifre tekrar (aralarında 16dp) ve "Devam Et". Öğretmen kaydında Google/Apple düğmesi yoktur.
// En altta (alt boşluk 16dp) kullanım şartları ve gizlilik metni (12sp, ortalı).
@MainActor
struct KayitGorunumu: View {
    @ObservedObject var model: GirisModeli

    private var devamMetni: String {
        if model.kayitGonderiliyor {
            return LoginFlowTexts.shared.BUTTON_SENDING
        }
        if model.devamBekliyor(eposta: model.kayitEposta) {
            let kalan: Int32 = model.devamKalanSaniye(eposta: model.kayitEposta)
            return OtpRules.shared.resendCooldownTryAgainLabel(sec: kalan)
        }
        return LoginFlowTexts.shared.BUTTON_CONTINUE
    }

    private var devamEtkin: Bool {
        return !model.kayitGonderiliyor && !model.devamBekliyor(eposta: model.kayitEposta)
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                ZStack(alignment: .topLeading) {
                    GirisGeriDugmesi {
                        model.geri()
                    }
                    .disabled(model.kayitGonderiliyor)
                    icerik
                }
                .padding(.horizontal, GirisStil.kenarBoslugu)
                .padding(.top, GirisStil.kenarBoslugu)
            }
            KayitSartlarMetni()
                .padding(.horizontal, GirisStil.kenarBoslugu)
                .padding(.bottom, GirisStil.kenarBoslugu + 16)
        }
    }

    private var icerik: some View {
        VStack(spacing: 0) {
            Text("Hesap Oluştur")
                .font(Tema.yazi(24))
                .foregroundColor(Color("dark_text"))
                .padding(.top, 54)
            Text("Eğitim yolculuğuna başla")
                .font(Tema.yazi(16))
                .foregroundColor(Color("dark_text_secondary"))
                .padding(.top, 8)
            if model.kayitOgretmen {
                ogretmenFormu
                    .padding(.top, 32)
            } else {
                ogrenciAdimi
                    .padding(.top, 32)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var ogrenciAdimi: some View {
        VStack(spacing: 0) {
            GirisMetinAlani(ipucu: "E-posta", metin: $model.kayitEposta, tur: .eposta)
            GirisDoluDugme(metin: devamMetni, etkin: devamEtkin) {
                GirisStil.klavyeyiKapat()
                Task { await model.kayitDevam() }
            }
            .padding(.top, 16)
            GirisGoogleDugmesi(metin: "Google ile Kayıt Ol") {
                GirisStil.klavyeyiKapat()
                Task { await model.saglayiciIleKayit(apple: false) }
            }
            .disabled(model.kayitGonderiliyor)
            .padding(.top, 6)
            GirisAppleDugmesi(metin: GirisMetin.appleIleKayitOl) {
                GirisStil.klavyeyiKapat()
                Task { await model.saglayiciIleKayit(apple: true) }
            }
            .disabled(model.kayitGonderiliyor)
        }
    }

    private var ogretmenFormu: some View {
        VStack(spacing: 16) {
            GirisMetinAlani(ipucu: "Kullanıcı adı", metin: $model.ogretmenAd, tur: .ad)
            GirisMetinAlani(ipucu: "E-posta", metin: $model.ogretmenEposta, tur: .eposta)
            GirisMetinAlani(ipucu: "Şifre", metin: $model.ogretmenSifre, tur: .sifre)
            GirisMetinAlani(ipucu: "Şifreyi tekrar girin", metin: $model.ogretmenSifreTekrar, tur: .sifre)
            GirisDoluDugme(metin: LoginFlowTexts.shared.BUTTON_CONTINUE, etkin: !model.kayitGonderiliyor) {
                GirisStil.klavyeyiKapat()
                Task { await model.ogretmenKayitDevam() }
            }
        }
        .padding(24)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color("dark_surface")))
        .shadow(color: Color.black.opacity(0.4), radius: 8, x: 0, y: 4)
    }
}

/// "Hesap oluşturarak Kullanım Şartlarını ve Gizlilik Politikasını kabul etmiş olursunuz."
/// Android: RegisterActivity.setupTermsAndPrivacyText — iki bağlantı kalın ve dark_text renginde,
/// altı çizili değil; dokununca tarayıcıda açılır. Metin ve adresler ortak koddadır.
@MainActor
struct KayitSartlarMetni: View {
    private var metin: AttributedString {
        let metinler = LoginFlowTexts.shared
        var sonuc = AttributedString(metinler.TERMS_FULL_TEXT)
        // Tema bütün TextView'ları kalın çizer (Widget.NumiGoo.TextView); bağlantılar yalnızca
        // renkle ayrılır.
        sonuc.font = Tema.yazi(12)
        sonuc.foregroundColor = Color("dark_text_secondary")
        let baglantilar: [(String, String)] = [
            (metinler.TERMS_LINK_TEXT, metinler.TERMS_URL),
            (metinler.PRIVACY_LINK_TEXT, metinler.PRIVACY_URL),
        ]
        for (parca, adres) in baglantilar {
            if let aralik = sonuc.range(of: parca) {
                sonuc[aralik].font = Tema.yazi(12)
                sonuc[aralik].foregroundColor = Color("dark_text")
                sonuc[aralik].link = URL(string: adres)
            }
        }
        return sonuc
    }

    var body: some View {
        Text(metin)
            .tint(Color("dark_text"))
            .multilineTextAlignment(.center)
            .lineSpacing(3)
            .frame(maxWidth: .infinity)
    }
}
