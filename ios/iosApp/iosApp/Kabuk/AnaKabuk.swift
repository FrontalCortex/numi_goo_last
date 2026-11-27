import Combine
import SwiftUI

// Uygulamanın ana kabuğu: üst durum çubuğu + sekme içeriği + alt gezinme çubuğu.
// Android karşılığı: MainActivity ve res/layout/activity_main.xml.
//
// Sekmelerin dışarı verdiği geçişler (mağaza, sandık, ders, kupa, rozet ...) yöneticiye
// (Yonetim/UygulamaYoneticisi) iletilir; tam ekranları kök görünüm açar.
@MainActor
struct AnaKabuk: View {
    @ObservedObject var yonetici: UygulamaYoneticisi
    @State private var secili: Sekme = .harita

    /// Enerji servisinde dinleyici yok; üst çubuk yarım dakikada bir yeniden okunur.
    private let saat = Timer.publish(every: 30, on: .main, in: .common).autoconnect()

    init(yonetici: UygulamaYoneticisi) {
        self.yonetici = yonetici
    }

    var body: some View {
        VStack(spacing: 0) {
            if secili.ustCubukGorunur {
                UstCubuk(durum: yonetici.oturum, magazaAc: { yonetici.magazaAc() })
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
        .environmentObject(yonetici.oturum)
        .onAppear { yonetici.oturumuYenile() }
        .onChange(of: secili) { _ in yonetici.oturumuYenile() }
        .onReceive(saat) { _ in yonetici.oturumuYenile() }
    }

    // Sekme içeriği. Harita, sekme değişse de bellekte tutulur (kaydırma konumu kaybolmasın);
    // diğer sekmeler seçilince kurulur.
    private var icerik: some View {
        ZStack {
            HaritaEkrani(model: yonetici.haritaModeli, yerTutucuGoster: false)
                .opacity(secili == .harita ? 1 : 0)
                .allowsHitTesting(secili == .harita)
                .accessibilityHidden(secili != .harita)
            digerSekme
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }

    @ViewBuilder
    private var digerSekme: some View {
        switch secili {
        case .harita:
            EmptyView()
        case .profil:
            profil
        case .gorevler:
            gorevler
        case .magaza:
            // Android'de 4. sekme mağaza değil abaküs özelleştirmedir (AbacusCustomizationFragment).
            OzellestirmeEkrani(magazaAc: { yonetici.magazaAc() })
        case .bildirim:
            // NotificationFragment henüz yazılmadı.
            YakindaEkrani(sekme: .bildirim)
        case .kesfet:
            kesfet
        }
    }

    private var profil: some View {
        let nesne: UygulamaYoneticisi = yonetici
        return ProfilEkrani(
            kupaAc: { secim in nesne.profilKupasiniAc(secim) },
            rozetlerAc: { nesne.rozetleriAc() },
            magazaAc: { nesne.magazaAc() },
            cikisYapildi: { nesne.cikisYapildi() }
        )
    }

    private var gorevler: some View {
        let nesne: UygulamaYoneticisi = yonetici
        var ekran = GorevlerEkrani()
        ekran.sandikAc = { istek in nesne.gorevSandiginiAc(istek) }
        return ekran
    }

    private var kesfet: some View {
        let nesne: UygulamaYoneticisi = yonetici
        var ekran = GunlukSoruEkrani()
        ekran.soruBaslat = { veri in nesne.gunlukSoruyuBaslat(veri) }
        ekran.odulAc = { istek in nesne.gunlukSoruOdulunuAc(istek) }
        ekran.kartSecildi = { kart in nesne.kesfetKartiSecildi(kart) }
        // rozetGoster: rozet seviye atlama ekranı (BadgeLevelUpFragment) henüz yazılmadı.
        return ekran
    }
}
