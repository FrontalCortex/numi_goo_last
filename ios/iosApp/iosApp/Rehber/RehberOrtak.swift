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
// Açılma koşulları ve gösterim sınırı ortak koddadır (ders/GuideHelper.kt → GuideHelper); burada yalnızca
// kayıt deposu okunur/yazılır. Sayfa içeriği için bkz. RehberIcerigi.swift.
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

    // MARK: Gösterim sınırı

    /// GuideShowTracker.canShow: yalnızca rehber 6 (abaküs boyutu ipucu) en çok iki kez gösterilir
    /// (ortak: GuideHelper.canShow).
    static func gosterilebilir(rehberNo: Int) -> Bool {
        return GuideHelper.shared.canShow(guideNumber: Int32(rehberNo), guide6ShownCount: rehber6Gosterimi())
    }

    /// Rehber panel kaymaya başlarken çağrılır (Android: showGuidePanelWithAnimation'dan sonraki kayıt satırları).
    static func gosterildi(_ baslatma: RehberBaslatma) {
        if baslatma.atlaOgreticisi {
            depo.atlaOgreticisiniIsaretle()
        }
        if GuideHelper.shared.recordsShown(guideNumber: Int32(baslatma.rehberNo)) {
            depo.gosterimiKaydet(rehberNo: baslatma.rehberNo)
        }
    }

    // MARK: Abaküs dersi

    /// AbacusFragment.setupGuidePanel (ortak: GuideHelper.abacusLessonGuideNumber). `ilkAdim`:
    /// LessonItem.currentStep == 1.
    static func abakusDersiRehberi(rehberNo: Int?, ilkAdim: Bool) -> RehberBaslatma? {
        let adim: Int32 = ilkAdim ? 1 : 0
        let secilen: KotlinInt? = GuideHelper.shared.abacusLessonGuideNumber(
            abacusGuideNumber: kutula(rehberNo), currentStep: adim, guide6ShownCount: rehber6Gosterimi()
        )
        guard let no = secilen else {
            return nil
        }
        let rehber: Int = Int(no.intValue)
        return RehberBaslatma(rehberNo: rehber, adimlar: RehberIcerigi.adimlar(rehberNo: rehber), atlaOgreticisi: false)
    }

    // MARK: Kör ders

    /// BlindingLessonFragment.initializeGuideSystem (ortak: GuideHelper.blindingLessonGuideNumber): "sayıyı geç"
    /// düğmesi görünüyorsa ve öğretici daha önce gösterilmediyse rehber 5; aksi hâlde dersin kendi rehberi
    /// (5 hariç, yalnızca ders adımı 1'de).
    static func korDersiRehberi(rehberNo: Int?, ilkAdim: Bool, adimAtlaGorunur: Bool) -> RehberBaslatma? {
        let adim: Int32 = ilkAdim ? 1 : 0
        let secilen: KotlinInt? = GuideHelper.shared.blindingLessonGuideNumber(
            abacusGuideNumber: kutula(rehberNo),
            currentStep: adim,
            skipStepButtonVisible: adimAtlaGorunur,
            skipStepTutorialShown: depo.atlaOgreticisiGosterildi(),
            guide6ShownCount: rehber6Gosterimi()
        )
        guard let no = secilen else {
            return nil
        }
        let rehber: Int = Int(no.intValue)
        // Normal yol 5'i açmaz: 5 her zaman atlama öğreticisi yolundan gelir.
        let atla: Bool = rehber == Int(GuideHelper.shared.SKIP_STEP_GUIDE_NUMBER)
        return RehberBaslatma(rehberNo: rehber, adimlar: RehberIcerigi.adimlar(rehberNo: rehber), atlaOgreticisi: atla)
    }

    private static func rehber6Gosterimi() -> Int32 {
        let no: Int = Int(GuideHelper.shared.LIMITED_GUIDE_NUMBER)
        return Int32(depo.gosterimSayisi(rehberNo: no))
    }

    private static func kutula(_ deger: Int?) -> KotlinInt? {
        guard let d = deger else {
            return nil
        }
        return KotlinInt(int: Int32(d))
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
