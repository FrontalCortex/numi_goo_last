import Combine
import SwiftUI
import UIKit

// Kupa Yolu ekranı.
//
// Android karşılığı: Görevler sekmesindeki "Kupa Yolu" satırına basınca açılan alt panel
// (TasksFragment.showCupPathPanel → res/layout/panel_cup_path.xml) ve bir karta basınca açılan
// zorluk penceresi (showCupDifficultyPanel → res/layout/panel_cup_difficulty.xml).
// Altı kart vardır; her kart kendi kupa skorunu gösterir ve ilgili part'ın son sandığı
// bitince açılır. BAŞLAT'a basınca ders parametreleri `kupaDersiBaslat` ile dışarı verilir.

struct KupaEkrani: View {
    private let kapat: (() -> Void)?
    private let kupaDersiBaslat: ((KupaDersBaslangici) -> Void)?
    private let magazaAc: (() -> Void)?
    private let sonuc: KupaDersSonucu?

    @StateObject private var model: KupaModeli
    /// Giriş animasyonu: panel alttan kayarak gelir.
    @State private var gorundu: Bool = false

    private let saniyeSayaci = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    /// - Parameters:
    ///   - kapat: panel dışına ya da tutamağa dokununca çağrılır; nil ise panel kapatılamaz.
    ///   - kupaDersiBaslat: BAŞLAT'a basınca ders parametreleriyle çağrılır.
    ///   - magazaAc: can göstergesine basınca ya da can yokken BAŞLAT'a basınca çağrılır.
    ///   - sonuc: kupa dersinden dönüşte verilir; ilgili kartın skoru eski değerden yenisine sayar.
    ///   - ortam: veri servisleri (varsayılan: örnek veri).
    init(
        kapat: (() -> Void)?,
        kupaDersiBaslat: ((KupaDersBaslangici) -> Void)?,
        magazaAc: (() -> Void)? = nil,
        sonuc: KupaDersSonucu? = nil,
        ortam: KupaOrtami = KupaOrtami.sahte
    ) {
        self.kapat = kapat
        self.kupaDersiBaslat = kupaDersiBaslat
        self.magazaAc = magazaAc
        self.sonuc = sonuc
        _model = StateObject(wrappedValue: KupaModeli(ortam: ortam))
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            arkaKarartma
            panel
                .offset(y: gorundu ? 0 : UIScreen.main.bounds.height)
            if model.zorlukPaneli != nil {
                zorlukKarartmasi
                    .transition(.opacity)
                    .zIndex(2)
            }
            if let durum = model.zorlukPaneli {
                zorlukKatmani(durum)
                    .transition(zorlukGecisi)
                    .zIndex(3)
            }
        }
        .animation(.linear(duration: 0.3), value: model.zorlukPaneli != nil)
        .onAppear {
            model.yukle()
            model.sonucuIsle(sonuc)
            // Android BottomSheetDialog: panel alttan kayarak gelir.
            withAnimation(.easeOut(duration: 0.3)) {
                gorundu = true
            }
        }
        .onChange(of: sonuc) { yeni in
            model.sonucuIsle(yeni)
        }
        .onReceive(saniyeSayaci) { _ in
            model.enerjiyiYenile()
        }
    }

    private var arkaKarartma: some View {
        Color.black.opacity(0.6)
            .ignoresSafeArea()
            .onTapGesture {
                kapat?()
            }
    }

    private var panel: some View {
        VStack(spacing: 0) {
            KupaPanelBasligi(
                baslik: model.panelBasligi,
                enerjiMetni: model.enerjiMetni,
                enerjiyeDokunuldu: { magazaAc?() }
            )
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 10) {
                    ForEach(model.kartlar) { kart in
                        KupaYolKarti(
                            kart: kart,
                            yukleniyor: model.yukleniyor,
                            dokunuldu: { model.kartaDokunuldu(kart) }
                        )
                    }
                }
                .padding(.horizontal, 12)
                .padding(.top, 12)
                .padding(.bottom, 24)
            }
        }
        .background(
            KupaUstYuvarlakSekil(yaricap: 16)
                .fill(Color("background_color"))
                .ignoresSafeArea(edges: .bottom)
        )
        .padding(.top, 48)
    }

    /// Android AlertDialog'un arka karartması; dışına dokununca pencere kapanır.
    private var zorlukKarartmasi: some View {
        Color.black.opacity(0.6)
            .ignoresSafeArea()
            .onTapGesture {
                model.zorlukPaneliniKapat()
            }
    }

    /// Android R.style.DialogAnimationSlideLeft: soldan girer (slide_in_left), sağa çıkar
    /// (slide_out_right); ikisi de 300 ms.
    private var zorlukGecisi: AnyTransition {
        return AnyTransition.asymmetric(
            insertion: AnyTransition.move(edge: .leading),
            removal: AnyTransition.move(edge: .trailing)
        )
    }

    private func zorlukKatmani(_ durum: KupaZorlukPaneliDurumu) -> some View {
        KupaZorlukPaneli(
            durum: durum,
            model: model,
            baslat: baslataBasildi
        )
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func baslataBasildi() {
        switch model.baslat() {
        case .bekle:
            break
        case .enerjiYok:
            magazaAc?()
        case .basla(let baslangic):
            kupaDersiBaslat?(baslangic)
        }
    }
}

// MARK: - Panel başlığı

/// panel_cup_path.xml başlığı: tutamak, "Kupa Yolu" ve sağda can göstergesi.
private struct KupaPanelBasligi: View {
    let baslik: String
    let enerjiMetni: String
    let enerjiyeDokunuldu: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Rectangle()
                .fill(Color.white.opacity(0.5))
                .frame(width: 32, height: 4)
            ZStack {
                Text(baslik)
                    .font(Tema.yazi(20))
                    .foregroundColor(.white)
                HStack(spacing: 4) {
                    Spacer(minLength: 0)
                    Button(action: enerjiyeDokunuldu) {
                        HStack(spacing: 4) {
                            Image("heart_ic")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                            Text(enerjiMetni)
                                .font(Tema.yazi(15))
                                .foregroundColor(KupaStil.renk(0xFF5252))
                        }
                    }
                    .buttonStyle(.plain)
                    .padding(.trailing, 8)
                }
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 16)
        .padding(.horizontal, 16)
    }
}

// MARK: - Kart

/// panel_cup_path.xml kartı + TasksFragment.setupCupPathCard görünüm kuralları.
private struct KupaYolKarti: View {
    let kart: KupaKartDurumu
    let yukleniyor: Bool
    let dokunuldu: () -> Void

    private var kilitRengi: Color { Color("lesson_locked") }
    private var zemin: Color { kart.acik ? Color(kart.acikRenkAdi) : Color("background_color") }
    private var yaziRengi: Color { kart.acik ? Color.white : kilitRengi }

    var body: some View {
        Button(action: dokunuldu) {
            HStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(kart.baslik)
                        .font(Tema.yazi(18))
                        .foregroundColor(yaziRengi)
                        .multilineTextAlignment(.leading)
                    skorSatiri
                }
                Spacer(minLength: 8)
                // Android setupCupPathCard: açık kartta döngüyle oynar; kilitli kartta ilk karede
                // durur ve lesson_locked rengine boyanır.
                KupaAnimasyonGorunumu(
                    dosya: kart.animasyonDosyasi,
                    duzRenk: kart.acik ? nil : kilitRengi,
                    dongu: kart.acik,
                    oynat: kart.acik
                )
                .frame(width: 70, height: 70)
                .id(kart.acik)
            }
            .padding(16)
            .frame(maxWidth: .infinity)
            .background(RoundedRectangle(cornerRadius: 18).fill(zemin))
            .shadow(color: Color.black.opacity(0.25), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(.plain)
        .disabled(!kart.acik || yukleniyor)
    }

    private var skorSatiri: some View {
        HStack(spacing: 5) {
            kupaSimgesi
            Text(String(kart.gosterilenSkor))
                .font(Tema.yazi(15))
                .foregroundColor(yaziRengi)
                .opacity(yukleniyor || kart.skor == nil ? 0 : 1)
            if let delta = kart.deltaMetni {
                Text(delta)
                    .font(Tema.yazi(15))
                    .foregroundColor(KupaStil.renk(kart.deltaRengi))
            }
        }
    }

    @ViewBuilder
    private var kupaSimgesi: some View {
        if kart.acik {
            Image("infinity_cup_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 20, height: 20)
        } else {
            Image("infinity_cup_ic")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(kilitRengi)
                .frame(width: 20, height: 20)
        }
    }
}

// MARK: - Zorluk paneli

/// panel_cup_difficulty.xml: başlık, kapat, hayvan animasyonu, zorluk kaydırıcısı,
/// (3000+ kupada) basamak seçimi ve BAŞLAT.
private struct KupaZorlukPaneli: View {
    let durum: KupaZorlukPaneliDurumu
    @ObservedObject var model: KupaModeli
    let baslat: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            baslikSatiri
            // cupDifficultyAnim: 120dp, lottie_autoPlay + lottie_loop.
            KupaAnimasyonGorunumu(dosya: durum.kart.animasyonDosyasi, dongu: true)
                .frame(width: 120, height: 120)
                // Android'de animasyon başlığın (40dp'lik satırda ortalı ~32dp) altına 8dp bağlıdır.
                .padding(.top, 4)
            if durum.kaydiriciGorunur {
                Text(model.zorlukIletisi)
                    .font(Tema.yazi(16))
                    .foregroundColor(KupaStil.renk(0xFFECB3))
                    .padding(.top, 16)
                KupaZorlukKaydiricisi(
                    deger: $model.zorluk,
                    enBuyuk: model.kaydiriciEnBuyuk,
                    etiket: model.zorlukYuzdesi
                )
                .padding(.top, 24)
                .padding(.horizontal, 16)
            }
            if !durum.basamakSecenekleri.isEmpty {
                basamakSatiri
                    .padding(.top, 20)
                    .padding(.horizontal, 16)
            }
            baslatDugmesi
                .padding(.top, 24)
        }
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 18).fill(Color("panel_background")))
    }

    private var baslikSatiri: some View {
        HStack(spacing: 0) {
            Text(durum.baslik)
                .font(Tema.yazi(20))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 8)
            Button(action: { model.zorlukPaneliniKapat() }) {
                Image("ic_close_black")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(.white)
                    .padding(8)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }

    private var basamakSatiri: some View {
        HStack(spacing: 12) {
            Text(model.basamakEtiketi)
                .font(Tema.yazi(16))
                .foregroundColor(.white)
            Menu {
                Button(model.basamakSecenekEtiketi(0)) {
                    model.basamakSec(0)
                }
                ForEach(durum.basamakSecenekleri, id: \.self) { basamak in
                    Button(model.basamakSecenekEtiketi(basamak)) {
                        model.basamakSec(basamak)
                    }
                }
            } label: {
                HStack(spacing: 0) {
                    Text(model.basamakSecenekEtiketi(durum.secilenBasamak))
                        .font(Tema.yazi(16, kalin: false))
                        .foregroundColor(.white)
                    Spacer(minLength: 8)
                    Image(systemName: "arrowtriangle.down.fill")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 10, height: 10)
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .contentShape(Rectangle())
            }
        }
    }

    private var baslatDugmesi: some View {
        let etkin: Bool = durum.kupaSkoru != nil
        return Button(action: baslat) {
            // MaterialButton: 60dp yerleşim yüksekliğinin üstten ve alttan 6dp'si boşluktur
            // (varsayılan insetTop/insetBottom); görünen dolgu 48dp. backgroundTint tek renk
            // olduğu için düğme kapalıyken de aynı renkte kalır.
            Text(model.baslatEtiketi)
                .font(Tema.yazi(18))
                .foregroundColor(.black)
                .padding(.horizontal, 24)
                .frame(minWidth: 160)
                .frame(height: 48)
                .background(
                    RoundedRectangle(cornerRadius: 15)
                        .fill(Color("button_enabled"))
                )
                .padding(.vertical, 6)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!etkin)
    }
}

/// Beş duraklı kaydırıcı (0..4) ve başparmağın üstünde duran yüzde etiketi.
private struct KupaZorlukKaydiricisi: View {
    @Binding var deger: Double
    let enBuyuk: Double
    let etiket: String

    /// UISlider başparmağının yaklaşık genişliği; etiketi başparmağın ortasına hizalamak için.
    private let basparmakGenisligi: CGFloat = 27

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            GeometryReader { geo in
                let izGenisligi: CGFloat = max(0, geo.size.width - basparmakGenisligi)
                let oran: CGFloat = enBuyuk > 0 ? CGFloat(deger / enBuyuk) : 0
                let merkez: CGFloat = basparmakGenisligi / 2 + izGenisligi * oran
                Text(etiket)
                    .font(Tema.yazi(12))
                    .foregroundColor(.white)
                    .fixedSize()
                    .position(x: merkez, y: 8)
            }
            .frame(height: 16)
            Slider(value: $deger, in: 0...enBuyuk, step: 1)
                .tint(Color("button_enabled"))
        }
    }
}
