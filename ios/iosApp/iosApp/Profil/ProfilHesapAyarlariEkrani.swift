import SwiftUI
import UIKit

// "Ayarlar" ekranı. Android: AccountSettingsFragment + res/layout/fragment_account_settings.xml.
//
// Güncel Android kartları (yukarıdan aşağı) ve iOS'taki durumu:
//   Hesap: Profil · Ses ve Bildirimler · Dil · (Reklam gizlilik seçenekleri)
//   Sınıf: Sınıfa katıl / Sınıfım                         — iOS'ta GİZLİ (sınıf/kurs özelliği ilk sürümde yok)
//   Veliler için: Veli Paneli · Arkadaşını davet et        — iOS'ta GİZLİ (veli paneli ilk iOS sürümünün dışında;
//                                                            davet sistemi / Universal Links henüz yok)
//   Ödemeler ve abonelikler: Abonelik İptali (plan Free değilse)
//   Destek: Geri bildirim · Kaynaklar ve lisanslar
//   OTURUMU KAPAT
// "Reklam gizlilik seçenekleri" Android'de yalnızca UMP "gerekli" dediğinde (AEA/İngiltere/İsviçre) görünür,
// Türkiye'de gizli; iOS'ta reklam SDK'sı / UMP henüz yok, satır gizli.
// Hesap silme Android'de bu ekranda değil, "Profil" satırının açtığı profil düzenleme ekranındadır
// (EditProfileFragment; App Store kuralı 5.1.1(v) için uygulama içinde) — akış aynen korunmuştur.

struct ProfilHesapAyarlariEkrani: View {
    @ObservedObject var gezinme: ProfilGezinme
    /// "Ödemeler ve abonelikler" bölümü plan okunana kadar gizlidir; plan "Free" ise hiç görünmez.
    @State private var odemelerGorunur: Bool = false
    @State private var dilSeciciAcik: Bool = false

    /// App Store abonelik yönetimi (Android'de Play Store abonelik sayfası açılır).
    private static let abonelikAdresi = "https://apps.apple.com/account/subscriptions"

    var body: some View {
        ZStack {
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
            if dilSeciciAcik {
                ProfilDilSecici(kapat: { dilSeciciAcik = false })
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.15), value: dilSeciciAcik)
        .task { await planiYukle() }
    }

    /// accountSettingsHeader: ortada "Ayarlar" (20, #BDBDBD), solda 48x48 geri düğmesi.
    private var baslik: some View {
        ZStack {
            Text(Metin.al("fragment_account_settings_account_settings_header_title"))
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
                .accessibilityLabel(Text(Metin.al("fragment_followers_following_btn_back_desc")))
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 5)
    }

    private var icerik: some View {
        VStack(alignment: .leading, spacing: 0) {
            hesapBolumu
            if odemelerGorunur {
                ProfilAyarBolumBasligi(metin: Metin.al("fragment_account_settings_tv_payments_header"))
                ProfilAyarKarti {
                    ProfilAyarSatiri(
                        metin: Metin.al("fragment_account_settings_text_6"),
                        eylem: { abonelikSayfasiniAc() }
                    )
                }
                .padding(.bottom, 28)
            }
            destekBolumu
            ProfilCerceveliDugme(
                metin: Metin.al("fragment_account_settings_btn_account_settings_logout"),
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

    private var hesapBolumu: some View {
        VStack(alignment: .leading, spacing: 0) {
            ProfilAyarBolumBasligi(metin: Metin.al("fragment_account_settings_text"))
            ProfilAyarKarti {
                ProfilAyarSatiri(
                    metin: Metin.al("fragment_account_settings_text_2"),
                    eylem: { gezinme.ac(.profilDuzenle) }
                )
                ProfilStil.ayarAyirici.frame(height: 1)
                // Android: SoundSettingsFragment → Profil/ProfilSesAyarlariEkrani.
                ProfilAyarSatiri(
                    metin: Metin.al("fragment_account_settings_text_3"),
                    eylem: { gezinme.ac(.sesAyarlari) }
                )
                ProfilStil.ayarAyirici.frame(height: 1)
                // Dil (Android: AppLanguage; seçim uygulamayı yeniden başlatır, iOS'ta kök görünüm yeniden
                // kurulur). Değer kayıtlı seçimin adı (Android: languageLabel(AppLanguage.choice)).
                ProfilAyarSatiri(
                    metin: Metin.al("settings_language"),
                    deger: ProfilDilSecici.etiket(Metin.secim),
                    eylem: { dilSeciciAcik = true }
                )
            }
            .padding(.bottom, 28)
        }
    }

    private var destekBolumu: some View {
        VStack(alignment: .leading, spacing: 0) {
            ProfilAyarBolumBasligi(metin: Metin.al("fragment_account_settings_text_7"))
            ProfilAyarKarti {
                ProfilAyarSatiri(
                    metin: Metin.al("fragment_account_settings_text_8"),
                    eylem: { gezinme.ac(.geriBildirim) }
                )
                ProfilStil.kartIciAyirici.frame(height: 1)
                ProfilAyarSatiri(metin: Metin.al("credits_title"), eylem: { gezinme.ac(.kaynaklar) })
            }
            .padding(.bottom, 28)
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
            // Android: `ui_play_store_acilamadi` ("Play Store açılamadı"); iOS karşılığı metinlerde yok.
            gezinme.bildir(Metin.al("ios_profil_app_store_acilamadi"))
        }
    }

    /// Android: onay sorulmadan FCM belirteci temizlenir, oturum kapatılır, giriş ekranı açılır.
    private func oturumuKapat() {
        gezinme.depo.cikisYap()
        gezinme.oturumKapandi()
    }
}

/// Dil seçimi (Android: showLanguagePicker — başlık "Dil", tek seçimli liste, "Vazgeç").
/// Seçenekler Android'deki sırayla: Telefonun dili, Türkçe, English, Bahasa Indonesia, العربية
/// (dil adları kendi dillerinde). Etkin olanlar: Telefonun dili ve `Metin.acikDiller` (bu sürümde Türkçe ve
/// English); açılmamış diller soluk ve dokunulmaz. Seçim `Metin.secimiUygula` ile saklanır ve uygulanır
/// (Android: AppLanguage.apply); aynı seçenek seçilirse yalnızca pencere kapanır.
struct ProfilDilSecici: View {
    let kapat: () -> Void

    /// Seçimin görünen adı (Android: AccountSettingsFragment.languageLabel).
    static func etiket(_ secim: String) -> String {
        if secim == Metin.sistemSecimi {
            return Metin.al("settings_language_system")
        }
        return Metin.al("settings_language_" + secim)
    }

    private struct Secenek: Identifiable {
        let id: String
        let etiket: String
        let etkin: Bool
    }

    private var secenekler: [Secenek] {
        var liste: [Secenek] = [
            Secenek(id: Metin.sistemSecimi, etiket: ProfilDilSecici.etiket(Metin.sistemSecimi), etkin: true)
        ]
        for kod in Metin.tumDiller {
            liste.append(Secenek(id: kod, etiket: ProfilDilSecici.etiket(kod), etkin: Metin.acikDiller.contains(kod)))
        }
        return liste
    }

    private func sec(_ kod: String) {
        kapat()
        Metin.secimiUygula(kod)
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()
                .onTapGesture(perform: kapat)
            pencere
                .padding(.horizontal, 32)
        }
    }

    private var pencere: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("settings_language"))
                .font(ProfilStil.yazi(20))
                .foregroundColor(ProfilStil.ayarMetni)
                .padding(.horizontal, 24)
                .padding(.top, 20)
                .padding(.bottom, 8)
            ForEach(secenekler) { secenek in
                ProfilDilSatiri(
                    etiket: secenek.etiket,
                    secili: secenek.id == Metin.secim,
                    etkin: secenek.etkin,
                    sec: { sec(secenek.id) }
                )
            }
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                Button(action: kapat) {
                    Text(Metin.al("class_cancel"))
                        .font(ProfilStil.yazi(16))
                        .foregroundColor(Color("dark_primary"))
                        .padding(.horizontal, 16)
                        .frame(height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 8)
            .padding(.bottom, 4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color("bright_background_color")))
    }
}

/// Tek seçimli liste satırı (radyo düğmesi + etiket, 48 pt).
struct ProfilDilSatiri: View {
    let etiket: String
    let secili: Bool
    let etkin: Bool
    let sec: () -> Void

    var body: some View {
        Button(action: sec) {
            HStack(spacing: 20) {
                ZStack {
                    Circle()
                        .strokeBorder(secili ? Color("dark_primary") : ProfilStil.soluk, lineWidth: 2)
                        .frame(width: 20, height: 20)
                    if secili {
                        Circle()
                            .fill(Color("dark_primary"))
                            .frame(width: 10, height: 10)
                    }
                }
                Text(etiket)
                    .font(ProfilStil.yazi(16))
                    .foregroundColor(ProfilStil.ayarMetni)
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 24)
            .frame(height: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!etkin)
        .opacity(etkin ? 1.0 : 0.4)
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

/// 72 pt satır: metin (20, #E6EEF2) + isteğe bağlı değer (16, #9FB3BF, oktan 8 önce) + sağda 20x20 ok
/// (#607887); yatay iç boşluk 18.
struct ProfilAyarSatiri: View {
    let metin: String
    var deger: String? = nil
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 0) {
                Text(metin)
                    .font(ProfilStil.yazi(20))
                    .foregroundColor(ProfilStil.ayarMetni)
                    .lineLimit(1)
                Spacer(minLength: 8)
                if let degerMetni = deger {
                    Text(degerMetni)
                        .font(ProfilStil.yazi(16))
                        .foregroundColor(ProfilStil.ayarIkincil)
                        .lineLimit(1)
                        .padding(.trailing, 8)
                }
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
