import SwiftUI
import UIKit

// "Ayarlar" ekranı: Profil, Ses ve Bildirimler, Abonelik İptali, Geri bildirim, OTURUMU KAPAT.
// Android: AccountSettingsFragment + res/layout/fragment_account_settings.xml.
// Hesap silme Android'de bu ekranda değil, "Profil" satırının açtığı profil düzenleme ekranındadır
// (EditProfileFragment); akış aynen korunmuştur.

struct ProfilHesapAyarlariEkrani: View {
    @ObservedObject var gezinme: ProfilGezinme
    /// "Ödemeler ve abonelikler" bölümü plan okunana kadar gizlidir; plan "Free" ise hiç görünmez.
    @State private var odemelerGorunur: Bool = false

    /// App Store abonelik yönetimi (Android'de Play Store abonelik sayfası açılır).
    private static let abonelikAdresi = "https://apps.apple.com/account/subscriptions"

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                baslik
                ProfilStil.ayarAyirici
                    .frame(height: 2)
                    .padding(.top, 3)
                icerik
                    .padding(.horizontal, 16)
                    .padding(.top, 14)
                    .padding(.bottom, 24)
            }
        }
        .task { await planiYukle() }
    }

    /// accountSettingsHeader: ortada "Ayarlar" (20, #BDBDBD), solda 48x48 geri düğmesi.
    private var baslik: some View {
        ZStack {
            Text("Ayarlar")
                .font(ProfilStil.yazi(20))
                .foregroundColor(ProfilStil.ayarBaslik)
            HStack(spacing: 0) {
                Button(action: { gezinme.geri() }) {
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
                .accessibilityLabel(Text("Geri"))
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 5)
    }

    private var icerik: some View {
        VStack(alignment: .leading, spacing: 0) {
            ProfilAyarBolumBasligi(metin: "Hesap")
            ProfilAyarKarti {
                ProfilAyarSatiri(metin: "Profil", eylem: { gezinme.ac(.profilDuzenle) })
                ProfilStil.ayarAyirici.frame(height: 1)
                // Android: SoundSettingsFragment (başka klasörün ekranı; henüz bağlı değil).
                ProfilAyarSatiri(metin: "Ses ve Bildirimler", eylem: {})
            }
            .padding(.bottom, 28)

            if odemelerGorunur {
                ProfilAyarBolumBasligi(metin: "Ödemeler ve abonelikler")
                ProfilAyarKarti {
                    ProfilAyarSatiri(metin: "Abonelik İptali", eylem: { abonelikSayfasiniAc() })
                }
                .padding(.bottom, 28)
            }

            ProfilAyarBolumBasligi(metin: "Destek")
            ProfilAyarKarti {
                ProfilAyarSatiri(metin: "Geri bildirim", eylem: { gezinme.ac(.geriBildirim) })
            }
            .padding(.bottom, 28)

            ProfilCerceveliDugme(
                metin: "OTURUMU KAPAT",
                metinRengi: ProfilStil.cikisMetni,
                zemin: Color("background_color"),
                cizgi: ProfilStil.cikisCizgisi,
                kose: 20,
                yukseklik: 52,
                yaziBoyu: 20,
                eylem: { oturumuKapat() }
            )
        }
    }

    private func planiYukle() async {
        guard let uid = gezinme.depo.oturum?.uid else { return }
        // Android: okuma başarısızsa bölüm gizli kalır.
        guard let belge = try? await gezinme.depo.profilBelgesi(uid: uid, baskasi: false) else { return }
        odemelerGorunur = ProfilOrtak.odemelerGorunur(plan: belge.plan)
    }

    private func abonelikSayfasiniAc() {
        guard let adres = URL(string: ProfilHesapAyarlariEkrani.abonelikAdresi) else { return }
        if UIApplication.shared.canOpenURL(adres) {
            UIApplication.shared.open(adres)
        } else {
            gezinme.bildir("App Store açılamadı")
        }
    }

    /// Android: onay sorulmadan FCM belirteci temizlenir, oturum kapatılır, giriş ekranı açılır.
    private func oturumuKapat() {
        gezinme.depo.cikisYap()
        gezinme.oturumKapandi()
    }
}

/// Bölüm başlığı: 22, #E6EEF2, alttan 10.
struct ProfilAyarBolumBasligi: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(ProfilStil.yazi(22))
            .foregroundColor(ProfilStil.ayarMetni)
            .padding(.bottom, 10)
    }
}

/// MaterialCardView: 20 pt köşe, 2 pt #66FFFFFF çizgi, zemin background_color.
struct ProfilAyarKarti<Icerik: View>: View {
    @ViewBuilder let icerik: () -> Icerik

    var body: some View {
        VStack(spacing: 0) {
            icerik()
        }
        .background(RoundedRectangle(cornerRadius: 20).fill(Color("background_color")))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(Color.white.opacity(0.4), lineWidth: 2))
    }
}

/// 72 pt satır: metin (20, #E6EEF2) + sağda 20x20 ok (#607887); yatay iç boşluk 18.
struct ProfilAyarSatiri: View {
    let metin: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 0) {
                Text(metin)
                    .font(ProfilStil.yazi(20))
                    .foregroundColor(ProfilStil.ayarMetni)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Image("dualingo_arrow_ic")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(ProfilStil.ayarOku)
                    .frame(width: 20, height: 20)
            }
            .padding(.horizontal, 18)
            .frame(height: 72)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
