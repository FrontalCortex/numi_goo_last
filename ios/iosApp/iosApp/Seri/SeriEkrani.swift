import Shared
import SwiftUI

// Günlük seri ekranı. Android karşılığı: StreakFragment.kt + res/layout/fragment_streak.xml.
// Üst çubuktaki aleve ve kutlama şeridine dokununca açılır.
//
// Yapı (yukarıdan aşağı): üst bar (geri, "Günlük Seri") · alev (yaşıyorsa Lottie, kırıksa sönük simge) ·
// büyük sayı · "gün üst üste" / "Serin şu anda kırık" · alt başlık · Bugün kartı (çubuk) · hafta şeridi ·
// meydan okuma kartı · Seri Ödülleri (kilometre taşları) · En uzun serin · Seri dondurma satırı ·
// "Günlük hedefi değiştir" · "Bildirim saatini değiştir · 19:00".
// Meydan okuma bu ekrandan DEĞİŞTİRİLEMEZ (ödülü var; yeni söz yalnızca seri kırılınca ders dönüşünde).
struct SeriEkrani: View {
    @StateObject private var model: SeriEkranModeli
    private let kapat: () -> Void
    private let dondurmaMagazasiniAc: () -> Void
    private let cuzdanDegisti: () -> Void

    /// - Parameters:
    ///   - servis: Seri verisi; verilmezse uygulamanın tek sahte servisi (`SeriOrtak.servis`).
    ///   - kapat: Geri düğmesi (Android: popBackStack).
    ///   - dondurmaMagazasiniAc: "Seri dondurma: Yok" satırı — mağaza seri dondurma kartında açılmalı
    ///     (Android: openShopFragment(focusStreakFreeze = true)).
    ///   - cuzdanDegisti: Ödül toplandı; üst çubuktaki altın/anahtar tazelenmeli (Android: refreshWalletUi).
    ///   - acikPencere: Galeri için açılışta açık pencere (hedef / hatırlatma / saat).
    init(
        servis: SeriServisi? = nil,
        kapat: @escaping () -> Void,
        dondurmaMagazasiniAc: @escaping () -> Void = {},
        cuzdanDegisti: @escaping () -> Void = {},
        acikPencere: SeriPenceresi? = nil
    ) {
        let kaynak: SeriServisi = servis ?? SeriOrtak.servis
        let model = SeriEkranModeli(servis: kaynak)
        model.pencere = acikPencere
        _model = StateObject(wrappedValue: model)
        self.kapat = kapat
        self.dondurmaMagazasiniAc = dondurmaMagazasiniAc
        self.cuzdanDegisti = cuzdanDegisti
    }

    var body: some View {
        VStack(spacing: 0) {
            SeriUstBar(kapat: kapat)
            ScrollView(.vertical, showsIndicators: false) {
                SeriEkranIcerigi(
                    model: model,
                    dondurmaMagazasiniAc: dondurmaMagazasiniAc,
                    cuzdanDegisti: cuzdanDegisti
                )
            }
        }
        .background(Color("background_color").ignoresSafeArea())
        .overlay(alignment: .bottom) {
            if let metin = model.bildirim {
                SeriBildirimGorunumu(metin: metin)
                    .transition(.opacity)
            }
        }
        .overlay {
            SeriEkranPencereleri(model: model)
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .preferredColorScheme(.dark)
        .onAppear { model.basla() }
    }
}

/// 56dp üst bar: 48dp geri düğmesi (12dp iç boşlukla 24dp beyaz ok), ortada 18sp kalın başlık, sağda 48dp boşluk.
private struct SeriUstBar: View {
    let kapat: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: kapat) {
                Image("ic_back_arrow")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(Color.white)
                    .frame(width: 24, height: 24)
                    .frame(width: 48, height: 48)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(Metin.al("fragment_streak_btn_streak_back_desc")))
            Text(Metin.al("fragment_streak_text"))
                .font(Tema.yazi(18))
                .foregroundColor(Color.white)
                .frame(maxWidth: .infinity)
            Color.clear.frame(width: 48, height: 48)
        }
        .padding(.leading, 4)
        .padding(.trailing, 16)
        .frame(height: 56)
    }
}

/// Kaydırılan içerik: yanlarda 20dp, üstte 8dp, altta 28dp.
private struct SeriEkranIcerigi: View {
    @ObservedObject var model: SeriEkranModeli
    let dondurmaMagazasiniAc: () -> Void
    let cuzdanDegisti: () -> Void

    var body: some View {
        let ui: StreakScreenUi = model.veri.ui
        return VStack(spacing: 0) {
            SeriBuyukAlev(ui: ui)
            SeriBugunKarti(ui: ui)
                .padding(.top, 24)
            SeriHaftaSeridi(gunler: model.veri.hafta)
                .padding(.top, 14)
            if let kart = ui.challenge {
                SeriMeydanOkumaKarti(kart: kart, toplaniyor: model.toplaniyor) {
                    model.meydanOkumaTopla(cuzdanDegisti: cuzdanDegisti)
                }
                .padding(.top, 14)
            }
            SeriOdulListesi(satirlar: ui.milestones) { tas in
                model.tasiTopla(tas, cuzdanDegisti: cuzdanDegisti)
            }
            .padding(.top, 22)
            SeriAltSatirlar(model: model, ui: ui, dondurmaMagazasiniAc: dondurmaMagazasiniAc)
                .padding(.top, 14)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 28)
    }
}

/// Alev, büyük sayı, etiket ve alt başlık.
private struct SeriBuyukAlev: View {
    let ui: StreakScreenUi

    var body: some View {
        VStack(spacing: 0) {
            if ui.alive {
                // streakLottie: 150dp, fire_anim, sürekli.
                LottieGorunumu("fire_anim", dongu: true)
                    .frame(width: 150, height: 150)
            } else {
                // streakDeadIcon: 120dp, alfa 0,45, üstte ve altta 15dp.
                Image("streak_flame_ic")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 120, height: 120)
                    .opacity(0.45)
                    .padding(.vertical, 15)
            }
            Text(String(ui.bigNumber))
                .font(Tema.yazi(52))
                .foregroundColor(ui.alive ? SeriStil.turuncu : SeriStil.sonuk)
            Text(SeriMetin.yaz(ui.bigLabel))
                .font(Tema.yazi(17))
                .foregroundColor(Color.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 4)
            Text(SeriMetin.yaz(ui.bigSubtitle))
                .font(Tema.yazi(14))
                .foregroundColor(SeriStil.soluk)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        }
    }
}

/// "Bugün" kartı: 15sp kalın başlık, 12dp altında çubuk ("2 / 5 dakika" ya da "Hedef tamamlandı ✓").
private struct SeriBugunKarti: View {
    let ui: StreakScreenUi

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("fragment_streak_text_2"))
                .font(Tema.yazi(15))
                .foregroundColor(Color.white)
            SeriIlerlemeCubugu(yuzde: ui.todayPercent, tamam: ui.todayComplete, yazi: SeriMetin.yaz(ui.todayText))
                .padding(.top, 12)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .seriKart()
    }
}

/// Hafta şeridi (StreakViews.buildWeekStrip): Pazartesiden Pazara; 28dp daire + 6dp altında 12sp harf.
/// Tutturulmuş: turuncu + "✓"; donmuş: buz mavisi + kar tanesi; bugün: 2dp turuncu halka; boş: #2E4149.
struct SeriHaftaSeridi: View {
    let gunler: [StreakWeekDayUi]

    var body: some View {
        let harfler: [String] = Metin.al("ui_week_initials_monday_first")
            .split(separator: " ")
            .map { String($0) }
        return HStack(spacing: 0) {
            ForEach(Array(gunler.enumerated()), id: \.offset) { oge in
                SeriHaftaGunu(gun: oge.element, harf: SeriHaftaSeridi.harf(harfler, Int(oge.element.index)))
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 14)
        .seriKart()
    }

    static func harf(_ harfler: [String], _ sira: Int) -> String {
        if sira >= 0 && sira < harfler.count {
            return harfler[sira]
        }
        return ""
    }
}

private struct SeriHaftaGunu: View {
    let gun: StreakWeekDayUi
    let harf: String

    var body: some View {
        VStack(spacing: 6) {
            daire
                .frame(width: 28, height: 28)
            Text(harf)
                .font(Tema.yazi(12))
                .foregroundColor(gun.today ? SeriStil.turuncu : SeriStil.soluk)
        }
    }

    @ViewBuilder
    private var daire: some View {
        if gun.done {
            ZStack {
                Circle().fill(SeriStil.turuncu)
                Text("✓")
                    .font(Tema.yazi(14))
                    .foregroundColor(Color.white)
            }
        } else if gun.frozen {
            ZStack {
                Circle().fill(SeriStil.gunDonmus)
                Image("streak_snowflake_ic")
                    .resizable()
                    .scaledToFit()
                    .padding(6)
            }
            .accessibilityLabel(Text(Metin.al("ui_seri_dondurma_kullanildi")))
        } else if gun.today {
            Circle().strokeBorder(SeriStil.turuncu, lineWidth: 2)
        } else {
            Circle().fill(SeriStil.gunBos)
        }
    }
}

/// Meydan okuma kartı (streakChallengeCard): alev 36dp, başlık 15sp, alt başlık 13sp; ödül rozeti;
/// 14dp altında çubuk; toplanabilirse "Ödülü topla" (bg_streak_claim, 11dp dikey boşluk).
struct SeriMeydanOkumaKarti: View {
    let kart: StreakChallengeUi
    let toplaniyor: Bool
    let topla: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ust
            SeriIlerlemeCubugu(yuzde: kart.percent, tamam: kart.complete, yazi: cubukYazisi)
                .padding(.top, 14)
            if kart.claimVisible {
                Button(action: topla) {
                    Text(Metin.al("fragment_streak_streak_challenge_claim"))
                        .font(Tema.yazi(15))
                        .foregroundColor(SeriStil.toplaYazi)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(RoundedRectangle(cornerRadius: 20).fill(SeriStil.turuncu))
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .disabled(toplaniyor)
                .padding(.top, 14)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .seriKart()
    }

    private var cubukYazisi: String {
        if let anahtar = kart.progressKey {
            return Metin.al(anahtar)
        }
        return kart.progressText
    }

    private var ust: some View {
        HStack(spacing: 0) {
            Image("streak_flame_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 36, height: 36)
            VStack(alignment: .leading, spacing: 3) {
                Text(SeriMetin.yaz(kart.title))
                    .font(Tema.yazi(15))
                    .foregroundColor(Color.white)
                Text(SeriMetin.yaz(kart.subtitle))
                    .font(Tema.yazi(13))
                    .foregroundColor(SeriStil.soluk)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, 12)
            if kart.rewardBadgeVisible {
                rozet
                    .padding(.leading, 8)
            }
        }
    }

    /// bg_streak_reward_badge: "+500" (14sp kalın #FFC107) + 16dp altın simgesi.
    private var rozet: some View {
        HStack(spacing: 4) {
            Text("+" + String(kart.reward))
                .font(Tema.yazi(14))
                .foregroundColor(SeriStil.altin)
            Image("gold_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 16, height: 16)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(RoundedRectangle(cornerRadius: 12).fill(SeriStil.rozetZemini))
        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(SeriStil.rozetCizgisi, lineWidth: 1))
    }
}

/// "Seri Ödülleri" başlığı (16sp kalın, altta 2dp) ve kilometre taşı satırları (aralarında 10dp).
private struct SeriOdulListesi: View {
    let satirlar: [StreakMilestoneRowUi]
    let topla: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("fragment_streak_streak_rewards_title"))
                .font(Tema.yazi(16))
                .foregroundColor(Color.white)
                .padding(.bottom, 2)
            ForEach(Array(satirlar.enumerated()), id: \.offset) { oge in
                SeriOdulSatiri(satir: oge.element, topla: topla)
                    .padding(.top, 10)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// StreakViews.buildMilestoneRows satırı: iç boşluk 14dp; "N gün" (15sp kalın) + ödül şeridi (18dp simge,
/// 14sp sayı); sağda "Topla" ya da "N gün kaldı" (13sp soluk). Toplanabilir satır seçili çerçeveyle.
private struct SeriOdulSatiri: View {
    let satir: StreakMilestoneRowUi
    let topla: (Int) -> Void

    var body: some View {
        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 3) {
                Text(SeriMetin.gun(Int(satir.milestone)))
                    .font(Tema.yazi(15))
                    .foregroundColor(Color.white)
                odulSeridi
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            sag
        }
        .padding(14)
        .seriKart(secili: satir.claimable)
    }

    private var odulSeridi: some View {
        let renk: Color = satir.claimable ? SeriStil.turuncu : SeriStil.soluk
        return HStack(spacing: 0) {
            if satir.keys > 0 {
                SeriOdulParcasi(simge: "key", miktar: Int(satir.keys), renk: renk)
            }
            if satir.gold > 0 {
                SeriOdulParcasi(simge: "gold_ic", miktar: Int(satir.gold), renk: renk)
                    .padding(.leading, satir.keys > 0 ? 10 : 0)
            }
        }
    }

    @ViewBuilder
    private var sag: some View {
        if satir.claimable {
            let tas: Int = Int(satir.milestone)
            Button {
                topla(tas)
            } label: {
                Text(Metin.al("ui_topla"))
                    .font(Tema.yazi(14))
                    .foregroundColor(SeriStil.toplaYazi)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 20).fill(SeriStil.turuncu))
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        } else {
            let kalan: Int = Int(satir.daysLeft)
            Text(Metin.cogul("ui_x_gun_kaldi", kalan, kalan))
                .font(Tema.yazi(13))
                .foregroundColor(SeriStil.soluk)
        }
    }
}

private struct SeriOdulParcasi: View {
    let simge: String
    let miktar: Int
    let renk: Color

    var body: some View {
        HStack(spacing: 5) {
            Image(simge)
                .resizable()
                .scaledToFit()
                .frame(width: 18, height: 18)
            Text(String(miktar))
                .font(Tema.yazi(14))
                .foregroundColor(renk)
        }
    }
}

/// En uzun seri, seri dondurma satırı ve iki ayar düğmesi.
private struct SeriAltSatirlar: View {
    @ObservedObject var model: SeriEkranModeli
    let ui: StreakScreenUi
    let dondurmaMagazasiniAc: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            enUzun
            dondurma
                .padding(.top, 14)
            ayarDugmesi(Metin.al("fragment_streak_streak_change_goal")) {
                model.pencere = .hedef
            }
            .padding(.top, 20)
            ayarDugmesi(hatirlatmaMetni) {
                model.pencere = .hatirlatma
            }
            .padding(.top, 12)
        }
    }

    private var hatirlatmaMetni: String {
        return Metin.al("ui_bildirim_saatini_degistir") + StreakRules.shared.hourText(hour: ui.reminderHour)
    }

    private var enUzun: some View {
        HStack(spacing: 0) {
            Text(Metin.al("fragment_streak_text_3"))
                .font(Tema.yazi(15))
                .foregroundColor(Color.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(SeriMetin.gun(Int(ui.longest)))
                .font(Tema.yazi(15))
                .foregroundColor(SeriStil.turuncu)
        }
        .padding(16)
        .seriKart()
    }

    /// streakFreezeRow: 24dp simge (10dp sağ boşluk), "Seri dondurma", "Hazır"/"Yok", tıklanabilirken "›".
    private var dondurma: some View {
        Button {
            if model.dondurmaSatirinaDokunuldu() {
                dondurmaMagazasiniAc()
            }
        } label: {
            HStack(spacing: 0) {
                Image("streak_freeze_ic")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .padding(.trailing, 10)
                Text(Metin.al("fragment_streak_text_4"))
                    .font(Tema.yazi(15))
                    .foregroundColor(Color.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(ui.freezeHeld ? Metin.al("ui_hazir_2") : Metin.al("ui_yok"))
                    .font(Tema.yazi(15))
                    .foregroundColor(ui.freezeHeld ? SeriStil.buz : SeriStil.sonuk)
                if ui.freezeRowClickable {
                    Text("›")
                        .font(Tema.yazi(22))
                        .foregroundColor(SeriStil.soluk)
                        .padding(.leading, 8)
                }
            }
            .padding(16)
            .seriKart()
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!ui.freezeRowClickable)
    }

    /// 52dp yüksekliğinde, bg_streak_option zeminli, 15sp beyaz metinli düğme.
    private func ayarDugmesi(_ metin: String, _ eylem: @escaping () -> Void) -> some View {
        Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(15))
                .foregroundColor(Color.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .seriKart()
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Hedef, hatırlatma ve saat pencereleri (dialog_streak_options.xml).
private struct SeriEkranPencereleri: View {
    @ObservedObject var model: SeriEkranModeli

    var body: some View {
        ZStack {
            switch model.pencere {
            case .some(.hedef):
                SeriPencere(
                    baslik: Metin.al("ui_gunluk_hedefin"),
                    altBaslik: Metin.al("ui_her_gun_ne_kadar_ogrenmek_istersin"),
                    kapat: { model.pencere = nil }
                ) {
                    SeriSecenekListesi(satirlar: model.hedefSatirlari, sagRengi: SeriStil.soluk) { dakika in
                        model.hedefSecildi(dakika)
                    }
                }
            case .some(.hatirlatma):
                SeriPencere(
                    baslik: Metin.al("ui_bildirim_saatin"),
                    altBaslik: Metin.al("ui_sobi_sana_her_gun_ne_zaman"),
                    kapat: { model.pencere = nil }
                ) {
                    SeriSecenekListesi(satirlar: model.hatirlatmaSatirlari, sagRengi: SeriStil.soluk) { deger in
                        model.hatirlatmaSatiriSecildi(deger)
                    }
                }
            case .some(.saat):
                SeriPencere(
                    baslik: Metin.al("ui_saat_sec"),
                    altBaslik: Metin.al("ui_sobi_sana_her_gun_bu_saatte"),
                    kapat: { model.pencere = nil }
                ) {
                    SeriSaatIzgarasi(secili: model.servis.hatirlatmaSaati) { saat in
                        model.saatSecildi(saat)
                    }
                }
            case .none:
                EmptyView()
            }
        }
    }
}
