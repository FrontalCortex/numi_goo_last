import SwiftUI

// "Profili Düzenle" ekranı: ad, (salt okunur) kullanıcı kodu ve e-posta, yeni parola, kaydet,
// HESABI KALICI OLARAK SİL.
// Android: EditProfileFragment + res/layout/fragment_edit_profile.xml; hesap silme
// AccountDeletionHelper.performCompleteAccountDeletion.
//
// Hesap silme akışı (Android'deki gibi; App Store kuralı 5.1.1(v) gereği uygulama içinde):
//   1. Onay penceresi ("Hesabı Sil" / SİL / İPTAL).
//   2. Kapatılamayan "siliniyor" katmanı.
//   3. Son giriş 270 sn'den eskiyse hiçbir veri silinmeden: bildirim + oturum kapatma + giriş ekranı
//      (kullanıcı yeniden girip hemen silmeyi dener).
//   4. Değilse depo siler: başarı → bildirim + oturum kapatma + giriş ekranı; hata → "Hata: …".

@MainActor
final class ProfilDuzenleModeli: ObservableObject {
    @Published var ad: String = ""
    @Published var kullaniciKodu: String = ""
    @Published var eposta: String = ""
    @Published var parola: String = ""
    @Published var adHatasi: String? = nil
    @Published var parolaAlaniGorunur: Bool = true
    @Published var kaydediliyor: Bool = false
    @Published var silmeOnayiAcik: Bool = false
    @Published var siliniyor: Bool = false

    private let depo: ProfilDeposu
    private var yuklendi: Bool = false

    init(depo: ProfilDeposu) {
        self.depo = depo
    }

    var kaydetMetni: String {
        return kaydediliyor ? Metin.al("ui_kaydediliyor") : Metin.al("ui_degisiklikleri_kaydet")
    }

    /// Android: loadUserData.
    func yukle() async {
        if yuklendi { return }
        yuklendi = true
        guard let oturum = depo.oturum else { return }
        eposta = oturum.eposta ?? ""
        do {
            let belge = try await depo.profilBelgesi(uid: oturum.uid, baskasi: false)
            ad = belge?.ad ?? ""
            kullaniciKodu = ProfilOrtak.kullaniciKoduAlani(belge?.kullaniciKodu ?? "")
            parolaAlaniGorunur = ProfilOrtak.parolaAlaniGorunur(
                saglayicilar: oturum.saglayicilar, rol: belge?.rol ?? "", belgeOkundu: true
            )
        } catch {
            parolaAlaniGorunur = ProfilOrtak.parolaAlaniGorunur(
                saglayicilar: oturum.saglayicilar, rol: nil, belgeOkundu: false
            )
        }
    }

    /// Android: setupTextValidation + maxLength=25.
    func adDegisti() {
        if ad.count > 25 {
            ad = String(ad.prefix(25))
        }
        adHatasi = ProfilOrtak.adHatasi(ad.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    /// Android: saveChanges → updatePassword → finishSaving.
    func kaydet(gezinme: ProfilGezinme) {
        guard depo.oturum != nil else { return }
        let yeniAd = ad.trimmingCharacters(in: .whitespacesAndNewlines)
        let yeniParola = parola
        if let hata = ProfilOrtak.adHatasi(yeniAd) {
            gezinme.bildir(hata)
            return
        }
        kaydediliyor = true
        Task {
            do {
                try await depo.adGuncelle(yeniAd)
            } catch ProfilHata.veritabani {
                gezinme.bildir(Metin.al("ui_firestore_guncellenemedi"))
                self.kaydediliyor = false
                return
            } catch {
                gezinme.bildir(Metin.al("ui_auth_profil_guncellenemedi"))
                self.kaydediliyor = false
                return
            }

            if ProfilOrtak.parolaGuncellenmeli(parola: yeniParola, alanGorunur: self.parolaAlaniGorunur) {
                if let hata = ProfilOrtak.parolaHatasi(yeniParola) {
                    gezinme.bildir(hata)
                    self.kaydediliyor = false
                    return
                }
                do {
                    try await depo.parolaGuncelle(yeniParola)
                    gezinme.bildir(Metin.al("ui_parola_basariyla_guncellendi"))
                    self.parola = ""
                } catch ProfilHata.yenidenGirisGerekli {
                    gezinme.bildir(Metin.al("ui_guvenlik_nedeniyle_parolayi_degistirmek_icin"), uzun: true)
                    self.cikVeYonlendir(gezinme: gezinme)
                    return
                } catch {
                    gezinme.bildir(Metin.al("ui_parola_guncellenemedi_x", ProfilSayfaModeli.hataMetni(error)))
                    self.kaydediliyor = false
                    return
                }
            }

            gezinme.bildir(Metin.al("ui_degisiklikler_kaydedildi"))
            self.kaydediliyor = false
            gezinme.yenilemeSayaci += 1
            gezinme.geri()
        }
    }

    /// Android: deleteAccount.
    func hesabiSil(gezinme: ProfilGezinme) {
        guard let oturum = depo.oturum else {
            gezinme.bildir(Metin.al("ui_hata_x", ProfilOrtak.kullaniciBulunamadi), uzun: true)
            return
        }
        // Güvenlik denetimi: son giriş eskiyse Firestore verisine dokunmadan durulur.
        let simdiMs = Int64(Date().timeIntervalSince1970 * 1000)
        if ProfilOrtak.yenidenGirisGerekli(sonGirisMs: oturum.sonGirisMs, simdiMs: simdiMs) {
            gezinme.bildir(Metin.al("ui_guvenlik_nedeniyle_hesabi_silmek_icin_lutfen"), uzun: true)
            cikVeYonlendir(gezinme: gezinme)
            return
        }
        siliniyor = true
        Task {
            let sonuc = await depo.hesabiSil()
            self.siliniyor = false
            switch sonuc {
            case .basarili:
                gezinme.bildir(Metin.al("ui_hesabiniz_basariyla_silindi"))
                self.cikVeYonlendir(gezinme: gezinme)
            case .yenidenGirisGerekli:
                gezinme.bildir(Metin.al("ui_guvenlik_nedeniyle_hesabi_silmek_icin_lutfen"), uzun: true)
                self.cikVeYonlendir(gezinme: gezinme)
            case .hata(let mesaj):
                gezinme.bildir(Metin.al("ui_hata_x", mesaj), uzun: true)
            }
        }
    }

    /// Android: logoutAndRedirect — auth.signOut() + LoginStartActivity.
    private func cikVeYonlendir(gezinme: ProfilGezinme) {
        depo.cikisYap()
        gezinme.oturumKapandi()
    }
}

struct ProfilDuzenleEkrani: View {
    @ObservedObject var gezinme: ProfilGezinme
    @StateObject private var model: ProfilDuzenleModeli

    init(gezinme: ProfilGezinme) {
        self.gezinme = gezinme
        _model = StateObject(wrappedValue: ProfilDuzenleModeli(depo: gezinme.depo))
    }

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                ProfilUstCubuk(
                    baslik: Metin.al("fragment_edit_profile_text"),
                    simge: "ic_arrow_back",
                    yaziBoyu: 20,
                    solBosluk: 16,
                    sagBosluk: 16,
                    geri: { gezinme.geri() }
                )
                ScrollView(.vertical, showsIndicators: false) {
                    form.padding(24)
                }
            }
            if model.siliniyor {
                silmeKatmani
            }
        }
        .task { await model.yukle() }
        .onChange(of: model.ad) { _ in model.adDegisti() }
        .alert(Text(Metin.al("ui_hesabi_sil")), isPresented: $model.silmeOnayiAcik) {
            Button(Metin.al("ui_sil"), role: .destructive) { model.hesabiSil(gezinme: gezinme) }
            Button(Metin.al("ui_iptal"), role: .cancel) {}
        } message: {
            Text(Metin.al("ui_hesabinizi_kalici_olarak_silmek_istediginize"))
        }
    }

    private var form: some View {
        VStack(alignment: .leading, spacing: 0) {
            ProfilAlanEtiketi(metin: Metin.al("fragment_edit_profile_text_2"))
            ProfilGirisKutusu {
                TextField("", text: $model.ad, prompt: Text(Metin.al("fragment_edit_profile_et_name_hint")).foregroundColor(Color("gray")))
                    .font(ProfilStil.yazi(16))
                    .foregroundColor(Color.white)
                    .textInputAutocapitalization(.words)
                    .autocorrectionDisabled(true)
            }
            .padding(.bottom, 24)
            if let hata = model.adHatasi {
                Text(hata)
                    .font(ProfilStil.yazi(12))
                    .foregroundColor(Color("red"))
            }

            ProfilAlanEtiketi(metin: Metin.al("fragment_edit_profile_text_3"))
            ProfilGirisKutusu {
                Text(model.kullaniciKodu)
                    .font(ProfilStil.yazi(16))
                    .foregroundColor(Color("gray"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.bottom, 24)

            ProfilAlanEtiketi(metin: Metin.al("fragment_edit_profile_text_4"))
            ProfilGirisKutusu {
                Text(model.eposta)
                    .font(ProfilStil.yazi(16))
                    .foregroundColor(Color("gray"))
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.bottom, 24)

            if model.parolaAlaniGorunur {
                parolaBolumu
            }

            // Bu iki düğmede app:cornerRadius yoktur: Material 2 varsayılanı 4dp.
            ProfilCerceveliDugme(
                metin: model.kaydetMetni,
                zemin: Color("outline_button_bg"),
                cizgi: Color("outline_button_stroke"),
                cizgiKalinligi: 1,
                kose: 4,
                yukseklik: 48,
                etkin: !model.kaydediliyor,
                eylem: { model.kaydet(gezinme: gezinme) }
            )
            .padding(.top, 22)
            .padding(.bottom, 12)

            ProfilCerceveliDugme(
                metin: Metin.al("fragment_edit_profile_btn_delete_account"),
                metinRengi: Color("red"),
                zemin: Color("outline_button_bg"),
                cizgi: Color("outline_button_stroke"),
                cizgiKalinligi: 1,
                kose: 4,
                yukseklik: 48,
                eylem: { model.silmeOnayiAcik = true }
            )
        }
    }

    private var parolaBolumu: some View {
        VStack(alignment: .leading, spacing: 0) {
            ProfilAlanEtiketi(metin: Metin.al("fragment_edit_profile_text_5"))
            ProfilGirisKutusu {
                SecureField(
                    "", text: $model.parola,
                    prompt: Text(Metin.al("fragment_edit_profile_et_password_hint")).foregroundColor(Color("gray"))
                )
                .font(ProfilStil.yazi(16))
                .foregroundColor(Color.white)
            }
            .padding(.bottom, 8)
            Text(Metin.al("fragment_edit_profile_text_6"))
                .font(ProfilStil.yazi(12))
                .foregroundColor(Color("gray"))
                .fixedSize(horizontal: false, vertical: true)
                .padding(.bottom, 24)
        }
    }

    /// Android: iptal edilemeyen ProgressDialog.
    private var silmeKatmani: some View {
        ZStack {
            Color.black.opacity(0.6).ignoresSafeArea()
            VStack(spacing: 16) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
                    .scaleEffect(1.4)
                Text(Metin.al("ui_hesabiniz_ve_iliskili_verileriniz_siliniyor"))
                    .font(ProfilStil.yazi(15))
                    .foregroundColor(Color.white)
                    .multilineTextAlignment(.center)
            }
            .padding(24)
            .background(RoundedRectangle(cornerRadius: 16).fill(Color("bright_background_color")))
            .padding(.horizontal, 32)
        }
    }
}

/// Alan etiketi: 14, @color/gray, alttan 8.
struct ProfilAlanEtiketi: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(ProfilStil.yazi(14))
            .foregroundColor(Color("gray"))
            .padding(.bottom, 8)
    }
}

/// bg_edit_profile_input: 56 pt yükseklik, 12 pt köşe, zemin #203137, 1 pt #30363D çizgi, yatay iç boşluk 16.
struct ProfilGirisKutusu<Icerik: View>: View {
    @ViewBuilder let icerik: () -> Icerik

    var body: some View {
        icerik()
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(RoundedRectangle(cornerRadius: 12).fill(ProfilStil.girisZemini))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(ProfilStil.girisCizgisi, lineWidth: 1))
    }
}
