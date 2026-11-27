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
//
// Güncel Android'e göre her kartta bir ödül satırı vardır: ilerleme çubuğu ("250 / 300") ve çubuğun
// sonunda sıradaki eşiğin sandığı (cardNRewardZone / cardNRewardChest). Çubuğa dokunmak Kupa Yolu
// (Trophy Road) ekranını açar (`yolAc`); hazır sandığa dokunmak sandığı açar (`sandikAc`; sandık
// ekranı dışarıda, `Sandik/` içindedir). Panel ikisinde de kapanır (Android dismissCupPathPanel).
// Kilitli karta dokunmak "önce şu bölümü bitir" uyarısını gösterir (LockedHint).

struct KupaEkrani: View {
    private let kapat: (() -> Void)?
    private let kupaDersiBaslat: ((KupaDersBaslangici) -> Void)?
    private let magazaAc: (() -> Void)?
    private let sonuc: KupaDersSonucu?
    private let yolAc: ((String) -> Void)?
    private let sandikAc: ((SandikGirdisi) -> Void)?

    @StateObject private var model: KupaModeli
    /// Giriş animasyonu: panel alttan kayarak gelir.
    @State private var gorundu: Bool = false

    private let saniyeSayaci = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    /// - Parameters:
    ///   - kapat: panel dışına ya da tutamağa dokununca çağrılır; nil ise panel kapatılamaz.
    ///   - kupaDersiBaslat: BAŞLAT'a basınca ders parametreleriyle çağrılır.
    ///   - magazaAc: can göstergesine basınca ya da can yokken BAŞLAT'a basınca çağrılır.
    ///   - sonuc: kupa dersinden dönüşte verilir; ilgili kartın skoru eski değerden yenisine sayar.
    ///   - yolAc: karttaki ödül çubuğuna (ya da hazır olmayan sandığa) dokununca, panel kapandıktan sonra
    ///     çağrılır; kupa alanı verilir (Android: openCupPathRoad → CupPathRoadFragment). Ekran: `KupaYoluOdulEkrani`.
    ///   - sandikAc: hazır sandığa dokununca, panel kapandıktan sonra çağrılır (Android: NewChestFragment
    ///     kupa yolu sandığı kipi). Girdi `Sandik/` ekranına verilir.
    ///   - ortam: veri servisleri (varsayılan: örnek veri).
    init(
        kapat: (() -> Void)?,
        kupaDersiBaslat: ((KupaDersBaslangici) -> Void)?,
        magazaAc: (() -> Void)? = nil,
        sonuc: KupaDersSonucu? = nil,
        yolAc: ((String) -> Void)? = nil,
        sandikAc: ((SandikGirdisi) -> Void)? = nil,
        ortam: KupaOrtami = KupaOrtami.sahte
    ) {
        self.kapat = kapat
        self.kupaDersiBaslat = kupaDersiBaslat
        self.magazaAc = magazaAc
        self.sonuc = sonuc
        self.yolAc = yolAc
        self.sandikAc = sandikAc
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
            if let ipucu = model.kilitIpucu {
                KupaKilitIpucu(metin: ipucu)
                    .transition(.opacity)
                    .zIndex(4)
            }
        }
        .animation(.linear(duration: 0.3), value: model.zorlukPaneli != nil)
        .animation(.easeInOut(duration: 0.2), value: model.kilitIpucu != nil)
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
                sonsuz: model.sonsuzEnerji,
                pro: model.proPlan,
                enerjiyeDokunuldu: { magazaAc?() }
            )
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 10) {
                    ForEach(model.kartlar) { kart in
                        kartGorunumu(kart)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.top, 12)
                // Son kartın alt boşluğu (10dp) + NestedScrollView alt dolgusu (24dp).
                .padding(.bottom, 34)
            }
        }
        .background(
            KupaUstYuvarlakSekil(yaricap: 16)
                .fill(Color("background_color"))
                .ignoresSafeArea(edges: .bottom)
        )
        .padding(.top, 48)
    }

    private func kartGorunumu(_ kart: KupaKartDurumu) -> some View {
        return KupaYolKarti(
            kart: kart,
            yukleniyor: model.yukleniyor,
            dokunuldu: { model.kartaDokunuldu(kart) },
            yolaDokunuldu: { yoluAc(kart) },
            sandigaDokunuldu: { sandigaDokunuldu(kart) }
        )
    }

    /// Android onCupPathChestTapped / zone tıklaması: panel kapanır, Kupa Yolu ekranı açılır.
    private func yoluAc(_ kart: KupaKartDurumu) {
        if kart.odul == nil { return }
        kapat?()
        yolAc?(kart.alan)
    }

    /// Karttaki sandığa dokunuldu: hazırsa panel kapanır ve sandık açılır, değilse Kupa Yolu ekranı.
    private func sandigaDokunuldu(_ kart: KupaKartDurumu) {
        if kart.odul == nil { return }
        guard let girdi = model.sandikGirdisi(kart: kart) else {
            yoluAc(kart)
            return
        }
        kapat?()
        sandikAc?(girdi)
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
///
/// Can göstergesi (Android EnergyDisplay.apply): 22dp kalp (Pro planda `heart_pro_ic`); sonsuz enerjide
/// kalbin ÜZERİNDE beyaz "∞" (13sp) görünür ve yanındaki sayı boşalır.
private struct KupaPanelBasligi: View {
    let baslik: String
    let enerjiMetni: String
    let sonsuz: Bool
    let pro: Bool
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
                        enerjiGostergesi
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

    private var enerjiGostergesi: some View {
        HStack(spacing: 4) {
            ZStack {
                Image(pro ? "heart_pro_ic" : "heart_ic")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 22, height: 22)
                if sonsuz {
                    Text("∞")
                        .font(Tema.yazi(13))
                        .foregroundColor(.white)
                }
            }
            .frame(width: 22, height: 22)
            Text(enerjiMetni)
                .font(Tema.yazi(15))
                .foregroundColor(KupaStil.renk(0xFF5252))
        }
    }
}

// MARK: - Kart

/// panel_cup_path.xml kartı + TasksFragment.setupCupPathCard görünüm kuralları.
///
/// Yerleşim (ConstraintLayout, 16dp dolgu): sol sütunda başlık, kupa satırı (6dp altında) ve ödül
/// satırı (10dp altında, sağdan 16dp boşluklu); sağ üstte 70dp hayvan animasyonu.
private struct KupaYolKarti: View {
    let kart: KupaKartDurumu
    let yukleniyor: Bool
    let dokunuldu: () -> Void
    let yolaDokunuldu: () -> Void
    let sandigaDokunuldu: () -> Void

    private var kilitRengi: Color { Color("lesson_locked") }
    private var zemin: Color { kart.acik ? Color(kart.acikRenkAdi) : Color("background_color") }
    private var yaziRengi: Color { kart.acik ? Color.white : kilitRengi }

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            solSutun
                .frame(maxWidth: .infinity, alignment: .leading)
                // Sandık hayvan animasyonunun üstüne taşar (Android: sandık elevation 6dp).
                .zIndex(1)
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
        .contentShape(RoundedRectangle(cornerRadius: 18))
        .onTapGesture(perform: dokunuldu)
    }

    private var solSutun: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(kart.baslik)
                .font(Tema.yazi(18))
                .foregroundColor(yaziRengi)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
            skorSatiri
                .padding(.top, 6)
            KupaOdulSatiri(
                odul: yukleniyor ? nil : kart.odul,
                yolaDokunuldu: yolaDokunuldu,
                sandigaDokunuldu: sandigaDokunuldu
            )
            .padding(.top, 10)
            .padding(.trailing, 16)
        }
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

// MARK: - Ödül satırı (çubuk + sandık)

/// cardNRewardZone + cardNRewardChest: 28dp (daily_question_progress_height) ilerleme çubuğu ve
/// çubuğun SONUNA oturan 60dp sandık. Çubuk günlük soru kartındakiyle aynı çizimdir
/// (daily_question_progress_track / _fill / _shine; hazırken altın dolgu ve parlama).
///
/// Sandığın merkezi çubuğun bittiği yerdedir (x) ve çizimdeki üst boşluğu telafi etmek için
/// cup_path_chest_center_offset (18dp) kadar alt payla çubuğun 9dp üstüne çıkar: merkez çubuğun üstünden
/// 5dp aşağıda, yani sandığın üst kenarı çubuğun 25dp üstünde. `odul` nil iken (veri yok / kartlar
/// yükleniyor) sandık lesson_locked rengine boyanır, çubuk boş kalır ve dokunma kapalıdır
/// (Android applyCupPathRewardLoadingState).
private struct KupaOdulSatiri: View {
    let odul: KupaOdulDurumu?
    let yolaDokunuldu: () -> Void
    let sandigaDokunuldu: () -> Void

    /// dimens.xml: daily_question_progress_height.
    private let yukseklik: CGFloat = 28
    /// dimens.xml: cup_path_chest_size.
    private let sandikBoyu: CGFloat = 60

    var body: some View {
        cubuk
            .frame(height: yukseklik)
            .contentShape(Rectangle())
            .onTapGesture {
                if odul != nil {
                    yolaDokunuldu()
                }
            }
            .overlay(alignment: .topTrailing) {
                sandik
                    // Merkez x = çubuğun sağ ucu; üst kenar çubuğun 25dp üstünde.
                    .offset(x: sandikBoyu / 2, y: -25)
            }
    }

    private var cubuk: some View {
        GeometryReader { geo in
            let genislik: CGFloat = geo.size.width
            let oran: CGFloat = odul == nil ? 0 : CGFloat(max(0.0, min(1.0, odul?.oran ?? 0)))
            let dolgu: CGFloat = (genislik * oran).rounded(.down)
            // mission_progress_shine_*: soldan 14dp, sağdan 10dp boşluk, yükseklik 6dp, üstten 8dp, köşe 3dp.
            let parlama: CGFloat = max(0, dolgu - 14 - 10)
            let hazir: Bool = odul?.hazir ?? false
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color("daily_question_progress_track"))
                    .frame(width: genislik, height: yukseklik)
                if dolgu > 0 {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(hazir ? "missions_progress_complete" : "daily_question_progress_fill"))
                        .frame(width: dolgu, height: yukseklik)
                }
                if parlama > 0 {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color(hazir ? "missions_progress_gold_shine" : "daily_question_progress_shine"))
                        .frame(width: parlama, height: 6)
                        .offset(x: 14, y: 8)
                }
                if let etiket = odul?.etiket {
                    // Metin ortalı ama sağdan 32dp dolgulu (sandığa yer bırakır).
                    Text(etiket)
                        .font(Tema.yazi(13))
                        .foregroundColor(.white)
                        // Android'den BİLİNÇLİ SAPMA: Android'de etiket gölgesizdir ama altın/açık zemin
                        // üstünde okunmuyor; görev çubuğundaki gibi koyu gölge eklendi.
                        .shadow(color: Color.black.opacity(0.9), radius: 2, x: 0, y: 1)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                        .padding(.trailing, 32)
                        .frame(width: genislik, height: yukseklik, alignment: .center)
                }
            }
            .frame(width: genislik, height: yukseklik, alignment: .topLeading)
        }
    }

    @ViewBuilder
    private var sandik: some View {
        if let odul = odul {
            Image(odul.sandikResmi)
                .resizable()
                .scaledToFit()
                .frame(width: sandikBoyu, height: sandikBoyu)
                .contentShape(Rectangle())
                .onTapGesture(perform: sandigaDokunuldu)
        } else {
            // Yükleme hâli: kartlardaki gri (lesson_locked), PorterDuff SRC_IN = düz renk.
            Image("new_chest_close_ic1")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color("lesson_locked"))
                .frame(width: sandikBoyu, height: sandikBoyu)
                .allowsHitTesting(false)
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
