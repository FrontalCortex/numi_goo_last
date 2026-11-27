import SwiftUI

// Profil klasörünün ortak görsel yardımcıları: layout XML'lerinde doğrudan yazılmış renkler,
// yazı tipi ve birden çok ekranda yinelenen küçük parçalar (üst çubuk, çerçeveli düğme,
// harf avatarı, bildirim balonu, Lottie çizimi).

enum ProfilStil {
    /// 0xRRGGBB biçimindeki sabit renk.
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// "#RRGGBB" metninden renk (ortak kodun verdiği harf avatarı renkleri için).
    static func renk(onaltilik: String) -> Color {
        let temiz = onaltilik.replacingOccurrences(of: "#", with: "")
        let deger = UInt32(temiz, radix: 16) ?? 0x1CB0F6
        return renk(deger)
    }

    static func yazi(_ boyut: CGFloat) -> Font {
        return Tema.yazi(boyut, kalin: true)
    }

    /// #93A5B3 — ikincil metin (fragment_profile, item_follow_user, fragment_add_friend).
    static let soluk: Color = ProfilStil.renk(0x93A5B3)
    /// #131F24 / #37464F — çerçeveli düğme zemini ve çizgisi (btnAddFriend, btnLoadMore).
    static let dugmeZemini: Color = ProfilStil.renk(0x131F24)
    static let dugmeCizgisi: Color = ProfilStil.renk(0x37464F)
    /// #1A262B — abaküs önizleme kartı.
    static let onizlemeKarti: Color = ProfilStil.renk(0x1A262B)
    /// #1CB0F6 — seçili sekme, takip düğmesi (bg_add_friend_btn).
    static let mavi: Color = ProfilStil.renk(0x1CB0F6)
    /// #4D93A5B3 — gönderilmiş takip düğmesi (bg_add_friend_btn_sent).
    static let dugmeGonderildi: Color = ProfilStil.renk(0x93A5B3, alfa: Double(0x4D) / 255.0)
    /// #1A3040 — sekme altı ayırıcı.
    static let sekmeAyirici: Color = ProfilStil.renk(0x1A3040)
    /// #203137 — arama çubuğu (bg_search_bar) ve giriş alanı zemini (bg_edit_profile_input).
    static let girisZemini: Color = ProfilStil.renk(0x203137)
    /// #30363D — giriş alanı çizgisi.
    static let girisCizgisi: Color = ProfilStil.renk(0x30363D)
    /// #5C7A8A — arama ipucu metni.
    static let aramaIpucu: Color = ProfilStil.renk(0x5C7A8A)
    /// #E6EEF2 / #607887 / #3B4A55 / #BDBDBD / #47B0F2 / #4A5A67 — fragment_account_settings.
    static let ayarMetni: Color = ProfilStil.renk(0xE6EEF2)
    static let ayarOku: Color = ProfilStil.renk(0x607887)
    static let ayarAyirici: Color = ProfilStil.renk(0x3B4A55)
    static let ayarBaslik: Color = ProfilStil.renk(0xBDBDBD)
    static let cikisMetni: Color = ProfilStil.renk(0x47B0F2)
    static let cikisCizgisi: Color = ProfilStil.renk(0x4A5A67)
    /// #292E2C — hint_background (@color/message_getter).
    static let ipucuZemini: Color = ProfilStil.renk(0x292E2C)
    /// #FF8F00 — açık rozet değerinin dış çizgisi (ProfileFragment.unlockedValueStrokeColor).
    static let rozetDegerCizgisi: Color = ProfilStil.renk(0xFF8F00)
}

// MARK: - Üst çubuk

/// 56 pt yüksekliğinde başlık çubuğu: solda geri düğmesi (48x48), ortada başlık, sağda dengeleyici boşluk.
/// Android: fragment_followers_following / fragment_add_friend / fragment_edit_profile araç çubukları.
struct ProfilUstCubuk: View {
    let baslik: String
    var simge: String = "ic_back_arrow"
    var yaziBoyu: CGFloat = 18
    var solBosluk: CGFloat = 4
    var sagBosluk: CGFloat = 16
    let geri: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: geri) {
                Image(simge)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundColor(Color.white)
                    .frame(width: 48, height: 48)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Geri"))
            Text(baslik)
                .font(ProfilStil.yazi(yaziBoyu))
                .foregroundColor(Color.white)
                .lineLimit(1)
                .frame(maxWidth: .infinity)
            Color.clear.frame(width: 48, height: 48)
        }
        .padding(.leading, solBosluk)
        .padding(.trailing, sagBosluk)
        .frame(height: 56)
    }
}

// MARK: - Çerçeveli düğme

/// MaterialButton (zemin #131F24, 2 pt #37464F çizgi, 12 pt köşe, dikey 12 pt iç boşluk).
struct ProfilCerceveliDugme: View {
    let metin: String
    var simge: String? = nil
    var metinRengi: Color = Color.white
    var zemin: Color = ProfilStil.dugmeZemini
    var cizgi: Color = ProfilStil.dugmeCizgisi
    var cizgiKalinligi: CGFloat = 2
    var kose: CGFloat = 12
    var yukseklik: CGFloat = 48
    var yaziBoyu: CGFloat = 16
    var etkin: Bool = true
    var opaklik: Double = 1.0
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 8) {
                if let simgeAdi = simge {
                    Image(simgeAdi)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                // TextAppearance.MaterialComponents.Button: letterSpacing = 0.0892857143 em.
                Text(metin)
                    .tracking(yaziBoyu * 0.0892857143)
                    .font(ProfilStil.yazi(yaziBoyu))
                    .lineLimit(1)
            }
            .foregroundColor(metinRengi)
            .frame(maxWidth: .infinity)
            .frame(height: yukseklik)
            .background(RoundedRectangle(cornerRadius: kose).fill(zemin))
            .overlay(RoundedRectangle(cornerRadius: kose).strokeBorder(cizgi, lineWidth: cizgiKalinligi))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!etkin)
        .opacity(opaklik)
    }
}

// MARK: - Avatar

/// Liste satırlarındaki avatar: 1...12 ise `avatar_icN` görseli, değilse renkli daire içinde adın ilk harfi.
/// Android: FollowUserAdapter / FriendSearchAdapter (tvAvatarLetter).
struct ProfilSatirAvatari: View {
    let ad: String
    let seciliAvatar: Int
    let boyut: CGFloat
    let harfBoyu: CGFloat
    /// "#RRGGBB"
    let renk: String

    var body: some View {
        Group {
            if ProfilOrtak.avatarGecerli(seciliAvatar) {
                Image(ProfilOrtak.avatarAdi(seciliAvatar))
                    .resizable()
                    .scaledToFit()
            } else {
                ZStack {
                    Circle().fill(ProfilStil.renk(onaltilik: renk))
                    Text(ProfilOrtak.avatarHarfi(ad: ad))
                        .font(ProfilStil.yazi(harfBoyu))
                        .foregroundColor(Color.white)
                }
            }
        }
        .frame(width: boyut, height: boyut)
    }
}

/// 44x44, 12 pt köşeli mavi "kişi ekle" düğmesi (bg_add_friend_btn + ic_person_add).
struct ProfilKisiEkleDugmesi: View {
    var gonderildi: Bool = false
    var etkin: Bool = true
    var opaklik: Double = 1.0
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Group {
                if gonderildi {
                    // Android: android.R.drawable.ic_menu_add (sistem görseli; burada SF Symbols karşılığı).
                    Image(systemName: "plus")
                        .font(.system(size: 18, weight: .bold))
                } else {
                    Image("ic_person_add")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }
            .foregroundColor(Color.white)
            .frame(width: 44, height: 44)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(gonderildi ? ProfilStil.dugmeGonderildi : ProfilStil.mavi)
            )
        }
        .buttonStyle(.plain)
        .disabled(!etkin)
        .opacity(opaklik)
        .accessibilityLabel(Text("Takip Et"))
    }
}

// MARK: - Bildirim (Android: Toast)

struct ProfilBildirim: Equatable {
    let id: Int
    let metin: String
}

struct ProfilBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(ProfilStil.yazi(14))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(ProfilStil.renk(0x323232, alfa: 0.95)))
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
    }
}

/// Yükleniyor göstergesi (Android: ProgressBar).
struct ProfilYukleniyor: View {
    var body: some View {
        ProgressView()
            .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
            .scaleEffect(1.4)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
    }
}

// MARK: - Lottie

/// Android'de Lottie ile çizilen bir animasyonun istenen hali.
struct ProfilAnimasyonIstegi {
    /// assets içindeki dosya adı (ör. "dinosaur_anim.json").
    let dosya: String
    /// true: döngüde oynar (kupa kartları). false: `kare`de durur (rozetler).
    let oynat: Bool
    /// Durdurulacak kare (9999 = son kare); `oynat` true ise kullanılmaz.
    let kare: Float
    /// Bütün katmanlara uygulanacak düz renk (kilitli rozet) ya da ton; yoksa nil.
    let renk: Color?
    /// true: `renk` çizimin bütün renklerinin yerine geçer (kilitli rozet; Android:
    /// PorterDuff.Mode.SRC_ATOP). false: `renk` çizimle çarpılır (kademe tonu; MULTIPLY).
    var duzRenk: Bool = false
}

/// Animasyonlar ortak sarmalayıcıyla (`LottieGorunumu`) çizilir; sabit kare sarmalayıcının
/// `baslangicKare` parametresiyle verilir. Sarmalayıcının sözleşmesinde renk süzgeci yoktur
/// (SwiftUI karışım kipleriyle yaklaşık çizilir); tam karşılayan bir çizim yazılırsa yönetici oturum
/// `ProfilAnimasyon.saglayici`'yı atar, Profil dosyalarına dokunmak gerekmez.
enum ProfilAnimasyon {
    static var saglayici: ((ProfilAnimasyonIstegi) -> AnyView)? = nil

    /// Android dosya adı → sarmalayıcının istediği uzantısız ad
    /// ("dinosaur_anim.json" → "dinosaur_anim", "silver_medal_anim.json.json" → "silver_medal_anim").
    static func lottieAdi(_ dosya: String) -> String {
        var ad: String = dosya
        while ad.hasSuffix(".json") {
            ad = String(ad.dropLast(5))
        }
        return ad
    }
}

@MainActor
struct ProfilAnimasyonGorunumu: View {
    let istek: ProfilAnimasyonIstegi

    var body: some View {
        if let saglayici = ProfilAnimasyon.saglayici {
            saglayici(istek)
        } else {
            boyali
        }
    }

    /// Android'deki oynatma hali:
    ///   - kupa kartı: lottie_autoPlay + lottie_loop → sürekli döner
    ///   - rozet (ProfileFragment.applyBadge…: `progress = frameToProgress(topHoldFrame, …)` +
    ///     `pauseAnimation()`; alt katmanda `progress = 1f`): istenen karede sabit durur.
    ///     9999 = son kare; sarmalayıcı animasyonun son karesinden büyük değeri son kareye kırpar.
    @ViewBuilder
    private var cizim: some View {
        let ad: String = ProfilAnimasyon.lottieAdi(istek.dosya)
        if istek.oynat {
            LottieGorunumu(ad, dongu: true, hiz: 1.0, oynat: true, bitti: nil)
        } else {
            LottieGorunumu(
                ad,
                dongu: false,
                hiz: 1.0,
                oynat: false,
                bitti: nil,
                baslangicKare: Double(istek.kare)
            )
        }
    }

    @ViewBuilder
    private var boyali: some View {
        if let renk = istek.renk {
            if istek.duzRenk {
                cizim
                    .overlay(Rectangle().fill(renk).blendMode(.sourceAtop))
                    .compositingGroup()
            } else {
                cizim
                    .colorMultiply(renk)
            }
        } else {
            cizim
        }
    }
}
