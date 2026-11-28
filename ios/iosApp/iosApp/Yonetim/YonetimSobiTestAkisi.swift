import Combine
import Foundation
import SwiftUI

// Sobi'nin günlük testinin uygulamaya bağlandığı yerler (SobiTest/ ekranlarını yönetici bağlar).
//
// Android karşılıkları:
//   MapSobiTestButton.open            → sobiTestSayfasiniAc (haritadaki düğme; SobiTestOrtak.dugme.acildi)
//   SobiDailyTest.start               → sobiTestiBaslat (MainActivity.showAbacusOverlayFragment(HomeworkPlayerFragment.newSobiTest))
//   HomeworkPlayerFragment.openSobiChest → sobiTestBitti(.sandik): oynatıcı kapanır, sıradan sandık
//                                       (NewChestFragment, kaynak CHEST_SOURCE_SOBI_TEST)
//   SobiTestSheet.offerPro            → planKarsilastirmaAc (Android SobiProTrialFragment)
// Android'deki geçiş reklamı kapısı (shouldShowAdOnReturn) iOS'ta yok.

/// Sobi testi katmanları (YonetimEkrani.sobiTest).
enum YonetimSobiTestKatmani {
    /// Alttan açılan panel (SobiTestSheet); altındaki harita görünür kalır.
    case sayfa
    /// Oynatıcı (HomeworkPlayerFragment, Sobi testi kipi).
    case oynatici(SobiTestBaslangici)
}

extension UygulamaYoneticisi {

    /// init'te bir kez: haritadaki düğme ilerleme deposunu okusun ve dokununca paneli açsın.
    func sobiTestiBagla() {
        SobiTestOrtak.ilerlemeKaynagi = ilerleme
        SobiTestOrtak.dugme.acildi = { [weak self] in
            self?.sobiTestSayfasiniAc()
        }
        // Ders (ya da test) dönüşünde harita yeniden görünür ama onAppear gelmez (katmanlar ZStack'te):
        // yığın boşalınca açılma ve nokta yeniden hesaplanır.
        SobiTestOrtak.katmanAboneligi = $katmanlar
            .map { (liste: [YonetimKatmani]) -> Bool in liste.isEmpty }
            .removeDuplicates()
            .filter { (bos: Bool) -> Bool in bos }
            .receive(on: DispatchQueue.main)
            .sink { (_: Bool) in
                SobiTestOrtak.dugme.yenile(balonla: false)
            }
    }

    func sobiTestSayfasiniAc() {
        if let ust = katmanlar.last, case .sobiTest = ust.ekran {
            return
        }
        ac(.sobiTest(.sayfa))
    }

    /// Panel kapanır, oynatıcı açılır.
    func sobiTestiBaslat(_ baslangic: SobiTestBaslangici, sayfaKatmani: Int) {
        kapat(sayfaKatmani)
        ac(.sobiTest(.oynatici(baslangic)))
    }

    /// Oynatıcı yerinde sandıkla değiştirilir; sandık kapanınca harita (düğmenin noktası tazelenir).
    func sobiTestBitti(_ bitis: SobiTestBitis, katman: Int) {
        switch bitis {
        case .sandik:
            let girdi = SandikGirdisi(baslangicNadirligi: "COMMON")
            degistir(katman, .sandik(girdi, kapandi: { _ in
                SobiTestOrtak.dugme.yenile(balonla: false)
            }))
        }
    }
}

extension YonetimKatmanGorunumu {

    @ViewBuilder
    func sobiTestKatmani(_ tur: YonetimSobiTestKatmani) -> some View {
        switch tur {
        case .sayfa:
            sobiTestSayfasi
        case .oynatici(let baslangic):
            sobiTestOynaticisi(baslangic)
        }
    }

    private var sobiTestSayfasi: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SobiTestSayfasi(
            kapat: { nesne.kapat(no) },
            basla: { baslangic in nesne.sobiTestiBaslat(baslangic, sayfaKatmani: no) },
            proAc: { nesne.planKarsilastirmaAc() }
        )
    }

    private func sobiTestOynaticisi(_ baslangic: SobiTestBaslangici) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        // "Sobi ile analiz et": ders ekranlarındakiyle aynı bağlantı (SobiAnaliz/ penceresi üstte açılır).
        let analiz = AbakusSobiAnalizBaglantisi(
            yapilabilir: { sayilar in SobiAnalizModeli.analizYapilabilir(sayilar) },
            istendi: { istek in nesne.ac(.sobiAnaliz(istek)) }
        )
        return SobiTestEkrani(
            baslangic: baslangic,
            sobiAnalizi: analiz,
            bitti: { bitis in nesne.sobiTestBitti(bitis, katman: no) },
            kapat: { nesne.kapat(no) }
        )
    }
}
