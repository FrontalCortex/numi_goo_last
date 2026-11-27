import SwiftUI
import UIKit

// Özelleştirme ekranının dört sekmesi. Ölçü ve renkler Android layout dosyalarından alınmıştır:
// layout_tab1_bead_options.xml, layout_tab2_bead_color.xml, layout_tab3_frame_options.xml,
// layout_tab4_frame_color.xml ve AbacusCustomizationFragment'ın kodla kurduğu satırlar.

// MARK: - 1. sekme: boncuk tipi

/// Dörderli satırlar hâlinde boncuk kartları. Satın alınmamış kartların üstünde fiyat örtüsü durur.
struct OzellestirmeBoncukTipiSekmesi: View {
    @ObservedObject var model: OzellestirmeModeli

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                Text(model.boncukTipiBasligi)
                    .font(Tema.yazi(12))
                    .foregroundColor(OzellestirmeStil.solukYazi)
                    .padding(.bottom, 12)
                VStack(spacing: 8) {
                    ForEach(satirlar.indices, id: \.self) { sira in
                        satir(satirlar[sira])
                    }
                }
            }
            .padding(16)
        }
    }

    /// Kartları satır başına `boncukSatirSayisi` olacak biçimde böler.
    private var satirlar: [[OzellestirmeBoncukKarti]] {
        let adet: Int = max(1, model.boncukSatirSayisi)
        var sonuc: [[OzellestirmeBoncukKarti]] = []
        var baslangic: Int = 0
        while baslangic < model.boncukKartlari.count {
            let bitis: Int = min(baslangic + adet, model.boncukKartlari.count)
            sonuc.append(Array(model.boncukKartlari[baslangic..<bitis]))
            baslangic = bitis
        }
        return sonuc
    }

    private func satir(_ kartlar: [OzellestirmeBoncukKarti]) -> some View {
        let adet: Int = max(1, model.boncukSatirSayisi)
        return HStack(spacing: 0) {
            ForEach(0..<adet, id: \.self) { konum in
                hucre(konum < kartlar.count ? kartlar[konum] : nil)
            }
        }
    }

    /// Son satır eksikse boş hücreler kartların genişliğini korur.
    @ViewBuilder
    private func hucre(_ kart: OzellestirmeBoncukKarti?) -> some View {
        if let kart = kart {
            OzellestirmeSecimKarti(
                gorselAdi: kart.gorselAdi,
                gorselGenisligi: kart.gorselGenisligi,
                gorselYuksekligi: kart.gorselYuksekligi,
                ad: kart.ad,
                adUstBoslugu: kart.adUstBoslugu,
                icBosluk: 4,
                secili: kart.secili,
                kilitFiyati: kart.kilitli ? kart.fiyatMetni : nil,
                kilitSimgesi: kart.birimSimgeAdi,
                model: model,
                dokun: { model.boncukKartinaDokun(kart.tipAdi) }
            )
            .padding(.horizontal, 4)
        } else {
            Color.clear
                .frame(maxWidth: .infinity)
                .frame(height: OzellestirmeStil.kartYuksekligi)
                .padding(.horizontal, 4)
        }
    }
}

/// Boncuk ve çerçeve sekmelerinin ortak kartı: görsel, ad, seçili çerçevesi, kilit örtüsü.
/// Android: MaterialCardView (#1A262B, köşe 12 dp, yükseklik 110 dp).
struct OzellestirmeSecimKarti: View {
    let gorselAdi: String
    let gorselGenisligi: CGFloat
    let gorselYuksekligi: CGFloat
    let ad: String
    let adUstBoslugu: CGFloat
    let icBosluk: CGFloat
    let secili: Bool
    /// Kilitliyse örtüde gösterilecek fiyat; kilitli değilse nil.
    let kilitFiyati: String?
    let kilitSimgesi: String
    let model: OzellestirmeModeli
    let dokun: () -> Void

    var body: some View {
        Button(action: dokun) {
            icerik
                .frame(maxWidth: .infinity)
                .frame(height: OzellestirmeStil.kartYuksekligi)
                .background(model.kartZemini)
                .overlay(kilit)
                .clipShape(RoundedRectangle(cornerRadius: OzellestirmeStil.kartKosesi, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: OzellestirmeStil.kartKosesi, style: .continuous)
                        .strokeBorder(
                            secili ? model.seciliCerceveRengi : Color.clear,
                            lineWidth: model.seciliCerceveKalinligi
                        )
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(OzellestirmeBasmaStili())
        .accessibilityLabel(Text(ad))
    }

    private var icerik: some View {
        VStack(spacing: 0) {
            OzellestirmeGorsel(ad: gorselAdi, genislik: gorselGenisligi, yukseklik: gorselYuksekligi)
            Text(ad)
                .font(Tema.yazi(11))
                .foregroundColor(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .padding(.top, adUstBoslugu)
        }
        .padding(icBosluk)
    }

    @ViewBuilder
    private var kilit: some View {
        if let fiyat = kilitFiyati {
            ZStack {
                model.kilitOrtusu
                OzellestirmeFiyat(
                    metin: fiyat,
                    simgeAdi: kilitSimgesi,
                    yaziBoyu: 13,
                    simgeBoyu: 20,
                    aralik: 4,
                    renk: .white
                )
            }
        }
    }
}

// MARK: - 2. sekme: boncuk rengi

/// İki sütun: solda seçili olmayan, sağda seçili boncuğun renkleri. Renk özelliği satın
/// alınmadıysa bütün sekmenin üstünde kilit örtüsü durur.
struct OzellestirmeBoncukRengiSekmesi: View {
    @ObservedObject var model: OzellestirmeModeli

    var body: some View {
        ZStack {
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    basliklar
                        .padding(.bottom, 8)
                    HStack(alignment: .top, spacing: 12) {
                        OzellestirmeRenkSutunu(
                            model: model,
                            satirlar: model.normalSatirlar,
                            degisti: { sira, ton, parlaklik in
                                model.boncukRengiDegisti(secili: false, sira: sira, ton: ton, parlaklik: parlaklik)
                            },
                            sifirla: { model.boncukRenkleriniSifirla(secili: false) }
                        )
                        OzellestirmeRenkSutunu(
                            model: model,
                            satirlar: model.seciliSatirlar,
                            degisti: { sira, ton, parlaklik in
                                model.boncukRengiDegisti(secili: true, sira: sira, ton: ton, parlaklik: parlaklik)
                            },
                            sifirla: { model.boncukRenkleriniSifirla(secili: true) }
                        )
                    }
                }
                .padding(12)
            }
            if model.boncukRenkKilitli {
                OzellestirmeRenkKilidi(
                    fiyatMetni: model.boncukRenkFiyatMetni,
                    zemin: model.kilitOrtusu,
                    dokun: model.boncukRenkKilidineDokun,
                    simgeAdi: model.boncukRenkParaBirimiSimgesi
                )
            }
        }
    }

    private var basliklar: some View {
        HStack(spacing: 0) {
            baslik(resim: model.normalBoncukResmi, etiket: model.normalEtiketi)
            baslik(resim: model.seciliBoncukResmi, etiket: model.seciliEtiketi)
        }
    }

    private func baslik(resim: UIImage?, etiket: String) -> some View {
        VStack(spacing: 4) {
            OzellestirmeBoncukGorunumu(resim: resim, genislik: 60, yukseklik: 46)
            Text(etiket)
                .font(Tema.yazi(10))
                .foregroundColor(OzellestirmeStil.solukYazi)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Bir boncuk durumunun (seçili / seçili değil) renk satırları ve altında sıfırlama düğmesi.
private struct OzellestirmeRenkSutunu: View {
    let model: OzellestirmeModeli
    let satirlar: [OzellestirmeRenkSatiri]
    let degisti: (Int, Int, Int) -> Void
    let sifirla: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ForEach(satirlar) { satir in
                OzellestirmeRenkSatiriGorunumu(
                    model: model,
                    satir: satir,
                    ornekAraligi: 4,
                    etiketBoyu: 9,
                    altBosluk: 10,
                    degisti: { ton, parlaklik in degisti(satir.sira, ton, parlaklik) }
                )
            }
            Button(action: sifirla) {
                OzellestirmeGorsel(ad: "special_reset_ic", genislik: 56, yukseklik: 56)
                    .contentShape(Rectangle())
            }
            .buttonStyle(OzellestirmeBasmaStili())
            .padding(.top, 8)
            .accessibilityLabel(Text("Sıfırla"))
        }
        .frame(maxWidth: .infinity)
    }
}

/// Tek renk satırı. Görünüm sırası Android'de kodla eklenen sırayla aynıdır: örnek daire + etiket,
/// "Renk" yazısı, "Parlaklık" yazısı, ton kaydırıcısı, parlaklık kaydırıcısı (iki yazı da
/// kaydırıcıların üstünde durur).
struct OzellestirmeRenkSatiriGorunumu: View {
    let model: OzellestirmeModeli
    let satir: OzellestirmeRenkSatiri
    let ornekAraligi: CGFloat
    let etiketBoyu: CGFloat
    let altBosluk: CGFloat
    /// (ton, parlaklık)
    let degisti: (Int, Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: ornekAraligi) {
                Circle()
                    .fill(satir.renk)
                    .frame(width: 14, height: 14)
                Text(satir.etiket)
                    .font(Tema.yazi(etiketBoyu))
                    .foregroundColor(OzellestirmeStil.solukYazi)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            Text(model.tonEtiketi)
                .font(Tema.yazi(8))
                .foregroundColor(OzellestirmeStil.dahaSolukYazi)
                .padding(.top, 2)
            Text(model.parlaklikEtiketi)
                .font(Tema.yazi(8))
                .foregroundColor(OzellestirmeStil.dahaSolukYazi)
                .padding(.top, 2)
            OzellestirmeKaydirici(
                deger: satir.ton,
                azami: model.tonAzami,
                zemin: model.tonGradyani,
                basRengi: satir.renk,
                degisti: { yeniTon in degisti(yeniTon, satir.parlaklik) }
            )
            OzellestirmeKaydirici(
                deger: satir.parlaklik,
                azami: model.parlaklikAzami,
                zemin: satir.parlaklikGradyani,
                basRengi: satir.renk,
                degisti: { yeniParlaklik in degisti(satir.ton, yeniParlaklik) }
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 4)
        .padding(.bottom, altBosluk)
    }
}

// MARK: - 3. sekme: çerçeve tipi

/// İkişerli satırlar hâlinde çerçeve kartları.
struct OzellestirmeCerceveTipiSekmesi: View {
    @ObservedObject var model: OzellestirmeModeli

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                Text(model.cerceveTipiBasligi)
                    .font(Tema.yazi(12))
                    .foregroundColor(OzellestirmeStil.solukYazi)
                    .padding(.bottom, 12)
                VStack(spacing: 12) {
                    ForEach(satirlar.indices, id: \.self) { sira in
                        HStack(spacing: 8) {
                            ForEach(satirlar[sira]) { kart in
                                kartGorunumu(kart)
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }

    private var satirlar: [[OzellestirmeCerceveKarti]] {
        let adet: Int = max(1, model.cerceveSatirSayisi)
        var sonuc: [[OzellestirmeCerceveKarti]] = []
        var baslangic: Int = 0
        while baslangic < model.cerceveKartlari.count {
            let bitis: Int = min(baslangic + adet, model.cerceveKartlari.count)
            sonuc.append(Array(model.cerceveKartlari[baslangic..<bitis]))
            baslangic = bitis
        }
        return sonuc
    }

    private func kartGorunumu(_ kart: OzellestirmeCerceveKarti) -> some View {
        OzellestirmeSecimKarti(
            gorselAdi: kart.gorselAdi,
            gorselGenisligi: 70,
            gorselYuksekligi: 50,
            ad: kart.ad,
            adUstBoslugu: 8,
            icBosluk: 12,
            secili: kart.secili,
            kilitFiyati: kart.kilitli ? kart.fiyatMetni : nil,
            kilitSimgesi: "gold_ic",
            model: model,
            dokun: { model.cerceveKartinaDokun(kart.tipAdi) }
        )
    }
}

// MARK: - 4. sekme: çerçeve rengi

/// Seçili çerçevenin renk satırları ve sıfırlama düğmesi; renk özelliği yoksa kilit örtüsü.
struct OzellestirmeCerceveRengiSekmesi: View {
    @ObservedObject var model: OzellestirmeModeli

    var body: some View {
        ZStack {
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    ForEach(model.cerceveSatirlari) { satir in
                        OzellestirmeRenkSatiriGorunumu(
                            model: model,
                            satir: satir,
                            ornekAraligi: 6,
                            etiketBoyu: 10,
                            altBosluk: 14,
                            degisti: { ton, parlaklik in
                                model.cerceveRengiDegisti(sira: satir.sira, ton: ton, parlaklik: parlaklik)
                            }
                        )
                    }
                    Button(action: model.cerceveRenkleriniSifirla) {
                        OzellestirmeGorsel(ad: "special_reset_ic", genislik: 40, yukseklik: 40)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(OzellestirmeBasmaStili())
                    .padding(.top, 12)
                    .accessibilityLabel(Text("Sıfırla"))
                }
                .padding(16)
            }
            if model.cerceveRenkKilitli {
                OzellestirmeRenkKilidi(
                    fiyatMetni: model.renkOzelligiFiyatMetni,
                    zemin: model.kilitOrtusu,
                    dokun: model.cerceveRenkKilidineDokun
                )
            }
        }
    }
}
