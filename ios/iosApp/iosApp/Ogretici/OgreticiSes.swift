import Foundation

// Öğretici sesleri.
//
// Android: TutorialFragment.playSound (adımın sesi; res/raw/tutorial*.mp3, Türkçe dışındaki dillerde
// Firebase Storage `tutorial_audio/<dil>_<anahtar>.mp3`) ve playCorretSound (correct_answer_sound
// 0.3 ses düzeyiyle, incorrect_answer_sound). Ayar: AppPrefs "tutorial_sound_enabled" ve "sound_enabled".
// Ses akışı ilerletmez: adımlar dokunmayla geçer; ses yoksa sessiz geçilir.
//
// Ses dosyaları Sesler/Ogretici/ altındadır; gerçek çalıcı OgreticiSesCalar.swift'te
// (OgreticiGercekSesCalici). `anahtar` res/raw adıdır ("tutorial1_1").

protocol OgreticiSesCalici: AnyObject {
    /// Adımın sesini çalar; önceki ses susturulur. `anahtar` nil ise yalnızca susturur.
    /// `uzakYol`: gömülü ses yoksa Storage yolu (Android soundStoragePath).
    func adimSesiniCal(anahtar: String?, uzakYol: String?)
    /// Doğru / yanlış cevap sesi.
    func sonucSesiniCal(dogru: Bool)
    /// Ekran kapanırken.
    func durdur()
}

/// Ses çalmayan uygulama; son istenen anahtarı tutar.
final class SahteOgreticiSesCalici: OgreticiSesCalici {
    private(set) var sonAnahtar: String? = nil

    init() {}

    func adimSesiniCal(anahtar: String?, uzakYol: String?) {
        sonAnahtar = anahtar
    }

    func sonucSesiniCal(dogru: Bool) {}

    func durdur() {
        sonAnahtar = nil
    }
}
