import SwiftUI

// Ders içi kural durumu: kitap açık mı, panel açık mı, panelde hangi tablo.
//
// Android: abacus/LessonRulesPanel.kt (panel durumu: activeTable, selectedMultiplicationDigit, show / hideAll /
// toggle / select) ve AbacusFragment / BlindingLessonFragment içindeki sihirli değnek ve kitap düğmesi
// dinleyicileri (onRulesPanelButtonClicked, openRulesBook, applyRulesTableSelection).
final class KuralModeli: ObservableObject {
    let ders: KuralDersBilgisi
    /// Kitabın açacağı tablolar; ders sabit olduğundan açılışta bir kez hesaplanır.
    let kitapAyari: KuralKitabiAyari

    @Published private(set) var kitapAcik: Bool = false
    @Published private(set) var panelAcik: Bool = false
    /// LessonRulesPanel.activeTable.
    @Published private(set) var aktifTablo: KuralTablosu = .yok
    /// LessonRulesPanel.selectedMultiplicationDigit.
    @Published private(set) var carpimBasamagi: Int? = nil

    init(ders: KuralDersBilgisi, baslangic: KuralBaslangicDurumu = .kapali) {
        self.ders = ders
        self.kitapAyari = ders.kitapAyari()
        if !ders.seciciVar {
            aktifTablo = ders.siraninPanelTablosu()
        }
        switch baslangic {
        case .kapali:
            break
        case .kitap:
            kitapAcik = true
        case .panel(let secim):
            if let s = secim {
                uygula(s)
            }
            panelAcik = icerikVar
        }
    }

    /// Android: hasRulesPanelContentToShow (çarpımda rakam da seçilmiş olmalı).
    var icerikVar: Bool {
        if aktifTablo == .yok { return false }
        if aktifTablo == .carpim && carpimBasamagi == nil { return false }
        return true
    }

    // MARK: Düğmeler

    /// rulesPanelButton (sihirli değnek). Android: onRulesPanelButtonClicked — seçici varken gösterilecek
    /// tablo yoksa kitabı açar; yoksa paneli açar / kapatır (toggleRulesPanelTable).
    @discardableResult
    func panelDugmesiDokunuldu() -> KuralEylemi {
        if !ders.seciciVar {
            aktifTablo = ders.siraninPanelTablosu()
        }
        if ders.seciciVar && !icerikVar {
            kitabiAc()
            return .kitapAcildi
        }
        if aktifTablo == .yok {
            return .yok
        }
        if panelAcik {
            panelAcik = false
            return .panelKapandi
        }
        panelAcik = icerikVar
        return icerikVar ? .panelAcildi : .yok
    }

    /// Android: closeRulesPanelIfOpen / hideAll.
    func panelKapat() {
        if panelAcik {
            panelAcik = false
        }
    }

    /// Android: rulesBookButton → openRulesBook (slide_down, 300 ms, yavaşlayarak).
    func kitabiAc() {
        withAnimation(.easeOut(duration: 0.3)) {
            kitapAcik = true
        }
    }

    /// Android: RulesFragment.closeWithAnimation (slide_up, 300 ms, hızlanarak).
    func kitabiKapat() {
        withAnimation(.easeIn(duration: 0.3)) {
            kitapAcik = false
        }
    }

    /// Kitapta tabloya dokunuldu. Android: dinleyici seçimi uygulayıp paneli gösterir, ardından kitap kapanır.
    func tabloSecildi(_ secim: KuralSecimi) {
        uygula(secim)
        panelAcik = true
        kitabiKapat()
    }

    /// LessonRulesPanel.select
    private func uygula(_ secim: KuralSecimi) {
        switch secim {
        case .bes, .cikarmaBes:
            aktifTablo = .bes
        case .onBes:
            aktifTablo = .onBes
        case .on:
            aktifTablo = .on
        case .boncuk:
            aktifTablo = .boncuk
        case .onCikarma:
            aktifTablo = .onCikarma
        case .boncukCikarma:
            aktifTablo = .boncukCikarma
        case .carpim(let basamak):
            carpimBasamagi = basamak
            aktifTablo = .carpim
        }
    }
}
