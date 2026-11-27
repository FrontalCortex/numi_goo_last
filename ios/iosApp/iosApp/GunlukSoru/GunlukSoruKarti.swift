import SwiftUI

// Günlük soru kartı (item_bulletin_daily_question_card.xml) ve "Mücadeleye devam et" paneli
// (panel_daily_question_continue.xml).

// MARK: - Kart

struct GunlukSoruKarti: View {
    @ObservedObject var model: GunlukSoruModeli

    var body: some View {
        ZStack(alignment: .trailing) {
            icerik
            // Kalp ve ok kartın dikey ortasında, sağda (kalp okun solunda, 6dp arayla).
            HStack(alignment: .center, spacing: 6) {
                GunlukSoruKalpGorunumu(durum: model.kalp)
                    .frame(width: 28, height: 28)
                GunlukSoruOk()
            }
            .allowsHitTesting(false)
        }
        .padding(16)
        .background(zemin)
        // cardElevation = 2dp
        .shadow(color: Color.black.opacity(0.25), radius: 2, x: 0, y: 1)
        .contentShape(RoundedRectangle(cornerRadius: 18))
        .onTapGesture {
            model.kartaDokunuldu()
        }
    }

    /// bg_daily_question_card.xml: açı 135° (sağ alttan sol üste), #FFB74D → #FF8F00, 18dp köşe.
    private var zemin: some View {
        RoundedRectangle(cornerRadius: 18)
            .fill(
                LinearGradient(
                    colors: [GunlukSoruStil.kartGecisBaslangic, GunlukSoruStil.kartGecisBitis],
                    startPoint: .bottomTrailing,
                    endPoint: .topLeading
                )
            )
    }

    private var icerik: some View {
        VStack(alignment: .leading, spacing: 0) {
            baslikSatiri
            Text(uniteMetni)
                .font(Tema.yazi(14))
                .foregroundColor(GunlukSoruStil.uniteRengi)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 8)
            GunlukSoruIlerlemeBolgesi(cizim: model.cubuk)
                .contentShape(Rectangle())
                .onTapGesture {
                    model.ilerlemeyeDokunuldu()
                }
                // Ödül alındıysa bölge tıklanmaz; dokunuş karta düşer.
                .allowsHitTesting(model.ilerlemeTiklanir)
                .padding(.top, 12)
            altSatir
                .padding(.top, 10)
        }
    }

    private var baslikSatiri: some View {
        HStack(alignment: .center, spacing: 6) {
            Image("target_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 28, height: 28)
            Text(Metin.al("daily_question_card_title"))
                .font(Tema.yazi(18))
                .foregroundColor(.white)
        }
        // Başlık satırı kalbin soluna kadar uzanır (kalp 28 + ara 6 + ok 16).
        .padding(.trailing, 56)
    }

    /// Android: ünite adı boşsa varsayılan alt başlık.
    private var uniteMetni: String {
        let unite: String = model.kart.uniteBasligi
        if unite.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return Metin.al("daily_question_card_subtitle_default")
        }
        return unite
    }

    private var altSatir: some View {
        HStack(alignment: .bottom, spacing: 8) {
            if let odak = model.kart.odakMetni {
                Text(Metin.al("daily_question_focus_format", odak))
                    .font(Tema.yazi(13))
                    .foregroundColor(GunlukSoruStil.acikSari)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                Spacer(minLength: 0)
            }
            Text(model.geriSayim)
                .font(Tema.yazi(12))
                .foregroundColor(GunlukSoruStil.acikSari)
                .lineLimit(1)
        }
    }
}

// MARK: - İlerleme bölgesi

/// 200×28dp ilerleme çubuğu ve sağ ucundaki sandık (48dp; sağdan 8dp taşar, 8dp yukarıda).
/// Dolgu ve parlama ölçüleri ortak koddan gelir.
struct GunlukSoruIlerlemeBolgesi: View {
    let cizim: GunlukSoruCubukCizimi

    private let genislik: CGFloat = 200
    private let yukseklik: CGFloat = 28

    var body: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(cizim.zeminRengi))
                .frame(width: genislik, height: yukseklik)
            if cizim.dolguGorunur {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(cizim.dolguRengi))
                    .frame(width: cizim.dolguGenisligi, height: yukseklik)
            }
            if cizim.parlamaGorunur {
                // mission_progress_shine_*: yükseklik 6dp, üstten 8dp, soldan 14dp, köşe 3dp.
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color(cizim.parlamaRengi))
                    .frame(width: cizim.parlamaGenisligi, height: 6)
                    .offset(x: 14, y: 8)
            }
            Text(cizim.etiket)
                .font(Tema.yazi(14))
                .foregroundColor(Color(cizim.etiketRengi))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .padding(.trailing, 14)
                .frame(width: genislik, height: yukseklik, alignment: .center)
        }
        .frame(width: genislik, height: yukseklik, alignment: .topLeading)
        .overlay(alignment: .topLeading) {
            // Sandığın merkezi: x = 200 + 8 - 24 = 184, y = 14 - 8 = 6.
            Image("new_chest_close_ic2")
                .resizable()
                .scaledToFit()
                .frame(width: 48, height: 48)
                .offset(x: 160, y: -18)
                .allowsHitTesting(false)
        }
    }
}

// MARK: - Kalp

/// Kartın sağındaki kalp (28dp). Android: LottieAnimationView, assets/broken_heart_anim.json
/// (60 kare/sn, 198 kare; 0. kare sağlam, 116. kare kırık), döngüsüz, kendiliğinden oynamaz.
/// Dört durum tek bir Lottie görünümünün kare parametreleriyle verilir (TasksFragment.kt,
/// DailyQuestionVH.bindBrokenHeart):
///  - sağlam (showBrokenHeartFrame(0)): 0. karede sabit;
///  - kırılıyor (playBrokenHeartBreakToHold116): 0 → 116 oynar ve 116. karede kalır;
///  - kırık (showBrokenHeartFrame(116)): 116. karede sabit;
///  - iyileşiyor (playBrokenHeartHealThenFirstFrame): 116 → son kare oynar; bitince model durumu
///    "sağlam" yapar (0. kare).
/// Durum geçişlerinin zamanını model tutar (GunlukSoruModeli.kalbiBagla).
struct GunlukSoruKalpGorunumu: View {
    let durum: GunlukSoruKalpDurumu

    /// DailyQuestionPeriod.BROKEN_HEART_HOLD_FRAME
    private static let tutmaKaresi: Double = 116

    private var oynat: Bool {
        return durum == .kiriliyor || durum == .iyilesiyor
    }

    private var baslangicKare: Double {
        switch durum {
        case .saglam, .kiriliyor:
            return 0
        case .kirik, .iyilesiyor:
            return GunlukSoruKalpGorunumu.tutmaKaresi
        }
    }

    /// nil: animasyonun son karesi.
    private var bitisKare: Double? {
        return durum == .kiriliyor ? GunlukSoruKalpGorunumu.tutmaKaresi : nil
    }

    var body: some View {
        LottieGorunumu(
            "broken_heart_anim",
            dongu: false,
            hiz: 1.0,
            oynat: oynat,
            bitti: nil,
            baslangicKare: baslangicKare,
            bitisKare: bitisKare
        )
    }
}

// MARK: - "Mücadeleye devam et" paneli

/// Paneli ekranın ortasında, karartılmış zeminin üstünde gösterir (Android: Dialog, genişlik
/// ekranın %88'i, dışına dokununca kapanır).
struct GunlukSoruDevamKatmani: View {
    let istek: GunlukSoruDevamIstegi
    let kapat: () -> Void
    let devam: () -> Void

    /// Android Dialog'un açılış animasyonu: panel hafifçe büyüyerek ve belirerek gelir.
    @State private var acik: Bool = false

    var body: some View {
        GeometryReader { alan in
            ZStack {
                Color.black.opacity(0.6)
                    .ignoresSafeArea()
                    .onTapGesture(perform: kapat)
                GunlukSoruDevamPaneli(kapat: kapat, devam: devam)
                    .frame(width: alan.size.width * 0.88)
                    .scaleEffect(acik ? 1.0 : 0.9)
                    .opacity(acik ? 1.0 : 0.0)
            }
            .frame(width: alan.size.width, height: alan.size.height)
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.2)) {
                acik = true
            }
        }
    }
}

/// panel_daily_question_continue.xml: zemin panel_background, 18dp köşe, iç boşluk 20dp.
struct GunlukSoruDevamPaneli: View {
    let kapat: () -> Void
    let devam: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ustSatir
            // Android: LottieAnimationView 120dp, bandaged_heart_anim.json, autoPlay + loop.
            LottieGorunumu("bandaged_heart_anim", dongu: true)
                .frame(width: 120, height: 120)
                .padding(.top, 8)
            Text(Metin.al("daily_question_continue_message"))
                .font(Tema.yazi(14))
                .foregroundColor(GunlukSoruStil.acikSari)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
            devamDugmesi
                .padding(.top, 20)
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(Color("panel_background"))
        )
    }

    /// Başlık (20sp, ortalı) ve sağ üstte kapatma düğmesi (40dp, simge 24dp).
    private var ustSatir: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(Metin.al("daily_question_continue_title"))
                .font(Tema.yazi(20))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 8)
            Button(action: kapat) {
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
            .accessibilityLabel(Text(Metin.al("daily_question_continue_close")))
        }
    }

    /// Yeşil düğme: anahtar simgesi (32dp) + "x1" (18sp, siyah); yükseklik 60dp, en az 160dp, köşe 15dp.
    /// MaterialButton'ın varsayılan insetTop/insetBottom değeri 6dp'dir: 60dp'lik düğmenin görünen
    /// zemini 48dp yüksekliğindedir.
    private var devamDugmesi: some View {
        Button(action: devam) {
            HStack(alignment: .center, spacing: 8) {
                Image("key")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 32, height: 32)
                Text(Metin.al("daily_question_continue_diamond"))
                    .font(Tema.yazi(18))
                    .foregroundColor(.black)
            }
            .padding(.horizontal, 24)
            .frame(minWidth: 160)
            .frame(height: 60)
            .background(
                RoundedRectangle(cornerRadius: 15)
                    .fill(Color("button_enabled"))
                    .padding(.vertical, 6)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(GunlukSoruKartBasmaStili())
    }
}

// MARK: - Soru yer tutucusu

/// Kör ders ekranı (Android: BlindingLessonFragment, günlük soru kipi) bağlanana dek gösterilen
/// yer tutucu: soruyu yazıyla gösterir ve sonucu elle seçtirir.
struct GunlukSoruYerTutucuSoru: View {
    let veri: GunlukSoruBaslangici

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Text(Metin.al("daily_question_card_title"))
                .font(Tema.yazi(24))
                .foregroundColor(.white)
            Text("Soru \(veri.slotIndex + 1) / 3")
                .font(Tema.yazi(16))
                .foregroundColor(GunlukSoruStil.acikSari)
            Text(soruMetni)
                .font(Tema.yazi(28))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            Text("Soru ekranı henüz bağlanmadı. Sonucu seç:")
                .font(Tema.yazi(14, kalin: false))
                .foregroundColor(Color.white.opacity(0.7))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            Spacer()
            HStack(spacing: 16) {
                dugme("Yanlış", zemin: Color("red"), dogru: false)
                dugme("Doğru", zemin: Color("button_enabled"), dogru: true)
            }
            .padding(.bottom, 48)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color").ignoresSafeArea())
        .preferredColorScheme(.dark)
    }

    private var soruMetni: String {
        if let islem = veri.islem {
            return "\(islem.ilk) \(islem.islec) \(islem.ikinci)"
        }
        var parcalar: [String] = []
        for sayi in veri.dizi {
            parcalar.append(String(sayi))
        }
        return parcalar.joined(separator: "   ")
    }

    private func dugme(_ baslik: String, zemin: Color, dogru: Bool) -> some View {
        Button(action: { veri.sonuc(dogru) }) {
            Text(baslik)
                .font(Tema.yazi(18))
                .foregroundColor(.black)
                .frame(minWidth: 120)
                .frame(height: 52)
                .background(
                    RoundedRectangle(cornerRadius: 15)
                        .fill(zemin)
                )
        }
        .buttonStyle(GunlukSoruKartBasmaStili())
    }
}
