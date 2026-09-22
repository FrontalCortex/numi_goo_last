package com.example.app

import androidx.fragment.app.Fragment

/**
 * Seri kurulum akışını gerektiğinde araya sokar.
 *
 * İlk dersi bitiren kullanıcı iki ayrı ekrandan kayıt akışına gönderiliyor (dersi bitirince
 * ve başarısız olunca). Karar iki yerde tekrar edilmesin diye tek kapı: çağıran taraf
 * "kayıt ekranını açmadan önce bunu sor" diyor, akışın gerekip gerekmediğine burası karar
 * veriyor.
 */
object StreakOnboardingLauncher {

    /**
     * Akış gerekiyorsa açar ve `true` döner — bu durumda çağıran taraf DURMALI, [onDone]
     * akış bitince çalışır. Gerekmiyorsa `false` döner ve çağıran kendi yoluna devam eder.
     */
    fun showIfNeeded(fragment: Fragment, onDone: () -> Unit): Boolean {
        val context = fragment.context ?: return false
        if (StreakRepository.isOnboardingDone(context)) return false
        if (!fragment.isAdded) return false

        val fm = fragment.parentFragmentManager
        // Dinleyici çağıranın görünümüne bağlı: akış `add` ile üstüne eklendiği için çağıran
        // yaşamaya devam ediyor, sonuç geldiğinde hâlâ dinliyor olacak.
        fm.setFragmentResultListener(
            StreakOnboardingFragment.RESULT_KEY,
            fragment.viewLifecycleOwner,
        ) { _, _ -> onDone() }

        // Rozet kutlamasının kabı kullanılıyor: tam ekran ve diğer her şeyin üstünde.
        // İlk ders sonrası bir rozet kutlaması kuyrukta olmadığı için çakışma yok.
        fm.beginTransaction()
            .add(R.id.badgeFragmentContainter, StreakOnboardingFragment())
            .commitAllowingStateLoss()
        return true
    }
}
