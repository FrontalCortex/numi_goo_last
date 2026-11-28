import SwiftUI

// Plan karşılaştırma / Pro satın alma ekranı.
//
// Android karşılığı: ProDiffirentFragment.kt + res/layout/fragment_pro_diffirent.xml (lacivert zeminde
// Ücretsiz / Pro karşılaştırma tablosu) ve onun "N HAFTA ÜCRETSİZ DENE" düğmesiyle açtığı PlanFragment.kt +
// res/layout/fragment_plan.xml (Pro / Lite kartları ve satın alma). iOS'ta ikisi tek ekranın iki sayfasıdır:
// ilk sayfa tablo, düğmeye basınca sağdan ikinci sayfa (kartlar) gelir. Mağazadaki Süper kart ve Pro kartı
// (Android: ShopFragment.shopSuperCard / shopProCard) bu ekranı açar.
//
// iOS'a özgü (App Store şartı): ikinci sayfada fiyat ve dönem, otomatik yenileme açıklaması, Kullanım Şartları
// ve Gizlilik Politikası bağlantıları ve "Satın alımları geri yükle"; zaten Pro olan kullanıcıda Pro etiketi
// ve "Aboneliği yönet" bağlantısı. Kullanıcı zaten Pro ise ikinci sayfaya geçilmez.

struct PlanKarsilastirmaEkrani: View {
    @StateObject private var model: PlanModeli
    private let kapat: () -> Void

    /// - Parameters:
    ///   - servisler: ödeme, cüzdan ve diğer servisler; verilmezse sahte servisler (Pro alınca plan "Pro" olur).
    ///   - hosgeldinKredisi: hoş geldin kredisi satırının görünürlüğü (Android: WelcomeCreditEligibility).
    ///   - baslangicSayfasi: açılış sayfası (Android'de her zaman karşılaştırma).
    ///   - kapat: kapatma düğmesi ya da başarılı satın alma.
    init(
        servisler: MagazaBagimliliklar = MagazaBagimliliklar.sahte,
        hosgeldinKredisi: PlanHosgeldinKredisiKaynagi = SahtePlanHosgeldinKredisi(),
        baslangicSayfasi: PlanSayfasi = PlanSayfasi.karsilastirma,
        kapat: @escaping () -> Void
    ) {
        self.kapat = kapat
        _model = StateObject(
            wrappedValue: PlanModeli(
                servisler: servisler,
                hosgeldin: hosgeldinKredisi,
                baslangicSayfasi: baslangicSayfasi
            )
        )
    }

    var body: some View {
        ZStack {
            sayfa
            PlanIslemKatmani(model: model)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .animation(.easeInOut(duration: 0.3), value: model.sayfa)
        .onAppear {
            model.basla()
        }
        .onDisappear {
            model.dur()
        }
    }

    /// Android: DialogAnimationSlideRight — ikinci sayfa sağdan girer, ilk sayfa sola çekilir.
    @ViewBuilder
    private var sayfa: some View {
        if model.sayfa == .karsilastirma {
            PlanKarsilastirmaSayfasi(model: model, kapat: kapat)
                .transition(AnyTransition.move(edge: .leading))
        } else {
            PlanSecimSayfasi(model: model, kapat: kapat)
                .transition(AnyTransition.move(edge: .trailing))
        }
    }
}

// MARK: - İşlem göstergesi ve bildirim

/// Satın alma / geri yükleme sürerken tam ekran bekleme göstergesi, sonuçta alt bildirim.
private struct PlanIslemKatmani: View {
    @ObservedObject var model: PlanModeli

    var body: some View {
        ZStack {
            if model.islemSuruyor {
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
                    .scaleEffect(1.4)
            }
            if let metin = model.bildirim {
                VStack(spacing: 0) {
                    Spacer(minLength: 0)
                    Text(metin)
                        .font(Tema.yazi(14, kalin: false))
                        .foregroundColor(Color.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Capsule().fill(PlanStil.renk(0x323232)))
                        .padding(.horizontal, 24)
                        .padding(.bottom, 48)
                }
                .allowsHitTesting(false)
                .transition(AnyTransition.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
    }
}

// MARK: - 1. sayfa: karşılaştırma tablosu (fragment_pro_diffirent.xml)

/// Tablonun bir hücresi.
enum PlanHucre {
    /// correct_ic2 (16dp).
    case tik
    /// "—"
    case tire
    /// "2/gün" gibi kısa yazı (13sp).
    case yazi(String)
}

struct PlanKarsilastirmaSatirVerisi: Identifiable {
    let id: Int
    let metin: String
    let ucretsiz: PlanHucre
    let pro: PlanHucre
}

/// Ölçüler: sütun zemini 80dp (bg_super_column), kenar boşluğu 24dp, "ÜCRETSİZ" başlığı ile sütun arası 24dp.
private enum PlanTabloOlculeri {
    static let proSutunGenisligi: CGFloat = 80
    static let kenarBoslugu: CGFloat = 24
    static let sutunAraligi: CGFloat = 24
    /// "ÜCRETSİZ" başlığının (14sp) yaklaşık genişliği; satırlardaki Ücretsiz hücreleri de bu genişlikte ortalanır.
    static let ucretsizSutunGenisligi: CGFloat = 62
}

struct PlanKarsilastirmaSayfasi: View {
    @ObservedObject var model: PlanModeli
    let kapat: () -> Void

    var body: some View {
        ZStack {
            Color("paywall_navy")
                .ignoresSafeArea()
            VStack(spacing: 0) {
                ustSatir
                baslik
                maskot
                PlanKarsilastirmaAlti(model: model)
            }
        }
    }

    /// btnClose (28dp, beyaz, sol 24 / üst 24) ve superBadge (sağ 24 / üst 24).
    private var ustSatir: some View {
        HStack(alignment: .top, spacing: 0) {
            Button(action: kapat) {
                Image(systemName: "xmark")
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(Color.white)
                    .padding(5)
                    .frame(width: 28, height: 28)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Metin.al("plan_close_desc"))
            Spacer(minLength: 0)
            PlanProRozeti(boyut: 14, yatayBosluk: 12, dikeyBosluk: 4)
        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
    }

    /// tvTitle: 22sp beyaz, ortalı, kapat düğmesinin 32dp altında, yanlarda 24dp.
    private var baslik: some View {
        Text(Metin.al("ui_pro_ile_daha_hizli_ogren_daha"))
            .font(Tema.yazi(22))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 24)
            .padding(.top, 32)
    }

    /// centerMascot: taçlı Sobi (CROWN), genişlik %75, başlık ile beyaz bölüm arasındaki boşluğa sığar
    /// (üstten 8dp, alttan 24dp).
    private var maskot: some View {
        GeometryReader { alan in
            SobiMaskotGorunumu(emote: "CROWN")
                .frame(width: alan.size.width * 0.75, height: alan.size.height)
                .frame(width: alan.size.width, height: alan.size.height)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }
}

/// bottomWhiteScroll: üst köşeleri 24dp yuvarlak beyaz bölüm — başlıklar, satırlar ve düğme.
private struct PlanKarsilastirmaAlti: View {
    @ObservedObject var model: PlanModeli

    var body: some View {
        VStack(spacing: 0) {
            tablo
            dugmeAlani
        }
        .padding(.top, 32)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity)
        .background(
            PlanUstYuvarlakSekil(yaricap: 24)
                .fill(Color.white)
                .ignoresSafeArea(edges: .bottom)
        )
    }

    // MARK: Tablo

    private var tablo: some View {
        VStack(spacing: 0) {
            baslikSatiri
            ForEach(satirlar) { satir in
                PlanKarsilastirmaSatiri(satir: satir)
                    .padding(.top, satir.id == 1 ? 24 : 20)
            }
        }
        .padding(.bottom, 16)
        .background(alignment: .topTrailing) {
            // superColumnBg: başlıktan düğmenin 16dp üstüne kadar uzanan açık mor sütun.
            RoundedRectangle(cornerRadius: 16)
                .fill(PlanStil.proSutunu)
                .frame(width: PlanTabloOlculeri.proSutunGenisligi)
                .padding(.trailing, PlanTabloOlculeri.kenarBoslugu)
        }
    }

    /// tvHeaderFree ("ÜCRETSİZ", 14sp #4A4A4A) ve tvHeaderSuper ("PRO" rozeti, 12sp).
    private var baslikSatiri: some View {
        HStack(spacing: 0) {
            Spacer(minLength: 0)
            Text(Metin.al("fragment_pro_diffirent_tv_header_free"))
                .font(Tema.yazi(14))
                .foregroundColor(PlanStil.ucretsizYazi)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(width: PlanTabloOlculeri.ucretsizSutunGenisligi)
            Spacer()
                .frame(width: PlanTabloOlculeri.sutunAraligi)
            PlanProRozeti(boyut: 12, yatayBosluk: 8, dikeyBosluk: 2)
                .frame(width: PlanTabloOlculeri.proSutunGenisligi)
            Spacer()
                .frame(width: PlanTabloOlculeri.kenarBoslugu)
        }
    }

    /// Android satır sırası: 1, 2, 3, 4, 5, 7 (Sobi testi), 6 (hoş geldin; gizlenebildiği için sonda).
    private var satirlar: [PlanKarsilastirmaSatirVerisi] {
        var liste: [PlanKarsilastirmaSatirVerisi] = []
        liste.append(PlanKarsilastirmaSatirVerisi(
            id: 1, metin: Metin.al("fragment_pro_diffirent_row1_text"), ucretsiz: .tik, pro: .tik))
        liste.append(PlanKarsilastirmaSatirVerisi(
            id: 2, metin: Metin.al("fragment_pro_diffirent_row2_text"), ucretsiz: .tire, pro: .tik))
        liste.append(PlanKarsilastirmaSatirVerisi(
            id: 3, metin: Metin.al("fragment_pro_diffirent_row3_text"), ucretsiz: .tire, pro: .tik))
        liste.append(PlanKarsilastirmaSatirVerisi(
            id: 4, metin: Metin.al("fragment_pro_diffirent_row4_text"), ucretsiz: .tire, pro: .tik))
        // Sobi mesajı her iki planda var, fark miktarda: hücrelerde ✓/— yerine günlük hak.
        liste.append(PlanKarsilastirmaSatirVerisi(
            id: 5,
            metin: Metin.al("fragment_pro_diffirent_row5_text"),
            ucretsiz: .yazi(Metin.al("fragment_pro_diffirent_row5_free")),
            pro: .yazi(Metin.al("fragment_pro_diffirent_row5_pro"))
        ))
        // Sobi testi: ücretsizde günün ilk testi bedava, Pro'da sınırsız.
        liste.append(PlanKarsilastirmaSatirVerisi(
            id: 7,
            metin: Metin.al("fragment_pro_diffirent_row7_text"),
            ucretsiz: .yazi(Metin.al("fragment_pro_diffirent_row7_free")),
            pro: .tik
        ))
        if model.hosgeldinSatiriGorunur {
            liste.append(PlanKarsilastirmaSatirVerisi(
                id: 6, metin: Metin.al("fragment_pro_diffirent_row6_text"), ucretsiz: .tire, pro: .tik))
        }
        return liste
    }

    // MARK: Düğme

    /// btnTryFree: 56dp (dudak dahil), yanlarda 24dp, son satırın 32dp altında (16dp'si tablonun alt boşluğu), köşe 12dp. Zaten Pro olan kullanıcıda
    /// düğme yerine etiket gösterilir (iOS'a özgü).
    @ViewBuilder
    private var dugmeAlani: some View {
        if model.proMu {
            proAktifAlani
        } else {
            Button(action: { model.planlaraGec() }) {
                Text(model.ctaMetni)
                    .font(Tema.yazi(16))
                    .foregroundColor(Color.white)
            }
            .buttonStyle(
                GocenDugmeStili(zemin: PlanStil.dugmeYuzu, golge: PlanStil.dugmeDudagi, koseYaricapi: 12)
            )
            .frame(height: 56)
            .padding(.horizontal, 24)
            .padding(.top, 16)
        }
    }

    private var proAktifAlani: some View {
        VStack(spacing: 10) {
            Text(PlanMetin.proAktif)
                .font(Tema.yazi(16))
                .foregroundColor(PlanStil.dugmeYuzu)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(RoundedRectangle(cornerRadius: 12).fill(PlanStil.proSutunu))
            if let adres = URL(string: PlanMetin.abonelikYonetimAdresi) {
                Link(destination: adres) {
                    Text(PlanMetin.aboneligiYonet)
                        .font(Tema.yazi(13))
                        .foregroundColor(PlanStil.ikincilYazi)
                        .padding(.vertical, 4)
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 16)
    }
}

/// Tablonun bir satırı: soldaki açıklama, Ücretsiz hücresi, Pro hücresi (row1Text … row7Text).
private struct PlanKarsilastirmaSatiri: View {
    let satir: PlanKarsilastirmaSatirVerisi

    var body: some View {
        HStack(spacing: 0) {
            Text(satir.metin)
                .font(Tema.yazi(14))
                .foregroundColor(PlanStil.ikincilYazi)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 32)
                .padding(.trailing, 16)
            PlanHucreGorunumu(hucre: satir.ucretsiz, ucretsizSutun: true)
                .frame(width: PlanTabloOlculeri.ucretsizSutunGenisligi)
            Spacer()
                .frame(width: PlanTabloOlculeri.sutunAraligi)
            PlanHucreGorunumu(hucre: satir.pro, ucretsizSutun: false)
                .frame(width: PlanTabloOlculeri.proSutunGenisligi)
            Spacer()
                .frame(width: PlanTabloOlculeri.kenarBoslugu)
        }
    }
}

private struct PlanHucreGorunumu: View {
    let hucre: PlanHucre
    let ucretsizSutun: Bool

    var body: some View {
        switch hucre {
        case .tik:
            Image("correct_ic2")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 16, height: 16)
                .foregroundColor(ucretsizSutun ? PlanStil.ucretsizYazi : Color.black)
        case .tire:
            Text("—")
                .font(Tema.yazi(14))
                .foregroundColor(PlanStil.renk(0x999999))
        case .yazi(let metin):
            Text(metin)
                .font(Tema.yazi(13))
                .foregroundColor(ucretsizSutun ? PlanStil.ucretsizYazi : Color.black)
        }
    }
}
