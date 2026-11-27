import Combine
import SwiftUI

// Uygulamanın ana kabuğu: üst durum çubuğu + sekme içeriği + alt gezinme çubuğu.
// Android karşılığı: MainActivity ve res/layout/activity_main.xml.
//
// Sekme → ekran (Android: MainActivity.setupClickListeners):
//   harita    → PartSelectionFragment / MapFragment            → HaritaEkrani
//   profil    → ProfileFragment                                → ProfilEkrani
//   gorevler  → TasksFragment(MODE_MISSIONS): görev listesi, günlük soru kartı, Abaküs kartı
//                                                              → GorevlerEkrani + GunlukSoruEkrani
//   magaza    → AbacusCustomizationFragment                    → OzellestirmeEkrani
//   bildirim  → öğrencide SobiChatFragment, öğretmende TeacherClassesFragment → YakindaEkrani
//               ("öğretmene sor" / NotificationFragment: TeacherQuestionsFeature.ENABLED = false)
//   kesfet    → TasksFragment(MODE_CUP): yalnızca Kupa Yolu kartı → KabukKupaYoluSekmesi
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
            ustKisim
            icerik
            altKisim
        }
        // Arka plan çentiğin ve alt ana ekran çizgisinin altına kadar uzanır;
        // içerik güvenli alanın içinde kalır (Android: fitsSystemWindows + sistem çubuğu rengi).
        .background(Color("background_color").ignoresSafeArea())
        // Uygulama sistem temasından bağımsız hep koyudur (Android: Theme.NumiGoo).
        .preferredColorScheme(.dark)
        .environmentObject(yonetici.oturum)
        .onAppear { yonetici.oturumuYenile() }
        .onChange(of: secili) { _ in
            // Android: sekme seçilince closeBottomSheet() (açık ders paneli kapanır).
            yonetici.haritaModeli.paneliKapat()
            yonetici.oturumuYenile()
        }
        .onReceive(saat) { _ in yonetici.oturumuYenile() }
    }

    // MARK: Üst ve alt çubuk

    /// Üst çubuk ve altındaki çizgi yalnızca harita, kısım seçimi ve Görevler / Kupa Yolu
    /// sekmelerinde (Android: updateCurrencyPanelVisibility + syncCurrencyPanelDivider).
    @ViewBuilder
    private var ustKisim: some View {
        if secili.ustCubukGorunur {
            let nesne: UygulamaYoneticisi = yonetici
            VStack(spacing: 0) {
                KabukUstCubukBaglayici(
                    harita: nesne.haritaModeli,
                    oturum: nesne.oturum,
                    haritaSekmesi: secili == .harita,
                    magazaAc: { nesne.magazaAc() },
                    seriAc: { AnaKabuk.seriEkraniniAc(nesne) }
                )
                KabukAyirici()
            }
            .overlay { KabukKarartma(harita: nesne.haritaModeli, aktif: secili == .harita, kenar: .top) }
        }
    }

    private var altKisim: some View {
        let nesne: UygulamaYoneticisi = yonetici
        return VStack(spacing: 0) {
            KabukAyirici()
            KabukAltCubukBaglayici(
                oturum: nesne.oturum,
                secili: $secili,
                ayniSekmeyeDokunuldu: { sekme in ayniSekme(sekme) }
            )
        }
        .overlay { KabukKarartma(harita: nesne.haritaModeli, aktif: secili == .harita, kenar: .bottom) }
    }

    /// Android: streakContainer → openStreakFragment. Seri ekranı (StreakFragment) henüz yazılmadı.
    private static func seriEkraniniAc(_ yonetici: UygulamaYoneticisi) {
        yonetici.yerTutucuAc(
            Metin.al("activity_main_streak_icon_desc"),
            "Seri ekranı",
            ["Android: StreakFragment"]
        )
    }

    // MARK: Sekme içeriği

    // Harita, sekme değişse de bellekte tutulur (kaydırma konumu kaybolmasın);
    // diğer sekmeler seçilince kurulur.
    private var icerik: some View {
        ZStack {
            HaritaEkrani(model: yonetici.haritaModeli, yerTutucuGoster: false)
                // Geri düğmesini üst çubuk çizer (Android: lessonPartBackButton currencyPanel içinde).
                .environment(\.haritaGeriKabukta, true)
                .opacity(secili == .harita ? 1 : 0)
                .allowsHitTesting(secili == .harita)
                .accessibilityHidden(secili != .harita)
            digerSekme
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }

    /// Android: R.id.map seçiliyken yeniden dokunulursa ve MapFragment açıksa
    /// popBackStack("part_map") ile kısım seçimine dönülür; diğer sekmelerde bir şey olmaz.
    private func ayniSekme(_ sekme: Sekme) {
        if sekme == .harita && yonetici.haritaModeli.seciliPart != nil {
            yonetici.haritaModeli.kisimSeciminDon()
        }
    }

    @ViewBuilder
    private var digerSekme: some View {
        switch secili {
        case .harita:
            EmptyView()
        case .profil:
            profil
        case .gorevler:
            gorevlerSekmesi
        case .magaza:
            // Android'de 4. sekme mağaza değil abaküs özelleştirmedir (AbacusCustomizationFragment).
            OzellestirmeEkrani(magazaAc: { yonetici.magazaAc() })
        case .bildirim:
            KabukSohbetSekmesi(oturum: yonetici.oturum)
        case .kesfet:
            kupaYoluSekmesi
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

    /// Android: TasksFragment(MODE_MISSIONS) tek listede görevler, günlük soru kartı ve Abaküs
    /// kartı gösterir. iOS'ta bu ikisi henüz ayrı giriş görünümleri (Gorevler/, GunlukSoru/) ve
    /// her biri kendi kaydırma alanına sahip; o klasörler güncellenene dek alt alta iki bölme.
    private var gorevlerSekmesi: some View {
        VStack(spacing: 0) {
            gorevler
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            KabukAyirici()
            gunlukSoru
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private var gorevler: some View {
        let nesne: UygulamaYoneticisi = yonetici
        var ekran = GorevlerEkrani()
        ekran.sandikAc = { istek in nesne.gorevSandiginiAc(istek) }
        return ekran
    }

    private var gunlukSoru: some View {
        let nesne: UygulamaYoneticisi = yonetici
        var ekran = GunlukSoruEkrani()
        ekran.soruBaslat = { veri in nesne.gunlukSoruyuBaslat(veri) }
        ekran.odulAc = { istek in nesne.gunlukSoruOdulunuAc(istek) }
        ekran.kartSecildi = { kart in nesne.kesfetKartiSecildi(kart) }
        // rozetGoster: rozet seviye atlama ekranı (BadgeLevelUpFragment) henüz yazılmadı.
        return ekran
    }

    private var kupaYoluSekmesi: some View {
        let nesne: UygulamaYoneticisi = yonetici
        return KabukKupaYoluSekmesi(sec: { nesne.kesfetKartiSecildi(.kupaYolu) })
    }
}

// Üst çubuğu harita modeline bağlar: geri düğmesi yalnızca harita sekmesinde bir kısım açıkken
// görünür (Android: PartSelectionFragment.openMapForPart -> lessonPartBackButton VISIBLE;
// MainActivity.updateCurrencyPanelVisibility -> MapFragment dışında GONE).
// Ayrı görünüm olmasının nedeni: kabuk yalnızca yöneticiyi gözler, modelin `seciliPart`
// değişikliği burada gözlenmezse üst çubuk yeniden çizilmez.
private struct KabukUstCubukBaglayici: View {
    @ObservedObject var harita: HaritaModeli
    @ObservedObject var oturum: OturumDurumu
    let haritaSekmesi: Bool
    let magazaAc: () -> Void
    let seriAc: () -> Void

    var body: some View {
        let geriGorunur: Bool = haritaSekmesi && harita.seciliPart != nil
        return UstCubuk(
            durum: oturum,
            magazaAc: magazaAc,
            seriAc: seriAc,
            geriGorunur: geriGorunur,
            geri: { harita.kisimSeciminDon() }
        )
    }
}

// Alt çubuğu oturuma bağlar (öğretmende sohbet sekmesinin simgesi Sınıflarım).
private struct KabukAltCubukBaglayici: View {
    @ObservedObject var oturum: OturumDurumu
    @Binding var secili: Sekme
    let ayniSekmeyeDokunuldu: (Sekme) -> Void

    var body: some View {
        AltCubuk(secili: $secili, ogretmen: oturum.ogretmenMi, ayniSekmeyeDokunuldu: ayniSekmeyeDokunuldu)
    }
}

/// Ders paneli açıkken üst ve alt çubuğun (ve telefonun şeritlerinin) karartması; dokununca
/// panel kapanır (Android: LessonAdapter.addChromeDims + dimSystemBars, alfa 0,7).
private struct KabukKarartma: View {
    @ObservedObject var harita: HaritaModeli
    let aktif: Bool
    let kenar: Edge.Set

    var body: some View {
        ZStack {
            if aktif && harita.panel != nil {
                Color.black.opacity(KabukOlculeri.karartma)
                    .ignoresSafeArea(.all, edges: kenar)
                    .contentShape(Rectangle())
                    .onTapGesture { harita.paneliKapat() }
                    .transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.18), value: harita.panel?.id)
    }
}

/// Sohbet sekmesi: öğrencide Sobi (SobiChatFragment), öğretmende Sınıflarım
/// (TeacherClassesFragment; onay bekleyen öğretmende NotificationFragment). Henüz yazılmadı.
private struct KabukSohbetSekmesi: View {
    @ObservedObject var oturum: OturumDurumu

    var body: some View {
        let ogretmen: Bool = oturum.ogretmenMi
        let android: String = ogretmen ? "TeacherClassesFragment" : "SobiChatFragment"
        return YakindaEkrani(sekme: .bildirim, ogretmen: ogretmen, androidKarsiligi: android)
    }
}

/// Kupa Yolu sekmesi (Android: TasksFragment(MODE_CUP) → listede tek kart "cup_path").
/// Kart görünümü GunlukSoru/ klasörünün Kupa Yolu kartıdır; dokununca yönetici Kupa Yolu
/// panelini açar. Boşluklar fragment_tasks.xml ile aynı (yatay 16, üst 8 + kart 10, alt 16 + 10).
struct KabukKupaYoluSekmesi: View {
    let sec: () -> Void

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 20) {
                GunlukSoruKesfetKartGorunumu(tur: .kupaYolu, dokunuldu: sec)
            }
            .padding(.horizontal, 16)
            .padding(.top, 18)
            .padding(.bottom, 26)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color"))
    }
}
