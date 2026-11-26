import SwiftUI

// Kör (zihinden) ders ekranı.
//
// Android: BlindingLessonFragment + res/layout/fragment_blinding_lesson.xml. Aynı ekran dört işte
// kullanılır: harita dersleri (kör toplama/çıkarma/çarpma), yarış dersleri, kupa modu
// (globalPartId == 9) ve günlük soru.
//
// Akış: "Başla" → sayılar tahtada sırayla belirir (her sayı `timePeriod` ms, arada 200 ms boş) →
// cevap sayı kutusuna yazılır → "Kontrol Et" → alttan doğru/yanlış paneli → sıradaki soru ya da
// ders bitişi (`bitti`). Kurallar ortak koddadır; bkz. KorDersModeli.
//
// Bu ekranda OLMAYANLAR (Android'de var): abaküs (cevabı abaküsle verilen dersler için yer
// tutucu çizilir), kural kitabı ve kural tablosu paneli, rehber (guide) paneli, abaküs ayar paneli,
// öğretmene soru düğmesi, Lottie animasyonları (konfeti ve düğme simgeleri), soru özeti
// (QuestionPanelFragment; `KorDersSonucu.soruPaneliGosterilmeli` ile dışarı bildirilir).

struct KorDersEkrani: View {
    @StateObject private var model: KorDersModeli
    @FocusState private var kutuOdakta: Bool

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

    var body: some View {
        ZStack {
            Color("background_color")
                .ignoresSafeArea()
                .onTapGesture { kutuOdakta = false }
            anaDuzen
                .padding(KorDersStil.kokBosluk)
            yanDugmeKatmani
            panelKatmani
        }
        .onAppear { model.gorundu() }
        .onDisappear { model.kayboldu() }
    }

    // MARK: Ana düzen

    private var anaDuzen: some View {
        VStack(spacing: 0) {
            ustBolum
            ortaBolum
            altDugme
        }
    }

    /// Çıkış düğmesi, ilerleme çubuğu ve tahta üst üste biner. Android'de tahta XML'de sonra
    /// geldiği için ikisinin üstüne çizilir; burada düğme ve çubuk görünür kalsın diye üstte.
    private var ustBolum: some View {
        ZStack(alignment: .top) {
            KorDersTahta(model: model)
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

    /// Android: quitButton (@android:drawable/ic_menu_close_clear_cancel).
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

    /// Tahta ile alt düğme arası: süre göstergesi üstte, cevap alanı ortada.
    private var ortaBolum: some View {
        ZStack(alignment: .top) {
            if model.sureGorunur {
                Text(model.sureMetni)
                    .font(Tema.yazi(KorDersStil.sureYaziBoyu))
                    .foregroundColor(Color("yellow"))
                    .offset(y: KorDersStil.sureUstBosluk)
            }
            cevapAlani
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private var cevapAlani: some View {
        if model.klavyeyleCevap {
            KorDersSayiKutusu(metin: $model.girdi, odak: $kutuOdakta)
        } else {
            abakusYerTutucu
        }
    }

    /// Android: abacusContainer. Abaküs görünümü (Abakus/AbakusGorunumu) bağlanana kadar yer tutucu:
    /// cevap sayı kutusuna yazılır ve abaküs değeri olarak okunur.
    private var abakusYerTutucu: some View {
        VStack(spacing: 12) {
            Text(KorDersStil.abakusYerTutucuMetni)
                .font(Tema.yazi(14))
                .foregroundColor(.white.opacity(0.7))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            KorDersSayiKutusu(metin: $model.girdi, odak: $kutuOdakta)
        }
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

    /// Sağ kenardaki düğmeler: alt düğmenin hemen üstünde, ekran kenarına yaslı.
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
        .padding(.trailing, 1)
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
