import SwiftUI

// Uygulamanın giriş noktası.
@main
struct SorobitApp: App {
    init() {
        // Paketle gelen yazı tiplerini sisteme tanıtır (Tema/Tema.swift).
        Tema.yaziTipleriniKaydet()
        // Uygulama dili: kayıtlı seçim ya da `-dil <kod>` başlatma argümanı (Metin/Metin.swift).
        Metin.baslat()
    }

    var body: some Scene {
        WindowGroup {
            // Dil değişince içerik baştan kurulur (Metin/MetinDilDurumu.swift; Android uygulamayı yeniden başlatır).
            MetinDilKoku {
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
}
