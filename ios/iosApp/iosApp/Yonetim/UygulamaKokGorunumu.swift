import SwiftUI

// Uygulamanın kök görünümü: taban (giriş akışı ya da kabuk) + üstündeki tam ekran katmanları.
// Hangi ekranın açık olduğunu UygulamaYoneticisi tutar; burada yalnızca çizilir.

@MainActor
struct UygulamaKokGorunumu: View {
    @StateObject private var yonetici: UygulamaYoneticisi
    /// Seri sayacı yalnızca uygulama öndeyken çalışır (Android: StudyTimeTracker, arkaya geçince null ekran).
    @Environment(\.scenePhase) private var sahneEvresi

    /// - Parameter girisAtla: true ise giriş akışı atlanır, doğrudan kabuk açılır (ekran galerisi).
    init(girisAtla: Bool = false) {
        // Otomatik kapanış: yönetici yalnızca görünüm ilk kez kurulurken oluşturulur.
        _yonetici = StateObject(wrappedValue: UygulamaYoneticisi(girisAtla: girisAtla))
    }

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            taban
            ForEach(yonetici.katmanlar) { katman in
                YonetimKatmanGorunumu(katman: katman, yonetici: yonetici)
                    .zIndex(Double(katman.id))
                    .transition(UygulamaKokGorunumu.gecis(katman.ekran))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: yonetici.katmanKimlikleri)
        // Uygulama sistem temasından bağımsız hep koyudur (Android: Theme.NumiGoo).
        .preferredColorScheme(.dark)
        // Günlük seri: önde ders ekranı varken çalışma süresi sayılır (YonetimSeriAkisi.swift).
        .onChange(of: yonetici.katmanKimlikleri) { _ in
            yonetici.seriCalismaEkraniniGuncelle(aktif: sahneEvresi == .active)
        }
        .onChange(of: sahneEvresi) { evre in
            yonetici.seriCalismaEkraniniGuncelle(aktif: evre == .active)
        }
    }

    /// Katmanın giriş/çıkış geçişi. Ders akışı ekranları (abaküs, kör ders, sonuç, sandık) sağdan
    /// girip SOLA çıkar, 400 ms (Android: queue_screen_in / queue_screen_out); ötekiler eskisi gibi.
    private static func gecis(_ ekran: YonetimEkrani) -> AnyTransition {
        if ekran.saydam {
            return AnyTransition.opacity
        }
        switch ekran {
        case .abakusDers, .korDers, .sonuc, .sandik, .seri(.yeniSeri), .rozetKutlama:
            return SonucGecisi.sagdanGirSolaCik
        case .seri(.seri):
            // Android: openStreakFragment → slide_down / slide_up.
            return AnyTransition.move(edge: .top)
        default:
            return AnyTransition.move(edge: .trailing)
        }
    }

    @ViewBuilder
    private var taban: some View {
        switch yonetici.asama {
        case .giris:
            girisAkisi
        case .kabuk:
            AnaKabuk(yonetici: yonetici)
        }
    }

    /// Çıkıştan sonra `girisKimligi` değişir; akış (ve görünüm modeli) sıfırdan kurulur ve açılış
    /// atlanıp başlangıç ekranından başlanır (Android: çıkışta LoginStartActivity doğrudan açılır).
    private var girisAkisi: some View {
        let nesne: UygulamaYoneticisi = yonetici
        return GirisAkisi(
            servis: nesne.kimlikServisi,
            girisYapildi: { nesne.girisYapildi() },
            misafirOlarakDevam: { hedef in nesne.misafirOlarakDevam(hedef) },
            acilisiAtla: nesne.girisKimligi > 0
        )
        .id(nesne.girisKimligi)
    }
}

/// Yığındaki tek bir katman: yöneticinin `YonetimEkrani` değerini ilgili ekranın giriş görünümüne çevirir.
@MainActor
struct YonetimKatmanGorunumu: View {
    let katman: YonetimKatmani
    @ObservedObject var yonetici: UygulamaYoneticisi

    var body: some View {
        ZStack {
            if !katman.ekran.saydam {
                Color("background_color").ignoresSafeArea()
            }
            icerik
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func kapat() {
        yonetici.kapat(katman.id)
    }

    @ViewBuilder
    private var icerik: some View {
        switch katman.ekran {
        case .abakusDers(let baslangic):
            abakusDers(baslangic)
        case .korDers(let baslangic, let kaynak):
            korDers(baslangic, kaynak)
        case .sonuc(let girdi, let baglam):
            sonuc(girdi, baglam)
        case .sandik(let girdi, let kapandi):
            sandik(girdi, kapandi)
        case .magaza:
            magaza
        case .kupa:
            kupa
        case .kupaYoluOdul(let kupaAlani):
            kupaYoluOdul(kupaAlani)
        case .kupaGecmisi(let secim):
            kupaGecmisi(secim)
        case .rozetler:
            rozetler
        case .rozetDetay(let modAdi):
            RozetDetayEkrani(modAdi: modAdi, kapat: { kapat() })
        case .liderTablosu(let partId, let dersAnahtari, let baslik):
            KupaLiderTablosuEkrani(partId: partId, dersAnahtari: dersAnahtari, dersBasligi: baslik, kapat: { kapat() })
        case .ogretici(let hedef):
            ogretici(hedef)
        case .seri(let tur):
            seriKatmani(tur)
        case .plan:
            planKatmani
        case .rozetKutlama(let kuyruk, let bitti):
            rozetKutlamaKatmani(kuyruk, bitti)
        case .sobiAnaliz(let istek):
            sobiAnalizKatmani(istek)
        case .yerTutucu(let veri):
            YonetimYerTutucuEkrani(
                baslik: veri.baslik, aciklama: veri.aciklama, ayrintilar: veri.ayrintilar,
                birincilDugme: nil, birincilEylem: {}, kapat: { kapat() }
            )
        }
    }

    // MARK: Ders, sonuç, sandık

    private func abakusDers(_ baslangic: AbakusDersBaslangici) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return AbakusDersEkrani(
            dersBaslangici: baslangic,
            hizmetler: GercekAbakusDersHizmetleri(),
            sobiAnalizi: sobiAnalizBaglantisi(nesne),
            // Android isFirstRunLesson: hesapsız ilk öğreticinin dersinde çıkış düğmesi gizli.
            cikisGizli: nesne.ilkOgreticiAkisi,
            bitti: { sonuc in nesne.abakusDersiBitti(sonuc, baslangic: baslangic, katman: no) },
            kapat: { nesne.kapat(no) }
        )
    }

    /// Yanlış cevap panelindeki "Sobi ile analiz et" düğmesinin bağlantısı (Android: MistakeReviewDialog).
    /// Karar ortak koddaki kural motorundan: `MistakeReviewDialog.canReview` (yalnızca toplama/çıkarma ve
    /// abaküse sığan diziler). Android ayrıca öğretmen hesabında düğmeyi gizler (SobiAccess.isAllowed);
    /// iOS'ta öğretmen rolü yok. Pencere üstte açılır; kapanınca `istek.kapandi` (sandık dersinde süre sürer).
    private func sobiAnalizBaglantisi(_ nesne: UygulamaYoneticisi) -> AbakusSobiAnalizBaglantisi {
        return AbakusSobiAnalizBaglantisi(
            yapilabilir: { sayilar in SobiAnalizModeli.analizYapilabilir(sayilar) },
            istendi: { istek in
                nesne.ac(.sobiAnaliz(istek))
            }
        )
    }

    private func sobiAnalizKatmani(_ istek: AbakusSobiAnalizIstegi) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SobiAnalizEkrani(
            istek: istek,
            kapat: {
                istek.kapandi()
                nesne.kapat(no)
            }
        )
    }

    private func korDers(_ baslangic: KorDersBaslangici, _ kaynak: YonetimDersKaynagi) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return KorDersEkrani(
            baslangic: baslangic,
            kayit: SahteKorDersKayitServisi(),
            enerji: SahteKorDersEnerjiServisi(),
            ses: GercekKorDersSesServisi(),
            sobiAnalizi: sobiAnalizBaglantisi(nesne),
            bitti: { sonuc in nesne.korDersBitti(sonuc, kaynak: kaynak, baslangic: baslangic, katman: no) },
            kapat: { nesne.kapat(no) }
        )
    }

    private func sonuc(_ girdi: SonucGirdisi, _ baglam: YonetimSonucBaglami) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SonucEkrani(
            girdi: girdi,
            ses: GercekSonucSesCalar(),
            devam: { nesne.sonucDevam(girdi, baglam: baglam, katman: no) },
            tekrar: { nesne.sonucTekrar(baglam: baglam, katman: no) }
        )
    }

    private func sandik(_ girdi: SandikGirdisi, _ kapandi: @escaping (SandikSonucu) -> Void) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SandikEkrani(
            girdi: girdi,
            odulServisi: SahteSandikOdulServisi(),
            ses: GercekSandikSesCalar(),
            kapat: { sonuc in
                kapandi(sonuc)
                nesne.kapat(no)
            }
        )
    }

    // MARK: Mağaza, kupa, rozet

    private var magaza: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return MagazaEkrani(
            kapat: { nesne.kapat(no) },
            sandikAc: { nonce in nesne.reklamSandiginiAc(nonce: nonce) },
            planKarsilastirmaAc: { nesne.planKarsilastirmaAc() }
        )
    }

    private var kupa: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return KupaEkrani(
            kapat: { nesne.kapat(no) },
            kupaDersiBaslat: { veri in nesne.kupaDersiniBaslat(veri) },
            magazaAc: { nesne.magazaAc() },
            sonuc: nesne.kupaSonucu,
            // Android: karttaki çubuk/sandık panelden CupPathRoadFragment'ı ya da kupa yolu sandığını
            // (NewChestFragment) açar; panel kapanmış olur (KupaEkrani kapat'ı önce çağırır).
            yolAc: { alan in nesne.ac(.kupaYoluOdul(kupaAlani: alan)) },
            sandikAc: { girdi in nesne.ac(.sandik(girdi, kapandi: { _ in })) }
        )
    }

    /// Kupa Yolu ödül yolu: hazır eşiğe dokunulunca sandık üstte açılır, kapanınca bu ekran tazelenir.
    private func kupaYoluOdul(_ kupaAlani: String) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return KupaYoluOdulEkrani(
            kupaAlani: kupaAlani,
            kapat: { nesne.kapat(no) },
            sandikAc: { girdi, kapandi in nesne.ac(.sandik(girdi, kapandi: kapandi)) }
        )
    }

    private func kupaGecmisi(_ secim: ProfilKupaSecimi) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return KupaGecmisiEkrani(
            kullaniciKimligi: secim.uid,
            alan: secim.alan,
            baslik: secim.baslik,
            animasyonDosyasi: secim.animasyonDosyasi,
            kapat: { nesne.kapat(no) }
        )
    }

    private var rozetler: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return RozetlerEkrani(
            kapat: { nesne.kapat(no) },
            rozetSecildi: { secim in nesne.rozetSecildi(secim) },
            ortam: KupaOrtami.gercekSesli
        )
    }

    // MARK: Öğretici

    /// İçeriği taşınmış öğretici OgreticiEkrani'yla, taşınmamışsa eski yer tutucuyla açılır.
    @ViewBuilder
    private func ogretici(_ hedef: YonetimOgreticiHedefi) -> some View {
        if OgreticiBaslangici.icerikVar(hedef.ogreticiNo) {
            ogreticiEkrani(hedef)
        } else {
            ogreticiYerTutucusu(hedef)
        }
    }

    private func ogreticiEkrani(_ hedef: YonetimOgreticiHedefi) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        let baslangic = OgreticiBaslangici(
            ogreticiNo: hedef.ogreticiNo,
            ilkAcilis: hedef.ilkAcilis,
            baslangicAdimi: 0,
            baslik: hedef.baslik
        )
        return OgreticiEkrani(
            baslangic: baslangic,
            ses: OgreticiGercekSesCalici(),
            bitti: { _ in nesne.ogreticiGecildi(hedef, katman: no) },
            kapat: { nesne.kapat(no) }
        )
    }

    private func ogreticiYerTutucusu(_ hedef: YonetimOgreticiHedefi) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return YonetimYerTutucuEkrani(
            baslik: hedef.baslik,
            aciklama: "Öğretici (\(hedef.ogreticiNo))",
            ayrintilar: ["Kısım: \(hedef.partId)", "Harita sırası: \(hedef.sira)"],
            birincilDugme: "Öğreticiyi geç, derse başla",
            birincilEylem: { nesne.ogreticiGecildi(hedef, katman: no) },
            kapat: { nesne.kapat(no) }
        )
    }
}

/// Henüz yazılmamış ekranların yer tutucusu (öğretici, yarış paneli, öğretmene sor, abaküs
/// alıştırması, bize ulaşın). Metinler Android'de yoktur; ekran yazılınca kalkacaktır.
struct YonetimYerTutucuEkrani: View {
    let baslik: String
    let aciklama: String
    let ayrintilar: [String]
    /// Doluysa "Kapat"ın üstünde ikinci bir düğme gösterilir.
    let birincilDugme: String?
    let birincilEylem: () -> Void
    let kapat: () -> Void

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            VStack(spacing: 16) {
                Text(baslik)
                    .font(Tema.yazi(24))
                    .foregroundColor(Color.white)
                    .multilineTextAlignment(.center)
                Text(aciklama)
                    .font(Tema.yazi(17))
                    .foregroundColor(Color("lesson_completed"))
                    .multilineTextAlignment(.center)
                satirlar
                Text("Bu ekran henüz hazır değil.")
                    .font(Tema.yazi(14, kalin: false))
                    .foregroundColor(Color.white.opacity(0.6))
                dugmeler
            }
            .padding(24)
        }
    }

    private var satirlar: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(Array(ayrintilar.enumerated()), id: \.offset) { satir in
                Text(satir.element)
                    .font(Tema.yazi(15, kalin: false))
                    .foregroundColor(Color.white)
            }
        }
    }

    private var dugmeler: some View {
        VStack(spacing: 12) {
            if let metin = birincilDugme {
                YonetimYerTutucuDugmesi(metin: metin, vurgulu: true, eylem: birincilEylem)
            }
            YonetimYerTutucuDugmesi(metin: "Kapat", vurgulu: birincilDugme == nil, eylem: kapat)
        }
        .padding(.top, 8)
    }
}

struct YonetimYerTutucuDugmesi: View {
    let metin: String
    let vurgulu: Bool
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(16))
                .foregroundColor(vurgulu ? Color("background_color") : Color.white)
                .padding(.horizontal, 24)
                .frame(minHeight: 44)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(vurgulu ? Color("lesson_completed") : Color("circleBackground_color"))
                )
        }
        .buttonStyle(.plain)
    }
}
