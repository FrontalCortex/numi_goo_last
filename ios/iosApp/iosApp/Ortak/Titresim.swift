import Foundation
import UIKit

// Dokunma titreşimleri.
//
// Android: Haptics.kt — "AppPrefs" / "vibration_enabled" (varsayılan açık) kapalıysa hiç titretmiyor;
// `click(view)` = performHapticFeedback(CONTEXT_CLICK). Android'de titreşim veren yerler:
// alt çubuk sekme geçişi (MainActivity.kt:1218), avatar seçimi (AvatarCustomFragment.kt:86, 258),
// rozet ekranı (BadgeFragment.kt:1278). Boncuk ve doğru/yanlış cevapta Android'de titreşim YOKTUR;
// iOS'ta da eklenmez.
//
// İş parçacığı: UIKit üreteçleri ana iş parçacığında kullanılır; başka iş parçacığından gelen çağrı
// ana iş parçacığına aktarılır.

enum Titresim {
    /// Android Haptics.KEY. Ayar ekranı (ProfilSesAyarlariEkrani) bu anahtarı yazar.
    static let anahtar: String = "vibration_enabled"

    /// Anahtar yoksa varsayılan açık (Android getBoolean(KEY, true)).
    static func acikMi() -> Bool {
        if let deger = UserDefaults.standard.object(forKey: anahtar) as? Bool {
            return deger
        }
        return true
    }

    /// Android Haptics.click: kısa "tık" titreşimi (sekme, avatar seçimi, rozet).
    static func tik() {
        etki(.light)
    }

    // MARK: İç işler

    private static func etki(_ stil: UIImpactFeedbackGenerator.FeedbackStyle) {
        anaIsParcacigindaCalistir {
            guard acikMi() else { return }
            let uretec: UIImpactFeedbackGenerator = UIImpactFeedbackGenerator(style: stil)
            uretec.impactOccurred()
        }
    }

    private static func anaIsParcacigindaCalistir(_ isi: @escaping () -> Void) {
        if Thread.isMainThread {
            isi()
        } else {
            DispatchQueue.main.async(execute: isi)
        }
    }
}
