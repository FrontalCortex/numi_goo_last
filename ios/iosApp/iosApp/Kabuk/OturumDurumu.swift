import SwiftUI

// Üst çubuktaki göstergelerin (seri, elmas, enerji, anahtar) değerlerini tutar.
// Değerleri yönetici (Yonetim/UygulamaYoneticisi.oturumuYenile) yazar: elmas ve anahtar mağazanın
// (şimdilik sahte) cüzdanından, enerji mağazanın (şimdilik sahte) can servisinden gelir; öğretmen
// ve enerji engeli alanları hâlâ sabittir. İleride Firestore dinleyicileri bu alanları güncelleyecek.
// Kurallar (azami enerji, sonsuz enerji, yenilenme süresi, cüzdan varsayılanları) ortak Kotlin
// kodunda hazırdır (ios/shared/.../oyun/EnergyManager.kt, UserWalletFirestore.kt); bağlanırken
// buradaki alanlar o sınıfların sonuçlarıyla doldurulmalı, mantık Swift'te yeniden yazılmamalı.
final class OturumDurumu: ObservableObject {
    // Elmas (jeton) bakiyesi.
    // Android: MainActivity.applyWalletToUi -> binding.currencyText; kaynak Firestore
    // `users/{uid}.currency` (UserWalletFirestore.listenToWallet). Varsayılan DEFAULT_CURRENCY = 0.
    @Published var elmas: Int = 0

    // Anahtar bakiyesi.
    // Android: MainActivity.applyWalletToUi -> binding.keyText; kaynak Firestore
    // `users/{uid}.keys`. Varsayılan DEFAULT_KEYS = 1.
    @Published var anahtar: Int = 1

    // Anlık enerji.
    // Android: EnergyManager.getCurrentEnergy() (setEnergyUpdateCallback ile
    // MainActivity.updateEnergyDisplay'e gelir); kaynak `users/{uid}.energy_full_time`.
    @Published var enerji: Int = 5

    // Azami enerji.
    // Android: EnergyManager.getMaxEnergy() - plan "Lite" ise 10, değilse 5 (`users/{uid}.plan`).
    @Published var azamiEnerji: Int = 5

    // Sonsuz enerji.
    // Android: EnergyManager.isInfiniteEnergy() - onaylı öğretmen ya da Pro/Premium plan
    // (`users/{uid}.plan`, `planExpiresAt`, `teacherApproved`).
    @Published var sonsuzEnerji: Bool = false

    // Enerji engeli.
    // Android: EnergyManager.isEnergyBlocked() - onaylanmamış öğretmen hesabı; enerji hep 0.
    @Published var enerjiEngelli: Bool = false

    // Öğretmen hesabı mı.
    // Android: AuthManager.getCurrentUserType() == ROLE_TEACHER (`users/{uid}.role`);
    // öğretmende üst çubuktaki bütün göstergeler gizlenir (MainActivity.onCreate).
    @Published var ogretmenMi: Bool = false

    // Günlük seri (gün sayısı).
    // Android: StreakRepository.refresh(context).current → MainActivity.refreshStreakUi
    // (binding.streakText); 0 iken alev söner. Yönetici yazar (YonetimSeriAkisi.seriyiYenile; kaynak Seri/ servisi).
    @Published var seri: Int = 0

    // Kalbin üstünde "∞" rozeti gösterilir mi (Android: EnergyDisplay.apply, isInfinite =
    // !isEnergyBlocked && isInfiniteEnergy); gösterilirken yanındaki sayı boşalır.
    var sonsuzRozeti: Bool {
        return !enerjiEngelli && sonsuzEnerji
    }

    // Enerji göstergesinin metni; Android MainActivity.updateEnergyDisplay + EnergyDisplay.apply.
    var enerjiMetni: String {
        if sonsuzRozeti {
            return ""
        }
        if enerjiEngelli {
            return "0/\(azamiEnerji)"
        }
        return "\(enerji)/\(azamiEnerji)"
    }
}
