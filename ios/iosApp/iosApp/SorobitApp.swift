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
            AnaKabuk()
        }
    }
}
