import Shared
import SwiftUI

// Yaş ve "bizi nereden duydunuz" adımları. Android: UserInfoFragment + fragment_user_info.xml.
// Kayıttan ÖNCE gösterilir; yaştan hesaplanan doğum yılı ve seçilen kaynak kayıt çağrısıyla
// sunucuya gider (`users/{uid}.birthYear`, `acquisitionSource`). Doğum yılı reklamların yaş
// korumasını belirler (AgeConsentManager.treatmentFor: rıza yaşının altı → çocuk muamelesi).
//
// Yerleşim: geri düğmesi 44dp (sol üst);
//   - yaş adımı: üstten 48dp'de başlık "Kaç yaşındasın?" (26sp), 20dp altında 140x56dp yaş alanı
//     (bg_edit_profile_input: #203137 zemin, köşe 12dp, 1dp #30363D çizgi; 22sp, en çok 3 hane)
//   - kaynak adımı: üstten 60dp'de başlık (22sp), 24dp altında ikişerli 4 satır kart (64dp,
//     aralık 12dp, yan boşluk 24dp; zemin background_color, 2dp çizgi; seçilince dark_primary)
//   - altta "Devam Et" (56dp, yan 10dp, alt 24dp; devre dışıyken dark_text_secondary zemin)
@MainActor
struct GirisYasGorunumu: View {
    @ObservedObject var model: GirisModeli

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .topLeading) {
                GirisGeriDugmesi {
                    model.geri()
                }
                if model.bilgiAdimi == .yas {
                    yasAdimi
                } else {
                    kaynakAdimi
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)

            GirisDoluDugme(
                metin: LoginFlowTexts.shared.BUTTON_CONTINUE,
                etkin: model.bilgiDevamEtkin,
                buyukHarf: false,
                yaziBoyu: 18,
                devreDisiZemin: Color("dark_text_secondary")
            ) {
                GirisStil.klavyeyiKapat()
                model.bilgiDevam()
            }
            .padding(.horizontal, 10)
            .padding(.top, 16)
            .padding(.bottom, 24)
        }
    }

    private var yasAdimi: some View {
        VStack(spacing: 0) {
            Text("Kaç yaşındasın?")
                .font(Tema.yazi(26))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
            GirisYasAlani(metin: $model.yasMetni)
                .padding(.top, 20)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 48)
    }

    private var kaynakAdimi: some View {
        let adlar: [String] = UserInfoRules.shared.SOURCE_NAMES
        let satirSayisi: Int = (adlar.count + 1) / 2
        return VStack(spacing: 0) {
            Text("Bizi nereden duydunuz?")
                .font(Tema.yazi(22))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    ForEach(0..<satirSayisi, id: \.self) { satir in
                        HStack(spacing: 12) {
                            kart(adlar, satir * 2)
                            kart(adlar, satir * 2 + 1)
                        }
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .padding(.top, 24)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }

    @ViewBuilder
    private func kart(_ adlar: [String], _ sira: Int) -> some View {
        if sira < adlar.count {
            let ad: String = adlar[sira]
            GirisKaynakKarti(ad: ad, secili: model.seciliKaynak == ad) {
                model.kaynakSec(ad)
            }
        } else {
            Color.clear.frame(height: 64)
        }
    }
}

/// Yaş alanı (etAge).
@MainActor
struct GirisYasAlani: View {
    @Binding var metin: String

    var body: some View {
        let azami: Int = Int(UserInfoRules.shared.AGE_MAX_LENGTH)
        ZStack {
            if metin.isEmpty {
                Text("Yaş")
                    .font(Tema.yazi(22))
                    .foregroundColor(Color("dark_text_secondary"))
                    .allowsHitTesting(false)
            }
            TextField("", text: $metin)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(Tema.yazi(22))
                .foregroundColor(Color("dark_text"))
                .tint(Color("dark_primary"))
        }
        .padding(.horizontal, 16)
        .frame(width: 140, height: 56)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color("bright_background_color")))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(GirisStil.yasAlaniCizgisi, lineWidth: 1))
        .onChange(of: metin) { yeni in
            // inputType="number": yalnızca rakam; maxLength="3".
            let rakamlar = yeni.filter { $0 >= "0" && $0 <= "9" }
            let kisaltilmis = String(rakamlar.prefix(azami))
            if kisaltilmis != yeni {
                metin = kisaltilmis
            }
        }
    }
}

/// Kaynak kartı (MaterialCardView: 64dp, zemin background_color, 2dp çizgi, köşe 4dp).
@MainActor
struct GirisKaynakKarti: View {
    let ad: String
    let secili: Bool
    let eylem: () -> Void

    var body: some View {
        let cizgi: Color = secili ? Color("dark_primary") : GirisStil.kartCizgisi
        Button(action: eylem) {
            Text(ad)
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity)
                .frame(height: 64)
                .background(RoundedRectangle(cornerRadius: 4).fill(Color("background_color")))
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(cizgi, lineWidth: 2))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
