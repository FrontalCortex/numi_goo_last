import Combine
import SwiftUI

// Giriş akışının dışa açılan tek görünümü. Android'in güncel akışı (bkz. GirisModeli.swift):
//
//   açılış (SplashActivity, en az 1200 ms + zorunlu güncelleme denetimi) → SplashRules.route:
//     - oturum var            → tek cihaz denetimi → `girisYapildi()`
//     - başlangıç hiç görülmedi ve oturum yok (ilk açılış) → `misafirOlarakDevam(.ogretici)`
//       (bu cihazda daha önce hesap açıldıysa `.harita`); Android misafiri öğreticiye sokar,
//       ilk ders bitince başlangıç ekranını açar
//     - başlangıç görüldü, oturum yok → başlangıç ekranı
//   başlangıç (LoginStartActivity: maskot, "Başla", "Zaten hesabım var", öğretmen geçişi)
//     "Başla"          → kayıt soruları (UserInfoFragment) → kayıt (RegisterActivity)
//     "Zaten hesabım var" → giriş (LoginActivity; öğretmende TeacherLoginActivity)
//   kayıt → doğrulama kodu (OtpVerificationFragment) | 13 yaş altı: veli onayı → veli kodu
//   giriş → doğrulama kodu; kayıtlı değilse kayıt sorularına yönlendirilir.
@MainActor
struct GirisAkisi: View {
    @StateObject private var model: GirisModeli

    private let saat = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    /// - Parameters:
    ///   - servis: kimlik servisi (şimdilik SahteKimlikServisi).
    ///   - girisYapildi: oturum açıldı ve bu cihaza izinli.
    ///   - misafirOlarakDevam: hesapsız ilk açılış (SplashRules.Route.GUEST_MAIN); hedef öğretici ya
    ///     da harita. Verilmezse bir şey yapılmaz (ekran galerisi).
    ///   - acilisiAtla: true ise açılış ekranı gösterilmeden başlangıç ekranından başlanır
    ///     (Android: çıkıştan sonra LoginStartActivity doğrudan açılır).
    ///   - ornek: ekran galerisi için hazır durum (yönlendirme yapılmaz, bayrak yazılmaz).
    ///   - bildirimServisi: hatırlatma sorusundaki bildirim izni (şimdilik sahte; izin istemi çağrılmaz).
    ///   - surumServisi: zorunlu güncelleme denetimi (şimdilik sahte; kilit yok).
    @MainActor
    init(
        servis: KimlikServisi,
        girisYapildi: @escaping () -> Void,
        misafirOlarakDevam: @escaping (GirisMisafirHedefi) -> Void = { _ in },
        acilisiAtla: Bool = false,
        ornek: GirisOrnekAdimi? = nil,
        bildirimServisi: GirisBildirimServisi = SahteGirisBildirimServisi(),
        surumServisi: GirisSurumServisi = SahteGirisSurumServisi()
    ) {
        let ilk: GirisEkran = acilisiAtla ? GirisEkran.baslangic : GirisEkran.acilis
        let nesne = GirisModeli(
            servis: servis,
            depo: GirisYerelDepo(),
            bildirimServisi: bildirimServisi,
            surumServisi: surumServisi,
            baslangic: ilk,
            girisYapildi: girisYapildi,
            misafirOlarakDevam: misafirOlarakDevam
        )
        if let adim = ornek {
            GirisOrnekleri.kur(nesne, adim)
        }
        _model = StateObject(wrappedValue: nesne)
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color("background_color").ignoresSafeArea()
            ekranGorunumu
                .allowsHitTesting(!model.mesgul)
            if let bildirim = model.bildirim {
                GirisBildirimGorunumu(metin: bildirim.metin)
                    .transition(.opacity)
            }
            if model.surumKilitli {
                GirisSurumKilidiGorunumu()
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .onReceive(saat) { _ in
            model.saatVurdu()
        }
        .preferredColorScheme(.dark)
    }

    @ViewBuilder
    private var ekranGorunumu: some View {
        switch model.ekran {
        case .acilis:
            GirisAcilisGorunumu(model: model)
        case .baslangic:
            GirisBaslangicGorunumu(model: model)
        case .ogrenciGiris:
            GirisEpostaGorunumu(model: model)
        case .sorular:
            sorularGorunumu
        case .kayit:
            KayitGorunumu(model: model)
        case .veliOnayi:
            GirisVeliOnayiGorunumu(model: model)
        case .kod(let eposta, let tur):
            GirisKodGorunumu(model: model, eposta: eposta, tur: tur)
        case .ogretmenGiris:
            GirisOgretmenGorunumu(model: model)
        case .sifremiUnuttum:
            GirisSifremiUnuttumGorunumu(model: model)
        case .yeniSifre(let eposta, let kod):
            GirisYeniSifreGorunumu(model: model, eposta: eposta, kod: kod)
        }
    }

    @ViewBuilder
    private var sorularGorunumu: some View {
        if let akis = model.sorular {
            GirisSorulariGorunumu(akis: akis, geri: { model.geri() })
        } else {
            Color.clear
        }
    }
}

/// Zorunlu güncelleme penceresi (ForceUpdateManager.showBlockingDialog): kapatılamaz.
/// Android'deki "Çık" düğmesi iOS'ta yok (App Store yönergesi: uygulama kendini kapatmaz).
/// YAPILACAK: "Güncelle" App Store sayfasını açmalı (uygulama kimliği henüz yok).
@MainActor
struct GirisSurumKilidiGorunumu: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.6).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                Text(Metin.al("ui_guncelleme_gerekli"))
                    .font(Tema.yazi(20))
                    .foregroundColor(Color("dark_text"))
                Text(Metin.al("ui_bu_surum_artik_kullanilamiyor"))
                    .font(Tema.yazi(15, kalin: false))
                    .foregroundColor(Color("dark_text_secondary"))
                    .padding(.top, 12)
                HStack {
                    Spacer(minLength: 0)
                    GirisBaglantiDugmesi(metin: Metin.al("ui_guncelle"), yaziBoyu: 15) {}
                }
                .padding(.top, 16)
            }
            .padding(24)
            .background(RoundedRectangle(cornerRadius: 8).fill(Color("dark_surface")))
            .padding(.horizontal, 32)
        }
    }
}
