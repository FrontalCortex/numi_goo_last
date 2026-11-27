import SwiftUI

// Giriş başlangıç ekranı. Android: LoginStartActivity + activity_login_start.xml (yeniden tasarım).
// Yerleşim (iç boşluk: yanlar 24dp, üst 16dp, alt 24dp; zemin background_color), yukarıdan aşağı:
//   - sağ üstte öğretmen/öğrenci geçişi (14sp, dark_text_secondary, en az 48dp, iç boşluk 12/4dp)
//   - maskot (genişliğin %70'i, en çok 320dp, kare), başlık (28sp kalın, 12dp), gövde metni (17sp,
//     satır aralığı ×1,2, dark_text_secondary, 12dp; altı 48dp). Bu üçü sıkışık bir dikey zincir,
//     bias 1: grup "Başla"nın hemen üstüne yaslanır.
//   - "Başla": 56dp, köşe 16dp, dark_primary yüz, yazı 18sp kalın background_color, göçen
//     (DepthButtons.primary(16f))
//   - "Zaten hesabım var": bağlantı görünümü (17sp kalın dark_primary, 12dp üstte, en alta yaslı)
// Maskot: açılış geçişi bitince (400 ms) el sallar, beklerken 5 sn'de bir selam verir, dokununca
// sevinir. Android'de geri tuşu bu ekranda işlevsizdir; burada geri düğmesi yoktur.
@MainActor
struct GirisBaslangicGorunumu: View {
    @ObservedObject var model: GirisModeli

    var body: some View {
        GeometryReader { alan in
            let maskotKenari: CGFloat = min(alan.size.width * 0.7, 320)
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    Spacer(minLength: 0)
                    modDugmesi
                }
                Spacer(minLength: 0)
                GirisBaslangicMaskotu()
                    .frame(width: maskotKenari, height: maskotKenari)
                metinler
                GirisGocenDugme(
                    metin: Metin.al("login_start_start_button"),
                    buyukHarf: false,
                    yaziBoyu: 18,
                    kose: 16,
                    yaziRengi: Color("background_color")
                ) {
                    model.baslangicBasla()
                }
                GirisBaglantiDugmesi(metin: Metin.al("login_start_login_button")) {
                    model.baslangicGirisYap()
                }
                .padding(.top, 12)
            }
            .frame(width: alan.size.width, height: alan.size.height)
        }
        .padding(.horizontal, 24)
        .padding(.top, 16)
        .padding(.bottom, 24)
        .onAppear {
            model.baslangicGoruldu()
        }
    }

    private var metinler: some View {
        let ogretmen: Bool = model.ogretmenModu
        let baslik: String = Metin.al(ogretmen ? "login_start_teacher_title" : "login_start_student_title")
        let govde: String = Metin.al(ogretmen ? "login_start_teacher_subtitle" : "login_start_student_subtitle")
        return VStack(spacing: 0) {
            Text(baslik)
                .font(Tema.yazi(28))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
            Text(govde)
                .font(Tema.yazi(17))
                .foregroundColor(Color("dark_text_secondary"))
                .multilineTextAlignment(.center)
                // lineSpacingMultiplier 1.2: satır yüksekliğinin %20'si kadar ek aralık.
                .lineSpacing(17 * 0.2)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
                .padding(.bottom, 48)
        }
        .id(ogretmen)
        .transition(.opacity)
    }

    private var modDugmesi: some View {
        Button {
            model.modDegistir()
        } label: {
            Text(Metin.al(model.ogretmenModu ? "login_start_student_mode_button" : "login_start_teacher_mode_button"))
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .padding(.leading, 12)
                .padding(.trailing, 4)
                .frame(minHeight: 48)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Başlangıç ekranının maskotu (BunnyMascotView): 400 ms sonra selam (greet), sonra 5 sn'de bir
/// kendiliğinden selam (autoGreetIntervalMs = 5000), dokununca sevinç (cheer). Araya giren bir hâl
/// sayacı baştan başlatır. Aynı hâli baştan oynatmak için görünüm kimliği değiştirilir.
/// (Bilerek @MainActor değil: `.task` kapanışı içinden `oynat` eşzamanlı çağrılıyor.)
struct GirisBaslangicMaskotu: View {
    /// "" → nötr bekleme duruşu (SobiMaskotGorunumu bilinmeyen adda bekler).
    @State private var emote: String = ""
    @State private var sayac: Int = 0
    @State private var sonOynatma: Date = Date()

    /// Android: autoGreetIntervalMs.
    private static let selamAraligi: Double = 5.0

    var body: some View {
        ZStack {
            SobiMaskotGorunumu(emote: emote)
                .id(sayac)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            oynat("CHEER")
        }
        .task {
            try? await Task.sleep(nanoseconds: 400_000_000)
            oynat("GREET")
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 500_000_000)
                let gecen: Double = Date().timeIntervalSince(sonOynatma)
                if gecen >= GirisBaslangicMaskotu.selamAraligi {
                    oynat("GREET")
                }
            }
        }
    }

    private func oynat(_ ad: String) {
        emote = ad
        sayac += 1
        sonOynatma = Date()
    }
}
