import Shared
import SwiftUI

// Plan karşılaştırma ekranının 2. sayfası: Pro / Lite kartları ve satın alma düğmesi.
//
// Android karşılığı: PlanFragment.kt + res/layout/fragment_plan.xml (beyaz zemin, kapat düğmesi 48dp,
// iki kart: bg_plan_bireysel seçili / bg_plan_lite seçili değil, kart başına Lottie simgesi, alta yapışık
// "otomatik yenilenir" yazısı ve 56dp'lik mavi düğme). Kartta seçili olanın sağ üst köşesinde tikli rozet durur.
//
// Android'den farklar:
//  - "Ya da arkadaşını davet et" bağlantısı yok (davet ekranı iOS'a alınmadı).
//  - Yıllık karşılık satırı ("₺1.188/yıl") yok: iOS ödeme servisi yalnızca biçimlenmiş fiyat metni verir,
//    tutar ve para birimi ayrı gelmediği için çarpım yapılamaz.
//  - Fiyat mağazadan gelene kadar boş kalır (Android layout'taki "Aylık ₺99" yer tutucusunu gösterir).
//  - Lite kartı yalnızca mağaza Lite ürününü döndürürse çıkar.
//  - iOS'a özgü (App Store şartı): yenileme açıklaması (fiyat, dönem, iptal), Kullanım Şartları ve Gizlilik
//    Politikası bağlantıları, "Satın alımları geri yükle".
//  - Üstte telefonun durum çubuğu alanı koyu uygulama rengindedir: durum çubuğu simgeleri hep açık renklidir
//    (Android beyaz zeminde koyu simge kullanır).

private struct PlanKartVerisi {
    let plan: String
    let baslik: String
    let ozellikler: [String]
    let animasyon: String
    let animasyonBoyu: CGFloat
    let animasyonSagBosluk: CGFloat
}

struct PlanSecimSayfasi: View {
    @ObservedObject var model: PlanModeli
    let kapat: () -> Void

    var body: some View {
        GeometryReader { alan in
            ZStack(alignment: .top) {
                Color.white
                    .ignoresSafeArea()
                VStack(spacing: 0) {
                    kapatSatiri
                    kartlar
                    PlanAltCubuk(model: model, kapat: kapat)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                // Durum çubuğu alanı: beyaz zeminde açık renkli simgeler okunmaz, bu yüzden koyu şerit.
                Color("background_color")
                    .frame(height: alan.safeAreaInsets.top)
                    .offset(y: -alan.safeAreaInsets.top)
            }
            .frame(width: alan.size.width, height: alan.size.height)
        }
    }

    /// btnPlanClose: 48dp, sol 4 / üst 4, ic_close_black (12dp iç boşluk, #666666).
    private var kapatSatiri: some View {
        HStack(spacing: 0) {
            Button(action: kapat) {
                Image("ic_close_black")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .padding(8)
                    .frame(width: 48, height: 48)
                    .foregroundColor(PlanStil.ikincilYazi)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Metin.al("plan_close_desc"))
            Spacer(minLength: 0)
        }
        .padding(.leading, 4)
        .padding(.top, 4)
    }

    // MARK: Kartlar

    private var proKarti: PlanKartVerisi {
        var satirlar: [String] = [
            Metin.al("fragment_plan_text"),
            Metin.al("fragment_plan_text_2"),
            Metin.al("fragment_plan_text_3"),
            Metin.al("fragment_plan_text_4"),
            Metin.al("fragment_plan_text_8"),
        ]
        // Hoş geldin kredisi cihaz başına bir kez verilir; tükettiyse satır gizlenir.
        if model.hosgeldinSatiriGorunur {
            satirlar.append(Metin.al("fragment_plan_text_5"))
        }
        return PlanKartVerisi(
            plan: "Pro",
            baslik: Metin.al("fragment_plan_tv_bireysel_title"),
            ozellikler: satirlar,
            animasyon: "trex_anim",
            animasyonBoyu: 80,
            animasyonSagBosluk: 8
        )
    }

    private var liteKarti: PlanKartVerisi {
        let satirlar: [String] = [
            Metin.al("fragment_plan_text_6"),
            Metin.al("fragment_plan_text_7"),
            Metin.al("fragment_plan_text_9"),
        ]
        return PlanKartVerisi(
            plan: "Lite",
            baslik: Metin.al("fragment_plan_tv_lite_title"),
            ozellikler: satirlar,
            animasyon: "tiger_anim",
            animasyonBoyu: 70,
            animasyonSagBosluk: 16
        )
    }

    private var kartlar: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 16) {
                PlanKarti(
                    veri: proKarti,
                    secili: model.seciliPlan == "Pro",
                    fiyat: model.fiyatYazisi(plan: "Pro"),
                    dokun: { model.planSec("Pro") }
                )
                if model.liteVar {
                    PlanKarti(
                        veri: liteKarti,
                        secili: model.seciliPlan == "Lite",
                        fiyat: model.fiyatYazisi(plan: "Lite"),
                        dokun: { model.planSec("Lite") }
                    )
                }
                Text(model.yenilemeAciklamasi)
                    .font(Tema.yazi(12, kalin: false))
                    .foregroundColor(PlanStil.ikincilYazi)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 4)
                    .padding(.bottom, 8)
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 12)
        }
    }
}

// MARK: - Kart

private struct PlanKarti: View {
    let veri: PlanKartVerisi
    let secili: Bool
    let fiyat: String
    let dokun: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            baslikSatiri
            ozellikSatiri
        }
        .padding(16)
        .background(zemin)
        .overlay(alignment: .topTrailing) {
            if secili {
                secimRozeti
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            dokun()
        }
    }

    /// tvBireyselTitle (18sp) ve tvBireyselPriceRight (14sp), ikisi de #333333.
    private var baslikSatiri: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(veri.baslik)
                .font(Tema.yazi(18))
                .foregroundColor(PlanStil.koyuYazi)
            Spacer(minLength: 8)
            Text(fiyat)
                .font(Tema.yazi(14))
                .foregroundColor(PlanStil.koyuYazi)
        }
    }

    /// Özellik listesi (üstten 16dp) ve sağda Lottie simgesi (listeyle dikey ortalı).
    private var ozellikSatiri: some View {
        HStack(alignment: .center, spacing: 0) {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(veri.ozellikler.enumerated()), id: \.offset) { sira in
                    PlanKartOzelligi(metin: sira.element)
                }
            }
            Spacer(minLength: 8)
            LottieGorunumu(veri.animasyon, dongu: true)
                .frame(width: veri.animasyonBoyu, height: veri.animasyonBoyu)
                .padding(.trailing, veri.animasyonSagBosluk)
        }
        .padding(.top, 16)
    }

    /// Seçili kart: 2dp degrade çerçeve (bg_plan_bireysel); değilse 2dp #E0E0E0 çizgi (bg_plan_lite). Köşe 16dp.
    @ViewBuilder
    private var zemin: some View {
        if secili {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(PlanStil.kartCercevesi)
                RoundedRectangle(cornerRadius: 14)
                    .fill(Color.white)
                    .padding(2)
            }
        } else {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .strokeBorder(PlanStil.renk(0xE0E0E0), lineWidth: 2)
                )
        }
    }

    /// imgBadgeBireysel: 28dp, #00E676, köşe 8dp, iç boşluk 6dp; kartın sağ üst köşesinden 10dp taşar.
    private var secimRozeti: some View {
        Image("correct_ic2")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundColor(Color.white)
            .padding(6)
            .frame(width: 28, height: 28)
            .background(RoundedRectangle(cornerRadius: 8).fill(PlanStil.secimRozeti))
            .offset(x: 10, y: -10)
    }
}

/// Kart içindeki bir özellik satırı: 16dp tik (#00B0FF, üstten 2dp) ve 14sp #666666 yazı.
private struct PlanKartOzelligi: View {
    let metin: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image("correct_ic2")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 16, height: 16)
                .foregroundColor(PlanStil.kartTiki)
                .padding(.top, 2)
            Text(metin)
                .font(Tema.yazi(14))
                .foregroundColor(PlanStil.ikincilYazi)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

// MARK: - Alt çubuk

/// Alta yapışık bölüm: "otomatik yenilenir" yazısı, satın alma düğmesi ve yasal bağlantılar.
private struct PlanAltCubuk: View {
    @ObservedObject var model: PlanModeli
    let kapat: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Android'deki kısa "otomatik yenilenir" cümlesi (fragment_plan_tv_footer_text2) kaydırılan
            // yenileme açıklamasıyla aynı şeyi söylediği için burada tekrarlanmaz.
            dugme
            PlanYasalSatiri(model: model)
                .padding(.top, 6)
        }
        .padding(.top, 8)
        .padding(.bottom, 6)
        .frame(maxWidth: .infinity)
        .background(Color.white)
    }

    /// btnTryFree: 56dp (dudak dahil), yanlarda 16dp, köşe 12dp, #3D4DFF.
    private var dugme: some View {
        Button(action: { model.satinAl(bitti: kapat) }) {
            Text(model.ctaMetni)
                .font(Tema.yazi(16))
                .foregroundColor(Color.white)
        }
        .buttonStyle(
            GocenDugmeStili(zemin: PlanStil.dugmeYuzu, golge: PlanStil.dugmeDudagi, koseYaricapi: 12)
        )
        .frame(height: 56)
        .padding(.horizontal, 16)
    }
}

/// Kullanım Şartları · Gizlilik Politikası · Satın alımları geri yükle (iOS'a özgü; App Store şartı).
/// Bağlantı adresleri ortak koddan (LoginFlowTexts.termsUrl / privacyUrl), sayfa uygulamanın dilinde.
private struct PlanYasalSatiri: View {
    @ObservedObject var model: PlanModeli

    var body: some View {
        HStack(spacing: 14) {
            baglanti(PlanMetin.kullanimSartlari, LoginFlowTexts.shared.termsUrl(langCode: Metin.dil))
            baglanti(PlanMetin.gizlilikPolitikasi, LoginFlowTexts.shared.privacyUrl(langCode: Metin.dil))
            Button(action: { model.geriYukle() }) {
                etiket(PlanMetin.geriYukle)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private func baglanti(_ metin: String, _ adres: String) -> some View {
        if let url = URL(string: adres) {
            Link(destination: url) {
                etiket(metin)
            }
        }
    }

    private func etiket(_ metin: String) -> some View {
        Text(metin)
            .font(Tema.yazi(12))
            .foregroundColor(PlanStil.dugmeYuzu)
            .underline()
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .padding(.vertical, 6)
    }
}
