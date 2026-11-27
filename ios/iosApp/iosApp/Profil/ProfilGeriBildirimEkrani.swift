import SwiftUI

// "Bize Ulaşın" (geri bildirim) ekranı.
// Android: FeedbackFragment + res/layout/fragment_feedback.xml. Saatlik gönderim sınırı (5) ve
// `feedback` dokümanının alanları ortak koddadır (FeedbackRules).
// Android'de bu ekran görevler katmanından da açılabilir; burada yalnızca hesap ayarlarından açılır.

struct ProfilGeriBildirimEkrani: View {
    @ObservedObject var gezinme: ProfilGezinme
    @State private var metin: String = ""
    @State private var gonderiliyor: Bool = false
    @FocusState private var odakta: Bool

    var body: some View {
        ZStack {
            ScrollView(.vertical, showsIndicators: false) {
                icerik.padding(20)
            }
            if gonderiliyor {
                // sendBlockingOverlay: #CC000000, dokunuşları yutar.
                ZStack {
                    Color.black.opacity(0.8).ignoresSafeArea()
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
                        .scaleEffect(1.4)
                }
            }
        }
    }

    private var icerik: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                RoundedRectangle(cornerRadius: 8).fill(ProfilStil.ipucuZemini)
                Image("feedback_ic")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 76, height: 76)
            }
            .frame(width: 90, height: 90)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)

            Text(Metin.al("fragment_feedback_text"))
                .font(ProfilStil.yazi(22))
                .foregroundColor(Color("dark_text"))
                .frame(maxWidth: .infinity)
                .padding(.bottom, 8)
            Text(Metin.al("fragment_feedback_text_2"))
                .font(ProfilStil.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity)
                .padding(.bottom, 32)

            Text(Metin.al("fragment_feedback_text_3"))
                .font(ProfilStil.yazi(15))
                .foregroundColor(Color("dark_text"))
                .padding(.bottom, 12)
            girisAlani
                .padding(.bottom, 32)

            Text(Metin.al("fragment_feedback_text_4"))
                .font(ProfilStil.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .frame(maxWidth: .infinity)
                .padding(.bottom, 32)

            dugmeler
                .padding(.bottom, 16)
        }
    }

    /// 200 pt yükseklik, 8 pt köşe, zemin #292E2C, iç boşluk 16; ipucu #80FFFFFF.
    private var girisAlani: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 8).fill(ProfilStil.ipucuZemini)
            if metin.isEmpty {
                Text(Metin.al("fragment_feedback_feedback_input_hint"))
                    .font(ProfilStil.yazi(14))
                    .foregroundColor(Color.white.opacity(0.5))
                    .padding(16)
                    .allowsHitTesting(false)
            }
            TextEditor(text: $metin)
                .font(ProfilStil.yazi(14))
                .foregroundColor(Color("dark_text"))
                .scrollContentBackground(.hidden)
                .focused($odakta)
                // TextEditor'ın kendi iç boşluğu (yaklaşık 5 / 8) düşülür.
                .padding(.horizontal, 11)
                .padding(.vertical, 8)
        }
        .frame(height: 200)
    }

    private var dugmeler: some View {
        HStack(spacing: 8) {
            Spacer(minLength: 0)
            Button(action: { kapat() }) {
                Text(Metin.al("fragment_feedback_close_button"))
                    .font(ProfilStil.yazi(15))
                    .foregroundColor(Color("dark_text_secondary"))
                    .padding(.horizontal, 24)
                    .frame(height: 40)
                    .background(RoundedRectangle(cornerRadius: 20).fill(Color("background_color")))
                    .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(ProfilStil.cikisCizgisi, lineWidth: 2))
            }
            .buttonStyle(.plain)
            Button(action: { gonder() }) {
                Text(Metin.al("fragment_feedback_send_feedback_button"))
                    .font(ProfilStil.yazi(15))
                    .foregroundColor(gonderEtkin ? Color("button_text_enabled") : Color("button_text_disabled"))
                    .padding(.horizontal, 32)
                    .frame(height: 40)
                    .background(
                        RoundedRectangle(cornerRadius: 20)
                            .fill(gonderEtkin ? Color("lesson_completed") : Color("button_disabled"))
                    )
            }
            .buttonStyle(.plain)
            .disabled(!gonderEtkin)
        }
    }

    private var gonderEtkin: Bool {
        return ProfilOrtak.geriBildirimGonderilebilir(metin)
    }

    private func kapat() {
        odakta = false
        gezinme.geri()
    }

    /// Android: sendFeedbackButton → canSendFeedback → sendFeedbackToFirestore.
    private func gonder() {
        let depo = gezinme.depo
        let simdiMs = Int64(Date().timeIntervalSince1970 * 1000)
        let izin = ProfilOrtak.geriBildirimIzni(kayitliZamanlar: depo.geriBildirimZamanlari, simdiMs: simdiMs)
        // Temizlenmiş liste her denetimde geri yazılır.
        depo.geriBildirimZamanlari = izin.1
        if !izin.0 {
            gezinme.bildir(Metin.al("ui_son_1_saat_icinde_cok_fazla"), uzun: true)
            return
        }
        let mesaj = metin.trimmingCharacters(in: .whitespacesAndNewlines)
        if mesaj.isEmpty { return }
        gonderiliyor = true
        Task {
            do {
                try await depo.geriBildirimGonder(mesaj: mesaj)
                gonderiliyor = false
                let bitisMs = Int64(Date().timeIntervalSince1970 * 1000)
                depo.geriBildirimZamanlari = ProfilOrtak.geriBildirimKaydi(
                    kayitliZamanlar: depo.geriBildirimZamanlari, simdiMs: bitisMs
                )
                gezinme.bildir(Metin.al("ui_gorusleriniz_basariyla_iletildi_tesekkurler"), uzun: true)
                kapat()
            } catch {
                gonderiliyor = false
                gezinme.bildir(
                    Metin.al("ui_gonderilemedi_x", ProfilSayfaModeli.hataMetni(error)), uzun: true
                )
            }
        }
    }
}
