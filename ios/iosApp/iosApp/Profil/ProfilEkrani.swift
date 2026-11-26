import SwiftUI

// Profil sekmesinin giriş noktası.
//
// Android'deki akış: ProfileFragment (alt çubuğun 2. sekmesi) → aynı kapsayıcıda `replace` +
// `addToBackStack` ile açılan alt ekranlar:
//   ARKADAŞ EKLE → AddFriendFragment → (satıra dokununca) ProfileFragment.newInstance(uid)
//   Takip edilen / Takipçi → FollowersFollowingFragment → ProfileFragment.newInstance(uid)
//   Ayarlar → AccountSettingsFragment → EditProfileFragment (hesap silme burada) / FeedbackFragment
//   Avatar → AvatarPickerFragment (alttan açılan sayfa)
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
    case profilDuzenle
    case geriBildirim
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

    let depo: ProfilDeposu
    var kupaAc: ((ProfilKupaSecimi) -> Void)?
    var rozetlerAc: (() -> Void)?
    var magazaAc: (() -> Void)?
    var cikisYapildi: (() -> Void)?

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
    init(
        kupaAc: ((ProfilKupaSecimi) -> Void)? = nil,
        rozetlerAc: (() -> Void)? = nil,
        magazaAc: (() -> Void)? = nil,
        cikisYapildi: (() -> Void)? = nil,
        depo: ProfilDeposu? = nil
    ) {
        let kaynak: ProfilDeposu = depo ?? SahteProfilDeposu()
        let nesne = ProfilGezinme(depo: kaynak)
        nesne.kupaAc = kupaAc
        nesne.rozetlerAc = rozetlerAc
        nesne.magazaAc = magazaAc
        nesne.cikisYapildi = cikisYapildi
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
        case .profilDuzenle:
            ProfilDuzenleEkrani(gezinme: gezinme)
        case .geriBildirim:
            ProfilGeriBildirimEkrani(gezinme: gezinme)
        }
    }
}
