import Shared
import SwiftUI

// Rozet kutlaması (seviye atlama) ekranı.
//
// Android karşılığı: BadgeFragment.kt'nin kutlama kipi + res/layout/fragment_badge.xml (tek rozet ekranıyla aynı
// yerleşim: 560dp'lik taban, 280dp'lik üst animasyon, sayı, açıklama, alttaki 32dp'lik ilerleme çubuğu; kapat
// düğmesi yok, geri tuşu kapalı). Açılış: BadgeProgressFirestore.openBadgeCelebration (ders sonrası kuyruğunun
// "rozet" adımı: MainActivity.showBadgeStep) ve görev/günlük soru ödülü sonrası TasksFragment.
//
// Akış ve ayrıntılar `RozetKutlamaModeli.swift` başlığındadır. Madalya ve kupa gibi kademesiz rozetlerde Android
// kutlama animasyonu çalıştırmaz, düz rozet ekranını gösterir ve dokunuş kuyruğu ilerletir; burada aynısı için
// `RozetDetayEkrani` gösterilir (kapat düğmesi kuyruğu ilerletir).
//
// Basitleştirilenler: Android'in sayı patlaması (badgeRewardPopBurst) çizilmez; sezon liderliği madalya kuyruğu
// (seasonLeaderboardAckAfterQueue) iOS'ta yok; çıkış geçişi yöneticiden gelir.

/// Kutlama kuyruğu: her öğe "MODE|from|to|target" (ortak kod BadgeProgressFirestore.payloadToQueueItem).
/// Sondaki rozetten sonra dokunmak `bitti`yi çağırır.
struct RozetKutlamaEkrani: View {
    private let kuyruk: [String]
    private let ortam: KupaOrtami
    private let bitti: () -> Void

    /// Android: BadgeFragment.newLevelUpSequenceInstance(payloads, index) içindeki dizin.
    @State private var sira: Int = 0

    /// - Parameters:
    ///   - kuyruk: kutlanacak seviye atlamalar (boş olmamalı; Android boş kuyrukta ekranı hiç açmaz).
    ///   - ortam: ses ve rozet servisleri (varsayılan: örnek veri; yalnızca `sesCalici` kullanılır).
    ///   - bitti: kuyruk bitti ve son rozete dokunuldu (Android: closeCelebrationFragmentWithSlide).
    init(kuyruk: [String], ortam: KupaOrtami = KupaOrtami.sahte, bitti: @escaping () -> Void) {
        self.kuyruk = kuyruk
        self.ortam = ortam
        self.bitti = bitti
    }

    var body: some View {
        RozetKutlamaSahnesi(kuyruk: kuyruk, sira: sira, ortam: ortam, ilerle: { ilerle() })
            .id(sira)
    }

    /// Android: hasNextCelebrationInQueue → goToNextCelebrationInQueue, yoksa kapat.
    private func ilerle() {
        let boyut: KotlinInt = KotlinInt(int: Int32(kuyruk.count))
        let sonrakiVar: Bool = BadgeFragmentLogic.shared.hasNextCelebrationInQueue(queueSize: boyut, index: Int32(sira))
        if sonrakiVar {
            sira += 1
        } else {
            bitti()
        }
    }
}

// MARK: - Sahne (tek rozet)

private struct RozetKutlamaSahnesi: View {
    @StateObject private var model: RozetKutlamaModeli
    private let modAdi: String
    private let kutlamaVar: Bool
    private let ortam: KupaOrtami
    private let ilerle: () -> Void

    init(kuyruk: [String], sira: Int, ortam: KupaOrtami, ilerle: @escaping () -> Void) {
        let veri: RozetKutlamaVerisi = RozetKutlamaVerisi.olustur(kuyruk: kuyruk, sira: sira)
        _model = StateObject(wrappedValue: RozetKutlamaModeli(veri: veri, ses: ortam.sesCalici))
        self.modAdi = veri.modAdi
        self.kutlamaVar = veri.kutlamaVar
        self.ortam = ortam
        self.ilerle = ilerle
    }

    var body: some View {
        if kutlamaVar {
            RozetKutlamaAnaGorunum(model: model, ilerle: ilerle)
        } else {
            RozetDetayEkrani(modAdi: modAdi, kapat: ilerle, ortam: ortam)
        }
    }
}

private struct RozetKutlamaAnaGorunum: View {
    @ObservedObject var model: RozetKutlamaModeli
    let ilerle: () -> Void

    var body: some View {
        ZStack {
            Color("background_color")
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture {
                    dokunuldu()
                }
            RozetKutlamaOrtasi(model: model)
                .allowsHitTesting(false)
            altKatman
                .allowsHitTesting(false)
        }
        .onAppear {
            model.basla()
        }
        .onDisappear {
            model.dur()
        }
    }

    /// Android: animasyon bitene kadar dokunma dinleyicisi yoktur.
    private func dokunuldu() {
        if model.asama == RozetKutlamaModeli.Asama.bitti {
            ilerle()
        }
    }

    /// badgeLockedProgressContainer: yanlarda 24dp, alttan 80dp.
    private var altKatman: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            RozetKutlamaCubugu(metin: model.ilerlemeMetni, yuzde: model.ilerlemeYuzdesi, tamam: model.tamamlandi)
                .padding(.horizontal, 24)
                .padding(.bottom, 80)
        }
    }
}

// MARK: - Rozet (taban, üst animasyon, sayı, açıklama)

/// Tek rozet ekranıyla aynı yerleşim (`RozetDetayOlculeri`): her parça 1×1'lik bir çapaya bağlanır ve oradan taşar.
private struct RozetKutlamaOrtasi: View {
    @ObservedObject var model: RozetKutlamaModeli

    private var veri: RozetKutlamaVerisi { return model.veri }

    /// Siyah kuşakta sayı ve açıklama 20dp yukarı kayar (KARATE_REWARD_SLOT_OFFSET_UP_DP); öbür modlarda kayma yok.
    private var ekKayma: CGFloat { return veri.karate ? -20 : 0 }

    var body: some View {
        ZStack {
            taban
            ust
            sayi
            aciklama
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: Taban

    /// dailyTasksCompliteBadge: sayım sırasında son karede (önceki tonla), kutlamada baştan oynar (yeni tonla).
    @ViewBuilder
    private var taban: some View {
        if !veri.tabanDosyasi.isEmpty {
            tabanKatmani
                .frame(width: RozetDetayOlculeri.tabanKenari, height: RozetDetayOlculeri.tabanKenari)
                .frame(width: 1, height: 1)
                .offset(y: RozetDetayOlculeri.merkezKaymasi)
        }
    }

    @ViewBuilder
    private var tabanKatmani: some View {
        if model.asama == RozetKutlamaModeli.Asama.sayim {
            RozetKutlamaLottie(
                dosya: veri.tabanDosyasi,
                duzRenk: veri.oncekiTon.isEmpty ? Color("badge_locked_base") : nil,
                sabitKare: KupaStil.sonKare
            )
            .modifier(KupaRozetTonu(ton: veri.oncekiTon))
        } else {
            RozetKutlamaLottie(
                dosya: veri.tabanDosyasi,
                duzRenk: veri.yeniTon.isEmpty ? Color("badge_locked_base") : nil
            )
            .modifier(KupaRozetTonu(ton: veri.yeniTon))
        }
    }

    // MARK: Üst animasyon

    private var ustKenar: CGFloat {
        return RozetDetayOlculeri.ustKenari * veri.ustOlcek
    }

    private var ust: some View {
        let kayma: CGFloat = RozetDetayOlculeri.merkezKaymasi + RozetDetayOlculeri.ustKaymasi
        return ustKatmani
            .frame(width: ustKenar, height: ustKenar)
            .modifier(RozetKutlamaDaire(daireli: veri.daireli))
            .scaleEffect(model.ustCarpani)
            .frame(width: 1, height: 1)
            .offset(y: kayma)
    }

    /// rocketBadgeAnim: sayımda tutma karesinde sabit (önceki ton yoksa düz gri), kutlamada baştan oynar,
    /// bitince tutma karesinde durur.
    @ViewBuilder
    private var ustKatmani: some View {
        switch model.asama {
        case .sayim:
            RozetKutlamaLottie(
                dosya: veri.ustDosyasi,
                duzRenk: veri.oncekiTon.isEmpty ? Color("badge_locked_top") : nil,
                sabitKare: veri.ustTutmaKaresi
            )
        case .kutlama:
            RozetKutlamaLottie(
                dosya: veri.ustDosyasi,
                hiz: veri.ustHiz,
                bitti: { model.ustAnimasyonuBitti() }
            )
            .id(model.ustTur)
        case .bitti:
            RozetKutlamaLottie(dosya: veri.ustDosyasi, sabitKare: veri.ustTutmaKaresi)
        }
    }

    // MARK: Sayı ve açıklama

    /// badgeRewardAmount: 56sp çerçeveli sayı; sayımda eski değer, kutlamada yeni değer büyüyerek gelir.
    private var sayi: some View {
        let kayma: CGFloat = RozetDetayOlculeri.merkezKaymasi + RozetDetayOlculeri.sayiUzakligi + ekKayma
        return RozetDegerYazisi(metin: String(model.gosterilenDeger), boyut: 56, acik: true)
            .scaleEffect(model.degerOlcegi)
            .frame(width: 1, height: 1)
            .offset(y: kayma)
    }

    /// badgeRewardDetailText: 350dp genişlik, ortalı, en çok 4 satır, 20sp, #E6FFFFFF.
    private var aciklama: some View {
        let kayma: CGFloat = RozetDetayOlculeri.merkezKaymasi + RozetDetayOlculeri.aciklamaUzakligi + ekKayma
        return Color.clear
            .frame(maxWidth: 350)
            .frame(height: 1)
            .overlay(alignment: .top) {
                Text(veri.aciklama)
                    .font(Tema.yazi(20))
                    .foregroundColor(Color.white.opacity(0.9))
                    .multilineTextAlignment(.center)
                    .lineLimit(4)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .offset(y: kayma)
    }
}

/// bg_badge_circle_frame (oval, saydam) + clipToOutline: üst animasyon daireye kırpılır.
private struct RozetKutlamaDaire: ViewModifier {
    let daireli: Bool

    func body(content: Content) -> some View {
        if daireli {
            content.clipShape(Circle())
        } else {
            content
        }
    }
}

/// `KupaAnimasyonGorunumu` ile aynı (düz renk filtresi + sabit kare) ve ek olarak oynatma hızı:
/// Android `rocketBadgeAnim.speed = cfg.topSpeed` (bowling 0.5).
private struct RozetKutlamaLottie: View {
    let dosya: String
    var duzRenk: Color? = nil
    var hiz: Double = 1.0
    var sabitKare: Double? = nil
    var bitti: (() -> Void)? = nil

    var body: some View {
        let parlaklik: Double = duzRenk == nil ? 0.0 : 1.0
        let carpim: Color = duzRenk ?? Color.white
        let oynasin: Bool = sabitKare == nil
        return LottieGorunumu(
            KupaStil.lottieAdi(dosya),
            dongu: false,
            hiz: hiz,
            oynat: oynasin,
            bitti: bitti,
            baslangicKare: sabitKare,
            bitisKare: nil
        )
        .brightness(parlaklik)
        .colorMultiply(carpim)
        .accessibilityHidden(true)
    }
}

// MARK: - İlerleme çubuğu

/// badgeLockedProgressContainer: 32dp yüksek iz (missions_track, köşe 16), dolgu ve üstünde ince parlama şeridi,
/// ortada "2/10" yazısı. Sayım bitince altın dolgu (mission_progress_fill_gold) ve "Tamamlandı !" yazısı
/// (applyMissionProgressOverlay, done = true). Yazı beyaz + koyu gölge (tek rozet ekranındaki çubukla aynı).
private struct RozetKutlamaCubugu: View {
    let metin: String
    let yuzde: Int
    let tamam: Bool

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
                    .fill(Color(tamam ? "missions_progress_complete" : "missions_progress_blue"))
                    .frame(width: dolguGenisligi)
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color(tamam ? "missions_progress_gold_shine" : "missions_progress_blue_shine"))
                    .frame(width: parlamaGenisligi, height: 6)
                    .offset(x: 14, y: 8)
                Text(metin)
                    .font(Tema.yazi(18))
                    .foregroundColor(Color.white)
                    .shadow(color: KupaStil.renk(0x000000, alfa: 230.0 / 255.0), radius: 4, x: 0, y: 2)
                    .frame(width: geo.size.width, height: geo.size.height)
                    .zIndex(1)
            }
        }
        .frame(height: 32)
    }
}
