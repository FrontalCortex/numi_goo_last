import Foundation

// Sandık sesleri: SandikSesCalar'ın gerçek uygulaması.
//
// Android: ChestSoundPlayer.kt (NewChestFragment.kt:221 preload, 344/359 playForRarity, 629 playReward).
//  - Seviye/adım sesi: res/raw/chest_level_sound.mp3, ses düzeyi 0,7, perde = `hiz` (ortak koddan:
//    nadirlik yükseldikçe 1,0 → 1,25 → 1,5 → 1,75; Android coerceIn(1, 2)).
//  - Ödül sesi: res/raw/reward_sound.mp3, ses düzeyi 0,7, perde 1,0.
//  - "sound_enabled" kapalıysa çalmaz (denetimi `SesCalar` yapar).

struct GercekSandikSesCalar: SandikSesCalar {
    func hazirla() {
        SesCalar.ortak.hazirla(ad: "chest_level_sound")
        SesCalar.ortak.hazirla(ad: "reward_sound")
    }

    func seviyeSesiCal(hiz: Float) {
        SesCalar.ortak.cal(ad: "chest_level_sound", sesDuzeyi: 0.7, hiz: hiz)
    }

    func odulSesiCal() {
        SesCalar.ortak.cal(ad: "reward_sound", sesDuzeyi: 0.7, hiz: 1.0)
    }
}
