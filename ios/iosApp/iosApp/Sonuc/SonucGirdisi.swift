import Foundation

// Ders sonucu ekranının girdisi ve dış bağımlılıkları.
//
// Android'de üç ayrı Fragment vardır; hangisinin açılacağına dersi oynatan ekran karar verir
// (AbacusFragment.showLessonResult / showLessonResultFalseOnly / showChestResult ve
// BlindingLessonFragment'teki eşleri). iOS'ta tek giriş görünümü (`SonucEkrani`) vardır ve tür
// girdiyle seçilir. Karar kuralları ortak koddadır (AbacusLessonLogic, BlindingLessonLogic).

/// Sonuç ekranına aktarılan değerler (Android: Fragment argümanları).
struct SonucGirdisi {
    enum Tur {
        /// LessonResult — "Ders Bitti!", toplam puan ve başarı kutuları, "Devam et".
        case dersBasarili
        /// LessonResultFalse — "Ders Geçilemedi!", neden: başarı oranı %75'ten düşük.
        case dersBasarisiz
        /// LessonResultFalse (isChestFailure = true) — neden: puan 500'den düşük.
        case sandikBasarisiz
        /// ChestResult — süre, çarpan, toplam puan çubuğu ve üç yıldız.
        case sandikSonucu
    }

    var tur: Tur

    /// Android argümanı: "correctAnswers".
    var dogruSayisi: Int
    /// Android argümanı: "totalQuestions". Yanlış sayısı = toplamSoru - dogruSayisi.
    var toplamSoru: Int

    // Aşağıdakiler yalnızca `.sandikSonucu` türünde kullanılır.

    /// Android argümanı: "time" — "d:ss" ya da "s:dd:ss".
    var sure: String = ""
    /// Android argümanı: "carpan" — süreye göre puan çarpanı.
    var carpan: Float = 1
    /// Android argümanı: "toplamPuan" — ders puanı × çarpan.
    var toplamPuan: Int = 0
    /// Puan çubuğunun tavanı = 3 yıldız eşiği (dersin cupPoint1'i).
    /// Android bunu ekranın içinde LessonManager'dan çözer; burada yalın değer olarak gelir:
    /// `SonucEkraniKoprusu.shared.puanTavani(mapFragmentStepIndex:)`.
    var puanTavani: Int = 1
    /// 2 yıldız eşiği (dersin cupPoint2'si):
    /// `SonucEkraniKoprusu.shared.ikiYildizEsigi(mapFragmentStepIndex:puanTavani:)`.
    var ikiYildizEsigi: Int = 1

    /// Başarısız sonuçta "Tekrar dene" düğmesi gösterilsin mi. Android'de bu düğme YOKTUR
    /// (yalnızca "Çıkış"); varsayılan `false` Android görünümünü verir.
    var tekrarDugmesiGoster: Bool = false

    init(
        tur: Tur,
        dogruSayisi: Int,
        toplamSoru: Int,
        sure: String = "",
        carpan: Float = 1,
        toplamPuan: Int = 0,
        puanTavani: Int = 1,
        ikiYildizEsigi: Int = 1,
        tekrarDugmesiGoster: Bool = false
    ) {
        self.tur = tur
        self.dogruSayisi = dogruSayisi
        self.toplamSoru = toplamSoru
        self.sure = sure
        self.carpan = carpan
        self.toplamPuan = toplamPuan
        self.puanTavani = puanTavani
        self.ikiYildizEsigi = ikiYildizEsigi
        self.tekrarDugmesiGoster = tekrarDugmesiGoster
    }
}

/// Sonuç ekranının sesleri. Gerçek uygulama AVFoundation ile yazılacak; ayarlardaki
/// "ses açık" tercihi (Android: AppPrefs / "sound_enabled") uygulamanın içinde denetlenmeli.
protocol SonucSesCalar {
    /// Başarılı ders sonucu açılırken bir kez.
    /// Android: LessonResultSoundPlayer.play — res/raw/lesson_result_sound.wav, ses düzeyi 0.3.
    func sonucSesiCal()

    /// Sandık sonucunda puan bir yıldız eşiğini geçtiğinde.
    /// Android: ChestResult.playStarSoundForTier — res/raw/star_sound_effect.mp3, ses düzeyi 0.1,
    /// `hiz` oynatma hızıdır (1. yıldız 0.9, 2. yıldız 1.0, 3. yıldız 1.12).
    func yildizSesiCal(hiz: Float)
}

/// Ses çalmayan sahte uygulama.
struct SahteSonucSesCalar: SonucSesCalar {
    func sonucSesiCal() {}
    func yildizSesiCal(hiz: Float) {}
}
