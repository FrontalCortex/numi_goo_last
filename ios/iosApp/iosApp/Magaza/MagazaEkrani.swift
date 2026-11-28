import SwiftUI

// Mağaza ekranı. Android karşılığı: ShopFragment.kt + res/layout/fragment_shop.xml.
// Üst çubuktaki göstergelere (ve özelleştirme ekranındaki bakiyeye) dokununca açılır.
// (Alt çubuktaki mağaza sekmesi mağaza değil abaküs özelleştirmeyi açar: MainActivity.kt R.id.shop.)
//
// Yapı (yukarıdan aşağı): üst çubuk (kapat, can, altın, anahtar, Sobi kredisi) · süper (Pro) kartı ·
// Özel Teklifler (reklamla sandık, reklamla can, seri dondurma) · Can (Pro aboneliği, anahtarla can) ·
// Sobi kredisi paketleri · Altın paketleri · Anahtar paketleri · satın alımları geri yükle (yalnızca iOS).
// iOS'a alınmayanlar: Davet kartı (shopInviteCard) ve Öğretmene sor kredisi bölümü (Android'de de gizli).
struct MagazaEkrani: View {
    @StateObject private var model: MagazaModeli
    private let kapat: () -> Void

    /// - Parameters:
    ///   - kapat: Kapat düğmesine basıldı (Android: popBackStack).
    ///   - servisler: Ödeme, cüzdan, can, reklam ve seri servisleri; verilmezse sahte servisler.
    ///   - sandikAc: Reklam izlendi; sıradan (COMMON) sandık ekranı bu reklam nonce'uyla açılmalı.
    ///     Verilmezse "hazır değil" bildirimi gösterilir.
    ///   - planKarsilastirmaAc: Süper karta dokunuldu (Android: ProDiffirentFragment).
    ///   - odak: Açılışta kaydırılacak kart (Android: newInstance(focusStreakFreeze, focusSobiCredits)).
    init(
        kapat: @escaping () -> Void,
        servisler: MagazaBagimliliklar = MagazaBagimliliklar.sahte,
        sandikAc: ((String) -> Void)? = nil,
        planKarsilastirmaAc: (() -> Void)? = nil,
        odak: MagazaOdak? = nil
    ) {
        self.kapat = kapat
        _model = StateObject(
            wrappedValue: MagazaModeli(
                servisler: servisler,
                sandikAc: sandikAc,
                planKarsilastirmaAc: planKarsilastirmaAc,
                baslangicOdagi: odak
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
        .overlay {
            if model.dondurmaOnayiAcik {
                MagazaDondurmaPenceresi(
                    onayla: model.seriDondurmayiOnayla,
                    vazgec: { model.dondurmaOnayiAcik = false }
                )
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .preferredColorScheme(.dark)
        .onAppear { model.basla() }
        .onDisappear { model.dur() }
    }
}

// MARK: - Üst çubuk

/// shopHeader: solda kapat düğmesi, sağda can · altın · anahtar · Sobi kredisi.
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
            .accessibilityLabel(Text(Metin.al("ios_erisim_kapat")))

            Spacer(minLength: 0)

            MagazaCanGostergesi(bolum: model.canBolumu, pro: model.proPlan)
                .padding(.trailing, 12)
            MagazaUstGosterge(
                simge: MagazaGorsel(ad: "gold_ic", genislik: 22, yukseklik: 22),
                metin: String(model.altin),
                renk: MagazaStil.altinYaziRengi,
                erisilebilirlikAdi: Metin.al("fragment_shop_text_desc")
            )
            .padding(.trailing, 12)
            MagazaUstGosterge(
                simge: MagazaGorsel(ad: "key", genislik: 22, yukseklik: 22),
                metin: String(model.anahtar),
                renk: MagazaStil.anahtarYaziRengi,
                erisilebilirlikAdi: Metin.al("fragment_shop_text_desc_2")
            )
            .padding(.trailing, 12)
            MagazaUstGosterge(
                simge: MagazaSobiKafasi(boyut: 22),
                metin: String(model.sobiKredisi),
                renk: MagazaStil.sobiYaziRengi,
                erisilebilirlikAdi: Metin.al("shop_sobi_credit_desc")
            )
            .padding(.trailing, 16)
        }
        .frame(height: MagazaStil.ustCubukYuksekligi)
        .frame(maxWidth: .infinity)
        .background(Color("background_color"))
    }
}

/// Can göstergesi (Android: EnergyDisplay): sonsuzda kalbin üzerinde "∞", Pro'da Pro kalbi.
private struct MagazaCanGostergesi: View {
    let bolum: MagazaCanBolumu
    let pro: Bool

    var body: some View {
        HStack(spacing: 4) {
            ZStack {
                MagazaGorsel(ad: pro ? "heart_pro_ic" : "heart_ic", genislik: 22, yukseklik: 22)
                if bolum.sonsuz {
                    Text("∞")
                        .font(Tema.yazi(13))
                        .foregroundColor(.white)
                }
            }
            .frame(width: 22, height: 22)
            Text(bolum.ustCanMetni)
                .font(Tema.yazi(15))
                .foregroundColor(MagazaStil.canYaziRengi)
                .lineLimit(1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(Metin.al("fragment_shop_shop_energy_icon_desc") + ": " + bolum.ustCanMetni))
    }
}

private struct MagazaUstGosterge<Simge: View>: View {
    let simge: Simge
    let metin: String
    let renk: Color
    let erisilebilirlikAdi: String

    var body: some View {
        HStack(spacing: 4) {
            simge
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
        ScrollViewReader { vekil in
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    MagazaSuperVeTeklifler(model: model)
                    MagazaCanBolumuGorunumu(model: model)
                    MagazaSobiBolumu(model: model)
                    MagazaParaBolumleri(model: model)
                }
                .padding(.horizontal, MagazaStil.icerikYatayBosluk)
                .padding(.top, MagazaStil.icerikUstBosluk)
                .padding(.bottom, MagazaStil.icerikAltBosluk)
            }
            .onAppear { odagaKaydir(vekil) }
        }
    }

    /// Android: focusCard — kaydırma anında, vurgu açılış animasyonundan sonra.
    private func odagaKaydir(_ vekil: ScrollViewProxy) {
        guard let odak = model.baslangicOdagi else { return }
        let hedef: UnitPoint = (odak == .alt) ? UnitPoint.bottom : UnitPoint.top
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            vekil.scrollTo(odak, anchor: hedef)
        }
        model.vurguyuBaslat()
    }
}

/// Süper kart, "Özel Teklifler" başlığı ve üç teklif kartı (sandık reklamı, can reklamı, seri dondurma).
private struct MagazaSuperVeTeklifler: View {
    @ObservedObject var model: MagazaModeli

    var body: some View {
        VStack(spacing: 0) {
            MagazaSuperKart(dugmeMetni: model.superDugmeMetni, eylem: model.superKartaDokunuldu)
                .padding(.bottom, 24)

            MagazaBolumBasligi(metin: model.ozelTekliflerBasligi, boyut: 16, altBosluk: 10)
            MagazaReklamKarti(
                gorselAdi: "new_chest_close_ic1",
                baslik: Metin.al("fragment_shop_text_2"),
                aciklama: Metin.al("fragment_shop_text_3"),
                dugmeMetni: Metin.al("fragment_shop_text_4"),
                dugmeOpakligi: 1,
                dugmeEtkin: true,
                ucus: nil,
                eylem: model.sandikIcinReklamIzle
            )
            .padding(.bottom, 12)
            MagazaReklamKarti(
                gorselAdi: "heart_ic",
                baslik: Metin.al("fragment_shop_text_5"),
                aciklama: Metin.al("fragment_shop_text_6"),
                dugmeMetni: Metin.al("fragment_shop_text_7"),
                dugmeOpakligi: model.canBolumu.reklamOpakligi,
                dugmeEtkin: model.canBolumu.canAlinabilir,
                ucus: model.reklamKalpUcusu,
                eylem: model.canIcinReklamIzle
            )
            .zIndex(1)
            .padding(.bottom, 12)
            MagazaSeriDondurmaKarti(
                dondurmaVar: model.dondurmaVar,
                ucus: model.dondurmaUcusu,
                eylem: model.seriDondurmayaDokunuldu
            )
            .modifier(MagazaOdakIsareti(odak: .seriDondurma, vurgulanan: model.vurgulanan))
            .zIndex(1)
            .padding(.bottom, 24)
        }
    }
}

/// "Can" başlığı, Pro kartı ve anahtarla can kartı.
private struct MagazaCanBolumuGorunumu: View {
    @ObservedObject var model: MagazaModeli

    var body: some View {
        VStack(spacing: 0) {
            MagazaBolumBasligi(metin: model.canBasligi, boyut: 18, altBosluk: 12)
            MagazaProKarti(fiyatMetni: model.proFiyatMetni, eylem: model.proKartinaDokunuldu)
                .padding(.bottom, 12)
            MagazaEkstraCanKarti(
                bolum: model.canBolumu,
                ucus: model.anahtarKalpUcusu,
                eylem: model.anahtarlaCanAl
            )
            .zIndex(1)
            .padding(.bottom, 24)
        }
    }
}

/// Sobi kredisi bölümü (Android: shop_sobi_credit_title, shopSobiCreditBalanceText, shopSobiCreditSection).
private struct MagazaSobiBolumu: View {
    @ObservedObject var model: MagazaModeli

    var body: some View {
        VStack(spacing: 0) {
            MagazaBolumBasligi(metin: model.sobiBasligi, boyut: 16, altBosluk: 2)
            Text(model.sobiBilgiMetni)
                .font(Tema.yazi(12))
                .foregroundColor(MagazaStil.aciklamaRengi)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, 12)
            MagazaSobiSirasi(model: model)
                .modifier(MagazaOdakIsareti(odak: .sobiKredisi, vurgulanan: model.vurgulanan))
                .zIndex(1)
                .padding(.bottom, 24)
        }
    }
}

/// Üç Sobi kredisi kartı yan yana, eşit genişlikte (kartlar arası 9 pt: 6 + 3 kenar boşlukları).
private struct MagazaSobiSirasi: View {
    @ObservedObject var model: MagazaModeli

    var body: some View {
        HStack(alignment: .top, spacing: 9) {
            ForEach(model.sobiPaketleri) { paket in
                MagazaSobiKartiGorunumu(
                    paket: paket,
                    ucus: ucus(paket.urunKimligi),
                    eylem: { model.sobiPaketiSatinAl(paket) }
                )
                .zIndex(ucus(paket.urunKimligi) == nil ? 0 : 1)
            }
        }
    }

    private func ucus(_ urunKimligi: String) -> MagazaUcusIstegi? {
        guard let ucus = model.paketUcusu, ucus.urunKimligi == urunKimligi else { return nil }
        return ucus.istek
    }
}

/// Altın ve anahtar paketleri; en altta satın alımları geri yükle (yalnızca iOS).
private struct MagazaParaBolumleri: View {
    @ObservedObject var model: MagazaModeli

    var body: some View {
        VStack(spacing: 0) {
            MagazaBolumBasligi(metin: model.altinBasligi, boyut: 16, altBosluk: 12)
            MagazaPaketSirasi(model: model, kartlar: model.altinKartlari)
                .zIndex(1)
                .padding(.bottom, 24)

            MagazaBolumBasligi(metin: model.anahtarBasligi, boyut: 16, altBosluk: 12)
            MagazaPaketSirasi(model: model, kartlar: model.anahtarKartlari)
                .zIndex(1)
                .padding(.bottom, 16)

            MagazaGeriYukleDugmesi(eylem: model.geriYukle)
                .id(MagazaOdak.alt)
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
            Text(Metin.al("ios_magaza_geri_yukle"))
                .font(Tema.yazi(13))
                .foregroundColor(MagazaStil.aciklamaRengi)
                .padding(.vertical, 8)
                .padding(.horizontal, 12)
                .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
    }
}

// MARK: - Seri dondurma onay penceresi

/// Android: confirmBuyStreakFreeze → dialog_streak_options.xml + StreakViews.buildOptionRows.
/// Zemin #18272D, çizgi #2E4149 (2), köşe 20, kenar boşluğu 24, iç boşluk 20; iki seçenek satırı
/// (Satın al seçili: #2A3B42 + 3 pt turuncu çizgi; Vazgeç: #203137 + 2 pt çizgi), köşe 14.
private struct MagazaDondurmaPenceresi: View {
    let onayla: () -> Void
    let vazgec: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()
                .onTapGesture(perform: vazgec)
            kutu
        }
    }

    private var kutu: some View {
        let bedel: String = String(MagazaStil.dondurmaBedeli)
        return VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("ui_seri_dondurma"))
                .font(Tema.yazi(18))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(Metin.al("ui_bir_gun_calisamazsan_serin_bozulmaz_x", bedel))
                .font(Tema.yazi(13))
                .foregroundColor(MagazaStil.pencereAltYazi)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 6)
            VStack(spacing: 12) {
                MagazaPencereSecenegi(
                    baslik: Metin.al("ui_satin_al"),
                    sag: Metin.al("ui_x_altin", bedel),
                    secili: true,
                    eylem: onayla
                )
                MagazaPencereSecenegi(
                    baslik: Metin.al("ui_vazgec"),
                    sag: "",
                    secili: false,
                    eylem: vazgec
                )
            }
            .padding(.top, 16)
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(MagazaStil.pencereZemini)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(MagazaStil.pencereCizgisi, lineWidth: 2)
        )
        .padding(.horizontal, 24)
        .frame(maxWidth: 420)
    }
}

/// Pencerenin tek seçenek satırı (Android: StreakViews.buildOptionRows satırı).
private struct MagazaPencereSecenegi: View {
    let baslik: String
    let sag: String
    let secili: Bool
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 0) {
                Text(baslik)
                    .font(Tema.yazi(17))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(sag)
                    .font(Tema.yazi(15))
                    .foregroundColor(MagazaStil.pencereAltin)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(secili ? MagazaStil.seciliSecenekZemini : MagazaStil.secenekZemini)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(
                        secili ? MagazaStil.seciliSecenekCizgisi : MagazaStil.pencereCizgisi,
                        lineWidth: secili ? 3 : 2
                    )
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
    }
}
