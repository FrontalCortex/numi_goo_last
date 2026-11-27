import Foundation
import Shared

// Kural kitabı ve ders içi kural paneli: hangi dersin hangi tabloyu gösterdiği, düğmelerin görünürlüğü,
// kitabın hangi tabloları açtığı.
//
// DİKKAT (ortak koda taşınmalı): Bu dosyadaki kurallar Android'in Fragment'larında kalmış saf
// `when`/`if` tablolarının BİREBİR KOPYASIDIR; ortak Kotlin'de (ios/shared) karşılıkları yoktur
// (yalnızca BlindingLessonRules.usesRulesTablePicker vardır, o doğrudan çağrılır). Android'de bir kural
// değişirse buradaki değişiklik elle yapılmalıdır. Kaynaklar (app/src/main/java/com/example/app/):
//  - AbacusFragment.kt: updateActiveRulesPanelTable (1302), applyRulesPanelFiveText (1354),
//    rulesBookVisibility (1405), rulesBookSetup (1430), applyRulesDividersVisibility (1490),
//    changeRulesTableText (1518), onRulesPanelButtonClicked (1262)
//  - BlindingLessonFragment.kt: updateActiveRulesPanelTable (1116), rulesPanelFiveIsSubtraction (1138),
//    rulesBookVisibility (1150), rulesBookSetup (1167), applyRulesDividersVisibility (1204),
//    onRulesPanelButtonClicked (1094)
//  - abacus/LessonRulesPanel.kt (panel durumu), model/RulesFragment.kt (kitap).
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

    /// Kitaptan tablo seçilerek mi çalışıyor (panel tablosu derse göre değil, çocuğun seçimine göre).
    /// Android: kör derste usesRulesTablePicker (bölüm 4..9 ya da günlük soru), abaküste globalPartId == 3.
    var seciciVar: Bool {
        if korMu {
            return BlindingLessonRules.shared.usesRulesTablePicker(
                globalPartId: Int32(partId), isDailyQuestionMode: gunlukMu
            )
        }
        return partId == 3
    }

    // MARK: Düğmeler

    /// rulesBookButton. Abaküs: AbacusFragment.rulesBookVisibility; kör: yalnızca seçici varsa görünür.
    var kitapDugmesi: KuralGorunurluk {
        if korMu {
            return seciciVar ? .gorunur : .yok
        }
        if hesapsiz { return .yok }
        if partId == 1 && sira < 12 { return .yok }
        if partId == 1 && sira < 17 { return .gizli }
        if partId == 2 && sira < 5 { return .yok }
        return .gorunur
    }

    /// rulesPanelButton (sihirli değnek).
    var panelDugmesi: KuralGorunurluk {
        if korMu {
            return seciciVar ? .gorunur : .yok
        }
        if hesapsiz { return .yok }
        if partId == 1 && sira < 12 { return .yok }
        if partId == 2 && sira < 5 { return .yok }
        return .gorunur
    }

    // MARK: Panel

    /// Android: updateActiveRulesPanelTable (seçici yokken dersin sırasına göre tablo).
    /// Kör derste bölüm 2'nin boncuk çıkarma dalı yoktur (Android'de de böyle).
    func siraninPanelTablosu() -> KuralTablosu {
        let i: Int = sira
        if partId == 1 {
            if i > 28 { return .boncuk }
            if i > 22 { return .on }
            if i > 16 { return .onBes }
            if i >= 12 { return .bes }
            return .yok
        }
        if partId == 2 {
            if !korMu && i > 15 { return .boncukCikarma }
            if i > 10 { return .onCikarma }
            if i > 5 { return .bes }
        }
        return .yok
    }

    /// 5'li tablonun başlığı çıkarma diliyle mi ("Çıkarılacak sayı / 5 gider. Kardeş gelir.").
    /// Android: applyRulesPanelFiveText / rulesPanelFiveIsSubtraction.
    var besCikarmaMi: Bool {
        return (partId == 2 && sira > 5) || partId == 5
    }

    /// Panel tablolarının metin anahtarı öneki: abaküs fragment_abacus_*, kör ders fragment_blinding_lesson_*.
    var panelMetinOnEki: String {
        return korMu ? "fragment_blinding_lesson_" : "fragment_abacus_"
    }

    // MARK: Kitap

    /// Kitabın bu derste açacağı tablolar. Android: rulesBookSetup + applyRulesDividersVisibility +
    /// changeRulesTableText.
    func kitapAyari() -> KuralKitabiAyari {
        var ayar: KuralKitabiAyari = korMu ? korKitapAyari() : abakusKitapAyari()
        // changeRulesTableText: applyRulesTableHeaderTexts(globalPartId == 2)
        ayar.etiket = partId == 2 ? Metin.al("ui_cikarilacak_sayi") : Metin.al("ui_eklenecek_sayi_2")
        return ayar
    }

    private func abakusKitapAyari() -> KuralKitabiAyari {
        var a: KuralKitabiAyari = KuralKitabiAyari()
        let p: Int = partId
        let i: Int = sira
        if p == 1 && i > 16 {
            a.bolum = .toplama
            a.on = false
            a.boncuk = false
        }
        if p == 1 && i > 22 { a.on = true }
        if p == 1 && i > 28 { a.boncuk = true }
        if p == 2 && i > 5 {
            a.bolum = .cikarma
            a.onCikarma = false
            a.boncukCikarma = false
        }
        if p == 2 && i > 10 { a.onCikarma = true }
        if p == 2 && i > 15 { a.boncukCikarma = true }
        if p == 3 {
            a.bolum = .toplama
            a.carpim = true
            a.tabloSecici = true
        }
        let b: (KuralGorunurluk, KuralGorunurluk, KuralGorunurluk) = siraBolucular()
        a.bolucu1 = b.0
        a.bolucu2 = b.1
        a.bolucu3 = b.2
        return a
    }

    private func korKitapAyari() -> KuralKitabiAyari {
        var a: KuralKitabiAyari = KuralKitabiAyari()
        let p: Int = partId
        let gunluk: Int? = gunlukPartId
        if seciciVar {
            // enableTablePicker: toplama tabloları açık, çarpım kapalı, bölücüler hep görünür.
            a.tabloSecici = true
            a.bolucu1 = .gorunur
            a.bolucu2 = .gorunur
            a.bolucu3 = .gorunur
        } else {
            let b: (KuralGorunurluk, KuralGorunurluk, KuralGorunurluk) = siraBolucular()
            a.bolucu1 = b.0
            a.bolucu2 = b.1
            a.bolucu3 = b.2
        }
        if p == 5 || p == 8 || (gunluk != nil && gunluk == 2) {
            a.bolum = .cikarma
        }
        if p == 6 || (gunluk != nil && (gunluk == 1 || gunluk == 3)) {
            a.bolum = .toplama
            a.carpim = true
            a.bolucu1 = .gorunur
            a.bolucu2 = .gorunur
            a.bolucu3 = .gorunur
        }
        return a
    }

    /// Android: applyRulesDividersVisibility (view1, view2, view3).
    private func siraBolucular() -> (KuralGorunurluk, KuralGorunurluk, KuralGorunurluk) {
        let p: Int = partId
        let i: Int = sira
        if p == 1 && i > 11 {
            let g1: KuralGorunurluk = i > 16 ? .gorunur : .gizli
            let g2: KuralGorunurluk = i > 22 ? .gorunur : .gizli
            let g3: KuralGorunurluk = i > 28 ? .gorunur : .gizli
            return (g1, g2, g3)
        }
        if p == 2 && i >= 11 && i <= 15 {
            return (.gorunur, .gorunur, .gizli)
        }
        if p == 2 && i >= 16 && i <= 20 {
            return (.gorunur, .gorunur, .gorunur)
        }
        if !korMu && p == 3 {
            return (.gorunur, .gorunur, .gorunur)
        }
        return (.gizli, .gizli, .gizli)
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
