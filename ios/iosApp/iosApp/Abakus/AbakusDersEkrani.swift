import SwiftUI

// Abaküs dersi ekranı.
//
// Android karşılığı: AbacusFragment + res/layout/fragment_abacus.xml. Ölçüler dimens.xml'deki
// abacus_fragment_* ve tutorial_* değerleridir (dp → pt birebir).
//
// Kapsam dışı bırakılan Android parçaları (ayrı işler): rehber paneli (panelContent, btnBack,
// btnForward), kurallar kitabı ve kural tabloları (rulesBookButton, rulesPanelButton,
// rulesPanelScrollView), öğretmene sor düğmesi, abaküs ayar paneli (abacusModeButton,
// abacusSettingsPanel), ders sonu anketi (QuestionPanelFragment), konfeti Lottie'si.

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

// MARK: - İçerik

/// Kök ConstraintLayout'un içindekiler: üstte ilerleme ve tahta, altta abaküs ve düğme.
private struct AbakusDersIcerigi: View {
    @ObservedObject var model: AbakusDersModeli
    let stil: AbakusStili
    let kapat: () -> Void

    var body: some View {
        ZStack(alignment: .top) {
            AbakusDersUstBolumu(model: model, kapat: kapat)
            AbakusDersAltBolumu(model: model, stil: stil)
        }
    }
}

/// Tahta, soru, ilerleme çubuğu, kapatma düğmesi, süre ve ipucu.
private struct AbakusDersUstBolumu: View {
    @ObservedObject var model: AbakusDersModeli
    let kapat: () -> Void

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 0) {
                AbakusKaraTahta(ilkSayi: model.ilkSayi, islec: model.islec, ikinciSayi: model.ikinciSayi)
                    // blackboardImage: layout_marginTop
                    .padding(.top, 20)
                if model.sureGorunur {
                    Text(model.sureMetni)
                        .font(Tema.yazi(18))
                        .foregroundColor(Color("yellow"))
                        // abacus_fragment_timer_margin_top
                        .padding(.top, -10)
                }
                Spacer(minLength: 0)
            }
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
            AbakusIpucuBolumu(model: model)
                // hintContainer: tahtanın altına hizalı (20 + 300).
                .padding(.top, 320)
        }
    }
}

/// Abaküs ve altındaki "Kontrol Et" / "Başla" düğmesi.
private struct AbakusDersAltBolumu: View {
    @ObservedObject var model: AbakusDersModeli
    let stil: AbakusStili

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            AbakusGorunumu(
                denetleyici: model.abakus,
                basamakSayisi: 5,
                kilitli: model.ortuAcik,
                stil: stil,
                animasyonSuresiMs: model.boncukAnimasyonSuresiMs,
                ses: model.hizmetler
            )
            // tutorial_abacus_linear_margin_bottom
            .padding(.bottom, 10)
            dugmeAlani
        }
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
        .frame(height: 72, alignment: .bottom)
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
