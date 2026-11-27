import Shared
import SwiftUI
import UIKit

// Tek rozet ekranı.
//
// Android karşılığı: BadgeFragment.kt + res/layout/fragment_badge.xml — "Başarılar" ekranında bir
// rozete basınca açılan görünüm (BadgeFragment.newInstance(mode, unlocked)): büyük rozet, üzerinde
// sayı, altında açıklama, kademeli rozetlerde ilerleme çubuğu; madalya ve kupada "Nx" düğmesiyle
// açılan ünite/sezon listesi.
// Kutlama akışı (seviye atlama animasyonu, kutlama kuyruğu, ses) bu ekranda YOKTUR; bkz. rapor.
// Açık/kilitli durumu, sayı, açıklama, ilerleme ve parça satırları ortak koddan gelir
// (KupaEkranKoprusu.rozetEkrani → BadgeFragmentLogic, BadgeProgressRepository).
//
// Yerleşim (fragment_badge.xml): 560dp'lik badgeAnimContainer ekranın ortasından 75dp yukarıdadır
// (layout_marginBottom=150dp). Taban Lottie'si kabın tamamını (560dp) kaplar ve ekrandan taşar —
// rozet şekli Lottie tuvalinin yalnızca ortasındaki ~%43'tür. Üst Lottie 280dp'dir ve ortadadır
// (translationY −5dp). Sayı, kabın altına 20dp mesafeli 220dp'lik yuvanın ortasındadır (kap
// merkezinin 150dp altı); açıklama yuvanın altından 40dp yukarıda başlar (merkezin 220dp altı).

/// Madalya/kupa parça satırı.
struct RozetParcaDurumu: Identifiable, Equatable {
    let sira: Int
    let unite: String
    /// Kupa satırında sıra; madalyada boş.
    let derece: String
    let sezon: String

    var id: Int { sira }
}

/// Tek rozet ekranının durumu.
struct RozetDetayDurumu: Equatable {
    let modAdi: String
    let acik: Bool
    let sayiMetni: String
    let sayiGorunur: Bool
    let aciklama: String
    let ton: String
    let tabanDosyasi: String
    let ustDosyasi: String
    let ustOlcek: CGFloat
    /// Üst animasyonun durduğu kare (Android holdFrame): 0 = ilk kare, 9999 = son kare.
    let ustTutmaKaresi: CGFloat
    let ilerlemeGorunur: Bool
    let ilerlemeMetni: String
    /// 0..100
    let ilerlemeYuzdesi: Int
    /// "3x"; boş = düğme yok.
    let parcaSayisiMetni: String
    let parcaKupaMi: Bool
    let parcalar: [RozetParcaDurumu]
    let parcaDosyasi: String
    let dokununcaOynat: Bool
}

final class RozetDetayModeli: ObservableObject {
    @Published private(set) var durum: RozetDetayDurumu? = nil
    @Published var parcaPaneliAcik: Bool = false

    private let modAdi: String
    private let depo: KupaRozetDeposu
    private let kopru: KupaEkranKoprusu = KupaEkranKoprusu.shared

    init(modAdi: String, depo: KupaRozetDeposu) {
        self.modAdi = modAdi
        self.depo = depo
    }

    /// Önce bellekteki rozet durumuyla çizer, sonra dokümanı okuyup yeniler.
    func yukle() {
        durumuKur()
        depo.rozetDurumunuGetir { [weak self] dokuman in
            guard let self = self else { return }
            self.kopru.rozetDurumunuYukle(doc: dokuman)
            self.durumuKur()
        }
    }

    private func durumuKur() {
        let v: RozetEkranVerisi = kopru.rozetEkrani(modeName: modAdi)
        var parcalar: [RozetParcaDurumu] = []
        for (sira, p) in v.pieceRows.enumerated() {
            parcalar.append(RozetParcaDurumu(sira: sira, unite: p.titleUnit, derece: p.rankText, sezon: p.seasonText))
        }
        durum = RozetDetayDurumu(
            modAdi: v.modeName,
            acik: v.unlocked,
            sayiMetni: v.rewardAmountText,
            sayiGorunur: v.showRewardAmount,
            aciklama: v.detailText,
            ton: v.toneName,
            tabanDosyasi: v.baseAsset,
            ustDosyasi: v.topAsset,
            ustOlcek: CGFloat(v.topScale),
            ustTutmaKaresi: CGFloat(v.topHoldFrame),
            ilerlemeGorunur: v.showProgress,
            ilerlemeMetni: v.progressText,
            ilerlemeYuzdesi: Int(v.progressPercent),
            parcaSayisiMetni: v.pieceCountText,
            parcaKupaMi: v.pieceIsCup,
            parcalar: parcalar,
            parcaDosyasi: v.pieceAsset,
            dokununcaOynat: v.replayOnTap
        )
        if parcalar.isEmpty {
            parcaPaneliAcik = false
        }
    }
}

/// fragment_badge.xml ölçüleri ve BadgeFragment'ın moda göre uyguladığı kaymalar.
enum RozetDetayOlculeri {
    /// badgeAnimContainer / dailyTasksCompliteBadge kenarı.
    static let tabanKenari: CGFloat = 560
    /// rocketBadgeAnim kenarı (ölçeklenmeden önce).
    static let ustKenari: CGFloat = 280
    /// Kap merkezinin ekran merkezine göre dikey konumu (layout_marginBottom=150dp → −75dp).
    static let merkezKaymasi: CGFloat = -75
    /// rocketBadgeAnim.translationY = −5dp.
    static let ustKaymasi: CGFloat = -5
    /// Sayının kap merkezine uzaklığı: 280 − 20 (yuva alt boşluğu) − 110 (220dp'lik yuvanın yarısı).
    static let sayiUzakligi: CGFloat = 150
    /// Açıklamanın üst kenarının kap merkezine uzaklığı: 280 − 20 − 40 (layout_marginTop=−40dp).
    static let aciklamaUzakligi: CGFloat = 220

    /// Sayı, açıklama, "Nx" düğmesi ve parça panelinin moda özgü dikey kayması (yalnızca açık rozette):
    /// kupa +50dp (SINGLE_TOP_LOTTIE_REWARD_SLOT_EXTRA_DOWN_DP), siyah kuşak −20dp
    /// (KARATE_REWARD_SLOT_OFFSET_UP_DP). Kilitli görünümde (showLockedStaticBadge) kayma yoktur.
    static func ekKayma(_ durum: RozetDetayDurumu) -> CGFloat {
        if !durum.acik { return 0 }
        if durum.modAdi == "CUP_GOOGLE" { return 50 }
        if durum.modAdi == "KARATE" { return -20 }
        return 0
    }

    /// Madalyalarda üst animasyon sürekli döner (Android playRocketSingleLottieLoop).
    static func madalyaMi(_ durum: RozetDetayDurumu) -> Bool {
        return durum.modAdi == "GOLD_MEDAL" || durum.modAdi == "SILVER_MEDAL" || durum.modAdi == "BRONZE_MEDAL"
    }

    /// Tabanlı açık rozetlerde (roket dışında) üst animasyon ilk gösterimde 1 sn boyunca sıfırdan
    /// büyür, sonra oynar (Android startDelayedTopLoopCycle, growTopScaleOverDelay). Dokununca
    /// yeniden oynatmada büyüme yoktur (replayTopAnimationOnly → startTopOnlyCycle).
    static func buyuyerekGelirMi(_ durum: RozetDetayDurumu, tekrar: Int) -> Bool {
        if !durum.acik || tekrar > 0 { return false }
        if durum.tabanDosyasi.isEmpty { return false }
        return durum.modAdi != "ROCKET_WITH_BASE"
    }
}

struct RozetDetayEkrani: View {
    private let kapat: (() -> Void)?

    @StateObject private var model: RozetDetayModeli
    /// Dokununca artar; üst animasyon baştan kurulup yeniden oynar (Android replayTopAnimationOnly).
    @State private var tekrar: Int = 0

    /// - Parameters:
    ///   - modAdi: ortak kod `BadgeAnimMode` adı (ör. "DINO"); `RozetSecimi.modAdi`.
    ///   - kapat: sol üstteki kapat düğmesi.
    ///   - ortam: veri servisleri (varsayılan: örnek veri).
    init(
        modAdi: String,
        kapat: (() -> Void)?,
        ortam: KupaOrtami = KupaOrtami.sahte
    ) {
        self.kapat = kapat
        _model = StateObject(wrappedValue: RozetDetayModeli(modAdi: modAdi, depo: ortam.rozetDeposu))
    }

    var body: some View {
        ZStack {
            Color("background_color")
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture {
                    ekranaDokunuldu()
                }
            if let durum = model.durum {
                RozetDetayOrtasi(durum: durum, tekrar: tekrar)
                    .allowsHitTesting(false)
                altKatman(durum)
            }
            kapatDugmesi
        }
        .onAppear {
            model.yukle()
        }
    }

    private var kapatDugmesi: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Button(action: { kapat?() }) {
                    Image(systemName: "xmark")
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(.white)
                        .padding(14)
                        .frame(width: 48, height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .padding(10)
                Spacer(minLength: 0)
            }
            Spacer(minLength: 0)
        }
    }

    /// Alt bölüm: ilerleme çubuğu, parça listesi ve "Nx" düğmesi.
    private func altKatman(_ durum: RozetDetayDurumu) -> some View {
        let ekKayma: CGFloat = RozetDetayOlculeri.ekKayma(durum)
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            if model.parcaPaneliAcik && !durum.parcalar.isEmpty {
                RozetParcaPaneli(durum: durum)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
                    .offset(y: ekKayma)
            }
            if !durum.parcaSayisiMetni.isEmpty {
                // medalPieceNamesList: marginStart=20dp, başı ve sonu ekrana bağlı → kalan alanda ortalı.
                parcaDugmesi(durum)
                    .frame(maxWidth: .infinity)
                    .padding(.leading, 20)
                    .padding(.bottom, 100)
                    .offset(y: ekKayma)
            }
            if durum.ilerlemeGorunur {
                RozetIlerlemeCubugu(metin: durum.ilerlemeMetni, yuzde: durum.ilerlemeYuzdesi)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 80)
            }
        }
    }

    private func parcaDugmesi(_ durum: RozetDetayDurumu) -> some View {
        Button(action: {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            model.parcaPaneliAcik.toggle()
        }) {
            Text(durum.parcaSayisiMetni)
                .font(Tema.yazi(22))
                .foregroundColor(KupaStil.renk(0xB8D4FF))
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(minHeight: 48)
                .background(RoundedRectangle(cornerRadius: 12).fill(Color("background_color")))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /// Android: panel açıksa dışına dokununca kapanır; açık rozette dokunuş üst animasyonu yeniden
    /// oynatır (replayTopAnimationOnly önce paneli kapatır).
    private func ekranaDokunuldu() {
        if model.parcaPaneliAcik {
            model.parcaPaneliAcik = false
        }
        guard let durum = model.durum, durum.dokununcaOynat else { return }
        tekrar += 1
    }
}

// MARK: - Rozet (taban, üst animasyon, sayı, açıklama)

/// Ekranın ortasındaki rozet. Her parça 1×1'lik bir çapaya bağlanır ve oradan taşar; böylece
/// 560dp'lik taban ekrandan geniş olsa da yerleşimi bozmaz.
private struct RozetDetayOrtasi: View {
    let durum: RozetDetayDurumu
    let tekrar: Int

    private var ekKayma: CGFloat { RozetDetayOlculeri.ekKayma(durum) }

    var body: some View {
        ZStack {
            taban
            ust
            sayi
            aciklama
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// dailyTasksCompliteBadge: açık rozette bir kez oynar ve son karede kalır, kademe tonuna
    /// boyanır; kilitli rozette son karede sabittir ve düz gridir (badge_locked_base). Tabansız
    /// modlarda (madalya, kupa, siyah kuşak) Android alpha=0 yapar; burada hiç çizilmez.
    @ViewBuilder
    private var taban: some View {
        if !durum.tabanDosyasi.isEmpty {
            KupaAnimasyonGorunumu(
                dosya: durum.tabanDosyasi,
                duzRenk: durum.acik ? nil : Color("badge_locked_base"),
                sabitKare: durum.acik ? nil : KupaStil.sonKare
            )
            .modifier(KupaRozetTonu(ton: durum.acik ? durum.ton : ""))
            .frame(width: RozetDetayOlculeri.tabanKenari, height: RozetDetayOlculeri.tabanKenari)
            .frame(width: 1, height: 1)
            .offset(y: RozetDetayOlculeri.merkezKaymasi)
        }
    }

    private var ust: some View {
        let kayma: CGFloat = RozetDetayOlculeri.merkezKaymasi + RozetDetayOlculeri.ustKaymasi
        return RozetDetayUstAnimasyonu(durum: durum, tekrar: tekrar)
            .id("\(tekrar)-\(durum.acik)")
            .frame(width: 1, height: 1)
            .offset(y: kayma)
    }

    @ViewBuilder
    private var sayi: some View {
        if durum.sayiGorunur {
            let kayma: CGFloat = RozetDetayOlculeri.merkezKaymasi + RozetDetayOlculeri.sayiUzakligi + ekKayma
            RozetDetaySayisi(metin: durum.sayiMetni, acik: durum.acik)
                .id(durum.acik)
                .frame(width: 1, height: 1)
                .offset(y: kayma)
        }
    }

    /// badgeRewardDetailText: 350dp genişlik, ortalı, en çok 4 satır, 20sp, #E6FFFFFF.
    private var aciklama: some View {
        let kayma: CGFloat = RozetDetayOlculeri.merkezKaymasi + RozetDetayOlculeri.aciklamaUzakligi + ekKayma
        return Color.clear
            .frame(maxWidth: 350)
            .frame(height: 1)
            .overlay(alignment: .top) {
                Text(durum.aciklama)
                    .font(Tema.yazi(20))
                    .foregroundColor(Color.white.opacity(0.9))
                    .multilineTextAlignment(.center)
                    .lineLimit(4)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .offset(y: kayma)
    }
}

/// rocketBadgeAnim: rozetin üstündeki animasyon.
///
/// - Kilitli: tutma karesinde sabit, düz badge_locked_top rengi (showLockedStaticBadge).
/// - Açık madalya: sürekli döner.
/// - Açık, diğerleri: bir kez sonuna kadar oynar, bitince tutma karesine geçer (Android
///   onAnimationEnd → progress = topHoldProgress). Tabanlı rozetlerde (roket dışında) ilk
///   gösterimde önce 1 sn boyunca sıfırdan büyür.
private struct RozetDetayUstAnimasyonu: View {
    let durum: RozetDetayDurumu
    let tekrar: Int

    /// 0 = bekliyor (ilk kare), 1 = oynuyor, 2 = bitti (tutma karesinde sabit).
    @State private var asama: Int = 0
    @State private var olcek: CGFloat

    init(durum: RozetDetayDurumu, tekrar: Int) {
        self.durum = durum
        self.tekrar = tekrar
        let buyur: Bool = RozetDetayOlculeri.buyuyerekGelirMi(durum, tekrar: tekrar)
        let ilkOlcek: CGFloat = buyur ? 0.0 : 1.0
        _olcek = State<CGFloat>(initialValue: ilkOlcek)
    }

    private var kenar: CGFloat {
        return RozetDetayOlculeri.ustKenari * durum.ustOlcek
    }

    var body: some View {
        icerik
            .frame(width: kenar, height: kenar)
            .scaleEffect(olcek)
            .onAppear {
                basla()
            }
    }

    @ViewBuilder
    private var icerik: some View {
        if !durum.acik {
            KupaAnimasyonGorunumu(
                dosya: durum.ustDosyasi,
                duzRenk: Color("badge_locked_top"),
                sabitKare: Double(durum.ustTutmaKaresi)
            )
        } else if RozetDetayOlculeri.madalyaMi(durum) {
            KupaAnimasyonGorunumu(dosya: durum.ustDosyasi, dongu: true)
        } else if asama == 1 {
            KupaAnimasyonGorunumu(dosya: durum.ustDosyasi, bitti: { oynatmaBitti() })
        } else if asama == 2 {
            KupaAnimasyonGorunumu(dosya: durum.ustDosyasi, sabitKare: Double(durum.ustTutmaKaresi))
        } else {
            KupaAnimasyonGorunumu(dosya: durum.ustDosyasi, oynat: false)
        }
    }

    private func basla() {
        if !durum.acik || RozetDetayOlculeri.madalyaMi(durum) { return }
        if RozetDetayOlculeri.buyuyerekGelirMi(durum, tekrar: tekrar) {
            // Android: ObjectAnimator SCALE 0 → hedef, topStartDelayMs = 1000; bitince oynatılır.
            withAnimation(.easeInOut(duration: 1.0)) {
                olcek = 1.0
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                asama = 1
            }
        } else {
            asama = 1
        }
    }

    private func oynatmaBitti() {
        // Tutma karesi son kareyse (9999) animasyon zaten orada durur; değilse o kareye geçilir
        // (ör. siyah kuşak 1, ok ve roket 60, bowling 47, olta 24).
        if Double(durum.ustTutmaKaresi) < KupaStil.sonKare {
            asama = 2
        }
    }
}

/// badgeRewardAmount: 56sp çerçeveli sayı. Açık rozette 400 ms'de sıfırdan büyür
/// (REWARD_AMOUNT_SCALE_MS); kilitli rozette durağandır ve gri tonlardadır.
private struct RozetDetaySayisi: View {
    let metin: String
    let acik: Bool

    @State private var olcek: CGFloat

    init(metin: String, acik: Bool) {
        self.metin = metin
        self.acik = acik
        let ilkOlcek: CGFloat = acik ? 0.0 : 1.0
        _olcek = State<CGFloat>(initialValue: ilkOlcek)
    }

    var body: some View {
        RozetDegerYazisi(metin: metin, boyut: 56, acik: acik)
            .scaleEffect(olcek)
            .onAppear {
                if acik {
                    withAnimation(.easeInOut(duration: 0.4)) {
                        olcek = 1.0
                    }
                }
            }
    }
}

// MARK: - İlerleme çubuğu

/// badgeLockedProgressContainer: 32dp yüksek iz (missions_track, köşe 16), mavi dolgu ve üstünde
/// ince parlama şeridi, ortada "2/10" yazısı (Android applyMissionProgressOverlay, done=false).
///
/// Yazı Android'de de en üsttedir (elevation=2dp) ve button_disabled (#CCCCCC) rengindedir; bu
/// renk mavi dolgu ve açık mavi parlama üzerinde soluk kaldığı için burada yazıya ince koyu bir
/// gölge eklendi (Android'de yok) — sayı dolgunun üzerinde okunur kalır.
private struct RozetIlerlemeCubugu: View {
    let metin: String
    let yuzde: Int

    var body: some View {
        GeometryReader { geo in
            let oran: CGFloat = CGFloat(max(0, min(100, yuzde))) / 100.0
            let dolguGenisligi: CGFloat = geo.size.width * oran
            // Parlama: baştan 14dp içeride, dolgunun sonuna 10dp kala biter.
            let parlamaGenisligi: CGFloat = max(0, dolguGenisligi - 14 - 10)
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color("missions_track"))
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color("missions_progress_blue"))
                    .frame(width: dolguGenisligi)
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color("missions_progress_blue_shine"))
                    .frame(width: parlamaGenisligi, height: 6)
                    .offset(x: 14, y: 8)
                Text(metin)
                    .font(Tema.yazi(18))
                    .foregroundColor(Color.white)
                    .shadow(color: Color.black.opacity(0.9), radius: 2, x: 0, y: 1)
                    .frame(width: geo.size.width, height: geo.size.height)
                    .zIndex(1)
            }
        }
        .frame(height: 32)
    }
}

// MARK: - Parça paneli

/// badgePiecePanel: madalya/kupa kazanılan ünite ve sezonların listesi
/// (item_badge_piece_popup_cup.xml / item_badge_piece_popup_medal.xml).
private struct RozetParcaPaneli: View {
    let durum: RozetDetayDurumu

    /// Android BadgePieceMaxHeightScrollView: liste uzarsa panel bu yükseklikte kalır ve kayar.
    private let enCokYukseklik: CGFloat = 260

    var body: some View {
        VStack(spacing: 0) {
            baslikSatiri
            ScrollView(.vertical, showsIndicators: true) {
                VStack(spacing: 0) {
                    ForEach(durum.parcalar) { parca in
                        satir(parca)
                        if parca.sira < durum.parcalar.count - 1 {
                            Rectangle()
                                .fill(Color("badge_piece_row_divider"))
                                .frame(height: 1)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 4)
                        }
                    }
                }
            }
            .frame(maxHeight: listeYuksekligi)
        }
        .padding(.vertical, 4)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color("circleBackground_color")))
        .contentShape(Rectangle())
        // Panelin içine dokunmak paneli kapatmaz (Android: panel içi dokunuşlar yok sayılır).
        .onTapGesture {}
    }

    /// Kısa listede panel içeriği kadar, uzun listede en çok `enCokYukseklik` kadar yer kaplar.
    private var listeYuksekligi: CGFloat {
        let tahmini: CGFloat = CGFloat(durum.parcalar.count) * 61
        return min(tahmini, enCokYukseklik)
    }

    private var baslikSatiri: some View {
        HStack(spacing: 0) {
            Text(KupaMetin.unite)
                .font(Tema.yazi(16))
                .foregroundColor(Color.white.opacity(0.9))
                .frame(maxWidth: .infinity, alignment: .leading)
            Color.clear
                .frame(width: 44, height: 1)
                .padding(.leading, 14)
            Text(KupaMetin.sezon)
                .font(Tema.yazi(16))
                .foregroundColor(KupaStil.solukYazi)
                .padding(.leading, 14)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 6)
    }

    private func satir(_ parca: RozetParcaDurumu) -> some View {
        HStack(spacing: 0) {
            Text(parca.unite)
                .font(Tema.yazi(19))
                .foregroundColor(Color.white.opacity(0.9))
                .frame(maxWidth: .infinity, alignment: .leading)
            parcaSimgesi(parca)
                .padding(.trailing, 30)
            Text(parca.sezon)
                .font(Tema.yazi(17))
                .foregroundColor(KupaStil.solukYazi)
                .padding(.trailing, 15)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(minHeight: 52)
    }

    /// Kupa satırı (pieceRankContainer, 44dp genişlik): 40dp'lik kupa animasyonu 135. karede sabit
    /// (CUP_GOOGLE_HOLD_FRAME), altına 25dp bindirilmiş çerçeveli sıra yazısı. Madalya satırı
    /// (pieceMedalIconSlot, 44×44dp): 40dp'lik madalya animasyonu ilk karesinde sabit.
    @ViewBuilder
    private func parcaSimgesi(_ parca: RozetParcaDurumu) -> some View {
        if durum.parcaKupaMi {
            VStack(spacing: 0) {
                KupaAnimasyonGorunumu(dosya: durum.parcaDosyasi, sabitKare: 135)
                    .frame(width: 40, height: 40)
                KupaCerceveliYazi(
                    metin: parca.derece,
                    boyut: 22,
                    dolgu: Color.white,
                    cerceve: KupaStil.sayiCercevesi
                )
                .padding(.top, -25)
            }
            .frame(width: 44)
        } else {
            KupaAnimasyonGorunumu(dosya: durum.parcaDosyasi, oynat: false)
                .frame(width: 40, height: 40)
                .frame(width: 44, height: 44)
        }
    }
}
