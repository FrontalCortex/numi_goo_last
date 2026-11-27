import Foundation

// Rehberin "gösterildi" kayıtları.
//
// Android karşılıkları (hepsi yerel tercih; anahtar adları aynı):
//   MarathonGuideStore.kt  → SharedPreferences "GuidePanelPrefs":
//       "marathon_guide_shown"   (Boolean)  bir kez gösterildi
//       "marathon_guide_pending" (Boolean)  ilk maraton sandığı bitti, rehber sırada
//       "cupFragment_guide_shown" (Boolean) eski anahtar; true ise tekrar gösterilmez
//   GuideHelper.kt GuideShowTracker → SharedPreferences "AppPrefs":
//       "guide_shown_count_<rehberNo>" (Int) rehberin kaç kez gösterildiği (rehber 6 en çok 2 kez)
//   AuthManager.checkSkipStepTutorialShown / setSkipStepTutorialShown → Firestore
//       `users/{uid}.skip_step_tutorial_shown` (Boolean): kör dersteki "sayıyı geç" rehberi (rehber 5).
//       Firebase henüz yok: burada yalnızca cihazda tutulur (hesap değişince sıfırlanmaz).

/// Rehber kayıtlarının arayüzü. Gerçek uygulama `UserDefaultsRehberDeposu`; testte ya da galeride
/// başka bir uygulama verilebilir.
protocol RehberKayitDeposu: AnyObject {
    /// MarathonGuideStore.isShown: "marathon_guide_shown" ya da eski "cupFragment_guide_shown".
    func maratonGosterildi() -> Bool
    /// MarathonGuideStore.isPending.
    func maratonBekliyor() -> Bool
    /// MarathonGuideStore.scheduleIfEligible'ın yazdığı bayrak / clearPending.
    func maratonBekleyenAyarla(_ bekliyor: Bool)
    /// MarathonGuideStore.markShown: "marathon_guide_shown" = true, "marathon_guide_pending" = false.
    func maratonuGosterildiOlarakIsaretle()
    /// GuideShowTracker.canShow için okunan sayı.
    func gosterimSayisi(rehberNo: Int) -> Int
    /// GuideShowTracker.recordShown.
    func gosterimiKaydet(rehberNo: Int)
    /// AuthManager.checkSkipStepTutorialShown (hesap yokken Android "gösterilmiş say" der; burada hesap
    /// ayrımı yok).
    func atlaOgreticisiGosterildi() -> Bool
    /// AuthManager.setSkipStepTutorialShown.
    func atlaOgreticisiniIsaretle()
}

/// `UserDefaults` tabanlı gerçek uygulama.
final class UserDefaultsRehberDeposu: RehberKayitDeposu {
    static let anahtarMaratonGosterildi: String = "marathon_guide_shown"
    static let anahtarMaratonBekliyor: String = "marathon_guide_pending"
    static let anahtarEskiMaratonGosterildi: String = "cupFragment_guide_shown"
    static let anahtarAtlaOgreticisi: String = "skip_step_tutorial_shown"

    private let varsayilanlar: UserDefaults

    init(varsayilanlar: UserDefaults = UserDefaults.standard) {
        self.varsayilanlar = varsayilanlar
    }

    private static func sayacAnahtari(_ rehberNo: Int) -> String {
        return "guide_shown_count_\(rehberNo)"
    }

    func maratonGosterildi() -> Bool {
        let yeni: Bool = varsayilanlar.bool(forKey: UserDefaultsRehberDeposu.anahtarMaratonGosterildi)
        let eski: Bool = varsayilanlar.bool(forKey: UserDefaultsRehberDeposu.anahtarEskiMaratonGosterildi)
        return yeni || eski
    }

    func maratonBekliyor() -> Bool {
        return varsayilanlar.bool(forKey: UserDefaultsRehberDeposu.anahtarMaratonBekliyor)
    }

    func maratonBekleyenAyarla(_ bekliyor: Bool) {
        varsayilanlar.set(bekliyor, forKey: UserDefaultsRehberDeposu.anahtarMaratonBekliyor)
    }

    func maratonuGosterildiOlarakIsaretle() {
        varsayilanlar.set(true, forKey: UserDefaultsRehberDeposu.anahtarMaratonGosterildi)
        varsayilanlar.set(false, forKey: UserDefaultsRehberDeposu.anahtarMaratonBekliyor)
    }

    func gosterimSayisi(rehberNo: Int) -> Int {
        return varsayilanlar.integer(forKey: UserDefaultsRehberDeposu.sayacAnahtari(rehberNo))
    }

    func gosterimiKaydet(rehberNo: Int) {
        let anahtar: String = UserDefaultsRehberDeposu.sayacAnahtari(rehberNo)
        let simdiki: Int = varsayilanlar.integer(forKey: anahtar)
        varsayilanlar.set(simdiki + 1, forKey: anahtar)
    }

    func atlaOgreticisiGosterildi() -> Bool {
        return varsayilanlar.bool(forKey: UserDefaultsRehberDeposu.anahtarAtlaOgreticisi)
    }

    func atlaOgreticisiniIsaretle() {
        varsayilanlar.set(true, forKey: UserDefaultsRehberDeposu.anahtarAtlaOgreticisi)
    }
}
