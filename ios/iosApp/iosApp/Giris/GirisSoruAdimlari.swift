import Shared
import SwiftUI

// Kayıt sorularının adım kapları (fragment_user_info.xml: ageContainer, sourceContainer,
// streakContainer, roadmapContainer, challengeIntroContainer) ve "Başka bir saat" penceresi.

/// O adımın kayan kabı.
@MainActor
struct GirisSoruKabi: View {
    @ObservedObject var akis: GirisSoruModeli

    var body: some View {
        switch akis.adim {
        case .yas:
            GirisYasAlani(metin: $akis.yasMetni)
                .frame(maxWidth: .infinity)
                .padding(.top, 24)
        case .kaynak:
            GirisSecenekListesi(satirlar: akis.kaynakSatirlari, sagRengi: GirisSecenekSatiriGorunumu.sonukRenk) { sira in
                akis.kaynakSecildi(sira)
            }
            .padding(.top, 24)
            .padding(.bottom, 16)
        case .abakus:
            GirisSecenekListesi(satirlar: akis.abakusSatirlari, sagRengi: GirisSecenekSatiriGorunumu.sonukRenk) { sira in
                akis.abakusSecildi(sira)
            }
            .padding(.top, 24)
            .padding(.bottom, 16)
        case .hedef, .meydan, .hatirlatma:
            GirisSeriKabi(akis: akis)
                .padding(.top, 16)
        case .yol:
            GirisYolKabi(akis: akis)
                .padding(.top, 16)
        case .meydanGiris:
            Text(Metin.al("fragment_user_info_challenge_intro_text"))
                .font(Tema.yazi(24))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 28)
                .padding(.top, 16 + 12)
        case .tanisma, .ozet:
            Color.clear
        }
    }
}

/// Yaş alanı (etAge): 140x56dp, bg_edit_profile_input (bright_background_color zemin, köşe 12dp,
/// 1dp #30363D çizgi), ortalı 22sp kalın, ipucu "Yaş", yalnızca rakam, en çok 3 hane.
@MainActor
struct GirisYasAlani: View {
    @Binding var metin: String

    var body: some View {
        let azami: Int = Int(UserInfoRules.shared.AGE_MAX_LENGTH)
        ZStack {
            if metin.isEmpty {
                Text(Metin.al("fragment_user_info_et_age_hint"))
                    .font(Tema.yazi(22))
                    .foregroundColor(Color("dark_text_secondary"))
                    .allowsHitTesting(false)
            }
            TextField("", text: $metin)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(Tema.yazi(22))
                .foregroundColor(Color("dark_text"))
                .tint(Color("dark_primary"))
        }
        .padding(.horizontal, 16)
        .frame(width: 140, height: 56)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color("bright_background_color")))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(GirisStil.renk(0x30363D), lineWidth: 1))
        .onChange(of: metin) { yeni in
            // inputType="number": yalnızca rakam; maxLength="3".
            let rakamlar = yeni.filter { $0 >= "0" && $0 <= "9" }
            let kisaltilmis = String(rakamlar.prefix(azami))
            if kisaltilmis != yeni {
                metin = kisaltilmis
            }
        }
    }
}

/// Seçenek satırları (StreakViews.buildOptionRows): alt alta, aralarında 12dp; kaydırılabilir;
/// yanlarda 24dp, altta 8dp iç boşluk.
@MainActor
struct GirisSecenekListesi: View {
    let satirlar: [GirisSecenekSatiri]
    let sagRengi: Color
    let secildi: (Int) -> Void

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 12) {
                ForEach(satirlar) { satir in
                    GirisSecenekSatiriGorunumu(satir: satir, sagRengi: sagRengi) {
                        secildi(satir.id)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 8)
        }
    }
}

/// Tek satır: solda ana metin (17sp kalın beyaz), sağda açıklama (15sp). Zemin bg_streak_option
/// (#203137, 2dp #2E4149, köşe 14dp); seçiliyken bg_streak_option_selected (#2A3B42, 3dp #FF9800).
/// İç boşluk 16dp.
@MainActor
struct GirisSecenekSatiriGorunumu: View {
    /// StreakViews.COLOR_MUTED
    static let sonukRenk: Color = GirisStil.renk(0x93A5B3)
    /// StreakViews.COLOR_GOLD: sağdaki metin bir ödül olduğunda.
    static let altinRenk: Color = GirisStil.renk(0xFFC107)

    let satir: GirisSecenekSatiri
    let sagRengi: Color
    let eylem: () -> Void

    var body: some View {
        let zemin: Color = satir.secili ? GirisStil.renk(0x2A3B42) : GirisStil.renk(0x203137)
        let cizgi: Color = satir.secili ? GirisStil.renk(0xFF9800) : GirisStil.renk(0x2E4149)
        let kalinlik: CGFloat = satir.secili ? 3 : 2
        Button(action: eylem) {
            HStack(spacing: 0) {
                Text(satir.etiket)
                    .font(Tema.yazi(17))
                    .foregroundColor(Color.white)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(satir.sagMetin)
                    .font(Tema.yazi(15))
                    .foregroundColor(sagRengi)
                    .lineLimit(1)
            }
            .padding(16)
            .background(RoundedRectangle(cornerRadius: 14).fill(zemin))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(cizgi, lineWidth: kalinlik))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Seri adımlarının kabı (streakContainer): başlık yalnızca gün ve hatırlatma sorusunda (22sp
/// kalın, ortalı, yanlar 24dp); alt başlık 10dp altında (14sp dark_text_secondary); satırlar 28dp altında.
@MainActor
struct GirisSeriKabi: View {
    @ObservedObject var akis: GirisSoruModeli

    var body: some View {
        VStack(spacing: 0) {
            if let baslik = baslikMetni {
                Text(baslik)
                    .font(Tema.yazi(22))
                    .foregroundColor(Color("dark_text"))
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 24)
            }
            Text(altBaslikMetni)
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 24)
                .padding(.top, 10)
            satirlar
                .padding(.top, 28)
        }
    }

    private var baslikMetni: String? {
        switch akis.adim {
        case .meydan:
            return Metin.al("ui_kac_gun_ust_uste_ogreneceksin")
        case .hatirlatma:
            return Metin.al("ui_sobi_sana_her_gun_hatirlatsin_mi")
        default:
            // Hedef sorusunda soru balonda.
            return nil
        }
    }

    private var altBaslikMetni: String {
        switch akis.adim {
        case .meydan:
            return Metin.al("ui_gunde_x_dakika_bu_secim_sonradan", akis.hedefDakika)
        case .hatirlatma:
            return Metin.al("ui_saati_istedigin_zaman_degistirebilirsin")
        default:
            return Metin.al("ui_bu_hedefi_istedigin_zaman_degistirebilirsin")
        }
    }

    @ViewBuilder
    private var satirlar: some View {
        switch akis.adim {
        case .meydan:
            // Ödül burada yazar: seçim burada yapılıyor ve bir daha değiştirilemiyor.
            GirisSecenekListesi(satirlar: akis.meydanSatirlari, sagRengi: GirisSecenekSatiriGorunumu.altinRenk) { gun in
                akis.meydanSec(gun)
            }
        case .hatirlatma:
            GirisSecenekListesi(satirlar: akis.hatirlatmaSatirlari, sagRengi: GirisSecenekSatiriGorunumu.sonukRenk) { deger in
                akis.hatirlatmaSatiriSecildi(deger)
            }
        default:
            GirisSecenekListesi(satirlar: akis.hedefSatirlari, sagRengi: GirisSecenekSatiriGorunumu.sonukRenk) { dakika in
                akis.hedefSec(dakika)
            }
        }
    }
}

/// İlerleme yolu kabı (roadmapContainer): çizim yanlardan 12dp, kabın boş alanında dikey %30'da.
/// Çizim kabın kayarak gelmesi bitince başlar (STEP_ANIM_MS + 80 ms).
@MainActor
struct GirisYolKabi: View {
    @ObservedObject var akis: GirisSoruModeli

    var body: some View {
        GeometryReader { alan in
            let yolBoyu: CGFloat = GirisYolYerlesimi(genislik: 360).yukseklik
            let bos: CGFloat = max(0, alan.size.height - yolBoyu)
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                    .frame(height: bos * 0.3)
                GirisOgrenmeYolu(
                    adimlar: GirisYolPlani.adimlar(hedefDakika: akis.hedefDakika),
                    oynatma: akis.yolOynatmaSayaci,
                    gecikmeMs: GirisSoruModeli.gecisSuresi * 1000 + 80,
                    bitti: { akis.yolCizimiBitti() }
                )
                .padding(.horizontal, 12)
                Spacer(minLength: 0)
            }
        }
    }
}

/// "Başka bir saat" penceresi (StreakViews.showHourPicker + dialog_streak_options.xml): 24 saat,
/// 4 sütunlu ızgara (06:00'dan başlar), hücre 44dp, aralık 8dp; dokununca seçilip kapanır. Pencere
/// zemini bg_streak_dialog (#18272D, 2dp #2E4149, köşe 20dp), iç boşluk 20dp, yanlardan 24dp.
@MainActor
struct GirisSaatPenceresi: View {
    @ObservedObject var akis: GirisSoruModeli

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()
                .onTapGesture {
                    akis.saatPenceresiAcik = false
                }
            pencere
                .padding(.horizontal, 24)
        }
    }

    private var pencere: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("ui_saat_sec"))
                .font(Tema.yazi(18))
                .foregroundColor(Color.white)
            Text(Metin.al("ui_sobi_sana_her_gun_bu_saatte"))
                .font(Tema.yazi(13))
                .foregroundColor(GirisSecenekSatiriGorunumu.sonukRenk)
                .padding(.top, 6)
            izgara
                .padding(.top, 16)
        }
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 20).fill(GirisStil.renk(0x18272D)))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(GirisStil.renk(0x2E4149), lineWidth: 2))
    }

    private var izgara: some View {
        let saatler: [Int] = akis.saatIzgarasi
        let satirSayisi: Int = (saatler.count + 3) / 4
        return VStack(spacing: 8) {
            ForEach(0..<satirSayisi, id: \.self) { satir in
                HStack(spacing: 8) {
                    ForEach(0..<4, id: \.self) { sutun in
                        hucre(saatler, satir * 4 + sutun)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func hucre(_ saatler: [Int], _ sira: Int) -> some View {
        if sira < saatler.count {
            let saat: Int = saatler[sira]
            let secili: Bool = saat == akis.hatirlatmaSaati
            Button {
                akis.saatSec(saat)
            } label: {
                Text(SignupStreakRules.shared.hourText(hour: Int32(saat)))
                    .font(Tema.yazi(15))
                    .foregroundColor(Color.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(RoundedRectangle(cornerRadius: 14).fill(GirisStil.renk(secili ? 0x2A3B42 : 0x203137)))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .strokeBorder(GirisStil.renk(secili ? 0xFF9800 : 0x2E4149), lineWidth: secili ? 3 : 2)
                    )
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        } else {
            Color.clear.frame(maxWidth: .infinity).frame(height: 44)
        }
    }
}
