import SwiftUI

// Profil sekmesinin giriş noktası.
//
// Android'deki akış: ProfileFragment (alt çubuğun profil sekmesi) → aynı kapsayıcıda `replace` +
// `addToBackStack` ile açılan alt ekranlar:
//   ARKADAŞ EKLE → AddFriendFragment → (satıra dokununca) ProfileFragment.newInstance(uid)
//   Takip edilen / Takipçi → FollowersFollowingFragment → ProfileFragment.newInstance(uid)
//   Ayarlar → AccountSettingsFragment → EditProfileFragment (hesap silme burada) / FeedbackFragment /
//             CreditsFragment ("Kaynaklar ve lisanslar")
//   Avatar alanı → AvatarCustomFragment (avatar düzenleme; eski 12'li AvatarPickerFragment silindi)
// Burada bunlar tek kök görünümde, kendi geri yığınıyla (ProfilGezinme.yigin) toplanır; geçiş
// Android'deki slide_in_right / slide_out_right karşılığı sağdan kaymadır.
//
// Kupa geçmişi (CupHistoryFragment), rozet ayrıntısı (BadgeDetailFragment) ve çıkış sonrası giriş
// ekranı başka klasörlere aittir; kapanışlarla dışarı verilir.

/// Geri yığınındaki ekranlar.
enum ProfilHedef: Equatable {
    case kullanici(String)
    case takipListesi(Int)
    case arkadasAra
    case hesapAyarlari
    case sesAyarlari
    case profilDuzenle
    case geriBildirim
    case avatarDuzenle
    case kaynaklar
}

struct ProfilRota: Identifiable, Equatable {
    let id: Int
    let hedef: ProfilHedef
}

/// Profil ekranlarının ortak durumu: geri yığını, bildirim balonu, depo ve dışa açılan kapanışlar.
@MainActor
final class ProfilGezinme: ObservableObject {
    @Published var yigin: [ProfilRota] = []
    @Published var bildirim: ProfilBildirim? = nil
    /// Profil düzenlemeden dönünce kök profilin yeniden yüklenmesi için artırılır.
    @Published var yenilemeSayaci: Int = 0
    /// Avatar kaydedilince artırılır; kök profil yerel kopyadan yeniden çizer.
    @Published var avatarSayaci: Int = 0

    let depo: ProfilDeposu
    var kupaAc: ((ProfilKupaSecimi) -> Void)?
    var rozetlerAc: (() -> Void)?
    var magazaAc: (() -> Void)?
    var cikisYapildi: (() -> Void)?
    var avatarDegisti: ((String) -> Void)?

    private var rotaSayaci: Int = 0
    private var bildirimSayaci: Int = 0

    init(depo: ProfilDeposu) {
        self.depo = depo
    }

    func ac(_ hedef: ProfilHedef) {
        rotaSayaci += 1
        yigin.append(ProfilRota(id: rotaSayaci, hedef: hedef))
    }

    func geri() {
        if !yigin.isEmpty {
            yigin.removeLast()
            // Android: geri dönünce ProfileFragment yeniden kurulur ve loadUserData çalışır; iOS'ta kök profil
            // bellekte kaldığı için `onAppear` bir daha çalışmaz. Alt ekranda takip edilip bırakılmış olabilir
            // (başkasının profili, takip listesi, arkadaş arama): kök profilin sayaçları yenilensin.
            yenilemeSayaci += 1
        }
    }

    /// Android: Toast (kısa 2 sn, uzun 3,5 sn).
    func bildir(_ metin: String, uzun: Bool = false) {
        bildirimSayaci += 1
        let kimlik = bildirimSayaci
        bildirim = ProfilBildirim(id: kimlik, metin: metin)
        let sure: UInt64 = uzun ? 3_500_000_000 : 2_000_000_000
        Task {
            try? await Task.sleep(nanoseconds: sure)
            if self.bildirim?.id == kimlik {
                self.bildirim = nil
            }
        }
    }

    /// Android: LoginStartActivity'yi temiz görev yığınıyla açar.
    func oturumKapandi() {
        yigin = []
        cikisYapildi?()
    }

    /// Kullanılan avatar (Android: AvatarStore.loadActive; hiç kaydedilmemişse varsayılan).
    var etkinAvatar: String {
        return depo.yerelAvatar ?? AvatarOrtak.varsayilan
    }

    /// Android: AvatarCustomFragment "Kaydet" → AvatarStore.save (yerel + `users/{uid}.avatarConfig`;
    /// Firestore yazımı başarısız olsa da yerel kayıt kalır) → MainActivity.refreshProfileNavIcon →
    /// "Avatarın kaydedildi".
    func avatarKaydet(_ config: String) {
        let duz: String = AvatarOrtak.duzgun(config)
        depo.yerelAvatar = duz
        let kaynak: ProfilDeposu = depo
        Task {
            try? await kaynak.avatarKaydet(duz)
        }
        avatarSayaci += 1
        avatarDegisti?(duz)
        bildir(Metin.al("ui_avatarin_kaydedildi"))
    }
}

struct ProfilEkrani: View {
    @StateObject private var gezinme: ProfilGezinme

    /// - Parameters:
    ///   - kupaAc: Kupa yolu kartına dokunulunca (Android: CupHistoryFragment).
    ///   - rozetlerAc: Rozetler kartına ya da okuna dokunulunca (Android: BadgeDetailFragment).
    ///   - magazaAc: Android profilinde mağazaya giden bir yol yoktur; şimdilik çağrılmaz
    ///     (kabuk bağlantısı için ayrılmıştır).
    ///   - cikisYapildi: Oturum kapatılınca ya da hesap silinince (Android: LoginStartActivity).
    ///   - depo: Veri kaynağı; verilmezse örnek veriyle çalışan sahte depo kullanılır.
    ///   - avatarDegisti: Avatar kaydedilince yeni dizgeyle (Android: MainActivity.refreshProfileNavIcon —
    ///     alt çubuktaki profil sekmesi ikonu kullanıcının avatarı). Kabuk bağlamazsa nil.
    init(
        kupaAc: ((ProfilKupaSecimi) -> Void)? = nil,
        rozetlerAc: (() -> Void)? = nil,
        magazaAc: (() -> Void)? = nil,
        cikisYapildi: (() -> Void)? = nil,
        depo: ProfilDeposu? = nil,
        avatarDegisti: ((String) -> Void)? = nil
    ) {
        let kaynak: ProfilDeposu = depo ?? SahteProfilDeposu()
        let nesne = ProfilGezinme(depo: kaynak)
        nesne.kupaAc = kupaAc
        nesne.rozetlerAc = rozetlerAc
        nesne.magazaAc = magazaAc
        nesne.cikisYapildi = cikisYapildi
        nesne.avatarDegisti = avatarDegisti
        _gezinme = StateObject(wrappedValue: nesne)
    }

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            ProfilSayfasi(hedefUid: nil, gezinme: gezinme)
            ForEach(gezinme.yigin) { rota in
                ProfilRotaGorunumu(rota: rota, gezinme: gezinme)
                    .transition(.move(edge: .trailing))
                    .zIndex(Double(rota.id))
            }
            bildirimKatmani
        }
        .animation(.easeInOut(duration: 0.25), value: gezinme.yigin)
        .animation(.easeInOut(duration: 0.2), value: gezinme.bildirim)
    }

    @ViewBuilder
    private var bildirimKatmani: some View {
        if let bildirim = gezinme.bildirim {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                ProfilBildirimGorunumu(metin: bildirim.metin)
            }
            .allowsHitTesting(false)
            .transition(.opacity)
            .zIndex(100_000)
        }
    }
}

/// Yığındaki bir rotanın ekranı. Alttaki ekranlar bellekte kalır (Android geri yığını gibi).
struct ProfilRotaGorunumu: View {
    let rota: ProfilRota
    @ObservedObject var gezinme: ProfilGezinme

    var body: some View {
        icerik
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color("background_color").ignoresSafeArea())
    }

    @ViewBuilder
    private var icerik: some View {
        switch rota.hedef {
        case .kullanici(let uid):
            ProfilSayfasi(hedefUid: uid, gezinme: gezinme)
        case .takipListesi(let sekme):
            ProfilTakipListesiEkrani(baslangicSekmesi: sekme, gezinme: gezinme)
        case .arkadasAra:
            ProfilArkadasAraEkrani(gezinme: gezinme)
        case .hesapAyarlari:
            ProfilHesapAyarlariEkrani(gezinme: gezinme)
        case .sesAyarlari:
            ProfilSesAyarlariEkrani(gezinme: gezinme)
        case .profilDuzenle:
            ProfilDuzenleEkrani(gezinme: gezinme)
        case .geriBildirim:
            ProfilGeriBildirimEkrani(gezinme: gezinme)
        case .avatarDuzenle:
            avatarDuzenleme
        case .kaynaklar:
            ProfilKaynaklarEkrani(geri: { gezinme.geri() })
        }
    }

    private var avatarDuzenleme: some View {
        let nesne: ProfilGezinme = gezinme
        return AvatarDuzenleEkrani(
            baslangic: nesne.etkinAvatar,
            kaydet: { yeni in nesne.avatarKaydet(yeni) },
            kapat: { nesne.geri() }
        )
    }
}
