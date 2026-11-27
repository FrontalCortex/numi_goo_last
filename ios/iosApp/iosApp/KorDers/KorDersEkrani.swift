import SwiftUI

// Kör (zihinden) ders ekranı.
//
// Android: BlindingLessonFragment + res/layout/fragment_blinding_lesson.xml. Aynı ekran dört işte
// kullanılır: harita dersleri (kör toplama/çıkarma/çarpma), yarış dersleri, kupa modu
// (globalPartId == 9) ve günlük soru.
//
// Akış: "Başla" → sayılar tahtada sırayla belirir (her sayı `timePeriod` ms, arada 200 ms boş) →
// cevap sayı kutusuna yazılır ya da abaküse dizilir → "Kontrol Et" → alttan doğru/yanlış paneli →
// sıradaki soru ya da ders bitişi (`bitti`). Kurallar ortak koddadır; bkz. KorDersModeli.
//
// Bu ekranda OLMAYANLAR (Android'de var): kural kitabı ve kural tablosu paneli, rehber (guide)
// paneli, abaküs ayar paneli (abacusModeButton, abacusSettingsPanel), öğretmene soru düğmesi,
// soru özeti (QuestionPanelFragment; `KorDersSonucu.soruPaneliGosterilmeli` ile dışarı bildirilir).
//
// Kısa ekran: Android yerleşimi sabit dp kullanır ve ekran yüksekliğine göre ölçeklemez. iOS'ta
// tahta (ve varsa abaküs) mevcut yüksekliğe sığacak oranda küçültülür; yer yetiyorsa oran 1'dir.

struct KorDersEkrani: View {
    @StateObject private var model: KorDersModeli
    @FocusState private var kutuOdakta: Bool
    /// Abaküsle cevaplanan derslerde abaküsün görünümü (bkz. `abakusStili(_:)`).
    private var abakusGorunumStili: AbakusStili = .varsayilan

    init(
        baslangic: KorDersBaslangici,
        bitti: @escaping (KorDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        self.init(
            baslangic: baslangic,
            kayit: SahteKorDersKayitServisi(),
            enerji: SahteKorDersEnerjiServisi(),
            ses: SahteKorDersSesServisi(),
            bitti: bitti,
            kapat: kapat
        )
    }

    /// Gerçek servisler bağlandığında kullanılacak kurucu.
    init(
        baslangic: KorDersBaslangici,
        kayit: KorDersKayitServisi,
        enerji: KorDersEnerjiServisi,
        ses: KorDersSesServisi,
        bitti: @escaping (KorDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        _model = StateObject(
            wrappedValue: KorDersModeli(
                baslangic: baslangic,
                kayit: kayit,
                enerji: enerji,
                ses: ses,
                bitti: bitti,
                kapat: kapat
            )
        )
    }

    /// Abaküsle cevaplanan derslerde (isBlinding == null) kullanıcının seçtiği boncuk ve çerçeve.
    /// Android: AbacusBeadRenderer / AbacusFrameRenderer bu ekranda da aynı tercihleri uygular.
    func abakusStili(_ stil: AbakusStili) -> KorDersEkrani {
        var kopya: KorDersEkrani = self
        kopya.abakusGorunumStili = stil
        return kopya
    }

    var body: some View {
        ZStack {
            Color("background_color")
                .ignoresSafeArea()
                .onTapGesture { kutuOdakta = false }
            GeometryReader { alan in
                anaDuzen(boyut: alan.size)
            }
            .padding(KorDersStil.kokBosluk)
            yanDugmeKatmani
            konfetiKatmani
            panelKatmani
        }
        .onAppear { model.gorundu() }
        .onDisappear { model.kayboldu() }
    }

    // MARK: Ölçek

    /// Tahta ile abaküsün küçültülme oranı. `yukseklik`: kök dolgunun içinde kalan yükseklik.
    private func olcekHesapla(yukseklik: CGFloat) -> CGFloat {
        let dugme: CGFloat = KorDersStil.anaDugmeYukseklik + KorDersStil.anaDugmeAltBosluk
        if model.klavyeyleCevap {
            // Tahta + sayı kutusu + düğme; kutunun üstünde ve altında en az 8 pt.
            let kutu: CGFloat = KorDersStil.kutuYukseklik + 2 * KorDersStil.kutuEnAzBosluk
            let sabit: CGFloat = KorDersStil.tahtaUstBosluk + kutu + dugme
            let kalan: CGFloat = yukseklik - sabit
            if kalan >= KorDersStil.tahtaYukseklik {
                return 1
            }
            let oran: CGFloat = kalan / KorDersStil.tahtaYukseklik
            return min(max(oran, AbakusDersOlcegi.enKucuk), 1)
        }
        // Tahta + abaküs + düğme.
        let bosluklar: CGFloat = KorDersStil.tahtaUstBosluk + AbakusDersOlcegi.enAzAra
            + KorDersStil.abakusAltBosluk
        return AbakusDersOlcegi.hesapla(yukseklik: yukseklik, sabit: bosluklar + dugme)
    }

    // MARK: Ana düzen

    private func anaDuzen(boyut: CGSize) -> some View {
        let olcek: CGFloat = olcekHesapla(yukseklik: boyut.height)
        return VStack(spacing: 0) {
            ustBolum(olcek: olcek)
            ortaBolum(olcek: olcek)
            altDugme
        }
        .frame(width: boyut.width, height: boyut.height, alignment: .top)
    }

    /// Çıkış düğmesi, ilerleme çubuğu ve tahta üst üste biner. Android'de tahta XML'de sonra
    /// geldiği için ikisinin üstüne çizilir; burada düğme ve çubuk görünür kalsın diye üstte.
    private func ustBolum(olcek: CGFloat) -> some View {
        ZStack(alignment: .top) {
            KorDersTahta(model: model, olcek: olcek)
                .padding(.top, KorDersStil.tahtaUstBosluk)
            if !model.ilerlemeGizli {
                KorDersIlerlemeCubugu(oran: model.ilerleme, renk: model.ilerlemeRengi)
                    .padding(.top, KorDersStil.ilerlemeUstBosluk)
            }
            HStack(spacing: 0) {
                cikisDugmesi
                Spacer(minLength: 0)
            }
        }
    }

    /// Android: quitButton (@android:drawable/ic_menu_close_clear_cancel; sistem görseli, asset yok).
    private var cikisDugmesi: some View {
        Button(action: { model.cik() }) {
            Image(systemName: "xmark")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)
                .frame(width: KorDersStil.cikisBoyu, height: KorDersStil.cikisBoyu)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /// Tahta ile alt düğme arası: süre göstergesi üstte, cevap alanı altında.
    private func ortaBolum(olcek: CGFloat) -> some View {
        ZStack(alignment: .top) {
            if model.sureGorunur {
                Text(model.sureMetni)
                    .font(Tema.yazi(KorDersStil.sureYaziBoyu))
                    .foregroundColor(Color("yellow"))
                    .offset(y: KorDersStil.sureUstBosluk)
            }
            cevapAlani(olcek: olcek)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Android: blindingOrRace — isBlinding == true ise numberInput, değilse abaküs görünür.
    @ViewBuilder
    private func cevapAlani(olcek: CGFloat) -> some View {
        if model.klavyeyleCevap {
            // numberInput: tahtanın altı ile kontrolButton'ın üstü arasında ortalı.
            KorDersSayiKutusu(metin: $model.girdi, odak: $kutuOdakta)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            abakusAlani(olcek: olcek)
        }
    }

    /// Android: abacusContainer — kontrolButton'ın üstüne 10 dp boşlukla oturur, yatayda ortalı.
    private func abakusAlani(olcek: CGFloat) -> some View {
        let boyut: CGSize = AbakusGorunumu.boyut(basamakSayisi: 5)
        let en: CGFloat = boyut.width * olcek
        let boy: CGFloat = boyut.height * olcek
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            AbakusGorunumu(
                denetleyici: model.abakus,
                basamakSayisi: 5,
                kilitli: false,
                stil: abakusGorunumStili,
                animasyonSuresiMs: model.boncukAnimasyonMs
            )
            .scaleEffect(olcek)
            .frame(width: en, height: boy)
            .padding(.bottom, KorDersStil.abakusAltBosluk)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Android: startButton ("Başla") ve yerine geçen kontrolButton ("Kontrol Et").
    private var altDugme: some View {
        Group {
            if model.basladi {
                KorDersBasmaDugmesi(
                    baslik: KorDersStil.kontrolMetni,
                    zemin: Color(model.kontrolBasilabilir ? "button_enabled" : "button_disabled"),
                    yaziRengi: Color(
                        model.kontrolBasilabilir ? "button_text_enabled" : "button_text_disabled"
                    ),
                    etkin: model.kontrolBasilabilir,
                    eylem: { model.kontrolEt() }
                )
            } else {
                KorDersBasmaDugmesi(
                    baslik: KorDersStil.baslaMetni,
                    zemin: KorDersStil.dogruYesil,
                    yaziRengi: .black,
                    basmaAnimasyonu: false,
                    eylem: { model.basla() }
                )
            }
        }
        .padding(.horizontal, KorDersStil.anaDugmeYanBosluk)
        .padding(.bottom, KorDersStil.anaDugmeAltBosluk)
    }

    // MARK: Katmanlar

    /// Sağ kenardaki düğmeler: alt düğmenin hemen üstünde, ekran kenarına yaslı. Kenara uzaklık
    /// düğmeden düğmeye değişir (0 ya da 1 pt); bkz. KorDersYanDugmeler.
    private var yanDugmeKatmani: some View {
        let altBosluk: CGFloat =
            KorDersStil.kokBosluk + KorDersStil.anaDugmeAltBosluk + KorDersStil.anaDugmeYukseklik
            + KorDersStil.yanDugmeAraligi
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                KorDersYanDugmeler(model: model)
            }
        }
        .padding(.bottom, altBosluk)
    }

    /// Android: lottieView (animaton_thirteen.json, 300 dp, ekranın ortasında; autoPlay=false,
    /// loop=false). Son soruda bütün cevaplar doğruysa showResultPanel içinde bir kez oynatılır.
    @ViewBuilder
    private var konfetiKatmani: some View {
        if model.konfeti {
            LottieGorunumu("animaton_thirteen", dongu: false)
                .frame(width: 300, height: 300)
                .allowsHitTesting(false)
        }
    }

    /// Android: overlay (panel açıkken alttaki her şeye dokunmayı keser) + correctPanel / incorrectPanel.
    private var panelKatmani: some View {
        ZStack(alignment: .bottom) {
            if model.panel != nil {
                Color.black.opacity(0.001)
                    .ignoresSafeArea()
                    .onTapGesture {}
            }
            if let panel = model.panel {
                panelIcerigi(panel)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
    }

    @ViewBuilder
    private func panelIcerigi(_ panel: KorDersPanel) -> some View {
        switch panel {
        case .dogru:
            KorDersDogruPaneli(devam: { model.devamEt() })
        case .yanlis:
            KorDersYanlisPaneli(
                dogruCevapEtiketi: model.dogruCevapEtiketi,
                seninCevabinMetni: model.seninCevabinMetni,
                tamam: { model.tamam() }
            )
        }
    }
}
