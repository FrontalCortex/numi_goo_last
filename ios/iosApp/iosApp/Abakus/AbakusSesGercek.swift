import Foundation
import Shared

// Abaküs sesleri ve titreşimi: AbakusBoncukSesi / AbakusDersHizmetleri'nin gerçek uygulamaları.
//
// Android: abacus/AbacusSoundPlayer.kt (boncuk tık sesi), AbacusFragment.playSound
// (AbacusFragment.kt:3044-3059, 3164: correct_answer_sound 0.3, incorrect_answer_sound 1.0).
//  - Boncuk sesi: seçili ses (1...5; "abacus_sound_choice", varsayılan 1) çalınır; boncuk etkinleşirken
//    perde 1,0, geri alınırken 0,75; ses düzeyi 4. ses için 1,0, diğerleri 0,6. Seçim, ad, düzey ve
//    perde kuralları ortak koddaki `AbacusSoundPlayer`'dadır (AbacusPurchases.kt:300-337).
//  - Ayar kapalıysa ("sound_enabled") çalmaz: `SesCalar` denetler.
//  - Titreşim Android'de bu ekranda yoktur; iOS'ta da yoktur.

/// Boncuk tık sesi. `AbakusGorunumu` bunu güçlü tutar; tahta zayıf tutar.
final class GercekAbakusBoncukSesi: AbakusBoncukSesi {
    /// Öğretici ve ekranlar arasında paylaşılan örnek (zayıf başvurulara karşı ömrü uzun).
    static let ortak: GercekAbakusBoncukSesi = GercekAbakusBoncukSesi()

    /// Android AppPrefs "abacus_sound_choice" (1...5).
    static let secimAnahtari: String = "abacus_sound_choice"

    private let ayarlar: UserDefaults

    init(ayarlar: UserDefaults = UserDefaults.standard) {
        self.ayarlar = ayarlar
    }

    /// Kayıtlı seçim, 1...5 aralığına kırpılmış (kayıt yoksa 1).
    static func seciliIndeks(ayarlar: UserDefaults = UserDefaults.standard) -> Int32 {
        var kayitli: KotlinInt? = nil
        if let sayi = ayarlar.object(forKey: GercekAbakusBoncukSesi.secimAnahtari) as? Int {
            kayitli = KotlinInt(int: Int32(truncatingIfNeeded: sayi))
        }
        return AbacusSoundPlayer.shared.selectedIndex(storedChoice: kayitli)
    }

    /// Android preload: abaküs ekranı kurulurken seçili sesi hazırlar.
    func hazirla() {
        let indeks: Int32 = GercekAbakusBoncukSesi.seciliIndeks(ayarlar: ayarlar)
        let ad: String = AbacusSoundPlayer.shared.soundResFor(index: indeks)
        SesCalar.ortak.hazirla(ad: ad)
    }

    /// Android AbacusSoundPlayer.choose: ayar ekranında seçim yapılınca kaydeder ve hemen çalar.
    static func sec(indeks: Int32, ayarlar: UserDefaults = UserDefaults.standard) {
        ayarlar.set(Int(indeks), forKey: GercekAbakusBoncukSesi.secimAnahtari)
        let ses: GercekAbakusBoncukSesi = GercekAbakusBoncukSesi(ayarlar: ayarlar)
        ses.sesiCal(etkinlesiyor: true)
    }

    func boncukSesi(etkinlesiyor: Bool) {
        sesiCal(etkinlesiyor: etkinlesiyor)
    }

    private func sesiCal(etkinlesiyor: Bool) {
        let indeks: Int32 = GercekAbakusBoncukSesi.seciliIndeks(ayarlar: ayarlar)
        let ad: String = AbacusSoundPlayer.shared.soundResFor(index: indeks)
        let duzey: Float = AbacusSoundPlayer.shared.playbackVolumeFor(index: indeks)
        let hiz: Float = AbacusSoundPlayer.shared.playbackRate(activating: etkinlesiyor)
        SesCalar.ortak.cal(ad: ad, sesDuzeyi: duzey, hiz: hiz)
    }
}

/// Abaküs dersinin hizmetleri: ses ve titreşim gerçek, geri kalanı `temel`e devredilir (Firebase,
/// enerji, ilerleme kaydı henüz örnek uygulamada; gerçekleri bağlanınca `temel` olarak verilir).
final class GercekAbakusDersHizmetleri: AbakusDersHizmetleri {
    private let temel: AbakusDersHizmetleri
    private let boncuk: GercekAbakusBoncukSesi

    init(temel: AbakusDersHizmetleri = SahteAbakusDersHizmetleri()) {
        self.temel = temel
        self.boncuk = GercekAbakusBoncukSesi.ortak
        self.boncuk.hazirla()
        SesCalar.ortak.hazirla(ad: "correct_answer_sound")
        SesCalar.ortak.hazirla(ad: "incorrect_answer_sound")
    }

    var cevrimici: Bool {
        return temel.cevrimici
    }

    func boncukSesi(etkinlesiyor: Bool) {
        boncuk.boncukSesi(etkinlesiyor: etkinlesiyor)
    }

    func sesCal(_ ses: AbakusDersSesi) {
        switch ses {
        case .dogru:
            SesCalar.ortak.cal(ad: "correct_answer_sound", sesDuzeyi: 0.3)
        case .yanlis:
            SesCalar.ortak.cal(ad: "incorrect_answer_sound", sesDuzeyi: 1.0)
        }
    }

    /// Android'de bu ekranda titreşim yok: bilerek boş.
    func titret(_ tur: AbakusTitresimi) {}

    func cevrimdisiGoster() {
        temel.cevrimdisiGoster()
    }

    func ogrenmeSuresiniKaydet(milisaniye: Int) {
        temel.ogrenmeSuresiniKaydet(milisaniye: milisaniye)
    }

    func enerjiDus(baslangic: AbakusDersBaslangici) {
        temel.enerjiDus(baslangic: baslangic)
    }

    func ilerlemeyiKaydet(sonuc: AbakusDersSonucu) {
        temel.ilerlemeyiKaydet(sonuc: sonuc)
    }

    func soruGirisiniKaydet(baslangic: AbakusDersBaslangici) {
        temel.soruGirisiniKaydet(baslangic: baslangic)
    }

    func cevapsizTerkiKaydet(baslangic: AbakusDersBaslangici) {
        temel.cevapsizTerkiKaydet(baslangic: baslangic)
    }
}
