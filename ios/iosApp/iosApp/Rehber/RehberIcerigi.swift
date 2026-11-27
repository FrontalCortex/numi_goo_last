import SwiftUI

// Ders içi rehberin sayfaları.
//
// KAYNAK: app/src/main/java/com/example/app/GuideHelper.kt → SharedGuideHelper.getGuideContentsForNumber
// (birebir kopya: sayfa sırası, Sobi hâlleri, metin anahtarları, renkler, boncuk kimlikleri).
// NOT: Bu kural Android'de yalnızca `app/` içinde duruyor, ortak kodda (ios/shared) YOK. Şimdilik burada tek
// yerde tutuluyor; ortak koda taşınmalı (`GuidePanelData`'ya benzer, ama işaret edilen öğe + boncuk kimlikleri
// alanlarıyla). Metinler `strings.xml` adlarıyla Metin.al'dan gelir.
//
// Rehber numaraları (LessonItem.abacusGuideNumber):
//   1 toplama (abaküse ilk sayıyı yaz, ikinciyi ekle, Kontrol Et)   2 sihirli değnek (kural tablosu)
//   3 kurallar kitabı                                                4 kural tablosunu abaküse alma
//   5 kör derste "sayıyı geç" oku                                    6 abaküs boyutu (ölçekleme düğmesi)
// Ders verisinde atananlar: 6 → ilk ders (GlobalLessonData), 1, 2, 3, 4 (bkz. GlobalLessonDataKatalog.kt).

enum RehberIcerigi {

    /// Android: `else -> emptyList()`.
    static func adimlar(rehberNo: Int) -> [RehberAdimi] {
        switch rehberNo {
        case 1:
            return toplama()
        case 2:
            return sihirliDegnek()
        case 3:
            return kurallarKitabi()
        case 4:
            return kuralTablosunuAlma()
        case 5:
            return sayiyiGecme()
        case 6:
            return abakusBoyutu()
        default:
            return []
        }
    }

    // MARK: Renkler (Color.parseColor karşılıkları)

    private static let sari: Color = RehberRenk.renk(0xFFFF00)
    private static let kuralYesili: Color = RehberRenk.renk(0x8BC34A)
    private static let kuralAcikYesili: Color = RehberRenk.renk(0xDFF0D4)
    private static let okKoyu: Color = RehberRenk.renk(0x263C31)
    private static let okAcik: Color = RehberRenk.renk(0x00FF8C)

    // MARK: Rehber 1 — toplama

    /// Sobi anlatıyor; yazı düz geliyor (yazma efekti ve ses yok).
    private static func toplama() -> [RehberAdimi] {
        let ilk: RehberAdimi = RehberAdimi(
            emote: "TEACH_TALK",
            metin: Metin.al("ui_bu_testte_toplama_islemini_abakuste_yapacaks")
        )
        var ikinci: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_once_abakuse_ilk_sayiyi_yazip")
        )
        ikinci.hedef = .ilkSayi
        ikinci.vurguRengi = sari
        ikinci.boncuklar = ["rod4BottomBead4"]
        ikinci.geriBoncuklari = ["rod4BottomBead1"]

        var ucuncu: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_sonrasinda_ikinci_sayiyi_ekle")
        )
        ucuncu.hedef = .ikinciSayi
        ucuncu.vurguRengi = sari
        ucuncu.boncuklar = ["rod4TopBead"]

        var dorduncu: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_ve_islem_bitince_kontrol_et_butonuna")
        )
        dorduncu.hedef = .kontrolDugmesi
        dorduncu.vurguOlcegi = 1.1

        var besinci: RehberAdimi = RehberAdimi(
            emote: "TEACH_WARN",
            metin: Metin.al("ui_sakin_aklindan_toplayip_o_sayiyi_abakuse")
        )
        besinci.bitisBoncuklari = ["rod4BottomBead1", "rod4TopBead"]
        return [ilk, ikinci, ucuncu, dorduncu, besinci]
    }

    // MARK: Rehber 2 — sihirli değnek / kural tablosu

    private static func sihirliDegnek() -> [RehberAdimi] {
        let ilk: RehberAdimi = RehberAdimi(
            emote: "TEACH_TALK",
            metin: Metin.al("ui_kurallari_unutursan_sagdaki_sihirli_degneye")
        )
        var ikinci: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_burada_derste_ogrendigin_sayilar_ve_kardesle")
        )
        ikinci.hedef = .kuralPaneliDugmesi
        ikinci.vurguRengi = kuralYesili
        ikinci.vurguAcikRengi = kuralAcikYesili
        ikinci.vurguOlcegi = 1.1
        ikinci.tiklamaGerekli = true
        return [ilk, ikinci]
    }

    // MARK: Rehber 3 — kurallar kitabı

    private static func kurallarKitabi() -> [RehberAdimi] {
        let ilk: RehberAdimi = RehberAdimi(
            emote: "TEACH_TALK",
            metin: Metin.al("ui_kurallari_unutursan_sag_ustteki_kitaba_tikla")
        )
        var ikinci: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_burada_ogrendigin_kurallar_yer_alir")
        )
        ikinci.hedef = .kuralKitabiDugmesi
        ikinci.vurguRengi = kuralYesili
        ikinci.vurguAcikRengi = kuralAcikYesili
        ikinci.vurguOlcegi = 1.1
        ikinci.tiklamaGerekli = true
        return [ilk, ikinci]
    }

    // MARK: Rehber 4 — kural tablosunu abaküse alma

    private static func kuralTablosunuAlma() -> [RehberAdimi] {
        let ilk: RehberAdimi = RehberAdimi(
            emote: "TEACH_TALK",
            metin: Metin.al("ui_kurallar_kitabina_tikladiginda_ekrana_gelen")
        )
        var ikinci: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_kurallar_kitabina_tikla")
        )
        ikinci.hedef = .kuralKitabiDugmesi
        ikinci.vurguRengi = kuralYesili
        ikinci.vurguAcikRengi = kuralAcikYesili
        ikinci.vurguOlcegi = 1.1
        ikinci.tiklamaGerekli = true
        ikinci.tiklamaIlerletir = true

        var ucuncu: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_ekrana_gelen_kurallardan_herhangi_birisini_s")
        )
        ucuncu.kuralSecimiBekler = true
        return [ilk, ikinci, ucuncu]
    }

    // MARK: Rehber 5 — sayıyı geçme oku (kör ders)

    private static func sayiyiGecme() -> [RehberAdimi] {
        var ilk: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_eger_tahtada_gosterilen_sayiyi_hemen_gecmek")
        )
        ilk.hedef = .adimAtlaDugmesi
        ilk.vurguRengi = okKoyu
        ilk.vurguAcikRengi = okAcik
        ilk.vurguOlcegi = 1.1
        let ikinci: RehberAdimi = RehberAdimi(
            emote: "TEACH_TALK",
            metin: Metin.al("ui_bu_butona_tahtada_bir_sayi_varken")
        )
        return [ilk, ikinci]
    }

    // MARK: Rehber 6 — abaküs boyutu

    private static func abakusBoyutu() -> [RehberAdimi] {
        var ilk: RehberAdimi = RehberAdimi(
            emote: "TEACH_POINT",
            metin: Metin.al("ui_abakusun_boyutunu_sag_alttaki_olcekleme_buto")
        )
        ilk.hedef = .abakusAyarDugmesi
        ilk.vurguRengi = okKoyu
        ilk.vurguAcikRengi = okAcik
        ilk.vurguOlcegi = 1.1
        ilk.tiklamaGerekli = true
        return [ilk]
    }

    // MARK: Geri dönüşte boncuk sadeleştirme

    /// AbacusFragment.optimizeBeadIdsForReverse: aynı çubuğun alt boncuklarından yalnızca en yüksek
    /// numaralısı kalır (o zaten bütün boncukları hareket ettirir); üst boncuklar ve diğerleri aynen.
    static func tersiIcinSadelestir(_ kimlikler: [String]) -> [String] {
        var sonuc: [String] = []
        var cubukSirasi: [Int] = []
        var gruplar: [Int: [String]] = [:]
        for kimlik in kimlikler {
            var cubukNo: Int? = nil
            for cubuk in 0..<5 {
                if kimlik.hasPrefix("rod\(cubuk)BottomBead") {
                    cubukNo = cubuk
                    break
                }
            }
            if let cubuk = cubukNo {
                if gruplar[cubuk] == nil {
                    cubukSirasi.append(cubuk)
                    gruplar[cubuk] = []
                }
                gruplar[cubuk]?.append(kimlik)
            } else {
                sonuc.append(kimlik)
            }
        }
        for cubuk in cubukSirasi {
            guard let boncuklar = gruplar[cubuk], let ilk = boncuklar.first else { continue }
            var enIyi: String = ilk
            var enIyiPuan: Int = boncukPuani(ilk)
            for boncuk in boncuklar {
                let puan: Int = boncukPuani(boncuk)
                if puan > enIyiPuan {
                    enIyi = boncuk
                    enIyiPuan = puan
                }
            }
            sonuc.append(enIyi)
        }
        return sonuc
    }

    /// Bead4 > Bead3 > Bead2 > Bead1.
    private static func boncukPuani(_ kimlik: String) -> Int {
        if kimlik.contains("Bead4") { return 4 }
        if kimlik.contains("Bead3") { return 3 }
        if kimlik.contains("Bead2") { return 2 }
        if kimlik.contains("Bead1") { return 1 }
        return 0
    }
}
