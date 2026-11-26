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

struct RozetDetayEkrani: View {
    private let kapat: (() -> Void)?

    @StateObject private var model: RozetDetayModeli
    /// Dokununca üst animasyonun yeniden oynatılması (yer tutucuda kısa bir sıçrama).
    @State private var sicrama: Bool = false

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
                .onTapGesture {
                    ekranaDokunuldu()
                }
            if let durum = model.durum {
                orta(durum)
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

    /// Büyük rozet, üzerindeki sayı ve açıklama.
    private func orta(_ durum: RozetDetayDurumu) -> some View {
        VStack(spacing: 0) {
            ZStack {
                RozetGorseli(
                    tabanDosyasi: durum.tabanDosyasi,
                    ustDosyasi: durum.ustDosyasi,
                    ustOlcek: durum.ustOlcek,
                    acik: durum.acik,
                    ton: durum.ton,
                    boyut: 280
                )
                .scaleEffect(sicrama ? 1.08 : 1.0)
                if durum.sayiGorunur {
                    RozetDegerYazisi(metin: durum.sayiMetni, boyut: 56, acik: durum.acik)
                        .offset(y: 86)
                }
            }
            .frame(width: 280, height: 280)
            Text(durum.aciklama)
                .font(Tema.yazi(20))
                .foregroundColor(Color.white.opacity(0.9))
                .multilineTextAlignment(.center)
                .lineLimit(4)
                .frame(maxWidth: 350)
                .padding(.horizontal, 16)
                .padding(.top, 28)
        }
        .padding(.bottom, 150)
        .contentShape(Rectangle())
        .onTapGesture {
            ekranaDokunuldu()
        }
    }

    /// Alt bölüm: ilerleme çubuğu, parça listesi ve "Nx" düğmesi.
    private func altKatman(_ durum: RozetDetayDurumu) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            if model.parcaPaneliAcik && !durum.parcalar.isEmpty {
                RozetParcaPaneli(durum: durum)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
            }
            if !durum.parcaSayisiMetni.isEmpty {
                HStack(spacing: 0) {
                    parcaDugmesi(durum)
                    Spacer(minLength: 0)
                }
                .padding(.leading, 20)
                .padding(.bottom, 100)
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

    /// Android: panel açıksa dışına dokununca kapanır; açık rozette dokunuş üst animasyonu yeniden oynatır.
    private func ekranaDokunuldu() {
        if model.parcaPaneliAcik {
            model.parcaPaneliAcik = false
            return
        }
        guard let durum = model.durum, durum.dokununcaOynat else { return }
        withAnimation(.easeOut(duration: 0.15)) {
            sicrama = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.5)) {
                sicrama = false
            }
        }
    }
}

// MARK: - İlerleme çubuğu

/// badgeLockedProgressContainer: 32dp yüksek iz (missions_track, köşe 16), mavi dolgu ve üstünde
/// ince parlama şeridi, ortada "2/10" yazısı (Android applyMissionProgressOverlay, done=false).
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
                    .foregroundColor(Color("button_disabled"))
                    .frame(width: geo.size.width, height: geo.size.height)
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
            ZStack {
                KupaAnimasyonGorunumu(dosya: durum.parcaDosyasi)
                    .frame(width: 40, height: 40)
                if durum.parcaKupaMi {
                    KupaCerceveliYazi(
                        metin: parca.derece,
                        boyut: 22,
                        dolgu: Color.white,
                        cerceve: KupaStil.sayiCercevesi
                    )
                    .offset(y: 8)
                }
            }
            .frame(width: 44)
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
}
