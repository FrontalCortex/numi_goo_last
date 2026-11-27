import Foundation
import Shared
import SwiftUI

/// Seri ekranındaki açık pencere.
enum SeriPenceresi: Equatable {
    /// Günlük hedef (StreakFragment.showGoalPicker).
    case hedef
    /// Hatırlatma saati (showReminderPicker).
    case hatirlatma
    /// "Başka bir saat": 24 saatlik ızgara (StreakViews.showHourPicker).
    case saat
}

/// Seri ekranının (StreakFragment) görünüm modeli. Kararlar ortak koddan (StreakScreenLogic) gelir.
final class SeriEkranModeli: ObservableObject {
    @Published private(set) var veri: SeriEkranVerisi
    @Published var pencere: SeriPenceresi? = nil
    @Published private(set) var bildirim: String? = nil
    /// Ödül toplama sürüyor (çift dokunuşu eler).
    @Published private(set) var toplaniyor: Bool = false

    let servis: SeriServisi
    private var dinleyici: UUID? = nil
    private var bildirimSayaci: Int = 0
    /// Android: FREEZE_ROW_CLICK_GAP_MS (700 ms).
    private var sonDondurmaDokunusu: Date = .distantPast

    init(servis: SeriServisi) {
        self.servis = servis
        self.veri = servis.ekranVerisi()
    }

    deinit {
        if let kimlik = dinleyici {
            servis.dinlemeyiBirak(kimlik)
        }
    }

    /// Android: onResume → render + syncPendingDays + refreshFromServer.
    func basla() {
        if dinleyici == nil {
            dinleyici = servis.dinle { [weak self] in
                self?.ciz()
            }
        }
        ciz()
        servis.esitle(tamam: { [weak self] in
            self?.ciz()
        })
    }

    func ciz() {
        veri = servis.ekranVerisi()
    }

    // MARK: Ödüller

    func tasiTopla(_ tas: Int, cuzdanDegisti: @escaping () -> Void) {
        if toplaniyor { return }
        toplaniyor = true
        servis.kilometreTasiTopla(tas) { [weak self] sonuc in
            guard let self = self else { return }
            self.toplaniyor = false
            self.goster(sonuc.mesaj)
            if sonuc.basarili {
                cuzdanDegisti()
                self.ciz()
            }
        }
    }

    func meydanOkumaTopla(cuzdanDegisti: @escaping () -> Void) {
        if toplaniyor { return }
        toplaniyor = true
        servis.meydanOkumaOdulunuTopla { [weak self] sonuc in
            guard let self = self else { return }
            self.toplaniyor = false
            self.goster(sonuc.mesaj)
            if sonuc.basarili {
                cuzdanDegisti()
                self.ciz()
            }
        }
    }

    // MARK: Dondurma satırı

    /// Yalnızca elde yokken; art arda dokunuş elenir. true dönerse mağaza açılmalı.
    func dondurmaSatirinaDokunuldu() -> Bool {
        if !veri.ui.freezeRowClickable { return false }
        let simdi = Date()
        if simdi.timeIntervalSince(sonDondurmaDokunusu) < 0.7 { return false }
        sonDondurmaDokunusu = simdi
        return true
    }

    // MARK: Pencereler

    var hedefSatirlari: [StreakOptionRowUi] {
        return StreakScreenLogic.shared.goalRows(selected: Int32(servis.hedefDakika))
    }

    var hatirlatmaSatirlari: [StreakOptionRowUi] {
        return StreakScreenLogic.shared.reminderRows(selected: Int32(servis.hatirlatmaSaati))
    }

    func hedefSecildi(_ dakika: Int) {
        pencere = nil
        servis.hedefiDegistir(dakika)
        ciz()
    }

    func hatirlatmaSatiriSecildi(_ deger: Int) {
        if deger == Int(StreakScreenLogic.shared.CUSTOM_HOUR_ROW) {
            pencere = .saat
            return
        }
        saatSecildi(deger)
    }

    /// Android: applyReminderHour — saat kaydedilir, sunucuya gider; izin (Android 13+) burada sorulur.
    func saatSecildi(_ saat: Int) {
        pencere = nil
        servis.hatirlatmaSaatiniDegistir(saat)
        ciz()
    }

    // MARK: Bildirim (Android Toast.LENGTH_LONG ≈ 3,5 sn)

    private func goster(_ metin: String) {
        bildirimSayaci += 1
        let sira: Int = bildirimSayaci
        bildirim = metin
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.5) { [weak self] in
            guard let self = self, self.bildirimSayaci == sira else { return }
            self.bildirim = nil
        }
    }
}
