import Combine
import Shared
import SwiftUI

// Ders liderlik tablosu (sezonluk).
//
// Android karşılığı: RecordFragment.kt + RecordLeaderboardAdapter.kt,
// res/layout/fragment_record.xml ve item_record_leaderboard_row.xml.
// Haritada bitmiş bir sandık (ünite maratonu) dersinin "rekor" alanına basınca açılır
// (LessonAdapter / MapFragment → RecordFragment.newInstance(partId, lessonIndex, title)).
// Üstte sezon etiketi ("s14") ve sezon bitişine geri sayım; birinci büyük gösterilir, kalanlar listede.
// Sıralama, renkler ve metinler ortak koddadır (RecordLogic, SeasonClock, LessonLeaderboardRepository).

/// Liderlik tablosunun bir satırı (ekran durumu).
struct KupaLiderSatirDurumu: Identifiable, Equatable {
    let kullaniciKimligi: String
    let sira: Int
    let ad: String
    let rekorEtiketi: String
    let avatarConfig: String
    let avatarVar: Bool
    /// Android drawable adı (sıra rozetinin rengi).
    let rozetAdi: String
    let kartRenkAdi: String
    let adRenkAdi: String
    let sureRenkAdi: String

    var id: String { "\(sira)-\(kullaniciKimligi)" }
}

final class KupaLiderTablosuModeli: ObservableObject {
    @Published private(set) var sezonEtiketi: String = ""
    @Published private(set) var geriSayim: String = ""
    @Published private(set) var birinci: KupaLiderSatirDurumu? = nil
    @Published private(set) var digerleri: [KupaLiderSatirDurumu] = []
    /// nil değilse liste yerine bu metin gösterilir (boş durum ya da hata).
    @Published private(set) var bosMetin: String? = nil

    private let partId: Int
    private let dersAnahtari: String
    private let depo: KupaLiderlikDeposu
    private let kopru: KupaEkranKoprusu = KupaEkranKoprusu.shared
    private var tutamak: KupaDinlemeTutamagi? = nil
    private var bagliSezon: Int = -1

    init(partId: Int, dersAnahtari: String, depo: KupaLiderlikDeposu) {
        self.partId = partId
        self.dersAnahtari = dersAnahtari
        self.depo = depo
    }

    deinit {
        tutamak?.iptalEt()
    }

    func basla() {
        if tutamak == nil {
            bagla(sezon: Int(kopru.guncelSezon()))
        }
        saniyeGecti()
    }

    func dur() {
        tutamak?.iptalEt()
        tutamak = nil
        bagliSezon = -1
    }

    /// Saniyede bir: geri sayımı yeniler; sezon değiştiyse dinleyiciyi yeni sezona bağlar
    /// (Android: countdownRunnable + SeasonClock sezon değişimi dinleyicisi).
    func saniyeGecti() {
        let v: SezonVerisi = kopru.sezonVerisi(boundSeason: Int32(bagliSezon))
        geriSayim = v.countdownText
        sezonEtiketi = v.seasonLabel
        let guncel: Int = Int(v.season)
        if tutamak != nil && guncel != bagliSezon {
            bagla(sezon: guncel)
            sezonEtiketi = kopru.sezonVerisi(boundSeason: Int32(bagliSezon)).seasonLabel
        }
    }

    private func bagla(sezon: Int) {
        tutamak?.iptalEt()
        bagliSezon = sezon
        tutamak = depo.dinle(
            partId: partId,
            dersAnahtari: dersAnahtari,
            sezon: sezon,
            guncelle: { [weak self] kayitlar in
                self?.kayitlarGeldi(kayitlar)
            },
            hata: { [weak self] ileti, yetkiHatasi in
                self?.hataGeldi(ileti: ileti, yetkiHatasi: yetkiHatasi)
            }
        )
    }

    private func kayitlarGeldi(_ kayitlar: [KupaLiderKaydi]) {
        let ham: [LiderHamKayit] = kayitlar.map {
            LiderHamKayit(
                userId: $0.kullaniciKimligi,
                recordScore: Int32($0.puan),
                displayName: $0.ad,
                recordLabel: $0.rekorEtiketi,
                avatarConfig: $0.avatarConfig
            )
        }
        let satirlar: [LiderSatirVerisi] = kopru.liderSatirlari(kayitlar: ham, currentUid: depo.oturumKimligi)
        let durumlar: [KupaLiderSatirDurumu] = satirlar.map { s in
            KupaLiderSatirDurumu(
                kullaniciKimligi: s.userId,
                sira: Int(s.displayRank),
                ad: s.displayName,
                rekorEtiketi: s.recordLabel,
                avatarConfig: s.avatarConfig,
                avatarVar: s.hasAvatar,
                rozetAdi: s.rankBadgeDrawable,
                kartRenkAdi: s.cardColorRes,
                adRenkAdi: s.nameColorRes,
                sureRenkAdi: s.timeColorRes
            )
        }
        if durumlar.isEmpty {
            birinci = nil
            digerleri = []
            bosMetin = Metin.al("record_leaderboard_empty")
            return
        }
        bosMetin = nil
        birinci = durumlar.first
        digerleri = Array(durumlar.dropFirst())
    }

    private func hataGeldi(ileti: String, yetkiHatasi: Bool) {
        birinci = nil
        digerleri = []
        bosMetin = kopru.liderlikHataMetni(yerelIleti: ileti, yetkiHatasi: yetkiHatasi)
    }
}

struct KupaLiderTablosuEkrani: View {
    private let dersBasligi: String
    private let kapat: (() -> Void)?

    @StateObject private var model: KupaLiderTablosuModeli

    private let saniyeSayaci = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    /// - Parameters:
    ///   - partId: dersin part'ı (GlobalLessonData.globalPartId).
    ///   - dersAnahtari: dersin kalıcı kimliği (LessonItem.stableId); liste konumu değil.
    ///   - dersBasligi: başlığın altında gösterilen ders adı.
    ///   - kapat: geri düğmesi.
    ///   - ortam: veri servisleri (varsayılan: örnek veri).
    init(
        partId: Int,
        dersAnahtari: String,
        dersBasligi: String,
        kapat: (() -> Void)?,
        ortam: KupaOrtami = KupaOrtami.sahte
    ) {
        self.dersBasligi = dersBasligi
        self.kapat = kapat
        _model = StateObject(
            wrappedValue: KupaLiderTablosuModeli(partId: partId, dersAnahtari: dersAnahtari, depo: ortam.liderlikDeposu)
        )
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            susDaireleri
            icerik
        }
        .onAppear {
            model.basla()
        }
        .onDisappear {
            model.dur()
        }
        .onReceive(saniyeSayaci) { _ in
            model.saniyeGecti()
        }
    }

    /// fragment_record.xml: sağ üstte 120dp, sol altta 80dp yarı saydam daire (bg_record_decor_circle).
    ///
    /// Bu daireler Android'de de vardır ve içeriğin ARKASINDA durur (FrameLayout'un ilk iki çocuğu):
    /// - sağ üst: 120dp, layout_gravity top|end, marginTop -20dp, marginEnd -30dp
    /// - sol alt: 80dp, layout_gravity bottom|start, marginStart -24dp, marginBottom 80dp
    /// - dolgu: #33FFFFFF (beyaz, %20) oval
    /// Kök görünüm fitsSystemWindows olduğu için konumlar güvenli alana göredir.
    private var susDaireleri: some View {
        GeometryReader { geo in
            Circle()
                .fill(Color.white.opacity(0.2))
                .frame(width: 120, height: 120)
                .position(x: geo.size.width + 30 - 60, y: -20 + 60)
            Circle()
                .fill(Color.white.opacity(0.2))
                .frame(width: 80, height: 80)
                .position(x: -24 + 40, y: geo.size.height - 80 - 40)
        }
        .allowsHitTesting(false)
    }

    private var icerik: some View {
        VStack(spacing: 0) {
            ustCubuk
            Text(dersBasligi)
                .font(Tema.yazi(14))
                .foregroundColor(Color.white.opacity(0.8))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity)
            if let birinci = model.birinci {
                KupaLiderBirinciGorunumu(satir: birinci)
            }
            if !model.digerleri.isEmpty {
                Text(Metin.al("record_leaderboard_rank_list"))
                    .font(Tema.yazi(13))
                    .foregroundColor(Color.white.opacity(0.6))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 8)
                    // recordRankSectionLabel: android:background="@color/black" (süs dairesini örter).
                    .background(Color.black)
            }
            altBolum
        }
    }

    private var ustCubuk: some View {
        HStack(spacing: 0) {
            KupaGeriDugmesi(boyut: 44, icBosluk: 8, renk: .white, eylem: { kapat?() })
            Text(Metin.al("record_leaderboard_title"))
                .font(Tema.yazi(18))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.trailing, 8)
            Text(model.sezonEtiketi)
                .font(.system(size: 14, design: .monospaced))
                .foregroundColor(Color.white.opacity(0.7))
                .padding(.trailing, 8)
            Text(model.geriSayim)
                .font(.system(size: 14, design: .monospaced))
                .foregroundColor(Color.white.opacity(0.7))
                .frame(minWidth: 52, alignment: .trailing)
                .padding(.trailing, 4)
        }
        .padding(.leading, 8)
        .padding(.trailing, 12)
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private var altBolum: some View {
        if let metin = model.bosMetin {
            Text(metin)
                .font(Tema.yazi(15))
                .foregroundColor(Color.white.opacity(0x88 / 255.0))
                .multilineTextAlignment(.center)
                .padding(32)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView(.vertical, showsIndicators: false) {
                LazyVStack(spacing: 8) {
                    ForEach(model.digerleri) { satir in
                        KupaLiderSatirGorunumu(satir: satir)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 24)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

// MARK: - Parçalar

/// Satır avatarı. Android avatarı `AvatarConfig` ile kodla çizer (leaderboardAvatarBitmap); iOS'ta liderlik
/// satırı için avatar çizici henüz yok, bu yüzden `avatarConfig` taşınır ama şimdilik varsayılan simge
/// ("user" görseli; boş avatar için Android de varsayılan avatarı çizer) gösterilir.
private struct KupaLiderFotografi: View {
    let avatarConfig: String
    let avatarVar: Bool
    let doldur: Bool

    var body: some View {
        varsayilan
    }

    private var varsayilan: some View {
        Image("user")
            .resizable()
            .scaledToFit()
    }
}

/// Birinci: 120dp yuvarlak fotoğraf (3dp #FFE082 çerçeve), altında "1" rozeti, ad ve süre hapı.
private struct KupaLiderBirinciGorunumu: View {
    let satir: KupaLiderSatirDurumu

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .bottom) {
                KupaLiderFotografi(avatarConfig: satir.avatarConfig, avatarVar: satir.avatarVar, doldur: false)
                    .padding(4)
                    .frame(width: 120, height: 120)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(KupaStil.renk(0xFFE082), lineWidth: 3))
                Text("1")
                    .font(Tema.yazi(14))
                    .foregroundColor(.black)
                    .frame(width: 28, height: 28)
                    .background(Circle().fill(KupaStil.siraRozetiRengi("bg_record_rank_gold")))
                    .offset(y: 4)
            }
            Text(satir.ad)
                .font(Tema.yazi(20))
                .foregroundColor(.white)
                .padding(.top, 12)
            Text(satir.rekorEtiketi)
                .font(Tema.yazi(14))
                .foregroundColor(KupaStil.renk(0x1A1A1A))
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
                .background(RoundedRectangle(cornerRadius: 24).fill(KupaStil.renk(0xB8E986)))
                .padding(.top, 10)
        }
        .padding(.top, 16)
        .padding(.bottom, 8)
        .frame(maxWidth: .infinity)
    }
}

/// item_record_leaderboard_row.xml: fotoğraf, ad + süre, sağda sıra rozeti.
/// Oturum açmış kullanıcının satırı beyaz kart üzerinde siyah/gri yazıdır.
private struct KupaLiderSatirGorunumu: View {
    let satir: KupaLiderSatirDurumu

    var body: some View {
        HStack(spacing: 12) {
            KupaLiderFotografi(avatarConfig: satir.avatarConfig, avatarVar: satir.avatarVar, doldur: true)
                .frame(width: 44, height: 44)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(satir.ad)
                    .font(Tema.yazi(15))
                    .foregroundColor(Color(satir.adRenkAdi))
                    .lineLimit(1)
                Text(satir.rekorEtiketi)
                    .font(Tema.yazi(13))
                    .foregroundColor(Color(satir.sureRenkAdi))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(String(satir.sira))
                .font(Tema.yazi(15))
                .foregroundColor(.black)
                .frame(width: 36, height: 36)
                .background(Circle().fill(KupaStil.siraRozetiRengi(satir.rozetAdi)))
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color(satir.kartRenkAdi)))
    }
}
