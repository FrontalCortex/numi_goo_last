import Foundation

// Sonuç ekranı sesleri: SonucSesCalar'ın gerçek uygulaması.
//
// Android:
//  - LessonResultSoundPlayer.play (LessonResult.kt:86): res/raw/lesson_result_sound.wav, ses düzeyi 0,3.
//  - ChestResult.playStarSoundForTier (ChestResult.kt:705): res/raw/star_sound_effect.mp3, ses düzeyi
//    0,1, perde `hiz` (1. yıldız 0,9, 2. yıldız 1,0, 3. yıldız 1,12; ortak koddan gelir).
//  - "sound_enabled" kapalıysa çalmaz (denetimi `SesCalar` yapar).

struct GercekSonucSesCalar: SonucSesCalar {
    func sonucSesiCal() {
        SesCalar.ortak.cal(ad: "lesson_result_sound", sesDuzeyi: 0.3, hiz: 1.0)
    }

    func yildizSesiCal(hiz: Float) {
        SesCalar.ortak.cal(ad: "star_sound_effect", sesDuzeyi: 0.1, hiz: hiz)
    }
}
