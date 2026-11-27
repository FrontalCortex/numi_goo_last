import SwiftUI

// "Arkadaşlar" ekranı: TAKİP EDİLEN / TAKİPÇİ sekmeleri.
// Android: FollowersFollowingFragment + FollowUserAdapter,
// res/layout/fragment_followers_following.xml, item_follow_user.xml.
//
// Güncel Android: satır avatarı `publicProfiles/{uid}.avatarConfig`'ten çizilir (yoksa harfli daire);
// "Takip Edilen" sekmesinde her satırda X (takibi bırak, önce onay), "Takipçi" sekmesinde henüz geri takip
// edilmeyenlerde "+" (geri takip et).

/// Listedeki bir satır (Android: FollowUser).
struct ProfilTakipSatiri: Identifiable, Equatable {
    let kayit: ProfilTakipKaydi
    /// `publicProfiles/{uid}.avatarConfig`; nil → harfli daire.
    let avatarConfig: String?
    /// Takipçi sekmesinde, henüz geri takip etmediklerimizde "+" düğmesi görünür.
    let dugmeGorunur: Bool
    /// Yalnızca "Takip Edilen" sekmesinde: X (takibi bırak).
    let birakmaGorunur: Bool
    var id: String { kayit.uid }
}

@MainActor
final class ProfilTakipModeli: ObservableObject {
    @Published var sekme: Int
    @Published var yukleniyor: Bool = false
    @Published var satirlar: [ProfilTakipSatiri] = []
    @Published var bosMetin: String? = nil
    /// "+" düğmesine basılıp sonucu beklenen satırlar (Android: düğme devre dışı, alfa 0,4).
    @Published var bekleyenler: Set<String> = []
    /// Takibi bırakma onayı beklenen satır (Android: confirmUnfollow diyaloğu).
    @Published var birakilacak: ProfilTakipSatiri? = nil

    private let depo: ProfilDeposu
    private var takipEttiklerim: Set<String> = []
    private var basladi: Bool = false
    private var yuklemeSayaci: Int = 0

    /// FollowRules.TAB_FOLLOWING (0) / TAB_FOLLOWERS (1).
    static let takipEdilenSekmesi: Int = 0

    init(depo: ProfilDeposu, baslangicSekmesi: Int) {
        self.depo = depo
        self.sekme = baslangicSekmesi
    }

    var birakmaOnayiAcik: Bool {
        get { birakilacak != nil }
        set { if !newValue { birakilacak = nil } }
    }

    /// Android: önce takip ettiklerimizin kümesi okunur ("+" düğmesi için), sonra sekme yüklenir.
    func basla() {
        if basladi { return }
        basladi = true
        Task {
            self.takipEttiklerim = await depo.takipEttiklerim()
            await self.sekmeyiYukle()
        }
    }

    func sekmeSec(_ yeni: Int) {
        if yeni == sekme { return }
        sekme = yeni
        Task { await sekmeyiYukle() }
    }

    func sekmeyiYukle(bildir: ((String) -> Void)? = nil) async {
        guard depo.oturum != nil else { return }
        yuklemeSayaci += 1
        let buYukleme = yuklemeSayaci
        let aktifSekme = sekme
        yukleniyor = true
        satirlar = []
        bosMetin = nil

        let kayitlar: [ProfilTakipKaydi]
        do {
            kayitlar = try await depo.takipListesi(sekme: aktifSekme)
        } catch {
            if buYukleme != yuklemeSayaci { return }
            yukleniyor = false
            bildir?(Metin.al("ui_yuklenemedi_x", ProfilSayfaModeli.hataMetni(error)))
            return
        }
        if buYukleme != yuklemeSayaci { return }
        if kayitlar.isEmpty {
            yukleniyor = false
            bosMetin = ProfilTakipModeli.bosMetni(aktifSekme)
            return
        }

        // Avatarlar herkese açık profilden okunur (`publicProfiles/{uid}.avatarConfig`).
        let avatarlar: [String: String]
        do {
            avatarlar = try await depo.acikAvatarlar(uidler: kayitlar.map { $0.uid })
        } catch {
            if buYukleme != yuklemeSayaci { return }
            yukleniyor = false
            bildir?(Metin.al("ui_avatar_yuklenemedi_x", ProfilSayfaModeli.hataMetni(error)))
            return
        }
        if buYukleme != yuklemeSayaci { return }
        yukleniyor = false
        let takipEdilen: Bool = aktifSekme == ProfilTakipModeli.takipEdilenSekmesi
        satirlar = kayitlar.map { kayit in
            ProfilTakipSatiri(
                kayit: kayit,
                avatarConfig: avatarlar[kayit.uid],
                dugmeGorunur: ProfilOrtak.geriTakipDugmesiGorunur(
                    sekme: aktifSekme, zatenTakip: takipEttiklerim.contains(kayit.uid)
                ),
                birakmaGorunur: takipEdilen
            )
        }
    }

    /// Android: "Henüz kimseyi takip etmiyorsun" / "Henüz takipçin yok".
    static func bosMetni(_ sekme: Int) -> String {
        if sekme == ProfilTakipModeli.takipEdilenSekmesi {
            return Metin.al("ui_henuz_kimseyi_takip_etmiyorsun")
        }
        return Metin.al("ui_henuz_takipcin_yok")
    }

    /// Android: followUser — başarılıysa küme güncellenir ve liste yeniden yüklenir ("+" kaybolur).
    /// Güncel Android'de yazım AddFriendFragment ile aynıdır (benim satırım hedefin followers'ına,
    /// hedefin satırı benim following'ime); eski çapraz yazım düzeltildi.
    func geriTakipEt(_ satir: ProfilTakipSatiri, bildir: @escaping (String) -> Void) {
        guard depo.oturum != nil else { return }
        bekleyenler.insert(satir.kayit.uid)
        Task {
            do {
                try await depo.takipEt(
                    hedefUid: satir.kayit.uid, hedefKod: satir.kayit.kullaniciKodu, hedefAd: satir.kayit.ad
                )
                self.takipEttiklerim.insert(satir.kayit.uid)
                self.bekleyenler.remove(satir.kayit.uid)
                bildir(Metin.al("ui_x_takip_edildi_2", satir.kayit.ad))
                await self.sekmeyiYukle(bildir: bildir)
            } catch {
                // Android'de hata sonrası düğme devre dışı kalır; burada yeniden denenebilsin diye açılır.
                self.bekleyenler.remove(satir.kayit.uid)
                bildir(Metin.al("ui_hata_x", ProfilSayfaModeli.hataMetni(error)))
            }
        }
    }

    /// X düğmesi: önce onay (Android: confirmUnfollow). Düğme kapatılmaz; diyalog zaten modal.
    func birakmayiSor(_ satir: ProfilTakipSatiri) {
        birakilacak = satir
    }

    /// Android: unfollowUser — iki takip kaydı silinir; liste tazelenir (satır "Takip Edilen"den düşer).
    func takibiBirak(bildir: @escaping (String) -> Void) {
        guard let satir = birakilacak else { return }
        birakilacak = nil
        guard depo.oturum != nil else { return }
        Task {
            do {
                try await depo.takibiBirak(hedefUid: satir.kayit.uid)
                self.takipEttiklerim.remove(satir.kayit.uid)
                bildir(Metin.al("ui_x_takipten_cikarildi", satir.kayit.ad))
                await self.sekmeyiYukle(bildir: bildir)
            } catch {
                bildir(Metin.al("ui_hata_x", ProfilSayfaModeli.hataMetni(error)))
            }
        }
    }
}

struct ProfilTakipListesiEkrani: View {
    @ObservedObject var gezinme: ProfilGezinme
    @StateObject private var model: ProfilTakipModeli

    init(baslangicSekmesi: Int, gezinme: ProfilGezinme) {
        self.gezinme = gezinme
        _model = StateObject(
            wrappedValue: ProfilTakipModeli(depo: gezinme.depo, baslangicSekmesi: baslangicSekmesi)
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            ProfilUstCubuk(baslik: Metin.al("fragment_followers_following_text"), geri: { gezinme.geri() })
            ProfilSekmeCubugu(
                basliklar: [Metin.al("ui_takip_edilen"), Metin.al("ui_takipci")],
                secili: model.sekme,
                sec: { yeni in model.sekmeSec(yeni) }
            )
            ProfilStil.sekmeAyirici.frame(height: 1)
            if model.yukleniyor {
                ProfilYukleniyor()
            }
            govde
        }
        .onAppear { model.basla() }
        .alert(Text(""), isPresented: $model.birakmaOnayiAcik) {
            Button(Metin.al("ui_takibi_birak"), role: .destructive) {
                model.takibiBirak(bildir: { metin in gezinme.bildir(metin) })
            }
            Button(Metin.al("ui_iptal_2"), role: .cancel) {}
        } message: {
            Text(ProfilOrtak.takipBirakmaSorusu(ad: model.birakilacak?.kayit.ad ?? ""))
        }
    }

    @ViewBuilder
    private var govde: some View {
        if let bos = model.bosMetin {
            Text(bos)
                .font(ProfilStil.yazi(16))
                .foregroundColor(ProfilStil.soluk)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView(.vertical, showsIndicators: false) {
                LazyVStack(spacing: 0) {
                    ForEach(Array(model.satirlar.enumerated()), id: \.element.id) { sira, satir in
                        ProfilTakipSatirGorunumu(
                            satir: satir,
                            sira: sira,
                            bekliyor: model.bekleyenler.contains(satir.kayit.uid),
                            ac: { gezinme.ac(.kullanici(satir.kayit.uid)) },
                            takipEt: { model.geriTakipEt(satir, bildir: { metin in gezinme.bildir(metin) }) },
                            birak: { model.birakmayiSor(satir) }
                        )
                    }
                }
                .padding(.top, 8)
                .padding(.bottom, 16)
            }
        }
    }
}

/// item_follow_user.xml: avatar 56, ad 16, kod 13 (üstten 2), sağda 44x44 "+" ya da X; iç boşluk 16 / 14.
struct ProfilTakipSatirGorunumu: View {
    let satir: ProfilTakipSatiri
    let sira: Int
    let bekliyor: Bool
    let ac: () -> Void
    let takipEt: () -> Void
    let birak: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ProfilSatirAvatari(
                ad: satir.kayit.ad,
                avatarConfig: satir.avatarConfig,
                boyut: 56,
                harfBoyu: 22,
                renk: ProfilOrtak.takipAvatarRengi(sira: sira)
            )
            VStack(alignment: .leading, spacing: 2) {
                Text(satir.kayit.ad)
                    .font(ProfilStil.yazi(16))
                    .foregroundColor(Color.white)
                    .lineLimit(1)
                Text("@" + satir.kayit.kullaniciKodu)
                    .font(ProfilStil.yazi(13))
                    .foregroundColor(ProfilStil.soluk)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if satir.dugmeGorunur {
                ProfilKisiEkleDugmesi(
                    etkin: !bekliyor,
                    opaklik: bekliyor ? 0.4 : 1.0,
                    eylem: takipEt
                )
            }
            if satir.birakmaGorunur {
                ProfilTakibiBirakDugmesi(eylem: birak)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
        .onTapGesture(perform: ac)
    }
}

/// TabLayout karşılığı: 48 pt yükseklik, seçili sekme #1CB0F6 ve altında metin genişliğinde çizgi
/// (tabIndicatorFullWidth=false), diğerleri #93A5B3.
struct ProfilSekmeCubugu: View {
    let basliklar: [String]
    let secili: Int
    let sec: (Int) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<basliklar.count, id: \.self) { sira in
                Button(action: { sec(sira) }) {
                    Text(basliklar[sira])
                        .font(ProfilStil.yazi(14))
                        .foregroundColor(sira == secili ? ProfilStil.mavi : ProfilStil.soluk)
                        .frame(height: 46)
                        .overlay(
                            Rectangle()
                                .fill(sira == secili ? ProfilStil.mavi : Color.clear)
                                .frame(height: 2)
                                .offset(y: 1),
                            alignment: .bottom
                        )
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .background(Color("background_color"))
    }
}
