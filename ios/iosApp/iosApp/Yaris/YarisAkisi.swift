import Combine
import Foundation
import Shared
import SwiftUI

// Yarış panelinin uygulama akışına bağlanması (yönetici tarafı).
//
// Android: PartSelectionFragment.openMapForPart (kısım 7/8 → TYPE_RACE kukla öğesiyle showRacePanel) ve
// haritadaki yarış bayrağı (LessonAdapter.RaceViewHolder → showRacePanel) paneli açar; panelden BAŞLAT
// → LessonAdapter.onRaceStartClicked: GlobalLessonData.initialize(racePartId) + mapFragmentStepIndex /
// lessonStep ayarı + BlindingLessonFragment. Yarış bitince kör dersin sonucu haritadaki derslerle aynı
// yoldan işlenir (Yonetim/YonetimDersAkisi.swift: haritaKorDersiBitti → ödül → ChestFragmentLogic
// yarış dalları); panelin altındaki katman olduğu gibi kalır ve ilerleme değişince listeyi yeniden okur.

extension UygulamaYoneticisi {

    /// Haritanın yarış hedefi (kısım 7/8 kartı ya da yarış bayrağı) → paneli açar.
    func yarisPaneliniAc(_ hedef: HaritaHedefi) {
        // Android refreshRacePanelIfOpen: racePartId bulunamazsa 7 varsayılır.
        let partId: Int32 = hedef.yarisPartId ?? 7
        let girdi = YarisPaneliGirdisi(
            partId: partId,
            baslik: hedef.baslik,
            partSecimindenMi: hedef.yarisPartSecimindenMi,
            geriPartId: hedef.yarisGeriPartId
        )
        ac(.yarisPaneli(girdi))
    }

    /// Panelin ilerleme deposuna ve ders başlatmaya bağlantısı.
    func yarisBaglantisi() -> YarisPaneliBaglantisi {
        let yenileme: AnyPublisher<Void, Never> = haritaModeli.objectWillChange.eraseToAnyPublisher()
        return YarisPaneliBaglantisi(
            ogeler: { [weak self] (partId: Int32) -> [LessonItem] in
                guard let nesne = self else {
                    return []
                }
                // Android: GlobalLessonData.loadLessonItemsForPart(context, racePartId).
                let bulut: [LessonItem]? = nesne.ilerleme.bulutOgeleri(partId: partId)
                return GlobalLessonData.shared.loadLessonItemsForPart(partId: partId, cloudItems: bulut)
            },
            baslat: { [weak self] (partId: Int32, sira: Int, oge: LessonItem) -> Void in
                self?.yarisDersiniBaslat(partId: partId, sira: sira, oge: oge)
            },
            yenileme: yenileme
        )
    }

    /// Android: LessonAdapter.onRaceStartClicked — kısmın listesi belleğe alınır, yarış kör ders
    /// ekranında (BlindingLessonFragment) açılır. Yarış dersleri `isBlinding` taşımaz: sayılar gösterilir,
    /// cevap abaküsle verilir (KorDersEkrani.blindingOrRace). Kilitli (2) yarışta da aynı yol işler;
    /// bitişte ortak kural onu yalnızca kendisi için TAMAMLANDI yapar.
    func yarisDersiniBaslat(partId: Int32, sira: Int, oge: LessonItem) {
        ilerleme.bellegeYukle(partId: partId)
        let haritaSirasi: Int32 = ilerleme.haritaSirasi(oge, listeSirasi: sira)
        guard let guncel = ilerleme.dersOgesi(sira: haritaSirasi) else {
            yerTutucuAc(oge.title, "Yarış bulunamadı", ["Kısım: \(partId)", "Sıra: \(sira)"])
            return
        }
        let baslangic: KorDersBaslangici = ilerleme.korDersBaslangici(oge: guncel, partId: partId, sira: haritaSirasi)
        ac(.korDers(baslangic, .harita))
    }

    /// Panel kapandı. Android: haritadaki bayraktan açıldıysa aktif kısım `backRaceId`'ye döner,
    /// liste yeniden yüklenir (GlobalLessonData.initialize + updateItems); kısım seçiminden açıldıysa
    /// hiçbir şey değişmez.
    func yarisPaneliKapandi(_ girdi: YarisPaneliGirdisi, katman: Int) {
        kapat(katman)
        if !girdi.partSecimindenMi, let geri = girdi.geriPartId {
            ilerleme.bellegeYukle(partId: geri)
            haritaModeli.yenile()
        }
    }
}

/// Yönetici yığınındaki yarış paneli katmanı (Yonetim/UygulamaKokGorunumu bunu çağırır).
@MainActor
struct YarisKatmaniGorunumu: View {
    let girdi: YarisPaneliGirdisi
    let katman: Int
    let yonetici: UygulamaYoneticisi

    var body: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman
        let g: YarisPaneliGirdisi = girdi
        return YarisPaneliEkrani(
            girdi: g,
            baglanti: nesne.yarisBaglantisi(),
            cuzdan: MagazaBagimliliklar.sahte.cuzdan,
            kapat: { nesne.yarisPaneliKapandi(g, katman: no) }
        )
    }
}
