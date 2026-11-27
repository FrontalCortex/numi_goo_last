import SwiftUI

// Abaküs dersi ekranı.
//
// Android karşılığı: AbacusFragment + res/layout/fragment_abacus.xml. Ölçüler dimens.xml'deki
// abacus_fragment_* ve tutorial_* değerleridir (dp → pt birebir).
//
// Kapsam dışı bırakılan Android parçaları (ayrı işler): rehber paneli (panelContent, btnBack,
// btnForward), kurallar kitabı ve kural tabloları (rulesBookButton, rulesPanelButton,
// rulesPanelScrollView), öğretmene sor düğmesi, abaküs ayar paneli (abacusModeButton,
// abacusSettingsPanel), ders sonu anketi (QuestionPanelFragment).
//
// Kısa ekran: Android yerleşimi sabit dp kullanır (tahta 300, abaküs 290, düğme 72) ve ekran
// yüksekliğine göre ölçeklemez; kısa ekranda abaküs tahtanın üstüne biner (kullanıcı abaküsü ayar
// panelindeki kaydırıcılarla elle küçültür). iOS'ta bunun yerine tahta ile abaküs, mevcut
// yüksekliğe sığacak ortak bir oranla küçültülür (bkz. AbakusDersOlcegi); yer yetiyorsa oran 1'dir
// ve yerleşim Android ile aynıdır.

struct AbakusDersEkrani: View {
    @StateObject private var model: AbakusDersModeli
    private let kapat: () -> Void
    private let stil: AbakusStili

    /// - Parameters:
    ///   - dersBaslangici: hangi ders, hangi sorular.
    ///   - hizmetler: ses, titreşim, çevrimiçi durumu, ilerleme / enerji / süre kayıtları.
    ///   - stil: abaküsün görünümü (kullanıcının mağazadan seçtiği boncuk ve çerçeve).
    ///   - bitti: bütün sorular bitince bir kez çağrılır; sonuç ekranını yönetici oturum açar.
    ///   - kapat: kapatma düğmesine basılınca (kayma animasyonundan sonra) çağrılır.
    init(
        dersBaslangici: AbakusDersBaslangici,
        hizmetler: AbakusDersHizmetleri = SahteAbakusDersHizmetleri(),
        stil: AbakusStili = .varsayilan,
        bitti: @escaping (AbakusDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        _model = StateObject(
            wrappedValue: AbakusDersModeli(baslangic: dersBaslangici, hizmetler: hizmetler, bitti: bitti)
        )
        self.kapat = kapat
        self.stil = stil
    }

    var body: some View {
        GeometryReader { alan in
            ZStack {
                Color("background_color").ignoresSafeArea()
                AbakusDersIcerigi(model: model, stil: stil, kapat: kapat)
                    // abacus_fragment_root_padding
                    .padding(10)
                AbakusDersOrtusu(model: model)
                AbakusKonfetiKatmani(goster: model.konfeti)
                AbakusSonucPanelKatmani(model: model)
                AbakusBildirimKatmani(metin: model.bildirim)
            }
            // closeFragment: ekran genişliği kadar sağa kayar.
            .offset(x: model.kapaniyor ? alan.size.width : 0)
        }
        .onAppear { model.gorundu() }
        .onDisappear { model.kayboldu() }
    }
}

// MARK: - Ölçek

/// Kısa ekranlarda tahta ile abaküsün birlikte küçültülme oranı.
enum AbakusDersOlcegi {
    /// blackboardImage: layout_marginTop
    static let tahtaUstBosluk: CGFloat = 20
    /// blackboardImage: layout_height
    static let tahtaYuksekligi: CGFloat = 300
    /// abacusContainer: layout_marginBottom (tutorial_abacus_linear_margin_bottom)
    static let abakusAltBosluk: CGFloat = 10
    /// kontrolButton: 56 + tutorial_bottom_panel_margin_bottom 16
    static let dugmeAlani: CGFloat = 72
    /// Tahta ile abaküs arasında bırakılan en az boşluk (süre yazısı buraya düşer).
    static let enAzAra: CGFloat = 20
    static let enKucuk: CGFloat = 0.5

    /// - Parameters:
    ///   - yukseklik: kök dolgunun (10 pt) içinde kalan alanın yüksekliği.
    ///   - sabit: ölçeklenmeyen parçaların (boşluklar, düğme) toplam yüksekliği.
    static func hesapla(yukseklik: CGFloat, sabit: CGFloat) -> CGFloat {
        let abakusBoyu: CGFloat = AbakusGorunumu.boyut(basamakSayisi: 5).height
        let olceklenen: CGFloat = tahtaYuksekligi + abakusBoyu
        let kalan: CGFloat = yukseklik - sabit
        if kalan >= olceklenen {
            return 1
        }
        let oran: CGFloat = kalan / olceklenen
        return min(max(oran, enKucuk), 1)
    }

    /// Abaküs dersi ekranı için oran.
    static func dersIcin(yukseklik: CGFloat) -> CGFloat {
        let sabit: CGFloat = tahtaUstBosluk + enAzAra + abakusAltBosluk + dugmeAlani
        return hesapla(yukseklik: yukseklik, sabit: sabit)
    }
}

// MARK: - İçerik

/// Kök ConstraintLayout'un içindekiler: üstte ilerleme ve tahta, altta abaküs ve düğme.
private struct AbakusDersIcerigi: View {
    @ObservedObject var model: AbakusDersModeli
    let stil: AbakusStili
    let kapat: () -> Void

    var body: some View {
        GeometryReader { alan in
            katmanlar(boyut: alan.size)
        }
    }

    private func katmanlar(boyut: CGSize) -> some View {
        let olcek: CGFloat = AbakusDersOlcegi.dersIcin(yukseklik: boyut.height)
        return ZStack(alignment: .top) {
            AbakusDersUstBolumu(model: model, kapat: kapat, olcek: olcek)
            AbakusDersAltBolumu(model: model, stil: stil, olcek: olcek)
            // hintContainer XML'de abaküsten sonra gelir: abaküsün üstüne çizilir.
            AbakusDersIpucuKatmani(model: model, olcek: olcek)
        }
        .frame(width: boyut.width, height: boyut.height, alignment: .top)
    }
}

/// Tahta, soru, ilerleme çubuğu, kapatma düğmesi ve süre.
private struct AbakusDersUstBolumu: View {
    @ObservedObject var model: AbakusDersModeli
    let kapat: () -> Void
    let olcek: CGFloat

    var body: some View {
        ZStack(alignment: .top) {
            tahtaVeSure
            AbakusIlerlemeCubugu(oran: model.ilerleme, renk: model.ilerlemeRengi)
                // abacus_fragment_progress_margin
                .padding(.top, 16)
            HStack {
                Button(action: { model.kapatDokunuldu(kapat) }) {
                    Image("cross")
                        .resizable()
                        .scaledToFit()
                        // abacus_fragment_quit_size
                        .frame(width: 48, height: 48)
                }
                .buttonStyle(.plain)
                Spacer(minLength: 0)
            }
        }
    }

    private var tahtaVeSure: some View {
        VStack(spacing: 0) {
            AbakusKaraTahta(
                ilkSayi: model.ilkSayi, islec: model.islec, ikinciSayi: model.ikinciSayi,
                olcek: olcek
            )
            // blackboardImage: layout_marginTop
            .padding(.top, AbakusDersOlcegi.tahtaUstBosluk)
            if model.sureGorunur {
                Text(model.sureMetni)
                    .font(Tema.yazi(18))
                    .foregroundColor(Color("yellow"))
                    // abacus_fragment_timer_margin_top
                    .padding(.top, -10)
            }
            Spacer(minLength: 0)
        }
    }
}

/// hintContainer: tahtanın altına hizalı (üst boşluk + tahta yüksekliği).
private struct AbakusDersIpucuKatmani: View {
    @ObservedObject var model: AbakusDersModeli
    let olcek: CGFloat

    var body: some View {
        let tahtaBoyu: CGFloat = AbakusDersOlcegi.tahtaYuksekligi * olcek
        let ust: CGFloat = AbakusDersOlcegi.tahtaUstBosluk + tahtaBoyu
        return VStack(spacing: 0) {
            AbakusIpucuBolumu(model: model)
                .padding(.top, ust)
            Spacer(minLength: 0)
        }
    }
}

/// Abaküs ve altındaki "Kontrol Et" / "Başla" düğmesi.
private struct AbakusDersAltBolumu: View {
    @ObservedObject var model: AbakusDersModeli
    let stil: AbakusStili
    let olcek: CGFloat

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            abakus
                // tutorial_abacus_linear_margin_bottom
                .padding(.bottom, AbakusDersOlcegi.abakusAltBosluk)
            dugmeAlani
        }
    }

    /// Abaküs kendi sabit boyutunda kurulur, görsel olarak `olcek` kadar küçültülür ve yerleşimde
    /// küçülmüş boyutu kadar yer tutar. Dokunma hesabı görünümün kendi koordinatında yapılır.
    private var abakus: some View {
        let boyut: CGSize = AbakusGorunumu.boyut(basamakSayisi: 5)
        let en: CGFloat = boyut.width * olcek
        let boy: CGFloat = boyut.height * olcek
        return AbakusGorunumu(
            denetleyici: model.abakus,
            basamakSayisi: 5,
            kilitli: model.ortuAcik,
            stil: stil,
            animasyonSuresiMs: model.boncukAnimasyonSuresiMs,
            ses: model.hizmetler
        )
        .scaleEffect(olcek)
        .frame(width: en, height: boy)
    }

    /// kontrolButton ve startButton aynı yerde durur; biri görünürken öbürü gizlidir.
    private var dugmeAlani: some View {
        ZStack(alignment: .bottom) {
            if model.kontrolGorunur {
                AbakusAnaDugme(
                    metin: "Kontrol Et", yaziBoyu: 17, kose: 10, yukseklik: 56,
                    renk: AbakusRenk.renk(0x8CC63F), eylem: { model.kontrolDokunuldu() }
                )
                // Android: panel açıkken dinleyici kaldırılır; düğmenin görünümü değişmez.
                .allowsHitTesting(model.kontrolEtkin)
                // tutorial_bottom_panel_margin_bottom
                .padding(.bottom, 16)
            }
            if model.baslaGorunur {
                AbakusAnaDugme(
                    metin: "Başla", yaziBoyu: 18, kose: 15, yukseklik: 56,
                    renk: AbakusRenk.renk(0x8CC63F), eylem: { model.baslaDokunuldu() }
                )
                // startButton: layout_marginBottom
                .padding(.bottom, 15)
            }
        }
        // Düğme görünmezken de yer tutar (Android: visibility=invisible): 56 + 16.
        .frame(height: AbakusDersOlcegi.dugmeAlani, alignment: .bottom)
        // tutorial_control_button_side_margin
        .padding(.horizontal, 16)
    }
}

// MARK: - Katmanlar

/// Android: overlay — sonuç paneli açıkken altındaki her şeye dokunmayı engeller.
private struct AbakusDersOrtusu: View {
    @ObservedObject var model: AbakusDersModeli

    var body: some View {
        if model.ortuAcik {
            Color.black.opacity(0.001)
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture {}
        }
    }
}

/// lottieView (animaton_thirteen.json, 300 dp, ekranın ortasında): son soruda bütün cevaplar
/// doğruysa bir kez oynar (lottie_autoPlay=false, lottie_loop=false; showResultPanel içinde
/// playAnimation).
private struct AbakusKonfetiKatmani: View {
    let goster: Bool

    var body: some View {
        if goster {
            LottieGorunumu("animaton_thirteen", dongu: false)
                // abacus_fragment_lottie_size
                .frame(width: 300, height: 300)
                .allowsHitTesting(false)
        }
    }
}

/// Alttan kayan doğru / yanlış paneli.
private struct AbakusSonucPanelKatmani: View {
    @ObservedObject var model: AbakusDersModeli

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            if model.panel != .yok {
                panelIcerigi
                    // showResultPanel: translationY yükseklik → 0, alpha 0 → 1 (model 200 ms verir).
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    @ViewBuilder
    private var panelIcerigi: some View {
        if model.panel == .dogru {
            AbakusDogruPaneli(devam: { model.panelDugmesiDokunuldu() })
        } else {
            AbakusYanlisPaneli(
                dogruCevapMetni: model.dogruCevapMetni,
                cevapMetni: model.cevapMetni,
                tamam: { model.panelDugmesiDokunuldu() }
            )
        }
    }
}

/// Android: Toast karşılığı kısa bilgi balonu.
private struct AbakusBildirimKatmani: View {
    let metin: String?

    var body: some View {
        VStack {
            Spacer(minLength: 0)
            if let metin = metin {
                Text(metin)
                    .font(Tema.yazi(14))
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(Color.black.opacity(0.8)))
                    .padding(.bottom, 110)
            }
        }
        .allowsHitTesting(false)
    }
}
