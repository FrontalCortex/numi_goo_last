import SwiftUI

// Mağaza ekranı. Android karşılığı: ShopFragment.kt + res/layout/fragment_shop.xml.
// Üst çubuktaki göstergelere (ve özelleştirme ekranındaki bakiyeye) dokununca açılır.
//
// Yapı (yukarıdan aşağı): üst çubuk (kapat, can, altın, anahtar) · süper (Pro) kartı ·
// Özel Teklifler (reklamla sandık, reklamla can) · Can (Pro aboneliği, anahtarla can) ·
// Altın paketleri · Anahtar paketleri · satın alımları geri yükle (yalnızca iOS).
struct MagazaEkrani: View {
    @StateObject private var model: MagazaModeli
    private let kapat: () -> Void

    /// - Parameters:
    ///   - kapat: Kapat düğmesine basıldı (Android: popBackStack).
    ///   - servisler: Ödeme, cüzdan, can ve reklam servisleri; verilmezse sahte servisler.
    ///   - sandikAc: Reklam izlendi; sıradan (COMMON) sandık ekranı bu reklam nonce'uyla açılmalı.
    ///     Verilmezse "hazır değil" bildirimi gösterilir.
    ///   - planKarsilastirmaAc: Süper karta dokunuldu (Android: ProDiffirentFragment).
    init(
        kapat: @escaping () -> Void,
        servisler: MagazaBagimliliklar = MagazaBagimliliklar.sahte,
        sandikAc: ((String) -> Void)? = nil,
        planKarsilastirmaAc: (() -> Void)? = nil
    ) {
        self.kapat = kapat
        _model = StateObject(
            wrappedValue: MagazaModeli(
                servisler: servisler,
                sandikAc: sandikAc,
                planKarsilastirmaAc: planKarsilastirmaAc
            )
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            MagazaUstCubuk(model: model, kapat: kapat)
            Color("circleBackground_color")
                .frame(height: 1)
            MagazaIcerik(model: model)
        }
        .background(Color("background_color").ignoresSafeArea())
        .overlay(alignment: .bottom) {
            if let metin = model.bildirim {
                MagazaBildirimGorunumu(metin: metin)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .preferredColorScheme(.dark)
        .onAppear { model.basla() }
        .onDisappear { model.dur() }
    }
}

// MARK: - Üst çubuk

/// shopHeader: solda kapat düğmesi, sağda can · altın · anahtar.
private struct MagazaUstCubuk: View {
    @ObservedObject var model: MagazaModeli
    let kapat: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: kapat) {
                MagazaGorsel(ad: "cross", genislik: 24, yukseklik: 24)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            .buttonStyle(MagazaBasmaStili())
            .padding(.leading, 8)
            .accessibilityLabel(Text("Kapat"))

            Spacer(minLength: 0)

            MagazaUstGosterge(
                simgeAdi: "heart_ic",
                metin: model.canBolumu.ustCanMetni,
                renk: MagazaStil.canYaziRengi,
                erisilebilirlikAdi: "Can"
            )
            .padding(.trailing, 12)
            MagazaUstGosterge(
                simgeAdi: "gold_ic",
                metin: String(model.altin),
                renk: MagazaStil.altinYaziRengi,
                erisilebilirlikAdi: "Altın"
            )
            .padding(.trailing, 12)
            MagazaUstGosterge(
                simgeAdi: "key",
                metin: String(model.anahtar),
                renk: MagazaStil.anahtarYaziRengi,
                erisilebilirlikAdi: "Anahtar"
            )
            .padding(.trailing, 16)
        }
        .frame(height: MagazaStil.ustCubukYuksekligi)
        .frame(maxWidth: .infinity)
        .background(Color("background_color"))
    }
}

private struct MagazaUstGosterge: View {
    let simgeAdi: String
    let metin: String
    let renk: Color
    let erisilebilirlikAdi: String

    var body: some View {
        HStack(spacing: 4) {
            MagazaGorsel(ad: simgeAdi, genislik: 22, yukseklik: 22)
            Text(metin)
                .font(Tema.yazi(15))
                .foregroundColor(renk)
                .lineLimit(1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(erisilebilirlikAdi + ": " + metin))
    }
}

// MARK: - Kaydırılan içerik

private struct MagazaIcerik: View {
    @ObservedObject var model: MagazaModeli

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                MagazaSuperKart(eylem: model.superKartaDokunuldu)
                    .padding(.bottom, 24)

                MagazaBolumBasligi(metin: model.ozelTekliflerBasligi, boyut: 16, altBosluk: 10)
                MagazaReklamKarti(
                    gorselAdi: "new_chest_close_ic1",
                    baslik: "Ücretsiz Sandık",
                    aciklama: "Bir reklam izleyerek sandık kazan.",
                    dugmeMetni: "SANDIK KAZAN",
                    dugmeOpakligi: 1,
                    dugmeEtkin: true,
                    ucus: nil,
                    eylem: model.sandikIcinReklamIzle
                )
                .padding(.bottom, 12)
                MagazaReklamKarti(
                    gorselAdi: "heart_ic",
                    baslik: "Ücretsiz Can",
                    aciklama: "Bir reklam izleyerek can kazan.",
                    dugmeMetni: "CAN KAZAN",
                    dugmeOpakligi: model.canBolumu.reklamOpakligi,
                    dugmeEtkin: model.canBolumu.canAlinabilir,
                    ucus: model.reklamKalpUcusu,
                    eylem: model.canIcinReklamIzle
                )
                .zIndex(1)
                .padding(.bottom, 24)

                MagazaBolumBasligi(metin: model.canBasligi, boyut: 18, altBosluk: 12)
                MagazaProKarti(fiyatMetni: model.proFiyatMetni, eylem: model.proSatinAl)
                    .padding(.bottom, 12)
                MagazaEkstraCanKarti(
                    bolum: model.canBolumu,
                    ucus: model.anahtarKalpUcusu,
                    eylem: model.anahtarlaCanAl
                )
                .zIndex(1)
                .padding(.bottom, 24)

                MagazaBolumBasligi(metin: model.altinBasligi, boyut: 16, altBosluk: 12)
                MagazaPaketSirasi(model: model, kartlar: model.altinKartlari)
                    .zIndex(1)
                    .padding(.bottom, 24)

                MagazaBolumBasligi(metin: model.anahtarBasligi, boyut: 16, altBosluk: 12)
                MagazaPaketSirasi(model: model, kartlar: model.anahtarKartlari)
                    .zIndex(1)
                    .padding(.bottom, 16)

                MagazaGeriYukleDugmesi(eylem: model.geriYukle)
            }
            .padding(.horizontal, MagazaStil.icerikYatayBosluk)
            .padding(.top, MagazaStil.icerikUstBosluk)
            .padding(.bottom, MagazaStil.icerikAltBosluk)
        }
    }
}

/// Üç paket kartı yan yana, eşit genişlikte (kartlar arası 9 pt: 6 + 3 kenar boşlukları).
private struct MagazaPaketSirasi: View {
    @ObservedObject var model: MagazaModeli
    let kartlar: [MagazaPaketKarti]

    var body: some View {
        HStack(alignment: .top, spacing: 9) {
            ForEach(kartlar) { kart in
                MagazaPaketKartiGorunumu(
                    kart: kart,
                    ucus: ucus(kart),
                    eylem: { model.paketSatinAl(kart) }
                )
                .zIndex(ucus(kart) == nil ? 0 : 1)
            }
        }
    }

    private func ucus(_ kart: MagazaPaketKarti) -> MagazaUcusIstegi? {
        guard let ucus = model.paketUcusu, ucus.urunKimligi == kart.urunKimligi else { return nil }
        return ucus.istek
    }
}

/// Satın alımları geri yükle. Android'de yoktur; App Store kuralı gereği eklendi.
private struct MagazaGeriYukleDugmesi: View {
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Text("Satın alımları geri yükle")
                .font(Tema.yazi(13))
                .foregroundColor(MagazaStil.aciklamaRengi)
                .padding(.vertical, 8)
                .padding(.horizontal, 12)
                .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
    }
}
