import SwiftUI

// Uygulamanın ana kabuğu: üst durum çubuğu + sekme içeriği + alt gezinme çubuğu.
// Android karşılığı: MainActivity ve res/layout/activity_main.xml.
struct AnaKabuk: View {
    @StateObject private var durum = OturumDurumu()
    @State private var secili: Sekme = .harita

    var body: some View {
        VStack(spacing: 0) {
            if secili.ustCubukGorunur {
                UstCubuk(durum: durum)
            }
            KabukAyirici()
            icerik
            KabukAyirici()
            AltCubuk(secili: $secili)
        }
        // Arka plan çentiğin ve alt ana ekran çizgisinin altına kadar uzanır;
        // içerik güvenli alanın içinde kalır (Android: fitsSystemWindows + sistem çubuğu rengi).
        .background(Color("background_color").ignoresSafeArea())
        // Uygulama sistem temasından bağımsız hep koyudur (Android: Theme.NumiGoo).
        .preferredColorScheme(.dark)
        .environmentObject(durum)
    }

    // Sekme içeriği. Harita, sekme değişse de bellekte tutulur (kaydırma konumu kaybolmasın);
    // diğer sekmeler şimdilik yer tutucudur.
    private var icerik: some View {
        ZStack {
            HaritaEkrani()
                .opacity(secili == .harita ? 1 : 0)
                .allowsHitTesting(secili == .harita)
                .accessibilityHidden(secili != .harita)
            if secili != .harita {
                YakindaEkrani(sekme: secili)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}
