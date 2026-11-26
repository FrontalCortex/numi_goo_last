import Shared
import SwiftUI

// İskelet ekranı: gerçek bir ekran değil. Tek amacı, ortak Kotlin kodunun
// Swift'ten çağrılabildiğini göstermek (ders kataloğu ortak koddan okunur).
struct ContentView: View {
    private let dersler = GlobalLessonData.shared.createLessonItems(partId: 1)

    var body: some View {
        VStack(spacing: 16) {
            Text("Sorobit")
                .font(.largeTitle.bold())
            Text("Ortak kod bağlı")
                .font(.headline)
            Text("1. bölümde \(dersler.count) öğe var")
            if let ilk = dersler.first {
                Text("İlk öğe: \(ilk.title)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
    }
}
