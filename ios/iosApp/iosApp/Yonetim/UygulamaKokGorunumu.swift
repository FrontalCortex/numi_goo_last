import SwiftUI

// Uygulamanın kök görünümü: taban (giriş akışı ya da kabuk) + üstündeki tam ekran katmanları.
// Hangi ekranın açık olduğunu UygulamaYoneticisi tutar; burada yalnızca çizilir.

@MainActor
struct UygulamaKokGorunumu: View {
    @StateObject private var yonetici: UygulamaYoneticisi

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
                    .transition(katman.ekran.saydam ? AnyTransition.opacity : AnyTransition.move(edge: .trailing))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: yonetici.katmanKimlikleri)
        // Uygulama sistem temasından bağımsız hep koyudur (Android: Theme.NumiGoo).
        .preferredColorScheme(.dark)
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

    /// Çıkıştan sonra `girisKimligi` değişir; akış (ve görünüm modeli) sıfırdan kurulur.
    private var girisAkisi: some View {
        let nesne: UygulamaYoneticisi = yonetici
        return GirisAkisi(servis: nesne.kimlikServisi, girisYapildi: { nesne.girisYapildi() })
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
            bitti: { sonuc in nesne.abakusDersiBitti(sonuc, baslangic: baslangic, katman: no) },
            kapat: { nesne.kapat(no) }
        )
    }

    private func korDers(_ baslangic: KorDersBaslangici, _ kaynak: YonetimDersKaynagi) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return KorDersEkrani(
            baslangic: baslangic,
            bitti: { sonuc in nesne.korDersBitti(sonuc, kaynak: kaynak, baslangic: baslangic, katman: no) },
            kapat: { nesne.kapat(no) }
        )
    }

    private func sonuc(_ girdi: SonucGirdisi, _ baglam: YonetimSonucBaglami) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SonucEkrani(
            girdi: girdi,
            devam: { nesne.sonucDevam(girdi, baglam: baglam, katman: no) },
            tekrar: { nesne.sonucTekrar(baglam: baglam, katman: no) }
        )
    }

    private func sandik(_ girdi: SandikGirdisi, _ kapandi: @escaping (SandikSonucu) -> Void) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SandikEkrani(
            girdi: girdi,
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
            sandikAc: { nonce in nesne.reklamSandiginiAc(nonce: nonce) }
        )
    }

    private var kupa: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return KupaEkrani(
            kapat: { nesne.kapat(no) },
            kupaDersiBaslat: { veri in nesne.kupaDersiniBaslat(veri) },
            magazaAc: { nesne.magazaAc() },
            sonuc: nesne.kupaSonucu
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
            rozetSecildi: { secim in nesne.rozetSecildi(secim) }
        )
    }

    // MARK: Öğretici yer tutucusu

    private func ogretici(_ hedef: YonetimOgreticiHedefi) -> some View {
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
