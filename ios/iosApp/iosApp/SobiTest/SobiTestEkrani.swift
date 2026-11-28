import Shared
import SwiftUI

// Sobi testinin oynatıldığı ekran (Android'de ayrı ekran: ödev oynatıcısının Sobi testi kipi).
//
// Android kaynakları: homework/HomeworkPlayerFragment.kt, res/layout/fragment_homework_player.xml,
// drawable/bg_flash_card.xml (bright_background_color yüz, 2 kenar #37464F, köşe 22, 4 dudak #37464F),
// bg_depth_hw_control.xml (Başla / Kontrol: #8FD14F / #62A12E, pasif #CCCCCC / #A6A6A6, köşe 10),
// bottom_sheet_background.xml (#222, üst köşeler 16), bg_depth_tutorial_continue / _okay.xml (Devam:
// #8CC63F / #62982A, yanlışta #FF4B4B / #D12F2F, köşe 15), bg_hw_send.xml ("Sobi ile analiz et"),
// bg_add_friend_btn.xml (özetteki "Sandığını aç": #1CB0F6, köşe 12), bg_edit_profile_input.xml (cevap kutusu).
//
// Yerleşim (dp → pt): çıkış (48, kenarlardan 10) ve ortada adım yazısı (14 sp kalın, ikincil); 12 altta
// sayı kartı (140 yükseklik, yanlardan 24; yazı 56 sp, uzun sayıda 0,75 / 0,6 / 0,5); altta abaküs
// (Kontrol'ün 10 üstünde) ya da abaküssüzde cevap kutusu (220 × 64, 26 sp, Kontrol'ün 20 üstünde);
// Kontrol (56, yanlardan 16, alttan 16; 17 sp). Sağda sıfırla ve atla (48, side_button_background).
// Sonuç paneli alttan kayar (iç boşluk 16; başlık 18 sp yeşil #8CC63F / kırmızı #FF4B4B; yanlışta işlem
// 16 sp beyaz, 6 üstte; "Sobi ile analiz et" 44, 10 üstte; Devam 60, 16 üstte, siyah 18 sp).
// Özet: background_color tam ekran, iç boşluk 32, ortalı; başlık 24 sp, skor 20 sp #8CC63F (10 üstte),
// ileti 15 sp ikincil (8 üstte), "Sandığını aç" 52 (28 üstte, beyaz 16 sp).
//
// Android'de olup burada olmayanlar: kural kitabı / sihirli değnek paneli ve abaküs ayarı (boyut) düğmesi,
// sıfırla / atla Lottie oynatması (simgeler bekleme karesinde durur), geçiş reklamı kapısı, görev sayaçları
// (MissionsProgressStore), sandık isteğinin önceden yollanması (ServerRewards.prefetchChest).
struct SobiTestEkrani: View {
    @StateObject private var model: SobiTestOynaticiModeli
    private let sobiAnalizi: AbakusSobiAnalizBaglantisi?
    private let bitti: (SobiTestBitis) -> Void
    private let kapat: () -> Void
    @State private var cikisSor: Bool = false

    /// - Parameters:
    ///   - baslangic: panelin kurduğu plan.
    ///   - sobiAnalizi: yanlışta "Sobi ile analiz et" (nil: düğme yok).
    ///   - bitti: özetteki "Sandığını aç".
    ///   - kapat: çıkış (yarıdaysa onaydan sonra).
    init(
        baslangic: SobiTestBaslangici,
        sobiAnalizi: AbakusSobiAnalizBaglantisi? = nil,
        bitti: @escaping (SobiTestBitis) -> Void,
        kapat: @escaping () -> Void
    ) {
        _model = StateObject(wrappedValue: SobiTestOynaticiModeli(baslangic: baslangic))
        self.sobiAnalizi = sobiAnalizi
        self.bitti = bitti
        self.kapat = kapat
    }

    /// Hazır modelle (ekran galerisi).
    init(model: SobiTestOynaticiModeli, bitti: @escaping (SobiTestBitis) -> Void, kapat: @escaping () -> Void) {
        _model = StateObject(wrappedValue: model)
        self.sobiAnalizi = nil
        self.bitti = bitti
        self.kapat = kapat
    }

    var body: some View {
        let tutamak: SobiTestOynaticiModeli = model
        let cikis: () -> Void = kapat
        return ZStack(alignment: .bottom) {
            Color("background_color").ignoresSafeArea()
            SobiTestOyunAlani(model: model, cikisIstendi: { cikisIste() })
            SobiTestPanelKatmani(model: model, analizIste: analizIste())
            SobiTestOzetKatmani(model: model, sandikAc: {
                tutamak.kapanis()
                bitti(.sandik)
            })
        }
        .onAppear {
            DispatchQueue.main.async {
                tutamak.baslat()
            }
        }
        .onDisappear { tutamak.kapanis() }
        .alert(Metin.al("sobi_test_quit_title"), isPresented: $cikisSor) {
            Button(Metin.al("hw_player_quit_confirm"), role: .destructive) {
                tutamak.kapanis()
                cikis()
            }
            Button(Metin.al("class_cancel"), role: .cancel) {}
        } message: {
            Text(Metin.al("sobi_test_quit_message"))
        }
    }

    /// Android requestQuit: yarıdaysa onay (cevaplar kaydedilmez; yeniden açınca aynı sorular).
    private func cikisIste() {
        if model.yarida {
            cikisSor = true
        } else {
            model.kapanis()
            kapat()
        }
    }

    /// Analiz düğmesinin eylemi; bağlantı yoksa ya da bu soru analiz edilemiyorsa nil.
    private func analizIste() -> (() -> Void)? {
        guard let baglanti = sobiAnalizi else { return nil }
        let tutamak: SobiTestOynaticiModeli = model
        return {
            if baglanti.yapilabilir(tutamak.analizSayilari()) {
                baglanti.istendi(tutamak.analizIstegi())
            }
        }
    }
}

/// Üst çubuk, sayı kartı, abaküs / cevap kutusu, Kontrol ve sağ düğmeler.
struct SobiTestOyunAlani: View {
    @ObservedObject var model: SobiTestOynaticiModeli
    let cikisIstendi: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            SobiTestUstCubuk(adim: model.adimMetni, cikis: cikisIstendi)
            SobiTestSayiKarti(metin: model.tahta)
                .padding(.horizontal, 24)
                .padding(.top, 12)
            Spacer(minLength: 8)
            SobiTestAltAlan(model: model)
            SobiTestKontrolDugmesi(metin: model.dugmeMetni, etkin: model.dugmeEtkin, eylem: { model.anaDugme() })
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
        }
    }
}

struct SobiTestUstCubuk: View {
    let adim: String
    let cikis: () -> Void

    var body: some View {
        ZStack {
            Text(adim)
                .font(Tema.yazi(14))
                .foregroundColor(SobiTestRenk.ikincilYazi)
            HStack(spacing: 0) {
                Button(action: cikis) {
                    Image("cross")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 48, height: 48)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(Metin.al("hw_player_quit_desc")))
                Spacer(minLength: 0)
            }
            .padding(.leading, 10)
        }
        .frame(height: 48)
        .padding(.top, 10)
    }
}

/// bg_flash_card + flashText (56 sp; uzun metinde küçülür).
struct SobiTestSayiKarti: View {
    let metin: String

    private var boyut: CGFloat {
        let uzunluk: Int = metin.count
        let oran: CGFloat
        if uzunluk <= 5 {
            oran = 1
        } else if uzunluk <= 8 {
            oran = 0.75
        } else if uzunluk <= 11 {
            oran = 0.6
        } else {
            oran = 0.5
        }
        return 56 * oran
    }

    var body: some View {
        ZStack(alignment: .top) {
            RoundedRectangle(cornerRadius: 22)
                .fill(SobiTestRenk.kenar)
                .padding(.top, 4)
            RoundedRectangle(cornerRadius: 22)
                .fill(Color("bright_background_color"))
                .overlay(RoundedRectangle(cornerRadius: 22).strokeBorder(SobiTestRenk.kenar, lineWidth: 2))
                .padding(.bottom, 4)
            Text(metin)
                .font(Tema.yazi(boyut))
                .foregroundColor(SobiTestRenk.yazi)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.bottom, 8)
        }
        .frame(height: 140)
        .environment(\.layoutDirection, .leftToRight)
    }
}

/// Abaküs (abaküslü soruda) ya da cevap kutusu; sağda sıfırla ve atla.
struct SobiTestAltAlan: View {
    @ObservedObject var model: SobiTestOynaticiModeli
    @FocusState private var odak: Bool

    var body: some View {
        ZStack(alignment: .trailing) {
            orta
                .frame(maxWidth: .infinity)
            SobiTestYanDugmeleri(atlaGorunur: model.atlaGorunur, sifirla: { model.sifirla() }, atla: { model.atla() })
                .padding(.trailing, 4)
        }
        .onChange(of: model.cevapKutusu) { (acik: Bool) in
            odak = acik
        }
    }

    @ViewBuilder
    private var orta: some View {
        if model.abakusGorunur {
            abakus
                .padding(.bottom, 10)
        } else if model.cevapKutusu {
            cevapKutusu
                .padding(.bottom, 20)
        } else {
            Color.clear.frame(height: 84)
        }
    }

    private var abakus: some View {
        let tutamak: SobiTestOynaticiModeli = model
        return AbakusGorunumu(
            denetleyici: model.abakus,
            ses: GercekAbakusBoncukSesi.ortak,
            degerDegisti: { (yeni: Int) in tutamak.abakusDegisti(yeni) }
        )
    }

    private var cevapKutusu: some View {
        TextField(Metin.al("hw_player_answer_hint"), text: $model.cevapMetni)
            .keyboardType(.numberPad)
            .focused($odak)
            .font(Tema.yazi(26))
            .foregroundColor(SobiTestRenk.yazi)
            .multilineTextAlignment(.center)
            .frame(width: 220, height: 64)
            .background(RoundedRectangle(cornerRadius: 12).fill(Color("bright_background_color")))
            .environment(\.layoutDirection, .leftToRight)
            .onSubmit { model.anaDugme() }
    }
}

/// Sıfırla ve atla (48, side_button_background daire; Android'de Lottie, burada bekleme karesi).
struct SobiTestYanDugmeleri: View {
    let atlaGorunur: Bool
    let sifirla: () -> Void
    let atla: () -> Void

    var body: some View {
        VStack(spacing: 4) {
            yanDugme(animasyon: "reset", kare: 20, eylem: sifirla)
            if atlaGorunur {
                yanDugme(animasyon: "skip_step_arrow_anim", kare: 10, eylem: atla)
            }
        }
    }

    private func yanDugme(animasyon: String, kare: Double, eylem: @escaping () -> Void) -> some View {
        Button(action: eylem) {
            ZStack {
                Circle().fill(Color("side_button_background"))
                LottieGorunumu(animasyon, dongu: false, hiz: 1.0, oynat: false, bitti: nil, baslangicKare: kare, bitisKare: nil)
                    .frame(width: 40, height: 40)
            }
            .frame(width: 48, height: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Başla / Kontrol (bg_depth_hw_control).
struct SobiTestKontrolDugmesi: View {
    let metin: String
    let etkin: Bool
    let eylem: () -> Void

    var body: some View {
        let zemin: Color = etkin ? SobiTestRenk.yesil : SobiTestRenk.pasif
        let golge: Color = etkin ? SobiTestRenk.yesilDudak : SobiTestRenk.pasifDudak
        let yazi: Color = etkin ? Color("button_text_enabled") : Color("button_text_disabled")
        return Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(17))
                .foregroundColor(yazi)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(GocenDugmeStili(zemin: zemin, golge: golge, koseYaricapi: 10, derinlik: 4))
        .frame(height: 56)
        .disabled(!etkin)
    }
}

// MARK: - Sonuç paneli

struct SobiTestPanelKatmani: View {
    @ObservedObject var model: SobiTestOynaticiModeli
    let analizIste: (() -> Void)?

    var body: some View {
        ZStack(alignment: .bottom) {
            if let panel = model.panel {
                SobiTestCevapPaneliGorunumu(
                    panel: panel,
                    analizIste: panel.analiz ? analizIste : nil,
                    devam: { model.devam() }
                )
                .transition(AnyTransition.move(edge: .bottom).combined(with: AnyTransition.opacity))
            }
        }
        .animation(Animation.easeInOut(duration: 0.2), value: model.panel)
    }
}

struct SobiTestCevapPaneliGorunumu: View {
    let panel: SobiTestCevapPaneli
    let analizIste: (() -> Void)?
    let devam: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(panel.baslik)
                .font(Tema.yazi(18))
                .foregroundColor(panel.dogru ? SobiTestRenk.devamYesil : SobiTestRenk.kirmizi)
            if let mesaj = panel.mesaj {
                Text(mesaj)
                    .font(Tema.yazi(16))
                    .foregroundColor(.white)
                    .padding(.top, 6)
                    .environment(\.layoutDirection, .leftToRight)
            }
            if let analiz = analizIste {
                SobiTestAnalizDugmesi(eylem: analiz)
                    .padding(.top, 10)
            }
            Button(action: devam) {
                Text(Metin.al("hw_player_next"))
                    .font(Tema.yazi(18))
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(GocenDugmeStili(
                zemin: panel.dogru ? SobiTestRenk.devamYesil : SobiTestRenk.kirmizi,
                golge: panel.dogru ? SobiTestRenk.devamDudak : SobiTestRenk.kirmiziDudak,
                koseYaricapi: 15,
                derinlik: 4
            ))
            .frame(height: 60)
            .padding(.top, 16)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            SobiTestUstKoseler(yaricap: 16)
                .fill(SobiTestRenk.panel)
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

/// "Sobi ile analiz et": Sobi kafası (30) + metin (15 sp beyaz), bg_hw_send, 44 yükseklik.
struct SobiTestAnalizDugmesi: View {
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 6) {
                SobiTestKafasi(boyut: 30)
                Text(Metin.al("review_open_button"))
                    .font(Tema.yazi(15))
                    .foregroundColor(.white)
            }
            .padding(.leading, 8)
            .padding(.trailing, 14)
        }
        .buttonStyle(GocenDugmeStili(zemin: SobiTestRenk.mavi, golge: SobiTestRenk.maviDudak, koseYaricapi: 16, derinlik: 4))
        .frame(height: 44)
        .fixedSize(horizontal: true, vertical: false)
    }
}

// MARK: - Özet

struct SobiTestOzetKatmani: View {
    @ObservedObject var model: SobiTestOynaticiModeli
    let sandikAc: () -> Void

    var body: some View {
        if let ozet = model.ozet {
            SobiTestOzetGorunumu(ozet: ozet, sandikAc: sandikAc)
                .transition(AnyTransition.opacity)
        }
    }
}

/// Android summaryPanel (Sobi testi: "Sobi'nin testi bitti!", skor, kural satırları, "Sandığını aç").
struct SobiTestOzetGorunumu: View {
    let ozet: SobiTestOzet
    let sandikAc: () -> Void
    @State private var basildi: Bool = false

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            ScrollView(.vertical, showsIndicators: false) {
                icerik
                    .padding(32)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var icerik: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 40)
            Text(Metin.al("sobi_test_done_title"))
                .font(Tema.yazi(24))
                .foregroundColor(SobiTestRenk.yazi)
                .multilineTextAlignment(.center)
            Text(ozet.skor)
                .font(Tema.yazi(20))
                .foregroundColor(SobiTestRenk.devamYesil)
                .padding(.top, 10)
            Text(ozet.mesaj)
                .font(Tema.yazi(15))
                .foregroundColor(SobiTestRenk.ikincilYazi)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
            Button(action: {
                if basildi { return }
                basildi = true
                sandikAc()
            }) {
                Text(Metin.al("sobi_test_open_chest"))
                    .font(Tema.yazi(16))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(RoundedRectangle(cornerRadius: 12).fill(SobiTestRenk.mavi))
            }
            .buttonStyle(MagazaBasmaStili())
            .padding(.top, 28)
        }
    }
}
