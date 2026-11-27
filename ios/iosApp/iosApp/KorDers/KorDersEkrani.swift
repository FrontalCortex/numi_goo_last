import SwiftUI

// Kör (zihinden) ders ekranı.
//
// Android: BlindingLessonFragment + res/layout/fragment_blinding_lesson.xml (güncel, versionCode 26).
// Aynı ekran dört işte kullanılır: harita dersleri (kör toplama/çıkarma/çarpma), yarış dersleri, kupa
// modu (globalPartId == 9) ve günlük soru.
//
// Akış: "Başla" → sayılar kartta sırayla belirir (her sayı `timePeriod` ms, arada 200 ms boş) →
// cevap sayı kutusuna yazılır ya da abaküse dizilir → "Kontrol Et" → alttan doğru/yanlış paneli →
// sıradaki soru ya da ders bitişi (`bitti`). Kurallar ortak koddadır; bkz. KorDersModeli.
//
// Yerleşim (kök dolgu 10): solda çıkış düğmesi, üstte ortada ilerleme çubuğu, çıkış düğmesinin 12 altında
// 140 yüksekliğinde sayı kartı, kartın altında (sandıkta) süre; altta sayı kutusu ya da abaküs ve 48
// yüksekliğinde göçen "Kontrol Et" / "Başla" düğmesi; sağ kenarda düğmenin üstünde yan düğme sütunu
// (ipucu, sıfırla, adım atla, abaküs ayarı); alttan kayan doğru / yanlış panelleri.
//
// Bu ekranda OLMAYANLAR (Android'de var; ayrı işler, ayrıntı raporda): kural kitabı ve kural paneli
// (rulesBookButton, rulesPanelButton, view_rules_panel), rehber (guide) paneli, "Sobi ile analiz et"
// penceresinin kendisi (düğme ve çıkış noktası vardır; pencereyi dışarıdaki `sobiAnalizi` bağlantısı açar),
// öğretmene soru düğmesi (Android'de de gizli). Ders sonu anketi güncel Android'de kalktı.
//
// Kısa ekran: Android yerleşimi sabit dp kullanır ve ekran yüksekliğine göre ölçeklemez. iOS'ta abaküs
// (varsa) mevcut yüksekliğe sığacak oranda küçültülür; yer yetiyorsa oran 1'dir.

struct KorDersEkrani: View {
    @StateObject private var model: KorDersModeli
    @StateObject private var ayar: AbakusGorunumAyari
    @FocusState private var kutuOdakta: Bool
    /// Abaküsle cevaplanan derslerde abaküsün görünümü (bkz. `abakusStili(_:)`).
    private var abakusGorunumStili: AbakusStili = .varsayilan

    /// - Parameter sobiAnalizi: yanlış cevap panelindeki "Sobi ile analiz et" düğmesinin bağlantısı; nil ise
    ///   düğme hiç görünmez.
    init(
        baslangic: KorDersBaslangici,
        sobiAnalizi: AbakusSobiAnalizBaglantisi? = nil,
        bitti: @escaping (KorDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        self.init(
            baslangic: baslangic,
            kayit: SahteKorDersKayitServisi(),
            enerji: SahteKorDersEnerjiServisi(),
            ses: SahteKorDersSesServisi(),
            sobiAnalizi: sobiAnalizi,
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
        sobiAnalizi: AbakusSobiAnalizBaglantisi? = nil,
        bitti: @escaping (KorDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        _model = StateObject(
            wrappedValue: KorDersModeli(
                baslangic: baslangic,
                kayit: kayit,
                enerji: enerji,
                ses: ses,
                sobiAnaliz: sobiAnalizi,
                bitti: bitti,
                kapat: kapat
            )
        )
        _ayar = StateObject(wrappedValue: AbakusGorunumAyari())
    }

    /// Abaküsle cevaplanan derslerde (isBlinding == null) kullanıcının seçtiği boncuk ve çerçeve.
    /// Android: AbacusBeadRenderer / AbacusFrameRenderer bu ekranda da aynı tercihleri uygular.
    func abakusStili(_ stil: AbakusStili) -> KorDersEkrani {
        var kopya: KorDersEkrani = self
        kopya.abakusGorunumStili = stil
        return kopya
    }

    var body: some View {
        GeometryReader { alan in
            govde(ekran: alan.size)
        }
        .onAppear { model.gorundu() }
        .onDisappear { model.kayboldu() }
    }

    private func govde(ekran: CGSize) -> some View {
        ZStack {
            Color("background_color")
                .ignoresSafeArea()
                .onTapGesture { kutuOdakta = false }
            GeometryReader { alan in
                anaDuzen(boyut: alan.size, ekranGenisligi: ekran.width)
            }
            .padding(KorDersStil.kokBosluk)
            yanDugmeKatmani
            AbakusAyarKatmani(ayar: ayar, ekran: ekran)
            konfetiKatmani
            panelKatmani
        }
    }

    // MARK: Ölçek

    /// Abaküsün küçültülme oranı. `yukseklik`: kök dolgunun içinde kalan yükseklik. Sayı kutusuyla
    /// cevaplanan derste abaküs yok, oran 1.
    private func olcekHesapla(yukseklik: CGFloat) -> CGFloat {
        if model.klavyeyleCevap {
            return 1
        }
        // Çıkış satırı + boşluk + kart, tahta ile abaküs arası en az boşluk, abaküsün alt boşluğu,
        // düğme (48 + 15).
        let ust: CGFloat = AbakusDersOlcu.cikisBoyu + AbakusDersOlcu.kartUstBosluk
            + AbakusDersOlcu.kartYuksekligi
        let dugme: CGFloat = AbakusDersOlcu.anaDugmeYuksekligi + KorDersStil.anaDugmeAltBosluk
        let sabit: CGFloat = ust + AbakusDersOlcu.enAzAra + CGFloat(ayar.altBosluk) + dugme
        return AbakusDersOlcu.abakusOlcegi(yukseklik: yukseklik, sabit: sabit)
    }

    // MARK: Ana düzen

    private func anaDuzen(boyut: CGSize, ekranGenisligi: CGFloat) -> some View {
        let olcek: CGFloat = olcekHesapla(yukseklik: boyut.height)
        return VStack(spacing: 0) {
            ustBolum
            ortaBolum(olcek: olcek, ekranGenisligi: ekranGenisligi)
            altDugme
        }
        .frame(width: boyut.width, height: boyut.height, alignment: .top)
    }

    /// Çıkış düğmesi, ilerleme çubuğu ve sayı kartı üst üste biner: kart çıkış düğmesinin 12 altından
    /// başlar, çubuk ve düğme kartın üstündeki satırdadır.
    private var ustBolum: some View {
        ZStack(alignment: .top) {
            KorDersTahta(model: model)
                .padding(.horizontal, AbakusDersOlcu.kartYanBosluk)
                .padding(.top, AbakusDersOlcu.cikisBoyu + AbakusDersOlcu.kartUstBosluk)
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

    /// Kart ile alt düğme arası: süre göstergesi üstte, cevap alanı altında.
    private func ortaBolum(olcek: CGFloat, ekranGenisligi: CGFloat) -> some View {
        ZStack(alignment: .top) {
            if model.sureGorunur {
                Text(model.sureMetni)
                    .font(Tema.yazi(KorDersStil.sureYaziBoyu))
                    .foregroundColor(Color("yellow"))
                    // timerTextView: kartın altından abacus_fragment_timer_margin_top
                    .padding(.top, AbakusDersOlcu.sureUstBosluk)
            }
            cevapAlani(olcek: olcek, ekranGenisligi: ekranGenisligi)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Android: blindingOrRace — isBlinding == true ise numberInput, değilse abaküs görünür.
    @ViewBuilder
    private func cevapAlani(olcek: CGFloat, ekranGenisligi: CGFloat) -> some View {
        if model.klavyeyleCevap {
            // numberInput: kartın altı ile kontrolButton'ın üstü arasında ortalı.
            KorDersSayiKutusu(metin: $model.girdi, odak: $kutuOdakta)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            abakusAlani(olcek: olcek, ekranGenisligi: ekranGenisligi)
        }
    }

    /// Android: abacusContainer — kontrolButton'ın üstüne 10 dp boşlukla oturur, yatayda ortalı. Ayar
    /// panelindeki ölçek ve yan düğme sığdırması yalnızca çizimi değiştirir (pivot alt-orta).
    private func abakusAlani(olcek: CGFloat, ekranGenisligi: CGFloat) -> some View {
        let boyut: CGSize = AbakusGorunumu.boyut(basamakSayisi: 5)
        let en: CGFloat = boyut.width * olcek
        let boy: CGFloat = boyut.height * olcek
        let kullaniciX: CGFloat = CGFloat(ayar.abakusX)
        let kullaniciY: CGFloat = CGFloat(ayar.abakusY)
        let pay: CGFloat = AbakusYanSigdirma.pay(
            ekranGenisligi: ekranGenisligi, kullaniciX: kullaniciX, sutunVar: model.yanDugmeVar
        )
        let gorunenOlcek: CGSize = CGSize(width: kullaniciX * pay, height: kullaniciY * pay)
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            AbakusGorunumu(
                denetleyici: model.abakus,
                basamakSayisi: 5,
                kilitli: ayar.acik,
                stil: abakusGorunumStili,
                animasyonSuresiMs: model.boncukAnimasyonMs,
                degerDegisti: { (deger: Int) -> Void in model.abakusDegeriDegisti(deger) },
                boncukAyari: ayar.boncukAyari
            )
            .scaleEffect(olcek)
            .frame(width: en, height: boy)
            .scaleEffect(gorunenOlcek, anchor: .bottom)
            .padding(.bottom, CGFloat(ayar.altBosluk))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Android: startButton ("Başla") ve yerine geçen kontrolButton ("Kontrol Et"; bg_depth_hw_control:
    /// sayılar akarken pasif, gri).
    private var altDugme: some View {
        Group {
            if model.basladi {
                kontrolDugmesi
            } else {
                baslaDugmesi
            }
        }
        .padding(.horizontal, KorDersStil.anaDugmeYanBosluk)
        .padding(.bottom, KorDersStil.anaDugmeAltBosluk)
    }

    private var kontrolDugmesi: some View {
        let etkin: Bool = model.kontrolBasilabilir
        let yazi: Color = Color(etkin ? "button_text_enabled" : "button_text_disabled")
        let yuz: Color = etkin ? AbakusDersRenk.kontrolYuz : AbakusDersRenk.kontrolPasifYuz
        let dudak: Color = etkin ? AbakusDersRenk.kontrolDudak : AbakusDersRenk.kontrolPasifDudak
        return AbakusGocenDugme(
            metin: Metin.al("fragment_blinding_lesson_kontrol_button"),
            yaziBoyu: KorDersStil.dugmeYaziBoyu, yaziRengi: yazi, yuz: yuz, dudak: dudak,
            kose: KorDersStil.dugmeKose, yukseklik: AbakusDersOlcu.anaDugmeYuksekligi,
            eylem: { model.kontrolEt() }
        )
        .allowsHitTesting(etkin)
    }

    private var baslaDugmesi: some View {
        return AbakusGocenDugme(
            metin: Metin.al("fragment_blinding_lesson_start_button"),
            yaziBoyu: KorDersStil.dugmeYaziBoyu, yaziRengi: .black,
            yuz: AbakusDersRenk.kontrolYuz, dudak: AbakusDersRenk.kontrolDudak,
            kose: KorDersStil.dugmeKose, yukseklik: AbakusDersOlcu.anaDugmeYuksekligi,
            eylem: { model.basla() }
        )
    }

    // MARK: Katmanlar

    /// Sağ kenardaki düğmeler: alt düğmenin 4 üstünde, ekran kenarına yaslı. Kenara uzaklık düğmeden
    /// düğmeye değişir (0 ya da 1 pt); bkz. KorDersYanDugmeler.
    private var yanDugmeKatmani: some View {
        let altBosluk: CGFloat =
            KorDersStil.kokBosluk + KorDersStil.anaDugmeAltBosluk + AbakusDersOlcu.anaDugmeYuksekligi
            + AbakusDersOlcu.yanDugmeAraligi
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                KorDersYanDugmeler(model: model, ayar: ayar, ayarDokunuldu: { ayarDugmesiDokunuldu() })
            }
        }
        .padding(.bottom, altBosluk)
    }

    /// Android: abacusModeButton → AbacusSettingsPanel.toggle; açılırken resetAbacus + denetleyici kapalı.
    private func ayarDugmesiDokunuldu() {
        let denetleyici: AbakusDenetleyici = model.abakus
        ayar.degistir(
            acildi: { denetleyici.sifirla(animasyonlu: true) },
            kapandi: {}
        )
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
            AbakusDogruPaneli(
                baslik: Metin.al("fragment_blinding_lesson_correct_text"),
                dugmeMetni: Metin.al("fragment_blinding_lesson_continue_button"),
                devam: { model.devamEt() }
            )
        case .yanlis:
            AbakusYanlisPaneli(
                dogruCevapMetni: model.dogruCevapEtiketi,
                cevapMetni: model.seninCevabinMetni,
                analizGorunur: model.analizGorunur,
                analiz: { model.sobiAnaliziDokunuldu() },
                tamam: { model.tamam() },
                baslik: Metin.al("fragment_blinding_lesson_incorrect_text"),
                dugmeMetni: Metin.al("fragment_blinding_lesson_okay_button")
            )
        }
    }
}
