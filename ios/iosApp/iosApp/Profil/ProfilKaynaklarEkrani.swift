import SwiftUI
import UIKit

// Ayarlar → Destek → "Kaynaklar ve lisanslar". Android: CreditsFragment + res/layout/fragment_credits.xml.
//
// Atıf metinleri lisansların istediği biçimde ve İngilizce (çevrilmez); başlıklar uygulama dilinde.
// Personas avatarları (CC BY 4.0) ve Noto Emoji Lottie'leri (CC BY 4.0) için kaynak gösterme ŞART.
//
// iOS'ta olmayan: alttaki "Açık kaynak lisansları" kartı (Android: Google oss-licenses eklentisinin
// bağımlılıklardan ürettiği liste). iOS'ta karşılığı (lottie-ios, Kotlin kütüphaneleri) henüz yok; kart gizli.

/// Bir atıf satırı (Android: CreditsFragment.Credit).
struct ProfilKaynak: Identifiable {
    let id: Int
    /// Bölüm başlığının string adı.
    let bolum: String
    let atif: String
    let adres: String
    var lisansAdi: String? = nil
    var lisansAdresi: String? = nil
    var degistirildi: Bool = false
}

struct ProfilKaynaklarEkrani: View {
    let geri: () -> Void

    private static let ccBy4: String = "https://creativecommons.org/licenses/by/4.0/"

    /// Android: CreditsFragment.CREDITS (aynı sıra, aynı metin).
    static let kaynaklar: [ProfilKaynak] = [
        ProfilKaynak(
            id: 0, bolum: "credits_mascot", atif: "designed by Freepik - Magnific.com",
            adres: "https://www.magnific.com", degistirildi: true
        ),
        ProfilKaynak(
            id: 1, bolum: "credits_avatars", atif: "Personas by Draftbit", adres: "https://personas.draftbit.com",
            lisansAdi: "CC BY 4.0", lisansAdresi: ccBy4, degistirildi: true
        ),
        ProfilKaynak(
            id: 2, bolum: "credits_emoji", atif: "Noto Emoji Animation by Google",
            adres: "https://googlefonts.github.io/noto-emoji-animation/",
            lisansAdi: "CC BY 4.0", lisansAdresi: ccBy4, degistirildi: true
        ),
        ProfilKaynak(id: 3, bolum: "credits_icons", atif: "Designed by Magnific", adres: "https://www.magnific.com"),
        ProfilKaynak(
            id: 4, bolum: "credits_sounds", atif: "Sound Effect by floraphonic from Pixabay", adres: "https://pixabay.com"
        ),
        ProfilKaynak(id: 5, bolum: "credits_animations", atif: "LottieFiles", adres: "https://lottiefiles.com"),
        ProfilKaynak(
            id: 6, bolum: "credits_font", atif: "Baloo 2 by Ek Type", adres: "https://github.com/EkType/Baloo2",
            lisansAdi: "SIL Open Font License 1.1", lisansAdresi: "https://openfontlicense.org"
        ),
    ]

    var body: some View {
        VStack(spacing: 0) {
            baslik
            ProfilStil.ayarAyirici
                .frame(height: 2)
                .padding(.top, 3)
            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(Metin.al("credits_intro"))
                        .font(ProfilStil.yaziNormal(15))
                        .foregroundColor(ProfilStil.ayarIkincil)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom, 14)
                    ForEach(ProfilKaynaklarEkrani.kaynaklar) { kaynak in
                        ProfilKaynakSatiri(kaynak: kaynak)
                            .padding(.bottom, 10)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 24)
            }
        }
    }

    /// FrameLayout (üstten 5, yatay 16): solda 48x48 geri, ortada başlık (20, #BDBDBD).
    private var baslik: some View {
        ZStack {
            Text(Metin.al("credits_title"))
                .font(ProfilStil.yazi(20))
                .foregroundColor(ProfilStil.ayarBaslik)
            HStack(spacing: 0) {
                Button(action: geri) {
                    Image("ic_arrow_back")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(Color.white)
                        .frame(width: 24, height: 24)
                        .frame(width: 48, height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(Metin.al("fragment_add_friend_btn_back_desc")))
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 5)
    }
}

/// CreditsFragment.row: 20 pt köşe, 2 pt #66FFFFFF çizgi, iç boşluk 18 / 14.
struct ProfilKaynakSatiri: View {
    let kaynak: ProfilKaynak

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al(kaynak.bolum))
                .font(ProfilStil.yazi(13))
                .foregroundColor(ProfilStil.ayarIkincil)
            Text(kaynak.atif)
                .font(ProfilStil.yazi(17))
                .foregroundColor(ProfilStil.ayarMetni)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 4)
            ProfilKaynakBaglantisi(etiket: ProfilKaynakSatiri.alanAdi(kaynak.adres), adres: kaynak.adres)
            if let ad = kaynak.lisansAdi, let adres = kaynak.lisansAdresi {
                ProfilKaynakBaglantisi(etiket: ad, adres: adres)
            }
            if kaynak.degistirildi {
                Text(Metin.al("credits_modified"))
                    .font(ProfilStil.yaziNormal(13))
                    .foregroundColor(ProfilStil.ayarIkincil)
                    .padding(.top, 4)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(Color.white.opacity(0.4), lineWidth: 2))
    }

    /// Android: `Uri.parse(url).host?.removePrefix("www.") ?: url`.
    static func alanAdi(_ adres: String) -> String {
        guard let host = URL(string: adres)?.host else { return adres }
        if host.hasPrefix("www.") {
            return String(host.dropFirst(4))
        }
        return host
    }
}

/// Altı çizili bağlantı: 14, #58A6FF, üstten 4 / alttan 2.
struct ProfilKaynakBaglantisi: View {
    let etiket: String
    let adres: String

    var body: some View {
        Button(action: ac) {
            Text(etiket)
                .font(ProfilStil.yaziNormal(14))
                .underline()
                .foregroundColor(ProfilStil.renk(0x58A6FF))
                .padding(.top, 4)
                .padding(.bottom, 2)
        }
        .buttonStyle(.plain)
    }

    private func ac() {
        guard let url = URL(string: adres) else { return }
        UIApplication.shared.open(url)
    }
}
