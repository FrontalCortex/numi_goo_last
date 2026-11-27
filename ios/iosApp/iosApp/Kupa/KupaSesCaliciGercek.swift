import Foundation

// Rozet sesi: KupaSesCalici'nin gerçek uygulaması.
//
// Android: BadgeSoundPlayer.kt (BadgeFragment.kt:250 preload, 1656 play): res/raw/badge_sound.mp3,
// ses düzeyi 0,8, perde 1,0; "sound_enabled" kapalıysa çalmaz (denetimi `SesCalar` yapar).

final class GercekKupaSesCalici: KupaSesCalici {
    init() {}

    func rozetSesiniHazirla() {
        SesCalar.ortak.hazirla(ad: "badge_sound")
    }

    func rozetSesiniCal() {
        SesCalar.ortak.cal(ad: "badge_sound", sesDuzeyi: 0.8, hiz: 1.0)
    }
}

extension KupaOrtami {
    /// `KupaOrtami.sahte` ile aynı örnek veriler (aynı depo nesneleri); yalnızca ses çalıcısı gerçektir.
    static var gercekSesli: KupaOrtami {
        var ortam: KupaOrtami = KupaOrtami.sahte
        ortam.sesCalici = GercekKupaSesCalici()
        return ortam
    }
}
