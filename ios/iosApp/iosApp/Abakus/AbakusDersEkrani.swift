import SwiftUI

// Abaküs dersi ekranı.
//
// Android karşılığı: AbacusFragment + res/layout/fragment_abacus.xml (güncel, versionCode 26). Ölçüler
// dimens.xml'deki abacus_fragment_*, tutorial_* ve flash_card_height değerleridir (dp → pt birebir).
//
// Yerleşim (kök dolgu 10): solda çıkış düğmesi, üstte ortada ilerleme çubuğu, çıkış düğmesinin 12 altında
// 140 yüksekliğinde sayı kartı (eski kara tahta görselinin yerine), kartın altında (sandıkta) süre;
// altta abaküs ve 48 yüksekliğinde göçen "Kontrol Et" / "Başla" düğmesi; sağ kenarda, düğmenin üstünde
// yan düğme sütunu (abaküs ayarı); alttan kayan doğru / yanlış panelleri.
//
// Kapsam dışı bırakılan Android parçaları (ayrı işler; ayrıntı raporda):
//  - rehber paneli (panelContent, btnBack, btnForward; Sobi maskotlu adım adım rehber),
//  - öğretmene sor düğmesi (TeacherQuestionsFeature.ENABLED = false; Android'de de gizli),
//  - "Sobi ile analiz et" penceresinin kendisi (MistakeReviewDialog, MistakeAnalyzer, StepExplainer):
//    düğme ve çıkış noktası burada, pencereyi dışarıdaki `sobiAnalizi` bağlantısı açar.
// Ipucu düğmesi güncel Android'de gizli olduğundan ekranda yoktur.
//
// Kısa ekran: Android yerleşimi sabit dp kullanır ve ekran yüksekliğine göre ölçeklemez. iOS'ta
// abaküs, mevcut yüksekliğe sığacak oranda küçültülür (bkz. AbakusDersOlcu.abakusOlcegi); yer
// yetiyorsa oran 1'dir ve yerleşim Android ile aynıdır.

struct AbakusDersEkrani: View {
    @StateObject private var model: AbakusDersModeli
    @StateObject private var ayar: AbakusGorunumAyari
    /// Ders içi Sobi rehberi (Android: panelContent + rehber akışı; bkz. Rehber/).
    @StateObject private var rehber: RehberAkisi
    /// Kural kitabı ve sihirli değnek paneli (bkz. Kurallar/).
    @StateObject private var kural: KuralModeli
    private let kapat: () -> Void
    private let stil: AbakusStili
    private let cikisGizli: Bool

    /// - Parameters:
    ///   - dersBaslangici: hangi ders, hangi sorular.
    ///   - hizmetler: ses, titreşim, çevrimiçi durumu, ilerleme / enerji / süre kayıtları.
    ///   - stil: abaküsün görünümü (kullanıcının mağazadan seçtiği boncuk ve çerçeve).
    ///   - sobiAnalizi: yanlış cevap panelindeki "Sobi ile analiz et" düğmesinin bağlantısı; nil ise düğme
    ///     hiç görünmez (Android: bindReviewButton).
    ///   - cikisGizli: hesapsız kullanıcının ilk açılış akışındaki derste çıkış düğmesi gizlidir
    ///     (Android: isFirstRunLesson → quitButton INVISIBLE).
    ///   - bitti: bütün sorular bitince bir kez çağrılır; sonuç ekranını yönetici oturum açar.
    ///   - kapat: kapatma düğmesine basılınca (kayma animasyonundan sonra) çağrılır.
    init(
        dersBaslangici: AbakusDersBaslangici,
        hizmetler: AbakusDersHizmetleri = SahteAbakusDersHizmetleri(),
        stil: AbakusStili = .varsayilan,
        sobiAnalizi: AbakusSobiAnalizBaglantisi? = nil,
        cikisGizli: Bool = false,
        baslangicKurali: KuralBaslangicDurumu = .kapali,
        bitti: @escaping (AbakusDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        _model = StateObject(
            wrappedValue: AbakusDersModeli(
                baslangic: dersBaslangici, hizmetler: hizmetler, sobiAnaliz: sobiAnalizi, bitti: bitti
            )
        )
        _ayar = StateObject(wrappedValue: AbakusGorunumAyari())
        _rehber = StateObject(wrappedValue: RehberAkisi(girisSuresi: 0.3, cikisSuresi: 0.3))
        // Android: FirebaseAuth.currentUser == null → kural düğmeleri gizli (= hesapsız ilk öğretici dersi).
        _kural = StateObject(
            wrappedValue: KuralModeli(
                ders: KuralDersBilgisi.abakusDersi(dersBaslangici, hesapsiz: cikisGizli),
                baslangic: baslangicKurali
            )
        )
        self.kapat = kapat
        self.stil = stil
        self.cikisGizli = cikisGizli
    }

    var body: some View {
        GeometryReader { alan in
            ZStack {
                Color("background_color").ignoresSafeArea()
                AbakusDersIcerigi(
                    model: model, ayar: ayar, stil: stil, kapat: kapat, cikisGizli: cikisGizli,
                    ekranGenisligi: alan.size.width
                )
                // abacus_fragment_root_padding
                .padding(AbakusDersOlcu.kokBosluk)
                AbakusYanDugmeKatmani(model: model, ayar: ayar, kural: kural)
                // Kural kitabı düğmesi, kitap ve kural paneli (rulesBookButton, rulesFragmentContainer,
                // rulesPanelScrollView).
                KuralKatmani(
                    kural: kural,
                    yerlesim: KuralYerlesimi.abakusDersi(
                        ekran: alan.size, altBosluk: CGFloat(ayar.altBosluk), sureGorunur: model.sureGorunur
                    ),
                    klavyeAcik: false
                )
                AbakusAyarKatmani(ayar: ayar, ekran: alan.size)
                AbakusDersOrtusu(model: model)
                AbakusKonfetiKatmani(goster: model.konfeti)
                AbakusSonucPanelKatmani(model: model)
                AbakusBildirimKatmani(metin: model.bildirim)
            }
            // Ders içi rehber: dokunma engeli, vurgu ve kayan panel (panel abaküsün hemen üstüne oturur).
            .overlayPreferenceValue(RehberHedefAnahtari.self) { (hedefler: [RehberHedefi: Anchor<CGRect>]) in
                RehberKatmani(
                    akis: rehber, hedefler: hedefler, duzen: RehberDuzeni.abakusDersi,
                    yer: RehberYeri.abakusUstu(aralik: 0)
                )
            }
            // closeFragment: ekran genişliği kadar SOLA kayar, 400 ms.
            .offset(x: model.kapaniyor ? -alan.size.width : 0)
        }
        .onAppear {
            model.gorundu()
            rehberiBaslat()
        }
        .onDisappear { model.kayboldu() }
        // Rehber 4: kural tablosundan bir kural seçilince (panel açılınca) rehber ilerler
        // (Android: completeGuideIfWaitingForRulesSelection).
        .onChange(of: kural.panelAcik) { (acik: Bool) in
            if acik {
                rehber.kuralSecildi()
            }
        }
    }

    /// Android: AbacusFragment.setupGuidePanel — `abacusGuideNumber` varsa ve ders adımı 1 ise rehber açılır
    /// (rehber 6 en çok iki kez). Bir akış yalnızca bir kez başlar; ekran yeniden göründüğünde tekrarlanmaz.
    private func rehberiBaslat() {
        guard let baslatma = RehberOrtak.abakusDersiRehberi(
            rehberNo: model.baslangic.rehberNo, ilkAdim: model.baslangic.rehberIlkAdim
        ) else {
            return
        }
        let dersModeli: AbakusDersModeli = model
        let denetleyici: AbakusDenetleyici = model.abakus
        let ayarNesnesi: AbakusGorunumAyari = ayar
        let kuralNesnesi: KuralModeli = kural
        rehber.boncukOynat = { (kimlik: String) -> Void in
            denetleyici.rehberBoncugu(kimlik)
        }
        rehber.kuralKitabiniKapat = { () -> Void in
            kuralNesnesi.kitabiKapat()
        }
        rehber.hedefTiklandi = { (hedef: RehberHedefi) -> Void in
            // Android: handleRequiredClickTarget — hedefin asıl işi yapılır.
            switch hedef {
            case .abakusAyarDugmesi:
                ayarNesnesi.degistir(
                    acildi: { denetleyici.sifirla(animasyonlu: true) },
                    kapandi: {}
                )
            case .kuralPaneliDugmesi:
                _ = kuralNesnesi.panelDugmesiDokunuldu()
            case .kuralKitabiDugmesi:
                kuralNesnesi.kitabiAc()
            default:
                break
            }
        }
        rehber.bitti = { (normal: Bool) -> Void in
            if normal {
                dersModeli.rehberBitti()
            }
        }
        rehber.baslat(baslatma.adimlar, gecikme: 0.5, acildi: { RehberOrtak.gosterildi(baslatma) })
    }
}

// MARK: - İçerik

/// Kök ConstraintLayout'un içindekiler: üstte çıkış, ilerleme, sayı kartı; altta abaküs ve düğme.
private struct AbakusDersIcerigi: View {
    @ObservedObject var model: AbakusDersModeli
    @ObservedObject var ayar: AbakusGorunumAyari
    let stil: AbakusStili
    let kapat: () -> Void
    let cikisGizli: Bool
    let ekranGenisligi: CGFloat

    var body: some View {
        GeometryReader { alan in
            katmanlar(boyut: alan.size)
        }
    }

    private func katmanlar(boyut: CGSize) -> some View {
        // Ölçeklenmeyen parçalar: çıkış satırı + boşluk, kart, tahta ile abaküs arası en az boşluk,
        // abaküsün alt boşluğu, düğme alanı (48 + 16).
        let ust: CGFloat = AbakusDersOlcu.cikisBoyu + AbakusDersOlcu.kartUstBosluk
            + AbakusDersOlcu.kartYuksekligi
        let alt: CGFloat = CGFloat(ayar.altBosluk) + AbakusDersAltBolumu.dugmeAlaniYuksekligi
        let sabit: CGFloat = ust + AbakusDersOlcu.enAzAra + alt
        let olcek: CGFloat = AbakusDersOlcu.abakusOlcegi(yukseklik: boyut.height, sabit: sabit)
        return ZStack(alignment: .top) {
            AbakusDersUstBolumu(model: model, kapat: kapat, cikisGizli: cikisGizli)
            AbakusDersAltBolumu(
                model: model, ayar: ayar, stil: stil, olcek: olcek, ekranGenisligi: ekranGenisligi
            )
        }
        .frame(width: boyut.width, height: boyut.height, alignment: .top)
    }
}

/// Çıkış düğmesi, ilerleme çubuğu, sayı kartı ve süre.
private struct AbakusDersUstBolumu: View {
    @ObservedObject var model: AbakusDersModeli
    let kapat: () -> Void
    let cikisGizli: Bool

    var body: some View {
        ZStack(alignment: .top) {
            kartVeSure
            AbakusIlerlemeCubugu(oran: model.ilerleme, renk: model.ilerlemeRengi)
                // abacus_fragment_progress_margin
                .padding(.top, 16)
            HStack {
                cikisDugmesi
                Spacer(minLength: 0)
            }
        }
    }

    /// quitButton: 48 dp, `cross` görseli. Hesapsız ilk akışta görünmez ve dokunulmaz.
    private var cikisDugmesi: some View {
        Button(action: { model.kapatDokunuldu(kapat) }) {
            Image("cross")
                .resizable()
                .scaledToFit()
                // abacus_fragment_quit_size
                .frame(width: AbakusDersOlcu.cikisBoyu, height: AbakusDersOlcu.cikisBoyu)
        }
        .buttonStyle(.plain)
        .opacity(cikisGizli ? 0 : 1)
        .allowsHitTesting(!cikisGizli)
    }

    /// blackboardImage: çıkış düğmesinin 12 altında, kenarlarda 24; timerTextView kartın 6 altında.
    private var kartVeSure: some View {
        VStack(spacing: 0) {
            AbakusSayiKarti(ilkSayi: model.ilkSayi, islec: model.islec, ikinciSayi: model.ikinciSayi)
                .padding(.horizontal, AbakusDersOlcu.kartYanBosluk)
                .padding(.top, AbakusDersOlcu.cikisBoyu + AbakusDersOlcu.kartUstBosluk)
            if model.sureGorunur {
                Text(model.sureMetni)
                    // abacus_fragment_timer_text_size
                    .font(Tema.yazi(18))
                    .foregroundColor(Color("yellow"))
                    // abacus_fragment_timer_margin_top
                    .padding(.top, AbakusDersOlcu.sureUstBosluk)
            }
            Spacer(minLength: 0)
        }
    }
}

/// Abaküs ve altındaki "Kontrol Et" / "Başla" düğmesi.
private struct AbakusDersAltBolumu: View {
    @ObservedObject var model: AbakusDersModeli
    @ObservedObject var ayar: AbakusGorunumAyari
    let stil: AbakusStili
    let olcek: CGFloat
    let ekranGenisligi: CGFloat

    /// kontrolButton: yükseklik 48 + alt boşluk 16 (tutorial_bottom_panel_margin_bottom).
    static let dugmeAlaniYuksekligi: CGFloat = AbakusDersOlcu.anaDugmeYuksekligi + 16

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            abakus
                // tutorial_abacus_linear_margin_bottom (ayar panelindeki alt boşluk kaydırıcısı)
                .padding(.bottom, CGFloat(ayar.altBosluk))
            dugmeAlani
        }
    }

    /// Abaküs kendi sabit boyutunda kurulur; kısa ekran oranıyla küçültülür ve yerleşimde küçülmüş boyutu
    /// kadar yer tutar. Ayar panelindeki ölçek ve yan düğme sığdırması yalnızca çizimi değiştirir
    /// (Android: view.scaleX/scaleY, pivot alt-orta; yerleşim değişmez).
    private var abakus: some View {
        let boyut: CGSize = AbakusGorunumu.boyut(basamakSayisi: 5)
        let en: CGFloat = boyut.width * olcek
        let boy: CGFloat = boyut.height * olcek
        let kullaniciX: CGFloat = CGFloat(ayar.abakusX)
        let kullaniciY: CGFloat = CGFloat(ayar.abakusY)
        // Sihirli değnek de sütundadır (hesapsız ilk dersin düğmeleri zaten gizli olduğundan hesapsiz: false).
        let degnekGorunumu: KuralGorunurluk = KuralDersBilgisi.abakusDersi(
            model.baslangic, hesapsiz: false
        ).panelDugmesi
        let sutunVar: Bool = model.baslangic.ayarDugmesiGorunur || degnekGorunumu != .yok
        let pay: CGFloat = AbakusYanSigdirma.pay(
            ekranGenisligi: ekranGenisligi, kullaniciX: kullaniciX, sutunVar: sutunVar
        )
        let gorunenOlcek: CGSize = CGSize(width: kullaniciX * pay, height: kullaniciY * pay)
        let kilitli: Bool = model.ortuAcik || ayar.acik
        return AbakusGorunumu(
            denetleyici: model.abakus,
            basamakSayisi: 5,
            kilitli: kilitli,
            stil: stil,
            animasyonSuresiMs: model.boncukAnimasyonSuresiMs,
            ses: model.hizmetler,
            degerDegisti: { (deger: Int) -> Void in model.abakusDegeriDegisti(deger) },
            boncukAyari: ayar.boncukAyari
        )
        .scaleEffect(olcek)
        .frame(width: en, height: boy)
        .scaleEffect(gorunenOlcek, anchor: .bottom)
        // Rehber paneli bu çerçevenin üstüne oturur (Android: abacusContainer).
        .rehberHedefi(.abakusAlani)
    }

    /// kontrolButton ve startButton aynı yerde durur; biri görünürken öbürü gizlidir.
    private var dugmeAlani: some View {
        ZStack(alignment: .bottom) {
            if model.kontrolGorunur {
                AbakusGocenDugme(
                    metin: Metin.al("fragment_abacus_kontrol_button"), yaziBoyu: 17, yaziRengi: .black,
                    yuz: AbakusDersRenk.kontrolYuz, dudak: AbakusDersRenk.kontrolDudak, kose: 10,
                    yukseklik: AbakusDersOlcu.anaDugmeYuksekligi,
                    eylem: { model.kontrolDokunuldu() }
                )
                // Android: panel açıkken dinleyici kaldırılır; düğmenin görünümü değişmez.
                .allowsHitTesting(model.kontrolEtkin)
                .rehberHedefi(.kontrolDugmesi)
                // tutorial_bottom_panel_margin_bottom
                .padding(.bottom, 16)
            }
            if model.baslaGorunur {
                AbakusGocenDugme(
                    metin: Metin.al("fragment_abacus_start_button"), yaziBoyu: 18, yaziRengi: .black,
                    yuz: AbakusDersRenk.kontrolYuz, dudak: AbakusDersRenk.kontrolDudak, kose: 10,
                    yukseklik: AbakusDersOlcu.anaDugmeYuksekligi,
                    eylem: { model.baslaDokunuldu() }
                )
                // startButton: layout_marginBottom
                .padding(.bottom, 15)
            }
        }
        // Düğme görünmezken de yer tutar (Android: visibility=invisible): 48 + 16.
        .frame(height: AbakusDersAltBolumu.dugmeAlaniYuksekligi, alignment: .bottom)
        // tutorial_control_button_side_margin
        .padding(.horizontal, 16)
    }
}

// MARK: - Yan düğmeler ve ayar paneli

/// Sağ kenardaki düğme sütunu: kontrolButton'ın 4 üstünde, ekran kenarına yaslı (marginEnd −10 + kök
/// dolgu 10). Abaküs dersinde abaküs ayarı düğmesi ve onun üstünde sihirli değnek (kural paneli; bkz.
/// Kurallar/) vardır; kural kitabı düğmesi sağ üsttedir (KuralKatmani), öğretmene sor düğmesi Android'de
/// de gizli.
private struct AbakusYanDugmeKatmani: View {
    @ObservedObject var model: AbakusDersModeli
    @ObservedObject var ayar: AbakusGorunumAyari
    /// Sihirli değnek (rulesPanelButton) abaküs ayar düğmesinin 4 üstündedir (Android: bottomToTopOf abacusModeButton).
    @ObservedObject var kural: KuralModeli

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                VStack(alignment: .trailing, spacing: AbakusDersOlcu.yanDugmeAraligi) {
                    if kural.ders.panelDugmesi != .yok {
                        KuralDegnekDugmesi(eylem: { _ = kural.panelDugmesiDokunuldu() })
                            .rehberHedefi(.kuralPaneliDugmesi)
                    }
                    if model.baslangic.ayarDugmesiGorunur {
                        AbakusAyarDugmesi(acik: ayar.acik, eylem: { ayarDugmesiDokunuldu() })
                            .rehberHedefi(.abakusAyarDugmesi)
                    }
                }
            }
        }
        // kök dolgu + kontrolButton alt boşluğu + kontrolButton yüksekliği + abacus_fragment_ask_button_margin_top
        .padding(.bottom, AbakusDersOlcu.kokBosluk + AbakusDersAltBolumu.dugmeAlaniYuksekligi
            + AbakusDersOlcu.yanDugmeAraligi)
    }

    /// Android: abacusModeButtonClick → açılırken resetAbacus + denetleyici kapalı.
    private func ayarDugmesiDokunuldu() {
        let denetleyici: AbakusDenetleyici = model.abakus
        ayar.degistir(
            acildi: { denetleyici.sifirla(animasyonlu: true) },
            kapandi: {}
        )
    }
}

/// abacusSettingsPanel: 340 dp, ekranın sağından kayarak girer (300 ms), dikey konum yaklaşık %10
/// (layout_constraintVertical_bias = 0.1; panel yüksekliği ~300 dp varsayılarak).
struct AbakusAyarKatmani: View {
    @ObservedObject var ayar: AbakusGorunumAyari
    let ekran: CGSize

    var body: some View {
        let ust: CGFloat = max(0.1 * (ekran.height - 300), 0)
        return VStack(spacing: 0) {
            AbakusAyarPaneli(ayar: ayar, kaydet: { ayar.kaydet(kapandi: {}) })
                .padding(.top, ust)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity)
        .offset(x: ayar.acik ? 0 : ekran.width)
        .allowsHitTesting(ayar.acik)
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
            AbakusDogruPaneli(
                baslik: Metin.al("fragment_abacus_correct_text"),
                dugmeMetni: Metin.al("fragment_abacus_continue_button"),
                devam: { model.panelDugmesiDokunuldu() }
            )
        } else {
            AbakusYanlisPaneli(
                dogruCevapMetni: model.dogruCevapMetni,
                cevapMetni: model.cevapMetni,
                analizGorunur: model.analizGorunur,
                analiz: { model.sobiAnaliziDokunuldu() },
                tamam: { model.panelDugmesiDokunuldu() },
                baslik: Metin.al("fragment_abacus_incorrect_text"),
                dugmeMetni: Metin.al("fragment_abacus_okay_button")
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
