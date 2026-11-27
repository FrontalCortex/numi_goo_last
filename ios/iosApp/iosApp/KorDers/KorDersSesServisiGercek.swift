import Foundation

// Kör ders sesleri: KorDersSesServisi'nin gerçek uygulaması.
//
// Android: BlindingLessonFragment.playSound (BlindingLessonFragment.kt:1241-1258, 1379):
// MediaPlayer.create(res/raw/<ad>) + setVolume; "AppPrefs"/"sound_enabled" kapalıysa çalınmaz
// (denetimi `SesCalar` yapar). Doğru cevap 0,3, yanlış cevap 1,0 düzeyindedir (ortak koddan gelir).
//
// Aynı nesne abaküs boncuk sesini de verir (`AbakusBoncukSesi`): kör ders abaküsle cevaplanırken
// Android'de AbacusBeadController aynı AbacusSoundPlayer'ı çalar. `KorDersEkrani` bu nesneyi
// abaküse bağlar. Titreşim Android'de yoktur, iOS'ta da yoktur.

final class GercekKorDersSesServisi: KorDersSesServisi, AbakusBoncukSesi {
    private let boncuk: GercekAbakusBoncukSesi

    init() {
        self.boncuk = GercekAbakusBoncukSesi.ortak
        self.boncuk.hazirla()
    }

    func cal(ad: String, sesDuzeyi: Float) {
        SesCalar.ortak.cal(ad: ad, sesDuzeyi: sesDuzeyi)
    }

    func boncukSesi(etkinlesiyor: Bool) {
        boncuk.boncukSesi(etkinlesiyor: etkinlesiyor)
    }
}
