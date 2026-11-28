import Shared
import SwiftUI

// Ekran galerisi (Yonetim/EkranGalerisi.swift) için Sobi testi örnekleri:
//   sobi-test            panel: 10'luk toplama (seviye 5/20) + zihinden 5'lik (seviye 1/20), üç zorluk kartı.
//   sobi-test-kilitli    ek teste anahtar yetmedi (Android: sobi_test_not_enough_keys + "Pro'da ek testler ücretsiz").
//   sobi-test-sonuc      testin özeti (kural satırları, seviye değişimi, "Sandığını aç").
//   sobi-test-oynatici   oynatıcının ilk sorusu (Başla'dan önce).
// Haritadaki düğme "harita-kisim" / "harita-ders"te görünür (SobiTestDugmeModeli galeri kipi).
// Örnek veri Android SobiTestPlannerTest'teki `add` listesinden (kuralsız 40/2, 5'lik 30/3, 10'luk 12/10).
enum SobiTestGaleri {
    static let adlar: [String] = ["sobi-test", "sobi-test-kilitli", "sobi-test-sonuc", "sobi-test-oynatici"]

    static var ornekBilgi: SobiDailyTest.Info {
        let puanlar: [SobiTestPlanner.RuleScore] = [
            SobiTestPlanner.RuleScore(key: "add_direct", correct: 40, wrong: 2),
            SobiTestPlanner.RuleScore(key: "add_five", correct: 30, wrong: 3),
            SobiTestPlanner.RuleScore(key: "add_ten", correct: 12, wrong: 10),
        ]
        let seviyeler: [String: KotlinInt] = SobiTestOrtak.kotlinSozluk(["add_ten": 5])
        let gorulen: Set<String> = ["add_direct", "add_five"]
        return SobiDailyTest.Info(scores: puanlar, levels: seviyeler, lessonSeen: gorulen)
    }

    static var ornekOzet: SobiTestOzet {
        let onluk: String = Metin.al("sobi_test_rule_add_ten")
        let besli: String = Metin.al("sobi_test_rule_mental", Metin.al("sobi_test_rule_add_five"))
        let satirlar: [String] = [
            Metin.al("sobi_test_rule_line", onluk, 18, 16),
            Metin.al("sobi_test_level_up", 5, 6),
            Metin.al("sobi_test_rule_line_questions", besli, 3, 2),
            Metin.al("sobi_test_level_same", 1),
            Metin.al("sobi_test_cheer_good"),
        ]
        return SobiTestOzet(skor: Metin.al("hw_player_block_score", 8, 10), mesaj: satirlar.joined(separator: "\n"))
    }
}

struct SobiTestGaleriEkrani: View {
    let ad: String

    var body: some View {
        switch ad {
        case "sobi-test-kilitli":
            SobiTestSayfasi(
                model: SobiTestSayfaModeli.galeri(
                    .bos(Metin.al("sobi_test_not_enough_keys", 3, 1)), bilgi: nil, ekTest: true, proGoster: true
                ),
                kapat: {}, basla: { _ in }, proAc: {}
            )
        case "sobi-test-sonuc":
            SobiTestOzetGorunumu(ozet: SobiTestGaleri.ornekOzet, sandikAc: {})
        case "sobi-test-oynatici":
            SobiTestGaleriOynatici()
        default:
            SobiTestSayfasi(
                model: SobiTestSayfaModeli.galeri(.hazir, bilgi: SobiTestGaleri.ornekBilgi, ekTest: false, proGoster: false),
                kapat: {}, basla: { _ in }, proAc: {}
            )
        }
    }
}

/// Örnek planla oynatıcı (sayaç ve seviyeler yazılmaz). Plan kurulamazsa ad listesine düşmesin diye boş özet.
struct SobiTestGaleriOynatici: View {
    var body: some View {
        let tohum: Int32 = SobiDailyTest.shared.seed(today: "2026-11-28", testsToday: 0, offset: 0)
        let plan: SobiTestPlanner.Plan? = SobiDailyTest.shared.plan(info: SobiTestGaleri.ornekBilgi, offset: 0, seed: tohum)
        return icerik(plan)
    }

    @ViewBuilder
    private func icerik(_ plan: SobiTestPlanner.Plan?) -> some View {
        if let kurulan = plan {
            SobiTestEkrani(
                model: SobiTestOynaticiModeli(baslangic: SobiTestBaslangici(plan: kurulan), kalici: false),
                bitti: { _ in },
                kapat: {}
            )
        } else {
            SobiTestOzetGorunumu(ozet: SobiTestOzet(skor: "", mesaj: Metin.al("sobi_test_no_data")), sandikAc: {})
        }
    }
}
