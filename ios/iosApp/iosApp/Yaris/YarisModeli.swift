import Combine
import Foundation
import Shared

// Yarış paneli ("Ustalık Yolu") görünüm modeli.
//
// Android: LessonAdapter.showRacePanel / showRaceLessonBottomSheet / showRaceFastForwardPanel /
// onRaceStartClicked + RaceAdapter. Kurallar ortak koddadır (LessonAdapterLogic: raceRowStatus,
// raceLessonBottomSheetState, RACE_FAST_FORWARD_KEY_COST); burada yalnızca durum ve geçişler var.
//
// Dış dünya (ilerleme deposu, ders başlatma) `YarisPaneliBaglantisi` ile verilir; panel hiçbir ekranı
// doğrudan açmaz.

/// Panelin açılışını anlatan yalın değerler.
struct YarisPaneliGirdisi {
    /// Android: item.racePartId (7 = Toplama Ustalığı, 8 = Çıkarma Ustalığı).
    let partId: Int32
    /// Panel başlığı (Android: item.title — "Toplama Ustalığı" / "Çıkarma Ustalığı").
    let baslik: String
    /// Android: isFromPartSelection. true ise kapanınca aktif kısım değişmez.
    let partSecimindenMi: Bool
    /// Android: item.backRaceId — panel haritadaki bayraktan açıldıysa kapanınca bu kısım yeniden belleğe alınır.
    let geriPartId: Int32?
}

/// Panelin dış dünyaya dokunan işleri.
struct YarisPaneliBaglantisi {
    /// Android: GlobalLessonData.loadLessonItemsForPart(racePartId) — kısmın güncel yarış listesi.
    var ogeler: (Int32) -> [LessonItem]
    /// Android: onRaceStartClicked. Parametreler: kısım, listedeki sıra, yarış öğesi.
    var baslat: (Int32, Int, LessonItem) -> Void
    /// Ders ilerlemesi değişince (yarış bitti) haber verir; panel listeyi yeniden okur
    /// (Android: LessonManager.updateRaceItem → RaceAdapter.updateRaceItem).
    var yenileme: AnyPublisher<Void, Never>
}

/// Galeri için açılış durumu.
enum YarisBaslangicDurumu {
    /// Yalnızca liste.
    case liste
    /// Verilen satır için yarış alt paneli açık.
    case altPanel(satir: Int)
    /// Verilen (kilitli) satır için alt panel ve "İleri Sar" penceresi açık.
    case ileriSar(satir: Int)
}

/// Listedeki bir yarış satırı (Android: RaceAdapter.RaceViewHolder.bind).
struct YarisSatiri: Identifiable {
    /// Listedeki sıra (Android: clickedIndex).
    let id: Int
    let oge: LessonItem
    /// raceBusyLevel: 0 tamamlandı, 1 başla, 2 kilitli; null → başla gibi.
    let seviye: Int?
    let ad: String
    let durumMetni: String
    let kilitGorunur: Bool
    let durumEtkin: Bool
}

/// race_lesson_bottom_sheet.xml'in durumu (ortak koddan: raceLessonBottomSheetState).
struct YarisAltPanel {
    let satir: YarisSatiri
    let baslik: String
    let aciklama: String
    let kilitli: Bool
    let eylemMetni: String
}

final class YarisPaneliModeli: ObservableObject {
    let girdi: YarisPaneliGirdisi
    @Published private(set) var satirlar: [YarisSatiri] = []
    /// Açık yarış alt paneli.
    @Published private(set) var altPanel: YarisAltPanel? = nil
    /// Açık "İleri Sar" penceresi (kilitli yarış).
    @Published private(set) var ileriSarSatiri: YarisSatiri? = nil
    /// Anahtar harcaması sunucudayken düğme kapalı.
    @Published private(set) var anahtarIslemde: Bool = false
    /// Kısa süreli bilgi balonu (Android: Toast.LENGTH_SHORT).
    @Published private(set) var bildirim: String? = nil

    private let baglanti: YarisPaneliBaglantisi
    private let cuzdan: MagazaCuzdanServisi
    private let mantik = LessonAdapterLogic.shared
    private var abonelikler: Set<AnyCancellable> = []
    private var bildirimSayaci: Int = 0

    init(
        girdi: YarisPaneliGirdisi,
        baglanti: YarisPaneliBaglantisi,
        cuzdan: MagazaCuzdanServisi,
        baslangic: YarisBaslangicDurumu
    ) {
        self.girdi = girdi
        self.baglanti = baglanti
        self.cuzdan = cuzdan
        yenile()
        baglanti.yenileme
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.yenile()
            }
            .store(in: &abonelikler)
        baslangicDurumunuUygula(baslangic)
    }

    private func baslangicDurumunuUygula(_ baslangic: YarisBaslangicDurumu) {
        switch baslangic {
        case .liste:
            return
        case .altPanel(let sira):
            if sira >= 0 && sira < satirlar.count {
                satirDokun(satirlar[sira])
            }
        case .ileriSar(let sira):
            if sira >= 0 && sira < satirlar.count {
                satirDokun(satirlar[sira])
                altPanelEylemi()
            }
        }
    }

    // MARK: Liste

    /// Android: RaceAdapter.raceUpdateItems. Liste ortak koddaki güncel yarış listesinden okunur.
    func yenile() {
        let ham: [LessonItem] = baglanti.ogeler(girdi.partId)
        var yeni: [YarisSatiri] = []
        yeni.reserveCapacity(ham.count)
        for (sira, oge) in ham.enumerated() {
            yeni.append(satirKur(oge, sira: sira))
        }
        satirlar = yeni
    }

    private func satirKur(_ oge: LessonItem, sira: Int) -> YarisSatiri {
        let durum = mantik.raceRowStatus(item: oge)
        let seviye: Int? = oge.raceBusyLevel?.intValue
        return YarisSatiri(
            id: sira,
            oge: oge,
            seviye: seviye,
            ad: oge.title,
            durumMetni: durumMetniniCevir(durum.statusText),
            kilitGorunur: durum.lockVisible,
            durumEtkin: durum.statusEnabled
        )
    }

    /// Ortak kodun Türkçe durum metnini Android'deki string kaynağına çevirir (aynı metin).
    private func durumMetniniCevir(_ turkce: String) -> String {
        if turkce == "TAMAMLANDI" {
            return Metin.al("ui_tamamlandi_caps")
        }
        if turkce == "KİLİTLİ" {
            return Metin.al("ui_kilitli_2")
        }
        if turkce == "BAŞLA" {
            return Metin.al("ui_basla_2")
        }
        return turkce
    }

    // MARK: Alt panel

    /// Android: RaceAdapter satır / durum tıklaması → showRaceLessonBottomSheet.
    func satirDokun(_ satir: YarisSatiri) {
        let d = mantik.raceLessonBottomSheetState(raceItem: satir.oge)
        altPanel = YarisAltPanel(
            satir: satir,
            baslik: d.title,
            aciklama: d.description_,
            kilitli: d.isLockedRace,
            eylemMetni: eylemMetniniCevir(d.actionText)
        )
    }

    /// Ortak kodun Türkçe düğme metnini Android string kaynağına çevirir ("Tekrar dene", "BAŞLAT").
    private func eylemMetniniCevir(_ turkce: String) -> String {
        if turkce == mantik.TEXT_RETRY {
            return Metin.al("ui_tekrar_dene")
        }
        if turkce == mantik.TEXT_RACE_START {
            return Metin.al("ui_baslat")
        }
        return turkce
    }

    func altPaneliKapat() {
        altPanel = nil
    }

    /// Android: raceActionButton.setOnClickListener — kilitliyse İleri Sar penceresi, değilse doğrudan başlat.
    func altPanelEylemi() {
        guard let panel = altPanel else {
            return
        }
        if panel.kilitli {
            ileriSarSatiri = panel.satir
            return
        }
        altPanel = nil
        baglanti.baslat(girdi.partId, panel.satir.id, panel.satir.oge)
    }

    // MARK: İleri Sar (anahtarla)

    func ileriSarKapat() {
        ileriSarSatiri = nil
    }

    /// Android: raceFastForwardDiamond — RACE_FAST_FORWARD_KEY_COST anahtar harcanır (MainActivity.spendKeys,
    /// ölçüm kimliği AnalyticsLogger.ITEM_RACE_FAST_FORWARD); yetmezse ya da sunucu reddederse
    /// "Yeterli anahtarın yok." balonu. Başarıda pencere ve alt panel kapanır, yarış başlar.
    func ileriSarOnayla() {
        guard let satir = ileriSarSatiri else {
            return
        }
        if anahtarIslemde {
            return
        }
        let bedel: Int = Int(mantik.RACE_FAST_FORWARD_KEY_COST)
        if cuzdan.cuzdan.anahtar < bedel {
            bildir(Metin.al("daily_question_insufficient_keys"))
            return
        }
        anahtarIslemde = true
        let gerekce: String = WalletReason.shared.SPEND
        cuzdan.degistir(
            altinFarki: 0,
            anahtarFarki: -bedel,
            gerekce: gerekce,
            urunKimligi: "race_fast_forward",
            geriAlmaJetonu: nil
        ) { [weak self] degisim in
            guard let self = self else {
                return
            }
            self.anahtarIslemde = false
            if degisim == nil {
                self.bildir(Metin.al("daily_question_insufficient_keys"))
                return
            }
            self.ileriSarSatiri = nil
            self.altPanel = nil
            self.baglanti.baslat(self.girdi.partId, satir.id, satir.oge)
        }
    }

    // MARK: Bilgi balonu

    private func bildir(_ metin: String) {
        bildirimSayaci += 1
        let no: Int = bildirimSayaci
        bildirim = metin
        // Android: Toast.LENGTH_SHORT ≈ 2 sn.
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self else {
                return
            }
            if self.bildirimSayaci == no {
                self.bildirim = nil
            }
        }
    }
}
