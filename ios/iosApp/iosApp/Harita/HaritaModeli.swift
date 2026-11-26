import Foundation
import Shared
import SwiftUI

// Harita ekranının görünüm modeli.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyadadır; görünümler yalnızca aşağıdaki
// Swift yapılarını (HaritaOgesi, HaritaAltPanelDurumu, ...) okur. Böylece Kotlin → Swift
// adlandırmasından doğabilecek bir derleme hatası tek dosyada aranır.
//
// Android karşılıkları: MapFragment + LessonAdapter (liste), PartSelectionFragment +
// PartCardAdapter (kısım seçimi). Kurallar ortak koddan gelir: LessonAdapterLogic,
// MapFragmentLogic, PartSelectionLogic, MapLayoutLogic, GlobalLessonData.

// MARK: - Görünümlerin okuduğu veri yapıları

/// Listedeki satırın türü (Android: LessonItem.TYPE_*).
enum HaritaOgeTuru {
    case ders
    case baslik
    case sandik
    case yaris
}

/// Sandık kartındaki tek bir yıldız yuvası (ortak kod: LessonAdapterLogic.ChestStarSlot).
enum HaritaYildizi {
    case sari
    case acikGri
    case bos
}

/// Haritadaki bir satır: ortak koddaki LessonItem + ortak kuralların bu satır için verdiği kararlar.
struct HaritaOgesi: Identifiable {
    /// Listedeki sıra (Android: adapterPosition).
    let id: Int
    let oge: LessonItem
    let tur: HaritaOgeTuru
    let baslik: String
    /// Android: item.isCompleted (adı yanıltıcı; "kilidi açık" demek).
    let kilitsiz: Bool
    /// Kart arka planının renk kaynağı adı.
    let kartRengi: String
    /// İlerleme halkasının dilim sayısı ve dolu dilim sayısı.
    let dilimSayisi: Int
    let doluDilim: Int
    /// true: üç yıldızlı sıra; false: ortada tek yıldız.
    let ucYildiz: Bool
    let yildizlar: [HaritaYildizi]
    /// Altın bitiş görünümü (halka gizli, kart altın, parlamalar açık).
    let altin: Bool
    /// Halkanın "nefes alma" animasyonu oynar mı.
    let nefes: Bool
}

/// Listenin üstünde sabit duran ünite başlığı (ortak kod: MapFragmentLogic.StickyHeader).
struct HaritaYapiskanBaslik {
    let baslik: String
    let kisimUnite: String
    /// Renk kaynağı adı; yoksa nil.
    let renk: String?
}

/// Derse dokununca açılan alt panelin içeriği (ortak kod: LessonAdapterLogic.LessonBottomSheetState).
struct HaritaAltPanelDurumu: Identifiable {
    let id: Int
    let oge: HaritaOgesi
    let baslik: String
    let aciklama: String
    let dugmeMetni: String
    let dugmeEtkin: Bool
    let kilitli: Bool
    let egitimTekrarGorunur: Bool
    let rekorMetni: String?
    let rekorGizli: Bool
    let rekorTiklanir: Bool
    let atesGorunur: Bool
    /// Kilidi açık ama bitmemiş ders (Android: panel zemini panel_background, düğme yazısı panel_background).
    let basladi: Bool
}

/// Kısım seçim ekranındaki bir kart (ortak kod: PartState + PartSelectionLogic).
struct HaritaKisimKarti: Identifiable {
    let id: Int
    let partId: Int32
    let baslik: String
    let gorselEtkin: Bool
    let islevselEtkin: Bool
    let kapaliMesaji: String
    /// Renk kaynağı adları.
    let kartRengi: String
    let yaziRengi: String
    /// Android assets içindeki Lottie dosyasının adı (lottie-ios bağlanınca kullanılacak).
    let animasyon: String
}

/// Kısım seçim ekranındaki bir satır ("Toplama", "Çıkarma", "Çarpma").
struct HaritaKisimSatiri: Identifiable {
    let id: Int
    let baslik: String
    let kartlar: [HaritaKisimKarti]
}

/// Henüz yazılmamış bir ekranın yerine gösterilen yer tutucunun içeriği.
struct HaritaHedefi: Identifiable {
    let id = UUID()
    let baslik: String
    let aciklama: String
    let ayrintilar: [String]
}

/// Kısa süreli bilgi balonu (Android: Toast).
struct HaritaBildirimi: Identifiable, Equatable {
    let id: Int
    let metin: String
}

/// İleride Firestore'dan gelen ilerlemeyi takmak için kaynak arayüzü.
/// nil dönerse ortak koddaki varsayılan şablon kullanılır.
protocol HaritaIlerlemeKaynagi: AnyObject {
    func bulutOgeleri(partId: Int32) -> [LessonItem]?
}

// MARK: - Görünüm modeli

final class HaritaModeli: ObservableObject {
    /// Açık kısım; nil ise kısım seçim ekranı gösterilir.
    @Published private(set) var seciliPart: Int32? = nil
    @Published private(set) var ogeler: [HaritaOgesi] = []
    @Published private(set) var kisimSatirlari: [HaritaKisimSatiri] = []
    @Published private(set) var altPanel: HaritaAltPanelDurumu? = nil
    @Published private(set) var bildirim: HaritaBildirimi? = nil
    @Published var hedef: HaritaHedefi? = nil

    // Bu aşamada giriş ve Firebase yok: aşağıdakiler sabit varsayılanlardır.
    // Giriş/cüzdan bağlanınca dışarıdan atanır, ardından `yenile()` çağrılır.
    var plan: String = "Free"
    var ogretmenOnayli: Bool = false
    var sonsuzEnerji: Bool = false
    var enerjiYeterli: Bool = true
    weak var ilerlemeKaynagi: HaritaIlerlemeKaynagi?

    private var hamOgeler: [LessonItem] = []
    /// Android: playedFinalGoldAnimationKeys.
    private var altinOynananlar: Set<String> = []
    private var bildirimSayaci: Int = 0

    private let mantik = LessonAdapterLogic.shared
    private let kisimMantigi = PartSelectionLogic.shared

    init() {
        kisimlariYenile()
    }

    // MARK: Veri

    /// Bir kısmın ders listesi: bulut verisi varsa o, yoksa ortak koddaki şablon.
    private func partOgeleri(_ partId: Int32) -> [LessonItem] {
        let bulut: [LessonItem]? = ilerlemeKaynagi?.bulutOgeleri(partId: partId)
        return GlobalLessonData.shared.loadLessonItemsForPart(partId: partId, cloudItems: bulut)
    }

    /// Dışarıdan ilerleme değiştiğinde (ileride Firestore dinleyicisi) çağrılır.
    func yenile() {
        kisimlariYenile()
        if let partId = seciliPart {
            listeyiKur(partId: partId)
        }
    }

    private func listeyiKur(partId: Int32) {
        hamOgeler = partOgeleri(partId)
        var yeni: [HaritaOgesi] = []
        yeni.reserveCapacity(hamOgeler.count)
        for (sira, oge) in hamOgeler.enumerated() {
            yeni.append(ogeKur(oge, sira: sira))
        }
        ogeler = yeni
    }

    private func ogeKur(_ oge: LessonItem, sira: Int) -> HaritaOgesi {
        let sabitler = LessonItem.companion
        let turKodu: Int32 = oge.type
        var tur = HaritaOgeTuru.ders
        if turKodu == sabitler.TYPE_HEADER {
            tur = .baslik
        } else if turKodu == sabitler.TYPE_CHEST {
            tur = .sandik
        } else if turKodu == sabitler.TYPE_RACE {
            tur = .yaris
        }

        if tur == .baslik {
            return HaritaOgesi(
                id: sira, oge: oge, tur: tur, baslik: oge.title, kilitsiz: true, kartRengi: "",
                dilimSayisi: 1, doluDilim: 0, ucYildiz: false, yildizlar: [], altin: false, nefes: false
            )
        }
        if tur == .yaris {
            return HaritaOgesi(
                id: sira, oge: oge, tur: tur, baslik: oge.title,
                kilitsiz: mantik.raceFlagClickAllowed(item: oge),
                kartRengi: mantik.raceCardColor(item: oge),
                dilimSayisi: 1, doluDilim: 0, ucYildiz: false, yildizlar: [], altin: false, nefes: false
            )
        }

        // Ders ve sandık kartı (Android: LessonViewHolder.bind).
        let dilimler = mantik.stepSegments(item: oge)
        let anahtar: String = mantik.lessonProgressKey(item: oge, adapterPosition: Int32(sira))
        let oynandi = altinOynananlar.contains(anahtar)
        let karar = mantik.progressAnimationDecision(
            item: oge,
            goldAnimationPlayed: oynandi,
            pendingFromFilled: nil,
            canConsumePending: false,
            lastSeenFilled: nil
        )
        let eylem: String = karar.action.name
        let altin = eylem == "APPLY_PERSISTENT_GOLD" || eylem == "SHOW_TARGET_AND_PLAY_GOLD"
        if altin {
            altinOynananlar.insert(anahtar)
        }
        let nefes = mantik.shouldStartBreathingAnimation(item: oge, goldAnimationPlayed: oynandi || altin)

        var ucYildiz = false
        var yildizlar: [HaritaYildizi] = []
        if tur == .sandik {
            let sira3 = mantik.chestStarsForBind(item: oge)
            ucYildiz = sira3.threeStarMode
            for yuva in sira3.slots {
                let ad: String = yuva.name
                if ad == "YellowOn" {
                    yildizlar.append(.sari)
                } else if ad == "LightGrayOn" {
                    yildizlar.append(.acikGri)
                } else {
                    yildizlar.append(.bos)
                }
            }
        }

        return HaritaOgesi(
            id: sira, oge: oge, tur: tur, baslik: oge.title,
            kilitsiz: oge.isCompleted,
            kartRengi: mantik.lessonCardColor(item: oge),
            dilimSayisi: Int(dilimler.segmentCount),
            doluDilim: Int(dilimler.completedSegments),
            ucYildiz: ucYildiz, yildizlar: yildizlar, altin: altin, nefes: nefes
        )
    }

    // MARK: Kısım seçimi

    private func kisimlariYenile() {
        let pro = kisimMantigi.hasProPlan(plan: plan, teacherApproved: ogretmenOnayli)
        let p1 = kisimMantigi.lastChestComplete(items: partOgeleri(1))
        let p2 = kisimMantigi.lastChestComplete(items: partOgeleri(2))
        let p3 = kisimMantigi.lastChestComplete(items: partOgeleri(3))
        kisimSatirlari = [
            kisimSatiri(sira: 0, baslik: kisimMantigi.CATEGORY_TITLE_ADDITION,
                        partlar: kisimMantigi.ADDITION_PARTS, pro: pro, p1: p1, p2: p2, p3: p3),
            kisimSatiri(sira: 1, baslik: kisimMantigi.CATEGORY_TITLE_SUBTRACTION,
                        partlar: kisimMantigi.SUBTRACTION_PARTS, pro: pro, p1: p1, p2: p2, p3: p3),
            kisimSatiri(sira: 2, baslik: kisimMantigi.CATEGORY_TITLE_MULTIPLICATION,
                        partlar: kisimMantigi.MULTIPLICATION_PARTS, pro: pro, p1: p1, p2: p2, p3: p3),
        ]
    }

    private func kisimSatiri(
        sira: Int, baslik: String, partlar: [KotlinInt], pro: Bool, p1: Bool, p2: Bool, p3: Bool
    ) -> HaritaKisimSatiri {
        let durumlar: [PartState] = kisimMantigi.partStates(
            partIds: partlar,
            hasProPlan: pro,
            part1ChestComplete: p1,
            part2ChestComplete: p2,
            part3ChestComplete: p3
        )
        var kartlar: [HaritaKisimKarti] = []
        for durum in durumlar {
            let partId: Int32 = durum.partId
            kartlar.append(
                HaritaKisimKarti(
                    id: Int(partId),
                    partId: partId,
                    baslik: kisimMantigi.partTitle(partId: partId),
                    gorselEtkin: durum.visualActive,
                    islevselEtkin: durum.functionalActive,
                    kapaliMesaji: durum.inactiveMessage,
                    kartRengi: kisimMantigi.partCardColor(state: durum),
                    yaziRengi: kisimMantigi.partLabelColor(state: durum),
                    animasyon: kisimMantigi.partAnimationName(partId: partId)
                )
            )
        }
        return HaritaKisimSatiri(id: sira, baslik: baslik, kartlar: kartlar)
    }

    /// Android: PartCardAdapter kart tıklaması + PartSelectionFragment.openMapForPart.
    func kisimSec(_ kart: HaritaKisimKarti) {
        if !kart.islevselEtkin {
            bildir(kart.kapaliMesaji)
            return
        }
        if kisimMantigi.opensRacePanel(partId: kart.partId) {
            // Android burada harita yerine yarış panelini açar; panel henüz yazılmadı.
            hedef = HaritaHedefi(
                baslik: kisimMantigi.racePanelTitle(partId: kart.partId),
                aciklama: "Yarış paneli",
                ayrintilar: ["Kısım: \(kart.partId)"]
            )
            return
        }
        GlobalLessonData.shared.globalPartId = kart.partId
        listeyiKur(partId: kart.partId)
        seciliPart = kart.partId
    }

    /// Android: lessonPartBackButton (haritayı kapat, kısım seçimine dön).
    func kisimSeciminDon() {
        altPanel = nil
        seciliPart = nil
        ogeler = []
        hamOgeler = []
        kisimlariYenile()
    }

    // MARK: Sabit başlık ve yerleşim

    /// Android: MapFragment.updateStickyHeader.
    func yapiskanBaslik(ilkGorunen: Int) -> HaritaYapiskanBaslik? {
        guard let b = MapFragmentLogic.shared.stickyHeaderFor(items: hamOgeler, firstVisible: Int32(ilkGorunen)) else {
            return nil
        }
        return HaritaYapiskanBaslik(baslik: b.headerTitle, kisimUnite: b.sectionUnit, renk: b.color)
    }

    /// Android: DynamicOffsetDecoration (ders kartının zikzak yatay kayması).
    func yatayKayma(_ o: HaritaOgesi, listeGenisligi: CGFloat) -> CGFloat {
        let kayma: Float = MapLayoutLogic.shared.lessonHorizontalShiftDefault(
            item: o.oge, innerWidth: Float(listeGenisligi)
        )
        return CGFloat(kayma)
    }

    // MARK: Dokunmalar

    /// Android: lessonCard.setOnClickListener (LessonViewHolder / RaceViewHolder).
    func ogeyeDokun(_ o: HaritaOgesi) {
        switch o.tur {
        case .baslik:
            return
        case .yaris:
            if !o.kilitsiz {
                // strings.xml: race_unlock_complete_all_lessons
                bildir("Burayı açmak için bütün dersleri tamamla.")
            } else {
                hedef = HaritaHedefi(
                    baslik: o.baslik,
                    aciklama: "Yarış paneli",
                    ayrintilar: ["Harita sırası: \(o.id)"]
                )
            }
        case .ders, .sandik:
            altPanel = altPanelKur(o)
        }
    }

    private func altPanelKur(_ o: HaritaOgesi) -> HaritaAltPanelDurumu {
        let partId: Int32 = seciliPart ?? GlobalLessonData.shared.globalPartId
        let d = mantik.lessonBottomSheetState(item: o.oge, globalPartId: partId)
        return HaritaAltPanelDurumu(
            id: o.id,
            oge: o,
            baslik: d.title,
            aciklama: d.description_,
            dugmeMetni: d.actionText,
            dugmeEtkin: d.actionEnabled,
            kilitli: d.locked,
            egitimTekrarGorunur: d.againTutorialVisible,
            rekorMetni: d.recordText,
            rekorGizli: d.recordLayoutGone,
            rekorTiklanir: d.recordClickable,
            atesGorunur: d.fireAnimVisible,
            basladi: o.oge.isCompleted && !o.oge.stepIsFinish
        )
    }

    func altPaneliKapat() {
        altPanel = nil
    }

    /// Android: actionButton.setOnClickListener (enerji kararı + continueWithLesson).
    func eylemDugmesi(_ panel: HaritaAltPanelDurumu) {
        let oge = panel.oge.oge
        let karar: String = mantik.actionButtonDecision(
            item: oge, isInfiniteEnergy: sonsuzEnerji, hasEnoughEnergy: enerjiYeterli
        ).name
        if karar == "LOCKED_COLLAPSE" {
            return
        }
        altPanel = nil
        if karar == "NOT_ENOUGH_ENERGY" {
            // Android: panel kapanır, mağaza (enerji yenileme) açılır.
            hedef = HaritaHedefi(
                baslik: panel.baslik,
                aciklama: "Enerji yok",
                ayrintilar: ["Açılacak ekran: Mağaza (enerji yenileme)"]
            )
            return
        }
        let enerji: String
        if karar == "USE_ENERGY_AND_START" {
            enerji = "Enerji: \(mantik.LESSON_ENERGY_COST) harcanır"
        } else {
            enerji = "Enerji: harcanmaz (sonsuz enerji)"
        }
        let hedefAdi: String = mantik.continueWithLessonTarget(item: oge).name
        hedef = dersHedefi(panel: panel, hedefAdi: hedefAdi, ekSatir: enerji)
    }

    /// Android: againTutorial.setOnClickListener (öğreticiyi yeniden aç).
    func egitimiTekrarla(_ panel: HaritaAltPanelDurumu) {
        altPanel = nil
        hedef = dersHedefi(panel: panel, hedefAdi: "TUTORIAL", ekSatir: "Eğitimi tekrarla")
    }

    /// Android: recordLayout.setOnClickListener (liderlik tablosu / RecordFragment).
    func rekoraDokun(_ panel: HaritaAltPanelDurumu) {
        if !panel.rekorTiklanir {
            return
        }
        altPanel = nil
        hedef = HaritaHedefi(
            baslik: panel.baslik,
            aciklama: "Liderlik tablosu",
            ayrintilar: ["Harita sırası: \(panel.id)"]
        )
    }

    /// Android: askQuestionButton (öğretmene soru sor).
    func soruSorDokunuldu() {
        hedef = HaritaHedefi(baslik: "Öğretmene sor", aciklama: "Soru sorma ekranı", ayrintilar: [])
    }

    private func dersHedefi(panel: HaritaAltPanelDurumu, hedefAdi: String, ekSatir: String) -> HaritaHedefi {
        let oge = panel.oge.oge
        let ekran: String
        if hedefAdi == "TUTORIAL" {
            ekran = "Öğretici (\(oge.tutorialNumber))"
        } else if hedefAdi == "BLINDING_LESSON" {
            ekran = "Körleme dersi"
        } else {
            ekran = "Abaküs dersi"
        }
        var satirlar: [String] = ["Açılacak ekran: \(ekran)"]
        if let partId = seciliPart {
            satirlar.append("Kısım: \(partId)")
        }
        if let haritaSirasi = oge.mapFragmentIndex {
            satirlar.append("Harita sırası: \(haritaSirasi.intValue)")
        }
        if let adim = oge.startStepNumber {
            satirlar.append("Başlangıç adımı: \(adim.intValue)")
        }
        satirlar.append(ekSatir)
        return HaritaHedefi(baslik: panel.baslik, aciklama: panel.aciklama, ayrintilar: satirlar)
    }

    // MARK: Bilgi balonu

    private func bildir(_ metin: String) {
        bildirimSayaci += 1
        let no = bildirimSayaci
        bildirim = HaritaBildirimi(id: no, metin: metin)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self else { return }
            if self.bildirim?.id == no {
                self.bildirim = nil
            }
        }
    }
}
