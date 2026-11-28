import Foundation
import Shared
import SwiftUI

// Harita ekranının görünüm modeli.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyadadır; görünümler yalnızca aşağıdaki
// Swift yapılarını (HaritaOgesi, HaritaPanelDurumu, ...) okur. Böylece Kotlin → Swift
// adlandırmasından doğabilecek bir derleme hatası tek dosyada aranır.
//
// Android karşılıkları (güncel tasarım): MapFragment + LessonAdapter (alt alta ders kartları,
// karta bağlı ders paneli `lesson_popover`, anahtarla kilit açma), PartSelectionFragment +
// PartCardAdapter (kısım seçimi). Kurallar ortak koddan gelir: LessonAdapterLogic,
// PartSelectionLogic, GlobalLessonData.

// MARK: - Görünümlerin okuduğu veri yapıları

/// Listedeki satırın türü (Android: LessonItem.TYPE_*).
enum HaritaOgeTuru {
    case ders
    case baslik
    case sandik
    case yaris
}

/// Kartın ve panelin durum rengi (ortak kod: LessonAdapterLogic.CardState / PanelState):
/// kilitli gri, açık mavi, bitmiş altın.
enum HaritaKartDurumu {
    case kilitli
    case acik
    case bitti
}

/// Sandık kartındaki yıldız satırı (ortak kod: LessonAdapterLogic.ChestCardStars).
struct HaritaYildizDurumu {
    /// Kazanılan yıldız (0...3).
    let kazanilan: Int
    /// Bitmiş (altın) kartın yıldızları: koyu kahveye boyanır.
    let bitti: Bool
    /// Kazanılmamış yıldızın opaklığı.
    let bosOpaklik: Double
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
    /// Kartın şu anki durumu.
    let durum: HaritaKartDurumu
    /// true: kart bu kez mavi görünüp altına dönecek (Android: playFinalGoldMergeAnimation).
    let altinGecisi: Bool
    /// Sol ikonun görsel adı (ortak kod: cardIconName).
    let simge: String
    /// Adım çubuğunun dilim sayısı ve dolu dilim sayısı.
    let dilimSayisi: Int
    let doluDilim: Int
    /// Sandıkta yıldızlar: altına dönmeden önceki ve sonraki görünüm (derste nil).
    let yildizOnce: HaritaYildizDurumu?
    let yildizSonra: HaritaYildizDurumu?
    /// Kart hafifçe büyüyüp küçülür mü ("sıradaki ders").
    let nefes: Bool
    /// Yalnızca yarış bayrağında: kartın renk kaynağı adı.
    let yarisRengi: String
}

/// Derse dokununca karta bağlı açılan panelin içeriği (ortak kod: LessonAdapterLogic.LessonBottomSheetState).
struct HaritaPanelDurumu: Identifiable {
    /// Kartın listedeki sırası.
    let id: Int
    let oge: HaritaOgesi
    let baslik: String
    /// Üst etiket ("DERS" / "ÜNİTE MARATONU").
    let turMetni: String
    /// Çerçeve, etiket ve adım çubuğu rengi (Android: PanelState).
    let renk: HaritaKartDurumu
    let simge: String
    /// Adım satırı (dilimli çubuk + "2/5"): sandıkta ve kilitliyken gizli.
    let adimGorunur: Bool
    let doluAdim: Int
    let adimSayisi: Int
    /// Kilitli derste açıklama; boşsa satır gizli.
    let aciklama: String
    let dugmeMetni: String
    let dugmeEtkin: Bool
    /// Kilitli ama 1 anahtarla açılabilir: düğme açık mavisinde, anahtar simgeli.
    let anahtarlaAcilir: Bool
    let egitimTekrarGorunur: Bool
    /// Bitmiş sandığın rekor satırı; nil ise satır gizli.
    let rekorMetni: String?
    let rekorTiklanir: Bool
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
    /// Android assets içindeki Lottie dosyasının UZANTISIZ adı (`LottieGorunumu` bu biçimi ister).
    let animasyon: String
    /// Android: lottieView.repeatCount (kısım 2'de 2 → art arda 3 kez oynar; diğerlerinde 0).
    let animasyonTekrari: Int
}

/// Kısım seçim ekranındaki bir satır ("Toplama", "Çıkarma", "Çarpma").
struct HaritaKisimSatiri: Identifiable {
    let id: Int
    let baslik: String
    let kartlar: [HaritaKisimKarti]
}

/// Haritanın dışarı verdiği hedefin türü. Hedefi alan taraf (Yonetim/YonetimDersAkisi) hangi
/// ekranı açacağına YALNIZCA bu alana bakarak karar verir; `aciklama` / `ayrintilar` metinleri
/// sadece yer tutucuda gösterilir.
enum HaritaHedefTuru {
    /// AbacusFragment (ortak kural: LessonStartTarget.ABACUS).
    case abakusDersi
    /// BlindingLessonFragment (ortak kural: LessonStartTarget.BLINDING_LESSON).
    case korDers
    /// TutorialFragment; dersin öğreticisi henüz bitmemiş (ortak kural: LessonStartTarget.TUTORIAL).
    case ogretici
    /// TutorialFragment; paneldeki "Eğitimi tekrarla" ile istendi.
    case ogreticiTekrari
    /// Yarış paneli (yarış bayrağı ya da kısım 7 / 8 kartı).
    case yarisPaneli
    /// RecordFragment (bitmiş sandığın rekor satırı).
    case liderlikTablosu
    /// Mağaza; enerji ya da anahtar yetmedi (Android: openShopFragment).
    case magaza
}

/// Haritadan açılması istenen ekran. Yer tutucu da içeriğini buradan alır.
struct HaritaHedefi: Identifiable {
    let id = UUID()
    let tur: HaritaHedefTuru
    let baslik: String
    let aciklama: String
    let ayrintilar: [String]
    /// Yalnızca `.yarisPaneli`: açılacak yarış kısmı (Android: item.racePartId; 7 ya da 8).
    var yarisPartId: Int32? = nil
    /// Yalnızca `.yarisPaneli`: kısım seçiminden mi açıldı (Android: isFromPartSelection).
    var yarisPartSecimindenMi: Bool = false
    /// Yalnızca `.yarisPaneli`: haritadaki bayraktan açıldıysa kapanınca dönülecek kısım (Android: item.backRaceId).
    var yarisGeriPartId: Int32? = nil
}

/// Kısa süreli bilgi balonu. `uzun`: Android LockedHint (Snackbar, LENGTH_LONG, 5 satıra kadar);
/// değilse Toast (LENGTH_SHORT).
struct HaritaBildirimi: Identifiable, Equatable {
    let id: Int
    let metin: String
    let uzun: Bool
}

/// Ders ilerlemesinin kaynağı. Firestore yokken yönetimin bellekteki deposu
/// (Yonetim/YonetimIlerlemeDeposu) uygular; harita tek başına açıldığında (ekran galerisi)
/// `HaritaBellekIlerlemeKaynagi` kullanılır.
protocol HaritaIlerlemeKaynagi: AnyObject {
    /// Kısmın güncel listesi; nil dönerse ortak koddaki varsayılan şablon kullanılır.
    /// Android: GlobalLessonData.loadLessonItemsForPart (`users/{uid}/lessonProgress/{partId}/items`).
    func bulutOgeleri(partId: Int32) -> [LessonItem]?
    /// Bir dersi yerine yazar (Android: LessonManager.updateLessonItem(context, position, item) →
    /// `users/{uid}/lessonProgress/{partId}/items/{stableId}`).
    func dersiYaz(partId: Int32, sira: Int32, oge: LessonItem)
}

/// Bellekte tutulan sahte ilerleme (yönetici bağlı değilken).
final class HaritaBellekIlerlemeKaynagi: HaritaIlerlemeKaynagi {
    private var partlar: [Int32: [LessonItem]] = [:]

    func bulutOgeleri(partId: Int32) -> [LessonItem]? {
        return partlar[partId]
    }

    func dersiYaz(partId: Int32, sira: Int32, oge: LessonItem) {
        var liste: [LessonItem] = partlar[partId] ?? GlobalLessonData.shared.createLessonItems(partId: partId)
        let i: Int = Int(sira)
        if i >= 0 && i < liste.count {
            liste[i] = oge
        }
        partlar[partId] = liste
    }
}

// MARK: - Görünüm modeli

final class HaritaModeli: ObservableObject {
    /// Açık kısım; nil ise kısım seçim ekranı gösterilir.
    @Published private(set) var seciliPart: Int32? = nil
    @Published private(set) var ogeler: [HaritaOgesi] = []
    @Published private(set) var kisimSatirlari: [HaritaKisimSatiri] = []
    /// Açık ders paneli (Android: lesson_popover, LessonPanel.TAG).
    @Published private(set) var panel: HaritaPanelDurumu? = nil
    /// Anahtar harcaması sunucudayken düğme kapalı (Android: keyUnlockInProgress).
    @Published private(set) var anahtarIslemde: Bool = false
    @Published private(set) var bildirim: HaritaBildirimi? = nil
    @Published var hedef: HaritaHedefi? = nil

    // Bu aşamada giriş ve Firebase yok: aşağıdakiler sabit varsayılanlardır.
    // Giriş/cüzdan bağlanınca dışarıdan atanır, ardından `yenile()` çağrılır.
    var plan: String = "Free"
    var ogretmenOnayli: Bool = false
    var sonsuzEnerji: Bool = false
    var enerjiYeterli: Bool = true
    weak var ilerlemeKaynagi: HaritaIlerlemeKaynagi?
    /// Anahtar harcaması (Android: MainActivity.spendKeysConfirmed → UserWalletFirestore.applyKeyDelta,
    /// `users/{uid}.keys`). Varsayılan: mağazanın paylaşılan sahte cüzdanı (üst çubuk da onu gösterir).
    var cuzdan: MagazaCuzdanServisi = MagazaBagimliliklar.sahte.cuzdan

    private let yedekKaynak = HaritaBellekIlerlemeKaynagi()
    /// Android: playedFinalGoldAnimationKeys.
    private var altinOynananlar: Set<String> = []
    private var bildirimSayaci: Int = 0

    private let mantik = LessonAdapterLogic.shared
    private let kisimMantigi = PartSelectionLogic.shared

    init() {
        kisimlariYenile()
    }

    private var kaynak: HaritaIlerlemeKaynagi {
        return ilerlemeKaynagi ?? yedekKaynak
    }

    /// Android: GlobalLessonData.globalPartId.
    private var aktifPart: Int32 {
        return seciliPart ?? GlobalLessonData.shared.globalPartId
    }

    // MARK: Veri

    /// Kısmın ham listesi: kaynakta varsa o, yoksa ortak koddaki şablon.
    private func hamListe(_ partId: Int32) -> [LessonItem] {
        let bulut: [LessonItem]? = kaynak.bulutOgeleri(partId: partId)
        return GlobalLessonData.shared.loadLessonItemsForPart(partId: partId, cloudItems: bulut)
    }

    /// Haritada gösterilen liste. Android haritası GlobalLessonData.lessonItems'ı gösterir; o liste
    /// her yüklemede withDerivedUnlocks'tan geçer (önceki ders bitince sonraki açık sayılır).
    private func haritaListesi(_ partId: Int32) -> [LessonItem] {
        // Kotlin MutableList dönüşü Swift'e NSMutableArray olarak gelir; tip yazılmadan alınıp süzülür.
        let turetilmis = GlobalLessonData.shared.withDerivedUnlocks(partId: partId, items: hamListe(partId))
        var sonuc: [LessonItem] = []
        for eleman in turetilmis {
            if let oge = eleman as? LessonItem {
                sonuc.append(oge)
            }
        }
        return sonuc
    }

    /// Dışarıdan ilerleme değiştiğinde (ders bitti, ileride Firestore dinleyicisi) çağrılır.
    func yenile() {
        kisimlariYenile()
        if let partId = seciliPart {
            listeyiKur(partId: partId)
        }
    }

    private func listeyiKur(partId: Int32) {
        let liste: [LessonItem] = haritaListesi(partId)
        var yeni: [HaritaOgesi] = []
        yeni.reserveCapacity(liste.count)
        for (sira, oge) in liste.enumerated() {
            yeni.append(ogeKur(oge, sira: sira, partId: partId))
        }
        ogeler = yeni
    }

    private func ogeTuru(_ oge: LessonItem) -> HaritaOgeTuru {
        let sabitler = LessonItem.companion
        let turKodu: Int32 = oge.type
        if turKodu == sabitler.TYPE_HEADER {
            return .baslik
        }
        if turKodu == sabitler.TYPE_CHEST {
            return .sandik
        }
        if turKodu == sabitler.TYPE_RACE {
            return .yaris
        }
        return .ders
    }

    private func ogeKur(_ oge: LessonItem, sira: Int, partId: Int32) -> HaritaOgesi {
        let tur: HaritaOgeTuru = ogeTuru(oge)
        if tur == .baslik || tur == .yaris {
            let kilitsiz: Bool = tur == .yaris ? mantik.raceFlagClickAllowed(item: oge) : true
            let renk: String = tur == .yaris ? mantik.raceCardColor(item: oge) : ""
            return HaritaOgesi(
                id: sira, oge: oge, tur: tur, baslik: oge.title, kilitsiz: kilitsiz,
                durum: kilitsiz ? .acik : .kilitli, altinGecisi: false, simge: "",
                dilimSayisi: 1, doluDilim: 0, yildizOnce: nil, yildizSonra: nil, nefes: false,
                yarisRengi: renk
            )
        }

        // Ders ve sandık kartı (Android: LessonViewHolder.bind).
        let dilimler = mantik.stepSegments(item: oge)
        let anahtar: String = mantik.lessonProgressKey(item: oge, adapterPosition: Int32(sira))
        let oynandi: Bool = altinOynananlar.contains(anahtar)
        let karar = mantik.progressAnimationDecision(
            item: oge,
            goldAnimationPlayed: oynandi,
            pendingFromFilled: nil,
            canConsumePending: false,
            lastSeenFilled: nil
        )
        let eylem: String = karar.action.name
        let altinGecisi: Bool = eylem == "SHOW_TARGET_AND_PLAY_GOLD"
        let altinTamam: Bool = oynandi || altinGecisi || eylem == "APPLY_PERSISTENT_GOLD"
        if altinTamam {
            altinOynananlar.insert(anahtar)
        }
        let durum: HaritaKartDurumu = kartDurumu(mantik.cardState(item: oge, goldAnimationPlayed: altinTamam).name)
        let nefes: Bool = mantik.shouldStartBreathingAnimation(item: oge, goldAnimationPlayed: altinTamam)

        var yildizOnce: HaritaYildizDurumu? = nil
        var yildizSonra: HaritaYildizDurumu? = nil
        if tur == .sandik {
            yildizOnce = yildizDurumu(oge, altin: false)
            yildizSonra = yildizDurumu(oge, altin: durum == .bitti)
        }

        return HaritaOgesi(
            id: sira, oge: oge, tur: tur, baslik: oge.title,
            kilitsiz: oge.isCompleted,
            durum: durum,
            altinGecisi: altinGecisi,
            simge: mantik.cardIconName(item: oge, globalPartId: partId),
            dilimSayisi: Int(dilimler.segmentCount),
            doluDilim: Int(dilimler.completedSegments),
            yildizOnce: yildizOnce,
            yildizSonra: yildizSonra,
            nefes: nefes,
            yarisRengi: ""
        )
    }

    private func yildizDurumu(_ oge: LessonItem, altin: Bool) -> HaritaYildizDurumu {
        let y = mantik.chestCardStars(item: oge, goldDone: altin)
        return HaritaYildizDurumu(
            kazanilan: Int(y.earned),
            bitti: y.done,
            bosOpaklik: Double(y.emptyStarAlpha)
        )
    }

    /// Ortak koddaki CardState / PanelState adı ("LOCKED", "OPEN", "DONE").
    private func kartDurumu(_ ad: String) -> HaritaKartDurumu {
        if ad == "LOCKED" {
            return .kilitli
        }
        if ad == "DONE" {
            return .bitti
        }
        return .acik
    }

    // MARK: Metinler

    /// Ortak kodun Türkçe sabitlerini Android'deki string kaynağına çevirir (aynı metin; ileride
    /// başka diller için `Metin` üzerinden okunur). Tanınmayan metin olduğu gibi döner.
    private lazy var metinAnahtarlari: [String: String] = {
        let m = LessonAdapterLogic.shared
        var s: [String: String] = [:]
        s[m.TEXT_KIND_LESSON] = "ui_ders"
        s[m.TEXT_KIND_CHEST] = "ui_unite_maratonu"
        s[m.TEXT_START] = "ui_basla"
        s[m.TEXT_CONTINUE] = "ui_devam_et"
        s[m.TEXT_REVIEW] = "ui_gozden_gecir"
        s[m.TEXT_RETRY] = "ui_tekrar_dene"
        s[m.TEXT_LOCKED] = "ui_kilitli"
        s[m.TEXT_UNLOCK_WITH_KEY] = "ui_1_anahtarla_kilidi_ac"
        s[m.TEXT_UNLOCK_OFFER_DESCRIPTION] = "ui_onceki_dersleri_tamamla_ya_da_anahtarla_ac"
        s[m.TEXT_LOCKED_DESCRIPTION] = "ui_kilidi_acmak_icin_onceki_dersleri_tamamla"
        return s
    }()

    private func yerel(_ turkce: String) -> String {
        if let ad = metinAnahtarlari[turkce] {
            return Metin.al(ad)
        }
        return turkce
    }

    /// PartCardAdapter.bind / PartSelectionFragment.partTitle başlıkları.
    private func kisimBasligi(_ partId: Int32) -> String {
        switch partId {
        case 1: return Metin.al("ui_abakusun_temeli_ve_toplama")
        case 2: return Metin.al("ui_abakuste_cikarma")
        case 3: return Metin.al("ui_abakuste_carpma")
        case 4: return Metin.al("ui_korleme_toplama")
        case 5: return Metin.al("ui_korleme_cikarma")
        case 6: return Metin.al("ui_korleme_carpma")
        case 7: return Metin.al("ui_ustalik_yolu_toplama")
        case 8: return Metin.al("ui_ustalik_yolu_cikarma")
        default: return kisimMantigi.partTitle(partId: partId)
        }
    }

    /// Kısım kartının kilit mesajı: ortak kodun Türkçe metni Android string'ine eşlenir.
    private func kapaliMesaji(_ turkce: String) -> String {
        if turkce.isEmpty {
            return turkce
        }
        for onkosul in [Int32(1), Int32(2), Int32(3)] {
            if turkce == kisimMantigi.marathonGateMessage(prerequisitePart: onkosul) {
                return Metin.al("ui_x_bolumunun_son_unite_maratonunu_bitir", kisimBasligi(onkosul))
            }
        }
        // Ortak kodda sabit olarak dışa açılmayan üç metin (PartSelectionLogic.partState, kısım 7 ve 8).
        if turkce == "Bu kısmı açmak için planı Pro'ya yükselt." {
            return Metin.al("ui_bu_kismi_acmak_icin_plani_pro")
        }
        if turkce == "Abaküste Toplama kısmını bitir." {
            return Metin.al("ui_abakuste_toplama_kismini_bitir")
        }
        if turkce == "Abaküste Çıkarma kısmını bitir." {
            return Metin.al("ui_abakuste_cikarma_kismini_bitir")
        }
        return turkce
    }

    // MARK: Kısım seçimi

    private func kisimlariYenile() {
        let pro: Bool = kisimMantigi.hasProPlan(plan: plan, teacherApproved: ogretmenOnayli)
        let p1: Bool = kisimMantigi.lastChestComplete(items: hamListe(1))
        let p2: Bool = kisimMantigi.lastChestComplete(items: hamListe(2))
        let p3: Bool = kisimMantigi.lastChestComplete(items: hamListe(3))
        kisimSatirlari = [
            kisimSatiri(sira: 0, baslik: Metin.al("fragment_part_selection_category_title_addition"),
                        partlar: kisimMantigi.ADDITION_PARTS, pro: pro, p1: p1, p2: p2, p3: p3),
            kisimSatiri(sira: 1, baslik: Metin.al("fragment_part_selection_category_title_subtraction"),
                        partlar: kisimMantigi.SUBTRACTION_PARTS, pro: pro, p1: p1, p2: p2, p3: p3),
            kisimSatiri(sira: 2, baslik: Metin.al("fragment_part_selection_category_title_multiplication"),
                        partlar: kisimMantigi.MULTIPLICATION_PARTS, pro: pro, p1: p1, p2: p2, p3: p3),
        ]
    }

    private func kisimSatiri(
        sira: Int, baslik: String, partlar: [KotlinInt], pro: Bool, p1: Bool, p2: Bool, p3: Bool
    ) -> HaritaKisimSatiri {
        // Kapı: 2–4 → 1. bölümün, 5 → 2.'nin, 6 → 3.'nün SON ünite maratonu; onaylı öğretmende hepsi açık.
        let durumlar: [PartState] = kisimMantigi.partStates(
            partIds: partlar,
            hasProPlan: pro,
            part1ChestComplete: p1,
            part2ChestComplete: p2,
            part3ChestComplete: p3,
            teacherApproved: ogretmenOnayli
        )
        var kartlar: [HaritaKisimKarti] = []
        for durum in durumlar {
            let partId: Int32 = durum.partId
            kartlar.append(
                HaritaKisimKarti(
                    id: Int(partId),
                    partId: partId,
                    baslik: kisimBasligi(partId),
                    gorselEtkin: durum.visualActive,
                    islevselEtkin: durum.functionalActive,
                    kapaliMesaji: kapaliMesaji(durum.inactiveMessage),
                    kartRengi: kisimMantigi.partCardColor(state: durum),
                    yaziRengi: kisimMantigi.partLabelColor(state: durum),
                    animasyon: uzantisiz(kisimMantigi.partAnimationName(partId: partId)),
                    // PartCardAdapter.bind: `if (partId == 2) repeatCount = 2 else repeatCount = 0`.
                    animasyonTekrari: partId == 2 ? 2 : 0
                )
            )
        }
        return HaritaKisimSatiri(id: sira, baslik: baslik, kartlar: kartlar)
    }

    /// "rocket_anim.json" → "rocket_anim".
    private func uzantisiz(_ ad: String) -> String {
        let ek: String = ".json"
        if ad.hasSuffix(ek) {
            return String(ad.dropLast(ek.count))
        }
        return ad
    }

    /// Android: PartCardAdapter kart tıklaması + PartSelectionFragment.openMapForPart.
    func kisimSec(_ kart: HaritaKisimKarti) {
        if !kart.islevselEtkin {
            // Android: LockedHint (Snackbar, 5 satıra kadar).
            bildir(kart.kapaliMesaji, uzun: true)
            return
        }
        if kisimMantigi.opensRacePanel(partId: kart.partId) {
            // Android burada harita yerine yarış panelini açar (Yaris/YarisPaneli.swift).
            let baslik: String = kart.partId == 7 ? Metin.al("ui_toplama_ustaligi") : Metin.al("ui_cikarma_ustaligi")
            hedef = HaritaHedefi(
                tur: .yarisPaneli,
                baslik: baslik,
                aciklama: "Yarış paneli",
                ayrintilar: ["Kısım: \(kart.partId)"],
                yarisPartId: kart.partId,
                yarisPartSecimindenMi: true
            )
            return
        }
        GlobalLessonData.shared.globalPartId = kart.partId
        listeyiKur(partId: kart.partId)
        seciliPart = kart.partId
    }

    /// Android: lessonPartBackButton (haritayı kapat, kısım seçimine dön).
    func kisimSeciminDon() {
        panel = nil
        seciliPart = nil
        ogeler = []
        kisimlariYenile()
    }

    // MARK: Dokunmalar

    /// Android: lessonCard.setOnClickListener (LessonViewHolder / RaceViewHolder).
    func ogeyeDokun(_ o: HaritaOgesi) {
        switch o.tur {
        case .baslik:
            return
        case .yaris:
            if !o.kilitsiz {
                bildir(Metin.al("race_unlock_complete_all_lessons"), uzun: false)
            } else {
                hedef = HaritaHedefi(
                    tur: .yarisPaneli,
                    baslik: o.baslik,
                    aciklama: "Yarış paneli",
                    ayrintilar: ["Harita sırası: \(o.id)"],
                    yarisPartId: o.oge.racePartId?.int32Value ?? 7,
                    yarisPartSecimindenMi: false,
                    yarisGeriPartId: o.oge.backRaceId?.int32Value
                )
            }
        case .ders, .sandik:
            panel = panelKur(o)
        }
    }

    /// Android: LessonAdapter.showLessonBottomSheet (içerik ve renkler).
    private func panelKur(_ o: HaritaOgesi) -> HaritaPanelDurumu {
        let d = mantik.lessonBottomSheetState(item: o.oge, globalPartId: aktifPart)
        var rekor: String? = nil
        if d.recordText != nil && !d.recordLayoutGone {
            // Android: appString(R.string.ui_rekor_x, item.record ?: "—").
            let deger: String = o.oge.record.map { String($0.intValue) } ?? "—"
            rekor = Metin.al("ui_rekor_x", deger)
        }
        return HaritaPanelDurumu(
            id: o.id,
            oge: o,
            baslik: d.title,
            turMetni: yerel(d.kindText),
            renk: kartDurumu(d.panelState.name),
            simge: d.iconName,
            adimGorunur: d.stepRowVisible,
            doluAdim: Int(d.doneSteps),
            adimSayisi: Int(d.stepCount),
            aciklama: yerel(d.description_),
            dugmeMetni: yerel(d.actionText),
            dugmeEtkin: d.actionEnabled,
            anahtarlaAcilir: d.keyUnlockable,
            egitimTekrarGorunur: d.againTutorialVisible,
            rekorMetni: rekor,
            rekorTiklanir: d.recordClickable
        )
    }

    /// Android: dismissPanel (karartmaya, üst çubuğa ya da alt çubuğa dokunuş).
    func paneliKapat() {
        panel = nil
    }

    /// Android: actionButton.setOnClickListener.
    func eylemDugmesi(_ acik: HaritaPanelDurumu) {
        if anahtarIslemde {
            return
        }
        let karar: String = enerjiKarari(acik.oge.oge)
        if karar == "LOCKED_COLLAPSE" {
            // Diğer kilitliler: düğme zaten devre dışı.
            return
        }
        if karar == "UNLOCK_WITH_KEY" {
            anahtarlaAc(acik)
            return
        }
        panel = nil
        if karar == "NOT_ENOUGH_ENERGY" {
            magazayaGit(acik, neden: "Enerji yok")
            return
        }
        // Açılacak ekranı ortak kural seçer (Android: LessonAdapter.continueWithLesson).
        let hedefAdi: String = mantik.continueWithLessonTarget(item: acik.oge.oge).name
        let tur: HaritaHedefTuru
        if hedefAdi == "TUTORIAL" {
            tur = .ogretici
        } else if hedefAdi == "BLINDING_LESSON" {
            tur = .korDers
        } else {
            tur = .abakusDersi
        }
        hedef = dersHedefi(acik, tur: tur, ekSatir: enerjiSatiri(karar))
    }

    /// Android: againTutorial.setOnClickListener. "Eğitimi tekrarla" da 1 enerji harcar
    /// (spendLessonEnergyThen); enerji yoksa mağaza açılır.
    func egitimiTekrarla(_ acik: HaritaPanelDurumu) {
        let karar: String = enerjiKarari(acik.oge.oge)
        panel = nil
        if karar == "NOT_ENOUGH_ENERGY" {
            magazayaGit(acik, neden: "Enerji yok")
            return
        }
        hedef = dersHedefi(acik, tur: .ogreticiTekrari, ekSatir: enerjiSatiri(karar))
    }

    /// Android: recordLayout.setOnClickListener (liderlik tablosu / RecordFragment).
    func rekoraDokun(_ acik: HaritaPanelDurumu) {
        if !acik.rekorTiklanir {
            return
        }
        panel = nil
        hedef = HaritaHedefi(
            tur: .liderlikTablosu,
            baslik: acik.baslik,
            aciklama: "Liderlik tablosu",
            ayrintilar: ["Harita sırası: \(acik.id)"]
        )
    }

    /// Ortak kural: ActionButtonDecision adı.
    private func enerjiKarari(_ oge: LessonItem) -> String {
        return mantik.actionButtonDecision(
            item: oge,
            isInfiniteEnergy: sonsuzEnerji,
            hasEnoughEnergy: enerjiYeterli,
            globalPartId: aktifPart
        ).name
    }

    private func enerjiSatiri(_ karar: String) -> String {
        if karar == "USE_ENERGY_AND_START" {
            return "Enerji: \(mantik.LESSON_ENERGY_COST) harcanır"
        }
        return "Enerji: harcanmaz (sonsuz enerji)"
    }

    private func magazayaGit(_ acik: HaritaPanelDurumu, neden: String) {
        hedef = HaritaHedefi(
            tur: .magaza,
            baslik: acik.baslik,
            aciklama: neden,
            ayrintilar: ["Açılacak ekran: Mağaza"]
        )
    }

    private func dersHedefi(_ acik: HaritaPanelDurumu, tur: HaritaHedefTuru, ekSatir: String) -> HaritaHedefi {
        let oge = acik.oge.oge
        let ekran: String
        if tur == .ogretici || tur == .ogreticiTekrari {
            ekran = "Öğretici (\(oge.tutorialNumber))"
        } else if tur == .korDers {
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
        return HaritaHedefi(tur: tur, baslik: acik.baslik, aciklama: acik.aciklama, ayrintilar: satirlar)
    }

    // MARK: Anahtarla kilit açma (Android: LessonAdapter.unlockLessonWithKey)

    /// Kilitli dersi LESSON_UNLOCK_KEY_COST anahtarla açar: anahtar düşüldükten SONRA ders
    /// `isCompleted = true` yazılır ve panel açılmış hâliyle yeniden gösterilir. Can harcanmaz.
    /// Anahtar yetmiyorsa "Yetersiz anahtar!" + panel kapanır + mağaza; sunucu reddederse hata
    /// balonu ve düğme yeniden etkin (onay penceresi yok: Android doğrudan harcar).
    private func anahtarlaAc(_ acik: HaritaPanelDurumu) {
        let bedel: Int = Int(mantik.LESSON_UNLOCK_KEY_COST)
        // Android: MainActivity.spendKeysConfirmed — bilinen bakiye yetmiyorsa işlem başlamaz.
        if cuzdan.cuzdan.anahtar < bedel {
            bildir(Metin.al("ui_yetersiz_anahtar_2"), uzun: false)
            panel = nil
            magazayaGit(acik, neden: "Anahtar yetersiz")
            return
        }
        anahtarIslemde = true
        let partId: Int32 = aktifPart
        let sira: Int = acik.id
        let stableId: String = acik.oge.oge.stableId
        let yedek: LessonItem = acik.oge.oge
        let gerekce: String = WalletReason.shared.SPEND
        cuzdan.degistir(
            altinFarki: 0,
            anahtarFarki: -bedel,
            gerekce: gerekce,
            geriAlmaJetonu: nil
        ) { [weak self] degisim in
            guard let self = self else { return }
            self.anahtarIslemde = false
            if degisim == nil {
                self.bildir(Metin.al("ui_islem_tamamlanamadi_tekrar_deneyin"), uzun: false)
                return
            }
            self.acilanDersiYaz(partId: partId, sira: sira, stableId: stableId, yedek: yedek)
        }
    }

    private func acilanDersiYaz(partId: Int32, sira: Int, stableId: String, yedek: LessonItem) {
        // Panel açıkken liste değişmiş olabilir; en güncel item üzerine yazılır (aynı stableId).
        let guncelListe: [LessonItem] = haritaListesi(partId)
        var guncel: LessonItem = yedek
        if sira >= 0 && sira < guncelListe.count && guncelListe[sira].stableId == stableId {
            guncel = guncelListe[sira]
        }
        let acilan: LessonItem = mantik.unlockedWithKey(item: guncel)
        kaynak.dersiYaz(partId: partId, sira: Int32(sira), oge: acilan)
        panel = nil
        yenile()
        // Android: kapanma animasyonu bitince panel açılmış hâliyle yeniden gösterilir (220 ms).
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) { [weak self] in
            guard let self = self else { return }
            if self.seciliPart != partId || sira >= self.ogeler.count {
                return
            }
            self.panel = self.panelKur(self.ogeler[sira])
        }
    }

    // MARK: Bilgi balonu

    private func bildir(_ metin: String, uzun: Bool) {
        if metin.isEmpty {
            return
        }
        bildirimSayaci += 1
        let no: Int = bildirimSayaci
        bildirim = HaritaBildirimi(id: no, metin: metin, uzun: uzun)
        // Android: Toast.LENGTH_SHORT ≈ 2 sn, Snackbar.LENGTH_LONG 2,75 sn.
        let sure: Double = uzun ? 2.75 : 2.0
        DispatchQueue.main.asyncAfter(deadline: .now() + sure) { [weak self] in
            guard let self = self else { return }
            if self.bildirim?.id == no {
                self.bildirim = nil
            }
        }
    }
}
