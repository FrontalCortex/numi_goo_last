import Foundation
import SwiftUI

// Haritadan başlayan ders akışı: harita hedefi → (öğretici) → abaküs dersi / kör ders →
// sonuç ekranı → sandık → harita. Sonuç türleri arasındaki çeviri de buradadır.
//
// Android karşılıkları:
//   LessonAdapter.continueWithLesson                       → haritaDersiniBaslat
//   AbacusFragment.showLessonResult / …FalseOnly / showChestResult
//   BlindingLessonFragment.finishLessonAfterLastQuestion   → abakusDersiBitti / haritaKorDersiBitti
//   LessonResult claim, ChestResult continue, ChestFragment → sonucDevam
// Android bu sonuçlardan önce QuestionPanelFragment'ı (ders sonu soru özeti) gösterir; o ekran
// henüz yazılmadı, doğrudan sonuç ekranına geçilir.

extension UygulamaYoneticisi {

    // MARK: Harita hedefi

    /// Haritanın dışarı verdiği hedef. `panel`: hedeften hemen önce açık olan ders paneli.
    func haritaHedefiGeldi(_ hedef: HaritaHedefi, panel: HaritaPanelDurumu?) {
        haritaModeli.hedef = nil
        if asama != .kabuk {
            return
        }
        // Hedefin türü açık bir alandır (HaritaHedefi.tur); metinlere bakılmaz.
        switch hedef.tur {
        case .magaza:
            // Android: panel kapanır, mağaza açılır (enerji ya da kilit açmaya anahtar yetmedi).
            magazaAc()
            return
        case .liderlikTablosu:
            if let acik = panel {
                let partId: Int32 = haritaModeli.seciliPart ?? ilerleme.aktifPart
                // Android: RecordFragment dersin liste konumunu değil kalıcı kimliğini (stableId) alır.
                ac(.liderTablosu(partId: Int(partId), dersAnahtari: acik.oge.oge.stableId, baslik: acik.baslik))
            } else {
                yerTutucuAc(hedef.baslik, hedef.aciklama, hedef.ayrintilar)
            }
            return
        case .yarisPaneli:
            yerTutucuAc(hedef.baslik, hedef.aciklama, hedef.ayrintilar)
            return
        case .abakusDersi, .korDers, .ogretici, .ogreticiTekrari:
            break
        }
        // Ders hedefi: başlığı ve açıklaması ders panelininkiyle aynıdır.
        guard let acik = panel, acik.baslik == hedef.baslik else {
            yerTutucuAc(hedef.baslik, hedef.aciklama, hedef.ayrintilar)
            return
        }
        let tekrar: Bool = hedef.tur == .ogreticiTekrari
        haritaDersiniBaslat(acik, ogreticiIstendi: tekrar)
    }

    /// Android: LessonAdapter.continueWithLesson. Açılacak ekranı ortak kural seçer.
    private func haritaDersiniBaslat(_ panel: HaritaPanelDurumu, ogreticiIstendi: Bool) {
        let partId: Int32 = haritaModeli.seciliPart ?? ilerleme.aktifPart
        let sira: Int32 = ilerleme.haritaSirasi(panel.oge.oge, listeSirasi: panel.id)
        ilerleme.bellegeYukle(partId: partId)
        let oge = ilerleme.dersOgesi(sira: sira) ?? panel.oge.oge
        let hedefAdi: String = ogreticiIstendi ? "TUTORIAL" : ilerleme.baslamaHedefi(oge)
        if hedefAdi == "TUTORIAL" {
            let ogretici = YonetimOgreticiHedefi(
                baslik: panel.baslik,
                ogreticiNo: ilerleme.ogreticiNumarasi(oge),
                partId: partId,
                sira: sira,
                listeSirasi: panel.id
            )
            ogreticiAcildi(ogretici)
            ac(.ogretici(ogretici))
            return
        }
        if let ekran = dersEkrani(partId: partId, sira: sira, baslik: panel.baslik) {
            ac(ekran)
        }
    }

    /// OgreticiEkrani.bitti ("Devam Et" / "Eğitimi atla") ya da öğretici yer tutucusundaki "Derse geç"
    /// (Android: TutorialFragment dersi açar; isBlinding → BlindingLessonFragment, değilse AbacusFragment).
    func ogreticiGecildi(_ hedef: YonetimOgreticiHedefi, katman: Int) {
        if let ekran = dersEkrani(partId: hedef.partId, sira: hedef.sira, baslik: hedef.baslik) {
            degistir(katman, ekran)
        } else {
            kapat(katman)
        }
    }

    /// Bellekteki ders öğesinden abaküs dersi ya da kör ders ekranını kurar
    /// (Android: isBlinding == true → BlindingLessonFragment, değilse AbacusFragment).
    /// Ders açılamıyorsa yer tutucu açar ve nil döner.
    private func dersEkrani(partId: Int32, sira: Int32, baslik: String) -> YonetimEkrani? {
        ilerleme.bellegeYukle(partId: partId)
        guard let oge = ilerleme.dersOgesi(sira: sira) else {
            yerTutucuAc(baslik, "Ders bulunamadı", ["Kısım: \(partId)", "Harita sırası: \(sira)"])
            return nil
        }
        if ilerleme.korMu(oge) {
            let baslangic: KorDersBaslangici = ilerleme.korDersBaslangici(oge: oge, partId: partId, sira: sira)
            return .korDers(baslangic, .harita)
        }
        let abakus: AbakusDersBaslangici? = AbakusDersBaslangici.dersOgesinden(
            oge, partId: Int(partId), haritaSirasi: Int(sira)
        )
        guard let baslangic = abakus else {
            // Android'de çöken girdi (başlangıç adımı yok).
            yerTutucuAc(baslik, "Ders açılamadı", ["Kısım: \(partId)", "Harita sırası: \(sira)"])
            return nil
        }
        return .abakusDers(baslangic)
    }

    // MARK: Ders bitti → sonuç ekranı

    /// AbakusDersEkrani.bitti (Android: AbacusFragment.showQuestionPanel sonuç dinleyicisi).
    func abakusDersiBitti(_ sonuc: AbakusDersSonucu, baslangic: AbakusDersBaslangici, katman: Int) {
        let tur: SonucGirdisi.Tur
        switch sonuc.yonlendirme {
        case .dersSonucu:
            tur = .dersBasarili
        case .dersBasarisiz:
            tur = .dersBasarisiz
        case .sandikSonucu:
            tur = .sandikSonucu
        case .sandikBasarisiz:
            tur = .sandikBasarisiz
        }
        var girdi = SonucGirdisi(tur: tur, dogruSayisi: sonuc.dogruSayisi, toplamSoru: sonuc.soruSayisi)
        girdi.sure = sonuc.sure
        girdi.carpan = sonuc.carpan
        girdi.toplamPuan = sonuc.toplamPuan
        let baglam = YonetimSonucBaglami(
            partId: Int32(sonuc.partId),
            sira: Int32(sonuc.haritaSirasi),
            dersPuani: sonuc.dersPuani,
            toplamPuan: sonuc.toplamPuan,
            basariOrani: sonuc.basariOrani,
            tekrar: { [weak self] in
                self?.ac(.abakusDers(baslangic))
            }
        )
        sonucuGoster(girdi, baglam: baglam, katman: katman)
    }

    /// KorDersEkrani.bitti, harita dersi (Android: BlindingLessonFragment.finishLessonAfterLastQuestion).
    func haritaKorDersiBitti(_ sonuc: KorDersSonucu, baslangic: KorDersBaslangici, katman: Int) {
        let baglam = YonetimSonucBaglami(
            partId: Int32(sonuc.partId),
            sira: Int32(sonuc.dersIndeksi),
            dersPuani: sonuc.dersPuani,
            toplamPuan: sonuc.toplamPuan,
            basariOrani: sonuc.basariOrani,
            tekrar: { [weak self] in
                self?.ac(.korDers(baslangic, .harita))
            }
        )
        let tur: SonucGirdisi.Tur
        switch sonuc.yonlendirme {
        case .dersSonucu:
            tur = .dersBasarili
        case .dersSonucuBasarisiz:
            tur = sonuc.sandikBasarisiz ? .sandikBasarisiz : .dersBasarisiz
        case .sandikSonucu:
            if sonuc.yarisDersi {
                // Android: yarış dersinde ChestResult atlanır, doğrudan ChestFragment açılır.
                ilerleme.bellegeYukle(partId: baglam.partId)
                oduleGec(puan: sonuc.toplamPuan, baglam: baglam, katman: katman)
                return
            }
            tur = .sandikSonucu
        case .kapat, .gunlukSoruTamam, .gunlukSoruYanlis:
            // Yarış dersinin tamamlanmış hâli (raceBusyLevel == 0): ekran yalnızca kapanır.
            kapat(katman)
            return
        }
        var girdi = SonucGirdisi(tur: tur, dogruSayisi: sonuc.dogruSayisi, toplamSoru: sonuc.toplamSoru)
        girdi.sure = sonuc.sure
        girdi.carpan = sonuc.carpan
        girdi.toplamPuan = sonuc.toplamPuan
        sonucuGoster(girdi, baglam: baglam, katman: katman)
    }

    /// Ders katmanını sonuç ekranıyla değiştirir. Sandık sonucunda puan eşikleri ortak koddan
    /// çözülür ve rekor işlenir (Android: ChestResult.onViewCreated → record()).
    private func sonucuGoster(_ hamGirdi: SonucGirdisi, baglam: YonetimSonucBaglami, katman: Int) {
        var girdi = hamGirdi
        ilerleme.bellegeYukle(partId: baglam.partId)
        if girdi.tur == .sandikSonucu {
            let tavan: Int = ilerleme.puanTavani(sira: baglam.sira)
            girdi.puanTavani = max(1, tavan)
            girdi.ikiYildizEsigi = max(1, ilerleme.ikiYildizEsigi(sira: baglam.sira, puanTavani: girdi.puanTavani))
            ilerleme.sandikRekorunuIsle(sira: baglam.sira, toplamPuan: baglam.toplamPuan)
        }
        degistir(katman, .sonuc(girdi, baglam))
    }

    // MARK: Sonuç ekranı → sandık → harita

    /// SonucEkrani.devam ("Devam et" / "Çıkış").
    ///
    /// SERİ AKIŞI YOK (henüz yazılmadı): güncel Android'de sonuç → sandık → rozet → SERİ ekranı
    /// (NewStreakFragment / seri kutlama şeridi) → reklam tek öncelik kuyruğundan geçer
    /// (MainActivity.pumpPostLessonQueue, finalizeMapReturnAfterLessonClaim(isLessonTypeReturn)).
    /// Seri ekranı yazılınca `haritayaDon` ve sandığın `kapandi` dönüşünden önce sıraya girecek.
    func sonucDevam(_ girdi: SonucGirdisi, baglam: YonetimSonucBaglami, katman: Int) {
        ilerleme.bellegeYukle(partId: baglam.partId)
        switch girdi.tur {
        case .dersBasarili:
            // Android: LessonResult claim — ders zaten bitmişse sandık atlanır.
            if ilerleme.dersOduluAtlanir(sira: baglam.sira) {
                haritayaDon(katman)
            } else if ilkDerstenSonraGiriseGec() {
                // Android ChestFragment.onViewCreated: ilk öğreticinin dersi, oturum yok → başlangıç ekranı.
                return
            } else {
                oduleGec(puan: baglam.dersPuani, baglam: baglam, katman: katman)
            }
        case .sandikSonucu:
            // Android: ChestResult continue — bitmiş sandık tekrar oynandıysa yalnızca rekor güncellenir.
            if ilerleme.sandikTekrarOynandi(sira: baglam.sira) {
                ilerleme.tekrarSandiginiIsle(sira: baglam.sira, toplamPuan: baglam.toplamPuan)
                haritayaDon(katman)
            } else {
                oduleGec(puan: baglam.toplamPuan, baglam: baglam, katman: katman)
            }
        case .dersBasarisiz, .sandikBasarisiz:
            // Android LessonResultFalse claim: ilk öğreticinin dersi, oturum yok → başlangıç ekranı.
            if ilkDerstenSonraGiriseGec() {
                return
            }
            haritayaDon(katman)
        }
    }

    /// SonucEkrani.tekrar ("Tekrar dene").
    func sonucTekrar(baglam: YonetimSonucBaglami, katman: Int) {
        kapat(katman)
        baglam.tekrar()
    }

    /// Android: ChestFragment — harita ilerlemesi yazılır, ardından sandık (NewChestFragment, COMMON) açılır.
    private func oduleGec(puan: Int, baglam: YonetimSonucBaglami, katman: Int) {
        ilerleme.ilerlet(sira: baglam.sira, puan: puan, basariOrani: baglam.basariOrani)
        haritaModeli.yenile()
        degistir(katman, .sandik(SandikGirdisi(baslangicNadirligi: "COMMON"), kapandi: { _ in }))
    }

    private func haritayaDon(_ katman: Int) {
        haritaModeli.yenile()
        kapat(katman)
    }
}
