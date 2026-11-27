import Foundation
import Shared
import SwiftUI

// Rehberin "ne zaman, hangi koşulla açılır" kuralları ve ortak nesneler.
//
// Android karşılıkları:
//   AbacusFragment.setupGuidePanel                         → abakusDersiRehberi
//   BlindingLessonFragment.initializeGuideSystem / startNormalGuide / startGuideNumber → korDersiRehberi
//   GuideShowTracker (GuideHelper.kt)                       → gosterilebilir / gosterildi
//   MarathonGuideStore.scheduleIfEligible (ChestFragment.claim) → maratonuPlanla
//
// NOT (ortak koda taşınmalı): abaküs ve kör ders rehberinin açılma koşulları Android'de iki Fragment'ın
// içinde. Burada birebir kopya olarak tek yerde duruyor; sayfa içeriği için bkz. RehberIcerigi.swift.
// Maraton rehberinin uygunluk kuralı ve metinleri zaten ortak kodda (MarathonGuideLogic, marathonGuidePanelData).

/// Başlatılacak bir rehberin hazır hâli.
struct RehberBaslatma {
    let rehberNo: Int
    let adimlar: [RehberAdimi]
    /// Kör dersteki rehber 5: gösterilince "atla öğreticisi gösterildi" kaydı yazılır.
    let atlaOgreticisi: Bool
}

enum RehberOrtak {
    /// Gösterim kayıtları. Varsayılan: UserDefaults; galeri / test başka bir uygulama atayabilir.
    static var depo: RehberKayitDeposu = UserDefaultsRehberDeposu()

    /// Haritadaki maraton rehberi (harita ekranı ve yönetici ortak kullanır).
    static let maraton: RehberMaratonDurumu = RehberMaratonDurumu()

    /// GuideShowTracker.MAX_SHOW_COUNT.
    static let enCokGosterim: Int = 2

    // MARK: Gösterim sınırı

    /// GuideShowTracker.canShow: yalnızca rehber 6 (abaküs boyutu ipucu) en çok iki kez gösterilir.
    static func gosterilebilir(rehberNo: Int) -> Bool {
        if rehberNo == 6 {
            return depo.gosterimSayisi(rehberNo: rehberNo) < enCokGosterim
        }
        return true
    }

    /// Rehber panel kaymaya başlarken çağrılır (Android: showGuidePanelWithAnimation'dan sonraki kayıt satırları).
    static func gosterildi(_ baslatma: RehberBaslatma) {
        if baslatma.atlaOgreticisi {
            depo.atlaOgreticisiniIsaretle()
        }
        if baslatma.rehberNo == 6 {
            depo.gosterimiKaydet(rehberNo: 6)
        }
    }

    // MARK: Abaküs dersi

    /// AbacusFragment.setupGuidePanel: `abacusGuideNumber` yoksa ya da ders adımı 1 değilse rehber yok;
    /// rehber 6 ikiden fazla gösterilmez.
    static func abakusDersiRehberi(rehberNo: Int?, ilkAdim: Bool) -> RehberBaslatma? {
        guard let no = rehberNo, ilkAdim, gosterilebilir(rehberNo: no) else {
            return nil
        }
        let adimlar: [RehberAdimi] = RehberIcerigi.adimlar(rehberNo: no)
        if adimlar.isEmpty {
            return nil
        }
        return RehberBaslatma(rehberNo: no, adimlar: adimlar, atlaOgreticisi: false)
    }

    // MARK: Kör ders

    /// BlindingLessonFragment.initializeGuideSystem: "sayıyı geç" düğmesi görünüyorsa ve öğretici daha önce
    /// gösterilmediyse rehber 5; aksi hâlde dersin kendi rehberi (5 hariç, yalnızca ders adımı 1'de).
    static func korDersiRehberi(rehberNo: Int?, ilkAdim: Bool, adimAtlaGorunur: Bool) -> RehberBaslatma? {
        var secilen: Int? = nil
        var atla: Bool = false
        if adimAtlaGorunur && !depo.atlaOgreticisiGosterildi() {
            secilen = 5
            atla = true
        } else if let no = rehberNo, no != 5, ilkAdim {
            secilen = no
        }
        guard let no = secilen, gosterilebilir(rehberNo: no) else {
            return nil
        }
        let adimlar: [RehberAdimi] = RehberIcerigi.adimlar(rehberNo: no)
        if adimlar.isEmpty {
            return nil
        }
        return RehberBaslatma(rehberNo: no, adimlar: adimlar, atlaOgreticisi: atla)
    }

    // MARK: Maraton

    /// ChestFragment.claim → MarathonGuideStore.scheduleIfEligible: ilk ünite maratonu (1. kısım, ilk sandık)
    /// ilk kez bitirilirken, ödül verilmeden ÖNCEKİ ders öğesiyle çağrılır. Uygunsa "bekliyor" bayrağı yazılır.
    static func maratonuPlanla(partId: Int32, sira: Int32, oge: LessonItem?) {
        let uygun: Bool = MarathonGuideLogic.shared.shouldSchedule(partId: partId, lessonIndex: sira, item: oge)
        if uygun {
            depo.maratonBekleyenAyarla(true)
        }
    }
}
