import Foundation
import Shared
import SwiftUI
import UIKit

// Abaküs özelleştirme ekranının görünüm modeli. Android karşılığı: AbacusCustomizationFragment.kt.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyadadır; görünümler yalnızca aşağıdaki Swift
// yapılarını okur. Kurallar ortak koddadır:
//  - AbacusCustomizationUiLogic, AbacusPreviewMetrics (ios/shared/.../abakus/AbacusCustomizationUiLogic.kt)
//  - AbacusCustomizationRules, BeadData, FrameData (AbacusPurchases.kt)
//  - AbacusPreferences (AbacusPreferences.kt), AbacusCustomizationFirestore
//  - WalletReason (oyun/UserWalletFirestore.kt)
// Boncuk ve çerçeve tipleri görünümlere adlarıyla (BeadType.name / FrameType.name) verilir.

// MARK: - Görünümlerin okuduğu yapılar

/// 1. sekmedeki boncuk kartı.
struct OzellestirmeBoncukKarti: Identifiable {
    /// BeadType adı.
    let tipAdi: String
    let gorselAdi: String
    let ad: String
    let gorselGenisligi: CGFloat
    let gorselYuksekligi: CGFloat
    let adUstBoslugu: CGFloat
    let secili: Bool
    let kilitli: Bool
    let fiyatMetni: String
    let birimSimgeAdi: String

    var id: String { tipAdi }
}

/// 3. sekmedeki çerçeve kartı.
struct OzellestirmeCerceveKarti: Identifiable {
    /// FrameType adı.
    let tipAdi: String
    let gorselAdi: String
    let ad: String
    let secili: Bool
    let kilitli: Bool
    let fiyatMetni: String

    var id: String { tipAdi }
}

/// Bir renk satırı: örnek daire, etiket, ton ve parlaklık kaydırıcıları.
struct OzellestirmeRenkSatiri: Identifiable {
    let sira: Int
    let etiket: String
    var renk: Color
    /// Ton kaydırıcısı (0...359).
    var ton: Int
    /// Parlaklık kaydırıcısı (0...100).
    var parlaklik: Int
    /// Parlaklık kaydırıcısının zemini: siyah → tam renk → beyaz.
    var parlaklikGradyani: [Color]

    var id: Int { sira }
}

/// Önizleme abaküsündeki tek boncuk yuvası.
struct OzellestirmeYuva: Identifiable {
    let cubuk: Int
    let ustMu: Bool
    /// Alt boncuklarda 0...3 (0 = bariyere en yakın); üst boncukta 0.
    let sira: Int
    /// Boncuğun (seçili olmayan) resmi; görsel bulunamazsa nil.
    let resim: UIImage?

    var id: Int { cubuk * 10 + (ustMu ? 0 : sira + 1) }
}

/// Çerçevenin yuvarlatılmış dikdörtgen katmanı.
struct OzellestirmeCerceveKatmani: Identifiable {
    let id: Int
    /// Kenarlardan boşluk (ahşap çerçevede pt; gradyanlı çerçevede 400 birimlik tuvalde).
    let sol: CGFloat
    let ust: CGFloat
    let sag: CGFloat
    let alt: CGFloat
    let yaricap: CGFloat
    let renk: Color
    let gradyanMi: Bool
}

/// Çerçevenin nasıl çizileceği.
struct OzellestirmeCerceve {
    enum Tur {
        /// Ahşap: katmanlar kabın gerçek boyutunda, pt cinsinden kenar boşluklarıyla çizilir.
        case kenarKatmanlari
        /// Kalpli / yıldızlı: hazır resim kaba gerilerek çizilir.
        case resim
        /// Gradyanlı: katmanlar `tuvalKenari` birimlik kare tuvalde tanımlıdır, kaba gerilir.
        case tuvalKatmanlari
    }

    var tur: Tur = .kenarKatmanlari
    var resim: UIImage?
    var katmanlar: [OzellestirmeCerceveKatmani] = []
    var gradyanRenkleri: [Color] = []
    var gradyanDuraklari: [CGFloat] = []
    var tuvalKenari: CGFloat = 400
}

/// Önizleme abaküsünün ölçüleri (ortak kod: AbacusPreviewMetrics; dp → pt).
struct OzellestirmeOlculer {
    var cubukSayisi: Int = 5
    var cubukGenisligi: CGFloat = 45
    var cubukYuksekligi: CGFloat = 250
    var cizgiGenisligi: CGFloat = 5
    var cizgiRengi: Color = .black
    var boncukBoyu: CGFloat = 40
    var ustBoncukUstu: CGFloat = 0
    /// Alt boncukların üst kenarı (0 = bariyere en yakın).
    var altBoncukUstleri: [CGFloat] = [128, 152, 176, 200]
    var bariyerYuksekligi: CGFloat = 6
    var bariyerUstu: CGFloat = 72
    var yuvaPaneliBariyerYuksekligi: CGFloat = 16
    var yuvaPaneliBariyerUstu: CGFloat = 67
    var bariyerDisRengi: Color = .black
    var bariyerIcRengi: Color = .brown
    var bariyerIcBoslugu: CGFloat = 4
    var cerceveDikeyBosluk: CGFloat = 20
    var cerceveYatayBosluk: CGFloat = 24
}

/// Satın alma paneli (Android: panel_bead_purchase.xml; dört akış aynı paneli kullanır).
struct OzellestirmeSatinAlmaPaneli: Equatable {
    enum Tur: Equatable {
        case boncuk
        case boncukRenkOzelligi
        case cerceve
        case cerceveRenkOzelligi
    }

    let tur: Tur
    /// BeadType ya da FrameType adı.
    let tipAdi: String
    let gorselAdi: String
    let fiyatMetni: String
    let birimSimgeAdi: String
    /// Sağ alt köşedeki "renk özelliği" simgesi (color_ic) görünür mü.
    let ozellikSimgesiGorunur: Bool
}

/// Yuva seçme paneli (Android: panel_bead_skin_select.xml).
struct OzellestirmeYuvaPaneli {
    /// Yerleştirilecek boncuk tipinin adı.
    let tipAdi: String
    let envanterMetni: String
    let envanterResmi: UIImage?
    let fiyatMetni: String
    let birimSimgeAdi: String
    let dugmeRengi: Color
}

enum OzellestirmePanel: Equatable {
    case satinAlma(OzellestirmeSatinAlmaPaneli)
    case yuvaSecimi(tipAdi: String)
}

// MARK: - Görünüm modeli

final class OzellestirmeModeli: ObservableObject {
    // MARK: Yayınlanan durum

    @Published private(set) var altin: Int = 0
    @Published private(set) var anahtar: Int = 0
    /// Etkin sekme (0: boncuk tipi, 1: boncuk rengi, 2: çerçeve tipi, 3: çerçeve rengi).
    @Published private(set) var sekme: Int = 0

    @Published private(set) var yuvalar: [OzellestirmeYuva] = []
    @Published private(set) var cerceve = OzellestirmeCerceve()

    @Published private(set) var boncukKartlari: [OzellestirmeBoncukKarti] = []
    @Published private(set) var cerceveKartlari: [OzellestirmeCerceveKarti] = []

    @Published private(set) var normalBoncukResmi: UIImage?
    @Published private(set) var seciliBoncukResmi: UIImage?
    @Published private(set) var normalSatirlar: [OzellestirmeRenkSatiri] = []
    @Published private(set) var seciliSatirlar: [OzellestirmeRenkSatiri] = []
    @Published private(set) var boncukRenkKilitli: Bool = false

    @Published private(set) var cerceveSatirlari: [OzellestirmeRenkSatiri] = []
    @Published private(set) var cerceveRenkKilitli: Bool = false

    @Published private(set) var panel: OzellestirmePanel?
    @Published private(set) var yuvaPaneli: OzellestirmeYuvaPaneli?
    /// Satın alma sürerken düğmeler ve kapatma kilitlenir (Android: isEnabled = false, setCancelable(false)).
    @Published private(set) var panelMesgul: Bool = false
    @Published private(set) var bildirim: String?

    // MARK: Sabitler (ortak koddan bir kez okunur)

    let olculer: OzellestirmeOlculer
    let sekmeSimgeleri: [String]
    let seciliSekmeOpakligi: Double
    let digerSekmeOpakligi: Double
    let tonAzami: Int
    let parlaklikAzami: Int
    /// Ton kaydırıcısının zemini (gökkuşağı).
    let tonGradyani: [Color]
    let kartZemini: Color
    let kilitOrtusu: Color
    /// Seçili kartın çerçevesi (Android: #FFD600, 6 px ≈ 2 pt).
    let seciliCerceveRengi: Color
    let seciliCerceveKalinligi: CGFloat
    let renkOzelligiFiyatMetni: String
    let boncukTipiBasligi: String
    let cerceveTipiBasligi: String
    let yuvaPaneliBasligi: String
    let normalEtiketi: String
    let seciliEtiketi: String
    let tonEtiketi: String
    let parlaklikEtiketi: String
    let boncukSatirSayisi: Int
    let cerceveSatirSayisi: Int
    let satinAlmaPaneliGenislikOrani: CGFloat
    let yuvaPaneliGenislikOrani: CGFloat

    // MARK: Bağımlılıklar ve iç durum

    private let servisler: OzellestirmeBagimliliklar
    private var depo: AbacusPrefsStore { servisler.depo }
    private var kimlik: String? { servisler.kullaniciKimligi }

    /// Android: ownedBeads / ownedFrames.
    private var sahipBoncuklar: [String: BeadData] = [:]
    private var sahipCerceveler: [String: FrameData] = [:]
    /// 2. sekmenin açıldığı andaki genel boncuk tipi.
    private var renkSekmesiTipi: AbacusPreferences.BeadType?

    private var cuzdanDinleyicisi: UUID?
    private var esitlemeIsi: DispatchWorkItem?
    private var bildirimSirasi: Int = 0
    private var basladi: Bool = false

    init(servisler: OzellestirmeBagimliliklar) {
        self.servisler = servisler

        let ui = AbacusCustomizationUiLogic.shared
        let m = AbacusPreviewMetrics.shared
        var olcu = OzellestirmeOlculer()
        olcu.cubukSayisi = Int(m.ROD_COUNT)
        olcu.cubukGenisligi = CGFloat(m.ROD_WIDTH_DP)
        olcu.cubukYuksekligi = CGFloat(m.ROD_HEIGHT_DP)
        olcu.cizgiGenisligi = CGFloat(m.ROD_LINE_WIDTH_DP)
        olcu.cizgiRengi = OzellestirmeStil.renk(argb: m.ROD_LINE_COLOR)
        olcu.boncukBoyu = CGFloat(m.BEAD_SIZE_DP)
        olcu.ustBoncukUstu = CGFloat(m.topBeadTopDp())
        var altlar: [CGFloat] = []
        for i in 0..<Int(m.BOTTOM_BEADS_PER_ROD) {
            altlar.append(CGFloat(m.bottomBeadTopDp(index: Int32(i))))
        }
        olcu.altBoncukUstleri = altlar
        olcu.bariyerYuksekligi = CGFloat(m.BARRIER_HEIGHT_DP)
        olcu.bariyerUstu = CGFloat(m.barrierTopDp(barrierHeightDp: m.BARRIER_HEIGHT_DP))
        olcu.yuvaPaneliBariyerYuksekligi = CGFloat(m.SLOT_PANEL_BARRIER_HEIGHT_DP)
        olcu.yuvaPaneliBariyerUstu = CGFloat(m.barrierTopDp(barrierHeightDp: m.SLOT_PANEL_BARRIER_HEIGHT_DP))
        olcu.bariyerDisRengi = OzellestirmeStil.renk(argb: m.BARRIER_OUTER_COLOR)
        olcu.bariyerIcRengi = OzellestirmeStil.renk(argb: m.BARRIER_INNER_COLOR)
        olcu.bariyerIcBoslugu = CGFloat(m.BARRIER_INNER_INSET_DP)
        olcu.cerceveDikeyBosluk = CGFloat(m.FRAME_PADDING_VERTICAL_DP)
        olcu.cerceveYatayBosluk = CGFloat(m.FRAME_PADDING_HORIZONTAL_DP)
        olculer = olcu

        sekmeSimgeleri = ui.TAB_ICON_RES_NAMES
        seciliSekmeOpakligi = Double(ui.TAB_SELECTED_ALPHA)
        digerSekmeOpakligi = Double(ui.TAB_UNSELECTED_ALPHA)
        tonAzami = Int(ui.HUE_MAX)
        parlaklikAzami = Int(ui.LIGHTNESS_MAX)
        tonGradyani = ui.rainbowGradientColors().map { OzellestirmeStil.renk(argb: $0.int32Value) }
        kartZemini = OzellestirmeStil.renk(argb: ui.CARD_BACKGROUND_COLOR)
        kilitOrtusu = OzellestirmeStil.renk(argb: ui.LOCK_OVERLAY_COLOR)
        seciliCerceveRengi = OzellestirmeStil.renk(argb: AbacusCustomizationRules.shared.SELECTED_STROKE_COLOR)
        // Android kalınlığı ham piksel olarak verir; 3x yoğunlukta pt karşılığı üçte biridir.
        seciliCerceveKalinligi = CGFloat(AbacusCustomizationRules.shared.SELECTED_STROKE_WIDTH) / 3.0
        renkOzelligiFiyatMetni = String(AbacusCustomizationRules.shared.COLOR_FEATURE_PRICE)
        boncukTipiBasligi = ui.TITLE_BEAD_TYPE
        cerceveTipiBasligi = ui.TITLE_FRAME_TYPE
        yuvaPaneliBasligi = ui.TITLE_SLOT_PANEL
        normalEtiketi = ui.LABEL_NORMAL
        seciliEtiketi = ui.LABEL_SELECTED
        tonEtiketi = ui.LABEL_HUE
        parlaklikEtiketi = ui.LABEL_LIGHTNESS
        boncukSatirSayisi = Int(ui.BEAD_CARDS_PER_ROW)
        cerceveSatirSayisi = Int(ui.FRAME_CARDS_PER_ROW)
        satinAlmaPaneliGenislikOrani = CGFloat(ui.PURCHASE_PANEL_WIDTH_RATIO)
        yuvaPaneliGenislikOrani = CGFloat(ui.SLOT_PANEL_WIDTH_RATIO)

        let onbellek: MagazaCuzdan = servisler.cuzdan.cuzdan
        altin = onbellek.altin
        anahtar = onbellek.anahtar
        hepsiniYenile()
    }

    deinit {
        esitlemeIsi?.cancel()
        if let dinleyici = cuzdanDinleyicisi {
            servisler.cuzdan.dinlemeyiBirak(dinleyici)
        }
    }

    // MARK: Yaşam döngüsü

    /// Ekran göründü (Android: onCreateView). Cüzdan ve sahiplik kayıtları yüklenir.
    /// Android ayrıca başka cihazda kaydedilmiş özelleştirmeyi bir kez yerele uygular
    /// (AbacusCustomizationFirestore.ensureHydrated); Firebase bağlanınca burada yapılmalıdır:
    /// snapshotFromDocument → applySnapshotToLocalPreferences → hepsiniYenile.
    func basla() {
        if cuzdanDinleyicisi == nil {
            cuzdanDinleyicisi = servisler.cuzdan.dinle { [weak self] cuzdan in
                self?.cuzdaniYaz(cuzdan)
            }
        }
        cuzdaniYaz(servisler.cuzdan.cuzdan)
        // Ayarlar başka bir ekranda değişmiş olabilir.
        hepsiniYenile()
        if basladi { return }
        basladi = true
        servisler.cuzdan.yukle { [weak self] cuzdan in
            self?.cuzdaniYaz(cuzdan)
        }
        guard kimlik != nil else { return }
        servisler.sahiplik.boncuklariYukle { [weak self] kayitlar in
            guard let self = self else { return }
            var sozluk: [String: BeadData] = [:]
            for (ad, kayit) in kayitlar {
                sozluk[ad] = BeadData(count: Int32(kayit.adet), colorFeatureActive: kayit.renkOzelligi)
            }
            self.sahipBoncuklar = sozluk
            self.hepsiniYenile()
        }
        servisler.sahiplik.cerceveleriYukle { [weak self] kayitlar in
            guard let self = self else { return }
            var sozluk: [String: FrameData] = [:]
            for (ad, kayit) in kayitlar {
                sozluk[ad] = FrameData(owned: kayit.sahip, colorFeatureActive: kayit.renkOzelligi)
            }
            self.sahipCerceveler = sozluk
            self.hepsiniYenile()
        }
    }

    // MARK: Sekmeler

    /// Android: selectTab — sekme içeriği her seçimde yeniden kurulur.
    func sekmeSec(_ yeni: Int) {
        sekme = yeni
        sekmeIceriginiKur()
    }

    // MARK: 1. sekme — boncuk tipi

    func boncukKartinaDokun(_ tipAdi: String) {
        guard panel == nil, let tip = boncukTipi(tipAdi) else { return }
        let ui = AbacusCustomizationUiLogic.shared
        // Genel tip, satın alınmamış olsa da hemen yazılır (Android davranışı).
        let yuvaPaneliAcilir: Bool = ui.onBeadCardClicked(
            store: depo, uid: kimlik, ownedBeads: sahipBoncuklar, beadType: tip
        )
        hepsiniYenile()
        esitlemeyiZamanla()
        // Android: oturum yoksa panel açılmaz.
        guard kimlik != nil else { return }
        if yuvaPaneliAcilir {
            yuvaPaneliniAc(tip)
        } else {
            let kurallar = AbacusCustomizationRules.shared
            paneliAc(
                OzellestirmeSatinAlmaPaneli(
                    tur: .boncuk,
                    tipAdi: tip.name,
                    gorselAdi: kurallar.beadImageResName(beadType: tip),
                    fiyatMetni: String(kurallar.getBeadPrice(beadType: tip)),
                    birimSimgeAdi: kurallar.currencyIconResName(beadType: tip),
                    ozellikSimgesiGorunur: false
                )
            )
        }
    }

    // MARK: 2. sekme — boncuk rengi

    /// Kaydırıcı değişti (Android: onAnyChange).
    func boncukRengiDegisti(secili: Bool, sira: Int, ton: Int, parlaklik: Int) {
        guard let tip = renkSekmesiTipi else { return }
        let ui = AbacusCustomizationUiLogic.shared
        let yeniRenk: Int32 = ui.setBeadColorFromSliders(
            store: depo, beadType: tip, selected: secili,
            index: Int32(sira), hue: Int32(ton), lightness: Int32(parlaklik)
        )
        if secili {
            satiriGuncelle(&seciliSatirlar, sira: sira, ton: ton, parlaklik: parlaklik, renk: yeniRenk)
        } else {
            satiriGuncelle(&normalSatirlar, sira: sira, ton: ton, parlaklik: parlaklik, renk: yeniRenk)
        }
        basliklariYenile()
        onizlemeyiYenile()
        esitlemeyiZamanla()
    }

    /// Sütunun altındaki sıfırlama düğmesi.
    func boncukRenkleriniSifirla(secili: Bool) {
        guard let tip = renkSekmesiTipi else { return }
        AbacusPreferences.shared.resetBeadColors(store: depo, type: tip, selected: secili)
        if secili {
            seciliSatirlar = boncukSatirlari(tip, secili: true)
        } else {
            normalSatirlar = boncukSatirlari(tip, secili: false)
        }
        basliklariYenile()
        onizlemeyiYenile()
        esitlemeyiZamanla()
    }

    /// Boncuk renk özelliği fiyatı (Android: getBeadColorFeaturePrice); anahtarla satılan boncukta anahtar cinsinden.
    var boncukRenkFiyatMetni: String {
        guard let tip = renkSekmesiTipi else { return renkOzelligiFiyatMetni }
        return String(AbacusCustomizationRules.shared.getBeadColorFeaturePrice(beadType: tip))
    }

    /// Boncuk renk özelliği para birimi simgesi (boncukla aynı para birimi: "key" ya da "gold_ic").
    var boncukRenkParaBirimiSimgesi: String {
        guard let tip = renkSekmesiTipi else { return AbacusCustomizationRules.shared.ICON_GOLD }
        return AbacusCustomizationRules.shared.colorFeatureCurrencyIconResName(beadType: tip)
    }

    /// 2. sekmenin kilit örtüsüne dokunuldu: renk özelliği satın alma paneli.
    func boncukRenkKilidineDokun() {
        guard panel == nil, kimlik != nil, let tip = renkSekmesiTipi else { return }
        paneliAc(
            OzellestirmeSatinAlmaPaneli(
                tur: .boncukRenkOzelligi,
                tipAdi: tip.name,
                gorselAdi: AbacusCustomizationRules.shared.beadImageResName(beadType: tip),
                fiyatMetni: boncukRenkFiyatMetni,
                birimSimgeAdi: boncukRenkParaBirimiSimgesi,
                ozellikSimgesiGorunur: true
            )
        )
    }

    // MARK: 3. sekme — çerçeve tipi

    func cerceveKartinaDokun(_ tipAdi: String) {
        guard panel == nil, let tip = cerceveTipi(tipAdi) else { return }
        let ui = AbacusCustomizationUiLogic.shared
        if ui.onFrameCardClicked(store: depo, ownedFrames: sahipCerceveler, frameType: tip) {
            hepsiniYenile()
            esitlemeyiZamanla()
            return
        }
        guard kimlik != nil else { return }
        guard let gorsel = ui.frameColorFeatureImageResName(frameType: tip) else { return }
        paneliAc(
            OzellestirmeSatinAlmaPaneli(
                tur: .cerceve,
                tipAdi: tip.name,
                gorselAdi: gorsel,
                fiyatMetni: String(AbacusCustomizationRules.shared.getFramePrice(frameType: tip)),
                birimSimgeAdi: AbacusCustomizationRules.shared.ICON_GOLD,
                ozellikSimgesiGorunur: false
            )
        )
    }

    // MARK: 4. sekme — çerçeve rengi

    func cerceveRengiDegisti(sira: Int, ton: Int, parlaklik: Int) {
        let ui = AbacusCustomizationUiLogic.shared
        let yeniRenk: Int32 = ui.setFrameColorFromSliders(
            store: depo, index: Int32(sira), hue: Int32(ton), lightness: Int32(parlaklik)
        )
        satiriGuncelle(&cerceveSatirlari, sira: sira, ton: ton, parlaklik: parlaklik, renk: yeniRenk)
        onizlemeyiYenile()
        esitlemeyiZamanla()
    }

    /// Android: resetAllFrameColors — bütün çerçeve tiplerinin renkleri varsayılana döner.
    func cerceveRenkleriniSifirla() {
        AbacusPreferences.shared.resetAllFrameColors(store: depo)
        cerceveSatirlari = cerceveRenkSatirlari()
        onizlemeyiYenile()
        esitlemeyiZamanla()
    }

    /// 4. sekmenin kilit örtüsüne dokunuldu.
    func cerceveRenkKilidineDokun() {
        guard panel == nil, kimlik != nil else { return }
        let tip: AbacusPreferences.FrameType = AbacusPreferences.shared.getFrameType(store: depo)
        guard let gorsel = AbacusCustomizationUiLogic.shared.frameColorFeatureImageResName(frameType: tip) else {
            return
        }
        paneliAc(
            OzellestirmeSatinAlmaPaneli(
                tur: .cerceveRenkOzelligi,
                tipAdi: tip.name,
                gorselAdi: gorsel,
                fiyatMetni: renkOzelligiFiyatMetni,
                birimSimgeAdi: AbacusCustomizationRules.shared.ICON_GOLD,
                ozellikSimgesiGorunur: true
            )
        )
    }

    // MARK: Paneller

    /// Paneli kapatır (satın alma sürerken kapatılamaz).
    func paneliKapat() {
        if panelMesgul { return }
        panel = nil
        yuvaPaneli = nil
    }

    /// Satın alma panelindeki düğme.
    func satinAlmayiOnayla() {
        guard case .satinAlma(let bilgi)? = panel, !panelMesgul else { return }
        let ui = AbacusCustomizationUiLogic.shared
        let kurallar = AbacusCustomizationRules.shared
        let cuzdan: MagazaCuzdan = servisler.cuzdan.cuzdan

        switch bilgi.tur {
        case .boncuk:
            guard let tip = boncukTipi(bilgi.tipAdi) else { return }
            let denetim = ui.checkBeadPurchase(
                beadType: tip, cachedKeys: Int32(cuzdan.anahtar), cachedCurrency: Int32(cuzdan.altin)
            )
            let yeniAdet: Int32 = ui.beadCountAfterPurchase(
                ownedBeads: sahipBoncuklar, beadType: tip, fromSlotPanel: false
            )
            satinAl(
                denetim: denetim,
                urunKimligi: "bead_" + tip.name,
                kurallar: ui.BEAD_PURCHASE_PANEL,
                kaydet: { [weak self] tamam in
                    self?.servisler.sahiplik.boncukAdediniYaz(
                        boncukKimligi: tip.name, adet: Int(yeniAdet), tamam: tamam
                    )
                },
                basari: { [weak self] in
                    guard let self = self else { return }
                    self.sahipBoncuklar = kurallar.withBeadCount(
                        ownedBeads: self.sahipBoncuklar, beadType: tip, newCount: yeniAdet
                    )
                    // Android: panel kapanır, ardından yuva seçme paneli açılır.
                    self.panel = nil
                    self.hepsiniYenile()
                    self.yuvaPaneliniAc(tip)
                }
            )

        case .boncukRenkOzelligi:
            guard let tip = boncukTipi(bilgi.tipAdi) else { return }
            // Android: getBeadColorFeaturePrice — fiyat boncuğun para biriminde (ANIMAL3/ANIMAL8: 40 anahtar,
            // diğerleri 2000 altın); kayıt başarısız olursa harcanan iade edilir (BEAD_COLOR_FEATURE_PANEL).
            let denetim = ui.checkBeadColorFeaturePurchase(
                beadType: tip, cachedKeys: Int32(cuzdan.anahtar), cachedCurrency: Int32(cuzdan.altin)
            )
            satinAl(
                denetim: denetim,
                urunKimligi: "bead_color_" + tip.name,
                kurallar: ui.BEAD_COLOR_FEATURE_PANEL,
                kaydet: { [weak self] tamam in
                    self?.servisler.sahiplik.boncukRenkOzelliginiAc(boncukKimligi: tip.name, tamam: tamam)
                },
                basari: { [weak self] in
                    guard let self = self else { return }
                    self.sahipBoncuklar = kurallar.withBeadColorFeature(
                        ownedBeads: self.sahipBoncuklar, beadType: tip
                    )
                    self.panel = nil
                    self.hepsiniYenile()
                }
            )

        case .cerceve:
            guard let tip = cerceveTipi(bilgi.tipAdi) else { return }
            let denetim = ui.checkGoldPurchase(
                price: kurallar.getFramePrice(frameType: tip), cachedCurrency: Int32(cuzdan.altin)
            )
            satinAl(
                denetim: denetim,
                urunKimligi: "frame_" + tip.name,
                kurallar: ui.FRAME_PURCHASE_PANEL,
                kaydet: { [weak self] tamam in
                    self?.servisler.sahiplik.cerceveyiSahiplen(cerceveKimligi: tip.name, tamam: tamam)
                },
                basari: { [weak self] in
                    guard let self = self else { return }
                    // Sahiplik işaretlenir ve çerçeve seçili yapılır.
                    self.sahipCerceveler = ui.onFramePurchased(
                        store: self.depo, ownedFrames: self.sahipCerceveler, frameType: tip
                    )
                    self.esitlemeyiZamanla()
                    self.panel = nil
                    self.hepsiniYenile()
                }
            )

        case .cerceveRenkOzelligi:
            guard let tip = cerceveTipi(bilgi.tipAdi) else { return }
            let denetim = ui.checkGoldPurchase(
                price: kurallar.COLOR_FEATURE_PRICE, cachedCurrency: Int32(cuzdan.altin)
            )
            satinAl(
                denetim: denetim,
                urunKimligi: "frame_color_" + tip.name,
                kurallar: ui.FRAME_PURCHASE_PANEL,
                kaydet: { [weak self] tamam in
                    self?.servisler.sahiplik.cerceveRenkOzelliginiAc(cerceveKimligi: tip.name, tamam: tamam)
                },
                basari: { [weak self] in
                    guard let self = self else { return }
                    self.sahipCerceveler = kurallar.withFrameColorFeature(
                        ownedFrames: self.sahipCerceveler, frameType: tip
                    )
                    self.panel = nil
                    self.hepsiniYenile()
                }
            )
        }
    }

    /// Yuva panelindeki "5 tane daha al" düğmesi.
    func yuvaPanelindenSatinAl() {
        guard case .yuvaSecimi(let tipAdi)? = panel, !panelMesgul, let tip = boncukTipi(tipAdi) else { return }
        let ui = AbacusCustomizationUiLogic.shared
        let kurallar = AbacusCustomizationRules.shared
        let cuzdan: MagazaCuzdan = servisler.cuzdan.cuzdan
        let denetim = ui.checkSlotPanelPurchase(
            ownedBeads: sahipBoncuklar, beadType: tip,
            cachedKeys: Int32(cuzdan.anahtar), cachedCurrency: Int32(cuzdan.altin)
        )
        let yeniAdet: Int32 = ui.beadCountAfterPurchase(
            ownedBeads: sahipBoncuklar, beadType: tip, fromSlotPanel: true
        )
        satinAl(
            denetim: denetim,
            urunKimligi: "bead_" + tip.name,
            kurallar: ui.SLOT_PANEL_PURCHASE,
            kaydet: { [weak self] tamam in
                self?.servisler.sahiplik.boncukAdediniYaz(
                    boncukKimligi: tip.name, adet: Int(yeniAdet), tamam: tamam
                )
            },
            basari: { [weak self] in
                guard let self = self else { return }
                self.sahipBoncuklar = kurallar.withBeadCount(
                    ownedBeads: self.sahipBoncuklar, beadType: tip, newCount: yeniAdet
                )
                self.hepsiniYenile()
            }
        )
    }

    /// Yuva panelinde bir boncuğa dokunuldu (Android: placeBead).
    func yuvayaYerlestir(cubuk: Int, ustMu: Bool, sira: Int) {
        guard case .yuvaSecimi(let tipAdi)? = panel, let tip = boncukTipi(tipAdi) else { return }
        let sonuc = AbacusCustomizationUiLogic.shared.placeBeadInSlot(
            ownedBeads: sahipBoncuklar, store: depo, uid: kimlik, beadType: tip,
            rod: Int32(cubuk), isTop: ustMu, beadIndex: Int32(sira)
        )
        if let metin = sonuc.message {
            bildir(metin)
        }
        if sonuc.placed {
            hepsiniYenile()
            esitlemeyiZamanla()
        }
    }

    // MARK: Satın alma akışı

    /// Dört satın alma akışının ortak iskeleti: ön denetim → cüzdandan düş → sahipliği kaydet.
    /// Kayıt başarısız olursa ve akışın kuralı izin veriyorsa harcama geri alınır.
    private func satinAl(
        denetim: AbacusCustomizationUiLogic.PurchaseCheck,
        urunKimligi: String,
        kurallar: AbacusCustomizationUiLogic.PurchasePanelRules,
        kaydet: @escaping (@escaping (Bool) -> Void) -> Void,
        basari: @escaping () -> Void
    ) {
        panelMesgul = true
        if !denetim.allowed {
            if let metin = denetim.message {
                bildir(metin)
            }
            panelMesgul = false
            return
        }
        let fiyat: Int = Int(denetim.price)
        let anahtarlaMi: Bool = denetim.isKeyCurrency
        let gerekceler = WalletReason.shared
        servisler.cuzdan.degistir(
            altinFarki: anahtarlaMi ? 0 : -fiyat,
            anahtarFarki: anahtarlaMi ? -fiyat : 0,
            gerekce: gerekceler.SPEND,
            urunKimligi: urunKimligi,
            geriAlmaJetonu: nil
        ) { [weak self] degisim in
            guard let self = self else { return }
            guard let degisim = degisim else {
                self.bildir(kurallar.walletFailureMessage)
                self.panelMesgul = false
                return
            }
            self.cuzdaniYaz(degisim.cuzdan)
            kaydet { [weak self] kaydedildi in
                guard let self = self else { return }
                if kaydedildi {
                    self.panelMesgul = false
                    basari()
                    return
                }
                self.bildir(kurallar.saveFailureMessage)
                if kurallar.rollbackOnSaveFailure {
                    self.servisler.cuzdan.degistir(
                        altinFarki: anahtarlaMi ? 0 : fiyat,
                        anahtarFarki: anahtarlaMi ? fiyat : 0,
                        gerekce: gerekceler.PURCHASE_ROLLBACK,
                        urunKimligi: nil,
                        geriAlmaJetonu: degisim.geriAlmaJetonu
                    ) { [weak self] iade in
                        if let iade = iade {
                            self?.cuzdaniYaz(iade.cuzdan)
                        }
                    }
                }
                self.panelMesgul = false
            }
        }
    }

    private func paneliAc(_ bilgi: OzellestirmeSatinAlmaPaneli) {
        yuvaPaneli = nil
        panel = .satinAlma(bilgi)
    }

    private func yuvaPaneliniAc(_ tip: AbacusPreferences.BeadType) {
        panel = .yuvaSecimi(tipAdi: tip.name)
        yuvaPaneliniYenile()
    }

    // MARK: Yenileme

    /// Ekranda görünen her şeyi yerel ayarlardan ve sahiplik kayıtlarından yeniden kurar.
    private func hepsiniYenile() {
        onizlemeyiYenile()
        sekmeIceriginiKur()
        yuvaPaneliniYenile()
    }

    private func onizlemeyiYenile() {
        let ui = AbacusCustomizationUiLogic.shared
        var yeniYuvalar: [OzellestirmeYuva] = []
        for yuva in ui.slotBeads(store: depo, uid: kimlik) {
            yeniYuvalar.append(
                OzellestirmeYuva(
                    cubuk: Int(yuva.rod),
                    ustMu: yuva.isTop,
                    sira: Int(yuva.beadIndex),
                    resim: boncukResmi(yuva.beadType, secili: false)
                )
            )
        }
        yuvalar = yeniYuvalar
        cerceve = cerceveyiKur()
    }

    private func sekmeIceriginiKur() {
        let ui = AbacusCustomizationUiLogic.shared
        switch sekme {
        case Int(ui.TAB_BEAD_TYPE):
            boncukKartlari = ui.beadCardStates(store: depo, uid: kimlik, ownedBeads: sahipBoncuklar).map { durum in
                OzellestirmeBoncukKarti(
                    tipAdi: durum.card.beadType.name,
                    gorselAdi: durum.card.imageResName,
                    ad: durum.card.label,
                    gorselGenisligi: CGFloat(durum.card.imageWidthDp),
                    gorselYuksekligi: CGFloat(durum.card.imageHeightDp),
                    adUstBoslugu: CGFloat(durum.card.labelMarginTopDp),
                    secili: durum.selected,
                    kilitli: durum.locked,
                    fiyatMetni: String(durum.price),
                    birimSimgeAdi: durum.currencyIconResName
                )
            }
        case Int(ui.TAB_BEAD_COLOR):
            let tip: AbacusPreferences.BeadType = AbacusPreferences.shared.getBeadType(store: depo, uid: kimlik)
            renkSekmesiTipi = tip
            normalSatirlar = boncukSatirlari(tip, secili: false)
            seciliSatirlar = boncukSatirlari(tip, secili: true)
            boncukRenkKilitli = ui.isBeadColorLocked(ownedBeads: sahipBoncuklar, beadType: tip)
            basliklariYenile()
        case Int(ui.TAB_FRAME_TYPE):
            cerceveKartlari = ui.frameCardStates(store: depo, ownedFrames: sahipCerceveler).map { durum in
                OzellestirmeCerceveKarti(
                    tipAdi: durum.card.frameType.name,
                    gorselAdi: durum.card.imageResName,
                    ad: durum.card.label,
                    secili: durum.selected,
                    kilitli: durum.locked,
                    fiyatMetni: String(durum.card.price)
                )
            }
        default:
            cerceveSatirlari = cerceveRenkSatirlari()
            cerceveRenkKilitli = ui.isFrameColorLocked(store: depo, ownedFrames: sahipCerceveler)
        }
    }

    /// 2. sekmenin başındaki iki boncuk resmi (Android: refreshHeaders).
    private func basliklariYenile() {
        guard let tip = renkSekmesiTipi else { return }
        normalBoncukResmi = boncukResmi(tip, secili: false)
        seciliBoncukResmi = boncukResmi(tip, secili: true)
    }

    private func yuvaPaneliniYenile() {
        guard case .yuvaSecimi(let tipAdi)? = panel, let tip = boncukTipi(tipAdi) else {
            yuvaPaneli = nil
            return
        }
        let ui = AbacusCustomizationUiLogic.shared
        let durum = ui.slotPanelState(ownedBeads: sahipBoncuklar, store: depo, uid: kimlik, beadType: tip)
        let dugmeRengi: Color = durum.buyButtonMaxed
            ? OzellestirmeStil.renk(argb: ui.BUY_BUTTON_MAXED_COLOR)
            : Color("button_enabled")
        yuvaPaneli = OzellestirmeYuvaPaneli(
            tipAdi: tip.name,
            envanterMetni: durum.inventoryText,
            envanterResmi: boncukResmi(tip, secili: false),
            fiyatMetni: String(durum.price),
            birimSimgeAdi: durum.currencyIconResName,
            dugmeRengi: dugmeRengi
        )
    }

    // MARK: Renk satırları

    private func boncukSatirlari(_ tip: AbacusPreferences.BeadType, secili: Bool) -> [OzellestirmeRenkSatiri] {
        let ui = AbacusCustomizationUiLogic.shared
        return ui.beadColorRows(store: depo, beadType: tip, selected: secili).map { satiraCevir($0) }
    }

    private func cerceveRenkSatirlari() -> [OzellestirmeRenkSatiri] {
        AbacusCustomizationUiLogic.shared.frameColorRows(store: depo).map { satiraCevir($0) }
    }

    private func satiraCevir(_ satir: AbacusCustomizationUiLogic.ColorRow) -> OzellestirmeRenkSatiri {
        OzellestirmeRenkSatiri(
            sira: Int(satir.index),
            etiket: satir.label,
            renk: OzellestirmeStil.renk(argb: satir.color),
            ton: Int(satir.hue),
            parlaklik: Int(satir.lightness),
            parlaklikGradyani: parlaklikGradyani(ton: Int(satir.hue))
        )
    }

    private func parlaklikGradyani(ton: Int) -> [Color] {
        AbacusCustomizationUiLogic.shared.lightnessGradientColors(hue: Int32(ton)).map {
            OzellestirmeStil.renk(argb: $0.int32Value)
        }
    }

    /// Kaydırıcıların konumu kullanıcının bıraktığı yerde kalır; renkten geri hesaplanmaz
    /// (Android'de de SeekBar konumu sekme yeniden kurulana kadar değişmez).
    private func satiriGuncelle(
        _ satirlar: inout [OzellestirmeRenkSatiri],
        sira: Int,
        ton: Int,
        parlaklik: Int,
        renk: Int32
    ) {
        guard let konum = satirlar.firstIndex(where: { $0.sira == sira }) else { return }
        var satir: OzellestirmeRenkSatiri = satirlar[konum]
        if satir.ton != ton {
            satir.parlaklikGradyani = parlaklikGradyani(ton: ton)
        }
        satir.ton = ton
        satir.parlaklik = parlaklik
        satir.renk = OzellestirmeStil.renk(argb: renk)
        satirlar[konum] = satir
    }

    // MARK: Resimler

    /// Bir boncuk tipinin kullanıcı renkleriyle boyanmış resmi (Android: buildBeadForType).
    private func boncukResmi(_ tip: AbacusPreferences.BeadType, secili: Bool) -> UIImage? {
        let ui = AbacusCustomizationUiLogic.shared
        let boya = ui.beadPaint(store: depo, beadType: tip, selected: secili)
        let yerelDepo: AbacusPrefsStore = self.depo
        return OzellestirmeBoyaci.onbellekten("boncuk_" + boya.cacheKey) {
            let renkler: [UIColor] = boya.colors.map { OzellestirmeStil.uiRenk(argb: $0.int32Value) }
            if boya.kind == "TINTED_LAYERS" {
                return OzellestirmeBoyaci.katmanliResim(gorselAdlari: boya.resNames, renkler: renkler)
            }
            if boya.kind == "GRADIENT_PILL" {
                let g = ui.pillGeometry()
                return OzellestirmeBoyaci.hapResmi(
                    boyut: CGSize(width: CGFloat(g.width), height: CGFloat(g.height)),
                    disYaricap: CGSize(width: CGFloat(g.outerRadiusX), height: CGFloat(g.outerRadiusY)),
                    bosluk: CGSize(width: CGFloat(g.padX), height: CGFloat(g.padY)),
                    icYaricap: CGSize(width: CGFloat(g.innerRadiusX), height: CGFloat(g.innerRadiusY)),
                    renkler: renkler,
                    duraklar: g.stops.map { CGFloat($0.floatValue) }
                )
            }
            // REPLACED_BITMAP: renkler varsayılansa kaynak görsel olduğu gibi kullanılır.
            guard let gorselAdi = boya.resNames.first else { return nil }
            if boya.isDefaultColors {
                return OzellestirmeBoyaci.hamResim(gorselAdi)
            }
            return OzellestirmeBoyaci.pikselleriIslenmisResim(
                gorselAdi: gorselAdi,
                genislik: OzellestirmeStil.boncukPikselGenisligi,
                yukseklik: nil
            ) { pikseller, en, boy in
                ui.recolorBeadPixels(
                    store: yerelDepo, beadType: tip, selected: secili,
                    pixels: pikseller, width: en, height: boy
                )
            }
        }
    }

    /// Kayıtlı çerçevenin çizim tarifi (Android: AbacusFrameRenderer.buildFrameDrawable).
    private func cerceveyiKur() -> OzellestirmeCerceve {
        let ui = AbacusCustomizationUiLogic.shared
        let boya = ui.framePaint(store: depo, frameType: nil)
        var sonuc = OzellestirmeCerceve()
        sonuc.tuvalKenari = CGFloat(boya.canvasSize)
        sonuc.gradyanRenkleri = boya.colors.map { OzellestirmeStil.renk(argb: $0.int32Value) }
        sonuc.gradyanDuraklari = boya.gradientStops.map { CGFloat($0.floatValue) }

        if boya.kind == ui.FRAME_KIND_INSET_LAYERS {
            sonuc.tur = .kenarKatmanlari
            sonuc.katmanlar = katmanlaraCevir(boya.insetLayers, tuval: nil)
        } else if boya.kind == ui.FRAME_KIND_CANVAS_LAYERS {
            sonuc.tur = .tuvalKatmanlari
            sonuc.katmanlar = katmanlaraCevir(boya.canvasLayers, tuval: CGFloat(boya.canvasSize))
        } else {
            sonuc.tur = .resim
            guard let gorselAdi = boya.resName else { return sonuc }
            let tip: AbacusPreferences.FrameType = boya.frameType
            let kenar: Int = Int(boya.canvasSize)
            let yerelDepo: AbacusPrefsStore = self.depo
            sonuc.resim = OzellestirmeBoyaci.onbellekten("cerceve_" + boya.cacheKey) {
                OzellestirmeBoyaci.pikselleriIslenmisResim(
                    gorselAdi: gorselAdi, genislik: kenar, yukseklik: kenar
                ) { pikseller, _, _ in
                    ui.recolorFramePixels(store: yerelDepo, frameType: tip, pixels: pikseller)
                }
            }
        }
        return sonuc
    }

    /// Ortak koddaki katmanları kenar boşluklarına çevirir. `tuval` verilirse katmanın sağ ve
    /// alt değerleri tuval koordinatıdır; verilmezse dört değer de doğrudan kenar boşluğudur.
    private func katmanlaraCevir(
        _ katmanlar: [AbacusCustomizationUiLogic.FrameLayer],
        tuval: CGFloat?
    ) -> [OzellestirmeCerceveKatmani] {
        var sonuc: [OzellestirmeCerceveKatmani] = []
        for (sira, katman) in katmanlar.enumerated() {
            var sag: CGFloat = CGFloat(katman.right)
            var alt: CGFloat = CGFloat(katman.bottom)
            if let kenar = tuval {
                sag = kenar - sag
                alt = kenar - alt
            }
            sonuc.append(
                OzellestirmeCerceveKatmani(
                    id: sira,
                    sol: CGFloat(katman.left),
                    ust: CGFloat(katman.top),
                    sag: sag,
                    alt: alt,
                    yaricap: CGFloat(katman.radius),
                    renk: OzellestirmeStil.renk(argb: katman.color),
                    gradyanMi: katman.isGradient
                )
            )
        }
        return sonuc
    }

    // MARK: Yardımcılar

    private func boncukTipi(_ ad: String) -> AbacusPreferences.BeadType? {
        AbacusPreferences.shared.beadTypeFromName(name: ad)
    }

    private func cerceveTipi(_ ad: String) -> AbacusPreferences.FrameType? {
        AbacusPreferences.shared.frameTypeFromName(name: ad)
    }

    private func cuzdaniYaz(_ cuzdan: MagazaCuzdan) {
        altin = cuzdan.altin
        anahtar = cuzdan.anahtar
    }

    /// Değişiklikleri gecikmeli olarak sunucuya yazar (Android: scheduleFirestoreSync, 1 sn).
    private func esitlemeyiZamanla() {
        guard kimlik != nil else { return }
        esitlemeIsi?.cancel()
        let is_ = DispatchWorkItem { [weak self] in
            guard let self = self else { return }
            let kopru = AbacusCustomizationFirestore.shared
            let goruntu = kopru.buildSnapshotFromLocalPreferences(store: self.depo, uid: self.kimlik)
            self.servisler.sahiplik.ozellestirmeyiKaydet(veri: kopru.customizationData(snapshot: goruntu))
        }
        esitlemeIsi = is_
        let gecikme: Double = Double(AbacusCustomizationUiLogic.shared.FIRESTORE_SYNC_DEBOUNCE_MS) / 1000.0
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme, execute: is_)
    }

    private func bildir(_ metin: String) {
        bildirimSirasi += 1
        let sira: Int = bildirimSirasi
        bildirim = metin
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self, self.bildirimSirasi == sira else { return }
            self.bildirim = nil
        }
    }
}
