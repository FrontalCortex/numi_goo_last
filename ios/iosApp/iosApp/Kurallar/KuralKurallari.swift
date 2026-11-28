import Foundation
import Shared

// Kural kitabı ve ders içi kural paneli: hangi dersin hangi tabloyu gösterdiği, düğmelerin görünürlüğü,
// kitabın hangi tabloları açtığı.
//
// Kuralların kendisi ortak koddadır: `abakus/LessonRulesLogic.kt` (`LessonRulesLesson`; Android
// AbacusFragment / BlindingLessonFragment / LessonRulesPanel / RulesFragment kopyası). Bu dosya yalnızca
// ortak türleri Swift türlerine çevirir; kural burada yazılmaz. Panel dokunuşu (onRulesPanelButtonClicked)
// KuralModeli.swift'tedir.
//
// Sözlük: "sıra" = LessonItem.mapFragmentIndex (haritadaki sıra); "bölüm" = GlobalLessonData.globalPartId.

// MARK: - Türler

/// Panelde gösterilecek tablo. Android: LessonRulesPanel.Table (AbacusFragment.RulesPanelTableType).
enum KuralTablosu: Equatable {
    case yok
    case bes
    case onBes
    case on
    case boncuk
    case boncukCikarma
    case onCikarma
    case carpim
}

/// Kitapta dokunulan tablo. Android: RulesFragment.RulesTableSelection.
enum KuralSecimi: Equatable {
    case bes
    case onBes
    case on
    case boncuk
    /// Çıkarma bölümündeki 5'li tablo (panelde bes ile aynı tabloya gider).
    case cikarmaBes
    case onCikarma
    case boncukCikarma
    case carpim(Int)
}

/// Kitabın içerik bölümü. Android: RulesFragment.RulesContentSection (ADDITION / EXTRACTION).
enum KuralBolumu: Equatable {
    case toplama
    case cikarma
}

/// Android View.VISIBLE / INVISIBLE (yer tutar, çizilmez) / GONE.
enum KuralGorunurluk: Equatable {
    case gorunur
    case gizli
    case yok
}

/// Ders ekranının açılışta kural durumu (ekran galerisi için).
enum KuralBaslangicDurumu {
    case kapali
    /// Panel açık; nil ise dersin kendi tablosu, doluysa kitaptan seçilmiş gibi o tablo.
    case panel(KuralSecimi?)
    case kitap
}

/// Sihirli değnek düğmesinin dokunuşundan çıkan sonuç.
enum KuralEylemi: Equatable {
    case yok
    case panelAcildi
    case panelKapandi
    case kitapAcildi
}

// MARK: - Ders bilgisi

/// Bir ders ekranının kural kuralları için bildiği her şey.
struct KuralDersBilgisi: Equatable {
    /// GlobalLessonData.globalPartId.
    var partId: Int
    /// LessonItem.mapFragmentIndex (kör derste GlobalValues.mapFragmentStepIndex).
    var sira: Int
    /// true: BlindingLessonFragment (kör ders), false: AbacusFragment.
    var korMu: Bool
    /// Günlük soru modunda daily_part_id; değilse nil.
    var gunlukPartId: Int? = nil
    /// Android: AbacusFragment'ta FirebaseAuth.currentUser == null → düğmeler gizli (hesapsız ilk öğretici).
    var hesapsiz: Bool = false

    static func abakusDersi(_ baslangic: AbakusDersBaslangici, hesapsiz: Bool) -> KuralDersBilgisi {
        return KuralDersBilgisi(
            partId: baslangic.partId, sira: baslangic.haritaSirasi, korMu: false,
            gunlukPartId: nil, hesapsiz: hesapsiz
        )
    }

    static func korDersi(_ baslangic: KorDersBaslangici) -> KuralDersBilgisi {
        var gunluk: Int? = nil
        if let g = baslangic.gunlukSoru {
            gunluk = g.partId
        }
        return KuralDersBilgisi(
            partId: baslangic.partId, sira: baslangic.dersIndeksi, korMu: true,
            gunlukPartId: gunluk, hesapsiz: false
        )
    }

    var gunlukMu: Bool {
        return gunlukPartId != nil
    }

    /// Ortak koddaki kural girdisi.
    var ortakDers: LessonRulesLesson {
        let gunluk: Int = gunlukPartId ?? 0
        return LessonRulesLesson(
            globalPartId: Int32(partId),
            mapFragmentIndex: Int32(sira),
            isBlinding: korMu,
            isDailyQuestionMode: gunlukMu,
            dailyQuestionPartId: Int32(gunluk),
            signedOut: hesapsiz
        )
    }

    /// Kitaptan tablo seçilerek mi çalışıyor (panel tablosu derse göre değil, çocuğun seçimine göre).
    var seciciVar: Bool {
        return ortakDers.usesRulesTablePicker()
    }

    // MARK: Düğmeler

    /// rulesBookButton.
    var kitapDugmesi: KuralGorunurluk {
        return KuralDersBilgisi.gorunurluk(ortakDers.rulesBookButtonVisibility())
    }

    /// rulesPanelButton (sihirli değnek).
    var panelDugmesi: KuralGorunurluk {
        return KuralDersBilgisi.gorunurluk(ortakDers.rulesPanelButtonVisibility())
    }

    // MARK: Panel

    /// Android: updateActiveRulesPanelTable (seçici yokken dersin sırasına göre tablo).
    func siraninPanelTablosu() -> KuralTablosu {
        return KuralDersBilgisi.tablo(ortakDers.activeRulesPanelTable())
    }

    /// 5'li tablonun başlığı çıkarma diliyle mi ("Çıkarılacak sayı / 5 gider. Kardeş gelir.").
    var besCikarmaMi: Bool {
        return ortakDers.rulesPanelFiveIsSubtraction()
    }

    /// Panel tablolarının metin anahtarı öneki: abaküs fragment_abacus_*, kör ders fragment_blinding_lesson_*.
    var panelMetinOnEki: String {
        return korMu ? "fragment_blinding_lesson_" : "fragment_abacus_"
    }

    // MARK: Kitap

    /// Kitabın bu derste açacağı tablolar (ortak: LessonRulesLesson.rulesBookSetup).
    func kitapAyari() -> KuralKitabiAyari {
        let k: LessonRulesBookSetup = ortakDers.rulesBookSetup()
        var a: KuralKitabiAyari = KuralKitabiAyari()
        a.bolum = k.section == LessonRulesSection.extraction ? .cikarma : .toplama
        a.bes = k.fiveTable
        a.onBes = k.tenFiveTable
        a.on = k.tenTable
        a.boncuk = k.beadTable
        a.cikarmaBes = k.extractionFiveTable
        a.onCikarma = k.tenExtractionTable
        a.boncukCikarma = k.beadExtractionTable
        a.carpim = k.multiplicationTables
        a.bolucu1 = KuralDersBilgisi.gorunurluk(k.divider1)
        a.bolucu2 = KuralDersBilgisi.gorunurluk(k.divider2)
        a.bolucu3 = KuralDersBilgisi.gorunurluk(k.divider3)
        a.tabloSecici = k.tablePickerEnabled
        // changeRulesTableText: applyRulesTableHeaderTexts(globalPartId == 2)
        a.etiket = k.subtractionHeaderTexts ? Metin.al("ui_cikarilacak_sayi") : Metin.al("ui_eklenecek_sayi_2")
        return a
    }

    // MARK: Ortak türlerden çeviri

    private static func gorunurluk(_ g: LessonRulesVisibility) -> KuralGorunurluk {
        if g == LessonRulesVisibility.visible { return .gorunur }
        if g == LessonRulesVisibility.invisible { return .gizli }
        return .yok
    }

    private static func tablo(_ t: LessonRulesTable) -> KuralTablosu {
        if t == LessonRulesTable.five { return .bes }
        if t == LessonRulesTable.tenFive { return .onBes }
        if t == LessonRulesTable.ten { return .on }
        if t == LessonRulesTable.bead { return .boncuk }
        if t == LessonRulesTable.beadExtraction { return .boncukCikarma }
        if t == LessonRulesTable.tenExtraction { return .onCikarma }
        if t == LessonRulesTable.multiplication { return .carpim }
        return .yok
    }
}

// MARK: - Kitap ayarı

/// Kural kitabında hangi tablonun görüneceği. Varsayılanlar Android'in kitap açıldığındaki ham durumudur
/// (RulesFragment.onViewCreated: toplama bölümü, bütün tablolar görünür, çarpım tabloları kapalı).
struct KuralKitabiAyari: Equatable {
    var bolum: KuralBolumu = .toplama
    var bes: Bool = true
    var onBes: Bool = true
    var on: Bool = true
    var boncuk: Bool = true
    var cikarmaBes: Bool = true
    var onCikarma: Bool = true
    var boncukCikarma: Bool = true
    /// 1x ... 9x çarpım tabloları (ve aralarındaki bölücüler).
    var carpim: Bool = false
    /// view1, view2, view3: toplama tabloları arasındaki bölücüler (INVISIBLE yer tutar).
    var bolucu1: KuralGorunurluk = .gorunur
    var bolucu2: KuralGorunurluk = .gorunur
    var bolucu3: KuralGorunurluk = .gorunur
    /// Tablolara dokununca seçilir (setRulesTablePickerEnabled).
    var tabloSecici: Bool = false
    /// applyRulesTableHeaderTexts: toplama tablolarının ve çıkarma 5'lisinin sol başlık hücresi.
    /// nil: Android XML'indeki kendi metinleri kalır.
    var etiket: String? = nil

    /// Ders olmadan açılan kitap (ekran galerisi): seçilen bölümün bütün tabloları.
    static func hepsi(_ bolum: KuralBolumu) -> KuralKitabiAyari {
        var a: KuralKitabiAyari = KuralKitabiAyari()
        a.bolum = bolum
        a.carpim = bolum == .toplama
        return a
    }

    /// Kitabın kaydırılan içeriğini sıralı öğeler olarak çıkarır. Android: LinearLayout içindeki sıra,
    /// GONE olanlar yok, INVISIBLE bölücüler yer tutar.
    func ogeler() -> [KuralKitapOgesi] {
        var liste: [KuralKitapOgesi] = []
        var no: Int = 0
        func ekle(_ tur: KuralKitapOgeTuru, ust: CGFloat = 0, alt: CGFloat = 0) {
            liste.append(KuralKitapOgesi(id: no, tur: tur, ustBosluk: ust, altBosluk: alt))
            no += 1
        }
        if bolum == .toplama {
            if bes { ekle(.tablo(.bes), ust: 20) }
            ekle(.bolucu(bolucu1))
            if onBes { ekle(.tablo(.onBes)) }
            ekle(.bolucu(bolucu2))
            if on { ekle(.tablo(.on)) }
            ekle(.bolucu(bolucu3))
            if boncuk { ekle(.tablo(.boncuk), alt: 10) }
            if carpim {
                for basamak in 1...9 {
                    ekle(.bolucu(.gorunur))
                    ekle(.tablo(.carpim(basamak)))
                }
            }
            // multipleLayout: layout_marginBottom = 20dp
            ekle(.bosluk, alt: 20)
        } else {
            if cikarmaBes { ekle(.tablo(.cikarmaBes), ust: 20) }
            // view4 yalnızca 10'luk çıkarma tablosuyla, view5 yalnızca boncuk çıkarma tablosuyla görünür.
            if onCikarma {
                ekle(.bolucu(.gorunur))
                ekle(.tablo(.onCikarma))
            }
            if boncukCikarma {
                ekle(.bolucu(.gorunur))
                ekle(.tablo(.boncukCikarma), alt: 10)
            }
        }
        return liste
    }
}

enum KuralKitapOgeTuru: Equatable {
    case tablo(KuralSecimi)
    case bolucu(KuralGorunurluk)
    case bosluk
}

struct KuralKitapOgesi: Identifiable, Equatable {
    let id: Int
    let tur: KuralKitapOgeTuru
    var ustBosluk: CGFloat = 0
    var altBosluk: CGFloat = 0
}
