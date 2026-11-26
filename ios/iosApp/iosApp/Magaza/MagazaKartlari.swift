import SwiftUI

// Mağaza ekranının kartları. Ölçü, renk ve metinler res/layout/fragment_shop.xml'den
// birebir alınmıştır (dp → pt, sp → pt). Metinler Android'de strings.xml'de değil, layout
// dosyasında sabit yazılıdır; burada da Türkçe sabit olarak durur.

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

/// shopSuperCard: gradyan zemin, PRO rozeti, başlık, alt yazı, beyaz düğme.
/// Android'de sağda 140×140 dp'lik Lottie animasyonu (trex_anim.json) oynar; Lottie henüz
/// bağlı olmadığı için yeri boş bırakılmıştır.
struct MagazaSuperKart: View {
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            VStack(spacing: 0) {
                HStack(alignment: .center, spacing: 0) {
                    yazilar
                    Spacer(minLength: 0)
                    Color.clear
                        .frame(width: 140, height: 140)
                        .padding(.trailing, 10)
                }
                Text("1 HAFTA ÜCRETSİZ DENE")
                    .font(Tema.yazi(14))
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Color.white))
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
        }
        .buttonStyle(MagazaBasmaStili())
    }

    private var yazilar: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("PRO")
                .font(Tema.yazi(12))
                .foregroundColor(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .fill(MagazaStil.superRozetGradyani)
                )
                .padding(.top, 20)
            Text("Öğrenmeyi\n hızlandıran \n özellikler")
                .font(Tema.yazi(20))
                .foregroundColor(.white)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
            Text("Sınırsız enerji ve reklamsız\n deneyimin")
                .font(Tema.yazi(13))
                .foregroundColor(MagazaStil.superAltYaziRengi)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
        }
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

// MARK: - Can bölümü

/// shopProCard: Pro aboneliği (sınırsız can). Kartın tamamı dokunulabilir.
struct MagazaProKarti: View {
    let fiyatMetni: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(alignment: .center, spacing: 0) {
                MagazaGorsel(ad: "heart_ic", genislik: MagazaStil.buyukSimge, yukseklik: MagazaStil.buyukSimge)
                VStack(alignment: .leading, spacing: 0) {
                    Text("Sınırsız")
                        .font(Tema.yazi(16))
                        .foregroundColor(.white)
                        .padding(.bottom, 4)
                    Text("Pro ile canın hiç tükenmesin")
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
                Text("Ekstra Can")
                    .font(Tema.yazi(16))
                    .foregroundColor(.white)
                    .padding(.bottom, 4)
                Text("Daha fazla dersin üstesinden gelmek için 1 can al.")
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

    private var dugme: some View {
        Button(action: eylem) {
            HStack(spacing: 6) {
                MagazaGorsel(ad: "key", genislik: 16, yukseklik: 16)
                    .opacity(bolum.dugmeSimgeOpakligi)
                Text(bolum.bedelMetni)
                    .font(Tema.yazi(14))
                    .foregroundColor(bolum.dugmeYaziRengi)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(bolum.dugmeRengi))
            .contentShape(Rectangle())
        }
        .buttonStyle(MagazaBasmaStili())
        .disabled(!bolum.canAlinabilir)
        .modifier(MagazaUcusBindirmesi(ucus: ucus))
    }
}

// MARK: - Paket kartı

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
