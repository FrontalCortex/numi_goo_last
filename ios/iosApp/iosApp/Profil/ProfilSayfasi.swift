import SwiftUI

// Profil sayfası: kendi profili (hedefUid nil) ya da başkasının profili.
// Android: ProfileFragment + res/layout/fragment_profile.xml, item_profile_cup_card.xml.
// Ölçüler layout'tan birebir (dp → pt).
//
// Güncel Android düzeni (yukarıdan aşağı):
//   - Sabit üst panel (56): kendi profilinde ad (22) + ayarlar; başkasınınkinde geri + ad (18). Rengi avatarın
//     arka plan rengi, telefonun durum çubuğu da aynı renge boyanır; alttaki 2 pt çizgi yalnızca içerik aşağı
//     kaydırılmışken görünür.
//   - Avatar alanı: ekran genişliğinde, avatarın arka plan renginde; 240x240 avatar alt kenara oturur.
//     Kendi profilinde renkli alanın neresine dokunulsa avatar düzenleme ekranı açılır.
//   - Kullanıcı satırı, takip sayaçları, ARKADAŞ EKLE / TAKİP ET düğmesi.
//   - Kullanıcı İlerlemesi (tamamlanan %, toplam süre, EN UZUN SERİ — eski "plan" satırı kaldırıldı),
//     Kullanıcı Abaküsü, Kupa yolu kartları.
//   - Rozetler.
//
// iOS'ta olmayan: kurs öğretmeninin öğrenci profilindeki "ilerleme raporu" düğmesi (btnStudentProgress →
// ProgressHubFragment; veli paneli/sınıf özelliği ilk iOS sürümünde yok). Yeri Android'deki gibi boş tutulur.

struct ProfilSayfasi: View {
    @ObservedObject var gezinme: ProfilGezinme
    @StateObject private var model: ProfilSayfaModeli
    /// İçerik aşağı kaydırıldı mı (üst panelin alt çizgisi için).
    @State private var kaydirildi: Bool = false

    init(hedefUid: String?, gezinme: ProfilGezinme) {
        self.gezinme = gezinme
        _model = StateObject(wrappedValue: ProfilSayfaModeli(depo: gezinme.depo, hedefUid: hedefUid))
    }

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                ProfilUstAlan(
                    model: model,
                    cizgiGorunur: kaydirildi,
                    geri: { gezinme.geri() },
                    ayarlar: { gezinme.ac(.hesapAyarlari) }
                )
                kaydirma
            }
            .background(Color("background_color"))
            panelKatmani
        }
        .onAppear { model.gorundu() }
        .onChange(of: gezinme.yenilemeSayaci) { _ in
            if !model.baskasi {
                model.yenile()
            }
        }
        .onChange(of: gezinme.avatarSayaci) { _ in
            model.yerelAvatariGoster()
        }
        .alert(Text(""), isPresented: $model.takipBirakmaOnayiAcik) {
            Button(Metin.al("ui_takibi_birak"), role: .destructive) {
                model.takibiBirak(bildir: { metin in gezinme.bildir(metin) })
            }
            Button(Metin.al("ui_iptal_2"), role: .cancel) {}
        } message: {
            Text(ProfilOrtak.takipBirakmaSorusu(ad: model.ad))
        }
        .animation(.easeInOut(duration: 0.2), value: model.abakusPaneliAcik)
    }

    private var kaydirma: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                ProfilKaydirmaOlcer()
                ProfilAvatarAlani(
                    model: model,
                    ac: { if !model.baskasi { gezinme.ac(.avatarDuzenle) } }
                )
                .padding(.bottom, 16)
                VStack(spacing: 0) {
                    ProfilBaslikKarti(model: model, gezinme: gezinme)
                    ProfilIlerlemeKarti(model: model, gezinme: gezinme)
                        .padding(.bottom, 16)
                    if model.rozetKartiGorunur {
                        ProfilRozetKarti(model: model, ac: { gezinme.rozetlerAc?() })
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
        }
        .coordinateSpace(name: ProfilKaydirmaOlcer.alanAdi)
        .onPreferenceChange(ProfilKaydirmaAnahtari.self) { konum in
            let asagida: Bool = konum < -0.5
            if asagida != kaydirildi {
                kaydirildi = asagida
            }
        }
    }

    /// Android: Dialog (dışına dokununca kapanır, ekranın %92'si genişliğinde).
    @ViewBuilder
    private var panelKatmani: some View {
        if model.abakusPaneliAcik {
            Color.black.opacity(0.6)
                .ignoresSafeArea()
                .onTapGesture { model.abakusPaneliAcik = false }
                .transition(.opacity)
            ProfilAbakusPaneli(model: model, bildir: { metin in gezinme.bildir(metin) })
                .transition(.opacity)
        }
    }
}

// MARK: - Kaydırma ölçümü (iOS 16'da ScrollView konum API'si yok)

struct ProfilKaydirmaAnahtari: PreferenceKey {
    static let defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

/// İçeriğin en üstüne konan sıfır yükseklikli ölçer: kaydırma alanına göre dikey konumu bildirir.
struct ProfilKaydirmaOlcer: View {
    static let alanAdi: String = "profilKaydirma"

    var body: some View {
        GeometryReader { alan in
            Color.clear.preference(
                key: ProfilKaydirmaAnahtari.self,
                value: alan.frame(in: .named(ProfilKaydirmaOlcer.alanAdi)).minY
            )
        }
        .frame(height: 0)
    }
}

// MARK: - Üst panel ve avatar alanı

/// Üst alanın renkleri (AARRGGBB metinlerinden).
struct ProfilUstAlanRenkleri {
    let zemin: Color
    let yazi: Color
    let cizgi: Color

    /// Android: applyHeaderColor — renk yoksa background_color zemin, beyaz yazı; açık zeminde #3C3C3C yazı;
    /// çizgi zeminin %15 siyahla karışımı.
    static func hesapla(_ argb: String?) -> ProfilUstAlanRenkleri {
        guard let renk = argb else {
            // background_color (#141F23) koyu → beyaz; çizgi #141F23'ün %15 karartılmışı.
            let zemin: String = "FF141F23"
            return ProfilUstAlanRenkleri(
                zemin: Color("background_color"),
                yazi: Color.white,
                cizgi: AvatarRenkCevirici.renk(rrggbb: AvatarOrtak.ustAlanCizgiRengi(zemin: zemin))
            )
        }
        return ProfilUstAlanRenkleri(
            zemin: AvatarRenkCevirici.renk(rrggbb: renk),
            yazi: AvatarRenkCevirici.renk(rrggbb: AvatarOrtak.ustAlanYaziRengi(zemin: renk)),
            cizgi: AvatarRenkCevirici.renk(rrggbb: AvatarOrtak.ustAlanCizgiRengi(zemin: renk))
        )
    }
}

/// Kendi profilinde profileTopPanel, başkasınınkinde otherUserProfileTopBar (ikisi de 56 pt).
struct ProfilUstAlan: View {
    @ObservedObject var model: ProfilSayfaModeli
    let cizgiGorunur: Bool
    let geri: () -> Void
    let ayarlar: () -> Void

    var body: some View {
        let renkler: ProfilUstAlanRenkleri = ProfilUstAlanRenkleri.hesapla(model.ustAlanRengi)
        return ZStack(alignment: .bottom) {
            if model.baskasi {
                baskasininCubugu(renkler)
            } else {
                kendiPanelim(renkler)
            }
            renkler.cizgi
                .frame(height: 2)
                .opacity(cizgiGorunur ? 1 : 0)
        }
        .frame(height: 56)
        .frame(maxWidth: .infinity)
        // Android: setStatusBarTint — durum çubuğu da avatarın renginde.
        .background(renkler.zemin.ignoresSafeArea(edges: .top))
    }

    /// tvTopLeftName (22, başta 16, sonda 64 boşluk) + btnAccountSettings (48x48, iç boşluk 10, sağdan 8).
    private func kendiPanelim(_ renkler: ProfilUstAlanRenkleri) -> some View {
        ZStack {
            HStack(spacing: 0) {
                Text(model.ad)
                    .font(ProfilStil.yazi(22))
                    .foregroundColor(renkler.yazi)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .padding(.leading, 16)
                    .padding(.trailing, 64)
                Spacer(minLength: 0)
            }
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                Button(action: ayarlar) {
                    Image("setting_ic")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(renkler.yazi)
                        .frame(width: 28, height: 28)
                        .frame(width: 48, height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(Metin.al("fragment_profile_btn_account_settings_desc")))
                .padding(.trailing, 8)
            }
        }
        .frame(height: 56)
    }

    /// btnOtherUserProfileBack (48, iç boşluk 12) + ad (18, ortada) + 48'lik boş yer (btnStudentProgress).
    private func baskasininCubugu(_ renkler: ProfilUstAlanRenkleri) -> some View {
        HStack(spacing: 0) {
            Button(action: geri) {
                Image("ic_back_arrow")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(renkler.yazi)
                    .frame(width: 24, height: 24)
                    .frame(width: 48, height: 48)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(Metin.al("fragment_profile_btn_other_user_profile_back_desc")))
            Text(model.ad)
                .font(ProfilStil.yazi(18))
                .foregroundColor(renkler.yazi)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity)
            // btnStudentProgress (kurs öğretmeni → öğrencinin ilerleme raporu): ilk iOS sürümünde yok; Android'de
            // görünmezken de yer tuttuğu için (ad ortada kalsın) burada da 48'lik boşluk.
            Color.clear.frame(width: 48, height: 48)
        }
        .padding(.leading, 4)
        .padding(.trailing, 16)
        .frame(height: 56)
    }
}

/// avatarHeader: ekran genişliğinde, avatarın arka plan renginde; imgProfilePhoto 240x240, üstten 8,
/// alt kenara ortalı. Avatar dairesiz çizilir (Android: AvatarView.toBitmap(withBackground = false)).
struct ProfilAvatarAlani: View {
    @ObservedObject var model: ProfilSayfaModeli
    let ac: () -> Void

    var body: some View {
        let renkler: ProfilUstAlanRenkleri = ProfilUstAlanRenkleri.hesapla(model.ustAlanRengi)
        return Button(action: ac) {
            ZStack(alignment: .bottom) {
                renkler.zemin
                avatar
                    .frame(width: 240, height: 240)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 248)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(model.baskasi)
        .accessibilityLabel(Text(Metin.al("fragment_profile_img_profile_photo_desc")))
    }

    @ViewBuilder
    private var avatar: some View {
        if let config = model.avatarConfig {
            AvatarGorunumu(config: config, arkaPlan: false, yakinPlan: false)
        } else {
            Image("non_user")
                .resizable()
                .scaledToFit()
        }
    }
}

// MARK: - 1. kart: kullanıcı satırı, sayaçlar, düğme

struct ProfilBaslikKarti: View {
    @ObservedObject var model: ProfilSayfaModeli
    @ObservedObject var gezinme: ProfilGezinme

    var body: some View {
        VStack(spacing: 0) {
            Text(model.kullaniciSatiri)
                .font(ProfilStil.yazi(14))
                .foregroundColor(ProfilStil.soluk)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            HStack(spacing: 0) {
                ProfilSayac(sayi: model.takipSayisi, etiket: Metin.al("fragment_profile_text")) {
                    if !model.baskasi { gezinme.ac(.takipListesi(0)) }
                }
                ProfilSayac(sayi: model.takipciSayisi, etiket: Metin.al("fragment_profile_text_2")) {
                    if !model.baskasi { gezinme.ac(.takipListesi(1)) }
                }
            }
            dugme
                .padding(.horizontal, 16)
                .padding(.top, 6)
                .padding(.bottom, 14)
        }
        .frame(maxWidth: .infinity)
    }

    /// btnAddFriend'in görünen gövdesi. Android'de yükseklik wrap_content'tir: 16sp satır (~26dp)
    /// + paddingVertical 12dp x 2 ≈ 50dp; MaterialButton bunun üst ve altından 6'şar dp'yi boş
    /// bırakır (insetTop/insetBottom), görünen gövde ≈ 38–40dp kalır (cihazda ölçülmedi).
    /// Alttaki 14 = inset 6 + layout_marginBottom 8.
    static let dugmeYuksekligi: CGFloat = 40

    @ViewBuilder
    private var dugme: some View {
        if model.baskasi {
            // Takip edilirken düğme KAPALI değil: ikinci dokunuş onay sorup takibi bırakır (yarı saydamlık yok).
            ProfilCerceveliDugme(
                metin: model.takipDugmesiMetni,
                simge: "ic_person_add",
                yukseklik: ProfilBaslikKarti.dugmeYuksekligi,
                etkin: model.takipDugmesiEtkin,
                eylem: { model.takipDugmesineDokunuldu(bildir: { metin in gezinme.bildir(metin) }) }
            )
        } else {
            ProfilCerceveliDugme(
                metin: Metin.al("fragment_profile_btn_add_friend"),
                simge: "ic_person_add",
                yukseklik: ProfilBaslikKarti.dugmeYuksekligi,
                eylem: { gezinme.ac(.arkadasAra) }
            )
        }
    }
}

/// Sayaç sütunu: sayı (20) + etiket (14).
struct ProfilSayac: View {
    let sayi: String
    let etiket: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            VStack(spacing: 0) {
                Text(sayi)
                    .font(ProfilStil.yazi(20))
                    .foregroundColor(Color.white)
                Text(etiket)
                    .font(ProfilStil.yazi(14))
                    .foregroundColor(ProfilStil.soluk)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - 2. kart: ilerleme, abaküs önizlemesi, kupa yolu

struct ProfilIlerlemeKarti: View {
    @ObservedObject var model: ProfilSayfaModeli
    @ObservedObject var gezinme: ProfilGezinme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 0) {
                ilerleme
                Spacer(minLength: 8)
                abakus
            }
            Text(Metin.al("fragment_profile_text_4"))
                .font(ProfilStil.yazi(16))
                .foregroundColor(ProfilStil.soluk)
                .padding(.top, 16)
                .padding(.bottom, 12)
            kupaSeridi
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var ilerleme: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(Metin.al("fragment_profile_tv_user_progress_title"))
                .font(ProfilStil.yazi(16))
                .foregroundColor(ProfilStil.soluk)
                .padding(.bottom, 14)
            if model.ilerlemeGorunur {
                ProfilIlerlemeSatiri(simge: "profile_book_ic3", simgeBoyu: 28, metin: model.tamamlanma)
                    .padding(.bottom, 12)
                ProfilIlerlemeSatiri(simge: "profile_clock_ic2", simgeBoyu: 28, metin: model.toplamSure)
                    .padding(.bottom, 12)
                // tvLongestStreak: streak_flame_ic (24x24), alt boşluk yok.
                ProfilIlerlemeSatiri(simge: "streak_flame_ic", simgeBoyu: 24, metin: model.enUzunSeri)
            }
        }
    }

    private var abakus: some View {
        VStack(spacing: 0) {
            Text(Metin.al("fragment_profile_text_3"))
                .font(ProfilStil.yazi(16))
                .foregroundColor(ProfilStil.soluk)
                .padding(.bottom, 14)
            Button(action: { if model.boncukTipi != nil { model.abakusPaneliAcik = true } }) {
                ProfilBoncukKarti(tip: model.boncukTipi)
            }
            .buttonStyle(.plain)
        }
    }

    private var kupaSeridi: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(model.kupalar) { kart in
                    Button(action: {
                        if let secim = model.kupaSecimi(kart) {
                            gezinme.kupaAc?(secim)
                        }
                    }) {
                        ProfilKupaKartGorunumu(kart: kart)
                    }
                    .buttonStyle(.plain)
                }
            }
            // Son kartın layout_marginEnd'i (12dp) + HorizontalScrollView paddingEnd (16dp).
            .padding(.trailing, 28)
        }
    }
}

/// Simge (drawable'ın kendi boyu) + 8 + metin (16, beyaz). Android: drawableStartCompat + drawablePadding.
struct ProfilIlerlemeSatiri: View {
    let simge: String
    let simgeBoyu: CGFloat
    let metin: String

    var body: some View {
        HStack(spacing: 8) {
            Image(simge)
                .resizable()
                .scaledToFit()
                .frame(width: simgeBoyu, height: simgeBoyu)
            Text(metin)
                .font(ProfilStil.yazi(16))
                .foregroundColor(Color.white)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// 90x90, 12 pt köşeli boncuk kartı (abacusPreviewCard / previewBeadCard), görsel 14 pt içeride.
/// Android boncuğu AbacusBeadRenderer ile kullanıcının renkleriyle çizer; çizim Abakus klasörünün
/// işidir. Burada tipin adına göre yaklaşık bir sabit görsel gösterilir.
struct ProfilBoncukKarti: View {
    let tip: String?

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12).fill(ProfilStil.onizlemeKarti)
            if let tipAdi = tip {
                Image(ProfilBoncukKarti.gorselAdi(tipAdi))
                    .resizable()
                    .scaledToFit()
                    .padding(14)
            }
        }
        .frame(width: 90, height: 90)
    }

    /// BeadType adı → Assets içindeki sabit görsel (renk özelleştirmesi uygulanmaz).
    static func gorselAdi(_ tip: String) -> String {
        if tip.hasPrefix("BALL") {
            return "abacus_bead_ball" + String(tip.dropFirst(4))
        }
        if tip == "BOWLING" {
            return "bowling_ball_ic"
        }
        if tip == "ANIMAL" {
            return "animal_head_ic1"
        }
        if tip.hasPrefix("ANIMAL") {
            return "animal_head_ic" + String(tip.dropFirst(6))
        }
        if tip == "SOROBAN2" {
            return "soroban_bead2"
        }
        return "soroban_bead"
    }
}

/// item_profile_cup_card.xml: 110x130, 12 pt köşe, zemin bright_background_color.
struct ProfilKupaKartGorunumu: View {
    let kart: ProfilKupaKarti

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 4) {
                Text(kart.tanim.baslik)
                    .font(ProfilStil.yazi(14))
                    .foregroundColor(ProfilStil.soluk)
                    .lineLimit(1)
                if kart.tanim.yasakSimgesi {
                    Image("forbidden_abacus_ic")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                }
            }
            // cardLottie: 60dp genişlik, layout_weight=1 (başlık ile skor arasında kalan yükseklik),
            // üst/alt 4dp; lottie_autoPlay + lottie_loop. İçerik sığdırılır (fitCenter).
            ProfilAnimasyonGorunumu(
                istek: ProfilAnimasyonIstegi(dosya: kart.tanim.animasyonDosyasi, oynat: true, kare: 0, renk: nil)
            )
            .frame(width: 60)
            .frame(minHeight: 0, maxHeight: .infinity)
            .padding(.vertical, 4)
            Text(kart.skor)
                .font(ProfilStil.yazi(18))
                .foregroundColor(Color.white)
                .lineLimit(1)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 8)
        .frame(width: 110, height: 130)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color("bright_background_color")))
        .contentShape(Rectangle())
    }
}

// MARK: - 3. kart: rozetler

struct ProfilRozetKarti: View {
    @ObservedObject var model: ProfilSayfaModeli
    let ac: () -> Void

    var body: some View {
        Button(action: ac) {
            VStack(spacing: 12) {
                HStack(spacing: 0) {
                    Text(Metin.al("fragment_profile_tv_profile_badges_title"))
                        .font(ProfilStil.yazi(16))
                        .foregroundColor(ProfilStil.soluk)
                    Spacer(minLength: 0)
                    Image("dualingo_arrow_ic")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(ProfilStil.soluk)
                        .frame(width: 22, height: 22)
                        .frame(width: 32, height: 32)
                }
                HStack(alignment: .top, spacing: 0) {
                    ForEach(model.rozetler) { yuva in
                        ProfilRozetGorunumu(yuva: yuva)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(Metin.al("fragment_profile_btn_profile_badges_arrow_desc")))
    }
}

/// Tek rozet: 72x72 alan; alt katman 150x150 (taşar), üst katman ölçekli; değer metni 25 pt yukarı biner.
/// Android: bindSingleProfileBadge. Kilitli rozette katmanlar düz griye boyanır
/// (alt: badge_locked_base, üst: badge_locked_top); açık rozette alt katman kademe tonunu alır.
struct ProfilRozetGorunumu: View {
    let yuva: ProfilRozetYuvasi

    var body: some View {
        VStack(spacing: -25) {
            ZStack {
                if !yuva.altDosya.isEmpty {
                    ProfilAnimasyonGorunumu(
                        istek: ProfilAnimasyonIstegi(
                            dosya: yuva.altDosya, oynat: false, kare: 9999, renk: altRenk, duzRenk: !yuva.acik
                        )
                    )
                    .frame(width: 150, height: 150)
                }
                ProfilAnimasyonGorunumu(
                    istek: ProfilAnimasyonIstegi(
                        dosya: yuva.ustDosya, oynat: false, kare: yuva.ustKare, renk: ustRenk, duzRenk: !yuva.acik
                    )
                )
                .frame(width: 72, height: 72)
                .scaleEffect(yuva.ustOlcek)
                .offset(y: -1)
            }
            .frame(width: 72, height: 72)
            if yuva.degerGorunur {
                ProfilCizgiliMetin(
                    metin: yuva.deger,
                    boyut: 22,
                    renk: yuva.acik ? Color.white : Color("badge_locked_base"),
                    cizgi: yuva.acik ? ProfilStil.rozetDegerCizgisi : Color("badge_locked_top")
                )
            }
        }
    }

    private var altRenk: Color? {
        if !yuva.acik {
            return Color("badge_locked_base")
        }
        switch yuva.ton {
        case "BRONZE": return Color("badge_tone_bronze_filter")
        case "SILVER": return Color("badge_tone_silver_filter")
        case "RED": return Color("badge_tone_red_filter")
        case "PURPLE": return Color("badge_tone_purple_filter")
        default: return nil
        }
    }

    private var ustRenk: Color? {
        return yuva.acik ? nil : Color("badge_locked_top")
    }
}

/// Dış çizgili metin (Android: BadgeRewardTextView). Çizgi, metnin sekiz yöne kaydırılmış
/// kopyalarıyla yaklaşık olarak çizilir.
struct ProfilCizgiliMetin: View {
    let metin: String
    let boyut: CGFloat
    let renk: Color
    let cizgi: Color

    private static let kaymalar: [CGSize] = [
        CGSize(width: -1.5, height: 0), CGSize(width: 1.5, height: 0),
        CGSize(width: 0, height: -1.5), CGSize(width: 0, height: 1.5),
        CGSize(width: -1, height: -1), CGSize(width: 1, height: -1),
        CGSize(width: -1, height: 1), CGSize(width: 1, height: 1)
    ]

    var body: some View {
        ZStack {
            ForEach(0..<ProfilCizgiliMetin.kaymalar.count, id: \.self) { sira in
                Text(metin)
                    .font(ProfilStil.yazi(boyut))
                    .foregroundColor(cizgi)
                    .offset(ProfilCizgiliMetin.kaymalar[sira])
            }
            Text(metin)
                .font(ProfilStil.yazi(boyut))
                .foregroundColor(renk)
        }
    }
}
