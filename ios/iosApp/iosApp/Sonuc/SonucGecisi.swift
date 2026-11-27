import SwiftUI

// Ders akışı ekranlarının geçiş animasyonu.
//
// Android: ders akışındaki bütün ekranlar (öğretici, abaküs, kör ders, sonuç, sandık, rozet)
// SAĞDAN girer ve SOLA çıkar; süre 400 ms, eğri accelerate_decelerate
// (res/anim/queue_screen_in.xml, queue_screen_out.xml). Böylece art arda gelen iki ekran tek bir
// ileri hareket gibi okunur (giren ekran gideni sola iter).
//
// Kullanım: Yonetim/UygulamaKokGorunumu katmanın `.transition`'ında çağırır.

enum SonucGecisi {
    /// Sağdan gir, sola çık; 400 ms ease-in-out (AccelerateDecelerateInterpolator'ın SwiftUI karşılığı).
    static var sagdanGirSolaCik: AnyTransition {
        let giren: AnyTransition = AnyTransition.move(edge: .trailing)
        let cikan: AnyTransition = AnyTransition.move(edge: .leading)
        let cift: AnyTransition = AnyTransition.asymmetric(insertion: giren, removal: cikan)
        return cift.animation(Animation.easeInOut(duration: 0.4))
    }
}
