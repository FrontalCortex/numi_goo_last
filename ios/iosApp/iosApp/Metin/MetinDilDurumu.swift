import SwiftUI

// Dil değişince arayüzün yeniden kurulması.
//
// Android dil seçiminden sonra uygulamayı baştan açar (AppLanguage.apply → SplashActivity): sınıf
// açılışında okunmuş metinler ve bellekteki ders şablonu ancak öyle yeni dile geçer. iOS'ta süreç
// yeniden başlatılamaz; bunun yerine kök görünüm `.id(surum)` ile sıfırdan kurulur (yönetici, görünüm
// modelleri ve `@State`'ler yeniden oluşur). Oturum ömürlü sahte ortam (`SahteOrtam`) korunur.

/// Dil sürümü: `Metin.secimiUygula` her dil değişiminde bir artırır.
final class MetinDilDurumu: ObservableObject {
    static let ortak = MetinDilDurumu()

    @Published private(set) var surum: Int = 0

    func yenile() {
        surum += 1
    }
}

/// İçeriği dil sürümüne bağlar: sürüm değişince içerik baştan kurulur.
@MainActor
struct MetinDilKoku<Icerik: View>: View {
    @ObservedObject private var durum: MetinDilDurumu = MetinDilDurumu.ortak
    private let icerik: @MainActor () -> Icerik

    init(@ViewBuilder icerik: @escaping @MainActor () -> Icerik) {
        self.icerik = icerik
    }

    var body: some View {
        icerik()
            .id(durum.surum)
            // Sistem bileşenleri (tarih seçici, erişilebilirlik) de seçili dili kullansın.
            .environment(\.locale, Metin.yerel)
    }
}
