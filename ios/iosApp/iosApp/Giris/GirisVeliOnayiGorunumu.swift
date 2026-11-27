import Shared
import SwiftUI

// 13 yaş altı kayıtta velinin e-postası. Android: ParentConsentFragment + fragment_parent_consent.xml.
// Hesap bu ekranda açılmaz; veli kodu girilene kadar hiçbir yerde açılmaz (COPPA / GDPR / KVKK:
// doğrulanabilir veli onayı; App Store "Kids" ve 5.1.4 için de gerekli).
// Yerleşim (kayıt ekranının içinde, iç boşluk 8dp): başlık 20sp kalın; 12dp altında açıklama (14sp,
// ikincil); 12dp altında "Velin e-postada şunları görecek:" (14sp kalın); 4dp altında toplanan
// bilgilerin listesi (13sp, satır arası +2dp); 16dp altında veli e-postası alanı; 16dp altında
// "Veliye kod gönder" (56dp, dolu); 12dp altında not (12sp, ikincil).
// Gönderim sırasında düğme "Gönderiliyor..." yazar ve kapalıdır.
@MainActor
struct GirisVeliOnayiGorunumu: View {
    @ObservedObject var model: GirisModeli

    private var gonderMetni: String {
        return model.veliGonderiliyor ? Metin.al("ui_gonderiliyor") : Metin.al("fragment_parent_consent_btn_send")
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                GirisGeriDugmesi {
                    model.geri()
                }
                .disabled(model.veliGonderiliyor)
                icerik
                    .padding(8)
                    .padding(.top, 12)
            }
            .padding(GirisStil.kenarBoslugu)
        }
    }

    private var icerik: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("fragment_parent_consent_title"))
                .font(Tema.yazi(20))
                .foregroundColor(Color("dark_text"))
            Text(Metin.al("fragment_parent_consent_message"))
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
            Text(Metin.al("fragment_parent_consent_collected_title"))
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text"))
                .padding(.top, 12)
            Text(Metin.al("fragment_parent_consent_collected_list"))
                .font(Tema.yazi(13))
                .foregroundColor(Color("dark_text_secondary"))
                .lineSpacing(2)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 4)
            GirisMetinAlani(ipucu: Metin.al("fragment_parent_consent_et_hint"), metin: $model.veliEposta, tur: .eposta, azamiUzunluk: 254)
                .padding(.top, 16)
            GirisDoluDugme(metin: gonderMetni, etkin: !model.veliGonderiliyor) {
                GirisStil.klavyeyiKapat()
                Task { await model.veliyeKodGonder() }
            }
            .padding(.top, 16)
            Text(Metin.al("fragment_parent_consent_note"))
                .font(Tema.yazi(12))
                .foregroundColor(Color("dark_text_secondary"))
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
        }
    }
}
