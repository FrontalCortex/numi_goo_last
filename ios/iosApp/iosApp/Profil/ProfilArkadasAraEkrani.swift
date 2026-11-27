import SwiftUI

// "Arkadaş ara" ekranı: ada ya da kullanıcı koduna göre önek araması, takip etme.
// Android: AddFriendFragment + FriendSearchAdapter,
// res/layout/fragment_add_friend.xml, item_friend_result.xml.
// Eşikler, sayfa boyutu ve tekilleştirme ortak koddadır (FriendSearchRules).

@MainActor
final class ProfilAramaModeli: ObservableObject {
    @Published var sorgu: String = ""
    @Published var sonuclar: [ProfilAramaSonucu] = []
    @Published var sayiMetni: String? = nil
    @Published var bulunamadi: Bool = false
    @Published var yukleniyor: Bool = false
    @Published var dahaFazlaGorunur: Bool = false
    @Published var dahaFazlaYukleniyor: Bool = false
    /// Takip düğmesine basılan satırlar (Android: requestSent — düğme soluk ve devre dışı).
    @Published var gonderilenler: Set<String> = []

    private let depo: ProfilDeposu
    private var gecikmeGorevi: Task<Void, Never>? = nil
    private var birikenler: [ProfilAramaSonucu] = []
    private var guncelSorgu: String = ""
    private var araniyor: Bool = false
    private var dahaVar: Bool = true

    init(depo: ProfilDeposu) {
        self.depo = depo
    }

    var temizleGorunur: Bool {
        return !sorgu.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// Android: afterTextChanged — önceki bekleyen arama iptal edilir, 300 ms sonra aranır.
    func sorguDegisti() {
        let kirpilmis = sorgu.trimmingCharacters(in: .whitespacesAndNewlines)
        gecikmeGorevi?.cancel()
        if ProfilOrtak.aramaTemizlenmeli(kirpilmis) {
            sonuclar = []
            sayiMetni = nil
            bulunamadi = false
            return
        }
        let gecikme = ProfilOrtak.aramaGecikmesiNs
        gecikmeGorevi = Task {
            try? await Task.sleep(nanoseconds: gecikme)
            if Task.isCancelled { return }
            await self.ara(kirpilmis)
        }
    }

    /// Android: btnClearSearch.
    func temizle() {
        gecikmeGorevi?.cancel()
        sorgu = ""
        sonuclar = []
        sayiMetni = nil
        bulunamadi = false
        dahaFazlaGorunur = false
    }

    private func ara(_ metin: String) async {
        if !ProfilOrtak.aramaYapilmali(metin) { return }
        guncelSorgu = metin
        birikenler = []
        dahaVar = true
        await aramayiYurut(metin, devam: false)
    }

    /// Android: btnLoadMore.
    func dahaFazlaYukle() {
        if !araniyor && dahaVar && !guncelSorgu.isEmpty {
            let metin = guncelSorgu
            Task { await aramayiYurut(metin, devam: true) }
        }
    }

    private func aramayiYurut(_ metin: String, devam: Bool) async {
        araniyor = true
        if devam {
            dahaFazlaYukleniyor = true
        } else {
            yukleniyor = true
            sonuclar = []
            bulunamadi = false
            sayiMetni = nil
            dahaFazlaGorunur = false
        }

        let tur = await depo.ara(sorgu: metin, sayfaBoyu: ProfilOrtak.aramaSayfaBoyu(devam: devam), devam: devam)
        // Bu arada sorgu değiştiyse sonuç atılır.
        if metin != guncelSorgu {
            araniyor = false
            return
        }
        dahaVar = ProfilOrtak.dahaFazlaVar(buTurSonucSayisi: tur.count)

        araniyor = false
        yukleniyor = false
        dahaFazlaYukleniyor = false

        birikenler.append(contentsOf: tur)
        // uid'ye göre tekilleştir; ilk görülen kalır.
        let sirali = ProfilOrtak.tekilUidler(birikenler.map { $0.uid })
        var ilkler: [String: ProfilAramaSonucu] = [:]
        for sonuc in birikenler where ilkler[sonuc.uid] == nil {
            ilkler[sonuc.uid] = sonuc
        }
        let tekil: [ProfilAramaSonucu] = sirali.compactMap { ilkler[$0] }

        if tekil.isEmpty {
            bulunamadi = true
            sonuclar = []
            sayiMetni = nil
            dahaFazlaGorunur = false
        } else {
            // Android: appPlural(R.plurals.ui_x_sonuc_gosteriliyor, n, n).
            sayiMetni = Metin.cogul("ui_x_sonuc_gosteriliyor", tekil.count, tekil.count)
            bulunamadi = false
            dahaFazlaGorunur = dahaVar
            sonuclar = tekil
        }
    }

    /// Android: FriendSearchAdapter düğmesi + AddFriendFragment.followUser.
    func takipEt(_ sonuc: ProfilAramaSonucu, bildir: @escaping (String) -> Void) {
        gonderilenler.insert(sonuc.uid)
        guard depo.oturum != nil else {
            bildir(Metin.al("ui_oturum_bilgisi_alinamadi"))
            return
        }
        Task {
            do {
                // Önce zaten takip edilip edilmediğine bakılır.
                if try await depo.takipEdiyorMuyum(hedefUid: sonuc.uid) {
                    bildir(Metin.al("ui_zaten_takip_ediyorsunuz"))
                    return
                }
            } catch {
                bildir(Metin.al("ui_hata_x", ProfilSayfaModeli.hataMetni(error)))
                return
            }
            do {
                try await depo.takipEt(hedefUid: sonuc.uid, hedefKod: sonuc.kullaniciKodu, hedefAd: sonuc.ad)
                bildir(Metin.al("ui_x_takip_edildi", sonuc.ad))
            } catch {
                bildir(Metin.al("ui_takip_edilemedi_x", ProfilSayfaModeli.hataMetni(error)))
            }
        }
    }
}

struct ProfilArkadasAraEkrani: View {
    @ObservedObject var gezinme: ProfilGezinme
    @StateObject private var model: ProfilAramaModeli

    init(gezinme: ProfilGezinme) {
        self.gezinme = gezinme
        _model = StateObject(wrappedValue: ProfilAramaModeli(depo: gezinme.depo))
    }

    var body: some View {
        VStack(spacing: 0) {
            ProfilUstCubuk(baslik: Metin.al("fragment_add_friend_text"), geri: { gezinme.geri() })
            aramaCubugu
                .padding(.horizontal, 16)
                .padding(.top, 4)
            if let sayi = model.sayiMetni {
                Text(sayi)
                    .font(ProfilStil.yazi(18))
                    .foregroundColor(Color.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 8)
            }
            govde
            if model.dahaFazlaGorunur {
                ProfilCerceveliDugme(
                    metin: model.dahaFazlaYukleniyor
                        ? Metin.al("ui_yukleniyor") : Metin.al("ui_daha_fazla_yukle"),
                    etkin: !model.dahaFazlaYukleniyor,
                    eylem: { model.dahaFazlaYukle() }
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
        }
        .onChange(of: model.sorgu) { _ in model.sorguDegisti() }
    }

    /// 52 pt yükseklik, 16 pt köşe, zemin #203137; büyüteç + metin alanı + temizle düğmesi.
    private var aramaCubugu: some View {
        HStack(spacing: 0) {
            // Android: android.R.drawable.ic_menu_search (sistem görseli; burada SF Symbols karşılığı).
            Image(systemName: "magnifyingglass")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(ProfilStil.soluk)
                .frame(width: 20, height: 20)
            TextField(
                "", text: $model.sorgu,
                prompt: Text(Metin.al("fragment_add_friend_et_search_hint")).foregroundColor(ProfilStil.aramaIpucu)
            )
                .font(ProfilStil.yazi(16))
                .foregroundColor(Color.white)
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled(true)
                .submitLabel(.search)
                .padding(.leading, 10)
            if model.temizleGorunur {
                Button(action: { model.temizle() }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(ProfilStil.soluk)
                        .frame(width: 36, height: 36)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(Metin.al("fragment_add_friend_btn_clear_search_desc")))
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 8)
        .frame(height: 52)
        .background(RoundedRectangle(cornerRadius: 16).fill(ProfilStil.girisZemini))
    }

    @ViewBuilder
    private var govde: some View {
        if model.yukleniyor {
            VStack(spacing: 0) {
                ProfilYukleniyor()
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if model.bulunamadi {
            Text(Metin.al("fragment_add_friend_text_2"))
                .font(ProfilStil.yazi(16))
                .foregroundColor(ProfilStil.soluk)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView(.vertical, showsIndicators: false) {
                LazyVStack(spacing: 0) {
                    ForEach(Array(model.sonuclar.enumerated()), id: \.element.id) { sira, sonuc in
                        ProfilAramaSatiri(
                            sonuc: sonuc,
                            sira: sira,
                            gonderildi: model.gonderilenler.contains(sonuc.uid),
                            ac: { gezinme.ac(.kullanici(sonuc.uid)) },
                            takipEt: { model.takipEt(sonuc, bildir: { metin in gezinme.bildir(metin) }) }
                        )
                    }
                }
                .padding(.bottom, 16)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

/// item_friend_result.xml: avatar 48, ad 16, kod 13, sağda 44x44 düğme; iç boşluk 16 / 12.
struct ProfilAramaSatiri: View {
    let sonuc: ProfilAramaSonucu
    let sira: Int
    let gonderildi: Bool
    let ac: () -> Void
    let takipEt: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            ProfilSatirAvatari(
                ad: sonuc.ad,
                avatarConfig: sonuc.avatarConfig,
                boyut: 48,
                harfBoyu: 20,
                renk: ProfilOrtak.aramaAvatarRengi(sira: sira)
            )
            VStack(alignment: .leading, spacing: 2) {
                Text(sonuc.ad)
                    .font(ProfilStil.yazi(16))
                    .foregroundColor(Color.white)
                    .lineLimit(1)
                Text("@" + sonuc.kullaniciKodu)
                    .font(ProfilStil.yazi(13))
                    .foregroundColor(ProfilStil.soluk)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            ProfilKisiEkleDugmesi(
                gonderildi: gonderildi,
                etkin: !gonderildi,
                opaklik: gonderildi ? 0.5 : 1.0,
                eylem: takipEt,
                erisimMetni: "item_friend_result_btn_add_friend_desc"
            )
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
        .onTapGesture(perform: ac)
    }
}
