import SwiftUI

// Mağaza ekranının kartları. Ölçü ve renkler res/layout/fragment_shop.xml'den birebir alınmıştır
// (dp → pt, sp → pt); metinler strings.xml adlarıyla `Metin.al`'dan gelir.
// Göçen (içe basılan) düğmeler Android'de DepthButtons.style ile çizilir: `GocenDugmeStili`.

/// Kartların ortak zemini: #1E2A30, köşe 14.
private struct MagazaKartZemini: ViewModifier {
    func body(content: Content) -> some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: MagazaStil.kartKosesi, style: .continuous)
                    .fill(MagazaStil.kartZemini)
            )
    }
}

/// Fırlayan simgeleri bulunduğu görünümün ortasına bindirir.
private struct MagazaUcusBindirmesi: ViewModifier {
    let ucus: MagazaUcusIstegi?

    func body(content: Content) -> some View {
        content.overlay {
            if let istek = ucus {
                MagazaUcusKatmani(istek: istek)
                    .id(istek.id)
            }
        }
    }
}

// MARK: - Süper (Pro) kartı

/// shopSuperCard: gradyan zemin, PRO rozeti, başlık, alt yazı, beyaz göçen düğme.
/// Sağda 140×140 dp'lik Lottie animasyonu (superAnim: trex_anim.json, sürekli döner).
/// Kartın tamamı ve düğme aynı eylemi çağırır (Android: openProPlans).
struct MagazaSuperKart: View {
    let dugmeMetni: String
    let eylem: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 0) {
                yazilar
                LottieGorunumu("trex_anim", dongu: true)
                    .frame(width: 140, height: 140)
                    .padding(.trailing, 10)
            }
            dugme
                .frame(height: 52)
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 20)
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(MagazaStil.superKartGradyani)
        )
        .contentShape(Rectangle())
        .onTapGesture(perform: eylem)
    }

    /// superButton: beyaz yüz, #C7CCD1 dudak, köşe 16, yükseklik 52 (dudak dahil).
    private var dugme: some View {
        Button(action: eylem) {
            Text(dugmeMetni)
                .font(Tema.yazi(14))
                .foregroundColor(.black)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(
            GocenDugmeStili(zemin: .white, golge: MagazaStil.beyazDugmeDudagi, koseYaricapi: 16)
        )
    }

    private var yazilar: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("fragment_shop_super_badge"))
                .font(Tema.yazi(12))
                .foregroundColor(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .fill(MagazaStil.superRozetGradyani)
                )
                .padding(.top, 20)
            // Android metni satır başlarında birer boşluk içerir ("Öğrenmeyi\n hızlandıran \n özellikler");
            // olduğu gibi alınır. Metin sola yaslıdır; genişliği animasyonun soluna kadardır.
            Text(Metin.al("fragment_shop_super_title"))
                .font(Tema.yazi(20))
                .foregroundColor(.white)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
            Text(Metin.al("fragment_shop_super_subtitle"))
                .font(Tema.yazi(13))
                .foregroundColor(MagazaStil.superAltYaziRengi)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.leading, 20)
        .padding(.trailing, 8)
    }
}

// MARK: - Reklam kartları (Özel Teklifler)

/// shopCrystalAdCard / shopLifeAdCard: görsel + başlık + açıklama, altında mavi eylem düğmesi.
struct MagazaReklamKarti: View {
    let gorselAdi: String
    let baslik: String
    let aciklama: String
    let dugmeMetni: String
    let dugmeOpakligi: Double
    let dugmeEtkin: Bool
    let ucus: MagazaUcusIstegi?
    let eylem: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 0) {
                MagazaGorsel(ad: gorselAdi, genislik: MagazaStil.buyukSimge, yukseklik: MagazaStil.buyukSimge)
                VStack(alignment: .leading, spacing: 0) {
                    Text(baslik)
                        .font(Tema.yazi(16))
                        .foregroundColor(.white)
                        .padding(.bottom, 6)
                    Text(aciklama)
                        .font(Tema.yazi(13))
                        .foregroundColor(MagazaStil.aciklamaRengi)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.leading, 14)
                Spacer(minLength: 0)
            }
            dugme
                .padding(.top, 12)
                .padding(.leading, 78)
        }
        .padding(MagazaStil.kartIcBosluk)
        .modifier(MagazaKartZemini())
    }

    private var dugme: some View {
        Button(action: eylem) {
            HStack(spacing: 6) {
                // Android: @android:drawable/ic_media_play (sistem simgesi), 12 dp, #29B6F6.
                Image(systemName: "play.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 12, height: 12)
                    .foregroundColor(MagazaStil.vurguMavi)
                Text(dugmeMetni)
                    .font(Tema.yazi(13))
                    .kerning(0.65)
                    .foregroundColor(MagazaStil.vurguMavi)
            }
            .padding(4)
            .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
        .opacity(dugmeOpakligi)
        .disabled(!dugmeEtkin)
        .modifier(MagazaUcusBindirmesi(ucus: ucus))
    }
}

// MARK: - Seri dondurma kartı

/// shopStreakFreezeCard: altınla alınan seri dondurma. Elde yoksa altın fiyatlı göçen düğme,
/// varsa düz "✓ HAZIR" rozeti (Android: renderStreakFreezeCard).
/// Seri ekranı iOS'ta henüz yok; kart yerinde durur, satın alma sahte servistedir.
struct MagazaSeriDondurmaKarti: View {
    let dondurmaVar: Bool
    let ucus: MagazaUcusIstegi?
    let eylem: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 0) {
                MagazaGorsel(ad: "streak_freeze_ic", genislik: MagazaStil.buyukSimge, yukseklik: MagazaStil.buyukSimge)
                VStack(alignment: .leading, spacing: 0) {
                    Text(Metin.al("fragment_shop_text_8"))
                        .font(Tema.yazi(16))
                        .foregroundColor(.white)
                        .padding(.bottom, 6)
                    Text(aciklama)
                        .font(Tema.yazi(13))
                        .foregroundColor(MagazaStil.aciklamaRengi)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.leading, 14)
                Spacer(minLength: 0)
            }
            dugme
                .padding(.top, 12)
                .padding(.leading, 78)
        }
        .padding(MagazaStil.kartIcBosluk)
        .modifier(MagazaKartZemini())
    }

    private var aciklama: String {
        if dondurmaVar {
            return Metin.al("ui_serin_korunuyor_kullaninca_yenisini_alabilir")
        }
        return Metin.al("fragment_shop_shop_streak_freeze_desc")
    }

    @ViewBuilder
    private var dugme: some View {
        if dondurmaVar {
            hazirRozeti
        } else {
            satinAlDugmesi
        }
    }

    /// shopBuyStreakFreezeButton (alınabilir): #37474F yüz, köşe 8, altın simgesi + 4000.
    private var satinAlDugmesi: some View {
        Button(action: eylem) {
            HStack(spacing: 0) {
                MagazaGorsel(ad: "gold_ic", genislik: 16, yukseklik: 16)
                    .padding(.trailing, 6)
                Text(String(MagazaStil.dondurmaBedeli))
                    .font(Tema.yazi(14))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .buttonStyle(
            GocenDugmeStili(
                zemin: MagazaStil.dondurmaDugmeZemini,
                golge: MagazaStil.koyulastir(0x37474F),
                koseYaricapi: 8
            )
        )
        .modifier(MagazaUcusBindirmesi(ucus: ucus))
    }

    /// Elde: düz (dudaksız) #17394B yüz, "✓  HAZIR" #4FC3F7; basılacak bir şey yok.
    private var hazirRozeti: some View {
        Button(action: eylem) {
            Text(Metin.al("ui_hazir"))
                .font(Tema.yazi(14))
                .foregroundColor(MagazaStil.dondurmaHazirYazi)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        }
        .buttonStyle(
            GocenDugmeStili(zemin: MagazaStil.dondurmaHazirZemini, golge: Color.clear, koseYaricapi: 8)
        )
        .disabled(true)
        .modifier(MagazaUcusBindirmesi(ucus: ucus))
    }
}

// MARK: - Can bölümü

/// shopProCard: Pro aboneliği (sınırsız can). Kartın tamamı dokunulabilir; Android'de plan
/// karşılaştırma ekranını açar. Kalp: Pro kalbi (heart_pro_ic) ve üzerinde 30 pt "∞".
struct MagazaProKarti: View {
    let fiyatMetni: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(alignment: .center, spacing: 0) {
                proKalbi
                VStack(alignment: .leading, spacing: 0) {
                    Text(Metin.al("fragment_shop_text_10"))
                        .font(Tema.yazi(16))
                        .foregroundColor(.white)
                        .padding(.bottom, 4)
                    Text(Metin.al("fragment_shop_text_11"))
                        .font(Tema.yazi(13))
                        .foregroundColor(MagazaStil.aciklamaRengi)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.bottom, 12)
                    Text(fiyatMetni)
                        .font(Tema.yazi(14))
                        .foregroundColor(MagazaStil.proFiyatRengi)
                }
                .padding(.leading, 14)
                Spacer(minLength: 0)
            }
            .padding(MagazaStil.kartIcBosluk)
            .modifier(MagazaKartZemini())
            .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
    }

    private var proKalbi: some View {
        ZStack {
            MagazaGorsel(ad: "heart_pro_ic", genislik: MagazaStil.buyukSimge, yukseklik: MagazaStil.buyukSimge)
            Text("∞")
                .font(Tema.yazi(30))
                .foregroundColor(.white)
        }
        .frame(width: MagazaStil.buyukSimge, height: MagazaStil.buyukSimge)
    }
}

/// shopExtraLifeCard: anahtar karşılığı 1 can; sağda bir sonraki cana kalan süre.
struct MagazaEkstraCanKarti: View {
    let bolum: MagazaCanBolumu
    let ucus: MagazaUcusIstegi?
    let eylem: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            MagazaGorsel(ad: "heart_ic", genislik: MagazaStil.buyukSimge, yukseklik: MagazaStil.buyukSimge)
            VStack(alignment: .leading, spacing: 0) {
                Text(Metin.al("fragment_shop_text_12"))
                    .font(Tema.yazi(16))
                    .foregroundColor(.white)
                    .padding(.bottom, 4)
                Text(Metin.al("fragment_shop_text_13"))
                    .font(Tema.yazi(13))
                    .foregroundColor(MagazaStil.aciklamaRengi)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.bottom, 12)
                HStack(alignment: .center, spacing: 0) {
                    dugme
                    Spacer(minLength: 8)
                    Text(bolum.sayacMetni)
                        .font(Tema.yazi(13))
                        .foregroundColor(bolum.sayacRengi)
                        .monospacedDigit()
                }
            }
            .padding(.leading, 14)
        }
        .padding(MagazaStil.kartIcBosluk)
        .modifier(MagazaKartZemini())
    }

    /// shopBuyLifeButton: göçen düğme; alınamıyorken düz (dudaksız) ve soluk.
    private var dugme: some View {
        Button(action: eylem) {
            HStack(spacing: 0) {
                MagazaGorsel(ad: "key", genislik: 16, yukseklik: 16)
                    .opacity(bolum.dugmeSimgeOpakligi)
                    .padding(.trailing, 6)
                Text(bolum.bedelMetni)
                    .font(Tema.yazi(14))
                    .foregroundColor(bolum.dugmeYaziRengi)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .buttonStyle(
            GocenDugmeStili(zemin: bolum.dugmeRengi, golge: bolum.dugmeDudagi, koseYaricapi: 8)
        )
        .disabled(!bolum.canAlinabilir)
        .modifier(MagazaUcusBindirmesi(ucus: ucus))
    }
}

// MARK: - Paket kartları

/// shopGoldCard1..3 / shopKeyCard1..3: görsel, miktar, fiyat. Kartın tamamı dokunulabilir.
struct MagazaPaketKartiGorunumu: View {
    let kart: MagazaPaketKarti
    let ucus: MagazaUcusIstegi?
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            VStack(spacing: 0) {
                MagazaGorsel(ad: kart.gorselAdi, genislik: MagazaStil.paketSimge, yukseklik: MagazaStil.paketSimge)
                HStack(spacing: 6) {
                    MagazaGorsel(ad: kart.birimSimgeAdi, genislik: 18, yukseklik: 18)
                    Text(kart.miktarMetni)
                        .font(Tema.yazi(16))
                        .foregroundColor(.white)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .padding(.top, 8)
                Text(kart.fiyatMetni)
                    .font(Tema.yazi(14))
                    .foregroundColor(MagazaStil.vurguMavi)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .padding(.top, 4)
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: MagazaStil.kartKosesi, style: .continuous)
                    .fill(MagazaStil.kartZemini)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
        .modifier(MagazaUcusBindirmesi(ucus: ucus))
        .accessibilityLabel(Text(kart.miktarMetni + ", " + kart.fiyatMetni))
    }
}

/// shopSobiCreditCard1..3: Sobi kafası (64), kredi adedi (15 pt), fiyat. Kartın tamamı dokunulabilir.
struct MagazaSobiKartiGorunumu: View {
    let paket: MagazaSobiPaketi
    let ucus: MagazaUcusIstegi?
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            VStack(spacing: 0) {
                MagazaSobiKafasi(boyut: 64)
                    .padding(.top, 4)
                Text(paket.miktarMetni)
                    .font(Tema.yazi(15))
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .padding(.top, 8)
                Text(paket.fiyatMetni)
                    .font(Tema.yazi(14))
                    .foregroundColor(MagazaStil.vurguMavi)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .padding(.top, 4)
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: MagazaStil.kartKosesi, style: .continuous)
                    .fill(MagazaStil.kartZemini)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
        .modifier(MagazaUcusBindirmesi(ucus: ucus))
        .accessibilityLabel(Text(Metin.al("shop_sobi_credit_desc") + ": " + paket.miktarMetni + ", " + paket.fiyatMetni))
    }
}
