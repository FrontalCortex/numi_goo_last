import Shared
import SwiftUI

// Kayıt ekranı. Android: RegisterActivity + activity_register.xml.
// Yerleşim (kenar boşluğu 24dp, zemin background_color): geri düğmesi 44dp; üstten 54dp'de başlık
// "Hesap Oluştur" (24sp); 8dp altında alt başlık (16sp, dark_text_secondary); 32dp altında
//   - öğrenci: e-posta alanı, 16dp altında "Devam Et" (56dp, göçen), 6dp altında "Google ile Kayıt
//     Ol" (56dp, göçen beyaz) ve iOS'a özgü "Apple ile Kayıt Ol" (Google'ın altında, 6dp)
//   - öğretmen: dark_surface kart (köşe 16dp, iç boşluk 24dp): kullanıcı adı, e-posta, şifre,
//     şifre tekrar (aralarında 16dp) ve "Devam Et". Öğretmen kaydında Google/Apple düğmesi yoktur.
// En altta (alt boşluk 16dp) kullanım şartları ve gizlilik metni (12sp, ortalı).
// Android'de kod ve veli onayı adımları bu ekranın içinde açılır (başlıklar gizlenir); burada
// ayrı ekrandır (GirisKodGorunumu, GirisVeliOnayiGorunumu) ve geri düğmesi bir adım geri götürür.
@MainActor
struct KayitGorunumu: View {
    @ObservedObject var model: GirisModeli

    private var devamMetni: String {
        if model.kayitGonderiliyor {
            return Metin.al("ui_gonderiliyor")
        }
        if model.devamBekliyor(eposta: model.kayitEposta) {
            let kalan: Int32 = model.devamKalanSaniye(eposta: model.kayitEposta)
            // strings.xml: otp_resend_cooldown_try_again
            return OtpRules.shared.resendCooldownTryAgainLabel(sec: kalan)
        }
        return Metin.al("activity_register_btn_continue")
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
            Text(Metin.al("activity_register_tv_title"))
                .font(Tema.yazi(24))
                .foregroundColor(Color("dark_text"))
                .padding(.top, 54)
            Text(Metin.al("activity_register_tv_subtitle"))
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
            GirisMetinAlani(ipucu: Metin.al("activity_register_et_email_hint"), metin: $model.kayitEposta, tur: .eposta)
            GirisGocenDugme(metin: devamMetni, etkin: devamEtkin) {
                GirisStil.klavyeyiKapat()
                Task { await model.kayitDevam() }
            }
            .padding(.top, 16)
            GirisGoogleDugmesi(metin: Metin.al("activity_register_btn_google_sign_in"), etkin: !model.kayitGonderiliyor) {
                GirisStil.klavyeyiKapat()
                Task { await model.saglayiciIleKayit(apple: false) }
            }
            .padding(.top, 6)
            GirisAppleDugmesi(metin: GirisMetin.appleIleKayitOl, etkin: !model.kayitGonderiliyor) {
                GirisStil.klavyeyiKapat()
                Task { await model.saglayiciIleKayit(apple: true) }
            }
            .padding(.top, 6)
        }
    }

    private var ogretmenFormu: some View {
        VStack(spacing: 16) {
            GirisMetinAlani(ipucu: Metin.al("activity_register_et_name_hint"), metin: $model.ogretmenAd, tur: .ad)
            GirisMetinAlani(ipucu: Metin.al("activity_register_et_teacher_email_hint"), metin: $model.ogretmenEposta, tur: .eposta)
            GirisMetinAlani(ipucu: Metin.al("activity_register_et_password_hint"), metin: $model.ogretmenSifre, tur: .sifre)
            GirisMetinAlani(ipucu: Metin.al("activity_register_et_approval_code_hint"), metin: $model.ogretmenSifreTekrar, tur: .sifre)
            GirisGocenDugme(metin: Metin.al("activity_register_btn_register"), etkin: !model.kayitGonderiliyor) {
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
/// altı çizili değil; dokununca tarayıcıda açılır. Sayfa uygulamanın dilinde (privacy-policy-tr …);
/// metin strings.xml'den, adresler ortak koddan (LoginFlowTexts.termsUrl / privacyUrl).
@MainActor
struct KayitSartlarMetni: View {
    private var metin: AttributedString {
        let metinler = LoginFlowTexts.shared
        var sonuc = AttributedString(Metin.al("ui_hesap_olusturarak_kullanim_sartlarini_ve_giz"))
        // Tema bütün TextView'ları kalın çizer (Widget.NumiGoo.TextView); bağlantılar yalnızca
        // renkle ayrılır.
        sonuc.font = Tema.yazi(12)
        sonuc.foregroundColor = Color("dark_text_secondary")
        let baglantilar: [(String, String)] = [
            (Metin.al("ui_kullanim_sartlarini"), metinler.termsUrl(langCode: Metin.dil)),
            (Metin.al("ui_gizlilik_politikasini"), metinler.privacyUrl(langCode: Metin.dil)),
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
