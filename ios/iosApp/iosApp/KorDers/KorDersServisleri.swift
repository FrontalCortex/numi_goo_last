import Foundation

// Kör ders ekranının dış dünyaya dokunan işleri. Firebase, enerji ve ses henüz bağlı değil;
// hepsi protokol arkasında, sahte uygulamalarıyla.

/// İlerleme ve kupa kayıtları.
protocol KorDersKayitServisi {
    /// Kupa modunda cevap (ya da bırakma) sonrası kupa değişimi.
    /// Android: GlobalValues.pending…CupDelta alanına yazılır (alan adı = `hedef`), ardından
    /// BadgePrecalcHelper.executeCupDeltaUpdateAsync(lessonItem) Firestore'a işler
    /// (ilgili *CupRepository koleksiyonu; iyimser arayüz güncellemesi).
    func kupaDeltasiniIsle(hedef: String, delta: Int)

    /// Günlük soru sonucu.
    /// Android: oturum periyodu hâlâ geçerliyse (DailyQuestionPeriod.currentPeriodKey() ile aynıysa)
    /// DailyQuestionRepository.recordQuestionResult; doğruysa incrementSolvedCount +
    /// DailyQuestionBrokenHeartStore.clearBrokenHold116, yanlışsa markPendingDiamondContinue +
    /// requestPlayOnNextBind. Periyot geçersizse hiçbir şey yazılmaz. Her durumda `tamam` çağrılır
    /// ve ekran kapanır.
    func gunlukSoruSonucunuKaydet(
        donemAnahtari: String, yuva: Int, basarili: Bool, tamam: @escaping () -> Void
    )

    /// Ekranda geçirilen süre. Android: MissionsProgressStore.recordLearningDurationMs.
    func ogrenmeSuresiniKaydet(milisaniye: Int)
}

/// Enerji. Android: MainActivity.getEnergyManager().useEnergy(n).
protocol KorDersEnerjiServisi {
    func enerjiHarca(_ miktar: Int)
}

/// Ses. Android: MediaPlayer.create(res/raw/<ad>); "AppPrefs"/"sound_enabled" kapalıysa çalınmaz.
protocol KorDersSesServisi {
    func cal(ad: String, sesDuzeyi: Float)
}

struct SahteKorDersKayitServisi: KorDersKayitServisi {
    func kupaDeltasiniIsle(hedef: String, delta: Int) {}

    func gunlukSoruSonucunuKaydet(
        donemAnahtari: String, yuva: Int, basarili: Bool, tamam: @escaping () -> Void
    ) {
        tamam()
    }

    func ogrenmeSuresiniKaydet(milisaniye: Int) {}
}

struct SahteKorDersEnerjiServisi: KorDersEnerjiServisi {
    func enerjiHarca(_ miktar: Int) {}
}

struct SahteKorDersSesServisi: KorDersSesServisi {
    func cal(ad: String, sesDuzeyi: Float) {}
}
