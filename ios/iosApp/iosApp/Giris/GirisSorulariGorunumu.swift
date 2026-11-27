import Shared
import SwiftUI

// Kayıt soruları ekranı. Android: UserInfoFragment + fragment_user_info.xml.
// Yerleşim (zemin background_color, kenar boşluğu YOK — kap kökteki 24dp dolgunun dışında):
//   - ilerleme çubuğu: üstten 16dp, yanlardan 20dp, 18dp yükseklik (görevlerdeki çubuk: missions_track
//     zemin, mavi dolgu, üstte 4dp parlama); tanışma ekranlarında gizli
//   - soru başlığı (yaş, kaynak, abaküs, hedef, yol): 16dp altında, sol 8dp / sağ 20dp; solda 140dp
//     maskot (defter elde), sağında kuyruğu ona dönük balon (19sp, en az 64dp)
//   - seri maskotu (meydan okuma girişi, meydan okuma, hatırlatma): 170dp, ortada, 16dp altında
//   - adım kabı (kayarak değişir), altta "Devam Et" (56dp, yanlar 10dp, göçen; seçim yokken gri ve
//     dudaksız), hatırlatmada altında "Şimdi değil"
//   - tanışma (MEET, BRIEF): çubuğun altından düğmeye kadar; ortada (dikey %60) 180dp maskot,
//     üstünde aşağı kuyruklu balon (20sp)
// Android'de ekranda geri düğmesi yoktur, geri telefonun geri tuşuyla gider. iOS'ta donanım geri
// tuşu olmadığı için aynı davranış soldan kenar kaydırmasıyla verilir.
// Basitleştirme: Android'de tanışmadaki tavşan küçülerek soru başlığındaki maskotun yerine kayar;
// burada iki maskot arasında solarak geçilir.
@MainActor
struct GirisSorulariGorunumu: View {
    @ObservedObject var akis: GirisSoruModeli
    let geri: () -> Void

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                GirisIlerlemeCubugu(yuzde: akis.ilerleme)
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .opacity(akis.adim.tanismaMi ? 0 : 1)
                govde
                dugmeler
            }
            if akis.saatPenceresiAcik {
                GirisSaatPenceresi(akis: akis)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.15), value: akis.saatPenceresiAcik)
        .contentShape(Rectangle())
        .simultaneousGesture(kenarKaydirma)
        .onAppear {
            akis.yenidenGorundu()
        }
    }

    /// Soldan kenar kaydırması: Android geri tuşu karşılığı.
    private var kenarKaydirma: some Gesture {
        DragGesture(minimumDistance: 20)
            .onEnded { deger in
                let basX: CGFloat = deger.startLocation.x
                let yatay: CGFloat = deger.translation.width
                let dikey: CGFloat = abs(deger.translation.height)
                if basX < 24 && yatay > 60 && dikey < yatay {
                    geri()
                }
            }
    }

    @ViewBuilder
    private var govde: some View {
        if akis.adim.tanismaMi {
            GirisTanismaGorunumu(akis: akis)
                .transition(.opacity)
        } else {
            VStack(spacing: 0) {
                baslik
                GirisSoruKabi(akis: akis)
                    .id(akis.adim.kapKimligi)
                    .transition(kayma)
                    .frame(maxHeight: .infinity, alignment: .top)
                    .clipped()
            }
            .transition(.opacity)
        }
    }

    /// Android slideTo: çıkan görünüm yönün tersine kayıp saydamlaşır, giren karşı taraftan gelir.
    private var kayma: AnyTransition {
        let giris: Edge = akis.ileriYon ? .trailing : .leading
        let cikis: Edge = akis.ileriYon ? .leading : .trailing
        return AnyTransition.asymmetric(
            insertion: AnyTransition.move(edge: giris).combined(with: .opacity),
            removal: AnyTransition.move(edge: cikis).combined(with: .opacity)
        )
    }

    @ViewBuilder
    private var baslik: some View {
        if akis.adim.basliklimi {
            GirisSoruBasligi(akis: akis)
                .padding(.leading, 8)
                .padding(.trailing, 20)
                .padding(.top, 16)
        } else if akis.adim.seriMaskotluMu {
            GirisSeriMaskotu(adim: akis.adim)
                .frame(width: 170, height: 170)
                .padding(.top, 16)
        }
    }

    private var dugmeler: some View {
        let sonAdim: Bool = akis.adim == .hatirlatma
        return VStack(spacing: 0) {
            GirisGocenDugme(
                metin: akis.devamMetni,
                etkin: akis.devamEtkin,
                buyukHarf: false,
                yaziBoyu: 18,
                zemin: Color(akis.devamEtkin ? "dark_primary" : "dark_text_secondary")
            ) {
                GirisStil.klavyeyiKapat()
                akis.devam()
            }
            .padding(.horizontal, 10)
            .padding(.top, 24)
            .padding(.bottom, sonAdim ? 4 : 24)
            if sonAdim {
                GirisBaglantiDugmesi(metin: Metin.al("fragment_user_info_btn_later")) {
                    akis.simdiDegil()
                }
                .padding(.top, 4)
                .padding(.bottom, 12)
            }
        }
    }
}

/// Görevlerdeki çubuk (item_mission_quest, applyMissionProgressOverlayNow): 18dp yuvarlak koyu
/// zemin (missions_track, köşe 16dp), mavi dolgu (missions_progress_blue) ve üstünde ince parlama
/// (missions_progress_blue_shine, 4dp, soldan 14dp, sondan 10dp boşluk, üstten 4dp, köşe 3dp).
struct GirisIlerlemeCubugu: View {
    let yuzde: Double

    var body: some View {
        GeometryReader { alan in
            let w: CGFloat = alan.size.width
            let dolgu: CGFloat = min(max(w * CGFloat(yuzde) / 100, 0), w)
            let parlama: CGFloat = max(dolgu - 14 - 10, 0)
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 16).fill(Color("missions_track"))
                if dolgu > 0 {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color("missions_progress_blue"))
                        .frame(width: dolgu)
                }
                if parlama > 0 {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color("missions_progress_blue_shine"))
                        .frame(width: parlama, height: 4)
                        .padding(.leading, 14)
                        .padding(.top, 4)
                }
            }
        }
        .frame(height: 18)
    }
}

/// Soru başlığı: defterine yazan maskot + kuyruğu ona dönük balon (questionHeader).
@MainActor
struct GirisSoruBasligi: View {
    @ObservedObject var akis: GirisSoruModeli

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            SobiMaskotGorunumu(emote: akis.soruMaskotHali)
                .frame(width: 140, height: 140)
            GirisDaktiloMetni(parcalar: soruParcalari, boyut: 19)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 28)
                .padding(.trailing, 16)
                .padding(.vertical, 14)
                .frame(minHeight: 64)
                .background(GirisBalonZemini(kuyruk: .sol))
        }
    }

    /// UserInfoFragment.bindQuestionHeader.
    private var soruParcalari: [GirisMetinParcasi] {
        switch akis.adim {
        case .yas:
            return [GirisMetinParcasi(metin: Metin.al("ui_kac_yasindasin"))]
        case .kaynak:
            return [GirisMetinParcasi(metin: Metin.al("ui_bizi_nereden_duydun"))]
        case .abakus:
            return [GirisMetinParcasi(metin: Metin.al("ui_abakus_kullanmayi_biliyor_musun"))]
        case .hedef:
            return [GirisMetinParcasi(metin: Metin.al("ui_her_gun_ne_kadar_ogrenmek_istersin"))]
        case .yol:
            // "Her gün N dakikayla buraya varacaksın!": seçilen süre altın (lesson_ring_gold).
            return [
                GirisMetinParcasi(metin: Metin.al("ui_her_gun")),
                GirisMetinParcasi(metin: Metin.al("ui_x_dakikayla", akis.hedefDakika), renk: Color("lesson_ring_gold")),
                GirisMetinParcasi(metin: Metin.al("ui_buraya_varacaksin")),
            ]
        default:
            return []
        }
    }
}

/// Seri adımlarının ortak maskotu: meydan okuma girişinde sürekli utangaç (SHY, aralıksız
/// tekrar), gün sorusunda elinde alev (FLAME), hatırlatmada çalar saat (ALARM).
/// Basitleştirme: Android'de alev seçilen güne göre büyür (streakDays) ve saatin ibreleri seçili
/// saate döner (alarmHour); SobiMaskotGorunumu bu iki girdiyi almıyor.
@MainActor
struct GirisSeriMaskotu: View {
    let adim: GirisSoruAdimi

    var body: some View {
        switch adim {
        case .meydanGiris:
            GirisTekrarliMaskot(emote: "SHY", araMs: 0)
        case .meydan:
            SobiMaskotGorunumu(emote: "FLAME")
        default:
            SobiMaskotGorunumu(emote: "ALARM")
        }
    }
}

/// Tanışma (MEET: kendini tanıtır, 2 sn arayla el sallar; BRIEF: kaç soru sorulacağını söyler,
/// 2 sn arayla alkışlar). Tavşan kabın ortasına sabit (dikey %60), balon onun üstüne asılı ve
/// yukarı doğru büyür.
@MainActor
struct GirisTanismaGorunumu: View {
    @ObservedObject var akis: GirisSoruModeli

    var body: some View {
        GeometryReader { alan in
            let ust: CGFloat = max(0, (alan.size.height - 180) * 0.6)
            VStack(spacing: 0) {
                balon
                    .padding(.horizontal, 24)
                    .frame(maxWidth: .infinity)
                    .frame(height: ust, alignment: .bottom)
                GirisTekrarliMaskot(emote: akis.adim == .tanisma ? "GREET" : "CLAP", araMs: GirisSoruModeli.tanismaAraMs)
                    .frame(width: 180, height: 180)
                Spacer(minLength: 0)
            }
        }
    }

    private var balon: some View {
        GirisDaktiloMetni(parcalar: parcalar, boyut: 20, satirArasi: 4)
            .multilineTextAlignment(.leading)
            .padding(.leading, 20)
            .padding(.trailing, 20)
            .padding(.top, 14)
            // Alt dolgu = metin boşluğu (14dp) + kuyruk (16dp).
            .padding(.bottom, 30)
            .background(GirisBalonZemini(kuyruk: .alt))
    }

    private var parcalar: [GirisMetinParcasi] {
        if akis.adim == .tanisma {
            return [GirisMetinParcasi(metin: Metin.al("ui_selamlar_benim_adim_x", Metin.al("mascot_name")))]
        }
        // "İlk dersinden önce seni tanımak için sadece N kısa sorum var!" (N kalın; tema zaten kalın).
        return [
            GirisMetinParcasi(metin: Metin.al("ui_ilk_dersinden_once_seni_tanimak_icin")),
            GirisMetinParcasi(metin: Metin.al("ui_x_kisa_sorum", akis.soruSayisi)),
            GirisMetinParcasi(metin: Metin.al("ui_var")),
        ]
    }
}
