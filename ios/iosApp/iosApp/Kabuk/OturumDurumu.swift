import SwiftUI

// Üst çubuktaki göstergelerin (elmas, enerji, anahtar) değerlerini tutar.
// Şimdilik sabit örnek değerler taşır; ileride Firestore dinleyicileri bu alanları güncelleyecek.
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

    // Enerji göstergesinin metni; Android MainActivity.updateEnergyDisplay ile aynı sıra.
    var enerjiMetni: String {
        if enerjiEngelli {
            return "0/\(azamiEnerji)"
        }
        if sonsuzEnerji {
            return "∞"
        }
        return "\(enerji)/\(azamiEnerji)"
    }
}
