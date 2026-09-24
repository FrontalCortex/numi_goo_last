package com.example.app

import androidx.fragment.app.Fragment

/**
 * Seri kurulum akışını kayıt yolunun içine sokar.
 *
 * ## Neden kayıt anına bağlı
 * Akış eskiden ilk dersten hemen sonra açılıyordu. Bu, yalnızca temiz kurulumdan gelen
 * kullanıcıyı yakalıyordu: aynı cihazda ikinci hesap açan ya da tutorial'a hiç uğramadan
 * kayıt olan kimseye sorular sorulmuyordu.
 *
 * Şimdi tetikleyici "kayıt olma" olayının kendisi. Bu, bir soruyu da kendiliğinden çözüyor:
 * zaten hesabı olup yeni cihaza kurulum yapan kullanıcı GİRİŞ yapıyor, kayıt olmuyor — ona
 * hedef sorulmuyor, hedefi sunucudan geliyor.
 *
 * Yeni kurulumda kayıt zaten tutorial ve ilk dersten SONRA geldiği için, "kullanıcı ürünü
 * denemeden söz vermesin" kuralı da korunuyor.
 */
object StreakOnboardingLauncher {

    /**
     * Akış gerekiyorsa açar ve `true` döner — bu durumda çağıran taraf DURMALI, [onDone]
     * akış bitince çalışır. Gerekmiyorsa `false` döner ve çağıran kendi yoluna devam eder.
     *
     * @param containerId Akışın ekleneceği tam ekran kap. Kayıt akışı MainActivity'de
     *   yaşamadığı için sabit bir kimlik varsayılamıyor.
     */
    fun showIfNeeded(fragment: Fragment, containerId: Int, onDone: () -> Unit): Boolean {
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

        fm.beginTransaction()
            .add(containerId, StreakOnboardingFragment())
            .commitAllowingStateLoss()
        return true
    }
}
