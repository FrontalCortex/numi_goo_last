import Foundation
import SwiftUI

/// Görev ödül sandığını açma isteği. Sandık ekranını başka bir oturum yazar; bu ekran yalnızca
/// isteği dışarı verir ve sandık kapanınca `kapandi` çağrılmasını bekler.
/// Android: MissionsFragment.onQuestClicked → NewChestFragment.newInstance(startRarity) ve
/// "chest_closed" fragment sonucu.
struct GorevlerSandikIstegi: Identifiable {
    let id = UUID()
    /// Sandığın başlangıç nadirliği: haftalık görev "RARE", günlük görev "COMMON".
    let nadirlik: String
    /// Kapalı sandığın görsel adı.
    let simge: String
    /// Görev başlığı (yer tutucu ekranda gösterilir).
    let gorevBasligi: String
    /// Sandık ekranı kapanınca çağrılır: ödül alındı olarak işaretlenir ve liste yenilenir.
    let kapandi: () -> Void
}

/// Görevler ekranının görünüm modeli. Android karşılığı: MissionsFragment.
final class GorevlerModeli: ObservableObject {
    @Published private(set) var haftalik: [GorevlerSatiri] = []
    @Published private(set) var gunluk: [GorevlerSatiri] = []
    @Published private(set) var haftalikSayac: String = ""
    @Published private(set) var gunlukSayac: String = ""
    /// Sandık ekranı dışarıya bağlanmadıysa gösterilen yer tutucunun isteği.
    @Published var yerTutucuIstegi: GorevlerSandikIstegi? = nil

    private let veri: GorevlerVeriKaynagi
    private let odul: GorevlerOdulServisi
    /// Android: isVideoFlowOpen — sandık akışı açıkken ikinci dokunuş yok sayılır.
    private var akisAcik = false

    init(veri: GorevlerVeriKaynagi, odul: GorevlerOdulServisi) {
        self.veri = veri
        self.odul = odul
    }

    /// Android: updateMissionsUI (onViewCreated ve onResume'da çağrılır).
    func yenile() {
        let durum: GorevlerDurumu = veri.durumuOku()
        haftalik = durum.haftalik
        gunluk = durum.gunluk
        haftalikSayac = Metin.al(durum.haftalikSayac.metinAdi, durum.haftalikSayac.deger)
        gunlukSayac = Metin.al(durum.gunlukSayac.metinAdi, durum.gunlukSayac.deger)
    }

    /// Android: onQuestClicked. `disariAc` verilmişse sandık ekranı dışarıda açılır; verilmemişse
    /// yer tutucu gösterilir.
    func satiraDokunuldu(_ satir: GorevlerSatiri, disariAc: ((GorevlerSandikIstegi) -> Void)?) {
        if !satir.alinabilir || akisAcik {
            return
        }
        akisAcik = true
        // Sunucu isteği ekran açılmadan önce başlar (Android: ServerRewards.prefetchChest).
        odul.sandikOnIstegi(nadirlik: satir.sandikNadirligi)

        let istek = GorevlerSandikIstegi(
            nadirlik: satir.sandikNadirligi,
            simge: satir.simge,
            gorevBasligi: Metin.al(satir.baslikAdi),
            kapandi: { [weak self] in
                self?.sandikKapandi(satir)
            }
        )
        if let ac = disariAc {
            ac(istek)
        } else {
            yerTutucuIstegi = istek
        }
    }

    /// Android: "chest_closed" sonucu → markMissionRewardClaimed + updateMissionsUI.
    private func sandikKapandi(_ satir: GorevlerSatiri) {
        if !akisAcik {
            return
        }
        veri.odulAlindiIsaretle(satir)
        akisAcik = false
        yerTutucuIstegi = nil
        yenile()
    }
}
