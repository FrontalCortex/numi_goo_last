import Shared
import SwiftUI

// Ders içi rehberin sayfaları.
//
// Sayfa tablosu (Sobi hâlleri, metin anahtarları, işaret edilen öğe, renkler, boncuk kimlikleri, zorunlu
// tıklama) ortak koddadır: `ders/GuideHelper.kt` → `GuideHelper.getGuideContentsForNumber` (Android
// GuideHelper.kt kopyası). Bu dosya yalnızca ortak `GuideContent`'i SwiftUI türüne (`RehberAdimi`) çevirir;
// metinler `strings.xml` adlarıyla Metin.al'dan gelir.
//
// Rehber numaraları (LessonItem.abacusGuideNumber):
//   1 toplama (abaküse ilk sayıyı yaz, ikinciyi ekle, Kontrol Et)   2 sihirli değnek (kural tablosu)
//   3 kurallar kitabı                                                4 kural tablosunu abaküse alma
//   5 kör derste "sayıyı geç" oku                                    6 abaküs boyutu (ölçekleme düğmesi)

enum RehberIcerigi {

    /// Android: getGuideContentsForNumber (`else -> emptyList()`).
    static func adimlar(rehberNo: Int) -> [RehberAdimi] {
        let sayfalar: [GuideContent] = GuideHelper.shared.getGuideContentsForNumber(guideNumber: Int32(rehberNo))
        var sonuc: [RehberAdimi] = []
        for sayfa in sayfalar {
            sonuc.append(adim(sayfa))
        }
        return sonuc
    }

    /// AbacusFragment.optimizeBeadIdsForReverse (ortak: GuideHelper.optimizeBeadIdsForReverse).
    static func tersiIcinSadelestir(_ kimlikler: [String]) -> [String] {
        return GuideHelper.shared.optimizeBeadIdsForReverse(beadIds: kimlikler)
    }

    // MARK: Ortak türden çeviri

    private static func adim(_ sayfa: GuideContent) -> RehberAdimi {
        var a: RehberAdimi = RehberAdimi(emote: sayfa.emote, metin: Metin.al(sayfa.textKey))
        // Android'de zorunlu tıklama hedefi her sayfada vurgu hedefiyle aynı öğe.
        let hedefOgesi: GuideTarget? = sayfa.bubbleAnimationTarget ?? sayfa.requiredClickTarget
        if let h = hedefOgesi {
            a.hedef = hedef(h)
        }
        if let renk = sayfa.bubbleAnimationColor {
            a.vurguRengi = RehberRenk.renk(UInt32(renk.intValue))
        }
        if let acik = sayfa.bubbleAnimationTintLight {
            a.vurguAcikRengi = RehberRenk.renk(UInt32(acik.intValue))
        }
        a.vurguOlcegi = CGFloat(sayfa.bubbleAnimationMaxScale)
        a.boncuklar = sayfa.beadIds ?? []
        a.geriBoncuklari = sayfa.backBeadIds
        a.bitisBoncuklari = sayfa.finishBeadIds ?? []
        a.tiklamaGerekli = sayfa.requiredClickTarget != nil
        a.tiklamaIlerletir = sayfa.requiredClickAdvancesGuide
        a.kuralSecimiBekler = sayfa.waitForRulesTableSelection
        return a
    }

    private static func hedef(_ h: GuideTarget) -> RehberHedefi? {
        if h == GuideTarget.firstNumberText { return .ilkSayi }
        if h == GuideTarget.secondNumberText { return .ikinciSayi }
        if h == GuideTarget.kontrolButton { return .kontrolDugmesi }
        if h == GuideTarget.rulesPanelButton { return .kuralPaneliDugmesi }
        if h == GuideTarget.rulesBookButton { return .kuralKitabiDugmesi }
        if h == GuideTarget.skipStepButton { return .adimAtlaDugmesi }
        if h == GuideTarget.abacusModeButton { return .abakusAyarDugmesi }
        return nil
    }
}
