import SwiftUI

// Uygulamanın giriş noktası.
@main
struct SorobitApp: App {
    init() {
        // Paketle gelen yazı tiplerini sisteme tanıtır (Tema/Tema.swift).
        Tema.yaziTipleriniKaydet()
    }

    var body: some Scene {
        WindowGroup {
            // `-ekran <ad>` başlatma argümanı verildiyse yalnızca o ekran açılır (hata ayıklama:
            // Yonetim/EkranGalerisi.swift); verilmediyse uygulamanın olağan akışı.
            if let ad = EkranGalerisi.istenenAd {
                EkranGalerisi(ad: ad)
            } else {
                UygulamaKokGorunumu()
            }
        }
    }
}
