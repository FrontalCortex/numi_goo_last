import Foundation
import Shared
import SwiftUI

// Haritadaki ünite maratonu rehberi.
//
// Android karşılıkları:
//   ChestFragment.claim → MarathonGuideStore.scheduleIfEligible  → RehberOrtak.maratonuPlanla (YonetimDersAkisi.oduleGec)
//   MainActivity.runPostLessonQueue → showMarathonGuideStep      → UygulamaYoneticisi.maratonRehberiniDene
//   MapFragment.maybeShowPendingMarathonGuide / showGuidePanel   → RehberMaratonDurumu.dene / rehberiAc
//   GuidePanelView (harita; sağdan 500 ms girer, sola 300 ms çıkar)
//
// Akış: ilk ünite maratonu (1. kısım, ilk sandık) ilk kez bitince "bekliyor" bayrağı yazılır. Ders sonrası
// kuyruk sırası gelince (seri sorusundan sonra) harita istek alır: liste ilk sandığa kaydırılır, ders paneli
// o sandık için açılır, iki sayfalık rehber panelin altına gelir; son sayfada panel içindeki "Rekor"
// satırı vurgulanır ve dokunulunca liderlik tablosu açılır. Sayfa metinleri ve Sobi hâlleri ortak
// koddadır (marathonGuidePanelData).
//
// Android'den fark: Android rehber açıkken alt çubuk ve para panelini de kilitler (disableMainActivityViews);
// iOS'ta yalnızca harita alanı kilitlenir.

final class RehberMaratonDurumu: ObservableObject {
    /// Yönetici her istek gönderdiğinde artar; harita bu değişimi dinler.
    @Published private(set) var istekSayaci: Int = 0
    /// Haritanın kaydırması gereken satır (Android: scrollToPositionWithOffset(chestIndex, 0)).
    @Published private(set) var kaydirma: Int? = nil

    let akis: RehberAkisi = RehberAkisi(girisSuresi: 0.5, cikisSuresi: 0.3)
    private(set) var calisiyor: Bool = false

    /// MainActivity kuyruğundaki "rehber" adımı sırası geldi.
    func istekGonder() {
        istekSayaci += 1
    }

    /// MapFragment.maybeShowPendingMarathonGuide: bekleyen rehber varsa ve harita 1. kısmın listesini
    /// gösteriyorsa başlatır. Koşullar sağlanmıyorsa bayrak yerinde kalır; harita yeniden göründüğünde
    /// tekrar denenir.
    func dene(model: HaritaModeli) {
        if calisiyor || !RehberOrtak.depo.maratonBekliyor() {
            return
        }
        let kisim: Int32 = MarathonGuideLogic.shared.GUIDE_PART_ID
        guard let secili = model.seciliPart, secili == kisim else {
            return
        }
        let sira: Int = Int(MarathonGuideLogic.shared.firstMarathonLessonIndex())
        var bulunan: HaritaOgesi? = nil
        for oge in model.ogeler {
            if oge.id == sira {
                bulunan = oge
            }
        }
        guard let sandik = bulunan, sandik.tur == .sandik else {
            return
        }
        calisiyor = true
        // Liste sandığa kayar, ardından ders paneli açılır, panel yerleşince rehber girer
        // (Android: önce panel, sonra balon panelin altına yerleştirilir ve içeri kayar).
        kaydirma = sira
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { [weak self, weak model] in
            guard let kendisi = self, kendisi.calisiyor, let harita = model else { return }
            harita.ogeyeDokun(sandik)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { [weak kendisi, weak harita] in
                guard let k = kendisi, k.calisiyor, let h = harita else { return }
                k.rehberiAc(model: h)
            }
        }
    }

    /// MapFragment.showGuidePanel: ortak koddaki iki sayfa; son sayfa "Rekor" satırını işaret eder.
    private func rehberiAc(model: HaritaModeli) {
        let veri: [GuidePanelData] = GuidePanelDataKt.marathonGuidePanelData()
        var adimlar: [RehberAdimi] = []
        for (sira, parca) in veri.enumerated() {
            var adim: RehberAdimi = RehberAdimi(emote: parca.emote, metin: parca.text)
            if sira == veri.count - 1 {
                adim.hedef = .rekorSatiri
                adim.vurguOlcegi = 1.08
                adim.tiklamaGerekli = true
            }
            adimlar.append(adim)
        }
        akis.sifirla()
        akis.hedefTiklandi = { [weak model] (hedef: RehberHedefi) -> Void in
            // recordLayout tıklaması: ders paneli kapanır, liderlik tablosu açılır.
            if hedef == .rekorSatiri, let acik = model?.panel {
                model?.rekoraDokun(acik)
            }
        }
        akis.bitti = { [weak self] (_: Bool) -> Void in
            self?.bitir()
        }
        // MarathonGuideStore.markShown rehber açılırken yazılır.
        RehberOrtak.depo.maratonuGosterildiOlarakIsaretle()
        akis.baslat(adimlar, gecikme: 0, acildi: nil)
    }

    private func bitir() {
        calisiyor = false
        kaydirma = nil
    }

    /// Harita ekrandan çıkınca yarım kalan rehber bırakılır (kayıt zaten yazıldı).
    func durdur() {
        if calisiyor {
            akis.sifirla()
            bitir()
        }
    }
}

extension UygulamaYoneticisi {
    /// Ders sonrası kuyruğun "rehber" adımı (Android: runPostLessonQueue → showMarathonGuideStep). Yalnızca
    /// kabukta ve üstte ekran yokken; bekleyen bayrak yoksa hiçbir şey yapmaz. Seri sorusu açıldıysa o
    /// kapanınca yeniden denenir.
    func maratonRehberiniDene() {
        if !RehberOrtak.depo.maratonBekliyor() {
            return
        }
        // Kapanan ders / sandık katmanları yığından kalkana kadar kısa bir bekleme.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { [weak self] in
            guard let yonetici = self else { return }
            if yonetici.asama != .kabuk || !yonetici.katmanlar.isEmpty {
                return
            }
            RehberOrtak.maraton.istekGonder()
        }
    }
}
